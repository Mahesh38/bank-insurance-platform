# R0 Low-Level Design — AWS platform pack for the CTO and AWS platform team

**Workstream:** WS-3 · **Horizon:** H0 — R0 as designed
**Owner:** Mahesh — Principal Insurance Platform Architect (Board 1)
**Consumers:** CTO; AWS platform / landing-zone team; **Shivanshi** (SRE, Board 7 — provisions and operates); **Deepali** (Security — trust boundaries, IAM, KMS); **Aarti** (Database — Aurora/DynamoDB/S3 physical design)
**Status:** `AI-DRAFTED`. This file is the S09 *requirements pack*. It is **not** an approval to apply Terraform. Mandatory reviews before first `apply` to a non-dev account: Architecture (human T4), Security (human), Database, SRE, Compliance (residency and WORM).
**Date:** 2026-08-20 · **revised** 2026-08-24 · **revised** 2026-08-25 (`ADR-014`, `ADR-015`) · **revised** 2026-08-31 (`ADR-018`, attach to existing `AU-CTO-NETWORK`) · **revised** 2026-09-14 (`ADR-020` split API plane) · **revised** 2026-10-09 (`ADR-023` NLB + Istio; API Gateway withdrawn)
**Origin:** `SUG-20260820-hl1` · **revision** `SUG-20260824-gp1` … `gp5` ([`CR-012`](../governance/change-requests/CR-012-r0-platform-robustness.md)) · **revision** `SUG-20260825-ll1` · **revision** `ADR-015` (one NIP-APP; `ns:edge` is nip-web + #2 NIP BFF only) · **revision** `ADR-023` / `CR-017` Option B

> **Revision 2026-08-24 — R0 robustness round.** Five layers that were deferred are now **in R0**,
> under `ADR-009` … `ADR-013`: hybrid bank connectivity (**attach** to the existing `AU-CTO-NETWORK`
> Transit Gateway + VPN now, Direct Connect via the **existing** DX Gateway), centralised egress inspection (AWS Network Firewall), a managed cache
> tier (ElastiCache for Valkey), an event backbone (Amazon MSK, **with the transactional outbox
> retained as its source of truth**) and an operational search pipe (Amazon OpenSearch). What did
> **not** change at that revision: analytics warehouse stays out, one Aurora cluster
> stays (`ADR-008`), idempotency stays in the owning service's store, configuration still fails
> closed, and no regulatory evidence lives in a topic or an index. §1.4 states the per-environment
> shapes, because the cheapest way to make this set unaffordable is to build production three
> times. *(Mesh deferral from this revision is **overturned** by `ADR-023` — see 2026-10-09.)*
>
> **Revision 2026-08-25 — ADR-015 NIP-APP.** One Flutter enterprise client (web + APK + IPA).
> RM, IPR, admin and ops are **roles**, not applications. `ns:edge` holds **nip-web** and
> **#2 NIP BFF** only — nothing RM-named or admin-named. Web on EKS; APK on Play Store; IPA
> on the App Store. `#18` Reporting & MIS stays R0 W4 on the isolated read path (`C-ISO-1`).
> Glue/Athena/Redshift/QuickSight stay **out**.
>
> **Revision 2026-09-14 — ADR-020 split API plane.** Outbound (1SB, SMS, bank internal APIs,
> AD-verify) leaves via **Apigee**; 1SB allowlists **Apigee IPs**, not spoke NAT EIPs. Five
> Control Tower accounts (`dev` is a VPC inside the UAT account). No CUG at R0. Workforce
> AD-verify is the existing bank API via Apigee private — never LDAP from EKS. *(Inbound
> clause of this revision is **amended** by `ADR-023` — see below.)*
>
> **Revision 2026-10-09 — ADR-023 bank-standard inbound.** Inbound is Cloudflare → F5-XC →
> **internet-facing NLB** → **Istio Ingress Gateway** (path split to nip-web / BFF /
> PG-callback). Amazon API Gateway and Internal ALB-as-Proxy-2 are **withdrawn**. Security
> controls S1–S11 are non-negotiable (`2026-10-09-ADR-023-SECURITY-CONTROL-MAP.md`). Apigee
> outbound (`ADR-020`) and private Keycloak / token-hiding BFF / PDP stand unchanged.
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

This LLD **narrows** [`architecture-review/04-aws-infrastructure-architecture.md`](../platform/architecture-review/04-aws-infrastructure-architecture.md). That review is the North-Star AWS estate. R0 does not build all of it. Where the two disagree, **this file and ADR-001 / ADR-008 / ADR-009 … ADR-013 win**.

After the 2026-08-24 round the gap between the two is narrower and differently shaped. R0 now
builds the review's **event backbone, cache tier and search pipe**; it still does not build the
review's **service mesh, per-service database clusters or analytics warehouse**, and it uses the
review's cache tier for a deliberately shorter list of purposes than the review names
(`ADR-011` refuses idempotency).

Capacity context, so nobody sizes for a problem we do not have: R0 pilot demand is on the order of **~100 journey starts per hour BAU, ~7 per minute at Q4 peak** ([`05-nfr-catalogue.md`](../platform/ws3-platform/05-nfr-catalogue.md) CAP-A*). The platform is correctness-, evidence- and recovery-constrained, not throughput-constrained. Do not scale from CPU.

**This matters more after the robustness round, not less.** Three brokers, a cache replication
group and a search domain are sized in this file for **availability and evidence**, never for
throughput. Three MSK brokers carry tens of messages a minute. If a capacity conversation starts
from any of these components, it is the wrong conversation: name the business load, the real
bottleneck and the next downstream limit first (`NFR-THR-06` is still the one that bites — the
Aurora connection budget).

---

## 1. One-page AWS bill of materials

### 1.1 PROVISION in R0

| # | AWS service | What it is for | SKU / shape (starting point — Shivanshi confirms) |
|---|---|---|---|
| 1 | **AWS Organizations** + **5 accounts** | Isolation | `shared-services`, `security`, **`network`**, **`uat`**, `prod`. **No separate `dev` account. No CUG** (`ADR-020`). The UAT account hosts **two VPCs** (`vpc-dev`, `vpc-uat`) so stub vs real CBS do not share a route table. Spring profiles remain `dev` / `uat` / `prod`. `dev` data is synthetic |
| 2 | **Amazon VPC** × environment | Network | 3 AZs, public + private-app + private-data subnets. See §2 |
| 3 | **NAT Gateway** × AZ, **in the egress VPC** | Spoke egress with **fixed Elastic IPs** | Remaining internet destinations (toward Apigee / other allowlisted hosts). **Not** the 1SB allowlist — 1SB allowlists **Apigee egress IPs** (`ADR-020`). Do not publish these EIPs to 1SB. **Moved** out of the workload VPCs by `ADR-010` — see §2.3 |
| 4 | **Amazon Route 53** | Public and private DNS | Hosted zone per env; no latency-based DR routing in R0 |
| 5 | **Cloudflare Enterprise (CDN & DDoS)** — **SaaS, not AWS, not in any VPC** | Edge CDN in front of the API **and** the RM/admin web UIs | Bank standard. TLS 1.3; origin = F5-XC (SaaS) → **internet-facing NLB** → **Istio Ingress Gateway** (`ADR-023`). Static Flutter/admin assets from `nip-web` on that chain — **not** a public S3 website and **not** a PVC. Authenticated JSON is **never** cached |
| 6 | **F5 Distributed Cloud / F5-XC (Advanced WAF)** — **SaaS, not AWS, not in any VPC** | Bank standard L7 Web Application Firewall | OWASP Top 10, bot protection, L7 rate limits. **Not** an in-VPC F5 BIG-IP. Must present shared-secret header and/or mTLS to origin (`ADR-023` S3) |
| 7 | **Internet-facing Network Load Balancer (NLB)** | First **AWS** hop (bank-standard entry) | Public/DMZ subnets. Targets Istio Ingress Gateway pods. **SG allowlist = F5 (+ Cloudflare) egress CIDRs only** — never 0.0.0.0/0 (`ADR-023`). ALB variant only if bank ACM-at-LB standard requires it |
| 8 | **Istio** (Ingress Gateway + sidecars) | Cluster ingress + east–west mTLS | Ingress Gateway does path split (`/*` → nip-web, `/api/*` → BFF, `/callbacks/pg/*` → PG callback). Sidecars in app namespaces; PeerAuthentication → **STRICT** after dated PERMISSIVE cutover. **Admitted by `ADR-023`** (overturns prior mesh deferral) |
| 8a | **Apigee (bank outbound API plane)** — **not AWS, not Terraform** | Loading dock for every call that leaves the building | Unchanged (`ADR-020`). Adapter base URL = Apigee proxy. 1SB allowlists **Apigee IPs**. Flutter never calls Apigee |
| 8b | **Amazon API Gateway** | — | **WITHDRAWN (`ADR-023`).** Do not provision for inbound or PG-callback |
| 8c | **Internal ALB** (path-routing Proxy 2) | — | **WITHDRAWN as required hop (`ADR-023`).** Path routing is Istio’s job |
| 9 | **Amazon EKS** × environment | All microservices + Istio | Kubernetes 1.30+ (platform current). Private API endpoint. See §3 |
| 10 | **Amazon ECR** | Images | Immutable tags; scan on push; replicate to `ap-south-2` for DR images |
| 11 | **Amazon Aurora PostgreSQL** | **One** cluster, schema per bounded context | Multi-AZ writer + reader. See §5. **ADR-008** |
| 12 | **Amazon DynamoDB** | Journey state, quote/proposal jobs, audit event store, BFF session option | PITR on; encryption with CMK |
| 13 | **Amazon S3** + **Object Lock** (Compliance mode) | Raw payloads, policy documents, audit archive | 7-year WORM; CRR to `ap-south-2`; Block Public Access on |
| 14 | **AWS KMS** | CMK hierarchy | Separate CMKs: data, logs, secrets, WORM. India only |
| 15 | **AWS Secrets Manager** | DB creds, 1SB keys, PG keys, IdP secrets | Rotation exercised once (`NFR-SEC-07`) |
| 16 | **IAM Roles for Service Accounts (IRSA)** | Workload identity | No static keys in pods |
| 17 | **Amazon CloudWatch Logs** + **CloudWatch Metrics** | Operational logs/metrics | PII-scrubbed; 90-day operational retention. Alerts, container metrics, latency timers |
| 18 | **AWS CloudTrail** | Management & security audit log | In the `security` account. Mandatory governance trail recording all AWS API actions and IAM modifications |
| 19 | **AWS X-Ray** *or* **ADOT collector → AMP** | Tracing | One choice; Shivanshi picks. Traces must span BFF → services → Hub → adapter |
| 20 | **Amazon Managed Service for Prometheus** + **Amazon Managed Grafana** | RED metrics, later HPA custom metrics | Thin in R0: four dashboards, not a platform rewrite |
| 21 | **AWS Config** + **GuardDuty** + **Security Hub** | Account security posture | In the `security` account. Distinct from application `#16` |
| 22 | **GitLab CI/CD & Terraform** | Enterprise delivery & IaC standard | GitLab pipelines run compilation, ArchUnit, JaCoCo, and Terraform apply for all environments. Ansible executes automated DR drills, failover verification, and post-deployment sanity tests |
| 21 | **Amazon SNS** + **Amazon SQS** (optional, thin) | Outbox worker wake-up / notification send queue | Not an event bus. Do **not** introduce MSK because SQS exists |
| 22 | **AWS Backup** | Aurora, DynamoDB, EBS (if any) | Meeting `NFR-DR-02` RPO ≤ 5 min for transactional core |
| 23 | **VPC endpoints** | S3, DynamoDB, Secrets Manager, ECR, STS, Logs | Stop Secrets and ECR pulling via NAT |
| 24 | **AWS Certificate Manager** | Public/private certs for Istio Gateway TLS (or ALB if ALB variant); mesh trust anchors per platform standard | |

#### Added by the 2026-08-24 robustness round

| # | AWS service | What it is for | SKU / shape (starting point — Shivanshi confirms) | ADR |
|---|---|---|---|---|
| 25 | **AWS Transit Gateway** — **attach**, do not clone | Spoke attachment to the existing `AU-CTO-NETWORK` hub for bank-directed, inter-VPC and DR routing | **Do not provision a second TGW.** RAM-share or spoke-attach; Shivanshi confirms with bank network. **One route table per environment** — dev cannot route to a prod bank prefix. See §2.2 | `ADR-009` |
| 26 | **AWS Site-to-Site VPN** → bank DC | The bank path that is available in the same change as the VPC | 2 tunnels, BGP, TGW attachment. **Provisioned first**; stays as the standby path after DX lands | `ADR-009` |
| 27 | **AWS Direct Connect** via the **existing Direct Connect Gateway** | The production bank path | **Do not order a second circuit.** Use the existing DXGW (Sify / Airtel, already extended to Hyderabad). 2 hosted VIFs at 2 Mumbai DX locations. Primary by BGP preference once our prefixes are accepted | `ADR-009` |
| 28 | **Inspection / egress VPC** × environment (in `network`) | Where the NAT gateways and the firewall live | /24 firewall subnets + /24 public subnets × AZ. **One per environment** — prod egress never transits a dev-mutable VPC | `ADR-010` |
| 29 | **AWS Network Firewall** × environment | L7 egress and inter-VPC inspection with a domain allowlist and IPS | Restored to `ADR-010`. The 2026-08-27 in-VPC F5 BIG-IP icon is retracted (`ADR-018`): F5 on this estate is F5-XC SaaS on the north-south path, not an appliance in the inspection VPC. The existing AU Bank EDGE VPC already runs FortiGate NGFW (Active/Passive) — we attach as a spoke (`ASM-012`) | `ADR-010`, `ADR-018` |
| 30 | **Amazon ElastiCache for Valkey** × environment | BFF session vault · L2 read-through cache · rate-limit counters | Cluster mode **disabled**, primary + replica across 2 AZs, automatic failover **on**, CMK at rest, TLS in transit, per-service ACL user with a key prefix. **Never** the idempotency store | `ADR-011` |
| 31 | **Amazon MSK** × environment | Event backbone for domain-event fan-out | **3 brokers, one per AZ**, KRaft, TLS, at-rest CMK, **SASL/IAM** with per-topic policy. Fed by the outbox, which stays the source of truth | `ADR-012` |
| 32 | **AWS Glue Schema Registry** | Event contract for every topic | Backward compatibility enforced in CI (`FF-25`). Schema registry only — **not** Glue ETL | `ADR-012` |
| 33 | **Amazon OpenSearch Service** × environment | Operational search and log analytics | **VPC-only**, dedicated masters + data nodes, CMK, node-to-node encryption, fine-grained access control, ISM 30 d hot → delete at `RET-OPERATIONAL`. **Holds no evidence** | `ADR-013` |
| 34 | **Amazon Data Firehose** + Fluent Bit | The ingest path into #33 | Firehose delivery stream per environment with an S3 failed-delivery bucket, so a mapping error loses an index document and not a record | `ADR-013` |

### 1.2 PROVISION for WS-2 (R0 will not boot without these)

| # | Component | Notes |
|---|---|---|
| 35 | **Keycloak** on EKS (or equivalent private IdP) | Behind `identity-provider-adapter-service`. Not exposed to Flutter. **NIP-APP / Fireframe** is the only UI chrome for login and user/role/permission mapping — Keycloak admin console is not shown to bank users (`ADR-020`). Workforce AD-verify is the **existing bank API via Apigee private**, never LDAP from EKS. Partners are created in this IdP, never in AD |
| 36 | **Aurora schema `identity` / `keycloak`** | Keycloak's database. **Do not give Keycloak a PVC as its source of truth** — see §4 |
| 37 | **Session store for the token-hiding BFF** | **DECIDED: ElastiCache for Valkey** (BOM #30), per `ADR-011`. This closes the open DynamoDB-versus-Redis question in favour of WS-2's accepted design, so the two workstreams now specify one session store. The DynamoDB `sessions` table is **withdrawn** (§5.2) |

### 1.3 DO NOT PROVISION in R0

Shorter than it was, and the survivors are here on their own reasoning rather than by inheritance.

| AWS service | Why it appears in target-state docs | Why it is out of R0 |
|---|---|---|
| **Second / per-service Aurora clusters** | Misreading of `ARCH-004` | `ADR-008` — one cluster, schema per context |
| **Public RDS / public Service on domain pods / public EKS API / public OpenSearch** | Convenience | Standing constraint (`ADR-023`): only the **ingress NLB** (and bank SaaS) are internet-facing. Pods and data plane stay private. Search domain VPC-only (`ADR-013`) |
| **Amazon Cognito as the R0 IdP** | Older review text | `ARCH-018` — Keycloak first, adapter-neutral |
| **Amazon API Gateway** (inbound or PG-callback) | Former `ADR-018` | **Withdrawn** — `ADR-023`. Bank standard is NLB + Istio |
| **Internal ALB as north–south path router** | Former Proxy 2 | **Withdrawn** — Istio Ingress owns path split (`ADR-023`) |
| **AWS App Mesh** (instead of Istio) | AWS mesh product | Bank standard is **Istio**; do not run two meshes |
| **AWS Glue ETL / Athena / Redshift / QuickSight** | Analytics warehouse | **Still out of R0.** `#18` Reporting & MIS **is** in R0 (`ADR-014`) as an isolated **read path** (events / replica / extract consumed from `ns:jobs`) — that is not a warehouse. Glue **Schema Registry** (#32) is in; Glue ETL is not |
| **MSK Replicator / MirrorMaker to `ap-south-2`** | Target cross-region backbone | `ADR-012`: the outbox is in Aurora and Aurora is replicated, so events are reproducible by replay. A broker replica is a second copy of something already recoverable |
| **ElastiCache as an idempotency or evidence store** | Target cache tier names it | `ADR-011` refuses it. Idempotency must be atomic with the business write (`INV-IDM-01`, `INV-PAY-04`); a cache cannot be |
| **OpenSearch as the audit or evidence store** | It would be convenient and searchable | `ADR-013` refuses it. An index with a delete policy is not a 7-year immutable record, and a searchable copy becomes the copy people cite |
| **A third-party NGFW appliance (Fortigate / Palo Alto)** | The existing AU estate runs one | Rejected on operational surface, not capability (`ADR-010` alternatives). It is the named migration if the bank's network standard requires it |
| **Self-managed Redis, Kafka, Prometheus or ELK on EKS** | Cheaper on paper | Every one of them is a stateful cluster with PVCs, upgrades and an on-call rotation, owned by a team still closing `GATE-S08`. Managed equivalents are in the BOM above |
| **Insurer callback / webhook API Gateway** | Target inbound provider ingress | R0 polls (`S-11`). Building ingress early creates a public surface with no consumer |
| **Customer-facing CloudFront distribution for DIY** | `#1` Customer BFF | R1 |
| **Render.com as an environment** | Existing `render.yaml` | Dev-preview **only**. Never PII, never a gate artefact (`ADR-001`) |
| **Any resource outside `ap-south-1` except DR replicas in `ap-south-2`** | — | Control C6, `FF-08` |
| **Apigee as an R0 AWS / Terraform resource** | Bank API plane (`SUG-20260831-apg`) | **Not provisioned in our accounts.** Onboard as a bank product (`ADR-020`, BOM #8a). Drawn on **egress only** |
| **A second Transit Gateway, a second Direct Connect, or cloning EDGE as a second hub** | The live estate already has `AU-CTO-NETWORK` TGW, DX Gateway, EDGE VPC | Attach as a **spoke** (`ADR-009`) |
| **Internet Gateway on private-app / private-data subnet route tables** | Convenience | Pods stay private. Public/DMZ subnets for the **ingress NLB only** (`ADR-023`); do not give domain pods public IPs |
| **Public Istio routes to Keycloak / data plane** | Misuse of Gateway | Forbidden (`ADR-023` S5, S4) |

### 1.4 Per-environment shapes — the reason this set is affordable

The robustness round adds three stateful services, an inspection VPC and two circuits. Built at
production shape in three environment slices it triples the most expensive part of the estate and
proves nothing extra. **`dev` is deliberately not production-shaped**, and it is a **VPC inside
the UAT Control Tower account**, not a sixth account (`ADR-020`).

| Layer | `dev` (VPC in UAT account) | `uat` (VPC in UAT account) | `prod` |
|---|---|---|---|
| Bank path (`ADR-009`) | **VPN only.** CBS and AD **stubs permitted here and nowhere else** | VPN, then DX primary when the circuit lands. **Real CBS/AD test instances — no stubs** | DX primary (2 VIFs, 2 locations) + VPN standby, failover **exercised** (`NFR-NET-01`) |
| Inspection (`ADR-010`) | 1 firewall endpoint, 1 AZ, managed IPS in **alert** mode | 2 endpoints, 2 AZs, IPS alert → drop before prod | 3 endpoints, 3 AZs, IPS in **drop** mode |
| Cache (`ADR-011`) | 1 node, no failover | 2 nodes / 2 AZs, automatic failover on | 2 nodes / 2 AZs minimum, failover on, reserved capacity |
| Broker (`ADR-012`) | **MSK Serverless** (or 1 broker) | 3 brokers × 3 AZs, `kafka.m7g.large` starting point | 3 brokers × 3 AZs, sized by Shivanshi on measured lag |
| Search (`ADR-013`) | 1 data node, no dedicated master, **7-day** ISM | 2 data + 3 master, 30-day hot | 3 data + 3 master, 30 d hot → delete at 90 d |
| Aurora | Single instance | Writer + reader, 2 AZs | Writer + reader, different AZs, asserted in IaC |

Two rules keep this from drifting. A shape is raised in `dev` only with a reason recorded in the
same change. A shape is **never** lowered in `prod` to fit a cost conversation without a Security
and SRE verdict — `RISK-012` is the register entry for that conversation, and it is open.

---

## 2. Network — VPC (required)

The user-facing question "do we need a VPC?" is **yes**. Every environment gets its own VPC. Production does not share a VPC with UAT.

```text
WORKLOAD VPC per environment (e.g. 10.{env}.0.0/16)      Region ap-south-1
├── Public subnets        /24 × 3 AZs     reserved — NO NAT, no workloads (see §2.3)
├── Private-app subnets   /20 × 3 AZs     EKS nodes (services, BFF, Keycloak)
├── Private-data subnets  /24 × 3 AZs     Aurora, ElastiCache, MSK brokers, OpenSearch
├── TGW attachment subnets /28 × 3 AZs    one ENI per AZ — the only way out
└── Endpoints             in private      S3, DDB, Secrets, ECR, STS, Logs

INSPECTION / EGRESS VPC per environment, in the `network` account   (§2.3)
├── Firewall subnets      /24 × 3 AZs     AWS Network Firewall endpoints
├── Public subnets        /24 × 3 AZs     NAT Gateways + Elastic IPs (spoke→Apigee / remaining internet; **not** the 1SB allowlist)
├── TGW attachment subnets /28 × 3 AZs
└── Internet Gateway                      the only IGW with a route to anything

TRANSIT GATEWAY — attach to the existing `AU-CTO-NETWORK` hub (`ADR-009`)  (§2.2)
├── do NOT provision a second TGW "for insurance"
├── route table: dev      dev VPC + inspection-dev + VPN
├── route table: uat      uat VPC + inspection-uat + VPN/DX (uat prefixes only)
├── route table: prod     prod VPC + inspection-prod + DX primary, VPN standby
└── route table: dr       ap-south-2 attachment for the warm standby (D16)
```

Workload VPC **public/DMZ subnets** hold the **internet-facing NLB only** (`ADR-023`). They are
not a place for NAT, domain pods, or a public ALB in front of every service. Private-app and
private-data route tables still have **no** Internet Gateway and **no** local NAT — egress is
centralised (`ADR-010`). Do not copy the existing banking application's Public VPC + IGW +
Public ALB + VPC-peering pattern as the *whole* app front door; the NLB is a narrow, CIDR-locked
entry that targets Istio Ingress Gateway pods only.

| Control | Requirement |
|---|---|
| Internet-facing | Cloudflare + F5-XC (SaaS) + **ingress NLB** (SG = F5/CF CIDRs only; F5 shared-secret and/or mTLS — `ADR-023` S1–S3). **No** public Service/NodePort on domain pods. No public OpenSearch endpoint, no public broker listener. Keycloak never on public Istio routes (S5) |
| EKS API | Private endpoint; `publicAccess = false` in prod |
| Data subnets | No 0.0.0.0/0 route. Aurora, ElastiCache, MSK and OpenSearch cannot initiate internet traffic |
| Egress | Default route from private-app/data is the **Transit Gateway**, never a local NAT. Provider and leaving-the-building HTTPS: TGW → **Network Firewall** (Deepali still accepts this hop on **pod → Apigee**) → Apigee → target (`ADR-020`). **1SB allowlists Apigee IPs, not these NAT EIPs.** Do not publish the inspection-VPC EIP list to 1SB |
| Bank-directed | CBS/CIF and AD-verify reachable **via Apigee private targets** (`ADR-020`), not by hairpinning Cloudflare/F5, and not by LDAP from EKS to AD. Stubs in `dev` only |
| East-west, in cluster | Istio PeerAuthentication → **STRICT** after dated PERMISSIVE cutover **plus** Kubernetes `NetworkPolicy` default-deny (`ADR-023` S11). Firewall still does not replace pod-to-pod controls |
| East-west, inter-VPC | No VPC peering. Everything transits the TGW, so it is inspected and logged |
| DNS | Private hosted zone for `*.svc.cluster.local` plus mesh/internal DNS for Istio Gateway. Bank zones resolved by inbound Route 53 Resolver endpoints over the TGW |
| Flow logs | VPC, TGW, firewall, **NLB and Envoy** access/flow logs to CloudWatch and to OpenSearch (`ADR-013`, `ADR-023` S10), `RET-OPERATIONAL` |

**CBS and Bank AD are bank-internal, and R0 now provisions the path to them rather than deferring
it** (`ADR-009`, §2.2). The previous position — "either Direct Connect / VPN or a bank-hosted
proxy, decided at S09 entry, stubs acceptable in `dev`" — left the two longest-lead items on the
programme behind an unowned decision, and made the stub the only tested path. The pattern is now
decided; what remains genuinely external is the bank's own side of it, and that is recorded as a
dated dependency with a named owner, not as an architecture assumption. **Architecture still does
not invent the bank network:** prefixes, firewall rules and the DX order are Shivanshi's with the
bank network team.

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
| Public / DMZ subnet | ✅ | ✅ | ✅ | One /24 per AZ | Holds the **ingress NLB** (`ADR-023`). Not for NAT or domain pods. Cheap to create, expensive to retrofit |
| Private-app subnet | ✅ | ✅ | ✅ | One /20 per AZ | EKS nodes; the /20 is for pod IPs (VPC CNI), not node count |
| Private-data subnet | ✅ | ✅ | ✅ | One /24 per AZ | Aurora needs a subnet group spanning ≥ 2; the third keeps failover choice open. Now also holds ElastiCache, MSK and OpenSearch |
| **TGW attachment subnet** | ✅ | ✅ | ✅ | One /28 per AZ, every VPC | An attachment ENI missing from one AZ silently sends that AZ's egress across an AZ boundary, or nowhere |
| **NAT Gateway + EIP** *(inspection VPC only)* | ✅ | ✅ **prod/uat** | ⬜ *(prod)* | **prod: per AZ. dev: one** | A single NAT is an AZ-wide egress SPOF on the hop **pod → Apigee**. Every NAT adds an EIP — **do not publish that list to 1SB** (`ADR-020`). 1SB allowlists Apigee IPs (§2.3, §8) |
| **Network Firewall endpoint** | ✅ | ✅ **prod/uat** | ⬜ *(prod)* | One per AZ that has a NAT | The endpoint is the egress path. One endpoint for three AZs means an AZ event or a firewall maintenance window is a **total egress outage**, and the quote path notices first |
| Internet Gateway | — regional — | | | One per VPC, **inspection VPC only** | Not AZ-bound. The workload VPCs have none |
| **Ingress NLB** | ✅ | ✅ | ✅ | Public/DMZ subnets in all three AZs | First AWS hop (`ADR-023`). Losing it loses every RM session and PG callback |
| **Istio Ingress Gateway** | ✅ | ✅ | ✅ | Pods in private-app; spread across AZs | Path split + TLS terminate toward mesh (§3). Targets of the NLB |
| **EKS control plane** | — AWS-managed, multi-AZ — | | | Private endpoint, `publicAccess = false` in prod | AWS spreads it; we do not choose |
| **EKS managed node group** (sale path) | ✅ | ✅ | ⬜ *(prod: yes)* | **UAT: ≥ 3 nodes across ≥ 2 AZs. prod: across 3** | §4.2. Two AZs is the floor at which a `PodDisruptionBudget` of `minAvailable: 1` can still drain a node. Three removes the "lose an AZ, lose half the capacity" arithmetic |
| Sale-path pods | ✅ | ✅ | ⬜ | `minReplicas: 2`, `topologySpreadConstraints` across `topology.kubernetes.io/zone`, `PDB minAvailable: 1` | Two pods on one node in one AZ satisfies `min 2` and survives nothing. The spread constraint is the control, not the replica count |
| Keycloak | ✅ | ✅ | ⬜ | ≥ 2 replicas, ≥ 2 AZs, no PVC (§4.1) | Identity down is a total outage — the PDP fails closed by design (`S-02`) |
| **Aurora writer** | ✅ | ⬜ | ⬜ | One AZ at a time, by definition | A writer is single-AZ; Multi-AZ means the *failover target* is elsewhere |
| **Aurora reader** | ⬜ | ✅ | ⬜ | **Different AZ from the writer** — assert it in IaC | A reader in the writer's AZ is a read-scaling replica, not an availability one. This is the single most common Multi-AZ misconfiguration |
| DynamoDB · S3 · KMS · Secrets Manager · ECR | — regional, AWS multi-AZ — | | | Nothing to place | Do not build AZ logic around them |
| Interface VPC endpoints (Secrets Manager, ECR, STS, Logs) | ✅ | ✅ | ✅ | One ENI per AZ | An endpoint present in two of three AZs makes the third AZ's pods fail to pull images while looking healthy |
| Gateway VPC endpoints (S3, DynamoDB) | — route-table, not AZ — | | | Attach to every private route table | Missing on one table sends that AZ's S3 traffic through the firewall and NAT, quietly |
| **ElastiCache for Valkey** *(required — `ADR-011`)* | ✅ | ✅ | ⬜ | Replication group, **2 AZs**, automatic failover **on** | A single-node session vault makes an AZ loss a mass logout. Two nodes, not a shard: the R0 working set fits one node many times over |
| **MSK brokers** *(required — `ADR-012`)* | ✅ | ✅ | ✅ | **One broker per AZ, 3 AZs.** Replication factor 3, `min.insync.replicas` 2 | Three brokers is the **availability** floor at which a broker can be lost or patched without stopping the publisher. It is not a throughput calculation — R0 is tens of messages a minute |
| **OpenSearch data nodes** *(required — `ADR-013`)* | ✅ | ✅ | ✅ *(prod)* | Data nodes across every AZ in use; **3 dedicated masters** in `uat`/`prod` | Dedicated masters across 3 AZs are what prevent a split brain from becoming a lost index. `dev` runs one node and no dedicated master, on purpose |

**Two AZs or three?** Three for subnets, endpoints, TGW attachments, the ingress NLB and Istio
Gateway pods — they cost nothing per AZ (subnets) relative to retrofit risk, and cannot be added
later without renumbering. Two is acceptable for *paid*
capacity (nodes, NAT, firewall endpoints, Aurora reader, cache replica) in `uat`, three in `prod`.
**Three is not optional for the MSK brokers and the OpenSearch masters** in `uat` and `prod`:
quorum-based services degrade differently from stateless ones, and two of three is the difference
between losing a node and losing the cluster. `dev` is single-AZ deliberately: it is synthetic data
and an AZ failure there is not an incident.

**What AZ placement does not buy.** An AZ is not a region. Losing `ap-south-1` entirely is §11, and
no amount of AZ spreading answers it.

### 2.2 Hybrid bank connectivity — the path to CBS and Bank AD (`ADR-009`)

R0 reads CIF data from Core Banking (`#4`, seam `S-05`) through **Apigee private** (`ADR-020`)
and verifies workforce credentials against the **existing bank AD-verify API** (same plane;
never LDAP from EKS). The spoke still **attaches** to `AU-CTO-NETWORK` so that path is private.
Neither CBS nor AD-verify is reachable from a VPC by default; this round provisions the private
hop, not an LDAP bind and not a Cloudflare/F5 hairpin.

```text
         ap-south-1                                  AU Bank data centre
 ┌──────────────────────────┐
 │ workload VPC  dev/uat/prod│                       ┌────────────────────┐
 │   private-app  ──► TGW    │══ DX  (2 VIFs, 2 loc) │ CBS / CIF          │
 │   attachment ENIs         │──  VPN (2 tunnels) ──►│ Bank AD / SSO      │
 └──────────────────────────┘   standby, always kept │ bank firewall      │
              │                                      └────────────────────┘
              ▼
      inspection VPC (§2.3) ──► internet
```

| Element | R0 requirement | Owner |
|---|---|---|
| Transit Gateway | **Attach to the existing `AU-CTO-NETWORK` regional TGW** (Central Network Account Architecture V1). If RAM-share into this programme's `network` account, or attach the spoke to that hub — Shivanshi confirms with bank network. **Do not provision a second TGW.** Per-environment route tables so `dev` cannot reach a prod bank prefix. Default association **off** | Shivanshi + bank network |
| Route tables | One per environment plus one for DR. A bank prefix is advertised into **one** environment's table. Asserted in the IaC scan, not in review | Shivanshi + Deepali |
| Site-to-Site VPN | **Provisioned first**, 2 tunnels, BGP, ECMP off. This is what removes the stub from `uat` without waiting for a circuit | Shivanshi + bank network |
| Direct Connect | **Use the existing DX Gateway** (Sify / Airtel, already extended to Hyderabad). 2 hosted VIFs at 2 Mumbai DX locations through that DXGW. Do not order a second circuit for this programme. Primary by BGP local-preference once our prefixes are accepted | Shivanshi + bank network |
| Failover | DX loss falls to VPN automatically. **Exercised and timed once before prod** (`NFR-NET-01`) — an untested standby path is a claim | Shivanshi |
| DNS | Route 53 Resolver inbound/outbound endpoints so bank zones resolve from the VPCs and platform zones resolve from the bank side | Shivanshi + bank network |
| Encryption | TLS on every application flow **regardless** of the private path. A private circuit is not encryption, and "inside the circuit" is not an authorisation | Deepali |
| DR | The `ap-south-2` warm standby attaches to the same design (D16 in §11.1). A standby that cannot reach CBS is not a standby | Shivanshi |

**What is still external, stated as a dependency rather than a design.** The bank must terminate
the VPN, publish the prefixes, open the firewall, and accept the DX order. That is
`DEP-20260824-dx1`, owned by Shivanshi with Kalpana tracking it, and it is one of the two items on
this programme that **cannot be accelerated by working harder** (§13). The other is the Elastic IP
publication, which §2.3 has just changed the shape of.

**Stub policy, tightened.** `dev` may run CBS and AD stubs. `uat` and `prod` may not. A journey
evidenced against a stub is not evidence — that is the whole reason this layer moved into R0.

### 2.3 Centralised egress and inspection (`ADR-010`)

Every packet leaving the platform, and every packet crossing between VPCs, is inspected.

```text
pod ─► TGW ─► Network Firewall endpoint (per AZ) ─► Apigee (bank outbound plane) ─► 1SB / PG session-create / SMS
                        │                              │
                        │                              └─ 1SB allowlists **Apigee egress IPs** (ADR-020)
                        └─ alert + flow logs ─► CloudWatch + OpenSearch (ADR-013)

Internal bank APIs (AD-verify, EBS/CBS):
pod ─► TGW ─► (optional NFW on pod→Apigee) ─► Apigee **private** target ─► bank API
FORBIDDEN: pod ─► internet ─► Cloudflare ─► F5 ─► bank API
FORBIDDEN: Java ─► https://*.1silverbullet.tech
```

| Control | Requirement |
|---|---|
| Coverage | 100% of egress. Any route table whose default route is not the TGW **fails the IaC scan** (`NFR-NET-02`, `FF-22`) |
| Stateful rules | Strict order. Domain allowlist: 1SB, AU Bank PG, SMS/email gateway, plus the AWS endpoints not already covered by a VPC endpoint. **Everything else dropped and logged** |
| IPS | AWS managed rule groups, **alert mode until prod**, drop in prod. The first drop should be deliberate, not discovered |
| TLS inspection | Enabled for destinations we terminate normally. **Not** on the 1SB mTLS session — a man-in-the-middle on a mutually authenticated channel is an outage, not a control. Those flows are matched on SNI and destination and passed intact |
| Rule-set ownership | The allowlist is versioned configuration. A new egress destination arrives as a pull request in the same change as the code that needs it — a rule set nobody curates decays to permit-any within two incidents |
| Failure posture | Firewall unavailable = **no egress**. That is correct and it is also an outage: endpoints per AZ (§2.1) and a named runbook, because the quote path is the first thing to notice |
| What it is not | Not ingress inspection for public traffic — that is Cloudflare + F5-XC (SaaS) + NLB SG + Istio (`ADR-023`). Not a replacement for Istio mTLS or `NetworkPolicy` |

#### The Elastic IP list is no longer the 1SB allowlist — read this before publishing anything

| | Until ADR-020 | From `ADR-020` |
|---|---|---|
| Where the EIPs live | Inspection VPC of that environment | Unchanged — they still exist for spoke→Apigee / remaining internet |
| How many | One per AZ per environment, and stable | Unchanged |
| Who 1SB allowlists | Those NAT EIPs | **Apigee egress IPs** (`DEP-20260914-apg`). **Do not publish spoke NAT EIPs to 1SB** |
| Who the PG allowlists for **callbacks** | PG source IPs on our callback Gateway route | **Re-homed to Istio** `/callbacks/pg/*` + IP allowlist + signature verify (`ADR-023` S8 / TB-6). Outbound PG **session-create** goes via Apigee |

The list is still smaller and more stable than the pre-`ADR-010` design. Publishing it to 1SB is
now the defect: 1SB would allowlist the wrong host. Re-base any 1SB conversation onto Apigee IPs
(`DEP-20260824-eip` → `DEP-20260914-apg`).

---

## 3. Reverse proxy — SaaS perimeter + NLB + Istio (required)

R0 inbound is **bank SaaS perimeter → internet-facing NLB → Istio Ingress Gateway**. Path
routing lives on Istio Gateway / VirtualService. Amazon API Gateway and Internal ALB-as-Proxy-2
are **withdrawn** (`ADR-023`). East–west Envoy sidecars are **in scope** for application
namespaces (see BOM #8); they are not a substitute for BFF session or PDP.

```text
NIP-APP (web / APK / IPA)
    │  TLS 1.3 · one hostname
    ▼
Cloudflare Enterprise     ← SaaS · NOT AWS · NOT in any VPC
    │
    ▼
F5 Distributed Cloud / F5-XC   ← SaaS WAF · NOT AWS · NOT in any VPC
    │  shared-secret header and/or mTLS to origin (ADR-023 S3)
    ▼
Internet-facing NLB       ← FIRST AWS hop (public/DMZ) · SG = F5(+CF) CIDRs only
    │
    ▼
Istio Ingress Gateway     ← path split · TLS toward mesh (private-app)
    │  GET /* → nip-web · /api/* → #2 NIP BFF · /callbacks/pg/* → #12
    ▼
#2 NIP BFF                (and, same mesh, WS-2 workforce-access-bff
                            if they remain separate deployables — see note)
    │  mesh mTLS + NetworkPolicy
    ▼
Domain services (never published on Ingress)
```

| Hop | Terminates TLS? | AuthN? | AuthZ? | Business logic? |
|---|---|---|---|---|
| Cloudflare / F5-XC (SaaS) | Yes (bank edge) | No | No | No — CDN / DDoS / WAF / bot / rate only |
| Ingress NLB | Pass-through or TLS (ACM if ALB variant) | No | No | SG allowlist only (S2) |
| Istio Ingress Gateway | Yes (Gateway TLS) | Service / namespace rules only | Mesh AuthorizationPolicy ≠ insurance PDP (S7) | Path routing; Envoy size/throttle (S9) |
| BFF | Re-encrypts outbound | **Session** (token-hiding) | Calls PDP (`S-02`) | Aggregation only |
| Domain service | Mesh mTLS + IRSA | Service identity | Re-checks PDP on regulated actions | Yes |

**Note on two BFFs.** WS-2 already specifies `workforce-access-bff` (token-hiding). WS-3 specifies `#2` NIP BFF (journey + admin/MIS aggregation). R0 may deploy them as **one process** or **two**. That is Amit's packaging choice inside an approved boundary (`A1`). The edge contract does not change: NIP-APP talks to one public hostname and never receives OAuth tokens. There is **no** Admin BFF (`ADR-015`). Istio must **not** validate Flutter end-user JWTs as a substitute for BFF session + PDP (`ADR-023` S6).

### 3.1 Where NIP-APP actually runs

NIP-APP is one Flutter project. It is not a volume, not a second public origin, and not a family of apps. It produces **three artefacts**; only the web artefact is a pod.

```text
NIP-APP  (one Flutter project · role-based views)
  web     →  nip-web pod in ns:edge     image-baked, NO PVC     EKS
  APK     →  Google Play Store
  IPA     →  Apple App Store
  roles   →  BANK_RM · INSURER_PARTNER_REP · BANK_EMPLOYEE (admin/ops)
        │  TLS 1.3   one hostname
        ▼
Cloudflare (SaaS)  ──►  F5-XC (SaaS)  ──►  Ingress NLB     first AWS hop
        │  F5 CIDR SG + shared-secret / mTLS
        ▼
Istio Ingress Gateway                              path rules (no Internal ALB hop)
        ├──  GET  /*            →  nip-web     Flutter web BAKED INTO THE IMAGE
        ├──  /api/*             →  #2 NIP BFF  session, aggregation (admin/MIS too · C-ISO-1)
        └──  /callbacks/pg/*    →  #12 Payment  PG IP allowlist + signature (TB-6)
                │
                ▼
        Domain services (never published on Ingress; Keycloak never on public routes)
```

**Why it sits in EKS with the BFF.** The web UI is workforce-facing. Baking the Flutter web build into `nip-web` in `ns:edge` keeps it on the private-app subnets, behind the same SaaS + NLB + Istio chain, with no public S3 website and **no PVC**. Cloudflare may cache those static files; it must **not** cache authenticated JSON.

**What it is not.** A StatefulSet. A second CloudFront for DIY (`#1` is R1). A public bucket website. A separate admin-web, admin.{env}, or Admin BFF. Serving the SPA from the BFF process itself is allowed — that is Amit packaging the same boundary as one pod instead of two.

Admin / operations (R0 W4, `ADR-014`) are **roles on NIP-APP**. They read `#19` and `#18`. They **NEVER** use the Lead writer (`C-ISO-1`). They never call `lead.create`. Admin/ops are `BANK_EMPLOYEE` on Bank AD.

**Ingress:** Istio Ingress Gateway (mandatory under `ADR-023`). NLB (or bank-standard ALB) targets Gateway pods. Do **not** keep an Internal ALB solely for path routing. Do **not** provision Amazon API Gateway for inbound or PG-callback.

**Do we need Kong, Nginx Plus, or an in-VPC F5 BIG-IP?** No for R0. Cloudflare + F5-XC are the **existing bank SaaS perimeter**; NLB + Istio is the bank-standard AWS entry (`ADR-023`). Do not add a second public reverse-proxy product in front of the NLB.

**Is the Network Firewall a third inbound proxy?** No. `ADR-010`'s firewall sits on the **egress** path (§2.3) and on inter-VPC traffic. It terminates no inbound client session and makes no authorisation decision. Inbound AWS hops are NLB then Istio Ingress.

**Customer payment traffic does not enter the RM session VirtualService.** It goes device → AU Bank PG. PG callbacks enter via **`/callbacks/pg/*`** on Istio, IP-allowlisted to the PG, signature-verified in `#12` (TB-6 / `ADR-023` S8).

---

## 4. Kubernetes — PVC, nodes, namespaces

### 4.1 Persistent Volume Claims — **not required for R0 business services**

Every WS-3 service is **stateless at the pod level** (`ARCH-002`, HLD boundary 8). State lives in Aurora, DynamoDB or S3.

| Workload | PVC? | Why |
|---|---|---|
| All WS-3 domain services, Hub, `#2` NIP BFF, `nip-web`, `#17`, `#18` consumers, outbox workers | **No** | UI files are **in the image**. `emptyDir` only if a crash-only temp file is needed; `readOnlyRootFilesystem: true` |
| `1sb-integration-service` | **No** | Existing design; job state in `bank-persistence-service` / Aurora schema |
| Keycloak | **No PVC as database** | Keycloak JDBC → Aurora schema `keycloak`. A PVC here becomes an unreplicated source of identity truth |
| `aws-load-balancer-controller`, `external-dns`, ADOT, Fluent Bit | **No** | Use official Helm charts' default (hostPath/emptyDir as designed) |
| Prometheus / Grafana self-hosted | **Do not install** | Use AMP + AMG (BOM #19) so we do not own Prometheus PVCs |
| Kafka, Valkey/Redis, OpenSearch on EKS | **Do not install** | Managed services (BOM #30, #31, #33). The robustness round adds three stateful tiers and **none of them is a StatefulSet in this cluster** — that is most of the reason they are affordable |
| Fluent Bit (log shipper for `ADR-013`) | **No** | DaemonSet with the chart's default `hostPath` tail position. It ships to Firehose; it stores nothing |
| Future document-generation that writes local PDFs | Not in R0 | Would be S3, not PVC |

**Pod security (Deepali, S09-E07-S03):** non-root, no privileged, no hostNetwork, read-only root FS, drop all capabilities, seccomp RuntimeDefault. Enforce with Kyverno or Gatekeeper in `shared-services` → each cluster.

### 4.2 Cluster shape

| Item | R0 requirement |
|---|---|
| Clusters | **One EKS per environment** (`dev`, `uat`, `prod`). Not one cluster with namespace-as-env |
| Node groups | Managed on-demand for sale-path (BFF, Journey, Identity, Quotation, Proposal, Payment, Policy, Hub, Adapter). Starting size: 3 nodes × 2 AZ minimum in UAT/prod so PDB can drain |
| Karpenter | **PROVISION (thin)** in UAT/prod; optional in dev. Do not enable Spot on the sale path in R0 |
| HPA | CPU-based, min 2 / max 6 on sale-path deployments. Custom-metric HPA is S12. **NFR-THR-06:** `Σ(pods × pool size) ≤ 60%` of Aurora `max_connections` at max replica count — encode this as a comment in the HPA max and as an assertion in IaC review |
| KEDA | **PROVISION (thin)** — the precondition changed. `ADR-012` puts a broker in R0, so KEDA scales **MSK consumers on consumer-group lag** and nothing else. Do not point it at CPU, and do not use it on the `outbox-publisher`, which stays a `Deployment` with 2 replicas and a poll interval because its work is bounded by the outbox, not by a queue |
| PDB | `minAvailable: 1` on every sale-path Deployment |
| Add-ons | VPC CNI, CoreDNS, kube-proxy, EBS CSI (**installed but unused** by business apps — needed for some add-ons), AWS LB Controller, ExternalDNS, Secrets Store CSI (preferred over env-injected secrets), **Fluent Bit** (→ Firehose → OpenSearch, `ADR-013`), **ADOT collector** (traces, BOM #18), **KEDA** (consumer lag only) |

EBS CSI without a PVC consumer is not waste; it keeps the cluster able to run a future add-on without a change window.

### 4.3 Namespaces (aligns with HLD boundaries, not 1:1 with services)

```text
ns: edge              nip-web (Flutter web, image-baked)  +  #2 NIP BFF
                      (+ workforce-access-bff merged into #2 if Amit packages them as one)
                      NOTHING RM-named or admin-named. One hostname. No admin.{env}
ns: identity          WS-2 adapter, PDP, Keycloak
ns: shared-platform   #4 #5 #6 #7 #8 #9 #12 #13 #16 #17 #19
ns: life-cell         #10 Quotation  #11 Proposal     ← first physical split seam
ns: integration       #14 Hub  #15 1sb-adapter
ns: jobs              outbox-publisher, MSK consumers, reconciliation CronJob,
                      #18 Reporting/MIS consumers (isolated read — C-ISO-1)
ns: platform          kube-system-adjacent controllers only, Fluent Bit, ADOT, KEDA
```

NetworkPolicy: `life-cell` may call `integration` and `shared-platform`; it may **not** be called by `edge` except via `#9`. `identity` is reachable from all app namespaces on the PDP port only.

The three new tiers are **egress** rules on the app namespaces, not new namespaces:

| From | To | Allowed for |
|---|---|---|
| `edge` | ElastiCache `:6379` | Session vault and rate-limit counters only (`ADR-011`) |
| `shared-platform`, `life-cell` | ElastiCache `:6379` | L2 config and catalogue cache only. **Never** idempotency |
| `jobs` | MSK `:9098` (SASL/IAM) | `outbox-publisher` produces; consumers consume their own topics |
| `shared-platform` (`#16` Audit) | MSK `:9098` + DynamoDB + S3 | Consumes the audit topic and writes the evidence store. The topic is not the evidence (`ADR-012`) |
| `platform` (Fluent Bit) | Firehose endpoint | Log ship only. No app namespace talks to OpenSearch directly |
| any app namespace | OpenSearch `:443` | **Denied.** Humans query the domain through the VPC, workloads do not write to it |

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
| `idempotency-{env}` | `owner#key` | Optional shared *implementation* of INV-IDM-01 if Aarti prefers DDB over Postgres. Still **per-service IAM**, not a shared access pattern. **Not** moved to the cache tier — `ADR-011` refuses it | Yes | 24 h |
| ~~`sessions-{env}`~~ | — | **WITHDRAWN 2026-08-24.** The session vault is ElastiCache for Valkey (`ADR-011`, BOM #30), which closes the open DynamoDB-versus-Redis question in favour of WS-2's accepted design | — | — |
| `audit-events-{env}` | `journeyId` + `sequence_no` | Append-only event store. **This, plus the S3 archive, is the audit record** — the MSK topic feeding it is not (`ADR-012`) | Yes | No (archive to S3) |

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

## 6. Caching, messaging and search — the three tiers the robustness round adds

### 6.1 Caching (`ADR-011`)

Two layers, and the second one is new. L1 stays in-process per pod; L2 is the shared Valkey tier.

| Cache | Technology | Where | TTL | On miss |
|---|---|---|---|---|
| Configuration resolution (`S-21`) | **L1** in-process Caffeine → **L2** Valkey | Each service | Resolution TTL from `#19`, same TTL at both layers | **Fail closed** if the store is unreachable and both layers are expired. An L2 hit is not a licence to serve past TTL |
| Product catalogue reads | L1 → L2 | `#8` and callers | Short (minutes) | Read Aurora; breaker on `#8` |
| CBS identity snapshot | **In-process / row in `customer` schema** | `#4` | Freshness window | **No unbounded stale fallback** (`S-05`). Not in the shared tier — a bank identity snapshot is not a cache entry other pods should serve |
| BFF session vault | **Valkey** (`ADR-011`, closes the open decision) | `edge` | Session TTL | Re-authenticate. Survives pod restart, which the in-process alternative never did |
| Rate limit + OTP attempt counters | **Valkey** | `edge` | Window | Per-principal, not per-pod. The per-pod version was a control with a documented bypass |
| PDP decision | **Request-scoped only.** Not in Valkey | — | — | 300 ms fail closed. A stale allow is worse than a deny, and a shared cache makes the staleness longer |
| Idempotency | **Owning service store** (Aurora or DynamoDB) — **never the cache** | — | 24 h | — |
| Flutter / CDN | Cloudflare (SaaS) in front of `nip-web` in `ns:edge` | Edge | Cache-Control from the UI pod; **no** caching of authenticated JSON | — |

**The forbidden list is the load-bearing half of this section.** The cache is never a system of
record. It never holds idempotency, consent, suitability or audit data. It never serves
configuration past TTL because the store is down. It carries no PII beyond the session's principal
claims. Each service gets its own Valkey ACL user and key prefix — the same ownership rule
`ADR-008` applies to schemas — and `FF-23`/`FF-24` check the two that can be checked by machine.

If someone proposes widening it "because we will need it for Health", the answer is still no:
Health is R3, and the tier that exists is scoped to three uses.

### 6.2 Event backbone (`ADR-012`)

```text
service txn ─┬─► business tables          (one local transaction)
             └─► outbox row               ← THE SOURCE OF TRUTH
                     │
             outbox-publisher (Deployment ×2, jobs ns)
                     │
                   MSK topic ─┬─► #16 Audit consumer ─► DynamoDB + S3 WORM  ← THE RECORD
                              ├─► #17 Notification consumer
                              └─► compensation / reconciliation signals
```

| Property | R0 requirement |
|---|---|
| Brokers | 3, one per AZ, replication factor 3, `min.insync.replicas` 2 (§2.1) |
| Auth | SASL/IAM, **per-topic policy per consumer group**. A group cannot read a topic it was not granted |
| Encryption | TLS in transit, CMK at rest, private subnets only |
| Topics | Per domain, versioned (`platform.audit.v1`, `platform.notification.v1`, `platform.journey.v1`, `platform.payment.v1`, `platform.policy.v1`) plus a **DLQ per consumer group** |
| Partitioning | By `journeyId`, so per-journey ordering is a property of the topic rather than a hope. Partition count starts at the consumer group's pod count |
| Contracts | AWS Glue Schema Registry, backward compatibility enforced in CI (`FF-25`). A broker without a schema contract just moves the coupling into the payload |
| Consumers | **Idempotent on `eventId`, replay-tolerant by design rule.** Consumer offsets are not evidence of anything |
| Scaling | KEDA on consumer-group lag (§4.2). Not CPU |
| The rule that cannot be traded | **No regulatory evidence exists only in a topic.** Retention on a topic is an operational parameter; retention on evidence is a licence condition (`FF-26`) |
| Alert that matters | Outbox age (`NFR-DAT-05`), not broker health. A stalled publisher is the failure that loses time silently |

**Why both mechanisms.** The outbox solves the dual-write problem and the broker solves fan-out;
they are not alternatives. Removing the outbox would reintroduce "commit, then publish" — two
writes with no shared transaction, which loses an event on a crash and duplicates a business
change on a retry.

### 6.3 Operational search (`ADR-013`)

| Property | R0 requirement |
|---|---|
| Placement | **VPC-only** domain in private-data subnets. No public endpoint, ever |
| Ingest | Fluent Bit → Amazon Data Firehose → OpenSearch, with an S3 failed-delivery bucket |
| Indexed | Application logs (PII-masked at emission), Network Firewall alert/flow logs, VPC and TGW flow logs, MSK broker/consumer logs, **NLB and Envoy (Istio) access logs** (`ADR-023` S10) |
| Retention | ISM: 30 days hot → delete at `RET-OPERATIONAL` (90 days), with a disposal record (`NFR-DAT-07`) |
| Access | Fine-grained access control, IAM-mapped roles, human access audited |
| PII | Masked at emission (`FF-05`) **and** checked at the index (`FF-27`). A log pipeline is the most common way a restricted attribute reaches a store nobody classified |
| What it is **not** | Not the audit store (`FF-28`). Not a business search index — catalogue and journey queries stay in their owning stores. Not the analytics warehouse — that is S13 |

**The two-pipe rule survives this addition and is the reason it is approvable.** Operational:
CloudWatch + OpenSearch, 90 days, disposable. Regulatory: audit event store + S3 Object Lock,
7 years, immutable. Deleting an index deletes no evidence, and no gate is ever satisfied from a
search result.

---

## 7. End-to-end component view (what runs where)

```text
Z0 Internet
  NIP-APP native          (one APK on Play Store · one IPA on App Store)
  NIP-APP web             (browser · role-based views · nip-web in ns:edge)
  roles                   BANK_RM · INSURER_PARTNER_REP · BANK_EMPLOYEE (admin/ops)
  Customer device         (OTP SMS / PG hosted page) ──► AU Bank PG   [not our VPC]

Z1 Edge (public / DMZ + SaaS)
  Route 53 → Cloudflare (SaaS) → F5-XC (SaaS) → Ingress NLB (SG = F5/CF CIDRs)
  Istio Ingress Gateway (private-app): /* · /api/* · /callbacks/pg/* (PG IP allowlist)

Z2 Application (private-app subnets, EKS + Istio sidecars)
  edge:        nip-web, #2 NIP BFF  [both stateless, no PVC · nothing RM-named or admin-named]
  identity:    identity-provider-adapter, identity-authorization (PDP), keycloak  [Keycloak NOT on public Ingress]
  shared:      lead (#5), customer (#4), consent (#6), suitability (#7),
               catalogue (#8), journey (#9), payment (#12), policy (#13),
               audit (#16), notification (#17), configuration (#19)
  life-cell:   quotation (#10), proposal (#11)
  integration: integration-hub (#14), 1sb-integration-service (#15)
  jobs:        outbox-publisher, MSK consumers (audit, notification, #18 MIS),
               payment-reconcile (CronJob), issuance-recheck
  platform:    Istio (ingress + sidecars), Fluent Bit, ADOT collector, KEDA, admission controller

Z3 Network / inspection (the `network` account — §2.2, §2.3)
  Transit Gateway                     per-environment route tables
  inspection VPC × environment        Network Firewall endpoints, NAT + allowlisted EIPs
  VPN attachment                      always present
  Direct Connect + DX Gateway         primary once the circuit is accepted

Z4 Data (private-data subnets / AWS managed)
  Aurora cluster r0-platform          (schemas: §5.1)
  DynamoDB tables                     (§5.2)
  S3 WORM buckets                     (§5.3)
  ElastiCache for Valkey              sessions · L2 cache · rate limits   (§6.1)
  Amazon MSK                          3 brokers, outbox-fed               (§6.2)
  Amazon OpenSearch                   operational only, no evidence       (§6.3)
  KMS + Secrets Manager

Z5 Bank / provider (outside)
  Bank AD          ← WS-2 Phase 2, over the TGW (VPN → DX)
  CBS              ← #4 over the TGW (VPN → DX). Stubs in dev only
  AU Bank PG       ← #12 session + callback + settlement file
  1SilverBullet    ← #15 via the inspection VPC's NAT EIP, mTLS, uninspected payload
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

| Dependency | Direction | Protocol | Path and allowlist | Owner to confirm |
|---|---|---|---|---|
| 1SB APIs | Egress | HTTPS mTLS | **Apigee proxy**; 1SB allowlists **Apigee IPs** (`ADR-020`, §2.3). Adapter never calls a `*.1silverbullet.tech` origin. Firewall may inspect **pod → Apigee**; payload to 1SB is **not decrypted** | WS-1 / Shivanshi + Apigee team |
| AU Bank PG session | Egress | HTTPS | Via **Apigee** (outbound session-create). Do not publish spoke NAT EIPs as the PG allowlist for this hop | Payments + Shivanshi |
| AU Bank PG callback | Ingress | HTTPS | **PG source IPs only** on Istio `/callbacks/pg/*` + signature verify in `#12` (`ADR-023` S8). Not on the RM session VirtualService. Edge remains Cloudflare / F5-XC / NLB | Deepali + Payments |
| AU Bank PG settlement | Ingress or S3 drop | File | Separate from the API path | Aarti + Finance |
| CBS / CIF | Egress | Bank standard (often HTTPS or MQ) | **TGW → VPN, then DX** (`ADR-009`). Stubs in `dev` only | Bank network + `#4` |
| Bank AD | Egress | HTTPS to existing AD-verify API | **Apigee private target** (`ADR-020`). Never LDAP from EKS. `dev` may run Keycloak-local users | WS-2 + bank API platform |
| SMS/email gateway | Egress | HTTPS | Via inspection VPC; in the domain allowlist | Bank comms |

No inbound from 1SB in R0 (we poll). Do not open a public webhook "just in case".

---

## 9. Environments, CI/CD, secrets

| Env | Account | Data | Purpose |
|---|---|---|---|
| `dev` | **`uat` account**, `vpc-dev` | Synthetic only | Engineers. Isolated namespaces / schemas / Valkey prefix / MSK prefix / Apigee product. Render.com may remain as a **no-PII** preview alongside, never instead |
| `uat` | **`uat` account**, `vpc-uat` | Masked / synthetic; **no production CIF dumps** | Bank caller exercises quote + proposal (WS-1 gate 4.3). Real CBS/AD-verify — no stubs |
| `prod` | `prod` | Real ETB | Pilot branches only |

Promotion: **image built once** in `shared-services`, promoted by digest. Never rebuilt per env (`S09-E02-S02`).

CI: GitHub Actions (already in-repo) → ECR. CD: **Argo CD** in each cluster (recommended) or EKS-native. Shivanshi chooses; Architecture requires an audited, reversible deploy and a **UAT rollback drill** (`NFR-DR-05`).

Secrets: Secrets Manager → Secrets Store CSI → tmpfs. Rotation runbook exercised once. Emergency revocation path exists (`S09-E04-S05`).

---

## 10. Observability and audit (two pipes, never one)

| Pipe | Contents | Store | Retention |
|---|---|---|---|
| **Operational** | RED metrics, traces, application logs (PII-masked), **firewall / VPC / TGW flow logs, MSK and consumer logs** | AMP / X-Ray / CloudWatch **+ OpenSearch** (`ADR-013`) | `RET-OPERATIONAL` (90 days) then dispose with an audit record |
| **Regulatory audit** | Domain `AuditEvent`s, produced by the outbox and delivered over MSK (`S-17`, `S-23`, `S-24`) | DynamoDB + S3 Object Lock | `RET-7Y-IMMUTABLE` |
| **Cloud account audit** | CloudTrail, Config, GuardDuty | `security` account | Per bank policy; not the application audit store |

A journey cannot reach `SOLD` until the regulatory pipe has acknowledged the four required events (`INV-JRN-05`). Operational log loss is an SRE incident; audit-pipe loss is a **compliance** incident.

**Two additions from the robustness round, and one boundary restated.** OpenSearch joins the
operational pipe and makes it queryable — which is what the firewall, flow and broker logs are for
(`ADR-013`). MSK joins the regulatory pipe as **transport**, not as a store: the audit consumer's
write to DynamoDB and S3 is the record (`ADR-012`). The two pipes still never merge. A search
index with a delete policy and a topic with a retention window are both disposable; the evidence
store is not, and no gate is satisfied from either.

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
| **Bank connectivity** | **A TGW in `ap-south-2` with its own VPN attachment, provisioned now** (D16). A standby that cannot reach CBS or Bank AD cannot authorise a login or look up a customer, which makes it a standby for nothing | `NFR-DR-01`, `ADR-009` |
| **Cache / broker / search** | **Deliberately not replicated** (D13–D15). Sessions are re-established, events are replayed from the outbox, operational logs are not evidence | `ADR-011/012/013` |
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
| D10 | **NLB + Istio + Cloudflare/F5 origin re-point** | **No — part of the runbook** | The edge is re-pointed at the DR ingress NLB / Istio Gateway during failover. Document it as a step, not as automation | R0 posture |
| D11 | **DR runbook + measured exercise** | **Yes — the artefact is the deliverable** | Declaration → restore → verify → serve, wall-clock timed. `S09-G7` accepts the *record*, not the design | `NFR-DR-04`, `S09-G7` |
| D12 | **Rollback drill in UAT** | **Yes** | Not cross-region, but the same family of proof: a deliberately broken release rolled back, data intact, timed | `NFR-DR-05`, `S09-G4` |
| **D13** | **ElastiCache in DR** | **No — and this is a decision** | Sessions are re-established by re-authentication and the L2 cache is rebuilt on first miss. Replicating a cache to protect data that is by definition reconstructible is cost without recovery value | `ADR-011` |
| **D14** | **MSK in DR** | **No cluster, no Replicator** | A cluster is created at failover and **replayed from the outbox**, which lives in Aurora and is therefore already covered by D4. This is why `ADR-012` keeps the outbox: the broker is not a system of record, so it does not need a DR copy. The constraint it places on consumers — idempotent on `eventId`, replay-tolerant — is a design rule, and `NFR-EVT-03` proves it | `ADR-012` |
| **D15** | **OpenSearch in DR** | **No** | Operational logs are not evidence and are not needed to serve a journey. During a regional failover the operational pipe is CloudWatch in the DR region; the index is rebuilt from new logs, not restored | `ADR-013` |
| **D16** | **TGW + VPN attachment in `ap-south-2`** | **Yes — provisioned now, in the same change as D1** | The gap this closes: every earlier DR row assumed the standby could serve a journey, and a journey needs CIF and needs the PDP to authorise an RM who authenticates against Bank AD. Without a bank path in the DR region, the standby can start pods and answer nothing. VPN is enough here; a second DX order at failover is not a recovery plan | `NFR-DR-01`, `ADR-009` |

**Explicitly NOT DR in R0:** a second running EKS cluster · active-active traffic · cross-region
read routing · MSK Replicator (D14 — replay, not replicate) · a DR ElastiCache (D13) · a DR
OpenSearch domain (D15) · any resource outside India (`FF-08`, control `C6`).

**Three of the five new tiers are deliberately absent from the DR region, and that is the point.**
A tier is replicated when it holds something that cannot be reconstructed. Sessions, cached values
and operational logs can all be reconstructed; the outbox, the evidence store and the WORM archive
cannot, and those are exactly the rows that are `Yes`.

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

| Deployable | NS | Aurora schema | DDB | S3 | Cache (`ADR-011`) | Topics (`ADR-012`) | Outbound |
|---|---|---|---|---|---|---|---|
| nip-web | edge | — | — | — | — | — | static Flutter web only (image-baked) |
| nip-bff (#2) | edge | — | — | — | **sessions, rate limits** | — | PDP, all domain APIs |
| identity-provider-adapter | identity | — | — | — | — | — | Keycloak; **AD-verify via Apigee private** |
| identity-authorization | identity | `identity` | — | — | — | — | — |
| keycloak | identity | `keycloak` | — | — | — | — | Partners in IdP; **no LDAP to AD** |
| configuration | shared | `cfg` | — | — | L2 (read) | — | — |
| opportunity | shared | `opportunity` | — | — | L2 config | produces `journey.v1` | PDP |
| customer | shared | `customer` | — | — | L2 config | — | **CBS via Apigee private** |
| consent | shared | `consent` | — | — | L2 config | produces `journey.v1` | Notification (OTP) |
| suitability | shared | `suitability` | — | — | L2 config | produces `journey.v1` | Catalogue |
| catalogue | shared | `catalogue` | — | — | L2 catalogue | — | — |
| journey-orchestration | shared | optional | `journey-state` | — | L2 config | produces `journey.v1` | all domain refs |
| quotation | life-cell | `quotation` | `integration-jobs` | — | L2 config, catalogue | produces `journey.v1` | Hub |
| proposal | life-cell | `proposal` | `integration-jobs` | — | L2 config | produces `journey.v1` | Hub |
| payment | shared | `payment` | — | — | L2 config | produces `payment.v1` | **AU Bank PG session-create via Apigee** |
| policy | shared | `policy` | — | `docs` | L2 config | produces `policy.v1` | Hub |
| integration-hub | integration | `hub` | — | — | L2 config | — | Adapter only |
| 1sb-integration-service | integration | `onesb` via persistence svc | jobs | `raw` | — | — | **1SB via Apigee** (`ADR-020`) |
| bank-persistence-service | integration | `onesb`, audit ingest | — | — | — | — | Aurora only |
| audit | shared | optional | `audit-events` | `audit-archive` | — | **consumes `audit.v1`** | — |
| notification | shared | `notif` | — | — | — | **consumes `notification.v1`** | SMS/email gateway **via Apigee** |
| outbox-publisher | jobs | reads each service outbox | — | — | — | **produces every topic** | MSK only |

IAM: one IRSA role per deployable. No wildcard production policies (`FF-09`). Three additions from
the robustness round, all of them least-privilege in the same shape as the schema rule: one Valkey
ACL user per deployable with its own key prefix, one MSK IAM policy per deployable naming only the
topics in its row, and **no** deployable with write access to the OpenSearch domain.

Two rows are worth reading twice. `nip-bff` no longer holds a DynamoDB table — the
session vault moved to the cache tier (`ADR-011`), which is why `sessions-{env}` is withdrawn in
§5.2. `audit` **consumes** its topic and writes DynamoDB and S3: the consumption is transport and
the write is the record (`ADR-012`).

### 12.1 When — the provisioning sequence

The BOM says *what*. This says *in what order, who owns it, and what breaks if it is late*. Each
band is gated on the one above it; within a band, order is free. Bands **P0–P3** are the S09
critical path and no business service starts before them
([`03-solution-architecture-r0.md §3`](../platform/ws3-platform/03-solution-architecture-r0.md#3-r0-build-order--closing-s07-e01-s05): W0 is
"no services — pipeline, IaC, environments").

| Band | Provision | S09 story | Owner | First consumer | If it is late |
|---|---|---|---|---|---|
| **P0** Guardrails | Organizations + **5 accounts** (`shared-services`, `security`, `network`, **`uat`**, `prod` — **no separate `dev`, no CUG**; UAT account hosts `vpc-dev` + `vpc-uat`) · SCP region-pin to India · Terraform remote state + locking · `security` account (CloudTrail, Config, GuardDuty, Security Hub) · **KMS CMK hierarchy** · policy-as-code in the pipeline | `E01-S01/S02/S06/S07` · `E04-S03` | Shivanshi + Deepali | Everything | Every resource built before the region SCP has to be re-verified by hand for `S09-G9` residency attestation |
| **P1** Network | VPC × 3 envs · public / private-app / private-data / TGW-attachment × 3 AZs (§2.1) · **no IGW on the workload VPC** · **attach to existing `AU-CTO-NETWORK` TGW** + per-environment route tables · **inspection VPC × env with Network Firewall** (or share EDGE — `ASM-012`) · **NAT Gateway + Elastic IPs (now in the inspection VPC)** · **Site-to-Site VPN** · **attach to existing DX Gateway** · security groups · **VPC endpoints** · Route 53 private zone + Resolver endpoints · ACM certs · flow logs | `E01-S03` (network foundation) · `E07-S01` (segmentation) | Shivanshi + Deepali + bank network | P2 | **Still the longest external lead time, and now it has two external parties instead of one.** The EIP list is **not** published to 1SB (`ADR-020` — 1SB allowlists Apigee IPs; `DEP-20260914-apg`). The bank must terminate the VPN and accept our prefixes on the existing DXGW (`DEP-20260824-dx1`). Late here blocks W1 CBS lookups, W2 quotes and W3 payments regardless of code readiness |
| **P2** Compute | EKS × 3 envs, private endpoint · managed node groups (§2.1) · add-ons (VPC CNI, CoreDNS, kube-proxy, EBS CSI, ExternalDNS, Secrets Store CSI, **Istio**, **Fluent Bit, ADOT, KEDA**) · **Kyverno/Gatekeeper admission** · NetworkPolicy default-deny · Karpenter (thin, uat/prod) | `E01-S04` · `E07-S01/S03` | Shivanshi + Deepali | P4, P5 | Admission / mesh retro-fitted onto running workloads is a migration, not a control |
| **P3** Data & messaging | **One** Aurora cluster + schemas + per-schema roles · DynamoDB tables + PITR · S3 buckets + **Object Lock** + Block Public Access · **ElastiCache for Valkey + per-service ACL users** · **MSK 3 brokers + per-topic IAM + Glue Schema Registry** · AWS Backup plans · **`ap-south-2` replication (D1–D3, D6, D7, D16)** | `E01-S05` (data foundation) · `E06-S01/S03/S05` | Aarti + Shivanshi | W0b | Object Lock **cannot be applied retroactively** to objects already written. The broker and cache are needed at W0b–W1, not W3: `#19` resolves configuration through the L2 cache and the first journey emits audit events, so a "messaging comes later" plan means writing the audit path twice |
| **P4** Edge & proxy | **Ingress NLB** (or bank-standard ALB) · **Istio Ingress Gateway + mesh** · **Cloudflare + F5-XC SaaS** · F5 CIDR SG + shared-secret/mTLS (`ADR-023` S1–S3) · **Apigee outbound onboard** (`ADR-020` — not Terraform in our accounts) · Route 53 public zone · **Istio `/callbacks/pg/*` IP-allowlisted** · **no Amazon API Gateway** | `E07-S05` | Shivanshi + Deepali + bank platform (Istio) | W2 (Apigee/1SB) · W3 (callback) · W4 (RM traffic) | Apigee product onboard is on the **quote** critical path. The PG-callback route is needed at **W3**, earlier than the RM edge at W4. Treating "the edge" as one deliverable delays the money path by a wave |
| **P5** Identity (WS-2) | Keycloak on EKS + Aurora `keycloak`/`identity` schemas · **AD-verify via Apigee private** (never LDAP) · Fireframe / NIP-APP as the only UI chrome · Secrets Manager + rotation · **IRSA role per deployable** · Secrets Store CSI → tmpfs · **session vault on the P3 cache tier** | `E04-S01/S04/S06` | Deepali + WS-2 | W0b | The PDP fails closed by design (`S-02`). No identity means no service can authorise anything — this is not a "later" item |
| **P6** Observability & search | CloudWatch Logs/Metrics with PII masking · AMP + AMG · X-Ray *or* ADOT · **OpenSearch domain + Firehose + Fluent Bit + ISM policy** (`E05-S02` log aggregation — the story already existed; `ADR-013` decides what it aggregates *into*) · **audit pipe separated from the operational pipe** (`E05-S06`) · baseline dashboards + alert routing | `E05-S01…S06` | Shivanshi | W1 | Debugging the first end-to-end journey without correlated traces is where schedules are actually lost. The firewall, flow and broker logs from P1 and P3 are unqueryable until this band lands, which is most of why `ADR-013` is in R0 |
| **P7** Delivery & IaC | ECR + immutable tags + scan-on-push · GitLab CI/CD → ECR · **GitLab Runner** · **Terraform IaC** · promote-by-digest · migration job in the deploy path | `E02-S01…S06` · `E03-S01…S06` | Shivanshi + Amit | W0b | Rebuilding per environment breaks `S09-E02-S02` and makes every UAT result unattributable |
| **P8** Proof & Automation | **Restore executed and timed** · **Ansible rollback drill in UAT** · secret rotation exercised once · deletion-refusal test on a locked object · residency enumeration · **Ansible DX→VPN failover timed** (`NFR-NET-01`) · **Ansible outbox replay drill** (`NFR-EVT-03`) · **cache failover with sessions held** (`NFR-CAC-02`) · **automated DR failover drill** (`NFR-DR-04`, `S09-G7`) | `E06-S04` · `E03-S03` · `E04-S04` | Shivanshi + Aarti + Shailja | `GATE-S09` | `S09-G4`, `S09-G7`, `S09-G8`, `S09-G9` accept **records**, not designs. Ansible playbooks automate DR exercises, network drills, and post-deployment sanity validation |

**No new S09 story IDs were minted for the five new layers, deliberately.** Every one of them lands
inside a story that already exists in the S09 stage definition: hybrid connectivity and the
inspection VPC are the *network foundation* (`E01-S03`) and *network segmentation* (`E07-S01`); the
cache and the broker are the *data foundation* (`E01-S05`); the search domain is *log aggregation*
(`E05-S02`). What the ADRs change is **what those stories build**, not how many there are — and the
S09 backlog is generated from the stage files, so inventing an ID here would have produced a
reference that resolves to nothing.

**What each build wave needs to already exist**, so the platform request can be sequenced against
the service backlog rather than delivered as one lump:

| Wave | Services | Platform preconditions |
|---|---|---|
| **W0b** | `#19` Configuration | P0 · P1 · P2 · P3 (incl. **cache tier** — `#19` resolves through L2) · P5 · P7 |
| **W1** | `#5` `#9` `#14` `#4` `#8` | + **CBS reachable over the TGW** (VPN is sufficient; `#4` cannot be evidenced against a stub outside `dev`) · **MSK topics + schema registry** (the first journey emits events) · P6 |
| **W2** | `#6` `#7` `#10` | + **Apigee product onboarded; 1SB allowlists Apigee IPs** (`ADR-020`, §2.3) · firewall domain allowlist carries **Apigee** · S3 `raw` bucket locked |
| **W3** | `#11` `#12` `#13` `#16` | + **Istio `/callbacks/pg/*` + PG IP allowlist** · PG settlement drop path · S3 `docs` + `audit-archive` locked · DR replication live (D3) · **audit consumer group + DLQ** |
| **W4** | `#2` NIP BFF · `nip-web` · NIP-APP APK (Play) · NIP-APP IPA (App Store) · `#17` · `#18` MIS consumers | + Cloudflare + F5-XC (SaaS) + ingress NLB + Istio path rules (`GET /*` → `nip-web`, `/api/*` → `#2`) · **session vault on the cache tier** · SMS/email gateway in the firewall allowlist · **no** second hostname · **no** Amazon API Gateway · Keycloak not on public Ingress |

**The two items with an external lead time are P1's EIP publication and the bank-side
connectivity work.** Both depend on parties outside this programme, and the robustness round made
the second one bigger rather than smaller: the *pattern* is now decided (`ADR-009`, so it is no
longer an open decision), but the bank must still terminate the VPN, publish prefixes, open its
firewall and accept a DX order. Start both first. They remain the only things on this list that
cannot be accelerated by working harder.

---

## 13. Copy-paste requirement statement for the AWS platform team

The following block is intended to be pasted into an infrastructure request / CR.

```text
R0 AWS PLATFORM REQUEST — AU Bank Insurance Distribution Platform
Region: ap-south-1 (Mumbai). DR: ap-south-2 (Hyderabad) replicas only.
Accounts: shared-services, security, network, uat, prod.   <-- 5, `network` added 2026-08-24; `dev` is a VPC inside `uat` (ADR-020); no CUG

NETWORK
- 1 workload VPC per env, 3 AZs: public/DMZ (ingress NLB only — ADR-023) / private-app /
  private-data / TGW-attachment. Private-app/data: NO local NAT. Egress via TGW.
- ATTACH to the existing AU-CTO-NETWORK Transit Gateway. Do NOT provision a second TGW.
  ONE ROUTE TABLE PER ENVIRONMENT plus one for DR. No VPC peering.
- Inspection/egress VPC PER ENVIRONMENT in `network` (or share the existing EDGE VPC —
  ASM-012): AWS Network Firewall endpoint per AZ, NAT + allowlisted EIPs, the only IGW
- Workload VPC default route = TGW. NOT a local NAT. NOT a local IGW. 100% of egress is inspected
- Site-to-Site VPN FIRST. Direct Connect uses the EXISTING DX Gateway; VPN stays as standby
- CBS/CIF and AD-verify are reachable via Apigee **private** targets (ADR-020). Stubs permitted in dev ONLY. Never LDAP from EKS. Never hairpin Cloudflare/F5
- VPC endpoints: S3, DynamoDB, Secrets Manager, ECR, STS, CloudWatch Logs
- Route 53 Resolver inbound/outbound endpoints for bank zones
- Public/DMZ subnets: ingress NLB only (ADR-023). No public Service on domain pods.
  No public database, broker or search endpoint. Keycloak never on public Istio routes.
- Do NOT copy the existing banking app's Public VPC + IGW + Public ALB + VPC-peering as the whole front door
- DRAW Apigee on the **egress** path only (ADR-020). Do NOT put Apigee on the RM/mobile front door.
  First AWS inbound hop is the ingress NLB → Istio Ingress (ADR-023). Do NOT provision Amazon API Gateway.
  Do NOT Terraform Apigee in our accounts.

AVAILABILITY ZONES  (full table: LLD §2.1)
- Subnets, interface VPC endpoints, TGW attachments, ingress NLB and Istio Gateway: all 3 AZs, every env
- Paid capacity (EKS nodes, NAT, firewall endpoints, Aurora reader, cache replica):
  >= 2 AZs in uat, 3 in prod, 1 in dev
- Aurora reader MUST be in a different AZ from the writer - assert this in IaC
- MSK: 3 brokers, ONE PER AZ, RF 3, min.insync.replicas 2. OpenSearch: 3 dedicated masters
  across 3 AZs in uat/prod. These two are quorum services - 3 is not optional there
- Sale-path pods: minReplicas 2 + topologySpreadConstraints over topology.kubernetes.io/zone + PDB minAvailable 1
- Pin AZ IDs (aps1-azN), NOT AZ names - a name maps to a different physical AZ per account

EDGE (SaaS perimeter + NLB + Istio — ADR-023)
- Cloudflare Enterprise (SaaS) + F5-XC (SaaS WAF) + Route 53 + internet-facing NLB
- Cloudflare and F5-XC are NOT AWS and NOT in any VPC
- NLB Security Group = F5-XC (+ Cloudflare if required) egress CIDRs ONLY — never 0.0.0.0/0
- F5→origin MUST present shared-secret header AND/OR mTLS
- Istio Ingress Gateway does path split: GET /* → nip-web; /api/* → #2 NIP BFF;
  /callbacks/pg/* → Payment (#12) with PG IP allowlist + signature verify
- ONE public hostname. No admin.{env}
- ns:edge holds nip-web + #2 NIP BFF only — nothing RM-named or admin-named (ADR-015)
- Network Firewall is on the EGRESS path (pod → Apigee) - it is not a third inbound proxy
- DRAW Apigee outbound (ADR-020). Flutter never calls Apigee or Keycloak
- Do NOT provision Amazon API Gateway, VPC Link, Internal ALB-as-path-router,
  Kong/Nginx Plus, or an in-VPC F5 BIG-IP
- Istio IS in R0 (Ingress + sidecars; PeerAuthentication → STRICT after dated cutover)
- Retain NetworkPolicy alongside mesh mTLS (S11). Mesh authZ ≠ insurance PDP (S7)

COMPUTE
- 1 private EKS cluster per env; sale-path min 2 pods, 2 AZs, PDBs
- Namespaces: edge, identity, shared-platform, life-cell, integration, jobs, platform
- Stateless workloads: NO PersistentVolumeClaims for business services
- Keycloak uses Aurora, not a PVC; Keycloak NOT exposed on public Ingress
- Add-ons incl. Istio, Fluent Bit, ADOT, KEDA (KEDA on MSK consumer lag ONLY, never CPU)
- Do NOT run Kafka, Redis/Valkey, OpenSearch or Prometheus as StatefulSets here

DATA · CACHE · MESSAGING · SEARCH
- ONE Aurora PostgreSQL cluster, Multi-AZ, schema-per-bounded-context, no cross-schema grants
- DynamoDB: journey-state, integration-jobs, audit-events (PITR, CMK). `sessions` WITHDRAWN
- S3 buckets with Object Lock Compliance 7y + CRR to ap-south-2 for raw, docs, audit-archive
- ElastiCache for Valkey, cluster mode DISABLED, primary+replica across 2 AZs, failover ON,
  CMK + TLS, ONE ACL USER PER SERVICE with a key prefix.
  Permitted: BFF sessions, L2 config/catalogue cache, rate-limit counters.
  FORBIDDEN: idempotency, any system of record, serving config past TTL, PII
- Amazon MSK, 3 brokers, KRaft, TLS, CMK, SASL/IAM with PER-TOPIC policy, DLQ per consumer
  group, AWS Glue SCHEMA REGISTRY (backward compatibility enforced in CI).
  The TRANSACTIONAL OUTBOX STAYS and remains the source of truth - the topic is transport
- Amazon OpenSearch, VPC-ONLY, dedicated masters + data nodes, CMK, node-to-node encryption,
  fine-grained access control, ISM 30d hot -> delete at 90d.
  OPERATIONAL ONLY: it holds NO regulatory evidence and no gate is satisfied from it
- KMS CMK hierarchy + Secrets Manager (rotation to be exercised)
- Do NOT provision per-service RDS instances
- Do NOT provision Glue ETL/Athena/Redshift/QuickSight (schema registry only)
- Do NOT provision MSK Replicator (DR is replay from the outbox - see D14)

IDENTITY
- Keycloak private + existing WS-2 adapter/PDP/BFF pattern
- Session vault on the Valkey tier (closes the DynamoDB-vs-Redis question)
- IRSA everywhere; no static keys in pods

OPS
- CloudWatch + AMP/AMG + tracing (X-Ray or ADOT) + OpenSearch for correlation
- Firewall alert/flow, VPC flow, TGW flow, MSK and access logs all indexed
- CloudTrail/Config/GuardDuty/Security Hub in security account
- AWS Backup; a restore MUST be executed and timed in UAT before prod
- Terraform, remote encrypted state, no console-created prod resources
- Policy-as-code: fail any resource outside India regions; fail public/unencrypted stores;
  fail wildcard prod IAM; fail any default route that is not the TGW

DISASTER RECOVERY - ap-south-2, warm standby  (full table: LLD §11.1)
- Provision NOW, in the same change as the ap-south-1 primaries:
  empty VPC + subnets, ECR replication, S3 replica buckets with Object Lock 7y,
  KMS replica keys, Secrets Manager replica secrets,
  TGW + VPN attachment in ap-south-2 (D16 - a standby that cannot reach CBS or AD
  can start pods and answer nothing)
- Aurora DR: Aurora Global secondary OR AWS Backup cross-region copy - Aarti chooses.
  The constraint is that RTO <= 1h is MEASURED, not asserted
- DynamoDB PITR mandatory; cross-region global tables are a cost decision
- EKS in DR: NOT running. Created at failover or node groups at desired-count 0
- Route 53 failover and edge origin re-point are MANUAL runbook steps in R0
- Deliverable is the timed DR exercise record (S09-G7), not the design
- Do NOT provision: second running cluster, active-active, cross-region read routing,
  MSK Replicator (D14 - replay from the outbox), DR ElastiCache (D13 - sessions are
  re-established), DR OpenSearch (D15 - logs are not evidence), anything outside India

WHEN - provisioning sequence  (full table: LLD §12.1)
P0 guardrails (5 accounts — uat hosts vpc-dev + vpc-uat, no CUG; region SCP, TF state, security account, KMS hierarchy)
P1 network (VPC, 3 AZs, TGW + per-env route tables, inspection VPC + Network Firewall,
   NAT + EIPs NOT published to 1SB, VPN now + DX ordered, endpoints, ACM)   <-- START FIRST
P2 compute (EKS, node groups, add-ons incl. Fluent Bit/ADOT/KEDA, admission, NetworkPolicy)
P3 data & messaging (Aurora, DynamoDB, S3 + Object Lock, ElastiCache, MSK + schema registry,
   AWS Backup, ap-south-2 replication)
P4 edge (ingress NLB + Istio, Cloudflare + F5-XC SaaS, Apigee outbound onboard;
   PG-callback Istio route needed at W3, before W4; no Amazon API Gateway)
P5 identity (Keycloak private — NOT on public Ingress; AD-verify via Apigee private, never LDAP,
   Secrets Manager, IRSA, Secrets Store CSI, session vault on the cache; BFF token-hiding stands)
P6 observability & search (CloudWatch, AMP/AMG, tracing, OpenSearch + Firehose + ISM,
   separated audit pipe)
P7 delivery (ECR, GitOps, promote-by-digest, migration job)
P8 proof (restore timed, rollback drill, rotation exercised, Object Lock deletion refused,
   residency enumerated, DX->VPN failover timed, outbox replay drill, cache failover)

Two items have an external lead time and cannot be accelerated internally:
  1. Apigee product onboard + Apigee egress IP list to 1SB (ADR-020, DEP-20260914-apg).
     Do NOT publish inspection-VPC NAT EIPs to 1SB
  2. the bank side of the connectivity: VPN termination, prefixes, firewall change, DX order
     (the PATTERN is decided by ADR-009; the bank's own work is not)

PER-ENVIRONMENT SHAPES  (full table: LLD §1.4)
dev is deliberately NOT production-shaped: single-node cache, MSK Serverless or 1 broker,
1 OpenSearch data node, 1 firewall endpoint in alert mode, VPN only, stubs allowed.
Building production three times is the cheapest way to make this set unaffordable.

OUT OF SCOPE FOR THIS REQUEST
Customer DIY stack, insurer webhook ingress, Amazon API Gateway / VPC Link,
Internal ALB-as-path-router, AWS App Mesh, per-service database clusters,
ElastiCache-as-idempotency, OpenSearch-as-audit-store, MSK Replicator, third-party NGFW
appliance, self-managed Kafka/Redis/ELK, reporting warehouse (Glue ETL/Athena/
Redshift/QuickSight), a PVC for nip-web, a second admin/ops Flutter app, admin.{env},
RM-named or admin-named pods in ns:edge, public Keycloak routes, Render.com as a data path.
(Istio Ingress + sidecars ARE in scope per ADR-023 — do not omit them.)
```

---

## 14. What this LLD does not decide (and must not be inferred)

| Item | Owner | When |
|---|---|---|
| Exact instance classes, Aurora `max_connections`, Karpenter limits, broker/cache/search node sizes | Shivanshi + Aarti | S09 design review, validated S12 |
| ~~Direct Connect vs VPN vs bank proxy for CBS/AD~~ | **CLOSED 2026-08-24 by `ADR-009`** — VPN now, DX primary when the circuit lands, both over one TGW. What remains is the bank's own side of it (`DEP-20260824-dx1`) | — |
| ~~Session store = DynamoDB vs small Redis~~ | **CLOSED 2026-08-24 by `ADR-011`** — ElastiCache for Valkey, matching WS-2's accepted design | — |
| Firewall rule-set ownership and the alert→drop date for managed IPS | Deepali + Shivanshi | Before prod, per `ADR-010` |
| MSK partition counts, retention windows and topic-to-consumer IAM matrix | Shivanshi + Deepali | S09-E01, before W1 emits its first event |
| Whether the bank's enterprise SIEM/ELK supersedes the OpenSearch domain | Deepali + Shivanshi + bank security operations | Revisit trigger in `ADR-013`, not an R0 blocker |
| Aurora Global vs backup-restore for DR | Aarti + Shivanshi | Must still meet RTO ≤ 1 h **measured** |
| Argo CD vs alternative GitOps | Shivanshi + Amit | S09-E03 |
| ~~Flutter hosting (internal MDM vs public store)~~ | **CLOSED 2026-08-25 by `ADR-015`** — one web artefact on EKS (`nip-web`), one APK on Play Store, one IPA on App Store. Deepali still owns store **hardening** (pinning, attestation, no tokens on device — `S-01`) | — |
| **Cost envelope, now materially larger** | Shivanshi + Kalpana | S09 output, not an architecture invention. `RISK-012` is open against it, and §1.4 exists to stop the answer being "three times production" |

---

## 15. Sign-off required before this pack is used as S09 input

| Persona | Question they answer |
|---|---|
| **Mahesh / Architecture** (human T4) | Is this the R0 estate, and only the R0 estate? |
| **Deepali / Security** (human) | Trust boundaries TB-1…**TB-7** realised; no public data; KMS/IAM acceptable. **Plus `ADR-010` acceptance** — she owns the egress control, the alert-mode interim and the mTLS inspection exemption |
| **Aarti / Database** | One-cluster topology acceptable; schema isolation and restore design acceptable. **Plus the cache/store boundary and cache sizing (`ADR-011`)** |
| **Shivanshi / SRE** | Operable, observable, restorable; the EIP list and the bank path identified. **Plus the three new stateful tiers, their upgrade paths and the on-call surface she now carries (`RISK-014`)** |
| **Shailja / Compliance** | Residency + 7-year WORM + audit/operational pipe split permissible. **Plus the two evidence exclusions**: no evidence exists only in a topic (`ADR-012`), and OpenSearch holds none (`ADR-013`) |
| **Amit / Engineering** | Service packaging and IRSA mapping implementable. **Plus the L1/L2 cache port and the publisher/consumer shapes as shared libraries, written once** |
| **Kalpana / Delivery** | S08/S09 critical path sequenced; this request is on it. **Plus the external dependency `DEP-20260824-dx1` and the cost envelope `RISK-012`** |

Until those signatures exist, platform engineers may **draft** Terraform modules against this file in `dev` with synthetic data only.

**Nothing in the 2026-08-24 round is more provisionable than the rest of this file because it is
newer.** Five ADRs were written and five layers were designed; no approval was created by writing
them. `RISK-012` (cost envelope) and `RISK-014` (operational surface against S08 maturity) are open
precisely so that this set is decided by the people who carry it.

---

**Signed:** Mahesh — Principal Insurance Platform Architect (Board 1), AI-drafted
**signature_status:** `AI-DRAFTED — mandatory human T4 Architecture sign-off outstanding; Security, Database, SRE and Compliance reviews outstanding, and ADR-009 … ADR-013 add named approvals that are not notifications`
**Companion HLD:** [`R0-HLD.md`](./R0-HLD.md)
**Decisions:** [`ADR-009` … `ADR-013`](../platform/architecture-review/08-architecture-decision-log.md) · [`CR-012`](../governance/change-requests/CR-012-r0-platform-robustness.md)
**Diagrams:** [`r0-lld.svg`](./r0-lld.svg) · [`r0-platform-*.png`](./diagrams/README.md) · [`r0-reference-architecture.svg`](./r0-reference-architecture.svg)
