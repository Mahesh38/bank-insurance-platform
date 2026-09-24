# ADR-021 — Bank screen descriptor for NIP-APP (form, list, card, carousel)

**Status:** Proposed (`A3_JOINT_REVIEW` — Board 1; Board 4 on icon URL / PII)  
**Date:** 2026-09-23 · **Amended:** 2026-09-24 (runtime: store / L1+L2 / action bind / capture)  
**Deciders:** Mahesh (Architecture) — draft. Human T4 outstanding.  
**Consulted:** Amit (Engineering), Rajal (Product outcome), Deepali (icon URL / no raw S3), Aarti (physical capture)  
**Workstream:** WS-3  
**Stage:** S08 documentation · binds at S11 first store submit  
**Origin:** `SUG-20260923-sdu` · `ARCH-026` · `PLAN-006`

## Context

NIP-APP will ship on web, iOS and Android. Store approval is ~4 days. Product requires that
adding a LOB tile, icon, capture field or validation rule must not force a three-platform
resubmit. Insurer proposal forms already use a GET-schema / POST-values pattern
(`proposal-and-dynamic-forms.md`, S05 `SCR-13`). Bank RM screens had no equivalent.

Nested dependence is required: selecting X (radio / checkbox / dropdown) reveals Y; selecting
Y reveals Z. Free-text fields need required/optional, format (number, email, mobile, regex),
min/max length and min/max value. Frontend asked for **one** response family for form,
carousel, list and card.

Constraints that stay true: UI → NIP BFF only; no 1SB types on the device; unwrapped success
bodies; ADR-017 errors; no raw S3; Figma is reference (A11).

## Decision

We will publish a **closed** bank `ScreenDocument`:

1. Surfaces are `FORM` | `LIST` | `CARD` | `CAROUSEL` only.  
2. The only interactive atom is `Field` with a **closed** `widget` enum.  
3. Nested dependence is `options[].reveals` / `field.reveals` (max depth 3) plus
   `visibleWhen` / `requiredWhen` / `enabledWhen` predicates.  
4. Validation lives on the field; client and server use the same object.  
5. Submit is a flat `values` map against `version` **and** `actionId`. Hidden fields are ignored.
   Every GET carries `submission` — the blank POST the client clones (`href` + `body`). Writable
   `values` keys are listed with blank tokens so a new field is configuration, not a client change.  
6. `iconUrl` is https on the bank CDN/BFF.  
7. A new **widget** is a store release. A new **field** using a shipped widget is configuration.  
8. The **definition** (`SCREEN_DOCUMENT`) and the **binding** (`SCREEN_ACTION`) are two payloads
   in Configuration #19 (`administration.configuration_record`, `ADR-007`, `CF-2`). They do not
   open a form microservice.  
9. Submit is **not** “post JSON somewhere”. `actionId` resolves a closed `command`
   (`CREATE_LEAD`, `RESUME_LEAD`, `ASSIGN_LEAD`, `SELECT_PRODUCT_CLASS` in R0) owned by a named
   bounded context. Flutter never sees `command` or `ownerContext`.  
10. Validation is **two layers**: L1 schema in a shared `FormRuntime` lib; L2 invariants on the
    owning service. Client validation is UX only.  
11. One accepted submit writes **SoR columns** + an append-only `screen_submission` capture +
    an audit outbox event. Later steps read promoted columns, not the widget tree.

Canonical wire: [`10-nip-bff-screen-descriptor.md`](../ws3-platform/10-nip-bff-screen-descriptor.md).  
Canonical runtime: [`11-nip-bff-screen-runtime.md`](../ws3-platform/11-nip-bff-screen-runtime.md).  
Machine contract: [`nip-bff-screen-descriptor.openapi.yaml`](../ws3-platform/nip-bff-screen-descriptor.openapi.yaml).

This does **not** replace typed resources already published (`SearchPage`, `PipelinePage`,
`LeadCreated`). New capture / catalogue-picker screens use `ScreenDocument`. Login, pipeline
and payment-status stay typed.

## Alternatives considered

| Option | Why not |
|--------|---------|
| Hardcode every screen in Flutter | Violates the Product no-resubmit outcome; every field add is a store cycle |
| Copy 1SB GET-schema onto the BFF | Standing constraint: UI never speaks 1SB (`SUG-20260913-acl`) |
| Unbounded widget plugins / JS from the server | App-store and security reject; not reversible |
| Only `visibleWhen` predicates, no `reveals` | X→Y→Z is unreadable; both are needed |
| One `{success,data,message}` envelope | Rejected in lead LLD §2.1 / ADR-017 |
| Do nothing until S11 codes the first form | Shipping a hardcoded client makes the later descriptor a migration (X3) |
| A Form microservice that owns Lead / Journey state | Crosses context ownership; Lead still decides assignment (`07` §2) |
| Client invents the POST URL or the BFF runs SQL | Standing constraint; closed `command` enum only |
| Treat capture `values_json` as Suitability input | Promote first (`OPEN-SCR-PROMOTE`) |

## Consequences

**Positive**
- Web / iOS / Android share one renderer.
- Catalogue and validation changes are Configuration, not binaries.
- Nested dependence and formats are specified once.
- A field add is a seed. A **new command** is a service deploy — the store is not a back door
  into new domain behaviour.

**Negative / accepted costs**
- First client must ship the closed widget set (one store submit).
- Depth cap 3; deeper journeys become a second screen.
- Existing typed search/pipeline APIs are not rewritten in this ADR.
- CF-2 gains two enumerated domains (`SCREEN_DOCUMENT`, `SCREEN_ACTION`); that is an
  amendment of ADR-007's closed list, not a second store.

**Constrains future work**
- A second form DSL or 1SB envelope on Flutter is SF4 / REJECT.
- Raw S3 on `iconUrl` is SF4 / REJECT.
- Interpreting login / pipeline / payment as FORM stays out of `ARCH-026`.
- A Form service that owns insurance state is SF4 / REJECT.
- An arbitrary URL or script inside `ScreenDocument` is SF4 / REJECT.

## Reversibility

| Question | Answer |
|----------|--------|
| Cost to reverse | medium after first store client; low while docs-only |
| What makes it expensive | shipped widget enum + published `version`s |
| Point of no return | first NIP-APP store build that implements this OpenAPI |

## Revalidation triggers

- A required widget not in the closed enum (then a store release, not a silent add)
- Board 4 rejects CDN icon hosting
- Product requires nesting deeper than 3 on one screen
- Aarti rejects `values_json` capture in favour of a fully columnar model
- A required R0 command is missing from the closed catalogue (then a service deploy + seed)

## Compliance and security impact

- Regulatory obligations touched: none new; PAN format may exist but must not be echoed
- Security posture change: server-driven UI; treat `optionsUrl` as same-origin BFF only
- Audit: submissions of assignment remain material actions (`BR-SEC-030`); capture row stores
  `config_version` (`INV-CFG-03`)
- PII: `values_json` is restricted at the DB; no PAN/mobile in logs (Deepali)
