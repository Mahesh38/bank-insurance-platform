# Suggestion Register

**Every input that could become work gets a row here — admitted, parked, rejected, or escalated.**
Nothing is dropped. Nothing is deleted.

**Owner:** whichever agent or person triaged the input
**ID format:** `SUG-<YYYYMMDD>-<3 chars from 0-9a-z>` — collision-resistant, no shared counter.
Rules: [../state/CURRENT-STATE.yaml](../state/CURRENT-STATE.yaml) `id_allocation`
**Rules:** [08-BACKLOG_RULES.md](../08-BACKLOG_RULES.md) · [09-AI_EXECUTION_RULES.md](../09-AI_EXECUTION_RULES.md)

---

## How to add a row

1. Mint an ID: `SUG-<today>-<3 random chars>`, e.g. `SUG-20260812-a1b`. No counter to
   increment and no merge conflict when two branches triage at once.
2. Run pipeline steps 2–5 ([09 §2](../09-AI_EXECUTION_RULES.md#2-the-mandatory-sequence)).
3. Add the summary row below.
4. For anything beyond a trivial reject, add a detail block in §3 using
   [../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).
5. **Check for duplicates first** (Rule CS-2). A repeat is linked and increments
   `recurrence_count` — it is not a new row.

---

## 1. Status vocabulary

| Status | Meaning |
|--------|---------|
| `ADMITTED` | Entered a backlog for the current stage |
| `ADMIT-BYPASS` | Implemented under a human override of the process ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)) |
| `PARKED` | Real work, later stage — see [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) |
| `ESCALATED` | Awaiting a human decision as `CR-###` |
| `REJECTED` | Will not be done; reason recorded |
| `DUPLICATE` | Already tracked; linked |
| `SUPERSEDED` | Overtaken by another decision; linked |
| `LAPSED` | Idea closed by aging (AS-3) |
| `CLOSED-DELIVERED` | Admitted and shipped; linked to the PR |

---

## 2. Register

| ID | Date | Source | Summary | SF | SC | Necessity | Type | P now / target | Action | Ref |
|----|------|--------|---------|----|----|-----------|------|----------------|--------|-----|
| SUG-20260811-u1t | 2026-08-11 | human:Mahesh (SA) | UAT environment Phase 1 (foundation) + phased infra/cost plan document | SF0 | SC1 | MUST | INFRA | P1 / P1 | ADMITTED | [uat-environment-plan](../../platform/uat-environment-plan/README.md) |
| SUG-20260811-u2p | 2026-08-11 | human:Mahesh (SA) | UAT environment Phase 2–3 scale-out (execution) | SF3 | SC1 | NOT-NOW | INFRA | P4 / P1 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#1-parked--scheduled-work) |

<!--
Row format:
| SUG-0001 | 2026-08-08 | agent:claude | Redis-backed idempotency store | SF2 | SC0 | SHOULD | NFR | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#sug-0001) |
-->

---

## 3. Detail records

Detail blocks live here for every non-trivial triage. Format:
[../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).

<!-- ### SUG-0001 · <title>  ... full triage record ... -->

### SUG-20260811-u1t · UAT environment Phase 1 + phased infrastructure & cost plan

```yaml
# schema: triage-record
id: SUG-20260811-u1t
raised_at: "2026-08-11"
raised_by: "human:Mahesh (Solution Architect)"
source: "Solution Architect request to the platform team"
input: >
  Produce a phase-wise UAT environment requirement for the platform team covering the
  ~16-microservice platform: which components (Postgres, Redis, EKS, etc.) in which phase,
  at what configuration, with cluster size growing per phase, plus a cost estimate and
  cost optimisation (no servers up on Sundays, not after 9pm or before 9am).

context:
  workstream: WS-1                  # also serves WS-2 (GATE-IAM-P1)
  current_phase: "Phase 4 — Hardening & consumer enablement"
  canonical_stage: "L7 — Hardening"
  current_objective: "Term path signed off for UAT use by at least one bank caller"
  state_as_of: "2026-08-10"
  state_provisional: false
  freshness_check: "FRESH (exit 0)"
  active_work_item: null            # no work item was in flight

stage_fit:
  code: SF0
  rationale: >
    GATE-P4 criterion 4.3 — "at least one bank caller exercises quote + proposal against
    UAT" — cannot be met without a UAT environment. The current stage cannot exit without
    it. GATE-IAM-P1 (A.1–A.6) has the same dependency for WS-2.
  target_stage: null
  unpark_trigger: null

scope:
  code: SC1
  business_scope: "derived — the open gate's exit criteria are unachievable without it"
  serves: [P4-UAT-SIGNOFF, GATE-P4-4.3, GATE-P4-4.5, GATE-IAM-P1]
  failure_without_it: "GATE-P4 stays open indefinitely; no bank caller can exercise UAT"
  minimal: true
  authority: "CURRENT-STATE.yaml WS-1 current_gate GATE-P4; WS-2 current_gate GATE-IAM-P1"

necessity:
  now: MUST
  future_necessity: MUST
  binds_when: "immediately — gate is open now"
  evidence_tier: E1
  evidence:
    - "CURRENT-STATE.yaml GATE-P4 criterion 4.3 (state: OPEN)"
    - "CURRENT-STATE.yaml GATE-IAM-P1 criteria A.1–A.6 (state: OPEN)"
    - "R0-SCOPE.md §6 — 1SB UAT credentials named as an external dependency"
  confidence: C5
  assumptions: []
  anti_over_engineering:
    X1_named_consumer: true         # five built services need somewhere to run
    X3_cheap_later: false           # external lead times make late provisioning expensive
    X5_stage_necessity: true
    X9_problem_observed: true       # the gate is observably open

action: ADMIT
action_rationale: >
  SF0 + SC1 + MUST. Phase 1 is unblocked by every open compliance question — those gate
  Phase 3, not Phase 1 — so nothing is gained by deferring it.

classification:
  type: INFRA
  also: [DOC, OPS]
  breakdown: EPIC
  risk_tier: T3                     # new environment, external dependencies, spend commitment
  destination: "docs/platform/uat-environment-plan/"

priority:
  now: P1
  at_target: P1
  overrides_applied: [O-blocking-dependency]   # 05 §3 hard P1 override
  rationale: >
    Hard P1 override: blocking dependency. GATE-P4 4.3 cannot close without it, and the
    two longest-lead external dependencies (1SB EIP whitelist, UAT credentials) are on the
    critical path at 2–6 weeks.

dependencies:
  edges:
    - "1SB whitelisting of the UAT NAT egress EIP (external, 2–4 weeks)"
    - "1SB UAT distributor credentials (external, 2–6 weeks)"
    - "AWS account vending via bank Cloud CoE (1–3 weeks)"
    - "Site-to-Site VPN / Direct Connect for tester access (3–6 weeks)"
  state: READY                      # provisioning can start; exit criteria depend on the edges
  enablement_count: 2               # unblocks GATE-P4 and GATE-IAM-P1
  earliest_start: "immediately"

breakdown:
  children:
    - "Landing zone: Org account, VPC, single NAT + EIP, S3 Gateway Endpoint"
    - "EKS cluster + managed node group (3 × m7g.large, Bottlerocket, ARM64)"
    - "Aurora PostgreSQL Serverless v2 (min_capacity 0), logical DB per service"
    - "ElastiCache cache.t4g.micro; ECR; Secrets Manager; KMS; S3"
    - "Argo CD, External Secrets Operator, Karpenter, EKS Pod Identity"
    - "Shutdown schedule + hold-until override + failed-start alerting"
    - "Multi-arch image build (buildx amd64 + arm64) — prerequisite for Graviton"
  completion_definition: "Phase 1 exit criteria in 01-phase-plan-and-scope.md"
  not_included:
    - "MSK / event backbone (Phase 3)"
    - "Service mesh, API Gateway, WAF, CloudFront (Phase 2–3)"
    - "Managed Prometheus / Grafana (Phase 3)"
    - "Production environment (out of scope entirely)"
    - "Dev AWS environment (docker-compose covers it through Phase 1)"

outcome:
  registered_in: "registers/SUGGESTION-REGISTER.md"
  work_item_id: null                # platform team to raise its own tickets — see 06
  plan_id: "docs/platform/uat-environment-plan/"
  status: ADMITTED

notes:
  routing_observation: >
    CURRENT-STATE.yaml routes INFRA to the 1SB module PRODUCT-BACKLOG.md. That is the wrong
    home for a platform-wide environment item serving two workstreams. Filed under
    docs/platform/ instead, and raised here rather than silently mis-filed — the routing
    table may need a platform-scoped INFRA destination. Not actioned: agents do not edit
    the governance model.

resumed: "N/A — this was the requested work item, not an interruption"
```

### SUG-20260811-u2p · UAT environment Phase 2–3 scale-out (execution)

```yaml
SUG-20260811-u2p: "Provision UAT Phase 2 (core sale path) and Phase 3 (compliance/scale)"
context:     WS-1 · Phase 4 · raised alongside SUG-20260811-u1t
stage/scope: SF3 (depends on the architecture review, still an unapproved recommendation) / SC1
necessity:   NOT-NOW · MUST at target · confidence C4
action:      PARK → unparks on architecture-review approval by PO/Compliance/Sponsor
priority:    P4 now / P1 at target
sizing:      costed in docs/platform/uat-environment-plan/03-cost-estimate.md
             (~$583/mo Phase 2, ~$1,400/mo Phase 3, optimised)
reason:      >
  The components are specified and costed so the approval conversation has real numbers,
  but provisioning them commits ~$1,400/month against an architecture that PO, Compliance
  and Sponsor have not yet approved. Phase 1 capacity (max 6 nodes) absorbs roughly two
  weeks of approval slip before squads are constrained.
resumed:     "N/A"
```

---

## 4. Seeded from existing artefacts

AIGEM was adopted mid-flight. Rather than backfilling every past decision
([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)), the
already-deferred items in [TECH-DEBT.md](../../1sb-insurance-integration/service-ssot/TECH-DEBT.md)
were seeded directly into [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) as pre-existing parked work.
They keep their `TD-###` IDs; no `SUG-####` was minted retrospectively.

**Do not re-triage or re-report these** — they are known
([01 §6](../01-CURRENT_STATE.md#6-known-open-debt-affecting-triage)).

---

## 5. Register row convention (machine-enforced)

> **A table row whose first cell is a bare ID is that ID's DEFINITION.** Exactly one definition
> may exist per ID, across every register. `FreshnessCheck` enforces this and halts on a
> duplicate — that is how a cross-branch ID collision is caught after a merge.
>
> Cross-reference rows — the same item shown again in another view, such as an external
> dependency also listed under its edge, or an open risk repeated under accepted risks — must
> **point at** the definition rather than restate the bare ID — for example a leading cell
> of `→ [DEP-002](./DEPENDENCY-REGISTER.md#1-edges)` instead of a bare `DEP-002`.
