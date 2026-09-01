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
| — | ~~Persistence is platform-common (`bank-persistence-service`), reached over HTTP~~ | **SUPERSEDED by `ADR-019`** (2026-08-29, `CR-015` Option B) | Retired. Persistence ownership is now per bounded context; `bank-persistence-service` is the Integration Operations/Evidence context, not a platform-wide gateway |
| — | Integration service owns no Flyway/JPA | Accepted — **stands** under `ADR-019` | Any persistence change in the integration service. The rule is unchanged; only its old rationale ("persistence is platform-common") retired. `1sb-integration-service` still reaches its job store over HTTP |
| — | Orchestration first, LOB handler second (`QuoteService` → `TermQuoteHandler`) | Accepted | LOB expansion design |
| — | Flutter communicates only with the workforce BFF | Accepted | Any direct-IdP proposal |
| — | Flutter never receives OAuth tokens (token-hiding session) | Accepted | Session design |
| — | Keycloak behind `identity-provider-adapter-service`; provider-neutral | Accepted | Provider-specific code placement |
| — | `identity-authorization-service` is the business source of truth for authorization | Accepted | Any proposal to use IdP roles as business roles |
| — | Authorization is default-deny, RBAC + ABAC + resource relationships | Accepted | Policy design |
| — | Maker-checker for bulk and privileged identity changes | Accepted | Admin flows |
| — | Production IdP decision deferred behind the adapter | Accepted (deferral) | Do not re-open before WS-2 Phase 2 |
| ADR-001 | IaC on Terraform targeting AWS `ap-south-1`; Render.com is dev-preview only and never a PII path | Proposed | Any proposal to run regulated data outside AWS India, or to treat Render as an environment |
| ADR-002 | Workstream topology: WS-3 is the platform, WS-1 a supplier, WS-2 an enabler | Proposed (`A4_HUMAN_REQUIRED`) | Any proposal to grow WS-1 into the platform or register a fourth workstream for the client |
| ADR-003 | No stage passes S08 until the application carries the same enforcement the governance documents already have | Proposed | Any request to close GATE-S08 on partial enforcement |
| ADR-004 | Two R0 actors: the Bank RM is the certified Specified Person and SP is a certification attribute on that principal, not an actor or a channel; the Insurance Partner Representative is assist-only, gated on completed need analysis and suitability, and insurer-scoped at the persistence layer | Proposed (`A3_JOINT_REVIEW`) | Any proposal to add an actor type, to grant a partner a regulated action, to move the accountable SP, or to scope partner reads above the query layer |
| ADR-005 | The opportunity is the single origination point and only a `BANK_RM` may create one; context #5 moves into R0 Wave 1 | Proposed | Any second origination path — BFF-implicit, campaign, self-service or partner — before R1 |
| ADR-006 | `lob` is a first-class, non-null dimension on every entity, configuration record, audit event and authorization request from release 1; vocabulary frozen at `LIFE` / `HEALTH` / `GENERAL`; `lob` and `productClass` are separate | Proposed | Any nullable or defaulted `lob`, any product class in a `lob` column, and any proposal to fork a per-LOB platform |
| ADR-007 | The configuration layer ships in R0 — LOB-partitioned, append-only, versioned, effective-dated, seeded — independently of any admin UI; no business branch on an insurer, product, LOB or channel literal and no compiled-in fallback | Proposed | Any hardcoded product/insurer behaviour, any rule change delivered as a deployment, and any proposal to defer the layer until its UI is funded |
| ADR-008 | Data ownership is the invariant; physical cluster topology is an evidence-led decision. R0 runs one Aurora cluster with a schema per bounded context, and the first physical split follows the LOB-cell / shared-platform seam | Proposed (`A3_JOINT_REVIEW`) | Any proposal for a cluster per service, any cross-schema grant, and any claim that ownership and physical topology are the same decision |
| ADR-009 | Hybrid bank connectivity is provisioned in R0: a Transit Gateway hub with per-environment route tables, Site-to-Site VPN from day one and Direct Connect as the primary path when the circuit lands. `uat` and `prod` may not use CBS or Bank AD stubs | Proposed (`A3_JOINT_REVIEW`) | Any journey evidenced against a stub outside `dev`; any VPC-to-VPC peering; any bank prefix advertised into more than one environment's route table |
| ADR-010 | All egress and inter-VPC traffic is inspected by AWS Network Firewall in a per-environment inspection VPC, and the 1SB/PG-allowlisted Elastic IPs live there | Proposed (`A3_JOINT_REVIEW`) | Any workload route table with a default route that is not the Transit Gateway; any NAT gateway in a workload VPC; any Elastic IP published from the pre-2026-08-24 design; TLS inspection of a mutually authenticated provider session |
| ADR-011 | A managed cache tier (ElastiCache for Valkey) is provisioned in R0 for BFF sessions, an L2 read-through layer and rate-limit counters — and is never a system of record, never the idempotency store, and never a way to serve configuration past TTL | Proposed (`A3_JOINT_REVIEW`) | Any idempotency or evidence write to a cache; any shared key namespace; any proposal to degrade `S-21` into a stale-read fallback; re-opening the session-store choice |
| ADR-012 | Amazon MSK is the R0 event backbone and the transactional outbox remains its source of truth. **No regulatory evidence exists only in a topic** | Proposed (`A3_JOINT_REVIEW`) | Any direct publish that replaces the outbox; any audit claim satisfied by a topic read or an offset commit; any consumer that is not idempotent on `eventId`; MSK Replicator as a DR mechanism |
| ADR-013 | Amazon OpenSearch is the R0 operational search and log-analytics pipe, and holds no regulatory evidence | Proposed (`A3_JOINT_REVIEW`) | Any gate, audit or regulatory query answered from an index; any business search served from it; any index without a lifecycle policy; the analytics warehouse arriving under its name |
| ADR-014 | R0: Lead is the spoken/primary name; working inbox archives after sold; 7-year SoT is Payment/Policy history; off-platform Policy ingest; admin UI and MIS in R0 on an isolated path; issuanceMode STP/NON_STP/INSTA | Proposed (`A3_JOINT_REVIEW`) | Calling the context Opportunity in RM-facing copy; using Lead as the 7-year bag; MIS `lead.create`; admin/MIS on the Lead writer; STP/Insta skipping hard gates |
| ADR-015 | One NIP-APP Flutter client (web + APK + IPA); ns:edge is nip-web + #2 NIP BFF only; RM / IPR / admin / ops are roles; Play Store + App Store + EKS | Proposed (`A3_JOINT_REVIEW`) | A second admin/ops app or hostname; RM-named or admin-named pods in ns:edge; MDM-only as the default distribution |
| ADR-016 | Enterprise perimeter, integration and delivery baseline: Cloudflare (Edge CDN/DDoS), F5 BIG-IP / WAF (Bank Policy), External ALB before API Gateway, EBS (Enterprise Service Bus) for Core Banking (CBS / CIF), GitLab CI/CD, Terraform IaC, CloudTrail + CloudWatch | Proposed (`A3_JOINT_REVIEW`) | Bypassing bank enterprise perimeter; direct database or unmanaged point-to-point connections to CBS; omitting CloudTrail management auditability; manual console drifts |
| ADR-017 | One platform error contract: every error carries code / service / layer / category plus origin; a registry seeded from journey-execution 04 decides status, wording, retryability, audit and runbook once; one incidentId per failure across every hop; safe public rendering vs full diagnostic, with the BFF (L4) as the redaction boundary; one `bank.error.count` series; additive only | Proposed (`A3_JOINT_REVIEW`) | Returning an upstream body or internal route to a caller; wording an error at a throw site instead of the registry; re-wrapping a dependency failure as `INTERNAL_ERROR`; emitting a diagnostic past L4; renaming or removing an existing `ErrorCodes` value (G9 — that is T4); a metric tag that is a message, identifier or path |
| ADR-018 | The AIGEM governance model, registers and agent context tooling (`docs/`, `scripts/{governance,context,lifecycle}`, `AGENTS.md`, `CLAUDE.md`) live in a dedicated ninth GitLab project `governance/platform-governance`, not in `product/backend` and not split across repositories | Proposed — internal position `RECOMMENDED`, **bank Appendix C exception outstanding** | Placing the governance tree in an application repository; splitting `DOC-MAP.yaml` / `context-load.py` / `FreshnessCheck` across repositories; creating the project before the bank exception is accepted (`C-ARC-2`) |
| ADR-019 | Persistence ownership is **per bounded context** — each owns its write model, schema, credentials, Flyway history and repository layer. R0 may use one Aurora cluster but with separate schemas and no cross-schema grants. `bank-persistence-service` is **not** a platform-wide gateway and may survive only as the Integration Operations/Evidence context; Customer, Lead, Consent, Suitability, Catalogue, Quotation, Proposal, business Payment, Policy and Journey never persist through it. Table allocation is an independently reviewed **S09** migration, after the GitLab cutover | **APPROVED** 2026-08-29 (`CR-015` Option B) · supersedes "Persistence is platform-common" | Any business context persisting through `bank-persistence-service`; any cross-schema grant or FK; any shared write model; performing the allocation inside the CR-014 migration window; a second audit database |
| ADR-020 | GitLab is the only git source of truth. The first commit in each receiving project is an **orphan import** of the current tree under a company identity — personal-forge history, authors, trailers and GitHub merge subjects are not imported. Any personal GitHub / Cursor workbench may contribute only by **file-level one-way import**. Git-object sync is forbidden | **APPROVED** 2026-08-31 (`CR-017`) · supersedes `CR-014` constraint 2 | History-preserving `filter-repo` push; GitLab pull-mirroring from GitHub; `git fetch` between the sandbox and GitLab; a GitLab author using a personal or AI-vendor noreply identity; claiming the import was created on a company laptop |

> ADR IDs are assigned by the architecture decision log. New architectural decisions arising
> from AIGEM triage are raised there and indexed here.

### 1.1 Database decisions (Aarti)

| ID | Decision | Status | Constrains |
|----|----------|--------|------------|
| DB-DEC-0001 | R0 physical model: one Aurora cluster, schema per context, PostgreSQL SoR for every transactional context; no CRUD stored procedures; `lob` is an index prefix not a partition key | AI-DRAFTED (`APPROVED_WITH_OBSERVATIONS`) — human Aarti signature outstanding | A second engine for Journey/Quote/Audit at R0; CRUD stored procedures; declarative `lob` partitions at R0; extending `bank-persistence-service` to business contexts |
| DB-DEC-0002 | DATA-001 pack is **not** aligned to CR-013: need Lead archive, `issuance_mode`, Policy ingest + nullable `lead_id`, `stateHistory`, isolated MIS path, per-schema outbox | AI-DRAFTED (`CHANGES_REQUIRED`, `D1`) — schema delta is `DATA-002`; not implemented in the raise turn | Treating DATA-001 as sufficient for W1/W3/W4; second cluster / S08 isolation service; off-platform `lead.create`; purge before restore test |

Full records: [`DB-DEC-0001`](../../platform/data-architecture/DB-DEC-0001-r0-physical-model.md) · [`DB-DEC-0002`](../../platform/data-architecture/DB-DEC-0002-cr013-alignment-review.md).

## 2. Governance decisions

| ID | Date | Decision | Rationale | Decided by |
|----|------|----------|-----------|------------|
| GOV-001 | 2026-08-07 | Adopt AIGEM 1.0 as the governance model for this repository | Prevent AI scope drift; schedule suggestions rather than implementing or losing them | Pending ratification |
| GOV-002 | 2026-08-07 | Route admitted work to existing backlogs; AIGEM keeps no parallel backlog | Two sources of truth both rot | Pending ratification |
| GOV-003 | 2026-08-07 | Seed the parked backlog from deferred `TECH-DEBT.md` rows; no retrospective triage | Backfilling costs days and teaches nothing ([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)) | Pending ratification |
| GOV-004 | 2026-08-10 | **Ratify the current-state snapshot**: WS-1 at Phase 4 (Hardening), WS-2 at IAM Phase 1, with the scope and standing constraints as recorded | Reconstructed from `ACTION-PLAN.md`, phase `STATUS.md` files, `TECH-DEBT.md` and git history; reviewed and accepted | **Mahesh (Solution Architect), 2026-08-10** — PO counter-signature outstanding |
| GOV-005 | 2026-08-14 | Consolidate the architecture role into **Mahesh — Principal Insurance Platform Architect** as the single Board 1 persona, with modular authority/review files and a reciprocal Mahesh↔Shailja protocol | Avoid two overlapping architect identities while retaining deep architecture reasoning, evidence, compliance collaboration and human escalation | **Proposed by Mahesh** via CR-002 — Product counter-signature outstanding |
| GOV-006 | 2026-08-14 | Propose assigning existing **AIGEM R10 / Board 7 Operations** to **Shivanshi — Principal Insurance Platform SRE / Reliability Engineering Head** and mature the existing SRE capability | Preserve the original Operations controls while adding insurance/bancassurance domain intelligence, platform/CI-CD, observability, incidents/DR, business-aware capacity/scaling and developer enablement without creating a second SRE role | **Proposed by Mahesh** via CR-008 — required governance ratification pending |
| GOV-007 | 2026-08-14 | **Recalibrate the framework for flow**: T4 triggers become a change test (RG-5/RG-6); `GOV` work is queued and counted (BR-4/GC-1); board response clock (RG-7); approvals expire at 30 days or on changed context (RG-8); R12 may force a decision's timing but never its content (PA-1); binding-veto deadlock gets a named human tie-breaker (PA-2); persona roster closed at nine (CC-2/CC-3); gate criteria closed per week becomes the headline metric (GM-1); freshness accepts a reviewed-no-change acknowledgement (FR-1) | Measured: 0 of 7 and 0 of 6 gate exit criteria closed, 61 consecutive commits with no product code, docs-to-code ratio 2.10, one suggestion processed — while every mechanical check reported healthy. The framework was consuming the delivery capacity it exists to protect. No board, veto, jurisdiction or mandatory human sign-off is changed | **APPROVED 2026-08-14** — Mahesh / Architect (R2), repository owner, in full including A1 and B4. R1/R12 and the R8/R9 positions on A1/B4 authorised by R2; see CR-009 §9.1 — Deepali's and Shailja's independent verdicts are not separately recorded and their veto over A1/B4 survives |
| GOV-008 | 2026-08-16 | Add the **Principal Insurance Platform Business Analyst** as the functional AI reasoning persona for existing **R11 — Business Analyst / Product delegate**, with a modular bancassurance package and cross-persona references | Mature R11 from AC-only checking into end-to-end process, requirement, rule, information/state, exception, acceptance and traceability analysis while preserving Rajal and every specialist's authority; no application/runtime change, new board, AIGEM role or named-human roster seat | **DECIDED by Mahesh / repository owner via direct instruction, 2026-08-16 — ADMIT-BYPASS**; any future human naming or authority expansion remains separately governed |

## 3. Change requests

| ID | Date | Type | Summary | Decision | Approvers |
|----|------|------|---------|----------|-----------|
| CR-001 | 2026-08-10 | STAGE | Add exit criterion **4.7** (coverage gates green; QA-001 closed or waived with expiry) to the WS-1 Phase 4 gate | **APPROVED** 2026-08-10 | Mahesh (Solution Architect) — PO + QA Lead counter-signature outstanding |
| CR-002 | 2026-08-14 | GOV | Make Mahesh the single Principal Insurance Platform Architect persona; modularize his authority/review model; retain Shailja as independent Board 6; keep legacy architect path as compatibility-only | **PENDING RATIFICATION** | Mahesh approved preparation on review branch — Product Owner pending |
| CR-008 | 2026-08-14 | GOV | Name Shivanshi as existing R10 / Board 7 Operations persona and mature SRE with insurance-domain, platform, CI/CD, observability, incident/DR, capacity/scaling and developer-experience capability | **PENDING RATIFICATION** | Prepared on Mahesh/user direction — Architecture + Product and any other required governance ratification pending |
| CR-009 | 2026-08-14 | GOV | Recalibrate the framework for flow: T4 change test, `GOV` work queued and counted, board response clock, 30-day approval expiry, R12 decision-forcing, veto-deadlock tie-breaker, persona roster closed at nine, gate-closure headline metric, reviewed-no-change freshness | **APPROVED** 2026-08-14 | Mahesh / Architect (R2), repository owner — in full, including A1 and B4. Provenance of the R8/R9 approvals recorded in CR-009 §9.1 |
| CR-010 | 2026-08-16 | GOV | Portable context module, workstream-aware routing, semantic validation, application CI and proposal-only evidence-driven autopilot | **CANDIDATE** | Implementation authorised by Mahesh/repository owner; formal Product and affected specialist verdicts pending |
| CR-011 | 2026-08-20 | GOV | Mahesh target-state / North Star architecture doctrine: nine persona modules (`09`–`17`), the `VIN-001`/`VIN-002` references and the `hdl.svg` canvas contract | **PENDING RATIFICATION** | Prepared on repository-owner direction — Architecture and Product ratification pending. Indexed here on 2026-08-24; the file existed from 2026-08-20 without a register row |
| CR-012 | 2026-08-24 | ARCH | R0 platform robustness: admit hybrid bank connectivity, centralised egress inspection, a managed cache tier, an event backbone (outbox retained as source of truth) and an operational search pipe into R0 — `ADR-009`…`ADR-013` | **PENDING RATIFICATION** | Raised on repository-owner direction. **Security acceptance (Deepali), Compliance (Shailja), SRE (Shivanshi), Database (Aarti) and Delivery (Kalpana) are required and outstanding**; drafts in [`CR-012/verdicts/`](../change-requests/CR-012/verdicts/README.md). Mandatory human T4 Architecture signature outstanding |
| CR-013 | 2026-08-25 | SCOPE | Stakeholder pull: Lead language, lifecycle/archive, off-platform ingest, admin/MIS, issuance modes and PPHI mapping into R0 — `ADR-014` | **CANDIDATE** — transcribed into scope artefacts | Human T4 Architecture / Security / Compliance outstanding. Compliance conditions in [`CR-013` §5](../change-requests/CR-013-r0-lead-mis-admin-scope.md) |
| CR-016 | 2026-08-29 | CONSTRAINT | GitLab CE cannot enforce required approvals, CODEOWNERS enforcement or protected environments | **APPROVED_WITH_CONDITIONS** 2026-09-01 — Option B now, A as target; owner-relayed | Deepali human T4 on the weaker boundary outstanding. Does **not** gate M5.2 |
| CR-017 | 2026-08-31 | PLAN | Orphan import into GitLab (no personal-forge history) and file-level AI workbench — amends `CR-014` constraint 2; `ADR-020` | **APPROVED_WITH_CONDITIONS** — relayed by `human:Mahesh` | Seven boards, owner-relayed. Not a T4 signature artefact. `AC-6`…`AC-8`. Finding B and `C-CMP-1` still gate the first push |

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
decision:          APPROVED 2026-08-14 — in full, including A1 and B4
authority:         Raised by an AI agent under Rule CC-1, which forbids that agent from approving
                   it. Approved by the repository owner in the Mahesh / Architect (R2)
                   framework-custodian authority, who also authorised the R1, R12 and — for A1
                   and B4 — the R8 and R9 positions.
provenance:        The A1/B4 approvals were given by the R2 authority on behalf of R8 and R9.
                   They are NOT independently recorded Board 4 / Board 6 verdicts and are not
                   represented as such. Deepali and Shailja may record independent verdicts at
                   any time; their veto over A1 and B4 survives this approval, and an objection
                   is treated as a verdict on a live rule rather than a re-litigation requiring
                   new evidence. See CR-009 section 9.1.
scope_of_approval: Framework text only. Every individual change that A1 tiers down remains fully
                   subject to Board 4 and Board 6 binding veto, unchanged by this CR.
next_check:        First GM-1 INTERVENE check falls due 2026-08-28 (two weeks from ratification).
```

### CR-014 — Migrate the platform from personal GitHub to the company GitLab estate

**Date:** 2026-08-29 · **Type:** SCOPE (with `STAGE`, `GOV`, `PLAN`) · **Decision:** **`APPROVED_WITH_CONDITIONS` 2026-08-29** · **Approvers:** seven boards, relayed by `human:Mahesh`
**File:** [`CR-014`](../change-requests/CR-014-gitlab-estate-migration.md) · **Plan:** [`GLM-001`](../../platform/gitlab-migration/GLM-001-migration-plan.md) · **Positions:** [`CR-014/verdicts/`](../change-requests/CR-014/verdicts/README.md)

Adopt the bank's *GitLab Terraform Bootstrap Requirements* v1.0: a Terraform-provisioned estate under
`insurance/bank-insurance`, the monorepo split into `frontend` / `backend` / `platform-governance` as
**orphan first commits** (`CR-017` / `ADR-020`; constraint 2's history preservation is superseded),
five greenfield projects seeded, GitHub Actions re-expressed as reusable GitLab CI components, and
GitLab OIDC + AWS STS replacing static keys. Fourteen improvements accepted by the repository owner.

Required on three grounds ([14 §1](../14-CHANGE_CONTROL.md#1-what-needs-a-change-request)): four `GATE-S08` exit
criteria change evidence platform; the governance files move repository; the approved CI/deployment approach is
replaced. **No criterion is waived or re-worded.**

Approved with the **twenty-nine board conditions** plus five approval conditions: `AC-1` M0.3 Option B, GitHub Actions
green for rollback continuity only, `GATE-S08` stays `OPEN` throughout · `AC-2` the ninth project is conditional on the
bank's written Appendix C acceptance **before M4.3** · `AC-3` Render dev-preview only, no PII or production-like data ·
`AC-4` GitHub read-only at cutover, restorable 14 days, archived only after the custody disposition is approved ·
`AC-5` `bank-persistence-service` migrates unchanged · `AC-6` orphan first commits only (`CR-017`) ·
`AC-7` file-level workbench only · `AC-8` sealed offline bundle.

`C-SEC-1` (clean full-history secret scan) and `C-CMP-1` (data residency) remain **hard blocks on the first push**;
approval authorised the work, not starting it before its gates. The `verdicts/` files remain AI-drafted board inputs,
retained because the approved conditions are defined in them.

### CR-015 — `bank-persistence-service` versus bank baseline §3.3

**Date:** 2026-08-29 · **Type:** CONSTRAINT (`REVERSAL`) · **Decision:** **`APPROVED` — Option B, 2026-08-29** · **ADR:** `ADR-019`
**File:** [`CR-015`](../change-requests/CR-015-shared-persistence-vs-bank-baseline.md)

The bank baseline §3.3 forbids a generic shared persistence service for all domains. This register carries
*"Persistence is platform-common (`bank-persistence-service`), reached over HTTP"* as **Accepted**, with two further
Accepted decisions and one ArchUnit-enforced rule resting on it.

Four options were put **without a recommendation attached**, and the boards chose **B**: persistence ownership per
bounded context, implemented after the migration. Recorded as `ADR-019`, which supersedes the Accepted
"Persistence is platform-common" decision above.

The approval ratifies a physical design that already existed — `DATA-001` already specifies one Aurora cluster, one
schema per bounded context, no cross-schema grants, with `bank_persistence` scoped to the 1SB job store and audit
ingest. What changed is which artefacts are authoritative, not what the target looks like.

**The target is decided; the data migration is not approved.** Aarti's integrity and recovery guarantees (Q4),
including the restore test against RPO 5 min / RTO 30 min, remain outstanding and are a precondition of the S09
allocation. Parked to S09 behind the CR-014 cutover by `AC-5`.

### CR-016 — GitLab Community Edition cannot enforce the approved governance model

**Date:** 2026-08-29 · **Type:** CONSTRAINT · **Decision:** **`APPROVED_WITH_CONDITIONS` 2026-09-01** — Option B now, A as target
**File:** [`CR-016`](../change-requests/CR-016-gitlab-ce-control-model-gap.md) · **Driver:** validated assumption failure (`ASM-012`)
**Recorded:** [`DEC-20260901-01`](../DEC-20260901-01-owner-authorises-unblock-path.md)

The bank confirmed **GitLab Community Edition v19.1.2** at `https://gitlab-ce.au.bank.in/`, `insurance` group id `820`,
container registry available, AWS conventions unconfirmed. `ASM-012`'s pre-computed consequence assumed Premium;
**CE is below Premium**, so required MR approval rules, CODEOWNERS approval enforcement and protected environments are
absent entirely rather than reduced. `RISK-017` **FIRED** at exposure 9; `RISK-023` records the governance-enforcement gap.

Five approved `CR-014` conditions and baseline §6.2, §6.3 and §9.3 are unsatisfiable as written; `GLM-001` M6.3 and M6.6
cannot execute. **`S08-G1`, `G2` and `G9` are unaffected** — "Pipelines must succeed" is a Free-tier merge check, so the
`IMP-4` gate redesign survives — and `S08-G5` is achievable as blocking CI jobs, since the analyzers run in all tiers,
with results as JSON artefacts and no dashboard, MR widget or policy gate.

Owner-relayed 2026-09-01: **Option B** (compensating CI, including `governance-merge-gate`) is the R0
enforcement; **Option A** (`gitlab-ce` → `gitlab-ee`) remains the target; a time-boxed **Option C**
covers the platform-enforcement gap until EE; **Option D** (re-site) is rejected. This does **not**
gate M5.2. Deepali's human Board 4 signature on the weaker boundary remains outstanding.

### CR-017 — Orphan import into GitLab; file-level AI workbench

**Date:** 2026-08-31 · **Type:** PLAN · **Decision:** **`APPROVED_WITH_CONDITIONS` 2026-08-31** · **ADR:** `ADR-020`
**File:** [`CR-017`](../change-requests/CR-017-orphan-import-and-file-workbench.md) · **Decision record:** [`DEC-20260831-01`](../DEC-20260831-01-orphan-import-and-file-workbench.md)

Amends approved `CR-014` constraint 2. GitLab receives an **orphan first commit** per receiving
project under a company git identity. Personal-forge history, Gmail authors, Anthropic/Cursor
trailers and GitHub merge subjects are not imported. Personal GitHub / Cursor may continue as an
AI sandbox by **file-level one-way import only** (`AC-7`); git-object sync remains forbidden (the
`CR-014` rejection of dual-write stands). Original history is a sealed offline bundle (`AC-8`).

Board acceptance relayed by `human:Mahesh` 2026-08-31. Not a T4 signature artefact. Finding B
(`C-SEC-2`) and residency (`C-CMP-1`) still block the first push.

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

## 7. Product decisions — WS-3 realignment increment (CR-010)

Raised by **Rajal / Product (Board 3, R1)** on 2026-08-16 while producing the
[CR-010 Product verdict](../change-requests/CR-010/verdicts/board-3-product-rajal.md),
the [WS-3 charter](../workstreams/WS-3-PLATFORM-CHARTER.md) and the
[retroactive S00–S05 evidence](../../application-lifecycle-bible/evidence/README.md).

Every row is `AI-DRAFTED — mandatory human signature outstanding`. None is
binding until its named human signs; silence does not approve any of them.

| ID | Date | Decision | Rationale | Authority | Status |
|----|------|----------|-----------|-----------|--------|
| DEC-20260816-01 | 2026-08-16 | **Adopt the 16-stage lifecycle completion model** as subordinate to AIGEM (Rule SM-1: AIGEM governs admission, the S-model governs completion; on conflict AIGEM wins) | Three of the four mechanisms behind the current position are completion-model failures, not admission failures. The L4 → S08 + S09 split is the one this repository most needed: collapsing them let both go missing without either being visibly skipped | Rajal (PO2) + Mahesh | **PENDING** — Product verdict drafted, human signature outstanding |
| DEC-20260816-02 | 2026-08-16 | **Register WS-3 — AU Bank Insurance Distribution Platform as the primary workstream**, current stage S08, gate GATE-S08 `BLOCKED`; **re-parent WS-1 as the supplier workstream** for bounded contexts #14 and #15 | Governance evaluates stage fit against a workstream (Rule LC-1). With no workstream for the platform, foundation work triages as out of scope — the framework was correctly excluding the very thing that was missing. WS-1's L7 status remains true *for a component* | Mahesh + Rajal jointly | **TRANSCRIBED 2026-08-16** into `state/CURRENT-STATE.yaml` by the orchestrator; human ratification of CR-010 outstanding |
| DEC-20260816-03 | 2026-08-16 | **R0 is assisted-first.** DIY revisits at R1; hybrid at R2. Supersedes the Day-1 three-journey framing in `R0-SCOPE.md` v0.3 §2 A2 and D-002 **for R0 only** | Reopened on two material triggers under [authority §8](../../context/roles/principal-insurance-platform-product-owner/03-authority-and-decision-rights.md#8-existing-decision-protection): scope/stage change and new material cost. Three journeys across sixteen missing bounded contexts with no engineering foundation and no UI is not a deliverable R0. **Sequencing change, not scope reduction** — both journeys keep named revisit triggers. D-002's original text is preserved, not overwritten | Rajal (PO1/PO2 — Product scope) | **DECIDED by Product**; `R0-SCOPE.md` v0.4 republish outstanding (condition C4) |
| DEC-20260816-04 | 2026-08-16 | **GAP-023 (self-service and hybrid journey detail) is re-scoped from an R0 discovery gap to an R1 entry condition** | Resolved by scope rather than by more discovery, as a direct consequence of DEC-20260816-03. Recorded explicitly so it is not read as a quiet deferral | Rajal | **DECIDED by Product** |
| DEC-20260816-05 | 2026-08-16 | **WS-1 Phase 5 (Health → Motor LOB expansion) does not start.** Unfreeze condition: GATE-S08 `PASSED` **and** GATE-S11 `PASSED` for the R0 Term journey | Adding LOBs to a quote path that lacks its lawful suitability gate multiplies a compliance defect across three lines of business. Expansion over an unlawfully-gated path is negative value, not slower value. **Explicitly not stopped:** WS-2 IAM work, WS-1 criteria 4.4 and 4.5, all documentation and rule-pack work | Rajal + Kalpana (PO2) | **PENDING** — Delivery counter-signature outstanding |
| DEC-20260816-06 | 2026-08-16 | **Bind S11 entry on GAP-006 and GAP-007 closure.** Non-waivable by any authority including Product | A P0 label that does not block is not a severity, it is a note. Both gaps were labelled *build freeze* and the quote path was built, delivered and hardened past them. Now enforced at four layers: Rule SM-4, the WS-3 charter's S11 entry condition, Product verdict condition C5, and rule packs that make the gaps closable rather than perpetual | Rajal + Shailja | **BINDING on ratification**; both gaps remain OPEN |
| DEC-20260816-07 | 2026-08-16 | **Consent Rule Pack v1** (`CONSENT-PACK-v1.0`, 38 rules) and **Suitability Rule Pack v1** (`SUITABILITY-PACK-v1.0` / `SUIT-ALGO-LIFE-v1.0`, 48 rules) adopted as the Product-side business behaviour for consent and suitability | Closes the content half of GAP-006 and GAP-007. Product owns journey binding points, evidence fields, failure semantics and reuse policy; **Compliance owns permissibility, statement wording and calibration**, and ten open items are named rather than assumed | Rajal (business behaviour) + **Shailja (permissibility — outstanding)** | **CONTENT-COMPLETE, RATIFICATION-PENDING.** Not closed: S02-G3/G4 require E2 human signature |
| DEC-20260816-08 | 2026-08-16 | **QR-07 — offer ranking is by disclosed customer-relevant basis only.** Commission, insurer commercial arrangement and bank-internal preference are not inputs to ordering, and no field exists that could make them one | A bank distributing multiple insurers under a corporate agency licence, ordering offers by anything else, is a mis-selling exposure. Stated as a Product decision now so it is never a later feature request | Rajal; **Shailja asked to confirm it is an obligation, not only a preference (S04-OPEN-08)** | **DECIDED by Product**, Compliance confirmation outstanding |
| DEC-20260816-09 | 2026-08-16 | **`FRI-001` — Foundation Recovery Increment** defined as a two-tranche funding line: S08 + S09, 8–10 weeks, tranche 2 released on GATE-S08 G1/G2/G5 `MET`, escalation if G1 is not `MET` by week 4. Classified **strategic foundation + regulatory mandatory** | An open-ended foundation budget becomes indefinite; a single-tranche budget gets cut at week five. The classification is load-bearing: as revenue-generating alone, foundation work reads as pure cost and loses every prioritisation argument | Executive Sponsor — **GAP-010, no named person exists** | **BLOCKED** — a funding line with no approver is a proposal |
| DEC-20260816-10 | 2026-08-16 | **Nineteen technical enablers made visible** as owned, gate-bound, tranche-funded backlog items; S04-VT-06 moves FAIL → PASS | Foundation treated as overhead is foundation never scheduled — the direct mechanism by which S08 and S09 went missing. Sizing is Kalpana's and outstanding | Rajal (visibility) + Kalpana (sizing) | **DECIDED by Product**; sizing outstanding (S04-OPEN-04) |
| DEC-20260816-11 | 2026-08-16 | **No new work type is created by CR-010.** WS-3 routing is closed over the same sixteen canonical types in `06-WORK_CLASSIFICATION §2`, using only paths that exist today | UI/UX, IaC and pipeline work gets a legitimate home through destinations, not through new classification values. `ci-checks.py` asserts the closure, and changing the roster is a CR against Architecture's document, not Product's call | Rajal (proposal) + Mahesh | **TRANSCRIBED 2026-08-16** (condition C8) |
| DEC-20260816-12 | 2026-08-16 | **Build the RM Workspace Flutter application** at `apps/rm-workspace-app/`, scoped to the R0 assisted Term journey, with the three non-negotiable controls enforced structurally: suitability hard-gate, consent gate, payment device isolation | Every journey in the requirement set terminated at an interface that did not exist ([position assessment §3.3](../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md)). The guards are enforced by capability tokens with library-private constructors, route guards that run before screen construction, and — for payment — by the absence of any interface method or widget capable of taking an instrument | Rajal (Product scope) + Mahesh (architecture of the seam) | **DELIVERED as the interface half.** 105 tests pass; `flutter build web` succeeds. **Not S11 evidence** — no backend service exists |

### Decisions this increment deliberately did **not** take

| Not decided | Why | Owner |
|---|---|---|
| Whether the consent and suitability packs are regulatorily sufficient | Product does not own regulatory permissibility | Shailja |
| Whether the absent suitability gate on the delivered quote path is an acceptable residual risk | Material risk acceptance is reserved to a named human | Shailja + a named human risk owner |
| The R0 insurer, product and eligibility values | Commercial facts Product does not hold, and will not invent | Bancassurance |
| Payback period and funding envelope in currency | Requires blended cost per delivery week and a premium/commission baseline; both UNKNOWN with named owners | Kalpana + Bancassurance Finance |
| The named executive sponsor | GAP-010. Writing a name to make a gate green would be the most damaging fabrication available in this exercise | Rajal → Bancassurance leadership |

---

## 8. Lead-domain persona consensus — 2026-08-25

Single file: [`DEC-20260825-01-lead-domain-decisions.md`](../DEC-20260825-01-lead-domain-decisions.md).

Raised after the parked triage (`SUG-20260825-lt1` / `of1` / `st1` / `pp1` / `wl1`) when the human authorised every persona card to **decide**, not only to park. Recorded as `SUG-20260825-df1` `ADMIT-BYPASS`.

Every row is `AI-DRAFTED — mandatory human signature outstanding`. This is not a T4 Architecture, Security or Risk & Compliance sign-off. It does not edit `current_phase` or `stage_status`. `CR-013` did transcribe `current_scope` (admin/MIS in; campaign/bulk Lead create remains out).

| ID | Date | Decision | Rationale | Authority | Status |
|----|------|----------|-----------|-----------|--------|
| DEC-20260825-01 | 2026-08-25 | **Seven locked design decisions** (D1–D7 in the file), **pulled into R0** by `CR-013` / `ADR-014`: Lead language; archive working inbox; off-platform Policy ingest; R0 admin/MIS on isolated path; OLTP isolation; issuanceMode; PPHI condition | Stakeholder: R0 now, nothing parked, compliance calls only | All ten persona cards; Shailja conditions in CR-013 §5 | **AI-DRAFTED.** Human T4 outstanding. Build **ADMITTED** |

---

## 9. Phase M0 migration decisions — 2026-08-29

**File:** [`DEC-20260829-01`](../DEC-20260829-01-m0-migration-decisions.md) · **Status:** **`APPROVED` 2026-08-29**, relayed by `human:Mahesh` and recorded
**Origin:** human:Mahesh — *accept the improvements, activate every persona, start Phase M0, take the decisions with mutual discussion*

| ID | Decision | Owner | Status |
|----|----------|-------|--------|
| M0.3 | Re-evidence `S08-G1/G2/G5/G9` on GitLab. GitHub Actions is kept green **for rollback continuity only**. `GATE-S08` remains `OPEN` throughout the migration | Amit + boards | **`APPROVED`** (`AC-1`) |
| M0.4 | `governance/platform-governance` as a ninth project (`ADR-018`) | Mahesh → bank authority | **`APPROVED`, conditional** (`AC-2`) — bank Appendix C acceptance required **before M4.3**; until then M4.3 creates eight projects |
| M0.6 | Render survives as a dev-preview demo target — no PII, no real premium or quote values, no production-like data; retired only after EKS demonstrates equivalent deployment capability | Shivanshi + Kalpana | **`APPROVED`** (`AC-3`), bound by `C-SEC-8` and `C-CMP-5` |

**Two findings the board round produced that `GLM-001` did not contain:**

- `CMP-F01` → **IMP-14** — data residency is unresolved. `GLM-001` M1.2 asks for the GitLab URL, version and
  edition but never where the instance and its storage physically are, and the migration proceeds identically
  either way. The standing constraint forbids regulated data, backups, logs **or archives** outside AWS India
  regions. Capable of `R0`; can invalidate the destination rather than the schedule. `RISK-021` · `ASM-022`.
- `OPS-F04` — Shivanshi corrected her own plan: archiving the GitHub origin at cutover +24 h is shorter than the
  rollback-validation window it follows. Revised to read-only at cutover, restorable for 14 days, archived only
  once `C-CMP-4` names the disposition.

---

## 10. M3 readiness — seven board recommendations, 2026-08-29

**File:** [`DEC-20260829-02`](../DEC-20260829-02-m3-readiness-board-pack.md) · **Status:** `AI-DRAFTED — RECOMMENDATIONS`, nothing approved
**Origin:** repository-owner request to close M0/M1/M2 and start M3

**Headline recommendation:** do **not** serialise M3 behind M1 and M2. M0 is closed; M1 has no closure event
available to this team (7 of 12 inputs are the bank's); M2 closes on a human key rotation and a human image
review. Measured against `GLM-001` §4, exactly one constraint reaches M3 — `M1.6 ──► M3.3` — gating one task of
eleven. ~29 of M3's ~31 agent-hours are available immediately.

**New finding:** `SEC-F07` / `IMP-15` / `RISK-027` — **bootstrap Terraform state must not be GitLab-managed.**
The bootstrap state controls the estate; stored inside it, an apply that damages the estate destroys its own
recovery path, and it satisfies none of `C-SEC-6`'s three requirements. `M1.6` splits in two, and only the
bootstrap half blocks M3.3.

**Also recorded:** `C-ARC-6` (proposed) — no M3 module may be omitted because CE cannot apply it; unavailable
capabilities are declared and skipped, not deleted, so a licence upgrade is a flag change rather than a redesign.
Eight instance checks are listed that would close more open questions in thirty minutes than a week of waiting.

Nine recommendations `R1`–`R9` with named owners. No decision is recorded; each remains with its owner.
