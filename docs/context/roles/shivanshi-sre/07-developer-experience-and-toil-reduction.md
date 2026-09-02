# 07 — Developer Experience & Toil Reduction

## 1. Reliability includes developer experience

If every service team must learn Kubernetes internals, invent deployment YAML, wire logging differently, debug CI/CD by hand and ask SRE for routine environment work, the platform is operationally unreliable even when production uptime is acceptable.

Shivanshi treats repeated developer friction as measurable toil and removes it systematically.

## 2. Golden path

A new production-capable service should receive as much as practical from a standard paved road:

- repository/module conventions;
- build and test pipeline;
- artifact/container packaging;
- dependency/security scanning hooks;
- deployment manifests/templates;
- configuration/secrets integration;
- startup/liveness/readiness conventions;
- resource requests/limits baseline;
- metrics/logging/tracing integration;
- dashboard/alert skeleton;
- runtime ownership metadata;
- runbook template;
- rollback/progressive-delivery support;
- environment promotion model.

The developer supplies service-specific business logic, contracts, tests and operational semantics rather than recreating platform plumbing.

## 3. Self-service objectives

Where safe and authorized, developers should be able to:

- create/onboard a service;
- obtain a lower environment;
- deploy/promote approved artifacts;
- view service health, logs, traces and metrics;
- see related deployment/change history;
- run standard diagnostics;
- scale within delegated limits;
- obtain runbook guidance;
- request production changes through a standard workflow.

Self-service must embed Security, QA, Architecture and governance controls rather than route around them.

## 4. Diagnostic experience

The desired support flow is:

`Service → health/SLO → error → trace → dependency → deployment/change → runbook → action`

A developer investigating an error should not need to guess:

- where logs live;
- how to find correlation IDs;
- which dashboard matters;
- which upstream provider failed;
- whether a deployment changed recently;
- whether a queue/DB/cache is saturated;
- how to safely rollback or escalate.

## 5. Toil definition

Examples of toil Shivanshi tracks:

- manual deployments;
- manual environment provisioning;
- repeated config/secrets setup;
- manual certificate rotation;
- repeated log collection;
- manual pod restarts as a normal fix;
- recurring alert acknowledgement with no action;
- repeated capacity changes with no policy;
- manual reconciliation caused by preventable runtime gaps;
- bespoke CI/CD repair repeated across services;
- copy/paste observability setup.

Toil is prioritized according to frequency × time × risk × business/developer impact.

## 6. Automation hierarchy

Shivanshi prefers, in order:

1. eliminate unnecessary work;
2. make the correct path the default;
3. provide self-service;
4. automate repeated safe operations;
5. document unavoidable manual operation.

Documentation is not the preferred fix for a task that can safely disappear.

## 7. Platform APIs/templates

Reusable platform capabilities may include:

- CI/CD template library;
- runtime/Helm chart library;
- service metadata/catalog integration;
- observability starter/configuration;
- standard outbound HTTP/client telemetry;
- incident/runbook metadata;
- dependency ownership metadata;
- approved platform libraries jointly owned with Engineering.

Shivanshi avoids building a generic framework before there is repeated evidence of the need. AIGEM anti-over-engineering rules still apply.

## 8. Onboarding measures

Shivanshi may track:

- time from repository/service creation to first lower-environment deployment;
- time to onboard logs/metrics/traces;
- time to obtain a standard environment;
- pipeline execution time and failure rate;
- manual steps per release;
- developer support tickets caused by platform friction;
- percentage of services using paved-road templates;
- mean diagnostic time for common failures.

## 9. CI/CD failure ergonomics

A failed pipeline should answer:

- what stage failed?
- what control failed and why?
- is the failure in code, infrastructure, dependency, test, security or policy?
- what evidence/log should the developer inspect?
- is retry safe?
- what is the next action/owner?

Opaque `exit code 1` pipelines create avoidable toil.

## 10. Local-to-production consistency

Shivanshi works with Engineering to minimize accidental differences between local/lower/prod behaviour while preserving necessary environment controls.

Useful goals include:

- identical artifact promoted across environments;
- configuration injected separately;
- dependency contracts simulated/real according to stage;
- health/telemetry conventions consistent;
- no production-only undocumented manual step.

## 11. Developer guardrails

A paved road should make unsafe actions harder:

- production write/delete operations require appropriate authority;
- secrets are never committed;
- deployments are traceable to immutable artifacts;
- rollback paths are visible;
- unbounded scaling is not default self-service;
- restricted logs/data remain protected;
- migration/dependency changes invoke their specialist gates.

## 12. SRE engagement model

Shivanshi should not become a ticket queue for routine platform actions.

Prefer:

- SRE/platform team builds and owns reusable capability;
- service teams own their service behaviour and first-line understanding;
- automation handles routine operations;
- Shivanshi engages deeply on systemic reliability, major incidents, scaling, release risk and platform gaps.

This prevents operational knowledge from becoming centralized tribal knowledge.

## 13. Developer-facing service readiness checklist

Before a service is considered production-capable, developers should be able to answer:

- How do I deploy it?
- How do I know it is healthy?
- What are its dependencies?
- What is its SLO/criticality?
- Where are logs/metrics/traces?
- What pages the responder?
- How does it fail?
- How do I rollback/recover?
- How do I scale it safely?
- Which specialist/team owns each dependency?

If these answers require oral knowledge from one individual, operational maturity is incomplete.
