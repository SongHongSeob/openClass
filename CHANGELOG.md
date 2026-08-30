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

### Added — SPEC-FRONTEND-001: React 클라이언트 — 회원·강좌·수강신청 전 화면 및 관리자 콘솔

- **워크스페이스 부트스트랩 (M1)**: 저장소 내 신규 `frontend/` 패키지(Vite 8 + React 19 + TypeScript 6). `src/api/types.ts`(타입 단일 선언) · `src/api/errors.ts`(오류 정규화 단일 지점, 401-우선 판정 순서) · `src/api/client.ts`(fetch 래퍼). 포트 5173 고정(`strictPort`), `vite.config.ts`에 dev 프록시 없음 — 백엔드 CORS 설정에 직접 의존. 브라우저 실동작으로 프록시 없는 교차 오리진 `GET /api/courses` 성공(콘솔 CORS 오류 0건) 확인.
- **세션 및 인증 (M2)**: JWT 페이로드 디코드(서명 검증 없음, 표시 목적 한정) · `sessionStorage` 기반 세션 저장/복원/폐기 · React Context 세션 상태 · 401 전역 통지 단일 배선 지점 · `RequireAuth`/`RequireRole` 라우트 가드(보안 경계 아님, 실제 강제는 백엔드 403). 회원가입/로그인 화면. 라우터는 이 마일스톤에서 미도입(보호 화면 부재).
- **강좌 카탈로그 (M3)**: 목록·상세 화면, 페이지네이션 컨트롤, `CLOSED` 강좌 신청 차단 표시. 목록/상세 전환은 로컬 상태(라우터 미도입, 신규 의존성 0건).
- **수강신청 제출 및 상태 폴링 (M4)**: React Router + TanStack Query 도입(신규 의존성 2건). 신청 제출 → 요청 상태 폴링(1s→2s→3s 점증 스케줄, 종단 상태 도달 시 중단) → 확정/대기 결과 표시. 폴링 경과 시각 기준점을 `sessionStorage`에 보존(새로고침 후에도 리셋되지 않음, AC-FE-073a/073b).
- **관리자 강좌 관리 (M5)**: 강좌 생성·수정(전 필드 재전송)·마감 화면. `ADMIN` 역할에만 메뉴 노출, 그 외 역할은 URL 직접 진입 시 "권한 없음" 안내(로그인 유도 아님). 정원 증설 시 비동기 승격 안내 문구, 정원 축소 거부(409) 시 원문 미노출 안내. "삭제" 표현 미사용(마감으로 대체).
- **취소 및 보유 내역 (M6, 마지막 구현 마일스톤)**: 내 수강신청/내 대기명단 목록 화면, 확정 취소(202 비동기, 기존 폴링 경로 재사용) 및 대기 취소(200 동기, 즉시 재조회). 대기 취소 대상은 `waitlistEntryId`이며 `position`이 아님을 타입·로직 양쪽에서 구별(INV-FE-009). 대기 순번 표시는 항상 `courseTitle`과 나란히(전역 순위 아님, INV-FE-011).
- **수동 시나리오 검증 (M7, 검증 마일스톤 — 소스 변경 없음)**: `claude-in-chrome` 브라우저 자동화로 S1~S14 14개 시나리오를 실제 UI 조작으로 수행. 회원가입부터 신청·대기·관리자 CRUD·취소·세션 만료·탭 격리까지 종단 검증.

### Verification

- 인수 기준 AC-FE-001~112 및 AC-FE-901/907(총 86건) — `.moai/specs/SPEC-FRONTEND-001/acceptance.md` §D.1 추적성 매트릭스 기준. M1~M6 코드 레벨(단위 테스트 114건 + 정적 grep) 전부 PASS.
- M7 수동 시나리오 S1~S14: **12건 완전 PASS**, **2건(S7·S9) 대체·부분 검증** — S7(토큰 만료 후 재로그인 안내)은 30분 실시간 대기 대신 `sessionStorage` 토큰 무효화로 401 경로를 동등 검증(실시간 만료 자체는 미관측). S9(폴링 중단 관측)은 `claude-in-chrome` 자동화 탭의 `document.visibilityState` 항상 `hidden` 한계로 네트워크 탭 직접 관측을 하지 못해, M4에서 이미 검증된 `decideNextPoll` 코드 레벨 근거로 보완했다. 전 항목 실제 UI 조작 결과이며 자동 테스트 로그만으로 통과 처리한 항목은 없다.
- `cd frontend && npx tsc -b --force` exit=0, `npm run lint`(oxlint) exit=0, `npx vitest run` exit=0(15개 파일 114건 전부 통과), `npm run build` exit=0.

### Known Limitations

- S7(토큰 만료)·S9(폴링 중단 관측)은 완전한 실시간 원본 시나리오로 관측되지 않았다 — 각각 대체 방법과 코드 레벨 근거로 보완했다(위 Verification 참고).
- AC-FE-088(정원 증설 후 재조회 시 확정 인원 증가 관측)의 후반부와 REQ-ADM-007(409 축소 거부)의 M5 단계 확인은 확정 인원이 0인 강좌로만 시도되어 브라우저 실트리거를 하지 못했다(M7에서 등가 시나리오 S5/S6으로 해소됨 — progress.md §E.2 M5 잔여 위험 참고).
- 로그아웃 안내 문구가 로그인 직후에도 과거형으로 표시되는 부수 관찰(M2) — 기능 결함 아님, 후속 문구 다듬기 권장(progress.md §E.2 M2 참고).
