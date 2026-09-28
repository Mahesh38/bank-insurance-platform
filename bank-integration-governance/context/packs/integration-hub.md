---
lane: integration-hub
owner: architecture
verified_on: 2026-09-28
---

# Integration Hub & 1SB adapter — context pack

## Summary
Bank apps call `1sb-integration-service`, which exposes LOB-routed insurance APIs (quote, proposal,
status, masters) and translates them to 1SB. It owns **no database**: job store and audit go over
internal HTTP to `bank-persistence-service`. Term path is built and hardening (≈ M3); Savings and
ULIP handlers are in progress under EPIC-002 (M1–M2). Most common agent error: letting 1SB types
or wire codes leak outside `adapter.onesb.*`.

## Outcome now
- **O-IH-1** Term path usable in bank-UAT by one bank caller — see `state/outcomes.yaml`.
- **O-IH-2** Life LOB coverage (Term + Savings + ULIP) with typed payloads and explicit resilience.

## Contracts
- Service API: `docs/1sb-insurance-integration/api-catalog/` and the service's OpenAPI (publication is O-IH-1 evidence).
- 1SB endpoints and mandatory fields: `docs/1sb-insurance-integration/field-guides/`, `reference/extracted-schemas/`.
- Persistence: `services/bank-persistence-service` `/internal/v1` DTOs.

## Invariants in play
GR-ARC-01, GR-ARC-02, GR-ARC-03, GR-ARC-04, GR-ARC-07, GR-DAT-01, GR-ENG-01, GR-ENG-02.

## Open decisions
- DEC-OPEN-APIGEE — Apigee product onboarding and egress IP list. **Default:** configurable adapter
  base URL; build and test against WireMock/sandbox.
- DEC-OPEN-LOB-UNFREEZE — whether Health/Motor exploration may start before the R0 journey is
  prod-ready. **Default:** no Health/Motor code; contract study as `Explore` only.

## Known debt — do not re-report
TD-006 Secrets Manager stub (prod fails fast) · TD-007 ArchUnit `allowEmptyShould` · TD-009 missing
domain ports · TD-010 in-memory idempotency (single instance only) · TD-022 payment intimation ·
TD-023 raw payload capture breadth (scope set by compliance review) · QA-014 package coverage floors.

## Where truth lives
1. `docs/1sb-insurance-integration/service-ssot/README.md` (SSOT for this service) and `PRODUCT-BACKLOG.md`
2. `docs/1sb-insurance-integration/service-ssot/RESILIENCE-POLICY.md`
3. `docs/governance/plans/PLAN-EPIC-002-life-lob-adapter.md`, CR-014
4. `docs/architecture/2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md`, ADR-020 (decision register)
5. ASM-014 (quote masters gateway-common, proposal masters insurer-specific), ASM-015 (outbound via Apigee)

## Gotchas
- 1SB allowlists **Apigee** IPs, not NAT EIPs (ADR-020 amends ADR-010). Never send a NAT list.
- The 1SB sandbox is unstable; CI E2E may be a gated nightly (ASM-003) — do not hide flakes with retries.
- Horizontal scale-out is unsafe until idempotency moves off-heap (TD-010) — a UAT checkpoint item, not a reason to stop feature work.
- 1SB master lookups are a provider feed behind the adapter, never a frontend contract (GR-ARC-05).
