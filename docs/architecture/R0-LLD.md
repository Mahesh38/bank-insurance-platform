# R0 Low-Level Design — AWS platform pack for the CTO and AWS platform team

**Workstream:** WS-3 · **Horizon:** H0 — R0 as designed
**Owner:** Mahesh — Principal Insurance Platform Architect (Board 1)
**Consumers:** CTO; AWS platform / landing-zone team; **Shivanshi** (SRE, Board 7 — provisions and operates); **Deepali** (Security — trust boundaries, IAM, KMS); **Aarti** (Database — Aurora/DynamoDB/S3 physical design)
**Status:** `AI-DRAFTED`. This file is the S09 *requirements pack*. It is **not** an approval to apply Terraform. Mandatory reviews before first `apply` to a non-dev account: Architecture (human T4), Security (human), Database, SRE, Compliance (residency and WORM).
**Date:** 2026-08-20
**Origin:** `SUG-20260820-hl1`
**Picture this document walks:** [`r0-lld.svg`](./r0-lld.svg)
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
**Diagram:** [`r0-reference-architecture.svg`](./r0-reference-architecture.svg)
