---
id: SPEC-COURSE-001
title: "강좌 엔티티·카탈로그 조회 및 관리자 강좌 관리 — 진행 기록"
version: "0.1.2"
status: draft
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

## §E.2 Run-phase Evidence

_<pending run-phase>_

## §E.3 Run-phase Audit-Ready Signal

_<pending run-phase>_

## §E.4 Sync-phase Audit-Ready Signal

_<pending sync-phase>_
