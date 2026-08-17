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

### Added — SPEC-ENROLLMENT-001: 선착순 수강신청 큐·워커 및 대기명단 자동 승격

- **스키마 및 접수 순서 보장 (M1)**: `enrollment_request`/`enrollment`/`waitlist_entry` 테이블 및 상태 도메인. 강좌 단위 배타 잠금(접수 잠금)으로 `ENROLL`·`CANCEL`·`CAPACITY_INCREASE` 큐 INSERT 전 경로의 접수 순서를 보장.
- **워커 및 확정 경로 단일성 (M2)**: `EnrollmentQueueWorker` — `SELECT ... FOR UPDATE SKIP LOCKED` 클레임 쿼리로 큐를 순차 디스패치. `enrolled_count` 변경 경로를 워커 단일 소유로 확정하여 정원 초과 확정 0건을 보장. 처리 실패 건은 격리(다른 큐 항목 처리를 막지 않음).
- **상태 조회 (M3)**: `GET /api/enrollment-requests/{requestId}` — 요청자 소유권 범위로 스코프된 상태 조회, 대기 순번 노출.
- **대기명단 및 취소 (M4)**: `DELETE /api/enrollments/{enrollmentId}`, `DELETE /api/waitlist-entries/{entryId}` — CANCEL 처리, 대기명단 자동 승격 헬퍼, 소유권 2계층 검증(회원 본인 + 요청 소유 이중 확인).
- **관리자 정원 증설 연동 (M5)**: 기존 `PATCH /api/admin/courses/{id}` 강좌 수정 엔드포인트를 통한 정원 증설 시 대기명단 승격 큐 연동.
- **마감 정리 (M6)**: 입력 검증 강화, 추적성 로깅, 커버리지/린트 결과의 정직한 보고(jacoco 패키지 단위 집계 환경 제약을 클래스 단위 대체 증거로 명시).
- **sync-audit 후속 수정**: `Course.enrolled_count`에 `updatable=false` 추가(관리자 강좌 수정이 워커의 동시 정원 증가를 되돌릴 수 있던 잠재 결함 차단), `EnrollmentAggregateBoundaryArchitectureTest`에 `CourseCapacityRepository` 패키지 경계 ArchUnit 규칙 추가, `EnrollmentReceiptLockOrderTest` 잠금 순서 검증을 메서드 단위로 강화. 상세: progress.md §E.4 "sync-auditor 1차 감사".
- **보유 내역 조회 (M7, v0.3.0 제자리 개정)**: `GET /api/enrollments/mine`(내 활성 확정 목록, `enrollmentId` 오름차순), `GET /api/waitlist-entries/mine`(내 활성 대기 목록, `position` 오름차순) — 둘 다 인증 주체만으로 스코프되며 회원 식별자 입력 파라미터를 받지 않는다(`SPEC-FRONTEND-001`의 `DEP-2` 계약 폐쇄). 읽기 전용(`@Transactional(readOnly = true)`)이며 기존 워커·큐·접수 잠금·승격 경로는 무변경.

### Verification

- 인수 기준 AC-ENR-001~053 (53건) 중 52건 PASS + 1건 PASS-WITH-DEBT(AC-ENR-049, 커버리지 집계 환경 제약 — progress.md §E.2 M6 참고), 0건 FAIL — `.moai/specs/SPEC-ENROLLMENT-001/acceptance.md` §D.2 추적성 매트릭스 기준.
- (v0.3.0 M7 추가) 인수 기준 AC-ENR-054~058 (5건) 전부 PASS(격리 실행) — 누적 AC-ENR-001~058 총 58건 중 57건 PASS + 1건 PASS-WITH-DEBT, 0건 FAIL. `.moai/specs/SPEC-ENROLLMENT-001/acceptance.md` §D.2 추적성 매트릭스 기준.

### Fixed — SPEC-ENROLLMENT-001 M8 (v0.3.1/v0.3.2 제자리 개정): 큐 처리 실패의 진단 불가능성 해소

- **진단 로깅 (M8 Step A)**: `EnrollmentQueueWorker.drainQueue()`의 `catch (RuntimeException ex)` 블록이 포착한 예외를 **어떤 형태로도 기록하지 않고 폐기**하던 결함을 고침 — 이제 SLF4J로 WARN 이상 레벨에 예외 타입 전체 이름·메시지 원문·스택 트레이스를 기록한다. `enrollment`/`waitlist` 패키지 프로덕션 소스 전수 검사로 이런 무기록 폐기 catch 블록이 이 1건뿐임을 확인했다.

### Verification — M8 Step B: 실제 실패 재현 및 근본 원인 판정 (코드 수정 없음)

- M8 Step B는 `POST /api/courses/{courseId}/enrollments` 실제 HTTP 요청 흐름으로 프로덕션 실패(`DataIntegrityViolationException` — `course_term_id` NOT NULL 제약 위반)를 재현하고, DB 스키마 직접 조사 + 소스/git 이력 전수 grep(둘 다 `course_term` 0건 매치)으로 **(b) 외부/인프라 요인**으로 판정했다 — 이 로컬 개발용 Supabase 프로젝트에 이 코드베이스가 만들거나 참조한 적 없는 이질적 스키마 객체(`course_term` 테이블·FK·추가 컬럼·불일치 CHECK 제약)가 이미 존재하던 것이 원인이며, 이 코드베이스 자체의 결함이 아니다. SPEC 설계상 (b) 판정은 코드 수정을 요구하지 않으며 — 실제로 Step B에서 소스 코드는 **한 줄도 변경하지 않았다**(`progress.md`만 갱신). SPEC-FRONTEND-001 프론트엔드 개발 전 과정에서 관찰되던 지속적인 수강신청 처리 실패의 원인이, 이 코드베이스의 버그가 아니라 로컬 개발 환경 DB 상태였음이 이로써 규명되었다.
- 인수 기준 AC-ENR-059~061 (3건) 전부 PASS — `.moai/specs/SPEC-ENROLLMENT-001/acceptance.md` §D.2 추적성 매트릭스 기준. 누적 AC-ENR-001~061 총 61건 중 60건 PASS + 1건 PASS-WITH-DEBT(AC-ENR-049), 0건 FAIL.

### Known Limitations

- 다중 워커 인스턴스 배포 시 접수 순서 보장이 성립하지 않음(워커 1개 전제) — README.md 배포 전제 절 참고.
- jacoco 패키지 단위 커버리지 집계가 4회 연속(M4/M5/M6/M7) 미확보되어 클래스 단위 개별 실행 수치로 대체됨 — CI 환경에서 재시도 권장(progress.md §E.2 M7 잔여 위험 1번 — M6에서 근본 원인을 "연속 격리 재실행의 누적 Docker 자원 고갈"로 규명).
- 이 개정(M8)에서 진단 로깅을 도입했으나, 그 로그가 실제로 잡아낸 프로덕션 실패의 근본 원인은 이 로컬 개발 DB에 국한된 인프라 상태였다 — 다른 Supabase 프로젝트나 CI 환경에서는 재현되지 않을 수 있다.
