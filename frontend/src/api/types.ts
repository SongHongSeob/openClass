// @MX:ANCHOR: [AUTO] 백엔드 응답 타입의 단일 선언 지점 (REQ-NFR-005). M2~M6의
// 화면 10개 전부가 이 모듈에서 타입을 import한다 — 화면별 중복 선언 금지.
// @MX:REASON: design.md §A.1 — 이 SPEC에서 가장 되돌리기 어려운 결정이다. 형태가
// 바뀌면 전 화면이 영향받는다.

/**
 * 회원 역할. 백엔드 enum(`MemberRole.java`)은 `MEMBER`·`ADMIN` 2종으로 고정되어
 * 있고(v1 범위, 증가 계획 없음) 아래 상태 유니온들과 달리 닫힌 유니온으로 둔다.
 *
 * 참고: design.md §A.1은 `"USER" | "ADMIN"`으로 적었으나, 실제 백엔드 enum은
 * `MEMBER`/`ADMIN`이다(`member/MemberRole.java`,
 * `JwtTokenProvider.generateToken`의 `member.getRole().name()` 클레임으로 직접
 * 확인). design.md prose가 이 필드에서 낡았을 뿐 규범이 아니므로, 실제 관측된
 * 소스를 따른다(no-unobserved-claim 원칙).
 */
export type MemberRole = 'MEMBER' | 'ADMIN'

/** SignupResponse(id, email, role) → SignupResult */
export interface SignupResult {
  id: number
  email: string
  role: MemberRole
}

/** LoginResponse(accessToken) → LoginResult */
export interface LoginResult {
  accessToken: string
}

/**
 * 강좌 모집 상태. 백엔드 enum(`CourseStatus.java`)은 DB CHECK 제약으로
 * `OPEN`/`CLOSED` 2종만 가능하지만, 이 모듈의 다른 상태 유니온들과 일관되게
 * `(string & {})` 분기로 미지 값을 허용한다 — 인식하지 못한 값은 타입·런타임
 * 중단이 아니라 대체 표시로 낮춰진다.
 */
export type CourseStatus = 'OPEN' | 'CLOSED' | (string & {})

/**
 * CourseResponse → Course. `startsAt`/`endsAt`는 문자열 그대로 둔다 — 백엔드
 * `LocalDateTime`은 타임존 없이 직렬화되므로, 여기서 즉시 `Date`로 변환하면
 * 브라우저 로컬 타임존이 암묵 적용된다. 표시 직전에만 변환한다(design.md §A.1).
 */
export interface Course {
  id: number
  title: string
  description: string
  capacity: number
  enrolledCount: number
  remainingCapacity: number
  startsAt: string
  endsAt: string
  status: CourseStatus
}

/** CoursePageResponse → CoursePage (REQ-CAT-002 — 서버 기준 페이지 이동). */
export interface CoursePage {
  items: Course[]
  totalElements: number
  totalPages: number
  currentPage: number
}

/** EnrollmentReceiptResponse(requestId) → Receipt. 접수·취소(202) 공통 응답. */
export interface Receipt {
  requestId: number
}

/**
 * 큐 요청(접수·취소)의 종단 상태 도메인(`SPEC-ENROLLMENT-001` §A.4). `PENDING`만
 * 비종단이며, 종단 판정은 이 리터럴 목록과의 대조가 아니라 "`PENDING`이 아님"으로
 * 수행한다(REQ-ENR-004, INV-FE-002). `(string & {})` 분기로 유니온을 열어 두어
 * 백엔드가 종단 값을 추가해도 컴파일·런타임이 무너지지 않는다(REQ-ENR-009).
 */
export type EnrollmentStatus =
  | 'PENDING'
  | 'SUCCESS'
  | 'WAITLISTED'
  | 'CLOSED'
  | 'REJECTED'
  | 'FAILED'
  | 'CANCELLED'
  | 'PROMOTED'
  | 'NOOP'
  | (string & {})

/** EnrollmentStatusResponse(requestId, status, waitlistPosition) → RequestStatus */
export interface RequestStatus {
  requestId: number
  status: EnrollmentStatus
  waitlistPosition: number | null
}

/**
 * EnrollmentListItemResponse → EnrollmentListItem ("내 수강신청" 목록,
 * `GET /api/enrollments/mine`). `status`는 실제로는 `'ENROLLED'` 고정(백엔드가
 * 활성 행만 반환)이지만 다른 상태 필드와 동일하게 관대한 타입을 유지한다.
 * `enrolledAt`도 위 `Course`의 날짜 처리 규칙과 동일하게 문자열로 둔다.
 */
export interface EnrollmentListItem {
  enrollmentId: number
  courseId: number
  courseTitle: string
  status: EnrollmentStatus
  enrolledAt: string
}

/**
 * `waitlistEntryId`와 `position`을 타입 층위에서도 구별한다(AC-FE-109,
 * sync-auditor F1 — `.moai/specs/SPEC-FRONTEND-001/progress.md` §H.5).
 * 둘 다 백엔드에서 `Long`이라 필드명만으로는 구별되지 않으므로,
 * `waitlistEntryId`를 브랜디드 타입으로 만들어 `position`(순수 `number`)을
 * 취소 함수 인자로 넘기는 코드가 타입 검사에서 거부되게 한다(design.md §A.1
 * 판정 기준: "position을 취소 함수의 인자로 넘기는 코드가 타입 검사에서
 * 거부되는가").
 */
export type WaitlistEntryId = number & { readonly __brand: 'WaitlistEntryId' }

/**
 * WaitlistListItemResponse → WaitlistListItem ("내 대기명단" 목록,
 * `GET /api/waitlist-entries/mine`).
 *
 * `waitlistEntryId`와 `position`을 의도적으로 별개 필드로 구조화한다
 * (INV-FE-009) — 둘 다 백엔드에서 `Long`이라 필드명 외에는 런타임 구별 수단이
 * 없었으나, `waitlistEntryId`는 이제 `WaitlistEntryId` 브랜디드 타입이라
 * `position`과 구조적으로도 구별된다.
 * M6의 취소 호출(`DELETE /api/waitlist-entries/{entryId}`)은 반드시
 * `waitlistEntryId`를 읽어야 하며, `position`을 넣으면 본인 소유의 엉뚱한 행이
 * 조용히 취소된다(spec.md §A.4) — 지금은 타입 검사가 이를 컴파일 타임에 막는다.
 *
 * `position`은 **강좌 단위** 순번이며 전역 순위가 아니다(REQ-CNL-010 /
 * INV-FE-011) — 이 값을 렌더링하는 화면은 반드시 `courseTitle`과 함께 표시하고
 * 전역 대기 순위처럼 보이게 표시해서는 안 된다.
 */
export interface WaitlistListItem {
  waitlistEntryId: WaitlistEntryId
  courseId: number
  courseTitle: string
  position: number
  status: EnrollmentStatus
}
