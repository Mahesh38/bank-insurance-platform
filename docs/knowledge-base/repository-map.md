# Repository and documentation map

Use this page when the question is **“where do I go for this?”**

## Authority map

| Area | Question answered | Primary location | Authority note |
|---|---|---|---|
| Governance | Should this work be done, when, at what priority, and with which approvals? | `docs/governance/` | Binding process authority |
| Business/product | What does the bank want the platform to do? | `docs/au-bank-insurance-platform/` | Business/product source set |
| Current project truth | What stage/scope/gate is each workstream in now? | `docs/governance/state/CURRENT-STATE.yaml` | Current-state snapshot |
| WS-3 platform architecture | Who owns each domain truth and how do services communicate? | `docs/platform/ws3-platform/` | R0 platform architecture source set |
| Architecture decisions | Why was a consequential technical choice made? | `docs/platform/architecture-review/08-architecture-decision-log.md` | ADR source |
| R0 stakeholder architecture | What does R0 look like end to end? | `docs/architecture/R0-HLD.md` | Compiled stakeholder view; check signature status |
| R0 platform/AWS detail | What infrastructure is required and where does it run? | `docs/architecture/R0-LLD.md` | Platform-team requirements view; check signature status |
| Request/journey execution | How does a request travel through layers and controls? | `docs/journey-execution/` | Developer walkthrough; authority links inside |
| 1SB adapter engineering | How does the current 1SB integration service behave? | `docs/1sb-insurance-integration/service-ssot/` | WS-1 engineering SSOT |
| Workforce identity | How do login, sessions and business authorization work? | `docs/platform/authentication-authorization/` | Identity/auth SSOT |
| Personas / role reasoning | What perspective does Architect, Security, DBA, Product, SRE etc. apply? | `docs/context/roles/` | Context, not substitute for binding approvals |
| Global docs index | What documentation families exist and which wins on conflict? | `docs/README.md` | Master documentation index |

## Repository top-level map

| Path | Purpose |
|---|---|
| `apps/` | User-facing application projects, including the Flutter workforce app |
| `services/` | Executable backend service modules |
| `libs/` | Shared Java libraries and cross-cutting primitives |
| `docs/` | Business, architecture, governance, workstream, journey and role documentation |
| `config/` | Configuration assets used by the repository |
| `scripts/` | Governance/context/validation and engineering automation |
| `.github/` | GitHub Actions and repository automation |
| `.claude/` | Agent skills/context for repository-assisted work |
| `settings.gradle.kts` | Ground truth for executable Gradle modules currently included in the build |
| `AGENTS.md` | Agent/developer repository operating contract |

## Governance map

Start at `docs/governance/RUNBOOK.md`. Then use the numbered files by question:

| File | Question |
|---|---|
| `00-GOVERNANCE.md` | What is the overall triage/action model? |
| `01-CURRENT_STATE.md` | What does “current state” mean and how is freshness handled? |
| `02-PROJECT_SCOPE.md` | Is this actually our problem? `SC0–SC4` |
| `03-LIFECYCLE.md` | Is this the right stage? `SF0–SF4` |
| `04-STAGE_GATES.md` | What must happen before a stage passes? |
| `05-PRIORITY_MODEL.md` | Is it P1, P2, P3, P4 or P5? |
| `06-WORK_CLASSIFICATION.md` | What kind/size of work is this? |
| `07-DEPENDENCY_MODEL.md` | What blocks what and what executes first? |
| `08-BACKLOG_RULES.md` | READY, BLOCKED, PARKED, IDEAS etc. |
| `09-AI_EXECUTION_RULES.md` | How humans/agents execute work safely |
| `10-IMPLEMENTATION_PLAN_TEMPLATE.md` | How T2+ work is planned |
| `11-REVIEW_GATES.md` | T1–T4, G1–G10 and board review/sign-off |
| `12-DEFINITION_OF_READY.md` | When work may start |
| `13-DEFINITION_OF_DONE.md` | When work may be called done |
| `14-CHANGE_CONTROL.md` | When a CR is required |
| `16-DECISION_MODEL.md` | Necessity/confidence/decision discipline |
| `17-DRIFT_CONTROL.md` | What happens when implementation drifts from plan |
| `18-GOVERNANCE_METRICS.md` | Whether governance itself is healthy |

### Registers

| Register | What to look for |
|---|---|
| `registers/SUGGESTION-REGISTER.md` | Every suggestion and its triage result |
| `registers/PARKED-BACKLOG.md` | Deferred items, target stage and unpark trigger |
| `registers/DECISION-REGISTER.md` | Decisions that constrain future work |

## Business map

Use `docs/au-bank-insurance-platform/README.md` as the entry point.

Common routes:

- **What problem are we solving?** → `docs/context/business-problem-statement.md`
- **What is R0?** → `docs/au-bank-insurance-platform/requirements/R0-SCOPE.md`
- **What has the business already decided?** → working decisions + decision log
- **What is still open?** → readiness/discovery/clarification material linked by the business index

## Architecture map

Use different documents for different questions:

- **Domain ownership/invariants** → `platform/ws3-platform/01-domain-model-and-invariants.md`
- **Data/information semantics** → `platform/ws3-platform/02-information-model.md`
- **Components, waves and seams** → `platform/ws3-platform/03-solution-architecture-r0.md`
- **Security boundaries** → `platform/ws3-platform/04-security-architecture.md`
- **NFR targets** → `platform/ws3-platform/05-nfr-catalogue.md`
- **Why a major choice was made** → architecture ADR log
- **Stakeholder walkthrough** → `architecture/R0-HLD.md`
- **AWS/platform-team view** → `architecture/R0-LLD.md`

## Code map

Do not infer implementation from architecture diagrams.

For what exists **today**, check:

1. `settings.gradle.kts`
2. `services/`
3. `apps/`
4. `libs/`
5. each module's README
6. controllers/use cases/tests for actual implemented behaviour

The target architecture can legitimately describe components that are not yet implemented.

## Conflict rule

If two documents disagree, do not average them. Use `docs/README.md` to identify the authority layer, then follow the higher-authority/current source and record the inconsistency as a documentation defect if necessary.
