# AU Bank Insurance Platform — Business Clarifications & Working Decisions

**Document Status:** Working Draft v1  
**Purpose:** Capture business decisions and assumptions agreed during discovery discussions. Reference for architecture, product, BA, UX, and engineering until formally validated.  
**Owner:** Platform Product Owner  
**Bank:** AU Bank  
**Last updated:** 2026-07-31

---

## How teams must use this document

| Rule | Guidance |
|------|----------|
| Status | **Working decisions** — not yet board-signed; treat as default until overturned |
| Conflicts | If older docs disagree, **this file wins** until a newer version is published |
| Engineering | Prefer **configurable policy-driven controls** where §16 items are still pending |
| Aggregator | 1SB is the **current integration layer**, not a temporary hack (§18) |

Related: [BRD-OVERVIEW.md](./requirements/BRD-OVERVIEW.md) · [R0-SCOPE.md](./requirements/R0-SCOPE.md) · [knowledge-base/](./knowledge-base/README.md)

---

## 1. Product Scope — Line of Business (LOB)

### Phase 1 (MVP)

Platform supports **Life Insurance only**.

Supported product categories:

- Term Insurance  
- ULIP  
- Savings / Investment Plans  
- Future life products as required  

### Out of MVP

Health, Motor, Travel, and other non-life lines.

---

## 2. Customer Journeys

Platform must support **all three journeys from Day 1**.

### 2.1 RM Assisted Journey

RM assists customer throughout buying.

Typical flow:

```text
RM → Need Analysis → Quote → Proposal → Payment → Policy Issuance
```

### 2.2 Customer Self-Service Journey

Customer independently purchases via bank digital channels.

### 2.3 Hybrid Journey

Customer starts independently but can receive RM help at any stage.

Examples:

- Quote by customer → Proposal by RM  
- RM starts lead → Customer completes proposal  
- RM shares payment link  
- Customer completes payment on personal device  

**Architecture implication:** Journey engine must allow seamless movement between assisted and self-service modes.

---

## 3. Customer Segment

### MVP

**Existing-to-Bank (ETB) customers only.**

Existing includes any AU Bank relationship:

- Savings Account  
- Current Account  
- Loan Account  
- Credit Card  
- FASTag  
- Any other existing banking relationship  

No MVP restriction by Retail / HNI / Staff / Corporate — any ETB customer is eligible.

### Future (out of MVP)

**New-to-Bank (NTB)** — requires additional onboarding, KYC, and customer creation.

---

## 4. Definition of “Policy Sold”

For MVP reporting, a policy is **Sold** only when:

1. Policy has been successfully issued  
2. Bank receives issuance confirmation  
3. Financial reconciliation is possible  
4. Operations can track policy lifecycle  

**Not sold:** quotes alone, proposals alone, or successful payment alone.

---

## 5. Insurer Strategy

AU Bank has partnerships with multiple insurers, in two groups.

### Group A — Aggregator Integrated (1SilverBullet)

Primary insurers via 1SB (as applicable):

- ICICI Prudential  
- HDFC Life  
- Bajaj Allianz / Bajaj  

Supported via APIs:

- Quote  
- Proposal  
- Underwriting  
- Payment tracking  
- Policy issuance tracking  

**Journey remains inside AU Insurance Platform.**

### Group B — Non-Aggregated Insurers

Still in Product Catalogue (insurer, products, eligibility, suitability).

Quotes **not** available through 1SB.

Journey:

```text
Need Analysis → Product Recommendation → Redirect to insurer platform → Purchase there
```

---

## 6. Redirection Strategy (non-integrated insurers)

Customer redirected to insurer platform.

| Mode | Behaviour |
|------|-----------|
| Self-Service | Customer completes on insurer site on own device |
| RM Assisted | RM shares insurer journey link; customer completes on **personal device** |

**Hard rule:** No payment on RM device.

---

## 7. Product Catalogue

Platform owns Product Catalogue, independent of quote availability.

Catalogue fields (minimum):

- Insurer  
- Product  
- Product Variant  
- Eligibility  
- LOB  
- Product Type  
- Sales Status  
- Supported Journey  
- Aggregator Availability  

Even without quoting, catalogue still knows product availability.

---

## 8. Suitability & Need Analysis

**Decision:** Need Analysis is **mandatory**.  
**Decision:** Suitability assessment is **mandatory before quote generation**.  
Platform must **never bypass** this stage.

Purpose: regulatory compliance, suitability, recommendation, audit trail.

---

## 9. Consent

Customer consent is **mandatory**.

Working understanding: collect before policy purchase.  
**Exact sequencing requires regulatory validation** (see §16 and Open Validation Items).

R&D required for:

- IRDAI requirements  
- RBI requirements  
- Corporate Agency obligations for AU Bank  
- Mandatory consent stages  
- Whether consents can be consolidated without violating compliance  

**Principle:** UX may be optimized; compliance cannot be bypassed.

---

## 10. Insurance Agent Identity

Working understanding: each advisor/agent has an individual identifier for regulatory and insurer processes.

**Must validate** against current IRDAI rules and partner insurer onboarding before freezing the identity model.

---

## 11. Distributor Model (1SilverBullet)

Distributor in 1SB represents **AU Bank**.

```text
Distributor → AU Bank → Partner Insurers → Authentication Credentials
```

1SB creates a **Distributor ID** for AU Bank that identifies the requesting organization.

---

## 12. Credential Management

1. AU Bank signs partnership with insurer.  
2. Insurer provides API credentials (e.g. Client ID / Client Secret) for AU Bank.  
3. AU Bank authorizes 1SB as integration middleware.  
4. Credentials shared securely with 1SB.  
5. 1SB stores credentials against AU Bank Distributor ID.  
6. Runtime: request has Distributor ID → insurer identified → insurer-specific credentials selected → auth performed.  

One AU Bank Distributor ID can route to multiple insurers, each with its own auth config.

---

## 13. Environment Status (1SB)

Commercial onboarding with 1SB **completed**.

| Item | Status |
|------|--------|
| Agreement | Signed |
| SOP | Completed |
| UAT | Being provisioned |
| Distributor ID | Expected shortly |
| Initial integration | Starts on UAT |

---

## 14. Bank Platform Dependencies

| Dependency | Decision |
|------------|----------|
| SSO | Bank Identity platform |
| Notifications | Bank communication services |
| Payment Gateway | **AU Bank PG only** — no third-party PG planned |
| Lead Management (MVP) | Dedicated Lead module **inside** Insurance Platform |
| Lead Management (Future) | Migrate to AU Bank internal system (**“Sampath”**) — architecture must support migration |

---

## 15. Figma Prototype

Treat current Figma as **reference only**.

Contains MVP screens, concept screens, future-state ideas, incomplete journeys.  
**Not** source of truth. BRD and working decisions override Figma.

---

## 16. Compliance Research Required

Validate before hard-coding:

- IRDAI Corporate Agency obligations for AU Bank  
- Consent sequencing  
- Suitability obligations  
- Mandatory disclosures  
- Digital proposal compliance  
- Customer acknowledgement requirements  
- Insurance PII handling  
- Data retention  
- Logging requirements  
- Audit requirements  
- Record preservation periods  
- Data residency under applicable Indian regulations  

Until validated: implement **configurable policy-driven controls**.

---

## 17. Executive Sponsorship

**Role:** Head of Insurance Business / Insurance Platform at AU Bank.  
**Individual name:** to be confirmed.

---

## 18. 1SilverBullet Positioning

Do **not** describe 1SB as a temporary workaround or shortcut.

Position as:

- current integration layer  
- accelerator for insurer connectivity  
- abstraction over insurer-specific integrations  

Core business capabilities must **not** be tightly coupled to 1SB.  
Architecture must allow replacement by another aggregator or a future AU Bank-owned aggregation layer with minimal impact on business services.

---

## 19. Aggregator Strategy

| Horizon | Plan |
|---------|------|
| Current / MVP | Single aggregator (1SB) |
| Future | Extensible to multiple aggregators if needed |

Multi-aggregator routing is **not** an MVP requirement, but design should avoid major redesign to add it later.

---

## 20. Future Opportunities (Out of Scope for MVP)

Maintain in backlog; exclude from current delivery planning:

- Insurance offers during loan disbursement  
- Embedded insurance within banking journeys  
- New-to-Bank customer onboarding  
- Bank-owned insurance aggregation layer  
- Multiple external aggregators  
- Expanded LOB (Health, Motor, etc.)  
- Branch kiosk journey (pending business confirmation)  

---

## Open Validation Items

| Topic | Current Status |
|-------|----------------|
| Exact IRDAI consent model | Pending validation |
| RBI + IRDAI compliance mapping | Pending validation |
| Corporate Agency obligations | Pending validation |
| Insurance advisor identity model | Pending validation |
| PII retention period | Pending validation |
| Data residency requirements | Pending validation |
| Audit log retention | Pending validation |
| Executive sponsor name | Pending confirmation |
| Branch kiosk journey | Pending business decision |

---

## Decision log mapping (closed by this draft as Working)

Canonical IDs: [DECISION-LOG.md](./DECISION-LOG.md).

| ID | Decision from this draft |
|----|--------------------------|
| D-001 / Q-P0-01 | Life only (Term, ULIP, Savings/Investment, future life) |
| D-002 / Q-P0-02 | RM + Self-service + Hybrid from Day 1 |
| D-009 / Q-P0-03 | ETB only; any banking relationship |
| D-007 / Q-P0-04 | Policy issued + confirmation + reconcilable + ops trackable |
| D-010 / Q-P0-05 | Group A via 1SB; Group B catalogue + redirect |
| D-006 / Q-P0-06 | Customer device; AU Bank PG; no payment on RM device |
| D-005 / Q-P0-07 | Mandatory need analysis + suitability before quote |
| D-003 / Q-P0-09,16 | AU Bank Distributor ID in 1SB; UAT provisioning |
| D-004 / Q-P0-17 | Replaceability required — no tight coupling to 1SB |
| D-012 / Q-P0-11a | Figma = reference only |
| D-011 / Q-P0-10 | Consent mandatory; sequencing pending |
| D-008 / Q-P0-08 | Agent identity pending validation |
| — / Q-P0-20 | Sponsor role confirmed; name pending |

---

## Change control

| Version | Date | Change | Owner |
|---------|------|--------|-------|
| 1.0 | 2026-07-31 | Initial working draft from discovery clarifications | Platform PO |
| 1.1 | 2026-07-31 | PO document review: align Decision Log IDs; clarify Working vs Approved | Platform PO |
