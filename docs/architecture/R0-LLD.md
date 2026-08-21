# R0 Low-Level Design — AWS platform pack for the CTO and AWS platform team

**Workstream:** WS-3 · **Horizon:** H0 — R0 as designed
**Owner:** Mahesh — Principal Insurance Platform Architect (Board 1)
**Consumers:** CTO; AWS platform / landing-zone team; **Shivanshi** (SRE, Board 7 — provisions and operates); **Deepali** (Security — trust boundaries, IAM, KMS); **Aarti** (Database — Aurora/DynamoDB/S3 physical design)
**Status:** `AI-DRAFTED`. This file is the S09 *requirements pack*. It is **not** an approval to apply Terraform. Mandatory reviews before first `apply` to a non-dev account: Architecture (human T4), Security (human), Database, SRE, Compliance (residency and WORM).
**Date:** 2026-08-20
**Origin:** `SUG-20260820-hl1`
**Picture this document walks:** [`r0-lld.svg`](./r0-lld.svg)
**Platform-team deployment views** (generated — see [`diagrams/`](./diagrams/README.md)): [`topology`](./r0-platform-topology.svg) · [`availability zones`](./r0-platform-az.svg) · [`DR`](./r0-platform-dr.svg) · [`sequence`](./r0-platform-sequence.svg) · [`payment path`](./r0-platform-payment.svg)
**Companion HLD:** [`R0-HLD.md`](./R0-HLD.md) · [`r0-reference-architecture.svg`](./r0-reference-architecture.svg)

---

## 0. How to use this document

Hand this file to the AWS platform team as the R0 **bill of materials**. Every row is one of:

| Tag | Meaning |
|---|---|
| **PROVISION** | Required in R0. Build it in IaC for `dev` / `uat` / `prod` |
| **PROVISION (WS-2)** | Required because R0 cannot authorise a call without workforce identity |
| **PROVISION (thin)** | Required, smallest working shape — do not gold-plate |
| **DO NOT PROVISION** | Present in the *target-state* architecture review and **explicitly out of R0**. Provisioning it now is scope drift |
| **DECIDE WITH** | Architecture has set the constraint; the named persona owns the sizing / SKU / parameter |

This LLD **narrows** [`architecture-review/04-aws-infrastructure-architecture.md`](../platform/architecture-review/04-aws-infrastructure-architecture.md). That review is the North-Star AWS estate (Kafka, ElastiCache-for-everything, per-service clusters, Istio, analytics). R0 does not build that estate. Where the two disagree, **this file and ADR-001 / ADR-008 win**.

Capacity context, so nobody sizes for a problem we do not have: R0 pilot demand is on the order of **~100 journey starts per hour BAU, ~7 per minute at Q4 peak** ([`05-nfr-catalogue.md`](../platform/ws3-platform/05-nfr-catalogue.md) CAP-A*). The platform is correctness-, evidence- and recovery-constrained, not throughput-constrained. Do not scale from CPU.

---

## 1. One-page AWS bill of materials

### 1.1 PROVISION in R0

| # | AWS service | What it is for | SKU / shape (starting point — Shivanshi confirms) |
|---|---|---|---|
| 1 | **AWS Organizations** + 5 accounts | Isolation | `shared-services`, `security`, `dev`, `uat`, `prod` |
| 2 | **Amazon VPC** × environment | Network | 3 AZs, public + private + data subnets. See §2 |
| 3 | **NAT Gateway** × AZ (prod); × 1 (dev) | Egress with **fixed Elastic IPs** | 1SB IP-allowlists the NAT EIP. Non-negotiable |
| 4 | **Amazon Route 53** | Public and private DNS | Hosted zone per env; no latency-based DR routing in R0 |
| 5 | **Amazon CloudFront** | CDN in front of the API and (later) Flutter assets | TLS 1.3; origin = API Gateway. India price class is acceptable; **logs stay in `ap-south-1`** |
| 6 | **AWS WAF** + **AWS Shield Standard** | Edge protection | OWASP managed rule groups + rate limit. Shield Advanced is a cost decision for Shivanshi, not required to start |
| 7 | **Amazon API Gateway** (REST or HTTP API) | The only public reverse proxy for RM/IPR traffic | Mutual TLS not required on this edge in R0 (Flutter + session cookie). Request validation, throttling, no business logic |
| 8 | **Application Load Balancer** (internal) | Reverse proxy **inside** the VPC: Gateway → EKS | Internal scheme. Public ALB is **not** used for services |
| 9 | **Amazon EKS** × environment | All microservices | Kubernetes 1.30+ (platform current). Private API endpoint. See §3 |
| 10 | **Amazon ECR** | Images | Immutable tags; scan on push; replicate to `ap-south-2` for DR images |
| 11 | **Amazon Aurora PostgreSQL** | **One** cluster, schema per bounded context | Multi-AZ writer + reader. See §5. **ADR-008** |
| 12 | **Amazon DynamoDB** | Journey state, quote/proposal jobs, audit event store, BFF session option | PITR on; encryption with CMK |
| 13 | **Amazon S3** + **Object Lock** (Compliance mode) | Raw payloads, policy documents, audit archive | 7-year WORM; CRR to `ap-south-2`; Block Public Access on |
| 14 | **AWS KMS** | CMK hierarchy | Separate CMKs: data, logs, secrets, WORM. India only |
| 15 | **AWS Secrets Manager** | DB creds, 1SB keys, PG keys, IdP secrets | Rotation exercised once (`NFR-SEC-07`) |
| 16 | **IAM Roles for Service Accounts (IRSA)** | Workload identity | No static keys in pods |
| 17 | **Amazon CloudWatch Logs** + **CloudWatch Metrics** | Operational logs/metrics | PII-scrubbed; 90-day operational retention |
| 18 | **AWS X-Ray** *or* **ADOT collector → AMP** | Tracing | One choice; Shivanshi picks. Traces must span BFF → services → Hub → adapter |
| 19 | **Amazon Managed Service for Prometheus** + **Amazon Managed Grafana** | RED metrics, later HPA custom metrics | Thin in R0: four dashboards, not a platform rewrite |
| 20 | **AWS CloudTrail** + **AWS Config** + **GuardDuty** + **Security Hub** | Account audit | In the `security` account. Distinct from application `#16` |
| 21 | **Amazon SNS** + **Amazon SQS** (optional, thin) | Outbox worker wake-up / notification send queue | Not an event bus. Do **not** introduce MSK because SQS exists |
| 22 | **AWS Backup** | Aurora, DynamoDB, EBS (if any) | Meeting `NFR-DR-02` RPO ≤ 5 min for transactional core |
| 23 | **VPC endpoints** | S3, DynamoDB, Secrets Manager, ECR, STS, Logs | Stop Secrets and ECR pulling via NAT |
| 24 | **AWS Certificate Manager** | Public certs for CloudFront / API Gateway; private for internal ALB if used | |

### 1.2 PROVISION for WS-2 (R0 will not boot without these)

| # | Component | Notes |
|---|---|---|
| 25 | **Keycloak** on EKS (or equivalent private IdP) | Behind `identity-provider-adapter-service`. Not exposed to Flutter. Bank AD federation is WS-2 Phase 2 — do not block R0 waiting for AD |
| 26 | **Aurora schema `identity` / `keycloak`** | Keycloak's database. **Do not give Keycloak a PVC as its source of truth** — see §4 |
| 27 | **Session store for the token-hiding BFF** | Prefer **DynamoDB** (already in the BOM) so we do not stand up ElastiCache for one use. If WS-2's accepted SSOT forces Redis, provision **one** small ElastiCache node group in private subnets, used **only** for sessions — never for WS-3 idempotency. Record the choice with Deepali |

### 1.3 DO NOT PROVISION in R0

| AWS service | Why it appears in target-state docs | Why it is out of R0 |
|---|---|---|
| **Amazon MSK (Kafka)** | Target event backbone | Transactional outbox is the R0 async mechanism. Revisit trigger in HLD §3 boundary 9 |
| **Amazon ElastiCache (Redis) for catalogue / idempotency / rate-limit** | Target cache tier | Idempotency lives in the owning service's store; catalogue cache is in-process with TTL; config cache is in-process to `S-21` TTL. A shared Redis is a coupling we have not earned |
| **Second / per-service Aurora clusters** | Misreading of `ARCH-004` | `ADR-008` — one cluster, schema per context |
| **Public RDS / public ALB to a service / public EKS** | Convenience | Standing constraint: only API Gateway is public |
| **Amazon Cognito as the R0 IdP** | Older review text | `ARCH-018` — Keycloak first, adapter-neutral |
| **Istio / AWS App Mesh** | Target mesh | R0: NetworkPolicy + IRSA + in-app timeouts/breakers. Mesh is an S14 conversation |
| **AWS Glue / Athena / Redshift / QuickSight** | `#18` Reporting & MIS | S13 / R1. Pilot funnel is not a warehouse |
| **Insurer callback / webhook API Gateway** | Target inbound provider ingress | R0 polls (`S-11`). Building ingress early creates a public surface with no consumer |
| **Customer-facing CloudFront distribution for DIY** | `#1` Customer BFF | R1 |
| **Render.com as an environment** | Existing `render.yaml` | Dev-preview **only**. Never PII, never a gate artefact (`ADR-001`) |
| **Any resource outside `ap-south-1` except DR replicas in `ap-south-2`** | — | Control C6, `FF-08` |

---

## 2. Network — VPC (required)

The user-facing question "do we need a VPC?" is **yes**. Every environment gets its own VPC. Production does not share a VPC with UAT.

```text
VPC (e.g. 10.{env}.0.0/16)                Region ap-south-1
├── Public subnets        /24 × 3 AZs     NAT Gateways, (no workloads)
├── Private-app subnets   /20 × 3 AZs     EKS nodes (services, BFF, Keycloak)
├── Private-data subnets  /24 × 3 AZs     Aurora, (ElastiCache if WS-2 forces it)
└── Endpoints             in private      S3, DDB, Secrets, ECR, STS, Logs
```

| Control | Requirement |
|---|---|
| Internet-facing | CloudFront + API Gateway only. **No** public NLB/ALB onto EKS |
| EKS API | Private endpoint; `publicAccess = false` in prod |
| Data subnets | No 0.0.0.0/0 route. Aurora and DynamoDB (`via endpoint`) cannot initiate internet |
| Egress | All 1SB and PG and CBS calls leave via **NAT with Elastic IP**. Give that EIP list to 1SB and to AU Bank PG for allowlisting **before** UAT |
| East-west | Kubernetes `NetworkPolicy` default-deny per namespace; allow only documented seams |
| DNS | Private hosted zone for `*.svc.cluster.local` plus `internal.{env}.insurance.aubank.local` for the internal ALB |
| Flow logs | VPC flow logs to CloudWatch, `RET-OPERATIONAL` |

**CBS and Bank AD** are bank-internal. They will require either (a) AWS Direct Connect / VPN into a bank transit gateway, or (b) a bank-hosted reverse proxy that the platform calls. **Architecture does not invent the bank network.** S09 entry criterion includes "cloud account structure approved"; the connectivity pattern is a joint Shivanshi + bank network decision. Until it exists, Customer `#4` and WS-2 Phase 2 federation cannot be proven against real CBS/AD — stubs are acceptable in `dev` only.

### 2.1 Availability-zone placement — which resource sits where

The sources say *"3 AZs"* and *"min 2 AZ"* and stop there, which is not enough to provision from.
This table is the placement contract. It states **architecture's constraint**, not the SKU: how
many AZs a resource must span and why the number is what it is. Instance classes, node counts
beyond the minimum and Aurora Global-versus-restore stay with Shivanshi and Aarti (§14).

**AZ names are logical.** `AZ-A / AZ-B / AZ-C` here mean *three distinct AZ IDs in `ap-south-1`*.
Do not pin `ap-south-1a` in IaC: an AZ **name** maps to a different physical **AZ ID** in each
account, so a name pinned across five accounts silently co-locates resources that were meant to be
separated. Pin AZ **IDs** (`aps1-az1…`), or let the module take the first three from
`aws_availability_zones` and record the resulting ID map per account.

| Resource | AZ-A | AZ-B | AZ-C | Placement rule | Why this number |
|---|---|---|---|---|---|
| Public subnet | ✅ | ✅ | ✅ | One /24 per AZ | An ALB or NAT cannot exist in an AZ with no subnet. Cheap to create, expensive to retrofit |
| Private-app subnet | ✅ | ✅ | ✅ | One /20 per AZ | EKS nodes; the /20 is for pod IPs (VPC CNI), not node count |
| Private-data subnet | ✅ | ✅ | ✅ | One /24 per AZ | Aurora needs a subnet group spanning ≥ 2; the third keeps failover choice open |
| **NAT Gateway + EIP** | ✅ | ✅ **prod/uat** | ⬜ *(prod only, cost call)* | **prod: per AZ. dev: one, single AZ** | A single NAT is an AZ-wide egress SPOF, and egress is the 1SB path. Every added NAT adds an EIP that 1SB and the PG must allowlist — **decide the count before publishing the EIP list** (§8) |
| Internet Gateway | — regional — | | | One per VPC | Not AZ-bound |
| **Internal ALB** | ✅ | ✅ | ✅ | Subnets in all three; ALB places a node per enabled AZ | The only in-VPC reverse proxy (§3). Losing it loses every RM session |
| **EKS control plane** | — AWS-managed, multi-AZ — | | | Private endpoint, `publicAccess = false` in prod | AWS spreads it; we do not choose |
| **EKS managed node group** (sale path) | ✅ | ✅ | ⬜ *(prod: yes)* | **UAT: ≥ 3 nodes across ≥ 2 AZs. prod: across 3** | §4.2. Two AZs is the floor at which a `PodDisruptionBudget` of `minAvailable: 1` can still drain a node. Three removes the "lose an AZ, lose half the capacity" arithmetic |
| Sale-path pods | ✅ | ✅ | ⬜ | `minReplicas: 2`, `topologySpreadConstraints` across `topology.kubernetes.io/zone`, `PDB minAvailable: 1` | Two pods on one node in one AZ satisfies `min 2` and survives nothing. The spread constraint is the control, not the replica count |
| Keycloak | ✅ | ✅ | ⬜ | ≥ 2 replicas, ≥ 2 AZs, no PVC (§4.1) | Identity down is a total outage — the PDP fails closed by design (`S-02`) |
| **Aurora writer** | ✅ | ⬜ | ⬜ | One AZ at a time, by definition | A writer is single-AZ; Multi-AZ means the *failover target* is elsewhere |
| **Aurora reader** | ⬜ | ✅ | ⬜ | **Different AZ from the writer** — assert it in IaC | A reader in the writer's AZ is a read-scaling replica, not an availability one. This is the single most common Multi-AZ misconfiguration |
| DynamoDB · S3 · KMS · Secrets Manager · ECR | — regional, AWS multi-AZ — | | | Nothing to place | Do not build AZ logic around them |
| Interface VPC endpoints (Secrets Manager, ECR, STS, Logs) | ✅ | ✅ | ✅ | One ENI per AZ | An endpoint present in two of three AZs makes the third AZ's pods fail to pull images while looking healthy |
| Gateway VPC endpoints (S3, DynamoDB) | — route-table, not AZ — | | | Attach to every private route table | Missing on one table sends that AZ's S3 traffic through NAT, quietly |
| ElastiCache *(only if WS-2 forces it — §1.2)* | ✅ | ✅ | ⬜ | Replication group, 2 AZs, automatic failover **on** | A single-node session store makes an AZ loss a mass logout |

**Two AZs or three?** Three for subnets, endpoints and the internal ALB — they cost nothing per AZ
and cannot be added later without renumbering. Two is acceptable for *paid* capacity (nodes, NAT,
Aurora reader) in `uat`, three in `prod`. `dev` is single-AZ deliberately: it is synthetic data and
an AZ failure there is not an incident.

**What AZ placement does not buy.** An AZ is not a region. Losing `ap-south-1` entirely is §11, and
no amount of AZ spreading answers it.

---

## 3. Reverse proxy — external and internal (required)

R0 uses a **two-hop reverse proxy**. There is no extra Nginx/Envoy sidecar estate.

```text
RM / IPR device
    │  TLS 1.3
    ▼
CloudFront  ──►  AWS WAF
    │
    ▼
API Gateway          ← THE external reverse proxy
    │  private integration / VPC link
    ▼
Internal ALB         ← THE internal reverse proxy
    │  target: EKS (AWS Load Balancer Controller)
    ▼
#2 RM Workspace BFF  (and, same listener host-route, WS-2 workforce-access-bff
                       if they remain separate deployables — see note)
    │  cluster-private
    ▼
Domain services (never published)
```

| Hop | Terminates TLS? | AuthN? | AuthZ? | Business logic? |
|---|---|---|---|---|
| CloudFront / WAF | Yes (ACM) | No | No | No — bot/rate/OWASP only |
| API Gateway | Yes | Optional API key **not** used as auth | No | Request size, schema, throttle |
| Internal ALB | Yes (internal cert) | No | No | Path routing to BFF |
| BFF | Re-encrypts outbound | **Session** | Calls PDP (`S-02`) | Aggregation only |
| Domain service | mTLS-or-IRSA | Service identity | Re-checks PDP on regulated actions | Yes |

**Note on two BFFs.** WS-2 already specifies `workforce-access-bff` (token-hiding). WS-3 specifies `#2` RM Workspace BFF (journey aggregation). R0 may deploy them as **one process** or **two**. That is Amit's packaging choice inside an approved boundary (`A1`). The edge contract does not change: Flutter talks to one public hostname and never receives OAuth tokens.

**Ingress controller:** AWS Load Balancer Controller. One internal ALB, host/path rules, not an ALB per microservice.

**Do we need an additional "external proxy" product (Kong, Nginx Plus, F5)?** No for R0. API Gateway + CloudFront + WAF is the external reverse proxy. Adding a fourth hop adds a PCI/PII surface without an R0 consumer.

**Customer payment traffic does not enter this chain.** It goes device → AU Bank PG. PG callbacks enter via a **separate** API Gateway route, IP-allowlisted to the PG, signature-verified in `#12`, never on the RM session path (TB-6).

---

## 4. Kubernetes — PVC, nodes, namespaces

### 4.1 Persistent Volume Claims — **not required for R0 business services**

Every WS-3 service is **stateless at the pod level** (`ARCH-002`, HLD boundary 8). State lives in Aurora, DynamoDB or S3.

| Workload | PVC? | Why |
|---|---|---|
| All WS-3 domain services, Hub, `#2` BFF, `#17`, outbox workers | **No** | `emptyDir` only if a crash-only temp file is needed; `readOnlyRootFilesystem: true` |
| `1sb-integration-service` | **No** | Existing design; job state in `bank-persistence-service` / Aurora schema |
| Keycloak | **No PVC as database** | Keycloak JDBC → Aurora schema `keycloak`. A PVC here becomes an unreplicated source of identity truth |
| `aws-load-balancer-controller`, `external-dns`, ADOT, Fluent Bit | **No** | Use official Helm charts' default (hostPath/emptyDir as designed) |
| Prometheus / Grafana self-hosted | **Do not install** | Use AMP + AMG (BOM #19) so we do not own Prometheus PVCs |
| Future document-generation that writes local PDFs | Not in R0 | Would be S3, not PVC |

**Pod security (Deepali, S09-E07-S03):** non-root, no privileged, no hostNetwork, read-only root FS, drop all capabilities, seccomp RuntimeDefault. Enforce with Kyverno or Gatekeeper in `shared-services` → each cluster.

### 4.2 Cluster shape

| Item | R0 requirement |
|---|---|
| Clusters | **One EKS per environment** (`dev`, `uat`, `prod`). Not one cluster with namespace-as-env |
| Node groups | Managed on-demand for sale-path (BFF, Journey, Identity, Quotation, Proposal, Payment, Policy, Hub, Adapter). Starting size: 3 nodes × 2 AZ minimum in UAT/prod so PDB can drain |
| Karpenter | **PROVISION (thin)** in UAT/prod; optional in dev. Do not enable Spot on the sale path in R0 |
| HPA | CPU-based, min 2 / max 6 on sale-path deployments. Custom-metric HPA is S12. **NFR-THR-06:** `Σ(pods × pool size) ≤ 60%` of Aurora `max_connections` at max replica count — encode this as a comment in the HPA max and as an assertion in IaC review |
| KEDA | **DO NOT** until there is a broker. Outbox workers: `Deployment` with 2 replicas and a poll interval |
| PDB | `minAvailable: 1` on every sale-path Deployment |
| Add-ons | VPC CNI, CoreDNS, kube-proxy, EBS CSI (**installed but unused** by business apps — needed for some add-ons), AWS LB Controller, ExternalDNS, Secrets Store CSI (preferred over env-injected secrets) |

EBS CSI without a PVC consumer is not waste; it keeps the cluster able to run a future add-on without a change window.

### 4.3 Namespaces (aligns with HLD boundaries, not 1:1 with services)

```text
ns: edge              #2 RM BFF  (+ workforce-access-bff if separate)
ns: identity          WS-2 adapter, PDP, Keycloak
ns: shared-platform   #4 #5 #6 #7 #8 #9 #12 #13 #16 #17 #19
ns: life-cell         #10 Quotation  #11 Proposal     ← first physical split seam
ns: integration       #14 Hub  #15 1sb-adapter
ns: jobs              outbox pollers, reconciliation CronJob
ns: platform          kube-system-adjacent controllers only
```

NetworkPolicy: `life-cell` may call `integration` and `shared-platform`; it may **not** be called by `edge` except via `#9`. `identity` is reachable from all app namespaces on the PDP port only.

---

## 5. Database — Aurora, DynamoDB, S3

### 5.1 Aurora PostgreSQL — one cluster (`ADR-008`)

| Parameter | R0 |
|---|---|
| Engine | Aurora PostgreSQL, current extended-support-safe version Aarti selects |
| Topology | 1 writer + 1 reader, Multi-AZ |
| Name | `r0-platform-{env}` |
| Database | `insurance` |
| Schemas (separate owner role, **no** `GRANT SELECT` across) | `cfg` (#19), `opportunity` (#5), `customer` (#4), `consent` (#6), `suitability` (#7), `catalogue` (#8), `journey` (#9) *if not Dynamo-only*, `quotation` (#10) *relational side*, `proposal` (#11), `payment` (#12), `policy` (#13), `hub` (#14), `onesb` (existing job store — today's `bank-persistence-service`), `audit` (#16) *if relational mirror*, `identity`, `keycloak`, `notif` (#17) |
| Auth | IAM DB auth **or** Secrets Manager password rotated. Per-schema user. App role: DML on own schema only. Migrator role: DDL, used only by the migration Job |
| Encryption | Storage encrypted with data CMK |
| Backup | Aurora automated, 7-day PITR window for operational restore; long-term via AWS Backup to `ap-south-2` |
| `lob` | `NOT NULL` check constraint on every business table from the first migration (`FF-19`) |
| Existing `bank-persistence-service` | Keep. Point it at schemas `onesb` + `audit` ingestion. **Do not** route Opportunity/Consent/Payment through it |

**First physical split (not R0):** move `quotation` + `proposal` (the Life cell) to a second cluster when load or blast-radius evidence says so. No application redesign — that is the point of `ADR-008`.

Aarti owns indexes, connection limits, parameter groups, and whether `journey` is Aurora or DynamoDB. Architecture's constraint is ownership, not engine.

### 5.2 DynamoDB tables (PROVISION)

| Table | PK | Purpose | PITR | TTL |
|---|---|---|---|---|
| `journey-state-{env}` | `journeyId` | Stage + externalRefs + party snapshot | Yes | No |
| `integration-jobs-{env}` | `jobId` | Quote/proposal poll records (mirrors today's job entity) | Yes | 30 d after terminal |
| `idempotency-{env}` | `owner#key` | Optional shared *implementation* of INV-IDM-01 if Aarti prefers DDB over Postgres. Still **per-service IAM**, not a shared access pattern | Yes | 24 h |
| `sessions-{env}` | `sessionId` | Token-hiding BFF session vault (preferred over Redis) | Yes | session TTL |
| `audit-events-{env}` | `journeyId` + `sequence_no` | Append-only event store | Yes | No (archive to S3) |

IAM: each service role can `PutItem`/`GetItem`/`Query` **only its** table. Audit role has **no `UpdateItem` / `DeleteItem`**. Verify with a failing IaC test (`FF-10`).

### 5.3 S3 buckets (PROVISION)

| Bucket | Object Lock | Replication | Who writes |
|---|---|---|---|
| `aubank-ins-raw-{env}` | Compliance, 7 years | → `ap-south-2` | Adapter (provider payloads) |
| `aubank-ins-docs-{env}` | Compliance, 7 years | → `ap-south-2` | Policy `#13` |
| `aubank-ins-audit-archive-{env}` | Compliance, 7 years | → `ap-south-2` | Audit exporter |
| `aubank-ins-logs-archive-{env}` | Governance (not Compliance) | optional | CloudWatch export — **no PII** |
| `aubank-ins-tfstate-{account}` | Versioned | — | Terraform; in `shared-services` |

Block Public Access, TLS-only bucket policy, CMK, access logged. A delete-attempt test on a locked object must fail (`NFR-DAT-02`).

---

## 6. Caching — what R0 actually needs

| Cache | Technology | Where | TTL | On miss |
|---|---|---|---|---|
| Configuration resolution (`S-21`) | **In-process** Caffeine (or equivalent) per pod | Each service | Resolution TTL from `#19` | **Fail closed** if store unreachable and cache expired |
| Product catalogue reads | **In-process** | `#8` and callers | Short (minutes) | Read Aurora; breaker on `#8` |
| CBS identity snapshot | **In-process / row in `customer` schema** | `#4` | Freshness window | **No unbounded stale fallback** (`S-05`) |
| PDP decision | **Not cached across pods in R0** | — | — | 300 ms fail closed. A stale allow is worse than a deny |
| Idempotency | **Owning service store** (Aurora or DynamoDB), not Redis | — | 24 h | — |
| Flutter / CDN | CloudFront | Edge | Cache-Control from BFF; **no** caching of authenticated JSON | — |

**ElastiCache is not in the R0 BOM** except the WS-2 session exception in §1.2. If someone proposes Redis "because we will need it for Health", park it. Health is R3.

---

## 7. End-to-end component view (what runs where)

```text
Z0 Internet
  Flutter RM app          (mobile / tablet — RM device)
  IPR browser             (same hostname, same BFF)
  Customer device         (OTP SMS / PG hosted page) ──► AU Bank PG   [not our VPC]

Z1 Edge (public)
  Route 53 → CloudFront → WAF → API Gateway
  PG-callback API Gateway route (IP allowlist)

Z2 Application (private-app subnets, EKS)
  edge:        rm-workspace-bff (#2)  [stateless]
  identity:    identity-provider-adapter, identity-authorization (PDP), keycloak
  shared:      opportunity (#5), customer (#4), consent (#6), suitability (#7),
               catalogue (#8), journey (#9), payment (#12), policy (#13),
               audit (#16), notification (#17), configuration (#19)
  life-cell:   quotation (#10), proposal (#11)
  integration: integration-hub (#14), 1sb-integration-service (#15)
  jobs:        outbox-publisher, payment-reconcile (CronJob), issuance-recheck

Z4 Data (private-data subnets / AWS managed)
  Aurora cluster r0-platform          (schemas: §5.1)
  DynamoDB tables                     (§5.2)
  S3 WORM buckets                     (§5.3)
  KMS + Secrets Manager
  (optional) ElastiCache session-only

Z5 Bank / provider (outside)
  Bank AD          ← WS-2 Phase 2
  CBS              ← #4 via bank connectivity
  AU Bank PG       ← #12 session + callback + settlement file
  1SilverBullet    ← #15 via NAT EIP, mTLS
```

Existing repo services that **map onto** this:

| Today in git | R0 role |
|---|---|
| `services/1sb-integration-service` | `#15` |
| `services/bank-persistence-service` | Job store + audit ingestion **only** |
| `services/workforce-access-bff` (WS-2) | Token-hiding session |
| `services/identity-provider-adapter-service` | Keycloak isolation |
| `services/identity-authorization-service` | PDP |
| *(not yet in git)* | All other `#n` services and the Flutter app |

---

## 8. External connectivity checklist (platform team + bank network)

| Dependency | Direction | Protocol | Allowlist | Owner to confirm |
|---|---|---|---|---|
| 1SB APIs | Egress | HTTPS mTLS | **Our NAT EIPs** must be on 1SB's list; 1SB IPs on our egress SG | WS-1 / Shivanshi |
| AU Bank PG session | Egress | HTTPS | PG endpoints | Payments + Shivanshi |
| AU Bank PG callback | Ingress | HTTPS | **PG source IPs only** on the callback Gateway | Deepali + Payments |
| AU Bank PG settlement | Ingress or S3 drop | File | Separate from the API path | Aarti + Finance |
| CBS / CIF | Egress | Bank standard (often HTTPS or MQ) | Direct Connect / VPN | Bank network + `#4` |
| Bank AD | Egress from Keycloak | OIDC/SAML | Phase 2; R0 can run Keycloak-local users in `dev` | WS-2 |
| SMS/email gateway | Egress | HTTPS | Notification `#17` | Bank comms |

No inbound from 1SB in R0 (we poll). Do not open a public webhook "just in case".

---

## 9. Environments, CI/CD, secrets

| Env | Account | Data | Purpose |
|---|---|---|---|
| `dev` | `dev` | Synthetic only | Engineers. Render.com may remain as a **no-PII** preview alongside, never instead |
| `uat` | `uat` | Masked / synthetic; **no production CIF dumps** | Bank caller exercises quote + proposal (WS-1 gate 4.3) |
| `prod` | `prod` | Real ETB | Pilot branches only |

Promotion: **image built once** in `shared-services`, promoted by digest. Never rebuilt per env (`S09-E02-S02`).

CI: GitHub Actions (already in-repo) → ECR. CD: **Argo CD** in each cluster (recommended) or EKS-native. Shivanshi chooses; Architecture requires an audited, reversible deploy and a **UAT rollback drill** (`NFR-DR-05`).

Secrets: Secrets Manager → Secrets Store CSI → tmpfs. Rotation runbook exercised once. Emergency revocation path exists (`S09-E04-S05`).

---

## 10. Observability and audit (two pipes, never one)

| Pipe | Contents | Store | Retention |
|---|---|---|---|
| **Operational** | RED metrics, traces, application logs (PII-masked) | AMP / X-Ray / CloudWatch | `RET-OPERATIONAL` (90 days) then dispose with an audit record |
| **Regulatory audit** | Domain `AuditEvent`s from the outbox (`S-17`) | DynamoDB + S3 Object Lock | `RET-7Y-IMMUTABLE` |
| **Cloud account audit** | CloudTrail, Config, GuardDuty | `security` account | Per bank policy; not the application audit store |

A journey cannot reach `SOLD` until the regulatory pipe has acknowledged the four required events (`INV-JRN-05`). Operational log loss is an SRE incident; audit-pipe loss is a **compliance** incident.

---

## 11. DR and backup (R0 = warm standby, not active-active)

| Concern | R0 design | NFR |
|---|---|---|
| Region | Primary `ap-south-1`; DR `ap-south-2` | C6 |
| Compute | EKS in DR can be scaled from zero; images already in ECR replica | RTO ≤ 1 h |
| Aurora | Provisioned replica or restore-from-backup in `ap-south-2` — **Aarti chooses** between Aurora Global (faster, costlier) and backup-restore (cheaper, RTO risk). Constraint: RTO ≤ 1 h must be **demonstrated** (`S09-G7`) | `NFR-DR-01/02` |
| DynamoDB | PITR + optional global tables. Global tables are a cost decision; PITR is mandatory | RPO ≤ 5 min |
| S3 WORM | CRR mandatory; RPO **0** for audit/raw | `NFR-DR-03` |
| Secrets / KMS | Replica keys in `ap-south-2` or defined restore procedure | |
| Money path | Reconciliation, **not** database restore, is how payment state is recovered | HLD F-07/F-08 |

Active-active is **not** R0.

### 11.1 DR bill of materials — what actually exists in `ap-south-2`

§11 states the posture. This is the resource list, because "warm standby" is not something a
platform team can provision from. **Everything below is `ap-south-2`. Nothing else is.**

| # | DR resource | Running in R0? | Shape | Meets |
|---|---|---|---|---|
| D1 | **VPC + 3 subnet tiers + route tables** | **Yes — empty** | Same Terraform module, DR parameter set. No NAT until failover (nothing egresses from an idle region) | Precondition for D6/D7 |
| D2 | **ECR replication rule** | **Yes** | `ap-south-1 → ap-south-2`, all repositories. Already in BOM #10 | Without images, RTO is a build, not a restore |
| D3 | **S3 replica buckets** — `raw`, `docs`, `audit-archive` | **Yes** | CRR from the primaries, Object Lock Compliance 7 y **on the replica too**, replication metrics + a replication-lag alarm | `NFR-DR-03` **RPO 0** — non-negotiable, this is regulatory evidence |
| D4 | **Aurora DR** | **Yes — shape is Aarti's call** | **(a)** Aurora Global Database secondary (seconds of lag, runs and costs continuously) **or** **(b)** AWS Backup cross-region copy + restore (cheap, restore time is the risk). Architecture's constraint is not the option — it is that `NFR-DR-01` RTO ≤ 1 h is **measured**, not asserted | `NFR-DR-01/02` |
| D5 | **DynamoDB** | **PITR yes; global tables optional** | PITR is **mandatory** and is not a DR feature — it is same-region. Cross-region for `journey-state` / `audit-events` needs global tables, and that is a cost decision (§14) | `NFR-DR-02` |
| D6 | **KMS replica keys** | **Yes** | Replica CMK per class. Encrypted data replicated without its key is not recoverable — this is the most common silent DR failure | Precondition for D3/D4 |
| D7 | **Secrets Manager replica secrets** | **Yes** | Replica of DB, 1SB, PG and IdP secrets. A restored Aurora with no credential is not a restored service | Precondition for D8 |
| D8 | **EKS cluster** | **No — created at failover, or scaled from zero** | Node groups at desired-count `0` if the cluster exists. This is what "warm" means: images and data are ready, compute is not paid for | `NFR-DR-01` RTO ≤ 1 h |
| D9 | **Route 53 failover** | **No — manual in R0** | BOM #4: no latency-based or health-check DR routing. Failover is a deliberate, recorded human action, not an automatic flip | R0 posture |
| D10 | **API Gateway + CloudFront origin re-point** | **No — part of the runbook** | The edge is re-pointed at the DR internal ALB during failover. Document it as a step, not as automation | R0 posture |
| D11 | **DR runbook + measured exercise** | **Yes — the artefact is the deliverable** | Declaration → restore → verify → serve, wall-clock timed. `S09-G7` accepts the *record*, not the design | `NFR-DR-04`, `S09-G7` |
| D12 | **Rollback drill in UAT** | **Yes** | Not cross-region, but the same family of proof: a deliberately broken release rolled back, data intact, timed | `NFR-DR-05`, `S09-G4` |

**Explicitly NOT DR in R0:** a second running EKS cluster · active-active traffic · cross-region
read routing · MSK MirrorMaker (there is no MSK) · a DR ElastiCache · any resource outside India
(`FF-08`, control `C6`).

**The money path is not restored, it is reconciled.** Restoring the `payment` schema tells you what
the platform believed; only reconciliation against the AU Bank PG tells you what actually happened
(§11 row 8, HLD `F-07`/`F-08`). A DR exercise that "passes" without running reconciliation has not
tested the payment path.

**Sequencing note.** D1, D2, D3, D6 and D7 are provisioned in the *same* S09 change as their
`ap-south-1` primaries — replication configured after the fact means the window before it was
configured has no evidence copy, and for `audit-archive` that window is a compliance gap, not a
backlog item.

---

## 12. Per-service AWS resource matrix

Use this as the Terraform `for_each` checklist. Min pods = 2 in UAT/prod.

| Deployable | NS | Aurora schema | DDB | S3 | Outbound |
|---|---|---|---|---|---|
| rm-workspace-bff | edge | — | `sessions` | — | PDP, all domain APIs |
| identity-provider-adapter | identity | — | — | — | Keycloak, (later AD) |
| identity-authorization | identity | `identity` | — | — | — |
| keycloak | identity | `keycloak` | — | — | AD (phase 2) |
| configuration | shared | `cfg` | — | — | — |
| opportunity | shared | `opportunity` | — | — | PDP |
| customer | shared | `customer` | — | — | **CBS** |
| consent | shared | `consent` | — | — | Notification (OTP) |
| suitability | shared | `suitability` | — | — | Catalogue |
| catalogue | shared | `catalogue` | — | — | — |
| journey-orchestration | shared | optional | `journey-state` | — | all domain refs |
| quotation | life-cell | `quotation` | `integration-jobs` | — | Hub |
| proposal | life-cell | `proposal` | `integration-jobs` | — | Hub |
| payment | shared | `payment` | — | — | **AU Bank PG** |
| policy | shared | `policy` | — | `docs` | Hub |
| integration-hub | integration | `hub` | — | — | Adapter only |
| 1sb-integration-service | integration | `onesb` via persistence svc | jobs | `raw` | **1SB via NAT EIP** |
| bank-persistence-service | integration | `onesb`, audit ingest | — | — | Aurora only |
| audit | shared | optional | `audit-events` | `audit-archive` | — |
| notification | shared | `notif` | — | — | SMS/email gateway |
| outbox-worker | jobs | reads each service outbox | — | — | Audit, Notification |

IAM: one IRSA role per deployable. No wildcard production policies (`FF-09`).

### 12.1 When — the provisioning sequence

The BOM says *what*. This says *in what order, who owns it, and what breaks if it is late*. Each
band is gated on the one above it; within a band, order is free. Bands **P0–P3** are the S09
critical path and no business service starts before them
([`03-solution-architecture-r0.md §3`](../platform/ws3-platform/03-solution-architecture-r0.md#3-r0-build-order--closing-s07-e01-s05): W0 is
"no services — pipeline, IaC, environments").

| Band | Provision | S09 story | Owner | First consumer | If it is late |
|---|---|---|---|---|---|
| **P0** Guardrails | Organizations + 5 accounts · SCP region-pin to India · Terraform remote state + locking · `security` account (CloudTrail, Config, GuardDuty, Security Hub) · **KMS CMK hierarchy** · policy-as-code in the pipeline | `E01-S01/S02/S06/S07` · `E04-S03` | Shivanshi + Deepali | Everything | Every resource built before the region SCP has to be re-verified by hand for `S09-G9` residency attestation |
| **P1** Network | VPC × 3 envs · public / private-app / private-data × 3 AZs (§2.1) · **NAT Gateway + Elastic IPs** · security groups · **VPC endpoints** · Route 53 private zone · ACM certs · flow logs | `E01-S03` | Shivanshi | P2 | **Longest external lead time on the programme.** The NAT EIP list must reach 1SB and AU Bank PG for allowlisting *before* UAT (§8). Publishing it late blocks W2 quotes and W3 payments regardless of code readiness |
| **P2** Compute | EKS × 3 envs, private endpoint · managed node groups (§2.1) · add-ons (VPC CNI, CoreDNS, kube-proxy, EBS CSI, AWS LB Controller, ExternalDNS, Secrets Store CSI) · **Kyverno/Gatekeeper admission** · NetworkPolicy default-deny · Karpenter (thin, uat/prod) | `E01-S04` · `E07-S01/S03` | Shivanshi + Deepali | P4, P5 | Admission policy retro-fitted onto running workloads is a migration, not a control |
| **P3** Data | **One** Aurora cluster + schemas + per-schema roles · DynamoDB tables + PITR · S3 buckets + **Object Lock** + Block Public Access · AWS Backup plans · **`ap-south-2` replication (D1–D3, D6, D7)** | `E01-S05` · `E06-S01/S03/S05` | Aarti + Shivanshi | W0b | Object Lock **cannot be applied retroactively** to objects already written. Any evidence written before the bucket is locked is outside the 7-year WORM claim |
| **P4** Edge & proxy | **Internal ALB** (the in-VPC reverse proxy) · **API Gateway** (the only public one) · CloudFront + WAF + Shield Standard · Route 53 public zone · **separate PG-callback route, IP-allowlisted** | `E07-S05` | Shivanshi + Deepali | W3 (callback) then W4 (RM traffic) | The PG-callback route is needed at **W3**, earlier than the RM edge at W4. Treating "the edge" as one deliverable delays the money path by a wave |
| **P5** Identity (WS-2) | Keycloak on EKS + Aurora `keycloak`/`identity` schemas · Secrets Manager + rotation · **IRSA role per deployable** · Secrets Store CSI → tmpfs | `E04-S01/S04/S06` | Deepali + WS-2 | W0b | The PDP fails closed by design (`S-02`). No identity means no service can authorise anything — this is not a "later" item |
| **P6** Observability | CloudWatch Logs/Metrics with PII masking · AMP + AMG · X-Ray *or* ADOT · **audit pipe separated from the operational pipe** · baseline dashboards + alert routing | `E05-S01…S06` | Shivanshi | W1 | Debugging the first end-to-end journey without correlated traces is where schedules are actually lost |
| **P7** Delivery | ECR + immutable tags + scan-on-push · GitHub Actions → ECR · **Argo CD** (or chosen GitOps) · promote-by-digest · migration job in the deploy path | `E02-S01…S06` · `E03-S01…S06` | Shivanshi + Amit | W0b | Rebuilding per environment breaks `S09-E02-S02` and makes every UAT result unattributable |
| **P8** Proof | **Restore executed and timed** · **rollback drill in UAT** · secret rotation exercised once · deletion-refusal test on a locked object · residency enumeration | `E06-S04` · `E03-S03` · `E04-S04` | Shivanshi + Aarti + Shailja | `GATE-S09` | `S09-G4`, `S09-G7`, `S09-G8`, `S09-G9` accept **records**, not designs. Nothing here can be produced in the week the gate is reviewed |

**What each build wave needs to already exist**, so the platform request can be sequenced against
the service backlog rather than delivered as one lump:

| Wave | Services | Platform preconditions |
|---|---|---|
| **W0b** | `#19` Configuration | P0 · P1 · P2 · P3 · P5 · P7 |
| **W1** | `#5` `#9` `#14` `#4` `#8` | + egress path to **CBS** decided (Direct Connect / VPN / bank proxy — §14) · P6 |
| **W2** | `#6` `#7` `#10` | + **NAT EIPs allowlisted by 1SB** · S3 `raw` bucket locked |
| **W3** | `#11` `#12` `#13` `#16` | + **PG-callback API Gateway route** · PG settlement drop path · S3 `docs` + `audit-archive` locked · DR replication live (D3) |
| **W4** | `#2` BFF · Flutter · `#17` | + CloudFront + WAF + public API Gateway · internal ALB · SMS/email gateway egress |

**The two items with an external lead time are P1's EIP publication and the CBS connectivity
decision.** Both depend on parties outside this programme. Start them first; they are the only
things on this list that cannot be accelerated by working harder.

---

## 13. Copy-paste requirement statement for the AWS platform team

The following block is intended to be pasted into an infrastructure request / CR.

```text
R0 AWS PLATFORM REQUEST — AU Bank Insurance Distribution Platform
Region: ap-south-1 (Mumbai). DR: ap-south-2 (Hyderabad) replicas only.
Accounts: shared-services, security, dev, uat, prod.

NETWORK
- 1 VPC per env, 3 AZs: public / private-app / private-data
- NAT Gateway with Elastic IPs (prod: per AZ). EIP list to be published for 1SB + PG allowlists
- VPC endpoints: S3, DynamoDB, Secrets Manager, ECR, STS, CloudWatch Logs
- No public load balancer onto compute. No public database.

AVAILABILITY ZONES  (full table: LLD §2.1)
- Subnets, interface VPC endpoints and the internal ALB: all 3 AZs, every environment
- Paid capacity (EKS nodes, NAT, Aurora reader): >= 2 AZs in uat, 3 in prod, 1 in dev
- Aurora reader MUST be in a different AZ from the writer - assert this in IaC
- Sale-path pods: minReplicas 2 + topologySpreadConstraints over topology.kubernetes.io/zone + PDB minAvailable 1
- Pin AZ IDs (aps1-azN), NOT AZ names - a name maps to a different physical AZ per account

EDGE (external reverse proxy)
- Route 53 + CloudFront + AWS WAF (OWASP + rate limit) + API Gateway
- Internal ALB (AWS LB Controller) as the only in-VPC reverse proxy
- Separate API Gateway route for AU Bank PG callbacks, IP-allowlisted
- Do NOT provision Kong/Nginx Plus/F5, Istio, or a second public ALB

COMPUTE
- 1 private EKS cluster per env; sale-path min 2 pods, 2 AZs, PDBs
- Namespaces: edge, identity, shared-platform, life-cell, integration, jobs
- Stateless workloads: NO PersistentVolumeClaims for business services
- Keycloak uses Aurora, not a PVC

DATA
- ONE Aurora PostgreSQL cluster, Multi-AZ, schema-per-bounded-context, no cross-schema grants
- DynamoDB: journey-state, integration-jobs, sessions, audit-events (PITR, CMK)
- S3 buckets with Object Lock Compliance 7y + CRR to ap-south-2 for raw, docs, audit-archive
- KMS CMK hierarchy + Secrets Manager (rotation to be exercised)
- Do NOT provision MSK/Kafka
- Do NOT provision ElastiCache unless WS-2 session store cannot use DynamoDB
- Do NOT provision per-service RDS instances
- Do NOT provision Glue/Athena/Redshift/QuickSight

IDENTITY
- Keycloak private + existing WS-2 adapter/PDP/BFF pattern
- IRSA everywhere; no static keys in pods

OPS
- CloudWatch + AMP/AMG + tracing (X-Ray or ADOT)
- CloudTrail/Config/GuardDuty/Security Hub in security account
- AWS Backup; a restore MUST be executed and timed in UAT before prod
- Terraform, remote encrypted state, no console-created prod resources
- Policy-as-code: fail any resource outside India regions; fail public/unencrypted stores; fail wildcard prod IAM

DISASTER RECOVERY - ap-south-2, warm standby  (full table: LLD §11.1)
- Provision NOW, in the same change as the ap-south-1 primaries:
  empty VPC + subnets, ECR replication, S3 replica buckets with Object Lock 7y,
  KMS replica keys, Secrets Manager replica secrets
- Aurora DR: Aurora Global secondary OR AWS Backup cross-region copy - Aarti chooses.
  The constraint is that RTO <= 1h is MEASURED, not asserted
- DynamoDB PITR mandatory; cross-region global tables are a cost decision
- EKS in DR: NOT running. Created at failover or node groups at desired-count 0
- Route 53 failover and edge origin re-point are MANUAL runbook steps in R0
- Deliverable is the timed DR exercise record (S09-G7), not the design
- Do NOT provision: second running cluster, active-active, cross-region read routing,
  DR ElastiCache, any resource outside India

WHEN - provisioning sequence  (full table: LLD §12.1)
P0 guardrails (accounts, region SCP, TF state, security account, KMS hierarchy)
P1 network (VPC, 3 AZs, NAT + EIPs, endpoints, ACM)   <-- START FIRST, external lead time
P2 compute (EKS, node groups, add-ons, admission policy, NetworkPolicy)
P3 data (Aurora, DynamoDB, S3 + Object Lock, AWS Backup, ap-south-2 replication)
P4 edge (internal ALB, API Gateway, CloudFront, WAF; PG-callback route needed at W3, before W4)
P5 identity (Keycloak, Secrets Manager, IRSA, Secrets Store CSI)
P6 observability (CloudWatch, AMP/AMG, tracing, separated audit pipe)
P7 delivery (ECR, GitOps, promote-by-digest, migration job)
P8 proof (restore timed, rollback drill, rotation exercised, Object Lock deletion refused, residency enumerated)

Two items have an external lead time and cannot be accelerated internally:
  1. publishing the NAT Elastic IP list to 1SB and AU Bank PG for allowlisting
  2. the CBS / Bank AD connectivity decision (Direct Connect vs VPN vs bank-hosted proxy)

OUT OF SCOPE FOR THIS REQUEST
Customer DIY stack, insurer webhook ingress, service mesh, Kafka, shared Redis
idempotency cache, admin UI, reporting warehouse, Render.com as a data path.
```

---

## 14. What this LLD does not decide (and must not be inferred)

| Item | Owner | When |
|---|---|---|
| Exact instance classes, Aurora `max_connections`, Karpenter limits | Shivanshi + Aarti | S09 design review, validated S12 |
| Direct Connect vs VPN vs bank proxy for CBS/AD | Shivanshi + bank network + Deepali | S09 entry |
| Session store = DynamoDB vs small Redis | WS-2 + Deepali | Before Keycloak lands in UAT |
| Aurora Global vs backup-restore for DR | Aarti + Shivanshi | Must still meet RTO ≤ 1 h **measured** |
| Argo CD vs alternative GitOps | Shivanshi + Amit | S09-E03 |
| Flutter hosting (internal MDM vs public store) | Rajal + Deepali | S11 |
| Cost envelope | Shivanshi + Kalpana | S09 output, not an architecture invention |

---

## 15. Sign-off required before this pack is used as S09 input

| Persona | Question they answer |
|---|---|
| **Mahesh / Architecture** (human T4) | Is this the R0 estate, and only the R0 estate? |
| **Deepali / Security** (human) | Trust boundaries TB-1…TB-6 realised; no public data; KMS/IAM acceptable |
| **Aarti / Database** | One-cluster topology acceptable; schema isolation and restore design acceptable |
| **Shivanshi / SRE** | Operable, observable, restorable; NAT EIP and CBS connectivity path identified |
| **Shailja / Compliance** | Residency + 7-year WORM + audit/operational pipe split permissible |
| **Amit / Engineering** | Service packaging and IRSA mapping implementable |
| **Kalpana / Delivery** | S08/S09 critical path sequenced; this request is on it |

Until those signatures exist, platform engineers may **draft** Terraform modules against this file in `dev` with synthetic data only.

---

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1), AI-drafted
**signature_status:** `AI-DRAFTED — mandatory human T4 Architecture sign-off outstanding; Security, Database and SRE reviews outstanding`
**Companion HLD:** [`R0-HLD.md`](./R0-HLD.md)
**Diagrams:** [`r0-lld.svg`](./r0-lld.svg) · [`r0-platform-*.png`](./diagrams/README.md) · [`r0-reference-architecture.svg`](./r0-reference-architecture.svg)
