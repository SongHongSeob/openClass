package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.member.dto.MemberResponse;
import com.hongseob.openclass_ap.member.dto.MemberRoleUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 회원 관리 API — 전체 회원 목록 조회 및 역할 승격/강등.
 *
 * <p>인가는 {@code SecurityConfig}의 기존 {@code /api/admin/**} → hasRole("ADMIN")
 * 규칙이 처리한다 — 이 컨트롤러는 새 인가 규칙을 정의하지 않는다.</p>
 */
@RestController
@RequestMapping("/api/admin/members")
public class MemberAdminController {

    private final MemberService memberService;

    public MemberAdminController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> list() {
        List<MemberResponse> members = memberService.listAll().stream()
                .map(MemberResponse::from)
                .toList();
        return ResponseEntity.ok(members);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<MemberResponse> updateRole(
            @PathVariable Long id, @Valid @RequestBody MemberRoleUpdateRequest request, Authentication authentication) {
        Member updated = memberService.changeRole(id, request.role(), authentication.getName());
        return ResponseEntity.ok(MemberResponse.from(updated));
    }
}
