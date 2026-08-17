# Board 5 — QA · Verdict on CR-010

> ## ⚠️ THIS IS AN AI-DRAFTED SIMULATION
>
> **This document is an AI simulation of Swapnali's QA reasoning, authored by the Rajal (Product)
> agent.** It is **not** a verdict from Swapnali, and it is **not** a Board 5 conclusion.
>
> Under [11-REVIEW_GATES §8](../../../11-REVIEW_GATES.md) an AI agent simulating a board loads that
> persona's package and emits the canonical verdict form. That simulation **does not grant itself
> the persona's authority**. Swapnali holds `AP/B` — approval *and block* — at S03 testability, S08,
> S11, S12, S13 and S14. None of that is exercised here.
>
> **A Product agent drafting a QA verdict is a self-review in the direction that matters least to
> QA and most to Product.** Treat every conclusion below as a *question put to Swapnali*, not an
> answer from her. Where it is wrong, her correction wins without argument.

**Board:** 5 — QA · **Simulated persona:** [Swapnali — QA Lead](../../../../context/roles/swapnali-qa-lead/README.md)
**Reviewer type:** AGENT (Rajal agent simulating Board 5)
**Change request:** [CR-010](../../CR-010-context-module-and-safe-autopilot.md)
**Date:** 2026-08-16

---

# SIMULATED VERDICT: `APPROVE-WITH-MODIFICATION`

Four conditions (§4), of which **Q-C1 is a block** in the simulated reading.

---

## 1. Decision requested of QA

[CR-010 §4](../../CR-010-context-module-and-safe-autopilot.md#4-ratification) asks Swapnali / QA for
a conclusion on **evidence verification and CI sufficiency**. Two questions:

1. Does CR-010 make evidence *verifiable*, or only *structured*?
2. Is the CI it establishes sufficient to close the gate criteria that depend on it?

---

## 2. What was reviewed

| Artefact | Path |
|---|---|
| CR-010 | [`CR-010-context-module-and-safe-autopilot.md`](../../CR-010-context-module-and-safe-autopilot.md) |
| Structured gate evidence | [`state/GATE-EVIDENCE.yaml`](../../../state/GATE-EVIDENCE.yaml) |
| Evidence ladder and gate states | [`04-GATE-AND-SIGNOFF-MODEL.md`](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md) |
| S08 engineering foundation | [`stages/S08-engineering-foundation.md`](../../../../application-lifecycle-bible/stages/S08-engineering-foundation.md) |
| Quality norms | [`06-QUALITY-NORMS.md`](../../../../application-lifecycle-bible/06-QUALITY-NORMS.md) |
| Board 5 checklist Q1–Q8 | [`11-REVIEW_GATES.md §8`](../../../11-REVIEW_GATES.md) |
| Consent and suitability rule packs | [consent](../../../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) · [suitability](../../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) |
| S03 acceptance criteria | [`evidence/S03-requirements-evidence.md`](../../../../application-lifecycle-bible/evidence/S03-requirements-evidence.md) |
| Known open quality debt | QA-001, TD-007, TD-014 in [`CURRENT-STATE.yaml`](../../../state/CURRENT-STATE.yaml) |

---

## 3. Findings against the Board 5 checklist

| # | Check | Finding |
|---|---|---|
| **Q1** | Acceptance criteria observable and testable? | **Materially improved.** S03 evidence adds 60 Given/When/Then criteria plus 12 exception criteria, and the rule packs give every one of 86 rules its own pass/fail test. **Not yet verified by QA** — see Q-C1 |
| **Q2** | Unit / integration / E2E levels appropriate? | **Cannot be satisfied today.** [S08-G6](../../../../application-lifecycle-bible/stages/S08-engineering-foundation.md#5-exit-gate--gate-s08) requires every pyramid level operational. Testcontainers, WireMock, contract tests and the E2E harness are all **absent**. TD-014 open |
| **Q3** | Negative, boundary and error cases covered? | **Now specified**: 12 exception criteria, 9 consent failure conditions, a 7-case suitability gate matrix. Specified is not covered — none is implemented |
| **Q4** | Coverage gates still hold? | **Declaratively only.** JaCoCo is configured in `build.gradle.kts`; nothing executes it on a PR. **QA-001 is open at P0.** CR-010 adds the workflow; no green run is evidenced |
| **Q5** | Regression risk to existing journeys? | **Unmeasurable.** There is no automated build against which regression could be observed. Every "green" claim in every phase STATUS file is a human assertion |
| **Q6** | Test data realistic and PII-free? | **Absent.** [S08-E03-S06](../../../../application-lifecycle-bible/stages/S08-engineering-foundation.md#3-epics-and-stories) requires a synthetic generator covering joint life, minor nominee, NRI and PAN mismatch. None exists |
| **Q7** | Demonstrable to a PO? | **No.** There is no user interface. Nothing in R0 can be demonstrated to Rajal today |
| **Q8** | Follows TESTING-RULES? | Applies to WS-1; unchanged by this CR |

### 3.1 What CR-010 genuinely gets right, from a QA standpoint

1. **Gate evidence becomes structured and separate from human-owned lifecycle fields.**
   [`GATE-EVIDENCE.yaml`](../../../state/GATE-EVIDENCE.yaml) gives every criterion a
   `required_evidence_level`, a `verifier`, an `evidence[]` array and a `last_verified_at`. That is
   the difference between a gate that can be *audited* and one that can only be *read*.
2. **`evidence: []` is now visibly empty.** Nine of thirteen criteria across WS-1 and WS-2 carry an
   empty evidence array. Before, the absence was invisible; now it is a field.
3. **`execution_mode` distinguishes `AUTOMATABLE` from `HUMAN_REQUIRED` and `EXTERNAL_REQUIRED`.**
   This stops an agent proposing to automate a criterion that needs a person or a partner.
4. **`silence_approves: false` and `automatic_waivers: false` are explicit policy fields**, not
   prose. A machine can enforce them.
5. **Application CI is established as a prerequisite for current hardening evidence**
   ([CR-010 §1.7](../../CR-010-context-module-and-safe-autopilot.md#1-decision-requested)). This is
   the correct causal statement and QA has been unable to make it stick before.

### 3.2 The finding that drives the modification

> **CR-010 makes evidence *structured*. It does not yet make evidence *exist*.**

`GATE-EVIDENCE.yaml` is a schema for evidence, and a well-designed one. But the CR adds a workflow
file, not a green run. The distinction matters more here than usual, because this repository's
specific failure mode is **treating a definition artefact as proof of the behaviour it defines** —
[Rule GS-2](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#evidence-strength-ladder),
and the position assessment's [mechanism 3](../../../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md#7-how-this-happened--so-it-does-not-recur).

**The specific risk:** CR-010 ratifies, `.github/workflows/` gains an application build, and the
programme records S08 as progressing — while nothing has yet run, blocked anything, or produced a
report. That would be an E1 artefact closing an E4 criterion, which is the exact substitution the
gate model names as an anti-pattern.

### 3.3 On the rule packs

The two packs are the strongest testability artefact this programme has produced. Specific
observations:

| Rule | QA note |
|---|---|
| [SUIT-R05](../../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#33-the-computation) purity + [§3.4](../../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#34-worked-example-the-reference-test-case) reference case | **Exemplary.** A named reference case with expected outputs is a unit test written in prose. `SUIT-TC-REF-01` goes straight into the suite |
| [SUIT-R27](../../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#62-test-matrix--all-seven-cases-are-mandatory-tests) 100% branch on the gate, no waiver | Correct, and QA will hold it. It pairs with [S08-VT-07](../../../../application-lifecycle-bible/stages/S08-engineering-foundation.md#4-validation-tests) |
| [SUIT-R40](../../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#71-what-may-never-be-overridden) no bypass by flag/config/header | **Proving a negative.** The stated test — "search the codebase for any conditional that can disable the gate" — is not a repeatable automated assertion. Needs an ArchUnit rule or an equivalent structural test. **Q-C3** |
| [CNS-R16](../../../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#51-immutability) append-only by grant | Testable, and the test must run as the **application's** database role, not as a superuser. Easy to get wrong and produce a false pass |
| [CNS-R20](../../../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#51-immutability) / `AC-SEC-040-1` no PII in logs | Requires the log-scan harness of [S08-E05-S05](../../../../application-lifecycle-bible/stages/S08-engineering-foundation.md#3-epics-and-stories). Absent |
| [CNS-R35](../../../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#8-retention-and-retrieval) 4-hour retrieval SLA | **E3, not E4** — a timed drill, not a CI job. Correctly levelled in the pack |
| [SUIT-R26](../../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#62-test-matrix--all-seven-cases-are-mandatory-tests) fail closed on service unavailability | Requires fault injection in the test harness. Not currently possible |

**Ambiguity found, for Rajal:** [QR-01](../../../../application-lifecycle-bible/evidence/S04-product-definition-evidence.md#43-quote-rules--closes-gap-012)
defines offer validity as *"the shorter of the insurer's stated validity and 7 calendar days from
`quoteReceivedAt`"*. `quoteReceivedAt` is not defined — the platform's receipt from the aggregator,
or the aggregator's receipt from the insurer? On a slow insurer these differ by minutes, and an
expiry boundary test cannot be written until it is settled. **Minor, but it is exactly the class of
thing that becomes a defect at S11.**

---

## 4. Conditions

| # | Condition | Test | Owner | Due |
|---|---|---|---|---|
| **Q-C1** 🚫 | **No gate criterion at E3 or E4 may be marked `MET` on the existence of a workflow file, a rule pack or any other definition artefact.** E4 requires a run ID; E3 requires an executed report | Sample every criterion whose state is `MET`. Each carries a resolvable link to an executed artefact. Any `MET` whose evidence is a document path is reverted | Swapnali | continuous from ratification |
| **Q-C2** | **S08-G1 closes only on a green application-CI run over all `libs/` and `services/` on a pull request**, with a run ID recorded in `GATE-EVIDENCE.yaml` | `.github/workflows` build run, green, with `evidence[]` populated and `last_verified_at` set | Amit + Swapnali | S08 gate |
| **Q-C3** | **[SUIT-R40](../../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md#71-what-may-never-be-overridden) gains a structural test.** "No conditional can disable the suitability gate" must be an executable assertion — ArchUnit or equivalent — not a manual code search | An ArchUnit rule fails the build when a bypass conditional is introduced. Demonstrated by deliberately introducing one ([S08-VT-03](../../../../application-lifecycle-bible/stages/S08-engineering-foundation.md#4-validation-tests) pattern) | Swapnali + Amit | S08 gate |
| **Q-C4** | **QA-001 closes on a coverage report from CI, or is waived with all six waiver elements** including a named human risk owner and an expiry. It does not close by re-stating the threshold | Coverage report from a CI run, or a waiver record satisfying all six of [gate model §8](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#8-waivers-and-exceptions) | Swapnali | S08 gate |

**Q-C1 is the block.** Without it, CR-010's structured evidence model becomes a more organised way
of recording assertions, and the failure mode it exists to correct survives in a better schema.

---

## 5. What QA does **not** conclude

- Whether the two rule packs are **regulatorily sufficient** — Shailja's, and QA has no view.
- Whether the context-module architecture is sound — Mahesh's.
- Whether automation permissions are safe — Deepali's.
- **Whether WS-1 Phase 4 may pass.** Criteria 4.1, 4.6 and 4.7 remain `BLOCKED`/`PARTIAL` on
  evidence that does not exist, and nothing in CR-010 changes that.

---

## 6. Signature status

```yaml
board: QA
plan: CR-010
reviewer: "Swapnali — QA Lead (AI SIMULATION by the Rajal agent)"
reviewer_type: AGENT
self_review: true             # Product agent simulating QA on Product-authored artefacts
date: "2026-08-16"
decision: APPROVE_WITH_MODIFICATION
conditions: [Q-C1, Q-C2, Q-C3, Q-C4]
blocking: [Q-C1]
signature_status: "AI-DRAFTED — mandatory human signature outstanding"
```

> **`AI-DRAFTED — mandatory human signature outstanding`.**
>
> This is a **simulation authored by the Product agent**, not a Board 5 verdict. Swapnali's `AP/B`
> authority at S03 testability, S08, S11, S12, S13 and S14 is **not** exercised by it, and none of
> the four conditions binds anyone until she adopts them.
>
> Its purpose is to put QA's questions on the record early enough to be useful, and to make visible
> what a QA reviewer will ask — not to answer on her behalf.
>
> **Silence is not approval.** Under [Rule RG-7](../../../11-REVIEW_GATES.md), a board that does not
> respond is recorded as `NO_RESPONSE` against a named persona; it is never recorded as approval.

**Drafted by:** the Rajal agent, Board 3 / R1, simulating Board 5 · 2026-08-16
**Awaiting:** Swapnali — QA Lead, Board 5
