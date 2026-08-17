package com.hongseob.openclass_ap.enrollment;

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
 * 확정 수강신청 엔티티 (spec.md §A.3 "확정 수강신청", plan.md §C.1, design.md §1).
 *
 * <p>이 행을 생성하는 코드 경로는 큐 워커 처리 경로 단 1개소여야 한다
 * (REQ-WRK-001, INV-ENR-002) — M1은 {@link #create}를 M1 스텁 워커
 * ({@link com.hongseob.openclass_ap.enrollment.worker.EnrollmentRequestProcessor})에서만
 * 호출한다. "이 진입점 외 확정 경로가 없다"를 매핑 제약 + ArchUnit + DB 상태
 * 단언 3층으로 검증하는 것은 M2 범위다(design.md §5, AC-ENR-008/009).</p>
 *
 * <p>{@code (member_id, course_id) WHERE status='ENROLLED'} 부분 유니크
 * 인덱스는 {@code schema.sql}이 생성한다 — Hibernate JPA 애노테이션으로는
 * WHERE 절을 가진 인덱스를 표현할 수 없다(INV-ENR-003).</p>
 */
@Entity
@Table(name = "enrollment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    private Enrollment(Long memberId, Long courseId) {
        this.memberId = memberId;
        this.courseId = courseId;
        this.status = EnrollmentStatus.ENROLLED;
        this.enrolledAt = LocalDateTime.now();
    }

    /**
     * 확정 수강신청 생성의 유일한 진입점. 항상 {@link EnrollmentStatus#ENROLLED}로
     * 시작한다(INV-ENR-002 — 이 클래스에 다른 상태로 시작하는 생성 경로가
     * 없다는 것 자체가 방어의 일부다). 취소 전이({@code cancel()})는 M4가
     * 추가한다(plan.md §F M4).
     */
    public static Enrollment create(Long memberId, Long courseId) {
        return new Enrollment(memberId, courseId);
    }

    /**
     * 확정 수강신청을 취소 상태로 전이한다(REQ-WL-003, spec.md §A.4.2 합법
     * 전이 {@code ENROLLED → CANCELLED}, 1회만). 워커의 {@code CANCEL} 처리
     * 경로에서만 호출된다 — INV-ENR-002가 확정 생성 경로를 워커 1개소로
     * 한정하는 것과 동일한 원칙으로, 이 전이도 워커 1개소에서만 일어난다
     * (plan.md §F M4).
     */
    public void cancel() {
        this.status = EnrollmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
}
