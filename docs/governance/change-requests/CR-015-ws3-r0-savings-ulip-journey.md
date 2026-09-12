# CR-015 — Serve the complete assisted Life journey in R0 (Term + Savings/ULIP end to end)

**Date:** 2026-09-11 (raised) · restated 2026-09-12  
**Type:** SCOPE (with ARCH and COMP consequences)  
**Raised by:** Stakeholder decision → recorded by agent under human override ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process))  
**Workstream:** WS-3 (primary) · WS-1 adapter already admitted by [`CR-014`](./CR-014-ws1-life-lob-adapter-standards.md)  
**Stage:** WS-3 S08 — Engineering Foundation (L4) · *implementation* of sale screens remains S11  
**Decision:** **CANDIDATE — transcribed into WS-3 scope artefacts under ADMIT-BYPASS.** Human T4 Product / Architecture / Security / Risk & Compliance signatures outstanding. Listing Rajal and Mahesh as approvers is the required-approver set, **not** a HUMAN verdict.  
**Origin:** `SUG-20260911-uls` `ADMIT-BYPASS` (recurrence 2026-09-12: complete assisted Life e2e)  
**Epic:** [`EPIC-004`](../../platform/ws3-platform/EPIC-004.work-item.yaml)

---

## 1. Current position (before this transcription)

`R0-SCOPE.md` A1 already names Life = Term, ULIP, Savings/Investment. The **current increment** did not.

| Authority | What it said before transcription |
|---|---|
| [`CURRENT-STATE.yaml`](../state/CURRENT-STATE.yaml) WS-3 `current_objective` | `R0-ASSISTED-TERM-SALE` — one RM, one **Term Life** policy |
| WS-3 `current_scope.in_scope` | Product catalogue = “R0 matrix only: Life, Group A, **Term**” |
| WS-3 `out_of_scope` | “ULIP and Savings/Endowment product classes” — revisit at R1 |
| WS-1 `out_of_scope` | “WS-3 R0 product catalogue or RM journey for Savings/ULIP sales — revisit at R1 — adapter readiness does not admit R0 journey sales” |
| [`WS-3-PLATFORM-CHARTER.md` §3.2](../workstreams/WS-3-PLATFORM-CHARTER.md) | Same ULIP/Savings row, revisit R1 |
| [`CR-014` §3](./CR-014-ws1-life-lob-adapter-standards.md#3-what-this-cr-does-not-do) | Explicitly **did not** expand WS-3 R0 catalogue or RM journey — “until a **separate Product CR**” |
| Parked | [`SUG-20260821-jx2`](../registers/PARKED-BACKLOG.md) ULIP/Savings JES · [`SUG-20260907-fig`](../registers/PARKED-BACKLOG.md) Savings/ULIP picker · [`SUG-20260904-eng`](../registers/PARKED-BACKLOG.md) 7-layer / 206-product engine |

CR-014 admitted the **supplier adapter** (WS-1 `EPIC-002`). Sandbox quotes for Saving and ULIP already return priced ULIP offers. That is not an R0 assisted **sale** through the RM Workspace, catalogue, suitability gate, NIP BFF, or Flutter surface.

On 2026-09-11 the human directed: WS-3 Savings/ULIP journey sales **does not stay parked**. HALT (CS-1) blocked transcription that day. Kalpana / R12 refreshed freshness on 2026-09-11 (`state_as_of` 2026-09-11, `review_due` 2026-10-11); FreshnessCheck is FRESH.

On 2026-09-12 the human restated CR-015 as `decision: CANDIDATE` with required approvers Rajal (R1) and Mahesh (R2), and widened the product intent: **serve the complete Life insurance assisted journey in R0, including Term and Saving/ULIP end to end.**

---

## 2. Proposed change (transcribed)

Transcribe into **WS-3 current scope** (not stage fields), replace the Term-only proving objective with a complete assisted Life objective, and mint `EPIC-004`:

| # | Change |
|---|--------|
| 1 | **R0 objective** becomes `R0-ASSISTED-LIFE-SALE`: one RM sells a complete Life policy — **Term or Savings/ULIP** — to one ETB customer from one Group A insurer, end to end (lead → suitability → quote → proposal → customer-device payment → RECONCILED → issued policy + audit trail). Term is **one** of the R0 assisted paths, not the only one. |
| 2 | **R0 product classes** for the assisted journey: Life × Group A × {Term, Savings, ULIP}. Catalogue matrix expands from “Term only” to those three classes. |
| 3 | **Journey Execution Specification** for RM-assisted Savings and ULIP sales, carved out of parked `SUG-20260821-jx2` (that bag otherwise stays parked). Term JES remains `SUG-20260821-jx1`. |
| 4 | **NIP BFF / RM contract deltas** for Savings/ULIP lead + quote + proposal (extends `EPIC-003`; no Health picker, no meeting scheduler). |
| 5 | **RM Workspace + Flutter** assisted Term **and** Savings/ULIP sale screens — **S11**, not S08 feature breadth. Blocked on GATE-S08 and on GAP-006 / GAP-007 (Rajal C5). |
| 6 | Standing constraints **unchanged**: suitability before quote, customer-device OTP consent, customer-device payment, RECONCILED before Policy Sold, no Flutter → 1SB, no Flutter OAuth tokens, no `distributorId` from the caller. |

This transcription is **09 §8 ADMIT-BYPASS of a CANDIDATE CR**, the same pattern as CR-013 / CR-014. It is **not** APPROVED. Agents must not manufacture HUMAN T4 signatures. `decided_on` stays `null`; `stakeholder_direction_date` is 2026-09-12.

---

## 3. What this CR does not do

- Edit `current_phase` or `stage_status`. GATE-S08 stays OPEN.
- Convert `CANDIDATE` into `APPROVED`, or treat listing Rajal / Mahesh in YAML as a board signature.
- Implement Flutter / BFF / catalogue / JES **code** in the turn the restatement arrived ([09 one-rule](../09-AI_EXECUTION_RULES.md); [14 §3](../14-CHANGE_CONTROL.md#3-procedure) until HUMAN APPROVED).
- Admit Health, Motor, Travel, DIY, hybrid mode-switch, Group B redirect, Customer BFF, or the 7-layer / 206-product suitability engine (`SUG-20260904-eng` stays parked).
- Unpark Figma Health picker, ULIP-leads inbox tab, or post-create meeting scheduler (remain on `SUG-20260907-fig`).
- Waive GATE-S08, GATE-P4 Term UAT, coverage, ArchUnit, or consent/suitability hard gates.
- Replace GATE-P4 Term UAT exit criteria. Term remains a required proving path; it is no longer the *only* R0 Life class.
- Approve human T4 signatures by AI simulation.

---

## 4. Driver

**Business priority change** (stakeholder, 2026-09-11; restated 2026-09-12): R0 must serve the **complete assisted Life journey**, Term **and** Saving/ULIP, end to end. CR-014 already made the 1SB supplier Life-ready; parking the bank-side journey is withdrawn for this slice. The earlier “Term remains the only proving sale” alternative is overturned.

This is a Product increment change, which [14 §1](../14-CHANGE_CONTROL.md#1-what-needs-a-change-request) routes to PO + Architect. It is not a regulatory mandate (not SC4-external).

---

## 5. Relationship to parked items

| Parked item | Effect of this transcription |
|---|---|
| `SUG-20260821-jx2` ULIP/Savings JES | **Split.** Assisted Savings/ULIP JES → `EPIC-004` / `DOC-022`. DIY, hybrid, Group B, Health/Motor/Travel, renewals/servicing **stay parked**. Row is not deleted. |
| `SUG-20260907-fig` Savings/ULIP product picker | **Split.** Savings/ULIP picker → `EPIC-004` / `FUNC-021` (S11). ULIP-leads tab as a distinct inbox, Health picker, meeting scheduler **stay parked**. Row is not deleted. |
| `SUG-20260904-eng` 7-layer / 206-product engine | **Unchanged / still parked.** R0 uses the existing suitability pack + an expanded R0 matrix, not the research engine. |
| `SUG-20260903-lif` / CR-014 `not_included` | **Satisfied for the excluded WS-3 bag** by this CR. Adapter work is not re-opened. |
| E12 Annuity / Pension | **Unchanged.** |

Unpark of a parked row is re-triage, never auto-admit ([08 §5](../08-BACKLOG_RULES.md#5-unparking)). The carved slices move to `EPIC-004`; the remainder of each bag stays in [`PARKED-BACKLOG.md`](../registers/PARKED-BACKLOG.md).

---

## 6. Impact analysis (pipeline steps 2–8)

Per [14 §3](../14-CHANGE_CONTROL.md#3-procedure). Transcription is ADMIT-BYPASS of CANDIDATE scope, not HUMAN APPROVED.

| Step | After transcription |
|---|---|
| Stage fit | WS-3 S08: documentation/JES/contracts are SF2 absorbable-adjacent (same pattern as `EPIC-003`). Flutter/BFF **implementation** stays SF3 until S11. S08 feature-breadth bar is not waived. |
| Scope fit | SC0 after transcription: R0-SCOPE A1 + this CR. WS-1 out_of_scope row for “WS-3 R0 … Savings/ULIP sales” is removed. |
| Necessity | MUST at R0 product scope (stakeholder 2026-09-12). |
| Classification | FUNC + ARCH + DOC + COMP · EPIC · **T4** (R0 LOB, suitability/consent, RM journey) |
| Priority | P2 now (contracts/JES while S08 is open) · **P1 at S11** |
| Dependencies | Implementation is `PARKED-DEPENDENT` on GATE-S08, GAP-006, GAP-007. |
| Effort | L (JES + contracts + catalogue matrix + S11 screens). Not S. |
| Stage dates | GATE-S08 criteria unchanged. GATE-S11 acceptance grows (Term + Savings/ULIP assisted paths). |
| Risk if rejected | Adapter is Life-ready with no bank journey that can sell Savings/ULIP; the 2026-09-12 instruction is parked again and will recur. |

**Documents updated by this transcription:** `CURRENT-STATE.yaml` scope and objective (not stage fields) · `BOOT.md` (generated) · `WS-3-PLATFORM-CHARTER.md` §2.2 / §3 · `01-CURRENT_STATE.md` · `R0-SCOPE.md` increment note · `PARKED-BACKLOG.md` sweep · `EPIC-002` `not_included` pointer · new `EPIC-004.work-item.yaml`.

---

## 7. Authority

| Role | Action |
|---|---|
| Stakeholder / human override | Requested unpark (2026-09-11) and restated complete assisted Life e2e (2026-09-12). [09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process) authorises ADMIT-BYPASS transcription now that HALT is lifted. Does **not** satisfy T4. |
| **Rajal (R1)** | **Approver** — R0 LOB / journey increment ([card](../../context/personas/rajal-product.card.md)). HUMAN verdict outstanding. |
| **Mahesh (R2)** | **Approver** — structure, catalogue vs journey boundary, S08 vs S11 split. HUMAN T4 outstanding. |
| Principal BA (R11) | JES and AC quality for Savings/ULIP (Product delegate) |
| Amit (R3) | NIP BFF / RM engineering at S11; does not start S08 feature breadth |
| Deepali (R8) | T4 if new trust-boundary or PII surfaces on the journey |
| Shailja (R9) | T4 — ULIP/Savings suitability + consent vs Term; no waiver of hard gates |
| Swapnali (R7) | Journey evidence at S11; GATE-P4 Term criteria stay |
| Shivanshi (R10) | No new platform dependency expected at S08 |
| Kalpana (R12) | Freshness already refreshed 2026-09-11. Does not convert this CR into approval. |

**Bypass risk (one sentence):** pulling second and third Life product classes into R0 before a Term pilot sale widens the S11 slice and the suitability/consent surface while GATE-S08 is still 0 of 10 closed, and T4 boards have not sat.

---

## 8. Admitted actions (work breakdown)

See [`EPIC-004`](../../platform/ws3-platform/EPIC-004.work-item.yaml). Ordered for enablement. **Docs/contracts may start at S08; screens stay S11.**

1. **DOC-022** — Savings/ULIP assisted JES (carve from `SUG-20260821-jx2`)
2. **ARCH-024** — NIP BFF / RM OpenAPI deltas for Savings/ULIP (extends `EPIC-003`; no Health/meetings)
3. **FUNC-020** — R0 catalogue matrix Life / Group A / Term+Savings+ULIP (not `SUG-20260904-eng`)
4. **FUNC-021** — S11 RM Workspace + Flutter sale path for Term **and** Savings/ULIP (HARD-blocked on GATE-S08, GAP-006, GAP-007)
5. **QA-013** — assisted Savings/ULIP journey evidence; Term path non-regression

WS-1 remaining `EPIC-002` gaps (Single Quote pin, master-lookup path, proposal dynamic form) stay on **GATE-P4 / 4.1**. They are supplier prerequisites, not this CR’s implementation.

---

## 9. Alternatives considered

| Option | Consequence |
|---|---|
| Keep Term-only proving sale | Contradicts the 2026-09-12 instruction (“complete life insurance assisted journey … term and saving/ulip end2end”) |
| Leave CR-015 untranscribed after HALT lift | Contradicts 09 §8; the restatement is an ADMIT-BYPASS of a CANDIDATE already raised |
| Treat YAML `approvers` + `decided_on: 2026-09-12` as HUMAN APPROVED | Manufactures a T4 signature (CC-1, 11 §2). Forbidden. |
| Implement Flutter/JES in this turn | Violates [09 one-rule](../09-AI_EXECUTION_RULES.md) and [14 §3](../14-CHANGE_CONTROL.md#3-procedure) |
| Mint a new SUG | CS-2: this is a recurrence of `SUG-20260911-uls` |
| Expand to Health + 206-product engine + DIY/hybrid | Fails minimality; those stay parked |

---

## 10. Decision

```yaml
change_request:
  id: CR-015
  raised_by: "agent:cursor-grok (record of human:stakeholder 2026-09-11)"
  date: 2026-09-12
  type: SCOPE
  decision: CANDIDATE
  approvers: ["Rajal (R1)", "Mahesh (R2)"]
  decided_on: null
  stakeholder_direction_date: "2026-09-12"
  transcribed_on: "2026-09-12"
  conditions:
    - "CANDIDATE is transcribed into scope artefacts; it is not APPROVED"
    - "Listing Rajal (R1) and Mahesh (R2) as approvers is not a HUMAN T4 signature"
    - "Do not edit current_phase or stage_status; GATE-S08 stays OPEN"
    - "Do not implement Flutter / BFF / catalogue / JES code until HUMAN APPROVED"
    - "Human T4 Architecture, Security, Risk & Compliance before production use of Savings/ULIP sales"
    - "DIY, hybrid, Health, Group B, Customer BFF, and the 7-layer / 206-product engine stay parked"
    - "Standing constraints unchanged: suitability before quote; customer-device OTP consent; customer-device payment; RECONCILED before Policy Sold"
```
