# WS-3 — AU Bank Insurance Distribution Platform · Workstream Charter

**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-16
**Raised under:** [CR-010](../change-requests/CR-010-context-module-and-safe-autopilot.md) · Product verdict: [board-3-product-rajal.md](../change-requests/CR-010/verdicts/board-3-product-rajal.md)
**Fixes:** [GAP-D — the platform is not a governed workstream](../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md#gap-d--the-platform-is-not-a-governed-workstream--structural)

> **Status: PROPOSED CONTENT, NOT YET STATE.** I cannot edit
> [`state/CURRENT-STATE.yaml`](../state/CURRENT-STATE.yaml) — under
> [04-STAGE_GATES §5](../04-STAGE_GATES.md) `current_phase` and `stage_status` are human-only, and
> under [04-GATE-AND-SIGNOFF-MODEL §4](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#4-transition-procedure)
> only Mahesh and I *jointly* may write it. This document is the Product-authored source the
> orchestrator transcribes, after Architecture ratification. Until transcribed, WS-3 does not exist
> in the execution model and platform work continues to triage as out-of-scope.

---

## 1. Why this workstream must exist

[`CURRENT-STATE.yaml`](../state/CURRENT-STATE.yaml) defines WS-1 (1SB Integration) and WS-2
(Workforce Auth). Both are real and both are well run. **Neither of them is the product.**

Governance evaluates stage fit against a workstream ([Rule LC-1](../03-LIFECYCLE.md)). If the
platform is not a workstream, then platform foundation work — application CI, IaC, the Consent
service, the Flutter application — belongs to no workstream and therefore triages as **SC2/SC3:
out of scope**. The framework has been correctly and faithfully excluding the foundation from
scope, because the thing the foundation belongs to was never registered.

Registering WS-3 is not an administrative act. It is the single change that makes the missing work
admissible.

---

## 2. Platform objective and release slice

### 2.1 Objective

> Give AU SFB, under IRDAI Composite Corporate Agent licence CA0515, a **bank-owned multi-insurer
> insurance distribution platform in which the bank retains visibility and control across the whole
> sale** — from lead to issued, reconciled, audited policy — replacing the AU Beema Portal model
> in which the bank loses all visibility at the point of redirect.

Source: [`business-problem-statement.md §3`](../../context/business-problem-statement.md) ·
[`00-project-charter.md`](../../au-bank-insurance-platform/00-project-charter.md) ·
[`02-product-vision-and-outcomes.md`](../../au-bank-insurance-platform/02-product-vision-and-outcomes.md)

### 2.2 The release slice this workstream is cut to — R0

WS-3 exists to deliver **R0**, and R0 is one sentence:

> **One RM sells one Term Life policy to one ETB customer from one Group A insurer, end to end,
> through a real interface, with consent and suitability evidence, payment on the customer's own
> device, an issued and reconciled policy, and a complete audit trail.**

R0 is deliberately narrower than the requirement baseline
([`R0-SCOPE.md`](../../au-bank-insurance-platform/requirements/R0-SCOPE.md) v0.3 carries RM +
self-service + hybrid from Day 1). **Product direction is now assisted-first**, per
[S04 §6](../../application-lifecycle-bible/stages/S04-product-definition.md#6-current-position-in-this-repository---partial):
DIY follows the proven assisted journey; hybrid follows stable assisted *and* DIY paths. DIY and
hybrid are not removed from the product — they are sequenced behind a journey that has been
demonstrated once. See [S04 evidence](../../application-lifecycle-bible/evidence/S04-product-definition-evidence.md)
for the full R0 matrix and the rationale.

**R0 is a release slice, not a component list.** [S04-VT-01](../../application-lifecycle-bible/stages/S04-product-definition.md#4-validation-tests)
applies: a policy can be sold using only R0, by a real RM.

---

## 3. Scope

### 3.1 In scope

| # | Item | Why it is in R0 |
|---|---|---|
| 1 | Foundation Recovery Increment: S08 engineering foundation and S09 platform foundation | Nothing downstream can be *evidenced* without them |
| 2 | Consent capture and evidence (bounded context #6), per [consent rule pack](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) | Legal precondition for processing customer data |
| 3 | Suitability & need analysis (#7) including the quote hard-gate, per [suitability rule pack](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) | Bypassing suitability before quote is illegal on our own baseline |
| 4 | Lead service (#5) — create, resume, status, convert, archive | Entry point; working inbox, not the 7-year bag |
| 5 | Customer service (#4), thin — CBS/CIF lookup and prefill for ETB | ETB-only segment makes this the identity path |
| 6 | Product catalogue (#8), R0 matrix only — Life, Group A, Term | Quote needs an eligible-product answer |
| 7 | Journey orchestration (#9), R0 state machine only | Holds the gate sequence together |
| 8 | Quotation (#10) via the existing 1SB path | Already largely built in WS-1 |
| 9 | Proposal & UW tracking (#11), thin | Required to reach issuance |
| 10 | Payment (#12) — AU Bank PG, customer-device only | RBI device isolation |
| 11 | Policy & issuance (#13) — visibility, issuance history, `issuanceMode`, off-platform ingest | "Sold" is defined at issuance; MIS book capture |
| 12 | Audit & compliance (#16), append-only evidence store | Every rule pack depends on it |
| 13 | RM workspace BFF (#2) and the **Flutter RM application** for the R0 journey | Every journey currently terminates at an interface that does not exist |
| 14 | Identity & access (#3) — consumed from WS-2, not rebuilt | See §6 |
| 15 | Integration Hub (#14) and 1SB Adapter (#15) — consumed from WS-1, not rebuilt | See §5 |
| 16 | R0 experience design: service blueprint, screen inventory, states, design system | S05 is the emptiest stage and S11 cannot start without a thin slice of it |
| 17 | Reporting & MIS (#18) — R0 business reports on the isolated read path | Stakeholders require day-one reports; not on the Lead writer |
| 18 | Administration UI (#19) — R0 maker-checker config and report access | Layer already in W0b; UI pulled into R0 by CR-013 |
| 19 | Off-platform / portal Policy ingest (`source=OFF_PLATFORM`) | Completes the book; never `lead.create` |
| 20 | `issuanceMode` STP / NON_STP / INSTA on Proposal and Policy | One saga, three modes |
| 21 | PPHI 2024 control-to-seam map (Board 6 condition) | Compliance is the only remaining gate |

### 3.2 Out of scope, with revisit triggers

Every excluded item names the release or condition that revisits it. This list is what AIGEM
scope-fit triage consults for the rest of the programme's life
([S04-VT-03](../../application-lifecycle-bible/stages/S04-product-definition.md#4-validation-tests)).

| Item | `revisit_at` |
|---|---|
| Customer self-service (DIY) journey | R1 — after the assisted journey completes a real sale in pilot |
| Hybrid journey and assisted↔DIY mode switching | R2 — after assisted and DIY both have stable state and hand-off contracts |
| Group B insurers: catalogue entry + controlled redirect | R1 |
| ULIP and Savings/Endowment product classes | R1 — suitability model already covers them ([pack §4.3–4.4](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#43-savings--endowment)) |
| Customer BFF (#1) and the customer-facing Flutter surface | R1, with DIY |
| Notification service (#17) beyond OTP and payment-link delivery | R1 |
| Lead campaign and bulk origination (not single-RM create, not MIS policy ingest) | R1 |
| Renewals and servicing (BR-SERV) | R2+ |
| The remaining bounded contexts not listed in §3.1 | S13 — justified by the working slice, never by the diagram |
| Health, Motor, Travel and other non-life LOBs | R2+ **and** only after WS-1 Phase 5 is unfrozen (§4) |
| New-to-Bank (NTB) onboarding and V-KYC | R2+ |
| Multi-aggregator routing | Extensibility only; revisit on evidence of a second aggregator commitment |
| Branch kiosk journey | Pending a business decision (GAP-033) |
| Vernacular / multi-language content | R1 — `hi-IN` first |
| Consolidated executive control tower | R2, after the funnel produces real data |

### 3.3 Never

Not "later". Never — so they stop being re-proposed.

| Item | Why |
|---|---|
| A quote generated without a valid suitability evaluation ID | Illegal on our own baseline; [SUIT-R20](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#1-the-gate-stated-once-precisely) |
| Payment executed on an RM or bank-employee device | RBI cyber-security prohibition; [D-006](../../au-bank-insurance-platform/DECISION-LOG.md) |
| `distributorId` sourced from a caller-supplied value | Multi-tenant attribution spoofing |
| Consent recorded without a verified customer-device OTP | [CNS-R10, CNS-R13](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#4-capture-mechanism-by-channel) |
| Mutable or deletable consent, suitability or audit evidence | [CNS-R16](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#51-immutability), [SUIT-R23](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#5-the-evaluation-record) |
| Regulated data, backups, logs or archives outside AWS India regions | Non-waivable; [gate model §8](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#8-waivers-and-exceptions) |
| `Policy Sold` inferred from quote, proposal or payment alone | [D-007](../../au-bank-insurance-platform/DECISION-LOG.md); four conditions, all four |
| Claims administration or insurer underwriting decisioning | GAP-029, GAP-030 — explicitly out |
| An insurer or aggregator API defining the bank's canonical journey | [WD §18](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#18-1silverbullet-positioning) |
| Bank apps or the Flutter client calling 1SB or a database directly | Standing constraint |
| An agentic-AI action that substitutes for a deterministic hard gate | [Rajal persona §5](../../context/roles/principal-insurance-platform-product-owner/01-persona.md#5-repository-specific-context) |

---

## 4. Current stage, gate and the WS-1 Phase 5 stop

### 4.1 Stage

| Field | Value | Basis |
|---|---|---|
| Canonical AIGEM stage | **L4 — Foundation** | [02-STAGE-MODEL §1](../../application-lifecycle-bible/02-STAGE-MODEL.md#1-relationship-to-aigem--read-this-before-using-the-model) maps L4 → S08 + S09 |
| Current phase (S-model) | **S08 — Engineering Foundation** | [S08 §6](../../application-lifecycle-bible/stages/S08-engineering-foundation.md#6-current-position-in-this-repository---missing): missing at baseline |
| Stage status | `IN_PROGRESS` | CR-010 adds the application-CI workflow; a green run does not yet exist |
| Next stage | **S09 — Platform & Environment Foundation** | Runs overlapping from week 4 per [realignment §2](../../application-lifecycle-bible/03-REALIGNMENT-PLAN.md#move-3--underpin-execute-s08-and-s09--810-weeks) |

**Retroactive entry applies.** Under [Rule SM-3](../../application-lifecycle-bible/02-STAGE-MODEL.md#53-the-back-fill-rule),
S00–S05 are entered late, not skipped. Their retroactive evidence is in
[`application-lifecycle-bible/evidence/`](../../application-lifecycle-bible/evidence/README.md).
S06 and S07 are Mahesh's to assess.

### 4.2 Gate — `GATE-S08`

Criteria are transcribed from [S08 §5](../../application-lifecycle-bible/stages/S08-engineering-foundation.md#5-exit-gate--gate-s08).
States are Product's reading of the evidence as at 2026-08-16; **Amit, Swapnali, Deepali and
Shivanshi own the authoritative state of their own criteria** and may correct any row.

| # | Criterion | Level | State | Owner |
|---|---|---|---|---|
| S08-G1 | CI builds and tests every module on every PR | E4 | `OPEN` — workflow added under CR-010; no green run evidenced | Amit |
| S08-G2 | Merge to `main` impossible without a green pipeline | E4 | `OPEN` — branch protection unverified | Amit + Shivanshi |
| S08-G3 | Coverage thresholds enforced; QA-001 closed | E4 | `BLOCKED` on S08-G1 | Swapnali |
| S08-G4 | ArchUnit and static analysis enforced in CI | E4 | `BLOCKED` on S08-G1 | Amit |
| S08-G5 | Secret, SAST, SCA and image scanning in the pipeline | E4 | `OPEN` — none present at baseline | Deepali |
| S08-G6 | Test infrastructure operational at every pyramid level | E4 | `OPEN` — Testcontainers, WireMock, contract and E2E all absent; TD-014 | Swapnali + Amit |
| S08-G7 | No PII in logs, proven by automated test | E4 | `OPEN` — masking converter exists; nothing proves it works | Deepali + Swapnali |
| S08-G8 | Engineering and secure coding standards published and adopted | E2 | `OPEN` | Amit |
| S08-G9 | Pipeline feedback < 10 min at p95, flake < 1% | E4 | `BLOCKED` on S08-G1 (no run history) | Shivanshi |
| S08-G10 | A new engineer can build, test and ship in under a week | E3 | `OPEN` | Amit + Shivanshi |

**Gate state: `BLOCKED`.** Four criteria are blocked on S08-G1, which is itself blocked on evidence
that does not yet exist. [Rule GS-3](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#blocked-is-underused-and-matters-here)
requires that a criterion whose blocker is a missing prerequisite is marked `BLOCKED` and the
prerequisite named — not left `OPEN` as though effort will close it.

**Approvers (from [gate model §5](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#5-approver-map-by-stage)):**
Amit (AP) · Swapnali (AP/B) · Mahesh (AP) · Deepali (AP/B) · Shivanshi (AP) · Aarti (RV) ·
Rajal (RV) · Kalpana (RV).

At S08 **I am a reviewer, not an approver.** I am recording this gate because Product owns the
workstream registration, not because Product signs the engineering foundation.

### 4.3 The stop on WS-1 Phase 5

| Action | Effect |
|---|---|
| WS-1 Phase 5 (Health → Motor LOB expansion) **does not start** | `WS-1.lifecycle.next_stage` remains "Phase 5" as a *destination*, not an authorisation to begin |
| Unfreeze condition | GATE-S08 `PASSED` **and** GATE-S11 `PASSED` for the R0 Term journey |
| Explicitly **not** stopped | WS-2 IAM foundation; WS-1 criteria 4.4 (compliance review) and 4.5 (runbook), which need no CI; all documentation and rule-pack work |

**Reasoning.** Adding Health and Motor to a quote path that lacks its lawful suitability gate
multiplies a compliance defect across three lines of business. It does not deliver value; it
increases exposure. This is a Product scope decision within my authority (PO2 — Product-led with
consultation), taken with Kalpana on sequencing.

### 4.4 Completed stages

None, for WS-3, and stating otherwise would be the same error this workstream exists to correct.
S01 is retroactively assessable as substantially complete (see
[S01 evidence](../../application-lifecycle-bible/evidence/S01-discovery-evidence.md)); no WS-3 stage
gate has been signed by its approvers, so none is `PASSED`.

---

## 5. WS-1 re-parented as the supplier/adapter workstream

| | Before | After |
|---|---|---|
| What WS-1 is | The de-facto programme | A **supplier/adapter workstream feeding WS-3** |
| Bounded contexts owned | ambiguous | **#14 Integration Hub** and **#15 1SB Adapter** |
| Lifecycle status | L7 — Hardening | **L7 — Hardening, and that remains true** |

> **WS-1's L7 status stays true — for a component.** This is not a demotion and not a correction of
> anyone's assessment. The 1SB integration service is roughly 147 files of correct,
> boundary-enforced, architecturally sound work, and hardening is the right stage *for an adapter*.
> What changes is what the status is a status **of**. WS-1 is at L7 for bounded contexts #14 and
> #15. The platform is at S08. Both statements are accurate simultaneously, and the confusion
> between them is [mechanism 1](../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md#7-how-this-happened--so-it-does-not-recur)
> of how this happened.

**Consequences of re-parenting:**

1. WS-1's product decisions are made **against WS-3's journey requirements**, not independently.
   A 1SB capability with no WS-3 consumer is not R0 scope.
2. WS-1 gate criterion 4.3 ("at least one bank caller exercises quote and proposal against UAT")
   names a bank caller. **WS-3 is that caller.** The criterion is currently `BLOCKED` on DEP-001
   and DEP-002 and owned by me; re-parenting makes the dependency explicit rather than external.
3. Standing constraints on WS-1 (no 1SB types outside `adapter.onesb.*`, no Flyway/JPA in the
   integration service, persistence is platform-common) are inherited by WS-3 unchanged.
4. WS-1 keeps its own gate, its own backlog and its own routing. Re-parenting is a *dependency*
   relationship, not a merge.

---

## 6. WS-2 as a platform identity enabler

WS-2 (Workforce Authentication & Authorization) delivers **bounded context #3, workforce half**:
RM and insurer-representative identity, the token-hiding BFF session, the provider-neutral IdP
adapter, and `identity-authorization-service` as the PDP.

| WS-3 needs | WS-2 supplies | Gap |
|---|---|---|
| RM authentication for the R0 journey ([BR-SEC-010](../../au-bank-insurance-platform/requirements/BRD-P0-CAPABILITIES.md#br-sec-010--rm-authentication)) | Bank AD path behind the adapter | Bank AD federation is WS-2 Phase 2 |
| A stable `actorId` for every audit and evidence record | BFF session identity | — |
| RBAC/ABAC decisions for lead ownership and quote authority ([BR-SEC-020](../../au-bank-insurance-platform/requirements/BRD-P0-CAPABILITIES.md#br-sec-020--role-based-access)) | `identity-authorization-service` PDP, default deny | — |
| **`agentId` — the RM's IRDAI SP licence — and its expiry behaviour** | Certification metadata is in WS-2's in-scope list | **GAP-014 open**: sourcing and expired-certification behaviour undefined |
| Customer identity for DIY | **Not supplied** — retail-customer authentication is explicitly out of WS-2 scope | Deferred with DIY to R1 |

**WS-3 consumes WS-2; it does not rebuild identity.** The one item WS-3 must drive to closure is
GAP-014, because [SUIT-R21](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#5-the-evaluation-record)
and [BR-PROP-030](../../au-bank-insurance-platform/requirements/BRD-P0-CAPABILITIES.md#br-prop-030--submit)
both require `agentId` on regulated records, and neither can be satisfied by an identity system
that does not know whether the RM's certification is current.

---

## 7. Exact YAML to transcribe into `CURRENT-STATE.yaml`

Mirrors the WS-1 and WS-2 shape exactly. Insert after the WS-2 block.

```yaml
  - id: WS-3
    name: "AU Bank Insurance Distribution Platform"
    authority:
      - "docs/governance/workstreams/WS-3-PLATFORM-CHARTER.md"
      - "docs/application-lifecycle-bible/01-POSITION-ASSESSMENT.md"
      - "docs/application-lifecycle-bible/03-REALIGNMENT-PLAN.md"
      - "docs/au-bank-insurance-platform/requirements/R0-SCOPE.md"
      - "docs/au-bank-insurance-platform/requirements/BRD-P0-CAPABILITIES.md"
      - "docs/context/business-problem-statement.md"

    lifecycle:
      canonical_stage: "L4 — Foundation"
      current_phase: "S08 — Engineering Foundation (Foundation Recovery Increment)"
      stage_status: IN_PROGRESS
      next_stage: "S09 — Platform & Environment Foundation"

    current_objective:
      id: R0-ASSISTED-TERM-SALE
      description: >
        One RM sells one Term Life policy to one ETB customer from one Group A insurer,
        end to end, through a real interface, with consent and suitability evidence,
        payment on the customer's own device, an issued and reconciled policy, and a
        complete audit trail.

    current_deliverable:
      description: >
        Foundation Recovery Increment: application CI with enforced quality, security and
        architecture gates (S08); IaC, environments, secrets, observability and 7-year
        write-once retention in ap-south-1 (S09). Product-side in parallel: consent and
        suitability rule packs, R0 acceptance criteria and traceability, R0 product matrix
        and quote rules, and the R0 service blueprint, screen inventory and design system.

    current_scope:
      in_scope:
        - "Foundation Recovery Increment: S08 engineering foundation and S09 platform foundation"
        - "Consent capture and evidence (context #6) per the consent rule pack"
        - "Suitability and need analysis (context #7) including the quote hard-gate"
        - "Lead service (context #5) — create, resume, status, convert, archive (working inbox; attribution fields retained)"
        - "Customer service (context #4) — CBS/CIF lookup and prefill for ETB"
        - "Product catalogue (context #8) — R0 matrix only: Life, Group A, Term"
        - "Journey orchestration (context #9) — R0 state machine"
        - "Quotation (context #10) via the existing 1SB path"
        - "Proposal and UW tracking (context #11), thin"
        - "Payment (context #12) — AU Bank PG, customer device only"
        - "Policy and issuance (context #13) — visibility, issuance history, issuanceMode, off-platform ingest"
        - "Audit and compliance (context #16) — append-only evidence store"
        - "RM Workspace BFF (context #2) and the Flutter RM application for the R0 journey"
        - "R0 experience design: service blueprint, screen inventory, states, design system"
        - "Reporting and MIS (context #18) — R0 business reports on the isolated read path"
        - "Administration UI (context #19) — R0 maker-checker config and report access"
        - "Off-platform / insurer-portal sale ingest (Policy source=OFF_PLATFORM; never lead.create)"
        - "issuanceMode STP | NON_STP | INSTA on Proposal and Policy"
        - "PPHI 2024 control-to-seam mapping (Board 6 condition C-PPHI-1)"

      out_of_scope:
        - item: "Customer self-service (DIY) journey"
          revisit_at: "R1 — after the assisted journey completes a real sale in pilot"
        - item: "Hybrid journey and assisted/DIY mode switching"
          revisit_at: "R2 — after assisted and DIY both have stable state and hand-off contracts"
        - item: "Group B insurers: catalogue entry and controlled redirect"
          revisit_at: "R1"
        - item: "ULIP and Savings/Endowment product classes"
          revisit_at: "R1"
        - item: "Customer BFF (context #1) and the customer-facing Flutter surface"
          revisit_at: "R1, with DIY"
        - item: "Notification service (context #17) beyond OTP and payment-link delivery"
          revisit_at: "R1"
        - item: "Lead campaign and bulk origination (not single-RM create, not MIS policy ingest)"
          revisit_at: "R1"
        - item: "Renewals and servicing"
          revisit_at: "R2+"
        - item: "Bounded contexts not listed in in_scope"
          revisit_at: "S13 — justified by the working slice, not by the diagram"
        - item: "Health, Motor, Travel and other non-life LOBs"
          revisit_at: "R2+, and only after WS-1 Phase 5 is unfrozen"
        - item: "New-to-Bank onboarding and V-KYC"
          revisit_at: "R2+"
        - item: "Multi-aggregator routing"
          revisit_at: "evidence of a second aggregator commitment; extensibility only"
        - item: "Branch kiosk journey"
          revisit_at: "pending business decision (GAP-033)"
        - item: "Vernacular / multi-language content"
          revisit_at: "R1 — hi-IN first"
        - item: "Consolidated executive control tower"
          revisit_at: "R2, after the funnel produces real data"

      never:
        - "A quote generated without a valid suitability evaluation id"
        - "Payment executed on an RM or bank-employee device"
        - "distributorId sourced from a caller-supplied value"
        - "Consent recorded without a verified customer-device OTP"
        - "Mutable or deletable consent, suitability or audit evidence"
        - "Regulated data, backups, logs or archives outside AWS India regions"
        - "Policy Sold inferred from quote, proposal or payment alone"
        - "Claims administration or insurer underwriting decisioning"
        - "An insurer or aggregator API defining the bank's canonical journey"
        - "Bank apps or the Flutter client calling 1SB or a database directly"
        - "An agentic-AI action substituting for a deterministic hard gate"

    current_gate:
      id: GATE-S08
      state: BLOCKED
      exit_criteria:
        - id: "S08-G1"
          criterion: "CI builds and tests every module on every PR"
          state: OPEN
          owner: "Amit / Engineering"
        - id: "S08-G2"
          criterion: "Merge to main is impossible without a green pipeline"
          state: OPEN
          owner: "Amit / Engineering + Shivanshi / SRE"
        - id: "S08-G3"
          criterion: "Coverage thresholds enforced; QA-001 closed"
          state: BLOCKED
          owner: "Swapnali / QA"
          blockers: ["S08-G1"]
        - id: "S08-G4"
          criterion: "ArchUnit and static analysis enforced in CI"
          state: BLOCKED
          owner: "Amit / Engineering"
          blockers: ["S08-G1"]
        - id: "S08-G5"
          criterion: "Secret, SAST, SCA and image scanning in the pipeline"
          state: OPEN
          owner: "Deepali / Security"
        - id: "S08-G6"
          criterion: "Test infrastructure operational at every pyramid level"
          state: OPEN
          owner: "Swapnali / QA + Amit / Engineering"
          blockers: ["TD-014"]
        - id: "S08-G7"
          criterion: "No PII in logs, proven by automated test"
          state: OPEN
          owner: "Deepali / Security + Swapnali / QA"
        - id: "S08-G8"
          criterion: "Engineering and secure coding standards published and adopted"
          state: OPEN
          owner: "Amit / Engineering"
        - id: "S08-G9"
          criterion: "Pipeline feedback under 10 minutes at p95; flake under 1%"
          state: BLOCKED
          owner: "Shivanshi / SRE"
          blockers: ["S08-G1"]
        - id: "S08-G10"
          criterion: "A new engineer can build, test and ship in under a week"
          state: OPEN
          owner: "Amit / Engineering + Shivanshi / SRE"
      approvers: ["Amit / Engineering", "Swapnali / QA", "Mahesh / Architecture",
                  "Deepali / Security", "Shivanshi / SRE"]

    completed_stages: []

    depends_on:
      - workstream: WS-1
        relationship: "supplier — bounded contexts #14 Integration Hub and #15 1SB Adapter"
      - workstream: WS-2
        relationship: "enabler — bounded context #3 workforce identity and authorization"

    entry_conditions:
      - stage: S11
        condition: "No open P0 business gap (Rule SM-4). GAP-006 and GAP-007 must be CLOSED, which requires Shailja's signature on both rule packs."
```

> **Note on `depends_on` and `entry_conditions`.** Neither key exists in the WS-1 or WS-2 blocks
> today. They carry information the schema currently has nowhere to put — the supplier relationship
> and the S11 freeze — and I would rather propose two additive keys than record the relationships
> in prose that no validator reads. If Mahesh prefers to keep the schema unchanged, drop both keys;
> the relationships are stated normatively in §5, §6 and §8 of this charter, which is named in
> `authority`. **This is Architecture's call, not mine.**

---

## 8. The S11 entry condition

> **Binding: no WS-3 stage may enter S11 while GAP-006 or GAP-007 is open.**

This makes the "block scope / build freeze" label on those two P0 gaps
([gap register](../../au-bank-insurance-platform/po-drive/02-GAP-REGISTER.md)) actually freeze
something. Under [Rule SM-4](../../application-lifecycle-bible/02-STAGE-MODEL.md#54-freeze-semantics)
an open P0 business gap freezes S11 and everything after it — **not S08 and not S09**, because
foundation work is what makes the gap closable.

| Gap | What closes it | Owner | State at 2026-08-16 |
|---|---|---|---|
| GAP-006 | [Consent rule pack](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) signed by Shailja (E2) | Shailja + Rajal | **Content-complete, ratification-pending** |
| GAP-007 | [Suitability rule pack](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) signed by Shailja (E2) | Shailja + Rajal | **Content-complete, ratification-pending** |

**Content-complete is not closed.** Both packs are drafted, both are testable, and neither is
signed. I am not entitled to record a Compliance conclusion, and an AI drafting Shailja's reasoning
does not discharge a mandatory human signature. The gaps stay open until she signs.

---

## 9. Ownership

| Owner | Owns |
|---|---|
| **Rajal — Product (Board 3 / R1)** | Product vision and R0 value proposition · scope: in / out with revisit triggers / never · business capability priority and backlog order · journey intent for RM, customer and operations · business rules and expected behaviour · product acceptance criteria · the product matrix and quote rules · business readiness · R0 KPIs and outcome measurement · product-side insurer onboarding requirements · **acceptance or rejection of delivered product behaviour** |
| **Kalpana — Delivery (R12)** | Sequencing and the critical path · the Foundation Recovery Increment as a funded budget line · capacity allocation and feasibility · gate `CANDIDATE` marking and decision-forcing (Rule PA-1) · dependency register and blocker escalation · the feature freeze on `services/` outside recovery scope · evidence audit of existing "Done" claims |

**Neither of us owns**: service topology or technology selection (Mahesh) · security control
adequacy or exceptions (Deepali) · regulatory permissibility (Shailja) · test sufficiency
(Swapnali) · operational readiness (Shivanshi) · physical data design (Aarti) · material risk
acceptance (a named human).

**Joint, and only joint:** Mahesh + Rajal mark a stage `PASSED` and edit `CURRENT-STATE.yaml`
([gate model §4](../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#4-transition-procedure)).

---

## 10. Routing — where admitted WS-3 work goes

### 10.1 The constraint I am not going to break

[`06-WORK_CLASSIFICATION §2`](../06-WORK_CLASSIFICATION.md) defines exactly **sixteen** canonical
work types, and `ci-checks.py` asserts that every workstream's routing map is closed over exactly
those sixteen keys — no more, no fewer. That closure is deliberate: it stops a new work type being
added to the model without also being given a home.

So the answer to *"where does a Flutter story or a Terraform module go?"* is **not** a new `UI/UX`
or `IaC` work type. It is a WS-3 routing block, closed over the same sixteen keys, whose
destinations make those work types land somewhere legitimate.

| Platform work the model had no home for | Canonical type | Lands in |
|---|---|---|
| A Flutter screen implementing an R0 requirement | `FUNC` | WS-3 product backlog |
| A design-system token or component spec | `DOC` | Suggestion register → S05 evidence |
| A Terraform module for VPC, KMS or EKS | `INFRA` | WS-3 product backlog + ADR log |
| A CI pipeline stage or branch-protection rule | `INFRA` | WS-3 product backlog |
| A test harness, contract test or coverage gate | `QA` | WS-3 product backlog |
| An S3 Object Lock retention configuration | `COMP` | WS-3 product backlog + risk register |
| A screen-level accessibility conformance fix | `FUNC` | WS-3 product backlog |

If a future work type genuinely cannot be expressed in the sixteen, that is a CR against
`06-WORK_CLASSIFICATION` — **Architecture's document, not mine.** I am not proposing one.

### 10.2 Proposed YAML

Insert as a third top-level key under `routing:`, before `CROSS-CUTTING`.

```yaml
  WS-3:
    FUNC: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    BUG: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    NFR: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    ARCH: ["docs/platform/architecture-review/08-architecture-decision-log.md", "docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    SEC: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md", "docs/governance/registers/RISK-REGISTER.md"]
    COMP: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md", "docs/governance/registers/RISK-REGISTER.md"]
    INFRA: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    DEBT: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    REFACTOR: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    QA: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    OPS: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    SPIKE: ["docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    DOC: ["docs/governance/registers/SUGGESTION-REGISTER.md"]
    MIGRATION: ["docs/platform/architecture-review/08-architecture-decision-log.md", "docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    GOV: ["docs/governance/", "docs/au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md"]
    IDEA: ["docs/governance/registers/PARKED-BACKLOG.md"]
```

**Every path above exists today**, so CR-010's path validation passes on transcription. That is a
deliberate constraint on this proposal: routing to a file that does not exist fails validation and
leaves the workstream unroutable, which would be worse than a coarse-grained but working map.

### 10.3 Refinement, once the files exist

`03-PROGRAMME-TODO.md` is a Product master checklist, not a delivery backlog, and routing eleven
work types to it is coarse. The refinement below is proposed as a **follow-on**, conditional on
Kalpana creating the files as part of the Foundation Recovery Increment — routing must not point at
paths that do not exist yet.

| Key | Refined destination | Created by |
|---|---|---|
| `FUNC`, `BUG`, `NFR`, `SPIKE` | `docs/au-bank-insurance-platform/po-drive/WS3-PRODUCT-BACKLOG.md` | Rajal |
| `INFRA`, `OPS` | `docs/au-bank-insurance-platform/po-drive/WS3-FOUNDATION-BACKLOG.md` | Kalpana + Shivanshi |
| `QA` | `docs/au-bank-insurance-platform/po-drive/WS3-TEST-BACKLOG.md` | Swapnali |
| `DEBT`, `REFACTOR` | `docs/au-bank-insurance-platform/po-drive/WS3-TECH-DEBT.md` | Amit |

Raise as a separate CR when the files land. **Do not transcribe §10.3 today.**

---

## 11. Ratification

| Authority | Required conclusion | Status |
|---|---|---|
| Rajal / Product (Board 3) | WS-3 objective, R0 slice, scope in/out/never, priority, WS-1 Phase 5 stop, S11 entry condition, ownership | **APPROVED-WITH-MODIFICATION — Rajal, 2026-08-16.** See [board-3-product-rajal.md](../change-requests/CR-010/verdicts/board-3-product-rajal.md) |
| Mahesh / Architecture (Board 1) | Workstream registration, schema shape, `depends_on`/`entry_conditions` keys, routing closure, re-parenting of WS-1 | **PENDING** |
| Kalpana / Delivery (R12) | Sequencing, critical path, Foundation Recovery Increment as a funded line, feature freeze | PENDING — see [r12-delivery-kalpana.md](../change-requests/CR-010/verdicts/r12-delivery-kalpana.md) (AI-drafted) |
| Shailja / Compliance (Board 6) | The S11 entry condition and the non-waivable items in §3.3 | PENDING — human signature mandatory |
| Repository owner / Sponsor authority | Transcription into `CURRENT-STATE.yaml` | **PENDING — human action, orchestrator-owned** |

**Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.
This charter becomes state only when a human transcribes it into `CURRENT-STATE.yaml`. Silence does
not approve it, and its presence in the repository is not its ratification.

— **Rajal**, Principal Insurance Platform Product Owner, 2026-08-16
