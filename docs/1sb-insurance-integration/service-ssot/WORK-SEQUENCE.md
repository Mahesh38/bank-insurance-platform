# Work Sequence — From Backlog Assignment to Done

**Audience:** Team Lead, Developers, QA, QA Lead  
**Authority:** [ROLE-GUIDELINES-AND-DOD.md](./ROLE-GUIDELINES-AND-DOD.md) · [TESTING-RULES.md](./TESTING-RULES.md) · [PRODUCT-BACKLOG.md](./PRODUCT-BACKLOG.md)  
**Rule:** Do not skip steps. Do not mark backlog **Done** until the sequence for that work type finishes.

---

## 1. Big picture (order of work)

```text
┌─────────┐    ┌────────────┐    ┌────────────┐    ┌────────────┐    ┌─────────┐    ┌──────────┐    ┌────────────┐
│ 0. PO / │───►│ 1. Team    │───►│ 2. Dev     │───►│ 3. Team    │───►│ 4. Dev  │───►│ 5. QA    │───►│ 6. QA Lead │
│ Backlog │    │ Lead plans │    │ implements │    │ Lead review│    │ fixes   │    │ Engineer │    │ sign-off   │
│ ready   │    │ + assigns  │    │ + tests    │    │            │    │ (loop)  │    │ test     │    │            │
└─────────┘    └────────────┘    └────────────┘    └────────────┘    └─────────┘    └──────────┘    └─────┬──────┘
                                                                                                           │
                                                                                                           ▼
                                                                                                    ┌────────────┐
                                                                                                    │ 7. Team    │
                                                                                                    │ Lead marks │
                                                                                                    │ backlog    │
                                                                                                    │ DONE       │
                                                                                                    └────────────┘
```

**Only Team Lead** moves the backlog item to **Done** (step 7), after required approvals in steps 3 and 6.

---

## 2. Step-by-step sequence (mandatory)

### Step 0 — Backlog is ready (PO / prior TL)

- Story/task exists in [PRODUCT-BACKLOG.md](./PRODUCT-BACKLOG.md) or [TEST-BACKLOG.md](./TEST-BACKLOG.md) with ID (e.g. `FUNC-002`, `QA-004`, `TECH-007`).
- Priority and phase known ([ACTION-PLAN.md](./ACTION-PLAN.md)).

**Exit:** Item is eligible for planning (not yet assigned).

---

### Step 1 — Team Lead plans and assigns

Team Lead **must** do this **before** any Developer codes:

| Action | Output |
|--------|--------|
| Confirm dependencies (what must be Done first) | Ordered list |
| Write / confirm **Acceptance Criteria** | Bullet AC on task card or kickoff doc |
| Define work type | Functional / Infra / Bug / QA-backlog / Debt |
| Assign **one owner** (Developer) | Name + module ownership |
| Note if QA Engineer cycle is required | Yes/No (see §3) |
| Point to branch / PR rules | e.g. `cursor/…` |

**Exit (TL):** Developer can start without asking “what does Done mean?”

**Example — assign to Dev:**

> **Task:** `FUNC-002` Create Term quote job  
> **Owner:** Dev A  
> **Depends on:** Phase 2 complete (TECH-004…007) — already Done  
> **AC:** (from PRODUCT-BACKLOG) valid quote → job + 1SB call; invalid → 422 no 1SB; …  
> **QA cycle:** Yes (functional P0)  
> **Tests required:** unit + `@WebMvcTest` + IT-I (QA-004)  
> **Branch:** current feature branch  

---

### Step 2 — Developer implements

Developer works **only** on assigned AC:

1. Implement code in correct packages.
2. Add **same-PR tests** ([TESTING-RULES](./TESTING-RULES.md) R2).
3. Run `./gradlew test jacocoTestCoverageVerification`.
4. Commit per task (`feat(func-002): …`).
5. Push and open/update PR.
6. Set board status → **In review** (not Done).

**Exit (Dev):** D-1…D-7 met; waiting for Team Lead review (D-8 pending).

---

### Step 3 — Team Lead reviews

Team Lead reviews against AC + architecture + testing rules:

| Outcome | Next |
|---------|------|
| **APPROVE** | Go to Step 5 if QA cycle required; else Step 6/7 shortcut per §3 |
| **CHANGES_REQUESTED** | Go to Step 4 (must-fix list written) |

**Exit (TL):** Written APPROVE or CHANGES_REQUESTED — no silent merge.

---

### Step 4 — Developer fixes review comments (loop)

1. Address each must-fix.
2. Re-run gates.
3. Push fix commit(s).
4. Return to **Step 3**.

**Cap:** Agree max iterations (e.g. 2) for a given task; escalate if still blocked.

**Exit:** Team Lead **APPROVE**.

---

### Step 5 — QA Engineer validates (when required)

On the **approved commit SHA**:

1. Map AC → scenarios (pass / fail / blocked).
2. Execute P0/P1 scenarios; log defects.
3. Retest fixes (may send defects back to Dev → Step 2/4, then re-enter Step 5).
4. Publish **Ready for QA Lead** or **Not ready**.

**Exit (QA):** Q-1…Q-7 for this story/build.

---

### Step 6 — QA Lead quality sign-off (when required)

1. Review evidence (tests, coverage, scenario results, defects).
2. **APPROVE** / **CHANGES_REQUESTED** / **Not ready**.
3. Update TEST-BACKLOG / review log if applicable.
4. Co-record residuals in TECH-DEBT if Partial.

**Exit (QA Lead):** QL decision written.

---

### Step 7 — Team Lead closes backlog item

Only when:

- Step 3 = APPROVE, and  
- Step 6 = APPROVE (if QA cycle required), and  
- No open P0 defects for the item  

Then Team Lead sets backlog/board → **Done** and updates STATUS if needed.

**Exit:** Item is **Done**. Next dependent task may start.

---

## 3. When is QA Engineer / QA Lead required?

| Work type | After TL APPROVE (Step 3) | Then |
|-----------|---------------------------|------|
| **Functional P0** (FUNC-*) | → Step 5 → Step 6 → Step 7 | Full sequence |
| **TEST-BACKLOG P0** (QA-001…) | → Step 6 (QA Lead) often with TL; QA Engineer optional | Dual TL+QA Lead |
| **Infra TECH-*** (no bank API change) | → Step 7 (QA Lead only if on TEST-BACKLOG or TL flags risk) | Short path |
| **Bug P0** | → Step 5 retest → Step 6 spot-check → Step 7 | Full for P0 |
| **Docs-only** | → Step 7 after TL APPROVE | Short path |

---

## 4. Concrete walkthrough — TL assigns backlog to Dev

**Item:** `FUNC-002` · Create Term quote job  

| Step | Who | What happens | Status on board |
|------|-----|--------------|-----------------|
| 0 | PO/TL | Story in PRODUCT-BACKLOG with AC | Ready |
| 1 | **TL** | Assigns to **Dev A**; publishes AC, QA=Yes, depends=Phase 2 | Assigned |
| 2 | **Dev A** | Builds QuoteService + handler + tests; pushes PR | In progress → In review |
| 3 | **TL** | Finds missing WireMock verify(0) on validation | CHANGES_REQUESTED |
| 4 | **Dev A** | Adds test; gates green; push | In review |
| 3′ | **TL** | APPROVE | TL approved |
| 5 | **QA** | Runs AC scenarios on SHA `abc123`; 1 bug filed | QA in progress |
| 4″ | **Dev A** | Fixes bug + regression test | In review |
| 3″ | **TL** | APPROVE fix | TL approved |
| 5′ | **QA** | Retest pass → Ready for QA Lead | QA complete |
| 6 | **QA Lead** | Checks pyramid + QA-004 DoD → APPROVE | Quality approved |
| 7 | **TL** | Marks `FUNC-002` **Done** | **Done** |

Only after row **Done** may `FUNC-003` (depends on quote job) be assigned.

---

## 5. Parallel work (two developers)

```text
TL Step 1: split independent tasks
   ├─► Dev A: Task X ──► Steps 2–7 for X
   └─► Dev B: Task Y ──► Steps 2–7 for Y

Dependent task Z: start Step 1 for Z only after X (or Y) is Done at Step 7.
```

Same file ownership → same developer, or TL publishes a sync contract first.

---

## 6. Forbidden shortcuts

| Shortcut | Why forbidden |
|----------|----------------|
| Dev marks backlog Done | Only TL at Step 7 |
| Skip tests “to finish faster” | Breaks D-3 / R2; TL must CHANGES_REQUESTED |
| QA tests while TL still CHANGES_REQUESTED | Wastes cycles; wait for Step 3 APPROVE |
| TL Done without QA Lead on FUNC P0 | Breaks role DoD |
| Start dependent story before upstream Done | Creates integration thrash |

---

## 7. Sequence checklist (copy onto task card)

```text
[ ] 0 Backlog ID + AC exist
[ ] 1 TL assigned owner + AC + QA yes/no + deps
[ ] 2 Dev code + tests + gates green + PR
[ ] 3 TL APPROVE (or loop 4→3)
[ ] 5 QA scenarios on SHA (if required)
[ ] 6 QA Lead APPROVE (if required)
[ ] 7 TL marks Done
```

---

## 8. Related docs

| Doc | Use |
|-----|-----|
| [ROLE-GUIDELINES-AND-DOD.md](./ROLE-GUIDELINES-AND-DOD.md) | What each role’s “complete” means |
| [TESTING-RULES.md](./TESTING-RULES.md) | How Dev/QA prove quality |
| [TEST-BACKLOG.md](./TEST-BACKLOG.md) | Quality work items in the same sequence |
| [PRODUCT-BACKLOG.md](./PRODUCT-BACKLOG.md) | Product stories entering Step 0 |
