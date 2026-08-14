# Decision Register

Index of decisions that constrain future work: ADRs, change requests, stage transitions, and
board escalations. **A decision recorded here may not be re-litigated without new evidence**
([14 §6](../14-CHANGE_CONTROL.md#6-reversing-a-rejection)).

**Owner:** Architect
**Upstream logs:** [architecture-review/08-architecture-decision-log.md](../../platform/architecture-review/08-architecture-decision-log.md) ·
[au-bank-insurance-platform/DECISION-LOG.md](../../au-bank-insurance-platform/DECISION-LOG.md)

---

## 1. Architecture decisions (ADR index)

Full ADRs live in the architecture decision log. This index exists so triage can cite them
without a search.

| ID | Decision | Status | Constrains |
|----|----------|--------|------------|
| — | Bank apps never call 1SB or the DB directly; integration service is the only path | Accepted | All WS-1 topology suggestions |
| — | Replaceable middleware: 1SB behind ports/adapters (Case 2) | Accepted | Any proposal to call 1SB from orchestration |
| — | Persistence is platform-common (`bank-persistence-service`), reached over HTTP | Accepted | Any proposal to embed a DB in a consumer |
| — | Integration service owns no Flyway/JPA | Accepted | Any persistence change in the integration service |
| — | Orchestration first, LOB handler second (`QuoteService` → `TermQuoteHandler`) | Accepted | LOB expansion design |
| — | Flutter communicates only with the workforce BFF | Accepted | Any direct-IdP proposal |
| — | Flutter never receives OAuth tokens (token-hiding session) | Accepted | Session design |
| — | Keycloak behind `identity-provider-adapter-service`; provider-neutral | Accepted | Provider-specific code placement |
| — | `identity-authorization-service` is the business source of truth for authorization | Accepted | Any proposal to use IdP roles as business roles |
| — | Authorization is default-deny, RBAC + ABAC + resource relationships | Accepted | Policy design |
| — | Maker-checker for bulk and privileged identity changes | Accepted | Admin flows |
| — | Production IdP decision deferred behind the adapter | Accepted (deferral) | Do not re-open before WS-2 Phase 2 |

> ADR IDs are assigned by the architecture decision log. New architectural decisions arising
> from AIGEM triage are raised there and indexed here.

## 2. Governance decisions

| ID | Date | Decision | Rationale | Decided by |
|----|------|----------|-----------|------------|
| GOV-001 | 2026-08-07 | Adopt AIGEM 1.0 as the governance model for this repository | Prevent AI scope drift; schedule suggestions rather than implementing or losing them | Pending ratification |
| GOV-002 | 2026-08-07 | Route admitted work to existing backlogs; AIGEM keeps no parallel backlog | Two sources of truth both rot | Pending ratification |
| GOV-003 | 2026-08-07 | Seed the parked backlog from deferred `TECH-DEBT.md` rows; no retrospective triage | Backfilling costs days and teaches nothing ([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)) | Pending ratification |
| GOV-004 | 2026-08-10 | **Ratify the current-state snapshot**: WS-1 at Phase 4 (Hardening), WS-2 at IAM Phase 1, with the scope and standing constraints as recorded | Reconstructed from `ACTION-PLAN.md`, phase `STATUS.md` files, `TECH-DEBT.md` and git history; reviewed and accepted | **Mahesh (Solution Architect), 2026-08-10** — PO counter-signature outstanding |
| GOV-005 | 2026-08-14 | Consolidate the architecture role into **Mahesh — Principal Insurance Platform Architect** as the single Board 1 persona, with modular authority/review files and a reciprocal Mahesh↔Shailja protocol | Avoid two overlapping architect identities while retaining deep architecture reasoning, evidence, compliance collaboration and human escalation | **Proposed by Mahesh** via CR-002 — Product counter-signature outstanding |

## 3. Change requests

| ID | Date | Type | Summary | Decision | Approvers |
|----|------|------|---------|----------|-----------|
| CR-001 | 2026-08-10 | STAGE | Add exit criterion **4.7** (coverage gates green; QA-001 closed or waived with expiry) to the WS-1 Phase 4 gate | **APPROVED** 2026-08-10 | Mahesh (Solution Architect) — PO + QA Lead counter-signature outstanding |
| CR-002 | 2026-08-14 | GOV | Make Mahesh the single Principal Insurance Platform Architect persona; modularize his authority/review model; retain Shailja as independent Board 6; keep legacy architect path as compatibility-only | **PENDING RATIFICATION** | Mahesh approved preparation on review branch — Product Owner pending |

### CR-001 — add Phase 4 exit criterion 4.7

```text
current_position:  ACTION-PLAN.md Phase 4 defines exit criteria 4.1-4.6. Coverage is not
                   among them. QA-001 is tracked as P0 tech debt with "Partial" status.
proposed_change:   Add 4.7 to 04-STAGE_GATES.md as a binding criterion.
driver:            QA-001 is P0 debt; 15-TECH_DEBT_POLICY forbids a P0 debt item crossing a
                   gate. Either 4.7 is a criterion, or QA-001 is not P0. Today the two
                   documents disagree.
raised_because:    The criterion was added during framework authoring WITHOUT a CR - a
                   governance violation caught in review. It is demoted to PROPOSED until
                   ratified, rather than quietly kept.
impact:            If approved, Phase 4 cannot pass with the service coverage floor still
                   "interim". If rejected, QA-001 must be re-severitised below P0.
alternatives:      (a) approve as written  (b) reject and downgrade QA-001 to P1
                   (c) approve with a dated waiver for the interim service floor
decision:          APPROVED (2026-08-10, Mahesh / Solution Architect)
chosen_option:     (a) approve as written
consequence:       Phase 4 cannot pass with the service coverage floor still "interim".
                   QA-001 must close, or carry a dated waiver co-approved by TL + QA Lead
                   per 15-TECH_DEBT_POLICY section 4.
outstanding:       PO and QA Lead counter-signature. The criterion is binding now; the
                   counter-signature is recorded when they next review the gate.
```

### CR-002 — Mahesh Principal Insurance Platform Architect consolidation

Full request: [`../change-requests/CR-002-principal-architect-persona-integration.md`](../change-requests/CR-002-principal-architect-persona-integration.md)

```text
current_position:  Mahesh is the existing architecture owner; the first CR-002 draft introduced
                   a second Principal Architect persona attached to Mahesh.
proposed_change:   Make Mahesh the single Principal Insurance Platform Architect; move the deep
                   authority/decision/review model into Mahesh-named supporting files; preserve the
                   old generic path only as a compatibility redirect; keep Shailja independent.
driver:            One Architecture Board should have one architecture identity and one authority
                   model. Modular knowledge is useful; duplicate ownership is not.
impact:            Governance/persona grounding only; no runtime product behavior changes.
safeguards:        T4 human sign-offs remain mandatory; Security and Risk/Compliance vetoes remain
                   binding; Shailja R0 cannot be downgraded by Architecture.
decision:          PENDING RATIFICATION
authority:         Mahesh approved preparation on review branch; Product Owner counter-signature
                   required before governance changes are treated as ratified/binding.
```

## 4. Stage transitions

| Date | Workstream | From | To | Criteria met | Waivers | Approvers |
|------|------------|------|----|--------------|---------|-----------|
| — | WS-1 | Phase 3 | Phase 4 | Term vertical slice delivered (FUNC-001…007, FUNC-009) | — | Recorded retrospectively from `phase-4/STATUS.md`; not gate-reviewed under AIGEM |
| 2026-08-10 | Both | *(provisional)* | *(ratified)* | State snapshot accepted as the governing context — see GOV-004 | — | Mahesh (Solution Architect) |

## 5. Board escalations

Conflicts between boards that required an Architect + PO resolution
([11 §12](../11-REVIEW_GATES.md#12-aggregation)).

| ID | Date | Plan | Conflict | Resolution | Recorded as |
|----|------|------|----------|------------|-------------|
| — | — | — | *none* | — | — |

## 6. Rejections of note

Rejected proposals worth remembering, so they are not re-argued from scratch.

| ID | Proposal | Rejected because | Reopen if |
|----|----------|------------------|-----------|
| — | — | *none yet* | — |
