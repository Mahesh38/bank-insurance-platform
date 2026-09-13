# Board 5 — QA · Draft verdict on CR-016

**Board:** 5 — QA · **AIGEM role:** R7  
**Reviewer:** Swapnali — Principal Insurance Quality Engineering / QA Lead  
**Reviewer type:** AGENT (AI simulation)  
**Self-review:** false  
**Change request:** [CR-016](../../CR-016-north-star-and-service-workstream-strategy.md)  
**Date:** 2026-09-13

> ## Draft: `APPROVE_WITH_CONDITIONS`
> **Quality view:** operating model can improve traceability of blockers; it does **not** create test evidence by itself.
>
> **`signature_status: AI-DRAFTED`**

---

## 1. Checklist

| # | Check | Result |
|---|---|---|
| Q1 | Are claims of Done falsifiable? | **Not yet** for sync checks |
| Q2 | Does this weaken journey regression ownership? | Risk if service-green replaces journey-green |
| Q3 | Is board update itself evidenced? | Only via git history today |
| Q4 | Any fabricated evidence in this PR? | No — docs only |

---

## 2. Findings / concerns

**Concern 1 — “Done” inflation.** A service board marking Completed without linking to failing/passing tests or a plan AC is not quality evidence. I will not treat board Completed rows as GATE-S08 evidence.

**Concern 2 — Sync check standard missing.** CR-016 names contract / shared AC / DEP edge but does not define the **minimum evidence artefact**. Until that exists, cross-service Done is not auditable.

**Concern 3 — Parallelism vs critical journey.** Multiple green `SWS-*` boards can coexist with a red assisted-Life path. Programme exit still needs journey-level evidence packs I own for sufficiency.

---

## 3. Conditions (`must_fix`)

1. Before agents scale cross-SWS claims: publish a one-page **sync-check evidence bar** (what file/path/test id counts).
2. Completed rows that claim behaviour change must link to test report or AC id.
3. Board hygiene is never a substitute for GATE-S08 / QA-001 style gates.

## 4. Signature status

`AI-DRAFTED`. Board 5 has not sat as a human.
