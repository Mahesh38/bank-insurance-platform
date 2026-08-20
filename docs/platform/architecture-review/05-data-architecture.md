# 05 — Data Architecture

## Governing rule: ownership per service, topology by evidence

**Amended 2026-08-20 by [`ADR-008`](./08-architecture-decision-log.md).** This section previously
read *"database-per-service"* and bundled two different claims under it. They are separated now:

| Claim | Standing |
|---|---|
| One owner per authoritative datum; no service reads or writes another's tables | **Invariant.** Enforced by ArchUnit and IAM |
| Separate credentials and schema ownership per service | **Invariant.** This is what makes the first claim enforceable rather than aspirational |
| A separate physical cluster per service | **Decision, not principle.** Evidence-led: scale, blast radius, security isolation, RTO/RPO, cost |

**R0 runs one Aurora PostgreSQL cluster with a schema per bounded context**, per-context
credentials and no cross-schema grants. Physical separation is taken later, per workload, and the
first split follows the **LOB-cell / shared-platform seam** rather than the service boundary:
shared platform data in one cluster, and each LOB cell (`life_*`, then `health_*`, then
`general_*`) able to become its own cluster with no application redesign. *"Database per service"
does not mean forty Aurora clusters across three environments plus DR* (`VIN-001 §34`) — that is a
cost, upgrade, backup and on-call problem, not an architecture. What follows below describes the
**ownership** model; read every "own its own database" as "own its own schema, with its own
credential", and see `ADR-008` for when a schema becomes a cluster.

Each microservice in [02](./02-target-microservices-architecture.md) owns its schema exclusively; no other service connects to it directly, ever. Cross-service reads happen via the owning service's API (sync) or a replicated read model built from its published events (async) — never via a shared connection string. This is the same boundary discipline the 1SB adapter already enforces internally (`JobStorePort`, ArchUnit rule "no 1SB types outside adapter"), applied here at the platform level to the persistence layer itself.

### Why this changes the existing `bank-persistence-service` pattern

The spike's `bank-persistence-service` — one deployable, one Flyway history, every consumer (integration service, future audit-consumer, "other MS later") calling it over `/internal/v1` — is a sound shortcut for *two* related services sharing a genuinely common concern (job/correlation tracking + audit). It stops being sound as a platform-wide pattern: if Customer, Lead, Consent, Suitability, Catalogue, Payment, and Policy all write through one shared persistence service, that service becomes a single point of coupling and failure for the entire platform, and every schema change requires coordinating across every consuming team — the opposite of what microservices are for.

**Recommendation:** keep the *shape* of `bank-persistence-service` — a dedicated internal HTTP-fronted store — but scope it narrowly to what it was actually designed for: the **integration job/correlation store** and, if genuinely useful as a shared write path, the **audit event ingestion API** consumed by `Audit & Compliance`. Every new business-domain service gets **its own** Aurora schema (or DynamoDB table set), owns its own Flyway/migration history, and is never a "consumer" of a platform-wide DB service. This is a direct, explicit amendment to the existing pattern — flagged as `ARCH-004` in the decision log — not a silent departure. Those schemas sit in **one cluster at R0**; `ADR-008` decides when and along which seam they stop doing so.

## Datastore choice per service

| Service | Datastore | Why this engine |
|---------|-----------|------------------|
| Identity & Access | DynamoDB (sessions) + Aurora PostgreSQL (roles/entitlements) | Sessions are high-volume key-value with TTL; roles are small, relational, rarely-changing |
| Customer | Aurora PostgreSQL | Relational profile data, needs joins/constraints, moderate volume |
| Opportunity | Aurora PostgreSQL | Relational, workflow-state-heavy, reporting joins needed |
| Consent | Aurora PostgreSQL, **append-only table, no UPDATE/DELETE grants** | Regulatory evidence — immutability enforced at the grant level, same principle as the existing `audit_event` table design |
| Suitability & Recommendation | Aurora PostgreSQL | Versioned assessment records, relational rule evaluation history |
| Product Catalogue | Aurora PostgreSQL (source of truth) + ElastiCache Redis (read-through cache) | Low write rate, very high read rate — cache-first is the correct latency lever, not a bigger DB |
| Journey Orchestration | DynamoDB | State-machine document per journey, extremely high read/write rate relative to size, single-key access pattern (`journeyId`) — textbook DynamoDB fit, not a relational workload |
| Quotation | DynamoDB (job/poll records) + ElastiCache Redis (idempotency keys) | Directly mirrors the existing `IntegrationJobEntity` + Redis idempotency pattern already proven in `1sb-integration-service-architecture.md` §9 — short-lived, high-churn, single-key lookups |
| Proposal & UW-Tracking | Aurora PostgreSQL | Long-lived case records with requirements/documents as related rows; needs relational integrity over weeks-long lifecycles |
| Payment | Aurora PostgreSQL | Financial records demand ACID transactions and reconciliation joins |
| Policy & Issuance | Aurora PostgreSQL + S3 (policy PDFs, referenced by key) | Relational lifecycle + document store separated — never store binaries in the RDBMS |
| Integration Hub | DynamoDB (routing config only) | Tiny, low-write config data (`RoutingPolicy` per LOB/product); no business data lives here at all |
| 1SB Adapter | Aurora PostgreSQL (job store, exactly as today) + S3 (raw payload archive) | Unchanged from the existing, already-reviewed design |
| Audit & Compliance | DynamoDB (hot append-only event store) + S3 (cold archive after TTL) | Write-once, high-volume, queried by `resourceId`/`journeyId` — same access pattern the existing `/internal/v1/audit-events?resourceId=` endpoint already assumes |
| Notification | DynamoDB | Delivery log, TTL-friendly, simple key access |
| Reporting & MIS | S3 data lake + Redshift Serverless / Athena | Analytical, not transactional — must never share infrastructure with OLTP services |
| Administration & Config | Aurora PostgreSQL | Small, relational, versioned configuration |

## Cross-service data consistency

- **Within a bounded context:** standard ACID transactions inside that service's own database.
- **Across bounded contexts (e.g., "policy issued" must update Journey, trigger Notification, and land in Reporting):** **eventual consistency via the Kafka event backbone** (see [03](./03-communication-patterns.md)), not distributed transactions/2PC. Each consuming service applies the event idempotently (dedup on `eventId`), matching the existing idempotency discipline already used for inbound bank API calls.
- **Saga pattern** for the multi-step "sale" flow (quote → proposal → payment → issuance) owned by Journey Orchestration: each step's success/failure is a Journey Orchestration state transition driven by events from the owning service, with compensating actions (e.g., payment retry, proposal re-submission) modeled as explicit journey states — not baked into any one domain service.

## PII, retention, and encryption

Directly inherits the compliance posture already specified for the 1SB adapter (`1sb-integration-service-architecture.md` §7.7/§8) and generalizes it platform-wide:

| Rule | Applies to |
|------|------------|
| PII never appears in logs (masked at the logging framework, not per-call) | Every service, via shared `bank-common-observability` masking convention |
| Raw third-party (1SB/insurer) payloads retained encrypted, **7 years**, in S3 with Object Lock | 1SB Adapter (already the design); any future Direct Insurer Adapter inherits the same rule |
| Consent, Suitability, Audit records: append-only, retention aligned to regulatory minimum (**pending exact IRDAI/RBI figure** — see [06](./06-security-compliance-and-nfrs.md)) | Consent, Suitability, Audit & Compliance |
| Encryption at rest via KMS customer-managed keys, per-service key where data sensitivity differs (Payment/Consent get dedicated CMKs, not the platform-shared default) | Aurora, DynamoDB, S3 |
| Encryption in transit: TLS 1.2+ everywhere, mTLS pod-to-pod via service mesh | All services |
| Idempotency keys, quote jobs, sessions: short TTL (24h or less), never treated as durable records | Redis, DynamoDB job stores |

## Canonical objects → owning service (traceability to the business information model)

Every object in `knowledge-base/07-information-model-and-rules.md` maps to exactly one owning service — this table exists so nobody has to guess where a future "add a field to Consent" story lands:

| Canonical object | Owning service |
|---|---|
| Customer | Customer |
| Relationship Manager | Identity & Access (identity) / RM Workspace BFF (view) |
| Lead | Lead |
| Consent | Consent |
| Suitability Assessment | Suitability & Recommendation |
| Product Catalogue, Partner Insurer | Product Catalogue |
| Quote | Quotation |
| Proposal, Underwriting Case | Proposal & UW-Tracking |
| Payment | Payment |
| Policy | Policy & Issuance |
| Communication | Notification |
| Audit Event | Audit & Compliance |
| Reporting Metric | Reporting & MIS |
