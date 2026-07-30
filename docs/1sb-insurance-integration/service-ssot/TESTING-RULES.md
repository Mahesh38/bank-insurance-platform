# Testing Rules (Enforceable)

**Authority:** QA Lead · [QA-LEAD-TESTING-STRATEGY.md](./QA-LEAD-TESTING-STRATEGY.md)  
**Audience:** All developers, AI coding agents, reviewers  
**See also:** Role DoD — [ROLE-GUIDELINES-AND-DOD.md](./ROLE-GUIDELINES-AND-DOD.md)  
**Branch policy:** Applies from next commit on `cursor/phase1-foundations-c259` onward

---

## R1 — Test pyramid discipline

1. Prefer **unit** over Spring Boot context for pure logic.
2. Prefer **WireMock / MockRest** over live network.
3. Prefer **one focused test class per production class** for non-trivial types (`Foo` → `FooTest`).
4. `@SpringBootTest` is for wiring/IT — not a substitute for mapper/unit coverage.

## R2 — Same-PR rule (Developer)

A PR that adds or changes behaviour in these packages **must** add/update tests in the same PR:

- `application.*`, `lob.*`, `api.*`
- `adapter.onesb.client|error|polling|*`
- `adapter.idempotency.*`, `adapter.persistence.*`
- `com.bank.persistence.api.*`
- `libs/bank-common-*` public API

**Exception:** Package-info, pure DTO records with no logic, generated code — note in PR description.

## R3 — Naming & structure

```text
src/test/java/.../FooTest.java          # unit / slice
src/test/java/.../FooIT.java            # integration (@Tag("integration"))
src/test/resources/fixtures/            # JSON payloads (masked)
```

- Method names: `behaviour_expectedOutcome` or AssertJ fluent style with `@DisplayName`
- Use `@Tag("unit")`, `@Tag("integration")`, `@Tag("FUNC-002")` where useful for filtering

## R4 — Isolation

| Forbidden in unit tests | Allowed in IT only |
|-------------------------|--------------------|
| Real 1SB sandbox | Gated E2E profile |
| Real AWS / Vault | Contract stub |
| Shared mutable static state without reset | — |
| Sleeping &gt; 100ms without fake clock/sleeper | Poller IT with injected `Sleeper` |

## R5 — Assertions quality

**Reject in review:**
- `assertNotNull(object)` as sole assertion
- Tests that never call the method under test
- Catching exceptions without asserting type/code
- Snapshot of full logs containing possible PII

**Require:**
- Assert bank error **codes** (`ErrorCodes.*`) not only HTTP status
- Assert **no upstream call** when validation fails (WireMock `verify(0, ...)`)
- Assert PII absence for masking/audit paths

## R6 — Idempotency & async

- Every new mutating `/v1/**` endpoint: MockMvc cases for missing key, replay, conflict.
- Poller/async: use injectable clock/sleeper; assert job terminal state.

## R7 — Coverage gate (after QA-001)

- `./gradlew test jacocoTestReport jacocoTestCoverageVerification` must pass in CI.
- Waivers: add TECH-DEBT id + expiry; TL+QA Lead co-approve.

## R8 — AI / agent contributions

Agents **may** open PRs that only add tests. Humans (Dev or QA) must approve.  
Agents must run `./gradlew test` and paste summary in PR body.

## R9 — Definition of Done (story)

Story is not Done until:

- [ ] Unit tests for new logic
- [ ] Slice/WireMock for new HTTP boundaries
- [ ] AC mapped to test method names / tags
- [ ] No new ArchUnit violations
- [ ] Coverage gate green (once enabled)
- [ ] QA Lead or delegate spot-check on P0 stories

## R10 — Flaky tests

- Flake twice in CI → quarantine `@Disabled("flake: TD-xxx")` within 24h + ticket
- No silent retry loops hiding race conditions
