# 03 — Communication Patterns: Sync vs. Async

## Governing rule

> If a human (RM or customer) is on-screen waiting for the result **within the same interaction**, the call is synchronous request/response. If the result is a *consequence* of something that already happened — a record to keep, a message to send, a dashboard to update — it is an asynchronous event. Long-running third-party work (insurer quote/proposal turnaround) is **synchronous at the bank API boundary, asynchronous underneath** — the exact "sync API, async inside" rule the 1SB spike already applies at the adapter level (`1sb-integration-service-architecture.md` §1, Domain rule 3), extended here to the whole platform.

## Transport choices

| Style | Mechanism on AWS | Used for |
|-------|-------------------|----------|
| Synchronous, edge-facing | Amazon API Gateway → ALB → EKS Ingress (REST/JSON, HTTP/2) | Customer BFF / RM BFF ↔ outside world |
| Synchronous, service-to-service | AWS App Mesh / Istio on EKS (mTLS, REST or gRPC) | Journey Orchestration ↔ domain services |
| Asynchronous, event backbone | Amazon MSK (Kafka) | Domain events: `JourneyStageChanged`, `QuoteCompleted`, `ProposalSubmitted`, `PaymentCompleted`, `PolicyIssued`, `ConsentCaptured` |
| Asynchronous, point-to-point task | Amazon SQS | Notification dispatch queue, reconciliation batch queue, document-generation queue |
| Asynchronous, fan-out | Amazon SNS (+ SQS subscribers) | One event (e.g., `PolicyIssued`) needing delivery to Notification, Reporting, and Audit independently |
| Event-driven scaling glue | AWS Lambda | S3 upload triggers (KYC doc → virus scan → downstream event), scheduled reconciliation kick-off |

## Sync vs. async matrix, by interaction

| Interaction | Style | Why |
|-------------|-------|-----|
| BFF → Identity & Access (login/token validate) | **Sync** | Blocks every subsequent call; must be low-latency |
| BFF → Journey Orchestration (get/advance journey state) | **Sync** | Screen literally renders this |
| Journey Orchestration → Customer (CIF prefill) | **Sync** | Needed before rendering the next form |
| Journey Orchestration → Consent (capture/check) | **Sync** | Suitability/quote must be gated on this in real time (D-005, A7 — "never bypass") |
| Journey Orchestration → Suitability & Recommendation | **Sync** | Recommendation must render before product selection |
| Journey Orchestration → Product Catalogue | **Sync (cached)** | Read-heavy, low-change data — cache-first, see below |
| Journey Orchestration → Quotation (create quote) | **Sync ack, async fulfilment** | Caller gets a `jobId` immediately (< 300 ms target, matches existing `POST /v1/quotes` SLA); actual insurer round-trip happens via polling behind the API |
| Caller → Quotation (poll job) | **Sync** | Short poll, cache-backed, sub-100 ms |
| Quotation/Proposal/Payment/Policy → Integration Hub → 1SB Adapter | **Sync at the boundary, async internally** | Same Case-2 pattern already proven in `1sb-integration-service-architecture.md` §6 — do not change a working pattern |
| Journey Orchestration → Proposal (submit) | **Sync ack (202), async completion** | Underwriting turnaround can take days; nothing should hold an HTTP connection open for that |
| Payment → AU Bank PG | **Sync (redirect/callback)**, webhook for confirmation | Payment session creation is sync; the actual payment completion arrives as an async callback/webhook, converted to a `PaymentCompleted` event |
| Any domain service → Audit & Compliance | **Async (Kafka event)** | Never let an audit write slow down or fail the primary transaction; audit is a side effect, and it must still be reliable — Kafka + at-least-once consumer with dedup gives durability without coupling |
| Any domain service → Notification | **Async (SNS → SQS → Notification service)** | SMS/email must never block the journey; retries/backoff live entirely on the async path |
| Any domain service → Reporting & MIS | **Async (Kafka, consumed by Reporting only)** | Analytics must never be able to slow a transactional path; eventual consistency is fine for MIS dashboards |
| Administration & Config → all services | **Sync pull with cache + async invalidation event** | Services read config from Redis/cache at request time; Administration publishes a `ConfigChanged` event to bust the cache — no service blocks on Administration being up |

## Where performance is the deciding factor

| Concern | Pattern |
|---------|---------|
| Multi-insurer quote fan-out (Group A) | Quotation service issues parallel calls into Integration Hub per eligible insurer; **partial success is first-class** (already established: a job reaches `PARTIAL`, not `FAILED`, when at least one offer returns) — this avoids the whole quote request being gated on the slowest insurer |
| Repeated reads of rarely-changing data (product catalogue, master lookups, proposal schemas) | ElastiCache Redis in front of Aurora/1SB, TTL-based, exactly the `ProposalSchemaCache` pattern already in the 1SB adapter — reused platform-wide, not reinvented per service |
| RM-hour and campaign traffic spikes | Stateless BFFs and orchestration reads scale horizontally via HPA/Karpenter (see [04](./04-aws-infrastructure-architecture.md)); nothing in the hot read path is allowed a synchronous call to a service that isn't independently scalable |
| Idempotent retries from flaky mobile/branch networks | `Idempotency-Key` header on every mutating endpoint, Redis-backed, 24h TTL — same contract the 1SB adapter already implements, promoted to a platform-wide shared library (`bank-common-idempotency`) so every new service gets it for free |
| Insurer/aggregator slowness or outage | Circuit breaker (Resilience4j) at the Integration Hub boundary; on open circuit, new quote/proposal creation fails fast with `503 + Retry-After` while in-flight jobs already polled continue serving from the job store — same graceful-degradation contract already specified for the 1SB adapter, generalized to any adapter behind the Hub |

## Event catalog (initial, extend as domains land)

| Event | Producer | Consumers |
|-------|----------|-----------|
| `ConsentCaptured` | Consent | Audit, Journey Orchestration |
| `SuitabilityCompleted` | Suitability & Recommendation | Audit, Journey Orchestration |
| `QuoteCompleted` | Quotation | Journey Orchestration, Audit |
| `ProposalSubmitted` / `ProposalStatusChanged` | Proposal & UW-Tracking | Journey Orchestration, Audit, Notification |
| `PaymentInitiated` / `PaymentCompleted` | Payment | Journey Orchestration, Audit, Notification, Reporting |
| `PolicyIssued` | Policy & Issuance | Journey Orchestration, Audit, Notification, Reporting |
| `JourneyStageChanged` | Journey Orchestration | Audit, Reporting |
| `ConfigChanged` | Administration & Config | all services (cache invalidation) |

All events carry `journeyId`, `lob`, `actorId`/`rmEmployeeId`, `timestamp`, and a `traceId` propagated from the originating request — mirrors the tracing convention already defined in `bank-common-observability` (`TraceHeaders`, `MdcPropagationFilter`) so a single trace spans sync HTTP hops and async event hops alike.
