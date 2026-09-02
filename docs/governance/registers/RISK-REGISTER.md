# Risk Register

Open risks with owners, triggers, and responses. A risk is not a problem — it is a **problem
with a probability**. Risks feed the `R` factor in priority scoring
([05 §4](../05-PRIORITY_MODEL.md#4-the-scoring-model)) and the Risk & Compliance board.

**Owner:** Delivery Lead (register) · named owner per risk
**Reviewed:** at every stage gate

---

## 1. Scoring

`Exposure = Likelihood × Impact`, each 1–3.

| Likelihood | | Impact | |
|---|---|---|---|
| 1 | Unlikely in this stage | 1 | Contained; recoverable in hours |
| 2 | Plausible | 2 | Material; delays a gate or degrades a journey |
| 3 | Expected without action | 3 | Severe; data loss, breach, regulatory finding, or go-live block |

| Exposure | Response |
|----------|----------|
| 1–2 | Accept and monitor |
| 3–4 | Mitigate this stage |
| 6 | Mitigate now — usually a P1/P2 work item |
| 9 | Escalate to the PO and Architect immediately |

---

## 2. Open risks

| ID | Risk | WS | L | I | Exp | Owner | Response | Trigger to escalate |
|----|------|----|---|---|-----|-------|----------|---------------------|
| RISK-003 | Bank AD technology unconfirmed (DEP-010) | WS-2 | 2 | 3 | 6 | PO + Architect | Adapter design must remain federation-agnostic; do not pre-commit | Phase 1 gate PASSED with no confirmation |
| RISK-004 | In-memory idempotency is unsafe multi-instance (TD-010) | WS-1 | 2 | 3 | 6 | Tech Lead | Single-instance constraint documented; Redis scheduled Phase 5.4 | Any plan to run > 1 instance |
| RISK-005 | AWS Secrets Manager provider is a stub; prod profile fails fast (TD-006) | WS-1 | 2 | 3 | 6 | Platform | Keep fail-fast; schedule at Phase 6 | AWS deployment target confirmed |
| RISK-006 | Service coverage is on an interim floor (QA-001 partial) | WS-1 | 2 | 2 | 4 | QA Lead | Close or waive with expiry at gate 4.7 | Phase 4 gate CANDIDATE with 4.7 unresolved |
| RISK-007 | Raw payload capture incomplete for status / master-data (TD-023) | WS-1 | 2 | 2 | 4 | Tech Lead | Scope decided by compliance review 4.4 | Compliance requires full capture |
| RISK-008 | No QA Engineer / QA Lead cycle ran for Phase 4 stories (single-agent branch) | WS-1 | 2 | 2 | 4 | QA Lead | Recorded variance in `phase-4/STATUS.md`; QA pass before UAT sign-off | UAT exposure without a QA cycle |
| RISK-009 | 1SB sandbox instability could stall E2E in CI (gate 4.1) | WS-1 | 2 | 2 | 4 | Eng | Gated nightly fallback already sanctioned by ACTION-PLAN 4.1 | E2E flakiness blocks the pipeline |
| RISK-010 | Governance adopted mid-flight; historical work never passed AIGEM gates | Both | 3 | 1 | 3 | Delivery Lead | Deliberate ([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)) — forward-only adoption | An incident traces to un-triaged historical work |
| RISK-012 | The five 2026-08-24 platform layers (`ADR-009`…`ADR-013`) raise R0 **fixed** cost above the level the S09 budget line assumed, at ~100 journey starts an hour — so the estate is priced for availability and evidence while the business case is priced for a pilot | WS-3 | 3 | 2 | 6 | Shivanshi / SRE + Kalpana / R12 | Per-environment shapes cap it (`R0-LLD` §1.4): `dev` is deliberately not production-shaped. Envelope produced at S09 as `NFR-OPEN-6` **before** first `apply` to `uat`. A shape is never lowered in `prod` to fit a cost conversation without a Security and SRE verdict | Cost envelope not produced before `GATE-S09` entry, or a cost conversation proposing to drop a `prod` control |
| RISK-013 | Bank-side connectivity work (`DEP-20260824-dx1`) does not land, so `uat` keeps running against CBS and Bank AD stubs and `#4` Customer plus WS-2 Phase 2 cannot be evidenced | WS-3 | 2 | 3 | 6 | Shivanshi / SRE + bank network | VPN before Direct Connect, so the path needs a firewall rule rather than a carrier order; `dev` stubs stay legitimate; chase date on the dependency row | A UAT date is set while the VPN half is still unconfirmed |
| RISK-014 | Operational surface outruns team maturity: three stateful managed services (broker, cache, search), a firewall rule set and two circuits arrive while `GATE-S08` is still open and no service has run in a real environment | WS-3 | 3 | 2 | 6 | Shivanshi / SRE | Managed services only — nothing self-hosted (`R0-LLD` §4.1); shapes sized for availability, not throughput; the outbox keeps a broker outage to a delay rather than a loss; every new tier has a named runbook and a drill in the `P8` proof band | A tier reaches `uat` without its runbook and its drill, or an incident is resolved by disabling a control |
| RISK-015 | Invariant erosion under incident pressure: the cache becomes an idempotency or evidence store, a topic becomes the audit record, or the search index becomes the queried source of truth — each of which looks like a fix at 03:00 | WS-3 | 2 | 3 | 6 | Mahesh / Architecture + Deepali / Security | The forbidden lists are machine checks, not conventions: `FF-23`, `FF-24`, `FF-26`, `FF-27`, `FF-28`. `ADR-011`/`ADR-012`/`ADR-013` each name the temptation explicitly so it is recognised rather than rediscovered | Any proposal to serve configuration past TTL from cache, to extend topic retention for audit purposes, or to answer a compliance query from the index |
| RISK-016 | No delivered behaviour traces to a signed business requirement, so IRDAI CA0515 evidence (requirement → AC → test) cannot be produced and UAT/compliance sign-off has nothing to accept against | Both | 3 | 3 | **9** | PO | Requirements Traceability Matrix + retro-fit of the five built services ([CR-014](./DECISION-REGISTER.md#cr-014--process-realignment-dual-track-recovery)) | **Already at 9 — escalated to PO and Architect via the war room.** Escalate further if CR-014 is rejected or deferred past the next sprint |
| RISK-017 | The platform product has no governance workstream, so corrective requirement work is forced to SC2/PARK ([02 §3](../02-PROJECT_SCOPE.md#3-scope-fit-codes-l1-generic)) and the build keeps outrunning the signed scope | Both | 3 | 2 | 6 | Delivery Lead | Add WS-0 to `CURRENT-STATE.yaml` (CR-014 item 1) — human-only edit | A second flow is built with no requirement ID after CR-014 is decided |

## 3. Accepted risks

Risks knowingly carried, with the acceptance recorded so they are not re-raised as findings.

| ID | Risk | Accepted by | Until | Why acceptable |
|----|------|-------------|-------|----------------|
| → [RISK-004](#2-open-risks) | In-memory idempotency | Tech Lead | Phase 5.4 | Single instance in UAT; scale-out is gated. **Unchanged by the platform cache tier** — `ADR-011` keeps idempotency in the owning store, so a shared cache existing does not close this |
| → [RISK-010](#2-open-risks) | Forward-only governance adoption | Delivery Lead | — | Backfilling costs days and changes no shipped code |

> `RISK-012` … `RISK-017` are **not** accepted. They are open against a decision set that is
> AI-DRAFTED, and each names the human who has to accept or reject it. An agent recording a risk
> against its own proposal does not thereby carry it.

## 4. Closed risks

| ID | Risk | Closed | How |
|----|------|--------|-----|
| RISK-001 | Current-state file unratified; agents may triage against a wrong stage | 2026-08-10 | Ratified by the Solution Architect (GOV-004); `provisional: false`. `FreshnessCheck` now halts if the state goes stale, so the risk cannot silently return |
| RISK-002 | External UAT dependency (DEP-002) had no owner or date | 2026-08-16 | Assigned to Rajal / Product with follow-up on 2026-08-21 in the dependency and gate-evidence registers |

---

## 5. Raising a risk

```yaml
risk:
  id: RISK-011
  statement: "If X happens, then Y, causing Z"     # not "X is bad"
  workstream: WS-1
  likelihood: 2
  impact: 3
  exposure: 6
  owner: "named person or role"
  response: MITIGATE          # ACCEPT | MITIGATE | TRANSFER | AVOID
  mitigation: "the work item that reduces it"
  escalation_trigger: "the observable event that makes this urgent"
  review_at: "Phase 4 gate"
```

A risk without an owner and an escalation trigger is a worry, not a risk. Worries do not belong
in the register.
