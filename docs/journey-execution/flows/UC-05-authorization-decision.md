# UC-05 — Authorization decision (the PDP)

**Actor:** service principal (a PEP) · **Entry:** `POST /internal/v1/authorization/decisions`
**Seam:** `S-02` · **Trust boundary:** `TB-3` Application→Identity
**Status:** `AI-DRAFTED` · Security review required · Slice 1

Every other flow depends on this one. It runs at least twice per regulated action — once at the BFF
PEP, once at the owning domain service — and it has **no degraded mode**.

---

## 1. Two PEPs, one PDP, and why both calls exist

| Caller | Purpose | If it were removed |
|---|---|---|
| **L4 BFF PEP** | Fail fast, before assembling a cross-service request | Slower refusals; **nothing becomes possible** |
| **L5 service PEP** | The **enforcement** | Every internal path, job and future BFF would be unauthorized |

The BFF's allow is an **optimisation**. The service's is the control. A rule enforced only at L4 is
not enforced ([`01 §1`](../01-REQUEST-LIFECYCLE-STANDARD.md#1-the-eight-layers)).

`identity-authorization-service` is the **business** source of truth for this decision. Keycloak is
not — it owns credentials, ceremonies, provider sessions, MFA and token issuance, and it is
explicitly *not* the source of truth for business authorization.

---

## 2. Route

| # | From → to | Layer | Style | Budget | On failure |
|---|---|---|---|---|---|
| 1 | BFF PEP → PDP | L4→L5 `TB-3` | sync | **300 ms, no retry** | **Fail closed — deny** |
| 2 | PDP → authorization PostgreSQL | L5→L7 `TB-4` | sync | — | Deny |
| 3 | Domain service PEP → PDP | L5→L5 `TB-3` | sync | **300 ms, no retry** | **Fail closed — deny** |

**No retry, by design.** A retry on a 300 ms budget turns one slow decision into a queue of slow
decisions, and the answer to "the PDP is slow" is never "ask it again immediately".

**Never crosses this boundary:** business data, and any request that assumes an allow on PDP
failure (`TB-3`).

---

## 3. Decision input

```json
{
  "subjectId": "user-uuid",
  "action": "lead.read",
  "resource": {
    "type": "LEAD", "id": "lead-uuid",
    "branchCode": "BELAPUR", "insurerCode": "ICICI_PRU",
    "ownerId": "rm-a", "assignedUserIds": ["rm-a"], "sharedWithPartner": true
  },
  "context": { "channel": "WORKFORCE_FLUTTER", "correlationId": "...", "lob": "LIFE" }
}
```

| Field | Rule |
|---|---|
| `action` | A **business permission**, never a URL (`VR-046`). One endpoint may require several |
| `resource` | Carries the attributes the policy needs: branch, insurer, owner, assignees, sharing |
| `context.lob` | Non-null, from `{LIFE, HEALTH, GENERAL}` (`INV-LOB-01`) |
| `subjectId` | From the authenticated session. **Never** from a request body |

Permission vocabulary: `lead.*`, `suitability.perform`, `quote.*`, `proposal.*`, `policy.*`,
`partner_user.*`, `role.assign`, `entitlement.*`, `break_glass.approve`, `audit.read`, `report.*`.

---

## 4. The decision algorithm

Full pseudocode: [`03 §3`](../03-VALIDATION-RULE-CATALOGUE.md#3-authorization-rules-pdp--slice-1-expanded).
The shape that matters:

```text
 STAGE 1 — precedence (auth SSOT §8.3). Produces a candidate.
    suspension  >  explicit deny  >  direct grant  >  role grant  >  DEFAULT DENY

 STAGE 2 — constraints. These only ever NARROW a candidate ALLOW; none can create one.
    actor-type    IPR + regulated sales action     → ASSIST_ONLY_ACTOR       [VR-041]
    tenancy       resource.insurer != subject      → CROSS_INSURER_DENIED    [VR-043]
    scope         branch ∉ (subject ∩ grant)       → OUT_OF_BRANCH_SCOPE     [VR-044]
    qualification regulated selling action         → SP_CERTIFICATION_REQUIRED [VR-040]
    break-glass   reason/checker/expiry missing    → BREAK_GLASS_INVALID     [VR-048]

 STAGE 3 — return {effect, reasonCode, matchedPolicy, policyVersion}
```

Three properties a reviewer should confirm by reading the implementation, not the document:

1. **Default is deny, not "no opinion".** Falling off the end of the precedence chain is a decision.
2. **Branch scope intersects.** `subject.branchScope ∩ grant.branchScope`. A union would let a broad
   role grant *widen* a narrowly-scoped user — the opposite of what a scope is for.
3. **Constraints cannot promote.** No constraint in stage 2 turns a DENY into an ALLOW. If one ever
   could, the precedence chain would stop being total.

### 4.1 Certification, evaluated per action

```text
VR-040  certification_gate(subject, action, resource)          ← INV-ACT-01, C3, AC-1
 1  IF action ∉ REGULATED_SELLING_ACTIONS → skip.
    ← non-selling work is NOT gated by certification. Gating it would block an RM
      from the work they need in order to renew the certificate.
 2  IF subject.actorType != BANK_RM → DENY{ORIGINATION_RM_ONLY}
 3  cert := subject.certification_for(resource.lob)
 4  IF cert absent
       OR cert.status != ACTIVE                       ← expired or suspended
       OR NOT (cert.valid_from <= now < cert.valid_until)   ← closed-open; a tie FAILS  [T-3]
       OR resource.lob ∉ cert.covered_lobs
       → DENY{SP_CERTIFICATION_REQUIRED}
 5  `now` is the PDP's clock AT THIS EVALUATION — not login time, not a request
    timestamp, not a value cached earlier in the same request.                  [T-1, T-4]
```

This is `AC-1` as executable logic: **Specified Person is a certification attribute evaluated at the
instant of the action**, not a role granted at login and not a channel.

---

## 5. Caching, and its one hard limit

| Decision kind | Cacheable | Limit |
|---|---|---|
| **Read** | Yes, narrowly defined | Within its policy TTL only |
| **Write** | **No** | Never served from cache when the PDP is unavailable |
| Any, after `policy_version` change | **Invalidated** | `VR-025` |
| Any, for a suspended principal | **Overridden** | `VR-026` — suspension out-votes a live cache |

```text
VR-045  pep_call(request)
 1  decision := PDP.authorize(...)   timeout 300 ms, NO retry
 2  IF timeout OR transport error OR malformed response
       → 403 AUTHORIZATION_UNAVAILABLE
         emit ALERT{pdp_fail_closed}
    ← alert on the RATE. A fail-closed denial is an availability incident wearing a
      403; if nobody alerts, the platform silently stops working and looks "secure".
 3  Cached READ decisions may serve within TTL. WRITES never do.
 4  There is no degraded mode. "Allow because the PDP is down" is precisely the
    failure this entire design exists to prevent.
```

---

## 6. Outcomes

| # | Outcome | Trigger | Effect | Reason code | Audit |
|---|---|---|---|---|---|
| 1 | **Allow — role grant** | Role covers action and scope | ALLOW | `ROLE_GRANT` | decision log |
| 2 | **Allow — direct grant** | Scoped grant, unexpired | ALLOW | `DIRECT_GRANT` | decision log |
| 3 | **Allow — break-glass** | Reason, checker, expiry all valid | ALLOW | `BREAK_GLASS` | **enhanced audit** |
| 4 | **Allow — `ACCESS_ALL`** | Explicit scope | ALLOW | `ACCESS_ALL` | **separately audited** |
| 5 | Default deny | Nothing matched | DENY | `DEFAULT_DENY` | decision log |
| 6 | Explicit deny | Scoped deny matched | DENY | `EXPLICIT_DENY` | decision log |
| 7 | Suspended | Account suspended | DENY | `ACCOUNT_SUSPENDED` | compliance event |
| 8 | IPR sales attempt | Regulated action by IPR | DENY | `ASSIST_ONLY_ACTOR` | **compliance event** |
| 9 | Cross-insurer | Resource insurer ≠ subject insurer | DENY | `CROSS_INSURER_DENIED` | **compliance event** |
| 10 | Out of branch scope | Branch outside the intersection | DENY | `OUT_OF_BRANCH_SCOPE` | decision log |
| 11 | Not an RM | Non-`BANK_RM` on a selling action | DENY | `ORIGINATION_RM_ONLY` | **compliance event** |
| 12 | Certification expired | Past `valid_until` | DENY | `SP_CERTIFICATION_REQUIRED` | **compliance event** |
| 13 | Certification wrong LOB | `LIFE` action, non-`LIFE` coverage | DENY | `SP_CERTIFICATION_REQUIRED` | compliance event |
| 14 | Certification suspended | Status not `ACTIVE` | DENY | `SP_CERTIFICATION_REQUIRED` | compliance event |
| 15 | Break-glass incomplete | Reason, checker or expiry missing | DENY | `BREAK_GLASS_INVALID` | enhanced audit |
| 16 | **PDP timeout** | > 300 ms | **DENY at the PEP** | `AUTHORIZATION_UNAVAILABLE` | **alert** |
| 17 | **PDP unreachable** | Service down | **DENY at the PEP** | `AUTHORIZATION_UNAVAILABLE` | **alert** |
| 18 | PDP database unreachable | Authorization PostgreSQL down | DENY | `AUTHORIZATION_UNAVAILABLE` | **alert** |
| 19 | Stale cached allow, principal now suspended | Suspension inside cache TTL | DENY | `ACCOUNT_SUSPENDED` | compliance event |
| 20 | Scope change mid-session | `policy_version` incremented | Cache invalidated; re-decided | *(new decision)* | decision log |
| 21 | Malformed request | Missing `lob`, unknown action | DENY | `INVALID_DECISION_REQUEST` | decision log |

Outcomes 16–18 are the ones a load test must produce deliberately. They are correct behaviour and an
operational incident **at the same time** — a distinction that only holds if someone alerts on them.

Outcome 19 is the single best test of whether precedence was implemented as a chain or as a bag of
rules.

---

## 7. What every decision returns

```json
{ "effect": "DENY", "reasonCode": "SP_CERTIFICATION_REQUIRED",
  "matchedPolicy": "policy:regulated-selling:v7", "policyVersion": 7 }
```

`matchedPolicy` and `policyVersion` are not diagnostics. They are how an audit two years from now
replays the **exact rule set** that produced a decision, after the policies have changed several
times. A decision log without them can show what was decided but never why.

---

## 8. Never, in this flow

| # | Never | Source |
|---|---|---|
| 1 | Allow because the PDP is unavailable | `VR-045`, `S-02`, `TB-3` |
| 2 | Retry the PDP inside the request | `S-02` |
| 3 | Serve a **write** decision from cache when the PDP is unavailable | `VR-045` |
| 4 | Let a role grant widen a user's branch scope | `VR-044` |
| 5 | Let a grant, a cache or `ACCESS_ALL` out-vote a suspension | `VR-026`, `VR-047` |
| 6 | Grant an IPR a regulated sales action at any stage | `VR-041`, `INV-ACT-02` |
| 7 | Evaluate certification at login instead of at the action | `VR-040`, `INV-ACT-01`, `AC-1` |
| 8 | Model permissions as URLs | `VR-046` |
| 9 | Treat Keycloak as the source of truth for business authorization | standing constraint |
| 10 | Return a decision without `matchedPolicy` and `policyVersion` | §7 |
| 11 | Send business data across `TB-3` | `TB-3` |
