# CR-010 — Board verdicts

**Change request:** [CR-010 — Reusable Context Module and Safe Autopilot Foundation](../../CR-010-context-module-and-safe-autopilot.md)
**Origin:** `SUG-20260816-ap1` · **Plan:** PLAN-001
**Index author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-16

---

## 1. What is in this directory

One file per board or role that must return a conclusion before CR-010 becomes binding.
[CR-010 §4](../../CR-010-context-module-and-safe-autopilot.md#4-ratification) names seven authorities;
two further roles (Delivery and Database) are included because the CR's substance reaches them.

Each verdict follows the same shape: **decision requested · what was reviewed · findings ·
verdict · numbered conditions · signature status**. Verdict vocabulary is the canonical AIGEM set
([11-REVIEW_GATES §11](../../../11-REVIEW_GATES.md)): `APPROVED` · `APPROVED_WITH_CONDITIONS` ·
`REWORK` · `REJECTED` · `NOT_APPLICABLE`, recorded here in the CR's `APPROVE` /
`APPROVE-WITH-MODIFICATION` / `REJECT` / `ESCALATE` form.

> **Every file in this directory is AI-drafted.** Not one carries a human signature. A verdict here
> is a *drafted position for its named human to adopt, amend or reject* — it is not that person's
> conclusion and does not exercise their authority. Where a persona holds a binding veto (Security,
> Risk & Compliance) or a mandatory T4 human signature, **the AI draft does not and cannot satisfy
> it**.

---

## 2. Index

| File | Board / role | Persona | Authored by | Verdict |
|---|---|---|---|---|
| [`board-1-architecture-mahesh.md`](./board-1-architecture-mahesh.md) | Board 1 — Architecture | Mahesh | Architecture agent | `APPROVE-WITH-MODIFICATION` |
| [`r3-engineering-amit.md`](./r3-engineering-amit.md) | R3 — Technical / Engineering (Board 2) | Amit | Architecture agent | `APPROVE-WITH-MODIFICATION` |
| [**`board-3-product-rajal.md`**](./board-3-product-rajal.md) | **Board 3 — Product** | **Rajal** | **Product agent (own board)** | **`APPROVE-WITH-MODIFICATION`** |
| [`board-4-security-deepali.md`](./board-4-security-deepali.md) | Board 4 — Security | Deepali | Architecture agent | `APPROVE-WITH-MODIFICATION` |
| [`board-5-qa-swapnali.md`](./board-5-qa-swapnali.md) | Board 5 — QA | Swapnali | Product agent (**simulation**) | `APPROVE-WITH-MODIFICATION` |
| [`board-6-compliance-shailja.md`](./board-6-compliance-shailja.md) | Board 6 — Risk & Compliance | Shailja | Product agent (**simulation**) | `APPROVE-WITH-MODIFICATION` |
| [`board-7-operations-shivanshi.md`](./board-7-operations-shivanshi.md) | Board 7 — Operations | Shivanshi | Architecture agent | *pending in this increment* |
| [`dba-aarti.md`](./dba-aarti.md) | Database | Aarti | Architecture agent | *pending in this increment* |
| [`r12-delivery-kalpana.md`](./r12-delivery-kalpana.md) | R12 — Delivery Control | Kalpana | Product agent (**simulation**) | `APPROVE-WITH-MODIFICATION` |

**Only Board 3 is written by the persona whose board it is.** Every other file is one agent
simulating another persona's reasoning, and each says so in its own header. The two authored by the
Product agent that carry the greatest risk of over-reach — QA and **Risk & Compliance** — carry the
loudest warnings, because Board 6 holds a binding veto that no simulation may exercise.

---

## 3. Human-signature status

| Board / role | Persona | Human signature required | Binding veto | Status |
|---|---|---|---|---|
| Board 1 — Architecture | Mahesh | Yes, to ratify | No — overridable only by a recorded ADR where AIGEM permits | ❌ **OUTSTANDING** |
| Board 2 / R3 — Technical | Amit | Yes, to ratify | No | ❌ **OUTSTANDING** |
| Board 3 — Product | Rajal | Yes, to ratify | No | ❌ **OUTSTANDING** |
| **Board 4 — Security** | **Deepali** | **Yes — T4 mandatory, no AI substitution** | **Yes, binding** | ❌ **OUTSTANDING** |
| Board 5 — QA | Swapnali | Yes, to ratify | Blocks within jurisdiction | ❌ **OUTSTANDING** |
| **Board 6 — Risk & Compliance** | **Shailja** | **Yes — T4 mandatory, no AI substitution** | **Yes, binding** | ❌ **OUTSTANDING** |
| Board 7 — Operations | Shivanshi | Yes, to ratify | Blocks on operational readiness | ❌ **OUTSTANDING** |
| Database | Aarti | Yes, to ratify | Blocks on physical data integrity | ❌ **OUTSTANDING** |
| R12 — Delivery | Kalpana | Yes, to ratify | No — timing authority only (Rule PA-1) | ❌ **OUTSTANDING** |
| **Executive Sponsor** | **unnamed — GAP-010** | **Yes, to fund `FRI-001`** | — | ❌ **NO NAMED PERSON EXISTS** |

**Nine of nine outstanding, plus a sponsor who does not yet exist.**

> **Silence never approves.** [CR-010 §2](../../CR-010-context-module-and-safe-autopilot.md#2-non-negotiable-automation-boundary)
> forbids it; [Rule RG-7](../../../11-REVIEW_GATES.md) records non-response as `NO_RESPONSE` against a
> named persona and escalates to a named human. A verdict file with no human signature is a
> **draft**, and its presence in this directory is not its ratification.

---

## 4. Aggregate position

Under [11-REVIEW_GATES §12](../../../11-REVIEW_GATES.md), a plan is approved when every mandatory board
returns `APPROVED` or `APPROVED_WITH_CONDITIONS` (or a justified `NOT_APPLICABLE`) and no board
returns `REWORK` or `REJECTED`.

**Drafted aggregate: `APPROVE-WITH-MODIFICATION`.** Every board that has drafted a position has
drafted the same verdict, with conditions. **No board has rejected, and no board has demanded
rework.**

**Actual aggregate: none.** No human has signed, two mandatory human T4 signatures are outstanding,
and two verdicts are still to be drafted. CR-010 remains **CANDIDATE**
([decision register §3](../../../registers/DECISION-REGISTER.md)).

### 4.1 Conditions that recur across boards

Where more than one board reached the same condition independently, it is likely to be the real
constraint:

| Theme | Boards | Substance |
|---|---|---|
| **No E3/E4 criterion closes on a definition artefact** | QA (Q-C1), Compliance (R-C5), Product (C7) | A workflow file is not a green run; a rule pack is not a signature |
| **GAP-006/007 freeze S11 and the freeze holds** | Product (C5), Compliance (R-C4) | Non-waivable; the failure the realignment exists to correct |
| **The automation boundary needs detection, not only prohibition** | Compliance (R-C1), Product (C7) | A boundary with no violation signal is a policy, not a control |
| **`FRI-001` needs a named human approver** | Delivery (K-C1), Product (§6) | Blocked behind GAP-010 |
| **Residency and retention are stated, not implemented** | Compliance (R-C3), Product (charter §3.3) | Non-waivable; S09 gate items |
| **`BLOCKED` must be used where a prerequisite is missing** | Delivery (K-C5), QA | `OPEN` hides a dependency; `BLOCKED` schedules it |

### 4.2 Conditions declared non-waivable

| Condition | Board | Substance |
|---|---|---|
| C5 | Product | No S11 entry while GAP-006 or GAP-007 is open |
| C7 | Product | Automation never supplies a verdict, marks `PASSED`, or treats silence as approval |
| R-C1 | Compliance | The automation boundary gains a detection mechanism |
| R-C2 | Compliance | The absent suitability gate gets a risk-register entry with a **named human** owner |
| R-C3 | Compliance | No customer PII in an environment whose region is not evidenced |
| R-C5 | Compliance | Every AI-authored artefact carries a `signature_status` block |
| C2 | Product | WS-3 transcribed into `CURRENT-STATE.yaml`, or D2 is void |
| Q-C1 | QA | No E3/E4 criterion marked `MET` on a definition artefact |
| K-C1 | Delivery | `FRI-001` approved as a named budget line by a named human |

---

## 5. What has to happen next

| # | Action | Owner | Blocks |
|---|---|---|---|
| 1 | Draft the two remaining verdicts (Operations, Database) | Architecture agent | Aggregation |
| 2 | **Name the executive sponsor** (GAP-010) | Rajal → Bancassurance leadership | `FRI-001`, K-C1, the whole recovery |
| 3 | Risk-register entry for the absent suitability gate, **named human owner** (R-C2) | Shailja | CR-010 ratification |
| 4 | **Shailja signs, amends or rejects both rule packs** | Shailja | GAP-006, GAP-007, S11 entry |
| 5 | Human review and signature on all nine verdicts | each named persona | CR-010 ratification |
| 6 | Board response clock started with required-by dates (K-C4) | Kalpana | Perpetual-CANDIDATE risk |
| 7 | Transcribe WS-3 into `CURRENT-STATE.yaml` (C2) | Mahesh + Rajal **jointly, as humans** | All WS-3 work admission |

**Item 2 is the smallest action with the largest unblocking effect in this entire set.** One name.

---

## 6. Related

- [CR-010](../../CR-010-context-module-and-safe-autopilot.md) — the change request
- [WS-3 charter](../../../workstreams/WS-3-PLATFORM-CHARTER.md) — the Product-authored workstream definition
- [Retroactive S00–S05 evidence](../../../../application-lifecycle-bible/evidence/README.md) — what the stages beneath the current work hold
- [Consent](../../../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) · [Suitability](../../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) rule packs
- [Decision register](../../../registers/DECISION-REGISTER.md) · [Gap register](../../../../au-bank-insurance-platform/po-drive/02-GAP-REGISTER.md)
