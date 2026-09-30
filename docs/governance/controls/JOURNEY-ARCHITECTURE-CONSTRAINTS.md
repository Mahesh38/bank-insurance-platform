# Life / Term / Savings / ULIP journey architecture constraints (CR-015)

**signature_status:** `AI-DRAFTED — not a production authorisation`

## Life journey (R0 assisted)

One RM sells a complete Life policy — Term **or** Savings/ULIP — to one ETB customer from one Group A insurer: lead → suitability → quote → proposal → customer-device payment → RECONCILED → issued policy + audit.

## Term journey

Existing proving path. GATE-P4 Term UAT criteria remain. CR-015 does not replace them.

## Savings journey

Assisted Savings sale is in R0 **scope**. Screen implementation is **S11**. Blocked on GATE-S08 PASS and GAP-006/007 (non-waivable C5).

## ULIP journey

Same gating as Savings.

## Routing restrictions

UI → BFF → Integration Hub only. No UI/BFF 1SB or insurer wire codes. No platform service calls a provider adapter directly. `distributorId` is never caller-supplied.

## Integration Hub requirements

All provider traffic, including 1SB master lookup, stays behind the Hub/adapter. CR-014 adapter coverage is not Hub-bypass permission.

## Production restrictions

- No production Savings/ULIP **sales** until human T4 Architecture signs.
- Deepali / Shailja before production if PII or suitability surfaces expand beyond the Term pack.
- EPIC-004 complete-at-S08 is documentation, not S11 authorisation.
- No Flutter / BFF / catalogue runtime until HUMAN APPROVED and S11 is entered lawfully.
- DIY, hybrid, Health, Group B, Customer BFF, SUG-20260904-eng stay parked.
