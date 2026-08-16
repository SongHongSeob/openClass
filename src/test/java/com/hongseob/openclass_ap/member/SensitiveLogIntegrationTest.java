package com.hongseob.openclass_ap.member;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hongseob.openclass_ap.common.config.JwtProperties;
import com.hongseob.openclass_ap.member.fixture.AuthTestFixtureController;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 민감 정보 로그 미기록 통합 테스트 (plan.md §F M4 / REQ-NFR-002).
 * 회원가입·로그인·보호 엔드포인트 호출을 각 1회 수행하는 동안 실제로 기록된
 * 로그 이벤트 전체를 캡처하여, 평문 비밀번호·비밀번호 해시·발급된 액세스
 * 토큰 문자열·서명 비밀키가 어디에도 나타나지 않음을 검증한다(AC-AUTH-018).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestFixtureController.class)
class SensitiveLogIntegrationTest extends AbstractIntegrationTest {

    private static final String PLAIN_PASSWORD = "SensitiveSecret123";
    private static final String LOG_CHECK_EMAIL = "log-check@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void cleanUp() {
        memberRepository.deleteAll();
        AuthTestFixtureController.resetProtectedCallCount();
    }

    // AC-AUTH-018 — 민감 정보 로그 미기록
    @Test
    void 회원가입_로그인_보호엔드포인트_호출_로그에_민감정보가_기록되지_않는다() throws Exception {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);

        try {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType("application/json")
                            .content("""
                                    {"email":"%s","password":"%s","name":"LogCheck"}
                                    """.formatted(LOG_CHECK_EMAIL, PLAIN_PASSWORD)))
                    .andExpect(status().isCreated());

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("""
                                    {"email":"%s","password":"%s"}
                                    """.formatted(LOG_CHECK_EMAIL, PLAIN_PASSWORD)))
                    .andExpect(status().isOk())
                    .andReturn();

            String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");

            mockMvc.perform(get("/api/test/protected")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            String passwordHash = memberRepository.findByEmail(LOG_CHECK_EMAIL)
                    .orElseThrow()
                    .getPasswordHash();

            List<String> logMessages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .filter(message -> message != null)
                    .toList();

            assertThat(logMessages).noneMatch(message -> message.contains(PLAIN_PASSWORD));
            assertThat(logMessages).noneMatch(message -> message.contains(passwordHash));
            assertThat(logMessages).noneMatch(message -> message.contains(token));
            assertThat(logMessages).noneMatch(message -> message.contains(jwtProperties.secret()));
        } finally {
            rootLogger.detachAppender(appender);
        }
    }
}
