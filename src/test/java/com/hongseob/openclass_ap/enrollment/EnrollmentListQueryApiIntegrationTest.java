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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 내 확정 수강신청 목록 조회 API 통합 테스트 — AC-ENR-054 (M7, v0.3.0 개정).
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentListQueryApiIntegrationTest extends AbstractIntegrationTest {

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

    private Long createCourse(String title, int capacity) {
        return courseRepository.save(Course.create(title, "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();
    }

    private Long enrollmentIdOf(Long memberId, Long courseId) {
        return enrollmentRepository.findAll().stream()
                .filter(e -> e.getMemberId().equals(memberId) && e.getCourseId().equals(courseId))
                .findFirst().orElseThrow().getId();
    }

    // AC-ENR-054
    @Test
    void 내_활성_확정_목록만_enrollmentId_오름차순으로_반환하고_타인_행과_취소된_행은_섞이지_않는다() throws Exception {
        Member memberA = memberRepository.save(Member.createMember("list-a@example.com", "hash", "A"));
        Member memberB = memberRepository.save(Member.createMember("list-b@example.com", "hash", "B"));
        String tokenA = jwtTokenProvider.generateToken(memberA);

        Long courseP = createCourse("강좌P", 5);
        Long courseQ = createCourse("강좌Q", 5);
        Long courseR = createCourse("강좌R", 5);

        // A: P·Q 확정, R은 확정 후 취소
        receiptService.receiveEnrollment(memberA.getId(), courseP);
        receiptService.receiveEnrollment(memberA.getId(), courseQ);
        receiptService.receiveEnrollment(memberA.getId(), courseR);
        // B: P에도 확정 — A의 응답에 섞이면 안 된다
        receiptService.receiveEnrollment(memberB.getId(), courseP);
        worker.drainQueue();

        Long enrollmentR = enrollmentIdOf(memberA.getId(), courseR);
        mockMvc.perform(delete("/api/enrollments/" + enrollmentR).header("Authorization", bearer(tokenA)))
                .andExpect(status().isAccepted());
        worker.drainQueue();

        Long enrollmentP = enrollmentIdOf(memberA.getId(), courseP);
        Long enrollmentQ = enrollmentIdOf(memberA.getId(), courseQ);
        long expectedFirst = Math.min(enrollmentP, enrollmentQ);
        long expectedSecond = Math.max(enrollmentP, enrollmentQ);

        String body = mockMvc.perform(get("/api/enrollments/mine").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].enrollmentId").value(expectedFirst))
                .andExpect(jsonPath("$[0].status").value("ENROLLED"))
                .andExpect(jsonPath("$[1].enrollmentId").value(expectedSecond))
                .andExpect(jsonPath("$[1].status").value("ENROLLED"))
                .andExpect(jsonPath("$[0].courseTitle").exists())
                .andExpect(jsonPath("$[0].enrolledAt").exists())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .as("취소된 R과 B의 확정 행이 응답에 나타나지 않아야 한다")
                .doesNotContain("\"enrollmentId\":" + enrollmentR);
    }

    // AC-ENR-054 (빈 목록)
    @Test
    void 확정이_하나도_없는_회원이_조회하면_404가_아니라_200과_빈_배열이다() throws Exception {
        Member memberC = memberRepository.save(Member.createMember("list-c@example.com", "hash", "C"));
        String tokenC = jwtTokenProvider.generateToken(memberC);

        mockMvc.perform(get("/api/enrollments/mine").header("Authorization", bearer(tokenC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
