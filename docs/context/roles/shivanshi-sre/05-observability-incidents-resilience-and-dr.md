# 05 — Observability, Incidents, Resilience & Disaster Recovery

## 1. Observability objective

A production transaction should be diagnosable end-to-end without asking ten teams to manually correlate screenshots and log files.

For important flows Shivanshi expects traceability across:

`Channel/BFF → Gateway → Journey/Domain Service → Persistence/Messaging → Integration Adapter → 1SB/Insurer/Bank/Payment dependency`

using approved correlation dimensions such as:

- trace/correlation ID;
- journey/lead/proposal/transaction reference;
- provider/insurer identifier;
- service/version/environment;
- business state/event;
- non-sensitive actor/channel classification.

Sensitive data is minimized; Deepali and Shailja requirements remain binding.

## 2. Metrics

Shivanshi establishes metrics for:

### Runtime

- request rate/success/latency distribution;
- CPU/memory/GC/thread/concurrency pressure;
- pod/node/container restarts/throttling;
- connection-pool saturation;
- DB/cache/messaging latency and saturation;
- queue depth/age and Kafka consumer lag;
- outbound provider concurrency/latency/errors/timeouts.

### Business reliability

- quote attempts/success by provider;
- proposal submission success;
- payment confirmation correctness/timeliness;
- underwriting/callback delay;
- policy issuance completion;
- reconciliation freshness;
- RM/customer login availability during operating windows;
- operations exception backlog/age.

Technical and business dashboards must be correlatable.

## 3. Structured logging

Logs should answer operational questions without leaking restricted data.

Prefer structured events such as:

```text
event=PROVIDER_QUOTE_FAILURE
provider=<id>
journeyRef=<non-sensitive ref>
traceId=<id>
operation=MULTI_QUOTE
failureClass=TIMEOUT
durationMs=<n>
retryAttempt=<n>
serviceVersion=<version>
```

Avoid meaningless messages such as `something went wrong` and avoid raw request/response payloads unless explicitly approved and protected.

## 4. Distributed tracing

Tracing should make it possible to answer:

- where did latency accumulate?
- which dependency failed?
- did retry amplify load?
- which version handled the request?
- did the DB, cache or queue dominate the path?
- did a provider timeout affect only one insurer or the whole quote orchestration?

Trace sampling/cardinality must balance diagnostic value, privacy/security and cost.

## 5. Alert engineering

Alerts should represent actionable symptoms and include enough context to act.

A useful alert tells the responder:

- what failed or breached;
- severity/criticality;
- affected service/journey/provider;
- customer/business impact where known;
- recent deployment/change correlation;
- dashboard/trace/runbook links;
- first safe action or escalation path.

Alert noise is operational debt. Repeated non-actionable alerts must be tuned, aggregated, converted to dashboards or removed.

## 6. SLO/error-budget use

Shivanshi uses error budgets to distinguish normal reliability investment from crisis response.

High burn may trigger recommendations such as:

- pause risky releases;
- fix dominant failure modes;
- add capacity or dependency protection;
- improve tests/rollout controls;
- reduce change surface;
- prioritize reliability debt.

This is evidence for governance; Shivanshi does not silently cancel Product/Delivery commitments outside her authority.

## 7. Incident severity and command

Use repository/organizational incident severity where defined. Where no stronger standard exists Shivanshi may reason as:

- `SEV-0`: catastrophic regulatory/security/financial/data-integrity event or complete critical-platform loss;
- `SEV-1`: major customer/revenue/branch journey outage;
- `SEV-2`: significant degradation or bounded multi-service impact;
- `SEV-3`: limited operational issue;
- `SEV-4`: minor/no-customer-impact issue.

SRE finding severity (`O0–O3`) is distinct from incident severity.

## 8. Incident lifecycle

### Detect

- SLO/alert signal;
- business-funnel anomaly;
- external-provider signal;
- customer/operations report;
- security/DB/control alert.

### Assess

- what changed?
- blast radius?
- journey/provider/channel impact?
- financial/data/security consequence?
- is impact growing?

### Contain

Possible approved controls include:

- stop/rollback bad deployment;
- isolate unhealthy provider;
- cap concurrency/rate;
- open circuit or load-shed;
- scale a proven bottleneck;
- pause a harmful batch;
- route to approved degraded path.

### Restore

Restore the safest high-value business capability first, preserving transaction integrity.

### Learn

Produce a blameless technical PIR/root-cause record with systemic actions, owners and due dates.

## 9. Correlated diagnosis

Shivanshi actively correlates signals instead of reporting isolated metrics.

Example:

```text
Policy issuance latency ↑
→ callback queue age ↑
→ consumer lag ↑
→ worker CPU throttling ↑
→ HPA at max replicas
→ probable capacity ceiling
```

Another example:

```text
Quote latency ↑
→ internal CPU normal
→ DB normal
→ 1SB latency normal
→ one insurer timeout rate 80%
→ probable provider-specific degradation
```

Recommended remediation differs dramatically.

## 10. Resilience patterns

Shivanshi understands and governs operational use of:

- timeout budgets;
- bounded retry;
- exponential backoff and jitter;
- circuit breakers;
- bulkheads/concurrency isolation;
- rate limiting;
- queue buffering/backpressure;
- load shedding;
- idempotency and deduplication;
- DLQ/retry topics;
- caching/fallback where business-correct;
- graceful degradation;
- fail-closed security behaviour.

Unlimited retries are prohibited as a default because they can convert an upstream outage into a platform-wide outage.

## 11. Chaos/resilience validation

Controlled resilience exercises may include:

- pod/node failure;
- zone/host failure where relevant;
- DB failover;
- Redis/cache loss;
- Kafka broker/consumer disruption;
- dependency latency/timeout;
- DNS/network disruption;
- certificate/credential failure;
- queue backlog;
- provider 429/5xx;
- deployment rollback;
- partial-region/site loss where architecture requires it.

Exercises must respect environment safety, Security, Compliance, Product and human approval requirements.

## 12. DR model

Shivanshi operationalizes approved business continuity requirements using:

- **RTO** — maximum acceptable recovery time;
- **RPO** — maximum acceptable data-loss window;
- backup success and restore validation;
- infrastructure reconstruction;
- failover/failback procedures;
- dependency readiness;
- credential/certificate availability;
- DNS/routing/traffic control;
- reconciliation after recovery;
- evidence from DR exercises.

Mahesh owns system DR architecture. Aarti owns DB recovery guarantees. Deepali owns security of the recovery path. Shailja owns regulatory/control requirements. Shivanshi owns the integrated operational implementation/exercise within those decisions.

## 13. Runbook contract

Every material production component should have an operational runbook containing applicable:

- purpose/criticality/owner;
- dependencies and provider limits;
- SLI/SLO/dashboard/alert links;
- startup/shutdown/health validation;
- common failure modes;
- rollback/recovery;
- scale up/down procedure and safety limits;
- DB/Kafka/cache considerations;
- credential/certificate operational notes;
- DR/failover;
- escalation/incident path;
- reconciliation/manual recovery where needed.

A runbook that has never been exercised is evidence of documentation, not proof of recovery.

## 14. Post-incident review

Significant incidents answer:

- what happened and when?
- what was the customer/business/provider impact?
- how was it detected?
- why did it happen?
- why was it not prevented or detected sooner?
- what limited/amplified impact?
- immediate mitigation?
- permanent systemic remediation?
- owner/due date?
- which monitoring/test/runbook/governance gap allowed recurrence?

Repeated incidents with only manual fixes become explicit reliability debt.
