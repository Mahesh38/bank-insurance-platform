# CR-016 — Board verdicts (draft pack)

**Change request:** [CR-016](../../CR-016-north-star-and-service-workstream-strategy.md)  
**ADR:** [ADR-020](../../../../platform/architecture-review/ADR-020-north-star-and-service-workstream-strategy.md)  
**PR:** https://github.com/Mahesh38/bank-insurance-platform/pull/101  
**Risk tier:** **T3 Significant** (GOV operating-model change). G1–G10 T4 triggers **do not fire** — no authn/authz, PII, secrets, crypto, money, consent, migration, production topology, public contract, or regulator-control delta.  
**Date of board simulation:** 2026-09-13  
**Reviewer type:** `AGENT` · `self_review: false` (review of a human-directed Architecture decision already recorded)

> Per [11-REVIEW_GATES.md §2](../../../11-REVIEW_GATES.md#2-who-may-sit-on-a-board) and §15: these are **AI-drafted** board simulations. They do **not** satisfy mandatory human Product / Delivery countersignatures already named on CR-016, and they do not manufacture T4 signatures.

## Proportionality

| Board | Mandatory at T3? | Draft file |
|---|---|---|
| 1 Architecture | ✅ | [board-1-architecture-mahesh.md](./board-1-architecture-mahesh.md) |
| 2 Technical | ✅ | [board-2-technical-amit.md](./board-2-technical-amit.md) |
| 3 Product | ✅ | [board-3-product-rajal.md](./board-3-product-rajal.md) |
| 4 Security | ✅ | [board-4-security-deepali.md](./board-4-security-deepali.md) |
| 5 QA | ✅ | [board-5-qa-swapnali.md](./board-5-qa-swapnali.md) |
| 6 Risk & Compliance | ✅ | [board-6-compliance-shailja.md](./board-6-compliance-shailja.md) |
| 7 Operations | ✅ | [board-7-operations-shivanshi.md](./board-7-operations-shivanshi.md) |
| R12 Delivery (not a board) | Consulted — owns transcription | [r12-delivery-kalpana.md](./r12-delivery-kalpana.md) |

## Aggregation (Rule RG aggregation)

| Result | Meaning |
|---|---|
| **Gate draft: `APPROVE_WITH_CONDITIONS` / not closed** | No board issued `REWORK` or `REJECT`. Two **blocking human conditions** remain open: Rajal R1 countersign and Kalpana R12 countersign + `CURRENT-STATE.yaml` transcription. |
| Architecture human approval | Already recorded on CR-016 (Mahesh, 2026-09-13) — this pack does not re-approve it |
| Silence ≠ assent | Product and Delivery have not sat; their outstanding countersigns keep the gate open |

### Cross-cutting concerns (union of `must_fix` / watch items)

1. **Dual topology risk** — agents may treat `SWS-*` as lifecycle workstreams or invent a second backlog beside product/SSOT backlogs.
2. **North Star ownership** — programme objective wording remains Product’s; Architecture approved *lanes*, not a new outcome statement.
3. **False parallel green** — 21 boards can look busy while the critical path (Kalpana) and journey evidence (Swapnali) stall.
4. **Sync-check honesty** — markdown sync logs are honor-system until QA defines a minimum evidence bar.
5. **Trust-boundary drift** — ownership lanes must not excuse Hub bypass or bank→1SB/DB direct calls (Deepali / Mahesh standing constraints).
6. **State transcription not done** — until R12 writes `workstreams:` topology, BOOT/agents still see WS-1/2/3 only.
7. **GOV capacity** — board hygiene cost vs S08-G10 / GATE-S08 closure (BR-4 / GC-1 already named on CR).

### Recommended next human moves

| Owner | Action | Suggested window |
|---|---|---|
| Rajal (R1) | Countersign or amend North Star objective continuity | Kalpana may set required-by |
| Kalpana (R12) | Countersign parallelization model; transcribe topology when both R1+R2 conditions met | After R1 |
| Swapnali (R7) | Define minimum evidence for “sync check passed” | Before agents claim cross-SWS Done at scale |
| Mahesh (R2) | Clarify ADR-002 vs ADR-020 language if Product disputes “fourth workstream” reading | If challenged |
