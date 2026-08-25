# CR-012 — Board verdicts (drafts)

**Change request:** [CR-012 — R0 platform robustness](../../CR-012-r0-platform-robustness.md)
**Origin:** `SUG-20260824-gp1` … `gp5` · **Decisions:** `ADR-009` … `ADR-013`
**Index author:** Mahesh — Principal Insurance Platform Architect (Board 1 / R2), AI-drafted
**Date:** 2026-08-24

---

## 1. What is in this directory

One file per authority whose conclusion CR-012 needs and whose approval is **not** a notification.

> **Every file here is AI-drafted, and not one carries a human signature.** A file in this directory
> is a *drafted position for its named human to adopt, amend or reject*. It is not that person's
> conclusion and it does not exercise their authority. Where a persona holds a binding veto
> (Security, Risk & Compliance) or a mandatory T4 human signature, **the draft does not and cannot
> satisfy it** — and two of the items below are risk *acceptances*, which an agent may never make on
> someone's behalf.

Each file follows the same shape: **decision requested · what was reviewed · findings · draft
verdict · numbered conditions · what this draft may not do**.

## 2. Index

| File | Board / role | Persona | What is actually being asked | Draft position |
|---|---|---|---|---|
| [`board-4-security-deepali.md`](./board-4-security-deepali.md) | Board 4 — Security | Deepali | **Accept** `ADR-010` as a control; accept or reject `SEC-OPEN-7` and `SEC-OPEN-8`; review `TB-7`, the session vault and per-topic IAM | `APPROVE-WITH-MODIFICATION` (drafted) |
| [`board-6-compliance-shailja.md`](./board-6-compliance-shailja.md) | Board 6 — Risk & Compliance | Shailja | Sign the two evidence exclusions, or refuse them | `APPROVE-WITH-MODIFICATION` (drafted) |
| [`board-7-operations-shivanshi.md`](./board-7-operations-shivanshi.md) | Board 7 — Operations / SRE | Shivanshi | Whether four new layers are operable by this team at this maturity, and on what conditions | `APPROVE-WITH-MODIFICATION` (drafted) |
| [`dba-aarti.md`](./dba-aarti.md) | Database (specialist) | Aarti | The cache/store boundary, and that no system of record moves | `APPROVE-WITH-MODIFICATION` (drafted) |
| [`r12-delivery-kalpana.md`](./r12-delivery-kalpana.md) | R12 — Delivery Control | Kalpana | Sequencing, the external dependency, and whether the envelope is produced before it is spent | `APPROVE-WITH-MODIFICATION` (drafted) |

**Board 1 — Architecture has no separate file here, deliberately.** Its reasoning *is*
[`ADR-009` … `ADR-013`](../../../../platform/architecture-review/08-architecture-decision-log.md),
written in full with alternatives, negatives and revisit triggers. Duplicating it as a verdict would
create a second Architecture position to keep in sync. What remains outstanding for Board 1 is the
**mandatory human T4 signature**, which no file in this repository can supply.

**Board 3 — Product (Rajal) and Board 5 — QA (Swapnali) are notify, not approve.** No journey step,
acceptance criterion or actor changes. Rajal's interest is the pilot cost envelope (`RISK-012`);
Swapnali's is that seven new drills and seven new fitness functions arrive in the S09 evidence pack.
Neither is drafted here rather than being drafted badly.

## 3. The two things a reader should check first

1. **Does the set hold together if one ADR is refused?** `ADR-013` (search) is separable — a
   `SHOULD`, dropping it changes no other decision. `ADR-010` (inspection) runs over `ADR-009`'s
   Transit Gateway, so refusing the hub refuses the inspection design with it. CR-012 §9 states
   this so a partial verdict is coherent rather than contradictory.
2. **Is any invariant softened anywhere?** The drafts below all answer no, and each names the check
   that makes it verifiable rather than asserted: `FF-22` … `FF-28`. If a reviewer finds an
   invariant that is *only* protected by prose, that is the finding worth raising.
