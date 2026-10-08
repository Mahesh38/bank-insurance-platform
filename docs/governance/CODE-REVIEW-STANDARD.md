# Code review standard — coverage, structure, logging

**Authority:** GATE-S08 **S08-G8** adoption (thickens the existing engineering bar; does not reopen stage state)  
**Owner:** Amit / Engineering · **QA co-owner (floors):** Swapnali · **Observability contract:** Shivanshi (application-level now; dashboards/SLOs remain Phase 6) · **Security co-owner:** Deepali (SEC-C*)  
**Status:** Binding for every production-code PR · **Layer:** L3 — this repository  
**Adoption:** [PR-REVIEW-CHECKLIST.md](./PR-REVIEW-CHECKLIST.md) · `.github/pull_request_template.md`  
**Numeric coverage SSOT:** [COVERAGE.md](../1sb-insurance-integration/service-ssot/COVERAGE.md) — do not fork the floors here  
**Error/log shape SSOT:** [07-PLATFORM-ERROR-CONTRACT.md](../journey-execution/07-PLATFORM-ERROR-CONTRACT.md) §6–§8

This is the operational review contract. ENG/SEC ids stay in
[ENGINEERING-AND-SECURE-CODING-STANDARDS.md](./ENGINEERING-AND-SECURE-CODING-STANDARDS.md).
This file answers four questions the thin checklist did not: **what** is reviewed, **how**,
**what is mandatory to merge**, and **what coverage must already be true on the branch at push**.

A seven-board gate ([11-REVIEW_GATES.md](./11-REVIEW_GATES.md)) is a different artefact. This
standard is the **implementation review** every PR gets. It does not manufacture T4 signatures.

---

## 1. Coverage on the branch at push

CI `jacocoTestCoverageVerification` (or `check` / `build`) is the merge bar. `./gradlew test`
alone reports and **does not** fail floors ([COVERAGE.md](../1sb-insurance-integration/service-ssot/COVERAGE.md)).

| Module group | Line | Branch | Test presence (same PR) |
|---|---|---|---|
| `libs/*` | **≥ 80%** | **≥ 70%** | New/changed public behaviour has a `FooTest` (TESTING-RULES R1–R2) |
| `1sb-integration-service`, `bank-persistence-service` | **≥ 90%** | **≥ 70%** | R2 for `application.*` / `api.*` / adapters |
| Other `services/*` (scaffold, including BFF and customer-service) | **≥ 50%** line | *not gated* | R2; **publish measured branch %** in the PR anyway |
| New library module | 80 / 70 from the first commit | Same | No “we will cover it later” |

**Author, before push**

1. Run `./gradlew :<touched-module>:test jacocoTestCoverageVerification` for every module this branch changes.
2. Paste **measured** line % and branch % (and test count) into the PR template. Unexecuted is not passed.
3. Same-PR tests for new/changed behaviour (R2). A production type with logic and no `*Test` is a must-fix.
4. Do not merge a drop below the module floor. Do not lower a floor without TL + QA Lead + TECH-DEBT id + expiry.
5. New branches in security, error-translation, or token/session paths must be **executed** by a test, not only compiled.

**Reviewer**

- Reject if touched-module verification was not run, numbers are missing, or a new error/security branch has no test.
- Scaffold services may sit above 50% line with ungated branch; that is not a licence to skip tests on the diff.

Package-level strategy floors remain **QA-014** (not enforced as Jacoco rules). Dashboards that
chart coverage are not a substitute for the module gate.

---

## 2. What we review (aspects)

Every applicable row is in play. “Not applicable” must be stated, not skipped.

| Aspect | Looks for | Binding ids |
|---|---|---|
| Purpose / scope | Work item id; change matches the admitted AC; no silent extra feature | ENG-10, AE-1 |
| Structure / boundaries | Hex/ports; vendor types in adapters; shared behaviour in `libs/*`; Java 21 | ENG-1–ENG-3 |
| HTTP / API contract | OpenAPI when public; fail closed; encoded path segments | ENG-4, SEC-C3 |
| Tests | Pyramid, assertions on **error codes**, no PII in log snapshots | ENG-5, R1–R10 |
| Coverage | Floors in §1; numbers in the PR | ENG-5, ENG-9 |
| Static quality | Checkstyle, Spotless ratchet, ArchUnit | ENG-6, ENG-7 |
| Dependencies | No floating versions; no secret in git | ENG-8, SEC-C1, SEC-C6 |
| Authn / authz | Default-deny; tokens stay in BFF; session handle not in `Authentication` credentials | SEC-C4, SEC-C9 |
| Input / injection | Boundary validation; no string-concat SQL | SEC-C3, SEC-C5 |
| **Logging / monitoring** | §3 — mandatory on every behaviour change | OBS-1–OBS-10, SEC-C2, SEC-C8 |
| Errors | Catalogue `ErrorCodes.*` via `ServiceErrors`; no raw 500 for known upstream failure | Platform error contract |
| Operability | Timeouts on new HTTP clients; fail closed on missing config | OBS-9 |
| Docs / drift | ADR if trust boundary/crypto; DOC-MAP if a new doc; TODO has a work-item id | ENG-2, ENG-10, SEC-C10 |

---

## 3. Logging and monitoring — mandatory merge bar

Application logging and `bank.error.count` **are in scope now**. Grafana/SLO dashboards and
alert routing are **WS-1 Phase 6 / L9** and are **not** a PR merge blocker unless the change
*is* an observability deliverable. Do not invent a second telemetry stack on a feature PR.

### Must (blocks merge)

| ID | Rule |
|---|---|
| OBS-1 | Failures that leave the service as HTTP go through `ServiceErrors` → `PlatformErrorHandler` / `PlatformErrorAdvice` → `ErrorRecorder` (structured log + `bank.error.count`). Do not `catch` and return an ad-hoc body. |
| OBS-2 | No PAN, Aadhaar, phone, email, health, session handle, access token, client secret, or raw upstream body in logs or exception messages (SEC-C1, SEC-C2). CBS query values and request URIs with `q` never enter the message. |
| OBS-3 | Security-relevant outcomes use catalogue codes with actor/component/operation: session missing/expired/invalid, default-deny, token mint reject. That **is** the auditable event for this stage (SEC-C8). A second audit topic is not required on the same PR. |
| OBS-4 | Upstream failures log **status class + operation + catalogue code**. Never URI, never `q`, never token, never response body. |
| OBS-5 | `RequestDiagnosticFilter` seeds `correlationId`. New code must not log before that filter on the request path, and must not swallow exceptions before the handler (or the incident id never appears). |
| OBS-6 | Client-caused catalogue categories: **WARN**, no stack. Platform/upstream: **ERROR**, with stack. `Slf4jErrorRecorder` already does this — do not bypass it with `log.error("something went wrong", ex)`. |
| OBS-9 | New outbound HTTP clients: connect/read timeouts **and** translation to `UPSTREAM_*` / `RESOURCE_NOT_FOUND` / domain-specific types. `IllegalStateException` from a token client must not escape to a bare Spring 500. |
| OBS-10 | Tests assert **error codes** (R5) and that messages/logs under test **do not** contain secrets or the inquiry `q`. |

### Should (same PR if cheap; else SUG with owner + expiry)

| ID | Rule |
|---|---|
| OBS-7 | Non-error operational events (token cache hit, refresh under lock) stay **DEBUG** and never print the token. Token **reject** / remint-exhausted stay WARN/ERROR via OBS-1. |
| OBS-8 | `ErrorRecorder` → `bank.error.count` is the Phase-1/4 metric. Do not add a parallel Micrometer domain counter for the same failure without an SRE reason. |

### Not now (do not block the PR; do not build)

- Journey SLO dashboards, page alerts, error-budget burn (BOOT.md WS-1 out of scope: “Dashboards, alerting, SLOs — revisit at Phase 6”).
- Cluster cache hit-rate dashboards (Valkey / ADR-011 remainder).
- Logging raw request/response payloads “for debug”.

Libraries without a service id (for example `bank-common-apigee`) log a **structured event name +
status + operation** and throw. The **service** adapter must wrap that throw in `ServiceErrors`
so OBS-1 still holds at the HTTP boundary.

---

## 4. How we review

1. **Author** completes `.github/pull_request_template.md` (coverage table + ENG/SEC/OBS).
2. **Reviewer** walks [PR-REVIEW-CHECKLIST.md](./PR-REVIEW-CHECKLIST.md) against this file. Tick
   only what was actually inspected.
3. **Must-fix** comments block merge. **Should-fix** may land on the same PR or a dated SUG.
4. **Evidence:** Gradle test summary + Jacoco line/branch % for touched modules. Unexecuted = not passed.
5. **Verdict:** `APPROVE` · `CHANGES_REQUESTED` · `COMMENT`. Silent merge with open must-fix is forbidden
   (ROLE-GUIDELINES TL-3).
6. **Waivers:** owner + expiry + compensating control + TECH-DEBT or RISK id (standards §4).
7. **Boards / T4:** this review is Amit+Swapnali implementation quality. It is not Deepali T4,
   not Shivanshi Board 7 production readiness, and not IRDAI evidence.

Agents reviewing a PR load this file (capsule `code-change`) and record must-fix / should-fix
with ids. They never self-approve a board that requires a human.

---

## 5. Definition of done (PR)

A PR is review-complete only when:

1. §1 coverage evidence is in the body and CI `jacocoTestCoverageVerification` is green for touched modules.
2. Every applicable ENG / SEC / OBS row is ticked or waived.
3. Logging/monitoring §3 must-rows hold for the diff (including negative tests on new failure paths).
4. Spotless, Checkstyle, and ArchUnit remain green.
5. Work-item / SUG ids are named; no TODO without an id.
