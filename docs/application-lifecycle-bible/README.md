# The Application Lifecycle Bible

**The complete journey from business idea to mature enterprise platform — every stage, every
epic, every story, every gate, and how to prove you have passed one.**

Owner: Rajal (Product) + Mahesh (Architecture) · Version 1.0 · 2026-08-15 · Status: proposed
under CR-010

---

## Where we are right now

```
 S00  S01  S02  S03  S04  S05  S06  S07  S08  S09  S10  S11  S12  S13  S14  S15
  🟡   🟢   🟡   🟡   🟡   🔴   🟡   🟢   🔴   🔴   🟡   🔴   🟠   ⚪   ⚪   ⚪
                                     ▲     ▲          ▲     ▲
                                     └─────┴──────────┘     │
                                    FOUNDATION MISSING    ACTIVE WORK
```

**Active work is at S12 (Hardening). The two foundation stages beneath it, S08 and S09, are
essentially absent, and S11 — the stage that proves the business case — has never been
attempted.** That is the "seventh floor without a foundation" problem, stated in stage terms.

🟢 strong · 🟡 partial · 🟠 in progress · 🔴 missing or not started · ⚪ not reached

Full evidence: [`01-POSITION-ASSESSMENT.md`](./01-POSITION-ASSESSMENT.md).
What we do about it: [`03-REALIGNMENT-PLAN.md`](./03-REALIGNMENT-PLAN.md).

---

## Start here, depending on who you are

| You are… | Read this first |
|---|---|
| **New to the programme** | [`00-HOW-TO-USE.md`](./00-HOW-TO-USE.md), then the current stage file |
| **Asking "what phase are we in?"** | The banner above, then [`01-POSITION-ASSESSMENT.md §5`](./01-POSITION-ASSESSMENT.md) |
| **Asking "what do I do in this phase?"** | `stages/Sxx-*.md` → find your persona in the epic table |
| **Asking "when is this phase done?"** | Same file → *Exit gate* section |
| **Planning the next increment** | [`03-REALIGNMENT-PLAN.md`](./03-REALIGNMENT-PLAN.md) then [`09-JIRA-MODEL.md`](./09-JIRA-MODEL.md) |
| **An AI agent** | [`00-HOW-TO-USE.md §6`](./00-HOW-TO-USE.md) — and read AIGEM's `CURRENT-STATE.yaml` first, always |

---

## The framework

| Document | What it gives you |
|---|---|
| [`00-HOW-TO-USE.md`](./00-HOW-TO-USE.md) | Navigation, the five-minute orientation, agent rules |
| [`01-POSITION-ASSESSMENT.md`](./01-POSITION-ASSESSMENT.md) | Evidence-backed diagnosis of where this platform actually is |
| [`02-STAGE-MODEL.md`](./02-STAGE-MODEL.md) | The 16 stages, their mapping onto AIGEM L0–L10, ID scheme, movement rules |
| [`03-REALIGNMENT-PLAN.md`](./03-REALIGNMENT-PLAN.md) | The five-move underpinning programme |
| [`04-GATE-AND-SIGNOFF-MODEL.md`](./04-GATE-AND-SIGNOFF-MODEL.md) | Gate mechanics, evidence rules, RACI per stage |
| [`05-DOCUMENTATION-CANON.md`](./05-DOCUMENTATION-CANON.md) | Every document that should exist, at which stage, owned by whom |
| [`06-QUALITY-NORMS.md`](./06-QUALITY-NORMS.md) | Test strategy, pyramid, coverage, defect and regression policy |
| [`07-SECURITY-COMPLIANCE-CANON.md`](./07-SECURITY-COMPLIANCE-CANON.md) | Security gates, threat modelling cadence, IRDAI/RBI control evidence |
| [`08-SRE-READINESS-CANON.md`](./08-SRE-READINESS-CANON.md) | SLIs/SLOs, operational readiness review, incident and DR readiness |
| [`09-JIRA-MODEL.md`](./09-JIRA-MODEL.md) | Hierarchy, workflow, fields, import guide |
| [`10-ACTOR-EPIC-STORY-MAP.md`](./10-ACTOR-EPIC-STORY-MAP.md) | Application actors, delivery-wave boundaries and the epics/stories each actor requires |

## The stages

| Stage | File | AIGEM | Position |
|---|---|---|---|
| S00 Ideation & Business Case | [`stages/S00-ideation.md`](./stages/S00-ideation.md) | L0 | 🟡 |
| S01 Discovery & Capability Definition | [`stages/S01-discovery.md`](./stages/S01-discovery.md) | L0 | 🟢 |
| S02 Regulatory, Risk & Compliance Framing | [`stages/S02-regulatory-framing.md`](./stages/S02-regulatory-framing.md) | L1 | 🟡 |
| S03 Business Requirements & Process Design | [`stages/S03-business-requirements.md`](./stages/S03-business-requirements.md) | L1 | 🟡 |
| S04 Product Definition & Release Slicing | [`stages/S04-product-definition.md`](./stages/S04-product-definition.md) | L1 | 🟡 |
| S05 Experience Design & Service Blueprint | [`stages/S05-experience-design.md`](./stages/S05-experience-design.md) | L1 | 🔴 |
| S06 Domain & Information Architecture | [`stages/S06-domain-architecture.md`](./stages/S06-domain-architecture.md) | L2 | 🟡 |
| S07 Solution & Security Architecture | [`stages/S07-solution-architecture.md`](./stages/S07-solution-architecture.md) | L3 | 🟢 |
| **S08 Engineering Foundation** | [`stages/S08-engineering-foundation.md`](./stages/S08-engineering-foundation.md) | L4 | 🔴 |
| **S09 Platform & Environment Foundation** | [`stages/S09-platform-foundation.md`](./stages/S09-platform-foundation.md) | L4 | 🔴 |
| S10 Integration & Connectivity | [`stages/S10-integration.md`](./stages/S10-integration.md) | L5 | 🟡 |
| S11 Vertical Slice (MVP) | [`stages/S11-vertical-slice.md`](./stages/S11-vertical-slice.md) | L6 | 🔴 |
| S12 Hardening & Certification | [`stages/S12-hardening.md`](./stages/S12-hardening.md) | L7 | 🟠 |
| S13 Expansion & Scale | [`stages/S13-expansion.md`](./stages/S13-expansion.md) | L8 | ⚪ |
| S14 Production Readiness & Go-Live | [`stages/S14-production-readiness.md`](./stages/S14-production-readiness.md) | L9 | ⚪ |
| S15 Operate, Evolve & Continuous Assurance | [`stages/S15-operate-evolve.md`](./stages/S15-operate-evolve.md) | L10 | ⚪ |

## Backlog and templates

| Path | Purpose |
|---|---|
| [`backlog/BACKLOG.yaml`](./backlog/BACKLOG.yaml) | Machine-readable SSOT for every stage, epic and story |
| [`backlog/jira-import.csv`](./backlog/jira-import.csv) | Generated Jira import file |
| [`backlog/README.md`](./backlog/README.md) | How to regenerate and import |
| [`templates/`](./templates/) | Epic, story, gate sign-off, validation test, ORR templates |

---

## How this relates to AIGEM

AIGEM ([`docs/governance/`](../governance/README.md)) remains **authoritative for triage**: what
may be admitted, parked or rejected, and at what priority. Until CR-010 is ratified, this bible
is the **proposed completion model**: what a stage should produce, who should sign it, and what
evidence should close it.

> On any conflict, **AIGEM wins**, and the conflict is raised as a CR against this bible.

Practical division:

| Question | Authority |
|---|---|
| "Should we do X now?" | AIGEM — [`03-LIFECYCLE`](../governance/03-LIFECYCLE.md), [`05-PRIORITY_MODEL`](../governance/05-PRIORITY_MODEL.md) |
| "What must this stage produce?" | Binding AIGEM gate today; this bible after CR-010 ratification |
| "Who approves it?" | Both — [`PERSONA-AUTHORITY-MATRIX`](../governance/PERSONA-AUTHORITY-MATRIX.md) is canonical for authority; this bible maps it per stage |
| "Are we done?" | Current AIGEM gate; this bible is a completeness cross-check until ratified |
| "Where are we?" | AIGEM's [`state/CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml) is the machine truth; the banner above is its human rendering |

---

## Status and ratification

This framework is **proposed, not binding**. It was authored by the Product Owner persona and
carries Product-scope conclusions. Architecture, Security, Compliance, QA, SRE and Delivery
conclusions in it are **findings for those boards**, not verdicts.

Adoption requires **CR-010** with verdicts from Boards 1–7. Until then it is a planning
instrument: read it, plan against it, and do not cite it as authority in a triage record.
