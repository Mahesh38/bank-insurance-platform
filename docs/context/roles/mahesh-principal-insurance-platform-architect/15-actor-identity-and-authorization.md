# 15 — Mahesh Actor Identity and Authorization Doctrine

## 1. Purpose

Two things must be true at every horizon, and they pull in opposite directions if not stated
carefully:

1. **The bank's Active Directory remains the authoritative source of workforce identity** — the
   existing integration is kept, not replaced (`TI-01`).
2. **The platform must serve actors AD will never contain** — customers on a DIY journey, insurer
   representatives, certified sales partners, call-centre staff at a vendor, and services
   authenticating to each other (`VIN-001 §23`, `§24`).

Resolving that tension by putting non-workforce identities into AD, or by building a second
workforce directory, are the two failure modes this file exists to prevent.

---

## 2. The Active Directory invariant

**`TI-01` — Workforce identity stays federated to Bank Active Directory. The platform never
masters, stores or validates workforce credentials, at any horizon.**

This is not a preference and not a phase-1 convenience. It is the accepted baseline in
[`authentication-authorization/README.md`](../../../platform/authentication-authorization/README.md)
and it is preserved as-is:

```text
Flutter / workforce channel
   │  TLS · opaque session only — no OAuth tokens ever reach the device
   ▼
API Gateway + WAF                      (the only public entry point)
   ▼
workforce-access-bff                   (token-hiding session; tokens in the server-side vault)
   ├── identity-provider-adapter-service ──▶ Keycloak ──OIDC / SAML / LDAP──▶ Bank Active Directory
   └── identity-authorization-service (PDP) ──▶ default-deny RBAC + ABAC + relationship decision
```

### 2.1 The rules that keep it true

| ID | Rule | Source |
|---|---|---|
| **ID-01** | AD is the **system of record** for workforce identity and employment status. The platform mirrors selected attributes; it never masters them | `authentication-authorization §1.7`, `§7` |
| **ID-02** | The authentication ceremony is **bank-controlled**. The platform never validates an AD password and never persists one | `§4.1` |
| **ID-03** | **No credential forwarding.** The architecture does not assume an AD username/password can be replayed through OIDC; direct grant stays disabled unless the bank explicitly approves an LDAP/direct-grant arrangement | `§5.1` |
| **ID-04** | **Provider neutrality.** Keycloak is the initial implementation, not the architecture. BFF and PDP contracts must not change if the AD protocol, the broker or the IdP product changes | `ARCH-018`, `§1.3`, `§1.7` |
| **ID-05** | **Token-hiding session.** The device holds an opaque handle; provider tokens live encrypted server-side and are never logged | `ARCH-019`, `§6` |
| **ID-06** | **IdP claims are never business authorization.** Keycloak owns credentials, ceremonies, MFA and token issuance. The PDP owns business authorization, and it is the business source of truth | `§1.4`, `§1.5` |
| **ID-07** | **Default deny, fail closed.** The PDP is queried at 300 ms with no retry; unavailable means denied. Not degradable under load | `S-02`, `ARCH-020` |
| **ID-08** | **Defence in depth.** Business services re-check authorization; the BFF's check is not the only one | `§3` |
| **ID-09** | **Disablement propagates.** Revocation in AD terminates platform sessions within a stated SLA; refresh-token reuse detection terminates the session family | `§5.3`, `§6` |
| **ID-10** | **Only Gateway and BFF are in the public path.** Adapter, PDP, Keycloak, databases and business services stay private | `§3` |

**Rule ID-11 — any change to this baseline is `A3_JOINT_REVIEW` with Deepali and the Security
Board.** Mahesh may propose; he does not adjust the trust boundary alone.

### 2.2 What is still open

Carried from the auth SSOT §15 rather than assumed closed:

| Open item | Owner |
|---|---|
| Exact bank AD protocol (OIDC / SAML / LDAP) and authoritative attribute names | Bank IT + Deepali |
| Which RM certificate attributes AD exposes, and reconciliation frequency | Bank IT + Shailja |
| Whether any approved direct-grant arrangement exists | Bank IT + Deepali |

**`ID-04` is what makes these safe to leave open.** Because provider specifics are isolated behind
the adapter, the answer changes a configuration and an adapter — not the BFF contract, not the PDP,
and not a single business service.

---

## 3. Four identity planes

**Rule ID-12 — workforce, customer, partner and service identity are four separate planes. They are
never merged into one directory.**

| Plane | Who | Source of truth | Horizon | Never |
|---|---|---|---|---|
| **Workforce** | Bank employees: RM, branch, ops, admin, call centre (bank-employed) | **Bank AD**, brokered | H0 | Never mastered in the platform |
| **Customer** | Retail customers on DIY and hybrid journeys | Platform customer identity, linked to `CAP-101` Party | H1 | **Never in AD.** Putting customers in the corporate directory is a security and lifecycle failure with no upside |
| **Partner** | Insurance Partner Representatives (insurer employees), externally certified sales partners, outsourced call-centre agents | Platform `identity-authorization-service`, provisioned to the IdP after **maker-checker**; separate realm/broker | H0 (IPR) / H1 (external certified partners) | Never federated into bank AD; never granted a regulated action without the certification the action requires |
| **Service** | Workloads authenticating to each other | Workload identity + mTLS, secrets/KMS | H0 | Never a human account, never a shared credential |

`ARCH-022` already records the partner pattern: partner identities are created in Identity & Access
and provisioned to the IdP after maker-checker approval, with RM certification sourced from AD and
insurer-representative certification admin-uploaded in Phase 1.

**Rule ID-13 — one PDP across all four planes.** Separate identity planes, **one** authorization
decision point. Four planes with four authorization models produces four places to get default-deny
wrong.

**Rule ID-14 — customer identity is a bounded context, and it is not a small one.** Registration,
verification, recovery, device binding, step-up for payment and consent, and the link from customer
identity to `CAP-101` Party. It is an H1 entry condition for DIY, not a login screen.

---

## 4. The actor capability model

`VIN-001 §24`, adopted:

> **Authentication tells us who you are. Authorization must understand: role, actor type,
> certification, LOB, branch, assigned customer/lead, journey, current stage, requested action.
> Therefore permission is a backend business control, not a UI hide/show rule.**

**Rule ID-15 — the authorization decision input carries the full actor and resource context.** The
existing PDP contract already carries subject, action, resource (with branch, insurer, owner,
assignees, sharing) and channel context. The target state extends the same shape rather than
inventing a second one:

```yaml
authorization_request:
  subject:
    subjectId: "..."
    actorType: BANK_RM | BANK_EMPLOYEE | CUSTOMER | INSURER_PARTNER_REP | CALL_CENTRE_AGENT | SERVICE
    roles: []
    certifications: []          # type, issuer, validity window — see §5
    branchCodes: []
    insurerCodes: []
  action: "quote.create" | "proposal.submit" | "consent.capture" | ...
  resource:
    type: JOURNEY | OPPORTUNITY | QUOTE | PROPOSAL | POLICY | PARTY | ...
    id: "..."
    lob: LIFE | HEALTH | GENERAL
    ownerId: "..."
    assignedUserIds: []
    branchCode: "..."
  context:
    channel: RM_WORKSPACE | PARTNER_WORKSPACE | CUSTOMER_DIY | CALL_CENTRE | BRANCH
    journeyStage: "..."          # some actions are only lawful at some stages
    assistanceMode: "..."
    correlationId: "..."
```

**Rule ID-15a — a certification is never an actor type and never a channel.** `CERTIFIED_SP` was
listed in both enumerations above until 2026-08-20 and is removed from both. The bank RM **is** the
certified Specified Person; SP is certification state on that principal — certificate number,
issuing authority, LOB scope, validity window, status — carried in `subject.certifications` and
evaluated per action (`ID-20`). A certification modelled as an actor produces two principals for one
human and two attribution trails for one sale, and it makes "may assist but may not sell"
inexpressible. An externally certified *sales partner* at a later horizon is a partner-plane actor
type that also holds a certification; that is a different thing from the RM's SP status, and it
does not license reinstating `CERTIFIED_SP` as a synonym for either.

**Rule ID-15b — `INSURER_PARTNER_REP` is assist-only and insurer-scoped.** The insurer's employee
assists an RM or a customer; they are not a Specified Person, hold no regulated-sales action, and
see only their own insurer's records, only once the RM has completed need analysis and suitability.
The accountable SP on a record is the originating RM and is immutable. Normative statement:
[`ws3-platform/01 §2.4`](../../../platform/ws3-platform/01-domain-model-and-invariants.md); decision
`ADR-004`.

**Rule ID-16 — precedence is fixed and non-negotiable:**

```text
account/global suspension  >  explicit scoped deny  >  direct scoped grant
                           >  role-derived scoped grant  >  default deny
```

**Rule ID-17 — permissions describe business actions, not URLs.** `quote.create`, not
`POST /v1/quotes`. A permission model shaped like the API becomes wrong the first time the API is
refactored, and it cannot express "may assist but may not sell".

**Rule ID-18 — the journey stage is an authorization input.** Some actions are lawful only at some
stages. Stage-blind authorization is how a proposal gets submitted for a journey with no valid
suitability assessment, and it turns `C1` into a UI convention.

**Rule ID-19 — attribution is derived from the authenticated actor, never from the request**
(`TI-10`). `distributorId`, `agentId`, `channel` and `actorType` are injected server-side; a
caller-supplied value is **rejected**, not overwritten. `FF-13` tests this by injecting one.

---

## 5. Certification-gated activity

`VIN-001 §23`: a non-certified agent may assist navigation and arrange a callback; a certified actor
may perform regulated sales activities permitted by the approved business and compliance model.

**Rule ID-20 — certification is an authorization attribute with a validity window.** Type, issuing
authority, LOB scope, valid-from, valid-to. An expired certification fails closed on regulated
actions — it does not warn and proceed.

**Rule ID-21 — Mahesh builds the gate; Shailja sets the threshold.** *Which actions require which
certification* is a compliance determination. Mahesh does not infer it from a product requirement,
and he does not ship a default that guesses. Recorded as open item 6 in
[`10 §9.1`](./10-north-star-capability-model.md).

**Rule ID-22 — the same capability serves every actor** (`TI-14`, `JS-08`). There is no
`call-center-quote-service`. Quotation is one capability whose permitted actions differ by actor
type and certification. A second implementation per actor is where two actors' rules silently
diverge.

**Rule ID-23 — actor change during a journey is an authorized, audited transition.** Handover
(`JS-06`) re-evaluates authorization for the new actor against the journey's current stage. An RM
inheriting a journey does not inherit the customer's permissions, and a customer resuming does not
inherit the RM's.

---

## 6. Evolution across horizons

| Horizon | What changes | What must not change |
|---|---|---|
| **H0** | Workforce via AD federation; insurer reps as partner identities; PDP live and fail-closed | `ID-01`–`ID-11` |
| **H1** | Customer identity context for DIY; call-centre actor type; externally certified sales-partner actor type in the partner plane; certification enforcement widens | AD stays authoritative for workforce; customers never enter AD; one PDP; a certification never becomes an actor type (`ID-15a`) |
| **H2** | LOB scope becomes a real authorization dimension as Health arrives; hybrid handover authorization | Precedence order; default deny; server-side attribution |
| **H3** | Additional partner channels; broader delegated administration | Four planes stay separate; no shared credentials; no second PDP |

**Rule ID-24 — the identity architecture is horizon-stable by design.** If a horizon requires
changing `ID-01`–`ID-11`, the change is a security-board decision with an ADR, not an implementation
detail — and Mahesh says so before the work is scheduled.

---

## 7. Anti-patterns

| Anti-pattern | Consequence | Correct move |
|---|---|---|
| Putting customer identities in AD | Corporate directory carrying retail lifecycle and breach exposure | `ID-12` — separate plane |
| Building a second workforce directory in the platform | Two truths about employment status; leavers stay enabled | `ID-01` |
| Trusting IdP claims as business authorization | Roles from the directory become entitlements nobody governed | `ID-06` |
| Tokens on the device | Exfiltration surface; provider migration becomes a client release | `ID-05` |
| Degrading the PDP under load | Authorization absent exactly when the system is stressed | `ID-07` |
| UI hide/show as the control | One HTTP client away from absent | `TI-15`, `ID-17` |
| Permissions modelled on URLs | Breaks on refactor; cannot express assist-but-not-sell | `ID-17` |
| Stage-blind authorization | `C1` becomes bypassable | `ID-18` |
| Caller-supplied `distributorId`/`agentId` | Attribution forgeable; `C3` defeated | `ID-19`, `FF-13` |
| A service per actor type | Divergent rules per channel | `ID-22` |
| Certification checked at login only | An expiry mid-journey goes unnoticed | `ID-20` — check at the action |
| A certification modelled as an actor type or a channel | Two principals and two attribution trails for one human; assist-but-not-sell becomes inexpressible | `ID-15a` — certification is an attribute on the principal |
| A partner principal scoped in the service or presentation tier | One forgotten predicate is a silent cross-insurer disclosure | `ID-15b`, `AC-5` — scope where the query is built |
| Shared service credentials | No attribution, no revocation, no blast-radius control | `ID-12` service plane |

---

## 8. Authority

| Decision | Authority |
|---|---|
| Authorization model shape, permission vocabulary, PDP integration pattern | `A1_AUTONOMOUS` — Mahesh, ADR when durable |
| Adding an actor type to the model | `A2_NOTIFY`; **Product owns whether the bank supports that actor** |
| Any change to authentication, federation, trust boundaries, token handling, credential isolation | `A3_JOINT_REVIEW` — **Deepali / Security Board** |
| Which actions require which certification | **Shailja** |
| Retention of authentication and administrative events | **Shailja** (currently 7 years, configurable pending confirmation) |
| Customer identity design (registration, verification, recovery, step-up) | `A3_JOINT_REVIEW` — Deepali + Shailja |
| Weakening default-deny or fail-closed behaviour | `A4_HUMAN_REQUIRED` — and Mahesh recommends against it |
