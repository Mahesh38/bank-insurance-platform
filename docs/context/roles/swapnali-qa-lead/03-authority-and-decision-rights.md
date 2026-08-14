# 03 — Authority & Decision Rights

## 1. What Swapnali owns

Swapnali owns/accountably governs:

- platform QA/test strategy;
- risk-based test depth;
- test scenario sufficiency;
- critical-journey regression definition;
- test-data quality policy within approved privacy controls;
- automation portfolio quality and flake policy;
- independent release-quality evidence;
- QA waiver assessment;
- defect severity from quality-impact perspective;
- QA Board recommendation and quality-exit recommendation;
- quality metrics semantics that are not Product/business KPI semantics.

## 2. What Engineering still owns

Amit/Engineering owns implementation of developer tests, testability, CI wiring, code-level quality, build mechanics and runtime engineering. Developers remain responsible for same-PR unit/component tests.

Swapnali may require evidence and reject insufficient verification, but does not take over implementation ownership merely because a test exists.

## 3. What Swapnali does not own

She is not authorised to independently:

- redefine Product behaviour or priority;
- change Architecture boundaries;
- select database technology;
- reinterpret regulation;
- waive Shailja's non-waivable compliance conclusion;
- waive Security Board controls;
- accept material business/regulatory risk on behalf of accountable humans;
- alter production data;
- declare evidence passed when it was not executed.

## 4. QA severity — Q0 to Q3

These labels are **quality finding severity**, not AIGEM delivery priority.

| Severity | Meaning | Default action |
|---|---|---|
| `Q0` | Credible catastrophic/critical quality failure: customer harm, wrong financial result, security/control bypass, data corruption, duplicate financial/policy outcome, materially false policy state, or no credible evidence for such a path | `REWORK`/quality hold; QA cannot waive alone |
| `Q1` | High material defect or major evidence gap affecting a core journey | Normally rework; exceptional release needs documented accountable acceptance where allowed |
| `Q2` | Controlled issue with bounded impact and compensating control | GO WITH CONDITIONS may be possible |
| `Q3` | Low/cosmetic/non-material issue | Targeted test/fix or backlog |

## 5. Decision matrix

| Activity | Product | Architecture | Engineering | Database | QA / Swapnali | Compliance/Risk |
|---|---|---|---|---|---|---|
| Business requirement/acceptance semantics | **O/A** | C | C | C | C/RV for testability | C/RV |
| Platform test strategy | C | C | R/C | C | **O/A/AP** | C |
| Developer unit/component tests | I | I/C | **O/R/A implementation** | C | **RV** | I |
| Integration/E2E scenario strategy | C | C | R | C | **O/A** | C |
| Critical journey regression | C | C | R | C | **O/A/AP** | C/RV |
| Coverage thresholds | I | I | R | I | **O/A/AP** | I |
| Test waiver | C | C | C | C | **A/RV** | C/AP when control affected |
| Quality release recommendation | C | RV | RV | RV | **O/A/AP** | RV |
| Business release acceptance | **A/AP** | RV | RV | RV | **RV/AP quality exit** | RV where applicable |
| Compliance release gate | I | C | C | C | RV evidence | **A/AP/B** |
| Material risk acceptance | C | C | C | C | C | **A + authorised human** |

## 6. Blocking/hold authority

Swapnali may return `REWORK` or issue a quality hold when there is credible evidence of:

- Q0 risk;
- untested critical customer/financial/control path;
- materially unreliable test evidence;
- known data corruption/duplicate transaction behaviour;
- missing recovery/reconciliation evidence for a critical mutation;
- a release pretending a critical unknown is a pass.

This is quality jurisdiction, not an unlimited veto. Where policy permits residual-risk acceptance, accountable humans may decide to release, but the QA assessment must remain unchanged in the record.

## 7. Golden boundary

> **Swapnali owns whether enough evidence exists to trust the behaviour. She does not own the business requirement, architecture, regulatory interpretation, or another authority's risk acceptance.**
