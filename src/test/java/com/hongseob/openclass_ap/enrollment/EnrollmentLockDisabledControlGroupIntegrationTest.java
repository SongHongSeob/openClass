package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-ENR-007 — 잠금 제거 대조군 (메커니즘 유효성 입증, M1 단일 관문 ①의 짝).
 * 동시에 research.md §7 V2("잠금 없이 커밋 순서를 뒤집으면 순서 보장이
 * 깨진다")를 검증한다.
 *
 * <p>{@code app.enrollment.lock-enabled=false}가 필요해 {@link
 * EnrollmentOrderGuaranteeIntegrationTest}(기본 프로퍼티, 잠금 활성화)와는
 * 별도의 Spring 컨텍스트로 분리한다.</p>
 */
@SpringBootTest
@TestPropertySource(properties = "app.enrollment.lock-enabled=false")
class EnrollmentLockDisabledControlGroupIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EnrollmentQueueWorker worker;

    @Autowired
    private EnrollmentRequestRepository requestRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void cleanUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
    }

    private Long createCourse(int capacity) {
        Course course = courseRepository.save(Course.create("대조군강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30)));
        return course.getId();
    }

    // AC-ENR-007 — AC-ENR-006과 동일한 커밋 역전 시나리오(X 먼저 시작, 커밋
    // 지연 / Y 나중 시작)를 잠금 비활성화 상태로 실행한다. 잠금이 없으므로
    // Y는 X의 커밋을 기다리지 않고 즉시 삽입·커밋한다. X가 아직 보이지 않는
    // 시점에 워커를 1차 구동하면 D1이 재현된다 — Y가 확정되고, 뒤늦게 보이는
    // X는 대기명단으로 밀린다.
    @Test
    @Timeout(30)
    void AC_ENR_007_잠금을_끄면_커밋_역전_시나리오에서_순서_보장이_깨진다() throws Exception {
        Long courseId = createCourse(1);
        long memberXId = 601L;
        long memberYId = 602L;

        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            CountDownLatch xInsertedLatch = new CountDownLatch(1);
            CountDownLatch releaseXCommit = new CountDownLatch(1);

            // X: 잠금이 없어도 삽입 자체는 즉시 되지만, 커밋은 의도적으로
            // 지연시킨다. try/finally로 예외 경로에서도 반드시 커밋 또는
            // 롤백되도록 한다 — 그러지 않으면 이 트랜잭션이 점유한 JDBC
            // 커넥션이 HikariCP 풀에 영원히 반환되지 않는다(누수).
            Future<Long> xFuture = executor.submit(() -> {
                TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                boolean committed = false;
                try {
                    Long requestId = receiptService.receiveEnrollment(memberXId, courseId);
                    xInsertedLatch.countDown();
                    releaseXCommit.await(10, TimeUnit.SECONDS);
                    transactionManager.commit(status);
                    committed = true;
                    return requestId;
                } finally {
                    if (!committed) {
                        transactionManager.rollback(status);
                    }
                }
            });

            xInsertedLatch.await(5, TimeUnit.SECONDS);

            // Y: 잠금이 없으므로 X의 커밋을 기다리지 않고 즉시 삽입 + 커밋한다.
            long yId = receiptService.receiveEnrollment(memberYId, courseId);

            // X가 아직 커밋되지 않은 시점에 1차 구동 — Y의 행만 보인다.
            worker.drainQueue();

            // 이제 X를 커밋시켜 뒤늦게 가시화한다.
            releaseXCommit.countDown();
            long xId = xFuture.get(10, TimeUnit.SECONDS);

            // X가 뒤늦게 보이는 상태에서 2차 구동 — 이미 정원이 소진되어 대기로 밀린다.
            worker.drainQueue();

            assertThat(yId).as("Y의 순서값은 여전히 X보다 크다(입력 순서상)").isGreaterThan(xId);
            assertThat(requestRepository.findById(yId).orElseThrow().getResult())
                    .as("잠금이 없으므로 나중에 접수한 Y가 먼저 확정된다 — 선착순 위반이 재현된다")
                    .isEqualTo(RequestResult.SUCCESS);
            assertThat(requestRepository.findById(xId).orElseThrow().getResult())
                    .as("먼저 접수한 X가 대기명단으로 밀린다")
                    .isEqualTo(RequestResult.WAITLISTED);
            assertThat(enrollmentRepository.findAll())
                    .extracting(Enrollment::getMemberId)
                    .containsExactly(memberYId);
        } finally {
            executor.shutdownNow();
        }
    }
}
