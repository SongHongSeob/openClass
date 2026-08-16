package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentRequestProcessor;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-ENR-020 — 두 개의 동시 트랜잭션이 각각 클레임 쿼리({@code FOR UPDATE
 * SKIP LOCKED})를 실행해도 동일한 행 식별자가 양쪽에서 동시에 반환되지
 * 않는다(REQ-WRK-013, M2). 첫 번째 트랜잭션의 클레임을 수동 트랜잭션 제어로
 * 붙잡아 두 번째 트랜잭션의 조회 시점에 실제로 겹치도록 강제한다 —
 * {@code EnrollmentOrderGuaranteeIntegrationTest}(M1)와 동일한 하네스 패턴.
 */
@SpringBootTest
class EnrollmentClaimExclusivityConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private EnrollmentRequestProcessor processor;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EnrollmentRequestRepository requestRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void cleanUp() {
        requestRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @Timeout(30)
    void 두_동시_클레임_트랜잭션은_동일한_행_id를_반환하지_않는다() throws Exception {
        Long courseId = courseRepository.save(Course.create("클레임테스트강좌", "설명", 100,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();
        for (int i = 0; i < 20; i++) {
            Long memberId = memberRepository.save(
                    Member.createMember("claim" + i + "@example.com", "hash", "회원" + i)).getId();
            receiptService.receiveEnrollment(memberId, courseId);
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch batch1Claimed = new CountDownLatch(1);
            CountDownLatch releaseBatch1 = new CountDownLatch(1);

            Future<List<Long>> f1 = executor.submit(() -> {
                TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                boolean committed = false;
                try {
                    List<Long> batch = processor.claimBatch(15);
                    batch1Claimed.countDown();
                    releaseBatch1.await(10, TimeUnit.SECONDS);
                    transactionManager.commit(status);
                    committed = true;
                    return batch;
                } finally {
                    if (!committed) {
                        transactionManager.rollback(status);
                    }
                }
            });

            batch1Claimed.await(5, TimeUnit.SECONDS);

            Future<List<Long>> f2 = executor.submit(() -> {
                TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                boolean committed = false;
                try {
                    List<Long> batch = processor.claimBatch(15);
                    transactionManager.commit(status);
                    committed = true;
                    return batch;
                } finally {
                    if (!committed) {
                        transactionManager.rollback(status);
                    }
                }
            });

            // 두 번째 클레임이 실제로 첫 번째가 잠금을 보유한 상태에서 실행될
            // 시간을 확보한다(최선 노력 — EnrollmentOrderGuaranteeIntegrationTest와
            // 동일한 관용구).
            Thread.sleep(300);
            releaseBatch1.countDown();

            List<Long> batch1 = f1.get(10, TimeUnit.SECONDS);
            List<Long> batch2 = f2.get(10, TimeUnit.SECONDS);

            List<Long> overlap = batch1.stream().filter(batch2::contains).collect(Collectors.toList());
            assertThat(overlap).as("두 배치 사이에 겹치는 id가 없어야 한다").isEmpty();
            assertThat(batch1).as("첫 번째 배치가 먼저 잠금을 보유해야 한다").isNotEmpty();
            assertThat(batch2)
                    .as("첫 번째 배치가 잠근 행을 건너뛰고 나머지 행을 반환해야 한다")
                    .isNotEmpty();
        } finally {
            executor.shutdownNow();
        }
    }
}
