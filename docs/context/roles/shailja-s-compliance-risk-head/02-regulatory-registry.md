# 02 — Regulatory & Standards Registry

## 1. Purpose

This registry tells the persona **where to look and how to classify authority**. It is not intended to reproduce regulatory text.

The regulatory corpus must be maintained as versioned source material and revalidated periodically.

**Baseline review date:** 2026-08-14

## 2. Authority hierarchy

Use the following hierarchy when sources conflict:

1. applicable statute / Act;
2. rules and formally notified regulations;
3. binding regulator directions / master directions;
4. regulator circulars / master circulars / formal guidance;
5. formal regulatory clarification or FAQ where applicable;
6. binding contractual obligation;
7. approved enterprise policy;
8. approved enterprise standard / control baseline;
9. recognised industry standard;
10. architecture or engineering best practice;
11. expert judgement.

Never describe item 8–11 as a legal obligation unless an authoritative source incorporates it.

## 3. Core India regulatory registry

| Domain | Authority / source family | Applicability rule |
|---|---|---|
| Insurance | Insurance Act and applicable IRDAI regulations/directions | Determine based on insurer/intermediary/corporate-agent role and activity |
| Policyholder protection | IRDAI (Protection of Policyholder's Interests, Operations and Allied Matters of Insurers) Regulations, 2024 and relevant master circulars | Apply to relevant insurance solicitation, sale, servicing, claims and customer treatment |
| Insurance cyber security | IRDAI Information and Cyber Security Guidelines, 2023 and subsequent applicable instructions | Apply based on the regulated insurance entity and scope prescribed by IRDAI |
| Digital personal data | Digital Personal Data Protection Act, 2023 | Evaluate where digital personal data processing falls within its scope |
| DPDP implementation | Digital Personal Data Protection Rules, 2025, corrigenda and commencement/enforcement notifications | Apply provisions according to their legally effective dates |
| Cyber incidents | CERT-In directions under Section 70B of the IT Act and applicable FAQs/guidance | Apply to covered entities/systems and incident/security obligations |
| Banking IT governance | RBI Master Direction on IT Governance, Risk, Controls and Assurance Practices and applicable cyber instructions | Apply where the bank or RBI-regulated entity is responsible/in scope |
| IT outsourcing | RBI Master Direction on Outsourcing of Information Technology Services | Apply to covered RBI-regulated entities and relevant outsourcing arrangements |
| KYC/AML | PMLA, rules, applicable IRDAI/RBI KYC/AML requirements | Determine role and transaction-specific applicability |
| Payments | RBI/payment-system rules plus PCI DSS where payment-account data is handled | Apply according to payment design and data actually processed |
| Electronic records | Information Technology Act and applicable rules | Apply to electronic records, security and related legal requirements as relevant |
| AI governance | Applicable Indian law/regulatory guidance plus enterprise AI policy; ISO/IEC 42001 and ISO/IEC 23894 as frameworks | Treat standards as frameworks unless adopted/contractually mandated |

## 4. Authoritative source locations

The governed knowledge base should ingest sources directly from authoritative publishers where possible:

- IRDAI: `https://irdai.gov.in/`
- MeitY: `https://www.meity.gov.in/`
- CERT-In: `https://www.cert-in.org.in/`
- RBI: `https://www.rbi.org.in/`
- India Code / Gazette sources where necessary for statutory text
- ISO catalogue for standard identity/version (licensed standard content must respect licensing)
- OWASP for open application/API security standards
- PCI Security Standards Council for PCI DSS source material

## 5. Source metadata required in retrieval corpus

Every regulatory/control document should carry:

- canonical title;
- issuing authority;
- reference/circular/regulation number where available;
- publication date;
- effective date;
- superseded date if applicable;
- status: draft / issued / effective / partially effective / superseded / withdrawn;
- entity applicability;
- topic tags;
- source URL or internal authoritative repository location;
- last verification date;
- owner;
- change history.

## 6. Applicability test

Before citing a requirement, determine:

1. Which legal entity is acting?
2. Is it a bank, insurer, corporate agent, intermediary, vendor or processor/service provider?
3. Which customer and product journey is involved?
4. What data/action/system is in scope?
5. Is the source effective for the date being assessed?
6. Does the source impose a direct obligation or only guidance?
7. Is responsibility retained by the regulated entity even when execution is outsourced?

If any of these materially change the result, mark the decision `REQUIRES_CLARIFICATION` or `ESCALATE` rather than guessing.

## 7. Regulatory-change watch

The registry owner should review at least:

- new/superseded IRDAI regulations and master circulars;
- MeitY DPDP notifications, rules and commencement changes;
- CERT-In directions and security guidance;
- RBI directions relevant to the bank/platform;
- material KYC/AML updates;
- enterprise compliance policies that implement these obligations.

A regulatory update should trigger impact analysis against:

- controls;
- open exceptions;
- architecture decisions;
- products/journeys;
- vendors;
- data-retention schedules;
- AI-agent rules.
