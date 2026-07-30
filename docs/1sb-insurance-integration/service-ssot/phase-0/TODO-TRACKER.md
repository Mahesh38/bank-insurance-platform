# TODO Tracker — 1SB Integration Service, Phase 0

**SSOT:** [ACTION-PLAN.md](../ACTION-PLAN.md)  
**Branch convention:** Each agent or team appends their section below. Do not edit other agents' sections without cross-referencing.  
**Format per item:** `[ ]` = open · `[x]` = done · `[~]` = in-progress · `[!]` = blocked

---

## How to use this file

1. Add items under your agent/team section.
2. Include a short description, a link to the relevant artefact or doc, and an owner/due field.
3. Mark `[x]` when done; add a note with date and evidence link.
4. Blockers go in the "Blocked" sub-list with reason + unblock path.

---

## Section A — Agent A (Config Scaffolding & Confirmation Docs)

> Scope: Phase 0.1 config scaffolding, replaceability foundations, confirmation artefacts.

### Done

- [x] **A-001** Create `config/onesb/provider-config.yml` — provider identity, auth refs, timeouts, poll config, feature flags, sandbox/prod profile overlays.  
  _Artefact:_ [`config/onesb/provider-config.yml`](../../../../config/onesb/provider-config.yml)  
  _Completed:_ 2026-07-30

- [x] **A-002** Create `config/onesb/.env.example` — env var names and vault path conventions; no real values.  
  _Artefact:_ [`config/onesb/.env.example`](../../../../config/onesb/.env.example)  
  _Completed:_ 2026-07-30

- [x] **A-003** Create `config/onesb/README.md` — config schema overview, replaceability note, how to add a new provider.  
  _Artefact:_ [`config/onesb/README.md`](../../../../config/onesb/README.md)  
  _Completed:_ 2026-07-30

- [x] **A-004** Create `docs/.../phase-0/CONFIRM-01-onesb-access.md` — sandbox access confirmation checklist (URL, keys, distributorId, whitelist, curl proof).  
  _Artefact:_ [`CONFIRM-01-onesb-access.md`](./CONFIRM-01-onesb-access.md)  
  _Completed:_ 2026-07-30

- [x] **A-005** Update `ACTION-PLAN.md` Phase 0 row 0.1 with link to CONFIRM-01.  
  _Artefact:_ [`ACTION-PLAN.md`](../ACTION-PLAN.md)  
  _Completed:_ 2026-07-30

### Open

- [ ] **A-006** Confirm vault path conventions with Platform team and update placeholders in `provider-config.yml` and `.env.example`.  
  _Owner:_ Platform / DevSecOps  
  _Due:_ TBD  
  _Blocked by:_ Platform team availability

- [ ] **A-007** Confirm sandbox base URL with 1SB RM; update default in `provider-config.yml` sandbox profile if different from placeholder.  
  _Owner:_ PO / 1SB RM  
  _Due:_ TBD  
  _Depends on:_ CONFIRM-01 item A1

- [ ] **A-008** Confirm outbound egress CIDR with Infra/Network team; add as comment in `provider-config.yml` under `# egress.cidr.*`.  
  _Owner:_ Infra/Network  
  _Due:_ TBD

- [ ] **A-009** Once CONFIRM-01 is fully ticked ✅, update CONFIRM-01 header (`Status: CONFIRMED`) and commit curl proof response artefact (sanitised).  
  _Owner:_ Tech lead  
  _Due:_ after items A–E of CONFIRM-01 are complete

- [ ] **A-010** Review whether `backoffStrategy: EXPONENTIAL` is preferable once sandbox latency is measured (currently `FIXED` for Phase 0 simplicity).  
  _Owner:_ Eng  
  _Due:_ Phase 2 kick-off

### Blocked

- [!] **A-011** Real credentials cannot be placed in vault until 1SB RM completes onboarding.  
  _Blocked by:_ 1SB RM action (CONFIRM-01 items B1, B2, C1)  
  _Unblock path:_ 1SB RM to issue sandbox key/secret/distributorId → Platform team stores in vault.

---

## Section B — Agent B / Team B

> _This section is reserved for Agent B. Append items here._

<!-- Agent B: add your open items under this heading -->

---

## Section C — Phase 0.2–0.5 Owners

> _To be filled in by respective owners (Product, Security, PO)._

- [ ] **C-001** (0.2) Confirm at least one Term product quotable in sandbox — see [ACTION-PLAN.md 0.2](../ACTION-PLAN.md#phase-0--access--alignment-before-code).  
  _Owner:_ Product + 1SB  
  _Due:_ TBD

- [ ] **C-002** (0.3) Align bank→service auth approach (JWT claims / mTLS) and write runbook.  
  _Owner:_ Security + Architect  
  _Due:_ TBD

- [ ] **C-003** (0.4) Kickoff walkthrough of SSOT (decisions, backlog order, DRY/KISS).  
  _Owner:_ Tech lead  
  _Due:_ TBD

- [ ] **C-004** (0.5) Create tracking board from [PRODUCT-BACKLOG.md](../PRODUCT-BACKLOG.md) P0 stories (Jira/Linear).  
  _Owner:_ PO / Scrum master  
  _Due:_ TBD

---

_Last updated by: Agent A · 2026-07-30_
