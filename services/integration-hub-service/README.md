# Integration Hub Service

**Status:** PARTIAL — FUNC-032 QUOTE submit/poll to `1sb-integration-service` (EPIC-008). Proposal and sync queries still planned.

| Field | Value |
|---|---|
| Bounded context | #14 |
| Gradle module | `services/integration-hub-service` |
| GitLab group (proposed) | `ws3-integration` |
| HTTP port (local) | 8100 |
| Error contract `service-id` | `integration-hub` |
| Target datastore | Aurora `hub.provider_request` (HUB-1); interim in-memory store for FUNC-032 |

## Purpose

Provider-neutral door from Quotation / Proposal / Policy to insurance adapters (`IF-1`). Callers never
talk to `1sb-integration-service` directly. Contract:
[`docs/platform/contracts/integration-hub-service/`](../../docs/platform/contracts/integration-hub-service/).

## Built now (FUNC-032)

| Method | Path | Behaviour |
|---|---|---|
| `POST` | `/internal/v1/provider-requests` | `operation=QUOTE` only — route LIFE+TERM\|SAVINGS\|ULIP → ONE_SB, reject attribution, map canonical→adapter, submit once |
| `GET` | `/internal/v1/provider-requests/{id}` | Poll adapter job; map COMPLETED / PARTIAL / FAILED / TIMEOUT |

## Package layout

```text
com.bank.platform.integration.hub
├── api/                 ProviderRequestController
├── application/         SubmitQuoteService, GetProviderRequestService
├── domain/              ports, routing seed, attribution guard
├── adapter/onesb/       RestClient to 1sb-integration /v1/quotes
├── adapter/persistence/ InMemoryProviderRequestStore (HUB-1 replaces)
└── config/
```

## Run locally

```bash
./gradlew :services:integration-hub-service:test jacocoTestCoverageVerification
./gradlew :services:integration-hub-service:bootRun --args='--spring.profiles.active=local'
```

Inbound key (quotation): `INTEGRATIONHUB_QUOTATION_INTERNAL_KEY` (default
`local-integrationhub-quotation-internal-key`). Outbound to 1SB must match
`ONESB_HUB_INTERNAL_KEY`.
