# AIGEM — AI Governance & Execution Model

**Version:** 1.0
**Status:** Binding for all AI agents and human contributors working in this repository
**Custodian:** Platform / Solution Architect
**Source blueprint:** *AIGEM Executive Blueprint v0.1*

---

## 1. What this is

A decision framework that sits between **an idea** and **a code change**.

Every input — a requirement, a bug, a review comment, an AI suggestion, a scan finding — is
pushed through one deterministic pipeline before anyone writes code. The pipeline answers seven
questions in a fixed order and produces a written record:

1. **Where are we?** (lifecycle stage)
2. **Does this belong at this stage?** (stage fit)
3. **Is it inside approved scope?** (scope fit)
4. **How necessary is it?** (MUST / SHOULD / COULD / NOT-NOW / REJECT)
5. **What kind of work is it?** (feature / bug / NFR / debt / risk / compliance / …)
6. **How urgent is it, *today*?** (P1–P5, recomputed at the stage it actually lands in)
7. **What must happen first?** (dependencies, ordering)

Only then: work breakdown → implementation plan → seven-board review → approval gate →
execution → validation → register update.

**The purpose is not to slow work down. It is to stop good ideas arriving at the wrong time
from destroying the current objective — and to stop them being lost.**

---

## 2. The one rule that matters most

> **A suggestion is never implemented at the moment it is made.**
> It is triaged, recorded, and scheduled. Parked is not deleted. Rejected is not forgotten.

Corollary: an agent in the middle of `FUNC-011` that notices a missing index, a better
abstraction, or an unhandled edge case does **not** fix it inline. It writes a `SUG-####`
entry and finishes `FUNC-011`.

The only exceptions are the **P1 interrupt classes** in [09-AI_EXECUTION_RULES.md](./09-AI_EXECUTION_RULES.md#4-the-interrupt-rule).

---

## 3. Three layers

| Layer | Owns | Lives in | Portable? |
|-------|------|----------|-----------|
| **L1 — Framework** | Generic decision machinery: lifecycle model, priority maths, dependency algebra, review boards, drift control | `03`, `05`–`17`, `19`, `templates/`, `schemas/` | Yes — copy to any repo unchanged |
| **L2 — Organization** | Company standards: SDLC, security baseline, architecture principles, CI/CD, compliance regime | [`ORG-STANDARDS.md`](./ORG-STANDARDS.md) | Per organization |
| **L3 — Project** | This repo's live truth: where we are, what is in scope, what is parked, what is decided | `01`, `02`, `04`, `state/`, `registers/` | No — rewritten per repo |

An agent that reads only L1 will make well-formed decisions about the wrong project. **Always
resolve L3 first.** See [01-CURRENT_STATE.md](./01-CURRENT_STATE.md).

---

## 4. File index

### Start here

| File | Purpose |
|------|---------|
| **[RUNBOOK.md](./RUNBOOK.md)** | **The operating manual — who does what, how often, and what breaks if they don't.** Role cards, cadences, the staleness matrix, and the AI agent's knowledge contract. Most people need only their role card. |

### Decision pipeline (read in this order)

| # | File | Pipeline step |
|---|------|---------------|
| 00 | [00-GOVERNANCE.md](./00-GOVERNANCE.md) | Charter, principles, ownership, precedence rules |
| 01 | [01-CURRENT_STATE.md](./01-CURRENT_STATE.md) | **Step 1** — Context resolution (where are we now) |
| 02 | [02-PROJECT_SCOPE.md](./02-PROJECT_SCOPE.md) | **Step 3** — Scope validation |
| 03 | [03-LIFECYCLE.md](./03-LIFECYCLE.md) | **Step 2** — Lifecycle validation, stage-fit codes SF0–SF4 |
| 04 | [04-STAGE_GATES.md](./04-STAGE_GATES.md) | Entry/exit criteria per stage |
| 05 | [05-PRIORITY_MODEL.md](./05-PRIORITY_MODEL.md) | **Step 6** — P1–P5 scoring, stage-relative priority |
| 06 | [06-WORK_CLASSIFICATION.md](./06-WORK_CLASSIFICATION.md) | **Step 5** — Work type + **Step 8** epic/story/task/spike |
| 07 | [07-DEPENDENCY_MODEL.md](./07-DEPENDENCY_MODEL.md) | **Step 7** — Dependency types + execution ordering |
| 08 | [08-BACKLOG_RULES.md](./08-BACKLOG_RULES.md) | Where triaged work goes and how it moves |
| 09 | [09-AI_EXECUTION_RULES.md](./09-AI_EXECUTION_RULES.md) | **The agent contract** — read this before acting |
| 10 | [10-IMPLEMENTATION_PLAN_TEMPLATE.md](./10-IMPLEMENTATION_PLAN_TEMPLATE.md) | **Step 9** — Plan structure and required fields |
| 11 | [11-REVIEW_GATES.md](./11-REVIEW_GATES.md) | **Step 10** — Seven review boards + approval gate |
| 12 | [12-DEFINITION_OF_READY.md](./12-DEFINITION_OF_READY.md) | When work may be picked up |
| 13 | [13-DEFINITION_OF_DONE.md](./13-DEFINITION_OF_DONE.md) | When work may be closed |
| 14 | [14-CHANGE_CONTROL.md](./14-CHANGE_CONTROL.md) | Changing scope, stage, or an approved plan |
| 15 | [15-TECH_DEBT_POLICY.md](./15-TECH_DEBT_POLICY.md) | Deliberate shortcuts, expiry, repayment |
| 16 | [16-DECISION_MODEL.md](./16-DECISION_MODEL.md) | **Step 4** — Necessity, evidence, confidence, anti-over-engineering |
| 17 | [17-DRIFT_CONTROL.md](./17-DRIFT_CONTROL.md) | Detecting deviation and getting back on task |
| 18 | [18-GOVERNANCE_METRICS.md](./18-GOVERNANCE_METRICS.md) | Is the model working? Measures and targets |
| 19 | [19-PORTING_GUIDE.md](./19-PORTING_GUIDE.md) | Installing AIGEM into another repository |
| — | [ORG-STANDARDS.md](./ORG-STANDARDS.md) | Layer 2 — organization-wide architecture, security, compliance and quality baselines |
| — | [RUNBOOK.md](./RUNBOOK.md) | Operating manual: roles, cadences, maintenance, ceremonies |

### Live project data (L3)

| File | Contents |
|------|----------|
| [state/CURRENT-STATE.yaml](./state/CURRENT-STATE.yaml) | Machine-readable current stage, objective, scope, gate |
| [registers/SUGGESTION-REGISTER.md](./registers/SUGGESTION-REGISTER.md) | Every suggestion ever triaged, with verdict |
| [registers/PARKED-BACKLOG.md](./registers/PARKED-BACKLOG.md) | Deferred work, with the stage that unparks it |
| [registers/DECISION-REGISTER.md](./registers/DECISION-REGISTER.md) | Decisions + ADR index |
| [registers/DEPENDENCY-REGISTER.md](./registers/DEPENDENCY-REGISTER.md) | Edges of the dependency graph |
| [registers/RISK-REGISTER.md](./registers/RISK-REGISTER.md) | Open risks, owners, triggers |
| [registers/ASSUMPTION-REGISTER.md](./registers/ASSUMPTION-REGISTER.md) | Unvalidated assumptions and their expiry |

### Working artefacts

| File | Use |
|------|-----|
| [templates/TRIAGE-RECORD.md](./templates/TRIAGE-RECORD.md) | Emitted for **every** new input — the pipeline's output |
| [templates/WORK-ITEM.md](./templates/WORK-ITEM.md) | Story / task / bug / spike card |
| [templates/EPIC.md](./templates/EPIC.md) | Epic with story index and completion definition |
| [templates/IMPLEMENTATION-PLAN.md](./templates/IMPLEMENTATION-PLAN.md) | Plan submitted to the review board |
| [templates/REVIEW-VERDICT.md](./templates/REVIEW-VERDICT.md) | One reviewer's verdict |
| [templates/ADR.md](./templates/ADR.md) | Architecture decision record |
| [schemas/](./schemas/) | JSON Schema for each artefact — validate before accepting |

---

## 5. The pipeline

```text
                         NEW INPUT
                            │
           Requirement / Bug / Suggestion / Finding
                            │
                            ▼
                  1. CONTEXT RESOLUTION            → 01, state/CURRENT-STATE.yaml
                     Where are we now?
                            │
                            ▼
                 2. LIFECYCLE VALIDATION           → 03  (SF0…SF4)
              Is this relevant at this stage?
                            │
                            ▼
                    3. SCOPE VALIDATION            → 02  (SC0…SC4)
              Is this inside approved scope?
                            │
                            ▼
                 4. NECESSITY ASSESSMENT           → 16
        MUST / SHOULD / COULD / NOT NOW / REJECT
                            │
                            ▼
                   5. WORK CLASSIFICATION          → 06
     Feature / Bug / NFR / Debt / Risk / Compliance...
                            │
                            ▼
                   6. PRIORITY SCORING             → 05
                     P1 → P2 → P3 → P4 → P5
                            │
                            ▼
                 7. DEPENDENCY ANALYSIS            → 07
                What must happen before this?
                            │
                            ▼
                8. WORK BREAKDOWN ANALYSIS         → 06 §5
          Epic? Story? Task? Spike? Bug? ADR?
                            │
                            ▼
                 9. IMPLEMENTATION PLAN            → 10
                            │
                            ▼
                  10. MULTI-AGENT REVIEW           → 11
       Architecture ─ Product ─ Technical ─ Security
             QA ─ Risk/Compliance ─ Operations
                            │
                            ▼
                     APPROVAL GATE
                      /           \
                 REJECTED        APPROVED
                    │                │
                 Rework              ▼
                              BACKLOG READY        → 12
                                    │
                                    ▼
                            DEPENDENCY ORDERING    → 07 §5
                                    │
                                    ▼
                              IMPLEMENTATION       → 09, 17
                                    │
                                    ▼
                                VALIDATION         → 13
                                    │
                                    ▼
                              STAGE REGISTER       → registers/
```

**Early exits are normal and cheap.** Most inputs terminate at step 2 or 3 with `PARK` or
`REJECT` and a one-screen triage record. The full ten steps run only for work that will
actually be built now.

---

## 6. Sixty-second agent loop

An agent handed *anything* runs this:

```text
1. Read  state/CURRENT-STATE.yaml           → stage, objective, in/out of scope
2. Classify stage fit    (03 §3)            → SF0 | SF1 | SF2 | SF3 | SF4
3. Classify scope fit    (02 §4)            → SC0 | SC1 | SC2 | SC3 | SC4
4. Assess necessity      (16 §2)            → MUST | SHOULD | COULD | NOT-NOW | REJECT
5. Apply the action matrix (00 §6)          → ADMIT | PARK | REJECT | ESCALATE
6. If ADMIT   → classify (06), score (05), map dependencies (07), break down (06 §5),
                plan (10), review (11), then build.
   If PARK    → registers/PARKED-BACKLOG.md with target stage + unpark trigger. Stop.
   If REJECT  → registers/SUGGESTION-REGISTER.md with reason. Stop.
   If ESCALATE→ 14-CHANGE_CONTROL.md. Stop and ask a human.
7. Return to the work item you were on before this input arrived.
```

Step 7 is not optional. It is the whole point.

---

## 7. Precedence

When documents conflict:

```text
Human instruction in the current conversation
  > 14-CHANGE_CONTROL approved change request
    > L3 project files (01, 02, 04, state/, registers/)
      > L2 organization standards
        > L1 framework files
          > repository convention / existing code
            > agent judgement
```

An agent may never resolve a conflict silently. Record it in the triage record's
`conflicts` field and follow the order above.

---

## 8. What "mature" means here

This model is deliberately built to score against the blueprint's 9/10 maturity target:

| Capability | Where |
|------------|-------|
| Decision confidence | [16 §5](./16-DECISION_MODEL.md#5-confidence-levels) |
| Evidence & assumptions | [16 §4](./16-DECISION_MODEL.md#4-evidence-standard), [registers/ASSUMPTION-REGISTER.md](./registers/ASSUMPTION-REGISTER.md) |
| Traceability matrix | [08 §6](./08-BACKLOG_RULES.md#6-traceability) |
| Architecture principles engine | [11 §4](./11-REVIEW_GATES.md#4-board-1--architecture) |
| Decision log | [registers/DECISION-REGISTER.md](./registers/DECISION-REGISTER.md) |
| Cost vs value analysis | [05 §4](./05-PRIORITY_MODEL.md#4-the-scoring-model) |
| Project health dashboard | [18](./18-GOVERNANCE_METRICS.md) |
| Anti-over-engineering rules | [16 §6](./16-DECISION_MODEL.md#6-anti-over-engineering-tests) |
| Memory categorization | [08 §2](./08-BACKLOG_RULES.md#2-the-six-buckets) |
| Revalidation triggers | [16 §7](./16-DECISION_MODEL.md#7-revalidation-triggers) |
| Success metrics | [18 §2](./18-GOVERNANCE_METRICS.md#2-the-metrics) |

---

## 9. Guiding principle

> The AI's responsibility is not to build the most sophisticated system. Its responsibility is
> to build **the correct system for the approved scope, at the correct lifecycle stage, in the
> correct dependency order**, while capturing future improvements without derailing the current
> objective.
