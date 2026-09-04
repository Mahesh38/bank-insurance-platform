# Board 1 — Architecture · Position on CR-014

**Change request:** [CR-014](../../CR-014-gitlab-estate-migration.md) · **Plan:** [`GLM-001`](../../../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Board:** 1 — Architecture · **Role:** `R2`
**Persona:** Mahesh — Principal Insurance Platform Architect
**Reviewer type:** `AGENT` · **Self-review:** false · **Change tier:** `T4` · **Date:** 2026-08-29

> ### ⚠ AI-drafted — the mandatory human Architecture signature is NOT satisfied
> An AI may draft the architecture reasoning. It may not impersonate the human T4 Architecture
> sign-off.
>
> **`signature_status: AI-DRAFTED — mandatory human Board 1 signature outstanding`**

> ## Drafted verdict: `APPROVE-WITH-CONDITIONS`
> **Architecture severity: `A2` — manageable debt, dated.** No `A0`. One `A1` risk, which the
> conditions contain rather than remove.

---

## 1. The distinction the whole review turns on

> **Capability ≠ bounded context ≠ deployable unit ≠ code module.** Never conflate them.

CR-014 moves **code modules between repositories**. A repository is a code-module container. It is
not a capability, not a bounded context, and not a deployable unit. On that reading — the correct
one — this change touches no architecture at all, and my approval is close to procedural.

The `A1` risk is that the two get conflated **during execution**, because a repository split is the
cheapest moment in the platform's life to also move a boundary, and nobody will notice for months.
Every condition below exists to hold that line.

I note with approval that GLM-001 §6 already says so in its own words: *"a repository move that also
moves a boundary is a migration nobody can audit."* That is the right instinct and I am making it
binding.

---

## 2. Findings

### `ARC-F01` · `A1` — the split is the cheapest moment to smuggle a boundary change

`settings.gradle.kts` includes five services and five libraries. After the split, `backend` holds
all ten and `frontend` holds none of them — the Flutter application has no Gradle coupling, so the
seam is clean and requires no judgement. That is the good case, and it is the case we have.

The risk is the tempting adjacent moves: merging two libraries because they end up next to each
other, promoting a package to a module because the new tree "reads better", or resolving IMP-2 by
quietly splitting the persistence service while everything is being rewritten anyway. Each is a
`A0`-class change dressed as a file move.

**Condition `C-ARC-1`:** the split changes zero service boundaries. `settings.gradle.kts` after the
migration contains the same ten modules, in the same relationships, as before. The only permitted
delta is `rootProject.name` (IMP-12), which is a rename, not a boundary.

### `ARC-F02` · `A2` — IMP-1 is architecturally right, and it is not mine to grant

The governance tree is a genuine ownership boundary: it is owned by a different set of people, has a
different change cadence, a different review path and a different consumer set from either the
frontend or the backend. On the internal architecture question — *is `platform-governance` a
legitimate boundary?* — my answer is **yes**, and the alternatives are worse for the reasons CR-014
§6 sets out.

But the seven-project topology is the **bank's** approved baseline. Adding a ninth project is an
exception to a document this repository does not own. My jurisdiction covers architecture
exceptions *within* the platform; it does not extend to exceptions against an external authority's
approved baseline. The repository owner's acceptance of IMP-1 makes it our proposal. It does not
make it granted.

**Condition `C-ARC-2`:** `governance/platform-governance` is not created until the bank's GitLab
platform or architecture authority accepts the Appendix C exception in writing.

### `ARC-F03` · `A2` — I am declining the persistence question, and the reason matters

IMP-2 identifies a real conflict: the bank baseline §3.3 forbids a generic shared persistence
service for all domains, and the decision register carries *"Persistence is platform-common
(`bank-persistence-service`), reached over HTTP"* with status **Accepted**.

I am not resolving it here, on two grounds and each alone is sufficient.

First, **jurisdiction.** Shared datastore, cross-service database access and source-of-truth change
are a mandatory joint review with Aarti before any verdict. I do not hold this alone, and CR-014's
approver set does not include her.

Second, **process.** Reversing an *Accepted* decision requires the new-evidence grounds in
[14 §6](../../../14-CHANGE_CONTROL.md#6-reversing-a-rejection). "An external baseline disagrees" is
plausibly such evidence — that is exactly what [`CR-015`](../../CR-015-shared-persistence-vs-bank-baseline.md)
is for — but it is a decision with its own reasoning, its own alternatives and its own approvers.
It is not a rider on a repository move.

**Condition `C-ARC-3`:** the migration carries `bank-persistence-service` across **as-is**. CR-015
runs in parallel and its outcome is applied afterwards, on its own evidence.

### `ARC-F04` · `A2` — `service.yaml` records ownership, it does not invent it

IMP-12 proposes seeding `service.yaml` per service from the table in `AGENTS.md`. Supported — the
convention is worth reserving from day one, and generating it while every service directory is
already open is the cheap moment.

One caution. `service.yaml` carries `owner`, `dependencies`, `criticality` and
`independentlyDeployable`. Those are architecture statements. Written from what the code currently
does rather than from what is ratified, they become a second, unreviewed source of truth about
ownership that later tooling will treat as authoritative.

**Condition `C-ARC-4`:** every `service.yaml` field traces to a ratified source — `AGENTS.md`, an
ADR, or the architecture decision log. Anything not traceable is left absent, not guessed.

### `ARC-F05` · `A3` — horizon

This is `H1` work: the delivery and control plane moving to its target home, with no `H2`/`H3`
runtime commitment. GLM-001's separation of the SCM move from the Render → EKS re-platform (IMP-5)
keeps that horizon honest, and I support it for that reason as much as for the scheduling one.

---

## 3. Conditions

| ID | Condition | Must be true before |
|---|---|---|
| `C-ARC-1` | Zero service-boundary change in the split. Post-migration `settings.gradle.kts` holds the same ten modules in the same relationships; `rootProject.name` is the only permitted delta | GLM-001 M5.3 |
| `C-ARC-2` | Bank Appendix C exception accepted in writing before `governance/platform-governance` is created | GLM-001 M4.3 |
| `C-ARC-3` | `bank-persistence-service` migrates as-is; CR-015 decides its future separately and afterwards | GLM-001 M5.2 |
| `C-ARC-4` | Every `service.yaml` field traces to a ratified source; untraceable fields are omitted, not guessed | GLM-001 M5.8 |
| `C-ARC-5` | `CODEOWNERS` reflects ratified ownership, group-based; it does not create ownership that no ADR records | GLM-001 M5.9 |

---

## 4. What I am not deciding

- **Not** the persistence conflict (`ARC-F03`) — joint with Aarti, on CR-015.
- **Not** the bank's Appendix C exception — that authority is external to this repository.
- **Not** operational readiness. Shivanshi's Board 7 posture is not an architecture judgement, and I
  do not manufacture it.
- **Not** whether the eventual gate evidence is sufficient. That is Swapnali's.
- **Not** data residency (`CMP-F01`). Shailja raised it; a regulatory conclusion is not downgradable
  by an `A`-rating, mine included.
- I **cannot** satisfy my own signature. The human Board 1 signature on `CR-014` is outstanding.
