package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 정원 초과 방지 동시성 부하 — AC-ENR-010(이 SPEC의 **단일 관문 ②**),
 * AC-ENR-011(선착순 순서 일치), AC-ENR-012(정원 1 경계) (M2).
 */
@SpringBootTest
class EnrollmentOversellPreventionConcurrencyTest extends AbstractIntegrationTest {

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

    private Long createCourse(int capacity) {
        return courseRepository.save(Course.create("동시성부하강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();
    }

    // AC-ENR-010 (단일 관문 ②) + AC-ENR-011
    @Test
    @Timeout(60)
    void 정원_10에_50명이_동시_접수해도_확정은_정확히_10건이고_확정자는_접수_순서_상위_10명과_일치한다() throws Exception {
        int capacity = 10;
        int applicants = 50;
        Long courseId = createCourse(capacity);

        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < applicants; i++) {
            memberIds.add(memberRepository.save(
                    Member.createMember("oversell" + i + "@example.com", "hash", "회원" + i)).getId());
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
            startLatch.countDown();

            Map<Long, Long> requestIdByMember = new HashMap<>();
            for (int i = 0; i < applicants; i++) {
                requestIdByMember.put(memberIds.get(i), futures.get(i).get(20, TimeUnit.SECONDS));
            }

            worker.drainQueue();

            List<Enrollment> enrolled = enrollmentRepository.findAll();
            assertThat(enrolled).hasSize(capacity);
            assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount())
                    .as("확정 행 수와 course.enrolled_count가 일치해야 한다").isEqualTo(capacity);
            assertThat(enrolled.stream().filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED).count())
                    .isEqualTo((long) capacity);

            long successCount = requestRepository.findAll().stream()
                    .filter(r -> r.getResult() == RequestResult.SUCCESS)
                    .count();
            long waitlistedCount = requestRepository.findAll().stream()
                    .filter(r -> r.getResult() == RequestResult.WAITLISTED)
                    .count();
            assertThat(successCount).isEqualTo((long) capacity);
            assertThat(waitlistedCount).isEqualTo((long) (applicants - capacity));

            // AC-ENR-011 — 확정된 회원 집합이 접수 순서(requestId) 상위 capacity건과 일치한다.
            List<Long> topByRequestId = requestIdByMember.entrySet().stream()
                    .sorted(Comparator.comparingLong(Map.Entry::getValue))
                    .limit(capacity)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            List<Long> confirmedMembers = enrolled.stream().map(Enrollment::getMemberId).toList();
            assertThat(confirmedMembers)
                    .as("확정자 집합이 접수 순서 상위 %d명과 정확히 일치해야 한다", capacity)
                    .containsExactlyInAnyOrderElementsOf(topByRequestId);
        } finally {
            executor.shutdownNow();
        }
    }

    // AC-ENR-012
    @Test
    @Timeout(30)
    void 정원_1에_2명이_동시_접수하면_확정은_1건이고_순서값이_작은_쪽이_확정된다() throws Exception {
        Long courseId = createCourse(1);
        Long memberA = memberRepository.save(Member.createMember("boundary-a@example.com", "hash", "A")).getId();
        Long memberB = memberRepository.save(Member.createMember("boundary-b@example.com", "hash", "B")).getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch startLatch = new CountDownLatch(1);
            Future<Long> futureA = executor.submit(() -> {
                startLatch.await();
                return receiptService.receiveEnrollment(memberA, courseId);
            });
            Future<Long> futureB = executor.submit(() -> {
                startLatch.await();
                return receiptService.receiveEnrollment(memberB, courseId);
            });
            startLatch.countDown();

            long aId = futureA.get(10, TimeUnit.SECONDS);
            long bId = futureB.get(10, TimeUnit.SECONDS);

            worker.drainQueue();

            assertThat(enrollmentRepository.count()).isEqualTo(1);
            assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(1);

            long winnerRequestId = Math.min(aId, bId);
            long loserRequestId = Math.max(aId, bId);
            assertThat(requestRepository.findById(winnerRequestId).orElseThrow().getResult())
                    .as("순서값이 더 작은 요청이 확정되어야 한다").isEqualTo(RequestResult.SUCCESS);
            assertThat(requestRepository.findById(loserRequestId).orElseThrow().getResult())
                    .isEqualTo(RequestResult.WAITLISTED);
        } finally {
            executor.shutdownNow();
        }
    }
}
