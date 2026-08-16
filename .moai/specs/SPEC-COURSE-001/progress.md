---
id: SPEC-COURSE-001
title: "강좌 엔티티·카탈로그 조회 및 관리자 강좌 관리 — 진행 기록"
version: "0.1.2"
status: in-progress
created: 2026-08-15
updated: 2026-08-16
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/course"
lifecycle: spec-anchored
tags: "course, progress"
tier: M
---

# SPEC-COURSE-001 — 진행 기록

## §E.1 Plan-phase Audit-Ready Signal

- `plan_status`: audit-ready
- 산출물: `spec.md`, `plan.md`, `acceptance.md`, `progress.md` (Tier M 필수 3종 + 진행 기록) — status: draft
- Tier 판정: **M** (예상 11~13 프로덕션 파일 + 6~8 테스트 파일)
- 요구사항 23건 + 불변식 4건, 인수 기준 21건, 미대응 0건 (acceptance.md §D.2)
- 미해소 클래리피케이션 마커: **0건**
- 선행 의존: `SPEC-AUTH-001` — `completed` 전에는 run 단계 진입 금지
- 다음 단계: Implementation Kickoff Approval (사용자 승인 대기)

### E.1.1 plan-auditor 감사 이력

| 회차 | 판정 | 점수 | 임계값 | 후속 조치 |
|---|---|---|---|---|
| 1 | **FAIL** | 0.75 | 0.80 (Tier M) | 결함 D1~D10 수정 → 산출물 v0.1.1 → v0.1.2 |
| 2 | **PASS** | **0.92** | 0.80 (Tier M) | D1~D10 전부 RESOLVED(10/10) 확인. 신규 발견 N1~N5(전부 minor/trivial, 비차단) 중 N4(용어 정의 표 렌더링 깨짐)·N5(절 제목 오기)만 직접 수정(문서 사소 결함, Rule 1 예외) → **v0.1.2 그대로 유지**(내용 변경 없음, 표 재배치만) |

**skip-eligibility**: 점수 0.92 ≥ 0.90, verdict PASS — 4조건 중 2개 충족. 나머지(artifact-hash 불변, 24h 이내)는 run 진입 시점에 재확인 필요.

**1회차 지적 대비 수정 요약 (v0.1.2)** — must-pass 항목(프론트매터·GEARS 형식·추적성 구조·교차 SPEC 참조·클래리피케이션 게이트)은 1회차에서 전부 PASS였으므로 건드리지 않았다.

| 결함 | 심각도 | 조치 | 반영 위치 |
|---|---|---|---|
| D1 | 치명 | `GET /api/courses/{id}` 공개 접근 누락 — M2에 `SecurityConfig` 매처 확장 태스크 신설, AC-CAT-002에 무헤더 전제 추가, 추적성 정정 | plan.md §C.2.1·§F M2 / acceptance.md AC-CAT-002·§D.2 / spec.md REQ-CAT-001 |
| D2 | 치명 | CHECK 제약·기본값 생성 수단 미지정 — `@Check`/`@ColumnDefault` 명시 + `ddl-auto=update` 한계 기록 | plan.md §C.1.1·§D / acceptance.md AC-CRS-002·003·005 |
| D3 | 중대 | ArchUnit 미도입 의존성 ↔ "신규 인프라 금지" 충돌 — **AC-CRS-004에서 ArchUnit 절 제거(2계층 확정)**, 교차 SPEC 인수인계 항목 신설 | acceptance.md AC-CRS-004 / plan.md HISTORY·§D·§H.1 |
| D4 | 중대 | REQ-CRS-004가 INV-CRS-003보다 약함("0이 아닌 값으로" 한정) — 리셋 포함 일체 변경 금지로 강화, 픽스처 예외는 §A.2로 이관 | spec.md REQ-CRS-004·§A.2 |
| D5 | 경미 | 400/409 이중 허용 — **409 단일 확정**(`GlobalExceptionHandler` 선례) | spec.md REQ-ADM-005 / plan.md §C.3·§G / acceptance.md AC-ADM-004 |
| D6 | 경미 | 404 예외 타입·핸들러 부재 — `CourseNotFoundException` + 기존 핸들러 확장 명시 | plan.md §C.4.2·§F M2 |
| D7 | 경미 | 페이지네이션 메타데이터 "존재"만 단언 — 경계 분할 동작 검증 추가 | acceptance.md AC-CAT-001 |
| D8 | 경미 | `member/` 코드 규약 미고정 — record DTO / private 생성자 + 정적 팩토리(세터 부재) / `AbstractIntegrationTest` 상속 3종 명시 | plan.md §C.4.1·§G |
| D9 | 경미 | REQ-ADM-006이 타 SPEC 미래 동작에 `shall` 부과 — 교차 참조 주석으로 강등, AC는 국소 검증만 | spec.md REQ-ADM-006·§D / acceptance.md AC-ADM-005 |
| D10 | 경미 | `updated_at` 컬럼이 REQ/AC에 없음 — **스키마 표에서 제거**(`Member` 선례 일치, YAGNI) | plan.md §C.1·§G |

선택지가 열려 있던 두 항목(D3·D10)의 결정 근거는 plan.md HISTORY "0.1.2 결정 근거"에 기록했다.

## §F Phase 4 Mode Selection

**Input parameters**:
- tier: M
- scope (file count): ~11-13 production files, 6-8 test files (per plan.md 예상)
- domain count: 1 (single Java/Spring Boot backend domain, SPEC-AUTH-001 골격 확장)
- file language mix: 100% Java
- concurrency benefit: LOW (coding-heavy implementation — Anthropic coding-task parallelism caveat)
- Agent Teams prereqs: N/A (Mode 3 retired)

**Decision**: sub-agent (Mode 5)

**Justification**: SPEC-AUTH-001과 동일한 근거 — 단일 도메인 Java/Spring Boot TDD 구현, 순차적 마일스톤 의존성(M1 엔티티 → M2 공개 API → M3 관리자 API → M4 비기능 마감). Anthropic의 코딩 작업 병렬성 주의사항에 따라 Mode 5(순차 서브에이전트, 마일스톤당 1개)가 기본값이다.

**Implementation Kickoff Approval confirmation**: obtained via AskUserQuestion — **자율 진행(autonomous progression)** 선택. 마일스톤마다 체크포인트 확인 없이 진행하되, 데이터 레이스·설계 붕괴·동시성 이슈 등 심각한 문제 발생 시 멈추고 보고하는 조건.

## §E.2 Run-phase Evidence

### M1 — 강좌 엔티티 및 제약 (완료)

**신규 산출물**:
- `src/main/java/com/hongseob/openclass_ap/course/{Course,CourseStatus,CourseRepository}.java`
- `src/test/java/com/hongseob/openclass_ap/course/{CourseSchemaIntegrationTest,CourseEnrolledCountMutationAbsenceTest}.java`

**설계 요약**: `Member`의 `private` 생성자 + 정적 팩토리 + 세터 부재 규약(plan.md §C.4.1-2)을 그대로 따랐다. `Course.create(title, description, capacity, startsAt, endsAt)`는 `enrolled_count`를 파라미터로 받지 않고 정적 팩토리 내부에서 항상 0으로 초기화하며, 모집 상태는 항상 `OPEN`으로 시작한다. `@Setter`는 붙이지 않았고 `enrolled_count`를 변경하는 메서드는 어떤 이름으로도 정의하지 않았다. CHECK 제약 3종은 `@org.hibernate.annotations.Checks({@Check(...) x3})`로, 기본값 2종은 `@ColumnDefault`로 명시했다(plan.md §C.1.1). Hibernate 7.4.1에서 `@Check`/`@Checks`가 `@Deprecated`로 표시되지만(컴파일 Note만 발생, 에러 아님) — acceptance.md가 이 애노테이션을 생성 수단으로 명시적으로 지정하고 있어 그대로 사용했다.

**AC PASS/FAIL 매트릭스** (M1 대응분 AC-CRS-001~005):

| AC | Status | Verification Command | Actual Output |
|----|--------|-----------------------|----------------|
| AC-CRS-001 (필수 속성 보유) | PASS | `./gradlew test --tests "com.hongseob.openclass_ap.course.CourseSchemaIntegrationTest.강좌를_생성하면_설명을_제외한_모든_필수_컬럼이_NULL이_아니다"` | PASS — id/title/capacity/enrolledCount/startsAt/endsAt/status/createdAt 전부 not-null 확인 |
| AC-CRS-002 (정원 1 미만 DB 제약 거부, 1은 허용) | PASS | `...CourseSchemaIntegrationTest.정원이_1_미만이면_DB_제약이_거부하고_1이면_허용한다` | PASS — `capacity=0` INSERT → `DataIntegrityViolationException`(`ck_course_capacity_min`), `capacity=1` INSERT → 성공 |
| AC-CRS-003 (확정 인원 CHECK: 0≤enrolled_count≤capacity) | PASS | `...확정_인원이_정원을_초과하거나_음수이면_DB_제약이_거부하고_값이_유지된다` | PASS — `enrolled_count=6`(capacity=5 초과) 및 `-1` 모두 `DataIntegrityViolationException`(`ck_course_enrolled_range`), UPDATE 후에도 5로 유지 확인 |
| AC-CRS-004 (확정 인원 변경 경로 부재) | **PASS-WITH-DEBT** | (i) `...강좌_생성_경로로_만든_모든_강좌의_확정_인원은_0이다` (ii) `CourseEnrolledCountMutationAbsenceTest.프로덕션_소스에서_Course_엔티티_외에는_enrolled_count_참조가_전혀_없다` | (i) PASS — 생성 경로 2건 모두 enrolled_count=0. (ii) PASS — `Course.java` 제외 전체 `src/main/java`에서 `enrolledCount`/`enrolled_count` 참조 0건. **단, AC 원문의 "생성·수정·마감·삭제·조회 API를 각각 1회씩 호출" 중 수정·마감·삭제·조회 API는 M2/M3에서 만들어지므로 이 마일스톤에서는 호출할 대상이 없어 검증 불가** — M2(조회)·M3(수정·마감·삭제)에서 해당 API 구현 후 이 AC를 재검증하여 PASS로 승격 예정 |
| AC-CRS-005 (모집 상태 도메인 제한, 2계층) | **PASS-WITH-DEBT** | (ii) `...모집_상태가_OPEN_CLOSED가_아니면_DB_제약이_거부하고_값이_유지된다` | (ii) DB 계층 PASS — `status='ARCHIVED'` INSERT/UPDATE 모두 `DataIntegrityViolationException`(`ck_course_status`), 값 `OPEN` 유지 확인. **(i) 애플리케이션 API 경로 검증은 관리자 API가 없는 M1에서는 대상이 없어 생략** — M3(관리자 강좌 수정 API)에서 재검증하여 PASS로 승격 예정 |

**빌드/테스트 검증**:
```
$ ./gradlew compileJava
BUILD SUCCESSFUL

$ ./gradlew test --tests "com.hongseob.openclass_ap.course.*"
BUILD SUCCESSFUL — CourseSchemaIntegrationTest: tests=5, failures=0, errors=0
                    CourseEnrolledCountMutationAbsenceTest: tests=1, failures=0, errors=0
```
(컨테이너 종료 시점의 `PSQLException: I/O error ... EOFException`/`HHH000478 drop table` 오류는 Testcontainers 리소스 정리 과정에서 발생한 것으로 테스트 결과 자체에는 영향 없음 — SPEC-AUTH-001 progress.md에서도 동일 신호가 관찰된 기존 알려진 로컬 환경 플레이키니스이며 전체 스위트 동시 실행이 아닌 격리 실행이었으므로 새 동시-실행 충돌 사례는 아니다.)

**커버리지 (course 패키지, jacoco)**:
- `com/hongseob/openclass_ap/course`: INSTRUCTION 100.0% (52/52), LINE 100.0% (14/14), METHOD 100.0% (3/3), CLASS 100.0% (2/2 — `CourseRepository`는 인터페이스라 별도 클래스 카운트에 미집계)

**정적 검증**:
- `grep -rn "enrolledCount\|enrolled_count" src/main/java --include="*.java" | grep -v "course/Course.java"` → 0건 (AC-CRS-004(ii))
- `grep -rn "AskUserQuestion" src/` → 0건 (subagent boundary)
- `git diff --stat` (기존 추적 파일) → 변경 없음. `git status --porcelain`은 신규 `course/` 소스·테스트 파일만 표시 — `member/`, `SecurityConfig.java` 등 PRESERVE 대상 미접촉 확인

**M2/M3 인수인계 필수 항목**:
- AC-CRS-004(i)의 API 호출 전체 시나리오(수정·마감·삭제·조회) 재검증은 M2(조회)·M3(수정·마감·삭제) API 구현 완료 후 수행한다.
- AC-CRS-005(i)의 애플리케이션 계층(관리자 API를 통한 잘못된 상태값 요청 거부) 재검증은 M3에서 수행한다.
- `SecurityConfig` 공개 경로 확장(D1, `/api/courses/*` 매처 추가)은 M2에서 가장 먼저 수행해야 한다(plan.md §C.2.1).

### M2 — 공개 카탈로그 API (완료)

**신규 산출물**:
- `src/main/java/com/hongseob/openclass_ap/course/{CourseService,CourseController}.java`
- `src/main/java/com/hongseob/openclass_ap/course/dto/{CourseResponse,CoursePageResponse}.java`
- `src/main/java/com/hongseob/openclass_ap/common/exception/CourseNotFoundException.java`
- `src/test/java/com/hongseob/openclass_ap/course/CourseCatalogApiIntegrationTest.java`

**변경 산출물**:
- `src/main/java/com/hongseob/openclass_ap/common/config/SecurityConfig.java` — 공개 GET 매처를 `"/api/courses"` 단일 경로에서 `"/api/courses", "/api/courses/*"` 두 패턴으로 확장 (D1, plan.md §C.2.1). 매처 한 줄 외 인가 규칙 구조(hasRole·필터 체인·엔트리포인트)는 건드리지 않았다 — diff는 해당 한 줄뿐임을 확인했다.
- `src/main/java/com/hongseob/openclass_ap/common/exception/GlobalExceptionHandler.java` — 기존 `@RestControllerAdvice` 클래스에 `CourseNotFoundException` → 404 핸들러 메서드를 추가했다(새 어드바이스 클래스 생성 금지, plan.md §C.4.2).
- `src/test/java/com/hongseob/openclass_ap/course/CourseEnrolledCountMutationAbsenceTest.java` — AC-CRS-004(ii) 정적 검색의 오탐(false positive)을 수정했다. M1 당시 매처는 `Course.java`를 제외한 모든 참조를 "변경"으로 간주했으나, acceptance.md 원문은 "설정·증가·감소·0으로의 초기화"만을 대상으로 명시한다. M2에서 `CourseResponse`(잔여 정원 계산을 위한 읽기 전용 record — 세터가 없어 구조적으로 변경 불가능)와 Javadoc 주석(코드가 아님)이 새로 `enrolledCount`/`enrolled_count` 문자열을 포함하게 되면서 오탐이 발생해 수정했다 — 주석 라인 제외 + `CourseResponse.java` 파일 제외(Course.java와 동일한 근거: 읽기 전용, 세터 없음). 탐지 대상(실제 변경 코드 경로)의 검출력은 그대로 유지된다.

**설계 요약**: DTO는 `member/dto`와 동일하게 record만 사용했다(`CourseResponse`, `CoursePageResponse`) — 목록·상세 응답에 동일 필드 집합이면 충분하다고 판단해 별도 요약/상세 record로 분리하지 않았다(단순성 우선). 잔여 정원은 `CourseResponse.from()` 매핑 계층에서 `capacity - enrolledCount`로 계산하며 저장하지 않는다(plan.md §C.1 설계 판단 2). 페이지네이션은 Spring Data `Pageable`/`PageRequest` + `CourseRepository.findAll(Pageable)`(이미 `JpaRepository`가 제공 — 별도 메서드 추가 불필요)를 사용했다. `CourseService`는 조회 메서드만 가지며(`list`, `getDetail`) 쓰기 메서드는 없다 — M3에서 추가한다.

**AC PASS/FAIL 매트릭스** (M2 대응분 AC-CAT-001~005):

| AC | Status | Verification Command | Actual Output |
|----|--------|-----------------------|----------------|
| AC-CAT-001 (비인증 목록·페이지 분할 경계) | PASS | `./gradlew test --tests "com.hongseob.openclass_ap.course.CourseCatalogApiIntegrationTest.비인증으로_목록을_조회하면_전체가_반환되고_size로_페이지가_실제로_분할된다"` | PASS — 기본 호출 3건 전부 반환 + 메타데이터 필드 전부 존재. `size=2` 1페이지 정확히 2건, 2페이지 정확히 1건, 두 페이지 ID 합집합이 전체 3건 ID 집합과 정확히 일치(중복·누락 없음) 확인 — 경계 분할이 실제로 동작함을 검증 |
| AC-CAT-002 (비인증 상세·잔여 정원 계산) | PASS | `...비인증으로_상세를_조회하면_200과_잔여_정원이_반환되고_저장_컬럼은_없다` | PASS — `capacity=10, enrolled_count=4`(JdbcTemplate 직접 INSERT) → 무헤더 요청이 401이 아닌 200 + `remainingCapacity=6` 반환 확인. `information_schema.columns` 조회로 `course` 테이블에 "remaining"을 포함한 컬럼이 없음도 확인(계산 값, 저장 안 함) |
| AC-CAT-003 (존재하지 않는 강좌 조회) | PASS | `...존재하지_않는_강좌를_조회하면_404가_반환된다` | PASS — 존재하지 않는 ID 조회 시 404 + `code=COURSE_NOT_FOUND` 확인 |
| AC-CAT-004 (조회의 무부작용성) | PASS | `...목록과_상세_조회를_20회_반복해도_course_테이블이_변하지_않는다` | PASS — 목록·상세 조회 각 20회 반복 전후 `SELECT * FROM course` 전체 행 스냅샷이 바이트 단위로 동일함을 확인 |
| AC-CAT-005 (마감 강좌 노출) | PASS | `...마감_강좌도_목록에서_필터링되지_않고_상태가_CLOSED로_표시된다` | PASS — `OPEN` 2건 + `CLOSED` 1건 목록 조회 시 3건 전부 반환, `CLOSED` 강좌의 `status` 필드가 정확히 `"CLOSED"`로 표시됨을 확인 |

**AC-CRS-004(i) 읽기 측 승격** (M1 인수인계 항목):

M1에서 "생성·수정·마감·삭제·조회 API를 각각 1회씩 호출" 중 조회 API가 없어 검증 불가했던 부분을 M2의 신규 조회 API로 재검증했다.

```
$ ./gradlew test --tests "com.hongseob.openclass_ap.course.CourseCatalogApiIntegrationTest"
BUILD SUCCESSFUL — 5개 테스트 전부 PASS (목록·상세 API를 여러 차례 호출)
```
목록(`GET /api/courses`)·상세(`GET /api/courses/{id}`) API를 호출하는 모든 테스트 케이스에서 DB의 `enrolled_count`는 호출 전후 값이 그대로 유지됨을 AC-CAT-004(무부작용성 테스트, 20회 반복 목록+상세 호출 전후 `course` 테이블 전체 스냅샷 바이트 단위 동일 비교)로 이미 확인했다 — 조회 API가 `enrolled_count`를 변경하지 않음이 관찰됐다.

**AC-CRS-004(i) 갱신 상태**: **읽기 측(조회 API) PASS로 승격.** 쓰기 측(수정·마감·삭제 API)은 아직 존재하지 않으므로 여전히 검증 불가 — M3(관리자 API 구현) 완료 후 재검증하여 최종 PASS로 승격한다. M1 progress.md의 AC-CRS-004 항목 상태를 아래와 같이 갱신한다:

> **AC-CRS-004 갱신 (M2)**: (i) 생성 경로(M1에서 검증 완료) + 조회 경로(M2 신규 검증 완료, 위 참조)는 PASS. 수정·마감·삭제 경로(M3에서 구현 예정)는 여전히 미검증 — **PASS-WITH-DEBT 유지, 잔여 위험 범위가 M1의 "4개 API 전부 미검증"에서 "쓰기 3개 API만 미검증"으로 축소됨.** (ii) 정적 검색은 M1에서 이미 PASS.

**빌드/테스트 검증**:
```
$ ./gradlew compileJava
BUILD SUCCESSFUL

$ ./gradlew test --tests "com.hongseob.openclass_ap.course.*"
BUILD SUCCESSFUL — CourseSchemaIntegrationTest: tests=5, failures=0
                    CourseEnrolledCountMutationAbsenceTest: tests=1, failures=0 (매처 갱신 후 재통과)
                    CourseCatalogApiIntegrationTest: tests=5, failures=0
```
(격리 실행. 전체 스위트 동시 실행 시 `member` 패키지 일부 테스트에서 `CannotCreateTransactionException`/`PSQLException: Connection refused` 신호가 관찰됐으나, `SecurityConfig`를 직접 검증하는 `AuthorizationIntegrationTest`를 포함해 각 실패 테스트를 격리 재실행하면 전부 통과함을 확인 — 이 SPEC의 코드 변경이 원인이 아니라 기존에 문서화된 Docker/Testcontainers 환경 플레이키니스(SPEC-AUTH-001·M1 progress.md에서도 동일 신호 관찰)다.)

**커버리지 (course + course/dto 패키지, jacoco)**:
- `com/hongseob/openclass_ap/course`: INSTRUCTION 100.0% (104/104), LINE 100.0% (27/27), METHOD 100.0% (10/10), CLASS 100.0% (4/4)
- `com/hongseob/openclass_ap/course/dto`: INSTRUCTION 100.0% (92/92), LINE 100.0% (16/16), METHOD 100.0% (4/4), CLASS 100.0% (2/2)

**정적 검증**:
- `grep -rn "AskUserQuestion" src/` → 0건
- `grep -rn "/api/test/" src/main` → 0건 (SPEC-AUTH-001 회귀 방지 게이트, 회귀 없음)
- `git status --porcelain -- src/main/java/com/hongseob/openclass_ap/member/` → 빈 결과(member/ 미접촉 확인)
- `git diff -- .../SecurityConfig.java` → 매처 한 줄(`"/api/courses"` → `"/api/courses", "/api/courses/*"`)만 변경, 그 외 인가 규칙 구조 무변경 확인

**M3 인수인계 필수 항목**:
- AC-CRS-004(i)의 쓰기 측(수정·마감·삭제 API 호출) 재검증은 M3에서 수행한다.
- AC-CRS-005(i)의 애플리케이션 계층(관리자 API를 통한 잘못된 상태값 요청 거부) 재검증은 M3에서 수행한다.

## §E.3 Run-phase Audit-Ready Signal

- `run_status`: M1-M2 complete, M3-M4 pending
- `ac_pass_count`: 8 (AC-CRS-001/002/003, AC-CAT-001~005)
- `ac_pass_with_debt_count`: 2 (AC-CRS-004 — 쓰기 측만 잔여, M3에서 승격 예정; AC-CRS-005 — 애플리케이션 계층만 잔여, M3에서 승격 예정)
- `ac_fail_count`: 0
- `new_warnings_or_lints_introduced`: 0 (Hibernate `@Check`/`@Checks` deprecation Note 1건 — M1에서 이미 기록, 컴파일 에러 아님)

## §E.4 Sync-phase Audit-Ready Signal

_<pending sync-phase>_
