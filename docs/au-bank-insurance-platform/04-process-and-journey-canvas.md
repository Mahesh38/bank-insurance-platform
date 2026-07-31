# Process & journey canvas — AU Bank Insurance Platform

**Owner:** Business Analysis  
**Status:** Aligned to Working Decisions — **Life LOB; RM + Self + Hybrid**; validate detail against Figma (reference only) + SMEs  
**Canonical detail:** [knowledge-base/04-value-stream-and-journeys.md](./knowledge-base/04-value-stream-and-journeys.md)  
**SSOT decisions:** [07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)  
**Rule:** AU Bank process language. Aggregator steps are “external capability”, not the product.

---

## 1. Universal sale journey (working model)

Spine applies to **RM-assisted, self-service, and hybrid** (mode may switch mid-journey).

```text
[A] Actor context (RM session and/or Customer self-service)
        │
        ▼
[B] Customer find (ETB / CIF) + consent / disclosures
        │
        ▼
[C] Need analysis / suitability (bank-owned) — MANDATORY before quote
        │
        ▼
[D] Product recommendation (Catalogue: Group A and/or Group B)
        │
        ├─ Group A (1SB) ──────────────────────────────┐
        │                                              │
        ▼                                              ▼
[E] Quote → offers → compare/select          [E'] Redirect to insurer
        │                                              │
        ▼                                              ▼
[F] Proposal → submit → UW/requirements      [F'] Customer completes on insurer site
        │                                              │
        ▼                                              ▼
[G] Payment on **customer device** (AU Bank PG / insurer path)
        │
        ▼
[H] Policy issued → bank confirmation → Sold (reconcilable + ops-trackable)
```

**Hybrid examples:** Customer quotes → RM proposes; RM starts lead → customer completes proposal; RM shares payment link → customer pays on personal device. **No payment on RM device.**

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
5. Over-scoping Health/Motor patterns into **Life** MVP.  
6. Hybrid mode-switch without losing journey state / audit continuity.  
7. Group B redirect without clear bank-side recommendation audit.

---

## 7. Linkage to prior research (optional)

A richer technical mapping of stages ↔ APIs exists under  
`docs/1sb-insurance-integration/journeys/universal-lob-journey.md`.  
Use it **after** AU Bank stages are confirmed — do not reverse-drive the product from it.
