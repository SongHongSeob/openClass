---
id: SPEC-AUTH-001
title: "회원 가입·로그인 및 JWT 인증 기반 — 진행 기록"
version: "0.1.2"
status: completed
created: 2026-08-15
updated: 2026-08-30
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/member"
lifecycle: spec-anchored
tags: "auth, progress"
tier: M
amendment_of: SPEC-AUTH-001
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

### M2 — 토큰 발급 (완료)

**신규 산출물**:
- `src/main/java/com/hongseob/openclass_ap/member/jwt/{JwtTokenProvider,...}.java` — HMAC-SHA256 서명, `sub`/`role`/`iat`/`exp` 클레임
- `src/main/java/com/hongseob/openclass_ap/common/config/JwtProperties.java` — `app.jwt.secret`/`app.jwt.access-token-ttl` 프로퍼티 바인딩 (소스에 비밀키 리터럴 없음, AC-AUTH-009 정적 검색으로 확인)
- `src/main/java/com/hongseob/openclass_ap/common/exception/InvalidCredentialsException.java` + `GlobalExceptionHandler` 확장 — 로그인 실패를 원인 구분 없이 동일 401 응답으로 처리
- `src/main/java/com/hongseob/openclass_ap/member/dto/{LoginRequest,LoginResponse}.java`, `AuthController`에 `POST /api/auth/login` 추가 (M3 이전이라 여전히 공개 엔드포인트, `SecurityConfig` 없음)
- `src/test/java/com/hongseob/openclass_ap/member/LoginIntegrationTest.java` (Testcontainers 통합), `src/test/java/com/hongseob/openclass_ap/member/jwt/JwtTokenProviderTest.java` (순수 단위 테스트, 컨테이너 불필요)

**AC PASS/FAIL 매트릭스** (M2 대응분 + M1 승격분):

| AC | Status | Verification | Evidence |
|----|--------|--------------|----------|
| AC-AUTH-007 (로그인 성공/클레임) | PASS | `LoginIntegrationTest.로그인_성공시_200과_함께_클레임이_모두_담긴_토큰이_반환된다` | 200 + `sub`/`role`/`iat`/`exp` 클레임 전부 존재, `role=MEMBER`, 페이로드에 비밀번호 평문·해시 미포함 확인 |
| AC-AUTH-008 (로그인 실패 구별 불가) | PASS | `LoginIntegrationTest.틀린_비밀번호와_미가입_이메일의_로그인_실패_응답은_바이트_단위로_동일하다` | 두 실패 케이스 모두 401 + 응답 본문 바이트 단위 동일 확인 |
| AC-AUTH-009 (서명 비밀키 외부 주입) | PASS | `JwtTokenProviderTest.다른_비밀키로_생성된_토큰은_검증에_실패한다` + `grep -rn` 소스 검색 | 다른 비밀키로 검증 시 실패 확인 + `src/main/java` 전체에서 비밀키 리터럴 미검출(프로퍼티 참조만 존재) |
| AC-AUTH-006 (이메일 정규화 — M1 PASS-WITH-DEBT → 승격) | **PASS** | `LoginIntegrationTest.대소문자와_공백이_포함된_이메일로_가입해도_정규화된_이메일로_로그인_성공한다` | 로그인 API 구현 완료로 남은 절("정규화된 이메일로 로그인 성공") 검증 완료. M1의 부분 통과 상태 해소 |

**빌드/테스트 검증 (오케스트레이터 직접 재확인 — develop-auth-m2 에이전트가 결과 보고 없이 중단되어 직접 검증함)**:
- `./gradlew build` 격리 실행(`--tests "*LoginIntegrationTest"` 단독): **BUILD SUCCESSFUL, 5초**, 신규 테스트 전부 통과
- 전체 스위트(`./gradlew test`, 29개 테스트)를 5회 반복 실행한 결과 매번 3~7건이 간헐적으로 실패 — **원인은 코드가 아니라 이 개발 환경의 Docker 자원 경합**으로 판단함(근거: (1) 실패하는 테스트 클래스가 매회 달라짐, (2) 실패 스택트레이스가 매번 동일하게 `HikariPool ... Connection refused/timed out after 30s` — DB 연결 자체의 문제이지 애플리케이션 로직 assertion 실패가 아님, (3) 격리 실행 시 100% 통과, (4) Docker Desktop에 7.75GiB만 할당되어 있고 상시 구동 중인 다른 컨테이너(n8n)와 자원을 공유함)
- **잔여 위험(Residual risk)**: 이 환경에서 전체 테스트 스위트를 한 번에 돌리면 재시도가 필요할 수 있음. 코드 정확성 자체는 격리 실행으로 검증되었으므로 커밋을 막지 않기로 판단함. `SPEC-ENROLLMENT-001`도 Testcontainers를 요구하므로 동일한 환경 이슈가 재발할 수 있음 — 후속 SPEC 진입 시 참고할 것.

### M3 — 필터 체인 및 인가 (완료)

**신규 산출물**:
- `src/main/java/com/hongseob/openclass_ap/common/config/SecurityConfig.java` — `SecurityFilterChain` Bean. STATELESS, CSRF 비활성화(근거 주석 포함), 경로 인가(`/api/auth/**`+`GET /api/courses` permitAll, `/api/admin/**` hasRole(ADMIN), 그 외 authenticated), 401/403을 명시적 `AuthenticationEntryPoint`/`AccessDeniedHandler`로 구분
- `src/main/java/com/hongseob/openclass_ap/member/jwt/JwtAuthenticationFilter.java` — `Authorization: Bearer` 헤더 검증, `HttpSession` 미사용
- `src/test/java/com/hongseob/openclass_ap/member/fixture/AuthTestFixtureController.java` — `@TestConfiguration` + `@RestController`(컴포넌트 스캔·수동 등록 충돌 회피를 위해 같은 클래스에 결합), `GET /api/test/protected` + `GET /api/admin/test-ping`, `SecurityConfig` 미수정
- `src/test/java/com/hongseob/openclass_ap/member/AuthorizationIntegrationTest.java`(6 테스트), `src/test/java/com/hongseob/openclass_ap/member/jwt/JwtAuthenticationFilterTest.java`(3 테스트)
- `application.properties`의 M1 임시 `spring.autoconfigure.exclude` 줄 제거 완료(주석으로 제거 사실 기록)

**AC PASS/FAIL 매트릭스** (M3 대응분 AC-AUTH-010~015):

| AC | Status | Verification | Evidence |
|----|--------|--------------|----------|
| AC-AUTH-010 (토큰 없음 차단) | PASS | `AuthorizationIntegrationTest.토큰없이_보호_엔드포인트를_호출하면_401이_반환된다` | 401 확인 |
| AC-AUTH-011 (위조·만료 토큰 차단) | PASS | `AuthorizationIntegrationTest.위조되거나_만료된_토큰이면_401이_반환되고_핸들러에_도달하지_않는다` | 변조 서명 + 만료 토큰 각각 401, 픽스처 핸들러 호출 카운터 0 확인 |
| AC-AUTH-012 (MEMBER→관리자 엔드포인트 403) | PASS | `AuthorizationIntegrationTest.MEMBER_역할_토큰으로_관리자_엔드포인트를_호출하면_403이_반환된다` | 403 확인(401 아님 — 인증은 됐으나 인가 실패) |
| AC-AUTH-013 (ADMIN→관리자 엔드포인트 허용) | PASS | `AuthorizationIntegrationTest.ADMIN_역할_토큰으로_관리자_엔드포인트를_호출하면_200이_반환된다` | 200 + 픽스처 응답 도달 확인 |
| AC-AUTH-014 (무상태 검증) | PASS | `AuthorizationIntegrationTest.동일_토큰으로_20회_호출해도_전부_성공하고_세션_쿠키가_생기지_않는다` | 20회 반복 호출 전부 성공, `Set-Cookie: JSESSIONID` 미포함 확인 |
| AC-AUTH-015 (공개 엔드포인트 무토큰 접근) | PASS | `AuthorizationIntegrationTest.공개_엔드포인트는_토큰없이_호출해도_401이_아니다` | 회원가입/로그인 엔드포인트가 토큰 없이도 401이 아님 확인 |

**M3 완료 조건(HARD 게이트) 검증**: `grep -rn "/api/test/" src/main` → **0건**. 픽스처가 `src/test`에만 존재함을 확인.

**빌드/테스트 검증 (오케스트레이터 직접 재확인 — develop-auth-m3 에이전트 세션이 응답 없이 종료되어 직접 이어받아 검증함)**:
- develop-auth-m3는 코드 작성을 완료했으나(`SecurityConfig`/`JwtAuthenticationFilter`/픽스처/테스트 전부 존재), M2와 동일한 Docker/Testcontainers 자원 경합으로 전체 스위트 재시도를 반복하다 세션이 응답 없이 종료됨. 이미 실행 중이던 백그라운드 `./gradlew test jacocoTestReport` 프로세스는 계속 진행되어 완료됨(로그: `/tmp/gradle-build-attempt3.log`).
- 그 결과(38 테스트 중 7건 실패)를 직접 분석: 실패는 전부 `SignupIntegrationTest`(M1, M3와 무관)에서 발생했고 원인은 M1/M2에서 이미 기록한 것과 동일한 `HikariPool ... Connection refused/timed out` — M3 신규 테스트(`AuthorizationIntegrationTest` 6/6, `JwtAuthenticationFilterTest` 3/3)는 그 실행에서 **전부 통과**(XML 리포트 `failures="0" errors="0"` 직접 확인)
- 코드 리뷰: `SecurityConfig`/`JwtAuthenticationFilter`/픽스처 컨트롤러 3개 파일을 직접 읽고 plan.md §C.3/§C.5.1/§G 제약(STATELESS, CSRF 비활성화 근거, 경로 규칙, 401/403 구분, 픽스처의 SecurityConfig 미수정, 세션 미사용) 전부 충족 확인
- **잔여 위험**: M2와 동일한 환경 이슈(§E.2 M2 절 참조). 코드 정확성은 위 근거로 검증되어 커밋을 막지 않기로 판단함.

### M4 — 시더 및 비기능 마감 (완료)

**신규 산출물**:
- `src/main/java/com/hongseob/openclass_ap/member/seed/AdminSeeder.java` — `ApplicationRunner`로 기동 시 1회 실행되는 최초 관리자 계정 시더. `existsByEmail` 확인 후 없을 때만 생성(멱등)
- `src/main/java/com/hongseob/openclass_ap/common/config/AdminProperties.java` — `app.admin.*` 프로퍼티 바인딩(`JwtProperties`와 동일한 외부화 패턴, 소스에 비밀번호 리터럴 없음)
- `src/main/java/com/hongseob/openclass_ap/member/Member.java`에 `createAdmin` 정적 팩토리 추가 — 회원가입 API(사용자 입력)로는 도달 불가능한 통제된 경로이며 기존 `createMember` 경로는 변경하지 않음
- `src/main/resources/application.properties` / `src/test/resources/application-test.properties`에 `app.admin.email`/`app.admin.password` 추가
- `src/test/java/com/hongseob/openclass_ap/member/seed/AdminSeederTest.java`(2 테스트), `src/test/java/com/hongseob/openclass_ap/member/SensitiveLogIntegrationTest.java`(1 테스트)
- `README.md` 신규 — 로그아웃 제약(토큰 폐기 목록 없음, 탈취 시 최대 30분 유효) 사용자 대상 명시 + 최초 관리자 계정 안내(acceptance.md §D.3 DoD 항목)

**AC PASS/FAIL 매트릭스** (M4 대응분 AC-AUTH-016~020):

| AC | Status | Verification | Evidence |
|----|--------|--------------|----------|
| AC-AUTH-016 (관리자 계정 생성) | PASS | `AdminSeederTest.시더_실행시_관리자_계정이_생성되고_해당_계정으로_로그인하면_ADMIN_클레임_토큰이_발급된다` (격리 실행) | ADMIN 역할 회원 1건 생성 + 로그인 시 `role: ADMIN` 클레임 토큰 발급 확인. `grep -rn "app.admin" src/main` 결과 프로퍼티 참조만 존재, 리터럴 비밀번호 없음 |
| AC-AUTH-017 (시더 멱등성) | PASS | `AdminSeederTest.시더를_두번_실행해도_행수가_증가하지_않고_기존_관리자의_비밀번호_해시가_변하지_않는다` (격리 실행) | 2회 실행 후 행 수 불변 + `password_hash` 불변 확인 |
| AC-AUTH-018 (민감 정보 로그 미기록) | PASS | `SensitiveLogIntegrationTest.회원가입_로그인_보호엔드포인트_호출_로그에_민감정보가_기록되지_않는다` (격리 실행) | 회원가입·로그인·보호엔드포인트 호출 동안 캡처한 전체 로그에서 평문 비밀번호·해시·토큰·서명 비밀키 모두 미검출 |
| AC-AUTH-019 (백엔드 단독 검증 가능성) | PASS | 저장소 전체 구조 확인 | 프론트엔드 산출물 없음 — 이 SPEC의 모든 테스트가 Spring Boot 테스트만으로 구성됨 |
| AC-AUTH-020 (커버리지 및 정적 품질) | PASS | `./gradlew jacocoTestReport -x test` (기존 실행 데이터 집계) | 전체 커버리지 94%(575건 중 29건 미검증) — 85% 기준 상회. `member/seed` 패키지(M4 신규 코드) 92%. lint 플러그인 미구성으로 "린트 에러 0건"은 M1과 동일하게 공허하게 충족 |

**M1~M3 회귀 확인**: 전체 스위트를 여러 차례 재시도하는 동안 발생한 실패는 **10~11건 모두 예외 없이 동일한 인프라 시그니처**(`org.springframework.transaction.CannotCreateTransactionException` → `HikariPool ... Connection is not available, request timed out`)였다 — assertion 실패 0건. `grep -h '<failure message=' build/test-results/test/*.xml | grep -oE 'type="[^"]*"' | sort | uniq -c` → 전부 동일 타입 1종으로 집계 확인. M1~M3 관련 클래스(`SignupIntegrationTest`, `LoginIntegrationTest`, `AuthorizationIntegrationTest` 등)의 회귀는 발견되지 않았다.

**빌드/테스트 검증 (오케스트레이터 직접 재확인 — develop-auth-m4 에이전트가 코드 작성만 완료하고 최종 보고 없이 유휴 상태로 전환되어 직접 이어받아 검증함)**:
- `./gradlew compileJava` — UP-TO-DATE (컴파일 성공, `Member.createAdmin` 추가가 기존 코드에 영향 없음)
- M4 신규 테스트 격리 실행(`--tests "*AdminSeederTest" --tests "*SensitiveLogIntegrationTest"`): **BUILD SUCCESSFUL**, 3/3 전부 PASS (`failures="0" errors="0"`)
- 전체 스위트(`./gradlew clean test`, 41개 테스트)를 재시도하는 동안 이 개발 환경의 Docker 자원 경합(§E.2 M2/M3 절 참조)이 이번에는 더 심하게 나타나(10~11건 간헐적 실패) — 원인은 두 개의 전체 스위트 실행이 실수로 동시에 돌고 있었기 때문으로 판단, 발견 즉시 중복 프로세스를 정리(`./gradlew --stop` + 유령 워커 종료)하고 단일 실행으로 재시도했으나 이 환경 자체의 자원 여유가 이미 줄어든 상태라 유의미하게 개선되지 않음. 위 회귀 확인 절차로 코드 정확성은 별도로 검증함
- 서브에이전트 경계 grep: `grep -rn 'AskUserQuestion' src/` → 0건
- 프로덕션 반입 금지 게이트(M3에서 확립, M4에서도 재확인): `grep -rn "/api/test/" src/main` → 0건
- **잔여 위험**: M2/M3과 동일한 환경 이슈. 이번 세션 동안 자원 경합이 심화된 것을 관찰했으므로, 후속 SPEC(`SPEC-COURSE-001`)의 run 단계 진입 전 Docker Desktop 재시작 또는 리소스 재할당을 권장한다.

**DoD 체크리스트 확인 (acceptance.md §D.3 — SPEC 전체 마감 조건)**:
- [x] AC-AUTH-001~020 전부 통과 (M1~M3은 §E.2 각 절, M4는 위 매트릭스)
- [x] 추적성 매트릭스(acceptance.md §D.2) 요구사항 24건 + 불변식 4건 전부 커버
- [x] 전체 커버리지 94% (≥85%), 이번 커밋(`member/seed`) 92% (≥80%)
- [x] 컴파일 에러 0건. 별도 lint 플러그인 미구성(M1에서 확립된 상태, 범위 확장 안 함)
- [x] spec.md §D 범위 제외 항목(리프레시 토큰·폐기 목록·소셜 로그인·비밀번호 재설정) 미구현 확인 — M4 신규 파일은 `AdminSeeder`/`AdminProperties`뿐
- [x] 로그아웃 제약이 `README.md`에 사용자 대상으로 명시됨
- [x] plan.md §C.5.1 테스트 픽스처가 테스트 소스 트리에만 존재 (`grep -rn "/api/test/" src/main` → 0건)
- [x] 미해소 클래리피케이션 마커 없음 (`plan.md` §A.1: "미해소 클래리피케이션 마커가 없다")

### Amendment 1 — `/error` permitAll 수정 (2026-08-30, completed → in-progress → completed)

**배경**: 오케스트레이터가 실행 중인 실서버에 대해 직접 `curl` 재현 테스트를 수행하여 근본 원인을 규명했다. 자세한 배경·근거는 spec.md `## Amendments` Amendment 1 참고.

**RED — 회귀 재현**:
- 신규 파일: `src/test/java/com/hongseob/openclass_ap/common/config/SecurityErrorForwardIntegrationTest.java` — `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`(MockMvc 아님, 서블릿 컨테이너의 `/error` 내부 포워드를 실제로 재현하기 위함) 기반, 4개 테스트
- 수정 전 실행: 4/4 FAILED, 전부 `expected: 400 BAD_REQUEST but was: 401 UNAUTHORIZED` (verbatim)

**GREEN — 최소 수정**:
- `src/main/java/com/hongseob/openclass_ap/common/config/SecurityConfig.java` — `authorizeHttpRequests`에 `.requestMatchers("/error").permitAll()` 1줄 추가(다른 인가 규칙 변경 없음) + 근본 원인을 설명하는 클래스 javadoc 보강
- `build.gradle` — `spring-boot-resttestclient`가 참조하는 `RestTemplateBuilder`(Spring Boot 4 모듈화로 `spring-boot-restclient`에 분리)를 위해 `testImplementation 'org.springframework.boot:spring-boot-restclient'` 1줄 추가
- 수정 후 실행: 4/4 PASS

**라이브 서버 재검증 (curl)**:
```
$ curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/auth/signup -H "Content-Type: application/json" -d '{"email":"repro2@local.test","password":"short1"}'
{"timestamp":"2026-08-30T04:49:51.720Z","status":400,"error":"Bad Request","path":"/api/auth/signup"}
HTTP_STATUS:400
$ curl -s -w "\nHTTP_STATUS:%{http_code}\n" -X POST http://localhost:8080/api/auth/signup -H "Content-Type: application/json" -d '{"email":'
{"timestamp":"2026-08-30T04:49:51.742Z","status":400,"error":"Bad Request","path":"/api/auth/signup"}
HTTP_STATUS:400
```

**Blast radius 스팟체크 (신규 테스트 3/4)**:
- 로그인(`/api/auth/login`) 깨진 JSON → 400 확인
- ADMIN 토큰으로 `/api/admin/courses` 강좌명 누락 요청 → 400 확인(인증된 보호 엔드포인트에서도 동일 결함이었음을 확인)

**회귀 스윕 — 격리 실행(이 환경의 확립된 검증 방법, §E.2 M2/M3/M4 절 참조)**:
- `SecurityConfig`와 인접한 9개 테스트 클래스를 각각 격리 실행(`./gradlew test --tests "<클래스>" --rerun`): `SignupIntegrationTest`, `LoginIntegrationTest`, `AuthorizationIntegrationTest`, `CorsIntegrationTest`, `SensitiveLogIntegrationTest`, `AdminSeederTest`, `CourseInputValidationIntegrationTest`, `CourseAdminApiIntegrationTest`, `SecurityErrorForwardIntegrationTest` — **전부 BUILD SUCCESSFUL**, 신규 실패 0건
- 전체 스위트(`./gradlew test --rerun`)를 3회 시도 — 매회 M2/M3/M4에서 이미 기록한 것과 **동일한 인프라 시그니처**(`CannotCreateTransactionException` → `HikariPool ... ConnectException`)로 일부 클래스가 실패했다. `grep -oE "(Caused by: [a-zA-Z.]+Exception)" <log> | sort | uniq -c` 결과 4종 예외가 각각 동일 건수로 나타나 전부 하나의 연쇄에 속함을 확인, `AssertionFailedError`/`opentest4j` 매칭 **0건** — assertion 실패가 아니라 이 개발 환경의 Docker/Testcontainers 자원 경합(이미 §E.2 M2 절에서 "격리 실행 시 100% 통과"로 확립된 동일 패턴)임을 재확인했다. 이 결함은 `SecurityConfig` 변경과 무관한 `Course*`/`Enrollment*` 클래스에서도 동일하게 발생하여, 이번 변경이 원인이 아님을 뒷받침한다.
- **잔여 위험**: 이 환경에서 `./gradlew test`(전체 스위트) 단일 실행은 여전히 재시도가 필요할 수 있다(M2에서부터 기록된 기존 잔여 위험, 이번 amendment로 새로 발생한 것 아님).

**MX Tag**: 신규 위험 지점(고 fan_in, 고 complexity) 없음 — `SecurityConfig`의 1줄 인가 규칙 추가는 기존 @MX 태그 상태에 영향 없음.

## §E.3 Run-phase Audit-Ready Signal

- `run_status`: audit-ready
- M1~M4 전체 마일스톤 완료. AC-AUTH-001~020 전부 PASS, DoD 체크리스트 8항목 전부 충족
- 다음 단계: `/moai sync SPEC-AUTH-001` (문서 동기화 + `implemented → completed` 전이)

## §E.4 Sync-phase Audit-Ready Signal

- `sync_status`: audit-ready
- `sync_commit_sha`: this commit (self-referential — a commit cannot cite its own SHA; see `spec-frontmatter-schema.md` § SHA placeholder backfill exemption). 확인은 `git log -1 --format=%H .moai/specs/SPEC-AUTH-001/progress.md` 또는 이 커밋의 SHA로 갈음한다.
- 동기화 산출물: `CHANGELOG.md`(신규 생성, `[Unreleased]` 섹션에 SPEC-AUTH-001 항목 추가), `.moai/project/tech.md`(인증/인가 섹션 최신화 — Spring Security 6 + JWT 확정 반영), `.moai/project/structure.md`(패키지 구조 최신화), `spec.md`/`plan.md`/`acceptance.md`/`progress.md` frontmatter `status: in-progress → completed` 전이(단일 sync 커밋에 병합, 별도 Mx 커밋 없음)
- `product.md`: 이미 v1 범위 §1(회원 가입/로그인)이 정확히 기술되어 있어 변경 없음

## §G Post-Sync 독립 감사 후속 조치

sync-auditor 독립 감사 결과: **종합 PASS** (가중 87.5/100, 조화평균 86.6/100). Security 차원 Critical/High 0건으로 HARD 임계 미저촉. 차단 사유 없음. 비차단 권고 7건(F1~F7) 중 안전하고 범위가 명확한 2건을 이 자리에서 직접 수정했다(Rule 1 예외 — 단일 라인 수준의 명백한 결함 수정).

**수정한 항목**:
- **F1 (Low, high-confidence)** — `JwtAuthenticationFilter.authenticate()`가 `JwtException`만 catch하여, `"Bearer "` 뒤에 토큰이 없는 경우(jjwt가 `IllegalArgumentException`을 던짐) 예외가 필터 밖으로 전파될 수 있었다. 재현 테스트 작성(RED: `IllegalArgumentException` 확인) → `catch (JwtException | IllegalArgumentException e)`로 확장(GREEN) → `JwtAuthenticationFilterTest`(4/4 PASS, 격리 실행) 확인.
- **F4 (Low, medium-confidence)** — `AuthorizationIntegrationTest`의 AC-AUTH-014 세션 쿠키 부재 단정이 MockMvc 환경에서 원천적으로 실패할 수 없는 공허한 단정(vacuous assertion)이었다. `result.getRequest().getSession(false)`가 `null`인지 관찰하는 단정을 추가하여 STATELESS 정책이 실제로 세션을 생성하지 않음을 검증하도록 보강. `AuthorizationIntegrationTest`(6/6 PASS, 격리 실행) 확인.

**의도적으로 수정하지 않고 남긴 항목(범위 규율)**:
- **F2 (Medium, 이 SPEC 범위 외)** — `src/main/resources/application-local.properties`에 실제 로컬 Supabase DB 비밀번호가 평문으로 존재한다. 저장소 유출은 없음(`.gitignore` 매칭 확인, 커밋 이력 0건, 도입 커밋 `bdc4f54`는 M1 이전으로 이 SPEC 밖 유래). **사용자가 직접 확인·필요시 비밀번호 교체를 권장** — 이 파일은 SPEC 범위 밖이라 자동 수정하지 않았다.
- **F3 (Low)** — `SecurityConfig.java:41`의 `GET /api/courses` permitAll 규칙이 `SPEC-COURSE-001` 소유 경로를 선반영하고 있다. 현재 핸들러가 없어 보안 영향은 없으나 범위 선점이다. `SPEC-COURSE-001` plan 단계에서 이 규칙의 소유권을 확인하고 필요시 재검토할 것.
- F5~F7(Info) — 후속 SPEC의 AC 작성 시 커버리지 지표 명시(F5), lint 플러그인 도입 후 AC-AUTH-020 린트 절 재검증(F6), `role` 클레임 부재 토큰에 대한 방어적 null 검사 고려(F7). 모두 정보성 권고이며 즉시 조치 불필요.

**검증 (오케스트레이터 직접 재확인)**:
```
$ ./gradlew test --tests "*JwtAuthenticationFilterTest" --tests "*AuthorizationIntegrationTest"
BUILD SUCCESSFUL — JwtAuthenticationFilterTest 4/4, AuthorizationIntegrationTest 6/6, 실패 0건
```
- MX Tag 검증: sync 하위 단계로 수행 — `grep -rn '@MX:' src/main` 결과 신규 태그 없음(M1~M4에서 이미 SecurityConfig/필터 등 위험 지점에 대한 별도 @MX 태그를 추가하지 않았음을 확인; 이 SPEC 범위에서 새로 태그를 추가해야 할 고fan-in/위험 지점은 발견되지 않음)
- 다음 단계: 없음 (SPEC 완료, `status: completed`)
