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
| GOV-006 | 2026-08-14 | Propose assigning existing **AIGEM R10 / Board 7 Operations** to **Shivanshi — Principal Insurance Platform SRE / Reliability Engineering Head** and mature the existing SRE capability | Preserve the original Operations controls while adding insurance/bancassurance domain intelligence, platform/CI-CD, observability, incidents/DR, business-aware capacity/scaling and developer enablement without creating a second SRE role | **Proposed by Mahesh** via CR-008 — required governance ratification pending |
| GOV-007 | 2026-08-14 | **Recalibrate the framework for flow**: T4 triggers become a change test (RG-5/RG-6); `GOV` work is queued and counted (BR-4/GC-1); board response clock (RG-7); approvals expire at 30 days or on changed context (RG-8); R12 may force a decision's timing but never its content (PA-1); binding-veto deadlock gets a named human tie-breaker (PA-2); persona roster closed at nine (CC-2/CC-3); gate criteria closed per week becomes the headline metric (GM-1); freshness accepts a reviewed-no-change acknowledgement (FR-1) | Measured: 0 of 7 and 0 of 6 gate exit criteria closed, 61 consecutive commits with no product code, docs-to-code ratio 2.10, one suggestion processed — while every mechanical check reported healthy. The framework was consuming the delivery capacity it exists to protect. No board, veto, jurisdiction or mandatory human sign-off is changed | **Raised by agent:claude** via CR-009 — ratification pending; **A1 and B4 require Deepali and Shailja explicitly** |

## 3. Change requests

| ID | Date | Type | Summary | Decision | Approvers |
|----|------|------|---------|----------|-----------|
| CR-001 | 2026-08-10 | STAGE | Add exit criterion **4.7** (coverage gates green; QA-001 closed or waived with expiry) to the WS-1 Phase 4 gate | **APPROVED** 2026-08-10 | Mahesh (Solution Architect) — PO + QA Lead counter-signature outstanding |
| CR-002 | 2026-08-14 | GOV | Make Mahesh the single Principal Insurance Platform Architect persona; modularize his authority/review model; retain Shailja as independent Board 6; keep legacy architect path as compatibility-only | **PENDING RATIFICATION** | Mahesh approved preparation on review branch — Product Owner pending |
| CR-008 | 2026-08-14 | GOV | Name Shivanshi as existing R10 / Board 7 Operations persona and mature SRE with insurance-domain, platform, CI/CD, observability, incident/DR, capacity/scaling and developer-experience capability | **PENDING RATIFICATION** | Prepared on Mahesh/user direction — Architecture + Product and any other required governance ratification pending |
| CR-009 | 2026-08-14 | GOV | Recalibrate the framework for flow: T4 change test, `GOV` work queued and counted, board response clock, 30-day approval expiry, R12 decision-forcing, veto-deadlock tie-breaker, persona roster closed at nine, gate-closure headline metric, reviewed-no-change freshness | **PENDING RATIFICATION** | Raised by `agent:claude`; Architecture + Product required, **Deepali and Shailja mandatory for A1 and B4** |

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

### CR-008 — Shivanshi Principal Insurance Platform SRE / R10 integration

Full request: [`../change-requests/CR-008-add-shivanshi-sre-persona.md`](../change-requests/CR-008-add-shivanshi-sre-persona.md)

```text
current_position:  AIGEM already has an unnamed R10 DevOps/SRE role and Board 7 Operations with
                   deployability, observability, alerting, failure-mode, rollback, capacity/cost,
                   runbook and rolling-deploy controls.
proposed_change:   Preserve those controls and assign/mature the same role as Shivanshi — a
                   business-aware Principal Insurance Platform SRE with platform engineering,
                   infrastructure, CI/CD, incidents, resilience, DR, business-aware scaling and
                   developer self-service capability.
driver:            The bank insurance platform needs an SRE that understands the business workload,
                   not a second generic infrastructure persona or CPU-only scaling model.
impact:            Governance/persona grounding only; no runtime application behavior or production
                   configuration changes in this CR.
safeguards:        No eighth board; O1-O8 preserved; no blind/unbounded scaling; Product,
                   Architecture, Engineering, Security, DBA, QA, Compliance and Delivery authority
                   remain separate; mandatory human authority remains human.
decision:          PENDING RATIFICATION
authority:         Prepared on explicit Mahesh/user direction. Required governance approvals remain
                   to be recorded before treating the L1 change as ratified/binding.
```

### CR-009 — governance flow recalibration

Full request: [`../change-requests/CR-009-governance-flow-recalibration.md`](../change-requests/CR-009-governance-flow-recalibration.md)

```text
current_position:  The framework is internally consistent and every mechanical check passes.
                   11 section 3 escalates to T4 on subject matter, so every WS-2 and most WS-1
                   changes are T4 by definition. GOV work routes only through change control and
                   never enters the queue. Board silence has no clock. Approvals expire on stage
                   boundary. R12 owns the date with no lever. Binding vetoes have no tie-breaker.
proposed_change:   Ten changes: T4 becomes a change test (G1-G10, RG-5/RG-6); GOV work is triaged,
                   queued and counted (BR-4/GC-1); board response clock escalates to named humans
                   (RG-7); approvals expire at 30 days or on changed context (RG-8); R12 may force
                   a decision's timing but never its content (PA-1); veto deadlock gets a named
                   human tie-breaker by conflict class (PA-2); persona roster closed at nine
                   (CC-2/CC-3); gate criteria closed per week becomes the headline metric with a
                   self-alarm (GM-1); CI reports docs-to-code ratio; freshness accepts a dated,
                   attributed reviewed-no-change acknowledgement (FR-1).
driver:            New evidence — measurement of the framework's own operating record.
                   0 of 7 and 0 of 6 gate exit criteria closed. 61 consecutive commits with no
                   product code. Docs-to-code ratio 2.10. One suggestion processed. Six personas
                   added in seven days. Every check green throughout.
impact:            Governance framework and two scripts only. No runtime, API or configuration
                   change. No gate date moves directly.
safeguards:        Seven boards unchanged. Security and Compliance vetoes remain binding and
                   non-overridable. No mandatory T4 human sign-off relaxed. No jurisdiction moves —
                   PA-1 grants timing authority only. Silence still never approves. C2 warns and
                   cannot block. C3 resets age only and never suppresses a content check.
                   Ambiguous tiering goes to T3, not T1, and any single board may escalate to T4
                   with no CR.
decision:          PENDING RATIFICATION
authority:         Raised by an AI agent under Rule CC-1, which forbids that agent from approving
                   it. A1 and B4 change when Security and Compliance are convened and what happens
                   when they disagree; they must not merge without Deepali's and Shailja's
                   explicit verdicts.
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
| — | — | *none yet* | — | — |
