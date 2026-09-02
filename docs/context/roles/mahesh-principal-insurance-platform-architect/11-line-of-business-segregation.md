# 11 — Mahesh Line-of-Business Segregation Doctrine

## 1. Purpose

This file answers one question with enough precision that it stops being re-argued:

> **When does a line of business get its own anything — and when is it a field?**

The answer matters because both failure modes are expensive. Fork too eagerly and the platform
becomes three platforms with one logo, three consent implementations and three audit trails. Fork
too late and a Health release breaks Life, which is the failure the bank will actually notice.

The governing statement, from `VIN-001 §8`:

> **LOB is an isolation boundary, not one giant service.** Life, Health and General should be
> independently deployable, scalable, releasable, monitorable and failure-isolated. Life traffic
> should not force us to scale Health. A Health release should not break Life. **But a cell does not
> mean one `life-service`** — it means a collection of LOB-specific capabilities.

`VA-1` is the **one** variation axis where multiplication of deployable units is legitimate
(`09 §4.1 VR-01`). That licence is narrow, and this file is the boundary of it.

---

## 2. The LOB cell

A **cell** is the set of capabilities where Life, Health and Motor genuinely differ, deployed and
operated as an isolation unit.

```text
                    Plane 1 — Shared Customer & Sales Platform
                    Party · Opportunity · Work Mgmt · Consent
                    Product Governance · Journey Registry
                                     │
                            routes by LOB (CAP-106)
                                     │
   ┌─────────────────────────────────┼─────────────────────────────────┐
   ▼                                 ▼                                 ▼
┌──────────────────┐        ┌──────────────────┐        ┌──────────────────┐
│  LIFE CELL  (H0) │        │ HEALTH CELL (H2) │        │ MOTOR CELL  (H3) │
│  Journey Exec    │        │  Journey Exec    │        │  Journey Exec    │
│  Suitability     │        │  Suitability     │        │  Eligibility     │
│  Quotation       │        │  Quotation       │        │  Quotation       │
│  Proposal / Case │        │  Proposal / Case │        │  Proposal / Case │
│  Provider Integ. │        │  Provider Integ. │        │  Provider Integ. │
└────────┬─────────┘        └────────┬─────────┘        └────────┬─────────┘
         └───────────────────────────┼───────────────────────────┘
                                     ▼
        Plane 3 — Payment · Documents · Policy Portfolio · Notification · Audit
```

### 2.1 What belongs inside a cell

| Capability | Why it is LOB-specific |
|---|---|
| `CAP-201` Journey Execution | Stage detail, transitions and rules differ per line; a shared machine covering all lines becomes the giant state machine `VIN-001 §7` warns against |
| `CAP-202` Suitability / Eligibility | Life asks income, dependents, cover, tenure, objectives · Health asks members, conditions, sum insured, geography · Motor asks vehicle eligibility, not Life-style suitability at all |
| `CAP-203` Quotation | Life, Health and Motor quotes have radically different data and pricing constructs — `quoteCategory` alone means Premium/SA/Income in Term, Sum Insured in Health, New/Roll-Over in Motor |
| `CAP-204` Proposal / Case Management | Application data, insured persons, declarations and insurer questionnaires vary enormously by line |
| `CAP-205` Provider Integration runtime | Provider paths and payloads are LOB-scoped (`/insurance/lifeterm/v1`, `/insurance/lifehealth/v1`, `/insurance/motor/v1`), and Life quote volume must not consume the capacity Health integration depends on (`PR-36`) |

### 2.2 What must never be inside a cell

| Never in a cell | Owner | Why |
|---|---|---|
| Party / Customer | `CAP-101` | The same person buying Life and Health is one customer |
| Opportunity, assignment, queues | `CAP-102`, `CAP-103` | Sales mechanics do not vary by line; LOB is a routing attribute |
| Consent evidence | `CAP-104` | One consent record must be answerable across all lines |
| Bank-approved product offering | `CAP-105` | Three insurer masters produce three different answers to "what do we distribute?" |
| Journey identity, ownership, routing | `CAP-106` | Journey must survive a channel change; identity cannot live in a cell |
| Payment mechanics | `CAP-301` | Payment is common; the LOB decides *when* and *how much*, not *how* |
| Document storage | `CAP-302` | The LOB decides which document is required; storage is infrastructure |
| Policy portfolio | `CAP-303` | The customer has one portfolio across lines |
| Notification delivery | `CAP-304` | Channel delivery is not an insurance concern |
| Audit evidence store | `CAP-305` | Evidence must be reconstructable across lines in one place |
| Identity, authorization, config, observability, events | Plane 5 | Forking these forks the platform's controls |
| Canonical provider contracts, provider registry, credential framework, resilience policy, error model, routing policy | `CAP-402` / `CAP-403` control plane | Share the **control plane**; isolate only the **data plane** (`PR-36`, [`17 §14`](./17-provider-aggregation-and-connectivity.md)) |

**Rule LS-01.** A cell contains *execution*. It never contains *identity, evidence, governance or
money mechanics*.

**Rule LS-01a — a cell contains a provider *runtime*, never a provider *dependency*.** The Life cell
executes provider calls; it does not know which provider answered, and no cell capability may name
1SB, an insurer or a protocol outside its adapter package (`TI-19`, `PR-07`, `INV-ACL-01`).

---

## 3. The shared-versus-LOB decision test

Apply in order. The first line that answers decides.

| # | Question | If yes |
|---|---|---|
| 1 | Would two LOBs need **different answers to the same question about the same person or the same policy**? | **Shared.** Two answers is the bug |
| 2 | Is it **evidence, governance, money or identity**? | **Shared** (`LS-01`) |
| 3 | Does the **data model itself** differ materially between lines — not just its values? | **LOB-specific** |
| 4 | Do the **business rules** differ while the *framework* around them is identical? | **Shared framework, LOB-owned rules** (the `CAP-202` pattern) |
| 5 | Does it terminate at a **provider protocol** that is LOB-scoped? | **LOB runtime, shared contract** (`09 §5.1`) |
| 6 | Is the only difference a **field value** (`lobInterest`, `productType`, a queue name)? | **Shared.** A field is not a boundary |
| 7 | None of the above | **Shared by default.** Sharing is reversible; forking is not (`AP-10`) |

**Rule LS-02 — the default is shared.** Splitting is the exceptional act and carries the evidence
burden. Merging three implementations later is far more expensive than splitting one later, because
by then each has its own data, its own defects and its own stakeholders.

---

## 4. What actually differs between lines

Grounded in [`universal-lob-journey.md`](../../../1sb-insurance-integration/journeys/universal-lob-journey.md).
This table is the evidence base for §3, and it is the answer to "prove Health is really different."

| Dimension | Life (Term) | Life (Savings/ULIP/Annuity) | Health | Motor |
|---|---|---|---|---|
| Universal 9 stages | Same | Same | Same | Same |
| Quote category semantics | Premium / SA / Income | Premium / investment options | **Sum Insured** | **New / Roll-Over** |
| Pre-quote gating | Gate criteria form | Gate criteria form | Configuration constraints | Vehicle eligibility |
| Rating inputs | Age, cover, tenure, health declarations | Fund/annuity options | Members + relationships, conditions, geography | Vehicle master chain, RTO, financier, IDV, NCB, previous policy |
| Master data | Product enums | Fund lists, performance | Network lists, product families | **Vehicle type → make → model → fuel → variant** |
| Insured parties | Usually one life | One life | **Multiple members, family floater** | Vehicle + owner |
| KYC coupling | Standard | Standard | **Stronger CKYC coupling** | Standard |
| Extra artefacts | BI, riders, ROP addons, payout options | Fund performance | Plan details, network, wording | Policy download, inspection |
| Payment initiation | Common | Common | **LOB-specific payment URL API** | **LOB-specific payment URL** |
| Provider path prefix | `/insurance/lifeterm/v1` | `/insurance/lifesave/v1` | `/insurance/lifehealth/v1` | `/insurance/motor/v1` |

**Reading.** The *shape of the journey* is identical across all four — which is why Journey
Registry, Consent, Payment, Documents, Audit and Portfolio are shared. The *content at each stage*
diverges sharply — which is why Suitability, Quotation, Proposal and provider integration are
cellular.

**Motor is the largest deviation**, because of the asset master chain. Two consequences:

1. **Vehicle/asset master data is a data-ownership problem, not a journey problem.** It may earn its
   own capability inside or adjacent to the Motor cell. That is a `CAP-105`-adjacent decision, taken
   at H3 on evidence, not assumed now.
2. Motor's "suitability" is **eligibility**, not need analysis. `CAP-202` is named
   *Suitability & Eligibility* for exactly this reason — and the distinction is a compliance one, so
   the Motor need-analysis obligation is **Shailja's question, not Mahesh's assumption.**

**Life sub-lines (Savings/ULIP/Annuity/Pension) are not separate cells.** They share Term's
life-style skeleton and differ in quote fields and master data — `VA-2` (product), not `VA-1` (line).
Modelling ULIP as a cell would be the clearest possible violation of `LS-02`.

---

## 5. Isolation must be demonstrable

`VIN-001 §32`: *independence should be demonstrable operationally, not just shown as separate
diagram boxes.* This is `TI-18`, and it is where most "microservices" architectures quietly fail.

**Rule LS-03.** A cell claim of independence is unproven until each row below has a named
verification method.

| Claim | Verification |
|---|---|
| Independently deployable | Health deploys with no Life deployment, proven in a pipeline run |
| Independently scalable | Health load test leaves Life latency percentiles unmoved |
| Independently releasable | Contract tests prove a Health schema change cannot break Life |
| Independently monitorable | Per-LOB dashboards and SLOs exist and alert separately (`VIN-001 §31`) |
| Failure-isolated at LOB level | Health cell forced-failure exercise; Life journeys complete throughout |
| Failure-isolated at provider level | One insurer's forced timeout leaves other insurers' quotes unaffected — per-provider bulkheads and bounded pools |

**Rule LS-04 — shared capabilities are the real coupling.** Cells are isolated from *each other*,
but every cell depends on Payment, Consent, Registry, Authorization and Audit. Cell isolation
therefore does not reduce the availability requirement on shared capabilities — it concentrates it.
A target state that celebrates cell independence without raising the availability bar on Plane 1
and Plane 3 has moved the risk, not removed it.

**Rule LS-05 — isolation is also a blast-radius property for data.** A cell's stores are the cell's.
Cross-cell data access is a `TI-05` violation regardless of how convenient the query would be.

---

## 6. Adding a line of business

**Rule LS-06 — entry conditions before design.** `DEC-20260816-05` freezes LOB expansion until
`GATE-S08` **and** `GATE-S11` pass for the R0 Term journey, because adding lines to a quote path
that lacks its lawful suitability gate multiplies a compliance defect across three lines. `VIN-001
§36` reaches the same conclusion from the delivery side: *only after the shared platform is proven
should Health be plugged into the same architecture.*

### 6.1 LOB onboarding checklist

A new LOB is ready to be designed when all of these are answerable, and ready to be built when all
are answered:

| # | Item | Owner |
|---|---|---|
| 1 | Products, insurers and bank-approved offering registered in `CAP-105` — with effective dates, channels and ETB/NTB availability | Product + Bancassurance |
| 2 | Suitability/eligibility rule pack for the line, versioned, on the shared framework | Rajal + **Shailja** (permissibility) |
| 3 | Consent text and purposes covering the line's data use | **Shailja** |
| 4 | Canonical quote and proposal models for the line, mapped to the provider contract | Mahesh |
| 5 | Provider integration: paths, payloads, credentials, idempotency, bulkhead budget | Mahesh + Deepali (credentials) |
| 6 | LOB-specific document requirements mapped onto `CAP-302` | Rajal |
| 7 | Payment trigger points and amount semantics mapped onto `CAP-301` | Mahesh |
| 8 | Per-LOB dashboards, SLOs and alerting | **Shivanshi** |
| 9 | Isolation verification plan for every row of `LS-03` | Shivanshi + Swapnali |
| 10 | Physical data topology for the cell | **Aarti** |
| 11 | Confirmation that **no shared capability changed** to accommodate the line | Mahesh |

**Row 11 is the acceptance test for this whole doctrine.** If adding Health requires editing the
shared journey stage vocabulary, the Consent model, the Payment API or the audit event schema, the
LOB seam did not work and the finding is `A1` — reported before the LOB ships, not after.

### 6.2 The Life cell is not a special case

R0 builds Life without calling it a cell (`09 §2.1 HR-05`). Two H0 obligations follow, and both are
cheap now and expensive later:

1. **No Life concept may enter a shared capability's contract.** Not in Journey Registry's coarse
   stages, not in Consent, not in Payment, not in the audit event schema.
2. **The Life cell's boundary must be traceable in the code today** — package structure and ArchUnit
   rules that already separate what will become cellular from what will stay shared.

---

## 7. Anti-patterns

| Anti-pattern | Consequence | Correct move |
|---|---|---|
| One `life-service`, one `health-service` | A distributed monolith per line; the cell becomes the thing it replaced | A cell is several capabilities (`§2.1`) |
| Per-LOB Consent, Payment or Audit | Three evidence trails, three reconciliation models, one regulator | `LS-01` |
| A shared journey state machine with LOB branches | Every LOB change risks every LOB | Registry + cellular execution |
| ULIP or Annuity as its own cell | Confuses product with line | `VA-2`, not `VA-1` |
| A cell service that knows it is calling 1SB | The aggregator becomes a per-cell domain dependency; removing it is three rewrites | `TI-19`, `LS-01a` |
| Copying the Life cell to create the Health cell | Duplicates Life's assumptions and its defects into a line that does not share them | Instantiate the *pattern*; derive Health's rules from Health's requirements |
| Per-LOB insurer master | Three answers to "what do we distribute?" | `CAP-105` is shared |
| Adding a LOB by adding fields to shared contracts | Silent violation of `LS-06` row 11 | Report as `A1` before ship |
| Declaring isolation because the diagram has three boxes | Untested isolation fails on the day it is needed | `LS-03` verification |
| Building the Health cell to prove the seam works | The seam is proven by the shared platform running, not by a second cell | `VIN-001 §36`; `DEC-20260816-05` |

---

## 8. Authority

| Decision | Authority |
|---|---|
| Which capabilities are cellular versus shared | `A1_AUTONOMOUS` — Mahesh, ADR when it fixes a constraint |
| Splitting a cell capability into its own deployable unit | `A2_NOTIFY`, ADR, boundary-test evidence (`NS-04`) |
| Per-LOB integration runtime | `A2_NOTIFY` + ADR amending `SC-W3-5`; Deepali on credentials, Shivanshi on isolation evidence |
| Cell data topology | `A3_JOINT_REVIEW` — Aarti |
| Whether a LOB's suitability obligation differs | `A3_JOINT_REVIEW` — **Shailja**; Mahesh does not interpret it |
| **Which LOB is launched and when** | **Product — Rajal**, gated by `DEC-20260816-05` |
| Accepting an isolation exception at go-live | `A4_HUMAN_REQUIRED` |
