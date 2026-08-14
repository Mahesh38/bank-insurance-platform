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

## 3. Change requests

| ID | Date | Type | Summary | Decision | Approvers |
|----|------|------|---------|----------|-----------|
| CR-001 | 2026-08-10 | STAGE | Add exit criterion **4.7** (coverage gates green; QA-001 closed or waived with expiry) to the WS-1 Phase 4 gate | **APPROVED** 2026-08-10 | Mahesh (Solution Architect) — PO + QA Lead counter-signature outstanding |
| CR-002 | 2026-08-14 | STAGE + ARCH | Amend criterion **4.2**: OpenAPI is a **local/dev testing artefact**, not a published surface. No internal portal, no URL, and the spec endpoints are never served on UAT or production. Applies to **all** platform services from now on | **APPROVED** 2026-08-14 | Mahesh (Solution Architect) — PO + Security Architect counter-signature outstanding |

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

### CR-002 — OpenAPI is a local/dev artefact, not a published surface

```text
current_position:  Criterion 4.2 reads "OpenAPI published to the internal portal; consumer
                   collection available". Engineering had generated and verified the document
                   but recorded the portal publication as outstanding, holding 4.2 at PARTIAL.
proposed_change:   Amend 4.2. There is no internal portal and there will not be one. The
                   OpenAPI specification is a local and dev testing aid: it exists so that
                   someone wanting to exercise the API does not have to hand-build a Postman
                   collection. It is not a product surface.
driver:            1sb-integration-service is reachable from inside the VPC only. It is not on
                   a public cloud and has no public consumer, so "publishing a contract" has no
                   audience. The original criterion assumed a distribution model this platform
                   does not use.
raised_because:    The Solution Architect stated the deployment model while reviewing the 4.2
                   evidence. Amending a ratified gate criterion requires a CR (14 section 1) —
                   the same rule that produced CR-001. Silently redefining 4.2 to match what
                   had been built would be precisely the drift CR-001 was raised to punish.
impact:            (a) 4.2's portal requirement is removed; the criterion is met by the
                       generated, verified specification plus the consumer collection.
                   (b) A platform-wide constraint follows: EVERY service may carry an OpenAPI
                       specification, and NO service exposes it on UAT or production.
                   (c) A live gap is closed. springdoc was enabled by default in every profile,
                       so /v3/api-docs and /swagger-ui.html answered on any deployed host.
                       render.yaml publishes port 8080, so on that deployment the API browser
                       was publicly reachable.
alternatives:      (a) amend as written
                   (b) keep the portal requirement and hold 4.2 open indefinitely against a
                       portal that will never exist
                   (c) amend, but allow the spec on UAT for consumer convenience
decision:          APPROVED (2026-08-14, Mahesh / Solution Architect)
chosen_option:     (a) amend as written. (c) was not taken: UAT carries real journey data, and
                   an API browser there is an attack surface bought for a convenience the
                   committed specification already provides.
consequence:       4.2 is MET. springdoc defaults to disabled everywhere; uat and prod pin it
                   off so SPRINGDOC_ENABLED cannot re-enable it. Enforced by
                   OpenApiNotExposedTest, which fails if any profile serves the endpoints.
                   New services inherit the constraint — see 01 section 5.
outstanding:       PO counter-signature, and a Security Architect verdict on the exposure that
                   existed before this change (how long deployed hosts served Swagger UI).
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
