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

### 미결정 사항 — Plan 단계에서 결정 필요

- **인증/인가**: 현재 Spring Security 의존성이 build.gradle에 없다. 이메일+비밀번호 로그인 구현 시 Spring Security 도입 여부 및 세션/토큰(JWT 등) 전략을 SPEC plan 단계에서 결정해야 한다. 특정 라이브러리를 임의로 가정하지 않는다.

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
