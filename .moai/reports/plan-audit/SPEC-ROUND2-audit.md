# SPEC Round-2 Audit — SPEC-AUTH-001 / SPEC-COURSE-001 / SPEC-ENROLLMENT-001

Auditor: plan-auditor (independent, adversarial stance)
Date: 2026-08-15
Scope: full audit of SPEC-AUTH-001 + SPEC-COURSE-001 (new); delta re-audit of SPEC-ENROLLMENT-001 v0.2.0 against D1-D12 plus fresh full re-verification.

> **Reasoning context ignored per M1 Context Isolation.** The delegating message contained the authoring agent's self-report ("all 12 defects resolved", "3-way diff found 0 gaps: 28/28, 27/27, 57/57"). Every such claim was treated as an unverified hypothesis and re-derived mechanically from the artifact files. Where my derivation matched the claim, the match is stated with the command that produced it — not accepted on assertion.

---

## Verdict Summary

| SPEC | Tier | Threshold | Aggregate | Must-pass | Verdict |
|---|---|---|---|---|---|
| SPEC-AUTH-001 | M | 0.80 | **0.86** | 7/7 PASS | **PASS-WITH-CONCERNS** |
| SPEC-COURSE-001 | M | 0.80 | **0.95** | 7/7 PASS | **PASS** |
| SPEC-ENROLLMENT-001 | L | 0.85 | **0.91** | 7/7 PASS | **PASS-WITH-CONCERNS** |

**Combined recommendation: GO to Implementation Kickoff Approval, conditional.** No SPEC fails a must-pass criterion and no aggregate is below its tier threshold. Three defects must be closed before the milestone that depends on them (A1 before AUTH M3; E1 and E2 before ENROLLMENT M4). None blocks kickoff itself, because M1/M2 of every SPEC is unaffected.

**Score trajectory (ENROLLMENT):** iter1 0.76 (FAIL) → iter2 0.91 (PASS). No regression; the STOP-on-regression escalation does not apply.

---

## Part 1 — SPEC-ENROLLMENT-001 (delta re-audit, iteration 2)

### 1.1 Must-Pass Results

- **[PASS] MP-1 REQ number consistency** — mechanically extracted definition lines; 49 REQ + 8 INV = 57 unique IDs, zero duplicates (`uniq -d` empty), zero gaps. Per-family ranges: `REQ-QUE 001-007`, `REQ-WRK 001-015`, `REQ-STS 001-004`, `REQ-WL 001-008`, `REQ-CNL 001-005`, `REQ-ADX 001-004`, `REQ-NFR 001-006`, `INV-ENR 001-008`. Consistent 3-digit zero-padding throughout.
- **[PASS] MP-2 GEARS format compliance** — all 49 REQ lines carry a pattern annotation and a `shall`/`shall not` operator. Distribution: Ubiquitous 27, Event-driven 17, State-driven 4, Capability gate 1. Connective alignment verified mechanically: zero Event-driven REQs lacking `**When**`, zero State-driven lacking `**While**`, zero Ubiquitous wrongly carrying `When`/`While`/`Where`. The single `Where` (`spec.md:141` REQ-WRK-013, "워커 인스턴스가 2개 이상 동시 구동되는 배치") gates on deployment topology — the correct GEARS capability-gate usage. **D8 resolved.**
- **[PASS] MP-3 YAML frontmatter validity** — all 12 canonical fields present with correct types (`spec.md:2-16`): `id`, `title`, `version: "0.2.0"` (quoted semver), `status: draft`, `created`/`updated` as ISO dates (canonical names, not the rejected `created_at`/`updated_at` aliases), `author`, `priority: P0`, `phase`, `module`, `lifecycle: spec-anchored`, `tags` (comma-separated string, not `labels`). Optional `tier: L` and `depends_on:` present and well-formed.
- **[N/A] MP-4 language neutrality** — single-language project (Java/Spring Boot, `build.gradle:12` `JavaLanguageVersion.of(17)`). Multi-language tooling neutrality does not apply. N/A auto-passes.
- **[PASS] MP-5 D7 cross-SPEC reconciliation** — extracted every `SPEC-([A-Z][A-Z0-9]+-)+[0-9]+` reference. Four distinct IDs across the artifact set: AUTH-001, COURSE-001, ENROLLMENT-001, FRONTEND-001. The three existing SPECs all carry `status: draft` — none is `retired`/`superseded`/`archived`, so no reconciliation clause is required and no BLOCKING finding is emitted. `SPEC-FRONTEND-001` does not exist under `.moai/specs/` → D7-5 SHOULD severity only, and it is explicitly labelled "**아직 생성하지 않음 — 향후 계획**" at `spec.md:249`, `plan.md:57,148`. Benign. **D11 resolved.**
- **[N/A] MP-6 D8 cross-platform discipline** — `grep -rn 'syscall' .moai/specs/` returns zero matches across all three SPECs. The D8 verification verb is not applicable to a JVM project with no syscall surface. N/A auto-passes.
- **[PASS] MP-7 clarification gate** — `grep -rn 'NEEDS CLARIFICATION' .moai/specs/` returns 13 hits, and I inspected **every one**. Zero are unresolved markers. All 13 are meta-references without a topic payload — either an assertion of absence (`plan.md:28` "미해소 `[NEEDS CLARIFICATION]`: **0건**", `plan.md:180` "— **충족**", `progress.md:25`), a historical audit-response row (`plan.md:46`, recording D10's resolution), or a DoD checklist item requiring absence (`acceptance.md:453`). None matches the canonical `[NEEDS CLARIFICATION: <topic>]` form. **D10 resolved.** See defect E6 for the grep-collision hazard this creates.

### 1.2 Category Scores

| Dimension | Score | Rubric band | Evidence |
|---|---|---|---|
| Clarity | 0.90 | 0.75-1.0 | Every REQ carries a named subject (접수 API / 워커 / 상태 조회 API / 데이터베이스) — the prior audit's "bare 시스템 subject" deduction is gone. `spec.md:37-51` (§A.2) states the failure scenario, the root cause, the mechanism, and the precondition in sequence, which is unusually clear for a concurrency argument. `spec.md:65-111` (§A.4) enumerates the complete `request_type`/`state`/`result` domains plus the legal-transition set — **D6 resolved.** Docked for E2 (CLOSED-course interaction with CANCEL/CAPACITY_INCREASE is undefined) and E3 (two stale AC cross-references in `design.md` point implementers at the wrong criteria). |
| Completeness | 0.85 | 0.75-1.0 | All 5 Tier L artifacts exist and are substantive, not stubs: `spec.md` 277L, `plan.md` 271L, `acceptance.md` 454L, `design.md` 273L, `research.md` 221L — **D9 resolved.** Seven `### Out of Scope — <topic>` H3 sub-headings, each with concrete `-` bullets (`spec.md:212,220,227,233,238,245,251`), satisfying the `OutOfScopeRule` convention. The former ungated edge-case list is gone: `acceptance.md:421-433` (§D.3) maps all 7 prior edge cases to numbered ACs or documents their design-level elimination — **D7 resolved.** Docked for E1 (no requirement, constraint, or AC forbids duplicate active waitlist entries for the same member+course) and E2. |
| Testability | 0.90 | 0.75-1.0 | Zero weasel words: `grep -nE '적절(한\|히)\|합리적\|충분히 좋\|적당(한\|히)\|reasonable\|appropriate\|adequate'` over spec + acceptance returns no matches. Strong quantification throughout (`acceptance.md:96` "정확히 10건", `:108` "정원이 1", `:196` "5초 이내", `:202` "20회 반복"). `AC-ENR-007` (`acceptance.md:68-73`) is the standout: a lock-disabled **control group** that must *fail* for AC-ENR-006's pass to count as evidence, gated in the DoD at `acceptance.md:443` — this is the correct answer to the prior audit's "flaky test reports green" critique. Docked for E1/E2 having no AC, and for AC-ENR-044's Then clause (`acceptance.md:315`) not requiring the worker to skip an ineligible waiter and promote the next one. |
| Traceability | 1.00 | 1.0 | **Re-derived independently, not accepted from the author's report.** Extracted the 57 definition IDs from `spec.md` (`^- \*\*(REQ\|INV)-` plus the `§C` invariant table rows), the ID set from all 49 `- 대응:` lines in `acceptance.md`, and the left column of the §D.2 matrix (`acceptance.md:357-415`), then ran `comm` in all directions. Result: defs 57, 대응-refs 57, matrix 57; **zero** uncovered REQ, **zero** orphaned reference, **zero** matrix-only entry, **zero** 대응-only entry. Reverse direction also clean: 49 AC definitions, 49 `대응:` lines, 49 matrix-referenced ACs, no orphan either way. The range notation that hid the prior D3 gap is gone — the matrix now demonstrates coverage per-REQ instead of asserting it. **D3 resolved.** |

**Aggregate: (0.90 + 0.85 + 0.90 + 1.00) / 4 = 0.9125 ≥ 0.85 (Tier L).**

### 1.3 Priority-Check Findings (the items most likely to be over-claimed)

**D1 — FCFS ordering under commit-visibility races. RESOLVED, with two residual gaps (E4, E5).**

(a) *Is the lock actually per-course and does it cover the assign+commit window?* Yes. `spec.md:121` (REQ-QUE-003) is normative on both halves: the lock is acquired "큐 행의 순서값을 할당하기 **이전에**", is course-scoped ("대상 강좌 단위의 배타 잠금"), and is "트랜잭션 커밋 시점에 해제". `design.md:65-72` shows the concrete ordering with the lock as step (2) and the INSERT as step (3), and `design.md:74` flags the ordering dependency as invisible in code and therefore anchored by `@MX:ANCHOR` + AC-ENR-006/007. `design.md:76` specifies the 2-argument namespaced form `pg_advisory_xact_lock(classid, objid)` keyed deterministically from `course_id`. `plan.md:240` makes "acquire the lock *after* sequence assignment" an explicit anti-pattern with the correct reason ("순서 보장이 조용히 사라지고 테스트는 대부분 통과한다").

(b) *Does the prefix property actually follow?* Yes, the argument is valid. `research.md:109-117`: exclusivity ⇒ the two `[lock → commit]` critical sections are disjoint ⇒ if T₁ locks first then `s₁ < s₂` **and** `commit(T₁) < lock(T₂) < commit(T₂)` ⇒ per-course, sequence order = commit order ⇒ the worker's visible set is always a prefix. I checked the step the argument does not make explicit: the conclusion additionally requires that PostgreSQL makes a committing transaction *visible* before it *releases* its transaction-scoped advisory lock. In PostgreSQL's commit path, `ProcArrayEndTransaction` (visibility) precedes `ResourceOwnerRelease(RESOURCE_RELEASE_LOCKS)`, so the property holds — but the SPEC never names this dependency and `research.md:202-210` (§7 V1-V7) does not itemize it. See **E5**.

(c) *Does serializing receipts for one popular course create a throughput bottleneck the SLA does not account for?* **Partially unaddressed — see E4.** The mitigating facts are real: the critical section is one INSERT (`research.md:126` "밀리초 미만"), the lock is per-course so different courses are fully parallel (`research.md:125`), and `research.md:208` V5 explicitly defers "단일 강좌 접수 처리량 실측치" to run-phase measurement. But `design.md:191-199` (§6), the derivation table that `REQ-WRK-015` and `AC-ENR-022` check settings against, budgets **only** the worker-drain side (500 rows/s ⇒ 1.0s + 0.2s polling ≈ 1.2s). REQ-STS-003 defines latency end-to-end ("요청 접수 시점부터 종단 결과 확정까지"), and the receipt-side serialization introduced by the D1 fix is a new term in that budget that the table omits entirely. AC-ENR-026 does measure the real end-to-end figure (`acceptance.md:194-198`), so this will surface at run-phase rather than ship silently — hence minor, not major.

(d) *Is the single-worker assumption promoted to spec.md, not left in plan.md?* Yes. `spec.md:140` REQ-WRK-012 states it as a Ubiquitous requirement; `spec.md:51` restates it inline in §A.2 with the explicit note "이 전제는 계획 문서가 아니라 **요구사항으로** 명시한다"; `spec.md:141` REQ-WRK-013 bounds the multi-instance case (dedup only, no ordering); `spec.md:220-225` excludes multi-worker ordering from scope; and AC-ENR-019 (`acceptance.md:149-153`) verifies the assumption is documented in README/deployment docs *with* the multi-instance warning. Fully resolved.

**D2 — enrolled_count decrement ownership + latecomer overtaking the waitlist. RESOLVED.**

I walked the prior audit's exact interleaving against the new design. Setup: capacity 2, A and B enrolled, C and D waitlisted at positions 1 and 2.
- Under the new design the cancel API mutates nothing — `spec.md:165` (REQ-CNL-001) requires it to enqueue a `CANCEL` row and *not* touch the enrollment record or `enrolled_count`; `acceptance.md:254-258` (AC-ENR-035) asserts exactly that with the worker not run.
- E submits at id=99, A cancels at id=100. Worker at id=99: `enrolled_count` is still 2 = capacity ⇒ E → `WAITLISTED` (position 3). Worker at id=100: cancel A, decrement to 1, promote C, increment to 2 — all in one transaction (`spec.md:156` REQ-WL-003 "**같은 트랜잭션 안에서**"; `design.md:128-143`). C wins the seat.
- Reverse order (CANCEL at id=99, E at id=100): worker cancels+promotes C atomically back to 2, then E sees 2 = capacity ⇒ `WAITLISTED`. Identical outcome.

The gap the prior audit exploited was the *window* between decrement and promote. That window is now structurally absent, not merely narrowed. `REQ-WL-004` (`spec.md:157`) states the precedence rule normatively for the first time, and AC-ENR-031 (`acceptance.md:226-232`) tests **both** orderings, which is the right test design — a one-directional test would pass on the broken design too. `plan.md:242-243` adds both failure modes to the anti-pattern table. No equivalent gap remains.

**D12 — cancel IDOR. RESOLVED.**

Two layers, both required and both gated:
- Layer 1 (`spec.md:166`, REQ-CNL-002): non-owner ⇒ 403/404, **and no queue row is enqueued**. AC-ENR-036 (`acceptance.md:260-265`) is marked "필수 — 보안" and asserts all three post-conditions (status code, absence of the `CANCEL` row, invariance of A's `ENROLLED` row and `enrolled_count`). It is separately gated in the DoD at `acceptance.md:445`.
- Layer 2 (`spec.md:167`, REQ-CNL-003): the worker re-checks ownership and, on mismatch, records `REJECTED` and performs no domain change. `design.md:133` shows the concrete check (`if target.member_id != row.member_id → REJECTED`).
- The bypass path is directly tested: AC-ENR-037 (`acceptance.md:267-271`) inserts a `CANCEL` queue row *directly, bypassing the application layer*, with a non-owner member id, and asserts the worker rejects it with no change to the target row, `enrolled_count`, or the waitlist. This is a real second-layer test, not a restatement of layer 1.
- Coverage is complete across both cancel surfaces: waitlist cancellation gets the same treatment (`spec.md:161` REQ-WL-008 + AC-ENR-034), and `INV-ENR-008` (`spec.md:204`) states the invariant over both. `REQ-CNL-004` forbids admin proxy-cancel and AC-ENR-038 verifies the endpoint's absence.

One boundary I checked and am not raising as a defect: layer 2 compares the queue row's `member_id` against the target's owner, so an attacker who can write queue rows directly could also forge `member_id`. That attacker already has arbitrary DB write access, which is outside the stated threat model (enumerable identifiers over the API), and layer 1 closes the modelled path. Recording the check, not the defect.

**D4 — INV-002 AC bypassable via JPA cascade. RESOLVED.**

AC-ENR-008 (`acceptance.md:77-82`) is a genuine DB-state assertion, not another textual-pattern check: with the worker **never started**, call the receipt / cancel / capacity-increase APIs, then assert the `enrollment` row count and `course.enrolled_count` are "**최초 기록값과 정확히 동일**", with only `enrollment_request` having grown. This catches any write path — cascade, `merge`, dirty checking, native SQL — because it observes the outcome rather than the call site. `acceptance.md:81` states that rationale explicitly. AC-ENR-009 (`acceptance.md:84-89`) adds the two structural layers (JPA metamodel assertion that `CascadeType.PERSIST/ALL` targeting `Enrollment` is zero; ArchUnit package restriction). `design.md:170-187` documents the four bypass classes and why grep misses each. `plan.md:245` makes grep-only verification an anti-pattern. AC-ENR-008 is separately gated in the DoD (`acceptance.md:444`) and is an M2 completion condition (`plan.md:204`).

**D9 — Tier L artifact completeness + honesty of the research.md caveat. RESOLVED, caveat honestly stated.**

`research.md:21` states the verification posture without hedging: "본 plan 단계에서는 실제 DB에 대해 실행 검증을 수행하지 않았다. **run 단계 M1에서 §6의 검증 항목을 실제 PostgreSQL에서 실행하여 확인해야 하며** ... 확인 전까지 아래 서술은 **가설**로 취급한다." This is repeated at `progress.md:39-41` and enforced by a DoD line item (`acceptance.md:447`) requiring V1-V7 confirmation recorded in `progress.md §E.2`. The claims are presented as hypotheses, not as fact — the caveat is honest.

Internal consistency between research.md and the D1 fix in plan.md: verified end-to-end. `research.md §2` establishes sequence-order ≠ commit-order as the root cause; `§3` establishes that `SKIP LOCKED` addresses *locked* rows and is powerless against *invisible* rows (the correct distinction, tabulated at `:81-84`); `§4` adopts the advisory lock and carries the consistency proof; `plan.md:73-89` (§C.2) reproduces the same chain with the same conclusion; `spec.md:37-51` (§A.2) states it normatively. No divergence between the three. `research.md:65-71` also records the two rejected alternatives (redefine 접수 순서 as `id` order; visibility watermark) with rejection reasons — notably rejecting option (A), which was the cheapest option the prior audit offered, on the grounds that it weakens the requirement to fit the failure. That is the harder and more honest choice.

**Verification-debt classification — appropriate for all three items.**

1. *research.md's PostgreSQL claims (V1-V7)* — genuinely run-phase-verifiable: each item requires an actual PostgreSQL instance and a two-connection transaction harness. Nothing about them is a plan-phase decision the user could resolve. Correctly classified; correctly gated by `acceptance.md:447`.
2. *D5 throughput figures are calculated, not measured* — `design.md:207` and `plan.md:142` both state the calculated-vs-measured distinction and, critically, fix the direction of reconciliation: "**요구사항의 숫자를 실측에 맞게 개정한다** — 측정을 요구사항에 맞추지 않는다", with `plan.md:254` making the inverse an anti-pattern. AC-ENR-026 measures it and `acceptance.md:448` gates the reconciliation. Correctly classified.
3. *JWT library artifact unpinned* (AUTH-001, `plan.md:84`) — the choice between `io.jsonwebtoken:jjwt` and an equivalent has no requirement impact: REQ-LOGIN-003/004 constrain claims and key sourcing, both library-agnostic. It is a run-phase dependency selection, not a disguised plan-phase ambiguity. Correctly classified.

**None of the three should have been raised back to the user.** Raising them as `[NEEDS CLARIFICATION]` would have been marker inflation — the user cannot answer "what is this PostgreSQL instance's actual throughput" from a question prompt. The authoring agent's refusal to fabricate resolution here is the correct call and I am not overturning it.

### 1.4 Defects Found — SPEC-ENROLLMENT-001

**E1. WAITLIST-DUPLICATE-ENTRY-UNCONSTRAINED — `spec.md:135` (REQ-WRK-007) + `design.md:36-37` + `acceptance.md:312-316` — Severity: major — a member can hold multiple active waitlist positions for one course, and the resulting promotion can deadlock the queue head.**

*Discovered on the fresh full re-verification, not present in the prior defect list.*

`spec.md:135` (REQ-WRK-007) scopes duplicate detection to exactly two states: "이미 확정된 강좌 **또는** 큐에 미처리 상태로 남아 있는 강좌". An **active waitlist entry is neither.** AC-ENR-014 (`acceptance.md:118-122`) confirms this narrow reading — it tests the already-confirmed case and the pending-queue-row case, and nothing else.

Reachable sequence, using only requirements-conformant behaviour:
1. Member A submits `ENROLL` for a full course. Worker processes it → `WAITLISTED`, position 3. The queue row is now `state='DONE'`.
2. A sees `WAITLISTED`, is impatient, and submits `ENROLL` again — ordinary user behaviour, not an attack.
3. The worker evaluates request 2: A has no `ENROLLED` row (check 1 passes), and no `PENDING` queue row (check 2 passes — request 1 is `DONE`). `enrolled_count` still equals `capacity`, so `design.md:125` routes it to `INSERT waitlist_entry(position = 다음 순번)` → A now holds **two active waitlist entries**, positions 3 and 4.

Nothing forbids this state. `INV-ENR-003` (`spec.md:199`) constrains only *confirmed* enrollments. `INV-ENR-004` (`spec.md:200`) requires positions to be unique per course — which two entries for the same member satisfy. The constraint table at `design.md:36-37` carries a partial unique index on `enrollment (member_id, course_id) WHERE status='ENROLLED'` and on `waitlist_entry (course_id, position) WHERE status='WAITING'` — there is **no** `waitlist_entry (member_id, course_id)` uniqueness.

Two consequences, the second worse than the first:
- **Fairness.** A occupies two of the queue positions ahead of other members, in a SPEC whose stated core value is 선착순 정합성 (`spec.md:33`). This is the same class of fairness defect as prior-audit D2.
- **Liveness / promotion deadlock.** Suppose A's position-3 entry is promoted first: A becomes `ENROLLED`, and the position-4 entry remains `WAITING`. When it later reaches the head, `design.md:138-142` promotes exactly one `front` waiter and unconditionally `INSERT enrollment(front.member_id, status=ENROLLED)`. The partial unique index rejects it; the exception propagates out of `processOne`; per `design.md:112` the whole transaction rolls back and the request is recorded `FAILED`. **The cancel is silently lost, the freed seat is never reassigned, and A's stale entry stays at the head of the waitlist, failing every subsequent CANCEL the same way.** The queue head is permanently blocked.

AC-ENR-044 (`acceptance.md:312-316`) is aimed at the adjacent case but does not close this. Its Given calls the already-enrolled-waiter state "인위적으로 만들어졌을 때" — the sequence above shows it is reachable through normal use, not artificial. More importantly its Then requires only that no duplicate row is created and that `enrolled_count` stays consistent with the real row count; the `FAILED`-rollback path satisfies both while leaving the queue deadlocked. **The AC does not require the worker to skip an ineligible waiter and promote the next one**, which is the behaviour that would actually make the system live.

Required fix (all four parts):
1. Extend REQ-WRK-007 to a third state: reject an `ENROLL` when the member already holds an **active waitlist entry** for that course.
2. Add a `waitlist_entry (member_id, course_id) WHERE status='WAITING'` partial unique index to `design.md §1.1`, as the mechanical backstop, and add an INV covering it.
3. Add a requirement that the promotion loop **skips** a waiter who is ineligible (already `ENROLLED`) and advances to the next active waiter, rather than failing the transaction. This is a behavioural gap independent of (1)-(2) and should be fixed regardless.
4. Strengthen AC-ENR-044's Then to assert that the seat **is** filled by the next eligible waiter and that the `CANCEL` result is `CANCELLED`, not `FAILED`; add an AC for the duplicate-waitlist rejection in (1). Both belong to M4.

**E2. CLOSED-COURSE-CANCEL-AND-PROMOTION-UNDEFINED — `spec.md:134,156,176` + `design.md:128-166` + `acceptance.md:112-116` — Severity: major — a member can be promoted into a course that has been closed (and, per SPEC-COURSE-001, "deleted").**

`grep -n 'CLOSED'` over `spec.md` and `design.md` returns exactly four content hits, and **all four sit on the `ENROLL` path**: `spec.md:92` (the `result` domain row, scoped under `request_type = ENROLL`), `spec.md:134` (REQ-WRK-006, "워커는 `ENROLL` 요청에 대해"), `spec.md:178` (REQ-ADX-004, "미처리 `ENROLL` 요청"), and `design.md:119` (inside the `ENROLL` dispatch block).

The `CANCEL` branch (`design.md:128-143`) and the `CAPACITY_INCREASE` branch (`design.md:154-166`) contain **no course-status check whatsoever**, and neither `REQ-WL-003` (`spec.md:156`) nor `REQ-ADX-002` (`spec.md:176`) carries a status condition. `spec.md §A.4`'s result table offers no `CLOSED` outcome for either request type, so an implementer following the SPEC literally will promote a waiter into a `CLOSED` course.

This is reachable through ordinary administration, and SPEC-COURSE-001 makes it worse: `REQ-ADM-008` (`SPEC-COURSE-001/spec.md:85`) defines **delete as a CLOSED transition** — courses are never physically removed. So: admin "deletes" course C → C becomes `CLOSED` → enrolled member A cancels → the worker promotes waiter D into a deleted course. D now holds a confirmed enrollment in a course the administrator believes is gone. No requirement forbids it and AC-ENR-013 (`acceptance.md:112-116`) covers only pending `ENROLL` requests, so no AC detects it.

Both answers are defensible — honour the existing waitlist, or freeze the course entirely — but the SPEC picks neither, leaving the decision to run-phase invention. That is precisely the D6 failure mode the rewrite otherwise fixed.

Required fix: state the intended behaviour for `CANCEL` and `CAPACITY_INCREASE` on a `CLOSED` course as an explicit requirement; add the corresponding `result` values to §A.4 if a new terminal result is needed; extend `design.md §4.3`'s CANCEL and CAPACITY_INCREASE pseudocode with the status branch; add an AC. M4/M5 scope.

**E3. DESIGN-MD-STALE-AC-CROSSREFS — `design.md:181` and `design.md:203` — Severity: minor — two cross-references point implementers at the wrong acceptance criteria.**

Verified by extracting every `AC-ENR-[0-9]*` reference from `design.md`/`research.md`/`plan.md` and checking each against the AC titles in `acceptance.md`:

- `design.md:181` — "따라서 3층으로 검증한다 (acceptance.md **AC-ENR-003/004**)." The three-layer single-path verification is **AC-ENR-008/009** (`acceptance.md:77`, `:84`). AC-ENR-003 is "존재하지 않는 강좌 접수" (`acceptance.md:43`) and AC-ENR-004 is "요청 종류 도메인 제한" (`acceptance.md:49`) — entirely unrelated.
- `design.md:203` — "그 상한을 실제로 측정하는 AC를 둔다 (**AC-ENR-014**)." The load-measurement AC is **AC-ENR-026** (`acceptance.md:193`). AC-ENR-014 is "중복 신청 거부" (`acceptance.md:118`).

Every reference in `plan.md` (17 checked) is correct, and `design.md:74` is correct — the defect is confined to these two lines, and both are almost certainly renumbering residue from the rewrite. They matter because `design.md §5` and `§6` are exactly the sections an implementer consults for the D4 and D5 fixes, and both misdirect. Required fix: `AC-ENR-003/004` → `AC-ENR-008/009`; `AC-ENR-014` → `AC-ENR-026`.

**E4. RECEIPT-LOCK-SERIALIZATION-ABSENT-FROM-LATENCY-BUDGET — `design.md:191-199` + `spec.md:149` + `spec.md:143` — Severity: minor — the D1 fix adds a serialization point that the D5 fix's derivation table does not budget for.**

`REQ-STS-003` (`spec.md:149`) defines the 5-second target **end-to-end**: "요청 접수 시점부터 종단 결과 확정까지의 지연". The derivation table at `design.md:191-199` accounts only for worker drain — 200-row batches every 200ms, halved to a conservative 500 rows/s, giving `500 ÷ 500 + 0.2 ≈ 1.2s`. Under the new design all 500 concurrent receipts for the *same course* must pass single-file through `pg_advisory_xact_lock`, and that receipt-side term appears nowhere in the table. `REQ-WRK-015` (`spec.md:143`) requires the settings' derivation to be documented, and AC-ENR-022 (`acceptance.md:167-171`) checks the settings against this table — so the artifact the AC validates against is incomplete.

Mitigating: the critical section is one INSERT (`research.md:126`); `research.md:208` V5 already schedules "단일 강좌 접수 처리량 실측치" for run-phase; and AC-ENR-026 measures the true end-to-end figure, so the omission surfaces as a measurement discrepancy rather than shipping silently. Hence minor. Required fix: add a receipt-side serialization row to `design.md §6` (even as an estimate pending V5) so the budget is end-to-end, matching REQ-STS-003's own scope.

**E5. ADVISORY-LOCK-RELEASE-ORDERING-NOT-IN-VERIFICATION-DEBT — `research.md:109-117` + `research.md:202-210` — Severity: minor — the consistency proof depends on an unstated PostgreSQL ordering property that V1-V7 does not check.**

The prefix argument (`research.md:113`) concludes `commit(T₁) < 잠금획득(T₂) < commit(T₂)`. For the worker to be guaranteed a *visible* prefix, one further property is needed: PostgreSQL must make a committing transaction's rows visible **before** it releases that transaction's advisory lock. It does — `ProcArrayEndTransaction` runs before lock release in the commit path — so the argument stands. But this is exactly the kind of load-bearing engine-level assumption whose omission caused D1 in the first place, and `research.md §7`'s verification list does not itemize it: V1 checks only that the lock auto-releases on commit/rollback, not the release-versus-visibility ordering.

Required fix: add a V8 to `research.md §7` — "커밋 중 행 가시화가 권고 잠금 해제보다 먼저 일어난다" — verifiable with the same two-connection harness V1 already requires (T₂ acquires the lock, then immediately reads; T₁'s row must be visible). Near-zero incremental cost, and it closes the last unexamined link in the D1 argument.

**E6. TDD-PROCESS-REQUIREMENT-NOT-AC-VERIFIABLE + NEEDS-CLARIFICATION-GREP-COLLISION — `spec.md:187` + `acceptance.md:453` — Severity: minor (applies to all three SPECs).**

Two small items folded together:
- `REQ-NFR-006` (`spec.md:187`) requires "구현은 TDD(RED-GREEN-REFACTOR)로 **진행**" — a *process* claim stated as a product requirement. Its mapped criterion AC-ENR-049 (`acceptance.md:345-349`) verifies coverage and static-analysis cleanliness only; nothing verifies that tests were written first. The traceability matrix therefore reports coverage that the AC does not actually deliver. The same pattern appears at `SPEC-AUTH-001` REQ-NFR-004 → AC-AUTH-020 and `SPEC-COURSE-001` REQ-NFR-003 → AC-NFR-003. Required fix: either split the verifiable half (coverage thresholds) from the unverifiable half (process), or move the TDD directive to `plan.md §D` as a constraint rather than carrying it as a requirement with a traceability row.
- `acceptance.md:453`'s DoD line "`[NEEDS CLARIFICATION]` 마커가 ... 남아 있지 않다" is itself a literal `[NEEDS CLARIFICATION` string. A naive MP-7 gate (`grep -rn '\[NEEDS CLARIFICATION'`) flags it — I had to inspect all 13 hits by hand to clear the gate. Required fix: rewrite the checklist line to reference the marker without reproducing it (e.g. "클래리피케이션 마커가 남아 있지 않다"), in all three SPECs.

### 1.5 Regression Check (iteration 2 vs iteration 1)

| # | Prior defect | Status | Evidence |
|---|---|---|---|
| D1 | FCFS order not guaranteed under commit-visibility race | **RESOLVED** | `spec.md:37-51` §A.2 + REQ-QUE-003/004 (`:121-122`) + INV-ENR-007 (`:203`) + `research.md §2-§4` + `design.md:65-76` + AC-ENR-005/006/007. Option (A) — redefining 접수 순서 to dodge the requirement — explicitly rejected at `research.md:69`. Residual: E4, E5. |
| D2 | enrolled_count decrement owner ambiguous; latecomer overtakes waitlist | **RESOLVED** | Cancel is a `CANCEL` queue op (REQ-CNL-001, `spec.md:165`); cancel+decrement+promote are one transaction (REQ-WL-003, `:156`); precedence stated normatively (REQ-WL-004, `:157`); AC-ENR-031 tests both orderings. Interleaving re-walked — no equivalent gap. |
| D3 | Malformed `REQ-FCFS-005/006` token; matrix asserted coverage via ranges | **RESOLVED** | §D.2 rewritten as 57 per-REQ rows (`acceptance.md:357-415`), no range notation. Independently re-derived 3-way diff: 0 gaps, 0 orphans, both directions. |
| D4 | INV-002 verified by source grep, bypassable via JPA | **RESOLVED** | AC-ENR-008 asserts DB state with the worker not run (`acceptance.md:77-82`); AC-ENR-009 adds metamodel + ArchUnit; `design.md:170-187` enumerates the bypass classes. |
| D5 | 5s target contradicted by throughput; "정상 부하" unquantified | **RESOLVED** | REQ-STS-003 quantifies the ceiling at 500 concurrent (`spec.md:149`); throughput raised 10× (`design.md:191-199`); AC-ENR-026 measures at the ceiling, not at N=1. Residual: E4. |
| D6 | Queue state machine undefined; undefined `PROCESSING` referenced | **RESOLVED** | `spec.md:65-111` §A.4 enumerates `request_type`/`state`/`result` completely with the legal-transition set; `PROCESSING` eliminated by design with the rationale stated (`:84`) and the alternative recorded (`design.md:261`). |
| D7 | 7 edge cases ungated by any AC | **RESOLVED** | `acceptance.md:421-433` §D.3 maps all 7 to numbered ACs or documents design-level elimination; no non-gated list remains. |
| D8 | `Where` misused for runtime role state | **RESOLVED** | Only one `Where` remains (`spec.md:141`, deployment topology — correct). AUTH's role conditions use `While` (`SPEC-AUTH-001/spec.md:80-81`). Verified mechanically across all three SPECs. |
| D9 | Tier L artifact set 3/5 | **RESOLVED** | 5/5 present and substantive; caveat honestly stated (`research.md:21`) and gated (`acceptance.md:447`). |
| D10 | 3 unresolved `[NEEDS CLARIFICATION]` markers | **RESOLVED** | Zero unresolved markers; all 13 grep hits inspected individually. Residual: E6 (grep collision). |
| D11 | Dangling SPEC references | **RESOLVED** | AUTH-001 and COURSE-001 now exist with `depends_on` wired; FRONTEND-001 labelled as not-yet-created future work. |
| D12 | Cancel IDOR | **RESOLVED** | Two-layer ownership check (REQ-CNL-002/003) + AC-ENR-036 (API) + AC-ENR-037 (direct-insert bypass) + DoD gate + waitlist parity (REQ-WL-008/AC-ENR-034). |

**12 of 12 resolved.** No stagnation. Six new findings (E1-E6), none pre-existing.

---

## Part 2 — SPEC-AUTH-001 (full audit, iteration 1)

### 2.1 Must-Pass Results

- **[PASS] MP-1** — 24 REQ + 4 INV = 28 unique IDs, zero duplicates, zero gaps: `REQ-SIGNUP 001-007`, `REQ-LOGIN 001-004`, `REQ-AUTHZ 001-006`, `REQ-SEED 001-003`, `REQ-NFR 001-004`, `INV-AUTH 001-004`.
- **[PASS] MP-2** — all 24 REQs annotated and carry `shall`/`shall not`: Ubiquitous 13, Event-driven 8, State-driven 3. Connective alignment verified mechanically, zero violations. Role conditions correctly use `While` (`spec.md:80-81`).
- **[PASS] MP-3** — all 12 canonical fields present (`spec.md:2-14`), canonical names, `version: "0.1.0"` quoted, `status: draft`, ISO dates. `depends_on` correctly absent (this SPEC is first).
- **[N/A] MP-4** — single-language project.
- **[PASS] MP-5** — references AUTH/COURSE/ENROLLMENT/FRONTEND-001; the three existing are `draft`, FRONTEND-001 labelled "**아직 생성하지 않음**" (`spec.md:142`). No BLOCKING.
- **[N/A] MP-6** — zero `syscall` matches.
- **[PASS] MP-7** — 4 hits inspected (`plan.md:38,150`, `acceptance.md:207`, `progress.md:25`); all are absence assertions or DoD items, none a real marker.

### 2.2 Category Scores

| Dimension | Score | Band | Evidence |
|---|---|---|---|
| Clarity | 0.90 | 0.75-1.0 | `spec.md:45-53` (§A.3) pins the token contract as a table (format, required claims, TTL, transport, refresh) — no interpretation needed. `spec.md:61-67` distinguishes application-level and DB-level enforcement per requirement. Docked for REQ-AUTHZ-006 (§2.3, A2). |
| Completeness | 0.80 | 0.75-1.0 | HISTORY / §A 개요 / §B 요구사항 / §C 불변식 / §D 범위 제외 / §E 성공 기준 / §F 참조 all present. Five `### Out of Scope — <topic>` H3 sub-headings with concrete bullets (`spec.md:115,123,130,138,144`). Docked substantially for A1 — the SPEC never declares the protected/admin endpoint that five of its ACs require. |
| Testability | 0.80 | 0.75-1.0 | No weasel words. Several exemplary criteria: AC-AUTH-008 (`acceptance.md:74`) demands the two failure responses be "**바이트 단위로 동일**" — the correct way to test non-enumerability; AC-AUTH-005 dumps every column and searches for the plaintext; AC-AUTH-001 asserts the BCrypt prefix set. Docked for A1 (5 of 20 ACs not executable as written) and E6 (TDD process requirement). |
| Traceability | 0.95 | 0.75-1.0 | Independently re-derived: 28 definitions, 28 `대응:` references, 28 matrix rows; `comm` clean in all four directions. 20 AC definitions, 20 `대응:` lines, 20 matrix-referenced ACs, no orphan either way. Docked for A2 (AC-AUTH-015 covers only two of the three endpoints its mapped REQ names). |

**Aggregate: (0.90 + 0.80 + 0.80 + 0.95) / 4 = 0.8625 ≥ 0.80 (Tier M).**

### 2.3 Defects Found — SPEC-AUTH-001

**A1. PROTECTED-ENDPOINT-FIXTURE-UNDECLARED — `acceptance.md:86-114` + `plan.md:42-51,172-176,99-114` — Severity: major — five acceptance criteria call endpoints that this SPEC's scope does not contain and never declares.**

SPEC-AUTH-001's deliverable surface is: 회원가입 API, 로그인 API, the JWT filter chain, path-based authorization rules, the admin seeder, and the common exception skeleton (`plan.md:44-51` §B scope table; `plan.md:99-114` §C.5 package structure). **Both** of its HTTP endpoints — signup and login — are public (`spec.md:83`, REQ-AUTHZ-006). The SPEC therefore contains **zero protected endpoints and zero admin endpoints of its own.**

Five ACs nonetheless require one:
- AC-AUTH-010 (`acceptance.md:88`) — "보호 엔드포인트를 호출하면" ⇒ 401
- AC-AUTH-011 (`:94`) — forged/expired token against a protected endpoint
- AC-AUTH-012 (`:100`) — `MEMBER` calls "관리자 엔드포인트(`/api/admin/**`)" ⇒ 403
- AC-AUTH-013 (`:106`) — `ADMIN` calls it ⇒ request reaches the handler
- AC-AUTH-014 (`:112`) — 20 calls to a protected endpoint, asserting no `JSESSIONID`

`/api/admin/**` first acquires a real controller in SPEC-COURSE-001 (`SPEC-COURSE-001/plan.md:70-73`). Within AUTH-001 there is nothing to call. The fix is trivial — a test-scoped `@RestController` fixture exposing one protected and one admin path — but **no artifact declares it**: it is absent from `plan.md §C.5`'s package tree, absent from M3's deliverables (`plan.md:172-176`), and absent from the §E pre-flight list (`plan.md:143-151`). AC-AUTH-013's Then ("요청이 핸들러에 도달한다") is unsatisfiable without a handler to reach.

This is a completeness defect, not a design error: 25% of the SPEC's acceptance criteria are non-executable as written, and an implementer will discover it only at M3, mid-run. Required fix: add the test-fixture controller to `plan.md §C.5` and M3 deliverables, name the two paths it exposes, and state that it is test-scoped and must not ship in production (otherwise the fixture itself becomes a surface AC-AUTH-019's "no frontend dependency" style check would not catch).

**A2. REQ-AUTHZ-006-PARTIALLY-COVERED — `spec.md:83` + `acceptance.md:116-120` + `acceptance.md:180` — Severity: minor — the mapped AC verifies two of the three endpoints its requirement names.**

`REQ-AUTHZ-006` (`spec.md:83`) requires that "공개 엔드포인트(회원가입·로그인·**강좌 조회**)는 토큰 없이 접근 가능 **shall**해야 한다" — three endpoints. AC-AUTH-015 (`acceptance.md:118`) exercises "회원가입·로그인 엔드포인트" only, and the §D.2 matrix (`acceptance.md:180`) records the pair as fully covered.

The narrowing is *reasonable* — the catalog endpoint does not exist until SPEC-COURSE-001 — but it is silent, and the matrix therefore reports coverage the AC does not deliver. The cross-SPEC coverage does exist (`SPEC-COURSE-001` AC-CAT-001, `acceptance.md:62-66`, tests unauthenticated list access), so this is a bookkeeping defect rather than a real hole. Required fix: either drop 강좌 조회 from REQ-AUTHZ-006's enumeration and let SPEC-COURSE-001 own it, or annotate the matrix row to record the cross-SPEC deferral explicitly.

**A3 (= E6).** REQ-NFR-004's TDD process clause is not verified by AC-AUTH-020, and `acceptance.md:207` reproduces the `[NEEDS CLARIFICATION` literal. Same fix as E6.

### 2.4 JWT Trade-off Disclosure — assessed explicitly

**Documented as an accepted limitation, in four places, and gated. Not buried.**

1. `spec.md:33` (§A.1 배경, the first substantive paragraph of the SPEC): "**로그아웃은 클라이언트 측 토큰 폐기만으로 수행되고, 이미 발급된 토큰은 만료 시각까지 유효하다.** 이것은 누락이 아니라 **명시적으로 수용한 알려진 제약**이며".
2. `spec.md:53` (§A.3, the client-facing contract table): a `갱신 | **없음** (v1) | 만료 시 재로그인` row, plus `:51` noting that the 30-minute TTL "짧은 수명이 폐기 목록 부재를 보완하는 유일한 장치다" — it names the mitigation as the *only* mitigation rather than overstating it.
3. `spec.md:121` (§D Out of Scope): a bolded **수용한 제약(known limitation)** paragraph giving the blast radius ("탈취된 토큰은 `exp`(30분) 도래 전까지 유효하다"), the rationale, and the re-entry condition ("강제 무효화가 필요해지면 별도 SPEC으로 폐기 목록을 도입한다").
4. `plan.md:80` repeats it as an explicit trade-off, and `plan.md:194` makes adding a denylist "있으면 좋으니까" an anti-pattern requiring SPEC amendment first.

Critically, it is **enforced, not merely described**: `acceptance.md:206` is a Definition-of-Done checklist item — "spec.md §D에 기록된 **로그아웃 제약**이 README 또는 API 문서에 사용자 대상으로 명시되어 있다." The limitation cannot reach `completed` without being surfaced to end users. This is the correct handling of an accepted security trade-off and I have no finding against it.

I also checked `plan.md:90`'s CSRF-disabled decision, which is adjacent: it is justified by threat model ("쿠키 기반 자격 증명을 사용하지 않으므로 CSRF 공격면이 존재하지 않는다") and explicitly framed as removing an inapplicable defence rather than disabling a security feature. Consistent with `spec.md:52` (헤더 전달, 쿠키 미사용). Sound.

---

## Part 3 — SPEC-COURSE-001 (full audit, iteration 1)

### 3.1 Must-Pass Results

- **[PASS] MP-1** — 23 REQ + 4 INV = 27 unique IDs, zero duplicates, zero gaps: `REQ-CRS 001-005`, `REQ-CAT 001-006`, `REQ-ADM 001-009`, `REQ-NFR 001-003`, `INV-CRS 001-004`.
- **[PASS] MP-2** — all 23 annotated with `shall`: Ubiquitous 13, Event-driven 8, State-driven 2. Connective alignment clean. Role conditions use `While` (`spec.md:78-79`).
- **[PASS] MP-3** — all 12 canonical fields (`spec.md:2-14`) plus `tier: M` and `depends_on: [SPEC-AUTH-001]`.
- **[N/A] MP-4** — single-language project.
- **[PASS] MP-5** — references AUTH/COURSE/ENROLLMENT/FRONTEND-001; existing three are `draft`; FRONTEND-001 labelled not-yet-created (`spec.md:136`). No BLOCKING.
- **[N/A] MP-6** — zero `syscall` matches.
- **[PASS] MP-7** — 3 hits inspected (`plan.md:28,126`, `acceptance.md:213`, `progress.md:25`); all absence assertions or DoD items.

### 3.2 Category Scores

| Dimension | Score | Band | Evidence |
|---|---|---|---|
| Clarity | 0.95 | 1.0-adjacent | `spec.md:34` (§A.1) is the clearest passage in the three-SPEC set: it states the `enrolled_count` ownership boundary, that this SPEC creates the column but no mutation path, that ENROLLMENT's worker holds sole write authority, and *why* the boundary is nailed down now ("나중에 '편의상' 강좌 서비스에서 카운터를 만지는 경로가 생기면 선착순 정합성의 핵심 방어선이 무너지기 때문이다"). `spec.md:43` states that 잔여 정원 is computed, never stored. Docked only for C2. |
| Completeness | 0.95 | 1.0-adjacent | All required sections; five `### Out of Scope — <topic>` H3 sub-headings with concrete bullets (`spec.md:111,119,126,132,138`). Every endpoint the ACs exercise is created by this SPEC, so it has no analogue of AUTH's A1. |
| Testability | 0.90 | 0.75-1.0 | No weasel words. Boundary coverage is deliberate and explicit: AC-CRS-002 tests capacity 0 **and** 1 ("정원 1이 허용 경계다"), AC-CRS-003 tests both `enrolled_count = 6` and `-1`, AC-ADM-004 tests reduction to 6 (reject) **and** to exactly 7 (accept) — the boundary `plan.md:81` calls out as easy to get wrong. AC-CRS-004 combines a DB-state assertion with a static search rather than relying on grep alone, which is the D4 lesson applied pre-emptively. Docked for C1. |
| Traceability | 1.00 | 1.0 | Independently re-derived: 27 definitions, 27 `대응:` references, 27 matrix rows; `comm` clean in all four directions. 21 AC definitions, 21 `대응:` lines, 21 matrix-referenced ACs, no orphan either way. |

**Aggregate: (0.95 + 0.95 + 0.90 + 1.00) / 4 = 0.95 ≥ 0.80 (Tier M).**

### 3.3 Defects Found — SPEC-COURSE-001

**C1. REQ-CRS-005-VERIFIED-AT-WRONG-LAYER — `spec.md:65` + `acceptance.md:54-58` — Severity: minor — a storage-level prohibition is verified only at the application layer.**

`REQ-CRS-005` (`spec.md:65`) is a storage claim: "모집 상태는 §A.3의 두 값 중 하나 **shall**이며, 그 외의 값을 **저장 shall not**한다." Its mapped criterion AC-CRS-005 (`acceptance.md:54-58`) verifies only that "애플리케이션 계층에서 거부되고(400 또는 매핑 예외)".

This is inconsistent with how the SPEC's sibling storage requirements are verified: REQ-CRS-002 and REQ-CRS-003 are both verified by *bypassing the application layer* and asserting the DB refuses the write (`acceptance.md:37` "애플리케이션 계층을 우회하여 ... 직접 INSERT", `:43` same for UPDATE). REQ-CRS-005 gets no equivalent, so a direct INSERT of `status = 'ARCHIVED'` would succeed unless the implementer independently chooses a DB enum or CHECK — which `plan.md:55`'s constraint list does not require (it specifies `status NOT NULL DEFAULT 'OPEN'` only, with no value constraint).

Required fix: either add a DB-level enum/CHECK on `course.status` to `plan.md §C.1` and extend AC-CRS-005 with a bypass assertion matching AC-CRS-002/003, or weaken REQ-CRS-005's wording from "저장 shall not" to an application-layer rejection requirement so the AC matches the claim. The first is preferable — it matches the SPEC's own defence-in-depth pattern.

**C2. ENROLLED-COUNT-"ALWAYS-0"-WORDING-VS-TEST-FIXTURES — `spec.md:42` vs `acceptance.md:69,113,119` — Severity: minor — a glossary absolute is contradicted by the SPEC's own test fixtures.**

`spec.md:42` (§A.2 glossary) states "확정 인원 | `enrolled_count` | 현재 확정된 수강신청 수. **이 SPEC 범위에서는 항상 0**". But AC-CAT-002 (`acceptance.md:69`) sets `확정 인원 4`, AC-ADM-004 (`:113`) sets `7`, and AC-ADM-005 (`:119`) sets `2`, each via direct INSERT. AC-CRS-004 (`acceptance.md:50`) simultaneously asserts "모든 행의 `enrolled_count`가 0".

These do not actually contradict — the normative requirement REQ-CRS-004 (`spec.md:64`) constrains *code paths*, not values, and the fixtures bypass the application layer deliberately — but the glossary's unqualified "항상 0" reads as a state invariant and sits one line above a term the ACs routinely set non-zero. An implementer could reasonably read the glossary as licensing a "reject any non-zero `enrolled_count`" assumption, which AC-CAT-002 would then fail.

Required fix: reword `spec.md:42` to "이 SPEC의 코드 경로는 이 값을 변경하지 않는다 (테스트 픽스처는 직접 설정할 수 있다)", matching REQ-CRS-004's actual scope.

**C3 (= E6).** REQ-NFR-003's TDD clause unverified by AC-NFR-003; `acceptance.md:213` reproduces the `[NEEDS CLARIFICATION` literal.

---

## Part 4 — Cross-SPEC Integrity

**Dependency graph — consistent, acyclic, correctly ordered.**

| SPEC | `depends_on` | Declared execution position | Pre-flight gate |
|---|---|---|---|
| AUTH-001 | *(absent)* | 1st (`plan.md:26,28` "선행 의존: 없음") | — |
| COURSE-001 | `[SPEC-AUTH-001]` | 2nd (`plan.md:25-26`) | `plan.md:123` requires AUTH-001 `completed`; `acceptance.md:215` DoD item |
| ENROLLMENT-001 | `[SPEC-AUTH-001, SPEC-COURSE-001]` | 3rd (`plan.md:25-26`) | `plan.md:174` requires both `completed`; `acceptance.md:454` DoD item |

No cycle. Frontmatter matches prose in every case, and each dependency is additionally enforced as a run-phase pre-flight check *and* a DoD checklist item — the `depends_on` field is not decorative.

**Interface obligations — verified in both directions, no silent assumptions found.**

I checked each downstream assumption against the upstream SPEC's actual requirement text:

| Downstream assumption | Upstream provision | Verdict |
|---|---|---|
| COURSE: unauthenticated catalog access (REQ-CAT-001) | AUTH REQ-AUTHZ-006 (`spec.md:83`) enumerates "**강좌 조회**" among the public endpoints | **Anticipated.** AUTH pre-declares it rather than COURSE assuming it. (Reverse-direction defect A2 arises from this same line.) |
| COURSE: `ADMIN`-only admin endpoints (REQ-ADM-001/002) | AUTH REQ-AUTHZ-003/004 (`spec.md:80-81`) define admin-endpoint 403/allow; `plan.md:88` fixes the `/api/admin/**` pattern to `hasRole("ADMIN")` | **Satisfied.** COURSE `plan.md:75` correctly states it does not author new rules ("이 SPEC은 인가 규칙을 새로 만들지 않고, 결과(403)만 AC-ADM-002로 검증한다") and `plan.md:167` makes rewriting `SecurityConfig` an anti-pattern. |
| ENROLLMENT: 401 on unauthenticated enrollment APIs (REQ-QUE-006) | AUTH REQ-AUTHZ-001 (`spec.md:78`) covers all protected endpoints by default | **Satisfied.** ENROLLMENT's paths (`design.md:243-246`) are all outside the public set. |
| ENROLLMENT: `course.enrolled_count` CHECK constraint exists (`design.md:35`, `plan.md:175`) | COURSE REQ-CRS-003 (`spec.md:63`) mandates `0 ≤ enrolled_count ≤ capacity` as a CHECK; `plan.md:55` specifies it verbatim; AC-CRS-003 verifies it at the DB | **Satisfied.** ENROLLMENT REQ-WRK-014 (`spec.md:142`) restates the reliance without redefining the constraint. |
| ENROLLMENT: sole write authority over `enrolled_count` (REQ-WRK-002, INV-ENR-002) | COURSE REQ-CRS-004 (`spec.md:64`) *cedes* it explicitly — "`enrolled_count`의 변경 권한은 `SPEC-ENROLLMENT-001`의 큐 워커가 단독 보유 **shall**한다" — and INV-CRS-003 + AC-CRS-004 mechanically verify COURSE has no such path | **Exemplary.** Ownership is asserted from both sides and machine-checked on the ceding side. |
| ENROLLMENT: admin capacity increase enqueues `CAPACITY_INCREASE` (REQ-ADX-001) | COURSE REQ-ADM-006 (`spec.md:83`) applies the new capacity but explicitly defers promotion — "대기자 승격은 이 SPEC의 범위가 **아니며** `SPEC-ENROLLMENT-001`이 확장 **shall**한다" | **Satisfied, and forward-compatible.** COURSE AC-ADM-005 (`acceptance.md:118-122`) asserts only that `enrolled_count` is unchanged; it does *not* assert the absence of a queue row, so it will still pass after ENROLLMENT lands. Deliberate and correct. |
| ENROLLMENT: JWT `sub` identifies the member (ownership checks) | AUTH `spec.md:50` defines `sub` as 회원 식별자 | **Satisfied.** |

**One cross-SPEC gap found: E2** (CLOSED-course promotion, §1.4). It arises at the seam — SPEC-COURSE-001 REQ-ADM-008 defines delete as a CLOSED transition, and SPEC-ENROLLMENT-001's CANCEL/CAPACITY_INCREASE paths never read course status — so neither SPEC is individually wrong, and only the joint reading exposes it. That is precisely the failure mode a cross-SPEC integrity check exists to catch.

**Project-constraint claims — verified against the repository, not accepted from the SPEC.**

| Claim | Source line | Repository evidence | Verdict |
|---|---|---|---|
| Java 17 | all three `plan.md §D` | `build.gradle:12` `JavaLanguageVersion.of(17)` | **Confirmed** |
| Spring Boot 4.1.0 | all three `plan.md §D` | `build.gradle:3` `id 'org.springframework.boot' version '4.1.0'` | **Confirmed** |
| PostgreSQL | all three `plan.md §D` | `.moai/project/tech.md:18,28` | **Confirmed** |
| Greenfield, only a bootstrap class exists | `SPEC-AUTH-001/plan.md:25` | `find src -name '*.java'` → exactly `OpenclassApApplication.java` + `OpenclassApApplicationTests.java` | **Confirmed** |
| TDD methodology | all three `plan.md §D` | `.moai/config/sections/quality.yaml:4` `development_mode: "tdd"` | **Confirmed** |

Note for run-phase: `build.gradle` currently declares neither `spring-boot-starter-security`, nor Testcontainers, nor any JWT library. This is consistent with all three plans, which list them as run-phase additions in their §E pre-flight sections rather than claiming they are already present — no over-claim.

---

## Chain-of-Verification Pass

Second-look findings: **two new major defects discovered (E1, A1), one design-artifact defect confirmed by exhaustive cross-reference extraction (E3), one engine-level assumption traced to its unstated dependency (E5), and three non-defects confirmed and withdrawn.**

Re-checks performed on the second pass:

1. **Every REQ read individually across all three SPECs (24 + 23 + 49 = 96), not sampled.** This is what produced **E1**: reading REQ-WRK-007 in isolation makes the two-state duplicate check look complete; reading it against REQ-WL-001's waitlist semantics and `design.md §1.1`'s constraint list exposes the missing third state. The first pass had accepted "duplicate handling — covered by AC-ENR-014" and moved on.
2. **REQ sequencing verified end-to-end mechanically, not spot-checked.** Extracted every definition-position ID and computed per-family ranges plus `uniq -d` for all three SPECs. Equal definition/unique counts prove zero duplicates; contiguous per-family ranges prove zero gaps.
3. **Traceability verified for every REQ in both directions, not sampled.** Ran `comm -23` and `comm -13` for defs↔대응, defs↔matrix, and 대응↔matrix on all three SPECs, plus the reverse AC direction (AC-defined ↔ AC-in-matrix). All twelve comparisons empty. I did not accept the authoring agent's reported 28/28, 27/27, 57/57 — I re-derived them; the numbers happen to match.
4. **Out of Scope checked for specificity, not presence.** 5 / 5 / 7 `### Out of Scope — <topic>` H3 sub-headings, each carrying concrete `-` bullets with named exclusions, not placeholders. ENROLLMENT `spec.md:218` additionally forbids substituting the excluded alternatives during implementation, and `plan.md:256` mirrors it as an anti-pattern.
5. **Cross-requirement contradictions, not just intra-requirement.** Systematic pass of each §B against its §C invariants, its `plan.md §C` design claims, and — new this round — the *other two SPECs'* requirements. This produced **E2** (the CLOSED-course seam) and confirmed the seven interface obligations in Part 4. It also produced the C2 wording tension, which I examined and graded minor rather than a contradiction, because REQ-CRS-004's normative scope is code paths, not values.
6. **NEW — every AC checked for executability against its own SPEC's declared deliverable surface.** The first pass verified that ACs were binary and observable but never asked whether the artifact under test exists in scope. Doing so produced **A1**: five of SPEC-AUTH-001's twenty ACs call a protected or admin endpoint that the SPEC creates nowhere and declares nowhere. This is the single largest finding in Part 2 and would not have surfaced from format or traceability checking.
7. **NEW — every AC cross-reference in the non-acceptance artifacts extracted and checked against AC titles.** `grep -o 'AC-ENR-[0-9]*'` over `design.md`/`research.md`/`plan.md`, then each hit compared to the AC's actual subject. 17 references in `plan.md` correct; `design.md:74` correct; `design.md:181` and `:203` wrong (**E3**). Traceability checking alone would never catch this, because these references live outside `acceptance.md`.
8. **NEW — the D1 consistency argument audited for unstated premises.** Rather than checking that the argument was *present*, I checked whether each step follows. Every step does, but the conclusion additionally requires visibility-before-lock-release, which the SPEC never names and V1-V7 never tests (**E5**). Given that D1 originated in exactly this kind of unexamined engine assumption, leaving the last link untested is worth the finding.
9. **Non-defect confirmed and withdrawn (advisory-lock deadlock).** I drafted a finding that the receipt lock could deadlock against the worker. It cannot: `research.md:129` states the worker never acquires this lock, `research.md:128` notes only one lock is taken per receipt with no nesting, and `design.md §4.2`'s worker path uses row locks on `enrollment_request` only. No cycle is constructible. Withdrawn rather than kept as an overstated finding.
10. **Non-defect confirmed and withdrawn (worker-side ordering across courses).** I checked whether global-`id`-order processing could violate the per-course guarantee. It cannot: the guarantee (REQ-QUE-004, INV-ENR-007) is scoped per-course, and any global ordering that is monotone in `id` preserves every per-course subsequence. The prior audit's unconditional-prohibition problem is genuinely fixed by the per-course scoping, not merely reworded — I verified REQ-WRK-003, REQ-QUE-004, INV-ENR-007, and §E success criterion 2 all carry the same "동일 강좌에 대해" qualifier.
11. **Non-defect confirmed (D12 layer-2 forgery).** Assessed whether an attacker who can insert queue rows could forge `member_id` to defeat the worker's re-check. They could — but that attacker already holds arbitrary DB write access, which is outside the modelled threat (enumerable API identifiers), and layer 1 closes the modelled path. Recorded in §1.3 as a checked boundary, not raised as a defect.
12. **Non-defect confirmed (COURSE forward-compatibility with ENROLLMENT).** Checked whether ENROLLMENT's REQ-ADX-001 will break COURSE's AC-ADM-005 once implemented. It will not — AC-ADM-005 asserts capacity and `enrolled_count`, never the absence of a queue row. Good forward-compatible AC design; recorded as a positive, not a defect.

---

## Recommendation

### Combined go/no-go: **GO to Implementation Kickoff Approval — conditional on three fixes, none of which blocks kickoff.**

All three SPECs clear every must-pass criterion and every tier threshold. The three defects that matter are each confined to a milestone that is not first in its SPEC's sequence, so implementation can begin on M1 while they are corrected.

**Blocking a specific milestone (fix before that milestone starts):**

1. **A1 → blocks SPEC-AUTH-001 M3.** Declare the test-scoped protected/admin endpoint fixture in `plan.md §C.5` and M3 deliverables, naming the two paths. Without it, AC-AUTH-010/011/012/013/014 cannot execute. M1 (회원 도메인) and M2 (토큰 발급) are unaffected — AUTH-001 can start today.
2. **E1 → blocks SPEC-ENROLLMENT-001 M4.** Add (i) the third duplicate state to REQ-WRK-007, (ii) the `waitlist_entry (member_id, course_id) WHERE status='WAITING'` partial unique index to `design.md §1.1` plus a covering INV, (iii) a requirement that the promotion loop skips ineligible waiters and advances, and (iv) the corresponding ACs. M1-M3 are unaffected.
3. **E2 → blocks SPEC-ENROLLMENT-001 M4/M5.** Decide and state the `CANCEL` and `CAPACITY_INCREASE` behaviour on a `CLOSED` course, extend §A.4's result domain if a new terminal result is needed, update `design.md §4.3`, and add an AC. Coordinate with SPEC-COURSE-001 REQ-ADM-008, since delete-as-CLOSED is what makes the state reachable.

**Correct opportunistically (no milestone blocked):**

4. **E3** — fix the two stale AC cross-references in `design.md:181` (→ AC-ENR-008/009) and `design.md:203` (→ AC-ENR-026). One-line edits; worth doing before run-phase because both sit in the sections an implementer reads for the D4 and D5 fixes.
5. **E4** — add a receipt-side serialization row to `design.md §6` so the latency budget is end-to-end, matching REQ-STS-003's own scope.
6. **E5** — add V8 ("커밋 중 행 가시화가 권고 잠금 해제보다 먼저 일어난다") to `research.md §7`, verifiable with the two-connection harness V1 already requires.
7. **A2** — resolve REQ-AUTHZ-006's third endpoint: either drop 강좌 조회 from the enumeration or annotate the matrix row with the cross-SPEC deferral.
8. **C1** — add a DB-level constraint on `course.status` and a bypass assertion to AC-CRS-005, matching the AC-CRS-002/003 pattern.
9. **C2** — reword `spec.md:42`'s "항상 0" to match REQ-CRS-004's code-path scope.
10. **E6 / A3 / C3** — split the TDD process clause out of REQ-NFR-004 / REQ-NFR-003 / REQ-NFR-006, and rewrite the three DoD lines that reproduce the `[NEEDS CLARIFICATION` literal.

### Assessment of the rewrite

The v0.2.0 rewrite of SPEC-ENROLLMENT-001 closed all twelve prior defects with genuine changes rather than wording adjustments — three of them (D1, D2, D4) required real design changes, and all three were made rather than argued around. Two choices deserve specific credit because both were the harder option:

- **`research.md:69` rejects redefining 접수 순서 as `id` order** — the cheapest of the three fixes the prior audit offered, and the one that would have made the requirement true by weakening it. The stated reason ("요구사항을 실패에 맞춰 약화시키는 것. 사용자 가치가 훼손된다") is the correct one.
- **AC-ENR-007 is a control group that must fail**, gated in the DoD at `acceptance.md:443` with the explicit consequence that AC-ENR-006's pass is not accepted as evidence if the control also passes. This directly answers the prior audit's "flaky test reports green while an unconditional `shall not` is only probabilistically satisfied" critique, and it is a stronger construction than simply adding another assertion.

The three-way split into AUTH / COURSE / ENROLLMENT is sound: the ownership boundaries are asserted from *both* sides (COURSE cedes `enrolled_count` write authority in REQ-CRS-004 and mechanically proves it has no such path via AC-CRS-004; ENROLLMENT claims it in REQ-WRK-002 and proves single-path convergence via AC-ENR-008). That two-sided construction is what makes the split load-bearing rather than cosmetic.

### Verification transparency — what I re-derived and what I did not

**Re-derived independently (author claims not accepted):** all traceability counts and diffs in both directions for all three SPECs; REQ/INV sequencing and duplicate checks; GEARS annotation and connective alignment across all 96 requirements; frontmatter field-by-field validation; weasel-word scans; the MP-7 marker inspection (all 13 hits read individually); the D1 consistency argument re-derived step by step; the D2 interleaving re-walked against the new design in both orderings; every AC cross-reference in the non-acceptance artifacts extracted and checked; the D7 cross-SPEC status query; and every project-constraint claim checked against `build.gradle`, `tech.md`, `quality.yaml`, and the actual source tree.

**Accepted without independent re-derivation — three items, named explicitly:**

1. **PostgreSQL runtime semantics.** I did not run a database. Sequence non-transactionality, `SKIP LOCKED` visibility semantics, `pg_advisory_xact_lock` transaction scoping, and the commit-path ordering of visibility versus lock release were assessed against documented engine behaviour. The SPEC itself declares these as hypotheses pending run-phase verification (`research.md:21`, §7 V1-V7), and E5 records the one link that verification list omits. **If any of these engine assumptions is wrong, the D1 fix does not hold** — that risk transfers to run-phase M1 and is correctly gated by `acceptance.md:447`.
2. **Throughput arithmetic feasibility.** `design.md §6`'s 200-rows-per-200ms figure and the 50% conservatism factor are calculations I checked for internal arithmetic consistency (they are consistent) but not for physical achievability on this hardware and schema. AC-ENR-026 measures it; E4 notes the missing receipt-side term.
3. **Java/Spring implementability of the stated verification techniques.** JPA metamodel cascade inspection, ArchUnit package rules, `REQUIRES_NEW` failure recording, and disabling `@Scheduled` in the test profile are all standard, but I did not prototype any of them. If one proves impractical, the affected AC (AC-ENR-009, AC-ENR-017, AC-ENR-045) would need restating.

Everything else in this report is backed by a command I ran or a line I read. **This round is a clean re-verification, not a trust-the-author pass.**

---

*Report written by plan-auditor. Iteration 2 for SPEC-ENROLLMENT-001; iteration 1 for SPEC-AUTH-001 and SPEC-COURSE-001.*
