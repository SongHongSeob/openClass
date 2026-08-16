package com.hongseob.openclass_ap.member.seed;

import com.hongseob.openclass_ap.common.config.AdminProperties;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.member.MemberRole;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminSeeder 통합 테스트 (plan.md §C.4). AC-AUTH-016(관리자 계정 생성)과
 * AC-AUTH-017(시더 멱등성)을 검증한다.
 *
 * <p>Spring 컨텍스트가 기동될 때 {@link AdminSeeder}가 {@code ApplicationRunner}로
 * 이미 1회 실행되지만, 그 시점은 테스트 메서드 실행 이전이라 직접 관찰할 수 없다.
 * 각 테스트는 {@code memberRepository.deleteAll()}로 빈 상태를 재현한 뒤
 * {@code adminSeeder.run(null)}을 직접 호출하여 "시더 실행 경로" 자체를 검증한다
 * (plan.md §C.4 — "테스트에서는 프로퍼티 주입으로 시더 동작을 제어한다").</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminSeederTest extends AbstractIntegrationTest {

    @Autowired
    private AdminSeeder adminSeeder;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AdminProperties adminProperties;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String normalizedAdminEmail() {
        return Member.normalizeEmail(adminProperties.email());
    }

    // AC-AUTH-016 — 관리자 계정 생성
    @Test
    void 시더_실행시_관리자_계정이_생성되고_해당_계정으로_로그인하면_ADMIN_클레임_토큰이_발급된다() throws Exception {
        memberRepository.deleteAll();

        adminSeeder.run(null);

        Optional<Member> admin = memberRepository.findByEmail(normalizedAdminEmail());
        assertThat(admin).isPresent();
        assertThat(admin.get().getRole()).isEqualTo(MemberRole.ADMIN);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(adminProperties.email(), adminProperties.password())))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        Claims claims = jwtTokenProvider.parseClaims(token);
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    // AC-AUTH-017 — 시더 멱등성
    @Test
    void 시더를_두번_실행해도_행수가_증가하지_않고_기존_관리자의_비밀번호_해시가_변하지_않는다() {
        memberRepository.deleteAll();
        adminSeeder.run(null);

        long countAfterFirstRun = memberRepository.count();
        String hashAfterFirstRun = memberRepository.findByEmail(normalizedAdminEmail())
                .orElseThrow()
                .getPasswordHash();

        adminSeeder.run(null);

        assertThat(memberRepository.count()).isEqualTo(countAfterFirstRun);
        assertThat(memberRepository.findByEmail(normalizedAdminEmail()).orElseThrow().getPasswordHash())
                .isEqualTo(hashAfterFirstRun);
    }
}
