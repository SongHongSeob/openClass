package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.common.exception.DuplicateEmailException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MemberService 단위 테스트 — 리포지토리·PasswordEncoder는 모두 모킹한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void signup은_정규화된_이메일로_중복_여부를_확인한다() {
        // Given
        var service = new MemberService(memberRepository, passwordEncoder);
        when(memberRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        service.signup("  A@Example.COM  ", "password1", "Alice");

        // Then
        verify(memberRepository).existsByEmail("a@example.com");
    }

    @Test
    void signup은_이미_존재하는_이메일이면_DuplicateEmailException을_던지고_저장하지_않는다() {
        // Given
        var service = new MemberService(memberRepository, passwordEncoder);
        when(memberRepository.existsByEmail("a@example.com")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> service.signup("a@example.com", "password1", "Alice"))
                .isInstanceOf(DuplicateEmailException.class);
        verify(memberRepository, never()).save(any());
    }

    @Test
    void signup은_비밀번호를_PasswordEncoder로_해싱하여_저장한다() {
        // Given
        var service = new MemberService(memberRepository, passwordEncoder);
        when(memberRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("bcrypt-hash");
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        when(memberRepository.save(memberCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        service.signup("a@example.com", "plain-password", "Alice");

        // Then
        assertThat(memberCaptor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(MemberRole.MEMBER);
    }
}
