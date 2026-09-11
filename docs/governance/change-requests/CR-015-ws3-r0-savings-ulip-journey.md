# CR-015 — Pull WS-3 R0 assisted Savings/ULIP journey sales forward (separate Product CR from CR-014)

**Date:** 2026-09-11  
**Type:** SCOPE (with ARCH and COMP consequences)  
**Raised by:** Stakeholder decision → recorded by agent. Human asked that WS-3 Savings/ULIP journey sales **not stay parked**.  
**Workstream:** WS-3 (primary) · WS-1 adapter already admitted by [`CR-014`](./CR-014-ws1-life-lob-adapter-standards.md)  
**Stage:** WS-3 S08 — Engineering Foundation (L4) · proposed target for *implementation* remains S11 vertical slice  
**Decision:** **CANDIDATE — not transcribed.** HALT (CS-1) forbids ADMIT; [14 §3](../14-CHANGE_CONTROL.md#3-procedure) forbids implementing what this CR contemplates until APPROVED. Human T4 Product / Architecture / Security / Risk & Compliance signatures outstanding.  
**Origin:** `SUG-20260911-uls` `ESCALATED` (09 §8 unpark requested; ADMIT-BYPASS **not** executed because FreshnessCheck exit 2)  
**Proposed epic:** `EPIC-004` (not minted until this CR is APPROVED and Kalpana / R12 refreshes `CURRENT-STATE.yaml`)

---

## 1. Current position

`R0-SCOPE.md` A1 already names Life = Term, ULIP, Savings/Investment. The **current increment** does not.

| Authority | What it says today |
|---|---|
| [`CURRENT-STATE.yaml`](../state/CURRENT-STATE.yaml) WS-3 `current_objective` | `R0-ASSISTED-TERM-SALE` — one RM, one **Term Life** policy |
| WS-3 `current_scope.in_scope` | Product catalogue = “R0 matrix only: Life, Group A, **Term**” |
| WS-3 `out_of_scope` | “ULIP and Savings/Endowment product classes” — revisit at R1 |
| WS-1 `out_of_scope` | “WS-3 R0 product catalogue or RM journey for Savings/ULIP sales — revisit at R1 — adapter readiness does not admit R0 journey sales” |
| [`WS-3-PLATFORM-CHARTER.md` §3.2](../workstreams/WS-3-PLATFORM-CHARTER.md) | Same ULIP/Savings row, revisit R1 |
| [`BOOT.md` §5](../../context/BOOT.md) | Generated from the state file; same exclusion |
| [`CR-014` §3](./CR-014-ws1-life-lob-adapter-standards.md#3-what-this-cr-does-not-do) | Explicitly **does not** expand WS-3 R0 catalogue or RM journey — “until a **separate Product CR**” |
| Parked | [`SUG-20260821-jx2`](../registers/PARKED-BACKLOG.md) ULIP/Savings JES · [`SUG-20260907-fig`](../registers/PARKED-BACKLOG.md) Savings/ULIP picker · [`SUG-20260904-eng`](../registers/PARKED-BACKLOG.md) 7-layer / 206-product engine |

CR-014 admitted the **supplier adapter** (WS-1 `EPIC-002`). Sandbox quotes for `lob=SAVING` and `lob=ULIP` already return priced ULIP offers. That is not an R0 assisted **sale** through the RM Workspace, catalogue, suitability gate, NIP BFF, or Flutter surface.

On 2026-09-11 the human restated GATE-P4 / 4.1 continuation **and** directed: WS-3 Savings/ULIP journey sales **does not stay parked; we need to work on it.**

---

## 2. Proposed change

If APPROVED (Rajal R1 + Mahesh R2), pull **only** the assisted RM Savings/ULIP **sale path** into R0 product scope, and mint `EPIC-004`:

| # | Change |
|---|--------|
| 1 | **R0 product classes** for the assisted journey: Life × Group A × {Term, Savings, ULIP}. Catalogue matrix expands from “Term only” to those three classes. |
| 2 | **Journey Execution Specification** for RM-assisted Savings and ULIP sales, carved out of parked `SUG-20260821-jx2` (that bag otherwise stays parked). |
| 3 | **NIP BFF / RM contract deltas** for Savings/ULIP lead + quote + proposal (extends `EPIC-003`; no Health picker, no meeting scheduler). |
| 4 | **RM Workspace + Flutter** assisted Savings/ULIP sale screens — **S11**, not S08 feature breadth. Blocked on GATE-S08 and on GAP-006 / GAP-007 (Rajal C5). |
| 5 | Standing constraints **unchanged**: suitability before quote, customer-device OTP consent, customer-device payment, RECONCILED before Policy Sold, no Flutter → 1SB, no Flutter OAuth tokens. |

After APPROVED, a human updates `CURRENT-STATE.yaml` **scope** fields (not `current_phase` / `stage_status`), regenerates `BOOT.md`, and Delivery runs the [08 §5](../08-BACKLOG_RULES.md#5-unparking) unpark sweep. Agents must not transcribe those fields while HALT is in force.

---

## 3. What this CR does not do

- Edit `current_phase` or `stage_status`.
- Lift AIGEM HALT or refresh `state_as_of` / `review_due` (Kalpana / R12).
- Transcribe scope into `CURRENT-STATE.yaml` in this candidate (unlike CR-013 / CR-014, which transcribed under ADMIT-BYPASS when freshness was WARN, not HALT).
- Mint `EPIC-004` or start Flutter / BFF / catalogue / JES implementation before APPROVED.
- Change the first R0 pilot objective: **one Term Life sale remains the GATE-S11 proving path.** Savings/ULIP are additional R0 assisted paths, not a replacement for Term.
- Admit Health, Motor, Travel, DIY, hybrid mode-switch, Group B redirect, Customer BFF, or the 7-layer / 206-product suitability engine (`SUG-20260904-eng` stays parked).
- Unpark Figma Health picker or post-create meeting scheduler (remain on `SUG-20260907-fig`).
- Waive GATE-S08, GATE-P4 Term UAT, coverage, ArchUnit, or consent/suitability hard gates.
- Approve human T4 signatures by AI simulation.

---

## 4. Driver

**Business priority change** (stakeholder, 2026-09-11): the WS-3 assisted journey must sell Savings and ULIP, not only Term. CR-014 already made the 1SB supplier Life-ready; parking the bank-side journey is withdrawn for this slice.

“It would be better to keep Term-only until a pilot sale” is the alternative this CR exists to overturn. It is not a regulatory mandate (not SC4-external). It is a Product increment change, which [14 §1](../14-CHANGE_CONTROL.md#1-what-needs-a-change-request) routes to PO + Architect.

---

## 5. Relationship to parked items

| Parked item | Effect if this CR is APPROVED |
|---|---|
| `SUG-20260821-jx2` ULIP/Savings JES | **Split.** Assisted Savings/ULIP JES → proposed `EPIC-004`. DIY, hybrid, Group B, Health/Motor/Travel, renewals/servicing **stay parked**. |
| `SUG-20260907-fig` Savings/ULIP product picker | **Split.** Savings/ULIP picker → proposed `EPIC-004`. ULIP-leads tab as a distinct inbox, Health picker, meeting scheduler **stay parked**. |
| `SUG-20260904-eng` 7-layer / 206-product engine | **Unchanged / still parked.** R0 uses the existing suitability pack + an expanded R0 matrix, not the research engine. |
| `SUG-20260903-lif` / CR-014 `not_included` | **Satisfied for the excluded bag** by this CR. Adapter work is not re-opened. |
| E12 Annuity / Pension | **Unchanged.** |

Until APPROVED, every row above **remains in** [`PARKED-BACKLOG.md`](../registers/PARKED-BACKLOG.md). Unpark requested is not unpark completed ([08 §5](../08-BACKLOG_RULES.md#5-unparking): re-triage, never auto-admit).

---

## 6. Impact analysis (pipeline steps 2–8 as if approved)

Per [14 §3](../14-CHANGE_CONTROL.md#3-procedure). This is **not** an admission.

| Step | If approved |
|---|---|
| Stage fit | WS-3 S08: documentation/JES/contracts become SF2 absorbable-adjacent (same pattern as `EPIC-003`). Flutter/BFF **implementation** stays SF3 until S11. S08 feature-breadth bar is not waived. |
| Scope fit | SC0 after transcription: R0-SCOPE A1 + this CR. WS-1 out_of_scope row for “WS-3 R0 … Savings/ULIP sales” is removed. |
| Necessity | MUST at R0 product scope (stakeholder). Without this CR: NOT-NOW at S08 (GATE-S08 does not fail today). |
| Classification | FUNC + ARCH + DOC + COMP · EPIC · **T4** (R0 LOB, suitability/consent, RM journey) |
| Priority | P2 now (contracts/JES while S08 is open) · **P1 at S11** |
| Dependencies | See §8. Implementation is `PARKED-DEPENDENT` on GATE-S08, GAP-006, GAP-007. |
| Effort | L (JES + contracts + catalogue matrix + S11 screens). Not S. |
| Stage dates | GATE-S08 criteria unchanged. GATE-S11 acceptance grows (additional LOB paths). |
| Risk if rejected | Adapter is Life-ready with no bank journey that can sell Savings/ULIP; the 2026-09-11 instruction is parked again and will recur. |

**Documents to update only after APPROVED:** `CURRENT-STATE.yaml` scope (not stage fields) · `BOOT.md` (generated) · `WS-3-PLATFORM-CHARTER.md` §3 · `R0-SCOPE.md` increment notes · `PARKED-BACKLOG.md` sweep · `EPIC-002` `not_included` pointer · new `EPIC-004.work-item.yaml`.

---

## 7. Authority

| Role | Action |
|---|---|
| Stakeholder / human override | Requested unpark and immediate work ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)). HALT blocked ADMIT-BYPASS transcription. |
| **Rajal (R1)** | **Approver** — R0 LOB / journey increment ([card](../../context/personas/rajal-product.card.md)) |
| **Mahesh (R2)** | **Approver** — structure, catalogue vs journey boundary, S08 vs S11 split |
| Principal BA (R11) | JES and AC quality for Savings/ULIP (Product delegate) |
| Amit (R3) | NIP BFF / RM engineering at S11; does not start S08 feature breadth |
| Deepali (R8) | T4 if new trust-boundary or PII surfaces on the journey |
| Shailja (R9) | T4 — ULIP/Savings suitability + consent vs Term; no waiver of hard gates |
| Swapnali (R7) | Journey evidence at S11; GATE-P4 Term criteria stay |
| Shivanshi (R10) | No new platform dependency expected at S08 |
| Kalpana (R12) | Refresh `CURRENT-STATE.yaml` freshness **before any ADMIT**; does not convert this CR into approval |

**Bypass risk (one sentence):** pulling second and third Life product classes into R0 before a Term pilot sale widens the S11 slice and the suitability/consent surface while GATE-S08 is still 0 of 10 closed, and T4 boards have not sat.

---

## 8. Proposed work breakdown (not minted)

Ordered for enablement **after** APPROVED + freshness refresh:

1. **DOC** — Savings/ULIP assisted JES (carve from `SUG-20260821-jx2`)
2. **ARCH** — NIP BFF / RM OpenAPI deltas for Savings/ULIP (extends `EPIC-003`; no Health/meetings)
3. **FUNC** — R0 catalogue matrix Life / Group A / Term+Savings+ULIP (not `SUG-20260904-eng`)
4. **FUNC** — S11 RM Workspace + Flutter sale path (HARD-blocked on GATE-S08, GAP-006, GAP-007)
5. **QA** — assisted Savings/ULIP journey evidence; Term path non-regression

WS-1 remaining `EPIC-002` gaps (Single Quote pin, master-lookup path, proposal dynamic form) stay on **GATE-P4 / 4.1**. They are supplier prerequisites, not this CR’s implementation.

---

## 9. Alternatives considered

| Option | Consequence |
|---|---|
| Do nothing (keep R1 park) | Contradicts the 2026-09-11 instruction; CR-014’s “separate Product CR” remains unraised |
| ADMIT-BYPASS and transcribe now (CR-013/014 pattern) | Violates CS-1 HALT (`review_due` 2026-09-09 passed; `state_as_of` 32 days old) |
| Implement Flutter/JES in this turn | Violates [09 one-rule](../09-AI_EXECUTION_RULES.md) (never implement in the turn raised) and [14 §3](../14-CHANGE_CONTROL.md#3-procedure) |
| Treat as duplicate of `SUG-20260903-lif` | Wrong bag: `-lif` **excluded** this work; CS-2 requires a new SUG for the excluded slice |
| Expand to Health + 206-product engine | Fails minimality; those stay parked |

---

## 10. Decision

```yaml
change_request:
  id: CR-015
  raised_by: "agent:cursor-grok (record of human:stakeholder 2026-09-11)"
  date: 2026-09-11
  type: SCOPE
  decision: CANDIDATE
  approvers: ["Rajal (R1)", "Mahesh (R2)"]
  decided_on: null
  conditions:
    - "Do not transcribe CURRENT-STATE.yaml scope while HALT (CS-1) is in force"
    - "Do not mint EPIC-004 or implement journey code until APPROVED"
    - "Kalpana / R12 refreshes state_as_of and review_due before any ADMIT"
    - "Human T4 Architecture, Security, Risk & Compliance before production use of Savings/ULIP sales"
    - "Term remains the first R0 proving sale (R0-ASSISTED-TERM-SALE is not replaced)"
```
