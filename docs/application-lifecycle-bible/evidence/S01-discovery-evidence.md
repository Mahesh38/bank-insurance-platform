# S01 — Discovery & Capability Definition · Retroactive Stage Evidence

**Stage definition:** [`stages/S01-discovery.md`](../stages/S01-discovery.md)
**Workstream:** WS-3 ([charter](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md))
**Author:** Rajal — Principal Insurance Platform Product Owner (Board 3 / R1)
**Date:** 2026-08-16

**Retroactive stage verdict: `CLOSED-WITH-CONDITIONS`** — see §6.

> **This stage is confirmed, not redone.** [S01 §6](../stages/S01-discovery.md#6-current-position-in-this-repository---strong)
> states plainly: *"Do not redo this stage. Its output is sound. The realignment consumes it rather
> than repeating it."* This document is short because the work is already done. Padding it would
> imply a deficiency that does not exist.

---

## 1. What the stage requires

| # | Criterion | Level |
|---|---|---|
| S01-G1 | Capability map complete and prioritised | E1 |
| S01-G2 | Stakeholder catalogue with decision rights | E1 |
| S01-G3 | As-is and to-be journeys mapped with variants | E1 |
| S01-G4 | Glossary agreed and adopted | E2 |
| S01-G5 | Gap register established with severities that bind | E1 |
| S01-G6 | Discovery findings reviewed with business stakeholders | E2 |

**Approvers:** Rajal (AP) · Mahesh (RV) · Shailja (RV) · Swapnali (RV) · Kalpana (RV)

---

## 2. What exists — cited by path

| Criterion | Artefact | Path | Assessment |
|---|---|---|---|
| S01-G1 | Business capability map — layers, master catalogue, BR prefixes, ownership | [`knowledge-base/03-capability-map.md`](../../au-bank-insurance-platform/knowledge-base/03-capability-map.md) | 🟢 18 capabilities, each with a BR prefix that carries through to the BRD. Capability→domain→ownership all present |
| S01-G1 | Value stream and journeys | [`knowledge-base/04-value-stream-and-journeys.md`](../../au-bank-insurance-platform/knowledge-base/04-value-stream-and-journeys.md) | 🟢 |
| S01-G1 | Business process catalogue | [`knowledge-base/05-business-processes-catalogue.md`](../../au-bank-insurance-platform/knowledge-base/05-business-processes-catalogue.md) | 🟢 BP-xxx IDs |
| S01-G2 | Stakeholder catalogue | [`knowledge-base/06-stakeholders.md`](../../au-bank-insurance-platform/knowledge-base/06-stakeholders.md) | 🟢 |
| S01-G2 | Actors, hierarchies, decision rights | [`business-problem-statement.md §5`](../../context/business-problem-statement.md#5-operational-personas-organizational-hierarchies--identity-engine) | 🟢 Bank hierarchy RM→BM→CM→RM-regional→Zonal→National; insurer hierarchy; RBAC/ABAC decision rights sketched |
| S01-G2 | Stakeholder working session record | [`01-stakeholder-working-session.md`](../../au-bank-insurance-platform/01-stakeholder-working-session.md) | 🟢 |
| S01-G3 | As-is redirect journey with the visibility-loss point named | [`business-problem-statement.md §3.1`](../../context/business-problem-statement.md#31-as-is-state-au-beema-portal-redirect-model) | 🟢 The blind spot is identified precisely, not gestured at |
| S01-G3 | To-be journeys and process canvas | [`04-process-and-journey-canvas.md`](../../au-bank-insurance-platform/04-process-and-journey-canvas.md) | 🟢 CJ / RMJ / JRN identifiers |
| S01-G3 | Journey variants: assisted / DIY / hybrid, ETB / NTB, Group A / Group B | [`business-problem-statement.md §4`](../../context/business-problem-statement.md#4-customer-journeys--customer-segment-matrix) · [`WD §§2,3,5,6`](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) | 🟢 All three axes covered, with a comparison table per journey stage |
| S01-G4 | Glossary | [`knowledge-base/09-glossary.md`](../../au-bank-insurance-platform/knowledge-base/09-glossary.md) | 🟡 Present and adopted in practice; **no recorded Product + Architecture sign-off** (S01-G4 requires E2) |
| S01-G4 | Business-state vocabulary — what "sold" means | [`WD §4`](../../au-bank-insurance-platform/07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md#4-definition-of-policy-sold) · [D-007](../../au-bank-insurance-platform/DECISION-LOG.md) | 🟢 Four-condition definition, locked |
| S01-G5 | Gap register, severities, exit criteria, owners | [`po-drive/02-GAP-REGISTER.md`](../../au-bank-insurance-platform/po-drive/02-GAP-REGISTER.md) | 🟢 33 gaps, P0–P3, each with owner, impact and exit criterion |
| S01-G5 | Blocking rule stated | Same file, header: *"P0 block freeze"* | 🟢 stated · 🔴 **did not bind** — see §3 |
| S01-G6 | Discovery backlog of open questions | [`03-discovery-backlog.md`](../../au-bank-insurance-platform/03-discovery-backlog.md) | 🟢 |
| S01-G6 | PO assessment of discovery completeness | [`knowledge-base/10-gaps-and-po-assessment.md`](../../au-bank-insurance-platform/knowledge-base/10-gaps-and-po-assessment.md) | 🟢 |
| — | Source index for the knowledge base | [`knowledge-base/SOURCE-INDEX.md`](../../au-bank-insurance-platform/knowledge-base/SOURCE-INDEX.md) | 🟢 Rarer than it should be, and it is what makes the corpus auditable |
| — | Information model and rules | [`knowledge-base/07-information-model-and-rules.md`](../../au-bank-insurance-platform/knowledge-base/07-information-model-and-rules.md) | 🟡 Entities present, attributes not — that is GAP-016, an S03 item |

---

## 3. What was missing — and the one that matters

Two items. One is minor; one is the most instructive failure in this repository.

### 3.1 S01-VT-05 — the test this programme failed

> **S01-VT-05:** *For every P0 gap, name what it blocks and confirm that thing is actually blocked.*
> **Pass condition:** no P0 gap whose "blocked" work is proceeding.

GAP-006 (consent rules not executable) and GAP-007 (suitability content undefined) are both
labelled **P0 — block scope / build freeze**. Both remained open while the quote path was built,
delivered and moved to hardening.

**The register was correct. The severity was correct. Nothing enforced it.** A blocking rule that
lives only in a document header blocks nothing, and
[S01 §6](../stages/S01-discovery.md#6-current-position-in-this-repository---strong) says so:
*"The blocking rule exists on paper; it did not bind in practice."*

**Closure:** the rule is now given a mechanism, not more emphasis.

| Layer | Mechanism | Where |
|---|---|---|
| Model | [Rule SM-4](../02-STAGE-MODEL.md#54-freeze-semantics) — an open P0 business gap freezes entry to S11 and everything after it | 02-STAGE-MODEL |
| Workstream | WS-3 `entry_conditions`: S11 entry requires GAP-006 and GAP-007 `CLOSED` | [WS-3 charter §8](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md#8-the-s11-entry-condition) |
| Gate | Product verdict condition C5 on CR-010, binding and non-waivable | [board-3-product-rajal.md](../../governance/change-requests/CR-010/verdicts/board-3-product-rajal.md) |
| Content | Both packs now drafted and testable, so the gap is closable rather than perpetual | [consent](../../au-bank-insurance-platform/rule-packs/consent-rule-pack.md) · [suitability](../../au-bank-insurance-platform/rule-packs/suitability-rule-pack.md) |

Four layers, because the failure was not that anyone disagreed with the rule — it was that no
layer refused anything.

### 3.2 S01-G4 — glossary sign-off

The glossary exists and its terms are used consistently across the BRD, the knowledge base and the
code. S01-G4 requires **E2 — reviewed and signed** by Product and Architecture. No sign-off record
exists. This is a signature gap, not a content gap.

### 3.3 GAP-023 — self-service and hybrid journey detail

Thin at discovery, because at the time they were later-phase. [D-002](../../au-bank-insurance-platform/DECISION-LOG.md)
subsequently made all three journeys Day-1 scope, which retroactively raised the required depth.

**Product position:** this is now resolved by *scope*, not by more discovery. WS-3's R0 is
**assisted-first**; DIY revisits at R1 and hybrid at R2
([WS-3 charter §3.2](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md#32-out-of-scope-with-revisit-triggers)).
GAP-023 therefore stops blocking R0 and becomes an R1 entry condition. That is a deliberate Product
decision recorded as [DEC-20260816-04](../../governance/registers/DECISION-REGISTER.md), not a
quiet deferral.

---

## 4. New evidence added by this document

Little, deliberately.

1. **Confirmation with citations** (§2) — every S01 criterion mapped to a specific path, so a
   reviewer can verify the stage without re-reading the corpus.
2. **The S01-VT-05 closure mechanism** (§3.1) — four enforcement layers where there were zero.
3. **GAP-023 re-scoped** (§3.3) from a discovery gap blocking R0 to an R1 entry condition.
4. **Capability → R0 coverage check** below, which S01 does not require but S03 traceability
   consumes.

### 4.1 Capability coverage against R0

Walks [the capability map](../../au-bank-insurance-platform/knowledge-base/03-capability-map.md)
against [WS-3's R0 in-scope list](../../governance/workstreams/WS-3-PLATFORM-CHARTER.md#31-in-scope).
This is [S01-VT-01](../stages/S01-discovery.md#4-validation-tests) run against the actual release
slice.

| Capability | BR prefix | In R0 | Bounded context | Note |
|---|---|---|---|---|
| Customer Management | BR-CUST | ✅ thin | #4 | ETB lookup + prefill only |
| RM Workspace | BR-RM | ✅ thin | #2 | Pipeline list + resume |
| Lead Management | BR-LEAD | ✅ thin | #5 | Create / resume / status |
| Consent Management | BR-CONSENT | ✅ **full** | #6 | Rule pack drafted |
| Suitability & Recommendation | BR-SUIT | ✅ **full** | #7 | Rule pack drafted; hard-gate |
| Product Catalogue & Matrix | BR-PROD | ✅ thin | #8 | R0 matrix only — Life / Group A / Term |
| Quote Management | BR-QUOTE | ✅ | #10 + #15 | Largely built in WS-1 |
| Quote Comparison | BR-COMP | ✅ | #10 | Compare ≥ 2 offers |
| Proposal Management | BR-PROP | ✅ thin | #11 | |
| Underwriting Tracking | BR-UW | ✅ lite | #11 | Status refresh only |
| Payment Management | BR-PAY | ✅ | #12 | Customer device, AU Bank PG |
| Policy Issuance & Management | BR-POL | ✅ visibility | #13 | Policy number + status |
| Identity, Access, Audit | BR-SEC | ✅ | #3 (WS-2) + #16 | Consumed from WS-2 |
| Integration Hub | BR-INT | ✅ | #14 + #15 (WS-1) | Consumed from WS-1 |
| Reporting & Dashboards | BR-REP | ✅ minimal | #18 | Pilot funnel only |
| Notifications & Communication | BR-COMM | ✅ minimal | #17 | OTP + payment link only |
| Administration & Configuration | BR-ADMIN | ⛔ R1 | #19 | R0 seeds by config |
| Renewals & Servicing | BR-SERV | ⛔ R2+ | — | Out |

**No orphan capability and no orphan R0 item.** Sixteen of eighteen capabilities appear in R0, most
of them thin. Two are deliberately out with revisit triggers. That shape is correct for a vertical
slice: the slice is narrow in *depth*, not narrow in *breadth*, because a journey that skips a
capability is not a journey.

---

## 5. What remains genuinely open

| ID | Item | Criterion | Owner | Target | Evidence needed |
|---|---|---|---|---|---|
| S01-OPEN-01 | Glossary signed off by Product **and** Architecture | S01-G4 (E2) | Rajal + Mahesh | 2026-09-12 | Sign-off record on [`09-glossary.md`](../../au-bank-insurance-platform/knowledge-base/09-glossary.md) |
| S01-OPEN-02 | S01-VT-04: as-is journey reviewed with 3 practising RMs | S01-G6 | Rajal + Bancassurance | 2026-09-26 | Review record with attendees and corrections |
| S01-OPEN-03 | S01-VT-03: glossary disambiguation test (5 people, 10 terms, ≥ 90% agreement) | S01-G4 | Rajal | 2026-09-26 | Test record |
| GAP-023 | Self-service and hybrid journey detail | — | Rajal | **R1 entry**, not R0 | Journey maps at R1 depth |

None of these blocks the Foundation Recovery Increment or S11 entry. S01-OPEN-01 through -03 are
validation activities requiring real humans, which is exactly what makes them E2/E3 evidence and
exactly why an AI cannot manufacture them.

---

## 6. Retroactive stage verdict

> ## `CLOSED-WITH-CONDITIONS`

| Criterion | State | Basis |
|---|---|---|
| S01-G1 Capability map complete and prioritised | **MET** | [Capability map](../../au-bank-insurance-platform/knowledge-base/03-capability-map.md) + §4.1 R0 coverage |
| S01-G2 Stakeholder catalogue with decision rights | **MET** | [Stakeholders](../../au-bank-insurance-platform/knowledge-base/06-stakeholders.md) + [business problem statement §5](../../context/business-problem-statement.md#5-operational-personas-organizational-hierarchies--identity-engine) |
| S01-G3 As-is and to-be journeys with variants | **MET** | Journey canvas + all three variant axes |
| S01-G4 Glossary agreed and adopted | **PARTIAL** | Content present and in use; E2 signature absent. S01-OPEN-01 |
| S01-G5 Gap register with severities that bind | **MET** | Register present; the binding mechanism is now supplied (§3.1) — this criterion was substantively unmet until the four enforcement layers existed |
| S01-G6 Discovery reviewed with business stakeholders | **PARTIAL** | Working session recorded; the 3-RM as-is validation has not been run. S01-OPEN-02 |

**Assessment.** This is the best-executed stage in the programme and the verdict should not obscure
that. Four of six criteria are fully met on artefacts produced months ago. The two partials are
both **validation-with-real-humans** items, which no amount of documentation can substitute for.

**The single most important thing this stage teaches** is §3.1: discovery correctly identified two
P0 build-freeze gaps, and the programme built past them anyway. The defect was never in discovery.
It was in the absence of anything that could refuse.

**Conditions carried forward:** S01-OPEN-01, -02, -03. GAP-023 re-scoped to R1.

**Signature status:** `AI-DRAFTED — mandatory human signature outstanding`.
S01-G4 and S01-G6 require E2 human sign-off. Silence does not approve this stage.

— **Rajal**, Principal Insurance Platform Product Owner, 2026-08-16
