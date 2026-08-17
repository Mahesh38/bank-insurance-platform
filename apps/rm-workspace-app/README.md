# RM Workspace — AU Bank Insurance Distribution Platform (R0)

**Workstream:** WS-3 — AU Bank Insurance Distribution Platform
**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Stage:** S11 vertical slice — **interface half only**
**Flutter:** 3.24.5 · Dart 3.5.4 · target `web`

---

## What this is

The RM-facing application for the R0 assisted Term journey, built to the screen
inventory and design system in
[`S05-experience-evidence.md`](../../docs/application-lifecycle-bible/evidence/S05-experience-evidence.md),
the state machines and invariants in
[`01-domain-model-and-invariants.md`](../../docs/platform/ws3-platform/01-domain-model-and-invariants.md),
and the acceptance criteria in
[`S03-requirements-evidence.md`](../../docs/application-lifecycle-bible/evidence/S03-requirements-evidence.md).

Before this existed, `find . -name pubspec.yaml` returned nothing and
[position assessment §3.3](../../docs/application-lifecycle-bible/01-POSITION-ASSESSMENT.md)
recorded that *every journey in the requirement set terminates at an interface
that does not exist*. This closes that specific gap and no other.

## What this is **not**

> **This is not S11 evidence, and nothing in this directory should be cited as
> proof that the vertical slice works.**

The application runs entirely against an in-memory fake. Of the bounded contexts
the R0 journey needs — Lead, Customer, Consent, Suitability, Catalogue,
Quotation, Proposal, Payment, Policy, Audit — **none exists as a service**. The
1SB adapter (context #15) exists in `services/` and is not wired to this app.

S11 is passed when a real RM sells a real policy through real services with
evidence. That is a separate increment and separate evidence.

---

## Running it

```bash
export PATH=/opt/flutter-sdk/flutter/bin:$PATH
cd apps/rm-workspace-app

flutter pub get
flutter test          # 105 tests
flutter build web     # output in build/web
flutter run -d chrome # or serve build/web
```

## What a user can actually do today

Sign in, then walk the complete R0 journey end to end:

| Step | Screen | Real behaviour | Faked |
|---|---|---|---|
| Sign in | SCR-01 | Session gate — every other route refuses without it | Bank SSO / AD federation |
| Pipeline | SCR-02 | Empty and populated states; resume at last incomplete step | — |
| Customer search | SCR-03/04 | Search, no-match empty state, ETB eligibility refusal, masked mobile | CBS lookup — 3 sample customers |
| Lead | SCR-05 | Lead created, journey opened at `needAnalysis` | — |
| Consent | SCR-06 | Three consents dispatched, **no OTP field on the RM device**, waiting panel, append-only evidence with statement version + hash + OTP txn + agent id | SMS gateway; **statement wording is placeholder** |
| Prefill | SCR-07 | Prefill gated on `CNS-DP`, source and timestamp recorded | CBS |
| Suitability | SCR-08/09 | **Full `SUIT-ALGO-LIFE-v1.0`** — 12 inputs, income bands, age factors, human-life-value cover, per-class outcomes with every reason code, 30-day validity | — the algorithm is real and deterministic |
| Products | SCR-10 | Suitability filter then eligibility filter (CAT-R02), empty state with reason | Insurer panel is **sample data** (S04-OPEN-01) |
| Quote | SCR-11 | Idempotent request carrying the evaluation id; partial/failed states modelled | Pricing is a deterministic sample formula, not a rate table |
| Compare | SCR-12 | Ranking ascending by premium with the rule **disclosed on screen**; commission is not an input and no field exists for it | — |
| Proposal | SCR-13/14 | Sharing consent captured separately (insurer named); attribution and evidence trail shown | Insurer form schema |
| Underwriting | SCR-15 | Status progression, normalised bank status | Insurer decisioning |
| Payment | SCR-16 | Link issued to the customer's registered mobile, waiting panel, reconciliation before issuance. **No input surface at all** | PG; **scannable QR rendering is a later increment** |
| Policy | SCR-17 | All four "sold" conditions rendered individually; `isSold` derived, never set | Insurer issuance |
| Funnel | SCR-18 | Stage counts for the session; control KPIs 03/04 | Real funnel reads journey events |

Two "simulate" buttons stand in for the customer's own device — consent
confirmation and payment. They exist because there is no SMS gateway and no PG,
and they represent *the platform reporting that the customer acted*, never the
RM acting on the customer's behalf.

---

## The three non-negotiables, and how each is enforced

Each is enforced at more than one layer, because a control with a single
enforcement point is a control one refactor away from absent.

### 1. No quote without suitability — SUIT-R20 / INV-QUO-01 (control C1)

| Layer | Mechanism |
|---|---|
| **Type system** | `QuoteRepository.requestQuote` requires a `SuitabilityEvaluationRef`. Its constructor is library-private, and the only mint point is `SuitabilityAssessment.evaluationRef`, which applies all four SUIT-R20 conditions. **Code holding no valid assessment has nothing to pass — it does not compile.** |
| **Router** | `JourneyGuard` runs before any screen is built. `/quote`, `/products` and `/compare` render `GuardBlockedScreen`; the quote screen is never constructed. |
| **Repository** | The token is re-validated at the point of use — expiry and customer binding, because a token minted five minutes ago may be stale. |
| **Override** | There is none. `NOT_SUITABLE` has no override control, no approval workflow and no flag (SUIT-R38, SUIT-R40). |

### 2. No proposal without unexpired consent — INV-PRP-01 (control C2)

Same shape: `ProposalRepository.submit` requires a `ConsentGrantRef`, mintable
only from a `GRANTED`, unexpired grant; the router refuses `/proposal` and
`/submit`; the repository re-validates scope and expiry at submit time.

### 3. Payment on the customer's device — INV-PAY-01 (control C4)

Enforced by **absence**. `PaymentRepository` declares no method that accepts an
instrument, `PaymentHandoff` has no field that could carry one, and the payment
screen renders zero text inputs in every state. A source scan asserts that no
payment-instrument identifier exists anywhere under `lib/`.

### 4. No PII in logs — INV-LOG-01 (control C5)

`SafeLog` is the only sanctioned sink and redacts before emitting. A test walks
the complete journey and scans every emitted record for PAN, Aadhaar, mobile,
email, DOB and URL patterns. Another asserts no `print`/`debugPrint` exists
outside the logging library.

---

## Tests

```
flutter test    # 105 tests, all passing
```

| File | Proves |
|---|---|
| `test/guards/suitability_gate_test.dart` | The hard gate, at three layers plus the absence of an override |
| `test/guards/consent_gate_test.dart` | Consent gate; OTP required; withdrawal preserves evidence; scope and expiry refusals |
| `test/guards/payment_device_isolation_test.dart` | Zero inputs in every payment state; source scan for instrument capability |
| `test/guards/structural_guarantees_test.dart` | Private constructors, guard-carrying signatures, guard-before-screen ordering, every route guarded, no third-party dependencies |
| `test/logging/pii_redaction_test.dart` | Redaction, full-journey log scan, single-sink property |
| `test/domain/suitability_algorithm_test.dart` | `SUIT-TC-REF-01` reference case, purity, bands, age limits, outcome rules |
| `test/domain/journey_transitions_test.dart` | Legal transitions, terminal stages, compensation exits, four-part "sold" |
| `test/screens/screen_render_test.dart` | Every screen in its meaningful states |
| `test/journey/end_to_end_test.dart` | The whole journey, plus audit-event completeness |

The guard tests are the point. A test asserting *"quote is unreachable without a
suitability id"* is worth more than ten rendering tests, because the programme
has already shipped a quote path without that gate once
([GAP-C](../../docs/application-lifecycle-bible/01-POSITION-ASSESSMENT.md#gap-c--the-compliance-hard-gates-that-make-this-business-legal-are-not-implemented--critical)).

---

## Architecture

```
lib/
  design/      tokens.dart, components.dart     — S05 §4.6 design system
  domain/      ids, consent, suitability, sales, journey
                                                — aggregates, state machines, capability tokens
  data/        repositories.dart                — interfaces; the guards live in these signatures
               fake_backend.dart                — in-memory implementation
  guards/      journey_guard.dart               — route refusals
  state/       journey_controller.dart          — journey projection, ChangeNotifier
  logging/     safe_log.dart                    — the only log sink
  screens/     SCR-01 … SCR-18
  app.dart                                      — guarded router
```

**No third-party runtime dependency.** Only `flutter`, `flutter_test`,
`cupertino_icons` and `flutter_lints` — asserted by test. State management is
`ChangeNotifier` + `InheritedNotifier`. This keeps the supply-chain surface an
S08 SCA scan must cover at zero, which matters for the first application in a
repository that has never had dependency scanning.

## Wiring the real backend

Every screen depends on the interfaces in `lib/data/repositories.dart` and
nothing outside `lib/data/` names a concrete class. Swapping `FakeBackend` for
an HTTP implementation against the RM Workspace BFF (context #2) is the only
change required. The guards are in the interface signatures, so they survive the
swap by construction.

---

## Known gaps

| Gap | Owner | Reference |
|---|---|---|
| **Consent statement and disclosure wording is placeholder** — must not reach pilot | Shailja + Legal | S05-OPEN-03 |
| **Brand tokens are placeholders**; contrast must be re-verified against AU Bank's palette | Design + Brand | S05-OPEN-04 |
| Insurer, product and eligibility values are sample data | Bancassurance | S04-OPEN-01 |
| Scannable QR rendering | Design + Amit | later increment |
| Visual design not applied — this is the spec built literally, not a designed UI | Design | S05-OPEN-02 |
| No usability validation with real RMs or customers | Design + Rajal | S05-OPEN-06/07 |
| Accessibility: semantics and targets follow the standard; **no audit has been run** | Design + Swapnali | S05-OPEN-08 |
| DIY and hybrid journeys | Rajal | R1 / R2 |

**Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.
Publishing this application does not close S05, does not close S11, and does not
discharge any gate criterion. It is one artefact, and the gates that consume it
remain open.
