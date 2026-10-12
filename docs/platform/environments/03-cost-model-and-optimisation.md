# 03 — Cost Model and Optimisation

**Companion to:** [`01-uat-phased-build-plan.md`](./01-uat-phased-build-plan.md) ·
[`02-aws-component-configuration.md`](./02-aws-component-configuration.md)

> **Read this first.** Every figure is a **list-price estimate for `ap-south-1` (Mumbai) as of
> August 2026**, rounded, before any Enterprise Discount Program or private-pricing agreement the
> bank may already hold. The arithmetic is shown on every line specifically so it can be re-derived
> in the AWS Pricing Calculator and corrected — do not take a number to a budget committee without
> that step. Recorded as **ASM-CAND-1**. USD→INR conversions use ₹86/$.
>
> The *ratios* and the *ordering of the optimisation levers* are robust to price changes. The
> absolute totals are not.

---

## 1. The headline

| | UAT $/month | ₹/month | Basis |
|---|---|---|---|
| Naive prod-mirror UAT | **~$4,700** | ~₹4.0 L | §5 |
| **This plan** | **~$308** (~$345 with the bank-network VPN) | **~₹26,500** | §2 |
| Saving | **~93%** | ~₹3.7 L/month | |

Across the ~5 months from Phase U0 to go-live, that is roughly **₹18 lakh of avoided UAT spend** —
and the optimised design produces *more* gate evidence than the expensive one, because the ephemeral
dev environment proves S09-VT-01 weekly instead of once.

---

## 2. UAT steady state — line by line

Configuration as at the end of Phase U3. Business window **08:30–20:30 IST, Mon–Fri ≈ 217 hours per
month** (of 730). Rates in **USD**.

### 2.1 Compute — $122

| Line | Configuration | Arithmetic | $/mo |
|---|---|---|---|
| EKS control plane | 1 cluster | `730 h × $0.10` | **73.00** |
| System node group | 1 × `t4g.medium` On-Demand, 24×7 | `730 × $0.0336` | **24.53** |
| Workload capacity | 3 × `m7g.large` **Spot**, business window only | `3 × 217 × ~$0.030` | **19.53** |
| EBS (gp3) | 4 nodes × 40 GiB | `160 × $0.0912` | **~5.00** |
| | | **Compute subtotal** | **$122.06** |

The EKS control plane is **60% of the compute bill** and cannot be scheduled away — it is billed per
cluster-hour whether or not a pod is running. That single fact is why dev is ephemeral (§4, lever 8):
the only way to stop paying for a control plane is to delete the cluster.

### 2.2 Network — $79 (with the bank VPN) / $43 (without)

| Line | Arithmetic | $/mo |
|---|---|---|
| NAT Gateway × 1 | `730 × $0.056` | **40.88** |
| NAT data processing | `~30 GB × $0.056` | **1.68** |
| Elastic IP (attached) | — | **0.00** |
| VPC gateway endpoints (S3, DynamoDB) | free by design | **0.00** |
| Site-to-Site VPN to bank DC (**from Phase U3**, DEP-2) | `730 × $0.05` | **36.50** |
| | **Network subtotal** | **$79.06** |

> The VPN appears only from Phase U3. The $345 steady-state figure below includes it; the
> Phase U1/U2 run-rates in [`01`](./01-uat-phased-build-plan.md) §5 do not, which is why they are lower.

### 2.3 Data — $71

| Line | Configuration | Arithmetic | $/mo |
|---|---|---|---|
| Aurora Serverless v2 | 1 cluster, Single-AZ, min 0 / max 4 ACU, **auto-pause outside the window** | `217 h × ~1.5 ACU avg × $0.14` | **45.57** |
| Aurora storage | ~20 GB | `20 × $0.115` | **2.30** |
| Aurora I/O + backup | 7-day retention, PITR | estimate | **~4.00** |
| DynamoDB | 4 tables, on-demand + PITR | ~3 M WRU + 8 M RRU/month | **~5.50** |
| S3 | ~40 GB across 5 buckets + requests | `40 × $0.025` + requests | **~2.50** |
| Secrets Manager | **12 consolidated secrets** (one per service, JSON) | `12 × $0.40` + API calls | **~5.30** |
| KMS | **5 CMKs** + requests | `5 × $1.00` + requests | **~5.50** |
| | | **Data subtotal** | **$70.67** |

Note the two consolidation decisions visible here: **one secret per service instead of one per
credential** (18 services × 3 credentials × $0.40 = $21.60 → $5.30), and **five CMKs by data class
instead of one per service** ($18 → $5.50). Neither weakens the control; both are simply the cheaper
way to express the same isolation.

### 2.4 Edge — $32

| Line | Arithmetic | $/mo |
|---|---|---|
| ALB × 1 (**shared via `IngressGroup`**) | `730 × $0.0243` | **17.74** |
| ALB LCU | low volume | **~1.50** |
| WAF Web ACL + 5 rules | `$5 + 5 × $1` | **10.00** |
| WAF requests | ~2 M | `2 × $0.60` | **1.20** |
| CloudFront | within the always-free 1 TB / 10 M requests | **0.00** |
| Route 53 | 1 hosted zone + queries | **~1.00** |
| ACM | public certificates | **0.00** |
| | **Edge subtotal** | **$31.44** |

### 2.5 Observability — $21

| Line | Configuration | $/mo |
|---|---|---|
| Amazon Managed Service for Prometheus | ~18 services × ~1,500 series, 30 s scrape, business window | **~14.00** |
| Grafana | **OSS pod** — Managed Grafana would be `5 editors × $9 = $45` | **0.00** |
| Loki storage | S3, 14-day retention, ~30 GB | **~1.00** |
| CloudWatch Logs | EKS control plane + Lambda only, ~5 GB | `5 × $0.67` + storage | **~4.00** |
| X-Ray | 5% head sampling, 100% on error | **~1.50** |
| | **Observability subtotal** | **$20.50** |

### 2.6 Security and governance (UAT account's share) — $21

| Line | Configuration | $/mo |
|---|---|---|
| AWS Config | **changed resources only**, noisy resource types excluded | **~10.00** |
| GuardDuty | incl. EKS Runtime Monitoring, low volume | **~8.00** |
| CloudTrail | management events (first copy free); **data events off in UAT** | **~1.00** |
| VPC Flow Logs | 10% sample → S3 | **~2.00** |
| | **Security subtotal** | **$21.00** |

### 2.7 UAT total

| Group | $/mo |
|---|---|
| Compute | 122.06 |
| Network (incl. VPN from U3) | 79.06 |
| Data | 70.67 |
| Edge | 31.44 |
| Observability | 20.50 |
| Security & governance | 21.00 |
| **UAT total** | **≈ $345/month (~₹29,700)** |
| **UAT total excluding the bank VPN** (which is DEP-2 and may be Direct Connect instead) | **≈ $308/month (~₹26,500)** |

---

## 3. Whole-programme cost

### 3.1 Now → go-live (Aug 2026 – Jan 2027)

| Account / env | $/mo | Notes |
|---|---|---|
| Org baseline (management + 2 security accounts) | ~45 | Control Tower guardrails: Config, GuardDuty, Security Hub |
| `shared-services` | ~20 | ECR, Terraform state, Argo CD on the UAT cluster, artifacts |
| **`nonprod-uat`** | **~345** | §2 |
| `nonprod-dev` (**ephemeral**) | ~40 | Mon 08:00 → Fri 20:00, destroyed weekly |
| **Monthly during build** | **≈ $450 (~₹38,700)** | |
| **U0 → U5 total (5.5 months)** | **≈ $2,500 (~₹2.15 L)** | + ~$150 of one-off Phase U4 drill windows |

### 3.2 At go-live (Jan 2027 onward)

| Account / env | $/mo | Notes |
|---|---|---|
| Org baseline + `shared-services` | ~80 | Config/GuardDuty across 7 accounts, prod data events on |
| `nonprod-dev` | ~40 | |
| `nonprod-uat` | ~345 | |
| **`prod` (ap-south-1)** | **~1,100** | §3.3 |
| **`prod` DR (ap-south-2, warm standby)** | **~350** | Aurora Global secondary, minimal EKS, replicated stores |
| **Total at go-live** | **≈ $1,915/month (~₹1.65 L)** | |

### 3.3 Production breakdown (~$1,100 + $350 DR)

| Line | Configuration | $/mo |
|---|---|---|
| EKS control plane | 1 cluster | 73 |
| Nodes | 3 × `m7g.large` On-Demand baseline **with a 1-year Compute Savings Plan (~28% off)** + Karpenter Spot burst | ~150 |
| NAT × 3 + data | 1 per AZ | ~130 |
| Aurora Serverless v2 Multi-AZ | writer + reader, min 1 / max 8 ACU, 35-day retention | ~380 |
| DynamoDB | on-demand + PITR + Global Tables | ~30 |
| S3 | + Object Lock 7 y + CRR | ~25 |
| ALB + WAF + CloudFront + Route 53 | | ~70 |
| Observability | AMP 90 d + Managed Grafana (5 editors) + CloudWatch | ~150 |
| Security | GuardDuty + Config + Security Hub + Inspector, data events on | ~65 |
| Secrets + KMS | | ~25 |
| **Prod ap-south-1** | | **~1,098** |
| **DR ap-south-2** | Aurora Global secondary (~200), minimal EKS (~110), S3/DynamoDB replication (~40) | **~350** |

**Cost per issued policy** — this closes **NFR-OPEN-5** ("cost model per issued policy", owner
Shivanshi + Kalpana, target S14). At the CAP-A derived BAU rate of 100 journey starts/hour × 10 hours
× 22 working days ≈ **22,000 journey starts/month**, and assuming a 10% conversion to issued policy
(≈ 2,200 policies), the fully-loaded infrastructure cost is **≈ $0.87 (~₹75) per issued policy**.
The conversion assumption is Rajal's to supply; the infrastructure denominator is what this document
contributes. Note that the cost is almost entirely **fixed** at this scale — doubling volume barely
moves it, which is the honest form of the finding that R0 is not throughput-constrained.

---

## 4. The optimisation levers, ranked

Each lever is stated with what it saves, what it costs you, and when to reverse it.

| # | Lever | Saves ($/mo, UAT) | Trade-off | Reverse when |
|---|---|---|---|---|
| **1** | **No Amazon MSK.** The HDL already mandates a transactional outbox and *"no Kafka in R0"* | **~450** | Outbox polling adds latency to event propagation; NFR-DAT-05 (audit outbox lag p95 < 30 s) bounds it | R1, if fan-out consumers or replay requirements appear |
| **2** | **One Aurora Serverless v2 cluster, min 0 ACU, Single-AZ**, instead of 8 provisioned Multi-AZ clusters | **~3,150** | Shared blast radius; a ~15 s resume after auto-pause | Prod (Multi-AZ, min 1 ACU) — and per-context clusters at R1 if Aarti requires blast-radius separation |
| **3** | **Scale to zero outside 08:30–20:30 IST weekdays** (KEDA cron + Karpenter consolidation + Aurora auto-pause) | **~90** | UAT is unavailable for 70% of the week; overnight batch tests must be scheduled inside the window | Never in prod; extend the UAT window during load-test phases |
| **4** | **One shared ALB via `IngressGroup`** instead of one ALB per Ingress | **~320** | All routes share one ALB's listener rules (100-rule limit — ample for 18 services) | Never; this is correct in prod too |
| **5** | **Graviton + Spot via Karpenter** instead of On-Demand x86 | **~140** | Spot interruptions; requires `linux/arm64` images | Prod sale-path nodes are On-Demand; Spot stays for batch and recon |
| **6** | **1 NAT Gateway + free S3/DynamoDB gateway endpoints** instead of 3 NAT + interface endpoints | **~100** | No egress AZ-redundancy in UAT | Prod (3 NAT); UAT temporarily during the Phase U4 HA drill |
| **7** | **Loki-on-S3 + Grafana OSS** instead of CloudWatch ingestion + Managed Grafana | **~60** | You operate Loki; managed AMP still carries metrics | Prod uses Managed Grafana for the audited access trail |
| **8** | **Ephemeral dev** instead of a third always-on cluster | **~130** | Dev is unavailable at weekends | Never — and it *strengthens* S09-VT-01/G3 |
| **9** | **No ElastiCache** (DynamoDB `idempotency` table + in-process Caffeine) | **~90** | Cache is per-pod, not shared | R1, with measured evidence of cache pressure |
| **10** | **No API Gateway** (ALB + WAF rate-based rules) | **~20** | No per-consumer API keys | When a partner consumer exists |
| **11** | **Consolidated secrets (1/service) and CMKs by data class (5)** | **~30** | Rotating one secret rotates a service's whole credential set | Never; this is correct in prod too |
| **12** | **Object Lock with a 1-day horizon in UAT** | storage, compounding | Cannot be used to prove the 7-year *duration* | Prod: 2,555 days, where the obligation is real |
| **13** | **`prod` account created in Phase U5**, not U0 | ~120 over the build | Prod IaC is exercised later | — |
| **14** | **Compute Savings Plan only after the baseline is measured** | avoids over-commitment | Full On-Demand rates until U5 | Buy at U5 against measured usage, never against a forecast |

---

## 5. What the naive alternative costs (the ~$4,700 comparator)

Stated so the saving is auditable rather than asserted. This is UAT built as a faithful mirror of the
prod architecture in `04-aws-infrastructure-architecture.md`:

| Line | Naive configuration | $/mo |
|---|---|---|
| Aurora | 8 × provisioned `db.r6g.large` Multi-AZ, one per bounded context | ~3,200 |
| Amazon MSK | 3 × `kafka.m5.large`, 3 AZ | ~460 |
| ALBs | one per service Ingress × 17 | ~340 |
| Nodes | 6 × `m6i.large` On-Demand x86, 24×7 | ~380 |
| NAT | 3 Gateways | ~123 |
| EKS control plane | 1 | 73 |
| ElastiCache | 2-node `cache.t4g.small` | ~50 |
| Managed Grafana | 5 editors | 45 |
| CloudWatch Logs | all application logs, ~120 GB/month | ~85 |
| Everything else | edge, security, secrets, KMS, DynamoDB, S3 | ~60 |
| **Total** | | **~$4,816** |

Both columns satisfy the *architecture*. Only one of them is proportionate to a workload of
**1.7 journey starts per minute**.

---

## 6. Cost guardrails — controls, not intentions

| Control | Implementation | Owner |
|---|---|---|
| **Mandatory tagging** | SCP denies resource creation without `env`, `workstream`, `service`, `owner`, `cost-centre`, `data-class`. Terraform `default_tags` supplies them | Shivanshi |
| **Budgets with actions** | Per-account monthly budget; alerts at 50/80/100%; at **120% a budget action detaches the non-prod node scaling policy**. Prod budgets alert but never auto-act | Kalpana + Shivanshi |
| **Cost diff on every IaC PR** | **Infracost** comments the monthly delta on the pull request. A PR that adds $500/month says so before it merges, not at month end | Amit |
| **Cost Anomaly Detection** | Per-service monitors, alert on >20% daily deviation | Shivanshi |
| **Weekly schedule verification** | A synthetic check confirms UAT actually scaled to zero at 20:30 IST and dev was actually destroyed on Friday. **A scale-down that silently stops working is the classic way this plan quietly reverts to $4,700** | Shivanshi |
| **Monthly review** | Cost report against this model in the Delivery Control System cadence; variance > 15% is a delivery risk item | Kalpana |
| **Idle detection** | Trusted Advisor / Compute Optimizer reviewed monthly for idle load balancers, unattached EBS and EIPs, oversized nodes | Shivanshi |

---

## 7. Where this model is most likely to be wrong

Stated plainly, because an estimate presented without its error bars is a forecast pretending to be
a fact.

| Risk to the estimate | Direction | Size |
|---|---|---|
| Aurora ACU average (assumed 1.5) — JVM connection pools keep a Serverless v2 cluster warmer than expected | **up** | up to +$40/mo if the average is 3 ACU |
| **Data transfer**, consistently the most under-estimated AWS line: cross-AZ pod-to-pod, cross-AZ pod-to-Aurora, and NAT processing on 1SB/PG traffic | **up** | +$20–60/mo; mitigated by topology-aware routing and by keeping UAT single-AZ |
| Log volume above the assumed 20 GB/month (debug logging in UAT is normal and useful) | up | +$10–30/mo |
| Spot interruption forcing On-Demand fallback | up | +$25/mo worst case |
| GuardDuty EKS Runtime Monitoring at higher event volumes | up | +$10–20/mo |
| Bank connectivity turning out to require **Direct Connect** rather than Site-to-Site VPN | **up, materially** | Direct Connect is port-hours + a carrier circuit — potentially $300+/month and a much longer lead time. **This is DEP-2 and is the largest single unknown in the model** |
| An existing AWS EDP / private pricing agreement held by the bank | **down** | typically 5–20% |

**Recommended budget ask:** the modelled build-phase figure **plus 25% contingency** —
**≈ $560/month (~₹48,000)** through January 2027, and **≈ $2,400/month (~₹2.1 L)** from go-live.
Ask for the contingency explicitly rather than discovering it; DEP-2 alone can consume most of it.

---

**signature_status:** `AI-DRAFTED — figures are list-price estimates requiring validation in the AWS
Pricing Calculator before any budget submission (ASM-CAND-1). Mandatory human review outstanding
(Shivanshi, Kalpana, Aarti).`
**Date:** 2026-08-18
