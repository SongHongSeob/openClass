package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.request.RequestState;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentRequestProcessor;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 워커 디스패치 통합 테스트 — AC-ENR-008(워커 미구동 시 도메인 무변화),
 * AC-ENR-013(마감 강좌 요청 종결), AC-ENR-014(중복 신청 3종 거부) (M2).
 */
@SpringBootTest
class EnrollmentWorkerDispatchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private EnrollmentQueueWorker worker;

    @Autowired
    private EnrollmentRequestProcessor processor;

    @Autowired
    private EnrollmentRequestRepository requestRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void cleanUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        waitlistEntryRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private Long createCourse(int capacity) {
        return courseRepository.save(Course.create("워커테스트강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();
    }

    private Long createMember(String email) {
        return memberRepository.save(Member.createMember(email, "hash", "회원")).getId();
    }

    // AC-ENR-008 (M2 범위 — 접수 API만. 취소·정원증설 API는 각각 M4·M5가
    // 추가하며, 그 산출물이 생기면 이 테스트를 확장한다 — M1의
    // EnrollmentReceiptApiIntegrationTest AC-ENR-002가 세운 PASS-WITH-DEBT
    // 선례와 동일한 패턴이다.)
    @Test
    void 워커를_한번도_구동하지_않으면_접수_API_호출로도_도메인이_변하지_않는다() {
        Long courseId = createCourse(1);
        Long memberId = createMember("invariance@example.com");

        long initialEnrollmentCount = enrollmentRepository.count();
        int initialEnrolledCount = courseRepository.findById(courseId).orElseThrow().getEnrolledCount();

        receiptService.receiveEnrollment(memberId, courseId);

        assertThat(enrollmentRepository.count()).isEqualTo(initialEnrollmentCount);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount())
                .isEqualTo(initialEnrolledCount);
        assertThat(requestRepository.count()).isEqualTo(1);
    }

    // AC-ENR-013
    @Test
    void 마감된_강좌의_대기중_ENROLL_요청_3건은_전부_CLOSED로_종결되고_도메인_생성이_없다() {
        Long courseId = createCourse(5);
        Long m1 = createMember("closed1@example.com");
        Long m2 = createMember("closed2@example.com");
        Long m3 = createMember("closed3@example.com");
        receiptService.receiveEnrollment(m1, courseId);
        receiptService.receiveEnrollment(m2, courseId);
        receiptService.receiveEnrollment(m3, courseId);

        Course course = courseRepository.findById(courseId).orElseThrow();
        course.close();
        courseRepository.save(course);

        worker.drainQueue();

        assertThat(requestRepository.findAll())
                .extracting(com.hongseob.openclass_ap.enrollment.request.EnrollmentRequest::getResult)
                .containsOnly(RequestResult.CLOSED);
        assertThat(enrollmentRepository.count()).isZero();
        assertThat(waitlistEntryRepository.count()).isZero();
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isZero();
    }

    // AC-ENR-014 — 중복 검사 1번(이미 유효한 확정 보유)
    @Test
    void 이미_확정된_회원이_재신청하면_REJECTED이고_확정_행도_카운트도_불변이다() {
        Long courseId = createCourse(5);
        Long memberId = createMember("dup1@example.com");

        Long firstRequestId = receiptService.receiveEnrollment(memberId, courseId);
        worker.drainQueue();
        assertThat(requestRepository.findById(firstRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.SUCCESS);

        Long secondRequestId = receiptService.receiveEnrollment(memberId, courseId);
        worker.drainQueue();

        assertThat(requestRepository.findById(secondRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.REJECTED);
        assertThat(enrollmentRepository.count()).isEqualTo(1);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(1);
    }

    // AC-ENR-014 — 중복 검사 2번(미처리 PENDING 큐 요청 보유). firstRequestId를
    // 의도적으로 처리하지 않고 PENDING으로 남긴 채 secondRequestId만 처리해,
    // 검사 1(이미 확정)·검사 3(활성 대기)이 아니라 검사 2가 REJECTED의
    // 원인임을 분리해서 증명한다.
    @Test
    void 미처리_PENDING_요청을_보유한_상태에서_재신청하면_두번째_요청이_REJECTED다() {
        Long courseId = createCourse(5);
        Long memberId = createMember("dup2@example.com");

        Long firstRequestId = receiptService.receiveEnrollment(memberId, courseId);
        Long secondRequestId = receiptService.receiveEnrollment(memberId, courseId);

        processor.processOne(secondRequestId);

        assertThat(requestRepository.findById(firstRequestId).orElseThrow().getState())
                .as("첫 번째 요청은 의도적으로 미처리 상태로 남겨 둔다")
                .isEqualTo(RequestState.PENDING);
        assertThat(requestRepository.findById(secondRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.REJECTED);
        assertThat(enrollmentRepository.count()).isZero();
    }

    // REQ-WRK-007 중복 검사 3번(활성 WAITLISTED 항목 보유) — AC-ENR-014
    // 원문은 검사 1·2만 명시하지만 design.md §4.3 / plan.md §F M2가 이
    // 검사도 M2 범위로 규정하므로(2차 감사 E1) 함께 검증한다.
    @Test
    void 활성_대기_항목을_이미_보유한_회원이_재신청하면_REJECTED이고_대기_행이_중복되지_않는다() {
        Long courseId = createCourse(1);
        Long full = createMember("waiter-filler@example.com");
        Long waiter = createMember("waiter-owner@example.com");

        receiptService.receiveEnrollment(full, courseId);
        Long waiterFirstRequestId = receiptService.receiveEnrollment(waiter, courseId);
        worker.drainQueue();
        assertThat(requestRepository.findById(waiterFirstRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.WAITLISTED);

        Long waiterSecondRequestId = receiptService.receiveEnrollment(waiter, courseId);
        worker.drainQueue();

        assertThat(requestRepository.findById(waiterSecondRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.REJECTED);
        assertThat(waitlistEntryRepository.count())
                .as("중복 대기 항목이 새로 생성되지 않아야 한다")
                .isEqualTo(1);
    }
}
