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
import com.hongseob.openclass_ap.waitlist.WaitlistStatus;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
 * 조회 → 취소 계약 폐쇄 통합 테스트 — AC-ENR-056 (M7, v0.3.0 개정,
 * {@code DEP-2} 종결, spec.md §A.6 · REQ-LST-006). 이 개정의 단일 관문이다 —
 * 목록 조회 응답에서 꺼낸 식별자를 그대로 취소 API에 넣어 실제로 취소가
 * 성립하는지를 검증한다. 테스트 코드가 어떤 식별자도 DB에서 미리 읽지
 * 않는다 — 유일한 출처는 API 응답 본문이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentHoldingsListToCancelContractIntegrationTest extends AbstractIntegrationTest {

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

    // AC-ENR-056 — 이 개정의 단일 관문
    @Test
    void 목록_조회_응답에서_꺼낸_식별자만으로_확정_취소와_대기_취소가_실제로_성립한다() throws Exception {
        Member memberA = memberRepository.save(Member.createMember("contract-a@example.com", "hash", "A"));
        Member memberFiller = memberRepository.save(Member.createMember("contract-filler@example.com", "hash", "F"));
        String tokenA = jwtTokenProvider.generateToken(memberA);
        String tokenFiller = jwtTokenProvider.generateToken(memberFiller);

        Long courseP = createCourse("계약강좌P", 5);
        Long courseX = createCourse("계약강좌X", 1);

        // A: P 확정
        receiptService.receiveEnrollment(memberA.getId(), courseP);
        // A: X 대기 (필러가 먼저 확정을 채운다)
        receiptService.receiveEnrollment(memberFiller.getId(), courseX);
        receiptService.receiveEnrollment(memberA.getId(), courseX);
        worker.drainQueue();

        // (i) 확정 목록 조회 → 응답에서 enrollmentId를 꺼내 그대로 취소에 사용한다.
        String enrollmentListBody = mockMvc.perform(get("/api/enrollments/mine").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> enrollmentList = JsonPath.read(enrollmentListBody, "$");
        assertThat(enrollmentList).hasSize(1);
        long enrollmentIdFromResponse = ((Number) enrollmentList.get(0).get("enrollmentId")).longValue();

        mockMvc.perform(delete("/api/enrollments/" + enrollmentIdFromResponse)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists());
        worker.drainQueue();

        assertThat(enrollmentRepository.findById(enrollmentIdFromResponse).orElseThrow().getStatus())
                .as("REJECTED가 아니라 CANCELLED여야 한다 — REJECTED는 식별자가 잘못 대응되었다는 신호다")
                .isEqualTo(EnrollmentStatus.CANCELLED);

        // (ii) 대기 목록 조회 → 응답에서 waitlistEntryId를 꺼내 그대로 취소에 사용한다.
        String waitlistBody = mockMvc.perform(get("/api/waitlist-entries/mine").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> waitlistList = JsonPath.read(waitlistBody, "$");
        assertThat(waitlistList).hasSize(1);
        long waitlistEntryIdFromResponse = ((Number) waitlistList.get(0).get("waitlistEntryId")).longValue();

        mockMvc.perform(delete("/api/waitlist-entries/" + waitlistEntryIdFromResponse)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());

        assertThat(waitlistEntryRepository.findById(waitlistEntryIdFromResponse).orElseThrow().getStatus())
                .isEqualTo(WaitlistStatus.CANCELLED);

        // 필러 토큰은 위 흐름과 무관하게 사용되지 않았다는 것만 확인하는 방어적 no-op.
        assertThat(tokenFiller).isNotBlank();
    }
}
