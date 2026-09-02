# S12 — Hardening & Certification

**AIGEM stage:** L7 — Hardening · **Owner:** Swapnali (QA) + Deepali (Security) + Shailja (Compliance)
**Central question:** *Is it correct, safe, compliant and provable?*

---

## 1. Purpose

Prove the vertical slice is fit to carry real customers and real money. Hardening does not add
behaviour; it produces the **evidence** that behaviour is correct, secure, compliant and
operable — and fixes what the evidence exposes.

> **The defining constraint:** hardening is impossible without the machinery of S08 and S09. You
> cannot produce E4 evidence without a pipeline, or E3 evidence without an environment. A
> programme "hardening" without those is not hardening; it is asserting.
>
> That is precisely the state WS-1 Phase 4 is in today, which is why five of its seven criteria
> have stayed open.

## 2. Entry criteria

- [ ] GATE-S11 passed: the journey works end to end in UAT
- [ ] GATE-S08 and GATE-S09 passed: evidence machinery exists
- [ ] NFR targets carry numbers (GAP-017 closed) — you cannot test against an undefined threshold

## 3. Epics and stories

### S12-E01 — Functional certification · *Swapnali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S12-E01-S01 | Execute the full functional test suite | Every requirement's AC exercised; results published |
| S12-E01-S02 | Execute negative, boundary and exception testing | Not only the happy path; every failure path from S03 covered |
| S12-E01-S03 | Execute the data-variant suite | Joint life, minor nominee, NRI, PAN mismatch, mid-journey abandonment, duplicate submission |
| S12-E01-S04 | Build the regression suite | Full journey regression, automated, runnable on demand |
| S12-E01-S05 | Complete the traceability matrix | Requirement → code → test → evidence, closed with no orphans |
| S12-E01-S06 | Triage and close defects | Zero D0 and D1 open at exit; D2 agreed with PO and QA |

### S12-E02 — Security certification · *Deepali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S12-E02-S01 | Commission an independent penetration test | Scope covers every public surface, the partner boundary and the authorization model |
| S12-E02-S02 | Remediate findings per SLA | S0 and S1 closed; S2 scheduled; each with evidence of the fix |
| S12-E02-S03 | Execute authorization negative testing | Every role against every resource, including horizontal and vertical escalation attempts |
| S12-E02-S04 | Verify secrets and key handling in a running system | No secret in any image, log, config, error or telemetry path |
| S12-E02-S05 | Verify the trust boundaries as built | Segmentation matches the threat model; drift identified |
| S12-E02-S06 | Refresh the threat model against the built system | Design-time assumptions confirmed or corrected |

### S12-E03 — Compliance certification · *Shailja*

| ID | Story | Acceptance criteria |
|---|---|---|
| S12-E03-S01 | Certify every control C1–C10 | Each with its automated test result, reviewed and signed |
| S12-E03-S02 | Review audit schema and log samples | Signed review confirming completeness, attribution and PII absence |
| S12-E03-S03 | Verify consent and suitability evidence end to end | Records retrievable, complete, immutable, correctly versioned |
| S12-E03-S04 | Verify retention and immutability in the running system | Deletion attempts refused; purge operates at the horizon |
| S12-E03-S05 | Verify data residency in the running system | Every store, backup, log and archive attested in-region |
| S12-E03-S06 | Assemble the regulatory evidence pack | The artefact set a regulator would be shown, indexed and complete |

### S12-E04 — Performance and resilience certification · *Swapnali + Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S12-E04-S01 | Execute load testing at projected peak | Against the S07 NFR numbers, including the Q4 tax-season multiplier |
| S12-E04-S02 | Execute soak testing | Sustained load; no leak, no degradation, no connection exhaustion |
| S12-E04-S03 | Execute failure-injection testing | Each dependency failed in turn; behaviour matches design |
| S12-E04-S04 | Verify resilience patterns under load | Breakers, bulkheads and rate limits behave as designed when it matters |
| S12-E04-S05 | Establish the performance baseline | Recorded, so S13 expansion can be measured against it |

### S12-E05 — Operational certification · *Shivanshi*

| ID | Story | Acceptance criteria |
|---|---|---|
| S12-E05-S01 | Write the operational runbook | Secrets rotation, credential failure, provider 401/5xx, payment uncertainty, poll timeout, data-fix procedure |
| S12-E05-S02 | Exercise the runbook | Someone who did not write it executes each procedure successfully |
| S12-E05-S03 | Verify alerting | Every alert fires when it should, routes correctly, and links a runbook |
| S12-E05-S04 | Verify rollback under realistic conditions | With data written, mid-journey, and confirm no journey is corrupted |
| S12-E05-S05 | Complete failure-mode analysis | Every component's failure modes documented with blast radius |

### S12-E06 — Consumer enablement · *Rajal + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S12-E06-S01 | Publish API contracts | OpenAPI to the internal portal, with a consumer collection |
| S12-E06-S02 | Support a real consumer through integration | ≥ 1 bank caller exercises the journey against UAT, with traces |
| S12-E06-S03 | Publish integration documentation | Auth, errors, idempotency, rate limits, support path |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S12-VT-01 | All requirements verified | Traceability matrix query | 100% with a passing test |
| S12-VT-02 | No critical defects | Defect register | Zero D0 and D1 open |
| S12-VT-03 | Security findings remediated | Pentest retest | S0 and S1 closed and retested |
| S12-VT-04 | Every compliance control operates | Control test suite | 100% pass, signed |
| S12-VT-05 | NFRs met | Load test against the NFR sheet | Every target met or a waiver with a named human owner |
| S12-VT-06 | System degrades gracefully | Failure injection across all dependencies | No data loss, no money error, defined user experience |
| S12-VT-07 | Runbook works in someone else's hands | Independent execution | All procedures succeed |
| S12-VT-08 | Rollback is safe with data written | Rollback mid-journey | No corruption; journeys resolvable |
| S12-VT-09 | Evidence pack is complete | Compliance review | A regulator's questions answerable from the pack alone |
| S12-VT-10 | A real consumer can integrate | External caller against UAT | Successful journey with trace evidence |

## 5. Exit gate — GATE-S12

| # | Criterion | Level | Evidence artefact |
|---|---|---|---|
| S12-G1 | Full functional and regression suite green in CI | E4 | CI run |
| S12-G2 | Zero D0/D1 defects; D2 agreed | E1 | Defect register |
| S12-G3 | Penetration test complete; S0/S1 remediated and retested | E3 | Pentest report + retest |
| S12-G4 | All controls C1–C10 certified | E4 | Control test results + Compliance sign-off (**human**) |
| S12-G5 | Audit schema and log samples reviewed | E2 | Signed review |
| S12-G6 | Retention, immutability and residency verified in the running system | E3 | Verification records |
| S12-G7 | NFRs met under load at projected peak | E3 | Load test report vs the NFR sheet |
| S12-G8 | Resilience proven under failure injection | E3 | Results |
| S12-G9 | Runbook exercised by an independent operator | E3 | Execution record |
| S12-G10 | Rollback proven with data written | E3 | Rollback record |
| S12-G11 | Traceability matrix complete | E1 | Matrix |
| S12-G12 | ≥ 1 real consumer integrated against UAT | E3 | Consumer confirmation + traces |
| S12-G13 | Coverage gates green; QA-001 closed | E4 | Coverage report |

**Approvers:** Swapnali (AP, B) · Deepali (AP, B, **human**) · Shailja (AP, B, **human**) ·
Rajal (AP) · Mahesh (AP) · Shivanshi (AP) · Aarti (RV) · Kalpana (RV)

## 6. Current position in this repository — 🟠 In progress, on the wrong subject

WS-1 Phase 4 is the active work and maps to this stage. Its state:

| Criterion | Status | Real blocker |
|---|---|---|
| 4.1 Sandbox E2E suite in CI | ⛔ Blocked | Application-CI foundation exists; sandbox E2E harness and green run evidence remain (`GATE-4.1-SANDBOX-E2E`) |
| 4.2 OpenAPI published + consumer collection | 🟡 Partial | Generated; publication outstanding |
| 4.3 ≥ 1 bank caller exercises UAT | ⛔ Blocked | Consumer publication and named bank UAT slot remain (`DEP-001`, `DEP-002`) |
| 4.4 Compliance review of audit schema | ❌ Open | Achievable now — needs no CI |
| 4.5 Runbook | ❌ Open | Achievable now — owner assigned to Shivanshi by CR-008 |
| 4.6 Performance smoke, p95 | ⛔ Blocked | E2E harness/environment dependency (`DEP-003`); threshold evidence remains |
| 4.7 Coverage gates green; QA-001 | 🟡 Partial | Application workflow now executes coverage; a green CI run and final service threshold remain |

**Two findings.**

*First*, three criteria are now explicitly `BLOCKED` with named dependencies rather than appearing
actionable. Criterion 4.7 remains `PARTIAL`: the execution mechanism exists, but evidence has not
yet closed the quality exit. This makes safe non-blocked work selectable without hiding the gate.

*Second, and more important*: **this stage is being applied to the wrong subject.** S12 hardens a
vertical slice. What is being hardened is an integration adapter whose journey does not exist —
no suitability gate, no consent, no payment execution, no UI. Even if all seven criteria closed,
the result would be a well-tested component of an unbuilt product, and it would still not be
lawfully shippable, because C1 and C2 are unimplemented.

The two achievable items — 4.4 and 4.5 — are worth doing now: both are foundation-shaped, neither
needs CI, and both carry forward into the real S12.

## 7. Premature at this stage

New features · new LOBs · new channels · refactoring for elegance · performance optimisation
beyond meeting the NFR.

The characteristic S12 failure is scope creep disguised as polish. Every "while we're in here"
change invalidates evidence already produced and restarts the certification clock.
