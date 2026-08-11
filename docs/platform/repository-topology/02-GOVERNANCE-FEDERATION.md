# 02 — Governance Federation: one parent, many services

**Status:** Proposal under `CR-002`. Not approved.
**Parent:** [`README.md`](./README.md)

> This document answers the second half of the request: *how every service repository — and every
> agent working in one — reads the same documentation and governance from a single parent, and
> reads it **first**.*

---

## 1. The problem the parent repository solves

AIGEM exists to stop AI agents drifting. It works today because there is exactly one
`CURRENT-STATE.yaml`, one set of standing constraints, one parked backlog, and one `AGENTS.md`
telling an agent to read them before doing anything.

Split into nine repositories and the naive outcome is **nine governance models**: nine copies of
the pipeline, nine state files claiming different phases, nine parked backlogs, and an agent in
`bank-insurance-persistence-service` that has never heard of the constraint *"no second audit
database"*. The framework would then be documenting drift instead of preventing it.

[19-PORTING_GUIDE §2](../../governance/19-PORTING_GUIDE.md) describes the copy model, and it is
correct for its stated purpose — installing AIGEM into an *unrelated* repository. It is the wrong
model for nine repositories of **one platform under one governance state**, and
[19 §3](../../governance/19-PORTING_GUIDE.md) already says so for this case:

> *Multi-team platform — add a workstream per team; keep **one shared registers folder** so
> cross-team dependencies stay visible.*

This design generalises that sentence from the registers to the whole of `docs/governance/`.

---

## 2. What is common and what is not

The line is already drawn in [19 §1](../../governance/19-PORTING_GUIDE.md)'s layer table. Applied
to a federated platform:

| Layer | Artefacts | Home | Rationale |
|-------|-----------|------|-----------|
| **L1 — generic** | `00`, `03`, `05`, `06`, `07`, `08`, `09`, `10`–`19`, `RUNBOOK.md`, `templates/`, `schemas/` | **Parent**, read-only in consumers | Identical in every repo by definition. A local edit is drift, and read-only makes it impossible |
| **L2 — org** | `ORG-STANDARDS.md` | **Parent**, read-only | "Adapt once, reuse across repositories" — its own subtitle |
| **L3 — project** | `01-CURRENT_STATE.md`, `02-PROJECT_SCOPE.md`, `04-STAGE_GATES.md`, `state/CURRENT-STATE.yaml` | **Parent** | ⚠️ The non-obvious call — see §3 |
| **Registers** | `SUGGESTION-`, `PARKED-`, `DECISION-`, `DEPENDENCY-`, `RISK-`, `ASSUMPTION-REGISTER.md` | **Parent** | 19 §3, above. A dependency between two services is invisible if each repo keeps its own register |
| **Platform specs** | `docs/platform/**`, `docs/au-bank-insurance-platform/**`, `docs/context/**` | **Parent** | Cross-cutting by definition — `docs/platform/README.md` says a document belongs there when it is "true of the platform as a whole" |
| **Service SSOT** | `PRODUCT-BACKLOG.md`, `TECH-DEBT.md`, `TEST-BACKLOG.md`, `COVERAGE.md`, service architecture, API contracts | **Service repo** | Owned, changed, and reviewed by the team that owns the service. These are the *destinations* in the routing table, and they follow their service |

**The rule in one line:** *the parent owns whether work may start; the service repo owns the work.*

---

## 3. Why the state file and registers stay central

This is the decision most likely to be argued with, so here is the reasoning explicitly.

The alternative — a `CURRENT-STATE.yaml` per service repo — is superficially attractive: each
service knows its own phase. It fails on four counts:

1. **Workstreams already span repositories.** WS-2 is one lifecycle across *three* services. Three
   state files describing one gate (`GATE-IAM-P1`, criteria A.1–A.6) means three places to
   disagree about whether A.3 is met.
2. **The gate is the unit, not the repo.** `GATE-P4` criterion 4.1 ("sandbox E2E suite runs in
   CI") spans integration *and* persistence. A per-repo gate cannot express it.
3. **The ID scheme was already designed for this.** `CURRENT-STATE.yaml` explains that `SUG` and
   `DEP` are collision-resistant precisely because *"two agents on two branches reading the same
   counter would mint the same ID"*. Two branches, or nine repositories — same problem, and the
   design already anticipates it.
4. **`FreshnessCheck` becomes meaningless when forked.** Its whole job is to prove one state file
   is current. Nine state files with nine `state_as_of` dates cannot be checked as one truth.

**Cost, stated honestly.** Central registers mean a triage in a service repo must land a row in
the parent — a second PR, in a second repository. Mitigation in §7. This is a real cost, not a
rounding error, and it is the main thing an approver is trading for a single source of truth.

**What changes in the state file.** A `repos:` block must be added so an agent can resolve *which
workstream owns the repository it is standing in*:

```yaml
repos:
  bank-insurance-1sb-integration-service:      { workstream: WS-1, service: 1sb-integration-service }
  bank-insurance-persistence-service:          { workstream: WS-1, service: bank-persistence-service }
  bank-insurance-workforce-access-bff:         { workstream: WS-2, service: workforce-access-bff }
  bank-insurance-identity-provider-adapter-service: { workstream: WS-2, service: identity-provider-adapter-service }
  bank-insurance-identity-authorization-service:    { workstream: WS-2, service: identity-authorization-service }
  bank-insurance-common-libs:                  { workstream: WS-1, service: shared-libraries }
```

That is a change to `current-state.schema.json` and therefore a `GOV` change, folded into
`CR-002` rather than smuggled in.

The `routing` table also needs re-pointing: its destinations become
`<repo>:docs/PRODUCT-BACKLOG.md`-style qualified paths, because a bare relative path no longer
resolves from the parent. Two of its entries are **already stale today** and would break loudly
on the move (`docs/architecture-review/…` — the file lives at
`docs/platform/architecture-review/…`). Fixing them is a prerequisite of Wave 0, not a side
effect.

---

## 4. How a service repository consumes the parent

### 4.1 Mechanism — pinned git submodule

```bash
git submodule add https://github.com/<org>/bank-insurance-governance .governance
cd .governance && git checkout v1.4.0 && cd ..
git add .governance .gitmodules && git commit -m "Pin governance v1.4.0"
```

Every repo then has the entire governance model on disk at `.governance/`, at a **known
version**.

**Why a pinned tag and not `main`.** A floating pointer means a governance edit changes the rules
of an in-flight work item in nine repositories, mid-sprint, with no PR in any of them. Pinning
makes a rule change an explicit, reviewable commit in each consumer — which is exactly what
[14-CHANGE_CONTROL](../../governance/14-CHANGE_CONTROL.md) requires of a `GOV` change. Staleness
is then handled by §6 rather than by hope.

### 4.2 Alternatives considered

| Option | Why not |
|--------|---------|
| **Copy per repo** ([19 §2](../../governance/19-PORTING_GUIDE.md)) | Nine forks of the pipeline. This is the failure mode being designed against |
| **Subtree + sync bot** | Files always present, no submodule friction — but between syncs there are nine versions, and the bot's PRs are noise the team learns to rubber-stamp |
| **Fetch at agent runtime** (`raw.githubusercontent.com`) | No pinning, no offline, no audit trail; fails in network-restricted CI and sandboxes. Rejected |
| **Published tarball via bootstrap script** | Good, and it is kept — as the *fallback* in §5, for environments where submodules are unavailable |

### 4.3 The agent entry point

Each service repo gets a **generated** `AGENTS.md` (and `CLAUDE.md` pointing at it). Generated
from `.governance/templates/AGENTS-STANZA.md` by a parent-owned script, so nine copies cannot
drift into nine different agent contracts:

```markdown
# AGENTS.md — bank-insurance-1sb-integration-service

## Read this before anything else

This repository is governed by **bank-insurance-governance**, vendored at `.governance/`.
It is the source of truth for whether work may start. This repo is the source of truth
for the work itself.

1. `bash scripts/governance-bootstrap.sh`        — ensures .governance/ is present and at the pin
2. `java .governance/scripts/governance/FreshnessCheck.java --repo bank-insurance-1sb-integration-service`
   exit 0 fresh · 1 warn (disclose it) · 2 do NOT admit new work
3. Read `.governance/docs/governance/state/CURRENT-STATE.yaml` — this repo is **WS-1**;
   read that workstream's phase, objective, scope, gate and standing constraints.
4. Read `.governance/docs/governance/RUNBOOK.md` §8 — the ten facts and §8.3, how your
   thinking must change at this stage.
5. Follow `.governance/docs/governance/09-AI_EXECUTION_RULES.md` — the binding contract.
6. Triage every input through `/aigem-triage` **before** writing code. Record the outcome
   with `bash .governance/scripts/governance/record.sh` (§7) — it opens the register PR
   against the parent for you.

Service-scoped documents live in `docs/` **in this repository**: PRODUCT-BACKLOG,
TECH-DEBT, TEST-BACKLOG, COVERAGE, architecture, API contracts.

Core rules (full text in `.governance/`):
- A suggestion is never implemented in the turn it is raised.
- Exactly one work item in flight; only the P1 override classes interrupt.
- Parked is not deleted — target stage + unpark trigger, always.
- Standing constraints are platform-wide and are enforced here too.
```

Steps 1–3 are what the request means by *"agents will refer to this documentation first, then work
on the service"*, made mechanical rather than aspirational.

### 4.4 The triage skill travels too

`.claude/skills/aigem-triage/SKILL.md` currently lives in this repo and links to
`../../../docs/governance/…`. In a service repo those relative paths must resolve to
`.governance/docs/governance/…`. The skill therefore moves into the parent
(`skills/aigem-triage/`) and is installed by the bootstrap script, with the paths rewritten once,
centrally — not hand-edited nine times.

---

## 5. The fallback: no submodule required

Some environments (shallow CI checkouts, cloud agent sandboxes, `git clone` without
`--recurse-submodules`) will land without `.governance/`. Every repo carries a bootstrap script
so the agent's first instruction never fails:

```bash
#!/usr/bin/env bash
# scripts/governance-bootstrap.sh — idempotent; safe to run every session.
set -euo pipefail
PIN="$(git config -f .gitmodules submodule..governance.pin || echo v1.4.0)"

if [ -f .governance/docs/governance/state/CURRENT-STATE.yaml ]; then
  echo "governance present at $PIN"; exit 0
fi

if git submodule update --init --depth 1 .governance 2>/dev/null && \
   [ -f .governance/docs/governance/state/CURRENT-STATE.yaml ]; then
  echo "governance restored from submodule at $PIN"; exit 0
fi

# Fallback: fetch the release tarball of the pinned tag.
mkdir -p .governance
curl -fsSL "https://github.com/<org>/bank-insurance-governance/archive/refs/tags/${PIN}.tar.gz" \
  | tar -xz --strip-components=1 -C .governance
echo "governance fetched from ${PIN} tarball"
```

`.governance/` is committed as a submodule reference, so the tarball path is only ever a
degraded-mode recovery — never the normal one.

---

## 6. Keeping the pins honest

A pin that nobody bumps is a copy with extra steps. Three mechanisms, mirroring the freshness
thresholds AIGEM already uses:

| Mechanism | Where | Behaviour |
|-----------|-------|-----------|
| **Pin check** | Every service repo's `governance.yml` (3 lines, `uses:` the parent's reusable workflow) | Warns when the pin is 1 release behind; **fails** at 2 releases or 30 days, matching `state_as_of`'s existing 30-day limit |
| **Fan-out PR** | Parent, on tag | Opens one "bump governance to vX.Y.Z" PR in every consumer, with the parent's changelog in the body |
| **Org audit** | Parent, weekly cron | One issue listing every `bank-insurance-*` repo and its pin. The Delivery Lead sweeps it at the Governance Sync |

The reusable workflow is the same two-tier design as today's `.github/workflows/governance.yml` —
`FreshnessCheck` on the plain JDK 21 + Git baseline, `ci-checks.py` behind Python — so consumers
inherit both tiers without copying either.

---

## 7. The cross-repo write path

Triage happens in a service repo; the register row belongs in the parent. Without tooling this is
"clone a second repository, edit a table, open a PR" — friction that guarantees the registers
stop being written.

`record.sh` closes it. The agent emits a triage record as YAML (already the required output
format, already schema-validated), and the script dispatches it to a parent workflow that appends
the row and opens the PR:

```bash
.governance/scripts/governance/record.sh triage.yaml
#  → validates against .governance/docs/governance/schemas/triage-record.schema.json
#  → gh workflow run record-triage.yml -R <org>/bank-insurance-governance -f record=@triage.yaml
#  → prints the PR URL; the agent quotes it in the service-repo PR body
```

Three properties make this safe:

- **Collision-resistant IDs.** `SUG-<YYYYMMDD>-<3 chars>` needs no shared counter — nine repos can
  mint concurrently, which the current design already assumes.
- **Append-only rows.** One row per record means merge conflicts are near-impossible.
- **Schema-validated at the door.** The parent's `ci-checks.py` already validates every tagged
  `# schema:` block; the record is rejected before it lands, not after.

Sequential, human-owned IDs (`CR`, `RISK`, `ASM`, `ADR`) keep their counters and stay human-minted
in the parent — unchanged from today.

---

## 8. What an agent's session looks like afterwards

```text
agent starts in bank-insurance-1sb-integration-service
  │
  ├─ 1. scripts/governance-bootstrap.sh          .governance/ present at v1.4.0
  ├─ 2. FreshnessCheck --repo <this repo>        exit 0 → full pipeline
  ├─ 3. CURRENT-STATE.yaml                       repos[] → WS-1 → Phase 4, GATE-P4 open,
  │                                              standing constraints, known debt
  ├─ 4. RUNBOOK §8.3                             L7 Hardening → "prove it, don't extend it"
  ├─ 5. PARKED-BACKLOG.md                        do not re-propose TD-006/007/009/010/…
  ├─ 6. /aigem-triage <the input>                verdict
  │      └─ record.sh → PR against the parent
  └─ 7. work happens HERE, in this repo, against this service's backlog
```

Steps 1–6 are identical in all nine repositories, because they are the same files.
Step 7 is the only part that differs — which is the entire point of the split.
