---
id: SPEC-COURSE-001
title: "강좌 엔티티·카탈로그 조회 및 관리자 강좌 관리"
version: "0.1.2"
status: draft
created: 2026-08-15
updated: 2026-08-16
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/course"
lifecycle: spec-anchored
tags: "course, catalog, admin, capacity, spring-boot"
tier: M
depends_on: [SPEC-AUTH-001]
---

# SPEC-COURSE-001 — 강좌 엔티티·카탈로그 조회 및 관리자 강좌 관리

## HISTORY

| 버전 | 날짜 | 작성자 | 변경 내용 |
|---|---|---|---|
| 0.1.0 | 2026-08-15 | manager-spec | 최초 작성 (draft) — SPEC-ENROLLMENT-001 3분할 중 2번 |
| 0.1.1 | 2026-08-15 | manager-spec | 2차 감사 지적 반영 — C1(REQ-CRS-005의 저장 계층 금지를 DB enum/CHECK 제약 + 우회 INSERT 단언으로 실제 검증), C2(§A.2 `enrolled_count` "항상 0" 문구를 코드 경로 범위로 정정), C3/E6(REQ-NFR-003에서 TDD 프로세스 절 분리 → plan.md §D 제약으로 이동, DoD 문구 정정) |
| 0.1.2 | 2026-08-16 | manager-spec | plan-auditor 1회차 FAIL(0.75) 결함 D1~D10 수정. spec.md 해당분: D4(REQ-CRS-004를 INV-CRS-003과 동일 강도로 강화 — "0이 아닌 값으로" 한정 제거, 리셋 포함 일체 변경 금지. 테스트 픽스처 예외는 §A.2 용어 정의로 이관), D5(REQ-ADM-005 정원 축소 거부 응답을 **409 단일 확정** — `GlobalExceptionHandler`의 도메인 규칙 위반 → 409 선례를 따름), D9(REQ-ADM-006에서 타 SPEC 미래 동작에 대한 `shall` 의무 제거 → §D 범위 제외의 교차 참조 주석으로 강등), D1(REQ-CAT-001에 상세 경로 비인증 접근 명시) |

---

## §A 개요

### A.1 배경

수강신청의 대상이 되는 **강좌(Course)** 를 정의하고, 누구나 조회할 수 있는 카탈로그 API와 관리자 전용 강좌 관리 API를 제공한다.

이 SPEC은 `SPEC-ENROLLMENT-001`(선착순 큐)이 사용할 **정원 스키마와 정원 제약**을 확정한다. 특히 `course.enrolled_count`(확정 인원) 컬럼과 그 DB CHECK 제약이 여기서 만들어진다. 다만 **이 SPEC의 범위에서는 확정 수강신청 자체가 존재하지 않으므로, 이 SPEC의 어떤 코드 경로도 `enrolled_count`를 변경하지 않는다** (테스트 픽스처가 애플리케이션 계층을 우회해 직접 설정하는 것은 별개이며, 잔여 정원 계산 검증에 필요하다). `enrolled_count`의 변경 권한은 `SPEC-ENROLLMENT-001`의 큐 워커가 **단독으로** 소유한다. 이 소유권 규칙을 여기서 미리 못 박아 두는 이유는, 나중에 "편의상" 강좌 서비스에서 카운터를 만지는 경로가 생기면 선착순 정합성의 핵심 방어선이 무너지기 때문이다.

### A.2 용어 정의

| 용어 | 영문 식별자 | 의미 |
|---|---|---|
| 강좌 | `Course` | 정원과 일정을 가진 수강 대상 단위 |
| 정원 | `capacity` | 확정 수강신청이 도달할 수 있는 상한. 1 이상의 정수 |
| 확정 인원 | `enrolled_count` | 현재 확정된 수강신청 수. **이 SPEC의 프로덕션 코드 경로는 이 값을 일체 변경하지 않는다** (REQ-CRS-004 / INV-CRS-003) |
| 잔여 정원 | — | `capacity - enrolled_count`. 저장하지 않고 조회 시 계산한다 |
| 모집 상태 | `CourseStatus` | `OPEN`(모집 중) 또는 `CLOSED`(모집 마감) |

> **테스트 픽스처 예외 (D4 — REQ-CRS-004의 예외가 아니라 적용 범위의 정의다)**: REQ-CRS-004와 INV-CRS-003이 금지하는 것은 **프로덕션 코드 경로**에서의 `enrolled_count` 변경이다. 테스트 픽스처가 `JdbcTemplate` 등으로 애플리케이션 계층을 **우회하여** 0이 아닌 값을 직접 INSERT/UPDATE하는 것은 이 금지의 대상이 아니며, 잔여 정원 계산·정원 축소 경계 검증에 필요하다 (AC-CAT-002, AC-ADM-004/005). 즉 "값이 항상 0"이 아니라 "값을 바꾸는 프로덕션 경로가 없다"가 요구사항의 내용이다. 이 예외를 요구사항 문구 안에 섞어 쓰면(예: "0이 아닌 값으로 변경하지 않는다") **0으로 되돌리는 편의 메서드**가 요구사항 문구는 통과하되 불변식은 위반하는 틈이 생기므로, 예외 서술은 요구사항이 아니라 이 용어 정의에 둔다.

### A.3 모집 상태 (client-facing enum)

| 상태 | 의미 | 신규 수강신청 접수 |
|---|---|---|
| `OPEN` | 모집 중 | 가능 |
| `CLOSED` | 모집 마감 (관리자 마감 또는 삭제 요청 결과) | 불가 — 처리 결과는 `SPEC-ENROLLMENT-001`이 정의 |

강좌는 **물리적으로 삭제되지 않는다.** 삭제 요청은 `CLOSED` 전이로 처리한다 (§B.3, INV-CRS-004).

---

## §B 요구사항 (GEARS)

### B.1 강좌 엔티티 및 스키마 (CRS)

- **REQ-CRS-001** (Ubiquitous) — 강좌 레코드는 식별자·강좌명·설명·정원·확정 인원·시작 일시·종료 일시·모집 상태·생성 일시를 보유 **shall**한다.
- **REQ-CRS-002** (Ubiquitous) — 데이터베이스는 정원이 1 미만인 강좌 행의 저장을 **제약 조건 수준에서** 거부 **shall**한다. 애플리케이션 검증만을 유일한 방어선으로 삼아서는 **shall not** 한다.
- **REQ-CRS-003** (Ubiquitous) — 데이터베이스는 `0 ≤ 확정 인원 ≤ 정원` 조건을 CHECK 제약으로 강제 **shall**하며, 이를 위반하는 갱신을 거부 **shall**한다.
- **REQ-CRS-004** (Ubiquitous) — 이 SPEC 범위의 어떤 프로덕션 코드 경로도 `enrolled_count` 값을 변경 **shall not**한다 — **설정(set)·증가·감소·0으로의 초기화(reset)를 모두 포함한다.** `enrolled_count`의 변경 권한은 `SPEC-ENROLLMENT-001`의 큐 워커가 단독 보유 **shall**한다. (적용 범위와 테스트 픽스처 취급은 §A.2 참조 — INV-CRS-003과 동일 강도다.)
- **REQ-CRS-005** (Ubiquitous) — 모집 상태는 §A.3의 두 값(`OPEN`, `CLOSED`) 중 하나 **shall**이며, 데이터베이스는 그 외의 값의 저장을 **제약 조건 수준에서**(enum 타입 또는 CHECK) 거부 **shall**한다. 애플리케이션 계층 검증만을 유일한 방어선으로 삼아서는 **shall not** 한다 — REQ-CRS-002/003과 동일한 방어선 구성이다.

### B.2 강좌 카탈로그 조회 (CAT)

- **REQ-CAT-001** (Ubiquitous) — 강좌 카탈로그 API는 인증 없이 목록과 상세를 조회할 수 있게 **shall**한다. **목록 경로(`/api/courses`)와 상세 경로(`/api/courses/{id}`) 양쪽 모두** `Authorization` 헤더 없이 접근 가능해야 하며, 상세 경로에 대해 인증을 요구 **shall not**한다 (검증: AC-CAT-001·AC-CAT-002).
- **REQ-CAT-002** (Event-driven) — **When** 강좌 목록 조회 요청이 도착하면, 카탈로그 서비스는 각 강좌의 식별자·강좌명·정원·확정 인원·잔여 정원·시작/종료 일시·모집 상태를 포함한 페이지네이션된 목록을 반환 **shall**한다.
- **REQ-CAT-003** (Event-driven) — **When** 존재하는 강좌 식별자로 상세 조회 요청이 도착하면, 카탈로그 서비스는 해당 강좌의 상세 정보와 계산된 잔여 정원을 반환 **shall**한다.
- **REQ-CAT-004** (Event-driven) — **When** 존재하지 않는 강좌 식별자로 조회 요청이 감지되면, 카탈로그 서비스는 404를 반환 **shall**한다.
- **REQ-CAT-005** (Ubiquitous) — 카탈로그 조회 API는 부작용 없는 읽기 전용 동작 **shall**이어야 하며, 호출 횟수에 따라 저장된 강좌 상태를 변경 **shall not**한다.
- **REQ-CAT-006** (Ubiquitous) — 카탈로그 목록은 모집 마감(`CLOSED`) 강좌도 모집 상태와 함께 반환 **shall**하며, 마감을 이유로 목록에서 숨기 **shall not**한다.

### B.3 관리자 강좌 관리 (ADM)

- **REQ-ADM-001** (State-driven) — **While** 인증된 요청자의 역할이 `ADMIN`이면, 관리자 강좌 API는 강좌 생성·수정·마감 요청을 허용 **shall**한다.
- **REQ-ADM-002** (State-driven) — **While** 인증된 요청자의 역할이 `ADMIN`이 아니면, 관리자 강좌 API는 요청을 처리하지 않고 403을 반환 **shall**한다.
- **REQ-ADM-003** (Event-driven) — **When** 관리자가 강좌명·정원·시작/종료 일시를 담아 생성을 요청하면, 관리자 API는 모집 상태 `OPEN`·확정 인원 0인 강좌를 생성하고 식별자를 반환 **shall**한다.
- **REQ-ADM-004** (Ubiquitous) — 관리자 API는 정원이 1 미만인 값으로의 강좌 생성 또는 수정을 거부 **shall**한다.
- **REQ-ADM-005** (Event-driven) — **When** 관리자가 강좌 정원을 **현재 확정 인원보다 작은 값**으로 축소하려는 것이 감지되면, 관리자 API는 해당 수정을 거부하고 **409**를 반환 **shall**한다. 기존 확정자를 강제 탈락시키 **shall not**한다. 응답 코드는 **409 단일 값으로 확정**한다 — 이 코드베이스의 `GlobalExceptionHandler`가 도메인 규칙 위반(중복 이메일)을 이미 409로 매핑하고 있으므로 그 선례를 따른다. 400은 이 경우에 허용되지 **shall not**한다 (후속 `SPEC-FRONTEND-001` 소비자를 위한 단일 계약).
- **REQ-ADM-006** (Event-driven) — **When** 관리자가 강좌 정원을 확정 인원 이상의 값으로 수정하면, 관리자 API는 새 정원 값을 반영 **shall**한다. 이 SPEC의 범위에는 대기명단도 승격 로직도 존재하지 않으므로, 정원이 증설되어도 확정 인원은 변경되지 않는다 (검증: AC-ADM-005).

  > 정원 증설에 따른 대기자 승격 처리 자체는 이 SPEC의 범위 밖이다 (§D 범위 제외 참조). 다른 SPEC의 미래 동작에 대한 의무는 이 SPEC의 산출물로 검증할 수 없으므로 요구사항으로 두지 않는다.
- **REQ-ADM-007** (Event-driven) — **When** 관리자가 강좌 마감을 요청하면, 관리자 API는 해당 강좌의 모집 상태를 `CLOSED`로 전이 **shall**한다.
- **REQ-ADM-008** (Ubiquitous) — 관리자 API는 강좌 행의 물리적 삭제(hard delete)를 수행 **shall not**하며, 삭제 요청을 모집 마감 상태 전이로 처리 **shall**한다.
- **REQ-ADM-009** (Event-driven) — **When** 존재하지 않는 강좌 식별자로 수정·마감·삭제 요청이 감지되면, 관리자 API는 404를 반환하고 어떤 강좌 행도 변경 **shall not**한다.

### B.4 비기능 요구사항 (NFR)

- **REQ-NFR-001** (Ubiquitous) — 모든 외부 입력은 서버 측에서 검증 **shall**되며, 클라이언트 측 검증만을 신뢰 **shall not**한다.
- **REQ-NFR-002** (Ubiquitous) — 이 SPEC의 모든 인수 기준은 **Spring Boot 테스트만으로 검증 가능** **shall**해야 하며, 프론트엔드 구현을 전제 **shall not**한다.
- **REQ-NFR-003** (Ubiquitous) — 이 SPEC의 산출물은 커밋당 최소 커버리지 80%·전체 목표 85%를 충족 **shall**하며, LSP 에러·타입에러·린트에러를 0건으로 유지 **shall**한다.

> **개발 방법론(TDD RED-GREEN-REFACTOR)은 요구사항이 아니라 프로세스 제약이다.** "테스트를 먼저 작성했는가"는 산출물 관찰로 검증할 수 없으므로 요구사항으로 두면 추적성 매트릭스가 AC가 제공하지 않는 커버리지를 보고하게 된다. 이 지시는 plan.md §D 제약 조건 표로 이동했다.

---

## §C 시스템 불변식 (Invariants)

| ID | 불변식 |
|---|---|
| INV-CRS-001 | 임의의 강좌에 대해 `0 ≤ 확정 인원 ≤ 정원`이 항상 성립하며, DB 제약이 이를 강제한다. |
| INV-CRS-002 | 저장된 모든 강좌의 정원은 1 이상이다. |
| INV-CRS-003 | 이 SPEC 범위의 코드에는 `enrolled_count`를 변경하는 경로가 존재하지 않는다. |
| INV-CRS-004 | 물리적으로 삭제된 강좌 행은 존재하지 않는다 — 삭제 요청은 `CLOSED` 전이로만 처리된다. |

---

## §D 범위 제외 (Exclusions)

아래 항목은 이 SPEC의 범위에 포함되지 않는다. 구현 중 "있으면 좋을 것 같아서" 추가하는 것을 명시적으로 금지한다.

### Out of Scope — 수강신청 도메인 전체

- 수강신청 접수 API, 큐 테이블, 큐 워커
- 대기명단 등록·승격, 취소 처리
- **정원 증설에 따른 대기자 승격 처리** — 이 SPEC은 정원 값 반영까지만 담당한다 (REQ-ADM-006). 승격 동작은 `SPEC-ENROLLMENT-001`이 다룰 예정이나, 그 SPEC의 동작에 대한 의무를 이 SPEC이 규정하지는 않는다 (교차 참조일 뿐 요구사항이 아니다)
- `enrolled_count` 증감 로직

  위 항목은 전부 `SPEC-ENROLLMENT-001`이 소유한다. 이 SPEC에서 `enrolled_count`를 변경하는 코드를 작성하는 것은 INV-CRS-003 위반이다.

### Out of Scope — 카탈로그 고도화

- 검색·필터·정렬 고도화 (v1은 단순 목록 + 페이지네이션)
- 강좌 카테고리 / 태그 / 강사 엔티티
- 강좌 이미지·첨부 파일 업로드
- 수강 이력 통계, 대시보드, 리포트

### Out of Scope — 인증 구현

- 회원 엔티티, 로그인, JWT 발급·검증 필터

  `SPEC-AUTH-001`이 소유한다. 이 SPEC은 그 필터 체인이 이미 동작한다고 전제하고, 관리자 경로(`/api/admin/**`)에 대한 인가 결과만 검증한다.

### Out of Scope — 프론트엔드 구현

- React 애플리케이션 스캐폴딩, 강좌 목록·상세 화면

  이 SPEC은 백엔드 API 계약까지를 범위로 한다. 후속 `SPEC-FRONTEND-001`(**아직 생성하지 않음**)이 이 API를 소비할 예정이다. 이 SPEC의 인수 기준은 React 없이 100% 검증된다.

### Out of Scope — 다중 지점 운영

- 다중 문화센터(지점) 멀티테넌시, 지점별 강좌 분리

---

## §E 성공 기준

1. 정원 1 미만 강좌가 애플리케이션 검증과 DB 제약 **양쪽**에서 거부된다 (INV-CRS-002).
2. `enrolled_count`를 정원 초과 값으로 직접 갱신하려 하면 DB 제약이 거부한다 (INV-CRS-001).
3. 비인증 클라이언트가 강좌 목록·상세를 조회할 수 있고, 잔여 정원이 정확히 계산된다.
4. `MEMBER` 역할로는 관리자 강좌 API에 접근할 수 없다(403).
5. 삭제 요청 후에도 강좌 행이 남아 있고 모집 상태만 `CLOSED`로 바뀐다 (INV-CRS-004).
6. 전체 테스트 커버리지 85% 이상, LSP 에러·타입에러·린트에러 0건.

---

## §F 참조

- 선행 SPEC: `.moai/specs/SPEC-AUTH-001/spec.md`
- 후속 SPEC: `.moai/specs/SPEC-ENROLLMENT-001/spec.md`
- 구현 계획: `.moai/specs/SPEC-COURSE-001/plan.md`
- 인수 기준: `.moai/specs/SPEC-COURSE-001/acceptance.md`
- 제품/구조/기술: `.moai/project/{product,structure,tech}.md`
