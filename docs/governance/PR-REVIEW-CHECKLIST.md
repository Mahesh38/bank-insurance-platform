# Pull request review checklist

**Cites:** [ENGINEERING-AND-SECURE-CODING-STANDARDS.md](./ENGINEERING-AND-SECURE-CODING-STANDARDS.md)
· [CODE-REVIEW-STANDARD.md](./CODE-REVIEW-STANDARD.md)
· [COVERAGE.md](../1sb-insurance-integration/service-ssot/COVERAGE.md)  
**Also:** [ORG-STANDARDS.md](./ORG-STANDARDS.md) (organisational baseline)  
**Purpose:** Adoption mechanism for GATE-S08 **S08-G8** — every PR is reviewed against the published
standards. How to review, push-time coverage, and logging/monitoring: CODE-REVIEW-STANDARD.md.

Reviewers tick every applicable row. Authors pre-complete the Author column. “N/A” must be written,
not implied.

## Author

- [ ] Change purpose, work-item / SUG id, and risk are clear in the PR body
- [ ] Touched-module `test jacocoTestCoverageVerification` was run; **measured** line % and branch % are in the PR (CODE-REVIEW-STANDARD §1)
- [ ] Tests cover the behaviour change, including new error/security branches (ENG-5, R2, OBS-10)
- [ ] No secrets or raw PII in code, fixtures, exception messages, or intended log output (SEC-C1, SEC-C2, OBS-2)
- [ ] Failures use `ServiceErrors` / catalogue codes; new HTTP clients have timeouts (OBS-1, OBS-9)
- [ ] Public API / OpenAPI updated if the HTTP contract changed (ENG-4)
- [ ] ADR opened if this adds middleware, a trust boundary, or crypto (ENG-2 / SEC-C10)

## Reviewer — engineering (ENG)

- [ ] Module and package boundaries respected; no vendor leak past adapters (ENG-2, ENG-3)
- [ ] Coverage floors will remain green for **this branch's** touched modules (ENG-5, ENG-9, §1)
- [ ] No unexplained floating dependency or scaffold bypass (ENG-8, ENG-9)
- [ ] TODOs carry a work-item id (ENG-10)

## Reviewer — secure coding (SEC)

- [ ] Input validated at the boundary; failure modes fail closed (SEC-C3)
- [ ] AuthZ remains default-deny for new endpoints (SEC-C4)
- [ ] No SQL / command construction via string concat (SEC-C5)
- [ ] Logging will not emit PAN/Aadhaar/phone/email/health/token/handle (SEC-C2, OBS-2)
- [ ] Security-relevant outcomes use catalogue codes (SEC-C8, OBS-3)
- [ ] Security-sensitive design has Deepali (or delegate) review when SEC-C10 applies

## Reviewer — logging and monitoring (OBS)

Mandatory on every behaviour change. Dashboards/SLOs are Phase 6 and are **not** a merge blocker here.

- [ ] HTTP failures recorded via `ErrorRecorder` (structured log + `bank.error.count`) (OBS-1, OBS-8)
- [ ] Upstream / token failures do not leak URI, `q`, token, or body (OBS-4)
- [ ] Client-caused WARN without stack; platform/upstream ERROR with stack (OBS-6)
- [ ] `correlationId` path intact; exceptions not swallowed before the handler (OBS-5)
- [ ] Tests assert error **codes** and no secrets/`q` in messages (OBS-10)

## Merge bar

Do not approve if any unchecked row is applicable and unresolved. Waivers need owner + expiry +
compensating control + debt/risk id (standards §5).

Verdict: `APPROVE` · `CHANGES_REQUESTED` · `COMMENT`. This checklist is not a T4 signature.
