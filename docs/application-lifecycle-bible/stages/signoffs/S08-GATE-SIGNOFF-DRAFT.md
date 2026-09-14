# Gate Sign-off — S08 Engineering Foundation (DRAFT — awaiting humans)

> **DRAFT.** Evidence pre-filled from autopilot `STAGE_TRANSITION_CANDIDATE`.
> All `decision` fields are `PENDING_HUMAN`. Agents must not change them to `APPROVED`
> or set `outcome.decision: PASSED` (`docs/governance/04-STAGE_GATES.md` §5).
>
> When complete: rename to `S08-GATE-SIGNOFF-YYYY-MM-DD.md` and have Architect + PO jointly pass the gate.

```yaml
gate_signoff:
  stage: S08
  gate_id: GATE-S08
  transition: "S08 → PASSED"          # set only after human joint pass
  date: null                          # human fills on PASS
  marked_candidate_by: "agent / autopilot propose-transition"
  candidate_date: 2026-09-14
  freeze_in_effect: false             # Delivery Lead may set true when formally marking CANDIDATE
```

## 1. Evidence table

| # | Criterion | Required level | Evidence (summary) | State | Verified by |
|---|---|---|---|---|---|
| S08-G1 | CI builds and tests every module on every PR | E4 | Pipeline definition: .github/workflows/application-ci.yml (c4e8d9f, 2026-08-16). Triggers on pull_request with no paths filter and on push to every branch, so a working branch is evidenced before a PR exists. Module cove | MET | criterion owner (see CURRENT-STATE) |
| S08-G2 | Merge to main impossible without a green pipeline | E4 | The four status checks that must be required are named exactly in the application-ci.yml header and matching security-scanning.yml job names: "Java 21 tests and coverage gates", "Secret scanning (gitleaks)", "SAST (CodeQ | MET | criterion owner (see CURRENT-STATE) |
| S08-G3 | Coverage thresholds enforced; QA-001 closed | E4 | Enforcement: build.gradle.kts wires jacocoTestCoverageVerification into `check` and CI names the task explicitly, so a future rewiring cannot silently drop the gate. Thresholds in force: libs 80% line / 70% branch; Phase | MET | criterion owner (see CURRENT-STATE) |
| S08-G4 | ArchUnit and static analysis enforced | E4 | ArchUnit breadth: 18 modules carry an architecture test (ServiceArchitectureTest in 17 platform services plus ArchitectureTest in 1sb-integration-service). Rules bite: 1sb-integration-service carries ArchitectureRulesBit | MET | criterion owner (see CURRENT-STATE) |
| S08-G5 | Secret, SAST, SCA and image scanning in the pipeline | E4 | Secret scanning: gitleaks runs on the working tree and blocks the build; a scheduled job scans full git history separately. SAST: CodeQL Java analysis runs and fails the build on critical or high findings. | MET | criterion owner (see CURRENT-STATE) |
| S08-G6 | Test infrastructure operational at every pyramid level | E4 | Shared harness library: libs/bank-common-test — PostgresTestSupport (Testcontainers), WireMockHarness (dual 1SB+persistence servers), DomainFixtures, PyramidTags / ContractTags / E2ETags. Unit level: DomainFixturesTest + | MET | criterion owner (see CURRENT-STATE) |
| S08-G7 | No PII in logs, proven by automated test | E4 | Component proof: NoPiiInErrorOutputTest and ErrorDiagnosticAndRedactionTest in bank-common-error (EPIC-001, ADR-017); PiiMaskerTest in 1sb-integration-service. Framework emission scrubber: LogPiiScrubber applied by Slf4j | MET | criterion owner (see CURRENT-STATE) |
| S08-G8 | Engineering and secure coding standards published and adopted | E4 | Published standard: docs/governance/ENGINEERING-AND-SECURE-CODING-STANDARDS.md (ENG-1..10, SEC-C1..C10) — L3 repo standard building on ORG-STANDARDS.md. Adoption mechanism: docs/governance/PR-REVIEW-CHECKLIST.md cites th | MET | criterion owner (see CURRENT-STATE) |
| S08-G9 | Pipeline feedback under 10 minutes at p95; flake under 1% | E4 | Measurement script: scripts/governance/measure-pipeline-feedback.py (fetch via gh api against .github/workflows/application-ci.yml). Committed snapshot: scripts/governance/evidence/S08-G9-pipeline-feedback.json. Governan | MET | criterion owner (see CURRENT-STATE) |
| S08-G10 | A new engineer can build, test and ship in under a week | E4 | Human pack assembled 2026-09-13: scripts/governance/evidence/S08-G10-onboarding-human-pack.md (5-day path, reading list, attestation template). Day-0.5 / day-1 / day-2 dry-runs already logged under scripts/governance/evi | MET | criterion owner (see CURRENT-STATE) |

**States:** `MET` · `NOT MET` · `WAIVED`

## 2. Board verdicts — PENDING_HUMAN

```yaml
approvals:
  - persona: "Amit / Engineering"
    board: TECHNICAL
    reviewer_type: HUMAN
    self_review: false
    decision: PENDING_HUMAN
    must_fix: []
    conditions: []
    evidence: []
    date: null

  - persona: "Swapnali / QA"
    board: QA
    reviewer_type: HUMAN
    self_review: false
    decision: PENDING_HUMAN
    must_fix: []
    conditions: []
    evidence: []
    date: null

  - persona: "Mahesh / Architecture"
    board: ARCHITECTURE
    reviewer_type: HUMAN
    self_review: false
    decision: PENDING_HUMAN
    must_fix: []
    conditions: []
    evidence: []
    date: null

  - persona: "Deepali / Security"
    board: SECURITY
    reviewer_type: HUMAN            # mandatory HUMAN at T4 — no AI substitution
    self_review: false
    decision: PENDING_HUMAN
    must_fix: []
    conditions: []
    evidence: []
    date: null

  - persona: "Shivanshi / SRE"
    board: OPERATIONS
    reviewer_type: HUMAN
    self_review: false
    decision: PENDING_HUMAN
    must_fix: []
    conditions: []
    evidence: []
    date: null

```

## 3. Conditions carried forward

| Condition | From board | Owner | Backlog ID | Due |
|---|---|---|---|---|
| | | | | |

## 4. Waivers

None proposed by the agent candidate.

## 5. Outcome — not yet declared

```yaml
outcome:
  decision: PENDING_HUMAN           # humans set PASSED | OPEN | BLOCKED
  blocking_items: []
  passed_by: []                     # must be Architect + PO jointly when PASSED

  post_transition_actions:
    current_state_updated: false    # docs/governance/state/CURRENT-STATE.yaml — human only
    unpark_sweep_run: false
    decision_register_entry: ""
    position_banner_updated: false

  parked_items_released: []
  next_stage: S09
```

## 6. Notes

- Autopilot reports `may_mark_passed: false` and `human_approval_required: true`.
- Criterion evidence pack: `scripts/governance/evidence/S08-STAGE_TRANSITION_CANDIDATE.yaml`.
- G2/G10 closure evidence landed via merged PR #106.
- WS-1 Phase 5 remains separately gated (Rajal C6: GATE-S08 **and** GATE-S11) — closing S08 alone does not authorise Phase 5 LOB expansion.

