# S09-E01-S01 — IaC repository and module standard

**Date:** 2026-09-14  
**Gate criterion touched:** S09-G1 (partial progress only — not MET)  
**Owner seat:** Shivanshi / SRE (agent implementing under GATE-S09 OPEN)

## Delivered

- `infra/` Terraform root with required_version ≥ 1.6 and AWS provider ~> 5
- India-region validation on `aws_region` (`ap-south-1` | `ap-south-2`)
- `envs/{dev,uat,prod}/terraform.tfvars` sharing the same root (S09-G2 direction)
- `policies/region-pin.rego` OPA deny for non-India regions
- `bootstrap/README.md` for human-operated state backend creation
- Module conventions in `modules/README.md`

## Explicitly not claimed

- S09-G1 MET — no real resources exist yet; console-vs-IaC inventory not possible
- Remote state bucket/lock table — requires human AWS access (S09-E01-S02)
