# Sign-off packet — GAP-006 (consent) and GAP-007 (suitability)

**Status:** AI-DRAFTED 2026-10-21 for the human Risk & Compliance signatory (Board 6, T4).
**Signatures below are blank on purpose.** Nobody in this repository — agent or owner — can sign
them on Compliance's behalf: [`DEC-20260816-06`](../../governance/registers/DECISION-REGISTER.md)
makes GAP-006/007 closure non-waivable "by any authority including Product". This packet makes
that signature a short, well-scoped act, and makes every *later* compliance change a configuration
approval instead of a code change.

## 1. What closes the gaps

| Gap | Gate | Closes when | Today |
|---|---|---|---|
| [GAP-006](../po-drive/02-GAP-REGISTER.md) consent rules not executable | S02-G3 | Human Compliance signs the consent rule pack **and** the configuration below | Content complete; rules now executable as configuration |
| [GAP-007](../po-drive/02-GAP-REGISTER.md) suitability content & override | S02-G4 | Human Compliance signs the suitability rule pack **and** the configuration below | Content complete; rules now executable as configuration |

## 2. What changed since the packs were drafted (2026-08-16)

| Change | Source | Why Compliance must look |
|---|---|---|
| Rules are **configuration** (rule sets `CONSENT.POLICY`, `SUITABILITY.LIFE.TERM/SAVINGS/ULIP`, `SUITABILITY.GATE`), evaluated by one engine; precedence, thresholds, reason codes, validity periods and wording are configuration | [`ADR-024`](../../platform/architecture-review/ADR-024-configuration-driven-rule-engine.md) | Compliance approves **rule-set versions** from now on — a change you ask for tomorrow is a new version you approve, published without a release |
| Suitability question sets **per product class** — Term asks no objective, horizon or risk attitude | `D-022`, [suitability pack §2.3](./suitability-rule-pack.md#23-question-sets-by-product-class-v11-amendment--d-022) | Confirms the IRDAI suitability obligation is met per class |
| Consent captured **after suitability, before multi-quote**, reused for **90 days** (configurable) | `D-024`, [decisions §D5](../../platform/architecture-review/DECISIONS-2026-10-21.md#d5--consent-timing-and-validity-d-024) | The packs placed `CNS-DP`/`CNS-SOL` *before* the questionnaire. Under the DPDP Act 2023 the questionnaire processes personal data. **Permissibility of the owner's order is the main question for you.** If not permissible, the fix is one configuration change (move both purposes to `BEFORE_SUITABILITY`) |
| New purpose `CNS-QSH` — share quote inputs with the insurers quoted through 1SB | `D-024` | New statement wording needed |
| Outcome record per product class; quote gate reads `SUITABILITY.GATE.allowedOutcomes` | Decision D4 in [decisions §D4](../../platform/architecture-review/DECISIONS-2026-10-21.md#d4--suitability-outcome-record-and-the-savings--ulip-model-explained) | Confirms which outcomes may proceed and with what disclosure |

## 3. What you are asked to sign

Sign **content**, not code. Each row is one approval.

| # | Item | Where it is reviewed | Decision asked |
|---|---|---|---|
| 1 | Consent rule pack v1 (38 rules) as amended by `D-024` | [consent-rule-pack.md](./consent-rule-pack.md) | Approve / amend |
| 2 | `CONSENT.POLICY` v1.0.0: checkpoints, purposes per checkpoint, validity days (90 / 30 / none), reuse, binding to `customerId` | Admin-config simulation; seed in `libs/bank-rule-engine` resources | Approve version |
| 3 | Statement wording for `CNS-DP`, `CNS-SOL`, `CNS-QSH`, `CNS-SHR`, `CNS-COM` (English + regional) | Consent statements | Approve wording |
| 4 | Suitability rule pack v1 (48 rules) as amended by `D-022` | [suitability-rule-pack.md](./suitability-rule-pack.md) | Approve / amend |
| 5 | `SUITABILITY.LIFE.TERM` / `.SAVINGS` / `.ULIP` v1.0.0: bands, factors, formulas, conditions, precedence, reason codes, RM / customer texts | Admin-config simulation with test cases `SUIT-TC-REF-01` and §6.2 matrix | Approve each version |
| 6 | `SUITABILITY.GATE` v1.0.0: outcomes that may quote, caution acknowledgement, evaluation validity | Same | Approve version |
| 7 | `PROPOSAL.TAT` v1.0.0: insurer decision within **7 days** (IRDAI Master Circular on Protection of Policyholders' Interests, 5 Sep 2024) and its start event | Same | Confirm figure and start event |

## 4. Open Compliance questions (answer = configuration, not code)

| Id | Question | If the answer differs from the seed |
|---|---|---|
| `OPEN-CNS-DPDP` | May suitability run before `CNS-DP`/`CNS-SOL` are captured (owner order)? | Move purposes to `BEFORE_SUITABILITY` in `CONSENT.POLICY` |
| `OPEN-CNS-01` | May `CNS-DP` and `CNS-SOL` share one acknowledgement action? | UI flag in `CONSENT.POLICY` (`separateActions: true`) |
| `CNS-R13` | Must the customer enter the OTP on their own device, or may the customer read it to the RM? | `CONSENT.POLICY.captureMode` |
| `OPEN-SUIT-06` | Savings / ULIP: rule-pack arithmetic model or the BRD insurer-wise grid? | New rule-set versions (`tables` + `lookup`) |
| `OPEN-SUI-IMMUTABLE` | Edit after completion = new superseding evaluation (seed) | — |
| TAT | 7 days; clock starts at proposal receipt or at payment? | `PROPOSAL.TAT.startEvent` |

## 5. How a future compliance change works

```text
Compliance asks for a change
  → config maker drafts rule-set version n+1 (with the test cases that prove it)
  → simulate against the regression suite (publish is refused if any case fails)
  → Compliance approver (role COMPLIANCE_APPROVER, ≠ maker) approves
  → published with an effective date; services pick it up within the cache TTL (≤ 60 s)
  → every later evaluation records ruleSetId + version; earlier journeys keep the version they started with where pinned
```

No release, no code review, full evidence.

## 6. Signatures

| Authority | Scope | Name | Decision | Date | Evidence ref |
|---|---|---|---|---|---|
| Risk & Compliance (Board 6) — **human, T4** | Items 1–7, §4 answers | | | | |
| Architecture (Board 1) — Mahesh | Gate placement at the quote API boundary; availability coupling (SUIT-R26) | | | | |
| Security (Board 4) | SUIT-R25/R26/R40, OTP handling | | | | |
| QA (Board 5) | Test cases executable; 100% branch coverage of the gate | | | | |
| Database | Append-only consent by grant | | | | |

When row 1 is signed, update [GAP-006/007](../po-drive/02-GAP-REGISTER.md) and the rule packs' ratification
tables; then S02-G3/G4 can be marked met by the gate owner (not by an agent).
