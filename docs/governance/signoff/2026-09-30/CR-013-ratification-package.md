# CR-013 — Ratification Package (AIGEM-ACCEPTED 2026-09-30)

**Change request:** [`CR-013-r0-lead-mis-admin-scope.md`](../../change-requests/CR-013-r0-lead-mis-admin-scope.md) · `ADR-014`  
**AIGEM suggestion:** RATIFY WITH CONDITIONS (Mahesh, Deepali, Shailja).  
**signature_status:** `AI-DRAFTED — human T4 Architecture / Security / Compliance outstanding`

## Validation (2026-09-30)

| Topic | Binding rule | Repo status |
|---|---|---|
| Lead lifecycle | Spoken/primary name is Lead; Opportunity is durable-demand alias; `leadId` unchanged | Transcribed in CURRENT-STATE / charter / ADR-014 |
| Working inbox archive | After convert + Payment.RECONCILED + Policy.ACTIVE, Lead archives; 7-year SoT is Payment/Policy/Consent/Suitability/Audit + Lead attribution | Design; not a production job |
| Admin module scope | Admin UI in R0 on an isolated path; not on the Lead writer | In R0 scope; implementation is S11+ |
| MIS reporting scope | MIS in R0, isolated read path; MIS must not `lead.create` | In R0 scope; C-ISO-1 still a condition |
| Off-platform ingest | Policy ingest `source=OFF_PLATFORM`; never `lead.create`; maker-checker | C-ING-1; not implemented |
| Portal governance | Admin/MIS/reconciliation never use Lead/RM OLTP writer | C-ISO-1 |
| Issuance modes | `STP` / `NON_STP` / `INSTA` do not skip hard gates | C-ISS-1 |
| PPHI | Control-to-seam map before first regulated action | C-PPHI-1; Board 6 human |

ADR-005 (RM-only Lead create) and hard gates (suitability, consent, customer-device payment, RECONCILED-before-issue) stand.

## Compliance conditions (CR-013 §5) — not signed

C-RET-1 · C-RET-2 · C-ING-1 · C-ISO-1 · C-ISS-1 · C-PPHI-1 remain Shailja's. AI must not generate `TEMPORARY_EXCEPTION_APPROVED`.

Human ticks:

- [ ] Mahesh — Ratify (structure / ADR-014)
- [ ] Deepali — Ratify (trust boundary of admin/MIS/ingest)
- [ ] Shailja — Ratify with §5 conditions
