# `services/` feature freeze (CR-010 K-C3)

**Effective:** 2026-09-30 (AIGEM-ACCEPTED; human CR-010 ratification outstanding)  
**Adjudicator:** Kalpana / R12, on Amit / Engineering's technical read  
**Disputes:** escalate to Mahesh + Rajal  
**signature_status:** `AI-DRAFTED — published definition; not a funding approval`

Source table: [CR-010 Delivery verdict §3.6](../change-requests/CR-010/verdicts/r12-delivery-kalpana.md).

## Permitted during the freeze

- Any change delivering an S08 or S09 gate criterion
- Test-only additions to existing services
- Defect fixes on the delivered Term path (WS-1 in-scope)
- WS-2 IAM foundation work (explicitly not stopped)
- Refactors required to make existing code testable
- Documentation, rule packs, design, all governance work
- Schema/control changes required by an AIGEM-accepted CR condition (example: audit reconstruction columns before regulated writes)

## Not permitted

- New bounded contexts
- New endpoints, new LOB handlers (Health/Motor = WS-1 Phase 5 — stopped until GATE-S08 and GATE-S11 PASSED)
- Feature breadth in `services/` outside recovery / admitted CR scope
- Refactors of convenience
- Flutter / BFF / catalogue runtime for Savings/ULIP until CR-015 is HUMAN APPROVED and S11 is entered lawfully

## Sample adjudication

This 2026-09-30 PR: governance docs, CODEOWNERS, autopilot tests, Flyway V3 additive columns on `audit_event`, SUIT-R40 test. **Permitted** (governance + test + S08/S09 control evidence; no new LOB handler, no new public endpoint).
