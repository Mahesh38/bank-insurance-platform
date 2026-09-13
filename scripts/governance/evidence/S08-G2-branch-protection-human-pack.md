# S08-G2 — Human action pack (branch protection)

**Criterion:** Merge to `main` is impossible without a green pipeline.  
**Mode:** `HUMAN_REQUIRED` · Owner: Amit / Engineering · Verifier: `human-review`  
**Assembled / refreshed:** 2026-09-13 (agent) · **Not a MET declaration**

## Why an agent cannot close this

Classic branch-protection read returns `403` for this integration:

```text
gh api repos/Mahesh38/bank-insurance-platform/branches/main/protection
→ HTTP 403 Resource not accessible by integration
```

Rulesets **are** readable. Current repo ruleset:

| id | name | enforcement | rules |
|----|------|-------------|-------|
| `20028494` | No force push allowed | **disabled** | `deletion`, `non_fast_forward` |

There is **no** active ruleset requiring status checks on `main`. Enabling required checks
(and a blocked-merge demo) remains a human admin action.

## Required status checks (exact names from live `main` tip)

Captured 2026-09-13 from `GET .../commits/<main-sha>/check-runs` on tip `8369a9a…`:

| Check name (require exactly) | Observed on main tip |
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

## Admin steps (GitHub UI or Rulesets)

1. Settings → Rules → New branch ruleset targeting `main` (preferred over legacy branch protection).
2. Enforce: **Require a pull request before merging** (≥1 review recommended).
3. Enforce: **Require status checks to pass** — add the four names above **exactly**.
4. Enforce: **Require branches to be up to date before merging**.
5. Block force pushes / deletions (can enable existing ruleset `20028494` or fold into the new one).
6. Do **not** allow administrator bypass without a dated break-glass note.
7. Save with enforcement **Active**.

## Blocked-merge demonstration (E4)

After protection is on:

1. Open a throwaway PR that fails one required check (or use a known red run).
2. Confirm Merge is blocked (UI and/or `gh api` merge attempt fails).
3. Store screenshot or transcript next to this file as `S08-G2-blocked-merge.*`.
4. Amit seat declares **S08-G2 MET** on `human-review` with that evidence.

## Related CI note (2026-09-13)

`application-ci.yml` now uses `fetch-depth: 0` + `origin/main` fetch so Spotless
`ratchetFrom("origin/main")` works on GitHub runners (shallow clones previously failed
with `No such reference 'origin/main'`). That restores the green-pipeline *mechanism*;
it does not close S08-G2 without the admin ruleset above.
