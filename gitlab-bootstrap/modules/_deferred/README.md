# Deferred modules — one left, and it is blocked on content, not capability

`GLM-001` M3.4 listed three modules to defer. **Two have since been built**, because
`C-ARC-6` resolved the question they were waiting on.

## The tension, and how it resolved

The board round produced two positions that were not fully consistent:

- **Amit** deferred `branch-governance` and `environments` *"until `CR-016`"*, to avoid
  writing against a capability the instance lacks.
- **Mahesh** recorded **`C-ARC-6`**: *no M3 module may be omitted because CE cannot
  apply it; unavailable capabilities are declared and skipped, not deleted.*

`C-ARC-6` governs, and it dissolves the risk Amit was guarding against. The danger was
never *writing* a module for an absent capability — it was *applying* it. A capability
flag separates the two. Both modules are now built, express the approved model in full,
and apply only what `local.capabilities` permits.

Amit's own M3.8 position pointed the same way: *build the pipeline, implement the
protection as a manual job plus a restricted runner, and mark it as the compensating
control it is.* Build it; do not pretend it is equivalent.

| Module | Status |
|---|---|
| `branch-governance` | **BUILT.** Protected branches applied on CE; approval rules and CODEOWNERS approval declared, `count = 0` until the tier allows |
| `environments` | **BUILT.** Environments applied on CE; protection declared, applied only on Premium+ |
| `memberships` | **STILL DEFERRED** — see below |

## Why `memberships` is genuinely different

It is blocked on **`M1.4` / `ASM-014`**, and that is a *content* dependency, not a
capability one. The enterprise identity group names and IDs are unknown.

A capability flag cannot help here. Writing membership logic against guessed group names
produces a plan that **looks correct and grants the wrong access** — and unlike a missing
control, a wrong grant is active harm. There is nothing to declare-and-skip, because the
thing that is missing is the data, not the feature.

It is built when `M1.4` answers, and not before.
