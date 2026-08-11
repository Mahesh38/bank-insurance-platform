# 02 — Component & Sizing Matrix

**Up:** [UAT environment plan](./README.md)
**Audience:** Platform / Cloud Engineering. This is the provisioning request.

Everything below is `ap-south-1` (Mumbai), UAT account only. Where a configuration differs from
what production will need, that is called out — UAT is not a small production, and pretending it
is costs money without buying confidence.

---

## The sizing model — challenge this number first

Every compute figure in this document derives from one assumption:

> **A Spring Boot 3.3 / JDK 21 service under UAT load requests `250m` CPU and `1Gi` memory,
> with limits of `1000m` CPU and `1.5Gi` memory.**

```yaml
resources:
  requests: { cpu: 250m, memory: 1Gi }
  limits:   { cpu: 1000m, memory: 1.5Gi }
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
```

Three things about that model matter:

1. **JVM services in UAT are memory-bound, not CPU-bound.** The instinct is to request 500m–1000m
   CPU because that's what production sizing guides say. Under UAT concurrency the CPU sits idle
   while memory stays pinned — so requesting 250m nearly doubles pod density per node at no
   observable cost. The `limits` still allow a full core for startup and burst.
2. **`MaxRAMPercentage=70` inside a 1.5Gi limit** gives ~1GB heap with room for metaspace, thread
   stacks, and direct buffers. Setting `-Xmx` explicitly instead is the usual cause of UAT
   OOMKills after a limit change.
3. **`UseSerialGC` and `TieredStopAtLevel=1` are UAT-only.** They cut startup time and footprint
   materially at low load. Do **not** carry them to production — they will hurt throughput.
   Keep them in a `uat` Spring profile / Helm values file, not in the Dockerfile.

Platform overhead per cluster — CoreDNS, VPC CNI, kube-proxy, metrics-server, AWS Load Balancer
Controller, External Secrets, Argo CD, Karpenter, cert-manager — is **~1.5 vCPU / 4 GiB**, rising
to ~2.5 vCPU / 8 GiB once in-cluster Prometheus lands in Phase 2.

---

## Phase 1 — components

**Weeks 1–4.** ~13 app pods · requests ≈ 5 vCPU / 17.5 GiB including overhead.

### Compute & cluster

| Component | Configuration | Qty | Notes |
|---|---|---|---|
| EKS cluster | Kubernetes 1.31, control-plane logging on (`api`, `audit`, `authenticator`) | 1 | $73/mo fixed — the one line no schedule can reduce |
| Managed node group | `m7g.large` (2 vCPU / 8 GiB), Bottlerocket AMI, ARM64 | desired **3**, min 2, max 6 | Graviton. **Requires multi-arch images** |
| Node disk | `gp3`, 50 GB, 3000 IOPS | per node | `gp3` not `gp2` — ~20% cheaper, IOPS decoupled from size |
| Karpenter | v1.x, `consolidationPolicy: WhenEmptyOrUnderutilized` | 1 | Installed in Phase 1 so nodes drain themselves at shutdown |
| Namespaces | `edge`, `core-sales`, `integration`, `platform`, `identity` | 5 | Layer-aligned per [architecture-review/04](../architecture-review/04-aws-infrastructure-architecture.md) |

Node arithmetic: 3 × `m7g.large` = 6 vCPU / 24 GiB raw, ≈ **5.3 vCPU / 19.8 GiB allocatable**
after kubelet and system reservations. Against ~5 vCPU / 17.5 GiB of requests that is tight
during rolling deploys — which is exactly what `max: 6` and Karpenter are for. **Memory is the
binding constraint, not CPU.**

### Network

| Component | Configuration | Qty | Notes |
|---|---|---|---|
| VPC | `10.60.0.0/16`, 3 AZs | 1 | Non-overlapping with prod and bank ranges — confirm with bank network team |
| Private subnets | /20 per AZ (EKS nodes, Aurora, ElastiCache) | 3 | |
| Public subnets | /24 per AZ (NAT, ALB) | 3 | |
| **NAT Gateway** | Single, in AZ-a, with **Elastic IP** | **1** | Deliberately not one per AZ. The fixed EIP is what 1SB whitelists |
| **S3 Gateway VPC Endpoint** | Gateway type | 1 | **Free.** Removes ECR layer pulls and S3 traffic from NAT data processing — the single highest-value network optimisation |
| ALB | Internet-facing, via AWS Load Balancer Controller | 1 | |
| Route 53 | Private + public hosted zone | 1 | |
| ACM | TLS cert for the UAT hostname | 1 | Free |

### Data

| Component | Configuration | Qty | Notes |
|---|---|---|---|
| **Aurora PostgreSQL Serverless v2** | PostgreSQL 16.4, **`min_capacity: 0`**, `max_capacity: 2`, auto-pause after 10 min idle, Single-AZ writer | **1 cluster** | `min_capacity: 0` requires PG 13.15+/14.12+/15.7+/16.3+. This is what makes the DB follow the shutdown schedule with zero automation |
| Aurora storage | 20 GB initial, 7-day backup retention | | Backup retention 7 days in UAT, not 35 |
| Logical databases | One per service, own role, **no cross-database grants**, own Flyway history | 4–6 | Ownership boundary enforced by grants, not by separate clusters |
| **ElastiCache** | Valkey/Redis 7.x, `cache.t4g.micro`, **single node, no replica** | 1 | ~$12/mo. **Not** ElastiCache Serverless — its ~1 GB minimum bills ~$90/mo, 7× more at this scale |
| S3 | `uat-raw-payload`, `uat-documents`, `uat-artifacts` | 3 | **30-day lifecycle → Delete.** See the Object Lock warning below |
| ECR | One repo per service, lifecycle: expire untagged after 3 days, keep last 10 tagged | ~10 | |

> ⚠️ **Never enable S3 Object Lock in UAT.** The 7-year retention with Object Lock in
> [architecture-review/05](../architecture-review/05-data-architecture.md) is a *production*
> compliance control. Enabled in UAT it makes objects — and the bucket — literally undeletable
> for seven years, converting a test artefact into a permanent bill. UAT gets a 30-day
> expiry lifecycle rule instead.

### Security & platform services

| Component | Configuration | Qty |
|---|---|---|
| AWS Secrets Manager | 1SB credentials, DB creds, Keycloak client secrets | ~25 secrets |
| SSM Parameter Store | Non-secret config, feature flags, scheduler override | Standard tier (free) |
| KMS | 2 customer-managed keys (platform default + raw payload) | 2 |
| IAM | **EKS Pod Identity** (preferred over IRSA — simpler trust policy, no OIDC juggling) | per service |
| CloudWatch Logs | **7-day retention set at log-group creation** | — |
| Argo CD | In-cluster, app-of-apps | 1 |
| External Secrets Operator | Syncs Secrets Manager → K8s secrets | 1 |
| Keycloak | Self-hosted in `identity` namespace, backed by Aurora | 1 pod |

> ⚠️ **CloudWatch Logs default to never expire.** A log group created without an explicit
> retention policy accrues cost forever. Enforce 7-day retention via an AWS Config rule or a
> Terraform/CDK aspect at creation time — retro-fixing this after six months is the most common
> silent cost leak in a UAT account.

---

## Phase 2 — components added

**Weeks 5–14.** ~23 app pods · requests ≈ 8.3 vCPU / 32 GiB including overhead.

| Component | Change from Phase 1 | Notes |
|---|---|---|
| Managed node group | **3 × `m7g.xlarge`** (4 vCPU / 16 GiB), min 2, max 6 | ≈ 11.7 vCPU / 42 GiB allocatable — ~25% headroom |
| **Karpenter Spot NodePool** | `m7g.large`–`m7g.2xlarge`, `capacity-type: spot`, max 6 nodes | CI runners, batch, burst. ~70% discount |
| Node disk | `gp3` 60 GB | |
| **Aurora → 2 clusters** | `core` (0–4 ACU) · **`regulated`** (0–2 ACU, **dedicated CMK**) | Consent + Payment separated so per-sensitivity-tier CMK and append-only grants are testable |
| ElastiCache | → `cache.t4g.small` (1.37 GiB) | Now serving idempotency keys + sessions |
| **DynamoDB** | 4 tables, **`PAY_PER_REQUEST`** | journey-state, quote-jobs, sessions, hub-routing. On-demand = zero cost while switched off |
| **API Gateway** | HTTP API (not REST API — ~70% cheaper) in front of both BFFs | |
| **AWS WAF** | 1 regional web ACL + OWASP managed rule groups | |
| **SNS + SQS** | Notification, document-gen, reconciliation queues | Pay-per-request. **Not MSK** — see [01](./01-phase-plan-and-scope.md#why-snssqs-and-not-msk-here) |
| ALB | + 1 internal ALB | |
| **In-cluster observability** | `kube-prometheus-stack` + Loki | Runs on worker nodes → stops with them. No AWS charge |
| **OpenCost** | Per-namespace cost showback | Free. Tells each squad what its services cost |
| Secrets Manager | ~45 secrets | |
| KMS | 4 CMKs (+ regulated tier, + DynamoDB) | |

---

## Phase 3 — components added

**Weeks 15–24.** ~34 app pods · requests ≈ 14.5 vCPU / 45 GiB including mesh and overhead.

| Component | Change from Phase 2 | Notes |
|---|---|---|
| Managed node group | **4 × `m7g.xlarge`**, min 3, max 8 | ≈ 15.6 vCPU / 56 GiB allocatable |
| Karpenter Spot | + load-generator pool, max 6 | Burst only during test windows |
| **Amazon MSK** | **3 × `kafka.t3.small`**, 100 GB EBS per broker, 3 AZs | ~$147/mo, **cannot be stopped**. See the MSK note below |
| **Istio — ambient mode** | ztunnel DaemonSet + waypoint per namespace | **Not sidecar mode.** 34 sidecars ≈ 3.4 vCPU / 4.3 GiB of pure overhead ≈ one whole `m7g.xlarge` |
| **Aurora → 3 clusters** | + `integration`; `core` max **8 ACU** for load tests | |
| **Aurora reader** | 1 reader on `regulated` | Makes Multi-AZ failover a tested behaviour, not an assumption |
| ElastiCache | → `cache.t4g.medium` × 2, **Multi-AZ**, encryption in transit + AUTH | Failover testing |
| DynamoDB | 7 tables; **PITR on `audit-events` only** | PITR is $0.22/GB-mo — enable selectively |
| **AMP + Amazon Managed Grafana** | 3 editors, 10 viewers | Production parity. Managed Grafana is per-user, per-month — audit the user list |
| **CloudFront + WAF** | Edge distribution | |
| **Athena + Glue** | Over the S3 data lake for MIS | **Not Redshift Serverless** — 8-RPU floor ≈ $2,100/mo |
| **GuardDuty · Security Hub · AWS Config** | UAT account | |
| **AWS Backup** | Cross-region copy → `ap-south-2` | Proves the restore runbook. **Not a warm standby** |
| Secrets Manager | ~70 secrets | |

> **MSK is the one component that breaks the cost model.** It cannot be stopped, only deleted, and
> MSK Serverless is worse in UAT — a $0.75/hour cluster floor is ~$547/month regardless of traffic,
> nearly 4× the provisioned `t3.small` option. If Phase 3 slips, MSK provisioning should slip with
> it rather than be stood up early "so it's ready".

---

## Configuration standards — apply from Phase 1

These cost nothing to set on day one and are expensive to retrofit:

| Standard | Value | Why |
|---|---|---|
| **Container images** | Multi-arch `linux/amd64,linux/arm64` via `docker buildx` | Non-negotiable prerequisite for Graviton |
| **Node OS** | Bottlerocket | Smaller attack surface, and **faster boot** — which matters when you start the cluster 22 times a month |
| **Pod identity** | EKS Pod Identity | Simpler than IRSA; no OIDC provider trust policy per role |
| **Log retention** | 7 days, set at creation | See warning above |
| **ECR lifecycle** | Untagged expire 3d; keep last 10 tagged | |
| **EBS** | `gp3` only; weekly sweep for unattached volumes | |
| **PodDisruptionBudgets** | On every Deployment | Required for Karpenter consolidation to be safe |
| **`topologySpreadConstraints`** | Across AZs, `whenUnsatisfiable: ScheduleAnyway` | `DoNotSchedule` will deadlock a 3-node cluster |
| **Mandatory tags** | `Environment`, `Workstream`, `Service`, `Owner`, `CostCentre`, `Schedule` | Enforced by Tag Policy. Without these, cost attribution is impossible |
| **IaC** | Terraform or CDK — pick one; state in S3 + DynamoDB lock | The environment is rebuilt often enough that drift matters |
| **Test data** | Synthetic / masked only. **No production PII in UAT** | Non-negotiable compliance boundary |

---

## Deliberately *not* provisioned in UAT

| Not provisioned | Reason |
|---|---|
| **Fargate profiles** | [architecture-review/04](../architecture-review/04-aws-infrastructure-architecture.md) suggests Fargate for near-idle services — sound in production. In a *scheduled-off* UAT the logic inverts: per-pod Fargate pricing beats packing pods onto EC2 that is already switched off 59% of the time |
| **Redshift Serverless** | 8-RPU floor ≈ $2,100/mo. Athena answers the same MIS questions pay-per-query |
| **MSK Serverless** | $547/mo floor vs $147 provisioned |
| **ElastiCache Serverless** | ~1 GB minimum ≈ $90/mo vs $12 for `t4g.micro` |
| **NAT Gateway per AZ** | Saves $82/mo. Single-AZ NAT failure in a test environment is an acceptable risk, and one EIP is easier to get whitelisted |
| **Aurora I/O-Optimized** | Only pays off above ~25% I/O share of spend. UAT is nowhere near that |
| **Multi-AZ writers** | Phase 3 adds one reader to *test* failover. Full Multi-AZ across the data tier is a production control |
| **Savings Plans / RIs** | A deliberately scheduled-off environment has too low a utilisation floor to commit. Buy these for production |

---

**Next:** [03-cost-estimate.md](./03-cost-estimate.md) — what all of this costs.
