# Journey Execution Specification — R0

**How a request actually travels: every actor use case, every hop, the validation performed at
each layer with its algorithm, every external call, and every outcome the caller can observe.**

Owner: Mahesh — Principal Insurance Platform Architect (Board 1) · Origin: `SUG-20260821-jx1`
Status: **`AI-DRAFTED`.** Mandatory human T4 **Architecture** and **Security** sign-off outstanding.
Date: 2026-08-21 · Workstream: WS-3 (primary) · WS-2 (identity) · WS-1 (provider)

---

## 0. What this pack is, and what it is not

The architecture is decided and written down. What was missing is the **assembly**: no single
document said, for one request, *which layer enforces which rule, in what order, with what
algorithm, and what the caller sees when it fails.*

A developer implementing `POST /journeys/{id}/quotes` today can read that the C1 suitability gate
applies "via `S-08`" ([`R0-HLD §5.1`](../architecture/R0-HLD.md#51-public--bff--2-nip-bff))
and that `INV-QUO-01` is enforced at `QuotationService.request()`
([`01-domain-model §6.1`](../platform/ws3-platform/01-domain-model-and-invariants.md#61-compliance-hard-gates-s06-e03-s02)).
Neither source says whether the BFF should *also* check it, what "not `EXPIRED` at the instant of
the check" means when two clocks disagree, or what the RM sees. Three developers will answer those
three ways. That is what this pack removes.

> **This pack is not a source of truth.** Rule `HA-02` holds: where this pack and an authority
> document disagree, **the authority document wins** and the conflict is raised as a CR against
> this pack. Every rule here cites the source that owns it. Nothing here originates a decision.

| This pack answers | Authority that owns the underlying fact |
|---|---|
| Which layer enforces a rule, and in what order | [`01-domain-model §6.3`](../platform/ws3-platform/01-domain-model-and-invariants.md) placement summary |
| The algorithm a validation follows | Derived from the invariant assertion; pseudocode is this pack's contribution |
| The hop sequence and its timeouts | [`03-solution-architecture-r0 §5`](../platform/ws3-platform/03-solution-architecture-r0.md#5-seam-catalogue--synchronous-vs-asynchronous-with-semantics) seam catalogue |
| The proxy chain | [`R0-LLD §3`](../architecture/R0-LLD.md#3-reverse-proxy--external-and-internal-required) |
| The error a caller receives | [`R0-HLD §5.4`](../architecture/R0-HLD.md#54-typical-error-codes-stable) |
| Trust-boundary crossing rules | [`04-security-architecture §2`](../platform/ws3-platform/04-security-architecture.md#2-trust-boundaries) |
| Identity and session behaviour | [`authentication-authorization`](../platform/authentication-authorization/README.md) |

---

## 1. Read in this order

| # | File | What it gives you |
|---|---|---|
| 1 | [`01-REQUEST-LIFECYCLE-STANDARD.md`](./01-REQUEST-LIFECYCLE-STANDARD.md) | The eight-layer ladder every request runs. **Read once; every flow file assumes it.** |
| 2 | [`02-ACTOR-AND-USE-CASE-CATALOGUE.md`](./02-ACTOR-AND-USE-CASE-CATALOGUE.md) | Every actor, every R0 use case, `UC-nn` ids, and which are specified yet |
| 3 | [`03-VALIDATION-RULE-CATALOGUE.md`](./03-VALIDATION-RULE-CATALOGUE.md) | Every rule with its enforcement layer, algorithm and failure code |
| 4 | [`04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md`](./04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md) | Every error code: who emits it, what the client does, what the journey does |
| 5 | [`05-EXTERNAL-CALL-CATALOGUE.md`](./05-EXTERNAL-CALL-CATALOGUE.md) | Every call leaving the VPC: auth, timeout, retry, failure mapping |
| 6 | [`06-TRACEABILITY.md`](./06-TRACEABILITY.md) | `UC` → invariant → seam → error → test. The S03 RTM, per request |
| 7 | [`flows/`](./flows/) | One file per use case: hops, per-layer validation, outcomes, audit events |

**Building a story?** Open its `flows/UC-nn-*.md` and `03-VALIDATION-RULE-CATALOGUE.md`. Nothing else.
**Writing tests?** The *Outcomes* section of a flow file is the test case list, by construction.

---

## 2. Delivery status

This pack is built in slices so the format is corrected once, not thirty times.

| Slice | Contents | Status |
|---|---|---|
| **1** | All seven framework files + `UC-01`…`UC-05` (access and session) | **Delivered** — this change |
| 2 | `UC-06`…`UC-14` — origination, advisory, suitability, consent | Not started |
| 3 | `UC-15`…`UC-21` — quotation and proposal, incl. 1SB partial-success | Not started |
| 4 | `UC-22`…`UC-29` — payment, reconciliation, issuance, closure | Not started |
| 5 | `UC-30`…`UC-34` — configuration, audit, notification, compensation, gated partner read | Not started |

Rules for the whole R0 rule set are enumerated in `03-VALIDATION-RULE-CATALOGUE.md` from slice 1,
with pseudocode filled in as each slice lands. A rule with `pseudocode: pending` is **specified**
(layer, source, failure code are binding) but **not yet expanded**.

Out of scope for every slice: the DIY / customer journey, hybrid mode switching, Group B insurers,
ULIP and Savings, Health / Motor / Travel, renewals and servicing, the admin UI, and reporting
beyond the pilot funnel. All sit in `out_of_scope_now`; specifying them is parked as
`SUG-20260821-jx2`.

---

## 3. Conventions

| Convention | Meaning |
|---|---|
| `L0`…`L7` | Layer in the request ladder — see `01-REQUEST-LIFECYCLE-STANDARD.md §1` |
| `UC-nn` | Use case. Stable id; never renumbered |
| `VR-nnn` | Validation rule in this pack. Always cites the `INV-*` or control that owns it |
| `S-nn` | Seam, from the architecture seam catalogue. Not minted here |
| `INV-*` | Domain invariant. Not minted here |
| `C1`…`C8` | Compliance hard gate. Not minted here |
| **fail closed** | On dependency failure the action is refused. Never an allow, never a default |
| *(pending)* | Structurally specified, content arriving in a later slice |
