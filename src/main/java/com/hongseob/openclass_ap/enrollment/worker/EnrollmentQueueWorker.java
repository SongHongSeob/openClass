package com.hongseob.openclass_ap.enrollment.worker;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 큐 드레인 드라이버 — 큐가 빌 때까지 배치 클레임과 개별 처리를 반복한다
 * (design.md §4.1 {@code poll()}). 배치 전체를 한 트랜잭션으로 묶지 않는다 —
 * 1건 실패가 나머지를 롤백시키면 INV-ENR-005가 깨진다(그 격리는 {@link
 * EnrollmentRequestProcessor#processOne}이 요청 1건 = 트랜잭션 1개로 이미
 * 보장한다).
 *
 * <p><b>M1 범위 축소 고지</b>: {@code @Scheduled} 자동 폴링 등록은 M2 범위다
 * (plan.md §F M2 "폴링 빈 / 처리 빈 분리"). M1 테스트는 {@link #drainQueue()}를
 * 명시적으로 호출한다 — 타이밍 의존 테스트를 배제하는 research.md §6-2 원칙과
 * 일치한다. 이 클래스와 {@link EnrollmentRequestProcessor}를 별도 빈으로 분리한
 * 이유는 자기 호출(self-invocation) 시 Spring 프록시를 거치지 않아
 * {@code @Transactional}이 적용되지 않기 때문이다(research.md §5) — M2가
 * {@code @Scheduled}를 추가할 때도 이 분리를 그대로 재사용한다.</p>
 */
@Service
public class EnrollmentQueueWorker {

    // design.md §6은 200을 산출하지만 그 산출 근거(폴링 주기 200ms 등)는 M6
    // 범위다(REQ-WRK-015). M1은 그 값이 확정되기 전까지 보수적인 기본값을 쓴다.
    private static final int BATCH_SIZE = 50;

    private final EnrollmentRequestProcessor processor;

    public EnrollmentQueueWorker(EnrollmentRequestProcessor processor) {
        this.processor = processor;
    }

    /**
     * 큐가 빌 때까지 반복 처리하고 처리 건수를 반환한다. M1 통합 테스트가
     * "워커를 큐가 빌 때까지 구동" 단계에서 호출하는 진입점이다(acceptance.md
     * AC-ENR-006/007 Given/When).
     */
    public int drainQueue() {
        int processed = 0;
        List<Long> batch;
        while (!(batch = processor.claimBatch(BATCH_SIZE)).isEmpty()) {
            for (Long id : batch) {
                processor.processOne(id);
                processed++;
            }
        }
        return processed;
    }
}
