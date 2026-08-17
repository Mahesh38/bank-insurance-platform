# 05 — Forecast, Confidence and Descope Levers

**Owner:** Kalpana — Delivery Head (R12). Delivery forecast and confidence are `O/A/R`; Kalpana
**cannot average binding blockers into green or manufacture missing evidence**, and cannot decide
scope — every lever below requires Rajal's authority to pull.
**Method:** [Kalpana §7–§8, §12](../context/roles/kalpana-delivery-head/04-delivery-planning-critical-path-and-parallelization.md)

---

## 1. The forecast

Stated probabilistically with assumptions named, per §8. **False precision is worse than a range.**

| Outcome | Date | Confidence | What it assumes |
|---|---|---:|---|
| **Full R0 scope** — full pilot cohort, ops console, insurer-rep portal, management dashboards — deployed and certified | 2026-12-16 | **40%** | Every external dependency lands on its **first** named date, no material pentest finding, Diwali sprint delivers at plan |
| **R0-Core** — complete journey, all four controls, narrowed pilot width — deployed and certified | 2026-12-16 | **72%** | X-1 AWS by 20 Sep · X-2 pentest slot by 4 Oct · X-3 signatures by 28 Aug · levers L1–L3 available |
| **R0-Core live and selling** | 2027-01-01 | **72%** | As above, plus a dry run that produces no binding veto |
| **R0-Minimum** — one branch, 2 RMs, manual ops, all controls intact | 2027-01-01 | **85%** | Levers L1–L6 pulled as needed |
| **Any go-live on 2027-01-01** | 2027-01-01 | **< 20%** | **If X-1 (AWS landing zone) slips past 2026-10-05** |

### 1.1 The confidence explained rather than asserted

**Why not 90%?** Three external dependencies with zero float sit on the critical path
([03 §5](./03-DEPENDENCY-AND-PARALLELISATION-MAP.md#5-the-critical-path)), and there is **no buffer
sprint** in a 17-week window carrying 29 weeks of sequential work.

**Why not 40%?** Four things genuinely reduce the risk below what the raw arithmetic suggests:

1. **Requirements are ahead of code.** 60 G/W/T criteria, 38 consent rules, 48 suitability rules,
   an 18-screen inventory mapped to requirement IDs. Contract-first parallelisation is available
   *because that work was already done properly.* Most compressed programmes cannot do this.
2. **The foundation is further along than the position assessment's baseline.** Three CI workflows
   exist, SCA findings are cleared, ArchUnit is enforced, and the Flutter RM app exists with
   suitability-gate, consent-gate, device-isolation and PII-redaction tests already written.
3. **The architecture is sound and reviewed.** S07 is 🟢. We are building to a blueprint, not
   discovering one.
4. **The scope is genuinely thin.** One insurer, one product, one segment, one channel. Rajal has
   already excluded fourteen categories of work with named revisit triggers.

**Why 72% and not 60%?** Because levers L1–L6 exist, are pre-agreed with the authorities who own
them, and preserve every non-negotiable. A plan with rehearsed descope options is materially more
likely to land than one that discovers them in December.

### 1.2 Sensitivity — what each slip costs

| If this slips | Confidence for 1 Jan becomes | Recovery |
|---|---:|---|
| X-3 signatures slip 1 week (to 4 Sep) | 65% | Absorbed — S11 starts Sprint 3 regardless; SQ-2 pre-builds non-gated scaffolding |
| X-3 signatures slip 3 weeks (to 18 Sep) | 45% | Pull L1 immediately |
| X-1 AWS lands 4 Oct (2 weeks late) | 55% | Compress S09 into Sprint 3–4; pull L3 |
| **X-1 AWS lands after 5 Oct** | **< 20%** | **No recovery. Escalate to sponsor for a date change** |
| X-2 pentest slot slips to November | 50% | Two-vendor strategy; parallel internal assessment. **Deepali's control does not weaken** |
| X-8 AU Bank PG slips 2 weeks | 55% | Sandbox PG to Sprint 6; real PG certification becomes a dry-run Phase A item |
| Diwali sprint delivers at 60% not 80% | 62% | Pull L5 and L6 |
| Pentest returns ≥ 3 high findings | 55% | Sprint 8 becomes remediation-only; pull L4 |

---

## 2. Risk register additions this plan generates

To be recorded in [`registers/RISK-REGISTER.md`](../governance/registers/RISK-REGISTER.md) on
adoption. Scoring `Exposure = Likelihood × Impact`, each 1–3.

| ID | Risk statement | WS | L | I | Exp | Owner | Response | Escalation trigger |
|---|---|---|---:|---:|---:|---|---|---|
| **RISK-012** | If the AWS landing zone is not delivered by 2026-09-20, then S09 cannot execute, causing loss of the 2027-01-01 go-live with no engineering recovery | WS-3 | 3 | 3 | **9** | Shivanshi | MITIGATE — submitted Sprint 0; weekly named chase; sponsor escalation at first slip | **Escalate immediately — exposure 9 goes to PO and Architect on sight** |
| **RISK-013** | If `S02-G3`/`S02-G4` are not signed by 2026-08-28, then S11 (50 stories, the whole business case) cannot start, causing an unrecoverable 2+ week loss | WS-3 | 2 | 3 | **6** | Shailja | MITIGATE — `DL0`; convene within 1 working day of the required-by date | Date passes without signature or `CHANGES_REQUIRED` |
| **RISK-014** | If the pentest is not commissioned by 2026-10-04, then no independent security evidence exists, causing Deepali to block go-live | WS-3 | 2 | 3 | **6** | Deepali | MITIGATE — RFQ Sprint 0; two-vendor strategy; internal pre-assessment Sprint 5 | Vendor slot unconfirmed on 2026-10-04 |
| **RISK-015** | If the window carries no buffer sprint, then any single 2-week slip propagates directly to the go-live date | WS-3 | 3 | 2 | **6** | Kalpana | MITIGATE — levers L1–L6 pre-agreed and rehearsed; weekly forecast recalculation | Any milestone misses by > 3 days |
| **RISK-016** | If Sprint 6 (Diwali) under-delivers, then `GATE-S11` slips into the certification window, compressing S12 below viable | WS-3 | 2 | 3 | **6** | Kalpana | MITIGATE — Sprint 6 planned at 80%; scope is closure not build; heavy work in S4/S5 | Sprint 6 burn-down below 60% at mid-sprint |
| **RISK-017** | If the bank year-end change freeze is not exempted, then the dry run becomes read-only observation, causing Phase C destructive rehearsal to be lost | WS-3 | 2 | 3 | **6** | Kalpana | MITIGATE — exemption requested Sprint 0; fallback moves Phase C to 20–24 Dec | Exemption not granted by 2026-09-20 |
| **RISK-018** | If T4 human sign-off slots are not booked in Sprint 0, then senior calendars in mid-December make the signatures unobtainable, causing go-live to slip on administration rather than on readiness | WS-3 | 2 | 3 | **6** | Kalpana | MITIGATE — slots booked 2026-08-21 for 9–11 Dec | Any slot unconfirmed by 2026-08-28 |
| **RISK-019** | If five new deployable services enter production with an operations team that has never run them, then the first real incident is also the first rehearsal | WS-3 | 2 | 3 | **6** | Shivanshi | MITIGATE — dry-run Phase C is the rehearsal; on-call live before 1 Jan; incident simulation D12 | ORR incomplete at 2026-12-11 |
| **RISK-020** | If SQ-1 is under-staffed, then 35% of the backlog and the entire critical path are single-threaded on one squad | WS-3 | 2 | 3 | **6** | Kalpana + Amit | MITIGATE — SQ-1 staffed first and protected from reassignment through Sprint 8 | Any SQ-1 member reassigned |

**Existing risks re-dated.** RISK-005 (AWS Secrets Manager stub / TD-006) closes in Sprint 2 via
`S09-E04-S01`. RISK-004 (in-memory idempotency, TD-010) **escalates**: this plan runs multiple
instances, so its trigger — *"any plan to run > 1 instance"* — has fired. Redis idempotency moves
into Sprint 2 with the scale-out ADR (`DEP-006`), rather than remaining a Phase 5.4 deferral.

---

## 3. Assumptions this plan rests on

To be recorded in [`registers/ASSUMPTION-REGISTER.md`](../governance/registers/ASSUMPTION-REGISTER.md).
An assumption that is never validated is a risk wearing a disguise.

| ID | Assumption | Validated by | When | If false |
|---|---|---|---|---|
| ASM-009 | Six squads can be staffed with people who already know this domain and codebase | Amit + Kalpana | Sprint 0 | Onboarding cost lands in Sprint 1–2; pull L1 |
| ASM-010 | The bank's AWS landing-zone process takes ≤ 4 weeks, not 8 | Shivanshi | Sprint 1 | RISK-012 realises; date is lost |
| ASM-011 | 1SB sandbox is stable enough for daily E2E (RISK-009 does not dominate) | Amit | Sprint 2 | Gated nightly fallback, already sanctioned |
| ASM-012 | Group A insurer and product commercial values arrive by 2026-10-04 | Bancassurance | Sprint 4 | Catalogue cannot be seeded; **cannot go live on placeholder values** |
| ASM-013 | Pilot RMs are released from BAU for usability sessions and pilot selling | Sales Head | Sprint 5 | Usability evidence unobtainable; `S05-G6` fails |
| ASM-014 | Retrofit penalty on 20,200 existing lines is absorbed within Sprint 2 | Amit + Swapnali | Sprint 2 | Coverage gate slips; `GATE-S08` slips; everything slips |
| ASM-015 | Five thin services can be built to production quality in 4 sprints | Amit + Mahesh | Sprint 6 | Pull L5/L6; reduce to modules behind the BFF |
| ASM-016 | Q4 tax-season peak multiplier is known well enough to load-test against | Rajal + Shivanshi | Sprint 7 | Load test targets a guessed number; NFR evidence is weak |

---

## 4. Descope levers — in the order they get pulled

**The governing rule agreed by every authority in the session:**

> If the date is at risk, we cut **scope width**. We do not cut the
> [non-negotiables](./00-STAKEHOLDER-BRAINSTORMING-SESSION.md#52-agreed-non-negotiables--the-list-that-does-not-move-for-the-date).
> Any proposal to cut from that list is an escalation to the accountable human risk owner, not a
> delivery decision.

Levers are ordered by **damage to the business case, ascending**. Pull L1 before L2, and so on.
Each requires **Rajal's authority** — Kalpana supplies the timeline evidence and the recommendation.

### L1 — Narrow the pilot cohort · *no journey impact*

**Action:** go live with **1 branch and 2–5 RMs** instead of the full cohort.
**Buys:** ~1 sprint of certification effort — the data-variant suite (`S12-E01-S03`), UAT
coordination and business readiness all shrink proportionally.
**Costs:** slower business evidence. Widening happens under a feature flag **during** the pilot, so
no release is required.
**Pull when:** any `DL0` dependency slips by more than one week.
**Authority:** Rajal. **Preserves:** everything.

### L2 — Defer management oversight dashboards to R1 · *no journey impact*

**Action:** drop `S11-E11` (4 stories). Branch/regional/sales/business-head views become a weekly
data export.
**Buys:** ~4 stories in the Diwali sprint, where capacity is worst.
**Costs:** management sees the pilot through a spreadsheet for 6 weeks. **`S11-VT-16` role
separation must still be enforced at the API layer** — the *test* is not descoped, only the UI.
**Pull when:** Sprint 6 burn-down is below 60% at mid-sprint.
**Authority:** Rajal. **Preserves:** all controls, the journey, the audit trail.

### L3 — Defer the insurer representative portal to R1 · *no journey impact*

**Action:** drop `S11-E09` (3 stories). Insurer representatives work by email and phone during the
pilot, as they do today.
**Buys:** ~3 stories, plus the partner-scope authorization complexity in `S11-E08-S03`.
**Costs:** manual insurer interaction for underwriting requirements. At pilot volume (20–50 cases)
this is entirely workable.
**Pull when:** Sprint 6 is at risk, or X-5 (1SB commercials) slips.
**Authority:** Rajal. **Preserves:** everything.

### L4 — Reduce the catalogue to one product · *minimal journey impact*

**Action:** one Group A insurer, **one** Term product variant. Multi-quote comparison presents one
option with the comparison basis still disclosed.
**Buys:** the entire data-variant certification surface, and removes dependence on X-6 arriving
complete.
**Costs:** "multi-quote compare" — a headline capability — is not demonstrated at go-live.
**Rajal's reservation:** this cuts closer to the value proposition than L1–L3. Recommended only if
X-6 is genuinely unavailable.
**Pull when:** X-6 unresolved at 2026-10-18, or pentest remediation consumes Sprint 8.
**Authority:** Rajal. **Preserves:** all four controls, the complete journey, `Sold` definition.

### L5 — Operations console becomes a runbook · *operational impact*

**Action:** drop the `S11-E10` UI (4 stories). Operations works the exception queue via database
views, published runbooks and a shared tracker.
**Buys:** ~4 stories in Sprint 6.
**Costs:** higher operational toil; slower exception resolution. Acceptable at pilot volume, **not
acceptable at R1 scale.** Carries a mandatory R1 commitment.
**Shivanshi's condition:** exception *visibility* — alerting and the queue itself — is **not**
descoped. Only the console is. An invisible exception queue is a `O1` operational risk.
**Pull when:** Sprint 6 is failing and L2/L3 were insufficient.
**Authority:** Rajal, with Shivanshi consulted.

### L6 — Manual compliance verification for the pilot period · *the last lever*

**Action:** the four controls remain **fully enforced in code**. What becomes manual is the
*evidence assembly and sampling* — Compliance reviews every case before issuance rather than
relying on automated evidence reporting.
**Buys:** the reporting and dashboard work in `S11-E07-S03/S04` and part of `S12-E03-S06`.
**Costs:** Compliance carries a real daily workload through hypercare. Viable at 20–50 cases;
**not viable beyond it.**
**Shailja's condition, stated in the session:** *this is a lever on **how evidence is assembled**,
never on **whether the control operates**.* C1–C4 stay in code, at 100% coverage, enforced. If
anyone proposes this lever as a way to ship without a control, the answer is no, and it is a
Board 6 block.
**Pull when:** nothing else remains and the alternative is a no-go.
**Authority:** **Rajal + Shailja jointly.** This is the only lever requiring two signatures.

### 4.1 The levers laid against the calendar

| Decision point | Date | Levers available | Decided by |
|---|---|---|---|
| X-3 signature check | 2026-08-28 | L1 | Rajal |
| Sprint 2 close / `GATE-S08` | 2026-09-20 | L1, L2 | Rajal |
| **X-1 AWS check — the hard one** | **2026-10-05** | **L1–L4, or escalate for a date change** | **Rajal + sponsor** |
| Sprint 4 close / C1–C4 complete | 2026-10-18 | L1–L4 | Rajal |
| Sprint 6 mid-sprint burn-down | 2026-11-09 | L2, L3, L5 | Rajal |
| `GATE-S11` | 2026-11-15 | L4, L5 | Rajal |
| Pentest findings returned | 2026-11-29 | L4, L6 | Rajal (+ Shailja for L6) |
| Go/no-go | 2026-12-11 | GO-NARROW / GO-OBSERVE | All authorities |
| Dry-run final | 2026-12-31 | GO / GO-NARROW / NO-GO | All authorities |

---

## 5. What is not a lever

Restated because in the final three weeks of a compressed programme these get proposed, every time,
by someone acting in good faith:

| Proposed | Why it is refused | Who refuses |
|---|---|---|
| "Ship without the pentest, do it in January" | It is the control, not a report about the control. Regulated financial application handling PAN, Aadhaar and health data | **Deepali — binding** |
| "Suitability gate as a warning, not a 403" | Bypassing suitability before quote is described in our own requirement baseline as illegal | **Shailja — binding** |
| "Consent by RM attestation instead of customer OTP" | `never` constraint: *consent recorded without a verified customer-device OTP* | **Shailja — binding** |
| "Payment link into the RM session, just for the pilot" | `never` constraint: *payment executed on an RM or bank-employee device* | **Shailja + Deepali — binding** |
| "Lower the compliance-gate coverage from 100% to 80%" | The four gates are the non-bypassable set. `Q0` applies | **Swapnali — QA jurisdiction** |
| "Skip the DR test, we have the configuration" | A DR configuration that has never been exercised is a document | **Shivanshi — Board 7** |
| "Skip the restore test, backups are running" | Backup is a configuration. **Restore is a fact** | **Aarti** |
| "Mark `Sold` on payment success, reconcile later" | `never` constraint: *Policy Sold inferred from quote, proposal or payment alone.* Availability does not outrank financial correctness | **Rajal + Shailja** |
| "An AI persona signs the T4 review" | AI simulation cannot satisfy a mandatory human sign-off | **All — AIGEM** |
| "Run production out of region to save landing-zone time" | Regulated data outside AWS India regions is a `never` | **Shailja — binding** |
| "Add developers to recover the AWS delay" | Adding people to a single-threaded external bottleneck does not shorten the critical path | **Kalpana** |

---

## 6. Kalpana's statement of the forecast

Per the delivery authority boundary — **accountable for the date and the truthful forecast, holding
no decision right over any specialist domain.**

> This plan meets 2027-01-01. It meets it with **zero float on three external dependencies I do not
> control**, which is why I am publishing **72%** and not 90%, and why I have written the descope
> levers before we need them rather than after.
>
> I will publish a required-by date for every critical-path decision, mark it `OVERDUE` on the
> register the day it passes, convene the owning authority inside the `DL` window, and escalate to
> the accountable human. At no point will I supply the content of a decision that belongs to
> someone else, record a decision an owner has not given, or convert a `DECISION-BLOCKED` into a
> green forecast.
>
> **The single thing I would say to the sponsor if I had one sentence:** the AWS landing zone
> request must be submitted this week, because it is the one item on this plan where no amount of
> money, people or effort applied in November recovers a week lost in August.

---

## 7. Weekly forecast format

Published every Friday by Kalpana. Fixed format, so trend is readable at a glance.

```yaml
forecast_week: 2026-XX-XX
target_date: 2027-01-01
confidence: NN%            # recalculated from real progress, not restated
confidence_delta: +/-N%    # vs last week, with the reason

top_three_date_movers:
  - item: X-1 AWS landing zone
    owner: Shivanshi
    required_by: 2026-09-20
    state: IN_PROGRESS | OVERDUE | RESOLVED
    days_of_slack: N

gates:
  next: GATE-S08
  target: 2026-09-20
  criteria_evidenced: N/10
  state: OPEN | CANDIDATE | PASSED

blocked_decisions:          # never averaged into the confidence above
  - id: DEC-xxx
    owner: <named authority>
    overdue_days: N
    escalated_to: <named human>

levers_pulled: []           # with the date and the Rajal decision reference
levers_recommended: []      # Kalpana recommends; Rajal decides
```
