# Operations Runbook — 1SB Insurance Integration

**Satisfies:** WS-1 Phase 4 exit criterion **4.5** ([04-STAGE_GATES.md §6](../../governance/04-STAGE_GATES.md#6-project-gates-l3)) ·
`ACTION-PLAN.md` 4.5
**Owner:** DevOps / SRE · **Reviewers:** Tech Lead (technical accuracy), Ops (procedure)
**Scope:** `1sb-integration-service` and its dependency `bank-persistence-service`, in **UAT**.

> **Scope boundary — read before using this in production.**
> This runbook covers UAT operation, which is what Phase 4 delivers. Production
> topology, prod credential issuance, and *verification* of the 1SB IP whitelist are
> **Phase 6.1**, parked in
> [PARKED-BACKLOG.md](../../governance/registers/PARKED-BACKLOG.md). Sections below
> marked **[Phase 6]** describe the procedure so it is written down, but the control
> is not yet in place and must not be assumed.

---

## 1. What you are operating

| Component | Port | Role | Public? |
|---|---|---|---|
| `1sb-integration-service` | 8080 | Bank-facing API; the only path to 1SB | Yes |
| `bank-persistence-service` | 8081 | Jobs, offers, payments, audit, raw payloads | **No** — internal only |
| PostgreSQL | 5432 | Datastore for persistence service | No |

**Standing constraint:** bank apps never call 1SB or the database directly
([CURRENT-STATE.yaml](../../governance/state/CURRENT-STATE.yaml) `standing_constraints`).
If a caller reports talking to 1SB directly, that is an architecture incident, not an ops one —
escalate to the Architect.

### Health endpoints

```bash
curl -fsS http://<host>:8080/actuator/health/liveness    # process alive
curl -fsS http://<host>:8080/actuator/health/readiness   # ready for traffic
curl -fsS http://<host>:8081/actuator/health/liveness    # persistence
```

Exposed actuator endpoints are `health,info,metrics,prometheus`
(`application.yml`). Under the `prod` profile health detail is `never` — you get
status only, no component breakdown. Under `uat` it is `when-authorized`.

---

## 2. Secrets

### 2.1 Inventory

| Secret | Consumed by | Source | Notes |
|---|---|---|---|
| `ONESB_API_KEY` | integration | secrets provider | Basic auth username to 1SB |
| `ONESB_API_SECRET` | integration | secrets provider | Basic auth password to 1SB |
| `ONESB_DISTRIBUTOR_ID` | integration | secrets provider | 1SB tenant/distributor identity |
| `RAW_PAYLOAD_ENCRYPTION_KEY` | persistence | env | **base64, decodes to exactly 32 bytes** (AES-256, COMP-003) |
| `DATASOURCE_PASSWORD` | persistence | env | Postgres credential |

### 2.2 Which provider is active

Controlled by `insurance.secrets.source` (`INSURANCE_SECRETS_SOURCE`):

| Value | Provider | Used in |
|---|---|---|
| `PROPERTIES` | `PropertiesSecretProvider` — reads `onesb.api-key` etc. | local, test **only** |
| `ENV` | `EnvSecretProvider` — reads the OS env vars above | **UAT (default)** |
| `AWS_SECRETS_MANAGER` | `AwsSecretsManagerSecretProvider` | **[Phase 6] — STUB, see below** |

> ⚠️ **`AWS_SECRETS_MANAGER` is not implemented.** `AwsSecretsManagerSecretProvider`
> throws `UnsupportedOperationException` on every accessor. The `prod` profile
> (`application-prod.yml`) hard-sets `insurance.secrets.source: AWS_SECRETS_MANAGER`,
> so **starting with `SPRING_PROFILES_ACTIVE=prod` will fail at startup, by design**
> (TD-006 / RISK-005). This is a deliberate fail-fast, not a defect. Do not "fix" it by
> switching prod to `ENV` — raise it through change control.

### 2.3 Startup behaviour — fail fast

`SecretsStartupValidator` (`@Order(1)`, runs as an `ApplicationRunner`) resolves all three
1SB credentials at boot. On any missing or blank value the service **aborts startup**:

```
STARTUP FAILED: Missing required credential 'api-key': ...
Service startup aborted: required 1SB credential 'api-key' is unavailable.
```

Profiles `test` and `integration-test` skip validation. Everything else does not.

**Operational consequence:** a rotation that lands a blank or wrong-name variable does not
degrade quietly — the next restarted instance refuses to come up. Roll forward or roll back;
do not restart repeatedly hoping it settles.

### 2.4 Rotating 1SB credentials (UAT)

1SB credentials are **not hot-reloaded**. `OneSbClientConfig` resolves them when the
`RestClient` bean is built, so a rotation requires a restart.

**Prerequisite:** the new key/secret pair is already active on the 1SB side. Ask 1SB support
whether the old pair remains valid during an overlap window — if there is no overlap, this is a
brief outage, so schedule it.

```bash
# 1. Verify the NEW credentials before touching the running service.
#    Any authenticated GET works; /v1/probe is the lightest.
curl -i -u "<NEW_KEY>:<NEW_SECRET>" \
  https://demo.api.1silverbullet.tech/v1/probe
#    Expect 200. A 401 here means the new pair is not live — STOP.

# 2. Update the secret store (compose/.env, Render dashboard, or your secret manager).
#    Never commit the real .env — .env.example is the template.

# 3. Restart the integration service only. Persistence is unaffected.
docker compose up -d --force-recreate 1sb-integration-service

# 4. Confirm the service came up — remember startup validation fails hard on a bad secret.
curl -fsS http://localhost:8080/actuator/health/readiness

# 5. Confirm a real authenticated call to 1SB now succeeds (see §5.1 verification).
```

**Rollback:** restore the previous values and repeat step 3. Because startup validates, a
successful boot is itself evidence the restored pair resolves.

**Post-rotation check:** watch for `UPSTREAM_AUTH_FAILURE` in the logs for 15 minutes
(§4.1). Zero occurrences = rotation clean.

### 2.5 Rotating `RAW_PAYLOAD_ENCRYPTION_KEY`

> 🛑 **Do not rotate this key casually.** Raw payloads in `raw_payload` are encrypted with the
> key that was active when they were written. There is **no key-versioning or re-encryption
> job** in the current implementation. Rotating the key makes every previously stored payload
> undecryptable.

Rotating it is therefore a **change-control item, not an ops action**
([14-CHANGE_CONTROL.md](../../governance/14-CHANGE_CONTROL.md)) and needs a migration plan
(re-encrypt or accept loss) plus Compliance sign-off, since raw payloads are retained for
COMP-003. Generating a key for a *new* environment is fine:

```bash
openssl rand -base64 32   # 32 bytes → base64; this is the expected format
```

---

## 3. IP whitelist / egress

**1SB enforces IP whitelisting on its side.** The service must reach 1SB from a stable,
pre-registered egress IP
([architecture §7.6](../architecture/1sb-integration-service-architecture.md)).

| Environment | Egress control | Status |
|---|---|---|
| Local / compose | Developer's public IP | Ad hoc — register per developer with 1SB |
| UAT | Fixed egress IP of the UAT platform | Must be registered with 1SB before UAT traffic |
| Production | NAT gateway with fixed EIP | **[Phase 6]** — infra owns; verification is criterion 6.1 |

### 3.1 Diagnosing a suspected whitelist block

A whitelist rejection does **not** look like a 401. It typically presents as a connection
timeout or a TLS/HTTP failure before authentication, surfacing as `UPSTREAM_UNAVAILABLE`.

```bash
# What egress IP does this host actually present?
curl -fsS https://api.ipify.org; echo

# Can we reach 1SB at all (TLS handshake + any response)?
curl -sS -o /dev/null -w '%{http_code} %{time_total}s\n' \
  https://demo.api.1silverbullet.tech/v1/probe

# Compare: a 401 means we ARE reaching 1SB (auth problem — go to §4.1).
# A hang/timeout with no status means we may not be — suspect whitelist or network.
```

**If the egress IP has changed** (new NAT, new node pool, scaled to a new subnet): send the
new IP to the 1SB integration contact and request whitelisting. Until it is registered, calls
fail. There is no application-side workaround, and adding a proxy to route around it is an
architecture change, not an incident fix.

---

## 4. Incident: 1SB returns 401

### 4.1 Signature

| Where | What you see |
|---|---|
| Caller-facing response | HTTP **502**, `"code": "UPSTREAM_AUTH_FAILURE"` |
| Service log | `ServiceException` from `OneSbErrorNormaliser.normalise(401, …)` |
| Audit log | `action=ONESB_OUTBOUND_CALL outcome=FAILURE`, `metadata.upstreamHttpStatus=401` |

```bash
# Count 401s in the last hour of container logs
docker compose logs --since 1h 1sb-integration-service \
  | grep -c 'upstreamHttpStatus=401'
```

**By design there is no retry on 401.** `OneSbHttpClient` maps 401 to a terminal
`ServiceException` — repeated auth failures against 1SB risk credential lockout, so the
service fails fast instead. Do not add a retry as a mitigation.

### 4.2 Triage — is it all traffic or some?

| Observation | Likely cause | Go to |
|---|---|---|
| **Every** call 401s, started abruptly | Credential rotated/revoked on 1SB side, or bad rotation | §4.3 |
| Every call 401s **right after a deploy** | Wrong/blank secret for the environment | §4.3 step 2 |
| **Some** calls 401 | Distributor scoping, or a second instance with stale config | §4.4 |
| 401s only on one endpoint family | Entitlement not granted for that product/LOB | Escalate to 1SB |

### 4.3 Resolution — credentials

```bash
# 1. Which provider is this instance actually using?
docker compose exec 1sb-integration-service env | grep -E 'INSURANCE_SECRETS_SOURCE|SPRING_PROFILES_ACTIVE'
#    Expect INSURANCE_SECRETS_SOURCE=ENV and SPRING_PROFILES_ACTIVE=uat in UAT.

# 2. Are the variables present and non-blank? (Confirm presence, never echo the value.)
docker compose exec 1sb-integration-service sh -c \
  'for v in ONESB_API_KEY ONESB_API_SECRET ONESB_DISTRIBUTOR_ID; do
     eval "val=\$$v"; [ -n "$val" ] && echo "$v: set (${#val} chars)" || echo "$v: MISSING"; done'

# 3. Test the credentials directly against 1SB, outside the service.
curl -i -u "<KEY>:<SECRET>" https://demo.api.1silverbullet.tech/v1/probe
```

- **Step 3 returns 401** → the credentials themselves are dead. Contact 1SB for a reissue,
  then follow §2.4. This is an upstream incident; record it and notify consumers.
- **Step 3 returns 200 but the service still 401s** → the service is not using the values you
  just tested. Suspect a stale container, a second instance, or `INSURANCE_SECRETS_SOURCE`
  pointing at the wrong provider. Force-recreate (§2.4 step 3).

### 4.4 Partial 401s

Check `ONESB_DISTRIBUTOR_ID` matches the distributor the affected requests are scoped to, and
confirm every running instance has identical configuration — during a rolling restart, old and
new instances can hold different secrets, producing exactly this pattern.

### 4.5 Consumer communication

`UPSTREAM_AUTH_FAILURE` is **not retryable by the caller** (`retryable: false`). Tell bank
consumers to stop retrying and hold, rather than queue traffic that cannot succeed.

---

## 5. Incident: 1SB returns 5xx or times out

### 5.1 Signature

| Where | What you see |
|---|---|
| Caller-facing response | HTTP **502**, `"code": "UPSTREAM_UNAVAILABLE"` |
| Service log | `1SB returned 5xx`, or `1SB call failed: <METHOD> <path>` on transport failure |
| Audit log | `outcome=FAILURE`, `upstreamHttpStatus=503` (transport failures infer 503) |
| Jobs | Poll attempts recorded with `httpStatus=0` and an `errorMessage` |

```bash
# Is 1SB itself up, independent of our service?
curl -sS -o /dev/null -w 'status=%{http_code} total=%{time_total}s\n' \
  https://demo.api.1silverbullet.tech/v1/probe

# Our own error rate, last hour
docker compose logs --since 1h 1sb-integration-service | grep -c 'UPSTREAM_UNAVAILABLE'
```

### 5.2 What the service already does for you

- **Timeouts:** connect 3 s, read 30 s (`ONESB_CONNECT_TIMEOUT_MS` / `ONESB_READ_TIMEOUT_MS`).
- **Async polling:** quote/proposal polling runs on `pollingExecutor`, never a request thread.
  Backoff is exponential — base 1 s, ×2, capped at 30 s, **20 attempts** — so a single job
  tolerates roughly 10 minutes of upstream flakiness before giving up.
- **On exhaustion:** the job is failed with reason `POLL_TIMEOUT` and status `TIMEOUT`. The
  synchronous caller already holds a `jobId` and is expected to poll `GET /v1/quotes/{jobId}`,
  so a slow upstream degrades job completion rather than breaking the API.
- **There is no circuit breaker.** It is deliberately out of scope until Phase 5.5
  (CURRENT-STATE `out_of_scope`). Under a sustained 1SB outage this service keeps trying;
  expect elevated latency and thread usage on `pollingExecutor`, not automatic shedding.

### 5.3 Response by severity

| Situation | Action |
|---|---|
| Brief 5xx blip, error rate returning to normal | Monitor only. Backoff absorbs it. |
| Sustained 5xx > 15 min | Notify consumers: quotes will land in `TIMEOUT`. Open an incident with 1SB, quoting `traceId` and `reqId` from audit lines. |
| Timeouts with 1SB reachable from elsewhere | Suspect egress/network on our side — §3.1. |
| Read timeouts only on quote submit under load | Check `pollingExecutor` saturation; correlate with the 4.6 performance smoke baseline. |

**Do not** raise `ONESB_POLL_MAX_ATTEMPTS` during an incident to "give it more time" — it
multiplies in-flight polling work against an already-failing upstream and is a config change
outside change control. Let jobs land in `TIMEOUT`; they are recorded and re-runnable.

### 5.4 Recovering timed-out jobs

Jobs that failed with `POLL_TIMEOUT` are terminal in this phase — there is no automatic
re-drive. Consumers resubmit with a **new** `Idempotency-Key`. Reusing the old key returns the
original timed-out job, which is the intended idempotency behaviour, not a bug.

---

## 6. Escalation

| Symptom | First owner | Escalate to |
|---|---|---|
| 401 with credentials verified dead | Ops | 1SB integration contact (credential reissue) |
| Sustained 1SB 5xx | Ops | 1SB support, with `traceId` + `reqId` |
| Suspected IP whitelist block | Ops | Infra (egress IP) → 1SB (registration) |
| Service will not start, secret-related | Ops | Tech Lead |
| Prod profile fails on `AWS_SECRETS_MANAGER` | Ops | Tech Lead — this is TD-006, expected |
| Encryption-key rotation request | Ops | Architect + Compliance — change control, §2.5 |
| Consumer reports calling 1SB directly | Ops | **Architect** — standing-constraint breach |

**Contacts:** 1SB integration contact, on-call rota, and consumer distribution list are
environment-specific and are **not recorded in this repository**. They belong in the ops
directory alongside the deployment. Fill them in there before UAT go-live.

---

## 6.1 Audit sinks

Audit events go to the sinks named by `insurance.audit.sinks` (`INSURANCE_AUDIT_SINKS`).
Default: `LOG,PERSISTENCE`.

| Value | Effect |
|---|---|
| `LOG` | Structured line to the application log. Operator-facing; **not** durable evidence |
| `PERSISTENCE` | Appended to `audit_event` via `POST /internal/v1/audit-events`. The evidentiary trail |
| `KAFKA` | Declared but **not implemented** — selecting it **fails startup** by design, rather than accepting events and discarding them |

**Capture is best-effort.** If the persistence service is unavailable, the failure is logged and
the customer's transaction proceeds — evidence is lost silently. Watch for
`Failed to persist audit event` in the logs; a sustained run of those means the audit trail has
a hole, and Compliance has asked to be told about gaps (RISK-012).

```bash
docker compose logs --since 1h 1sb-integration-service | grep -c 'Failed to persist audit event'
```

**Do not disable the PERSISTENCE sink to quiet an incident.** That converts a noisy problem into
a silent compliance one. If persistence is down, fix persistence.

---

## 7. Related

| Document | Why |
|---|---|
| [COVERAGE.md](./COVERAGE.md) | Coverage gates (QA-001, criterion 4.7) |
| [PERFORMANCE-SMOKE.md](./PERFORMANCE-SMOKE.md) | p95 quote baseline (criterion 4.6) |
| [compliance/COMPLIANCE-REVIEW-PACK.md](./compliance/COMPLIANCE-REVIEW-PACK.md) | Audit schema + log samples (criterion 4.4) |
| [compliance/REGULATORY-RETENTION-FINDINGS.md](./compliance/REGULATORY-RETENTION-FINDINGS.md) | IRDAI / RBI / PMLA retention positions |
| [UAT-ENABLEMENT.md](./UAT-ENABLEMENT.md) | Bank consumer onboarding (criterion 4.3) |
| [architecture §7.6](../architecture/1sb-integration-service-architecture.md) | Security and egress design |
| [TECH-DEBT.md](./TECH-DEBT.md) | TD-006 (AWS SM stub), TD-010 (single-instance idempotency) |
