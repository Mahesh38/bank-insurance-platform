# Source — AU Bank Enterprise Cloud Estate (Control Tower, Network, SOP)

Transcription of AU Small Finance Bank Control Tower, EDGE VPC FortiGate, Transit Gateway, Sify/Airtel Direct Connect, cloud onboarding and S3 hosting SOP artefacts supplied 2026-09-14 (`VIN-003`).

**Intake ID:** `VIN-003`
**Date received:** 2026-09-14
**Provided by:** Repository owner, for Mahesh — Principal Insurance Platform Architect
**Original medium:** Bank-internal cloud documents and architecture diagrams (internal circulation)
**Relationship to `VIN-001` / `VIN-002`:** orthogonal — this is the **existing AU Small Finance Bank AWS/OCI estate** that any insurance-platform design must attach to. It does not change product, journey or provider-aggregation doctrine.
**Status:** **REFERENCE — non-binding.** Transcribed for grounding under
[`09-target-state-architecture-doctrine.md §10`](../../context/roles/mahesh-principal-insurance-platform-architect/09-target-state-architecture-doctrine.md) rule `VI-01`.
**Reconciliation against accepted repository decisions:**
[`18-au-bank-enterprise-cloud-estate.md`](../../context/roles/mahesh-principal-insurance-platform-architect/18-au-bank-enterprise-cloud-estate.md)

> **Standing of this document.** Bank organisation standards, transcribed substantively from the
> supplied artefacts. Not approved insurance-platform scope, not an ADR, not a licence to clone
> the current banking application's VPC. Where a note conflicts with an accepted decision, the
> conflict is recorded in module `18` for the later architecture rereview — it is not silently
> absorbed (`VI-02`).
>
> Personal support-contact telephone numbers and named TAM emails from the SOP are **not**
> copied here. They are operational, not architectural.

---

## Sources transcribed

| # | Artefact | Version / date | Author / ownership as printed |
|---|---|---|---|
| S1 | *AU AWS Landing Zone* (PowerPoint, internal) | v1.1 · issue 10-July-2026 · annual review | Prepared Manish Salaria (AVP Cloud); reviewed Shivendu Gupta (VP – Cloud Architect); owned Shrish Kumar Yadav (VP – Datacentre Operations) |
| S2 | *Cloud Console Application Onboard* | v1.4 · issue 6 June 2026 | Author Atul Singh (AWS Cloud Admin); reviewed Manish Salaria (AVP Cloud Operations); owned Shrish Kumar Yadav (VP Datacentre Operations) |
| S3 | *Cloud Standard Operating Procedure* | v1.7 · issue 30-Jun-26 · annual review | Author Anmol Chittora; reviewed Manish Salaria (AVP – Cloud Operations); owned Shrish Kumar Yadav (VP – Datacentre Operations) |
| S4 | *S3 Static Website Hosting Controls* | v1.1 · issue 09-July-26 | Author Naveen Gurjar; reviewed Manish Salaria; owned Shrish Kumar Yadav |
| D1 | *Central Network Account Architecture – V1* | diagram | Management account `AU_AWS_MAS`; EDGE VPC Mumbai + Hyderabad |
| D2 | Existing banking-application AWS landscape (Atul Singh / Manish Salaria / Shrish Kumar Yadav) | diagram v1.4 · 9-July-2026 · annual review | PUBLIC VPC DMZ + PVT VPC; Cloudflare → F5-XC → Public ALB |
| D3 | Existing production EKS application (dark canvas) | diagram | Cloudflare → F5; TGW + FortiGate + Direct Connect; GitLab / Terraform |

S2 v1.2 cites **RBI-20202-21/74 CSITE.SEC.No.1852/31.01.015/2020-21** as the regulatory driver for adding AWS and OCI information. S3 v1.2 carries the same citation; v1.5 added S3 static website hosting controls; v1.6 added AU Bank OCI tenant `aubankerp`.

---

## S1 — AU AWS Landing Zone

### Design principles (Control Tower)

- Zero Trust architecture for enhanced security posture.
- AWS Control Tower used for AU account provisioning.
- Instance isolation via AWS Security Groups.
- Logs from all AU accounts captured in a **central Audit account**.
- AWS Service Control Policies (SCPs) enforce organisation-wide access guidelines **to restrict India geo-location**.
- CSPM: **Prisma Cloud** (Palo Alto), enabled at organisation level.

### Control Tower structure (as drawn)

Management account **`AU_AWS_MAS`**. AWS Organizations with SCPs, account baselines and guardrails. AWS IAM Identity Center is **Central Access Management / SSO**.

Organisation layout as drawn:

- Prisma Cloud attached at the organisation.
- **Security OU** — Log Archive, Audit (and related security tooling).
- **Core OU**.
- **Customer / workload OUs** holding many spoke accounts.
- Named shared-function accounts under the landing zone include (as icons, not an exhaustive inventory): IAM Identity Center, CloudTrail, GuardDuty, Config, Security Hub, Network, Shared Services, plus workload accounts.

### Cloud landscape

- **Primary region:** AWS Mumbai (`ap-south-1`).
- **Secondary zone (AZ language on the slide):** Delhi / Kolkata zone — treated by Mahesh as *slide shorthand for multi-AZ inside Mumbai*, not as extra AWS regions. Binding DR region is Hyderabad.
- **Secondary region:** AWS Hyderabad (`ap-south-2`).
- **North-south ingress:** Cloudflare → F5 (F5-XC) → Public ALB.
- **Hybrid connectivity:** AWS Direct Connect with SD-WAN, with FortiGate firewall.

### Network connectivity across AWS accounts

- Direct Connect from **two partners** to DC/DR for AWS.
- FortiGate firewall deployed in the **AWS Network Account**.
- Integrated with **Transit Gateway** for connectivity across all sub-accounts.
- Traffic flow:
  - **North-South:** Cloudflare, F5 XC.
  - **East-West:** central FortiGate firewall.

### Security posture (slide)

- Prisma CSPM at organisation level.
- **AD for SSO**.
- Proxy control so no unauthorised cloud account not owned by the bank is accessed, and vice versa; control at IAM.
- FortiGate Layer 7 firewalls in Mumbai and Hyderabad; inspect and apply rules for north-south traffic via Cloudflare and F5 XC WAF.
- Central AD for DNS.
- Central CloudTrail pushing all account logs to SIEM.
- SCP control for India geo-location.
- Root account **caged access**.

### Top major services consumed (as listed)

- IaaS: EC2, EKS, EBS, EFS, S3, VPC.
- PaaS/SaaS: RDS MySQL managed DB, MongoDB, ELB (NLB and ALB), CloudTrail.
- Reference: bank *Cloud service qualification document* (not supplied in this intake).

---

## D1 — Central Network Account Architecture V1

**Title:** Central Network Account (EDGE VPC).

**Left rail — management:**

- Management account `AU_AWS_MAS`.
- AWS Control Tower → Organizations (Organization, SCPs, Account Baselines, Guardrails).
- AWS IAM Identity Center — Central Access Management SSO.

**Mumbai (`ap-south-1`):**

- Many **spoke accounts** attach via TGW attachments to **Transit Gateway Mumbai**.
- **EDGE VPC (Mumbai)** — private subnets AZ A and AZ B:
  - FortiGate NGFW **Active**
  - FortiGate NGFW **Passive**
  - HA link between them
- From FortiGate: internet egress, and to **Direct Connect Gateway**.
- Direct Connect (Active) to on-premises / datacentres: Back Office, DC, DR, NDR.

**Hyderabad (`ap-south-2`):** same spoke → TGW Hyderabad → EDGE VPC (FortiGate Active/Passive) pattern.

**Global Direct Connect (extended to Hyderabad region):**

- DX Primary Link — provider **Sify** — Active.
- DX Secondary Link — provider **Airtel** — Active.

**Legend:** east-west (cross-account); internet; on-prem DC/DR/NDR via Direct Connect; Direct Connect extended to Hyderabad; TGW attachments.

---

## D2 — Existing banking-application landscape

Document control on the canvas: author Atul Singh (AWS Cloud Admin); reviewed Manish Salaria (AVP Cloud Operations); owned Shrish Kumar Yadav (VP Datacentre Operations); version **1.4**, 9-July-2026, annual review.

**North-south:** Customer user → Cloudflare → F5-XC → AWS Mumbai public path.

**AWS Cloud, Asia Pacific (Mumbai) `ap-south-1`:**

- **PUBLIC VPC (DMZ zone) `10.30.0.0/16`** — public subnets AZ1/AZ2/AZ3, **Public ALB**, IGW.
- **PVT VPC (Private Zone) `10.32.0.0/16`:**
  - Private ALB
  - Web servers (multi-AZ)
  - App services (multi-AZ)
  - Databases (multi-AZ)

**East-west / hybrid:**

- On-prem DC, DR, AU BackOffice, Vendor.
- Vendor / back-office access via **Accops VDI**.
- Direct Connect → AWS Network Firewall / FortiGate → TGW → TGW attachment into the app VPCs.
- Public internet path also shown beside the hybrid path.

**Org inset:** AWS Control Tower, SCP, Core OU, Customer OU — same Control Tower story as S1.

This is the **current banking application's** spoke shape (public VPC + public ALB + IGW). It is **not** an instruction to clone that shape for the insurance platform.

---

## D3 — Existing production EKS application

A live AU production EKS estate (prod account / `ap-south-1`):

- End user → Cloudflare → F5 into the cluster.
- Public subnet (public ALB) and private / Fargate-style application subnets.
- Application namespaces typical of a bank digital platform (API, web, processing, integration, data).
- Database subnet with multiple stores as drawn: **MySQL RDS, Aurora PostgreSQL, EFS, Redis, Elasticsearch, Kafka**.
- Shared services: Terraform, GitLab; Accops VDI; IAM Identity Center.
- Shared network account: Transit Gateway ↔ FortiGate firewall ↔ Direct Connect ↔ on-premises DC/DR.
- Observability: Prometheus, Grafana; ASI / EKS EC2 capacity.

Confirms: the bank already operates EKS, GitLab CI/CD, Terraform, Aurora PostgreSQL, Kafka, Redis and Cloudflare+F5 on the north-south path, with FortiGate+TGW+DX on the hybrid path.

---

## S2 — Cloud Console Application Onboard v1.4

If an application is going on board, the application team must supply:

- Application **RIA ID**
- Infra architecture diagram
- Infra requirement sheet
- Infra cost approval
- Account creation details
- Account hardening

### Mandatory environments (default)

1. **Production (Prod)** — live business and customer-facing workloads.
2. **CUG (Closed User Group)** — restricted internal access for controlled workloads with strict identity and network access governance.
3. **UAT** — application testing, validation, business sign-off; **may host non-production workloads**.

### Default approach

Dev, SIT and Non-Prod environments **shall not be provisioned separately by default** and **shall be hosted within the UAT environment** for cost optimisation.

Logical segregation within UAT is by naming conventions, RBAC and resource tagging.

Separate Dev, SIT or Non-Prod **only** for specific cases (architectural, performance or compliance) with documented justification, impact assessment, and **formal approval from Cloud teams**.

### RIA ID sample fields

RIA ID; Banking days; Confidentiality (sample value 4); Availability (sample value 3); Exposure; Asset value; Tier; Category ID; Active; Escalation levels 1–3.

### Infra requirement sheet

Shared to `@psg.cloud` for cost creation. Cloud team validates the sheet and creates the infra cost estimate.

- UAT infra running **12 hours / day**.
- Prod infra running **24×7**.

---

## S3 — Cloud Standard Operating Procedure v1.7

### Scope

Applies to all personnel administering, managing and using Cloud services at AU Small Finance Bank: access management, resource provisioning, backup and DR, monitoring and alerting, incident management, change management, security practices, compliance, documentation and support.

**AD** is defined as **AU bank Active Directory**. AWS and **OCI** (Oracle Cloud Infrastructure) are both in scope of the SOP.

### OCI estate (parallel to AWS; not the insurance platform's home)

- Parent tenancy name: `aubank2`.
- Child tenancy: `aubankerp` (subscriptions managed from parent).
- Subscriptions include Universal Credits (Frankfurt infrastructure, start 2018) and Mumbai-region Oracle SaaS (EPM, Fusion ERP, Digital Assistant, Integration Cloud; start Aug 2023).

Insurance-platform workloads are **not** described as OCI tenants in these artefacts.

### AWS named accounts (as printed)

| Role | Account name | Account ID | Region named |
|---|---|---|---|
| AWS Cloud Master / Control Tower execution | `AU_AWS_MAS` | `379749588988` | `ap-south-1` |
| AWS Platform Master / Control Tower execution | `au-platform` | `534389188950` | `ap-south-1` |

Networking: AWS Cloud is connected to AUBANK DC/DR via **Direct Connect**. Subnet schemas are held in annexure workbooks (OCI, AWS-FCU, Platform AWS) — not supplied in this intake.

### Access and authentication

- Cloud user provisioning is by the **Central Bank AR process** with approval.
- After AR approval the user is integrated with **AU Central SSO Active Directory**, member of Cloud Secure login Group listed in UAC; role is mapped based on AR.
- Authentication via central SSO with AU Bank AD; MFA maintained for users.
- **Service users are created using IAM without MFA.**
- User access control reviewed periodically per standard UAC policy.

AD groups named for cloud login:

| Environment | AD group (as printed) |
|---|---|
| AWS Secure login | `aws secure login` |
| AWS platform secure login | `SSO-PLATFORM-USERS` |
| OCI secure login | `oci secure login` |
| OCI ERP secure login | `GROUP_OCI_AUBANKERP_PROD` |
| GCP secure login | `gcp secure login` |

GCP is named only as an AD login group in this SOP; no GCP landing-zone architecture was supplied.

### Resource management (VM)

- Regions: `ap-south-1a/b/c`, `ap-south-2a/b/c` (Mumbai and Hyderabad).
- Instance type: Virtual Machine / Bare Metal.
- Subnet: **Private only**; public requires **IS approval**.
- Secure Boot is **not** enabled because antivirus encryption in use requires it disabled.

### Public access control for buckets

**Policy: no bucket can be public.**

1. Account-level Public Access Block.
2. Bucket-level policy denying public access.

Fail-safe: if both controls are disabled, **QRadar SIEM** alerts the SOC.

### Logging and retention (AWS)

1. Account-level SIEM logs forwarded to **QRadar**.
2. **AWS CloudTrail logs retained 10 years.**
3. Amazon S3 bucket access logs retained **5 years**.

OCI account-level security logs likewise go to QRadar.

S3 static website hosting: refer to S4.

### Hosting S3 objects publicly via Cloudflare with SSE-S3

To serve static assets from **private** S3 buckets through Cloudflare:

- Cloudflare is the public-facing endpoint.
- Objects encrypted with **SSE-S3** (Amazon S3-managed keys). Cloudflare-routed requests **cannot use AWS Signature Version 4**, which is required for SSE-KMS.
- Cloudflare forwards to S3; SSE-S3 decrypts for response delivery.

At rest, objects must use **KMS CMK or CSP-managed keys as per requirements** — the static-hosting exception for SSE-S3 is specific to the Cloudflare origin path.

MeitY Cloud Security Best Practices and NIST SP 800-210 are cited as allowing CSP KMS when internal policy, legal regulation, authorisation, auditability and regulatory alignment hold.

### Storage / database / network

- VM boot volume 100 GB; application volume 100 GB (default sizes).
- Databases provisioned as **Oracle / Aurora MySQL / Postgres / MSSQL** per application requirement.
- Network segmentation by region and **application tier**.

### Backup and DR

- Bank standard: where customer Financial/Process Transaction data is stored in cloud and the pattern is **Active-Active**, the bank performs **failover testing rather than Disaster Recovery**.
- Backups from a **single source only**. If a read-replica and main RDS exist, take backups from one source only.

**Multi-AZ:**

- **Tier 1 and Tier 2 applications: Multi-AZ is mandatory in production.**
- **Tier 3 and above: Multi-AZ is not required** in production.

**DR planning:** Tier 1 and Tier 2 applications placed in cloud are **Active-Active**. Application HA testing follows the bank drill calendar. Data restoration follows Central Bank Restoration Policy by application tier.

### Monitoring, incident, change, security

- System/log/event notification and alerting **only for production**. Critical alerts to IM and Command central as tickets.
- Incident management: Central bank IM Policy (reporting, escalation, RCA). Quarterly report if a critical security incident is reported.
- Production change: Bank Change Management Policy — CR covering raise, approval, implementation, rollback, closure.
- Cloud account provisioned per **IS guidelines**.
- Patching per the patching SOP.
- Vulnerability management: bank Central IS VA/PT Policy; **quarterly** production VA scan, close per pre-defined timeline.

### Support

AWS support via AWS Support Console (`ap-south-1`). OCI via Oracle support. Cloud SLA refers to AWS Enterprise Agreement SLA and Oracle cloud SLA documents.

---

## S4 — S3 Static Website Hosting Controls v1.1

Purpose: serve static assets from **private** S3 buckets through Cloudflare.

### Bucket naming

FQDN format:

- Prod: `<bucket-name>.aubank.in`
- Non-prod: `<bucket-name>.aubankuat.in`

### Bucket policy

Grant **only** `s3:GetObject` for public or CDN access, conditioned on Cloudflare published IPv4 ranges (`https://www.cloudflare.com/en-in/ips/`). The SOP embeds the Cloudflare IPv4 list current at issue date; **do not treat that snapshot as immortal** — re-read the SOP and Cloudflare's published ranges before encoding them in IaC.

Principal `"*"`, action `s3:GetObject`, resource `arn:aws:s3:::<bucket-name>/*`, condition `aws:SourceIp` = Cloudflare IPv4 CIDRs.

### Encryption

SSE-S3 for the Cloudflare origin path (SigV4 / SSE-KMS incompatibility). CSP KMS otherwise per MeitY / NIST SP 800-210 as in S3.

---

## Claims extracted for Mahesh (not yet verdicts)

Each row is a source assertion. Reconciliation lives in module `18`.

| ID | Claim | Touches |
|---|---|---|
| C-01 | Control Tower + Organizations + SCPs + IAM Identity Center already exist in `AU_AWS_MAS` | topology, identity |
| C-02 | Spoke accounts attach to a **central** regional TGW; FortiGate Active/Passive lives in the EDGE VPC in the Network Account | topology, security |
| C-03 | Direct Connect uses existing Sify (primary) and Airtel (secondary) circuits, DX Gateway, extended to Hyderabad | hybrid connectivity |
| C-04 | North-south: Cloudflare → F5 XC; east-west: central FortiGate / TGW | trust boundaries |
| C-05 | Existing banking apps use Public VPC + IGW + Public ALB (`10.30.0.0/16` DMZ, `10.32.0.0/16` private) | **do not clone by default** |
| C-06 | Workforce cloud login is Bank AD / central SSO + MFA; service users are IAM without MFA | `TI-01` |
| C-07 | SCP restricts India geo-location; Prisma CSPM at org; CloudTrail → SIEM (QRadar); root caged | residency, security |
| C-08 | Default onboard: Prod + CUG + UAT; Dev/SIT live **inside UAT** unless Cloud team approves a split | environment topology |
| C-09 | No public buckets; public subnet needs IS approval | exposure |
| C-10 | Static public assets: private S3 + Cloudflare + SSE-S3 + Cloudflare IP allowlist; FQDN bucket names | edge, storage |
| C-11 | CloudTrail 10 years; S3 access logs 5 years; QRadar is the SIEM | retention |
| C-12 | Tier 1/2 production: Multi-AZ mandatory; cloud Tier 1/2 described as Active-Active with failover tests | DR |
| C-13 | Allowed DBs include Oracle, Aurora MySQL, Postgres, MSSQL; existing apps already run EKS, Aurora PostgreSQL, Kafka, Redis | technology qualification |
| C-14 | OCI `aubank2` / `aubankerp` is a parallel ERP/SaaS estate, not the NIP home | out of platform scope |
| C-15 | UAT infra 12h/day; prod 24×7; cost sheet via `@psg.cloud`; RIA ID required | onboarding / operations |
| C-16 | Production change follows bank CR process; quarterly VA/PT | change / security operations |
