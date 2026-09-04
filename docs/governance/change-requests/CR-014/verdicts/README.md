# CR-014 — Board positions

**Change request:** [CR-014 — Migrate the platform from personal GitHub to the company GitLab estate](../../CR-014-gitlab-estate-migration.md)
**Plan:** [`GLM-001`](../../../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Change tier:** `T4` · **Date:** 2026-08-29

> ### These are inputs to the boards, not approvals
> Every file here is **AI-drafted**. [Rule CC-1](../../../14-CHANGE_CONTROL.md#3-procedure): an agent
> may raise a change request and may never approve one. [BOOT §3](../../../../context/BOOT.md): the
> mandatory **human** T4 Architecture, Security and Risk & Compliance signatures cannot be satisfied
> by AI simulation. An agent may draft the reasoning and assemble the evidence; it may not
> manufacture the signature.
>
> `CR-014` §10 records `decision: PENDING` and `approvers: []`. That is the true state.

## Reading order

Read Security and Compliance first — they carry blocking authority, and both raise findings the
other boards' conditions depend on.

| # | Board / role | Persona | File | Drafted verdict | Severity |
|---|---|---|---|---|---|
| 1 | Board 4 — Security `B` | Deepali | [`board-4-security-deepali.md`](./board-4-security-deepali.md) | `APPROVE-WITH-CONDITIONS` | `S1` |
| 2 | Board 6 — Risk & Compliance `B` | Shailja S | [`board-6-compliance-shailja.md`](./board-6-compliance-shailja.md) | `APPROVE-WITH-CONDITIONS` | `R2` |
| 3 | Board 1 — Architecture | Mahesh | [`board-1-architecture-mahesh.md`](./board-1-architecture-mahesh.md) | `APPROVE-WITH-CONDITIONS` | `A2` |
| 4 | Board 7 — Operations | Shivanshi | [`board-7-operations-shivanshi.md`](./board-7-operations-shivanshi.md) | `APPROVE-WITH-CONDITIONS` | `O1` |
| 5 | `R3` — Engineering | Amit | [`r3-engineering-amit.md`](./r3-engineering-amit.md) | `APPROVE-WITH-CONDITIONS` | — |
| 6 | Board 5 — QA `B` | Swapnali | [`board-5-qa-swapnali.md`](./board-5-qa-swapnali.md) | `APPROVE-WITH-CONDITIONS` | `Q1` hold |
| 7 | `R12` — Delivery | Kalpana | [`r12-delivery-kalpana.md`](./r12-delivery-kalpana.md) | `CANDIDATE` | `DL1` |

Board 3 — Product (Rajal) was **not** convened: CR-014 changes no product scope, journey, rule, LOB
or acceptance criterion. Aarti (Database) is **not** an approver here; her jurisdiction is engaged by
[`CR-015`](../../CR-015-shared-persistence-vs-bank-baseline.md) and cannot be satisfied on this CR.

## Two findings this round produced that GLM-001 did not have

| # | Finding | Raised by | Consequence |
|---|---|---|---|
| **CMP-F01** | **Data residency is unresolved.** The standing constraint forbids regulated data, backups, logs *or archives* outside AWS India regions. If the bank GitLab is GitLab.com rather than self-managed in-region, repository content, CI logs and job artifacts sit outside India — and no phase of GLM-001 asks the question | Shailja (Board 6) | New blocking input on M1.2; `RISK-021`; recorded as **IMP-14** |
| **OPS-F01** | **Cutover +24 h to archive the GitHub origin is too tight.** It is shorter than the rollback-validation window and shorter than any realistic dispute about what the archive holds | Shivanshi (Board 7), correcting her own plan | GLM-001 M9.4 revised to +14 days restorable, then archive |
