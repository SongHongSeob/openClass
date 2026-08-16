package com.hongseob.openclass_ap.enrollment.request;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 큐 테이블 저장소. 클레임 쿼리 2종은 SPEC이 요구하는 {@code SELECT ... FOR UPDATE
 * SKIP LOCKED} 의미론(research.md §3, design.md §4.1-4.2)을 그대로 반영하기
 * 위해 네이티브 쿼리로 작성한다.
 *
 * <p><b>호출부 주의</b>: PostgreSQL은 읽기 전용으로 표시된 트랜잭션에서
 * {@code FOR UPDATE}를 거부할 수 있다. 이 저장소의 두 메서드는 반드시 쓰기
 * 가능한(readOnly가 아닌) {@code @Transactional} 경계 안에서 호출해야 한다 —
 * {@link com.hongseob.openclass_ap.enrollment.worker.EnrollmentRequestProcessor}가
 * 그 경계를 제공한다.</p>
 */
public interface EnrollmentRequestRepository extends JpaRepository<EnrollmentRequest, Long> {

    /**
     * 순서값 오름차순으로 최대 {@code limit}건의 미처리 요청 id를 클레임한다
     * (design.md §4.1 {@code claimNextBatch}). {@code SKIP LOCKED}는 v1의 단일
     * 워커 전제(REQ-WRK-012)에서는 실질적으로 무해하지만, 다중 워커 사고 대비로
     * 처음부터 적용한다(REQ-WRK-013, research.md §3).
     */
    @Query(value = "SELECT id FROM enrollment_request WHERE state = 'PENDING' "
            + "ORDER BY id ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Long> claimNextBatchIds(@Param("limit") int limit);

    /**
     * 요청 1건을 처리 대상으로 재확인·클레임한다(design.md §4.2 {@code processOne}
     * 1행). 이미 다른 트랜잭션에서 처리되어 {@code state='DONE'}이 되었으면 빈
     * 값을 반환한다 — 호출부가 이를 멱등 무동작으로 처리한다(REQ-WRK-011).
     */
    @Query(value = "SELECT * FROM enrollment_request WHERE id = :id AND state = 'PENDING' "
            + "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<EnrollmentRequest> findPendingForUpdateSkipLocked(@Param("id") Long id);

    /**
     * REQ-WRK-007 중복 검사 2번 — 동일 회원이 동일 강좌에 대해 이 요청 자신을
     * 제외한 미처리({@code PENDING}) 큐 요청을 이미 보유하는지 확인한다(M2).
     * {@code idNot}으로 처리 중인 요청 자신의 행(아직 {@code PENDING}인 상태로
     * 조회된다)을 제외해야 모든 요청이 자기 자신을 중복으로 오판하지 않는다.
     */
    boolean existsByMemberIdAndCourseIdAndStateAndIdNot(
            Long memberId, Long courseId, RequestState state, Long idNot);
}
