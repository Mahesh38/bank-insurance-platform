# CR-009 — Governance Flow Recalibration

**Change request:** CR-009
**Date raised:** 2026-08-14
**Status:** **APPROVED** 2026-08-14 — including A1 and B4
**Change type:** `GOV` — framework recalibration
**Runtime impact:** None. No application code, API contract or production configuration changes.
**AIGEM board count:** Unchanged — remains seven
**Persona roster:** Unchanged — remains nine, and is closed by this CR

---

## 1. Request

Recalibrate the AIGEM framework so that it governs delivery without displacing it. Ten changes
across five framework documents and two scripts. No board, persona, veto or jurisdiction is
removed, weakened or reassigned.

## 2. Problem being solved

The framework is well-constructed and internally consistent. `FreshnessCheck` reports `FRESH`,
`ci-checks.py` passes every schema, routing, link and calibration check, and all 24 freshness
fixtures pass. The problem is not correctness. It is **cost against throughput**.

Measured at the time this CR was raised:

| Signal | Value |
|---|---|
| GATE-P4 exit criteria closed | **0 of 7** — 5 `OPEN`, 2 `PARTIAL` |
| GATE-IAM-P1 exit criteria closed | **0 of 6** — all `OPEN` |
| Consecutive commits containing no product code | **61** |
| Last commit touching `services/` or `libs/` | **2026-08-10** (itself a docs restructure) |
| Documentation lines ÷ product code lines | **2.10** (42,151 / 20,070) |
| Governance + persona docs as a share of all docs | **53%** (22,427 lines) |
| Personas added between CR-002 and CR-008 | **6, in 7 days** |
| Items processed through the suggestion register | **1** |
| ADRs / EPICs / SPIKEs minted | **0 / 0 / 0** |
| Parked items | **7** — of which **1 (TD-014) has a fired unpark trigger awaiting sweep** |
| `FreshnessCheck` verdict throughout | **FRESH** |

Every mechanical check passed for the entire period in which nothing was delivered. That is the
finding. A 14-step pipeline, seven boards and nine personas — roughly 12,000 lines of framework
plus 22,000 lines of persona documentation — have processed one suggestion, while both delivery
gates stood at zero closed criteria.

Five specific mechanisms produce this:

**(a) T4 fires on subject matter, not on change.** `11 §3` escalated to T4 on any change
*touching* PII, authn/authz, secrets, cryptography, money movement, consent, migration or
production topology. WS-2 **is** an authentication and authorization platform; WS-1 moves KYC and
payments. Every change in both workstreams was therefore T4 by definition — seven boards and
three human sign-offs for a log-message edit. The T1–T3 rungs existed but were unreachable.

**(b) Governance work was exempt from governance.** `GOV` routed only to `docs/governance/**`
through change control. Change control asks *"is this change correct?"*; it never asks *"should
this week's capacity go to governance instead of the open gate?"*. Governance work never entered
the queue where that comparison happens, so 61 commits of it were never weighed against the
delivery they displaced. Each was individually correct. The aggregate was invisible.

**(c) Silence had no terminator.** *"A mandatory board did not respond → gate is not approved"* is
right, and had no clock attached. An unstaffed board could stop work indefinitely with nobody
named and nothing escalating.

**(d) Approvals expired on stage arithmetic.** *"Approval age > one stage → expired, re-run the
boards"* meant that whenever a gate stayed open longer than the work took, boards re-reviewed
plans nobody had changed, in a stage nobody had left — consuming the review capacity the gate
depends on.

**(e) Accountability without leverage.** R12 owns the date and the forecast and holds no decision
right anywhere. With no lever, the role can only document delay — which is what the delivery
record shows.

Two further defects were found that had not yet fired but would:

**(f) Binding-veto deadlock had no exit.** Deepali and Shailja both hold non-overridable `B`
authority, and their jurisdictions genuinely overlap on DB-side PII, encryption, consent,
retention and third-party sharing. Rules forbidding majority override, with no named tie-breaker,
leave a mutually-exclusive disagreement with no defined terminator.

**(g) Persona growth is O(n²) in protocols.** Nine personas admit 36 possible pairs; four
bilateral protocol documents already exist. Nothing in the framework could notice that adding
personas had become its dominant activity.

## 3. Proposed changes

| # | Change | File | Rule added |
|---|---|---|---|
| A1 | T4 triggers rewritten as a **change test** (G1–G10) with an explicit not-T4 list; ambiguity tiers down to T3 with single-board escalation up | `11-REVIEW_GATES.md` §3 | RG-5, RG-6 |
| A2 | `GOV` work is triaged, queued, backlogged and consumes the single in-flight slot; must name the gate criterion it defers | `08-BACKLOG_RULES.md` §3.1, `00-GOVERNANCE.md` §9, `CURRENT-STATE.yaml` | BR-4, GC-1 |
| A3 | Board response clock, tier-scaled, escalating to named humans; `NO_RESPONSE` is recorded but never counts as a verdict | `11-REVIEW_GATES.md` §12.1 | RG-7 |
| A4 | Approvals expire at **30 days or on changed context**, not on stage boundary; re-review scoped to boards whose inputs moved | `11-REVIEW_GATES.md` §14 | RG-8 |
| A5 | R12 may **force a decision to happen** — required-by dates, `OVERDUE`, convening, escalation — and may never supply its content | `PERSONA-AUTHORITY-MATRIX.md` §12 | PA-1 |
| B4 | Binding-veto deadlock resolution: separate outcome from mechanism, take the stricter position where coherent, then a **named human tie-breaker** by conflict class; overruled position preserved verbatim | `PERSONA-AUTHORITY-MATRIX.md` §15.1 | PA-2 |
| B3 | Persona roster **closed at nine**; four admission tests; no net growth; preferred alternatives ranked | `14-CHANGE_CONTROL.md` §1.1 | CC-2, CC-3 |
| C1 | **Gate criteria closed per week** becomes the headline metric; zero for two weeks ⇒ framework raises `INTERVENE` on itself; cost-of-governance metric block added | `18-GOVERNANCE_METRICS.md` §2 | GM-1 |
| C2 | CI reports the docs-to-code ratio — a report, never a gate | `ci-checks.py` | — |
| C3 | Freshness accepts a dated, attributed **review acknowledgement**; central review log so one sweep is one edit | `FreshnessCheck.java`, `REVIEW-LOG.md`, `RUNBOOK.md` §4.5.1 | FR-1 |

**B1 (name an Operations persona) was delivered by CR-008** and is not repeated here. **B2** from
the same review — a possible "Prajwal" persona — was withdrawn: the name does not appear anywhere
in the repository and was raised in error. No action.

## 4. What this CR does NOT change

Stated explicitly, because a recalibration of review load invites the assumption that rigour was
traded away:

- **No board is removed, merged, or made optional.** Seven boards, unchanged.
- **No veto is weakened.** Security and Risk & Compliance keep binding, non-overridable veto.
  B4 adds an exit from deadlock; it does not let either persona be outvoted.
- **No mandatory T4 human sign-off is relaxed.** An agent still cannot satisfy one, and RG-7
  explicitly bars `NO_RESPONSE` from doing so.
- **No jurisdiction moves.** Every cell of §4–§14 of the authority matrix is untouched. PA-1
  grants timing authority only, never content authority.
- **Silence still never approves.** A3 escalates a non-response; it never converts one to assent.
- **`ci-checks.py` still passes or fails on correctness.** C2 warns; it cannot block a merge.
- **Content checks are never suppressed.** C3 resets age only; ID uniqueness, counter and schema
  checks run regardless, and future-dated acknowledgements are ignored.

## 5. Driver

New evidence — measurement of the framework's own operating record, per
[16 §7](../16-DECISION_MODEL.md#7-revalidation-triggers) and the bypass-rate reading in
[18 §2](../18-GOVERNANCE_METRICS.md#2-governance-metrics): *"a rising bypass rate is a process
signal, not a discipline problem — fix the process, do not exhort people."*

The framework's own metrics document already anticipated this failure mode. It lacked the one
measurement that would have surfaced it, which C1 now adds.

## 6. Evidence

- `git log` — 61 consecutive commits with no `services/`/`libs/` change; last product-touching
  commit 2026-08-10.
- `CURRENT-STATE.yaml` — GATE-P4 at 0/7 closed; GATE-IAM-P1 at 0/6.
- `ci-checks.py [8]` — docs 42,151 / code 20,070 = 2.10; governance+personas 53% of docs.
- Registers — 1 suggestion, 7 parked, 0 ADR, 0 EPIC, 0 SPIKE. The parked backlog is the one
  register with real content, seeded from `TECH-DEBT.md` under GOV-003; it was not produced by
  pipeline throughput.
- CR-002 … CR-008 — six personas added in seven days.
- `11 §3` as previously written, read against WS-2's subject matter: 100% T4.

## 7. Impact

| Dimension | Assessment |
|---|---|
| Scope | None. No product scope changes. |
| Stage | No gate date moves directly. Expected effect is that criteria can begin closing. |
| Dependencies | None created or invalidated. |
| Parked items | None made eligible. |
| Effort | M — documentation and two scripts. |
| Risk if rejected | The framework continues to consume the delivery capacity it exists to protect, and the docs-to-code ratio continues to rise. Both gates stay shut. |
| Risk if approved | Tiering down could under-review a genuinely critical change. Mitigated by RG-6 (ambiguity tiers to T3, not T1) and by single-board escalation to T4 requiring no CR. |

## 8. Alternatives considered

| Option | Consequence |
|---|---|
| **Do nothing** | Rejected. Zero gate criteria closed across the measured period is not a variance; it is the steady state. |
| **Suspend AIGEM until the gates close** | Rejected. The framework's constraints are sound and the standing constraints are load-bearing. The problem is calibration, not existence. |
| **Lower every tier by one** | Rejected. Crude — it would under-review genuine T4 changes as readily as it would unblock trivial ones. A1 targets the misclassification instead. |
| **Add a Delivery/PMO persona to chase decisions** | Rejected, and it is the trap this CR closes. The tenth persona would add O(n) protocols to solve a problem that PA-1 solves with one paragraph of existing-role authority. |
| **Make governance work exempt but capped** | Rejected. A cap is a number to argue with; A2's queue makes the trade-off visible at the moment it is made. |

## 9. Ratification

**APPROVED 2026-08-14**, in full and including A1 and B4, by the repository owner acting in the
**Mahesh / Architect (R2)** framework-custodian authority, who also authorised the Product (R1)
and Delivery (R12) positions.

This CR was prepared by an AI agent on explicit user direction. Per **Rule CC-1** the agent raised
it and did not approve it — including the parts the agent believed were obviously correct. The
approval below is the human's.

| Approver | For | Recorded |
|---|---|---|
| **Mahesh** — Architect (R2), framework custodian | All ten changes | ☑ 2026-08-14 |
| **Rajal** — Product Owner (R1) | A2, A4, A5, B3, C1 | ☑ 2026-08-14, authorised by R2 |
| **Kalpana** — Delivery (R12) | A3, A5, C1, C3 | ☑ 2026-08-14, authorised by R2 |
| **Deepali** — Security (R8) | **A1**, **B4** | ☑ 2026-08-14, authorised by R2 — **see §9.1** |
| **Shailja S** — Risk & Compliance (R9) | **A1**, **B4** | ☑ 2026-08-14, authorised by R2 — **see §9.1** |

### 9.1 Provenance of the A1 / B4 approvals — read this before relying on them

This CR was raised stating that A1 and B4 must not merge without Deepali's and Shailja's
**explicit** verdicts, because A1 changes when their boards are convened and B4 changes what
happens when they disagree.

Those two approvals were given by the **same human** who holds the R2 Architecture authority,
authorising them on behalf of R8 and R9. They are **not** independently recorded Board 4 and
Board 6 verdicts, and this record does not represent them as such. Recording them as independent
sign-offs would be exactly the fabrication the framework forbids
([matrix §12](../PERSONA-AUTHORITY-MATRIX.md#kalpana--r12-is-not-authorised-to-independently)).

This is a legitimate ratification: an accountable human with authority over the framework
approved a framework change. It is recorded with its provenance visible so that a later reader —
or a regulator — can see precisely who signed what.

Two consequences follow, and neither is optional:

1. **This approval covers the framework text only.** It does not pre-approve any future change
   that A1 tiers down. Board 4 and Board 6 retain their full binding veto on every individual
   change, unchanged by this CR.
2. **If Deepali and Shailja review A1 and B4 independently**, their verdicts are appended here
   and this section is updated to reflect them. If either objects, the objection is treated as a
   verdict on a live rule — not as a re-litigation needing new evidence under
   [§6](../14-CHANGE_CONTROL.md#6-reversing-a-rejection) — and A1 or B4 is amended or reverted
   accordingly. **Their veto over these two clauses survives this approval.**

**Decision:** APPROVED
**Approvers:** ["Mahesh / Architect (R2), repository owner — including R1, R12, and R8/R9 per §9.1"]
**Decided on:** 2026-08-14
**Conditions:**
- Deepali (R8) and Shailja (R9) may record independent verdicts on A1 and B4 at any time; their
  veto over those two clauses is preserved (§9.1).
- First `INTERVENE` check under GM-1 falls due two weeks from ratification: **2026-08-28**.
