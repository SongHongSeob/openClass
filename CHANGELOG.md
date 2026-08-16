# Changelog

이 프로젝트의 주요 변경 사항을 기록합니다. 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.0.0/)를 참고합니다.

## [Unreleased]

### Added — SPEC-AUTH-001: 회원 가입·로그인 및 JWT 인증 기반

- **회원 도메인 (M1)**: `Member`/`MemberRole` 엔티티, `MemberRepository`, `MemberService`. `POST /api/auth/signup` 회원가입 API — BCrypt 비밀번호 해시 저장, 이메일 정규화(trim+lowercase), 중복 이메일 409 거부, 요청 본문의 `role` 필드 주입 차단(항상 `MEMBER`로 생성).
- **JWT 토큰 발급 (M2)**: `JwtTokenProvider`(HMAC-SHA256, `sub`/`role`/`iat`/`exp` 클레임), `JwtProperties`(`app.jwt.secret`/`app.jwt.access-token-ttl` 외부 프로퍼티 바인딩). `POST /api/auth/login` 로그인 API — 성공 시 액세스 토큰 발급, 실패(틀린 비밀번호/미가입 이메일) 시 원인 구분 없이 동일한 401 응답.
- **필터 체인 및 인가 (M3)**: `SecurityConfig`(`SecurityFilterChain` Bean, STATELESS 세션, CSRF 비활성화), `JwtAuthenticationFilter`(`Authorization: Bearer` 헤더 검증). 경로별 인가 — `/api/auth/**` 및 `GET /api/courses` permitAll, `/api/admin/**` `ADMIN` 역할 필요, 그 외 인증 필요. 인증 실패(401)와 인가 실패(403)를 명시적 `AuthenticationEntryPoint`/`AccessDeniedHandler`로 구분.
- **최초 관리자 시더 및 비기능 마감 (M4)**: `AdminSeeder`(`ApplicationRunner`, 기동 시 1회 실행, 멱등), `AdminProperties`(`app.admin.email`/`app.admin.password` 외부 프로퍼티). 민감 정보(평문 비밀번호/해시/토큰/서명 비밀키) 로그 미기록 검증. `README.md`에 로그아웃 제약(토큰 폐기 목록 없음) 및 최초 관리자 계정 안내 추가.

### Verification

- 인수 기준 AC-AUTH-001 ~ AC-AUTH-020 (20건) 전부 PASS — `.moai/specs/SPEC-AUTH-001/acceptance.md` §D.2 추적성 매트릭스 기준.
- 전체 테스트 커버리지 94% (목표 ≥85%). `member/seed` 패키지(M4 신규) 92%.
- `./gradlew build` BUILD SUCCESSFUL. 별도 lint 플러그인(checkstyle/spotbugs 등)은 아직 도입되지 않음.

### Known Limitations

- 로그아웃은 클라이언트 측 토큰 폐기로만 수행되며, 서버 측 토큰 폐기 목록(denylist)은 v1 범위에서 제외됨 — 탈취된 토큰은 만료 시각(기본 30분)까지 유효.
- 리프레시 토큰 회전, 소셜 로그인, 비밀번호 재설정은 v1 범위 밖.
