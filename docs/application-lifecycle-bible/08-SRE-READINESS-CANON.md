# 08 — SRE & Operational Readiness Canon

**Owner:** Shivanshi — Principal Insurance Platform SRE / Reliability Engineering Head
(AIGEM R10, Board 7 — Operations)
**Authority:** owns platform operability, CI/CD mechanics, observability standards, capacity, and
the Board 7 verdict. Does **not** own application logic, service boundaries, or business
behaviour.

---

## 1. The question Board 7 asks

> *Can the approved capability be safely deployed, observed, scaled, operated, contained and
> recovered under real insurance business load?*

Note what is not in that sentence: whether the feature is valuable, correct, or compliant. Those
belong to Product, QA and Compliance. Board 7 asks only whether it can be **run**.

---

## 2. Readiness is built across stages, not bolted on at S14

The most common enterprise failure is treating operational readiness as a launch checklist. By
S14 it is far too late to discover that a service emits no metrics.

| Stage | SRE contribution | Output |
|---|---|---|
| S07 | Availability and DR architecture; RTO/RPO targets; NFR feasibility review | DR architecture, NFR sign-off |
| **S08** | CI/CD platform mechanics; build and deployment automation; golden paths | Pipeline, developer platform |
| **S09** | IaC, landing zone, environments, observability substrate, secrets, deployment and rollback | The runnable platform |
| S10 | Provider protection: timeouts, retries, circuit breakers, rate limiting | Resilience policy + config |
| S11 | Instrumentation of the slice; first dashboards; first alerts | Working telemetry |
| S12 | Performance and resilience testing; runbook; failure-mode validation | Runbook, load test, chaos results |
| **S14** | **Operational Readiness Review**; SLO definition; DR exercise; on-call | ORR record, signed |
| S15 | SLO reporting, error budgets, incident learning, toil reduction, capacity | Operating cadence |

> **Rule SR-1 — A service with no metrics, no logs with correlation IDs, and no runbook is not
> "nearly ready to launch". It is not built.** Observability is part of Definition of Done at
> S11, not a S14 task.

---

## 3. SLIs and SLOs

Defined by Shivanshi against business criticality supplied by Rajal. Technical metrics do not
become business targets without Product.

### Journey-level SLIs (what the business actually feels)

| Journey step | SLI | Proposed SLO | Why this number |
|---|---|---|---|
| Quote generation | Successful quote responses / total, p95 latency | 99.5% success, p95 < 5s | RM is sitting with a customer; beyond ~5s the conversation breaks |
| Proposal submission | Successful submissions / total | 99.5% | Retry is expensive and confusing for the customer |
| Payment link issuance | Successful issuance / total | 99.9% | Money path — failure loses the sale at the last step |
| Policy issuance confirmation | Confirmations received / payments completed | 99.9% | The 4-part "sold" definition depends on it |
| Reconciliation | Payments reconciled within 24h | 100% | Financial control, non-negotiable |
| Authentication | Successful auth / attempts | 99.9% | Blocks every journey |

### Component SLIs

Availability, p50/p95/p99 latency, error rate, saturation (CPU, memory, connections, queue depth)
per service; and for every external dependency: availability, latency, and error rate **measured
from our side**, because a partner's status page is not our SLI.

### Error budget policy

| Budget consumed | Consequence |
|---|---|
| < 50% | Normal feature delivery |
| 50–75% | Reliability work prioritised alongside features |
| 75–100% | Feature freeze on the affected service; reliability only |
| Exhausted | Change freeze except reliability and D0 fixes; Product + SRE agree a recovery plan |

Error budgets are a **Product** conversation, not a technical one. Rajal agrees the policy;
Shivanshi reports against it.

---

## 4. Observability standard

Required of every service before it may pass S11.

| Signal | Requirement |
|---|---|
| **Metrics** | RED (rate, errors, duration) per endpoint; USE (utilisation, saturation, errors) per resource; business metrics per journey step |
| **Logs** | Structured JSON; correlation ID on every line; **no PII, proven by test**; consistent levels |
| **Traces** | Distributed tracing across every service hop and every external call; trace ID correlated with logs |
| **Audit events** | Separate from operational logs; immutable; attributable to an actor |
| **Health** | Liveness and readiness distinguished; readiness reflects real dependency state |

Correlation ID propagates: client → BFF → service → service → external provider → back. Without
it, a failed sale cannot be reconstructed, and reconstructing failed sales is the entire reason
this platform exists.

### Alerting discipline

| Principle | Meaning |
|---|---|
| Alert on symptoms, not causes | Page on "quote success rate below SLO", not "CPU at 80%" |
| Every page is actionable | If there is no action, it is a dashboard, not a page |
| Every page has a runbook | Link in the alert payload |
| Business-hours awareness | RM-assisted journeys are business-hours; a 3am quote-latency page is likely wrong |

> **Rule SR-2 — An alert with no runbook is an interruption, not an alert.** Alert fatigue is how
> real incidents get missed, and it is created by well-meaning over-alerting.

---

## 5. Operational Readiness Review (ORR) — the S14 gate

The ORR is Board 7's formal assessment. Template:
[`templates/ORR.md`](./templates/ORR.md). Checks map to the canonical O1–O8.

| # | Check | Evidence required |
|---|---|---|
| **O1** | **Deployability** — config, env vars, secrets, migrations, ordering | Successful deploy to UAT via pipeline, reproducible |
| **O2** | **Observability** — metrics, logs, traces, correlation IDs | Dashboard links; a trace walked end to end |
| **O3** | **Alerting** — what pages, on what threshold, to whom | Alert rules + routing + on-call roster |
| **O4** | **Failure modes and blast radius** | Failure-mode analysis; dependency failure tested |
| **O5** | **Rollback** — tested, and sufficient given data written | Rollback **executed** in UAT, with a data-compatibility statement |
| **O6** | **Capacity and cost** | Load test at projected peak + headroom; cost model |
| **O7** | **Runbooks** | Runbook per alert; incident playbook; exercised at least once |
| **O8** | **Backward compatibility during rolling deploy** | Contract tests + a rolling deploy performed with mixed versions live |

Plus, for this platform specifically:

| # | Check | Evidence |
|---|---|---|
| O9 | **DR exercised** — RTO/RPO achieved, not merely designed | DR test record with measured times |
| O10 | **Backup and restore proven** | Restore executed to a working state, timed |
| O11 | **Data residency attested** | Region configuration evidence across all stores, backups and logs |
| O12 | **Provider protection** — 1SB and insurer rate limits, timeouts, breakers | Config + a failure-injection test |
| O13 | **Retention and purge jobs** operating | Job execution record + immutability check |

> **Rule SR-3 — "Untested rollback" is the same as "no rollback".** Every incident where rollback
> was assumed and then failed was preceded by a checklist saying rollback existed.

**Severity: Shivanshi's `O0`–`O3` is operational severity and never replaces AIGEM `P1`–`P5`.**

---

## 6. Incident management

### Classification

| Sev | Definition | Response | Comms |
|---|---|---|---|
| **O0** | Money incorrect or lost; data loss; regulated data exposure; platform down | Immediate, all hands | Executive + regulator assessment |
| **O1** | Critical journey unavailable; major degradation | 15 min | Business stakeholders |
| **O2** | Degraded but usable; workaround exists | 1 hour | Affected teams |
| **O3** | Minor; no business impact | Next business day | Ticket only |

### During an incident, the boundaries hold

Shivanshi commands the technical response. She does **not** acquire other personas' authority
because the situation is urgent:

- Security dimension → **Deepali**
- Database integrity → **Aarti**
- Business priority and acceptable degraded experience → **Rajal**
- Code remediation → **Amit**
- Regulatory reportability → **Shailja**
- Stakeholder coordination → **Kalpana**

Specifically prohibited as incident workarounds: deleting or mutating production data without the
applicable data/business/security authority; unbounded retries against a provider; disabling a
compliance control to restore throughput.

### Postmortem

Blameless, within five working days, for every O0 and O1. Must produce: timeline, contributing
factors, why detection took as long as it did, why recovery took as long as it did, and **action
items with owners and dates** in the backlog. A postmortem whose actions never land in a backlog
is a story, not a control.

---

## 7. Capacity and cost

| Input | Source |
|---|---|
| Expected RM count, branches, and per-RM daily journeys | Rajal |
| Seasonal peaks (financial year end, tax-saving season) | Rajal |
| Per-journey resource cost | Load testing |
| Provider rate limits | 1SB and insurer contracts |
| Database limits | Aarti |

Term insurance in India peaks hard in Q4 (Jan–Mar tax season). A capacity model built on annual
averages will fail exactly when the business most needs the platform. Model the peak, not the
mean.

Cost per issued policy is a **Product** metric, not merely an infrastructure one — it belongs in
the S15 operating review alongside conversion and reconciliation.
