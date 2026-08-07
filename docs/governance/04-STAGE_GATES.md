# 04 — Stage Gates

**Layer:** L1 (gate mechanics) + L3 (criteria per stage)
**Owner:** Architect (technical exit) · PO (business exit) · Delivery Lead (cadence)

---

## 1. What a gate is

A stage gate is a **written, checkable list of exit criteria**. It exists so that:

- "are we done with this stage?" has an answer that is not a feeling;
- parked work has an observable trigger to come back ([03 §3](./03-LIFECYCLE.md#sf3-carries-three-mandatory-fields));
- an agent can tell whether an input is SF0 (blocks a criterion) or merely SF1.

> **Rule G-1 — If it does not appear in a gate criterion or an in-flight work item's acceptance
> criteria, it is not blocking.** Agents claiming "this is blocking" must name the criterion.

---

## 2. Gate anatomy

```yaml
gate:
  stage: "Phase 4 — Hardening & consumer enablement"
  entry_criteria: [...]       # what must be true to start the stage
  exit_criteria: [...]        # what must be true to leave it
  evidence_required: [...]    # artefact per criterion — not an assertion
  approvers: [...]            # roles that sign the transition
  parked_items_released: [...]# items whose unpark_trigger is this transition
```

Each exit criterion needs **evidence**, and evidence is an artefact: a CI run, a test report, a
signed document, a merged PR, a dashboard link. "Confirmed verbally" is not evidence.

---

## 3. Gate states

| State | Meaning | Agent behaviour |
|-------|---------|-----------------|
| `OPEN` | Stage in progress, criteria incomplete | Normal triage |
| `CANDIDATE` | All criteria claim done, evidence under review | Freeze on non-P1 ADMITs; finish in-flight work |
| `PASSED` | Approvers signed | Run the unpark sweep; advance current state |
| `BLOCKED` | A criterion cannot be met (external dependency, decision missing) | Blocking item becomes P1; other work continues in dependency order |

**Freeze rule:** in `CANDIDATE`, only SF0 and P1-override work is admitted. Everything else is
parked to the next stage — this is what stops a stage from being extended indefinitely by
late-arriving good ideas.

---

## 4. Transition procedure

```text
1. Delivery Lead marks gate CANDIDATE
2. For each exit criterion: attach evidence artefact
3. Required approvers review (see §6 per stage)
4. Any REWORK  → gate returns to OPEN with named blocking items (P1/P2)
5. All APPROVE → gate PASSED
6. Update state/CURRENT-STATE.yaml: current_phase, stage_status, next_stage
7. Run unpark sweep (08 §5): every parked item whose unpark_trigger matched is re-triaged
   *from step 2 of the pipeline* — not auto-admitted
8. Record the transition in registers/DECISION-REGISTER.md
```

Step 7 is deliberate: parked items are **re-evaluated**, not auto-promoted. A suggestion parked
six months ago may have been superseded, made obsolete, or already solved.

---

## 5. Who may declare a transition

| Actor | May mark CANDIDATE | May mark PASSED |
|-------|--------------------|-----------------|
| AI agent | ✅ (with evidence table) | ❌ **never** |
| Delivery Lead | ✅ | ❌ |
| Architect + PO (jointly) | ✅ | ✅ |

An agent that believes a gate is complete produces the evidence table and says so. It does not
edit `current_phase`.

---

## 6. Project gates (L3)

### WS-1 · Phase 4 → Phase 5 — Hardening & consumer enablement

**Status:** `OPEN`

| # | Exit criterion | Evidence required | State |
|---|----------------|-------------------|-------|
| 4.1 | Sandbox E2E suite for the Term path runs in CI (or gated nightly) | Green CI job link + suite location | ❌ Open |
| 4.2 | OpenAPI published to the internal portal; consumer collection available | Portal URL + collection file | 🟡 Partial — OpenAPI generated (`79c65f4`); publication + collection outstanding |
| 4.3 | ≥ 1 bank caller exercises quote + proposal against UAT | Consumer confirmation + UAT trace/correlation IDs | ❌ Open |
| 4.4 | Compliance review of audit schema + log samples | Signed review note in `service-ssot/` | ❌ Open |
| 4.5 | Runbook: secrets rotation, IP whitelist, 1SB 401/5xx incident | Runbook document | ❌ Open |
| 4.6 | Performance smoke: p95 quote under nominal concurrency | Measurement report + threshold | ❌ Open |
| 4.7 | Coverage gates green; QA-001 closed or explicitly waived with expiry | JaCoCo report + TECH-DEBT entry | 🟡 Partial — libs at 80/70; service on interim floor |

**Approvers:** Architect · PO · QA Lead · Compliance (4.4) · Ops (4.5)

**Parked items released on PASS:** every entry in
[registers/PARKED-BACKLOG.md](./registers/PARKED-BACKLOG.md) with
`unpark_trigger: "entry to Phase 5"` — currently TD-022 (FUNC-008 payment intimation),
TD-010 (Redis idempotency, per ACTION-PLAN 5.4).

**Entry criteria for Phase 5** (must hold before LOB expansion starts):
Term happy path green against sandbox; Term regression suite exists; no open P1 debt on the
Term path.

### WS-1 · Phase 5 → Phase 6 — Expand LOBs

**Status:** `not started`

| # | Exit criterion | Evidence |
|---|----------------|----------|
| 5.1 | Health sandbox path green, reusing `QuoteService` orchestration unchanged | CI + diff showing no orchestration change |
| 5.2 | Motor sandbox path green (separate schemas, shared poller/HTTP) | CI |
| 5.3 | Term regression still green | CI |
| 5.4 | Redis idempotency / multi-instance job ownership before scale-out | Design + tests (closes TD-010) |
| 5.5 | Circuit breaker in place; `consentRef` mandatory | Tests + compliance note |
| 5.6 | FUNC-008 payment intimation delivered or re-parked with reason | Backlog state (closes/rolls TD-022) |

### WS-1 · Phase 6 — Production readiness

| # | Exit criterion | Evidence |
|---|----------------|----------|
| 6.1 | Prod credentials, IP whitelist, distributorId, TLS egress verified | Ops checklist |
| 6.2 | Dashboards + alerts (auth failure, poll timeout, upstream 5xx, p95) | Dashboard links + alert rules |
| 6.3 | Retention job for raw payloads; backup/restore exercised | Job + restore test record |
| 6.4 | Go-live checklist signed by security, compliance, product | Signed checklist |
| 6.5 | Hypercare plan: error budget, 1SB escalation contact, rollback plan | Runbook |
| 6.6 | DR test executed | Test record (this is where the parked DR item lands) |

### WS-2 · Phase 1 → Phase 2 — Workforce IAM foundation

**Status:** `OPEN`

| # | Exit criterion | Evidence |
|---|----------------|----------|
| A.1 | BFF token-hiding session proven: Flutter never receives OAuth tokens | Test + code review evidence |
| A.2 | Keycloak isolated behind `identity-provider-adapter-service`; no provider types leak | Arch test / review |
| A.3 | `identity-authorization-service` is the PDP; default-deny verified | Policy evaluation tests incl. negative cases |
| A.4 | Maker-checker enforced for bulk + privileged changes | Tests |
| A.5 | Auth + admin events retained per policy; retention configurable | Config + compliance note |
| A.6 | Provisioning outbox delivers reliably (retry, idempotency) | Tests |

**Approvers:** Architect · Security Architect (**mandatory**) · Compliance (A.5) · QA Lead

---

## 7. Gate criteria as triage input

The gate table is the fastest way to settle "is this SF0?":

```text
Does the input make a currently-failing exit criterion pass?      → SF0
Does it serve the stage deliverable but no specific criterion?    → SF1
Does it serve the NEXT stage's criteria?                          → SF2 (apply absorption test)
Does it serve a criterion two or more stages out?                 → SF3, target = that stage
Does it serve no criterion in any listed gate?                    → SC2/SC3 territory, not SF
```
