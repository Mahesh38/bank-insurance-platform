# Board 6 — Risk & Compliance · Verdict on CR-010

> ## ⚠️ THIS IS AN AI-DRAFTED SIMULATION — AND BOARD 6 IS THE ONE WHERE THAT MATTERS MOST
>
> **This document is an AI simulation of Shailja's compliance reasoning, authored by the Rajal
> (Product) agent.** It is **not** a verdict from Shailja and **not** a Board 6 conclusion.
>
> Board 6 holds a **binding veto** that no aggregate or majority may override
> ([11-REVIEW_GATES §12](../../../11-REVIEW_GATES.md)). At **T4**, Compliance sign-off is a
> **mandatory human signature with no AI substitution** — and Board 6's own persona package states
> that it *"never replaces the T4 human sign-off rule"*.
>
> [Rajal's authority §3](../../../../context/roles/principal-insurance-platform-product-owner/03-authority-and-decision-rights.md#3-product-does-not-own)
> is unambiguous: Product does **not** own regulatory or legal interpretation, privacy-law
> permissibility, or mandatory human approvals. **Nothing below is a compliance conclusion.** Every
> item is a *question put to Shailja with the supporting evidence attached*, so that her review is
> faster — not so that it can be skipped.
>
> A Product agent simulating a binding-veto board is the highest-risk artefact in this entire set.
> It exists to surface findings early. It confers nothing.

**Board:** 6 — Risk & Compliance · **Simulated persona:** [Shailja S — Compliance & Risk Head](../../../../context/roles/shailja-s-compliance-risk-head/README.md)
**Reviewer type:** AGENT (Rajal agent simulating Board 6)
**Change request:** [CR-010](../../CR-010-context-module-and-safe-autopilot.md)
**Date:** 2026-08-16

---

# SIMULATED VERDICT: `APPROVE-WITH-MODIFICATION`

Five conditions (§5). **R-C1, R-C2 and R-C3 are non-waivable in the simulated reading** — and
whether they truly are is Shailja's determination, not mine.

---

## 1. Decision requested of Compliance

[CR-010 §4](../../CR-010-context-module-and-safe-autopilot.md#4-ratification) asks Shailja / Compliance
for a conclusion on the **non-waivable and human-signature boundaries**. Four questions, and the
CR's scope raises a fifth and sixth that Product has attached because they are live:

1. Is [CR-010 §2](../../CR-010-context-module-and-safe-autopilot.md#2-non-negotiable-automation-boundary)
   sufficient to prevent automation supplying a compliance conclusion?
2. Are the consent and suitability hard-gates correctly treated as non-waivable?
3. Is data residency adequately protected by this CR?
4. Is 7-year retention adequately protected?
5. *(attached)* What is the compliance position on a **delivered, hardened quote path with no
   suitability gate**?
6. *(attached)* Do the two new rule packs discharge GAP-006 and GAP-007?

---

## 2. What was reviewed

| Artefact | Path |
|---|---|
| CR-010, especially §2 | [`CR-010-context-module-and-safe-autopilot.md`](../../CR-010-context-module-and-safe-autopilot.md) |
| Gate evidence policy fields | [`state/GATE-EVIDENCE.yaml`](../../../state/GATE-EVIDENCE.yaml) |
| Non-waivable list and waiver requirements | [`04-GATE-AND-SIGNOFF-MODEL.md §8`](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#8-waivers-and-exceptions) |
| Approver map — where `H` (human mandatory) appears | Same, §5 |
| S02 regulatory framing | [`stages/S02-regulatory-framing.md`](../../../../application-lifecycle-bible/stages/S02-regulatory-framing.md) |
| Board 6 checklist R1–R8 | [`11-REVIEW_GATES.md §9`](../../../11-REVIEW_GATES.md) |
| Shailja's package | [regulatory registry](../../../../context/roles/shailja-s-compliance-risk-head/02-regulatory-registry.md) · [control catalogue](../../../../context/roles/shailja-s-compliance-risk-head/03-control-catalogue.md) · [evidence policy](../../../../context/roles/shailja-s-compliance-risk-head/06-evidence-policy.md) · [human exception model](../../../../context/roles/shailja-s-compliance-risk-head/07-human-exception-and-risk-acceptance.md) |
| Obligations: IRDAI CA0515, RBI device isolation, retention, residency | [`business-problem-statement.md §1.2, §9`](../../../../context/business-problem-statement.md#12-regulatory--compliance-directives) |
| New rule packs | [consent](../../../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) · [suitability](../../../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) |
| Position assessment GAP-C | [`01-POSITION-ASSESSMENT.md`](../../../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md#gap-c--the-compliance-hard-gates-that-make-this-business-legal-are-not-implemented--critical) |

---

## 3. Findings against the Board 6 checklist

| # | Check | Finding |
|---|---|---|
| **R1** | Does a regulatory obligation apply? | **Yes, several.** IRDAI corporate agency (CA0515): mandatory suitability before quote; explicit, timestamped, immutably stored consent; non-repudiable attribution to distributor and SP. RBI: no premium payment on a bank-employee device. Retention: 7 years. Residency: AWS India regions for all data, backups, logs and archives |
| **R2** | Consent captured, referenced and enforceable? | **Specified for the first time.** [Consent pack](../../../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) defines 5 events, 24 evidence fields, capture per channel, validity, withdrawal and retention across 38 testable rules. **Not implemented, and not signed** |
| **R3** | Auditable with actor attribution? | Specified: `actorId`, `agentId` (SP licence), server-sourced `distributorId` on every regulated record. `AC-SEC-030-2` asserts a caller-supplied `distributorId` is ignored. **Not implemented** |
| **R4** | Retention and deletion satisfied? | **No.** 7 years is stated as policy; [CNS-R33–R36](../../../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md#8-retention-and-retrieval) specify the control; **no S3 Object Lock configuration exists**. Per-class retention schedule and disposal method still absent |
| **R5** | Financial controls preserved? | Reconciliation is in the `Policy Sold` definition ([D-007](../../../../au-bank-insurance-platform/DECISION-LOG.md)) and `isSold` is specified as derived, never settable. Maker-checker exists in WS-2 scope. Unchanged by this CR |
| **R6** | Operational risk if it misbehaves? | **The material risk is not in CR-010's automation.** It is that a hardened quote path exists with no suitability gate — see §4 |
| **R7** | Traceable from requirement to evidence? | **Materially improved.** A traceability matrix now exists in [S03 evidence §6](../../../../application-lifecycle-bible/evidence/S03-requirements-evidence.md#6-traceability-matrix--closes-d4-satisfies-s03-g5) with a reverse check showing zero orphan controls. Previously there was an *example*, not a matrix |
| **R8** | Reporting or disclosure obligation created? | Not by this CR. §4 may create one; that is Shailja's determination |

### 3.1 On CR-010 §2 — the automation boundary

[CR-010 §2](../../CR-010-context-module-and-safe-autopilot.md#2-non-negotiable-automation-boundary)
states that automation may never independently mark a stage `PASSED`, provide Product, Architecture,
Security, QA, Compliance or Operations approval, accept material risk, create an open-ended waiver,
weaken a binding control because a reviewer is late, or treat silence as approval.

**In the simulated reading this is correctly drawn, and it is the single most important clause in
the CR.** Two supporting observations:

1. It is **mirrored in machine-readable policy**:
   [`GATE-EVIDENCE.yaml`](../../../state/GATE-EVIDENCE.yaml) carries `mode: proposal-only`,
   `human_pass_required: true`, `silence_approves: false`, `automatic_waivers: false`. A boundary
   stated only in prose is a boundary nothing enforces.
2. `execution_mode: HUMAN_REQUIRED` on criteria 4.4 and A.5 shows the model already distinguishing
   what an agent may not touch.

**The gap:** §2 forbids the acts but names no **detection** for a breach. A boundary with no
observable violation signal is a policy, not a control. **R-C1.**

### 3.2 On the rule packs — GAP-006 and GAP-007

Both packs are Product-drafted, structurally complete, and **explicitly reserve every
permissibility question to Compliance**. Each ends with five named open items owned by Shailja.

**In the simulated reading the drafting posture is correct**: they specify *structure, fields,
control points and failure semantics* while leaving *legal text, calibration and permissibility* to
Compliance. That is the right division and it lets engineering start without pre-empting a legal
conclusion.

**They do not close GAP-006 or GAP-007.** S02-G3 and S02-G4 require **E2 — reviewed and signed**.
An AI drafting compliance reasoning does not discharge a mandatory human Compliance signature. The
packs are *content-complete, ratification-pending*, and
[Rule SM-4](../../../../application-lifecycle-bible/02-STAGE-MODEL.md#54-freeze-semantics)
continues to freeze S11 entry. **In the simulated reading, that freeze is correct and should not be
relaxed to unblock delivery.**

Ten open items requiring a written Compliance determination: **R-C4**.

---

## 4. The finding this board would raise first

> **A delivered, hardened quote path exists with no suitability hard-gate in front of it.**

[GAP-C](../../../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md#gap-c--the-compliance-hard-gates-that-make-this-business-legal-are-not-implemented--critical)
records it, and the sequence is on the record:

1. GAP-006 and GAP-007 were classified **P0 — block scope / build freeze**.
2. Both remained open.
3. The quote path was built, delivered and moved to hardening.
4. IRDAI requires suitability to precede recommendation; **our own baseline calls bypassing it
   illegal**.

**Mitigating, and material:** the path is not in production; there is no user interface; WS-1 Phase
5 is stopped under Product condition C6; and the suitability content now exists in draft.

**Aggravating:** the exposure has existed for the duration of the phase, it is **not currently on
the risk register**, and it therefore has **no named human risk owner** — which is precisely the
condition Shailja's [human exception and risk acceptance model](../../../../context/roles/shailja-s-compliance-risk-head/07-human-exception-and-risk-acceptance.md)
exists to prevent.

**R-C2** requires a risk-register entry with a named human owner and a review date, **before**
CR-010 ratification rather than after. Whether the residual risk is acceptable, and whether any
disclosure obligation arises, are determinations **only Shailja can make**.

### 4.1 Residency and retention — stated, not controlled

| Obligation | Status | Non-waivable? |
|---|---|---|
| All data, backups, logs, archives in AWS India regions | **Stated** in [§9.2](../../../../context/business-problem-statement.md#92-security--data-governance-standards). **No IaC exists.** The current deployment path is a `starter`-plan Render.com service whose region is unchosen and unverified | **Yes** — [gate model §8](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#8-waivers-and-exceptions) |
| 7-year retention, write-once | **Stated.** No S3 Object Lock configuration exists | Statutory |

> **Any customer PII processed through the current deployment path is a data-residency question
> nobody has answered.** In the simulated reading this justifies an absolute constraint until the
> S09 controls exist: **R-C3** — no customer or production-like PII may traverse an environment
> whose region is not evidenced.

---

## 5. Conditions

| # | Condition | Test | Owner | Waivable |
|---|---|---|---|---|
| **R-C1** | The [CR-010 §2](../../CR-010-context-module-and-safe-autopilot.md#2-non-negotiable-automation-boundary) boundary gains a **detection mechanism**, not only a prohibition. Every automated transition proposal is logged with actor, proposed transition and evidence cited; any attempt to set `PASSED`, populate an approval, or extend a waiver from automation raises a **visible governance alarm** | Attempt each forbidden act from automation. Each is refused **and** produces a durable, attributable record | Mahesh + Deepali | **No** |
| **R-C2** | The **absent suitability gate on the delivered quote path** is entered in [`RISK-REGISTER.md`](../../../registers/RISK-REGISTER.md) with a **named human risk owner**, likelihood, impact, treatment and review date — **before CR-010 ratification** | A RISK-0xx row exists naming an individual, not a persona and not an agent | **Shailja** | **No** |
| **R-C3** | **No customer PII and no production-like PII may traverse any environment whose region is not evidenced as AWS India.** Binding until S09 delivers residency controls | Every environment holding such data has evidenced region configuration. Render.com is dev-preview only and never a data path for PII | Shivanshi + Deepali + Shailja | **No — residency for regulated data is on the non-waivable list** |
| **R-C4** | The **ten open Compliance items** inside the two rule packs receive **written determinations**. GAP-006 and GAP-007 close on Shailja's signature and on nothing else | Both packs carry a Compliance signature; all ten items are resolved | **Shailja** | Not applicable — this *is* the closure path |
| **R-C5** | Every AI-authored verdict, evidence file and rule pack carries an explicit `signature_status` block naming the human signature outstanding. **Silence is never recorded as approval** | Sample every AI-authored governance artefact; each carries the block. No gate transition cites an AI verdict as an approval | Rajal + Mahesh | **No** |

---

## 6. What this board would explicitly refuse

Recorded because a simulated verdict is most dangerous where it appears permissive:

| Refused | Reason |
|---|---|
| Any waiver of the suitability hard-gate before quote | Non-waivable by any authority at any tier |
| Any waiver of consent capture where consent is legally required | Non-waivable |
| Any waiver of data residency for regulated data | Non-waivable |
| Any substitution of an AI conclusion for a T4 human Compliance signature | Non-waivable; the boundary the framework rests on |
| Any waiver with no expiry | [Rule GS-5](../../../../application-lifecycle-bible/04-GATE-AND-SIGNOFF-MODEL.md#8-waivers-and-exceptions) — a scope change wearing a disguise |
| S11 entry with GAP-006 or GAP-007 open | Rule SM-4; and it is the failure this whole realignment exists to correct |

---

## 7. Signature status

```yaml
board: RISK_COMPLIANCE
plan: CR-010
reviewer: "Shailja S — Compliance & Risk Head (AI SIMULATION by the Rajal agent)"
reviewer_type: AGENT
self_review: true             # Product agent simulating a binding-veto board on Product-authored artefacts
date: "2026-08-16"
decision: APPROVE_WITH_MODIFICATION
conditions: [R-C1, R-C2, R-C3, R-C4, R-C5]
non_waivable: [R-C1, R-C2, R-C3, R-C5]
signature_status: "AI-DRAFTED — MANDATORY HUMAN T4 COMPLIANCE SIGNATURE OUTSTANDING"
binding_veto_exercised: false  # an AI cannot exercise Board 6's veto
```

> **`AI-DRAFTED — MANDATORY HUMAN T4 COMPLIANCE SIGNATURE OUTSTANDING`.**
>
> Board 6's veto is **binding and non-overridable by any aggregate or majority**. It has **not been
> exercised here**, because an AI cannot exercise it. This document is a briefing pack for Shailja
> with the evidence assembled and the questions stated — nothing more.
>
> **No conclusion in this document is a compliance conclusion.** Not F1–F6 in the
> [S02 evidence](../../../../application-lifecycle-bible/evidence/S02-regulatory-evidence.md#5-the-permissibility-conclusion--shailjas-to-make),
> not the rule packs, not R-C1 through R-C5. All are findings for Board 6 to ratify, amend or
> reject.
>
> **Silence is not approval** ([CR-010 §2](../../CR-010-context-module-and-safe-autopilot.md#2-non-negotiable-automation-boundary),
> [Rule RG-7](../../../11-REVIEW_GATES.md)). A late Compliance response is recorded as
> `NO_RESPONSE` and escalates to a named human. It never becomes consent.
>
> **GAP-006 and GAP-007 remain OPEN until Shailja signs.** S11 stays frozen. That is the correct
> state and no part of this document should be read as arguing otherwise.

**Drafted by:** the Rajal agent, Board 3 / R1, simulating Board 6 · 2026-08-16
**Awaiting:** Shailja S — Compliance & Risk Head, Board 6 · **human signature, T4, no substitution**
