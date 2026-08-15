# Validation Test — S<xx>-VT-<nn>

> A validation test proves a **stage** is complete. It is distinct from a functional test, which
> proves a *story* works. Stage validation asks: does the set of delivered work actually add up to
> the stage's outcome?

```yaml
validation_test:
  id: S08-VT-01
  stage: S08
  validates: "CI actually blocks bad code"
  serves_gate_criteria: [S08-G1, S08-G2]
  owner: "Swapnali / QA"
  evidence_level: E4          # E3 or E4 for behaviour; E1/E2 only for definition criteria
  cadence: once               # once | per-release | quarterly | annually
  status: NOT_RUN             # NOT_RUN | PASS | FAIL | BLOCKED
```

## 1. What this proves

One or two sentences on the property being established, and why the stage cannot be considered
complete without it.

Prefer tests that **deliberately break a rule and confirm the machine notices**. A pipeline that
runs but never blocks anything is theatre, and only a negative test distinguishes the two.

## 2. Method

Reproducible steps, precise enough for someone who has never run it.

```
1.
2.
3.
```

**Environment:** where this runs
**Prerequisites:** what must be true first
**Data:** what data is used, and confirmation it is PII-free

## 3. Pass condition

Unambiguous and measured, not judged.

> Example: "PR feedback completes within 10 minutes at p95 across 20 consecutive runs"
> Not: "the pipeline is acceptably fast"

## 4. Result

```yaml
result:
  date:
  executed_by:
  outcome:                  # PASS | FAIL
  measured_value:           # where the pass condition is numeric
  evidence_link:            # CI run, report, recording — something openable
  observations:
```

## 5. On failure

| Field | Value |
|---|---|
| Failure mode | |
| Root cause | |
| Backlog item raised | |
| Blocks gate criterion | |
| Retest date | |

A failed validation test does not fail the stage on its own — it names the work required to pass
it. What it must never do is quietly become a lower-strength piece of evidence for the same
criterion.

## 6. Re-execution

| Trigger | Required |
|---|---|
| Stage re-entered | Yes |
| Related architecture changed | Yes |
| Gate evidence older than 30 days at transition | Yes |
| Recurring cadence elapsed | Yes, per `cadence` above |
