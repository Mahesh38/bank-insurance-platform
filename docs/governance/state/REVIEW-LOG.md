# Governance Artefact Review Log

**Owner:** Kalpana — Delivery Head (R12), as part of register hygiene
**Read by:** `scripts/governance/FreshnessCheck.java`

---

## 1. What this file is for

Governance artefacts carry freshness limits: the suggestion register must be touched every 7
days, the stage gates every 14, and so on. Those limits exist for a good reason — a register
nobody has read in a month is not a source of truth, it is an archive.

But the check previously measured **the last edit**. If an owner re-read a register and correctly
concluded that nothing had changed, there was no way to say so. The only way to stay "fresh" was
to edit the file anyway.

That produces commits which look like progress and are not, and it quietly teaches owners to make
a token change instead of actually re-reading the artefact — the exact opposite of what the limit
is for.

**"Reviewed, nothing changed" is a real and common outcome.** This file is where it is recorded.

> **Rule FR-1 — Freshness means someone looked recently, not that someone typed recently.**

## 2. How to use it

After a register sweep, add one row per artefact you actually re-read. One sitting, one commit,
however many artefacts — which is how a sweep genuinely happens.

| Artefact | Reviewed | By | Outcome |
|---|---|---|---|
| _example_ `docs/governance/registers/RISK-REGISTER.md` | 2026-01-01 | Kalpana | no change — all 10 risks re-confirmed |

Rules:

- Only log an artefact you **actually re-read**. This file is an attestation, not a mute button —
  a false row is a governance breach, not a shortcut.
- Future dates are **ignored** by the checker. Backdating forward does not work.
- A review acknowledgement resets an artefact's **age** only. It never suppresses a **content**
  check — ID uniqueness, counter correctness and schema validation run regardless.
- If the review found something, fix it in the artefact. The edit refreshes the age by itself and
  needs no row here.

An in-file acknowledgement in the artefact's last few lines is also honoured, for a one-off:

```text
<!-- reviewed: 2026-08-14 by Kalpana -->
```

Prefer this log for sweeps — it is one edit rather than one per artefact.

## 3. Review log

| Artefact | Reviewed | By | Outcome |
|---|---|---|---|
| docs/governance/registers/PARKED-BACKLOG.md | 2026-08-14 | Claude (agent), for CR-009 | Re-read during the governance review. Empty; no parked items to age. Recorded as reviewed rather than edited, as the first use of this mechanism. |

---

## 4. What this file is not

It is not a substitute for doing the review, and it is not a place to keep an artefact nominally
fresh while it rots. A register whose only activity for two cycles is a row in this file is
telling you something — either it genuinely never changes and its limit is wrong, or it is not
being used and should be retired. Both are worth acting on; neither is fixed by another row here.
