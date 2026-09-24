# Board 1 — Architecture answers to the bank CAB / UAT environment sign-off checklist

**Board:** 1 — Architecture · **AIGEM role:** `R2`  
**Reviewer:** Mahesh — Principal Insurance Platform Architect  
**Reviewer type:** `AGENT` (AI simulation of Mahesh)  
**Self-review:** false  
**Date:** 2026-09-24  
**Decision id:** `ARCH-DEC-CAB-UAT-20260924`  
**Workstreams:** WS-1 (`P4-UAT-SIGNOFF`) · WS-3 (`R0` platform) · WS-2 (workforce IAM, not this CR)

> ## Draft verdict: `REWORK` for unconditional UAT environment sign-off
> **Architecture severity:** `A1` — the bank-grade UAT environment this form assumes has not been
> stood up. Signing the form as an unconditional **Yes** would claim an environment, a rollback
> proof, a DC/DR sync and a payment-file control set that do not exist.
>
> A **limited, named** UAT *entry* (Term quote + proposal against 1SB UAT / sandbox, no customer
> PII, no money movement) can be discussed as `APPROVED_WITH_CONDITIONS` once the conditions in
> §4 are owned. That is not this form as written.
>
> **`signature_status: AI-DRAFTED — mandatory human T4 Architecture signature outstanding`**
> [`11-REVIEW_GATES.md` §2](../governance/11-REVIEW_GATES.md#2-who-may-sit-on-a-board) is binding.
> This file does **not** constitute ARB approval, T4 Architecture sign-off, Board 4 Security
> sign-off, Board 6 Compliance sign-off, Board 5 QA evidence sufficiency, or Board 7 operational
> readiness. Copy the Yes / No / Remarks column into the ITSM CR. Do not tick the human signature
> block from this draft.

---

## 0. What this CR is, and what it is not

| | |
|---|---|
| **Asked** | UAT environment sign-off for the insurance platform (bank CAB / ITSM checklist). |
| **WS-1 fact** | Objective is `P4-UAT-SIGNOFF`. Open gate `GATE-P4` is **`BLOCKED`**. Criteria 4.1, 4.3, 4.6 **BLOCKED**; 4.2 **PARTIAL**; 4.4 and 4.5 **OPEN**; only 4.7 (coverage) is **MET**. [`BOOT.md` §5](../context/BOOT.md) · [`GATE-EVIDENCE.yaml`](../governance/state/GATE-EVIDENCE.yaml) `GATE-P4`. |
| **WS-3 fact** | Stage S08 Foundation is `CANDIDATE`. **Next** stage is S09 — Platform & Environment Foundation. S09 is **missing in this repository**: no IaC, no bank UAT account, no tested rollback. [`S09-platform-foundation.md` §6](../application-lifecycle-bible/stages/S09-platform-foundation.md#6-current-position-in-this-repository--missing). |
| **What exists today** | Application CI, Spring profiles `local` / `test` / `uat` / `prod`, a Render.com **dev-preview** (`render.yaml`). Render is **never** a data path for PII or production-like data ([`BOOT.md` §5 standing constraints](../context/BOOT.md)). |
| **Bank UAT slot** | `DEP-002` **OPEN / OVERDUE** — bank app team has not named a UAT integration slot. [`DEPENDENCY-REGISTER.md`](../governance/registers/DEPENDENCY-REGISTER.md) · ARB pack row 6. |
| **Horizon** | H0 / R0 assisted Life (Term + Savings/ULIP design). Not production, not DR-ready, not DIY. |

Architecture owns structure, boundaries, integration and NFR *shape*. Environment provisioning,
tested rollback, DC/DR *proof* and dual-instance payment operations are **Shivanshi (R10)** and,
on money movement, the **named bank payment owners**. Security exceptions are **Deepali**. File /
PII / settlement permissibility is **Shailja + Aarti**. I will not tick those seats.

---

## 1. Filled checklist (copy into the CR)

| S.No. | Description (bank form) | Yes / No / N.A. | Remarks (Architecture) |
|---|---|---|---|
| **1** | Change description is correct, enough elaborative to understand the change later by CAB / Audit | **Yes — with conditions** | Description that **must** be on the CR (do not shorten): *Stand up / admit a **lower** environment for the R0 assisted Life slice so a bank caller can exercise **quote + proposal** against 1SB UAT. Scope is Term path first (`P4-UAT-SIGNOFF`). Not in this change: customer-device payment go-live, FUNC-008 payment intimation (Phase 5.3), Health/Motor, DIY, production, DC/DR cutover, Render.com as a regulated path.* Canonical ask: `R0-ASSISTED-LIFE-SALE` in [`BOOT.md` §5](../context/BOOT.md). SAD: [`03-solution-architecture-r0.md`](../platform/ws3-platform/03-solution-architecture-r0.md). Attach the ARB first-sitting pack ([`ARB-PREREQUISITE-PACK.md`](./ARB-PREREQUISITE-PACK.md)) — it is **AI-drafted**, not an approval. **Condition:** Rajal confirms the CR text matches Product acceptance; Architecture will not invent a business BRD signature. |
| **2** | Implementation plan and rollback plan consist of all necessary steps; no steps or pre-checks missing | **No** | Application *design* of deploy/rollback is specified for S09 (`S09-E03`, `S09-VT-04`, `S09-G4`) and is **unproven**. Repository position: *Rollback — Absent. Never designed, never tested.* Environments `dev` / `UAT` / `prod` are **not provisioned** from IaC. [`S09` §3 / §6](../application-lifecycle-bible/stages/S09-platform-foundation.md). WS-1 runbook (secrets rotation, IP allowlist, 1SB 401/5xx) is gate **4.5 OPEN** (owner Shivanshi). **What Architecture will accept as a plan:** (a) artefact built once and promoted, not rebuilt; (b) named rollback = redeploy previous Git SHA / image digest; (c) persistence rollback owned by Aarti (Flyway lives only in `bank-persistence-service`); (d) 1SB UAT config / IP allowlist revert owned with Apigee (`DEP-20260914-apg`); (e) **no** customer PII loaded, so data rollback is empty-by-design. **Owner to attach the steps:** Shivanshi + Amit. Architecture will not sign a plan that only says “rollback if required”. |
| **3** | Risks and impact on other touchpoints or integrated channels are identified and highlighted in the approvals and CR | **Yes — with conditions** | Touchpoints that **must** appear on the CR (from [`INTEGRATION-AND-DEPENDENCY-MATRIX.md`](./arb-prerequisites/INTEGRATION-AND-DEPENDENCY-MATRIX.md) §1): **I-03** 1SB via Apigee (never EKS → `*.1silverbullet.tech`); **I-01** NIP-APP / bank caller (no OAuth tokens on device); **I-07** EBS/CBS — `uat` must not use `dev` stubs (`S-26`) but VPN/DX is `DEP-20260824-dx1` **OPEN** — if CBS is not connected, state **CIF stub residual** on the CR, do not hide it; **I-08** AD-verify via Apigee private; **I-04/I-05** AU Bank PG — **designed, not in this UAT increment** if the CR is quote+proposal only; **I-06** settlement file — out of band, not this change. Forbidden paths if anyone draws them: bank app → 1SB or DB; Flutter → Keycloak/Apigee/domain service. Residual risks: `DEP-002` (no bank caller slot), `DEP-20260914-apg` (Apigee IPs), `GATE-4.1-SANDBOX-E2E`. |
| **4** | Review effectiveness of post-implementation sanity checklist to ensure complete coverage of all integration points, workflows, journeys. Sanity checklist to be attached to the CR | **No** (checklist not yet attached / not sufficient) | Architecture **names** the journeys that must be on Swapnali’s list; QA owns evidence sufficiency ([`AUTHORITY-QUICK-CARD.md`](../context/personas/AUTHORITY-QUICK-CARD.md)). **Must cover if this CR claims “platform UAT”:** (1) Term quote happy path + 422/timeout; (2) proposal submit + poll; (3) idempotent POST replay; (4) no PII in logs; (5) bank app does not call 1SB or DB; (6) negative: quote without suitability id refused; (7) negative: payment link not issued into an RM session. **Must not be claimed on this CR:** customer-device payment, payment intimation, issuance/reconcile, Health/Motor, DIY. Evidence gap: `QA-009` sandbox E2E and gate **4.1 BLOCKED**; gate **4.3 BLOCKED** (no bank caller). [`TEST-BACKLOG.md`](../1sb-insurance-integration/service-ssot/TEST-BACKLOG.md). **Attach Swapnali’s executed sheet**, not this architecture list, before CAB treats row 4 as Yes. |
| **5** | Unconditional Business and Tech Sign Offs attached. For any conditional approvals, the same to be highlighted in the remarks during TD approval in ITSM tool | **No** | There is **no** unconditional Architecture, Product, Security, Compliance or SRE sign-off that can be attached today. `GATE-P4` required approvers (Architect, PO, QA Lead, Compliance 4.4, Shivanshi 4.5) have not closed the gate. Human T4 Architecture / Security / Risk & Compliance remain outstanding on CR-015 and the ARB pack. PO counter-signature on GOV-004 is outstanding ([`ARB-PREREQUISITE-PACK.md` §0 row 1](./ARB-PREREQUISITE-PACK.md#0-readiness-dashboard)). **If CAB proceeds at all, mark this CR CONDITIONAL in ITSM** and paste §4 below. Architecture refuses an unconditional tick. |
| **6** | Syncing of environments within and between DC and DR | **No** / **N.A. for this increment** | **N.A.** if the CR is a single lower-environment UAT entry with no DR claim — say so in the remark so CAB does not infer DR readiness. **No** if anyone claims DC↔DR sync is done. DR *shape* is designed (warm standby `ap-south-2`, RTO ≤ 1 h, RPO ≤ 5 min transactional, RPO 0 audit) and is **unproven** until S09 drills (`NFR-DR-04`, ARB pack row 8–9). Cache/MSK/OpenSearch are **not** replicated (D13–D15). Bank UAT/prod accounts are **not vended** (matrix I-12). S09-E02 “Provision dev, UAT and production” is not started. |
| **7** | Verification of Build id, file name in terms of Version / duplicity, build size, etc. | **Partial — Engineering/SRE to attach artefacts** | Architecture standard: **one artefact, promoted**, never rebuilt per environment (`S09-E02-S02`). CI today: Gradle + ArchUnit + JaCoCo; coverage gate **4.7 MET**. What this CR must attach (Amit + Shivanshi): Git SHA, image digest if containerised, module versions (`1sb-integration-service`, `bank-persistence-service`), lockfile / SBOM pointer ([`SBOM-RUNTIME-INVENTORY.md`](./arb-prerequisites/SBOM-RUNTIME-INVENTORY.md) — lockfile inventory exists; CycloneDX CI artefact still S09). Render Blueprint is **not** an acceptable UAT build record for a bank CAB. Architecture will review the attached IDs; it does not mint them. |
| **8** | Any exception for AppSec / VAPT / Regulatory / Audit point for this change: mention Exception ID with validity or N.A. | **N.A. — no exception requested, and none is granted** | Do **not** write “N.A.” to mean “security is fine”. VA/PT is **NOT-YET** (ARB pack row 11). SAST/ArchUnit/Trivy-on-lockfiles exist; DAST/VA/PT is planned **before UAT exit / S11–S12**, not claimed passed. No AppSec, VAPT, regulatory or audit **exception ID** is being raised by Architecture. Residual gap is owned by **Deepali (Board 4)** and **Swapnali**. Architecture will not waive a Security or Compliance conclusion ([Mahesh card — Never](../context/personas/mahesh-architecture.card.md)). If InfoSec requires an exception to enter UAT without VA/PT, that is a **human Deepali / CISO** paper — not this draft. |
| **9** | For any previous incident / rollback, the permanent fix is implemented to avoid any incident / rollback in future | **N.A.** | Nothing is in a bank production account ([`ARB-PREREQUISITE-PACK.md` §2–4](./ARB-PREREQUISITE-PACK.md)). There is no production incident or production rollback to close. Known **deferred** controls (do not re-open as new debt): Redis / multi-instance job ownership → Phase 5.4; FUNC-008 payment intimation → Phase 5.3; DR test / backup-restore → Phase 6 / S09; `TD-006` secrets stub. If a **dev-preview** incident exists outside this register, Shivanshi records it; Architecture has none to cite. |
| **10** | File-based integration & Payment (a–i) | **N.A. for the live premium path. No for settlement/MIS if those are in scope of this CR.** | See §2. Do **not** tick 10a–i Yes for customer-device PG payment — that path is **not a file drop**. |
| **10a** | File Landing Location different from pickup location | **N.A.** | Live premium path is `POST /v1/payment/url` → `paymentUrl` on the **customer device** (control **C4**). [`payment.md`](../1sb-insurance-integration/field-guides/payment.md) · SAD S-13/S-14. No landing/pickup directories. |
| **10b** | File automatically moved Landing → Pickup | **N.A.** | Same as 10a. |
| **10c** | After processing, file automatically moved to a **separate** archive location | **N.A.** for PG. **No** if settlement (I-06) or off-platform Policy ingest is claimed in this CR — archive/WORM is S09-E06 / `TI-07` and is **absent** today. |
| **10d** | File name contains `DDMMYYYY` | **N.A.** | No payment file name in this increment. If Finance later defines a settlement file, Aarti + Payments set the convention before first drop. |
| **10e** | `ok.txt` after all files, with file name and record count | **N.A.** | Not a file-batch premium integration. |
| **10f** | PII / sensitive file encrypted and decrypted automatically | **N.A.** for this CR **only if** no file and no production-like PII enter UAT. Standing constraint: no PII in logs; regulated data only in AWS India. Render.com is forbidden for PII. If a settlement or MIS file is added, **Shailja + Deepali** own encryption — Architecture will not mark Yes from design intent. |
| **10g** | Duplicate control enabled at transaction level | **Yes — on the API / money-path design; not proven in UAT** | Platform idempotency: `Idempotency-Key` on mutating APIs; payment additionally **INV-PAY-04** (no new attempt while `AUTHORISED` or `UNCERTAIN`). Idempotency is stored with the business write, **not** in cache (`ADR-011`). [`03-solution-architecture-r0.md` §5.2](../platform/ws3-platform/03-solution-architecture-r0.md). WS-1 Redis / multi-instance job ownership is **out of scope until Phase 5.4** — UAT must be **single-instance** or this control is incomplete. Settlement file duplicate control (I-06 / S-15) is designed (`pgTxnId` + amount), not implemented in this increment. |
| **10h** | Inquiry & retry handling methodology enabled | **Yes — designed for API jobs; attach Amit’s runbook before claiming operational** | Quote/proposal are async-poll with bounded backoff; payment callback is at-least-once with `pgTxnId` dedupe; missing callback → `UNCERTAIN`, **never assumed paid** (S-14, F-08). FUNC-008 payment intimation retry is **Phase 5.3**. Gate **4.5** runbook still OPEN. |
| **10i** | Recon is automated | **No** | Reconciliation is a **batch / scheduled** seam (S-15), daily + on-demand, unmatched → `RECONCILIATION_BREAK` / F-07. A policy is **never** issued against a payment that is not `RECONCILED` ([`BOOT.md` §5](../context/BOOT.md)). Automated recon is **not** in the UAT increment and must not be ticked Yes. |
| **11** | For payment system, if scheduler-based txn processing is happening… | **N.A. if this CR excludes money movement and settlement. No if it includes them.** | See §3. |
| **11a** | Dual instance running control | **No** (and must not be claimed) | Multi-instance job ownership / Redis idempotency is explicitly **out of scope until Phase 5.4** ([`BOOT.md` §5 WS-1](../context/BOOT.md)). UAT posture: **one payment / job-store instance**. Architecture will `REWORK` any CR text that implies active-active payment schedulers in UAT. |
| **11b** | Multiple-instance logic reviewed and signed off by Shivendu / Veerendra / Gagan / Kislay / Rajesh / Rajeev Bhatia / Nilesh Mokal | **No** | Those are **bank payment-system** owners, not this Board. Architecture does not sit their review and does not impersonate it. There is no multiple-instance payment scheduler in this increment to send them. If Payments later introduce one, **Shivanshi + Payments** raise that review; Architecture joins on structure only. |

---

## 2. File-based integration — why the honest answer is N.A. / No, not Yes

The bank form is written for **SFTP / landing-pickup-archive** payment files. This platform’s
**premium collection** is not that shape:

| Path | Style | In this UAT CR? |
|---|---|---|
| Session create (S-13) | Sync HTTPS via Apigee → AU Bank PG | Design only; **exclude** unless Payments + Deepali sit |
| Customer pays (C4) | Customer device → PG only; **never** RM/bank-employee device | Standing constraint — not waivable |
| Authorisation callback (S-14 / I-05) | HTTPS on a **separate** API Gateway route (`TB-6`) | Design only |
| Settlement (S-15 / I-06) | File **or** S3 drop, out of band | **Not this change** |
| Off-platform / MIS ingest (CR-013) | Policy ingest, maker-checker; not `lead.create` | R0 *scope* on paper; **not** UAT env evidence |

Ticking 10a–e **Yes** would describe a file hop we do not run. Ticking them **N.A.** without the
remark would hide settlement/MIS for a later CR. Use the wording in the table.

---

## 3. Scheduler / dual-instance — why Architecture blocks a Yes

Two different schedulers get conflated on this form:

1. **WS-1 job poller** (quote/proposal async-poll). Single-process today. Multi-instance ownership
   is a **known later** item (Phase 5.4), not a UAT defect to “fix in this CR”.
2. **Payment settlement scheduler** (S-15). Designed, not built. Dual-instance control is a
   **Payments + SRE** problem when that job exists.

CAB item 11 is a **production payment-system** control. Applying it to a quote+proposal UAT entry
is the wrong shape. If someone expands this CR to “payment system UAT”, the verdict stays **No**
until 11a is designed **and** the named humans in 11b have signed.

---

## 4. Conditions — become acceptance criteria if a human adopts a *limited* UAT entry

These are **conditions**, not a signature.

1. **Human T4 Architecture** signs this draft or a successor. This file is not that signature.
2. CR title and ITSM remarks say **CONDITIONAL**. Unconditional Business + Tech sign-off (row 5)
   stays **No**.
3. **Named scope on the CR:** Term (and, if Product adds them, documented Savings/ULIP)
   **quote + proposal** only. Payment URL issuance into an RM session, customer-device payment
   go-live, FUNC-008, issuance and recon are **out**.
4. **No regulated / production-like PII** in the environment used. Render.com is not the UAT
   path. India-region residency remains unattested until S09-G9 — Shailja + Shivanshi.
5. **Single instance** for job-store / payment workers. No dual-instance claim (row 11a).
6. **File-payment rows 10a–i** recorded as **N.A.** with the remarks above, unless a settlement
   or MIS file is in scope — in which case stop and retake Boards 4, 6 and Aarti.
7. **Sanity sheet** executed by Swapnali covering the seven items in row 4; gate 4.1 remains
   the platform’s own UAT-quality bar and is still BLOCKED.
8. **Build identity** (SHA / digest / versions) attached by Amit + Shivanshi (row 7).
9. **Deepali** confirms no AppSec exception is implied by “N.A.” on row 8. VA/PT is not passed.
10. **Rajal** owns `DEP-002`: a named bank-caller slot or a written refusal. Silence is not a slot.
11. **Shivanshi** attaches the implementation + rollback steps (row 2) and does **not** claim
    DC/DR sync (row 6).
12. No agent or architect edits `current_phase` / `stage_status` / `GATE-P4` to `PASSED` because
    this form was filled.

---

## 5. Board 1 checks (A1–A10) against *signing this form as Yes*

| # | Check | Finding |
|---|---|---|
| A1–A2 | Boundaries / responsibility | Filling file-payment and dual-instance rows as Yes would **put payment-file operations in the wrong component**. Premium is C4 + PG API. |
| A4 | Standing constraints | Unconditional UAT that uses Render or loads PII, or that lets a bank app call 1SB/DB, is a constraint breach. |
| A6 | Unnecessary infrastructure | Do not invent SFTP landing/pickup just to tick row 10. |
| A8 | Stage fit | S09 complexity (real UAT accounts, DR sync, tested rollback) is **the next stage**, not a paper we already have. Claiming it now imports a later stage (`A1`). |
| A9 | Smallest change | Smallest honest CR: conditional UAT *entry* for quote+proposal. Full “UAT env sign-off” as the form is written is larger than the evidence. |

---

## 6. Who else must sit (Architecture will not aggregate their silence)

| Seat | Why this form needs them | May they be skipped? |
|---|---|---|
| **Rajal (R1)** | Business description, `DEP-002`, unconditional vs conditional Product sign-off (row 1, 5) | No |
| **Amit (R3)** | Build id, implementation steps, single-instance job-store (rows 2, 7, 10g–h) | No |
| **Deepali (R8)** | Row 8 VA/PT / exception; trust boundary if PG callback is in scope | No for row 8 |
| **Aarti** | Persistence rollback; any settlement file (I-06) | Yes only if no file and no schema migrate |
| **Swapnali (R7)** | Row 4 sanity evidence | No |
| **Shailja (R9)** | PII in UAT, row 10f, residency | No if any real customer data |
| **Shivanshi (R10)** | Rows 2, 6, 7, 11a — environment, rollback, DC/DR | No |
| **Kalpana (R12)** | ITSM / CAB orchestration; does **not** convert this draft into approval | Coordinates only |
| **Named payment owners (row 11b)** | Only if scheduler / multi-instance payment is in the CR | N.A. if money movement is excluded |

---

## 7. Signature status

`AI-DRAFTED`. Board 1 has not sat. Agent must not treat this as `APPROVED`.

```yaml
review:
  board: ARCHITECTURE
  reviewer: "Mahesh / Principal Insurance Platform Architect"
  reviewer_type: AGENT
  self_review: false
  plan: CAB-UAT-ENV-SIGNOFF
  decision: REWORK
  decision_if_scope_narrowed: APPROVED_WITH_CONDITIONS
  architecture_severity: A1
  must_fix:
    - "Do not submit the CAB form with unconditional Yes on rows 2, 5, 6, 10a-i, 11a, 11b"
    - "Record rows 10 and 11 as N.A. unless settlement/scheduler is explicitly in scope — then they are No"
    - "Attach Swapnali sanity evidence, Amit/Shivanshi build+rollback, Rajal DEP-002 outcome"
  conditions:
    - "See §4 — twelve conditions for a limited quote+proposal UAT entry only"
  evidence:
    - "docs/context/BOOT.md §5 (GATE-P4 BLOCKED, S09 next, standing constraints)"
    - "docs/governance/state/GATE-EVIDENCE.yaml GATE-P4"
    - "docs/application-lifecycle-bible/stages/S09-platform-foundation.md §6"
    - "docs/architecture/ARB-PREREQUISITE-PACK.md §0 rows 6, 8, 9, 11"
    - "docs/architecture/arb-prerequisites/INTEGRATION-AND-DEPENDENCY-MATRIX.md I-03…I-06"
    - "docs/platform/ws3-platform/03-solution-architecture-r0.md S-13…S-15, §5.2"
    - "docs/1sb-insurance-integration/field-guides/payment.md"
    - "docs/governance/registers/DEPENDENCY-REGISTER.md DEP-001 DEP-002 DEP-003"
    - "docs/1sb-insurance-integration/service-ssot/TEST-BACKLOG.md QA-009"
    - "docs/governance/11-REVIEW_GATES.md §2 §4 §11"
    - "docs/context/personas/mahesh-architecture.card.md"
  notes: >
    Fill the bank form with the Yes/No/N.A. column in §1. Do not convert REWORK into
    a UAT environment sign-off. Human Mahesh still has to sit T4.
  date: 2026-09-24
```
