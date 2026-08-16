# 프로젝트 구조 (Structure)

## 현재 상태

표준 Gradle/Spring Boot 단일 모듈(single-module) 레이아웃. SPEC-AUTH-001(회원 가입·로그인·JWT 인증)이 완료되어 `member`, `common/config`, `common/exception`, `common/response` 패키지가 존재한다.

```
openclass-ap/
├── build.gradle                 # Spring Boot 4.1.0, Java 17 toolchain
├── settings.gradle
├── gradlew / gradlew.bat
├── README.md                    # 인증 API 안내 (로그아웃 제약, 최초 관리자 계정)
├── src/
│   ├── main/
│   │   ├── java/com/hongseob/openclass_ap/
│   │   │   ├── OpenclassApApplication.java
│   │   │   ├── member/
│   │   │   │   ├── Member.java / MemberRole.java / MemberRepository.java / MemberService.java
│   │   │   │   ├── AuthController.java              # POST /api/auth/signup, /login
│   │   │   │   ├── dto/                              # SignupRequest/Response, LoginRequest/Response
│   │   │   │   ├── jwt/                               # JwtTokenProvider, JwtAuthenticationFilter
│   │   │   │   └── seed/                              # AdminSeeder (최초 관리자 계정 시더)
│   │   │   └── common/
│   │   │       ├── config/                            # SecurityConfig, JwtProperties, AdminProperties, PasswordEncoderConfig
│   │   │       ├── exception/                          # DuplicateEmailException, InvalidCredentialsException, GlobalExceptionHandler
│   │   │       └── response/                           # ErrorResponse
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/hongseob/openclass_ap/
│           ├── support/AbstractIntegrationTest.java   # Testcontainers(PostgreSQL) 공통 베이스
│           └── member/                                # 단위/통합 테스트 (Signup/Login/Authorization/AdminSeeder 등)
├── .moai/                       # MoAI-ADK 설정 및 SPEC 워크스페이스
│   ├── config/                  # quality.yaml, language.yaml, user.yaml 등
│   ├── project/                 # product.md, structure.md, tech.md (본 문서)
│   └── specs/                   # SPEC-AUTH-001(완료), SPEC-COURSE-001, SPEC-ENROLLMENT-001(계획)
└── .claude/                     # Claude Code 에이전트/스킬/규칙
```

## 백엔드 패키지 구조

`com.hongseob.openclass_ap` 하위 도메인별 패키지 분할이 SPEC-AUTH-001에서 확정되었다: `member`(인증/회원 도메인), `common`(횡단 관심사 — config/exception/response). SPEC-COURSE-001의 `course`(강좌 도메인, 하위 `dto`/`admin` 패키지 포함)가 동일한 패턴(도메인 패키지 + `common` 재사용)을 그대로 따랐다. 이후 SPEC(`enrollment`, `waitlist` 등)도 동일한 패턴을 따를 것으로 예상된다.

## 프론트엔드

아직 존재하지 않음(그린필드). React 워크스페이스의 정확한 위치는 여전히 미결정 — 자세한 내용은 `tech.md`의 "미결정 사항" 참고.

## SPEC 워크스페이스

- **SPEC-AUTH-001** (완료) — 회원 가입·로그인 및 JWT 인증 기반. `.moai/specs/SPEC-AUTH-001/`
- **SPEC-COURSE-001** (완료) — 강좌 엔티티·카탈로그 조회 및 관리자 강좌 관리. `.moai/specs/SPEC-COURSE-001/`
- **SPEC-ENROLLMENT-001** — 계획 단계, 이후 SPEC에서 진행 예정

## 참고

- 데이터베이스: PostgreSQL. 테스트는 Testcontainers(PostgreSQL)로 실행.
- 개발 방법론: TDD — RED-GREEN-REFACTOR 사이클로 진행, `src/test/java` 하위에 도메인별 테스트 구조가 함께 성장.
