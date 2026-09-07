# Knowledge Hub bypass record

**Record:** `SUG-20260907-kb1`  
**Action:** `ADMIT-BYPASS`  
**Authorised by:** human repository owner in conversation, 2026-09-07  
**Scope:** centralized, UI-based project Knowledge Hub and its documentation-only generation/build tooling.

## Explicit instruction

The human owner explicitly authorised this Knowledge Hub work to proceed while bypassing the normal governance admission/review pipeline.

## What was skipped

Normal SF/SC classification, necessity/priority scoring, risk-tier board routing and admission/parking processing for this documentation/navigation work item.

## What was not bypassed

The override does **not** convert any existing Candidate/Proposed/AI-Drafted architecture, product, security or compliance decision into an approval. It does not modify lifecycle stage fields, gate evidence, runtime APIs, PII handling, payment behaviour, authorization rules, production topology or regulatory controls.

## Risk statement

The portal can mislead users if curated explanations or generated parsers drift from repository authorities. Mitigation: authoritative sources remain primary; generated pages are read-only derived views; the Knowledge Hub CI rebuilds the site from the current branch; every page states the source-of-truth rule.

## Related implementation

- `mkdocs.yml`
- `scripts/docs/generate_knowledge_base.py`
- `.github/workflows/knowledge-hub.yml`
- `docs/knowledge-base/`

This file exists only to make the human override discoverable from inside the repository. It is not an approval record for any other work item.
