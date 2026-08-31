# Identity sanitization — denylist for the GitLab import

**Status:** Binding under [`CR-017`](../../governance/change-requests/CR-017-orphan-import-and-file-workbench.md) `AC-6`
**Enforced by:** [`gitlab-bootstrap/scripts/identity_guard.py`](../../../gitlab-bootstrap/scripts/identity_guard.py)
**Owner:** Shivanshi (SRE) executes · Deepali (Security) owns the denylist · Shailja (Compliance) owns provenance wording

## 1. What must not appear on GitLab

### Git metadata (author, committer, message)

| Pattern | Why |
|---|---|
| Personal GitHub login | The login recorded in the sealed bundle; the regex is in `identity-guard.py` |
| Personal Gmail | Same — the regex is in `identity-guard.py`, not repeated here |
| AI-vendor noreply / session URLs / Cursor agent identity | Same |
| GitHub noreply committer / personal-login merge subjects | Same |

### Working tree

Same identity strings in files. Two known hits, redacted on 2026-08-31:

- `docs/au-bank-insurance-platform/references/2026-08-20-insurance-aggregation-and-provider-connectivity-notes.md`
- `docs/au-bank-insurance-platform/references/2026-08-20-north-star-architecture-brainstorming-notes.md`

### Allowed in the tree

Upstream OSS URLs (`github.com/gradle`, `github.com/gitleaks`, …) are not personal-forge identity.
The guard's `--tree` scan uses the identity denylist, not a blanket `github.com` ban.

## 2. What is not imported into GitLab

| Path | Why |
|---|---|
| `.github/` | GitHub Actions; CI is re-expressed as `ci-components` |
| `docs/platform/gitlab-migration/m2-evidence/` | Embeds committer emails and the personal login in scan JSON. Lives in the sealed bundle only |
| Personal git history | Orphan import. Sealed as `git bundle` offline (`AC-8`) |

## 3. Provenance wording

The GitLab first-commit message is **Initial import of the bank insurance platform**. It is not
"created on this machine". Board 6: stripping personal-forge metadata is hygiene; claiming a false
origin is not.

## 4. Guard invocation

```bash
python3 gitlab-bootstrap/scripts/identity-guard.py --tree /path/to/orphan-worktree
python3 gitlab-bootstrap/scripts/identity-guard.py --git  /path/to/orphan-repo
# exit 0 clean · exit 1 hits printed on stderr
```

`migrate-repositories.sh` refuses to push unless both pass. `PUSH=1` cannot override a guard failure.
