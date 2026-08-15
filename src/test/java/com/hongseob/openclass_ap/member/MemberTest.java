package com.hongseob.openclass_ap.member;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Member 엔티티 단위 테스트.
 * REQ-SIGNUP-005 / REQ-SIGNUP-007 / INV-AUTH-002 — 역할 고정 + 이메일 정규화.
 */
class MemberTest {

    @Test
    void createMember은_항상_MEMBER_역할을_부여한다() {
        // Given / When
        Member member = Member.createMember("a@example.com", "hashed-value", "Alice");

        // Then
        assertThat(member.getRole()).isEqualTo(MemberRole.MEMBER);
    }

    @ParameterizedTest
    @CsvSource({
            "'  A@Example.COM  ', a@example.com",
            "a@example.com, a@example.com",
            "MiXeD@Case.Com, mixed@case.com"
    })
    void createMember은_이메일을_트림_후_소문자로_정규화한다(String rawEmail, String expected) {
        // Given / When
        Member member = Member.createMember(rawEmail, "hashed-value", "Alice");

        // Then
        assertThat(member.getEmail()).isEqualTo(expected);
    }

    @Test
    void normalizeEmail은_null을_그대로_반환한다() {
        assertThat(Member.normalizeEmail(null)).isNull();
    }

    @Test
    void createMember은_전달받은_비밀번호_해시를_그대로_저장한다() {
        // Given / When
        Member member = Member.createMember("a@example.com", "already-hashed", "Alice");

        // Then — 엔티티는 해싱을 수행하지 않는다. 해싱은 MemberService의 책임이다.
        assertThat(member.getPasswordHash()).isEqualTo("already-hashed");
    }
}
