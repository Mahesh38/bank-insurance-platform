# Portable Context Framework

This folder is the reusable part of the context module. It contains no bank, insurer, product,
regulator, employee or project-specific conclusion. A new project copies this framework and the
project template, then supplies its own problem statement, authority sources, roles and overlays.

## Contract

| Layer | Portable? | Purpose | May decide? |
|---|---:|---|---:|
| Framework | Yes | Context structure, loading, validation and maintenance | No |
| Project | No | Problem, outcomes, stakeholders and constraints | Only through referenced SSOT |
| Domain | Usually no | Vocabulary, journeys, rules and evidence expectations | Only through referenced SSOT |
| Roles | Template yes; named instances no | Reasoning lenses and handoffs | Only where governance grants it |
| Protocols | Usually yes | Cross-role decision packages and escalation | No independent authority |

Read [CONTEXT-MODEL.md](./CONTEXT-MODEL.md) for the data model and
[LOADING-PROTOCOL.md](./LOADING-PROTOCOL.md) for deterministic loading.

## Create another project

```bash
python scripts/context/new-project-context.py \
  --id claims-modernisation \
  --name "Claims Modernisation" \
  --domain "property and casualty claims" \
  --repository-root ../claims-modernisation \
  --output ../claims-modernisation/docs/context
```

Then edit the generated problem statement and manifest and run:

```bash
python scripts/context/validate-context.py \
  --repository-root ../claims-modernisation \
  --manifest ../claims-modernisation/docs/context/context-manifest.yaml
```

The scaffolder refuses absolute or cross-repository manifest references. CI exercises the same
flow against a different sample domain with `python scripts/context/test_context.py`.

The generated module is self-contained: it carries this framework, the manifest schema and the
two `scripts/context/` tools into the target repository. Domain packs and personas are added only
when a named consumer and authority boundary exist.
