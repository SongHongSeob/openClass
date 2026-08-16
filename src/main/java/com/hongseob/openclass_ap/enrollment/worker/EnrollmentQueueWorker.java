package com.hongseob.openclass_ap.enrollment.worker;

import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 큐 드레인 드라이버 — 큐가 빌 때까지 배치 클레임과 개별 처리를 반복한다
 * (design.md §4.1 {@code poll()}). 배치 전체를 한 트랜잭션으로 묶지 않는다 —
 * 1건 실패가 나머지를 롤백시키면 INV-ENR-005가 깨진다(그 격리는 {@link
 * EnrollmentRequestProcessor#processOne}이 요청 1건 = 트랜잭션 1개로 이미
 * 보장한다).
 *
 * <p><b>단일 스케줄러 활성화 지점</b>(REQ-WRK-012, AC-ENR-019): {@link #poll()}이
 * 이 프로젝트에서 유일하게 워커 폴링을 트리거하는 {@code @Scheduled} 지점이다.
 * 단일 워커 인스턴스 전제와 다중 인스턴스에서 순서 보장이 성립하지 않는다는
 * 경고는 {@code README.md} "수강신청 워커" 절에 기록되어 있다. 이 클래스와
 * {@link EnrollmentRequestProcessor}를 별도 빈으로 분리한 이유는 자기 호출
 * (self-invocation) 시 Spring 프록시를 거치지 않아 {@code @Transactional}이
 * 적용되지 않기 때문이다(research.md §5).</p>
 */
@Service
public class EnrollmentQueueWorker {

    private final EnrollmentRequestProcessor processor;
    private final EnrollmentSchedulerProperties properties;

    public EnrollmentQueueWorker(EnrollmentRequestProcessor processor, EnrollmentSchedulerProperties properties) {
        this.processor = processor;
        this.properties = properties;
    }

    /**
     * design.md §4.1 {@code poll()}의 실제 구현 — 유일한 스케줄러 활성화
     * 지점이다(REQ-WRK-012, AC-ENR-019). 테스트 프로파일은
     * {@code app.enrollment.worker.scheduler-enabled=false}로 이 메서드를
     * 무동작시킨다(research.md §5 "테스트에서 스케줄러가 자동 동작" 대응) —
     * 테스트는 대신 {@link #drainQueue()}를 명시적으로 호출해 타이밍 의존성을
     * 배제한다.
     */
    @Scheduled(fixedDelayString = "${app.enrollment.worker.polling-delay-ms:200}")
    public void poll() {
        if (!properties.schedulerEnabled()) {
            return;
        }
        drainQueue();
    }

    /**
     * 큐가 빌 때까지 반복 처리하고 처리 건수를 반환한다. 통합 테스트가
     * "워커를 큐가 빌 때까지 구동" 단계에서 호출하는 진입점이다.
     *
     * <p>배치 크기는 {@link EnrollmentSchedulerProperties#batchSize()}에서
     * 읽는다(design.md §6 산출표와 정합 — AC-ENR-022). 1건 처리 중 예외가
     * 발생하면({@link EnrollmentRequestProcessor#processOne} 롤백) 이
     * 루프는 멈추지 않고 {@link EnrollmentRequestProcessor#recordFailure}로
     * 그 요청만 {@code FAILED} 종결한 뒤 나머지 요청 처리를 계속한다
     * (REQ-WRK-009, INV-ENR-005, AC-ENR-016).</p>
     */
    public int drainQueue() {
        int processed = 0;
        List<Long> batch;
        while (!(batch = processor.claimBatch(properties.batchSize())).isEmpty()) {
            for (Long id : batch) {
                try {
                    processor.processOne(id);
                } catch (RuntimeException ex) {
                    processor.recordFailure(id);
                }
                processed++;
            }
        }
        return processed;
    }
}
