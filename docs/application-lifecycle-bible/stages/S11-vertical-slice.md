# S11 — Vertical Slice (MVP)

**AIGEM stage:** L6 — Vertical Slice · **Owner:** Rajal (Product) + Amit (Engineering)
**Central question:** *Does one complete business journey work end to end?*

> **This is the stage that proves the business case, and this programme has never attempted it.**
> A hardened adapter is not a slice. A slice ends with an issued, reconciled, audited policy that a
> real RM sold to a real customer through a real interface.

---

## 1. Purpose

Make **one** business journey work completely, through every layer, for real users — thin in
breadth, complete in depth.

The discipline is refusing generalisation. One LOB, one product, one insurer, one channel, one
customer segment. Every instinct to "make it configurable while we are here" is deferred to S13,
because a framework built before the second case exists is a framework built against a guess.

The slice is done when someone who is not on the programme can sell a policy with it.

## 2. Entry criteria

**All of these are hard gates. Rule SM-4 applies.**

- [ ] GATE-S08 passed — CI, quality gates, test infrastructure
- [ ] GATE-S09 passed — environments, deployment, observability, secrets
- [ ] GATE-S10 passed for the dependencies this slice needs: aggregator, CBS, payment, identity, notification
- [ ] GATE-S05 passed for this journey — designed, validated with real RMs, regulated copy approved
- [ ] GATE-S06 passed — aggregates, journey saga, compensations designed
- [ ] **No open P0 business gap.** GAP-006 (consent) and GAP-007 (suitability) closed

## 3. The R0 slice, defined

| Dimension | R0 choice |
|---|---|
| Channel | RM-assisted |
| LOB / product | Life — one Term product |
| Insurer | One Group A insurer, via 1SB |
| Customer segment | ETB only |
| Journey | Lead → need analysis → suitability → consent → quote → proposal → payment → issuance → reconciliation → audit |
| Interface | Flutter RM app + customer-device payment and consent surfaces |
| Scale | Pilot: a named branch set, a named RM cohort |

**Explicitly not in the slice:** DIY and hybrid channels, Health and Motor, multi-insurer
comparison, NTB, ULIP and savings variants, Group B redirect, MIS and reporting, and the admin
console. Each is deferred to S13 with a revisit trigger.

> This is a **narrower R0 than the current `R0-SCOPE.md`**, which specifies assisted, DIY and
> hybrid on Day 1. Narrowing it is a Product recommendation in the realignment: one channel proven
> is worth more than three channels partially built, and it makes S11 achievable within the
> recovery timeline.

## 4. Epics and stories

### S11-E01 — Journey orchestration · *Amit + Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S11-E01-S01 | Implement the journey state machine | States and transitions per the S06 saga; state durable and resumable |
| S11-E01-S02 | Implement compensations | Every failure point has its designed compensation, tested |
| S11-E01-S03 | Implement save and resume | An abandoned journey resumes with full context, subject to consent and quote validity |
| S11-E01-S04 | Emit journey telemetry | Every state transition emits a metric and an audit event |

### S11-E02 — Lead and customer · *Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S11-E02-S01 | Create a lead | RM-initiated, attributed to RM and branch |
| S11-E02-S02 | Identify the ETB customer via CBS | CIF lookup; relationship verified; ETB rule enforced |
| S11-E02-S03 | Snapshot the customer profile | Only classified, purpose-justified fields; provenance recorded |

### S11-E03 — Suitability, consent and the compliance gates · *Amit + Shailja*

The regulatory heart of the slice.

| ID | Story | Acceptance criteria |
|---|---|---|
| S11-E03-S01 | Implement the need-analysis questionnaire | Per the S02 suitability pack; answers persisted with the algorithm version |
| S11-E03-S02 | Implement suitability evaluation | Deterministic; produces an evaluation ID with a validity window |
| S11-E03-S03 | **Implement the suitability hard-gate (C1)** | Quote endpoints return **403** without a valid evaluation ID. Negative test at 100% branch coverage |
| S11-E03-S04 | Implement override handling | Per the S02 rules; override recorded with actor, reason and disclosure |
| S11-E03-S05 | **Implement consent capture (C2)** | OTP to the customer's device; append-only record of statement text, version, CIF, OTP txn ID, timestamp, IP |
| S11-E03-S06 | Implement consent enforcement | Proposal cannot proceed without valid consent; negative test |
| S11-E03-S07 | **Implement attribution (C3)** | `distributorId` and SP licence server-injected; caller-supplied values rejected; expired certification blocks the sale |

### S11-E04 — Quote and proposal · *Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S11-E04-S01 | Generate a quote for the Term product | Through the existing 1SB path, now behind the suitability gate |
| S11-E04-S02 | Present the quote with disclosed basis | Comparison and ranking basis visible per the S05 design |
| S11-E04-S03 | Enforce quote validity | Expired quotes cannot be proposed against |
| S11-E04-S04 | Capture the proposal | Dynamic form per product; pre-filled from CIF; validated per the rules catalogue |
| S11-E04-S05 | Handle underwriting outcomes | Accepted, counter-offered, rejected, and pending — each with a defined actor experience |
| S11-E04-S06 | Upload and manage documents | Classified, encrypted, retained per schedule |

### S11-E05 — Payment and issuance · *Amit + Deepali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S11-E05-S01 | **Implement payment device isolation (C4)** | Link or QR to the customer's device; the RM app never renders a payment surface. Negative test |
| S11-E05-S02 | Handle payment status | Callback plus reconciliation polling; unknown state resolves within a defined window |
| S11-E05-S03 | Trigger and confirm policy issuance | Insurer confirmation received and persisted |
| S11-E05-S04 | Reconcile premium to policy | Both directions; unreconciled items surface on an operations queue |
| S11-E05-S05 | Implement the "policy sold" determination | All four conditions — issued, confirmed, reconciled, persisted |
| S11-E05-S06 | Deliver the policy document | COI generated or retrieved, delivered, and retrievable later |

### S11-E06 — RM application (Flutter) · *Digital + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S11-E06-S01 | Authenticate the RM | Via the BFF; no OAuth token reaches the client |
| S11-E06-S02 | Implement the journey screens | Per the S05 design, using the design system |
| S11-E06-S03 | Implement the customer-device hand-off | Consent and payment transitions, with status visible to the RM |
| S11-E06-S04 | Implement error, empty and degraded states | Per the S05 state catalogue; every failure tells the RM what to do |
| S11-E06-S05 | Implement offline and poor-connectivity behaviour | Branch connectivity is unreliable; no data loss on interruption |

### S11-E07 — Audit and observability · *Amit + Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S11-E07-S01 | Emit an audit event per regulated action | Immutable, attributable, with correlation to the journey |
| S11-E07-S02 | Prove journey reconstruction | Any completed or abandoned journey reconstructable from audit records alone |
| S11-E07-S03 | Instrument the journey KPIs | Conversion and drop-off per step, per the S04 KPI definitions |
| S11-E07-S04 | Build the journey dashboard | Business-visible: sales, drop-offs, failures by cause |

## 5. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S11-VT-01 | **A policy can actually be sold** | A real RM completes the journey in UAT with a test customer | Policy issued, confirmed, reconciled, audited |
| S11-VT-02 | The suitability gate blocks | Attempt a quote with no, expired, and mismatched suitability IDs | 403 in all three cases |
| S11-VT-03 | Consent is enforced and evidenced | Attempt a proposal without consent; then retrieve a consent record | Blocked; record complete and immutable |
| S11-VT-04 | Attribution cannot be spoofed | Send a caller-supplied `distributorId`; attempt a sale with expired RM certification | Rejected in both cases |
| S11-VT-05 | Payment isolation holds | Inspect every RM-app surface and network call during payment | No payment surface, no card data path on the RM device |
| S11-VT-06 | Money is never double-taken | Replay payment initiation; simulate duplicate callbacks | One charge, one policy |
| S11-VT-07 | Failure paths work | Inject failure at every journey step | Each produces the designed compensation and a defined RM experience |
| S11-VT-08 | Journeys resume | Abandon at each step; resume later | Context preserved, subject to validity rules |
| S11-VT-09 | Audit reconstructs the sale | Reconstruct a completed journey from audit records only | Complete reconstruction, including who did what and when |
| S11-VT-10 | No PII leaks | Scan all logs and telemetry after a full journey run | Zero regulated fields |
| S11-VT-11 | Real users can use it | 5+ RMs complete the journey unaided in UAT | ≥ 80% complete; time-on-task within the S05 target |
| S11-VT-12 | E2E suite runs in CI | Nightly run of the full journey suite | Green, repeatable |

**S11-VT-01 is the stage.** Everything else supports it. If a real RM cannot sell a policy, the
slice is not done regardless of how many components pass their own tests.

## 6. Exit gate — GATE-S11

| # | Criterion | Level | Evidence artefact |
|---|---|---|---|
| S11-G1 | Complete journey works end to end in UAT | E3 | Recorded walkthrough with correlation IDs and the issued policy |
| S11-G2 | All compliance hard-gates C1–C4 implemented and proven | E4 | Negative test results at 100% branch coverage |
| S11-G3 | E2E suite green in CI | E4 | CI run |
| S11-G4 | Journey reconstructable from audit | E3 | Reconstruction record |
| S11-G5 | RM app usable by real RMs | E3 | UAT usability record |
| S11-G6 | Failure paths and compensations proven | E4 | Failure-injection results |
| S11-G7 | KPIs instrumented and visible | E4 | Dashboard |
| S11-G8 | No PII in logs | E4 | Scan result |
| S11-G9 | Business acceptance of the slice | E2 | PO and business sign-off |

**Approvers:** Rajal (AP) · Mahesh (AP) · Amit (AP) · Swapnali (AP, B) · Deepali (AP, B) ·
Shailja (AP, B, **human**) · Shivanshi (AP) · Kalpana (AP) · Aarti (RV)

## 7. Current position in this repository — 🔴 Not started at platform level

**What exists:** an adapter-level Term path — master data, quote, proposal, payment session and
application status — implemented in `1sb-integration-service` (FUNC-001…007, FUNC-009) and
tech-lead approved. That is real work and it becomes part of this slice.

**What the slice needs and does not have:**

| Component | State |
|---|---|
| Journey orchestration | Absent — no saga, no state machine, no compensations |
| Lead | Absent |
| Customer / CBS | Absent |
| **Suitability + hard-gate (C1)** | **Absent** — the quote path has no gate |
| **Consent (C2)** | **Absent** |
| **Payment execution + device isolation (C4)** | **Absent** — only a payment *session* against 1SB |
| Policy issuance and reconciliation | Absent |
| **RM application** | **Absent** — no Flutter project exists |
| Audit reconstruction | Unproven |
| E2E suite | Absent |

**Assessment.** The delivered work is roughly the quote-and-proposal *plumbing* of one journey
step, hardened. The journey itself — the thing that generates revenue, satisfies the licence, and
proves the business case — has not been started. Entry to this stage is currently blocked on
GATE-S08, GATE-S09, GATE-S05, three S10 integrations, and two P0 business gaps.

That list is the honest scope of "getting to a working product", and it is what the realignment
sequences.

## 8. Premature at this stage

A second LOB · a second channel · multi-insurer comparison · configurability and frameworks ·
performance optimisation · admin consoles · MIS.

The strongest temptation at S11 is to generalise while building. Resist it: the second case is
what reveals the right abstraction, and it arrives at S13.
