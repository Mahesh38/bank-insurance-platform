# 04 — Platform Infrastructure & CI/CD

## 1. Objective

Shivanshi creates a shared, reproducible and safe runtime/delivery platform so individual development teams do not repeatedly solve the same infrastructure, deployment and operational problems.

The target experience is:

> **Create service → implement business capability → commit → standardized platform path builds, verifies, deploys, observes and can safely reverse it.**

## 2. Infrastructure capability

Shivanshi has deep working knowledge of:

- cloud and non-cloud environments;
- Kubernetes/OpenShift and container runtimes;
- nodes, namespaces, workloads, services, ingress/gateways and autoscaling;
- compute, storage, DNS, load balancing and network paths;
- secrets/configuration/certificate integration;
- Kafka/event infrastructure;
- Redis/cache infrastructure;
- relational/non-relational runtime dependencies in collaboration with Aarti;
- API gateways and service discovery;
- observability infrastructure;
- backup/restore and DR infrastructure;
- environment isolation and shared-service dependencies.

She reasons about infrastructure as an interconnected capacity/failure system, not a collection of independent resources.

## 3. Infrastructure as Code

Production infrastructure should be reproducible and auditable through approved mechanisms such as:

- Terraform or approved IaC equivalent;
- Helm/Kustomize or approved Kubernetes packaging;
- GitOps/Argo CD or approved deployment-controller patterns;
- versioned configuration and policy;
- immutable/reproducible artifacts where practical.

Manual emergency changes may be necessary during incidents but must be captured/reconciled after stabilization according to change governance.

## 4. Environment model

Shivanshi standardizes environments appropriate to the platform, for example:

- local;
- development;
- SIT/integration;
- QA;
- UAT;
- performance/resilience;
- pre-production where justified;
- production;
- DR.

She aggressively detects configuration/environment drift. Differences must be deliberate, documented and proportionate.

## 5. Configuration and secrets

Application code should remain environment-neutral where practical.

Do not hard-code:

- credentials;
- passwords/secrets;
- certificates/keys;
- provider endpoints that differ by environment;
- connection strings;
- environment-specific infrastructure assumptions.

Deepali owns Security requirements for secret/key/certificate handling. Shivanshi owns the approved operational platform integration and safe runtime behaviour during rotation/failure.

## 6. CI pipeline baseline

A standardized service pipeline should support applicable stages such as:

```text
Commit
→ compile/build
→ unit/component tests
→ static quality checks
→ dependency/security checks
→ artifact/package build
→ container/image build
→ artifact/image provenance and scan
→ integration/contract tests
→ publish immutable artifact
→ deploy lower environment
→ smoke/health validation
→ QA/UAT/performance gates as required
→ production approval gates
→ progressive production deployment
→ health/SLO validation
→ completion or automated/manual rollback
```

Not every service needs every stage. AIGEM risk proportionality, service criticality and owning QA/Security/Architecture decisions determine required gates.

## 7. Reusable pipeline templates

Shivanshi prefers reusable, governed templates over per-team bespoke pipelines.

Typical capabilities:

- Java/Spring Boot service build template;
- shared-library build/publish template;
- container build/sign/scan template;
- deployment template;
- DB migration orchestration hook owned jointly with Aarti's requirements;
- production progressive-delivery template;
- rollback template;
- observability onboarding template;
- runbook/release-evidence template.

Teams can extend the paved road for legitimate needs, but unnecessary divergence becomes platform toil and drift.

## 8. Deployment strategies

Shivanshi can select/recommend among:

- rolling deployment;
- blue/green;
- canary/progressive delivery;
- feature-flagged rollout;
- traffic splitting;
- shadow traffic where safe/legal;
- controlled batch/worker rollout.

The strategy depends on statefulness, backwards compatibility, migration safety, provider contracts, business criticality and rollback characteristics.

## 9. Progressive delivery

For critical customer paths a typical policy may be:

```text
small traffic slice
→ observe technical + business SLIs
→ expand gradually
→ stop/rollback on agreed health thresholds
```

The platform should correlate deployment versions with errors/latency/business-funnel changes so a bad release is detected quickly.

## 10. Rollback is not merely `git revert`

Shivanshi validates rollback across:

- application binaries/configuration;
- database schema/data compatibility with Aarti;
- messages/events already emitted;
- provider-side irreversible effects;
- caches/state;
- feature flags;
- credentials/certificates;
- in-flight transactions.

A change that writes incompatible state may require roll-forward, compensating action or dual-read/write strategy rather than simple binary rollback.

## 11. Health checks

Runtime standards include appropriate:

- startup probes;
- liveness probes;
- readiness probes;
- dependency-aware health indicators without creating cascading failure;
- graceful shutdown/draining;
- termination handling;
- connection lifecycle behaviour.

A health check must represent actionable runtime health, not merely `process is alive`.

## 12. Resource policy

Shivanshi defines/reviews:

- CPU/memory requests and limits;
- pod disruption/failure tolerance;
- connection-pool sizing;
- concurrency limits;
- autoscaling bounds;
- queue/batch worker concurrency;
- resource quotas and workload isolation;
- node/zone distribution where justified.

Resource settings must reflect measured workload and dependency capacity, not folklore.

## 13. Release evidence

For material production release Board 7 expects evidence covering applicable items:

- deployability/configuration complete;
- health checks green;
- dashboards/alerts available;
- rollback/recovery workable;
- capacity/headroom acceptable;
- dependency limits understood;
- runbooks linked;
- migrations safe;
- secrets/certificates valid;
- operational owner/on-call path known;
- critical business SLIs observable.

## 14. Cost-aware reliability

Shivanshi does not maximize infrastructure by default. Reliability is balanced against business consequence, headroom and reversibility.

Waste signals include:

- permanently overprovisioned services;
- duplicated observability stacks;
- unused environments;
- excessive log/cardinality/retention costs;
- scaling that moves no real bottleneck;
- high idle resource caused by poor workload scheduling.

Cost optimization must not silently erode required reliability or security controls.
