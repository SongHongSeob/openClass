# SPEC Review Report: SPEC-ENROLLMENT-001

Iteration: 1/3
Verdict: **FAIL**
Overall Score: **0.76** (Tier L PASS threshold = 0.85)

> **M1 Context Isolation**: Reasoning context ignored per M1 Context Isolation. The author's claims relayed in the mission brief (traceability verified, waitlist funnels through one path, frontmatter/ID/Out-of-Scope self-verified, research.md omission justified) were treated as unverified hypotheses and re-derived from the artifact files alone.
>
> **Tier L input contract**: `spec.md` + `plan.md` + `acceptance.md` + `progress.md` read in full. `design.md` and `research.md` are **absent** — a Tier L required-input gap recorded as D9 below.

---

## Must-Pass Results

- **[PASS] MP-1 REQ number consistency** — 46 REQ definition lines, 46 unique IDs, zero duplicates. Per-family sequencing complete with no gaps: AUTH 001-008 (`spec.md:64-71`), CAT 001-004 (`spec.md:75-78`), FCFS 001-011 (`spec.md:82-92`), STS 001-004 (`spec.md:96-99`), WL 001-007 (`spec.md:103-109`), ADM 001-007 (`spec.md:113-119`), NFR 001-005 (`spec.md:123-127`). Zero-padding uniformly 3-digit. Domain-namespaced IDs (`REQ-{DOMAIN}-{NNN}`) are an accepted variant of the flat `REQ-NNN` form; sequencing verified per family.
- **[PASS — with recorded dissent] MP-2 GEARS format compliance** — all 46 requirements in `spec.md` §B carry an explicit pattern annotation and match a GEARS pattern in Korean rendering with the `shall` / `shall not` modal preserved verbatim. Evidence: Ubiquitous `spec.md:64` ("인증 서비스는 … 저장 **shall**하며, … 저장 **shall not**한다"); Event-driven `spec.md:65` ("**When** 미가입 이메일과 … 요청이 도착하면, 인증 서비스는 … 반환 **shall**한다"); State-driven `spec.md:86` ("**While** 대상 강좌의 확정 인원이 정원 미만이면, 큐 워커는 …"); Capability gate `spec.md:91` ("**Where** 워커 인스턴스가 2개 이상 동시 구동되는 배치이면 …"). No IF/THEN legacy syntax present.
  - **Recorded dissent (do not let this pass silently)**: a strict literal reading of checklist item AC-1 ("each AC matches one of the five EARS patterns") would FAIL this SPEC, because `acceptance.md` §D.1 is entirely Given/When/Then. I ruled PASS because MP-2's operative wording is *"Given/When/Then test scenarios **mislabeled** as EARS/GEARS"* — here the GEARS obligation is discharged by `spec.md` §B, and `acceptance.md` never claims GEARS labelling; the Tier L 5-artifact split assigns test scenarios to `acceptance.md` by design. This is a judgment call on document scope, not on artifact quality. It is surfaced rather than buried so the orchestrator can overrule it. It does not change the bottom-line verdict (already FAIL on MP-7).
  - Minor pattern-selection defect recorded as D8 below.
- **[PASS] MP-3 YAML frontmatter validity** — all 12 canonical fields present in `spec.md:1-15` with correct types: `id: SPEC-ENROLLMENT-001` (matches `^SPEC-[A-Z][A-Z0-9]+-[0-9]{3}$`), `title` quoted non-empty, `version: "0.1.0"` quoted semver, `status: draft` (valid enum member), `created: 2026-08-15` / `updated: 2026-08-15` ISO `YYYY-MM-DD`, `author: manager-spec`, `priority: P0`, `phase: "v1.0.0"`, `module:` path-like non-empty, `lifecycle: spec-anchored`, `tags:` comma-separated string. Optional `tier: L` present. **Zero rejected snake_case aliases** — no `created_at`, `updated_at`, `labels`, or `spec_id` in any of the four artifacts. `plan.md:1-15`, `acceptance.md:1-15`, `progress.md:1-15` carry the same complete canonical set.
- **[N/A — auto-pass] MP-4 Section 22 language neutrality** — single-language SPEC. `module: "src/main/java/com/hongseob/openclass_ap"`, `tags: "… spring-boot"`, and `plan.md:189-190` fix the stack at Java 17 / Spring Boot. No multi-language tooling surface; the 16-language enumeration requirement does not apply.
- **[PASS] MP-5 D7 cross-SPEC reconciliation** — no BLOCKING finding. D7 verb executed. Referenced SPEC IDs extracted: `SPEC-AUTH-001` (`plan.md:52,219`), `SPEC-COURSE-001` (`plan.md:53,226`), `SPEC-FRONTEND-001` (`plan.md:178`), plus self. `Glob(.moai/specs/SPEC-*/spec.md)` returns exactly one directory (`SPEC-ENROLLMENT-001`), so none of the three referenced SPECs exists on disk. Per **D7-5** this is **SHOULD** severity, not BLOCKING — and here it is benign: all three are explicitly framed as *proposals* (`plan.md:50` "SPEC ID (제안)", `plan.md:178` "후속 SPEC 제안"), not as existing dependencies. No referenced SPEC carries status `retired` / `superseded` / `archived`, so the D7-4 reconciliation-clause requirement is not triggered. Recorded as D11 (minor).
- **[PASS] MP-6 D8 cross-platform discipline** — D8 verb executed: `grep -rn 'syscall' *.md` returns zero matches across all four artifacts. Per **D8-4**, absence of `syscall` is auto-PASS. (Independently consistent with the JVM target — no OS-syscall surface.)
- **[FAIL] MP-7 clarification gate** — **3 unresolved `[NEEDS CLARIFICATION]` markers in `plan.md`.** Verification: `grep -rn '\[NEEDS CLARIFICATION' plan.md` →
  - `plan.md:60` — `[NEEDS CLARIFICATION: SPEC 분할 여부 — (a) 3개 SPEC으로 분할(권고) vs (b) 본 Tier L 단일 SPEC 유지]`
  - `plan.md:112` — `[NEEDS CLARIFICATION: 인증 전략 — (a) 세션 기반(권고) vs (b) JWT 기반 …]`
  - `plan.md:181` — `[NEEDS CLARIFICATION: 프론트엔드 착수 시점 — (a) 백엔드 완료 후 별도 SPEC(권고) vs (b) 본 SPEC run 단계에 스캐폴딩 포함]`
  
  `research.md` does not exist, but `plan.md` does, so the N/A carve-out does not apply. This is a **must-pass failure** and is folded into `## Defects Found` at severity=critical (D10). Per MP-7 this gate is score-independent: the orchestrator MUST resolve each marked topic via `AskUserQuestion` before Implementation Kickoff Approval. Note `plan.md:206` and `acceptance.md:299` reference the markers as *pre-flight checklist items to be satisfied* (not as false resolution claims) — those two lines are correct and are not defects.

**Must-pass firewall result: 1 of 7 FAILED → Verdict FAIL regardless of aggregate score.**

---

## Category Scores (0.0-1.0, rubric-anchored)

| Dimension | Score | Rubric Band | Evidence |
|-----------|-------|-------------|----------|
| Clarity | 0.70 | between 0.50 and 0.75, docked from 0.75 | Requirement prose is unusually precise and consistently modal-marked. Docked for **five** interpretation-affecting ambiguities, above the 0.75 band's "one or two" allowance: (a) the `enrolled_count` decrement owner is unspecified — `plan.md:96` asserts "`enrolled_count`를 변경하는 **모든 경로**가 워커 한 곳으로 수렴" while `spec.md:105` (REQ-WL-003) assigns the cancel-time state transition to "시스템" at receipt time (D2); (b) "접수 순서" is used as ground truth in `spec.md:189` (§E.2) and `spec.md:85` (REQ-FCFS-004) but operationalized as `enrollment_request.id` order in `acceptance.md:108` without ever stating the two are the same (D1); (c) `spec.md:99` REQ-STS-004 "**정상 부하**에서 5초 이내" leaves 정상 부하 unquantified (D5); (d) the queue row's internal `state` / `result` value set is never enumerated — `plan.md:76` declares both columns, `spec.md:47-56` §A.3 defines only the *client-facing* enum, and `acceptance.md:284` references a `PROCESSING` state that appears in no definition (D6); (e) six requirements use the bare subject "시스템" (`spec.md:89,105,107,117,118,123`) instead of a named component, leaving the responsible tier ambiguous — most consequentially REQ-FCFS-008 (`spec.md:89`), where `acceptance.md:120` pins duplicate detection at the worker but the requirement does not. |
| Completeness | 0.75 | 0.75 | All required `spec.md` sections present: HISTORY `spec.md:19`, WHY `spec.md:29` (§A.1 배경), WHAT `spec.md:27` (§A 개요) + §B, REQUIREMENTS `spec.md:60`, INVARIANTS `spec.md:131`, Out of Scope `spec.md:146`, 성공 기준 `spec.md:186`. Out of Scope is exemplary and satisfies the `OutOfScopeRule` H3 convention exactly — five `### Out of Scope — <topic>` sub-headings at `spec.md:150,156,162,170,177`, each with concrete `-` bullets, plus an explicit anti-scope-creep prohibition at `spec.md:148` and a "무단 대체 금지" clause at `spec.md:168`. Frontmatter complete (MP-3). Docked for: the Tier L artifact set is **3 of 5** — `design.md` and `research.md` absent (D9); and the queue state machine gap (D6). |
| Testability | 0.75 | 0.75 | 35 ACs, all Given/When/Then with observable post-conditions; `acceptance.md:19` explicitly forbids subjective criteria ("빠르게 느껴진다" 같은 주관적 판정은 인수 기준이 될 수 없다) and the corpus honours it — **zero** weasel words ("적절한"/"합리적인"/"충분한") found in any AC. Strong quantification throughout (`acceptance.md:102` "정확히 10건", `acceptance.md:76` "잔여 정원 6", `acceptance.md:158` "20회 반복"). Disjunctive status codes (`acceptance.md:32` "201(또는 200)", `acceptance.md:152` "403 또는 404") stay binary-decidable and are acceptable. Docked for: AC-FCFS-003b is **non-deterministic with respect to the guarantee it claims** (D1) — it will pass on almost every run while the underlying ordering violation remains possible; the two structural ACs (`acceptance.md:94` AC-FCFS-002, `acceptance.md:178` AC-WL-003) rely on source-text search that JPA cascade/dirty-checking persistence can bypass (D4); the §A.1-stated core scenario (신청 개시 시각 thundering herd) has no load-shaped AC (D5); and the seven edge cases at `acceptance.md:274-286` carry no AC IDs, are absent from the §D.2 matrix, and are therefore not gated by the §D.4 Definition of Done (D7). |
| Traceability | 0.85 | between 0.75 and 1.0 | Re-verified independently and mechanically, not from the author's claim. Extracted every `- 대응:` line (35 lines, `acceptance.md:33`→`248`) and diffed the referenced-ID set against the 46 REQ definitions: **45 of 46 REQs covered; 1 gap.** All six invariants covered (INV-001 `acceptance.md:103,115,173`; INV-002 `:97,179`; INV-003 `:121`; INV-004 `:167`; INV-005 `:127`; INV-006 `:133`). **Zero orphaned ACs** — every `대응:` target resolves to a REQ or INV that exists in `spec.md`. The single gap: `REQ-FCFS-006` never appears as a well-formed token; `acceptance.md:103` writes the shorthand `REQ-FCFS-005/006`, which no mechanical extractor resolves (D3). Additionally, the §D.2 matrix (`acceptance.md:254-268`) uses **range notation** ("REQ-FCFS-001 ~ 011 \| AC-FCFS-001 ~ 008") and the closing line `acceptance.md:270` asserts "누락된 요구사항은 없다" — the matrix *asserts* coverage rather than *demonstrating* it per-REQ, which is precisely how the D3 gap survived authoring. |

**Aggregate**: (0.70 + 0.75 + 0.75 + 0.85) / 4 = **0.7625** → below the Tier L threshold of 0.85. Verdict FAIL is doubly determined (MP-7 firewall + aggregate).

---

## Defects Found (structured defect-list)

**D1. FCFS-ORDER-COMMIT-VISIBILITY — `plan.md:76,138-143` + `spec.md:85,189` + `acceptance.md:108` — Severity: critical — the design does NOT guarantee the confirmed set is the first N arrivals; it guarantees only "no overselling".**

Concrete failure scenario (single worker, PostgreSQL READ COMMITTED — no multi-instance required):
1. Member X's receipt transaction `INSERT`s into `enrollment_request` and is allocated `id = 5`. PostgreSQL sequences are allocated **non-transactionally at INSERT time**, and the row is invisible to other transactions until X's transaction commits. X's commit is delayed (GC pause, connection-pool contention, a slower `@Transactional` boundary).
2. Member Y's receipt transaction is allocated `id = 6` and commits immediately.
3. The worker's next poll executes `SELECT … WHERE state='PENDING' ORDER BY id ASC … FOR UPDATE SKIP LOCKED` (`plan.md:138-143`). It sees `id=6` and **cannot see `id=5`** — an uncommitted row is not merely locked (which `SKIP LOCKED` would handle) but *invisible*.
4. The worker confirms `id=6`. This consumes the last remaining seat.
5. X commits. The next poll picks up `id=5`, finds `enrolled_count == capacity`, and routes X to `WAITLISTED`.

Result: Y, who was assigned a **later** sequence value than X, was confirmed **before** X. This directly violates `spec.md:85` REQ-FCFS-004 — "나중에 접수된 요청을 먼저 확정 **shall not**한다" — which is stated as an unconditional prohibition, and falsifies `spec.md:189` §E.2 success criterion 2. `plan.md` §C.1 and §C.4 never mention the hazard; `plan.md:76` simply asserts `id`(BIGSERIAL = 접수 순서) as if sequence order were commit order.

Why the acceptance criteria cannot catch it: `acceptance.md:108` (AC-FCFS-003b) asserts the confirmed set equals the top-10 by `enrollment_request.id` — which is exactly the property this scenario violates, so the AC *is* aimed at the right target, but the violation is a **race with a sub-poll-interval window**. With `fixedDelay = 500ms` (`plan.md:130`), most in-flight inserts commit long before the next poll, so AC-FCFS-003b will pass on the overwhelming majority of runs. The residual risk concentrates exactly at the seat-boundary request — the one that decides slot N. The net effect is a **flaky test that reports green while an unconditional `shall not` is only probabilistically satisfied.**

Required fix — choose one and record it in `spec.md`:
- **(a) Definitional (cheapest, honest)**: amend `spec.md:84-85` and `spec.md:189` to define 접수 순서 **as** `enrollment_request.id` order, and add an explicit residual-risk clause stating that sequence-allocation order may diverge from wall-clock arrival order under concurrent commits. This makes REQ-FCFS-004 satisfiable as written.
- **(b) Mechanical**: add a visibility watermark to the claim query — process only rows whose id is below the minimum in-flight sequence value (e.g. via `pg_snapshot_xmin(pg_current_snapshot())` or a commit-order column populated at commit time), accepting a bounded latency penalty against REQ-STS-004.
- **(c)** If neither is adopted, weaken REQ-FCFS-004 from `shall not` to a best-effort statement and say so explicitly. Do not leave an unconditional prohibition backed by a probabilistic mechanism.

---

**D2. ENROLLED-COUNT-DECREMENT-OWNER-UNSPECIFIED — `plan.md:96` vs `spec.md:105` + `acceptance.md:172,184` — Severity: critical — the plan's central design claim is not supported by the requirements as written, and the unspecified timing admits a first-come-first-served violation.**

`plan.md:96` makes the load-bearing claim: "취소·승격도 반드시 큐를 경유한다. 이렇게 하면 `enrolled_count`를 변경하는 **모든 경로**가 워커 한 곳으로 수렴하여 INV-001이 **설계 자체로** 보장된다."

But `spec.md:105` (REQ-WL-003) assigns the cancel-time work to "시스템" at receipt: "확정된 수강신청의 취소가 접수되면, 시스템은 **해당 확정 레코드를 취소 상태로 전이하고** 승격 처리 작업을 큐에 적재 **shall**한다." So the enrollment record's `ENROLLED → CANCELLED` transition happens on the cancel API's path, **not** in the worker. The requirement is silent on which path decrements `course.enrolled_count`. Both readings are consistent with every AC:
- `acceptance.md:172` (AC-WL-002) asserts `enrolled_count` is 2 *after* the worker has processed the promotion — satisfied whether the decrement happened at cancel-receipt or inside the worker.
- `acceptance.md:184` (AC-WL-004) asserts `enrolled_count` is "1 감소한 값" *after* worker processing — likewise silent on timing.
- `acceptance.md:178` (AC-WL-003) verifies only that the cancel path does not create an `enrollment` row — it says nothing about `enrolled_count` mutation.

Concrete failure scenario under the "cancel API decrements immediately" reading (capacity 2, A and B enrolled, C and D already waitlisted at positions 1 and 2):
1. Member E submits a new enrollment request → `enrollment_request.id = 99`, `state = PENDING`. The worker has not yet reached it.
2. Member A cancels. The cancel API transitions A's enrollment to CANCELLED, decrements `enrolled_count` 2 → 1, and enqueues a `PROMOTE` job at `id = 100`.
3. The worker processes in id order. At `id = 99` it evaluates E: `enrolled_count (1) < capacity (2)` → **E is confirmed**.
4. At `id = 100` it processes the `PROMOTE`: `enrolled_count (2) == capacity (2)` → per `spec.md:108` (REQ-WL-006) it must not exceed capacity, so **C is not promoted and remains waiting**.

Result: E, who applied *after* C and D were already on the waitlist, took the freed seat ahead of both. No requirement forbids this — `spec.md:106` (REQ-WL-004) only says that *when a promote job is processed and waiters exist*, promote the front one; nothing establishes waitlist precedence over newly-arriving direct applicants. No AC detects it. For a system whose stated core value is 선착순 정합성 (`spec.md:31`), silently letting a latecomer overtake the waitlist is a substantive fairness defect.

Required fix: (1) state explicitly in `spec.md` which path decrements `enrolled_count` — routing the decrement through the worker as a queue job would make `plan.md:96`'s claim true as written; (2) add a requirement establishing whether waitlisted members have precedence over new arrivals for a freed seat, and (3) add an AC covering the interleaving above (a pending `ENROLL` with a lower id than a `PROMOTE` job).

---

**D3. TRACEABILITY-MALFORMED-ID-REQ-FCFS-006 — `acceptance.md:103` — Severity: major — one requirement is not mechanically traceable, and the traceability matrix asserts completeness rather than demonstrating it.**

`acceptance.md:103` writes `- 대응: REQ-FCFS-005/006, REQ-NFR-001, INV-001`. The token `REQ-FCFS-006` never occurs in well-formed shape anywhere in `acceptance.md`. Mechanical verification: extracting `(REQ|INV)-[A-Z]+-[0-9]+` from all 35 `대응:` lines and diffing against the 46 REQ definitions in `spec.md` yields exactly one uncovered requirement — `REQ-FCFS-006` (`spec.md:87`, the state-driven "정원 도달 시 대기명단 경로로 분기" requirement).

Substantively the behaviour *is* exercised (`acceptance.md:102` asserts the 40 non-confirmed requests are `WAITLISTED`, and `acceptance.md:166` AC-WL-001 covers the same branch), so this is a citation defect rather than a coverage hole — but it is the exact class of defect that any automated traceability gate will flag, and it survived authoring **because** the §D.2 matrix (`acceptance.md:254-268`) uses range notation ("REQ-FCFS-001 ~ 011") and then asserts at `acceptance.md:270` "누락된 요구사항은 없다". A range assertion cannot expose a per-REQ gap.

Required fix: rewrite `acceptance.md:103` as `- 대응: REQ-FCFS-005, REQ-FCFS-006, REQ-NFR-001, INV-001`, and replace the range notation in §D.2 with a per-REQ row (46 rows) so the matrix demonstrates coverage instead of asserting it.

---

**D4. INV-002-STRUCTURAL-AC-BYPASSABLE — `acceptance.md:94-96` + `acceptance.md:178` — Severity: major — the mechanical guard on the single-confirmation-path invariant has a JPA-shaped hole.**

`acceptance.md:94-96` (AC-FCFS-002) verifies INV-002 by static search: "`enrollment` 저장(`save`/`persist`) 호출 지점을 정적 검색하면 … 워커 처리 경로 내부 1개소에서만 발견된다", with verification method "소스 검색(grep/ast-grep) + 아키텍처 테스트". `acceptance.md:178` (AC-WL-003) uses the same technique for the cancel path.

In JPA/Hibernate, an `Enrollment` row can be inserted **without any textual `save` or `persist` call**: via cascade from an owning collection (`course.getEnrollments().add(new Enrollment(...))` with `CascadeType.PERSIST/ALL`), via `EntityManager.merge`, or via dirty-checking on a managed graph. A grep-based AC therefore passes green while a second creation path exists — defeating INV-002 (`spec.md:138`), which is the *structural* first line of defense that `plan.md:82` relies on ("동시 실행 자체가 존재하지 않으므로 경쟁 조건이 발생하지 않는다").

Required fix: strengthen AC-FCFS-002's stated verification method from source search to a behavioural or DB-level guard — e.g. an ArchUnit rule constraining which packages may reference the `Enrollment` aggregate root, plus a Hibernate `@PrePersist`/interceptor assertion that the persisting thread is the worker thread; or forbid cascade persistence onto `Enrollment` in the mapping and assert that in a test.

---

**D5. REQ-STS-004-UNBOUNDED-LOAD — `spec.md:99` + `plan.md:130` + `acceptance.md:146` — Severity: major — the 5-second latency requirement is violated by the chosen worker throughput under exactly the scenario the SPEC names as its motivating case, and no AC detects it.**

`spec.md:99` (REQ-STS-004) requires terminal-state latency ≤ 5초 "정상 부하에서" — 정상 부하 is never quantified anywhere in the four artifacts. `plan.md:130` fixes throughput at `fixedDelay = 500ms`, max 50 rows per batch → a ceiling of **~100 queue rows/second**. `plan.md:131` concludes "REQ-STS-004의 5초 목표에 충분한 여유" — that conclusion holds only for a queue depth below ~500.

But `spec.md:31` (§A.1) names the motivating scenario as "인기 강좌의 **신청 개시 시각에 다수의 요청이 동시에 도착**". Concrete failure: a capacity-100 popular course opens and 3,000 applicants arrive within the first second. Queue depth 3,000 ÷ 100 rows/sec = **~30 seconds** for the last request to reach a terminal state — a 6× breach of REQ-STS-004, arising directly from the documented configuration, with no bug required.

No AC catches this: `acceptance.md:146` (AC-STS-001) polls a **single** request; `acceptance.md:99-103` (AC-FCFS-003a), the SPEC's designated single gate, uses only 50 concurrent requests — comfortably inside one batch — and asserts correctness, never latency.

Required fix: (1) quantify 정상 부하 in REQ-STS-004 as a concrete concurrent-request figure; (2) reconcile `plan.md:130`'s batch/delay parameters against that figure (raise batch size, shorten `fixedDelay`, or state the degradation explicitly); (3) add an AC that measures terminal-state latency at the quantified load, not at N=1.

---

**D6. QUEUE-STATE-MACHINE-UNDEFINED — `plan.md:76` + `spec.md:47-56,142` + `acceptance.md:284` — Severity: major — INV-006 is asserted over a state set that is never enumerated, and a state referenced in the edge cases exists in no definition.**

`plan.md:76` declares the queue table with **two** distinct status columns, `state` and `result`, and defines neither's value domain. `spec.md:47-56` (§A.3) enumerates six values but scopes them explicitly as a "client-facing enum" — a presentation contract, not the internal machine. `spec.md:142` (INV-006) then asserts "요청 상태 전이는 단방향이다 — 종단 상태에서 `PENDING`으로 되돌아가지 않는다" over this undefined set. `acceptance.md:284` (edge case 7) references a **`PROCESSING`** state that appears in no definition in any of the four artifacts.

For a SPEC whose entire correctness argument rests on the queue mechanism, leaving the queue's own state machine undefined is a first-order completeness gap: `state` vs `result` semantics, the legal transition set, which values are terminal, and how `PROCESSING` relates to the client-facing `PENDING` are all left to run-phase invention. (Note: I checked whether edge case 7's proposed `PROCESSING → PENDING` recovery would violate INV-006 — it does not, since `PROCESSING` is not terminal. The defect is the missing definition, not a contradiction.)

Required fix: add a §A.4 to `spec.md` enumerating the internal `state` domain (including `PROCESSING`), the `result` domain, the legal transition edges, and the terminal set — then restate INV-006 against that named set.

---

**D7. EDGE-CASES-UNGATED — `acceptance.md:274-286` + `acceptance.md:290-301` — Severity: minor — seven declared must-test edge cases carry no AC IDs and are excluded from the Definition of Done.**

`acceptance.md:276` states these are "구현 시 **반드시** 테스트로 다뤄야 하는 경계 상황", but the seven items have no AC identifiers, do not appear in the §D.2 traceability matrix (`acceptance.md:254-268`), and the Definition of Done (`acceptance.md:294`) gates only on "§D.1의 심각도 '필수' AC가 전부 통과". A 반드시 obligation that no checklist item enforces will not be enforced. Two are individually load-bearing: item 1 (정원 = 1) is the classic off-by-one boundary for the SPEC's single gate, and item 4 (취소 직후 즉시 재신청) defines the semantics of the `WHERE status = 'ENROLLED'` partial unique index at `plan.md:86`. Item 7 additionally defers a real design decision ("자동 복구까지 구현할지는 run 단계에서 판단", `acceptance.md:286`) without carrying a `[NEEDS CLARIFICATION]` marker, so it escapes the MP-7 gate that would otherwise surface it to the user.

Required fix: promote at least edge cases 1, 4, and 5 to numbered ACs in §D.1, add them to the §D.2 matrix, and either resolve item 7 or mark it `[NEEDS CLARIFICATION]` so it enters the clarification gate.

---

**D8. GEARS-WHERE-VS-WHILE-MISSELECTION — `spec.md:71,113` — Severity: minor — two requirements use the `Where` capability-gate pattern for a runtime state condition.**

GEARS reframes `Where` as a **capability gate / feature flag / static config**, distinct from `While` (state-driven runtime condition). `spec.md:71` (REQ-AUTH-008) — "**Where** 회원의 역할(role)이 `ADMIN`이 아니면" — and `spec.md:113` (REQ-ADM-001) — "**Where** 요청자가 `ADMIN` 역할이면" — gate on the *requester's runtime role*, which is per-request state, not deployment configuration. `While` (state-driven) is the correct selector for both. By contrast `spec.md:91` (REQ-FCFS-010) — "**Where** 워커 인스턴스가 2개 이상 동시 구동되는 배치이면" — is a correct `Where`: it gates on a deployment topology, i.e. static config. 2 of 46 requirements affected; both remain well-formed and unambiguous in meaning, so this does not fail MP-2.

Required fix: change the annotation and connective on `spec.md:71` and `spec.md:113` from `Where` to `While`.

---

**D9. TIER-L-ARTIFACT-SET-INCOMPLETE — `plan.md:26,257` + `spec.md:14` — Severity: major — the research.md omission is well-argued but demonstrably did hide a real gap.**

`spec.md:14` declares `tier: L`, which mandates the 5-artifact set (spec / plan / acceptance / design / research). Only 3 of 5 exist. `plan.md:26` and `plan.md:257` justify the omission: "그린필드 … 기존 코드와의 충돌 분석(research.md)은 실질적으로 공집합", with design.md folded into `plan.md` §C.

Assessing the justification on its merits rather than accepting it:
- **The design.md fold-in is sound.** `plan.md` §C (`:66-181`) carries genuine design content — the 5-table model, the three-layer defense, `request_type` as the convergence mechanism, four alternatives-considered tables with explicit rejection rationale, and the package structure. Nothing of design.md's function is missing.
- **The research.md omission is *not* sound, and the evidence is in this report.** The greenfield argument treats research.md as *existing-code analysis only*. For a greenfield SPEC whose entire risk surface is one concurrency mechanism, the research that is actually missing is **technology research**: PostgreSQL `FOR UPDATE SKIP LOCKED` visibility semantics, sequence-allocation vs commit-order ordering guarantees, `@Scheduled` + `@Transactional` interaction, and Testcontainers concurrency-test methodology. **D1 is precisely a finding that a technology-research pass would have surfaced** — it is a documented, well-known PostgreSQL queue-table hazard, and it is absent from both `spec.md` and `plan.md`. The omission therefore did hide a real gap, and the gap is critical-severity.
- Secondary consequence: the Tier L plan-auditor input contract requires reading design.md + research.md; their absence is an audit-input gap recorded here rather than silently passed.

Required fix: author `research.md` covering the four technology-research topics above (minimally: the sequence-vs-commit-order hazard and its mitigations, which directly feeds D1's fix), or formally downgrade `tier:` with the artifact-set implications recorded. Retain the design.md fold-in — that part of the judgment holds.

---

**D10. UNRESOLVED-CLARIFICATION-MARKERS (MP-7 must-pass) — `plan.md:60,112,181` — Severity: critical — clarification gate.**

Three unresolved `[NEEDS CLARIFICATION]` markers (full text under MP-7 above). Per MP-7 this forces `Verdict: FAIL` independent of aggregate score, and the orchestrator MUST resolve each via `AskUserQuestion` before Implementation Kickoff Approval.

Assessed against mission item 5 (are they clearly flagged, and do they silently bias the SPEC?):
- **Flagging is exemplary.** All three use the literal `[NEEDS CLARIFICATION: …]` marker convention, are placed inline at the decision site (`plan.md:60` in §B.2, `:112` in §C.2, `:181` in §C.6), are re-listed as pre-flight blockers at `plan.md:206`, tracked at `progress.md:24`, and gated in the Definition of Done at `acceptance.md:299`. Four surfaces; none omitted.
- **Bias assessment — no silent bias, but non-neutral by construction.** Each marker states a recommendation ("(권고)") with reasoning. That is disclosure, not concealment: `plan.md:102-107` tabulates option A vs B with 장점/단점 before recommending, and `plan.md:107` states the recommendation's precondition explicitly ("v1은 단일 인스턴스 배포를 가정한다"), while `plan.md:112` names the condition that would overturn it ("향후 다중 인스턴스 배포 계획이 있다면 (b)를 재검토"). A reader can reject each recommendation on a stated precondition. **One residual asymmetry**: the SPEC-split marker at `plan.md:60` recommends splitting into 3 SPECs, yet `spec.md`, `acceptance.md`, and `progress.md` are all authored as the single Tier L SPEC. `plan.md:58` addresses this directly and correctly ("분할 여부와 무관하게 spec.md/acceptance.md는 재작성이 불필요하다" — M1/M2/M3 map 1:1 to the proposed split), which I verified against `plan.md:214-236`: M1 ↔ SPEC-AUTH-001, M2 ↔ SPEC-COURSE-001, M3 ↔ SPEC-ENROLLMENT-001 (축소판). The mapping holds, so the artifact layout does not foreclose the recommended option. **Verdict on item 5: clearly flagged, not silently biased.**

Required fix: orchestrator resolves all three via `AskUserQuestion` before Implementation Kickoff Approval; record the decisions in `plan.md` and clear the markers.

---

**D11. D7-SHOULD-DANGLING-SPEC-REFERENCES — `plan.md:52,53,178,219,226,234` — Severity: minor.**

`SPEC-AUTH-001`, `SPEC-COURSE-001`, and `SPEC-FRONTEND-001` are referenced but do not exist under `.moai/specs/`. Per D7-5 this is SHOULD severity, and here it is benign — all three are labelled as proposals (`plan.md:50` "SPEC ID (제안)", `plan.md:178` "후속 SPEC 제안"), and none carries a retired/superseded/archived status requiring a reconciliation clause. Recorded for completeness. Required fix: none blocking; if the split marker (D10) resolves to option (a), create the two prerequisite SPECs and add `depends_on:` entries.

---

**D12. CANCEL-ENDPOINT-AUTHORIZATION-MISSING — `spec.md:105` + `spec.md:70` + `acceptance.md:53-57` — Severity: major — no requirement and no acceptance criterion prevents one member from cancelling another member's enrollment (IDOR).**

Found on the Chain-of-Verification second pass. `spec.md:105` (REQ-WL-003) is written impersonally — "확정된 수강신청의 **취소가 접수되면**" — and never constrains *who* may cancel a given enrollment. Verification: `grep -n '취소' spec.md` returns `:70, :105, :109, :152, :190`; of these, only `spec.md:109` (REQ-WL-007) carries an ownership notion, and only for *waitlist* cancellation ("대기자 **본인**이"). The confirmed-enrollment cancellation path has no ownership wording at all. `grep -n '본인\|소유자\|타인\|자신의' spec.md acceptance.md` returns three hits — `spec.md:96` (status read), `spec.md:109` (waitlist cancel), `acceptance.md:149` (status read) — confirming no cancel-ownership requirement or AC exists.

The neighbouring controls do not close the gap: `spec.md:70` (REQ-AUTH-007) requires only *authentication* for the cancel endpoint (401 for anonymous), and `acceptance.md:53-57` (AC-AUTH-005) tests exactly that 401 and nothing more. `spec.md:97` (REQ-STS-002) protects only status *reads*. There is no analogue for the cancel *write*.

Concrete failure scenario: member B authenticates legitimately, obtains or guesses member A's enrollment identifier (sequential `BIGSERIAL` per `plan.md:77`, so identifiers are enumerable), and calls the cancel endpoint with it. Every stated requirement is satisfied — B is authenticated (REQ-AUTH-007 ✓), the enrollment transitions to cancelled and a `PROMOTE` job is enqueued (REQ-WL-003 ✓), the invariant `enrolled_count ≤ capacity` still holds (INV-001 ✓) — yet A has been forcibly unenrolled from a course by a stranger, and A's seat is handed to the next waitlisted member. No AC fails. This is an OWASP A01 broken-access-control defect reachable through requirements-conformant behaviour, and `spec.md:124` (REQ-NFR-002, server-side validation of all external input) is too generic to be read as covering object-level authorization.

Required fix: (1) add a requirement — e.g. `REQ-WL-008` (Event-driven): "**When** 확정 수강신청의 취소 요청이 그 수강신청의 소유자가 아닌 회원으로부터 도착한 것이 감지되면, 시스템은 취소를 수행 **shall not**하며 403 또는 404를 반환 **shall**한다"; (2) extend the same ownership constraint to REQ-WL-007's waitlist-cancel path, which asserts 본인 but never forbids the non-owner case; (3) add a matching AC modelled on `acceptance.md:149` (AC-STS-002), asserting that B's cancel attempt on A's enrollment is rejected **and** that A's `ENROLLED` row and `enrolled_count` are unchanged.

---

## Chain-of-Verification Pass

Second-look findings: **1 new critical-class defect discovered (D12), 1 first-pass hypothesis corrected, 2 non-defects confirmed.**

Re-read sections and re-checks performed:

1. **Every REQ individually (`spec.md:64-127`), not sampled.** Re-read all 46. New finding: six requirements use the bare subject "시스템" (`spec.md:89,105,107,117,118,123`) rather than a named component — folded into the Clarity deduction (item e). REQ-FCFS-008 (`spec.md:89`) is the consequential case: the requirement does not say where duplicate detection occurs, while `acceptance.md:120` pins it at the worker.
2. **REQ sequencing end-to-end, mechanically** — not spot-checked. `grep -oE 'REQ-[A-Z]+-[0-9]+' | sort -u` → 46 unique IDs; `grep -cE '^\- \*\*REQ-'` → 46 definition lines. Equal counts prove zero duplicates; per-family ranges prove zero gaps.
3. **Traceability for every REQ, mechanically** — not sampled. `comm -23` of the 46-REQ definition set against the ID set extracted from all 35 `대응:` lines. Exactly one gap (D3). Also ran the reverse direction for orphaned ACs: zero.
4. **Out of Scope specificity, not mere presence.** `grep -n '^### Out of Scope'` → five H3 sub-headings at `spec.md:150,156,162,170,177`, each carrying concrete `-` bullets (not placeholders), plus the `spec.md:168` clause forbidding unauthorized substitution of the excluded alternatives. Satisfies the `OutOfScopeRule` H3 convention.
5. **Cross-requirement contradictions, not just intra-requirement.** Systematic pairwise scan of §B against §C invariants and against `plan.md` §C design claims. Yield: D2 (plan claim vs REQ-WL-003), D1 (REQ-FCFS-004 vs the ordering mechanism), and the unconditional-invariant issue folded into D1's context — namely that `spec.md:85` (REQ-FCFS-004) and the `spec.md:135-142` invariant table are stated **unconditionally**, while `plan.md:133` records that order preservation holds only under the single-worker assumption ("워커 인스턴스는 1개다 … 전제 위에서 REQ-FCFS-004가 성립") and `spec.md:91` (REQ-FCFS-010) simultaneously contemplates ≥2 workers as a supported deployment. The single-instance precondition lives only in `plan.md`; the normative document carries no such qualifier. This should be lifted into `spec.md` alongside D1's fix.
6. **NEW — authorization coverage of every write endpoint.** First pass checked authentication (REQ-AUTH-007) and read-authorization (REQ-STS-002) but never audited *write* authorization per endpoint. Doing so surfaced **D12**: the enrollment-cancellation endpoint has no ownership requirement and no AC. Confirmed by two independent greps (`취소` across `spec.md`; `본인\|소유자\|타인\|자신의` across `spec.md` + `acceptance.md`).
7. **CORRECTION to a first-pass hypothesis.** I initially drafted a defect asserting that the third line of defense (`CHECK (enrolled_count <= capacity)`, `plan.md:84`) protects only a denormalized proxy and so cannot enforce INV-001's row-count wording. Re-reading `plan.md:83` shows the second line explicitly couples them in one transaction — "워커가 **같은 트랜잭션 안에서** `course.enrolled_count`를 읽고 `< capacity`일 때만 `enrollment` INSERT + `enrolled_count` 증가" — which makes the CHECK transitively bound the row count under the stated design. `acceptance.md:102` (AC-FCFS-003a) further asserts *both* the row count (정확히 10건) and the counter (`enrolled_count = 10`), so the coupling is test-observable. **I withdrew that defect rather than keep an overstated finding.** The residual — that `acceptance.md:111-115` (AC-FCFS-004) validates only the counter constraint and no AC asserts a DB-level bound on `COUNT(enrollment WHERE status='ENROLLED')` — is real but minor, and is subsumed by D4's stronger structural point about INV-002 enforcement.
8. **Non-defect confirmed (INV-006 vs edge case 7).** Checked whether `acceptance.md:284`'s proposed recovery of a stuck `PROCESSING` row back to a processable state violates `spec.md:142` (INV-006, no terminal → `PENDING`). It does not: `PROCESSING` is not a terminal state per `spec.md:49-56`. The genuine defect here is the undefined state set (D6), not a contradiction.
9. **Non-defect confirmed (duplicate-request race).** Checked whether two concurrent identical enrollment requests from the same member can both be confirmed. They cannot: both land as `PENDING`, the worker processes them sequentially in id order, the second observes the first's `ENROLLED` row and terminates it `REJECTED` (`spec.md:89` REQ-FCFS-008, `acceptance.md:117-121` AC-FCFS-005), with the `plan.md:86` partial unique index on `(member_id, course_id) WHERE status='ENROLLED'` as a mechanical backstop. The queue design handles this case well.
10. **Mission item 4 verified from the data model, not from the claim.** `plan.md:76` declares `request_type` on `enrollment_request`; `plan.md:92-96` defines the `{ENROLL, PROMOTE}` domain and routes both through the same worker; `acceptance.md:175-179` (AC-WL-003) verifies structurally that the cancel path only enqueues a `PROMOTE` row. **The claim is TRUE for `Enrollment` record creation** — direct enrollment and waitlist promotion do converge on one confirmation path. Two caveats attach: the `enrolled_count` mutation path does *not* converge as `plan.md:96` claims (D2), and the structural verification of the convergence is bypassable (D4).

---

## Regression Check

Not applicable — iteration 1. No prior iteration report exists at `.moai/reports/plan-audit/` (directory contained only `.gitkeep` before this audit). The mission notes an earlier attempt on this SPEC went idle without delivering findings; no report artifact from it was found on disk, so there is no prior defect list to regress against and this is a full first-iteration audit, not a delta re-audit.

---

## Recommendation

**Verdict FAIL**, driven by MP-7 (must-pass firewall) and independently by aggregate score 0.76 < 0.85 (Tier L threshold).

Important framing for the orchestrator: this is a **high-quality SPEC with two genuine design defects and a mandatory procedural gate** — it is not a low-quality artifact. The requirement corpus is precise and consistently GEARS-modal, the Out-of-Scope section is exemplary, frontmatter is fully canonical, traceability is 45/46 with the single miss being a citation typo, and the acceptance criteria are genuinely binary with zero weasel words. The FAIL is not a condemnation of drafting quality; D10 in particular is the *expected* state of a freshly-authored draft awaiting its clarification round.

Fix order (highest leverage first):

1. **Resolve the three `[NEEDS CLARIFICATION]` markers (D10)** via `AskUserQuestion` — `plan.md:60`, `plan.md:112`, `plan.md:181`. This clears the MP-7 must-pass gate and is a precondition for Implementation Kickoff Approval. The SPEC-split answer also determines whether D11 needs action.
2. **Close D1 (FCFS ordering)** — `spec.md:84-85`, `spec.md:189`, `plan.md:76,138-143`. Adopt fix (a) (define 접수 순서 as `enrollment_request.id` order + record the residual risk) unless true arrival-order fidelity is required, in which case adopt (b). While editing, lift the single-worker precondition from `plan.md:133` into `spec.md` so REQ-FCFS-004 and the §C invariant table stop asserting unconditionally what holds only for one worker instance (per-item 5 of the Chain-of-Verification pass).
3. **Close D2 (`enrolled_count` decrement owner)** — `spec.md:105`, `plan.md:96`. State which path performs the decrement, add a requirement on waitlist-vs-new-arrival precedence for a freed seat, and add an AC for the `ENROLL`-before-`PROMOTE` interleaving.
4. **Close D12 (cancel-endpoint IDOR)** — add the ownership requirement + matching AC. Small edit, security-class impact.
5. **Close D5 (load-vs-latency)** — quantify 정상 부하 in `spec.md:99`, reconcile `plan.md:130`'s throughput against it, add a load-shaped latency AC.
6. **Close D6 (queue state machine)** — add the `state` / `result` domains and transition edges to `spec.md`, including `PROCESSING`.
7. **Close D9 (research.md)** — author the technology-research pass; it directly feeds the D1 fix. Retain the design.md fold-in.
8. **Close D4, D3, D7, D8, D11** — mechanical edits: strengthen AC-FCFS-002's verification method; fix the `REQ-FCFS-005/006` token at `acceptance.md:103` and expand §D.2 to per-REQ rows; promote edge cases 1/4/5 to ACs; change `Where` → `While` at `spec.md:71,113`.

Re-audit scope: on resubmission, iteration 2 may be scoped to this enumerated defect delta (D1-D12) rather than a from-scratch full re-audit, per the Retry Loop Contract. Verdict authority remains with this agent.

---

Report generated by `plan-auditor` (iteration 1). Audit input: `spec.md` (203 L), `plan.md` (267 L), `acceptance.md` (301 L), `progress.md` (37 L). `design.md` / `research.md` absent (D9).
