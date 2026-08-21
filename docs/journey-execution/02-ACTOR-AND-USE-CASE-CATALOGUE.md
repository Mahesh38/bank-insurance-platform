# 02 — Actor and Use Case Catalogue

**Every actor in R0, every use case each one can reach, and the id that names it.**

Status: `AI-DRAFTED` · Owner: Mahesh (Board 1) · Origin: `SUG-20260821-jx1`

---

## 1. Actors

R0 has **two on-platform human actors** plus service principals. The customer is a *participant*,
not an actor: their device receives an OTP and a payment link and touches no platform service.
Source: [`R0-HLD §2.1`](../architecture/R0-HLD.md#21-two-actors-one-of-them-sells),
[`04-security-architecture §3`](../platform/ws3-platform/04-security-architecture.md#3-identity-architecture--four-principal-classes).

| Actor | `actorType` | Identity plane | May do | Never does |
|---|---|---|---|---|
| **Bank RM** | `BANK_RM` | Workforce, bank AD federated via Keycloak | Sole origination. Every regulated action. Accountable Specified Person for the record's life | Never anonymous. Never replaceable as the accountable SP |
| **Insurance Partner Rep** | `INSURER_PARTNER_REP` | Partner, maker-checker provisioned | Assist only: own-insurer product view/select, gated journey read, annotations | Never an SP. No origination, no advice, no regulated action |
| **Service principal** | `SERVICE` | IRSA / service identity | Internal calls, jobs, outbox delivery | Never originates a business record on a human's behalf |
| *Customer* | — | none | Receives OTP · pays on **their own** device | **Never** holds a platform session in R0 |

The vocabulary is **closed at three values**. Adding an actor type is an authorization change, not
a new service (`AC-3`).

**Specified Person is a certification attribute on the RM principal**, not an actor and not a
channel (`AC-1`, `ADR-004`). It is evaluated **at the instant of each regulated action**, not at
login. Expired, suspended or out-of-LOB certification **fails closed**.

### 1.1 IPR visibility is a persistence predicate, not a hidden button

```text
visible_to_IPR(record) := record.opportunityCreatedBy.actorType = BANK_RM
                       AND record.insurerId = principal.insurerId
                       AND record.sharedWithPartner = TRUE
                       AND record.branchCode ∈ principal.branchScope
```

Applied at **L7** as a mandatory query predicate (`AC-4`, `AC-5`, `INV-LED-07`). A row failing it is
**absent**, never a `403` naming the id. Cross-insurer partner access is *always* denied
([`authentication-authorization §8.3`](../platform/authentication-authorization/README.md#83-precedence)).

---

## 2. Use case catalogue

`Slice` is the delivery slice from [`README §2`](./README.md#2-delivery-status).
✅ specified · ⬜ not started.

### Group A — Access and session

| UC | Use case | Actors | Entry | Slice |
|---|---|---|---|---|
| ✅ `UC-01` | [RM login](./flows/UC-01-rm-login.md) | RM | `POST /api/v1/auth/login` → `GET /api/v1/auth/callback` | 1 |
| ✅ `UC-02` | [IPR login](./flows/UC-02-ipr-login.md) | IPR | same endpoints, partner realm path | 1 |
| ✅ `UC-03` | [Session status and refresh](./flows/UC-03-session-status-and-refresh.md) | RM, IPR | `GET /api/v1/auth/session` | 1 |
| ✅ `UC-04` | [Logout, disablement and revocation](./flows/UC-04-logout-and-revocation.md) | RM, IPR, admin | `POST /api/v1/auth/logout` | 1 |
| ✅ `UC-05` | [Authorization decision (PDP)](./flows/UC-05-authorization-decision.md) | service | `POST /internal/v1/authorization/decisions` | 1 |
| ⬜ `UC-06` | Principal snapshot — `GET /me` incl. SP certification state | RM, IPR | `GET /me` | 2 |

### Group B — Origination

| UC | Use case | Actors | Entry | Slice |
|---|---|---|---|---|
| ⬜ `UC-07` | Create opportunity (**RM only**) | RM | `POST /opportunities` | 2 |
| ⬜ `UC-08` | Customer lookup from CBS | RM (via Journey) | `S-04` → `S-05` | 2 |
| ⬜ `UC-09` | Start journey from a `QUALIFIED` opportunity | RM | `POST /opportunities/{leadId}/journeys` | 2 |

### Group C — Advisory and suitability

| UC | Use case | Actors | Entry | Slice |
|---|---|---|---|---|
| ⬜ `UC-10` | Complete need analysis | RM | `POST /journeys/{id}/need-analysis` | 2 |
| ⬜ `UC-11` | Evaluate suitability (**C1 producer**) | RM | `POST /journeys/{id}/suitability` | 2 |
| ⬜ `UC-12` | Override a suitability outcome | RM | suitability aggregate transition | 2 |

### Group D — Consent

| UC | Use case | Actors | Entry | Slice |
|---|---|---|---|---|
| ⬜ `UC-13` | Issue consent OTP challenge to the **customer device** | RM | `POST /journeys/{id}/consent/challenge` | 2 |
| ⬜ `UC-14` | Verify OTP and bind the grant (**C2 producer**) | RM | `POST /journeys/{id}/consent/verify` | 2 |

### Group E — Quotation

| UC | Use case | Actors | Entry | Slice |
|---|---|---|---|---|
| ⬜ `UC-15` | Create quote — **C1 gate**, async-poll to 1SB | RM | `POST /journeys/{id}/quotes` | 3 |
| ⬜ `UC-16` | Poll quote; partial success is success | RM | `GET …/quotes/{quoteId}` | 3 |
| ⬜ `UC-17` | Select an offer | RM | `POST …/quotes/{quoteId}/selection` | 3 |
| ⬜ `UC-18` | Browse the catalogue / offerings matrix | RM, IPR | `GET /catalogue/offerings` | 3 |

### Group F — Proposal and underwriting

| UC | Use case | Actors | Entry | Slice |
|---|---|---|---|---|
| ⬜ `UC-19` | Create proposal draft from the selected offer | RM | `POST /journeys/{id}/proposals` | 3 |
| ⬜ `UC-20` | Submit proposal — **C2 gate**, no auto-retry on submit | RM | `POST …/proposals/{id}/submit` | 3 |
| ⬜ `UC-21` | Track underwriting status | RM, IPR (gated) | `GET …/proposals/{id}` | 3 |

### Group G — Payment

| UC | Use case | Actors | Entry | Slice |
|---|---|---|---|---|
| ⬜ `UC-22` | Create payment; link goes to the **customer device** (**C4**) | RM | `POST /journeys/{id}/payments` | 4 |
| ⬜ `UC-23` | Customer pays on the hosted PG page | *customer* | outside the platform | 4 |
| ⬜ `UC-24` | PG authorisation callback | PG (`SERVICE`) | separate API GW route, IP-allowlisted | 4 |
| ⬜ `UC-25` | Settlement reconciliation batch | job | scheduled | 4 |
| ⬜ `UC-26` | Resolve an `UNCERTAIN` payment / `RECONCILIATION_BREAK` | ops | manual procedure F-07 | 4 |

### Group H — Policy and closure

| UC | Use case | Actors | Entry | Slice |
|---|---|---|---|---|
| ⬜ `UC-27` | Issue policy — only against a `RECONCILED` payment | service | `S-16` | 4 |
| ⬜ `UC-28` | Retrieve policy and documents | RM, IPR (gated) | `GET …/policies/{policyId}` | 4 |
| ⬜ `UC-29` | Journey reaches `SOLD` — **C8**, audit-complete | Journey | aggregate transition | 4 |

### Group I — Cross-cutting

| UC | Use case | Actors | Entry | Slice |
|---|---|---|---|---|
| ⬜ `UC-30` | Resolve configuration (**fail closed**) | service | `S-21` | 5 |
| ⬜ `UC-31` | Audit event delivery via transactional outbox | service | `S-17` | 5 |
| ⬜ `UC-32` | Notification delivery — never blocks the journey | service | `S-18` | 5 |
| ⬜ `UC-33` | Compensation on a failed journey | Journey | `S-19` | 5 |
| ⬜ `UC-34` | IPR gated read across the journey | IPR | `S-22` | 5 |
| ⬜ `UC-35` | Partner user provisioning under maker-checker | admin | `POST /internal/v1/partner-users` | 5 |

**35 use cases. 5 specified.**

---

## 3. Actor × use case reachability

The one-screen answer to "can this actor do this?". `✔` reachable · `gated` reachable subject to
the `AC-4` predicate · `—` not reachable at any layer.

| Use case group | Bank RM | Insurance Partner Rep | Service | Customer |
|---|:--:|:--:|:--:|:--:|
| A Access and session | ✔ | ✔ | ✔ (UC-05) | — |
| B Origination | ✔ | **—** `ORIGINATION_RM_ONLY` | — | — |
| C Advisory and suitability | ✔ | **—** `ASSIST_ONLY_ACTOR` | — | — |
| D Consent | ✔ | **—** | — | receives OTP on own device |
| E Quotation | ✔ | `UC-18` own-insurer only | — | — |
| F Proposal | ✔ | `UC-21` gated read | — | — |
| G Payment | ✔ create only | **—** | ✔ UC-24/25 | **pays on own device** |
| H Policy | ✔ | `UC-28` gated read | ✔ UC-27 | — |
| I Cross-cutting | — | `UC-34` | ✔ | — |

Every `—` in the IPR column is `INV-ACT-02` — default deny at the PDP, sourced from configuration,
not a hidden UI control.
