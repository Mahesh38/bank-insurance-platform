# CR-010 — Reusable Context Module and Safe Autopilot Foundation

**Change request:** CR-010  
**Date raised:** 2026-08-16  
**Status:** APPROVED-WITH-MODIFICATION (verdicts drafted) — all seven required boards plus Delivery and the DBA have returned a verdict; none rejected. **Not yet binding:** the T4 human signatures for Architecture, Security and Compliance remain outstanding. See [§4](#4-ratification) and the [verdict pack](./CR-010/verdicts/README.md).  
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

Every verdict below is recorded in full in [`CR-010/verdicts/`](./CR-010/verdicts/README.md).
**All nine are AI-DRAFTED.** An AI may draft and simulate a persona's reasoning where AIGEM
permits; it may never supply a mandatory human signature. The `Signature` column is therefore the
column that matters.

| Authority | Required conclusion | Verdict | Signature |
|---|---|---|---|
| [Mahesh / Architecture](./CR-010/verdicts/board-1-architecture-mahesh.md) | context boundaries, routing and automation design | APPROVED-WITH-MODIFICATION | **Human signature outstanding (T4, mandatory)** |
| [Rajal / Product](./CR-010/verdicts/board-3-product-rajal.md) | WS-3 adoption, lifecycle completion model and priority | APPROVE-WITH-MODIFICATION | AI-drafted |
| [Amit / Technical](./CR-010/verdicts/r3-engineering-amit.md) | scripts, CI and maintainability | APPROVED-WITH-CONDITIONS | AI-drafted |
| [Deepali / Security](./CR-010/verdicts/board-4-security-deepali.md) | automation permissions and protected-path boundary | APPROVED-WITH-CONDITIONS | **Human signature outstanding (T4, mandatory)** |
| [Swapnali / QA](./CR-010/verdicts/board-5-qa-swapnali.md) | evidence verification and CI sufficiency | APPROVED-WITH-MODIFICATION | AI-drafted |
| [Shailja / Compliance](./CR-010/verdicts/board-6-compliance-shailja.md) | non-waivable and human-signature boundaries | APPROVED-WITH-MODIFICATION | **Human signature outstanding (T4, mandatory)** |
| [Shivanshi / Operations](./CR-010/verdicts/board-7-operations-shivanshi.md) | CI/CD, scheduling, recovery and operational evidence | APPROVED-WITH-CONDITIONS | AI-drafted |
| [Kalpana / Delivery](./CR-010/verdicts/r12-delivery-kalpana.md) | sequencing, critical path, recovery increment as a funded line | APPROVED-WITH-MODIFICATION | AI-drafted |
| [Aarti / Database](./CR-010/verdicts/dba-aarti.md) | persistence, integrity, migrations, backup and DR | APPROVED-WITH-OBSERVATIONS | AI-drafted |

**No board rejected.** Product's verdict decides the four conclusions this CR specifically
required of it — the 16-stage lifecycle completion model (`APPROVE`), registration of WS-3
(`APPROVE`), WS-3 scope (`APPROVE-WITH-MODIFICATION`) and WS-3 priority including the stop on
WS-1 Phase 5 (`APPROVE`) — plus the S11 entry condition on GAP-006 and GAP-007 (`APPROVE`, on a
condition Product states it will not waive).

### 4.1 Conditions that are non-waivable by any authority

Carried from the verdicts, these three cannot be traded away by the board that raised them:

| Condition | Source | Why |
|---|---|---|
| No WS-3 stage enters S11 while GAP-006 or GAP-007 is open | Rajal C5 | Consent capture where legally required, and the suitability hard-gate before quote, are on the non-waivable list |
| Automation never supplies a board verdict, marks a stage `PASSED`, or treats silence as approval | Rajal C7, §2 above | This is the boundary that makes every other verdict meaningful |
| WS-1 Phase 5 does not start until GATE-S08 and GATE-S11 are `PASSED` | Rajal C6 | Adding LOBs to a quote path that lacks its lawful suitability gate multiplies a compliance defect |

### 4.2 What has and has not happened

**Done:** WS-3 is registered in `CURRENT-STATE.yaml` and `GATE-EVIDENCE.yaml` (Rajal condition
C2), WS-1 is re-parented as supplier, WS-2 is recorded as identity enabler, WS-1 criterion 4.7 is
`BLOCKED` on `S08-G3`, and the retroactive S00–S07 evidence pack exists.

**Not done:** the three mandatory human signatures. Registration is a state change; it is not
ratification, and it does not convert any AI-drafted verdict into an approval.

This CR may be implemented and reviewed on its branch. It does not become a human stage-transition
approval merely because its checks pass or its branch is technically mergeable.
