---
id: SPEC-AUTH-001
title: "회원 가입·로그인 및 JWT 인증 기반 — 진행 기록"
version: "0.1.1"
status: draft
created: 2026-08-15
updated: 2026-08-15
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/member"
lifecycle: spec-anchored
tags: "auth, progress"
tier: M
---

# SPEC-AUTH-001 — 진행 기록

## §E.1 Plan-phase Audit-Ready Signal

- `plan_status`: audit-ready
- 산출물: `spec.md`, `plan.md`, `acceptance.md`, `progress.md` (Tier M 필수 3종 + 진행 기록) — status: draft
- Tier 판정: **M** (plan.md §F 기준 예상 13~15 프로덕션 파일 + 6~8 테스트 파일)
- 요구사항 24건 + 불변식 4건, 인수 기준 20건, 미대응 0건 (acceptance.md §D.2)
- 미해소 클래리피케이션 마커: **0건** (인증 전략 JWT / 프론트엔드 백엔드 우선 / SPEC 3분할 모두 사용자 결정으로 확정)
- 선행 의존: 없음. 이 SPEC이 3분할 실행 순서의 1번이다
- 다음 단계: plan-auditor 감사 → Implementation Kickoff Approval

## §F Phase 4 Mode Selection

**Input parameters**:
- tier: M
- scope (file count): ~13-15 production files, 6-8 test files (per plan.md Tier 판정)
- domain count: 1 (single Java/Spring Boot backend domain)
- file language mix: 100% Java (+ Gradle build config)
- concurrency benefit: LOW (coding-heavy implementation — Anthropic coding-task parallelism caveat)
- Agent Teams prereqs: N/A (Mode 3 retired)

**Mode evaluation table**:

| Mode | Selected? | Rationale |
|------|-----------|-----------|
| 1 trivial | No | Non-trivial multi-file TDD implementation |
| 2 background | No | Write-capable milestone work, needs foreground sequencing |
| 3 agent-team | No | RETIRED |
| 4 parallel | No | Single-domain coding-heavy work, not research-heavy multi-domain |
| 5 sub-agent | **Yes** | Default fallback; coding-heavy work per Anthropic's coding-task parallelism caveat |
| 6 workflow | No | Not ≥30 files / not a uniform mechanical transform |

**Decision**: sub-agent

**Justification**: SPEC-AUTH-001 is a single-domain (Java/Spring Boot) TDD implementation task with sequential milestone dependencies (M1 회원 도메인 → M2 토큰 발급 → M3 필터체인/인가 → M4 시더/비기능). Per Anthropic's coding-task parallelism caveat ("most coding tasks involve fewer truly parallelizable tasks than research"), Mode 5 (sequential sub-agent, one milestone at a time) is the correct default. User selected semi-autonomous progression (checkpoint confirmation after each milestone) at Implementation Kickoff Approval.

**Implementation Kickoff Approval confirmation**: obtained via AskUserQuestion (semi-autonomous progression mode selected — checkpoint per milestone, no `/goal ac_converge` autonomy set).

## §E.2 Run-phase Evidence

### M1 — 회원 도메인 (완료)

**환경 사전 조치**:
- 로컬에 Gradle toolchain이 요구하는 Java 17이 없어(`java_home`에 26.0.1만 존재) `brew install openjdk@17` 설치 + `gradle.properties`에 `org.gradle.java.installations.paths` 추가로 해결.
- `spring-boot-starter-security`를 추가하면 Spring Security 기본 자동설정(모든 엔드포인트 인증 요구 + CSRF)이 걸려 아직 SecurityConfig가 없는 M1의 공개 signup API가 403으로 막히는 문제 발생 → `application.properties`에 `spring.autoconfigure.exclude`로 `SecurityAutoConfiguration`/`UserDetailsServiceAutoConfiguration`/`ServletWebSecurityAutoConfiguration`을 임시 제외(주석으로 M1 한정·M3에서 제거 필요 명시). SecurityConfig 클래스 자체는 생성하지 않았다(M3 범위 보존).
- Spring Boot 4.1.0의 모듈 재구성으로 `@DataJpaTest`/`@AutoConfigureTestDatabase`/`@AutoConfigureMockMvc`가 레거시 경로(`org.springframework.boot.test.autoconfigure.orm.jpa` 등)가 아닌 신규 모듈별 경로(`org.springframework.boot.data.jpa.test.autoconfigure.*`, `org.springframework.boot.jdbc.test.autoconfigure.*`, `org.springframework.boot.webmvc.test.autoconfigure.*`)에 위치함을 확인 후 사용.
- `SignupRequest`는 record 컴팩트 생성자에서 이메일 앞뒤 공백만 트림(소문자 변환은 하지 않음) — Bean Validation의 `@Email`이 정규화(Member.normalizeEmail) 이전의 원본 문자열을 검사하기 때문에, 공백 포함 이메일(AC-AUTH-006)이 형식 검증에서 거부되는 순서 문제를 해소하기 위함. 완전한 정규화(trim+lowercase)는 여전히 `Member.normalizeEmail()` 한 곳에서만 수행한다(plan.md §C.1).

**신규 산출물**:
- `src/main/java/com/hongseob/openclass_ap/member/{Member,MemberRole,MemberRepository,MemberService,AuthController}.java`
- `src/main/java/com/hongseob/openclass_ap/member/dto/{SignupRequest,SignupResponse}.java`
- `src/main/java/com/hongseob/openclass_ap/common/config/PasswordEncoderConfig.java`
- `src/main/java/com/hongseob/openclass_ap/common/exception/{DuplicateEmailException,GlobalExceptionHandler}.java`
- `src/main/java/com/hongseob/openclass_ap/common/response/ErrorResponse.java`
- `src/test/java/com/hongseob/openclass_ap/support/AbstractIntegrationTest.java` (Testcontainers PostgreSQL 공통 베이스)
- `src/test/java/com/hongseob/openclass_ap/member/{MemberTest,MemberRepositoryTest,MemberServiceTest,SignupIntegrationTest}.java`
- `gradle.properties` (JDK 17 toolchain 경로), `build.gradle` (security/jjwt/testcontainers/jacoco 의존성 추가)

**AC PASS/FAIL 매트릭스** (M1 대응분 AC-AUTH-001~006):

| AC | Status | Verification Command | Actual Output |
|----|--------|----------------------|----------------|
| AC-AUTH-001 (회원가입 성공) | PASS | `./gradlew test --tests SignupIntegrationTest.회원가입_성공시_201과_MEMBER_역할의_회원이_생성된다` | 201, role=MEMBER, password_hash != 평문, BCrypt(`^\$2[aby]\$`) 매칭 확인 |
| AC-AUTH-002 (중복 이메일 거부) | PASS | `SignupIntegrationTest.중복_이메일_가입은_409를_반환하고_행이_증가하지_않는다` + `MemberRepositoryTest.동일_이메일을_직접_저장하면_DB_유니크_제약_위반_예외가_발생한다` | HTTP 계층 409 확인 + 리포지토리 직접 호출 시 `DataIntegrityViolationException` 확인(실제 PostgreSQL 유니크 제약) |
| AC-AUTH-003 (입력 검증 실패) | PASS | `SignupIntegrationTest.잘못된_이메일_형식이면_400을_반환...` / `...비밀번호가_8자_미만이면_400을...` | 두 케이스 모두 400, 회원 행 0건 유지 확인 |
| AC-AUTH-004 (역할 주입 차단) | PASS | `SignupIntegrationTest.요청_본문에_role_ADMIN을_포함해도_MEMBER로만_생성된다` | 201 + role=MEMBER, DB 전체 ADMIN 0건 확인 |
| AC-AUTH-005 (저장소 평문 부재) | PASS | `SignupIntegrationTest.평문_비밀번호는_어떤_컬럼에도_저장되지_않는다` | 3명 가입 후 email/name/password_hash 컬럼 전체에서 평문 미발견 확인 |
| AC-AUTH-006 (이메일 정규화) | **PASS-WITH-DEBT** | `SignupIntegrationTest.대소문자와_공백이_포함된_이메일은_트림_소문자로_정규화되어_저장된다` | 트림+소문자 정규화 저장 확인 + 정규화된 이메일 재가입 시 409(행 수 1건 유지) 확인. **단, AC 원문의 "a@example.com으로 로그인이 성공한다" 절은 검증하지 않음** — 로그인 API는 REQ-LOGIN-001(M2 범위)이 아직 미구현이라 호출 대상이 없음. M2에서 로그인 API 구현 후 이 AC를 재검증하여 PASS로 승격 예정. |

**커버리지 (member + common 패키지, jacoco)**:
- `com/hongseob/openclass_ap/member`: INSTRUCTION 100.0% (111/111), LINE 100.0% (27/27)
- `com/hongseob/openclass_ap/member/dto`: INSTRUCTION 95.2% (40/42) — SignupRequest 컴팩트 생성자의 null 이메일 분기 1건 미검증(모든 테스트가 email 필드를 채워 보내므로)
- `com/hongseob/openclass_ap/common/{config,exception,response}`: 각 100.0%
- 전체 프로젝트 합계: INSTRUCTION 96.5% (192/199) — REQ-NFR-004의 커밋당 80%·전체 목표 85% 기준을 상회

**빌드/린트**:
- `./gradlew build` — BUILD SUCCESSFUL
- 프로젝트에 checkstyle/spotbugs 등 별도 lint 플러그인이 구성되어 있지 않아 "lint 에러 0건"은 공허하게 충족됨(도구 부재) — M1 범위에서 새 lint 플러그인 도입은 하지 않음(plan.md 의존성 목록 밖의 스코프 확장이므로)
- Subagent-boundary grep: `grep -rn 'AskUserQuestion' src/` → 0건

**M3 인수인계 필수 항목**:
- `application.properties`의 `spring.autoconfigure.exclude` 줄은 M1 한정 임시 조치다. M3에서 `SecurityConfig`(`SecurityFilterChain` Bean)를 작성하면 이 줄을 제거해야 한다.
- `PasswordEncoderConfig`의 `passwordEncoder()` Bean은 M3의 SecurityConfig가 재사용해야 하며, 중복 Bean 정의를 만들지 않는다.

## §E.3 Run-phase Audit-Ready Signal

_<pending run-phase>_

## §E.4 Sync-phase Audit-Ready Signal

_<pending sync-phase>_
