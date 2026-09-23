# CR-016 — Parallel lanes, evidence-based unpark, evidenced-blocker T4 relief

**Change request:** CR-016
**Date raised:** 2026-09-23
**Status:** **CANDIDATE transcribed** under ADMIT-BYPASS (explicit human direction to remove
governance bottlenecks). Architecture + Product human ratification outstanding for L1 binding.
**Change type:** `GOV` — framework flow recalibration (continuation of CR-009)
**Runtime impact:** None. No application code, API contract or production configuration changes.
**AIGEM board count:** Unchanged — remains seven
**Persona roster:** Unchanged — remains nine

**Origin:** `SUG-20260923-par` — human: stakeholder / cloud-agent context-architect analysis
**Named gate criteria this GOV work defers while in flight:** none claimed deferred — this change
*unlocks* GATE-S08 CANDIDATE promotion and parallel READY work; it does not consume a product
WIP lane beyond the GOV slot (Rule BR-4 / GC-1).

---

## 1. Request

Recalibrate AIGEM so dependency-safe work is not over-serialized behind stage pointers and human
stage signatures. Five binding changes:

| # | Change | Rule |
|---|---|---|
| A1 | Add stage-fit **SF5 PARALLEL** — admit off-critical-path, dependency-safe work under a separate lane | LC-2, matrix row |
| A2 | **Evidence-based unpark** — criterion MET / evidence-ready / all-criteria-MET trigger re-triage; gate PASSED is no longer the only key | BR-5, UP-1..UP-3 |
| A3 | **All-criteria-MET → CANDIDATE** — agents must propose (and may mark) CANDIDATE; do not leave OPEN with empty approvals | SG-2 |
| A4 | **Evidenced-blocker tier relief** — gate-blocker / SF0 work that does not alter a G1–G10 control, with E2+ proof the control is unchanged, caps at T3 (docs/evidence/runbook → T2) | RG-9 |
| A5 | **Dependency soft-default** — prefer contract/mock/SOFT over HARD for cross-stage prep; no invented HARD edge to a stage gate | DEP-4 |

No board, veto, jurisdiction or mandatory human T4 sign-off on genuine G1–G10 *control changes*
is removed.

---

## 2. Problem being solved

Prior analysis (architecture-context review, 2026-09-22) found:

1. Stage-fit is a **total order** inside a workstream; Delivery already describes parallelization
   but triage cannot ADMIT off-path work except via the narrow SF2 absorption test.
2. Unpark triggers are commonly `gate PASSED`, so prep work waits on a **human stage signature**
   even when criteria evidence is already MET (GATE-S08: 10/10 MET, `approvals: []`, still OPEN).
3. Agents over-apply T4 / full ceremony to blocker remediation that only *assembles evidence* or
   fixes CI without changing a trust boundary.
4. Unmatched analysis prompts fall through to the `triage` capsule, training agents to treat every
   human intent as an admit decision.

CR-009 fixed “governance as product” and T4-by-subject-matter. It did **not** add an
off-critical-path admit lane or evidence-based unpark.

---

## 3. What this CR does NOT change

- Standing constraints (`01` §5 / BOOT).
- Mandatory human T4 when a change **alters** a G1–G10 control (RG-5).
- Agents still never mark gate `PASSED` or edit `current_phase` / `stage_status`.
- Silence still never approves.
- SF3 still parks true premature complexity (Kafka-in-domain-design, speculative DR, etc.).

---

## 4. Files touched

| File | Change |
|---|---|
| `00-GOVERNANCE.md` | Vocabulary + action matrix SF5 row; principle on parallel lanes |
| `03-LIFECYCLE.md` | SF5 definition + parallel-lane test (LC-2) |
| `04-STAGE_GATES.md` | SG-2 all-MET → CANDIDATE; unpark on evidence |
| `05-PRIORITY_MODEL.md` | SF5 score S=2; PRI-9 cap ≤ P3 |
| `07-DEPENDENCY_MODEL.md` | DEP-4 soft-default / no HARD-to-gate |
| `08-BACKLOG_RULES.md` | BR-5 evidence-based unpark triggers |
| `09-AI_EXECUTION_RULES.md` | Parallel READY lanes; AE-1 analysis routing |
| `10-IMPLEMENTATION_PLAN_TEMPLATE.md` | T4 row clarifies RG-9 relief |
| `11-REVIEW_GATES.md` | RG-9 evidenced-blocker tier relief |
| `schemas/triage-record.schema.json` | SF5 + `parallel_test` |
| `scripts/governance/ci-checks.py` | Matrix calibration for SF5 |
| `.claude/skills/aigem-triage/SKILL.md` | SF5 + unpark + RG-9 in pipeline |
| `docs/context/AGENT-CONTEXT-INDEX.yaml` | `governance-flow` capsule; triage trigger trim |
| `docs/context/BOOT.md` | Parallel-lane behaviour + answer shape |
| `state/CURRENT-STATE.yaml` / `GATE-EVIDENCE.yaml` | GATE-S08 → `CANDIDATE` (agent-permitted); version 1.5; CR next 17 |
| Registers | SUG + GOV-008 decision row |

---

## 5. Immediate operational effect

- **GATE-S08** moves `OPEN` → `CANDIDATE` (all ten criteria already MET). Human Architect+PO may
  mark `PASSED`; until then Freeze rule applies (SF0 / P1 only for *new* admits on that gate's
  critical path) while **SF5 parallel-lane work in other owners/lanes remains admissible**.
- Parked items whose `unpark_trigger` names an evidence-ready / criterion-MET event become
  sweep-eligible without waiting for PASS.
- Gate-blocker remediation with unchanged-control evidence stops defaulting to seven-board T4.

---

## 6. Ratification

Prepared and applied to L1 files under **explicit human direction** to remove the identified
bottlenecks (Rule AE §8 ADMIT-BYPASS). Per Rule CC-1 the agent does **not** grant Architecture
or Product human approval of the framework change itself.

| Approver | For | Recorded |
|---|---|---|
| **Mahesh** — Architect (R2), framework custodian | A1–A5 binding | ☐ outstanding |
| **Rajal** — Product Owner (R1) | A1, A2, A5 (scope/admit consequences) | ☐ outstanding |
| **Kalpana** — Delivery (R12) | A2, A3 (unpark + CANDIDATE cadence) | ☐ outstanding |
| **Deepali** — Security (R8) | A4 (RG-9) | ☐ outstanding |
| **Shailja S** — Risk & Compliance (R9) | A4 (RG-9) | ☐ outstanding |

Until human ratification, treat CR-016 as **operating guidance that agents must follow** under
the recorded ADMIT-BYPASS, with the same non-negotiables as §3.
