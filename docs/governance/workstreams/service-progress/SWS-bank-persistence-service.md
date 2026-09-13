# SWS-bank-persistence-service — Platform Persistence

| | |
|---|---|
| **SWS ID** | `SWS-bank-persistence-service` |
| **Module** | `bank-persistence-service` |
| **Service id** | `persistence` |
| **Port** | `8081` |
| **Cluster** | platform-common |
| **Catalogue status** | `implemented` |
| **Program lane** | `WS-NS` (North Star Continuity) |
| **Board opened** | 2026-09-13 |
| **Architecture approval** | Mahesh · 2026-09-13 · CR-016 / ADR-020 |

Implemented; platform-common persistence for job-store and audit HTTP.

Standing rule: update this board in the same change that moves work. One in-flight item per owning agent lane. Cross-service Done requires the [dependency sync check](../WORKSTREAM-STRATEGY.md#53-dependency-sync-check).

## Active

| Work item | Owner | Started | Notes |
|---|---|---|---|
| — | — | — | No item in flight |

## Completed

| Work item | Completed | Evidence |
|---|---|---|
| — | — | — |

## Waiting to unblock

| Work item | Waiting on (SWS / DEP) | Since | Unblock trigger |
|---|---|---|---|
| — | — | — | None |

## Known peer services

- Consumers: integration, audit writers over `/internal/v1`

## Sync log

| Date | Peer SWS | Check | Result | Evidence |
|---|---|---|---|---|
| — | — | — | — | — |
