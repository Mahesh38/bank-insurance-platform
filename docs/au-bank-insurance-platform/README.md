# AU Bank — Insurance Platform (Product Reset)

**Bank:** AU Small Finance Bank (AU Bank)  
**Working name:** AU Bank Insurance Platform  
**Folder purpose:** Product Owner + Business Analysis + stakeholder alignment — **from scratch**  
**Status:** Discovery — **Working Decisions Draft v1** is the working SSOT for MVP scope (pending formal validation)

---

## Why this folder exists

We previously created research docs, engineering SSOT, and Figma screens to explore bancassurance + 1Silverbullet (1SB).  
Those artefacts were useful for learning. They are **not** automatically the product contract for AU Bank.

This folder is the **clean restart**:

1. Re-state the business problem for **AU Bank**
2. Align stakeholders on **who we serve, what journeys, what success looks like**
3. Re-validate process flow against Figma + uploaded baseline docs
4. Only then freeze scope, epics, and handoff to architecture/engineering

> Engineering work under `docs/1sb-insurance-integration/` remains **prior research / technical spike**.  
> It may inform options. It does **not** override decisions made here until Product explicitly adopts them.

---

## How we work in this reset

| Role | Responsibility in this folder |
|------|-------------------------------|
| Platform Product Owner | Outcomes, scope, prioritisation, go/no-go |
| Business Analysis | Journeys, use cases, rules, open questions, AC drafts |
| Bank business / Bancassurance | Product LOBs, RM model, compliance constraints |
| Channel / Digital | RM app + customer touchpoints, UX from Figma |
| Compliance / Risk / Infosec | Consent, audit, PII, attribution |
| Architecture (advisory) | Feasibility options — after product questions are clear |
| Delivery / Eng (advisory) | Effort signals — not scope owners in discovery |

**Rule:** Closed discovery answers live in [Working Decisions](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md). Remaining Open Validation Items and open rows in [03-discovery-backlog.md](./03-discovery-backlog.md) still block formal freeze / hard-coded compliance.

---

## Document map (start here)

| # | Doc | Purpose |
|---|-----|---------|
| **7** | **[07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)** | **Working SSOT — MVP LOB, journeys, ETB, sale, insurers, 1SB, open validation** |
| — | [DECISION-LOG.md](./DECISION-LOG.md) | Decision IDs mapped from Working Decisions |
| 0 | [00-project-charter.md](./00-project-charter.md) | Why AU Bank, problem, intent, boundaries |
| 1 | [01-stakeholder-working-session.md](./01-stakeholder-working-session.md) | Kickoff agenda, roles, decision log template |
| 2 | [02-product-vision-and-outcomes.md](./02-product-vision-and-outcomes.md) | Vision, outcomes, non-goals (draft → lock) |
| 3 | [03-discovery-backlog.md](./03-discovery-backlog.md) | Must-answer questions (many P0 now Answered) |
| 4 | [04-process-and-journey-canvas.md](./04-process-and-journey-canvas.md) | End-to-end process canvas (to validate with Figma) |
| 5 | [05-figma-and-artefact-intake.md](./05-figma-and-artefact-intake.md) | Figma link + intake log for uploaded docs |
| 6 | [06-po-opening-position.md](./06-po-opening-position.md) | PO kickoff stance for the stakeholder room |
| **KB** | **[knowledge-base/](./knowledge-base/README.md)** | **Application knowledge base (synthesized from uploaded Volumes/Phases)** |
| **PO drive** | **[po-drive/](./po-drive/00-PO-PROJECT-VIEW.md)** | **Project view · SWOT · Gap register · Programme TODO** |
| **Requirements** | **[requirements/](./requirements/README.md)** | **R0 scope · BRD overview · PRD · P0 BRD with AC** |
| — | [references/](./references/README.md) | Pointers to prior research (non-binding) |

---

## Current inputs

| Input | Status |
|-------|--------|
| Figma prototype (client review) | Linked — see [05](./05-figma-and-artefact-intake.md); **screen inventory pending** (login-gated; needs walkthrough / exports) |
| Baseline docs (Volumes 01–06, Phases 1–5) | **Ingested** → [knowledge-base/](./knowledge-base/README.md) |
| Prior repo research (`docs/1sb-insurance-integration/`) | Available as optional reference only |

---

## Next action for stakeholders

1. **Formally validate** [Working Decisions Draft v1](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) (sponsor sign-off).  
2. Close **Open Validation Items** (consent/IRDAI, agent identity, retention, sponsor name, kiosk).  
3. Align BRD chapters / PRD / UX to Life LOB + three journeys + Group A/B insurers.  
4. Treat Figma as **reference only**; inventory MVP vs concept ([05](./05-figma-and-artefact-intake.md)).  
5. Engineering: configurable compliance controls; keep core capabilities aggregator-agnostic.
