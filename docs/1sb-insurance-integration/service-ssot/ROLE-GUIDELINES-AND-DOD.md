# Team Role Guidelines & Definition of Done

**Audience:** Team Lead (Tech Lead), Developers, QA Engineers, QA Lead  
**Status:** Binding for work on this platform  
**Branch:** `cursor/phase1-foundations-c259` onward  
**Related:** [TESTING-RULES.md](./TESTING-RULES.md) · [QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md) · [PRODUCT-BACKLOG.md](./PRODUCT-BACKLOG.md) · [ACTION-PLAN.md](./ACTION-PLAN.md) · [TECH-DEBT.md](./TECH-DEBT.md)

---

## 1. Strategic intent

Everyone optimizes for the same outcomes:

1. **Bank apps never call 1SB or the DB directly** — topology and hex boundaries stay intact.
2. **Replaceable middleware** — 1SB stays behind ports/adapters; persistence stays on `bank-persistence-service`.
3. **Evidence over opinion** — “Done” means automated proof + review, not “code pushed.”
4. **Small, reviewable batches** — one task / one feature commit where practical; clear AC before coding.
5. **Quality is shared** — Developers own unit/slice tests; QA owns scenarios and sign-off; Leads own gates and debt.

**Golden rule:** A task is not complete when *you* finished your part — it is complete when the **next role can act without ambiguity** and the **exit criteria for that role’s DoD** are met.

---

## 2. Role map (one sentence each)

| Role | Exists to… |
|------|------------|
| **Team Lead (Tech Lead)** | Turn backlog into safe, ordered work; protect architecture; approve technical completion. |
| **Developer** | Deliver working, tested, reviewable increments that match AC and architecture. |
| **QA Engineer** | Prove behaviour against AC with scenarios, exploratory depth, and defect clarity. |
| **QA Lead** | Define test strategy/rules/gates; judge quality readiness; approve quality completion. |

---

## 3. Team Lead (Tech Lead) — guidelines

### 3.1 Responsibilities

- Break epics/stories into **small tasks** with **explicit acceptance criteria** before assignment.
- Distribute work to avoid file/ownership conflicts; document sync contracts when parallelizing.
- Review code against **architecture**, SOLID/DRY/KISS, security (no secrets in git), and [TESTING-RULES.md](./TESTING-RULES.md).
- Maintain / enforce [TECH-DEBT.md](./TECH-DEBT.md) — open, close, assign, set expiry.
- Decide **APPROVE** vs **CHANGES_REQUESTED**; never silent-merge with open must-fix comments.
- Gate phase exits ([ACTION-PLAN.md](./ACTION-PLAN.md)) with a written review (e.g. phase-2 TL-REVIEW).

### 3.2 What “task complete” means for Team Lead

A **task or sprint increment is complete for the Team Lead** only when all of the following are true:

| # | Criterion |
|---|-----------|
| TL-1 | Task had clear AC / DoD before developers started (or AC was amended in writing mid-flight). |
| TL-2 | Delivered code matches architecture boundaries (no illegal deps, no Flyway/JPA in integration service, 1SB only in `adapter.onesb.*`). |
| TL-3 | Review recorded: **APPROVE** or all **CHANGES_REQUESTED** items re-reviewed and closed. |
| TL-4 | Required tests exist and `./gradlew test jacocoTestCoverageVerification` (or agreed CI equivalent) is green. |
| TL-5 | New intentional shortcuts logged in TECH-DEBT with owner + severity (+ expiry if waiver). |
| TL-6 | STATUS / kickoff / review docs updated so another lead can continue without tribal knowledge. |
| TL-7 | Handoff to QA (if functional story) or explicit “infra-only / no QA cycle” note is recorded. |

**Not complete:** “Looks fine in the diff” without running gates, or approving while must-fix comments remain open.

### 3.3 Team Lead anti-patterns

- Assigning two developers the same ownership zone without a sync contract.
- Expanding Phase N scope into Phase N+1 mid-review without backlog update.
- Closing tech debt by renaming status without residual criteria.

---

## 4. Developer — guidelines

### 4.1 Responsibilities

- Implement **only** the assigned task AC; flag scope creep to Team Lead before coding it.
- Follow architecture, package ownership, and naming conventions.
- Write **same-PR tests** per [TESTING-RULES.md](./TESTING-RULES.md) R2 (unit + slice/WireMock as required).
- Prefer one commit per task/feature with a clear message (`feat(…)`, `test(…)`, `fix(…)`).
- Push for review; respond to TL and QA Lead comments until **both** approve when dual review is required.
- Never commit real secrets or live PII; use placeholders and synthetic fixtures.

### 4.2 What “task complete” means for Developer

A **developer task is complete** only when:

| # | Criterion |
|---|-----------|
| D-1 | All AC for the task are implemented (or explicitly deferred with Team Lead agreement + TECH-DEBT id). |
| D-2 | Code lives in the correct module/package; ArchUnit / topology rules not violated. |
| D-3 | Tests for new/changed behaviour are in the **same change set** (R2); names/tags follow R3. |
| D-4 | Local proof: `./gradlew test` green; after QA-001, coverage verification green for affected modules. |
| D-5 | Assertions meet R5 (error **codes**, not status alone; no assertNotNull-only). |
| D-6 | Commit(s) pushed; PR/branch description lists what was done and how to verify. |
| D-7 | STATUS or task board updated (task → Done *pending review* until leads approve). |
| D-8 | **Review loop closed:** Team Lead **APPROVE** (and QA Lead **APPROVE** when the item is on TEST-BACKLOG / P0 quality or functional story). |

**Developer “coding finished” ≠ “task complete.”** Coding finished is an internal checkpoint. Task complete requires D-1…D-8.

### 4.3 Developer anti-patterns

- Shipping adapters without WireMock/MockRest when outbound HTTP was added.
- “Tests later” on the same story.
- Fixing review comments without re-running the failing gate.
- Editing another developer’s ownership files without coordination.

---

## 5. QA Engineer — guidelines

### 5.1 Responsibilities

- Derive **test scenarios** from story AC, architecture NFRs, and [TEST-BACKLOG.md](./TEST-BACKLOG.md).
- Execute / automate slice, integration, and (when authorized) E2E/sandbox checks.
- File defects with: expected vs actual, environment, IDs (`jobId`, correlation), steps, severity.
- Retest fixes; confirm regression around the change.
- Keep fixtures synthetic; never store real customer PII in test artefacts.
- Escalate design/testability gaps to QA Lead (not only to developers).

### 5.2 What “task complete” means for QA Engineer

A **QA task (test cycle on a story/build) is complete** only when:

| # | Criterion |
|---|-----------|
| Q-1 | Scenario pack mapped to AC (pass/fail/blocked recorded per scenario). |
| Q-2 | All **P0/P1** scenarios for the story executed on the agreed build/commit SHA. |
| Q-3 | Failures logged as defects with severity; no “fails locally, skipped silently.” |
| Q-4 | Retest of fixed defects done on a new SHA; results updated. |
| Q-5 | Exploratory notes (if any) attached; blockers called out to QA Lead + Team Lead. |
| Q-6 | Recommendation issued: **Ready for QA Lead sign-off** / **Not ready** (with reasons). |
| Q-7 | Automated checks owned by QA for that story are green or waived via TECH-DEBT with QA Lead approval. |

**Not complete:** “Developers said tests pass” without QA scenario evidence, or signing off while open P0 defects remain.

### 5.3 QA Engineer anti-patterns

- Only happy-path clicking/API calls when AC lists error/timeout/idempotency cases.
- Closing a bug without retest on the fix commit.
- Using production-like PII in fixtures.

---

## 6. QA Lead — guidelines

### 6.1 Responsibilities

- Own [QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md), [TESTING-RULES.md](./TESTING-RULES.md), [TEST-BACKLOG.md](./TEST-BACKLOG.md), [COVERAGE.md](./COVERAGE.md).
- Set coverage floors, pyramid expectations, and what Automation/AI may vs must not do.
- Review quality deliverables (test design, coverage gates, IT templates) with Team Lead on baseline/P0 items.
- Approve **quality completion** and **phase/story readiness** from a test perspective.
- Prioritize TEST-BACKLOG; refuse “Done” on stories that skip QA-004-style gates when applicable.
- Co-approve coverage waivers (R7) with Team Lead; require expiry.

### 6.2 What “task complete” means for QA Lead

A **QA Lead item (strategy, gate, backlog story, or sign-off) is complete** only when:

| # | Criterion |
|---|-----------|
| QL-1 | Decision is written (APPROVE / CHANGES_REQUESTED / Not ready) with evidence references (tests, reports, SHA). |
| QL-2 | Rules/strategy/backlog updated if the decision changes process (no oral-only policy). |
| QL-3 | For backlog items: DoD of that item met (e.g. JaCoCo wired, IT template exists) **and** residual debt recorded if Partial. |
| QL-4 | Dual-review items: Team Lead stance known; conflicts resolved in writing ([QA-REVIEW-LOG.md](./QA-REVIEW-LOG.md) or equivalent). |
| QL-5 | For **release / phase quality gate**: P0 TEST-BACKLOG items required for that phase are Done or explicitly deferred with risk accepted by Team Lead + PO. |
| QL-6 | Stakeholders can answer: “What is still untested?” from TEST-BACKLOG + TECH-DEBT without asking you. |

**Not complete:** Verbal “LGTM” with no log, or approving while R7 coverage verification is red without a dated waiver.

### 6.3 QA Lead anti-patterns

- Raising gates without a path for the team to meet them (no backlog/tasks).
- Approving Phase 3 functional exit without QA-004-equivalent evidence.
- Letting interim coverage floors expire silently.

---

## 7. Shared Definition of Done — by work type

Use this table so every role knows the **finish line** for common work types.

| Work type | Developer Done | Team Lead Done | QA Engineer Done | QA Lead Done |
|-----------|----------------|----------------|------------------|--------------|
| **Infra / tech task** (e.g. TECH-004) | D-1…D-8 (TL approve; QA Lead if on TEST-BACKLOG) | TL-1…TL-7 | Scenarios only if risk flagged | Approve if quality backlog item |
| **Functional story** (e.g. FUNC-002) | Code + unit/slice/IT per R9 | Architecture + AC review APPROVE | Full AC scenario pack + retest | Quality sign-off / Not ready |
| **Bug fix** | Fix + regression test | Review APPROVE | Retest + adjacent smoke | Spot-check P0 bugs |
| **Tech debt item** | Implementation + tests | Debt closed or Partial with expiry | As applicable | Co-approve waivers |
| **Phase exit** | All phase tasks D-8 | Written phase TL review | Phase regression evidence | Phase quality gate APPROVE |

---

## 8. Task lifecycle (mandatory flow)

```text
1. Team Lead      → AC + ownership + order published
2. Developer      → implement + same-PR tests + push
3. Team Lead      → review (architecture + rules)
4. Developer      → fix CHANGES_REQUESTED (loop ≤ agreed iterations)
5. QA Engineer    → scenarios on agreed SHA (functional / P0 quality)
6. QA Lead        → quality review / sign-off
7. Team Lead      → mark backlog/phase item complete only after required approvals
```

**Parallel rule:** Independent tasks may run in parallel; **dependent chains stay with one developer**. Dual review (TL + QA Lead) is required for TEST-BACKLOG P0 items and for functional P0 stories.

---

## 9. Escalation

| Situation | Who decides |
|-----------|-------------|
| AC ambiguous | Team Lead (clarify with PO if product) |
| Architecture conflict | Team Lead (SSOT/architecture wins) |
| Test rule vs delivery pressure | QA Lead + Team Lead co-decide; record waiver |
| Coverage gate failure | Developer fixes or TL+QA Lead waiver with expiry |
| Open P0 defect at “Done” request | QA Lead blocks; Team Lead replans |

---

## 10. One-line cheat sheet (print this)

| Role | “I’m done when…” |
|------|------------------|
| **Team Lead** | …I can **approve** the change against architecture + AC, gates are green, debt is logged, and the next role has a clear handoff. |
| **Developer** | …AC is met, tests shipped **with** the code, gates are green, and **leads have approved** the review. |
| **QA Engineer** | …every required scenario has a result on a known SHA, defects are retested, and I gave a clear Ready / Not ready recommendation. |
| **QA Lead** | …quality evidence matches the strategy/rules, backlog/gates are updated, and I recorded APPROVE or Not ready with residuals owned. |

---

## 11. Adoption

- Team Lead links this doc from sprint kickoffs and phase kickoffs.
- PR template / review checklist should cite **D-*** / **TL-*** as applicable.
- Conflicts with older notes: **this document + TESTING-RULES win** for process/DoD; PRODUCT-BACKLOG wins for product AC.
