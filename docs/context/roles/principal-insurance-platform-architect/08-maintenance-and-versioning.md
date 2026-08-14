# 08 — Maintenance and Versioning

## 1. Purpose

This package is reusable grounding context, but it participates in a governed repository. Changes must remain aligned with AIGEM, Mahesh's baseline role and Shailja S's reciprocal protocol.

## 2. Versioning

Use semantic package versions:

- MAJOR — authority/ownership model, non-bypassable rules or review contract changes;
- MINOR — new competencies, decision rules or collaboration guidance without changing authority;
- PATCH — clarification, links, examples or wording with no semantic decision change.

Update the package version/date in `README.md` when a semantic change is accepted.

## 3. Review cadence

Review this package when any of the following occurs:

- AIGEM Architecture Board rules change;
- Mahesh's accountable role or delegated authority changes;
- Shailja S decision states/exception policy materially change;
- project architecture principles or accepted boundary model materially change;
- a recurring architecture-review failure shows a missing decision rule;
- the platform introduces a materially new delivery model such as NTB, new LoB, direct-insurer integration or consequential agentic automation;
- enterprise technology/security/compliance standards introduce a new mandatory constraint.

A calendar-only refresh is not required if no relevant source changed; staleness should be evaluated against source changes.

## 4. Reciprocal consistency checks

When changing this package, verify:

1. `docs/context/roles/mahesh-solution-architect.md` still attaches Mahesh to this package.
2. `docs/context/roles/shailja-s-compliance-risk-head/README.md` still points to the shared protocol.
3. Shailja's `08-agent-interaction-contract.md` and this package agree that control outcomes are compliance-owned and implementation design is architecture-owned unless mandated.
4. `docs/governance/11-REVIEW_GATES.md` still maps Board 1 to this persona and Board 6 to Shailja S.
5. AIGEM T4 human-sign-off requirements are not weakened.
6. Architecture `A0–A3`, compliance `R0–R3` and delivery `P1–P5` remain separate vocabularies.

## 5. Evidence/source precedence

This persona is non-binding context. If it conflicts with authoritative repository sources, follow repository precedence:

- current human instruction/approved change control;
- project current-state/scope/accepted decisions;
- organization standards;
- AIGEM framework;
- repository implementation/contract evidence;
- persona guidance;
- agent judgement.

Never use this persona to fabricate an architecture constraint that is absent from authoritative sources.

## 6. Learning from decisions

A new architecture lesson should enter this package only when it is general enough to improve future decisions. Project-specific outcomes belong primarily in ADRs/current-state/decision registers and may be referenced as examples.

Do not convert one project incident or one vendor quirk into a universal architecture principle without evidence that it generalizes.

## 7. Change governance

Changes to persona files are normal documentation changes, but any accompanying modifications to `docs/governance/**` remain subject to AIGEM change-control rules. An agent may prepare those changes on a review branch; required human approvals must still be recorded before the governance change is treated as ratified/binding.
