# S15 — Operate, Evolve & Continuous Assurance

**AIGEM stage:** L10 — Operate & Evolve · **Owner:** Shivanshi (SRE) + Rajal (Product)
**Central question:** *Is it healthy, improving, and still compliant?*

---

## 1. Purpose

Run the platform well, learn from it, and keep it lawful. This is not a stage that ends — it is
the operating mode a mature enterprise platform lives in, and it has its own disciplines that are
easy to let lapse once the launch excitement passes.

Three failure modes it exists to prevent:

1. **Reliability decay** — features accumulate, reliability erodes, nobody owns the trend.
2. **Compliance drift** — controls certified once at S12 quietly stop operating, and nobody
   notices until an audit.
3. **Debt accumulation** — every increment adds debt, none is repaid, and the platform slowly
   becomes unchangeable.

## 2. Entry criteria

- [ ] GATE-S14 passed: live, with hypercare underway

## 3. Epics and stories

### S15-E01 — Operate · *Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S15-E01-S01 | Run the SLO reporting cadence | SLO attainment and error budget consumption reported on a fixed rhythm to Product and Delivery |
| S15-E01-S02 | Run incident management | Every incident classified, responded to within target, and recorded |
| S15-E01-S03 | Run blameless postmortems | For every O0 and O1, within 5 working days, with action items in the backlog |
| S15-E01-S04 | Track and reduce toil | Toil measured; automation prioritised against it |
| S15-E01-S05 | Manage capacity proactively | Trend-based forecasting; the Q4 tax-season peak planned for, not reacted to |
| S15-E01-S06 | Exit hypercare deliberately | Against the stated criteria, with a handover to steady-state operation |

### S15-E02 — Business operation · *Rajal*

| ID | Story | Acceptance criteria |
|---|---|---|
| S15-E02-S01 | Report KPIs against the S00 outcomes | Conversion, drop-off by step, time-to-issue, reconciliation completeness, RM adoption |
| S15-E02-S02 | Compare against the legacy baseline | Prove the business case, or discover it is not proving |
| S15-E02-S03 | Run the exception operations process | Unreconciled payments, failed issuance, stuck journeys — owned, queued, resolved |
| S15-E02-S04 | Gather user feedback systematically | From RMs and customers, routed into the backlog through triage |

### S15-E03 — Continuous assurance · *Shailja + Deepali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S15-E03-S01 | Run periodic control attestation | Each control C1–C10 re-evidenced on a defined cadence, not assumed to still operate |
| S15-E03-S02 | Run continuous security scanning | SAST, SCA, image and infrastructure scanning on a schedule as well as per change |
| S15-E03-S03 | Refresh the threat model | On any trust-boundary change, and at least annually |
| S15-E03-S04 | Run periodic access review | Who has access to what, in production and in the data stores; revoke by exception |
| S15-E03-S05 | Monitor regulatory change | Named owner, defined cadence, and a route from change to backlog |
| S15-E03-S06 | Verify retention and purge operation | Data disposed of at the horizon; immutability intact for what remains |
| S15-E03-S07 | Re-verify residency | On any infrastructure change |

### S15-E04 — Evolve · *Rajal + Mahesh + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S15-E04-S01 | Run the triage pipeline continuously | Every input classified through AIGEM; nothing enters the backlog untriaged |
| S15-E04-S02 | Run the unpark sweep | At every stage transition and on a periodic cadence; parked items re-triaged, never auto-admitted |
| S15-E04-S03 | Repay technical debt | A standing allocation per increment, prioritised by the ledger |
| S15-E04-S04 | Run architecture evolution review | Drift from intended architecture detected and either corrected or consciously accepted |
| S15-E04-S05 | Maintain the documentation canon | Staleness limits enforced; stale canonical documents are a defect, not a backlog nicety |
| S15-E04-S06 | Maintain the governance framework | AIGEM and this bible reviewed; changes via CR |

## 4. Validation tests

| ID | Validates | Method | Cadence | Pass condition |
|---|---|---|---|---|
| S15-VT-01 | SLOs are met | SLO report | Monthly | Attainment at target, or a recovery plan in place |
| S15-VT-02 | Controls still operate | Control attestation | Quarterly | 100% evidenced, not asserted |
| S15-VT-03 | DR still works | DR exercise | Annually, or on topology change | RTO/RPO met |
| S15-VT-04 | Restore still works | Restore test | Quarterly | Working state, within RTO |
| S15-VT-05 | Runbooks are still accurate | Spot execution | Quarterly | Procedures valid against current topology |
| S15-VT-06 | Alerts are still useful | Alert review | Quarterly | Every alert actionable; noisy alerts removed or fixed |
| S15-VT-07 | Access is appropriate | Access review | Quarterly | No unjustified access |
| S15-VT-08 | Debt is not accumulating | Ledger trend | Per increment | Flat or declining |
| S15-VT-09 | The business case is proving | KPI report vs baseline | Monthly | Outcomes trending to target, or an honest reassessment |
| S15-VT-10 | Documentation is fresh | Staleness check | Monthly | No canonical document past its limit |

## 5. Health gate — GATE-S15 (recurring, not terminal)

S15 has no exit. It has a **periodic health assessment** — quarterly is the recommended cadence —
that either confirms healthy operation or triggers corrective work.

| # | Criterion | Level | Evidence |
|---|---|---|---|
| S15-G1 | SLOs met or a recovery plan active | E4 | SLO report |
| S15-G2 | All controls attested current | E3 | Attestation record |
| S15-G3 | No overdue S0/S1 security findings | E4 | Scan and remediation report |
| S15-G4 | DR and restore verified within cadence | E3 | Exercise records |
| S15-G5 | Incident actions closed within target | E1 | Postmortem action tracker |
| S15-G6 | Debt ledger flat or declining | E1 | Ledger trend |
| S15-G7 | KPIs reported against the business case | E1 | KPI report |
| S15-G8 | Canonical documentation within staleness limits | E4 | Freshness check output |
| S15-G9 | Access review completed | E1 | Review record |

**Approvers:** Shivanshi (AP) · Rajal (AP) · Deepali (AP) · Shailja (AP) · Mahesh (RV) ·
Swapnali (RV) · Kalpana (RV)

**A failed health gate does not stop the platform.** It creates P1 corrective work with named
owners, and repeated failure of the same criterion escalates to the accountable human for that
domain.

## 6. Current position in this repository — ⚪ Not reached

Not applicable yet, with two exceptions worth noting because they are already operating and
should be preserved into S15:

| Practice | State |
|---|---|
| Technical debt ledger | 🟢 Operating — `TECH-DEBT.md` with severities and triage notes |
| Governance freshness check | 🟢 Operating — `FreshnessCheck.java` runs weekly in CI, enforcing staleness limits on governance state |
| Triage pipeline | 🟢 Operating — AIGEM is genuinely in use |
| Architecture drift control | 🟡 Documented in `17-DRIFT_CONTROL.md`; no automated enforcement outside ArchUnit |

**The freshness check is worth calling out.** It is exactly the right mechanism, applied to
governance documents. The realignment's S08 work extends the same idea to the application: a
check that runs on a schedule, fails visibly, and makes decay detectable rather than gradual.

## 7. Premature — or rather, out of place — at this stage

Unbounded rewrites · re-platforming without evidence · new programmes disguised as evolution.

S15's characteristic failure is the opposite of the earlier stages': not premature complexity, but
**unbounded change presented as maintenance**. A rewrite is a new programme and re-enters at S00
with a business case, not at S15 as an increment.
