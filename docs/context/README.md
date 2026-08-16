# Context Module

**Status:** non-binding reasoning context

**Manifest:** [`context-manifest.yaml`](./context-manifest.yaml)

**Portable framework:** [`framework/`](./framework/README.md)

**Binding precedence:** [`docs/README.md`](../README.md#which-document-wins)

This module gives humans, agents and retrieval systems enough grounded context to understand a
project without confusing background with authority. Context explains and recommends; the
governance, business, platform and engineering SSOTs decide.

## Structure

```text
context/
├── context-manifest.yaml     active project, layers, roles and loading profiles
├── schemas/                  portable manifest contract
├── framework/                reusable, domain-neutral model and templates
├── business-problem-statement.md
├── roles/                    project/domain role instances
└── roadmaps/                 non-binding forward-looking options
```

The reusable framework is deliberately free of bank, insurance, regulator and named-persona
conclusions. This repository is one project instance of it.

## Current project overlay

| Entry | Purpose |
|---|---|
| [Business problem statement](./business-problem-statement.md) | Project orientation and problem/outcome framing |
| [Role index](./roles/README.md) | Canonical project role packages and cross-role protocols |
| [Roadmaps](./roadmaps/README.md) | Exploratory transformation options, never delivery commitments |

## Loading

1. Read the manifest.
2. Select the smallest loading profile that fits the decision.
3. Read the problem statement.
4. Resolve binding authority before asserting ownership or approval.
5. Load only materially affected roles/protocols.
6. Report assumptions, source status and conflicts.

Use [`framework/LOADING-PROTOCOL.md`](./framework/LOADING-PROTOCOL.md) for the complete contract.

## Validate or reuse

```bash
python scripts/context/validate-context.py
python scripts/context/new-project-context.py --help
```

On conflict, context loses. It never supplies regulation, scope, acceptance criteria, API
contracts, production authority, human sign-off or material risk acceptance.
