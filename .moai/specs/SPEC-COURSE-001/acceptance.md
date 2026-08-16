---
id: SPEC-COURSE-001
title: "강좌 엔티티·카탈로그 조회 및 관리자 강좌 관리 — 인수 기준"
version: "0.1.2"
status: completed
created: 2026-08-15
updated: 2026-08-16
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/course"
lifecycle: spec-anchored
tags: "course, catalog, admin, acceptance"
tier: M
---

# SPEC-COURSE-001 — 인수 기준 (acceptance)

## HISTORY

| 버전 | 날짜 | 작성자 | 변경 내용 |
|---|---|---|---|
| 0.1.0 | 2026-08-15 | manager-spec | 최초 작성 (draft) |
| 0.1.1 | 2026-08-15 | manager-spec | 2차 감사 지적 반영 — AC-CRS-005를 우회 INSERT/UPDATE 2계층으로 강화, AC-NFR-003 범위 주석 추가 |
| 0.1.2 | 2026-08-16 | manager-spec | plan-auditor 1회차 FAIL(0.75) 결함 D1~D10 수정. acceptance.md 해당분: D1(AC-CAT-002 Given에 "`Authorization` 헤더 없이" 전제 추가 + §D.2에서 REQ-CAT-001 → AC-CAT-001·AC-CAT-002로 정정), D2(AC-CRS-002/003/005의 검증 방법에 제약 생성 수단 `@Check`/`@ColumnDefault` 명시), D3(AC-CRS-004에서 **ArchUnit 절 제거** — 프로젝트 미도입 의존성이며 §D "신규 인프라 추가 금지"와 충돌. 정적 검색 + DB 상태 단언 2계층으로 확정), D5(AC-ADM-004 거부 응답을 409 단일 값으로 확정), D7(AC-CAT-001에 페이지 분할 경계 검증 추가 — 기존에는 메타데이터 "존재"만 단언했다), D9(AC-ADM-005에 승격 로직 부재의 정적 검증 추가) |

---

모든 인수 기준은 **자동화된 Spring Boot 테스트로 관찰 가능**해야 한다. 프론트엔드 없이 100% 검증된다.

검증 환경 전제: 통합 테스트는 **실제 PostgreSQL**(Testcontainers)에서 실행한다. CHECK 제약·유니크 제약의 실제 동작을 검증해야 하므로 H2 대체는 금지한다 (plan.md §D).

---

## §D.1 인수 기준

### 강좌 엔티티 및 스키마

**AC-CRS-001** — 강좌 필수 속성 보유 · 심각도: 필수
- **Given** 관리자가 강좌 1건을 생성했을 때
- **When** 저장된 `course` 행을 조회하면
- **Then** 식별자·강좌명·설명·정원·확정 인원·시작 일시·종료 일시·모집 상태·생성 일시 컬럼이 모두 존재하고 NULL이 아니다(설명 제외).
- 대응: REQ-CRS-001

**AC-CRS-002** — 정원 1 미만 DB 제약 거부 · 심각도: 필수
- **Given** Testcontainers PostgreSQL 스키마가 적용되어 있을 때
- **When** 애플리케이션 계층을 우회하여 `capacity = 0`인 행을 직접 INSERT하고, 이어서 `capacity = 1`인 행을 INSERT하면
- **Then** 전자는 DB 제약 위반 예외로 거부되고 후자는 성공한다 (정원 1이 허용 경계다).
- 검증 방법: Testcontainers PostgreSQL + `JdbcTemplate` 직접 INSERT. 제약의 **생성 수단**은 `Course` 엔티티의 `@org.hibernate.annotations.Check(name = "ck_course_capacity_min", constraints = "capacity >= 1")`이다 (plan.md §C.1.1) — 이 프로젝트는 마이그레이션 도구가 없어 평범한 JPA 애노테이션만으로는 CHECK 제약이 생성되지 않으므로, 이 AC의 통과는 `@Check`이 실제로 부착되었음을 의미한다.
- 대응: REQ-CRS-002, INV-CRS-002

**AC-CRS-003** — 확정 인원 CHECK 제약 · 심각도: 필수
- **Given** 정원 5, 확정 인원 5인 강좌 행이 직접 INSERT되어 있을 때
- **When** 애플리케이션 계층을 우회하여 (i) `enrolled_count = 6`, (ii) `enrolled_count = -1`로 각각 UPDATE를 시도하면
- **Then** 두 시도 모두 DB 제약 위반 예외로 거부되고, 행의 `enrolled_count`는 5로 유지된다.
- 검증 방법: Testcontainers PostgreSQL + `JdbcTemplate` 직접 UPDATE. 제약의 **생성 수단**은 `@Check(name = "ck_course_enrolled_range", constraints = "enrolled_count >= 0 AND enrolled_count <= capacity")`이며, 컬럼 기본값은 `@ColumnDefault("0")`로 생성한다 (plan.md §C.1.1).
- 대응: REQ-CRS-003, INV-CRS-001

**AC-CRS-004** — 확정 인원 변경 경로 부재 · 심각도: 필수
- **Given** 이 SPEC 범위의 구현 코드베이스가 주어졌을 때
- **When** (i) 강좌 생성·수정·마감·삭제·조회 API를 각각 1회씩 호출한 뒤 모든 `course` 행의 `enrolled_count`를 확인하고, (ii) 프로덕션 소스에서 `enrolled_count` 필드에 대한 세터·증감 연산 호출 지점을 정적 검색하면
- **Then** (i) 모든 행의 `enrolled_count`가 0이고, (ii) 정적 검색 결과가 0건이다 (엔티티 필드 선언과 JPA 읽기 매핑은 제외). 정적 검색은 **설정·증가·감소·0으로의 초기화를 모두** 대상으로 한다 (REQ-CRS-004).
- 검증 방법: **2계층 — 통합 테스트(DB 상태 단언) + 프로덕션 소스 정적 검색(grep/ast-grep).** ArchUnit은 사용하지 않는다: 이 프로젝트에 미도입 의존성이고 plan.md §D "신규 인프라 추가 금지"(사용자 명시적 결정)와 충돌하며, 이 AC가 검증할 명제는 "변경 경로가 0건"이라는 **부재 명제**여서 정적 검색으로 충분히 관찰된다 (패키지 스코프 규칙이 아니다). 근거 전문은 plan.md HISTORY 0.1.2 결정 근거 D3 참조.
- 구조적 뒷받침: 이 AC는 plan.md §C.4.1-2의 엔티티 설계(`private` 생성자 + 정적 팩토리, `@Setter` 부재, `enrolled_count` 변경 메서드 미정의)가 지켜지면 자연히 0건이 된다 — 사후 검사가 아니라 설계가 1차 방어선이다.
- 대응: REQ-CRS-004, INV-CRS-003

**AC-CRS-005** — 모집 상태 도메인 제한 (2계층) · 심각도: 필수
- **Given** 강좌 1건이 존재할 때
- **When** (i) 애플리케이션 API를 통해 모집 상태를 `OPEN`/`CLOSED` 이외의 값(`ARCHIVED` 등)으로 설정하려 시도하고, (ii) **애플리케이션 계층을 우회하여** `status = 'ARCHIVED'`인 행을 직접 INSERT하고, 이어서 기존 행의 `status`를 `'ARCHIVED'`로 직접 UPDATE하면
- **Then** (i)은 애플리케이션 계층에서 거부되고(400 또는 매핑 예외), (ii)의 INSERT·UPDATE는 **DB 제약 위반 예외로 거부**된다. 저장된 값은 §A.3의 두 값 중 하나로 유지된다.
- 근거: REQ-CRS-005는 "저장 shall not" — 즉 저장 계층의 금지다. 애플리케이션 계층 단언만으로는 우회 경로가 열려 있어도 통과한다. 형제 요구사항 REQ-CRS-002/003이 이미 우회 INSERT/UPDATE로 검증되므로(AC-CRS-002/003) 동일 패턴을 맞춘다.
- 검증 방법: 통합 테스트(애플리케이션 API) + `JdbcTemplate` 직접 INSERT/UPDATE (Testcontainers PostgreSQL). DB 측 제약의 **생성 수단**은 `@Check(name = "ck_course_status", constraints = "status IN ('OPEN','CLOSED')")`이며, 컬럼 기본값은 `@ColumnDefault("'OPEN'")`로 생성한다 (plan.md §C.1.1).
- 대응: REQ-CRS-005

### 강좌 카탈로그 조회

**AC-CAT-001** — 비인증 목록 조회 및 페이지 분할 경계 · 심각도: 필수
- **Given** 강좌 3건이 등록되어 있고 클라이언트가 `Authorization` 헤더 없이 요청할 때
- **When** (i) 강좌 목록 API를 기본 파라미터로 호출하고, 이어서 (ii) **`size=2`로 1페이지와 2페이지를 각각 호출**하면
- **Then** (i)은 200과 함께 3건이 반환되고, 각 항목에 식별자·강좌명·정원·확정 인원·잔여 정원·시작/종료 일시·모집 상태가 포함되며, 응답에 페이지네이션 메타데이터(전체 건수·전체 페이지 수·현재 페이지)가 존재한다. (ii)는 **1페이지가 정확히 2건, 2페이지가 정확히 1건**을 반환하고, 두 페이지 항목의 식별자 합집합이 전체 3건의 식별자 집합과 정확히 일치하며(중복·누락 없음), 전체 건수 메타데이터가 두 응답 모두에서 3이다.
- 근거: 메타데이터의 **존재**만 단언하면 페이지네이션 파라미터가 실제로 무시되고 항상 전체가 반환되는 구현도 통과한다. 경계(`size` × 페이지 수 ≠ 전체 건수) 검증이 REQ-CAT-002의 "페이지네이션된 목록"을 실제로 관찰하는 유일한 방법이다.
- 대응: REQ-CAT-001, REQ-CAT-002

**AC-CAT-002** — 비인증 상세 조회 및 잔여 정원 계산 · 심각도: 필수
- **Given** 정원 10, 확정 인원 4(테스트가 직접 INSERT)인 강좌가 존재하고, **클라이언트가 `Authorization` 헤더 없이 요청할 때**
- **When** 상세 조회 API(`GET /api/courses/{id}`)를 호출하면
- **Then** **401이 아닌 200**이 반환되고 잔여 정원 6이 함께 반환되며, `course` 테이블에 잔여 정원을 저장하는 컬럼이 존재하지 않는다 (계산 값이다).
- 근거: 무헤더 전제가 없으면 `SecurityConfig`의 공개 매처가 목록 경로에만 걸려 있어도(현 상태) 이 AC가 통과해 버린다 — 상세 경로의 비인증 접근이 실제로 관찰되지 않는다. 매처 확장은 plan.md §C.2.1 / M2 참조.
- 대응: REQ-CAT-001, REQ-CAT-003

**AC-CAT-003** — 존재하지 않는 강좌 조회 · 심각도: 필수
- **Given** 존재하지 않는 강좌 식별자가 주어졌을 때
- **When** 상세 조회를 요청하면
- **Then** 404가 반환된다.
- 대응: REQ-CAT-004

**AC-CAT-004** — 조회의 무부작용성 · 심각도: 필수
- **Given** 강좌 3건이 존재할 때
- **When** 목록·상세 조회를 각각 20회 반복 호출하면
- **Then** `course` 테이블의 행 수와 모든 행의 컬럼 값이 최초 조회 시점과 동일하다.
- 대응: REQ-CAT-005

**AC-CAT-005** — 마감 강좌 노출 · 심각도: 필수
- **Given** `OPEN` 강좌 2건과 `CLOSED` 강좌 1건이 존재할 때
- **When** 강좌 목록 API를 호출하면
- **Then** 3건이 모두 반환되고, `CLOSED` 강좌의 모집 상태 필드 값이 `CLOSED`로 표시된다.
- 대응: REQ-CAT-006

### 관리자 강좌 관리

**AC-ADM-001** — ADMIN 강좌 생성 · 심각도: 필수
- **Given** `ADMIN` 역할 토큰으로 인증했을 때
- **When** 강좌명·정원 10·시작/종료 일시를 담아 생성 API를 호출하면
- **Then** 201과 식별자가 반환되고, 생성된 `course` 행의 모집 상태가 `OPEN`이며 확정 인원이 0이다.
- 대응: REQ-ADM-001, REQ-ADM-003

**AC-ADM-002** — 비관리자 접근 차단 · 심각도: 필수
- **Given** `MEMBER` 역할 토큰으로 인증했을 때
- **When** 관리자 강좌 생성·수정·마감·삭제 API를 각각 호출하면
- **Then** 모두 403이 반환되고 `course` 테이블의 행 수와 내용이 변하지 않는다.
- 대응: REQ-ADM-002

**AC-ADM-003** — 정원 1 미만 거부 · 심각도: 필수
- **Given** `ADMIN`으로 인증했을 때
- **When** 정원 0 또는 음수로 강좌 생성 및 수정을 요청하면
- **Then** 400이 반환되고 어떤 강좌 행도 생성·변경되지 않는다.
- 대응: REQ-ADM-004

**AC-ADM-004** — 확정 인원 미만 정원 축소 거부 (경계 포함) · 심각도: 필수
- **Given** 정원 10, 확정 인원 7(테스트가 직접 INSERT)인 강좌가 있을 때
- **When** (i) 정원을 6으로 축소 요청하고, 이어서 (ii) 정원을 정확히 7로 축소 요청하면
- **Then** (i)은 **409**로 거부되고 정원이 10으로 유지되며, (ii)는 성공하여 정원이 7이 된다. 두 경우 모두 확정 인원 7은 변하지 않는다 (확정자 강제 탈락 없음).
- 상태 코드 단언은 **409 단일 값**이다 — 400은 이 경우에 허용하지 않는다. 근거: `GlobalExceptionHandler`가 도메인 규칙 위반(중복 이메일)을 이미 409로 매핑하는 선례를 따르며, 400은 입력 형식 오류(정원 1 미만 → AC-ADM-003)에만 쓴다. 두 코드를 모두 허용하면 후속 `SPEC-FRONTEND-001` 소비자에게 모호한 계약이 된다 (plan.md §C.3).
- 대응: REQ-ADM-005

**AC-ADM-005** — 정원 증설 반영, 승격 로직 부재 · 심각도: 필수
- **Given** 정원 2, 확정 인원 2(테스트가 직접 INSERT)인 강좌가 있을 때
- **When** (i) `ADMIN`이 정원을 4로 수정하고, (ii) 프로덕션 소스에서 대기명단·승격 관련 식별자(`waitlist`, `promote`, `승격` 등)를 정적 검색하면
- **Then** (i)은 200이 반환되고 정원이 4가 되며 확정 인원은 2로 **변하지 않는다**. (ii)의 정적 검색 결과가 **0건**이다 — 이 SPEC 범위에 대기명단도 승격 로직도 존재하지 않음이 관찰된다.
- 검증 범위 주석: 이 AC는 **이 SPEC의 산출물로 관찰 가능한 것만** 단언한다. `SPEC-ENROLLMENT-001`이 장차 승격을 어떻게 구현할지는 이 SPEC의 산출물로 검증할 수 없으므로 단언 대상이 아니다 (spec.md REQ-ADM-006 주석 / §D 범위 제외 참조).
- 검증 방법: 통합 테스트(DB 상태 단언) + 프로덕션 소스 정적 검색(grep/ast-grep)
- 대응: REQ-ADM-006

**AC-ADM-006** — 강좌 마감 전이 · 심각도: 필수
- **Given** `OPEN` 상태 강좌가 있을 때
- **When** `ADMIN`이 마감 API를 호출하면
- **Then** 200이 반환되고 해당 행의 모집 상태가 `CLOSED`가 되며, 행이 삭제되지 않는다.
- 대응: REQ-ADM-007

**AC-ADM-007** — 물리 삭제 금지 · 심각도: 필수
- **Given** 강좌 1건이 존재할 때
- **When** `ADMIN`이 삭제 API를 호출하면
- **Then** 해당 `course` 행이 여전히 존재하고 모집 상태만 `CLOSED`로 전이된다. 또한 프로덕션 소스 정적 검색에서 `course`에 대한 `delete`/`remove` 호출 지점이 0건이다.
- 검증 방법: 통합 테스트(행 존재 단언) + 소스 검색(grep/ast-grep)
- 대응: REQ-ADM-008, INV-CRS-004

**AC-ADM-008** — 존재하지 않는 강좌 변경 요청 · 심각도: 필수
- **Given** 존재하지 않는 강좌 식별자가 주어졌을 때
- **When** `ADMIN`이 수정·마감·삭제를 각각 요청하면
- **Then** 모두 404가 반환되고 `course` 테이블의 행 수와 내용이 변하지 않는다.
- 대응: REQ-ADM-009

### 비기능

**AC-NFR-001** — 서버 측 입력 검증 · 심각도: 필수
- **Given** `ADMIN`으로 인증했을 때
- **When** 강좌명 누락·종료 일시가 시작 일시보다 이른 값·정원이 정수가 아닌 값으로 생성을 요청하면
- **Then** 각 경우 400이 반환되고 강좌가 생성되지 않는다.
- 대응: REQ-NFR-001

**AC-NFR-002** — 백엔드 단독 검증 가능성 · 심각도: 필수
- **Given** 이 SPEC의 전체 테스트 스위트가 주어졌을 때
- **When** 프론트엔드 산출물이 존재하지 않는 저장소 상태에서 `./gradlew test`를 실행하면
- **Then** §D.1의 모든 필수 AC가 통과하고, 테스트 소스에 프론트엔드 빌드 산출물에 대한 의존이 존재하지 않는다.
- 대응: REQ-NFR-002

**AC-NFR-003** — 커버리지 및 정적 품질 · 심각도: 필수
- **Given** 전체 테스트를 실행했을 때
- **When** 커버리지 리포트와 LSP 진단을 확인하면
- **Then** 커버리지 ≥ 85%, LSP 에러 0건, 타입에러 0건, 린트에러 0건이다.
- 범위 주석: TDD 준수 여부(테스트를 먼저 작성했는가)는 산출물 관찰로 검증할 수 없으므로 이 AC의 대상이 아니다. 해당 지시는 plan.md §D의 프로세스 제약으로 관리한다.
- 대응: REQ-NFR-003

---

## §D.2 추적성 매트릭스 (요구사항 → 인수 기준)

범위 표기(`~`)를 사용하지 않고 요구사항 1건당 1행으로 나열한다.

| 요구사항 | 대응 인수 기준 |
|---|---|
| REQ-CRS-001 | AC-CRS-001 |
| REQ-CRS-002 | AC-CRS-002 |
| REQ-CRS-003 | AC-CRS-003 |
| REQ-CRS-004 | AC-CRS-004 |
| REQ-CRS-005 | AC-CRS-005 |
| REQ-CAT-001 | AC-CAT-001(목록·비인증), AC-CAT-002(상세·비인증) |
| REQ-CAT-002 | AC-CAT-001 (페이지 분할 경계 포함) |
| REQ-CAT-003 | AC-CAT-002 |
| REQ-CAT-004 | AC-CAT-003 |
| REQ-CAT-005 | AC-CAT-004 |
| REQ-CAT-006 | AC-CAT-005 |
| REQ-ADM-001 | AC-ADM-001 |
| REQ-ADM-002 | AC-ADM-002 |
| REQ-ADM-003 | AC-ADM-001 |
| REQ-ADM-004 | AC-ADM-003 |
| REQ-ADM-005 | AC-ADM-004 |
| REQ-ADM-006 | AC-ADM-005 |
| REQ-ADM-007 | AC-ADM-006 |
| REQ-ADM-008 | AC-ADM-007 |
| REQ-ADM-009 | AC-ADM-008 |
| REQ-NFR-001 | AC-NFR-001 |
| REQ-NFR-002 | AC-NFR-002 |
| REQ-NFR-003 | AC-NFR-003 |
| INV-CRS-001 | AC-CRS-003 |
| INV-CRS-002 | AC-CRS-002 |
| INV-CRS-003 | AC-CRS-004 |
| INV-CRS-004 | AC-ADM-007 |

요구사항 23건 + 불변식 4건 = 27건, 인수 기준 21건. 미대응 요구사항 0건.

REQ-CAT-001은 **목록 경로와 상세 경로 양쪽**의 비인증 접근을 요구하므로 AC 2건에 대응한다 (v0.1.2 / D1 — 이전에는 AC-CAT-001만 대응하여 상세 경로의 비인증 접근이 어떤 AC로도 검증되지 않았다).

---

## §D.3 완료 정의 (Definition of Done)

아래 항목이 **모두** 충족되어야 이 SPEC이 완료로 전이될 수 있다. §D.1의 모든 AC는 심각도 "필수"이므로 예외 없이 전부 통과해야 한다.

- [ ] §D.1의 AC-CRS-001 ~ AC-NFR-003이 **전부 통과**한다.
- [ ] AC-CRS-002·AC-CRS-003·**AC-CRS-005**(DB 제약 3종)이 실제 PostgreSQL 환경에서 통과한다 — 각각 우회 INSERT/UPDATE로 검증된다.
- [ ] §D.2 추적성 매트릭스의 요구사항 23건 + 불변식 4건이 모두 통과한 AC로 커버된다.
- [ ] 전체 테스트 커버리지 ≥ 85%, 커밋별 ≥ 80%.
- [ ] LSP 에러·타입에러·린트에러 0건, ast-grep 게이트 통과.
- [ ] spec.md §D(범위 제외) 항목이 구현되지 않았음이 확인된다 — 특히 `enrolled_count` 변경 경로와 대기명단 관련 코드가 0건임을 AC-CRS-004로 확인한다.
- [ ] 미해소 클래리피케이션 마커가 이 SPEC의 어느 산출물에도 남아 있지 않다.
- [ ] 선행 SPEC `SPEC-AUTH-001`이 `completed` 상태다.
