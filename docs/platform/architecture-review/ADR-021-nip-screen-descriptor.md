# ADR-021 — Bank screen descriptor for NIP-APP (form, list, card, carousel)

**Status:** Proposed (`A3_JOINT_REVIEW` — Board 1; Board 4 on icon URL / PII)  
**Date:** 2026-09-23  
**Deciders:** Mahesh (Architecture) — draft. Human T4 outstanding.  
**Consulted:** Amit (Engineering), Rajal (Product outcome), Deepali (icon URL / no raw S3)  
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
5. Submit is a flat `values` map against `version`. Hidden fields are ignored.  
6. `iconUrl` is https on the bank CDN/BFF.  
7. A new **widget** is a store release. A new **field** using a shipped widget is configuration.

Canonical prose: [`10-nip-bff-screen-descriptor.md`](../ws3-platform/10-nip-bff-screen-descriptor.md).  
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

## Consequences

**Positive**
- Web / iOS / Android share one renderer.
- Catalogue and validation changes are Configuration, not binaries.
- Nested dependence and formats are specified once.

**Negative / accepted costs**
- First client must ship the closed widget set (one store submit).
- Depth cap 3; deeper journeys become a second screen.
- Existing typed search/pipeline APIs are not rewritten in this ADR.

**Constrains future work**
- A second form DSL or 1SB envelope on Flutter is SF4 / REJECT.
- Raw S3 on `iconUrl` is SF4 / REJECT.
- Interpreting login / pipeline / payment as FORM stays out of `ARCH-026`.

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

## Compliance and security impact

- Regulatory obligations touched: none new; PAN format may exist but must not be echoed
- Security posture change: server-driven UI; treat `optionsUrl` as same-origin BFF only
- Audit: submissions of assignment remain material actions (`BR-SEC-030`)
