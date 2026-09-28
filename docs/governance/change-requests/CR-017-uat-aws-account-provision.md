# CR-017 — Authorize UAT AWS account-create and first S09 apply (`vpc-dev` + `vpc-uat`)

**Date:** 2026-09-28
**Type:** PLAN (with INFRA, SEC and COMP consequences)
**Raised by:** human request → recorded by agent (`SUG-20260928-inf`)
**Workstream:** WS-3 primary (S09 platform foundation) · WS-1 consumer (GATE-P4 criterion 4.3 needs a UAT path)
**Stage:** WS-3 S08 — Engineering Foundation, **S09 overlapped** · WS-1 L7 Hardening
**Decision:** **PENDING.** This CR is the Cloud / landing-zone request. It is **not** an approval to `terraform apply`, not a stage transition, and not a substitute for the human T4 signatures on [`R0-LLD.md` §15](../../architecture/R0-LLD.md#15-sign-off-required-before-this-pack-is-used-as-s09-input).
**Origin:** `SUG-20260928-inf`
**SSOT for SKUs, CIDRs, IAM and sequencing:** [`docs/architecture/R0-LLD.md`](../../architecture/R0-LLD.md) — this file extracts the **UAT-account slice**. If this CR and the LLD disagree, **the LLD wins**.
**Related:** [`CR-012`](./CR-012-r0-platform-robustness.md) (design of five platform layers — still PENDING RATIFICATION, **not** an approval to provision) · [`ADR-020`](../registers/DECISION-REGISTER.md) (dev-inside-UAT, no CUG, split API plane) · [`SUG-20260914-uat`](../registers/SUGGESTION-REGISTER.md#sug-20260914-uat--dev-inside-uat-no-cug-at-r0)

> **Freshness.** `CURRENT-STATE.yaml` `state_as_of` is 2026-09-13 (15 days old at raise). FreshnessCheck is WARN, not HALT. Stage, scope and objective are treated as unchanged. Kalpana / R12 should refresh `state_as_of` at the next Governance Sync.

---

```yaml
change_request:
  id: CR-017
  raised_by: "agent:cursor-grok"
  date: 2026-09-28
  type: PLAN
  current_position: >
    R0-LLD is the S09 AWS bill of materials. No Control Tower uat account has been
    vended in-repo; no Terraform/EKS/Aurora exists. CR-012 admitted the robustness
    layers into the design and explicitly is not an approval to provision.
    Dev lives inside the UAT account (ADR-020 / SUG-20260914-uat). GATE-P4 4.3
    (bank caller against UAT) and GATE-S09 cannot close without this account.
  proposed_change: >
    Authorize the bank Cloud / AWS platform team to vend the UAT Control Tower
    account and provision the UAT-account services listed in §3, with vpc-dev
    (synthetic) and vpc-uat (masked, real CBS/AD — no stubs). Production apply,
    a separate dev account, and a CUG account are out of this CR.
  driver: >
    External dependency + stage prerequisite. S09 cannot run without accounts.
    WS-1 GATE-P4 4.3 requires a UAT path. Account vending is the long-lead C-05
    onboarding pack (BOARD-1-REREVIEW C-05).
  evidence:
    - "R0-LLD §1.1 BOM #1 and §9 environment table"
    - "S09-E02-S01 provision dev, UAT and production from the same modules"
    - "GATE-P4 criterion 4.3 BLOCKED (DEP-001, DEP-002) — needs a UAT environment"
    - "ADR-020 / ASM-017 / ASM-018 — two VPCs in one UAT account; no CUG"
  impact:
    scope: "No journey, LOB, actor or bounded-context change"
    stage: "Does not move a gate date by itself; unblocks S09 apply and the UAT half of GATE-P4 4.3"
    dependencies: "Needs CR-012 design (already in LLD). Unblocks S09-E01/E02 and WS-1 4.3 environment half. Does not close DEP-002 (bank caller slot)."
    parked_items: "None unparked. TD-010 Redis idempotency stays parked — ADR-011 refuses it."
    effort: XL
    risk_if_rejected: "No UAT path; GATE-P4 4.3 and GATE-S09 stay blocked; Render.com remains the only runtime and cannot carry PII"
  alternatives_considered:
    - option: "do nothing — keep Render.com"
      consequence: "Standing constraint: Render is dev-preview only and never a PII path (ADR-001)"
    - option: "separate Control Tower dev account"
      consequence: "Rejected by ADR-020 / SUG-20260914-uat / ASM-017 — cost without isolation win"
    - option: "provision prod in the same CR"
      consequence: "Premature. R0-LLD §15 forbids first apply to a non-dev account without human T4. vpc-uat is the UAT slice, not prod."
  decision: PENDING
  approvers:
    - "Mahesh / Architecture — human T4: is this the R0 UAT estate and only that estate?"
    - "Deepali / Security — human: trust boundaries, KMS, IAM, no public data"
    - "Shailja / Compliance — human: residency + 7-year WORM on UAT stores that will hold regulated evidence"
    - "Shivanshi / SRE — operable, observable, restorable; owns the Cloud ticket"
    - "Aarti / Database — one-cluster topology and restore design for the UAT Aurora/DDB/S3 shape"
    - "Kalpana / Delivery — sequencing vs GATE-P4 and C-05 onboarding pack"
  decided_on: null
  conditions:
    - "Do not terraform apply until the approvers above have signed, except vpc-dev synthetic draft modules as already allowed by R0-LLD §15"
    - "Do not vend a sixth (dev) or seventh (CUG) account in this CR"
    - "Do not publish inspection-VPC NAT EIPs to 1SB — 1SB allowlists Apigee IPs (ADR-020)"
```

---

## 1. Current position

| Authority | What it says today |
|---|---|
| [`R0-LLD.md`](../../architecture/R0-LLD.md) header | S09 *requirements pack*. **Not** an approval to apply Terraform. Status `AI-DRAFTED`. |
| [`R0-LLD.md` §1.1 BOM #1](../../architecture/R0-LLD.md#11-provision-in-r0) | Five Control Tower accounts: `shared-services`, `security`, `network`, **`uat`**, `prod`. **No separate `dev` account. No CUG.** The UAT account hosts **two VPCs** (`vpc-dev`, `vpc-uat`). |
| [`R0-LLD.md` §9](../../architecture/R0-LLD.md#9-environments-cicd-secrets) | `dev` = UAT account / `vpc-dev` / synthetic. `uat` = UAT account / `vpc-uat` / masked, **real CBS/AD, no stubs**. `prod` is a different account. |
| [`CR-012` §8](./CR-012-r0-platform-robustness.md) | Robustness layers are in the design. **Nothing in CR-012 is an approval to provision.** |
| [`04-aws-infrastructure-architecture.md`](../../platform/architecture-review/04-aws-infrastructure-architecture.md) | Target-state estate. **Do not raise a landing-zone request from that file alone.** |
| [`S09-platform-foundation.md` §6](../../application-lifecycle-bible/stages/S09-platform-foundation.md#6-current-position-in-this-repository----missing) | IaC, EKS, environments, KMS, WORM, residency: **absent**. Runtime today is Render.com preview. |
| [`BOARD-1-REREVIEW` C-05](../../architecture/BOARD-1-REREVIEW-VIN003-ESTATE-2026-09-14.md) | Onboarding pack (RIA, diagram, cost sheet, hardening) **before account create**. Owner: Shivanshi. |
| WS-1 `GATE-P4` 4.3 | **BLOCKED** — bank caller against UAT (`DEP-001`, `DEP-002`). This CR supplies the environment, not the bank slot. |

Nothing in this repository creates AWS resources. Spring profiles `uat` / `prod` exist in config; the accounts they name do not.

---

## 2. Proposed change

Authorize Shivanshi (SRE) to raise the bank Cloud / `@psg.cloud` ticket that:

1. **Vends** the R0 `uat` Control Tower account in `ap-south-1` (Mumbai), with DR replicas only in `ap-south-2` (Hyderabad).
2. **Provisions**, from the same Terraform modules, **two isolated VPCs in that one account**: `vpc-dev` and `vpc-uat`, plus every AWS service in [§3](#3-what-to-create-in-the-uat-aws-account).
3. **Coordinates** the sibling-account and SaaS dependencies in [§4](#4-not-in-the-uat-account--required-for-uat-to-work) — those are separate tickets, listed so Cloud does not invent them inside `uat`.
4. **Does not** vend `prod`, a split `dev` account, or a CUG account in this CR ([§6](#6-do-not-create--out-of-this-cr)).

No bounded context, journey, actor, or gate criterion text is rewritten. Architecture is not re-decided: CR-012 / `ADR-008`…`ADR-020` stand.

**Two apply bands inside this CR** (from [`R0-LLD` §15](../../architecture/R0-LLD.md#15-sign-off-required-before-this-pack-is-used-as-s09-input)):

| Band | Where | When |
|---|---|---|
| **A — draft** | `vpc-dev`, synthetic data only | Architecture + SRE + Security may allow Terraform **draft/apply** against this file once C-05 exists |
| **B — UAT** | `vpc-uat`, masked data, real CBS/AD-verify, no stubs | Full human T4 (Architecture, Security, Compliance) + Database + SRE + Delivery **before** apply |

Agents do not apply, do not vend accounts, and do not sign.

---

## 3. What to create **in the UAT AWS account**

Hand this table to Cloud. Region: **`ap-south-1`**. Count: **two environment slices** (`dev` and `uat`) unless the row says otherwise. Starting SKUs are in [`R0-LLD` §1.1 / §1.4](../../architecture/R0-LLD.md#11-provision-in-r0) — Shivanshi confirms; do not size for throughput (~100 journey starts/hour BAU).

### 3.1 Network (workload VPCs — no Internet Gateway)

| AWS service | How many | UAT-account notes |
|---|---|---|
| **Amazon VPC** | 2 (`vpc-dev`, `vpc-uat`) | 3 AZs each. Tiers: public (**reserved, empty, no NAT, no workloads**), private-app, private-data, TGW-attachment. CIDR per LLD §2. **No IGW on either VPC.** Default route = Transit Gateway, not a local NAT. |
| **Subnets + route tables + NACLs + security groups + VPC flow logs** | per VPC × 3 AZs | One **route table per environment** so `vpc-dev` stubs cannot reach prod CBS prefixes (`ADR-009`). |
| **Transit Gateway attachment** | 2 (one per VPC) | **Attach** to the existing `AU-CTO-NETWORK` TGW. **Do not create a second TGW in this account.** |
| **Interface / gateway VPC endpoints** | per VPC | **S3, DynamoDB, Secrets Manager, ECR, STS, CloudWatch Logs.** Stop secrets and image pulls going via NAT. |
| **Amazon Route 53** private hosted zone | per env | Plus Resolver inbound/outbound endpoints for bank zones (LLD §8). Public zone may live in shared-services — Shivanshi confirms. |
| **AWS Certificate Manager** | per env | Public certs attach to **API Gateway**; private certs for the **internal ALB**. |

NAT Gateways, Elastic IPs, the only IGW, and AWS Network Firewall live in the **`network` account inspection VPC**, not here (`ADR-010`). See §4.

### 3.2 Edge (inbound only — no public ALB)

| AWS service | How many | UAT-account notes |
|---|---|---|
| **Amazon API Gateway** (REST or HTTP API) | per env | **Inbound only** (`ADR-018`, `ADR-020`). Request validation, throttling, payload inspection. **VPC Link → internal ALB.** Flutter never calls Apigee. Separate **PG-callback route**, IP-allowlisted. |
| **Application Load Balancer (internal)** | per env | The **only** ALB. AWS Load Balancer Controller. Path rules: `GET /*` → `nip-web`; `/api/*` → `#2` NIP BFF. **One hostname.** No `admin.{env}`. |
| **External / public ALB** | **0** | **WITHDRAWN (`ADR-018`). Do not provision.** |

Cloudflare Enterprise and F5-XC are **SaaS, not in this account** (§4.3).

### 3.3 Compute

| AWS service | How many | UAT-account notes |
|---|---|---|
| **Amazon EKS** | 2 clusters | Kubernetes 1.30+ (platform current). **Private API endpoint.** One cluster per VPC. Namespaces: `edge`, `identity`, `shared-platform`, `life-cell`, `integration`, `jobs`, `platform` (LLD §4.3). Sale-path **min 2 pods** in `vpc-uat`, PDBs, zone spread. |
| **EKS managed node groups** (+ thin **Karpenter** in `vpc-uat`) | per cluster | On-demand/reserved for transactional services. **No StatefulSets** for Kafka, Redis/Valkey, OpenSearch or Prometheus. **No PVC** for business services. Keycloak uses Aurora, not a PVC. |
| **Cluster add-ons** | per cluster | VPC CNI, CoreDNS, kube-proxy, EBS CSI (installed, unused by business apps), AWS LB Controller, ExternalDNS, Secrets Store CSI, **Fluent Bit**, **ADOT**, **KEDA** (consumer-lag **only**, never CPU), Kyverno/Gatekeeper, NetworkPolicy default-deny. |
| **IAM Roles for Service Accounts (IRSA)** | one role per deployable | No static keys in pods. No wildcard production-style policies. |

Images come from **ECR in `shared-services`**, replicated to `ap-south-2` — not a second registry in UAT.

### 3.4 Data

| AWS service | How many | UAT-account notes |
|---|---|---|
| **Amazon Aurora PostgreSQL** | 1 cluster **per env** (`r0-platform-{env}`) | **One cluster, schema per bounded context** (`ADR-008`). Database `insurance`. Schemas: `cfg`, `opportunity`, `customer`, `consent`, `suitability`, `catalogue`, `journey` *(if not Dynamo-only)*, `quotation`, `proposal`, `payment`, `policy`, `hub`, `onesb`, `audit` *(if relational mirror)*, `identity`, `keycloak`, `notif`. Per-schema owner; **no cross-schema `GRANT SELECT`**. Storage CMK. `vpc-dev`: single instance. `vpc-uat`: writer + reader, **reader in a different AZ** (assert in IaC). |
| **Amazon DynamoDB** | 4 tables × env | `journey-state-{env}`, `integration-jobs-{env}`, `idempotency-{env}`, `audit-events-{env}`. PITR **on**, CMK. **`sessions-{env}` WITHDRAWN** — sessions are ElastiCache (`ADR-011`). Audit role: **no `UpdateItem` / `DeleteItem`**. |
| **Amazon S3** + **Object Lock** | 4 app buckets × env | `aubank-ins-raw-{env}`, `aubank-ins-docs-{env}`, `aubank-ins-audit-archive-{env}`: **Object Lock Compliance, 7 years**, CRR to `ap-south-2`, Block Public Access. `aubank-ins-logs-archive-{env}`: Governance lock (not Compliance), **no PII**. Terraform state bucket is **`shared-services`**, not UAT. |
| **AWS Backup** | plans covering Aurora, DynamoDB, any EBS | Must support a **timed restore in UAT** before prod (`S09-G7`, `NFR-DR-02` RPO ≤ 5 min for transactional core). |

### 3.5 Cache, messaging, search (CR-012 layers — managed, not on EKS)

| AWS service | How many | UAT-account notes |
|---|---|---|
| **Amazon ElastiCache for Valkey** (`ADR-011`) | 1 replication group per env | Cluster mode **disabled**. CMK, TLS, **one ACL user per service** with a key prefix. **Permitted:** BFF sessions, L2 config/catalogue cache, rate-limit counters. **Forbidden:** idempotency, system of record, PII, serving config past TTL. `vpc-dev`: 1 node, no failover. `vpc-uat`: 2 nodes / 2 AZs, automatic failover **on**. |
| **Amazon MSK** (`ADR-012`) | 1 cluster per env | KRaft, TLS, CMK, **SASL/IAM per-topic**. Fed by the **transactional outbox** (source of truth — the topic is transport). `vpc-dev`: **MSK Serverless or 1 broker**. `vpc-uat`: **3 brokers, one per AZ**, `kafka.m7g.large` starting point, RF 3, `min.insync.replicas` 2. **No MSK Replicator** (DR is outbox replay). |
| **AWS Glue Schema Registry** | 1 | Event contracts only. **Not Glue ETL.** Backward compatibility enforced in CI. |
| **Amazon OpenSearch Service** (`ADR-013`) | 1 domain per env | **VPC-only.** CMK, node-to-node encryption, FGAC. **Operational logs only — no regulatory evidence, no gate satisfied from an index.** App namespaces **must not** write to it; Fluent Bit → Firehose. `vpc-dev`: 1 data node, no dedicated master, 7-day ISM. `vpc-uat`: 2 data + 3 dedicated masters, 30-day hot. |
| **Amazon Data Firehose** + failed-delivery S3 | per env | Ingest into OpenSearch. Mapping errors lose an index document, not a record. |
| **Amazon SNS + Amazon SQS** | thin, optional | Outbox worker wake-up / notification send queue. **Not** an event bus. Do not invent MSK because SQS exists. |

### 3.6 Keys, secrets, observability (UAT-account half)

| AWS service | How many | UAT-account notes |
|---|---|---|
| **AWS KMS** CMKs | data, logs, secrets, WORM — India only | Replica keys in `ap-south-2` in the **same change** (D6). Encrypted copies without the key are not recoverable. |
| **AWS Secrets Manager** | DB, 1SB, PG, IdP secrets per env | Secrets Store CSI → tmpfs. Rotation is an S09 drill, not a Cloud-create skip. Replica secrets in `ap-south-2` (D7). |
| **Amazon CloudWatch Logs + Metrics** | per env | PII-scrubbed. 90-day operational retention. **Not** the regulatory audit store. |
| **AWS X-Ray** *or* **ADOT → AMP** | one choice, Shivanshi picks | Traces must span BFF → services → Hub → adapter. |
| **Amazon Managed Service for Prometheus + Amazon Managed Grafana** | thin | Four dashboards in R0, not a platform rewrite. May be account-local or in `shared-services` — Shivanshi confirms. |

Account-level **CloudTrail / Config / GuardDuty / Security Hub** are in the **`security` account**, not duplicated as a second audit plane here.

### 3.7 Workloads Cloud will see on these clusters (not extra AWS products)

These run **on** the UAT EKS clusters. They are listed so namespaces, IRSA, Valkey ACL users and MSK topic IAM are created with the first apply, not bolted on later. Full matrix: [`R0-LLD` §12](../../architecture/R0-LLD.md#12-per-service-aws-resource-matrix).

| Namespace | Deployables |
|---|---|
| `edge` | `nip-web`, `#2` NIP BFF **only** — nothing RM-named or admin-named (`ADR-015`) |
| `identity` | `identity-provider-adapter`, `identity-authorization` (PDP), **Keycloak** (private IdP; Aurora `keycloak`/`identity`; **no LDAP to AD**) |
| `shared-platform` | configuration, opportunity, customer, consent, suitability, catalogue, journey-orchestration, payment, policy, audit, notification |
| `life-cell` | quotation, proposal |
| `integration` | integration-hub, `1sb-integration-service`, `bank-persistence-service` |
| `jobs` | outbox-publisher, MSK consumers, reconciliation CronJob, `#18` MIS consumers on the isolated read path |
| `platform` | Fluent Bit, ADOT, KEDA — no app OpenSearch writes |

Min pods = 2 in `vpc-uat`. Promotion: **image built once** in `shared-services`, promoted by digest. Never rebuilt per environment.

---

## 4. Not in the UAT account — required for UAT to work

Raise these as **sibling tickets**. Do not create them inside `uat` "to save a hop".

### 4.1 Other AWS accounts (same R0 five-account set)

| Account | Create / attach | Why UAT needs it |
|---|---|---|
| **`shared-services`** | Amazon **ECR** (immutable tags, scan-on-push, replicate to `ap-south-2`); Terraform **remote state** bucket `aubank-ins-tfstate-{account}`; GitLab runners / CI that push to ECR | Images and state must not live in the workload account |
| **`security`** | Org **CloudTrail** (mandatory governance trail), **AWS Config**, **GuardDuty**, **Security Hub** | Distinct from application `#16` audit. Do not stand up a second GuardDuty console in `uat` as the org aggregator |
| **`network`** | **Inspection / egress VPC per environment** with **AWS Network Firewall**, **NAT Gateway × AZ + fixed Elastic IPs**, the **only IGW**; spoke attach to existing **`AU-CTO-NETWORK` Transit Gateway**; **Site-to-Site VPN first**; **attach to existing Direct Connect Gateway** (do not order a second circuit) | Workload VPCs have **no NAT and no IGW**. 100% of egress is inspected (`ADR-010`). **Do not publish these NAT EIPs to 1SB.** |
| **`prod`** | **Do not vend or apply in this CR** | Separate account, later S09 band, after UAT restore/rollback drills exist |
| Org / management | **AWS Organizations / Control Tower** account vending; **SCPs pinning every resource to India** (`ap-south-1` / `ap-south-2` only) | `S09-G9` residency. Every resource built before the SCP has to be re-verified by hand |

### 4.2 DR in `ap-south-2` — same change as the primaries, not a follow-up

Provision **now** with the `ap-south-1` UAT primaries ([`R0-LLD` §11.1](../../architecture/R0-LLD.md#111-dr-bill-of-materials--what-actually-exists-in-ap-south-2)): empty DR VPC + subnets, ECR replication, S3 replica buckets with Object Lock 7y, KMS replica keys, Secrets Manager replica secrets, **TGW + VPN attachment in `ap-south-2` (D16)**. Aurora Global **or** Backup copy is Aarti's call; RTO ≤ 1 h must be **measured**.

**Do not** create in DR for R0: a second running EKS, DR ElastiCache, DR MSK / Replicator, DR OpenSearch, automatic Route 53 failover.

### 4.3 SaaS / bank products — **not** Terraform in our accounts

| Product | Role | Ticket |
|---|---|---|
| **Cloudflare Enterprise** | Edge CDN / DDoS in front of API **and** RM web | Bank perimeter. Origin = F5-XC → API Gateway. Logs stay in `ap-south-1`. Authenticated JSON is never cached. |
| **F5 Distributed Cloud (F5-XC) Advanced WAF** | L7 WAF, OWASP, bot, rate limits | SaaS. **Not** an F5 BIG-IP appliance in AWS (`ADR-018`). |
| **Apigee** (bank outbound API plane) | Every call that **leaves** the building: 1SB, SMS, CBS/CIF, AD-verify, AU Bank PG | Onboard NIP as an Apigee product (`DEP-20260914-apg`). Adapter base URL = Apigee proxy. Internal targets stay **private** (no Cloudflare/F5 hairpin). **1SB allowlists Apigee IPs.** |
| **GitLab CI/CD + Terraform + Ansible** | Delivery / IaC / DR drills | Bank standard (`ADR-016`). GitHub Actions may still build; CD is GitLab/GitOps. |
| **Fireframe / NIP-APP UI chrome** | The only login UI bank users see | Keycloak admin console is not shown to bank users (`ADR-020`). |

---

## 5. Per-environment shapes **inside this account**

From [`R0-LLD` §1.4](../../architecture/R0-LLD.md#14-per-environment-shapes--the-reason-this-set-is-affordable). Building production twice inside UAT is how this set becomes unaffordable.

| Layer | `vpc-dev` | `vpc-uat` |
|---|---|---|
| Data | Synthetic only. Render.com may remain as a **no-PII** preview **alongside**, never instead | Masked / synthetic. **No production CIF dumps** |
| Bank path | VPN only. CBS and AD **stubs permitted here and nowhere else** | VPN, then DX primary when the circuit lands. **Real CBS/AD test instances — no stubs** |
| Inspection (in `network`) | 1 firewall endpoint, 1 AZ, IPS **alert** | 2 endpoints, 2 AZs, IPS alert → drop before prod |
| Cache | 1 node, no failover | 2 nodes / 2 AZs, failover on |
| Broker | MSK Serverless or 1 broker | 3 brokers × 3 AZs |
| Search | 1 data node, 7-day ISM | 2 data + 3 master, 30-day hot |
| Aurora | Single instance | Writer + reader, 2 AZs |
| EKS paid capacity | 1 AZ acceptable | ≥ 2 AZs |

Isolation between the two VPCs: separate route tables, namespaces, schemas, Valkey key prefixes, MSK prefixes, Apigee products. That is the whole point of putting `dev` in this account without a sixth Control Tower account (`ASM-017`).

---

## 6. Do not create — out of this CR

Provisioning any of these is scope drift against [`R0-LLD` §1.3](../../architecture/R0-LLD.md#13-do-not-provision-in-r0).

| Do not create | Why |
|---|---|
| A **sixth Control Tower `dev` account** | `ADR-020` / `SUG-20260914-uat` |
| A **CUG** account or VPC | Waive, do not provision (`ASM-018`) |
| The **`prod` account apply** | Later S09 band; needs UAT restore + rollback evidence first |
| **Public ALB / public RDS / public EKS / public OpenSearch / IGW on a workload VPC** | Only API Gateway is public. Workload VPCs have no IGW |
| **Second Transit Gateway or second Direct Connect** | Attach to `AU-CTO-NETWORK` / existing DXGW |
| **Public VPC + IGW + VPC peering copied from the live banking app** | Spoke, do not clone (`BE-01`) |
| **Amazon Cognito** as R0 IdP | Keycloak behind the adapter |
| **Istio / AWS App Mesh** | NetworkPolicy + IRSA + in-app breakers. Mesh is S14 |
| **Glue ETL / Athena / Redshift / QuickSight** | Warehouse is S13. Schema Registry only |
| **MSK Replicator / MirrorMaker** | DR is outbox replay (`ADR-012` D14) |
| **ElastiCache as idempotency or evidence** | `ADR-011` refuses it. `TD-010` stays parked |
| **OpenSearch as audit / evidence** | `ADR-013` refuses it |
| **Self-managed Redis, Kafka, Prometheus or ELK on EKS** | Managed equivalents are in §3.5 |
| **Per-service Aurora clusters** | `ADR-008` — one cluster, schema per context |
| **Third-party NGFW appliance in our VPC** | FortiGate already sits on the bank EDGE; we attach (`ASM-012`) |
| **Insurer webhook / callback API Gateway** | R0 polls |
| **Customer DIY CloudFront** | R1 |
| **Apigee as an AWS resource we Terraform** | Bank product |
| **Render.com as a regulated-data path** | Preview only (`ADR-001`) |
| **Any resource outside `ap-south-1` except the §4.2 DR replicas in `ap-south-2`** | Control C6 |

---

## 7. Copy-paste for the bank Cloud / infra CR

The long form lives in [`R0-LLD` §13](../../architecture/R0-LLD.md#13-copy-paste-requirement-statement-for-the-aws-platform-team). This block is the **UAT-account ticket** — paste it, and attach the LLD.

```text
AU NIP — UAT AWS ACCOUNT CREATE (CR-017)
Region: ap-south-1 (Mumbai). DR replicas: ap-south-2 (Hyderabad) only.
Account: Control Tower "uat" only. TWO VPCs in this account: vpc-dev + vpc-uat.
Do NOT vend a separate dev account. Do NOT vend CUG. Do NOT apply prod in this ticket.

IN THIS ACCOUNT
- 2 workload VPCs, 3 AZs, no IGW, no local NAT. Default route = existing AU-CTO-NETWORK TGW.
  Subnets: public (empty/reserved) / private-app / private-data / TGW-attachment.
  VPC endpoints: S3, DynamoDB, Secrets Manager, ECR, STS, CloudWatch Logs.
  Route 53 private zones + Resolver endpoints. ACM certs.
- Amazon API Gateway (inbound only) + VPC Link + INTERNAL ALB only. NO public ALB.
  One hostname. GET /* → nip-web; /api/* → NIP BFF. Separate PG-callback route, IP-allowlisted.
- EKS × 2 (private endpoint). Namespaces: edge, identity, shared-platform, life-cell,
  integration, jobs, platform. Add-ons: LB Controller, ExternalDNS, Secrets Store CSI,
  Fluent Bit, ADOT, KEDA (consumer lag only), Kyverno/Gatekeeper, NetworkPolicy default-deny.
  No Kafka/Redis/OpenSearch/Prometheus StatefulSets. No PVC for business apps. Keycloak on Aurora.
  IRSA one role per deployable.
- Aurora PostgreSQL × env: ONE cluster, schema-per-context (ADR-008). vpc-dev single instance;
  vpc-uat writer+reader in different AZs.
- DynamoDB × env: journey-state, integration-jobs, idempotency, audit-events. PITR+CMK.
  sessions table WITHDRAWN (use ElastiCache).
- S3 × env: raw, docs, audit-archive (Object Lock Compliance 7y + CRR to ap-south-2);
  logs-archive (Governance lock, no PII). Block Public Access. No tfstate in this account.
- ElastiCache for Valkey × env (ADR-011): sessions, L2 cache, rate limits ONLY.
  Never idempotency / SoR / PII. vpc-dev 1 node; vpc-uat 2 AZ + failover.
- MSK × env (ADR-012) + Glue Schema Registry. Outbox stays source of truth. No Replicator.
  vpc-dev Serverless or 1 broker; vpc-uat 3 brokers / 3 AZ.
- OpenSearch VPC-only × env (ADR-013) + Firehose. Operational logs ONLY, not evidence.
- KMS CMKs (data/logs/secrets/WORM) + Secrets Manager. Replicate keys+secrets to ap-south-2 now.
- CloudWatch Logs/Metrics (PII-scrubbed). AMP/AMG + X-Ray or ADOT (Shivanshi picks).
- AWS Backup. A restore MUST be executable in UAT.

NOT IN THIS ACCOUNT (sibling tickets — do not invent them here)
- shared-services: ECR + Terraform state
- security: CloudTrail, Config, GuardDuty, Security Hub
- network: inspection VPC, Network Firewall, NAT+EIPs (NOT published to 1SB), only IGW,
  TGW hub (existing — attach only), Site-to-Site VPN, existing DX Gateway attach
- org SCPs: pin India regions
- Cloudflare, F5-XC, Apigee, GitLab: bank SaaS / products, not Terraform in uat
- 1SB allowlist = Apigee egress IPs, never spoke NAT EIPs (ADR-020)

SHAPES
vpc-dev = synthetic, stubs allowed, cheaper SKUs (LLD §1.4).
vpc-uat = masked, REAL CBS/AD, no stubs, HA SKUs.

OUT OF SCOPE
prod apply, CUG, split-dev account, public ALB, Cognito, mesh, warehouse,
MSK Replicator, cache-as-idempotency, OpenSearch-as-audit, second TGW/DX,
workload IGW, Render.com as a data path, anything outside India.
```

---

## 8. Stage fit, scope, necessity

| Lens | Code | Why |
|---|---|---|
| Stage | **SF1** on-stage for WS-3 (S08 with **S09 overlapped**; next stage is S09) | Account vending is `S09-E02-S01` / band P0–P3 in LLD §12.1. Not SF5: this **alters G1, G3, G4, G8, G10** (IAM, secrets/KMS, crypto ownership, network topology, regulator-evidenced controls) so the parallel-lane test fails item 6. |
| Scope | **SC0** explicit | WS-3 in-scope includes S09 platform foundation. Serves `S09-E01`, `S09-E02`, WS-1 `GATE-P4` 4.3 environment half. |
| Necessity | **MUST** | No UAT account ⇒ no UAT path ⇒ 4.3 and GATE-S09 cannot close. Render.com cannot carry PII (`ADR-001`). |
| Type | **INFRA** (also SEC, COMP) | Environments / runtime platform. |
| Risk tier | **T4** | G1/G3/G4/G8/G10 fire. RG-9 does **not** cap this: the delta *is* the control change. |
| Priority | **P2 now · P1 at S09 entry / UAT apply** | Score `2N+2S+2B+2R+D−E` = `8+6+6+4+2−3 = 23` (N=4 MUST, S=3 SF1, B=3 gate-blocking, R=2 material, D=2 account-vending lead time, E=3 XL). Matrix default SF1×MUST is P1–P2; this is P2 while S08 is still the named current phase and P1 when S09 apply is the in-flight item. **No O1–O8 override claimed** — GATE-P4 4.3 is also blocked by `DEP-002` (bank slot), so a missing account is necessary but not the sole in-flight blocker. |
| Action | **ESCALATE → CR-017 PENDING** | [`14-CHANGE_CONTROL`](../14-CHANGE_CONTROL.md) Rule CC-1: an agent may raise a CR; it may never approve one. Do not implement. |

---

## 9. Boards — drafts only, no signatures

Every row is **outstanding**. Listing a persona is the required-approver set, not a verdict.

| Board / role | Person | What they decide here |
|---|---|---|
| Board 1 — Architecture | Mahesh | Human **T4**: is this the R0 UAT estate and only that estate? |
| Board 4 — Security | Deepali | Human: TB-1…TB-7, no public data, KMS/IAM, `ADR-010` egress, no spoke EIP published to 1SB |
| Board 6 — Compliance | Shailja | Human: residency + 7-year WORM on `raw`/`docs`/`audit-archive`; OpenSearch holds no evidence; topics are not the record |
| Board 7 — SRE | Shivanshi | Owns the Cloud ticket, C-05 pack, SKUs, operability |
| Database | Aarti | One-cluster topology, restore design, cache/store boundary |
| R12 — Delivery | Kalpana | Sequence vs GATE-P4 / C-05; does **not** convert missing signatures into approval |
| R3 — Engineering | Amit | Notify — IRSA mapping / packaging |
| Board 3 — Product | Rajal | Notify — no journey change; interest is the UAT path for 4.3 |
| Board 5 — QA | Swapnali | Notify — UAT rollback drill becomes an evidence artefact (`S09-G4`) |

---

## 10. Preconditions and what this CR does not do

**Before Cloud vends:**

- C-05 onboarding pack: RIA ID, LLD + topology diagrams, this inventory, cost approval to `@psg.cloud`, Prisma/SCP hardening (`BOARD-1-REREVIEW` C-05).
- `ASM-017` (dev-inside-UAT) and `ASM-018` (no CUG) confirmed with Cloud, or a recorded exception.
- Human T4 on [`R0-LLD` §15](../../architecture/R0-LLD.md#15-sign-off-required-before-this-pack-is-used-as-s09-input) before **`vpc-uat` apply**. `vpc-dev` synthetic draft is already allowed by that section.

**This CR does not:**

- Approve CR-012, sign ADRs, or edit `current_phase` / `stage_status`.
- Close GATE-P4 4.3 (`DEP-002` bank caller slot remains Rajal's).
- Close TD-006 (Secrets Manager still a stub in application code until S09-E04 wires it).
- Close TD-010 (idempotency stays in the owning store).
- Onboard Apigee (`DEP-20260914-apg`) or terminate the bank VPN (`DEP-20260824-dx1`) — those stay external.
- Authorize production apply.

---

**Prepared by:** agent, drafting as Mahesh (structure) + Shivanshi (operability inventory) from `R0-LLD`
**signature_status:** `AI-DRAFTED — no board verdict recorded; mandatory human T4 Architecture, Security and Risk & Compliance sign-offs outstanding`
