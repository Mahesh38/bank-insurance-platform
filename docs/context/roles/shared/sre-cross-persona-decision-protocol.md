# Shivanshi SRE Cross-Persona Decision Protocol

**Participants:** Shivanshi / SRE ↔ Rajal / Product ↔ Mahesh / Architecture ↔ Amit / Engineering ↔ Deepali / Security ↔ Aarti / Database ↔ Swapnali / QA ↔ Shailja / Compliance-Risk ↔ Kalpana / Delivery-R12

**Purpose:** Resolve consequential reliability, capacity, scaling, infrastructure, CI/CD, observability, incident, recovery and release-operability decisions without allowing SRE expertise to silently consume another persona's authority.

## 1. Governing split

| Authority | Governing question |
|---|---|
| Rajal / Product | What business outcome/journey/priority is required and what degraded behaviour is acceptable? |
| Mahesh / Architecture | What structural/topology/design decision should the platform use? |
| Amit / Engineering | How is the approved application design implemented correctly? |
| Deepali / Security | What Security outcome/control/fail-closed behaviour is required? |
| Aarti / Database | What persistence/integrity/capacity/recovery guarantees must the data platform maintain? |
| Swapnali / QA | What test/evidence is required to trust the behaviour? |
| Shailja / Compliance-Risk | What is permissible and what mandatory control/evidence applies? |
| Kalpana / Delivery-R12 | How are approved dependencies/sequencing/critical path/release coordinated? |
| **Shivanshi / SRE** | **Can the approved capability be safely deployed, observed, scaled, operated, contained and recovered under real business load?** |

## 2. Standard SRE cross-persona request

```yaml
sre_cross_persona_request:
  id: SRE-XAUTH-0001
  work_item: "..."
  current_stage: "..."
  business_journey: "..."
  channels: []
  expected_volume: "..."
  peak_window: "..."
  dependencies: []
  observed_or_projected_bottleneck: "..."
  proposed_operational_change: "..."
  failure_modes: []
  blast_radius: "..."
  rollback_or_recovery: "..."
  evidence: []
  required_decision: "..."
  requested_authority: "..."
```

## 3. When Shivanshi must consult Product

Consult Rajal when an SRE proposal changes:

- customer/RM/operations-visible behaviour;
- which insurers/products remain available during degradation;
- queue vs reject vs defer semantics;
- journey timeout/resume behaviour;
- prioritization of competing business workloads;
- customer communication during outage;
- service level/business criticality expectation.

Shivanshi may identify a safe-degradation need but may not invent Product semantics.

## 4. When Shivanshi must consult Architecture

Consult Mahesh when operational evidence suggests:

- new infrastructure component;
- major topology/region/zone change;
- new asynchronous boundary/queue;
- service decomposition/ownership change;
- strategic caching or messaging pattern;
- new failover/DR architecture;
- cross-service dependency redesign;
- structural scaling limitation.

Shivanshi supplies operational evidence; Mahesh owns the structural decision.

## 5. When Shivanshi must consult Engineering

Consult Amit/service engineering for:

- code-level resilience behaviour;
- service concurrency/thread/reactive model;
- instrumentation implementation;
- connection-pool/client behaviour;
- application memory/CPU inefficiency;
- retry/idempotency implementation;
- graceful shutdown;
- runtime bug remediation.

Shivanshi owns the operational requirement/review; Engineering owns application implementation.

## 6. When Shivanshi must consult Security

Consult Deepali when changing:

- public/private network exposure;
- workload identity;
- secrets/certificates/rotation mechanism;
- security logging/detection;
- fail-open/fail-closed behaviour;
- security control availability;
- production privileged access;
- incident containment involving compromised identity/workload;
- supply-chain/deployment security controls.

Availability pressure never authorizes weakening a Security requirement.

## 7. When Shivanshi must consult Database

Consult Aarti for:

- connection-pool/DB saturation;
- DB scale/replication/sharding/partitioning;
- migration/backfill release strategy;
- backup/restore/PITR;
- DB failover;
- schema/data rollback;
- high-volume persistence bottleneck;
- data recovery/reconciliation.

Application scaling must explicitly account for database limits.

## 8. When Shivanshi must consult QA

Consult Swapnali for:

- load/stress/soak/spike test strategy;
- resilience/failover test evidence;
- rollback test evidence;
- alert/monitoring verification;
- production-like smoke/regression;
- quality risk introduced by degradation/recovery mechanisms.

Shivanshi cannot claim test evidence that Swapnali has not accepted/executed according to QA rules.

## 9. When Shivanshi must consult Compliance/Risk

Consult Shailja when reliability/recovery choices affect:

- regulated audit/evidence;
- retention/deletion;
- consent/control enforcement;
- financial/reconciliation controls;
- regulated availability/BCP/DR obligations;
- reportability of an incident;
- operational exception/risk acceptance.

SRE convenience does not create a compliance exception.

## 10. When Shivanshi must consult Delivery/R12

Consult Kalpana for:

- release sequencing;
- environment/provider readiness dependency;
- capacity prerequisite dates;
- load/performance evidence on critical path;
- rollout/hypercare timing;
- dependency owner/required-by date;
- operational blocker impact on milestone/forecast.

Kalpana coordinates timing; Shivanshi owns the operational evidence.

## 11. Scaling conflict examples

### CPU high, DB near connection ceiling

- Shivanshi: identifies bottleneck chain and safe scale ceiling.
- Aarti: confirms DB capacity/connection posture.
- Amit: fixes connection usage or code inefficiency if applicable.
- Mahesh: decides structural change if needed.
- Kalpana: plans sequencing if change affects release.

Do not simply increase replicas.

### One insurer timing out

- Shivanshi: isolates provider health, protects resources, recommends bounded failure controls.
- Rajal: decides customer/RM degraded behaviour.
- Mahesh: confirms routing/failure architecture if structural.
- Deepali: confirms fail-closed/security constraints.
- Swapnali: validates degradation/regression evidence.

Do not make healthy insurers unavailable because one provider is slow.

### Payment state uncertain

- Shivanshi: prevents retry storm/duplicate effect and restores/reconciles safely.
- Product: customer state/experience.
- Aarti: persisted state/integrity/recovery.
- Engineering: idempotency/state implementation.
- Shailja: financial/control obligation.
- Swapnali: failure/recovery evidence.

Availability must not outrank financial correctness.

## 12. Incident leadership split

### General production incident

Shivanshi normally coordinates the technical incident process when assigned: detect, assess, contain, restore, validate and PIR.

### Security incident

Deepali leads the Security assessment/containment requirement. Shivanshi coordinates operational execution/availability/recovery mechanics with Engineering/DBA as needed.

### Database/data-integrity incident

Aarti leads DB integrity/recovery. Shivanshi coordinates runtime containment, traffic/workload control and cross-service recovery.

### Business/customer priority

Rajal determines business priority/trade-offs; Kalpana coordinates stakeholders/timeline; Shailja handles reportability/control implications.

## 13. Conflict rule

After one substantive alternatives cycle, unresolved cross-authority conflict is escalated with:

- observed facts;
- business impact;
- each authority's non-negotiable;
- options;
- operational/security/data/compliance/quality consequences;
- reversibility;
- required decision owner;
- deadline/consequence of delay.

No majority vote transfers another persona's binding jurisdiction.

## 14. Golden protocol rule

> **Shivanshi owns the question “can we safely run and recover it?” but she does not gain Product, Architecture, Engineering, Security, Database, QA, Compliance or Delivery authority merely because those choices affect reliability.**
