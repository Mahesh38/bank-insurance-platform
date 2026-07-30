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

_Last updated by: Agent A · 2026-07-30_

---

## Section B — Agent B: Product Catalog (Phase 0.2)

> Scope: Term product/insurer allow-list catalog, config loading, Phase 0.2 confirmation.  
> Artefacts: [CONFIRM-02-term-products.md](./CONFIRM-02-term-products.md) · [config/catalog/term-products.example.yaml](../../../../config/catalog/term-products.example.yaml)

### Done

- [x] **B-001** Create `config/catalog/term-products.example.yaml` — configurable product allow-list with `manufacturerId`, `productCode`, `enabled` flag, `sandbox-only` guard, and startup validation rules.  
  _Artefact:_ [`config/catalog/term-products.example.yaml`](../../../../config/catalog/term-products.example.yaml)  
  _Completed:_ 2026-07-30

- [x] **B-002** Create `CONFIRM-02-term-products.md` — confirmation checklist for product team + 1SB RM; documents catalog loading approach and replaceability guarantee.  
  _Artefact:_ [`CONFIRM-02-term-products.md`](./CONFIRM-02-term-products.md)  
  _Completed:_ 2026-07-30

- [x] **B-003** Update `ACTION-PLAN.md` row 0.2 with links to CONFIRM-02 and catalog example.  
  _Artefact:_ [`ACTION-PLAN.md`](../ACTION-PLAN.md)  
  _Completed:_ 2026-07-30

### Open

- [ ] **B-004** Confirm `manufacturerId` for at least one Term insurer (CONFIRM-02 item C2-1).  
  _Owner:_ Product + 1SB RM · _Due:_ TBD · _Blocked by:_ 1SB RM availability

- [ ] **B-005** Confirm `productCode` for that insurer's Term product (CONFIRM-02 item C2-2).  
  _Owner:_ Product + 1SB RM · _Due:_ TBD · _Blocked by:_ B-004

- [ ] **B-006** Populate `config/catalog/term-products.yaml` (gitignored) with confirmed values and flip `enabled: true`.  
  _Owner:_ Eng · _Due:_ TBD · _Blocked by:_ B-004, B-005

- [ ] **B-007** Verify sandbox quote returns non-empty `offers[]` with populated catalog (CONFIRM-02 item C2-4).  
  _Owner:_ Eng · _Due:_ TBD · _Blocked by:_ B-006 + Phase 0.1 sandbox access

---

## Section C — Agent B: Inbound Authentication (Phase 0.3)

> Scope: Replaceable auth config (JWT / mTLS / NONE), Security team sign-off, agentId/distributorId rules.  
> Artefact: [CONFIRM-03-inbound-auth.md](./CONFIRM-03-inbound-auth.md)

### Done

- [x] **C-001** Create `CONFIRM-03-inbound-auth.md` — replaceable auth config with `auth.mode: JWT | MTLS | NONE`, JWT env-var config, mTLS truststore config, Security checklist, and explicit distributorId/agentId rules.  
  _Artefact:_ [`CONFIRM-03-inbound-auth.md`](./CONFIRM-03-inbound-auth.md)  
  _Completed:_ 2026-07-30

- [x] **C-002** Update `ACTION-PLAN.md` row 0.3 with links to CONFIRM-03 and coupling doc.  
  _Artefact:_ [`ACTION-PLAN.md`](../ACTION-PLAN.md)  
  _Completed:_ 2026-07-30

### Open

- [ ] **C-003** Security team to confirm JWT issuer URL (`JWT_ISSUER`) (CONFIRM-03 item C3-1).  
  _Owner:_ Security · _Due:_ TBD

- [ ] **C-004** Security team to confirm JWKS URI and reachability from service egress (C3-2).  
  _Owner:_ Security · _Due:_ TBD · _Blocked by:_ C-003

- [ ] **C-005** Agree JWT audience value with Security + Architect (C3-3).  
  _Owner:_ Security + Arch · _Due:_ TBD

- [ ] **C-006** Agree `actorId` claim name; verify it satisfies `agentId` requirement (C3-4, C3-6).  
  _Owner:_ Security + Arch · _Due:_ TBD

- [ ] **C-007** Obtain sample non-prod token for integration testing (C3-5).  
  _Owner:_ Security · _Due:_ TBD · _Blocked by:_ C-003..C-005

- [ ] **C-008** (Phase 1) Add ArchUnit test: no public DTO field named `distributorId` outside `adapter.onesb.*`.  
  _Owner:_ Eng · _Due:_ Phase 1

---

## Section D — Agent B: SSOT Kickoff (Phase 0.4)

> Scope: Team kickoff session — Case 2 + Term-first agreement, open blocker resolution.  
> Artefact: [CONFIRM-04-ssot-kickoff.md](./CONFIRM-04-ssot-kickoff.md)

### Done

- [x] **D-001** Create `CONFIRM-04-ssot-kickoff.md` — agenda, pre-read pack, key design points, open-questions log, attendee sign-off table.  
  _Artefact:_ [`CONFIRM-04-ssot-kickoff.md`](./CONFIRM-04-ssot-kickoff.md)  
  _Completed:_ 2026-07-30

- [x] **D-002** Update `ACTION-PLAN.md` row 0.4 with link to CONFIRM-04.  
  _Artefact:_ [`ACTION-PLAN.md`](../ACTION-PLAN.md)  
  _Completed:_ 2026-07-30

### Open

- [ ] **D-003** Tech Lead to schedule kickoff session with all required attendees.  
  _Owner:_ Tech Lead · _Due:_ TBD · _Depends on:_ 0.1 access status known (even if pending)

- [ ] **D-004** Distribute pre-read pack at least 2 days before session.  
  _Owner:_ Tech Lead · _Due:_ before session

- [ ] **D-005** Run session; fill open-questions log in CONFIRM-04.  
  _Owner:_ Tech Lead (facilitator) · _Due:_ session date

- [ ] **D-006** Collect sign-offs from all attendees in CONFIRM-04 sign-off table.  
  _Owner:_ Tech Lead · _Due:_ session day

---

## Section E — Agent B: Tracking Board (Phase 0.5)

> Scope: Jira/Linear backlog creation from PRODUCT-BACKLOG.md P0 stories.  
> Artefacts: [CONFIRM-05-tracking-board.md](./CONFIRM-05-tracking-board.md) · [p0-story-board.md](./p0-story-board.md)

### Done

- [x] **E-001** Create `CONFIRM-05-tracking-board.md` — tracking tool setup checklist, story-ID→ticket mapping table, ordering rules.  
  _Artefact:_ [`CONFIRM-05-tracking-board.md`](./CONFIRM-05-tracking-board.md)  
  _Completed:_ 2026-07-30

- [x] **E-002** Create `p0-story-board.md` — importable backlog seed with all 25 P0 story IDs as checkboxes grouped by epic, with suggested ticket titles and Jira/Linear import instructions.  
  _Artefact:_ [`p0-story-board.md`](./p0-story-board.md)  
  _Completed:_ 2026-07-30

- [x] **E-003** Update `ACTION-PLAN.md` row 0.5 with links to CONFIRM-05 and story board.  
  _Artefact:_ [`ACTION-PLAN.md`](../ACTION-PLAN.md)  
  _Completed:_ 2026-07-30

### Open

- [ ] **E-004** PO to choose tracking tool (Jira / Linear) and create project (CONFIRM-05 item C5-1).  
  _Owner:_ PO · _Due:_ TBD · _Note:_ Can start immediately — no dependency on 0.1–0.4

- [ ] **E-005** Create epic + priority + type labels matching PRODUCT-BACKLOG.md conventions (C5-3..5).  
  _Owner:_ PO · _Due:_ TBD

- [ ] **E-006** Create all 25 P0 tickets from `p0-story-board.md` with story ID labels (C5-6/7).  
  _Owner:_ PO / SM · _Due:_ TBD

- [ ] **E-007** Fill mapping table in CONFIRM-05 with real ticket IDs; share board link with team (C5-8/9).  
  _Owner:_ PO / SM · _Due:_ TBD · _Blocked by:_ E-006

---

## Phase 0 gate summary

| Phase | Description | Agent | Status |
|-------|-------------|-------|--------|
| 0.1 | 1SB sandbox access + credentials | A | **IN PROGRESS** (artefacts created; credentials PENDING) |
| 0.2 | Term products confirmed + catalog populated | B | **PENDING** |
| 0.3 | Inbound auth agreed + sample token available | B | **PENDING** |
| 0.4 | SSOT kickoff complete + team sign-off | B | **PENDING** |
| 0.5 | Tracking board live with all P0 tickets | B | **PENDING** |

**Phase 0 exit:** All rows = DONE → Team begins Phase 1 (Foundations: SHARED-001..004, TECH-001..003).

---

## Coordination notes (A ↔ B)

- The `distributorId` confirmed in 0.1 (A-003) feeds CONFIRM-03 (C-003..C-007) — coordinate before writing vault path docs.
- The auth mode decision from 0.3 feeds CONFIRM-04 kickoff agenda topic 6 — share Security decision before scheduling kickoff.
- Tracking board creation (0.5) has **no dependencies** on 0.1–0.4 and can start immediately from `p0-story-board.md`.
- Design rules binding both agents are in [COUPLING-AND-REPLACEABILITY.md](./COUPLING-AND-REPLACEABILITY.md) §4 ("what must never be hardcoded").

_Last updated by: Agent B · 2026-07-30_

---

## Update — 2026-07-30 (bank-provided Phase 0 data)

See **[PHASE-0-DATA-AND-GAPS.md](./PHASE-0-DATA-AND-GAPS.md)** for full scorecard.

**Received:** demo URL, distributor `BCIBL`, distribution defaults, ICICI / E38 LifeSave product.  
**Still open:** API key/secret, IP whitelist, curl proof, inbound auth, kickoff LOB decision (Saving vs Term), board tickets, additional insurers/products for multi-quote.

- [x] Persist received data into config templates (distribution.defaults.yaml, products.example.yaml)
- [ ] R1–R2 Obtain API key/secret → vault
- [ ] R4–R5 IP whitelist + curl proof
- [ ] R6 Inbound auth mode
- [ ] R13 Decide first LOB (LifeSave E38 available; Term not yet)
- [ ] Add more catalog rows as insurers/products are enabled
