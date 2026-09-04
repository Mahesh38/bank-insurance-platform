# BOOT — the tier-0 agent capsule

> **Read this first, and often read nothing else.** This file answers the ten-fact knowledge
> contract ([`RUNBOOK.md` section 8.1](../governance/RUNBOOK.md#81-the-knowledge-contract--ten-facts-before-acting))
> with *answers*, not with pointers to 90 KB of source. Its volatile half is **generated** from
> [`CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml) and CI fails when it drifts.
>
> Canonical on conflict: `docs/governance/` always wins over this capsule.

**Next step after this file:** resolve your task to one capsule in
[`AGENT-CONTEXT-INDEX.yaml`](./AGENT-CONTEXT-INDEX.yaml) and read *only* what it lists.

```bash
python3 scripts/context/context-load.py resolve "review the security of the payment callback"
```

Need one specific fact rather than a task capsule — a field's mandatory rule, one stage's evidence,
one extracted schema? Query the document map instead of exploring:

```bash
python3 scripts/context/context-load.py find "premium field mapping"
```

---

## 1. The five behaviours that decide whether you are useful here

1. **Triage before implementing.** A suggestion is *never* implemented in the turn it is raised —
   triage it, record it, schedule it, then return to the work item you were on.
2. **One work item in flight per executor lane.** Only an evidenced
   [hard `P1` override](../governance/05-PRIORITY_MODEL.md#3-hard-p1-overrides) interrupts:
   build failure · exploitable vulnerability · incorrect domain model · missing mandatory API ·
   regulatory violation · data corruption · blocking dependency · AC failure.
3. **Park with a target stage and an unpark trigger.** Never "someday". Parked is not deleted.
4. **Return to the task, out loud.** End every triage with `Continuing with <item>.`
5. **Report honestly.** Partial is partial, red gates are red, drift is reported not hidden.

## 2. Priority is stage-relative

The same item is `P4` during hardening and `P1` at production readiness. Always record
`priority_now` **and** `priority_at_target`. Read your workstream's posture row (section 4)
before every triage — your instincts do not change on their own; the row does it for them.

## 3. Hard boundaries on agent behaviour

- Agents **never** edit stage state (`current_phase`, `stage_status`) in `CURRENT-STATE.yaml`.
- Agents **never** approve a change request, and **never** self-approve a board that requires a human.
- **T4 human sign-offs** (Architecture, Security, Risk & Compliance) cannot be satisfied by AI
  simulation. An agent may draft the reasoning and assemble evidence; it may not manufacture the
  signature, a legal interpretation, production authority or material risk acceptance.
- No persona silently crosses an authority boundary →
  [Authority Quick Card](./personas/AUTHORITY-QUICK-CARD.md).
- **Do not scale blindly.** Name the business load, the amplification, the *actual* bottleneck,
  the next downstream limit, the safe range and the recovery behaviour. More pods are not a diagnosis.
- Every `TODO` carries a work item ID. Nothing is Done without evidence.
- Do not re-report known debt (section 5) and do not re-propose
  [parked items](../governance/registers/PARKED-BACKLOG.md).

## 4. Posture by lifecycle stage — the row that changes the verdict

| Stage | Default posture | Bias toward | Reject on sight |
|---|---|---|---|
| **L0–L1** Discovery / business design | Ask, don't build | Clarity, written rules | Any code, any technology choice |
| **L2** Domain design | Model concepts only | Invariants, state models, language | Persistence tuning, messaging, caching, observability stacks |
| **L3** Technical design | Decide and record | Contracts, boundaries, ADRs | Premature production tuning |
| **L4** Foundation | Build the floor, thinly | Scaffold, CI, arch tests, secrets | Feature breadth, generic frameworks |
| **L5** Connectivity | Talk to the outside safely | Clients, auth, error normalisation | Second LOB, expansion |
| **L6** Vertical slice / MVP | One path, all the way through | Depth over breadth | Generalisation, abstractions with one implementation |
| **L7** Hardening | Prove it, don't extend it | Tests, evidence, runbooks, **debt repayment** | New features, new LOBs, new infrastructure |
| **L8** Expansion | Reuse, don't rebuild | The *same* orchestration, second journey | Rearchitecting what works |
| **L9** Production readiness | Assume it will break | Dashboards, alerts, DR, retention, go-live checklist | Broad new scope |
| **L10** Operate & evolve | Measure, then change | SLOs, incident learning | Unbounded rewrites |

Full table with the wrong-instinct column:
[`RUNBOOK.md` section 8.3](../governance/RUNBOOK.md#83-how-the-agents-thinking-must-change-at-each-stage).

## 5. Current state — facts 1–9

<!-- BEGIN GENERATED: current-state -->

> Generated from [`CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml) by `scripts/context/build-boot-capsule.py`. Do not hand-edit this block.

**AIGEM 1.4** · state as of **2026-08-10** · review due **2026-09-09** · provisional: **no**

> **Fact 9 — freshness.** Past `review_due`, an agent may park and reject but **must not admit new work** (Rule CS-1). Run `java scripts/governance/FreshnessCheck.java` and act on the exit code: `0` fresh · `1` warn, disclose it · `2` do not admit.

### WS-3 — AU Bank Insurance Distribution Platform

**Stage:** S08 — Engineering Foundation · `IN_PROGRESS`  
**Phase:** Foundation Recovery Increment — S08 with S09 overlapped  
**Next:** S09 — Platform & Environment Foundation

**Objective** (`R0-ASSISTED-TERM-SALE`): One RM sells one Term Life policy to one ETB customer from one Group A insurer, end to end, through a real interface, with consent and suitability evidence, payment on the customer's own device, an issued and reconciled policy, and a complete audit trail.

**Open gate:** `GATE-S08` · state `OPEN` · 10 of 10 exit criteria still open
- `S08-G1` **OPEN** — CI builds and tests every module on every PR · Amit / Engineering
- `S08-G2` **OPEN** — Merge to main impossible without a green pipeline · Amit / Engineering
- `S08-G3` **OPEN** — Coverage thresholds enforced; QA-001 closed · Swapnali / QA
- `S08-G4` **OPEN** — ArchUnit and static analysis enforced · Amit / Engineering
- `S08-G5` **OPEN** — Secret, SAST, SCA and image scanning in the pipeline · Deepali / Security
- `S08-G6` **OPEN** — Test infrastructure operational at every pyramid level · Swapnali / QA
- `S08-G7` **OPEN** — No PII in logs, proven by automated test · Deepali / Security
- `S08-G8` **OPEN** — Engineering and secure coding standards published and adopted · Amit / Engineering
- `S08-G9` **OPEN** — Pipeline feedback under 10 minutes at p95; flake under 1% · Shivanshi / SRE
- `S08-G10` **OPEN** — A new engineer can build, test and ship in under a week · Amit / Engineering

**Out of scope now — do not propose, do not build:**
- Customer self-service (DIY) journey — revisit at R1 — after the assisted journey completes a real sale in pilot
- Hybrid journey and assisted/DIY mode switching — revisit at R2 — after assisted and DIY both have stable state and hand-off contracts
- Group B insurers: catalogue entry and controlled redirect — revisit at R1
- ULIP and Savings/Endowment product classes — revisit at R1
- Customer BFF (context #1) and the customer-facing Flutter surface — revisit at R1, with DIY
- Notification service (context #17) beyond OTP and payment-link delivery — revisit at R1
- Lead campaign and bulk origination (not single-RM create, not MIS policy ingest) — revisit at R1
- Renewals and servicing — revisit at R2+
- Bounded contexts not listed in in_scope — revisit at S13 — justified by the working slice, not by the diagram
- Health, Motor, Travel and other non-life LOBs — revisit at R2+, and only after WS-1 Phase 5 is unfrozen
- New-to-Bank onboarding and V-KYC — revisit at R2+
- Multi-aggregator routing — revisit at evidence of a second aggregator commitment; extensibility only
- Branch kiosk journey — revisit at pending business decision (GAP-033)
- Vernacular / multi-language content — revisit at R1 — hi-IN first
- Consolidated executive control tower — revisit at R2, after the funnel produces real data

**Never in this workstream:**
- A quote generated without a valid suitability evaluation id
- Payment executed on an RM or bank-employee device
- distributorId sourced from a caller-supplied value
- Consent recorded without a verified customer-device OTP
- Mutable or deletable consent, suitability or audit evidence
- Regulated data, backups, logs or archives outside AWS India regions
- Policy Sold inferred from quote, proposal or payment alone
- Claims administration or insurer underwriting decisioning
- An insurer or aggregator API defining the bank's canonical journey
- Bank apps or the Flutter client calling 1SB or a database directly
- An agentic-AI action substituting for a deterministic hard gate

### WS-1 — 1SB Insurance Integration

**Stage:** L7 — Hardening · `IN_PROGRESS`  
**Phase:** Phase 4 — Hardening & consumer enablement  
**Next:** Phase 5 — Expand LOBs (Health → Motor)

**Objective** (`P4-UAT-SIGNOFF`): Term path signed off for UAT use by at least one bank caller

**Open gate:** `GATE-P4` · state `BLOCKED` · 7 of 7 exit criteria still open
- `4.1` **BLOCKED** — Sandbox E2E suite for the Term path runs in CI (or gated nightly) · Amit / Engineering + R10 / Operations · blocked by GATE-4.1-SANDBOX-E2E
- `4.2` **PARTIAL** — OpenAPI published to internal portal; consumer collection available
- `4.3` **BLOCKED** — At least one bank caller exercises quote + proposal against UAT · Rajal / Product · blocked by DEP-001, DEP-002
- `4.4` **OPEN** — Compliance review of audit schema and log samples
- `4.5` **OPEN** — Runbook: secrets rotation, IP whitelist, 1SB 401/5xx incident
- `4.6` **BLOCKED** — Performance smoke: p95 quote under nominal concurrency · Amit / Engineering + R10 / Operations · blocked by DEP-003
- `4.7` **BLOCKED** — Coverage gates green; QA-001 closed or explicitly waived with expiry · Swapnali / QA · blocked by S08-G3

**Out of scope now — do not propose, do not build:**
- Kafka / event backbone — revisit at Integration architecture stage
- Health and Motor LOB handlers — revisit at Phase 5
- Redis idempotency / multi-instance job ownership — revisit at Phase 5.4
- FUNC-008 payment intimation — revisit at Phase 5.3
- Circuit breaker, mandatory consentRef — revisit at Phase 5.5
- Dashboards, alerting, SLOs — revisit at Phase 6
- Autoscaling, DR testing, retention jobs, backup/restore — revisit at Phase 6
- Saving / Annuity / Pension LOBs — revisit at Phase 6+
- Provider routing flag / fake adapter (E13) — revisit at Phase 6+
- Persistence performance optimisation — revisit at evidence-driven only
- Reactive rewrite / SDK framework — revisit at never, absent evidence

**Never in this workstream:**
- Bank apps calling 1SB or the database directly
- Flyway or JPA inside 1sb-integration-service
- A second audit database
- 1SB types outside adapter.onesb.*

### WS-2 — Workforce Authentication & Authorization

**Stage:** L4/L6 — Foundation into first vertical slice · `IN_PROGRESS`  
**Phase:** Phase 1 — Foundation implementation  
**Next:** Phase 2 — Bank AD federation + production IdP decision

**Objective** (`IAM-P1`): Provider-neutral workforce identity: token-hiding BFF session, Keycloak behind an adapter, business authorization service as the PDP.

**Open gate:** `GATE-IAM-P1` · state `OPEN` · 6 of 6 exit criteria still open
- `A.1` **OPEN** — BFF token-hiding proven: Flutter never receives OAuth tokens
- `A.2` **OPEN** — Keycloak isolated behind identity-provider-adapter-service
- `A.3` **OPEN** — identity-authorization-service is the PDP; default-deny verified
- `A.4` **OPEN** — Maker-checker enforced for bulk and privileged changes
- `A.5` **OPEN** — Auth and admin events retained per policy; retention configurable
- `A.6` **OPEN** — Provisioning outbox delivers reliably (retry, idempotency)

**Out of scope now — do not propose, do not build:**
- Retail-customer authentication — revisit at later bounded context
- Production IdP selection (Cognito vs Keycloak vs other) — revisit at Phase 2 — deliberately deferred behind the adapter
- Bank AD federation specifics (OIDC vs SAML vs LDAP) — revisit at Phase 2 — technology unconfirmed

**Never in this workstream:**
- Exposing Keycloak, Cognito, or AD directly to Flutter
- Sending OAuth access or refresh tokens to Flutter
- Treating the IdP as the source of truth for business authorization

### Standing constraints — never violate, in any workstream

- Bank apps never call 1SB or a database directly
- 1SB specifics live only in adapter.onesb.* (ArchUnit enforced)
- 1sb-integration-service owns no Flyway migrations and no JPA
- bank-persistence-service owns only the integration job store and audit ingest; each bounded context owns its own schema and Flyway (ADR-019)
- Flutter never receives OAuth tokens; the BFF holds them
- Keycloak is not the source of truth for business authorization
- No PII in logs
- Coverage gates: libs line >= 80% / branch >= 70%; services on interim floor
- No quote is produced without a valid, unexpired suitability assessment
- No proposal is submitted without an unexpired consent grant
- Premium payment executes only on the customer's device; no API path issues a payment link into an RM session
- A policy is never issued against a payment that is not RECONCILED
- No platform service calls a provider adapter directly; provider traffic routes through the Integration Hub
- Journey Orchestration holds stage and references only, never another context's business decision
- Render.com is dev-preview only and is never a data path for PII or production-like data

### Known open debt — **fact 7: do not re-report these**

`TD-006` · `TD-007` · `TD-009` · `TD-010` · `TD-014` · `TD-022` · `TD-023` · `QA-001`  
Detail: [`01-CURRENT_STATE.md` section 6](../governance/01-CURRENT_STATE.md#6-known-open-debt-affecting-triage).

<!-- END GENERATED: current-state -->

## 6. Fact 10 — what you were doing before this input arrived

> Fact 10 is the one agents lose, and the one that matters most. Everything else determines the
> verdict; fact 10 determines whether the project gets finished.

Name your current work item before you answer anything else, and return to it by name.

## 7. Session start and session end

```text
START  [ ] java scripts/governance/FreshnessCheck.java   → act on the exit code
       [ ] read this file (facts 1–9) — the state file only if you need a field it omits
       [ ] registers/PARKED-BACKLOG.md    → do not re-propose parked items
       [ ] section 5 known debt           → do not re-report it
       [ ] name the one work item for this lane; confirm it heads the READY queue

END    [ ] current item: done / in-flight with a snapshot / blocked with a blocker ID
       [ ] every suggestion raised this session is in a register
       [ ] drift incidents and their resolution recorded
       [ ] evidence attached for anything claimed Done
       [ ] registers and backlog updated; TODOs carry IDs
       [ ] uncommitted work committed or explicitly flagged
```

## 8. Answer shapes — use them verbatim

```text
TRIAGE
SUG-00NN · "<the suggestion>"
Stage: <phase> — <fits / belongs to X>     Scope: <SC code>
Necessity: <MUST|SHOULD|COULD|NOT-NOW>     Verdict: <ADMIT|PARK|REJECT|ESCALATE>
Priority: P<n> now · P<n> at target        Recorded: <register file>
Continuing with <current work item>.

CONSEQUENTIAL DECISION
project · problem/outcome · current facts · assumptions · affected authority
options · recommendation · evidence · unresolved owner/date · next safe action
```

| Situation | Say |
|---|---|
| Suggestion mid-task | "Noted as `SUG-00NN` (parked, Phase 5). Continuing `FUNC-011`." |
| Genuine P1 | "Interrupting `FUNC-011` for a P1. Snapshot recorded; returning after." |
| You drifted | "I drifted: changed an adjacent component. Reverting, registering as `SUG-00NN`, finishing the current item." |
| State is stale | "`CURRENT-STATE.yaml` is past `review_due`. I can park and reject against it, but not admit new work. **Kalpana / R12** needs to refresh it." |
| Asked to skip the process | "Understood — doing it directly. Recording the bypass and its one risk." |
| Item bigger than planned | "Larger than the plan — stopping to re-review rather than expanding scope." |
| Asked to scale production | "I'll first identify the business load, real bottleneck and downstream limit; scaling a non-bottleneck can worsen the incident." |

## 9. Where to go next — nothing else is loaded by default

| You need | Load |
|---|---|
| The exact files for your task | [`AGENT-CONTEXT-INDEX.yaml`](./AGENT-CONTEXT-INDEX.yaml) |
| The one document holding a specific fact | `context-load.py find "<the fact>"` → [`DOC-MAP.yaml`](./DOC-MAP.yaml) |
| To triage an input | [`.claude/skills/aigem-triage/SKILL.md`](../../.claude/skills/aigem-triage/SKILL.md) |
| To act as a persona | [`personas/`](./personas/README.md) — a card, not a package |
| Who decides / who cannot | [`personas/AUTHORITY-QUICK-CARD.md`](./personas/AUTHORITY-QUICK-CARD.md) |
| Where a document lives | [`docs/README.md`](../README.md) |
| The binding agent contract | [`09-AI_EXECUTION_RULES.md`](../governance/09-AI_EXECUTION_RULES.md) |
| A state field this capsule omits | [`CURRENT-STATE.yaml`](../governance/state/CURRENT-STATE.yaml) |
