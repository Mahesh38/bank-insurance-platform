# CR-015 — Approval Package (AIGEM-ACCEPTED 2026-09-30)

**Change request:** [`CR-015-ws3-r0-savings-ulip-journey.md`](../../change-requests/CR-015-ws3-r0-savings-ulip-journey.md) · `EPIC-004`  
**AIGEM suggestion:** APPROVE WITH CONDITIONS (Rajal, Mahesh).  
**signature_status:** `AI-DRAFTED — HUMAN Product + Architecture verdicts outstanding. Listing approvers is not a signature.`

Journey constraints (full): [`JOURNEY-ARCHITECTURE-CONSTRAINTS.md`](../../controls/JOURNEY-ARCHITECTURE-CONSTRAINTS.md).

## Documented journeys (R0 assisted)

| Journey | R0 meaning | Production restriction |
|---|---|---|
| Life (umbrella) | `R0-ASSISTED-LIFE-SALE` — Term **or** Savings/ULIP, one RM, one ETB customer, one Group A insurer, end to end | No production Savings/ULIP **sale** until human T4 Architecture signs |
| Term | Existing proving path; GATE-P4 Term UAT remains | Not replaced by CR-015 |
| Savings | Assisted sale screens are **S11**, not S08 feature breadth | Blocked on GATE-S08 and GAP-006/007 (C5) |
| ULIP | Same as Savings | Same |

## Architecture constraints (Mahesh conditions)

1. Human T4 Architecture signature required before production use of Savings/ULIP sales.
2. No stage-field edits (honoured by this PR).
3. Provider traffic still routes only through the Integration Hub.
4. Deepali / Shailja sit before production if Savings/ULIP adds PII or suitability surfaces beyond the Term pack — Architecture does not waive those boards.
5. EPIC-004 documentation-complete at S08 does not authorise S11 screen implementation.

## Product constraints (Rajal conditions)

1. CR-015 stays CANDIDATE until a HUMAN Product verdict with `reviewer_type: HUMAN`.
2. C5 — GAP-006 / GAP-007 — remains non-waivable for S11.
3. DIY, hybrid, Health, Group B, Customer BFF, and SUG-20260904-eng stay parked.
4. GATE-P4 Term UAT criteria are not replaced.
5. No Flutter / BFF / catalogue runtime until HUMAN APPROVED and S11 is entered lawfully.

## Routing / Integration Hub / production

Hop remains UI → BFF → Integration Hub. UI/BFF never receive 1SB or insurer wire codes. 1SB master lookup is a provider feed behind the adapter.

Human ticks:

- [ ] Rajal — Approve with conditions (`reviewer_type: HUMAN`)
- [ ] Mahesh — Approve with conditions (`reviewer_type: HUMAN`)
