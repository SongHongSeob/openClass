package com.hongseob.openclass_ap.enrollment.worker;

import com.hongseob.openclass_ap.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@code course.enrolled_count} 증가 전용 게이트웨이 — design.md §7 패키지 구조
 * "Enrollment 생성·enrolled_count 변경은 여기서만"의 실제 구현체다.
 *
 * <p>{@link Course} 엔티티 자체에는 이 값을 바꾸는 세터·메서드가 의도적으로
 * 존재하지 않는다(course 패키지는 이 SPEC의 PRESERVE 대상 — {@code Course.java}
 * 클래스 주석 "변경 권한은 SPEC-ENROLLMENT-001의 큐 워커가 단독으로 갖는다"
 * 참고). 그래서 이 SPEC의 워커 패키지가 이 저장소를 통해 JPQL UPDATE로
 * {@code course} 패키지 파일을 전혀 수정하지 않고 원자적 정원 게이트(WHERE
 * enrolled_count &lt; capacity)를 직접 수행한다. 이 방식은 course 패키지
 * PRESERVE 경계를 지키면서 INV-ENR-002("확정 경로 = 워커 1개소")를 만족시키는
 * 방법이며, 동시에 {@code Course} 엔티티의 세터 부재라는 기존 설계를 그대로
 * 보존한다.</p>
 *
 * <p>단일 워커 인스턴스 전제(REQ-WRK-012)에서는 요청이 순차적으로 한 건씩
 * 처리되므로 이 WHERE 가드 없이도 경쟁 조건이 발생하지 않지만, DB 최종
 * 방어선(REQ-WRK-014)과 일관되게 방어적으로 유지한다.</p>
 *
 * <p>M1 범위: {@code ENROLL} 확정 시 증가만 지원한다. {@code CANCEL} 처리 시
 * 감소는 M4가, 정원 증설 시 반복 증가는 M5가 재사용한다(plan.md §F).</p>
 */
public interface CourseCapacityRepository extends JpaRepository<Course, Long> {

    @Modifying
    @Query("UPDATE Course c SET c.enrolledCount = c.enrolledCount + 1 "
            + "WHERE c.id = :courseId AND c.enrolledCount < c.capacity")
    int incrementEnrolledCountIfAvailable(@Param("courseId") Long courseId);
}
