# 05 — Stakeholder Review & Sign-Off Sheet

**Proposal under review:** [02-REALIGNMENT-PROPOSAL.md](./02-REALIGNMENT-PROPOSAL.md) ·
change request [`CR-002`](../../../governance/registers/DECISION-REGISTER.md#3-change-requests)
**Instruction:** fill in your block **before** the war room. Bring it with you.

---

## How to review

Four verdicts are available. Choose one.

| Verdict | Meaning |
|---------|---------|
| **APPROVE** | Proceed as proposed |
| **APPROVE WITH CONDITIONS** | Proceed, and these named conditions are binding on the plan |
| **REJECT** | Do not proceed — **state the alternative**, per ground rule 4 |
| **ABSTAIN** | Not my domain. Recorded, not counted |

Three rules:

1. **Answer your questions in writing.** A signature with blank questions is not a review.
2. **Conditions must be testable.** "Be careful with security" is not a condition; "no flow
   reaches S6 without a threat model for the identity path" is.
3. **Your veto is limited to your domain** (right-hand column below), and inside that domain it
   is absolute. Security and Compliance vetoes on T4 items can never be waived
   ([14 §1](../../../governance/14-CHANGE_CONTROL.md#1-what-needs-a-change-request)).

---

## 1. Solution Architect — Mahesh

**You approve:** `CR-002` jointly with the PO · WS-0 workstream and its stage map · Rules S-1 to
S-4 · the canonical-model-first principle (P5)
**Your veto domain:** architecture, contracts, canonical model, stage gates

| # | Question you must answer | Your answer |
|:-:|--------------------------|-------------|
| A1 | Is the finding correct that L1/L2 (business design, domain design) were skipped **at platform level**, while the 1SB module's own design is sound? | |
| A2 | Can the canonical domain model (PZ.5) for Lead / Consent / Suitability / Quote / Policy be produced in **one sprint** to v0 depth? If not, what is realistic? | |
| A3 | Is **Rule S-2** (canonical contracts owned by Track A; Track B never mints one) enforceable in review, given the existing ports/adapters and ArchUnit setup? | |
| A4 | Does the built architecture constrain the canonical model in any way we would not choose freely today — i.e. **has 1SB's model already leaked into the bank's?** | |
| A5 | Do you accept sitting in Track A rather than Track B ([02 §6](./02-REALIGNMENT-PROPOSAL.md#6-staffing--the-split-the-po-proposed) non-negotiable 1)? | |
| A6 | Should WS-1 and WS-2 become **modules within WS-0**, or remain peer workstreams? | |

> **A4 is the question this pack most needs answered honestly.** If the answer is "yes, in
> places", say where — that is a design finding to schedule, not a criticism of anyone.

**Verdict:** ☐ APPROVE ☐ APPROVE WITH CONDITIONS ☐ REJECT ☐ ABSTAIN
**Conditions:**
**Signature / date:** _______________________

---

## 2. Technical Head — Amit

**You approve:** capacity split · staffing non-negotiables · engineering feasibility of the flow plan
**Your veto domain:** engineering capacity, team structure, technical strategy

| # | Question you must answer | Your answer |
|:-:|--------------------------|-------------|
| T1 | Is the **~40 / 60** Track A / Track B split achievable with today's team, and what is the actual headcount it maps to? | |
| T2 | Can we honour **"nobody is on both tracks"**, or does team size force overlap? If it forces overlap, which roles and what is the mitigation? | |
| T3 | Is 1–2 sprints per flow realistic for F3–F6 (lead, consent, suitability, catalogue), given they are greenfield? | |
| T4 | What happens to delivery velocity in Sprints 1–3, and what do we tell steering **before** they see it in the burndown? | |
| T5 | Do you agree the five built services should be **retro-fitted rather than parked** ([01 §4](./01-PROCESS-GAP-ANALYSIS.md#4-are-the-five-services-the-wrong-scope))? | |

**Verdict:** ☐ APPROVE ☐ APPROVE WITH CONDITIONS ☐ REJECT ☐ ABSTAIN
**Conditions:**
**Signature / date:** _______________________

---

## 3. Delivery Lead

**You approve:** ways of working (PZ.2) · sprint cadence · governance state changes you will execute
**Your veto domain:** delivery process, cadence, governance state maintenance

| # | Question you must answer | Your answer |
|:-:|--------------------------|-------------|
| D1 | Can you execute the eight governance changes in [02 §9](./02-REALIGNMENT-PROPOSAL.md#9-what-changes-in-the-repository-if-cr-002-is-approved) within 2 working days of approval? | |
| D2 | Does the two-track model fit one sprint calendar and one board, or do we need two boards? What is the cost of two? | |
| D3 | How do we **detect** a Rule S-1 breach — PR template field, CI check, or review discipline? Pick one and own it | |
| D4 | Is a demo every sprint realistic during Sprints 1–2, when Track A output is mostly specifications? | |
| D5 | `GOV-004` and `CR-001` still carry outstanding PO / QA counter-signatures. Do we close them in this session? | |

**Verdict:** ☐ APPROVE ☐ APPROVE WITH CONDITIONS ☐ REJECT ☐ ABSTAIN
**Conditions:**
**Signature / date:** _______________________

---

## 4. Tech Lead

**You approve:** Track B backlog and its cap · debt sequencing · the retro-fit method
**Your veto domain:** Track B execution, technical debt, buildability

| # | Question you must answer | Your answer |
|:-:|--------------------------|-------------|
| L1 | Is the Track B allowed-work list complete — gate criteria 4.1–4.7, A.1–A.6, TD-006/009/010/014/022/023, QA-001? What is missing? | |
| L2 | Can WS-1 Phase 4 close in **2 sprints** excluding the external 4.3 dependency? | |
| L3 | The PO refers to *"hardening, boilerplate removal, smaller stuff"*. Name it concretely — which debt IDs, and what is not yet on the ledger? | |
| L4 | Does Rule S-1 (no requirement ID, no merge) create friction that would stall legitimate hardening work? Where exactly? | |
| L5 | For the retro-fit ([02 §5](./02-REALIGNMENT-PROPOSAL.md#5-retro-fitting-traceability--how-concretely)) — how many behaviours are we mapping, roughly, across the five services? | |

**Verdict:** ☐ APPROVE ☐ APPROVE WITH CONDITIONS ☐ REJECT ☐ ABSTAIN
**Conditions:**
**Signature / date:** _______________________

---

## 5. QA Lead

**You approve:** the S1 acceptance-criteria gate · the S5 evidence gate · the DoD amendment
**Your veto domain:** testability, test strategy, quality gates

| # | Question you must answer | Your answer |
|:-:|--------------------------|-------------|
| Q1 | Do you accept the role of **confirming every acceptance criterion is testable before the PO signs it** (S1 gate)? | |
| Q2 | Is the RTM (requirement → AC → test evidence) the right artefact, and are you the right owner of its test column? | |
| Q3 | `RISK-008` records that no QA cycle ran for Phase 4 stories. Does this plan close that, and by when? | |
| Q4 | Gate 4.7 / `QA-001` — close in Sprint 1, or waive with an expiry date per [15-TECH_DEBT_POLICY](../../../governance/15-TECH_DEBT_POLICY.md)? | |
| Q5 | For retro-fitted flows F1/F2, can AC be written **and executed as tests** against already-built services without redesigning them? | |
| Q6 | You have an outstanding counter-signature on `CR-001`. Confirm or object today | |

**Verdict:** ☐ APPROVE ☐ APPROVE WITH CONDITIONS ☐ REJECT ☐ ABSTAIN
**Conditions:**
**Signature / date:** _______________________

---

## 6. Security Architect

**You approve:** security posture of the dual-track model · WS-2 gate criteria · S2 security review point
**Your veto domain:** identity, authorization, secrets, data protection — **never waivable for T4**

| # | Question you must answer | Your answer |
|:-:|--------------------------|-------------|
| S1 | Commit `cd40460` landed the auth **specification and its implementation together** — no design review point existed. Does the built design hold up under review now? | |
| S2 | Is a security verdict at **S2 (design)** for every flow touching identity, PII, payment or consent sufficient, or do you need a second at S5? | |
| S3 | Can WS-2 gate criteria A.1–A.6 be evidenced within 2–3 sprints as planned? | |
| S4 | The business role / entitlement model was implemented before it was specified. **Is there a security gap in what was assumed?** | |
| S5 | Does Track B's cap risk deferring a security fix that should be P1? How would we catch that — the P1 override classes in [05 §3](../../../governance/05-PRIORITY_MODEL.md#3-hard-p1-overrides)? | |
| S6 | `TD-006` — AWS Secrets Manager provider is still a stub with a fail-fast prod profile. Acceptable until Phase 6? | |

**Verdict:** ☐ APPROVE ☐ APPROVE WITH CONDITIONS ☐ REJECT ☐ ABSTAIN
**Conditions:**
**Signature / date:** _______________________

---

## 7. Compliance

**You approve:** regulatory adequacy of the plan · consent and suitability sequencing · audit evidence model
**Your veto domain:** IRDAI CA0515, RBI, consent, suitability, PII, retention — **never waivable**

| # | Question you must answer | Your answer |
|:-:|--------------------------|-------------|
| C1 | Today no built behaviour traces to a signed business requirement ([01 §4](./01-PROCESS-GAP-ANALYSIS.md#4-are-the-five-services-the-wrong-scope)). **Is that an audit finding as it stands?** | |
| C2 | Is the RTM (requirement → AC → test evidence) the evidence artefact you would need under a CA0515 review? What else? | |
| C3 | `GAP-006` consent rule pack and `GAP-007` suitability pack are open and they **block F4 and F5**. What is the realistic date and who owns it? | |
| C4 | Gate criterion 4.4 (compliance review of audit schema and log samples) — can it complete in Sprint 2? | |
| C5 | Do you accept that consent/suitability controls stay **configurable** until your rule packs land, rather than hard-coded? | |
| C6 | Is any behaviour already built that you would consider **non-compliant today** and want stopped rather than scheduled? | |

> **C6 is the one question in this pack with the power to change the plan tonight.** If the
> answer is yes, it is a P1 override and it interrupts everything.

**Verdict:** ☐ APPROVE ☐ APPROVE WITH CONDITIONS ☐ REJECT ☐ ABSTAIN
**Conditions:**
**Signature / date:** _______________________

---

## 8. Business Analyst

**You approve:** requirement format · AC standard · RTM ownership · the S1 workload
**Your veto domain:** requirement quality and feasibility of the analysis pipeline

| # | Question you must answer | Your answer |
|:-:|--------------------------|-------------|
| B1 | Can you produce signed requirements with Gherkin AC for **one flow per sprint**, sustained? | |
| B2 | Is `BR-xxx` → PRD story → AC → `FUNC-xxx` the right ID chain, and does it fit the existing BRD overview structure? | |
| B3 | The RTM retro-fit for five built services — how long, and what do you need from engineering to do it? | |
| B4 | Is full-time allocation to Track A available to you, or are you split across other commitments? | |
| B5 | Which existing artefacts are reusable as-is (knowledge base, journey canvas, Figma), and which need rewriting to AC standard? | |

**Verdict:** ☐ APPROVE ☐ APPROVE WITH CONDITIONS ☐ REJECT ☐ ABSTAIN
**Conditions:**
**Signature / date:** _______________________

---

## 9. Optional reviewers

Recorded but not counted toward the eight required sign-offs.

| Role | Focus | Verdict | Conditions | Signature / date |
|------|-------|:-------:|-----------|------------------|
| **Ops / DevOps** | PZ.3 environments; gate 4.5 runbook; gate 4.6 perf smoke | | | |
| **UX / Digital** | `GAP-009` Figma→journey inventory; S2 screen reconciliation | | | |
| **Executive Sponsor** | PZ.1 scope signature; `GAP-010`; steering cadence | | | |

---

## 10. Consolidated result

Completed by the PO at the close of the session.

| Role | Verdict | Conditions carried into the plan |
|------|:-------:|----------------------------------|
| Solution Architect | | |
| Technical Head | | |
| Delivery Lead | | |
| Tech Lead | | |
| QA Lead | | |
| Security Architect | | |
| Compliance | | |
| Business Analyst | | |

**`CR-002` outcome:** ☐ APPROVED ☐ APPROVED WITH CONDITIONS ☐ REJECTED ☐ DEFERRED
**Recorded in:** [`governance/registers/DECISION-REGISTER.md`](../../../governance/registers/DECISION-REGISTER.md) §3
**Date:** _______________  **Recorded by:** _______________

> Per [14 §1](../../../governance/14-CHANGE_CONTROL.md#1-what-needs-a-change-request), a `GOV`
> change request is approved by **PO + Architect**; changes to stage exit criteria additionally
> require the **affected boards**. Security and Compliance verdicts on T4 items are never
> waivable. Conditions recorded above are binding on the plan — they are not advisory notes.
