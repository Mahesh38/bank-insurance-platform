# DEC-20260831-01 — Orphan import into GitLab and file-level AI workbench

**Status:** **`APPROVED` 2026-08-31** — board outcome relayed by `human:Mahesh` and recorded.
**Recorded by:** `agent:claude`. The agent recorded a decision the owner gave; it did not supply one
(Rule CC-1, Rule PA-1).
**Date:** 2026-08-31
**Workstream:** WS-3 — AU Bank Insurance Distribution Platform
**Stage:** S08 — Engineering Foundation (`GATE-S08` `OPEN`), S09 overlapped
**Origin:** human:Mahesh — *As board member we all Accept Option 1 + file-level workbench*
**Scope CR:** [`CR-017`](./change-requests/CR-017-orphan-import-and-file-workbench.md) amends [`CR-014`](./change-requests/CR-014-gitlab-estate-migration.md)
**ADR:** [`ADR-020`](../platform/architecture-review/08-architecture-decision-log.md)
**Plan:** [`GLM-001`](../platform/gitlab-migration/GLM-001-migration-plan.md) M5.2 / M5.10 rewritten
**Workbench SOP:** [`WORKBENCH.md`](../platform/gitlab-migration/WORKBENCH.md)
**Freshness:** `state_as_of` is 21 days old — `FreshnessCheck` exit `1` (WARN). Review due 2026-09-09.

---

## 1. What this file is, and is not

**Is:** the record of the identity-sanitization decision that replaces `CR-014` constraint 2, and
the standing rule for any later AI workbench.

**Is not:** a human T4 Architecture, Security or Risk & Compliance signature · authority to push to
GitLab · a waiver of Finding B / `C-CMP-1` · a claim that the code was created on a company laptop.

The GitLab first commit is an **initial import of pre-existing engineering work** with personal-forge
metadata removed. Matching blobs on the personal GitHub, if it still exists, are expected; they are
not a defect of this decision. A false-origin story would be.

---

## 2. The decision

### 2.1 GitLab import — Option 1

We will **not** push the 358-commit personal GitHub graph. Each receiving project gets one orphan
commit of the current path-split tree, authored under a company git identity. `identity-guard.py`
must pass. Terraform still creates empty projects; the orphan commit is the first content.

### 2.2 Ongoing workbench — file-level only

Company GitLab will not allow external AI MCPs. Personal GitHub / Cursor remains a sandbox.
Contribution back to GitLab is a **tree drop**, committed on GitLab with company identity. Git
remotes are never fetched between the two.

### 2.3 Options considered and rejected

| Option | Why not |
|---|---|
| Preserve history (`CR-014` constraint 2) | Copies personal Gmail, GitHub merges, Anthropic and Cursor authorship into the bank SoR |
| Rewrite authors, keep the graph | SHAs change (so no git sync) and a 358-commit timeline still does not look local |
| GitLab pull-mirror / `git push --all` | Re-imports identity; `CR-014` already rejected git dual-write |
| Do nothing | First push under the approved plan is the leak |

---

## 3. Conditions carried forward

`AC-1`…`AC-5` stand. New: `AC-6` (orphan only), `AC-7` (file-level workbench), `AC-8` (sealed bundle).

Hard blocks on the first push, unchanged: Finding B rotation (`C-SEC-2`), residency (`C-CMP-1`),
source full-history scan already executed (`C-SEC-1` fired).

---

## 4. What was skipped (ADMIT-BYPASS risk)

A full second seven-board written-verdict pack was not produced for `CR-017`. The owner relayed
unanimous board acceptance of the PR 82 review recommendation in one instruction. **Risk:** T4
human signature artefacts were not independently witnessed. Recorded under
[09 §8](./09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process). Non-negotiables
(secrets, PII, provenance) are **not** waived: Finding B and residency still gate the push.
