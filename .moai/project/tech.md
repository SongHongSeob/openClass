# 기술 스택 (Tech)

## 백엔드

- **언어**: Java 17 (Gradle toolchain 고정)
- **프레임워크**: Spring Boot 4.1.0
- **빌드 도구**: Gradle (`io.spring.dependency-management` 1.1.7 플러그인 사용)
- **패키지 베이스**: `com.hongseob.openclass_ap` (Gradle group `com.hongseob`, 애플리케이션 클래스 `OpenclassApApplication`)

### 현재 선언된 의존성 (build.gradle)

| 구분 | 의존성 | 용도 |
|---|---|---|
| implementation | spring-boot-starter-data-jpa | JPA/DB 접근 |
| implementation | spring-boot-starter-validation | 입력값 검증 |
| implementation | spring-boot-starter-webmvc | REST API (Spring MVC) |
| compileOnly / annotationProcessor | lombok | 보일러플레이트 코드 축소 |
| runtimeOnly | postgresql | PostgreSQL JDBC 드라이버 |
| testImplementation | spring-boot-starter-data-jpa-test, spring-boot-starter-validation-test, spring-boot-starter-webmvc-test | 계층별 테스트 슬라이스 |
| testRuntimeOnly | junit-platform-launcher | JUnit 5 실행 |
| implementation | spring-boot-starter-security | Spring Security 6 — 인증/인가 필터 체인 |
| implementation | io.jsonwebtoken:jjwt-api (0.12.6) | JWT 발급/검증 (JJWT API) |
| runtimeOnly | io.jsonwebtoken:jjwt-impl, jjwt-jackson (0.12.6) | JJWT 런타임 구현체 |
| testImplementation | spring-boot-starter-security-test | Security 테스트 슬라이스 |
| testImplementation | spring-boot-testcontainers, org.testcontainers:junit-jupiter, org.testcontainers:postgresql | Testcontainers(PostgreSQL) 기반 통합 테스트 |

### 인증/인가 (SPEC-AUTH-001에서 확정 — `src/main/java/com/hongseob/openclass_ap/member/`, `common/config/`)

- **인증 방식**: JWT 액세스 토큰 단일 방식(HMAC-SHA256, `io.jsonwebtoken:jjwt` 0.12.6). 리프레시 토큰 회전·서버 측 토큰 폐기 목록(denylist)은 v1 범위 제외 — 로그아웃은 클라이언트 측 토큰 폐기로만 수행.
- **세션 전략**: STATELESS(`SecurityConfig`의 `SecurityFilterChain` Bean). `HttpSession` 미사용, `JwtAuthenticationFilter`가 `Authorization: Bearer` 헤더를 매 요청마다 검증.
- **비밀번호 해싱**: BCrypt(`PasswordEncoderConfig`).
- **인가 규칙**: `/api/auth/**` + `GET /api/courses`, `/api/courses/*`(목록·상세) permitAll, `/api/admin/**` `ADMIN` 역할 필요, 그 외 인증 필요. 401(미인증)/403(권한부족)을 명시적 `AuthenticationEntryPoint`/`AccessDeniedHandler`로 구분.
- **테스트 인프라**: Testcontainers(PostgreSQL)로 통합 테스트 실행 — `AbstractIntegrationTest` 공통 베이스.

## 데이터베이스

- **PostgreSQL** — 이미 runtime 의존성으로 선언되어 있다.

## 프론트엔드

- **React** — 이 저장소에는 아직 스캐폴딩되지 않은 그린필드 상태.
- 정확한 배치(예: 별도 sibling 디렉터리 vs. 저장소 내 `frontend/` 폴더)는 아직 결정되지 않았으며, SPEC plan 단계에서 확정할 열린 결정 사항이다. 구조를 임의로 고정하지 않는다.

## 개발 방법론

- **TDD (Test-Driven Development)** — `.moai/config/sections/quality.yaml`의 `constitution.development_mode: "tdd"` 설정에 따름 (RED-GREEN-REFACTOR 사이클, 커밋당 최소 커버리지 80%, 목표 커버리지 85%).

## 품질 게이트

- LSP 기반 품질 게이트 활성화 (`lsp_quality_gates.enabled: true`) — run 단계 에러/타입에러/린트에러 0건 요구
- ast-grep 패턴 기반 정적 분석 게이트 활성화
