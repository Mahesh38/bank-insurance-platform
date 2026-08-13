# 03 — Delivery Model & Flow Plan

**Owner:** Platform Product Owner
**Date:** 2026-08-12
**Status:** 🟠 PROPOSAL — the mechanics of [02-REALIGNMENT-PROPOSAL.md](./02-REALIGNMENT-PROPOSAL.md)
**Scope:** How work is phased, chunked, staffed, cadenced and demonstrated from the next sprint onward

---

## 1. The corrected phase model

The PO's stated ideal — *"analysis, get the details done, define the guidelines, define the
phases, define the scope of each phase, sub-phases, create smaller deliverable chunks, then
work on those chunks"* — expressed as a model we can actually run against.

There are **two levels**. Confusing them is what produced the current situation: the programme
ran the flow level (build a service) without ever completing the programme level (agree what the
product is).

```text
PROGRAMME LEVEL  (once, now — this is the real "Phase Zero")
   PZ.1 Scope sign-off        PZ.2 Ways of working      PZ.3 Environment & tooling
   PZ.4 Flow map & sequence   PZ.5 Canonical model v0   PZ.6 Traceability baseline (RTM)

FLOW LEVEL       (repeats, once per flow, forever)
   S1 Requirement → S2 Design → S3 Ready → S4 Build → S5 Prove → S6 Demo & sign-off
```

### Phase Zero (PZ) — the analysis phase that was skipped

Runs in Sprint 1, Track A. **It is not a documentation phase; it is six named artefacts with
owners and a signature each.**

| ID | Deliverable | Definition of complete | Owner | Closes |
|----|-------------|------------------------|-------|--------|
| **PZ.1** | **Scope sign-off** | Sponsor **named**; Working Decisions v1 and R0-SCOPE signed, not "working" | PO + Sponsor | `GAP-010`, Wave 0 exit |
| **PZ.2** | **Ways of working** | Definition of Ready, Definition of Done incl. traceability (Rule S-3), branch/PR rules, review boards per risk tier, sprint calendar, demo format | Delivery Lead + Architect | `GAP-018` |
| **PZ.3** | **Environment & tooling** | Dev / test / UAT environments named and reachable; 1SB sandbox credentials; Keycloak realm; CI pipeline; test data set; Jira/board structure mirroring flows | Tech Lead + Ops | Extends [PO-DEV-ENV-REQUIREMENTS](../../../1sb-insurance-integration/service-ssot/phase-0/PO-DEV-ENV-REQUIREMENTS.md) to the platform |
| **PZ.4** | **Flow map & sequence** | The flow list in §3 agreed, sequenced, and each flow sized to fit 1–2 sprints | PO + Architect + Tech Lead | — |
| **PZ.5** | **Canonical model v0** | Bank-owned aggregates — Lead, Consent, Suitability, Quote, Proposal, Policy — with lifecycles and invariants, **independent of 1SB's model** | Architect + BA | `GAP-016`, P5 |
| **PZ.6** | **Traceability baseline** | RTM exists; every delivered behaviour in the five built services has a row (`BR` mapped, or flagged as unrequested) | BA | F4 exposure in [01 §4](./01-PROCESS-GAP-ANALYSIS.md#4-are-the-five-services-the-wrong-scope) |

**PZ exit criterion (one line):** *A developer can pick up the top item on the board and know
which signed requirement it satisfies, which environment to build it in, and who signs it off.*
Today, none of those three are true.

### Sub-phases of a flow (S1–S6)

Every flow — new or retro-fitted — passes through the same six sub-phases. This is the "smaller
deliverable chunk" the PO asked for, and it is small enough to fit in one or two sprints.

| Sub-phase | Produces | Owner | Gate to leave it |
|-----------|----------|-------|------------------|
| **S1 · Requirement** | `BR-xxx` business requirement + PRD story + **Gherkin acceptance criteria** + business rules + data attributes | BA + PO | PO signs; **QA Lead confirms each AC is testable**; Compliance flags any regulated rule |
| **S2 · Design** | Journey/screen reconciliation to Figma · canonical contract (API + domain) · ADR for anything new · NFR targets for this flow · security & threat notes | Architect + UX + Security | Architect signs the contract; Security signs if the flow touches identity, PII, payment or consent |
| **S3 · Ready** | Work items split, estimated, dependencies resolved, test cases drafted from the AC, environment/test data ready | Tech Lead + QA | [Definition of Ready](../../../governance/12-DEFINITION_OF_READY.md) met — **nothing enters a sprint without this** |
| **S4 · Build** | Working code behind the canonical contract; unit + integration tests; every commit cites the requirement ID | Engineering | Code review + coverage gate green |
| **S5 · Prove** | AC executed as tests · E2E on the flow · security check · compliance evidence sample (consent log, audit record) · perf smoke where the NFR demands it | QA + Security + Compliance | [Definition of Done](../../../governance/13-DEFINITION_OF_DONE.md) with **evidence attached** |
| **S6 · Demo & sign-off** | Sprint demo to stakeholders; RTM row closed; flow marked Done | PO | Business accepts, or a defect is raised — never "accepted with reservations" |

> **The rule that makes this real:** S1 and S2 for flow *n+1* run **in the same sprint** as S4
> and S5 for flow *n*. That is what keeps a pipeline full without the build ever running ahead of
> the signed requirement. It is also exactly what did not happen in the last three weeks.

---

## 2. Flow sequence — and one correction to the proposed order

The PO proposed: *login flow → authorization flow → quotation flow → lead creation flow.*

**The first two are right. The last two are in the wrong order, and the repository already says
so:** decision **D-005** makes need analysis and suitability a **mandatory gate before a quote**,
and `D-002`/`D-009` scope the journey to ETB customers reached through a lead. A quote with no
lead behind it has no customer, no consent, and no suitability record — so it cannot legally be
shown to a customer under the corporate-agency rules the programme is built around.

**Corrected sequence: lead → consent → suitability → catalogue → quote.** The 1SB quote adapter
already being built does not change this; it means F7 arrives with most of its engineering
already done, which is a bonus, not a reason to sequence it earlier.

---

## 3. The flow map

| # | Flow | Depends on | Built today? | Track | Sprint (indicative) |
|:-:|------|-----------|--------------|-------|:-------------------:|
| **F0** | **Phase Zero** — PZ.1–PZ.6 | — | Partly (env, CI) | A | 1 |
| **F1** | **Workforce login** — RM/staff authenticates; token-hiding session | F0 | ✅ `workforce-access-bff`, `identity-provider-adapter-service` | B builds · **A retro-specs** | 1–2 |
| **F2** | **Authorization & entitlements** — roles, branch/insurer scope, hierarchy, maker-checker | F1 | ✅ `identity-authorization-service` (PDP) — **business role model unspecified** | B builds · **A specifies** | 2–3 |
| **F3** | **Customer lookup & lead creation** — CIF prefill, ETB validation, lead lifecycle | F2, PZ.5 | ❌ Not started | **A** | 3–4 |
| **F4** | **Consent capture & gate** — digital consent, sequencing, immutable log | F3, `GAP-006` | ❌ Not started | **A** | 4–5 |
| **F5** | **Need analysis & suitability** — mandatory gate, recommendation record, override rules | F4, `GAP-007` | ❌ Not started | **A** | 5–6 |
| **F6** | **Product catalogue & eligibility** — Product Matrix, Group A/B routing | F5, `GAP-013` | ❌ Not started | **A** | 6 |
| **F7** | **Quote & compare** — canonical quote contract over the 1SB adapter | F6 | ⚠️ Adapter side ✅ (Term slice); **canonical contract and compare rules** ❌ | A+B **seam** | 6–7 |
| **F8** | **Proposal & dynamic form** — proposal submission, dynamic form rendering | F7 | ⚠️ Adapter side ✅ | A+B seam | 7–8 |
| **F9** | **Payment** — customer-device payment link, AU Bank PG, status reconciliation | F8, `GAP-011` | ⚠️ Partial — `FUNC-008` payment intimation open (`TD-022`) | A+B seam | 8–9 |
| **F10** | **Policy issuance & documents** — issuance visibility, document access, "Sold" confirmation | F9 | ❌ Not started | **A** | 9–10 |
| **F11** | **Audit, attribution & MIS** — cross-cutting; extended by every flow above | All | ⚠️ Audit lib ✅; attribution model open (`GAP-014`) | Both, incremental | Every sprint |

**Sprint numbers are indicative and deliberately not committed.** F4 and F5 both sit behind open
regulatory rule packs (`GAP-006`, `GAP-007`); until Compliance closes those, their S1 sub-phase
cannot complete and any date attached to them is fiction. **Say this to the steering committee
before they infer a plan from the right-hand column.**

### How F1 and F2 are handled — the pattern for all retro-fitted flows

F1 and F2 are the template for retro-fitting, because they are the flows where code already
exists:

```text
Track B                          Track A                        Converge
────────                         ────────                       ─────────
Close A.1–A.6 gate criteria      S1: write BR-xxx + AC for      RTM rows closed;
Prove token-hiding               login and entitlements,        AC run as tests against
Prove default-deny PDP           retrospectively, to current    the built services;
Maker-checker evidence           standard                       gaps become defects,
Retention config evidence        S2: confirm the built design   not rework
                                 matches the canonical model
```

If the built design and the written requirement disagree, that is a **decision for the war room's
escalation path (Rule S-4)** — not a unilateral fix in either direction. The likely candidates
are the business role model and RM hierarchy, where `GAP-014` is still open.

---

## 4. Cadence — what happens every two weeks

| When | Event | Duration | Output |
|------|-------|----------|--------|
| Sprint day 1 | **Sprint planning** — both tracks, one room | 2 h | Committed sprint backlog; every item cites a requirement ID or a gate/debt ID |
| Daily | Stand-up per track | 15 min | Blockers; cross-track blockers go to PO + Tech Lead same day |
| Mid-sprint | **Design review** for the next flow's S2 | 1 h | Architect + Security verdicts recorded |
| Mid-sprint | **AC review** for the next flow's S1 | 1 h | QA Lead confirms testability; Compliance flags regulated rules |
| Sprint day 10 | **Demo to stakeholders** | 1 h | Working software, one flow, end to end. **Not slides** |
| Sprint day 10 | **Retro + governance check** | 1 h | `FreshnessCheck` green; RTM updated; Rule S-1 breaches reviewed |
| Every 2 sprints | **Steering update** | 30 min | Flow completion, gate states, risks, decisions needed |

**Demo rule:** every sprint demonstrates *working software*, even during Phase Zero. Sprint 1's
demo is Track B's hardening evidence (E2E run, OpenAPI, runbook) — not a document walkthrough.
This is the countermeasure to risk R1 in [02 §8](./02-REALIGNMENT-PROPOSAL.md#8-risks-of-this-proposal-stated-by-the-proposer),
and it is what the PO's *"show progress every two weeks"* actually requires.

---

## 5. The first three sprints, concretely

| | **Track A — Foundation-first** | **Track B — Harden & hold** | **Demo at day 10** |
|---|---|---|---|
| **Sprint 1** | PZ.1 sponsor + scope signature · PZ.2 ways of working · PZ.4 flow map agreed · PZ.6 RTM skeleton + F1/F2 rows · S1 for F1 (login) | WS-1: 4.1 sandbox E2E in CI · 4.5 ops runbook · 4.7 coverage/QA-001 · WS-2: A.1 token-hiding evidence | Sandbox E2E running in CI; token-hiding proven |
| **Sprint 2** | PZ.5 canonical model v0 (Lead, Consent, Suitability) · S1 for F2 (entitlements) · S2 for F3 (lead creation contract) | WS-1: 4.4 compliance review · 4.6 perf smoke · 4.2 consumer collection · WS-2: A.2–A.4 | Compliance evidence pack; PDP default-deny; p95 quote measurement |
| **Sprint 3** | S1 for F3 · S3+S4 for F3 (lead creation build starts, contract-first) · consent rule pack chase with Compliance | WS-1: 4.3 bank caller UAT (external dependency) · WS-2: A.5–A.6 · debt: TD-014, TD-009 | **Lead creation flow, end to end, against the canonical contract** |

**Sprint 3's demo is the proof point of this entire proposal:** the first flow built the right
way round, requirement to demo, in one sprint, on foundations that already exist. If that demo
does not happen, the model is not working and the room should say so at the Sprint 3 retro rather
than at the end of the quarter.

---

## 6. Definition of Ready and Definition of Done — the two amendments

The existing [12-DEFINITION_OF_READY](../../../governance/12-DEFINITION_OF_READY.md) and
[13-DEFINITION_OF_DONE](../../../governance/13-DEFINITION_OF_DONE.md) are sound. This proposal
adds exactly two clauses, both part of `CR-002`:

| Amendment | Text | Why |
|-----------|------|-----|
| **DoR +1** | *"The work item cites a signed requirement ID (`BR-xxx` / PRD story) with acceptance criteria, or a gate criterion / debt ID it closes."* | Makes Rule S-1 enforceable at planning, not at merge |
| **DoD +1** | *"The RTM row for this behaviour is complete: requirement ID → acceptance criteria → test evidence."* | Produces the audit trail continuously instead of retrospectively |

Two clauses. That is the entire process change required at the team level — everything else in
this pack is sequencing and staffing.

---

## 7. What we measure

Four metrics, reviewed at every retro. Chosen because each one detects a specific way this
proposal fails.

| Metric | Target | Detects |
|--------|:------:|---------|
| **Traceability coverage** — % of delivered behaviours with an RTM row | 100% by C5 (Sprint 4) | The original defect returning |
| **Rule S-1 breaches** — merged PRs with no requirement/gate/debt ID | 0 per sprint | Track B drifting ahead again |
| **Flow lead time** — S1 signed → S6 demoed | ≤ 2 sprints | Flows sized too large to demo |
| **Demo count** — sprints ending with working software demonstrated | 1 per sprint, every sprint | Track A becoming a documentation phase (R1) |

**Deliberately not measured:** story points, velocity, and document counts. None of them would
have detected the problem this pack exists to fix — the programme was fast, productive, and
building the wrong way round at the same time.

→ Run the session: [04-WAR-ROOM-RUNSHEET.md](./04-WAR-ROOM-RUNSHEET.md)
