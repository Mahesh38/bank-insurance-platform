# 01 — Phase Plan & Scope

**Up:** [UAT environment plan](./README.md)

Three phases, ~24 weeks, tracking the delivery roadmap in
[architecture-review/07](../architecture-review/07-delivery-roadmap-and-estimate.md). Infrastructure
arrives one phase *ahead* of the services that need it — never in the same sprint, because a squad
blocked on a missing database costs more than a month of that database running idle.

---

## The sequencing principle

Two rules decide what goes in which phase:

> **Rule 1 — Provision on demonstrated need, not on the architecture diagram.** The target
> architecture names MSK, Redshift, Istio, and per-service Aurora clusters. None of them earn
> their place in UAT until a workload actually exercises them. A component with no consumer is
> pure cost with no test value.

> **Rule 2 — Defer always-on services hardest.** The shutdown schedule reclaims ~59% of anything
> that can be stopped. It reclaims **nothing** from MSK, ElastiCache, Managed Grafana, or
> GuardDuty. So the ordering question for every managed service is not "do we need it?" but
> "can it be switched off, and if not, how late can it arrive?"

Rule 2 is why this plan looks different from a naive "build the target architecture in UAT" plan.
MSK in Phase 1 would cost $147/month × 6 months = $880 to host events that no service publishes
until week 15.

---

## Phase 1 — Foundation & walking skeleton

**Weeks 1–4** · Target: **~$327/month** optimised

### What this phase exists to do

Make [GATE-P4 criterion 4.3](../../governance/state/CURRENT-STATE.yaml) achievable — *"at least
one bank caller exercises quote + proposal against UAT"* — and stand up the landing zone that
everything after it depends on. This is the phase that is gate-blocking today.

### Workloads hosted by end of phase

| Workload | Status | Replicas |
|---|---|---|
| `1sb-integration-service` | **Built** | 2 |
| `bank-persistence-service` | **Built** | 2 |
| `workforce-access-bff` | **Built** | 2 |
| `identity-provider-adapter-service` | **Built** | 1 |
| `identity-authorization-service` | **Built** | 2 |
| Keycloak (self-hosted, per the auth SSOT) | Config | 1 |
| `customer` · `product-catalogue` · `journey-orchestration` · `integration-hub` | P0 build | 1 each |

~13 application pods. Five of these services already exist in this repository and can deploy in
week 1 — the environment is genuinely the constraint, not the code.

### Infrastructure delivered

AWS Organizations + dedicated `uat` account · VPC across 3 AZs · single NAT Gateway with fixed
EIP · EKS cluster with one managed node group (3 × `m7g.large`) · Aurora PostgreSQL Serverless v2
(one cluster, 0–2 ACU, logical database per service) · ElastiCache `cache.t4g.micro` · ECR ·
Secrets Manager · KMS · S3 · ALB · Route 53 · CloudWatch Logs at 7-day retention · Argo CD ·
External Secrets Operator · Karpenter · S3 Gateway VPC Endpoint.

Full configuration: [02-component-and-sizing-matrix.md](./02-component-and-sizing-matrix.md).

### Exit criteria

- [ ] A bank caller completes quote + proposal against UAT through `1sb-integration-service` — **this is GATE-P4 4.3**
- [ ] Workforce login works end to end: Flutter → BFF → Keycloak, with no OAuth token reaching the client (GATE-IAM-P1 A.1)
- [ ] 1SB has whitelisted the UAT NAT egress EIP and UAT distributor credentials are in Secrets Manager
- [ ] Argo CD deploys every workload from git; no `kubectl apply` by hand
- [ ] The shutdown schedule runs for one full week without a failed morning start
- [ ] One month of Cost Explorer data exists to re-baseline [03](./03-cost-estimate.md) against

### Explicitly not in Phase 1

MSK/Kafka · DynamoDB · service mesh · API Gateway · WAF · CloudFront · Managed Prometheus/Grafana
· multi-cluster Aurora · Multi-AZ anything · load testing · GuardDuty/Security Hub. Every one of
these has no consumer yet.

---

## Phase 2 — Core sale path

**Weeks 5–14** · Target: **~$583/month** optimised

### What this phase exists to do

Host the complete Term Life journey — need analysis → consent → quote → proposal → payment →
issuance — for RM-assisted mode, then self-service. This is where the platform stops being a
walking skeleton and starts being testable by the business.

### Workloads added

`suitability-recommendation` · `consent` · `quotation` · `proposal-uw-tracking` · `payment` ·
`policy-issuance` — plus both BFFs promoted to 2 replicas.

~23 application pods across 14 services.

### Infrastructure added

- **Compute:** node group grows to 3 × `m7g.xlarge`; **Karpenter Spot NodePool** added for CI
  runners, batch, and burst
- **Data:** Aurora splits into **two clusters** — `core` (0–4 ACU) and `regulated` (0–2 ACU, its
  own KMS CMK) so the per-sensitivity-tier encryption and append-only grant rules from
  [architecture-review/05](../architecture-review/05-data-architecture.md) are actually testable
  rather than merely documented
- **DynamoDB** on-demand: journey state, quote jobs, sessions, hub routing. On-demand billing
  means these cost nothing while the environment is switched off
- **ElastiCache** upgraded to `cache.t4g.small` — now serving idempotency keys and sessions, not
  just catalogue cache
- **Edge:** API Gateway (HTTP API) in front of the BFFs + AWS WAF with OWASP managed rules
- **Async:** SNS + SQS — pay-per-request, and therefore free overnight. **Not MSK.**
- **Observability:** `kube-prometheus-stack` in-cluster (Prometheus + Grafana + Loki). Runs on
  the worker nodes, so it stops with them. AWS-managed observability waits for Phase 3.

### Why SNS/SQS and not MSK here

The async interactions that exist in Phase 2 are point-to-point and fan-out — notification
dispatch, document generation, reconciliation kick-off. SNS+SQS serves all of them at
effectively zero cost in a scheduled-off environment. The *event backbone* pattern from
[architecture-review/03](../architecture-review/03-communication-patterns.md) only earns MSK when
there are durable, replayable, multi-consumer domain event streams — which means when Audit,
Reporting, and Notification all consume the same event. That is Phase 3. This also matches
`CURRENT-STATE.yaml`, which parks the event backbone until the integration-architecture stage.

### Exit criteria

- [ ] Full Term journey completes end to end in UAT, RM-assisted mode
- [ ] Consent and Payment data verifiably encrypted under their own CMK, with append-only grants proven by a failed `UPDATE` test
- [ ] Idempotency replay test passes against ElastiCache
- [ ] Spot interruption during a UAT test run does not lose a journey — proves pod-level statelessness (NFR from [architecture-review/06](../architecture-review/06-security-compliance-and-nfrs.md))
- [ ] Per-namespace cost showback live (OpenCost), so squads can see their own spend

---

## Phase 3 — Compliance, scale & launch readiness

**Weeks 15–24** · Target: **~$1,400/month** optimised

### What this phase exists to do

Prove the things that go/no-go depends on: audit evidence, NFR targets under load, DR restore,
and a security review. This phase costs more than the other two combined because it is the first
time UAT has to look like production rather than merely behave like it.

### Workloads added

`audit-compliance` · `notification` · `administration-config` · `lead` · `reporting-mis` —
reaching all 19 services from the target architecture.

~34 application pods.

### Infrastructure added

- **Compute:** 4 × `m7g.xlarge` on-demand baseline + Karpenter Spot pool sized for load-test
  generators
- **Amazon MSK:** 3 × `kafka.t3.small`, 100 GB per broker. The event backbone, arriving with its
  consumers
- **Service mesh: Istio in ambient mode**, not sidecar mode. 34 sidecars at 100m CPU / 128Mi
  would cost roughly a whole `m7g.xlarge` in pure overhead; ambient's per-node ztunnel plus
  per-namespace waypoints deliver the same mTLS and L7 policy for a fraction of it
- **Aurora:** third cluster (`integration`), plus a **reader instance on `regulated`** so
  Multi-AZ failover is genuinely tested rather than assumed. Max 8 ACU on `core` to absorb load tests
- **ElastiCache:** `cache.t4g.medium` × 2 with Multi-AZ, to test failover behaviour
- **Observability:** Amazon Managed Prometheus + Amazon Managed Grafana, for production parity
- **Edge:** CloudFront + WAF at the edge
- **Analytics:** Athena + Glue over the S3 data lake. **Not Redshift Serverless** — its 8-RPU
  floor is ~$2,100/month, which would more than double this environment's entire bill to serve
  MIS queries that Athena answers pay-per-query
- **Security:** GuardDuty, Security Hub, AWS Config, org-wide CloudTrail
- **DR:** AWS Backup with cross-region copy to `ap-south-2` — proves the restore runbook. **Not**
  a warm standby

### Exit criteria

- [ ] Load test meets the p50/p99 targets in [architecture-review/06](../architecture-review/06-security-compliance-and-nfrs.md) — quote ack < 300ms p50 / < 800ms p99, quote poll < 100ms p50
- [ ] Audit events for a full journey are reconstructable from the Audit service — the compliance evidence requirement
- [ ] DR drill: restore `regulated` cluster from cross-region backup, RTO measured and recorded
- [ ] Security review / VAPT completed against the UAT endpoint
- [ ] Karpenter consolidation and PodDisruptionBudgets survive a forced node rotation with no journey loss

---

## Phase boundaries as a table

| | Phase 1 | Phase 2 | Phase 3 |
|---|---|---|---|
| **Weeks** | 1–4 | 5–14 | 15–24 |
| **Services hosted** | 8–10 | 14 | 19 |
| **App pods** | ~13 | ~23 | ~34 |
| **EKS nodes (on-demand)** | 3 × `m7g.large` | 3 × `m7g.xlarge` | 4 × `m7g.xlarge` |
| **Spot** | — | CI + batch | + load generators |
| **Aurora clusters** | 1 | 2 | 3 + 1 reader |
| **Max ACU** | 2 | 4 | 8 |
| **Redis** | `t4g.micro` | `t4g.small` | `t4g.medium` × 2 Multi-AZ |
| **DynamoDB** | — | 4 tables | 7 tables + PITR |
| **Event backbone** | — | SNS/SQS | **+ MSK** |
| **Service mesh** | — | — | Istio ambient |
| **Observability** | CloudWatch | + in-cluster Prometheus | + AMP / Managed Grafana |
| **Edge** | ALB | + API Gateway + WAF | + CloudFront |
| **Monthly (optimised)** | **$327** | **$583** | **$1,400** |

---

## What would make this plan wrong

Stated up front so it can be challenged now rather than discovered in week 12:

| Assumption | If it breaks |
|---|---|
| UAT concurrency stays low — a handful of testers, not sustained load | Phase 2/3 node counts rise. The 250m CPU request is the number to revisit first |
| Squads deploy to UAT continuously but do not need per-squad isolated environments | Per-squad namespaces are free; per-squad *clusters* would multiply the $73/month control plane and the whole data tier |
| The Phase 3 load test is a bounded exercise, not continuous | Sustained load testing breaks the shutdown schedule's economics and needs its own budget line |
| 1SB UAT is available on the sandbox contract already in place | If 1SB requires a separate paid UAT tenancy, that is a new cost line outside AWS entirely |
| Bank InfoSec accepts a single NAT Gateway and single-AZ data tier in a *test* environment | Multi-AZ across the data tier roughly doubles Aurora and ElastiCache cost |

---

**Next:** [02-component-and-sizing-matrix.md](./02-component-and-sizing-matrix.md) — the request table.
