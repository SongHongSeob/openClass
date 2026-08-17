package com.hongseob.openclass_ap.waitlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대기명단 항목 엔티티 (spec.md §A.3 "대기명단 항목", plan.md §C.1, design.md §1).
 *
 * <p>{@code (course_id, position) WHERE status='WAITING'}·{@code (member_id,
 * course_id) WHERE status='WAITING'} 부분 유니크 인덱스 2종은 {@code schema.sql}이
 * 생성한다(INV-ENR-004, INV-ENR-009).</p>
 *
 * <p>순번 부여 규칙(REQ-WL-001, 2회차 감사 후속 N5)은
 * {@link WaitlistEntryRepository#nextPosition}이 강좌 전체 이력(종단 상태 포함)의
 * 최대 순번 + 1로 계산한다 — 활성 항목 개수 기반({@code COUNT(활성)+1})이
 * 아니다. M1은 정원 초과 시 대기 등록 1건만 만든다({@link
 * com.hongseob.openclass_ap.enrollment.worker.EnrollmentRequestProcessor}) —
 * 취소({@link #WAITING} → {@link WaitlistStatus#CANCELLED})·승격({@link #WAITING}
 * → {@link WaitlistStatus#PROMOTED}/{@link WaitlistStatus#DUPLICATE}) 전이
 * 메서드는 M4가 추가한다(plan.md §F M4).</p>
 */
@Entity
@Table(name = "waitlist_entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitlistStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    private WaitlistEntry(Long memberId, Long courseId, Long position) {
        this.memberId = memberId;
        this.courseId = courseId;
        this.position = position;
        this.status = WaitlistStatus.WAITING;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 대기명단 등록의 유일한 진입점. 항상 {@link WaitlistStatus#WAITING}으로
     * 시작한다. {@code position}은 호출부가 {@link WaitlistEntryRepository#nextPosition}
     * 결과를 그대로 전달해야 한다 — 이 클래스 자체는 부여 규칙을 재계산하지
     * 않는다(단일 책임: 저장소가 규칙을, 엔티티가 불변식을 보존).
     */
    public static WaitlistEntry create(Long memberId, Long courseId, Long position) {
        return new WaitlistEntry(memberId, courseId, position);
    }

    /**
     * 대기자 본인의 대기 신청 취소(REQ-WL-007, {@code WAITING → CANCELLED}).
     * {@code waitlist.WaitlistService}가 소유권 검증 후 호출한다(M4). 뒤 순번
     * 대기자들의 {@code position} 값은 건드리지 않는다 — 상대 순서가 그대로
     * 보존된다(AC-ENR-033).
     */
    public void cancel() {
        this.status = WaitlistStatus.CANCELLED;
    }

    /**
     * 승격 완료 전이({@code WAITING → PROMOTED}, REQ-WL-003/REQ-ADX-002).
     * 워커의 승격 헬퍼({@code promoteNextEligible})만 호출한다(M4).
     */
    public void promote() {
        this.status = WaitlistStatus.PROMOTED;
        this.promotedAt = LocalDateTime.now();
    }

    /**
     * 승격 부적격 판정 후 종결({@code WAITING → DUPLICATE}, REQ-WL-009).
     * 다시 대기 선두에 남지 않도록 종단 처리한다 — 이 메서드를 호출하는
     * 쪽은 예외를 던지지 않고 다음 대기자로 건너뛴다(design.md §4.3,
     * AC-ENR-044의 큐 생존성 요구).
     */
    public void markDuplicate() {
        this.status = WaitlistStatus.DUPLICATE;
    }
}
