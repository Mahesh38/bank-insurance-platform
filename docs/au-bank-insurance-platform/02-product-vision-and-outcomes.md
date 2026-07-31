# Product vision & outcomes — AU Bank Insurance Platform

**Owner:** Platform Product Owner  
**Status:** Draft — lock after Session 1–3  
**Audience:** All stakeholders

---

## Vision (draft)

> AU Bank’s Relationship Managers sell the right insurance to the right existing customer, in a guided, compliant journey that feels like **AU Bank** — while the bank stays free to change the insurance connectivity behind the scenes.

---

## Product pillars

| Pillar | Meaning for AU Bank |
|--------|---------------------|
| **Bank-owned journey** | Stages, suitability, disclosures, and UX copy are bank product decisions |
| **RM-assisted bancassurance** | Default operating model is assisted sale; customer self-serve is additive |
| **Multi-insurer choice** | Customer/RM can compare offers where product strategy allows |
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
