package com.hongseob.openclass_ap.enrollment.request;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import org.hibernate.annotations.ColumnDefault;

/**
 * 큐 테이블 엔티티 (spec.md §A.3 "신청 요청" / §A.4.1, plan.md §C.1, design.md §1).
 *
 * <p>이 행의 {@code id}(BIGSERIAL)가 곧 접수 순서값이다. 값 자체는 단조
 * 증가하지만, 그것만으로는 선착순이 보장되지 않는다(spec.md §A.2 — 시퀀스
 * 할당은 비트랜잭션적이고 행의 가시성은 커밋 시점에 결정되므로 순서값 순서와
 * 커밋 순서가 어긋날 수 있다). 그래서 이 엔티티를 저장하는 유일한 프로덕션
 * 경로({@link com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService})는
 * INSERT(순서값 할당)에 앞서 강좌 단위 접수 잠금을 반드시 획득한다
 * (REQ-QUE-003, AC-ENR-005).</p>
 *
 * <p>CHECK 제약 4종은 spec.md §A.4.1이 규범적으로 정의한 큐 상태 도메인 전체를
 * DB 계층에서도 강제한다 — 애플리케이션 계층을 우회한 INSERT/UPDATE까지
 * 방어한다(INV-ENR-006, AC-ENR-004).</p>
 */
@Entity
@Table(name = "enrollment_request", indexes = {
        @Index(name = "ix_enrollment_request_state_id", columnList = "state, id")
})
@Checks({
        @Check(name = "ck_enrollment_request_type",
                constraints = "request_type IN ('ENROLL','CANCEL','CAPACITY_INCREASE')"),
        @Check(name = "ck_enrollment_request_state", constraints = "state IN ('PENDING','DONE')"),
        @Check(name = "ck_enrollment_request_result",
                constraints = "result IS NULL OR result IN "
                        + "('SUCCESS','WAITLISTED','CLOSED','REJECTED','FAILED','CANCELLED','PROMOTED','NOOP')"),
        @Check(name = "ck_enrollment_request_state_result",
                constraints = "(state = 'PENDING' AND result IS NULL) "
                        + "OR (state = 'DONE' AND result IS NOT NULL)")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrollmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * {@code CAPACITY_INCREASE}(M5)는 특정 회원에 귀속되지 않는 관리자 API
     * 적재 요청이므로 이 컬럼은 NULL을 허용한다(spec.md §A.4.1 "적재 주체:
     * 관리자 API"). {@code ENROLL}·{@code CANCEL}은 항상 값을 채운다.
     */
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /** {@code CANCEL} 요청이 취소 대상 확정 레코드를 가리킨다. {@code ENROLL}·
     * {@code CAPACITY_INCREASE}에서는 NULL이다(design.md §1.2). M1은 {@code ENROLL}만
     * 생성하므로 항상 NULL이다 — M4가 이 값을 채우는 경로를 추가한다. */
    @Column(name = "target_enrollment_id")
    private Long targetEnrollmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'PENDING'")
    private RequestState state;

    @Enumerated(EnumType.STRING)
    private RequestResult result;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    private EnrollmentRequest(Long memberId, Long courseId, Long targetEnrollmentId, RequestType requestType) {
        this.memberId = memberId;
        this.courseId = courseId;
        this.targetEnrollmentId = targetEnrollmentId;
        this.requestType = requestType;
        this.state = RequestState.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    /**
     * 접수 API가 사용하는 유일한 생성 진입점(REQ-QUE-002). {@code CANCEL}·
     * {@code CAPACITY_INCREASE} 생성 진입점은 각각 M4·M5가 추가한다(plan.md §F).
     */
    public static EnrollmentRequest createEnroll(Long memberId, Long courseId) {
        return new EnrollmentRequest(memberId, courseId, null, RequestType.ENROLL);
    }

    /**
     * 취소 API가 사용하는 생성 진입점(REQ-CNL-001, M4). {@code
     * targetEnrollmentId}가 취소 대상 확정 레코드를 가리킨다 — {@code
     * receipt.EnrollmentReceiptService#receiveCancel}이 1차 소유권 검증을
     * 마친 뒤에만 이 팩토리를 호출한다.
     */
    public static EnrollmentRequest createCancel(Long memberId, Long courseId, Long targetEnrollmentId) {
        return new EnrollmentRequest(memberId, courseId, targetEnrollmentId, RequestType.CANCEL);
    }

    /**
     * 관리자 정원 증설 API 경로가 사용하는 생성 진입점(REQ-ADX-001, M5). 특정
     * 회원이 접수하는 것이 아니므로 {@code memberId}는 NULL로 적재한다(§A.4.1
     * "적재 주체: 관리자 API"). {@code
     * com.hongseob.openclass_ap.course.CourseService#update}가 정원이 실제로
     * 증가했을 때만(신규 정원 &gt; 이전 정원) {@code
     * receipt.EnrollmentReceiptService#receiveCapacityIncrease}를 거쳐 이
     * 팩토리를 호출한다 — 무변경·축소 갱신에서는 호출되지 않는다.
     */
    public static EnrollmentRequest createCapacityIncrease(Long courseId) {
        return new EnrollmentRequest(null, courseId, null, RequestType.CAPACITY_INCREASE);
    }

    /**
     * 워커가 처리를 종결하며 호출한다(REQ-WRK-010, spec.md §A.4.1 합법 전이
     * {@code PENDING → DONE}, 1회만 · INV-ENR-006). 이미 {@code DONE}인 요청에
     * 다시 호출하는 프로덕션 경로는 존재하지 않는다 — M1 스텁 워커
     * ({@link com.hongseob.openclass_ap.enrollment.worker.EnrollmentRequestProcessor})는
     * 항상 {@code state='PENDING'}으로 재확인한 행에만 이 메서드를 호출한다.
     * 재처리 멱등성의 전체 계약(AC-ENR-018)은 M2가 검증한다.
     */
    public void markDone(RequestResult result) {
        this.state = RequestState.DONE;
        this.result = result;
        this.processedAt = LocalDateTime.now();
    }
}
