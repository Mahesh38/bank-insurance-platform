# BRD alignment — Rajal pack 2026-09-28

**Work item:** `SUG-20260928-brd` · `DOC-005`  
**Owner:** Rajal (intent) · Principal BA (analysis quality)  
**Freshness:** `FreshnessCheck` 2026-09-28 exit 1 — `state_as_of` is 15 days old; disclosed, work still admissible.  
**Verdict on application code this turn:** **not started**. Conflicts and TBDs are unresolved. The pack forbids silent resolution.

This file is analysis, not a Product decision. Rajal must answer the questions in §5 before any module is implemented against this pack.

---

## 1. What arrived

Nine detailed BRDs, ingested under [`brd-detailed/`](./brd-detailed/README.md). QA fidelity check on the context extracts: **passed** (0 missing source values).

| BRD | Covers BRD-OVERVIEW | Product types | Formal status |
|-----|---------------------|---------------|---------------|
| Login 1.0 | §1 Login (partial: OTP/lock/unlock; Forgot Password **removed** from both modes) | n/a | Draft, blank approvals |
| Lead 1.0 | §3 Lead + §4 CBS search | Term, Savings, ULIP | Draft, blank approvals |
| Suitability 1.0 | §6 Suitability (Savings/ULIP only) | Savings, ULIP | Draft, blank approvals |
| Savings listing / compare 1.0 | §7.1–7.5 (Savings) | Savings | Draft, blank approvals |
| Product details Savings 1.0 | §7.3 View more / product details (Savings) | Savings | Draft, blank approvals |
| Quote finalisation 1.0 | §7.7–7.8, §7.10 eBI (Savings) | Savings | Draft, blank approvals |
| Pitch Deck 1.0 | not a numbered overview chapter | Savings | Draft, blank approvals |
| Customer Buying Journey 1.2 | §8 Proposal + §10 Payment + §11 Submission | via insurer API | Draft, blank approvals |
| Exception Handling 1.0 | §5 approval/block slice only | Life generic | Draft, blank approvals |

---

## 2. Precedence after this ingest (`DOC-005`)

When two documents disagree on **module behaviour** for a chapter this pack covers:

```text
1. docs/governance/                              process — still wins on "may we start"
2. requirements/brd-detailed/                    module behaviour (this pack)
3. 07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS  programme decisions not restated here
4. DECISION-LOG                                  D-xxx / DOC-xxx
5. R0-SCOPE + BRD-OVERVIEW                       sequencing / TOC
6. PRD + BRD-P0-CAPABILITIES                     superseded where they conflict with 2
7. knowledge-base / Figma / prior 1SB notes      non-binding
```

**Still true until Rajal overturns them** (not restated as changed by this pack):

- Life only (`D-001`)
- ETB only (`D-009`)
- Sold = issued + confirmation + reconcilable + ops-trackable (`D-007`)
- No payment on RM device; AU Bank PG (`D-006`)
- Need analysis + suitability mandatory before quote (`D-005`) — **but see conflict C5**
- 1SB is current aggregator and must stay replaceable (`D-003`, `D-004`)
- Figma is reference only (`D-012`)
- Current increment sequences DIY / hybrid **mode-switch** behind assisted (`CR-015` / `R0-SCOPE`) — **but see question Q2**

Unresolved TBD / TBC / assumption / indicative text in a BRD is **not** implementation authority.

---

## 3. Cross-BRD conflicts — do not implement until Rajal answers

### C1 / C2 — When does BI lock suitability and make the lead Eligible?

| Source | Rule |
|--------|------|
| Lead BRD `BR-BI-001` | BI Generated only when the **Quote API successfully returns** a Benefit Illustration |
| Product Details `AC-025` | Successful **customer-specific toolkit BI** (UI may say "Sample BI") marks BI Generated, Quote Generated, Eligible Lead, **locks Suitability**, blocks reassignment |
| Suitability `AC-038` / Quote finalisation STEP-12 | Lock after **successful final BI** |

These are three different events. Implementing any one as "the" lock is an assumption.

### C3 — Insurance RM assignment timing and Bank RM vs Bank SP

| Source | Rule |
|--------|------|
| Lead BRD | Bank SP selects Insurance RM at **lead creation**; Insurance RM selects branch/SP |
| Quote finalisation §20.1 and Customer Buying Journey v1.2 §32.1.2 | Bank RM / Bank seller: **no Insurance RM selection at lead creation**; assign **after successful final BI**. Customer v1.2 says: *"The Lead Module BRD should be updated to match the above timing."* |

Also unresolved: is **Bank RM** the same role as **Bank SP**, or a distinct RBAC role?

### C5 — Can the seller buy a product outside the suitable set?

| Source | Rule |
|--------|------|
| Listing `KBR-03` | Products outside Suitability **can be compared and purchased in the current scope** |
| Suitability §4.2 | Seller override to choose a product outside the suitable-product result is **out of scope** |
| Working Decisions `D-005` | Suitability is mandatory before quote; never bypass |

`KBR-03` is a direct tension with `D-005`. Not resolved here.

### C7 — Exception Handling, multiple rules

| Source | Rule |
|--------|------|
| `EH-FR-MULTI-015` | If any triggered rule has a **Block** outcome, the lead is blocked |
| §20.3 / `EH-BR-MULTI-002` / `EH-AC-015` | Follow the rule with the **highest configured hierarchy** (Block-if-any is not stated) |

Internal contradiction in one BRD.

### Aligned (do not reopen)

- Application ID is created **only when Proposal Form initiation succeeds** (Quote + Customer v1.2). Any Application ID on the Quote Created wireframe is replaced with BI Reference.
- Saving a lead does **not** run exception rules.
- Pitch Deck product-first share creates a Diary Lead and does **not** assign SP / Insurance RM.

---

## 4. Conflicts with existing repository SSOT (realigned this turn)

| Existing statement | Pack statement | Action taken |
|--------------------|----------------|--------------|
| `BRD-OVERVIEW.md`: "detailed requirements to be written under each section" | Nine detailed chapters now exist | Overview updated to point at `brd-detailed/` |
| Working Decisions / `docs/README.md`: Working Decisions win all content conflicts | Rajal: this pack is source of truth for module behaviour | Precedence updated (`DOC-005`). Programme `D-xxx` kept until explicitly overturned |
| Login overview §1.2 Forgot Password = R0/R1 | Login BRD: Forgot Password **removed** from both login modes; partner recovery is Unlock User | Overview §1.2 annotated; not implemented |
| Login overview §1.1 AD Integration / §1.4 SSO | Login BRD: Bank RM uses Employee ID + **bank system password** + OTP; Partner uses corporate email + platform password | Recorded; existing BFF is OAuth/OIDC token-hiding — **technology is Mahesh**, behaviour is Rajal |
| Suitability rule pack is Term-shaped and lives in Flutter | Suitability BRD is **Savings/ULIP only**; Term "final logic" TBD | Rule pack **not** overwritten. Term suitability has no new BRD |
| `D-002` Day-1 = RM + Self-service + Hybrid | Pack is **Assisted / PWA Assisted**; Customer v1.2 is seller handoff then **customer-controlled** payment | `D-002` **not** silently overturned — see Q2 |
| Lead campaign / bulk is out of current increment (`BOOT` / `R0-SCOPE`) | Lead BRD also keeps bulk/campaign **out** unless a formal scope change | No change needed |
| Exception handling in repo = platform HTTP error contract | Exception BRD = **business rule / approval / block** module (AUBIMA) | Different thing. Do not treat `bank-common-error` as this BRD |
| ADR-015 / Flutter NIP-APP | Several BRDs say **PWA Assisted Journey** | Channel **label** vs implementation stack — see Q8. Technology choice stays with Mahesh |

Older `BRD-P0-CAPABILITIES.md` and knowledge-base journey text are **superseded** for any behaviour these nine files specify. They were not rewritten line-by-line this turn.

---

## 5. Questions for Rajal — do not assume

### Q1 — Formal status

All nine files are *Draft for Business and Technology Review* with blank Business / Technology / IS / QA rows. Confirm: treat them as **working SSOT now**, or wait for those sign-offs before implementation?

### Q2 — Journeys vs `D-002` / `CR-015`

This pack specifies assisted seller flows plus a **customer-controlled** verification/payment/submission slice. It does **not** specify a standalone DIY journey or assisted/DIY mode-switch.

Is the current increment still **assisted-first** (`CR-015`, `R0-SCOPE`), with DIY/hybrid mode-switch later? Or does Customer v1.2 replace `D-002` "three journeys Day 1"?

### Q3 — BI lock event (C1 / C2)

Which **single** event locks Suitability, marks Eligible Lead, and blocks reassignment?

- Quote API BI only (Lead `BR-BI-001`)
- First customer-specific toolkit / "Sample BI" (Product Details `AC-025`)
- Successful **final** BI (Suitability / Quote finalisation)
- Some other rule you will write

### Q4 — Insurance RM assignment (C3)

Does Bank SP / Bank seller select Insurance RM at **lead creation** (Lead BRD), or only **after successful final BI** (Quote + Customer v1.2, which ask for the Lead BRD to be updated)?

Are **Bank RM** and **Bank SP** the same role, or two RBAC roles?

### Q5 — Buy outside suitability (C5)

May a seller Compare / Buy Now a product that is **not** in the suitable-product result (`KBR-03`)? If yes, that is a bypass of `D-005` and needs an explicit Product + Compliance decision. If no, Listing `KBR-03` must be rewritten.

### Q6 — Exception multi-rule (C7)

On multiple triggered rules: **Block-if-any-Block**, or **highest hierarchy wins**?

### Q7 — Implementation order

Existing application surfaces today:

- Login: BFF + identity services (Slice 1) — closest to a real path
- Lead / Suitability / Quote: Flutter fake backend + domain-service **skeletons**
- Pitch Deck: Figma only
- Customer buying: no customer app
- Exception (AUBIMA): not built (platform HTTP errors are a different module)

Which module should we implement **first** after Q1–Q6 are answered?

### Q8 — "PWA" vs Flutter NIP-APP

BRDs say PWA Assisted Journey. The admitted client is Flutter NIP-APP (`ADR-015`). Is "PWA" a **channel requirement** (must be a PWA), or a design-file label? (Stack remains Mahesh.)

### Q9 — Missing chapters

Confirm the chapters in [`brd-detailed/README.md` § Not in this pack](./brd-detailed/README.md#not-in-this-pack) are **later BRDs**, not "build from the overview headings".

### Q10 — Open formulas (do not invent)

Suitability §22: tentative premium formula, PPT / policy term rule, occupation / education masters — all **to be defined**. Listing §21: eligibility, ordering, tax API, comparison rows, AU Bank Advantage content — **to be finalised**. Exception: notification content, API timeouts, retention — **to be confirmed**.

Who supplies each, and when?

---

## 6. Requirement-to-code impact map (no code this turn)

| BRD | UI today | API today | Data / tests today | First honest gap |
|-----|----------|-----------|--------------------|------------------|
| Login | Flutter demo SSO | `workforce-access-bff` `/api/v1/auth/*` + identity adapter + PDP | JES UC-01…UC-05 delivered | Captcha, mandatory OTP every login, lock after 3 failures / 30-day inactivity, Unlock User, partner password create/expiry — **not built**. Bank password ownership is bank IdP, not this platform (matches Login out-of-scope) |
| Lead | `apps/rm-workspace-app` fake inbox / create | NIP BFF lead OpenAPI **spec only**; `lead-service` skeleton | PLAN-004 / PLAN-005 contracts | No `/leads` runtime. Assignment timing blocked on Q4 |
| Suitability | Flutter `SUIT-ALGO-LIFE-v1.0` + screens | `suitability-service` skeleton | Rule pack is Term-shaped | Savings/ULIP screens and lock rules blocked on Q3, Q5, Q10 |
| Savings listing / compare | Term sample compare UI | `product-catalogue-service` / `quotation-service` skeletons; 1SB savings quote adapter exists | 1SB `SavingQuoteHandler` | No savings listing/compare journey; tax popup API unknown |
| Product details Savings | Figma only | Term `ProductUiDataController` only | — | No savings details / toolkit / sample-BI path |
| Quote finalisation / riders / final BI | Basic quote + compare | 1SB quote path | — | No riders, final BI, share, post-BI RM assignment |
| Pitch Deck | Figma only | none | — | Greenfield |
| Customer Buying Journey | none | 1SB proposal/payment controllers (adapter), no customer BFF/app | — | Customer-controlled surface is out of current increment unless Q2 says otherwise |
| Exception (AUBIMA) | journey-guard UX only | `bank-common-error` (HTTP contract — **different module**) | — | Greenfield business-exception engine; blocked on Q6 |

Standing constraints that any later implementation must still honour (`BOOT.md` §5): no quote without valid suitability; no proposal without unexpired consent; payment only on customer device; UI/BFF never see 1SB wire codes; Flutter never receives OAuth tokens.

---

## 7. BA readiness

| Artefact quality | Result |
|------------------|--------|
| Source fidelity of the nine extracts | `READY` (QA pack passed) |
| Cross-BRD consistency | `NOT_READY` — C1, C2, C3, C5, C7 open |
| Deterministic AC for implementation | `NOT_READY` — Q10 formulas and masters open |
| Product intent | **Rajal owns**. This file does not invent it |

---

## Change control

| Version | Date | Change | Owner |
|---------|------|--------|-------|
| 0.1 | 2026-09-28 | Ingest pack, record conflicts, ask Rajal. No application code. | Agent (BA analysis) / Rajal (intent) |
