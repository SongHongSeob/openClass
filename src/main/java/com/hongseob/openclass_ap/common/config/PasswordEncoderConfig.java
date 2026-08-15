package com.hongseob.openclass_ap.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 해시(PasswordEncoder) 전용 설정.
 *
 * <p>M1은 회원 도메인·회원가입 API만 다루며 HTTP 계층 인가 규칙은 아직 없다
 * (plan.md M1 범위). SecurityFilterChain을 정의하는 SecurityConfig는 M3에서
 * 작성되므로 이 클래스는 SecurityConfig가 아니며, M3 작업자가 SecurityConfig를
 * 작성할 때 이 Bean을 재사용하면 된다(PasswordEncoder Bean 중복 정의 방지).</p>
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
