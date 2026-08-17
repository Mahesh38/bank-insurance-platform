# Board 1 — Architecture · Verdict on CR-010

**Change request:** [CR-010 — Reusable Context Module and Safe Autopilot Foundation](../../CR-010-context-module-and-safe-autopilot.md)
**Plan:** [PLAN-001](../../../plans/PLAN-001-context-module-and-safe-autopilot.md)
**Board:** 1 — Architecture · **Persona:** Mahesh — Principal Insurance Platform Architect (R2)
**Reviewer type:** `AGENT` (AI simulation of Mahesh) · **Self-review:** false
**Change tier:** T4 — triggers G8 (production topology and trust boundary, via ADR-001) and G10
(creates and changes controls a regulator can ask us to evidence)
**Date:** 2026-08-16

> ## Verdict: `APPROVE-WITH-MODIFICATION`
> **Architecture severity:** `A1` — one major structural issue in the automation boundary itself,
> plus five A2 items. None is A0. Nothing here blocks the direction of CR-010; conditions C-01 and
> C-02 must close before the CR binds.
>
> **`signature_status: AI-DRAFTED — mandatory human signature outstanding`**
> [`11-REVIEW_GATES.md §2`](../../../11-REVIEW_GATES.md) is binding: a T4 Architecture verdict
> requires a human signature and this AI simulation does not supply it. CR-010's own ratification
> table records the Architecture conclusion as `PENDING`, and correctly notes that the repository
> owner's request to implement on a branch **is not an Architecture verdict**. That remains true
> after this document exists.

---

## 1. Decision requested of Board 1

CR-010 §4 asks Architecture to conclude on **context boundaries, routing and automation design**.
The realignment plan and position assessment add four more items that are Architecture-owned and
that I rule on here because CR-010 is the instrument carrying them:

| # | Decision requested | Source |
|---|---|---|
| D-1 | Context module boundaries — reusable framework vs project overlay | CR-010 §3 |
| D-2 | Workstream-aware routing and path validation | CR-010 §3 |
| D-3 | The non-negotiable automation boundary and its enforcement | CR-010 §2 |
| D-4 | Registration of WS-3; re-parenting of WS-1 as a supplier workstream and WS-2 as an enabler | [`01-POSITION-ASSESSMENT.md §8`](../../../../application-lifecycle-bible/01-POSITION-ASSESSMENT.md), [`03-REALIGNMENT-PLAN.md` Move 4](../../../../application-lifecycle-bible/03-REALIGNMENT-PLAN.md) |
| D-5 | The S08/S09 Foundation Recovery Increment | Realignment plan Move 3 |
| D-6 | Re-statement of WS-1 Phase 4 criteria | Realignment plan Move 1, assigned to *Mahesh + Swapnali* |
| D-7 | Separation of structured gate evidence from human-owned lifecycle state | CR-010 §3 |

---

## 2. What I reviewed

Evidence per Rule RG-3. This list is what I actually opened, not a bibliography.

**Governance and change control**
`CR-010`, `PLAN-001`, `AGENTS.md`, `docs/governance/README.md`, `PERSONA-AUTHORITY-MATRIX.md`,
`14-CHANGE_CONTROL.md`, `11-REVIEW_GATES.md`, `04-STAGE_GATES.md`, `03-LIFECYCLE.md`,
`state/CURRENT-STATE.yaml`, `state/GATE-EVIDENCE.yaml`.

**Lifecycle bible**
`01-POSITION-ASSESSMENT.md`, `02-STAGE-MODEL.md`, `03-REALIGNMENT-PLAN.md`,
`04-GATE-AND-SIGNOFF-MODEL.md`, `06-QUALITY-NORMS.md`, `07-SECURITY-COMPLIANCE-CANON.md`,
`08-SRE-READINESS-CANON.md`, `stages/S02`, `stages/S03`, `stages/S06`, `stages/S07`, `stages/S08`,
`stages/S09`.

**Architecture and domain sources**
`docs/platform/architecture-review/02`, `06`, `08`; `docs/platform/authentication-authorization/README.md`;
`docs/1sb-insurance-integration/architecture/1sb-integration-service-architecture.md`;
`docs/1sb-insurance-integration/canonical-model/contexts.md`;
`docs/au-bank-insurance-platform/knowledge-base/07-information-model-and-rules.md`;
`docs/au-bank-insurance-platform/requirements/R0-SCOPE.md`;
`docs/context/business-problem-statement.md`; `docs/context/context-manifest.yaml`.

**Code and automation — read, not assumed**
`scripts/governance/autopilot.py` (full), `scripts/governance/test_autopilot.py`,
`scripts/governance/ci-checks.py` (checks 2, 5, 6, 8), `.github/workflows/application-ci.yml`,
`.github/workflows/governance.yml`, `.github/` directory listing,
`services/bank-persistence-service/src/main/resources/db/migration/V1__init_schema.sql`,
`services/1sb-integration-service/.../domain/` package and model classes
(`QuoteJob`, `JobStatus`, `PaymentStatus`, `PaymentSession`, `BankApplicationStatus`),
`services/` and `libs/` module inventory.

**Board 1 checklist A1–A10** applied to CR-010 as a whole; results in §3.

---

## 3. Findings

Severity is Mahesh's architecture scale `A0`–`A3` and must not be read as AIGEM `P1`–`P5`,
Deepali's `S0`–`S3` or Shailja's `R0`–`R3`.

### A-F01 · `A1` · The automation boundary is documentary at exactly the point where it is writable

CR-010 §2 states that automation *may never independently* mark a stage `PASSED`, provide board
approval, or weaken a binding control. The controller's **content** honours this: `candidate_proposal()`
emits `state: CANDIDATE` with `may_mark_passed: false`, `validate_policy()` refuses to run outside
proposal-only mode, and `test_autopilot.py` asserts both.

But `autopilot.py propose-transition` accepts `--output <Path>` and executes
`args.output.write_text(rendered, encoding="utf-8")` with **no constraint on the destination**.
Nothing in the repository prevents:

```
python scripts/governance/autopilot.py propose-transition \
    --workstream WS-1 --output docs/governance/state/CURRENT-STATE.yaml
```

That would overwrite the human-owned state file with a proposal document. There is no CODEOWNERS
file (`.github/` contains only `workflows/`), no protected-path assertion in `ci-checks.py`, and no
test covering the write path.

This is not a claim that the controller *would* do this — it is proposal-only by design and by test.
It is a claim about the **boundary**: CR-010's central safety property is enforced by what the
controller writes, and not at all by where it may write. For a control plane whose entire purpose is
to be trustworthy near human-owned state, that asymmetry is a structural defect, and it is cheap to
close.

**Required:** constrain `--output` to a proposals directory (reject any path resolving outside it,
including via `..`), and add a safety test asserting that a protected path is refused. See
condition **C-01**.

### A-F02 · `A2` · Safety tests assert a property of today's data, not of the controller

`test_autopilot.py` calls `autopilot.load_bundle()` with no arguments, which reads the **live**
`GATE-EVIDENCE.yaml`, and then asserts `selected["id"] == "4.2"` and `== "4.4"`.

Two consequences. First, the safety property "blocked work is never selected" is verified only for
the criteria that happen to be blocked today; a future evidence file with a different shape could
pass the test while violating the property. Second — and this is the coupling that will actually
bite — a legitimate edit to gate evidence breaks a *safety* test, which trains people to edit the
test alongside the state. A safety test that moves with the data it guards is not a guard.

**Required:** fixtures. See condition **C-02**.

### A-F03 · `A2` · Two ratified documents contradict each other on customer identity

[`R0-SCOPE.md §2`](../../../../au-bank-insurance-platform/requirements/R0-SCOPE.md) decision A2 puts
**self-service and hybrid journeys in Day-1 scope**. `CURRENT-STATE.yaml` WS-2 `out_of_scope` lists
*"Retail-customer authentication — revisit_at: later bounded context"*.

A self-service journey requires a customer principal. Both statements cannot hold. Today the
contradiction is latent because no journey runs; at S11 it becomes a blocking design question with
Security consequences, discovered at the worst possible moment.

I am not resolving it — the resolution is a Product scope decision (Rajal) with a Security design
consequence (Deepali), not an architecture preference. See condition **C-07**.

### A-F04 · `A2` · `context-manifest.yaml` precedence omits the lifecycle bible

The manifest's `authority.precedence` lists `docs/governance/`, the business working-decisions and
decision log, `docs/platform/`, `docs/1sb-insurance-integration/service-ssot/` and `docs/context/`.
It does not list `docs/application-lifecycle-bible/`.

That directory now carries the 16-stage model, the gate and sign-off model, the security/compliance
control catalogue C1–C10 and the SRE readiness canon. A document set that defines gate criteria has
no declared position in the conflict-resolution order. Given that CR-010's stated purpose is to make
context authority explicit, this is precisely the kind of gap it should close. See condition **C-03**.

### A-F05 · `A2` · Path filters plus required status checks

`application-ci.yml` triggers `on: pull_request` with a `paths:` filter over `services/**`,
`libs/**` and the build files. This is correct and efficient for compute, and it interacts badly
with the branch protection that S08-G2 requires: a required status check that is skipped by a path
filter never reports, and on GitHub that either blocks the PR indefinitely or — with the wrong
configuration — allows a merge with no check. Both outcomes defeat S08-G2.

This is Amit's to solve mechanically; it is an Architecture finding because S08-G2 is a gate
criterion and this makes it unachievable as configured. See condition **C-08**.

### A-F06 · `A2` · `id_allocation.sequential.ADR` is unadvanced

`CURRENT-STATE.yaml` declares `ADR: 1`. This increment allocates ADR-001, ADR-002 and ADR-003 in
[`08-architecture-decision-log.md`](../../../../platform/architecture-review/08-architecture-decision-log.md).
`FreshnessCheck` verifies `next` values against the registers, so the counter must advance to `4`
or the next agent will mint a collision. See condition **C-10**.

### A-F07 · `A3` · No `concurrency` group on either workflow

Successive pushes queue full runs. Minor cost and feedback-latency issue against NFR-ENG-01; a
one-line fix. Non-blocking.

### Board 1 checklist result

| # | Check | Result |
|---|---|---|
| A1 | Respects module, service and bounded-context boundaries? | **Yes.** CR-010 touches governance, context and CI only; `services/` and `libs/` are untouched, and PLAN-001's `out_of_scope` says so |
| A2 | Responsibility in the correct component? | **Yes**, with A-F01: the write boundary sits in the CLI where it cannot be reasoned about, rather than in a guarded helper |
| A3 | Coupling justified and directional? | **Yes**, with A-F02: the safety test is wrongly coupled to live state |
| A4 | Violates a principle or standing constraint? | **No.** It adds seven (see [`ws3-platform/00 §6`](../../../../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md)) |
| A5 | ADR exists where a decision changes? | **It did not.** Now supplied: ADR-001, ADR-002, ADR-003 |
| A6 | Unnecessary infrastructure? | **No.** No runtime infrastructure is added. Deliberately: no event broker in R0 |
| A7 | Future migration problem? | **One**: the four audit-schema additions (OPEN-I3). Recorded, not silent |
| A8 | Fits the current stage? | **Yes**, and it is the change that makes stage fit computable at all — GAP-D |
| A9 | Smallest structural change? | **Nearly.** Registering one workstream and re-parenting two is the minimum that fixes GAP-D. I rejected a fourth workstream for the Flutter client (ADR-002) |
| A10 | Cost to replace later? | **Low** for the context module and autopilot; **high** for the workstream topology, which is why ADR-002 is `A4_HUMAN_REQUIRED` |

---

## 4. Rulings on the seven requested decisions

### D-1 — Context module boundaries · `APPROVED`

The `framework` (portable, `authority: none`) / project-overlay split is correct, and
`portability.excluded_from_reuse` is the part that makes it real: it names the bancassurance
personas, regulatory conclusions and live lifecycle state as non-portable. A "reusable" module that
carried those would be a template with someone else's regulator baked in.

`authority.context_is_binding: false` with `canonical_authority` pointing at the persona matrix is
the right shape. Context informs; it does not decide. Conditioned on **C-03**.

### D-2 — Workstream-aware routing and path validation · `APPROVED`

Making the first routing key the owning workstream is the fix for a real defect: a valid WS-2 or
cross-cutting classification could previously land in the WS-1 supplier backlog. Asserting closure
over the sixteen canonical work types **and** that every destination path exists converts routing
from documentation into a checked invariant, which is exactly the right treatment.

One ruling that limits scope. Realignment Move 4 item 3 asks for *"routing entries for the new work
types the platform generates (UI/UX, IaC, pipeline)"*. **There are no new work types.** A Flutter
story is `FUNC`; a Terraform module is `INFRA`; a pipeline stage is `INFRA` or `QA`. The sixteen are
closed and correct, and extending them would break the closure `ci-checks.py` enforces. What was
missing was a **workstream** for that work to belong to. Condition **C-04**.

### D-3 — The non-negotiable automation boundary · `APPROVED-WITH-MODIFICATION`

The boundary as written in CR-010 §2 is correct and I would not weaken a word of it. Its
implementation is correct in content and incomplete in reach — A-F01 and A-F02. Conditions
**C-01** and **C-02**.

I want one thing on the record, because it is the property that makes the rest defensible: the
controller **refuses** rather than degrades. `validate_policy()` raises on any policy that is not
exactly proposal-only; `candidate_proposal()` raises when evidence is incomplete. A control plane
that fails closed under an unexpected configuration is a materially different artefact from one
that logs a warning and proceeds, and the distinction is worth naming in a repository that is about
to build a suitability gate on the same principle.

### D-4 — WS-3 registration and re-parenting · `APPROVED-WITH-MODIFICATION`

GAP-D is correct, and it is a **routing** defect, which is Architecture's to fix. The architecture
registration — technical shape, S08 placement, interface contracts IF-1/IF-2/IF-3, standing
constraints, and the exact YAML for transcription — is in
[`ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md`](../../../../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md)
and recorded as **ADR-002**.

Rulings within D-4:

| # | Ruling |
|---|---|
| D-4a | **WS-3 registered at S08, not S06, S07 or S11.** The binding constraint is machinery, not knowledge. Reasoning in the registration §2.1 |
| D-4b | **WS-1 re-parented as a supplier workstream; its stage does not move.** `L7 — Hardening` stays true for a component. Any reading of re-parenting as a demotion is a misreading, and I have said so in the registration §2.2 |
| D-4c | **WS-2 re-parented as an enabler.** Interface IF-2; its gate and criteria are untouched |
| D-4d | **No fourth workstream for the Flutter client.** It is part of WS-3 (ADR-002 alternatives) |
| D-4e | **The foundation is built once, in WS-3, and consumed by all three** (IF-3). A second pipeline for WS-1 would repeat at platform level the duplication the shared `bank-common-*` libraries correctly avoid at code level |

ADR-002 is `A4_HUMAN_REQUIRED`. An AI simulation of Mahesh may recommend it; it may not finalise a
material scope and stage decision. Condition **C-09**.

### D-5 — S08/S09 Foundation Recovery Increment · `APPROVED-WITH-MODIFICATION`

Approved, with three architecture rulings.

1. **S09 runs overlapped with S08, not after it.** The deployment pipeline is an extension of the
   platform. In series you get a pipeline with nowhere to deploy, followed by a platform with
   nothing proven to deploy onto. The S09 stage file already states this as an entry criterion; I am
   ratifying it as the sequencing decision.
2. **Admission rule AR-S08 is binding** and is recorded as **ADR-003**: no stage passes S08 until
   the application carries machine enforcement equivalent to what the governance documents already
   carry. Operationally that is the fifteen fitness functions in
   [`ws3-platform/03 §7`](../../../../platform/ws3-platform/03-solution-architecture-r0.md) executing
   *and failing builds*, demonstrated per S08-VT-01…VT-05.
3. **Merging `application-ci.yml` does not satisfy S08-G1.** S08-G1 requires E4 — a run history. A
   workflow file is a mechanism. The position is materially better than before and it is still not a
   pass, and I would rather say that now than have it discovered at the gate.

Condition **C-05** constrains what may be built during the increment: **no new bounded context**.
The instinct on seeing "16 of 19 contexts missing" is to start writing services; that is the
original error at four times the scale.

### D-6 — WS-1 Phase 4 re-statement · `APPROVED-WITH-MODIFICATION`

Full criterion-by-criterion re-statement in
[`ws3-platform/00 §5`](../../../../platform/ws3-platform/00-WS3-ARCHITECTURE-REGISTRATION.md).
Summary: 4.1, 4.3 and 4.6 remain `BLOCKED` and their named prerequisites are widened to include the
S08/S09 deliverables they actually depend on; 4.2 stays `PARTIAL`; 4.4 and 4.5 stay `OPEN` and are
correctly not stopped by the recovery increment.

**One state changes: 4.7 moves from `PARTIAL` to `BLOCKED`.** JaCoCo is configured and has never
executed on a pull request. Under Rule GS-3 a criterion whose blocker is a missing prerequisite is
`BLOCKED`; `PARTIAL` implies someone can close it by trying harder. It returns to `PARTIAL` on the
first green application-CI run.

This is a joint Board 1 + Board 5 re-statement and **Swapnali's concurrence is required**;
it is not mine to record. Condition **C-06**.

I also note, because it is the more useful half: criterion 4.6 was unevaluable for a reason nobody
had named — *"p95 quote under nominal concurrency"* defined neither term. It is now
**NFR-LAT-03: p95 < 5 s at 6.8 journey starts per minute**. The criterion is still blocked, but it
is blocked on machinery rather than on ambiguity, and only one of those was ever schedulable.

### D-7 — Structured gate evidence separated from lifecycle state · `APPROVED`

This is the best structural idea in CR-010 and I want it recorded as such. `CURRENT-STATE.yaml`
holds human-owned lifecycle fields that agents must not edit; `GATE-EVIDENCE.yaml` holds
machine-readable evidence with owners, verifiers, blockers, execution mode and approvals — and
`ci-checks.py` check 3 asserts the two agree. That separation is what lets automation be genuinely
useful near a human-owned decision without ever being able to make it. It is the same principle as
the token-hiding BFF: the thing that does the work never holds the thing that grants authority.

---

## 5. Conditions

Under Rule GS-4 and [`11-REVIEW_GATES.md §14`](../../../11-REVIEW_GATES.md), **these conditions
become acceptance criteria and are tracked to closure.** An `APPROVE-WITH-MODIFICATION` whose
conditions are not tracked is an unconditional approval with extra words.

| # | Condition | Severity | Owner | Required by |
|---|---|---|---|---|
| **C-01** | Constrain `autopilot.py --output` to a proposals directory. Reject any destination resolving outside it, including via `..` or a symlink. Add a safety test asserting that a protected path — at minimum `docs/governance/state/CURRENT-STATE.yaml`, `GATE-EVIDENCE.yaml` and any file under `docs/governance/change-requests/` — is **refused**, and wire that test into the governance workflow. | `A1` | Amit + Deepali | **Before CR-010 binds** |
| **C-02** | Autopilot safety tests run against committed fixtures, not the live `GATE-EVIDENCE.yaml`. The properties asserted must be "blocked work is never selected" and "no path emits PASSED" as *properties*, not as assertions about criterion 4.2 and 4.4. | `A2` | Amit + Swapnali | **Before CR-010 binds** |
| **C-03** | Add `docs/application-lifecycle-bible/` to `context-manifest.yaml` `authority.precedence`, positioned after `docs/governance/`. It defines gate criteria and currently has no declared conflict-resolution position. | `A2` | Mahesh + Rajal | With CR-010 ratification |
| **C-04** | The canonical work-type enum stays closed at sixteen. The WS-3 routing table is transcribed exactly as in the architecture registration §7.6. If Rajal's charter introduces a dedicated WS-3 backlog file, substitute it in both the routing table and `authority`. | `A2` | Orchestrator | With CR-010 ratification |
| **C-05** | No new bounded context is implemented during the Foundation Recovery Increment (AR-S08-4). Recovery scope is S08 and S09 machinery, plus defect fixes on delivered paths. | `A2` | Kalpana + Amit | For the duration of the increment |
| **C-06** | WS-1 criterion 4.7 recorded as `BLOCKED` with blocker `S08-G3` in both `CURRENT-STATE.yaml` and `GATE-EVIDENCE.yaml`, **with Swapnali's Board 5 concurrence**. Architecture cannot re-state a QA criterion alone. | `A2` | Swapnali + orchestrator | With CR-010 ratification |
| **C-07** | Resolve the customer-identity contradiction (A-F03) between R0-SCOPE A2 and the WS-2 out-of-scope list. Product decides scope; Security designs the consequence; Architecture records the interface. | `A2` | Rajal + Deepali | Before S11 entry |
| **C-08** | Reconcile `application-ci.yml` path filtering with the branch protection S08-G2 requires, so that a required check cannot be silently skipped. | `A2` | Amit + Shivanshi | Before S08-G2 is claimed |
| **C-09** | ADR-002 (workstream topology) obtains the human approvals its `A4_HUMAN_REQUIRED` class demands: Mahesh (human), Rajal, Kalpana and the accountable sponsor. GAP-010 records the executive sponsor as still unnamed; that is the gap to close first. | `A1` | Sponsor + Rajal + Kalpana | **Before CR-010 binds** |
| **C-10** | Advance `id_allocation.sequential.ADR` to `4` in `CURRENT-STATE.yaml`. | `A3` | Orchestrator | With CR-010 ratification |
| **C-11** | GATE-S08 architecture approval is withheld until S07-G3 and S07-G4 carry a **human** Security signature. A foundation stage cannot pass while the threat model it implements is unratified. | `A1` | Deepali (human) | Before GATE-S08 |
| **C-12** | Every open item in [`ws3-platform/01 §10`](../../../../platform/ws3-platform/01-domain-model-and-invariants.md), [`02 §7`](../../../../platform/ws3-platform/02-information-model.md), [`04 §4.1`](../../../../platform/ws3-platform/04-security-architecture.md) and [`05 §5`](../../../../platform/ws3-platform/05-nfr-catalogue.md) is transcribed into the appropriate register with its owner and target. An open item that lives only in an architecture document is not tracked. | `A2` | Kalpana | With CR-010 ratification |

---

## 6. What I am explicitly not doing

| Not doing | Why |
|---|---|
| Approving a stage transition | Only Mahesh + Rajal jointly, as humans, mark a gate `PASSED`. This verdict is a board conclusion on a change request |
| Editing `CURRENT-STATE.yaml`, `GATE-EVIDENCE.yaml` or CR-010 | Orchestrator-owned this increment; stage state is human-only regardless |
| Ruling on regulatory permissibility of the suitability or consent design | Shailja's jurisdiction. I name enforcement points; she rules on sufficiency |
| Ruling on security posture | Deepali's. My security architecture document is an **input** to Board 4, explicitly labelled as such |
| Accepting the residual risk of shipping without S08 | `A4_HUMAN_REQUIRED`. I can describe it; I cannot accept it |
| Overriding a Compliance or Security block | AP-08 and the non-bypassable rules. Architecture proposes alternatives; it does not convert a block into accepted risk |
| Recording Swapnali's, Rajal's, Shailja's or Kalpana's verdicts | Not mine. Rajal's lane authors those |

---

## 7. Canonical Board 1 record

```yaml
architecture_review:
  decision_id: ARCH-DEC-CR010-B1
  reviewer_persona: "Mahesh — Principal Insurance Platform Architect"
  reviewer_type: AGENT
  self_review: false
  change_request: CR-010
  plan: PLAN-001
  change_tier: T4
  decision: APPROVED_WITH_CONDITIONS
  architecture_severity: A1
  authority_class: A3_JOINT_REVIEW   # ADR-002 within it is A4_HUMAN_REQUIRED
  confidence: HIGH
  summary: >
    CR-010's direction is right and its central structural idea — separating machine-readable gate
    evidence from human-owned lifecycle state — is the best thing in it. The automation boundary is
    correct in content and incomplete in reach: the controller cannot emit PASSED, but nothing
    constrains where it may write. WS-3 registration at S08 is approved and is the fix for GAP-D,
    which is a routing defect and therefore Architecture's to correct. WS-1 is re-parented as a
    supplier without any change to its stage. WS-1 criterion 4.7 moves from PARTIAL to BLOCKED
    under Rule GS-3, subject to Swapnali's concurrence.
  findings:
    - {id: A-F01, severity: A1, summary: "autopilot --output is an unconstrained write path; the CR-010 §2 boundary is documentary where it is writable"}
    - {id: A-F02, severity: A2, summary: "autopilot safety tests bind to live gate evidence rather than fixtures"}
    - {id: A-F03, severity: A2, summary: "R0-SCOPE A2 (self-service Day 1) contradicts WS-2 out-of-scope (no retail-customer auth)"}
    - {id: A-F04, severity: A2, summary: "context-manifest precedence omits docs/application-lifecycle-bible/"}
    - {id: A-F05, severity: A2, summary: "path-filtered workflow cannot serve as a required status check for S08-G2"}
    - {id: A-F06, severity: A2, summary: "id_allocation.sequential.ADR not advanced for ADR-001..003"}
    - {id: A-F07, severity: A3, summary: "no concurrency group on either workflow"}
  required_board_reviews:
    product: true
    technical: true
    security: true
    qa: true
    risk_compliance: true
    operations: true
  adr:
    required: true
    records: [ADR-001, ADR-002, ADR-003]
    location: "docs/platform/architecture-review/08-architecture-decision-log.md"
  debt_or_exception:
    required: true
    type: DEBT
    reason: >
      Four audit-schema additions (OPEN-I3) and the S07/S06 open items are recorded as debt with
      owners and targets. None is presented as target architecture.
  revisit_trigger: >
    GATE-S08 candidate; or any change to the automation boundary in CR-010 §2; or resolution of the
    customer-identity contradiction, which changes IF-2.
  next_action: >
    Close C-01, C-02 and C-09 before CR-010 binds. Obtain the human Architecture signature. Route
    the security architecture to Board 4 and the information model to Aarti.
  signature_status: "AI-DRAFTED — mandatory human signature outstanding"
```

---

**Persona:** Mahesh — Principal Insurance Platform Architect · Board 1 / R2
**signature_status:** `AI-DRAFTED — mandatory human signature outstanding`
**Date:** 2026-08-16
