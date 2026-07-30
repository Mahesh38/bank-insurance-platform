# Phase 0 — Access, config & confirmation pack

**Goal:** Unblock Phase 1 without hardcoding unknowns. Everything missing is **configurable** + tracked in confirmation checklists / TODO tracker.

**Binding rules:** [COUPLING-AND-REPLACEABILITY.md](./COUPLING-AND-REPLACEABILITY.md) · SOLID + DRY + KISS

---

## Agent assignments (completed scaffolding)

| Agent | Phase items | Delivered |
|-------|-------------|-----------|
| **Developer A** | 0.1 1SB access | Provider config, `.env.example`, CONFIRM-01, TODO Section A |
| **Developer B** | 0.2–0.5 catalog, auth, kickoff, board | Catalog YAML, CONFIRM-02…05, story board, coupling doc, TODO Sections B–E |

---

## Read / work this pack

| Doc / artefact | Purpose |
|----------------|---------|
| **[PHASE-0-DATA-AND-GAPS.md](./PHASE-0-DATA-AND-GAPS.md)** | **What we have vs what is still required** |
| [TODO-TRACKER.md](./TODO-TRACKER.md) | Master open/blocked list — **do not miss items** |
| [CONFIRM-01-onesb-access.md](./CONFIRM-01-onesb-access.md) | Confirm URL, keys, distributorId, IP whitelist |
| [CONFIRM-02-term-products.md](./CONFIRM-02-term-products.md) | Confirm insurers/products (multi-entry catalog) |
| [CONFIRM-03-inbound-auth.md](./CONFIRM-03-inbound-auth.md) | Confirm JWT vs mTLS (replaceable `auth.mode`) |
| [CONFIRM-04-ssot-kickoff.md](./CONFIRM-04-ssot-kickoff.md) | Team kickoff sign-off (+ first LOB decision) |
| [CONFIRM-05-tracking-board.md](./CONFIRM-05-tracking-board.md) | Create Jira/Linear tickets |
| [p0-story-board.md](./p0-story-board.md) | Importable P0 story checklist |
| [COUPLING-AND-REPLACEABILITY.md](./COUPLING-AND-REPLACEABILITY.md) | What must stay configurable |
| [`config/onesb/`](../../../../config/onesb/) | Provider + distribution defaults |
| [`config/catalog/products.example.yaml`](../../../../config/catalog/products.example.yaml) | Multi insurer/product catalog |

---

## Phase 0 exit gate

Do **not** start Phase 1 coding until:

- [ ] CONFIRM-01 critical path ticked (or explicitly risk-accepted with vault placeholders only for local mock)
- [ ] At least one Term product placeholder filled after 1SB confirmation (CONFIRM-02)
- [ ] Auth mode chosen and documented (CONFIRM-03)
- [ ] Kickoff sign-off (CONFIRM-04)
- [ ] P0 tickets created (CONFIRM-05)
- [ ] No open `[!]` blockers in TODO-TRACKER without an unblock owner

Parent plan: [../ACTION-PLAN.md](../ACTION-PLAN.md)
