# Artefact disposition — product repository, file group by file group

Paths are in `mahesh38/bank-insurance-platform`. **Stays** = product/architecture knowledge, untouched
(FLOW governs process, not content). **Link** = stays, and a context pack links to it.
**Archive** = moved read-only to `docs/archive/aigem-1.5/` at M4, byte-identical.
**Retire** = archived and no successor. **Replace** = archived; successor named.

## Governance (`docs/governance/`)

| Path | Disposition | Successor / note |
|---|---|---|
| `00-GOVERNANCE.md` … `19-PORTING_GUIDE.md` | Replace | `docs/01`–`09` here; concept-by-concept in [`concept-map.md`](./concept-map.md) |
| `README.md`, `RUNBOOK.md` | Replace | `README.md`, `docs/08` cadence, persona cards |
| `PERSONA-AUTHORITY-MATRIX.md` | Replace | `personas/decision-rights.md`; PA-1/PA-2 kept in `docs/05 §6` |
| `DELIVERY-CONTROL-SYSTEM.md` | Replace | `docs/04`; its §11 parallelisation becomes the default |
| `ENGINEERING-AND-SECURE-CODING-STANDARDS.md`, `ORG-STANDARDS.md`, `PR-REVIEW-CHECKLIST.md` | **Stays** | These are engineering content, not flow governance. Linked from the engineering and security cards' lanes |
| `DEC-20260825-01-lead-domain-decisions.md` | **Stays** (move beside the decision log) | Product decision content |
| `state/CURRENT-STATE.yaml` | Archive | Standing constraints → `state/guardrails.yaml`; objectives → `state/outcomes.yaml`; gates → [`carry-over.md`](./carry-over.md) |
| `state/GATE-EVIDENCE.yaml`, `state/REVIEW-LOG.md` | Archive | Evidence already produced is referenced from checkpoint records |
| `registers/DECISION-REGISTER.md` | **Stays** as the ADR log (renamed `docs/architecture/DECISION-LOG.md`) | Open rows indexed in `state/decisions.yaml` |
| `registers/SUGGESTION-REGISTER.md` | Retire | Tracker replaces it; history archived |
| `registers/PARKED-BACKLOG.md` | Archive | Items → tracker `Later` with `review_by` ([`carry-over.md`](./carry-over.md)) |
| `registers/DEPENDENCY-REGISTER.md` | Archive | `state/dependencies.yaml` |
| `registers/RISK-REGISTER.md`, `ASSUMPTION-REGISTER.md` | **Stays** (move to `docs/architecture/`) | Risks and assumptions are content; open ones get cards or decisions where actionable |
| `change-requests/**` (CR-002…CR-016 + verdicts) | Archive | Their **outcomes** (scope, ADRs, rule packs) already live in product docs |
| `plans/**` | **Stays** where still in flight (PLAN-002…005, EPIC-002); PLAN-001 archive | Linked from packs |
| `schemas/**`, `templates/**` | Replace | `templates/` here; ADR template stays for the ADR log |
| `workstreams/**` | Archive | `state/lanes.yaml`; WS-3 charter scope content → distribution-journey pack links |
| `autopilot/README.md` | Replace | Flow-steward card |

## Context layer (`docs/context/`, `.claude/`)

| Path | Disposition | Successor / note |
|---|---|---|
| `BOOT.md` | Replace | `AGENTS.md` (tier 0) |
| `AGENT-CONTEXT-INDEX.yaml`, `DOC-MAP.yaml`, `context-manifest.yaml`, `schemas/`, `framework/` | Replace | `context/context-map.yaml`, `context/README.md` |
| `personas/*.card.md`, `AUTHORITY-QUICK-CARD.md` | Replace | `personas/cards/*.md`, `personas/decision-rights.md` |
| `roles/<persona>/**` packages | Split: domain knowledge → **Link** from packs where still true; procedure → Retire | Rule of thumb: a paragraph that would be true whoever read it is knowledge; one that tells a persona how to behave is procedure |
| `roles/*-agentic-ai-evolution.md`, duplicate `mahesh-solution-architect.md`, `rajal-product-owner.md` | Retire | Duplicated identities (diagnosis F7) |
| `roles/shared/*-protocol.md`, `cross-persona-operating-model.md` | Replace | `docs/05 §6–7` |
| `business-problem-statement.md` | **Stays** | Linked from distribution-journey pack |
| `roadmaps/**` | Archive | `state/outcomes.yaml` |
| `.claude/skills/aigem-triage`, `.claude/skills/context-load` | Replace | `flow-intake`, `load-context`, `advise` |

## Lifecycle Bible (`docs/application-lifecycle-bible/`)

| Path | Disposition | Successor / note |
|---|---|---|
| `00`–`04`, `stages/S00–S15`, `evidence/**` | Archive | Maturity grid `docs/07`; evidence referenced from checkpoints |
| `05-DOCUMENTATION-CANON.md`, `06-QUALITY-NORMS.md`, `07-SECURITY-COMPLIANCE-CANON.md`, `08-SRE-READINESS-CANON.md` | **Link** | Quality, security/compliance and SRE content feeds the prod checkpoint and the respective cards |
| `09-JIRA-MODEL.md`, `10-ACTOR-EPIC-STORY-MAP.md`, `backlog/**` | **Link** during M1 import, then Archive | Source for tracker import |

## Product, architecture and service docs

| Path | Disposition |
|---|---|
| `docs/au-bank-insurance-platform/**` (BRDs, decision log, rule packs, knowledge base) | **Stays** — behaviour SSOT. `po-drive/03-PROGRAMME-TODO.md` → imported to tracker at M1, then archived |
| `docs/1sb-insurance-integration/**` | **Stays** — service SSOT. `service-ssot/PRODUCT-BACKLOG.md`, `TECH-DEBT.md`, `TEST-BACKLOG.md` → imported to tracker at M1, then frozen with a pointer |
| `docs/platform/**`, `docs/architecture/**`, `docs/journey-execution/**`, `docs/figma/**` | **Stays** — linked from packs |
| `docs/knowledge-base/**` | **Stays**; `bypass-record.md` archived with AIGEM |

## Scripts and CI

| Path | Disposition |
|---|---|
| `scripts/governance/FreshnessCheck.java`, `test-freshness-check.sh` | Retire (M3.4) — staleness becomes a warning in `validate.py` |
| `scripts/governance/ci-checks.py` | Replace — keep any check that enforces a guardrail; drop process checks (BOOT drift, capsule budgets, matrix calibration) |
| `scripts/governance/autopilot.py` | Replace — flow steward |
| `scripts/governance/measure-pipeline-feedback.py` | **Stays** — it measures CI, a real metric |
| `scripts/governance/evidence/**` | Archive with AIGEM (evidence of S08 criteria) |
| `scripts/context/*` | Replace — `scripts/validate.py route` |
| `.github/workflows/governance.yml` | Replace — run `validate.py` from this repo; keep guardrail checks |
| `.github/workflows/application-ci.yml`, `security-scanning.yml` | **Stays** — they are guardrails GR-ENG-01/02 |
