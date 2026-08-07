# 14 — Change Control

**Layer:** L1 — generic
**Owner:** Product Owner + Architect (joint approval)

---

## 1. What needs a change request

A `CR-###` is required to change any of the **fixed points** the pipeline depends on:

| Change | CR required | Approvers |
|--------|:-----------:|-----------|
| Add or remove something from project scope ([02](./02-PROJECT_SCOPE.md)) | ✅ | PO + Architect |
| Pull a parked item forward into an earlier stage | ✅ | PO + Architect |
| Change a stage's exit criteria ([04](./04-STAGE_GATES.md)) | ✅ | Architect + PO + affected boards |
| Declare a stage transition without meeting a criterion (waiver) | ✅ | Architect + PO + Compliance if regulatory |
| Change a standing constraint ([01 §5](./01-CURRENT_STATE.md#5-standing-constraints-apply-to-every-triage-in-this-repo)) | ✅ | Architect (+ ADR) |
| Reverse a REJECTED decision | ✅ | Original approver or PO |
| Change an approved plan materially (§4) | ✅ (lightweight) | Affected boards only |
| Change these governance files (`GOV`) | ✅ | Architect + PO |
| Admit an SC4 externally-mandated item | ✅ | PO + Compliance + Architect |
| Waive a review board for a tier that requires it | ✅ | Architect; **never** for T4 Security/Compliance |

**No CR needed** for: normal triage outcomes, priority recomputation at a stage change,
re-parking with a new target, adding acceptance criteria that clarify rather than expand, or
recording a variance inside `files_expected`.

---

## 2. Change request format

```yaml
change_request:
  id: CR-004
  raised_by: "agent:claude"
  date: 2026-08-07
  type: SCOPE            # SCOPE | STAGE | PLAN | CONSTRAINT | REVERSAL | WAIVER | GOV

  current_position: >
    What the approved documents say today. Quote them.

  proposed_change: >
    Precisely what changes, in which document, with the replacement text.

  driver: >
    Why. One of: regulatory/legal mandate · security finding · external dependency change ·
    validated assumption failure · business priority change · new evidence.
    "It would be better" is not a driver.

  evidence:
    - "IRDAI circular reference / CVE ID / measurement / failing gate criterion"

  impact:
    scope:        "..."
    stage:        "does this move a gate date?"
    dependencies: "which items become blocked, unblocked, or invalid"
    parked_items: "which parked items this makes eligible"
    effort:       "S | M | L | XL"
    risk_if_rejected: "..."

  alternatives_considered:
    - option: "do nothing"
      consequence: "..."

  decision: PENDING       # PENDING | APPROVED | REJECTED | DEFERRED
  approvers: []
  decided_on:
  conditions: []
```

---

## 3. Procedure

```text
1. Raise CR-###. Do not implement anything the CR contemplates.
2. Impact analysis: run pipeline steps 2–8 as if the change were approved,
   so approvers see the downstream consequences, not just the request.
3. Route to approvers (§1). Regulatory drivers additionally route to Compliance.
4. Decision recorded in registers/DECISION-REGISTER.md.
5. If APPROVED:
     a. Update the affected document (02 / 04 / 01 / governance file).
     b. Update state/CURRENT-STATE.yaml.
     c. Run the unpark sweep (08 §5) — scope changes routinely free parked work.
     d. Recompute priorities for READY and PARKED items.
     e. Create or re-triage the work items the CR enables.
6. If REJECTED: record the reason on the CR and on the originating SUG-####,
   so the same proposal is not re-raised without new evidence.
```

> **Rule CC-1 — An agent may raise a CR; it may never approve one.** Not even its own, not even
> when the change seems obviously correct, not even under time pressure.

---

## 4. Changing an approved plan

Plans change during implementation. The question is whether the change is a *variance* or a
*new decision*.

| Change | Handling |
|--------|----------|
| A file outside `files_expected` in a component already listed | **Variance** — log it in `variance_log`, continue |
| A new component not in `affected_components` | Re-review: **Architecture + Technical** |
| Any change to `data_changes`, `api_changes`, `security_impact`, `compliance_impact` | Re-review by the corresponding boards, mandatory |
| Acceptance criteria clarified without widening | Variance |
| Acceptance criteria widened | New work item or CR — not a plan edit |
| Approach replaced with a different mechanism | Re-review all boards that approved the original |
| Something in `out_of_scope` turns out to be required | **Stop.** New work item, fresh triage. This is the single most common creep vector |
| Effort estimate doubles | Re-check breakdown ([06 §5](./06-WORK_CLASSIFICATION.md#sizing-sanity-checks)); likely needs splitting |

Re-review is scoped to the affected boards, not the whole board — proportionality applies here
too.

---

## 5. Emergency changes

For genuine production or security emergencies:

```text
1. Act. Stop the harm first — the framework never blocks incident response.
2. Record within the same working session:
     - what was changed and why
     - which controls were bypassed
     - the blast radius
3. Raise CR-### retrospectively, marked type: EMERGENCY.
4. Within one stage: run the boards that were skipped, and either
   ratify the change or schedule its remediation as a P1/P2 item.
5. Root cause goes to the risk register; the shortcut goes to the debt ledger.
```

Emergency is a **narrow** category: production impact, active exploit, data loss, or regulatory
breach in progress. "The demo is tomorrow" is not an emergency; it is a priority conversation.

---

## 6. Reversing a rejection

Rejections are durable but not eternal. Reopen only with **new evidence**:

| Valid grounds | Not valid |
|---------------|-----------|
| Scope changed via an approved CR | "Asking again" |
| The stage advanced and it is now on-stage | "A different agent thinks so" |
| New regulatory or security obligation | "It's a best practice" |
| The original rejection rested on an assumption now proven false | Time passing |
| Recurrence count ≥ 3 with independent sources | A single repeat |

Reopening links back to the original `SUG-####` and states the new evidence. The history stays
visible — that is what stops the same argument being re-run every quarter.
