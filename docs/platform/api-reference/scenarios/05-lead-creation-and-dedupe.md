# Scenario 05 — The RM creates (or resumes) a lead

[← API reference home](../README.md) · Prev: [04](./04-customer-search.md) · Next: [06](./06-lead-onboarding-and-assignment.md)

**Screens:** `SCR-05` product class → create. **Status:** IMPLEMENTED-PARTIAL (persisted in `lead_lms.lead`; BFF asks the PDP for
`lead.create`; no idempotency key enforcement; no outbox yet).

**Canonical order (`D-019`):** search → pick product class → **dedupe** → **Save** (this scenario)
→ Start Onboarding (exception rules) → assign SP → suitability. **Save never runs exception rules
and does not require an SP assignee.**

## 1. Sequence

```mermaid
sequenceDiagram
    autonumber
    participant App as NIP-APP
    participant BFF as BFF
    participant PDP as identity-authorization (PDP)
    participant LS as lead-service
    participant DB as lead_lms.lead
    App->>BFF: GET /api/v1/catalogue/product-classes
    BFF-->>App: LIFE/TERM, LIFE/SAVINGS, LIFE/ULIP (static today)
    App->>BFF: GET /api/v1/customers/{customerId}/active-leads?productClass=TERM
    BFF->>LS: GET /internal/v1/leads?owner=me (X-Actor-Id)
    LS-->>BFF: actor's book
    BFF-->>App: {items: non-terminal leads for this customer + class, hasMore}
    alt an unfinished lead exists
        Note over App: Popup: Continue existing | Cancel (no Delete / Replace — Lead BRD Table 18)
        App->>BFF: GET /api/v1/leads/{existingLeadId}  (Continue)
    else none
        App->>BFF: POST /api/v1/leads {customerId, lob: LIFE, productClass: TERM, branchId}
        BFF->>BFF: @NotBlank customerId, lob, productClass
        BFF->>PDP: decide lead.create {branchCode: branchId} — 300 ms, fail closed
        PDP-->>BFF: allowed (deny → 403 OUT_OF_BRANCH_SCOPE / DEFAULT_DENY …)
        BFF->>LS: POST /internal/v1/leads (X-Actor-Id = businessUserId)
        LS->>LS: customerId/actor present · lob == LIFE · productClass ∈ TERM|SAVINGS|ULIP
        LS->>DB: find unfinished (creator, customer, class)
        alt duplicate (race with another tab)
            LS-->>BFF: 409 CONFLICT errors[0] = {field: leadId, message: existing ULID}
            BFF-->>App: 409 → same popup
        else
            LS->>DB: INSERT lead (ULID, NEW, NOT_EVALUATED, DIARY) [+ idempotency + outbox OpportunityCreated — target]
            LS-->>BFF: 201 Lead
            BFF-->>App: 201 LeadRecord
        end
    end
```

## 2. Request and response

```http
POST /api/v1/leads
Idempotency-Key: 6c0e…   (contract)
X-XSRF-TOKEN: …          (web)

{ "customerId": "CUST-0433", "lob": "LIFE", "productClass": "TERM", "branchId": "BR-MUM-001" }
```

```json
{ "leadId": "01JQX4K7R8M2N3P4Q5S6T7V8W9", "customerId": "CUST-0433", "lob": "LIFE", "productClass": "TERM",
  "branchId": "BR-MUM-001", "state": "NEW", "dashboardStage": "NEW", "assignedRmId": null, "assignedSpId": null,
  "exceptionHold": false, "exceptionOutcome": "NOT_EVALUATED", "exceptionRequired": false,
  "journeyId": "01JQX4K7R8M2N3P4Q5S6T7V8W9", "createdAt": "…", "updatedAt": "…" }
```

## 3. Validation summary

| Where | Rule | Error |
|---|---|---|
| BFF | `customerId`, `lob`, `productClass` non-blank | `422 VALIDATION_ERROR` |
| Lead | `lob = LIFE` (R0) | `422 UNSUPPORTED_LOB` |
| Lead | product class `TERM` / `SAVINGS` / `ULIP` | `422 UNSUPPORTED_LOB` |
| Lead | no unfinished lead for creator + customer + class | `409 CONFLICT`, `errors[0].field = leadId`, `errors[0].message` = the existing lead's ULID (Continue target); a race past the check hits `ux_lead_dedupe_open` → `409 CONFLICT` |
| Target | actor allowed to originate (`INV-LED-04`; PDP `lead.create`) | `403 ORIGINATION_RM_ONLY` |
| Target | customer in the accountable SP's ETB book (`INV-LED-05`) | `422 CUSTOMER_NOT_IN_BOOK` |
| Target | `distributorId` / insurer / plan in body | `422 ATTRIBUTION_NOT_CALLER_SUPPLIED` / rejected |
| Target | `Idempotency-Key` reused with a different body | `409 IDEMPOTENCY_CONFLICT` |

## 4. Data written (target)

One `lead_lms` transaction: `lead` (+ `config_versions`, `expires_at`; today `expires_at` is set
only when `lead.expiry-days` is configured — unset by default, the value is OPEN-D4 — and
`config_versions` is not yet written), `idempotency_record`,
`outbox_event OpportunityCreated` → Journey, Audit, Reporting. The lead is **not** a customer
record: it stores `customer_id` only.

## 5. Pages

[BFF §3.4–§3.6](../services/workforce-access-bff.md#35-post-apiv1leads) · [lead-service §3.1](../services/lead-service.md#31-post-internalv1leads--save) · [BFF to Lead](../interactions/bff-to-lead.md) · [Lead sequences §2](../../ws3-platform/11-lead-module-sequences.md)
