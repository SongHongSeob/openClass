---
id: SPEC-AUTH-001
title: "회원 가입·로그인 및 JWT 인증 기반 — 인수 기준"
version: "0.1.1"
status: draft
created: 2026-08-15
updated: 2026-08-15
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/member"
lifecycle: spec-anchored
tags: "auth, jwt, member, acceptance"
tier: M
---

# SPEC-AUTH-001 — 인수 기준 (acceptance)

모든 인수 기준은 **자동화된 Spring Boot 테스트로 관찰 가능**해야 한다. "안전해 보인다", "잘 동작한다" 같은 주관적 판정은 인수 기준이 될 수 없다. 프론트엔드 없이 100% 검증된다.

---

## §D.1 인수 기준

### 회원 등록

**AC-AUTH-001** — 회원가입 성공 · 심각도: 필수
- **Given** 등록되지 않은 이메일 `a@example.com`이 주어졌을 때
- **When** 해당 이메일과 8자 이상 비밀번호로 회원가입 API를 호출하면
- **Then** 201(또는 200)이 반환되고, `member` 테이블에 해당 행이 정확히 1건 생성되며, 그 행의 `role`이 `MEMBER`이고 `password_hash`가 입력 평문과 다르며 BCrypt 접두사(`$2a$`/`$2b$`/`$2y$`) 형식을 만족한다.
- 대응: REQ-SIGNUP-001, REQ-SIGNUP-002

**AC-AUTH-002** — 중복 이메일 가입 거부 · 심각도: 필수
- **Given** `a@example.com`이 이미 가입되어 있을 때
- **When** 동일 이메일로 다시 회원가입을 요청하면
- **Then** 409(또는 400)가 반환되고 `member` 행 수가 증가하지 않는다. 또한 애플리케이션 계층을 우회하여 동일 이메일 행을 직접 INSERT하면 DB 유니크 제약 위반 예외가 발생한다.
- 대응: REQ-SIGNUP-003, REQ-SIGNUP-006, INV-AUTH-004

**AC-AUTH-003** — 입력 검증 실패 · 심각도: 필수
- **Given** 형식이 잘못된 이메일(`not-an-email`) 또는 8자 미만 비밀번호가 주어졌을 때
- **When** 회원가입을 요청하면
- **Then** 400이 반환되고 `member` 행이 생성되지 않는다.
- 대응: REQ-SIGNUP-004, REQ-NFR-001

**AC-AUTH-004** — 역할 주입 차단 · 심각도: 필수
- **Given** 미등록 이메일이 주어졌을 때
- **When** 회원가입 요청 본문에 `role: "ADMIN"` 필드를 추가하여 호출하면
- **Then** 가입은 성공하되 생성된 `member` 행의 `role`은 `MEMBER`이고, DB 전체에서 회원가입 API를 통해 생성된 `ADMIN` 행은 0건이다.
- 대응: REQ-SIGNUP-005, INV-AUTH-002

**AC-AUTH-005** — 저장소 평문 부재 · 심각도: 필수
- **Given** 비밀번호 `PlainSecret123`으로 회원 3명이 가입했을 때
- **When** `member` 테이블 전체 컬럼 값을 문자열로 덤프하여 검사하면
- **Then** 문자열 `PlainSecret123`이 어떤 컬럼에서도 발견되지 않는다.
- 대응: INV-AUTH-001

**AC-AUTH-006** — 이메일 정규화 · 심각도: 필수
- **Given** `  A@Example.COM  `(앞뒤 공백·대문자 포함)으로 회원가입했을 때
- **When** 저장된 행을 조회하고, 이어서 `a@example.com`으로 다시 회원가입을 시도하면
- **Then** 저장된 이메일은 `a@example.com`이며, 두 번째 가입은 중복으로 거부되고 `member` 행 수는 1건으로 유지된다. 또한 `a@example.com`으로 로그인이 성공한다.
- 대응: REQ-SIGNUP-007

### 로그인 및 토큰 발급

**AC-AUTH-007** — 로그인 성공 및 토큰 클레임 · 심각도: 필수
- **Given** 가입된 회원이 존재할 때
- **When** 올바른 자격 증명으로 로그인 API를 호출하면
- **Then** 200과 함께 액세스 토큰이 반환되고, 그 토큰을 디코딩하면 `sub`·`role`·`iat`·`exp` 클레임이 모두 존재하며 `role`이 `MEMBER`이고, 페이로드 문자열에 비밀번호 평문·비밀번호 해시가 포함되지 않는다.
- 대응: REQ-LOGIN-001, REQ-LOGIN-003

**AC-AUTH-008** — 로그인 실패 구별 불가 · 심각도: 필수
- **Given** `a@example.com`만 가입되어 있을 때
- **When** (i) `a@example.com` + 틀린 비밀번호, (ii) 미가입 `zzz@example.com` + 임의 비밀번호로 각각 로그인하면
- **Then** 두 경우 모두 401이 반환되고, 두 응답의 상태 코드와 응답 본문이 **바이트 단위로 동일**하다.
- 대응: REQ-LOGIN-002

**AC-AUTH-009** — 서명 비밀키 외부 주입 · 심각도: 필수
- **Given** 테스트 프로퍼티로 서명 비밀키 `secret-A`를 주입한 컨텍스트가 기동되었을 때
- **When** 그 컨텍스트에서 발급한 토큰을, 비밀키 `secret-B`로 구성한 검증기로 검증하면
- **Then** 검증이 실패한다. 또한 소스 트리 정적 검색에서 서명 비밀키 리터럴이 프로덕션 코드에 존재하지 않는다.
- 검증 방법: 프로퍼티 주입 통합 테스트 + 소스 검색(grep/ast-grep)
- 대응: REQ-LOGIN-004

### 토큰 검증 및 인가

> **검증 대상 엔드포인트 (AC-AUTH-010 ~ 014 공통 전제)**: 이 SPEC의 프로덕션 엔드포인트는 회원가입·로그인 2개뿐이며 둘 다 공개다. 따라서 아래 다섯 AC는 plan.md §C.5.1이 선언한 **테스트 전용 픽스처 컨트롤러**의 두 경로를 호출한다 — 보호 엔드포인트는 `GET /api/test/protected`, 관리자 엔드포인트는 `GET /api/admin/test-ping`. 픽스처는 경로를 제공만 하며 401/403 판정은 프로덕션 `SecurityConfig`가 수행한다. 픽스처는 테스트 소스 트리에만 존재한다.

**AC-AUTH-010** — 토큰 없음 차단 · 심각도: 필수
- **Given** `Authorization` 헤더가 없는 클라이언트일 때
- **When** 보호 엔드포인트 `GET /api/test/protected`를 호출하면
- **Then** 401이 반환된다.
- 대응: REQ-AUTHZ-001

**AC-AUTH-011** — 위조·만료 토큰 차단 · 심각도: 필수
- **Given** (i) 서명 바이트를 1비트 변조한 토큰, (ii) `exp`가 현재 시각보다 1초 이전인 토큰이 각각 주어졌을 때
- **When** 각 토큰으로 보호 엔드포인트 `GET /api/test/protected`를 호출하면
- **Then** 두 경우 모두 401이 반환되고, 요청이 픽스처 핸들러에 도달하지 않는다(핸들러 호출 카운터가 0이다).
- 대응: REQ-AUTHZ-002, INV-AUTH-003

**AC-AUTH-012** — MEMBER의 관리자 엔드포인트 접근 차단 · 심각도: 필수
- **Given** `MEMBER` 역할 토큰을 가진 회원일 때
- **When** 관리자 엔드포인트 `GET /api/admin/test-ping`(`/api/admin/**` 패턴)을 호출하면
- **Then** 403이 반환된다 (401 아님 — 인증은 되었으나 인가 실패).
- 대응: REQ-AUTHZ-003

**AC-AUTH-013** — ADMIN의 관리자 엔드포인트 접근 허용 · 심각도: 필수
- **Given** `ADMIN` 역할 토큰을 가진 회원일 때
- **When** 관리자 엔드포인트 `GET /api/admin/test-ping`을 호출하면
- **Then** 401도 403도 반환되지 않고 요청이 픽스처 핸들러에 도달한다(200 + 픽스처 고정 본문).
- 대응: REQ-AUTHZ-004

**AC-AUTH-014** — 무상태 검증 · 심각도: 필수
- **Given** 로그인으로 토큰을 발급받았을 때
- **When** 서버의 `HttpSession` 저장소를 비운(또는 세션 생성 정책이 `STATELESS`인) 상태에서 같은 토큰으로 보호 엔드포인트 `GET /api/test/protected`를 20회 호출하면
- **Then** 20회 모두 성공하고, 응답에 `Set-Cookie: JSESSIONID`가 포함되지 않는다.
- 대응: REQ-AUTHZ-005

**AC-AUTH-015** — 공개 엔드포인트 무토큰 접근 · 심각도: 필수
- **Given** 토큰이 없는 클라이언트일 때
- **When** 이 SPEC이 소유한 공개 엔드포인트(회원가입·로그인)를 호출하면
- **Then** 401이 반환되지 않는다 (각 엔드포인트의 정상 처리 결과가 반환된다).
- 범위 주석: REQ-AUTHZ-006이 함께 언급하는 **강좌 조회**는 이 SPEC이 생성하지 않는 엔드포인트이므로 여기서 호출하지 않는다. 해당 동작 검증은 `SPEC-COURSE-001` AC-CAT-001(비인증 목록 조회)이 담당한다 — §D.2 매트릭스의 교차 SPEC 이관 표기 참조.
- 대응: REQ-AUTHZ-006

### 최초 관리자 시더

**AC-AUTH-016** — 관리자 계정 생성 · 심각도: 필수
- **Given** `member` 테이블이 비어 있고 설정에 관리자 이메일·비밀번호가 지정되어 있을 때
- **When** 애플리케이션 컨텍스트를 기동하면
- **Then** 해당 이메일의 `member` 행이 1건 생성되고 그 `role`이 `ADMIN`이며, 그 계정으로 로그인하면 `role: ADMIN` 클레임을 가진 토큰이 발급된다. 초기 비밀번호 리터럴은 프로덕션 소스에서 발견되지 않는다.
- 대응: REQ-SEED-001, REQ-SEED-003

**AC-AUTH-017** — 시더 멱등성 · 심각도: 필수
- **Given** 관리자 계정이 이미 존재하고 그 `password_hash`가 기록되어 있을 때
- **When** 시더 실행 경로를 한 번 더 수행하면
- **Then** `member` 행 수가 증가하지 않고, 기존 관리자 행의 `password_hash`가 변경되지 않는다.
- 대응: REQ-SEED-002

### 비기능

**AC-AUTH-018** — 민감 정보 로그 미기록 · 심각도: 필수
- **Given** 회원가입·로그인·보호 엔드포인트 호출을 각각 1회 수행했을 때
- **When** 수집된 로그 출력 전체를 검사하면
- **Then** 평문 비밀번호, `password_hash` 값, 발급된 액세스 토큰 문자열, 서명 비밀키가 모두 발견되지 않는다.
- 대응: REQ-NFR-002

**AC-AUTH-019** — 백엔드 단독 검증 가능성 · 심각도: 필수
- **Given** 이 SPEC의 전체 테스트 스위트가 주어졌을 때
- **When** 프론트엔드 산출물이 존재하지 않는 저장소 상태에서 `./gradlew test`를 실행하면
- **Then** §D.1의 모든 필수 AC가 통과하고, 테스트 소스에 프론트엔드 빌드 산출물에 대한 의존이 존재하지 않는다.
- 대응: REQ-NFR-003

**AC-AUTH-020** — 커버리지 및 정적 품질 · 심각도: 필수
- **Given** 전체 테스트를 실행했을 때
- **When** 커버리지 리포트와 LSP 진단을 확인하면
- **Then** 커버리지 ≥ 85%, LSP 에러 0건, 타입에러 0건, 린트에러 0건이다.
- 범위 주석: TDD 준수 여부(테스트를 먼저 작성했는가)는 산출물 관찰로 검증할 수 없으므로 이 AC의 대상이 아니다. 해당 지시는 plan.md §D의 프로세스 제약으로 관리한다.
- 대응: REQ-NFR-004

---

## §D.2 추적성 매트릭스 (요구사항 → 인수 기준)

범위 표기(`~`)를 사용하지 않고 요구사항 1건당 1행으로 나열한다.

| 요구사항 | 대응 인수 기준 |
|---|---|
| REQ-SIGNUP-001 | AC-AUTH-001 |
| REQ-SIGNUP-002 | AC-AUTH-001 |
| REQ-SIGNUP-003 | AC-AUTH-002 |
| REQ-SIGNUP-004 | AC-AUTH-003 |
| REQ-SIGNUP-005 | AC-AUTH-004 |
| REQ-SIGNUP-006 | AC-AUTH-002 |
| REQ-SIGNUP-007 | AC-AUTH-006 |
| REQ-LOGIN-001 | AC-AUTH-007 |
| REQ-LOGIN-002 | AC-AUTH-008 |
| REQ-LOGIN-003 | AC-AUTH-007 |
| REQ-LOGIN-004 | AC-AUTH-009 |
| REQ-AUTHZ-001 | AC-AUTH-010 |
| REQ-AUTHZ-002 | AC-AUTH-011 |
| REQ-AUTHZ-003 | AC-AUTH-012 |
| REQ-AUTHZ-004 | AC-AUTH-013 |
| REQ-AUTHZ-005 | AC-AUTH-014 |
| REQ-AUTHZ-006 | AC-AUTH-015 (회원가입·로그인) + **교차 SPEC 이관**: 강좌 조회는 `SPEC-COURSE-001` AC-CAT-001이 검증 |
| REQ-SEED-001 | AC-AUTH-016 |
| REQ-SEED-002 | AC-AUTH-017 |
| REQ-SEED-003 | AC-AUTH-016 |
| REQ-NFR-001 | AC-AUTH-003 |
| REQ-NFR-002 | AC-AUTH-018 |
| REQ-NFR-003 | AC-AUTH-019 |
| REQ-NFR-004 | AC-AUTH-020 |
| INV-AUTH-001 | AC-AUTH-005 |
| INV-AUTH-002 | AC-AUTH-004 |
| INV-AUTH-003 | AC-AUTH-011 |
| INV-AUTH-004 | AC-AUTH-002 |

요구사항 24건 + 불변식 4건 = 28건, 인수 기준 20건. 미대응 요구사항 0건.

---

## §D.3 완료 정의 (Definition of Done)

아래 항목이 **모두** 충족되어야 이 SPEC이 완료로 전이될 수 있다. §D.1의 모든 AC는 심각도 "필수"이므로 예외 없이 전부 통과해야 한다.

- [ ] §D.1의 AC-AUTH-001 ~ AC-AUTH-020이 **전부 통과**한다.
- [ ] §D.2 추적성 매트릭스의 요구사항 24건 + 불변식 4건이 모두 통과한 AC로 커버된다.
- [ ] 전체 테스트 커버리지 ≥ 85%, 커밋별 ≥ 80%.
- [ ] LSP 에러·타입에러·린트에러 0건, ast-grep 게이트 통과.
- [ ] spec.md §D(범위 제외) 항목이 구현되지 않았음이 확인된다 (리프레시 토큰·폐기 목록·소셜 로그인·비밀번호 재설정 부재).
- [ ] spec.md §D에 기록된 **로그아웃 제약**이 README 또는 API 문서에 사용자 대상으로 명시되어 있다.
- [ ] plan.md §C.5.1의 테스트 픽스처 컨트롤러가 **테스트 소스 트리에만** 존재한다 — `src/main` 트리에서 `/api/test/` 문자열이 0건이다.
- [ ] 미해소 클래리피케이션 마커가 이 SPEC의 어느 산출물에도 남아 있지 않다.
