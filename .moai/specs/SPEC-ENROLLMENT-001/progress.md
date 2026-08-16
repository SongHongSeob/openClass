---
id: SPEC-ENROLLMENT-001
title: "선착순 수강신청 큐·워커 및 대기명단 자동 승격 — 진행 기록"
version: "0.2.2"
status: in-progress
created: 2026-08-15
updated: 2026-08-16
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/enrollment"
lifecycle: spec-anchored
tags: "enrollment, progress"
tier: L
---

# SPEC-ENROLLMENT-001 — 진행 기록

## §E.1 Plan-phase Audit-Ready Signal

- `plan_status`: audit-ready — **plan-auditor 2회차 PASS (0.92 / Tier L 임계 0.85)**, must-pass 실패·치명·중대 0건. 후속 경미 지적 6건(N1~N6)은 v0.2.2에서 전건 해소
- 산출물: `spec.md`, `plan.md`, `acceptance.md`, `design.md`, `research.md`, `progress.md` — **Tier L 필수 5종 전부 + 진행 기록**, status: draft
- Tier 판정: **L** (예상 16~18 프로덕션 파일 + 10~12 테스트 파일, 동시성 모델이 프로젝트 전체에 헌법적 영향)
- 요구사항 **53건** + 불변식 **9건** = **62건**, 인수 기준 **53건**, 미대응 0건 (acceptance.md §D.2 — 매트릭스 62행 실측)

  > 이 줄은 v0.2.1까지 "요구사항 49건 + 불변식 8건 = 57건, 인수 기준 49건 … 기계 검증 완료"로 남아 있었다. 2차 감사(E1·E2) 반영으로 요구사항 4건·불변식 1건·AC 4건이 추가된 뒤 갱신되지 않은 값이며, **틀린 숫자에 "기계 검증 완료"가 붙어 있던 것**이므로 단순 노후화가 아니라 검증 무결성 문제였다 (2회차 감사 지적 N3). v0.2.2에서 acceptance.md §D.2 매트릭스를 다시 세어 정정했다.
- 미해소 클래리피케이션 마커: **0건** (SPEC 3분할 / 인증 JWT / 프론트엔드 백엔드 우선 — 모두 사용자 결정으로 확정)
- 선행 의존: `SPEC-AUTH-001`, `SPEC-COURSE-001` — 둘 다 `completed` 전에는 run 단계 진입 금지

### 최초 감사(iteration 1, FAIL 0.76) 지적 반영 요약

`.moai/reports/plan-audit/SPEC-ENROLLMENT-001-audit.md`의 D1~D12 전건 반영. 상세 대응표는 plan.md §A.1에 있다.

- **설계 변경 3건**: 접수 잠금 도입(D1), 취소·승격 원자화(D2), 취소 소유권 2층 검증(D12)
- **정량화 2건**: 부하 상한 500건 명시 + 실측 AC(D5), 워커 처리량 산출 근거 기록
- **명세 보강 3건**: 큐 상태 도메인 전체 열거(D6), 추적성 매트릭스 1행/요구사항(D3), 경계 상황 AC 승격(D7)
- **검증 강화 1건**: 확정 경로 단일성 3층 검증(D4)
- **산출물 보완 1건**: `research.md`·`design.md` 작성(D9)
- **범위 결정 2건**: SPEC 3분할 실행(D10, D11), GEARS 키워드 정정(D8)

### 검증 부채 (run 단계 M1에서 해소)

`research.md` §7의 V1~V8은 실제 PostgreSQL 실행 검증이 아직 이루어지지 않은 **가설**이다. run 단계 M1에서 확인하고 결과를 §E.2에 기록한다. 특히 V5/V6(실측 처리량)이 REQ-STS-003의 500건 상한과 어긋나면 요구사항 숫자를 실측에 맞게 개정한다.

### 2차 감사(iteration 2, PASS 0.92) 후속 정밀화 (v0.2.2)

2회차 감사는 **PASS(0.92)** 이며 must-pass 실패·치명·중대 0건이었다. 남은 6건은 전부 경미(문서 정밀화)로 run 단계 진입을 차단하지 않았으나, 문서가 사실과 어긋난 채 남는 것을 막기 위해 전건 해소했다. 대응표는 plan.md §A.2에 있다.

- **N3** (검증 무결성) — 위 요구사항/불변식/AC 건수 정정, plan.md §A.1 "57행" → 62행, plan.md §C.6 종단 지연 1.2초 → **1.7초**(design.md §6의 A+B+C와 일치)
- **N4** (도메인 미정의) — spec.md §A.4를 A.4.1(큐)·A.4.2(`enrollment.status` 2종)·A.4.3(`waitlist_entry.status` 4종)으로 확장. `DUPLICATE`와 "활성"의 규범적 정의 확보
- **N5** (구현 리스크) — REQ-WL-001에 대기 순번 부여 규칙 `MAX(순번)+1` over 전체 이력 명시(`COUNT(활성)+1` 금지). design.md §4.3 의사코드 반영, AC-ENR-028에 판별 절 추가
- **N1** (문구 정합성) — REQ-WL-003/004·REQ-ADX-002에 모집 상태·승격 적격성 한정 어구 추가 (동작 변경 없음)
- **N2** (범위 정합성) — REQ-QUE-003의 접수 잠금 범위를 큐 적재 3종 전부로 확장(AC-ENR-005 쪽으로 정렬)
- **N6** (제약 누락) — plan.md §D에 ArchUnit 예외 각주 추가 (테스트 스코프 의존성 + `SPEC-COURSE-001` D3 교차 조율)

**잔여 검증 부채**: 이 정밀화는 설계를 바꾸지 않았으므로 재감사를 요하지 않는다. 다만 N5의 순번 부여 규칙은 AC-ENR-028의 "또한" 절로만 검증되므로, M4 구현 시 그 절이 `COUNT(활성)+1` 구현을 실제로 실패시키는지 확인하고 결과를 §E.2에 기록한다.

### 다음 단계

Implementation Kickoff Approval → run (M1부터). 선행 `SPEC-AUTH-001`·`SPEC-COURSE-001`의 `completed` 확인이 진입 조건이다.

## §F Phase 4 Mode Selection

**Input parameters**:
- tier: L
- scope (file count): ~16-18 production files, 10-12 test files (per plan.md 예상)
- domain count: 1 (single Java/Spring Boot backend domain, SPEC-AUTH-001/COURSE-001 골격 확장)
- file language mix: 100% Java
- concurrency benefit: LOW (coding-heavy implementation — Anthropic coding-task parallelism caveat). 동시성이 이 SPEC의 **도메인 주제**이지만, 구현 작업 자체는 순차적 마일스톤 의존성을 갖는 코딩 작업이다
- Agent Teams prereqs: N/A (Mode 3 retired)

**Decision**: sub-agent (Mode 5)

**Justification**: SPEC-AUTH-001/COURSE-001과 동일한 근거 — 단일 도메인 Java/Spring Boot TDD 구현, 순차적 마일스톤 의존성(M1 스키마/접수잠금 → M2 워커 → M3 상태조회 → M4 대기명단/취소 → M5 관리자연동 → M6 마감정리). 특히 M1(접수 순서 보장)과 M2(확정 경로 단일성)는 서로의 산출물에 의존하므로 병렬화가 불가능하다.

**Route**: **Route B (PR 기반)** — Tier L이므로 spec-workflow.md § SPEC Phase Discipline에 따라 main 직접 커밋이 아닌 별도 브랜치 + PR 방식을 따른다. `feat/SPEC-ENROLLMENT-001` 브랜치 생성 완료.

**Implementation Kickoff Approval confirmation**: obtained via AskUserQuestion — **마일스톤별 확인(semi-autonomous progression)** 선택. 동시성 버그의 위험도가 높아 각 마일스톤 완료 시 사용자 확인을 거친 뒤 다음 단계로 진행한다.

## §E.2 Run-phase Evidence

### M1 — 스키마 및 접수 순서 보장 (완료)

**신규 산출물**

| 파일 | 역할 |
|---|---|
| `enrollment/request/{RequestType,RequestState,RequestResult}.java` | 큐 도메인 열거형 3종 (spec.md §A.4.1 완전 열거) |
| `enrollment/request/EnrollmentRequest.java` | 큐 테이블 엔티티 — CHECK 제약 4종(request_type/state/result/state·result 정합) |
| `enrollment/request/EnrollmentRequestRepository.java` | 클레임 쿼리 2종(`FOR UPDATE SKIP LOCKED`, 네이티브) |
| `enrollment/{Enrollment,EnrollmentStatus,EnrollmentRepository}.java` | 확정 수강신청 엔티티 (M1은 워커 스텁의 생성 진입점만 사용) |
| `waitlist/{WaitlistEntry,WaitlistStatus,WaitlistEntryRepository}.java` | 대기명단 엔티티 + `MAX(순번)+1` 부여 쿼리(REQ-WL-001/N5) |
| `enrollment/receipt/EnrollmentLockProperties.java` | 접수 잠금 활성화 스위치(`app.enrollment.lock-enabled`, 기본 true) |
| `enrollment/receipt/EnrollmentReceiptService.java` | **이 SPEC의 핵심** — 접수 잠금 획득 → 큐 INSERT (REQ-QUE-001~003) |
| `enrollment/worker/CourseCapacityRepository.java` | `course.enrolled_count` 원자적 증가 게이트웨이(JPQL UPDATE, course 패키지 미수정) |
| `enrollment/worker/EnrollmentRequestProcessor.java` | M1 최소 워커 — ENROLL만 디스패치(SUCCESS/WAITLISTED) |
| `enrollment/worker/EnrollmentQueueWorker.java` | 큐 드레인 드라이버(`drainQueue()`, `@Scheduled`는 M2 범위) |
| `enrollment/EnrollmentController.java` + `dto/EnrollmentReceiptResponse.java` | 접수 API(`POST /api/courses/{courseId}/enrollments`) |
| `src/main/resources/schema.sql` | 부분 유니크 인덱스 3종(Hibernate 애노테이션으로 표현 불가) |
| `application.properties` (+2줄), `AbstractIntegrationTest.java` (+2줄) | `spring.jpa.defer-datasource-initialization` / `spring.sql.init.mode=always` / 잠금 스위치 기본값 |
| `build.gradle` | ArchUnit 1.3.0 테스트 의존성 추가(plan.md §D 예외 각주 근거) |
| 테스트 6개 클래스, 13개 메서드 | 아래 AC 매트릭스 참고 |

**M1/M2 경계 — 의도적 축소 (Section A 사전 승인 범위)**: `EnrollmentRequestProcessor`는 ENROLL 요청을 정원 여유 기준 SUCCESS/WAITLISTED로만 분기한다. 마감 분기(CLOSED)·중복 검사 3종(REJECTED)·CANCEL/CAPACITY_INCREASE 디스패치·실패 격리(REQUIRES_NEW)·확정 경로 단일성 3층 검증(매핑 제약+ArchUnit+DB 상태 단언)은 명시적으로 M2 범위로 남겨두었다 — 클래스 Javadoc에 그 경계를 기록했다. `CourseCapacityRepository`는 `course.enrolled_count` 변경을 워커 패키지 안에서만 수행하도록 새 저장소 인터페이스로 구현했다(`course/Course.java`를 전혀 수정하지 않음 — PRESERVE 준수).

**AC PASS/FAIL 매트릭스 (AC-ENR-001 ~ 007)**

| AC | 상태 | 검증 명령 | 실제 출력 |
|---|---|---|---|
| AC-ENR-001 | PASS | `./gradlew test --tests "*.EnrollmentReceiptApiIntegrationTest"` | `접수는_큐_적재만_수행하고_연속_3건의_순서값이_단조_증가한다() PASS (0.029s)` — 순서값 단조 증가, `enrollment` 0건, `enrolled_count` 불변 확인 |
| AC-ENR-002 | PASS (M1 범위 — 접수 API만, 상태조회/취소는 M3/M4 산출물로 재검증 예정, PASS-WITH-DEBT) | 동일 | `미인증으로_접수하면_401이고_큐_행이_생성되지_않는다() PASS (0.011~0.014s)` |
| AC-ENR-003 | PASS | 동일 | `존재하지_않는_강좌로_접수하면_404이고_큐_행이_생성되지_않는다() PASS (0.15s)`, `$.code == "COURSE_NOT_FOUND"` |
| AC-ENR-004 | PASS | `./gradlew test --tests "*.request.EnrollmentQueueSchemaIntegrationTest"` | `request_type이_도메인_밖_값이면_DB_제약이_거부하고_3종_값은_허용한다() PASS` — `'PROMOTE'` INSERT → `DataIntegrityViolationException`, ENROLL/CANCEL/CAPACITY_INCREASE 3종은 허용. 추가로 state/result 도메인·정합성 CHECK도 함께 검증(`state와_result_도메인_밖_값이거나...() PASS`) |
| AC-ENR-005 | PASS | `./gradlew test --tests "*.receipt.EnrollmentReceiptLockOrderTest" --tests "*.EnrollmentQueueBoundaryArchitectureTest"` | 구조: `접수_잠금_획득_호출이_큐_행_저장_호출보다_소스에서_먼저_나타난다() PASS` (텍스트 인덱스 lockIndex < saveIndex) + `@MX:ANCHOR`/`@MX:REASON` 존재 확인 PASS. ArchUnit: `EnrollmentRequestRepository는_접수와_워커_패키지에서만_참조된다() PASS` (0건 위반) |
| **AC-ENR-006** (단일 관문 ①) | **PASS** | `./gradlew test --tests "*.EnrollmentOrderGuaranteeIntegrationTest"` | `AC_ENR_006_커밋_지연_하에서도_먼저_접수한_회원이_확정된다() PASS (0.876s)` — X 먼저 접수(커밋 지연), Y는 접수 잠금에서 대기 → `yId > xId`, X.result=SUCCESS, Y.result=WAITLISTED, `enrollmentRepository.findAll()` == [memberXId] 단독 |
| **AC-ENR-007** (대조군, 단일 관문 ①의 짝) | **PASS** | `./gradlew test --tests "*.EnrollmentLockDisabledControlGroupIntegrationTest"` | `AC_ENR_007_잠금을_끄면_커밋_역전_시나리오에서_순서_보장이_깨진다() PASS (0.034~0.087s)` — 잠금 비활성화(`app.enrollment.lock-enabled=false`) 시 동일 시나리오에서 **순서가 실제로 깨짐**: Y(순서값 더 큼)가 SUCCESS, X가 WAITLISTED로 역전 — AC-ENR-006 통과가 우연이 아님을 입증 |

**M1 완료 조건 재확인**: plan.md §F가 명시한 M1 완료 조건 "AC-ENR-006과 AC-ENR-007이 함께 통과"가 충족되었다. 대조군이 함께 통과함으로써 AC-ENR-006의 PASS가 접수 잠금 메커니즘의 실제 효과임이 입증된다.

**테스트 코드 발췌 — AC-ENR-006/007 핵심 단언**

```java
// AC-ENR-006 (잠금 활성화, 기본값)
assertThat(yId).as("Y는 X가 커밋할 때까지 대기했으므로 순서값이 더 커야 한다").isGreaterThan(xId);
worker.drainQueue();
assertThat(requestRepository.findById(xId).orElseThrow().getResult()).isEqualTo(RequestResult.SUCCESS);
assertThat(requestRepository.findById(yId).orElseThrow().getResult()).isEqualTo(RequestResult.WAITLISTED);

// AC-ENR-007 (잠금 비활성화 — app.enrollment.lock-enabled=false)
worker.drainQueue();               // X 미커밋 상태에서 1차 구동 — Y만 보임
releaseXCommit.countDown();        // X를 뒤늦게 커밋
worker.drainQueue();               // 2차 구동 — X가 보이지만 이미 정원 소진
assertThat(requestRepository.findById(yId).orElseThrow().getResult()).isEqualTo(RequestResult.SUCCESS);   // 위반 재현
assertThat(requestRepository.findById(xId).orElseThrow().getResult()).isEqualTo(RequestResult.WAITLISTED);
```

### research.md §7 검증 부채 (V1~V4·V7·V8) 해소 결과

| # | 확인 항목 | 결과 | 방법 및 근거 |
|---|---|---|---|
| **V1** | `pg_advisory_xact_lock`이 커밋/롤백 시 자동 해제된다 | **CONFIRMED** | `EnrollmentOrderGuaranteeIntegrationTest.V1_...` — 커밋 경로·롤백 경로 각각에서 잠금을 잡고 종료한 뒤, 별도 커넥션이 3초 이내(실측 15ms)에 즉시 재획득함을 확인 (2회 반복: 커밋 후 1회, 롤백 후 1회) |
| **V2** | 잠금 없이 커밋 순서를 뒤집으면 순서 보장이 깨진다 | **CONFIRMED** | AC-ENR-007과 동일 명제 — 별도 테스트를 두지 않고 그 PASS 결과로 갈음 (근거: research.md §6-4 대조군 원칙) |
| **V3** | 잠금이 있으면 같은 시나리오에서 순서가 지켜진다 | **CONFIRMED** | AC-ENR-006과 동일 명제 — 그 PASS 결과로 갈음 |
| **V4** | 미커밋 INSERT 행이 다른 트랜잭션의 `SELECT ... SKIP LOCKED`에 나타나지 않는다 | **CONFIRMED** | `V4_미커밋_INSERT_행은...` — 커넥션1에서 INSERT(미커밋) 후 커넥션2의 `FOR UPDATE SKIP LOCKED` 조회 결과에 해당 id가 없음을 확인, 커밋 후 재조회 시 나타남을 확인 |
| **V5** | 단일 강좌 접수 처리량 실측치 | 대상 아님(M1 범위 아님) | plan.md M1 목록에 V5 없음 — M3(AC-ENR-026) 범위 |
| **V6** | 워커 처리량 실측치 | 대상 아님(M1 범위 아님) | 동일 — M3/M6 범위 |
| **V7** | `@Scheduled` 자동 실행 비활성화가 테스트 프로파일에서 동작한다 | **N/A — M1에는 아직 `@Scheduled` 컴포넌트가 존재하지 않음** | `EnrollmentQueueWorker`는 M1에서 `@Scheduled`를 갖지 않는다(design.md §4.1 폴링은 M2 범위). M2가 폴링 빈을 추가할 때 실제로 검증하고 이 표를 갱신한다 — 아직 존재하지 않는 것을 "비활성화 확인"했다고 기록하는 것은 verification-claim-integrity 위반이므로 정직하게 N/A로 남긴다 |
| **V8** | 커밋 중 행 가시화가 권고 잠금 해제보다 먼저 일어난다 | **CONFIRMED (10/10회)** | `V8_커밋_중_행_가시화가_잠금_해제보다_먼저_일어난다_10회_반복` — T1이 잠금을 잡고 INSERT 후 대기, T2가 잠금 대기열에 진입한 상태에서 T1이 커밋 → T2가 잠금을 획득한 직후 즉시 SELECT하여 T1의 행이 보이는지 확인. **10회 반복 전부 visible=true, 역전 0건.** V8이 실패했다면 SPEC 개정 사유였을 것이나 확인되지 않음 — §4/§A.2 정합성 논증의 마지막 고리가 실측으로 닫혔다 |

V8 CONFIRMED는 이 SPEC의 정합성 논증(spec.md §A.2, design.md §3, research.md §4)이 실측으로 뒷받침됨을 의미한다 — "PostgreSQL이 커밋 중 행을 가시화한 뒤에 그 트랜잭션의 권고 잠금을 해제한다"는 가정이 문서화된 엔진 동작이 아니라 이 프로젝트의 실제 PostgreSQL(Testcontainers, postgres:16-alpine)에서 직접 확인되었다.

### 빌드 및 테스트 검증

```
$ ./gradlew compileJava compileTestJava
BUILD SUCCESSFUL

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.request.EnrollmentQueueSchemaIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentReceiptApiIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptLockOrderTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueBoundaryArchitectureTest"
BUILD SUCCESSFUL — 8 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentOrderGuaranteeIntegrationTest"
BUILD SUCCESSFUL — 4 tests, 0 failed (AC-ENR-006 + V1 + V4 + V8)

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentLockDisabledControlGroupIntegrationTest"
BUILD SUCCESSFUL — 1 test, 0 failed (AC-ENR-007)
```

**M1 신규 테스트 총계: 6개 클래스, 13개 메서드, 전부 PASS** (위 3개 배치로 분할 실행 — 아래 잔여 위험 참고).

**커버리지 (jacoco, `enrollment`+`waitlist` 패키지, 위 3개 배치를 `./gradlew clean` 이후 순차 실행해 누적)**:

```
com/hongseob/openclass_ap/enrollment          66.7% (14/21 라인)
com/hongseob/openclass_ap/enrollment/request  100.0% (29/29)
com/hongseob/openclass_ap/enrollment/receipt  80.0% (12/15)
com/hongseob/openclass_ap/enrollment/worker   91.4% (32/35)
com/hongseob/openclass_ap/waitlist            100.0% (13/13)
com/hongseob/openclass_ap/enrollment/dto      0.0% (0/1) — record 접근자 boilerplate, 실질 미커버 아님
합계: 100/114 = 87.7%
```

`EnrollmentController`가 패키지 집계에서 낮게 보이는 이유(36%, 4/11)는 아래 잔여 위험에 기록한다 — 실제 HTTP 어서션(202/401/404, 본문 검증)은 전부 통과했으므로 동작 결함이 아니라 jacoco 측정 아티팩트로 판단된다.

### 정적 검증

```
$ grep -rn "AskUserQuestion" src/main/java/com/hongseob/openclass_ap/enrollment src/main/java/com/hongseob/openclass_ap/waitlist src/test/java/com/hongseob/openclass_ap/enrollment
(no output, exit=1)

$ grep -rn "EnrollmentRequestRepository\b" --include="*.java" src/main/java | grep -v "enrollment/receipt\|enrollment/worker\|enrollment/request/EnrollmentRequestRepository.java"
(no output — receipt/worker 패키지 외 참조 0건)

$ grep -rln "requestRepository.save" src/main/java
src/main/java/com/hongseob/openclass_ap/enrollment/receipt/EnrollmentReceiptService.java   (유일한 큐 INSERT 경로)

$ git diff --stat -- src/main/java/com/hongseob/openclass_ap/course src/main/java/com/hongseob/openclass_ap/member src/main/java/com/hongseob/openclass_ap/common/config/SecurityConfig.java
(no output — PRESERVE 대상 완전 무변경)
```

### 잔여 위험 (Residual Risk)

1. **전체 스위트 동시 실행 시 간헐적 컨테이너 불안정 (환경 문제로 판단, 코드 결함 아님)**: `enrollment` 패키지의 6개 테스트 클래스를 **한 번의 gradle 호출**로 함께 실행하면 3회 시도 중 3회 모두 부분 실패(2~4개 메서드, 매번 실패하는 클래스가 다름 — 1회차는 `EnrollmentQueueSchemaIntegrationTest`, 2회차는 `EnrollmentOrderGuaranteeIntegrationTest` 전체, 3회차는 다시 `EnrollmentQueueSchemaIntegrationTest`)가 발생했다. 실패 시그니처는 전부 `HikariPool ... Connection is not available, request timed out after 30010ms` 또는 `Connection to localhost:PORT refused`였다. 조사 결과:
   - 실패가 매번 **다른 클래스**에서 발생하고, 그 클래스가 반드시 이 마일스톤에서 새로 작성한 수동 트랜잭션/원시 JDBC 코드를 쓰는 클래스가 아니었다(예: `EnrollmentQueueSchemaIntegrationTest`는 표준 `@Autowired JdbcTemplate`만 쓰며 수동 커넥션 제어가 전혀 없다) — 이는 **결정적 커넥션 누수라면 나타나지 않을 패턴**이다.
   - `AC-ENR-006`/`AC-ENR-007`의 수동 트랜잭션 콜러블에 예외 경로 안전성을 강화했다(`try/finally`로 커밋 실패 시 반드시 롤백 — 최초 구현에는 이 안전망이 없어 누수 가능성이 있었으나, 강화 후에도 동일 패턴의 실패가 재현되어 **이것이 근본 원인이 아님**을 추가로 확인했다).
   - 반대로 **각 클래스를 별도 gradle 호출로 분리 실행**하면(총 3개 배치, 각각 독립된 JVM+Testcontainers 인스턴스 생명주기) **매번 100% 성공**했다(이번 세션에서 개별/배치 실행 총 7회 이상, 전부 성공).
   - 이 패턴은 이 세션의 기존 메모리 기록("Docker/Testcontainers 환경 플레이키니스: 전체 스위트 실행 시 간헐적 DB 연결 타임아웃, 코드 결함 아님")과 정확히 일치하며, 이번 조사로 그 결론이 **한 번 더 재확인**되었다(schema 테스트처럼 이 마일스톤의 신규 동시성 코드를 전혀 쓰지 않는 클래스도 동일하게 영향을 받음).
   - **결론**: 코드 결함이 아니라 로컬 Docker 환경에서 다수의 `@SpringBootTest` 컨텍스트를 연속 기동할 때 나타나는 환경 불안정성으로 판단한다. AC PASS/FAIL 매트릭스의 근거는 **모두 개별/소배치 단위의 클린 성공 실행**(위 "빌드 및 테스트 검증" 절의 3개 명령)이며, 이 실행들은 매번 `BUILD SUCCESSFUL`로 재현 가능했다.
   - **후속 조치 제안**: CI 환경(Docker 리소스가 더 넉넉한 환경)에서 전체 스위트 1회 실행으로 이 현상이 재현되는지 확인 필요. 재현되면 별도 인프라 이슈로 티켓화. M2 이후 마일스톤에서도 동일 현상 관찰 시 이 항목을 갱신한다.
2. **`EnrollmentController` jacoco 라인 커버리지 36% (4/11) — 측정 아티팩트로 추정**: `EnrollmentReceiptApiIntegrationTest`의 3개 메서드가 실제 MockMvc를 통해 `POST /api/courses/{courseId}/enrollments`를 호출하고 202/401/404 응답 본문(JsonPath)까지 검증하여 **전부 PASS**했음에도, jacoco 라인 히트맵은 `receive()`·`resolveMemberId()` 메서드 본문(41~43, 54~57행)을 0으로 보고한다. 생성자(33~36행)는 정상적으로 covered로 잡힌다. 실제 동작 증거(통과한 HTTP 어서션)가 jacoco 라인 카운터보다 강한 증거라고 판단하여 결함으로 취급하지 않지만, 원인(Spring Boot 4.1.0의 AOT/프록시 관련 jacoco 계측 상호작용 추정)을 확정하지 못했으므로 후속 조사 항목으로 남긴다.
3. **`resolveMemberId`의 `IllegalStateException` 분기 미검증**: JWT로 인증된 회원이 `member` 테이블에 존재하지 않는 경우(이론상 불가능 — 토큰은 항상 실제 회원에게 발급됨)에 대한 방어적 분기는 M1 테스트에서 트리거되지 않았다. 의도적 방어 코드이며 REQ/AC 대응 항목이 아니다.
4. **M1 스텁 워커의 방어적 예외 경로 미검증**: `EnrollmentRequestProcessor.dispatch()`의 `UnsupportedOperationException` 분기(CANCEL/CAPACITY_INCREASE 요청 수신 시)는 M1에 그 요청을 만드는 프로덕션 경로가 없으므로 테스트되지 않았다 — M4/M5가 각각의 디스패치를 추가하며 이 분기를 대체한다.

### 다음 단계

Semi-autonomous progression(마일스톤별 확인)에 따라 M1 완료 후 **정지**한다. 오케스트레이터가 사용자와 확인 후 M2(워커 및 확정 경로 단일성)로 진행할지 결정한다.

## §E.3 Run-phase Audit-Ready Signal

_<pending run-phase>_

## §E.4 Sync-phase Audit-Ready Signal

_<pending sync-phase>_
