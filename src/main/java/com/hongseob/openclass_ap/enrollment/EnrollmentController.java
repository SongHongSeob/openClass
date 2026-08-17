package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.common.exception.InvalidCourseIdException;
import com.hongseob.openclass_ap.enrollment.dto.EnrollmentReceiptResponse;
import com.hongseob.openclass_ap.enrollment.dto.EnrollmentStatusResponse;
import com.hongseob.openclass_ap.enrollment.query.EnrollmentStatusQueryService;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.member.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 수강신청 접수 / 상태 조회 API (design.md §8). 접수는 {@code POST
 * /api/courses/{courseId}/enrollments}(plan.md §F M1), 상태 조회는 {@code GET
 * /api/enrollment-requests/{requestId}}(M3) — 두 경로의 접두사가 다르므로 클래스
 * 수준 {@code @RequestMapping}을 두지 않고 메서드마다 전체 경로를 명시한다.
 * 인증은 이 컨트롤러가 아니라 {@code SecurityConfig}의 {@code
 * anyRequest().authenticated()} 기본 규칙이 처리한다 — 세 경로 모두 어떤
 * {@code permitAll} 매처에도 걸리지 않는다(REQ-QUE-006, REQ-STS-001,
 * REQ-CNL-001; {@code SecurityConfig.java}는 이 SPEC의 PRESERVE 대상이라 새
 * 인가 규칙을 추가하지 않는다 — 관리자 대리 취소용 규칙도 추가하지 않는다,
 * REQ-CNL-004).</p>
 */
@RestController
public class EnrollmentController {

    private final EnrollmentReceiptService receiptService;
    private final EnrollmentStatusQueryService statusQueryService;
    private final MemberRepository memberRepository;

    public EnrollmentController(EnrollmentReceiptService receiptService,
            EnrollmentStatusQueryService statusQueryService, MemberRepository memberRepository) {
        this.receiptService = receiptService;
        this.statusQueryService = statusQueryService;
        this.memberRepository = memberRepository;
    }

    /**
     * courseId 형식 검증(M6, REQ-NFR-003, AC-ENR-046)을 DB 조회보다 먼저
     * 수행한다 — 0 이하는 형식 오류로 간주해 400을 반환하고 큐 행을 적재하지
     * 않는다. 숫자가 아닌 값은 여기 도달하기 전에 Spring의 기본
     * {@code MethodArgumentTypeMismatchException} 처리가 이미 400으로
     * 거부한다. 존재하지 않는(하지만 형식은 유효한) 강좌 식별자는 이 검증을
     * 통과한 뒤 {@link EnrollmentReceiptService}가 여전히 404({@link
     * com.hongseob.openclass_ap.common.exception.CourseNotFoundException})로
     * 거부한다 — 두 응답의 경계는 겹치지 않는다.
     */
    @PostMapping("/api/courses/{courseId}/enrollments")
    public ResponseEntity<EnrollmentReceiptResponse> receive(
            @PathVariable Long courseId, Authentication authentication) {
        if (courseId <= 0) {
            throw new InvalidCourseIdException(courseId);
        }
        Long memberId = resolveMemberId(authentication);
        Long requestId = receiptService.receiveEnrollment(memberId, courseId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new EnrollmentReceiptResponse(requestId));
    }

    /**
     * 상태 조회 API(M3, REQ-STS-001~002, AC-ENR-024/025). 본인 소유가 아니거나
     * 존재하지 않는 요청은 동일하게 404로 응답한다 — 소유권 검증은
     * {@link EnrollmentStatusQueryService}가 수행한다.
     */
    @GetMapping("/api/enrollment-requests/{requestId}")
    public ResponseEntity<EnrollmentStatusResponse> getStatus(
            @PathVariable Long requestId, Authentication authentication) {
        Long memberId = resolveMemberId(authentication);
        return ResponseEntity.ok(statusQueryService.getStatus(requestId, memberId));
    }

    /**
     * 확정 취소 접수 API(M4, REQ-CNL-001/002, design.md §8). 큐 적재만
     * 수행한다 — 확정 취소·{@code enrolled_count} 변경은 절대 하지 않는다
     * (REQ-WRK-002). 1차 소유권 검증(AC-ENR-036, 감사 D12)은 {@link
     * EnrollmentReceiptService#receiveCancel}이 수행하며, 소유자가 아니거나
     * 존재하지 않으면 큐 행을 적재하지 않고 404를 반환한다(관리자 대리
     * 취소용 별도 경로는 존재하지 않는다 — REQ-CNL-004, AC-ENR-038).
     */
    @DeleteMapping("/api/enrollments/{enrollmentId}")
    public ResponseEntity<EnrollmentReceiptResponse> cancel(
            @PathVariable Long enrollmentId, Authentication authentication) {
        Long memberId = resolveMemberId(authentication);
        Long requestId = receiptService.receiveCancel(memberId, enrollmentId);
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
