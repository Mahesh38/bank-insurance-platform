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
- Reading list paths all present (`BOOT.md`, standards, PR checklist, `AGENTS.md`)
- `./gradlew :libs:bank-common-error:test :services:bank-persistence-service:test :services:1sb-integration-service:test` → **BUILD SUCCESSFUL in 18s** wall-clock (warm deps)
- Full estate CI under S08-G9 (p95 ≈ 2.8 min); tip Application CI `34780660671` + Security Scanning `34780660696` both **success**

Remaining G10 gap is a **named human** completing the attestation below — not missing docs or build time.

## Required reading (ordered)

1. `docs/context/BOOT.md`
2. `docs/governance/ENGINEERING-AND-SECURE-CODING-STANDARDS.md`
3. `docs/governance/PR-REVIEW-CHECKLIST.md`
4. `AGENTS.md` / service SSOT testing rules for the module touched

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
