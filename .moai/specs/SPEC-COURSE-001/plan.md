---
id: SPEC-COURSE-001
title: "강좌 엔티티·카탈로그 조회 및 관리자 강좌 관리 — 구현 계획"
version: "0.1.1"
status: draft
created: 2026-08-15
updated: 2026-08-15
author: manager-spec
priority: P0
phase: "v1.0.0"
module: "src/main/java/com/hongseob/openclass_ap/course"
lifecycle: spec-anchored
tags: "course, catalog, admin, plan"
tier: M
---

# SPEC-COURSE-001 — 구현 계획 (plan)

> **읽는 순서 안내**: §C는 **되돌리기 어려운 순서**로 배치했다. §C.1(스키마)이 가장 비싸고, 아래로 갈수록 변경 비용이 낮다.

---

## §A 배경 (Context)

- 3분할된 SPEC 중 **두 번째**다. 실행 순서: `SPEC-AUTH-001` → **`SPEC-COURSE-001`** → `SPEC-ENROLLMENT-001`.
- **선행 의존: `SPEC-AUTH-001`** (관리자 엔드포인트 인가에 JWT 필터 체인과 `ADMIN` 역할이 필요하다). 선행 SPEC이 `completed`가 되기 전에는 run 단계에 진입하지 않는다.
- `SPEC-AUTH-001`이 만든 `common/config`, `common/exception`, `common/response` 골격을 그대로 사용한다. 재설계하지 않는다.
- 미해소 클래리피케이션 마커: **0건**.

### A.1 이 SPEC의 핵심 책임 — 정원 스키마의 소유권 경계

이 SPEC이 `course.enrolled_count` 컬럼과 그 CHECK 제약을 만든다. 그러나 **값을 바꾸는 코드는 만들지 않는다.** 변경 권한은 `SPEC-ENROLLMENT-001`의 큐 워커가 단독으로 갖는다 (REQ-CRS-004 / INV-CRS-003).

이 경계를 지금 문서에 못 박고 AC-CRS-004로 기계 검증하는 이유는, 선착순 정합성의 1차 방어선이 "확정 인원을 바꾸는 코드 경로가 단 하나뿐"이라는 **구조적 성질**이기 때문이다. 강좌 서비스에 "편의상" 카운터 조정 메서드가 하나라도 생기면 그 방어선은 그 순간 사라진다. 나중에 지우기보다 처음부터 만들지 않는 편이 싸다.

---

## §B 범위 (Scope)

| 포함 | 제외 (다른 SPEC 소유) |
|---|---|
| `Course` 엔티티 + `capacity` / `enrolled_count` + CHECK 제약 | 회원·인증 → `SPEC-AUTH-001` |
| 공개 카탈로그 목록·상세 API | 큐·워커·대기명단·취소 → `SPEC-ENROLLMENT-001` |
| 관리자 강좌 생성·수정·마감·삭제(soft) API | 정원 증설 시 대기자 승격 → `SPEC-ENROLLMENT-001` |
| 페이지네이션 | 프론트엔드 화면 → `SPEC-FRONTEND-001` (**아직 생성하지 않음**) |

---

## §C 기술 설계 (되돌리기 어려운 순)

### C.1 데이터 모델 — 최우선

| 테이블 | 컬럼 | 제약 |
|---|---|---|
| `course` | `id` (BIGSERIAL), `title`, `description`, `capacity`, `enrolled_count`, `starts_at`, `ends_at`, `status`, `created_at`, `updated_at` | `CHECK (capacity >= 1)`, `CHECK (enrolled_count >= 0 AND enrolled_count <= capacity)`, **`CHECK (status IN ('OPEN','CLOSED'))`**, `enrolled_count` NOT NULL DEFAULT 0, `status` NOT NULL DEFAULT `'OPEN'` |

설계 판단:

0. **`status` 값 제약은 DB에도 건다** (REQ-CRS-005). `CHECK (status IN ('OPEN','CLOSED'))` 또는 PostgreSQL enum 타입 중 하나를 사용한다 — 둘 중 어느 쪽이든 애플리케이션을 우회한 직접 INSERT를 DB가 거부하면 요구사항을 만족한다. CHECK 쪽이 스키마 변경 비용이 낮아 기본 선택이다. 이 제약이 없으면 REQ-CRS-005의 "저장 shall not"이 애플리케이션 계층에서만 지켜지는 상태가 되어, 형제 요구사항 REQ-CRS-002/003(둘 다 DB 제약으로 검증)과 방어선 깊이가 어긋난다. AC-CRS-005가 우회 INSERT로 이를 검증한다.

1. **`enrolled_count`는 비정규화 카운터다.** `enrollment` 행을 매번 세는 대신 카운터를 유지하는 이유는, `SPEC-ENROLLMENT-001`의 워커가 정원 검사와 증가를 **한 트랜잭션 안에서 원자적으로** 수행할 수 있어야 하고, DB CHECK 제약이 그 원자성의 최종 방어선이 되어야 하기 때문이다. `COUNT(*)`로는 CHECK 제약을 걸 수 없다.
2. **잔여 정원은 저장하지 않는다.** `capacity - enrolled_count`로 조회 시 계산한다. 저장하면 동기화해야 할 상태가 하나 늘어난다 (AC-CAT-002가 컬럼 부재를 검증한다).
3. **`status`는 문자열 + Java enum.** 별도 상태 이력 테이블을 만들지 않는다.
4. **삭제는 항상 soft**다. 물리 삭제 경로를 아예 만들지 않으므로 "확정자가 있으면 삭제 금지" 같은 분기 자체가 불필요하다 (단순화). AC-ADM-007이 정적 검색으로 `delete` 호출 부재를 확인한다.

### C.2 API 계약

| 메서드 | 경로 | 인가 | 비고 |
|---|---|---|---|
| GET | `/api/courses` | 공개 | 페이지네이션 (`page`, `size`) |
| GET | `/api/courses/{id}` | 공개 | 잔여 정원 포함 |
| POST | `/api/admin/courses` | `ADMIN` | 생성 |
| PATCH | `/api/admin/courses/{id}` | `ADMIN` | 강좌명·설명·정원·일정 수정 |
| POST | `/api/admin/courses/{id}/close` | `ADMIN` | 마감 전이 |
| DELETE | `/api/admin/courses/{id}` | `ADMIN` | **soft** — 내부적으로 마감 전이 |

`/api/admin/**` 경로 인가는 `SPEC-AUTH-001`의 `SecurityConfig`가 이미 처리한다. 이 SPEC은 인가 규칙을 새로 만들지 않고, 결과(403)만 AC-ADM-002로 검증한다.

### C.3 정원 축소 검증의 위치

REQ-ADM-005(확정 인원 미만 축소 거부)는 **애플리케이션 계층에서 검증**한다. DB CHECK 제약(`enrolled_count <= capacity`)이 이미 최종 방어선이지만, 제약 위반 예외를 그대로 500으로 흘리는 대신 400/409로 명확히 응답하기 위해서다. 두 계층 모두 필요하며, AC-ADM-004(애플리케이션)와 AC-CRS-003(DB)이 각각을 검증한다.

경계값에 주의: 정원을 **정확히 확정 인원과 같은 값**으로 축소하는 것은 **허용**이다 (`enrolled_count <= capacity`가 성립한다). AC-ADM-004가 이 경계를 명시적으로 테스트한다.

### C.4 패키지 구조 (SPEC-AUTH-001 골격 확장)

```
com.hongseob.openclass_ap
├── common/               # SPEC-AUTH-001에서 확정 — 재설계 금지
├── member/               # SPEC-AUTH-001 소유
└── course/
    ├── Course.java, CourseStatus.java, CourseRepository.java
    ├── CourseService.java          # 조회 + 관리자 변경
    ├── CourseController.java       # 공개 조회
    ├── admin/CourseAdminController.java
    └── dto/
```

`admin`을 최상위 도메인 패키지로 두지 **않는다**. 관리자는 도메인이 아니라 **인가 관점**이며, 같은 `Course` 애그리게이트를 다루기 때문이다.

### C.5 프론트엔드 — 이 SPEC 범위 밖

- 사용자 결정(**백엔드 우선**)에 따라 React 스캐폴딩을 생성하지 않는다.
- 모든 인수 기준은 Spring Boot 테스트만으로 검증 가능하며, AC-NFR-002가 이를 기계적으로 확인한다.
- 후속 `SPEC-FRONTEND-001`(**아직 생성하지 않음 — 향후 계획**)이 이 SPEC의 카탈로그 API를 소비할 예정이다.

---

## §D 제약 조건

| 항목 | 값 | 출처 |
|---|---|---|
| 언어/런타임 | Java 17 | `build.gradle` |
| 프레임워크 | Spring Boot 4.1.0 | `build.gradle` |
| DB | PostgreSQL | `tech.md` |
| 통합 테스트 DB | **Testcontainers(PostgreSQL) — H2 대체 금지** | CHECK 제약의 실제 동작을 검증해야 한다 (AC-CRS-002/003). H2는 제약 위반 시 동작과 예외 유형이 달라 검증이 무의미해진다 |
| 개발 방법론 | **TDD(RED-GREEN-REFACTOR) 준수** — 프로세스 제약이며 요구사항이 아니다. 산출물 관찰로 검증할 수 없으므로 AC를 부여하지 않는다 (spec.md REQ-NFR-003 주석 참조) | `quality.yaml` |
| 커버리지 | 커밋당 ≥ 80%, 전체 목표 85% — 이쪽은 관찰 가능하므로 REQ-NFR-003 + AC-NFR-003이 검증한다 | `quality.yaml` |
| 문서 언어 | 한국어 | `language.yaml` |
| 신규 인프라 | 추가 금지 | 사용자 명시적 결정 |

---

## §E 사전 점검 (Pre-flight)

1. `SPEC-AUTH-001`이 `completed` 상태이고 `ADMIN` 역할 토큰을 발급받을 수 있다.
2. Testcontainers(PostgreSQL) 기반 통합 테스트 하네스가 `SPEC-AUTH-001`에서 이미 동작 중이다 (재사용).
3. 테스트에서 애플리케이션 계층을 우회해 `course` 행을 직접 INSERT/UPDATE할 수단(`JdbcTemplate` 또는 네이티브 쿼리)이 준비되어 있다 — AC-CRS-002/003/004, AC-CAT-002, AC-ADM-004/005가 이를 요구한다.
4. 미해소 클래리피케이션 마커가 0건이다. — **충족**

---

## §F 마일스톤

### M1 — 강좌 엔티티 및 제약 (우선순위: 높음)

- `Course` 엔티티 + `CourseStatus` + 스키마 + CHECK 제약 **3종** (`capacity >= 1`, `0 ≤ enrolled_count ≤ capacity`, `status IN ('OPEN','CLOSED')`)
- 대응 요구사항: REQ-CRS-001 ~ REQ-CRS-005
- 대응 AC: AC-CRS-001 ~ AC-CRS-005

### M2 — 공개 카탈로그 API (우선순위: 높음)

- 목록(페이지네이션)·상세 조회, 잔여 정원 계산, 404 처리
- 대응 요구사항: REQ-CAT-001 ~ REQ-CAT-006
- 대응 AC: AC-CAT-001 ~ AC-CAT-005

### M3 — 관리자 강좌 관리 API (우선순위: 높음)

- 생성·수정·마감·soft 삭제, 정원 축소 검증(경계 포함), 403/404 처리
- 대응 요구사항: REQ-ADM-001 ~ REQ-ADM-009
- 대응 AC: AC-ADM-001 ~ AC-ADM-008

### M4 — 비기능 마감 (우선순위: 중)

- 입력 검증 보강, 커버리지 도달, 정적 품질 0건
- 대응 요구사항: REQ-NFR-001 ~ REQ-NFR-003
- 대응 AC: AC-NFR-001 ~ AC-NFR-003

---

## §G 안티패턴 (구현 중 금지)

| 안티패턴 | 왜 금지인가 |
|---|---|
| 강좌 서비스에 `enrolled_count` 증감 메서드 추가 | REQ-CRS-004 / INV-CRS-003 위반. 선착순 정합성의 구조적 1차 방어선이 무너진다. AC-CRS-004가 정적 검색으로 검출한다 |
| 잔여 정원을 컬럼으로 저장 | 동기화해야 할 상태가 하나 늘고, `capacity`/`enrolled_count`와 어긋날 수 있다 |
| 강좌 물리 삭제(`delete`/`remove`) 구현 | INV-CRS-004 위반. 확정 수강신청의 참조 무결성이 깨진다 |
| 정원 축소 검증을 DB 제약에만 의존 | 제약 위반이 500으로 흘러 사용자에게 원인이 전달되지 않는다. 애플리케이션 계층 검증을 함께 둔다 |
| 정원을 확정 인원과 **같은 값**으로 축소하는 것을 거부 | 경계 오해. `enrolled_count <= capacity`가 성립하므로 허용이다 (AC-ADM-004) |
| `SecurityConfig` 인가 규칙 재작성 | `SPEC-AUTH-001` 소유 영역. 경로만 추가하고 규칙 구조는 건드리지 않는다 |
| 통합 테스트를 H2로 대체 | CHECK 제약 동작이 재현되지 않아 AC-CRS-002/003 검증이 무력화된다 |
| React 스캐폴딩 생성 | 사용자 결정(백엔드 우선) 위반. spec.md §D 범위 제외 |

---

## §H 교차 참조

- 요구사항 정의: `.moai/specs/SPEC-COURSE-001/spec.md`
- 인수 기준: `.moai/specs/SPEC-COURSE-001/acceptance.md`
- 진행 기록: `.moai/specs/SPEC-COURSE-001/progress.md`
- 선행 SPEC: `.moai/specs/SPEC-AUTH-001/`
- 후속 SPEC: `.moai/specs/SPEC-ENROLLMENT-001/`
- 제품/구조/기술: `.moai/project/{product,structure,tech}.md`
