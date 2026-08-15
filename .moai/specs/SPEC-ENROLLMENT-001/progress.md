---
id: SPEC-ENROLLMENT-001
title: "선착순 수강신청 큐·워커 및 대기명단 자동 승격 — 진행 기록"
version: "0.2.1"
status: draft
created: 2026-08-15
updated: 2026-08-15
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

- `plan_status`: audit-ready (재감사 대기 — iteration 2)
- 산출물: `spec.md`, `plan.md`, `acceptance.md`, `design.md`, `research.md`, `progress.md` — **Tier L 필수 5종 전부 + 진행 기록**, status: draft
- Tier 판정: **L** (예상 16~18 프로덕션 파일 + 10~12 테스트 파일, 동시성 모델이 프로젝트 전체에 헌법적 영향)
- 요구사항 49건 + 불변식 8건 = 57건, 인수 기준 49건, 미대응 0건 (acceptance.md §D.2, 기계 검증 완료)
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

### 다음 단계

plan-auditor 재감사(iteration 2, D1~D12 델타 범위) → Implementation Kickoff Approval → run (M1부터)

## §E.2 Run-phase Evidence

_<pending run-phase>_

## §E.3 Run-phase Audit-Ready Signal

_<pending run-phase>_

## §E.4 Sync-phase Audit-Ready Signal

_<pending sync-phase>_
