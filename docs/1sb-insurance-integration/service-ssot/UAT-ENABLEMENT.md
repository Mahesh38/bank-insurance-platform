# UAT enablement — bank consumer onboarding

**For:** WS-1 Phase 4 exit criterion **4.3** — "At least one bank caller exercises quote +
proposal against UAT"
([04-STAGE_GATES.md §6](../../governance/04-STAGE_GATES.md#6-project-gates-l3))
**Owner:** Engineering (this pack) · **Blocked on:** a bank app team (DEP-002)
**Status:** ⏳ **Ready for a consumer — no consumer assigned**

> **Criterion 4.3 cannot be closed by Engineering.** It requires a **bank caller** — a person on
> another team — to exercise quote and proposal against UAT and confirm it. No amount of
> preparation substitutes for that, and this document does not claim otherwise. What it does is
> remove every reason for the exercise to be slow once someone is named.
>
> The named owner and date are still missing: **DEP-002** is flagged `⚠️ unassigned / unset` in
> the [dependency register](../../governance/registers/DEPENDENCY-REGISTER.md), and **RISK-002**
> escalates when the gate reaches CANDIDATE with 4.3 still open. Naming that person is a PO
> action ([ASM-004](../../governance/registers/ASSUMPTION-REGISTER.md)).

---

## 1. What the consumer is being asked to do

Exercise the Term path end to end against UAT and confirm it worked:

1. Get a quote for a Term policy.
2. Retrieve the offers.
3. Fetch the proposal form and submit a proposal.
4. Confirm an application number came back.

Payment and status are **welcome but not required** by the criterion. If the consumer is willing
to go further, §6 says what to watch for.

**Time required:** roughly half a day for a developer who has the collection, including reading
this document. It is not a project.

---

## 2. What Engineering has already provided

| Need | Where | State |
|---|---|---|
| API contract | [OpenAPI document](../api-catalog/openapi/1sb-integration-service.json) | ✅ Verified against the running code on every build |
| Runnable request collection | [Postman collection](../api-catalog/collections/1sb-integration-term-journey.postman_collection.json) | ✅ Chains the whole journey |
| Integration guide | [PUBLISHED-API.md](../api-catalog/PUBLISHED-API.md) | ✅ Includes the mistakes integrators usually make |
| Interactive browsing | `/swagger-ui.html` on the UAT host | ✅ |
| What to do when it breaks | [OPERATIONS-RUNBOOK.md](./OPERATIONS-RUNBOOK.md) | ✅ |
| Sign-off template | §7 below | ✅ |
| **A named bank app team and a slot** | — | ❌ **DEP-002 — PO action** |
| **A reachable UAT deployment** | — | ⚠️ See §3 |

---

## 3. Environment prerequisites

These must be true before the consumer starts. Confirm them rather than assuming — a consumer
who hits a misconfigured environment on day one is expensive to re-recruit.

| # | Prerequisite | Owner | How to verify |
|---|---|---|---|
| 1 | `1sb-integration-service` deployed and reachable on the UAT host | Ops | `curl -fsS https://<uat-host>/actuator/health/readiness` |
| 2 | `bank-persistence-service` reachable **from the integration service** (not publicly) | Ops | Integration health is `UP`; a quote reaches `PENDING` rather than 502 |
| 3 | 1SB **sandbox** credentials present as `ONESB_API_KEY` / `ONESB_API_SECRET` / `ONESB_DISTRIBUTOR_ID` | Ops | The service starts at all — `SecretsStartupValidator` aborts startup on a missing credential |
| 4 | The UAT egress IP is registered with 1SB | Ops + 1SB | [Runbook §3.1](./OPERATIONS-RUNBOOK.md) |
| 5 | `RAW_PAYLOAD_ENCRYPTION_KEY` set (base64, 32 bytes) | Ops | The persistence service starts at all |
| 6 | The consumer knows the UAT base URL | Eng | Sent with the collection |

> **A consumer-facing note on prerequisite 3:** UAT points at the 1SB **sandbox**, not
> production. Quotes and proposals created here are not real policies and cost nobody anything.
> Test freely.

---

## 4. Getting started, for the bank app team

1. Import the [Postman collection](../api-catalog/collections/1sb-integration-term-journey.postman_collection.json).
2. Create an environment with `baseUrl` = the UAT host, no trailing slash.
3. Set `actorId` to **your real employee identifier**. It is recorded in the audit trail; leaving
   the placeholder makes the trail useless for the compliance review that runs alongside this.
4. Run requests 1–6 in order. Each captures what the next one needs.
5. Record what you observe in the §7 template and send it back.

Read [PUBLISHED-API.md §3](../api-catalog/PUBLISHED-API.md) first — it lists the failure modes
integrators hit most often, and reading it takes less time than debugging one of them.

### The two things most likely to trip you up

**Quoting is asynchronous.** `POST /v1/quotes` returns `202` and a `jobId`, not offers. Poll
`GET /v1/quotes/{jobId}` until the status is terminal. An empty `offers` array while the status
is `PENDING` or `RUNNING` is correct behaviour, not an error.

**Every mutating request needs `Idempotency-Key`.** Omitting it returns `400`. Sending the same
key twice returns the *original* job rather than creating a second one — that is deliberate, and
it is how you should handle a client-side retry.

---

## 5. Correlating a problem back to us

When something behaves unexpectedly, this is what lets us find it without guesswork. Capture all
four:

| Capture | Why |
|---|---|
| `jobId` or `proposalJobId` | Primary key for everything downstream |
| `journeyId` you sent | Correlates every step of one customer journey in the audit trail |
| `Idempotency-Key` you sent | Distinguishes a genuine retry from a new request |
| Timestamp with timezone | Narrows the log search |

The service records an audit event for every outbound 1SB call, carrying `latencyMs`,
`upstreamHttpStatus` and a hash of the masked request body — so we can confirm exactly what was
sent without either of us handling customer data.

**No PII in problem reports, please.** Send the identifiers above, not the customer's name, PAN,
mobile or date of birth. The service is built so those never reach a log; a support email should
not undo that.

### Error codes you may see, and whose problem they are

| Code | HTTP | Whose problem | Action |
|---|---|---|---|
| `VALIDATION_ERROR` | 422 | Yours | Fix the request body |
| `MISSING_IDEMPOTENCY_KEY` | 400 | Yours | Add the header |
| `IDEMPOTENCY_CONFLICT` | 409 | Yours | Same key, different body — use a new key |
| `PROPOSAL_REJECTED` | 422 | The insurer's | Business rejection, not retryable; field errors are included |
| `PROPOSAL_NOT_PAYABLE` | 409 | Sequencing | The proposal is not in a payable state yet |
| `UPSTREAM_BUSINESS_ERROR` | 422 | The insurer's | Their rules rejected it; detail is passed through |
| `UPSTREAM_UNAVAILABLE` | 502 | **Ours / 1SB's** | Retryable. Tell us if it persists |
| `UPSTREAM_AUTH_FAILURE` | 502 | **Ours** | Our credentials to 1SB failed. **Not retryable by you** — stop and tell us ([runbook §4](./OPERATIONS-RUNBOOK.md)) |
| `QUOTE_TIMEOUT` / job `TIMEOUT` | — | 1SB was slow | Resubmit with a **new** Idempotency-Key |

---

## 6. Optional, and genuinely useful if you have time

Not required by criterion 4.3, but each answers a question we cannot answer for ourselves:

- **Payment and status** (requests 7–8) — exercises the rest of the journey.
- **End-to-end timing.** How long from quote submit to offers on screen? Our
  [performance smoke](./PERFORMANCE-SMOKE.md) measures our own overhead with 1SB stubbed; only a
  real UAT run shows what a customer would actually wait. If it feels slow, say so — that
  observation is the missing input to the p95 target (ASM-009).
- **Field-level friction.** Anything in the proposal schema that is awkward to render or
  ambiguous to fill.
- **Error message quality.** When you got an error, could you tell what to do next? If not, that
  is a defect in our error normalisation, and we would rather hear it now than from a branch.

---

## 7. Confirmation template

Criterion 4.3 is met when a bank caller completes and returns this. Engineering cannot fill it
in on the consumer's behalf.

```text
consumer team:      __________________________
tester:             __________________________
date:               __________________________
environment:        UAT — base URL ____________________

REQUIRED
  quote submitted           YES / NO      jobId: ______________________
  offers retrieved          YES / NO      offer count: _________________
  proposal schema fetched   YES / NO
  proposal submitted        YES / NO      applicationNumber: ___________

OPTIONAL
  payment session created   YES / NO / NOT ATTEMPTED
  status retrieved          YES / NO / NOT ATTEMPTED

OBSERVATIONS
  quote submit -> offers visible took approximately: __________ seconds
  anything confusing, awkward, or wrong:
  ________________________________________________________________
  ________________________________________________________________

  defects raised (ids):    ________________________________________

VERDICT
  Term path is usable for UAT:   YES / YES WITH DEFECTS / NO
  signature: __________________________
```

Send it to Engineering; it is filed in `service-ssot/` as the gate evidence for 4.3.

---

## 8. What blocks this criterion

| Blocker | Type | State | Owner |
|---|---|---|---|
| DEP-001 — OpenAPI published | HARD | ⚠️ **Partial** — document published and verified in-repo; internal portal publication outstanding ([PUBLISHED-API §4](../api-catalog/PUBLISHED-API.md)) | Eng + Platform |
| DEP-002 — bank app team UAT slot | EXTERNAL | ❌ **Unassigned, no date** | **PO** |
| UAT environment prerequisites (§3) | — | ❓ Unverified in this repository | Ops |

**The critical path runs through DEP-002, and it is a people problem, not an engineering one.**
Everything Engineering can do without a consumer is done. Per
[07 §DEP-3](../../governance/07-DEPENDENCY_MODEL.md), the chase for an external dependency is
itself a work item with an owner and a date — that item does not exist yet, and RISK-002 exists
precisely because of it.

---

## 9. Related

| Document | Why |
|---|---|
| [PUBLISHED-API.md](../api-catalog/PUBLISHED-API.md) | The integration guide to read first |
| [OPERATIONS-RUNBOOK.md](./OPERATIONS-RUNBOOK.md) | What we do when the consumer reports a failure |
| [PERFORMANCE-SMOKE.md](./PERFORMANCE-SMOKE.md) | Why UAT timing observations matter (ASM-009) |
| [COMPLIANCE-REVIEW-PACK.md](./compliance/COMPLIANCE-REVIEW-PACK.md) | Why `actorId` must be a real identifier |
