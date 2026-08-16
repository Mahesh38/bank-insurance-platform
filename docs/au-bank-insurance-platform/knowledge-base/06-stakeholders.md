# 06 — Stakeholder catalogue (synthesized)

**Source:** Volume 04  
**Note:** Role descriptions in the PDF are template-level; this KB retains the **role inventory** and groups them for RACI-style use.

---

## Groups & roles

### Executive & governance
- Business Sponsor  
- Programme Sponsor  
- Steering Committee  
- Enterprise Architecture Review Board  
- Compliance & Risk  
- Information Security  
- Legal  
- Audit  

### Sales & distribution
- Relationship Manager (RM) — **primary day-1 user**  
- Branch Manager  
- Regional Manager  
- Sales Head  
- Insurance Business Head  

### Customers
- Existing Customer  
- New Customer  
- Prospective Customer  
- Nominee  

### Operations
- Operations Executive  
- Back Office  
- Policy Servicing Team  
- Customer Support  
- Quality Assurance  

### Insurance partners
- Partner Insurer  
- Underwriting Team (insurer)  
- Medical Team  
- Insurer Operations  
- Partner Relationship Manager  

### Platform administration
- Product Owner  
- Business Analyst  
- Platform Administrator  
- Configuration Administrator  
- Reporting Administrator  

### Technology & delivery
- Technology  
- DevOps  
- Integration Team  
- Data Team  
- QA Team  

### External systems / partners
- External Systems  
- Payment Gateway  
- **1SilverBullet** (aggregator — connectivity stakeholder, not product owner)

---

## RACI snapshot (PO working view)

| Decision / work | A | R | C | I |
|-----------------|---|---|---|---|
| Vision & scope freeze | Business Sponsor | Product Owner | Compliance, Insurance Business Head | Steering |
| Suitability / consent policy | Compliance | BA + Product | Legal, Sales | RM network |
| Product / insurer availability | Insurance Business Head | Product Team | Integration, Compliance | RM |
| Journey UX (Figma → MVP) | Product Owner | Digital/BA | RM ops, Compliance | Eng |
| Integration partner choice | Programme Sponsor | Integration + Partner Team | Architecture, Infosec | Product |
| Pilot success metrics | Business Sponsor | Product Owner | BI, Sales Head | Ops |

This source-derived RACI describes **human organizational roles** and is not replaced by AI personas.

---

## Repository AI mapping — Executive Sponsor perspective

For repository reasoning, the Product package provides **[Dilip — AI Executive Sponsor Perspective](../../context/roles/principal-insurance-platform-product-owner/executive-sponsor-perspective/README.md)**.

This lens may simulate the **business/insurance sponsor perspective** for:

- vision and business-problem challenge;
- P0/R0 scope value and conscious deferral;
- bancassurance growth/distribution questions;
- product/insurer/channel strategic trade-offs;
- build / buy / partner decisions;
- budget, TCO, ROI and payback reasoning;
- pilot-success metrics and benefits realization;
- identifying business gaps that may require digital capability or operating-model change.

It does **not** mutate the stakeholder catalogue or human RACI above. In particular:

- Rajal remains the canonical repository Product authority;
- the AI lens cannot satisfy a real Business Sponsor or Programme Sponsor signature when the organization requires one;
- the lens cannot override Compliance, Architecture, Security, QA, Database, SRE or other canonical specialist authority;
- persisted results must be labelled `AI Executive Sponsor Perspective`, not as a decision personally made by the real Dilip Kumar Vidyarthi.

The purpose is to make the sponsor's **decision perspective available to the team before material P0 stories and strategic choices are finalized**, without creating a fictitious human approval.

---

## Engagement model for discovery restart

| Cadence | Who | Why |
|---------|-----|-----|
| Session 1 (charter) | Sponsors, Insurance Business Head, PO, Compliance | Freeze problems + LOB |
| Session 2 (journey) | PO, BA, Digital, RM ops | Map Figma → CJ/RMJ/JRN |
| Session 3 (scope) | PO, BA, Architecture, Integration | Freeze R0 capabilities |
| Ongoing | 1SB Partner RM | Sandbox, panel, SLAs — after product freeze |

For repository-based AI preparation of Session 1 or any equivalent P0 scope/business-value checkpoint, load the Dilip sponsor lens before Rajal finalizes the Product decision.
