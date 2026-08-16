---
id: SPEC-AUTH-001
title: "회원 가입·로그인 및 JWT 인증 기반 — 구현 계획"
version: "0.1.1"
status: completed
created: 2026-08-15
updated: 2026-08-16
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/member"
lifecycle: spec-anchored
tags: "auth, jwt, member, plan"
tier: M
---

# SPEC-AUTH-001 — 구현 계획 (plan)

> **읽는 순서 안내**: §C는 **되돌리기 어려운 순서**로 배치했다. 위쪽 항목일수록 나중에 바꾸는 비용이 크다. 리뷰는 §C.1 → §C.2 → §C.3 순으로 집중하는 것이 효율적이다.

---

## §A 배경 (Context)

- 그린필드 프로젝트다. `src/main/java/com/hongseob/openclass_ap/`에는 부트스트랩 클래스 `OpenclassApApplication.java`만 존재한다.
- 이 SPEC은 **3분할된 SPEC 중 첫 번째**다. 실행 순서는 `SPEC-AUTH-001` → `SPEC-COURSE-001` → `SPEC-ENROLLMENT-001`이며, 뒤의 두 SPEC은 여기서 확정하는 인증 필터 체인·역할 모델·패키지 구조를 전제로 한다.
- 개발 방법론은 TDD로 고정되어 있다 (`quality.yaml` → `constitution.development_mode: "tdd"`).
- **선행 의존: 없음.**

### A.1 확정된 사용자 결정 사항

| 결정 항목 | 확정 값 | 근거 |
|---|---|---|
| 인증 방식 | **JWT 액세스 토큰** | 사용자 결정 (세션 대신 JWT 채택) |
| 토큰 수명 관리 | 단일 단기 토큰, 리프레시·폐기 목록 없음 | 프로젝트 규모에 맞춘 최소 설계. 제약은 spec.md §D에 명시 기록 |
| 프론트엔드 | **백엔드 우선.** 이 SPEC 범위에 React 없음 | 사용자 결정. 모든 AC는 Spring Boot 테스트만으로 검증 |

이 SPEC에는 미해소 클래리피케이션 마커가 없다.

---

## §B 범위 (Scope)

| 포함 | 제외 (다른 SPEC 소유) |
|---|---|
| `Member` 엔티티 + `role` | 강좌 엔티티 → `SPEC-COURSE-001` |
| 회원가입 / 로그인 API | 관리자 강좌 CRUD → `SPEC-COURSE-001` |
| JWT 발급·검증 필터 체인 | 수강신청 큐·워커 → `SPEC-ENROLLMENT-001` |
| 경로 기반 인가 규칙 (`/api/admin/**`) | 프론트엔드 화면 → `SPEC-FRONTEND-001` (**아직 생성하지 않음**) |
| 최초 관리자 시더 | |
| 공통 예외 응답 골격 | |

---

## §C 기술 설계 (되돌리기 어려운 순)

### C.1 데이터 모델 — 최우선

가장 되돌리기 어려운 결정이다. 스키마 확정 후에는 마이그레이션 비용이 발생한다.

| 테이블 | 컬럼 | 제약 |
|---|---|---|
| `member` | `id` (BIGSERIAL), `email`, `password_hash`, `name`, `role`, `created_at` | `email` UNIQUE (정규화된 소문자 값 기준), `role` NOT NULL DEFAULT `'MEMBER'` |

- `role`은 문자열 컬럼 + Java `enum MemberRole { MEMBER, ADMIN }`으로 매핑한다. 별도 권한 테이블을 만들지 않는다 (역할 2종, 세분화 요구 없음 — spec.md §D).
- 이메일 정규화(REQ-SIGNUP-007)는 **엔티티 진입 지점 1개소**(`Member` 생성 팩토리 또는 `@PrePersist`)에서 수행한다. 컨트롤러·서비스 여러 곳에서 각각 `toLowerCase()`를 호출하는 방식은 누락 경로를 만들므로 금지한다.

### C.2 인증 전략 — JWT (확정)

사용자 결정에 따라 JWT를 채택한다. 아래는 **의도적으로 단순하게 유지한** 설계이며, 각 "도입하지 않음" 항목은 spec.md §D에 제약으로 기록되어 있다.

| 구성 요소 | v1 설계 | 도입하지 않는 것 |
|---|---|---|
| 토큰 종류 | 액세스 토큰 1종 | 리프레시 토큰, 토큰 회전 |
| 서명 | HMAC-SHA256, 대칭키 | 비대칭키(RS256), 키 회전(JWKS) |
| 수명 | 30분 (`app.jwt.access-token-ttl` 프로퍼티) | 슬라이딩 만료 |
| 무효화 | 없음 — 만료까지 유효 | 폐기 목록(denylist), 서버 측 강제 로그아웃 |
| 전달 | `Authorization: Bearer` 헤더 | 쿠키 저장 (→ CSRF 대응 불필요) |

**수용한 트레이드오프 (명시)**: 토큰 폐기 목록이 없으므로 **로그아웃은 클라이언트가 토큰을 버리는 것으로만 이루어지고, 탈취된 토큰은 최대 30분간 유효하다.** 이를 완화하는 장치는 짧은 수명 하나뿐이다. 이 제약은 v1 규모에서 수용 가능하다고 판단했으며, 숨겨진 결함이 아니라 **문서화된 알려진 제약**이다 (spec.md §D + acceptance.md §D.3 체크리스트 항목). 강제 무효화가 필요해지는 시점(다중 사용자 운영, 보안 사고 대응 요구)에 별도 SPEC으로 폐기 목록을 도입한다.

**JWT 선택이 가져오는 이점 (기록)**: 무상태 검증이므로 이후 애플리케이션 인스턴스를 늘려도 세션 저장소가 필요 없다. 단, `SPEC-ENROLLMENT-001`의 큐 워커는 순서 보장을 위해 **여전히 단일 인스턴스여야 한다** — JWT는 워커의 단일 인스턴스 제약을 해소하지 않는다.

추가 의존성: `spring-boot-starter-security`, `spring-boot-starter-security-test`, JWT 라이브러리(`io.jsonwebtoken:jjwt` 계열 또는 동등물 — 구체 아티팩트는 run 단계에서 최신 안정 버전 확인 후 확정).

### C.3 인가 모델

- 경로 매칭 기반: `/api/admin/**` → `hasRole("ADMIN")`, `/api/auth/**`·`/api/courses`(GET) → `permitAll`, 그 외 → `authenticated`.
- 세션 정책은 `SessionCreationPolicy.STATELESS` (REQ-AUTHZ-005 / AC-AUTH-014의 근거).
- CSRF는 비활성화한다 — 쿠키 기반 자격 증명을 사용하지 않으므로 CSRF 공격면이 존재하지 않는다. 이는 "보안 기능을 껐다"가 아니라 **위협 모델에 해당하지 않는 방어를 제거한 것**이며, 근거를 여기에 기록한다.
- 401(미인증)과 403(인가 실패)을 구분해 반환한다 — AC-AUTH-012가 이 구분을 기계적으로 검증한다.

### C.4 최초 관리자 시더

- `ApplicationRunner`로 기동 시 1회 실행. 설정: `app.admin.email`, `app.admin.password`.
- 존재하면 아무것도 하지 않는다(멱등) — AC-AUTH-017이 `password_hash` 불변까지 검증한다.
- 테스트에서는 프로퍼티 주입으로 시더 동작을 제어한다.

### C.5 패키지 구조 (프로젝트 전체 골격 — 후속 SPEC이 상속)

```
com.hongseob.openclass_ap
├── OpenclassApApplication.java
├── common/
│   ├── config/          # SecurityConfig, JwtProperties
│   ├── exception/       # 도메인 예외 + @RestControllerAdvice
│   └── response/        # 공통 응답/에러 바디
└── member/
    ├── Member.java, MemberRole.java, MemberRepository.java
    ├── MemberService.java, AuthController.java
    ├── jwt/             # JwtTokenProvider, JwtAuthenticationFilter
    ├── seed/            # AdminSeeder
    └── dto/
```

테스트는 `src/test/java/com/hongseob/openclass_ap/` 아래 동일 구조를 미러링한다.

> 이 골격은 `SPEC-COURSE-001`(`course/`)과 `SPEC-ENROLLMENT-001`(`enrollment/`, `waitlist/`)이 그대로 확장한다. 여기서 정한 `common/` 구성은 이후 SPEC에서 재설계하지 않는다.

#### C.5.1 테스트 전용 엔드포인트 픽스처 (필수 산출물 — M3)

이 SPEC이 노출하는 **프로덕션 엔드포인트는 회원가입·로그인 2개뿐이고 둘 다 공개**다. 즉 이 SPEC에는 **보호 엔드포인트도 관리자 엔드포인트도 존재하지 않는다.** 그런데 AC-AUTH-010 / 011 / 012 / 013 / 014 다섯 건은 보호·관리자 엔드포인트 호출을 전제한다. `/api/admin/**`에 실제 컨트롤러가 처음 생기는 것은 `SPEC-COURSE-001`이므로, 이 SPEC 안에서 그 다섯 AC를 실행하려면 **테스트 소스 트리에만 존재하는 픽스처 컨트롤러**가 필요하다. 이를 산출물로 명시 선언한다.

| 항목 | 값 |
|---|---|
| 위치 | `src/test/java/com/hongseob/openclass_ap/member/fixture/AuthTestFixtureController.java` — **테스트 소스 트리 전용** |
| 노출 경로 1 | `GET /api/test/protected` — 보호 엔드포인트 (인증만 필요). AC-AUTH-010 / 011 / 014의 호출 대상 |
| 노출 경로 2 | `GET /api/admin/test-ping` — 관리자 엔드포인트 (`/api/admin/**` 패턴에 매칭). AC-AUTH-012 / 013의 호출 대상 |
| 활성화 방식 | `@TestConfiguration`으로 등록하여 해당 테스트 클래스에서만 컨텍스트에 포함 |
| 응답 | 200 + 고정 본문. 도메인 로직·DB 접근을 포함하지 않는다 |

**프로덕션 반입 금지 (HARD)**: 이 픽스처는 `src/main` 트리에 존재해서는 안 된다. 프로덕션 반입 시 인증 우회 검증용 경로가 실제 서비스 표면이 되므로, 배포 산출물에 `/api/test/**` 경로가 없음을 M3 완료 시 확인한다 (§G 안티패턴 참조). 픽스처가 `SecurityConfig`의 인가 규칙을 수정하는 것도 금지한다 — 픽스처는 경로를 **제공만** 하고, 401/403 판정은 전적으로 프로덕션 `SecurityConfig`가 수행해야 AC가 의미를 갖는다.

### C.6 프론트엔드 — 이 SPEC 범위 밖

- 사용자 결정에 따라 **백엔드 우선**으로 진행한다. 이 SPEC의 run 단계에서 React 스캐폴딩을 생성하지 않는다.
- 이 SPEC의 모든 인수 기준은 Spring Boot 테스트(통합/슬라이스)만으로 검증 가능하도록 작성되어 있다 (AC-AUTH-019가 이를 기계적으로 확인한다).
- 후속 `SPEC-FRONTEND-001`(**아직 생성하지 않음 — 향후 계획**)이 이 SPEC이 노출하는 회원가입·로그인 API를 소비할 예정이다.
- CORS 설정은 이 SPEC에 **포함하지 않는다**. 프론트엔드 착수 시점에 실제 오리진이 정해진 뒤 추가하는 것이 추측 설정보다 안전하다.

---

## §D 제약 조건

| 항목 | 값 | 출처 |
|---|---|---|
| 언어/런타임 | Java 17 (Gradle toolchain 고정) | `build.gradle` |
| 프레임워크 | Spring Boot 4.1.0 | `build.gradle` |
| DB | PostgreSQL | `tech.md` |
| 통합 테스트 DB | **Testcontainers(PostgreSQL)** | 유니크 제약 동작을 실제 DB에서 검증해야 함 (AC-AUTH-002) |
| 개발 방법론 | **TDD(RED-GREEN-REFACTOR) 준수** — 프로세스 제약이며 요구사항이 아니다. 산출물 관찰로 검증할 수 없으므로 AC를 부여하지 않는다 (spec.md REQ-NFR-004 주석 참조) | `quality.yaml` |
| 커버리지 | 커밋당 ≥ 80%, 전체 목표 85% — 이쪽은 관찰 가능하므로 REQ-NFR-004 + AC-AUTH-020이 검증한다 | `quality.yaml` |
| 문서 언어 | 한국어 | `language.yaml` |
| 테스트 픽스처 | 보호·관리자 경로 픽스처는 **테스트 소스 트리 전용** (§C.5.1) | 2차 감사 A1 |
| 프론트엔드 | 이 SPEC 범위 밖 | 사용자 결정 |

---

## §E 사전 점검 (Pre-flight)

run 단계 진입 전 확인:

1. PostgreSQL 인스턴스가 기동 가능하고 접속 정보가 설정되어 있다.
2. 추가 의존성이 승인되어 있다: `spring-boot-starter-security`, `spring-boot-starter-security-test`, JWT 라이브러리, `org.testcontainers:postgresql`, `spring-boot-testcontainers`.
3. 테스트용 JWT 서명 비밀키·관리자 시드 계정 프로퍼티가 테스트 프로파일에 정의되어 있다.
4. **M3 진입 전**: §C.5.1의 테스트 전용 픽스처 컨트롤러 산출물이 M3 작업 목록에 포함되어 있다. AC-AUTH-010 ~ 014는 이 픽스처 없이는 실행 불가다.
5. 미해소 클래리피케이션 마커가 0건이다. — **충족** (§A.1에서 3건 모두 확정)

---

## §F 마일스톤

시간 추정 없이 우선순위·순서로만 기술한다.

### M1 — 회원 도메인 (우선순위: 높음)

- `Member` 엔티티 + `MemberRole` + 유니크 제약 + 이메일 정규화
- 회원가입 API + 검증 + BCrypt 해시
- 대응 요구사항: REQ-SIGNUP-001 ~ REQ-SIGNUP-007, REQ-NFR-001
- 대응 AC: AC-AUTH-001 ~ AC-AUTH-006

### M2 — 토큰 발급 (우선순위: 높음)

- `JwtTokenProvider` (서명·클레임·만료), 로그인 API
- 실패 응답 동일화 (AC-AUTH-008이 바이트 단위 동일성을 검증)
- 대응 요구사항: REQ-LOGIN-001 ~ REQ-LOGIN-004
- 대응 AC: AC-AUTH-007 ~ AC-AUTH-009

### M3 — 필터 체인 및 인가 (우선순위: 높음)

- `JwtAuthenticationFilter` + `SecurityConfig` (STATELESS, 경로 인가, 401/403 구분)
- **테스트 전용 엔드포인트 픽스처** (§C.5.1): `AuthTestFixtureController` — `GET /api/test/protected` + `GET /api/admin/test-ping`. **테스트 소스 트리에만 생성하며 프로덕션에 반입하지 않는다.**
- 대응 요구사항: REQ-AUTHZ-001 ~ REQ-AUTHZ-006
- 대응 AC: AC-AUTH-010 ~ AC-AUTH-015
- **M3 완료 조건**: 픽스처가 `src/test` 아래에만 존재하고 `src/main` 트리에서 `/api/test/` 문자열이 0건임을 확인한다. 이 확인 없이는 AC-AUTH-010 ~ 014가 실행 자체를 못 하거나, 실행되더라도 프로덕션 표면을 넓힌 상태가 된다.

### M4 — 시더 및 비기능 마감 (우선순위: 중)

- `AdminSeeder` (멱등), 로그 정합성, 커버리지 도달
- 대응 요구사항: REQ-SEED-001 ~ REQ-SEED-003, REQ-NFR-002 ~ REQ-NFR-004
- 대응 AC: AC-AUTH-016 ~ AC-AUTH-020

---

## §G 안티패턴 (구현 중 금지)

| 안티패턴 | 왜 금지인가 |
|---|---|
| 서명 비밀키를 소스에 상수로 포함 | REQ-LOGIN-004 위반. AC-AUTH-009가 정적 검색으로 검출한다 |
| 로그인 실패 사유를 응답으로 구분 노출 | REQ-LOGIN-002 위반. 계정 열거(enumeration) 취약점 |
| 회원가입 요청 본문의 `role`을 신뢰 | REQ-SIGNUP-005 / INV-AUTH-002 위반. 권한 상승 경로가 열린다 |
| 요청 처리 중 `HttpSession`에 인증 상태 저장 | REQ-AUTHZ-005 위반. 무상태 전제가 깨지고 AC-AUTH-014가 실패한다 |
| 토큰 폐기 목록·리프레시 토큰을 "있으면 좋으니까" 추가 | spec.md §D 범위 제외. 추가하려면 SPEC 개정이 선행되어야 한다 |
| 이메일 정규화를 여러 호출 지점에 분산 | 누락 경로가 생겨 REQ-SIGNUP-007이 국소적으로 깨진다. 진입 지점 1개소로 수렴시킨다 |
| 통합 테스트를 H2로 대체 | 유니크 제약·문자열 정렬 동작이 PostgreSQL과 달라 AC-AUTH-002 검증이 약화된다 |
| React 스캐폴딩 생성 | 사용자 결정(백엔드 우선) 위반. spec.md §D 범위 제외 |
| §C.5.1 픽스처 컨트롤러를 `src/main`에 생성 | 인증 검증용 임시 경로가 실제 서비스 표면이 된다. 픽스처는 테스트 소스 트리 전용이며, M3 완료 시 `src/main`에 `/api/test/`가 0건임을 확인한다 |
| 픽스처가 `SecurityConfig` 인가 규칙을 자체적으로 완화 | 픽스처는 경로를 제공만 한다. 인가 판정을 픽스처가 만지는 순간 AC-AUTH-010 ~ 014는 프로덕션 설정이 아니라 테스트 설정을 검증하게 되어 무의미해진다 |

---

## §H 교차 참조

- 요구사항 정의: `.moai/specs/SPEC-AUTH-001/spec.md`
- 인수 기준: `.moai/specs/SPEC-AUTH-001/acceptance.md`
- 진행 기록: `.moai/specs/SPEC-AUTH-001/progress.md`
- 후속 SPEC: `.moai/specs/SPEC-COURSE-001/`, `.moai/specs/SPEC-ENROLLMENT-001/`
- 제품/구조/기술: `.moai/project/{product,structure,tech}.md`
- 품질 설정: `.moai/config/sections/quality.yaml`
