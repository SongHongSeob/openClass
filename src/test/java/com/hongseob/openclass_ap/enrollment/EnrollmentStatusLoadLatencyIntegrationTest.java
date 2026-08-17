package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequest;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.request.RequestState;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 부하 상한(동시 접수 500건)에서의 종단 지연 실측 — AC-ENR-026 (M3, 이 SPEC의
 * **단일 관문은 아니지만 가장 무거운 테스트**).
 *
 * <p>REQ-STS-003이 정한 부하 상한(500건)에서 직접 측정한다 — N=1 폴링으로는
 * 이 요구사항을 검증하지 못한다(감사 D5). 종단 지연 = "500건을 동시에 접수시킨
 * 시각"부터 "워커가 큐를 정해진 배치 크기({@link
 * com.hongseob.openclass_ap.enrollment.worker.EnrollmentSchedulerProperties#batchSize()},
 * design.md §6과 일치하는 200건)로 구동해 마지막 요청까지 종단 결과에 도달한
 * 시각"까지다. {@link EnrollmentQueueWorker#drainQueue()}가 큐가 빌 때까지
 * 반복 처리하므로 최초 1회 호출로 이 구간 전체를 담당한다 — 스케줄러의
 * 폴링 대기(design.md §6 C항, 0.2초)는 첫 트리거 지연이며 이 측정에는 포함하지
 * 않는다(직접 구동이므로 그 지연이 발생하지 않는다 — 5초 목표 대비 더
 * 낙관적인 값이 나오는 방향이며, 목표 미달을 감추는 방향이 아니다).</p>
 *
 * <p><b>격리 실행 필수</b>(Section B 사전 지시): 이 클래스는 이 SPEC에서 가장
 * 자원 집약적인 테스트이므로 다른 테스트 클래스와 배치 실행하지 않는다.
 * HikariCP 풀 크기를 60으로 확장한다 — 500개의 동시 접수 트랜잭션이 각자
 * 커넥션을 점유한 채 강좌 단위 접수 잠금 대기열에 들어갈 수 있으므로, 기본
 * 풀 크기(10)로는 커넥션 획득 자체가 병목이 되어 이 테스트가 측정하려는
 * "접수 잠금 직렬화 지연"이 아니라 "테스트 인프라의 커넥션 부족"을 재는
 * 결과가 된다. 이는 정당한 테스트 설정 조정이며 코드 결함이 아니다.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=60"
})
class EnrollmentStatusLoadLatencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private EnrollmentQueueWorker worker;

    @Autowired
    private EnrollmentRequestRepository requestRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void cleanUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        waitlistEntryRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();
    }

    // AC-ENR-026
    @Test
    @Timeout(60)
    void 정원_100_강좌에_500명이_동시_접수해도_마지막_요청까지_5초_이내에_종단_결과에_도달한다() throws Exception {
        int capacity = 100;
        int applicants = 500;

        Long courseId = courseRepository.save(Course.create("부하측정강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();

        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < applicants; i++) {
            memberIds.add(memberRepository.save(
                    Member.createMember("load" + i + "@example.com", "hash", "회원" + i)).getId());
        }

        ExecutorService executor = Executors.newFixedThreadPool(applicants);
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<Long>> futures = new ArrayList<>();
            for (Long memberId : memberIds) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    return receiptService.receiveEnrollment(memberId, courseId);
                }));
            }

            // "500건을 동시에 접수시킨 시각" — 동시 기동 트리거 직전을 시작점으로 삼는다.
            long startNanos = System.nanoTime();
            startLatch.countDown();
            for (Future<Long> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            // "정해진 배치 크기로 워커를 구동" — drainQueue()는 EnrollmentSchedulerProperties의
            // batchSize를 그대로 사용하며 큐가 빌 때까지(모든 요청이 종단 결과에 도달할 때까지)
            // 반복한다. "마지막 요청이 종단 결과에 도달한 시각"은 이 호출이 반환한 시점이다.
            int processed = worker.drainQueue();
            long endNanos = System.nanoTime();

            long elapsedMs = (endNanos - startNanos) / 1_000_000;
            double throughputPerSec = applicants / (elapsedMs / 1000.0);

            System.out.printf(
                    "AC-ENR-026 실측 — 동시 접수 %d건, 종단 지연 %dms, 처리량 %.1f건/초"
                            + " (design.md §6 계산 예산 ≈1,700ms, A+B+C)%n",
                    applicants, elapsedMs, throughputPerSec);

            assertThat(processed).as("500건 전부 워커가 처리해야 한다").isEqualTo(applicants);

            List<EnrollmentRequest> allRequests = requestRepository.findAll();
            assertThat(allRequests)
                    .as("모든 요청이 종단 결과(state=DONE)에 도달해야 한다")
                    .allSatisfy(r -> assertThat(r.getState()).isEqualTo(RequestState.DONE));

            assertThat(elapsedMs)
                    .as("REQ-STS-003 — 부하 상한(동시 접수 500건)에서 종단 지연은 5초(5000ms) 이내여야 한다")
                    .isLessThanOrEqualTo(5000L);

            long successCount = allRequests.stream().filter(r -> r.getResult() == RequestResult.SUCCESS).count();
            long waitlistedCount = allRequests.stream()
                    .filter(r -> r.getResult() == RequestResult.WAITLISTED).count();
            assertThat(successCount).as("확정은 정원과 정확히 일치해야 한다").isEqualTo((long) capacity);
            assertThat(waitlistedCount).isEqualTo((long) (applicants - capacity));
            assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount())
                    .isEqualTo(capacity);
        } finally {
            executor.shutdownNow();
        }
    }
}
