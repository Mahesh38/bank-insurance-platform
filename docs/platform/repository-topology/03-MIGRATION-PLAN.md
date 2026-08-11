# 03 — Migration Plan

**Status:** Proposal under `CR-002`. Not approved. No repository may be created before that
decision.
**Parent:** [`README.md`](./README.md)

---

## 1. The recommendation in one paragraph

**Split — but not yet, and not all at once.** The target topology in
[01](./01-TARGET-TOPOLOGY.md) is the right end state for a platform heading toward ~16 services
([ARCH-003](../architecture-review/08-architecture-decision-log.md)) and more than one delivery
team. It is the wrong thing to start **this week**, because WS-1 is at L7 Hardening with
`GATE-P4` open, and three of that gate's unmet criteria run straight through the build the split
would rewrite: **4.1** (sandbox E2E suite in CI), **4.2** (OpenAPI publication), and **4.7**
(coverage gates green / QA-001 closed, added by the already-approved `CR-001`). The plan below
therefore front-loads everything that is *reversible and gate-neutral* — the parent repository,
the build conventions, the published libraries — and moves no WS-1 service until `GATE-P4` has
passed.

If the approvers want the split sooner, the honest trade is stated in §7: Wave 2 can start before
the gate, at the cost of re-proving criteria 4.1 and 4.7 on a build that changed underneath them.

---

## 2. What must be built before anything moves

These are not migration steps; they are the things the monorepo currently gives away for free and
that nine repositories must be given explicitly. Each is a prerequisite of the wave named.

| # | Capability | What exists today | What is needed | Needed by |
|---|-----------|-------------------|----------------|-----------|
| **P1** | **Binary artifact hosting** | `include("libs:…")` — source dependencies, no registry | GitHub Packages or AWS CodeArtifact, credentials in every repo's CI, a version policy (semver + BOM) | Wave 0 |
| **P2** | **Build conventions as a plugin** | 130 lines of `subprojects { }` in the root `build.gradle.kts`: Java 21, Spring BOM 3.3.4, Lombok, JaCoCo, `check` wiring | `bank-insurance-build-conventions` publishing `bank.java-conventions`, `bank.spring-service`, `bank.coverage` | Wave 0 |
| **P3** | **Coverage floors, preserved exactly** | Per-module floors computed in one file: libs 80/70, `1sb-integration-service` **90/70**, other services 50 line | The same floors encoded in the `bank.coverage` plugin, keyed by repo — **not** re-argued per repo, or `CR-001`'s criterion 4.7 silently weakens | Wave 0 |
| **P4** | **Reusable governance workflow** | `.github/workflows/governance.yml` — `FreshnessCheck` (JDK baseline) + `ci-checks.py` (Python) | Same two tiers as a `workflow_call` workflow in the parent; consumers call it in 3 lines | Wave 0 |
| **P5** | **Governance routing paths fixed** | `routing` in `CURRENT-STATE.yaml` points `ARCH`/`MIGRATION` at `docs/architecture-review/…`, which **does not exist** (the file is under `docs/platform/`) | Correct paths, then repo-qualified paths ([02 §3](./02-GOVERNANCE-FEDERATION.md#3-why-the-state-file-and-registers-stay-central)) | Wave 0 |
| **P6** | **Contract tests across the HTTP seam** | Integration ↔ persistence is proven by `SYNC-CONTRACT.md` and in-repo tests; full E2E is **parked as TD-014**, whose trigger has already fired | Consumer-driven contract tests (Pact or Spring Cloud Contract) running in both repos. Once the services are in different repos, no single CI run proves the seam — **TD-014 stops being debt and becomes a blocker** | Wave 2 |
| **P7** | **Local + cloud dev stack** | `docker-compose*.yml`, combined `Dockerfile`, `render.yaml`, `config/keycloak`, `config/onesb` | `bank-insurance-devstack`, referencing published images by tag | Wave 1 |

> **P6 is the one that bites.** Today a single `./gradlew test` proves integration and persistence
> agree. After the split nothing does, unless contract tests exist first. Moving WS-1 services
> without P6 would trade a green gate for an unverifiable seam.

---

## 3. The waves

### Wave 0 — Parent and substrate. Nothing moves.

**Entry:** `CR-002` approved.
**Blast radius:** the monorepo keeps building and deploying throughout. Fully reversible.

1. Create `bank-insurance-governance`. Move `docs/**`, `scripts/governance/**`, `AGENTS.md`
   template, and `.claude/skills/aigem-triage/` into it, preserving history
   (`git filter-repo --path docs --path scripts/governance`).
2. Add the `repos:` block and fix `routing` (**P5**); update `current-state.schema.json`
   accordingly. Tag `v1.0.0`.
3. The monorepo adds `.governance/` as a submodule pinned to `v1.0.0` and **deletes its own
   `docs/`**. It becomes the first consumer of the parent — which is how the mechanism gets
   proven before any service depends on it.
4. Create `bank-insurance-build-conventions` (**P2**, **P3**) and publish `v0.1.0`.
5. Create `bank-insurance-common-libs` from `libs/**` (history preserved); publish the five
   artifacts + BOM (**P1**).
6. The monorepo replaces `include("libs:…")` with the published BOM and coordinates.

**Exit evidence:** monorepo CI green with zero source-included libraries; `./gradlew test
jacocoTestCoverageVerification` produces the *same* coverage floors as before the change;
`FreshnessCheck` green when run from `.governance/`.

**Rollback:** revert the two commits in the monorepo (`settings.gradle.kts` and the submodule
add). The new repositories are additive and harm nothing if abandoned.

---

### Wave 1 — Pilot: the three WS-2 identity services.

**Entry:** Wave 0 exit evidence accepted, **P7** ready.
**Why these first, not the WS-1 pair:** WS-2 is at **L4/L6 — Foundation into first vertical
slice**, where [RUNBOOK §8.3](../../governance/RUNBOOK.md#8-what-the-ai-agent-must-know-about-this-project)'s
posture is *"build the floor"* and CI churn is cheap. WS-1 is at **L7 — Hardening**, where the
posture is *"prove it, don't extend it"* and CI churn is precisely what the open gate cannot
absorb. Piloting where a mistake costs a rebuild rather than a gate is the whole reason to have a
pilot.

1. Extract each service with `git filter-repo --path services/<svc> --path-rename services/<svc>/:`.
2. Apply the convention plugin; depend on the published libs; wire the reusable governance
   workflow; install the `AGENTS.md` stanza, the bootstrap script, and the triage skill.
3. Move service-scoped docs into the new repo; leave cross-cutting docs in the parent.
4. **Freeze, don't delete:** the monorepo path stays but is locked — CODEOWNERS blocks it and a CI
   check fails any PR touching it. Deletion happens only after one green release from the new
   repo.
5. Stand up `bank-insurance-devstack` and prove
   `docker compose --env-file .env.identity -f docker-compose.identity.yml up --build` still
   works against the published images.

**Exit evidence:** three repos green independently; identity stack boots from devstack; a
deliberate triage run inside a service repo produces a register PR in the parent via `record.sh`
([02 §7](./02-GOVERNANCE-FEDERATION.md#7-the-cross-repo-write-path)); pin-staleness check
demonstrated failing on a stale pin.

**Rollback:** un-freeze the monorepo paths, archive the three repos. Cost is the sprint, not the
product.

---

### Wave 2 — WS-1: persistence, then integration.

**Entry:** `GATE-P4` **PASSED** (all seven criteria, per [04-STAGE_GATES](../../governance/04-STAGE_GATES.md))
**and P6 contract tests green in the monorepo first**.

Order matters: `bank-persistence-service` moves first because integration depends on it over
**HTTP**, not at compile time (`BANK_PERSISTENCE_BASE_URL`) — so the dependency survives the move
untouched. Reverse the order and integration temporarily depends on a service that has not yet
learned to publish its contract.

1. Extract persistence; contract tests run in both repos against the published contract.
2. Extract integration; verify against the standing constraints — *no Flyway or JPA in
   `1sb-integration-service`*, *1SB types only in `adapter.onesb.*`* — which are ArchUnit-enforced
   **inside** the module and therefore travel with it intact.
3. Re-run the Phase 4 evidence on the new builds: sandbox E2E (4.1), published OpenAPI (4.2),
   coverage gates (4.7) at the preserved 90/70 floor.
4. Move the combined `Dockerfile` + `render.yaml` to devstack, or retire them in favour of
   per-service images.

**Exit evidence:** every `GATE-P4` criterion re-evidenced on split builds; contract tests green in
both repos; no standing constraint regressed.

**Rollback:** highest cost of any wave. Both repos stay frozen-but-live for one full release cycle
so a revert means re-pointing CI, not recovering code.

---

### Wave 3 — Retire the monorepo.

Archive `bank-insurance-platform` read-only, with a `README.md` redirecting to the nine
repositories. History is preserved in both places — the archive keeps the whole thing, and each
extraction kept its own path's history.

---

### Wave 4 — New services are born split.

The remaining ~14 services from [ARCH-003](../architecture-review/08-architecture-decision-log.md)
are created directly from a template repository carrying `.governance/`, the conventions plugin,
the workflow, and the agent stanza. **They are never migrated, because they are never in the
monorepo.** From Wave 0 onward this is the cheapest path even if Waves 2–3 are deferred
indefinitely.

---

## 4. Sequencing at a glance

```text
CR-002 approved
   │
   ├─ Wave 0  parent + conventions + published libs        monorepo intact, reversible
   │            └── proves the mechanism on one consumer
   │
   ├─ Wave 1  WS-2 identity ×3  (L4 — cheap to churn)      pilot
   │
   ├─ ⛔ GATE-P4 must PASS ────────────────────────────────  hard barrier
   │
   ├─ Wave 2  persistence → integration  (L7 — hardening)  needs P6 contract tests
   │
   ├─ Wave 3  archive the monorepo
   │
   └─ Wave 4  the other ~14 services, born split           no migration, ever
```

**Effort — engineering judgment, not a committed date**, in the same spirit as
[07-delivery-roadmap-and-estimate.md](../architecture-review/07-delivery-roadmap-and-estimate.md):
Wave 0 ≈ 2–3 weeks (the artifact pipeline dominates), Wave 1 ≈ 2 weeks, Wave 2 ≈ 3 weeks plus the
contract-test build, Wave 3 ≈ days. Pressure-test against real team size before quoting it.

---

## 5. What the platform loses, stated plainly

| Property | Today | After |
|----------|-------|-------|
| Atomic cross-service change | One PR, one CI run | N PRs, expand/contract protocol, version skew |
| Shared-lib change | Compile-time feedback in every consumer immediately | Publish → bump → nine PRs; feedback delayed by a release |
| One coverage view | `./gradlew test jacocoTestCoverageVerification` across everything | Per-repo reports; the rollup must be built (**P3**) |
| Seam proof | One test run proves integration ↔ persistence | Nothing proves it without contract tests (**P6**) |
| Onboarding | `git clone` | Clone + submodule + registry credentials + devstack |
| Governance | One state file, one register set | Same — **only because** the parent is central ([02 §3](./02-GOVERNANCE-FEDERATION.md#3-why-the-state-file-and-registers-stay-central)) |

What it gains: independent release cadence, per-service ownership and CODEOWNERS, blast-radius
isolation in CI, and a topology that scales to ~16 services and multiple teams without a 19-module
`settings.gradle.kts`.

---

## 6. Risks

Registered against the parent's `RISK-REGISTER.md` (next free ID is `RISK-012`) **only if
`CR-002` is approved** — an agent does not mint risks for a change that may not happen.

| # | Risk | Severity | Mitigation |
|---|------|----------|------------|
| R1 | Splitting during hardening endangers `GATE-P4` criteria 4.1 / 4.2 / 4.7 | **High** | Hard barrier before Wave 2; WS-2 pilots first |
| R2 | Governance drifts across nine repos — the exact failure AIGEM exists to prevent | **High** | Central parent, read-only vendoring, pinned tags, pin-staleness CI, weekly org audit ([02 §6](./02-GOVERNANCE-FEDERATION.md#6-keeping-the-pins-honest)) |
| R3 | Version skew across shared libs | Medium | BOM + Renovate + "no more than 2 minors behind" CI check |
| R4 | The integration ↔ persistence seam loses its proof | **High** | P6 is a Wave 2 entry condition, not a follow-up |
| R5 | Register writes stop happening because they need a second PR | Medium | `record.sh` automates the cross-repo PR ([02 §7](./02-GOVERNANCE-FEDERATION.md#7-the-cross-repo-write-path)) |
| R6 | An agent in a service repo loses the platform picture | Medium | `.governance/` carries the full `docs/` tree, and step 3 of the stanza resolves the repo → workstream before any work |
| R7 | Coverage floors quietly weaken during the move (90/70 → 50) | Medium | P3 encodes the floors centrally; Wave 0 exit evidence compares before/after |
| R8 | Nine repos, one team — process overhead exceeds the benefit | Medium | Waves 2–3 are optional and reversible; Wave 4 delivers most of the value alone |

---

## 7. If the approvers want it sooner

The plan's one hard barrier is `GATE-P4`. Removing it is a legitimate choice, not a violation, but
it is a `WAIVER`-class decision under
[14-CHANGE_CONTROL §1](../../governance/14-CHANGE_CONTROL.md) and needs to be recorded as one. The
trade:

- Criteria **4.1** and **4.7** must be re-evidenced on a build that changed underneath them —
  roughly two weeks of re-proof, and any regression is discovered against a moving target.
- **P6** becomes urgent rather than sequenced: the seam between integration and persistence would
  be unproven for as long as contract tests lag the split.
- Phase 4's exit date moves. That is a gate-date change, which needs the same approvers anyway.

**Recommendation: don't.** Waves 0, 1 and 4 deliver the parent repository, the federation
mechanism, and the "new services born split" property — the substance of the request — without
touching WS-1 at all.
