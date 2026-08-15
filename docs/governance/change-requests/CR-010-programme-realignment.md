# CR-010 — Programme Realignment: Govern R0 as a Workstream and Re-scope GATE-P4

**Change request:** CR-010
**Date raised:** 2026-08-15
**Raised by:** Rajal — Principal Insurance Platform Product Owner (R1 / Board 3)
**Status:** **PENDING RATIFICATION**
**Change type:** Project scope + stage gate re-scope + governance routing correction
**Approvers required:** **PO + Architect** ([14 §1](../14-CHANGE_CONTROL.md#1-what-needs-a-change-request)),
plus Compliance (Shailja) on §4.2 and Security (Deepali) on §4.1
**Runtime impact:** None. No code changes.
**Proposal detail:** [`05-PROGRAMME-REALIGNMENT.md`](../../au-bank-insurance-platform/po-drive/05-PROGRAMME-REALIGNMENT.md)

---

## 1. Request

Four changes, separable — each may be approved or rejected independently:

| # | Change | CR trigger ([14 §1](../14-CHANGE_CONTROL.md#1-what-needs-a-change-request)) |
|---|---|---|
| **A** | Add **WS-3 — R0 Distribution Platform** to `CURRENT-STATE.yaml`, with phases R0.0…R0.5 and gate `GATE-R0.0` | Add something to project scope |
| **B** | Re-designate **GATE-P4 as an epic gate for `E-HUB`**, not a programme stage gate; move criterion 4.3 to the R0.3 journey gate; park 4.6 pending NFR numbers | Change a stage's exit criteria |
| **C** | Correct `routing.ARCH` — the configured path does not exist | Change a governance file |
| **D** | Record WS-1 and WS-2 as epics `E-HUB` and `E-ID` inside WS-3, retaining their own lifecycles | Add to project scope |

---

## 2. Problem being solved

`CURRENT-STATE.yaml` governs two workstreams. The approved R0 programme
([`R0-SCOPE.md`](../../au-bank-insurance-platform/requirements/R0-SCOPE.md),
[`PRD-R0`](../../au-bank-insurance-platform/requirements/PRD-R0-DISTRIBUTION-PLATFORM.md)) needs
**29 epics**. WS-1 is one of them (`E-HUB`); WS-2 is roughly another (`E-ID`).

The remaining 27 have **no workstream, no gate, no priority and no owner** in the governed state.
The consequence is not theoretical — the governance pipeline evaluates every input against
WS-1's L7 Hardening posture, so programme-level L1/L2 work (rule packs, attribute sheets,
journey specs) reads as *premature* against a component that is four stages ahead of it. **The
framework is working correctly against the wrong baseline.**

Three contradictions follow from that gap, evidenced in the proposal §3:

- **S4** — `R0-SCOPE.md` A2 locks *RM + Self-service + Hybrid Day 1*; WS-2 parks retail-customer
  authentication to a *"later bounded context"*. Two of three Day-1 channels have no identity path.
- **S5** — A6 defines *Sold* as issued **+ confirmation + reconcilable + ops-trackable*; no
  workstream delivers reconciliation or ops tracking.
- **S6** — A7 makes suitability mandatory *before quote*; D11 ships `consentRef` WARN-only and no
  suitability gate exists, while the quote path is already delivered and being hardened.

---

## 3. Change A — add WS-3

Proposed `workstreams[]` entry (abridged; full YAML applied on ratification):

```yaml
- id: WS-3
  name: "R0 Distribution Platform"
  authority:
    - "docs/au-bank-insurance-platform/requirements/R0-SCOPE.md"
    - "docs/au-bank-insurance-platform/requirements/PRD-R0-DISTRIBUTION-PLATFORM.md"
    - "docs/au-bank-insurance-platform/po-drive/05-PROGRAMME-REALIGNMENT.md"
  lifecycle:
    canonical_stage: "L1 — Business Design"
    current_phase: "R0.0 — Freeze"
    stage_status: IN_PROGRESS
    next_stage: "R0.1 — Ratify & model"
  current_objective:
    id: R0-FREEZE
    description: "Business scope signed by the sponsor; consent and suitability rule packs executable"
  current_gate:
    id: GATE-R0.0
    state: OPEN
    exit_criteria:
      - { id: "R0.0.1", criterion: "Executive sponsor named; RACI and steering cadence published", state: OPEN }
      - { id: "R0.0.2", criterion: "Sponsor sign-off on Working Decisions v1 + R0-SCOPE", state: OPEN }
      - { id: "R0.0.3", criterion: "Consent rule pack v1 approved by Compliance", state: OPEN }
      - { id: "R0.0.4", criterion: "Suitability rule pack v1 approved by Compliance", state: OPEN }
      - { id: "R0.0.5", criterion: "BRD chapters carry acceptance criteria", state: OPEN }
      - { id: "R0.0.6", criterion: "Quote/compare rule pack v1 (Group A)", state: OPEN }
      - { id: "R0.0.7", criterion: "S4 resolved — customer-identity path decided", state: OPEN }
      - { id: "R0.0.8", criterion: "S6 resolved — Compliance verdict on the live quote path", state: OPEN }
    approvers: ["PO (Rajal)", "Architect (Mahesh)", "Compliance (Shailja) — R0.0.3/4/8", "Sponsor — R0.0.2"]
```

**Note on stage posture.** WS-3 sits at **L1** while WS-1 sits at L7. That is legitimate under
[Rule LC-1](../03-LIFECYCLE.md#5-multi-workstream-evaluation) — stage fit is evaluated against the
input's *own* workstream, never the repository's most advanced one. WS-3's existence is what
makes rule-pack and attribute-sheet work correctly read as **SF1**, not SF3.

---

## 4. Change B — re-scope GATE-P4

GATE-P4 as written is **unpassable by construction**:

| Criterion | Problem | Proposed |
|---|---|---|
| 4.3 — a bank caller exercises quote + proposal against UAT | Needs a journey, a consent gate and a suitability gate. None exist, and per S6 they must *precede* quote. An adapter cannot satisfy this alone. | **Move to the R0.3 journey gate.** Not waived — relocated to the altitude where it can be met. |
| 4.6 — p95 quote under nominal concurrency | No threshold exists; the NFR sheet was never written (GAP-017). The criterion has nothing to test against. | **Park** until GAP-017 sets a number, then re-admit *with* the threshold. |
| 4.1, 4.2, 4.4, 4.7 | Legitimately adapter-scoped | **Keep on `E-HUB`.** |
| 4.5 | Runbook drafted; awaits only Board 7 verdict | **Closeable now.** |

**No requirement is dropped.** Every criterion keeps its force; two change altitude. This is the
distinction between a re-scope and a waiver, and I am explicitly asking for the former.

### 4.1 Security consultation — S4

Whether `E-CUSTID` (customer identity for self-service/hybrid) is built in R0.2 or self-service
is formally dropped from Day 1 is a **Security + Architecture** decision, not a Product one.
Deepali (R8) must rule on the trust boundary before R0.2 planning.

### 4.2 Compliance consultation — S6

Whether the already-shipped quote path is permissible without suitability and consent gates is
**Shailja's (R9) call alone**. Product cannot self-certify this. If the verdict is that it is not
permissible, the finding becomes an [O5 regulatory P1](../05-PRIORITY_MODEL.md#3-hard-p1-overrides)
against the delivered Term path, and GATE-P4 should not close at all until remediated.

**This CR does not presume that verdict in either direction.**

---

## 5. Change C — correct the ARCH routing path

`CURRENT-STATE.yaml` routes ARCH work to `docs/architecture-review/08-architecture-decision-log.md`.
**That path does not exist.** The file is at `docs/platform/architecture-review/08-architecture-decision-log.md`.

Consequence: ARCH work has had nowhere to land since AIGEM adoption, which is a contributing
cause of **zero ADRs existing** (`id_allocation.ADR: 1`) despite ARCH-001…022 being authored.

```diff
   ARCH:
-    - "docs/architecture-review/08-architecture-decision-log.md"
+    - "docs/platform/architecture-review/08-architecture-decision-log.md"
```

The same correction applies to the `MIGRATION` route, which carries the identical broken path.

This change is **factual, not discretionary** — the current value is a broken reference. It is
separable and can be approved immediately regardless of A, B and D.

---

## 6. Change D — fold WS-1 and WS-2 into WS-3 as epics

WS-1 and WS-2 **keep their own lifecycles, gates and backlogs**. This change records their
*relationship* to the programme so that:

- programme-level dependency ordering can see them ([07 §5](../07-DEPENDENCY_MODEL.md));
- their gates are read as epic gates, not programme stage gates;
- `E-HUB` and `E-ID` stop being mistaken for the whole project.

Nothing about how WS-1 or WS-2 is executed changes.

---

## 7. What this CR does NOT change

- No code changes. No runtime impact.
- **D1–D14** (PO ↔ Architect 1SB session) — unchanged and still binding.
- **ARCH-001…022** — this CR asks for them to be *ratified*, not re-opened. Re-litigating settled
  design is the drift [17](../17-DRIFT_CONTROL.md) forbids.
- WS-1's engineering, architecture and delivered scope — explicitly endorsed, not criticised.
- Persona roster — closed as of CR-009; unchanged.
- No stage transition is declared. `current_phase` and `stage_status` remain human-owned
  ([04 §5](../04-STAGE_GATES.md#5-who-may-declare-a-transition)); this CR *proposes* values for a
  new workstream and applies them only on ratification.

---

## 8. Risk of not doing this

| If rejected | Consequence |
|---|---|
| A rejected | 27 epics stay ungoverned; programme L1/L2 work keeps reading as SF3 against WS-1's L7 posture and gets parked automatically |
| B rejected | GATE-P4 cannot pass; WS-1 stays at L7 indefinitely and the parked items keyed to "Phase 4 gate PASSED" (TD-022, TD-010) never unpark |
| C rejected | ARCH work continues to have no destination; ADR count stays at zero |
| D rejected | Cosmetic only; A still delivers most of the value |

---

## 9. Provenance and honesty note

This CR is raised by an agent reasoning in the **Rajal / R1** persona. Per the precedent set in
CR-009 §9.1, that is recorded plainly rather than smoothed over:

- **It is not a Product board verdict.** It is a proposal for the human R1 authority to accept,
  amend or reject.
- **It does not carry Compliance or Security approval.** §4.1 and §4.2 name decisions reserved to
  Deepali (R8) and Shailja (R9). Nothing here may be read as pre-approving either.
- **Findings S4, S5 and S6 are assertions of contradiction between existing documents**, each
  cited to its source. They are verifiable by reading those documents, and should be verified
  rather than taken on the agent's word.
- The agent did not edit `current_phase`, `stage_status`, or any gate state
  ([09 §Never](../09-AI_EXECUTION_RULES.md)), and does not approve this CR.

## 10. Ratification

| Role | Name | Verdict | Date |
|---|---|---|---|
| Product Owner (R1) | Rajal | ⬜ pending | — |
| Architect (R2) | Mahesh | ⬜ pending | — |
| Compliance (R9) — §4.2 | Shailja | ⬜ pending | — |
| Security (R8) — §4.1 | Deepali | ⬜ pending | — |
