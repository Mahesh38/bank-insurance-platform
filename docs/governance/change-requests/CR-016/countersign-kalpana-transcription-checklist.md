# Kalpana countersign + transcription checklist (CR-016 / H2)

**To:** Kalpana — Delivery (R12)  
**From:** Mahesh — Architecture (R2), drafting the Delivery act for signature  
**Date:** 2026-09-13  
**Related:** [CR-016](../CR-016-north-star-and-service-workstream-strategy.md) · [CONDITIONS-CLOSURE](./CONDITIONS-CLOSURE.md) · HOLD **H2**  
**Rule PA-1:** You may force timing of Rajal’s H1; you may not invent Product content.

---

## 1. Preconditions (do not transcribe until true)

| # | Precondition | Evidence |
|---|---|---|
| P1 | Mahesh R2 Architecture approval on CR-016 | CR-016 §8 (present) |
| P2 | Rajal H1 = Option A/B complete, **or** Option C dated deferral | [countersign-rajal](./countersign-rajal-north-star-continuity.md) |
| P3 | Conditions C1–C21 published | [CONDITIONS-CLOSURE](./CONDITIONS-CLOSURE.md) |
| P4 | Sync-check bar + programme roll-up published | linked from strategy |

If P2 is missing: set a **required-by** date for Rajal (PA-1). Do **not** rewrite `workstreams:` yet.

---

## 2. What you are countersigning

1. Parallel `SWS-*` ownership is allowed under ADR-020.
2. Programme forecast uses [PROGRAMME-ROLLUP](../../workstreams/PROGRAMME-ROLLUP.md), not board-count green.
3. Waiting rows age on DL0–DL3 windows.
4. You will perform (or sponsor) the **human** `CURRENT-STATE.yaml` topology transcription per §3.
5. Transcription **does not** mark any GATE criterion MET.

---

## 3. Transcription checklist (human PR)

When P1–P4 hold, a human Delivery-owned PR should:

- [ ] Add programme lane language for `WS-NS` **or** document WS-3 as carrier of WS-NS (choose one scheme; prefer “WS-3 carries WS-NS” to minimise churn).
- [ ] Leave WS-1 / WS-2 / WS-3 historical ids intact as cluster labels / gate carriers.
- [ ] Reference ADR-020 + CR-016 in the workstream `authority` / notes fields — **do not** delete ADR-002 history.
- [ ] Run `python3 scripts/context/build-boot-capsule.py` (or repo equivalent) so BOOT matches state.
- [ ] Run FreshnessCheck + ci-checks; no stage_status / current_phase inventiveness.
- [ ] Explicit PR statement: “No GATE criterion moved to MET.”
- [ ] Link PROGRAMME-ROLLUP and SYNC-CHECK-EVIDENCE-BAR from workstreams README (if not already).

**Agents must not perform this transcription.**

---

## 4. Signature block (human only)

```text
Preconditions P1–P4:  MET / NOT MET (list gaps)
Countersign parallelization model:  YES / NO
Authorise transcription PR:  YES / NO / AFTER DATE __________
Name:     Kalpana
Role:     R12 Delivery
Date:     __________
Signature: __________
Required-by for Rajal H1 (if open): __________
```

Agents must not fill this block.
