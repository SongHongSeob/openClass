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
 * 동일 회원 중복 대기 거부 2계층 통합 테스트 — AC-ENR-050 (M4, 2차 감사 E1,
 * INV-ENR-009).
 */
@SpringBootTest
class WaitlistDuplicatePreventionIntegrationTest extends AbstractIntegrationTest {

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
    private WaitlistService waitlistService;

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
        return courseRepository.save(Course.create("중복대기강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();
    }

    private Long createMember(String email) {
        return memberRepository.save(Member.createMember(email, "hash", "회원")).getId();
    }

    // AC-ENR-050 — (i) 애플리케이션 계층: 이미 활성 대기자인 회원의 재신청은 REJECTED다.
    @Test
    void 이미_활성_대기자인_회원이_재신청하면_REJECTED이고_활성_대기_항목은_여전히_1건이다() {
        Long courseId = createCourse(1);
        Long filler = createMember("dup-wait-filler@example.com");
        Long member = createMember("dup-wait-member@example.com");

        receiptService.receiveEnrollment(filler, courseId);
        Long firstRequestId = receiptService.receiveEnrollment(member, courseId);
        worker.drainQueue();
        assertThat(requestRepository.findById(firstRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.WAITLISTED);
        WaitlistEntry originalEntry = waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(member, courseId, WaitlistStatus.WAITING).orElseThrow();

        Long secondRequestId = receiptService.receiveEnrollment(member, courseId);
        worker.drainQueue();

        assertThat(requestRepository.findById(secondRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.REJECTED);
        long activeWaitCount = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(member) && entry.getStatus() == WaitlistStatus.WAITING)
                .count();
        assertThat(activeWaitCount).isEqualTo(1L);
        assertThat(waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(member, courseId, WaitlistStatus.WAITING).orElseThrow()
                .getPosition())
                .as("새 순번이 부여되지 않고 기존 항목 그대로여야 한다")
                .isEqualTo(originalEntry.getPosition());
    }

    // AC-ENR-050 — (ii) DB 최종 방어선: 애플리케이션 계층을 우회한 직접 INSERT는 제약 위반으로 거부된다.
    @Test
    void 애플리케이션_계층을_우회한_동일_회원_동일_강좌_활성_대기_직접_INSERT는_DB_제약으로_거부된다() {
        Long courseId = createCourse(1);
        Long filler = createMember("dup-wait-db-filler@example.com");
        Long member = createMember("dup-wait-db-member@example.com");

        receiptService.receiveEnrollment(filler, courseId);
        receiptService.receiveEnrollment(member, courseId);
        worker.drainQueue();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO waitlist_entry (member_id, course_id, position, status, created_at) "
                        + "VALUES (?, ?, 999, 'WAITING', ?)",
                member, courseId, LocalDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // AC-ENR-050 — (iii) 대기 취소 후 재신청은 정상 허용된다(WHERE status='WAITING' 필터의 의도).
    @Test
    void 대기를_취소한_뒤_재신청하면_정상적으로_접수된다() {
        Long courseId = createCourse(1);
        Long filler = createMember("dup-wait-reapply-filler@example.com");
        Long member = createMember("dup-wait-reapply-member@example.com");

        receiptService.receiveEnrollment(filler, courseId);
        Long firstRequestId = receiptService.receiveEnrollment(member, courseId);
        worker.drainQueue();
        assertThat(requestRepository.findById(firstRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.WAITLISTED);

        WaitlistEntry entry = waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(member, courseId, WaitlistStatus.WAITING).orElseThrow();
        waitlistService.cancel(entry.getId(), member);

        Long secondRequestId = receiptService.receiveEnrollment(member, courseId);
        worker.drainQueue();

        assertThat(requestRepository.findById(secondRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.WAITLISTED);
        assertThat(waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(member, courseId, WaitlistStatus.WAITING))
                .as("재신청으로 새 활성 대기 항목이 생성되어야 한다")
                .isPresent();
    }
}
