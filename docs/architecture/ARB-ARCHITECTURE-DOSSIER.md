# AU Bank Insurance Distribution Platform (NIP / 1SB) — Architecture Review Board (ARB) Dossier

**Document Reference:** `ARB-DOSSIER-2026-R0`  
**Workstream:** WS-3 (Platform Core) · WS-1 (1SB Supplier Integration) · WS-2 (Workforce Identity & Access Enabler)  
**Target Release:** Release 0 (R0 Assisted Term Life Slice) & Target Architecture (Horizons H0 → H3)  
**Primary Architect:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2)  
**Co-Reviewers & Stakeholders:**  
- **Rajal** — Product Owner (Board 3 / R1)  
- **Amit** — Technical Head / Engineering Lead (Board 2 / R3)  
- **Deepali** — Security Architect (Board 4 / R8)  
- **Aarti** — Data & Persistence Architect (Specialist / Data Board)  
- **Shivanshi** — Principal Reliability & SRE Lead (Board 7 / R10)  
- **Shailja S** — Compliance & Regulatory Risk Head (Board 6 / R9)  
- **Swapnali** — QA & Verification Lead (Board 5 / R7)  
- **Kalpana** — Delivery & Program Lead (R12)  
**Status:** ALIGNED WITH INTERNAL ARCHITECTURE TEAM DIRECTIVES (Mandatory Human Sign-offs Pending)  
**Visual Reference Diagram:** `R0 on AWS — What Runs Where` (Enterprise Bank Ingress & EBS Core Connectivity)

---

## Executive Summary & Strategic Objective

### 1. The Business Context
AU Small Finance Bank holds an IRDAI Composite Corporate Agency Licence (`CA0515`). Historically, third-party bancassurance integrations relied on direct redirection to aggregator or insurer portals (e.g., Beema), causing:
1. **Loss of sales visibility & drop-off analytics** once the customer leaves the bank boundary.
2. **Regulatory vulnerability** under IRDAI mis-selling, suitability, and consent mandates.
3. **Severe vendor lock-in** where bank applications were hard-coupled to external aggregator wire protocols.
4. **Attribution leakage**, risking incorrect commission payouts and lack of an immutable audit trail.

### 2. The Core Architecture Mission
The AU Bank Insurance Distribution Platform (National Insurance Platform - NIP) establishes a **bank-owned, evidence-bearing, multi-carrier distribution engine**. 

> **The R0 Objective (`R0-ASSISTED-TERM-SALE`):**  
> Enable one Relationship Manager (RM) — acting as a certified Specified Person (SP) — to sell one Term Life policy to one Existing-to-Bank (ETB) customer across Group A insurers end-to-end through a unified Flutter interface, with automated suitability evaluation, customer-device OTP consent, customer-device payment execution, reconciled issuance, and immutable audit logging.

### 3. Key Architectural Tenets Defended at ARB
1. **Capability before Service, Ownership before Deployment:** Every service owns one bounded context write-model. No cross-service database access.
2. **Enterprise Bank Perimeter Ingress:** Edge access traverses **Cloudflare (Enterprise CDN & DDoS)** → **F5 BIG-IP / WAF (Bank Standard)** → **External ALB** → **Amazon API Gateway** → **Internal ALB**.
3. **Centralized Inspection & Egress:** Outbound traffic traverses a dedicated Inspection & Egress VPC (AWS Network Firewall + NAT EIPs allowlisted by insurers).
4. **Hard Regulatory Controls Enforced in Services, Not UI:** Suitability (**C1**), Consent (**C2**), Customer-Device Payment Isolation (**C4**), and Audit-before-Sold (**C7/C8**) cannot be bypassed by any API client.
5. **Replaceable Provider Boundary:** Domain services communicate only in bank-canonical contracts through an Integration Hub. 1SilverBullet (1SB) or direct insurers are pluggable adapters.
6. **Core Banking via EBS (Enterprise Service Bus):** Customer profile retrieval and CIF lookups integrate via bank **EBS (Enterprise Service Bus) APIs** over private Transit Gateway hybrid connectivity.
7. **Enterprise DevSecOps & Observability Baseline:** 
   - **GitLab CI/CD** as the unified enterprise pipeline standard.
   - **Terraform** for Infrastructure as Code (IaC) provisioning across AWS environments.
   - **AWS CloudTrail** (governance, account audits, and IAM non-repudiation) alongside **Amazon CloudWatch** (operational logs, performance metrics, and alarms).

---

## High-Level Architecture & End-to-End Topology

```
+-----------------------------------------------------------------------------------------------------------------------------------+
|                                                 BANK PERIMETER & PUBLIC AWS EDGE (ap-south-1)                                     |
|  [RM / Partner Device]                                                                                                            |
|         |                                                                                                                         |
|         v                                                                                                                         |
|  CLOUDFLARE (Enterprise CDN / Edge DDoS / DNS / TLS 1.3 - Bank Standard)                                                          |
|         |                                                                                                                         |
|         v                                                                                                                         |
|  F5 BIG-IP / WAF (Bank Enterprise L7 Security Policy & Application Firewall)                                                      |
|         |                                                                                                                         |
|         v                                                                                                                         |
|  EXTERNAL ALB (Edge Ingress Load Balancer)                                                                                        |
|         |                                                                                                                         |
|         v                                                                                                                         |
|  AMAZON API GATEWAY (REST Throttling / Request Validation / Token Inspection / VPC Link)                                          |
+-------------------------------------------------------------------+---------------------------------------------------------------+
                                                                    | VPC Link
                                                                    v
+-------------------------------------------------------------------+---------------------------------------------------------------+
| WORKLOAD VPC (10.env.0.0/16 - 3 AZs)                              |                                                               |
| PRIVATE APP SUBNETS (/20 x 3 AZ)                                  | INTERNAL ALB (Private Ingress Router)                         |
|                                                                   +---------------------------------------------------------------+
|                                                                           |
|  +------------------------------------------------------------------------v----------------------------------------------------+  |
|  | AMAZON EKS CLUSTER                                                                                                          |  |
|  |                                                                                                                             |  |
|  |  [ns: edge]               nip-web (Flutter Web)               #2 NIP BFF (Token-Hiding Session Custody)                     |  |
|  |                                                                                                                             |  |
|  |  [ns: identity]           Keycloak (Private IdP)     identity-provider-adapter     #3 identity-authorization (PDP Engine)   |  |
|  |                                                                                                                             |  |
|  |  [ns: shared-platform]    #19 Configuration (W0b)    #5 Lead (Origination)         #9 Journey Orchestration (State/Saga)    |  |
|  |                           #4 Customer (EBS Client)   #8 Product Catalogue (Term)                                            |  |
|  |                           #6 Consent (OTP/WORM)      #7 Suitability (C1 Gate)      #12 Payment (Device Isolation)           |  |
|  |                           #13 Policy (Reconciled)    #16 Audit (C7/C8 Immutable)   #17 Notification (OTP/Link)              |  |
|  |                                                                                                                             |  |
|  |  [ns: life-cell]          #10 Quotation (Term LOB)   #11 Proposal & UW (Term LOB)                                           |  |
|  |                                                                                                                             |  |
|  |  [ns: integration]        #14 Integration Hub (Attribution/Bulkheads)    #15 1SB Adapter (WS-1 mTLS Engine)                 |  |
|  |                                                                                                                             |  |
|  |  [ns: jobs]               outbox-publisher           payment-reconcile             MSK Consumers      MIS/Reporting (RO)    |  |
|  +-----------------------------------------------------------------------------------------------------------------------------+  |
|                                                                           | (Inspected Egress)                                    |
+---------------------------------------------------------------------------+-------------------------------------------------------+
| PRIVATE DATA SUBNETS (/24 x 3 AZ)                                         | TGW Attachment                                        |
|  - Aurora PostgreSQL (1 Cluster, Schema/Context)   - DynamoDB (Journeys)  v                                                       |
|  - ElastiCache Valkey (Sessions, L2, Rate Limits)  - Amazon MSK (Outbox)  +-------------------------------------------------------+
|  - OpenSearch (VPC-only SRE Logs)                  - S3 Object Lock (WORM)| AWS TRANSIT GATEWAY (TGW)                             |
+---------------------------------------------------------------------------+---+-------------------+-------------------------------+
                                                                                |                   |
                                        +---------------------------------------+                   +-------------------------------+
                                        | (Egress Inspection Path)                                  | (Private Hybrid Transit)      |
                                        v                                                           v
            +-------------------------------------------------------+           +---------------------------------------------------+
            | INSPECTION & EGRESS VPC (network account)             |           | BANK ON-PREMISES & PARTNER NETWORK                |
            |  - AWS Network Firewall (Domain Allowlist / IPS)      |           |  - EBS [Enterprise Service Bus] (CBS / CIF APIs)  |
            |  - NAT Gateways with Fixed Elastic IPs (EIPs)         |           |  - Bank Active Directory (AD / SSO)               |
            |  - Passthrough mTLS to 1SilverBullet / Providers      |           |  - AU Bank Payment Gateway                        |
            +-------------------------------------------------------+           |  - 1SilverBullet Gateway (Whitelisted EIPs)       |
                                                                                +---------------------------------------------------+
```

---

## Detailed Component-by-Component Defense

For each tier and component in the architecture, this section articulates:
1. **Presence & Function:** What it does in the ecosystem.
2. **Necessity:** Why it is mandatory (technical, operational, regulatory).
3. **Alternative Options Evaluated:** What else was considered.
4. **Rationale for Selection:** Why our choice is the optimal fit for AU Bank.
5. **Trade-offs & Mitigations:** Downside risks and how they are controlled.

---

### 1. Perimeter, Edge Ingress & Network Security

#### 1.1 Cloudflare (Enterprise Edge CDN & DDoS Protection)
- **What it is & What it does:** Cloudflare Enterprise CDN and Edge Security layer terminating client TLS 1.3 traffic, providing global edge DDoS mitigation, DNS resolution, and edge caching for static Flutter web bundles.
- **Why Required:** Aligns directly with AU Bank's existing enterprise perimeter contract. Shields AWS origin infrastructure from direct internet exposure.
- **Alternatives Considered:**
  - *AWS CloudFront:* Fully functional AWS native CDN, but adopting Cloudflare leverages AU Bank's existing enterprise licensing, global security policies, and standard edge operations.
  - *Akamai:* Higher operational complexity and distinct toolchain from bank standard.
- **Why Best Suited:** Bank standard; provides multi-terabit volumetric DDoS mitigation, bot management, and Indian PoP traffic acceleration.
- **Trade-offs & Mitigations:** Dynamic API responses must never be cached at Cloudflare (`Cache-Control: no-store` strictly returned on all API endpoints).

#### 1.2 F5 BIG-IP / Advanced WAF
- **What it is & What it does:** Bank enterprise Layer 7 Web Application Firewall inspecting all HTTP/S traffic for OWASP Top 10 exploits, protocol anomalies, credential stuffing, and application-layer DDoS.
- **Why Required:** Enforces AU Bank's central InfoSec security policies and compliance inspection rules across all inbound banking channels.
- **Alternatives Considered:**
  - *AWS WAF:* Native and lightweight, but does not offer the deep payload inspection, custom bank F5 iRules, and unified policy management mandated by the bank's central security team.
- **Why Best Suited:** Enterprise security standard of AU Bank; provides consistent compliance reporting, fine-grained L7 attack signature detection, and seamless InfoSec integration.

#### 1.3 External Application Load Balancer (External ALB)
- **What it is & What it does:** Public-facing AWS Application Load Balancer positioned behind Cloudflare and F5, serving as the ingress termination point before routing to Amazon API Gateway.
- **Why Required:** Provides a clean AWS entry point with managed SSL certificates, health checks, and cross-AZ traffic distribution.
- **Alternatives Considered:**
  - *Direct Cloudflare to API Gateway routing:* Bypasses internal AWS perimeter routing and limits custom target group health checking.
- **Why Best Suited:** High-throughput, resilient AWS edge ingress that terminates incoming traffic from F5/Cloudflare and forwards it securely via private VPC links.

#### 1.4 Amazon API Gateway (Proxy 1 of 2)
- **What it is & What it does:** Managed REST API Gateway enforcing request parameter validation, client throttling, API keys, and routing requests via private VPC Link to the internal ALB.
- **Why Required:** Acts as the primary API governance boundary, preventing unvalidated requests from entering workload subnets.
- **Alternatives Considered:**
  - *Direct ALB to Pod routing:* Bypasses API Gateway throttling and managed request schema validation.
  - *Self-hosted Kong on EKS:* Adds extra operational burden and state management on Kubernetes in R0.
- **Why Best Suited:** Serverless, highly scalable, Multi-AZ gateway providing seamless private VPC Link connectivity.

#### 1.5 Internal Application Load Balancer (Proxy 2 of 2)
- **What it is & What it does:** Private L7 load balancer in internal app subnets, receiving traffic from API Gateway VPC Link and load-balancing to EKS ingress controller pods.
- **Why Required:** Decouples API Gateway routing from dynamic Kubernetes pod IP churn; provides target-group level health checking and zero-downtime rolling deployment routing.
- **Alternatives Considered:**
  - *NLB (Network Load Balancer):* Lacks L7 path-based routing and internal HTTP header inspection.
- **Why Best Suited:** Clean L7 internal boundary with private ACM TLS termination.

#### 1.6 Transit Gateway (TGW) & Inspection / Egress VPC (`ADR-009`, `ADR-010`)
- **What it is & What it does:** Central routing hub connecting workload VPCs, on-premises bank data centers, and an Inspection VPC containing AWS Network Firewall and NAT Gateways.
- **Why Required:**
  - Insurers and 1SB require **static allowlisted Elastic IPs (EIPs)** for mTLS whitelisting.
  - RBI & Bank Cyber Security policies mandate **100% inspection of outbound traffic** (drop-by-default domain allowlist).
  - Isolates routing rules so a dev environment can never route to production Core Banking.
- **Alternatives Considered:**
  - *NAT Gateway inside each Workload VPC:* Spreads EIPs across accounts, fails central audit, and does not allow L7 stateful inspection.
  - *Direct VPC Peering:* Mesh complexity grows quadratically \(O(N^2)\); cannot enforce centralized firewall inspection.
- **Why Best Suited:** Segregates networking into a governed `network` account; provides automated failover from Direct Connect (DX) to Site-to-Site VPN; enables single-point outbound security inspection.

---

### 2. Client & Edge Applications

#### 2.1 Unified Flutter Enterprise Application (`NIP-APP` / `nip-web`) (`ADR-015`)
- **What it is & What it does:** A single Flutter codebase distributed across Web (`nip-web` hosted on EKS), Android (APK on Play Store / MDM), and iOS (IPA on App Store). Serves Bank RMs, Insurance Partner Reps (IPR), Branch Ops, and Admins.
- **Why Required:** Eliminates multi-app maintenance overhead. Role perspectives are determined dynamically by the Authorization PDP, not by maintaining separate apps.
- **Alternatives Considered:**
  - *Separate Web Apps for Admin, RM, and Partner:* Multiplies UI frameworks, design system drift, and security patch surfaces.
  - *Native Android/iOS apps:* Triples front-end engineering headcount with zero bancassurance benefit.
- **Why Best Suited:** Single design system, shared business logic and validations, offline form-caching capabilities, compiled for multi-channel reach.

#### 2.2 #2 NIP BFF (Backend-for-Frontend)
- **What it is & What it does:** The sole backend service exposed to `NIP-APP`. Manages **token-hiding sessions**, translates UI screen interactions into domain API calls, and injects principal identity into downstream calls.
- **Why Required:** **Security Invariant (`ARCH-019`):** Mobile/Web clients must never receive raw OAuth access/refresh tokens. The BFF stores OAuth tokens in ElastiCache Valkey and returns an opaque, encrypted HttpOnly session cookie to the client.
- **Alternatives Considered:**
  - *Direct Client-to-Microservices Architecture:* Leaks internal microservice topology and domain schemas to the frontend; forces client to handle complex distributed sagas.
  - *GraphQL Gateway:* Heavyweight schema governance; high CPU cost; complex caching and security authorization controls for R0.
- **Why Best Suited:** Lightweight Spring Boot stateless proxy layer; aggregates data per screen without embedding domain business rules.

---

### 3. Identity & Access Management (WS-2 Enabler)

#### 3.1 Keycloak (Private Workforce Identity Provider)
- **What it is & What it does:** Private OIDC/SAML IdP deployed in `ns:identity`, federated with Bank Active Directory (AD) via Transit Gateway.
- **Why Required:** Centralizes workforce identity authentication without hardcoding bank AD quirks directly into domain services.
- **Alternatives Considered:**
  - *AWS Cognito:* Lacks on-premise Active Directory Kerberos/SAML enterprise features; vendor lock-in; inflexible session token customization.
  - *Direct Bank AD LDAP Calls from Services:* Violates security boundaries and overloads enterprise directory services.
- **Why Best Suited:** Enterprise-grade open-source IdP, backed by Aurora PostgreSQL (`keycloak` schema), completely private with no internet ingress.

#### 3.2 #3 Identity Provider Adapter & Authorization PDP (Policy Decision Point)
- **What it is & What it does:**
  - *IdP Adapter:* Abstracts Keycloak/Cognito behind a provider-neutral interface.
  - *Authorization PDP:* Central Policy Decision Point executing RBAC + ABAC evaluation and evaluating **Specified Person (SP) certification attributes** at every regulated action.
- **Why Required:**
  - **IRDAI Obligation:** Regulated insurance actions may only be performed by a certified Specified Person. SP is an **attribute on the RM principal** (validity dates, LOB scope, certificate number), evaluated dynamically, not a static role checked at login.
  - **Default-Deny Invariant:** Authorization logic must fail closed (Seam `S-02`, 300 ms budget).
- **Alternatives Considered:**
  - *Embedded Role Checks in Services / Spring Security annotations:* Leads to fragmented, unauditable authorization policies scattered across 14 codebases.
  - *Open Policy Agent (OPA) / Cedar:* Adds external sidecar complexity in R0.
- **Why Best Suited:** Java-native PDP microservice with fine-grained caching; evaluates bank-specific compliance rules (SP validity, branch hierarchy, partner scoping).

---

### 4. Shared Platform Domain Services

#### 4.1 #19 Configuration Service (Wave 0b) (`ADR-007`)
- **What it is & What it does:** Central, versioned, append-only, effective-dated store managing journey steps, eligibility matrices, validation rules, document checklists, and insurer routing policies.
- **Why Required:** **Non-negotiable Rule:** A compliance rule change (e.g., IRDAI revised consent statement or suitability threshold) must never require a code deployment or pipeline run (`CF-1`…`CF-5`).
- **Alternatives Considered:**
  - *Application `application.yml` / GitOps ConfigMaps:* Requires pod restarts and full deployment pipelines to change business rules; fails audit trails.
  - *Spring Cloud Config / AWS AppConfig:* Lacks effective-dating (scheduling future rule activations) and domain-aware `(lob, productClass)` query resolution.
- **Why Best Suited:** Dedicated relational store with read-through cache; fail-closed behavior (no unvalidated fallback literals allowed).

#### 4.2 #5 Lead / Opportunity Service (Wave 1) (`ADR-005`, `ADR-014`)
- **What it is & What it does:** The **single origination entry point** for all on-platform insurance journeys (`AC-8`). Creates and manages the lead lifecycle (create, resume, assign, convert, archive).
- **Why Required:** Enforces that no quote, proposal, or policy can exist without an authenticated Bank RM origination record carrying the accountable Specified Person ID and LOB.
- **Alternatives Considered:**
  - *Starting journeys from Customer Lookup directly:* Creates an un-attributed funnel; violates IRDAI traceability.
  - *CRM-hosted Leads (Salesforce/LeadSquared):* Lacks microsecond synchronous integration with bank suitability gates and exposes PII to external SaaS.
- **Why Best Suited:** In-house domain aggregate; strict state machine; working inbox archives immediately upon conversion and policy issuance.

#### 4.3 #9 Journey Orchestration Service (Wave 1)
- **What it is & What it does:** State machine and saga coordinator managing the end-to-end customer journey progression and executing compensation workflows upon failure.
- **Why Required:** Prevents frontend BFFs from implementing fragmented workflow logic (`ARCH-005`). Holds journey state transitions and correlation IDs.
- **Invariant:** Owns **stage and references only**; never stores authoritative business decisions (e.g., does not decide suitability scores or calculate quote premiums).
- **Alternatives Considered:**
  - *AWS Step Functions:* High per-transition cost at scale, difficult to test locally in CI/CD without AWS mocks, vendor lock-in.
  - *Temporal / Camunda:* Heavy infrastructure footprint requiring dedicated Cassandra/Elasticsearch clusters in R0.
- **Why Best Suited:** Lightweight Spring Boot state machine persisting state snapshots to DynamoDB / Aurora, driven by deterministic events.

#### 4.4 #4 Customer Service (Wave 1) — EBS (Enterprise Service Bus) Integration
- **What it is & What it does:** Facade over Bank Core Systems integrating directly via **EBS (Enterprise Service Bus) APIs** to fetch Core Banking (CBS / CIF) customer snapshots, pre-fill journey forms, and validate KYC status over Transit Gateway.
- **Why Required:** Standardizes bank customer lookups through the enterprise-governed EBS layer rather than connecting directly to backend database tables or proprietary mainframe interfaces.
- **Alternatives Considered:**
  - *Direct database query to CBS mainframe:* Violates bank data ownership and risks mainframe table locks.
  - *Point-to-point SOAP integration to CBS:* Bypasses central bank ESB governance and monitoring.
- **Why Best Suited:** Caching facade with circuit breakers calling standard bank EBS REST/JSON APIs; protects CBS while enforcing PII masking.

#### 4.5 #8 Product Catalogue Service (Wave 1)
- **What it is & What it does:** Manages insurance product master data, rider definitions, premium rate matrices, and insurer eligibility rules.
- **Why Required:** Decouples product metadata from quotation engines. Allows RMs to view available plans before triggering external quote aggregators.
- **Alternatives Considered:**
  - *Querying 1SB Catalogue on every page load:* Slow (seconds latency), dependent on aggregator uptime, cannot overlay bank-specific commercial rules.
- **Why Best Suited:** Read-heavy service backed by ElastiCache Valkey L2 cache; ensures sub-100ms catalogue navigation for branch staff.

#### 4.6 #6 Consent Service (Wave 2)
- **What it is & What it does:** Captures, verifies, and immutably stores customer consent. Sends OTP to the **customer's registered device** and validates verification before proposal submission.
- **Why Required:** **Mandatory Gate C2:** IRDAI regulations strictly prohibit storing or processing customer proposals without verified, explicit, unexpired consent.
- **Alternatives Considered:**
  - *Storing consent flags inside Proposal Service:* Violates separation of compliance evidence; mutable proposal tables could compromise legal standing.
- **Why Best Suited:** Append-only persistence model with dedicated KMS customer-managed key (CMK); rejects any update or delete operations.

#### 4.7 #7 Suitability & Recommendation Service (Wave 2)
- **What it is & What it does:** Executes algorithmic suitability evaluation based on customer financial profile, life stage, risk appetite, and existing coverage (`SUIT-ALGO-LIFE-v1.0`). Generates immutable Suitability Evaluation PDF.
- **Why Required:** **Mandatory Gate C1:** Under IRDAI mis-selling regulations, no quote may be presented or policy sold without an unexpired suitability assessment.
- **Alternatives Considered:**
  - *UI-based checklist / checkbox:* Legally non-defensible; easily bypassed by compromised or fraudulent frontends.
- **Why Best Suited:** Hard-gate backend engine; returns signed `suitabilityAssessmentId` with TTL required by Quotation Service (Seam `S-08`).

---

### 5. Line-of-Business (LOB) Execution Services (`ns:life-cell`)

#### 5.1 #10 Quotation Service (Wave 2)
- **What it is & What it does:** Orchestrates quote requests, fans out quote requests to Integration Hub, polls for insurer offers, applies suitability filters, and manages quote selection.
- **Why Required:** Quotation represents a bursty, asynchronous, short-lived job lifecycle (polling 5+ insurers simultaneously), distinct from long-lived proposal state machines.
- **LOB Segregation (`ADR-006`):** Quotation is **LOB-owned execution**. Life Term quotes operate in this cell; future Health or Motor quotes deploy their own dedicated quotation pods to isolate failure domains.
- **Alternatives Considered:**
  - *Direct integration with 1SB:* Couples quotation domain model to external JSON/XML schemas.
  - *Shared Monolithic Quote/Proposal Service:* Blurs data lifecycle boundaries and causes table contention between high-churn quote jobs and long-term proposals.
- **Why Best Suited:** Uses DynamoDB for fast TTL-driven quote jobs and Aurora for finalized quotes; enforces Seam `S-08` suitability verification.

#### 5.2 #11 Proposal & Underwriting Tracking Service (Wave 3)
- **What it is & What it does:** Manages comprehensive proposal form prefill, medical/financial document checklist tracking, insurer underwriting submission, and underwriter status updates.
- **Why Required:** Proposals represent long-lived relational aggregates (lasting days to weeks) requiring strict ACID transactions and relational foreign keys between applicant, nominee, declarations, and requirements.
- **Alternatives Considered:**
  - *MongoDB Document Store:* Lacks cross-table ACID constraints for complex multi-party insurance cases; risk of schema drift across underwriting revisions.
- **Why Best Suited:** Hosted on Aurora PostgreSQL with strict relational integrity; supports multi-stage underwriting state transitions.

---

### 6. Fulfilment, Money Path & Compliance Services

#### 6.1 #12 Payment Service (Wave 3)
- **What it is & What it does:** Initiates customer payment sessions with AU Bank Payment Gateway, processes asynchronous payment callbacks, executes daily batch settlement reconciliation, and manages payment failure states.
- **Why Required:** **Mandatory Control C4 (RBI Compliance):** RMs are strictly forbidden from handling customer card/banking instruments. Payment Service issues payment links delivered directly to the **customer's personal device** via SMS/Email.
- **Alternatives Considered:**
  - *Embedding Payment inside Proposal Service:* Violates PCI-DSS and financial segregation; risks issuing policies on unreconciled or unverified transactions.
- **Why Best Suited:** Dedicated financial state machine; enforces invariant that payment is `UNCERTAIN` until PG webhook/reconciliation settles; enforces unique `paymentId` idempotency.

#### 6.2 #13 Policy & Issuance Service (Wave 3)
- **What it is & What it does:** Receives policy issuance confirmations from insurers, stores policy numbers and master policy PDFs, manages offline policy ingest (`source=OFF_PLATFORM`), and initiates servicing records.
- **Why Required:** **Standing Invariant `SC-W3-4`:** A policy can **never** be issued unless the associated payment status is verified as `RECONCILED`.
- **Alternatives Considered:**
  - *Directly marking policy as active upon payment gateway callback:* Payment gateway callbacks can be spoofed or subject to settlement chargebacks; leads to financial discrepancy.
- **Why Best Suited:** Single source of truth for in-force insurance policies; provides immutable document retrieval via S3 pre-signed URLs.

#### 6.3 #16 Audit & Compliance Service (Wave 3)
- **What it is & What it does:** Ingests domain events from the transactional outbox over Amazon MSK, indexes regulatory evidence, and archives raw transaction records into S3 Object Lock.
- **Why Required:** **Mandatory Control C7/C8:** IRDAI requires 7-year tamper-proof reconstruction of the entire sales chain (who sold, what suitability was calculated, what consent was captured, what quote was chosen).
- **Invariant:** A journey cannot transition to `SOLD` until the Audit Service acknowledges the durable evidence write (`INV-JRN-05`).
- **Alternatives Considered:**
  - *Centralized log aggregation (Elasticsearch/CloudWatch) as audit:* Logs can be purged, truncated, or modified; fails regulatory legal non-repudiation standards.
  - *Direct database writes from all services into an Audit DB:* Creates database contention and couples all microservices to a single central relational schema.
- **Why Best Suited:** Decoupled event-driven consumer writing to DynamoDB for fast operational queries and S3 Object Lock (Compliance Mode) for permanent 7-year WORM storage.

#### 6.4 #17 Notification Service (Wave 4)
- **What it is & What it does:** Transactional messaging worker consuming notification events from MSK to dispatch customer OTPs, payment links, proposal summaries, and policy welcome kits via SMS/Email.
- **Why Required:** Isolates third-party telecom gateway latency and outages from blocking core customer journey transactions.
- **Alternatives Considered:**
  - *Synchronous HTTP SMS calls from Payment/Consent services:* If the SMS provider degrades, core payment or consent APIs time out and fail.
- **Why Best Suited:** Asynchronous MSK consumer with retry queues and fallback SMS gateways; failure raises an operational alert without aborting the sales journey.

---

### 7. Integration & Supplier Layer (`ns:integration`)

#### 7.1 #14 Integration Hub (Wave 1)
- **What it is & What it does:** The routing and abstraction mediator standing between internal domain services and external provider adapters. Injects bank attribution (`distributorId`), manages per-provider connection pools and circuit breakers, and enforces standardized canonical data exchange.
- **Why Required:** **Strategic Invariant:** Guarantees that Quotation and Proposal services never import or know about 1SilverBullet or direct insurer wire formats (`INV-ACL-01`).
- **Alternatives Considered:**
  - *Direct microservice-to-adapter calls:* Couples 4+ domain services to provider-specific nuances; makes adding a direct insurer require sweeping code changes across the platform.
- **Why Best Suited:** Centralized traffic governor; provides per-insurer bulkhead isolation so one failing insurer cannot exhaust connection threads for other insurers.

#### 7.2 #15 1SilverBullet (1SB) Adapter (WS-1 Supplier)
- **What it is & What it does:** Translates bank-canonical quote and proposal requests into 1SilverBullet's proprietary API protocol, manages mTLS transport, handles asynchronous quote polling, and archives raw XML/JSON request/response payloads.
- **Why Required:** Encapsulates all 1SB integration complexity in an Anti-Corruption Layer (ACL).
- **Alternatives Considered:**
  - *In-line transformation within Integration Hub:* Blurs routing logic with protocol parsing; breaks modularity when direct insurer adapters are introduced in Phase B.
- **Why Best Suited:** Proven, isolated adapter with independent deployment lifecycle and dedicated S3 payload archiving.

---

### 8. Data, Caching & Messaging Tier

```
+-----------------------------------------------------------------------------------------------------------------------------------+
| DATA PERSISTENCE & MESSAGING SELECTION MATRIX                                                                                     |
+--------------------------+-----------------------+------------------------------------------+-------------------------------------+
| Technology Component     | Primary Role          | Rejection of Alternative                 | Architectural Guardrail / Rule      |
+--------------------------+-----------------------+------------------------------------------+-------------------------------------+
| Aurora PostgreSQL        | Transactional Core    | MongoDB rejected for Proposal/Case Mgmt  | 1 Cluster in R0 (ADR-008); isolated |
| (Multi-AZ)               | (ACID Relational)     | due to lack of strict relational schema. | schema per context. Cross-DB joins  |
|                          |                       | Cluster-per-service rejected (cost/ops). | strictly prohibited by ArchUnit.    |
+--------------------------+-----------------------+------------------------------------------+-------------------------------------+
| Amazon DynamoDB          | Fast Key-Value &      | Relational DB rejected for high-churn    | Point-in-time recovery (PITR) on.   |
|                          | High-Churn State      | quote jobs due to table vacuum overhead. | Dedicated CMK encryption.           |
+--------------------------+-----------------------+------------------------------------------+-------------------------------------+
| Amazon S3 + Object Lock  | 7-Year Regulatory     | Standard EBS/EFS rejected because files  | Compliance Mode Object Lock.        |
| (Compliance Mode)        | WORM Evidence Store   | can be deleted/altered by admin users.   | Cross-region replication to DR.     |
+--------------------------+-----------------------+------------------------------------------+-------------------------------------+
| ElastiCache for Valkey   | Sessions, L2 Cache,   | DynamoDB sessions withdrawn (ADR-011).   | NEVER store idempotency records.    |
|                          | Rate-Limit Counters   | Redis cluster-mode rejected for R0 size. | NEVER serve config past TTL.        |
+--------------------------+-----------------------+------------------------------------------+-------------------------------------+
| Amazon MSK               | Event Backbone &      | Direct publish without outbox rejected   | Outbox in DB remains single source  |
| (Managed Kafka)          | Domain Fan-Out        | (dual-write data loss risk).             | of truth. Topic is transport only.  |
+--------------------------+-----------------------+------------------------------------------+-------------------------------------+
| Amazon OpenSearch        | Operational Logs &    | Ingesting regulatory evidence rejected;  | VPC-only; 30-day hot retention;     |
| (VPC-Only)               | SRE Diagnostic Search | CloudWatch alone lacks full-text grep.   | holds ZERO compliance evidence.     |
+--------------------------+-----------------------+------------------------------------------+-------------------------------------+
```

#### 8.1 Persistence Rationale: Aurora PostgreSQL vs MongoDB (`ADR-008`)
- **Decision:** Use **Aurora PostgreSQL** as the core relational engine, structured as **one cluster with an isolated schema per bounded context** in R0.
- **Why not MongoDB for Proposals:**
  1. *Relational Invariants:* Insurance proposals are highly constrained structures requiring multi-table referential integrity (applicants, nominees, riders, declarations, medical questionnaires). Relational foreign keys prevent corrupt states at write time.
  2. *Audit & Reconstruction:* Regulators require deterministic tabular reconstruction of policy case files.
  3. *Operational Familiarity:* The bank's DBA and operations teams possess deep PostgreSQL expertise; MongoDB would introduce an unnecessary second database operational stack.
- **Why not Cluster-per-Service in R0:** 14 separate database clusters for a pilot carrying ~100 journeys/hour represents massive cost waste and operational overhead. Schema isolation with distinct IAM credentials provides identical logical boundary enforcement at a fraction of the cost.

#### 8.2 Caching Strategy & Valkey Defense (`ADR-011`)
- **Decision:** Provision **ElastiCache for Valkey** for:
  1. BFF token-hiding session storage.
  2. Product Catalogue L2 read-through cache.
  3. Configuration resolution cache (strictly expiring at TTL).
  4. Per-principal rate limiting counters.
- **Strict Non-Uses (Defended at ARB):**
  - **NEVER use Cache for Idempotency:** Idempotency records must be written in the **exact same ACID database transaction** as the business entity change. Storing idempotency in Redis causes the dual-write failure: Redis succeeds, DB fails, and a retry is falsely rejected as a duplicate.
  - **NEVER serve Configuration past TTL:** When a compliance rule expires, the service must re-resolve from the source of truth or fail closed; serving stale compliance rules is illegal.
  - **Deliberately Absent from DR Region:** Because the cache holds zero durable data, it does not require cross-region replication; it warm-starts and populates from Aurora on first read.

#### 8.3 Event Backbone & Transactional Outbox Pattern (`ADR-012`)
- **Decision:** Deploy **Amazon MSK** (3 brokers across 3 AZs), fed strictly via the **Transactional Outbox Pattern**.
- **The Core Problem Solved:** Direct publishing to Kafka after committing a database transaction creates the dual-write vulnerability (if the service crashes between DB commit and Kafka publish, the event is lost forever).
- **The Mechanism:** The microservice writes the business entity change and the outbox event row in a **single local database transaction**. A lightweight background publisher worker (`outbox-publisher`) reads the outbox table and dispatches events to MSK with guaranteed at-least-once delivery.
- **Regulatory Invariant:** MSK is a transport pipe, not an audit store. Audit events are consumed from MSK and written to S3 WORM; the topic retention is operational (7 days), while the S3 archive is regulatory (7 years).

---

### 9. DevSecOps, Infrastructure as Code & Observability Baseline

#### 9.1 GitLab CI/CD (Enterprise Delivery Standard)
- **What it is & What it does:** Centralized source control and CI/CD platform executing automated builds, ArchUnit tests, JaCoCo coverage gates, security SAST/DAST scans, and automated Terraform deployments.
- **Why Required:** Standardizes deployment pipelines across all microservices and enforces strict gate controls before merging to `main`.
- **Alternatives Considered:**
  - *GitHub Actions / Jenkins:* GitLab is AU Bank's existing, approved enterprise pipeline runner with native audit trails, runner autoscaling, and secret isolation.
- **Why Best Suited:** Bank enterprise standard; provides native integration with bank artifact repositories, SonarQube, and container vulnerability scanners.

#### 9.2 Terraform for Infrastructure as Code (IaC) & Ansible for Automation
- **What it is & What it does:**
  - **Terraform:** Declarative Infrastructure as Code defining 100% of AWS cloud resources (VPCs, TGW, EKS, Aurora, Valkey, MSK, IAM roles, Security Groups, and KMS CMKs).
  - **Ansible:** Automation engine for disaster recovery (DR) failover drills, DX→VPN network failover drills, outbox replay drills, and post-deployment environment sanity checks.
- **Why Required:** Guarantees reproducible, immutable, auditable environment provisioning across `dev`, `uat`, `prod`, and `dr`, while providing automated proof of recovery and health (`Gate S09-G4/G7`).
- **Alternatives Considered:**
  - *AWS CloudFormation / CDK:* Terraform is multi-account, cloud-agnostic, bank standard, and supports policy-as-code linting (tfsec/checkov) in GitLab CI.
  - *Manual DR / Sanity Runbooks:* Manual failover is prone to human error and fails the RTO ≤ 1 h measured demonstration mandate.
- **Why Best Suited:** Prevents manual AWS console drifts; integrates directly with bank security scanning pipelines; automated drills prove recovery capability without manual intervention.

#### 9.3 AWS CloudTrail vs Amazon CloudWatch (Both Mandatory)
- **What they are & Why both are required:**
  - **AWS CloudTrail (Management & Security Governance):** Logs every AWS API call made by users, roles, and automated pipelines across all AWS accounts. Essential for InfoSec, SOC monitoring, and non-repudiation of infrastructure changes.
  - **Amazon CloudWatch (Operational Metrics & Service Observability):** Collects application metrics, container CPU/memory usage, p95/p99 latency timers, and triggers automated alerts and pager rotations.
- **Why Neither Replaces the Other:** CloudTrail answers *"Who modified this IAM policy or database configuration in AWS?"*, while CloudWatch answers *"What is the CPU usage and HTTP 5xx error rate of the Quotation pod?"*. Both are non-negotiable for enterprise bank operations.

---

## Non-Negotiable Regulatory & Architecture Invariants

The platform enforces the following hard invariants across all codebases and infrastructure deployments:

1. **Gate C1 (Suitability Hard Gate):** No quotation can be generated without a valid, unexpired `suitabilityAssessmentId` generated by Context #7.
2. **Gate C2 (Consent Hard Gate):** No proposal can be submitted to an insurer without an unexpired customer OTP consent grant recorded in Context #6.
3. **Gate C4 (Payment Device Isolation):** Premium payment links are dispatched exclusively to the customer's personal device. No API endpoint exists allowing an RM or bank terminal to execute customer payment.
4. **Attribution Integrity (Gate C3):** The `distributorId` is injected exclusively by Context #14 (Integration Hub). Any client-supplied attribution header is rejected.
5. **Core Banking via EBS APIs:** Customer profile retrieval and CIF queries must route via bank EBS APIs; direct database connections to core banking are forbidden.
6. **No Direct Microservice Database Access:** No microservice may connect to or query the schema of another microservice. Data exchange is strictly via REST APIs or MSK domain events.
7. **No PII in Logs:** Logs are scrubbed of PAN, Aadhaar, Mobile, and Bank Account numbers at the logger appender level, validated by automated CI test suites (`FF-05`).
8. **Audit Immutability (Gate C7/C8):** Context #16 Audit tables and S3 archives reject all `UPDATE` and `DELETE` requests at both database and IAM policy levels.
9. **Fail-Closed Policy Resolution:** If Context #19 Configuration or WS-2 Authorization is unreachable, the calling service must fail closed (deny request). Fallback to compiled-in default constants is forbidden.

---

## ARB Presentation Defense Script & Anticipated Q&A

### Question 1: "Why use Cloudflare and F5 BIG-IP instead of standard AWS edge services?"
**Defense Answer:**
> "Adopting **Cloudflare Enterprise** and **F5 BIG-IP** aligns our platform directly with AU Bank's existing cybersecurity framework and enterprise tooling. Cloudflare provides carrier-grade global DDoS mitigation and Indian edge acceleration, while F5 BIG-IP enforces central InfoSec inspection rules and compliance policies. This ensures that the insurance platform conforms to the bank's perimeter standard rather than introducing a siloed security stack."

### Question 2: "How does the platform integrate with Core Banking (CBS)?"
**Defense Answer:**
> "All core customer lookups and CIF queries route through **#4 Customer Service** calling **EBS (Enterprise Service Bus) APIs** over private Transit Gateway links. This adheres to bank enterprise architecture by leveraging the governed EBS integration layer rather than making direct point-to-point connections to the CBS mainframe."

### Question 3: "Why do we need 14 microservices for an R0 pilot with only ~100 journeys an hour? Isn't this over-engineered?"
**Defense Answer:**
> "The service count is dictated by **regulatory boundaries and independent failure domains**, not by transaction volume. In bancassurance, Suitability (C1), Consent (C2), Payment (C4), and Audit (C7) represent distinct legal and compliance obligations. Merging them into a single monolithic 'Sales Service' makes it impossible to provide legally defensible, immutable audit trails, and causes any minor change in a payment gateway integration to force redeployment and re-certification of our suitability engine. Furthermore, our deployment is highly efficient: all 14 services run as lightweight stateless containers in a single EKS cluster sharing one Aurora database cluster with schema-level isolation. We get clean domain boundaries without multiplying infrastructure costs."

### Question 4: "Why can't we use MongoDB for Quotes and Proposals since insurance forms have dynamic schemas?"
**Defense Answer:**
> "We evaluated MongoDB and rejected it for two critical reasons:
> 1. **Data Lifecycle Divergence:** Quotation represents bursty, short-lived, high-churn key-value state (which we optimize using Amazon DynamoDB with automatic TTLs). Proposal, by contrast, is a multi-week relational case file requiring strict ACID guarantees across applicants, nominees, medical declarations, and payment references.
> 2. **Integrity & Operational Skill:** In a regulated bank, relational constraints in Aurora PostgreSQL prevent corrupt underwriting states from ever being written. Additionally, our DBA and platform teams already operate PostgreSQL; introducing MongoDB creates a second operational database stack with zero tangible access-pattern benefits."

### Question 5: "Why are both AWS CloudTrail and Amazon CloudWatch mandatory, and what is the role of Ansible alongside Terraform?"
**Defense Answer:**
> "1. **CloudTrail vs CloudWatch:** They serve fundamentally different, non-overlapping enterprise requirements:
>    - **CloudTrail** provides immutable governance and security audit logging of all AWS management API actions (e.g., who provisioned a resource, modified a security group, or accessed KMS).
>    - **CloudWatch** provides runtime operational telemetry, application logs, container metrics, and latency alarms for our engineering and SRE teams.
>    A banking platform requires both to satisfy RBI cybersecurity auditability and 24/7 operational reliability.
> 2. **Terraform vs Ansible:**
>    - **Terraform** manages 100% immutable Infrastructure as Code (IaC) provisioning across AWS accounts.
>    - **Ansible** automates operational execution: DR failover drills, DX→VPN failover validation, outbox replay drills, and post-deployment sanity testing across private endpoints, ensuring that recovery capabilities (RTO ≤ 1 h) are mathematically demonstrated rather than asserted."

---

## Conclusion & Board Approval Request

This architecture dossier provides a comprehensive, mathematically grounded, and regulatory-compliant foundation for the AU Bank Insurance Distribution Platform. It fulfills all IRDAI mandates, enforces bank security policies, eliminates vendor lock-in, and provides a clear evolution path from R0 Term Life to full multi-LOB bancassurance distribution.

**Recommendation to ARB:**  
**Approve the R0 Solution Architecture, BOM, and associated ADRs (`ADR-001` through `ADR-016`) for progression into S09 Platform Infrastructure Provisioning using Terraform and GitLab CI/CD.**
