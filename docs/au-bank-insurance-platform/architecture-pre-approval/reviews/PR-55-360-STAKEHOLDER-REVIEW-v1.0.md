# PR #55 — Architecture Pre-Approval Pack · 360° Stakeholder Review

**Review ID:** `REV-PR55-360-v1.0`
**Subject:** [PR #55 — Add architecture pre-approval document pack](https://github.com/Mahesh38/bank-insurance-platform/pull/55)
**Head commit reviewed:** `deadeb6` · branch `agent/architecture-pre-approval-pack` · 19 files (8 DOCX, 11 PNG)
**Review date:** 2026-08-17
**Workstream:** WS-3 — AU Bank Insurance Distribution Platform · canonical stage **S08**, gates **S06/S07** retroactively `CLOSED-WITH-CONDITIONS`
**Reviewed against:** AIGEM ([`09-AI_EXECUTION_RULES`](../../../governance/09-AI_EXECUTION_RULES.md), [`11-REVIEW_GATES`](../../../governance/11-REVIEW_GATES.md), [`17-DRIFT_CONTROL`](../../../governance/17-DRIFT_CONTROL.md)), [`ORG-STANDARDS`](../../../governance/ORG-STANDARDS.md), [`PERSONA-AUTHORITY-MATRIX v1.6`](../../../governance/PERSONA-AUTHORITY-MATRIX.md), [`CURRENT-STATE.yaml`](../../../governance/state/CURRENT-STATE.yaml), [`GATE-EVIDENCE.yaml`](../../../governance/state/GATE-EVIDENCE.yaml) and the existing architecture SSOT.

> **Authority boundary — read first.** This is an **AI-drafted review record**. It simulates the
> reasoning of the ten canonical personas so that human reviewers arrive at their meeting with the
> challenge already assembled. **It is not a verdict, not a signature, and not a board outcome.**
> Every `Recommend` below remains `Pending` until the named human records a decision. Security
> (Board 4) and Risk & Compliance (Board 6) sign-offs at T4 remain mandatory human acts under
> [`11-REVIEW_GATES §2`](../../../governance/11-REVIEW_GATES.md).

---

## 1. Headline

The pack is **well-constructed as a communication artefact and honest about its own approval
state** — the "Draft for review / not authorised for development" banner, the approval-control
notice on every document, and the refusal to fabricate a CTO meeting outcome are all correct and
should be preserved verbatim in v0.2.

It is **not yet mergeable as an architecture baseline**, for one structural reason and one factual
reason:

1. **Structural — it forks the architecture SSOT.** The repository already holds an approved-shape
   architecture set: [`platform/architecture-review/01–08`](../../../platform/architecture-review/README.md)
   (including ADRs **ARCH-001…ARCH-022**), [`platform/ws3-platform/00–05`](../../../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md)
   (domain model, information model, R0 solution architecture, security architecture, **measurable
   NFR catalogue**), and the retroactive [S06](../../../application-lifecycle-bible/evidence/S06-domain-architecture-evidence.md)/[S07](../../../application-lifecycle-bible/evidence/S07-solution-architecture-evidence.md)
   stage evidence. PR #55 introduces a **third** decomposition, a **third** risk register, a
   **third** set of decisions and a **third** approval flow, in a new folder, in a binary format,
   citing none of them. Under [`17-DRIFT_CONTROL`](../../../governance/17-DRIFT_CONTROL.md) that is
   drift, not documentation.
2. **Factual — it is stale against decisions already recorded.** Six items the pack lists as
   "unresolved" or "unconfirmed" have already been decided or built
   (DEC-20260816-03, -07, -12; `apps/rm-workspace-app`; the five services under `services/`; the
   consent and suitability rule packs). See [§5](#5-factual-corrections--the-pack-is-stale-against-recorded-decisions).

Both are fixable inside one revision cycle. Nothing in the pack's *content* is architecturally
wrong in a way that requires a redesign — the design positions it takes are the same positions the
existing SSOT takes. What is missing is **traceability, numbers, named signatories and the
regulatory layer**.

**Recommended board outcome:** `CHANGES REQUIRED` → re-submit as **v0.2** with the corrections in
[§5](#5-factual-corrections--the-pack-is-stale-against-recorded-decisions), [§6](#6-what-must-be-added) and [§9](#9-action-plan).

---

## 2. What was reviewed, and the commit history behind it

### 2.1 The pack itself

| # | Document | Pages of substance | Diagram(s) embedded |
|---|---|---|---|
| 01 | Solution Vision | 11 sections | solution vision overview |
| 02 | Business Capability Map | 6 sections, 20-row capability table | business capability map |
| 03 | Business Domain and Ownership Model | 6 sections, 18-row ownership table | domain ownership map |
| 04 | High-Level Design | 12 sections, 18-component table, 13-step flow | system context, component architecture, end-to-end flow |
| 05 | Security Design Review | 10 sections, 14-row threat table, SEC-01…SEC-08 | security trust zones, risk heatmap |
| 06 | Technical Risks, Dependencies, Assumptions | R-01…R-18, D-01…D-15, A-01…A-10 | risk heatmap, dependency map |
| 07 | CTO Architecture Review Record | CTO-01…CTO-07, empty discussion table | system context, approval flow |
| 08 | Architecture Progress Summary | Status and acknowledgement tables | progress dashboard |

Eleven PNG diagrams at 2400×1350. One commit, no Markdown source, no folder README, no index entry.

### 2.2 Commit history across **all** architectural-approval artefacts

This is the timeline a reviewer needs, because PR #55 is the fourth attempt at the same artefact
class and the first three are still open or already merged.

| Date | Commit / PR | Author | What it did to the approval surface |
|---|---|---|---|
| 2026-07-31 | `5e50b0d`, `419313a` | Cursor Agent | Captured Working Decisions v1 as MVP SSOT; PO document review — created `DECISION-LOG.md` |
| 2026-08-10 | `53441ac` | Claude | Restructured `docs/` into the segregated tree; moved `architecture-review/01–08` to its current home |
| 2026-08-12 | **PR #32 (open, draft)** | — | Adds `architecture-review/09-stakeholder-lld-approval-baseline.md`, `10-cto-approval-workflows.md` **and a CTO approval `.pptx`** — updates `docs/README.md`, `DECISION-LOG.md`, `SUGGESTION-REGISTER.md` |
| 2026-08-14 | `5a279fe`, `4cd060e` | Claude | CR-009 AIGEM recalibration ratified (GOV-007) |
| 2026-08-16 | `63ac9e3` | Claude | **Registered WS-3 as primary workstream**; wrote `GATE-EVIDENCE.yaml` WS-3 block (S08-G1…G6) |
| 2026-08-16 | `dee66f4` | Claude | **CR-010 board verdicts, WS-3 charter, `ws3-platform/00–05`, S00–S07 retroactive evidence** — the current architecture baseline |
| 2026-08-16 | `5d16662` | Claude | Consent rule pack v1 (GAP-006) |
| 2026-08-17 | `4bbf218` | Claude | **S11 RM app journey delivered** — `apps/rm-workspace-app`, 105 tests, three controls enforced structurally (DEC-20260816-12) |
| 2026-08-17 | **PR #54 (open)** | — | `COMP-APPROVE-CNS-SUIT-v1.1` — consent (42 rules) and suitability (62 rules) approval submission for GAP-006/007 |
| 2026-08-17 | **PR #55 `deadeb6`** | Mahesh Narkar | **This pack** — 19 binaries, new folder, no register/index updates |

**F-01 — the finding the history produces on its own, and the highest-value one in this review.**
Three open PRs (#32, #54, #55) now carry overlapping approval material for the same gate, in three
formats (`.md`, `.pptx`, `.docx`), in three locations, none referencing the others — on top of an
architecture baseline (`architecture-review/01–08`, `ws3-platform/00–05`, S06/S07 evidence) that is
already merged. **No document states which is canonical.** Whichever merges last silently becomes
the baseline by accident. This must be decided before any of the three merges (action **A-01**).

---

## 3. Stakeholder brainstorming — twelve lenses

Each lens is the reasoning that persona's package would produce, with the boundary the
[authority matrix](../../../governance/PERSONA-AUTHORITY-MATRIX.md) sets on it. Verdicts are
**simulated recommendations**, not decisions.

### 3.1 Product — Rajal (Board 3 / R1)

**Simulated verdict:** `CHANGES REQUIRED`

- ✅ The first-release boundary (one RM, one ETB customer, one Term Life product, one insurer,
  assisted) matches `CURRENT-STATE.yaml` `R0-ASSISTED-TERM-SALE` and **DEC-20260816-03**.
- ❌ **It never cites DEC-20260816-03.** Read against the published
  [`R0-SCOPE.md` v0.3 §2 A2](../../requirements/R0-SCOPE.md) — still the business SSOT on `main`,
  still saying *"RM + Self-service + Hybrid from Day 1"* — the pack reads as an **unauthorised
  scope reduction by an architect**. Product scope is Rajal's; architecture must quote the decision,
  not re-derive it. (**F-06**)
- ❌ **Group B insurers are absent entirely.** `R0-SCOPE` A5 puts Group B in scope as
  *catalogue + redirect*. The capability map, the domain model and the HLD have no redirect path,
  no Group B product state, and no answer for "who owns the journey after the customer leaves".
  (**F-07**)
- ❌ **Lead module contradiction.** The capability map defers "broader lead management"; `R0-SCOPE`
  §3 and A10 put the Lead module in the platform. [`03-solution-architecture-r0.md §3`](../../../platform/ws3-platform/03-solution-architecture-r0.md)
  defers Lead to S13 *with a stated rationale*. The pack defers it with none. (**F-08**)
- ❌ **No acceptance criteria, no KPI baselines.** §8 "Expected outcomes" lists directions
  ("higher completion", "fewer disputes") with no baseline, target, measure or owner. Product
  cannot sign an outcome it cannot later be held to. (**F-09**)
- ❌ **No traceability to BRD/PRD/BG IDs.** `R0-SCOPE` maps to BG-001…BG-006;
  [`BRD-P0-CAPABILITIES.md`](../../requirements/BRD-P0-CAPABILITIES.md) carries capability IDs. The
  capability map re-invents twenty capability names with no requirement IDs, so no one can prove
  coverage. (**F-10**)
- ⚠️ Offer ordering (**DEC-20260816-08 / QR-07** — disclosed customer-relevant basis only, no
  commission input) is a *mis-selling* control and belongs in the HLD's Quotation component. Absent.

### 3.2 Business Analysis — R11 (Product delegate)

**Simulated verdict:** `CHANGES REQUIRED`

- ❌ **R11 appears in exactly one of eight sign-off tables** (doc 02). The BA owns end-to-end
  process, rule, information, state, exception clarity and traceability preparation — which is
  precisely what docs 02, 03 and 04 assert. Add R11 to 02, 03, 04, 06. (**F-11**)
- ❌ **No state model.** Doc 03 §3 lists a fourteen-step happy path in prose. There is no journey
  state machine, no legal state transitions, no terminal states, no compensation/rollback
  semantics — while [`ws3-platform/01-domain-model-and-invariants.md`](../../../platform/ws3-platform/01-domain-model-and-invariants.md)
  already carries aggregates and invariants. Cite it or supersede it explicitly. (**F-12**)
- ❌ **Exception paths are named but not specified.** "Failed automatic recovery creates an owned
  operations task" — with no exception taxonomy, no SLA per exception class, no queue definition,
  no reopen/close rules. (**F-13**)
- ❌ **No information model / glossary.** Terms shift between documents: *journey* vs *lead* vs
  *opportunity*; *offer* vs *quote*; *sold* vs *complete*. [`02-information-model.md`](../../../platform/ws3-platform/02-information-model.md)
  and [`knowledge-base/09-glossary.md`](../../knowledge-base/09-glossary.md) exist. (**F-14**)

### 3.3 Architecture — Mahesh (Board 1 / R2) · *self-review, `self_review: true`*

**Simulated verdict:** `CHANGES REQUIRED` (an agent-authored plan self-reviewed by the same
persona needs at least one human board — [`11 §2`](../../../governance/11-REVIEW_GATES.md))

- ❌ **Third decomposition, no mapping.** Nineteen bounded contexts
  ([`business-problem-statement.md §6`](../../../context/business-problem-statement.md)) → twelve
  services + one app ([`03-solution-architecture-r0.md §3`](../../../platform/ws3-platform/03-solution-architecture-r0.md))
  → **eighteen "components"** (doc 04 §3) → **fourteen boxes** (component diagram). Four counts,
  no crosswalk. A reviewer cannot tell whether this is a rename, a refinement or a redesign.
  (**F-02**)
- ❌ **The existing estate is invisible.** `services/1sb-integration-service`,
  `services/bank-persistence-service`, `services/identity-authorization-service`,
  `services/identity-provider-adapter-service`, `services/workforce-access-bff` and
  `apps/rm-workspace-app` all exist in this repository today. The HLD names none of them, so it
  reads as greenfield. (**F-03**)
- 🔴 **Direct contradiction with an Accepted ADR.** Doc 04 §7: *"Direct access to another area's
  database is not allowed… each business area owns its official data."* The Decision Register
  records **Accepted**: *"Persistence is platform-common (`bank-persistence-service`), reached over
  HTTP"*. Both can be true (HTTP-mediated ownership inside one physical store), but the pack does
  not say so, and a Security or DBA reviewer reading only the pack would sign a different topology
  than the one implemented. **This is the single most important correction.** (**F-04**)
- ❌ **Twelve "key design decisions" with no ADR IDs.** ORG-STANDARDS **AP-7** — *no new runtime
  component without an ADR stating its operational cost*. Eighteen components proposed, zero ADRs,
  and ARCH-001…ARCH-022 already exist and already cover several of these decisions. (**F-05**)
- ❌ **No deployment view, no environment topology, no technology choices.** "Hosting and network
  design" (doc 04 §4) is nine prose lines; [`architecture-review/04-aws-infrastructure-architecture.md`](../../../platform/architecture-review/04-aws-infrastructure-architecture.md)
  already carries the AWS/EKS target. Region is described as "primary India region" — the
  repository says **ap-south-1**. Name it. (**F-15**)
- ❌ **No interface catalogue / contract-first artefacts.** **AP-5**: OpenAPI is the source of
  truth. Doc 04 §6 lists seven integrations with a "Method" column of prose
  ("Immediate request"). No contract, no schema, no version, no error model, no timeout numbers —
  while [`1sb-integration-service-architecture.md §7`](../../../1sb-insurance-integration/architecture/1sb-integration-service-architecture.md)
  already carries timeouts, retry, idempotency and concurrency numbers. (**F-16**)
- ❌ **No fitness functions** (S07-G8) and no mention of the existing **ArchUnit** boundary rules —
  **AP-6** says boundaries are enforced by tests, not convention. (**F-17**)
- ❌ **No sequence diagrams for the failure paths** the pack itself calls critical: uncertain
  payment, delayed insurer callback, duplicate submission, partial issuance. Prose is not a
  design for these. (**F-18**)
- ⚠️ Hexagonal/ports-and-adapters (**AP-2**) and replaceable middleware (**AP-3**) are the standing
  constraints on partner integration. "Translated at the boundary" is the same idea in different
  words — use the repository's words so the constraint is checkable.

### 3.4 Engineering — Amit (Principal Engineering)

**Simulated verdict:** `CHANGES REQUIRED`

- ❌ **No technology stack, no build/runtime standards.** The estate is Java 21 / Spring Boot /
  Gradle Kotlin DSL / Flutter. The pack is technology-free — defensible for a vision document,
  not for an HLD that engineering is asked to declare feasible. (**F-19**)
- ❌ **"Engineering Head" is not a named person** in any sign-off table. Amit is the canonical
  identity. Unnamed signatories make the tables unexecutable. (**F-20**)
- ❌ **R-13 ("delivery begins before automated quality and security checks work") is stale.** CI now
  runs Java 21 tests + coverage gates, SAST (CodeQL), SCA (Trivy), SBOM (CycloneDX) and gitleaks;
  ten SCA findings were cleared on 2026-08-17 (`86a986f`, `0546ab0`). Restate the risk as
  *residual* against the S08 gate criteria, not as *absent*. (**F-21**)
- ⚠️ **This PR is not green.** `SAST (CodeQL, Java)` is `failure` on `deadeb6` for a docs-only
  change — i.e. inherited from the base. It should be triaged before merge, or explicitly recorded
  as pre-existing. (**F-22**)
- ⚠️ Idempotency is asserted ("request key, stored result, duplicate-content check") without a key
  derivation rule, scope, TTL or storage owner — and **RISK-004 / TD-010** already record that
  in-memory idempotency is unsafe multi-instance.

### 3.5 Security — Deepali (Board 4)

**Simulated verdict:** `CHANGES REQUIRED` — and **Security cannot approve on this material** even
if it agreed with every line, because S07-G3 asks for a threat model *per trust boundary*.

- ✅ The self-limiting language in doc 05 §1 and §8 ("permission to treat design as approved: No")
  is exactly right and must survive into v0.2.
- ❌ **The threat table is not a threat model.** Fourteen flat threat rows with no data-flow
  diagram, no boundary decomposition, no STRIDE (or equivalent) coverage argument, no residual-risk
  rating, no accepted-by. **S07-G3 requires E2, signed by a human.** (**F-23**)
- ❌ **No security numbers anywhere.** Absent: session lifetime and idle timeout, MFA posture,
  device-binding mechanism, token lifetimes, rate-limit values, mTLS between services, key rotation
  intervals, certificate lifecycle, log retention, egress allow-listing, WAF/DDoS posture, PAM and
  break-glass. Controls without parameters cannot be tested. (**F-24**)
- 🔴 **Payment callback authentication is unspecified.** "Authenticated callback" is the entire
  control against forged payment results (threat row, and **R-06/SEC-05**). Signature scheme?
  mTLS? IP allow-list? Replay window? Clock skew? This is a money-movement control (**G5** T4
  trigger) and it is one adjective. (**F-25**)
- 🔴 **The payment link is a customer-facing secret and is not specified.** One-time use? Expiry?
  Bound to the OTP-verified device? What happens on forward? The whole "payment only on the
  customer's device" control rests on it. (**F-26**)
- ❌ **No fraud/abuse model** beyond "customer search abuse": RM impersonation, velocity limits,
  collusion, mis-selling detection, dormant-session hijack. (**F-27**)
- ❌ **No secure-SDLC section** even though the pipeline now has SAST/SCA/SBOM/secret scanning —
  and no pen-test / VAPT plan before go-live. (**F-28**)
- ❌ **[`ws3-platform/04-security-architecture.md`](../../../platform/ws3-platform/04-security-architecture.md)
  and the [workforce authN/authZ SSOT](../../../platform/authentication-authorization/README.md)
  are never cited**, so doc 05 cannot be read as an increment on them. (**F-29**)
- ⚠️ Trust-zone diagram gaps: no customer-device zone (the control the pack leans on hardest), no
  DMZ/egress separation, no India-region boundary, no data-store zone — plus two label collisions
  (see [§7](#7-diagram-by-diagram-review)).

### 3.6 Data & Database — Aarti

**Simulated verdict:** `CHANGES REQUIRED`

- ❌ **No logical data model, no physical model, no data classification table.** S06-G4 (logical
  data model per context) and S07-G5 (data architecture approved *including backup and retention*)
  cannot be met by an ownership table. [`architecture-review/05-data-architecture.md`](../../../platform/architecture-review/05-data-architecture.md),
  [`02-information-model.md`](../../../platform/ws3-platform/02-information-model.md) and
  `V1__init_schema.sql` exist. (**F-30**)
- 🔴 **Retention is recorded as "not approved" while the repository already carries numbers.**
  `CURRENT-STATE.yaml` names *7-year write-once retention in ap-south-1* as an S09 deliverable;
  **ASM-005** records 7-year retention for auth/admin events as an open assumption; PR #54's rule
  packs define retention anchored to the latest journey that relied on the record, with legal
  hold. The pack's SEC-07/D-13 must cite these, not restate a blank. (**F-31**)
- 🔴 **RPO conflict inside one document.** Doc 04 §8: *"restore core sale services within one hour
  with no more than five minutes of transaction loss"* **and** *"audit evidence is designed for no
  loss during a regional recovery"*. RPO 5 min and RPO 0 are different architectures with different
  costs (async vs synchronous replication). State them as two NFRs with IDs and justify the
  second. (**F-32**)
- ❌ **The NFR numbers are un-IDed and un-derived.** [`05-nfr-catalogue.md`](../../../platform/ws3-platform/05-nfr-catalogue.md)
  exists precisely to fix this — it assigns IDs, measurement methods, verification stages and
  derivations, and records CAP-A1…CAP-A3 capacity assumptions. Reusing its IDs costs nothing and
  makes every number testable. (**F-33**)
- ❌ No archival/purge design, no lineage for the reporting copies, no masking specification for
  lower environments beyond the sentence "generated or masked", no PII inventory. (**F-34**)

### 3.7 QA — Swapnali (Board 5)

**Simulated verdict:** `CHANGES REQUIRED`

- ❌ **No test strategy in the pack at all** — no levels, no risk-based scope, no entry/exit, no
  environments, no partner-sandbox plan, no UAT approach. `TESTING-RULES.md`,
  `QA-LEAD-TESTING-STRATEGY.md`, `COVERAGE.md` and `TEST-BACKLOG.md` exist and are unreferenced.
  (**F-35**)
- ❌ **Controls are not traced to tests.** Doc 05 §6 ends each threat with "Remaining action:
  negative access tests / duplicate-request tests / rotation exercise" — none has a test ID, an
  owner, an environment or an evidence level. A control with no test is an assertion. (**F-36**)
- ❌ **No evidence levels (E1–E4)** anywhere, though the whole gate model is built on them. "Status:
  Designed" is not an evidence level. (**F-37**)
- ❌ **No NFR verification method** — QA cannot prove `RTO ≤ 1 h` from prose. (Covered by F-33.)
- ⚠️ Doc 08's "Architecture drafting completion 100%" invites exactly the misreading its own
  footnote warns against. Prefer "7 of 7 document types drafted; 0 of 10 reviews complete" with no
  percentage. (**F-38**)

### 3.8 Compliance & Risk — Shailja (Board 6)

**Simulated verdict:** `CHANGES REQUIRED` — **this is the largest content gap in the pack.**

- 🔴 **Not one regulation is named.** Eight documents about a regulated bancassurance platform, and
  the words IRDAI, RBI, DPDP, PMLA, KYC and "corporate agency" do not appear. "Data remains in
  India" has no cited basis; "required retention period" has no regime; "regulated actions" has no
  definition. Compliance cannot rule on permissibility against an unnamed obligation.
  The minimum set to map: **IRDAI corporate-agency and Protection of Policyholders' Interests
  regulations, RBI outsourcing / IT-governance directions, DPDP Act 2023, PMLA/KYC, payment-data
  localisation.** [`shailja-s-compliance-risk-head/02-regulatory-registry.md`](../../../context/roles/shailja-s-compliance-risk-head/02-regulatory-registry.md)
  and `04-risk-taxonomy.md` are the existing anchors. (**F-39**)
- 🔴 **Consent and suitability are described as unresolved when v1 packs exist and v1.1 is in
  review.** DEC-20260816-07 adopted `CONSENT-PACK-v1.0` (38 rules) and
  `SUITABILITY-PACK-v1.0` / `SUIT-ALGO-LIFE-v1.0` (48 rules) as Product-side behaviour,
  content-complete, ratification pending; **PR #54** carries `COMP-APPROVE-CNS-SUIT-v1.1`
  (42 consent rules, 62 suitability rules). SEC-02, R-02, D-02 and D-03 must cite them and state
  what is *actually* outstanding: **Shailja's human signature at E2**, not the rules themselves.
  (**F-40**)
- ❌ **GAP-006/GAP-007 and DEC-20260816-06 are unmentioned** — the non-waivable S11 entry condition
  that binds this entire programme. An architecture pre-approval pack that omits the one binding
  build-freeze condition is incomplete. (**F-41**)
- ❌ **Missing regulated-journey obligations:** free-look and cancellation, nominee handling,
  grievance/complaint and ombudsman routing, policyholder communication obligations, AML/CFT
  screening, sales-attribution and distributor-licence checks (asserted as a control but not tied
  to a certification regime), record reconstruction SLA for a regulator request. (**F-42**)
- ❌ **Consent withdrawal is named in the capability gap table and nowhere else** — no journey
  behaviour, no downstream effect on an in-flight proposal, no evidence rule. Under DPDP that is a
  first-order requirement. (**F-43**)
- ❌ **No maker-checker mapping** (CMP-4) beyond "privileged changes require a second approver".
  (**F-44**)
- ⚠️ Positive: "compliance debt is never accepted" (CMP-6) is honoured in spirit — nothing in the
  pack proposes to defer a compliance control. Keep it that way in v0.2.

### 3.9 SRE / Operations — Shivanshi (Board 7 / R10)

**Simulated verdict:** `CHANGES REQUIRED`

- ❌ **No SLIs, no SLOs, no error budget.** "Requests have clear time limits" is not an SLO. The
  SRE canon states journey SLOs in **p95**; the architecture review states p50/p99; the pack states
  neither. (**F-45**)
- ❌ **No capacity model.** The pack asserts A-07 ("initial volume is modest") with no RM count, no
  journey rate, no transaction amplification, no downstream insurer/1SB/PG limit, no DB ceiling —
  the exact reasoning Shivanshi's package forbids skipping. `05-nfr-catalogue.md §2` already
  records CAP-A1 (250 RMs), CAP-A2 (20% concurrency), CAP-A3 (2 journey starts/RM/hr) as explicit
  assumptions. Reuse them. (**F-46**)
- ❌ **No observability design** — no telemetry model, no correlation-ID propagation across the
  partner boundary, no alerting, no dashboards, no runbooks, no on-call, no incident severity
  matrix. `libs/bank-common-observability` exists. (**F-47**)
- ❌ **No environment matrix and no release/rollback strategy** — how many environments, what data,
  what promotion path, what rollback for a partner-contract change. (**F-48**)
- ❌ **DR is a target with no exercise plan.** "Backups tested through restore exercises" needs a
  cadence, an owner, an evidence artefact and a first-run date. The [ORR template](../../../application-lifecycle-bible/templates/ORR.md)
  exists and should be referenced as the operational-readiness exit. (**F-49**)
- ⚠️ D-15 ("operations ownership and support process") is the right dependency, with no required-by
  date and no named owner — see F-52.

### 3.10 Delivery — Kalpana (R12)

**Simulated verdict:** `CHANGES REQUIRED`

- 🔴 **Fifteen dependencies, zero dates.** D-01…D-15 have providers but no *required-by* date, no
  ageing, no critical-path position and no escalation trigger. Kalpana's protocol requires delivery
  impact, authority owner and required-by date to be explicit. A dependency register without dates
  cannot drive a plan. (**F-50**)
- 🔴 **The "Owner" columns name functions, not people.** "Product, Security and Architecture",
  "Finance, Operations and Engineering" — three-way ownership is no ownership. Each risk,
  dependency, assumption and decision needs **one** accountable name. (**F-51**)
- ❌ **No plan, no sequence, no milestones, no critical path, no estimate, no funding line.**
  `FRI-001` (DEC-20260816-09) is a two-tranche funding line that is **BLOCKED — no named
  approver exists (GAP-010)**; CTO-06 asks for funding confirmation from a CTO who is not in the
  authority matrix. These two facts must meet on one page. (**F-52**)
- ❌ **No decision-by dates.** Every document says "Next action: stakeholder review and recorded
  decision". None says *by when*, or what happens if the date passes. (**F-53**)
- ❌ **No RACI** for the pack itself: who convenes the review, who chairs, who records, who
  aggregates the verdicts, who publishes v0.2. (**F-54**)

### 3.11 Finance — *no persona package exists*

**Simulated verdict:** `SEAT MISSING`

Finance is assigned ownership in the pack itself — capability ownership *"Payment and
reconciliation: Product and Finance"*, R-06 owner, D-08 provider, "Policy sold status: Finance
approval pending" — **and appears in none of the eight sign-off tables.** Either Finance signs
(add the row) or the ownership claims are wrong. Settlement timing, break-handling, chargeback and
refund accounting are unaddressed. (**F-55**)

### 3.12 Executive Sponsor / CTO — *authority undefined*

**Simulated verdict:** `AUTHORITY UNDEFINED — resolve before the meeting`

- 🔴 **"Chief Technology Officer" does not exist in [PERSONA-AUTHORITY-MATRIX v1.6](../../../governance/PERSONA-AUTHORITY-MATRIX.md).**
  Document 07 asks a CTO for seven endorsements (CTO-01…CTO-07) including funding, and the approval
  flow diagram places CTO review as step 3 of 5, ahead of the mandatory approvers. AIGEM has
  **seven boards and one aggregator**; strategic technology decisions sit with Architecture
  (`A/AP`), and funding sits with an **Executive Sponsor who does not exist (GAP-010)**. Either
  raise a CR to define the CTO's decision rights, or re-label document 07 as an *Architecture Board
  submission with an executive-sponsor funding annex*. Until then, doc 07 asks an undefined
  authority to endorse a design. (**F-56**)
- ⚠️ The pack correctly refuses to record a meeting that did not happen (doc 07 §1). Preserve that.

---

## 4. Governance conformance (AIGEM)

| Requirement | Source | State in PR #55 |
|---|---|---|
| Input triaged before artefact creation | [`09-AI_EXECUTION_RULES`](../../../governance/09-AI_EXECUTION_RULES.md) · README §5 | ❌ No triage record, no `SUG-`/work-item ID |
| Commits reference the work item ID | ORG-STANDARDS **D-2** | ❌ `docs: add architecture pre-approval pack` |
| Risk tier declared, boards mapped | [`11 §3`](../../../governance/11-REVIEW_GATES.md) | ❌ No tier. Content touches **G1, G2, G5, G6, G8** → **T4** |
| Board verdicts recorded to schema | [`approval-gate.schema.json`](../../../governance/schemas/approval-gate.schema.json) | ❌ Free-text sign-off tables instead of the seven board verdicts |
| Security + Risk&Compliance = human at T4 | [`11 §2`](../../../governance/11-REVIEW_GATES.md) | ✅ Stated in prose · ❌ not encoded |
| Gate criteria cited (S06-G1…G8, S07-G1…G8) | stage files + `GATE-EVIDENCE.yaml` | ❌ None cited |
| Evidence levels E1–E4 | [`04-STAGE_GATES`](../../../governance/04-STAGE_GATES.md) | ❌ Absent |
| Registers updated (risk/dep/assumption/decision) | `registers/` | ❌ Parallel `R-`/`D-`/`A-` IDs created instead |
| Drift control — supersedes/relates-to declared | [`17-DRIFT_CONTROL`](../../../governance/17-DRIFT_CONTROL.md) | ❌ Existing architecture set not cited |
| Doc map / index updated | `docs/README.md` | ❌ New folder is unindexed and has no README |
| Freshness check before admission | `FreshnessCheck.java` | ✅ Claimed in the PR body |
| Approval-state honesty | — | ✅ **Exemplary** |

---

## 5. Factual corrections — the pack is stale against recorded decisions

| # | Pack says | Repository says | Correction |
|---|---|---|---|
| C-1 | A-01 *"first release is one assisted Term Life sale"* — **Unconfirmed** | **DEC-20260816-03 — DECIDED by Product** (assisted-first; DIY at R1, hybrid at R2) | Cite the decision; note the outstanding `R0-SCOPE` v0.4 republish (condition C4) |
| C-2 | R-02 / SEC-02 / D-02 / D-03 *"consent and suitability rules unresolved"* | DEC-20260816-07: both packs adopted, content-complete; **PR #54** carries v1.1 (42 + 62 rules) | Restate as *"rules drafted; Shailja's E2 human signature outstanding"* |
| C-3 | *"payment on customer device"* — **Pending** | DEC-20260816-12: enforced structurally in `apps/rm-workspace-app` — no interface method can take an instrument; 105 tests pass | Restate as *designed and enforced in the interface half; backend half outstanding* |
| C-4 | Component list implies greenfield | Five services + one Flutter app exist and are built by CI | Add an "existing estate" column mapping each component to what exists |
| C-5 | R-13 *"delivery begins before automated checks work"* | CI runs tests+coverage, CodeQL, Trivy SCA, CycloneDX SBOM, gitleaks; 10 SCA findings cleared 2026-08-17 | Restate as residual against S08-G1…G6 |
| C-6 | SEC-07/D-13 *"retention periods not approved"* | 7-year write-once in **ap-south-1** named in `CURRENT-STATE.yaml`; ASM-005 open; PR #54 defines the anchoring rule | Cite all three; state precisely which signature is missing |
| C-7 | Doc 08: *"Requested document types prepared 7 of 7"*, *"Documents marked as pre-approval 7 of 7"* | The pack contains **8** documents; the dashboard PNG says **8 of 8** | Reconcile: 7 requested *types*, 8 *documents* — say both, or the reader assumes one is wrong |
| C-8 | Doc 04 §8 RTO 1 h / RPO 5 min | Same numbers exist in `architecture-review/06` and are ID'd in `05-nfr-catalogue.md` | Reuse the NFR IDs; add p95, measurement method, verification stage |
| C-9 | *"primary India region"* / *"secondary India region"* | `ap-south-1` (and the DR pair) named in governance state | Name the regions |
| C-10 | DOCX core properties: created **2013-12-23T23:15:00Z** | Authored 2026-08-17 | Template metadata leaked into all 8 files — set real dates |
| C-11 | Commit author `mh,narkar@gmail.com` | Comma instead of a dot | Malformed address — breaks attribution; fix with `git commit --amend --author` |

---

## 6. What must be added

### 6.1 New documents for pack v0.2

| # | Document | Why it is mandatory | Gate criterion |
|---|---|---|---|
| 09 | **Traceability matrix** — BG/BRD/PRD/R0 requirement → capability → domain → component → control → test | Nobody can currently prove coverage or impact | S06-G5, S07-G7 |
| 10 | **NFR sheet** (adopt `05-nfr-catalogue.md` IDs; add p95, measurement method, verification stage, owner) | Numbers without measurement methods are not NFRs | **S07-G6** |
| 11 | **Threat model per trust boundary** with data-flow diagrams and residual-risk ratings | Flat threat tables cannot satisfy an E2 human signature | **S07-G3** |
| 12 | **Data architecture** — logical model per context, classification, retention schedule, archival, masking, lineage | Ownership tables are not a data architecture | **S07-G5** |
| 13 | **Regulatory obligation register** — IRDAI / RBI / DPDP / PMLA / localisation → control → evidence → owner | Zero regulations are currently named | Board 6 |
| 14 | **Test & evidence strategy** — levels, risk-based scope, control-to-test map, partner sandbox, UAT, evidence levels | Controls with no tests are assertions | Board 5 |
| 15 | **Operational readiness** — SLIs/SLOs (p95), capacity model, observability, runbooks, incident and DR exercise plan | "Recovery targets proposed" is not readiness | Board 7 / [ORR](../../../application-lifecycle-bible/templates/ORR.md) |
| 16 | **Delivery plan** — sequence, milestones, critical path, dependency dates, estimate, funding tranches (`FRI-001`) | A pack with no dates cannot be planned against | R12 |
| 17 | **ADR set** for every new runtime component and each of the twelve key decisions (use [`templates/ADR.md`](../../../governance/templates/ADR.md); extend ARCH-0xx) | **AP-7** | **S07-G2** |
| 18 | **Cost / TCO and licensing view** — infrastructure, DR, partner, tooling | CTO-06 asks for funding with no number attached | Sponsor |

### 6.2 Sections to add inside existing documents

- **Every document:** an *AIGEM header block* — workstream, stage, gate criteria addressed, risk
  tier, evidence level, board verdicts required, human-signature rule, and a **supersedes /
  relates-to** table naming `architecture-review/01–08`, `ws3-platform/00–05` and the S06/S07
  evidence files.
- **Doc 01:** decision citations (DEC-20260816-03, -06, -07, -08); outcome measures with baseline,
  target, measurement owner and date.
- **Doc 02:** requirement IDs per capability; Group B redirect capability; Lead-module position
  with its decision reference; consent-withdrawal capability.
- **Doc 03:** crosswalk to the 19 bounded contexts; journey state machine with terminal states and
  compensations; exception taxonomy with SLAs.
- **Doc 04:** existing-estate mapping; deployment and environment view; interface catalogue
  (contract-first); failure-path sequence diagrams; idempotency-key specification; fitness
  functions; the `bank-persistence-service` reconciliation (**F-04**).
- **Doc 05:** security parameters (**F-24**); callback authentication scheme; payment-link
  security; fraud/abuse model; secure-SDLC and pen-test plan; citations to
  `ws3-platform/04-security-architecture.md` and the authN/authZ SSOT.
- **Doc 06:** required-by dates, single named owners, exposure scoring on the repository's
  `L × I` scale, and **merge into the canonical registers** rather than a parallel numbering.
- **Doc 07:** resolve the CTO authority question (**F-56**); attach the funding annex to
  `FRI-001`/GAP-010; replace the five-step flow with the seven-board model.
- **Doc 08:** drop the completion percentage; add the gate-criteria coverage view.

### 6.3 Stakeholder seats to add to the sign-off tables

Current tables are inconsistent (6, 5, 7, 9, 8, 10, 8 and 10 rows) and name **functions, not
people**. Standardise one table across all documents, with named individuals:

| Seat | Canonical person | Present today |
|---|---|---|
| Product (Board 3) | Rajal | ✅ all docs |
| Business Analysis (R11) | Principal BA | ⚠️ doc 02 only → **add to 03, 04, 06** |
| Architecture (Board 1) | Mahesh | ✅ |
| Engineering | Amit | ⚠️ named as "Engineering Head" → **name the person** |
| Security (Board 4, **human**) | Deepali | ⚠️ same |
| Data / DBA | Aarti | ⚠️ **absent from docs 01, 02** |
| QA (Board 5) | Swapnali | ⚠️ **absent from docs 01, 02** |
| Compliance & Risk (Board 6, **human**) | Shailja | ⚠️ **absent from doc 02** |
| SRE / Operations (Board 7) | Shivanshi | ⚠️ **absent from docs 01, 02, 03** |
| Delivery (R12) | Kalpana | ⚠️ **absent from docs 03, 05** |
| **Finance** | *unassigned* | ❌ **absent everywhere** — owns reconciliation in the pack's own tables |
| **Executive Sponsor** | *GAP-010 — vacant* | ❌ absent; funding decisions have no approver |
| **Legal / DPO** | *unassigned* | ❌ absent; DPDP obligations have no owner |
| **Internal Audit** | *unassigned* | ⚠️ optional observer for the evidence model |

---

## 7. Diagram-by-diagram review

| Diagram | Verdict | Findings |
|---|---|---|
| `solution_vision_overview` | Fit for purpose | — |
| `business_capability_map` | Fit for purpose | Add requirement IDs and first-release shading |
| `domain_ownership_map` | Fit for purpose | Add the bounded-context number per domain |
| `system_context` | Fit for purpose | Add the customer device as a distinct actor boundary |
| `component_architecture` | **Correct before use** | Shows **14 grouped boxes** vs the HLD's **18 components** — merges consent+suitability, quote+proposal, payment+policy, which the domain model deliberately separates. Arrows are generic top-down, not real dependencies. No data stores. (**F-57**) |
| `end_to_end_flow` | Adequate | Happy path only — add the four failure paths |
| `security_trust_zones` | **Correct before use** | Zone labels *"Private service zone"* and *"Protected data zone"* are **overlapped by the boxes drawn on top of them** — text is partly illegible. No customer-device zone, no egress/DMZ, no region boundary. (**F-58**) |
| `architecture_risk_heatmap` | Adequate | Rescale to the repository's `L × I` 1–3 model and exposure bands |
| `architecture_dependency_map` | **Correct before use** | Add required-by dates; without them it is a picture, not a dependency map |
| `stakeholder_approval_flow` | **Correct before use** | Five-step linear flow contradicts the seven-board + aggregator model; places CTO review ahead of mandatory approvers; omits the block authority of Security and Compliance and the two-round rework cap. (**F-59**) |
| `architecture_progress_dashboard` | **Correct before use** | "8 of 8" contradicts doc 08's "7 of 7"; "100%" invites the misreading the document warns against. (**F-60**) |

---

## 8. Format, packaging and hygiene

| # | Finding | Correction |
|---|---|---|
| F-61 | **Binary-only pack.** Eight DOCX + eleven PNG, no Markdown source. Review comments cannot be anchored to a line; `git diff` shows `Bin 0 -> N bytes`; a v0.2 re-render changes every byte with no visible delta. The entire governance model depends on citable, diffable paths | Keep Markdown as the source of truth per document; generate DOCX/PNG as build outputs from a committed script. The presentation goal is fully preserved |
| F-62 | **Version embedded in every filename** (`_v0.1`) inside a folder already named `v0.1/` | Drop the suffix from filenames; the folder carries the version. Otherwise every revision creates 19 new paths and breaks every link |
| F-63 | **No folder README / index**, and `docs/README.md` is not updated | Add `architecture-pre-approval/README.md` (purpose, contents, status, canonical relationship to `architecture-review/` and `ws3-platform/`) and index it in the docs map |
| F-64 | **No entry in `DECISION-LOG.md` or any register** | Record the pack's creation and its relationship to the existing baseline |
| F-65 | **Diagram sources not committed** (PNG only) | Commit the generating script/source so diagrams are maintainable |
| F-66 | **Three competing approval artefacts open at once** (PR #32 `.pptx` + `.md`, PR #54 `.docx`, PR #55 `.docx`) | Declare which is canonical **before** any of them merges — see **F-01** |
| F-67 | Commit message carries no work-item ID (**D-2**) | Re-message with the governed ID |
| F-68 | `SAST (CodeQL, Java)` failing on a docs-only PR | Triage or record as pre-existing base failure |

---

## 9. Action plan

Ordered by what unblocks the most. Owners are the canonical personas; **dates are proposals** for
the review meeting to set.

### Blockers — must clear before the pack can be reviewed as a baseline

| ID | Action | Owner | Findings |
|---|---|---|---|
| A-01 | **Declare the canonical approval artefact** across PRs #32/#54/#55 and state the relationship of this pack to `architecture-review/01–08` and `ws3-platform/00–05` (supersedes / summarises / presents) | Mahesh + Kalpana | F-01, F-66 |
| A-02 | **Reconcile the component model** — one crosswalk table: 19 contexts → 12 services + 1 app → pack components → existing `services/` and `apps/` | Mahesh | F-02, F-03, F-57 |
| A-03 | **Resolve the `bank-persistence-service` contradiction** — state explicitly how "one owner per data store" coexists with the accepted platform-common persistence ADR | Mahesh + Aarti | F-04 |
| A-04 | **Apply the eleven factual corrections C-1…C-11** | Mahesh | §5 |
| A-05 | **Add the regulatory obligation register** — no compliance review can start without it | Shailja | F-39, F-42, F-43 |
| A-06 | **Resolve the CTO authority question** (CR, or re-label doc 07) and attach funding to `FRI-001`/GAP-010 | Mahesh + Kalpana | F-56, F-52 |

### Majors — required for the gate, not for the meeting

| ID | Action | Owner | Findings |
|---|---|---|---|
| A-07 | Adopt the NFR catalogue IDs; add p95, measurement method, verification stage; resolve the RPO conflict | Mahesh + Shivanshi + Aarti | F-32, F-33, F-45 |
| A-08 | Produce the per-boundary threat model with data-flow diagrams and security parameters | Deepali | F-23, F-24, F-25, F-26, F-27, F-28 |
| A-09 | Produce the data architecture: logical model, classification, retention schedule, masking, lineage | Aarti | F-30, F-31, F-34 |
| A-10 | Merge `R-`/`D-`/`A-` items into the canonical registers with single named owners and required-by dates | Kalpana | F-50, F-51, F-53, F-13 |
| A-11 | Write ADRs for the twelve key decisions and each new runtime component | Mahesh | F-05, F-17 |
| A-12 | Add the traceability matrix to BG/BRD/PRD/R0 IDs | R11 BA + Rajal | F-10, F-12, F-14 |
| A-13 | Add the test & evidence strategy with a control-to-test map and evidence levels | Swapnali | F-35, F-36, F-37 |
| A-14 | Add SLOs, capacity model, observability, runbooks, DR exercise plan, environment matrix | Shivanshi | F-46, F-47, F-48, F-49 |
| A-15 | Add Group B redirect, Lead-module position, consent withdrawal, outcome measures with baselines | Rajal | F-07, F-08, F-09, F-43 |
| A-16 | Standardise the sign-off table across all eight documents with **named** signatories, adding Finance, R11, Sponsor and Legal/DPO | Mahesh + Kalpana | F-11, F-20, F-55, §6.3 |

### Minors — hygiene, do them with the v0.2 render

`A-17` Markdown source + generation script (F-61) · `A-18` drop filename version suffixes (F-62) ·
`A-19` folder README + docs-map entry (F-63, F-64) · `A-20` fix DOCX core-property dates (C-10) ·
`A-21` fix the malformed commit author email (C-11) · `A-22` fix the two diagram label collisions
and the 7-vs-8 count (F-58, F-60) · `A-23` redraw the approval flow as the seven-board model
(F-59) · `A-24` commit diagram sources (F-65) · `A-25` work-item ID in the commit message (F-67) ·
`A-26` triage the CodeQL failure (F-68).

---

## 10. What the pack already gets right — keep these in v0.2

1. **Approval honesty.** The banner, the per-document approval-control notice, the refusal to
   record a CTO meeting that has not happened, and doc 05's *"Permission to treat design as
   approved: No"*. This is exactly the behaviour AIGEM asks of an AI-drafted artefact.
2. **Separation of duties is stated, not blurred** — doc 07 §6 keeps Product, Security, Compliance,
   Data, QA and Operations decisions independent of the CTO review.
3. **The three non-negotiable controls are consistent throughout** — suitability before quotation,
   consent before proposal, payment only on the customer's device — and they match
   `R0-SCOPE` A7/A8/A9, DEC-20260816-12 and the rule packs.
4. **"Sold" is correctly defined** as issuance + reconciliation + audit completeness, matching
   `R0-SCOPE` A6.
5. **Failure-first thinking** — uncertain payment states, no blind resubmission, duplicate
   handling, reconciliation over restore. This is the right instinct and only needs numbers and
   sequence diagrams.
6. **Scope discipline** — the first-release boundary is genuinely narrow and matches the Product
   decision. It only needs the citation.
7. **Accessibility and rendering quality** of the DOCX output.

---

## 11. Review provenance

| Field | Value |
|---|---|
| Method | Full text extraction of all 8 DOCX (paragraphs + tables, in document order), visual inspection of the diagrams, cross-reference against `docs/governance/`, `docs/platform/`, `docs/application-lifecycle-bible/`, `docs/au-bank-insurance-platform/`, `services/`, `apps/`, and the commit history of every architectural-approval path |
| Personas simulated | Rajal (Product), R11 (BA), Mahesh (Architecture), Amit (Engineering), Deepali (Security), Aarti (Data/DBA), Swapnali (QA), Shailja (Compliance/Risk), Shivanshi (SRE/Ops), Kalpana (Delivery) + two vacant seats (Finance, Executive Sponsor) |
| Findings | 68 (`F-01…F-68`) + 11 factual corrections (`C-1…C-11`) |
| Signature status | **AI-DRAFTED — no human signature. Not a board verdict. Not a gate outcome.** |
| Binding rule | Security (Board 4) and Risk & Compliance (Board 6) verdicts at T4 require a human reviewer; no aggregate override exists |
