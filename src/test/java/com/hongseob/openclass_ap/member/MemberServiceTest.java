package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.common.exception.DuplicateEmailException;
import com.hongseob.openclass_ap.common.exception.InvalidCredentialsException;
import java.util.Optional;
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
import static org.mockito.ArgumentMatchers.eq;
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

    // AC-AUTH-007 — 로그인 성공
    @Test
    void login은_정규화된_이메일과_올바른_비밀번호로_회원을_반환한다() {
        // Given
        var service = new MemberService(memberRepository, passwordEncoder);
        Member member = Member.createMember("a@example.com", "bcrypt-hash", "Alice");
        when(memberRepository.findByEmail("a@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password1", "bcrypt-hash")).thenReturn(true);

        // When
        Member result = service.login("  A@Example.COM  ", "password1");

        // Then
        assertThat(result).isSameAs(member);
    }

    // AC-AUTH-008 — 로그인 실패 구별 불가 (틀린 비밀번호)
    @Test
    void login은_비밀번호가_틀리면_InvalidCredentialsException을_던진다() {
        // Given
        var service = new MemberService(memberRepository, passwordEncoder);
        Member member = Member.createMember("a@example.com", "bcrypt-hash", "Alice");
        when(memberRepository.findByEmail("a@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> service.login("a@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // AC-AUTH-008 — 로그인 실패 구별 불가 (미가입 이메일)
    @Test
    void login은_가입되지_않은_이메일이면_InvalidCredentialsException을_던진다() {
        // Given — 더미 해시 계산(encode)은 생성자에서 즉시 호출되므로 서비스 생성 전에 스텁한다.
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        var service = new MemberService(memberRepository, passwordEncoder);
        when(memberRepository.findByEmail("zzz@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> service.login("zzz@example.com", "any-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // 계정 열거 방지 — 미가입 이메일에도 PasswordEncoder.matches가 호출된다(더미 해시 비교)
    @Test
    void login은_미가입_이메일이어도_비밀번호_비교_연산을_수행한다() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        var service = new MemberService(memberRepository, passwordEncoder);
        when(memberRepository.findByEmail("zzz@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When
        try {
            service.login("zzz@example.com", "any-password");
        } catch (InvalidCredentialsException ignored) {
            // expected
        }

        // Then
        verify(passwordEncoder).matches(eq("any-password"), anyString());
    }
}
