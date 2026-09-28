---
lane: experience-bff
owner: architecture
verified_on: 2026-09-28
---

# Experience BFF (NIP) — context pack

## Summary
The NIP BFF is the only edge the workforce client talks to. The client (NIP-APP, Flutter) lives in
another repository (DOC-006; ADR-015 stands: one client, roles not apps). BFF contracts are
evaluated against Figma as the visual reference, and against BRDs for behaviour. Maturity ≈ M1.
Most common agent error: shaping a BFF contract after a provider payload instead of the screen's need.

## Outcome now
- **O-XB-1** Lead-phase and customer-search contracts stable enough for the client team to build against a stub.

## Contracts
- `docs/platform/ws3-platform/nip-bff-lead-phase.openapi.yaml`
- `docs/platform/ws3-platform/07-nip-bff-lead-phase-api-lld.md`, `09-nip-bff-customer-search-contract.md`

## Invariants in play
GR-ARC-05, GR-IAM-01, GR-IAM-03, GR-DAT-01.

## Open decisions
- DEC-OPEN-BRD-Q8 "PWA" wording vs Flutter NIP-APP. **Default:** ADR-015 (Flutter) until Product says otherwise.

## Where truth lives
1. `docs/figma/README.md` (visual reference) · BRD chapters (behaviour)
2. `docs/governance/plans/PLAN-004-*.md`, `PLAN-005-*.md`

## Gotchas
- No 1SB or insurer wire codes in any BFF response.
- The client repo is outside this programme's repository — publish contracts, do not edit client code.
