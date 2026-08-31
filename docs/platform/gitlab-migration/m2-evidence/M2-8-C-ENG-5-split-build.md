# `C-ENG-5` — backend build from the split clone · **FAIL**

**Executed:** 2026-08-29 · **Condition:** `C-ENG-5` (Amit) · **Recommendation:** `R8` of [`DEC-20260829-02`](../../../governance/DEC-20260829-02-m3-readiness-board-pack.md)
**Method:** fresh `git clone` of the M2.7 split `backend`, then `./gradlew test`

> ## Result: **503 tests, 4 failures.** The approved split breaks an `ADR-017` enforcement control.
>
> `libs/bank-common-error` › `CatalogueParityTest` — all four tests fail because they read two
> **ratified governance documents** that the approved split moves to a different repository.

---

## 1. Two false passes before the real answer

Worth recording, because both would have shipped a wrong verdict.

| Run | Result | Why it was wrong |
|---|---|---|
| Monorepo `./gradlew test` (M2.8) | **PASS** — 503/0 | `docs/` was present. The monorepo cannot detect this class of coupling **by construction** |
| Split clone, default flags | **PASS** — "53 tasks: 13 executed, 40 from cache" | **40 tasks came `FROM-CACHE`** from the monorepo run. The Gradle build cache is shared, so the tests never re-executed. A cached green is not an execution |
| Split clone, `--no-build-cache --rerun-tasks --continue` | **FAIL** — 503 tests, 4 failures, 52/52 executed | The real answer |

`ArchitectureTest` says it better than I can, in its own Javadoc: *"A rule that passes vacuously is
worse than no rule, because it reports success."* A cached test result is the same failure mode.

**Method note for the real migration:** any build used as split evidence runs with the build cache
disabled. Otherwise the check silently measures the monorepo.

---

## 2. The finding

`libs/bank-common-error/src/test/java/com/bank/common/error/CatalogueParityTest.java` resolves two
files by walking up to six parent directories:

- `docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md`
- `docs/journey-execution/08-SUPPORT-RUNBOOK.md`

Under the approved split those go to **`governance/platform-governance`** (`ADR-018`), while the test
goes to **`product/backend`**. The resolver then fails with its own message:

> *"could not locate `docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md` … this test
> must be able to read the ratified documents it checks against"*

**These four tests are not incidental.** They are the machine check that makes `ADR-017`'s error
contract enforceable — the ADR's stated purpose is that *"the catalogue becomes executable rather
than paper."* The split turns it back into paper.

| | |
|---|---|
| Failing | `everyCodeCatalogue04NamesIsRegisteredOrARecordedException` · `everyRegisteredCodeSourcedFromCatalogue04ActuallyAppearsInIt` · `everyRegisteredCodeHasASupportRunbookPage` · `everyRunbookPageNamesWhatSupportMustNeverDo` |
| Enforces | `ADR-017` · `EPIC-001` · `SUG-20260827-err` |
| Blast radius | `backend` CI red from the first pipeline after cutover |

### 2.1 What is NOT affected

**`ArchitectureTest` passes**, and its `docs/` references are **Javadoc only** — it reads nothing at
runtime. `S08-G4`'s ArchUnit enforcement is **not** broken by the split. Checked specifically,
because if it had been, `C-ENG-2` would have been broken too.

No other test in `services/` or `libs/` reads a repository document.

---

## 3. Why the M2.7 rehearsal could not have caught this

M2.7 passed 18/18 — six tree-hash equivalences, authorship, author-date sequences, exclusions. Every
one of those checks is about **content identity**, and by that measure the split is perfect.

**Tree hashes cannot see a runtime dependency between two repositories.** Only executing the build
can, which is exactly why `C-ENG-5` exists as a separate condition from `C-QA-1`. Swapnali asked for
this thirty-minute run in the board pack; it found a real defect that four other checks could not.

---

## 4. Options — technical read, no recommendation attached

The decision is **Mahesh** (boundary), **Amit** (implementation) and **Swapnali** (evidence
sufficiency). It is not the agent's.

| # | Option | Technical read |
|---|---|---|
| A | Move the two documents into `backend` | Fastest. But they are ratified governance artefacts under `ADR-018` / `IMP-1`, and moving them splits the governance tree — the thing `IMP-1` exists to prevent |
| B | Publish the catalogue as a **versioned artefact** from `platform-governance`; `backend` consumes it via the Package Registry | Proper decoupling and matches the baseline's contracts thinking. Costs a publish pipeline, and `ASM-017`'s Package Registry half is still unconfirmed |
| C | Move the catalogue to `product/contracts` | It *is* an interface contract — the error taxonomy is partner-consumed per `ADR-017` G9. Puts it beside OpenAPI/AsyncAPI, where consumers already look |
| D | Make the test skip when the documents are absent | **Recommend against, and Amit's `C-ENG-2` already forbids the shape**: a check that passes when its input is missing is the silent-green failure mode. It converts a red build into a disabled control |
| E | Keep a synchronised copy in `backend`, CI-verified against upstream | Works, but introduces a second copy of a ratified document — precisely the drift `ADR-017` was written to remove |

**Option D is the one that will be proposed under time pressure**, because it makes the red go away
in one line. It is the only option that removes the control rather than relocating it.

---

## 5. Status

| | |
|---|---|
| `C-ENG-5` | **FAIL** — recorded, not waived |
| M2.8 backend | **PASS on the monorepo, FAIL on the split.** Both are true and both are recorded |
| M2 | **Cannot close.** This is now a third open item alongside finding B and the binary assets |
| M5.2 | Unaffected *today* — but the split must build before it is pushed |
| Registers | `IMP-16` (GLM-001) · `RISK-028` |
