# Operational Readiness Review — <Service or Release>

> Board 7 (Operations) formal assessment, owned by **Shivanshi / SRE**. Required at S14 before
> go-live. Checks O1–O8 are the canonical AIGEM Board 7 checklist; O9–O13 are this platform's
> additions.

```yaml
orr:
  subject: "AU Bank Insurance Distribution Platform — R0 launch"
  stage: S14
  date:
  reviewer: "Shivanshi / SRE"
  reviewer_type: HUMAN
  verdict:                # READY | READY_WITH_CONDITIONS | NOT_READY
```

## 1. Checks

Each check needs an artefact. "Confirmed by the team" is not evidence.

| # | Check | Evidence required | Evidence (link) | Verdict |
|---|---|---|---|---|
| **O1** | **Deployability** — config, env vars, secrets, migrations, ordering | Successful reproducible pipeline deploy to UAT | | |
| **O2** | **Observability** — metrics, logs, traces, correlation IDs | Dashboard links; one trace walked end to end | | |
| **O3** | **Alerting** — what pages, on what threshold, to whom | Alert rules + routing + on-call roster | | |
| **O4** | **Failure modes and blast radius** | Failure-mode analysis; dependency failure tested | | |
| **O5** | **Rollback** — tested, sufficient given data written | Rollback **executed** in UAT + data-compatibility statement | | |
| **O6** | **Capacity and cost** | Load test at projected peak with headroom; cost model | | |
| **O7** | **Runbooks** | Runbook per alert; incident playbook; exercised once | | |
| **O8** | **Backward compatibility during rolling deploy** | Contract tests + rolling deploy with mixed versions live | | |
| **O9** | **DR exercised** | DR test record with measured RTO/RPO | | |
| **O10** | **Backup and restore proven** | Restore executed to a working state, timed | | |
| **O11** | **Data residency attested** | Region evidence across stores, backups, logs, archives | | |
| **O12** | **Provider protection** — rate limits, timeouts, breakers | Config + failure-injection test | | |
| **O13** | **Retention and purge operating** | Job execution record + immutability check | | |

**Verdicts:** `PASS` · `PASS_WITH_CONDITIONS` · `FAIL` · `NOT_APPLICABLE` (with a one-line reason)

## 2. Operational profile

```yaml
profile:
  criticality:              # supplied by Product, not assumed by SRE
  business_hours:           # RM-assisted journeys are business-hours weighted
  peak_period:              # Q4 tax season for term life in India
  slos: []                  # journey-level and component-level
  error_budget_policy:      # agreed with Product before launch
  rto:
  rpo:
  on_call_model:
  escalation_path:
```

## 3. Dependencies

| Dependency | Criticality | Failure behaviour | Owner | Escalation contact |
|---|---|---|---|---|
| | stops sale / degrades / invisible | | | |

## 4. Known operational risks

| Risk | Severity (O0–O3) | Mitigation | Accepted by | Expiry |
|---|---|---|---|---|
| | | | | |

## 5. Conditions

If the verdict is `READY_WITH_CONDITIONS`, every condition needs an owner, a backlog ID and a date.
Conditions become acceptance criteria and are tracked to closure — they are not notes.

| Condition | Owner | Backlog ID | Due |
|---|---|---|---|
| | | | |

## 6. Verdict

```yaml
verdict:
  decision:
  rationale:
  blocking_items: []
  date:
  signed_by: "Shivanshi / SRE"
```

> **Board 7 may block a launch.** Delivery pressure, a committed date, and executive interest do
> not transfer operational authority. A `NOT_READY` verdict is overturned by fixing what is not
> ready, not by aggregating other approvals over it.
