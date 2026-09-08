# Knowledge Hub operating guide

This directory is the **navigation/explanation layer** for the Bank Insurance Platform documentation.

## Primary access model: GitHub / GitLab

No internal web hosting is required.

The primary Knowledge Hub experience is the repository's own Markdown UI:

1. open the repository;
2. open `README.md`;
3. click **Bank Insurance Platform Knowledge Hub**;
4. navigate the Markdown links to governance, current state, backlog, decisions, architecture, APIs/services, 1SB, identity and role guides.

GitHub and GitLab both render these Markdown files directly. The committed pages under `docs/knowledge-base/generated/` make the current-state and service dashboards visible without running Python or MkDocs.

## Why generated pages are committed

`python scripts/docs/generate_knowledge_base.py` reads repository sources of truth and regenerates four derived pages:

| Generated page | Source / purpose |
|---|---|
| `generated/current-state.md` | Current workstreams, stages and gates from `governance/state/CURRENT-STATE.yaml` |
| `generated/service-inventory.md` | Registered modules from `settings.gradle.kts` |
| `generated/backlog-index.md` | Git-native navigation to the authoritative suggestion/parking/dependency/risk registers |
| `generated/decision-index.md` | Git-native navigation to the authoritative decision/change/ADR records |

These generated pages are **not authority**. They are committed only so repository viewers can see them immediately.

CI regenerates the pages and runs `git diff`. If a source changes without its generated dashboard being refreshed, the Knowledge Hub workflow fails.

## Source-of-truth rule

Do not copy authoritative content into the Knowledge Hub merely to make it look nicer.

Prefer:

1. a concise explanation;
2. a direct link to authority;
3. a generated/read-only summary only where it improves navigation.

If a generated or explanatory page disagrees with an authoritative source, the authoritative source wins.

## Refresh the committed dashboards

From the repository root:

```bash
python -m venv .venv-docs
source .venv-docs/bin/activate   # Windows PowerShell: .venv-docs\Scripts\Activate.ps1
pip install -r requirements-docs.txt
python scripts/docs/generate_knowledge_base.py
```

Commit any changed files under:

```text
docs/knowledge-base/generated/
```

This is required whenever a source feeding those dashboards changes.

## Optional richer MkDocs UI

MkDocs remains available as an optional local UI. It is **not required for ordinary reading**.

```bash
python scripts/docs/generate_knowledge_base.py
mkdocs serve
```

Or build static HTML:

```bash
mkdocs build
```

The resulting `site/` directory is an optional local/static artifact. Do not publish it publicly by default.

## CI validation

`.github/workflows/knowledge-hub.yml`:

1. installs documentation dependencies;
2. regenerates the committed dashboards;
3. fails if the committed dashboards are stale;
4. builds the optional MkDocs site;
5. rejects warnings introduced by `knowledge-base/`;
6. uploads the static site as a CI artifact.

Historical Markdown link debt outside the Knowledge Hub is not silently repaired by this workflow; it remains separate documentation debt.

## Adding knowledge

1. Put explanatory/navigation content under `docs/knowledge-base/`.
2. Link authoritative content from its original location.
3. Add globally useful pages to `mkdocs.yml` navigation.
4. Add new abbreviations to `glossary.md`.
5. Add new documentation families to `repository-map.md`.
6. If information has a canonical machine-readable source, extend the generator instead of maintaining a competing manual snapshot.

## What stays curated

Human-readable interpretation remains curated for:

- glossary meanings and namespace collisions;
- role-based reading paths;
- authority/precedence rules;
- business/domain mental models;
- the distinction between target architecture, implemented code and evidenced completion.
