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
| CR-002 | 2026-08-11 | SCOPE | Split the monorepo into one repository per microservice, with `bank-insurance-governance` as the parent holding all common documentation and the governance model | **PENDING** | PO + Architect (required); Security Architect before Wave 0 |

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

### CR-002 — federated multi-repository topology with a governance parent

**Full impact analysis:** [`docs/platform/repository-topology/`](../../platform/repository-topology/README.md)
([target topology](../../platform/repository-topology/01-TARGET-TOPOLOGY.md) ·
[governance federation](../../platform/repository-topology/02-GOVERNANCE-FEDERATION.md) ·
[migration plan](../../platform/repository-topology/03-MIGRATION-PLAN.md) ·
[draft ARCH-023](../../platform/repository-topology/04-DRAFT-ADR-ARCH-023.md))

```yaml
change_request:
  id: CR-002
  raised_by: "agent:claude"
  date: 2026-08-11
  type: SCOPE            # also GOV and CONSTRAINT-adjacent — see `also` below
  also: [GOV, PLAN]
  origin: SUG-20260811-r7k

  current_position: >
    One repository. settings.gradle.kts includes 5 libs and 5 services; the root
    build.gradle.kts carries the whole build convention (Java 21, Spring BOM 3.3.4,
    Lombok, JaCoCo and the per-module coverage floors); docs/ carries governance,
    business SSOT, platform specs and module SSOT together. Repository topology is
    named in no authority document's in-scope list and is ratified by no ARCH-xxx
    decision. docs/context/business-problem-statement.md calls the monorepo one of the
    technical architecture decisions to uphold, but docs/README.md classifies context/
    as non-binding.

  proposed_change: >
    Adopt a federated multi-repository topology: nine repositories (parent governance,
    build conventions, common libs, five services, devstack), with the parent consumed
    by every other repository as a git submodule pinned to a release tag at
    .governance/. CURRENT-STATE.yaml and all six registers stay central; service-scoped
    backlogs move with their service. Migration proceeds in waves, with no WS-1 service
    moving before GATE-P4 has passed, and the ~14 not-yet-existing services created in
    their own repositories from the outset. Replacement text is the four documents under
    docs/platform/repository-topology/; ARCH-023 is drafted but not entered in the
    architecture decision log unless this CR is approved.

  driver: >
    Business priority change — a direct instruction from the Solution Architect, plus
    the target platform of ~16 services in ARCH-003, which no single settings.gradle.kts
    will review sensibly. NOT a measured failure: no monorepo pain is recorded in
    TECH-DEBT.md or any phase STATUS file, and the agent's own triage verdict on the
    merits alone is PARK (SF3/SC2/NOT-NOW, P5 now / P2 at L8 Expansion).

  evidence:
    - "ARCH-003 — ~16 domain services + 2 BFFs + routing layer, phased across P0-P3"
    - "19-PORTING_GUIDE section 3 — multi-team platforms keep one shared registers folder"
    - "GATE-P4 criteria 4.1, 4.2 and 4.7 are OPEN/PARTIAL and all run through the build"
    - "CURRENT-STATE.yaml routing already points ARCH/MIGRATION at a path that does not exist"

  impact:
    scope: >
      Adds repository topology to platform scope. Requires a new `repos:` block in
      CURRENT-STATE.yaml and a matching change to current-state.schema.json; requires
      routing destinations to become repo-qualified; requires 19-PORTING_GUIDE to gain a
      federated-consumption section. Each of those is itself a GOV change carried by
      this CR rather than made silently.
    stage: >
      No gate date moves IF the plan's hard barrier is kept: Waves 0, 1 and 4 are
      gate-neutral, and Wave 2 starts only after GATE-P4 has PASSED. Removing that
      barrier is a WAIVER-class decision needing the same approvers (see plan section 7).
    dependencies: >
      Blocked by GATE-P4 for the WS-1 services. Requires a binary artifact registry
      decision (GitHub Packages vs AWS CodeArtifact) — currently unowned and unmade.
      Escalates TD-014 (E2E integration <-> persistence) from parked debt to a Wave 2
      entry condition, because after the split no single CI run proves that seam.
    parked_items: >
      Makes none eligible. Changes TD-014's character from parked to blocking; TD-007
      (ArchUnit allowEmptyShould) is unaffected since those rules live inside modules
      and travel with them.
    effort: "XL — multi-component, roughly 2-3 weeks per wave on engineering judgment"
    risk_if_rejected: >
      None immediate. The monorepo remains viable for the current team and service
      count. The cost of rejection is deferred and grows with the service count: every
      service built in the monorepo is one more to extract later. Note that Wave 4
      (new services born split) captures most of the value and can be approved on its
      own even if Waves 2-3 are refused.

  alternatives_considered:
    - option: "do nothing — remain a monorepo"
      consequence: >
        Genuinely viable today; becomes expensive when independent release cadence or
        multi-team ownership is required. This is the fallback if Wave 1 fails.
    - option: "approve Wave 0 + Wave 4 only (parent repo + new services born split)"
      consequence: >
        Delivers the parent repository, the federation mechanism, and the no-migration
        property for the ~14 future services, without touching WS-1 or WS-2 code at all.
        The lowest-risk way to say yes.
    - option: "split everything now"
      consequence: >
        Rewrites the build during hardening against three open gate criteria, with the
        integration <-> persistence seam unproven until contract tests catch up.
    - option: "copy governance into each repo per 19-PORTING_GUIDE section 2"
      consequence: >
        Nine forks of the pipeline and nine state files — the drift AIGEM exists to
        prevent.

  decision: PENDING
  approvers: []
  decided_on:
  conditions:
    - "Security board review of the artifact-registry supply chain BEFORE Wave 0 (T4)"
    - "Artifact registry owner and decision named before Wave 0 starts"
    - "Wave 2 gated on GATE-P4 PASSED unless an explicit WAIVER is recorded"
    - "If approved: ARCH-023 entered in the architecture decision log; the eight risks in plan section 6 raised in the risk register; 19-PORTING_GUIDE extended"
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
