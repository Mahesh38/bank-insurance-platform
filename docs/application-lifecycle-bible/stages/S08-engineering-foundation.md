# S08 — Engineering Foundation

**AIGEM stage:** L4 — Foundation · **Owner:** Amit (Engineering) + Swapnali (Quality)
**Central question:** *Can we build, test and prove code safely?*

> **This stage is missing in this repository and is the primary subject of the realignment.**
> Everything downstream that requires evidence — every gate criterion at E3 or E4 — depends on
> machinery this stage installs. Until it exists, later stages can be worked on but cannot be
> *passed*.

---

## 1. Purpose

Build the machinery that turns writing code into **provable** delivery: a pipeline that builds and
tests everything on every change, quality gates that fail the build rather than file a ticket,
security scanning in the path of every merge, and test infrastructure that makes behaviour
verifiable at every level of the pyramid.

The distinguishing property of this stage is that its output is **not a feature**. That is
precisely why it gets skipped, and precisely why skipping it is unrecoverable without a
deliberate recovery increment.

> **The test of S08:** a new engineer opens a pull request that breaks a compliance control, and
> the pipeline stops them — without a human noticing.

## 2. Entry criteria

- [ ] GATE-S07 passed: architecture, constraints and NFR targets defined
- [ ] Repository and branching model agreed
- [ ] Engineering capacity allocated to foundation work as scheduled backlog, not overhead

## 3. Epics and stories

### S08-E01 — Continuous integration pipeline · *Amit + Shivanshi*

The single highest-value epic in the realignment.

| ID | Story | Acceptance criteria |
|---|---|---|
| S08-E01-S01 | Build every module on every pull request | Full Gradle build across all `libs/` and `services/`; failure blocks merge; build under 10 minutes |
| S08-E01-S02 | Run unit and component tests on every PR | All tests execute; any failure blocks merge; results published as a readable report |
| S08-E01-S03 | Run integration tests on merge to main | Testcontainers-backed; failure blocks the merge queue |
| S08-E01-S04 | Publish build artefacts | Versioned JARs and container images to a registry, tagged with the commit SHA |
| S08-E01-S05 | Enforce branch protection | No merge to `main` without a green pipeline and an approving review |
| S08-E01-S06 | Make pipeline status visible | Build badge, failure notification to the owning team, and a dashboard of pipeline health |
| S08-E01-S07 | Optimise pipeline feedback time | Parallelised module builds, dependency caching; PR feedback under 10 minutes or the pipeline gets bypassed in practice |

### S08-E02 — Quality gates in the pipeline · *Swapnali + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S08-E02-S01 | Enforce JaCoCo coverage thresholds | Build **fails** below threshold — libs 80/70, services per policy. Closes QA-001's mechanism |
| S08-E02-S02 | Enforce ArchUnit rules in CI | Boundary violations fail the build; `allowEmptyShould(true)` removed (closes TD-007) |
| S08-E02-S03 | Enforce static analysis | Linting and code-quality rules; new violations fail, existing ones are a tracked baseline |
| S08-E02-S04 | Enforce build reproducibility | Same commit produces the same artefact; no snapshot or floating dependencies |
| S08-E02-S05 | Publish coverage and quality trend | Per-module trend visible over time, so decline is noticed before it is entrenched |
| S08-E02-S06 | Define and enforce 100% coverage on compliance-gate code | Control paths C1–C10 at 100% branch coverage, no waiver |

### S08-E03 — Test infrastructure · *Swapnali + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S08-E03-S01 | Testcontainers for PostgreSQL | Integration tests run against a real database, created and destroyed per run |
| S08-E03-S02 | WireMock harness for 1SB and external providers | Provider behaviour stubbed including error, timeout and malformed responses (closes TD-014) |
| S08-E03-S03 | Shared test fixture library | Realistic domain fixtures reused across modules; no copy-pasted test data |
| S08-E03-S04 | Contract testing across service seams | Provider and consumer contracts verified in CI at every seam |
| S08-E03-S05 | E2E test harness | Framework, environment hookup, data setup and teardown, reporting |
| S08-E03-S06 | PII-free synthetic test data generator | Generates realistic profiles including the awkward cases: joint life, minor nominee, NRI, PAN mismatch |
| S08-E03-S07 | Performance test harness | Load-generation tooling wired to run against a deployed environment and publish results |

### S08-E04 — Security in the pipeline · *Deepali + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S08-E04-S01 | Secret scanning | Pre-commit hook and CI stage; any detected credential fails the build; historical scan performed once |
| S08-E04-S02 | Static application security testing (SAST) | Runs on every PR; new critical or high findings fail; findings routed to the risk register |
| S08-E04-S03 | Dependency and supply-chain scanning (SCA) | Vulnerability check on every build; critical with a reachable path fails |
| S08-E04-S04 | Container image scanning | Every image scanned before publication; critical CVEs block |
| S08-E04-S05 | Generate an SBOM per build | Stored with the artefact; licence compliance checked |
| S08-E04-S06 | Define the vulnerability remediation SLA | S0 immediate, S1 next release, S2 two releases, S3 backlog — tracked, not aspirational |

### S08-E05 — Engineering standards · *Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S08-E05-S01 | Publish coding standards | Language level, formatting (automated), naming, package structure, error handling |
| S08-E05-S02 | Publish the secure coding standard | Input validation, output encoding, authorization checks, secret handling, logging prohibitions |
| S08-E05-S03 | Define the branching and review model | Trunk-based or short-lived branches; review requirements by change tier |
| S08-E05-S04 | Define the observability standard for code | Required metrics, structured logging with correlation IDs, trace propagation — as a library, not a convention |
| S08-E05-S05 | Define the PII-masking standard and prove it | Masking converter applied at the framework level, with a test that scans emitted logs for regulated field patterns |
| S08-E05-S06 | Publish the Definition of Done | Agreed with QA; enforced at review |

### S08-E06 — Developer experience · *Shivanshi + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S08-E06-S01 | One-command local environment | A new engineer runs the full stack locally on day one |
| S08-E06-S02 | Fast local feedback | Unit tests under 3 minutes; a documented inner-loop workflow |
| S08-E06-S03 | Service scaffolding template | A new service starts with observability, security, error handling, health checks and CI already wired |
| S08-E06-S04 | Onboarding documentation | A new engineer makes a merged, tested change within their first week |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S08-VT-01 | CI actually blocks bad code | Open a PR with a deliberately failing test | Merge is blocked |
| S08-VT-02 | Coverage gate bites | Open a PR that drops coverage below threshold | Build fails |
| S08-VT-03 | Architecture rules bite | Open a PR importing a 1SB type outside `adapter.onesb.*` | Build fails |
| S08-VT-04 | Secret scanning bites | Open a PR containing a test credential in a realistic format | Build fails, secret not merged |
| S08-VT-05 | SCA bites | Introduce a dependency with a known critical CVE | Build fails |
| S08-VT-06 | PII never reaches logs | Run the full test suite; scan all emitted logs for PAN, Aadhaar, phone, email and health patterns | Zero matches |
| S08-VT-07 | Compliance-gate coverage is total | Coverage report filtered to control paths C1–C10 | 100% branch |
| S08-VT-08 | Pipeline is fast enough to be trusted | Measure PR feedback time across 20 runs | p95 under 10 minutes |
| S08-VT-09 | Pipeline is reliable | Measure flaky failures across 50 runs | Under 1% |
| S08-VT-10 | A new engineer can contribute | Onboard someone unfamiliar; time to first merged change | Under one week |
| S08-VT-11 | Builds are reproducible | Build the same commit twice on different agents | Identical artefacts |

**S08-VT-01 through VT-05 are the ones that matter.** A pipeline that runs but never blocks
anything is theatre. Each of these tests deliberately breaks a rule and confirms the machine
notices — this is the difference between having gates and having gate-shaped configuration.

## 5. Exit gate — GATE-S08

| # | Criterion | Level | Evidence artefact |
|---|---|---|---|
| S08-G1 | CI builds and tests every module on every PR | E4 | Pipeline definition + run history |
| S08-G2 | Merge to main is impossible without a green pipeline | E4 | Branch protection config + a blocked-merge demonstration |
| S08-G3 | Coverage thresholds enforced; QA-001 closed | E4 | Coverage report from CI + QA-001 closure record |
| S08-G4 | ArchUnit and static analysis enforced | E4 | CI run showing enforcement + a demonstrated failure |
| S08-G5 | Secret, SAST, SCA and image scanning in the pipeline | E4 | Scan reports from a CI run |
| S08-G6 | Test infrastructure operational at every pyramid level | E4 | Tests of each type running in CI |
| S08-G7 | No PII in logs, proven by automated test | E4 | Log-scan test result |
| S08-G8 | Engineering and secure coding standards published and adopted | E2 | Standards documents + review checklist referencing them |
| S08-G9 | Pipeline feedback under 10 minutes at p95, flake under 1% | E4 | Pipeline metrics over ≥ 50 runs |
| S08-G10 | A new engineer can build, test and ship in under a week | E3 | Onboarding record |

**Approvers:** Amit (AP) · Swapnali (AP, B) · Mahesh (AP) · Deepali (AP, B) · Shivanshi (AP) ·
Aarti (RV) · Rajal (RV) · Kalpana (RV)

## 6. Current position in this repository — 🔴 Missing

This is the foundation the brief describes as absent, and the evidence is unambiguous.

| Capability | State | Evidence |
|---|---|---|
| Application CI | **Absent** | `.github/workflows/` contains only `governance.yml`, which validates markdown and schemas |
| Coverage enforcement | Configured, **never executed** | JaCoCo in `build.gradle.kts`; nothing runs it on a PR. QA-001 open at P0 |
| ArchUnit enforcement | Written, **never executed in CI** | Rules exist in the 1SB service; no pipeline runs them |
| Secret scanning | **Absent** | — |
| SAST | **Absent** | — |
| SCA / dependency scanning | **Absent** | — |
| Image scanning | **Absent** | — |
| SBOM | **Absent** | — |
| Testcontainers | **Absent** | — |
| WireMock harness | **Absent** | TD-014 open |
| Contract tests | **Absent** | — |
| E2E harness | **Absent** | WS-1 gate criterion 4.1 blocked on it |
| Performance harness | **Absent** | WS-1 gate criterion 4.6 blocked on it |
| PII-in-logs test | **Absent** | Masking converter exists; nothing proves it works |
| Test data generator | **Absent** | — |
| Branch protection | Unverified | — |

**Test depth today:** 1SB service 42 test files against 147 main (29%); persistence 7:45 (16%);
authorization 4:20; BFF 4:19; IdP adapter 2:8.

**The consequence, stated plainly.** Twenty thousand lines of Java in a regulated financial
application have never been built or tested by an automated system. Every claim of "green" in
every phase status document is a human assertion. WS-1 Phase 4 criteria 4.1, 4.6 and 4.7 are not
merely open — they are **unachievable** until this stage is delivered, and marking them `OPEN`
rather than `BLOCKED` has hidden that for the duration of the phase.

**Recovery sequencing** is in [`03-REALIGNMENT-PLAN.md §2`](../03-REALIGNMENT-PLAN.md): weeks 1–3
deliver E01 and E02 (the pipeline and its gates), weeks 3–6 deliver E03 and E04 (test
infrastructure and security scanning). E01-S01 alone — a build that runs — converts every future
gate claim from assertion to artefact and is the highest-leverage single change available to this
programme.

## 7. Premature at this stage

Feature breadth · new bounded contexts · production infrastructure tuning · autoscaling ·
multi-region.

The temptation during a foundation recovery is to also build the sixteen missing services because
the gap list is long. That would recreate the original error at four times the scale.
