# 07 — Platform Error Contract

**One failure, one incident id, two renderings: a safe one for the end user and a complete one for
the engineer. Every error says which service produced it, at which layer, and what to do next.**

Status: `AI-DRAFTED` · Owner: Mahesh (Board 1) + Amit (Board 2) · Origin: `SUG-20260827-err` ·
Decision: `ADR-017`

This file is the **implementation contract** for the refusals catalogued in
[`04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md`](./04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md), keyed to
the layer ladder in [`01-REQUEST-LIFECYCLE-STANDARD.md`](./01-REQUEST-LIFECYCLE-STANDARD.md#1-the-eight-layers)
and the rule codes in [`03-VALIDATION-RULE-CATALOGUE.md`](./03-VALIDATION-RULE-CATALOGUE.md).
Catalogue 04 says *what* the platform refuses. This file says *how every service emits that refusal*
so that the answer is identical everywhere.

> **Scope discipline.** This contract hardens the existing `libs/bank-common-error` and
> `libs/bank-common-observability` modules, which already have three consumers. It is not a new
> framework, it introduces no new runtime dependency, and it changes no existing `code` value —
> `ErrorCodes` is documented as consumed by partners, so every change here is **additive**.

---

## 1. The problem this closes

Measured on the current tree, not asserted:

| # | Defect today | Evidence |
|---|---|---|
| D1 | **Upstream vendor text is returned to bank callers verbatim** | `OneSbErrorNormaliser` sets `.detail(parsed.detail())`; `GlobalExceptionHandler` serialises the body as-is |
| D2 | **Internal routes and vendor identity leak** | `.detail("1SB call failed: " + method + " " + path)`, `.detail("Unexpected 1SB status " + httpStatus)` |
| D3 | **No error says which service produced it** | `ServiceErrorResponse` has no service, origin or layer field. A 502 seen at the BFF is unattributable |
| D4 | **The same condition is worded differently at each throw site** | `"Proposal job not found: " + jobId` · `"Quote job not found: " + jobId` · `"No status found"` — three phrasings, three shapes, one condition |
| D5 | **The catalogue is not implemented** | Catalogue 04 defines ~60 codes across seven classes. `ErrorCodes` defines 24, and the two sets barely intersect — `SESSION_INVALID`, `DEFAULT_DENY`, `SUITABILITY_REQUIRED`, `PAYMENT_DEVICE_ISOLATION` exist only on paper |
| D6 | **Three exception handlers, three contracts** | 1SB returns problem+json with the platform envelope; persistence returns the same envelope with different statuses for the same class; the BFF returns a bare Spring `ProblemDetail` with no `code` at all |
| D7 | **Nothing is countable** | `MetricNames.ERROR_COUNT` exists but is `bank.onesb.*`-prefixed and service-local. There is no platform-wide, consistently tagged error counter to build a dashboard on |

D1 and D2 are the reason this is not a tidy-up. Everything else is the reason debugging costs what
it costs.

---

## 2. The four coordinates of every error

Every failure the platform produces answers these four, always, in this order:

| Coordinate | Field | Example | Question it answers |
|---|---|---|---|
| **What** | `code` | `SUITABILITY_REQUIRED` | which rule refused |
| **Who** | `service` | `journey-orchestration` | which service emitted this response |
| **Where** | `layer` | `L6` | which rung of the ladder ([`01 §1`](./01-REQUEST-LIFECYCLE-STANDARD.md#1-the-eight-layers)) |
| **Class** | `category` | `COMPLIANCE_GATE` | how the caller must treat it |

and, when the failure did not originate in the responding service, a fifth:

| **Origin** | `origin` | `{service: journey-orchestration, code: SUITABILITY_REQUIRED, layer: L6}` | where it *first* failed |

`origin` is what makes the RM's screen debuggable. In the journey the requirement describes —
BFF → customer consent → orchestrator, and the orchestrator's validation refuses — the BFF's
response and every log line along the path carry `origin.service = journey-orchestration`,
`origin.code`, `origin.layer = L6` and **one shared `incidentId`**.

### 2.1 Categories

Fixed, closed enum. A category, not a status, decides how a caller reacts.

| Category | Typical HTTP | Retryable | Log level | Audit | Meaning |
|---|---|---|---|---|---|
| `VALIDATION` | 400 / 422 | after-fix | WARN | no | The request is wrong. The caller can fix it |
| `AUTHENTICATION` | 401 | after re-auth | WARN | yes | Who you are is not established |
| `AUTHORIZATION` | 403 | no | WARN | yes | Who you are is established and not permitted |
| `NOT_FOUND` | 404 / 422 | no | WARN | no | The referenced thing does not exist for this caller |
| `CONFLICT` | 409 | sometimes | WARN | varies | State disagrees — idempotency, transition, expiry |
| `COMPLIANCE_GATE` | 403 / 409 | after the RM acts | WARN | **compliance event** | A regulator-mandated refusal. The refusal is itself evidence |
| `UPSTREAM` | 502 / 503 / 504 | usually | ERROR | no | A dependency failed. Not our defect, still our incident |
| `CONFIG` | 422 / 503 | no | ERROR | no | The platform cannot resolve its own configuration. Fail closed |
| `RATE_LIMIT` | 429 | yes, on `Retry-After` | WARN | no | Throttled |
| `INTERNAL` | 500 | no | ERROR | no | Our defect. Always a bug, always alertable |

> **Why the log level is in this table.** Today every failure is a candidate for `ERROR`. A
> validation refusal is a normal, expected outcome of a public API; logging it at `ERROR` with a
> stack trace is what makes an error dashboard unreadable within a week. Client-caused categories
> log at `WARN` **without** a stack; platform-caused categories log at `ERROR` **with** one.

### 2.2 Service identity

One short, stable id per deployable, set once from configuration and never typed at a throw site:

| Service id | Module |
|---|---|
| `bff` | `services/workforce-access-bff` |
| `onesb` | `services/1sb-integration-service` |
| `persistence` | `services/bank-persistence-service` |
| `idp-adapter` | `services/identity-provider-adapter-service` |
| `authz` | `services/identity-authorization-service` |

New services register their id here in the same change that creates the module.

---

## 3. The registry — one place where a code's behaviour is decided

The root cause of D4 and D6 is that a code's HTTP status, retryability, wording and audit
behaviour are re-decided at every throw site. They are declared **once**:

```text
ErrorCatalogue.entry(SUITABILITY_REQUIRED)
  category        COMPLIANCE_GATE
  httpStatus      403
  retryable       AFTER_REMEDIATION
  publicTitle     "Suitability assessment required"
  publicDetail    "This quote needs a completed suitability assessment before it can be produced."
  audit           COMPLIANCE_EVENT
  alert           on_rate
  runbook         RB-SUITABILITY_REQUIRED
  catalogueRef    04 §5
```

A throw site supplies only what the registry cannot know: the identifiers, the developer reason,
and the cause. It never re-states the status, the wording or the retryability.

**This is the mechanism behind "we should not randomly state same error".** Two engineers throwing
`QUOTE_EXPIRED` in two services cannot produce two different responses, because neither of them
writes the response.

The registry is seeded from catalogue 04 and is the single artefact CI diffs against it: a code in
the catalogue with no registry entry fails the build, and vice versa. That is how the catalogue
stops being paper.

---

## 4. Two renderings of one incident

### 4.1 `incidentId`

Generated at the **point of first failure**, propagated unchanged across every hop, printed in every
log line, and shown to the end user. It is the only token an RM needs to read out and the only one
L1 needs to paste into the log platform.

`correlationId` groups a request. `incidentId` identifies *this failure*. A request that fails twice
has one correlation id and two incident ids.

### 4.2 Public rendering — crosses the trust boundary

RFC 7807 `application/problem+json`, extended. This is what L0 receives:

```json
{
  "type": "https://errors.<platform>/SUITABILITY_REQUIRED",
  "title": "Suitability assessment required",
  "status": 403,
  "detail": "This quote needs a completed suitability assessment before it can be produced.",
  "code": "SUITABILITY_REQUIRED",
  "category": "COMPLIANCE_GATE",
  "retryable": false,
  "incidentId": "01JQ8F3K2M7Z9V4T",
  "correlationId": "b7c1e0f2-...",
  "timestamp": "2026-08-27T09:41:22Z",
  "errors": [ { "code": "...", "field": "...", "message": "..." } ]
}
```

**Every field here is safe by construction.** `title` and `detail` come from the registry, not from
a throw site and never from an upstream body. There is no `diagnostic`, no `origin`, no upstream
text, no path, no vendor name, no stack.

### 4.3 Diagnostic rendering — never crosses the trust boundary

Emitted to logs at the point of failure, and carried in the body on **internal** hops only:

```json
{
  "incidentId": "01JQ8F3K2M7Z9V4T",
  "code": "SUITABILITY_REQUIRED",
  "category": "COMPLIANCE_GATE",
  "service": "onesb",
  "layer": "L6",
  "component": "QuoteService",
  "operation": "createQuote",
  "origin":   { "service": "onesb", "code": "SUITABILITY_REQUIRED", "layer": "L6" },
  "reason":   "assessmentId a3f2 expired at 2026-08-27T08:10Z, quote requested 09:41Z",
  "upstream": { "system": "1SB", "code": "UW_DECLINE_17", "httpStatus": 422, "reference": "1SB-7741" },
  "causeChain": ["SuitabilityExpiredException: assessment a3f2 expired"],
  "remediation": "RM must refresh the suitability assessment, then re-quote. Do not override.",
  "runbook": "RB-SUITABILITY_REQUIRED",
  "correlationId": "b7c1e0f2-...",
  "journeyId": "...", "jobId": "...", "actorRef": "<pseudonymous>"
}
```

### 4.4 The redaction boundary — stated once, enforced once

> **The BFF (L4) is the redaction boundary.** It is the last hop that may *hold* a diagnostic and
> the first that must never *emit* one. Everything below L4 exchanges full diagnostics; nothing
> above L4 receives one.

Two consequences, both testable:

1. No response leaving L4 toward L0 contains `diagnostic`, `origin`, `upstream` or `causeChain`.
2. `detail` is **public text**. Anything an engineer needs that is not safe for an end user goes in
   `reason`, never in `detail`. That single rule closes D1 and D2.

---

## 5. Propagation — how service B's failure reaches service A's caller

When A calls B and B refuses, A must do exactly this:

```text
1  Parse B's problem+json.  Never pass its body through.  Never log its body as A's own message.
2  incidentId  := B.incidentId          # preserved — one incident across the whole chain
3  origin      := B.origin ?: {B.service, B.code, B.layer}     # first origin wins, transitively
4  Map B's category to A's own code, from the registry:
      B UPSTREAM/INTERNAL/CONFIG  -> A emits UPSTREAM_UNAVAILABLE | UPSTREAM_BAD_RESPONSE  (5xx)
      B VALIDATION/CONFLICT/...   -> A propagates the SAME code when A's caller can act on it,
                                     otherwise UPSTREAM_BUSINESS_ERROR with origin preserved
5  A's public rendering carries A's own safe wording; the origin travels in A's diagnostic.
```

Rule 4 is the one that gets built wrong: a compliance refusal from the orchestrator must reach the
RM **as that refusal**, not as a generic 502, because the RM can act on it. A connection failure to
the orchestrator must not reach the RM as a validation error, because they cannot. The registry
declares which codes are `propagate` and which are `wrap`.

**Never** re-wrap an error as `INTERNAL_ERROR` to make it go away. `INTERNAL` means *our defect*,
and treating a dependency failure as one destroys the only signal that says whose defect it is.

---

## 6. The log record — the six questions, answered by field

The requirement is that the logs answer *why, how, when, where, what* and *what to do*. Each maps to
a field that is mandatory, not optional:

| Question | Field(s) |
|---|---|
| **What** failed | `code`, `title`, `category` |
| **Where** it failed | `service`, `layer`, `component`, `operation` — and `origin.*` when it began elsewhere |
| **When** | `timestamp`, plus `correlationId` / `journeyId` / `jobId` for the surrounding request |
| **Why** | `reason` (developer text, the one free-text field), `upstream.code`, `upstream.httpStatus` |
| **How** | `causeChain` — exception classes and redacted messages, innermost last |
| **What to do** | `remediation` and `runbook` — from the registry, so L1 reads the same sentence every time |
| **Who / which request** | `incidentId`, `actorRef` (pseudonymous), never a raw actor identifier |

Emitted as one structured line, with `MdcKeys` already carrying `correlationId`, `journeyId`,
`jobId`, `traceId`. This contract adds `incidentId`, `errorCode` and `originService` to
[`MdcKeys`](../../libs/bank-common-observability/src/main/java/com/bank/common/observability/MdcKeys.java)
so that every line logged *during* a failed request — not only the error line — is filterable by
incident.

---

## 7. Monitorability

One counter, platform-wide, consistently tagged. This is what a dashboard is built from:

```text
bank.error.count { service, code, category, layer, originService, retryable, httpStatus }
```

Every tag is a bounded enum or a registered service id from §2.2 and §3. **No tag is ever a message,
an identifier, a path or a free-text value** — that is the cardinality guard, and it is the
difference between a dashboard and a metrics outage.

Answerable directly from this one series, without a new metric per question:

| Question | Query shape |
|---|---|
| Which errors are growing? | top-k by `code` over time |
| Which service is failing? | by `service` |
| Which service is *actually* at fault? | by `originService` — the panel that ends "is it us or them" |
| Are we refusing for compliance more than usual? | `category = COMPLIANCE_GATE` |
| Is the platform or the caller at fault? | `category` in (`INTERNAL`,`UPSTREAM`,`CONFIG`) vs the rest |
| Which failures should have retried? | `retryable = true` rate |

The existing `bank.onesb.*` names in `MetricNames` are unchanged and stay service-local; this is an
additional platform-level series, not a rename.

---

## 8. PII

`publicTitle`, `publicDetail`, `remediation` and `reason` are **templates with named placeholders**.
Only values on the non-PII allow-list may be substituted: `jobId`, `quoteId`, `proposalId`,
`applicationNumber`, `correlationId`, `incidentId`, `lob`, `insurerCode`, `field` names.

Never substituted, in any rendering, including diagnostics: PAN, Aadhaar, name, DOB, address, phone,
email, income, health attributes, account or card numbers, OTPs, tokens, full request bodies.

This is what makes gate **`S08-G7`** ("no PII in logs, proven by automated test") provable: the test
asserts over the registry — a finite, enumerable set of templates — instead of trying to prove a
negative over every log statement in the codebase.

---

## 9. Support-facing output

Each registry entry names a runbook page, `RB-<CODE>`, with a fixed four-part shape so L1 and L2
read the same structure every time:

```text
RB-SUITABILITY_REQUIRED
  What it means      A quote was refused because no valid suitability assessment exists.
  Is it a defect?    No. This is a compliance gate working correctly.
  L1 action          Ask the RM to complete/refresh the suitability assessment and re-quote.
  L2 escalation      Only if the assessment exists, is unexpired, and the refusal repeats:
                     capture the incidentId and raise to the journey-orchestration owner.
  Never              Never override, and never re-issue the quote without a fresh assessment.
```

`incidentId` is the join key: the RM reads it off the screen, L1 pastes it into the log platform and
gets every line of that failure across every service, and the `runbook` field on the record tells
them which page they are on before they have diagnosed anything.

---

## 10. What this contract does **not** change

Recorded so the increment cannot expand into them:

- **No existing `code` value changes.** `ErrorCodes` is documented as consumed by partners
  (trigger `G9`). Every code is added, none renamed or removed.
- **No new runtime dependency**, no new service, no new infrastructure.
- **Dashboards and alert rules themselves** are `L9` work. This contract makes them *possible* by
  emitting the series; it does not build them.
- **Retry, circuit breaking and bulkheads** are unchanged — resilience policy stays in
  [`04 §8`](./04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md#8-dependency-down-behaviour).
- **The degraded states in [`04 §7`](./04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md#7-degraded-states--the-ones-with-no-error-code)
  stay codeless.** They are journey states, not refusals, and giving them error codes would be the
  single easiest way to get this design wrong.

---

## 11. Delivery — `EPIC-001`

```yaml
epic:
  id: EPIC-001
  title: "Platform error contract"
  origin: SUG-20260827-err
  decision: ADR-017
  outcome: >
    Every service emits the same error envelope; every error names its emitting service, its
    layer and its origin; end users receive safe text while engineers and L1/L2 receive a
    complete diagnostic under one incident id; and platform error rate is queryable by code,
    category, service and origin service.
  completion_definition: >
    All five services return the §4.2 envelope AND no response crossing L4 carries a diagnostic
    field, proven by test AND the registry and catalogue 04 agree, proven by CI AND
    bank.error.count is emitted with the §7 tag set AND the S08-G7 PII test asserts over the
    registry.
  not_included:
    - "Any change to an existing ErrorCodes value (G9 — additive only)"
    - "Grafana dashboards and alert rules (L9)"
    - "Retry / circuit breaker / bulkhead policy"
    - "Error codes for the codeless degraded states in 04 §7"
    - "Runbook prose for codes outside the R0 slice"
  stories:
    - id: ERR-001
      title: "Registry, envelope and diagnostic in bank-common-error"
      outcome: "ErrorCatalogue + ErrorCategory + ErrorDiagnostic exist; ServiceErrorResponse gains service/category/incidentId/origin; existing factories keep working"
    - id: ERR-002
      title: "One shared exception handler; redaction at L4"
      outcome: "The three divergent handlers become one shared advice; no diagnostic field leaves L4"
    - id: ERR-003
      title: "Cross-service propagation preserves incidentId and origin"
      outcome: "An orchestrator refusal is attributable at the BFF, with one incident id along the chain"
    - id: ERR-004
      title: "Structured error logging and bank.error.count"
      outcome: "MdcKeys gains incidentId/errorCode/originService; the §7 counter is emitted once per failure"
    - id: ERR-005
      title: "Seed the registry from catalogue 04 for the R0 slice"
      outcome: "Every R0 code in catalogue 04 has a registry entry; CI diffs the two"
    - id: ERR-006
      title: "Evidence: PII test (S08-G7), ArchUnit leak rule, contract tests"
      outcome: "Automated proof that no upstream body and no PII reaches a client"
    - id: ERR-007
      title: "RB-* runbook pages for the R0 codes"
      outcome: "L1/L2 have a four-part page per code, joined to logs by incidentId"
  dependencies:
    - "04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md — the source of code semantics"
    - "S08-G7 (PII in logs) and S08-G8 (engineering standards) consume this"
  owner: "Amit — Technical Head (Board 2), with Mahesh (Board 1) on the contract"
```

### 11.1 Order, and why

`ERR-001` → `ERR-005` → `ERR-002` → `ERR-003` → `ERR-004` → `ERR-006` → `ERR-007`.

The registry lands before the handler, because a shared handler with per-site wording would rebuild
D4 inside one class. `ERR-006` lands before the runbooks, because a runbook for behaviour that is
not yet proven is fiction.

### 11.2 Review boards

Risk tier **T3** ([`11 §3`](../governance/11-REVIEW_GATES.md#3-proportionality--which-boards-are-mandatory)):
Architecture, Technical, Product, QA, Security, Risk & Compliance, Operations.

T4 was considered and does not fire, per Rule `RG-6`, recorded here rather than left implicit:

| Trigger | Considered | Why it does not fire |
|---|---|---|
| `G9` breaks a public contract | **yes — the closest call** | `ErrorCodes` is partner-consumed, so the contract is additive-only: no value renamed or removed, new fields only. If any story proposes changing an existing value, that story is T4 and stops here |
| `G2` widens who sees PII/restricted fields | yes | The change **narrows** exposure (§4.4, §8). Error-message changes that leave control logic unchanged are explicitly not T4 |
| `G10` changes an evidenceable control | yes | Compliance-gate refusals keep the audit behaviour catalogue 04 already specifies; this contract carries them, it does not redefine them |

---

## 12. Traceability

| This contract | Serves |
|---|---|
| §4.4, §8 | `S08-G7` — no PII in logs, proven by automated test |
| §3, §5, §6 | `S08-G8` — engineering and secure coding standards published and adopted |
| §6, §9 | `GATE-P4 4.4` compliance review of log samples · `4.5` incident runbook |
| §2, §3 | Implements [`04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md`](./04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md) |
| §2.1 layer column | Keys on [`01 §1`](./01-REQUEST-LIFECYCLE-STANDARD.md#1-the-eight-layers) |

---

## 13. Discrepancies found while seeding the registry (`ERR-005`)

Recorded, not silently reconciled. Each needs a decision by the catalogue's owner; the registry
currently follows the published constant, because `ErrorCodes` is partner-consumed and this
increment is additive-only.

| # | Catalogue 04 says | `ErrorCodes` says | Registry follows | Needs |
|---|---|---|---|---|
| 1 | `IDEMPOTENCY_KEY_CONFLICT` (§2) | `IDEMPOTENCY_CONFLICT` | `IDEMPOTENCY_CONFLICT` | Catalogue 04 §2 corrected to the published value. Minting a second code for one condition would recreate defect **D4**, so the code was not added |
| 2 | `OPPORTUNITY_REQUIRED` (§6) | *(absent)* | `OPPORTUNITY_REQUIRED`, public text reads "Lead required" | `ADR-014` renamed the context Opportunity → Lead, but catalogue 04 §6 still carries the old code name. The wire value is left alone (G9); the RM-facing wording follows `ADR-014` |

Neither is repaired here: both are edits to a ratified catalogue, which is the owner's call, and
this increment's scope is the library. Raised so the CI diff in `ERR-005` does not silently
normalise them away.

### 13.1 What `ERR-001` and `ERR-005` delivered

| Delivered | Where |
|---|---|
| `ErrorCategory`, `Retryability`, `AuditDisposition`, `Propagation`, `PlatformLayer` | `libs/bank-common-error` |
| `ErrorDefinition` + `ErrorCatalogue` — 63 codes seeded from catalogue 04 | `ErrorCatalogue.java` |
| `ErrorDiagnostic`, `ErrorOrigin` (first-origin-wins), `IncidentId` (sortable, 26-char) | `libs/bank-common-error` |
| `ServiceErrorResponse` gains `category`, `service`, `incidentId`, `correlationId`, `origin`, `diagnostic`, and `toPublic()` | `ServiceErrorResponse.java` |
| `ServiceException.of(code)` — catalogue-driven, builds both halves under one incident id | `ServiceException.java` |
| Evidence: 98.5% line / 82.9% branch on the lib; full multi-module build green | `./gradlew build` |

Still open, in order: `ERR-002` (one shared handler; redaction wired at L4), `ERR-003`
(propagation across services), `ERR-004` (structured logging and `bank.error.count`), `ERR-006`
(PII test, ArchUnit leak rule), `ERR-007` (runbook pages).

**`toPublic()` is built and tested but not yet wired.** Until `ERR-002` calls it at the boundary,
the leaks in **D1** and **D2** are still live in the three existing handlers. The capability
exists; the enforcement does not.
