---
id: SPEC-AUTH-001
title: "회원 가입·로그인 및 JWT 인증 기반"
version: "0.1.1"
status: draft
created: 2026-08-15
updated: 2026-08-15
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/member"
lifecycle: spec-anchored
tags: "auth, jwt, member, role, security, spring-boot"
tier: M
---

# SPEC-AUTH-001 — 회원 가입·로그인 및 JWT 인증 기반

## HISTORY

| 버전 | 날짜 | 작성자 | 변경 내용 |
|---|---|---|---|
| 0.1.0 | 2026-08-15 | manager-spec | 최초 작성 (draft) — SPEC-ENROLLMENT-001 3분할 중 1번. 인증 전략은 사용자 결정에 따라 **JWT**로 확정 |
| 0.1.1 | 2026-08-15 | manager-spec | 2차 감사 지적 반영 — A1(테스트 전용 보호/관리자 엔드포인트 픽스처 미선언 → plan.md §C.5·M3에 선언), A2(REQ-AUTHZ-006의 강좌 조회를 교차 SPEC 이관으로 명시), A3/E6(REQ-NFR-004에서 TDD 프로세스 절을 분리하여 plan.md §D 제약으로 이동, DoD 문구 정정) |

---

## §A 개요

### A.1 배경

문화센터 강좌 수강신청 서비스의 **인증 기반**이다. 이 SPEC은 프로젝트의 첫 SPEC이며, 여기서 확정되는 인증 방식·역할 모델·패키지 구조가 이후 `SPEC-COURSE-001`, `SPEC-ENROLLMENT-001`의 전제가 된다.

인증 방식은 **JWT 액세스 토큰**으로 확정되었다 (사용자 결정). 단일 액세스 토큰만 사용하며, 리프레시 토큰 회전과 분산 토큰 폐기 목록(denylist)은 v1 범위에서 제외한다 — 그 결과 **로그아웃은 클라이언트 측 토큰 폐기만으로 수행되고, 이미 발급된 토큰은 만료 시각까지 유효하다.** 이것은 누락이 아니라 **명시적으로 수용한 알려진 제약**이며 §D와 plan.md §C.2에 근거를 기록한다.

### A.2 용어 정의

| 용어 | 영문 식별자 | 의미 |
|---|---|---|
| 회원 | `Member` | 이메일·비밀번호로 가입/로그인하는 사용자 |
| 역할 | `MemberRole` | `MEMBER` 또는 `ADMIN`. 회원 1명은 정확히 1개 역할을 갖는다 |
| 액세스 토큰 | `AccessToken` | 로그인 성공 시 발급되는 서명된 단기 JWT |
| 보호 엔드포인트 | — | 유효한 액세스 토큰 없이는 접근할 수 없는 API |
| 관리자 엔드포인트 | — | 보호 엔드포인트 중 `ADMIN` 역할이 추가로 필요한 API |

### A.3 액세스 토큰 규격 (client-facing contract)

| 항목 | 값 | 비고 |
|---|---|---|
| 형식 | JWT (JWS, HMAC-SHA256) | 대칭키 서명 |
| 필수 클레임 | `sub`(회원 식별자), `role`, `iat`, `exp` | `role`은 `MEMBER` 또는 `ADMIN` |
| 유효 기간 | 30분 (설정값) | 짧은 수명이 폐기 목록 부재를 보완하는 유일한 장치다 |
| 전달 방식 | `Authorization: Bearer <token>` 헤더 | 쿠키 미사용 → CSRF 토큰 불필요 |
| 갱신 | **없음** (v1) | 만료 시 재로그인 |

---

## §B 요구사항 (GEARS)

### B.1 회원 등록 (SIGNUP)

- **REQ-SIGNUP-001** (Ubiquitous) — 인증 서비스는 회원 비밀번호를 단방향 적응형 해시(BCrypt)로만 저장 **shall**하며, 평문 또는 복호화 가능한 형태로 저장 **shall not**한다.
- **REQ-SIGNUP-002** (Event-driven) — **When** 미등록 이메일과 정책을 만족하는 비밀번호로 회원가입 요청이 도착하면, 인증 서비스는 역할 `MEMBER`인 회원 레코드를 1건 생성하고 성공 응답을 반환 **shall**한다.
- **REQ-SIGNUP-003** (Event-driven) — **When** 이미 등록된 이메일로 회원가입 요청이 도착하면, 인증 서비스는 회원 레코드를 생성하지 않고 중복 이메일 오류를 반환 **shall**한다.
- **REQ-SIGNUP-004** (Event-driven) — **When** 이메일 형식 위반 또는 비밀번호 정책(최소 8자) 위반이 감지되면, 인증 서비스는 400을 반환하고 회원 레코드를 생성 **shall not**한다.
- **REQ-SIGNUP-005** (Ubiquitous) — 회원가입 API는 요청 본문에 역할 지정 값이 포함되어 있어도 이를 무시 **shall**하며, `ADMIN` 역할의 회원을 생성 **shall not**한다.
- **REQ-SIGNUP-006** (Ubiquitous) — 이메일은 회원 전체에서 유일 **shall**하며, 데이터베이스는 중복 이메일 저장을 제약 조건 수준에서 거부 **shall**한다.
- **REQ-SIGNUP-007** (Ubiquitous) — 인증 서비스는 이메일을 저장·조회하기 전에 앞뒤 공백을 제거하고 소문자로 정규화 **shall**한다. 대소문자만 다른 두 이메일이 서로 다른 회원으로 등록 **shall not**된다.

### B.2 로그인 및 토큰 발급 (LOGIN)

- **REQ-LOGIN-001** (Event-driven) — **When** 올바른 이메일·비밀번호로 로그인 요청이 도착하면, 인증 서비스는 §A.3 규격의 액세스 토큰을 발급하고 응답 본문으로 반환 **shall**한다.
- **REQ-LOGIN-002** (Event-driven) — **When** 미등록 이메일 또는 불일치 비밀번호로 로그인 요청이 도착하면, 인증 서비스는 401을 반환 **shall**하며, 두 실패 사유를 구분할 수 있는 정보를 상태 코드·응답 본문 어디에도 포함 **shall not**한다.
- **REQ-LOGIN-003** (Ubiquitous) — 발급되는 액세스 토큰은 `sub`·`role`·`iat`·`exp` 클레임을 모두 포함 **shall**하며, 비밀번호 해시 등 인증 자격 증명을 클레임에 포함 **shall not**한다.
- **REQ-LOGIN-004** (Ubiquitous) — 액세스 토큰의 서명 비밀키는 애플리케이션 설정(외부 주입 가능한 프로퍼티)에서 읽어 **shall** 사용하며, 소스 코드에 상수로 포함 **shall not**한다.

### B.3 토큰 검증 및 인가 (AUTHZ)

- **REQ-AUTHZ-001** (State-driven) — **While** 요청에 유효한 액세스 토큰이 없는 상태이면, 인증 필터는 모든 보호 엔드포인트에 대해 401을 반환 **shall**한다.
- **REQ-AUTHZ-002** (Event-driven) — **When** 서명이 위조되었거나 `exp`가 현재 시각보다 이전인 토큰이 감지되면, 인증 필터는 해당 요청을 인증 실패로 처리하고 401을 반환 **shall**한다.
- **REQ-AUTHZ-003** (State-driven) — **While** 인증된 요청자의 역할이 `ADMIN`이 아니면, 인증 필터는 관리자 엔드포인트에 대해 403을 반환 **shall**한다.
- **REQ-AUTHZ-004** (State-driven) — **While** 인증된 요청자의 역할이 `ADMIN`이면, 인증 필터는 관리자 엔드포인트 접근을 허용 **shall**한다.
- **REQ-AUTHZ-005** (Ubiquitous) — 인증 필터는 요청마다 토큰을 검증 **shall**하며, 서버 측 세션 상태에 의존 **shall not**한다 (무상태 검증).
- **REQ-AUTHZ-006** (Ubiquitous) — 이 SPEC이 소유한 공개 엔드포인트(**회원가입·로그인**)는 토큰 없이 접근 가능 **shall**해야 한다. 강좌 조회 경로(`GET /api/courses`, `GET /api/courses/{id}`)도 같은 공개 규칙의 적용 대상이나, **그 엔드포인트 자체는 `SPEC-COURSE-001`이 생성**하므로 이 SPEC은 경로 패턴을 `SecurityConfig`에 선반영만 하고 동작 검증은 `SPEC-COURSE-001` AC-CAT-001에 위임 **shall**한다.

### B.4 최초 관리자 계정 (SEED)

- **REQ-SEED-001** (Event-driven) — **When** 애플리케이션이 기동되고 설정에 지정된 관리자 이메일의 회원이 존재하지 않는 것이 감지되면, 시더는 해당 이메일로 `ADMIN` 역할 회원을 1건 생성 **shall**한다.
- **REQ-SEED-002** (Event-driven) — **When** 애플리케이션이 기동되고 해당 관리자 회원이 이미 존재하면, 시더는 어떤 회원 레코드도 생성하거나 수정 **shall not**한다 (멱등성).
- **REQ-SEED-003** (Ubiquitous) — 시더가 사용하는 초기 관리자 비밀번호는 설정에서 주입 **shall**되며, 소스 코드에 하드코딩 **shall not**된다.

### B.5 비기능 요구사항 (NFR)

- **REQ-NFR-001** (Ubiquitous) — 모든 외부 입력은 서버 측에서 검증 **shall**되며, 클라이언트 측 검증만을 신뢰 **shall not**한다.
- **REQ-NFR-002** (Ubiquitous) — 시스템은 평문 비밀번호·비밀번호 해시·액세스 토큰 문자열·서명 비밀키를 로그에 기록 **shall not**한다.
- **REQ-NFR-003** (Ubiquitous) — 이 SPEC의 모든 인수 기준은 **Spring Boot 테스트만으로 검증 가능** **shall**해야 하며, 프론트엔드 구현을 전제 **shall not**한다.
- **REQ-NFR-004** (Ubiquitous) — 이 SPEC의 산출물은 커밋당 최소 커버리지 80%·전체 목표 85%를 충족 **shall**하며, LSP 에러·타입에러·린트에러를 0건으로 유지 **shall**한다.

> **개발 방법론(TDD RED-GREEN-REFACTOR)은 요구사항이 아니라 프로세스 제약이다.** "테스트를 먼저 작성했는가"는 산출물 관찰로 검증할 수 없으므로 요구사항으로 두면 추적성 매트릭스가 AC가 제공하지 않는 커버리지를 보고하게 된다. 이 지시는 plan.md §D 제약 조건 표로 이동했다.

---

## §C 시스템 불변식 (Invariants)

| ID | 불변식 |
|---|---|
| INV-AUTH-001 | 저장소에 평문 비밀번호 또는 복호화 가능한 비밀번호가 존재하지 않는다. |
| INV-AUTH-002 | 회원가입 API를 통해 생성된 회원의 역할은 예외 없이 `MEMBER`다. |
| INV-AUTH-003 | 서명이 무효하거나 만료된 토큰으로는 어떤 보호 엔드포인트도 통과할 수 없다. |
| INV-AUTH-004 | 동일 이메일을 가진 회원 레코드는 최대 1건이다. |

---

## §D 범위 제외 (Exclusions)

아래 항목은 이 SPEC의 범위에 포함되지 않는다. 구현 중 "있으면 좋을 것 같아서" 추가하는 것을 명시적으로 금지한다.

### Out of Scope — 토큰 수명 관리 고도화

- 리프레시 토큰 발급 및 회전(rotation)
- 토큰 폐기 목록(denylist / blacklist) 및 그 저장소
- 서버 측 강제 로그아웃

  **수용한 제약(known limitation)**: 위 3개를 제외한 결과, v1의 로그아웃은 **클라이언트가 토큰을 폐기하는 것**으로만 이루어진다. 탈취된 토큰은 `exp`(30분) 도래 전까지 유효하다. 이는 1인 규모·단일 인스턴스 프로젝트의 규모에 맞춘 **의도적 트레이드오프**이며, 완화 장치는 짧은 토큰 수명 하나뿐이다. 강제 무효화가 필요해지면 별도 SPEC으로 폐기 목록을 도입한다.

### Out of Scope — 계정 관리 부가 기능

- 비밀번호 재설정 / 찾기
- 이메일 인증(verification) 메일 발송
- 소셜 로그인(OAuth2 provider 연동)
- 로그인 실패 횟수 기반 계정 잠금

### Out of Scope — 권한 모델 고도화

- 별도 권한(permission) 테이블 및 역할-권한 매핑
- 3종 이상의 역할
- 리소스 단위 ACL

  v1의 역할은 `MEMBER` / `ADMIN` 2종뿐이며 `member.role` 컬럼 하나로 표현한다.

### Out of Scope — 프론트엔드 구현

- React 애플리케이션 스캐폴딩, 로그인 화면, 토큰 저장 전략 구현

  이 SPEC은 백엔드 API 계약까지를 범위로 한다. 프론트엔드는 후속 `SPEC-FRONTEND-001`(**아직 생성하지 않음**)에서 다룬다. 이 SPEC의 인수 기준은 React 없이 100% 검증된다.

### Out of Scope — 다른 SPEC이 소유한 영역

- 강좌 엔티티·카탈로그·관리자 강좌 CRUD → `SPEC-COURSE-001`
- 수강신청 큐·워커·대기명단 → `SPEC-ENROLLMENT-001`

---

## §E 성공 기준

1. 회원가입 후 저장된 비밀번호가 평문과 다르고 BCrypt 형식을 만족한다 (INV-AUTH-001).
2. 로그인 성공 시 §A.3 규격의 토큰이 발급되고, 그 토큰으로 보호 엔드포인트에 접근할 수 있다.
3. 위조 토큰·만료 토큰·토큰 없음 세 경우 모두 401이 반환된다 (INV-AUTH-003).
4. `MEMBER` 역할로는 관리자 엔드포인트에 접근할 수 없다(403).
5. 회원가입 요청에 역할을 주입해도 `ADMIN`이 생성되지 않는다 (INV-AUTH-002).
6. 전체 테스트 커버리지 85% 이상, LSP 에러·타입에러·린트에러 0건.

---

## §F 참조

- 제품 정의: `.moai/project/product.md`
- 기술 스택: `.moai/project/tech.md`
- 구현 계획: `.moai/specs/SPEC-AUTH-001/plan.md`
- 인수 기준: `.moai/specs/SPEC-AUTH-001/acceptance.md`
- 후속 SPEC: `SPEC-COURSE-001` → `SPEC-ENROLLMENT-001`
