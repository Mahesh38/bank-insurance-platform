# 03 — Business capability map

**Sources:** Volume 01, Phase 1, Phase 5 domains, Volume 03 / Phase 2 BR areas

---

## Capability layers (Vol 01)

```text
Acquisition     → Customer, RM, Lead
Advisory        → Consent, Suitability, Recommendation
Sales           → Product Catalogue, Quote, Proposal
Fulfilment      → Underwriting, Payment, Policy
Operations      → Reporting, Communication, Administration
Platform        → Identity, Audit, Integration Hub, Configuration
```

---

## Master capability catalogue

| Domain | Capability | BR prefix | Notes |
|--------|------------|-----------|-------|
| Customer | Customer Management | BR-CUST | Profile, search, update; existing + new |
| Sales | RM Workspace | BR-RM | Portfolio, tasks, assisted sale |
| Sales | Lead Management | BR-LEAD | Create, assign, track, reopen |
| Compliance | Consent Management | BR-CONSENT | Capture, version, withdraw, evidence |
| Advisory | Suitability & Recommendation | BR-SUIT | Needs, risk, eligibility, recommendations |
| Products | Product Catalogue & Product Matrix | BR-PROD | Eligibility matrix across insurers/products |
| Sales | Quote Management | BR-QUOTE | Request, track, refresh |
| Sales | Quote Comparison | BR-COMP | Compare offers (Phase 2 distinct from Quote) |
| Sales | Proposal Management | BR-PROP | Dynamic questionnaire, submit, resume |
| Operations | Underwriting Tracking | BR-UW | Track medical/docs/insurer decisions (not underwrite) |
| Payments | Payment Management | BR-PAY | Initiate, monitor, retry, reconcile |
| Policies | Policy Issuance & Management | BR-POL | Issuance status, documents, lifecycle |
| Service | Renewals & Servicing | BR-SERV | Later-phase; listed in Phase 1/2 |
| Communication | Notifications & Communication | BR-COMM | SMS/email/templates across stages |
| Analytics | Reporting & Dashboards | BR-REP | RM, branch, ops, exec |
| Admin | Administration & Configuration | BR-ADMIN | Products, rules, users, integrations |
| Integration | Integration Hub | BR-INT | 1SB now; direct insurers later |
| Security | Identity, Access, Audit & Compliance | BR-SEC | AuthZ, audit, retention |

---

## Phase 5 business domains (architecture view of same map)

Identity & Access · Customer · Lead · Consent · Suitability · Product Catalogue · Quote · Proposal · Underwriting · Payments · Policy · Communications · Reporting · Administration · Integration Hub · Audit & Compliance

**PO note:** Domains ≠ mandated microservice count. They are ownership boundaries for SoR and APIs.

---

## Capability ownership (from Phase 4 governance)

| Area | Business owner | Decision authority |
|------|----------------|--------------------|
| Products | Product Team | Launch, retirement, availability |
| Suitability | Business & Compliance | Rules and recommendation policy |
| Lead | Sales | Assignment and lifecycle |
| Consent | Compliance | Wording and validity |
| Integration | Technology & Partner Team | Partner onboarding |
| Reporting | BI | KPI definitions |
| Security | Information Security | Access and audit policies |

---

## Cross-capability quality bar (repeated in BR templates)

Every capability is expected to support, at minimum:

- Create / view / update / search / track  
- Audit actions  
- Configurable, versioned rules  
- Reporting hooks  
- Exception handling & recovery  
- Future readiness: 1SB → direct without changing business behaviour  

Detailed AC per capability = **outstanding BA work** (see [10](./10-gaps-and-po-assessment.md)).
