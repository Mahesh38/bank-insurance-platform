# Management readiness — PO document review & ask

**Audience:** Head of Insurance Business / Platform (Sponsor) + Steering  
**Owner:** Platform Product Owner  
**Date:** 2026-07-31  
**Status:** Pack for approval discussion — **not** yet sponsor-signed

---

## 1. What we are asking management to approve

| # | Ask | Decision needed |
|---|-----|-----------------|
| 1 | Adopt [Working Decisions Draft v1](../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) as the **working baseline** for MVP scope | Approve / Revise |
| 2 | Adopt [R0-SCOPE.md](../requirements/R0-SCOPE.md) as the MVP one-pager | Approve / Revise |
| 3 | Confirm **named** Executive Sponsor (role = Head of Insurance Business / Platform) | Name + accept |
| 4 | Confirm Steering cadence and that Open Validation Items follow **configurable controls until Compliance signs** | Accept approach |
| 5 | Authorise Wave 0 exit → Wave 1 design (not full build commitment yet) | Approve Wave 0 process |

**We are not asking** to approve a finished BRD, Figma as SoT, or hard-coded consent/retention rules.

---

## 2. Working baseline (30-second)

| Topic | Decision |
|-------|----------|
| LOB | **Life only** (Term, ULIP, Savings/Investment) |
| Journeys | **RM + Self-service + Hybrid** from Day 1 |
| Customers | **ETB only** (any AU Bank relationship) |
| Sold | **Policy issued** + confirmation + reconcilable + ops-trackable |
| Insurers | **Group A** via 1SB (in-platform); **Group B** catalogue + redirect |
| Gates | Need analysis + suitability **mandatory** before quote; consent **mandatory** (sequencing TBD) |
| Payment | Customer device; **AU Bank PG** only; no RM-device payment |
| 1SB | Current integration layer / accelerator — **not** the product; must be replaceable |
| Figma | Reference only |

Canonical IDs: [DECISION-LOG.md](../DECISION-LOG.md) (`D-001`…`D-014`).

---

## 3. Document hierarchy (how we avoid drift)

```text
1. Working Decisions (07)     — business MVP SSOT (working → then Approved)
2. Decision Log               — canonical D-xxx / DOC-xxx IDs
3. R0-SCOPE + BRD-OVERVIEW    — delivery one-pager + BRD chapter map
4. PRD + BRD-P0               — must not contradict 1–3
5. Discovery backlog + Gaps   — status trackers
6. Charter / Vision / Canvas  — narrative
7. Knowledge base             — baseline corpus (Volumes/Phases); superseded by WD where conflicted
8. Figma / prior 1SB eng      — non-binding
```

---

## 4. PO review findings (summary)

Full remediation applied in this PR cycle. Key issues found:

| Severity | Finding | Action |
|----------|---------|--------|
| Critical | Two conflicting `D-xxx` schemes | Unified in Decision Log (`D-` business / `DOC-` process) |
| Critical | PRD/BRD-P0 still said self-serve = later / P2 | Aligned to Day 1 (D-002) |
| High | Kickoff / SWOT / KB still said LOB/channel undecided | Stamped superseded / updated |
| High | Journey canvas MVP cut contradicted Sold=issuance | Fixed |
| Medium | Gaps/TODO checkboxes overstated “closed” vs “working” | Clarified Working vs Sponsor-signed |
| Medium | Redundant Open Validation lists | SSOT list kept in WD; others link |

**Verdict before sign-off:** Scope **intent** is coherent enough for a management validation discussion. Pack is **not** “project approved / BRD done.”

---

## 5. Still open — do not present as closed

| Item | Why it stays open |
|------|-------------------|
| Consent sequencing (IRDAI / RBI / Corporate Agency) | Regulatory R&D |
| Agent identity model | IRDAI + insurer onboarding |
| PII / audit retention & data residency | Compliance pack |
| Sponsor **name** | Governance |
| Suitability / consent **content** packs | Gate locked; content not written |
| Figma screen inventory | Walkthrough pending |
| Numeric KPI targets / pilot cohort / geography | Not set |
| BRD chapter depth under approved TOC | Headings only |
| 1SB Distributor ID / UAT keys | Provisioning in progress |
| Branch kiosk | Pending business decision |

---

## 6. What is done (honest)

| Done | Meaning |
|------|---------|
| Working Decisions Draft v1 captured | Working SSOT exists |
| P0 discovery Qs answered for scope | LOB, journeys, ETB, Sold, insurers, payment, suitability gate, 1SB stance |
| BRD overview headings PO-approved | TOC only — not detailed BRD |
| Gap register updated | GAP-001…005 closed; others open/partial |
| Doc conflicts from pre-WD era cleaned | Hierarchy + stale language remediations |

---

## 7. Recommended Steering agenda (60 min)

1. Confirm Working Decisions §§1–8, 14–15, 18–19 (15 min)  
2. Confirm Open Validation approach — configurable until Compliance OK (10 min)  
3. Name Sponsor + RACI owners for open items (10 min)  
4. Agree Wave 0 exit criteria + next 90-day design focus (15 min)  
5. Risks: hybrid UX complexity, compliance R&D, 1SB UAT timing, BR depth (10 min)

---

## 8. Sign-off

| Role | Name | Decision | Date |
|------|------|----------|------|
| Executive Sponsor | *TBC* | Approve working baseline / Revise | |
| Platform PO | | Ready for Steering | 2026-07-31 |
| Compliance (provisional) | | Noted — Open Validation path | |
