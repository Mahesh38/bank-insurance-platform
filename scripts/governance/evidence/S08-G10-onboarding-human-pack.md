# S08-G10 — New engineer ship-in-a-week pack

**Criterion:** A new engineer can build, test and ship a change in under a week.  
**Mode:** `HUMAN_REQUIRED` · Owner: Amit / Engineering · Verifier: `human-review` · E3  
**Assembled:** 2026-09-13 · **Not a MET declaration**

## Timed path (target ≤ 5 working days)

| Day | Outcome |
|-----|---------|
| 0.5 | Clone, JDK 21, `./gradlew test` green on laptop; read `docs/context/BOOT.md` |
| 1 | Run local Phase-1 pair (`bank-persistence-service` + `1sb-integration-service`); hit actuator health |
| 2 | Read ENGINEERING-AND-SECURE-CODING-STANDARDS + PR-REVIEW-CHECKLIST; shadow one PR review |
| 3–4 | Own a small change (docs or test) through PR using the template checklist |
| 5 | Change merged (or merge-ready); engineer records wall-clock in the attestation below |

### Agent timing sample (not a MET)

2026-09-13 on tip `2da9ce3`:

- Day-0.5 dry-run log: `scripts/governance/evidence/S08-G10-day0.5-dry-run.log`
- Day-1 local boot dry-run: `scripts/governance/evidence/S08-G10-day1-local-boot-dry-run.log` — `bank-persistence-service` :8081 and `1sb-integration-service` :8080 both `/actuator/health` **UP** (local profile + documented local-only secrets)
- Day-2 dry-run log: `scripts/governance/evidence/S08-G10-day2-dry-run.log` — `ENGINEERING-AND-SECURE-CODING-STANDARDS.md` + `PR-REVIEW-CHECKLIST.md` + PR template present; PR #104 used as shadow-review surface (checklist smoke only)
- Reading list paths all present (`BOOT.md`, standards, PR checklist, `AGENTS.md`)
- `./gradlew :libs:bank-common-error:test :services:bank-persistence-service:test :services:1sb-integration-service:test` → **BUILD SUCCESSFUL in 18s** wall-clock (warm deps)
- Full estate CI under S08-G9 (p95 ≈ 2.8 min); tip Application CI `34780660671` + Security Scanning `34780660696` both **success**

Remaining G10 gap is a **named human** completing days 3–5 + the attestation below — not missing docs, build time, or day-2 reading material.

## Required reading (ordered)

1. `docs/context/BOOT.md`
2. `docs/governance/ENGINEERING-AND-SECURE-CODING-STANDARDS.md`
3. `docs/governance/PR-REVIEW-CHECKLIST.md`
4. `AGENTS.md` / service SSOT testing rules for the module touched


## Who must act (confirmed 2026-09-13)

Repo collaborator API lists only **`Mahesh38`** (`role_name=admin`). Until another engineer is added, G10 closure requires that account (or a named engineer they designate) to:

1. Follow the 5-day path (or compress with the day-0.5 / day-1 dry-run evidence already on this branch).
2. Copy `S08-G10-onboarding-attestation.TEMPLATE.md` → `S08-G10-onboarding-attestation.md`, fill it, and commit.
3. Declare **S08-G10 MET** on `human-review`.

Agent dry-runs already logged (not MET):
- `S08-G10-day0.5-dry-run.log` — Phase-1 unit tests ~18s
- `S08-G10-day1-local-boot-dry-run.log` — persistence + 1sb-integration actuator health UP
- `S08-G10-day2-dry-run.log` — standards + checklist + PR template; PR #104 shadow-review smoke

## Attestation (human)

```text
Engineer: _______________
Buddy / reviewer: _______________
Start date: _______________
First merged PR: _______________
Elapsed working days: _______________
Blockers encountered: _______________
Amit confirmation (date): _______________
```

Attach the filled attestation (or PR link + dates) under
`scripts/governance/evidence/S08-G10-onboarding-attestation.md` and declare S08-G10 MET
on `human-review`. Until then this criterion stays OPEN.
