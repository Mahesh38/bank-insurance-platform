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

## Run locally

From the repository root:

```bash
python -m venv .venv-docs
source .venv-docs/bin/activate   # Windows PowerShell: .venv-docs\Scripts\Activate.ps1
pip install -r requirements-docs.txt
mkdocs serve
```

Open the local address printed by MkDocs (normally `http://127.0.0.1:8000`).

## Build static HTML

```bash
mkdocs build
```

The generated `site/` directory is a static website that can be served by an approved internal web server, artifact server, container or platform route. **Do not publish it publicly by default.** Hosting choice is an operational/security decision separate from this documentation UI.

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
6. If you add a service/API, update `api-service-catalogue.md` or automate that catalogue from code metadata.

## Recommended next automation

The first version is curated. A later improvement should generate parts of the portal from source files so they cannot drift:

- workstream/stage/gate cards from `CURRENT-STATE.yaml`;
- READY/PARKED/BLOCKED counts from governance registers/backlogs;
- ADR/CR signature status from the decision register and change-request files;
- service list from `settings.gradle.kts`;
- API list from OpenAPI/contracts/controllers where a canonical machine-readable source exists;
- glossary lint to flag undefined identifier prefixes.

Generation should read authoritative files and write derived pages only; it must not mutate lifecycle state or approvals.
