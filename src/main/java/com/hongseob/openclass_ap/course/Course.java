package com.hongseob.openclass_ap.course;

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
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import org.hibernate.annotations.ColumnDefault;

/**
 * 강좌 엔티티 (plan.md §C.1 데이터 모델, §C.4.1-2 코드 규약).
 *
 * <p>이 프로젝트는 마이그레이션 도구가 없어 스키마가 전적으로 Hibernate
 * {@code ddl-auto}로 생성된다. CHECK 제약 3종은 평범한 JPA 애노테이션으로는
 * 생성되지 않으므로 {@link Check}로 명시한다 (plan.md §C.1.1).</p>
 *
 * <p><b>{@code enrolled_count}는 이 SPEC 범위의 어떤 코드 경로로도 변경되지
 * 않는다</b> (REQ-CRS-004 / INV-CRS-003). {@code member.Member}가 role 파라미터를 받는
 * 생성 경로를 아예 없애 권한 상승을 구조적으로 차단한 것과 동일한 이유로,
 * 이 클래스는 {@code enrolled_count}를 변경하는 세터·메서드를 어떤 이름으로도
 * 정의하지 않는다 — 정적 팩토리 {@link #create}가 파라미터로 받지 않고 항상 0
 * (DB {@code DEFAULT 0})으로 시작하는 것이 그 방어의 실체다. 변경 권한은
 * {@code SPEC-ENROLLMENT-001}의 큐 워커가 단독으로 갖는다.</p>
 *
 * <p><b>{@code enrolled_count} 필드에 {@code updatable = false}가 붙어 있는 이유</b>
 * (SPEC-ENROLLMENT-001 sync-audit F1): {@code @DynamicUpdate}가 없는 이 엔티티는
 * dirty 상태로 커밋되면 Hibernate가 updatable한 전 컬럼을 UPDATE SET 절에
 * 포함시킨다. 관리자가 {@link CourseService#update}로 이 엔티티를 로드해 제목만
 * 수정하는 동안, 워커({@code CourseCapacityRepository})가 별도 트랜잭션의 JPQL
 * 벌크 UPDATE로 {@code enrolled_count}를 증가시키고 먼저 커밋하면, 관리자
 * 트랜잭션의 커밋이 그 증가분을 로드 시점 스냅샷 값으로 덮어써 워커의 정원
 * 게이트(단일 관문 ②, AC-ENR-010)를 우회하는 과소 계상을 만들 수 있었다.
 * {@code updatable = false}는 이 필드를 엔티티 dirty-checking UPDATE의 SET 절에서
 * 구조적으로 제외한다 — {@code CourseCapacityRepository}의 JPQL 벌크 UPDATE는
 * 엔티티 dirty-checking을 거치지 않는 별도 경로이므로 영향받지 않는다.</p>
 */
@Entity
@Table(name = "course")
@Checks({
        @Check(name = "ck_course_capacity_min", constraints = "capacity >= 1"),
        @Check(name = "ck_course_enrolled_range", constraints = "enrolled_count >= 0 AND enrolled_count <= capacity"),
        @Check(name = "ck_course_status", constraints = "status IN ('OPEN','CLOSED')")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "enrolled_count", nullable = false, updatable = false)
    @ColumnDefault("0")
    private Integer enrolledCount;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'OPEN'")
    private CourseStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Course(String title, String description, Integer capacity, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.title = title;
        this.description = description;
        this.capacity = capacity;
        this.enrolledCount = 0;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = CourseStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 강좌 생성의 유일한 진입점. {@code enrolled_count}를 파라미터로 받지 않고
     * 항상 0으로 시작하며, 모집 상태는 항상 {@link CourseStatus#OPEN}으로
     * 시작한다 (REQ-ADM-003 / REQ-CRS-004 / INV-CRS-003).
     */
    public static Course create(String title, String description, Integer capacity,
            LocalDateTime startsAt, LocalDateTime endsAt) {
        return new Course(title, description, capacity, startsAt, endsAt);
    }

    /**
     * 관리자가 강좌명·설명·정원·일정을 수정한다(plan.md §F M3 / §C.4.1-2).
     * {@code enrolled_count}는 파라미터로도 받지 않고 이 메서드가 건드리는 필드
     * 목록에도 없다 — 정원 축소가 확정 인원 미만인지 여부는 이 메서드 호출
     * 이전에 서비스 계층이 검증을 마친 뒤에만 호출된다(plan.md §C.3).
     */
    public void updateDetails(String title, String description, Integer capacity,
            LocalDateTime startsAt, LocalDateTime endsAt) {
        this.title = title;
        this.description = description;
        this.capacity = capacity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /**
     * 강좌를 마감 상태로 전이한다. 물리 삭제 경로는 이 클래스에 존재하지 않으며,
     * "삭제" API도 내부적으로 이 메서드를 재사용한다(REQ-ADM-008, INV-CRS-004,
     * plan.md §C.1 설계 판단 4).
     */
    public void close() {
        this.status = CourseStatus.CLOSED;
    }
}
