package com.hongseob.openclass_ap.waitlist;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.EnrollmentRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 내 대기명단 항목 목록 조회 API 통합 테스트 — AC-ENR-055 (M7, v0.3.0 개정).
 */
@SpringBootTest
@AutoConfigureMockMvc
class WaitlistListQueryApiIntegrationTest extends AbstractIntegrationTest {

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

    private Long waitingEntryIdOf(Long memberId, Long courseId) {
        return waitlistEntryRepository.findAll().stream()
                .filter(w -> w.getMemberId().equals(memberId) && w.getCourseId().equals(courseId)
                        && w.getStatus() == WaitlistStatus.WAITING)
                .findFirst().orElseThrow().getId();
    }

    // AC-ENR-055
    @Test
    void 내_활성_대기_항목만_position_오름차순으로_반환하고_승격된_행과_타인_행은_섞이지_않는다() throws Exception {
        Member m0 = memberRepository.save(Member.createMember("wl-m0@example.com", "hash", "M0"));
        Member m1 = memberRepository.save(Member.createMember("wl-m1@example.com", "hash", "M1"));
        Member m2 = memberRepository.save(Member.createMember("wl-m2@example.com", "hash", "M2"));
        Member memberA = memberRepository.save(Member.createMember("wl-a@example.com", "hash", "A"));
        Member memberB = memberRepository.save(Member.createMember("wl-b@example.com", "hash", "B"));
        String tokenA = jwtTokenProvider.generateToken(memberA);
        String tokenM0 = jwtTokenProvider.generateToken(m0);

        Long courseZ = createCourse("강좌Z", 1);
        Long courseY = createCourse("강좌Y", 1);
        Long courseX = createCourse("강좌X", 1);

        receiptService.receiveEnrollment(m0.getId(), courseZ);
        receiptService.receiveEnrollment(m1.getId(), courseY);
        receiptService.receiveEnrollment(m2.getId(), courseX);
        worker.drainQueue();

        receiptService.receiveEnrollment(memberA.getId(), courseZ); // A: Z 대기 1순위 (나중에 승격됨)
        receiptService.receiveEnrollment(memberA.getId(), courseY); // A: Y 대기 1순위
        receiptService.receiveEnrollment(memberB.getId(), courseX); // B: X 대기 1순위
        receiptService.receiveEnrollment(memberA.getId(), courseX); // A: X 대기 2순위
        worker.drainQueue();

        Long zEntryId = waitingEntryIdOf(memberA.getId(), courseZ);

        // M0가 Z 확정을 취소하면 A가 즉시 승격되어 Z의 대기 항목은 PROMOTED로 종결된다.
        Long m0EnrollmentId = enrollmentIdOf(m0.getId(), courseZ);
        mockMvc.perform(delete("/api/enrollments/" + m0EnrollmentId).header("Authorization", bearer(tokenM0)))
                .andExpect(status().isAccepted());
        worker.drainQueue();

        assertThat(waitlistEntryRepository.findById(zEntryId).orElseThrow().getStatus())
                .isEqualTo(WaitlistStatus.PROMOTED);

        Long yEntryId = waitingEntryIdOf(memberA.getId(), courseY);
        Long xEntryId = waitingEntryIdOf(memberA.getId(), courseX);

        String body = mockMvc.perform(get("/api/waitlist-entries/mine").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].waitlistEntryId").value(yEntryId))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].status").value("WAITING"))
                .andExpect(jsonPath("$[1].waitlistEntryId").value(xEntryId))
                .andExpect(jsonPath("$[1].position").value(2))
                .andExpect(jsonPath("$[1].status").value("WAITING"))
                .andExpect(jsonPath("$[0].courseTitle").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .as("승격된 Z 항목과 B의 X 대기 항목이 응답에 나타나지 않아야 한다")
                .doesNotContain("\"waitlistEntryId\":" + zEntryId);
    }

    // AC-ENR-055 (빈 목록)
    @Test
    void 대기가_하나도_없는_회원이_조회하면_200과_빈_배열이다() throws Exception {
        Member memberC = memberRepository.save(Member.createMember("wl-c@example.com", "hash", "C"));
        String tokenC = jwtTokenProvider.generateToken(memberC);

        mockMvc.perform(get("/api/waitlist-entries/mine").header("Authorization", bearer(tokenC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
