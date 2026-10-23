# ADR-025 — One event envelope, one topic convention, one publisher library

**Status:** Accepted (owner decision 2026-10-21, `reviewer_type: HUMAN`). Topic provisioning is S09.
**Date:** 2026-10-21
**Deciders:** Mahesh (owner). Shivanshi (MSK provisioning, S09).
**Origin:** [`SUG-20261021-rul`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20261021-rul--configuration-driven-rule-engine-and-the-nine-open-decisions) — review decision D6.

## Context

The design names ~30 domain events and an outbox per service, but has no envelope, no payload
schemas, no topic list, no DLQ rule and no publisher; two outbox patterns exist (Postgres
`outbox_event`, DynamoDB stream items — `DB-DEC-0005`). No Kafka client exists in the code.

## Decision

1. **Envelope** (`event-envelope/v1`, CloudEvents-compatible names). Every event on every topic:

   | Field | Type | Rule |
   |---|---|---|
   | `eventId` | ULID | Unique; the consumer dedupe key |
   | `eventType` | string | `PascalCase` past tense, e.g. `PaymentReconciled` |
   | `schemaVersion` | int | Increments on a breaking change; additive changes keep it |
   | `source` | string | Producing service id, e.g. `payment` |
   | `aggregateType` / `aggregateId` | string | The entity the event is about; the partition key |
   | `occurredAt` | RFC 3339 UTC | When the business fact happened |
   | `journeyId`, `leadId` | ULID, nullable | Correlation across contexts |
   | `actor` | `{id, type, actingCapacity}` | `INV-ACT-04` |
   | `traceId`, `correlationId` | string | Observability |
   | `configVersionRef` | string, nullable | Rule-set / config version that decided it (`ADR-024`) |
   | `data` | object | Event payload: **ids and states only — never customer PII** |

   JSON-Schema: `docs/platform/contracts/events/schemas/event-envelope.v1.json`; one schema per
   event in the same folder; the catalogue is [`docs/platform/contracts/events/CATALOGUE.md`](../contracts/events/CATALOGUE.md).
2. **Topics:** `{env}.{context}.{aggregate}.v{n}` (e.g. `prod.payment.payment.v1`), one topic per
   aggregate, key = `aggregateId` (ordering per aggregate), 6 partitions at R0, retention 7 days,
   `min.insync.replicas=2`. DLQ per consumer group: `{env}.dlq.{consumerGroup}`; retry topic
   `{env}.retry.{consumerGroup}` with backoff 1 s, 10 s, 60 s, then DLQ; a DLQ message is an ops
   task.
3. **Producers:** transactional outbox only — never a direct send in a request.
   - Postgres services: `outbox_event` (ULID `outbox_id`, `published_at`, `attempts`, `last_error`,
     `parked_at`) relayed by `libs/bank-common-events` `JdbcOutboxRelay`: `SELECT … FOR UPDATE SKIP
     LOCKED` in batches of 100, publish, mark published; after 10 failures the row is **parked**
     (alert), never silently dropped; multiple replicas are safe.
   - DynamoDB services (quotation, proposal): the OUTBOX item carries `published=false`; a stream
     consumer publishes and sets `published=true`; a sweeper republishes unpublished items older
     than 5 minutes, so a stream outage longer than the 24 h stream retention loses nothing.
4. **Consumers:** idempotent by `eventId` (`processed_event` table or the owning store's
   idempotency record), at-least-once, bounded retry, then DLQ. `libs/bank-common-events` provides
   `IdempotentEventHandler`.
5. **Transport port:** the library exposes `EventPublisher`; the Kafka (MSK, IAM auth) adapter is
   one implementation, an in-memory one is for tests. Services never import Kafka classes directly.
6. **Ids:** ULID everywhere for `eventId` and `outbox_id` (replaces `uuid` in `OUTBOX-PHYSICAL-DESIGN.md`).

## Alternatives considered

| Option | Why not |
|---|---|
| Debezium CDC on outbox tables | Another runtime to operate at R0; keep as a later swap behind `EventPublisher` |
| EventBridge | MSK is the ratified backbone (03 §5.1) |
| Per-service ad-hoc publishers | The current state; inconsistent and unbuilt |

## Consequences

Every async seam (S-17, S-18, S-23, S-24) becomes buildable; consent, policy and catalogue need
outbox tables (DDL updated); `PaymentReconciled` gains `quoteId`, `proposalId`, `journeyId`
(`D-025`).
