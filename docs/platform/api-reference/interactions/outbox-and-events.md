# Outbox → MSK → consumers (seams S-17, S-18, S-19, S-23, S-24)

[← API reference home](../README.md)

> **Authoritative event list:** [`contracts/events/CATALOGUE.md`](../../contracts/events/CATALOGUE.md). It holds
> the envelope, every event and its JSON-Schema, the topics, retry and DLQ naming, versioning, and both outbox
> patterns. This page explains the pattern; where the two differ, the catalogue wins.
> Decision: [`ADR-025`](../../architecture-review/ADR-025-event-specification-and-publisher.md) (accepted 2026-10-21).

| | |
|---|---|
| **Pattern** | **Transactional outbox in front of a broker** (`ADR-012`, `ADR-025`). The outbox row is the source of truth and the replay log. Amazon MSK is transport and fan-out |
| **Envelope** | `event-envelope/v1`, one per message: [schema](../../contracts/events/schemas/event-envelope.v1.json) |
| **Topics** | `{env}.{context}.{aggregate}.v{n}`, keyed by `aggregateId`; 6 partitions, retention 7 days, `min.insync.replicas=2`. Retry topic `{env}.retry.{consumerGroup}` (backoff 1 s / 10 s / 60 s), DLQ `{env}.dlq.{consumerGroup}` |
| **Producers** | Every service that owns a schema and changes state: Lead, Suitability, Consent, Journey, Quotation, Proposal, Payment, Policy. Identity's provisioning outbox is internal and is not published |
| **Consumers** | Audit (#16, mandatory), Notification (#17), Journey (stage moves), Quotation / Proposal (materialise on `PaymentReconciled`), Policy (issuance tracking), Lead (`JourneySold`), Reporting (#18, S13+) |
| **Guarantee** | At-least-once. Consumers **must** dedupe on `eventId` (`IdempotentEventHandler` + `processed_event`) and tolerate replay |
| **Code status** | `libs/bank-common-events` **IMPLEMENTED**: envelope, `OutboxWriter`, `JdbcOutboxRelay`, `EventPublisher`, Kafka adapter, `IdempotentEventHandler`. Lead, quotation and proposal write outbox rows today but with attribute-map payloads; moving them to the envelope is pending. No service runs a relay yet, and MSK topics are provisioned in S09 |

## 1. Why both an outbox and a broker

"Commit, then publish" is two writes with no shared transaction. A crash between them loses the event, and a retry
duplicates the business change. Writing the event **row in the same transaction** as the business change removes that
problem. The broker then provides fan-out, so each new consumer does not become another poller against the producer's
database.

**Rule that cannot be traded:** no regulatory evidence exists only in a topic. Audit consumes the topic and writes
its own evidence store, and **that write** is what lets a journey reach `SOLD` (`INV-JRN-05`, `FF-26`). The
evidence-store design is [`ADR-026`](../../architecture-review/ADR-026-audit-store-and-transport.md), which is
*proposed*: an `AuditRecorded` event into an append-only, hash-chained `audit.audit_event`.

## 2. Flow

```text
service txn:  BEGIN
                INSERT/UPDATE business rows
                INSERT idempotency_record
                INSERT outbox_event (outbox_id = eventId ULID, aggregate, event_type,
                                     payload = whole event-envelope/v1 JSON, created_at)
              COMMIT
JdbcOutboxRelay (S-23), every poll, n replicas:
              claim head row per aggregate  FOR UPDATE SKIP LOCKED  (batch 100)
              → publish to MSK (acks=all, idempotent producer, IAM auth), key = aggregateId
              → success: published_at
              → failure: attempts+1, last_error; later events of that aggregate wait
              → attempts = 10: parked_at + alert (never dropped; ops re-drives)
consumer (S-24): receive → claim processed_event(eventId, group) in the work's txn → work → COMMIT → offset
                 failure: retry topic 1 s / 10 s / 60 s → DLQ per consumer group = operations task
```

The DynamoDB services (quotation, proposal) use the second pattern: an OUTBOX item with `published=false`, a stream
publisher, and a 5-minute sweeper. See [catalogue §6.2](../../contracts/events/CATALOGUE.md#62-dynamodb-outbox-quotation-proposal--db-dec-0005).

## 3. Outbox table (per schema)

| Column | Meaning |
|---|---|
| `outbox_id` | CHAR(26) ULID, equal to the envelope `eventId`, which is the dedupe key everywhere (ADR-025 §6 replaces `uuid`) |
| `aggregate_type`, `aggregate_id` | e.g. `PAYMENT`, `01JY…` |
| `event_type` | e.g. `PaymentReconciled` |
| `payload` | The whole envelope, JSONB or text: **ids, state and attribution only, never PII**. Money only as amount + currency where reconciliation needs it |
| `created_at`, `published_at`, `attempts`, `last_error`, `parked_at` | Relay bookkeeping. `parked_at` is set after 10 failed attempts |

Full DDL and indexes: [catalogue §6.1](../../contracts/events/CATALOGUE.md#61-postgres-outbox-every-aurora-owning-service).

## 4. Event catalogue

This section is now a **summary**. The complete list (≈36 events, each with producer, topic, key, consumers,
schema, status and notes) is in [`CATALOGUE.md` §2](../../contracts/events/CATALOGUE.md#2-events).

| Event | Producer | Key payload (see schema) | Consumers |
|---|---|---|---|
| `LeadCreated` / `LeadExceptionEvaluated` / `LeadAssigned` / `LeadQualified` / `LeadClosed` | Lead | leadId, state, productClass, exceptionOutcome | Journey, Audit, Reporting |
| `ConsentGranted` / `ConsentWithdrawn` | Consent | consentId, customerId, **purpose** (incl. new `CNS-QSH`), checkpoint, validUntil | Journey, Audit (+ Notification on withdraw) |
| `SuitabilityCompleted` / `SuitabilityOverridden` | Suitability | evaluationId, customerId, **outcomes per product class** (D4), ruleSetVersion | Journey, Quotation gate, Audit, Reporting |
| `QuoteRequested` / `QuoteCompleted` / `QuoteFailed` / `OfferSelected` / `QuoteConverted` | Quotation | quoteId, state (+ journeyId, offerId) | Journey, Audit, Reporting |
| `QuoteRejectedNoSuitability` | Quotation | quoteId, customerId, productClass, reasonCode | **Audit (mandatory)**, Reporting |
| `ProposalCreated` / `ProposalSubmitted` / `ProposalAcknowledged` / `UnderwritingDecision` / `ProposalWithdrawn` / `ProposalMaterialised` | Proposal | proposalId, state, journeyId (+ applicationNumber) | Journey, Policy, Audit, Notification |
| `PaymentLinkIssued` / `PaymentAuthorised` / `PaymentCaptured` / `PaymentDeclined` / `PaymentExpired` | Payment | paymentId (+ pgTxnId, amount, currency) | Journey, Quotation / Proposal (end in-flight), Audit, Notification |
| `PaymentReconciled` | Payment | **paymentId, quoteId, proposalId, journeyId, applicationNumber**, settlementRef, method, paidAt, reconciledAt, amount, currency (D8) | Journey, **Quotation, Proposal (materialise)**, Policy (start TAT clock), Audit, Reporting |
| `PaymentReconciliationBreak` | Payment | paymentId, expectedAmount, observedAmount, breakCode | **Audit, Operations alert** |
| `PolicyStatusChanged` | Policy | policyId, proposalId, applicationNumber, status (`APPLICATION_RECEIVED` … `REFUNDED`), statusSource (D8) | Journey, Notification, Audit, Reporting |
| `PolicyIssued` / `ProposalDeclined` | Policy | policyId, policyNumber / reasonCode, proposalId, journeyId | Journey, Notification, Payment (refund on decline), Audit |
| `PolicyIssuanceFailed` | Policy | proposalId, paymentId, reasonCode | **Journey (→ COMPENSATING)**, Audit, Operations |
| `IssuanceTatEscalated` | Policy | proposalId, level (`WARNING` / `BREACHED`), decisionDays (D8, `PROPOSAL.TAT`) | Operations alert, Notification, Audit |
| `JourneySold` | Journey | journeyId, policyId, paymentId, leadId | **Lead (convert/archive)**, Audit, Reporting |
| `JourneyCompensating` | Journey | journeyId, failurePoint | Operations alert, Audit |
| `AuditRecorded` *(proposed, ADR-026)* | every service | action, resourceType, resourceId, outcome, prior/new state | audit-compliance-service |

Identity's `IDENTITY_PROVISIONING_REQUESTED` stays an internal outbox drained by identity's own dispatcher. It is
not a bus event.

**Versioning** ([catalogue §4](../../contracts/events/CATALOGUE.md#4-versioning)):

- Additive changes keep the `schemaVersion`.
- A breaking change gets a new `schemaVersion`, published in parallel on the same topic until every consumer has
  migrated.
- Consumers ignore unknown fields.
- Producers never send undeclared fields, because the data schemas are closed.

## 5. Sources

- [ADR-025](../../architecture-review/ADR-025-event-specification-and-publisher.md) · [Decisions 2026-10-21 §D5, §D8](../../architecture-review/DECISIONS-2026-10-21.md) · [ADR-026 (proposed)](../../architecture-review/ADR-026-audit-store-and-transport.md)
- [R0 HLD Boundary 9](../../../architecture/R0-HLD.md#boundary-9--event-and-messaging) · [R0 solution architecture §5.1](../../ws3-platform/03-solution-architecture-r0.md#51-the-event-backbone-and-why-the-outbox-stays-in-front-of-it--s07-e02-s03) · [Domain model §7](../../ws3-platform/01-domain-model-and-invariants.md)
- `libs/bank-common-events` · `services/identity-authorization-service/src/main/java/com/bank/identity/authz/application/ProvisioningOutboxDispatcher.java`
