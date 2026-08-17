# Retroactive stage evidence — S00 to S05

**Workstream:** WS-3 — AU Bank Insurance Distribution Platform ([charter](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md))
**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-16
**Under:** [Rule SM-3 — a stage may be entered late, but never skipped](../02-STAGE-MODEL.md#53-the-back-fill-rule)

---

## What these files are

The platform is executing at S08 with S00–S05 never formally gated. Under Rule SM-3 the earlier
stages are entered **retroactively**: their epics are executed against what already exists, and
their gates are assessed against the current system rather than a hypothetical one. This is
*underpinning* — the subject of [`03-REALIGNMENT-PLAN.md`](../03-REALIGNMENT-PLAN.md).

Each file follows one shape:

1. what the stage requires (from its `stages/Sxx-*.md`)
2. **what already exists, cited by path** — inventory before creating anything
3. what was actually missing
4. the new evidence added
5. what remains genuinely open, with an owner and a target date
6. a retroactive stage verdict

> **These files do not recreate existing work.** [S01](./S01-discovery-evidence.md) is short
> because discovery is already strong and its stage file says plainly: *do not redo this stage*.
> [S05](./S05-experience-evidence.md) is long because it was empty. Length tracks the gap, not the
> effort.

---

## Index and verdicts

| Stage | File | Verdict | Gaps closed **on content** | Still blocking |
|---|---|---|---|---|
| **S00** Ideation & Business Case | [`S00-ideation-evidence.md`](./S00-ideation-evidence.md) | `CLOSED-WITH-CONDITIONS` | Business case, options paper, 10-KPI model, decision rights, steering design, `FRI-001` funding line | **GAP-010 sponsor unnamed** · funding not approved |
| **S01** Discovery & Capability Definition | [`S01-discovery-evidence.md`](./S01-discovery-evidence.md) | `CLOSED-WITH-CONDITIONS` | S01-VT-05 enforcement mechanism; GAP-023 re-scoped to R1 | Glossary E2 signature · 3-RM as-is validation |
| **S02** Regulatory & Compliance Framing | [`S02-regulatory-evidence.md`](./S02-regulatory-evidence.md) | `CLOSED-WITH-CONDITIONS` | **GAP-006** (38 consent rules) · **GAP-007** (48 suitability rules) | **Shailja's signature on both packs** · field-level classification · residency and retention *implementation* |
| **S03** Business Requirements | [`S03-requirements-evidence.md`](./S03-requirements-evidence.md) | `CLOSED-WITH-CONDITIONS` | **GAP-008** (60 G/W/T criteria + 12 exception criteria) · **GAP-016** (attribute sheets) · traceability matrix · GAP-014 behaviour half | Swapnali's testability review · stakeholder acceptance |
| **S04** Product Definition & Release Slicing | [`S04-product-definition-evidence.md`](./S04-product-definition-evidence.md) | `CLOSED-WITH-CONDITIONS` | **GAP-012** (12 quote rules) · **GAP-013** (11 matrix dimensions + R0 population rule) · product routing PR-01…06 · **19 technical enablers — S04-VT-06 moves FAIL → PASS** | R0 insurer named · Kalpana's sizing and critical path |
| **S05** Experience Design | [`S05-experience-evidence.md`](./S05-experience-evidence.md) | `CLOSED-WITH-CONDITIONS` | **GAP-009** (18-screen inventory mapped to requirements) · service blueprint · flow · 18×4 state catalogue · design-system spec · accessibility standard | **Consent/disclosure copy** · brand substitution · usability validation with real RMs and customers |

**Six stages, six `CLOSED-WITH-CONDITIONS`.** Not one `CLOSED`, and that is the honest result:
every stage now has every artefact Product can produce, and every stage still needs at least one
human act — a signature, an approval, a test with real people — that no document and no AI can
supply.

---

## The supporting rule packs

The two P0 build-freeze gaps are closed on content by artefacts that live with the product
documentation, not with the evidence:

| Pack | Path | Rules | Closes |
|---|---|---|---|
| Consent Rule Pack v1 | [`rule-packs/consent-rule-pack.md`](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) | `CNS-R01`–`CNS-R38` | GAP-006 |
| Suitability Rule Pack v1 | [`rule-packs/suitability-rule-pack.md`](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) | `SUIT-R01`–`SUIT-R48` | GAP-007 |

Every rule carries an ID, a normative statement and an executable pass/fail test.

---

## What "content-complete" means, and what it does not

> **Content-complete ≠ closed.** A rule pack is an **E2** artefact under the
> [evidence ladder](../04-GATE-AND-SIGNOFF-MODEL.md#evidence-strength-ladder), and E2 means
> *reviewed and signed*. An AI drafting Compliance's reasoning does not discharge a mandatory human
> Compliance signature, and [CR-010 §2](../../governance/change-requests/CR-010-context-module-and-safe-autopilot.md#2-non-negotiable-automation-boundary)
> forbids treating silence as approval.

**GAP-006 and GAP-007 therefore remain OPEN**, and under
[Rule SM-4](../02-STAGE-MODEL.md#54-freeze-semantics) they continue to freeze S11 entry. That
freeze is the correct outcome. It is the mechanism that was missing when the quote path was built
and hardened past two open P0 build-freeze gaps, and I am not looking for a way around it.

---

## Reading order

For someone assessing whether the platform is ready to move:

1. [`01-POSITION-ASSESSMENT.md`](../01-POSITION-ASSESSMENT.md) — where the platform actually is
2. [`03-REALIGNMENT-PLAN.md`](../03-REALIGNMENT-PLAN.md) — the five moves
3. [WS-3 charter](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md) — what the workstream is
4. This directory, S00 → S05 — what the stages beneath the current work actually hold
5. [CR-010 verdicts](../../governance/change-requests/CR-010/verdicts/README.md) — what each board concluded

---

## Related

- [`02-STAGE-MODEL.md`](../02-STAGE-MODEL.md) — the sixteen stages and the movement rules
- [`04-GATE-AND-SIGNOFF-MODEL.md`](../04-GATE-AND-SIGNOFF-MODEL.md) — evidence ladder, gate states, waivers
- [`05-DOCUMENTATION-CANON.md`](../05-DOCUMENTATION-CANON.md) — which artefact each stage owes
- [`stages/`](../stages/S00-ideation.md) — the stage definitions these files are assessed against
- [`po-drive/02-GAP-REGISTER.md`](../../au-bank-insurance-platform/po-drive/02-GAP-REGISTER.md) — the gap register these verdicts update
