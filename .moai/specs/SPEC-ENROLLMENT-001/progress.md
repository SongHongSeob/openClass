---
id: SPEC-ENROLLMENT-001
title: "선착순 수강신청 큐·워커 및 대기명단 자동 승격 — 진행 기록"
version: "0.2.2"
status: draft
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

## §E.2 Run-phase Evidence

_<pending run-phase>_

## §E.3 Run-phase Audit-Ready Signal

_<pending run-phase>_

## §E.4 Sync-phase Audit-Ready Signal

_<pending sync-phase>_
