# Stakeholder personas

**Parent:** [`docs/context/README.md`](../README.md)
**Status:** 🟡 Non-binding context — see the [context folder rules](../README.md#what-this-folder-is)

---

## What these are

Each file captures **how one role thinks** — their domain focus, vocabulary, priorities, and
the questions they ask first. They are written as RAG/grounding context so that a discussion
(or an AI assistant) can adopt a specific viewpoint deliberately rather than blending all
perspectives into one voice.

They are **not** job descriptions, and they are **not** the authoritative stakeholder
catalogue — that is
[`knowledge-base/06-stakeholders.md`](../../au-bank-insurance-platform/knowledge-base/06-stakeholders.md).

---

## The panel

Each persona has two files: a **baseline** (how the role operates today) and an
**agentic-AI evolution** (how the same role operates at the next maturity level).

| Role | Baseline persona | Agentic-AI evolution | Domain focus |
|------|-----------------|---------------------|--------------|
| 📋 **Product Owner** | [rajal-product-owner.md](./rajal-product-owner.md) | [→ agentic evolution](./rajal-product-owner-agentic-ai-evolution.md) | Bancassurance vision, IRDAI CA0515, BRD/user stories, customer & RM journeys |
| 🏛️ **Solution Architect** | [mahesh-solution-architect.md](./mahesh-solution-architect.md) | [→ agentic evolution](./mahesh-solution-architect-agentic-ai-evolution.md) | Banking/insurance architecture, microservices, security, aggregator abstraction |
| ⚙️ **Technical Head** | [amit-technical-head.md](./amit-technical-head.md) | [→ agentic evolution](./amit-technical-head-agentic-ai-evolution.md) | Engineering leadership, AWS EKS, CI/CD, quality gates, reliability & SLAs |
| ⚖️ **Compliance Officer** | [vaishnavi-compliance-officer.md](./vaishnavi-compliance-officer.md) | *not yet written* | IRDAI CA0515 **licence holder**, consent & suitability rules, PII, audit evidence, code compliance review |

**Read the baseline first.** Each evolution file is written as a delta on top of its
baseline and assumes you know it.

> **Vaishnavi's persona differs from the other three.** Sections 1–2 are role-derived rather
> than self-reported, and §7 lists what must be confirmed with her directly. She is also the
> only persona here whose sign-off is **binding** — a compliance verdict on a regulated item
> cannot be waived ([14 §1](../../governance/14-CHANGE_CONTROL.md#1-what-needs-a-change-request)).
> Her review scope for this repository is §5 of her file.

---

## Shared source

All six personas are grounded in the same problem statement:

→ [`../business-problem-statement.md`](../business-problem-statement.md)

The three evolution files are consolidated into a single panel view in
[`../roadmaps/agentic-ai-transformation-roadmap.md`](../roadmaps/agentic-ai-transformation-roadmap.md).
