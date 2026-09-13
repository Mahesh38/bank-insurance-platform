# Service progress boards

**Authority:** [WORKSTREAM-STRATEGY.md](../WORKSTREAM-STRATEGY.md) · [CR-016](../../change-requests/CR-016-north-star-and-service-workstream-strategy.md) · [ADR-020](../../../platform/architecture-review/ADR-020-north-star-and-service-workstream-strategy.md)  
**Approved (Architecture):** Mahesh · 2026-09-13

One markdown board per microservice. Agents owning a service update that board in the same
change that moves work. Boards are progress ledgers — they do **not** carry lifecycle stage.

## Index

| SWS ID | Service | Module | Cluster | Board |
|---|---|---|---|---|
| SWS-1sb-integration-service | 1SB Adapter | `1sb-integration-service` | WS-1 | [board](./SWS-1sb-integration-service.md) |
| SWS-bank-persistence-service | Platform Persistence | `bank-persistence-service` | platform-common | [board](./SWS-bank-persistence-service.md) |
| SWS-identity-provider-adapter-service | Identity Provider Adapter | `identity-provider-adapter-service` | WS-2 | [board](./SWS-identity-provider-adapter-service.md) |
| SWS-identity-authorization-service | Identity & Access (PDP) | `identity-authorization-service` | WS-2 | [board](./SWS-identity-authorization-service.md) |
| SWS-workforce-access-bff | Workforce Access BFF | `workforce-access-bff` | WS-2 | [board](./SWS-workforce-access-bff.md) |
| SWS-customer-service | Customer | `customer-service` | WS-3 | [board](./SWS-customer-service.md) |
| SWS-lead-service | Lead | `lead-service` | WS-3 | [board](./SWS-lead-service.md) |
| SWS-consent-service | Consent | `consent-service` | WS-3 | [board](./SWS-consent-service.md) |
| SWS-suitability-service | Suitability | `suitability-service` | WS-3 | [board](./SWS-suitability-service.md) |
| SWS-product-catalogue-service | Product Catalogue | `product-catalogue-service` | WS-3 | [board](./SWS-product-catalogue-service.md) |
| SWS-journey-orchestration-service | Journey Orchestration | `journey-orchestration-service` | WS-3 | [board](./SWS-journey-orchestration-service.md) |
| SWS-quotation-service | Quotation | `quotation-service` | WS-3 | [board](./SWS-quotation-service.md) |
| SWS-proposal-service | Proposal & UW-Tracking | `proposal-service` | WS-3 | [board](./SWS-proposal-service.md) |
| SWS-payment-service | Payment | `payment-service` | WS-3 | [board](./SWS-payment-service.md) |
| SWS-policy-issuance-service | Policy & Issuance | `policy-issuance-service` | WS-3 | [board](./SWS-policy-issuance-service.md) |
| SWS-integration-hub-service | Integration Hub | `integration-hub-service` | WS-3 / WS-1 | [board](./SWS-integration-hub-service.md) |
| SWS-audit-compliance-service | Audit & Compliance | `audit-compliance-service` | WS-3 | [board](./SWS-audit-compliance-service.md) |
| SWS-notification-service | Notification | `notification-service` | WS-3 | [board](./SWS-notification-service.md) |
| SWS-reporting-mis-service | Reporting & MIS | `reporting-mis-service` | WS-3 | [board](./SWS-reporting-mis-service.md) |
| SWS-administration-config-service | Administration & Config | `administration-config-service` | WS-3 | [board](./SWS-administration-config-service.md) |
| SWS-direct-insurer-adapter-service | Direct Insurer Adapter | `direct-insurer-adapter-service` | WS-3 / WS-1 | [board](./SWS-direct-insurer-adapter-service.md) |

## How to update

1. Move at most one item into **Active** for the owning agent lane.
2. When Done, move it to **Completed** with evidence (PR, test path, ADR).
3. When blocked by another service, add a row under **Waiting to unblock** on *both* boards.
4. Run the [dependency sync check](../WORKSTREAM-STRATEGY.md#53-dependency-sync-check) before clearing a cross-service wait.
