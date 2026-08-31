# CR-017 — Orphan import into GitLab; file-level AI workbench

**Date:** 2026-08-31
**Type:** PLAN (amends approved `CR-014` constraint 2) with `SEC` and `COMP` consequences
**Raised by:** `agent:claude` on the previous-turn review of PR 82; origin `SUG-20260831-ids`
**Workstream:** WS-3 · **Stage:** S08 with S09 overlapped
**Related:** [`CR-014`](./CR-014-gitlab-estate-migration.md) · [`GLM-001`](../../platform/gitlab-migration/GLM-001-migration-plan.md) · [`ADR-020`](../../platform/architecture-review/08-architecture-decision-log.md)
**Authority:** [14 §4](../14-CHANGE_CONTROL.md#4-changing-an-approved-plan) — *approach replaced with a different mechanism* → re-review by the boards that approved `CR-014`

> ## Decision: `APPROVED_WITH_CONDITIONS` — 2026-08-31
> Relayed by `human:Mahesh` (repository owner) as **"As board member we all Accept Option 1 +
> file-level workbench."** The agent recorded a decision the owner gave; it did not supply one
> (Rule CC-1, Rule PA-1).
>
> **This is not a T4 signature artefact.** Architecture, Security and Risk & Compliance human
> sign-offs remain mandatory and outstanding in the same sense as `CR-014`: the owner-relayed
> outcome authorises the *plan change*; it does not waive `C-SEC-1` / `C-SEC-2` / `C-CMP-1`, and
> it does not authorise the first push.
>
> **What changed.** `CR-014` constraint 2 ("preserve history, authorship and dates via
> `git filter-repo`") is **superseded**. GitLab receives an **orphan / squash first commit** per
> project under a company git identity. Personal GitHub may continue as an AI workbench by
> **file-level one-way import only** — never `git fetch` / `git push --all` / GitLab pull-mirroring.

---

## 1. Why this needs a CR

`CR-014` is approved. Changing how history lands is not a variance: it replaces the approved
mechanism ([14 §4](../14-CHANGE_CONTROL.md#4-changing-an-approved-plan)). The same seven boards
that approved constraint 2 must re-approve its replacement.

Grounds:

| # | Ground | Trigger |
|---|---|---|
| 1 | Approach replaced | History-preserving `filter-repo` → orphan import |
| 2 | Security impact | Personal-forge identity, AI-vendor trailers and a personal Gmail leave the bank SoR |
| 3 | Compliance / audit | Provenance of the GitLab first commit is an **initial import**, not a false "built here" claim; the original history is sealed offline, not destroyed by this CR |

---

## 2. Current position (`CR-014` as approved)

| Document | What it says |
|---|---|
| `CR-014` §3 row 2 | Split frontend / backend "preserving history, authorship and dates via `git filter-repo`" |
| `GLM-001` M5.2 | Push with history, authorship, dates |
| `GLM-001` M5.10 | Verify history and authorship **preserved** |
| `CR-014` rejected option | "Mirror to GitLab, keep GitHub writable" — two sources of truth |

Measured on 2026-08-31 (all refs in this clone): 233 commits authored with a personal Gmail;
106 commits authored by the AI-vendor noreply identity; 104 by the Cursor cloud-agent identity;
78 GitHub merge committers; 101 AI co-author trailers; 101 AI session URLs; 67 GitHub merge
subjects naming the personal login. A history-preserving push copies all of that into the bank estate.

---

## 3. Decision

### 3.1 Option 1 — orphan import (GitLab first commit)

For each GitLab project that would have received migrated history (`frontend`, `backend`,
`platform-governance`):

1. Rotate or formally retire Finding B (`C-SEC-2`) **before** any import. Orphaning HEAD does not
   retire a key that may still decrypt `raw_payload`.
2. Seal a `git bundle` of the personal origin **offline**. It is not pushed to GitLab. Disposition
   is Board 6 (`C-CMP-4`) after Finding B is closed.
3. Copy the **current tree** (path-split) into a new orphan branch. One commit. Company
   `user.name` / `user.email` only.
4. Run the identity denylist (`identity-guard.py`) on the tree and on that commit. Refuse to push
   on any hit.
5. Frame the commit message as **Initial import**, not as local-from-scratch development.

### 3.2 File-level workbench (ongoing)

| | |
|---|---|
| GitLab | Only git source of truth. Company identity. Bank CI. |
| Personal GitHub / Cursor | AI sandbox. External MCPs allowed because the company GitLab will not permit them. |
| Join | Tree snapshot (`git archive` / rsync excluding `.git`). One company-authored commit on GitLab per import. |
| Forbidden | `git fetch` either way · GitLab pull-mirroring · rewritten-history fast-forward · dual-write of commits |

This does **not** reopen the `CR-014` rejection of git dual-write. File-level import is a different
mechanism: SHAs never match, and that is required.

### 3.3 What this CR does not do

- Does not push to GitLab.
- Does not rotate Finding B.
- Does not confirm data residency (`C-CMP-1`).
- Does not manufacture T4 signatures.
- Does not claim the work was created on a company laptop. The honest label is **initial import of
  pre-existing engineering work, personal-forge metadata removed**.

---

## 4. Approval conditions (`AC-6` … `AC-8`)

Added to `CR-014`. The original `AC-1`…`AC-5` stand.

| ID | Condition | Gates |
|---|---|---|
| `AC-6` | GitLab receives **orphan first commits only**. No personal-forge history, authors, committers, trailers or GitHub merge subjects. `identity-guard.py` must exit 0 on every project before push | **M5.2** |
| `AC-7` | Any ongoing contribution from a personal GitHub / Cursor workbench is **file-level one-way import** onto GitLab under company identity. Git-object sync is forbidden | Continuous |
| `AC-8` | The original history is a **sealed offline bundle**, not a GitLab remote. Destroy or legal-hold only after Finding B is closed and Board 6 names the disposition (`C-CMP-4`) | **M5.2** create · **M9.4** dispose |

---

## 5. Conditions that still block the first push

Unchanged from `CR-014`:

- `C-SEC-1` — source full-history secret scan (already fired on Finding B)
- `C-SEC-2` — rotate/retire Finding B, then import. The orphan commit is a *tree* of HEAD; it does
  not replace rotation
- `C-CMP-1` — data residency confirmed permissible
- Tree gitleaks of the orphan commit must be clean (HEAD today has no Finding B; confirm, do not assume)

---

## 6. Change request record

```yaml
change_request:
  id: CR-017
  raised_by: "agent:claude"
  date: 2026-08-31
  type: PLAN
  driver: "owner-relayed board acceptance of Option 1 + file-level workbench after PR 82 identity review"
  evidence:
    - "PR 82 review 2026-08-31 — identity leak scan (233 personal Gmail authors, 101 AI-vendor session trailers, 67 personal-login merge subjects)"
    - "CR-014 constraint 2 — preserve history, authorship and dates"
    - "CR-014 rejected option — Mirror to GitLab, keep GitHub writable"
    - "C-SEC-1 fired on RAW_PAYLOAD_ENCRYPTION_KEY in main history (Finding B)"
  impact:
    scope: "no product scope change"
    stage: "no GATE-S08 criterion waived; G1/G2/G5/G9 still re-evidenced on GitLab (AC-1)"
    dependencies: "amends CR-014 M5.2/M5.10; Finding B and C-CMP-1 still gate the first push"
    parked_items: "none"
    effort: "M"
    risk_if_rejected: >
      First GitLab push copies personal Gmail, GitHub merge commits and Anthropic/Cursor
      authorship into the bank SoR.
  decision: APPROVED_WITH_CONDITIONS
  approvers: ["Board 1 Architecture", "Board 4 Security", "Board 5 QA", "Board 6 Risk & Compliance", "Board 7 Operations", "R3 Engineering", "R12 Delivery"]
  decided_on: "2026-08-31"
  recorded_by: "agent:claude, from a board outcome relayed by human:Mahesh on 2026-08-31"
  conditions:
    - "AC-6 orphan first commits only; identity-guard must pass"
    - "AC-7 file-level workbench only; git-object sync forbidden"
    - "AC-8 sealed offline bundle; disposition after Finding B + C-CMP-4"
    - "C-SEC-1, C-SEC-2, C-CMP-1 remain hard blocks on the first push"
  signature_status: >
    Board acceptance relayed by human:Mahesh 2026-08-31 and recorded. This is not a T4
    signature artefact. CR-014 verdicts/ remain AI-drafted inputs.
```
