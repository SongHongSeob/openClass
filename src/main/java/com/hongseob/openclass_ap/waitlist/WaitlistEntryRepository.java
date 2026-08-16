package com.hongseob.openclass_ap.waitlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@link WaitlistEntry} 저장소.
 */
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    /**
     * 다음 대기 순번을 계산한다 — REQ-WL-001 / 2회차 감사 후속 N5. 강좌 전체
     * 이력(종단 상태 {@code PROMOTED}·{@code CANCELLED}·{@code DUPLICATE} 항목
     * 포함)에서 관측된 최대 순번 + 1이며, {@code status} 필터가 없는 것이 이
     * 쿼리의 핵심이다 — 현재 활성 항목의 개수(COUNT)로 계산하면 승격 직후 순번이
     * 충돌해 부분 유니크 인덱스 위반으로 트랜잭션이 롤백되고 큐 선두가 영구히
     * 막힌다(design.md §4.3 근거).
     */
    @Query("SELECT COALESCE(MAX(w.position), 0) + 1 FROM WaitlistEntry w WHERE w.courseId = :courseId")
    long nextPosition(@Param("courseId") Long courseId);
}
