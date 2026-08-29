# Deferred modules — blocked, not abandoned

Three modules from `GLM-001` M3.4 are deliberately **not built yet**. They are
recorded here so their absence is a decision with a reason rather than a gap.

Per **`C-ARC-6`**: a module is never omitted *because CE cannot apply it*. These
three are deferred because a **decision they depend on is open**, which is a
different thing.

| Module | Blocked on | Why it cannot be written now |
|---|---|---|
| `branch-governance` | **`CR-016`** (D3) | Expresses the risk-based approval matrix of baseline §6.3. CE has no required-approval or CODEOWNERS-approval capability, so `CR-016` decides whether this becomes a licence upgrade, a compensating CI control, or a recorded exception. Each produces a different module. |
| `environments` | **`CR-016`** (D3) | Baseline §9.3 wants DEV→DR protected with approvers. Protected environments are Premium+. Same three-way decision. |
| `memberships` | **`M1.4`** (`ASM-014`) | Enterprise identity group names and IDs are unknown. Writing membership logic against guessed group names produces a plan that looks correct and grants the wrong access. |

**When they are built**, `locals.capabilities` already carries the flags they read
(`merge_request_approval_rules`, `code_owner_approval`, `protected_environments`).
The wiring exists; the modules do not.

Deferral recorded in `DEC-20260829-02` §2 (Amit's sequencing position) and in
`GLM-001` M3.4.
