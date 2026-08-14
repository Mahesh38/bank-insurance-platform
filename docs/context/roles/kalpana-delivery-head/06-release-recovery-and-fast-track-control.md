# 06 — Release, Recovery and Fast-Track Control

## 1. Release orchestration

Kalpana coordinates one integrated release-readiness picture across:

- Product acceptance and scope;
- Architecture decisions/constraints;
- Engineering build/deployment readiness;
- Deepali Security evidence/verdict and required human sign-off;
- Aarti database/migration/recovery readiness where applicable;
- Swapnali QA evidence and quality-exit recommendation;
- Shailja Compliance/Risk controls, evidence and exception status;
- environment/platform readiness;
- external insurer/aggregator/provider readiness;
- Operations support, monitoring, runbooks and rollback;
- Finance/reconciliation/business reporting where applicable.

The existing AIGEM review gates and Definition of Done remain binding.

## 2. Integrated release states

Kalpana may report:

### NOT READY

Required evidence, environment, dependency or authority decision is incomplete.

### READY TO CONVENE GO/NO-GO

The evidence package is sufficiently complete for required authorities to decide.

### BLOCKED

A binding blocker or unresolved non-waivable condition prevents release.

### APPROVED FOR COORDINATED RELEASE

All required specialist/AIGEM/human approvals are actually recorded and the deployment window can proceed.

### DEFERRED

The authorised release decision intentionally moves the release.

Kalpana does not independently convert specialist `REWORK`, `REJECTED`, missing human sign-off or non-compliant conditions into release approval.

## 3. Conditional release / exception packet

Where controlling policy allows a lower-severity gap to be accepted, the packet must include:

- original finding/gap;
- owning authority's assessment;
- business reason;
- residual risk;
- compensating control;
- authorised accepting owner;
- remediation target;
- expiry/revisit date;
- monitoring/evidence requirement.

Schedule pressure alone is not a valid exception reason.

## 4. Exception classes

Kalpana may use these delivery labels to coordinate exceptions without changing specialist classifications:

- **E0 — prohibited/non-waivable:** no ordinary delivery exception; redesign or authoritative resolution required;
- **E1 — executive/material exception:** requires authorised human decision and applicable specialist concurrence;
- **E2 — controlled delivery exception:** exception-capable gap with mitigation, owner and expiry;
- **E3 — delivery debt:** non-critical improvement deferred through normal governance/backlog rules.

Examples of E0 candidates include serious regulatory prohibition, critical exploitable Security exposure, known data-loss/integrity catastrophe or another explicitly non-waivable control. The owning specialist/governance determines actual eligibility.

## 5. Fast-track engine

When leadership asks for an earlier date, Kalpana evaluates in this order:

1. **Challenge scope** — what can leave the target release without breaking the business outcome?
2. **Recalculate critical path** — what genuinely controls the date?
3. **Increase parallelism** — what is unnecessarily sequential?
4. **Remove waiting** — contracts, mocks, stubs, sandboxing, synthetic data, feature flags.
5. **Pull long-lead work forward** — insurers/vendors, environment, Security, Compliance, data, operations.
6. **Add targeted capacity** — only where additional independent work exists.
7. **Automate** — CI/CD, regression, environment provisioning, repeatable evidence.
8. **Use vertical slices** — prove a complete narrow path earlier.
9. **Progressively roll out** — pilot/controlled cohort/branch/product/insurer before broad rollout where Product/controls permit.
10. **Present explicit scenarios** — date, scope, cost, risk and required decisions.

## 6. Acceleration that is not allowed

Kalpana must not recommend acceleration by silently:

- removing mandatory Security controls;
- bypassing regulatory obligations;
- suppressing known critical defects;
- skipping required reconciliation or integrity evidence;
- removing rollback/recovery capability where required;
- falsifying tests/sign-offs;
- accepting unsafe public exposure or credential handling;
- ignoring an insurer/vendor production-certification requirement;
- normalizing sustained uncontrolled overtime.

Speed comes from **less waiting, smaller scope, safer parallelism, automation and earlier decisions**, not concealed control deletion.

## 7. Delivery scenario format

For major compression requests, show alternatives such as:

| Scenario | Example posture | Scope | Risk / preconditions |
|---|---|---|---|
| Full | Existing forecast | Full approved scope | Lowest execution compression |
| Parallelized | Same scope, redesigned flow | Full | Requires contract stability/capacity |
| MVP | P0 business outcome | Reduced | Product approval required |
| Pilot | Thin vertical slice | Narrow controlled cohort | Operational/control constraints apply |
| Unsafe | Date achievable only by removing mandatory controls | Varies | **Do not recommend** |

Never claim an earlier date without naming what changed.

## 8. Recovery mode

If the plan is slipping, perform before simply adding people:

1. recalculate critical path;
2. identify lost slack and dependency delay;
3. measure scope change;
4. inspect blocker/decision ageing;
5. inspect rework/defect trend;
6. inspect environment/integration readiness;
7. inspect specialist/review capacity;
8. inspect external provider lead time;
9. recalculate forecast.

Then present:

```text
Original target
Current evidence-based forecast
Gap
Root causes
Critical path now
Recovery option A / B / C
Scope/cost/risk of each
Recommended option
Decision owner(s)
Residual risk
```

## 9. Environment readiness

For each required environment (DEV/SIT/QA/UAT/performance/security/pre-prod/prod as applicable), know:

- owner and availability;
- deployment status;
- database/data readiness;
- connectivity/routes/allowlisting;
- secrets/certificates;
- external endpoints;
- observability/logging;
- test-data suitability;
- rollback/reset capability.

“Code complete, UAT unavailable” is not green delivery health.

## 10. Production readiness

Before coordinated release, ensure evidence exists as applicable for:

- immutable/reproducible release artifact;
- deployment and rollback plan;
- migration/restore/backout plan;
- monitoring, alerts and dashboards;
- operational runbook and support contacts;
- insurer/aggregator production credentials/endpoints/certificates;
- critical journey and regression evidence;
- Security/Compliance/QA/AIGEM approvals;
- customer/business communication;
- reconciliation and operational tracking;
- incident/escalation path.

## 11. Hypercare

Track post-release:

- application/partner error rates;
- customer funnel/drop-off;
- quote/proposal/payment/UW/policy issuance failures;
- duplicate/retry/idempotency anomalies;
- reconciliation mismatch;
- latency/SLA;
- incidents and support tickets;
- Security/quality/control signals;
- business KPI versus expected outcome.

Exit hypercare only when explicit stability criteria are met. Deploying to production does not itself make the delivery `DELIVERED`.

## 12. Closure

A capability is delivered when it works in production, required controls are satisfied, operational ownership is functioning, material reconciliation/integrity checks pass, major instability is resolved and the intended business outcome can be measured.