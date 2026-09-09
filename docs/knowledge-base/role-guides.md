# Start by role

The same repository looks very different depending on what you are responsible for. Use the shortest path that gets you productive without reading every document first.

## Developer

Read in this order:

1. [Knowledge Hub](index.md)
2. [Glossary](glossary.md)
3. [Current state & decisions](status-and-decisions.md)
4. [Business problem](../context/business-problem-statement.md)
5. [R0 HLD](../architecture/R0-HLD.md)
6. [Domain model & invariants](../platform/ws3-platform/01-domain-model-and-invariants.md)
7. [Request lifecycle](../journey-execution/01-REQUEST-LIFECYCLE-STANDARD.md)
8. [API/service catalogue](api-service-catalogue.md)
9. `settings.gradle.kts` + the README of the service you are changing
10. [Developer role card / Runbook](../governance/RUNBOOK.md)

Before coding, know: work item ID, current workstream/stage, acceptance criteria, relevant invariants, expected files, dependencies and required tests.

## Architect

1. [Current state](status-and-decisions.md)
2. [Business documentation](../au-bank-insurance-platform/README.md)
3. [WS-3 registration](../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md)
4. [Domain model](../platform/ws3-platform/01-domain-model-and-invariants.md)
5. [Information model](../platform/ws3-platform/02-information-model.md)
6. [Solution architecture](../platform/ws3-platform/03-solution-architecture-r0.md)
7. [Security architecture](../platform/ws3-platform/04-security-architecture.md)
8. [NFR catalogue](../platform/ws3-platform/05-nfr-catalogue.md)
9. [ADR log](../platform/architecture-review/08-architecture-decision-log.md)
10. [Review gates](../governance/11-REVIEW_GATES.md)

Focus on decision status: Accepted vs Proposed vs AI-DRAFTED vs human-signature outstanding.

## Product Owner / Business Analyst

1. [Business documentation index](../au-bank-insurance-platform/README.md)
2. [Business problem](../context/business-problem-statement.md)
3. R0 scope and working decisions
4. Current state and parked backlog
5. Domain model and actor/journey rules
6. Product board / governance Runbook
7. Decision register and open CRs

Focus on: business necessity, actor behaviour, scope, acceptance criteria, sequencing and unresolved business decisions.

## QA / QA Lead

1. R0 business journey
2. Domain invariants and standing controls
3. Request lifecycle / validation / error flows
4. NFR catalogue
5. Definition of Ready / Definition of Done
6. Stage gate evidence
7. Service tests and CI workflows

Build tests from invariants and observable acceptance criteria, not only happy-path UI screens.

## Security

1. [Authentication & authorization SSOT](../platform/authentication-authorization/README.md)
2. [Security architecture](../platform/ws3-platform/04-security-architecture.md)
3. R0 HLD/LLD trust boundaries
4. T4 triggers G1–G10 and Board 4 rules
5. ADRs affecting identity, public exposure, secrets, crypto, PII, networking and provider connections
6. NFR/security evidence and CI security checks

## DBA / Data Architect

1. Information model
2. Domain ownership/invariants
3. Data architecture decisions / DB decision records
4. R0 LLD data topology
5. Retention/evidence rules
6. Migration/backfill T4 rules
7. Service persistence boundaries and migration files

Focus on logical ownership separately from physical cluster topology.

## SRE / DevOps / Platform Engineer

1. Current stage/gate
2. R0 LLD
3. Platform foundation decisions (ADR-009 onward where applicable)
4. NFR catalogue
5. CI/CD and foundation gate criteria
6. Operations/SRE role guidance
7. DR, observability, alerting, deployment, rollback and environment evidence

Do not infer that a Proposed ADR is approved to provision.

## Compliance / Risk

1. Business scope and working decisions
2. Standing controls C1–C8
3. Domain invariants
4. Consent/suitability/payment/audit evidence paths
5. Review gates Board 6 and T4 triggers
6. Open CRs with compliance conditions
7. Retention and regulator-evidence requirements

## Delivery / Programme

1. Current state
2. Gate state and blockers
3. READY/BLOCKED/PARKED queues
4. Dependency model
5. Priority model
6. CR/ADR decision status
7. Build waves and release roadmap

The delivery question is not “is this a good idea?” but “is it approved, dependency-ready, correctly ordered and appropriate to the current stage?”
