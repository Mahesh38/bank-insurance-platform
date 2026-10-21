# Scenario 11 — Proposal: form, submit, underwriting tracking

[← API reference home](../README.md) · Prev: [10](./10-multi-quote-and-selection.md) · Next: [12](./12-payment-and-reconciliation.md)

**Status:** adapter half **IMPLEMENTED** (schema, submit, poll, status). Proposal service, Journey
and Hub **CONTRACTED / PROPOSED**.

## 1. Sequence

```mermaid
sequenceDiagram
    autonumber
    participant App as NIP-APP
    participant BFF as BFF
    participant JO as journey-orchestration
    participant PS as proposal-service
    participant CN as consent-service
    participant HUB as integration-hub
    participant A as 1sb-integration-service
    participant SB as 1SB / insurer
    App->>BFF: POST /api/v1/journeys/{jid}/proposals
    BFF->>JO: create proposal (stage QUOTE_SELECTED)
    JO->>PS: POST /internal/v1/proposals {quoteId, offerId, consentId}
    PS->>PS: offer is the SELECTED one · issuanceMode resolved · INSERT proposal_working DRAFT
    JO->>JO: stage PROPOSAL_IN_PROGRESS · ref proposalId
    App->>BFF: GET form schema
    BFF->>PS: form-schema
    PS->>HUB: PROPOSAL_FORM_SCHEMA → adapter GET /v1/proposals/schema
    HUB-->>PS: bank-envelope form
    App->>BFF: save answers (page by page)
    BFF->>PS: PUT …/answers (encrypted envelope)
    App->>BFF: POST /api/v1/journeys/{jid}/proposals/{pid}/submit
    BFF->>JO: submit
    JO->>PS: POST …/{pid}/submit
    PS->>CN: GET /consents/{consentId} (C2: GRANTED, unexpired, purposes)
    alt no valid consent
        PS-->>JO: 403 CONSENT_REQUIRED (stays DRAFT)
    end
    PS->>HUB: PROPOSAL_SUBMIT (key proposal:{pid}:submit, NO retry)
    HUB->>A: POST /v1/proposals {schemaId, offerId, values, consentRef, agentId, journeyId}
    A->>SB: POST /insurance/lifeterm/v1/proposal
    SB-->>A: {reqId}
    A-->>HUB: 201 {proposalJobId, PENDING}
    PS->>PS: SUBMITTED · outbox ProposalSubmitted
    JO->>JO: stage UNDERWRITING (on insurer acknowledgement)
    loop adapter poller
        A->>SB: GET …/proposal/poll/{reqId}
    end
    A->>A: job COMPLETED + applicationNumber
    PS->>HUB: poll → applicationNumber → UNDER_WRITING
    loop scheduled status sync (Scenario 14)
        PS->>HUB: APPLICATION_STATUS → adapter GET /v1/status/{applicationNumber}
        HUB-->>PS: bankStatus UW_REQUIREMENTS | UW_IN_PROGRESS | CUSTOMER_DECISION | PAYMENT | ISSUED | CLOSED
        PS->>PS: REQUIREMENTS_PENDING / UW_APPROVED / UW_COUNTER_OFFER / UW_DECLINED · outbox UnderwritingDecision
    end
    PS-->>JO: UnderwritingDecision (UW_APPROVED → next: payment)
```

## 2. Rules

| Rule | Where | Error |
|---|---|---|
| Proposal only on the selected offer of a selected quote | Proposal (`INV-PRP-02`) | `422 INVALID_OFFER_REFERENCE` |
| `issuanceMode` ∈ `STP`/`NON_STP`/`INSTA`; no mode bypasses gates | Proposal (`INV-PRP-06`) | `422 ISSUANCE_MODE_INVALID` |
| Consent re-checked **at submit** | Proposal (`INV-PRP-01`, C2) | `403 CONSENT_REQUIRED` |
| Quote expired while drafting | Proposal stays `DRAFT`; journey back to `QUOTING` (F-03) | `409 QUOTE_EXPIRED` |
| **Never resubmit blind** | Hub / adapter | Poll; exhaustion → `SUBMISSION_FAILED` + ops task (F-04) |
| Agent attribution mandatory on submit | Hub / adapter | `422 AGENT_ATTRIBUTION_MISSING` |
| PII form values only in encrypted stores | Proposal envelope, adapter raw payload (`INV-PRP-05`) | — |
| UW is the insurer's decision | Proposal only tracks | — |
| Withdraw not allowed at/after `AWAITING_PAYMENT` | `INV-PRP-04` | `409 WITHDRAWAL_NOT_PERMITTED` |

## 3. Where data goes

`proposal.proposal_working` (form answers, nominee, requirements — encrypted; control columns
`application_number`, `state`, `underwriting_state`, `schema_id`); adapter job + raw payloads in
`bank_persistence`; journey refs `proposalId`, `applicationNumber`; outbox `ProposalSubmitted`,
`UnderwritingDecision`. Structured `proposal`, `uw_requirement`, `uw_document_ref` appear only after
payment reconciles.

## 4. Pages

[Proposal](../services/proposal-service.md) · [1SB adapter §3.4–§3.6](../services/1sb-integration-service.md#34-get-v1proposalsschema) · [Provider sync](./14-provider-status-sync.md)
