# 08 — Architecture Decision Log (this review)

**ID scheme:** `ARCH-xxx` — deliberately distinct from the business/scope `D-xxx` and process `DOC-xxx` IDs in `docs/au-bank-insurance-platform/DECISION-LOG.md`, so this technology-layer log never collides with that business-layer one. Cite `D-xxx`/`DOC-xxx` for business scope; cite `ARCH-xxx` for platform architecture/technology choices.

**Status legend:** `Proposed` = this review's recommendation, not yet ratified by an architecture review board / PO · `Confirms` = restates an already-accepted decision from prior docs, unchanged.

| ID | Decision | Status | Rationale | Supersedes / relates to |
|----|----------|--------|-----------|--------------------------|
| ARCH-001 | Target cloud is **AWS only**; no multi-cloud abstraction | Proposed | Explicit constraint for this review | — |
| ARCH-002 | Compute substrate is **Amazon EKS** for every microservice; elasticity via Karpenter + HPA + KEDA | Proposed | Explicit constraint for this review | KEDA's precondition — a broker to read lag from — is met in R0 by `ADR-012` |
| ARCH-003 | Target-state platform = **~16 domain-aligned microservices** + 2 edge BFFs + 1 routing layer, sequenced across 4 delivery phases (P0–P3), not built simultaneously | Proposed | Capability map defines domains, not service count (`knowledge-base/03-capability-map.md` PO note); this review makes that call | See [02](./02-target-microservices-architecture.md) |
| ARCH-004 | **Data ownership per service** — one owner per authoritative datum, no cross-service table access, separate credentials and schema ownership per service; the existing `bank-persistence-service` shared-HTTP-store pattern is scoped **only** to the integration job/correlation store and audit ingestion — not extended to Customer/Opportunity/Consent/Suitability/Catalogue/Payment/Policy/etc. **The physical-cluster-per-service half is withdrawn — see `ADR-008`.** | Proposed, qualified by `ADR-008` | A platform-wide shared persistence service becomes a single coupling/failure point once 10+ business domains exist. But ownership and physical topology are different claims, and only the first is a principle (`TI-05`, `VIN-001 §34`) | Amends `docs/1sb-insurance-integration/architecture/bank-persistence-service.md` scope, does not delete it · physical topology decided by `ADR-008` |
| ARCH-005 | **Journey Orchestration** is a first-class microservice owning the cross-domain journey state machine | Proposed (new service, not previously named) | Someone must own `Journey { stage, externalRefs, partySnapshot }` (`canonical-model/contexts.md` §8) across domains or every BFF reimplements it, breaking replaceability | New; builds on the Journey aggregate already defined in the 1SB research pack |
| ARCH-006 | **1SB Adapter** (existing `1sb-integration-service`) is retained as-is and placed behind a new **Integration Hub** routing layer; no rewrite | Confirms + extends | The service is already well-designed (hexagonal, SOLID/DRY/KISS, Case-2 pattern) and explicitly scoped as a Phase-A adapter slice in `knowledge-base/08-integration-strategy.md` | Confirms `docs/1sb-insurance-integration/architecture/replaceable-middleware.md` and `08-integration-strategy.md` |
| ARCH-007 | Sync at every point a human is waiting in-session; async (Kafka/SQS/SNS) for every cross-domain side effect (audit, notification, reporting) | Proposed | Generalizes the already-accepted "sync API, async inside" rule from the 1SB adapter (Domain rule 3) to the whole platform | Confirms and extends `1sb-integration-service-architecture.md` §1 · qualified by `ADR-012`: the broker is R0, and the **transactional outbox in front of it is the source of truth**, not a transitional step |
| ARCH-008 | Shared cross-cutting libraries (`bank-common-error`, `-security`, `-audit`, `-idempotency`, `-observability`) remain the reuse mechanism for cross-cutting concerns; business logic is never extracted into a shared library | Confirms | Already-accepted decision (D13 in `service-ssot/00-po-architect-design-session.md`); this review extends the same libraries platform-wide rather than introducing a second convention | Confirms existing decision |
| ARCH-009 | Primary AWS region `ap-south-1` (Mumbai), DR in `ap-south-2` (Hyderabad) | Proposed, **pending compliance confirmation** | Data residency is an explicitly open item in `DECISION-LOG.md`; this is this review's working assumption, not a substitute for that sign-off | Flags open item from `docs/au-bank-insurance-platform/DECISION-LOG.md` |
| ARCH-010 | All compliance-sensitive behavior (consent rules, retention periods, masking policy) is configuration-driven via Administration & Config, never hardcoded per service | Proposed | Multiple compliance questions (D-008, D-011, and the PII/retention/residency items) are explicitly still pending; config-first absorbs the eventual answer without a re-architecture | Directly implements D-014's "configurable policy-driven controls" principle at the technology layer |
| ARCH-018 | Workforce authentication is isolated behind a provider-neutral adapter; **private Keycloak is the initial implementation**, while Cognito or another standards-compliant IdP remains replaceable | Accepted | The bank AD protocol and final provider are not yet confirmed; OIDC/SAML/LDAP differences must not leak into Flutter, BFF contracts, or business authorization | Supersedes the Cognito-specific recommendation in [04](./04-aws-infrastructure-architecture.md) and [06](./06-security-compliance-and-nfrs.md) |
| ARCH-019 | Flutter uses a **token-hiding BFF** and never receives provider access or refresh tokens | Accepted | Server-side session custody reduces token exfiltration risk and isolates Flutter from provider migration | See `docs/platform/authentication-authorization/README.md` |
| ARCH-020 | Authorization uses **default-deny RBAC + ABAC + relationship rules** with suspension and explicit denial taking precedence over grants | Accepted | Roles alone cannot safely express insurer tenancy, multi-branch scope, hierarchy, assignment, sharing, or certification gates | See `docs/platform/authentication-authorization/README.md` |
| ARCH-021 | Phase 1 workforce identity comprises three custom services: `workforce-access-bff`, `identity-provider-adapter-service`, and `identity-authorization-service`; Keycloak is a separate infrastructure workload | Accepted | Separates public session handling, provider-specific integration, and business authorization/data ownership | See `docs/platform/authentication-authorization/README.md` |
| ARCH-022 | Partner identities are created in Identity & Access and provisioned to the IdP after maker-checker approval; RM certification is sourced from AD, while insurer-representative certification is optional and admin-uploaded in Phase 1 | Accepted | Preserves a provider-independent business source of truth and supports later mandatory partner qualification without redesign | See `docs/platform/authentication-authorization/README.md` |
| ARCH-023 | R0 has **two** on-platform actors — Bank RM and Insurance Partner Representative. **Specified Person is a certification attribute on the RM principal**, not an actor type and not a channel; the R0 actor-type vocabulary is closed at `BANK_RM`, `INSURER_PARTNER_REP`, `SERVICE` | Proposed | A certification modelled as an actor produces two principals and two audit trails for one human, and makes "may assist but may not sell" inexpressible | Promoted to **ADR-004**; supersedes the `CERTIFIED_SP` actor type in `15 §4` |
| ARCH-024 | The **opportunity is the single origination point**, creatable only by a `BANK_RM`; every downstream module consumes it. Context #5 moves from deferred-to-S13 into R0 Wave 1 | Proposed | Reconciles the architecture document with `CURRENT-STATE.yaml` `in_scope`, and removes the second funnel entry a customer-lookup start would have created | Promoted to **ADR-005**; amends the build order in `ws3-platform/03 §3` |
| ARCH-025 | Partner visibility is **gated and insurer-scoped at the persistence layer** — invisible until the RM completes need analysis and suitability, never across `insurer_id`, absent from result sets rather than refused by identifier | Proposed | A control in the service or presentation tier is one direct call away from absent; a `403` on a named id is itself a disclosure | Promoted to **ADR-004**; extends ARCH-020 |
| ARCH-026 | **`lob` is a first-class dimension from release 1** — mandatory and non-null on every business entity, configuration record, audit event and authorization request; vocabulary frozen at `LIFE` / `HEALTH` / `GENERAL`; `lob` and `productClass` are separate | Proposed | Health and General follow R0 on the same template. The dimension is free to carry now and is a migration across every table on the sale path later | Promoted to **ADR-006**; corrects `ws3-platform/02 §4.2`, which recorded `lob = TERM` |
| ARCH-027 | **The configuration layer ships in R0 without an administration UI** — one LOB-partitioned, append-only, effective-dated store with source-controlled seeds and a resolution contract; no business branch on an insurer, product, LOB or channel literal; no compiled-in fallback | Proposed | Extends ARCH-010 from compliance-sensitive behaviour to the whole R0 behaviour surface, and withdraws the earlier trade under which a rule-pack change required a deployment | Promoted to **ADR-007**; supersedes the R0 configuration trade in `ws3-platform/03 §3` |

## What this review deliberately did **not** decide

- Exact consent sequencing/wording (compliance R&D dependency, D-011 — pending).
- Exact IRDAI/RBI regulatory control mapping (pending; Audit & Compliance is built to capture a superset of evidence so it can absorb the answer).
- Insurance advisor/agent identity model specifics (D-008 — pending; kept behind an interface so the model can change independently). *ARCH-023 fixes the R0 actor set and the certification model; it does not resolve D-008's wider advisor/agent question.*
- Where assistance ends and solicitation begins for a non-SP partner employee. ARCH-023 and ARCH-025 build the gate and ship it default-deny; the threshold is Shailja's determination (OPEN-D9).
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

## ADR-004 — Two R0 actors; Specified Person is a certification, not an actor; the partner is assist-only, gated and insurer-scoped

```yaml
id: ADR-004
status: PROPOSED
problem: >
  The R0 design read as a single-actor model with "Certified SP" appearing alongside the RM as
  though it were a separate actor and, in the persona doctrine's authorization contract, a separate
  actorType and channel value. It is neither: the RM *is* the certified Specified Person, and SP is
  an eligibility attribute on that principal. Modelling it as an actor produces two principals for
  one human and two attribution trails for one sale. Separately, the Insurance Partner
  Representative — an insurer's employee who assists the RM or the customer on the platform — had
  no distinct model at all, which left three questions unanswered: what may a non-certified partner
  employee do, what may they see, and how is their activity distinguished from the accountable SP's
  in the record IRDAI reads.
context_stage: "WS-3 at S08; R0 design artefacts unsigned, nothing implemented"
decision: >
  R0 has exactly two on-platform human actors. BANK_RM is the certified Specified Person and the
  accountable party on every record; SP certification is modelled as certification state on the RM
  profile — certificate number, issuing authority, LOB scope, validity window, status — sourced
  from Identity and Access and evaluated at the instant of each regulated action rather than at
  login. INSURER_PARTNER_REP is an insurer's employee, provisioned as a partner-plane principal
  after maker-checker, and is assist-only: no regulated-sales grant at any journey stage, no
  origination right, own-insurer product view and selection, and gated read. The accountable SP on
  a record is written once at origination and is immutable for the record's life; no partner
  action, assignment or handover moves it. Partner reads are gated on the RM having created the
  opportunity and completed need analysis and suitability, and scoped to the partner's own
  insurer_id, both applied as mandatory predicates at the persistence layer rather than in a
  service filter or the UI. Out-of-scope records are absent from result sets, never refused by
  identifier. Every partner action is audited with acting_capacity ASSIST_ONLY, actor_insurer_id
  and assisted_actor_id. The R0 actor-type vocabulary is closed at BANK_RM, INSURER_PARTNER_REP and
  SERVICE; CERTIFIED_SP is removed as an actor type and as a channel value.
authority_class: A3_JOINT_REVIEW
alternatives:
  - option: "Keep CERTIFIED_SP as an actor type for forward compatibility with H1 sales partners"
    rejected_because: >
      It conflates the certification with the principal that holds it. A genuine external certified
      sales partner at H1 is a new actor type in the partner plane that also carries a
      certification; it is not the same thing as the bank RM's SP status. Carrying the wrong
      abstraction now to serve a horizon that has not arrived is how the RM ends up with two
      identities.
  - option: "Enforce partner scoping in the BFF or the service layer"
    rejected_because: >
      Every service re-check is one forgotten predicate away from a cross-insurer disclosure, and
      the failure is silent. Applying it where the query is built means an unscoped read cannot be
      written, which is the difference between a control and a convention (TI-15, ID-17).
  - option: "Return 403 for records outside the partner's scope"
    rejected_because: >
      A refusal that names an identifier confirms that identifier exists. Enumeration of another
      insurer's pipeline is exactly the disclosure the scoping rule exists to prevent.
  - option: "Give the IPR a limited sales grant under RM supervision"
    rejected_because: >
      The IPR is not a Specified Person. A supervised sales grant is still solicitation performed
      by an uncertified party, and no audit design makes that lawful. Assistance and sale must be
      distinguishable in the record, not blended in it.
consequences:
  positive:
    - "One human, one principal, one attribution trail — the solicitation record is single-threaded to one accountable SP"
    - "'May assist but may not sell' becomes expressible, which a URL-shaped or role-only permission model cannot express"
    - "Cross-insurer disclosure becomes structurally hard rather than review-dependent"
    - "Adding the customer, the call centre or an external certified partner later is an authorization change, not an architecture change (JS-08)"
  negative:
    - "The persistence layer gains a mandatory-predicate mechanism that every repository must go through — real engineering cost in W0/W1"
    - "Certification is evaluated per action, not per session; the certification lookup becomes a fail-closed dependency on every regulated path"
    - "The partner surface needs its own product-selection and assistance UX even though it shares every service"
compliance_impact: >
  Material and intended. IRDAI corporate-agency distribution requires attribution to the certified
  Specified Person. This decision makes the accountable SP immutable, makes uncertified assistance
  separately attributable, and denies the uncertified actor every regulated action by default.
  Which assistance actions remain lawful for a non-SP is Shailja's determination, not Architecture's
  — recorded as OPEN-D9 and shipped default-deny until she sets it.
security_impact: >
  Adds a fourth principal class (Partner) with its own realm and grant set, and a non-enumeration
  requirement on the partner read path. Requires Deepali's review.
reversibility: LOW
revisit_trigger: >
  A business decision to admit an externally certified sales partner as an on-platform actor; or a
  compliance determination that widens or narrows the assist-only action set.
approvals:
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Deepali / Security — required (new principal class, trust boundary, non-enumeration)"
  - "Shailja / Compliance — required (the assist-only threshold, OPEN-D9)"
  - "Rajal / Product — required (whether the bank supports the IPR actor at all, 15 section 8)"
```

---

## ADR-005 — The opportunity is the single origination point, and only the RM may create it

```yaml
id: ADR-005
status: PROPOSED
problem: >
  Two things were wrong at once. The build order in ws3-platform/03 deferred context #5 to S13 and
  started journeys from a customer lookup, while CURRENT-STATE.yaml current_scope.in_scope lists
  "Lead service (context #5) — create, resume, status" as R0 scope — an architecture document
  disagreeing with the ratified state file. And starting a journey from a lookup creates a second
  entry into the funnel: a journey with no origination record has no accountable SP, no lob and
  nothing for a conversion metric or an audit reconstruction to hang from.
context_stage: "WS-3 at S08; R0 build order unsigned; no service implemented"
decision: >
  Context #5 is the single origination point for R0 and moves into Wave 1. Its aggregate is the
  opportunity: the record answering why we are contacting this person. Only a BANK_RM principal may
  create one, for a customer inside their own ETB book, carrying a non-null lob covered by the
  creator's SP certification. An INSURER_PARTNER_REP has no create right on any path. Every
  downstream aggregate — journey, suitability assessment, consent grant, quote, proposal, payment,
  policy — carries the originating reference and cannot be created without it. There is no parallel
  origination path in MVP: no BFF-created journey, no quote outside an opportunity, no proposal
  assembled independently. What remains deferred to S13 is sales-management breadth on top of
  origination — campaigns, bulk import, ageing policy — which adds no journey capability.
context_naming: >
  The North Star capability model calls this record the Opportunity (CAP-102). CURRENT-STATE.yaml,
  the BRD and the S03 acceptance criteria call the context Lead and its identifier leadId. The
  architecture documents keep the registered, Product-owned labels and state normatively that they
  denote the opportunity. Re-labelling is Rajal's, is behaviour-neutral, and is recorded as
  OPEN-D10. The identifier is not the point; the single-origination rule is.
naming_resolution: >
  OPEN-D10 CLOSED 2026-08-20 on structure; spoken name superseded 2026-08-25 by
  DEC-20260825-01 D1 / ADR-014. People, Product, UI and architecture primary text
  say **Lead**. Opportunity remains the durable-demand alias only: a renewal, lapse
  recovery, cross-sell or abandoned-journey recovery mints a **new Lead and a new
  Journey**, it does not reopen an archived inbox row. That rule is unchanged.

  What does not change: identifiers and register IDs. leadId, INV-LED-* and
  CAP-102 keep their tokens. RM-only origination, no-campaign and no-auto-create
  stand. A rename that reversed those rules would be a reversal and is rejected.
authority_class: A2_NOTIFY
alternatives:
  - option: "Keep context #5 deferred and start journeys from a customer lookup"
    rejected_because: >
      It contradicts the ratified state file, and it produces journeys with no origination record.
      Every question a pilot must answer — conversion rate, who owned the sale, what the RM was
      certified for at the time — is asked of the opportunity, not the journey.
  - option: "Let the BFF mint an opportunity implicitly on first journey action"
    rejected_because: >
      An implicit origination is an origination whose actor, certification check and lob were never
      validated. It also puts business creation logic in the edge, which ARCH-005 exists to prevent.
  - option: "Allow the IPR to create an opportunity that the RM then adopts"
    rejected_because: >
      Origination by an uncertified insurer employee is solicitation regardless of who adopts it
      afterwards, and the adoption step would rewrite the accountable SP that ADR-004 makes
      immutable.
consequences:
  positive:
    - "One entry to the funnel; every downstream module has an origination record to consume"
    - "Architecture document and ratified scope agree again — a live drift is closed"
    - "Conversion measurement, SP attribution and audit reconstruction all have a root object"
  negative:
    - "One more service in Wave 1, against a build order deliberately kept minimal"
    - "Every downstream aggregate gains a mandatory reference, which is a schema constraint on seven aggregates"
compliance_impact: >
  Positive. Attribution to the accountable Specified Person begins at origination rather than being
  reconstructed later.
security_impact: >
  Adds an origination authorization check; no new trust boundary.
reversibility: MEDIUM
revisit_trigger: >
  Admission of a non-RM origination channel — campaign, self-service or partner — which is an R1+
  scope decision for Rajal, not an architecture preference.
approvals:
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Rajal / Product — required (build-order change). The Lead/Opportunity label, OPEN-D10, is DECIDED by Mahesh 2026-08-20 — see naming_resolution; Rajal's confirmation is still required for Product-owned and UI-facing artefacts, and for the CURRENT-STATE.yaml transcription"
  - "Kalpana / Delivery — notify (Wave 1 grows by one service)"
```

---

## ADR-006 — Line of business is a first-class dimension from release 1

```yaml
id: ADR-006
status: PROPOSED
problem: >
  R0 sells one Term Life product, and the R0 model treats the line of business accordingly: the
  information model recorded lob = TERM on the opportunity, conflating the line with the product
  class, and no document required lob on configuration, audit or authorization records. Health and
  General follow R0 on the same template. A dimension that is absent from the schema when the second
  line arrives is not a design change; it is a backfill across every table on the sale path, with an
  audit history that cannot be backfilled at all because nobody recorded which line those events
  belonged to.
context_stage: "WS-3 at S08; no migration written, no service implemented"
decision: >
  lob is mandatory and non-null on every business entity, every configuration record, every audit
  event and every authorization request, from the first migration. The vocabulary is frozen at
  LIFE, HEALTH and GENERAL; R0 populates LIFE only and the other two exist unpopulated in the
  enumeration, the configuration partitioning and the authorization model. lob and productClass are
  distinct attributes — lob = LIFE, productClass = TERM — which corrects the information model.
  Everything that varies by line is partitioned on lob from release 1: product and eligibility,
  journey step definitions, business rules, field validations, document checklists, commission and
  routing policy. Partitioning is not forking: party, opportunity, consent evidence, journey
  identity, payment mechanics, document storage, policy portfolio and audit stay shared and
  single-instance.
authority_class: A1_AUTONOMOUS
alternatives:
  - option: "Add lob when Health is admitted"
    rejected_because: >
      It is a migration across every table on the sale path plus an audit history that cannot be
      corrected, because the line each historical event belonged to was never recorded. This is the
      textbook case of a dimension that is free now and unaffordable later.
  - option: "Leave lob nullable and default it to LIFE"
    rejected_because: >
      A nullable dimension with a default is a dimension that is silently wrong for every row
      written before anyone noticed. A missing lob must be a rejection, not an inference.
  - option: "Fork a Life service now and clone it for Health"
    rejected_because: >
      LS-02: the default is shared, and splitting carries the evidence burden. Three
      implementations of consent, suitability and audit is the failure mode the LOB cell model
      exists to prevent — LOB is an isolation boundary, not three platforms with one logo.
consequences:
  positive:
    - "Adding Health becomes a cell plus configuration rather than a schema migration"
    - "Per-LOB isolation, scaling and release independence stay reachable at H2 without redesign"
    - "The frozen vocabulary makes the LOB-onboarding acceptance test answerable at H0, before a second line exists to reveal the problem"
  negative:
    - "Every migration, every entity and every event schema carries a column R0 never varies"
    - "Configuration seeds must be authored per LOB even where only one is populated"
    - "Physical partitioning strategy becomes a real decision for the DBA rather than a deferred one"
compliance_impact: >
  Positive. Regulatory attribution and evidence reconstruction are per line of business; audit
  records without lob cannot be segmented for a line-specific regulatory question.
security_impact: >
  lob becomes an authorization dimension — SP certification is scoped to the lines it covers, and
  an out-of-scope line is a refusal (INV-ACT-01).
reversibility: LOW
revisit_trigger: >
  A vocabulary change — a fourth top-level line the LIFE/HEALTH/GENERAL split cannot express.
approvals:
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Aarti / Database — required (physical partitioning strategy, OPEN-I6)"
```

---

## ADR-007 — The configuration layer ships in R0, and it is independent of any front end

```yaml
id: ADR-007
status: PROPOSED
problem: >
  ARCH-010 already required compliance-sensitive behaviour to be configuration-driven, but the R0
  build order delivered Administration and Config as versioned artefacts consumed at startup, and
  accepted that a rule-pack change would require a deployment until S13. Two things follow from
  that trade and neither is acceptable. The deployment pipeline becomes the rule-change mechanism,
  which is the coupling the configuration-first principle exists to remove. And a service written
  before the configuration layer exists is a service that branches on product and insurer literals
  in code — and those branches are never removed afterwards, because by then something depends on
  each of them.
context_stage: "WS-3 at S08; no business service implemented"
decision: >
  The configuration layer ships in R0 as a Wave 0b component, built first after the S08/S09
  foundation gate: one store, LOB-partitioned,
  append-only and versioned, with effective-dated activation windows, source-controlled idempotent
  seeds, and a resolution contract that every service consumes through a port. The configuration
  domains are enumerated as a closed list — product and plan definitions, product eligibility,
  journey step definitions and transitions, business rule packs, field validation rules, document
  checklists, role-to-permission grants including the partner gate, commission (namespace reserved,
  no consumer until R1), provider routing policy and attribution values. No business path branches
  on an insurer, product, LOB or channel literal; code that differs per provider lives only in that
  provider's adapter package. There is no compiled-in fallback: a service that cannot resolve its
  rules refuses the action. Every business record stores the configuration version that governed
  it. This decision is explicitly independent of front-end availability — there is no admin UI in
  R0, there may be none in R1, and administrators may have no interface at all. The UI is a later
  consumer of a layer that already exists.
authority_class: A1_AUTONOMOUS
alternatives:
  - option: "Keep the startup-artefact trade and build the config service at S13"
    rejected_because: >
      It makes a deployment the mechanism for a compliance rule change, and it guarantees that the
      services built in W1 to W4 contain the hardcoded branches the principle forbids. The debt
      would be discovered the first time Compliance changed a consent statement.
  - option: "Build the config service together with its admin UI, or not at all"
    rejected_because: >
      This is the coupling the review comment specifically rejects. The UI is a convenience for
      administrators; the layer is a structural property of the platform. Tying the second to the
      first means the rules live in code until someone funds a screen.
  - option: "Feature flags for the varying behaviour"
    rejected_because: >
      Flags are boolean and unversioned. They cannot express an effective-dated eligibility matrix,
      a versioned consent statement pack or an LOB-partitioned document checklist, and they leave no
      record of which rule governed a seven-year-old decision.
consequences:
  positive:
    - "A rule change is a seeded version plus an activation, not a release"
    - "Services are written against a resolution port from their first line, so the hardcoded branch never appears"
    - "A business record remains explicable under the rules in force when it was created — which is what a seven-year evidence obligation actually requires"
    - "The admin UI, whenever it is funded, is a consumer of an existing contract rather than a re-platforming"
  negative:
    - "One more service (Wave 0b) before any business capability is delivered"
    - "Every service gains a fail-closed dependency on configuration resolution"
    - "Seed authoring becomes a real engineering discipline, including per-LOB seeds for lines that are unpopulated"
compliance_impact: >
  Positive and material. Consent statement packs, suitability questionnaires and retention policy
  become versioned, effective-dated and independently evidenced, and the version that governed any
  business record is recoverable for the full retention horizon.
security_impact: >
  Role-to-permission grants become configuration, so the configuration store becomes an
  authorization-relevant asset: write access, seed provenance and change attribution require
  Deepali's review.
reversibility: MEDIUM
revisit_trigger: >
  Evidence that the resolution path cannot meet its latency budget under real load, which would
  reopen the caching and materialisation design — not the configuration-first principle.
approvals:
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Deepali / Security — required (configuration as an authorization-relevant asset)"
  - "Shailja / Compliance — required (rule-pack versioning and evidence)"
  - "Kalpana / Delivery — notify (a new Wave 0b lands before Wave 1)"
```

---

## ADR-008 — Data ownership is the invariant; physical cluster topology is an evidence-led decision

```yaml
id: ADR-008
status: PROPOSED
problem: >
  ARCH-004 bundled three claims under one heading — "database-per-service" — and the repository has
  been citing all three as though they were equally settled. Two of them are principles. The third
  is a topology decision with cost, upgrade, backup, DR and DBA consequences, and it had never been
  argued on its own terms. The bundling produced a live contradiction between two published
  diagrams: the R0 reference architecture asserted a store per service, while the North Star's
  boundary 8 asserted schema separation in a shared cluster with physical splitting deferred until
  scale, security or RTO justify it. Two artefacts, two R0 data topologies, no way for a reader to
  tell which was current. Raised as OPEN-A1 under SUG-20260820-al7.
context_stage: >
  WS-3 at S08. GATE-S08 open, GATE-S09 not started. No business service is implemented, no schema
  exists and no migration has been written — so this costs a documentation change today and a
  migration across every table in every context once R0 has run.
decision: >
  Split ARCH-004 into its parts and decide each on its own evidence.

  INVARIANT, unchanged and enforced. One owner per authoritative datum. No service reads or writes
  another service's tables, ever. Each service holds its own credential and owns its own schema.
  This is what makes the ownership claim enforceable rather than aspirational, and it is verified
  by ArchUnit and by IAM policy in the S09 IaC scan.

  DECISION, evidence-led, and taken here. R0 runs ONE Aurora PostgreSQL cluster with a schema per
  bounded context, per-context credentials and no cross-schema grants. A separate physical cluster
  per service is NOT a principle and is not adopted. Physical separation is taken later, per
  workload, when scale, blast radius, security isolation or RTO/RPO justify it — and the first
  split follows the LOB-cell / shared-platform seam, not the service boundary: shared platform data
  (party, opportunity, consent, journey registry, catalogue, payment, policy portfolio, audit index)
  in one cluster, and each LOB cell (life_journey, life_quote, life_proposal, then health_*, then
  general_*) able to become its own cluster with no application redesign.

  The seam is chosen deliberately. LB-5 already says the LOB boundary is where behaviour, traffic
  profile and change rate genuinely diverge, and the North Star's boundary 8 already draws the
  target that way. Splitting along a seam the domain model already recognises is a data move.
  Splitting per service is forty clusters across three environments plus DR, which is a cost,
  upgrade, backup and on-call problem rather than an architecture.
authority_class: A3_JOINT_REVIEW
alternatives:
  - option: "Keep ARCH-004 whole — a physical cluster per business service from R0"
    rejected_because: >
      It buys isolation nobody has evidence of needing and charges for it immediately, in cluster
      cost, patching, backup verification, connection management, cross-cluster consistency and DBA
      load, across dev, UAT, production and a DR region. VIN-001 section 34 names this directly:
      "database per service doesn't necessarily mean 40 separate Aurora clusters". It also weakens
      the thing that actually matters — teams that cannot afford forty clusters quietly share one
      and lose the ownership rule with it.
  - option: "Drop the ownership rule too and let R0 share tables inside one cluster"
    rejected_because: >
      This is the failure this ADR exists to prevent. Physical convenience must not erode the
      ownership half. A shared table between two contexts is a finding, not a shortcut, and no cost
      argument reaches it: cross-service table access is what makes a later split impossible.
  - option: "Defer the decision until S09 when the IaC is written"
    rejected_because: >
      The contradiction is already published in two diagrams that delivery reads. Deferring keeps
      both answers alive through the exact window in which the first schemas get written.
consequences:
  positive:
    - "One R0 data topology, asserted identically by both diagrams and by every architecture document"
    - "R0 runs on one cluster: one thing to patch, back up, restore-test and hold an on-call runbook for, at the stage where GATE-S09 must prove a timed restore"
    - "The ownership invariant is now stated on its own and is harder to trade away, because it is no longer bundled with a cost argument"
    - "The split axis is named in advance, so cell-owned schemas are laid out from day one to be extractable"
  negative:
    - "One cluster is one blast radius. A Life quote storm, a runaway migration or a cluster-level failure reaches shared platform data too"
    - "Noisy-neighbour risk is real and must be watched, not assumed away — it is the primary evidence this decision waits on"
    - "Per-schema credential discipline now carries the whole ownership guarantee, so a mis-scoped grant is a more serious defect than it would be with physical separation"
mitigations:
  - "Connection-pool caps per context so one context cannot exhaust the cluster on another's behalf"
  - "Per-context credentials with no cross-schema grant, verified in the S09 IaC scan alongside FF-09"
  - "Cell schemas named and laid out for extraction from day one (life_journey, life_quote, life_proposal), so the first split is a data move"
  - "The revisit trigger below is measured, not remembered"
compliance_impact: >
  Neutral. Residency, retention, WORM audit storage and encryption are unchanged — those are
  properties of the store and its region, not of how many clusters there are.
security_impact: >
  Material and must be reviewed, not assumed. Isolation that was going to be physical is now
  logical, so per-context credentials and schema grants carry the whole of it. Deepali's review is
  required on that basis. No trust boundary moves and nothing becomes internet-reachable.
reversibility: HIGH
revisit_trigger: >
  Measured, not remembered. Any of: a context whose load, connection count or lock profile
  demonstrably degrades another; an RTO/RPO requirement one cluster cannot meet; a security or
  regulatory requirement for physical isolation of a data class; or the Health cell arriving at R3,
  which is the first point at which the LOB seam has a second occupant to justify it.
supersedes: >
  ARCH-004, physical-topology half only. The ownership and credential/schema-ownership halves of
  ARCH-004 are retained and restated above, not withdrawn.
approvals:
  - "Mahesh / Architecture — DECIDED 2026-08-20 in session; human signature outstanding"
  - "Aarti / Database — REQUIRED and outstanding. CR-011 names her as the accountable approver for the ARCH-004 reconciliation; physical model, recovery and the restore-time evidence for GATE-S09 are hers"
  - "Deepali / Security — REQUIRED and outstanding (isolation moves from physical to logical)"
  - "Shivanshi / SRE — notify (blast radius, connection limits, restore drill)"
```

---

## ADR-009 — Hybrid bank connectivity is provisioned in R0: a Transit Gateway hub, VPN from day one, Direct Connect as the primary path when the circuit lands

```yaml
id: ADR-009
status: PROPOSED
problem: >
  R0 depends on two bank-internal systems it has no network path to. Context #4 Customer reads
  CIF/ETB data from Core Banking, and WS-2 Phase 2 federates Keycloak to Bank AD. The LLD said
  only that this "will require either Direct Connect / VPN or a bank-hosted reverse proxy", named
  it as an S09-entry decision owned by Shivanshi and the bank network team, and permitted stubs in
  dev. Nothing said what R0 provisions, so nothing was provisioned, and the decision had no date.
  That leaves the two longest-lead items on the programme — a carrier circuit and a bank firewall
  change — sitting behind a decision nobody had been asked to take, while a stub in dev quietly
  became the only tested path. An ETB lookup against a stub proves the code compiles, not that the
  journey works, and W1 cannot be evidenced without the real path.
context_stage: >
  WS-3 at S08 with S09 overlapped. No VPC exists yet, so this is a greenfield network decision
  today and a re-addressing exercise after the first environment is built.
decision: >
  R0 provisions hybrid connectivity as a first-class layer, in the same S09 change as the VPCs.

  TOPOLOGY. A Transit Gateway in a new `network` account is the single hub for every
  bank-directed and inter-VPC route. Workload VPCs (dev, uat, prod) attach to it; nothing peers
  VPC-to-VPC. A separate TGW route table per environment carries only that environment's
  attachments and only the bank prefixes that environment is entitled to, so no dev workload can
  route to a production bank prefix even by misconfiguration. The account is added deliberately:
  five accounts become six, because a shared network plane owned by an environment account is an
  environment that can change everyone else's routing.

  TWO PATHS, IN THIS ORDER. Site-to-Site VPN over the TGW is provisioned FIRST, because it needs
  a public IP and a bank firewall rule rather than a carrier order, and it is what removes the
  stub from uat. Direct Connect follows: two hosted VIFs at two Mumbai DX locations through one
  Direct Connect Gateway, and it becomes the primary path when the circuit is accepted. BGP
  prefers DX; the VPN stays configured as the standby path forever rather than being decommissioned
  once DX is live.

  WHAT DEPENDS ON IT. `dev` may use CBS and AD stubs. `uat` and `prod` may not: from R0, a bank
  system is reached over this path or the journey is not evidenced. That is the gap this ADR
  closes, and it is the whole of its cost.
authority_class: A3_JOINT_REVIEW
alternatives:
  - option: "Keep the decision open and run CBS/AD stubs through UAT, as the previous LLD allowed"
    rejected_because: >
      It defers the two longest-lead items on the programme past the point where they block W1,
      and it makes the stub the tested path. WS-1 gate 4.3 already shows how this ends: a criterion
      that cannot close because an external party was engaged too late. Stubs are a dev convenience,
      never an evidence source.
  - option: "A bank-hosted reverse proxy over the internet with mTLS, and no private circuit"
    rejected_because: >
      It puts CIF traffic on the public internet, and Bank AD federation over a public hop is a
      trust-boundary change Deepali would have to accept for a saving the bank has not asked for.
      It also does not remove the bank-side firewall work, which is the actual lead time — so it
      buys nothing and costs a boundary.
  - option: "Direct Connect only, and wait for the circuit before UAT"
    rejected_because: >
      A carrier order is the one dependency on this list that working harder cannot accelerate.
      Making it the only path makes the whole of uat wait on it. VPN-first is strictly better:
      same routing, same TGW, available in the same change as the VPC.
  - option: "VPN only, and treat Direct Connect as an R1 upgrade"
    rejected_because: >
      Acceptable for uat, not for prod. A VPN tunnel is bandwidth-limited and internet-path
      dependent, and CIF lookups sit inside the RM's NFR-LAT-01 300 ms p95 budget. It is a
      standby path, not a production one.
  - option: "One shared Transit Gateway route table for all environments"
    rejected_because: >
      It converts the hard account boundary the landing zone exists to create into a routing
      convention. Environment separation must survive a mistake in one attachment.
consequences:
  positive:
    - "The two external-lead-time items start at the beginning of S09 instead of being discovered at W1"
    - "uat and prod exercise the real CBS and the real AD, so an ETB lookup is evidence rather than a mock"
    - "One hub carries bank routing, DR routing and inter-VPC routing, so the DR region is reachable by the same design (see D16 in the LLD)"
    - "Environment isolation is enforced in the route table, not in a review comment"
  negative:
    - "A sixth AWS account, a TGW, per-environment route tables and attachment costs, plus two DX ports once the circuit lands — a permanent run-rate increase before the first sale"
    - "Two paths mean two failure modes and a failover to test. An untested standby path is a claim, not a capability (NFR-NET-01)"
    - "The bank network team becomes a hard dependency of S09 rather than a consultee, with its own change windows"
    - "TGW data-processing charges apply to every bank-directed and inspected byte, including the egress path in ADR-010"
mitigations:
  - "VPN first, so no gate waits on a carrier order"
  - "DX failover to VPN is exercised and timed once before prod, as NFR-NET-01, not assumed from a BGP config"
  - "Per-environment TGW route tables, asserted in the IaC scan next to FF-09"
  - "Bank-side dependency raised as a dated external dependency with a named owner rather than an architecture assumption"
compliance_impact: >
  Positive and material. CIF and AD traffic leaves the public internet entirely, which is the
  posture Shailja's residency and confidentiality position assumes. No data crosses a region
  boundary: both DX locations and the TGW are in ap-south-1, and the DR attachment terminates in
  ap-south-2. Control C6 is unaffected.
security_impact: >
  Material, and Deepali's review is required. This creates a new trust boundary — TB-7, platform
  to bank internal over a private circuit — and it is the first boundary where traffic originates
  outside AWS. Route tables become an authorization surface: a prefix advertised into the wrong
  route table is a lateral path between environments. The boundary is default-deny at the firewall
  in ADR-010 and at the security group, and "inside the circuit" is not an authorisation.
reversibility: MEDIUM
revisit_trigger: >
  The bank mandating a different termination pattern (a bank-hosted proxy or an existing enterprise
  TGW we attach to instead of owning); measured DX utilisation above 60% of port capacity; or a
  second region becoming active, at which point the hub becomes a multi-region routing decision
  rather than a single-hub one.
approvals:
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Shivanshi / SRE — REQUIRED and outstanding. The circuit, the BGP design, the failover exercise and the bank-network engagement are R10's"
  - "Deepali / Security — REQUIRED and outstanding (new trust boundary TB-7; routing as an authorization surface)"
  - "Shailja / Compliance — notify (CIF and AD traffic leaves the public internet; residency unchanged)"
  - "Kalpana / Delivery — REQUIRED. This adds an external dependency to the S09 critical path and it is the one item that cannot be recovered by working harder"
```

---

## ADR-010 — Every egress and inter-VPC flow is inspected centrally by AWS Network Firewall, and the allowlisted Elastic IPs move to a per-environment egress VPC

```yaml
id: ADR-010
status: PROPOSED
problem: >
  R0's network controls stopped at layer 4. Security groups filter by IP and port, Kubernetes
  NetworkPolicy filters by namespace and label, and the generated topology diagram asserted
  "Service mesh — NetworkPolicy is enough". For east-west traffic that is true. For egress it is
  not: a pod that can reach the NAT gateway can reach any address on the internet on 443, and
  nothing in R0 could see or stop it. That is the path that matters here — the platform holds PAN,
  income and health attributes and calls out to an aggregator, a payment gateway and an SMS
  gateway, so unrestricted 443 egress from a compromised or misconfigured workload is the
  exfiltration route with the shortest description. The comparison with AU Bank's existing estate
  made the gap concrete: that platform inspects egress at a next-generation firewall, and R0 did
  not inspect it at all.
context_stage: >
  WS-3 at S08, S09 overlapped, no VPC built. Centralised inspection is a routing decision, and
  routing decisions are cheap before the first subnet exists and invasive afterwards — every
  workload VPC route table and the entire published Elastic IP list change with it.
decision: >
  All north-south egress and all inter-VPC traffic is inspected.

  TOPOLOGY. Each environment gets an inspection/egress VPC in the `network` account, attached to
  the ADR-009 Transit Gateway. Workload VPC default routes point at the TGW, not at a local NAT.
  The inspection VPC holds AWS Network Firewall endpoints (one per AZ), and the NAT gateways with
  the Elastic IPs sit behind the firewall. Traffic path: pod → TGW → firewall endpoint → NAT →
  IGW. Bank-directed traffic takes the same hub and the same inspection.

  ONE INSPECTION VPC PER ENVIRONMENT, not one shared across environments. Production egress never
  transits a VPC that a dev change can alter, and a firewall rule change in dev cannot silently
  apply to prod.

  THE ELASTIC IPs MOVE, AND THIS IS THE LOAD-BEARING CONSEQUENCE. The addresses 1SB and the AU
  Bank Payment Gateway allowlist are now the egress VPC's NAT EIPs, one per AZ per environment,
  and they stop changing when a workload VPC changes. The list is smaller and more stable than the
  per-VPC list it replaces, but it must be published to both external parties before uat exactly
  as before — and it must be published from this design, not the old one. Publishing the wrong
  EIPs is indistinguishable, from 1SB's side, from not publishing them.

  RULE POSTURE. Stateless rules drop obvious noise. Stateful rules are strict-order with a domain
  allowlist: the aggregator, the PG, the SMS/email gateway, ECR and the AWS endpoints not already
  covered by a VPC endpoint. Everything else is dropped and logged. Managed IPS rule groups are
  enabled in alert mode first and moved to drop before prod. TLS inspection is configured for
  destinations we terminate normally; the mTLS flows to 1SB are NOT decrypted — they are matched
  on SNI and destination and left intact, because a man-in-the-middle on a mutually authenticated
  channel is an outage, not a control.

  WHAT THIS IS NOT. It is not ingress inspection for public traffic: the public edge is
  CloudFront + WAF + API Gateway (ADR unchanged), and Network Firewall is not on that path. It is
  not a service mesh, and it does not replace NetworkPolicy — east-west inside a cluster stays with
  NetworkPolicy and IRSA. Claiming otherwise would be the kind of overreach that makes a control
  look installed when it is not.
authority_class: A3_JOINT_REVIEW
alternatives:
  - option: "Security groups and NetworkPolicy only, as R0 previously specified"
    rejected_because: >
      Neither can express "this workload may reach the aggregator and nothing else on 443", and
      neither produces a log a responder can query. The control that catches the exfiltration case
      is domain-aware and it did not exist.
  - option: "Squid or a self-managed proxy fleet on EKS for egress filtering"
    rejected_because: >
      It puts a self-patched, self-scaled, self-monitored fleet on the money and provider path,
      owned by a team that has not yet run one service in a real environment. A managed firewall
      has a worse feature set and a much better failure profile."
  - option: "A third-party NGFW appliance (Fortigate, Palo Alto) in an inspection VPC"
    rejected_because: >
      This is what the existing AU estate runs, and it is a defensible answer with a real
      advantage in shared tooling and existing operator skill. It is rejected for R0 on
      operational surface, not capability: licensing, HA pairs, version upgrades and vendor
      support all land on a team of this size before the first sale. It stays the obvious
      migration if the bank's network standard requires it — which is this ADR's revisit trigger.
  - option: "One shared inspection VPC for all environments"
    rejected_because: >
      Cheaper, and it puts production egress on a path that a dev change can modify. The account
      boundary exists precisely so that it cannot."
  - option: "Decrypt everything, including the 1SB mTLS session"
    rejected_because: >
      Mutual TLS to a provider cannot be intercepted without breaking client authentication.
      Attempting it produces a broken quote path and a false sense of coverage.
consequences:
  positive:
    - "Egress becomes an allowlist with a log, so 'what did this pod talk to' is answerable"
    - "The EIP set that 1SB and the PG allowlist is centralised and stable, and no longer changes with workload topology"
    - "Firewall, flow and TGW logs give ADR-013's search pipe something worth indexing — the two closures are complementary, not independent"
    - "One inspection design covers internet egress, bank-directed traffic and inter-VPC traffic"
  negative:
    - "Firewall endpoints are charged per AZ per environment plus per GB processed, and every inspected byte also crosses the TGW — this is the most expensive of the five closures at R0 volumes, and R0 volumes are small"
    - "A new mandatory hop on the provider and payment path. A firewall rule error is now a full egress outage, and the quote path is the first thing to notice"
    - "The published Elastic IP list changes shape. Any allowlist conversation already started with 1SB or the PG has to be redone against this design"
    - "Someone must own the domain allowlist. A rule set nobody curates decays into permit-any within two incidents"
mitigations:
  - "Managed IPS rule groups run in alert mode until prod, so the first drop is deliberate rather than discovered"
  - "The domain allowlist is versioned configuration reviewed in the same change as the code that needs a new destination — a new egress destination is a pull request, not a ticket"
  - "Firewall endpoints in every AZ the workloads use; a single-endpoint egress path is an AZ-wide outage waiting for a maintenance window"
  - "NFR-NET-02 asserts 100% of egress traverses inspection: any route table with a default route that is not the TGW fails the IaC scan"
  - "dev runs a single endpoint in alert-mostly mode; the cost shape is deliberately not production-shaped"
compliance_impact: >
  Positive. Egress inspection with retained logs is directly evidential for the RBI cyber-security
  expectations the payment control already cites, and it makes an exfiltration claim testable. The
  logs carry destinations and metadata, never payloads, so no new PII surface is created — and TLS
  inspection is never enabled on a path carrying regulated payloads to a mutually authenticated
  provider.
security_impact: >
  This is a security control, so Deepali owns its acceptance, not merely its review. Two things
  need her judgement rather than mine: whether alert-mode IPS before prod is an acceptable interim,
  and whether the TLS-inspection exemption for mTLS destinations is scoped tightly enough that it
  cannot be used as a general bypass.
reversibility: MEDIUM
revisit_trigger: >
  A bank network standard mandating the enterprise NGFW platform; measured firewall processing cost
  exceeding the compute cost of the workloads behind it; or a second active region, which makes the
  single-hub egress design a multi-region one.
approvals:
  - "Deepali / Security — REQUIRED. This is her control and her acceptance, not a notification"
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Shivanshi / SRE — REQUIRED and outstanding. She owns the rule set, the endpoint placement, the failure runbook and the EIP publication"
  - "Kalpana / Delivery — REQUIRED (the EIP publication moves onto the S09 critical path in a new shape)"
  - "Shailja / Compliance — notify (inspection logs as evidence; no payload retention)"
```

---

## ADR-011 — A managed cache tier is provisioned in R0 for sessions, read-through L2 and rate limiting; it is never a system of record and never softens a fail-closed rule

```yaml
id: ADR-011
status: PROPOSED
problem: >
  R0 had no shared cache, and two things were wrong because of it. First, a real contradiction:
  WS-2's accepted design specifies a Redis session vault and ships a Redis container in
  `docker-compose.identity.yml`, while the R0 LLD said sessions should prefer DynamoDB and treated
  ElastiCache as an exception to be avoided. Two published designs, two session stores, and the
  decision was recorded as open. Second, an in-process-only cache tier means every pod holds its
  own copy of configuration and catalogue data with its own TTL, so N pods produce N different
  answers for the window of one TTL — and configuration resolution is on the path of every
  regulated action. A per-pod cache is not a correctness problem the moment a rule version
  changes; it is a correctness problem for exactly as long as the TTL.
context_stage: >
  WS-3 at S08. No service holds a session or resolves configuration yet, so the port shape is
  still free. After W0b and W4 this becomes a change to the two things every request touches.
decision: >
  R0 provisions ONE ElastiCache for Valkey replication group per environment, in the private-data
  subnets, cluster mode disabled, primary plus replica in two AZs with automatic failover on,
  encrypted at rest with a CMK and in transit, and reached only from the app subnets.

  PERMITTED USES — a closed list. (a) The token-hiding BFF session vault. This CLOSES the open
  decision in favour of WS-2's accepted design, and the DynamoDB `sessions` table is withdrawn
  from the BOM. (b) An L2 read-through cache behind the existing in-process L1 for configuration
  resolution and catalogue reads. (c) Distributed rate-limit and OTP-attempt counters at the BFF,
  which are per-principal and therefore wrong when they are per-pod.

  FORBIDDEN USES — also closed, and these are the invariants the tier must not erode.
  Idempotency records stay in the owning service's store, written in the same transaction as the
  business change (INV-IDM-01): a cache cannot be transactionally consistent with a database
  write, and idempotency that is only mostly right on the money path is worse than none.
  The cache is never a system of record for anything. It never becomes a fallback for an
  unreachable configuration store: `S-21` still fails closed when the L1 TTL has expired, and an
  L2 hit is only usable inside its own TTL for the same reason. No PII beyond the session's
  principal claims. Per-service Valkey ACL users with a key-prefix grant, so one service cannot
  read another's keyspace — the same ownership rule ADR-008 applies to schemas.

  SIZING. Two nodes, not a sharded cluster. At CAP-A volumes — about 100 journey starts an hour,
  6.8 a minute at Q4 peak — the working set is thousands of session and configuration keys, and
  cluster mode would add resharding complexity to a workload that fits in one node's memory many
  times over. The safe range is stated so nobody has to guess: scale up a size before scaling out,
  and revisit sharding only on measured eviction or CPU, never on headcount of services.
authority_class: A3_JOINT_REVIEW
alternatives:
  - option: "DynamoDB for sessions, in-process caches only, as the previous LLD preferred"
    rejected_because: >
      It is a defensible design and it was chosen to avoid a new managed service. But it leaves
      the per-pod configuration divergence in place, leaves rate limiting per-pod and therefore
      per-pod evadable, and contradicts WS-2's accepted session design — which means one of the
      two workstreams was going to be rewritten. Closing the contradiction in favour of the tier
      also closes the divergence.
  - option: "Self-managed Redis or Valkey on EKS"
    rejected_because: >
      Persistence, failover, patching and backup for a session store, owned by the team that is
      still closing GATE-S08. The managed service costs money; the self-managed one costs an
      on-call rotation.
  - option: "Provision the tier but also move idempotency into it, as the target-state review says"
    rejected_because: >
      This is the trap. Idempotency must be atomic with the business write or it does not do its
      job, and the money path (INV-PAY-04) depends on it. Target state names ElastiCache for
      idempotency; target state is wrong about that, and this ADR says so explicitly rather than
      leaving it to be discovered.
  - option: "Let the cache serve stale configuration when the store is unreachable"
    rejected_because: >
      It is the compiled-in fallback that CF-1 forbids, arriving through a different door. A
      platform that cannot resolve the rule must refuse the action.
consequences:
  positive:
    - "One session design across WS-2 and WS-3, and a published contradiction disappears"
    - "Configuration and catalogue reads converge across pods, so a rule activation is visible platform-wide within one TTL rather than one TTL per pod"
    - "Rate limits and OTP attempt counters become per-principal facts instead of per-pod ones — the per-pod version was a control with a documented bypass"
    - "Session survival across pod restart and deploy, which the in-process alternative never had"
  negative:
    - "A stateful managed service in the R0 estate, with its own version upgrades, maintenance windows and failover behaviour"
    - "A new shared dependency on the session path: cache loss is a mass logout even with failover, and the failover window is real"
    - "The strong temptation, on day one of the first incident, to put idempotency or a business fact in it. This ADR's forbidden list is the only thing standing there"
    - "Run-rate cost in every environment, for a working set that would fit in a pod's heap today"
mitigations:
  - "Two nodes across two AZs with automatic failover on; a single-node session store makes an AZ event a mass logout"
  - "Per-service ACL user and key prefix, verified in the IaC scan (FF-24)"
  - "FF-23 asserts no idempotency or evidence write targets the cache — the forbidden list is a machine check, not a convention"
  - "NFR-CAC-02 measures session survival across a rolling restart and a forced failover"
  - "dev runs a single node deliberately: it is synthetic data and a failover there is not an incident"
compliance_impact: >
  Limited and reviewable. Session material and configuration values are cached; regulated evidence
  is not, and consent, suitability and audit records never enter the tier. Encryption at rest with
  a bank-owned CMK and in-transit TLS keep the residency and key-ownership position unchanged.
security_impact: >
  Material. A session vault is an authentication asset: read access to it is read access to live
  sessions. Deepali's review covers the ACL model, the AUTH credential rotation path, and the
  confirmation that no OAuth token reaches the device even though the tokens now live in a shared
  store rather than a per-service one.
reversibility: HIGH
revisit_trigger: >
  Measured eviction pressure or CPU saturation on the primary (scale up first, shard only then);
  a second line of business needing keyspace isolation stronger than an ACL prefix; or evidence
  that the L2 layer is not earning its latency, in which case it is removed and the L1 stays.
approvals:
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Deepali / Security — REQUIRED and outstanding (session vault is an authentication asset; ACL and rotation model)"
  - "Aarti / Database — REQUIRED and outstanding. Caching topology, eviction policy, node sizing and the boundary between cache and store are hers"
  - "Amit / Engineering — notify (the L1/L2 port shape is implemented once, in a shared library, not per service)"
  - "WS-2 — the open session-store decision closes here in favour of their accepted design"
```

---

## ADR-012 — Amazon MSK is the R0 event backbone, and the transactional outbox stays as its source of truth

```yaml
id: ADR-012
status: PROPOSED
problem: >
  R0 chose a transactional outbox per service and no broker, with a revisit trigger of "a third
  distinct consumer class, sustained outbox lag, or Reporting entering scope". The reasoning was
  sound and the outbox is the right mechanism for the dual-write problem. What it does not solve
  is fan-out. Every consumer of a domain event has to be given its own poller against the
  producing service's outbox table, which means every new consumer is a change to the producer's
  database access pattern, and the audit, notification and reconciliation consumers of R0 are
  already three shapes of the same code. The revisit trigger was also going to fire during R0
  rather than after it: audit, notification and compensation are three consumer classes, and #18
  Reporting is the fourth as soon as the pilot funnel needs a read model. Deferring the broker
  until the trigger fires means adopting it in the middle of the vertical slice instead of before
  it, which is the worst of the three available moments.
context_stage: >
  WS-3 at S08. No outbox table, no publisher and no consumer exists yet, so the publish contract
  is free to shape. Once #16 Audit is written against a direct outbox poll, moving it to a broker
  is a rewrite of the one component that must not lose a record.
decision: >
  R0 provisions Amazon MSK as the event backbone AND KEEPS the transactional outbox. This is one
  decision, not two, and the order matters.

  THE OUTBOX REMAINS THE SOURCE OF TRUTH. A service writes its business change and its outbox row
  in one local transaction. `outbox-publisher` reads the outbox and publishes to MSK. The outbox
  row is the durable record and the replay log; the topic is transport and fan-out. This keeps
  exactly the property the original decision was made for — no dual write, no lost event on a
  broker outage — and adds the property it lacked.

  NO REGULATORY EVIDENCE EXISTS ONLY IN A TOPIC. #16 Audit consumes from MSK and writes to
  DynamoDB and the S3 WORM archive; that write, not the topic, is what satisfies INV-JRN-05 and
  lets a journey reach SOLD. Retention on a topic is an operational parameter. Retention on
  evidence is a licence condition. A topic is never cited as the audit record.

  SHAPE. Provisioned MSK, three brokers, one per AZ, KRaft, TLS in transit, at-rest encryption
  with a CMK, SASL/IAM authentication with per-topic IAM policy so a consumer group cannot read a
  topic it was not granted. Topics are versioned and named for the domain, with a dead-letter
  topic per consumer group. Event contracts are registered in the AWS Glue Schema Registry with
  backward compatibility enforced in CI (FF-25) — adopting a broker without a schema contract
  just moves the coupling from the database into the payload.

  KEDA IS NOW PERMITTED, for consumer-lag scaling only. The previous "DO NOT until there is a
  broker" was correct and its condition is now met.

  DR. MSK is NOT replicated to ap-south-2 and MSK Replicator is not provisioned. Because the
  outbox is in Aurora and Aurora is replicated, every event is reproducible in the DR region by
  replaying the outbox — so a broker replica would be a second copy of something already
  recoverable. The constraint this places on every consumer is stated as a design rule rather than
  a hope: consumers must be idempotent on `eventId` and must tolerate replay, and consumer offsets
  are not evidence of anything.

  SIZING, STATED SO NOBODY SCALES BLINDLY. R0 is ~100 journey starts an hour, and a journey emits
  single-digit events. That is tens of messages a minute against a three-broker cluster sized for
  availability, not throughput. Three brokers is the AZ-availability floor, not a capacity
  calculation, and partition counts start at the minimum that lets a consumer group scale to its
  pod count.
authority_class: A3_JOINT_REVIEW
alternatives:
  - option: "Keep the outbox alone and let the revisit trigger fire, as the previous decision said"
    rejected_because: >
      The trigger fires inside R0 — three consumer classes exist in the design already. Adopting a
      broker mid-slice means changing the audit path, which is the one path that must not lose a
      record, at the moment it is being evidenced for a gate."
  - option: "Replace the outbox with direct publishing to MSK"
    rejected_because: >
      This is the dual-write bug in its classic form. A business commit followed by a publish is
      two writes with no shared transaction: a crash between them loses the event, and a retry
      duplicates the business change. The outbox exists to prevent exactly this and keeping it is
      most of this ADR's value.
  - option: "SNS + SQS fan-out instead of Kafka"
    rejected_because: >
      Genuinely simpler and cheaper, and it was the right answer while there was one consumer.
      It is rejected on replay and ordering: SQS gives no durable, re-readable log, so a
      consumer that needs to rebuild — the reporting read model, or an audit re-verification —
      cannot. Per-journey ordering is also a real requirement (sequence_no gaplessness) and topic
      partitioning by journeyId expresses it directly.
  - option: "MSK Serverless everywhere"
    rejected_because: >
      Attractive for dev and it is what dev will run. For uat and prod it removes the broker
      controls (per-broker metrics, storage tuning, replication factor) that a first DR and a
      first load test need to be able to see."
consequences:
  positive:
    - "A new consumer is a consumer group, not a change to the producer's database access"
    - "Per-journey ordering and durable replay become available to audit and to the reporting read model that follows in S13"
    - "The audit path keeps its exactly-once-in-effect property, because the outbox still owns durability and consumers dedupe on eventId"
    - "KEDA can scale consumers on real lag instead of CPU, which is what a post-outage backlog actually needs"
  negative:
    - "A three-broker stateful cluster per environment, with version upgrades, storage growth, partition rebalancing and its own failure modes — the largest single operational addition of the five closures"
    - "Two mechanisms on the async path instead of one. Outbox plus broker is more moving parts than outbox alone, and the publisher becomes a component that must not silently stop"
    - "Massively over-provisioned for R0 throughput, and that is the honest trade: three brokers buys AZ availability, not capacity"
    - "A schema registry and a compatibility policy to maintain, or the payload becomes the new coupling"
mitigations:
  - "The outbox stays, so a broker outage delays events rather than losing them"
  - "Outbox age is the alert that matters (NFR-DAT-05, unchanged) — a stalled publisher is visible in seconds, not at the next audit review"
  - "Consumers idempotent on eventId, replay-tolerant by design rule, and proven by a replay drill (NFR-EVT-03)"
  - "FF-26 asserts no audit or evidence claim is satisfied by a topic read alone"
  - "dev runs MSK Serverless or a single broker; the three-broker shape exists only where availability is being evidenced"
compliance_impact: >
  Neutral if the evidence rule holds, and a finding if it does not. The audit record remains the
  DynamoDB row and the WORM archive. Topic data is encrypted with a bank CMK, stays in
  ap-south-1, and carries no PII beyond the identifiers the audit event already carries — event
  payloads follow the same PII rules as logs (PII-B), which is why the schema registry matters
  for review rather than only for compatibility.
security_impact: >
  Material. SASL/IAM per-topic authorisation becomes an access-control surface: a consumer group
  granted a topic it should not read is a data-access defect that no application code shows.
  Deepali's review covers the topic-to-role matrix and the confirmation that the DLQ topics do not
  become an unmanaged copy of every payload that ever failed.
reversibility: MEDIUM
revisit_trigger: >
  Measured, in both directions. Toward removal: if R0 completes with only one real consumer class
  and no replay ever used, the broker is a cost to withdraw rather than a decision to defend.
  Toward growth: sustained consumer lag that partition-level scaling cannot absorb, or a
  cross-region consumer, which reopens replication.
approvals:
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Shivanshi / SRE — REQUIRED and outstanding. Broker sizing, upgrade path, lag alerting and the replay drill are R10's, and she is the one carrying the new on-call surface"
  - "Deepali / Security — REQUIRED and outstanding (per-topic IAM, DLQ content, payload PII rules)"
  - "Shailja / Compliance — REQUIRED. The evidence rule above is the whole of her interest and it must be signed, not assumed"
  - "Amit / Engineering — notify (publisher and consumer are shared-library shapes, written once)"
```

---

## ADR-013 — Amazon OpenSearch is the R0 operational search and log-analytics pipe, and it never holds regulatory evidence

```yaml
id: ADR-013
status: PROPOSED
problem: >
  R0's only log destination was CloudWatch Logs. That is a sound store and a poor investigation
  tool: cross-service correlation over a journey means Logs Insights queries per log group, priced
  per gigabyte scanned, at the moment an incident is live. The gap became structural rather than
  merely inconvenient once the other closures were admitted — ADR-010 produces firewall and flow
  logs, ADR-009 produces TGW logs, ADR-012 produces broker and consumer logs, and none of that is
  worth generating if nobody can query it. The comparison with the existing AU estate named the
  same layer from the other direction: that platform runs a search cluster for exactly this, and
  R0 had no equivalent and no plan for one before S13.
context_stage: >
  WS-3 at S08 with S09 overlapped, and the first end-to-end journey (S11) is where correlated
  search first pays for itself. Retrofitting a log pipeline after the pipeline exists means
  re-emitting, not re-indexing.
decision: >
  R0 provisions one Amazon OpenSearch Service domain per environment as the OPERATIONAL search
  and analytics pipe.

  SHAPE. VPC-only domain in the private-data subnets — no public endpoint, ever. Dedicated master
  nodes plus data nodes across the environment's AZs, encryption at rest with a CMK, node-to-node
  encryption, TLS 1.2+ enforced, and fine-grained access control with IAM-mapped roles. Ingest is
  Fluent Bit on the nodes to Amazon Data Firehose to OpenSearch, with a failed-delivery bucket in
  S3 so a mapping error loses a document from the index and not from the record.

  WHAT IT INDEXES. Application logs (already PII-masked at emission), plus the logs the other four
  closures generate: Network Firewall alert and flow logs, VPC flow logs, TGW flow logs, MSK
  broker and consumer logs, and ALB and API Gateway access logs.

  THE TWO-PIPE RULE IS UNCHANGED AND NOW HAS A THIRD PARTY TO IT. The operational pipe is
  CloudWatch plus OpenSearch, retained to RET-OPERATIONAL (90 days) with an ISM policy that rolls
  30 days hot and deletes at the horizon. The regulatory pipe is the audit event store and the S3
  WORM archive, and OpenSearch is NOT part of it. Deleting an OpenSearch index deletes no
  evidence, and no gate, audit or regulatory query is ever satisfied from OpenSearch. That
  sentence is the reason this ADR can be approved without reopening the retention position.

  NO PII, ENFORCED TWICE. Masking stays where it is, at emission (FF-05). A second check runs over
  the index itself (FF-27), because a log pipeline is the most common way a restricted attribute
  reaches a store nobody classified.

  WHAT THIS IS NOT. It is not the analytics warehouse. Glue, Athena, Redshift and QuickSight stay
  out of R0: #18 Reporting & MIS is S13, and a pilot funnel is not a warehouse. It is not a
  business search index either — catalogue and journey search stay in their owning stores, because
  a search cluster fed by a log pipeline is not a system of record for a domain query.
authority_class: A3_JOINT_REVIEW
alternatives:
  - option: "CloudWatch Logs Insights only, as R0 previously specified"
    rejected_because: >
      It works and it is what the platform would use at 03:00 on the first incident, per log group,
      per query, priced per gigabyte scanned. The first end-to-end journey debugging session is
      where that cost is actually paid, in hours rather than rupees.
  - option: "Self-managed ELK on EKS"
    rejected_because: >
      A stateful cluster with PVCs, in a design whose whole PVC position is that business workloads
      do not have them. Upgrades and index management would land on the same team closing GATE-S08.
  - option: "Ship logs to the bank's existing enterprise ELK or SIEM"
    rejected_because: >
      Probably right eventually, and it is named as the revisit trigger: the bank has a security
      operations function and two log estates is one too many. It is not available now — the
      connectivity, the schema agreement and the bank-side onboarding are not in place, and R0
      cannot wait on them to be able to query a firewall drop.
  - option: "Also point the audit archive at OpenSearch so evidence is searchable"
    rejected_because: >
      This is the failure mode this ADR exists to prevent. A searchable copy becomes the copy
      people cite, and an index with an ISM delete policy is not a seven-year immutable record.
      Evidence is queried from its own store."
consequences:
  positive:
    - "One place to correlate a journey across BFF, services, hub, adapter, firewall and broker"
    - "The logs the other four closures generate become usable rather than merely retained"
    - "Investigation cost stops scaling with the volume scanned per query, which is what makes people stop looking"
    - "A named home for the security-relevant event classes the security architecture already requires to be separated from operational logs"
  negative:
    - "A stateful cluster per environment with version upgrades, shard management and its own capacity behaviour — and it is the closure most likely to be under-used if nobody builds the dashboards"
    - "Run-rate cost in three environments for a log volume that CloudWatch already holds; this is a duplicate store by design"
    - "A second place PII can land, which is why the enforcement is doubled rather than moved"
    - "The temptation to make it the audit store, or the business search index, or both"
mitigations:
  - "ISM policy from day one: 30 days hot, delete at RET-OPERATIONAL. An index with no lifecycle policy grows until it becomes an incident"
  - "FF-27 asserts no restricted attribute reaches an index; FF-28 asserts the audit archive is not written by the log pipeline"
  - "dev runs a single data node with a 7-day policy; production shape is not replicated downward"
  - "The domain is VPC-only with fine-grained access control, so 'searchable' never means 'reachable'"
compliance_impact: >
  Neutral by construction, and only because of the exclusion above. Operational logs carry no
  regulated attributes (PII-B) and are retained to RET-OPERATIONAL with a disposal record
  (NFR-DAT-07). The seven-year immutable position is untouched: OpenSearch holds no evidence, so
  its ISM delete policy cannot be a retention violation.
security_impact: >
  Two-sided and Deepali's to weigh. It creates a store that aggregates operational data across
  every service, which is a valuable target and needs VPC-only placement, fine-grained access
  control and audited human access. It also gives security operations the first queryable view of
  firewall drops, denied authorisations and attribution rejections that the security architecture
  §9 already requires to exist.
reversibility: HIGH
revisit_trigger: >
  The bank's enterprise SIEM or ELK becoming available to onboard onto, which supersedes this
  domain rather than extending it; or measured index cost exceeding the CloudWatch cost it was
  meant to relieve; or S13's Reporting & MIS arriving, which is a warehouse decision and not this
  one.
approvals:
  - "Shivanshi / SRE — REQUIRED and outstanding. Observability is R10's; domain sizing, ISM policy, dashboards and the ingest pipeline are hers"
  - "Mahesh / Architecture — AI-DRAFTED, human signature outstanding"
  - "Deepali / Security — REQUIRED and outstanding (aggregated operational store, access control, security event classes)"
  - "Shailja / Compliance — REQUIRED. The evidence exclusion is the whole of her interest here, and it is the reason this can be approved without reopening retention"
  - "Aarti / Database — notify (a second stateful store, its capacity behaviour and its boundary against the systems of record)"
```

---

## Signature status for ADR-001 — ADR-013

All thirteen records are **`AI-DRAFTED — mandatory human signature outstanding`**. ADR-002 is
`A4_HUMAN_REQUIRED`: it is a material scope and stage decision and an AI simulation of Mahesh must
not finalise it. None of these ADRs is `ACCEPTED` until the approvals listed in each record are
present, and none of them becomes binding merely because CR-010's checks pass or its branch is
mergeable.

ADR-004 is `A3_JOINT_REVIEW`: it creates a principal class and a trust boundary, so Deepali's
Security review is not optional, and its assist-only threshold is Shailja's to set (OPEN-D9).
ADR-005 changes the R0 build order and a Product-owned scope label, so Rajal's decision is
required. ADR-004 through ADR-007 were raised by the 2026-08-20 HLD review round and are recorded
under `SUG-20260820-hr0`.

ADR-008 is `A3_JOINT_REVIEW` and is the one record here whose decision half is **taken** while its
approvals are not. Mahesh decided the topology in session on 2026-08-20, closing `OPEN-A1`; the
record is written and the documents and diagrams follow it. But `CR-011` names **Aarti** as the
accountable approver for any `ARCH-004` reconciliation, and this ADR moves service isolation from
physical to logical, which makes **Deepali's** Security review non-optional. Neither approval
exists. An architect's decision is not a DBA's sign-off and an agent does not supply either.
ADR-008 is recorded under `SUG-20260820-dc4`, which also closes `OPEN-D10` inside ADR-005.

**ADR-009 through ADR-013 are the R0 robustness set**, raised on 2026-08-24 under
`SUG-20260824-gp1` … `SUG-20260824-gp5` and carried by
[`CR-012`](../../governance/change-requests/CR-012-r0-platform-robustness.md). They are recorded
together because they were decided together and because three of them only make sense as a set:
ADR-010's inspection path runs over ADR-009's hub, and ADR-013 exists in R0 largely because
ADR-009, ADR-010 and ADR-012 each generate logs that were otherwise unqueryable.

All five are `A3_JOINT_REVIEW`, and each names a persona whose approval is **not** a notification:

| ADR | The approval that is not optional | Why it is theirs and not Architecture's |
|---|---|---|
| ADR-009 | **Shivanshi** (circuit, BGP, failover) + **Kalpana** (external dependency on the critical path) | Architecture may specify a private path; it may not commit a carrier order or a bank change window |
| ADR-010 | **Deepali** — acceptance, not review | It is a security control. Alert-mode IPS before prod and the mTLS inspection exemption are her risk acceptances |
| ADR-011 | **Deepali** (session vault) + **Aarti** (cache/store boundary, sizing) | A session store is an authentication asset and a cache topology is a data decision |
| ADR-012 | **Shailja** (no evidence exists only in a topic) + **Shivanshi** (the new on-call surface) | The evidence rule is a licence position, not an architecture preference |
| ADR-013 | **Shailja** (the evidence exclusion) + **Shivanshi** (observability is R10's) | The exclusion is what makes the retention position survive a new searchable store |

**These five are also the set with the largest cost and operational consequence in the R0 estate,
and that is deliberate rather than incidental.** They add three stateful managed services, a
sixth AWS account, an inspection VPC per environment and two carrier circuits to a platform
carrying about 100 journey starts an hour. Every one of them is justified by a failure mode or an
evidence gap rather than by throughput, and each record says so in its own negatives. Nothing in
this set may be cited as authority to provision until the approvals above exist —
`RISK-012` (cost envelope) and `RISK-014` (operational surface against S08 maturity) are open
against exactly that.

---

## ADR-014 — R0 Lead language, evidence split, off-platform ingest, admin/MIS, issuance modes

```yaml
id: ADR-014
status: PROPOSED
problem: >
  Stakeholders require Lead as the spoken name, a light working inbox, 7-year evidence on
  payment/policy history, off-platform book capture, day-one admin and MIS, STP/non-STP/Insta,
  and PPHI alignment — in R0, not later.
context_stage: "WS-3 at S08; stakeholder override CR-013"
decision: >
  Context #5 is named Lead in Product, UI and architecture primary text. Opportunity remains
  the durable-demand alias only. Identifiers stay leadId / INV-LED-*. After CONVERTED
  (Payment RECONCILED and Policy ACTIVE) the working Lead archives; 7-year SoT is Payment,
  Policy.stateHistory, Consent, Suitability, Audit, plus Lead attribution fields (C-RET-1).
  Off-platform sales are Policy ingest (source=OFF_PLATFORM), never lead.create.
  Administration UI and Reporting/MIS are in R0 W4 and must not use the Lead writer (C-ISO-1).
  issuanceMode STP|NON_STP|INSTA is mandatory on Proposal/Policy (C-ISS-1).
  ADR-005 RM-only origination stands. ADR-007 configuration layer stands; its UI deferral
  is withdrawn.
authority_class: A3_JOINT_REVIEW
origin: CR-013
```

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2) · 2026-08-16 ·
**revised** 2026-08-20 · **revised** 2026-08-24 (R0 robustness set, ADR-009 … ADR-013) ·
**revised** 2026-08-25 (`ADR-014`, `CR-013`)

---

## ADR-015 — One NIP-APP client; ns:edge is NIP web + NIP BFF only

```yaml
id: ADR-015
status: PROPOSED
problem: >
  RM desktop, admin/ops and Insurance Partner Rep were drawn as separate webs and
  hostnames (admin-web, admin.{env}, Admin BFF). That is not the channel. One Flutter
  enterprise application serves every workforce role. Store vs MDM was still open at S11.
context_stage: "WS-3 at S08; architecture decision taken by Mahesh 2026-08-25"
decision: >
  There is one workforce Flutter project: NIP-APP (New Insurance Platform).
  It produces three artefacts from the same codebase: a web build, an Android APK
  and an iOS IPA. Roles (BANK_RM, INSURER_PARTNER_REP, BANK_EMPLOYEE admin/ops)
  are PDP-enforced views, not applications (JS-08, JS-10, ID-22, TI-15).
  ns:edge contains exactly two workloads: nip-web (image-baked Flutter web, no PVC)
  and #2 NIP BFF (token-hiding session). Nothing RM-named or admin-named lives there.
  One public hostname. GET /* → nip-web; /api/* → #2. Admin/MIS screens are routes
  on NIP-APP; they still must not use the Lead writer (C-ISO-1, ADR-014).
  Distribution: nip-web on EKS; APK on Google Play; IPA on the Apple App Store.
  Deepali owns store hardening (device attestation, pinning, no tokens on device —
  already S-01). Amit may merge workforce-access-bff into #2 (packaging).
  #1 Customer BFF remains R1. A second admin/ops app or hostname is SF4.
authority_class: A3_JOINT_REVIEW
origin: "human:Mahesh taken decision · SUG-20260825-nip"
```

**Drafted:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2) · 2026-08-25.
Human T4 Architecture sign-off outstanding. Deepali jointly owns store-listing exposure.

---

## ADR-016 — Enterprise perimeter, integration and delivery baseline (Cloudflare, F5, EBS, GitLab, Terraform, CloudTrail/CloudWatch)

```yaml
id: ADR-016
status: PROPOSED
problem: >
  Internal architecture team review mandated alignment with bank enterprise standards:
  Cloudflare Enterprise CDN/DDoS, F5 WAF for L7 security policy, EBS (Enterprise Service Bus)
  APIs for Core Banking (CBS / CIF), GitLab CI/CD pipelines, Terraform for IaC, and mandatory
  CloudTrail + CloudWatch.
context_stage: "WS-3 at S08/S09; internal architecture review directive 2026-08-25"
decision: >
  1. Edge ingress: AMENDED by ADR-018. Cloudflare (Enterprise Edge CDN / DDoS, SaaS) ->
     F5 Distributed Cloud / F5-XC (Bank Policy, SaaS) -> Amazon API Gateway (VPC Link) ->
     Internal ALB. The 2026-08-25 "External ALB before API Gateway" hop is withdrawn.
     Cloudflare and F5 are not AWS services and are not placed in any platform VPC.
  2. Core Banking integration: Customer lookups route via bank EBS (Enterprise Service Bus) APIs
     over Transit Gateway private links. Terminology is standardized as EBS (CBS / CIF).
  3. Delivery & IaC: GitLab CI/CD for enterprise pipelines and Terraform for 100% IaC provisioning.
  4. Observability & Audit: AWS CloudTrail (mandatory management auditability) alongside
     Amazon CloudWatch (runtime operational metrics and alerts).
authority_class: A3_JOINT_REVIEW
origin: SUG-20260825-arb
amended_by: ADR-018
```

**Drafted:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2) · 2026-08-25.
**Amended:** 2026-08-31 — ingress hop 1 corrected by `ADR-018` (`SUG-20260831-alb`).
Human T4 Architecture sign-off outstanding. Deepali jointly owns perimeter security policy.

---

## ADR-017 — One platform error contract: service-attributed errors, two renderings, one incident id

```yaml
id: ADR-017
status: PROPOSED
problem: >
  Errors are not attributable and not safe. ServiceErrorResponse carries no service, origin or
  layer, so a 502 observed at the BFF cannot be traced to the service that produced it. Upstream
  1SB text and internal routes are returned to callers verbatim (OneSbErrorNormaliser sets
  .detail(parsed.detail()); throw sites set .detail("1SB call failed: " + method + " " + path)).
  Three services run three different exception handlers with three different contracts, the same
  condition is worded differently at each throw site, and the ~60 codes catalogued in
  journey-execution/04 are largely unimplemented — ErrorCodes defines 24 that barely intersect it.
  There is no consistently tagged platform error series, so no error dashboard can be built.
context_stage: "WS-3 at S08 Engineering Foundation; requirement raised by Mahesh 2026-08-27 for release zero"
decision: >
  Adopt one platform error contract, specified in
  docs/journey-execution/07-PLATFORM-ERROR-CONTRACT.md, hardening the existing
  libs/bank-common-error and libs/bank-common-observability modules rather than adding a framework.
  1. Every error carries four coordinates — code (what), service (who), layer (where),
     category (class) — plus origin when it did not begin in the responding service.
  2. A code's HTTP status, retryability, public wording, audit behaviour and runbook are declared
     ONCE in an error registry seeded from catalogue 04, never at a throw site. CI diffs the
     registry against the catalogue in both directions.
  3. One failure produces one incidentId, generated at first failure and preserved across every
     hop. It is shown to the end user and printed on every log line, and it is the join key L1
     support uses.
  4. Two renderings. The public rendering (RFC 7807 + code, category, incidentId, correlationId)
     is safe by construction: title and detail come from the registry, never from a throw site and
     never from an upstream body. The diagnostic rendering (service, layer, component, operation,
     origin, reason, upstream, causeChain, remediation, runbook) goes to logs and internal hops only.
  5. The BFF (L4) is the redaction boundary: the last hop that may hold a diagnostic, the first
     that must never emit one. Enforced by test, not by convention.
  6. Propagation preserves incidentId and the first origin transitively. A compliance refusal the
     RM can act on propagates as itself; a dependency failure wraps as UPSTREAM_*. Re-wrapping a
     dependency failure as INTERNAL_ERROR is forbidden — INTERNAL means our defect, and conflating
     the two destroys the only signal that says whose defect it is.
  7. One platform counter, bank.error.count{service, code, category, layer, originService,
     retryable, httpStatus}. Every tag is a bounded enum or registered service id; no tag is ever
     a message, identifier or path.
  8. Log level follows category: client-caused categories WARN without a stack, platform-caused
     categories ERROR with one.
  9. Additive only. No existing ErrorCodes value is renamed or removed (trigger G9); ErrorCodes is
     documented as partner-consumed. Any proposal to change an existing value is T4 and stops.
consequences: >
  Positive — an error names its origin service and layer; end users get safe text while L1/L2 and
  engineers get a complete diagnostic under one incident id; the catalogue becomes executable
  rather than paper; S08-G7 becomes provable by asserting over a finite registry instead of over
  every log statement; an error dashboard becomes buildable from one series.
  Negative — a registry entry is required before a new code can be thrown, which is deliberate
  friction; five services must migrate to one handler; the envelope grows by five fields.
alternatives:
  - option: "Keep per-service handlers, fix the leaking detail strings only"
    rejected_because: "Closes D1/D2 and nothing else. Attribution, wording consistency and
      countability — the substance of the requirement — remain unsolved, and the leak returns at
      the next throw site because nothing prevents it"
  - option: "A new generic cross-cutting error framework module"
    rejected_because: "S08 posture rejects generic frameworks on sight. Two shared libs with three
      existing consumers already exist; this hardens them"
  - option: "Renumber codes into a structured BNK-<SVC>-<CAT>-<NNNN> scheme"
    rejected_because: "Fires G9 — ErrorCodes values are partner-consumed. Catalogue 04's names are
      already the published taxonomy; service attribution belongs in its own field, not smuggled
      into the code string"
risk_tier: T3
authority_class: A3_JOINT_REVIEW
origin: "human:Mahesh · SUG-20260827-err · EPIC-001"
```

**Drafted:** agent, for Mahesh — Principal Insurance Platform Architect (Board 1 / R2) · 2026-08-27.
Board verdicts outstanding: Architecture, Technical, Product, QA, Security, Risk & Compliance,
Operations (T3, seven boards per [`11 §3`](../../governance/11-REVIEW_GATES.md#3-proportionality--which-boards-are-mandatory)).
Deepali jointly owns the redaction boundary (§4.4) and the PII allow-list (§8); Shivanshi owns the
metric tag set (§7). Contract: [`07-PLATFORM-ERROR-CONTRACT.md`](../../journey-execution/07-PLATFORM-ERROR-CONTRACT.md).

---

## ADR-018 — North-south ingress is SaaS Cloudflare → SaaS F5-XC → API Gateway → Internal ALB (no public ALB)

```yaml
id: ADR-018
status: PROPOSED
problem: >
  ADR-016 hop 1 and the 2026-08-25/27 diagrams assumed an External / public ALB in front of
  Amazon API Gateway, and drew Cloudflare and F5 as if they sat on AWS or in a platform VPC.
  The existing AU Bank application architecture (v1.4, 9-July-2026) treats Cloudflare and
  F5-XC as external SaaS outside the AWS Cloud box. The existing Central Network Account
  Architecture V1 inspects east-west and internet egress through the AU-CTO-NETWORK EDGE VPC
  (FortiGate NGFW) and Transit Gateway — it does not place an F5 appliance in a spoke VPC.
  A public ALB in front of API Gateway is a hop the insurance platform does not need and
  that the human Architecture owner has now withdrawn.
context_stage: "WS-3 at S08/S09; correction against existing bank estate, 2026-08-31"
decision: >
  1. North-south (customer / RM / partner) ingress is exactly:
     device -> Cloudflare Enterprise (SaaS, not AWS, not in any VPC)
            -> F5 Distributed Cloud / F5-XC (SaaS WAF, not AWS, not in any VPC)
            -> Amazon API Gateway (AWS managed regional service; not in the workload VPC)
            -> VPC Link
            -> Internal ALB (the only load balancer, and the only hop inside the VPC)
            -> nip-web / #2 NIP BFF.
  2. Do not provision a public or External ALB in front of API Gateway. The current banking
     application's Public ALB in the DMZ is that application's AWS entry; this platform's
     AWS entry is API Gateway.
  3. Cloudflare and F5 are bank-enterprise SaaS. Diagrams must draw them outside the AWS
     region box and outside every VPC. F5 on this estate is F5-XC (Distributed Cloud), not
     an F5 BIG-IP appliance we place in AWS. SUG-20260827-tpo's in-VPC F5 firewall icon is
     retracted; egress inspection remains ADR-010 (AWS Network Firewall in the inspection
     VPC) pending ASM-012 (whether we also share the existing EDGE FortiGate path).
  4. Inbound remains two proxies: API Gateway, then the Internal ALB. SaaS hops are the
     bank perimeter, not additional AWS reverse proxies.
  5. Payment callbacks still enter on a separate API Gateway route (TB-6). Customer payment
     does not traverse the RM ingress chain.
  6. This amends ADR-016 decision clause 1 only. EBS, GitLab, Terraform, CloudTrail and
     CloudWatch clauses of ADR-016 are unchanged.
authority_class: A3_JOINT_REVIEW
origin: SUG-20260831-alb
amends: ADR-016
```

**Drafted:** agent, for Mahesh — Principal Insurance Platform Architect (Board 1 / R2) · 2026-08-31.
Human T4 Architecture sign-off outstanding. Deepali jointly owns the perimeter security outcome;
Shivanshi owns the S09 landing-zone request that must no longer ask for a public ALB.
Evidence: existing AU Bank application architecture v1.4 (Atul Singh, reviewed Manish Salaria,
9-July-2026); Central Network Account Architecture V1 (AU_AWS_MAS, Mumbai + Hyderabad EDGE VPC).
