package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.common.exception.DuplicateEmailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 유스케이스 (REQ-SIGNUP-001 ~ 007).
 */
@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 회원가입을 처리한다. 역할은 항상 MEMBER로 고정된다 — 이 메서드의 시그니처
     * 자체가 role 파라미터를 받지 않으므로 호출부(컨트롤러)가 요청 본문의 role을
     * 넘겨받더라도 여기까지 전달할 방법이 없다 (REQ-SIGNUP-005 / INV-AUTH-002).
     */
    @Transactional
    public Member signup(String rawEmail, String rawPassword, String name) {
        String normalizedEmail = Member.normalizeEmail(rawEmail);
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }
        String passwordHash = passwordEncoder.encode(rawPassword);
        Member member = Member.createMember(normalizedEmail, passwordHash, name);
        return memberRepository.save(member);
    }
}
