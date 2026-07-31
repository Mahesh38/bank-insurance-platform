# 01 — Executive synthesis (Platform PO)

**Product name (from docs):** Insurance Distribution Platform  
**Bank context:** AU Bank  
**PO verdict:** Directionally clear vision; catalogue structure is solid; **detail depth is still baseline/template** — not yet a build-ready BRD.

---

## What the documents say we are building

A **bank-owned, insurer-agnostic insurance distribution platform** that owns the full journey:

**Lead → Consent → Suitability → Product discovery → Quote → Proposal → Underwriting tracking → Payment → Policy issuance → Servicing / reporting**

…while:

1. Integrating with **1SilverBullet (1SB) first**
2. Moving to **hybrid**, then **direct insurer** integrations
3. Keeping a **stable bank business model** so apps and processes do not rewrite when connectivity changes

---

## Why (business drivers)

| Driver | Implication for AU Bank |
|--------|-------------------------|
| Own the customer journey | Platform SoT for lead, consent, suitability, journey state — not the aggregator UI |
| RM productivity | One RM workspace for customer → quote → proposal → follow-up |
| Standardize insurance sales | Same process across insurers/products via Product Matrix |
| Visibility & reporting | End-to-end status, conversion, SLA dashboards |
| Reduce aggregator lock-in | Integration Hub as replaceable adapters |
| Faster insurer onboarding | Configuration + catalogue, not new apps per insurer |

**Current state (stated):** fragmented journeys on external platforms; weak operational control.  
**Target state:** platform owns customer, RM workspace, lead, consent, suitability, catalogue, quote orchestration, proposal, payment tracking, policy lifecycle, reporting, administration.

---

## Binding product themes (PO adopts)

These are strong enough to treat as working product doctrine until a sponsor overturns them:

1. **Business before technology** — capabilities drive system boundaries.  
2. **Canonical bank model** — stable IDs and journey language independent of insurer/1SB payloads.  
3. **Configuration over code** — rules versioned and auditable.  
4. **Replaceable integrations** — 1SB is Phase-1 connectivity, not the product.  
5. **Security & audit by design** — every material action auditable; IRDAI/bank governance called out.  
6. **Assisted + self-service** — both journey modes are in goals (RM workspace is central).  
7. **Exclusions for now:** insurer core systems; claims administration as a platform scope (servicing/renewals appear as later capabilities).

---

## What is *not* decided yet (critical)

Source packs do **not** lock:

- First LOB / product for AU Bank pilot (Term vs Health vs Motor…)  
- Exact insurer panel for go-live  
- Whether R0 is existing-customer-only or also new/prospective (both personas are listed)  
- Suitability questionnaire content and override policy  
- Payment rail (gateway vs aggregator URL)  
- Definition of pilot success (quote vs payment vs policy)  
- Detailed acceptance criteria per BR-* (Volume 03 / Phase 2 are structural templates)

See [10-gaps-and-po-assessment.md](./10-gaps-and-po-assessment.md).

---

## Traceability chain (from docs)

```text
Vision → Capability → Business Requirement → Business Rule → Business Process
      → Journey → Information Object → Functional Spec → API → Test → KPI
```

Every delivery item should map to a **Business Goal (BG-001…008)**.

---

## PO recommendation

| Priority | Action |
|----------|--------|
| Now | Use this KB as the programme vocabulary |
| Next workshop | Freeze first journey (recommend **JRN-001 RM Assisted New Policy Purchase**) + first LOB |
| BA workstream | Replace template BR-* with real AC, data, and exceptions |
| UX | Map Figma screens onto CJ/RMJ / JRN stages |
| Eng advisory | Treat Integration Hub + domain list as architecture *inputs*, not frozen microservices |
