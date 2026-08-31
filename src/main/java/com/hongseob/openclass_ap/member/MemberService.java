package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.common.exception.DuplicateEmailException;
import com.hongseob.openclass_ap.common.exception.InvalidCredentialsException;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입·로그인 유스케이스 (REQ-SIGNUP-001 ~ 007, REQ-LOGIN-001 ~ 002).
 */
@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 미가입 이메일로 로그인을 시도할 때도 동일한 시간 특성으로 비밀번호 비교
     * 연산을 수행하기 위한 더미 해시(REQ-LOGIN-002 — 계정 열거 방지). 실제 회원의
     * password_hash가 아니며 어떤 회원과도 일치하지 않는다.
     */
    private final String dummyPasswordHash;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing-safety");
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

    /**
     * 로그인을 처리한다. 미가입 이메일과 비밀번호 불일치는 동일한
     * {@link InvalidCredentialsException}으로 구분 없이 처리된다(REQ-LOGIN-002 /
     * AC-AUTH-008) — 컨트롤러·예외 핸들러 계층까지 실패 사유가 전달되지 않는다.
     */
    public Member login(String rawEmail, String rawPassword) {
        String normalizedEmail = Member.normalizeEmail(rawEmail);
        Member member = memberRepository.findByEmail(normalizedEmail).orElse(null);
        String hashToCompare = member != null ? member.getPasswordHash() : dummyPasswordHash;
        boolean matches = passwordEncoder.matches(rawPassword, hashToCompare);
        if (member == null || !matches) {
            throw new InvalidCredentialsException();
        }
        return member;
    }

    /**
     * 관리자 회원 관리 화면의 전체 회원 목록 조회. 회원 수가 적은 규모라
     * 페이지네이션을 도입하지 않는다.
     */
    public List<Member> listAll() {
        return memberRepository.findAll();
    }

    /**
     * 대상 회원의 역할을 변경한다. 호출자(ADMIN)가 자기 자신을 대상으로 지정하면
     * {@link SelfRoleChangeNotAllowedException}을 던져 거부한다.
     */
    @Transactional
    public Member changeRole(Long targetId, MemberRole newRole, String callerEmail) {
        Member target = memberRepository.findById(targetId)
                .orElseThrow(() -> new MemberNotFoundException(targetId));
        if (target.getEmail().equals(Member.normalizeEmail(callerEmail))) {
            throw new SelfRoleChangeNotAllowedException();
        }
        target.changeRole(newRole);
        return target;
    }
}
