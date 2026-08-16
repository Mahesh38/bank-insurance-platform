# S13 — Expansion & Scale

**AIGEM stage:** L8 — Expansion · **Owner:** Rajal (Product) + Mahesh (Architecture)
**Central question:** *Does it generalise without rework?*

---

## 1. Purpose

Add the second, third and fourth cases — LOBs, channels, insurers, segments — **reusing the
orchestration proven at S11 without changing it**.

This stage is the test of whether S06 and S07 were right. If adding Health requires rewriting the
quote orchestration, the abstraction was wrong, and the cheapest moment to discover that is now,
with one journey in production, rather than after five.

> **The measure of success is a diff.** Adding an LOB should touch LOB-specific handlers,
> configuration and tests — and leave the orchestration untouched. If the diff spreads into shared
> code, stop and reassess the abstraction rather than pushing through.

## 2. Entry criteria

- [ ] GATE-S12 passed: the first journey is certified
- [ ] The first journey is live or in pilot, with real usage data
- [ ] Regression suite covering the first journey exists and is green

## 3. Epics and stories

### S13-E01 — Expansion readiness · *Mahesh*

| ID | Story | Acceptance criteria |
|---|---|---|
| S13-E01-S01 | Assess the abstraction against the first real second case | Named extension points; nothing shared requires modification |
| S13-E01-S02 | Publish the expansion pattern guide | A step-by-step for adding an LOB, channel or insurer |
| S13-E01-S03 | Define the reuse fitness function | An automated check that expansion has not modified orchestration |
| S13-E01-S04 | Establish the pre-expansion regression baseline | Green, recorded, and the bar every expansion must clear |

### S13-E02 — LOB expansion · *Rajal + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S13-E02-S01 | Specify the per-LOB delta | Products, fields, rules, documents, underwriting differences |
| S13-E02-S02 | Add Health | Schemas and handlers only; orchestration unchanged; full journey green |
| S13-E02-S03 | Add Motor | Same constraint; motor-specific vehicle and RTO data handled in the LOB layer |
| S13-E02-S04 | Verify no regression to prior LOBs | Full regression of every previously shipped journey, green |
| S13-E02-S05 | Verify per-LOB compliance controls | C1–C10 hold for each new LOB; suitability rules are LOB-specific and must be re-certified |

### S13-E03 — Channel expansion · *Rajal + Digital*

| ID | Story | Acceptance criteria |
|---|---|---|
| S13-E03-S01 | Add the DIY self-service journey | Customer authentication via digital banking SSO; self-consent per the S02 pack |
| S13-E03-S02 | Add the hybrid journey | RM-initiated, customer-completed, with attribution preserved across the hand-off |
| S13-E03-S03 | Verify attribution across channels | Distributor and SP attribution correct in every channel variant |
| S13-E03-S04 | Verify compliance gates per channel | Suitability and consent enforced identically regardless of channel |

### S13-E04 — Insurer and catalogue expansion · *Rajal + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S13-E04-S01 | Add further Group A insurers | Configuration and catalogue, not code, wherever possible |
| S13-E04-S02 | Implement multi-insurer comparison | Ranking and display per the S03 rules; basis disclosed |
| S13-E04-S03 | Implement the Group B controlled redirect | Attribution and audit preserved through the redirect — the legacy model's exact failure, not repeated |
| S13-E04-S04 | Build catalogue administration | Product matrix maintainable by business users, with maker-checker |

### S13-E05 — Scale readiness · *Shivanshi + Aarti*

| ID | Story | Acceptance criteria |
|---|---|---|
| S13-E05-S01 | Implement distributed idempotency and job ownership | Multi-instance safe before scale-out (closes TD-010) |
| S13-E05-S02 | Verify performance at expanded scope | Load test with all LOBs and channels active |
| S13-E05-S03 | Address data growth | Partitioning, archival and index strategy against real volumes |
| S13-E05-S04 | Verify provider capacity | Aggregate rate limits across all LOBs stay within contracted limits |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S13-VT-01 | Expansion did not modify orchestration | Diff review + fitness function | No change to shared orchestration |
| S13-VT-02 | Prior journeys still work | Full regression of every shipped journey | 100% green |
| S13-VT-03 | Compliance holds per LOB and channel | Control suite per variant | 100% pass for every combination |
| S13-VT-04 | Attribution survives every path | Trace attribution through each channel including Group B redirect | Correct in every case |
| S13-VT-05 | Performance holds at expanded scope | Load test | Within NFR; no degradation of the S12 baseline |
| S13-VT-06 | Multi-instance operation is safe | Run with N instances; concurrent identical operations | No duplicate job, no double effect |
| S13-VT-07 | Expansion is genuinely cheap | Measure effort for LOB 2 vs LOB 3 | Declining, and consistent with the pattern guide |

## 5. Exit gate — GATE-S13

| # | Criterion | Level | Evidence artefact |
|---|---|---|---|
| S13-G1 | Each new LOB's journey green end to end | E4 | CI runs per LOB |
| S13-G2 | Orchestration unchanged by expansion | E4 | Diff + fitness function result |
| S13-G3 | Full regression of all shipped journeys green | E4 | CI run |
| S13-G4 | Compliance controls certified per LOB and channel | E4 | Control results + Compliance sign-off |
| S13-G5 | Attribution correct across all channels including redirect | E4 | Test results |
| S13-G6 | Performance within NFR at expanded scope | E3 | Load test report |
| S13-G7 | Multi-instance safety proven; TD-010 closed | E4 | Concurrency test results |
| S13-G8 | Expansion pattern guide published and used | E1 | Guide + evidence it was followed |

**Approvers:** Rajal (AP) · Mahesh (AP) · Amit (AP) · Swapnali (AP, B) · Deepali (AP, B) ·
Shailja (AP) · Shivanshi (AP) · Kalpana (AP) · Aarti (RV)

## 6. Current position in this repository — ⚪ Not started

Queued as WS-1 Phase 5 (Health → Motor), with its gate criteria already drafted in
`04-STAGE_GATES.md` — criteria 5.1 through 5.6, including the requirement that Health reuse
`QuoteService` orchestration unchanged. That criterion is exactly right and is the correct test of
the abstraction.

**Recommendation: do not start.** Under the realignment, Phase 5 is held until S08, S09 and a real
S11 slice are complete. The reasons are cumulative:

1. **There is no regression suite** to protect Term while Health is added. S13's entry criteria
   cannot be met.
2. **The compliance gates are absent.** Adding Health and Motor to a quote path with no suitability
   gate replicates a regulatory defect across three lines of business instead of one.
3. **Expansion tests an abstraction against a journey that does not exist.** Reusing `QuoteService`
   unchanged proves little when the orchestration it belongs to has no lead, consent, payment or
   issuance around it.
4. **Per-LOB suitability rules do not exist.** Health suitability differs materially from Term
   suitability, and neither rule pack has been written (GAP-007).

Health and Motor are genuinely valuable and genuinely next. They are next *after* the foundation
and the first real slice, and the value of doing them then is substantially higher than doing them
now.

## 7. Premature at this stage

Multi-region · unrelated architectural rewrites · optimisation without measurement · new product
categories outside the approved panel.

The characteristic S13 failure is rearchitecting the thing that works, because expansion exposes
its imperfections. Note them as debt; do not rebuild the only proven journey while adding to it.
