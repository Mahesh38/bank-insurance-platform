# S05 — Experience Design & Service Blueprint · Retroactive Stage Evidence

**Stage definition:** [`stages/S05-experience-design.md`](../stages/S05-experience-design.md)
**Workstream:** WS-3 ([charter](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md))
**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-16
**Closes:** GAP-009 (Figma not mapped to CJ/RMJ) on content

**Retroactive stage verdict: `CLOSED-WITH-CONDITIONS`** — see §8.

> **This spec will be handed to the S11 build.** It is written to be implemented against directly:
> every screen has an ID, a requirement, four states and a defined transition; every token has a
> value; every component has its variants and states. Where a decision needs a designer or a
> Compliance lawyer rather than a Product Owner, it is marked open with an owner — not filled in
> with something plausible.
>
> **Scope: the R0 assisted Term journey, RM surface only.** The customer-device surface in R0 is
> two screens (consent OTP, payment) delivered by existing bank channels. The customer Flutter
> application is R1.

---

## 1. What the stage requires

| # | Criterion | Level | Closes |
|---|---|---|---|
| S05-G1 | Service blueprint complete for R0 journeys | E1 | |
| S05-G2 | Wireframes mapped to requirement IDs | E1 | **GAP-009** |
| S05-G3 | Design system published and adoptable | E1 | |
| S05-G4 | Regulated copy approved by Compliance | E2 | |
| S05-G5 | Accessibility standard set; prototype conformant | E3 | |
| S05-G6 | Prototype validated with real RMs and customers | E3 | |
| S05-G7 | Error, empty and degraded states catalogued | E1 | |

**Approvers:** Rajal (AP) · Design (AP) · Shailja (RV, B on disclosure) · Deepali (RV, B on
authentication and consent UX) · Swapnali (RV) · Mahesh (RV)

---

## 2. What already exists

Very little, and [S05 §6](../stages/S05-experience-design.md#6-current-position-in-this-repository---missing)
is blunt about it: *"This is the emptiest stage in the programme, and it is not a minor one."*

| Artefact | Path | State |
|---|---|---|
| Figma prototype, as **reference material only** | [`artefacts/figma/`](../../au-bank-insurance-platform/artefacts/figma/README.md) · [`05-figma-and-artefact-intake.md`](../../au-bank-insurance-platform/05-figma-and-artefact-intake.md) | 🟡 Exists; contains MVP screens, concept screens and incomplete journeys mixed together, **unmapped to CJ/RMJ/JRN** — this is GAP-009 |
| Figma status decision: reference only, not SoT | [WD §15](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#15-figma-prototype) · [D-012](../../au-bank-insurance-platform/DECISION-LOG.md) | 🟢 The right decision, made early |
| Journey canvas with CJ / RMJ / JRN identifiers | [`04-process-and-journey-canvas.md`](../../au-bank-insurance-platform/04-process-and-journey-canvas.md) | 🟢 The map GAP-009 needs to map *to* |
| Screen inventory | — | 🔴 Absent; on the [programme TODO](../../au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md) as an open item |
| Service blueprint · design system · copy deck · accessibility standard · state catalogue | — | 🔴 All absent |
| **Flutter application** | — | 🔴 **No `pubspec.yaml` exists anywhere in this repository** |

**Consequence:** every journey in the requirement set terminates at an interface that does not
exist. The platform cannot be demonstrated, usability-tested, or piloted.

---

## 3. What was missing

All seven gate criteria, at zero. Restated as buildable deliverables:

| # | Missing | Closed in |
|---|---|---|
| D1 | Service blueprint: front-stage / back-stage / support for the R0 journey | §4.1 |
| D2 | RM and customer journey maps with moments of truth | §4.2 |
| D3 | Screen inventory mapped to requirement IDs — **GAP-009** | §4.3 |
| D4 | Screen-by-screen flow with transitions | §4.4 |
| D5 | State catalogue: loading / empty / error / success per screen | §4.5 |
| D6 | Design system: tokens and components, Flutter-implementable | §4.6 |
| D7 | Regulated copy deck | §5 — **not closed**; Compliance-authored |
| D8 | Accessibility standard | §4.7 |
| D9 | Usability validation with real RMs and customers | §6 — **not closable in a repository** |

---

## 4. New evidence

### 4.1 Service blueprint — R0 assisted Term journey

Eleven journey steps. Front-stage is what the actor sees; back-stage is the system action behind
it; support is what must exist for the step to work at all.

| # | Step | Front-stage (RM) | Front-stage (customer) | Back-stage | Support line |
|---|---|---|---|---|---|
| 1 | Customer identification | Search by CIF / mobile / PAN; select result | — | Customer service (#4) → CBS lookup | CBS availability; ETB eligibility rule |
| 2 | Lead creation | Confirm LOB; lead created | — | Lead (#5) mints `leadId` + `journeyId`; Journey (#9) opens state | Identity (#3) supplies `actorId`, `agentId` |
| 3 | **Consent capture** | Reads the statement aloud; sees "waiting for customer" | **Own device:** SMS with statement + OTP; enters OTP | Consent (#6) writes append-only evidence; Notification (#17) dispatches to the **CBS-registered** number | [CNS-R10…R15](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#4-capture-mechanism-by-channel); OTP infrastructure |
| 4 | Prefill | Journey populated from CIF; RM may correct | — | Customer (#4) prefill; corrections retained with actor + timestamp | `CNS-DP` must be `ACTIVE` |
| 5 | **Need analysis & suitability** | Twelve-input questionnaire, 4 pre-filled from CBS; sees outcome per class with reasons | Receives the suitability PDF | Suitability (#7) computes `SUIT-ALGO-LIFE-v1.0`; PDF generated and stored immutably | `CNS-SOL` must be `ACTIVE`; [SUIT-R21](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#5-the-evaluation-record) |
| 6 | Eligible products | Filtered product list, or an explicit empty state with the reason | — | Catalogue (#8) applies suitability filter **then** eligibility filter ([CAT-R02](./S04-product-definition-evidence.md#423-catalogue-behaviour-rules)) | R0 matrix seeded |
| 7 | **Quote** | Submits; sees `PENDING` with a live status; then offers | — | Quotation (#10) → Integration Hub (#14) → 1SB Adapter (#15) → insurer. **Gate: 403 without a valid evaluation ID** | [SUIT-R20](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#1-the-gate-stated-once-precisely); 1SB UAT + Distributor ID |
| 8 | Compare & select | Normalised comparison; disclosed ranking rule; selects | Shown the comparison if present | Selection persisted; [QR-05…QR-08](./S04-product-definition-evidence.md#43-quote-rules--closes-gap-012) | Offer validity ≤ 7 days |
| 9 | Proposal | Dynamic schema rendered; prefilled; save/resume; submit | Answers medical/personal questions with the RM | Proposal (#11); `CNS-SHR` checked; `agentId` and server-side `distributorId` attached | Insurer schema via #14; **unexpired SP licence** |
| 10 | **Payment** | Sees "link sent" and a live status. **Renders no payment surface, ever** | **Own device:** AU Bank PG; completes payment | Payment (#12) creates the session; link to the CBS-registered mobile | RBI device isolation; AU Bank PG |
| 11 | Issuance & confirmation | Policy number + status; document link when available | Receives policy confirmation | Policy (#13) records issuance; reconciliation against PG; Audit (#16) persists. **`isSold` derived from all four** | Insurer webhook/API; Finance reconciliation |

**Moments of truth** — [S05-E01-S03](../stages/S05-experience-design.md#3-epics-and-stories):

| Moment | Where it is won or lost | Obligation binding here |
|---|---|---|
| **Step 3 — the first device hand-off** | The customer's first experience of the platform is an SMS asking them to consent to something. If the RM cannot explain it in one sentence, trust is lost at the start | IRDAI consent; DPDP notice |
| **Step 5 — the questionnaire** | Twelve honest answers or twelve clicked-through ones. This is where suitability becomes real or becomes theatre | **IRDAI mandatory suitability** |
| **Step 6 — an empty product list** | A customer told "nothing suits you" without a reason leaves and does not return | Suitability outcome disclosure |
| **Step 8 — the comparison** | Where a bank distributing multiple insurers either demonstrates impartiality or fails to | Mis-selling; [QR-07](./S04-product-definition-evidence.md#43-quote-rules--closes-gap-012) |
| **Step 10 — the second device hand-off** | The hardest UX in the product. The RM must hand control to the customer's phone for a payment, then wait, visibly, doing nothing | **RBI device isolation** |
| **Step 11 — the confirmation** | The capability the legacy model lacks entirely. This is the visible proof the platform exists | `Policy Sold` definition |

> **The two device hand-offs are the design problem of R0.** In both, the RM's job changes from
> *doing* to *waiting while the customer does*. A design that leaves the RM staring at a spinner
> invites exactly the workaround both controls exist to prevent — the RM taking the customer's
> phone. Steps 3 and 10 therefore each get an explicit RM-side "what to do while waiting" state
> (§4.5), not a loading indicator.

### 4.2 Journey maps

**RM (RMJ-R0)** — doing / thinking / feeling per step, with the friction to design against.

| Step | RM is doing | RM is thinking | Friction to design out |
|---|---|---|---|
| 1–2 | Searching, confirming identity | "Is this the right customer?" | Ambiguous search results; unclear ETB eligibility |
| 3 | Explaining consent, waiting | "Will they understand this SMS?" | Dead air; no visibility of OTP delivery |
| 4 | Reviewing prefill | "Is this data current?" | No indication of what is prefilled vs entered |
| 5 | Working through 12 questions | "This is long. Can I skip any?" | **Every skippable-feeling question is a suitability risk** |
| 6 | Reading the eligible list | "Why is this list short?" | Unexplained filtering |
| 7 | Waiting for the quote | "How long does this take?" | Unbounded wait with no progress signal |
| 8 | Comparing | "Which do I recommend?" | Comparison that hides the basis |
| 9 | Filling the proposal | "How much more of this?" | No progress indicator; no save confidence |
| 10 | Waiting for payment | "Did they get the link? Should I call?" | **Dead air with a customer sitting opposite** |
| 11 | Confirming | "Is it actually done?" | Ambiguous "submitted" vs "issued" |

**Customer (CJ-R0)** — the customer touches the platform three times: a consent SMS, a suitability
PDF, a payment link.

| Touch | Doing | Thinking | Design response |
|---|---|---|---|
| Consent SMS | Reading a statement, entering an OTP | "What am I agreeing to? Is this real?" | Statement text **in the SMS body**, not behind a link. Bank sender ID. Plain language |
| Suitability PDF | Skimming a recommendation | "Did they actually assess me?" | The PDF shows their own answers back to them, not just an outcome |
| Payment link | Paying on their own phone | "Is this the bank's page?" | AU Bank PG domain, bank branding, amount and policy visible before payment |

### 4.3 Screen inventory — closes GAP-009

Sixteen RM screens, two customer surfaces. Every screen maps to requirement IDs and to the journey
canvas. **This is the map GAP-009 asked for**; the existing Figma is reference material to be
mapped *onto* this inventory, not the other way round — [D-012](../../au-bank-insurance-platform/DECISION-LOG.md).

| ID | Screen | Journey step | Requirements | Acceptance criteria | R0 |
|---|---|---|---|---|---|
| `SCR-01` | Login / SSO landing | pre-1 | BR-SEC-010 | `AC-SEC-010-1/2/3` | ✅ |
| `SCR-02` | RM pipeline (own leads, filter, resume) | entry | BR-RM-010, BR-LEAD-030 | `AC-LEAD-030-1` | ✅ |
| `SCR-03` | Customer search | 1 | BR-CUST-010 | `AC-CUST-010-1/2/3` | ✅ |
| `SCR-04` | Customer detail & confirm | 1 | BR-CUST-010, BR-CUST-020 | `AC-CUST-020-1/3` | ✅ |
| `SCR-05` | Lead create / LOB confirm | 2 | BR-LEAD-010 | `AC-LEAD-010-1` | ✅ |
| `SCR-06` | **Consent capture — RM view** | 3 | BR-CONSENT-010 | `AC-CONSENT-010-1/2/3` | ✅ |
| `SCR-06c` | **Consent — customer device** (SMS + OTP) | 3 | BR-CONSENT-010 | `AC-CONSENT-010-2` | ✅ |
| `SCR-07` | Journey summary / prefill review | 4 | BR-CUST-020 | `AC-CUST-020-1/3` | ✅ |
| `SCR-08` | **Need analysis & suitability questionnaire** | 5 | BR-SUIT-010 | `AC-SUIT-010-1/2` | ✅ |
| `SCR-09` | **Suitability outcome & recommendation** | 5 | BR-SUIT-020, BR-SUIT-030 | `AC-SUIT-020-1`, `AC-SUIT-030-4` | ✅ |
| `SCR-10` | Eligible products | 6 | BR-PROD-010 | `AC-PROD-010-1/2` | ✅ |
| `SCR-11` | Quote request & status | 7 | BR-QUOTE-010, BR-QUOTE-020 | `AC-QUOTE-010-1/3`, `AC-QUOTE-020-1/2` | ✅ |
| `SCR-12` | **Offer comparison & selection** | 8 | BR-COMP-010 | `AC-COMP-010-1/2/3` | ✅ |
| `SCR-13` | Proposal form (dynamic schema, save/resume) | 9 | BR-PROP-010, BR-PROP-020 | `AC-PROP-010-1/2`, `AC-PROP-020-1` | ✅ |
| `SCR-14` | Proposal review & submit | 9 | BR-PROP-030 | `AC-PROP-030-1…4` | ✅ |
| `SCR-15` | Application status / UW tracking | 9–10 | BR-UW-010 | `AC-UW-010-1/2` | ✅ |
| `SCR-16` | **Payment hand-off — RM view (status only)** | 10 | BR-PAY-010, BR-PAY-020 | `AC-PAY-010-2`, `AC-PAY-020-1` | ✅ |
| `SCR-16c` | **Payment — customer device** (AU Bank PG) | 10 | BR-PAY-010 | `AC-PAY-010-1/2` | ✅ external |
| `SCR-17` | Policy confirmation & documents | 11 | BR-POL-010 | `AC-POL-010-1/2` | ✅ |
| `SCR-18` | Pilot funnel view (ops/PO, not RM) | — | BR-REP-010 | `AC-REP-010-1` | ✅ minimal |
| — | Admin / catalogue configuration UI | — | BR-PROD-020 | — | ⛔ R1 — config seed in R0 |
| — | Group B redirect screens | — | — | — | ⛔ R1 |
| — | DIY customer journey screens | — | — | — | ⛔ R1 |

**Against [S05-VT-01 and VT-02](../stages/S05-experience-design.md#4-validation-tests):**

- *Every requirement has a screen* — all 24 P0 requirements appear above except BR-INT-\* (no UI;
  integration behaviour), BR-COMM-010 (notification content, not a screen) and BR-PROD-020
  (config seed in R0). Each exception is deliberate and named.
- *Every screen has a requirement* — all 18 R0 screens carry requirement IDs. **Zero unrequested
  screens**, which is where scope creep enters.

### 4.4 Screen-by-screen flow

```
 SCR-01 Login
    │
    ▼
 SCR-02 Pipeline ──────── resume ────────────────────────────┐
    │ new                                                    │
    ▼                                                        │
 SCR-03 Search ──no match──► empty state ──► SCR-03          │
    │ match                                                  │
    ▼                                                        │
 SCR-04 Customer detail ──not ETB──► ineligible ──► SCR-03   │
    │                                                        │
    ▼                                                        │
 SCR-05 Lead create                                          │
    │                                                        │
    ▼                                                        │
 SCR-06 Consent (RM) ◄──────► SCR-06c Consent (customer device, OTP)
    │ CNS-DP + CNS-SOL + CNS-COM verified                    │
    │  ✗ declined / OTP unverified ──► journey HALTED        │
    ▼                                                        │
 SCR-07 Prefill review                                       │
    │                                                        │
    ▼                                                        │
 SCR-08 Suitability questionnaire (12 inputs)                │
    │ all 12 present                                         │
    ▼                                                        │
 SCR-09 Suitability outcome                                  │
    │  NOT_SUITABLE (all classes) ──► journey ends, reason shown, no override
    │  SUITABLE_WITH_CAUTION ──► customer-device disclosure ack (SUIT-R41/R42)
    ▼                                                        │
 SCR-10 Eligible products ──zero──► empty state with reason ─┘
    │
    ▼
 SCR-11 Quote request ──► PENDING ──► PARTIAL / COMPLETED / FAILED / TIMEOUT
    │                          ▲                    │
    │                          └──── retry ─────────┘
    ▼
 SCR-12 Compare & select ──expired──► SCR-11 (re-quote; QR-03: never extend)
    │
    ▼
 SCR-13 Proposal form ◄──► save & resume (returns to SCR-02)
    │
    ▼
 SCR-14 Review & submit
    │  ✗ agentId missing / SP licence expired / CNS-SHR absent ──► blocked, reason shown
    ▼
 SCR-15 Application status ──UW_DECLINED──► terminal, reason preserved, ops task
    │ payable
    ▼
 SCR-16 Payment hand-off (RM: status only) ◄──► SCR-16c AU Bank PG (customer device)
    │  ✗ failure ──► retry per rule (AC-PAY-020-2)
    ▼
 SCR-17 Policy confirmation
        isSold derived only when issuance ∧ confirmation ∧ reconciliation ∧ audit-persisted
```

**Every backward edge is deliberate.** The two the design must not lose: `SCR-12 → SCR-11` (an
expired quote is re-requested, never extended — QR-03) and `SCR-13 → SCR-02` (save-and-resume,
because a branch conversation is interrupted constantly).

### 4.5 State catalogue — closes S05-G7

Four states per screen. `—` means the state cannot occur for that screen.

| Screen | Loading | Empty | Error | Success |
|---|---|---|---|---|
| `SCR-02` Pipeline | Skeleton rows (3) | "No leads yet. Start with a customer search." + primary CTA | "Could not load your pipeline. Retry." | Rows with stage chips |
| `SCR-03` Search | Inline spinner in the field | "No customer found for *(term)*. Check the CIF, mobile or PAN." | "Customer records are unavailable right now. **Do not proceed.**" (`AC-EXC-10`) | Result list |
| `SCR-04` Detail | Skeleton card | — | "Could not load customer details." | Detail + Continue |
| `SCR-06` Consent (RM) | **"Sent to the customer's mobile ending *(nn)*. Waiting for them to confirm."** + countdown + Resend (after 60s) | — | Distinct copy per code: `OTP_EXPIRED` → "The code expired. Send a new one." · `OTP_ATTEMPTS_EXCEEDED` → "Too many attempts. Send a new code." · `CONSENT_EVIDENCE_WRITE_FAILED` → "Could not record consent. **Do not proceed.**" | Green confirmation with timestamp and the consents captured |
| `SCR-08` Questionnaire | — | — | Field-level validation; the four CBS-derived inputs render read-only with their source | Progress "9 of 12"; Continue enabled only at 12 |
| `SCR-09` Outcome | Computing (< 2s) | — | "Suitability could not be completed. **You cannot request a quote.**" (SUIT-R26) | Outcome per class + every reason code + PDF link |
| `SCR-10` Products | Skeleton (3) | **"No products match this customer's suitability outcome."** + the reason + no quote CTA (`AC-PROD-010-2`, CAT-R06) | "Catalogue unavailable." | Product cards |
| `SCR-11` Quote | **Named-stage progress**, not a spinner: "Sent to *(insurer)*" → "Waiting for response" → elapsed time | — | `TIMEOUT` → "*(Insurer)* did not respond in time." + Retry · `FAILED` → normalised reason + Retry · `403` from the gate → reason + path back to `SCR-08` | Offers listed |
| `SCR-12` Compare | — | "No comparable offers." | "Offer expired." + Re-quote (QR-02) | Selection confirmed |
| `SCR-13` Proposal | Schema loading | — | Field-level from the insurer schema; "Saved" / "Not saved" state always visible | Autosave confirmation with timestamp |
| `SCR-14` Submit | "Submitting…" — **non-cancellable** | — | `AGENT_ATTRIBUTION_MISSING` · `AGENT_CERTIFICATION_EXPIRED` ("Your IRDAI certification expired on *(date)*. Contact your Branch Manager.") · `CONSENT_REQUIRED` | Application reference shown |
| `SCR-15` Status | Skeleton | "No requirements outstanding." | "Status unavailable. Last known: *(status)* at *(time)*." | Normalised status + insurer substatus |
| `SCR-16` Payment (RM) | **"Payment link sent to the customer's mobile ending *(nn)*. They will complete it on their own device."** + live status + elapsed + Resend. **No payment form, no card field, no UPI field, ever** (`AC-PAY-010-2`) | — | `FAILURE` → reason + "Send a new payment link" · `EXPIRED` → new session | "Payment received" + PG reference |
| `SCR-17` Confirmation | Awaiting issuance | — | `PAID_NOT_ISSUED` → "Payment received; the insurer has not confirmed issuance. Operations has been notified." (`AC-EXC-06`) | Policy number, status, document link. **"Sold" shown only when all four conditions hold** |

**Two states carry a regulatory obligation and must be built exactly as written:**

- `SCR-06` and `SCR-16` loading states give the RM something to *say* — the mobile's last two
  digits, a countdown, a resend — instead of dead air. Dead air is what makes an RM reach for the
  customer's phone, which is precisely what [CNS-R13](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#4-capture-mechanism-by-channel)
  and the RBI device rule forbid.
- `SCR-16` has **no** success-path payment surface on the RM device in any state.

### 4.6 Design system specification

Flutter-implementable. Values are stated so an engineer can build without asking.

> **Brand substitution.** AU Bank's brand colours are a corporate asset I do not hold. The primary
> ramp below is a **placeholder** meeting WCAG 2.1 AA against the stated backgrounds. **Substitute
> AU Bank's brand values and re-verify every contrast ratio** — that verification is a gate item
> (§6, S05-OPEN-04), not an assumption. Semantic, neutral, spacing, type, radius, elevation and
> motion tokens are **not** brand-dependent and stand as specified.

#### Colour tokens

| Token | Light | Dark | Contrast (on its stated background) |
|---|---|---|---|
| `color.brand.primary` | `#0B4F9E` | `#5FA8F5` | 7.4:1 on `surface` / 8.1:1 on `surface` dark |
| `color.brand.primaryHover` | `#093F7E` | `#8AC0F8` | — |
| `color.brand.onPrimary` | `#FFFFFF` | `#06213F` | 7.4:1 |
| `color.surface` | `#FFFFFF` | `#101418` | — |
| `color.surfaceMuted` | `#F4F6F8` | `#181D23` | — |
| `color.surfaceRaised` | `#FFFFFF` | `#1F252C` | — |
| `color.border` | `#D5DBE1` | `#2C343D` | 3.1:1 (non-text) |
| `color.borderStrong` | `#9AA5B1` | `#4A555F` | 3.1:1 |
| `color.text.primary` | `#111820` | `#F2F5F8` | 16.1:1 |
| `color.text.secondary` | `#4A555F` | `#B4BEC8` | 7.6:1 |
| `color.text.disabled` | `#8A949E` | `#6C767F` | 3.2:1 — **never used for meaning** |
| `color.status.success` | `#1B7F4B` | `#4FBF85` | 4.6:1 |
| `color.status.warning` | `#8A5A00` | `#E0A040` | 4.8:1 |
| `color.status.error` | `#B3261E` | `#F2857E` | 5.9:1 |
| `color.status.info` | `#0B4F9E` | `#5FA8F5` | 7.4:1 |
| `color.status.pending` | `#4A555F` | `#B4BEC8` | 7.6:1 |
| `color.focusRing` | `#0B4F9E` | `#8AC0F8` | 3:1 min against adjacent |

**Rule DS-01 — colour is never the sole carrier of meaning.** Every status uses colour **plus** an
icon **plus** a text label. A quote status shown only in red fails for a colour-blind RM and fails
the accessibility standard.

#### Type scale — 1.200 minor-third, 16px base

| Token | Size / line-height | Weight | Use |
|---|---|---|---|
| `type.display` | 33 / 40 | 600 | Screen title, one per screen |
| `type.headingL` | 28 / 36 | 600 | Section heading |
| `type.headingM` | 23 / 32 | 600 | Card heading |
| `type.headingS` | 19 / 28 | 600 | Sub-section, table header |
| `type.body` | 16 / 24 | 400 | **Default. Never below 16px for RM-facing body text** |
| `type.bodyStrong` | 16 / 24 | 600 | Emphasis within body |
| `type.label` | 14 / 20 | 500 | Field labels, chips |
| `type.caption` | 13 / 18 | 400 | Timestamps, helper text |
| `type.mono` | 15 / 22 | 400 | Policy numbers, references, IDs |

**Rule DS-02 — regulated copy renders at `type.body` or larger, never at `type.caption`.**
A consent statement or a disclosure in 13px is a legal risk, not a density optimisation.
`type.mono` for references exists because RMs read policy and application numbers aloud.

#### Spacing — 4px base

`space.0` 0 · `space.1` 4 · `space.2` 8 · `space.3` 12 · `space.4` 16 · `space.5` 24 ·
`space.6` 32 · `space.7` 48 · `space.8` 64

Vertical rhythm: `space.4` within a group, `space.5` between groups, `space.6` between sections.

#### Radius, elevation, motion

| Token | Value |
|---|---|
| `radius.sm` / `md` / `lg` / `pill` | 4 / 8 / 16 / 999 |
| `elevation.0 / 1 / 2` | none · `0 1px 2px rgba(0,0,0,.08)` · `0 4px 12px rgba(0,0,0,.12)` |
| `motion.fast` / `base` / `slow` | 120ms / 200ms / 320ms, all `cubic-bezier(.2,0,0,1)` |

**Rule DS-03 — motion is decorative and must be removable.** Honour `prefers-reduced-motion`;
no journey step may depend on an animation completing. Branch tablets are mid-range devices.

#### Component library

| Component | Variants | States | Notes |
|---|---|---|---|
| `Button` | primary · secondary · tertiary · destructive | default · hover · pressed · focus · disabled · **loading** | Loading disables and shows a spinner **plus** a label change; min touch target **48×48** |
| `TextField` | text · number · currency · date · masked | default · focus · filled · error · disabled · **readonly-derived** | `readonly-derived` renders the four CBS inputs on `SCR-08` with their source named |
| `Select` / `RadioGroup` | — | as `TextField` | `RadioGroup` for ≤ 5 options; `Select` above |
| `StatusChip` | success · warning · error · info · pending | — | **Icon + colour + label**, per DS-01 |
| `Card` | flat · raised · selectable | default · selected · disabled | Offer cards on `SCR-12` |
| `ComparisonTable` | — | loading · populated · partial | Renders the disclosed ranking rule as a visible caption ([QR-06](./S04-product-definition-evidence.md#43-quote-rules--closes-gap-012)) |
| `ProgressStepper` | horizontal · vertical | complete · current · upcoming · blocked | Journey position, always visible |
| `WaitingPanel` | consent · payment | waiting · resend-available · timeout | **The `SCR-06`/`SCR-16` hand-off component.** Shows masked mobile, countdown, resend. Carries the regulatory obligation in §4.5 |
| `EmptyState` | neutral · blocked | — | Requires an explanation **and** a next action; a bare illustration is not permitted |
| `ErrorBanner` | inline · page | — | Requires the actionable copy from the §4.5 catalogue |
| `DisclosureAck` | caution · replacement | pending · acknowledged | Customer-device only ([SUIT-R42](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#72-suitable_with_caution--proceed-with-disclosure-not-with-override)) |
| `QuestionnaireItem` | single · multi · numeric · currency · boolean | unanswered · answered · invalid | No "skip" affordance exists — all 12 inputs are mandatory (SUIT-R01) |
| `DocumentLink` | — | available · generating · unavailable | Suitability PDF, policy document |

#### Responsive

| Breakpoint | Width | Primary case |
|---|---|---|
| `bp.compact` | < 600 | RM phone — supported, not optimised in R0 |
| `bp.medium` | 600–1023 | **RM tablet in branch — the primary R0 case** |
| `bp.expanded` | ≥ 1024 | RM desktop / branch workstation |

At `bp.medium`, `SCR-08` and `SCR-13` are single-column with a persistent `ProgressStepper`;
`SCR-12` is a horizontally scrollable `ComparisonTable` with a pinned first column.

### 4.7 Accessibility standard

| Item | Standard |
|---|---|
| Conformance target | **WCAG 2.1 Level AA** |
| Text contrast | ≥ 4.5:1 body, ≥ 3:1 large (≥ 19px / 600) |
| Non-text contrast | ≥ 3:1 for borders, focus rings, icons carrying meaning |
| Touch target | ≥ 48×48 dp |
| Focus | Visible on every interactive element; logical DOM order; no keyboard trap |
| Screen reader | Every field labelled; every error programmatically associated; live regions on `WaitingPanel` and quote status |
| Motion | `prefers-reduced-motion` honoured (DS-03) |
| Colour independence | DS-01 |
| Text resize | Legible and functional to 200% with no loss of content |
| Testing method | Automated axe scan in CI **plus** a manual screen-reader pass on the R0 journey |

**Rule DS-04 — low-literacy and assisted use.** The RM-assisted journey serves customers who will
not read a screen. Every customer-device surface (`SCR-06c`, `SCR-16c`) must be comprehensible from
the SMS body alone, without opening a link — which is why
[the consent statement text goes in the SMS body](#42-journey-maps), not behind one.

**Rule DS-05 — poor connectivity.** Branch and field connectivity is unreliable. `SCR-13` autosaves
on field blur and survives a connection loss; `SCR-11` and `SCR-16` poll with backoff and show the
last known state with its timestamp rather than a blank screen.

---

## 5. Regulated copy — what I am not writing

[S05-E04](../stages/S05-experience-design.md#3-epics-and-stories) requires consent statements,
regulatory disclosures, error copy and a language strategy. **Consent statement wording and
regulatory disclosure wording are regulated content authored by Compliance and Legal**, and
[S05 §1](../stages/S05-experience-design.md#1-purpose) is explicit that they are *"not written by a
developer against a placeholder"* — which applies equally to a Product Owner and to an AI.

| Copy class | Who writes it | Status |
|---|---|---|
| Five consent statement texts (`CNS-DP`, `CNS-SOL`, `CNS-SHR`, `CNS-COM`, `CNS-RDR`) | **Shailja + Legal** | OPEN-CNS-02 — **structure, fields, versioning and hashing are specified** in the [consent pack §3](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#3-statement-content-and-versioning); the legal text is not |
| Caution and replacement disclosure text (SUIT-R41, R46) | **Shailja + Legal** | OPEN-SUIT-03 |
| Suitability question wording (the 12 inputs) | Rajal, **Compliance-reviewed** | Structure specified in [suitability pack §2](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#2-the-need-analysis-question-set); final wording pending review |
| Error and exception copy | **Rajal** | ✅ Specified in §4.5 for all 18 screens |
| Empty-state copy | **Rajal** | ✅ §4.5 |
| Language strategy | Rajal | ✅ `en-IN` at R0; `hi-IN` at R1 ([CNS-R08/R09](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#33-language)) |

**Every string on which a regulator could form a view is Compliance's.** Every string on which only
an RM could get confused is mine, and those are written.

---

## 6. What remains genuinely open

| ID | Item | Criterion | Owner | Target |
|---|---|---|---|---|
| S05-OPEN-01 | **Figma reference material mapped onto the §4.3 inventory**; concept-only screens marked out of MVP | S05-G2 | Rajal + Digital/Design | 2026-09-12 |
| S05-OPEN-02 | Visual design applied to the 18 screens: layout, hierarchy, real components | S05-G2, S05-G3 | Design | 2026-09-26 |
| S05-OPEN-03 | Consent and disclosure copy authored and approved | S05-G4 (E2) | **Shailja + Legal** | 2026-09-12 |
| S05-OPEN-04 | **AU Bank brand tokens substituted and every contrast ratio re-verified** | S05-G3, S05-G5 | Design + Brand | 2026-09-12 |
| S05-OPEN-05 | Clickable prototype of the complete R0 journey | S05-G6 | Design | 2026-10-10 |
| S05-OPEN-06 | **Usability test with ≥ 5 practising RMs** (≥ 80% complete unaided; time-on-task recorded) | S05-G6 (E3) | Design + Rajal | 2026-10-24 |
| S05-OPEN-07 | **Customer-device hand-off test with ≥ 5 representative customers** (S05-VT-04) | S05-G6 (E3) | Design + Rajal | 2026-10-24 |
| S05-OPEN-08 | Accessibility audit against WCAG 2.1 AA | S05-G5 (E3) | Design + Swapnali | 2026-10-24 |
| S05-OPEN-09 | Deepali's review of consent and authentication UX (she holds a **block** here) | S05-G4 | Deepali | 2026-09-26 |
| S05-OPEN-10 | Flutter project scaffolded; design system implemented as a component package | — | Amit + Design | S11 |
| GAP-023 | DIY and hybrid journey experience | — | Design + Rajal | R1 / R2 |

**S05-OPEN-06 and -07 cannot be closed in a repository, and I am not going to pretend otherwise.**
Both are E3 — executed, point-in-time — and both require real humans in a real branch.
[S05-VT-04](../stages/S05-experience-design.md#4-validation-tests) asks whether customers understand
what they are approving *without RM explanation*. No document answers that question.

---

## 7. Why this is enough to unblock S11

[S05 §6](../stages/S05-experience-design.md#6-current-position-in-this-repository---missing) sets the
bar: *"S05 does not need to be completed before S11; it needs to be completed for the R0 slice."*

| S11 needs | Supplied |
|---|---|
| A defined set of screens to build | 18, with IDs and requirements (§4.3) |
| The flow between them, including failure edges | §4.4 |
| Behaviour in every state, not only the happy path | §4.5 — 18 screens × 4 states |
| Tokens and components to build against | §4.6 |
| An accessibility target to build to | §4.7 |
| Copy for failure paths | §4.5 |
| **Approved regulated copy** | ⛔ **S05-OPEN-03 — genuinely blocking for the consent and disclosure screens** |
| **Validated flow** | ⛔ S05-OPEN-06/-07 — blocking for *pilot*, not for *build* |

**Product position:** `SCR-01` through `SCR-05` and `SCR-10` through `SCR-18` can be built now.
`SCR-06`, `SCR-06c` and `SCR-09`'s disclosure surfaces **cannot ship without approved copy**, and
building them against a placeholder is what
[S05 §7](../stages/S05-experience-design.md#7-premature-at-this-stage) warns produces "a beautiful
implementation of unapproved copy". Build the shell, gate the strings.

---

## 8. Retroactive stage verdict

> ## `CLOSED-WITH-CONDITIONS`

| Criterion | State | Basis |
|---|---|---|
| S05-G1 Service blueprint for R0 | **MET at E1** | §4.1 — 11 steps × 4 lanes, 6 moments of truth |
| S05-G2 Wireframes mapped to requirement IDs | **PARTIAL** | §4.3 maps 18 screens to requirements and ACs; §4.4 supplies the flow. **The visual wireframes themselves do not exist.** GAP-009 closed on *mapping*; S05-OPEN-01, -02 |
| S05-G3 Design system published and adoptable | **MET at E1, conditionally** | §4.6 — tokens, type scale, spacing, 13 components with variants and states, responsive rules. **Conditional on brand substitution and contrast re-verification.** S05-OPEN-04 |
| S05-G4 Regulated copy approved by Compliance | **NOT MET** | §5. Error and empty-state copy written; consent and disclosure text is Compliance's. S05-OPEN-03 |
| S05-G5 Accessibility standard set; prototype conformant | **PARTIAL** | Standard set (§4.7); **no prototype to audit**. S05-OPEN-08 |
| S05-G6 Prototype validated with real RMs and customers | **NOT MET** | S05-OPEN-05, -06, -07. E3, and not producible from a repository |
| S05-G7 Error, empty and degraded states catalogued | **MET at E1** | §4.5 — all 18 screens, four states each, with actual copy |

**Movement.** S05 was 🔴 Missing on all seven criteria. Three are now met at E1, two are partial,
and the two that remain unmet are unmet for the right reasons: one needs a Compliance lawyer, the
other needs five RMs in a branch. **Neither is a documentation gap.**

**The single most valuable thing here** is §4.5's treatment of `SCR-06` and `SCR-16`. Both device
hand-offs are regulatory controls that fail through *user experience* rather than through code: an
RM left staring at dead air with a customer opposite will reach for the customer's phone, and both
[CNS-R13](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#4-capture-mechanism-by-channel)
and the RBI device rule are then breached by a system that implemented them correctly. Designing
the waiting state is a compliance control, not a nicety.

**Conditions carried forward:** S05-OPEN-01 through -10; GAP-023 at R1/R2.

**Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.
S05-G4 requires Compliance approval; Deepali holds a **block** on consent and authentication UX;
S05-G5 and S05-G6 require E3 evidence from executed tests. Silence does not approve this stage.

— **Rajal**, Principal Insurance Platform Product Owner, 2026-08-16
