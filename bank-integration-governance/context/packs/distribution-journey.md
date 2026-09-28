---
lane: distribution-journey
owner: product
verified_on: 2026-09-28
---

# Distribution journey (R0 assisted Life sale) — context pack

## Summary
R0: one RM sells one Life policy (Term or Savings/ULIP) to one existing-to-bank customer from one
Group A insurer, end to end — lead, customer prefill, consent, suitability, quote, proposal,
payment on the customer's device, reconciled issuance, full audit trail. Services are scaffolded
(M0–M1); behaviour SSOT is the September 2026 BRD pack (DOC-005). Most common agent error:
inventing a rule or formula the BRD leaves open.

## Outcome now
- **O-DJ-1** Consent and suitability rule packs implemented as configurable, evidenced controls.
- **O-DJ-2** Lead → quote thin slice against stubs, end to end in dev.

## Contracts
- Hub-facing: integration-hub contracts (bank language only).
- BFF-facing: `docs/platform/ws3-platform/nip-bff-lead-phase.openapi.yaml`, `09-nip-bff-customer-search-contract.md`.

## Invariants in play
GR-JNY-01, GR-JNY-02, GR-JNY-03, GR-JNY-04, GR-PAY-01, GR-PAY-02, GR-ARC-05, GR-ARC-06, GR-IAM-03, GR-DAT-03.

## Open decisions
- DEC-OPEN-BRD-C1C2 BI lock / Eligible lead timing — Rajal answered (D-015: on successful final BI).
- DEC-OPEN-BRD-C5 buy outside suitable set — product intent D-017; **Compliance must decide**.
  **Default:** GR-JNY-01 stands; build the rule behind configuration, off.
- DEC-OPEN-BRD-C7 exception multi-rule handling. **Default:** first-match, configurable.
- DEC-OPEN-GAP006-007 consent / suitability sign-off by Compliance. **Default:** rule packs v1 as configuration; gates prod.

## Known debt — do not re-report
Scaffolded services are skeletons — module presence is not delivery.

## Where truth lives
1. `docs/au-bank-insurance-platform/requirements/brd-detailed/` (DOC-005) and `BRD-ALIGNMENT-2026-09-28.md`
2. `docs/au-bank-insurance-platform/DECISION-LOG.md` (D-xxx, DOC-xxx)
3. `docs/au-bank-insurance-platform/rule-packs/consent-rule-pack.md`, `suitability-rule-pack.md`
4. `docs/platform/ws3-platform/01-domain-model-and-invariants.md`

## Gotchas
- Figma is reference, not behaviour SSOT (D-012).
- "Policy Sold" is never inferred from quote, proposal or payment alone.
- Off-platform sales enter as Policy `source=OFF_PLATFORM`, never via `lead.create`.
