# Detailed module BRDs — Rajal pack, September 2026

**Owner:** Platform Product Owner (Rajal)  
**Status:** Ingested as **working module-behaviour SSOT** on 2026-09-28 (`SUG-20260928-brd`, `DOC-005`).  
Each source file still says *Draft for Business and Technology Review* and has **blank sign-off rows**. Formal Business / Technology / Information Security / QA approval is outstanding.  
**Do not implement a TBD, TBC, assumption, indicative value, or unresolved cross-BRD conflict.** Raise it to Rajal.

Related: [alignment and conflict report](../BRD-ALIGNMENT-2026-09-28.md) · [BRD overview](../BRD-OVERVIEW.md) · [Working Decisions](../../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md)

---

## How to use this folder

1. Select BRDs **by module**. Do not blend all nine into one assumed workflow.
2. Source-stated requirements are authoritative for that module.
3. Text labelled assumption, dependency, open item, TBD, TBC, indicative, or subject to confirmation is **not** a finalized implementation decision.
4. Where two BRDs contradict each other, report the conflict. Do not pick a side.
5. Programme-level decisions that these files do not restate (ETB, Life-only, Sold = issuance, no RM-device payment, 1SB replaceability) stay in [Working Decisions](../../07-BUSINESS-CLARIFICATIONS-WORKING-DECISIONS.md) until Rajal overturns them.
6. Figma remains reference only (`D-012`).

Fidelity check on ingest: every source value in the nine context files plus Exception annexures was present (`qa_report` passed; 0 missing).

---

## Pack

| Module | File | Version | Date | Journey label |
|--------|------|---------|------|----------------|
| Login, Authentication and Account Recovery | [Login_Module_BRD_Detailed_CONTEXT.md](./Login_Module_BRD_Detailed_CONTEXT.md) | 1.0 | 04-Sep-2026 | Assisted Journey |
| Lead Management | [Lead_Module_BRD_Detailed_CONTEXT.md](./Lead_Module_BRD_Detailed_CONTEXT.md) | 1.0 | 04-Sep-2026 | Assisted Journey |
| Suitability (Savings / ULIP) | [Suitability_Module_BRD_Detailed_CONTEXT.md](./Suitability_Module_BRD_Detailed_CONTEXT.md) | 1.0 | 05-Sep-2026 | PWA Assisted |
| Savings listing and compare | [Savings_Listing_and_Compare_Plans_BRD_Detailed_CONTEXT.md](./Savings_Listing_and_Compare_Plans_BRD_Detailed_CONTEXT.md) | 1.0 | 07-Sep-2026 | PWA Assisted |
| Savings product details | [Product_Details_Savings_Plan_BRD_Detailed_CONTEXT.md](./Product_Details_Savings_Plan_BRD_Detailed_CONTEXT.md) | 1.0 | 08-Sep-2026 | PWA Assisted |
| Quote finalisation, riders, final BI | [Quote_Finalisation_Riders_and_Final_BI_BRD_Detailed_CONTEXT.md](./Quote_Finalisation_Riders_and_Final_BI_BRD_Detailed_CONTEXT.md) | 1.0 | 08-Sep-2026 | PWA Assisted |
| Create Pitch Deck | [Create_Pitch_Deck_BRD_Detailed_CONTEXT.md](./Create_Pitch_Deck_BRD_Detailed_CONTEXT.md) | 1.0 | 08-Sep-2026 | PWA Assisted |
| Customer verification, payment, proposal submission | [Customer_Buying_Journey_BRD_V1.2_CONTEXT.md](./Customer_Buying_Journey_BRD_V1.2_CONTEXT.md) | 1.2 | 08-Sep-2026 | Seller handoff → customer-controlled |
| Exception handling (AUBIMA) | [Exception_Handling_BRD_V1.0_CONTEXT.md](./Exception_Handling_BRD_V1.0_CONTEXT.md) | 1.0 | 16-Sep-2026 | Cross-cutting |

Source hashes: [manifest.json](./manifest.json).

---

## Not in this pack

These [BRD-OVERVIEW](../BRD-OVERVIEW.md) chapters have **no** September 2026 detailed BRD yet. Do not invent them from this pack.

- User Management (§2)
- Lead / Application Rules and workflow designer (§5) — Exception Handling covers approval/block rules only
- Term suitability (Suitability BRD excludes Term)
- ULIP listing / product details / detailed quote (Savings-only files)
- Insurance Risk and fraud / PTL / RAG (§9)
- Welcome Calling (§12)
- Post-issuance servicing beyond status (§13)
- DWH push (§14)
- Commission (§15)
- Full MIS / Dashboard (§16)
- Customer self-service (DIY) as a standalone journey
