# Figma & artefact intake log

**Owner:** Platform Product Owner / BA  
**Purpose:** Single place to register every input for the product reset

---

## 1. Figma (client review prototype)

| Field | Value |
|-------|-------|
| File name | For Client Review |
| Prototype URL | https://www.figma.com/proto/JyLGAaO88ELjnyVF2FQ3Bx/For-Client-Review?node-id=208-9666&page-id=208%3A2982 |
| Access from automation | **Blocked** (login wall) |
| Inventory status | **Not started** — map to CJ/RMJ/JRN after walkthrough |
| Working rule | Figma is **reference only** (MVP + concept + future mixed). Not SoT — see Working Decisions §15 |

### BA checklist

- [ ] Full screen list  
- [ ] Happy-path click path (RM)  
- [ ] Customer-only steps  
- [ ] Error / empty / pending states  
- [ ] LOB(s) depicted  
- [ ] Compliance screens (suitability, consent)  
- [ ] MVP vs concept flags  

---

## 2. Baseline docs — ingested

| # | Artefact | Date | Summary | Adopt / Reopen / Reject | Linked |
|---|----------|------|---------|-------------------------|--------|
| 1 | `Volume_01_Vision_and_Strategy_v1.0_*.pdf` | 2026-07-31 | Bank-owned insurer-agnostic platform; 1SB→direct roadmap; capability layers | **Adopt** (vision/principles) | KB 01, 02, 03, 08 |
| 2 | `Volume_02_Business_Goals_v1.0_*.pdf` | 2026-07-31 | BG-001…008 + CX/RM/growth/compliance goals | **Adopt** | KB 02 |
| 3 | `Volume_03_Business_Requirements_v1.0_*.pdf` | 2026-07-31 | BR-* capability shells (template depth) | **Reopen** for detailed AC | KB 03, 10 |
| 4 | `Volume_04_Stakeholder_Catalogue_v1.0_*.pdf` | 2026-07-31 | Full stakeholder inventory by group | **Adopt** (roles); reopen RACI detail | KB 06 |
| 5 | `Volume_05_Business_Processes_v1.0_*.pdf` | 2026-07-31 | BP-001…018 + value stream | **Adopt** (catalogue) | KB 05, 04 |
| 6 | `Volume_06_Customer_and_RM_Journeys_v1.0_*.pdf` | 2026-07-31 | CJ-01…13, RMJ-01…13, exceptions, state model | **Adopt** | KB 04 |
| 7 | `Phase_1_Business_Discovery_Capability_Map_v0.1_*.pdf` | 2026-07-31 | Capability map + principles | **Adopt** | KB 03 |
| 8 | `Phase_2_Business_Requirement_Catalogue_v0.1_*.pdf` | 2026-07-31 | BR catalogue incl. Quote Comparison / Servicing | **Reopen** for AC depth | KB 03, 10 |
| 9 | `Phase_3_Business_Processes_and_Customer_Journeys_v0.1_*.pdf` | 2026-07-31 | JRN-001… (RM assisted sale etc.) | **Adopt** spine; reopen detail | KB 04 |
| 10 | `Phase_4_Business_Rules_Information_Model_and_Governance_v0.1_*.pdf` | 2026-07-31 | Rule categories, canonical objects, governance | **Adopt** structure | KB 07 |
| 11 | `Phase_5_Solution_Architecture_Blueprint_v0.1_*.pdf` | 2026-07-31 | Domains, integration phases A–C, flow | **Adopt** as advisory blueprint | KB 08 |

**Location:** `docs/au-bank-insurance-platform/artefacts/uploads/`

**PO synthesis:** [knowledge-base/](./knowledge-base/README.md)

---

## 3. Prior repository research (non-binding)

| Path | Treatment |
|------|-----------|
| `docs/1sb-insurance-integration/**` | Candidate Integration Hub / Phase A adapter research — not full Distribution Platform |

---

## 4. Immediate ask

1. Figma walkthrough or frame exports → map to CJ/RMJ.  
2. Session 1 to freeze LOB + R0 definition of done ([10](./knowledge-base/10-gaps-and-po-assessment.md)).  
3. BA pack: Consent/Suitability rules + JRN-001 deep dive.
