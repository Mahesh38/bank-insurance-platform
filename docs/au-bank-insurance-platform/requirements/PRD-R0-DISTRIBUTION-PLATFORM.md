# PRD — R0 Insurance Distribution Platform (AU Bank)

**Product:** AU Bank Insurance Distribution Platform  
**Release:** R0 Pilot  
**Owner:** Platform Product Owner  
**Version:** 0.1  
**Related:** [R0-SCOPE.md](./R0-SCOPE.md) · [BRD-P0-CAPABILITIES.md](./BRD-P0-CAPABILITIES.md) · KB journeys

---

## 1. Problem

Insurance sales at the bank are fragmented across external tools. RMs lack a single compliant workspace; the bank lacks end-to-end visibility; aggregator dependence risks lock-in.

## 2. Goals

| Goal | BG | R0 contribution |
|------|----|-----------------|
| Single platform journey | BG-001 | JRN-001 on bank UI |
| RM productivity | BG-002 | One workspace for lead→policy |
| Conversion | BG-003 | Instrument funnel |
| Integration independence | BG-004 | Hub + 1SB adapter only at edge |
| Visibility | BG-005 | Journey state + pilot dashboard |
| Compliance | BG-006 | Consent, suitability, audit, attribution |

## 3. Personas (R0)

| Persona | Needs |
|---------|-------|
| **RM** | Find customer, run journey, explain quotes, complete proposal, chase payment/UW |
| **Existing customer** | Consent, answer suitability, complete OTP/payment/docs as prompted |
| **Ops (lite)** | See stuck journeys; escalate insurer issues |
| **Compliance** | Reconstruct who did what with evidence |
| **Platform admin (lite)** | Seed products/rules/config (may be backoffice, not full UI) |

## 4. User stories (epic-level)

### 4.1 RM happy path

1. As an RM, I authenticate so my actions are attributable.  
2. As an RM, I search an existing customer and start/resume a lead.  
3. As an RM, I capture customer consent before advice/sale steps.  
4. As an RM, I complete suitability and see eligible products.  
5. As an RM, I request multi/single quotes and compare offers.  
6. As an RM, I select an offer and complete the proposal form.  
7. As an RM, I track underwriting/requirements status.  
8. As an RM, I initiate payment and see payment/policy outcome.  
9. As an RM, I see my pipeline of open journeys and next actions.

### 4.2 Customer-assisted steps

10. As a customer, I provide consent (and OTP if required).  
11. As a customer, I complete payment on the payment URL and return to bank landing.  

### 4.3 Compliance / platform

12. As Compliance, I can retrieve audit events for a journey including agent and distributor.  
13. As the platform, I never trust caller-supplied distributor identity.  
14. As the platform, I hide aggregator wire formats from channel apps.

## 5. Functional requirements (summary)

Detailed AC in BRD. Summary:

| ID | Requirement | Priority |
|----|-------------|----------|
| PRD-F-01 | RM auth + session with actor id | P0 |
| PRD-F-02 | Customer CIF search + profile display/prefill | P0 |
| PRD-F-03 | Lead lifecycle (create/resume/status) | P0 |
| PRD-F-04 | Consent capture, validity check, block if invalid | P0 |
| PRD-F-05 | Suitability capture + recommendation record + gate | P0 |
| PRD-F-06 | Product eligibility via Product Matrix (first LOB) | P0 |
| PRD-F-07 | Quote create + async result + compare + select | P0 |
| PRD-F-08 | Proposal schema fetch, fill, submit, status | P0 |
| PRD-F-09 | Application/UW status normalised for RM | P0 |
| PRD-F-10 | Payment session/URL + status + retry | P0 |
| PRD-F-11 | Policy visibility when issued | P0 |
| PRD-F-12 | RM workspace list/filter of journeys | P0 |
| PRD-F-13 | Audit events for material actions | P0 |
| PRD-F-14 | Integration Hub routes insurer ops via adapter | P0 |
| PRD-F-15 | Pilot funnel metrics export/dashboard | P1 (minimum P0 counts) |
| PRD-F-16 | Notifications (SMS/email) on key stages | P1 |
| PRD-F-17 | Full admin rule authoring UI | P2 |
| PRD-F-18 | Customer self-serve full journey | P2 |

## 6. Non-functional requirements (draft targets)

| ID | Area | Target (draft — confirm GAP-017) |
|----|------|----------------------------------|
| PRD-N-01 | Availability (pilot) | 99.5% monthly for RM workspace |
| PRD-N-02 | Quote request p95 | < 3s to accept job (async completion separate) |
| PRD-N-03 | Security | RBAC; secrets in vault; no PII in logs |
| PRD-N-04 | Audit retention | Align bank policy (draft 7 years for raw evidence) |
| PRD-N-05 | Privacy | Mask PAN/mobile/email/DOB in logs |
| PRD-N-06 | Resilience | Aggregator down → clear RM error + retry; no silent fail |
| PRD-N-07 | Replaceability | Channel apps call bank APIs only |

## 7. UX requirements

| ID | Requirement |
|----|-------------|
| PRD-U-01 | Primary flows match inventoried Figma MVP screens (post GAP-009) |
| PRD-U-02 | Brand is AU Bank; aggregator not primary brand |
| PRD-U-03 | Async waits (quote/UW) show status + allow resume |
| PRD-U-04 | Dynamic proposal forms supported (not only static screens) |
| PRD-U-05 | Payment URL never shown in logs; show ref only to ops |

## 8. Data & audit

| ID | Requirement |
|----|-------------|
| PRD-D-01 | Journey id stable across stages |
| PRD-D-02 | Dual ids: bank ids + external refs (insurer/1SB) |
| PRD-D-03 | Consent & suitability versions stored |
| PRD-D-04 | Distributor id from config/secrets only |
| PRD-D-05 | Agent id required on proposal submit |

## 9. Release plan (product)

| Milestone | Outcome |
|-----------|---------|
| M0 | Scope + rules signed |
| M1 | Design freeze JRN-001 |
| M2 | UAT sandbox E2E |
| M3 | Pilot production cohort |
| M4 | Retro → R1 |

## 10. Open questions

Tracked in GAP register and discovery backlog. PRD must be revised when A1–A9 change.

## 11. Out of scope

See [R0-SCOPE.md](./R0-SCOPE.md) §4.
