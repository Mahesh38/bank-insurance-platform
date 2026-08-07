# Suggestion Register

**Every input that could become work gets a row here — admitted, parked, rejected, or escalated.**
Nothing is dropped. Nothing is deleted.

**Owner:** whichever agent or person triaged the input
**Next ID:** see `next_ids.SUG` in [../state/CURRENT-STATE.yaml](../state/CURRENT-STATE.yaml)
**Rules:** [08-BACKLOG_RULES.md](../08-BACKLOG_RULES.md) · [09-AI_EXECUTION_RULES.md](../09-AI_EXECUTION_RULES.md)

---

## How to add a row

1. Claim the next `SUG-####` and increment `next_ids.SUG`.
2. Run pipeline steps 2–5 ([09 §2](../09-AI_EXECUTION_RULES.md#2-the-mandatory-sequence)).
3. Add the summary row below.
4. For anything beyond a trivial reject, add a detail block in §3 using
   [../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).
5. **Check for duplicates first** (Rule CS-2). A repeat is linked and increments
   `recurrence_count` — it is not a new row.

---

## 1. Status vocabulary

| Status | Meaning |
|--------|---------|
| `ADMITTED` | Entered a backlog for the current stage |
| `ADMIT-BYPASS` | Implemented under a human override of the process ([09 §8](../09-AI_EXECUTION_RULES.md#8-when-a-human-overrides-the-process)) |
| `PARKED` | Real work, later stage — see [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) |
| `ESCALATED` | Awaiting a human decision as `CR-###` |
| `REJECTED` | Will not be done; reason recorded |
| `DUPLICATE` | Already tracked; linked |
| `SUPERSEDED` | Overtaken by another decision; linked |
| `LAPSED` | Idea closed by aging (AS-3) |
| `CLOSED-DELIVERED` | Admitted and shipped; linked to the PR |

---

## 2. Register

| ID | Date | Source | Summary | SF | SC | Necessity | Type | P now / target | Action | Ref |
|----|------|--------|---------|----|----|-----------|------|----------------|--------|-----|
| — | — | — | *No suggestions triaged yet. AIGEM adopted 2026-08-07; triage begins with the next input.* | — | — | — | — | — | — | — |

<!--
Row format:
| SUG-0001 | 2026-08-08 | agent:claude | Redis-backed idempotency store | SF2 | SC0 | SHOULD | NFR | P4 / P2 | PARKED | [PARKED-BACKLOG](./PARKED-BACKLOG.md#sug-0001) |
-->

---

## 3. Detail records

Detail blocks live here for every non-trivial triage. Format:
[../templates/TRIAGE-RECORD.md](../templates/TRIAGE-RECORD.md).

<!-- ### SUG-0001 · <title>  ... full triage record ... -->

---

## 4. Seeded from existing artefacts

AIGEM was adopted mid-flight. Rather than backfilling every past decision
([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)), the
already-deferred items in [TECH-DEBT.md](../../1sb-insurance-integration/service-ssot/TECH-DEBT.md)
were seeded directly into [PARKED-BACKLOG.md](./PARKED-BACKLOG.md) as pre-existing parked work.
They keep their `TD-###` IDs; no `SUG-####` was minted retrospectively.

**Do not re-triage or re-report these** — they are known
([01 §6](../01-CURRENT_STATE.md#6-known-open-debt-affecting-triage)).
