# 10 — Maintenance, Versioning & Governance

## 1. Purpose

A strong persona becomes unsafe if its regulatory/control knowledge becomes stale. Persona behaviour and regulatory knowledge must therefore be versioned separately.

## 2. Versioned artefacts

Maintain versions for:

- persona;
- risk taxonomy;
- decision policy;
- exception policy;
- control catalogue;
- regulatory registry;
- evidence policy;
- interaction schema.

Example:

```text
persona_version: 1.0.0
decision_policy_version: 1.0.0
control_catalogue_version: 1.0.0
regulatory_registry_version: 2026.08.14
```

## 3. Change classes

### Major

Changes decision authority, non-bypassable categories, regulatory perimeter or core risk model.

### Minor

Adds controls, new regulation mappings, new risk categories or decision metadata without changing core semantics.

### Patch

Clarifies text, fixes references or improves examples without substantive policy change.

## 4. Required review cadence

Suggested minimum:

- regulatory registry: event-driven plus periodic review;
- critical regulatory sources: whenever new/superseding instruction is issued;
- persona/decision policy: at least annually or after material governance incidents;
- control catalogue: at least semi-annually or after major architecture/technology changes;
- open risk-R1 exceptions: frequent governance review;
- risk-R2/R3 exceptions: review according to expiry and risk policy.

## 5. Regulatory-change impact workflow

When a material source changes:

1. register the new source/version;
2. mark superseded source status;
3. identify affected controls;
4. query existing architecture decisions;
5. query active exceptions;
6. query affected products/journeys/vendors;
7. determine effective/transition dates;
8. create remediation items;
9. notify accountable owners;
10. update the persona knowledge index only after validation.

## 6. Persona change governance

Changes to these rules require formal review:

- risk-R0 definition;
- non-bypassable controls;
- exception authority;
- risk-acceptance delegation;
- regulatory-source hierarchy;
- autonomous AI decision authority.

Engineering teams should not be able to edit these controls simply to get a blocked release approved.

## 7. Audit requirements

Retain enough history to determine:

- which persona/policy version issued a decision;
- which sources were used;
- which facts were known;
- who approved an exception;
- whether the exception expired;
- what subsequently superseded the decision.

## 8. Continuous improvement

Use production incidents, audit findings, near misses, regulator observations and recurring exceptions to improve:

- control catalogue;
- examples;
- prompts;
- escalation thresholds;
- technical evidence expectations;
- automated policy checks.

Repeated exception patterns should lead to platform-level fixes rather than permanent exception administration.
