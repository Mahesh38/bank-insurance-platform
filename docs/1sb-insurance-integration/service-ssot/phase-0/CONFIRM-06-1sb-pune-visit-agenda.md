# CONFIRM-06 — 1SB Pune Office Visit: Agenda & Attendees

**Phase:** 0.6
**Status:** `DRAFT` — agenda proposed, date/attendees to be locked with 1SB RM
**Owner:** Product Owner (Rajal)
**Trigger:** Contracts signed with 1Silverbullet (1SB); this is the kickoff technical/product visit before Phase 1 build starts.
**Data log:** [PHASE-0-DATA-AND-GAPS.md](./PHASE-0-DATA-AND-GAPS.md) · [CONFIRM-01](./CONFIRM-01-onesb-access.md) · [CONFIRM-02](./CONFIRM-02-term-products.md) · [CONFIRM-03](./CONFIRM-03-inbound-auth.md)

---

## Purpose

AU Bank team (Product Owner + Solution Architect) travels to 1SB's Pune office with a **4–4.5 hour window**. Goal: convert everything currently stuck as `⬜ Pending` / `Ask 1SB` across CONFIRM-01, CONFIRM-02, CONFIRM-03 and the [onboarding email draft](./EMAIL-DRAFT-1SB-ONBOARDING-REQUEST.md) into **signed-off answers, live demos, and named owners with dates** — things that are slow over email but fast face-to-face. It is not a re-introduction to 1SB; it is a working session to unblock Phase 1.

Two products in scope, one decision already made:

| 1SB product | Bank's decision | Why we still spend time on it |
|---|---|---|
| **Insurance Gateway** (API middleware to insurers) | **Adopt** — bank backend integrates via `1sb-integration-service` | Core of the whole visit — auth, catalog, journey, payment, SLAs |
| **Application Layer** ("player" web app — Vue.js frontend / Node.js backend) | **Do not adopt** — bank owns its own UI/app layer (RM app + customer app, per Mahesh's architecture) end to end, talking only to the bank's own backend, which in turn talks to the Gateway | Still worth a **guided walkthrough**, purely as a reference for journey UX, screen sequencing, and how they compose Gateway calls — not as code or a component we integrate |

**Non-negotiable architecture reminder for the room:** bank UI and bank domain services never call 1SB shapes directly — everything passes through the Bank Insurance Gateway's canonical ports (`QuotePort`, `ProposalPort`, `PaymentPort`, `StatusPort`, `DocumentPort`, `MasterDataPort`, `EligibilityPort`, `IdentityVerificationPort`, `AgentPort`). See [replaceable-middleware.md](../../architecture/replaceable-middleware.md). Nothing agreed on-site should require breaking that.

---

## Pre-visit prep (send 2–3 days before, do not wait to ask these live)

- [ ] Send the [1SB onboarding email](./EMAIL-DRAFT-1SB-ONBOARDING-REQUEST.md) (asks S1–S7) ahead of the visit so 1SB's team arrives with data, not promises to "check and revert."
- [ ] Ask 1SB to confirm who will attend for: Relationship/Account management, Solution Architecture / Integration engineering, Product/insurer onboarding & console, Payments/reconciliation, Application Layer (frontend) team, Support/Delivery. Reserve the slot only once this is confirmed — a visit without the right people on their side is the single biggest risk to the 4-hour budget.
- [ ] Share [replaceable-middleware.md](../../architecture/replaceable-middleware.md) and the executive overview with 1SB's architect beforehand so the on-site architecture discussion starts from "confirm/challenge this" rather than a first explanation.
- [ ] Print/carry this agenda plus [CONFIRM-01](./CONFIRM-01-onesb-access.md), [CONFIRM-02](./CONFIRM-02-term-products.md), [CONFIRM-03](./CONFIRM-03-inbound-auth.md) — use them as literal checklists to tick live in the room.
- [ ] Confirm whether a bank Security/Infra representative can join the auth block by video call — Mahesh can carry it alone, but a live security voice strengthens the mTLS/IP-whitelist conversation.

---

## Attendees

### Bank side (traveling)

| Role | Name | Owns |
|---|---|---|
| Product Owner | Rajal | Commercial, product/insurer catalog, payment & "Policy Sold" definition, roadmap |
| Solution Architect | Mahesh | API/auth architecture, replaceability, Application Layer review, technical Q&A |

Recommended (remote, if available): a Security/Infra representative dialed in for the auth block (30 min), and/or a backend engineer dialed in for the API deep-dive if one isn't traveling.

### 1SB side (requested — confirm names before locking the date)

| Role | Needed for | Blocks (if absent) |
|---|---|---|
| Relationship/Account Manager | Whole visit, commercial, credential issuance authority | Nothing can be signed off without them |
| Solution Architect / Integration Tech Lead | Gateway architecture, auth, error handling, SLAs | Block 1, 2 |
| Product/Onboarding specialist (console owner) | Insurer & product catalog, console demo, product enablement lead time | Block 3 |
| Payments/Reconciliation specialist | Payment flow, intimation, settlement/reconciliation reporting | Block 4 |
| Application Layer engineer or product owner | Live demo of the Vue.js/Node.js app | Block 5 |
| Support/Delivery Manager | Environment promotion, SLAs, escalation path, versioning | Block 6 |

---

## Agenda (4.5 hours, trims to 4 hours by cutting the italicized optional items)

| # | Time | Block | Duration | Priority | 1SB attendee(s) |
|---|---|---|---|---|---|
| 0 | 09:30–09:45 | Welcome, intros, confirm today's goal = leave with signed answers not promises | 15 min | — | All |
| 1 | 09:45–10:30 | Gateway architecture & end-to-end journey walkthrough | 45 min | P0 | Solution Architect |
| 2 | 10:30–11:00 | Authentication, credentials & network access — **handover, not discussion** | 30 min | P0 | Solution Architect + Account Manager |
| 3 | 11:00–11:45 | Product/insurer catalog + **console** demo | 45 min | P0 | Product/Onboarding specialist |
| — | 11:45–12:00 | Break | 15 min | — | — |
| 4 | 12:00–12:30 | Payment flow & reconciliation | 30 min | P1 | Payments specialist |
| 5 | 12:30–13:00 | *Application Layer ("player" app) live demo — reference only* | 30 min | P2 | App team |
| 6 | 13:00–13:20 | Environments, support, SLAs, versioning | 20 min | P1 | Delivery Manager |
| 7 | 13:20–13:35 | *Commercial & roadmap check-in (Health/Motor timing, pricing tiers)* | 15 min | P2 | Account Manager |
| 8 | 13:35–14:00 | Wrap-up: recap decisions, owners, dates; agree follow-up cadence | 25 min | P0 | All |

**If the day compresses to 4 hours:** drop or shorten Block 5 to a 10-minute screen-share instead of a full demo, and fold Block 7 into email follow-up — do not cut Blocks 1–4, they carry every open item currently blocking Phase 1.

---

## Block 1 — Gateway architecture & journey walkthrough (45 min)

**Goal:** validate the bank's mental model of the journey state machine against 1SB's actual behavior, and surface anything the docs got wrong or left vague.

Working from [01-executive-overview.md](../01-executive-overview.md) mental model: Assess → Quote → Select → Proposal → Underwriting/Requirements → Payment → Policy Issued.

Questions to close:
- [ ] Walk the full Term + Saving journey live against demo/sandbox — does the sequence and the async poll pattern (quote poll, proposal poll) match our documented model exactly?
- [ ] What is the actual SLA on quote poll and proposal poll — typical and worst-case latency, and recommended poll interval/backoff? (Feeds `provider-config.yml` `poll.*`.)
- [ ] Full list of `applicationStatus` values and manufacturer sub-statuses per LOB — is [application-status.md](../field-guides/application-status.md) complete and current?
- [ ] Webhook/callback support as an alternative to polling, for any step — does it exist, and is it on their roadmap?
- [ ] Error/retry semantics: what's retryable vs terminal, idempotency keys, rate limits per distributor.
- [ ] API versioning and change-management policy — how much notice before a breaking change to proposal schema or status enums?
- [ ] `varFields` / dynamic proposal form behavior — how often do required fields change per insurer, and is there a changelog or webhook for form-schema changes?

**Exit check:** architecture doc updated (if needed) same day; no open questions about the happy-path journey.

---

## Block 2 — Authentication, credentials & network access (30 min)

**Goal:** this is the block most likely to actually unblock engineering — treat it as a handover, not a discussion. Everything here maps directly to [CONFIRM-01](./CONFIRM-01-onesb-access.md) sections B–E and [email draft](./EMAIL-DRAFT-1SB-ONBOARDING-REQUEST.md) asks S1, S2, S4, S5, S6.

Must leave with:
- [ ] Dedicated `BCIBL` API key + secret for **Dev/Demo and UAT**, issued or a firm issuance date (CONFIRM-01 B1/B2). Production credentials — confirm process and timeline even if not issued today.
- [ ] Confirmed base URL per environment if it differs from `https://demo.api.1silverbullet.tech` (email ask S2).
- [ ] Production IP whitelisting: exact process, where to send bank egress CIDRs, turnaround time, and confirmation format (CONFIRM-01 D, email ask S6). Get a named contact for this, not just "submit a ticket."
- [ ] Confirm `agentID 109337` / alternate `8925` — which insurers/environments they're valid for (email ask S4); confirm Sales Channel / Channel Type / Type-of-Sale enum values (email ask S5).
- [ ] Credential rotation policy and secret storage guidance on their side (does 1SB support key rotation without downtime?).
- [ ] Auth mechanism confirmed as HTTP Basic (API key/secret) + IP whitelist + `distributorId` for outbound calls — any MTLS option on their side, or is Basic Auth the only supported mode?

Note: bank's **inbound** auth (JWT/mTLS from bank apps → `1sb-integration-service`, per [CONFIRM-03](./CONFIRM-03-inbound-auth.md)) is an internal bank decision — nothing to negotiate with 1SB here, but worth mentioning so their architect understands why bank API contracts stay stable even if 1SB's outbound auth changes.

**Exit check:** CONFIRM-01 status flips from `PARTIAL` to `CONFIRMED` for at least Dev/UAT, or every remaining gap has a named 1SB owner and date.

---

## Block 3 — Product/insurer catalog + console demo (45 min)

**Goal:** close the multi-insurer product matrix gap and understand how catalog changes actually get made — this directly unblocks [CONFIRM-02](./CONFIRM-02-term-products.md) and the Term-vs-Saving-first decision.

Questions to close:
- [ ] Full list of insurers/products enabled for `BCIBL` across Demo/UAT/Prod (email ask S3) — Term and Saving at minimum, with `manufacturerId`, `productCode`, `productName`, `productType`, participating/non-participating flags, and any restrictions (Single-Quote-only, BI flags).
- [ ] Live demo of the **1SB console** used to configure products/insurers: is catalog management self-service for the bank, or 1SB-managed on request? Who has access, and what's the RBAC model?
- [ ] Turnaround time to enable a new product/insurer once agreed commercially — is it instant in the console, or does it require a 1SB-side release?
- [ ] Confirm which insurers sit in **Group A** (in-platform via 1SB) vs any that the bank will route as **Group B** (catalogue + redirect, no 1SB quote) — this maps to bank decision [D-010](../../../au-bank-insurance-platform/DECISION-LOG.md).
- [ ] Gate-criteria / eligibility API behavior — does it vary meaningfully per insurer, or is it a fixed shape with dynamic values?
- [ ] Master Lookup coverage — confirm it covers every enum the bank UI needs (channel, gender, relationship, etc.) so nothing gets hardcoded.
- [ ] Sandbox quote returns non-`empty` `offers[]` for the confirmed catalog entries — verify live if credentials from Block 2 are active by then.

**Exit check:** CONFIRM-02 has a committed Term product row (not just Saving/E38), and a documented answer for "how do we add product #3 without waiting weeks."

---

## Block 4 — Payment flow & reconciliation (30 min)

**Goal:** the bank will **not** use 1SB's payment gateway (payment happens on the customer's own device via AU Bank PG only, per [D-006](../../../au-bank-insurance-platform/DECISION-LOG.md)) — but the bank still needs to fully understand how 1SB's payment/reconciliation model works, because the bank's own reconciliation ledger has to reach the same end state 1SB expects (`Payment URL` → `Payment Intimation` → insurer confirmation → policy issuance). See [payment.md](../field-guides/payment.md).

Questions to close:
- [ ] Since the bank supplies its own payment URL/redirect flow and skips 1SB's hosted payment page, confirm exactly what 1SB needs back: is **Payment Intimation** mandatory for every insurer, or only some? What's the SLA to call it after a successful bank-PG transaction?
- [ ] What happens if intimation fails or times out (`PAYMENT_INTIMATION_FAILED`) — retry window, manual fallback, who to call.
- [ ] How does premium reconciliation work end to end between bank PG → 1SB → insurer settlement? What reports/APIs does 1SB expose for reconciliation and commission/MIS (ties to Rajal's "Policy Sold" definition — reconcilable premium is one of the four conditions)?
- [ ] Settlement cycle and commission statement format/frequency from 1SB and insurers.
- [ ] Refund/cancellation handling — does 1SB have a role once payment is bank-PG-only, or does that stay entirely internal to the bank?
- [ ] Any premium-amount or currency validation 1SB performs that the bank must mirror before calling Payment Intimation (to avoid mismatches with the quoted premium).

**Exit check:** a clear sequence diagram (bank PG success → Payment Intimation call → status confirmation → reconciliation record) the team can build against, even though the bank never routes money through 1SB.

---

## Block 5 — Application Layer ("player" app) live demo — reference only (30 min)

**Goal:** the bank already reviewed this product and decided **not** to adopt it — the bank's own UI (RM-assisted + customer, per Mahesh's Flutter-based client layer) will be the system of record for UX. This block exists purely to mine it for reference: how they sequence Gateway calls per screen, what states/errors they surface to the end user, and any UX pattern worth reusing or deliberately avoiding.

Questions to close:
- [ ] Live walkthrough of the customer/RM journey screens end to end (quote compare, proposal form, payment redirect, status).
- [ ] Which Gateway APIs does each screen call, in what order, and how do they handle the async poll waits in the UI (spinners, timeouts, retry UX)?
- [ ] How do they render the **dynamic proposal form** schema generically across LOBs/insurers — any lessons for the bank's own dynamic-form renderer?
- [ ] Ask if they can share sequence diagrams, a Postman collection tied to the demo flow, or short screen-recordings afterward — the bank is not asking for their source code or intending to integrate the app itself, only using it as a design reference.
- [ ] Explicitly confirm with 1SB that not adopting the Application Layer doesn't affect commercials or support — the bank is Gateway + Building Blocks only.

**Exit check:** a short internal note (not shared with 1SB) capturing 2–3 UX patterns worth reusing and 2–3 to avoid, filed alongside the bank's own Application Layer design work.

---

## Block 6 — Environments, support, SLAs, versioning (20 min)

- [ ] UAT and Production go-live timeline and any gating steps beyond credentials/whitelisting.
- [ ] Named support/escalation contact and process for Dev, UAT, Prod (email ask S7) — response-time SLA for a Sev1 (e.g., quote/proposal API down) vs a Sev3 (data question).
- [ ] Sandbox stability/uptime — is demo suitable for ongoing CI/integration tests, or does it degrade under load?
- [ ] Postman collection / latest OpenAPI spec / changelog handoff (email ask S7).
- [ ] Deprecation and breaking-change notice policy (ties back to Block 1's versioning question — confirm the actual process, e.g., advance notice period, sandbox-first rollout).

---

## Block 7 — Commercial & roadmap (15 min, optional if time-constrained)

- [ ] Any volume-based pricing tiers or minimum commitments relevant to the bank's Phase 1 (Life-only) vs later Health/Motor expansion.
- [ ] 1SB's own roadmap for Group/Embedded products, if the bank considers that later.
- [ ] Confirm relationship/escalation ownership on 1SB's side going forward (single point of contact for Product vs Engineering).

---

## Block 8 — Wrap-up (25 min)

- [ ] Recap every checklist item above with a status: **Closed today** / **Owner + date** / **Parked**.
- [ ] Agree a recurring sync cadence (suggest weekly 30-min call) through Phase 1 delivery.
- [ ] Agree who updates [CONFIRM-01](./CONFIRM-01-onesb-access.md), [CONFIRM-02](./CONFIRM-02-term-products.md), and [PHASE-0-DATA-AND-GAPS.md](./PHASE-0-DATA-AND-GAPS.md) scorecards after the visit (owner: bank PO, within 24h).
- [ ] Log any business/scope decisions surfaced during the visit into [DECISION-LOG.md](../../../au-bank-insurance-platform/DECISION-LOG.md).

---

## Post-visit checklist (within 24–48h)

- [ ] Update [CONFIRM-01](./CONFIRM-01-onesb-access.md) status column and header.
- [ ] Update [CONFIRM-02](./CONFIRM-02-term-products.md) with confirmed Term row(s).
- [ ] Update [PHASE-0-DATA-AND-GAPS.md](./PHASE-0-DATA-AND-GAPS.md) scorecard (§4) — most rows should move from `⬜` toward `✅`.
- [ ] File the vault-ready credentials per [CONFIRM-01](./CONFIRM-01-onesb-access.md) §B (never in git/chat).
- [ ] Send a written recap to 1SB attendees confirming agreed action items/dates (paper trail beats verbal agreement).
- [ ] Feed [CONFIRM-04 SSOT kickoff](./CONFIRM-04-ssot-kickoff.md) with anything that changes the internal team's design assumptions before that session runs.
