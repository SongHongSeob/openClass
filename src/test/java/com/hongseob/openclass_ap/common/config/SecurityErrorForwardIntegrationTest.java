package com.hongseob.openclass_ap.common.config;

import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPEC-AUTH-001 in-place amendment (2026-08-30) — {@code /error}가 {@code permitAll}
 * 목록에 없어 서블릿 컨테이너의 내부 에러 페이지 포워드({@code /error})가 Spring
 * Security의 {@code anyRequest().authenticated()}에 걸려 401(빈 본문)로 대체되는
 * 회귀를 재현·검증한다.
 *
 * <p><b>왜 MockMvc가 아닌 실제 서버(RANDOM_PORT)인가</b> — {@code MockMvc}는
 * {@code DefaultHandlerExceptionResolver}가 예외를 해석한 결과를 곧바로 캡처하며,
 * 서블릿 컨테이너가 실제로 수행하는 {@code /error} 내부 포워드를 재현하지 않는다.
 * 이 결함은 {@link com.hongseob.openclass_ap.member.SignupIntegrationTest}의
 * MockMvc 기반 400 검증 테스트가 통과하는 동안에도 실서버에서는 401을 반환하는
 * MockMvc/실서버 패리티 갭이었다 — {@code TestRestTemplate} + 실제 내장 서블릿
 * 컨테이너(RANDOM_PORT)만이 이 경로를 재현할 수 있다.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SecurityErrorForwardIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void cleanUp() {
        courseRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // 회귀 재현 (1/3) — 공개 엔드포인트: @Valid 실패(비밀번호 8자 미만)가
    // 401이 아닌 400 + 에러 본문으로 반환되어야 한다.
    @Test
    void 짧은_비밀번호로_회원가입하면_401이_아닌_400과_에러_본문이_반환된다() {
        String body = """
                {"email":"repro@local.test","password":"short1","name":"Repro"}
                """;
        HttpEntity<String> request = new HttpEntity<>(body, jsonHeaders());

        ResponseEntity<String> response =
                restTemplate.postForEntity(url("/api/auth/signup"), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotBlank();
        assertThat(memberRepository.count()).isZero();
    }

    // 회귀 재현 (2/3) — 공개 엔드포인트: 파싱 불가능한(malformed) JSON 요청이
    // 401이 아닌 400으로 반환되어야 한다 (HttpMessageNotReadableException 경로).
    @Test
    void 회원가입에_깨진_JSON을_보내면_401이_아닌_400이_반환된다() {
        String malformedJson = "{\"email\":";
        HttpEntity<String> request = new HttpEntity<>(malformedJson, jsonHeaders());

        ResponseEntity<String> response =
                restTemplate.postForEntity(url("/api/auth/signup"), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // 회귀 재현 (3/3) — blast radius 스팟체크: 로그인 엔드포인트에 깨진 JSON을
    // 보내도 동일하게 401이 아닌 400이어야 한다 (같은 /error 포워드 경로 공유).
    @Test
    void 로그인에_깨진_JSON을_보내면_401이_아닌_400이_반환된다() {
        String malformedJson = "{\"email\":";
        HttpEntity<String> request = new HttpEntity<>(malformedJson, jsonHeaders());

        ResponseEntity<String> response =
                restTemplate.postForEntity(url("/api/auth/login"), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // 회귀 재현 (blast radius 스팟체크 2/2) — 인증된(ADMIN) 보호 엔드포인트에서도
    // @Valid 실패는 401이 아닌 400이어야 한다 (인증 여부와 무관하게 동일 결함).
    @Test
    void ADMIN_토큰으로_강좌명_누락_요청을_보내면_401이_아닌_400이_반환된다() {
        Member admin = memberRepository.save(Member.createAdmin("admin-repro@example.com", "hash", "관리자"));
        String token = jwtTokenProvider.generateToken(admin);

        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        String body = """
                {"title":"","description":"설명","capacity":10,"startsAt":"2027-01-01T00:00:00","endsAt":"2027-01-31T00:00:00"}
                """;
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(url("/api/admin/courses"), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(courseRepository.findAll()).isEmpty();
    }
}
