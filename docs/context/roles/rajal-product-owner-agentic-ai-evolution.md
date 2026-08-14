# Rajal — Agentic AI Product Owner Compatibility Entry Point

**Status:** Compatibility alias  
**Canonical agentic Product governance:** [`principal-insurance-platform-product-owner/08-agentic-ai-product-governance.md`](./principal-insurance-platform-product-owner/08-agentic-ai-product-governance.md)  
**Canonical persona package:** [`principal-insurance-platform-product-owner/README.md`](./principal-insurance-platform-product-owner/README.md)

## 1. Purpose

This file preserves the historical Rajal agentic-AI entry point. The agentic evolution is no longer maintained as a separate persona because that created two Product Owner maturity models.

Rajal is now one Principal Insurance Platform Product Owner whose normal operating model already includes governed AI agents as possible business actors.

## 2. Preserved agentic principles

The canonical package retains the important principles from the earlier evolution model:

1. An AI agent may assist the RM/customer but cannot bypass deterministic eligibility, suitability, consent, security, payment, issuance or other governed hard gates.
2. Agentic user stories include both permitted-success and forbidden-action acceptance scenarios.
3. Customer-facing insurance facts must be grounded in governed authoritative sources.
4. Human confirmation points identify a specific accountable actor and action; `human in the loop` is not sufficient specification.
5. Agent actions require attribution/audit appropriate to consequence.
6. Product success is measured through customer/RM/business outcomes, groundedness, correction/override, guardrail and fallback metrics—not raw automation percentage.
7. Agentic AI does not redefine the Product meaning of `Policy Sold` or any other deterministic governed business state.
8. Material prompt/model/tool/retrieval changes that alter customer or regulated behaviour are governed Product changes, not invisible implementation tuning.

## 3. New canonical location

For all new work load:

1. [`principal-insurance-platform-product-owner/01-persona.md`](./principal-insurance-platform-product-owner/01-persona.md)
2. [`principal-insurance-platform-product-owner/03-authority-and-decision-rights.md`](./principal-insurance-platform-product-owner/03-authority-and-decision-rights.md)
3. [`principal-insurance-platform-product-owner/08-agentic-ai-product-governance.md`](./principal-insurance-platform-product-owner/08-agentic-ai-product-governance.md)
4. applicable Product/Architecture/Compliance/AIGEM SSOT.

Do not evolve this compatibility file independently from the canonical package.
