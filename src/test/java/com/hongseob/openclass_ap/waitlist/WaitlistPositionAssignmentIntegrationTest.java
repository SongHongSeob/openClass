package com.hongseob.openclass_ap.waitlist;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.EnrollmentRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 대기명단 순번 부여 규칙 통합 테스트 — AC-ENR-028(결정적 순번 부여 ·
 * {@code MAX(순번)+1} 규칙 검증), AC-ENR-029(대기 순번 중복 DB 제약) (M4).
 */
@SpringBootTest
class WaitlistPositionAssignmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private EnrollmentQueueWorker worker;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        waitlistEntryRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private Long createCourse(int capacity) {
        return courseRepository.save(Course.create("대기순번강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();
    }

    private Long createMember(String email) {
        return memberRepository.save(Member.createMember(email, "hash", "회원")).getId();
    }

    // AC-ENR-028 — 앞부분: 정원 2에 4명이 순서대로 접수하면 뒤 2명이 순번 1·2로 대기한다
    @Test
    void 정원_초과_시_대기_순번이_접수_순서와_일치하고_중복되지_않는다() {
        Long courseId = createCourse(2);
        Long m1 = createMember("wl-pos-1@example.com");
        Long m2 = createMember("wl-pos-2@example.com");
        Long m3 = createMember("wl-pos-3@example.com");
        Long m4 = createMember("wl-pos-4@example.com");

        receiptService.receiveEnrollment(m1, courseId);
        receiptService.receiveEnrollment(m2, courseId);
        Long thirdRequestId = receiptService.receiveEnrollment(m3, courseId);
        Long fourthRequestId = receiptService.receiveEnrollment(m4, courseId);

        worker.drainQueue();

        assertThat(requestRepository.findById(thirdRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.WAITLISTED);
        assertThat(requestRepository.findById(fourthRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.WAITLISTED);

        WaitlistEntry thirdEntry = waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(m3, courseId, WaitlistStatus.WAITING).orElseThrow();
        WaitlistEntry fourthEntry = waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(m4, courseId, WaitlistStatus.WAITING).orElseThrow();
        assertThat(thirdEntry.getPosition()).isEqualTo(1L);
        assertThat(fourthEntry.getPosition()).isEqualTo(2L);
    }

    // AC-ENR-028 — "또한" 절: 승격으로 활성 대기자가 1명(순번 2)만 남은 뒤 신규
    // 접수자의 순번이 3이어야 한다(COUNT(활성)+1 = 2가 아니다). 이 절이야말로
    // REQ-WL-001의 MAX(순번)+1 규칙을 실제로 판별한다(2회차 감사 후속 N5).
    @Test
    void 승격_후_신규_대기자의_순번은_활성_항목_수가_아니라_전체_이력_최대값_1이다() {
        Long courseId = createCourse(2);
        Long a = createMember("wl-pos-a@example.com");
        Long b = createMember("wl-pos-b@example.com");
        Long c = createMember("wl-pos-c@example.com");
        Long d = createMember("wl-pos-d@example.com");
        Long e = createMember("wl-pos-e@example.com");

        receiptService.receiveEnrollment(a, courseId);
        receiptService.receiveEnrollment(b, courseId);
        receiptService.receiveEnrollment(c, courseId); // 대기 순번 1
        receiptService.receiveEnrollment(d, courseId); // 대기 순번 2
        worker.drainQueue();

        // A가 취소 → C(순번 1)가 승격되어 활성 대기자는 D(순번 2) 1명만 남는다.
        Long cancelRequestId = receiptService.receiveCancel(a, findEnrollmentId(a, courseId));
        worker.drainQueue();
        assertThat(requestRepository.findById(cancelRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.CANCELLED);
        assertThat(waitlistEntryRepository.findByMemberIdAndCourseIdAndStatus(c, courseId, WaitlistStatus.WAITING))
                .as("C는 승격되어 더 이상 활성 대기자가 아니어야 한다").isEmpty();
        assertThat(waitlistEntryRepository.findByMemberIdAndCourseIdAndStatus(d, courseId, WaitlistStatus.WAITING))
                .as("D는 여전히 활성 대기자(순번 2)여야 한다").isPresent();

        // 새 회원 E가 접수하면 정원(2)이 이미 A취소+C승격으로 다시 가득 찼으므로 대기 등록된다.
        Long eRequestId = receiptService.receiveEnrollment(e, courseId);
        worker.drainQueue();

        assertThat(requestRepository.findById(eRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.WAITLISTED);
        WaitlistEntry eEntry = waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(e, courseId, WaitlistStatus.WAITING).orElseThrow();
        assertThat(eEntry.getPosition())
                .as("COUNT(활성)+1이면 2가 되어 D의 순번과 충돌한다 — MAX(전체 이력)+1이면 3이다")
                .isEqualTo(3L);
    }

    private Long findEnrollmentId(Long memberId, Long courseId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM enrollment WHERE member_id = ? AND course_id = ? AND status = 'ENROLLED'",
                Long.class, memberId, courseId);
    }

    // AC-ENR-029
    @Test
    void 애플리케이션_계층을_우회한_동일_강좌_동일_순번_활성_대기_INSERT는_DB_제약으로_거부된다() {
        Long courseId = createCourse(1);
        Long waiter = createMember("wl-const-waiter@example.com");
        Long filler = createMember("wl-const-filler@example.com");
        Long intruder = createMember("wl-const-intruder@example.com");

        receiptService.receiveEnrollment(filler, courseId);
        receiptService.receiveEnrollment(waiter, courseId);
        worker.drainQueue();

        WaitlistEntry existing = waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(waiter, courseId, WaitlistStatus.WAITING).orElseThrow();
        assertThat(existing.getPosition()).isEqualTo(1L);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO waitlist_entry (member_id, course_id, position, status, created_at) "
                        + "VALUES (?, ?, 1, 'WAITING', ?)",
                intruder, courseId, LocalDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
