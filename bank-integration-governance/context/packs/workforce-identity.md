---
lane: workforce-identity
owner: security
verified_on: 2026-09-28
---

# Workforce identity & access — context pack

## Summary
Provider-neutral workforce identity for bank employees and insurer representatives: a
token-hiding BFF session (`workforce-access-bff`), the IdP behind
`identity-provider-adapter-service` (Keycloak first), and `identity-authorization-service` as the
business PDP (RBAC + ABAC, default deny). Maturity ≈ M1–M2; no evidence has moved for weeks.
Most common agent error: treating the IdP as the authority for business permissions.

## Outcome now
- **O-WI-1** Token-hiding BFF, isolated IdP and PDP default-deny proven with tests (old GATE-IAM-P1 A.1–A.3).
- **O-WI-2** Maker-checker, retention and provisioning outbox (old A.4–A.6) — none of these depend on the AD decision.

## Contracts
- `services/workforce-access-bff` session API; login BFF evaluated in `docs/au-bank-insurance-platform/requirements/LOGIN-BFF-FIGMA-EVALUATION.md`.
- PDP decision API in `services/identity-authorization-service`.

## Invariants in play
GR-IAM-01, GR-IAM-02, GR-IAM-03, GR-DAT-01.

## Open decisions
- DEC-OPEN-AD-TECH — bank AD technology (OIDC / SAML / LDAP) and ASM-019 (credentials verified by an
  existing bank API via Apigee). **Default:** adapter port with a stub verifier; no federation code.
- DEC-OPEN-PROD-IDP — production IdP choice. **Default:** Keycloak behind the adapter, dev only.
- DEC-OPEN-RETENTION-AUTH — 7-year retention for auth/admin events (ASM-005). **Default:** retention configurable, 7 years set.

## Known debt — do not re-report
None recorded beyond AIGEM TD list.

## Where truth lives
1. `docs/platform/authentication-authorization/README.md` (SSOT)
2. `docs/architecture/2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md`
3. Rajal Login BRD chapter in `docs/au-bank-insurance-platform/requirements/brd-detailed/`

## Gotchas
- Every change here is *near* G1; only changes that **alter** access decisions are R2.
- The client never sees OAuth tokens — not in redirects, not in error bodies, not in logs.
