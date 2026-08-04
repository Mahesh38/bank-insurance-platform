# AU Small Finance Bank (AU SFB) Insurance Distribution Platform — Strategic Brainstorming, Roadmap & Action Plan

**Document Version:** 1.0  
**Target Directory:** `docs/contextRoles/brainstorming_roadmap_action_plan.md`  
**Consolidated Persona Panel:**  
- 📋 **Rajal** — Product Owner (Business Requirements, IRDAI Compliance & Customer Journeys)  
- 🏛️ **Mahesh** — Solution Architect (System Topology, 19 Bounded Contexts & ADRs)  
- ⚙️ **Amit** — Technical Head (AWS EKS Infrastructure, CI/CD Quality Gates & Squad Delivery)  

**Regulatory License:** IRDAI Composite Corporate Agent License (**Registration No. CA0515**)  
**Core Technology Stack:** Java 21 / Spring Boot 3.3.4 (Gradle Monorepo), Flutter, AWS EKS, PostgreSQL, DynamoDB, Redis, AWS MSK, 1SilverBullet Aggregator Abstraction  

---

## 1. Executive Summary & Problem Statement Refinement

The legacy **AU Beema Portal** operated as a basic redirect gateway. Once an RM or customer selected an insurance product, the session was redirected to an external insurer portal. This introduced a **critical operational blind spot**: AU SFB lost 100% visibility post-redirection over funnel drop-offs, underwriting queries, payment statuses, and policy issuances.

Through this collaborative brainstorming session, **Rajal**, **Mahesh**, and **Amit** have aligned on transforming AU SFB's insurance distribution into a **bank-owned, multi-insurer digital insurance platform**.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        AS-IS: AU BEEMA PORTAL (REDIRECT BLIND SPOT)                    │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ RM/Customer ──► [AU Beema Portal] ──► [Redirect to Insurer Web Portal]                 │
│                                                   │                                    │
│                                                   ▼                                    │
│                                        [BANK LOSES 100% VISIBILITY]                    │
│                                   (Drop-offs, Payment, Issuance Unknown)               │
└────────────────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────────────────┐
│                   TO-BE: AU SFB BANK-OWNED INSURANCE PLATFORM (TARGET)                 │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ [RM / App] ──► [AU Identity & CBS] ──► [Suitability Gate] ──► [1SB Multi-Quote Engine] │
│                      │                                                                 │
│                      ▼                                                                 │
│ [Dynamic Proposal] ──► [Digital Consent Log] ──► [Bank PG Link on Cust Device] ──► COI│
│                      │                                                                 │
│                      ▼                                                                 │
│      [100% End-to-End Funnel Visibility, Audit Trail & Instant Reconciliation]        │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### Core Non-Negotiable Pillars Agreed by the Panel
1. **IRDAI CA0515 Compliance:** Mandatory attribution to AU Bank Corporate Agency ID (`distributorId`) and certified RM Specified Person ID (`spLicenseId`).
2. **Suitability Hard-Gate:** Mandatory need analysis and risk profiling before fetching quotes. Un-evaluated requests return `HTTP 403 Forbidden`.
3. **Immutable Digital Consent:** Customer consent captured via OTP/SMS and stored in append-only storage with 7-year WORM retention on Amazon S3.
4. **RBI Device Payment Isolation:** Zero premium payment processing on RM devices. Payment URLs are transmitted to customer personal devices for execution via AU Bank Payment Gateway.
5. **Strict 4-Step Definition of "Policy Sold":** A policy is declared Sold ONLY when:
   - Insurer issues policy contract & Certificate of Insurance (COI).
   - Bank receives valid API webhook confirmation.
   - Financial reconciliation completes against AU PG.
   - Policy record and audit evidence are persisted in bank datastores.

---

## 2. Target System Topology & Architecture Blueprint

Mahesh (Solution Architect) and Amit (Technical Head) have finalized the cloud-native microservices architecture on **AWS EKS** across `ap-south-1` (Mumbai) and `ap-south-2` (Hyderabad DR):

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                          Flutter Cross-Platform Client Layer                           │
│        (RM-Assisted Tablet/Web App  |  Customer Self-Service Mobile/Web App)          │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ HTTPS / TLS 1.3 (JSON REST API)
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│                    AWS API Gateway / ALB Ingress Controller                            │
│           (WAF, DDoS Protection, Rate Limiting, Central JWT Authorization)              │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Internal Mesh / gRPC / REST
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│                             AWS EKS Microservices Cluster                              │
│                                                                                        │
│ ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────────────────┐ │
│ │ Identity & Access    │ │ Customer Service     │ │ Lead & Opportunity               │ │
│ └──────────────────────┘ └──────────────────────┘ └──────────────────────────────────┘ │
│ ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────────────────┐ │
│ │ Product Catalogue    │ │ Suitability Engine   │ │ Consent Service (Append-Only)    │ │
│ └──────────────────────┘ └──────────────────────┘ └──────────────────────────────────┘ │
│ ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────────────────┐ │
│ │ Quotation Service    │ │ Proposal & UW Svc    │ │ Journey Orchestration (Saga)     │ │
│ └──────────────────────┘ └──────────────────────┘ └──────────────────────────────────┘ │
│ ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────────────────┐ │
│ │ Payment Service      │ │ Policy & Issuance    │ │ 1SB Adapter Gateway (ACL Layer)  │ │
│ └──────────────────────┘ └──────────────────────┘ └──────────────────────────────────┘ │
│ ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────────────────┐ │
│ │ Audit & Compliance   │ │ Notification Service │ │ Administration & Config          │ │
│ └──────────────────────┘ └──────────────────────┘ └──────────────────────────────────┘ │
└──────────────┬────────────────────────────┬─────────────────────────────┬──────────────┘
               │                            │                             │
┌──────────────▼──────────────┐ ┌───────────▼──────────────┐ ┌────────────▼─────────────┐
│  Amazon Aurora PostgreSQL   │ │    Amazon DynamoDB       │ │   Amazon ElastiCache Redis│
│ (Database-Per-Service, RDS) │ │(State, Jobs, Audit Trail)│ │ (Idempotency, UI Caching) │
└─────────────────────────────┘ └──────────────────────────┘ └───────────────────────────┘
               │                            │                             │
┌──────────────▼────────────────────────────▼─────────────────────────────▼──────────────┐
│                  AWS MSK (Managed Streaming for Apache Kafka Event Bus)                │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Asynchronous Event Archive & Analytics
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│                Amazon S3 (7-Year WORM Object Lock & Policy Document Store)             │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Architecture Decision Records (ARCH-001 to ARCH-010)

| ADR ID | Decision Title | Architectural Strategy & Rationale | Status |
| :--- | :--- | :--- | :---: |
| **ARCH-001** | **Hexagonal Anti-Corruption Layer** | Core microservices interact strictly with Canonical Data Models (CDM). The `1sb-adapter-service` maps CDM to aggregator schemas, ensuring zero lock-in. | Approved |
| **ARCH-002** | **Database-Per-Service Rule** | Every business microservice owns its private Aurora PostgreSQL schema. Cross-service DB joins are strictly forbidden; communication occurs via REST or Kafka. | Approved |
| **ARCH-003** | **AD SSO + SP License Gate** | Real-time verification of RM Specified Person (SP) certification against compliance DB. Expired/suspended SPs are blocked with `HTTP 403 Forbidden`. | Approved |
| **ARCH-004** | **Resilience4j Circuit Breakers** | Outbound 1SB calls wrapped with 5.0s timeouts, failure rate thresholds (50%), and asynchronous fallback queues to shield core services. | Approved |
| **ARCH-005** | **Cryptographic Audit Ledger** | Event sourcing log with SHA-256 hash chaining archived to Amazon S3 configured with 7-Year WORM Object Lock for IRDAI compliance. | Approved |
| **ARCH-006** | **Device Payment Isolation** | Payment URLs generated as signed, single-use links sent via SMS/WhatsApp to customer devices. Zero payment capture on RM tablets/phones. | Approved |
| **ARCH-007** | **Orchestrated Saga Pattern** | Journey state transitions managed by `Journey Orchestration Service` using DynamoDB for state and AWS MSK for event signals. | Approved |
| **ARCH-008** | **Redis Idempotency Locks** | Distributed idempotency locks (`SHA256(cif + productId + inputs)`) with 60s TTL preventing redundant insurer quote fan-outs. | Approved |
| **ARCH-009** | **Logback PII Masking & KMS** | Log-level masking using `PiiMaskingConverter` and AES-256 KMS field-level encryption for customer financial/KYC data at rest. | Approved |
| **ARCH-010** | **Dual-Track Insurer Strategy** | Group A (API integrated via 1SB) for in-platform sales; Group B (Catalog only) for controlled SAML/OAuth redirect journeys. | Approved |

---

## 4. Product Backlog Epics & Gherkin Acceptance Criteria

Rajal (Product Owner) has defined the core epic backlog for Phase 1:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ EP-01: Authentication & AD / SP License Verification                                   │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ EP-02: User Hierarchy & Permission Management (Bank & IP Roles)                        │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ EP-03: Lead Management & Bank CBS Customer Data Prefill                                │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ EP-04: Mandatory Need Analysis & Suitability Module (HTTP 403 Gate)                    │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ EP-05: Multi-Insurer Quote Comparison & Sharing                                        │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ EP-06: Dynamic Proposal Capture & Customer Digital Consent                             │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ EP-07: AU Bank Payment Gateway Trigger & Policy Issuance Tracking                       │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Unified Strategic Roadmap (Weeks 1 to 40)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ Phase 0: Infrastructure Foundation & Core Scaffolding (Weeks 1–8)                       │
│ • AWS EKS Landing Zone  • GitLab CI/CD & 80% JaCoCo Gates  • Gradle Monorepo Baseline   │
│ • AD Auth & SP Gate     • 1SB Gateway Adapter Baseline     • Database-per-Service Setup │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│ Phase 1: Core Sale Path - Life LOB RM-Assisted Journey (Weeks 9–22)                    │
│ • Suitability Engine    • 1SB Multi-Quote Compare Engine • Dynamic Proposal JSONB       │
│ • Customer Payment Link • Policy Issuance & Sold Recon   • Bulk IP User Onboarding      │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│ Phase 2: Compliance, Governance & Operational Hardening (Weeks 23–32)                  │
│ • Immutable Audit S3 WORM • Notification Service Engine  • 500 TPS K6 Load Testing     │
│ • Pen-Testing & Security  • Automated DR Failover Drill  • APM Distributed Tracing      │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│ Phase 3: Extend, Scale & Future LOB Preparation (Weeks 33–40)                           │
│ • Group B Catalog Redirect• Health & General LOB Adapters • Customer DIY App Flow       │
│ • Redshift ETL Analytics  • NTB V-KYC Customer Onboard   • Go-Live Production Hardening │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Master Prioritized Action Plan (P0 to P3)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ P0: CRITICAL (PHASE 0 & PHASE 1 BLOCKERS — MVP SCOPE)                                 │
│ • AWS EKS Multi-AZ Cluster & IaC Terraform Scaffolding                                 │
│ • GitLab CI/CD Automation Pipelines with 80% JaCoCo Line / 70% Branch Coverage Gates   │
│ • Central AuthN/AuthZ Service (AD SSO Federation + Real-Time SP License Gate)          │
│ • 1SB Canonical Adapter & Anti-Corruption Layer (ACL) Setup                            │
│ • Database-per-Service Aurora PostgreSQL Schemas & Flyway Migration Baseline            │
│ • Suitability & Need Analysis Engine with Mandatory HTTP 403 Regulatory Hard-Gate      │
│ • RBI Payment Isolation Link Generation via AU Bank Payment Gateway                    │
│ • Policy Issuance Webhook Handler & Strict 4-Tier "Policy Sold" Verification           │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ P1: HIGH (PHASE 1 OPERATIONAL LAUNCH ENHANCEMENTS)                                     │
│ • Multi-Insurer Parallel Quote Fan-Out Engine with Redis Idempotency Locks             │
│ • Dynamic Insurer Proposal Engine supporting JSONB Schemas & Medical Questionnaires    │
│ • Customer Digital Consent Service via SMS/OTP Verification & Event Evidence           │
│ • Bulk Insurer Partner (IP) User Onboarding Engine using Spring Batch (50k records)    │
│ • Cross-Platform Flutter RM Mobile App & Customer Web Portal UI Engine                 │
│ • Journey Orchestration Saga State Machine (DynamoDB + MSK Kafka Event Bus)            │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ P2: MEDIUM (PHASE 2 COMPLIANCE & OPERATIONAL HARDENING)                                │
│ • Immutable Regulatory Audit Log Service (DynamoDB Event Ledger + SHA-256 + S3 WORM)   │
│ • Asynchronous Notification Service (SMS, Email, Push via Kafka Consumers)             │
│ • System Performance Tuning & K6 Load Testing (500 TPS Peak, <200ms p95 SLA)           │
│ • Logback `PiiMaskingConverter` & KMS Field-Level Data Encryption                      │
│ • Security Pen-Testing Remediation & Automated Multi-AZ DR Failover Drills             │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ P3: FUTURE (PHASE 3 SCOPE — EXTENSIONS & ADVANCED CAPABILITIES)                        │
│ • Group B Non-Aggregated Insurer Catalog & Controlled Redirect Engine                  │
│ • Standalone Health & General Insurance LOB Adapters via 1SB Gateway                   │
│ • Customer Self-Service (DIY) Journey via AU Mobile / Net Banking SSO                  │
│ • New-To-Bank (NTB) V-KYC Integration & Instant CIF Onboarding                         │
│ • S3 Data Lake & AWS Redshift ETL Pipeline for Business Intelligence & Commission Recon│
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Comprehensive RACI Responsibility Matrix

To establish operational accountability across Product, Architecture, Engineering, Security, and Operations:

* **R = Responsible:** Role that executes the task/code.
* **A = Accountable:** Role with final decision authority.
* **C = Consulted:** SME providing domain/technical input.
* **I = Informed:** Kept updated on progress.

| Platform Deliverable / Activity | Product Owner (Rajal) | Solution Architect (Mahesh) | Technical Head (Amit) | Lead Dev (Java) | DevOps Lead | SecOps Lead | QA Lead |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **BRD / PRD Ownership & Scope Defense** | **A / R** | C | C | I | I | I | I |
| **Jira Epics (EP-01 to EP-07) & Gherkin AC**| **A / R** | C | C | I | I | I | C |
| **IRDAI CA0515 & RBI Regulatory Sign-off**| **A / R** | C | C | I | I | C | I |
| **System Architecture Topology & ADRs** | I | **A / R** | C | C | C | C | I |
| **1SB Canonical Adapter & Schema Mapping** | I | **A / R** | C | R | I | I | I |
| **Database-per-Service PostgreSQL Schemas** | I | **A / R** | C | R | I | I | I |
| **AWS EKS Infrastructure & Terraform IaC** | I | C | **A** | I | **R** | C | I |
| **GitLab CI/CD & JaCoCo 80% Quality Gates** | I | C | **A** | C | **R** | I | C |
| **AD AuthN & Redis SP License Gate** | C | C | **A** | **R** | I | C | I |
| **Suitability & Consent Hard-Gate Engines** | C | C | **A** | **R** | I | I | C |
| **Resilience4j Circuit Breakers (1SB 5s)** | I | C | **A** | **R** | I | I | C |
| **Spring Batch Bulk IP Engine (50k)** | I | C | **A** | **R** | I | I | I |
| **AU PG Payment Isolation Link Engine** | C | C | **A** | **R** | I | C | C |
| **Immutable Audit Engine & S3 7-Yr WORM** | C | C | **A** | **R** | C | C | I |
| **Automated K6 Load Testing Pyramid** | I | I | **A** | C | C | I | **R** |
| **Multi-AZ DR Drills & Security Hardening**| I | C | **A** | I | **R** | **R** | I |
