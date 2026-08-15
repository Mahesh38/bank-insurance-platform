# Operations Runbook — 1SB Integration Service

**Gate criterion:** [GATE-P4 4.5](../../../governance/04-STAGE_GATES.md#6-project-gates-l3) —
"Runbook: secrets rotation, IP whitelist, 1SB 401/5xx incident"
**Board:** 7 — Operations ([11 §10](../../../governance/11-REVIEW_GATES.md#10-board-7--operations))
**Owner / approver:** **Shivanshi — Principal Insurance Platform SRE (R10)**, named by CR-008
**Status:** `DRAFT — awaiting Board 7 verdict`. This document does **not** close 4.5 on its own;
it is the evidence artefact 4.5 requires. Criterion 4.5 closes when Shivanshi records an
`APPROVED` (or `APPROVED_WITH_CONDITIONS`) verdict against it.
**Scope:** `1sb-integration-service` and the `bank-persistence-service` it depends on, in
**UAT** — the only environment this stage targets. Production topology is Phase 6.

---

## 0. Read this first — three constraints that shape every procedure below

These are current, verified properties of the deployed system. They are not defects to report;
they are already-tracked debt ([`known_open_debt`](../../../governance/state/CURRENT-STATE.yaml)).
Every procedure in this runbook is written to be correct *given* them.

| # | Constraint | Consequence for operations | Tracked as |
|---|------------|----------------------------|------------|
| C1 | **The AWS Secrets Manager provider is a stub.** `application-prod.yml` pins `insurance.secrets.source: AWS_SECRETS_MANAGER` with no fallback, and that provider is not implemented. | The `prod` profile **cannot start today** — it fails fast at `SecretsStartupValidator`. UAT runs on `INSURANCE_SECRETS_SOURCE=ENV`. Every rotation procedure below is therefore an **ENV-source** procedure. | TD-006 |
| C2 | **Idempotency and job ownership are in-memory** (`InMemoryIdempotencyStore`; poller state held per-process). | The service is **single-instance only**. Do not scale replicas > 1 — two instances will duplicate polls and lose idempotency de-duplication. Restart = loss of in-flight job state and the idempotency window. | TD-010 |
| C3 | **There is no sandbox E2E suite and no performance baseline** (gate criteria 4.1 and 4.6 are open). | There is no green-path regression signal to compare against during an incident, and no measured p95 to declare a latency regression against. Diagnosis below leans on metrics and audit events, not on a known-good benchmark. | 4.1 / 4.6 open |

> **Escalation contact for anything this runbook does not cover:** Shivanshi (R10 / Board 7).
> For a criterion that cannot be met, escalate to **Architect + PO** before the gate is marked
> `CANDIDATE` ([RUNBOOK §9](../../../governance/RUNBOOK.md#9-escalation)).

---

## 1. Service topology and deployment (O1, O8)

Both services ship in **one combined Docker image** (root `Dockerfile`, started by
`docker-entrypoint.sh`):

```text
container
├── bank-persistence-service   :8081   started FIRST  (heap -Xmx384m)
│     └── Postgres datastore, Flyway migrations, audit + raw_payload
└── 1sb-integration-service    :8080   started AFTER persistence liveness passes
      └── outbound → 1SB  (https://demo.api.1silverbullet.tech in sandbox)
```

- Only **:8080** is published. `bank-persistence-service` is internal-only, reached over
  localhost inside the container.
- The entrypoint **waits on the persistence liveness probe** before starting the integration
  service, forwards `SIGTERM`/`SIGINT` to both, and exits non-zero if either process dies so the
  orchestrator restarts the container.
- Render health check: `GET /actuator/health/liveness` (see `render.yaml`).
- Heap caps are explicit (`PERSISTENCE_JAVA_OPTS`, `ONESB_JAVA_OPTS`, both `-Xmx384m`) because
  two JVMs share the plan's RAM. **If the container OOMs on boot, raise the plan or lower the
  caps — do not remove them**, or one JVM will OOM-kill the other non-deterministically.

**Rolling deploy (O8):** with C2 in force there is no safe rolling overlap. Deploy is
**stop-then-start**, single instance. In-flight quote/proposal polls are lost on restart; the
bank caller sees the job in `TIMEOUT` (reason `POLL_TIMEOUT`) or must re-submit under a new
`Idempotency-Key`. Announce a UAT deploy window to the consuming bank team first.

**Rollback (O5):** redeploy the previous image tag. Rollback is safe for the integration service
(stateless beyond in-memory job state, which a restart discards anyway). It is **not**
automatically safe for `bank-persistence-service` if the newer image applied a Flyway migration —
check `flyway_schema_history` before rolling that service back, and treat any applied migration
as forward-only unless a down path has been written and tested.

---

## 2. Secrets — inventory and rotation

### 2.1 Inventory

| Secret | Consumed by | Source in UAT | Blank/absent ⇒ |
|--------|-------------|---------------|----------------|
| `ONESB_API_KEY` | 1SB Basic Auth | ENV | **startup aborts** |
| `ONESB_API_SECRET` | 1SB Basic Auth | ENV | **startup aborts** |
| `ONESB_DISTRIBUTOR_ID` | 1SB request attribution | ENV | **startup aborts** |
| `RAW_PAYLOAD_ENCRYPTION_KEY` | AES-256 raw-payload encryption at rest (COMP-003) | ENV | persistence service cannot encrypt/decrypt raw payloads |
| `POSTGRES_PASSWORD` | persistence datastore | ENV | persistence service cannot connect |

`SecretsStartupValidator` (`@Order(1)`, runs before traffic is served) validates the three 1SB
credentials and **aborts startup** if any resolves blank or unavailable. This is deliberate: a
missing credential fails at boot, not on the first customer request. Validation is skipped only
under the `test` / `integration-test` profiles.

Secret source is selected by `INSURANCE_SECRETS_SOURCE` → `PROPERTIES` (default/local),
`ENV` (UAT), `AWS_SECRETS_MANAGER` (prod — **stub, see C1**).

### 2.2 Rotating a 1SB credential (`ONESB_API_KEY` / `ONESB_API_SECRET`)

**Expect downtime.** With C2 there is no second instance to drain onto, and the credential is
read at startup.

```text
1. Obtain the new key/secret from the 1SB RM. Confirm which distributorId it is bound to —
   a rotated credential issued against a different distributorId will authenticate and then
   fail business validation, which looks like a data bug, not an auth bug.
2. Announce the window to the consuming bank team (in-flight jobs will be lost).
3. Update the value in the deployment's secret store:
     Render → service → Environment → edit the `sync: false` var → Save.
   Never place a real credential in git, .env, render.yaml, or a ticket.
4. Redeploy / restart the service (the value is read at boot; editing it alone does nothing).
5. Verify, in this order:
     a. Startup log contains "1SB credentials validated successfully."
        Absent  ⇒ the new value did not reach the process. Do NOT retry blindly — go to §2.4.
     b. GET /actuator/health/readiness returns UP.
     c. Exercise one real quote against the 1SB sandbox and confirm 200 + a job that reaches
        COMPLETED. A green liveness probe does NOT prove the credential works — the probe never
        calls 1SB.
6. Confirm the counter `onesb.upstream.auth.failure` is flat after the change.
7. Ask the 1SB RM to revoke the OLD credential only after step 5c passes.
```

**Rollback:** restore the previous value in the secret store and restart. This is why step 7 —
revoking the old credential last — is not optional: revoking early removes the rollback path.

### 2.3 Rotating `RAW_PAYLOAD_ENCRYPTION_KEY`

**Do not rotate this key as a routine operation.** Raw payloads already at rest were encrypted
under the previous key; changing the value without a re-encryption path makes existing rows
undecryptable, which is a **compliance evidence loss**, not an availability incident.

If rotation is required (suspected key compromise), it is an **Architect + Compliance (Shailja,
Board 6) + SRE** decision, not a runbook step. Raise it via the escalation table in
[RUNBOOK §9](../../../governance/RUNBOOK.md#9-escalation). Generate replacement keys with
`openssl rand -base64 32` (must decode to exactly 32 bytes).

### 2.4 Secret did not take effect

| Symptom | Most likely cause |
|---------|-------------------|
| Startup aborts: `required 1SB credential '<name>' is unavailable` | `INSURANCE_SECRETS_SOURCE` does not match where you put the value (e.g. set in ENV while the profile resolves `PROPERTIES`) |
| Startup aborts: `... resolved to blank` | Variable exists but is empty — commonly a trailing-newline or quoting error when pasting |
| Starts, then every 1SB call 401s | Credential is valid-shaped but wrong/revoked → §4 |
| Starts, 1SB authenticates, business errors on every request | Credential bound to a different `distributorId` → confirm with the 1SB RM |

---

## 3. IP whitelist / egress

> **Open dependency.** `CONFIRM-01 §D` records the 1SB IP whitelist as **still pending**
> (D3 "1SB confirms whitelist live" — not done). Until 1SB confirms, egress failures are an
> *expected* condition in UAT, not an incident to page on.

**Signature of a whitelist problem:** connection timeouts or connection-refused on **every**
outbound 1SB call, from a service that started cleanly (credentials validated) — i.e. the
failure is at the transport layer, before any HTTP status exists.

```text
Distinguishing whitelist failure from credential failure:

  no HTTP status at all, calls hang to the 3000ms connect timeout   → egress/whitelist  → §3
  HTTP 401 returned by 1SB                                          → credential        → §4
  HTTP 5xx returned by 1SB                                          → upstream outage   → §5
```

**Diagnosis:**

1. Confirm the service resolved and attempted the call: `onesb.http.call` timer records an
   attempt; `UPSTREAM_UNAVAILABLE` appears on the error counter with no `statusCode` tag value
   from 1SB.
2. From the **deployed egress IP** (not a laptop), check reachability of
   `onesb.client.base-url`. A curl that works from your machine and not from the deployment
   proves the whitelist, not the network.
3. Confirm the deployment's current public egress IP against the address 1SB whitelisted.
   **A platform redeploy can change the egress IP** on shared-infrastructure plans — this is the
   most common cause of "it worked yesterday".

**Action:** send 1SB RM the current egress IP and request whitelist confirmation. Record the
outcome against `CONFIRM-01 §D`. Do not work around it by disabling TLS verification or routing
through an unapproved proxy — both are standing-constraint violations.

**Relevant timeouts:** `ONESB_CONNECT_TIMEOUT_MS` (default 3000), `ONESB_READ_TIMEOUT_MS`
(default 30000). A whitelist failure manifests against the *connect* timeout.

---

## 4. Incident — 1SB returns 401 (`UPSTREAM_AUTH_FAILURE`)

**Severity:** O1 if sustained across all calls (the Term path is fully down); O2 if intermittent.

**Behaviour by design — confirm before diagnosing:** `OneSbHttpClient` handles 401 as a distinct
terminal case and **never retries it**. Retrying a 401 risks tripping a 1SB lockout, so the
absence of retries is correct, not a bug to fix during the incident.

- Bank-facing response: normalised envelope, code `UPSTREAM_AUTH_FAILURE`, **not** retryable.
  Raw 1SB JSON never reaches the caller.
- Metric: `onesb.upstream.auth.failure` counter, tagged by `operation`.
- Audit: an outbound audit event is still emitted (`upstreamHttpStatus=401`) with a SHA-256 hash
  of the **masked** request body — never the body itself.

**Triage:**

```text
1. Is it ALL operations or one?
     all  → credential is wrong, expired, or revoked            → step 2
     one  → 1SB-side permission for that endpoint/distributor   → step 4

2. Was a credential rotated, or the service redeployed, in the last 24h?
     yes → §2.4, then re-run the §2.2 verification steps
     no  → step 3

3. Ask the 1SB RM whether the credential was revoked or expired on their side.
   Sandbox credentials are commonly time-boxed. Do NOT keep retrying while waiting —
   repeated 401s can trigger an upstream lockout that outlives the original cause.

4. Confirm the distributorId bound to the credential still has the product/LOB enabled
   (CONFIRM-02 catalog). A de-provisioned product can present as 401 on that path only.
```

**Containment while blocked:** the failure is already contained — callers receive a
non-retryable `UPSTREAM_AUTH_FAILURE` rather than hanging or retry-storming. No traffic shedding
is required. Notify the consuming bank team that the Term path is down and give an ETA from the
1SB RM.

**Do not:** rotate credentials speculatively to "try something". Rotation costs a restart, loses
in-flight jobs, and destroys the evidence of what the failing credential was.

---

## 5. Incident — 1SB returns 5xx (`UPSTREAM_UNAVAILABLE`)

**Severity:** O1 if sustained; O2 if error rate is elevated but the path completes.

**Behaviour by design:** any 1SB status ≥ 500 normalises to `ServiceException.upstreamUnavailable`
→ bank code `UPSTREAM_UNAVAILABLE` (inferred status 503). The full upstream-status mapping:

| 1SB response | Bank error code | HTTP to caller | Retryable |
|--------------|-----------------|----------------|-----------|
| 401 | `UPSTREAM_AUTH_FAILURE` | 401 | no |
| 4xx (non-401) | `UPSTREAM_BUSINESS_ERROR` | 422 | no |
| ≥ 500 | `UPSTREAM_UNAVAILABLE` | 503 | — |
| anything else unexpected | `UPSTREAM_BAD_RESPONSE` | 502 | no |
| connect/read timeout | `UPSTREAM_UNAVAILABLE` | 503 | — |

> There is **no circuit breaker** — it is explicitly out of scope until Phase 5.5
> ([`CURRENT-STATE.yaml`](../../../governance/state/CURRENT-STATE.yaml) `out_of_scope`). During a
> sustained 1SB outage the service will keep attempting calls at the configured poll cadence.
> That is a known, accepted posture for UAT at this stage, not a gap to fix mid-incident.

**Triage:**

```text
1. Scope it: one operation or all? Check `onesb.http.call` timer tagged by operation/statusCode
   and the error counter tagged errorCode=UPSTREAM_UNAVAILABLE.
2. Confirm it is upstream, not us: the service's own liveness/readiness are UP and
   bank-persistence-service is healthy. If persistence is also failing, you have a
   container-level problem (§1), not a 1SB outage.
3. Contact the 1SB RM with: affected operations, start time, sample correlation IDs
   (X-Correlation-Id → MDC `correlationId`), and observed statuses. Sandbox 5xx is frequently
   a 1SB-side deploy.
4. Watch job outcomes: polls that never complete terminate at max attempts
   (ONESB_POLL_MAX_ATTEMPTS, default 20; backoff 1000ms × 2.0, capped 30000ms) and the job goes
   to TIMEOUT with reason POLL_TIMEOUT. Callers see QUOTE_TIMEOUT rather than a hang.
```

**Recovery:** no action is needed on our side when 1SB recovers — there is no breaker to reset
and no queue to drain. Jobs that already reached `TIMEOUT` are **terminal**; the bank caller must
re-submit under a **new** `Idempotency-Key`. Say this explicitly when you notify them, or they
will retry the old key and get the cached terminal outcome back.

---

## 6. Observability — what to look at (O2, O3)

**Endpoints** (`management.endpoints.web.exposure`): `health`, `info`, `metrics`, `prometheus`.
Liveness and readiness probes are enabled. Health detail is `when-authorized`, and **`never`**
under the `prod` profile.

**Metrics** (`com.bank.common.observability.MetricNames`):

| Metric | Type | Tags | Read it for |
|--------|------|------|-------------|
| job created | counter | `lob`, `jobType` | inbound demand |
| job terminal | counter | `lob`, `jobType`, `finalStatus` | `finalStatus=TIMEOUT` rising ⇒ §5 |
| job duration | timer | `lob`, `jobType`, `outcome` | end-to-end latency |
| `onesb.http.call` | timer | `lob`, `operation`, `statusCode` | upstream health + latency |
| error events | counter | `lob`, `errorCode`, `source` | which normalised code is firing |
| `onesb.upstream.auth.failure` | counter | `operation` | §4 |
| idempotency cache hits | counter | — | duplicate-submission rate |

**Correlation:** callers send `X-Correlation-Id`; it lands in MDC as `correlationId` and is the
join key across service logs, audit events and the 1SB conversation. **Always collect correlation
IDs before escalating to the 1SB RM** — without them the upstream cannot find the request.

**Alerting (O3) — not yet configured.** Dashboards, alerts and SLOs are parked to Phase 6.2 and
are deliberately out of scope here. Until then, UAT monitoring is **pull-based**: the on-call
engineer checks the metrics above during the UAT window. This is a stated gap, not an oversight —
if it is unacceptable for UAT, that is a scope change for Architect + PO, not a runbook edit.

**PII:** no PII in logs is a standing constraint (COMP-002). `paymentUrl` is never logged or
audited — only session/application references. If you find PII in a log sample while diagnosing,
that is a **compliance incident** → Shailja (Board 6), not a note in the ticket.

---

## 7. Failure modes and blast radius (O4)

| Failure | Blast radius | Caller sees | Auto-recovers? |
|---------|--------------|-------------|----------------|
| 1SB credential invalid | Whole Term path | `UPSTREAM_AUTH_FAILURE`, non-retryable | No — §2.2 |
| Egress/whitelist broken | Whole Term path | `UPSTREAM_UNAVAILABLE` after connect timeout | No — §3 |
| 1SB 5xx outage | Whole Term path | `UPSTREAM_UNAVAILABLE` / `QUOTE_TIMEOUT` | Yes, when 1SB recovers |
| 1SB slow (< read timeout) | Latency only | Success, degraded latency | Yes |
| Poll exceeds max attempts | Single job | `QUOTE_TIMEOUT`, job `TIMEOUT` | No — caller re-submits, new key |
| `bank-persistence-service` down | Whole path (jobs cannot persist) | 5xx | On container restart |
| Container OOM | Both services | Connection failure | Orchestrator restart; fix heap caps (§1) |
| Service restart | **All in-flight jobs + idempotency window** (C2) | Jobs never complete | No — inherent to C2 |
| Second replica started | **Duplicate polling, broken idempotency** (C2) | Inconsistent | No — **never scale > 1** |

**Capacity and cost (O6):** unmeasured. Criterion 4.6 (p95 quote under nominal concurrency) is
open, so there is **no capacity statement to make** and no defensible concurrency limit to quote
to the bank team. Sizing today is one instance, two JVMs at 384 MB heap each. Any capacity
question beyond "is UAT single-caller traffic fine" must wait for 4.6.

---

## 8. Board 7 checklist coverage

| # | Check | Where | State |
|---|-------|-------|-------|
| O1 | Deployability: config, env vars, secrets, migrations, ordering | §1, §2 | Covered |
| O2 | Observability: metrics, logs, traces, correlation IDs | §6 | Covered |
| O3 | Alerting: what pages, on what threshold | §6 | **Gap — parked to Phase 6.2**; UAT is pull-based |
| O4 | Failure modes and blast radius | §7 | Covered |
| O5 | Rollback: tested, sufficient given data written | §1 | Covered for the integration service; **Flyway down-path untested** |
| O6 | Capacity and cost impact | §7 | **Gap — blocked on criterion 4.6** |
| O7 | Runbook updates needed | this document | Covered |
| O8 | Backward compatibility during rolling deploy | §1 | Covered — **no rolling deploy possible (C2)** |

Three of eight checks are **not** fully satisfiable at this stage (O3, O5-partial, O6), each for a
reason that is already governed and recorded. They are surfaced here rather than written around,
so Board 7's verdict is made on the real posture: this runbook makes the Term path **operable and
recoverable in UAT**, and does not claim production readiness.

---

## 9. Sign-off

| Role | Name | Verdict | Date |
|------|------|---------|------|
| Board 7 — Operations / SRE (R10) | Shivanshi | ⬜ pending | — |

Criterion 4.5 remains **OPEN** until this row carries a verdict.
Verdict format: [templates/REVIEW-VERDICT.md](../../../governance/templates/REVIEW-VERDICT.md).
