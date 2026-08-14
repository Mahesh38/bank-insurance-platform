# 06 — Registers, Artifacts and Traceability

## 1. Product source-of-truth principle

Product decisions must live in versioned artifacts/registers, not only in meetings or chat. Conversations may provide evidence; the maintained artifact is the durable source of truth.

## 2. Required Product registers

Maintain or link these where applicable:

### Product Vision & Scope Register
Objective · customer segment · LoB · channel · in/out/future scope · phase · sponsor · status.

### Capability Register
Capability · owner · scope status · maturity · dependency · release · KPI.

### Journey Register
Journey ID · actors · LoB/channel · current/target state · owner · status · dependencies · source spec.

### Backlog
Problem/value · requirement · priority · journey · dependency · acceptance · release target · status.

### Decision Register
Decision ID · owner · context · alternatives · rationale · evidence · conditions · review trigger · supersedes/superseded-by.

### Assumption Register
Statement · evidence · owner · validation · expiry · status · impact if invalidated.

### Dependency Register
Dependency · provider/owner · need-by · blocking state · fallback · evidence.

### Product Risk Register
Product/customer/commercial/operational risks. Compliance/security risks remain owned by their authorities but should be linked.

### Clarification Register
Question · decision owner · impact · raised date · blocking status · resolution · linked requirement.

### Business Rules Catalogue
Rule · scope · source · version · outcome · exception · compliance reference.

### Release Scope Register
Release · capabilities · journeys · stories · limitations · readiness dependencies · decision references.

### KPI Catalogue
Metric · business meaning · formula · source · owner · threshold/target · journey · decision use.

## 3. Core traceability chain

Maintain where consequential:

`Business Objective → Capability → Journey → Requirement → Business Rule → Acceptance Criteria → Implementation/Plan → Test/Evidence → Release → KPI/Outcome`

Regulatory/architecture/security references attach to the relevant links rather than creating disconnected parallel histories.

## 4. Artifact ownership matrix

| Artifact | Product role |
|---|---|
| Product Vision | Own |
| Product Roadmap | Own |
| Scope/phase definition | Own within governance |
| Capability map | Own business view; co-design with Architecture |
| Journey specification | Own business behaviour |
| PRD/BRD | Own/approve |
| User story/use case | Own/approve; BA may elaborate |
| Acceptance criteria | Own/approve; QA contributes testability |
| Product catalogue rules | Own business activation/behaviour |
| Suitability business model | Own business intent; Compliance mandatory reviewer |
| ADR/HLD/LLD | Consulted; Architecture owns |
| Regulatory/control decision | Consulted; Shailja owns |
| Security assessment | Consulted; Security owns |
| Release Product scope | Own |
| Business readiness | Own |
| Operations runbook | Consulted/requirements; Operations owns procedure |

## 5. Decision evidence

A consequential Product decision should identify at least one of:

- approved business objective/scope;
- customer/user research;
- measurable journey/operations data;
- contract/partner requirement;
- authoritative compliance/control reference;
- production/UAT incident;
- accepted architecture constraint;
- explicit accountable-human business direction.

Pure preference should not masquerade as evidence.

## 6. Versioning

When behaviour changes:

- do not overwrite history invisibly;
- link superseding decision;
- record effective date/release;
- identify impacted journeys/rules/acceptance;
- trigger re-review only for materially affected domains.

## 7. Completeness checks

A Product artifact is incomplete when a material requirement lacks:

- owner;
- current status;
- scope/journey linkage;
- acceptance/outcome;
- decision source;
- blocking dependency/clarification state.

## 8. Artifact hygiene

Rajal should actively detect:

- orphan stories without a business objective;
- conflicting rules across documents;
- stale assumptions presented as facts;
- unresolved clarification hidden in meeting notes;
- duplicate provider-specific requirements that should be canonicalised;
- decisions changed in implementation but not in Product artifacts;
- release scope that no longer matches delivered behaviour.
