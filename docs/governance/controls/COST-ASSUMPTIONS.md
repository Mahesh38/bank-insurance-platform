# Cost assumption notes (CR-012 / RISK-012 / NFR-OPEN-6)

**signature_status:** `AI-DRAFTED — assumptions, not an approved envelope. DEP-20260824-cst remains OPEN.`

`dev` is not production-shaped (`R0-LLD` §1.4). A `prod` control is not lowered to fit a cost conversation without Security + SRE verdicts.

## What must be priced before first `uat` apply

- Three stateful managed services (MSK, Valkey, OpenSearch) per environment shape
- Inspection VPC + Network Firewall endpoints
- Sixth AWS account / Control Tower line if still in the S09 BOM
- Site-to-Site VPN (and DX when ordered)
- Cloudflare / F5 / Apigee run-rate (enterprise, not invented here)

## What this repository does not have

A number. No agent will invent INR/USD. Kalpana + Shivanshi produce `NFR-OPEN-6`. GATE-S09 **entry** stays blocked until that envelope exists.

Pilot load used for conversation only: ~100 journey starts an hour (RISK-012). That is availability/evidence pricing, not a business-case premium forecast.
