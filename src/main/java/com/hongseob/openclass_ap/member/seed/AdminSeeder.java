package com.hongseob.openclass_ap.member.seed;

import com.hongseob.openclass_ap.common.config.AdminProperties;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최초 관리자 계정 시더 (plan.md §C.4). {@link ApplicationRunner}로 애플리케이션
 * 기동 시 1회 실행된다. 관리자 계정이 이미 존재하면 아무것도 하지 않는다 —
 * {@code member} 행 수가 늘지 않고 기존 관리자의 {@code password_hash}도 바뀌지
 * 않는다(멱등, REQ-SEED-002 / AC-AUTH-017).
 *
 * <p>초기 비밀번호는 {@link AdminProperties}를 통해 프로퍼티로만 주입되며 이
 * 클래스와 로그 출력 어디에도 리터럴로 남지 않는다(REQ-SEED-001 / REQ-SEED-003 /
 * AC-AUTH-016, REQ-NFR-002 / AC-AUTH-018).</p>
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public AdminSeeder(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
            AdminProperties adminProperties) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedEmail = Member.normalizeEmail(adminProperties.email());
        if (memberRepository.existsByEmail(normalizedEmail)) {
            log.info("관리자 계정이 이미 존재하여 시더를 건너뜁니다.");
            return;
        }
        String passwordHash = passwordEncoder.encode(adminProperties.password());
        Member admin = Member.createAdmin(normalizedEmail, passwordHash, "Admin");
        memberRepository.save(admin);
        log.info("최초 관리자 계정을 생성했습니다.");
    }
}
