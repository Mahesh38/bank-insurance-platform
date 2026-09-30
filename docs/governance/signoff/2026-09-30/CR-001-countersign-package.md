# CR-001 — validation, counter-sign preparation, impact

**Change request:** add Phase 4 exit criterion 4.7 (coverage gates; QA-001 closed or waived).  
**AIGEM 2026-09-30 suggestion:** COUNTER-SIGN (Rajal / PO, Swapnali / QA).  
**Recorded decision:** **APPROVED 2026-08-10** (Mahesh). This note does not counter-sign.  
**signature_status:** `AI-DRAFTED — Ready for PO and QA counter-sign. No signature fabricated.`

## 1. Validation review note (RG-8)

Context is 51 days old. Re-checked 2026-09-30 against the documents 4.7 depends on:

| Dependency | 2026-08-10 position | 2026-09-30 position | Changed? |
|---|---|---|---|
| Phase 4 exit set | 4.1–4.6 plus proposed 4.7 | 4.7 is in `04-STAGE_GATES.md` and `GATE-EVIDENCE.yaml` | In force, as approved |
| QA-001 | P0 partial | **Closed 2026-09-13** — Phase-1 90/70; scaffold floor ratified; package floors → QA-014 (expiry 2026-10-31) | Yes — the original tension (P0 debt vs missing criterion) is resolved on the criterion side |
| Coverage gates | interim service floor | Executing on every PR; WS-1 4.7 is `MET` in GATE-EVIDENCE | Yes — evidence landed; counter-sign is still the missing human record |
| Standing constraint on coverage | libs 80/70 | Unchanged | No |

Nothing the 2026-08-10 approval depended on has been reversed. QA-001 closure **supports** the approval; it does not require a reversing CR.

## 2. Counter-sign preparation note

The decision is already in force. Withholding a counter-sign does not undo it. Sign only if you still agree with option (a) — approve 4.7 as written.

**Where to file:** the CR-001 row in [`DECISION-REGISTER.md` §3](../../registers/DECISION-REGISTER.md) and the `outstanding` block under "CR-001 — add Phase 4 exit criterion 4.7". There is no separate CR-001 file.

Suggested ticks (humans only):

- [ ] Rajal / Product — Counter-sign CR-001
- [ ] Swapnali / QA Lead — Counter-sign CR-001
- [ ] Decline, and raise a CR to reverse it — reason: …

## 3. Impact assessment

| If counter-signed | Phase 4 continues to require 4.7. Already MET in the evidence ledger; the signature closes the record-ambiguity AIGEM flagged. |
| If declined | A reversing CR is required. QA-001 is closed, so the original "downgrade QA-001" alternative is stale. |
| Runtime | None. Criterion and coverage gates already execute. |
| Gates that cite it | GATE-P4 4.7; S08-G3 (related coverage). |

**Ready for PO and QA counter-sign.**
