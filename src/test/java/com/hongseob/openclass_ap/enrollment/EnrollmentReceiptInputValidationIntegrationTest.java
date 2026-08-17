package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 접수 API 서버 측 입력 검증 통합 테스트(M6, REQ-NFR-003, AC-ENR-046). 강좌
 * 식별자가 숫자가 아닌 값·음수·(0을 "명시적으로 유효 범위를 벗어난" 값으로
 * 취급해 누락에 준하는 경우로) 형식적으로 유효하지 않은 경우를 다룬다 — 세
 * 경우 모두 큐 행이 생성되지 않고 400을 반환한다. 존재하지만 형식은 유효한
 * courseId가 미존재 강좌를 가리키는 경우는 AC-ENR-003(404)의 영역이며 이
 * 클래스가 재검증하지 않는다 — 두 응답의 경계가 겹치지 않음을 별도로
 * 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentReceiptInputValidationIntegrationTest extends AbstractIntegrationTest {

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

    private String memberToken;
    private Long courseId;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        Member member = memberRepository.save(Member.createMember("validator@example.com", "hash", "학생"));
        memberToken = jwtTokenProvider.generateToken(member);

        Course course = courseRepository.save(Course.create("검증용강좌", "설명", 10,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30)));
        courseId = course.getId();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // AC-ENR-046 (숫자가 아닌 값) — Spring의 기본 MethodArgumentTypeMismatchException 처리가 400을 반환한다
    @Test
    void 강좌식별자가_숫자가_아니면_400이고_큐_행이_생성되지_않는다() throws Exception {
        mockMvc.perform(post("/api/courses/not-a-number/enrollments")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isBadRequest());

        assertThat(requestRepository.count()).isZero();
    }

    // AC-ENR-046 (음수 값) — 컨트롤러 진입 시점 형식 검증이 DB 조회 전에 400을 반환한다
    @Test
    void 강좌식별자가_음수이면_400이고_큐_행이_생성되지_않는다() throws Exception {
        mockMvc.perform(post("/api/courses/-1/enrollments")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COURSE_ID"));

        assertThat(requestRepository.count()).isZero();
    }

    // AC-ENR-046 (누락에 준하는 명시적 범위 밖 값 — 0) — 동일한 형식 검증 경로가 400을 반환한다
    @Test
    void 강좌식별자가_0이면_400이고_큐_행이_생성되지_않는다() throws Exception {
        mockMvc.perform(post("/api/courses/0/enrollments")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COURSE_ID"));

        assertThat(requestRepository.count()).isZero();
    }

    // 경계 확인 — 존재하지 않지만 형식은 유효한 courseId는 여전히 404다(AC-ENR-003과 겹치지 않음)
    @Test
    void 형식은_유효하지만_존재하지_않는_강좌식별자는_여전히_404다() throws Exception {
        mockMvc.perform(post("/api/courses/999999/enrollments")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));

        assertThat(requestRepository.count()).isZero();
    }

    // 경계 확인 — 형식·존재 둘 다 유효한 courseId는 정상 접수된다(무회귀)
    @Test
    void 형식과_존재_둘_다_유효한_강좌식별자는_정상_접수된다() throws Exception {
        mockMvc.perform(post("/api/courses/" + courseId + "/enrollments")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isAccepted());

        assertThat(requestRepository.count()).isEqualTo(1);
    }
}
