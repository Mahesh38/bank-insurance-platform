# Cloud architecture and shared-responsibility matrix — R0

**Pack:** [`ARB-PREREQUISITE-PACK.md`](../ARB-PREREQUISITE-PACK.md) row 17  
**Status:** `AI-DRAFTED`. Aligns `ADR-001`, `ADR-009`, `ADR-010`, `ADR-018`, `ADR-020`, LLD, and estate paper `18` (`BE-01`).  
**Cloud:** AWS, regions `ap-south-1` (primary) and `ap-south-2` (DR replicas only).

---

## 1. What we consume vs what we clone

The insurance platform **attaches as a spoke** to the existing AU bank cloud estate. It does not provision a second Control Tower, a second Transit Gateway, a second Direct Connect, a FortiGate pair, or a copy of the neighbour app’s Public VPC + Public ALB.

| Layer | Already exists (bank) | This programme adds |
|---|---|---|
| Org / SSO | Control Tower, IAM Identity Center, SCPs pinning India | Five accounts: `shared-services`, `security`, `network`, `uat`, `prod`. **No separate `dev` account. No CUG.** UAT account hosts `vpc-dev` + `vpc-uat` (`ADR-020`) |
| Network hub | `AU-CTO-NETWORK` TGW, DX Gateway, EDGE FortiGate | Spoke attachment, per-environment route tables, per-environment inspection VPC (or share EDGE — `ASM-012`, Deepali) |
| North-south SaaS | Cloudflare Enterprise, F5-XC | Configuration of **this** hostname. AWS entry = API Gateway (`ADR-018`) |
| Outbound API plane | Apigee | NIP product + proxies (`DEP-20260914-apg`). Not Terraform in our accounts |
| Delivery | GitLab, Terraform standard | This repo’s pipelines and modules |

---

## 2. Shared-responsibility matrix

R = Responsible (does the work) · A = Accountable (the named owner ARB can call) · C = Consulted · I = Informed.

| Control outcome | AWS | Bank cloud / network | This programme (NIP) | Apigee / API platform | 1SB / insurer |
|---|---|---|---|---|---|
| Physical datacentre, hypervisor, region availability | **A/R** | I | I | — | — |
| Account vending, SCPs, org trail | C | **A/R** (Control Tower) | C (Shivanshi requests five accounts) | — | — |
| IAM Identity Center / human SSO | C | **A/R** | I | — | — |
| Workload IAM (IRSA, per-service role) | C | I | **A/R** (Shivanshi + Amit) | — | — |
| KMS CMK policy, rotation | C | C (key admin pattern) | **A/R** (Deepali + Shivanshi) | — | — |
| Encryption in transit / at rest configuration | C | I | **A/R** | TLS on proxies | mTLS as they require |
| Inbound WAF / DDoS | — | **A/R** (Cloudflare, F5-XC) | C (hostname, origin) | — | — |
| Inbound AWS API Gateway + Internal ALB | C | I | **A/R** | — | — |
| Outbound 1SB and bank APIs | — | C (private path) | R (adapter base URL) | **A** (product, IPs, private target) | R (their origin + allowlist of **Apigee IPs**) |
| Egress inspection pod→Apigee | C (NFW) | C (FortiGate hub) | **A** design; **Deepali accepts** remainder | I | — |
| CBS/CIF correctness | — | **A/R** (EBS/CBS) | R (consume snapshot) | C (private proxy) | — |
| Workforce credential SoR | — | **A/R** (Bank AD) | R (call AD-verify; never LDAP) | C (private proxy) | — |
| Partner identity in IdP | — | I | **A/R** (WS-2 Keycloak + PDP) | — | — |
| Suitability / consent / payment-device isolation | — | I (PG hosts 3-DS) | **A/R** (C1/C2/C4 in services) | — | Underwrite only |
| 7-year WORM evidence | C (S3 Object Lock) | I | **A/R** (Aarti + `#16`) | — | — |
| Application vulnerabilities (code) | — | I | **A/R** (Amit + Deepali + Swapnali) | — | Their adapter surface |
| VA/PT of bank SaaS (Cloudflare/F5/Apigee) | — | **A/R** | I | C | — |
| VA/PT of NIP apps and APIs | — | I | **A/R** (Deepali + Swapnali) | — | — |
| DR drill **record** (NFR-DR-04) | C | C (DX/VPN) | **A/R** (Shivanshi, timed) | I | I |
| PII in logs | — | I | **A/R** (fail the pipeline) | Must not log payloads | Must not log bank PII |

AWS’s published Shared Responsibility Model covers “security **of** the cloud”. Everything in the NIP columns is security **in** the cloud and is not outsourced by using managed services.

---

## 3. Accounts and blast radius

| Account | Holds | If it is compromised |
|---|---|---|
| `shared-services` | Terraform state pattern, ECR, shared runners | Build supply chain — not customer data at rest |
| `security` | CloudTrail, GuardDuty, Security Hub, Config | Detection failure; not the sale-path datastore |
| `network` | Inspection VPCs, attachments | Egress / routing; production data does not live here |
| `uat` | `vpc-dev` (synthetic) + `vpc-uat` (masked) | Two route tables so stubs cannot reach prod CBS |
| `prod` | Real ETB, real AD-verify, real 1SB | The CIS blast radius |

No workload VPC has an Internet Gateway. The only IGW is on the inspection / EDGE path.
