# Templates

Copy these; do not edit them in place.

| Template | Use when | Copy to |
|---|---|---|
| [`EPIC.md`](./EPIC.md) | Adding an epic to a stage | The stage file, plus Jira |
| [`STORY.md`](./STORY.md) | Writing a story that needs more detail than a backlog row | Jira, or a linked document for complex stories |
| [`VALIDATION-TEST.md`](./VALIDATION-TEST.md) | Defining or recording a stage validation test | `stages/validation/` |
| [`GATE-SIGNOFF.md`](./GATE-SIGNOFF.md) | Every gate transition | `stages/signoffs/S<xx>-GATE-SIGNOFF-<date>.md` |
| [`ORR.md`](./ORR.md) | S14 operational readiness review | `stages/signoffs/` |

## Existing AIGEM templates still apply

This set does not replace [`docs/governance/templates/`](../../governance/templates/). Those
govern triage and review; these govern stage execution. Use both.

| AIGEM template | For |
|---|---|
| `TRIAGE-RECORD.md` | Classifying any incoming item — **always first** |
| `WORK-ITEM.md` | An admitted work item |
| `IMPLEMENTATION-PLAN.md` | A plan going to the seven boards |
| `REVIEW-VERDICT.md` | A board verdict on a plan |
| `EPIC.md` | AIGEM's epic form |
| `ADR.md` | An architecture decision |

Where AIGEM has a template for the same artefact, **AIGEM's wins** — these add stage context, they
do not supersede the governance forms.

## A note on filling them in

Every template here has sections that are easy to leave empty and expensive to leave empty: the
`evidence` fields, the `out of scope` section, the `on failure` block, the notes.

Those are the sections a reader in eighteen months — or a regulator — actually needs. The tables
above them mostly restate what the diff already shows.
