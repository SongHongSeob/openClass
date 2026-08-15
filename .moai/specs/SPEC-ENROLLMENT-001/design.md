---
id: SPEC-ENROLLMENT-001
title: "선착순 수강신청 큐·워커 — 설계"
version: "0.1.1"
status: draft
created: 2026-08-15
updated: 2026-08-15
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/enrollment"
lifecycle: spec-anchored
tags: "enrollment, design, queue, worker, concurrency"
tier: L
---

# SPEC-ENROLLMENT-001 — 설계 (design)

---

## §1 데이터 모델

`SPEC-AUTH-001`(`member`)과 `SPEC-COURSE-001`(`course`)이 만든 테이블 위에 3개 테이블을 추가한다.

| 테이블 | 핵심 컬럼 | 역할 |
|---|---|---|
| `enrollment_request` | `id`(BIGSERIAL = 순서값), `member_id`, `course_id`, `target_enrollment_id`, `request_type`, `state`, `result`, `requested_at`, `processed_at` | **큐 테이블** |
| `enrollment` | `id`, `member_id`, `course_id`, `status`, `enrolled_at`, `cancelled_at` | 확정 수강신청 |
| `waitlist_entry` | `id`, `member_id`, `course_id`, `position`, `status`, `created_at`, `promoted_at` | 대기명단 |

### 1.1 제약 조건 (기계적 방어선)

| 제약 | 대상 | 보장하는 불변식 |
|---|---|---|
| `CHECK (enrolled_count >= 0 AND enrolled_count <= capacity)` | `course` (SPEC-COURSE-001이 생성) | INV-ENR-001 |
| `(member_id, course_id)` 부분 유니크 인덱스 `WHERE status = 'ENROLLED'` | `enrollment` | INV-ENR-003 |
| `(course_id, position)` 부분 유니크 인덱스 `WHERE status = 'WAITING'` | `waitlist_entry` | INV-ENR-004 |
| **`(member_id, course_id)` 부분 유니크 인덱스 `WHERE status = 'WAITING'`** | `waitlist_entry` | **INV-ENR-009** |
| `(state, id)` 인덱스 | `enrollment_request` | 워커 클레임 쿼리 성능 |
| `request_type` / `state` / `result` 값 제약 (enum 또는 CHECK) | `enrollment_request` | §A.4 도메인, INV-ENR-006 |

부분 유니크 인덱스가 `status`로 필터링되는 이유: 취소된 수강신청은 이력으로 남아야 하고(감사 가능성), 취소 후 같은 강좌에 재신청할 수 있어야 하기 때문이다. `WHERE status = 'ENROLLED'` 조건이 이 두 요구를 동시에 만족시킨다.

**`waitlist_entry (member_id, course_id) WHERE status='WAITING'` 인덱스 (2차 감사 E1)**: `enrollment` 쪽에는 동일 회원 중복 확정을 막는 인덱스가 있었지만 `waitlist_entry` 쪽에는 대응물이 없었다. 그 결과 이미 대기자인 회원이 재신청하면 같은 강좌의 대기 순번을 2개 점유할 수 있었다 — `WHERE status='WAITING'` 필터 덕분에 **대기 취소 후 재신청·승격 완료 후 재신청은 여전히 허용**되므로(`CANCELLED`/`PROMOTED` 행은 인덱스 대상이 아니다) 정상 흐름을 막지 않는다. 이 인덱스는 REQ-WRK-007의 애플리케이션 검사에 대한 **기계적 backstop**이며, 두 방어선 모두 필요하다: 애플리케이션 검사는 사용자에게 `REJECTED`라는 의미 있는 결과를 돌려주고, 인덱스는 검사를 우회한 경로까지 막는다.

### 1.2 `target_enrollment_id`

`CANCEL` 요청이 어떤 확정 레코드를 취소하는지 가리킨다. `ENROLL`·`CAPACITY_INCREASE`에서는 NULL이다. 이 컬럼이 있어야 워커가 소유권을 재검증할 수 있다 (REQ-CNL-003).

---

## §2 정원 초과를 막는 3중 방어선

| 순서 | 방어선 | 성격 | 대응 |
|---|---|---|---|
| 1 | `Enrollment` 생성과 `enrolled_count` 변경 경로를 워커 1개소로 한정 | **구조적** — 동시 실행 자체가 존재하지 않으므로 경쟁 조건이 발생하지 않는다 | REQ-WRK-001/002, INV-ENR-002 |
| 2 | 워커가 같은 트랜잭션에서 `enrolled_count`를 읽고 `< capacity`일 때만 확정 + 증가 | 트랜잭션 내 검사 | REQ-WRK-004, REQ-WRK-010 |
| 3 | `course` CHECK 제약 | DB 최종 거부 — 애플리케이션 버그가 있어도 막힌다 | REQ-WRK-014 |

1차 방어선이 가장 강력하지만 **코드 구조에 의존**하므로 가장 쉽게 무너진다. 그래서 acceptance.md에서 정적 검색만으로 검증하지 않고 **DB 상태 단언 + 우회 시도 테스트**로 검증한다 (§5).

---

## §3 접수 순서 보장 — 접수 잠금

정합성 논증 전문은 research.md §4에 있다. 여기서는 코드 형태만 기록한다.

```
[접수 API 트랜잭션]
  BEGIN
    (1) 강좌 존재·인증 검증
    (2) pg_advisory_xact_lock(<course_id 기반 키>)   ← 순서값 할당보다 반드시 먼저
    (3) INSERT INTO enrollment_request (...)          ← 여기서 순서값 확정
  COMMIT                                              ← 잠금 자동 해제
```

**(2)와 (3)의 순서가 뒤바뀌면 보장이 전부 무너진다.** 이 순서 의존성은 코드만 봐서는 드러나지 않으므로, 해당 지점에 `@MX:ANCHOR` 주석으로 불변 계약을 남기고 acceptance.md AC-ENR-006/007이 기계적으로 검증한다.

잠금 키는 `course_id`에서 결정적으로 유도한다. 다른 용도의 권고 잠금과 충돌하지 않도록 네임스페이스를 분리한 2-인자 형태(`pg_advisory_xact_lock(classid, objid)`)를 사용한다.

---

## §4 워커 처리 흐름

### 4.1 폴링 루프 (트랜잭션 밖)

```
@Scheduled(fixedDelay = <설정값>)
poll():
    while (true):
        ids = claimNextBatch()          # 별도 트랜잭션, 읽기 전용
        if ids is empty: break
        for id in ids:
            processor.processOne(id)    # 빈 분리 → 프록시 경유 (research.md §5)
```

배치 전체를 한 트랜잭션으로 묶지 않는다. 1건 실패가 나머지를 롤백시키면 INV-ENR-005가 깨진다.

### 4.2 요청 1건 처리 (트랜잭션 1개)

```
@Transactional
processOne(requestId):
    row = SELECT ... WHERE id = :requestId AND state = 'PENDING'
          FOR UPDATE SKIP LOCKED
    if row is null: return                       # 이미 처리됨 → 멱등 (REQ-WRK-011)

    result = dispatch(row.request_type, row)     # §4.3
    row.state  = 'DONE'
    row.result = result
    row.processed_at = now()
COMMIT   # 도메인 변경 + 상태 전이가 원자적으로 함께 커밋 (REQ-WRK-010)
```

예외 발생 시: 트랜잭션이 롤백되어 도메인 변경이 사라진다. 그 뒤 **별도 트랜잭션**(`REQUIRES_NEW`)으로 `state='DONE', result='FAILED'`를 기록한다. 이 분리가 없으면 `FAILED` 기록까지 롤백되어 요청이 `PENDING`으로 남고 무한 재시도가 된다 (research.md §5).

### 4.3 요청 종류별 처리

**`ENROLL`**

```
if course.status == CLOSED:                        → CLOSED
# 중복 검사 3종 (REQ-WRK-007) — 3번이 2차 감사 E1에서 추가됨
if 이미 확정됨(status=ENROLLED)                     → REJECTED
   or 미처리 중복 요청 존재(state=PENDING)          → REJECTED
   or 활성 대기 항목 존재(status=WAITING)           → REJECTED
if course.enrolled_count < course.capacity:
    INSERT enrollment(status=ENROLLED)
    course.enrolled_count += 1                     → SUCCESS
else:
    INSERT waitlist_entry(position = 다음 순번)     → WAITLISTED
```

세 번째 검사가 빠지면: 이미 대기자가 된 회원의 이전 요청은 `state='DONE'`이므로 두 번째 검사에 걸리지 않고, 확정도 아니므로 첫 번째에도 걸리지 않는다. 그대로 `WAITLISTED` 분기로 내려가 **같은 회원이 같은 강좌의 대기 순번을 2개 점유**한다. 이는 평범한 사용자 행동(대기 상태에서 조급하게 재신청)만으로 도달 가능한 상태였다.

**`CANCEL`** — 감사 지적 D2의 핵심 수정

```
target = SELECT enrollment WHERE id = row.target_enrollment_id
if target is null or target.status != ENROLLED: → REJECTED
if target.member_id != row.member_id:           → REJECTED   # 2차 소유권 검증 (REQ-CNL-003)

target.status = CANCELLED; target.cancelled_at = now()
course.enrolled_count -= 1

# 마감 강좌에서는 취소만 하고 승격은 동결한다 (REQ-WL-011, spec.md §A.5)
if course.status == CLOSED:                     → CANCELLED   # 대기명단 순번은 그대로 보존

promoted = promoteNextEligible(course)          # 아래 승격 헬퍼 — 부적격 대기자는 건너뛴다
                                                → CANCELLED
```

**승격 헬퍼 `promoteNextEligible(course)` — 부적격 대기자 건너뛰기 (REQ-WL-009, 2차 감사 E1)**

```
while true:
    front = 가장 앞선 활성 대기자 (position 오름차순 1건)
    if front is null: return false                    # 승격 대상 없음

    if front.member_id 가 이 강좌에 이미 유효한 확정 보유:
        front.status = DUPLICATE                      # 종결 처리 — 다시 선두에 남지 않는다
        continue                                      # 다음 대기자로 진행 (예외를 던지지 않는다)

    INSERT enrollment(front.member_id, status=ENROLLED)
    course.enrolled_count += 1
    front.status = PROMOTED
    return true
```

**부적격 검사를 INSERT 앞에 두는 것이 핵심이다.** 검사 없이 곧장 INSERT하면 `enrollment (member_id, course_id) WHERE status='ENROLLED'` 부분 유니크 인덱스가 예외를 던지고, 그 예외가 `processOne` 트랜잭션 **전체를 롤백**시킨다. 결과는 요청이 `FAILED`로 종결되고 **취소가 소실되며 여유 정원이 재배정되지 않는 것**이고, 부적격 항목이 계속 대기 선두에 남아 이후의 모든 `CANCEL`이 같은 방식으로 실패한다 — 큐 선두가 영구히 막힌다. `DUPLICATE`로 종결시키는 것은 그 항목이 다음 라운드에서 다시 선두로 올라오지 않게 하기 위해서다.

`CAPACITY_INCREASE`도 같은 헬퍼를 사용한다. REQ-WRK-007의 3번 검사가 이 상태의 **발생**을 막고, 이 헬퍼가 이미 발생한 상태에서의 **복구**를 보장한다 — 두 층 모두 필요하다.

**여기가 이 SPEC에서 가장 중요한 설계 판단이다.** 취소로 인한 감소와 대기자 승격을 **같은 트랜잭션 안에서 연속으로** 수행한다. 이렇게 해야:

- `enrolled_count` 감소가 커밋된 시점에는 승격도 이미 커밋되어 있다.
- **여유 정원이 외부에 노출되는 시간 창이 존재하지 않는다.**
- 따라서 대기자보다 늦게 접수한 신규 `ENROLL` 요청이 그 자리를 가로챌 수 없다 (REQ-WL-004).

취소를 접수 API에서 처리하고 승격을 별도 큐 작업으로 미루는 설계(최초 설계)는 그 사이에 **틈**을 만든다. 감사에서 지적된 실패 시나리오가 정확히 그 틈이었다.

**`CAPACITY_INCREASE`**

```
# 마감 강좌에서는 어떤 승격도 하지 않는다 (REQ-ADX-005, spec.md §A.5)
if course.status == CLOSED:                     → CLOSED   # 대기명단 순번은 그대로 보존

promoted = 0
while course.enrolled_count < course.capacity:
    if not promoteNextEligible(course): break   # 부적격 대기자는 헬퍼가 건너뛴다 (REQ-WL-009)
    promoted += 1
→ promoted > 0 ? PROMOTED : NOOP
```

`CLOSED` 분기가 없으면, 관리자가 삭제(=`CLOSED` 전이, `SPEC-COURSE-001` REQ-ADM-008)한 강좌에 정원 증설 요청이 남아 있을 때 대기자가 **삭제된 강좌로 승격**된다. `CANCEL` 쪽도 동일한 경로를 갖는다 — spec.md §A.5가 두 요청의 정책과 그 채택 근거를 규범적으로 확정한다.

---

## §5 INV-ENR-002를 실제로 검증하는 방법

"확정 경로가 1개소"라는 **구조적** 불변식을 소스 검색(`grep "save"`)만으로 검증하면 다음 우회로가 전부 통과한다.

| 우회 경로 | 왜 `grep`에 안 잡히는가 |
|---|---|
| JPA 연쇄 저장 (`cascade = PERSIST/ALL`) | `course.getEnrollments().add(...)`만 있으면 `save` 호출 문자열이 없다 |
| `EntityManager.merge` | 다른 메서드명 |
| 변경 감지 (dirty checking) | 호출 자체가 없다 — 영속 상태 객체의 필드를 바꾸면 커밋 시 자동 UPDATE |
| 네이티브 쿼리 / JDBC | 문자열 SQL |

따라서 3층으로 검증한다 (acceptance.md AC-ENR-008/009).

1. **매핑 제약** — `Course`에서 `Enrollment`로의 연쇄 저장 설정을 두지 않는다. JPA 메타모델을 읽어 `Enrollment`를 대상으로 하는 `CascadeType.PERSIST`/`ALL`이 0건임을 단언한다.
2. **아키텍처 규칙(ArchUnit)** — `EnrollmentRepository`와 `Enrollment` 애그리게이트를 참조할 수 있는 패키지를 워커 처리 패키지로 한정한다.
3. **행동 검증** — 접수·취소·관리자 API를 호출한 뒤 **워커를 돌리지 않은 상태**에서 `enrollment` 행 수와 `course.enrolled_count`가 **변하지 않았음**을 DB에서 직접 단언한다. 어떤 우회 경로를 쓰더라도 이 단언은 깨진다.

3번이 결정적이다. 1·2번은 알려진 우회로를 막고, 3번은 **모르는 우회로까지** 잡는다.

---

## §6 워커 처리량과 지연 목표

REQ-STS-003의 5초는 **종단(end-to-end)** 목표다 — "요청 접수 시점부터 종단 결과 확정까지". 따라서 예산은 **접수 측**과 **워커 측** 두 항을 모두 포함해야 한다.

| # | 항목 | 값 | 근거 |
|---|---|---|---|
| — | 폴링 주기 (`fixedDelay`) | 200ms | 아래 계산 |
| — | 1회 배치 크기 | 200건 | 아래 계산 |
| — | 이론 처리량 (워커) | 200건 / 200ms = **1,000건/초** | 처리 시간 0 가정 (낙관적 상한) |
| — | 보수적 처리량 (워커) | **500건/초** 가정 | 건당 처리 시간·트랜잭션 오버헤드 반영, 이론값의 50% |
| **A** | **접수 측 직렬화 지연** (§3 접수 잠금) | **추정 ≤ 0.5초** (500건 × 건당 ≤ 1ms) | 동일 강좌의 500건 접수가 `pg_advisory_xact_lock`을 통과해야 하므로 **단일 파일로 직렬화**된다. 임계 구간은 INSERT 1건(research.md §126 "밀리초 미만")이지만 0이 아니며, 종단 예산의 독립 항이다. **미실측 — research.md §7 V5가 측정한다** |
| **B** | 워커 소진 지연 | 500 ÷ 500건/초 = 1.0초 | 큐에 쌓인 500건을 워커가 비우는 시간 |
| **C** | 폴링 대기 | 0.2초 | 최악의 경우 한 주기를 기다린다 |
| — | **종단 합계 (A+B+C)** | ≈ **1.7초** | 5초 목표 대비 여유 3.3초 |
| — | 부하 상한 (REQ-STS-003) | 동시 접수 **500건** | 위 합계가 목표 이내인 최대 부하 |

**최초 설계와의 차이 (감사 지적 D5)**: 최초 설계는 `fixedDelay=500ms` × 50건 = 100건/초였고, 부하 상한을 "정상 부하"라고만 적어 정량화하지 않았다. 그 구성에서 3,000건이 몰리면 마지막 요청은 30초가 걸려 5초 목표를 6배 초과한다 — **문서화된 설정 자체가 요구사항을 위반**하고 있었다.

지금은 (a) 처리량을 10배로 올리고, (b) 부하 상한을 500건으로 **명시**하고, (c) 그 상한을 실제로 측정하는 AC를 둔다 (AC-ENR-026).

**A항이 이 표에 추가된 경위 (2차 감사 E4)**: §3의 접수 잠금은 D1(접수 순서 보장) 해결책으로 도입되었는데, 그 부작용으로 **동일 강좌의 접수가 직렬화되는 새로운 지연 항**이 생겼다. 최초 표는 워커 측(B·C)만 예산에 넣어 REQ-STS-003이 정의한 종단 범위와 어긋나 있었다. A항은 아직 **계산이 아니라 추정**이며, V5 실측 전까지는 가설이다.

**상한 초과 시 동작**: 지연은 큐 깊이에 비례해 선형 증가한다. 예: 동시 2,000건 → 약 4초(B) + 0.2초(C) + A항. 5,000건 → 약 10초 (목표 초과). 이 특성을 API 문서에 명시한다 (REQ-STS-003 후단). A항 역시 동일 강좌 부하에 비례하므로, 단일 인기 강좌에 부하가 집중될수록 A항의 비중이 커진다 — V5 실측 시 이 비례 관계를 함께 확인한다.

**실측 우선 원칙**: 위 숫자는 계산값이다. research.md §7 V5/V6에서 실측한 뒤, 실측치가 어긋나면 **요구사항의 숫자를 실측에 맞게 개정한다.** 측정을 요구사항에 맞추지 않는다.

---

## §7 패키지 구조

```
com.hongseob.openclass_ap
├── common/                 # SPEC-AUTH-001 소유
├── member/                 # SPEC-AUTH-001 소유
├── course/                 # SPEC-COURSE-001 소유
├── enrollment/
│   ├── Enrollment.java, EnrollmentStatus.java, EnrollmentRepository.java
│   ├── request/            # EnrollmentRequest(큐), RequestType, RequestState,
│   │                       # RequestResult, EnrollmentRequestRepository
│   ├── receipt/            # EnrollmentReceiptService (접수 잠금 + 큐 적재)
│   ├── worker/             # EnrollmentQueueWorker(폴링) + EnrollmentRequestProcessor(1건 처리)
│   │                       # ← Enrollment 생성·enrolled_count 변경은 여기서만
│   ├── query/              # EnrollmentStatusQueryService (읽기 전용)
│   ├── EnrollmentController.java   # 접수 / 상태 조회 / 취소
│   └── dto/
└── waitlist/
    ├── WaitlistEntry.java, WaitlistStatus.java, WaitlistRepository.java
    └── WaitlistService.java        # 순번 관리·조회만. 확정 생성 권한 없음
```

**폴링과 1건 처리를 다른 빈으로 분리한 이유**: 같은 빈 내부 호출은 Spring 프록시를 거치지 않아 `@Transactional`이 적용되지 않는다 (research.md §5). 이 분리는 스타일이 아니라 **정확성 요구**다.

`waitlist`는 순번 관리와 조회만 담당하고 `Enrollment`를 생성하지 않는다 — 승격 시 확정 레코드를 만드는 것은 워커다 (INV-ENR-002).

---

## §8 API 계약

| 메서드 | 경로 | 인가 | 응답 |
|---|---|---|---|
| POST | `/api/courses/{courseId}/enrollments` | 인증 | 202 + 요청 식별자 (큐 적재만) |
| GET | `/api/enrollment-requests/{requestId}` | 인증 + **본인** | 클라이언트 노출 상태 + 대기 순번 |
| DELETE | `/api/enrollments/{enrollmentId}` | 인증 + **본인** | 202 + `CANCEL` 요청 식별자 |
| DELETE | `/api/waitlist-entries/{entryId}` | 인증 + **본인** | 200 (큐 미경유 — `enrolled_count` 불변) |

**대기 취소가 큐를 경유하지 않는 이유**: 대기명단 항목의 취소는 `enrolled_count`를 건드리지 않는다. 큐를 경유해야 하는 것은 **정원 카운터를 바꾸는 작업뿐**이며, 불필요하게 큐를 태우면 지연만 늘어난다.

**소유권 검증 위치 (감사 지적 D12)**: 확정 취소는 **API 계층에서 1차**, **워커에서 2차**로 소유권을 검증한다. 2차 검증이 필요한 이유는 접수와 처리 사이에 시간 간격이 있고, 큐 행을 직접 조작하는 경로가 생길 경우를 대비하기 위해서다. 상태 조회와 대기 취소는 즉시 처리되므로 API 계층 검증 1회로 충분하다.

---

## §9 설계 대안 비교

| 결정 | 채택 | 기각한 대안 | 기각 사유 |
|---|---|---|---|
| 접수 순서 보장 | 강좌 단위 권고 잠금 | (a) 순서 정의를 `id`로 바꾸기 | 요구사항을 실패에 맞춰 약화 |
| | | (b) 가시성 워터마크 | 장수명 트랜잭션 1개가 큐 전체를 지연 |
| 취소·승격 | 워커에서 한 트랜잭션에 연속 수행 | 취소 API가 감소 + 별도 `PROMOTE` 작업 적재 | 여유 정원 노출 창 발생 → 신규 신청자가 대기자를 앞지름 (D2) |
| 큐 처리 상태 | `PENDING`/`DONE` 2종 | `PROCESSING` 중간 상태 추가 | 멈춘 `PROCESSING` 행 복구 스위퍼가 필요해진다. 행 잠금으로 충분 |
| 워커 트리거 | `@Scheduled(fixedDelay)` 폴링 | 이벤트 드리븐 트리거 | 재시작 시 유실 트리거 복구용 스위퍼가 결국 필요 → 폴링을 겸해야 함 |
| INV-ENR-002 검증 | 매핑 + ArchUnit + DB 상태 단언 | 소스 `grep "save"` | JPA 연쇄 저장·`merge`·변경 감지로 우회 가능 (D4) |
| 대기 취소 경로 | 큐 미경유 (즉시 처리) | 큐 경유 | `enrolled_count`를 바꾸지 않으므로 큐를 태울 이유가 없다 |

---

## §10 교차 참조

- 요구사항: `.moai/specs/SPEC-ENROLLMENT-001/spec.md`
- 기술 조사: `.moai/specs/SPEC-ENROLLMENT-001/research.md`
- 계획: `.moai/specs/SPEC-ENROLLMENT-001/plan.md`
- 인수 기준: `.moai/specs/SPEC-ENROLLMENT-001/acceptance.md`
