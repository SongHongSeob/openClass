package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.member.dto.LoginRequest;
import com.hongseob.openclass_ap.member.dto.LoginResponse;
import com.hongseob.openclass_ap.member.dto.SignupRequest;
import com.hongseob.openclass_ap.member.dto.SignupResponse;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 공개 API (spec.md §A.2 — 회원가입·로그인). 이 SPEC이 소유한 공개
 * 엔드포인트로, permitAll 대상이다 (REQ-AUTHZ-006 — 실제 경로 인가 규칙은 M3).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(MemberService memberService, JwtTokenProvider jwtTokenProvider) {
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        Member member = memberService.signup(request.email(), request.password(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(SignupResponse.from(member));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Member member = memberService.login(request.email(), request.password());
        String accessToken = jwtTokenProvider.generateToken(member);
        return ResponseEntity.ok(new LoginResponse(accessToken));
    }
}
