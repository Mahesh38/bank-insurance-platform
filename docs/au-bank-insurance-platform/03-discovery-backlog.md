# Discovery backlog — must-answer before scope freeze

**Owner:** Business Analysis (with Platform PO)  
**Rule:** P0 items block Release scope freeze. P1 can defer with date. P2 are nice-to-know.  
**SSOT for closed items:** [07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md](./07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)

Status values: `Open` · `In discussion` · `Answered` · `Deferred`

---

## P0 — Business & channel

| ID | Question | Why it matters | Owner | Status | Answer |
|----|----------|----------------|-------|--------|--------|
| Q-P0-01 | What is the **first LOB** for AU Bank MVP? | Everything sequences from this | Bancassurance | **Answered** | **Life only** — Term, ULIP, Savings/Investment, future life products. Health/Motor/Travel out of MVP. |
| Q-P0-02 | Is MVP **RM-assisted only**, customer self-serve, or hybrid? | UX, auth, journey ownership | Digital + PO | **Answered** | **All three from Day 1**: RM-assisted, self-service, hybrid with seamless mode switching. |
| Q-P0-03 | Which customer segment? | Eligibility & prefill | Product | **Answered** | **ETB only** — any AU Bank relationship (SA/CA/Loan/CC/FASTag/etc.). No Retail/HNI/Staff/Corporate cut. NTB = future. |
| Q-P0-04 | What is the official **definition of sold**? | Metrics & engineering DoD | PO | **Answered** | **Policy issued** + bank confirmation + financial reconciliation possible + ops can track lifecycle. Quote/proposal/payment alone ≠ sold. |
| Q-P0-05 | Which insurer(s) must be live for pilot? | Aggregator panel + testing | Bancassurance | **Answered** | **Group A (1SB):** ICICI Prudential, HDFC Life, Bajaj Allianz/Bajaj (as applicable). **Group B:** catalogue + redirect only. |
| Q-P0-06 | Who completes **payment**? | Payment UX & liability | Digital + Payments | **Answered** | Customer on **personal device**. RM may share link. **No payment on RM device.** AU Bank PG only (no third-party PG). IFT + PG in MVP; cheque later. |
| Q-P0-07 | Is **suitability / need analysis** mandatory before quote? | Compliance gate | Compliance + Product | **Answered** | **Yes — mandatory.** Never bypass before quote. |
| Q-P0-08 | How is **RM ↔ agentId** mapped for each insurer? | Attribution | Ops + Compliance | **In discussion** | Individual advisor identifier expected; **validate vs IRDAI + insurer onboarding** before freeze. |
| Q-P0-09 | Confirm **distributorId** custody? | Spoofing / multi-tenant risk | Infosec + PO | **Answered** | 1SB Distributor ID = **AU Bank**. Insurer credentials stored under Distributor ID by 1SB; runtime selects insurer-specific creds. Bank apps must not trust caller-supplied distributor. |
| Q-P0-10 | Consent model: what / when / retention? | Legal gate | Compliance | **In discussion** | Consent **mandatory**. Exact sequencing pending IRDAI/RBI/Corporate Agency R&D. UX may consolidate; compliance cannot be bypassed. |

---

## P0 — Experience & process (Figma-driven)

| ID | Question | Why it matters | Owner | Status | Answer |
|----|----------|----------------|-------|--------|--------|
| Q-P0-11 | Screen inventory of Figma prototype complete? | Single process truth | Digital + BA | Open | Figma = **reference only** (MVP + concept + future). Not SoT. |
| Q-P0-12 | Which Figma flows are MVP vs concept/future? | Avoid building demos | PO + Digital | Open | Use BRD + working decisions as SoT; Figma supports UX only |
| Q-P0-13 | Where does journey **pause** for async quote/UW? Who is notified? | Ops + UX | Digital + Ops | Open | |
| Q-P0-14 | Requirements / document upload in MVP or later? | Scope cut | PO | Open | |
| Q-P0-15 | Branch / geography rollout constraints? | Pilot design | Bancassurance | Open | Branch kiosk = pending / out of MVP |

---

## P0 — Platform & vendor

| ID | Question | Why it matters | Owner | Status | Answer |
|----|----------|----------------|-------|--------|--------|
| Q-P0-16 | Is **1SB** confirmed commercial + sandbox for AU Bank? | Go-live dependency | Bancassurance | **Answered** | Agreement signed; SOP done; UAT provisioning; Distributor ID expected shortly; start on UAT. |
| Q-P0-17 | Hard requirement: bank apps never call aggregator shapes? | Architecture binding | PO | **Answered** | **Yes.** 1SB = current integration layer / accelerator; no tight coupling; replaceable by other aggregator or bank-owned layer. |
| Q-P0-18 | Which bank systems are **mandatory** for MVP? | Dependency map | Architecture + PO | **Answered** | SSO (bank identity), bank notifications, **AU Bank PG**, CBS for ETB fetch, Lead module in Insurance Platform (future → Sampath). |
| Q-P0-19 | Data residency / logging / retention for insurance PII? | Compliance build | Compliance + Infosec | **In discussion** | Listed under Open Validation Items — use configurable policies until validated. |
| Q-P0-20 | Who is executive sponsor? | Decision velocity | PO | **Answered (role)** | Head of Insurance Business / Insurance Platform at AU Bank. **Name pending.** |

---

## P1 — Important but deferrable with date

| ID | Question | Owner | Status | Defer-to date |
|----|----------|-------|--------|---------------|
| Q-P1-01 | Health / Motor sequencing after Life MVP | Bancassurance | **Deferred** | Post-MVP |
| Q-P1-02 | Ops console vs RM-only tools for exceptions | Ops | Open | |
| Q-P1-03 | Multi-language / vernacular | Digital | Open | |
| Q-P1-04 | Staff insurance / affinity products | Product | Open | Covered under ETB if staff are bank customers |
| Q-P1-05 | Commission / MIS reporting consumers | Finance + Ops | Open | Commission out of MVP core; MIS R1 |
| Q-P1-06 | Lead migration to Sampath | Architecture | **Answered (direction)** | MVP = in-platform Lead; future = Sampath; design for migration |
| Q-P1-07 | Multi-aggregator routing | Architecture | **Deferred** | Extensible design only; not MVP |

---

## P2 — Parking

| ID | Question | Status |
|----|----------|--------|
| Q-P2-01 | White-label 1SB application layer as front door? | Rejected as primary UX — bank owns journey |
| Q-P2-02 | Dual-aggregator routing in year 1? | Deferred — extensibility only |
| Q-P2-03 | Embedded insurance in loan disbursement? | Deferred — backlog |
| Q-P2-04 | NTB onboarding | Deferred — post-MVP |
| Q-P2-05 | Branch kiosk journey | Pending business decision |
| Q-P2-06 | Bank-owned aggregation layer | Deferred — future |

---

## How BA closes a question

1. Record **Answer** in the table (short).  
2. Link supporting artefact (prefer Working Decisions §).  
3. If it changes charter/vision/journey — update those docs in the same change.  
4. Escalate conflicts to Platform PO; do not silently pick engineering convenience.
