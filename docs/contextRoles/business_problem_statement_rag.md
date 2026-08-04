# AU Small Finance Bank (AU SFB) Insurance Distribution Platform — Comprehensive Business & Problem Statement RAG Context

**Document Status:** Comprehensive Business & Architecture RAG Context (V2.0)  
**Target File Location:** `docs/contextRoles/business_problem_statement_rag.md`  
**Consolidated Knowledge Base:** Synthesized from `docs/au-bank-insurance-platform/*` and `docs/architecture-review/*`  
**License & Entity:** AU Small Finance Bank (IRDAI Composite Corporate Agent License Reg. No. CA0515)  
**Target Infrastructure:** AWS (EKS, Aurora PostgreSQL, MSK, DynamoDB), Java 21 / Spring Boot 3.3.4, Flutter, Active Directory, 1SilverBullet Aggregator Abstraction  

---

## Table of Contents
1. [Executive Summary & Regulatory Framework](#1-executive-summary--regulatory-framework)
2. [Partner Insurer Ecosystem (Multi-Sector Distribution)](#2-partner-insurer-ecosystem-multi-sector-distribution)
3. [As-Is State vs. Target Platform Vision](#3-as-is-state-vs-target-platform-vision)
4. [Customer Journeys & Customer Segment Matrix](#4-customer-journeys--customer-segment-matrix)
5. [Operational Personas, Organizational Hierarchies & Identity Engine](#5-operational-personas-organizational-hierarchies--identity-engine)
6. [Business Capabilities & Bounded Contexts](#6-business-capabilities--bounded-contexts)
7. [Target Architecture & Technology Stack](#7-target-architecture--technology-stack)
8. [Phased Delivery & Implementation Roadmap](#8-phased-delivery--implementation-roadmap)
9. [Regulatory Compliance, Audit & Security Controls](#9-regulatory-compliance-audit--security-controls)
10. [RAG System Context & Usage Directives](#10-rag-system-context--usage-directives)

---

## 1. Executive Summary & Regulatory Framework

### 1.1 IRDAI Registration & Open Architecture Mandate
**AU Small Finance Bank (AU SFB)** is registered with the **Insurance Regulatory and Development Authority of India (IRDAI)** under a **Composite Corporate Agent License (Registration No. CA0515)**. 

Under IRDAI Corporate Agency open architecture regulations, a Composite Corporate Agent is legally empowered to tie up with multiple insurance companies across three distinct Lines of Business (LOBs):
1. **Life Insurance**
2. **General Insurance (Non-Life)**
3. **Health Insurance (Standalone)**

This license allows AU SFB to distribute products from multiple public and private insurers concurrently, offering tailored risk-mitigation and investment solutions to its retail, MSME, HNI, and corporate banking clients across India.

### 1.2 Regulatory & Compliance Directives
* **Corporate Agency Attribution:** IRDAI requires explicit, non-repudiable attribution of every policy sold to the registered Corporate Agent (**AU Bank Distributor ID**) and the certified sales representative (**Specified Person / SP License ID**).
* **Mandatory Need Analysis & Suitability Assessment:** IRDAI guidelines require that product recommendations be strictly preceded by documented customer suitability analysis. Bypassing suitability check prior to quote rendering is illegal.
* **Consent & Disclosures:** Digital customer consent must be explicitly captured, timestamped, and stored immutably with full audit trails.
* **RBI Payment Isolation:** RBI cyber-security guidelines prohibit accepting insurance premium payments on bank employee / RM mobile devices. Payment execution must happen on the customer's personal device via AU Bank Payment Gateway.

---

## 2. Partner Insurer Ecosystem (Multi-Sector Distribution)

AU SFB maintains active distribution tie-ups with leading public and private insurers, organized into three primary sectors:

```
                                  ┌──────────────────────────────────────────┐
                                  │      AU SFB Partner Insurer Ecosystem    │
                                  └────────────────────┬─────────────────────┘
                                                       │
         ┌─────────────────────────────────────────────┼─────────────────────────────────────────────┐
         ▼                                             ▼                                             ▼
┌─────────────────────────────────┐           ┌─────────────────────────────────┐           ┌─────────────────────────────────┐
│     Life Insurance Partners     │           │    Health Insurance Partners    │           │   General Insurance Partners    │
├─────────────────────────────────┤           ├─────────────────────────────────┤           ├─────────────────────────────────┤
│ • LIC of India                  │           │ • Niva Bupa Health Insurance    │           │ • United India Insurance (UIIC) │
│ • SBI Life Insurance            │           │   (Strengthened post Fincare    │           │ • ICICI Lombard General Ins.    │
│ • HDFC Life Insurance           │           │    SFB merger)                  │           │ • Bajaj Allianz General Ins.    │
│ • ICICI Prudential Life Ins.    │           │ • Care Health Insurance         │           └─────────────────────────────────┘
│ • Kotak Mahindra Life Ins.      │           │ • Aditya Birla Health Insurance │
│ • Bharti AXA Life Insurance     │           └─────────────────────────────────┘
│ • Bajaj Allianz Life Insurance  │
│ • Future Generali India Life    │
└─────────────────────────────────┘
```

### Insurer Integration Categorization
To manage technical connectivity, partner insurers are categorized into two structural groups:

* **Group A (Aggregator Integrated - 1SilverBullet):**
  * Insurers accessible via standard API integrations (e.g., HDFC Life, ICICI Prudential, Bajaj Allianz).
  * In-platform journey: Need Analysis → Suitability → In-Platform Quote → Proposal → Payment → Policy Issuance.
* **Group B (Catalog & Non-Aggregated Redirect):**
  * Insurers available in the AU Bank Product Catalog and Suitability Engine, but without live quote APIs integrated via 1SB.
  * In-platform journey: Need Analysis → Suitability → Product Recommendation → Controlled Redirect to Insurer Portal.

---

## 3. As-Is State vs. Target Platform Vision

### 3.1 As-Is State ("AU Beema Portal" Redirect Model)
In the current operational model:
1. RM/Customer accesses the legacy **AU Beema Portal**.
2. RM gathers basic customer details and inputs preliminary preferences.
3. The portal displays a basic list of matching products.
4. **The Blind Spot:** Upon product selection, customer data and parameters are redirected to the external insurance company's portal.
5. **Business Failure:** **AU Bank loses ALL visibility** post-redirection. The bank cannot track conversion rates, drop-offs, underwriting rejections, payment statuses, or policy issuance details.

```
[RM / Customer] ──► [AU Beema Portal] ──► (Basic Suitability) ──► [Redirect to Insurer Web Portal]
                                                                            │
                                                                            ▼
                                                                 [BANK LOSES VISIBILITY!]
                                                              (Drop-off, Payment, Issuance Unknown)
```

### 3.2 Target Vision (AU New Bank-Owned Insurance Distribution Platform)
AU SFB is building a brand-new, **bank-owned multi-insurer digital insurance platform** (similar to PolicyBazaar in user choice, but deeply integrated into AU Bank's identity, core banking, payment gateway, and compliance framework).

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        AU BANK NEW INSURANCE DISTRIBUTION PLATFORM                     │
├────────────────────────────────────────────────────────────────────────────────────────┤
│  • Single Unified Web/Mobile Interface (Flutter Cross-Platform)                         │
│  • End-to-End In-Platform Sales Engine: Lead ──► Need Analysis ──► Suitability ──►     │
│    Consent ──► Multi-Quote Compare ──► Proposal Capture ──► Bank PG ──► Policy Issuance│
│  • 100% Auditability, Real-Time Conversion Analytics & Commission Reconciliation       │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Customer Journeys & Customer Segment Matrix

The platform supports **RM-Assisted**, **Customer Self-Service (DIY)**, and **Hybrid** journeys.

```
                            ┌──────────────────────────────┐
                            │    AU Insurance Platform     │
                            └──────────────┬───────────────┘
                                           │
                   ┌───────────────────────┴───────────────────────┐
                   ▼                                               ▼
     ┌───────────────────────────┐                   ┌───────────────────────────┐
     │     Assisted Journey      │                   │        DIY Journey        │
     ├───────────────────────────┤                   ├───────────────────────────┤
     │ • RM / Branch Staff led   │                   │ • Customer Self-Service   │
     │ • Valid IRDAI Cert required│                  │ • Direct Bank App / Web   │
     │ • Customer Consent via OTP│                   │ • Self consent statement  │
     │ • Mandatory Distributor ID│                   │ • Mandatory Distributor ID│
     │ • Payment on Cust Device  │                   │ • Payment on Cust Device  │
     └───────────────────────────┘                   └───────────────────────────┘
```

### 4.1 Comparison of Journey Characteristics

| Journey Stage / Aspect | RM-Assisted Journey | Customer Self-Service (DIY) Journey |
| :--- | :--- | :--- |
| **Primary Actor** | Relationship Manager (RM) + Customer | Customer directly (RM / IP optional) |
| **Identity & Authentication** | RM via Bank Active Directory (AD); Customer via CBS CIF lookup | Customer via Bank Digital Banking SSO (ETB) / Mobile OTP |
| **Attribution** | **Compulsory:** AU Bank Distributor ID + RM Certified Agent ID | **Compulsory:** AU Bank Distributor ID (RM optional) |
| **Lead Creation** | RM logs lead in internal Lead Module | Automatic Lead creation on customer login/search |
| **Need & Suitability Analysis**| Mandatory guided questionnaire filled by RM with customer | Mandatory interactive suitability evaluation |
| **Consent Capture** | Formal SMS/OTP consent verification sent to customer device | In-app digital consent checkbox + OTP verification statement |
| **Quote & Compare** | Multi-insurer quote generation and comparison | Multi-insurer quote generation and comparison |
| **Proposal Capture** | RM fills detailed proposal based on customer answers | Customer self-fills proposal (pre-filled via CIF/KYC) |
| **Payment Execution** | Payment link sent to customer's personal device (**No pay on RM device**) | Customer completes payment directly via AU Bank Payment Gateway |
| **Policy Issuance** | Real-time policy issuance, COI download, full audit log | Real-time policy issuance, COI download, full audit log |

### 4.2 Customer Segment Rules (ETB vs. NTB)
* **Existing-To-Bank (ETB) [Phase 1 Focus]:** Any customer with an active AU Bank relationship (Savings Account, Current Account, Fixed Deposit, Loan, Credit Card, FASTag). Customer identification and KYC details are pre-filled directly from Core Banking Systems (CBS).
* **New-To-Bank (NTB) [Future Phase]:** Requires real-time video KYC (V-KYC), PAN verification, Aadhaar validation, and instant CIF generation prior to proposal submission.

### 4.3 Definition of "Policy Sold" (Business KPI Standard)
A policy is **NOT** counted as Sold at quote, proposal, or payment stage. A policy is declared **Sold** strictly when:
1. Policy contract is successfully issued by the partner insurer.
2. Bank receives valid API/webhook confirmation of issuance.
3. Financial reconciliation of premium payment is verified against AU Bank Payment Gateway.
4. Policy record is persisted in bank operations and audit stores.

---

## 5. Operational Personas, Organizational Hierarchies & Identity Engine

```
                      ┌─────────────────────────────────────────┐
                      │    Authentication & Authorization       │
                      │               Service                   │
                      └────────────────────┬────────────────────┘
                                           │
                ┌──────────────────────────┴──────────────────────────┐
                ▼                                                     ▼
   ┌───────────────────────────┐                         ┌───────────────────────────┐
   │    AU Bank Employees      │                         │ Insurance Partner (IP)    │
   │  (Active Directory / AD)  │                         │       Representatives     │
   ├───────────────────────────┤                         ├───────────────────────────┤
   │ • Relationship Manager    │                         │ • IP Desk Representative  │
   │ • Branch Manager          │                         │ • IP Regional Ops         │
   │ • Regional Manager        │                         │ • IP Admin (Bulk Upload)  │
   │ • Cluster Manager         │                         └───────────────────────────┘
   └───────────────────────────┘
```

### 5.1 Key Operational Actors
1. **Relationship Manager (RM):** Bank employee selling insurance. **Mandatory:** Valid IRDAI Specified Person (SP) Certificate. Authentication via AU Bank Active Directory (AD) / AD API.
2. **Insurance Partner Representative (IP):** Partner insurer employees (HDFC Life, ICICI Pru, etc.) who monitor proposals, assist underwriting queries, and view partner analytics. Created and managed via bulk CSV upload or IP Admin APIs.

### 5.2 Organizational Hierarchies
* **AU Bank Hierarchy:** RM → Branch Manager (BM) → Cluster Manager (CM) → Regional Manager (RM) → Zonal Head → National Head.
* **Insurance Partner Hierarchy:** Partner Desk Officer → Partner Regional Manager → Partner Corporate Head.

### 5.3 Centralized AuthN & AuthZ Service
* **Multi-Tenant SSO:** Federates with Bank AD for bank staff, Customer Digital Banking SSO for ETB customers, and DB/JWT credentials for IP representatives.
* **RBAC & ABAC Engine:** Manages fine-grained permissions controlling lead allocation, quote override, proposal verification, compliance audit log access, and MIS report visibility based on user role and branch mapping.

---

## 6. Business Capabilities & Bounded Contexts

Synthesized from `knowledge-base/03-capability-map.md` and `07-information-model-and-rules.md`, the platform is divided into **19 Bounded Contexts / Services**:

| # | Bounded Context / Microservice | Domain Ownership & Core Responsibility | Datastore Engine |
|---|--------------------------------|-----------------------------------------|------------------|
| 1 | **Customer BFF** | Edge API facade for customer mobile/web apps | Stateless |
| 2 | **RM Workspace BFF** | Edge API facade for RM mobile app / web portal | Stateless |
| 3 | **Identity & Access** | Authentication, SSO federation, AD mapping, RBAC | DynamoDB (sessions) + Aurora (roles) |
| 4 | **Customer Service** | Customer profile snapshot, CBS lookup | Aurora PostgreSQL |
| 5 | **Lead Service** | Lead capture, assignment, follow-ups | Aurora PostgreSQL |
| 6 | **Consent Service** | Digital consent evidence capture & versioning | Aurora PostgreSQL (Append-Only) |
| 7 | **Suitability & Recommendation** | Financial need analysis & suitability evaluation | Aurora PostgreSQL |
| 8 | **Product Catalogue** | Insurer product rules, eligibility matrix | Aurora PostgreSQL + Redis Cache |
| 9 | **Journey Orchestration** | Cross-domain journey state machine & Saga owner | DynamoDB |
| 10| **Quotation Service** | Multi-insurer quote generation, compare, fan-out | DynamoDB (jobs) + Redis (idempotency) |
| 11| **Proposal & UW-Tracking** | Detailed proposal capture, medical questionnaire, UW status | Aurora PostgreSQL |
| 12| **Payment Service** | AU Bank Payment Gateway trigger, link generation, recon | Aurora PostgreSQL |
| 13| **Policy & Issuance** | Policy record, COI generation, document management | Aurora PostgreSQL + S3 (PDFs) |
| 14| **Integration Hub** | Routing policy layer insulating core platform from third-parties | DynamoDB (routing config) |
| 15| **1SB Adapter** | Aggregator adapter slice (existing `1sb-integration-service`) | Aurora PostgreSQL + S3 (7yr raw archive) |
| 16| **Audit & Compliance** | Immutable regulatory audit log event store | DynamoDB (append-only) + S3 |
| 17| **Notification Service** | SMS, Email, Push notification delivery | DynamoDB (delivery logs) |
| 18| **Reporting & MIS** | Analytical read models, business intelligence | S3 Data Lake + Redshift / Athena |
| 19| **Administration & Config** | System config, feature flags, product rule administration | Aurora PostgreSQL |

---

## 7. Target Architecture & Technology Stack

```
   ┌────────────────────────────────────────────────────────────────────────┐
   │                         Flutter Frontend (Web/Mobile)                  │
   └───────────────────────────────────┬────────────────────────────────────┘
                                       │ HTTPS / REST / JSON
   ┌───────────────────────────────────▼────────────────────────────────────┐
   │                          AWS API Gateway / ALB                         │
   └───────────────────────────────────┬────────────────────────────────────┘
                                       │
   ┌───────────────────────────────────▼────────────────────────────────────┐
   │                     AWS EKS (Kubernetes Cluster)                       │
   │  ┌────────────────────────┐ ┌───────────────────────┐ ┌──────────────┐ │
   │  │ AuthN/AuthZ Service    │ │ Lead & Suitability Svc│ │  Quote Svc   │ │
   │  └────────────────────────┘ └───────────────────────┘ └──────────────┘ │
   │  ┌────────────────────────┐ ┌───────────────────────┐ ┌──────────────┐ │
   │  │ Proposal & Consent Svc │ │ 1SB Adapter Gateway   │ │ Audit Engine │ │
   │  └────────────────────────┘ └───────────────────────┘ └──────────────┘ │
   └───────────────────────────────────┬────────────────────────────────────┘
                                       │
   ┌───────────────────────────────────▼────────────────────────────────────┐
   │                     PostgreSQL Database (AWS RDS)                      │
   └────────────────────────────────────────────────────────────────────────┘
```

| Layer | Technology Chosen | Strategic Context & Architecture Standard |
| :--- | :--- | :--- |
| **Backend Core** | **Java 21 / Spring Boot 3.3.4** | Multi-module Gradle monorepo (`libs/` for shared security, errors, audit; `services/` for business domains). |
| **Frontend UI** | **Flutter (Dart)** | Unified cross-platform app for Android, iOS, and Web for RMs and customers. |
| **Relational Data** | **Amazon Aurora PostgreSQL (Multi-AZ)**| Database-per-service architecture; Flyway migrations per service. |
| **Key-Value / State**| **Amazon DynamoDB** | Journey state machine, session storage, quotation job tracking, audit logs. |
| **Compute / Infra** | **AWS EKS (Kubernetes)** | Elastic container orchestration using Karpenter (node scaling), HPA (pod scaling), and KEDA (Kafka lag scaling). |
| **Adapter Middleware**| **1SilverBullet (1SB)** | Integration abstraction layer converting bank canonical JSON requests into insurer API formats. |
| **CI/CD & Source** | **GitLab & GitLab CI/CD** | Automated builds, secret scanning, JaCoCo test coverage verification, and GitOps deployments. |
| **Agile & Requirements**| **Jira & Confluence** | Product backlog, epic structure, user story tracking, and DoD management. |

---

## 8. Phased Delivery & Implementation Roadmap

Derived from `architecture-review/07-delivery-roadmap-and-estimate.md`, the platform implementation follows a 4-phase rollout strategy:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ Phase 0: Foundation (Weeks 1–8)                                                        │
│ AWS Landing Zone, EKS, AuthN/AuthZ, Customer Svc, Catalogue, Journey Svc, 1SB Adapter  │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ Phase 1: Core Sale Path - Life LOB (Weeks 9–22)                                        │
│ RM-Assisted Term Life Journey: Suitability, Consent, Quote, Proposal, Payment, Issuance│
├────────────────────────────────────────────────────────────────────────────────────────┤
│ Phase 2: Compliance & Operations Hardening (Weeks 23–32)                               │
│ Full Audit & Compliance Service, Notification, Admin Config, Security Review, DR Drill │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ Phase 3: Extend & Scale (Weeks 33–40)                                                  │
│ Lead Module, ULIP/Savings Variants, Group B Redirect, DIY/Hybrid, Reporting/MIS        │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Regulatory Compliance, Audit & Security Controls

### 9.1 Regulatory Compliance Gates
1. **Suitability Hard-Gate:** Quote generation APIs automatically return `403 Forbidden` if a valid suitability evaluation ID is not attached.
2. **Consent Evidence:** Every consent event logs statement text, version ID, customer CIF, OTP transaction ID, timestamp, and IP address into an append-only store.
3. **Attribution Control:** `distributorId` is injected strictly from secure server-side configuration (never caller-supplied) to prevent multi-tenant spoofing.
4. **Device Payment Isolation:** RM device displays a QR code / sends SMS payment link; payment execution occurs strictly on customer device via AU Bank Payment Gateway.

### 9.2 Security & Data Governance Standards
* **Data Residency:** All application data, database backups, logs, and document archives reside in AWS India Region (`ap-south-1` Mumbai primary, `ap-south-2` Hyderabad DR).
* **PII Encryption & Masking:** Encryption at rest via KMS CMKs (AES-256) and in transit via TLS 1.3. PII (Aadhaar, PAN, phone, email) is automatically masked at the logging framework level using `PiiMaskingConverter`.
* **7-Year Retention:** Raw payload archives and regulatory audit logs are retained in S3 with Object Lock for 7 years to meet IRDAI compliance obligations.

---

## 10. RAG System Context & Usage Directives

This document serves as the **Single Source of Truth (SSOT)** for AI agent RAG retrieval regarding AU Bank's Insurance Platform business problem, regulatory environment, system architecture, and product boundaries.

### Instructions for AI Agents / RAG Systems:
1. **Always reference AU SFB's IRDAI Composite Corporate Agency License (CA0515)** when answering regulatory scope questions.
2. **Enforce the distinction between Phase 1 Scope** (RM-Assisted, Life Insurance LOB, ETB customers) and **Future Scope** (DIY, Health/General LOBs, NTB customers).
3. **Uphold the technical architecture decisions:** AWS EKS compute substrate, Java 21 / Spring Boot 3.3.4 monorepo, Flutter UI, PostgreSQL database-per-service, and 1SilverBullet aggregator abstraction.
4. **Never compromise on compliance rules:** Suitability before quote, customer device payment isolation, and 4-tier definition of Policy Sold.
