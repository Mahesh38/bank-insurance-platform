# CR-010 — Reusable Context Module and Safe Autopilot Foundation

**Change request:** CR-010  
**Date raised:** 2026-08-16  
**Status:** CANDIDATE — implementation authorised on a review branch; ratification and stage transitions remain subject to required human/specialist approvals  
**Origin:** SUG-20260816-ap1  
**Plan:** PLAN-001  
**Change type:** GOV / DOC / INFRA / QA  
**Runtime impact:** CI and repository automation only; no production deployment or business API change

## 1. Decision requested

Adopt a portable context-module architecture and a safe autopilot control plane that can:

1. validate context, governance, lifecycle and routing semantics as well as syntax;
2. keep project/domain facts separate from reusable reasoning and authority contracts;
3. scaffold the same context structure for another project or domain;
4. keep productive work moving when one item or external dependency is blocked;
5. prepare evidence-backed stage-transition proposals without fabricating approval;
6. remove duplicated compatibility content while preserving stable paths;
7. establish application CI as a prerequisite for current hardening evidence.

## 2. Non-negotiable automation boundary

Automation may evaluate evidence, select a READY item, record a blocker, mark/propose
`CANDIDATE`, open a review package and update derived artefacts after an authorised merge.

Automation may never independently:

- mark a stage `PASSED`;
- provide Product, Architecture, Security, QA, Compliance or Operations approval;
- accept material risk or create an open-ended waiver;
- weaken a binding control because a reviewer or dependency is late;
- treat silence as approval.

## 3. Structural changes

- `docs/context/framework/` becomes the reusable, domain-neutral context engine.
- `docs/context/context-manifest.yaml` declares the active project overlay, sources and personas.
- `scripts/context/` validates and scaffolds context modules.
- work routing becomes workstream-aware and every configured path is validated.
- gate evidence becomes structured separately from human-owned lifecycle stage fields.
- generated lifecycle backlog drift is checked in CI.
- compatibility aliases are reduced to redirects; canonical packages remain the only maintained content.

## 4. Ratification

| Authority | Required conclusion | Status |
|---|---|---|
| Mahesh / Architecture | context boundaries, routing and automation design | PENDING — branch implementation was requested by the repository owner; that request is not an Architecture verdict |
| Rajal / Product | WS-3 adoption, lifecycle completion model and priority | PENDING |
| Amit / Technical | scripts, CI and maintainability | PENDING |
| Deepali / Security | automation permissions and protected-path boundary | PENDING |
| Swapnali / QA | evidence verification and CI sufficiency | PENDING |
| Shailja / Compliance | non-waivable and human-signature boundaries | PENDING |
| Shivanshi / Operations | CI/CD, scheduling, recovery and operational evidence | PENDING |

This CR may be implemented and reviewed on its branch. It does not become a human stage-transition
approval merely because its checks pass or its branch is technically mergeable.
