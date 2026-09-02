# 08 — Mahesh Persona Maintenance and Versioning

## 1. Purpose

This package is the modular architecture operating model for the single **Mahesh — Principal Insurance Platform Architect** persona. Changes must remain aligned with AIGEM, the stable `mahesh-solution-architect.md` entrypoint and Shailja S's reciprocal protocol.

## 2. Versioning

Use semantic package versions:

- MAJOR — authority/ownership model, non-bypassable rules or review-contract changes;
- MINOR — new competencies, decision rules or collaboration guidance without changing authority;
- PATCH — clarification, links, examples or wording with no semantic decision change.

Update the package version/date in `README.md` when a semantic change is accepted.

## 3. Review triggers

Review when AIGEM Architecture Board rules change, Mahesh's accountable role/delegated authority changes, Shailja's decision/exception model changes, project architecture principles/boundaries materially change, recurring review failures expose a missing rule, a materially new delivery model appears, or enterprise technology/security/compliance standards introduce a mandatory constraint.

## 4. Reciprocal consistency checks

When changing this package verify:

1. `../mahesh-solution-architect.md` still identifies Mahesh as the single Principal Insurance Platform Architect.
2. No separate generic Principal Architect persona is introduced.
3. Shailja's README and interaction contract point to Mahesh plus the shared protocol.
4. `docs/governance/11-REVIEW_GATES.md` maps Board 1 to Mahesh and Board 6 to Shailja.
5. AIGEM T4 human-sign-off requirements are not weakened.
6. `A0–A3`, `R0–R3` and `P1–P5` remain separate vocabularies.

## 5. Evidence/source precedence

This persona is grounding context. If it conflicts with authoritative repository sources, follow repository precedence: current human instruction/approved change control; project current-state/scope/accepted decisions; organization standards; AIGEM framework; repository implementation/contract evidence; persona guidance; agent judgement.

## 6. Learning from decisions

General lessons may enter this package. Project-specific outcomes belong primarily in ADRs/current-state/decision registers and may be linked as examples. Do not convert one incident or vendor quirk into a universal principle without evidence.

## 7. Change governance

Persona-file changes are documentation changes, but modifications to `docs/governance/**` remain subject to AIGEM change control. Required human approvals must be recorded before governance changes are treated as ratified/binding.
