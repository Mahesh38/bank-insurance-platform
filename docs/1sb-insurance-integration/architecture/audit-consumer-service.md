# Audit Consumer Service — Stub Design

**Role:** Future platform microservice that sinks audit events into durable storage  
**Status:** Design stub only (no Boot app in this loop — TD-021)  
**Depends on:** [bank-persistence-service.md](./bank-persistence-service.md)

---

## 1. Purpose

`audit-consumer-service` will consume audit events from an async source and persist them via **bank-persistence-service** over internal HTTP. It does **not** own a database or Flyway migrations.

---

## 2. Topology

```text
  (async source TBD)          bank-persistence-service (:8081)
         │                              ▲
         ▼                              │
  audit-consumer-service ──HTTP only───┘
         │
         └── POST/GET /internal/v1/audit-events
```

| Concern | Owner |
|---------|--------|
| `audit_event` table + Flyway | `bank-persistence-service` only |
| Append / list audit rows | HTTP `/internal/v1/audit-events` on that same service |
| Second audit DB | **Forbidden** |

---

## 3. Persistence API usage (binding)

| Operation | Call |
|-----------|------|
| Persist an audit record | `POST /internal/v1/audit-events` |
| List by resource | `GET /internal/v1/audit-events?resourceId={id}` |

Config (target): `bank.persistence.base-url` / `BANK_PERSISTENCE_BASE_URL` (same platform keys as other consumers).

---

## 4. Async source (TBD)

Inbound transport is **not decided** in Phase 1. Candidates for a later story:

- Message bus (Kafka / SQS / bank-standard queue)
- Outbox relay from producers
- Direct publish from `bank-common-audit` adapters

This stub does not prescribe the broker. The persistence side of the contract is fixed: HTTP to `bank-persistence-service` only.

---

## 5. Non-goals (this loop)

- Scaffolding a Spring Boot module under `services/`
- Choosing or wiring the async transport
- Duplicating `audit_event` schema or Flyway elsewhere
- Changing `/internal/v1/audit-events` paths

Full consumer implementation is Phase 2+ / a separate story.
