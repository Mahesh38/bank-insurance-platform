# API and service catalogue

This page answers two different questions:

1. **What services/modules exist in the repository today?**
2. **What API/domain responsibilities does the architecture assign to them?**

Do not equate “module exists” with “feature is complete”. For implementation status, inspect the module README, controllers/use cases and tests.

## Executable Gradle modules currently registered

The authoritative executable-module list is `settings.gradle.kts`. At the time this portal was introduced it includes:

### Shared libraries

- `bank-common-error`
- `bank-common-domain`
- `bank-common-security`
- `bank-common-audit`
- `bank-common-observability`
- `bank-common-secrets`

### Services

| Module | Architectural purpose |
|---|---|
| `1sb-integration-service` | 1SB provider adapter / existing WS-1 integration capability |
| `administration-config-service` | Administration and configuration capability |
| `audit-compliance-service` | Audit/compliance evidence capability |
| `bank-persistence-service` | Existing integration/platform persistence support; do not assume it owns every business context |
| `consent-service` | Consent capture/evidence domain |
| `customer-service` | ETB customer lookup/prefill domain |
| `direct-insurer-adapter-service` | Direct insurer adapter seam/module; verify admitted usage before wiring it into R0 |
| `identity-authorization-service` | Business authorization/PDP source of truth |
| `identity-provider-adapter-service` | Provider-neutral authentication/IdP adapter |
| `integration-hub-service` | Bank-canonical provider routing/integration hub |
| `journey-orchestration-service` | Journey/saga state and references |
| `lead-service` | Lead working-inbox/origination domain |
| `notification-service` | Notification capability |
| `payment-service` | Bank payment session/reconciliation domain |
| `policy-issuance-service` | Policy/issuance state and visibility |
| `product-catalogue-service` | Product catalogue/configured product availability |
| `proposal-service` | Proposal and underwriting-tracking domain |
| `quotation-service` | Platform quotation aggregate and suitability-gated quote behaviour |
| `reporting-mis-service` | Reporting/MIS isolated read capability |
| `suitability-service` | Need analysis/suitability evaluation domain |
| `workforce-access-bff` | Token-hiding workforce Backend for Frontend |

## R0 responsibility map

| Capability | Key rule to remember |
|---|---|
| NIP BFF | Only workforce-facing application surface; Flutter must not receive provider OAuth tokens |
| Lead | Single on-platform origination point for R0 assisted sale |
| Customer | ETB lookup/prefill; customer is not an authenticated platform actor in assisted R0 |
| Consent | Purpose-scoped evidence; customer-device verification |
| Suitability | Hard gate before quote |
| Product Catalogue | Bank-controlled eligible product view, configuration-driven |
| Journey | Owns journey stage + references; does not duplicate other aggregates' truth |
| Quotation | Owns bank quotation state; no quote without valid suitability |
| Proposal | No submit without required valid consent |
| Payment | Customer-device payment; financial state and reconciliation belong here |
| Policy | Issuance/policy state; issue only after reconciled payment where required |
| Audit | Immutable evidence; logs are not audit evidence |
| Integration Hub | Bank-canonical contract, provider routing, server-derived attribution |
| 1SB Adapter | Provider-specific translation; 1SB vocabulary terminates inside adapter boundary |
| Reporting/MIS | Isolated read path; must not turn the Lead/RM OLTP writer into reporting storage |
| Administration/Config | Versioned/effective configuration and maker-checker administration |

## Documented R0 API families

The R0 HLD and solution-architecture documents are the starting point for target contracts. Representative families include:

| Area | Representative contract shape | Important guard |
|---|---|---|
| Workforce BFF | `/api/*` / session/workspace routes | opaque session, authorization/CSRF/idempotency controls |
| Lead | internal lead create/read/resume/status/convert/archive operations | only permitted actor may originate |
| Customer | internal ETB lookup/prefill | bank/customer source boundaries |
| Consent | challenge/verify/grant/read operations | customer-device verification; evidence immutable |
| Suitability | start/answer/complete/read evaluation | produces validity/eligibility evidence used by Quote |
| Catalogue | product/eligibility resolution | configuration-driven, LOB-aware |
| Quotation | create/read/select/compare quote operations | C1 / `INV-QUO-*` suitability gate |
| Proposal | draft/submit/status operations | C2 / `INV-PRP-*` consent gate |
| Payment | create session/callback/status/reconciliation | C4 customer-device isolation; callback authenticity; reconciliation truth |
| Policy | create/issue/read/ingest policy | reconciled payment and issuance rules |
| Hub | provider request/status contract | caller cannot supply trusted `distributorId` |
| Audit | append/query evidence | append-only / immutable evidence semantics |
| Configuration | resolve + controlled admin writes | no compiled-in business fallback |

Use [R0 HLD](../architecture/R0-HLD.md) and [R0 solution architecture](../platform/ws3-platform/03-solution-architecture-r0.md) for the current contract descriptions.

## Existing 1SB adapter API

The existing WS-1 adapter has concrete APIs that should not be confused with the future/platform domain APIs. Key families include:

- `POST /v1/quotes` — create asynchronous quote job
- `GET /v1/quotes/{jobId}` — poll quote job/result
- proposal schema / submit / poll APIs
- `POST /v1/payments` — provider/integration payment-session capability
- application/status APIs

These are **adapter capabilities**. The intended R0 chain is conceptually:

```text
NIP-APP
  → NIP BFF
  → platform domain service
  → Integration Hub
  → 1SB Adapter
  → 1SB / insurer
```

A platform domain service should not call 1SB directly.

## How to inspect one service as a developer

For any module, read in this order:

1. module `README.md`
2. controller/API package — what endpoints actually exist
3. application/use-case layer — orchestration/business command handling
4. domain package — aggregates, value objects, invariants
5. ports/interfaces — dependencies the domain expects
6. adapters — HTTP/provider/persistence implementations
7. tests — what behaviour is actually evidenced
8. architecture/invariant docs — what behaviour must exist even if code is incomplete

## API status labels to use in this portal

When this catalogue is expanded, every API should carry one of:

- **IMPLEMENTED** — endpoint/code exists.
- **IMPLEMENTED-PARTIAL** — endpoint exists but target behaviour/evidence is incomplete.
- **CONTRACTED** — approved/ratified contract exists but implementation does not.
- **PROPOSED** — architecture/design describes it, approval still outstanding.
- **DEFERRED** — valid future contract, deliberately out of current release/stage.

This prevents the common mistake “I saw an endpoint in the HLD, therefore it exists in code.”
