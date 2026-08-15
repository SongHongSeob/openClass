package com.hongseob.openclass_ap.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 엔티티 (plan.md §C.1 데이터 모델).
 *
 * <p>role은 항상 {@link MemberRole#MEMBER}로 생성된다 — 회원가입 API를 통해 ADMIN이
 * 생성되는 경로는 존재하지 않는다 (REQ-SIGNUP-005 / INV-AUTH-002).</p>
 */
@Entity
@Table(name = "member", uniqueConstraints = @UniqueConstraint(name = "uk_member_email", columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Member(String email, String passwordHash, String name, MemberRole role) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 회원가입 API가 사용하는 유일한 생성 진입점.
     * 요청 본문에 role이 포함되어 있어도 무시되며 항상 MEMBER로 생성된다
     * (REQ-SIGNUP-005 / INV-AUTH-002 — 이 메서드가 role 파라미터를 아예
     * 받지 않는 것 자체가 권한 상승 경로를 원천 차단하는 설계다).
     */
    public static Member createMember(String rawEmail, String passwordHash, String name) {
        return new Member(rawEmail, passwordHash, name, MemberRole.MEMBER);
    }

    /**
     * 이메일 정규화의 단일 진입점 (REQ-SIGNUP-007). 저장 시점(이 클래스의 생성자)과
     * 조회 시점(MemberService) 양쪽에서 이 메서드로만 정규화를 수행한다 — 여러
     * 호출부에서 각각 toLowerCase()를 호출하는 방식은 누락 경로를 만들므로 금지한다
     * (plan.md §C.1).
     */
    public static String normalizeEmail(String rawEmail) {
        return rawEmail == null ? null : rawEmail.trim().toLowerCase(Locale.ROOT);
    }
}
