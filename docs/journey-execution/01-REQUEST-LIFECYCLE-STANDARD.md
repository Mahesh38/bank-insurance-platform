# 01 — Request Lifecycle Standard

**The ladder every request climbs, stated once. Each flow file records only what differs from it.**

Status: `AI-DRAFTED` · Owner: Mahesh (Board 1) · Origin: `SUG-20260821-jx1`

---

## 1. The eight layers

Derived from the two-hop reverse proxy in
[`R0-LLD §3`](../architecture/R0-LLD.md#3-reverse-proxy--external-and-internal-required) and the
invariant placement summary in
[`01-domain-model §6.3`](../platform/ws3-platform/01-domain-model-and-invariants.md).

```text
L0  Device            Flutter RM app · IPR browser · (customer device — never on this path)
     │ TLS 1.3
L1  CloudFront + WAF  TLS termination · bot · rate · OWASP managed rules          [TB-1]
     │
L2  API Gateway       Request size · schema · throttle · route                     THE external reverse proxy
     │ private integration / VPC link
L3  Internal ALB      Host/path routing to a BFF target group                      THE internal reverse proxy
     │
L4  BFF               Session authN · CSRF · PEP call to PDP · aggregation         [TB-2, TB-3]
     │ cluster-private, service identity
L5  Domain service    Service identity · PDP re-check on regulated actions ·
     │                 cross-aggregate business validation · orchestration
L6  Aggregate         Domain invariants, in one transaction                        ← most gates live here
     │
L7  Store             NOT NULL / CHECK / UNIQUE · INSERT-only roles ·
                       mandatory visibility predicates · Object Lock               [TB-4]
```

**The rule that makes the ladder worth having:** a check is placed at the **lowest layer that can
see everything it needs**, and it is *also* placed higher only where a higher layer must fail fast
for a human waiting in session. A higher-layer check is an **optimisation**; the lower-layer check
is the **enforcement**. Removing the higher one must never change what is possible — only how fast
the caller learns.

> Corollary, and the single most common way this design gets built wrong: **a rule enforced only at
> L4 is not enforced.** L4 is reachable only from the edge; every service-to-service path,
> every job, and every future BFF bypasses it. If a flow file shows a gate at L4 and nowhere below,
> that is a defect in the flow file — raise it.

### 1.1 What each layer must not do

| Layer | Never |
|---|---|
| L1 CloudFront / WAF | Authenticate · authorize · cache an authenticated JSON response |
| L2 API Gateway | Use an API key as authentication · hold business logic |
| L3 Internal ALB | Terminate a business decision · exist per microservice |
| L4 BFF | Return an OAuth access or refresh token to L0 · be the only enforcer of any rule · accept `distributorId` |
| L5 Domain service | Call a provider adapter directly · read another context's database · assume PDP allowed on timeout |
| L6 Aggregate | Permit a transition not drawn in the state machine · hold another context's business decision |
| L7 Store | Expose `UPDATE`/`DELETE` on consent, suitability or audit · allow a nullable `lob` |

Sources: [`R0-LLD §3`](../architecture/R0-LLD.md#3-reverse-proxy--external-and-internal-required) ·
[`R0-HLD §3`](../architecture/R0-HLD.md#3-ten-boundaries) ·
[`04-security-architecture §2`](../platform/ws3-platform/04-security-architecture.md#2-trust-boundaries) ·
[`BOOT.md §5`](../context/BOOT.md) standing constraints.

---

## 2. The standard request algorithm

Every authenticated mutating platform request runs exactly this. A flow file states only its
**deltas** — extra steps, different timeouts, additional gates.

```text
ALGORITHM  standard_request(http_request)

 L1  WAF
 1   IF matches OWASP managed rule OR bot signature OR rate bucket exhausted
        → 403 at the edge. Never reaches the VPC. No platform audit event exists.
        (This is the one refusal class with no application-side record — see §6.)

 L2  API Gateway
 2   IF body size > route limit                    → 413
 3   IF request does not match the route schema    → 400  (shape only, never semantics)
 4   IF throttle bucket for the route exhausted    → 429  + Retry-After

 L4  BFF — authentication
 5   session_ref := cookie (web) OR opaque bearer handle (native)
 6   IF session_ref absent OR not found in the session vault OR expired
        → 401 SESSION_INVALID.                     ← never 403; the caller must re-authenticate
 7   IF client is cookie-based
        AND (CSRF token absent/mismatched OR Origin/Referer not allow-listed)
        → 403 CSRF_REJECTED
 8   principal := session.principal                ← from the vault, NEVER from the request body
 9   IF session.absolute_expiry < now OR session.idle_expiry < now
        → 401 SESSION_EXPIRED, destroy the session

 L4  BFF — request envelope
10   IF request mutates AND Idempotency-Key header absent
        → 400 MISSING_IDEMPOTENCY_KEY                                      [INV-IDM-01]
11   IF request body contains distributorId (or any attribution field)
        → 422 ATTRIBUTION_NOT_CALLER_SUPPLIED, emit a security event       [INV-DIS-01, C3]
12   correlation_id := header X-Correlation-Id ?: generate()
13   journey_id     := header X-Journey-Id (required once a journey exists)

 L4  BFF — authorization (PEP → PDP)
14   decision := PDP.authorize({
                   subjectId : principal.id,
                   action    : <business permission, NOT the URL>,
                   resource  : {type, id, branchCode, insurerCode, ownerId,
                                assignedUserIds, sharedWithPartner},
                   context   : {channel, correlationId, lob}})
        timeout 300 ms · NO retry                                          [S-02]
15   IF decision times out OR errors
        → 403 AUTHORIZATION_UNAVAILABLE.           ← FAIL CLOSED. Default-deny is not degradable.
16   IF decision.effect != ALLOW
        → 403 <decision.reasonCode>

 L5  Domain service
17   IF caller service identity not permitted for this internal route
        → 403 SERVICE_IDENTITY_REJECTED            ← a leaked internal URL is not an entry point
18   RE-CHECK the PDP decision for any regulated action.                   [INV-ACT-01, ID-08]
        The BFF's allow is an optimisation; this one is the enforcement.
19   Resolve every rule that varies by insurer/product/LOB/channel from
        Configuration. 300 ms, no retry, no compiled-in default.           [S-21, INV-CFG-01]
        IF unresolvable → 422 CONFIGURATION_UNRESOLVABLE                   ← FAIL CLOSED
20   Idempotency lookup on the server-derived key:
        same key + same body fingerprint → return the stored response verbatim
        same key + different fingerprint → 409 IDEMPOTENCY_KEY_CONFLICT
21   Run cross-aggregate preconditions (the hard gates that read another
        context: C1 suitability, C2 consent, payment-state guards).

 L6  Aggregate — one transaction
22   IF the requested transition is not drawn in the state machine
        → 409 ILLEGAL_TRANSITION                                           [INV-JRN-01 et al.]
23   Assert every aggregate invariant listed for this operation.
24   Apply the change and append the audit event to the SAME transaction's
        outbox table.                                                      [S-17, INV-AUD-02]

 L7  Store
25   Constraints are the last line, not the first: NOT NULL lob, CHECK,
        UNIQUE, INSERT-only role, mandatory visibility predicate.          [INV-LOB-01, INV-LED-07]
26   COMMIT. The business transaction and its audit event commit together
        or not at all.

 post-commit
27   Outbox poller delivers the audit event at-least-once until acknowledged.
        The journey cannot reach SOLD until every required event is
        acknowledged.                                                      [INV-JRN-05, F-10]
28   Return the response. Store it against the idempotency key (24 h).
```

### 2.1 Read requests

Steps 1–9, 12–18 and 25 apply unchanged. Steps 10, 11, 20, 22–24, 26–28 do not.
One addition, and it is not optional:

```text
R1   A read for an INSURER_PARTNER_REP principal applies the AC-4 visibility
     predicate at L7, in the query, as a mandatory persistence-layer predicate.
     Out-of-scope rows are ABSENT from the result set.
     NEVER a 403 on a named id — a refusal that names an id confirms the id.   [INV-LED-07, S-22]
```

---

## 3. Where each hard gate actually sits

The table the pack exists to produce. `C1`…`C8` are the standing constraints from
[`R0-HLD §6`](../architecture/R0-HLD.md#6-business-logic--what-the-platform-will-not-do).

| Gate | L4 BFF | L5 Service | L6 Aggregate | L7 Store | Enforcement layer |
|---|:--:|:--:|:--:|:--:|---|
| **C1** suitability before quote | fast-fail | reads `S-08` | `INV-QUO-01` | — | **L6** |
| **C2** consent before proposal submit | fast-fail | reads consent | `INV-PRP-01` | — | **L6** |
| **C3** `distributorId` never caller-supplied | reject | `INV-DIS-01` at Hub | — | — | **L5 (Hub)** |
| **C4** payment on customer device only | reject | `INV-PAY-01` | `INV-PAY-01` | — | **L5 + L6** |
| **C5** no PII in logs | converter | converter | converter | — | **framework + CI** |
| **C6** India-region residency | — | — | — | IaC policy | **pre-apply + drift job** |
| **C7** evidence immutable | — | — | — | INSERT-only role, Object Lock | **L7** |
| **C8** sale never inferred | — | — | `INV-JRN-05` | — | **L6** |
| SP certification at the instant of action | PDP call | PDP re-check `INV-ACT-01` | — | — | **L5** |
| Origination is RM-only | `S-20` reject | rejects non-RM | `INV-LED-04` | — | **L6** |
| IPR visibility | — | — | — | `INV-LED-07` predicate | **L7** |
| Idempotency | key required | stored response | — | — | **L5** |

Read the last column as: *delete every other tick and the platform is still correct — slower to
say no, but never wrong.*

---

## 4. The request envelope

Every call carries these. Sources:
[`R0-HLD §5`](../architecture/R0-HLD.md#5-api-details) ·
[`03-solution-architecture §5.2`](../platform/ws3-platform/03-solution-architecture-r0.md#52-idempotency-standard--s07-e02-s05).

| Header | L0 → L4 | L4 → L5 | L5 → L5 | Rule |
|---|---|---|---|---|
| `Authorization` | opaque session (cookie or handle) | down-scoped, audience-specific internal token | service identity | **Never** an OAuth access/refresh token below L4 toward L0 |
| `X-Correlation-Id` | optional, generated if absent | propagated | propagated | One value for the whole request tree |
| `X-Journey-Id` | required once a journey exists | propagated | propagated | Absent before journey creation |
| `Idempotency-Key` | **client-supplied**, mutations only | **server-derived** | **server-derived** | Derived = owning aggregate id + operation, so an internal retry cannot mint a new key |
| `X-Lob` / body `lob` | required | propagated | propagated | Non-null, one of `LIFE`/`HEALTH`/`GENERAL` [`INV-LOB-01`] |

Idempotency record: `key → {requestFingerprint, responseSnapshot, createdAt}`, in the **owning
service's own store**, 24 h retention. Not a shared Redis — that is a recorded R0 deferral, not an
oversight.

---

## 5. Time, clocks and "at the instant of"

Four invariants say "at the instant of the check" (`INV-QUO-01`, `INV-ACT-01`, `INV-PRP-01`,
`INV-QUO-04`). Left unstated, three developers implement three semantics. This pack fixes one:

```text
RULE T-1  Expiry is evaluated against the CHECKING service's clock, at the moment the
          guard executes — never against a timestamp carried in the request, and never
          against a value cached before the guard ran.

RULE T-2  All services run NTP-synchronised UTC. Every stored instant is UTC with an
          explicit offset. No local time is persisted, compared or logged.

RULE T-3  A validity window is evaluated CLOSED-OPEN: valid_from <= now < valid_until.
          An assessment expiring at exactly now is EXPIRED. Ties fail closed.

RULE T-4  A guard that reads a remote validity (S-08 suitability, S-21 configuration)
          uses the value AS AT the guard's execution, not as at the start of the request.
          A long-running request re-reads rather than trusting a value read earlier in
          the same request.

RULE T-5  Clock skew is not a defence. A guard must never widen a window to absorb skew.
          If skew makes a legitimate action fail, that is a monitoring finding, not a
          reason to loosen the guard.
```

---

## 6. What is observable when a request fails

| Refused at | HTTP | Platform audit event? | RM sees | Ops sees |
|---|---|---|---|---|
| L1 WAF | 403 | **No** — never entered the VPC | Generic network error | WAF logs only |
| L2 API Gateway | 400 / 413 / 429 | No | Typed client error | Gateway metrics |
| L4 session | 401 | `LoginFailed` / `SessionRevoked` | Re-authenticate prompt | Auth event stream |
| L4 PDP deny | 403 + reason code | Yes — authorization event | The reason, in business terms | PDP decision log |
| L4 PDP unavailable | 403 `AUTHORIZATION_UNAVAILABLE` | Yes | "Temporarily unavailable" | **Alert** — fail-closed denials are an incident signal |
| L5 gate (C1/C2/C4) | 403 | Yes — compliance event | The specific gate, actionable | Compliance event stream |
| L5 configuration | 422 `CONFIGURATION_UNRESOLVABLE` | Yes | "Cannot proceed" | **Alert** |
| L6 invariant | 409 / 422 | Yes | The illegal transition, in business terms | Integrity alert on `INV-JRN-01` |
| L7 constraint | 500 | Yes — integrity alert | Generic failure | **Alert** — reaching L7 means a guard above is missing |

> **A `500` from an L7 constraint is always a bug in an L6 guard.** The store is the backstop, not
> the validator. Every one of these is a defect ticket, not a retry.

Public authentication errors never reveal whether a username exists
([`authentication-authorization §11`](../platform/authentication-authorization/README.md#11-initial-service-apis)).
No error body at any layer carries PII (`C5`, `INV-LOG-01`).
