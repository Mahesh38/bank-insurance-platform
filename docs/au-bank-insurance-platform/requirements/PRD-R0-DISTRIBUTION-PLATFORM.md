# PRD — R0 Insurance Distribution Platform (AU Bank)

**Product:** AU Bank Insurance Distribution Platform  
**Release:** R0 Pilot  
**Owner:** Platform Product Owner  
**Version:** 0.2  
**Related:** [Working Decisions](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) · [R0-SCOPE.md](./R0-SCOPE.md) · [BRD-OVERVIEW.md](./BRD-OVERVIEW.md) · [BRD-P0-CAPABILITIES.md](./BRD-P0-CAPABILITIES.md) · KB journeys  
**Alignment:** MVP = Life ETB; journeys = RM + Self + Hybrid; Sold = issuance; Group A/B insurer model

---

## 1. Problem

Insurance sales at the bank are fragmented across external tools. RMs and customers lack a single compliant workspace; the bank lacks end-to-end visibility to **policy issuance**; aggregator dependence risks lock-in.

## 2. Goals

| Goal | BG | R0 contribution |
|------|----|-----------------|
| Single platform journey | BG-001 | Assisted, self-service, and hybrid Life paths |
| RM productivity | BG-002 | One workspace for lead→issuance (+ share links) |
| Conversion | BG-003 | Instrument funnel; KPI = **Sold** (issued) |
| Integration independence | BG-004 | Hub + 1SB adapter only at edge |
| Visibility | BG-005 | Journey state + issuance confirmation |
| Compliance | BG-006 | Need analysis, suitability, consent, audit, attribution |

## 3. Personas (R0)

| Persona | Needs |
|---------|-------|
| **RM** | Find ETB customer, run/assist journey, share payment/insurer links, never take payment on RM device |
| **Customer (ETB)** | Self-serve or hybrid: suitability, consent, proposal steps, payment on personal device |
| **Ops (lite)** | See stuck journeys; escalate insurer issues; track lifecycle post-issuance |
| **Compliance** | Reconstruct who did what with evidence |
| **Platform admin (lite)** | Seed catalogue/rules/config (may be backoffice) |

## 4. User stories (epic-level)

### 4.1 RM-assisted happy path

1. As an RM, I authenticate so my actions are attributable.  
2. As an RM, I search an ETB customer and start/resume a lead.  
3. As an RM, I capture customer consent before advice/sale steps.  
4. As an RM, I complete need analysis / suitability and see eligible catalogue products.  
5. As an RM, for Group A I request quotes and compare offers; for Group B I recommend and share insurer redirect.  
6. As an RM, I select an offer and complete or hand off the proposal form.  
7. As an RM, I track underwriting/requirements status.  
8. As an RM, I share a payment link; customer pays on their device; I see payment/policy outcome.  
9. As an RM, I see my pipeline of open journeys and next actions.

### 4.2 Self-service & hybrid

10. As a customer, I start a Life journey on bank digital channels.  
11. As a customer, I complete need analysis / suitability before any quote.  
12. As a customer or RM, I can hand off mid-journey (quote↔proposal↔payment) without restarting.  
13. As a customer, I complete payment on my personal device (never on RM device).  

### 4.3 Compliance / platform

14. As Compliance, I can retrieve audit events for a journey including agent and distributor.  
15. As the platform, I never trust caller-supplied distributor identity.  
16. As the platform, I hide aggregator wire formats from channel apps.  
17. As Ops, I only count **Sold** when policy is issued, confirmed, reconcilable, and trackable.

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
| PRD-F-18 | Customer self-service journey (Life ETB) | **P0** (D-002 Day 1) |
| PRD-F-19 | Hybrid mode-switch without journey restart | **P0** (D-002) |
| PRD-F-20 | Group B recommendation + insurer redirect | **P0** (D-010) |

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
