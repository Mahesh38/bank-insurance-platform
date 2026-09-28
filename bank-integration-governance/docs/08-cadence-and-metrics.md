# 08 — Cadence and metrics

## 1. Cadence — short, few, skippable when nothing changed

| Event | When | Length | Who | Output |
|---|---|---|---|---|
| **Flow digest** | Daily, async | 2 min to read | Flow steward (AI) posts; lane leads read | Aged, blocked, over-WIP, past-SLA items — or nothing |
| **Decision clinic** | Twice weekly | 15 min | Decision owners with items due; delivery lead chairs | Decisions taken in `needed_by` order; missed ones escalated |
| **Replenishment** | Weekly | 30 min | PO, lane leads | Next reordered; Now pulled within WIP |
| **Outcome review / demo** | Fortnightly | 45 min | Everyone incl. bank stakeholders when useful | Working software shown per lane; outcomes re-scored |
| **Governance retro** | Monthly | 45 min | Delivery lead (chair), PO, architecture, anyone | Rule changes as PRs to this repo ([09](./09-changing-the-governance.md)) |
| **Roadmap** | Quarterly | 60 min | PO + sponsor | Outcomes for the next quarter; releases re-committed |

**Rule CAD-1.** A ceremony with no agenda items is cancelled automatically. Freshness is
proven by the tracker and git history, not by meetings or by editing a date.

AIGEM ceremonies map: Governance Sync → replenishment + decision clinic; Gate Review (90 min) →
checkpoint query (no meeting); Register Hygiene → handled by `Later` review dates and the retro.

## 2. Metrics

Measured from the tracker and git; no manual register counting.

### Flow (primary)

| Metric | Definition | Target / alarm |
|---|---|---|
| **Lead time** | Card created → merged/promoted to dev | Trend down; alarm if p85 > 15 working days |
| **Throughput** | Cards finished per week per lane | Stable or rising |
| **WIP age** | Days a `Now` card has been open | Alarm > 10 working days |
| **Review wait** | PR ready → first human review | Alarm p85 > 2 working days |
| **Decision latency** | Decision ready → decided (Type-1) | Alarm if > 20% miss SLA in a month |
| **Blocked time %** | Share of card-time spent `blocked` | Alarm > 25%; every blocked card names its §4 row |
| **External dependency age** | Days past chase date | Any > 0 appears in the digest |

### Quality and safety (guardrails on speed)

| Metric | Target |
|---|---|
| Change failure rate (promotions rolled back or hot-fixed) | < 15% |
| Escaped defects on guardrail/control paths | 0 — any occurrence is an Expedite + retro item |
| Guardrails enforced by `code`/`ci` vs `review` | Ratio rises every quarter |
| R2 changes released without the named approval | 0, checked by PR audit |

### Governance cost (keeps the model honest)

| Metric | Alarm |
|---|---|
| **Bypass rate** — work done outside the flow by human override | > 10% in a month → the rule is wrong; fix the rule |
| Governance PRs ÷ all PRs | > 15% for two consecutive months |
| Size of this repository's governance text | > 200 KB (enforced by `validate.py`) |
| Default agent context load | > 25 KB (enforced by `validate.py`) |
| Time from input to first outcome (card/decline/fold-in) | p85 > 1 working day |

AIGEM's headline metric, "gate criteria closed per week", is replaced by **throughput with lead
time and decision latency beside it**: gates no longer exist to close, and one number without its
counterweights rewards gaming.
