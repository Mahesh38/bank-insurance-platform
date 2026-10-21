# Outbox → MSK → consumers (seams S-17, S-18, S-19, S-23, S-24)

[← API reference home](../README.md)

| | |
|---|---|
| **Pattern** | **Transactional outbox in front of a broker** (`ADR-012`). The outbox row is the source of truth and the replay log; Amazon MSK is transport and fan-out |
| **Producers** | Every service that owns a schema and changes state: Lead, Suitability, Consent, Journey, Quotation, Proposal, Payment, Policy, Identity (provisioning) |
| **Consumers** | Audit (#16, mandatory), Notification (#17), Journey (stage moves), Quotation / Proposal (materialise on `PaymentReconciled`), Lead (`JourneySold`), Reporting (#18, S13+) |
| **Guarantee** | At-least-once; consumers **must** dedupe on `eventId` and tolerate replay |
| **Code status** | Identity provisioning outbox **IMPLEMENTED** (poller, no broker). Domain outboxes + MSK **CONTRACTED** |

## 1. Why both an outbox and a broker

"Commit, then publish" is two writes with no shared transaction: a crash between them loses the
event; a retry duplicates the business change. Writing the event **row in the same transaction**
as the business change removes that. The broker then gives fan-out so every new consumer is not
another poller against the producer's database.

**Rule that cannot be traded:** no regulatory evidence exists only in a topic. Audit consumes the
topic and writes `bank_persistence.audit_event` + S3 WORM — **that write** is what lets a journey
reach `SOLD` (`INV-JRN-05`, `FF-26`).

## 2. Flow

```text
service txn:  BEGIN
                INSERT/UPDATE business rows
                INSERT idempotency_record
                INSERT outbox_event (event_id ULID, event_type, aggregate id, payload JSONB, created_at)
              COMMIT
outbox-publisher (S-23): SELECT unpublished ORDER BY created_at
                         → publish to MSK (idempotent producer, SASL/IAM per topic)
                         → UPDATE published_at   (failure: attempts+1, last_error; retry forever with backoff;
                                                  alert on outbox age NFR-DAT-05 — nothing is dropped)
consumer (S-24):         receive → dedupe on eventId → do the work → commit offset
                         bounded failures → DLQ per consumer group = operations task
```

## 3. Outbox table (per schema)

| Column | Meaning |
|---|---|
| `event_id` / `outbox_id` | ULID — the dedupe key everywhere |
| `aggregate_type`, `aggregate_id` | e.g. `PAYMENT`, `01JY…` |
| `event_type` | e.g. `PaymentReconciled` |
| `payload` | JSONB — **ids, state, attribution only; never PII**; money only as amount + currency where reconciliation needs it |
| `created_at`, `published_at`, `attempts`, `last_error` | Publisher bookkeeping |

## 4. Event catalogue

| Event | Producer | Key payload | Consumers |
|---|---|---|---|
| `OpportunityCreated` | Lead | leadId, customerId, rmId, lob, accountableSpId | Journey, Audit, Reporting |
| `LeadQualified` | Lead | leadId, customerId, rmId, lob | Journey, Audit, Reporting |
| `ConsentGranted` / `ConsentWithdrawn` | Consent | consentId, customerId, statementVersion, otpTxnId / actor, reason | Journey, Audit (+ Notification on withdraw) |
| `SuitabilityCompleted` / `SuitabilityOverridden` | Suitability | suitabilityId, customerId, lob, outcome, validUntil | Journey, Audit (+ Reporting) |
| `QuoteRequested` / `QuoteCompleted` | Quotation | quoteId, lob, offerCount, partialErrorCount | Journey, Audit, Reporting |
| `QuoteRejectedNoSuitability` | Quotation | quoteId, customerId, reason | **Audit (mandatory)**, Reporting |
| `OfferSelected` | Quotation | quoteId, offerId, insurerCode, premium | Journey, Audit |
| `ProposalSubmitted` | Proposal | proposalId, applicationNumber, insurerCode | Journey, Audit |
| `UnderwritingDecision` | Proposal | proposalId, decision, requirementCount | Journey, Audit, Notification |
| `PaymentLinkIssued` | Payment | paymentId, channel, expiresAt | Journey, Audit, **Notification** |
| `PaymentAuthorised` / `PaymentCaptured` | Payment | paymentId, pgTxnId, amount, currency | Journey, Audit |
| `PaymentReconciled` | Payment | paymentId, settlementRef | Journey, **Quotation, Proposal (materialise)**, Policy, Audit, Reporting |
| `PaymentReconciliationBreak` | Payment | paymentId, expected, observed | **Audit, Operations alert** |
| `PolicyIssued` | Policy | policyId, policyNumber, insurerCode | Journey, Audit, Notification |
| `PolicyIssuanceFailed` | Policy | proposalId, paymentId, reason | **Journey (→ COMPENSATING)**, Audit, Operations |
| `JourneySold` | Journey | journeyId, policyId, paymentId, leadId | **Lead (convert/archive)**, Audit, Reporting |
| `JourneyCompensating` | Journey | journeyId, failurePoint | Operations alert, Audit |
| `IDENTITY_PROVISIONING_REQUESTED` | Identity (implemented) | businessUserId | Identity's own dispatcher → IdP adapter |

Versioning: additive within a major version; removing or retyping a field needs a new major
version published in parallel until every consumer migrates; consumers ignore unknown fields.

## 5. Sources

- [R0 HLD Boundary 9](../../../architecture/R0-HLD.md#boundary-9--event-and-messaging) · [R0 solution architecture §5.1](../../ws3-platform/03-solution-architecture-r0.md#51-the-event-backbone-and-why-the-outbox-stays-in-front-of-it--s07-e02-s03) · [Domain model §7](../../ws3-platform/01-domain-model-and-invariants.md)
- `services/identity-authorization-service/src/main/java/com/bank/identity/authz/application/ProvisioningOutboxDispatcher.java`
