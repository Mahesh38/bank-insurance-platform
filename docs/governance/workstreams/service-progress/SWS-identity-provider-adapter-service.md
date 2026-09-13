# SWS-identity-provider-adapter-service — Identity Provider Adapter

| | |
|---|---|
| **SWS ID** | `SWS-identity-provider-adapter-service` |
| **Module** | `identity-provider-adapter-service` |
| **Service id** | `idp-adapter` |
| **Port** | `8082` |
| **Cluster** | WS-2 |
| **Catalogue status** | `implemented` |
| **Program lane** | `WS-NS` (North Star Continuity) |
| **Board opened** | 2026-09-13 |
| **Architecture approval** | Mahesh · 2026-09-13 · CR-016 / ADR-020 |

Implemented under WS-2 IAM.

Standing rule: update this board in the same change that moves work. One in-flight item per owning agent lane. Cross-service Done requires the [dependency sync check](../WORKSTREAM-STRATEGY.md#53-dependency-sync-check).


## Board hygiene (CR-016 conditions)

| Rule | Requirement |
|---|---|
| Work-item IDs | Every Active/Completed row cites `FUNC-###` / plan id / CR id (WS-NS-4) |
| Claim gate | No ownership claim without READY work for this module (WS-NS-5) |
| No PII/secrets | IDs and service names only (WS-NS-6) |
| Standing constraints | No Hub bypass / bank→1SB·DB / Flutter tokens (WS-NS-7) |
| Not gate evidence | Board rows do not satisfy GATE-S08, Board 7, or regulatory packs (WS-NS-8) |
| Cross-SWS Done | Pass [SYNC-CHECK-EVIDENCE-BAR](../SYNC-CHECK-EVIDENCE-BAR.md) before clearing a wait |

## Active

| Work item ID | Owner | Started | Notes |
|---|---|---|---|
| — | — | — | No item in flight |

## Completed

| Work item ID | Completed | Evidence |
|---|---|---|
| — | — | — |

## Waiting to unblock

| Work item ID | Waiting on (SWS / DEP) | Since | DL age | Unblock trigger |
|---|---|---|---|
| — | — | — | — | None |

## Known peer services

- SWS-workforce-access-bff
- SWS-identity-authorization-service

## Sync log

| Date | Peer SWS | Check | Result | Evidence |
|---|---|---|---|---|
| — | — | — | — | — |
