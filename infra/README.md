# Infrastructure as code — GATE-S09

**Workstream:** WS-3 · **Stage:** S09 — Platform & Environment Foundation  
**Stories:** S09-E01-S01 (this scaffold) onward  
**Region pin:** `ap-south-1` primary · `ap-south-2` DR only

## What this is

The Terraform root and module layout for the bank insurance platform. No production resource
is created by console (S09-G1). Environments share modules with different tfvars (S09-G2).

## Layout

```
infra/
  versions.tf / variables.tf   # provider + India-region validation
  modules/                     # reusable modules (empty scaffold)
  envs/{dev,uat,prod}/         # per-environment tfvars + backend.hcl
  policies/region-pin.rego     # OPA policy refusing non-India regions
  bootstrap/                   # one-time human state-backend bootstrap
```

## Current status (2026-09-14)

| Item | State |
|---|---|
| IaC repo / module standard (S09-E01-S01) | **Started** — this tree |
| Remote state + locking (S09-E01-S02) | Not started — needs human AWS account bootstrap |
| Network / compute / data foundations | Not started |
| GATE-S09 criteria | 0/13 MET |

## Non-goals

- Applying anything to a real AWS account from this PR
- Claiming S09-G1 MET (scaffold ≠ estate defined as code)
- Starting WS-1 Phase 5
