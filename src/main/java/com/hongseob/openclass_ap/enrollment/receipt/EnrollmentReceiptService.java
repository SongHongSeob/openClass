package com.hongseob.openclass_ap.enrollment.receipt;

import com.hongseob.openclass_ap.common.exception.CourseNotFoundException;
import com.hongseob.openclass_ap.common.exception.EnrollmentNotFoundException;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.EnrollmentRepository;
import com.hongseob.openclass_ap.enrollment.EnrollmentStatus;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequest;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수강신청 접수 — 큐 적재만 수행한다(REQ-QUE-001/002). 정원 검사·확정 처리는
 * 절대 하지 않는다 — 그것은 워커의 유일한 책임이다(REQ-WRK-001/002, plan.md
 * §G 안티패턴 1번).
 *
 * <p><b>이 클래스가 이 SPEC의 핵심이다</b>(spec.md §A.2 / plan.md §C.2 / design.md
 * §3). PostgreSQL 시퀀스는 INSERT 시점에 비트랜잭션적으로 할당되지만 행의
 * 가시성은 커밋 시점에 결정되므로, 잠금 없이 큐 행을 적재하면 순서값 순서와
 * 커밋 순서가 어긋날 수 있다(감사 D1, research.md §2). 그래서 {@link
 * #receiveEnrollment}는 큐 행 INSERT(순서값 할당)에 앞서 강좌 단위 접수 잠금
 * ({@code pg_advisory_xact_lock})을 반드시 먼저 획득한다 — 트랜잭션 범위
 * 잠금이므로 커밋·롤백 어느 쪽으로 끝나든 자동 해제되며 해제 누락이 구조적으로
 * 발생하지 않는다.</p>
 */
@Service
public class EnrollmentReceiptService {

    /**
     * 접수 잠금의 권고 잠금 네임스페이스(classid). 강좌 식별자(objid)와 조합해
     * {@code pg_advisory_xact_lock(classid, objid)} 2-인자 형태로 호출한다 —
     * 이 프로젝트의 다른 권고 잠금 용도와 충돌하지 않도록 분리된 고정 상수다
     * (design.md §3, plan.md §D 제약). 이 시점 기준 이 프로젝트에 다른 권고
     * 잠금 사용처는 없지만, 향후 추가될 경우 반드시 다른 classid를 사용해야
     * 한다.
     */
    private static final int ENROLLMENT_LOCK_CLASSID = 887711;

    private final CourseRepository courseRepository;
    private final EnrollmentRequestRepository requestRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EnrollmentLockProperties lockProperties;

    public EnrollmentReceiptService(CourseRepository courseRepository,
            EnrollmentRequestRepository requestRepository, EnrollmentRepository enrollmentRepository,
            JdbcTemplate jdbcTemplate, EnrollmentLockProperties lockProperties) {
        this.courseRepository = courseRepository;
        this.requestRepository = requestRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.lockProperties = lockProperties;
    }

    /**
     * 수강신청을 접수한다. 존재하지 않는 강좌면 {@link CourseNotFoundException}을
     * 던져 큐 행을 적재하지 않는다(REQ-QUE-005, AC-ENR-003). 인증되지 않은
     * 요청은 이 메서드에 도달하기 전에 {@code SecurityConfig}가 401로 차단한다
     * (REQ-QUE-006, AC-ENR-002).
     *
     * @return 생성된 큐 행의 순서값(요청 식별자)
     */
    @Transactional
    public Long receiveEnrollment(Long memberId, Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }

        if (lockProperties.lockEnabled()) {
            // AC-ENR-007 대조군에서만 false — 잠금을 끄면 아래 순서 보장이
            // 실제로 깨진다는 것을 증명한다.
            //
            // pg_advisory_xact_lock의 2-인자 형태는 두 인자 모두 32비트
            // int다(PostgreSQL 문서 — 1-인자 형태만 64비트 bigint를 받는다).
            // courseId(Long/bigint)를 그대로 바인딩하면 "함수가 존재하지
            // 않음"으로 거부되므로 intValue()로 명시 변환한다 — 이 강좌
            // 카탈로그 규모에서 course id가 int 범위를 넘는 일은 없다.
            jdbcTemplate.queryForObject("SELECT pg_advisory_xact_lock(?, ?)", Object.class,
                    ENROLLMENT_LOCK_CLASSID, courseId.intValue());
        }

        // @MX:ANCHOR: [AUTO] 접수 잠금 획득이 큐 행 INSERT(순서값 할당)보다 반드시 선행해야 한다
        // @MX:REASON: spec.md §A.2 / design.md §3 — 이 순서가 뒤바뀌면 "순서값 순서 = 커밋 순서" 정합성 논증이 성립하지 않고 선착순 보장이 조용히 사라진다(감사 D1). AC-ENR-005(구조)·AC-ENR-006(핵심 시나리오)·AC-ENR-007(대조군)이 이 순서를 기계적으로 검증한다.
        EnrollmentRequest request = EnrollmentRequest.createEnroll(memberId, courseId);
        EnrollmentRequest saved = requestRepository.save(request);
        return saved.getId();
    }

    /**
     * 확정 수강신청 취소를 접수한다(M4, REQ-CNL-001). 취소·{@code
     * enrolled_count} 감소·승격은 절대 하지 않는다(REQ-WRK-002, plan.md §G
     * 안티패턴) — 이 메서드는 큐 적재만 수행한다.
     *
     * <p>1차 소유권 검증(REQ-CNL-002, 감사 D12)을 이 메서드가 수행한다 —
     * 존재하지 않거나 본인 소유가 아니거나 이미 취소된 대상이면 {@link
     * EnrollmentNotFoundException}을 던지고 {@code CANCEL} 큐 행을 적재하지
     * 않는다(AC-ENR-036 "큐 행이 생성되지 않는다"). 이 검증은 {@link
     * EnrollmentRepository#findCourseIdByIdAndMemberIdAndStatus} 프로젝션
     * 조회로 수행한다 — 아키텍처 경계 규칙(AC-ENR-009)이 워커 패키지로
     * 한정한 {@code Enrollment} 엔티티 자체는 이 패키지에서 참조할 수
     * 없으므로, courseId(접수 잠금 키)만 원시 값으로 돌려받는다.</p>
     *
     * @return 생성된 {@code CANCEL} 큐 행의 순서값(요청 식별자)
     */
    @Transactional
    public Long receiveCancel(Long memberId, Long enrollmentId) {
        Long courseId = enrollmentRepository
                .findCourseIdByIdAndMemberIdAndStatus(enrollmentId, memberId, EnrollmentStatus.ENROLLED)
                .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        if (lockProperties.lockEnabled()) {
            jdbcTemplate.queryForObject("SELECT pg_advisory_xact_lock(?, ?)", Object.class,
                    ENROLLMENT_LOCK_CLASSID, courseId.intValue());
        }

        // @MX:ANCHOR: [AUTO] 접수 잠금 획득이 CANCEL 큐 행 INSERT(순서값 할당)보다 반드시 선행해야 한다
        // @MX:REASON: REQ-QUE-003(2회차 감사 후속 N2) — 접수 잠금은 ENROLL뿐 아니라 CANCEL·CAPACITY_INCREASE 적재 경로 전부에 동일하게 적용된다. 이 순서가 뒤바뀌면 AC-ENR-005가 검증하는 "잠금 없이 큐 행을 INSERT하는 프로덕션 경로 0건" 조건이 CANCEL 행에서 깨진다.
        EnrollmentRequest request = EnrollmentRequest.createCancel(memberId, courseId, enrollmentId);
        EnrollmentRequest saved = requestRepository.save(request);
        return saved.getId();
    }

    /**
     * 관리자 정원 증설을 접수한다(M5, REQ-ADX-001). {@code
     * course.CourseService#update}가 정원이 실제로 증가했을 때만(신규 정원 &gt;
     * 이전 정원) 호출한다 — 무변경·축소 갱신에서는 호출되지 않는다. 승격 처리는
     * 이 메서드가 절대 하지 않는다(plan.md §G 안티패턴 "정원 증설을 관리자
     * API에서 직접 승격 처리") — 큐 적재만 수행하고 즉시 반환한다.
     *
     * <p>{@code CAPACITY_INCREASE} 요청은 특정 회원에 귀속되지 않으므로 {@link
     * EnrollmentRequest#createCapacityIncrease}가 {@code member_id}를 NULL로
     * 적재한다.</p>
     *
     * @return 생성된 {@code CAPACITY_INCREASE} 큐 행의 순서값(요청 식별자)
     */
    @Transactional
    public Long receiveCapacityIncrease(Long courseId) {
        if (lockProperties.lockEnabled()) {
            jdbcTemplate.queryForObject("SELECT pg_advisory_xact_lock(?, ?)", Object.class,
                    ENROLLMENT_LOCK_CLASSID, courseId.intValue());
        }

        // @MX:ANCHOR: [AUTO] 접수 잠금 획득이 CAPACITY_INCREASE 큐 행 INSERT(순서값 할당)보다 반드시 선행해야 한다
        // @MX:REASON: REQ-QUE-003(2회차 감사 후속 N2) — ENROLL·CANCEL과 동일하게 CAPACITY_INCREASE 적재도 접수 잠금을 거쳐야 AC-ENR-005의 "잠금 없이 큐 행을 INSERT하는 프로덕션 경로 0건" 조건을 만족한다.
        EnrollmentRequest request = EnrollmentRequest.createCapacityIncrease(courseId);
        EnrollmentRequest saved = requestRepository.save(request);
        return saved.getId();
    }
}
