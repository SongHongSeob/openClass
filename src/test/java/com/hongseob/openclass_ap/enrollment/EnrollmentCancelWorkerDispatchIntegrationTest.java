package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequest;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.hongseob.openclass_ap.waitlist.WaitlistEntry;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import com.hongseob.openclass_ap.waitlist.WaitlistStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code CANCEL} 요청의 워커 디스패치 통합 테스트(서비스/워커 계층, HTTP
 * 경유하지 않음) — AC-ENR-030(같은 트랜잭션 내 승격), AC-ENR-031(대기자 우선
 * 배정, 양방향 순서), AC-ENR-032(대기자 없는 취소), AC-ENR-037(워커 2차
 * 소유권 검증), AC-ENR-039(이미 취소/부재 대상), AC-ENR-040(취소 직후 재신청
 * 허용), AC-ENR-044(부적격 대기자 건너뛰기 + 큐 생존성), AC-ENR-052(마감
 * 강좌에서 취소 허용·승격 동결) (M4).
 */
@SpringBootTest
class EnrollmentCancelWorkerDispatchIntegrationTest extends AbstractIntegrationTest {

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
        return courseRepository.save(Course.create("취소워커강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();
    }

    private Long createMember(String email) {
        return memberRepository.save(Member.createMember(email, "hash", "회원")).getId();
    }

    private Long confirmedEnrollmentIdOf(Long memberId, Long courseId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM enrollment WHERE member_id = ? AND course_id = ? AND status = 'ENROLLED'",
                Long.class, memberId, courseId);
    }

    // AC-ENR-030 + INV-ENR-001
    @Test
    void 취소_처리는_같은_트랜잭션에서_대기_1순위를_승격시키고_enrolled_count가_2로_유지된다() {
        Long courseId = createCourse(2);
        Long a = createMember("dispatch-a@example.com");
        Long b = createMember("dispatch-b@example.com");
        Long c = createMember("dispatch-c@example.com");
        Long d = createMember("dispatch-d@example.com");

        receiptService.receiveEnrollment(a, courseId);
        receiptService.receiveEnrollment(b, courseId);
        receiptService.receiveEnrollment(c, courseId);
        receiptService.receiveEnrollment(d, courseId);
        worker.drainQueue();

        Long enrollmentA = confirmedEnrollmentIdOf(a, courseId);
        Long cancelRequestId = receiptService.receiveCancel(a, enrollmentA);
        worker.drainQueue();

        assertThat(requestRepository.findById(cancelRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.CANCELLED);
        assertThat(enrollmentRepository.findById(enrollmentA).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.CANCELLED);

        WaitlistEntry cEntry = waitlistEntryRepository
                .findAll().stream().filter(entry -> entry.getMemberId().equals(c)).findFirst().orElseThrow();
        assertThat(cEntry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(enrollmentRepository.findAll())
                .anyMatch(e -> e.getMemberId().equals(c) && e.getStatus() == EnrollmentStatus.ENROLLED);

        WaitlistEntry dEntry = waitlistEntryRepository
                .findAll().stream().filter(entry -> entry.getMemberId().equals(d)).findFirst().orElseThrow();
        assertThat(dEntry.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(dEntry.getPosition()).isEqualTo(2L);

        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(2);
        long activeEnrolledCount = enrollmentRepository.findAll().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED).count();
        assertThat(activeEnrolledCount).isEqualTo(2L);
    }

    // AC-ENR-031 — 정방향: 신규 신청 E가 CANCEL보다 먼저 접수(작은 순서값)되어도
    // 대기자 C가 우선 배정된다.
    @Test
    void 신규_신청이_취소보다_먼저_접수돼도_대기자가_우선_배정되고_신규_신청자가_추월하지_않는다() {
        Long courseId = createCourse(2);
        Long a = createMember("order-fwd-a@example.com");
        Long b = createMember("order-fwd-b@example.com");
        Long c = createMember("order-fwd-c@example.com");
        Long d = createMember("order-fwd-d@example.com");
        Long e = createMember("order-fwd-e@example.com");

        receiptService.receiveEnrollment(a, courseId);
        receiptService.receiveEnrollment(b, courseId);
        receiptService.receiveEnrollment(c, courseId);
        receiptService.receiveEnrollment(d, courseId);
        worker.drainQueue();

        // E가 먼저 큐에 적재된 뒤 A의 취소가 더 큰 순서값으로 적재된다.
        Long eRequestId = receiptService.receiveEnrollment(e, courseId);
        Long enrollmentA = confirmedEnrollmentIdOf(a, courseId);
        receiptService.receiveCancel(a, enrollmentA);
        worker.drainQueue();

        assertThat(requestRepository.findById(eRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.WAITLISTED);
        WaitlistEntry eEntry = waitlistEntryRepository
                .findAll().stream().filter(entry -> entry.getMemberId().equals(e)).findFirst().orElseThrow();
        assertThat(eEntry.getPosition()).isEqualTo(3L);

        assertThat(enrollmentRepository.findAll())
                .anyMatch(en -> en.getMemberId().equals(c) && en.getStatus() == EnrollmentStatus.ENROLLED);
        assertThat(enrollmentRepository.findAll())
                .noneMatch(en -> en.getMemberId().equals(e) && en.getStatus() == EnrollmentStatus.ENROLLED);
    }

    // AC-ENR-031 — 역방향: CANCEL이 신규 신청 E보다 먼저 접수(작은 순서값)돼도
    // 결과는 동일하다 — 여유 정원 노출 창이 존재하지 않기 때문이다.
    @Test
    void 취소가_신규_신청보다_먼저_접수돼도_결과가_동일하다() {
        Long courseId = createCourse(2);
        Long a = createMember("order-rev-a@example.com");
        Long b = createMember("order-rev-b@example.com");
        Long c = createMember("order-rev-c@example.com");
        Long d = createMember("order-rev-d@example.com");
        Long e = createMember("order-rev-e@example.com");

        receiptService.receiveEnrollment(a, courseId);
        receiptService.receiveEnrollment(b, courseId);
        receiptService.receiveEnrollment(c, courseId);
        receiptService.receiveEnrollment(d, courseId);
        worker.drainQueue();

        // A의 취소가 먼저 큐에 적재된 뒤 E가 더 큰 순서값으로 접수된다.
        Long enrollmentA = confirmedEnrollmentIdOf(a, courseId);
        receiptService.receiveCancel(a, enrollmentA);
        Long eRequestId = receiptService.receiveEnrollment(e, courseId);
        worker.drainQueue();

        assertThat(requestRepository.findById(eRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.WAITLISTED);
        WaitlistEntry eEntry = waitlistEntryRepository
                .findAll().stream().filter(entry -> entry.getMemberId().equals(e)).findFirst().orElseThrow();
        assertThat(eEntry.getPosition()).isEqualTo(3L);
        assertThat(enrollmentRepository.findAll())
                .anyMatch(en -> en.getMemberId().equals(c) && en.getStatus() == EnrollmentStatus.ENROLLED);
    }

    // AC-ENR-032
    @Test
    void 대기자가_없는_취소는_예외_없이_CANCELLED로_종결되고_enrolled_count만_감소한다() {
        Long courseId = createCourse(3);
        Long member = createMember("no-waiter@example.com");

        receiptService.receiveEnrollment(member, courseId);
        worker.drainQueue();
        Long enrollmentId = confirmedEnrollmentIdOf(member, courseId);

        Long cancelRequestId = receiptService.receiveCancel(member, enrollmentId);
        worker.drainQueue();

        assertThat(requestRepository.findById(cancelRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.CANCELLED);
        assertThat(enrollmentRepository.count()).isEqualTo(1);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isZero();
    }

    // AC-ENR-037 — 워커의 2차 소유권 검증. API 계층을 우회하여 소유자가 아닌
    // 회원 식별자로 CANCEL 큐 행을 직접 INSERT한다.
    @Test
    void 소유자가_아닌_회원으로_직접_INSERT된_CANCEL_요청은_워커가_REJECTED로_거부한다() {
        Long courseId = createCourse(3);
        Long owner = createMember("second-layer-owner@example.com");
        Long intruder = createMember("second-layer-intruder@example.com");

        receiptService.receiveEnrollment(owner, courseId);
        worker.drainQueue();
        Long enrollmentId = confirmedEnrollmentIdOf(owner, courseId);
        int enrolledCountBefore = courseRepository.findById(courseId).orElseThrow().getEnrolledCount();

        EnrollmentRequest forged = EnrollmentRequest.createCancel(intruder, courseId, enrollmentId);
        Long forgedId = requestRepository.save(forged).getId();

        worker.drainQueue();

        assertThat(requestRepository.findById(forgedId).orElseThrow().getResult())
                .isEqualTo(RequestResult.REJECTED);
        assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount())
                .isEqualTo(enrolledCountBefore);
        assertThat(waitlistEntryRepository.count()).isZero();
    }

    // AC-ENR-039
    @Test
    void 이미_취소된_확정에_대한_CANCEL_요청은_REJECTED이고_추가_감소나_승격이_없다() {
        Long courseId = createCourse(2);
        Long a = createMember("already-cancelled-a@example.com");
        Long b = createMember("already-cancelled-b@example.com");

        receiptService.receiveEnrollment(a, courseId);
        worker.drainQueue();
        Long enrollmentId = confirmedEnrollmentIdOf(a, courseId);

        receiptService.receiveCancel(a, enrollmentId);
        worker.drainQueue();
        assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.CANCELLED);
        int enrolledCountAfterFirstCancel = courseRepository.findById(courseId).orElseThrow().getEnrolledCount();

        // 애플리케이션 계층(API)은 이미 취소된 대상을 1차에서 걸러내므로,
        // 워커 2차 방어선을 직접 검증하기 위해 큐 행을 직접 INSERT한다.
        EnrollmentRequest duplicateCancel = EnrollmentRequest.createCancel(a, courseId, enrollmentId);
        Long duplicateCancelId = requestRepository.save(duplicateCancel).getId();

        worker.drainQueue();

        assertThat(requestRepository.findById(duplicateCancelId).orElseThrow().getResult())
                .isEqualTo(RequestResult.REJECTED);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount())
                .isEqualTo(enrolledCountAfterFirstCancel);
        assertThat(waitlistEntryRepository.count()).isZero();
    }

    // AC-ENR-040
    @Test
    void 취소_직후_재신청은_REJECTED가_아니며_이전_확정은_이력으로_보존된다() {
        Long courseId = createCourse(3);
        Long member = createMember("reapply@example.com");

        receiptService.receiveEnrollment(member, courseId);
        worker.drainQueue();
        Long firstEnrollmentId = confirmedEnrollmentIdOf(member, courseId);

        receiptService.receiveCancel(member, firstEnrollmentId);
        worker.drainQueue();

        Long secondRequestId = receiptService.receiveEnrollment(member, courseId);
        worker.drainQueue();

        assertThat(requestRepository.findById(secondRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.SUCCESS);
        assertThat(enrollmentRepository.findById(firstEnrollmentId).orElseThrow().getStatus())
                .as("취소된 이전 확정 행은 이력으로 보존된다")
                .isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(enrollmentRepository.findAll())
                .filteredOn(e -> e.getMemberId().equals(member) && e.getStatus() == EnrollmentStatus.ENROLLED)
                .hasSize(1);
    }

    // AC-ENR-044 — 승격 대상자의 중복 확정 방지 및 큐 생존성 (가장 중요한 M4 안전 계약).
    @Test
    void 부적격_대기자는_DUPLICATE로_종결되고_다음_적격_대기자가_승격되며_취소는_소실되지_않고_큐_선두가_막히지_않는다() {
        Long courseId = createCourse(2);
        Long a = createMember("liveness-a@example.com");
        Long b = createMember("liveness-b@example.com");
        Long c = createMember("liveness-c@example.com"); // 이미 X에 유효한 확정을 보유(부적격) + 대기 순번 1
        Long d = createMember("liveness-d@example.com"); // 대기 순번 2

        receiptService.receiveEnrollment(a, courseId);
        receiptService.receiveEnrollment(b, courseId);
        worker.drainQueue();

        // C는 REQ-WRK-007의 3번째 중복 검사 도입 이전 상태를 재현한다 — 이미
        // 확정을 보유한 채로 대기 항목도 직접 INSERT해 부적격 상태를 만든다.
        jdbcTemplate.update(
                "INSERT INTO enrollment (member_id, course_id, status, enrolled_at) VALUES (?, ?, 'ENROLLED', ?)",
                c, courseId, LocalDateTime.now());
        jdbcTemplate.update(
                "INSERT INTO waitlist_entry (member_id, course_id, position, status, created_at) "
                        + "VALUES (?, ?, 1, 'WAITING', ?)",
                c, courseId, LocalDateTime.now());
        jdbcTemplate.update(
                "INSERT INTO waitlist_entry (member_id, course_id, position, status, created_at) "
                        + "VALUES (?, ?, 2, 'WAITING', ?)",
                d, courseId, LocalDateTime.now());

        Long enrollmentA = confirmedEnrollmentIdOf(a, courseId);
        Long cancelRequestId = receiptService.receiveCancel(a, enrollmentA);
        worker.drainQueue();

        // 1) C에 대한 중복 확정 행이 생성되지 않고 DUPLICATE로 종결된다.
        WaitlistEntry cEntry = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(c) && entry.getPosition() == 1L)
                .findFirst().orElseThrow();
        assertThat(cEntry.getStatus()).isEqualTo(WaitlistStatus.DUPLICATE);
        long cEnrolledRowCount = enrollmentRepository.findAll().stream()
                .filter(e -> e.getMemberId().equals(c) && e.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
        assertThat(cEnrolledRowCount).isEqualTo(1L); // 기존 1건만 존재, 중복 추가 없음

        // 2) 여유 정원이 다음 적격 대기자 D에게 실제로 배정된다.
        WaitlistEntry dEntry = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(d)).findFirst().orElseThrow();
        assertThat(dEntry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(enrollmentRepository.findAll())
                .anyMatch(e -> e.getMemberId().equals(d) && e.getStatus() == EnrollmentStatus.ENROLLED);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(2);

        // 3) CANCEL 요청 자체의 결과는 CANCELLED다(FAILED가 아니다) — 취소가 소실되지 않는다.
        assertThat(requestRepository.findById(cancelRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.CANCELLED);

        // 4) 이어서 또 다른 CANCEL을 처리해도 큐 선두가 막히지 않고 정상 종결된다.
        Long enrollmentB = confirmedEnrollmentIdOf(b, courseId);
        Long secondCancelId = receiptService.receiveCancel(b, enrollmentB);
        worker.drainQueue();
        assertThat(requestRepository.findById(secondCancelId).orElseThrow().getResult())
                .isEqualTo(RequestResult.CANCELLED);
    }

    // AC-ENR-052 — 마감 강좌에서의 취소: 취소 허용·승격 동결.
    @Test
    void 마감_강좌에서_취소는_정상_수행되지만_대기자는_승격되지_않고_대기명단이_보존된다() {
        Long courseId = createCourse(2);
        Long a = createMember("closed-cancel-a@example.com");
        Long b = createMember("closed-cancel-b@example.com");
        Long c = createMember("closed-cancel-c@example.com");
        Long d = createMember("closed-cancel-d@example.com");

        receiptService.receiveEnrollment(a, courseId);
        receiptService.receiveEnrollment(b, courseId);
        receiptService.receiveEnrollment(c, courseId);
        receiptService.receiveEnrollment(d, courseId);
        worker.drainQueue();

        Long enrollmentA = confirmedEnrollmentIdOf(a, courseId);

        Course course = courseRepository.findById(courseId).orElseThrow();
        course.close();
        courseRepository.save(course);

        WaitlistEntry cEntryBefore = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(c)).findFirst().orElseThrow();
        WaitlistEntry dEntryBefore = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(d)).findFirst().orElseThrow();

        Long cancelRequestId = receiptService.receiveCancel(a, enrollmentA);
        worker.drainQueue();

        assertThat(requestRepository.findById(cancelRequestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.CANCELLED);
        assertThat(enrollmentRepository.findById(enrollmentA).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(1);

        WaitlistEntry cEntryAfter = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(c)).findFirst().orElseThrow();
        WaitlistEntry dEntryAfter = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(d)).findFirst().orElseThrow();
        assertThat(cEntryAfter.getStatus()).isEqualTo(cEntryBefore.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(cEntryAfter.getPosition()).isEqualTo(cEntryBefore.getPosition());
        assertThat(dEntryAfter.getStatus()).isEqualTo(dEntryBefore.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(dEntryAfter.getPosition()).isEqualTo(dEntryBefore.getPosition());
        assertThat(enrollmentRepository.findAll())
                .noneMatch(e -> (e.getMemberId().equals(c) || e.getMemberId().equals(d))
                        && e.getStatus() == EnrollmentStatus.ENROLLED);
    }
}
