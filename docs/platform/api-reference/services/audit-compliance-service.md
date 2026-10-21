# audit-compliance-service — Audit and compliance (#16)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | Turns every regulated state change into **immutable evidence**: consumes domain events from MSK (fed by each service's outbox), validates attribution (`INV-ACT-04`), dedupes on `eventId`, and writes the evidence record. The journey **cannot reach `SOLD` until this write lands** (`INV-JRN-05`) — not when the topic accepts the message. Logs and OpenSearch are never evidence |
| **Module** | `services/audit-compliance-service` · port 8101 · `service-id: audit` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1/audit` |
| **Callers** | MSK consumer group (S-24) · compliance query UI (target) · Journey (SOLD precondition check) |
| **Calls out** | `bank-persistence-service` `POST /internal/v1/audit-events` (the record) · S3 Object Lock archive (7-year WORM) |
| **Owns data** | **No database of its own** — no second audit DB, no Dynamo (`DR-OWN-04`). Evidence lives in `bank_persistence.audit_event` (INSERT-only) + S3 WORM |
| **Code status** | **SKELETON**. Ingest exists today as `bank-persistence-service` `POST /internal/v1/audit-events` (IMPLEMENTED); the 1SB adapter already publishes audit events there |

## 1. API index

| Method | Path | Status | Purpose |
|---|---|---|---|
| (consumer) | MSK topics → ingest | PROPOSED | At-least-once consume; dedupe on `eventId`; DLQ after bounded attempts |
| `GET` | `/internal/v1/audit/events?resourceType&resourceId` | PROPOSED | Reconstruct a resource's history (delegates to persistence) |
| `GET` | `/internal/v1/audit/journeys/{journeyId}/completeness` | PROPOSED | The four `SOLD` audit events persisted? (for `INV-JRN-05`) |
| `POST` | `/internal/v1/audit-events` (on persistence) | IMPLEMENTED | The write itself — see [bank-persistence](./bank-persistence-service.md#5-audit-events) |

## 2. Ingest algorithm

```text
consume(event):
  1  validate schema: actor, actorType, actingCapacity (SP_ACCOUNTABLE | ASSIST_ONLY),
     resource, outcome, journeyId, distributorId, agentId, traceId; IPR: actorInsurerId, assistedActorId
     invalid → reject to DLQ; the producer's outbox retries (INV-ACT-04)
  2  POST persistence /internal/v1/audit-events {eventId = event.eventId, …}
     duplicate eventId → treat as success (idempotent)
  3  archive to S3 Object Lock (retain_until ≥ event time + 7 years, INV-AUD-01)
  4  only now commit the consumer offset (the evidence write is the completion condition)
  bounded failures → DLQ = operations task; SOLD stays blocked
```

## 3. Sources

- [Outbox and events](../interactions/outbox-and-events.md) · [R0 HLD Boundary 9](../../../architecture/R0-HLD.md#boundary-9--event-and-messaging) · [Audit consumer service](../../../1sb-insurance-integration/architecture/audit-consumer-service.md) · [Store segregation §3.1](../../data-architecture/04-store-segregation-and-roadmap.md)
