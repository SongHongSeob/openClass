package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.enrollment.dto.EnrollmentReceiptResponse;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.member.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 수강신청 접수 API (plan.md §F M1 — {@code POST
 * /api/courses/{courseId}/enrollments}, design.md §8). 인증은 이 컨트롤러가
 * 아니라 {@code SecurityConfig}의 {@code anyRequest().authenticated()} 기본
 * 규칙이 처리한다 — 이 경로는 어떤 {@code permitAll} 매처에도 걸리지 않는다
 * (REQ-QUE-006, AC-ENR-002; {@code SecurityConfig.java}는 이 SPEC의 PRESERVE
 * 대상이라 새 인가 규칙을 추가하지 않는다).
 *
 * <p>상태 조회({@code GET /api/enrollment-requests/{requestId}})와 취소
 * ({@code DELETE /api/enrollments/{enrollmentId}})는 각각 M3·M4가 추가한다
 * (design.md §8).</p>
 */
@RestController
@RequestMapping("/api/courses")
public class EnrollmentController {

    private final EnrollmentReceiptService receiptService;
    private final MemberRepository memberRepository;

    public EnrollmentController(EnrollmentReceiptService receiptService, MemberRepository memberRepository) {
        this.receiptService = receiptService;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/{courseId}/enrollments")
    public ResponseEntity<EnrollmentReceiptResponse> receive(
            @PathVariable Long courseId, Authentication authentication) {
        Long memberId = resolveMemberId(authentication);
        Long requestId = receiptService.receiveEnrollment(memberId, courseId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new EnrollmentReceiptResponse(requestId));
    }

    /**
     * JWT 필터({@code member.jwt.JwtAuthenticationFilter}, PRESERVE 대상)는
     * 이메일을 인증 주체(principal)로 설정한다 — 회원 식별자를 클레임에 담지
     * 않는다. 그래서 여기서 이메일로 {@code Member}를 조회해 식별자를 얻는다.
     * M3(상태 조회)·M4(취소)도 동일한 조회가 필요할 것이므로, 이 메서드가
     * 재사용 후보의 최초 형태다.
     */
    private Long resolveMemberId(Authentication authentication) {
        String email = authentication.getName();
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("인증된 토큰의 회원을 찾을 수 없습니다: " + email))
                .getId();
    }
}
