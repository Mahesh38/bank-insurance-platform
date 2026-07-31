# Process & journey canvas — AU Bank Insurance Platform

**Owner:** Business Analysis  
**Status:** Draft skeleton — **must be validated against Figma + bank SMEs**  
**Rule:** This is AU Bank process language. Aggregator steps are “external capability”, not the product.

---

## 1. Universal sale journey (working model)

Use this as the discussion spine until Figma proves otherwise.

```text
[A] RM login & context
        │
        ▼
[B] Customer find (CIF) + consent / disclosures
        │
        ▼
[C] Need analysis / suitability (bank-owned)
        │
        ▼
[D] Product / LOB selection
        │
        ▼
[E] Quote request → wait → offers
        │
        ▼
[F] Compare & select offer (+ literature / BI if any)
        │
        ▼
[G] Proposal form (dynamic) → submit → wait
        │
        ▼
[H] Underwriting / requirements (docs, OTP, KYC…)  ⟵ often multi-visit
        │
        ▼
[I] Customer decision / counter-offer (if any)
        │
        ▼
[J] Payment handoff → confirmation
        │
        ▼
[K] Policy issued → fulfilment / vault
```

---

## 2. Stage card template (fill per stage in Session 2)

For each stage A–K:

| Field | Content |
|-------|---------|
| Stage id / name | |
| Actor (RM / Customer / System / Ops) | |
| Trigger | |
| Happy path | |
| Failure / alternate | |
| Data needed (bank) | |
| Data needed (external gateway) | |
| Compliance / disclosure | |
| Figma screen id(s) | *TBD after inventory* |
| MVP? (Yes / Later / Out) | |

---

## 3. MVP cut hypothesis (reopen in Session 2)

| Stage | Hypothesis for R0 | Rationale (draft) |
|-------|-------------------|-------------------|
| A–C | **Yes** | Bank DNA of the sale |
| D–G | **Yes** | Core commercial path |
| H | **Partial / Later** | Often heavy; may soft-launch with status-only |
| I | **Later** unless product requires | Complexity |
| J | **Yes** if D-007 includes payment | Common pilot bar |
| K | **Yes** visibility; vault depth Later | Proof of completion |

---

## 4. Figma → stage mapping (blank — fill after walkthrough)

| Figma frame / screen name | Node / link | Journey stage | MVP? | Notes / gaps |
|---------------------------|-------------|---------------|------|--------------|
| *Awaiting inventory* | [Prototype](https://www.figma.com/proto/JyLGAaO88ELjnyVF2FQ3Bx/For-Client-Review?node-id=208-9666&page-id=208%3A2982) | | | Login-gated; needs export or live walkthrough |

---

## 5. Swimlanes (who owns what — draft)

| Concern | AU Bank owns | External gateway (e.g. 1SB) | Insurer |
|---------|--------------|-----------------------------|---------|
| Customer identity | Yes (CIF) | No | No |
| Suitability content | Yes | No (unless used as helper) | No |
| Quote calculation | Orchestrates | Yes | Yes (via gateway) |
| Proposal schema | Renders bank UX | Supplies dynamic schema | Yes |
| Payment money movement | Landing / reconciliation UX | Payment URL / intimation | Often yes |
| Policy issuance truth | Stores bank copy / refs | Status / docs APIs | Yes |
| Agent licence / mapping | Yes | May validate SP data | May require codes |

---

## 6. Open process risks (BA watchlist)

1. Async waits (quote/UW) without clear RM resume UX.  
2. Dynamic proposal forms that Figma treated as static screens.  
3. Payment redirect leaving bank session — resume & reconciliation.  
4. Multi-visit requirements without task inbox.  
5. Over-scoping Health/Motor patterns into Term MVP.

---

## 7. Linkage to prior research (optional)

A richer technical mapping of stages ↔ APIs exists under  
`docs/1sb-insurance-integration/journeys/universal-lob-journey.md`.  
Use it **after** AU Bank stages are confirmed — do not reverse-drive the product from it.
