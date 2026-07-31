# Figma & artefact intake log

**Owner:** Platform Product Owner / BA  
**Purpose:** Single place to register every input for the product reset

---

## 1. Figma (client review prototype)

| Field | Value |
|-------|-------|
| File name | For Client Review |
| Prototype URL | https://www.figma.com/proto/JyLGAaO88ELjnyVF2FQ3Bx/For-Client-Review?node-id=208-9666&viewport=20644%2C20677%2C0.17&t=VN5kR25s6psZI6Js-1&scaling=scale-down&content-scaling=fixed&page-id=208%3A2982 |
| File key | `JyLGAaO88ELjnyVF2FQ3Bx` |
| Page id | `208:2982` |
| Start node | `208:9666` |
| Access from automation | **Blocked** (login wall) — needs human walkthrough or exported frames |
| Inventory status | **Not started** |

### What we need from Figma (BA checklist)

- [ ] Full screen list (name + purpose)  
- [ ] Happy-path click path (RM)  
- [ ] Customer-only steps (if any)  
- [ ] Error / empty / pending states shown  
- [ ] LOB(s) depicted (Term / Health / Motor / …)  
- [ ] Explicit compliance screens (suitability, consent, disclosures)  
- [ ] Mark concept-only screens vs MVP candidates  

### Preferred handoff formats (any one)

1. Live walkthrough with PO + BA (recorded notes into §4 mapping in [04](./04-process-and-journey-canvas.md))  
2. PDF / PNG export per frame into `docs/au-bank-insurance-platform/artefacts/figma/`  
3. Figma access for a service account (if later available)

---

## 2. Baseline docs — awaiting upload

> You said you will upload the earlier basic docs. Log each file below when it arrives.

| # | Artefact name / path | Source | Date received | Summary (1–2 lines) | Adopt / Reopen / Reject | Linked discovery Q |
|---|----------------------|--------|---------------|----------------------|-------------------------|--------------------|
| 1 | *pending upload* | | | | | |

**Drop location (recommended):**  
`docs/au-bank-insurance-platform/artefacts/uploads/`  
(keep original filenames; add a short note in this table)

---

## 3. Prior repository research (already in git — non-binding)

These are **not** product SSOT until adopted via decision log.

| Path | What it is | Suggested treatment |
|------|------------|---------------------|
| `docs/1sb-insurance-integration/01-executive-overview.md` | 1SB + bank fit narrative | Reopen under AU Bank branding |
| `docs/1sb-insurance-integration/02-rm-assisted-bank-checklist.md` | Practical build checklist | Mine for discovery questions |
| `docs/1sb-insurance-integration/journeys/universal-lob-journey.md` | Universal stages | Align to canvas after Figma |
| `docs/1sb-insurance-integration/service-ssot/**` | Eng backlog & phase delivery | Engineering history — do not auto-adopt scope |
| `docs/1sb-insurance-integration/architecture/**` | Technical architecture | Options after Session 3 |

See also [references/README.md](./references/README.md).

---

## 4. Intake protocol

1. New file arrives → add row in §2.  
2. BA skims → 5 bullets max into Summary.  
3. PO marks **Adopt / Reopen / Reject**.  
4. If Adopt: update charter / vision / journey / discovery answers in the same PR when possible.  
5. If Reopen: create or link a discovery question in [03](./03-discovery-backlog.md).

---

## 5. Immediate ask to stakeholders

1. **Upload** all earlier basic docs (product / process / any decks).  
2. **Walk Figma** with PO + BA, or export frames.  
3. Confirm executive sponsor name for Q-P0-20.
