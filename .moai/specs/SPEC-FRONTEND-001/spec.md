---
id: SPEC-FRONTEND-001
title: "React 클라이언트 — 회원·강좌·수강신청 전 화면 및 관리자 콘솔"
version: "0.2.1"
status: draft
created: 2026-08-17
updated: 2026-08-17
author: manager-spec
priority: P1
phase: "v1.0.0"
module: "frontend"
lifecycle: spec-anchored
tags: "frontend, react, vite, typescript, spa, polling, jwt"
tier: L
depends_on: [SPEC-AUTH-001, SPEC-COURSE-001, SPEC-ENROLLMENT-001]
---

# SPEC-FRONTEND-001 — React 클라이언트 (회원·강좌·수강신청·관리자)

## HISTORY

| 버전 | 날짜 | 작성자 | 변경 내용 |
|---|---|---|---|
| 0.1.0 | 2026-08-17 | manager-spec | 최초 작성 (draft). 백엔드 3개 SPEC(AUTH/COURSE/ENROLLMENT)이 노출한 12개 엔드포인트를 소비하는 React 클라이언트 전 범위. 프론트엔드 워크스페이스 위치를 **저장소 내 `frontend/`** 로 확정(`tech.md`·`structure.md`의 "미결정" 해소). 백엔드 선행 의존성 2건(CORS 미설정, 취소 대상 식별자 미노출)을 차단 요소로 식별 |
| 0.2.0 | 2026-08-17 | manager-spec | **plan-audit 1회차(FAIL 0.75) 지적 사항 반영 — 사실 재기준화(re-baseline).** 0.1.0 작성 이후 백엔드가 두 차단 의존성을 모두 닫았고, 그 결과 0.1.0의 사실 진술 다수가 낡았다. (1) **`DEP-1` 해소** — `main` 커밋 `29a1560`이 `SecurityConfig`에 CORS를 설정했다. (2) **`DEP-2` 해소** — `SPEC-ENROLLMENT-001` v0.3.0 제자리 개정(M7)이 조회 엔드포인트 2종을 추가했다. 경로·필드명이 0.1.0의 **제안과 다르므로**(`/api/enrollments/mine`·`/api/waitlist-entries/mine`, `waitlistEntryId`) §A.4·§B.4를 실제 계약에 맞춰 정정. (3) 엔드포인트 전수 **12개 → 14개**. (4) `REQ-ENR-004` 종단 판정을 화이트리스트 → **`PENDING`의 여집합**으로 정정 (`plan.md` AP-3·`design.md` §A.2와의 자기모순 해소). (5) `REQ-CNL-006`~`009` 신설(목록 조회·빈 목록·정렬 계약), `REQ-ENR-007`·`010` 문구 정정 |
| 0.2.1 | 2026-08-17 | manager-spec | plan-audit 2회차(PASS 0.89) 지적 사항 N1~N8 반영 — N1(대기명단 순서 오기술 정정)·N2(main 검증 범위 오기술 정정)이 major, 나머지 6건은 minor(상호참조·표기 정정). AC 총계 84→85(N5, AC-FE-073 분할) |

---

## §A 개요

### A.1 배경 — 이 SPEC이 존재하는 이유

`SPEC-AUTH-001`(인증)·`SPEC-COURSE-001`(강좌)·`SPEC-ENROLLMENT-001`(선착순 큐·대기명단)은 **모두 백엔드 API 계약까지만을 범위로** 삼았고, 각 SPEC이 자신의 인수 기준을 "React 없이 100% 검증된다"고 명시했다. 그 결과 현재 이 시스템은 **통합 테스트로는 전부 검증되었으나 사람이 한 번도 클릭해 본 적이 없다.**

이 SPEC의 목적은 그 간극을 메우는 것이다. 사용자가 명시한 목표는 **"실제 실행을 확인"** — 즉 회원가입부터 관리자 정원 증설까지 전 시나리오를 브라우저에서 직접 클릭해 시스템이 실제로 동작함을 눈으로 확인하는 것이다.

이 목표는 이 SPEC의 우선순위를 규정한다:

> **올바르게 배선된 동작하는 클라이언트 > 시각적 완성도.**

디자인 시스템·애니메이션·반응형 정교화·접근성 심화는 이 SPEC의 성공 기준이 아니다. **14개 엔드포인트가 화면에서 실제로 호출되고, 응답이 정확히 해석되며, 오류가 사용자에게 정직하게 표시되는 것**이 성공 기준이다.

### A.2 워크스페이스 위치 확정 — 열린 결정의 해소

`.moai/project/tech.md` § 프론트엔드와 `.moai/project/structure.md` § 프론트엔드는 React 워크스페이스의 위치를 **"미결정 — SPEC plan 단계에서 확정할 열린 결정 사항"** 으로 남겨두었다.

**이 SPEC이 그 결정을 확정한다: 별도 sibling 저장소가 아니라, 현 저장소 내부의 `frontend/` 디렉터리다.**

채택 근거:

1. **단일 저장소 = 단일 진실.** 백엔드 API 계약이 바뀌면 프론트엔드가 같은 커밋 범위에서 함께 바뀐다. 별도 저장소는 계약 변경 시 두 저장소의 버전 정합을 사람이 추적해야 한다.
2. **이 SPEC의 목표가 "전체 시스템이 실제로 동작함을 확인"이다.** 확인 대상이 두 저장소에 흩어져 있으면 확인 절차 자체가 두 배로 복잡해진다.
3. **1인 개발 규모.** 별도 저장소의 이점(독립 배포 주기, 팀 경계 분리)은 현재 규모에서 이득이 없고 비용만 발생한다.

> 이 결정은 **`.moai/project/tech.md`·`structure.md`의 "미결정" 표기를 갱신해야 하는 후속 작업을 발생시킨다.** 그 갱신은 sync 단계(manager-docs)의 소유이며, 이 SPEC은 갱신에 필요한 결정 내용만 여기에 규범적으로 기록한다. 이 SPEC 자체는 `.moai/project/*.md`를 수정하지 않는다.

### A.3 용어 정의

| 용어 | 의미 |
|---|---|
| 세션 | 로그인 성공으로 획득한 액세스 토큰과 그로부터 파생된 회원 식별 정보(이메일·역할·만료 시각)의 클라이언트 측 보유 상태 |
| 종단 상태 | `SPEC-ENROLLMENT-001` §A.4가 정의한 큐 요청의 최종 결과값. 이 값에 도달하면 폴링을 중단한다 |
| 폴링 | 접수 후 종단 상태에 도달할 때까지 상태 조회 API를 주기적으로 재호출하는 동작 |
| 관리자 화면 | `ADMIN` 역할 회원만 사용하는 강좌 생성·수정·마감·정원 증설 화면 |
| 경로 가드 | 특정 화면 진입을 세션 상태·역할에 따라 허용/차단하는 클라이언트 측 라우팅 장치 |
| 오류 응답 정규화 | 백엔드가 반환하는 **서로 다른 3종의 오류 바디 형태**를 화면이 소비할 단일 형태로 변환하는 동작 (§A.6) |

### A.4 소비 대상 API 계약 — 전수 목록 (읽기 전용 인용)

이 SPEC이 소비하는 엔드포인트는 아래 14개가 **전부**이며, 이 목록에 없는 엔드포인트는 존재하지 않는다. 이 표는 선행 3개 SPEC의 구현 산출물(소스 파일)에서 직접 확인한 것이다.

**확인 기준점(staleness 탐지용)**: 두 기준점으로 나뉜다 — **1~4·9~12번은 `main` HEAD(커밋 `29a1560`)**, **5~8·13~14번은 `sync/SPEC-ENROLLMENT-001` HEAD(PR #1, 커밋 `871d247`)**에서 컨트롤러 매핑 애너테이션과 응답 레코드를 읽어 확인했다.

두 기준점으로 나뉘는 이유: `SPEC-ENROLLMENT-001`의 구현(`enrollment`·`waitlist` 패키지)이 아직 `main`에 병합되지 않았다(`DEP-3`, §A.5). `main` HEAD에 존재하는 컨트롤러는 `CourseController`·`CourseAdminController`·`AuthController` **3개뿐**이므로, 5~8·13·14번(수강신청 접수·상태 조회·취소 2종·목록 2종)은 `main`에서 확인할 수 없다. `research.md` §1이 같은 사실을 기록한다.

0.1.0이 "12개가 전부"라고 단정한 뒤 백엔드가 2개를 추가하여 그 단정이 무너졌으므로, 이후 개정도 **prose가 아니라 소스를 다시 읽어** 이 표를 갱신한다. 위 두 SHA가 그 재확인의 출발점이다.

| # | 메서드 · 경로 | 인가 | 성공 응답 | 응답 바디 |
|---|---|---|---|---|
| 1 | `POST /api/auth/signup` | 공개 | 201 | `{id, email, role}` |
| 2 | `POST /api/auth/login` | 공개 | 200 | `{accessToken}` |
| 3 | `GET /api/courses?page&size` | 공개 | 200 | `{items[], totalElements, totalPages, currentPage}` |
| 4 | `GET /api/courses/{id}` | 공개 | 200 | `{id, title, description, capacity, enrolledCount, remainingCapacity, startsAt, endsAt, status}` |
| 5 | `POST /api/courses/{courseId}/enrollments` | 인증 | **202** | `{requestId}` |
| 6 | `GET /api/enrollment-requests/{requestId}` | 인증(본인) | 200 | `{requestId, status, waitlistPosition}` |
| 7 | `DELETE /api/enrollments/{enrollmentId}` | 인증(본인) | **202** | `{requestId}` |
| 8 | `DELETE /api/waitlist-entries/{entryId}` | 인증(본인) | 200 | (본문 없음) |
| 9 | `POST /api/admin/courses` | `ADMIN` | 201 | `CourseResponse` |
| 10 | `PATCH /api/admin/courses/{id}` | `ADMIN` | 200 | `CourseResponse` |
| 11 | `POST /api/admin/courses/{id}/close` | `ADMIN` | 200 | `CourseResponse` |
| 12 | `DELETE /api/admin/courses/{id}` | `ADMIN` | 200 | `CourseResponse` (내부적으로 11번과 동일한 마감 처리) |
| 13 | `GET /api/enrollments/mine` | 인증(본인) | 200 | `[{enrollmentId, courseId, courseTitle, status, enrolledAt}]` |
| 14 | `GET /api/waitlist-entries/mine` | 인증(본인) | 200 | `[{waitlistEntryId, courseId, courseTitle, position, status}]` |

**계약상 주의 사항 (화면 설계에 직접 영향):**

- **5·7번은 202(Accepted)이며 200이 아니다.** 접수는 즉시 확정이 아니라 큐 적재이므로, 화면은 5·7번의 성공을 "신청 완료/취소 완료"로 표시해서는 **안 된다**. 정확한 표시는 "접수됨 — 처리 중"이며, 확정 여부는 6번 폴링으로만 확정된다.
- **12번(`DELETE /api/admin/courses/{id}`)은 물리 삭제가 아니라 마감이다.** `SPEC-COURSE-001` REQ-ADM-008이 삭제를 `CLOSED` 전이로 정의했다. 화면은 이를 "삭제"라고 표기해서는 안 되며 "마감"으로 표기한다.
- **정원 증설은 별도 엔드포인트가 없다.** 10번(`PATCH /api/admin/courses/{id}`)에 증가된 `capacity`를 실어 보내면 `SPEC-ENROLLMENT-001` REQ-ADX-001에 따라 백엔드가 `CAPACITY_INCREASE` 큐 요청을 적재하고 대기자를 승격한다. 관리자 화면은 이를 별도 조작으로 보여줄 수 있으나, 호출하는 엔드포인트는 10번 하나다.
- **10번은 부분 갱신이 아니다.** 메서드는 `PATCH`이지만 `CourseUpdateRequest`의 `title`·`capacity`·`startsAt`·`endsAt`가 모두 필수(`@NotNull`/`@NotBlank`)이므로, 화면은 **현재 값 전체를 실어 보내야** 한다. 변경 필드만 보내면 400이 반환된다.
- **13·14번은 보유 내역이 0건일 때 404가 아니라 `200` + 빈 배열 `[]`을 반환한다.** 백엔드 `REQ-LST-001`·`REQ-LST-002`가 이를 명시적으로 규정한다. 화면이 빈 배열을 오류로 해석하면, 아직 아무것도 신청하지 않은 **모든 신규 회원에게 오류 화면이 표시된다.**
- **13·14번의 정렬은 백엔드가 정한다.** 13번은 `enrollmentId` 오름차순, 14번은 `position` 오름차순이다(`EnrollmentListQueryService.listMine`·`WaitlistService.listMine`의 리포지토리 메서드로 확인). 화면은 이 순서를 그대로 표시하며 자체 재정렬하지 않는다. 다만 **14번의 `position`은 강좌 단위 순번**이므로(아래 항목), 이 정렬은 결정적 표시 순서일 뿐 강좌를 가로지르는 어떤 의미도 갖지 않는다.
- **14번의 `position`은 해당 항목이 속한 강좌 안에서의 순번이며, 회원의 전체 대기 현황을 가로지르는 순번이 아니다.** 백엔드 `WaitlistEntryRepository.nextPosition`이 `WHERE w.courseId = :courseId`로 강좌별 최댓값 + 1을 계산하고, 부분 유니크 인덱스도 `(course_id, position) WHERE status='WAITING'`으로 강좌 단위다. 즉 한 회원이 강좌 A에서 1번, 강좌 B에서 2번을 갖는 것은 정상이며, 이는 **A가 B보다 먼저 승격된다는 뜻이 아니다** — 두 대기열은 각 강좌의 정원 변동에 따라 독립적으로 진행한다. 따라서 화면은 `position`을 반드시 `courseTitle`과 나란히 표시하고, 목록 전역의 순위처럼 보이게 표시해서는 **안 된다**.
- **14번의 취소 식별자는 `waitlistEntryId`이며 `position`이 아니다.** 두 값 모두 `Long`이므로 타입 검사가 혼동을 잡아 주지 못한다. `position`을 8번(`DELETE /api/waitlist-entries/{entryId}`)에 넣으면 **엉뚱한 행을 지목**하며, 그 행이 타인 소유이면 403/404가, 본인 소유이면 **의도하지 않은 항목이 취소**된다. 백엔드 `REQ-LST-006`이 "목록이 반환한 식별자 = 취소 API가 받는 식별자"를 종결 조건으로 못박은 이유다.
- **13·14번은 회원 식별자를 입력받지 않는다.** 반환 범위는 오직 인증 주체에서 유도되므로(`REQ-LST-003`), 화면이 회원 식별자를 질의 파라미터로 붙일 여지가 없다.

### A.5 백엔드 선행 의존성 — 상태 (0.2.0에서 재기준화)

이 SPEC은 백엔드 소스를 수정하지 않는다. 0.1.0은 백엔드 변경 없이는 원리적으로 구현 불가한 2건(`DEP-1`·`DEP-2`)을 차단 의존성으로 기록했다. **두 건 모두 0.1.0 작성 이후 백엔드가 닫았다.** 아래는 그 해소 사실과, 해소로 인해 이 SPEC이 새로 짊어지는 제약을 기록한다.

| ID | 0.1.0 시점 | 현재 | 해소 근거 |
|---|---|---|---|
| `DEP-1` (CORS 미설정) | 미해소 — 전 범위 차단 | **해소됨** | `main` 커밋 `29a1560` "fix(security): CORS 설정 추가" |
| `DEP-2` (취소 대상 식별자 미노출) | 미해소 — 취소 2개 화면 차단 | **해소됨** | `SPEC-ENROLLMENT-001` v0.3.0 제자리 개정 M7 (`sync/SPEC-ENROLLMENT-001`, PR #1) |
| `DEP-3` (선행 PR 미병합) | 시간이 해소 | **미해소 — 유일하게 남은 차단 요소** | `plan.md` §B.3 |

#### DEP-1 — CORS (해소됨)

`common/config/SecurityConfig.java`가 `.cors(cors -> cors.configurationSource(corsConfigurationSource))`를 필터 체인에 포함하고, `CorsConfigurationSource` Bean을 선언한다. 허용 오리진은 `CorsProperties`(`app.cors.allowed-origins`)로 주입되며 소스에 하드코딩되지 않는다.

`SPEC-AUTH-001`·`SPEC-ENROLLMENT-001`이 "프론트엔드 착수 시점에 실제 오리진이 정해진 뒤 추가한다"고 유예했던 결정이, 이 SPEC의 `research.md` §2를 근거로 실제 수행되었다(`CorsProperties.java`의 javadoc이 `SPEC-FRONTEND-001 research.md §2 DEP-1`을 인용한다).

**해소가 만든 새 제약** — 이것이 `DEP-1`을 대체하여 이 SPEC이 지켜야 할 사항이다:

| 항목 | 백엔드가 고정한 값 | 프론트엔드에 대한 함의 |
|---|---|---|
| 허용 오리진 | `${CORS_ALLOWED_ORIGINS:http://localhost:5173}` | Vite 개발 서버를 **5173이 아닌 포트로 띄우면** `CORS_ALLOWED_ORIGINS`를 함께 설정하지 않는 한 전 호출이 차단된다 |
| 허용 헤더 | `Authorization`, `Content-Type` **2종만** | 임의의 커스텀 요청 헤더를 추가하면 preflight에서 조용히 실패한다 |
| 허용 메서드 | `GET`·`POST`·`PATCH`·`DELETE`·`OPTIONS` | 이 SPEC이 쓰는 메서드는 전부 포함된다 |
| `allowCredentials` | `false` | 쿠키·자격 증명 기반 요청은 불가. §C.4가 `httpOnly` 쿠키를 기각한 근거를 백엔드가 독립적으로 확증한다 |

이 제약은 `design.md` §B.1이 개발 환경 구성으로 전개하고, `REQ-NFR-003`이 기준 주소 주입 방식으로 다룬다.

#### DEP-2 — 취소 대상 식별자 (해소됨)

`SPEC-ENROLLMENT-001` v0.3.0이 조회 엔드포인트 2종(§A.4의 13·14번)을 추가하여, 취소 엔드포인트 2종(7·8번)이 요구하는 `enrollmentId`·`waitlistEntryId`를 응답으로 노출한다. 백엔드 `REQ-LST-006`이 **"목록 조회가 반환한 식별자 = 취소 API가 받는 식별자"** 를 개정의 종결 조건으로 명시했다.

**0.1.0의 제안과 실제 구현의 차이 (이 SPEC이 정정해야 했던 지점)**:

| 항목 | 0.1.0 제안 | 실제 구현 (이 SPEC이 따르는 값) |
|---|---|---|
| 경로 형태 | `me` 소유자 접두사를 앞에 두는 형태 | **리소스 경로 뒤에 `mine` 접미사** — `GET /api/enrollments/mine`, `GET /api/waitlist-entries/mine` |
| 대기 항목 식별자 필드명 | `entryId` | **`waitlistEntryId`** |
| 대기 항목 `status` 필드 | 없음 | **있음** |
| 빈 목록·정렬 계약 | 명시하지 않음 | **200 + `[]`, `enrollmentId`/`position` 오름차순** |

접미사 형태를 택한 것은 백엔드의 의도된 결정이다 — 취소 경로(`/api/enrollments/{id}`·`/api/waitlist-entries/{id}`)와 접두사를 일치시켜 같은 리소스 계열임을 드러낸다. **이 SPEC은 실제 구현을 따르며, 0.1.0의 제안 경로는 폐기한다 — 그 경로는 존재하지 않으며 호출하면 404다.**

§B.4가 이 계약을 요구사항으로 고정한다.

### A.6 백엔드 오류 응답 형태 — 3종이며 단일하지 않다

프론트엔드가 오류를 일관되게 표시하려면 백엔드가 실제로 반환하는 형태를 정확히 알아야 한다. 코드를 직접 확인한 결과 **형태는 3종이다.**

| 형태 | 발생 경로 | 바디 | 해당 상태 코드 |
|---|---|---|---|
| **F1 — 도메인 오류** | `GlobalExceptionHandler`의 8개 `@ExceptionHandler` | `{code, message}` | 400(`INVALID_COURSE_ID`), 401(`INVALID_CREDENTIALS`), 404(`COURSE_NOT_FOUND`·`ENROLLMENT_NOT_FOUND`·`ENROLLMENT_REQUEST_NOT_FOUND`·`WAITLIST_ENTRY_NOT_FOUND`), 409(`DUPLICATE_EMAIL`·`CAPACITY_BELOW_ENROLLMENT`) |
| **F2 — 인증/인가 실패** | `SecurityConfig`의 `authenticationEntryPoint`·`accessDeniedHandler`가 호출하는 `response.sendError(...)` | Spring 기본 오류 바디 (`{timestamp, status, error, path}`) — **`code` 필드가 없다** | 401(무토큰·만료), 403(역할 부족) |
| **F3 — 입력 검증 실패** | `@Valid` 위반에 대한 Spring 기본 처리 (`GlobalExceptionHandler`가 의도적으로 위임) | Spring 기본 검증 오류 바디 — **`code` 필드가 없다** | 400 |

**이것이 요구사항으로 승격되는 이유**: 화면이 `body.code`만 읽도록 구현하면 F2·F3에서 `undefined`가 되어 **"알 수 없는 오류"가 표시된다.** 그런데 F2(401 만료)와 F3(400 검증 실패)는 사용자가 **가장 자주 마주치는** 두 오류다. 즉 순진한 구현은 가장 흔한 오류에서 가장 무의미한 메시지를 낸다. §B.6이 이를 요구사항으로 고정한다.

### A.7 보안 경계에 관한 규범적 선언 — 경로 가드는 보안 장치가 아니다

`SecurityConfig`는 `/api/admin/**`에 `hasRole("ADMIN")`을 적용하여 **API 계층에서** 권한을 강제한다. 위반 시 403이 반환된다.

이 SPEC이 정의하는 클라이언트 측 경로 가드(§B.5)는 **오로지 UX 편의 장치**다:

- 일반 회원에게 사용할 수 없는 관리자 메뉴를 노출하여 403을 유발하는 것을 막는다
- 브라우저 주소창으로 관리자 경로에 직접 진입했을 때 즉시 안내한다

**이 가드는 보안 통제가 아니다.** 클라이언트 코드는 사용자가 변경할 수 있으므로, 가드를 우회한 요청은 여전히 발생할 수 있고 그때 실제로 막는 것은 **백엔드의 403**이다. 이 SPEC은 이 구분을 명시적으로 기록하여, 후속 작업자가 클라이언트 가드를 보안 통제로 오인하는 것을 방지한다.

동일한 논리가 **역할 판별 수단**에도 적용된다. 로그인 응답은 `{accessToken}`만 반환하며 역할을 별도로 주지 않는다. 클라이언트는 JWT 페이로드의 `role` 클레임을 **서명 검증 없이** 읽어 화면 표시에 사용한다. 이는 **표시 목적 한정**이며, 위조된 `role` 클레임으로 관리자 화면을 열더라도 API 호출은 서명 검증을 통과하지 못해 401/403으로 차단된다.

---

## §B 요구사항 (GEARS)

### B.1 세션 및 인증 (SES)

- **REQ-SES-001** (Event-driven) — **When** 방문자가 회원가입 양식을 제출하면, 클라이언트는 `POST /api/auth/signup`을 호출하고 성공 시 로그인 화면으로 유도 **shall**한다.
- **REQ-SES-002** (Event-driven) — **When** 방문자가 로그인 양식을 제출하면, 클라이언트는 `POST /api/auth/login`을 호출하고 성공 시 반환된 액세스 토큰으로 세션을 수립 **shall**한다.
- **REQ-SES-003** (Ubiquitous) — 클라이언트는 인증이 필요한 모든 요청에 `Authorization: Bearer <token>` 헤더를 부착 **shall**하며, 공개 엔드포인트(`/api/auth/**`, `GET /api/courses`, `GET /api/courses/*`)에는 세션이 없어도 요청을 보낼 수 있어야 **shall**한다.
- **REQ-SES-004** (Ubiquitous) — 세션 토큰은 **브라우저 탭의 수명을 넘어 영속 shall not**한다. 탭을 닫으면 저장된 토큰이 소멸 **shall**한다.

  > 근거: 백엔드는 토큰 폐기 목록(denylist)과 리프레시 토큰 회전을 **v1 범위에서 제외**했다(`SPEC-AUTH-001`). 즉 발급된 토큰은 만료(30분) 전까지 서버가 무효화할 수단이 없다. 이 조건에서 토큰을 디스크에 영속시키면 서버가 통제할 수 없는 자격 증명이 사용자의 저장소에 남는다. 탭 수명으로 노출 창을 제한하는 것이 백엔드의 실제 능력과 정합한 선택이다. 저장 기술의 선택은 `plan.md` §C.4가 정한다.
- **REQ-SES-005** (Event-driven) — **When** 사용자가 로그아웃을 요청하면, 클라이언트는 보유한 토큰과 파생 세션 정보를 폐기 **shall**한다.
- **REQ-SES-006** (Ubiquitous) — 로그아웃 화면 문구는 서버 측 토큰 무효화가 일어났다고 표현 **shall not**하며, 토큰이 만료 시각까지 유효하게 남는다는 사실을 사용자가 확인할 수 있도록 **shall**한다.

  > 근거: `README.md`가 "로그아웃은 클라이언트가 저장된 토큰을 버리는 것으로만 이루어진다"고 문서화한 제약을 화면이 부정하면, 사용자는 존재하지 않는 보안 속성을 믿게 된다. 백엔드가 갖지 않은 능력을 화면이 암시하는 것을 금지한다.
- **REQ-SES-007** (Event-driven) — **When** 임의의 API 호출이 401을 반환한 것이 감지되면, 클라이언트는 보유 세션을 폐기하고 로그인 화면으로 유도 **shall**하며, 만료로 인해 재로그인이 필요함을 안내 **shall**한다.
- **REQ-SES-008** (Ubiquitous) — 클라이언트가 화면 표시 목적으로 사용하는 회원 역할은 토큰 페이로드에서 읽 **shall**되, 이 값을 보안 판정의 근거로 사용 **shall not**한다 (§A.7).
- **REQ-SES-009** (State-driven) — **While** 세션이 없는 상태이면, 인증이 필요한 화면(수강신청 접수·상태 조회·취소·관리자 화면)은 진입을 허용 **shall not**하고 로그인 화면으로 유도 **shall**한다.

### B.2 강좌 카탈로그 (CAT)

- **REQ-CAT-001** (Event-driven) — **When** 사용자가 강좌 목록 화면에 진입하면, 클라이언트는 `GET /api/courses`를 호출하여 강좌 목록을 표시 **shall**한다.
- **REQ-CAT-002** (Ubiquitous) — 강좌 목록 화면은 백엔드 응답의 페이지 메타데이터(`totalElements`·`totalPages`·`currentPage`)를 사용하여 페이지 이동 수단을 제공 **shall**하며, 클라이언트에서 전체 목록을 받아 자체 분할 **shall not**한다.
- **REQ-CAT-003** (Ubiquitous) — 강좌 항목은 정원(`capacity`)·확정 인원(`enrolledCount`)·잔여 정원(`remainingCapacity`)·모집 상태(`status`)를 사용자가 식별할 수 있게 표시 **shall**한다.
- **REQ-CAT-004** (Event-driven) — **When** 사용자가 강좌 항목을 선택하면, 클라이언트는 `GET /api/courses/{id}`를 호출하여 상세 정보를 표시 **shall**한다.
- **REQ-CAT-005** (State-driven) — **While** 강좌의 모집 상태가 `CLOSED`이면, 화면은 해당 강좌에 대한 수강신청 조작을 제공 **shall not**하고 마감 상태임을 표시 **shall**한다.
- **REQ-CAT-006** (Ubiquitous) — 강좌 목록·상세 화면은 세션이 없는 방문자에게도 열람 가능 **shall**하다 (백엔드가 `permitAll`로 노출한 범위와 일치).

### B.3 수강신청 접수 및 상태 폴링 (ENR)

- **REQ-ENR-001** (Event-driven) — **When** 인증된 회원이 강좌 상세 화면에서 수강신청을 실행하면, 클라이언트는 `POST /api/courses/{courseId}/enrollments`를 호출하고 응답의 요청 식별자를 보관 **shall**한다.
- **REQ-ENR-002** (Ubiquitous) — 접수 성공(202) 직후의 화면 표시는 **"접수됨 / 처리 중"** 계열의 표현 **shall**이며, 확정·성공을 뜻하는 표현을 사용 **shall not**한다.

  > 근거: 202는 큐 적재 완료이지 확정이 아니다(§A.4). 이 시점에 "신청 완료"를 표시하면, 이후 폴링 결과가 `WAITLISTED`나 `CLOSED`로 확정될 때 화면이 자기 자신을 부정하게 된다.
- **REQ-ENR-003** (Event-driven) — **When** 요청 식별자가 확보되면, 클라이언트는 `GET /api/enrollment-requests/{requestId}` 폴링을 개시 **shall**한다.
- **REQ-ENR-004** (Event-driven) — **When** 폴링 응답의 상태가 `PENDING`이 **아닌** 것이 감지되면, 클라이언트는 그 값을 종단으로 판정 **shall**하고 폴링을 즉시 중단 **shall**하며 해당 결과를 표시 **shall**한다. 종단 판정은 **`PENDING`의 여집합**으로 수행 **shall**되며, 알려진 종단 값의 목록과 대조하는 방식(화이트리스트)으로 수행 **shall not**한다.

  > 근거: 백엔드가 결과값을 추가하면 화이트리스트는 그 미지 값을 종단으로 인식하지 못해 **폴링이 영원히 멈추지 않는다**(`plan.md` AP-3, INV-FE-002 위반). 여집합 판정은 미지 값을 자동으로 종단 취급하므로 이 결함이 구조적으로 발생하지 않는다. 아래는 **현재 알려진** 종단 값 8종이며, 이 목록은 REQ-ENR-009의 문구 매핑을 위한 **참고 정보**일 뿐 판정 근거가 아니다 — `SUCCESS`·`WAITLISTED`·`CLOSED`·`REJECTED`·`FAILED`·`CANCELLED`·`PROMOTED`·`NOOP`.
- **REQ-ENR-005** (Ubiquitous) — 클라이언트는 종단 상태에 도달한 요청에 대해 상태 조회를 계속 호출 **shall not**한다.
- **REQ-ENR-006** (State-driven) — **While** 상태가 `PENDING`인 동안, 클라이언트는 요청 접수 시점으로부터 경과 시간이 커질수록 폴링 간격을 늘리 **shall**며, 고정 간격으로 무한히 반복 **shall not**한다.

  > 근거: 백엔드 REQ-STS-003은 동시 500건 이하 부하에서 5초 이내 종단 도달을 보장하고, **그 상한을 넘으면 지연이 큐 깊이에 비례해 증가**한다고 명시했다. 즉 지연이 길어지는 상황은 곧 큐가 혼잡한 상황이며, 그때 클라이언트가 고정 간격으로 계속 두드리는 것은 혼잡을 가중시킨다.
- **REQ-ENR-007** (Event-driven) — **When** 폴링이 상한 시간에 도달했는데도 상태가 `PENDING`인 것이 감지되면, 클라이언트는 자동 폴링을 중단하고 **사용자가 직접 재확인할 수 있는 수단을 제공** **shall**한다. 요청 식별자는 이때 유실 **shall not**된다.
- **REQ-ENR-008** (State-driven) — **While** 결과가 `WAITLISTED`이면, 화면은 응답의 대기 순번(`waitlistPosition`)을 함께 표시 **shall**한다.
- **REQ-ENR-009** (Ubiquitous) — 클라이언트는 REQ-ENR-004가 참고 정보로 열거한 **현재 알려진 종단 결과 8종** 각각에 대해 서로 구별되는 사용자 안내 문구를 제공 **shall**하며, 그 목록에 없는 미지의 상태값을 받았을 때 화면이 오류로 중단 **shall not**하고 일반 안내 문구로 대체 **shall**한다.
- **REQ-ENR-010** (Ubiquitous) — 상태 조회는 부작용이 없는 읽기 동작이므로, 클라이언트는 폴링 중 네트워크 오류가 발생하면 다음 주기에 재시도 **shall**한다. 단 401 응답에 대해서는 재시도 **shall not**하며 REQ-SES-007의 세션 폐기 흐름이 우선 **shall**한다.
- **REQ-ENR-011** (Ubiquitous) — 폴링 간격과 상한 계산의 기준이 되는 **접수 시각**은 요청 식별자와 함께 보존 **shall**되며, 화면 재진입·새로고침으로 초기화 **shall not**된다. 상한은 **최초 접수 시점**부터 측정 **shall**되며 화면 마운트 시점부터 측정 **shall not**한다.

  > 근거: `design.md` §A.6이 요청 상태 화면을 `/requests/:requestId` 경로로 정의하므로 새로고침과 주소 직접 진입이 일급 경로다. 요청 식별자는 URL에 있어 살아남지만 **접수 시각은 메모리에만 두면 소실**된다. 그러면 새로고침할 때마다 스케줄이 1초 간격에서 다시 시작되고 30초 상한도 초기화되어, 사용자가 새로고침을 반복하는 것만으로 정체된 요청을 **영구히 고빈도 폴링** 상태로 유지할 수 있다 — REQ-ENR-006이 막으려 한 혼잡 가중이 그대로 발생한다.

### B.4 취소 (CNL)

> **`DEP-2`는 해소되었다(§A.5).** 백엔드가 §A.4의 13·14번 조회 엔드포인트로 `enrollmentId`·`waitlistEntryId`를 노출하므로 이 절은 **구현 가능하며 검증 가능하다.** 0.1.0에서 이 절에 걸려 있던 차단 표기는 제거되었다. 남은 조건은 `DEP-3`(선행 PR 병합)이며, 이는 이 절만의 조건이 아니라 §A.4의 5~8·13·14번을 쓰는 **모든 절에 공통**이다(`plan.md` §B.3).

- **REQ-CNL-001** (Event-driven) — **When** 인증된 회원이 자신의 확정 수강신청 취소를 실행하면, 클라이언트는 `DELETE /api/enrollments/{enrollmentId}`를 호출하고 응답의 요청 식별자로 폴링을 개시 **shall**한다. 이때 사용하는 `enrollmentId`는 `GET /api/enrollments/mine` 응답의 `enrollmentId` 필드에서만 유래 **shall**한다.
- **REQ-CNL-002** (Ubiquitous) — 취소 응답(202) 직후의 화면 표시는 확정된 취소 완료를 뜻하는 표현을 사용 **shall not**한다 (REQ-ENR-002와 동일한 근거 — 취소도 큐를 경유한다).
- **REQ-CNL-003** (Event-driven) — **When** 인증된 회원이 자신의 대기명단 항목 취소를 실행하면, 클라이언트는 `DELETE /api/waitlist-entries/{entryId}`를 호출 **shall**한다. 경로 변수에 넣는 값은 `GET /api/waitlist-entries/mine` 응답의 **`waitlistEntryId`** **shall**이며, 같은 응답의 `position`을 사용 **shall not**한다. 이 호출은 200 동기 응답이므로 폴링을 개시 **shall not**한다.

  > 근거: `waitlistEntryId`와 `position`은 둘 다 `Long`이라 타입 검사가 혼동을 잡아 주지 못한다(§A.4). `position`을 넣으면 엉뚱한 행을 지목하며, 그것이 본인 소유 행이면 **의도하지 않은 항목이 조용히 취소**된다 — 403/404조차 나지 않으므로 화면상 정상 동작으로 보인다.

  > 대기명단 취소만 큐를 경유하지 않는 비대칭이 존재한다(백엔드 `WaitlistController`가 `WaitlistService.cancel`을 직접 호출). 화면은 이 두 취소를 시각적으로 유사하게 보이더라도 **후속 동작이 다르다**는 점을 구현에서 혼동 **shall not**한다.
- **REQ-CNL-004** (Ubiquitous) — 클라이언트는 취소 대상 식별자를 사용자 입력으로 직접 받 **shall not**한다. 식별자는 백엔드 조회 응답에서만 획득 **shall**된다.

  > 근거: `DEP-2` 해소 전에는 식별자 직접 입력이 손쉬운 우회처럼 보였고, 해소된 지금은 조회 API가 있으므로 그 유혹 자체가 사라졌다. 그럼에도 이 금지를 요구사항으로 남기는 이유는, 목록에 없는 항목을 취소하고 싶다는 요구가 나올 때 같은 유혹이 재발하기 때문이다. 타인의 식별자를 시험 삼아 입력하는 경로를 화면이 제공하면, 백엔드가 소유권을 2계층으로 검증하여 실제 침해는 막더라도 **화면이 그런 조작을 유도하는 것 자체가 잘못된 설계**다.
- **REQ-CNL-005** (Event-driven) — **When** 취소 요청이 403 또는 404를 반환한 것이 감지되면, 클라이언트는 대상이 사용자 소유가 아니거나 존재하지 않음을 안내 **shall**하며, 두 응답을 구별하여 소유자 존재 여부를 추측할 수 있는 정보를 노출 **shall not**한다.
- **REQ-CNL-006** (Event-driven) — **When** 인증된 회원이 내 수강신청 화면 또는 내 대기명단 화면에 진입하면, 클라이언트는 각각 `GET /api/enrollments/mine`·`GET /api/waitlist-entries/mine`을 호출하여 취소 가능한 항목을 표시 **shall**한다. 두 요청에 회원 식별자를 질의 파라미터·경로 변수·요청 본문 중 어떤 형태로도 실어 보내 **shall not**한다.

  > 근거: 백엔드 `REQ-LST-003`이 반환 범위를 인증 주체에서만 유도하도록 규정했다. 클라이언트가 회원 식별자를 실어 보내면 백엔드가 무시하므로 무해해 보이나, 화면이 "회원 식별자를 바꾸면 타인 목록을 볼 수 있다"는 잘못된 모델을 드러내게 된다.
- **REQ-CNL-007** (Event-driven) — **When** 두 목록 조회 중 하나가 `200`과 빈 배열 `[]`을 반환한 것이 감지되면, 클라이언트는 이를 **보유 내역 없음**으로 표시 **shall**하며 오류로 표시 **shall not**한다.

  > 근거: 백엔드 `REQ-LST-001`·`REQ-LST-002`가 0건일 때 404가 아니라 200 + 빈 배열을 반환하도록 명시했다. 빈 배열을 오류로 해석하면 **아직 아무것도 신청하지 않은 모든 신규 회원**이 첫 진입에서 오류 화면을 보게 된다 — 가장 흔한 정상 상태가 오류로 표시되는 결함이다.
- **REQ-CNL-008** (Ubiquitous) — 클라이언트는 두 목록 조회 응답의 항목 순서를 백엔드가 반환한 그대로 표시 **shall**하며, 자체 기준으로 재정렬 **shall not**한다.

  > 근거: 백엔드가 확정 목록은 `enrollmentId` 오름차순, 대기 목록은 `position` 오름차순으로 반환한다(§A.4). 이 순서는 **결정적(deterministic)** 이며, 클라이언트가 재정렬하면 화면에 보이는 순서가 응답의 순서와 어긋나 AC-FE-110(응답 순서와 표시 순서의 대조)이 검증하려는 대상 자체가 사라진다. 순서 재구성은 이득이 없고 검증 가능성만 잃는다.

  > **`position`에 관한 주의 (이 요구사항이 함의하지 **않는** 것)**: 대기 목록의 `position`은 **그 항목이 속한 강좌 안에서의 순번**이며(§A.4 — 백엔드가 `nextPosition`을 강좌별로 계산하고 부분 유니크 인덱스도 `(course_id, position)`이다), 회원의 전체 대기 현황을 가로지르는 순번이 아니다. 여러 강좌에 대기 중인 회원의 목록을 `position` 오름차순으로 늘어놓아도 그것은 **승격 예정 순서가 아니다** — 각 강좌의 대기열은 그 강좌의 정원 변동에 따라 독립적으로 진행한다. 따라서 화면은 `position`을 반드시 `courseTitle`과 나란히 렌더링 **shall**하며, 목록 전역의 대기 순위처럼 보이는 표현을 사용 **shall not**한다. 이를 어기면 INV-FE-004 계열의 결함(화면이 사실이 아닌 것을 사용자에게 말함)이 된다.
- **REQ-CNL-009** (Ubiquitous) — 취소가 성공한 뒤 클라이언트가 표시하는 목록은 해당 조회 API를 **재호출하여 얻은 결과** **shall**이며, 클라이언트가 보유하던 이전 목록에서 항목을 임의로 제거한 결과를 표시 **shall not**한다.

  > 근거: 확정 취소는 큐를 경유하므로(REQ-CNL-001) 202 시점에 아직 취소가 확정되지 않았다. 클라이언트가 목록에서 항목을 먼저 지우면 INV-FE-001(202를 확정으로 표시하지 않음)을 목록 화면에서 우회하게 된다.

### B.5 관리자 화면 (ADM)

- **REQ-ADM-001** (Capability gate) — **Where** 세션의 역할이 `ADMIN`이면, 클라이언트는 관리자 화면 진입 수단을 노출 **shall**한다. 그 외의 경우 노출 **shall not**한다.
- **REQ-ADM-002** (Event-driven) — **When** 역할이 `ADMIN`이 아닌 사용자가 관리자 경로로 직접 진입한 것이 감지되면, 클라이언트는 해당 화면을 렌더링 **shall not**하고 권한 없음을 안내 **shall**한다.
- **REQ-ADM-003** (Ubiquitous) — 클라이언트 측 경로 가드는 보안 통제가 아니라 UX 장치 **shall**이며, 실제 권한 강제는 백엔드의 403에 의존 **shall**한다 (§A.7). 이 사실은 코드 주석과 산출 문서에 기록 **shall**된다.
- **REQ-ADM-004** (Event-driven) — **When** 관리자가 강좌 생성을 실행하면, 클라이언트는 `POST /api/admin/courses`를 호출 **shall**한다.
- **REQ-ADM-005** (Event-driven) — **When** 관리자가 강좌 수정을 실행하면, 클라이언트는 `PATCH /api/admin/courses/{id}`를 호출 **shall**하며, 백엔드가 필수로 요구하는 모든 필드(제목·정원·시작 일시·종료 일시)를 현재 값으로 채워 전송 **shall**한다.

  > 근거: 엔드포인트가 `PATCH`임에도 요청 본문의 필드가 모두 필수다(§A.4). 변경 필드만 보내는 통상적인 `PATCH` 관행을 따르면 400이 반환된다.
- **REQ-ADM-006** (Event-driven) — **When** 관리자가 정원을 현재 값보다 크게 수정하면, 클라이언트는 이 조작이 대기자 승격을 유발할 수 있음을 안내 **shall**하고, 승격 결과가 즉시 반영되지 않을 수 있음을 표시 **shall**한다.

  > 근거: 백엔드 REQ-ADX-001에 따라 정원 증설은 `CAPACITY_INCREASE` 큐 요청을 적재하며, 승격은 워커가 비동기로 수행한다. 관리자 API 응답이 200으로 돌아온 시점에는 아직 승격이 반영되지 않았을 수 있다.
- **REQ-ADM-007** (Event-driven) — **When** 정원 축소 요청이 409(`CAPACITY_BELOW_ENROLLMENT`)로 거부된 것이 감지되면, 클라이언트는 현재 확정 인원 미만으로 축소할 수 없음을 안내 **shall**한다.
- **REQ-ADM-008** (Event-driven) — **When** 관리자가 강좌 마감을 실행하면, 클라이언트는 `POST /api/admin/courses/{id}/close` 또는 `DELETE /api/admin/courses/{id}` 중 하나를 호출 **shall**한다.
- **REQ-ADM-009** (Ubiquitous) — 강좌 마감 조작의 화면 표기는 데이터가 제거된다는 의미의 "삭제"를 사용 **shall not**하며, 확정자·대기자가 보존된 채 모집만 중단됨을 표현 **shall**한다 (§A.4).
- **REQ-ADM-010** (Ubiquitous) — 관리자 화면의 강좌 목록은 공개 카탈로그 엔드포인트(`GET /api/courses`)를 재사용 **shall**한다. 관리자 전용 목록 조회 엔드포인트는 존재하지 않는다.

### B.6 오류 처리 (ERR)

- **REQ-ERR-001** (Ubiquitous) — 클라이언트는 백엔드의 오류 응답 3종(§A.6 F1·F2·F3)을 모두 해석 **shall**하며, 어느 형태에 대해서도 화면에 의미 없는 문구만 표시 **shall not**한다.
- **REQ-ERR-002** (Ubiquitous) — 오류 해석은 단일 지점에서 수행 **shall**되며, 각 화면이 개별적으로 오류 바디를 해석 **shall not**한다.
- **REQ-ERR-003** (Event-driven) — **When** 응답 바디에 `code` 필드가 존재하면, 클라이언트는 그 값을 사용자 안내 문구 선택의 근거로 사용 **shall**한다.
- **REQ-ERR-004** (Event-driven) — **When** 응답 바디에 `code` 필드가 없는 것이 감지되면, 클라이언트는 HTTP 상태 코드에 근거한 안내 문구로 대체 **shall**하며, 원문을 그대로 노출 **shall not**한다.
- **REQ-ERR-005** (Ubiquitous) — 클라이언트는 백엔드에 도달하지 못한 실패(네트워크 단절·CORS 차단)와 백엔드가 반환한 오류를 구별하여 안내 **shall**한다.

  > 근거: `DEP-1`은 해소되었으나 CORS 차단은 여전히 **설정 실수로 재발할 수 있다** — 허용 오리진이 `http://localhost:5173` 하나로 고정되어 있으므로(§A.5), 개발 서버를 다른 포트로 띄우면 전 호출이 다시 차단된다. 이때 화면이 "서버 오류"라고 표시하면 개발자가 백엔드 로직을 잘못 의심하며 시간을 낭비한다. 두 실패를 구별하는 것은 이 SPEC의 목표("실제 실행 확인")에 직접 기여한다.
- **REQ-ERR-006** (Ubiquitous) — 사용자 안내 문구는 스택 트레이스·내부 경로·예외 클래스명을 포함 **shall not**한다.

### B.7 비기능 요구사항 (NFR)

- **REQ-NFR-001** (Ubiquitous) — 프론트엔드 워크스페이스는 저장소 내 `frontend/` 디렉터리에 배치 **shall**되며, 백엔드 소스 트리(`src/`)와 파일을 공유 **shall not**한다.
- **REQ-NFR-002** (Ubiquitous) — 이 SPEC의 구현은 백엔드 소스 파일(`src/**`)과 빌드 설정(`build.gradle`)을 수정 **shall not**한다. 백엔드 변경이 필요하면 §A.5의 의존성으로 처리 **shall**된다.
- **REQ-NFR-003** (Ubiquitous) — 백엔드 API의 기준 주소는 빌드 시점 환경 설정으로 주입 **shall**되며, 소스에 하드코딩 **shall not**된다.
- **REQ-NFR-004** (Ubiquitous) — 클라이언트는 정적 타입 검사를 통과 **shall**하며, 타입 오류·린트 오류가 0건 **shall**이어야 한다.
- **REQ-NFR-005** (Ubiquitous) — 백엔드 응답 형태에 대응하는 타입 정의는 단일 지점에 선언 **shall**되며, 화면별로 중복 선언 **shall not**된다.
- **REQ-NFR-006** (Ubiquitous) — 세션 토큰·비밀번호는 로그·오류 보고·URL 질의 문자열에 기록 **shall not**된다.
- **REQ-NFR-007** (Ubiquitous) — 이 SPEC의 검증은 사람이 브라우저에서 시나리오를 수행하여 확인하는 절차를 포함 **shall**한다. 자동 테스트만으로 완료를 선언 **shall not**한다.

  > 근거: 이 SPEC의 존재 이유 자체가 "자동 테스트는 통과했으나 아무도 클릭해 보지 않았다"는 상태를 해소하는 것이다(§A.1). 자동 테스트만으로 완료를 선언하면 이 SPEC은 자신의 목적을 달성하지 못한 채 닫힌다.

---

## §C 시스템 불변식 (Invariants)

| ID | 불변식 | 근거 |
|---|---|---|
| INV-FE-001 | 화면은 202 응답을 확정된 결과로 표시하지 않는다 | REQ-ENR-002, REQ-CNL-002 |
| INV-FE-002 | 종단 상태에 도달한 요청에 대한 상태 조회 호출은 발생하지 않는다 | REQ-ENR-005 |
| INV-FE-003 | 세션 토큰은 탭 수명을 넘어 존속하지 않는다 | REQ-SES-004 |
| INV-FE-004 | 화면은 백엔드가 갖지 않은 능력(서버 측 로그아웃·토큰 무효화)을 암시하지 않는다 | REQ-SES-006 |
| INV-FE-005 | 클라이언트 경로 가드의 우회가 권한 상승으로 이어지지 않는다 (백엔드 403이 최종 방어선) | REQ-ADM-003, §A.7 |
| INV-FE-006 | 오류 바디 해석 실패가 화면 중단으로 이어지지 않는다 | REQ-ERR-001, REQ-ERR-004 |
| INV-FE-007 | 백엔드 소스 트리(`src/**`)는 이 SPEC의 구현으로 변경되지 않는다 | REQ-NFR-002 |
| INV-FE-008 | 취소 대상 식별자는 사용자 입력이 아닌 백엔드 응답에서만 유래한다 | REQ-CNL-004 |
| INV-FE-009 | 대기명단 취소에 전달되는 값은 `waitlistEntryId`이며 `position`이 아니다 | REQ-CNL-003, §A.4 |
| INV-FE-010 | 목록 조회의 빈 배열은 오류가 아니라 정상 상태로 표시된다 | REQ-CNL-007 |

---

## §D 범위 제외 (Exclusions)

이 절은 이 SPEC이 **의도적으로 만들지 않는 것**을 열거한다. 여기 있는 항목을 구현하는 것은 범위 이탈이다.

### Out of Scope — 백엔드 코드 변경

- `src/**` 하위 모든 Java 소스의 수정
- `build.gradle`·`application.properties` 등 백엔드 빌드/설정 파일의 수정
- `SecurityConfig`의 CORS 설정 **변경** — 설정 자체는 백엔드가 이미 추가했다(`DEP-1` 해소). 허용 오리진·헤더·메서드가 부족하더라도 이 SPEC은 그것을 고치지 않고 §A.5의 제약에 맞춰 클라이언트를 구성한다
- 취소 대상 조회 API의 **계약 변경** — API 자체는 백엔드가 이미 추가했다(`DEP-2` 해소). 경로·필드명·정렬·빈 목록 동작이 불편하더라도 이 SPEC은 실제 계약을 그대로 소비한다

### Out of Scope — 시각적 완성도

- 디자인 시스템 도입, 테마·다크모드
- 애니메이션·전환 효과
- 정교한 반응형 레이아웃 (모바일 전용 화면 설계)
- 접근성 심화 대응 (스크린 리더 최적화·키보드 내비게이션 전면 설계)

  > 이 SPEC의 목표는 배선 검증이며 시각적 완성도가 아니다(§A.1). 위 항목은 후속 SPEC의 영역이다.

### Out of Scope — 백엔드가 제공하지 않는 기능

- 결제 화면 — 백엔드 v1이 결제를 범위에서 제외했다(`product.md`)
- 알림 수신 화면 (이메일·푸시) — 백엔드가 폴링으로 대체했다
- 서버 강제 로그아웃 / 토큰 폐기 화면 — 백엔드에 denylist가 없다
- 관리자의 타인 수강신청 대리 취소 — 백엔드 REQ-CNL-004가 금지한다
- 강좌 검색·필터·정렬 — `GET /api/courses`가 page·size만 받는다
- 회원 정보 수정·비밀번호 변경·회원 탈퇴 — 해당 엔드포인트가 존재하지 않는다

### Out of Scope — 인프라 및 배포

- 프로덕션 배포 파이프라인, 정적 호스팅 구성
- 컨테이너 이미지 빌드, 백엔드와의 통합 배포 구성
- 브라우저 자동화 기반 종단 간 테스트 스위트 구축

  > `REQ-NFR-007`이 요구하는 것은 **사람에 의한 수동 시나리오 확인**이며, 자동화 E2E 도구 도입이 아니다.

### Out of Scope — 다른 SPEC이 소유한 영역

- 큐·워커·정원 정합성 로직 → `SPEC-ENROLLMENT-001`
- 강좌 도메인 규칙 → `SPEC-COURSE-001`
- 인증·인가 규칙 → `SPEC-AUTH-001`
- `.moai/project/{tech,structure,product}.md`의 갱신 → sync 단계(manager-docs)

---

## §E 성공 기준

1. 브라우저에서 **회원가입 → 로그인 → 강좌 목록 → 강좌 상세 → 수강신청 → 상태 확정 표시** 시나리오가 사람의 조작으로 완주된다.
2. 정원이 찬 강좌에 신청하면 화면이 `WAITLISTED`와 **대기 순번**을 표시한다.
3. 관리자 계정으로 로그인하면 관리자 메뉴가 나타나고, 일반 회원 계정에서는 나타나지 않는다.
4. 관리자가 강좌를 생성·수정·마감할 수 있고, 마감된 강좌는 일반 화면에서 신청 조작이 제공되지 않는다.
5. 관리자가 정원을 증설하면 화면이 대기자 승격이 비동기로 일어남을 안내하고, 잠시 후 재조회 시 확정 인원 증가가 관측된다.
6. 정원을 확정 인원 미만으로 축소하면 화면이 409를 해석한 안내 문구를 표시한다 (원문 노출 없음).
7. 토큰 만료 후 임의 조작 시 화면이 재로그인을 안내하고, 사용자가 재로그인하여 작업을 이어갈 수 있다.
8. 브라우저 탭을 닫았다 다시 열면 세션이 남아 있지 않다.
9. 접수 후 종단 상태에 도달하면 상태 조회 호출이 중단된다 (개발자 도구 네트워크 탭으로 관측 가능).
10. 내 수강신청·내 대기명단 목록이 화면에 표시되고, 그 목록에서 선택한 항목의 확정 수강신청 취소와 대기명단 취소가 완주된다. 보유 내역이 0건인 신규 회원에게는 오류가 아니라 "내역 없음"이 표시된다.
11. 타입 검사·린트 오류 0건.
12. 백엔드 소스 트리(`src/**`)의 변경이 0건이다.

---

## §F 참조

- 선행 SPEC: `.moai/specs/SPEC-AUTH-001/spec.md`, `.moai/specs/SPEC-COURSE-001/spec.md`, `.moai/specs/SPEC-ENROLLMENT-001/spec.md`
- 기술 조사: `.moai/specs/SPEC-FRONTEND-001/research.md`
- 설계: `.moai/specs/SPEC-FRONTEND-001/design.md`
- 구현 계획: `.moai/specs/SPEC-FRONTEND-001/plan.md`
- 인수 기준: `.moai/specs/SPEC-FRONTEND-001/acceptance.md`
- 제품/구조/기술: `.moai/project/{product,structure,tech}.md`
