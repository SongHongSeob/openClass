package com.hongseob.openclass_ap.waitlist;

import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.waitlist.dto.WaitlistListItemResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대기명단 취소 API(M4, design.md §8 — {@code DELETE
 * /api/waitlist-entries/{entryId}})와 내 대기명단 목록 조회 API(M7, v0.3.0
 * 개정, REQ-LST-002 — {@code GET /api/waitlist-entries/mine}). 인증은 이
 * 컨트롤러가 아니라 {@code SecurityConfig}의 {@code
 * anyRequest().authenticated()} 기본 규칙이 처리한다(REQ-WL-008, REQ-LST-005;
 * {@code SecurityConfig.java}는 이 SPEC의 PRESERVE 대상이라 새 인가 규칙을
 * 추가하지 않는다).
 */
@RestController
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final MemberRepository memberRepository;

    public WaitlistController(WaitlistService waitlistService, MemberRepository memberRepository) {
        this.waitlistService = waitlistService;
        this.memberRepository = memberRepository;
    }

    @DeleteMapping("/api/waitlist-entries/{entryId}")
    public ResponseEntity<Void> cancel(@PathVariable Long entryId, Authentication authentication) {
        Long memberId = resolveMemberId(authentication);
        waitlistService.cancel(entryId, memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 회원 식별자를 받는 파라미터가 존재하지 않는다 — 반환 범위는 오직
     * {@code Authentication}에서 유도한 회원 식별자로만 결정된다(REQ-LST-003,
     * spec.md §A.6.4).
     */
    @GetMapping("/api/waitlist-entries/mine")
    public ResponseEntity<List<WaitlistListItemResponse>> listMine(Authentication authentication) {
        Long memberId = resolveMemberId(authentication);
        return ResponseEntity.ok(waitlistService.listMine(memberId));
    }

    /**
     * {@code enrollment.EnrollmentController#resolveMemberId}와 동일한
     * 패턴이다 — JWT 필터가 이메일을 인증 주체(principal)로 설정하므로
     * 이메일로 {@code Member}를 조회해 식별자를 얻는다. 컨트롤러가 다른
     * 패키지에 있어 재사용하지 않고 그대로 반복한다(Scope Discipline —
     * 이 SPEC 범위 밖의 기존 파일을 리팩터링하지 않는다).
     */
    private Long resolveMemberId(Authentication authentication) {
        String email = authentication.getName();
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("인증된 토큰의 회원을 찾을 수 없습니다: " + email))
                .getId();
    }
}
