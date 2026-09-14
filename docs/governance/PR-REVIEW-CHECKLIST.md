# Pull request review checklist

**Cites:** [ENGINEERING-AND-SECURE-CODING-STANDARDS.md](./ENGINEERING-AND-SECURE-CODING-STANDARDS.md)  
**Also:** [ORG-STANDARDS.md](./ORG-STANDARDS.md) (organisational baseline)  
**Purpose:** Adoption mechanism for GATE-S08 **S08-G8** — every PR is reviewed against the published standards.

Reviewers tick every applicable row. Authors pre-complete the Author column.

## Author

- [ ] Change purpose and risk are clear in the PR body
- [ ] Tests cover the behaviour change (ENG-5)
- [ ] No secrets or raw PII in code, fixtures, or intended log output (SEC-C1, SEC-C2)
- [ ] Public API / OpenAPI updated if the HTTP contract changed (ENG-4)
- [ ] ADR opened if this adds middleware, a trust boundary, or crypto (ENG-2 / SEC-C10)

## Reviewer — engineering (ENG)

- [ ] Module and package boundaries respected; no vendor leak past adapters (ENG-2, ENG-3)
- [ ] Coverage and quality gates will remain green (ENG-5, ENG-6, ENG-7)
- [ ] No unexplained floating dependency or scaffold bypass (ENG-8, ENG-9)

## Reviewer — secure coding (SEC)

- [ ] Input validated at the boundary; failure modes fail closed (SEC-C3)
- [ ] AuthZ remains default-deny for new endpoints (SEC-C4)
- [ ] No SQL / command construction via string concat (SEC-C5)
- [ ] Logging will not emit PAN/Aadhaar/phone/email/health (SEC-C2)
- [ ] Security-sensitive design has Deepali (or delegate) review when SEC-C10 applies

## Merge bar

Do not approve if any unchecked row is applicable and unresolved. Waivers need owner + expiry +
compensating control + debt/risk id (standards §4).
