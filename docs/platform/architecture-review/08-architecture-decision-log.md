# 08 — Architecture Decision Log (this review)

**ID scheme:** `ARCH-xxx` — deliberately distinct from the business/scope `D-xxx` and process `DOC-xxx` IDs in `docs/au-bank-insurance-platform/DECISION-LOG.md`, so this technology-layer log never collides with that business-layer one. Cite `D-xxx`/`DOC-xxx` for business scope; cite `ARCH-xxx` for platform architecture/technology choices.

**Status legend:** `Proposed` = this review's recommendation, not yet ratified by an architecture review board / PO · `Confirms` = restates an already-accepted decision from prior docs, unchanged.

| ID | Decision | Status | Rationale | Supersedes / relates to |
|----|----------|--------|-----------|--------------------------|
| ARCH-001 | Target cloud is **AWS only**; no multi-cloud abstraction | Proposed | Explicit constraint for this review | — |
| ARCH-002 | Compute substrate is **Amazon EKS** for every microservice; elasticity via Karpenter + HPA + KEDA | Proposed | Explicit constraint for this review | — |
| ARCH-003 | Target-state platform = **~16 domain-aligned microservices** + 2 edge BFFs + 1 routing layer, sequenced across 4 delivery phases (P0–P3), not built simultaneously | Proposed | Capability map defines domains, not service count (`knowledge-base/03-capability-map.md` PO note); this review makes that call | See [02](./02-target-microservices-architecture.md) |
| ARCH-004 | **Database-per-service** for every business-domain service; the existing `bank-persistence-service` shared-HTTP-store pattern is scoped **only** to the integration job/correlation store and audit ingestion — not extended to Customer/Lead/Consent/Suitability/Catalogue/Payment/Policy/etc. | Proposed (amends prior pattern) | A platform-wide shared persistence service becomes a single coupling/failure point once 10+ business domains exist; the pattern was designed for two closely-related consumers, not the whole platform | Amends `docs/1sb-insurance-integration/architecture/bank-persistence-service.md` scope, does not delete it |
| ARCH-005 | **Journey Orchestration** is a first-class microservice owning the cross-domain journey state machine | Proposed (new service, not previously named) | Someone must own `Journey { stage, externalRefs, partySnapshot }` (`canonical-model/contexts.md` §8) across domains or every BFF reimplements it, breaking replaceability | New; builds on the Journey aggregate already defined in the 1SB research pack |
| ARCH-006 | **1SB Adapter** (existing `1sb-integration-service`) is retained as-is and placed behind a new **Integration Hub** routing layer; no rewrite | Confirms + extends | The service is already well-designed (hexagonal, SOLID/DRY/KISS, Case-2 pattern) and explicitly scoped as a Phase-A adapter slice in `knowledge-base/08-integration-strategy.md` | Confirms `docs/1sb-insurance-integration/architecture/replaceable-middleware.md` and `08-integration-strategy.md` |
| ARCH-007 | Sync at every point a human is waiting in-session; async (Kafka/SQS/SNS) for every cross-domain side effect (audit, notification, reporting) | Proposed | Generalizes the already-accepted "sync API, async inside" rule from the 1SB adapter (Domain rule 3) to the whole platform | Confirms and extends `1sb-integration-service-architecture.md` §1 |
| ARCH-008 | Shared cross-cutting libraries (`bank-common-error`, `-security`, `-audit`, `-idempotency`, `-observability`) remain the reuse mechanism for cross-cutting concerns; business logic is never extracted into a shared library | Confirms | Already-accepted decision (D13 in `service-ssot/00-po-architect-design-session.md`); this review extends the same libraries platform-wide rather than introducing a second convention | Confirms existing decision |
| ARCH-009 | Primary AWS region `ap-south-1` (Mumbai), DR in `ap-south-2` (Hyderabad) | Proposed, **pending compliance confirmation** | Data residency is an explicitly open item in `DECISION-LOG.md`; this is this review's working assumption, not a substitute for that sign-off | Flags open item from `docs/au-bank-insurance-platform/DECISION-LOG.md` |
| ARCH-010 | All compliance-sensitive behavior (consent rules, retention periods, masking policy) is configuration-driven via Administration & Config, never hardcoded per service | Proposed | Multiple compliance questions (D-008, D-011, and the PII/retention/residency items) are explicitly still pending; config-first absorbs the eventual answer without a re-architecture | Directly implements D-014's "configurable policy-driven controls" principle at the technology layer |
| ARCH-018 | Workforce authentication is isolated behind a provider-neutral adapter; **private Keycloak is the initial implementation**, while Cognito or another standards-compliant IdP remains replaceable | Accepted | The bank AD protocol and final provider are not yet confirmed; OIDC/SAML/LDAP differences must not leak into Flutter, BFF contracts, or business authorization | Supersedes the Cognito-specific recommendation in [04](./04-aws-infrastructure-architecture.md) and [06](./06-security-compliance-and-nfrs.md) |
| ARCH-019 | Flutter uses a **token-hiding BFF** and never receives provider access or refresh tokens | Accepted | Server-side session custody reduces token exfiltration risk and isolates Flutter from provider migration | See `docs/platform/authentication-authorization/README.md` |
| ARCH-020 | Authorization uses **default-deny RBAC + ABAC + relationship rules** with suspension and explicit denial taking precedence over grants | Accepted | Roles alone cannot safely express insurer tenancy, multi-branch scope, hierarchy, assignment, sharing, or certification gates | See `docs/platform/authentication-authorization/README.md` |
| ARCH-021 | Phase 1 workforce identity comprises three custom services: `workforce-access-bff`, `identity-provider-adapter-service`, and `identity-authorization-service`; Keycloak is a separate infrastructure workload | Accepted | Separates public session handling, provider-specific integration, and business authorization/data ownership | See `docs/platform/authentication-authorization/README.md` |
| ARCH-022 | Partner identities are created in Identity & Access and provisioned to the IdP after maker-checker approval; RM certification is sourced from AD, while insurer-representative certification is optional and admin-uploaded in Phase 1 | Accepted | Preserves a provider-independent business source of truth and supports later mandatory partner qualification without redesign | See `docs/platform/authentication-authorization/README.md` |

## What this review deliberately did **not** decide

- Exact consent sequencing/wording (compliance R&D dependency, D-011 — pending).
- Exact IRDAI/RBI regulatory control mapping (pending; Audit & Compliance is built to capture a superset of evidence so it can absorb the answer).
- Insurance advisor/agent identity model specifics (D-008 — pending; kept behind an interface so the model can change independently).
- Final PII/audit retention periods and data residency confirmation (pending; see [06](./06-security-compliance-and-nfrs.md)).
- Branch kiosk journey (explicitly deferred pending a business decision per `DECISION-LOG.md`).

These stay open on purpose — an architecture review shouldn't quietly resolve business/compliance decisions that the business SSOT has flagged as pending sponsor/compliance sign-off.

---

# Platform architecture decision records — `ADR-xxx`

**Added:** 2026-08-16 under [CR-010](../../governance/change-requests/CR-010-context-module-and-safe-autopilot.md) · **Author:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)

## Why a second identifier namespace, and how it relates to `ARCH-xxx`

`ARCH-xxx` above is this review's technology-layer decision **log** — a compact table of decisions
this architecture review took or restated, most of them still `Proposed`. It stays exactly as it is.

`ADR-xxx` is the **sequential ADR namespace declared in
[`state/CURRENT-STATE.yaml`](../../governance/state/CURRENT-STATE.yaml) `id_allocation`** (`ADR: 1`,
first unused number `ADR-001`). It is used for decisions that are **programme-binding rather than
review-scoped**: decisions that create a durable constraint, are cited by a change request, and
carry the full record the
[architecture decision framework §8](../../context/roles/mahesh-principal-insurance-platform-architect/04-architecture-decision-framework.md)
requires — authority class, alternatives, consequences, reversibility, revisit trigger and
approvals.

The relationship, stated once so it never has to be guessed:

| Namespace | Scope | Form | Allocation |
|---|---|---|---|
| `D-xxx` / `DOC-xxx` | Business scope decisions | Table row | `docs/au-bank-insurance-platform/DECISION-LOG.md` |
| `ARCH-xxx` | This review's technology-layer decisions | Table row | This file, above |
| **`ADR-xxx`** | **Programme-binding architecture decisions** | **Full record** | **`CURRENT-STATE.yaml` `id_allocation.sequential.ADR`** |

An `ARCH-xxx` row may be **promoted** to an `ADR-xxx` record when it becomes binding. ADR-001 below
does exactly that for ARCH-009. Promotion supersedes nothing; both entries remain readable.

> `id_allocation.sequential.ADR` must be advanced to `4` when CR-010 is ratified. That file is
> orchestrator-owned this increment; this note is the request, not the edit.

---

## ADR-001 — Infrastructure as code on Terraform, targeting AWS `ap-south-1`; Render.com is dev-preview only

```yaml
id: ADR-001
status: PROPOSED
problem: >
  The platform has no infrastructure as code and no defined environments. What is deployed is a
  single Render.com starter-plan web service running two JVMs in one container over localhost.
  The architecture specifies AWS EKS, Aurora, DynamoDB, KMS and India-region residency. The gap
  between them is the whole of stage S09, and the current deployment has an unverified data
  residency position on a platform handling PAN, income and health attributes.
context_stage: "S09 — Platform & Environment Foundation (WS-3), overlapped with S08"
decision: >
  All platform infrastructure is defined as code in Terraform, with versioned reviewed modules,
  remote encrypted locked state, and no console-created production resource. Every resource is
  pinned to ap-south-1 with DR in ap-south-2, enforced by a policy-as-code check that fails the
  plan before apply rather than detecting drift after it. Render.com is retained ONLY as a
  developer preview and demonstration target. It is never a data path for PII, production or
  production-like data, never an environment any gate may cite as evidence, and never a
  destination for a customer, RM or partner journey.
authority_class: A3_JOINT_REVIEW
alternatives:
  - option: "CloudFormation or CDK"
    rejected_because: >
      No advantage over Terraform for this estate, and the team's stated skills and the bank's
      existing tooling do not indicate either. Reversibility is comparable; the decision is not
      load-bearing enough to justify further analysis at this stage.
  - option: "Promote Render.com to the R0 environment and defer AWS"
    rejected_because: >
      Data residency is a licence condition, not a preference. Render's region is not chosen,
      not pinned and not attestable, and the platform cannot provide S3 Object Lock immutability
      for the 7-year IRDAI retention obligation. This option is not available to us.
  - option: "Retain Render.com for UAT only"
    rejected_because: >
      UAT exercises real journeys with realistic data. WS-1 gate criterion 4.3 requires a bank
      caller to exercise quote and proposal against UAT, which means proposal payloads. Those
      carry PII. The same residency and retention argument applies.
  - option: "Delete render.yaml entirely"
    rejected_because: >
      It is a legitimate and useful demo target and costs nothing to keep. The defect is not its
      existence but the absence of a stated boundary, which this ADR supplies.
consequences:
  positive:
    - "Environments become reproducible; S09-VT-01 (destroy and recreate) becomes answerable"
    - "Residency (control C6) becomes a pre-apply gate rather than an audit finding"
    - "7-year Object Lock retention (control C7) becomes implementable"
    - "The demo path is preserved with an explicit, testable boundary"
  negative:
    - "Terraform, state management and policy-as-code are new operational surface for the team"
    - "AWS landing-zone cost begins before the first business journey runs"
    - "Two deployment targets exist during the transition, and the boundary between them must be enforced rather than trusted"
compliance_impact: review-required
security_impact: review-required
reversibility: MEDIUM
revisit_trigger: >
  A bank enterprise standard mandating a different IaC tool; or a regulatory change to the India
  residency obligation; or evidence that a managed platform can satisfy residency, Object Lock
  immutability and KMS CMK ownership under attestation.
approvals:
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Shivanshi / SRE — required (IaC and environments are R10 jurisdiction)"
  - "Deepali / Security — required (residency, key ownership, network boundary)"
  - "Shailja / Compliance — required (residency and retention are licence conditions)"
```

**Enforcement.** FF-08 and FF-09 in
[`ws3-platform/03 §7`](../ws3-platform/03-solution-architecture-r0.md), standing constraint SC-W3-7
in [`ws3-platform/00 §6`](../ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md), and invariant
INV-DAT-01. **Promotes and supersedes the `Proposed, pending compliance confirmation` status of
ARCH-009**, and adds the Render boundary that ARCH-009 did not address.

**Open item this ADR does not resolve.** Whether any customer PII has already passed through the
current Render deployment is a question for Compliance to answer now rather than at audit — the
S09 stage file raises it and it remains unanswered. Recorded as SEC-OPEN-5.

---

## ADR-002 — Workstream topology: WS-3 is the platform; WS-1 is a supplier; WS-2 is an enabler

```yaml
id: ADR-002
status: PROPOSED
problem: >
  CURRENT-STATE.yaml registers two workstreams, WS-1 (1SB Integration) and WS-2 (Workforce Auth).
  Neither is the product. AIGEM evaluates stage fit against a workstream, so platform foundation
  work — CI, IaC, the Consent service, the Flutter application — belongs to no workstream and
  therefore triages as out of scope. The governance model has been correctly and faithfully
  excluding the foundation, because the thing the foundation belongs to was never registered.
  This is GAP-D in the position assessment and it is the root cause of the other four gaps.
context_stage: "Cross-cutting governance; WS-3 registered at S08"
decision: >
  Register WS-3 — AU Bank Insurance Distribution Platform — as the primary workstream at stage
  S08, with GATE-S08 open. Re-parent WS-1 as a SUPPLIER workstream delivering bounded contexts
  #14 Integration Hub and #15 1SB Adapter behind interface IF-1, and WS-2 as an ENABLER
  workstream delivering the workforce half of bounded context #3 behind interface IF-2. WS-1 and
  WS-2 retain their current stages and gates unchanged; re-parenting states what they deliver
  into, not how far along they are. The S08 and S09 foundation is built once in WS-3 and consumed
  by all three workstreams as a paved road (IF-3). The Flutter client is part of WS-3, not a
  fourth workstream.
authority_class: A4_HUMAN_REQUIRED
alternatives:
  - option: "Leave WS-1 as the de-facto programme and grow it into the platform"
    rejected_because: >
      WS-1's scope, authority documents and never-list are correctly scoped to an adapter. Growing
      it would either dilute those constraints or route platform work into a supplier backlog.
      The supplier interface was mistaken for the product once already; this would institutionalise
      that mistake.
  - option: "Register WS-3 at S06 or S07"
    rejected_because: >
      The S06/S07 gaps were documentation-shaped and are closed by this increment. Registering
      three stages behind the code that already exists would put every existing service out of
      scope for maintenance.
  - option: "Register WS-3 at S11 and treat S08/S09 as enabling work inside it"
    rejected_because: >
      S11 entry requires a UI that does not exist, six services that do not exist, and closure of
      two P0 build-freeze compliance gaps. Registering at S11 would legitimise starting sixteen
      bounded contexts with no CI, no environments and no test infrastructure — the original error
      at four times the scale.
  - option: "Register a fourth workstream for the Flutter client"
    rejected_because: >
      The client renders the journey; separating them puts the UI outside the contract it consumes
      and produces exactly the BFF-reimplements-journey-state failure ARCH-005 exists to prevent.
consequences:
  positive:
    - "Foundation work has a legitimate home and stops triaging as out of scope — GAP-D closed"
    - "WS-1 and WS-2 gain a stated consumer, so their interface contracts become explicit (IF-1, IF-2)"
    - "The R0 minimum service set can be stated against a workstream rather than against a diagram"
    - "One pipeline and one IaC estate serve all three workstreams"
  negative:
    - "Three concurrent workstreams need coordinated sequencing — Kalpana's critical path grows"
    - "WS-1 loses its position as the programme's primary lane, which is a real change in emphasis for the team working on it"
    - "Routing, gate evidence and register hygiene all grow by one workstream"
compliance_impact: none
security_impact: none
reversibility: LOW
revisit_trigger: >
  A decision to buy rather than build the distribution platform; or a decision to make the 1SB
  adapter a standalone product with consumers outside this bank.
approvals:
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Rajal / Product — required (workstream scope and objective)"
  - "Kalpana / Delivery — required (sequencing and critical path)"
  - "Accountable human sponsor — required: A4_HUMAN_REQUIRED, this is a material scope and stage decision under change control"
```

**Transcription.** The exact YAML for `CURRENT-STATE.yaml` is in
[`ws3-platform/00 §7`](../ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md). Interface contracts
IF-1, IF-2 and IF-3 are in §3 of the same document.

---

## ADR-003 — No stage passes S08 until the application has the same enforcement the governance documents already have

```yaml
id: ADR-003
status: PROPOSED
problem: >
  Governance markdown, schemas, routing closure, link integrity and state freshness are validated
  on every pull request by four separate checks. Roughly 20,000 lines of Java in a regulated
  financial application have never been built or tested by an automated system. Every "green"
  claim in every phase status document is a human assertion. The rigour was applied where it was
  easy to apply, and the inversion is the clearest single statement of where the foundation went
  missing.
context_stage: "S08 — Engineering Foundation (WS-3)"
decision: >
  Adopt admission rule AR-S08 as a binding architecture constraint: no stage passes S08 until the
  application carries machine enforcement equivalent to what the governance documents already
  carry. Operationally, the fifteen fitness functions enumerated in the R0 solution architecture
  that are marked "from S08" must be EXECUTING AND FAILING BUILDS, demonstrated by a deliberately
  broken pull request per validation tests S08-VT-01 through S08-VT-05. A pipeline that runs but
  blocks nothing is gate-shaped configuration, not a gate. Explicitly: merging an application-CI
  workflow file does not satisfy S08-G1, because S08-G1 requires evidence level E4 — a run
  history — and a workflow file is a mechanism, not evidence.
authority_class: A3_JOINT_REVIEW
alternatives:
  - option: "Rely on code review and the Definition of Done"
    rejected_because: >
      Both already exist and both were satisfied while coverage stayed on an interim floor,
      ArchUnit never executed in CI, and QA-001 sat open at P0. Human review does not scale to a
      constraint that must hold on every commit for seven years of audit history.
  - option: "Enforce gates as warnings first, then make them blocking later"
    rejected_because: >
      A warning that never becomes an error is a metric, not a control. The stage file states it
      precisely: a pipeline that runs but never blocks anything is theatre. The graduated approach
      is available for a NEW rule on an existing baseline; it is not available for the first
      installation of enforcement.
  - option: "Exempt the existing estate and enforce only on new code"
    rejected_because: >
      The existing estate is the code carrying the compliance controls. A baseline exemption on
      static analysis is reasonable and is provided for (S08-E02-S03); a baseline exemption on
      coverage of control paths C1 to C10, on ArchUnit boundaries, or on secret scanning is not.
consequences:
  positive:
    - "Every downstream gate claim converts from assertion to artefact"
    - "The QA-001, TD-007 and TD-014 mechanisms close as a side effect rather than as separate campaigns"
    - "Regression of the governance-versus-application inversion becomes detectable"
    - "WS-1 criteria 4.1, 4.6 and 4.7 become achievable for the first time"
  negative:
    - "The retrofit penalty is real: adding tests and enforcement to code written without them is slower than writing it that way"
    - "Builds will fail on the existing estate before they pass, and that will be uncomfortable"
    - "Feature delivery pauses for the duration of the foundation recovery increment"
compliance_impact: review-required
security_impact: review-required
reversibility: HIGH
revisit_trigger: >
  Evidence that a specific fitness function produces material false positives that cannot be
  narrowed. Retire or narrow that function through a change request; do not weaken the rule.
approvals:
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Amit / Engineering — required (S08 co-owner)"
  - "Swapnali / QA — required (S08 co-owner, block authority)"
  - "Deepali / Security — required (pipeline security gates, block authority)"
  - "Shivanshi / SRE — required (CI/CD platform mechanics)"
```

**Enforcement.** The fitness function list is in
[`ws3-platform/03 §7`](../ws3-platform/03-solution-architecture-r0.md); the S08 architecture
approval conditions AR-S08-1…5 are in
[`ws3-platform/00 §4`](../ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md).

---

## Signature status for ADR-001 — ADR-003

All three records are **`AI-DRAFTED — mandatory human signature outstanding`**. ADR-002 is
`A4_HUMAN_REQUIRED`: it is a material scope and stage decision and an AI simulation of Mahesh must
not finalise it. None of these ADRs is `ACCEPTED` until the approvals listed in each record are
present, and none of them becomes binding merely because CR-010's checks pass or its branch is
mergeable.

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2) · 2026-08-16
