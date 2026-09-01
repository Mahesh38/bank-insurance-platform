# DEC-20260901-01 — Owner authorises the recommended unblock path

**Status:** **`AUTHORISED` 2026-09-01** — recorded from `human:Mahesh` (repository owner).
**Recorded by:** `agent:cursor`. The agent recorded an instruction the owner gave; it did not
supply one (Rule CC-1, Rule PA-1, [09 §8](./09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)).
**Date:** 2026-09-01
**Workstream:** WS-3 — AU Bank Insurance Distribution Platform
**Stage:** S08 — Engineering Foundation (`GATE-S08` `OPEN`), S09 overlapped
**Origin:** human:Mahesh — *authorise you to take the recommended actions… no traceback to GitHub…
no AI commits… fresh commit on day 0*
**Related:** [`CR-017`](./change-requests/CR-017-orphan-import-and-file-workbench.md) ·
[`CR-016`](./change-requests/CR-016-gitlab-ce-control-model-gap.md) ·
[`ADR-020`](../platform/architecture-review/08-architecture-decision-log.md)
**Freshness:** `state_as_of` is 22 days old — `FreshnessCheck` exit `1` (WARN). Review due 2026-09-09.

---

## 1. What this file is, and is not

**Is:** the owner's instruction to execute the 2026-09-01 recommended unblock path, and a restatement
that the GitLab graph must be a **fresh start** under company identity.

**Is not:** a human T4 Architecture, Security or Risk & Compliance signature · a waiver of
Finding B / `C-CMP-1` · authority to push to GitLab · a claim the work was created on a company
laptop. The sealed bundle (`AC-8`) remains the honest provenance of the pre-import history.

---

## 2. Authorised actions

| # | Action | Agent may | Human still must |
|---|---|---|---|
| 1 | Finding B — assemble retirement evidence; default to formal retire **if** no live consumer and no decryptable row | Draft the pack | Operator attestation · Aarti row check · Deepali `RETIRED` or `ROTATED` |
| 2 | `C-CMP-1` — send the residency questionnaire | Draft and file the questions | Bank infra answers · Shailja rules `PERMISSIBLE` |
| 3 | Push `pre-gitlab-migration` at `b8027751738b04d00dbe071a77b2aba56828a2cd` | Attempt from this environment | Operator with tag-write scope if this credential is `403` |
| 4 | T4 signatures on the existing `CR-014` / `CR-017` packs | Assemble the slip | Mahesh, Deepali, Shailja sign |
| 5 | `CR-016` Option B now, A as target, C until EE, D rejected | Record + add the compensating CI component | Deepali human sign-off on the weaker boundary |
| 6 | GitLab day-0 graph: one company-authored orphan commit, no GitHub / AI identity | Enforce in `identity-guard` and `migrate-repositories.sh` | Company `user.name` / `user.email` at `PUSH=1` |

---

## 3. GitLab appearance (binding on the import)

Each receiving project:

- exactly **one** commit, **no parents**
- company `GIT_AUTHOR_*` / `GIT_COMMITTER_*` only
- subject **`Initial commit`**
- `identity-guard.py` clean on the tree and on that commit
- no `.github/`, no `m2-evidence/`, no `CLAUDE.md`, no `.claude/`

Ongoing contribution from the personal GitHub sandbox remains **file-level only** (`AC-7`).
Subsequent GitLab commits are company-authored on a developer machine or bank CI — never
`git fetch` of the personal graph.

---

## 4. What was skipped (ADMIT-BYPASS)

A second written seven-board pack was not produced. The owner authorised the previously
recommended path in one instruction. Non-negotiables are **not** waived: Finding B and
residency still gate the first push. T4 signatures stay human.
