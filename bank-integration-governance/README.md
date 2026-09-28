# Bank Integration Governance

**FLOW governance for the Bank Integration Service programme** — the successor to AIGEM 1.5.

Status: **v2.0-draft** · proposed 2026-09-28 · replaces nothing until the human decisions in
[`migration/README.md` §2](./migration/README.md#2-decisions-only-humans-can-make) are taken.

---

## Why this repository exists

AIGEM was built while requirements, integration topology and even the repository boundary were
still unsettled. It was designed to stop AI agents from building the wrong thing at the wrong time,
and it did that — by serialising almost everything behind lifecycle stages, a 10-step triage
pipeline, seven review boards and ten persona packages. The measured result
([`docs/00-diagnosis.md`](./docs/00-diagnosis.md)):

- a gate with **10 of 10 criteria met** still waiting on a stage signature;
- **23 of 69** triaged suggestions admitted through `ADMIT-BYPASS` — people routed around the process a third of the time;
- **21 architecture and data decisions** sitting in `Proposed` or `AI-DRAFTED`, against **one** accepted ADR;
- persona packages of **up to 316 KB**, and **5.7 MB of Markdown in 605 files** that agents cannot afford to read;
- four overlapping lifecycle models (L0–L10, S00–S15, Phase 0–6, R0–R2) plus a fifth delivery-state model (D0–D6).

The rules were individually sound. Together they made change expensive at exactly the moment
requirements were changing fastest.

## What replaces it — the model on one screen

```text
            GUARDRAILS  (few, automated, non-negotiable)   → docs/02-guardrails.md
                 │   everything not forbidden is allowed to START
                 ▼
 INTAKE (90 s) ──► BOARD (Now / Next / Later, per capability lane, WIP on human attention)
                 │           │
                 │           ├─ contracts & stubs break dependencies → parallel by default
                 │           └─ decisions have owners, SLAs and a "proceed-while-pending" default
                 ▼
  CHANGE (PR) ──► RISK LABEL ──► reviewers chosen by risk, not by stage   → docs/06
                 ▼
  RELEASE CHECKPOINTS (dev → bank-UAT → prod)  — evidence is a query, not a ceremony
                 ▼
  LEARN: flow metrics + monthly governance retro — the rules change when the data says so
```

Five principles ([`docs/01-operating-model.md`](./docs/01-operating-model.md)):

1. **Guardrails, not gates.** A small set of invariants is enforced by code and CI. Everything else is flow.
2. **Gate the release, never the start.** Building behind a stub, flag or synthetic data is always allowed; exposing real customers, real money or real PII is what needs evidence and sign-off.
3. **Parallel by default.** A dependency is a contract to publish, not a queue to join.
4. **Decide at the edge, escalate by exception.** Reversible decisions are made by the doer after advice; only irreversible or control-changing decisions go to named humans — with a clock.
5. **Governance is a product.** It has users, metrics and a retro. A rule that is bypassed often is a wrong rule.

## Map

| Folder | What is in it | Read when |
|---|---|---|
| [`AGENTS.md`](./AGENTS.md) | The only default read for an AI agent (≤ 4 KB) | Every session |
| [`docs/`](./docs/) | The operating model, ten short documents | You need a rule |
| [`personas/`](./personas/README.md) | Role lenses (≤ 2.5 KB each), the human roster, one decision-rights table | You act as, or need, a role |
| [`context/`](./context/README.md) | Context architecture: tiers, budgets, path-based routing, capability packs | Before touching a capability |
| [`state/`](./state/) | Guardrails, open decisions, dependencies, lanes and outcomes as YAML | You need a live fact |
| [`templates/`](./templates/) | Intake card, decision record, release checkpoint, retro | You create one of those |
| [`migration/`](./migration/README.md) | AIGEM → FLOW plan, concept map, file-by-file disposition, carry-over | You are running the cutover |
| [`.claude/skills/`](./.claude/skills/) | `flow-intake`, `load-context`, `advise` | Agents use them automatically |
| [`scripts/validate.py`](./scripts/validate.py) | Structure, budgets, routing, freshness **warnings** | CI and locally |

Total size of this repository's governance text is budgeted at **< 200 KB** and checked by
`validate.py`. If the rules need more words than that, the rules are wrong.

## Scope

Governs the **Bank Integration Service** programme as it exists in
`mahesh38/bank-insurance-platform`: the 1SB integration adapter and Integration Hub (was WS-1),
workforce identity (was WS-2), and the AU Bank insurance distribution platform that consumes
them (was WS-3). Product behaviour, architecture content and code stay in the product
repository; this repository holds **how work flows, who decides, and what agents load**.

## Extracting this into its own repository

This tree was authored inside the product repository because the session could not create a new
GitHub repository. It is self-contained — no link points outside it except deliberate references
to source documents in the product repo. To move it:

```bash
# from the product repository root
git subtree split --prefix=bank-integration-governance -b bank-integration-governance-main
git push git@github.com:<owner>/bank-integration-governance.git bank-integration-governance-main:main
```
