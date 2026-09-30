# Operational Runbook

**Owner:** Shivanshi / SRE  
**CR:** CR-010 OPS-C2, CR-012 Board 7 condition 1, WS-1 criterion 4.5  
**signature_status:** `AI-DRAFTED — required before uat; drills not executed`

No new tier reaches `uat` without its section below being staffed. This file is the named runbook; it is not evidence that a drill ran.

## 0. WS-1 operations (criterion 4.5)

| Failure | Action |
|---|---|
| Secrets rotation | Rotate in the secret store; never commit values; fail-fast if AWS provider still stubbed (TD-006 / prod profile) |
| IP / Apigee allowlist miss | Do **not** send spoke NAT EIPs to 1SB while `ASM-015` is the working belief. Chase DEP-20260824-eip. Quotes timing out in UAT with green code is this failure |
| 1SB 401 | **Never retry 401.** Treat as credential/config incident. Page Engineering + SRE |
| 1SB 5xx | Back off per published poll/retry policy; trip circuit breaker; do not widen retries against the aggregator |

## 1. Broker / MSK (when provisioned)

Lag, broker loss, outbox replay (`NFR-EVT-03`). Recovery is **replay from the outbox**, not "the topic is the record".

## 2. Cache / Valkey

Failover and mass-logout. Cache is never SoR, never idempotency, never evidence (`ADR-011`, `FF-23`). Uncertain payment state is recovered by **reconciliation, not restore** (DB-C9 / DB-R5).

## 3. Search / OpenSearch

Index pressure, ISM failure. Holds **no** regulatory evidence (`FF-27`/`FF-28`). A 90-day ISM delete is not a retention violation because the store is not the archive.

## 4. Firewall / inspection VPC

Rule rollback is an **outage** runbook. Named curator: Shivanshi. Every new destination arrives in the same PR as the code that needs it.

## 5. Connectivity

DX → VPN failover and back (`NFR-NET-01`). Bank path is DEP-20260824-dx1 (external). `dev` stubs remain legitimate; `uat`/`prod` must not use CBS/AD stubs once the VPN half exists.

## S09 drill calendar (condition: schedule at start of S09, not the end)

| Drill | NFR | Plan placement | Executed? |
|---|---|---|---|
| DX→VPN failover | NFR-NET-01 | First two weeks of S09 build | **No** |
| Outbox replay | NFR-EVT-03 | First two weeks of S09 build | **No** |
| Cache failover with sessions held | NFR-CAC-01/02 | First two weeks of S09 build | **No** |

These dates are sequencing, not evidence.
