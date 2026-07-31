# Platform PO opening position — restarting from zero

**Voice:** Platform Product Owner (with BA facilitation)  
**Date:** Discovery restart  
**Bank:** AU Bank

---

## What I am asking the room to agree

We are **not** continuing the previous engineering narrative as the product truth.  
We are building an **AU Bank Insurance Platform** for bancassurance — starting with shared understanding of customers, journeys, and release outcomes.

Prior 1SB research and code spikes are valuable. They answer “can we connect?”  
They do **not** yet answer “what must AU Bank ship for RMs and customers?”

---

## Stakeholder positions I want on the table

### Bancassurance / Product

- Which insurance LOBs does AU Bank actually want to push in the next 6–12 months?  
- Which insurers are commercially real for the pilot?  
- Is the first win “more policies” or “controlled compliant pilot”?

### Digital / Channel / UX (Figma owners)

- Walk us through the Figma as the intended RM (and customer) experience.  
- Call out which screens are aspirational.  
- Tell us where the journey breaks today (data, ops, compliance).

### Compliance / Risk / Infosec

- Non-negotiables before any customer sees a quote: consent, suitability, attribution, logging.  
- What evidence must exist after a sale for audit.

### Branch / RM ops

- Who starts the journey? Who finishes payment? Who chases documents?  
- What would make RMs refuse to use the tool?

### Architecture / Engineering (advisory only for now)

- Listen first. Offer options after Sessions 1–2.  
- Do not defend prior service boundaries until product scope is frozen.

---

## Working hypotheses (challenge these)

1. **AU Bank brand** owns the UX; aggregator is invisible to end users.  
2. **RM-assisted** is the default operating model for MVP.  
3. **Existing customers** only for R0.  
4. **One LOB first** (Term Life is a common candidate — not approved).  
5. **1SB** is the current connectivity choice — bank APIs must survive its replacement.  
6. **Payment + status visibility** are likely in the first “real” release; full UW/docs inbox may be phased.

---

## What “done” looks like for this discovery phase

Not code. Not backlog tickets.

1. Charter approved for AU Bank.  
2. Figma + uploads reconciled into one journey canvas.  
3. P0 discovery questions answered or deferred with dates.  
4. Release 0 scope written in product language.  
5. Architecture invited to an options workshop with **clear constraints**.

---

## Immediate next steps

| # | Action | Who |
|---|--------|-----|
| 1 | Upload earlier basic docs into `artefacts/uploads/` and log them | You / PO |
| 2 | Figma walkthrough or frame exports | Digital + PO + BA |
| 3 | Schedule Session 1 ([01](./01-stakeholder-working-session.md)) | PO |
| 4 | Start filling decision log D-001…D-008 | All |

Until then, this folder is the **only** product SSOT for the reset.  
Engineering SSOT under `docs/1sb-insurance-integration/` stays parked as research.
