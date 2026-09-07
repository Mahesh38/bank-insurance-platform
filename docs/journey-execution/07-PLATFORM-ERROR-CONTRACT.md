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
| `customer` | `services/customer-service` |
| `lead` | `services/lead-service` |
| `consent` | `services/consent-service` |
| `suitability` | `services/suitability-service` |
| `catalogue` | `services/product-catalogue-service` |
| `journey-orchestration` | `services/journey-orchestration-service` |
| `quotation` | `services/quotation-service` |
| `proposal` | `services/proposal-service` |
| `payment` | `services/payment-service` |
| `policy` | `services/policy-issuance-service` |
| `integration-hub` | `services/integration-hub-service` |
| `audit` | `services/audit-compliance-service` |
| `notification` | `services/notification-service` |
| `reporting` | `services/reporting-mis-service` |
| `admin-config` | `services/administration-config-service` |
| `direct-insurer` | `services/direct-insurer-adapter-service` |

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

Emitted as one structured line. `MdcKeys` declared `correlationId`, `journeyId`, `jobId` and
`traceId` from the start, but **nothing populated them** until `ERR-006` added
`RequestDiagnosticFilter`; a log platform cannot join lines on a field nobody writes.

Two joins, and they are not the same one — an earlier draft of this section claimed a single join
that is not achievable:

| Join | Covers | Set by |
|---|---|---|
| `correlationId` | **every line** of the request, in every service it reaches | `RequestDiagnosticFilter`, at the edge, generated when the caller sends none |
| `incidentId` | the failure itself, and every line after it | the failure, at the point it occurs |

An incident id cannot appear on lines logged *before* the failure it names, because it does not
exist yet. `correlationId` is the join that reaches those; `incidentId` is the one the caller
quotes. Support uses the second to find the first.

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
| 3 | *(not stated)* | `UPSTREAM_BAD_RESPONSE` was **retryable at one call site and not at another** | `Retryability.YES` | Nothing — resolved. `FUNC-007-ASSIGNMENT.md` line 28 ratifies "502, retryable" and `FUNC-007-REVIEW.md` records it passed, so the documented AC decides. This is defect **D4** in its purest form: one code, two behaviours, caught the moment a registry forced a single answer |
| 4 | `QUOTE_EXPIRED` = **409** (§6, also `VR-082`, `INV-QUO-04`, `AC-COMP-010-3`) | `FUNC-004` AC-2 ratifies **410** for the same code | 409, with one audited override at the FUNC-004 site | **A decision.** These are two different conditions wearing one code — an offer selected past its validity window (409) versus a quote job that is gone (410). Both are human-ratified. Splitting them into two codes is additive but changes `FUNC-004`'s ratified wire response, so it is the catalogue owner's call, not the library's. Preserved as-is and surfaced: `grep -rn statusOverride` lists every such departure |

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

### 13.2 What `ERR-002` delivered

| Delivered | Where |
|---|---|
| `PlatformErrorHandler` — the one place an error becomes an HTTP response | `libs/bank-common-error` (Spring/slf4j `compileOnly`, the lib house pattern) |
| All three advices now extend it; the duplicated mapping code is gone | `onesb` · `persistence` · `bff` |
| Redaction wired at L4, and **`PUBLIC` is the default** — a service that declares no boundary redacts | `PlatformErrorHandler.Boundary` |
| Log-then-redact ordering, proven by test | `redactionHappensAfterTheDiagnosticIsRecorded` |
| Log level follows category: client-caused `WARN` without a stack, platform-caused `ERROR` with one | `PlatformErrorHandler.record` |
| **D1 closed** — upstream 1SB prose no longer reaches `detail`; it moves to `reason` | `OneSbErrorNormaliser` |
| **D2 closed** — `"1SB call failed: GET /path"` and `"Unexpected 1SB status"` no longer reach `detail` | `OneSbHttpClient` · `PaymentService` |
| **D6 closed** — the BFF returns the platform envelope with a `code`, instead of a bare `ProblemDetail` | `BffExceptionHandler` |

**One caller-visible change, made deliberately.** The BFF's 400 path used to echo
`IllegalArgumentException.getMessage()`, guarded by a test that called the asymmetry intentional.
It is withdrawn: the exceptions reaching that path include
`"workforce.session.encryption-key must decode to 32 bytes"`, so "the message is safe" was a
coincidence, not a property. The message now goes to the diagnostic and the caller receives an
incident id instead. The test was rewritten to assert the stronger property across **both** paths
rather than deleted. This is the one item on this increment a reviewer should look at first.

### 13.3 What `ERR-003`, `ERR-004` and the `D4` completion delivered

**`D4` is now fully closed.** `grep -rn "ServiceErrorResponse.builder()\|new ServiceException("` over
service main code returns **nothing**: every throw site in all five services goes through
`ServiceException.of(code)` and takes its status, wording, retryability and runbook from the
registry. What is specific to a request — the identifiers, the developer reason — travels in
`reason` and `errors[]`, where it belongs.

| Delivered | Where |
|---|---|
| Every application throw site migrated to the catalogue | `ProposalService` · `QuoteService` · `StatusService` · `PaymentService` · `MasterDataService` · both LOB registries · `IdempotencyFilter` · persistence `NotFound` |
| `statusOverride(status, ratifiedBy)` — narrow, mandatory-attribution escape for a ratified disagreement | `ServiceException.Builder`; exactly **one** use in the tree, listed above |
| **`ERR-003`** `ErrorPropagation` — preserves incident id and first origin, and decides propagate-vs-wrap from the registry | `libs/bank-common-error` |
| An upstream `INTERNAL_ERROR` now **wraps** rather than propagating | `ErrorCatalogue` — `INTERNAL` means *our* defect; propagating theirs claims their bug as ours |
| **`ERR-004`** `MdcKeys` gains `incidentId`, `errorCode`, `errorCategory`, `service`, `originService`, `layer` | `libs/bank-common-observability` |
| **`ERR-004`** `bank.error.count` with the §7 tag set, emitted once per failure | `ErrorMetrics`, called from `PlatformErrorHandler` |
| Cardinality guard — absent, punctuated and digit-heavy values collapse to `unknown` | `ErrorMetrics.safeTag` |
| Micrometer added **no dependency**: all five services already ship `spring-boot-starter-actuator` | service `build.gradle.kts` (unchanged) |

Metric emission is optional by construction: a `@WebMvcTest` slice has no `MeterRegistry`, and a
service must not lose its error responses because it cannot count them.

### 13.4 What `ERR-006` and `ERR-007` delivered

| Delivered | Where |
|---|---|
| **`ERR-006`** `RequestDiagnosticFilter` — seeds `correlationId`, `service`, `journeyId`; echoes the correlation id on the response; clears MDC in a `finally` | `libs/bank-common-observability`, registered at highest precedence in all five services |
| The handler stamps the seeded `correlationId` onto the response, so the caller can quote it | `PlatformErrorHandler.emit` |
| **`S08-G7` evidence** — `NoPiiInErrorOutputTest`: no caller-facing text names a regulated attribute or a platform internal, every string is a constant, a fully-loaded diagnostic survives redaction with nothing leaked, and no MDC key would put an attribute in the log index | `libs/bank-common-error` |
| **ArchUnit leak rule** — no service class may call `ServiceErrorResponse.builder()`, with a fixture proving the rule bites | `ArchitectureTest` · `ArchitectureRulesBiteTest` · `fixtures/errors` |
| **Parity, as §3 promised** — `CatalogueParityTest` fails the build if a registered code is missing from catalogue 04, if catalogue 04 names a refusal no service can emit, or if a code has no runbook page | `libs/bank-common-error` |
| **`ERR-007`** `08-SUPPORT-RUNBOOK.md` — 64 `RB-*` pages, each with What it means / Is it a defect? / L1 action / L2 escalation / **Never** | generated by `scripts/support/build-error-runbook.py` |

**The runbook is generated, deliberately.** A support page maintained separately from the code
describes last quarter's behaviour, and the gap is discovered mid-incident. Guidance lives in the
generator: category defaults for the ordinary cases, curated text for the fifteen that carry real
consequence — the compliance gates, `PAYMENT_STATE_UNCERTAIN`, `PREMIUM_MISMATCH`,
`AUTHENTICATION_FAILED`.

Two mislabelled `catalogueRef` values in the registry were found by the parity test and corrected:
`QUOTE_TIMEOUT` cited 04 §7, which names the journey state `TIMED_OUT` rather than that code, and
`IDEMPOTENCY_CONFLICT` cited 04 §2, which names `IDEMPOTENCY_KEY_CONFLICT` (§13 row 1).

### 13.5 Design review, and what it changed

A SOLID/DRY review of the delivered increment found two things that were reported as done and were
not, plus real duplication. Recorded rather than quietly fixed, because the first two change what
the epic can claim.

**Corrections.**

| # | Finding | Resolution |
|---|---|---|
| 1 | **`ERR-003` was not wired.** `ErrorPropagation` was referenced only by its own test, and no service deserialised a peer's envelope — so cross-service attribution did not happen at runtime | `ProblemJsonReader` added; both BFF clients now propagate, preserving incident id and first origin. `IdentityAuthorizationClient` previously let a downstream refusal escape as a raw `RestClientResponseException` into Spring's default 500 |
| 2 | **Two of five services had no handler.** `identity-authorization-service` (the PDP) and `identity-provider-adapter-service` returned Spring's default error — no `code`, no `incidentId`, no attribution — contradicting this epic's `completion_definition` | Both now get the envelope from `PlatformErrorAutoConfiguration` without writing any code |

**DRY and configuration.** The service id was a string literal in **31 places**; five
`RequestDiagnosticConfig` classes were byte-identical apart from one string; `ServiceErrorResponse`
re-listed all fifteen constructor arguments in five places; and the same
`.service(…).layer(…)` preamble opened twenty-three throw sites.

| Was | Is |
|---|---|
| 31 service-id literals | `bank.error.service-id`, injected once via `ServiceErrors` — **0 literals** in main code |
| 5 identical filter configs + 2 advice subclasses | one `@AutoConfiguration`; **7 classes deleted** |
| `validationStatus()` overridden by subclassing | `bank.error.validation-status` |
| Redaction fixed at compile time | `bank.error.boundary` (architectural) **and** `bank.error.expose-diagnostics` (debug, **refused under the prod profile**) |
| 15-argument copy repeated 5× | one `copy()` path over `toBuilder()` |
| `ErrorDiagnostic`: 15 hand-written getters | Lombok `@Value`, 171 → 134 lines, no call-site change |

The two settings in that fourth row are deliberately separate. Collapsing them into one flag would
have forced `1sb-integration-service` — cluster-private, and legitimately not redacting — to enable
the debug switch, and then fail to start in production against the guard protecting devices.
Architectural position is permanent; debug exposure is per-environment.

**Boilerplate.** A second pass removed the hand-written code that duplicated what Lombok already
generates, and the dead API that predated the catalogue.

| File | Was | Is | Why it went |
|---|---:|---:|---|
| `ServiceErrorResponse` | 277 | 183 | `@Singular` gives exactly the accumulate / `addError` / `clearErrors` semantics that were coded by hand, plus an immutable list; `@Builder.Default` covers `type` and `timestamp`; `@NonNull` covers the null checks. The custom `build()` re-listed all fifteen constructor arguments |
| `ErrorHandlingSettings` | 101 | 71 | `@Value @Builder` — the whole builder and every accessor were generated code written out |
| `PlatformErrorProperties` | 76 | 58 | `@Getter @Setter` — twelve accessors |
| `ServiceException` | 209 | 194 | Seven pre-catalogue factories deleted (see below); duplicated `reason` state removed |
| `ErrorDiagnostic` | 134 | 123 | Ten pass-through delegates deleted; `@Singular` for the cause chain |

Net **−252 lines of main code**. What survives in the three builder classes is only what a generated
setter cannot do: `diagnostic()` adopts the incident id and origin, `upstream()` sets three fields
that always travel together, `cause()` walks an exception chain, and `incidentId()` ignores a null
so a peer that sent none does not blank the token support searches on.

> One thing deliberately undone during this pass: the first version of
> `ErrorDiagnostic.Builder.incidentId` reached into Lombok's generated `$value` / `$set` fields to
> override a `@Builder.Default`. It compiled and the tests passed. It was replaced anyway — a shared
> library coupled to codegen internals breaks on a Lombok upgrade, in every consumer at once.

**Dead pre-catalogue API removed.** `ServiceException.validation / upstreamBusiness /
upstreamUnavailable / upstreamAuth / unauthorized / forbidden / internal` and the matching
`ServiceErrorResponse` factories had **no production call sites** once every throw site moved to the
catalogue. They were also the defect this contract removes, preserved as public API: each hand-built
an envelope with a literal title and status. The ArchUnit rule stops a service reaching for
`ServiceErrorResponse.builder()`; leaving these would have left the same door open one method along.

**SOLID.** `PlatformErrorHandler` mapped, stamped, redacted, logged and metered; recording moved to
`ErrorRecorder` / `Slf4jErrorRecorder`, leaving the handler the HTTP contract. `ErrorCatalogue` was
a closed static map; `ErrorDefinitionProvider` (via `ServiceLoader`) lets a module contribute codes
without editing the shared library, and fails at class initialisation if one redefines an existing
code.

> **Recorded per Rule 3 of the AI execution contract.** The SPI was flagged before it was built as
> premature structure — S08 posture rejects generic extension points on sight, and there is one
> consumer set today. Mahesh directed it be built anyway; this note is the record, not a
> re-argument. Revisit at the second LOB module: if nothing has implemented
> `ErrorDefinitionProvider` by then, delete it.

### 13.6 Still open


All seven stories of `EPIC-001` are delivered. What remains is not code:

- **Seven T3 board verdicts on `ADR-017`** — Architecture, Technical, Product, QA, Security, Risk &
  Compliance, Operations. The contract is `PROPOSED` until they are recorded.
- **The `QUOTE_EXPIRED` 409/410 decision** (§13 row 4) — the catalogue owner's call. One audited
  `statusOverride` preserves both ratified behaviours until it is made.
- **Grafana dashboards and alert rules** remain `L9` work, as §10 says. The series is emitted; the
  panels are not built.
- **What `S08-G7` does not prove**: that no engineer ever writes a customer attribute into a
  `reason` at a throw site. That is bounded by review and by `ErrorDiagnostic`'s contract, and it is
  stated in the test itself so the limit is visible rather than implied by a green run.
