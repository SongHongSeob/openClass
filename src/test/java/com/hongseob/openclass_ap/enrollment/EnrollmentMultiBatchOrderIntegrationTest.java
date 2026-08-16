package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentRequestProcessor;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.hongseob.openclass_ap.waitlist.WaitlistEntry;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-ENR-023 — 워커 배치 크기보다 많은 요청이 여러 배치에 걸쳐 처리되어도
 * 확정 순서·대기 순번이 접수 순서와 일치한다(REQ-WRK-003, M2).
 */
@SpringBootTest
class EnrollmentMultiBatchOrderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private EnrollmentRequestProcessor processor;

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

    @Test
    void 배치_크기보다_많은_요청이_여러_배치에_걸쳐_처리되어도_확정과_대기_순번이_접수_순서와_일치한다() {
        int capacity = 5;
        int applicants = 12;
        int forcedBatchLimit = 3; // 반드시 여러 배치가 필요하도록 작게 강제한다

        Long courseId = courseRepository.save(Course.create("다중배치강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();

        List<Long> memberIds = new ArrayList<>();
        for (int i = 0; i < applicants; i++) {
            Long memberId = memberRepository.save(
                    Member.createMember("multibatch" + i + "@example.com", "hash", "회원" + i)).getId();
            memberIds.add(memberId);
            receiptService.receiveEnrollment(memberId, courseId);
        }

        // EnrollmentQueueWorker.drainQueue()의 설정 배치 크기(200)를 우회해
        // processor.claimBatch를 직접 작은 크기(3)로 반복 호출한다 — "배치
        // 크기 초과" 시나리오를 결정적으로 재현하기 위함이다.
        int batchCount = 0;
        List<Long> batch;
        while (!(batch = processor.claimBatch(forcedBatchLimit)).isEmpty()) {
            batchCount++;
            for (Long id : batch) {
                processor.processOne(id);
            }
        }
        assertThat(batchCount)
                .as("배치 크기(%d)보다 많은 요청(%d건)이므로 최소 2회 이상의 배치가 필요하다",
                        forcedBatchLimit, applicants)
                .isGreaterThan(1);

        List<Long> expectedSuccessMembers = memberIds.subList(0, capacity);
        List<Long> expectedWaitlistedMembers = memberIds.subList(capacity, applicants);

        List<Enrollment> enrolled = enrollmentRepository.findAll();
        assertThat(enrolled.stream().map(Enrollment::getMemberId).toList())
                .as("확정자 집합이 접수 순서 상위 %d명과 일치해야 한다", capacity)
                .containsExactlyInAnyOrderElementsOf(expectedSuccessMembers);

        List<WaitlistEntry> waitlist = waitlistEntryRepository.findAll();
        waitlist.sort(Comparator.comparing(WaitlistEntry::getPosition));
        assertThat(waitlist.stream().map(WaitlistEntry::getMemberId).toList())
                .as("대기 순번이 접수 순서와 일치해야 한다")
                .containsExactlyElementsOf(expectedWaitlistedMembers);
        for (int i = 0; i < waitlist.size(); i++) {
            assertThat(waitlist.get(i).getPosition()).isEqualTo((long) (i + 1));
        }

        assertThat(requestRepository.findAll().stream()
                .filter(r -> r.getResult() == RequestResult.SUCCESS).count())
                .isEqualTo((long) capacity);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(capacity);
    }
}
