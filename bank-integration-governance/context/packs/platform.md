---
lane: platform
owner: reliability
verified_on: 2026-09-28
---

# Platform (CI/CD, IaC, environments, connectivity) — context pack

## Summary
Engineering foundation is done: CI on every PR, merge blocked without green checks, coverage,
ArchUnit, static analysis, secret/SAST/SCA/image scans, PII-in-logs test (old GATE-S08: 10/10 met).
Platform foundation is **not started**: Terraform/IaC, AWS accounts in ap-south-1, secrets manager,
observability, 7-year write-once retention, bank connectivity. Most common agent error: waiting for
a stage signature before starting IaC — there is no such dependency now.

## Outcome now
- **O-PL-1** dev environment in AWS ap-south-1 from Terraform, with secrets and observability baseline.
- **O-PL-2** Priced cost envelope for the R0 platform layers (unblocks account structure).

## Contracts
- `.github/workflows/application-ci.yml`, `security-scanning.yml`; ruleset 23340894.
- Reference architecture: `docs/architecture/R0-HLD.md`, `R0-LLD.md`.

## Invariants in play
GR-DAT-02, GR-ENG-01, GR-ENG-02, GR-ARC-07.

## Open decisions
- DEC-OPEN-COST — cost envelope for ADR-009…013 layers (RISK-012). **Default:** build dev with minimal sizes; price from Terraform plans.
- DEC-OPEN-APIGEE (shared with integration-hub).
- ADR-001, ADR-009…013, ADR-016, ADR-018, ADR-020 are `Proposed`. **Default:** build to them in dev; they gate UAT.

## External dependencies
DEP-VPN-DX (bank VPN/DX), DEP-ALLOWLIST (1SB/PG allowlist of Apigee IPs), DEP-APIGEE — see `state/dependencies.yaml`.

## Where truth lives
1. `docs/architecture/R0-HLD.md`, `R0-LLD.md`, `ARB-ARCHITECTURE-DOSSIER.md`
2. Decision register ADR-001, 009–013, 016, 018, 020; ASM-009…019
3. `docs/platform/engineering/`

## Gotchas
- dev is a slice inside the UAT account (ASM-017); no CUG environment at R0 (ASM-018).
- Render.com is dev-preview only and never carries PII (GR-DAT-02).
- Scaling claims need load, bottleneck and downstream limit (reliability card).
