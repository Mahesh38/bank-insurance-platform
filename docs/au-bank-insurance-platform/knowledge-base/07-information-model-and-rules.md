# 07 — Information model, business rules & governance

**Source:** Phase 4

---

## Canonical business objects

Each object is bank-owned vocabulary (independent of 1SB/insurer wire format).  
Later elaboration must add: ownership, lifecycle, identifiers, attributes, validation, relationships, audit, retention.

| Object | Intent |
|--------|--------|
| Customer | Person/party buying or holding insurance via bank |
| Relationship Manager | Bank seller / advisor identity |
| Lead | Sales opportunity / journey seed |
| Consent | Permission evidence (versioned) |
| Suitability Assessment | Needs/risk/eligibility decision record |
| Product Catalogue | Bank view of sellable products + matrix |
| Quote | Quote request + offers + selection |
| Proposal | Application/proposal submission case |
| Underwriting Case | Tracking shell for UW/medical/requirements |
| Payment | Premium payment attempt / reconciliation |
| Policy | Issued policy + lifecycle |
| Communication | Outbound notices / templates instances |
| Audit Event | Immutable business/tech evidence |
| Reporting Metric | KPI definitions and measures |
| Partner Insurer | Insurer partner master |

**Principle:** Business identifiers remain stable regardless of integration provider.

---

## Business rule categories

| Category | Example topics |
|----------|----------------|
| Lead | Creation, ownership, assignment, expiry, merge, reopen |
| Consent | Capture, versioning, validity, withdrawal, audit |
| Suitability | Eligibility, recommendation, override, versioning |
| Product | Availability, insurer mapping, effective dates |
| Quote | Validity, refresh, comparison, selection |
| Proposal | Mandatory fields, save/resume, submission |
| Underwriting | Medical, document review, insurer decisions |
| Payment | Retry, reconciliation, settlement |
| Policy | Issuance, activation, servicing, renewal |
| Security | Authentication, authorization, audit, retention |

### Standard rule template fields

Rule ID · Name · Business statement · Applies to · Trigger · Validation · Exception · Owner · Priority (P0/P1/P2) · Audit evidence  

---

## Information governance principles

1. Canonical vocabulary across integrations  
2. Stable business identifiers  
3. Configuration preferred over code  
4. Auditable state transitions  
5. Version-controlled business rules  
6. PII classification + retention per object  
7. Traceable regulatory evidence  

---

## PO note

Rule *categories* exist; concrete rule statements (e.g. consent TTL, quote validity hours, suitability override who-can) are **not** in the baseline PDFs. Those are the next BA deliverable before build.
