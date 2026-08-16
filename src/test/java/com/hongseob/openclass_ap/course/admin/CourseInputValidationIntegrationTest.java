package com.hongseob.openclass_ap.course.admin;

import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 서버 측 입력 검증 통합 테스트 — AC-NFR-001 (M4).
 *
 * <p>강좌명 누락·종료 일시가 시작 일시보다 이른 값·정원이 정수가 아닌 값 3종을
 * 각각 검증한다(REQ-NFR-001). 강좌명 누락과 종료/시작 일시 역전은
 * {@code CourseCreateRequest}의 Bean Validation 제약({@code @NotBlank},
 * {@link com.hongseob.openclass_ap.common.validation.ValidDateRange})이 400으로
 * 매핑하고, 정원이 정수가 아닌 값은 Jackson 역직렬화 실패가 Spring Boot 기본
 * {@code HttpMessageNotReadableException} 처리로 400이 된다 — 두 경로 모두
 * {@code GlobalExceptionHandler}에 별도 핸들러를 추가하지 않는다(plan.md §C.4.2
 * "새 @RestControllerAdvice 클래스를 만들지 않는다"와 같은 이유로, 이미 Spring
 * 기본 처리가 존재하는 예외에 대해서도 중복 핸들러를 추가하지 않는다).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CourseInputValidationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        memberRepository.deleteAll();
        Member admin = memberRepository.save(Member.createAdmin("admin@example.com", "hash", "관리자"));
        adminToken = jwtTokenProvider.generateToken(admin);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // AC-NFR-001 (i) — 강좌명 누락
    @Test
    void 강좌명이_누락되면_400이고_강좌가_생성되지_않는다() throws Exception {
        String body = """
                {"title":"","description":"설명","capacity":10,"startsAt":"2027-01-01T00:00:00","endsAt":"2027-01-31T00:00:00"}
                """;

        mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(courseRepository.findAll()).isEmpty();
    }

    // AC-NFR-001 (ii) — 종료 일시가 시작 일시보다 이른 값
    @Test
    void 종료일시가_시작일시보다_이르면_400이고_강좌가_생성되지_않는다() throws Exception {
        String body = """
                {"title":"역전강좌","description":"설명","capacity":10,"startsAt":"2027-01-31T00:00:00","endsAt":"2027-01-01T00:00:00"}
                """;

        mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(courseRepository.findAll()).isEmpty();
    }

    // AC-NFR-001 (iii) — 정원이 정수가 아닌 값
    @Test
    void 정원이_정수가_아니면_400이고_강좌가_생성되지_않는다() throws Exception {
        String body = """
                {"title":"정원형식오류","description":"설명","capacity":"열개","startsAt":"2027-01-01T00:00:00","endsAt":"2027-01-31T00:00:00"}
                """;

        mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(courseRepository.findAll()).isEmpty();
    }
}
