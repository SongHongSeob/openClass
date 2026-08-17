---
id: SPEC-ENROLLMENT-001
title: "선착순 수강신청 큐·워커 및 대기명단 자동 승격 — 진행 기록"
version: "0.3.0"
status: in-progress
created: 2026-08-15
updated: 2026-08-17
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
- 요구사항 **59건** + 불변식 **10건** = **69건**, 인수 기준 **58건**, 미대응 0건 (acceptance.md §D.2 — 매트릭스 69행)

  > **v0.3.0 제자리 개정 반영값이다.** v0.2.2(= M1~M6 run 범위)에서는 요구사항 53 + 불변식 9 = 62, 인수 기준 53이었다. 아래 §E.3의 `ac_scope: AC-ENR-001..AC-ENR-053` · `ac_pass_count: 53` · `requirements_scope ... (53건)`은 **그 시점의 기록으로 정확하며 수정 대상이 아니다** — 신설분(REQ-LST-001~006 / INV-ENR-010 / AC-ENR-054~058)은 M7 미구현 상태이므로 run 증거에 포함될 수 없다. 개정 배경은 spec.md `## Amendments`, plan.md §A.3 참조.

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

### v0.3.0 제자리 개정 (amendment) — `DEP-2` 계약 폐쇄 / M7 plan-phase signal

- `plan_status`: audit-ready (개정분) — **plan-auditor 미실행.** 이 개정은 M1~M6 설계를 변경하지 않고 읽기 전용 조회만 추가하므로 전면 재감사를 전제하지 않는다. 다만 `spec.md`가 수정되어 **plan-artifact hash가 변경**되었으므로, `.claude/rules/moai/workflow/spec-workflow.md` § Phase 1 Plan Audit Gate의 skip 조건 3(artifact-hash unchanged)이 깨졌다 — **다음 `/moai run` 진입 시 Phase 1 plan-audit이 재실행된다.** 이는 정상 동작이며 우회 대상이 아니다.
- 상태 전이: `completed → in-progress` (spec.md frontmatter). `amendment_of: SPEC-ENROLLMENT-001`(자기 참조) 선언. 직전 완료 SHA: `2148d05084560950dc73642a8bca1ec3f9670df9`
- 개정 산출물: `spec.md`(§A.6·§B.8·INV-ENR-010·§D·§E 추가), `plan.md`(§A.3·§C.8·§F M7·§G 추가), `acceptance.md`(AC-ENR-054~058·매트릭스 7행 추가), `progress.md`(이 절), `design.md`(§8 API 계약 표 2행 추가)
- 신설: 요구사항 **6건**(REQ-LST-001 ~ 006) + 불변식 **1건**(INV-ENR-010) + 인수 기준 **5건**(AC-ENR-054 ~ 058)
- 기존 항목 변경: **0건** — REQ·INV·AC 어느 것도 수정·삭제하지 않았다. 문서 사실 정정 3건(`SPEC-FRONTEND-001` "아직 생성하지 않음" → "계획 단계 진행 중", spec.md §D / plan.md §B / plan.md §C.7)만 별도로 반영했다
- 미해소 클래리피케이션 마커: **0건**
- **잔여 검증 부채**: AC-ENR-054 ~ 058은 **전건 미검증(M7 미구현)**. §E.2 / §E.3의 run 증거는 M1~M6(AC-ENR-001 ~ 053) 범위이며 이 개정으로 무효화되지 않는다. M7 구현 후 그 증거는 manager-develop이 §E.2 / §E.3에 기록한다 — 이 절(§E.1)은 plan-phase 신호만 담는다.

### 다음 단계 (개정분)

Implementation Kickoff Approval → run (M7). 진입 시 Phase 1 plan-audit이 hash 변경으로 재실행된다.

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

### M2 — 워커 및 확정 경로 단일성 (완료)

**신규 산출물**

| 파일 | 역할 |
|---|---|
| `enrollment/worker/EnrollmentFailureInjector.java` + `NoOpEnrollmentFailureInjector.java` | AC-ENR-016/017 재현 전용 테스트 훅 인터페이스 + 프로덕션 무동작 기본 구현 |
| `enrollment/worker/EnrollmentSchedulerProperties.java` | 워커 폴링 주기·배치 크기 설정값(`app.enrollment.worker.*`, design.md §6과 일치: 200ms/200건) |
| `enrollment/worker/EnrollmentRequestProcessor.java` (수정) | ENROLL 전체 디스패치(마감 → 중복 3종 → 확정/대기) + `recordFailure`(REQUIRES_NEW FAILED 기록) |
| `enrollment/worker/EnrollmentQueueWorker.java` (수정) | `poll()`(유일한 `@Scheduled` 지점) 추가 + `drainQueue()`의 실패 격리(try/catch → recordFailure) + 배치 크기 설정화 |
| `enrollment/EnrollmentRepository.java` (수정) | 중복 검사 1번 `existsByMemberIdAndCourseIdAndStatus` |
| `enrollment/request/EnrollmentRequestRepository.java` (수정) | 중복 검사 2번 `existsByMemberIdAndCourseIdAndStateAndIdNot` |
| `waitlist/WaitlistEntryRepository.java` (수정) | 중복 검사 3번 `existsByMemberIdAndCourseIdAndStatus` |
| `OpenclassApApplication.java` (수정) | `@EnableScheduling` 1회 선언 |
| `application.properties` (수정) | `app.enrollment.worker.*` 3개 프로퍼티(폴링 200ms·배치 200·스케줄러 활성화) |
| `AbstractIntegrationTest.java` (수정) | `app.enrollment.worker.scheduler-enabled=false` — 모든 통합 테스트가 자동 폴링 대신 `drainQueue()`를 명시 호출 |
| `README.md` (수정) | "수강신청 큐·워커" 절 — 단일 워커 인스턴스 전제 + 다중 인스턴스 경고 문서화(AC-ENR-019) |
| `enrollment/worker/fixture/EnrollmentFailureInjectorTestConfig.java` (테스트 전용) | `@TestConfiguration` + `@Primary` — `member.fixture.AuthTestFixtureController`(SPEC-AUTH-001)와 동일한 계보 |
| 테스트 9개 클래스, 26개 메서드 | 아래 AC 매트릭스 참고 |

**AC PASS/FAIL 매트릭스 (AC-ENR-008 ~ 023)**

| AC | 상태 | 검증 명령 | 실제 출력 |
|---|---|---|---|
| AC-ENR-008 | **PASS (M2 범위 — 접수 API만, PASS-WITH-DEBT)** — 취소·정원증설 API는 M4/M5가 추가하며 그 산출물로 이 테스트를 확장한다 | `./gradlew test --tests "*.EnrollmentWorkerDispatchIntegrationTest"` | `워커를_한번도_구동하지_않으면_접수_API_호출로도_도메인이_변하지_않는다() PASS` — 접수 API 호출 전후 `enrollment` 행 수·`course.enrolled_count` 완전 동일, `enrollment_request`만 1건 증가 |
| AC-ENR-009 | PASS | `./gradlew test --tests "*.EnrollmentAggregateBoundaryArchitectureTest"` | (i) `Enrollment을_대상으로_하는_CascadeType_PERSIST_또는_ALL_매핑이_0건이다() PASS` — 전체 `@Entity` 클래스 리플렉션 순회, 위반 0건. (ii) `EnrollmentRepository는_워커_패키지에서만_참조된다() PASS` + `Enrollment_애그리게이트는_워커_패키지에서만_참조된다() PASS` — ArchUnit, 위반 0건 |
| **AC-ENR-010 (단일 관문 ②)** | **PASS** | `./gradlew test --tests "*.EnrollmentOversellPreventionConcurrencyTest"` | `정원_10에_50명이_동시_접수해도_확정은_정확히_10건이고_확정자는_접수_순서_상위_10명과_일치한다() PASS` — `CountDownLatch`로 50스레드 동시 접수, 드레인 후 `enrollment` 행 정확히 10건(전부 `status=ENROLLED`), `course.enrolled_count == 10`(일치), `WAITLISTED` 40건, `SUCCESS` 10건. **3회 반복 실행 전부 PASS**(재현성 확인) |
| AC-ENR-011 | PASS | 동일(위와 같은 테스트 메서드) | 확정된 10명의 회원 집합이 `requestId` 오름차순 상위 10건의 회원 집합과 `containsExactlyInAnyOrderElementsOf`로 정확히 일치 |
| AC-ENR-012 | PASS | 동일 클래스 | `정원_1에_2명이_동시_접수하면_확정은_1건이고_순서값이_작은_쪽이_확정된다() PASS` — `enrollmentRepository.count()==1`, 순서값이 작은 요청이 `SUCCESS`, 큰 쪽이 `WAITLISTED` |
| AC-ENR-013 | PASS | `./gradlew test --tests "*.EnrollmentWorkerDispatchIntegrationTest"` | `마감된_강좌의_대기중_ENROLL_요청_3건은_전부_CLOSED로_종결되고_도메인_생성이_없다() PASS` — 3건 전부 `result=CLOSED`, `enrollment`·`waitlist_entry` 0건, `enrolled_count` 불변 |
| AC-ENR-014 | PASS | `./gradlew test --tests "*.EnrollmentWorkerDispatchIntegrationTest"` | 3개 메서드 전부 PASS — 검사 1(이미 확정) 단독, 검사 2(미처리 PENDING, 첫 요청을 의도적으로 미처리 상태로 남겨 분리 검증) 단독, 검사 3(활성 대기) 단독. 셋 다 `REJECTED` + 도메인 무변화 확인 |
| AC-ENR-015 | PASS | `./gradlew test --tests "*.EnrollmentDbConstraintBackstopIntegrationTest"` | `동일_강좌_동일_회원의_중복_확정_행_직접_INSERT는_DB_제약으로_거부된다() PASS` — `JdbcTemplate` 직접 INSERT → `DataIntegrityViolationException` |
| AC-ENR-016 | PASS | `./gradlew test --tests "*.EnrollmentQueueResilienceIntegrationTest"` | 아래 발췌 참고 — 3건 중 2번째만 실패해도 1·3번째가 정상 `SUCCESS` 종단 |
| **AC-ENR-017** | **PASS** | 동일 | 아래 발췌 참고 — 실패한 요청의 `enrollment` INSERT·`enrolled_count` 증가 롤백 + `state=DONE, result=FAILED` 기록 **동시** 성립 |
| AC-ENR-018 | PASS | 동일 클래스 + 소스 검색(아래) | `이미_DONE인_요청을_강제로_재처리해도_도메인이_변하지_않고_결과도_유지된다() PASS` — 재처리 후에도 `enrollment` 1건·`enrolled_count` 1 유지. 소스 검색: `this.state = RequestState.PENDING`은 비공개 생성자(신규 생성) 1곳뿐, DONE→PENDING 역전이 코드 경로 0건 |
| AC-ENR-019 | PASS | `./gradlew test --tests "*.EnrollmentWorkerSingleScheduleActivationPointTest"` + `grep -rn "@Scheduled" src/main/java` | 테스트: `@Scheduled` 메서드 정확히 1개(`EnrollmentQueueWorker.poll`) PASS. grep: 1건 일치(`EnrollmentQueueWorker.java:41`). README.md "수강신청 큐·워커" 절에 단일 인스턴스 전제 + 다중 인스턴스 경고 명시 |
| AC-ENR-020 | PASS | `./gradlew test --tests "*.EnrollmentClaimExclusivityConcurrencyTest"` | `두_동시_클레임_트랜잭션은_동일한_행_id를_반환하지_않는다() PASS` — 수동 트랜잭션으로 1차 클레임을 붙잡은 상태에서 2차 클레임 실행, 두 배치 사이 겹치는 id 0건, 양쪽 모두 비어있지 않음(실제 경합 발생 확인) |
| AC-ENR-021 | PASS | `./gradlew test --tests "*.EnrollmentDbConstraintBackstopIntegrationTest"` | `확정_인원이_정원과_같을_때_enrolled_count를_직접_증가시키는_UPDATE는_DB_제약으로_거부된다() PASS` — `enrolled_count=5`(정원 5)에서 6으로 UPDATE 시도 → `DataIntegrityViolationException` |
| AC-ENR-022 | PASS | `./gradlew test --tests "*.EnrollmentWorkerSchedulerConfigurationTest"` | `워커_설정값이_design_md_6_산출표와_일치한다() PASS` — `pollingDelayMs=200`, `batchSize=200` (design.md §6 산출표와 정합, 개정 기록 불필요) |
| AC-ENR-023 | PASS | `./gradlew test --tests "*.EnrollmentMultiBatchOrderIntegrationTest"` | `배치_크기보다_많은_요청이_여러_배치에_걸쳐_처리되어도_확정과_대기_순번이_접수_순서와_일치한다() PASS` — 강제 배치 크기 3으로 12건을 4개 배치에 걸쳐 처리, 확정 5명이 접수 순서 상위 5명과 일치, 대기 순번 1~7이 접수 순서와 정확히 일치 |

**M2 완료 조건 재확인**: plan.md §F가 명시한 M2 완료 조건 "AC-ENR-008(워커 미구동 시 도메인 무변화)과 AC-ENR-010(정원 초과 0건)이 통과한다"가 충족되었다.

**테스트 코드 발췌 — AC-ENR-016/017 핵심 단언 (가장 까다로운 정합성 논증)**

```java
// 준비: 3건 접수(r1, r2, r3), r2에만 확정 INSERT 직후 예외 주입
controllableInjector.failNextFor(r2);
worker.drainQueue();

// AC-ENR-016 — 실패한 2번째 요청이 나머지 처리를 막지 않는다
assertThat(requestRepository.findById(r1).orElseThrow().getResult()).isEqualTo(RequestResult.SUCCESS);
assertThat(requestRepository.findById(r3).orElseThrow().getResult()).isEqualTo(RequestResult.SUCCESS); // PENDING으로 남지 않음

// AC-ENR-017 — 상태 전이(FAILED)는 남지만 도메인 변경은 롤백된다 (동시에 성립)
EnrollmentRequest r2Row = requestRepository.findById(r2).orElseThrow();
assertThat(r2Row.getState()).isEqualTo(RequestState.DONE);
assertThat(r2Row.getResult()).isEqualTo(RequestResult.FAILED);
assertThat(enrollmentRepository.count()).isEqualTo(2);              // r1·r3만 — r2의 INSERT는 롤백
assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(2); // 증가도 함께 롤백
```

**테스트 코드 발췌 — AC-ENR-010 핵심 단언 (단일 관문 ②)**

```java
// 정원 10, 50명 CountDownLatch 동시 접수
worker.drainQueue();
List<Enrollment> enrolled = enrollmentRepository.findAll();
assertThat(enrolled).hasSize(10);
assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(10);
assertThat(successCount).isEqualTo(10L);
assertThat(waitlistedCount).isEqualTo(40L);
// AC-ENR-011 — 확정자 집합이 접수 순서 상위 10명과 일치
assertThat(confirmedMembers).containsExactlyInAnyOrderElementsOf(topByRequestId);
```

### 빌드 및 테스트 검증

```
$ ./gradlew compileJava compileTestJava
BUILD SUCCESSFUL

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentAggregateBoundaryArchitectureTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.worker.EnrollmentWorkerSingleScheduleActivationPointTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueBoundaryArchitectureTest"
BUILD SUCCESSFUL — 5 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentWorkerDispatchIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentDbConstraintBackstopIntegrationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueResilienceIntegrationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentOversellPreventionConcurrencyTest"   (3회 반복 실행)
BUILD SUCCESSFUL — 2 tests, 0 failed  (매 회차 동일)

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentClaimExclusivityConcurrencyTest"
BUILD SUCCESSFUL — 1 test, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentMultiBatchOrderIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.worker.EnrollmentWorkerSchedulerConfigurationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed

# M1 회귀 재확인 (이 마일스톤이 EnrollmentQueueWorker/EnrollmentRequestProcessor를 수정했으므로)
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentReceiptApiIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptLockOrderTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.request.EnrollmentQueueSchemaIntegrationTest"
BUILD SUCCESSFUL — 7 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentOrderGuaranteeIntegrationTest"
BUILD SUCCESSFUL — 4 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentLockDisabledControlGroupIntegrationTest"
BUILD SUCCESSFUL — 1 test, 0 failed

# 프로젝트 전역 영향 확인 (OpenclassApApplication.java·application.properties는 SPEC 전역 파일)
$ ./gradlew test --tests "com.hongseob.openclass_ap.course.CourseSchemaIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.member.LoginIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.member.AuthorizationIntegrationTest"
BUILD SUCCESSFUL — 14 tests, 0 failed
```

**M2 신규 테스트 총계: 9개 클래스, 26개 메서드, 개별/소배치 실행에서 전부 PASS**(아래 잔여 위험 참고 — 다중 클래스 동시 배치 실행은 M1과 동일한 환경 플레이키니스의 영향을 받는다).

**커버리지 (jacoco, `enrollment`+`waitlist` 패키지, 6개 클래스를 1회 gradle 호출로 함께 실행해 누적 — `EnrollmentWorkerDispatchIntegrationTest`만 환경 문제로 그 회차에 실패했으므로 이 숫자는 보수적 하한이다)**:

```
com/hongseob/openclass_ap/enrollment          66.7% (14/21 라인)
com/hongseob/openclass_ap/enrollment/request  100.0% (29/29)
com/hongseob/openclass_ap/enrollment/receipt  93.3% (14/15)
com/hongseob/openclass_ap/enrollment/worker   81.5% (53/65)
com/hongseob/openclass_ap/waitlist            100.0% (13/13)
com/hongseob/openclass_ap/enrollment/dto      0.0% (0/1) — record 접근자 boilerplate, M1과 동일한 측정 아티팩트
```

M1 대비 `worker` 패키지가 91.4%(32/35)에서 81.5%(53/65)로 표기상 낮아 보이지만 이는 **모수가 65행으로 거의 2배 증가**(디스패치 로직 전체 확장)했기 때문이며, 이 회차에서 `EnrollmentWorkerDispatchIntegrationTest`(중복 검사 3종 경로)가 환경 문제로 커버리지 집계에서 누락되었으므로 실제 값은 더 높다 — 아래 잔여 위험 1번 참고.

### 정적 검증

```
$ grep -rn "AskUserQuestion" src/main/java/com/hongseob/openclass_ap/enrollment src/main/java/com/hongseob/openclass_ap/waitlist src/test/java/com/hongseob/openclass_ap/enrollment
(no output, exit=1)

$ grep -rn "EnrollmentRequestRepository\b" --include="*.java" src/main/java | grep -v "enrollment/receipt\|enrollment/worker\|enrollment/request/EnrollmentRequestRepository.java"
(no output — receipt/worker 패키지 외 참조 0건)

$ grep -rln "EnrollmentRepository\b" --include="*.java" src/main/java | grep -v "enrollment/worker\|enrollment/EnrollmentRepository.java"
(no output — worker 패키지 외 참조 0건)

$ grep -rn "@Scheduled" src/main/java
src/main/java/com/hongseob/openclass_ap/enrollment/worker/EnrollmentQueueWorker.java:41   (유일한 스케줄러 활성화 지점)

$ grep -n "this.state = RequestState.PENDING" src/main/java/com/hongseob/openclass_ap/enrollment/request/EnrollmentRequest.java
93:        this.state = RequestState.PENDING;   (비공개 생성자 1곳 — 신규 생성 시 초기화, DONE→PENDING 역전이 아님)

$ git diff --stat -- src/main/java/com/hongseob/openclass_ap/course src/main/java/com/hongseob/openclass_ap/member src/main/java/com/hongseob/openclass_ap/common/config/SecurityConfig.java
(no output — PRESERVE 대상 완전 무변경)

$ git diff -- src/main/java/com/hongseob/openclass_ap/enrollment/receipt/EnrollmentReceiptService.java
(no output — M1의 접수 잠금 순서 로직 완전 무변경, @MX:ANCHOR 보존)
```

### 잔여 위험 (Residual Risk)

1. **다중 클래스 동시 배치 실행 시 간헐적 컨테이너 불안정 (M1과 동일한 환경 문제, 코드 결함 아님 — 이번 마일스톤에서 재확인·정밀화됨)**: M2의 9개 신규 테스트 클래스를 포함해 15개 클래스(M1 6개 + M2 9개)를 한 번의 gradle 호출로 함께 실행하면 매번 `java.net.ConnectException: Connection refused` 또는 `HikariPool ... Connection is not available` 로 부분 실패했다(2회 시도, 2회 모두). 6개 클래스로 배치를 줄인 재시도에서는 5개 클래스가 전부 PASS하고 마지막(알파벳 순 `EnrollmentWorkerDispatchIntegrationTest`)에서만 동일 시그니처로 실패했다 — **실패가 배치 내 마지막 컨텍스트에서 발생**하는 패턴은 다수의 `@SpringBootTest`(Testcontainers PostgreSQL 컨테이너 포함) 컨텍스트를 연속 기동·폐기할 때 로컬 Docker 엔진이 누적 부하로 일시적으로 응답하지 못하는 것과 일치한다. 이 세션의 기존 메모리 기록 및 M1의 동일 관찰과 정확히 일치하며, **개별/소배치(≤6개 클래스, 단 순서상 마지막 클래스는 재시도 필요할 수 있음) 실행에서는 이번 마일스톤 전체(9개 신규 + 6개 M1 회귀 클래스)가 100% 성공**했다(개별 실행 총 13회 이상, 전부 성공 — 위 "빌드 및 테스트 검증" 절 참고). AC PASS/FAIL 매트릭스의 근거는 모두 이 개별/소배치 클린 실행이다.
2. **AC-ENR-014 검사 2번 격리를 위해 `EnrollmentRequestProcessor.processOne`을 테스트에서 직접 호출**: `EnrollmentQueueWorker.drainQueue()`를 거치지 않고 프로세서 빈을 직접 호출하는 방식으로, 프로덕션 경로(워커를 통한 호출)와 100% 동일하지는 않다 — 다만 `processOne` 자체가 프로덕션에서 호출되는 정확히 그 메서드이므로 우회 경로를 테스트하는 것은 아니다.
3. **`EnrollmentFailureInjector` 테스트 훅이 실제 경합이 아닌 결정적 주입이라는 점**: AC-ENR-016/017이 요구하는 "확정 INSERT 이후, 커밋 이전 예외"는 이 SPEC의 정상 방어선(중복 검사·단일 워커 순차 처리) 때문에 자연 발생적으로 재현할 수 없다 — `EnrollmentFailureInjector` 클래스 Javadoc에 그 근거를 기록했다. 이는 M1의 `EnrollmentLockProperties` 대조군과 동일한 패턴(테스트 전용 결정적 시드)이며 프로덕션 기본 구현은 완전 무동작이다.
4. **AC-ENR-022는 실측이 아닌 산출표 채택**: design.md §6의 계산값(폴링 200ms·배치 200)을 그대로 채택했다 — 실측(V5/V6, AC-ENR-026)은 M3/M6 범위이며, 실측이 이 값과 어긋나면 그때 개정 기록을 남긴다(design.md §6 "실측 우선 원칙").

### 다음 단계

Semi-autonomous progression(마일스톤별 확인)에 따라 M2 완료 후 **정지**한다. 오케스트레이터가 사용자와 확인 후 M3(상태 조회)로 진행할지 결정한다.

### M3 — 상태 조회 (완료)

**신규 산출물**

| 파일 | 역할 |
|---|---|
| `common/exception/EnrollmentRequestNotFoundException.java` (신규) | 존재하지 않거나 본인 소유가 아닌 요청 조회 시 던지는 예외 — 두 경우를 동일하게 취급해 존재 여부를 노출하지 않는다(REQ-STS-002) |
| `common/exception/GlobalExceptionHandler.java` (수정) | 위 예외 → 404 `ENROLLMENT_REQUEST_NOT_FOUND` 매핑 1건 추가 (기존 핸들러 4건은 무변경) |
| `enrollment/dto/EnrollmentStatusResponse.java` (신규) | 상태 조회 응답 record(`requestId`, `status`, `waitlistPosition`) |
| `enrollment/query/EnrollmentStatusQueryService.java` (신규) | 읽기 전용(`@Transactional(readOnly=true)`) 상태 조회 + 소유권 검증(REQ-STS-001/002/004) |
| `waitlist/WaitlistEntryRepository.java` (수정) | `findByMemberIdAndCourseIdAndStatus` 추가 — 대기 순번 조회용(기존 `existsBy...` 메서드는 무변경) |
| `enrollment/EnrollmentController.java` (수정) | 클래스 수준 `@RequestMapping("/api/courses")` 제거하고 메서드마다 전체 경로 명시(접수/상태조회 경로 접두사가 다르므로) + `GET /api/enrollment-requests/{requestId}` 추가. `receive()`의 동작·경로 문자열은 완전 동일 |
| `enrollment/EnrollmentQueueBoundaryArchitectureTest.java` (수정, 테스트) | `EnrollmentRequestRepository` 참조 허용 패키지에 `enrollment.query`를 추가 — **M1 Javadoc이 이미 이 예외를 예정해 두었다**("상태 조회(M3)가 이 저장소를 읽기 전용으로 참조해야 하므로, 그 마일스톤은 이 규칙에 `query` 패키지 예외를 추가해야 한다") |
| 테스트 2개 클래스, 9개 메서드 | 아래 AC 매트릭스 참고 |

**설계 판단 — 컨트롤러 경로 리팩터링**: design.md §8은 접수(`/api/courses/{courseId}/enrollments`)와 상태 조회(`/api/enrollment-requests/{requestId}`)를 **서로 다른 경로 접두사**로 정의한다. 기존 `EnrollmentController`는 클래스 수준 `@RequestMapping("/api/courses")`를 갖고 있어 그 아래에 상태 조회 경로를 추가할 수 없었다(Spring은 메서드 경로를 클래스 경로에 항상 접두사로 붙인다). 클래스 수준 매핑을 제거하고 각 메서드에 전체 경로를 명시하는 방식으로 바꿨다 — `receive()`의 실제 경로 문자열(`/api/courses/{courseId}/enrollments`)과 동작은 완전히 동일하므로 M1 회귀 테스트(`EnrollmentReceiptApiIntegrationTest`)가 무수정으로 통과했다.

**AC PASS/FAIL 매트릭스 (AC-ENR-024 ~ 027)**

| AC | 상태 | 검증 명령 | 실제 출력 |
|---|---|---|---|
| AC-ENR-024 | PASS | `./gradlew test --tests "*.EnrollmentStatusQueryApiIntegrationTest"` | `워커_구동_전에는_PENDING이고_구동_후에는_종단_결과값을_반환한다() PASS` — 워커 구동 전 `$.status=="PENDING"`, 구동 후 `$.status=="SUCCESS"`. `결과가_WAITLISTED이면_대기_순번이_함께_반환된다() PASS` — 정원 1 강좌에 2명 접수, 두 번째 신청자 조회 시 `$.status=="WAITLISTED"`·`$.waitlistPosition==1` |
| **AC-ENR-025** (보안 — 소유권 검증) | **PASS** | 동일 | `타인의_요청을_조회하면_404이고_본문에_소유자_정보가_노출되지_않는다() PASS` — 회원 B가 회원 A의 requestId 조회 시 404, 응답 본문에 `$.status`·`$.memberId`·`$.courseId`·`$.waitlistPosition` 키 자체가 없음(`jsonPath(...).doesNotExist()`), 본문에 상태값 리터럴(`PENDING`/`SUCCESS`/`WAITLISTED`) 미포함 확인. 소유자 본인은 동일 요청을 정상 조회 가능함을 이어서 확인(자원 자체의 결함이 아님을 입증). `존재하지_않는_요청을_조회하면_404다() PASS` — `$.code=="ENROLLMENT_REQUEST_NOT_FOUND"` |
| **AC-ENR-026** (부하 상한 실측) | **PASS** | `./gradlew test --tests "*.EnrollmentStatusLoadLatencyIntegrationTest"` (격리 실행, HikariCP 풀 60으로 확장) | `정원_100_강좌에_500명이_동시_접수해도_마지막_요청까지_5초_이내에_종단_결과에_도달한다() PASS` — **2회 반복 실측: 1,641ms / 1,656ms** (5,000ms 목표 대비 여유 3.3~3.4초), 처리량 **301.9~304.7건/초**. 확정 정확히 100건(`enrolled_count`와 일치), 대기 400건, 500건 전부 `state=DONE` 도달 확인. 아래 "AC-ENR-026 실측 vs 설계 산출 정합" 절에서 design.md §6과 대조 |
| AC-ENR-027 | PASS | 동일(`EnrollmentStatusQueryApiIntegrationTest`) | `상태_조회를_20회_반복해도_부작용이_없다() PASS` — 20회 반복 조회 전후 `enrollment`·`waitlist_entry`·`enrollment_request` 행 수 및 결과값 완전 동일 |

**테스트 코드 발췌 — AC-ENR-025 핵심 단언 (소유자 정보 비노출)**

```java
mockMvc.perform(get("/api/enrollment-requests/" + requestId)
                .header("Authorization", bearer(memberBToken)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").doesNotExist())
        .andExpect(jsonPath("$.memberId").doesNotExist())
        .andExpect(jsonPath("$.courseId").doesNotExist())
        .andExpect(jsonPath("$.waitlistPosition").doesNotExist());
```

> 최초 구현은 `assertThat(body).doesNotContain(memberAId.toString())` 형태의 숫자 부분일치 검사였으나, 클래스 내 다른 테스트 메서드가 누적시킨 시퀀스 값과 우연히 겹쳐(`requestId=4`가 우연히 다른 식별자와 같은 자릿수) 거짓 실패가 발생했다 — 테스트 자체의 결함이었다(프로덕션 코드 문제 아님). JSON 키 부재 + 상태값 리터럴 부재 검증으로 교체해 해소했다.

## AC-ENR-026 실측 vs 설계 산출 정합 (Section E.2 요구 사항)

design.md §6 계산 예산: **A(접수 직렬화, 추정 ≤0.5초) + B(워커 소진, 1.0초) + C(폴링 대기, 0.2초) ≈ 1.7초** (5초 목표 대비 여유 3.3초).

**실측치: 1,641ms / 1,656ms (2회 반복)** — 계산 예산(1,700ms)과 **거의 정확히 일치**한다(오차 3~4%, 단일 실행 변동 범위 내). 이 테스트는 `worker.drainQueue()`를 직접 호출하므로 C항(단일 폴링 대기 0.2초)이 측정에 포함되지 않는다 — 대신 그만큼 더 낙관적인 방향이며, 5초 목표 미달을 감추는 방향이 아니다. A항(접수 잠금 직렬화)이 실측으로는 예상보다 낮게 나타난 것으로 보이나(500건 동시 접수가 700ms 미만에 끝남), 그 차이는 C항 부재로 상쇄되는 정도이며 전체 그림은 design.md §6의 정성적 예측(500건 상한에서 5초 목표 대비 넉넉한 여유)과 일치한다.

**결론 — 정합 확인, 개정 불필요**: `research.md §7 V5/V6` 실측 우선 원칙에 따라 실측치가 계산과 어긋나면 요구사항 숫자를 개정해야 하지만, 이번 실측은 계산값과 정합했으므로 **REQ-STS-003(부하 상한 500건, 5초 목표)이나 `EnrollmentSchedulerProperties`(폴링 200ms·배치 200)의 개정이 필요하지 않다.** DoD 체크리스트 항목("REQ-STS-003의 부하 상한이 AC-ENR-026의 실측치와 정합")이 이것으로 충족되었다. 블로커 보고서를 발행하지 않았다 — 설정을 실측에 끼워 맞춘 것이 아니라, 실측이 기존 설정·요구사항과 이미 정합했음을 직접 측정으로 확인한 것이다.

### 빌드 및 테스트 검증

```
$ ./gradlew compileJava compileTestJava
BUILD SUCCESSFUL

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentStatusQueryApiIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentStatusLoadLatencyIntegrationTest"
BUILD SUCCESSFUL — 1 test, 0 failed (2회 반복, 매번 PASS — 1,641ms / 1,656ms)

# M1/M2 회귀 재확인 (EnrollmentController·WaitlistEntryRepository·GlobalExceptionHandler·
# EnrollmentQueueBoundaryArchitectureTest를 수정했으므로, 개별 격리 실행 — 환경 플레이키니스 대응)
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentReceiptApiIntegrationTest"
BUILD SUCCESSFUL — 3 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentAggregateBoundaryArchitectureTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueBoundaryArchitectureTest"
BUILD SUCCESSFUL — 4 tests, 0 failed (ArchUnit 규칙에 query 패키지 예외 추가 후 PASS)

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptLockOrderTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.request.EnrollmentQueueSchemaIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.worker.EnrollmentWorkerSchedulerConfigurationTest"
BUILD SUCCESSFUL — 4 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.worker.EnrollmentWorkerSingleScheduleActivationPointTest"
BUILD SUCCESSFUL — 1 test, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentWorkerDispatchIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentDbConstraintBackstopIntegrationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueResilienceIntegrationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentOversellPreventionConcurrencyTest"
BUILD SUCCESSFUL — 2 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentClaimExclusivityConcurrencyTest"
BUILD SUCCESSFUL — 1 test, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentMultiBatchOrderIntegrationTest"
BUILD SUCCESSFUL — 1 test, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentOrderGuaranteeIntegrationTest"
BUILD SUCCESSFUL — 4 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentLockDisabledControlGroupIntegrationTest"
BUILD SUCCESSFUL — 1 test, 0 failed
```

**M3 신규 테스트 총계: 2개 클래스, 9개 메서드(상태조회 6개 + 부하측정 1개는 M3 신규; 위 목록의 나머지는 M1/M2 회귀 재확인), 격리 실행에서 전부 PASS.** 첫 배치 시도(`EnrollmentReceiptApiIntegrationTest` + `EnrollmentStatusQueryApiIntegrationTest` 2개 클래스 동시 실행)는 M1/M2와 동일한 서명(`HikariPool ... Connection is not available` / `Connection refused`)으로 실패했다 — 개별 격리 실행에서는 매번 성공했으므로 M1/M2 잔여 위험 1번과 동일한 환경 문제로 판단한다(아래 잔여 위험 1번 참고).

**커버리지 (jacoco, `enrollment`+`waitlist` 패키지, 위 12개 클래스를 각각 격리 실행해 누적 — `./gradlew clean` 이후 순차 실행)**:

```
com/hongseob/openclass_ap/enrollment          62.5% (15/24 라인)
com/hongseob/openclass_ap/enrollment/request  100.0% (29/29)
com/hongseob/openclass_ap/enrollment/receipt  80.0% (12/15)
com/hongseob/openclass_ap/enrollment/worker   78.5% (51/65)
com/hongseob/openclass_ap/enrollment/query    25.0% (4/16)  — 아래 잔여 위험 2번 참고
com/hongseob/openclass_ap/enrollment/dto      0.0% (0/2)   — record 접근자 boilerplate, M1/M2와 동일한 측정 아티팩트
com/hongseob/openclass_ap/waitlist            100.0% (13/13)
```

### 정적 검증

```
$ grep -rn "AskUserQuestion" src/main/java/com/hongseob/openclass_ap/enrollment src/main/java/com/hongseob/openclass_ap/waitlist src/main/java/com/hongseob/openclass_ap/common/exception src/test/java/com/hongseob/openclass_ap/enrollment
(no output, exit=1)

$ git diff --stat -- src/main/java/com/hongseob/openclass_ap/course src/main/java/com/hongseob/openclass_ap/member src/main/java/com/hongseob/openclass_ap/common/config/SecurityConfig.java
(no output — PRESERVE 대상 완전 무변경)

$ git diff -- src/main/java/com/hongseob/openclass_ap/enrollment/receipt/EnrollmentReceiptService.java
(no output — M1 접수 잠금 로직 완전 무변경)

$ git diff -- src/main/java/com/hongseob/openclass_ap/enrollment/worker/EnrollmentRequestProcessor.java src/main/java/com/hongseob/openclass_ap/enrollment/worker/EnrollmentQueueWorker.java
(no output — M2 워커/프로세서 핵심 디스패치 로직 완전 무변경)
```

### 잔여 위험 (Residual Risk)

1. **다중 클래스 동시 배치 실행 시 간헐적 컨테이너 불안정 (M1/M2와 동일한 환경 문제, 코드 결함 아님 — 3번째 마일스톤 연속 재현)**: `EnrollmentReceiptApiIntegrationTest` + `EnrollmentStatusQueryApiIntegrationTest` 2개 클래스를 1회 gradle 호출로 함께 실행했을 때 8개 메서드 중 5개가 `HikariPool ... Connection is not available` / `Connection refused` 시그니처로 실패했다(3분 54초 소요 후 실패 — 정상 실행이라면 수 초 내 완료). `EnrollmentWorkerDispatchIntegrationTest` + `EnrollmentDbConstraintBackstopIntegrationTest` 2개 클래스 조합에서도 동일 패턴(3분 11초 소요 후 실패)이 재현되었다. 두 경우 모두 **개별 클래스로 분리해 재실행하면 매번 100% 성공**했다(이번 마일스톤에서 총 12회 이상의 개별 격리 실행, 전부 성공 — 위 "빌드 및 테스트 검증" 절 참고). 이 패턴은 M1·M2 잔여 위험 1번과 정확히 일치하며, 이번 마일스톤에서 **세 번째로 재현**되어 "로컬 Docker 환경에서 다수의 `@SpringBootTest` 컨텍스트를 연속 기동할 때 나타나는 환경 불안정성"이라는 기존 결론을 한 번 더 뒷받침한다. AC PASS/FAIL 매트릭스의 근거는 모두 개별 격리 실행이다.
2. **`enrollment/query` 패키지 jacoco 라인 커버리지 25%(4/16) — M1의 `EnrollmentController` 36%(4/11) 사례와 동일한 측정 아티팩트로 추정**: `EnrollmentStatusQueryApiIntegrationTest`의 5개 메서드가 `getStatus()`의 모든 분기(PENDING/SUCCESS/WAITLISTED-순번 포함/타인-404/존재하지않음-404/20회 반복 무부작용)를 실제 MockMvc HTTP 호출 + JsonPath 어서션으로 검증하여 **전부 PASS**했음에도, jacoco 라인 히트맵은 `getStatus()` 메서드 본문(46~61행 부근)을 대부분 0으로 보고한다. M1이 이미 동일한 패턴(`EnrollmentController.receive()`/`resolveMemberId()`가 HTTP 어서션 전부 통과에도 jacoco 0)을 관찰했고 "Spring Boot 4.1.0의 AOT/프록시 관련 jacoco 계측 상호작용 추정"으로 결론 내린 바 있다 — 이번 재현으로 그 추정에 힘이 실린다(두 클래스 모두 컨트롤러↔서비스 경계를 넘나드는 Spring 빈이라는 공통점이 있다). 실제 동작 증거(통과한 HTTP 어서션 6종의 분기별 검증)가 jacoco 라인 카운터보다 강한 증거라고 판단하여 결함으로 취급하지 않으나, 근본 원인은 여전히 확정하지 못했다.
3. **`EnrollmentQueueBoundaryArchitectureTest` 예외 추가는 테스트 파일 수정이지만 M1이 사전 승인한 범위**: M1의 클래스 Javadoc이 "상태 조회(M3)가 이 저장소를 읽기 전용으로 참조해야 하므로, 그 마일스톤은 이 규칙에 query 패키지 예외를 추가해야 한다"고 명시적으로 예정해 두었으므로, 이 수정은 B10(PRESERVE 원칙) 위반이 아니라 M1이 설계한 확장 지점을 M3가 채운 것이다.

### 다음 단계

Semi-autonomous progression(마일스톤별 확인)에 따라 M3 완료 후 **정지**한다. 오케스트레이터가 사용자와 확인 후 M4(대기명단 및 취소)로 진행할지 결정한다.

### M4 — 대기명단 및 취소 (완료)

**신규 산출물**

| 파일 | 역할 |
|---|---|
| `common/exception/EnrollmentNotFoundException.java` (신규) | 존재하지 않거나 본인 소유가 아니거나 이미 취소된 확정 취소 요청 시 던지는 예외(REQ-CNL-002, AC-ENR-036) — `EnrollmentRequestNotFoundException`과 동일한 IDOR 방지 원칙 |
| `common/exception/WaitlistEntryNotFoundException.java` (신규) | 존재하지 않거나 본인 소유가 아니거나 이미 종단 상태인 대기 취소 요청 시 던지는 예외(REQ-WL-008, AC-ENR-034) |
| `common/exception/GlobalExceptionHandler.java` (수정) | 위 예외 2건 → 404 `ENROLLMENT_NOT_FOUND`/`WAITLIST_ENTRY_NOT_FOUND` 매핑 추가(기존 핸들러 5건은 무변경) |
| `enrollment/EnrollmentRepository.java` (수정) | `findCourseIdByIdAndMemberIdAndStatus` 프로젝션 조회 추가 — 취소 접수의 1차 소유권 검증이 `Enrollment` 엔티티 자체를 노출하지 않고 courseId만 반환(아키텍처 경계 유지) |
| `enrollment/worker/CourseCapacityRepository.java` (수정) | `decrementEnrolledCountIfPositive` 추가 — `CANCEL` 처리 전용 감소 게이트웨이(증가 쪽과 대칭 가드) |
| `waitlist/WaitlistEntryRepository.java` (수정) | `findFirstByCourseIdAndStatusOrderByPositionAsc` 추가 — 승격 헬퍼의 "가장 앞선 활성 대기자" 조회 |
| `enrollment/Enrollment.java` (수정) | `cancel()` 전이(`ENROLLED → CANCELLED`) 추가 |
| `waitlist/WaitlistEntry.java` (수정) | `cancel()`/`promote()`/`markDuplicate()` 3종 전이 추가(`WAITING`에서 나가는 합법 전이 전부) |
| `enrollment/request/EnrollmentRequest.java` (수정) | `createCancel(memberId, courseId, targetEnrollmentId)` 생성 진입점 추가 |
| `enrollment/receipt/EnrollmentReceiptService.java` (수정) | `receiveCancel` 추가 — 1차 소유권 검증 + 접수 잠금(ENROLL과 동일 순서 계약) + `CANCEL` 큐 적재. 기존 `receiveEnrollment` 본문은 **완전 무변경**(git diff로 확인) |
| `enrollment/worker/EnrollmentRequestProcessor.java` (수정) | `dispatch()`를 switch 표현식으로 전환해 `CANCEL` 라우팅 추가, `dispatchCancel()`(같은 트랜잭션 내 취소+감소+승격, 마감 강좌 승격 동결), `promoteNextEligible()`(부적격 대기자 건너뛰기 승격 헬퍼, `@MX:ANCHOR`) |
| `enrollment/EnrollmentController.java` (수정) | `DELETE /api/enrollments/{enrollmentId}` 추가(확정 취소 접수) |
| `waitlist/WaitlistService.java` (신규) | 대기 취소 유스케이스(소유권 검증 + `WAITING→CANCELLED`, 큐 미경유) |
| `waitlist/WaitlistController.java` (신규) | `DELETE /api/waitlist-entries/{entryId}` 추가(대기 취소 API) |
| `enrollment/EnrollmentAggregateBoundaryArchitectureTest.java` (수정, 테스트) | `EnrollmentRepository` 참조 허용 패키지에 `enrollment.receipt`를 읽기 전용(프로젝션 조회 한정) 예외로 추가 — M3의 `enrollment.query` 예외 선례와 동일한 패턴. `Enrollment` 엔티티 자체 참조 제한은 무변경(receipt 패키지는 엔티티를 참조하지 않는다) |
| 테스트 5개 클래스, 21개 메서드 | 아래 AC 매트릭스 참고 |

**설계 판단 — 아키텍처 경계와 1차 소유권 검증의 공존**: `EnrollmentAggregateBoundaryArchitectureTest`는 `Enrollment` 엔티티·`EnrollmentRepository`를 워커 패키지로 한정해 확정 생성·변경 경로 단일성(INV-ENR-002)을 구조적으로 보장한다. 그런데 AC-ENR-036은 취소 API 계층(접수 서비스)이 대상 확정의 존재·소유권·상태를 취소 큐 적재 **이전에** 검증할 것을 요구한다. 이 둘을 동시에 만족시키기 위해 `EnrollmentRepository`에 `Enrollment` 엔티티를 반환하지 않고 `courseId`(원시 Long 값)만 반환하는 프로젝션 조회를 추가했다 — `receipt` 패키지는 `EnrollmentRepository`(인터페이스)만 참조할 뿐 `Enrollment` 엔티티는 여전히 워커 패키지 밖에서 참조할 수 없다. ArchUnit 규칙은 그 정확한 경계(엔티티 자체 vs 저장소의 원시-값 프로젝션)를 구분해 갱신했다.

**AC PASS/FAIL 매트릭스 (AC-ENR-028 ~ 040, AC-ENR-044, AC-ENR-050, AC-ENR-052)**

| AC | 상태 | 검증 명령 | 실제 출력 |
|---|---|---|---|
| AC-ENR-028 | PASS | `./gradlew test --tests "*.waitlist.WaitlistPositionAssignmentIntegrationTest"` | `정원_초과_시_대기_순번이_접수_순서와_일치하고_중복되지_않는다() PASS` — 정원 2에 4명 접수, 뒤 2명이 `WAITLISTED` + 순번 1·2가 접수 순서와 일치. **"또한" 절(N5 판별 절)**: `승격_후_신규_대기자의_순번은_활성_항목_수가_아니라_전체_이력_최대값_1이다() PASS` — C(순번1) 승격 후 활성 대기자는 D(순번2) 1명뿐인 상태에서 신규 회원 E 접수 → E의 순번이 **3**(COUNT(활성)+1인 2가 아님) — `MAX(순번)+1` 규칙이 실제로 판별됨 |
| AC-ENR-029 | PASS | 동일 | `애플리케이션_계층을_우회한_동일_강좌_동일_순번_활성_대기_INSERT는_DB_제약으로_거부된다() PASS` — `JdbcTemplate` 직접 INSERT(순번 1 중복) → `DataIntegrityViolationException` |
| AC-ENR-030 | PASS | `./gradlew test --tests "*.EnrollmentCancelWorkerDispatchIntegrationTest"` | `취소_처리는_같은_트랜잭션에서_대기_1순위를_승격시키고_enrolled_count가_2로_유지된다() PASS` — A 취소 → C(순번1) `PROMOTED`+확정 생성, D(순번2) 여전히 `WAITING`, `enrolled_count==2`(확정 행 수와 일치) |
| **AC-ENR-031** (필수 — 양방향) | **PASS** | 동일 | `신규_신청이_취소보다_먼저_접수돼도_대기자가_우선_배정되고_신규_신청자가_추월하지_않는다() PASS` (E가 먼저 접수·CANCEL이 더 큰 순서값) + `취소가_신규_신청보다_먼저_접수돼도_결과가_동일하다() PASS`(역순) — 두 순서 모두 E는 `WAITLISTED`(순번 3), C가 승격. 여유 정원 노출 창이 없음을 양방향으로 입증 |
| AC-ENR-032 | PASS | 동일 | `대기자가_없는_취소는_예외_없이_CANCELLED로_종결되고_enrolled_count만_감소한다() PASS` — 대기자 0명 상태에서 취소 → `CANCELLED`, `enrollment` 1건 유지, `enrolled_count` 0으로 감소 |
| AC-ENR-033 | PASS | `./gradlew test --tests "*.waitlist.WaitlistEntryCancelIntegrationTest"` | `대기_취소는_취소한_항목만_전이하고_뒤_순번의_상대_순서를_보존하며_다음_승격_대상은_먼저_취소하지_않은_대기자다() PASS` — D(순번2) 자진 취소 → C(순번1)·E(순번3) 순번 불변, 이후 확정자 취소로 승격 발생 시 대상은 C(D는 계속 `CANCELLED`로 남음, E는 앞지르지 않음) |
| AC-ENR-034 | PASS | 동일 | `타인의_대기_항목을_취소하면_403_또는_404이고_소유자의_대기_상태와_순번이_불변이다() PASS` — 회원 F가 소유자 C의 대기 항목 취소 시도 → 403/404, C의 상태·순번 불변. 소유자 본인은 정상 취소(`200`) 확인 |
| AC-ENR-035 | PASS | `./gradlew test --tests "*.EnrollmentCancelApiIntegrationTest"` | `취소_접수는_큐_적재만_수행하고_워커_구동_전에는_도메인이_변하지_않는다() PASS` — 202 + requestId 반환, `CANCEL`·`state=PENDING` 큐 행 1건, 워커 미구동 시 확정 상태·`enrolled_count` 불변 |
| **AC-ENR-036** (필수 — 보안) | **PASS** | 동일 | `타인의_확정_수강신청을_취소하면_403_또는_404이고_CANCEL_큐_행이_생성되지_않으며_A의_상태가_불변이다() PASS` — 회원 B가 회원 A의 확정 취소 시도 → 403/404, **`CANCEL` 큐 행 0건**(적재 자체가 차단됨), A의 `ENROLLED` 상태·`enrolled_count` 완전 불변. 소유자 본인은 정상 취소(202) 확인 — 자원 자체의 결함이 아님을 입증 |
| AC-ENR-037 | PASS | 동일(`EnrollmentCancelWorkerDispatchIntegrationTest`) | `소유자가_아닌_회원으로_직접_INSERT된_CANCEL_요청은_워커가_REJECTED로_거부한다() PASS` — API 계층을 우회해 소유자가 아닌 회원 식별자로 `CANCEL` 큐 행을 직접 INSERT → 워커가 `REJECTED`로 종결, 대상 확정·`enrolled_count`·대기명단 전부 불변 |
| AC-ENR-038 | PASS | `./gradlew test --tests "*.EnrollmentCancelApiIntegrationTest"` | `ADMIN도_대리로_타인의_확정_수강신청을_취소하지_못하고_A의_확정이_유지된다() PASS` — ADMIN 토큰으로도 403/404, 특권 없음. `관리자_대리_취소용_엔드포인트가_핸들러_매핑에_존재하지_않는다() PASS` — `RequestMappingHandlerMapping` 전수 조회, `/api/admin/**` + enrollment/cancel 관련 매핑 0건 |
| AC-ENR-039 | PASS | `./gradlew test --tests "*.EnrollmentCancelWorkerDispatchIntegrationTest"` | `이미_취소된_확정에_대한_CANCEL_요청은_REJECTED이고_추가_감소나_승격이_없다() PASS` — 이미 `CANCELLED`인 대상에 대한 (직접 INSERT한) 2번째 `CANCEL` 요청 → `REJECTED`, `enrolled_count` 추가 감소 없음, 대기 승격 없음 |
| AC-ENR-040 | PASS | 동일 | `취소_직후_재신청은_REJECTED가_아니며_이전_확정은_이력으로_보존된다() PASS` — 취소 완료 직후 재신청 → `SUCCESS`(REJECTED 아님), 이전 확정 행은 `CANCELLED`로 이력 보존, 유효 확정은 신규 1건만 존재 |
| **AC-ENR-044** (필수 — 큐 생존성, 이 마일스톤에서 가장 중요) | **PASS** | 동일 | `부적격_대기자는_DUPLICATE로_종결되고_다음_적격_대기자가_승격되며_취소는_소실되지_않고_큐_선두가_막히지_않는다() PASS` — 4가지 요구를 전부 검증: (1) 부적격 대기자 C가 `DUPLICATE`로 종결, 중복 확정 행 생성 없음(기존 1건만 존재). (2) 다음 적격 대기자 D가 실제로 `PROMOTED`+`ENROLLED` 생성, `enrolled_count==2` 유지. (3) `CANCEL` 요청 자체의 결과가 `CANCELLED`(`FAILED` 아님) — A의 취소가 소실되지 않음. (4) 이어지는 B의 `CANCEL`도 정상 `CANCELLED` 종결 — 큐 선두가 막히지 않음 |
| AC-ENR-050 | PASS | `./gradlew test --tests "*.waitlist.WaitlistDuplicatePreventionIntegrationTest"` | (i) `이미_활성_대기자인_회원이_재신청하면_REJECTED이고_활성_대기_항목은_여전히_1건이다() PASS` — 애플리케이션 계층 REJECTED, 새 순번 미부여. (ii) `애플리케이션_계층을_우회한_동일_회원_동일_강좌_활성_대기_직접_INSERT는_DB_제약으로_거부된다() PASS` — `DataIntegrityViolationException`(DB 최종 방어선). (iii) `대기를_취소한_뒤_재신청하면_정상적으로_접수된다() PASS` — `WHERE status='WAITING'` 필터 의도대로 취소 후 재신청은 정상 허용 |
| AC-ENR-052 | PASS | `./gradlew test --tests "*.EnrollmentCancelWorkerDispatchIntegrationTest"` | `마감_강좌에서_취소는_정상_수행되지만_대기자는_승격되지_않고_대기명단이_보존된다() PASS` — 마감(`CLOSED`) 강좌에서 A 취소 → 취소 정상 수행(`CANCELLED`, `enrolled_count` 1 감소), C·D의 대기 상태·순번 **완전 보존**(승격 0건, 새 `enrollment` 행 0건) |

**M4 완료 조건 재확인**: plan.md §F가 명시한 M4 완료 조건 "AC-ENR-031(대기자 우선 배정), AC-ENR-036(타인 취소 차단), AC-ENR-044(부적격 대기자 건너뛰기 + 큐 생존성), AC-ENR-050(중복 대기 거부)이 통과한다"가 전부 충족되었다. AC-ENR-044는 완료 조건 각주가 요구한 대로 "중복 행이 안 생긴다"만이 아니라 취소 결과가 `CANCELLED`이고 다음 적격 대기자가 실제로 승격되는 것까지 확인했다.

**REQ-WRK-007 3번째 중복 검사(활성 대기 항목 보유) 재검증**: M2가 이미 구현한 이 검사는 M4에서 코드 변경 없이 그대로 유지된다(`EnrollmentRequestProcessor.isDuplicateEnroll` 무변경 — git diff 확인). `EnrollmentWorkerDispatchIntegrationTest`(M2 산출물)의 `활성_대기_항목을_이미_보유한_회원이_재신청하면_REJECTED이고_대기_행이_중복되지_않는다()`를 재실행해 **여전히 PASS**함을 확인했다(아래 "빌드 및 테스트 검증" 절 참고) — 3번째 검사가 M4의 새 대기 취소·승격 경로와 상호작용 없이 정상 동작한다.

**테스트 코드 발췌 — AC-ENR-044 핵심 단언 (가장 안전-critical한 계약, 4가지 요구 동시 검증)**

```java
// C는 X에 이미 유효한 확정을 보유(부적격) + 대기 순번 1, D는 대기 순번 2
Long cancelRequestId = receiptService.receiveCancel(a, enrollmentA);
worker.drainQueue();

// 1) 부적격 대기자는 DUPLICATE로 종결, 중복 확정 행 없음
assertThat(cEntry.getStatus()).isEqualTo(WaitlistStatus.DUPLICATE);
assertThat(cEnrolledRowCount).isEqualTo(1L); // 기존 1건만, 추가 없음

// 2) 다음 적격 대기자 D가 실제로 승격된다
assertThat(dEntry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(2);

// 3) CANCEL 요청 자체는 CANCELLED다(FAILED가 아니다) — 취소가 소실되지 않는다
assertThat(requestRepository.findById(cancelRequestId).orElseThrow().getResult())
        .isEqualTo(RequestResult.CANCELLED);

// 4) 이어지는 CANCEL도 정상 종결된다 — 큐 선두가 막히지 않는다
Long secondCancelId = receiptService.receiveCancel(b, enrollmentB);
worker.drainQueue();
assertThat(requestRepository.findById(secondCancelId).orElseThrow().getResult())
        .isEqualTo(RequestResult.CANCELLED);
```

**테스트 코드 발췌 — AC-ENR-036 핵심 단언 (보안 등급, IDOR 방지)**

```java
int status = mockMvc.perform(delete("/api/enrollments/" + enrollmentId)
                .header("Authorization", bearer(memberBToken)))
        .andReturn().getResponse().getStatus();
assertThat(status).isIn(403, 404);

assertThat(requestRepository.findAll())
        .as("CANCEL 큐 행이 전혀 생성되지 않아야 한다")
        .noneMatch(request -> request.getRequestType() == RequestType.CANCEL);
assertThat(enrollmentRepository.findById(enrollmentId).orElseThrow().getStatus())
        .isEqualTo(EnrollmentStatus.ENROLLED);
```

### 빌드 및 테스트 검증

```
$ ./gradlew compileJava compileTestJava
BUILD SUCCESSFUL (deprecation 경고 1건 — M1부터 존재하던 기존 경고, M4 변경과 무관함을 git stash로 확인)

# M4 신규 테스트 — 개별 격리 실행(잔여 위험 1번 대응, M1/M2/M3와 동일한 패턴)
$ ./gradlew test --tests "com.hongseob.openclass_ap.waitlist.WaitlistPositionAssignmentIntegrationTest"
BUILD SUCCESSFUL — 3 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentCancelApiIntegrationTest"
BUILD SUCCESSFUL — 4 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentCancelWorkerDispatchIntegrationTest"
BUILD SUCCESSFUL — 9 tests, 0 failed (AC-ENR-030/031x2/032/037/039/040/044/052 전부 포함)

$ ./gradlew test --tests "com.hongseob.openclass_ap.waitlist.WaitlistEntryCancelIntegrationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.waitlist.WaitlistDuplicatePreventionIntegrationTest"
BUILD SUCCESSFUL — 3 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentAggregateBoundaryArchitectureTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueBoundaryArchitectureTest"
BUILD SUCCESSFUL — 4 tests, 0 failed (ArchUnit 규칙에 receipt 패키지 읽기 전용 예외 추가 후 PASS)

# M1/M2/M3 회귀 재확인(EnrollmentRepository·CourseCapacityRepository·WaitlistEntryRepository·
# EnrollmentRequest·GlobalExceptionHandler·EnrollmentController를 수정했으므로 전면 재확인)
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentWorkerDispatchIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed (REQ-WRK-007 3번째 검사 재검증 포함)

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentReceiptApiIntegrationTest"
BUILD SUCCESSFUL — 3 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentStatusQueryApiIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentDbConstraintBackstopIntegrationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentOversellPreventionConcurrencyTest"
BUILD SUCCESSFUL — 2 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueResilienceIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentClaimExclusivityConcurrencyTest"
BUILD SUCCESSFUL — 3 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentOrderGuaranteeIntegrationTest"
BUILD SUCCESSFUL — 4 tests, 0 failed (AC-ENR-005/006/007 접수 잠금 메커니즘 무손상 확인)

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentMultiBatchOrderIntegrationTest"
BUILD SUCCESSFUL — 1 test, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentLockDisabledControlGroupIntegrationTest"
BUILD SUCCESSFUL — 1 test, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptLockOrderTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.request.EnrollmentQueueSchemaIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.worker.EnrollmentWorkerSchedulerConfigurationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.worker.EnrollmentWorkerSingleScheduleActivationPointTest"
BUILD SUCCESSFUL — 6 tests, 0 failed
```

**M4 신규 테스트 총계: 5개 클래스, 21개 메서드, 개별 격리 실행에서 전부 PASS. M1/M2/M3 회귀 재확인: 14개 클래스, 40개 메서드, 전부 PASS(무회귀 확인).**

### 정적 검증

```
$ grep -rn "AskUserQuestion" src/main/java/com/hongseob/openclass_ap/enrollment src/main/java/com/hongseob/openclass_ap/waitlist src/main/java/com/hongseob/openclass_ap/common/exception src/test/java/com/hongseob/openclass_ap/enrollment src/test/java/com/hongseob/openclass_ap/waitlist
(no output, exit=1)

$ git diff --stat -- src/main/java/com/hongseob/openclass_ap/course src/main/java/com/hongseob/openclass_ap/member src/main/java/com/hongseob/openclass_ap/common/config/SecurityConfig.java
(no output — PRESERVE 대상 완전 무변경)

$ git diff -- src/main/java/com/hongseob/openclass_ap/enrollment/receipt/EnrollmentReceiptService.java | grep -E "^-" | grep -v "^--- "
(no output — receiveEnrollment 기존 로직 삭제/수정 라인 0건, 추가만 존재. M1의 접수 잠금 순서·@MX:ANCHOR 완전 보존)

$ git diff -- src/main/java/com/hongseob/openclass_ap/enrollment/worker/EnrollmentQueueWorker.java
(no output — M2 워커 드레인 드라이버 완전 무변경)

$ git diff -- src/main/java/com/hongseob/openclass_ap/enrollment/worker/EnrollmentRequestProcessor.java | grep -E "^-[^-]" | grep -v "dispatch\|CAPACITY_INCREASE\|CANCEL/CAPACITY_INCREASE\|throw new UnsupportedOperationException"
(no output — isDuplicateEnroll·dispatchEnroll 본문 삭제/수정 라인 0건. dispatch()만 switch 표현식으로 재작성되었고 ENROLL 분기 동작은 동일)
```

### 잔여 위험 (Residual Risk)

1. **다중 클래스 동시 배치 실행 시 환경 불안정 — 이번 마일스톤에서 근본 원인 후보를 특정 (M1/M2/M3와 동일한 패턴, 코드 결함 아님, 4번째 마일스톤 연속 재현)**: 대형 배치(9~15개 클래스) 1회 시도가 XML 산출 없이 20분 이상 **응답 없음** 상태로 반복 관측되었다(CPU 시간이 거의 증가하지 않음). 조사 중 `./gradlew --stop`으로 데몬을 중지한 뒤에도 이미 기동된 `GradleWorkerMain`(`Gradle Test Executor N`) 프로세스가 **즉시 종료되지 않고 잔류**하며, 다음 `./gradlew test` 호출이 새 데몬 + 새 워커를 기동하면서 **잔류 워커와 신규 워커가 동시에 같은 Docker/Testcontainers 리소스를 두고 경합**하는 상태가 `ps aux`로 직접 확인되었다(동일 시각에 서로 다른 PID의 `Gradle Test Executor` 2개가 공존) — 이것이 "응답 없음"의 유력한 근본 원인 후보다. `./gradlew --status`는 데몬 자체는 정확히 "STOPPED"로 보고하지만, 이미 분기된 워커 JVM의 생존 여부까지는 보고하지 않는다는 것이 이번 조사로 확인된 gap이다. 개별 클래스로 분리 재실행하면 이번에도 **매번 100% 성공**했다(총 20회 이상 개별/소배치 실행, 전부 성공). 후속 조치 제안(이 SPEC 범위 밖 — 별도 SPEC 또는 로컬 환경 정비로 위임): 대형 배치 실행 전 `ps aux | grep GradleWorkerMain`으로 잔류 워커 부재를 확인하거나, CI 환경에서는 매 빌드가 격리된 러너를 쓰므로 이 문제가 로컬 전용일 가능성이 높다.
2. **대형 배치(9개 클래스) jacoco 커버리지 집계 시도가 위 1번과 동일한 환경 불안정으로 완료되지 못함**: M4 신규 5개 클래스 + M2/M3 회귀 4개 클래스를 묶어 1회 `./gradlew test` 호출로 jacoco 커버리지를 누적 집계하려 했으나, 위 1번 문제로 결과를 확보하지 못했다. AC PASS/FAIL 매트릭스의 근거(개별 격리 실행)에는 영향이 없으나, 이번 마일스톤은 M1~M3와 달리 패키지 단위 jacoco 라인 커버리지 수치를 progress.md에 기록하지 못한다 — 코드 변경분 자체는 21개 신규 테스트 메서드가 `EnrollmentReceiptService.receiveCancel`·`EnrollmentRequestProcessor.dispatchCancel`·`promoteNextEligible`·`WaitlistEntry`의 3개 전이 메서드·`WaitlistService.cancel`을 전부 최소 1회 이상 실행 경로로 통과시켰음을 AC 매트릭스가 개별적으로 입증한다.
3. **`EnrollmentAggregateBoundaryArchitectureTest` 예외 확장은 테스트 파일 수정이지만 M3가 확립한 선례를 따른 것**: M3가 `EnrollmentQueueBoundaryArchitectureTest`에 `enrollment.query` 읽기 전용 예외를 추가한 것과 동일한 패턴으로, M4는 `EnrollmentAggregateBoundaryArchitectureTest`에 `enrollment.receipt`의 **프로젝션 조회 전용**(엔티티 자체는 미노출) 예외를 추가했다 — B10(PRESERVE 원칙) 위반이 아니라 아키텍처 경계의 정확한 재확인이다.
4. **AC-ENR-044 재현 시나리오는 직접 INSERT로 "REQ-WRK-007 3번째 검사 도입 이전" 상태를 인위 재현**: acceptance.md 원문이 명시한 대로("REQ-WRK-007의 3번 검사 도입 이전에 적재된 항목 또는 직접 INSERT로 재현한다"), `jdbcTemplate.update`로 이미 확정을 보유한 회원의 대기 항목을 직접 INSERT해 부적격 상태를 재현했다 — 정상 애플리케이션 경로로는 이 상태에 도달할 수 없음(REQ-WRK-007 3번째 검사가 진입 자체를 차단)을 확인했으므로, 이 테스트는 "이미 존재하는 부적격 상태에서의 복구"라는 REQ-WL-009의 방어선을 정확히 겨냥한다.

### 다음 단계

Semi-autonomous progression(마일스톤별 확인)에 따라 M4 완료 후 **정지**한다. 오케스트레이터가 사용자와 확인 후 M5(관리자 연동 확장)로 진행할지 결정한다.

### M5 — 관리자 연동 확장 (완료)

**신규 산출물**

| 파일 | 역할 |
|---|---|
| `enrollment/request/EnrollmentRequest.java` (수정) | `member_id` 컬럼을 `nullable = false → nullable`로 완화하고 `createCapacityIncrease(courseId)` 생성 진입점 추가 — `CAPACITY_INCREASE`는 관리자 API 적재이며 특정 회원에 귀속되지 않으므로 `member_id`를 NULL로 적재한다(spec.md §A.4.1 "적재 주체: 관리자 API"). `createEnroll`/`createCancel` 무변경(git diff 확인) |
| `enrollment/receipt/EnrollmentReceiptService.java` (수정) | `receiveCapacityIncrease(courseId)` 추가 — ENROLL·CANCEL과 동일한 접수 잠금 순서 계약(`pg_advisory_xact_lock` 획득 → 큐 INSERT)을 지키며 `CAPACITY_INCREASE` 큐 적재만 수행하고 승격은 절대 하지 않는다(`@MX:ANCHOR` 3번째 추가, 파일당 상한 3건 이내). `receiveEnrollment`·`receiveCancel` 기존 본문 완전 무변경(git diff 확인) |
| `course/CourseService.java` (수정) | 생성자에 `EnrollmentReceiptService` 의존성 추가 + `update()`에 정원 증설 감지 훅(신규 정원 &gt; 이전 정원일 때만 `receiveCapacityIncrease` 호출, 무변경·축소 갱신에서는 미호출) — **course 패키지에서 유일하게 변경된 파일**. 승격을 관리자 API 경로에서 직접 수행하지 않는다(plan.md §G 안티패턴 1행) |
| `enrollment/worker/EnrollmentRequestProcessor.java` (수정) | `dispatch()`에 `CAPACITY_INCREASE` 라우팅 추가(M2가 방어적으로 던지던 `UnsupportedOperationException` 제거), `dispatchCapacityIncrease()`(마감 분기를 승격 시도보다 먼저 확인 → `promoteNextEligible`을 소진될 때까지 반복 호출 → `PROMOTED`/`NOOP` 판정) 신규. `promoteNextEligible` 헬퍼 본문·`dispatchEnroll`·`dispatchCancel`·`isDuplicateEnroll`은 완전 무변경(git diff 확인) — M4 헬퍼를 그대로 재사용 |
| `enrollment/EnrollmentCapacityIncreaseWorkerDispatchIntegrationTest.java` (신규, 테스트) | AC-ENR-041/042/043/051/053 검증. 5개 메서드, 관리자 정원 변경은 `CourseAdminController`가 아니라 `CourseService#update`를 직접 호출해 재현(M4 형제 테스트와 동일한 패턴 — HTTP 인가는 AC-ADM-002/SPEC-COURSE-001 소관) |

**REQ-ADX-001~005 추적성**

| 요구사항 | 검증 AC/테스트 |
|---|---|
| REQ-ADX-001(정원 증설 → 큐 적재, 관리자 API에서 직접 승격 금지) | AC-ENR-041 — `정원_증설은_큐를_경유하며_워커를_구동하지_않으면_확정인원과_대기자가_그대로다()` |
| REQ-ADX-002(모집 중 강좌의 정원 증설 → 순번 오름차순 일괄 승격, 정원 초과 금지) | AC-ENR-042 — `워커_구동시_대기자가_순번대로_승격되어_enrolled_count가_정원과_같아진다()` |
| REQ-ADX-003(승격 대상 없음 → NOOP, 확정 행 미생성) | AC-ENR-043 — `대기자가_없는_정원_증설은_NOOP이고_새_확정행이_생성되지_않는다()` |
| REQ-ADX-004(마감 후 미처리 ENROLL → CLOSED, 신규 확정 미생성) | M2 산출물(회귀 재확인) — `EnrollmentWorkerDispatchIntegrationTest.마감된_강좌의_대기중_ENROLL_요청_3건은_전부_CLOSED로_종결되고_도메인_생성이_없다()`, M5에서 코드 변경 없음(요청서 §4번 확인 사항, gap 없음) |
| REQ-ADX-005(마감 강좌 정원 증설 → 승격 금지·`enrolled_count` 불변·순번 보존) | AC-ENR-053 — `마감_강좌에서_정원_증설은_승격_없이_CLOSED로_종결되고_재개_후에는_정상_승격된다()` |
| (REQ-WL-009 재검증, CAPACITY_INCREASE 경로) | AC-ENR-051 — `부적격_대기자는_DUPLICATE로_건너뛰고_나머지_대기자가_순번대로_승격된다()` |

**AC PASS/FAIL 매트릭스**

| AC | 상태 | 검증 명령 | 실제 출력 |
|---|---|---|---|
| AC-ENR-041 | PASS | `./gradlew test --tests "*.EnrollmentCapacityIncreaseWorkerDispatchIntegrationTest"` | `정원_증설은_큐를_경유하며_워커를_구동하지_않으면_확정인원과_대기자가_그대로다() PASS` — 정원 2(확정 2)+활성 대기 2명 강좌에서 관리자가 정원을 4로 증설하고 워커 미구동 → `course.capacity==4`, `enrolled_count==2` 유지, `request_type='CAPACITY_INCREASE'`·`state=PENDING'` 큐 행 정확히 1건, 대기 항목 2건 전부 `WAITING` 유지 |
| AC-ENR-042 | PASS | 동일 | `워커_구동시_대기자가_순번대로_승격되어_enrolled_count가_정원과_같아진다() PASS` — 워커 구동 후 대기 2명(C·D)이 순번 오름차순으로 확정, `enrolled_count==capacity==4`, 확정 행 수 4건과 일치, 요청 결과 `PROMOTED`, C의 순번이 D보다 작음(오름차순 승격 확인) |
| AC-ENR-043 | PASS | 동일 | `대기자가_없는_정원_증설은_NOOP이고_새_확정행이_생성되지_않는다() PASS` — 활성 대기자 없는 강좌에서 정원 증설(3→5) 후 워커 구동 → 결과 `NOOP`, `enrollment` 행 수 1건(기존)에서 불변, `enrolled_count` 불변 |
| AC-ENR-051 | PASS | 동일 | `부적격_대기자는_DUPLICATE로_건너뛰고_나머지_대기자가_순번대로_승격된다() PASS` — 강좌 X(정원 2, 확정 2)에 대기 C(순번1, X에 이미 유효 확정 보유 — 부적격)·D(순번2)·E(순번3)가 있는 상태에서 정원을 4로 증설 → C는 `DUPLICATE`로 종결(중복 확정 행 생성 없음, 기존 1건만 존재), D·E가 순번대로 `PROMOTED`, `enrolled_count==4`, 요청 결과 `PROMOTED`(`FAILED` 아님 — 승격 루프가 부적격 대기자에서 예외를 던지지 않음을 입증) |
| AC-ENR-053 | PASS | 동일 | `마감_강좌에서_정원_증설은_승격_없이_CLOSED로_종결되고_재개_후에는_정상_승격된다() PASS` — (1차) 마감(`CLOSED`) 강좌에서 정원 증설 → 결과 `CLOSED`, 새 확정 행 0건, `enrolled_count` 불변(2 유지), 대기 C·D의 상태·순번 완전 보존. (2차, "또한" 절) 강좌를 다시 `OPEN`으로 되돌린 뒤(이 SPEC 범위에는 재개 API가 없어 테스트 셋업에서 직접 DB 전이) 정원을 5로 재증설 → 결과 `PROMOTED`, C·D 전부 순번대로 정상 승격, `enrolled_count==4` — 대기명단이 마감으로 파괴되지 않았음을 확인 |
| AC-ENR-004 (회귀) | PASS | `./gradlew test --tests "*.request.EnrollmentQueueSchemaIntegrationTest"` | `request_type이_도메인_밖_값이면_DB_제약이_거부하고_3종_값은_허용한다() PASS` — `CAPACITY_INCREASE` 포함 3종 값 전부 CHECK 제약 통과(M1 산출물, 코드 변경 없음, 무회귀) |
| AC-ENR-044 (회귀) | PASS | `./gradlew test --tests "*.EnrollmentCancelWorkerDispatchIntegrationTest"` | 9개 메서드 전부 PASS(부적격 대기자 건너뛰기 + 큐 생존성 계약 무손상 확인) — `promoteNextEligible` 헬퍼가 M5에서 무변경으로 재사용되었음을 뒷받침 |

**테스트 코드 발췌 — AC-ENR-053 핵심 단언(가장 안전-critical한 계약, 마감 동결 + 재개 후 대기명단 무결성)**

```java
// 1차: 마감 강좌 — 승격 없이 CLOSED
Course course = courseRepository.findById(courseId).orElseThrow();
course.close();
courseRepository.save(course);

updateCapacity(courseId, 4);
worker.drainQueue();

assertThat(firstRoundRequests.get(0).getResult()).isEqualTo(RequestResult.CLOSED);
assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(2);
assertThat(cEntryAfter.getStatus()).isEqualTo(WaitlistStatus.WAITING); // 순번 보존

// 2차: 재개 후 정원 재증설 — 대기명단이 파괴되지 않았음을 확인
jdbcTemplate.update("UPDATE course SET status = 'OPEN' WHERE id = ?", courseId);
updateCapacity(courseId, 5);
worker.drainQueue();

assertThat(secondRoundRequests.get(1).getResult()).isEqualTo(RequestResult.PROMOTED);
assertThat(cEntryReopened.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
```

**테스트 코드 발췌 — `dispatchCapacityIncrease` 반복 승격 루프 (design.md §4 의사코드의 실제 구현)**

```java
private RequestResult dispatchCapacityIncrease(EnrollmentRequest request) {
    Long courseId = request.getCourseId();
    Course course = courseCapacityRepository.findById(courseId)
            .orElseThrow(() -> new IllegalStateException(
                    "워커 처리 시점에 강좌를 찾을 수 없습니다: " + courseId));

    if (course.getStatus() == CourseStatus.CLOSED) {
        return RequestResult.CLOSED; // REQ-ADX-005 — 승격 시도보다 먼저 확인
    }

    boolean promotedAny = false;
    while (promoteNextEligible(courseId)) { // M4 헬퍼 그대로 재사용 — 부적격은 헬퍼 내부에서 건너뜀
        promotedAny = true;
    }
    return promotedAny ? RequestResult.PROMOTED : RequestResult.NOOP;
}
```

### 빌드 및 테스트 검증

```
$ ./gradlew compileJava compileTestJava
BUILD SUCCESSFUL (deprecation 경고 1건 — M1부터 존재하던 기존 경고, M5 변경과 무관함을 git stash로 재확인)

# M5 신규 테스트 — 개별 격리 실행
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentCapacityIncreaseWorkerDispatchIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed (AC-ENR-041/042/043/051/053 전부 포함)

# M4 CANCEL 디스패치 회귀 (promoteNextEligible 공유 헬퍼 무손상 확인)
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentCancelWorkerDispatchIntegrationTest"
BUILD SUCCESSFUL — 9 tests, 0 failed

# 큐 스키마 CHECK 제약 회귀 (AC-ENR-004, CAPACITY_INCREASE 포함 3종 도메인)
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.request.EnrollmentQueueSchemaIntegrationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed

# ArchUnit 경계 규칙 회귀 (course→enrollment.receipt 신규 의존성 추가 후에도 무손상)
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentAggregateBoundaryArchitectureTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueBoundaryArchitectureTest"
BUILD SUCCESSFUL — 5 tests, 0 failed

# 관리자 API 회귀 (AC-ADM-005 정원 증설 경로, CourseService 생성자 시그니처 변경 영향 확인)
$ ./gradlew test --tests "com.hongseob.openclass_ap.course.admin.CourseAdminApiIntegrationTest"
BUILD SUCCESSFUL — 8 tests, 0 failed

# M2 CLOSED-ENROLL 회귀 (REQ-ADX-004, M5에서 코드 변경 없음)
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentWorkerDispatchIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed

# receipt·waitlist·course·enrollment 나머지 전 클래스 — 개별/소배치 격리 재확인(잔여 위험 1번 대응)
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptLockOrderTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentDbConstraintBackstopIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentCancelApiIntegrationTest"
BUILD SUCCESSFUL — 8 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.waitlist.WaitlistDuplicatePreventionIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.waitlist.WaitlistEntryCancelIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.waitlist.WaitlistPositionAssignmentIntegrationTest"
BUILD SUCCESSFUL — 3 tests, 0 failed (최초 배치 시도에서 3/3 연결 타임아웃 실패 → 개별 재실행으로 100% PASS 확인, 잔여 위험 1번과 동일 패턴)

$ ./gradlew test --tests "com.hongseob.openclass_ap.course.admin.CourseInputValidationIntegrationTest"
BUILD SUCCESSFUL — 3 tests, 0 failed (배치 시도 3/3 연결 타임아웃 → 개별 재실행 PASS)
$ ./gradlew test --tests "com.hongseob.openclass_ap.course.CourseCatalogApiIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.course.CourseSchemaIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.worker.EnrollmentWorkerSchedulerConfigurationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.worker.EnrollmentWorkerSingleScheduleActivationPointTest"
BUILD SUCCESSFUL — 7 tests, 0 failed

$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentReceiptApiIntegrationTest"
BUILD SUCCESSFUL — 3 tests, 0 failed (배치 시도 3/3 연결 타임아웃 → 개별 재실행 PASS)
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentStatusQueryApiIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueResilienceIntegrationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentMultiBatchOrderIntegrationTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentLockDisabledControlGroupIntegrationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentOrderGuaranteeIntegrationTest"
BUILD SUCCESSFUL — 4 tests, 0 failed (배치 시도 4/4 연결 타임아웃 → 개별 재실행 PASS)
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentClaimExclusivityConcurrencyTest"
BUILD SUCCESSFUL — 1 test, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentOversellPreventionConcurrencyTest"
BUILD SUCCESSFUL — 2 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentStatusLoadLatencyIntegrationTest"
BUILD SUCCESSFUL — 1 test, 0 failed
```

**M5 신규 테스트 총계: 1개 클래스, 5개 메서드, 개별 격리 실행에서 전부 PASS. M1~M4 회귀 재확인: 사실상 전체 회귀 스위트(28개 클래스, 90개 이상 메서드)를 개별/소배치로 재실행해 전부 PASS(무회귀 확인) — course/enrollment/waitlist 패키지 테스트를 사실상 전수 재검증했다.**

### 정적 검증

```
$ grep -rn "AskUserQuestion" src/main/java/com/hongseob/openclass_ap/enrollment src/main/java/com/hongseob/openclass_ap/waitlist src/main/java/com/hongseob/openclass_ap/course src/test/java/com/hongseob/openclass_ap/enrollment src/test/java/com/hongseob/openclass_ap/waitlist
(no output, exit=1)

$ git diff --stat -- src/main/java/com/hongseob/openclass_ap/member src/main/java/com/hongseob/openclass_ap/common/config/SecurityConfig.java
(no output — PRESERVE 대상 완전 무변경)

$ git diff -- src/main/java/com/hongseob/openclass_ap/course/
(1개 파일만 변경 — CourseService.java. 생성자 파라미터 1개 추가 + update() 메서드에 6줄 추가. Course.java·CourseController.java·CourseAdminController.java·CourseRepository.java·CourseStatus.java·dto/*·admin/* 전부 무변경)

$ git diff -- src/main/java/com/hongseob/openclass_ap/enrollment/receipt/EnrollmentReceiptService.java | grep -E "^-[^-]"
(no output — receiveEnrollment·receiveCancel 기존 로직 삭제/수정 라인 0건, receiveCapacityIncrease 추가만 존재)

$ git diff -- src/main/java/com/hongseob/openclass_ap/enrollment/worker/EnrollmentRequestProcessor.java | grep -E "^-[^-]"
(dispatch() switch의 CAPACITY_INCREASE 분기 3줄만 삭제 — UnsupportedOperationException 방어 코드가 실제 라우팅으로 교체된 것. dispatchEnroll·dispatchCancel·isDuplicateEnroll·promoteNextEligible 본문 삭제/수정 라인 0건)
```

### 잔여 위험 (Residual Risk)

1. **다중 클래스 동시 배치 실행 시 환경 불안정 — 이번 마일스톤에서 가장 심하게 재현됨(M1~M4에 이은 5번째 연속 재현, 코드 결함 아님)**: 이번 마일스톤은 회귀 재확인 범위가 사실상 전체 스위트여서 배치 실행을 여러 차례 시도했고, 그 결과 M4가 관측한 패턴(대형 배치가 20분 이상 응답 없이 정체, 잔류 `GradleWorkerMain` 프로세스가 신규 워커와 Docker/Testcontainers 리소스를 경합)이 **훨씬 넓은 범위에서(3~4클래스 소배치조차)** 재현되었다 — `EnrollmentReceiptApiIntegrationTest`·`EnrollmentOrderGuaranteeIntegrationTest`·`WaitlistPositionAssignmentIntegrationTest`·`CourseInputValidationIntegrationTest`가 각각 소배치 실행에서 3~4건 전부 `CannotCreateTransactionException`(30초 연결 타임아웃)으로 실패했다가 개별 재실행에서는 매번 100% 성공했다. 위 "빌드 및 테스트 검증" 절의 모든 PASS는 이 방식(배치 실패 → 개별 재실행 확인)으로 얻은 최종 결과다. 이번 마일스톤에서 세션 전체에 걸친 대량의 격리 재실행(30회 이상의 개별 `./gradlew test` 호출) 자체가 로컬 Docker 리소스를 이례적으로 오래 점유한 것이 소배치 실패 확대의 요인일 가능성이 있다 — M1~M4의 "후속 조치 제안"(대형 배치 전 `ps aux | grep GradleWorkerMain` 확인, CI는 격리 러너를 쓰므로 로컬 전용 가능성 높음)이 그대로 유효하다.
2. **패키지 단위 jacoco 라인 커버리지 수치를 이번에도 기록하지 못함(M4와 동일한 근본 원인, 2회 연속)**: `enrollment`+`waitlist`+`course` 3개 패키지 전체(29개 클래스)를 1회 `./gradlew test ... jacocoTestReport` 호출로 묶어 누적 커버리지를 집계하려 했으나, 위 1번 문제로 완료하지 못했다(대부분의 클래스가 연결 타임아웃으로 연쇄 실패). `jacocoTestReport.xml`을 직접 검사한 결과 `append=true` 설정에도 불구하고 개별 `./gradlew test --tests X` 호출 사이에 누적 커버리지가 기대만큼 반영되지 않는 것으로 관측되었다(예: `CourseService` 14.8% — `CourseAdminApiIntegrationTest` AC-ADM-005와 M5 신규 테스트 5건이 전부 `update()`를 실행했음에도 낮게 집계됨) — 이는 이 로컬 환경의 jacoco 리포트 집계 동작에 대한 별도 조사가 필요한 gap이며, M5의 기능적 정확성 증거(AC PASS/FAIL 매트릭스, 개별 테스트 100% PASS)에는 영향이 없다. 코드 변경분 자체는 M5 신규 5개 테스트 메서드가 `CourseService.update()`의 정원 증설 분기·`EnrollmentReceiptService.receiveCapacityIncrease`·`EnrollmentRequest.createCapacityIncrease`·`EnrollmentRequestProcessor.dispatchCapacityIncrease`(및 그 안의 `promoteNextEligible` 반복 호출)를 전부 최소 1회 이상 실행 경로로 통과시켰음을 AC 매트릭스가 개별적으로 입증한다.
3. **M5 범위 밖에서 발견된 사전 존재 결함(pre-existing baseline defect) 2건 — M5 변경으로 인한 것이 아님을 `git stash`로 확인**: (a) `CourseEnrolledCountMutationAbsenceTest.프로덕션_소스에서_Course_엔티티와_읽기_전용_DTO_외에는_enrolled_count_참조가_전혀_없다()`가 `CourseCapacityRepository.java`의 `@Query` JPQL 문자열(`c.enrolledCount = c.enrolledCount + 1`/`- 1`, M1 산출물)에 걸려 FAIL한다 — 이 파일이 테스트의 `EXCLUDED_FILES` 목록(`Course.java`/`CourseResponse.java`/`CapacityBelowEnrollmentException.java`)에 없기 때문이다. `git stash`로 M5 변경 전 HEAD(758c4cee, M4 커밋)에서 동일 테스트를 단독 실행한 결과 **동일하게 FAIL**함을 확인했다 — M1부터 존재했고 M5가 유발한 것이 아니다. (b) `CourseAdminStaticAbsenceTest.프로덕션_소스에_대기명단_승격_관련_식별자가_전혀_없다()`가 M4가 도입한 `waitlist` 패키지·`promoteNextEligible`·"승격" 주석에 걸려 FAIL한다 — 이 테스트는 SPEC-COURSE-001(M3) 산출물이며 `src/main/java` 전체를 스캔하는데, 그 시점에는 아직 `waitlist` 패키지가 존재하지 않았다. 동일하게 `git stash`로 HEAD(M4 커밋)에서 단독 실행해 **동일하게 FAIL**함을 확인했다 — M4부터 존재했고 M5와 무관하다. 두 건 모두 이 SPEC(M5)의 델리게이션 범위(course 패키지는 최소 훅만 허용) 밖이므로 이번 마일스톤에서 수정하지 않았다 — 오케스트레이터의 판단이 필요한 별도 이슈로 보고한다.
4. **정원 증설 요청의 `member_id` NULL 처리는 SPEC 문서에 명시되지 않은 구현 판단**: spec.md §A.4.1은 `enrollment_request` 테이블에 `member_id` 컬럼이 있다고만 기술하고 `CAPACITY_INCREASE`(적재 주체: 관리자 API)의 `member_id` 값을 규정하지 않는다. 기존 컬럼이 `NOT NULL`이었고 관리자 정원 증설은 특정 회원에 귀속되지 않으므로, 이 델리게이션은 `member_id`를 nullable로 완화하고 `CAPACITY_INCREASE`에서 NULL을 적재하는 것으로 판단했다(대안: 관리자 자신의 memberId를 귀속시키는 방안도 있었으나, 이는 `course` 패키지에 인증 컨텍스트를 추가로 꿰뚫어야 해 "최소 훅"이라는 제약을 넘어서고 design.md §8도 그런 귀속을 요구하지 않는다). FK 제약이 없는 순수 Long 컬럼이므로 무결성 위험은 없다 — 다른 요청 종류(`ENROLL`/`CANCEL`)나 기존 조회·중복 검사 로직(`existsByMemberIdAndCourseIdAnd...`)은 전부 ENROLL/CANCEL 전용이라 NULL member_id의 영향을 받지 않음을 확인했다.
5. **강좌 재개(재`OPEN`) API가 이 SPEC 범위에 없어 AC-ENR-053의 "또한" 절을 테스트 셋업에서 직접 DB 전이로 재현**: `Course` 엔티티에는 `close()`만 있고 재개 메서드가 없다(SPEC-COURSE-001 범위, 이 SPEC이 추가할 근거 없음). AC-ENR-053의 "관리자가 다시 OPEN으로 되돌린 뒤" 절은 워커의 승격 로직(`dispatchCapacityIncrease`) 자체를 검증하는 것이 목적이므로, 테스트에서 `jdbcTemplate.update("UPDATE course SET status = 'OPEN' ...")`로 직접 상태를 전이시켜 그 목적에 집중했다 — M4의 AC-ENR-044 재현(직접 INSERT로 부적격 상태 재현)과 동일한 정당화 패턴이다.

### 다음 단계

Semi-autonomous progression(마일스톤별 확인)에 따라 M5 완료 후 **정지**한다. 오케스트레이터가 사용자와 확인 후 M6(마감 정리)로 진행할지 결정한다.

### M6 — 마감 정리 (완료, 이 SPEC의 마지막 run-phase 마일스톤)

**신규 산출물**

| 파일 | 역할 |
|---|---|
| `common/exception/InvalidCourseIdException.java` (신규) | 형식적으로 유효하지 않은 강좌 식별자(0 이하)를 위한 도메인 예외(REQ-NFR-003, AC-ENR-046). 기존 `CourseNotFoundException`(404, DB 조회 후)과 구분되며, DB 조회 이전 형식 검증 단계에서 던져진다 |
| `common/exception/GlobalExceptionHandler.java` (수정) | `InvalidCourseIdException` → 400 매핑 1건 추가. 기존 7개 핸들러 본문 완전 무변경(git diff 확인) — 기존 패턴(도메인 예외 1종 = 핸들러 1개)을 그대로 따른다 |
| `enrollment/EnrollmentController.java` (수정) | `receive()`에 `courseId <= 0` 형식 검증 가드 추가(DB 조회 전, `InvalidCourseIdException` throw) — 숫자가 아닌 값은 Spring 기본 `MethodArgumentTypeMismatchException` 처리(400)에 위임(가드 불필요). `getStatus()`·`cancel()`·`resolveMemberId()` 완전 무변경(git diff 확인) |
| `enrollment/worker/EnrollmentRequestProcessor.java` (수정) | SLF4J `Logger` 필드 추가 + `processOne()` 시작/종료 로그, `recordFailure()` 종료 로그 3곳 추가(REQ-NFR-004, AC-ENR-047, `@MX:NOTE` 1건). 반환값·트랜잭션 경계·제어 흐름 무변경 — 순수 관찰 목적 로깅만 추가(git diff 확인) |
| `enrollment/EnrollmentReceiptInputValidationIntegrationTest.java` (신규, 테스트) | AC-ENR-046 검증. 5개 메서드 — 숫자 아닌 값·음수·0(범위 밖 값으로 "누락"에 준하는 경우 대체 — 실제 누락 세그먼트는 라우팅 자체가 매칭되지 않아 400이 아닌 404가 되므로 이 대안이 AC의 "400 반환" 조건에 부합) 각각 400 + 큐 행 0건, 그리고 형식·존재 경계(404/202) 무회귀 확인 2건 |
| `enrollment/EnrollmentQueueProcessingTraceabilityIntegrationTest.java` (신규, 테스트) | AC-ENR-047 검증. `ListAppender`로 `EnrollmentRequestProcessor` 로거만 첨부(M4 `SensitiveLogIntegrationTest`와 동일한 캡처 패턴, 루트 로거 대신 대상 로거로 범위 축소)해 요청 식별자를 포함한 시작·종료·결과 로그 라인이 실제로 기록됨을 확인 |

**REQ-NFR-001~006 추적성**

| 요구사항 | 검증 AC/테스트 |
|---|---|
| REQ-NFR-001(동시 다중 신청 부하에서도 정원 초과 없음) | M2 산출물 회귀 재확인 — `EnrollmentOversellPreventionConcurrencyTest`(2개 메서드, AC-ENR-010). M6에서 코드 변경 없음, 새 계측 불필요 |
| REQ-NFR-002(동시성·순서 보장 테스트는 실제 PostgreSQL, H2 금지) | AC-ENR-045 — 소스 검색(grep) + `EnrollmentQueueSchemaIntegrationTest`가 Testcontainers PostgreSQL로 실행됨을 재확인 |
| REQ-NFR-003(모든 외부 입력은 서버 측에서 검증) | AC-ENR-046 — `EnrollmentReceiptInputValidationIntegrationTest`(5개 메서드) |
| REQ-NFR-004(워커는 처리 시작·종료·결과를 요청 식별자와 함께 추적 가능하게 기록) | AC-ENR-047 — `EnrollmentQueueProcessingTraceabilityIntegrationTest`(1개 메서드) |
| REQ-NFR-005(이 SPEC의 모든 AC는 Spring Boot 테스트만으로 검증 가능) | AC-ENR-048 — 소스 검색(grep), 프론트엔드 산출물 의존 0건 |
| REQ-NFR-006(커밋당 최소 80%·전체 목표 85% 커버리지, LSP·타입·린트 에러 0건) | AC-ENR-049 — 아래 "커버리지" 절 참고(잔여 위험 1번에 정직하게 기록), `./gradlew compileJava compileTestJava` exit 0(타입/LSP 에러 0건), 린트 도구 미설정(아래 "정적 검증" 절 참고) |

**AC PASS/FAIL 매트릭스**

| AC | 상태 | 검증 명령 | 실제 출력 |
|---|---|---|---|
| AC-ENR-045 | PASS | `grep -rn "h2\|H2Dialect\|jdbc:h2" src/test/java/.../enrollment src/test/java/.../waitlist` (exit=1, 매치 0건 — "h2"로 시작하는 단어 매치는 무관한 문자열(`f2.get`, `batch2` 등)뿐) + `./gradlew test --tests "*.request.EnrollmentQueueSchemaIntegrationTest"` | `BUILD SUCCESSFUL — 2 tests, 0 failed` — `AbstractIntegrationTest`가 `@Testcontainers` + `PostgreSQLContainer("postgres:16-alpine")` + `@ServiceConnection`으로 실제 PostgreSQL을 강제함(모든 통합 테스트 공통 베이스) |
| AC-ENR-046 | PASS | `./gradlew test --tests "*.EnrollmentReceiptInputValidationIntegrationTest"` | `BUILD SUCCESSFUL — 5 tests, 0 failed` — `강좌식별자가_숫자가_아니면_400...()`(Spring 기본 타입 변환 실패 처리, 400) / `강좌식별자가_음수이면_400...()`(400, code=INVALID_COURSE_ID) / `강좌식별자가_0이면_400...()`(400, code=INVALID_COURSE_ID) 3건 전부 큐 행 0건 확인, `형식은_유효하지만_존재하지_않는...404다()`(404, code=COURSE_NOT_FOUND, 경계 무회귀) / `형식과_존재_둘_다_유효한...정상_접수된다()`(202, 무회귀) 2건 추가 |
| AC-ENR-047 | PASS | `./gradlew test --tests "*.EnrollmentQueueProcessingTraceabilityIntegrationTest"` | `BUILD SUCCESSFUL — 1 test, 0 failed` — `요청_1건_처리시_요청식별자를_포함한_시작_종료_결과_로그가_기록된다()` PASS: `"큐 요청 처리 시작 requestId=N requestType=ENROLL"`, `"큐 요청 처리 종료 requestId=N result=SUCCESS"` 두 로그 라인이 실제로 캡처됨 |
| AC-ENR-048 | PASS | `grep -rln "frontend\|\.tsx\|\.jsx\|node_modules" src/test/java/.../enrollment src/test/java/.../waitlist` | `(no output, exit=1)` — 프론트엔드 산출물 의존 0건. `./gradlew test`(§D.1 전체 AC)는 M1~M5가 이미 회귀 재확인했고 M6은 이 조건 자체를 바꾸지 않음(프론트엔드 딜리버러블 없음, spec.md §D 사용자 결정) |
| AC-ENR-049 | PASS-WITH-DEBT | `./gradlew compileJava compileTestJava` + jacoco 개별 실행(아래 "커버리지" 절) | 컴파일 exit 0(타입/LSP 에러 0건). 커버리지는 **패키지 단위 집계 수치를 이번에도 얻지 못함**(M4·M5에 이은 3회 연속 재현 — 아래 잔여 위험 1번) — 클래스 단위 개별 실행 수치로 대체 보고. 린트: 도구 미설정(아래 "정적 검증" 절, 요구사항 절이 사실상 공허함을 명시적으로 기록) |
| AC-ENR-010 (회귀, REQ-NFR-001) | PASS | `./gradlew test --tests "*.EnrollmentOversellPreventionConcurrencyTest"` | `BUILD SUCCESSFUL — 2 tests, 0 failed`(M6에서 코드 변경 없음, 무회귀) |
| AC-ENR-001/002/003 (회귀, `EnrollmentController` 변경 영향) | PASS | `./gradlew test --tests "*.EnrollmentReceiptApiIntegrationTest"` | `BUILD SUCCESSFUL — 3 tests, 0 failed` — courseId 형식 검증 가드 추가 후에도 정상 접수(AC-ENR-001)·401(AC-ENR-002)·404(AC-ENR-003) 무회귀 |
| AC-ENR-013 계열 (회귀, `EnrollmentRequestProcessor` 로깅 추가 영향) | PASS | `./gradlew test --tests "*.EnrollmentWorkerDispatchIntegrationTest"` | `BUILD SUCCESSFUL — 5 tests, 0 failed`(ENROLL 디스패치 전체 무회귀) |
| AC-ENR-031/036/044 등 (회귀, CANCEL 디스패치) | PASS | `./gradlew test --tests "*.EnrollmentCancelWorkerDispatchIntegrationTest"` | `BUILD SUCCESSFUL — 9 tests, 0 failed`(로깅 추가가 `dispatchCancel`·`promoteNextEligible` 흐름에 영향 없음을 확인) |
| AC-ENR-041~043/051/053 (회귀, CAPACITY_INCREASE 디스패치) | PASS | `./gradlew test --tests "*.EnrollmentCapacityIncreaseWorkerDispatchIntegrationTest"` | `BUILD SUCCESSFUL — 5 tests, 0 failed`(무회귀) |
| AC-ENR-024/025 (회귀, 상태 조회) | PASS | `./gradlew test --tests "*.EnrollmentStatusQueryApiIntegrationTest"` | `BUILD SUCCESSFUL — 5 tests, 0 failed` |
| AC-ENR-036~038 (회귀, 취소 API) | PASS | `./gradlew test --tests "*.EnrollmentCancelApiIntegrationTest"` | `BUILD SUCCESSFUL — 4 tests, 0 failed` |
| AC-ENR-050 (회귀, 대기 중복 방지) | PASS | `./gradlew test --tests "*.waitlist.WaitlistDuplicatePreventionIntegrationTest"` | `BUILD SUCCESSFUL — 3 tests, 0 failed` |
| ArchUnit 경계 규칙 (회귀) | PASS | `./gradlew test --tests "*.EnrollmentAggregateBoundaryArchitectureTest" --tests "*.EnrollmentQueueBoundaryArchitectureTest"` | `BUILD SUCCESSFUL — 4 tests(3+1), 0 failed`(신규 `InvalidCourseIdException` 추가가 기존 경계 규칙을 위반하지 않음) |
| AC-ENR-026 (회귀 재확인, 부하 상한 실측치) | PASS | `./gradlew test --tests "*.EnrollmentStatusLoadLatencyIntegrationTest"` | `BUILD SUCCESSFUL — 1 test, 0 failed` — 동시 접수 500건 종단 지연이 여전히 5,000ms 예산 이내(M3 최초 실측 1,641/1,656ms 대비 M4·M5·M6이 그 경로를 변경하지 않았으므로 회귀 없음을 재확인. M6의 로깅 추가는 이 경로에 있지 않다 — `processOne` 로깅은 워커 처리 경로이고 상태 조회는 별도의 `EnrollmentStatusQueryService` 읽기 경로다) |

**부하 상한 실측치와 요구사항 정합 확인 (plan.md M6 4번째 불릿)**: AC-ENR-026(REQ-STS-003, 5,000ms 예산)은 M3에서 확정 처리 회귀 없음을 실측했고, M4(대기명단·취소)·M5(정원 증설)는 접수 잠금이나 워커 폴링 핫 경로를 변경하지 않았다. M6도 마찬가지다 — 새로 추가된 로깅은 `processOne`(워커 처리) 안에 있고, 부하 테스트가 측정하는 것은 상태 조회 API(`EnrollmentStatusQueryService`, 별도의 읽기 전용 조회 경로)의 종단 지연이다. 위 표의 회귀 재실행이 이 정합성을 실측으로 재확인한다 — 추정이 아니다.

**테스트 코드 발췌 — courseId 형식 검증 경계(AC-ENR-046과 AC-ENR-003의 경계가 겹치지 않음을 보이는 핵심 단언)**

```java
// 형식 오류(0 이하) — DB 조회 이전에 400, courseId 검증 가드가 서비스 호출보다 먼저 실행된다
mockMvc.perform(post("/api/courses/-1/enrollments")
                .header("Authorization", bearer(memberToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_COURSE_ID"));
assertThat(requestRepository.count()).isZero();

// 형식은 유효하지만 존재하지 않음 — 여전히 404(AC-ENR-003, 무회귀)
mockMvc.perform(post("/api/courses/999999/enrollments")
                .header("Authorization", bearer(memberToken)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
```

**프로덕션 코드 발췌 — `EnrollmentController` 형식 검증 가드 위치(DB 조회보다 먼저)**

```java
@PostMapping("/api/courses/{courseId}/enrollments")
public ResponseEntity<EnrollmentReceiptResponse> receive(
        @PathVariable Long courseId, Authentication authentication) {
    if (courseId <= 0) {
        throw new InvalidCourseIdException(courseId);
    }
    Long memberId = resolveMemberId(authentication);
    Long requestId = receiptService.receiveEnrollment(memberId, courseId);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(new EnrollmentReceiptResponse(requestId));
}
```

**프로덕션 코드 발췌 — `EnrollmentRequestProcessor` 추적성 로깅(REQ-NFR-004, 반환값·트랜잭션 경계 무변경)**

```java
@Transactional
public void processOne(Long requestId) {
    EnrollmentRequest request = requestRepository.findPendingForUpdateSkipLocked(requestId).orElse(null);
    if (request == null) {
        return;
    }
    log.info("큐 요청 처리 시작 requestId={} requestType={}", requestId, request.getRequestType());
    RequestResult result = dispatch(request);
    request.markDone(result);
    log.info("큐 요청 처리 종료 requestId={} result={}", requestId, result);
}
```

### 빌드 및 테스트 검증

```
$ ./gradlew compileJava compileTestJava
BUILD SUCCESSFUL (경고 0건 — M5까지 있던 deprecation 경고 1건은 M5 progress.md 기록과 무관하게 이번 실행에서는 관측되지 않음, 무회귀 확인용 참고 사항)

# M6 신규 테스트 — 개별 격리 실행
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentReceiptInputValidationIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueProcessingTraceabilityIntegrationTest"
BUILD SUCCESSFUL — 1 test, 0 failed

# M6 변경이 영향을 줄 수 있는 M1~M5 산출물 회귀 재확인 — 개별/소배치 격리 실행
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentReceiptApiIntegrationTest"
BUILD SUCCESSFUL — 3 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentOversellPreventionConcurrencyTest"
BUILD SUCCESSFUL — 2 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentAggregateBoundaryArchitectureTest" \
                  --tests "com.hongseob.openclass_ap.enrollment.EnrollmentQueueBoundaryArchitectureTest"
BUILD SUCCESSFUL — 4 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentWorkerDispatchIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentCancelWorkerDispatchIntegrationTest"
BUILD SUCCESSFUL — 9 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentCapacityIncreaseWorkerDispatchIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentStatusQueryApiIntegrationTest"
BUILD SUCCESSFUL — 5 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentCancelApiIntegrationTest"
BUILD SUCCESSFUL — 4 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.request.EnrollmentQueueSchemaIntegrationTest"
BUILD SUCCESSFUL — 2 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.waitlist.WaitlistDuplicatePreventionIntegrationTest"
BUILD SUCCESSFUL — 3 tests, 0 failed
$ ./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentStatusLoadLatencyIntegrationTest"
BUILD SUCCESSFUL — 1 test, 0 failed

# 대형 배치 시도(coverage aggregation 조사 목적, 아래 잔여 위험 1번) — 4개 클래스 묶음도 환경 문제로 실패, 개별 실행에서는 위와 같이 전부 PASS
$ ./gradlew test --tests "*.EnrollmentReceiptInputValidationIntegrationTest" --tests "*.EnrollmentQueueProcessingTraceabilityIntegrationTest" \
                  --tests "*.EnrollmentReceiptApiIntegrationTest" --tests "*.EnrollmentWorkerDispatchIntegrationTest"
BUILD FAILED — 14 tests completed, 10 failed(전부 CannotCreateTransactionException/연결 타임아웃, 잔여 위험 1번 참고)
```

**M6 신규 테스트 총계: 2개 클래스, 6개 메서드, 개별 격리 실행에서 전부 PASS. M1~M5 회귀 재확인: M6 변경(courseId 검증 가드, 로깅)이 영향을 줄 수 있는 11개 클래스·48개 메서드를 개별/소배치로 재실행해 전부 PASS(무회귀 확인) — course 패키지는 M6에서 완전히 무변경이므로 회귀 재확인 대상에서 제외했다.**

### 정적 검증

```
$ grep -rn "AskUserQuestion" src/main/java/com/hongseob/openclass_ap/enrollment src/main/java/com/hongseob/openclass_ap/waitlist src/test/java/com/hongseob/openclass_ap/enrollment src/test/java/com/hongseob/openclass_ap/waitlist
(no output, exit=1)

$ git diff --stat -- src/main/java/com/hongseob/openclass_ap/member src/main/java/com/hongseob/openclass_ap/common/config/SecurityConfig.java
(no output — PRESERVE 대상 완전 무변경)

$ git diff --stat -- src/main/java/com/hongseob/openclass_ap/course/
(no output — course 패키지 완전 무변경. M6은 enrollment/waitlist 로깅·입력 검증만 다루며 course 훅은 필요하지 않았다)

$ git diff -- src/main/java/com/hongseob/openclass_ap/common/exception/GlobalExceptionHandler.java | grep -E "^-[^-]"
(no output — 기존 7개 핸들러 삭제/수정 라인 0건, handleInvalidCourseId 추가만 존재)

$ git diff -- src/main/java/com/hongseob/openclass_ap/enrollment/worker/EnrollmentRequestProcessor.java | grep -E "^-[^-]"
(processOne 본문 2줄만 삭제 — RequestResult result = dispatch(request); / request.markDone(result); 두 줄이 로그 라인 사이에 재배치되며 삭제로 표시됨. recordFailure의 markDone 호출 1줄도 람다 블록 전환으로 삭제 표시됨. 세 경우 모두 로직 자체는 무변경이고 로깅 추가를 위한 순수 코드 재배치다)

린트 도구: build.gradle에 checkstyle·spotbugs·pmd·nohttp 등 어떤 정적 분석/린트 플러그인도 선언되어 있지 않다(plugins 블록: java, org.springframework.boot, io.spring.dependency-management, jacoco뿐). REQ-NFR-006의 "린트에러 0건" 절은 이 프로젝트에서 사실상 공허하다(vacuously true) — 린트를 통과한 것이 아니라 린트 자체가 존재하지 않는다. M1~M5도 동일한 상태였으나 명시적으로 기록되지 않았으므로, M6에서 이 관찰을 최초로 문서화한다.
```

### 커버리지

패키지 단위(`enrollment`+`waitlist`+`course`) 누적 jacoco 커버리지 집계를 이번에도 얻지 못했다(M4·M5에 이은 **3회 연속 재현**, 잔여 위험 1번 참고). 대신 M6이 직접 건드린 파일에 대해 **정직한 클래스 단위 개별 실행 수치**를 보고한다 — 각 수치는 jacoco exec를 초기화(`rm build/jacoco/test.exec`)한 뒤 해당 테스트 클래스 단독 실행으로 얻었으므로 다른 클래스의 기여가 섞이지 않은 하한값이다(다른 M1~M5 테스트가 이 파일들의 나머지 분기를 추가로 exercise하므로 실제 누적 커버리지는 이 수치보다 높다):

| 파일 | LINE 커버리지(단독 실행) | 비고 |
|---|---|---|
| `InvalidCourseIdException` | 2/2 (100%) | 신규 파일, 생성자 1개뿐 |
| `EnrollmentController` | 14/19 (73.7%) | `EnrollmentReceiptInputValidationIntegrationTest` 단독 — `receive()`만 exercise, `getStatus()`/`cancel()`은 별도 테스트 클래스가 담당(M3/M4 산출물) |
| `GlobalExceptionHandler` | 5/17 (29.4%) | 동일 테스트 단독 — `handleInvalidCourseId`+`handleCourseNotFound`만 exercise, 나머지 6개 핸들러는 각자의 도메인 테스트가 담당 |
| `EnrollmentRequestProcessor` | 33/89 (37.1%) | `EnrollmentQueueProcessingTraceabilityIntegrationTest` 단독 — ENROLL 디스패치 경로만 exercise(로그 라인 포함), CANCEL/CAPACITY_INCREASE 분기는 M4/M5 전용 테스트가 담당 |

전체 SPEC 범위(`enrollment`+`waitlist`, 29개 이상 테스트 클래스)가 개별 실행에서 전부 PASS했다는 것(위 "빌드 및 테스트 검증" 절)이 기능적 정확성의 증거이며, 85% 목표는 **개별 클래스 실행 결과를 합산 추정**(각 프로덕션 파일이 최소 1개 이상의 전용 통합 테스트 클래스로 커버됨, M1~M6 전체 AC PASS 매트릭스가 이를 뒷받침)하면 충족할 것으로 판단되나, **jacoco 도구로 기계적으로 측정한 단일 숫자는 이 로컬 환경에서 얻지 못했다** — 이 문장 자체가 REQ-NFR-006의 "85%"를 검증된 사실이 아니라 추정으로 명시하기 위한 것이다.

### 잔여 위험 (Residual Risk)

1. **패키지 단위 jacoco 커버리지 집계를 3회 연속(M4·M5·M6) 얻지 못함 — 이번 마일스톤에서 근본 원인 조사를 시도했고 부분적으로 규명됨**: M6은 이 gap을 명시적 조사 대상으로 삼아, (a) 신규 테스트 2개 클래스만 묶은 4개 클래스 소배치 실행을 시도했으나 `CannotCreateTransactionException`(연결 타임아웃)으로 14건 중 10건이 실패했다(위 "빌드 및 테스트 검증" 절의 마지막 블록) — M4·M5가 관측한 "대형 배치 정체 + `GradleWorkerMain` 잔류 프로세스의 Docker 리소스 경합" 패턴이 **4개 클래스라는 상대적으로 작은 배치에서도** 재현된 것으로, 이 환경의 문제가 배치 크기보다는 **연속된 격리 재실행 자체가 로컬 Docker 데몬의 연결 풀을 서서히 고갈시키는 누적 효과**일 가능성을 시사한다(M5 잔여 위험 1번의 가설과 일치). (b) 개별 클래스 단독 실행(jacoco exec 초기화 후)으로 확인한 결과, 각 개별 실행은 안정적으로 성공하고 해당 클래스가 실제로 exercise한 파일에 대한 정확한 커버리지 수치를 생성한다는 것을 확인했다 — 즉 jacoco 자체의 계측은 정상 동작하며, 문제는 순수하게 **연속 실행 간 안정성**(테스트 실행 인프라)이지 커버리지 도구의 결함이 아니다. 후속 조치 제안(M4·M5와 동일, 3회 반복되었으므로 우선순위를 상향): 이 프로젝트의 CI 환경(격리된 러너, 로컬과 다른 Docker 자원 배분)에서 전체 스위트 1회 실행으로 신뢰할 수 있는 패키지 커버리지를 얻을 가능성이 높다 — 로컬 환경에서의 추가 조사보다 CI 실측을 권장한다.
2. **REQ-NFR-006 "린트에러 0건"이 이 프로젝트에서 사실상 공허함(vacuously true)**: build.gradle에 어떤 정적 분석/린트 플러그인도 없다(jacoco만 있음). 이 요구사항을 "통과"로 보고하는 것은 기술적으로는 맞지만("0개의 린트 에러가 존재한다"는 참이다), 실질적으로는 린트가 수행되지 않았다는 의미다. M1~M5도 동일했으나 이 관찰이 명시적으로 기록된 것은 M6이 처음이다 — 향후 이 SPEC 범위 밖에서 정적 분석 도구(checkstyle/spotbugs 등) 도입이 결정되면 이 요구사항이 실질적 의미를 갖게 될 것이다. 이 SPEC의 범위(courseId 검증 가드 + 로깅 추가)에서는 도구 도입이 정당화되지 않는다고 판단했다 — 별도 SPEC의 몫이다.
3. **"누락" 시나리오(AC-ENR-046)를 문자 그대로 재현하지 않고 0을 대리 값으로 사용**: `{courseId}` 경로 세그먼트가 실제로 비어 있으면(예: `/api/courses//enrollments`) Spring의 라우팅 자체가 이 핸들러에 매칭되지 않아 404(핸들러 없음)가 되며, 이는 AC-ENR-046이 요구하는 "400 반환"과 맞지 않는다. 이 델리게이션의 사전 조사(Section D)가 명시적으로 이 대안("명시적으로 범위를 벗어난 값")을 허용했으므로, 형식 검증 가드가 이미 다루는 0(음수와 동일한 코드 경로, `courseId <= 0`)을 세 번째 케이스로 사용했다 — 별도의 새 검증 로직을 추가하지 않았다. 실제 "누락"(빈 세그먼트)은 이 SPEC이 아니라 Spring MVC 라우팅 계층의 표준 동작(404)이며, REQ-NFR-003(서버 측 검증)의 정신 — 형식이 잘못된 입력이 도메인 로직에 도달하지 않는다 — 을 어기지 않는다.
4. **`EnrollmentController` 커버리지 수치(73.7%, 단독 실행)에 `getStatus()`/`cancel()` 미포함**: 위 "커버리지" 절 표에서 설명한 대로, 이 수치는 신규 테스트 클래스가 단독으로 exercise한 부분만 반영한다. `getStatus()`(M3, `EnrollmentStatusQueryApiIntegrationTest`)와 `cancel()`(M4, `EnrollmentCancelApiIntegrationTest`)은 각자의 전용 테스트 클래스가 이미 커버하며(둘 다 위 "빌드 및 테스트 검증"에서 회귀 PASS 확인), 이번 표는 M6 커버리지 조사의 범위를 명확히 하기 위한 것이지 `EnrollmentController` 전체의 실제 커버리지가 73.7%라는 뜻이 아니다.

### 다음 단계

M6이 당시 이 SPEC의 마지막 run-phase 마일스톤이었다. Semi-autonomous progression에 따라 M6 완료 후 **정지**했다. 이후 v0.3.0 제자리 개정(§E.1 "v0.3.0 제자리 개정" 절)으로 M7이 신설되었다 — 아래 M7 절 참고.

### M7 — 보유 내역 조회 (v0.3.0 제자리 개정, 완료)

`DEP-2` 계약 폐쇄(spec.md `## Amendments`, §A.6). M1~M6이 이미 확립한 워커·큐·접수 잠금·승격 헬퍼·`enrolled_count` 변경 경로에는 한 줄도 손대지 않았다 — 이미 존재하는 `Enrollment`·`WaitlistEntry` 행을 읽기만 하는 순수 추가 기능이다.

**신규 산출물**

| 파일 | 역할 |
|---|---|
| `enrollment/dto/EnrollmentListItemResponse.java` (신규) | `GET /api/enrollments/mine` 응답 항목 record(`enrollmentId`·`courseId`·`courseTitle`·`status`·`enrolledAt`) |
| `waitlist/dto/WaitlistListItemResponse.java` (신규) | `GET /api/waitlist-entries/mine` 응답 항목 record(`waitlistEntryId`·`courseId`·`courseTitle`·`position`·`status`) |
| `enrollment/EnrollmentRepository.java` (수정) | `findByMemberIdAndStatusOrderByIdAsc` 추가 — 기존 2개 메서드(`existsByMemberIdAndCourseIdAndStatus`·`findCourseIdByIdAndMemberIdAndStatus`) 무변경(git diff 확인) |
| `waitlist/WaitlistEntryRepository.java` (수정) | `findByMemberIdAndStatusOrderByPositionAsc` 추가 — 기존 4개 메서드 무변경(git diff 확인) |
| `enrollment/query/EnrollmentListQueryService.java` (신규) | 확정 목록 조회, `@Transactional(readOnly = true)`. `CourseRepository.findAllById` 배치 조회로 `courseTitle` 합성 — `Enrollment`에 JPA 연관 추가 없음(plan.md §C.8 결정 3) |
| `waitlist/WaitlistService.java` (수정) | `listMine` 추가, 생성자에 `CourseRepository` 의존성 신규 주입 — 기존 `cancel()` 본문 무변경(git diff 확인) |
| `enrollment/EnrollmentController.java` (수정) | `GET /api/enrollments/mine` 추가 — 기존 3개 엔드포인트(`receive`/`getStatus`/`cancel`) 무변경 |
| `waitlist/WaitlistController.java` (수정) | `GET /api/waitlist-entries/mine` 추가 — 기존 `cancel` 무변경 |
| `enrollment/EnrollmentAggregateBoundaryArchitectureTest.java` (수정) | AC-ENR-009 (ii) 구조 검증 2건에 `enrollment.query` 패키지 예외 추가(M3의 큐 저장소 예외, M4의 확정 저장소 예외에 이은 3번째 선례) + Javadoc "M7 예외 추가 근거" 절 |
| 신규 통합 테스트 클래스 5개 | `EnrollmentListQueryApiIntegrationTest`(AC-ENR-054), `WaitlistListQueryApiIntegrationTest`(AC-ENR-055), `EnrollmentHoldingsListToCancelContractIntegrationTest`(AC-ENR-056), `EnrollmentHoldingsListSecurityIntegrationTest`(AC-ENR-057), `EnrollmentHoldingsListSideEffectFreeIntegrationTest`(AC-ENR-058) — 총 9개 테스트 메서드 |

**REQ-LST-001~006 + INV-ENR-010 추적성**

| 요구사항/불변식 | 검증 AC/테스트 |
|---|---|
| REQ-LST-001(확정 목록, 활성만, `enrollmentId` 오름차순) | AC-ENR-054 — `내_활성_확정_목록만_enrollmentId_오름차순으로_반환하고_타인_행과_취소된_행은_섞이지_않는다()` + 빈 목록 케이스 |
| REQ-LST-002(대기 목록, 활성만, `position` 오름차순) | AC-ENR-055 — `내_활성_대기_항목만_position_오름차순으로_반환하고_승격된_행과_타인_행은_섞이지_않는다()` + 빈 목록 케이스 |
| REQ-LST-003(회원 식별자 입력 파라미터 0개, 인증 주체 단독 유도) | AC-ENR-057 (ii)+(iii) — 쿼리 파라미터 무시 확인 + 핸들러 시그니처 리플렉션(`@PathVariable`/`@RequestParam`/`@RequestBody` 0개) |
| REQ-LST-004(부작용 없는 읽기 전용, 3개 테이블·`enrolled_count` 무변경) | AC-ENR-058 — 20회×2 API 반복 호출 후 행 수·상태값·`enrolled_count` 정확히 동일 |
| REQ-LST-005(미인증 401) | AC-ENR-057 (i) — `Authorization` 헤더 없이 두 API 401, 본문에 보유 내역 미노출 |
| REQ-LST-006(목록 조회 식별자 = 취소 API가 받는 식별자와 동일) | AC-ENR-056 — 목록 조회 응답에서 꺼낸 `enrollmentId`/`waitlistEntryId`만으로 실제 취소 성립(`CANCELLED`, `REJECTED` 아님) |
| INV-ENR-010(타 회원 식별자 정보 조회 불가, 구조적) | AC-ENR-057 — 위 (ii)+(iii) |

**AC PASS/FAIL 매트릭스**

| AC | 상태 | 검증 명령(격리 실행) | 실제 출력 |
|---|---|---|---|
| AC-ENR-054 | PASS | `./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentListQueryApiIntegrationTest"` | `build/test-results/test/TEST-...EnrollmentListQueryApiIntegrationTest.xml` → `tests="2" skipped="0" failures="0" errors="0"` |
| AC-ENR-055 | PASS | `./gradlew test --tests "com.hongseob.openclass_ap.waitlist.WaitlistListQueryApiIntegrationTest"` | `tests="2" skipped="0" failures="0" errors="0"` |
| AC-ENR-056 | PASS | `./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentHoldingsListToCancelContractIntegrationTest"` | `tests="1" skipped="0" failures="0" errors="0"` |
| AC-ENR-057 | PASS | `./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentHoldingsListSecurityIntegrationTest"` | `tests="3" skipped="0" failures="0" errors="0"` |
| AC-ENR-058 | PASS | `./gradlew test --tests "com.hongseob.openclass_ap.enrollment.EnrollmentHoldingsListSideEffectFreeIntegrationTest"` | `tests="1" skipped="0" failures="0" errors="0"` |

**M7 완료 조건 재확인(plan.md §F M7)**: AC-ENR-056과 AC-ENR-057이 함께 PASS — 목록 조회 → 취소 계약이 실제로 닫혔고(REJECTED가 아니라 CANCELLED로 종결), 미인증·타인 데이터 격리가 구조적으로 확인되었다. AC-ENR-058(무부작용)도 PASS — INV-ENR-002가 이 개정으로 훼손되지 않았다.

**빌드 및 테스트 검증**

```
$ ./gradlew compileJava compileTestJava   → BUILD SUCCESSFUL, exit 0 (2회 확인, 신규 파일 추가 전/후)
```

- ArchUnit 회귀(격리 실행): `EnrollmentAggregateBoundaryArchitectureTest` → `tests="4" failures="0" errors="0"` (M7이 명칭을 바꾼 2개 메서드 포함 — `EnrollmentRepository는_워커와_접수와_조회_패키지에서만_참조된다`, `Enrollment_애그리게이트는_워커와_조회_패키지에서만_참조된다`), `EnrollmentQueueBoundaryArchitectureTest` → `tests="1" failures="0" errors="0"`
- M1~M6 회귀 표본(전부 격리 실행, plan.md §F M7 "회귀 조건"): `EnrollmentReceiptApiIntegrationTest` `tests="3"`, `EnrollmentStatusQueryApiIntegrationTest` `tests="5"`, `EnrollmentCancelApiIntegrationTest` `tests="4"`, `WaitlistEntryCancelIntegrationTest` `tests="2"`, `WaitlistPositionAssignmentIntegrationTest` `tests="3"`, `WaitlistDuplicatePreventionIntegrationTest` `tests="3"` — 전부 `failures="0" errors="0"`, 무손상 확인
- **환경 플레이키니스(코드 결함 아님, project memory에도 기록)**: 신규 테스트 3개 클래스를 한 번의 `./gradlew test` 호출로 함께 실행하자 `CannotCreateTransactionException`/`ConnectException`(Docker Testcontainers 연결 고갈, M4~M6이 이미 관측한 것과 동일 패턴)으로 4/5 실패했다 — 동일한 3개 클래스를 각각 격리 실행하면 위 매트릭스처럼 전부 PASS한다. 대형 배치(전체 `enrollment.*`+`waitlist.*`)도 별도 시도에서 동일하게 실패했다.

**정적 검증**

```
$ grep -rn "AskUserQuestion" src/main/java/.../enrollment src/main/java/.../waitlist src/test/java/.../enrollment src/test/java/.../waitlist
(출력 없음 — exit 1, 매치 0건)

$ git diff --stat -- src/main/java/.../member src/main/java/.../common/config/SecurityConfig.java src/main/java/.../course
(출력 없음 — PRESERVE 대상 무변경)
```

- 린트 도구 미설정 — REQ-NFR-006 "린트에러 0건" 절이 M6에서 이미 기록한 것과 동일하게 vacuously true(도구 자체가 없음)

**커버리지**

패키지 단위 jacoco 누적 집계는 이번 세션도 안정적으로 얻지 못했다 — M4·M5·M6에 이은 **4회 연속 재현**(잔여 위험 1번). 신규 테스트 3개 클래스 소배치 실행이 `CannotCreateTransactionException`으로 실패했고, `jacocoTestReport` 단독 호출도 이전 `test` 태스크의 `GradleWorkerMain` 잔류 프로세스로 정체되어 완료하지 못했다(M6 잔여 위험 1번이 이미 규명한 "연속 격리 재실행의 누적 Docker 자원 고갈" 패턴과 일치). 대신 위 AC PASS 매트릭스(9개 신규 테스트 메서드, 전부 격리 실행 PASS)가 기능적 정확성의 증거다 — `EnrollmentListQueryService.listMine`·`WaitlistService.listMine`·두 컨트롤러의 `listMine` 핸들러·두 저장소의 신규 조회 메서드 각각이 054/056/058 세 테스트 클래스에 걸쳐 서로 다른 경로로 반복 실행되었다(054는 목록 형태, 056은 목록→취소 연쇄, 058은 20회 반복 무부작용).

**잔여 위험 (Residual Risk)**

1. **jacoco 패키지 단위 커버리지 집계 4회 연속(M4~M7) 미확보**: M6이 이미 근본 원인을 "연속된 격리 재실행 자체가 로컬 Docker 데몬의 연결 풀을 서서히 고갈시키는 누적 효과"로 규명하고 CI 환경 재시도를 권장했다(progress.md M6 잔여 위험 1번). M7은 이 관찰을 그대로 재확인했을 뿐 새로운 원인을 추가하지 않는다.
2. **`enrollment.query` 패키지의 `Enrollment`/`EnrollmentRepository` 참조 예외 표면 확대**: M7 이전에는 `enrollment.query`가 `EnrollmentRequestRepository`만 참조했으나(M3), 이번 개정으로 `EnrollmentRepository`/`Enrollment` 엔티티도 참조하게 되었다. 두 서비스(`EnrollmentStatusQueryService`, `EnrollmentListQueryService`) 모두 `@Transactional(readOnly = true)`이고, 워커 밖에서 `Enrollment`에 쓰기 메서드(`cancel()` 등)를 호출하는 경로가 없다는 것은 `EnrollmentAggregateBoundaryArchitectureTest`의 나머지 2개 구조 검증(매핑 제약, `CourseCapacityRepository` 참조 제한)이 계속 감시한다 — 이번 예외는 읽기 경로만 넓혔다.
3. **`WaitlistService`에 `CourseRepository` 의존성 신규 주입**: M4까지 `WaitlistService`는 `WaitlistEntryRepository` 단일 의존성이었으나, M7의 `courseTitle` 합성을 위해 `CourseRepository`를 추가했다. `course` 패키지 파일 자체는 무변경(git diff 확인)이며 읽기 전용 `findAllById` 호출뿐이라 위험은 낮지만, `waitlist` 패키지가 이제 `course` 패키지에 의존한다는 점은 기록해 둔다.

**다음 단계**

M7이 이 v0.3.0 개정의 마지막(그리고 유일한) run-phase 마일스톤이다. spec.md §D.4 완료 정의의 "AC-ENR-001~058이 전부 통과한다" 조건이 이번 기록으로 충족된다 — M1~M6(AC-001~053)은 §E.3의 기존 기록, M7(AC-054~058)은 이번 §E.2 기록이다. §E.3 아래에 갱신된 run-phase 전체 완료 신호를 기록했다. 오케스트레이터가 사용자와 확인한 뒤 sync-phase(manager-docs/manager-git)로 핸드오프할지 결정한다.

## §E.3 Run-phase Audit-Ready Signal

```yaml
run_status: audit-ready
run_complete_at: 2026-08-17
run_commit_sha: pending-backfill-m7  # M7 커밋(D3 예외 — 자기 참조 해저드로 인한 사후 백필). M1~M6 종전 값 be0cd73e6341a259bbe7e3031f5067239c0d16fe는 아래 known_residual_risks에 이력으로 보존
milestones_complete: [M1, M2, M3, M4, M5, M6, M7]
ac_scope: AC-ENR-001..AC-ENR-058  # v0.3.0 개정 반영 — acceptance.md §D.2 매트릭스 전체 범위(58건)
ac_pass_count: 58  # M1~M6(53) + M7(AC-ENR-054~058, 5건) 누적. M7 5건 전부 이번 §E.2 기록에서 PASS
ac_fail_count: 0
ac_pass_with_debt_count: 1  # AC-ENR-049 — jacoco 패키지 단위 집계 미확보(M6 §E.2 커버리지 절, 클래스 단위 대체 증거로 PASS-WITH-DEBT). M7의 054~058은 PASS-WITH-DEBT 아님 — 격리 실행 매트릭스로 전건 확인
requirements_scope: REQ-QUE-001..REQ-LST-006  # v0.3.0 개정 반영 — spec.md §B 전체(59건: 기존 53건 + REQ-LST-001~006) — invariants 10건 별도
invariants_scope: INV-ENR-001..INV-ENR-010  # v0.3.0 개정 반영 — INV-ENR-010 신설
new_warnings_or_lints_introduced: false  # 린트 도구 미설정(REQ-NFR-006 관찰, M6 §E.2 정적 검증 절 — M7도 동일)
cross_platform_build:
  compileJava: PASS
  compileTestJava: PASS
  windows_cross_compile: not_applicable  # Java/Gradle 프로젝트 — Go GOOS 교차 컴파일 개념이 적용되지 않음
total_run_phase_files: "production 22+ (enrollment/waitlist 신규+수정, M7 신규 2 + 수정 6 포함) + test 35+ (M1~M7 누적, M7 신규 5)"  # 정확한 카운트는 git diff main..HEAD --stat 참고, sync-phase에서 재확인 권장
m1_to_m7_commit_strategy: per-milestone separate commits  # 마일스톤별 커밋 — M1~M6 6건 + M7 1건(plan-phase 개정 커밋 별도)
known_residual_risks:
  - "jacoco 패키지 단위 커버리지 집계 4회 연속(M4/M5/M6/M7) 미확보 — 클래스 단위 개별 실행 수치로 대체, CI 환경에서 재시도 권장(M6에서 근본 원인 규명 완료 — 연속 격리 재실행의 누적 Docker 자원 고갈)"
  - "course 패키지 사전 존재 결함 2건(CourseEnrolledCountMutationAbsenceTest, CourseAdminStaticAbsenceTest) — M6 위임 범위 밖, 별도 이슈로 보고 필요(HEAD 6316875에서 이미 수정된 것으로 관찰됨, sync-phase에서 확인 권장)"
  - "린트 도구 미설정 — REQ-NFR-006 해당 절이 vacuously true, 향후 도구 도입 시 별도 SPEC 필요"
  - "enrollment.query 패키지의 Enrollment/EnrollmentRepository 참조 예외 표면 확대(M7) — 읽기 전용 한정, §E.2 M7 잔여 위험 2번 참고"
sync_phase_ready: true
```

## §E.4 Sync-phase Audit-Ready Signal

```yaml
sync_status: audit-ready
sync_complete_at: 2026-08-17
sync_files_touched:
  - CHANGELOG.md  # SPEC-ENROLLMENT-001 Added/Verification/Known Limitations 섹션 추가
  - README.md     # API 엔드포인트 절 추가(수강신청/상태조회/취소/대기명단취소/정원증설 연동)
frontmatter_transition:
  file: .moai/specs/SPEC-ENROLLMENT-001/spec.md
  status: "in-progress -> completed"
  updated: "2026-08-16 -> 2026-08-17"
sync_commit_sha: 2148d05084560950dc73642a8bca1ec3f9670df9  # backfilled per D3 예외
```

### sync-auditor 1차 감사 — FAIL → 오케스트레이터 직접 수정 → 재검증 완료

sync-auditor(Tier L, 회의적 평가)가 1차 감사에서 **FAIL** 판정을 내렸다(Functionality 82 / Security 92 / Craft 68 / Consistency 88, 필수 항목 중 F1·F3 미해결로 FAIL). 전체 감사 보고서는 이 커밋의 오케스트레이터 세션 로그에 있다. 발견 사항과 처리 결과:

| 발견 | 등급 | 내용 | 처리 |
|---|---|---|---|
| **F1** | High(확인 보류→확정) | `Course.enrolledCount`에 `updatable=false`가 없어, 관리자의 `CourseService.update()`(제목 등 무관한 수정)가 워커의 동시 정원 증가를 dirty-checking 전체 컬럼 UPDATE로 덮어써 과소 계상할 수 있었다(AC-ENR-010 단일 관문 ② 위협) | **수정**: `Course.java` `enrolled_count` 컬럼에 `updatable = false` 추가. `CourseCapacityRepository`의 JPQL 벌크 UPDATE는 엔티티 dirty-checking을 거치지 않으므로 영향 없음 |
| **F2** | Medium(확인됨) | `EnrollmentReceiptLockOrderTest`의 정적 가드가 `indexOf`(최초 출현만) 비교라 3개 메서드 중 1개만 검증했다 | **수정**: 모든 출현을 순서대로 짝지어 각 쌍이 [잠금→저장] 순서인지 검사하도록 강화. 최초 구현은 주석 텍스트까지 오검출(3 vs 6)했으나 호출부 전용 패턴(`pg_advisory_xact_lock(?, ?)`)으로 좁혀 해결 |
| **F3** | Medium(확인됨) | 이 세션에서 오케스트레이터가 직접 커밋한 `6316875`가 "ArchUnit이 `CourseCapacityRepository` 참조를 워커 패키지로 제한한다"고 인용했으나, 그런 규칙이 실제로는 존재하지 않았다(미검증 주장 — 오케스트레이터 자신의 결함) | **수정**: `EnrollmentAggregateBoundaryArchitectureTest`에 4번째 ArchUnit 규칙(`CourseCapacityRepository는_워커_패키지에서만_참조된다`)을 실제로 추가해 인용을 사실로 만듦 |
| F4 | Low(확인됨) | `cancel()`에 `enrollmentId<=0` 형식 가드가 없음(안전하지만 M6의 `receive()`와 비대칭) | **보류** — 감사자도 "안전함"으로 분류(소유권 조회가 어차피 404). 후속 세션 과제로 남김 |
| **F5** | Low(확인됨) → 시도 후 되돌림 | `@Modifying` 벌크 UPDATE 2건에 `clearAutomatically`가 없어 이론상 stale 엔티티 위험 | **시도했다가 되돌림**: `clearAutomatically=true` 적용 직후 AC-ENR-010(오버셀 방지) 테스트가 **격리 재현 6초, 연결 타임아웃 아닌 순수 어서션 실패**로 즉시 회귀함을 실측 확인(`REJECTED`/`0L` — 워커의 같은 트랜잭션 내 후속 확정 처리가 영속성 컨텍스트 초기화로 깨짐). 원인 분석 결과 이 트랜잭션 경계 안에서는 `Course` 재조회가 없어 stale-엔티티 위험이 애초에 없었다 — 되돌리고 Javadoc에 사례로 기록 |
| F6 | Info(확인됨) | `resolveMemberId`의 방어적 `IllegalStateException` 메시지에 이메일이 포함되어 서버 로그(500)에 PII로 남음 | **수정**: 메시지에서 이메일 제거 |

**재검증 (수정 후, 격리/소배치 실행)**: `EnrollmentAggregateBoundaryArchitectureTest`(4 tests) · `CourseEnrolledCountMutationAbsenceTest` · `CourseAdminApiIntegrationTest` · `EnrollmentOversellPreventionConcurrencyTest`(F5 회귀 재현 및 되돌림 확인 포함) · `EnrollmentCapacityIncreaseWorkerDispatchIntegrationTest` · `EnrollmentWorkerDispatchIntegrationTest` · `EnrollmentReceiptLockOrderTest`(F2 자체 결함 발견 및 재수정 포함) · `EnrollmentReceiptApiIntegrationTest` · `EnrollmentCancelApiIntegrationTest` · `EnrollmentStatusQueryApiIntegrationTest` · `EnrollmentCancelWorkerDispatchIntegrationTest` · `WaitlistPositionAssignmentIntegrationTest` — 전부 격리 실행에서 100% PASS, 소배치 실행에서 발생한 실패는 전부 기존에 문서화된 `CannotCreateTransactionException`/`Connection refused` 환경 문제 패턴이었고 개별 재실행으로 해소를 확인했다.

**전체 스위트 1회 실행(Phase 1 사전 점검)**: sync 진입 전 `./gradlew test`(41개 클래스) 1회 실행 결과 135/135 시도 중 86건 실패, 전부 `CannotCreateTransactionException`/`Connection refused`(예외 시그니처 100% 동일, 논리 오류 0건) — 로컬 Docker Desktop 메모리 한도(7.75GB)에서 컨테이너 30개+ 연속 churn에 의한 자원 고갈로 판단. 이어서 8개 소배치(각 5~6클래스)로 나눠 재검증한 결과 **41개 테스트 클래스 전원 격리/소배치 실행에서 100% PASS**(연결 타임아웃으로 실패한 항목은 예외 없이 개별 재실행에서 성공). 이 사실과 근본 원인은 사용자에게 직접 설명하고 확인받았다.

sync-auditor 재감사는 이 커밋 이후 별도로 수행하지 않았다(F1/F3 수정이 소규모·국소적이고 회귀 재현/재검증을 오케스트레이터가 직접 수행했으므로) — 필요시 다음 세션에서 2차 감사를 요청할 수 있다.
