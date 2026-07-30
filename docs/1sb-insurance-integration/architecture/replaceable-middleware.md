# Replaceable middleware architecture

## Goal

Use 1SB now without forcing a rewrite when the bank builds its own aggregator / direct insurer connectors.

## Layered design

```text
┌──────────────────────────────────────────────────────────────┐
│ Presentation                                                 │
│  - Customer app / netbanking / mobile                        │
│  - RM assisted desk / tablet journey                         │
└────────────────────────────┬─────────────────────────────────┘
                             │ bank DTOs only
┌────────────────────────────▼─────────────────────────────────┐
│ Experience & Orchestration (Bank)                            │
│  - Suitability / need analysis                               │
│  - Journey state machine                                     │
│  - RM assignment, audit, disclosures                         │
│  - Prefill from CIF / CKYC / account data                    │
└────────────────────────────┬─────────────────────────────────┘
                             │ canonical commands/queries
┌────────────────────────────▼─────────────────────────────────┐
│ Bank Insurance Gateway (anti-corruption layer)               │
│  Ports: QuotePort, ProposalPort, PaymentPort,                │
│         StatusPort, DocumentPort, MasterDataPort,            │
│         EligibilityPort, IdentityVerificationPort            │
└────────────────────────────┬─────────────────────────────────┘
                             │ adapter interface
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
 ┌──────────────┐   ┌──────────────┐   ┌────────────────────┐
 │ 1SB Adapter  │   │ Insurer A    │   │ Future Aggregator  │
 │ (phase 1)    │   │ Adapter      │   │ Adapter            │
 └──────┬───────┘   └──────────────┘   └────────────────────┘
        │
        ▼
  1SB APIs (Basic Auth)
```

## Ports (stable bank interfaces)

Define these as the **only** contracts UI/orchestration may call:

| Port | Responsibility | 1SB maps to |
|------|----------------|-------------|
| `MasterDataPort` | Enums / lookup values | `POST /v1/master/lookup` (+ motor lookers) |
| `EligibilityPort` | Product gate criteria get/submit | Term/Saving/Annuity/Pension gateCriteria APIs |
| `ProductContentPort` | Brochures, UI copy, plan details | getProductUIData, View Plan Details, Health config |
| `QuotePort` | Create quote + poll | LOB `.../quote` + `.../quote/poll/{requestId}` |
| `ProposalPort` | Get dynamic form, submit, poll | `.../proposal` GET/POST + proposal poll |
| `StatusPort` | Application / policy / premium status | Application Status / Motor Proposal Status |
| `RequirementPort` | Pending docs/medicals | Get Requirement |
| `DocumentPort` | Upload / download | Doc upload / download / motor policy download |
| `PaymentPort` | Create pay URL + intimation | Payment URL (+ LOB-specific) / Payment Intimation |
| `IdentityVerificationPort` | OTP, CKYC, penny drop, customer info | Building blocks |
| `AgentPort` | SP/PoSP validation | Get SP Data |

## Adapter rules (non-negotiable)

1. **Map at the edge.** Convert bank canonical → 1SB JSON inside the adapter only.
2. **Normalize statuses.** Map 1SB `applicationStatus` + manufacturer substatus → bank enum.
3. **Keep correlation ids.** Persist `reqId`, `applicationNo`, `quoteId`, `policyNo`, manufacturer ids as external references on bank journey aggregate.
4. **Treat dynamic forms as data.** Proposal GET returns a schema; submit posts filled schema. Do not hardcode Term proposal fields into bank domain models as required columns for all LOBs.
5. **varFields are an escape hatch.** Prefer first-class canonical fields; dump leftovers into `extensions` with namespacing (`1sb.*`) so they can be dropped later.
6. **Timeouts & polling are adapter concerns.** Quote/proposal poll loops should not leak into UI as “1SB poll”; expose `QuoteJob` / `ProposalJob` with bank statuses `PENDING|PARTIAL|COMPLETE|FAILED`.

## Suggested package / module split

```text
insurance-platform/
  domain/                 # Journey, Quote, Proposal, Policy aggregates
  application/            # Use-cases: StartQuote, SelectQuote, SubmitProposal...
  ports/                  # Interfaces above
  adapters/
    1sb/
      client/             # HTTP + Basic Auth + IP egress
      mappers/            # bidirectional mappers per LOB
      polling/            # quote/proposal pollers
    bank-cif/
    bank-payments-ux/
  api/                    # Bank BFF for customer + RM apps
```

## Phased replaceability roadmap (technical, not calendar)

### Phase A — 1SB-backed

- Implement all ports with 1SB adapter.
- Launch Term + Health (highest bancassurance overlap), then Motor / Savings.
- Store full 1SB payloads only in `integration_outbox` / audit tables.

### Phase B — Dual-run ready

- Introduce `RoutingPolicy`: `provider = ONE_SB | DIRECT | BANK_MW` per LOB/product.
- Add one direct insurer adapter behind the same ports (proves abstraction).
- Feature-flag journeys by product.

### Phase C — Bank middleware

- Move multi-insurer fan-out, product rules, and payment orchestration in-house.
- 1SB becomes optional fallback or is retired per LOB.
- Canonical contracts stay unchanged → UI/RM flows untouched.

## What must stay bank-owned from day 1

- Suitability / need analysis / product recommendation logic
- RM authentication, maker-checker, branch hierarchy
- Customer consent, disclosures, audit trail
- Journey persistence and SLA timers
- Payment redirect landing UX and reconciliation ledger (even if pay page is insurer/1SB hosted)
- Mapping of bank agent codes ↔ insurer SP/PoSP codes

## Anti-patterns to avoid

| Anti-pattern | Why it blocks replacement |
|--------------|---------------------------|
| UI posts raw 1SB quote JSON | UI rewrite required later |
| Persisting only 1SB `reqId` without bank journey id | Broken ownership of state |
| Hardcoding Term proposal screens | Health/Motor/dynamic forms won’t fit |
| Using 1SB Application Layer as system of record | Harder to detach |
| Ignoring poll async model | Fragile UX and retries |
| Treating `Multi-Quote` response shape as UI model | Couples listing page to 1SB |
