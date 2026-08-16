package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestState;
import com.hongseob.openclass_ap.enrollment.request.RequestType;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수강신청 접수 API 통합 테스트 — AC-ENR-001, AC-ENR-002(부분), AC-ENR-003 (M1).
 *
 * <p><b>AC-ENR-002 M1 범위 주석</b>: acceptance.md 원문은 "접수·상태 조회·취소
 * API를 각각 호출"을 요구하지만, 상태 조회·취소 API는 M3·M4가 추가한다
 * (plan.md §F). 이 클래스는 접수 API만 검증한다(PASS-WITH-DEBT, M3/M4에서
 * 나머지 두 엔드포인트로 재검증 — {@code CourseSchemaIntegrationTest}가 확립한
 * 선례와 동일한 패턴).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentReceiptApiIntegrationTest extends AbstractIntegrationTest {

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

    private String memberToken;
    private Long courseId;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        Member member = memberRepository.save(Member.createMember("student@example.com", "hash", "학생"));
        memberToken = jwtTokenProvider.generateToken(member);

        Course course = courseRepository.save(Course.create("정원1강좌", "설명", 1,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30)));
        courseId = course.getId();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // AC-ENR-001
    @Test
    void 접수는_큐_적재만_수행하고_연속_3건의_순서값이_단조_증가한다() throws Exception {
        int initialEnrolledCount = courseRepository.findById(courseId).orElseThrow().getEnrolledCount();

        List<Long> requestIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String body = mockMvc.perform(post("/api/courses/" + courseId + "/enrollments")
                            .header("Authorization", bearer(memberToken)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.requestId").exists())
                    .andReturn().getResponse().getContentAsString();
            long requestId = ((Number) JsonPath.read(body, "$.requestId")).longValue();
            requestIds.add(requestId);
        }

        // 순서값이 단조 증가하고 3건 모두 서로 다르다
        assertThat(requestIds).isSorted();
        assertThat(new HashSet<>(requestIds)).hasSize(3);

        assertThat(requestRepository.count()).isEqualTo(3);
        requestRepository.findAll().forEach(request -> {
            assertThat(request.getRequestType()).isEqualTo(RequestType.ENROLL);
            assertThat(request.getState()).isEqualTo(RequestState.PENDING);
            assertThat(request.getResult()).isNull();
        });

        // 응답 시점에 enrollment 행과 course.enrolled_count가 전혀 변하지 않는다
        assertThat(enrollmentRepository.count()).isZero();
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount())
                .isEqualTo(initialEnrolledCount);
    }

    // AC-ENR-002 (M1 범위 — 접수 API만)
    @Test
    void 미인증으로_접수하면_401이고_큐_행이_생성되지_않는다() throws Exception {
        mockMvc.perform(post("/api/courses/" + courseId + "/enrollments"))
                .andExpect(status().isUnauthorized());

        assertThat(requestRepository.count()).isZero();
    }

    // AC-ENR-003
    @Test
    void 존재하지_않는_강좌로_접수하면_404이고_큐_행이_생성되지_않는다() throws Exception {
        mockMvc.perform(post("/api/courses/999999/enrollments")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));

        assertThat(requestRepository.count()).isZero();
    }
}
