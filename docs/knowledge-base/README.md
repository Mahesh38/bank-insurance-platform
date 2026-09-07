# Knowledge Hub operating guide

This directory contains the **navigation/explanation layer** for the Bank Insurance Platform documentation portal.

## Why MkDocs

The project already stores its knowledge as Markdown in Git. MkDocs lets us turn those existing files into a searchable, tabbed static site without introducing a wiki or moving ownership outside the repository.

The portal therefore remains:

- version-controlled with the code and architecture;
- accessible to every role with repository/internal-site access;
- searchable;
- navigable by tabs/sections;
- deployable as static HTML on any approved internal hosting;
- explicit about which source is authoritative.

## Live generated views

Some portal pages are generated at build time by `scripts/docs/generate_knowledge_base.py`.

The generator reads only authoritative repository sources and writes disposable derived Markdown under `docs/knowledge-base/generated/`:

| Generated page | Source |
|---|---|
| Live current state | `docs/governance/state/CURRENT-STATE.yaml` |
| Backlog / suggestion index | `docs/governance/registers/SUGGESTION-REGISTER.md` + `PARKED-BACKLOG.md` |
| Decision index | `docs/governance/registers/DECISION-REGISTER.md` |
| Module / service inventory | `settings.gradle.kts` + current source tree |

Generated pages are intentionally **not authority** and are not used to change stage, gate, approval or backlog state.

## Run locally

From the repository root:

```bash
python -m venv .venv-docs
source .venv-docs/bin/activate   # Windows PowerShell: .venv-docs\Scripts\Activate.ps1
pip install -r requirements-docs.txt
python scripts/docs/generate_knowledge_base.py
mkdocs serve
```

Open the local address printed by MkDocs (normally `http://127.0.0.1:8000`).

Whenever `CURRENT-STATE.yaml`, governance registers, service modules or source controllers change, rerun the generator before refreshing the portal.

## Build static HTML

```bash
python scripts/docs/generate_knowledge_base.py
mkdocs build
```

The generated `site/` directory is a static website that can be served by an approved internal web server, artifact server, container or platform route. **Do not publish it publicly by default.** Hosting choice is an operational/security decision separate from this documentation UI.

## CI validation

`.github/workflows/knowledge-hub.yml` validates portal changes and source changes that feed the portal. It:

1. installs the documentation-only Python dependencies;
2. regenerates the live pages from the checked-out branch;
3. runs `mkdocs build --strict`;
4. uploads the built static site as a CI artifact.

This gives reviewers a reproducible documentation build without deploying anything publicly.

## Content rule

Do not copy whole authoritative documents into `knowledge-base/` just to make them look nicer.

Prefer:

1. a concise explanation,
2. a link to the authority,
3. a status/interpretation note where useful.

This keeps the portal from becoming a competing source of truth.

## Adding a page

1. Put explanatory/navigation content under `docs/knowledge-base/`.
2. Link authoritative content from its original location under `docs/`.
3. Add the page to `mkdocs.yml` navigation if it should be globally visible.
4. If you introduce a new abbreviation, add it to `glossary.md`.
5. If you introduce a new documentation family, add it to `repository-map.md`.
6. If information already has a machine-readable or canonical repository source, prefer extending the generator instead of creating another manually maintained status page.

## What remains curated

Some knowledge cannot be safely generated from identifiers alone and therefore stays human-readable/curated:

- glossary meanings and namespace-collision explanations;
- role-based reading paths;
- repository authority/precedence explanation;
- business/domain mental models;
- explanation of the difference between target architecture, implemented code and evidenced completion.

The automation goal is **less drift**, not replacing human explanation with generated tables.
