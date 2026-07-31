# 08 — Integration strategy

**Sources:** Volume 01, Phase 1, Phase 5

---

## Strategic stance

Integrations are **replaceable implementation details**.  
Business domains and journeys stay stable across connectivity phases.

```text
Bank channels (RM / Customer)
        │
        ▼
Insurance Distribution Platform (capabilities / domains)
        │
        ▼
Integration Hub  ──►  Adapter: 1SB (now)
                 ──►  Adapter: Direct insurer (later)
                 ──►  Adapter: Hybrid coexistence
```

---

## Connectivity roadmap

| Phase | Mode | Meaning |
|-------|------|---------|
| **A / Phase 1** | 1SB | Platform integrates with 1SilverBullet for supported insurers |
| **B / Phase 2** | Hybrid | New direct insurer integrations can coexist with 1SB |
| **C / Phase 3** | Direct expand | Direct integrations replace 1SB where commercial/tech feasible |
| **Phase 4** | Mature hub | Enterprise Integration Hub operating model |

---

## Canonical request flow (Phase 5)

1. RM authenticates via bank identity  
2. Lead created in platform  
3. Consent captured and stored  
4. Suitability determines needs  
5. Product Catalogue identifies eligible products  
6. Integration Hub routes to appropriate insurer capability  
7. Quote → proposal → UW → payment progress in platform  
8. Policy issuance status captured and reported  

---

## Cross-cutting architecture expectations

| Concern | Expected outcome |
|---------|------------------|
| Security | RBAC, encryption, audit |
| Availability | Highly available business services |
| Performance | Responsive RM and customer journeys |
| Observability | Monitoring, tracing, ops dashboards |
| Configuration | Business rules without code changes |
| Compliance | IRDAI + bank governance support |
| Scalability | Additional insurers and products |

---

## Relationship to prior engineering spike

`docs/1sb-insurance-integration/` explored a **thin integration service** (quote/proposal/payment/status).  

**PO framing for AU Bank platform:**

| Layer | Owns |
|-------|------|
| Distribution platform (this KB) | Lead, consent, suitability, catalogue, journey, RM workspace, reporting… |
| Integration Hub / adapters | Connectivity to 1SB/insurers; hide wire formats |
| Prior 1SB service work | Candidate **adapter slice** for Phase A — not the whole platform |

Do not equate “1SB integration service done” with “Insurance Distribution Platform done.”
