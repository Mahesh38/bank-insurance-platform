# 01 — Position Assessment: Where This Platform Actually Is

**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-15
**Status:** PO assessment. Product-scope conclusions are mine to state; Architecture, Security,
Compliance, QA and SRE conclusions below are *findings for those boards to ratify*, not verdicts
I am entitled to issue ([PERSONA-AUTHORITY-MATRIX §5](../governance/PERSONA-AUTHORITY-MATRIX.md)).
**Method:** repository evidence only — file counts, CI configuration, gate state, registers. No
claim in this document rests on a document asserting itself.

> **Snapshot note (2026-08-16):** This assessment records the pre-CR-010 baseline. CR-010 adds an
> application-CI workflow and structured gate evidence. That removes the missing-CI mechanism,
> but it does not supply a green run, sandbox E2E, UAT, performance or mandatory human evidence.

---

## 1. The finding in one paragraph

The repository is executing **hardening of an integration adapter** while the **platform that
adapter exists to serve has not been built, and the engineering and environment foundations
underneath it were never laid**. Governance says WS-1 is at *L7 Hardening* — the fifth-from-last
rung — and that is accurate for what WS-1 covers. But WS-1 covers a supplier interface, not the
product. Measured against the business the platform was funded to run, we are hardening the
seventh floor of a building whose foundation, ground floor, and load-bearing structure are
absent. The metaphor in the brief is not an exaggeration; the evidence below is more specific
than the metaphor.

---

## 2. What the business asked for

From [`business-problem-statement.md`](../context/business-problem-statement.md) and the
[requirements baseline](../au-bank-insurance-platform/requirements/README.md), AU SFB is building
a bank-owned multi-insurer distribution platform under IRDAI Composite Corporate Agent licence
CA0515. The value proposition is precisely the thing the legacy AU Beema Portal loses: **the bank
keeps visibility and control across the whole sale**, from lead to issued policy.

The declared end-to-end journey:

```
Lead → Need Analysis → Suitability → Consent → Multi-Quote Compare
     → Proposal Capture → Bank Payment Gateway → Policy Issuance → Reconciliation
```

Nineteen bounded contexts were named to deliver it. A Flutter application was named as the sole
customer- and RM-facing surface. AWS EKS, Aurora, DynamoDB, ap-south-1 with ap-south-2 DR, and
7-year S3 Object Lock retention were named as the runtime.

**A policy is only "sold"** when it is issued by the insurer, confirmed by API, reconciled
against the payment gateway, and persisted in bank audit stores — all four.

That is the product. Hold it next to what exists.

---

## 3. What actually exists

### 3.1 Code inventory (measured, 2026-08-15)

| Module | Main `.java` | Test `.java` | Business meaning |
|---|---:|---:|---|
| `services/1sb-integration-service` | 147 | 42 | Bounded context #15 — the aggregator adapter |
| `services/bank-persistence-service` | 45 | 7 | Durable state behind an HTTP contract |
| `services/identity-authorization-service` | 20 | 4 | Part of bounded context #3 |
| `services/workforce-access-bff` | 19 | 4 | Part of bounded context #3 |
| `services/identity-provider-adapter-service` | 8 | 2 | Part of bounded context #3 |
| `libs/bank-common-*` (5 libraries) | 38 | — | Shared error, security, audit, observability, secrets |
| **Total** | | | **~20,200 lines of Java** |

### 3.2 Bounded-context coverage

Of the 19 named contexts, **2.5 exist**: the 1SB Adapter (#15), Identity & Access (#3, partial —
workforce only, no customer identity), and a persistence service that is an enabler rather than a
named context.

**Absent entirely — sixteen and a half contexts:**

Customer BFF · RM Workspace BFF · Customer Service · Lead Service · Consent Service ·
Suitability & Recommendation · Product Catalogue · Journey Orchestration · Quotation Service ·
Proposal & UW-Tracking · Payment Service · Policy & Issuance · Integration Hub · Audit &
Compliance · Notification · Reporting & MIS · Administration & Config

### 3.3 The user interface

`find . -name pubspec.yaml` returns nothing. **There is no Flutter application.** No RM can use
this platform, and no customer can see it. Every journey in the requirement set terminates at an
interface that does not exist.

---

## 4. The five structural gaps

These are ordered by how much they invalidate work already done.

### GAP-A — There was no CI pipeline for the application code at assessment time · **CRITICAL**

`.github/workflows/` contains exactly one file: `governance.yml`. It validates governance
markdown, schemas, and document freshness. It runs `FreshnessCheck.java`.

**It does not build the application. It does not run a single test. It does not check coverage.**

Twenty thousand lines of Java across five services, and no automated build. Consequences:

- **Phase 4 gate criterion 4.1** requires "Sandbox E2E suite for the Term path runs in CI." There
  is no CI for it to run in. The criterion cannot be satisfied as written.
- **Criterion 4.7** requires "coverage gates green." JaCoCo is configured in `build.gradle.kts`,
  but nothing executes it on a pull request. The gate is declarative only.
- Every "green" claim in every phase STATUS document rests on someone having run Gradle locally.
  That is an assertion, and [04-STAGE_GATES §2](../governance/04-STAGE_GATES.md) is explicit that
  evidence is an artefact, not an assertion.
- No SAST, no dependency/SCA scanning, no secret scanning, no container image scanning, on a
  regulated financial application handling PAN, Aadhaar, and health data.

The governance system is more rigorously automated than the banking software it governs. That
inversion is the single clearest statement of where the foundation went missing.

### GAP-B — There is no environment or infrastructure foundation · **CRITICAL**

The architecture specifies AWS EKS, Aurora PostgreSQL Multi-AZ, DynamoDB, MSK, Karpenter/HPA/KEDA,
ap-south-1 primary with ap-south-2 DR, and KMS CMKs.

What exists: **`render.yaml`** — a single `starter`-plan Render.com web service running two JVMs
inside one container, with a comment acknowledging the plan's RAM is probably insufficient.

There is **no Terraform, no CloudFormation, no CDK, no Helm chart, no Kubernetes manifest**, and
no environment definition for dev, UAT, or production. There is no AWS landing zone, no VPC
design, no network trust boundary implementation, no KMS key hierarchy, no S3 Object Lock
configuration for the 7-year IRDAI retention obligation.

Data residency is a **regulatory** requirement (all data in AWS India regions). Render.com's
region is not chosen, not verified, and not evidenced. Any customer PII processed through the
current deployment path is a data-residency question nobody has answered.

### GAP-C — The compliance hard-gates that make this business legal are not implemented · **CRITICAL**

The business statement lists these as non-negotiable:

| Control | Required behaviour | Implementation status |
|---|---|---|
| Suitability hard-gate | Quote APIs return **403** without a valid suitability evaluation ID | **Not implemented** — no Suitability service exists |
| Consent evidence | Append-only capture of statement text, version, CIF, OTP txn ID, timestamp, IP | **Not implemented** — no Consent service exists |
| Attribution | `distributorId` injected server-side, never caller-supplied | Partially implemented in the 1SB adapter |
| Payment device isolation | Payment must execute on the customer's device, never the RM's | **Not implemented** — no Payment service exists |

Simultaneously, the PO gap register carries **GAP-006 (consent rules not executable)** and
**GAP-007 (suitability content undefined)** as **P0 — "block scope / build freeze"**, still open.

Read those two facts together. A P0 gap explicitly labelled *build freeze* is open, and we
delivered a quote path and moved it to hardening anyway. The quote path we hardened is one that,
in production, would generate quotes without the suitability gate IRDAI requires. Bypassing
suitability before quote is described in our own requirement baseline as illegal.

This is not a missing feature. It is a **hardened path that is not lawfully shippable**, and
hardening it further does not change that.

### GAP-D — The platform is not a governed workstream · **STRUCTURAL**

`CURRENT-STATE.yaml` defines two workstreams: WS-1 (1SB Integration) and WS-2 (Workforce Auth).
Both are real, both are well run.

**Neither of them is the product.** The AU Bank Insurance Distribution Platform — the 19 contexts,
the journeys, the Flutter app, the P&L — has no workstream, no current stage, no gate, and no
owner in the state file. It exists in `docs/au-bank-insurance-platform/` as requirements, and
nowhere in the execution model.

This is the root cause, and everything above is downstream of it. Governance evaluates stage fit
against a workstream ([Rule LC-1](../governance/03-LIFECYCLE.md#5-multi-workstream-evaluation)).
If the platform is not a workstream, then platform foundation work — CI, IaC, the Consent
service, the Flutter app — belongs to no workstream, and therefore triages as **SC2/SC3: out of
scope**. The governance model has been correctly and faithfully *excluding the foundation* from
scope, because the thing the foundation belongs to was never registered.

The framework is not broken. It is pointed at a supplier interface and asked to certify a
platform.

### GAP-E — Test depth does not support a hardening claim · **HIGH**

Test-to-main file ratios: 1SB service 42:147 (29%), persistence 7:45 (16%), authorization 4:20
(20%), BFF 4:19 (21%), IdP adapter 2:8 (25%).

QA-001 sits open at **P0** with service coverage on an "interim floor." TD-014 records no
WireMock or E2E coverage between the integration and persistence services — the exact seam the
Term path runs through. There is no contract test, no consumer-driven contract, and no E2E suite.

"Hardened" is a claim about evidence. The evidence infrastructure has not been built.

---

## 5. Stage position, honestly stated

Against the 16-stage model in [`02-STAGE-MODEL.md`](./02-STAGE-MODEL.md):

| Stage | Position | Evidence |
|---|---|---|
| S00 Ideation & Business Case | 🟡 Partial | Charter and vision exist; GAP-010 executive sponsor still unnamed |
| S01 Discovery & Capability Definition | 🟢 Strong | 6 volumes, capability map, stakeholder catalogue, journey canvas |
| S02 Regulatory & Compliance Framing | 🟡 Partial | Regulatory registry and control catalogue exist; GAP-006/007 open at P0 |
| S03 Business Requirements & Process Design | 🟡 Partial | BRD exists; GAP-008 (no acceptance criteria), GAP-016 (attributes missing) |
| S04 Product Definition & Release Slicing | 🟡 Partial | R0-SCOPE and PRD exist; GAP-012 quote rules, GAP-013 product matrix open |
| S05 Experience Design | 🔴 Missing | GAP-009 Figma unmapped; no design system; no UI code of any kind |
| S06 Domain & Information Architecture | 🟡 Partial | 19 contexts named; no platform aggregate/state model; GAP-016 |
| S07 Solution & Security Architecture | 🟢 Strong | 8-part architecture review, ADR log, security package; GAP-017 NFR numbers open |
| **S08 Engineering Foundation** | 🔴 **Missing at 2026-08-15 baseline** | **No application CI at assessment time. No SAST/SCA/secret scanning. QA-001 P0 open** |
| **S09 Platform & Environment Foundation** | 🔴 **Missing** | **No IaC, no EKS, no environments, no observability stack, no KMS** |
| S10 Integration & Connectivity | 🟡 Partial | 1SB connectivity genuinely good; CBS, PG, AD, notification absent |
| S11 Vertical Slice (MVP) | 🔴 Not started **at platform level** | Adapter slice done; no business journey runs end to end |
| **S12 Hardening & Certification** | 🟠 **In progress at 2026-08-15 baseline** | **WS-1 Phase 4 then had 5 of 7 criteria open; see live gate evidence for current classification** |
| S13 Expansion & Scale | ⚪ Not started | Queued as WS-1 Phase 5 |
| S14 Production Readiness & Go-Live | ⚪ Not started | — |
| S15 Operate & Evolve | ⚪ Not started | — |

**The shape of that column is the whole problem.** Active work sits at S12. Two stages beneath it
are empty, and the stage that should carry the product — S11 — has not begun. You cannot certify
S12 for a system that has no S08 and no S09; hardening is the act of producing evidence, and
evidence requires the machinery S08 and S09 were supposed to install.

---

## 6. What is genuinely good, and must not be discarded

A realignment that treats existing work as waste would be a second, larger error. The following
is high quality and is **kept in full**:

1. **The AIGEM governance framework.** Stage-fit triage, the parked-backlog discipline, the
   seven-board review model, evidence rules, and change control are better than most enterprise
   programmes achieve. The realignment *uses* AIGEM; it does not replace it.
2. **The persona and authority model.** Nine personas with genuine segregation of duties, binding
   vetoes, a named deadlock terminator, and R12's decision-forcing power. This is the operating
   system for everything below.
3. **The 1SB integration service.** Ports-and-adapters, ArchUnit-enforced boundaries, replaceable
   middleware, LOB routing, async job/poll infrastructure. It becomes bounded context #15 under
   the platform, unchanged. Roughly 147 files of correct work.
4. **The business discovery corpus.** Six volumes, five phase artefacts, capability map, journey
   canvas, information model, glossary. S01 is close to exemplary.
5. **The architecture review.** Target microservices, communication patterns, AWS infrastructure,
   data architecture, security/NFR, ADR log. The blueprint is sound; it simply has not been built.

**Nothing in the realignment plan deletes any of this.** The problem is not what was built. It is
that it was built in an order that left nothing underneath it.

---

## 7. How this happened — so it does not recur

Not blame. Mechanism. Four causes, each with a fix carried into the framework.

| # | Mechanism | Fix |
|---|---|---|
| 1 | The **supplier interface was mistaken for the product**. 1SB integration was tractable, well-specified, and had a willing partner, so it became the de-facto programme. | Register the platform as **WS-3** and make it the primary workstream ([03 §4](./03-REALIGNMENT-PLAN.md)) |
| 2 | **Governance was built before the thing governed.** AIGEM has CI enforcement; the application does not. Rigour was applied where it was easy to apply. | S08 gate: no stage may pass S08 until the application has the same enforcement the docs already have |
| 3 | **Documentation maturity was read as delivery maturity.** A 250-file docs tree creates a strong impression of progress that file counts in `services/` do not support. | Every gate criterion in this bible requires an **executable or measurable** artefact — a CI run, a scan report, a restore test — never a document asserting a state |
| 4 | **P0 "build freeze" gaps did not freeze the build.** GAP-006 and GAP-007 are labelled build-freeze and are open, and building continued past them. | Freeze authority made explicit and assigned: an open P0 business gap blocks *entry* to S11, enforced at the gate |

---

## 8. What I am asking for, as Product Owner

1. **Stop the ascent.** WS-1 Phase 5 (Health/Motor LOB expansion) does not start. Adding LOBs to
   an unlawfully-gated quote path multiplies the defect; it does not deliver value.
2. **Register the platform as WS-3** in `CURRENT-STATE.yaml`, with S08 as its current stage. This
   requires a CR and Architect ratification — I can propose scope, I cannot edit `current_phase`.
3. **Authorise a Foundation Recovery Increment** covering S08 and S09, sequenced in
   [`03-REALIGNMENT-PLAN.md`](./03-REALIGNMENT-PLAN.md).
4. **Re-scope WS-1 Phase 4 honestly.** Criteria 4.1 and 4.7 are not achievable without S08. They
   should be marked `BLOCKED` on a named dependency rather than left `OPEN` as though effort will
   close them.
5. **Close GAP-006 and GAP-007 before S11 entry.** Consent and suitability rule packs are mine to
   deliver with Compliance. Until they exist, no platform vertical slice starts.

None of this is a criticism of the engineering. It is a request to put the floors in the order
that lets the building stand.

---

## 9. Ratification

This assessment is **PO-authored and not yet ratified**. Under
[14-CHANGE_CONTROL](../governance/14-CHANGE_CONTROL.md), adopting it requires **CR-010** (next
unused CR per `id_allocation`), carrying:

- registration of WS-3 in `state/CURRENT-STATE.yaml` (Architect + PO);
- the Phase 4 criteria re-statement (Architect + QA Lead);
- the S08/S09 recovery increment (Architect, SRE, Security, Delivery);
- the S11 entry condition on GAP-006/007 (Compliance + PO).

Boards that must return a verdict before this becomes binding: **Architecture (Mahesh),
Security (Deepali), Risk & Compliance (Shailja), QA (Swapnali), Operations (Shivanshi),
Delivery (Kalpana)**. My Product verdict is recorded above; it is one of seven.
