# S08-G2 — Human action pack (branch protection)

**Criterion:** Merge to `main` is impossible without a green pipeline.  
**Mode:** `HUMAN_REQUIRED` · Owner: Amit / Engineering · Verifier: `human-review`  
**Assembled:** 2026-09-13 (agent) · **Not a MET declaration**

## Why an agent cannot close this

Branch protection on `main` is a repository-administrator setting. This token received
`403 Resource not accessible by integration` on
`GET /repos/Mahesh38/bank-insurance-platform/branches/main/protection` (2026-09-13).
A green pipeline is the mechanism; requiring those checks on `main` is what makes it a gate.

## Required status checks (exact names)

From workflow headers / job `name:` fields — require these on `main` for PRs:

| Check name (must match Actions check run name) | Workflow |
|---|---|
| `Java 21 tests and coverage gates` | `.github/workflows/application-ci.yml` |
| `Secret scanning (gitleaks)` | `.github/workflows/security-scanning.yml` (confirm job `name:` in UI if renamed) |
| `SAST (CodeQL, Java)` | `.github/workflows/security-scanning.yml` |
| `SCA (Trivy dependency scan)` | `.github/workflows/security-scanning.yml` |

Also consider requiring the image-scan / Checkstyle jobs once stable, but the four names above
are what `application-ci.yml` already documents as the S08-G2 contract.

## Admin steps (GitHub UI)

1. Settings → Branches → Branch protection rule for `main` (or Rulesets).
2. Enable **Require a pull request before merging**.
3. Enable **Require status checks to pass before merging**.
4. Enable **Require branches to be up to date before merging** (recommended).
5. Add the four check names exactly as listed (copy from a green PR Checks tab if unsure).
6. Do **not** allow administrators to bypass (or document a time-boxed break-glass exception).
7. Save.

## Blocked-merge demonstration (E4)

After protection is on:

1. Open a throwaway PR that fails one required check (or wait for a red run).
2. Confirm the Merge button is blocked / API merge returns failure.
3. Attach screenshot or `gh pr view` + merge attempt transcript to this folder as
   `S08-G2-blocked-merge.*`.
4. Amit seat declares **S08-G2 MET** on `human-review` with that evidence.

## Agent probe (2026-09-13)

```text
gh api repos/Mahesh38/bank-insurance-platform/branches/main/protection
→ HTTP 403 Resource not accessible by integration
```

Interpretation: either protection exists but the integration cannot read it, or the agent
lacks admin scope. A human with admin must confirm and attach the settings export / screenshot.
