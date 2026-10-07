# Onboarding pack — AU Bank Insurance Platform

**Work item:** `SUG-20261005-cfp`  
**Authority:** non-binding communication. Teaching pages for Confluence.  
**Audience:** anyone joining the programme (engineer, BA, architect, RM, operations, vendor).

This folder is the **paste-ready Confluence space** for "what are we building, why, and how does a sale actually run?". Each file in `pages/` is one Confluence page. Each diagram in `diagrams/` is an attachment (PNG for Confluence, SVG as the source render).

## Main page vs sub-pages

**Create this page first — it is the only parent:**

| Confluence title | File | Role |
|------------------|------|------|
| **AU Bank Insurance Platform — Start here** | [`pages/00-home.md`](./pages/00-home.md) | **MAIN PAGE** |

Then create these **as children of that page**, in this order:

| # | Confluence title | File |
|---|------------------|------|
| 1 | Why we are building this | [`pages/01-why.md`](./pages/01-why.md) |
| 2 | What we are building now (R0) | [`pages/02-what.md`](./pages/02-what.md) |
| 3 | Who uses it | [`pages/03-who.md`](./pages/03-who.md) |
| 4 | Use cases | [`pages/04-use-cases.md`](./pages/04-use-cases.md) |
| 5 | The assisted Life sale journey | [`pages/05-sale-journey.md`](./pages/05-sale-journey.md) |
| 6 | Architecture | [`pages/06-architecture.md`](./pages/06-architecture.md) |
| 7 | Sequence diagrams | [`pages/07-sequences.md`](./pages/07-sequences.md) |
| 8 | Rules you must never break | [`pages/08-hard-rules.md`](./pages/08-hard-rules.md) |
| 9 | What lives in this repository | [`pages/09-this-repo.md`](./pages/09-this-repo.md) |
| 10 | Where to read next | [`pages/10-read-next.md`](./pages/10-read-next.md) |

Then nest **grandchildren** under Architecture and Sequence diagrams. Do not leave them as siblings of the home page.

| Parent | Child title | File |
|--------|-------------|------|
| Architecture | How a request travels | [`pages/06a-hops.md`](./pages/06a-hops.md) |
| Architecture | Bounded contexts and services | [`pages/06b-contexts.md`](./pages/06b-contexts.md) |
| Sequence diagrams | Sequence — RM login | [`pages/07a-login.md`](./pages/07a-login.md) |
| Sequence diagrams | Sequence — Lead create and assign | [`pages/07b-lead.md`](./pages/07b-lead.md) |
| Sequence diagrams | Sequence — Quote via 1SB | [`pages/07c-quote.md`](./pages/07c-quote.md) |
| Sequence diagrams | Sequence — Payment and issuance | [`pages/07d-payment.md`](./pages/07d-payment.md) |

Picture of the tree: [`diagrams/00-page-tree.png`](./diagrams/00-page-tree.png)

Step-by-step Confluence instructions: [`HOW-TO-PUBLISH.md`](./HOW-TO-PUBLISH.md)  
Exact parent/child table: [`PAGE-TREE.md`](./PAGE-TREE.md)

## How a new person should read it

1. Home (one screen: problem, R0 sentence, current stage)
2. Why, then What
3. Who, then Use cases, then the sale journey
4. Architecture + hops
5. The four sequence pages
6. Hard rules — pin this
7. This repository and Where to read next, when they need the SSOT

## Regenerating pictures

```bash
python3 scripts/confluence/render_onboarding_diagrams.py
```

Writes SVG + PNG under `diagrams/`. Edit the script, not the SVG.

## What this pack is not

It is not a BRD, not an HLD, not an ADR, and not permission to start work. Cited sources remain the owners of the facts.
