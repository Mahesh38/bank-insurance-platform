# 12 — Mahesh Journey Segregation Doctrine

## 1. Purpose

A journey is the most over-loaded concept on a distribution platform. Everyone uses the word, and
they mean at least four different things: the customer's experience, the sales opportunity, the
state machine, and the audit narrative. This file separates them, and then answers:

> **When does a journey fork — and when is it the same journey with a different actor, channel or
> stage?**

The default answer is **it does not fork**. `TI-12` and `TI-13` are the two invariants that make
that default safe rather than naive.

---

## 2. Three objects, three lifetimes

`VIN-001 §21` gives the shape the repository adopts:

```text
Original sale     Opportunity O1  ──▶  Journey J1  ──▶  Policy P1
Renewal           P1 ──▶ Renewal Opportunity O2      ──▶  Journey J2
Lapse recovery    P1 ──▶ Lapse Recovery Opportunity O3 ──▶ Journey J3
```

| Object | Answers | Lifetime | Owner |
|---|---|---|---|
| **Opportunity** | *Why are we contacting this person?* | Opens and closes on sales outcome; may spawn many journeys | `CAP-102` |
| **Journey** | *Where has this specific purchase process reached?* | One attempt at one purchase; terminal states are final | `CAP-106` + `CAP-201` |
| **Policy** | *What did the customer end up owning?* | Years; renews, lapses, is serviced | `CAP-303` |

**Rule JS-01 — journeys are immutable history (`TI-13`).** A completed or abandoned journey is
never reopened. Renewal, lapse recovery and abandonment-driven re-attempts create a **new
opportunity and a new journey**, linked to the old one by reference.

Why this is structural rather than tidy: a reopened journey destroys the answer to *what actually
happened the first time*, which is precisely the question asked in a mis-selling review, a
reconciliation break or a complaint. `TI-07` (audit reconstructs business action) is unachievable if
journeys are mutable.

**Rule JS-02 — one opportunity, many journeys.** A customer who abandons a Term quote, is
re-engaged, and buys three weeks later has one opportunity and two journeys. The conversion metric
belongs to the opportunity; the process record belongs to each journey.

---

## 3. Registry versus execution

`VIN-001 §7`, adopted:

```text
   Journey Registry  ──▶  LOB Router  ──▶  Life / Health / General Journey Execution
      (shared)                                        (cellular)
```

| | **Journey Registry** `CAP-106` | **Journey Execution** `CAP-201` |
|---|---|---|
| Answers | Who is it for · which LOB · what coarse stage · who owns it now · where does it route | What exactly happens next in *this* line of business |
| Scope | Shared, cross-LOB | Inside the LOB cell |
| Holds | Identity, references, coarse stage, lifecycle status, current actor and channel | Detailed stage machine, transitions, LOB rules, resumption points |
| Must not hold | Any LOB stage detail; any other context's business decision (`SC-W3-6`) | Journey identity or routing |

**Rule JS-03 — the registry holds stage and references only.** This is `SC-W3-6` / `INV-JRN-02`,
enforced by `FF-04`. `VIN-001 §7` reaches it independently: the split exists *precisely* to stop the
shared journey service becoming a giant state machine containing every insurance type.

**Rule JS-04 — the coarse stage vocabulary is LOB-agnostic, small, and frozen hard.** It is the
single highest-risk shared contract in the platform: everything routes on it, and every LOB will
want to add to it.

A candidate coarse vocabulary, derived from the universal nine stages:

```text
INITIATED · QUALIFYING · QUOTED · PROPOSED · UNDER_ASSESSMENT
AWAITING_PAYMENT · PAID · ISSUED · CLOSED
```

with lifecycle status `ACTIVE | ABANDONED | COMPLETED` held separately.

**The test that keeps it honest:** *could Motor be added without changing this list?* If not, the
list has absorbed Life detail. `LS-06` row 11 makes this an acceptance criterion for every LOB
onboarding, and it applies at H0 — before the second line exists to reveal the problem.

**Rule JS-05 — R0 combines registry and Life execution in one service, and that is correct.**
`#9 Journey Orchestration` is Registry + Life execution today (`NS-03`). The split becomes real at
H2. Its cost is determined entirely by whether `JS-04` was respected at H0.

---

## 4. Channel continuity — one journey, many actors

`TI-12`, from `VIN-001 §6`:

> Customer starts on mobile → Call centre assists → RM continues → Customer pays on web.
> **All of that remains the same `journeyId`.**

**Rule JS-06 — the journey survives channel and actor change.** Handover changes *who is assisting*,
not *which journey exists*. Consequences that must be designed, not assumed:

| Consequence | Design requirement |
|---|---|
| Current assisting actor is journey state | Registry holds `currentActor` and `currentChannel`, and every change is an audited event |
| Handover must be explicit | A transition with an actor, a timestamp, a reason and an authorization check — never an implicit side effect of someone opening a screen |
| Concurrency is real | Two actors may hold the journey at once (RM on the phone, customer on web). Optimistic concurrency on the journey aggregate, and a defined last-writer rule |
| Resumption is a first-class path | Every stage declares whether it is resumable, and from where |
| Device isolation survives handover | `TI-11` is unconditional: consent OTP and payment happen on the customer device regardless of who is assisting |
| Suitability follows the journey, not the actor | A suitability assessment does not become invalid because an RM took over — nor does an RM's involvement create one that never happened |

**Rule JS-07 — never create a duplicate journey because an actor changed.** `VIN-001 §22` states it
directly: *no duplicate proposal/journey should be created merely because another actor took over.*
Duplicate journeys are the most common way a platform quietly double-submits a proposal to an
insurer.

---

## 5. Actors, not architectures

`TI-14`, from `VIN-001 §23`:

> Don't build a `call-center-quote-service`. The **same** Life Quote capability serves Customer, RM,
> Call centre and Certified SP. What differs is authorization and certification.

**Rule JS-08 — a new actor type is an authorization change, not an architecture change.** Adding the
call centre adds: an actor type, authorization rules, queues (`CAP-103`), and possibly engagement
cadence (`CAP-306`). It adds **no** business service.

**Rule JS-09 — certification gates regulated activity, and Compliance defines the gate.** A
non-certified actor may assist navigation and arrange a callback; a certified actor may perform
regulated sales activities permitted by the approved business and compliance model. **Which actions
require which certification is Shailja's determination** — Mahesh builds the gate, he does not set
its threshold. `ARCH-022` already sources RM certification from AD, so the mechanism exists; the
policy does not.

**Rule JS-10 — the permission decision is server-side (`TI-15`).** A UI that hides a button is a
usability feature. The control is the PDP refusing the action (`CAP-502`, default-deny, fail
closed). Anything else means the control is one HTTP client away from absent.

---

## 6. Journey variants

A **variant** is a declared path through the one journey model. Variants are how `VA-4` (channel)
and `VA-5` (assistance mode) express themselves without forking anything.

**Rule JS-11 — a variant is legitimate only when it changes one of:**

1. the **mandatory control set** (which of C1–C10 apply, and where);
2. the **actor and consent model** (who may act, whose device captures what);
3. the **terminal outcomes** (what states this variant can end in);
4. the **evidence set** required for audit.

A variant that changes only screens, copy, ordering or effort is **not** a variant. It is UI.

**Rule JS-12 — variants are declared, not discovered.** Each is a registered configuration artefact
with an owner, so the set of supported journeys is enumerable rather than emergent:

```yaml
journey_variant:
  id: JV-...
  name: "..."
  lob: LIFE | HEALTH | GENERAL
  channel: RM_WORKSPACE | CUSTOMER_DIY | CALL_CENTRE | CERTIFIED_SP | BRANCH
  assistance_mode: ASSISTED | DIY | HYBRID | CALL_CENTRE_ASSISTED | SP_ASSISTED
  customer_type: ETB | NTB
  connectivity_mode: AGGREGATOR | DIRECT | REDIRECT
  process: NEW_BUSINESS | RENEWAL | LAPSE_RECOVERY | ABANDONMENT_RECOVERY
  mandatory_controls: [C1, C2, C4, ...]
  actor_requirements:
    certification_required: true | false            # Shailja determines
    permitted_actions: []
  device_rules:
    consent_capture: CUSTOMER_DEVICE
    payment: CUSTOMER_DEVICE                        # TI-11, never variable
  terminal_states: []
  resumption:
    resumable_from: []
    expiry: "..."
  evidence_set: []
  horizon: H0 | H1 | H2 | H3
  status: SUPPORTED | PLANNED | NOT_SUPPORTED
```

### 6.1 Variants across horizons

| Variant | Horizon | Standing |
|---|---|---|
| Life · RM workspace · assisted · ETB · aggregator · new business | **H0** | The R0 slice |
| Life · customer DIY | H1 | `DEC-20260816-03`; `GAP-023` is an R1 entry condition (`DEC-20260816-04`) |
| Life · call-centre-assisted | H1 | **Product decision outstanding — Rajal** |
| Life · certified-SP-assisted | H1 | **Product + Compliance outstanding** |
| Life · hybrid | H2 | `DEC-20260816-03` |
| Life · renewal / lapse recovery | H1 | New opportunity + journey (`JS-01`) |
| Health · all supported Life variants | H2 | Gated by `DEC-20260816-05` |
| Group B redirect | not scheduled | Out of the R0 platform slice — Rajal |

**Rule JS-13 — controls do not weaken because an RM is absent.** A DIY journey has no RM to perform
need analysis, and that is not a reason to relax `C1`. If the assessment cannot be conducted
lawfully in self-service, the correct outcome is *the variant is not supported yet* — not a variant
with a weaker gate. That call is Shailja's, and it is an H1 entry condition, not an implementation
detail.

**Rule JS-14 — Group B redirect terminates honestly.** Where the bank hands off to an insurer
journey it cannot observe, the journey reaches an explicit `HANDOFF` terminal state with the
evidence captured up to that point. **The platform never simulates stages it cannot see.** A
fabricated `ISSUED` is worse than an honest `HANDOFF`.

---

## 7. Abandonment and recovery

`VIN-001 §22`, as a designed sequence:

```text
Customer reaches Quote and leaves
        │
        ▼  journey keeps its existing state — nothing is deleted or reset
CAP-306 Engagement detects inactivity and decides whether/when to engage
        │
        ▼
CAP-103 Work Management creates a recovery work item
        │
        ▼
Call centre / SP / RM resumes THE SAME journey  (TI-12, JS-07)
```

**Rule JS-15 — abandonment is a lifecycle status, not a deletion.** The journey stays queryable, its
evidence stays intact, and its stage is the starting point for recovery.

**Rule JS-16 — recovery of a *terminal* journey creates a new one.** If the journey has expired or
been closed, recovery is a new opportunity and a new journey (`JS-01`). Only a still-`ACTIVE`
journey is resumed in place. The boundary between the two is the journey expiry policy, which each
variant declares (`JS-12`) — **Product owns the values.**

**Rule JS-17 — engagement decides, notification delivers.** Detecting inactivity and choosing the
cadence (30 min → push · 6 h → WhatsApp · 24 h → call-centre work item) is `CAP-306`. Sending the
message is `CAP-304`. Merging them produces a notification service full of business rules that
nobody can find (`NS-08`).

---

## 8. What a journey must never own

| Not owned by Journey | Owner | Failure if violated |
|---|---|---|
| Another context's business decision | The owning context | `SC-W3-6` / `FF-04` violation — `A0` |
| Suitability verdict, consent evidence, quote figures, proposal data, payment status, policy record | `CAP-202`, `CAP-104`, `CAP-203`, `CAP-204`, `CAP-301`, `CAP-303` | The orchestrator becomes a distributed monolith |
| Queues, assignment, SLA, escalation | `CAP-103` | Journey becomes a work manager and stops being resumable |
| Whether to engage the customer | `CAP-306` | Marketing cadence lands in the state machine |
| Whether an actor may perform an action | `CAP-502` | Authorization by convention rather than by control |
| The audit record | `CAP-305` | Evidence held by the thing being evidenced |

**Rule JS-18 — the journey holds references and stage. If it holds an answer, it is wrong.**

---

## 9. Anti-patterns

| Anti-pattern | Consequence | Correct move |
|---|---|---|
| A journey service per channel | Business logic duplicated per actor; continuity impossible | `JS-08` — actors, not architectures |
| Reopening a journey for renewal | Historic record destroyed; audit cannot reconstruct | `JS-01` |
| New journey on actor handover | Duplicate proposals to the insurer | `JS-07` |
| Coarse stages that mirror Life's detailed stages | Health and Motor cannot route without changing the shared contract | `JS-04` |
| Journey holding the suitability verdict "for convenience" | `SC-W3-6` violation; the gate can now be bypassed by writing state | `JS-18` |
| Weakening `C1` for DIY | Unlawful journey shipped as a feature | `JS-13` |
| Simulating insurer stages after a redirect | Fabricated evidence | `JS-14` |
| Variants that differ only by screen | Combinatorial explosion of "journeys" nobody can test | `JS-11` |
| Deleting abandoned journeys | Recovery impossible; conversion analysis impossible | `JS-15` |

---

## 10. Authority

| Decision | Authority |
|---|---|
| Registry/execution split, stage vocabulary shape, resumption design | `A1_AUTONOMOUS` — Mahesh, ADR when it fixes a constraint |
| Adding a journey variant to the model | `A2_NOTIFY` + variant declaration |
| **Which variants the bank supports, and journey expiry values** | **Product — Rajal** |
| Whether a control may differ in a variant | `A3_JOINT_REVIEW` — **Shailja**; `C1`/`C2`/`C4` are non-waivable |
| Certification thresholds for regulated actions | **Shailja** |
| Actor authorization model, handover authorization | `A3_JOINT_REVIEW` — Deepali |
| Launching a variant with a known control gap | `A4_HUMAN_REQUIRED` |
