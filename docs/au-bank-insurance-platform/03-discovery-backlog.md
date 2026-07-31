# Discovery backlog — must-answer before scope freeze

**Owner:** Business Analysis (with Platform PO)  
**Rule:** P0 items block Release scope freeze. P1 can defer with date. P2 are nice-to-know.

Status values: `Open` · `In discussion` · `Answered` · `Deferred`

---

## P0 — Business & channel

| ID | Question | Why it matters | Owner | Status | Answer |
|----|----------|----------------|-------|--------|--------|
| Q-P0-01 | What is the **first LOB** for AU Bank MVP? | Everything sequences from this | Bancassurance | Open | |
| Q-P0-02 | Is MVP **RM-assisted only**, customer self-serve, or hybrid? | UX, auth, journey ownership | Digital + PO | Open | |
| Q-P0-03 | Which customer segment? (existing retail only / HNI / staff / …) | Eligibility & prefill | Product | Open | |
| Q-P0-04 | What is the official **definition of sold** for pilot success? | Metrics & engineering DoD | PO | Open | |
| Q-P0-05 | Which insurer(s) must be live for pilot? | Aggregator panel + testing | Bancassurance | Open | |
| Q-P0-06 | Who completes **payment** — customer on device, RM link share, branch kiosk? | Payment UX & liability | Digital + Payments | Open | |
| Q-P0-07 | Is **suitability / need analysis** mandatory before quote? Content owner? | Compliance gate | Compliance + Product | Open | |
| Q-P0-08 | How is **RM ↔ agentId** mapped for each insurer? | Attribution | Ops + Compliance | Open | |
| Q-P0-09 | Confirm **distributorId** custody (vault/config only)? | Spoofing / multi-tenant risk | Infosec + PO | Open | |
| Q-P0-10 | Consent model: what is captured, when, evidence retention? | Legal gate | Compliance | Open | |

---

## P0 — Experience & process (Figma-driven)

| ID | Question | Why it matters | Owner | Status | Answer |
|----|----------|----------------|-------|--------|--------|
| Q-P0-11 | Screen inventory of Figma prototype complete? | Single process truth | Digital + BA | Open | Pending walkthrough |
| Q-P0-12 | Which Figma flows are MVP vs concept/future? | Avoid building demos | PO + Digital | Open | |
| Q-P0-13 | Where does journey **pause** for async quote/UW? Who is notified? | Ops + UX | Digital + Ops | Open | |
| Q-P0-14 | Requirements / document upload in MVP or later? | Scope cut | PO | Open | |
| Q-P0-15 | Branch / geography rollout constraints? | Pilot design | Bancassurance | Open | |

---

## P0 — Platform & vendor

| ID | Question | Why it matters | Owner | Status | Answer |
|----|----------|----------------|-------|--------|--------|
| Q-P0-16 | Is **1SB** confirmed commercial + sandbox for AU Bank? | Go-live dependency | Bancassurance | Open | |
| Q-P0-17 | Hard requirement: bank apps never call aggregator shapes? | Architecture binding | PO | Open | |
| Q-P0-18 | Which bank systems are **mandatory integrations** for R0? (CIF, SSO, notifications, DMS…) | Dependency map | Architecture + PO | Open | |
| Q-P0-19 | Data residency / logging / retention policy for insurance PII? | Compliance build | Compliance + Infosec | Open | |
| Q-P0-20 | Who is executive sponsor for this platform? | Decision velocity | PO | Open | |

---

## P1 — Important but deferrable with date

| ID | Question | Owner | Status | Defer-to date |
|----|----------|-------|--------|---------------|
| Q-P1-01 | Health / Motor sequencing after first LOB | Bancassurance | Open | |
| Q-P1-02 | Ops console vs RM-only tools for exceptions | Ops | Open | |
| Q-P1-03 | Multi-language / vernacular | Digital | Open | |
| Q-P1-04 | Staff insurance / affinity products | Product | Open | |
| Q-P1-05 | Commission / MIS reporting consumers | Finance + Ops | Open | |

---

## P2 — Parking

| ID | Question |
|----|----------|
| Q-P2-01 | White-label 1SB application layer as temporary MVP shortcut? |
| Q-P2-02 | Dual-aggregator routing in year 1? |
| Q-P2-03 | Embedded insurance in loan disbursement flows? |

---

## How BA closes a question

1. Record **Answer** in the table (short).  
2. Link supporting artefact (meeting note, Figma frame, uploaded doc).  
3. If it changes charter/vision/journey — update those docs in the same change.  
4. Escalate conflicts to Platform PO; do not silently pick engineering convenience.
