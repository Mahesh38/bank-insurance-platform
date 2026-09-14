# Terraform modules

Reusable modules for GATE-S09. Every production resource is created through a module here —
never by console (S09-G1).

## Conventions

- One concern per module (`network`, `eks`, `aurora`, `kms`, `observability`, …).
- Modules take `environment` and `aws_region`; region validation rejects non-India regions.
- No hard-coded account IDs or secrets.
- Outputs are the only cross-module contract.

## Status

Scaffold only (S09-E01-S01). Modules land as subsequent stories close S09-G1…G13.
