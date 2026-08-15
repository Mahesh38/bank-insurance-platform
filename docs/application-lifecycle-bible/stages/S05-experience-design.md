# S05 — Experience Design & Service Blueprint

**AIGEM stage:** L1 — Business Design · **Owner:** Digital / Design + Rajal (Product)
**Central question:** *What does the human actually see and do?*

---

## 1. Purpose

Design the experience for every actor before building it. In an RM-assisted insurance journey the
experience *is* much of the product: an RM sitting with a customer, working through need analysis
and quote comparison, will abandon a platform that is slower or more confusing than the legacy
portal — regardless of how good the back end is.

This stage also carries a compliance load that is easy to miss. **Disclosure wording, consent
statements and suitability questions are regulated content.** They are designed here, with
Compliance, not written by a developer against a placeholder.

## 2. Entry criteria

- [ ] GATE-S03 passed: requirements and journeys defined
- [ ] GATE-S02 passed: consent and disclosure obligations known

## 3. Epics and stories

### S05-E01 — Service blueprint · *Design + Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S05-E01-S01 | Blueprint each journey front-stage | What the actor sees at every step of every journey variant |
| S05-E01-S02 | Blueprint back-stage | Supporting actions, systems and hand-offs behind each front-stage step |
| S05-E01-S03 | Map the moments of truth | Where the sale is won or lost; where an obligation binds |
| S05-E01-S04 | Design the assisted/customer hand-off | The RM-device to customer-device transition for consent and payment — a regulatory requirement, and the hardest UX in the product |

### S05-E02 — Interaction design · *Design*

| ID | Story | Acceptance criteria |
|---|---|---|
| S05-E02-S01 | Wireframe every journey step | Every wireframe references a requirement ID (**closes GAP-009**) |
| S05-E02-S02 | Design the need-analysis and suitability flow | Regulated questionnaire rendered so it is completed honestly, not clicked through |
| S05-E02-S03 | Design multi-quote comparison | Comparison basis visible; ranking rules disclosed; no dark patterns |
| S05-E02-S04 | Design proposal capture | Progressive disclosure; pre-fill from CIF/KYC; save and resume |
| S05-E02-S05 | Design the payment hand-off | QR/SMS link to the customer's device; RM sees status, never the payment surface |
| S05-E02-S06 | Design status, tracking and document access | Post-sale visibility — the capability the legacy model lacks entirely |

### S05-E03 — Design system · *Design*

| ID | Story | Acceptance criteria |
|---|---|---|
| S05-E03-S01 | Define the component library | Reusable Flutter-ready components with states and variants |
| S05-E03-S02 | Define the design tokens | Colour, type, spacing, elevation — with accessible contrast proven |
| S05-E03-S03 | Define responsive behaviour | Mobile, tablet and web; RM tablet use in a branch is a primary case |
| S05-E03-S04 | Define the interaction and motion standard | Consistent, and cheap enough to render on mid-range devices |

### S05-E04 — Content and regulated copy · *Rajal + Shailja*

| ID | Story | Acceptance criteria |
|---|---|---|
| S05-E04-S01 | Write consent statements | Exact wording, versioned, Compliance-approved, matching the S02 consent pack |
| S05-E04-S02 | Write regulatory disclosures | Placement, prominence and acknowledgement mechanism defined |
| S05-E04-S03 | Write the error and exception copy | Every failure path has copy that tells the actor what to do next |
| S05-E04-S04 | Define language and vernacular strategy | Which languages at R0, and how content is versioned per language |

### S05-E05 — Accessibility and inclusion · *Design*

| ID | Story | Acceptance criteria |
|---|---|---|
| S05-E05-S01 | Set the accessibility standard | A named conformance target with a testing method |
| S05-E05-S02 | Design for low-literacy and assisted use | The RM-assisted journey serves customers who will not read a screen |
| S05-E05-S03 | Design for poor connectivity | Branch and field connectivity is unreliable; define offline and retry behaviour |

### S05-E06 — Validation with real users · *Design + Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S05-E06-S01 | Prototype the primary journey | Clickable prototype covering the complete RM-assisted Term journey |
| S05-E06-S02 | Test with real RMs | ≥ 5 practising RMs complete the journey unaided |
| S05-E06-S03 | Test with real customers | ≥ 5 representative customers complete the customer-device steps |
| S05-E06-S04 | Iterate on findings | Findings logged, prioritised, and either fixed or explicitly deferred |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S05-VT-01 | Every requirement has a screen | Traverse R0 requirements against wireframes | No requirement with no interface |
| S05-VT-02 | Every screen has a requirement | Traverse wireframes against requirements | No unrequested screen — this is where scope creep enters |
| S05-VT-03 | RMs can complete the journey unaided | Usability test, 5+ RMs | ≥ 80% complete without help; time-on-task recorded |
| S05-VT-04 | The device hand-off is understood | Observe the consent and payment hand-off | Customers understand what they are approving without RM explanation |
| S05-VT-05 | Regulated copy is approved | Compliance reviews every consent and disclosure string | 100% approved, versioned |
| S05-VT-06 | Accessibility target is met | Audit the prototype against the standard | Conformance achieved or gaps logged with owners |
| S05-VT-07 | Failure paths have copy | Traverse the exception catalogue | Every failure path has actionable copy |

## 5. Exit gate — GATE-S05

| # | Criterion | Evidence level | Evidence artefact |
|---|---|---|---|
| S05-G1 | Service blueprint complete for R0 journeys | E1 | Blueprint |
| S05-G2 | Wireframes mapped to requirement IDs | E1 | Traceability map — **closes GAP-009** |
| S05-G3 | Design system published and adoptable | E1 | Component library + tokens |
| S05-G4 | Regulated copy approved by Compliance | E2 | Signed copy deck, versioned |
| S05-G5 | Accessibility standard set and prototype conformant | E3 | Audit report |
| S05-G6 | Prototype validated with real RMs and customers | E3 | Usability test report with findings and disposition |
| S05-G7 | Error, empty and degraded states catalogued | E1 | State catalogue |

**Approvers:** Rajal (AP) · Design (AP) · Shailja (RV, B on disclosure) · Deepali (RV, B on
authentication and consent UX) · Swapnali (RV) · Mahesh (RV)

## 6. Current position in this repository — 🔴 Missing

This is the emptiest stage in the programme, and it is not a minor one.

| Item | State |
|---|---|
| Service blueprint | Absent |
| Wireframes mapped to requirements | **GAP-009 open** — Figma exists as reference material but is not mapped to CJ/RMJ |
| Design system | Absent |
| Regulated copy deck | Absent — consent statement wording does not exist anywhere |
| Accessibility standard | Absent |
| Usability validation | Never performed |
| **Flutter application** | **No `pubspec.yaml` exists anywhere in the repository** |

**Consequence.** Every journey in the requirement set terminates at an interface that does not
exist. The platform cannot be demonstrated to a business stakeholder, cannot be usability-tested,
and cannot be piloted. S11 — the vertical slice that proves the business case — is not achievable
without at least a thin slice of this stage.

**Recommendation.** S05 does not need to be completed before S11; it needs to be completed *for
the R0 slice*. One journey, designed properly, with approved consent copy and a validated
prototype, is enough to unblock S11 and is achievable in parallel with the S08/S09 foundation
work using design capacity that engineering does not compete for.

## 7. Premature at this stage

Production Flutter code · state management architecture · API integration in the UI · pixel-level
polish on unvalidated flows.

Building the app before the journey is validated produces a beautiful implementation of the wrong
flow — and in a regulated product, of unapproved copy.
