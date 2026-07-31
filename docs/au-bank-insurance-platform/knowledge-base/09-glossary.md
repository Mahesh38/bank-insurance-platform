# 09 — Business glossary

Working vocabulary for AU Bank Insurance Distribution Platform. Prefer these terms in PRDs, journeys, and APIs (bank language).

| Term | Meaning |
|------|---------|
| **Insurance Distribution Platform** | Bank-owned system that runs insurance sales & fulfilment journeys |
| **AU Bank** | Distributing bank / programme context for this KB |
| **RM** | Relationship Manager — primary assisted-sales user |
| **Lead** | Sales opportunity that starts/resumes a journey |
| **Consent** | Customer permission record with versioning and evidence |
| **Suitability** | Bank assessment of needs/risk/eligibility before product push |
| **Product Catalogue** | Bank master of sellable insurance products |
| **Product Matrix** | Rules linking customer profile → eligible products/insurers |
| **Quote** | Request for premium/offers from one or more insurers |
| **Quote Comparison** | Side-by-side evaluation of offers |
| **Proposal** | Application/proposal submission to insurer path |
| **Underwriting Tracking** | Platform tracking of insurer UW/medical/requirements (bank does not underwrite) |
| **Payment** | Premium collection initiation, status, reconciliation |
| **Policy** | Issued contract as known to the bank platform |
| **Integration Hub** | Bank capability that routes to external insurance connectivity |
| **1SB / 1SilverBullet** | Current aggregator/partner for Phase A connectivity |
| **Canonical model** | Bank business objects/IDs independent of partner payloads |
| **Journey state** | Platform SoT for where a sale sits in the value stream |
| **Configurable rule** | Business rule managed by configuration + versioning, not hard-coded release |
| **BG-xxx** | Business Goal ID |
| **BR-xxx** | Business Requirement area / ID |
| **BP-xxx** | Business Process ID |
| **CJ-xx / RMJ-xx** | Customer / RM journey stage IDs |
| **JRN-xxx** | Named end-to-end or sub-journey ID |

### Terms to avoid in bank UX copy

Vendor field names (`manufacturerId`, `distributorID`, `reqId`, etc.) — keep inside Integration Hub adapters only.
