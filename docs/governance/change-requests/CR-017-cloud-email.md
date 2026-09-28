# Email to Cloud / infra — UAT AWS account (copy and send)

**Use:** paste into Outlook / the bank Cloud ticket.  
**SSOT:** [`R0-LLD.md`](../../architecture/R0-LLD.md) · inventory: [`CR-017-uat-aws-account-provision.md`](./CR-017-uat-aws-account-provision.md)

---

**To:** AWS platform / Cloud team (`@psg.cloud`)  
**Cc:** Shivanshi (SRE), Mahesh (Architecture), Deepali (Security)  
**Subject:** UAT AWS account create — AU Bank Insurance platform (NIP R0) — `vpc-dev` + `vpc-uat`

---

Hello Cloud team,

Please create the **UAT AWS environment** for the AU Bank Insurance distribution platform (NIP / R0). This mail is the request: **one Control Tower account named `uat`**, in **Mumbai (`ap-south-1`)**, with two isolated VPCs in that same account. Please do **not** create a separate Dev account, a CUG account, or Production as part of this request.

Detail and SKUs are in the R0 LLD (bill of materials). This mail is the work order.

### 1. Account and region

- **Account:** Control Tower `uat` only  
- **Region:** `ap-south-1` (Mumbai)  
- **DR copies only:** `ap-south-2` (Hyderabad) — S3 Object Lock replicas, KMS replica keys, Secrets Manager replicas, empty DR VPC, TGW+VPN in DR. Not a second running cluster.  
- **Inside this account:** `vpc-dev` (engineers, synthetic data, CBS/AD stubs allowed) and `vpc-uat` (bank UAT, masked data, **real CBS/AD — no stubs**). Keep them apart with separate route tables, schemas, and prefixes.

### 2. Please create **in the UAT account**

**Network**

- Two workload VPCs, 3 AZs each  
- Subnets: public (reserved and empty), private-app, private-data, TGW-attachment  
- **No Internet Gateway and no NAT in these VPCs.** Default route = existing `AU-CTO-NETWORK` Transit Gateway (attach; do not build a second TGW)  
- VPC endpoints: S3, DynamoDB, Secrets Manager, ECR, STS, CloudWatch Logs  
- Route 53 private hosted zones + Resolver endpoints  
- ACM certificates (API Gateway public certs; internal ALB private certs)

**Inbound edge**

- Amazon API Gateway (inbound only) with VPC Link to an **internal** ALB  
- One hostname: `GET /*` → nip-web, `/api/*` → NIP BFF  
- Separate payment-gateway callback route, IP-allowlisted  
- **Do not create a public / external ALB**

**Compute**

- One private EKS cluster per VPC (Kubernetes 1.30+ or current platform version)  
- Namespaces: `edge`, `identity`, `shared-platform`, `life-cell`, `integration`, `jobs`, `platform`  
- Add-ons: AWS Load Balancer Controller, ExternalDNS, Secrets Store CSI, Fluent Bit, ADOT, KEDA (Kafka consumer lag only), Kyverno/Gatekeeper, NetworkPolicy default-deny  
- IRSA: one IAM role per deployable; no static keys in pods  
- No Kafka / Redis / OpenSearch / Prometheus as StatefulSets on this cluster  
- No PVC for business apps; Keycloak uses Aurora

**Data**

- Aurora PostgreSQL: **one cluster per environment**, database `insurance`, schema per bounded context (not one RDS per microservice)  
  - `vpc-dev`: single instance  
  - `vpc-uat`: writer + reader in **different** AZs  
- DynamoDB (PITR + CMK), per env: `journey-state`, `integration-jobs`, `idempotency`, `audit-events`  
  - Do **not** create a `sessions` table — sessions go to ElastiCache  
- S3, Block Public Access, per env:  
  - `raw`, `docs`, `audit-archive` — Object Lock **Compliance, 7 years**, replicate to `ap-south-2`  
  - `logs-archive` — Governance lock, no PII  
  - Terraform state does **not** belong in this account  

**Cache, messaging, search**

- ElastiCache for **Valkey**: BFF sessions, L2 cache, rate limits only. Never idempotency, never a system of record, never PII  
  - `vpc-dev`: 1 node  
  - `vpc-uat`: 2 nodes / 2 AZs, failover on  
- Amazon MSK + Glue **Schema Registry** (registry only — no Glue ETL)  
  - `vpc-dev`: MSK Serverless or 1 broker  
  - `vpc-uat`: 3 brokers, one per AZ  
  - No MSK Replicator  
- OpenSearch, **VPC-only**, operational logs only (not audit evidence) + Firehose  
- Optional thin SNS/SQS for worker wake-up (not an event bus)

**Keys and operations**

- KMS CMKs: data, logs, secrets, WORM — India only; replicate keys to `ap-south-2` in this same change  
- Secrets Manager for DB, 1SB, payment gateway, and IdP secrets; replicate to `ap-south-2`  
- CloudWatch Logs/Metrics (PII-scrubbed)  
- Amazon Managed Prometheus + Managed Grafana, and X-Ray **or** ADOT (SRE to confirm which tracing path)  
- AWS Backup, with a restore that can be run in UAT  

### 3. Please create **outside** this account (separate tickets; do not invent them inside `uat`)

| Where | What |
|---|---|
| `shared-services` | ECR (scan on push, replicate images to `ap-south-2`); Terraform remote state |
| `security` | CloudTrail, Config, GuardDuty, Security Hub |
| `network` | Inspection/egress VPC, AWS Network Firewall, NAT + Elastic IPs, the only IGW, Site-to-Site VPN, attach to existing Direct Connect Gateway |
| Org / Control Tower | SCPs pinning resources to India (`ap-south-1` / `ap-south-2` only) |

Cloudflare Enterprise, F5-XC WAF, Apigee, and GitLab are **bank products / SaaS**. Do not Terraform them in the UAT account. 1SB must allowlist **Apigee egress IPs**, not the inspection-VPC NAT EIPs.

### 4. Please do **not** create

- A sixth `dev` Control Tower account, or CUG  
- Production apply  
- Public ALB, public RDS, public EKS, public OpenSearch, or an IGW on a workload VPC  
- A second Transit Gateway or a second Direct Connect  
- Cognito, Istio/App Mesh, Glue ETL / Athena / Redshift / QuickSight  
- Self-managed Redis, Kafka, Prometheus, or ELK on EKS  
- ElastiCache used as idempotency or evidence; OpenSearch used as the audit store  

### 5. What I need back from you

1. Account ID for `uat`  
2. VPC IDs and CIDRs for `vpc-dev` and `vpc-uat`  
3. Private EKS API endpoints and internal ALB DNS names  
4. Aurora endpoints and the four DynamoDB table names per env  
5. S3 bucket names  
6. ElastiCache, MSK, and OpenSearch endpoints  
7. Confirmation that workload VPCs have no IGW/NAT and that NAT EIPs will **not** be sent to 1SB  

Happy to walk through the LLD on a call if useful. Please flag anything that needs a Cloud exception **before** you vend the account.

Thanks,  
[Your name]  
AU Bank Insurance platform (NIP)
