# Where to read next

> **Teaching pack** · `SUG-20261005-cfp` · not a source of truth.
>
> **Confluence parent:** AU Bank Insurance Platform — Start here  
> **This page:** child #10

When this pack is not enough, open the document that **owns** the fact. Do not keep copying from here.

## By question

| You need | Open |
|----------|------|
| Problem, licence, as-is vs target | [`docs/context/business-problem-statement.md`](../../../context/business-problem-statement.md) |
| Product vision | [`docs/au-bank-insurance-platform/02-product-vision-and-outcomes.md`](../../../au-bank-insurance-platform/02-product-vision-and-outcomes.md) |
| Behaviour (screens, fields, rules) | Detailed BRD pack [`docs/au-bank-insurance-platform/requirements/brd-detailed/`](../../../au-bank-insurance-platform/requirements/brd-detailed/) |
| R0 HLD in prose | [`docs/architecture/R0-HLD.md`](../../../architecture/R0-HLD.md) |
| AWS / VPC / what to provision | [`docs/architecture/R0-LLD.md`](../../../architecture/R0-LLD.md) |
| Domain invariants, saga, actors | [`docs/platform/ws3-platform/01-domain-model-and-invariants.md`](../../../platform/ws3-platform/01-domain-model-and-invariants.md) |
| Hop-by-hop use case | [`docs/journey-execution/README.md`](../../../journey-execution/README.md) then one `flows/UC-nn-*.md` |
| Lead sequences / APIs | [`docs/platform/ws3-platform/11-lead-module-sequences.md`](../../../platform/ws3-platform/11-lead-module-sequences.md) |
| Workforce auth | [`docs/platform/authentication-authorization/README.md`](../../../platform/authentication-authorization/README.md) |
| 1SB adapter build | [`docs/1sb-insurance-integration/service-ssot/README.md`](../../../1sb-insurance-integration/service-ssot/README.md) |
| Current stage and open gates | [`docs/context/BOOT.md`](../../../context/BOOT.md) |
| May I start this work? | [`docs/governance/RUNBOOK.md`](../../../governance/RUNBOOK.md) — triage first |
| Frontend look | [`docs/figma/README.md`](../../../figma/README.md) — not behaviour SSOT |

## Which document wins (content)

1. Detailed BRD pack  
2. Working decisions  
3. Decision log (`D-xxx` / `DOC-xxx`)  
4. Workforce auth SSOT  
5. 1SB module SSOT (adapter only)  
6. Architecture review (recommendation until T4)  
7. Knowledge base  
8. Context / this pack — never binding  

Process questions (may this start, which gate) are settled by `docs/governance/`. Diagrams lose to documents.

## Official pictures to attach on the Architecture page

| File | Question |
|------|----------|
| `docs/hdl.svg` | Target state by release |
| `docs/architecture/r0-reference-architecture.svg` | R0 executable architecture |
| `docs/architecture/r0-lld.svg` | Trust zones, what not to provision |

Human T4 Architecture / Security / Risk sign-off on those packs is **outstanding**. Do not treat an AI-drafted HLD as a production authorisation.

## Back to the start

[AU Bank Insurance Platform — Start here](./00-home.md)
