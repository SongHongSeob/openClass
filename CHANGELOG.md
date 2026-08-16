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

### Added — SPEC-COURSE-001: 강좌 엔티티·카탈로그 조회 및 관리자 강좌 관리

- **강좌 엔티티 및 제약 (M1)**: `Course`/`CourseStatus`/`CourseRepository`. CHECK 제약 3종(`ck_course_capacity_min`, `ck_course_enrolled_range`, `ck_course_status`) — 정원 1 미만, `enrolled_count`가 `[0, capacity]` 범위 밖, 모집 상태가 `OPEN`/`CLOSED` 외의 값을 DB 계층에서 거부. `enrolled_count`는 정적 팩토리에서 항상 0으로 초기화되며 이 SPEC의 어떤 프로덕션 코드 경로도 이를 변경하지 않는다(`SPEC-ENROLLMENT-001`의 큐 워커가 단독 소유).
- **공개 카탈로그 API (M2)**: `GET /api/courses`(페이지네이션 목록), `GET /api/courses/{id}`(상세) — 인증 없이 접근 가능. 잔여 정원(`capacity - enrolledCount`)은 저장하지 않고 조회 시 계산. 존재하지 않는 강좌 조회 시 404. 모집 마감(`CLOSED`) 강좌도 목록에서 숨기지 않음. `SecurityConfig`의 공개 GET 매처를 `/api/courses`, `/api/courses/*` 두 패턴으로 확장.
- **관리자 강좌 관리 API (M3)**: `POST/PATCH/POST .../close/DELETE /api/admin/courses...` — `ADMIN` 역할만 허용(그 외 403). 강좌 생성 시 `OPEN`·확정 인원 0으로 초기화. 정원 1 미만 생성/수정 거부(400). 정원을 확정 인원 미만으로 축소하는 요청은 409로 거부(기존 확정자 강제 탈락 없음). 삭제 요청은 물리 삭제 대신 `CLOSED` 전이로 처리(hard delete 없음). 존재하지 않는 강좌에 대한 수정/마감/삭제는 404.
- **비기능 마감 (M4)**: `HasDateRange`/`@ValidDateRange`/`DateRangeValidator` — 종료 일시가 시작 일시보다 이른 요청을 400으로 거부하는 클래스 레벨 교차 필드 검증. 강좌명 누락, 정원 형식 오류(비정수)도 서버 측에서 400 거부.

### Verification

- 인수 기준 AC-CRS-001~005, AC-CAT-001~005, AC-ADM-001~008, AC-NFR-001~003 (총 19건) 전부 PASS — `.moai/specs/SPEC-COURSE-001/acceptance.md` §D.2 추적성 매트릭스 기준.
- 커버리지: `course`/`course/dto`/`course/admin` 패키지 100%, 신규 `common/validation` 패키지 89% — 전부 85% 목표 상회.
- `./gradlew compileJava compileTestJava` BUILD SUCCESSFUL. 별도 lint 플러그인은 아직 도입되지 않음(SPEC-AUTH-001과 동일).

### Known Limitations

- 정원 증설에 따른 대기명단 승격 처리는 이 SPEC의 범위 밖(`SPEC-ENROLLMENT-001`이 소유 예정).
- 수강신청 접수·큐·대기명단 등록/취소 전체는 이 SPEC의 범위 밖.
