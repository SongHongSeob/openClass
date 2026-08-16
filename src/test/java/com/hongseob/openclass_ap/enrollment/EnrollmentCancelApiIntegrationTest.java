package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestState;
import com.hongseob.openclass_ap.enrollment.request.RequestType;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 확정 취소 접수 API 통합 테스트 — AC-ENR-035(큐 경유), AC-ENR-036(보안 —
 * 타인 확정 취소 API 계층 차단), AC-ENR-038(관리자 대리 취소 부재) (M4).
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentCancelApiIntegrationTest extends AbstractIntegrationTest {

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

    private Long courseId;
    private Long memberAId;
    private String memberAToken;
    private String memberBToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        waitlistEntryRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        Member memberA = memberRepository.save(Member.createMember("cancel-api-a@example.com", "hash", "A"));
        Member memberB = memberRepository.save(Member.createMember("cancel-api-b@example.com", "hash", "B"));
        Member admin = memberRepository.save(Member.createAdmin("cancel-api-admin@example.com", "hash", "관리자"));
        memberAId = memberA.getId();
        memberAToken = jwtTokenProvider.generateToken(memberA);
        memberBToken = jwtTokenProvider.generateToken(memberB);
        adminToken = jwtTokenProvider.generateToken(admin);

        Course course = courseRepository.save(Course.create("취소API강좌", "설명", 5,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30)));
        courseId = course.getId();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long confirmedEnrollmentIdOf(Long memberId) {
        receiptService.receiveEnrollment(memberId, courseId);
        worker.drainQueue();
        return enrollmentRepository.findAll().stream()
                .filter(e -> e.getMemberId().equals(memberId))
                .findFirst().orElseThrow().getId();
    }

    // AC-ENR-035
    @Test
    void 취소_접수는_큐_적재만_수행하고_워커_구동_전에는_도메인이_변하지_않는다() throws Exception {
        Long enrollmentId = confirmedEnrollmentIdOf(memberAId);

        int enrolledCountBefore = courseRepository.findById(courseId).orElseThrow().getEnrolledCount();
        EnrollmentStatus statusBefore = enrollmentRepository.findById(enrollmentId).orElseThrow().getStatus();

        mockMvc.perform(delete("/api/enrollments/" + enrollmentId)
                        .header("Authorization", bearer(memberAToken)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists());

        assertThat(requestRepository.findAll())
                .filteredOn(request -> request.getRequestType() == RequestType.CANCEL)
                .hasSize(1)
                .allSatisfy(request -> {
                    assertThat(request.getState()).isEqualTo(RequestState.PENDING);
                    assertThat(request.getTargetEnrollmentId()).isEqualTo(enrollmentId);
                });

        // 워커를 구동하지 않았으므로 도메인은 변하지 않는다.
        assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getStatus()).isEqualTo(statusBefore);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount())
                .isEqualTo(enrolledCountBefore);
    }

    // AC-ENR-036 — 보안 등급
    @Test
    void 타인의_확정_수강신청을_취소하면_403_또는_404이고_CANCEL_큐_행이_생성되지_않으며_A의_상태가_불변이다() throws Exception {
        Long enrollmentId = confirmedEnrollmentIdOf(memberAId);
        int enrolledCountBefore = courseRepository.findById(courseId).orElseThrow().getEnrolledCount();

        int status = mockMvc.perform(delete("/api/enrollments/" + enrollmentId)
                        .header("Authorization", bearer(memberBToken)))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(403, 404);

        assertThat(requestRepository.findAll())
                .as("CANCEL 큐 행이 전혀 생성되지 않아야 한다")
                .noneMatch(request -> request.getRequestType() == RequestType.CANCEL);
        assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount())
                .isEqualTo(enrolledCountBefore);

        // 소유자 본인은 정상적으로 취소를 접수할 수 있다 — 위 실패가 자원
        // 자체의 결함이 아님을 확인한다.
        mockMvc.perform(delete("/api/enrollments/" + enrollmentId)
                        .header("Authorization", bearer(memberAToken)))
                .andExpect(status().isAccepted());
    }

    // AC-ENR-038 — 전반부: ADMIN도 특권 없이 동일하게 차단된다
    @Test
    void ADMIN도_대리로_타인의_확정_수강신청을_취소하지_못하고_A의_확정이_유지된다() throws Exception {
        Long enrollmentId = confirmedEnrollmentIdOf(memberAId);

        int status = mockMvc.perform(delete("/api/enrollments/" + enrollmentId)
                        .header("Authorization", bearer(adminToken)))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(403, 404);

        assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.ENROLLED);
    }

    // AC-ENR-038 — 후반부: 관리자 대리 취소용 엔드포인트가 API 목록에 존재하지 않는다
    @Test
    void 관리자_대리_취소용_엔드포인트가_핸들러_매핑에_존재하지_않는다() {
        boolean adminProxyCancelExists = handlerMapping.getHandlerMethods().entrySet().stream()
                .anyMatch(this::looksLikeAdminProxyCancel);

        assertThat(adminProxyCancelExists)
                .as("관리자 대리 취소용 엔드포인트가 API 목록에 존재해서는 안 된다(REQ-CNL-004)")
                .isFalse();
    }

    private boolean looksLikeAdminProxyCancel(Map.Entry<RequestMappingInfo, HandlerMethod> entry) {
        RequestMappingInfo info = entry.getKey();
        String pattern = String.valueOf(info.getPatternValues());
        boolean isAdminPath = pattern.contains("/api/admin");
        boolean mentionsEnrollmentCancellation = pattern.toLowerCase().contains("enrollment")
                || pattern.toLowerCase().contains("cancel");
        return isAdminPath && mentionsEnrollmentCancellation;
    }
}
