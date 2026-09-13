# S08-G2 — Human action pack (branch protection)

**Criterion:** Merge to `main` is impossible without a green pipeline.  
**Mode:** `HUMAN_REQUIRED` · Owner: Amit / Engineering · Verifier: `human-review`  
**Assembled / refreshed:** 2026-09-13T20:21Z (agent) · **Not a MET declaration**

## Why an agent cannot close this

Classic branch-protection read returns `403` for this integration:

```text
gh api repos/Mahesh38/bank-insurance-platform/branches/main/protection
→ HTTP 403 Resource not accessible by integration
```

Ruleset **create** and **update** also return `403` (probed 2026-09-13T20:16Z):

```text
POST /repos/Mahesh38/bank-insurance-platform/rulesets
PUT  /repos/Mahesh38/bank-insurance-platform/rulesets/20028494
→ HTTP 403 Resource not accessible by integration
```

Rulesets **are** readable. Current repo ruleset:

| id | name | enforcement | rules |
|----|------|-------------|-------|
| `20028494` | No force push allowed | **disabled** | `deletion`, `non_fast_forward` |

There is **no** active ruleset requiring status checks on `main`. Enabling required checks
(and a blocked-merge demo) remains a human admin action.

## Prerequisite now satisfied (green tip pipeline)

Branch `cursor/close-s08-g3-eb1b` tip `2da9ce3` (2026-09-13) — all required-named checks **success**:

| Workflow run | Conclusion | URL |
|---|---|---|
| Application CI `34780660671` | success | https://github.com/Mahesh38/bank-insurance-platform/actions/runs/34780660671 |
| Security Scanning `34780660696` | success | https://github.com/Mahesh38/bank-insurance-platform/actions/runs/34780660696 |
| Governance (AIGEM) `34780660661` | success | https://github.com/Mahesh38/bank-insurance-platform/actions/runs/34780660661 |
| Knowledge Hub `34780660629` | success | https://github.com/Mahesh38/bank-insurance-platform/actions/runs/34780660629 |

A green pipeline is necessary but **not sufficient** for S08-G2 — admin must still require the four checks below on `main`.

## Required status checks (exact job names — require these strings)

Captured 2026-09-13 from tip `2da9ce3` and `main` tip `8369a9a` check-runs:

| Check name (require exactly) | Observed |
|---|---|
| `Java 21 tests and coverage gates` | success |
| `Secret scanning (gitleaks)` | success |
| `SAST (CodeQL, Java)` | success |
| `SCA (Trivy dependency scan)` | success |

Optional (present but not part of the original four-name contract):

| Check name | Notes |
|---|---|
| `SBOM (CycloneDX)` | supply-chain artefact; recommend once G2 four are required |
| `Schemas, records and links` / `State freshness (JDK baseline)` / `Verify Git-native Knowledge Hub` | governance; keep if already used as soft gates |
| `Container image scan (Trivy) — *` | image matrix; useful after G2 four |

## Admin steps (GitHub UI or Rulesets)

1. Settings → Rules → New branch ruleset targeting `main` (preferred over legacy branch protection).
2. Enforce: **Require a pull request before merging** (≥1 review recommended).
3. Enforce: **Require status checks to pass** — add the four names above **exactly**.
4. Enforce: **Require branches to be up to date before merging**.
5. Block force pushes / deletions (enable existing ruleset `20028494` or fold into the new one).
6. Do **not** allow administrator bypass without a dated break-glass note.
7. Save with enforcement **Active**.

### Copy-paste ruleset JSON (admin token / UI advanced)

```json
{
  "name": "Require GATE-S08 CI checks",
  "target": "branch",
  "enforcement": "active",
  "conditions": {
    "ref_name": { "include": ["refs/heads/main"], "exclude": [] }
  },
  "rules": [
    {
      "type": "required_status_checks",
      "parameters": {
        "strict_required_status_checks_policy": true,
        "required_status_checks": [
          { "context": "Java 21 tests and coverage gates" },
          { "context": "Secret scanning (gitleaks)" },
          { "context": "SAST (CodeQL, Java)" },
          { "context": "SCA (Trivy dependency scan)" }
        ]
      }
    },
    { "type": "non_fast_forward" },
    { "type": "deletion" }
  ]
}
```



## Who must act (confirmed 2026-09-13)

Repo collaborator API shows **`Mahesh38`** with `role_name=admin` (agent token has no admin). Only that account (or another admin PAT) can run:

```bash
# from repo root, with an admin PAT:
export GH_TOKEN=<admin-pat-with-repo-admin>
./scripts/governance/evidence/S08-G2-apply-required-checks.sh
./scripts/governance/evidence/S08-G2-verify-required-checks.sh   # expect exit 0
```

Until that runs, `S08-G2-verify-required-checks.sh` exits 1 and S08-G2 stays OPEN.

## Helper scripts (2026-09-13)

| Script | Who | Purpose |
|---|---|---|
| `scripts/governance/evidence/S08-G2-verify-required-checks.sh` | anyone / agent | Read-only GET probe — exit 1 until the four checks are enforced (latest log: `S08-G2-verify-required-checks.out`) |
| `scripts/governance/evidence/S08-G2-apply-required-checks.sh` | **repo admin** | Creates the active required-checks ruleset (`DRY_RUN=1` to print payload). Agent token gets HTTP 403. |

After apply: re-run verify (expect exit 0), then do the blocked-merge demo below before declaring MET.

## Blocked-merge demonstration (E4)

After protection is on:

1. Open a throwaway PR that fails one required check (or use a known red run).
2. Confirm Merge is blocked (UI and/or `gh api` merge attempt fails).
3. Store screenshot or transcript next to this file as `S08-G2-blocked-merge.*`.
4. Amit seat declares **S08-G2 MET** on `human-review` with that evidence.

## Related CI notes (2026-09-13)

1. `application-ci.yml` uses `fetch-depth: 0` + `origin/main` fetch so Spotless
   `ratchetFrom("origin/main")` works on GitHub runners.
2. `libs/bank-common-test` JaCoCo raised to libs floor (80% line / 70% branch) —
   tip Application CI success after covering `register()` + long-prefix `idempotencyKey`.
3. Neither change closes S08-G2 without the admin ruleset above.
