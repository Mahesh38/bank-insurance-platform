# 01 — Current State (Context Resolution)

**Layer:** L3 — **project-specific, rewritten per repository**
**Pipeline step:** 1 — Context Resolution
**Machine-readable twin:** [state/CURRENT-STATE.yaml](./state/CURRENT-STATE.yaml) — *agents read the YAML; this file explains it*
**Update cadence:** on every stage transition, scope change, and at each gate review
**Owner:** Delivery Lead (edits) · Architect + PO (ratify)

---

## 1. Why this file exists

Step 1 of the pipeline is **"Where are we now?"** — and it is the step agents most often skip.
Every downstream classification (stage fit, necessity, priority) is meaningless without it. An
agent that cannot resolve current state **must stop and ask**, not guess.

> **Rule CS-1 — No context, no verdict.** If `state/CURRENT-STATE.yaml` is missing, malformed,
> or older than its `review_due` date, the agent halts triage and reports the gap. It does not
> fall back to "probably fine".

---

## 2. How an agent resolves context

```text
1. Read state/CURRENT-STATE.yaml
2. Identify which workstream the input belongs to (§4 below)
   → if it maps to none, scope fit is at best SC2; usually SC3.
3. Load that workstream's current_stage, objective, deliverable, gate
4. Load in_scope / out_of_scope for that workstream
5. Check staleness: state_as_of + review_due
6. Carry the resolved context into the triage record's `context` block
```

The resolved context is quoted verbatim in the triage record so a later reader can tell what
the agent believed at decision time — a decision that was correct against a stale state is a
*state* defect, not a *judgement* defect, and the two are repaired differently.

---

## 3. Snapshot

| Field | Value |
|-------|-------|
| Project | Bank Insurance Platform (`mahesh38/bank-insurance-platform`) |
| State as of | **2026-09-11** — refreshed by R12 (Delivery Lead seat) |
| Review due | 2026-10-11 |
| Ratified by | **Mahesh (Solution Architect), 2026-08-10** (GOV-004) — PO counter-signature outstanding; stage, scope and objective values re-confirmed unchanged on 2026-09-11 |
| Active workstreams | **3** (see §4) — WS-3 is primary |
| Governance version | AIGEM 1.4 |

> This snapshot was reconstructed from repository artefacts (`ACTION-PLAN.md`, phase
> `STATUS.md` files, `TECH-DEBT.md`, git history) and **ratified by the Solution Architect on
> 2026-08-10** ([GOV-004](./registers/DECISION-REGISTER.md#2-governance-decisions)). Agents run
> the full pipeline against it. The PO counter-signature is a recorded formality, not a
> restriction — see §7.

> **2026-09-11 refresh.** The file had gone 32 days without review and past its `review_due` of
> 2026-09-09, which halted new-work admission for every agent under Rule CS-1. The refresh is a
> review sweep, not a date bump: the delta is in §4.1 and the per-criterion evidence is in
> [`state/GATE-EVIDENCE.yaml`](./state/GATE-EVIDENCE.yaml). **No stage advanced, no gate was
> marked `CANDIDATE` or `PASSED`, and no criterion was marked `MET`.** Three workstreams sit in
> the same phase they did on 2026-08-10.

---

## 4. Workstreams

This repository carries more than one lifecycle at once. **Stage fit is evaluated against the
workstream the input belongs to, never against the repository as a whole.** A suggestion about
Keycloak session handling is on-stage for WS-2 and premature for WS-1.

### 4.1 What changed between 2026-08-10 and 2026-09-11

Read this before triaging anything against the previous snapshot. Nine facts moved; none of them
moved a stage.

| # | Change | Where the evidence is |
|---|--------|-----------------------|
| 1 | **WS-3 was missing from this file entirely.** It is the primary workstream, registered by CR-010, and §4 listed only WS-1 and WS-2. Added below. | [CR-010](./change-requests/) · [WS-3 charter](./workstreams/WS-3-PLATFORM-CHARTER.md) |
| 2 | **Application CI exists and runs on every push and pull request**, building and testing all 27 modules. 348 recorded runs; the 15 most recent all green. | `.github/workflows/application-ci.yml` (`c4e8d9f`) |
| 3 | **A security scanning pipeline exists and blocks**: gitleaks, CodeQL Java SAST, Trivy SCA, scheduled full-history secret scan, SBOM. Container image scanning is still absent. | `.github/workflows/security-scanning.yml` |
| 4 | **The service estate was scaffolded from the architecture catalogue** — 21 services and 6 shared libraries, each service carrying an ArchUnit boundary test. They are skeletons: 2 test files each. | `settings.gradle.kts` (`b6d4304`, `b9040c3`) |
| 5 | **EPIC-001 platform error contract delivered** across the shared libraries: one error handler, L4 redaction, incident ids, MDC propagation. | ADR-017 · `6243aa5`…`e6c806f` |
| 6 | **EPIC-002 Life LOB adapter coverage delivered on main**: typed Term, Saving and ULIP quote and proposal handlers, bank domain model extracted, circuit breaker corrected for 422, QA-012 WireMock regression. | `e273a82`, `0f407b9`, `a866143`, `59f193c`, `eb1dbc0` |
| 7 | **EPIC-003 NIP BFF lead landing and create API contract published.** | `888096f`…`ba20387` |
| 8 | **Five GATE-S08 criteria moved OPEN → PARTIAL** on assembled evidence (G1, G3, G4, G5, G7) and **WS-1 criterion 4.7 moved BLOCKED → PARTIAL** under the condition the state file itself recorded. | [`GATE-EVIDENCE.yaml`](./state/GATE-EVIDENCE.yaml) |
| 9 | **Every blocker follow-up date in the programme had passed** — the newest by 12 days, the oldest by 21. All re-dated against their named owners. | [`GATE-EVIDENCE.yaml`](./state/GATE-EVIDENCE.yaml) · [DEPENDENCY-REGISTER](./registers/DEPENDENCY-REGISTER.md#2-external-dependencies) |

**What did not change, and is the more important half:** no gate criterion closed in 32 days. WS-1
GATE-P4 has been `BLOCKED` since 2026-08-16 on four blockers that nobody chased. WS-2 had no
dedicated effort at all. The capability delivered in this period (services, error contract, Life
LOB, lead contract) is real, and none of it was gate-closing work.

**Four named gaps** stand between the current evidence and GATE-S08, each against a ratified
criterion rather than new scope:

1. **Static analysis has no mechanism** (S08-G4). No Checkstyle, PMD, SpotBugs or Spotless in the
   root build. ArchUnit is enforced; the other half of the criterion is not.
2. **No container image scanning** (S08-G5). Three of four named scanners are in the pipeline and a
   root `Dockerfile` ships an image that nothing scans.
3. **No log-scan PII test** (S08-G7). Redaction is unit-tested; S08-VT-06 asks for a test that
   scans all emitted logs, which would prove no path bypasses the masker.
4. **No shared integration harness** (S08-G6). WireMock exists in one module, Testcontainers in
   none, so the middle of the test pyramid has nothing to stand on. TD-014 is the ledger entry.

### WS-3 · AU Bank Insurance Distribution Platform — **primary**

| Field | Value |
|-------|-------|
| **Canonical stage** | S08 — Engineering Foundation |
| **Current phase** | Foundation Recovery Increment — S08 with S09 overlapped |
| **Stage status** | IN_PROGRESS |
| **Current objective** | `R0-ASSISTED-TERM-SALE` — one RM sells one Term Life policy to one ETB customer, end to end, with consent and suitability evidence, payment on the customer's own device, and a complete audit trail |
| **Deliverable** | Application CI with enforced quality, security and architecture gates (S08); IaC, environments, secrets, observability and 7-year write-once retention in ap-south-1 (S09); consent and suitability rule packs, R0 acceptance criteria, product matrix and service blueprint in parallel |
| **Delivered so far** | Application CI and security scanning pipelines; 21 services and 6 libraries scaffolded with ArchUnit boundary tests; EPIC-001 error contract; EPIC-003 lead API contract |
| **Not yet started** | All of S09 — IaC, environments, secrets management, the ap-south-1 retention path |
| **Gate** | `GATE-S08` · `OPEN` · 5 of 10 criteria PARTIAL, 5 OPEN, none MET |
| **Next stage** | S09 — Platform & Environment Foundation |
| **Authority** | [WS-3 charter](./workstreams/WS-3-PLATFORM-CHARTER.md) · [architecture registration](../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md) |
| **Entry condition on S11** | Non-waivable (Rajal condition C5): no WS-3 stage enters S11 while GAP-006 (consent) or GAP-007 (suitability) is open |

**Gate to exit S08** — evidence and owners in
[`state/GATE-EVIDENCE.yaml`](./state/GATE-EVIDENCE.yaml):

| # | Exit criterion | State | Owner |
|---|----------------|-------|-------|
| S08-G1 | CI builds and tests every module on every PR | 🟡 Partial — E4 evidence assembled, `MET` is Engineering's to declare | Amit |
| S08-G2 | Merge to main impossible without a green pipeline | ❌ Open — branch protection is a repo-admin setting, not a file here | Amit |
| S08-G3 | Coverage thresholds enforced; QA-001 closed | 🟡 Partial — enforcement runs; QA-001 open on the interim service floor | Swapnali |
| S08-G4 | ArchUnit and static analysis enforced | 🟡 Partial — ArchUnit in 18 modules; no static analysis at all | Amit |
| S08-G5 | Secret, SAST, SCA and image scanning in the pipeline | 🟡 Partial — three of four; no image scanning | Deepali |
| S08-G6 | Test infrastructure at every pyramid level | ❌ Open — blocked by TD-014; no shared integration harness | Swapnali |
| S08-G7 | No PII in logs, proven by automated test | 🟡 Partial — redaction unit-tested; no log-scan test (S08-VT-06) | Deepali |
| S08-G8 | Engineering and secure coding standards published | ❌ Open — no mechanism started; blocks nothing, which is why it keeps slipping | Amit |
| S08-G9 | Pipeline feedback < 10 min p95; flake < 1% | ❌ Open — unmeasured; observed sample is inside both thresholds | Shivanshi |
| S08-G10 | A new engineer can build, test and ship in under a week | ❌ Open — needs an onboarding record, which has a lead time the others do not | Amit |

### WS-1 · 1SB Insurance Integration

| Field | Value |
|-------|-------|
| **Current phase** | Phase 4 — Hardening & consumer enablement |
| **Stage status** | IN_PROGRESS (partial) |
| **Current objective** | Term path signed off for UAT use by at least one bank caller |
| **Deliverable** | Hardened Term vertical slice: sandbox E2E evidence, published OpenAPI, compliance-reviewed audit schema, runbook, performance smoke |
| **Completed** | Phases 0–3. Term path FUNC-001…007, FUNC-009 delivered and TL-approved (`phase-3/STATUS.md`, `phase-4/STATUS.md`). COMP-003 raw-payload encryption, JaCoCo gates, Docker packaging, OpenAPI publication landed (commit `79c65f4`). **EPIC-002 Life LOB adapter coverage delivered on main 2026-09-03** — typed Term, Saving and ULIP handlers, bank domain model extracted, circuit breaker corrected for 422 business errors, QA-012 WireMock regression (CR-014). |
| **Not yet done in this phase** | Sandbox E2E suite in CI (4.1), bank consumer spike (4.3), compliance review of audit schema (4.4), ops runbook (4.5), performance smoke (4.6). **None of the seven criteria closed between 2026-08-10 and 2026-09-11.** Adapter capability grew; gate closure did not. Criterion 4.7 moved BLOCKED → 🟡 Partial because application CI now executes the coverage gate on every PR, which is the condition the state file had already recorded. |
| **Gate state** | `BLOCKED` since 2026-08-16 on four blockers (`GATE-4.1-SANDBOX-E2E`, `DEP-001`, `DEP-002`, `DEP-003`) whose follow-up dates had all passed by 12–21 days. Re-dated 2026-09-11 against their named owners. |
| **Next stage** | Phase 5 — Expand LOBs (Health → Motor) |
| **Authority** | [ACTION-PLAN.md](../1sb-insurance-integration/service-ssot/ACTION-PLAN.md) · [PRODUCT-BACKLOG.md](../1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md) |

**Gate to exit Phase 4** — see [04-STAGE_GATES.md](./04-STAGE_GATES.md):
- [ ] Term happy path green against 1SB sandbox in CI (or gated nightly)
- [ ] OpenAPI published + consumer collection available
- [ ] ≥ 1 bank caller has exercised quote + proposal against UAT
- [ ] Compliance sign-off on audit schema and log samples
- [ ] Runbook (secrets rotation, IP whitelist, 1SB 401/5xx incident) exists
- [ ] p95 quote latency measured under nominal concurrency
- [ ] Coverage gates green; QA-001 closed or waived with expiry (added by CR-001)

### WS-2 · Workforce Authentication & Authorization

| Field | Value |
|-------|-------|
| **Current phase** | Phase 1 — Foundation implementation |
| **Stage status** | IN_PROGRESS |
| **Current objective** | Provider-neutral workforce identity: BFF token-hiding session, Keycloak adapter, business authorization service |
| **Deliverable** | `workforce-access-bff`, `identity-provider-adapter-service`, `identity-authorization-service` meeting the accepted decisions in the SSOT |
| **Completed** | Architecture baseline approved; foundation services scaffolded with PDP, provisioning outbox, Keycloak realm config (commit `cd40460`) |
| **Movement since 2026-08-10** | **None.** All six criteria A.1–A.6 remain OPEN with no evidence attached and no blockers recorded. The only commits touching the three services were cross-cutting (the EPIC-001 error handler, CI dependency fixes). Six criteria with no blockers and no progress is an unstaffed workstream, not a blocked one. |
| **Why it matters to WS-3** | WS-2 is the ENABLER for bounded context #3 behind interface IF-2. A.1 (BFF token-hiding) is the criterion the WS-3 RM journey cannot be evidenced without, so a WS-2 stall surfaces later as a WS-3 slip. Sequencing it against WS-3 R0 is a Product and Architecture call. |
| **Next stage** | Phase 2 — Federation with bank AD + production provider decision |
| **Authority** | [authentication-authorization/README.md](../platform/authentication-authorization/README.md) |
| **Explicitly out of scope, Phase 1** | Retail-customer authentication; production IdP selection (deferred behind the adapter) |

### Cross-cutting

| Field | Value |
|-------|-------|
| Platform architecture baseline | [architecture-review/](../platform/architecture-review/README.md) — target microservices, comms patterns, AWS infra, data, security/NFR, ADR log |
| Business requirement baseline | [au-bank-insurance-platform/requirements/](../au-bank-insurance-platform/requirements/) — R0 scope, BRD/PRD |
| Shared libraries | `libs/bank-common-{error,domain,security,audit,observability,secrets}` — changes here affect every workstream and always require an Architecture verdict. `bank-common-domain` was added by EPIC-002 to hold the bank-owned model extracted out of `1sb-integration-service`. |
| Service estate | 21 services on the root build (`settings.gradle.kts`), scaffolded 2026-09-02 from the architecture catalogue. **A module's presence is not delivery of its bounded context** — each scaffolded service carries 2 test files. Do not read the module list as capability. |

---

## 5. Standing constraints (apply to every triage in this repo)

These are stable facts an agent must not re-derive or re-litigate:

1. **Bank apps never call 1SB or a database directly.** Topology is Bank → integration service
   → ports/adapters → 1SB; durable state via `bank-persistence-service` HTTP only.
2. **1SB specifics live only in `adapter.onesb.*`.** Enforced by ArchUnit.
3. **The integration service owns no Flyway migrations and no JPA.** (TD-011, closed — do not
   reintroduce.)
4. **Persistence is platform-common,** not 1SB-owned (TD-016/TD-017, closed).
5. **Flutter never receives OAuth tokens;** the BFF holds them (WS-2 decision 2).
6. **Keycloak is not the source of truth for business authorization** (WS-2 decision 5).
7. **No PII in logs.** Masking is a compliance gate, not a preference.
8. **Coverage gates:** libs line ≥ 80% / branch ≥ 70%; services on the interim floor
   ([COVERAGE.md](../1sb-insurance-integration/service-ssot/COVERAGE.md)).

A suggestion that violates a standing constraint is **SF4 / REJECT** unless it arrives as a
formal change request under [14](./14-CHANGE_CONTROL.md).

---

## 6. Known open debt affecting triage

Agents should recognise these so they do not re-report them as new findings. Full ledger:
[TECH-DEBT.md](../1sb-insurance-integration/service-ssot/TECH-DEBT.md).

| ID | Sev | Summary | Triage note |
|----|-----|---------|-------------|
| TD-006 | P2 | AWS Secrets Manager provider is a stub | Known. Re-reporting = duplicate, close as `DUPLICATE-OF TD-006`. |
| TD-007 | P3 | ArchUnit `allowEmptyShould(true)` | Known. |
| TD-009 | P2 | Missing domain ports vs architecture | Known. |
| TD-010 | P2 | No Redis idempotency/cache adapter | Known; in-memory accepted until Phase 5.4. |
| TD-014 | P2 | No WireMock/E2E for integration ↔ persistence | Overlaps Phase 4 gate item 4.1. |
| TD-022 | P1 | FUNC-008 payment intimation not implemented | Scheduled Phase 5.3. |
| TD-023 | P2 | Raw payload capture not wired for status/master-data | Known. |
| QA-001 | P0 | JaCoCo gates — **partial** | Libs done; service floor interim. |

> **Rule CS-2 — Duplicate suppression.** Before creating a `SUG-####`, an agent checks the debt
> ledger and [registers/SUGGESTION-REGISTER.md](./registers/SUGGESTION-REGISTER.md). A repeat
> finding is linked, not re-opened. Repeat findings *do* increment the item's
> `recurrence_count`, which is a priority input ([05 §4](./05-PRIORITY_MODEL.md#4-the-scoring-model)).

---

## 7. Ratification status

**Ratified 2026-08-10** by the Solution Architect. `provisional` is `false`; agents run the
full pipeline, including ADMIT, against this state.

**The 2026-09-11 refresh did not re-ratify it.** It could not: an agent in the R12 seat may compel
a decision to happen and may never supply its content (Rule PA-1). What the refresh establishes is
narrower and enough to lift the CS-1 halt — the stage, scope and objective values Mahesh signed on
2026-08-10 are **unchanged**, so there is nothing new to ratify, and the fields that did change are
freshness dates and per-criterion evidence, both owned by R12 under
[RUNBOOK §4.1](./RUNBOOK.md#41-live-project-state-l3--highest-decay-rate).

| Agent action | Allowed |
|--------------|---------|
| PARK an input with a target stage | ✅ Yes |
| REJECT an input that violates a standing constraint (§5) | ✅ Yes |
| ADMIT work for the current phase | ✅ Yes |
| Declare a stage transition | ❌ No — human only ([04 §5](./04-STAGE_GATES.md#5-who-may-declare-a-transition)) |
| Edit `current_phase` / `stage_status` | ❌ No — human only |

**Outstanding, with required-by dates set by R12 on 2026-09-11:**

| Owed | Owner | Required by | If it does not arrive |
|------|-------|-------------|-----------------------|
| PO counter-signature on GOV-004 and CR-001 | Rajal / Product | 2026-09-18 | Recorded as OVERDUE against Product at the next Governance Sync. It does not revert this file to provisional. |
| `review_due` re-ratification of the stage values | Mahesh / Architecture + Rajal / Product | 2026-10-11 | Agents lose ADMIT again on 2026-10-12 under Rule CS-1 |

R12 sets these dates and names the owner. R12 does not decide either matter, and neither date is a
default answer standing in for a signature. If the PO disagrees with any stage or scope value, that
is a `CR` against this file — not a reversion to provisional.

**Re-ratification** is required whenever a stage transitions, and at every `review_due`
(now 2026-10-11).

---

## 8. Maintenance

**Owner, cadence, and staleness limits:**
[RUNBOOK §4](./RUNBOOK.md#4-maintenance--staleness-matrix). This file and
`state/CURRENT-STATE.yaml` are the highest-decay artefacts in the repository — past 30 days they
do not merely age, they actively mislead, because agents trust them.

| Trigger | Action |
|---------|--------|
| Stage gate passed | Advance `current_phase`, reset `stage_status`, re-run [08 §5](./08-BACKLOG_RULES.md#5-unparking) unpark sweep |
| Scope changed via CR | Update [02-PROJECT_SCOPE.md](./02-PROJECT_SCOPE.md) **and** this file |
| New workstream starts | Add a WS block here + a `workstreams[]` entry in the YAML |
| `review_due` passed | Delivery Lead refreshes or explicitly extends; agents warn until refreshed |
| Refresh finds no stage change | Advance `state_as_of` and `review_due`, record the delta in §4.1 and the per-criterion evidence in `GATE-EVIDENCE.yaml`. A date bump with no sweep behind it is the failure mode Rule FR-1 exists to prevent |
