# Context Module

**Status:** non-binding reasoning context

**Start here (agents):** [`BOOT.md`](./BOOT.md) — tier 0 · [`AGENT-CONTEXT-INDEX.yaml`](./AGENT-CONTEXT-INDEX.yaml) — task routing

**Manifest:** [`context-manifest.yaml`](./context-manifest.yaml)

**Portable framework:** [`framework/`](./framework/README.md)

**Manifest contract:** [`schemas/context-manifest.schema.json`](./schemas/context-manifest.schema.json) — what `validate-context.py` checks a manifest against

**Binding precedence:** [`docs/README.md`](../README.md#which-document-wins)

This module gives humans, agents and retrieval systems enough grounded context to understand a
project without confusing background with authority. Context explains and recommends; the
governance, business, platform and engineering SSOTs decide.

## Structure

```text
context/
├── BOOT.md                   TIER 0 — the ten facts, generated from CURRENT-STATE.yaml
├── AGENT-CONTEXT-INDEX.yaml  TIER 1 — task -> exact, budgeted read list (19 capsules)
├── DOC-MAP.yaml              TIER 1 — every document under docs/, routed (generated)
├── personas/                 TIER 1 — decision cards; adopt a persona in 3-6 KB
├── context-manifest.yaml     active project, layers, roles and loading profiles
├── schemas/                  portable manifest contract — context-manifest.schema.json
├── framework/                reusable, domain-neutral model and templates
├── business-problem-statement.md
├── roles/                    TIER 2 — full role packages; opened only on a named condition
└── roadmaps/                 non-binding forward-looking options
```

## The three tiers

Loading everything relevant is not the same as loading everything. The tiers exist so an agent
spends its context window on the work rather than on finding the work.

| Tier | What | Cost | When |
|---|---|---|---|
| **0** | [`BOOT.md`](./BOOT.md) | ~17 KB | Every session, before anything else |
| **1** | One capsule from [`AGENT-CONTEXT-INDEX.yaml`](./AGENT-CONTEXT-INDEX.yaml) + the persona [card](./personas/README.md) it names | ~6–20 KB | Once the task is known |
| **2** | A named file inside a [role package](./roles/README.md) or an SSOT document | as needed | Only when a card's *Load deeper* row matches |

```bash
python3 scripts/context/context-load.py resolve "<the request>"   # tier 1, resolved for you
```

### What the tiers buy

Measured in bytes of source an agent actually opens. *Before* is the previous mandatory boot set —
`AGENTS.md` + `CURRENT-STATE.yaml` + `RUNBOOK.md` + `09-AI_EXECUTION_RULES.md` +
`governance/README.md` + `PERSONA-AUTHORITY-MATRIX.md` — plus the persona package or gate document
the task then required. *After* is `BOOT.md` plus the capsule's always-load list.

| Task | Before | After | Saved |
|---|---:|---:|---:|
| Session boot | 179,828 | 17,301 | **90%** |
| Boot + architecture review | 423,641 | 28,647 | **93%** |
| Boot + security review | 255,280 | 24,232 | **91%** |
| Boot + scaling question | 252,179 | 27,750 | **89%** |
| Boot + compliance review | 245,199 | 28,002 | **89%** |
| Boot + triage an input | 210,013 | 29,056 | **86%** |
| Boot + Java code change | 210,013 | 36,173 | **83%** |

At roughly four bytes per token, session boot falls from about 45,000 tokens to about 4,300. The
saving is not the point on its own — the point is that what remains is the part that changes the
answer, so the reasoning happens in the window instead of the retrieval.

Re-measure any capsule at any time:

```bash
python3 scripts/context/context-load.py list      # every capsule, real cost against its budget
```

The reusable framework is deliberately free of bank, insurance, regulator and named-persona
conclusions. This repository is one project instance of it.

## Current project overlay

| Entry | Purpose |
|---|---|
| [Boot capsule](./BOOT.md) | The ten facts, stage posture, open gates, standing constraints, known debt |
| [Agent context index](./AGENT-CONTEXT-INDEX.yaml) | Task-to-context routing with enforced budgets |
| [Persona cards](./personas/README.md) | Adopt a persona without loading its package |
| [Business problem statement](./business-problem-statement.md) | Project orientation and problem/outcome framing |
| [Role index](./roles/README.md) | Canonical project role packages and cross-role protocols |
| [Roadmaps](./roadmaps/README.md) | Exploratory transformation options, never delivery commitments |

## Loading

0. Read [`BOOT.md`](./BOOT.md), then resolve the task to a capsule. Steps 1–6 below are the
   underlying contract that the capsules implement.
1. Read the manifest.
2. Select the smallest loading profile that fits the decision.
3. Read the problem statement.
4. Resolve binding authority before asserting ownership or approval.
5. Load only materially affected roles/protocols.
6. Report assumptions, source status and conflicts.

Use [`framework/LOADING-PROTOCOL.md`](./framework/LOADING-PROTOCOL.md) for the complete contract.

## Validate or reuse

```bash
python3 scripts/context/validate-context.py        # the portable manifest and every path it names
python3 scripts/context/context-load.py validate   # capsule paths, anchors, budgets, persona agreement
python3 scripts/context/build-boot-capsule.py      # regenerate BOOT.md after a state change
python3 scripts/context/new-project-context.py --help
```

The routing layer is portable too: `AGENT-CONTEXT-INDEX.yaml` carries the *shape* (tiers, rules,
capsule structure, budgets) while its capsule contents are project-specific. Reuse the shape;
rewrite the contents.

On conflict, context loses. It never supplies regulation, scope, acceptance criteria, API
contracts, production authority, human sign-off or material risk acceptance.
