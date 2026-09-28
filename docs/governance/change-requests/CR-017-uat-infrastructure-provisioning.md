# CR-017 — Provision the AU Bank Insurance Platform UAT infrastructure

**Raised:** 2026-09-28  
**Type:** INFRA / ARCH / SEC / OPS / DATA (provisioning request, not an architectural redesign)  
**Origin:** Human request to prepare a detailed UAT infrastructure CR  
**Workstream:** WS-3 primary; WS-1 integration and WS-2 identity are dependent consumers  
**Stage:** S08 — Engineering Foundation, with S09 — Platform & Environment Foundation overlapped  
**Status:** **PROPOSED / AI-DRAFTED; mandatory human approvals outstanding. No Terraform apply or environment provisioning authorized by this document.**  
**Priority:** S09 foundation critical-path input; execution scheduling belongs to Kalpana/Delivery  
**Architecture authority:** [R0 LLD](../../architecture/R0-LLD.md), [2026-09-14 human direction / ADR-020](../../architecture/2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md), [ADR register](../../platform/architecture-review/08-architecture-decision-log.md), [S09 stage and gates](../../application-lifecycle-bible/stages/S09-platform-foundation.md). Those sources and ratified ADRs prevail over this derived CR wherever they conflict.

## 1. Business reason and desired outcome

Request the bank Cloud, Network, Security, API-platform, Database and SRE teams to **design, approve and provision an isolated, observable, recoverable UAT estate** for the R0 assisted Life insurance journey: one bank RM, one existing-to-bank customer, Term or Savings/ULIP through 1SB/insurer test interfaces, consent and suitability gates, proposal, customer-device payment, reconciliation, issuance and durable audit evidence. UAT is used to prove these seams with **real bank non-production** CBS/CIF and workforce AD-verification dependencies, not developer stubs.

The repository already has application CI and scaffolded modules, UAT Spring profiles for several services, and the S09 platform architecture. A UAT YAML profile **does not establish an operational UAT estate**. Build the approved bank-grade platform rather than extending the Render.com preview or treating every scaffolded module as production-ready.

**Outcome:** platform team delivers IaC-defined UAT and dependency onboarding; app teams can deploy immutable images, execute full UAT journeys and capture repeatable S09 acceptance, security, data-protection and recovery evidence. No production or customer-facing go-live is implied.

## 2. Environment, ownership and boundaries

| Estate | Scope in this CR | Rule |
|---|---|---|
| Control Tower | Reuse/vend the approved **UAT** account; confirm access to existing **shared-services**, **security** and **network** accounts | **Five-account R0 model overall:** shared-services, security, network, uat, prod. This request does **not** independently provision prod or recreate existing enterprise services. |
| Development | Isolated **vpc-dev inside the UAT account**; economical dev EKS and non-production stores/config | Synthetic data; stubs permitted **here only**; separate IAM roles, namespaces/cluster, network policy, credentials, Aurora schemas or databases/Flyway history, cache key prefixes and MSK topic prefixes. |
| UAT | Independent **vpc-uat inside the same UAT account**, distinct private EKS cluster and isolated managed data | Masked/synthetic test data only (no production CIF dump or production secrets); real **non-production bank** CBS/CIF and AD verification via approved Apigee paths; no developer stub or dev routing. |
| Production | **Out of provisioning scope** | No cross-account route or shared credentials/data; same versioned Terraform modules can later be parameterized for production after its separate approvals. |
| Closed User Group | **Not in R0** (ADR-020) | Obtain an explicit bank Cloud onboarding waiver if its form still requires CUG; never create an empty extra estate to satisfy the form. |
| Regions | Mumbai `ap-south-1` primary; approved Hyderabad `ap-south-2` **UAT recovery-test scope only** | India-only data, log, replica, backup and archive placement. No assumption that a UAT exercise by itself creates or approves production DR. |

Bank Cloud must supply account IDs, permitted AZ **IDs**, CIDRs, RIA tier/qualification, project tags, budget, tenancy/SSO groups, existing shared-service interfaces and the approved deployment boundary **before** applying Terraform. Publish no invented IP, hostname or cloud account ID in this CR.

## 3. Provisioning bill of materials — UAT

| ID | Component / requested infrastructure | Required topology and acceptance |
|---|---|---|
| U01 | **Landing zone and state** | UAT account onboarding, region-restricting SCP/controls, tagging/budget alarms, central Security and shared-services integration; versioned Terraform modules with isolated encrypted remote state and locking. Identify *reusable* bank resources versus *new* NIP resources. |
| U02 | **UAT VPC and networking** | Dedicated `vpc-uat` in `ap-south-1`, 3 AZ subnet coverage (private-app, private-data, TGW attachments), routing, private DNS/Resolver, security groups, NACL review and VPC/TGW flow logs. Paid UAT HA capacity in at least 2 AZs. **No workload VPC IGW, NAT, public ALB or public data endpoints.** The legacy LLD's empty public workload subnets are subject to Board-1 F-01: do not provision before Cloud/Security settles the bank SOP exception. |
| U03 | **Bank network attachments and inspection** | Attach the UAT spoke to the **existing** `AU-CTO-NETWORK` TGW with UAT-only route tables; bank-side VPN first and existing DXGW connectivity as authorized. No second TGW, second DX circuit, VPC peering or direct prod path. Determine via **ASM-012 / Deepali + bank Network** whether the approved egress inspection is existing EDGE FortiGate or additional UAT AWS Network Firewall/inspection VPC. Do not provision duplicate appliances on an unresolved assumption. If the spoke inspection variant is approved, use 2 UAT firewall endpoints/AZs and centrally placed NAT/EIPs; no NAT in the workload VPC. |
| U04 | **Private access and ingress** | VPC endpoints (S3, DynamoDB, ECR API/DKR, Secrets Manager, STS, CloudWatch Logs as applicable), private Route 53 and Resolver, ACM/TLS. Inbound bank standard: **Cloudflare Enterprise SaaS → F5-XC SaaS → Amazon API Gateway → VPC Link → internal ALB → EKS**. One environment-specific public hostname with `GET /*` to `nip-web`, `/api/*` to NIP BFF; separate tightly source-restricted PG-callback API route. No public ALB or additional admin hostname. Edge SaaS is a bank onboarding dependency, not a VPC appliance. |
| U05 | **Outbound API onboarding (bank-owned)** | **Apigee outbound**, with approved UAT product/proxies, private path for internal AD-verify and CBS/EBS interfaces, approved paths for 1SB/PG/SMS, client credentials/certificates, rate limits, error/timeout contracts and gateway telemetry. 1SB allowlists **Apigee UAT egress IPs**, not UAT spoke NAT addresses. `1sb-integration-service` uses an Apigee-configured base URL; no direct 1SB origin from EKS and no Cloudflare/F5 hairpin for internal bank APIs. Block UAT external integration until `SPIKE-001` written onboarding answers are supplied. |
| U06 | **EKS and add-ons** | One dedicated **private UAT EKS cluster** (distinct from dev), supported bank-approved Kubernetes version, managed on-demand worker groups spanning at least 2 AZs (starting LLD: 3 worker nodes across 2 AZs; final instance/node sizing by SRE). Private control-plane access, audit logs, managed upgrades, pod identity/IRSA, namespaces `edge`, `identity`, `shared-platform`, `life-cell`, `integration`, `jobs`, `platform`; default-deny NetworkPolicy with explicit seam allowances, pod security admission, image provenance and Karpenter if qualified. Add VPC CNI, CoreDNS, kube-proxy, AWS LB Controller, ExternalDNS, Secrets Store CSI, Fluent Bit, ADOT and KEDA (consumer lag only). Sale-path deployments min 2 replicas, PDB minAvailable 1, cross-AZ spread. Stateless business services use no PVC. |
| U07 | **Container images and delivery** | Approved ECR repositories in shared-services, immutable digests/scan-on-push, cross-region DR image replication if DR exercise requires it. **GitLab CI/CD + approved GitLab Runner + Terraform** is the enterprise deployment target (ADR-016 / current platform topology); existing GitHub Actions are source CI until migration/cutover is approved. Build image **once**, promote **by digest** to UAT, signed approvals, audit trail, externalized UAT config, separate schema migration Job, progressive/rolling deployment and executable rollback. **Ansible** for post-deploy checks, rollback, network and DR exercises. Do not silently add Argo CD despite older §9 LLD wording. |
| U08 | **Aurora PostgreSQL** | **One UAT Aurora PostgreSQL cluster**, writer and reader in different AZs, private-data subnets, encrypted with approved KMS key, PITR/backup, monitored connections and per-context schemas/least-privilege roles. Include `onesb`, `hub`, `identity`, `keycloak` and bounded-context schemas only as their approved features are deployed. `bank-persistence-service` stays on its authorized `onesb`/audit-ingestion scope; no cross-schema grants or cluster-per-service pattern. Aarti owns supported engine/version, sizing, credentials, parameter groups, max connections and migration strategy. |
| U09 | **DynamoDB** | Environment-isolated `journey-state-uat`, `integration-jobs-uat`, `audit-events-uat` with CMKs, PITR, least-privilege table IAM, TTL **only** on approved transient jobs; optional per-owner idempotency implementation requires Aarti's explicit decision. Audit writers cannot update/delete events. No `sessions` DynamoDB table (ADR-011). |
| U10 | **S3 and immutable evidence** | Private, public-access-blocked `raw`, `docs` and `audit-archive` UAT buckets with **Object Lock Compliance mode/7-year project retention requirement**, TLS-only policy, CMKs, access logging, replication/bucket-lock proof in approved India-region UAT DR scope. Separate TF-state and operational failed-delivery buckets (no confused retention). Do not assume asynchronous S3 CRR alone proves **zero evidence loss**: define and test the required replication/acknowledgment/reconciliation design with Compliance. |
| U11 | **Valkey** | Managed ElastiCache for Valkey, UAT **primary + replica across 2 AZs**, TLS/CMK, auto failover, SG isolation and per-service ACL users/prefixes. Use for token-hiding BFF sessions, authorized L2 read cache and rate-limit counters; **never** idempotency, PII cache, consent or audit system of record. |
| U12 | **Events** | Private Amazon MSK UAT **3 brokers across 3 AZs**, replication factor 3, min ISR 2, encrypted TLS/CMK, per-topic SASL/IAM, domain/versioned UAT topics and DLQs, AWS Glue **Schema Registry only**. Transactional outbox remains source; outbox publisher and replay-tolerant idempotent consumers are application responsibilities. No Kafka StatefulSet or MSK Replicator. |
| U13 | **Operations search and telemetry** | CloudWatch Logs/Metrics, distributed tracing (ADOT with bank-approved backend), AMP/AMG if qualified, and VPC-only **OpenSearch UAT** (LLD starting shape: 2 data nodes + 3 dedicated masters), Fluent Bit→Firehose with failed-delivery bucket; firewall/TGW/VPC/API/MSK and app logs searchable with trace correlation. PII masked before log emission and verified on the index; operational retention per `RET-OPERATIONAL` (LLD: 30-day hot / 90-day disposal). No regulatory proof in OpenSearch or MSK. Prefer an existing bank-approved enterprise telemetry facility **only if** Board 7/Security approve the documented equivalent. |
| U14 | **Identity and workload security** | Private Keycloak (or approved provider-neutral IdP behind adapter), `identity-provider-adapter-service`, `identity-authorization-service` PDP and token-hiding NIP BFF; Keycloak database in Aurora, **not PVC**. Bank RM credentials remain with existing AD/SSO bank service reached via **Apigee private**; no direct EKS LDAP, no bank users mastered in Keycloak, no default Keycloak UI shown to bank users. Partner test users can be provisioned in platform IdP with maker-checker. IAM workload role per deployable, KMS data/log/secrets/evidence key hierarchy, Secrets Manager/CSI tmpfs mounts, exercised rotation/revocation. |
| U15 | **Security baseline and shared operations** | Central account CloudTrail, AWS Config, GuardDuty/Security Hub integrations, KMS/secret access audit, scoped operator IAM/SSO and break-glass path; image/dependency/secrets/Terraform policy scans block unsafe changes. Central alarms/on-call and runbooks cover pods, connections, Apigee/1SB/PG, audit backlog, outbox age, MSK lag, WORM replication, Aurora/Valkey failover, certificates and backup failures. |
| U16 | **Backup and recovery-test resources** | AWS Backup/PITR policies for Aurora/DynamoDB and India-region encrypted backups. Within **approved UAT recovery-test scope**, prepare `ap-south-2` network/key/secret/image/WORM replica prerequisites and connectivity to **non-production** bank test dependencies if exercising regional recovery. Aarti chooses Aurora Global secondary vs cross-region backup/restore by **measured** RTO/RPO; validate DynamoDB's cross-region recovery independently (PITR alone is same-region). **No automatic running second EKS cluster, replicated MSK, replicated Valkey or replicated OpenSearch.** Finance/Payments must reconcile payment state after restore. |

**Resource class and cost are proposals, not procurement approval:** SRE and DBA must supply the UAT cost envelope, cloud qualifications, quotas and final node/DB/broker/search sizing before approval. Avoid a 3×production-shaped dev copy or unnecessary duplicate bank gateways.

## 4. Connectivity and service-dependency matrix

| Source → target | Transport/boundary | Proof required before UAT |
|---|---|---|
| UAT web/mobile → backend | Cloudflare → F5-XC → API Gateway → VPC Link/internal ALB → BFF | Edge TLS, WAF/source restrictions, no public workload exposure, BFF token-hiding |
| `integration-hub-service` → `1sb-integration-service` → 1SB | Namespace policy; adapter HTTPS/mTLS via **Apigee UAT** | Bank Apigee product/path, test credentials, 1SB UAT Apigee IP allowlist, quote/poll smoke test |
| Customer service → CBS/CIF / EBS | **Private Apigee target** over approved bank path | Reachable real bank **test** dependency, allowed prefix/port/DNS, no dev stub |
| Workforce login → AD-verify | Existing bank API via **private Apigee**, never direct LDAP | Test-user login and role/PDP default-deny, no bank password stored in platform |
| Payment → AU Bank PG | Outbound Apigee for session; separate source-allowlisted inbound callback and settlement path | UAT session/callback/replay/reconciliation tests; payment on customer test device only |
| Audit/outbox → MSK → audit consumer → DynamoDB and S3 WORM | Per-topic IAM, private endpoints, append-only evidence | Proof that outage/replay cannot mark SOLD without durable audit ack; no evidence solely in broker/search |
| UAT → production stores | **DENY** | Cross-account, route-table, IAM and secret access negative tests |
| Dev → UAT | **DENY by default** except explicitly approved shared tooling | Isolation and synthetic-only dev data validated |

No insurer webhook ingress is required in R0; the approved 1SB flow uses polling.

## 5. Implementation sequence / deliverables

These are **request packages** mapped to existing S09 stories; they do **not** invent a parallel backlog or authorize implementation.

1. **P0 — Bank onboarding and controls:** approve account/tenant reuse, UAT budget, app tier, India SCP, Terraform state/locking, IAM/SSO, security-account integration, baseline keys. **Deliver:** account IDs, shared-resource ownership map, signed sizing/cost and qualification list.
2. **P1 — Network first:** agree CIDR/AZ IDs, route isolation, existing TGW/VPN/DXGW, `ASM-012` egress-control disposition, private Apigee paths and bank test firewall changes. **Deliver:** approved topology + rules/route tables + connectivity proof, never an invented 1SB NAT allowlist.
3. **P2 — Private compute:** UAT EKS/namespace/policy/add-on baseline and private ALB prerequisites. **Deliver:** repeatable cluster apply and denied-traffic test.
4. **P3 — Data/evidence/messaging:** Aurora, DynamoDB, locked S3, Valkey, MSK/Schema Registry, KMS/Secrets/backup and approved UAT DR prerequisites. **Deliver:** DB migration smoke, failover and locked-object denial, outbox/event proof.
5. **P4 — Enterprise API onboarding:** Cloudflare/F5-XC/API Gateway/ALB, Apigee partner and private bank API products, PG callback/settlement. **Deliver:** end-to-end non-prod paths and written `SPIKE-001` answers.
6. **P5 — Private identity:** adapter/PDP/Keycloak and bank workforce test identity integration; IRSA and secret injection. **Deliver:** authorization deny/allow and session restart proof.
7. **P6 — Observability:** metrics, masked logs, traces, alarm ownership, optional approved bank-equivalent search. **Deliver:** correlated trace BFF→Hub→1SB adapter and alert tests.
8. **P7 — GitLab/Terraform/Ansible delivery:** reviewed plan/apply stages, digest promotion, schema migrations, deployment audit trail and rollback automation. **Deliver:** green pipeline and deployment provenance.
9. **P8 — Executed acceptance:** negative isolation tests, real bank test connectivity, restore/rollback/rotation/replication/replay/failover drills. **Deliver:** signed evidence and outstanding-defect register before UAT declared ready.

Start P0/P1 bank-side external requests immediately on CR approval; P1 can proceed in parallel with IaC design but **network-dependent application integration cannot be claimed ready from stubs**.

## 6. UAT entry/exit and acceptance evidence

UAT is ready for integrated testing **only when all mandatory controls below pass with attached evidence**. Map evidence to [S09 validation tests and GATE-S09](../../application-lifecycle-bible/stages/S09-platform-foundation.md); an infrastructure ticket alone cannot mark S09 PASSED.

- [ ] Approved UAT account, budget/tags and scoped inventory; dev and UAT separate VPCs/cluster/data/roles, no production access.
- [ ] Terraform plan reviewed; policy-as-code blocks non-India placement, public storage/DB/EKS, broad IAM and wrong routes; remote state lock and drift detection tested.
- [ ] Bank Network approves existing-TGW attachment, inspection variant and UAT routing; VPN/approved DX path to **non-prod** CBS tested, Apigee private bank targets documented.
- [ ] Apigee UAT onboarding and **its egress IPs** approved by 1SB; 1SB quote/poll, AD-verify, PG request/callback and settlement verified without direct provider-origin egress.
- [ ] Private EKS, namespace isolation, admission, NetworkPolicy, minimum replicas/PDB and cross-AZ scheduling demonstrated.
- [ ] Aurora physical segregation/access denial, DynamoDB permissions/PITR, Valkey failover and MSK topic/consumer authorization exercised.
- [ ] S3 Compliance Object Lock/retention and regional replication configured **before** any regulated UAT evidence is accepted; locked delete denied; any asserted zero-loss requirement has a validated technical control beyond asynchronous CRR.
- [ ] KMS, real Secrets Manager/CSI and operator/workload IAM in service; secret rotation **and emergency revocation** rehearsed.
- [ ] Build once/promotion by digest audited in approved delivery pipeline; intentionally broken UAT release **rolled back** while preserving data and applying compatible DB changes.
- [ ] CloudWatch/approved telemetry: metrics, logs, traces, dashboards, on-call routing, masked PII search and security event forwarding proven by a walked transaction.
- [ ] Aurora/DynamoDB/backup/approved regional UAT restore **executed and timed** against signed RTO/RPO; verify evidence store, bank test connectivity, outbox replay and payment reconciliation after recovery. No claim that an untested RPO target is met.
- [ ] Negative tests: dev→UAT/prod data, unauthorized namespace/service, direct 1SB origin, direct EKS→AD LDAP, direct app→OpenSearch, wildcard IAM, mutable audit evidence, public data endpoint and no-OTP payment path all refused.
- [ ] Evidence pack: diagrams as built, Terraform state refs/plan outputs (no secrets), cost estimate, bank connectivity and Apigee approvals, CMDB inventory, runbooks, certificates, masked test accounts, owner/on-call contacts and timed test reports.

**Suggested UAT handover threshold:** `S09-G1…G13` have evidence at the levels required by the stage file; blockers are explicitly recorded and approved by their legitimate human owners. Never manufacture a PASSED stage gate or approvals in a CR.

## 7. Explicitly out of scope and exceptions

Do **not** silently add a separate dev account, CUG, production estate, duplicate TGW/DX, public workload ALB/IGW, AWS-hosted F5 appliance, Apigee as AWS Terraform, direct 1SB origins, direct EKS LDAP, service mesh, per-service Aurora clusters, self-managed Redis/Kafka/search, a warehouse (Glue ETL/Athena/Redshift/QuickSight), MSK Replicator, always-on multi-region active-active, insurer webhook ingress, customer DIY, or additional mobile/admin applications. AWS Glue **Schema Registry** remains required with MSK.

**Outstanding approvals/decisions, not silently assumed:**

| Decision/dependency | Owner / required evidence | Why it blocks |
|---|---|---|
| Bank Control Tower vending, CUG waiver, app tier, resource qualification and **UAT cost** | Bank Cloud + Shivanshi + Kalpana | Cannot procure/apply unknown account and cost |
| Existing EDGE FortiGate versus incremental AWS Network Firewall (`ASM-012`) and whether TLS inspection is exempt on the mTLS partner path | Deepali + bank Network + Shivanshi | Cannot choose firewall/route by diagram interpretation |
| Apigee edition/product, private UAT endpoint and per-path onboarding, Apigee UAT egress IP list (`SPIKE-001`) | Bank API-platform + Shivanshi + WS-1 | Blocks 1SB, workforce AD API, CBS and PG |
| Exact CIDRs/prefixes/DNS and UAT bank firewall/VPN/DX acceptance | Bank Network | Blocks integration and DR test |
| DB engines/instance, connection budget, Aurora recovery option and DynamoDB cross-region strategy | Aarti + Shivanshi | Blocks reliable size, RPO/RTO claim |
| Mandatory retention basis, evidence acknowledgement and demonstrated cross-region zero-loss claim | Shailja + Aarti + Deepali | Async replication alone is not proof |
| GitLab CI/CD migration/cutover from existing GitHub CI and bank telemetry reuse | Shivanshi + Amit + bank Platform | Prevents duplicate/conflicting delivery and telemetry estates |

## 8. Governance and authorization

**Review package only.** Requested approvals: **Mahesh** human Board 1/T4 Architecture (scope/topology); **Deepali** human Board 4 Security (inspection, IAM, trust boundaries, secrets); **Shailja S** human Board 6 Compliance (India residency, immutable retention/evidence); **Aarti** Data/DBA (cluster isolation/backup/restore); **Shivanshi** Board 7 SRE (runbooks, sizing, operating budget, readiness); **Kalpana** Delivery (dependency and schedule); Bank Cloud, Network and API-platform teams for their own enterprise changes. **Amit** Engineering, **Swapnali** QA and **Rajal** Product review their respective deployability, UAT evidence and R0 journey constraints.

**Do not execute Terraform in non-dev AWS accounts, create production-facing routes, publish IP allowlists or mark stage gates approved until the named human and bank-team approvals are recorded.** Draft IaC and synthetic-data proof may be prepared separately under existing governance.

## 9. Reference hierarchy

- [R0 LLD: resource and deployment requirements, §1–§13](../../architecture/R0-LLD.md); [platform topology/DR diagrams](../../architecture/README.md).
- [2026-09-14 owner direction and ADR-020](../../architecture/2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md) supersedes older direct-provider egress, separate-dev-account and legacy Argo suggestions.
- [Board-1 estate rereview: unresolved FortiGate, public-subnet and bank-tier conditions](../../architecture/BOARD-1-REREVIEW-VIN003-ESTATE-2026-09-14.md).
- [Canonical S09 stories, verification tests and exit gate](../../application-lifecycle-bible/stages/S09-platform-foundation.md).
- [Current machine-readable stage and ID allocation](../state/CURRENT-STATE.yaml).

**Prepared by:** AI assistant for Architecture/Cloud/SRE review; **signature status:** `AI-DRAFTED — HUMAN APPROVALS OUTSTANDING`.
