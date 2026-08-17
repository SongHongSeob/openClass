package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.course.CourseService;
import com.hongseob.openclass_ap.course.dto.CourseUpdateRequest;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequest;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.request.RequestState;
import com.hongseob.openclass_ap.enrollment.request.RequestType;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.hongseob.openclass_ap.waitlist.WaitlistEntry;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import com.hongseob.openclass_ap.waitlist.WaitlistStatus;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code CAPACITY_INCREASE} 요청의 큐 적재·워커 디스패치 통합 테스트(서비스/
 * 워커 계층, HTTP 경유하지 않음) — AC-ENR-041(큐 경유 적재), AC-ENR-042(대기자
 * 일괄 승격), AC-ENR-043(대기자 없는 증설 NOOP), AC-ENR-051(부적격 대기자
 * 건너뛰기), AC-ENR-053(마감 강좌 승격 동결 + 재개 후 정상 승격) (M5).
 *
 * <p>정원 증설은 {@code course.CourseAdminController}가 아니라 그 아래
 * {@link CourseService#update}를 직접 호출해 재현한다 — AC-ENR-04x는 관리자
 * API 인가(AC-ADM-002, {@code SPEC-COURSE-001} 소관)가 아니라 큐 적재·워커
 * 디스패치 동작을 검증 대상으로 하므로, HTTP 계층을 경유하지 않는 M4 형제
 * 테스트({@code EnrollmentCancelWorkerDispatchIntegrationTest})와 동일한
 * 패턴을 따른다.</p>
 */
@SpringBootTest
class EnrollmentCapacityIncreaseWorkerDispatchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

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
        return courseRepository.save(Course.create("증설워커강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();
    }

    private Long createMember(String email) {
        return memberRepository.save(Member.createMember(email, "hash", "회원")).getId();
    }

    /** {@link CourseService#update}를 직접 호출해 관리자 정원 변경을 재현한다. */
    private void updateCapacity(Long courseId, int newCapacity) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        CourseUpdateRequest request = new CourseUpdateRequest(course.getTitle(), course.getDescription(),
                newCapacity, course.getStartsAt(), course.getEndsAt());
        courseService.update(courseId, request);
    }

    private List<EnrollmentRequest> capacityIncreaseRequests() {
        return requestRepository.findAll().stream()
                .filter(r -> r.getRequestType() == RequestType.CAPACITY_INCREASE)
                .sorted(Comparator.comparing(EnrollmentRequest::getId))
                .toList();
    }

    // AC-ENR-041 — 정원 증설은 큐를 경유하고, 워커를 구동하지 않으면 확정
    // 인원·대기자가 그대로 유지된다.
    @Test
    void 정원_증설은_큐를_경유하며_워커를_구동하지_않으면_확정인원과_대기자가_그대로다() {
        Long courseId = createCourse(2);
        Long a = createMember("adx041-a@example.com");
        Long b = createMember("adx041-b@example.com");
        Long c = createMember("adx041-c@example.com");
        Long d = createMember("adx041-d@example.com");

        receiptService.receiveEnrollment(a, courseId);
        receiptService.receiveEnrollment(b, courseId);
        receiptService.receiveEnrollment(c, courseId);
        receiptService.receiveEnrollment(d, courseId);
        worker.drainQueue();

        updateCapacity(courseId, 4);

        Course reloaded = courseRepository.findById(courseId).orElseThrow();
        assertThat(reloaded.getCapacity()).isEqualTo(4);
        assertThat(reloaded.getEnrolledCount()).isEqualTo(2);

        List<EnrollmentRequest> capacityRequests = capacityIncreaseRequests();
        assertThat(capacityRequests).hasSize(1);
        assertThat(capacityRequests.get(0).getState()).isEqualTo(RequestState.PENDING);
        assertThat(capacityRequests.get(0).getResult()).isNull();

        assertThat(waitlistEntryRepository.findAll())
                .allMatch(entry -> entry.getStatus() == WaitlistStatus.WAITING);
        assertThat(enrollmentRepository.findAll().stream()
                        .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED).count())
                .isEqualTo(2L);
    }

    // AC-ENR-042 — 워커 구동 시 대기 2명이 순번 오름차순으로 확정되어
    // enrolled_count가 정원과 같아진다.
    @Test
    void 워커_구동시_대기자가_순번대로_승격되어_enrolled_count가_정원과_같아진다() {
        Long courseId = createCourse(2);
        Long a = createMember("adx042-a@example.com");
        Long b = createMember("adx042-b@example.com");
        Long c = createMember("adx042-c@example.com");
        Long d = createMember("adx042-d@example.com");

        receiptService.receiveEnrollment(a, courseId);
        receiptService.receiveEnrollment(b, courseId);
        receiptService.receiveEnrollment(c, courseId);
        receiptService.receiveEnrollment(d, courseId);
        worker.drainQueue();

        updateCapacity(courseId, 4);
        worker.drainQueue();

        List<EnrollmentRequest> capacityRequests = capacityIncreaseRequests();
        assertThat(capacityRequests).hasSize(1);
        assertThat(capacityRequests.get(0).getResult()).isEqualTo(RequestResult.PROMOTED);

        Course reloaded = courseRepository.findById(courseId).orElseThrow();
        assertThat(reloaded.getEnrolledCount()).isEqualTo(4);
        assertThat(reloaded.getEnrolledCount()).isEqualTo(reloaded.getCapacity());

        WaitlistEntry cEntry = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(c)).findFirst().orElseThrow();
        WaitlistEntry dEntry = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(d)).findFirst().orElseThrow();
        assertThat(cEntry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(dEntry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(cEntry.getPosition()).isLessThan(dEntry.getPosition());

        long enrolledRowCount = enrollmentRepository.findAll().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED).count();
        assertThat(enrolledRowCount).isEqualTo(4L);
    }

    // AC-ENR-043 — 활성 대기자가 없는 강좌에서 정원 증설 → NOOP, 확정 인원 불변.
    @Test
    void 대기자가_없는_정원_증설은_NOOP이고_새_확정행이_생성되지_않는다() {
        Long courseId = createCourse(3);
        Long a = createMember("adx043-a@example.com");

        receiptService.receiveEnrollment(a, courseId);
        worker.drainQueue();

        updateCapacity(courseId, 5);
        worker.drainQueue();

        List<EnrollmentRequest> capacityRequests = capacityIncreaseRequests();
        assertThat(capacityRequests).hasSize(1);
        assertThat(capacityRequests.get(0).getResult()).isEqualTo(RequestResult.NOOP);

        assertThat(enrollmentRepository.count()).isEqualTo(1);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(1);
    }

    // AC-ENR-051 — 부적격 대기자(이미 유효한 확정 보유)는 건너뛰어 DUPLICATE로
    // 종결되고, 나머지 대기자가 순번대로 승격된다.
    @Test
    void 부적격_대기자는_DUPLICATE로_건너뛰고_나머지_대기자가_순번대로_승격된다() {
        Long courseId = createCourse(2);
        Long a = createMember("adx051-a@example.com");
        Long b = createMember("adx051-b@example.com");
        Long c = createMember("adx051-c@example.com"); // 이미 X에 유효한 확정 보유(부적격) + 대기 순번 1
        Long d = createMember("adx051-d@example.com"); // 대기 순번 2
        Long e = createMember("adx051-e@example.com"); // 대기 순번 3

        receiptService.receiveEnrollment(a, courseId);
        receiptService.receiveEnrollment(b, courseId);
        worker.drainQueue();

        // C는 REQ-WRK-007의 3번째 중복 검사 도입 이전 상태를 재현한다(M4
        // AC-ENR-044 테스트와 동일한 재현 패턴) — 이미 확정을 보유한 채로
        // 대기 항목도 직접 INSERT해 부적격 상태를 만든다.
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
        jdbcTemplate.update(
                "INSERT INTO waitlist_entry (member_id, course_id, position, status, created_at) "
                        + "VALUES (?, ?, 3, 'WAITING', ?)",
                e, courseId, LocalDateTime.now());

        updateCapacity(courseId, 4);
        worker.drainQueue();

        WaitlistEntry cEntry = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(c) && entry.getPosition() == 1L)
                .findFirst().orElseThrow();
        assertThat(cEntry.getStatus()).isEqualTo(WaitlistStatus.DUPLICATE);
        long cEnrolledRowCount = enrollmentRepository.findAll().stream()
                .filter(en -> en.getMemberId().equals(c) && en.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
        assertThat(cEnrolledRowCount).isEqualTo(1L); // 기존 1건만 존재, 중복 추가 없음

        WaitlistEntry dEntry = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(d)).findFirst().orElseThrow();
        WaitlistEntry eEntry = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(e)).findFirst().orElseThrow();
        assertThat(dEntry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(eEntry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);

        Course reloaded = courseRepository.findById(courseId).orElseThrow();
        assertThat(reloaded.getEnrolledCount()).isEqualTo(4);

        List<EnrollmentRequest> capacityRequests = capacityIncreaseRequests();
        assertThat(capacityRequests).hasSize(1);
        assertThat(capacityRequests.get(0).getResult()).isEqualTo(RequestResult.PROMOTED);
    }

    // AC-ENR-053 — 마감 강좌에서의 정원 증설: 승격 없음. 이어서 강좌를 다시
    // OPEN으로 되돌리고 정원 증설을 재요청하면 대기자가 정상 승격된다(대기명단이
    // 마감으로 파괴되지 않았음을 확인).
    @Test
    void 마감_강좌에서_정원_증설은_승격_없이_CLOSED로_종결되고_재개_후에는_정상_승격된다() {
        Long courseId = createCourse(2);
        Long a = createMember("adx053-a@example.com");
        Long b = createMember("adx053-b@example.com");
        Long c = createMember("adx053-c@example.com");
        Long d = createMember("adx053-d@example.com");

        receiptService.receiveEnrollment(a, courseId);
        receiptService.receiveEnrollment(b, courseId);
        receiptService.receiveEnrollment(c, courseId);
        receiptService.receiveEnrollment(d, courseId);
        worker.drainQueue();

        Course course = courseRepository.findById(courseId).orElseThrow();
        course.close();
        courseRepository.save(course);

        WaitlistEntry cEntryBefore = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(c)).findFirst().orElseThrow();
        WaitlistEntry dEntryBefore = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(d)).findFirst().orElseThrow();

        updateCapacity(courseId, 4);
        worker.drainQueue();

        List<EnrollmentRequest> firstRoundRequests = capacityIncreaseRequests();
        assertThat(firstRoundRequests).hasSize(1);
        assertThat(firstRoundRequests.get(0).getResult()).isEqualTo(RequestResult.CLOSED);

        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(2);
        assertThat(enrollmentRepository.findAll())
                .noneMatch(en -> (en.getMemberId().equals(c) || en.getMemberId().equals(d))
                        && en.getStatus() == EnrollmentStatus.ENROLLED);

        WaitlistEntry cEntryAfter = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(c)).findFirst().orElseThrow();
        WaitlistEntry dEntryAfter = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(d)).findFirst().orElseThrow();
        assertThat(cEntryAfter.getStatus()).isEqualTo(cEntryBefore.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(cEntryAfter.getPosition()).isEqualTo(cEntryBefore.getPosition());
        assertThat(dEntryAfter.getStatus()).isEqualTo(dEntryBefore.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(dEntryAfter.getPosition()).isEqualTo(dEntryBefore.getPosition());

        // 대기명단이 마감으로 파괴되지 않았음을 확인 — 강좌를 다시 OPEN으로
        // 되돌린 뒤(이 SPEC 범위에는 재개 API가 없으므로 테스트 셋업으로
        // 직접 전이한다) 정원 증설을 재요청하면 대기자가 정상 승격된다.
        jdbcTemplate.update("UPDATE course SET status = 'OPEN' WHERE id = ?", courseId);

        updateCapacity(courseId, 5);
        worker.drainQueue();

        List<EnrollmentRequest> secondRoundRequests =
                capacityIncreaseRequests().stream()
                        .filter(r -> r.getState() == RequestState.DONE)
                        .toList();
        assertThat(secondRoundRequests).hasSize(2); // 1차(CLOSED) + 2차(PROMOTED)
        assertThat(secondRoundRequests.get(1).getResult()).isEqualTo(RequestResult.PROMOTED);

        WaitlistEntry cEntryReopened = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(c)).findFirst().orElseThrow();
        WaitlistEntry dEntryReopened = waitlistEntryRepository.findAll().stream()
                .filter(entry -> entry.getMemberId().equals(d)).findFirst().orElseThrow();
        assertThat(cEntryReopened.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(dEntryReopened.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(4);
    }
}
