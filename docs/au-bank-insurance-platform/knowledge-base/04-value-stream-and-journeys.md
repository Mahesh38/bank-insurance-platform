# 04 — Value stream & journeys

**Sources:** Volume 05/06, Phase 3

---

## End-to-end business value stream

```text
Customer Identification
    → Lead
    → Consent
    → Suitability
    → Product Discovery
    → Quote
    → Proposal
    → Underwriting
    → Payment
    → Policy Issuance
    → Customer Communication
    → Reporting
    → Ongoing Administration / Servicing
```

**Journey state model (Vol 06):**  
Lead → Consent → Suitability → Product Discovery → Quote → Proposal → Underwriting → Payment → Policy Issuance → Active Policy → Ongoing Servicing

---

## Customer journey (CJ)

| ID | Stage | Description |
|----|-------|-------------|
| CJ-01 | Discover | Customer expresses insurance need |
| CJ-02 | Registration | Register/login and identity verification |
| CJ-03 | Customer Profile | Capture or retrieve customer details |
| CJ-04 | Consent | Obtain and record consent |
| CJ-05 | Suitability | Assess objectives, demographics, risk |
| CJ-06 | Product Discovery | Eligible products via Product Matrix |
| CJ-07 | Quote Comparison | Fetch and compare insurer quotes |
| CJ-08 | Quote Selection | Select preferred quote |
| CJ-09 | Proposal | Questionnaire and declarations |
| CJ-10 | Underwriting | Track medical/UW decisions |
| CJ-11 | Payment | Pay premium; verify transaction |
| CJ-12 | Policy Issuance | Receive policy and documents |
| CJ-13 | Post-Issuance | Status, communications, servicing requests |

---

## Relationship Manager journey (RMJ)

| ID | Stage | Description |
|----|-------|-------------|
| RMJ-01 | Lead Creation | Create or receive lead |
| RMJ-02 | Customer Lookup | Search existing or register new |
| RMJ-03 | Customer Assistance | Guide onboarding |
| RMJ-04 | Consent Validation | Ensure valid consent |
| RMJ-05 | Suitability | Capture financial and insurance needs |
| RMJ-06 | Product Recommendation | Review eligible products |
| RMJ-07 | Quote Assistance | Request and explain quotes |
| RMJ-08 | Proposal Assistance | Help complete proposal |
| RMJ-09 | Underwriting Coordination | Coordinate insurer requirements |
| RMJ-10 | Payment Follow-up | Ensure payment completion |
| RMJ-11 | Policy Delivery | Confirm issuance and acknowledgement |
| RMJ-12 | Portfolio Management | Leads, policies, renewals, follow-ups |
| RMJ-13 | Exception Handling | Validations, insurer queries, escalations |

---

## Named journeys (Phase 3 — JRN catalogue)

Phase 3 decomposes operations into named journeys. Each shares a common 12-step spine, actors (RM, Customer, Operations), and standard alternates/exceptions. Distinct intent by ID:

| ID | Journey | PO use |
|----|---------|--------|
| **JRN-001** | RM Assisted New Policy Purchase | **Primary MVP candidate** |
| **JRN-002** | Existing Customer Policy Purchase | Prefill / CIF path variant of sale |
| JRN-003 | Lead Creation & Assignment | Sub-journey |
| JRN-004 | Consent Capture | Sub-journey / compliance gate |
| JRN-005 | Suitability Assessment | Sub-journey / compliance gate |
| JRN-006 | Product Discovery & Recommendation | Sub-journey |
| JRN-007 | Quote Generation (1SB / Future Direct) | Integration-touching |
| *(further JRN pages in Phase 3)* | Proposal / UW / Payment / Policy variants | Same spine; specialize in BA |

### Shared standard flow (sale spine)

1. RM identifies need  
2. Lead created/resumed  
3. Consent captured/validated  
4. Suitability completed  
5. Eligible products identified  
6. Quote/journey via configured integration  
7. Customer reviews options  
8. Proposal prepared/submitted  
9. UW/medical tracked  
10. Payment completed  
11. Policy issued  
12. Status updated/reported  

### Shared alternates

Customer already exists · Pause/resume · RM changes product · Multiple products · Customer stops  

### Shared exceptions

Consent expired · Eligibility fail · Quote unavailable · Payment failure · UW rejection · Policy issuance timeout  

### Shared business rules (journey-level)

- Every customer action auditable  
- Suitability recommendations recorded  
- Journey status traceable  
- Customer data per bank security policy  
- Prefer configurable rules over hard-coding  

---

## Common alternate & exception catalogue (Vol 06)

- Abandon quote → resume later  
- Consent expires or withdrawn  
- Ineligible after suitability  
- No eligible products  
- Quote expires before proposal  
- UW requests info / medical exam  
- Payment failure and retry  
- Policy delayed or rejected  
- Manual ops intervention  
- Insurer integration unavailable → retry/escalation  

---

## Mapping hint for Figma

When inventoring the client-review prototype, tag each screen with **CJ-xx / RMJ-xx / JRN-xxx** and MVP Yes/Later. Store mapping in `../04-process-and-journey-canvas.md` §4.
