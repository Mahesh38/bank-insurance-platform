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
| State as of | 2026-08-10 |
| Ratified by | **Mahesh (Solution Architect), 2026-08-10** (GOV-004) — PO counter-signature outstanding |
| Active workstreams | 2 (see §4) |
| Governance version | AIGEM 1.0 |

> This snapshot was reconstructed from repository artefacts (`ACTION-PLAN.md`, phase
> `STATUS.md` files, `TECH-DEBT.md`, git history) and **ratified by the Solution Architect on
> 2026-08-10** ([GOV-004](./registers/DECISION-REGISTER.md#2-governance-decisions)). Agents run
> the full pipeline against it. The PO counter-signature is a recorded formality, not a
> restriction — see §7.

---

## 4. Workstreams

This repository carries more than one lifecycle at once. **Stage fit is evaluated against the
workstream the input belongs to, never against the repository as a whole.** A suggestion about
Keycloak session handling is on-stage for WS-2 and premature for WS-1.

### WS-1 · 1SB Insurance Integration

| Field | Value |
|-------|-------|
| **Current phase** | Phase 4 — Hardening & consumer enablement |
| **Stage status** | IN_PROGRESS (partial) |
| **Current objective** | Term path signed off for UAT use by at least one bank caller |
| **Deliverable** | Hardened Term vertical slice: sandbox E2E evidence, published OpenAPI, compliance-reviewed audit schema, runbook, performance smoke |
| **Completed** | Phases 0–3. Term path FUNC-001…007, FUNC-009 delivered and TL-approved (`phase-3/STATUS.md`, `phase-4/STATUS.md`). COMP-003 raw-payload encryption, JaCoCo gates, Docker packaging, OpenAPI publication landed (commit `79c65f4`). |
| **Not yet done in this phase** | Sandbox E2E suite in CI (4.1), bank consumer spike (4.3), compliance review of audit schema (4.4), ops runbook (4.5), performance smoke (4.6) |
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
| **Next stage** | Phase 2 — Federation with bank AD + production provider decision |
| **Authority** | [authentication-authorization/README.md](../authentication-authorization/README.md) |
| **Explicitly out of scope, Phase 1** | Retail-customer authentication; production IdP selection (deferred behind the adapter) |

### Cross-cutting

| Field | Value |
|-------|-------|
| Platform architecture baseline | [architecture-review/](../architecture-review/) — target microservices, comms patterns, AWS infra, data, security/NFR, ADR log |
| Business requirement baseline | [au-bank-insurance-platform/requirements/](../au-bank-insurance-platform/requirements/) — R0 scope, BRD/PRD |
| Shared libraries | `libs/bank-common-{error,security,audit,observability,secrets}` — changes here affect both workstreams and always require an Architecture verdict |

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

| Agent action | Allowed |
|--------------|---------|
| PARK an input with a target stage | ✅ Yes |
| REJECT an input that violates a standing constraint (§5) | ✅ Yes |
| ADMIT work for the current phase | ✅ Yes |
| Declare a stage transition | ❌ No — human only ([04 §5](./04-STAGE_GATES.md#5-who-may-declare-a-transition)) |
| Edit `current_phase` / `stage_status` | ❌ No — human only |

**Outstanding:** PO counter-signature on GOV-004 and CR-001, recorded at the next gate review.
If the PO disagrees with any stage or scope value, that is a `CR` against this file — not a
reversion to provisional.

**Re-ratification** is required whenever a stage transitions, and at every `review_due`
(currently 2026-09-09).

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
