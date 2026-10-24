# 02 — AWS Component Configuration (dev · UAT · prod)

**Companion to:** [`01-uat-phased-build-plan.md`](./01-uat-phased-build-plan.md)
**Rule that governs every table below:** one module, three parameter profiles. If a row cannot be
expressed as a variable, it is a design defect — S09-E02-S01 requires environments built *"from the
same IaC modules with different parameters — not hand-built variants."*

Profiles: **`minimal`** (dev, ephemeral) · **`lean`** (UAT) · **`resilient`** (prod).

---

## 1. Account and region topology

| Account | OU | Purpose | Created in |
|---|---|---|---|
| `management` | root | Organizations, Control Tower, SCPs, consolidated billing | U0 |
| `security-log-archive` | Security | Org CloudTrail, Config snapshots, VPC flow logs, **Object Lock** on all | U0 |
| `security-audit` | Security | GuardDuty/Security Hub/Config **aggregator**, read-only cross-account audit role | U0 |
| `shared-services` | Infrastructure | ECR, Terraform state, Argo CD, CI OIDC roles, Route 53 parent zone | U0 |
| `nonprod-dev` | Workloads | Ephemeral dev environment | U0 (account) / U1 (first apply) |
| `nonprod-uat` | Workloads | UAT | U0 (account) / U1 |
| `prod` | Workloads | Production | **U5** — created late on purpose; an empty governed account still bills Config + GuardDuty |

**Regions:** `ap-south-1` primary, `ap-south-2` DR. Enforced by SCP `DenyNonIndiaRegions` **and** by
`conftest` in the pipeline — belt and braces, because FF-08 and NFR-DAT-06 are licence conditions,
not preferences. Global-only services (IAM, Route 53, CloudFront, WAF-CloudFront-scope, Organizations)
are the sole SCP exceptions and are listed explicitly, never wildcarded.

---

## 2. Network

### 2.1 CIDR plan — allocated once, in U0, for all environments

| Environment | VPC CIDR | Public (NAT/ALB) | Private-app (EKS pods) | Private-data (Aurora/ElastiCache) |
|---|---|---|---|---|
| dev | `10.30.0.0/16` | `10.30.0.0/24`, `.1.0/24`, `.2.0/24` | `10.30.16.0/20`, `.32.0/20`, `.48.0/20` | `10.30.64.0/24`, `.65.0/24`, `.66.0/24` |
| **uat** | `10.40.0.0/16` | `10.40.0.0/24`, `.1.0/24`, `.2.0/24` | `10.40.16.0/20`, `.32.0/20`, `.48.0/20` | `10.40.64.0/24`, `.65.0/24`, `.66.0/24` |
| prod | `10.50.0.0/16` | `10.50.0.0/24`, `.1.0/24`, `.2.0/24` | `10.50.16.0/20`, `.32.0/20`, `.48.0/20` | `10.50.64.0/24`, `.65.0/24`, `.66.0/24` |
| prod-DR (ap-south-2) | `10.60.0.0/16` | same shape | same shape | same shape |

`/20` per AZ for pods = 4,094 usable IPs per AZ. With the VPC CNI in prefix-delegation mode this
comfortably carries R0 and several multiples of it. **Non-overlapping with each other and reserved
against the bank's on-premises ranges** — confirm the bank's allocation before the U0 apply, because
changing a VPC CIDR later means rebuilding the environment.

### 2.2 Per-environment network parameters

| Parameter | `minimal` (dev) | `lean` (UAT) | `resilient` (prod) |
|---|---|---|---|
| AZs | 2 | **3 subnets, 1 AZ actively used** | 3 |
| NAT | **NAT instance** (`fck-nat`, t4g.nano, ~$4/month) | **1 NAT Gateway + static EIP** | **3 NAT Gateways**, one per AZ |
| Fixed egress IP | not required | **required** — 1SB whitelisting | **required** |
| VPC endpoints | S3, DynamoDB (gateway, free) | S3, DynamoDB (gateway, free) | + interface endpoints for ECR api/dkr, STS, Secrets Manager, CloudWatch Logs |
| Flow logs | off | **10% sample → S3** | **100% → S3**, 90-day retention |
| Bank connectivity (CBS) | none — CBS is stubbed | **Site-to-Site VPN**, 2 tunnels, BGP | Site-to-Site VPN or Direct Connect per bank mandate |

> **Why one NAT Gateway in UAT.** Three NAT Gateways buy AZ-failure survivability for *egress*. UAT
> does not have an availability SLO, and the AZ-failure behaviour that matters is exercised
> deliberately in the Phase U4 HA drill, when the second NAT is temporarily added. Two NAT Gateways
> sitting idle for five months cost roughly ₹70,000 to prove nothing.

> **Why interface endpoints are *not* in UAT.** They look like a NAT saving and are not, at this
> scale. Each interface endpoint is ~$0.013/hour per AZ (~$9.50/month). UAT pulls perhaps 150 GB/month
> through NAT — about $8.40 of processing. Two ECR endpoints alone would cost more than the traffic
> they divert. In prod, at 3 AZs and higher volume, the arithmetic reverses; that is why the profile
> differs.

### 2.3 Security groups and network policy

Default-deny throughout. Each rule below is a `security_group_rule` in the network module, not a
console click.

| From | To | Port | Purpose |
|---|---|---|---|
| Internet | ALB SG | 443 | Public entry — the only ingress in the whole VPC |
| ALB SG | node SG | 30000–32767 | ALB target group → pods |
| node SG | node SG | all | Intra-cluster |
| node SG | Aurora SG | 5432 | Application → database |
| node SG | VPC endpoints SG | 443 | AWS API access |
| node SG | 0.0.0.0/0 via NAT | 443 | 1SB, AU Bank PG, insurer callbacks — the single audited egress path |
| VPN/CBS CIDR | node SG | 443 | CBS ETB lookup, initiated inbound-from-bank where required |

Kubernetes NetworkPolicy mirrors the HDL seams: namespaces `edge`, `identity`, `core-sales`,
`fulfilment`, `integration`, `platform`; default-deny ingress and egress in every one; explicit
allows only where the HDL draws an arrow. **`core-sales` and `fulfilment` may not reach the internet
at all** — all provider traffic goes through `integration` (SC-W3-5, *"ALL provider traffic routes
here"*), and the NetworkPolicy is what makes that architectural statement true at runtime rather
than aspirational.

---

## 3. Compute — EKS

| Parameter | `minimal` | `lean` (UAT) | `resilient` (prod) |
|---|---|---|---|
| Kubernetes version | 1.31 | 1.31 | 1.31 (n-1 policy; never the newest in prod) |
| API endpoint | public, CIDR-allowlisted | **private + CIDR-allowlisted public** for CI | **private only**; CI via in-VPC runner |
| Control-plane logs | api, audit | api, audit, authenticator | api, audit, authenticator, controllerManager, scheduler |
| System node group | 1 × `t4g.small` | **1 × `t4g.medium` On-Demand** | 3 × `m7g.large` On-Demand (1/AZ) + Savings Plan |
| Workload capacity | Karpenter, Spot, Graviton | **Karpenter, Spot-first, Graviton, consolidation on** | Karpenter: On-Demand for the sale path, Spot for batch/recon |
| Node instance families | `t4g`, `m7g` | `m7g`, `c7g`, `r7g` (let Karpenter choose) | `m7g`, `c7g` On-Demand + Spot pool for batch |
| AMI | Bottlerocket ARM64 | **Bottlerocket ARM64** — smaller attack surface, faster boot, atomic updates (S09-E07-S03) | Bottlerocket ARM64 |
| Replicas per service | 1 | **1** (2 during the U4 HA drill) | **≥ 2 across ≥ 2 AZs** (HDL requirement, NFR-AVL-01) |
| PodDisruptionBudget | none | `minAvailable: 0` | **`minAvailable: 1` on every service**, enforced by admission policy |
| Scale-to-zero | destroyed nightly | **KEDA cron scaler: 0 replicas outside 08:30–20:30 IST, Mon–Fri** | never |

**Graviton is not optional here** — every service is a JVM 21 Spring Boot application with a
multi-arch-capable build, `arm64` gives roughly 20% better price/performance than the equivalent x86
instance, and Bottlerocket ARM64 is a first-class EKS AMI. The only prerequisite is that the
`Dockerfile` produces a `linux/arm64` image; confirm this in the S08 pipeline before Phase U1.

### 3.1 Pod sizing — the 18 R0 services

Spring Boot on JVM 21 with `-XX:MaxRAMPercentage=75.0`, CDS archive enabled, and
`-XX:+UseSerialGC` for the small-heap services (below 1 GiB, Serial beats G1 on both footprint and
startup).

| Tier | Services | CPU req / limit | Mem req / limit | UAT replicas |
|---|---|---|---|---|
| **Hot path** | Journey Orchestration, RM Workspace BFF, Integration Hub | 300m / 1000m | 1 Gi / 1 Gi | 1 |
| **Standard** | Customer, Consent, Suitability, Quotation, Proposal, Payment, Policy, 1SB Adapter, Audit, identity-authorization (PDP), workforce-access-bff, bank-persistence | 200m / 800m | 768 Mi / 768 Mi | 1 |
| **Light** | Product Catalogue, Lead, Notification, identity-provider-adapter | 150m / 500m | 512 Mi / 512 Mi | 1 |
| **Platform** | Keycloak (UAT only), Grafana, Loki, ADOT, Argo CD agent, Karpenter, KEDA | — | ~3 Gi total | 1 |

**UAT working set:** ~4.0 vCPU requested, ~15 GiB requested → **3 × `m7g.large`** (6 vCPU / 24 GiB)
during the business window, dropping to the single `t4g.medium` system node outside it.
Requests equal limits on memory (JVMs do not benefit from memory burst, and equal req/limit gives
`Guaranteed` QoS, so a noisy neighbour cannot evict a service mid-journey). CPU limits sit above
requests because JVM startup is CPU-bursty and a throttled start looks like a failed readiness probe.

**Note on the PDP.** NFR-LAT-09 gives the authorisation decision a **300 ms budget, no retry, fail
closed (S-02)** and it is called on *every* request. Give it the same tier as the hot path in prod
and keep its HPA floor at 2 — a cold-started PDP fails closed, which means the entire platform
fails closed.

---

## 4. Data stores

### 4.1 Aurora PostgreSQL

| Parameter | `minimal` | `lean` (UAT) | `resilient` (prod) |
|---|---|---|---|
| Deployment | Serverless v2, 1 writer | **Serverless v2, 1 writer, Single-AZ** | **Serverless v2, writer + reader, Multi-AZ** |
| Capacity | 0–2 ACU | **min 0 (auto-pause) / max 4 ACU** | **min 1 / max 8 ACU** per instance |
| Engine | PostgreSQL 16.x | 16.x | 16.x |
| Clusters | 1 | **1** | **1** (see ADR-CAND-02) or 3 by trust tier if Aarti requires blast-radius separation |
| Databases | 1 per service | **1 per bounded context** | 1 per bounded context |
| Auth | password in Secrets Manager | **IAM database authentication** per service, via IRSA | IAM database authentication, mandatory |
| Backup retention | 1 day | **7 days**, PITR | **35 days**, PITR |
| Cross-region | none | none | **Aurora Global Database → ap-south-2** |
| Encryption | CMK `k-pii` | CMK per data class | CMK per data class, separate key per environment |
| Deletion protection | off | off | **on** |
| Connection budget | — | **NFR-THR-06 asserted in the IaC review**: `Σ(pods × pool size) ≤ 60%` of `max_connections` at maximum replica count | same, re-measured at S12 |

**Auto-pause is the single biggest data-layer saving.** Aurora Serverless v2 scaling to **0 ACU**
after an inactivity window means the UAT database costs nothing for the ~118 hours a week nobody is
testing. Storage and backups still bill (a few dollars). Resume takes ~15 seconds — irrelevant for
UAT, and the reason `min = 1 ACU` in prod.

**Per-service isolation without per-service clusters.** Each bounded context gets its own database,
its own PostgreSQL role, its own `rds_iam` grant and its own IRSA role. `journey_svc` physically
cannot connect to `payment_db`. That is what ARCH-004's stated control — *"no cross-service DB
access (ArchUnit + IAM, verified in the S09 IaC scan)"* — actually tests, and it is verified by the
same fitness function whether the databases sit on one cluster or eight. The residual difference is
**blast radius**, and that is Aarti's decision to accept for R0 volumes (ADR-CAND-02).

### 4.2 DynamoDB

| Table | Key | UAT | Prod |
|---|---|---|---|
| `journey-state` | `PK journeyId` | On-demand, PITR, CMK, TTL 90 d | On-demand, PITR, **Global Table → ap-south-2** |
| `quote-jobs` | `PK quoteId`, `SK providerId` | On-demand, PITR, TTL 30 d | + Global Table |
| `idempotency` | `PK idempotencyKey` | On-demand, **TTL 24 h** — implements the 1SB 24-hour idempotency contract (INV-ACL-01) without ElastiCache | + Global Table |
| `outbox-cursor` | `PK serviceName` | On-demand | + Global Table |

On-demand billing throughout. At 1.7 journeys/minute, provisioned capacity with autoscaling costs
more in management attention than it saves in rupees, and on-demand absorbs the Q4 ×4 peak
(CAP-A6) with no capacity planning at all.

### 4.3 S3

| Bucket | UAT | Prod |
|---|---|---|
| `raw-payloads` | **Object Lock COMPLIANCE, default 1 day** · versioned · CMK `k-audit-immutable` · lifecycle → IA at 30 d, expire 90 d | **Object Lock COMPLIANCE, default 2,555 days (7 y)** · **CRR → ap-south-2** · no expiry inside the horizon |
| `audit-archive` | Object Lock COMPLIANCE, default 1 day | Object Lock COMPLIANCE, **7 years**, CRR, replication-time control |
| `policy-docs` | versioned, CMK `k-pii`, expire 90 d | versioned, CRR, lifecycle → Glacier IR at 180 d |
| `loki-logs` | lifecycle expire **14 d** | expire 30 d, then Glacier |
| `flow-logs`, `cloudtrail`, `config` | in `security-log-archive`, Object Lock, expire per policy | same, 7-year where regulated |

Every bucket: `BlockPublicAcls`/`IgnorePublicAcls`/`BlockPublicPolicy`/`RestrictPublicBuckets` all
true, `aws:SecureTransport` denied when false, SSE-KMS enforced by bucket policy.

> **The one-day Object Lock is deliberate and worth stating for the auditor.** S09-VT-08 asks that a
> deletion attempt under Object Lock be **refused**. A one-day COMPLIANCE retention refuses deletion
> exactly as a 2,555-day one does — the mechanism, the IAM path and the failure mode are identical.
> What differs is that UAT test data does not become undeletable until 2034. The **7-year horizon
> itself** is proven in prod under S09-G8, where it is a real obligation.

### 4.4 What is deliberately absent

| Component | Why not in R0 |
|---|---|
| **Amazon MSK** | The HDL states *"transactional outbox (S-17, at-least-once, **no Kafka in R0**)"*. Outbox rows in Aurora, polled and published. ~$400–500/month/environment avoided, plus an entire operational surface. See ADR-CAND-01 |
| **ElastiCache for Redis** | ElastiCache Serverless has a ~1 GB floor (~$90/month) and a node-based cluster still costs ~$12–50/month per environment. R0 idempotency lives in the DynamoDB `idempotency` table; catalogue caching is in-process (Caffeine, with the catalogue being a small, slow-changing R0 matrix). Revisit at R1 volumes with evidence |
| **Amazon API Gateway** | ALB + WAF covers the R0 edge; API Gateway's value is partner API-key management and per-consumer throttling, and R0 has no partner consumers. See ADR-CAND-03 |
| **Service mesh (Istio/App Mesh)** | S09 §7 names it *premature at this stage*: *"a service mesh installed before there are services to mesh is complexity with no counterparty."* mTLS between pods arrives with the mesh at R1; R0 uses TLS at the ALB and NetworkPolicy inside |
| **Redshift / Glue / Athena / QuickSight** | Reporting & MIS is `out_of_scope` beyond the pilot funnel. The pilot funnel is a handful of SQL queries against the read side |
| **Transit Gateway** | One VPC per environment, no inter-VPC traffic in R0. ~$36/month of attachments plus data processing for a topology that does not exist |

---

## 5. Security, identity and keys

### 5.1 KMS hierarchy — by data class, not by service

| CMK | Protects | Key policy grants |
|---|---|---|
| `k-pii` | Aurora (customer, consent, suitability, proposal), `policy-docs` | IRSA roles of the contexts holding PII |
| `k-financial` | Aurora payment/policy databases, settlement artefacts | Payment and Policy IRSA roles only |
| `k-audit-immutable` | `audit-archive`, `raw-payloads`, org CloudTrail | Audit service write-only; **no principal has `kms:Delete*`** |
| `k-secrets` | Secrets Manager, SSM SecureString | External Secrets Operator IRSA role |
| `k-logs` | CloudWatch Logs, Loki S3 backend, flow logs | Log producers |

Five CMKs, not eighteen. Automatic annual rotation on all. Separate key set per environment — a UAT
key can never decrypt a prod object, which is half of S09-VT-13's answer.

### 5.2 IRSA — no static credentials anywhere

One IAM role per service, assumed via the EKS OIDC provider, scoped to `system:serviceaccount:<ns>:<sa>`.
No wildcards in any prod policy (FF-09). Concretely:

| Service | May do | May not |
|---|---|---|
| Journey Orchestration | `dynamodb:*Item` on `journey-state` only; `rds-db:connect` as `journey_svc` | Touch payment or policy stores |
| Payment | `rds-db:connect` as `payment_svc`; read the PG credential secret | Read `k-pii`-encrypted customer data |
| Audit & Compliance | `s3:PutObject` + `s3:PutObjectRetention` on `audit-archive` | **`s3:DeleteObject` — denied explicitly, at both the IAM and bucket-policy layer** (FF-10) |
| 1SB Adapter | Read the 1SB credential secret; egress via NAT | Reach any database directly |
| External Secrets Operator | `secretsmanager:GetSecretValue` on `/<env>/*` | Write secrets |

The audit-service denial is worth calling out separately: FF-10 requires that *"UPDATE / DELETE
rejected"* is a property of the platform, not of the application code. Object Lock refuses the
delete, **and** no principal in the account holds the permission to attempt it. Two independent
controls, because one control that the application can bypass is not a control.

### 5.3 Platform security baseline (S09-E07)

| Control | UAT | Prod |
|---|---|---|
| Container hardening | Bottlerocket, non-root, read-only rootfs, no privileged, seccomp `RuntimeDefault` | same + **Pod Security Admission `restricted`** enforced |
| Admission policy | Kyverno audit mode | **Kyverno enforce**: signed images only, no `:latest`, resource limits mandatory, no `hostPath` |
| Image scanning | ECR enhanced scanning (Inspector) on push; pipeline blocks on Critical (NFR-SEC-03) | same, plus block on High |
| Runtime detection | GuardDuty EKS Runtime Monitoring | GuardDuty EKS Runtime Monitoring + Security Hub |
| Edge | WAF managed rules + rate-based rule | + **Shield Advanced only if the bank's risk position requires it** — it is $3,000/month and is an explicit Board 4/6 decision, not a default |
| Secrets in artefacts | `gitleaks` + image scan, build fails on any hit (NFR-SEC-02) | same |

---

## 6. Observability

| Signal | UAT | Prod | Why they differ |
|---|---|---|---|
| Metrics | **AMP** (managed), 30-day retention, ADOT scrape | AMP, 90-day retention | Managed Prometheus removes the operational burden at a cost proportional to samples — small here |
| Dashboards | **Grafana OSS pod** | Amazon Managed Grafana | AMG bills $9/editor + $5/viewer per month. UAT dashboards are read by the same five people who can `kubectl port-forward` |
| Logs | **Loki, S3 backend, in-cluster**, 14-day retention | Loki, 30-day + CloudWatch for control plane | ap-south-1 CloudWatch ingestion is ~$0.67/GB. Routing ~20 GB/month of structured JSON there costs more than Loki's entire storage bill |
| Traces | ADOT → X-Ray, **5% head sampling**, 100% on error | X-Ray, 10% + 100% on error | Sampling is the whole cost control in tracing |
| Audit events | **Separate pipeline** — outbox → Audit service → S3 Object Lock. Never through Loki or CloudWatch (S09-E05-S06) | same | Operational logs are retained for 14 days and are mutable; audit evidence is retained 7 years and is not. Mixing them contaminates both |
| Alerting | Alertmanager → SNS → email/Slack, runbook link in every payload | + PagerDuty/on-call rotation with escalation | UAT has no on-call |

**PII masking is verified in the pipeline, not assumed** (S09-E05-S02, NFR-SEC-01): the log-scan test
runs against the aggregated store after a full suite run, and S09-VT-10's pass condition is **zero**
matches for regulated field patterns. `bank-common-observability`'s `PiiMaskingConverter` is the
mechanism; the pipeline test is the evidence.

---

## 7. Deployment and promotion

| Aspect | Configuration |
|---|---|
| Registry | ECR in `shared-services`, immutable tags, scan-on-push, lifecycle policy keeping the last 10 images + anything tagged `released` |
| Promotion | **Build once, promote the digest.** `dev → uat → prod` moves an image *digest*, never a rebuild (S09-E02-S02). The tag records provenance; the digest is what deploys |
| GitOps | Argo CD in `shared-services`, one `ApplicationSet` per environment, targeting the env cluster over cross-account IRSA. Git is the only path to a cluster |
| Progressive delivery | UAT: rolling. Prod: **canary via Argo Rollouts** for the sale path (Journey, Payment, Policy, BFF); rolling for the rest, each choice justified per S09-E03-S02 |
| Database migrations | Flyway as a pre-sync Argo hook. **Expand/contract only** — every migration must be safe with the previous version still serving traffic (S09-E03-S04), which is what makes a rolling deploy and a rollback both survivable |
| Rollback | `argocd app rollback` to the previous digest. **Executed and timed in UAT in Phase U4** (S09-VT-04, NFR-DR-05) — *"an untested rollback is no rollback"* |
| Prod gating | Deploy requires a recorded approval; an audited emergency path exists (S09-E03-S05). Every deploy emits an event that annotates the Grafana dashboards so a bad release is visible immediately (S09-E03-S06) |
| Config | Externalised; per-environment values in Git (non-secret) and Secrets Manager (secret). The image is identical across environments — the same digest, byte for byte |

---

## 8. Environment configuration reference (Spring profiles)

The repository already carries `SPRING_PROFILES_ACTIVE: uat` in `docker-compose.yml`. The UAT
environment supplies these through External Secrets and ConfigMaps rather than through the compose
file:

| Variable | Source in UAT | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | ConfigMap | `uat` |
| `DATASOURCE_URL` | ConfigMap | Aurora cluster endpoint, per-service database |
| `DATASOURCE_USERNAME` | ConfigMap | Per-service PostgreSQL role |
| Database credential | **IAM auth token**, not a password | Removes the credential class entirely |
| `INSURANCE_SECRETS_SOURCE` | ConfigMap | `AWS_SECRETS_MANAGER` — this is what closes **TD-006**'s stub provider |
| `ONESB_API_KEY` / `_SECRET` / `_DISTRIBUTOR_ID` | Secrets Manager `/uat/1sb-adapter` | **`distributorId` is injected server-side; a caller-supplied value is rejected** (HDL, and `never` list) |
| `ONESB_BASE_URL` | ConfigMap | 1SB UAT sandbox |
| `RAW_PAYLOAD_ENCRYPTION_KEY` | Secrets Manager, CMK `k-audit-immutable` | Rotated in the Phase U2 exercise |
| `BANK_PERSISTENCE_BASE_URL` | ConfigMap | In-cluster service DNS |
| `CBS_BASE_URL` | ConfigMap | Over the Site-to-Site VPN path (DEP-2) |
| `PG_*` (AU Bank Payment Gateway) | Secrets Manager `/uat/payment` | Sandbox credentials (DEP-3) |

---

## 9. Prod-only components (built in Phase U5)

Listed separately so nobody builds them early "for parity" — every one of them is an ongoing cost
that buys nothing until there is production traffic.

| Component | Configuration | Justified by |
|---|---|---|
| Multi-AZ everything | 3 NAT, Aurora Multi-AZ, ≥2 pods across ≥2 AZs, PDBs | NFR-AVL-01 (99.9%), HDL |
| Aurora Global Database → ap-south-2 | Secondary cluster, managed replication | NFR-DR-02 (RPO ≤ 5 min) |
| DynamoDB Global Tables | 4 tables replicated | NFR-DR-02 |
| S3 Cross-Region Replication | `raw-payloads`, `audit-archive`, `policy-docs` | **NFR-DR-03 (RPO = 0** for audit and raw payloads) |
| Warm standby EKS in ap-south-2 | Minimal node group, Argo CD syncing the same manifests | NFR-DR-01 (RTO ≤ 1 hour) |
| Route 53 health-check failover | Latency/failover routing to the DR ALB | NFR-DR-01 |
| Compute Savings Plan | 1-year, no-upfront, sized to the **measured** always-on baseline only | ~28% off On-Demand; never bought before the baseline is measured |
| Shield Advanced | Only if Board 4/6 requires it | $3,000/month — an explicit decision, never a default |

---

**signature_status:** `AI-DRAFTED — mandatory human review outstanding (Shivanshi, Deepali, Aarti, Mahesh).`
**Date:** 2026-08-18
