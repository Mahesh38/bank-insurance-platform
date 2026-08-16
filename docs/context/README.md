# Context — programme background & AI/RAG source material

**Parent:** [`docs/README.md`](../README.md)
**Status:** 🟡 **Non-binding.** Nothing in this folder overrides a decision made in the
business SSOT or an engineering SSOT.

---

## What this folder is

These are **context documents**: background narrative, stakeholder personas, and
forward-looking roadmaps. They exist to give a reader — human or AI assistant — enough
grounding to reason about the programme without having to read every requirement document
first.

They are deliberately kept **separate from the delivery documentation** because they serve a
different purpose:

| | Context documents (this folder) | Delivery documentation |
|---|---|---|
| **Purpose** | Explain and orient | Specify and commit |
| **Authority** | None — descriptive | Binding — prescriptive |
| **Changes when** | Understanding improves | A decision is made |
| **Lives in** | `docs/context/` | `docs/au-bank-insurance-platform/`, `docs/1sb-insurance-integration/`, `docs/platform/` |

**Use them for:** onboarding, RAG/agent grounding, framing a discussion, understanding
*why* a constraint exists.

**Do not use them for:** scope, acceptance criteria, compliance obligations, or API
contracts. Those live in the SSOTs — see [which document wins](../README.md#which-document-wins).

A persona may define a review method or reasoning posture, but it does not make a statement legally binding merely by saying so. In particular, the Shailja S compliance persona points reviewers to authoritative regulatory and policy evidence; AIGEM's Risk & Compliance board supplies the formal repository gate.

---

## Contents

| Document | Purpose |
|----------|---------|
| **[business-problem-statement.md](./business-problem-statement.md)** | **Consolidated business + architecture problem statement.** The single richest orientation document — synthesized from the business SSOT and the architecture review. Start here. |
| [roles/](./roles/README.md) | Canonical stakeholder/reasoning personas, including the [Principal Insurance Platform Business Analyst / R11](./roles/principal-insurance-platform-business-analyst/README.md), with explicit cross-persona authority boundaries |
| [roadmaps/](./roadmaps/README.md) | Forward-looking transformation plans (current-state alignment + agentic-AI maturity overlay) |

---

## How these relate to the binding documents

```text
                      docs/context/  (this folder — descriptive)
                             │
              ┌──────────────┴───────────────┐
              │  synthesized FROM            │  grounds reasoning ABOUT
              ▼                              ▼
  au-bank-insurance-platform/     platform/architecture-review/
  (business SSOT — binding)       (architecture recommendation)
```

The context documents are **downstream** of the SSOTs: they summarise and interpret them.
When an SSOT changes, the context documents may lag. **On conflict, the SSOT is correct.**

---

## Related

- [`roles/shailja-s-compliance-risk-head/README.md`](./roles/shailja-s-compliance-risk-head/README.md) — Shailja S compliance/risk persona package used to ground AIGEM Board 6 reviews
- [`roles/principal-insurance-platform-business-analyst/README.md`](./roles/principal-insurance-platform-business-analyst/README.md) — end-to-end bancassurance Business Analyst reasoning package for existing R11 / Product delegate
- [`../governance/11-REVIEW_GATES.md`](../governance/11-REVIEW_GATES.md) — binding multi-agent review gate; Board 6 is Risk & Compliance
- [`../au-bank-insurance-platform/knowledge-base/06-stakeholders.md`](../au-bank-insurance-platform/knowledge-base/06-stakeholders.md) — the *authoritative* stakeholder catalogue (these personas are an interpretive layer on top of it)
- [`../au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md`](../au-bank-insurance-platform/po-drive/03-PROGRAMME-TODO.md) — the *authoritative* programme plan (the roadmaps here are exploratory, not a substitute)
