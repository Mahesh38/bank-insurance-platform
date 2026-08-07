# 00 — Governance Charter

**Layer:** L1 (generic) with L3 hooks
**Status:** Binding
**Custodian:** Platform / Solution Architect

---

## 1. Problem this solves

AI agents are good at producing *plausible improvements* and bad at knowing *when an
improvement is welcome*. Left ungoverned, an agent asked to finish an aggregate lifecycle will
volunteer Kafka, a caching layer, a retry framework, and an observability stack — each
individually defensible, collectively fatal to the current objective.

The failure is not that the suggestions are wrong. It is that they are **unscheduled**.

AIGEM makes scheduling explicit and mechanical:

| Failure mode | Control |
|--------------|---------|
| Premature infrastructure | Stage-fit gate (SF0–SF4), [03](./03-LIFECYCLE.md) |
| Scope creep | Scope-fit gate (SC0–SC4), [02](./02-PROJECT_SCOPE.md) |
| Gold-plating | Necessity + anti-over-engineering tests, [16](./16-DECISION_MODEL.md) |
| Good ideas lost | Park registers, never delete, [08](./08-BACKLOG_RULES.md) |
| Work started out of order | Dependency ordering, [07](./07-DEPENDENCY_MODEL.md) |
| Unreviewed design | Seven-board approval gate, [11](./11-REVIEW_GATES.md) |
| Silent shortcuts | Tech debt policy with expiry, [15](./15-TECH_DEBT_POLICY.md) |
| Mid-task deviation | Drift detection + recovery, [17](./17-DRIFT_CONTROL.md) |

---

## 2. Principles

1. **Governance is separate from implementation.** This folder never contains product logic,
   and product docs never contain triage rules.
2. **Correct beats clever.** The target is the approved system, not the best conceivable system.
3. **Stage before merit.** A MUST-have at the wrong stage is a PARK, not a P1.
4. **Nothing is discarded.** Every input leaves a record: admitted, parked, or rejected with a
   reason.
5. **Written before built.** No implementation without a plan; no plan without a review verdict.
6. **Proportional ceremony.** A typo fix does not need seven approvals. Rigour scales with the
   risk tier ([11 §3](./11-REVIEW_GATES.md#3-proportionality--which-boards-are-mandatory)).
7. **Evidence over assertion.** "This is needed" without a named consumer, a failing test, a
   regulation, or a gate criterion is an opinion — score it as low confidence.
8. **One active objective.** An agent has exactly one work item in flight. Everything else is
   queued.
9. **Reversibility is a first-class input.** Cheap-to-change-later beats build-it-now.
10. **The register is the memory.** Agents forget between sessions; the registers do not.

---

## 3. Scope of authority

AIGEM governs **what work is admitted and in what order**. It does not govern:

- how code is written (that is architecture + coding standards);
- how tests are written ([TESTING-RULES.md](../1sb-insurance-integration/service-ssot/TESTING-RULES.md));
- who reviews human PRs (that is [ROLE-GUIDELINES-AND-DOD.md](../1sb-insurance-integration/service-ssot/ROLE-GUIDELINES-AND-DOD.md)).

Where AIGEM and an existing repository process overlap, AIGEM defers to the existing process
and adds only the triage layer in front of it. Concretely, in this repository:

| Existing artefact | AIGEM relationship |
|-------------------|--------------------|
| `service-ssot/PRODUCT-BACKLOG.md` | Destination for ADMITTED functional work. AIGEM decides *whether and when*; the backlog stays the story catalogue. |
| `service-ssot/ACTION-PLAN.md` | Source of truth for the phase list consumed by [03-LIFECYCLE.md](./03-LIFECYCLE.md). |
| `service-ssot/TECH-DEBT.md` | Destination for DEBT-classified items. [15](./15-TECH_DEBT_POLICY.md) adds expiry rules on top. |
| `service-ssot/WORK-SEQUENCE.md` | Runs *after* the AIGEM approval gate — TL assignment → Dev → TL review → QA → done. |
| `service-ssot/ROLE-GUIDELINES-AND-DOD.md` | Human role DoD. AIGEM's [13](./13-DEFINITION_OF_DONE.md) references it rather than replacing it. |
| `service-ssot/TEST-BACKLOG.md` | Destination for QA-classified items. |
| `docs/au-bank-insurance-platform/DECISION-LOG.md` | Upstream business decisions; AIGEM's decision register links to it. |

**AIGEM never duplicates a backlog.** It routes to the existing ones.

---

## 4. Roles

| Role | Accountable for | Board seat ([11](./11-REVIEW_GATES.md)) |
|------|-----------------|------------------------------------------|
| Platform / Solution Architect | Custodian of this framework; stage transitions; architecture verdicts | Architecture |
| Product Owner | Scope definition; necessity disputes; priority ties | Product |
| Business Analyst | Requirement clarity; acceptance criteria quality | Product (delegate) |
| Engineering / Tech Lead | Technical verdicts; work breakdown; debt ledger | Technical |
| QA Lead | Testability, coverage, validation evidence | QA |
| Security Architect | Security verdict (**veto**) | Security |
| Risk & Compliance | Regulatory verdict (**veto**) | Risk & Compliance |
| DevOps / SRE | Operability, deployability, rollback | Operations |
| Delivery Lead | Gate cadence; parked-item grooming; metrics | — |
| **AI agent** | Running the pipeline faithfully; producing records; **not** granting itself approvals | Executes review roles only where [11 §2](./11-REVIEW_GATES.md#2-who-may-sit-on-a-board) permits |

An AI agent may *simulate* a board to produce a draft verdict. A simulated verdict is marked
`reviewer_type: AGENT` and can never satisfy a mandatory human sign-off — see
[11 §2](./11-REVIEW_GATES.md#2-who-may-sit-on-a-board).

---

## 5. Vocabulary (used identically in every file and schema)

| Term | Meaning |
|------|---------|
| **Input** | Anything arriving that could become work: requirement, bug, suggestion, scan finding, review comment. |
| **Triage record** | The pipeline's output for one input. Always produced. `SUG-####`. |
| **Work item** | An admitted input that has an ID, a type, a priority and a home. |
| **ADMIT** | Work enters a backlog for the *current* stage. |
| **PARK** | Work is recorded against a *future* stage with an unpark trigger. |
| **REJECT** | Work will not be done; the reason is recorded permanently. |
| **ESCALATE** | The decision exceeds agent authority → [14-CHANGE_CONTROL.md](./14-CHANGE_CONTROL.md). |
| **Stage fit (SF)** | How the input relates to the current lifecycle stage. SF0–SF4. |
| **Scope fit (SC)** | How the input relates to approved business/technical scope. SC0–SC4. |
| **Necessity** | MUST / SHOULD / COULD / NOT-NOW / REJECT. |
| **Priority** | P1–P5, always stage-relative — see [05](./05-PRIORITY_MODEL.md). |
| **Gate** | A checkpoint with written exit criteria. Stage gates ([04](./04-STAGE_GATES.md)) and the approval gate ([11](./11-REVIEW_GATES.md)). |

---

## 6. The action matrix

This is the core routing table. Stage fit (rows) × necessity (columns). Scope fit is applied
first as a filter — see the note beneath.

| | **MUST** | **SHOULD** | **COULD** | **NOT-NOW** |
|---|---|---|---|---|
| **SF0** prerequisite | ADMIT · P1 | ADMIT · P2 | ADMIT · P3 | *invalid — a prerequisite cannot be NOT-NOW* |
| **SF1** on-stage | ADMIT · P1–P2 | ADMIT · P2–P3 | ADMIT · P3 | PARK · P4 |
| **SF2** adjacent | ADMIT if absorbable, else PARK · ≤P3 | PARK · P4 | PARK · P5 | PARK · P4 |
| **SF3** premature | PARK · P4 (+ future necessity MUST) | PARK · P4 | PARK · P5 | PARK · P5 |
| **SF4** stage-invalid | REJECT | REJECT | REJECT | REJECT |

Scope filter, applied **before** the matrix:

| Scope fit | Effect |
|-----------|--------|
| **SC0** explicit in scope | Proceed to matrix. |
| **SC1** derived necessity | Proceed to matrix; record the in-scope deliverable it serves. |
| **SC2** adjacent value | Force PARK to *Ideas* regardless of necessity. Never ADMIT. |
| **SC3** out of scope | REJECT. Record so the same idea is not re-litigated. |
| **SC4** externally mandated | **ESCALATE** to change control. Never silently admitted, never silently rejected. |

Overrides that bypass the matrix entirely: the **P1 interrupt classes** in
[05 §3](./05-PRIORITY_MODEL.md#3-hard-p1-overrides). Nothing else bypasses it.

---

## 7. Lifecycle of a governed change

```text
INPUT ──► TRIAGE (SUG-####) ──┬─► REJECT  → SUGGESTION-REGISTER (closed, reason)
                              ├─► PARK    → PARKED-BACKLOG (target stage + unpark trigger)
                              ├─► ESCALATE→ CHANGE-CONTROL (CR-###) → human decision
                              └─► ADMIT   → WORK ITEM (FUNC/NFR/TD/…)
                                              │
                                              ├─ needs > 1 acceptance outcome? → EPIC + stories
                                              ├─ confidence < C3?              → SPIKE first
                                              └─ IMPLEMENTATION PLAN
                                                     │
                                                     ▼
                                              REVIEW BOARDS (11)
                                                     │
                                        ┌────────────┴────────────┐
                                     REWORK                   APPROVED
                                        │                         │
                                   (max 2 rounds)                 ▼
                                        │              DEFINITION OF READY (12)
                                        └──► escalate            │
                                                                 ▼
                                                        DEPENDENCY ORDER (07)
                                                                 │
                                                                 ▼
                                                          IMPLEMENT (09/17)
                                                                 │
                                                                 ▼
                                                    DEFINITION OF DONE (13)
                                                                 │
                                                                 ▼
                                                     REGISTER UPDATE + METRICS
```

---

## 8. Non-negotiables

These have no override path short of an approved change request:

1. No code change without an admitted work item ID.
2. No `TODO` / `FIXME` / `HACK` without a work item ID in the same line.
3. No new runtime dependency, infrastructure component, or public contract without an
   Architecture verdict.
4. No PII, secret, credential, or raw upstream payload handling change without Security **and**
   Risk & Compliance verdicts.
5. No item marked Done without evidence satisfying [13](./13-DEFINITION_OF_DONE.md).
6. No parked item deleted without a recorded rejection reason.
7. No stage transition without the exit criteria in [04](./04-STAGE_GATES.md) being met and
   recorded.

---

## 9. Amending this framework

The framework itself is governed. Changes to any `docs/governance/**` file follow
[14-CHANGE_CONTROL.md](./14-CHANGE_CONTROL.md) with:

- work type `GOV`;
- Architecture + Product boards mandatory;
- a version bump in [README.md](./README.md) §1;
- a note in [registers/DECISION-REGISTER.md](./registers/DECISION-REGISTER.md).

An agent may **propose** framework changes. It may not apply them to L1 files without a human
approval recorded in the change request.
