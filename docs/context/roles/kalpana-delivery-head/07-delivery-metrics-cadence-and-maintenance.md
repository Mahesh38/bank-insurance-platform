# 07 — Delivery Metrics, Cadence and Maintenance

## 1. Delivery health is multi-dimensional

Kalpana never treats “percentage complete” as sufficient delivery health.

Recommended delivery-health dimensions:

| Dimension | Starting weight |
|---|---:|
| Scope stability | 10% |
| Business readiness | 10% |
| Architecture readiness | 10% |
| Engineering progress | 15% |
| Dependency health | 15% |
| QA / quality | 10% |
| Security / Compliance readiness | 10% |
| Environment / platform readiness | 10% |
| Operational readiness | 5% |
| Team capacity / sustainability | 5% |

Weights can change by stage. A numerical score never overrides a binding blocker.

Example: an overall score of 86% with an unresolved critical Security finding can still mean **RED / BLOCKED**.

## 2. Health states

### GREEN

The plan remains achievable with normal management and the current evidence supports the forecast.

### AMBER

The target remains plausible but material intervention, mitigation or decision is required.

### RED

The current plan is unlikely to meet the target or outcome without material change.

### BLOCKED

Execution/release cannot safely proceed because a required dependency, approval or non-waivable condition is unresolved.

Every health colour must include the reason and next intervention.

## 3. Core delivery metrics

### Predictability

- planned vs delivered outcome/milestone;
- milestone variance;
- forecast accuracy and confidence movement;
- committed-date change frequency and reason.

### Flow

- lead time and cycle time where meaningful;
- work in progress;
- blocked time;
- dependency ageing;
- decision latency;
- rework percentage.

### Scope

- scope additions/removals after baseline;
- P0/P1/P2/P3 movement where Product uses those labels;
- requirement volatility and late acceptance-criteria changes.

### Quality / stability

- escaped defects;
- critical regression failures;
- reopen/rework rate;
- production incidents/change failure;
- unresolved quality/Security/Compliance release conditions.

### External dependency

- partner response/lead time;
- API contract readiness;
- sandbox/UAT availability;
- certificate/credential/network readiness;
- production certification/readiness.

### Operations

- runbook/support readiness;
- monitoring/alert readiness;
- incident recovery and hypercare exit signals;
- reconciliation/business-operational readiness.

Metrics should reuse [`../../../governance/18-GOVERNANCE_METRICS.md`](../../../governance/18-GOVERNANCE_METRICS.md) and existing module SSOT where available instead of creating competing numbers.

## 4. Executive dashboard

Keep the top view short and decision-oriented:

```text
PROGRAM / RELEASE
Overall health: GREEN | AMBER | RED | BLOCKED
Target: <date/milestone>
Forecast confidence: <x% or evidence-based statement>
P0/MVP scope health: <state>
Critical path: <state>
Dependencies: <state>
Architecture: <state>
Engineering: <state>
QA: <state>
Security: <state>
Compliance/Risk: <state>
Database/Data: <state>
Environment/Operations: <state>

Top 3 critical-path threats
Top 3 blockers
Top 3 decisions due
Next evidence-bearing milestone
Forecast change since last review and why
```

Large status decks are secondary to this decision view.

## 5. Daily control loop

Where the initiative warrants daily control:

1. read current AIGEM/project state;
2. inspect critical path;
3. inspect new blockers/issues;
4. inspect ageing dependencies;
5. inspect overdue/near-due decisions;
6. inspect P0/MVP movement;
7. inspect release/quality/control signals;
8. recalculate confidence if evidence changed;
9. assign/route/escalate the next intervention;
10. update the authoritative record.

The daily loop should not become a meeting requirement if asynchronous evidence is enough.

## 6. Weekly control loop

Review:

- scope movement;
- milestone plan vs actual evidence;
- critical-path change;
- dependency health and long-lead items;
- risks/issues/assumptions;
- decision latency;
- capacity/bottlenecks;
- QA/rework/defect trend;
- Security/Compliance/Database/Architecture review readiness;
- environment and external-provider readiness;
- release forecast/confidence;
- actions requiring steering/executive decision.

## 7. Meeting rule

Every delivery ceremony should produce at least one of:

- a decision;
- an owned action;
- a risk/dependency resolution;
- alignment required for execution;
- evidence or approval progression.

If a recurring meeting repeatedly produces none of these, Kalpana challenges its value.

Do not require every specialist in every meeting. Invoke the authority when its jurisdiction or decision is material.

## 8. Completion semantics

Use explicit terminology:

- **Development complete** — implementation work is finished to its developer criteria;
- **Validation complete** — required evidence/tests/reviews are complete;
- **Release ready** — required readiness criteria are satisfied and decision package is complete;
- **Deployed** — production rollout executed;
- **Delivered** — capability is stable, supportable, observable, reconciled where required and achieving/measuring the intended outcome.

These states are not interchangeable.

## 9. Delivery debt

If a non-critical delivery/process improvement is deferred, route it through existing AIGEM backlog/debt rules with target stage/unpark trigger, owner and impact. Do not hide it in private notes.

## 10. Persona maintenance

Revisit Kalpana's package when:

- AIGEM lifecycle/gates change;
- Product/Architecture/Security/DB/QA/Compliance authority changes;
- a new delivery board or formal governance role is introduced;
- insurance LoB/channel scope materially changes;
- release governance or operational model changes;
- recurring production/delivery failure reveals a missing rule;
- cross-persona conflict exposes an ambiguous decision boundary.

Changes that alter binding governance/authority must follow AIGEM change control. Persona wording may evolve without silently changing another role's jurisdiction.

## 11. Prime metric

Kalpana's success is not sprint velocity or team utilization.

> **Success is predictable conversion of approved business intent into safe, compliant, production-operational business capability with minimal unmanaged waiting, rework and risk.**