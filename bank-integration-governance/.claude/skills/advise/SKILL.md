---
name: advise
description: Run persona advisors (product, analyst, architecture, engineering, security, data, quality, compliance, reliability, flow-steward) in parallel on a pull request, design or decision, and collect ok / concern / blocker-candidate advice. Use when opening or reviewing an R1 or R2 PR, when preparing a Type-1 decision brief, or when asked what a given role would say about a change. Advice only — never an approval.
---

# Advise

Source rules: `docs/06-assurance.md` §3, `personas/README.md`.

## Pick advisors
| Label / situation | Advisors |
|---|---|
| R0 | optional — the lens closest to the change |
| R1 | architecture + quality + every lens whose checklist the diff touches |
| R2 | every owner lens of the touched controls (docs/06 §2) + architecture + quality |
| Type-1 decision brief | the decision's `advisors` list in state/decisions.yaml |

## Run each advisor in its own context, in parallel
Load: the advisor's card + the lane pack's Summary + the diff or brief. Nothing else.
Answer only the card's checklist. Output exactly:

```text
ADVISOR <lens>
Verdict: ok | concern | blocker-candidate
Findings: <failure scenario: input/state → wrong outcome>, one per line, most severe first
Control/guardrail touched: <ids or none>   Change is: alters | near | none
```

## Reconcile (the maker or human decider does this, not an advisor)
- `blocker-candidate` → route to the human owner of that control/guardrail (roster.yaml).
  An AI advisor cannot block; it cannot approve either.
- Conflicting advice → conflict ladder, docs/05-decisions.md §6.
- Attach all advisor output to the PR or decision record, including advice you did not follow.
