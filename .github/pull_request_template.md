## Summary

<!-- What and why. Link story / GATE / SUG / EPIC id. -->

## Standards checklist

Authors and reviewers complete
[`docs/governance/PR-REVIEW-CHECKLIST.md`](../docs/governance/PR-REVIEW-CHECKLIST.md)
against
[`CODE-REVIEW-STANDARD.md`](../docs/governance/CODE-REVIEW-STANDARD.md)
(coverage on push, review aspects, logging/monitoring) and
[`ENGINEERING-AND-SECURE-CODING-STANDARDS.md`](../docs/governance/ENGINEERING-AND-SECURE-CODING-STANDARDS.md)
(S08-G8 ENG / SEC / OBS ids).

- [ ] ENG rows applicable to this change are satisfied (or waived with id + expiry)
- [ ] SEC rows applicable to this change are satisfied (or waived with id + expiry)
- [ ] OBS rows applicable to this change are satisfied (or waived with id + expiry)

## Coverage on this branch (required)

Floors: [`COVERAGE.md`](../docs/1sb-insurance-integration/service-ssot/COVERAGE.md)
(`libs` 80% line / 70% branch; Phase-1 services 90/70; other services 50% line).
Run `jacocoTestCoverageVerification` on **touched** modules — `./gradlew test` alone does not fail floors.

| Module | Tests | Line % | Branch % | Floor (L/B) | Command |
|--------|------:|-------:|---------:|-------------|----------|
| | | | | | `./gradlew :<module>:test jacocoTestCoverageVerification` |

- [ ] New/changed behaviour has same-PR tests (R2), including error and security branches (OBS-10)

## Logging / monitoring (required on behaviour changes)

- [ ] HTTP failures go through `ServiceErrors` → `ErrorRecorder` (OBS-1)
- [ ] No PII, token, secret, URI-with-`q`, or upstream body in logs/exception messages (OBS-2, OBS-4)
- [ ] Security-relevant outcomes use catalogue codes (OBS-3)
- [ ] New HTTP clients have timeouts and catalogue translation (OBS-9)
- [ ] Dashboards/SLOs not claimed unless this PR *is* a Phase 6 observability deliverable (OBS-8)

## Test plan

- [ ] `./gradlew` tests + coverage verification for touched modules
- [ ] Notes / evidence for any manual check

## Risk / rollback

<!-- Blast radius and how to undo -->
