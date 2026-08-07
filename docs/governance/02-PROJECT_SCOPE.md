# 02 — Project Scope (Scope Validation)

**Layer:** L3 — project-specific, with an L1 classification model in §3–§5
**Pipeline step:** 3 — Scope Validation
**Owner:** Product Owner (content) · Architect (technical boundaries)

---

## 1. Purpose

Stage fit answers *"is it too early?"*. Scope fit answers a different and blunter question:
**"is this our problem at all?"**

Both gates must pass. An idea can be perfectly timed and still outside scope; an idea can be
squarely in scope and still three stages early.

---

## 2. Scope is not one thing

Four scopes are checked independently. An input is out of scope if it fails **any** of them.

| Scope | Question | Authority |
|-------|----------|-----------|
| **Business scope** | Does this serve an approved business capability or requirement? | [requirements/R0-SCOPE.md](../au-bank-insurance-platform/requirements/R0-SCOPE.md), BRD/PRD |
| **Product scope** | Is it in the approved product increment for this workstream? | [PRODUCT-BACKLOG.md](../1sb-insurance-integration/service-ssot/PRODUCT-BACKLOG.md), WS-2 SSOT |
| **Technical scope** | Is it inside the approved architecture and service boundaries? | [architecture-review/](../architecture-review/), service architecture docs |
| **Operational scope** | Do we own and run the thing being changed? | [01 §5](./01-CURRENT_STATE.md#5-standing-constraints-apply-to-every-triage-in-this-repo) |

Worked example — *"add a Kafka topic for quote events"*:
business ✅ (auditability is a requirement) · product ❌ (not in this increment) ·
technical ⚠️ (event backbone exists in the target architecture, not in the current one) ·
operational ❌ (no broker is run today). Verdict: out of current scope → and separately SF3
premature → **PARK to Integration Architecture**.

---

## 3. Scope-fit codes (L1 — generic)

| Code | Name | Test | Default routing |
|------|------|------|-----------------|
| **SC0** | IN-SCOPE-EXPLICIT | Named in an authority document's in-scope list, or traceable to an approved requirement ID | Proceed to stage-fit matrix |
| **SC1** | IN-SCOPE-DERIVED | Not named, but an in-scope deliverable is **incorrect, unsafe, non-compliant, or unusable** without it | Proceed; record the deliverable it serves in `serves` |
| **SC2** | ADJACENT-VALUE | Outside scope; plausible future value; nothing currently depends on it | **Force PARK → Ideas.** Never ADMIT, whatever the necessity |
| **SC3** | OUT-OF-SCOPE | Outside scope, no mandate, no dependant | **REJECT** with reason (so it is not re-proposed) |
| **SC4** | MANDATED-EXTERNAL | Outside scope but compelled by law, regulator, security policy, contract, or platform decision | **ESCALATE** → [14-CHANGE_CONTROL.md](./14-CHANGE_CONTROL.md). Never silently admitted *or* rejected |

### SC1 is the dangerous one

SC1 is where scope creep hides — every gold-plate can be narrated as "derived". Apply all three
tests; **all must pass**:

1. **Named beneficiary.** Which in-scope work item ID fails or is wrong without this? If the
   answer is "future work" or "the system generally", it is SC2, not SC1.
2. **Demonstrable failure.** Can you state the concrete failure — a failing AC, an incorrect
   result, a compliance breach, a security hole? "Would be cleaner / faster / more scalable"
   is not a failure.
3. **Minimality.** Is the proposal the *smallest* change that removes the failure? If it also
   introduces a framework, an abstraction layer, or a new dependency, split it: the minimal
   part is SC1, the rest is SC2.

> **Rule SC-1 — Derived necessity must name its beneficiary.** An SC1 claim without a work item
> ID in `serves` is invalid and downgrades to SC2.

### SC4 is never an agent decision

Regulatory, legal, and security mandates change *scope*, and scope belongs to the PO. An agent
that identifies an SC4 item writes the triage record, opens a `CR-###`, and stops. It does not
implement, and it does not reject on the grounds that "it's out of scope".

---

## 4. Necessity ≠ scope

Keep these separate; conflating them is the most common triage error.

| | In scope | Out of scope |
|---|---|---|
| **Necessary** | SC0/SC1 → schedule by stage | SC4 → change control |
| **Not necessary** | SC0 + COULD → low priority, still tracked | SC2/SC3 → park or reject |

Blueprint example:

```yaml
input: "Prevent duplicate payment processing"
business_scope:      "Not explicitly stated in the requirement"
technical_necessity: "Required — payment submission is retried by bank callers"
scope_fit:           SC1        # derived: FUNC-007 is incorrect without it
serves:              [FUNC-007]
necessity:           MUST
```

Not stated in the BRD, still MUST — because an in-scope deliverable is *wrong* without it.
That is exactly what SC1 exists for.

---

## 5. Recording a scope verdict

Every triage record carries:

```yaml
scope:
  code: SC1
  business_scope: "not explicit"
  serves: [FUNC-007]              # required for SC1
  failure_without_it: "duplicate payment session on bank retry"
  minimal: true
  authority: "PRODUCT-BACKLOG.md#E06"
```

---

## 6. Current scope — WS-1 · 1SB Insurance Integration

**In scope**

- LOB-routed insurance APIs exposed to bank apps: master data, quote, proposal, payment,
  application status
- 1SB protocol translation behind ports/adapters (`adapter.onesb.*`)
- Async job + polling infrastructure shared across quote and proposal
- Idempotency on mutating APIs
- Audit events, PII masking, raw-payload capture and encryption
- Shared libs: error model, security, audit, observability, secrets
- Durable state via `bank-persistence-service` HTTP contract
- LOBs: Term (P0), Health and Motor (P1)

**Out of scope (current increment)**

| Item | Why | Revisit at |
|------|-----|------------|
| Kafka / event backbone | Not in the current service topology | Integration architecture stage |
| Reactive rewrite | No demonstrated throughput requirement | Never, absent evidence |
| SDK / client framework for bank apps | Consumers integrate over HTTP + OpenAPI | Post-GA, if ≥ 2 consumers ask |
| Persistence performance optimisation | No measured problem | Production readiness |
| Multi-region / DR automation | Single-region UAT target | Production readiness |
| Saving / Annuity / Pension LOBs | P2 — after Term/Health/Motor are stable in prod | Phase 6+ |
| Provider routing flag / fake adapter | P2 replaceability proof (E13) | Phase 6+ |
| Retail-customer identity | Belongs to WS-2's later phase | WS-2 Phase 2+ |

**Out of scope permanently (reject on sight)**

- Bank apps calling 1SB or the database directly
- A second audit database (audit persists through `bank-persistence-service`)
- Flyway or JPA inside `1sb-integration-service`
- 1SB types leaking outside `adapter.onesb.*`

## 7. Current scope — WS-2 · Workforce Auth & Authorization

**In scope (Phase 1)**

- Workforce identity for bank employees and insurer representatives
- Token-hiding BFF session pattern for the Flutter app
- Provider-neutral adapter (Keycloak first)
- Business authorization service: roles, permissions, insurer/branch scope, hierarchy,
  certification metadata, grants and denials; default-deny RBAC + ABAC
- Maker-checker for bulk and privileged changes
- Authentication/administrative event retention (7 years, configurable)

**Out of scope (Phase 1)**

| Item | Revisit at |
|------|------------|
| Retail-customer authentication | Later bounded context |
| Production IdP selection (Cognito vs Keycloak vs other) | Phase 2 — deliberately deferred behind the adapter |
| Bank AD federation specifics (OIDC vs SAML vs LDAP) | Phase 2 — technology unconfirmed |
| Exposing Keycloak directly to any client | **Never** — architectural invariant |

---

## 8. Maintenance

Scope changes only through [14-CHANGE_CONTROL.md](./14-CHANGE_CONTROL.md). When a CR is
approved: update the relevant list here, update
[state/CURRENT-STATE.yaml](./state/CURRENT-STATE.yaml), then run the unpark sweep
([08 §5](./08-BACKLOG_RULES.md#5-unparking)) — a scope change frequently makes parked items
eligible.
