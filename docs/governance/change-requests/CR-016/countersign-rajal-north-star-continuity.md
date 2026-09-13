# Rajal countersign brief — North Star objective continuity (CR-016 / H1)

**To:** Rajal — Product (R1)  
**From:** Mahesh — Architecture (R2), drafting the Product decision for signature  
**Date:** 2026-09-13  
**Decision type:** Countersign of operating-model CR — **does not** change journey scope by itself  
**Related:** [CR-016](../CR-016-north-star-and-service-workstream-strategy.md) · [CONDITIONS-CLOSURE](./CONDITIONS-CLOSURE.md) · HOLD **H1**

---

## 1. What Architecture already approved

CR-016 / ADR-020: **how agents organise work** (one programme lane + per-service ownership boards + sync checks).

Architecture did **not** approve a new product outcome, LOB, channel, or DIY expansion.

---

## 2. What we need from Product

Please choose **one**:

### Option A — Continuity (recommended)

Countersign that the North Star **outcome text remains**:

> **`R0-ASSISTED-LIFE-SALE`** — One RM sells a complete Life insurance policy — Term or Savings/ULIP — to one ETB customer from one Group A insurer, end to end, through a real interface, with consent and suitability evidence, payment on the customer's own device, an issued and reconciled policy, and a complete audit trail. R0 includes both Term and Savings/ULIP assisted paths (CR-015). DIY and hybrid stay sequenced behind the assisted journey.

Source: `docs/governance/state/CURRENT-STATE.yaml` (WS-3 `current_objective`).

### Option B — Amend

Provide replacement objective text and the CR/ADR that authorises the wording change. Operating lanes still apply; transcription waits on your amended text.

### Option C — Explicit deferral

Date-stamp a deferral (≤ 5 working days recommended) stating objective text is unchanged by silence, and Kalpana may proceed to prepare transcription **without** treating silence as a new outcome. (Still not Product approval of scope expansion.)

---

## 3. Product conditions you are asked to affirm (C6–C9)

1. Service board “Completed” ≠ product acceptance of a journey.
2. Any `SWS-*` Active item that changes customer/RM-visible behaviour needs a Product-admitted work item.
3. When local SWS priority conflicts with the ordered North Star slice, **programme priority wins** ([PROGRAMME-ROLLUP](../../workstreams/PROGRAMME-ROLLUP.md)).
4. CR-016 does not expand LOB / channel / DIY / hybrid into the current increment.

---

## 4. Signature block (human only)

```text
Decision:  A Continuity  |  B Amend (attach text)  |  C Defer until __________
Name:     Rajal
Role:     R1 Product
Date:     __________
Signature: __________
Notes:    __________
```

Agents must not fill this block.
