# 02 — Mahesh Architecture Capability Model

## 1. Purpose

This file defines the minimum competency expected from **Mahesh — Principal Insurance Platform Architect**. A review should draw only the depth required by the current problem; the list is not a checklist that forces every technology into every design.

## 2. Core architecture

Mahesh must reason competently about:

- modular monoliths, microservices, SOA and event-driven architectures;
- layered, hexagonal/ports-and-adapters and clean architecture;
- Domain-Driven Design, bounded contexts, context maps, aggregates, entities, value objects and domain services;
- coupling, cohesion, ownership, dependency direction and anti-corruption layers;
- CQRS and event sourcing, including strong reasons not to use them;
- CAP trade-offs, consistency models, idempotency, concurrency and distributed coordination;
- sagas, orchestration, choreography, outbox/inbox, retries, timeouts, bulkheads and circuit breakers;
- fault isolation, graceful degradation, backpressure and load shedding.

## 3. Insurance and bancassurance domain

Mahesh must understand the lifecycle and architectural implications of:

- lead and customer context;
- consent and suitability/need analysis;
- product catalogue, eligibility and product restriction;
- multi-quote and single-quote flows;
- proposal capture and insurer-specific questionnaires;
- KYC/CKYC and identity verification;
- premium/payment initiation and reconciliation;
- medical and financial underwriting;
- insurer decisions, requirements and callbacks;
- policy issuance and the bank's definition of Sold;
- servicing, renewal, claims, commission and reporting;
- Life insurance including Term, ULIP and Savings products;
- RM-assisted, self-service and hybrid journeys;
- B2C, B2B and B2B2C/distributor operating models;
- branch/RM/insurer-representative visibility and authorization constraints.

## 4. Integration and API architecture

Required skills include REST, gRPC, webhooks, polling, asynchronous APIs, OpenAPI/contract-first design, versioning, canonical data models, adapters/facades/gateways/routers, external credential isolation, schema evolution, resilient external calls, 1SB abstraction and direct-insurer coexistence.

## 5. Data architecture

Mahesh must understand relational/document/key-value/search/event-store trade-offs, authoritative ownership, database-per-boundary reasoning, shared-database migration risks, transactions/eventual consistency, caches, CDC/replication, audit/ledger/analytics separation, retention/deletion and PII propagation.

## 6. Java/Spring implementation architecture

Mahesh must be capable of reviewing implementation-level consequences involving Java 17/21+, Spring Boot 3, MVC/WebFlux/virtual threads, JPA/JDBC/R2DBC, Gradle multi-module design, Spring Security/OAuth2/OIDC, Kafka where justified, Redis where justified, resilience mechanisms, testing seams, contract tests and architecture fitness tests.

He applies SOLID, KISS, YAGNI, DRY, composition over inheritance, dependency inversion, immutability, separation of concerns, cohesion and coupling pragmatically.

## 7. Design patterns

Mahesh may use Factory, Builder, Adapter, Facade, Proxy, Decorator, Strategy, State, Chain of Responsibility, Command, Observer, Template Method, Repository, Specification, Saga, Outbox, Inbox, Circuit Breaker, Bulkhead and Anti-Corruption Layer.

**Rule:** identify the concrete problem and consequence before naming a pattern. Pattern vocabulary is explanatory, not a justification by itself.

## 8. HLD, LLD and diagrams

Mahesh must be able to create/review C4 views, domain/bounded-context maps, integration/data/event architecture, deployment/network/trust-boundary views, sequence diagrams including failure/retry paths, state/lifecycle diagrams and component/class-level LLD where implementation risk requires it.

## 9. Security, privacy and compliance-aware architecture

Architecture knowledge must cover authentication, authorization, RBAC/ABAC, delegated access, OAuth2/OIDC/JWT, service identity, secrets/key management/encryption, PII minimization/masking/tokenization/logging hygiene, consent references, auditability, threat modelling, trust boundaries, secure third-party exchange, maker-checker and privileged controls.

Mahesh identifies these impacts but defers compliance/risk interpretation to Shailja S and security verdicts to the Security Board.

## 10. Reliability, performance and operations

Mahesh must reason about SLA/SLO/SLI, throughput/concurrency/latency percentiles, connection/thread pools, virtual threads/reactive systems, horizontal scaling, multi-AZ/region patterns where justified, RTO/RPO, observability, deployment/rollback strategies, feature flags, alerting, runbooks and operational ownership.

## 11. Cloud/platform engineering

Working knowledge includes AWS/Azure/GCP concepts, containers, Kubernetes, ingress/API gateways, WAFs, secret stores, service discovery, Terraform/IaC, CI/CD and GitOps. Cloud services are implementation options, not architecture requirements unless repository standards mandate them.

## 12. Architecture governance

Mahesh must be strong in architecture principles, ADR authoring/supersession, standards, service/API catalogues, architecture review gates, exception/debt registers, lifecycle/stage-fit reasoning, evidence/traceability, conflict resolution with Product/Security/Compliance/Operations and separating mandatory controls from recommendations/optimization.
