# Product vision & outcomes — AU Bank Insurance Platform

**Owner:** Platform Product Owner  
**Status:** Draft — aligned to [Working Decisions v1](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md); lock after formal sponsor validation  
**Audience:** All stakeholders

---

## Vision (draft)

> AU Bank’s RMs and existing customers buy the right **Life** insurance through **assisted, self-service, or hybrid** journeys that feel like **AU Bank** — with mandatory suitability and consent — while the bank stays free to change insurance connectivity behind the scenes.

---

## Product pillars

| Pillar | Meaning for AU Bank |
|--------|---------------------|
| **Bank-owned journey** | Stages, suitability, disclosures, catalogue, and UX copy are bank product decisions |
| **Three journeys Day 1** | RM-assisted, customer self-service, and hybrid with seamless mode switching |
| **Multi-insurer choice** | Group A in-platform quotes; Group B catalogue + redirect |
| **Compliance by design** | Consent, agent/distributor attribution, and audit are release gates |
| **Replaceable connectivity** | Apps talk to AU Bank insurance APIs — not aggregator wire formats |

---

## Outcomes (draft OKRs-style)

### Outcome A — Sell with confidence (RM)

- RM can identify customer, capture need, obtain quotes, complete proposal, and track status **without leaving the bank journey**.
- Time-to-first-quote and time-to-proposal are measured after pilot.

### Outcome B — Customer trust

- Customer sees AU Bank branding and clear disclosures.
- Sensitive steps (OTP, payment, e-sign if any) are explicit and attributable.

### Outcome C — Regulatory readiness

- Every proposal submit has agent attribution.
- Distributor identity cannot be spoofed by a caller.
- Audit can reconstruct the sale path for a sample of policies.

### Outcome D — Platform leverage

- Second LOB reuses the same journey skeleton and bank APIs.
- Aggregator change does not force RM app rewrite.

---

## Non-goals (draft)

| Non-goal | Why called out |
|----------|----------------|
| Rebuild CIF / CRM | Bank already owns customer |
| Day-1 claims & renewals suite | Different product; sequence later |
| 1SB UI as long-term front door | Brand + replaceability |
| Every LOB at once | Focus; learn from first LOB |
| Perfect insurer coverage on day 1 | Panel expands with bancassurance |

---

## Release intent (hypothesis — not roadmap)

| Release | Intent | “Done” means (hypothesis) |
|---------|--------|---------------------------|
| **R0 — Pilot** | One LOB, limited insurers, controlled RM cohort | Happy path to payment or policy (per D-007) |
| **R1 — Scale** | More RMs, hardening, ops visibility | Stable SLAs + compliance pack |
| **R2 — Expand** | Next LOB + richer requirements/docs | Repeatable LOB playbook |

*Lock D-001 and D-007 before publishing any external roadmap.*

---

## Principles for BA write-ups

1. Write in **AU Bank language** (customer, RM, journey, policy) — not vendor field names.  
2. Every MVP step needs: actor, trigger, data in/out, compliance note, failure path.  
3. If Figma and business disagree, **business wins**; Figma is updated.  
4. Prior engineering decisions are **options**, not defaults, until adopted here.
