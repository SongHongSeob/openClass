package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 상태 조회 API 통합 테스트 — AC-ENR-024, AC-ENR-025, AC-ENR-027 (M3).
 *
 * <p>AC-ENR-026(부하 상한에서의 지연 목표)은 이 SPEC에서 가장 무거운 테스트라서
 * 별도 클래스({@link EnrollmentStatusLoadLatencyIntegrationTest})로 분리해
 * 독립 실행한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentStatusQueryApiIntegrationTest extends AbstractIntegrationTest {

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

    private String memberAToken;
    private String memberBToken;
    private Long memberAId;
    private Long memberBId;
    private Long courseId;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        waitlistEntryRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        Member memberA = memberRepository.save(Member.createMember("status-a@example.com", "hash", "A"));
        Member memberB = memberRepository.save(Member.createMember("status-b@example.com", "hash", "B"));
        memberAId = memberA.getId();
        memberBId = memberB.getId();
        memberAToken = jwtTokenProvider.generateToken(memberA);
        memberBToken = jwtTokenProvider.generateToken(memberB);

        Course course = courseRepository.save(Course.create("상태조회강좌", "설명", 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30)));
        courseId = course.getId();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // AC-ENR-024
    @Test
    void 워커_구동_전에는_PENDING이고_구동_후에는_종단_결과값을_반환한다() throws Exception {
        Long requestId = receiptService.receiveEnrollment(memberAId, courseId);

        mockMvc.perform(get("/api/enrollment-requests/" + requestId)
                        .header("Authorization", bearer(memberAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.status").value("PENDING"));

        worker.drainQueue();

        mockMvc.perform(get("/api/enrollment-requests/" + requestId)
                        .header("Authorization", bearer(memberAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertThat(requestRepository.findById(requestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.SUCCESS);
    }

    // AC-ENR-024 (WAITLISTED — 대기 순번 동반 반환)
    @Test
    void 결과가_WAITLISTED이면_대기_순번이_함께_반환된다() throws Exception {
        // 정원 1 — 첫 신청자가 확정, 두 번째 신청자가 대기 1순위가 된다.
        receiptService.receiveEnrollment(memberAId, courseId);
        Long secondRequestId = receiptService.receiveEnrollment(memberBId, courseId);

        worker.drainQueue();

        mockMvc.perform(get("/api/enrollment-requests/" + secondRequestId)
                        .header("Authorization", bearer(memberBToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(1));
    }

    // AC-ENR-025
    @Test
    void 타인의_요청을_조회하면_404이고_본문에_소유자_정보가_노출되지_않는다() throws Exception {
        Long requestId = receiptService.receiveEnrollment(memberAId, courseId);

        // 응답 본문에 A의 요청 정보(회원 식별자·강좌 식별자·상태) 자체가 존재하지
        // 않아야 한다(AC-ENR-025). 숫자 문자열 부분일치는 다른 테스트 메서드가
        // 누적시킨 시퀀스 값과 우연히 겹칠 수 있어 신뢰할 수 없으므로(예:
        // requestId 자체가 우연히 courseId와 같은 값일 수 있다), JSON 키 부재와
        // 상태값 리터럴 부재로 검증한다.
        String body = mockMvc.perform(get("/api/enrollment-requests/" + requestId)
                        .header("Authorization", bearer(memberBToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.memberId").doesNotExist())
                .andExpect(jsonPath("$.courseId").doesNotExist())
                .andExpect(jsonPath("$.waitlistPosition").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .as("에러 응답 본문에 A의 상태 값이 노출되지 않아야 한다")
                .doesNotContain("PENDING")
                .doesNotContain("SUCCESS")
                .doesNotContain("WAITLISTED");

        // 소유자 본인은 여전히 정상 조회 가능 — 위 404가 자원 자체의 결함이 아님을 확인
        mockMvc.perform(get("/api/enrollment-requests/" + requestId)
                        .header("Authorization", bearer(memberAToken)))
                .andExpect(status().isOk());
    }

    // AC-ENR-025 (존재하지 않는 요청)
    @Test
    void 존재하지_않는_요청을_조회하면_404다() throws Exception {
        mockMvc.perform(get("/api/enrollment-requests/999999")
                        .header("Authorization", bearer(memberAToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENROLLMENT_REQUEST_NOT_FOUND"));
    }

    // AC-ENR-027
    @Test
    void 상태_조회를_20회_반복해도_부작용이_없다() throws Exception {
        Long requestId = receiptService.receiveEnrollment(memberAId, courseId);
        worker.drainQueue();

        long enrollmentCountBefore = enrollmentRepository.count();
        long waitlistCountBefore = waitlistEntryRepository.count();
        long requestCountBefore = requestRepository.count();
        RequestResult resultBefore = requestRepository.findById(requestId).orElseThrow().getResult();

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/enrollment-requests/" + requestId)
                            .header("Authorization", bearer(memberAToken)))
                    .andExpect(status().isOk());
        }

        assertThat(enrollmentRepository.count()).isEqualTo(enrollmentCountBefore);
        assertThat(waitlistEntryRepository.count()).isEqualTo(waitlistCountBefore);
        assertThat(requestRepository.count()).isEqualTo(requestCountBefore);
        assertThat(requestRepository.findById(requestId).orElseThrow().getResult()).isEqualTo(resultBefore);
    }
}
