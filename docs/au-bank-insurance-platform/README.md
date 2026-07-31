# AU Bank — Insurance Platform (Product Reset)

**Bank:** AU Small Finance Bank (AU Bank)  
**Working name:** AU Bank Insurance Platform  
**Folder purpose:** Product Owner + Business Analysis + stakeholder alignment — **from scratch**  
**Status:** Discovery kickoff (not a delivery SSOT yet)

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

**Rule:** No technical solution is “final” until the product questions in [03-discovery-backlog.md](./03-discovery-backlog.md) are answered or explicitly deferred.

---

## Document map (start here)

| # | Doc | Purpose |
|---|-----|---------|
| 0 | [00-project-charter.md](./00-project-charter.md) | Why AU Bank, problem, intent, boundaries |
| 1 | [01-stakeholder-working-session.md](./01-stakeholder-working-session.md) | Kickoff agenda, roles, decision log template |
| 2 | [02-product-vision-and-outcomes.md](./02-product-vision-and-outcomes.md) | Vision, outcomes, non-goals (draft → lock) |
| 3 | [03-discovery-backlog.md](./03-discovery-backlog.md) | Must-answer questions before build lock |
| 4 | [04-process-and-journey-canvas.md](./04-process-and-journey-canvas.md) | End-to-end process canvas (to validate with Figma) |
| 5 | [05-figma-and-artefact-intake.md](./05-figma-and-artefact-intake.md) | Figma link + intake log for uploaded docs |
| — | [references/](./references/README.md) | Pointers to prior research (non-binding) |

---

## Current inputs

| Input | Status |
|-------|--------|
| Figma prototype (client review) | Linked — see [05](./05-figma-and-artefact-intake.md); **screen inventory pending** (login-gated; needs walkthrough / exports) |
| Baseline docs from earlier work | **Awaiting upload** from Product |
| Prior repo research (`docs/1sb-insurance-integration/`) | Available as optional reference only |

---

## Next action for stakeholders

1. Upload / attach all earlier basic docs into the intake log ([05](./05-figma-and-artefact-intake.md)).
2. Walk the Figma prototype with PO + BA (or share PNG/PDF exports per screen).
3. Run Session 1 from [01](./01-stakeholder-working-session.md) and fill the decision log.
4. Close P0 questions in [03](./03-discovery-backlog.md) before any scope freeze.
