# M2.5 — Branch triage (full history)

> Re-run on the **unshallowed** clone. The first run used the shallow clone and its ancestry was unreliable.

**Remote branches (excl. `main`):** 81  
**Merged into `main` by ancestry:** 52  
**Not merged by ancestry:** 29 — of which **4** have no unapplied patches (squash-merged; content is in `main`) and **25** carry genuinely unapplied work

## Carry unapplied work — decision required before archiving

| Branch | Last commit | Author | Ahead | Unapplied patches |
|---|---|---|---|---|
| `cursor/comp-004-agent-distributor-attribution-c259` | 2026-07-30 | Cursor Agent | 16 | **16** |
| `cursor/comp-003-raw-payload-encryption-c259` | 2026-07-31 | Mahesh38 | 17 | **16** |
| `cursor/func-009-application-status-c259` | 2026-07-31 | Mahesh38 | 13 | **12** |
| `cursor/func-007-payment-url-c259` | 2026-07-31 | Mahesh38 | 10 | **9** |
| `claude/cr-001-completion-8e1v5z` | 2026-08-14 | Claude | 9 | **9** |
| `claude/consent-suitability-compliance-doc-lsq02z` | 2026-08-24 | Claude | 9 | **8** |
| `claude/pr-55-architectural-review-tefr98` | 2026-08-17 | Claude | 4 | **4** |
| `claude/github-gitlab-migration-91h9ay` | 2026-08-29 | Claude | 4 | **4** |
| `cursor/func-006-get-proposal-job-c259` | 2026-07-31 | Mahesh38 | 4 | **3** |
| `claude/ajal-process-realignment-843yjm` | 2026-08-12 | Claude | 3 | **3** |
| `agent/stakeholder-lld-baseline` | 2026-08-12 | Mahesh Narkar | 2 | **2** |
| `claude/criteria-gate-closure-el8r8i` | 2026-08-15 | Claude | 2 | **2** |
| `claude/aws-deployment-uat-phases-42cen8` | 2026-08-18 | Claude | 2 | **2** |
| `cursor/r0-hld-diagram-bd86` | 2026-08-18 | Cursor Agent | 2 | **2** |
| `agent/architecture-pre-approval-pack` | 2026-08-25 | Mahesh38 | 3 | **2** |
| `claude/sv-integration-deployment-check-2m78bi` | 2026-08-04 | Claude | 1 | **1** |
| `claude/persona-system-refactoring-uu8a30` | 2026-08-10 | Claude | 1 | **1** |
| `claude/microservices-migration-docs-governance-av8pal` | 2026-08-11 | Claude | 1 | **1** |
| `claude/phased-platform-uat-architecture-dhg3ok` | 2026-08-11 | Claude | 1 | **1** |
| `claude/project-timeline-sprint-planning-2h8bip` | 2026-08-17 | Claude | 1 | **1** |
| `claude/bank-insurance-arch-review-tkmpky` | 2026-08-18 | Claude | 1 | **1** |
| `cursor/r0-hld-lld-stakeholder-pack-96ad` | 2026-08-20 | Cursor Agent | 1 | **1** |
| `claude/governance-triage-process-docs-3de0fn` | 2026-08-21 | Claude | 1 | **1** |
| `cursor/park-polyrepo-split-2a31` | 2026-08-27 | Cursor Agent | 1 | **1** |
| `claude/figma-design-extraction-eid84x` | 2026-08-28 | Claude | 1 | **1** |

## Content already in `main` — archive as bundles (56)

52 merged by ancestry, 4 squash-merged with zero unapplied patches. Bundle and retain; no migration value.

