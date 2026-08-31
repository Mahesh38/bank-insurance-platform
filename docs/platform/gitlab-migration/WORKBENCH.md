# File-level workbench — personal GitHub / Cursor → company GitLab

**Status:** Binding under [`ADR-020`](../../governance/registers/DECISION-REGISTER.md) / [`CR-017`](../../governance/change-requests/CR-017-orphan-import-and-file-workbench.md) `AC-7`
**Owner:** Shivanshi (mechanism) · Amit (what lands in `backend`/`frontend`) · Deepali (secrets on both trees)

Company GitLab will not allow external AI account MCPs. The personal GitHub repository stays a
**sandbox**. GitLab is the only git source of truth.

```text
Personal GitHub / Cursor   =  sandbox (AI MCPs allowed)
Company GitLab             =  only source of truth (bank identity, bank CI)
Direction                  =  tree snapshot, never commits
```

## Forbidden

- `git remote add` + `git fetch` / `git pull` / `git push --all` either way
- GitLab **pull mirroring** from GitHub
- Rewriting authors on a pre-push hook and fast-forwarding
- Dual-write of the same commit graph (`CR-014` rejected this; `AC-7` restates it)
- Putting regulated, production-like, or real premium/quote data on the personal GitHub
- Using a personal Gmail or an AI-vendor / Cursor / GitHub-noreply identity as
  the GitLab author

## Import (sandbox → GitLab)

On a machine with the **company** git identity configured:

```bash
# 1. Snapshot the sandbox tree (no .git)
git -C "$SANDBOX" archive HEAD | tar -x -C /tmp/workbench-drop

# 2. Apply onto a GitLab clone (example: backend)
rsync -a --delete \
  --exclude .git \
  --exclude .github \
  /tmp/workbench-drop/services /tmp/workbench-drop/libs \
  /tmp/workbench-drop/config /tmp/workbench-drop/gradle* \
  /tmp/workbench-drop/Dockerfile /tmp/workbench-drop/render.yaml \
  "$GITLAB_BACKEND"/

# 3. Identity guard, then one company-authored commit
python3 gitlab-bootstrap/scripts/identity-guard.py --tree "$GITLAB_BACKEND"
git -C "$GITLAB_BACKEND" add -A
git -C "$GITLAB_BACKEND" commit -m "Workbench import $(date +%F)"
python3 gitlab-bootstrap/scripts/identity-guard.py --git "$GITLAB_BACKEND"
git -C "$GITLAB_BACKEND" push origin HEAD:main   # or an MR branch
```

Adjust the rsync set per project (`frontend` = `apps/rm-workspace-app/`; `platform-governance` =
`docs/` `scripts/` `AGENTS.md` `CLAUDE.md` `.claude/`).

## Reverse (GitLab → sandbox)

Same mechanism, opposite direction, after a GitLab-native MR that the sandbox needs. Still a tree
copy. Never `git fetch gitlab`.

## Secret scanning

Scan **both** trees. Finding B class issues will appear on the sandbox first. A clean GitLab tree
scan is not evidence about the sandbox.

## Commit message convention on GitLab

`Workbench import YYYY-MM-DD` — not "created locally", not a copy of the sandbox commit subject
(those subjects name the personal GitHub login and AI-vendor session URLs).
