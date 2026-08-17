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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 미인증 차단 및 타인 데이터 격리(구조적) 통합 테스트 — AC-ENR-057 (M7, v0.3.0
 * 개정, 보안 등급 · REQ-LST-003, REQ-LST-005, INV-ENR-010).
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentHoldingsListSecurityIntegrationTest extends AbstractIntegrationTest {

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

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

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

    // AC-ENR-057 (i) — Authorization 헤더 없이 호출하면 401이고 본문에 보유 내역이 없다
    @Test
    void 인증_헤더가_없으면_두_API_모두_401이고_본문에_보유_내역이_노출되지_않는다() throws Exception {
        Member memberA = memberRepository.save(Member.createMember("sec-a@example.com", "hash", "A"));
        Long courseP = createCourse("보안강좌P", 5);
        receiptService.receiveEnrollment(memberA.getId(), courseP);
        worker.drainQueue();

        String enrollmentsBody = mockMvc.perform(get("/api/enrollments/mine"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(enrollmentsBody).doesNotContain("enrollmentId");

        String waitlistBody = mockMvc.perform(get("/api/waitlist-entries/mine"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(waitlistBody).doesNotContain("waitlistEntryId");
    }

    // AC-ENR-057 (ii) — 타 회원 식별자를 쿼리 파라미터로 덧붙여도 본인 목록만 반환되고 B의 식별자는 나타나지 않는다
    @Test
    void 쿼리_파라미터로_타_회원_식별자를_덧붙여도_본인_목록만_반환되고_타인_식별자가_섞이지_않는다() throws Exception {
        Member memberA = memberRepository.save(Member.createMember("sec-a2@example.com", "hash", "A"));
        Member memberB = memberRepository.save(Member.createMember("sec-b2@example.com", "hash", "B"));
        String tokenA = jwtTokenProvider.generateToken(memberA);

        Long courseP = createCourse("보안강좌P2", 5);
        receiptService.receiveEnrollment(memberA.getId(), courseP);
        receiptService.receiveEnrollment(memberB.getId(), courseP);
        worker.drainQueue();

        Long enrollmentIdOfB = enrollmentRepository.findAll().stream()
                .filter(e -> e.getMemberId().equals(memberB.getId()))
                .findFirst().orElseThrow().getId();

        String body = mockMvc.perform(get("/api/enrollments/mine")
                        .header("Authorization", bearer(tokenA))
                        .param("memberId", String.valueOf(memberB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .as("쿼리 파라미터로 덧붙인 값을 해석하는 경로가 없으므로 B의 데이터가 나타나서는 안 된다")
                .doesNotContain("\"enrollmentId\":" + enrollmentIdOfB);
    }

    // AC-ENR-057 (iii) — 핸들러 시그니처에 회원 식별자를 받는 파라미터가 0개다 (구조적 검증)
    @Test
    void 두_목록_조회_핸들러의_시그니처에_회원_식별자_파라미터가_0개다() {
        Map<RequestMappingInfo, HandlerMethod> handlers = handlerMapping.getHandlerMethods();

        HandlerMethod enrollmentsMineHandler = findHandler(handlers, "/api/enrollments/mine");
        HandlerMethod waitlistMineHandler = findHandler(handlers, "/api/waitlist-entries/mine");

        assertThat(enrollmentsMineHandler)
                .as("GET /api/enrollments/mine 핸들러가 존재해야 한다")
                .isNotNull();
        assertThat(waitlistMineHandler)
                .as("GET /api/waitlist-entries/mine 핸들러가 존재해야 한다")
                .isNotNull();

        assertThat(countMemberIdentifierParameters(enrollmentsMineHandler.getMethod())).isZero();
        assertThat(countMemberIdentifierParameters(waitlistMineHandler.getMethod())).isZero();
    }

    private HandlerMethod findHandler(Map<RequestMappingInfo, HandlerMethod> handlers, String pathSuffix) {
        return handlers.entrySet().stream()
                .filter(entry -> String.valueOf(entry.getKey().getPatternValues()).contains(pathSuffix))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    /**
     * {@code @PathVariable}·{@code @RequestParam}·{@code @RequestBody}로
     * 선언된 파라미터 개수를 센다 — 회원 식별자를 지목할 수 있는 입력
     * 경로다(spec.md §A.6.4). {@code Authentication} 파라미터는 인증 주체
     * 유도 경로이지 사용자가 조작할 수 있는 입력이 아니므로 제외한다.
     */
    private int countMemberIdentifierParameters(Method method) {
        int count = 0;
        for (Parameter parameter : method.getParameters()) {
            for (Annotation annotation : parameter.getAnnotations()) {
                if (annotation instanceof PathVariable || annotation instanceof RequestParam
                        || annotation instanceof RequestBody) {
                    count++;
                }
            }
        }
        return count;
    }
}
