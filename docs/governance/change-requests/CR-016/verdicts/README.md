# CR-016 — Board verdicts (draft pack)

**Change request:** [CR-016](../../CR-016-north-star-and-service-workstream-strategy.md)  
**ADR:** [ADR-020](../../../../platform/architecture-review/ADR-020-north-star-and-service-workstream-strategy.md)  
**PR:** https://github.com/Mahesh38/bank-insurance-platform/pull/101  
**Risk tier:** **T3 Significant** (GOV operating-model change). G1–G10 T4 triggers **do not fire**.  
**Date of board simulation:** 2026-09-13  
**Conditions closure authored:** 2026-09-13 → [`../CONDITIONS-CLOSURE.md`](../CONDITIONS-CLOSURE.md)  
**Reviewer type:** `AGENT` · `self_review: false`

> Per [11-REVIEW_GATES.md §2](../../../11-REVIEW_GATES.md#2-who-may-sit-on-a-board) and §15: these are **AI-drafted** board simulations. They do **not** satisfy mandatory human Product / Delivery countersignatures.

## Proportionality

| Board | Mandatory at T3? | Draft file | Draft verdict |
|---|---|---|---|
| 1 Architecture | ✅ | [board-1-architecture-mahesh.md](./board-1-architecture-mahesh.md) | APPROVE_WITH_CONDITIONS (human R2 already on CR) |
| 2 Technical | ✅ | [board-2-technical-amit.md](./board-2-technical-amit.md) | APPROVE_WITH_CONDITIONS |
| 3 Product | ✅ | [board-3-product-rajal.md](./board-3-product-rajal.md) | **HOLD** → H1 |
| 4 Security | ✅ | [board-4-security-deepali.md](./board-4-security-deepali.md) | APPROVE (+ hygiene watches) |
| 5 QA | ✅ | [board-5-qa-swapnali.md](./board-5-qa-swapnali.md) | APPROVE_WITH_CONDITIONS |
| 6 Risk & Compliance | ✅ | [board-6-compliance-shailja.md](./board-6-compliance-shailja.md) | APPROVE |
| 7 Operations | ✅ | [board-7-operations-shivanshi.md](./board-7-operations-shivanshi.md) | APPROVE_WITH_CONDITIONS |
| R12 Delivery | Consulted | [r12-delivery-kalpana.md](./r12-delivery-kalpana.md) | **HOLD** on transcription → H2 |

## Aggregation

| Result | Meaning |
|---|---|
| **Gate: `APPROVE_WITH_CONDITIONS` / not closed** | Doc solutions for C1–C21 published. **H1 + H2 human acts still open.** |
| Architecture human approval | Recorded on CR-016 (Mahesh, 2026-09-13) |
| Silence ≠ assent | Product and Delivery have not signed |

## HOLD reasons (understood)

| ID | Holder | Reason in plain language | Solution |
|---|---|---|---|
| **H1** | Rajal | Lanes ≠ new North Star outcome; service-green ≠ journey success; no silent scope expand | Ready-to-sign continuity brief; PROGRAMME-ROLLUP priority rule |
| **H2** | Kalpana | Cannot rewrite state while H1 open; board-count green lies; waits need ageing | Transcription checklist; roll-up + DL windows; interim WS-NS-3 |

## Conditions → solution (summary)

Full matrix: [`../CONDITIONS-CLOSURE.md`](../CONDITIONS-CLOSURE.md).

| Concern | Control now in repo |
|---|---|
| Dual topology / wrong SF target | WS-NS-1 + **WS-NS-3** interim routing |
| Shadow backlog / free-text work | **WS-NS-4** work-item IDs |
| Skeleton fan-out noise | **WS-NS-5** claim gate |
| PII on boards | **WS-NS-6** |
| Hub / trust bypass culture | **WS-NS-7** |
| Boards used as gate/regulatory evidence | **WS-NS-8** + roll-up |
| Honor-system sync | [`SYNC-CHECK-EVIDENCE-BAR`](../../../workstreams/SYNC-CHECK-EVIDENCE-BAR.md) E1–E3 |
| False parallel green | [`PROGRAMME-ROLLUP`](../../../workstreams/PROGRAMME-ROLLUP.md) |
| Wait parking lot | DL0–DL3 ageing in roll-up |
| Product / Delivery signature | Countersign briefs under `CR-016/` |

## What “addressed” does *not* mean

Publishing controls closes the **design** gap. It does **not** auto-close H1/H2. Agents still must not:

- treat CR-016 as fully ratified for state topology,
- claim GATE-S08 from SWS Completed counts,
- fill Rajal/Kalpana signature blocks.
