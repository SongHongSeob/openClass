package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MemberRepository 슬라이스 테스트 — 실제 PostgreSQL(Testcontainers)에서 유니크
 * 제약이 실제로 강제되는지 검증한다 (AC-AUTH-002, INV-AUTH-004).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 동일_이메일을_직접_저장하면_DB_유니크_제약_위반_예외가_발생한다() {
        // Given
        memberRepository.saveAndFlush(Member.createMember("dup@example.com", "hash-1", "First"));

        // When / Then — 애플리케이션 계층(서비스의 existsByEmail 체크)을 우회하여
        // 리포지토리를 직접 호출해도 DB 유니크 제약이 저장을 거부해야 한다.
        assertThatThrownBy(() ->
                memberRepository.saveAndFlush(Member.createMember("dup@example.com", "hash-2", "Second"))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByEmail은_저장된_이메일에_대해_true를_반환한다() {
        // Given
        memberRepository.saveAndFlush(Member.createMember("exists@example.com", "hash", "Name"));

        // When / Then
        assertThat(memberRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(memberRepository.existsByEmail("nobody@example.com")).isFalse();
    }
}
