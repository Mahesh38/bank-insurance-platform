# 13 — Definition of Done

**Layer:** L1 (model) + L3 (project gates)
**Owner:** Tech Lead (technical) · QA Lead (quality) · PO (business)
**Companion:** [ROLE-GUIDELINES-AND-DOD.md](../1sb-insurance-integration/service-ssot/ROLE-GUIDELINES-AND-DOD.md) — role-level DoD, still binding

---

## 1. Principle

> **Done means proven, not finished.** Every criterion is satisfied by an *artefact* — a test
> run, a report, a review record, a document — never by an assertion.

The repository's existing rule already says it: *"Done means automated proof + review, not
'code pushed'."* This file adds the governance closure that turns a completed change into a
closed loop.

---

## 2. Universal DoD (every work item)

| # | Criterion | Evidence |
|---|-----------|----------|
| D1 | All acceptance criteria pass, including conditions added by review boards | Test run / demo |
| D2 | Required tests exist at the agreed levels | Test files + CI run |
| D3 | Build and gates green: `./gradlew test jacocoTestReport jacocoTestCoverageVerification` | CI link or local output |
| D4 | Architecture boundaries hold (ArchUnit green) | CI |
| D5 | No PII, secrets, or credentials in code, config, logs, or test data | Masking test / review note |
| D6 | Plan's `out_of_scope` respected; diff matches `files_expected` ± logged variances | [17](./17-DRIFT_CONTROL.md) drift check |
| D7 | Public contract changes reflected in OpenAPI / SSOT docs | Updated spec |
| D8 | New deliberate shortcuts logged as `TD-###` with owner, severity, expiry | [15](./15-TECH_DEBT_POLICY.md) |
| D9 | Every `TODO`/`FIXME` added carries a work item ID | Grep / review |
| D10 | Registers updated: suggestion closed, dependency edges resolved, risks updated | [08](./08-BACKLOG_RULES.md) |
| D11 | Commits reference the work item ID | Git history |
| D12 | Handoff recorded: next role can act without asking | Status doc / PR body |

---

## 3. Additional DoD by work type

| Type | Adds |
|------|------|
| `FUNC` | PO-demonstrable; QA cycle per [WORK-SEQUENCE.md](../1sb-insurance-integration/service-ssot/WORK-SEQUENCE.md) (or an explicit, recorded variance); audit event emitted where the story requires |
| `BUG` | Root cause stated; regression test that **fails before the fix**; scan for the same defect class elsewhere |
| `NFR` | Target stated **and measured**; measurement method repeatable and recorded |
| `SEC` | Security verdict closed; negative tests (denied paths) present; fails closed |
| `COMP` | Compliance verdict closed; audit/attribution evidence captured; retention respected |
| `ARCH` | ADR merged and indexed; boundaries enforced by a test, not a convention |
| `DEBT` | Ledger entry closed with the actual remedy, or rolled with a new expiry and a reason |
| `REFACTOR` | Behaviour provably unchanged (tests unchanged and green); no contract change |
| `QA` | Gate/threshold updated in [COVERAGE.md](../1sb-insurance-integration/service-ssot/COVERAGE.md) if it moved |
| `SPIKE` | Written answer; follow-on items raised and triaged; prototype code **not** merged to main |
| `MIGRATION` | Cutover and rollback both exercised; data verified before and after |
| `OPS` | Runbook updated; alert tested at least once |
| `DOC` | Links resolve; superseded content removed, not merely appended to |
| `GOV` | Version bumped; decision register entry; agent entry points updated |

---

## 4. Governance closure

A change is not Done until the loop closes. This is the step teams skip, and it is why the
same suggestion arrives four times.

```text
[ ] Origin SUG-#### marked CLOSED-DELIVERED, linked to the PR
[ ] Parked items this change unblocked have been re-triaged (07, 08 §5)
[ ] Dependency edges this change satisfied are marked resolved (DEP-###)
[ ] New risks discovered are in the risk register (RISK-###)
[ ] Assumptions validated or invalidated (ASM-###)
[ ] Suggestions raised during implementation are registered (09 §5)
[ ] Gate criteria this change advances are updated in 04-STAGE_GATES.md
[ ] Metrics inputs recorded: rework rounds, drift incidents, plan variances (18)
```

---

## 5. Who declares Done

| Work type | Declared by |
|-----------|-------------|
| `FUNC` (functional story) | Tech Lead, after QA Lead sign-off |
| `BUG` | Tech Lead, after regression test verified |
| `NFR`, `ARCH`, `INFRA`, `OPS` | Tech Lead (+ Architect for `ARCH`) |
| `SEC`, `COMP` | Tech Lead **and** the relevant board reviewer |
| `QA` | QA Lead |
| `DEBT` | Tech Lead (ledger owner) |
| `DOC`, `SPIKE` | Author + one reviewer |
| `GOV` | Architect + PO |
| **AI agent** | **Never declares Done unilaterally** — it produces the evidence table and requests closure |

An agent's honest report of a partial outcome is worth more than a premature Done. If gates
are red, say they are red and show the output.

---

## 6. Partial and blocked completion

| Situation | Correct handling |
|-----------|------------------|
| Some AC pass, others blocked externally | Item stays open; split the passing part into its own Done item if independently shippable |
| Implementation complete, QA cycle not run | Not Done. Record the variance explicitly (as `phase-4/STATUS.md` does) — never silently |
| Done except a follow-up improvement | Done. The improvement is a new `SUG-####` |
| Done but a debt item was created | Done, provided the debt is registered with owner, severity, and expiry |
| Gate criterion met but evidence not captured | Not Done. Evidence is the criterion |

---

## 7. Done checklist for an agent to paste

```text
Work item:      NFR-011  (origin SUG-0043, plan PLAN-011)
AC:             AC-1 ✅  AC-2 ✅  AC-3 ✅   (+ SEC condition C1 ✅)
Tests:          12 added · unit + integration · negative cases included
Gates:          test ✅  jacoco verification ✅  ArchUnit ✅
Drift:          files_expected honoured; 1 variance logged (PaymentSessionMapper.java)
Debt:           none created
Registers:      SUG-0043 CLOSED-DELIVERED · DEP-031 resolved · no new risks
Docs:           OpenAPI updated · SSOT unchanged
Gate impact:    advances Phase 4 exit criterion 4.1
Requesting:     Tech Lead closure
```
