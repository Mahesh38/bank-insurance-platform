# 06 — Security, Compliance & Non-Functional Requirements

## Compliance posture: what's already decided vs. what's still open

The business SSOT is explicit that several compliance questions are **not yet answered**, and this review does not pretend otherwise (`docs/au-bank-insurance-platform/DECISION-LOG.md`, "Open validation items"):

| Open item | Architectural consequence taken here |
|-----------|----------------------------------------|
| Exact IRDAI consent model (D-011, pending) | Consent service built with **versioned, pluggable rule evaluation** (configurable via Administration & Config), not a hardcoded consent flow — matches D-014 ("configurable policy-driven controls until compliance validated") |
| RBI + IRDAI compliance mapping (pending) | Audit & Compliance designed to capture a **superset** of plausible evidence (agent, distributor, consent, suitability, timestamps) now, rather than retrofitting fields later — cheaper to not-use a captured field than to backfill history |
| Insurance advisor / agent identity model (D-008, pending) | Agent/distributor attribution kept as a **mapping table behind an interface** (`AgentMappingClient`, already the pattern in the 1SB adapter), so the identity model can change without touching Quotation/Proposal/Payment |
| PII retention period (pending) | Default assumption: align with the 1SB adapter's already-specified **7-year** retention for raw payloads/audit until compliance issues a different figure; retention is a configuration value, not hardcoded in each service |
| Data residency (pending) | **Assumption for this review: India (`ap-south-1`)** primary, DR in `ap-south-2` — both within India — pending explicit compliance confirmation; flagged in [04](./04-aws-infrastructure-architecture.md) |
| Audit log retention (pending) | Same treatment as PII retention above |

**This review's stance:** build every compliance-sensitive control (consent capture, retention, masking, attribution) as **configuration-driven**, never as logic baked into a domain service's code. When compliance finalizes the open items, they become a config/rule change, not a re-architecture.

## Security controls (defense in depth)

| Layer | Control |
|-------|---------|
| Edge | AWS WAF (OWASP managed rule sets) + AWS Shield in front of CloudFront/API Gateway |
| Authentication | Provider-neutral adapter with private Keycloak initially, federated to bank AD/SSO (OIDC/SAML/LDAP); token-hiding BFF; MFA for all production workforce users |
| Authorization | Default-deny RBAC + ABAC + relationship policies in Identity & Access; BFF and owning domain service both enforce sensitive decisions |
| Service-to-service | mTLS via service mesh (Istio/App Mesh) + IRSA (no static credentials in pods) |
| Secrets | AWS Secrets Manager, automatic rotation, zero secrets in source/config files (CI-enforced via secret scanning, e.g. `gitleaks` — already a stated control in the 1SB adapter, kept platform-wide) |
| Data at rest | AWS KMS envelope encryption, per-sensitivity-tier CMKs |
| Data in transit | TLS 1.2+ minimum everywhere; TLS 1.0/1.1 disabled at the JVM/runtime level |
| PII in logs | Masking at the logging framework (`PiiMaskingConverter` pattern), enforced platform-wide via `bank-common-observability`, not per-service opt-in |
| Network | Private subnets for all compute/data, NetworkPolicy per namespace, no direct internet egress except through the whitelisted NAT Gateway |
| Account-level audit | AWS CloudTrail, Config, GuardDuty, Security Hub — infrastructure evidence, separate from the application-level Audit & Compliance service |
| Attribution | `distributorId` from secrets/config only, never from a caller-supplied field (prevents tenant spoofing) — unchanged from the existing D7/8.5 rule, now enforced at the Integration Hub for every adapter, not just 1SB |

## Availability & latency targets

| Tier | Availability target | Rationale |
|------|----------------------|-----------|
| Customer-facing critical path (BFFs, Journey Orchestration, Identity, Payment, Policy) | **99.9%** monthly (≈43 min/month) | Matches the target already set for the 1SB adapter; extended as the platform-wide floor for anything on the sale path |
| Advisory/read-heavy (Catalogue, Suitability, Reporting) | 99.5%+ | Degraded-but-cached reads acceptable; not on the critical write path |
| Batch/analytics (Reporting & MIS) | Best-effort, business-hours SLA | Eventual consistency is explicitly acceptable here |

| Endpoint class | p50 target | p99 target |
|---|---|---|
| BFF reads (journey state, catalogue) | < 150 ms | < 400 ms |
| Quote creation (ack) | < 300 ms | < 800 ms |
| Quote poll | < 100 ms | < 300 ms |
| Proposal submit (ack) | < 500 ms | < 1.5 s |
| Payment session creation | < 1 s | < 3 s |
| Status/policy check | < 800 ms | < 2 s |

(These figures are carried forward unchanged from the already-approved 1SB adapter NFRs, since the requester's own SLA expectations were already validated there — no reason to loosen them platform-wide.)

## Scalability & elasticity

- Every customer-facing service is **stateless at the pod level** — state lives in DynamoDB/Aurora/Redis, never in-memory, so any pod can be killed/replaced/scaled without session loss.
- Elasticity mechanics (Karpenter, HPA, KEDA) detailed in [04](./04-aws-infrastructure-architecture.md); the NFR being satisfied here is: **the platform absorbs RM-hour and campaign-driven spikes automatically and scales back down**, so cost tracks actual usage rather than peak-provisioned capacity.
- Multi-insurer quote fan-out is designed for **partial success** (already established pattern) specifically so p99 latency isn't dictated by the single slowest insurer integration.

## Disaster recovery

Detailed in [04](./04-aws-infrastructure-architecture.md) — Aurora Global Database + MSK cross-region replication + S3 CRR, RTO ≤ 1 hour / RPO ≤ 5 min for the transactional core.

## Testing & quality gates (carried forward, now platform-wide)

The existing QA discipline (`service-ssot/QA-LEAD-TESTING-STRATEGY.md`, `TESTING-RULES.md`, JaCoCo coverage gates) is sound engineering practice and should be the **template every new service's team lead adopts**, not reinvented per service: shared-lib coverage floor (80%/70%), ArchUnit boundary enforcement per service, contract tests at every service-to-service and event boundary (Pact or equivalent) so the async event catalog in [03](./03-communication-patterns.md) doesn't silently drift between producer and consumer.
