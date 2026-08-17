package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 보유 내역 조회의 무부작용성 통합 테스트 — AC-ENR-058 (M7, v0.3.0 개정,
 * REQ-LST-004, INV-ENR-002).
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentHoldingsListSideEffectFreeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EnrollmentRequestRepository requestRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private EnrollmentQueueWorker worker;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        waitlistEntryRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // AC-ENR-058
    @Test
    void 두_목록_조회_API를_20회씩_반복_호출해도_세_테이블의_상태가_최초_기록값과_정확히_동일하다() throws Exception {
        Member memberA = memberRepository.save(Member.createMember("noeffect-a@example.com", "hash", "A"));
        String tokenA = jwtTokenProvider.generateToken(memberA);

        Long courseFull = courseRepository.save(Course.create("무부작용강좌", "설명", 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();

        // 확정 1건 + 대기 1건 + 큐 행이 모두 존재하는 상태를 만든다.
        Member memberB = memberRepository.save(Member.createMember("noeffect-b@example.com", "hash", "B"));
        receiptService.receiveEnrollment(memberB.getId(), courseFull); // 확정
        receiptService.receiveEnrollment(memberA.getId(), courseFull); // 대기
        worker.drainQueue();

        long enrollmentCountBefore = enrollmentRepository.count();
        long waitlistCountBefore = waitlistEntryRepository.count();
        long requestCountBefore = requestRepository.count();
        int enrolledCountBefore = courseRepository.findById(courseFull).orElseThrow().getEnrolledCount();
        var enrollmentStatusesBefore = enrollmentRepository.findAll().stream()
                .map(Enrollment::getStatus).sorted().toList();
        var waitlistStatusesBefore = waitlistEntryRepository.findAll().stream()
                .map(entry -> entry.getStatus().name()).sorted().toList();

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/enrollments/mine").header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/waitlist-entries/mine").header("Authorization", bearer(tokenA)))
                    .andExpect(status().isOk());
        }

        assertThat(enrollmentRepository.count()).isEqualTo(enrollmentCountBefore);
        assertThat(waitlistEntryRepository.count()).isEqualTo(waitlistCountBefore);
        assertThat(requestRepository.count()).isEqualTo(requestCountBefore);
        assertThat(courseRepository.findById(courseFull).orElseThrow().getEnrolledCount())
                .isEqualTo(enrolledCountBefore);
        assertThat(enrollmentRepository.findAll().stream().map(Enrollment::getStatus).sorted().toList())
                .isEqualTo(enrollmentStatusesBefore);
        assertThat(waitlistEntryRepository.findAll().stream().map(entry -> entry.getStatus().name()).sorted().toList())
                .isEqualTo(waitlistStatusesBefore);
    }
}
