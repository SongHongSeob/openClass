# SPEC-FRONTEND-001 — 기술 조사 (research)

> 이 문서는 `spec.md` 작성 이전에 수행한 코드베이스 조사와 기술 선택 검토의 근거를 기록한다. 여기의 사실 진술은 모두 **실제 소스 파일을 읽어 확인한 것**이며, 추정은 추정으로 표기한다.

> **0.2.0 재기준화 고지 (반드시 먼저 읽을 것).** 이 문서의 §2·§3은 원래 두 개의 백엔드 공백을 "확인됨"으로 보고했다. **백엔드가 그 조사 결과를 근거로 두 공백을 모두 닫았다.** 따라서 §2·§3의 원래 판정은 **더 이상 현재 상태가 아니다** — 각 절의 머리에 해소 사실과 확인 SHA를 명시했다. 원래 관측을 지우지 않고 남기는 이유는 두 가지다: (a) 백엔드 변경의 근거가 된 조사이므로 이력으로서 가치가 있고(`CorsProperties.java`의 javadoc이 이 문서 §2를 이름으로 인용한다), (b) "조사 시점의 사실이 이후 무효화될 수 있다"는 것 자체가 이 SPEC이 배운 교훈이기 때문이다.
>
> **이후 개정자에게**: 이 문서의 사실 진술을 인용하기 전에 **해당 소스를 다시 읽어라.** prose를 신뢰하지 말 것 — 1회차 감사가 FAIL한 주된 원인이 정확히 그것이었다.

| 관측 대상 | 조사 시점 판정 (0.1.0) | 현재 (0.2.0 재확인) | 재확인 기준점 |
|---|---|---|---|
| CORS 설정 (§2) | 없음 — 전 범위 차단 | **있음 — 해소됨** | `main` 커밋 `29a1560` |
| 취소 대상 식별자 노출 (§3) | 없음 — 취소 구현 불가 | **있음 — 해소됨** | `sync/SPEC-ENROLLMENT-001` (PR #1) M7 |
| 오류 응답 3종 분기 (§4) | 3종 | **변동 없음** | `sync/SPEC-ENROLLMENT-001` |
| JWT `role` 클레임 (§5) | 있음 | **변동 없음** | `sync/SPEC-ENROLLMENT-001` |

---

## §1 조사 범위와 방법

| 대상 | 방법 | 확인 시점 기준 |
|---|---|---|
| `common/config/SecurityConfig.java` | 전문 읽기 (0.1.0) + 재확인 (0.2.0) | `main` 브랜치 — 0.2.0 재확인 기준점은 커밋 `29a1560` |
| `common/exception/GlobalExceptionHandler.java` | `main` + `sync/SPEC-ENROLLMENT-001` 양쪽 읽기 | 두 브랜치 비교 |
| `common/response/ErrorResponse.java` | 전문 읽기 | `main` |
| 전 컨트롤러 (`AuthController`·`CourseController`·`CourseAdminController`·`EnrollmentController`·`WaitlistController`) | 전문 읽기 | `EnrollmentController`·`WaitlistController`는 `sync/SPEC-ENROLLMENT-001` |
| 전 DTO 레코드 | 전문 읽기 | 동일 |
| `member/jwt/JwtTokenProvider.java` | 전문 읽기 | `sync/SPEC-ENROLLMENT-001` |
| 엔드포인트 전수 | 전 파일 대상 매핑 애너테이션 grep | `sync/SPEC-ENROLLMENT-001` |
| CORS 설정 존재 여부 | `.moai/specs/`·`src/`·`README.md` 전체 grep | 전 범위 |

**브랜치 상태에 관한 사실 (0.2.2 갱신)**: 조사 시점에 `SPEC-ENROLLMENT-001`의 구현(`enrollment`·`waitlist` 패키지, 29개 파일)은 `main`에 병합되지 않은 채 브랜치 `sync/SPEC-ENROLLMENT-001`에 있었다. **그 상태는 해소되었다 — PR #1이 병합되었고(병합 커밋 `21eab8a`, 병합 시각 `2026-08-17T09:04:24Z`) 해당 SPEC은 `main`에서 `status: completed`다.** 따라서 이 문서가 `sync/SPEC-ENROLLMENT-001`을 기준점으로 표기한 관측(§1 표의 하단 3행, §3, §4, §5)은 **모두 현재 `main`에서 그대로 확인된다.** `plan.md` **§B.3**이 `DEP-3` 해소 사실과 그 게이트의 성격을 기록한다.

---

## §2 발견 1 — CORS (0.1.0 조사: 미설정 → **0.2.0 현재: 설정됨, 해소**)

### 2.0 현재 상태 (0.2.0 재확인 — 아래 2.1~2.4보다 이것이 우선한다)

**CORS는 설정되어 있다.** `main` 커밋 `29a1560` "fix(security): CORS 설정 추가"가 아래를 추가했다:

- `SecurityConfig.securityFilterChain`에 `.cors(cors -> cors.configurationSource(corsConfigurationSource))` 추가
- `CorsConfigurationSource` Bean 신설 — 허용 오리진은 `CorsProperties`(`app.cors.allowed-origins`)로 주입
- `application.properties`에 `app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:5173}` 추가
- `CorsProperties.java` 신설 — 그 javadoc이 **이 문서의 §2 DEP-1을 이름으로 인용**한다

즉 §2.2가 "이 SPEC에게 인계된 결정"이라고 판정한 그 결정이, 이 조사를 근거로 실제 수행되었다. **`DEP-1`은 해소되었다.**

고정된 값 (프론트엔드가 지켜야 할 제약 — `spec.md` §A.5가 규범화):

| 항목 | 값 |
|---|---|
| 허용 오리진 | `${CORS_ALLOWED_ORIGINS:http://localhost:5173}` — 기본값이 Vite 기본 포트와 일치 |
| 허용 헤더 | `Authorization`, `Content-Type` 2종만 |
| 허용 메서드 | `GET`, `POST`, `PATCH`, `DELETE`, `OPTIONS` |
| `allowCredentials` | `false` (고정) |

**§2.4가 검토한 Vite 프록시 우회안은 더 이상 필요하지 않다.** 개발 서버를 기본 포트 5173으로 띄우면 브라우저가 직접 호출해도 CORS가 통과한다. 프록시는 선택 사항으로 강등되었다(`design.md` §B.1).

### 2.1 관측 (0.1.0 조사 시점 — 이력)

`SecurityConfig.securityFilterChain`의 체인 구성은 다음 7개 호출이 전부다:

```
csrf(disable) → sessionManagement(STATELESS) → httpBasic(disable) → formLogin(disable)
→ authorizeHttpRequests(...) → exceptionHandling(...) → addFilterBefore(JwtAuthenticationFilter)
```

`.cors(...)` 호출이 없고, `CorsConfigurationSource` Bean도 프로젝트 전체에 존재하지 않는다. `WebMvcConfigurer`를 통한 `addCorsMappings` 구현체도 없으며, 어떤 컨트롤러에도 `@CrossOrigin`이 붙어 있지 않다.

### 2.2 이것이 우연이 아니라 의도된 유예라는 증거

두 개의 선행 SPEC이 동일한 문장으로 이를 명시적으로 유예했다:

- `SPEC-AUTH-001/plan.md:139` — "CORS 설정은 이 SPEC에 **포함하지 않는다**. 프론트엔드 착수 시점에 실제 오리진이 정해진 뒤 추가하는 것이 추측 설정보다 안전하다."
- `SPEC-ENROLLMENT-001/plan.md:169` — "CORS 설정은 프론트엔드 착수 시점에 실제 오리진이 정해진 뒤 추가한다."

즉 이것은 **누락이 아니라 이 SPEC에게 인계된 결정**이다. "실제 오리진이 정해지는 시점"이 바로 지금이다.

### 2.3 영향 범위 판정 (0.1.0 조사 시점 — §2.0에 의해 무효화됨)

브라우저는 다른 오리진(Vite 개발 서버 포트)에서 온 XHR/fetch 요청에 대해 서버가 `Access-Control-Allow-Origin`을 응답하지 않으면 응답을 자바스크립트에 전달하지 않는다. 또한 `Authorization` 헤더는 단순 요청(simple request) 조건을 위반하므로 **모든 인증 요청이 사전 요청(preflight `OPTIONS`)을 유발**한다. 현재 `SecurityConfig`는 `OPTIONS`에 대한 별도 허용이 없고 `anyRequest().authenticated()`가 적용되므로, 사전 요청 자체가 401로 거부된다.

**당시 판정: 당시 12개였던 엔드포인트 전부 차단.** 부분적 우회(공개 엔드포인트만 사용) 조차 불가능하다 — `GET /api/courses`도 CORS 헤더 없이는 브라우저가 응답을 넘겨주지 않는다.

> **이 판정은 현재 유효하지 않다** (§2.0). CORS가 설정되었고 엔드포인트도 14개로 늘었다. 위 문단은 백엔드 변경의 근거가 된 조사 기록으로만 남긴다.

### 2.4 검토한 우회안과 기각 사유 (0.1.0 조사 시점 — 해소로 인해 우회 자체가 불필요해짐)

| 우회안 | 평가 |
|---|---|
| **Vite 개발 서버 프록시** (`server.proxy`) | 개발 중에는 동작한다 — 브라우저가 보기에 동일 오리진이 되므로 CORS가 발생하지 않는다. **그러나 이는 개발 편의일 뿐 백엔드 요구를 소멸시키지 않는다.** 정적 빌드 산출물을 별도 오리진에서 서빙하는 순간 다시 차단된다. `plan.md`는 이를 **개발 단계 임시 수단**으로만 채택하고 `DEP-1`을 존치한다 |
| 백엔드에 `@CrossOrigin` 추가 | **범위 이탈.** 백엔드 소스 수정은 REQ-NFR-002가 금지한다 |
| 브라우저 보안 비활성화 실행 | 기각. 검증 환경이 실제 환경과 달라져 "실제 실행 확인"이라는 목표 자체를 훼손한다 |

---

## §3 발견 2 — 취소 대상 식별자 (0.1.0 조사: 미노출 → **0.2.0 현재: 노출됨, 해소**)

### 3.0 현재 상태 (0.2.0 재확인 — 아래 3.1~3.4보다 이것이 우선한다)

**취소 대상 식별자는 노출된다.** `SPEC-ENROLLMENT-001`이 v0.3.0 제자리 개정(M7, `amendment_of: SPEC-ENROLLMENT-001`)으로 조회 엔드포인트 2종을 신설했고, 그 개정의 계기가 **이 문서의 §3**이다. 확인 기준점은 `sync/SPEC-ENROLLMENT-001`(PR #1) HEAD이며, 컨트롤러 매핑 애너테이션과 응답 레코드를 직접 읽어 확인했다.

| 엔드포인트 | 컨트롤러 | 응답 레코드 (필드 순서 그대로) |
|---|---|---|
| `GET /api/enrollments/mine` | `EnrollmentController.listMine` | `EnrollmentListItemResponse(Long enrollmentId, Long courseId, String courseTitle, String status, LocalDateTime enrolledAt)` |
| `GET /api/waitlist-entries/mine` | `WaitlistController.listMine` | `WaitlistListItemResponse(Long waitlistEntryId, Long courseId, String courseTitle, Long position, String status)` |

서비스 구현에서 확인한 동작 계약:

- **정렬** — `EnrollmentListQueryService.listMine`은 `findByMemberIdAndStatusOrderByIdAsc`로 `enrollmentId` 오름차순, `WaitlistService.listMine`은 `findByMemberIdAndStatusOrderByPositionAsc`로 `position` 오름차순.
- **필터** — 확정 목록은 `status='ENROLLED'`, 대기 목록은 `status='WAITING'`인 활성 행만.
- **빈 집합** — 0건이면 404가 아니라 `200` + `[]`. 백엔드 `REQ-LST-001`·`REQ-LST-002`가 명시.
- **소유권** — 회원 식별자를 입력받는 파라미터가 아예 없다. 반환 범위는 `Authentication`에서만 유도(`REQ-LST-003`).
- **취소 API와의 동일성** — 백엔드 `REQ-LST-006`이 "목록이 반환한 식별자 = 취소 API가 받는 식별자"를 개정의 종결 조건으로 규정.

**0.1.0의 §3.4 제안과 실제 구현이 다르다 (이것이 1회차 감사의 critical 지적 사항이다)**:

| 항목 | 0.1.0 제안 (§3.4) | 실제 구현 |
|---|---|---|
| 경로 형태 | `me` 소유자 접두사를 앞에 두는 형태 | **리소스 경로 뒤 `mine` 접미사** |
| 대기 항목 식별자 필드명 | `entryId` | **`waitlistEntryId`** |
| 대기 항목 `status` | 없음 | **있음** |
| 확정 항목 `enrolledAt` | `...`로 생략 | **있음** |

**§3.4의 제안 경로는 존재하지 않으며 호출하면 404다.** `spec.md` §A.4가 실제 계약을 정본으로 기록하며, `plan.md`·`design.md`도 그것을 따른다.

### 3.1 관측 (0.1.0 조사 시점 — 이력)

전 파일 대상 매핑 애너테이션 grep 결과 **당시** 엔드포인트는 정확히 12개였다. 현재는 위 2개가 추가되어 **14개**다(`spec.md` §A.4). 취소 계열 2개는 경로 변수를 요구한다:

- `DELETE /api/enrollments/{enrollmentId}`
- `DELETE /api/waitlist-entries/{entryId}`

응답 DTO 3종을 전수 확인했다:

```
EnrollmentReceiptResponse(Long requestId)
EnrollmentStatusResponse(Long requestId, String status, Long waitlistPosition)
CourseResponse(id, title, description, capacity, enrolledCount, remainingCapacity, startsAt, endsAt, status)
```

`EnrollmentStatusQueryService.getStatus`의 반환 경로도 직접 확인했다 — `WaitlistEntry`를 조회하지만 **`position`만 꺼내 쓰고 `id`는 버린다**:

```java
waitlistPosition = waitlistEntryRepository
        .findByMemberIdAndCourseIdAndStatus(memberId, request.getCourseId(), WaitlistStatus.WAITING)
        .map(WaitlistEntry::getPosition)   // ← id가 아니라 position
        .orElse(null);
```

`Enrollment` 엔티티의 `id`는 어떤 응답 DTO에도 등장하지 않는다. "내 수강신청 목록" / "내 대기명단" 조회 엔드포인트는 존재하지 않는다.

### 3.2 판정 (0.1.0 조사 시점 — §3.0에 의해 무효화됨)

**프론트엔드가 취소 대상을 지목할 경로가 원리적으로 없다.** 이는 클라이언트 설계로 해결 가능한 문제가 아니다. 사용자가 요구한 범위 (3) 수강신청 취소와 (4) 대기명단 취소는 백엔드 조회 API 추가 없이는 구현 불가다.

> **이 판정은 현재 유효하지 않다** (§3.0). 백엔드가 조회 엔드포인트 2종(`GET /api/enrollments/mine`·`GET /api/waitlist-entries/mine`)을 추가하여 `DEP-2`가 해소되었고, 취소 대상 식별자는 응답으로 노출된다. 위 문단은 백엔드 변경의 근거가 된 조사 기록으로만 남긴다.

### 3.3 검토한 우회안과 기각 사유

| 우회안 | 평가 |
|---|---|
| 사용자에게 식별자를 직접 입력받기 | 기각. 타인 식별자 시도를 화면이 유도하게 된다. 백엔드가 소유권을 2계층 검증하므로 실제 침해는 없으나, 그런 조작을 제공하는 화면 설계 자체가 잘못이다 (`spec.md` REQ-CNL-004) |
| 식별자를 순차 탐색(1, 2, 3…)하여 200을 찾기 | 기각. 열거 공격의 형태이며, 성공하더라도 타인 데이터에 대한 404/403 응답을 대량 유발한다 |
| 접수 시 `requestId`로부터 `enrollmentId` 유도 | 불가능. 두 값은 서로 다른 테이블의 독립 시퀀스이며 대응 관계가 응답으로 노출되지 않는다 |
| 프론트엔드가 로컬에 확정 이력을 축적 | 기각. 다른 기기·다른 탭·저장소 삭제 시 소실되며, 무엇보다 §3.1에서 확인했듯 **`enrollmentId`는 애초에 클라이언트에 한 번도 전달되지 않는다** — 축적할 값 자체가 없다 |

### 3.4 제안했던 백엔드 변경의 최소 형태 (0.1.0 — **폐기됨, §3.0이 실제 구현을 기록한다**)

0.1.0은 조회 엔드포인트 2개 추가를 제안하면서 소유자 접두사를 앞에 두는 경로 형태와 `entryId`라는 필드명을 함께 적었다. **백엔드는 조회 엔드포인트 방식이라는 큰 방향은 채택했으나 경로 형태와 필드명은 다르게 정했다.** 이 절의 제안 값을 그대로 구현에 쓰면 404가 난다 — 정본은 §3.0과 `spec.md` §A.4다.

또한 더 작은 대안으로 `EnrollmentStatusResponse`에 식별자를 추가하는 방안도 함께 검토했으나, 그 방식은 **요청 식별자를 보관하고 있는 경우에만** 취소가 가능해져 탭을 새로 연 사용자는 여전히 취소할 수 없다. 백엔드가 조회 엔드포인트 방식을 택한 것은 이 판단과 일치한다.

> **교훈**: 이 SPEC이 제안한 계약과 백엔드가 구현한 계약이 갈라질 수 있다. 제안을 그대로 구현 근거로 삼지 말고 **구현된 소스를 다시 읽을 것.** 1회차 감사가 이 SPEC을 FAIL시킨 두 critical 지적 중 하나가 정확히 이것이었다.

---

## §4 발견 3 — 오류 응답 형태가 3종으로 분기한다 (확인됨)

### 4.1 관측

`GlobalExceptionHandler`(sync 브랜치 기준)는 8개의 `@ExceptionHandler`를 가지며 모두 `ErrorResponse(code, message)`를 반환한다:

| 예외 | 상태 | code |
|---|---|---|
| `DuplicateEmailException` | 409 | `DUPLICATE_EMAIL` |
| `InvalidCredentialsException` | 401 | `INVALID_CREDENTIALS` |
| `CourseNotFoundException` | 404 | `COURSE_NOT_FOUND` |
| `CapacityBelowEnrollmentException` | 409 | `CAPACITY_BELOW_ENROLLMENT` |
| `EnrollmentRequestNotFoundException` | 404 | `ENROLLMENT_REQUEST_NOT_FOUND` |
| `EnrollmentNotFoundException` | 404 | `ENROLLMENT_NOT_FOUND` |
| `WaitlistEntryNotFoundException` | 404 | `WAITLIST_ENTRY_NOT_FOUND` |
| `InvalidCourseIdException` | 400 | `INVALID_COURSE_ID` |

그러나 두 개의 중요한 경로가 이 핸들러를 **거치지 않는다**:

1. **인증/인가 실패** — `SecurityConfig`가 `response.sendError(SC_UNAUTHORIZED)` / `sendError(SC_FORBIDDEN)`를 직접 호출한다. 이는 서블릿 컨테이너의 오류 처리로 넘어가 Spring Boot 기본 오류 바디(`timestamp`/`status`/`error`/`path`)를 낸다. **`code` 필드가 없다.**
2. **입력 검증 실패** — `GlobalExceptionHandler`의 클래스 주석이 이를 명시한다: *"입력 검증 실패(400)는 Spring의 기본 `@Valid` 처리에 위임한다."* 따라서 `SignupRequest`의 `@Email`·`@Size(min=8)` 위반, `CourseCreateRequest`의 `@Min(1)`·`@NotNull` 위반, `@ValidDateRange` 위반은 모두 기본 바디로 나온다. **`code` 필드가 없다.**

### 4.2 왜 이것이 위험한가

`code` 필드가 없는 두 경로가 하필 **사용자가 가장 자주 마주치는 오류**다:

- 401 만료 — 토큰 수명이 30분이므로 일상적으로 발생한다
- 400 검증 실패 — 회원가입 비밀번호 8자 미만, 강좌 정원 0 등 첫 시도에서 흔히 발생한다

`body.code`만 읽는 순진한 구현은 이 두 경우에 `undefined`를 받아 "알 수 없는 오류"를 표시한다. 즉 **가장 흔한 오류에서 가장 쓸모없는 메시지**를 내게 된다. `spec.md` §B.6이 이를 요구사항으로 승격한 이유다.

### 4.3 추정으로 표기하는 부분

Spring Boot 4.x의 `@Valid` 실패 기본 바디에 필드별 오류 배열이 포함되는지는 `server.error.include-*` 설정에 따라 달라진다. 현재 `application.properties`에는 관련 설정이 없으므로 **기본값이 적용된다**는 사실만 확인했고, 정확한 바디 형태는 구현 시점에 실제 응답을 관측하여 확정해야 한다. `plan.md` §F **M1**이 이를 확인 작업으로 배치한다(`acceptance.md` §F F4도 "M1 수행 중"으로 기록한다).

---

## §5 발견 4 — JWT 페이로드로 역할을 알 수 있다 (확인됨)

`JwtTokenProvider.generateToken`이 실제로 담는 클레임:

```java
.subject(member.getEmail())
.claim("role", member.getRole().name())   // "ADMIN" | "USER"
.issuedAt(...)
.expiration(...)
```

로그인 응답(`LoginResponse`)은 `accessToken` 하나뿐이므로 역할을 별도로 알 방법이 없어 보이지만, **토큰 페이로드에 `role`과 `exp`가 들어 있다.** 클라이언트는 JWT의 가운데 세그먼트를 base64url 디코드하여 이 값을 읽을 수 있다 — 서명 검증 없이.

**판정: 백엔드 변경 없이 관리자 메뉴 노출 판정과 만료 시각 파악이 가능하다.** 서명 검증을 하지 않으므로 이 값은 신뢰할 수 없으나, `spec.md` §A.7이 규정하듯 이 값의 용도는 **표시 한정**이며 실제 강제는 백엔드 403이 한다.

> 이 발견이 `DEP-2`와 다른 점: `DEP-2`는 데이터가 **어디에도 없어서** 우회 불가였지만, 역할 정보는 **이미 클라이언트 손에 있다**(토큰 안에). 따라서 백엔드 의존성이 아니다.

---

## §6 기술 선택 검토

사용자가 확정한 것은 **React + Vite + TypeScript** 세 가지뿐이며, 나머지는 이 SPEC이 결정한다. 결정의 기준은 프로젝트 헌장(`moai-constitution.md` § Agent Core Behaviors #4)의 **단순성 사다리**다 — 코드베이스 내 재사용 → 표준 라이브러리 → 플랫폼 기본 기능 → 이미 설치된 의존성 → 최소 코드. 새 의존성은 사다리의 아래 단계로 해결되지 않을 때만 추가한다.

### 6.1 라우팅 — React Router 채택

| 후보 | 평가 |
|---|---|
| **React Router (채택)** | 화면 8~10개의 평범한 다중 화면 SPA이며, 경로 가드(`spec.md` REQ-SES-009·REQ-ADM-002)를 중첩 라우트로 자연스럽게 표현할 수 있다. 생태계 표준이라 참고 자료가 풍부하다 |
| TanStack Router | 타입 안전 라우팅이 강점이나, 이 SPEC의 경로 수와 파라미터 복잡도가 그 이점을 정당화할 만큼 크지 않다. 학습 비용이 "빠르게 배선을 확인한다"는 목표와 상충 |
| 라우터 없이 조건부 렌더링 | 기각. 뒤로 가기·주소 공유·직접 진입(REQ-ADM-002의 검증 대상)이 성립하지 않는다 |

### 6.2 HTTP 클라이언트 — 표준 `fetch` 채택 (신규 의존성 없음)

단순성 사다리 3단계(플랫폼 기본 기능)에서 해결된다. 필요한 것은 세 가지뿐이다: 기준 주소 결합, `Authorization` 헤더 부착, 오류 바디 정규화. 이는 얇은 래퍼 모듈 하나로 충분하다.

`axios` 등을 도입하면 인터셉터를 얻지만, 위 세 가지를 위해 의존성을 추가하는 것은 사다리 4단계로의 불필요한 상승이다. 또한 `REQ-ERR-002`가 요구하는 "단일 지점 오류 해석"은 어차피 자체 래퍼가 필요하므로, 라이브러리를 넣어도 래퍼는 사라지지 않는다.

### 6.3 서버 상태·폴링 — TanStack Query 채택 (유일하게 추가하는 실질 의존성)

**이것이 이 SPEC에서 사다리 4단계(새 의존성)를 정당화하는 유일한 항목이다.**

폴링 요구사항(`spec.md` REQ-ENR-003~007)은 겉보기보다 까다롭다:

- 종단 상태 도달 시 즉시 중단 (INV-FE-002)
- 경과에 따른 간격 증가
- 상한 도달 시 자동 중단 + 수동 재확인 수단 유지
- 화면 이탈(언마운트) 시 타이머 정리
- 네트워크 실패 시 재시도, 단 401은 예외 처리

이를 `useEffect` + `setTimeout`으로 직접 구현하면 정리 누락·중복 타이머·언마운트 후 상태 갱신 같은 결함이 발생하기 쉽다. 그런데 **이 SPEC의 목적이 바로 "시스템이 실제로 동작함을 확인"하는 것**이므로, 확인 도구 자체에 결함이 있으면 목적이 훼손된다. 여기서 검증된 라이브러리를 쓰는 것은 사다리의 "안전을 깎지 말 것" 단서에 부합한다.

TanStack Query의 `refetchInterval`은 함수 형태를 받아 **직전 데이터를 보고 다음 간격을 정하거나 `false`를 반환해 중단**할 수 있으므로, 위 요구를 선언적으로 표현할 수 있다.

| 기각한 대안 | 사유 |
|---|---|
| 직접 만든 `usePolling` 훅 | 위에 열거한 결함 표면이 그대로 남는다. 이 SPEC의 목적과 상충 |
| SWR | 폴링 자체는 가능하나, 데이터 기반 동적 간격/중단 표현이 `refetchInterval` 함수 형태만큼 직접적이지 않다 |
| WebSocket / SSE로 전환 | **백엔드가 제공하지 않는다.** `product.md`가 "v1에서는 이메일/비동기 알림 채널 없음 — 폴링으로 대체"를 명시했다. 프론트엔드가 단독으로 채택할 수 없다 |

### 6.4 클라이언트 상태 — React Context + `useReducer` (신규 의존성 없음)

전역으로 공유해야 하는 클라이언트 상태는 **세션 하나**뿐이다: `{ accessToken, email, role, exp }`. 서버에서 온 데이터는 전부 §6.3이 관리한다.

Redux·Zustand·Jotai는 이 규모에서 순비용이다. 사다리 3단계(React 기본 기능)에서 해결된다.

### 6.5 폼 — 제어 컴포넌트 + 소형 검증 헬퍼 (신규 의존성 없음)

폼은 총 4개(회원가입·로그인·강좌 생성·강좌 수정)이며 각각 필드 5개 이하다. 더 중요한 것은 **검증의 진실 원천이 백엔드라는 점**이다 — `@Valid`가 400을 반환하므로 클라이언트 검증은 왕복을 줄이는 편의일 뿐 권위가 없다.

`react-hook-form` 등의 이점(비제어 성능 최적화, 복잡한 스키마)이 이 규모에서 발현되지 않는다.

### 6.6 스타일링 — CSS Modules (Vite 기본 지원, 신규 의존성 없음)

Vite는 `*.module.css`를 별도 설정 없이 처리한다. 사다리 4단계(플랫폼/도구 기본 기능)에서 해결된다.

Tailwind는 빌드 설정 표면과 클래스 관습을 추가하는데, 이 SPEC의 성공 기준(§E)에는 시각적 완성도 항목이 하나도 없다. 스타일링 선택은 **가장 되돌리기 쉬운 결정**이므로, 후속 SPEC이 시각적 완성도를 다룰 때 재검토하도록 남긴다.

### 6.7 토큰 저장 위치 — `sessionStorage` 채택

이 결정은 백엔드의 **실제 능력**에 정합해야 한다. 확인된 백엔드 조건:

- 토큰 폐기 목록(denylist) 없음 → 서버는 발급된 토큰을 무효화할 수단이 없다
- 리프레시 토큰 회전 없음 → 단일 액세스 토큰, TTL 30분
- 세션 STATELESS, CSRF 비활성 — **그리고 그 비활성의 근거가 "쿠키 기반 자격 증명을 사용하지 않는다"** (`SecurityConfig` 주석)

| 후보 | 평가 |
|---|---|
| `localStorage` | 탭·브라우저를 닫아도 디스크에 남는다. 서버가 무효화할 수 없는 자격 증명이 무기한 잔류한다 — 백엔드 조건과 가장 나쁜 조합 |
| 메모리 전용 | XSS 지속성 측면에서 가장 안전하지만, **새로고침마다 세션이 소실**된다. 사람이 클릭하며 확인하는 것이 목적인 SPEC에서 이는 심각한 마찰이다. 또한 XSS는 메모리의 토큰도 읽을 수 있으므로 방어 효과가 통념만큼 크지 않다 |
| **`sessionStorage` (채택)** | 탭 단위로 격리되고 탭을 닫으면 소멸한다. 노출 창이 탭 수명으로 제한되어 30분 TTL과 정합하며, README가 문서화한 "로그아웃 = 클라이언트가 토큰을 버림" 모델과도 일치한다. 새로고침은 견딘다 |
| `httpOnly` 쿠키 | 보안상 가장 강하나 **백엔드 변경이 필수**이고(Set-Cookie), 쿠키 자격 증명이 생기면 `SecurityConfig`가 CSRF를 비활성화한 근거가 무너져 CSRF 방어를 다시 도입해야 한다. 범위 이탈이며 부작용이 크다 |

**정직한 한계 표기**: `sessionStorage`는 XSS에 대한 방어가 아니다. 탭이 열려 있는 동안의 XSS 노출은 `localStorage`와 동일하다. 얻는 것은 **지속 기간의 축소**뿐이며, 실질적 완화는 백엔드의 30분 TTL이다. 이 한계는 `spec.md` REQ-SES-004의 근거 주석과 `plan.md` §C.4에 기록한다.

---

## §7 조사에서 확인하지 못한 것 (Gap)

정직성을 위해 **확인하지 못한 항목**을 명시한다. 아래는 구현 착수 시 실측으로 확정해야 한다.

1. **`@Valid` 실패 응답의 정확한 바디 형태** — §4.3. Spring Boot 4.x 기본값 + 현 설정 조합의 실제 출력을 관측하지 않았다.
2. **백엔드 실행 포트** — `application.properties`에 `server.port` 설정이 없으므로 기본값이 적용된다고 추정되나, 실행하여 확인하지 않았다.
3. ~~**`sync/SPEC-ENROLLMENT-001`의 `main` 병합 시점**~~ — **해소됨 (0.2.2에서 닫음).** 0.2.0 시점에는 이것이 유일하게 남은 차단 요소(`DEP-3`)였으나, PR #1이 병합되어(병합 커밋 `21eab8a`, `2026-08-17T09:04:24Z`) 코드 가용성과 `depends_on` 사전 점검 게이트가 모두 해결되었다. 이 항목은 더 이상 미확인 사항이 아니다 — `plan.md` §B.3.
4. **동시 부하 상황에서의 실제 폴링 지연 분포** — 백엔드가 "500건 이하에서 5초 이내"를 보장하나, 로컬 개발 환경에서의 실측치는 다를 수 있다. 폴링 상한값은 이 보장을 근거로 정하되 실측 후 조정 대상이다.
5. **선택한 라이브러리들의 정확한 버전 조합** — 버전 고정은 구현 시점의 실제 설치 결과로 확정한다. 이 문서는 라이브러리 선택의 **근거**를 기록할 뿐 버전을 단정하지 않는다.

---

## §8 참조

- 백엔드 SPEC: `.moai/specs/SPEC-AUTH-001/`, `.moai/specs/SPEC-COURSE-001/`, `.moai/specs/SPEC-ENROLLMENT-001/`
- 단순성 사다리: `.claude/rules/moai/core/moai-constitution.md` § Agent Core Behaviors #4
- 이 SPEC의 요구사항: `spec.md`
- 이 SPEC의 설계: `design.md`
- 이 SPEC의 계획: `plan.md`
