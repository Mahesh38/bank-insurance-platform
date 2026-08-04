# Phase 0 — Access, config & confirmation pack

**Goal:** Unblock Phase 1 without hardcoding unknowns. Everything missing is **configurable** + tracked in confirmation checklists / TODO tracker.

**Binding rules:** [COUPLING-AND-REPLACEABILITY.md](./COUPLING-AND-REPLACEABILITY.md) · SOLID + DRY + KISS  
**Latest ack:** [PO-DEV-ENV-REQUIREMENTS.md](./PO-DEV-ENV-REQUIREMENTS.md)

---

## Read / work this pack

| Doc / artefact | Purpose |
|----------------|---------|
| **[PHASE-0-DATA-AND-GAPS.md](./PHASE-0-DATA-AND-GAPS.md)** | What we have vs still required |
| **[PO-DEV-ENV-REQUIREMENTS.md](./PO-DEV-ENV-REQUIREMENTS.md)** | PO↔Dev ack + Dev/UAT/Prod needs from 1SB |
| **[EMAIL-DRAFT-1SB-ONBOARDING-REQUEST.md](./EMAIL-DRAFT-1SB-ONBOARDING-REQUEST.md)** | Mail for Product → 1SB |
| [TODO-TRACKER.md](./TODO-TRACKER.md) | Open/blocked list |
| [CONFIRM-01-onesb-access.md](./CONFIRM-01-onesb-access.md) | Access checklist |
| [CONFIRM-02-term-products.md](./CONFIRM-02-term-products.md) | Multi insurer/product catalog |
| [CONFIRM-03-inbound-auth.md](./CONFIRM-03-inbound-auth.md) | JWT + mTLS dual-ready |
| [CONFIRM-04-ssot-kickoff.md](./CONFIRM-04-ssot-kickoff.md) | Kickoff (Life-first) |
| [CONFIRM-05-tracking-board.md](./CONFIRM-05-tracking-board.md) | Board tickets |
| [CONFIRM-06-1sb-pune-visit-agenda.md](./CONFIRM-06-1sb-pune-visit-agenda.md) | 1SB Pune office visit — agenda, attendees, close-out checklist |
| [EMAIL-DRAFT-1SB-PUNE-VISIT-AGENDA.md](./EMAIL-DRAFT-1SB-PUNE-VISIT-AGENDA.md) | Short mail to 1SB proposing the visit agenda |
| [COUPLING-AND-REPLACEABILITY.md](./COUPLING-AND-REPLACEABILITY.md) | Configurable / replaceable rules |
| [`secrets-source.example.yaml`](../../../../config/onesb/secrets-source.example.yaml) | PROPERTIES vs AWS SM |
| [`inbound-auth.example.yaml`](../../../../config/onesb/inbound-auth.example.yaml) | Dual auth |
| [`products.example.yaml`](../../../../config/catalog/products.example.yaml) | Life Term/Saving catalog |

---

## Phase 0 exit gate (updated)

Phase 1 scaffolding may start with **risk acceptance** on temporary partner UAT keys for local/dev only.

**Before trusting UAT integration:**
- [ ] Dedicated BCIBL API key/secret requested from 1SB (email draft)
- [ ] Secrets source configured (`PROPERTIES` now → AWS SM later)
- [ ] ≥1 Life product enabled (✅ E38 Saving; Term when listed)
- [ ] Dual inbound auth adapters planned (JWT + mTLS)
- [ ] Kickoff sign-off (Life-first)
- [ ] P0 tickets created

**Prod-only TODOs (not UAT blockers):**
- [ ] IP whitelist with 1SB
- [ ] Connectivity proof from bank prod egress

Parent plan: [../ACTION-PLAN.md](../ACTION-PLAN.md)
