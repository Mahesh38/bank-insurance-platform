# 03 — Validation Rule Catalogue

**Every rule, the layer that enforces it, the algorithm it follows, and the code it fails with.**

Status: `AI-DRAFTED` · Owner: Mahesh (Board 1) · Security review required · Origin: `SUG-20260821-jx1`

---

## 0. How to use this file

Each rule has a stable `VR-nnn`, cites the `INV-*` / control / document that **owns** the
underlying assertion, names its enforcement layer from
[`01-REQUEST-LIFECYCLE-STANDARD §1`](./01-REQUEST-LIFECYCLE-STANDARD.md#1-the-eight-layers), and —
once its slice lands — carries pseudocode precise enough that two developers produce the same
behaviour.

- A rule marked **`pseudocode: pending`** is *specified*: layer, source and failure code are
  binding now. Only the expansion is outstanding.
- Pseudocode is **language-neutral and deliberately not a Java signature.** Where the guard lives
  in the module structure is an S11 implementation decision, not this pack's.
- `now` in pseudocode always means the checking service's clock at guard execution — rules `T-1`
  to `T-5` in [`01 §5`](./01-REQUEST-LIFECYCLE-STANDARD.md#5-time-clocks-and-at-the-instant-of).

---

## 1. Envelope rules — every request

| ID | Rule | Layer | Owns it | Fails with |
|---|---|---|---|---|
| `VR-001` | Mutating request carries `Idempotency-Key` | L4 → L5 | `INV-IDM-01` | `400 MISSING_IDEMPOTENCY_KEY` |
| `VR-002` | Same key + different body is a conflict, not a replay | L5 | `INV-IDM-01` | `409 IDEMPOTENCY_KEY_CONFLICT` |
| `VR-003` | `distributorId` / attribution never accepted from the caller | L4 + L5 | `INV-DIS-01`, `C3` | `422 ATTRIBUTION_NOT_CALLER_SUPPLIED` |
| `VR-004` | `lob` present and in `{LIFE, HEALTH, GENERAL}` | L5 + L7 | `INV-LOB-01` | `422 LOB_REQUIRED` / store reject |
| `VR-005` | `lob` never carries a product-class value | L7 | `INV-LOB-02` | store `CHECK` reject |
| `VR-006` | Correlation id propagated unchanged through the tree | all | observability standard | — (generated if absent) |
| `VR-007` | No log record at any level contains a regulated-field pattern | framework + CI | `INV-LOG-01`, `C5` | build fails; runtime masks |

```text
VR-001/002  idempotency(key, body, operation, aggregate_id)
 1  IF request mutates AND key is absent            → 400 MISSING_IDEMPOTENCY_KEY
 2  IF caller is internal (L5→L5)
       key := derive(aggregate_id, operation)       ← server-derived; a retry cannot mint a new key
 3  fingerprint := hash(canonicalise(body))
 4  record := store.get(key)                        ← the OWNING service's store, 24 h retention
 5  IF record is absent
       store.put(key, {fingerprint, status = IN_PROGRESS})   ← atomic put-if-absent
       proceed
 6  ELSE IF record.fingerprint = fingerprint
       IF record.status = IN_PROGRESS → 409 REQUEST_IN_PROGRESS   ← do not run it twice concurrently
       ELSE return record.responseSnapshot with its ORIGINAL status code
 7  ELSE                                            → 409 IDEMPOTENCY_KEY_CONFLICT
 8  On completion: store.put(key, {fingerprint, responseSnapshot, createdAt = now})
```

```text
VR-003  attribution_is_server_resolved(request, principal)
 1  IF any of {distributorId, agentId, spLicenceId, sourcingCode} appears anywhere
    in the request body or query
       → 422 ATTRIBUTION_NOT_CALLER_SUPPLIED
         emit SECURITY_EVENT{type: CALLER_SUPPLIED_ATTRIBUTION, principal, correlationId}
       ← REJECT, never silently ignore. Ignoring teaches a caller the field is accepted.
 2  distributorId := configuration.resolve(domain = "attribution", lob, key = "distributorId")
 3  spLicenceId   := principal.certification.licenceId          ← from the authenticated principal
 4  IF either is unresolvable → 422 CONFIGURATION_UNRESOLVABLE  ← fail closed
 5  Attach both at the Integration Hub, immediately before adapter dispatch — never earlier,
    so no intermediate layer can be induced to carry a caller value.
```

---

## 2. Access and session rules — **slice 1, expanded**

Source of record: [`authentication-authorization`](../platform/authentication-authorization/README.md)
§§4–6, 8, 11, 13.

| ID | Rule | Layer | Owns it | Fails with |
|---|---|---|---|---|
| `VR-010` | Login begins a pending-login transaction bound to state, nonce and PKCE | L4 | §5.1 step 2 | `400 INVALID_LOGIN_REQUEST` |
| `VR-011` | Return location must be on the allow-list | L4 | §5.1 step 2 | `400 RETURN_LOCATION_NOT_ALLOWED` |
| `VR-012` | Callback `state` must match an unconsumed pending-login transaction | L4 | §5.1 step 6 | `400 INVALID_STATE` |
| `VR-013` | Authorization code is exchanged exactly once | L4 → adapter | §5.1 step 6 | `400 CODE_ALREADY_CONSUMED` |
| `VR-014` | `id_token` nonce must match the pending-login nonce | adapter | OIDC + §5.1 | `401 AUTHENTICATION_FAILED` |
| `VR-015` | Provider subject resolves to exactly one business identity | L5 PDP svc | §5.1 step 7 | `401 AUTHENTICATION_FAILED` (generic) |
| `VR-016` | Business identity is `ACTIVE`; employment and branch mapping present | L5 | §5.1 step 7 | `401 AUTHENTICATION_FAILED` (generic) |
| `VR-017` | Certification state is **recorded at login, never trusted as an entitlement** | L5 | `INV-ACT-01`, `AC-1` | — (login proceeds) |
| `VR-018` | Provider tokens stored server-side only; L0 receives an opaque reference | L4 | §5.1 step 8, standing constraint | design defect if violated |
| `VR-019` | Cookie clients require CSRF token **and** Origin/Referer allow-list | L4 | §4.1, §6 | `403 CSRF_REJECTED` |
| `VR-020` | Session honours both idle and absolute lifetimes | L4 | §6 | `401 SESSION_EXPIRED` |
| `VR-021` | Refresh-token reuse terminates the whole session family | L4 | §6 | `401 SESSION_REVOKED` |
| `VR-022` | Public auth errors never reveal whether a username exists | L4 | §11 | — |
| `VR-023` | No authentication fail-open, ever | L4 | §13 | `401` / `503`, never an allow |
| `VR-024` | Logout destroys the platform session **and** asks the provider to revoke | L4 → adapter | §5.3 | best-effort on the provider leg |
| `VR-025` | A material identity change increments `policy_version` and invalidates cached decisions | L5 | §5.3 | — |
| `VR-026` | Emergency suspension is an absolute deny, above every grant | L5 PDP | §8.3 | `403 ACCOUNT_SUSPENDED` |
| `VR-027` | Partner identity is provisioned only through maker-checker by a **different** checker | L5 | §5.2 | `403 SEPARATION_OF_DUTIES` |
| `VR-028` | No initial partner password is ever stored in platform data | L5 | §5.2 step 4 | design defect if violated |
| `VR-029` | Admin actions require recent authentication and MFA step-up | L4 | §6 | `401 STEP_UP_REQUIRED` |
| `VR-030` | NetworkPolicy denies BFF→Keycloak and service→Keycloak directly | infra | §13 | connection refused |

### 2.1 Login initiation

```text
VR-010/011  login_initiate(request, client_type)
 1  IF an authenticated, unexpired session already exists
       → 200 with the existing session status.        ← do NOT start a second ceremony
 2  IF client_type ∉ {WEB, NATIVE}                     → 400 INVALID_LOGIN_REQUEST
 3  return_to := request.returnTo ?: default_for(client_type)
 4  IF return_to ∉ configuration.allowlist("auth.returnLocations")
       → 400 RETURN_LOCATION_NOT_ALLOWED
         emit SECURITY_EVENT{type: OPEN_REDIRECT_ATTEMPT}
       ← an unvalidated return location is an open redirect and a token-theft primitive
 5  state         := random(>= 128 bits)
    nonce         := random(>= 128 bits)
    pkce_verifier := random(43..128 chars, unreserved)
    pkce_challenge:= base64url(sha256(pkce_verifier))            ← S256 only; "plain" is refused
 6  pending := {state, nonce, pkce_verifier, client_type, return_to,
                created_at = now, ttl = SHORT (minutes, not hours),
                consumed = FALSE}
    store pending keyed by state                       ← server-side; never in a cookie payload
 7  uri := adapter.authorization_uri({state, nonce, pkce_challenge, client_type})   [private API]
 8  return 302/200 carrying uri only.                  ← no token, no secret, no identity hint
```

### 2.2 Callback — the security-critical step

```text
VR-012/013/014/015/016/017/018  login_callback(code, state)
 1  pending := store.get_and_mark_consumed(state)       ← ATOMIC. Single-use by construction.
 2  IF pending is absent OR pending.consumed was already TRUE OR pending.expired(now)
       → 400 INVALID_STATE
         emit SECURITY_EVENT{type: CSRF_OR_REPLAY_ON_CALLBACK}
       ← covers login-CSRF, state fixation and callback replay in one check
 3  tokens := adapter.token_exchange({code, pkce_verifier: pending.pkce_verifier})
       IF adapter reports the code was already redeemed → 400 CODE_ALREADY_CONSUMED
       IF adapter is unreachable                        → 503 IDENTITY_PROVIDER_UNAVAILABLE
       ← NEVER fall back to any other authentication path (VR-023)
 4  claims := adapter.validated_claims(tokens)          ← signature, issuer, audience, exp
                                                          validated INSIDE the adapter
 5  IF claims.nonce != pending.nonce                    → 401 AUTHENTICATION_FAILED
 6  identity := authorization_service.resolve_business_identity(claims.sub)
 7  IF identity is absent
       OR identity.lifecycle != ACTIVE
       OR identity.employment is absent
       OR identity.branch_scope is empty
       → 401 AUTHENTICATION_FAILED                      ← ONE generic code for all four (VR-022)
         emit LoginFailed{subjectRef, reasonCode}       ← the specific reason goes to the EVENT,
                                                          never to the caller
 8  certification := identity.certification_snapshot(now)
       Record it on the session for display and telemetry.
       DO NOT treat it as an entitlement: every regulated action re-evaluates it
       at the instant of the action (VR-040 / INV-ACT-01).
 9  session := {id: random(>= 128 bits),
                principal: {id, actorType, branchScope, insurerId?, roles},
                provider_tokens: ENCRYPTED(tokens),    ← Redis/session vault, KMS key, never logged
                idle_expiry: now + idle_ttl(role),
                absolute_expiry: now + absolute_ttl(role),
                policy_version: identity.policy_version,
                client_type: pending.client_type}
    store session in the vault
10  IF pending.client_type = WEB
       set cookie: HttpOnly, Secure, SameSite, host-scoped, value = session.id
       issue a CSRF token bound to the session
    ELSE
       return {sessionHandle: session.id}               ← native stores it in Keychain/Keystore
11  emit LoginSucceeded{subjectId, actorType, correlationId}     ← no credentials, no raw tokens
12  redirect to pending.return_to
```

### 2.3 Session validation on every subsequent request

```text
VR-019/020/021/025  validate_session(request)
 1  ref := cookie value (WEB) | bearer handle (NATIVE)
 2  session := vault.get(ref);  IF absent → 401 SESSION_INVALID
 3  IF session.absolute_expiry <= now                   → 401 SESSION_EXPIRED, destroy
 4  IF session.idle_expiry     <= now                   → 401 SESSION_EXPIRED, destroy
 5  IF session.client_type = WEB
       IF request mutates AND (csrf_token invalid OR Origin/Referer not allow-listed)
          → 403 CSRF_REJECTED
 6  IF session.policy_version != authorization_service.current_policy_version(session.principal.id)
       → invalidate every cached decision for this principal
         re-read the principal snapshot
         IF the principal is now suspended or disabled → 401 SESSION_REVOKED, destroy
       ← this is how a revocation lands mid-session without waiting for token expiry
 7  session.idle_expiry := now + idle_ttl(role)         ← sliding idle window
 8  IF the down-scoped internal token for the target audience has expired
       new := adapter.refresh(session.provider_tokens.refresh)
       IF the adapter reports refresh-token REUSE
          → terminate the entire session family for this principal
            emit SessionRevoked{reason: REFRESH_REUSE_DETECTED}
            → 401 SESSION_REVOKED                       ← reuse means the token leaked; assume theft
 9  principal := session.principal                      ← ALWAYS from the vault, never from the body
```

### 2.4 Logout and revocation

```text
VR-024  logout(session_ref)
 1  session := vault.get(session_ref)
 2  vault.delete(session_ref)                           ← platform session dies FIRST and locally
 3  best_effort: adapter.revoke(session.provider_tokens)
       A provider-leg failure does NOT fail the logout and does NOT resurrect the session.
       It raises an operations task; short access-token lifetimes bound the exposure.
 4  clear the cookie (WEB)
 5  emit SessionRevoked{subjectId, reason: USER_LOGOUT}
 6  → 204. Idempotent: logging out an already-dead session is also 204.

VR-025/026  administrative revocation (out-of-band)
 1  Disablement, branch/insurer removal or a material permission change
       increments identity.policy_version.
 2  Every live session detects the mismatch at step 6 of validate_session — bounded by the
    next request, not by token lifetime.
 3  Emergency suspension is evaluated ABOVE every grant in the precedence chain, so it denies
    even where a stale cached decision or a direct grant would otherwise allow (VR-026).
```

### 2.5 Partner provisioning under maker-checker

```text
VR-027/028  provision_partner_identity(request, maker)
 1  Maker creates or imports the partner business identity      → status PENDING_APPROVAL
 2  IF checker.id = maker.id                                    → 403 SEPARATION_OF_DUTIES
       ← the check is on identity, not on role. A user holding both roles is still one person.
 3  On approval, an OUTBOX command provisions the provider identity through the adapter.
       At-least-once with retry; the approval transaction and the outbox row commit together.
 4  Keycloak sends or requires a credential-setup action.
       NO initial password is generated by, passed through, or stored in platform data.
 5  emit IdentityProvisioningRequested → IdentityProvisioned
```

---

## 3. Authorization rules (PDP) — **slice 1, expanded**

| ID | Rule | Layer | Owns it | Fails with |
|---|---|---|---|---|
| `VR-040` | SP certification evaluated at the **instant of the action**, not at login | L5 | `INV-ACT-01`, `C3` | `403 SP_CERTIFICATION_REQUIRED` |
| `VR-041` | IPR is granted no regulated-sales action at any stage | L5 PDP | `INV-ACT-02` | `403 ASSIST_ONLY_ACTOR` |
| `VR-042` | Default deny; precedence is fixed and total | L5 PDP | §8.3 | `403 <reasonCode>` |
| `VR-043` | Cross-insurer partner access always denied | L5 PDP | §8.3 | row absent (read) / `403` (write) |
| `VR-044` | Branch scope is **intersected** with, never expanded by, a role grant | L5 PDP | §8.3 | `403 OUT_OF_BRANCH_SCOPE` |
| `VR-045` | PDP unavailable ⇒ deny. Never a cached allow past its TTL on a write | L4 + L5 | `S-02`, §13 | `403 AUTHORIZATION_UNAVAILABLE` |
| `VR-046` | Permissions are business actions, never URLs | L4 | §8.2 | design defect if violated |
| `VR-047` | `ACCESS_ALL` is explicit and separately audited; never a silent bypass | L5 PDP | §8.3 | — |
| `VR-048` | Break-glass requires reason, checker, expiry and enhanced audit | L5 PDP | §8.3 | `403 BREAK_GLASS_INVALID` |

```text
VR-042  authorize(subject, action, resource, context)      ← the total precedence order
 1  IF subject.account_status ∈ {SUSPENDED, DISABLED}
       → DENY{ACCOUNT_SUSPENDED}                    ← above everything. No grant overrides it.
 2  IF explicit scoped DENY matches (subject, action, resource-scope)
       → DENY{EXPLICIT_DENY}
 3  IF direct scoped GRANT matches and is unexpired
       → candidate := ALLOW{DIRECT_GRANT}
 4  ELSE IF role-derived scoped grant matches
       → candidate := ALLOW{ROLE_GRANT}
 5  ELSE → DENY{DEFAULT_DENY}                       ← the default is deny, not "no opinion"

 -- candidate ALLOWs are then constrained; constraints only ever narrow --
 6  IF subject.actorType = INSURER_PARTNER_REP
       IF action ∈ REGULATED_SALES_ACTIONS          → DENY{ASSIST_ONLY_ACTOR}       [VR-041]
       IF resource.insurerCode != subject.insurerId → DENY{CROSS_INSURER_DENIED}    [VR-043]
 7  IF resource.branchCode ∉ (subject.branchScope ∩ grant.branchScope)
       → DENY{OUT_OF_BRANCH_SCOPE}                  ← INTERSECTION, never union     [VR-044]
 8  IF action ∈ REGULATED_SELLING_ACTIONS
       IF subject.actorType != BANK_RM              → DENY{ORIGINATION_RM_ONLY}
       cert := subject.certification_for(resource.lob)
       IF cert is absent
          OR cert.status != ACTIVE
          OR NOT (cert.valid_from <= now < cert.valid_until)     ← closed-open, ties fail  [T-3]
          OR resource.lob ∉ cert.covered_lobs
          → DENY{SP_CERTIFICATION_REQUIRED}                                          [VR-040]
       ← non-selling work is NOT denied by a missing certification; only regulated
         selling actions are. Denying everything would be a wrong, and costly, over-read.
 9  IF grant.type = BREAK_GLASS
       IF reason absent OR checker absent OR expiry <= now → DENY{BREAK_GLASS_INVALID}
       ELSE emit ENHANCED_AUDIT{break_glass}                                          [VR-048]
10  return ALLOW{matchedPolicy, policyVersion, reasonCode}
    ← every decision returns matchedPolicy + policyVersion so a later audit can replay
      the exact rule set that produced it
```

```text
VR-045  pdp_call_from_a_pep(request)                       ← both L4 and L5 PEPs
 1  decision := PDP.authorize(...)  with timeout 300 ms, NO retry            [S-02]
 2  IF timeout OR transport error OR malformed response
       → 403 AUTHORIZATION_UNAVAILABLE
         emit ALERT{pdp_fail_closed}          ← fail-closed denials are an incident signal,
                                                 not routine traffic. Alert on the RATE.
       ← there is no degraded mode. "Allow because the PDP is down" is the failure this
         entire design exists to prevent.
 3  A narrowly defined cached READ decision may serve within its policy TTL.
    A WRITE never serves from cache when the PDP is unavailable.
 4  L5 re-checks independently. The L4 allow is an optimisation; L5 is the enforcement.
```

---

## 4. Journey and domain rules — enumerated, expanded in later slices

Binding now: layer, owner and failure code. `pseudocode: pending` marks the expansion slice.

### 4.1 Compliance hard gates

| ID | Rule | Layer | Owns it | Fails with | Slice |
|---|---|---|---|---|---|
| `VR-050` | No quote without a valid, unexpired, `ELIGIBLE` suitability assessment | L6 | `INV-QUO-01`, `C1` | `403 SUITABILITY_REQUIRED` | 3 |
| `VR-051` | No proposal submit without an unexpired `GRANTED` consent covering the purpose set | L6 | `INV-PRP-01`, `C2` | `403 CONSENT_REQUIRED` | 3 |
| `VR-052` | Payment link binds to a customer-device channel only | L5 + L6 | `INV-PAY-01`, `C4` | `403 PAYMENT_DEVICE_ISOLATION` | 4 |
| `VR-053` | Accountable SP written once at origination, immutable thereafter | L7 | `INV-ACT-03` | store reject + integrity alert | 2 |
| `VR-054` | Every audit event carries actor, actingCapacity, and IPR attribution fields | L5 ingest | `INV-ACT-04` | event rejected; outbox retries | 5 |
| `VR-055` | Audit and raw payloads immutable, `retain_until` >= event + 7 years | L7 | `INV-AUD-01`, `C7` | store/Object-Lock reject | 5 |
| `VR-056` | Every aggregate transition emits exactly one audit event | L6 | `INV-AUD-02`, `C8` | transition commits; outbox retries | 5 |
| `VR-057` | Every store, backup, log sink and archive resolves to an AWS India region | IaC | `INV-DAT-01`, `C6` | `apply` blocked; drift = O0 | — |

### 4.2 Origination and opportunity

| ID | Rule | Layer | Owns it | Fails with | Slice |
|---|---|---|---|---|---|
| `VR-060` | Lead created only by `BANK_RM` — no BFF path, no service path | L6 | `INV-LED-04` | `403 ORIGINATION_RM_ONLY` | 2 |
| `VR-061` | Lead only for a customer in the creating RM's ETB book | L6 | `INV-LED-05` | `422 CUSTOMER_NOT_IN_BOOK` | 2 |
| `VR-062` | Lead assigned only to a currently-certified principal for the LOB | L6 | `INV-LED-03` | `422 RM_NOT_CERTIFIED` | 2 |
| `VR-063` | Terminal lead accepts no further transition | L6 | `INV-LED-01` | `409 ILLEGAL_TRANSITION` | 2 |
| `VR-064` | At most one journey drives a lead to `CONVERTED` | L6 | `INV-LED-02` | idempotent ignore; alert on mismatch | 2 |
| `VR-065` | Every downstream aggregate references exactly one `leadId` | L6 + L7 | `INV-LED-06` | `422 OPPORTUNITY_REQUIRED` | 2 |
| `VR-066` | IPR reads apply the `AC-4` predicate at the store; rows are absent | L7 | `INV-LED-07` | row absent, never `403` | 5 |

### 4.3 Consent and suitability

| ID | Rule | Layer | Owns it | Fails with | Slice |
|---|---|---|---|---|---|
| `VR-070` | Consent evidence is write-once, all evidence fields mandatory | L7 | `INV-CNS-01` | store reject | 2 |
| `VR-071` | `GRANTED` reachable only from `OTP_PENDING` with a verified OTP txn id | L6 | `INV-CNS-02` | `409 ILLEGAL_TRANSITION` | 2 |
| `VR-072` | A `COMPLETED` assessment is immutable; corrections supersede | L6 | `INV-SUI-01` | `409 ASSESSMENT_IMMUTABLE` | 2 |
| `VR-073` | An override records actor, reason and timestamp, and is itself audited | L6 | `INV-SUI-02` | transition refused | 2 |

### 4.4 Quotation and proposal

| ID | Rule | Layer | Owns it | Fails with | Slice |
|---|---|---|---|---|---|
| `VR-080` | A successful quote holds >= 1 offer and 0 failed offers | L6 | `INV-QUO-02` | quote → `FAILED`, not zero-offer success | 3 |
| `VR-081` | Selectable offers have premium > 0 and sumAssured > 0 | L6 | `INV-QUO-03` | offer `INVALID`, excluded | 3 |
| `VR-082` | Offer selectable only inside the quote validity window | L6 | `INV-QUO-04` | `409 QUOTE_EXPIRED` | 3 |
| `VR-083` | Selection atomically marks siblings `NOT_SELECTED` | L6 | `INV-QUO-05` | transaction rolls back | 3 |
| `VR-084` | Proposal references one offer whose quote was `SELECTED` | L6 | `INV-PRP-02` | `422 INVALID_OFFER_REFERENCE` | 3 |
| `VR-085` | `AWAITING_PAYMENT` only from `UW_APPROVED` | L6 | `INV-PRP-03` | `409 ILLEGAL_TRANSITION` | 3 |
| `VR-086` | No withdrawal at or after `AWAITING_PAYMENT` | L6 | `INV-PRP-04` | `409 WITHDRAWAL_NOT_PERMITTED` | 3 |
| `VR-087` | Proposal PII only in the encrypted payload store, never in queryable columns | L7 | `INV-PRP-05` | persistence reject; build fails | 3 |

### 4.5 Payment and policy

| ID | Rule | Layer | Owns it | Fails with | Slice |
|---|---|---|---|---|---|
| `VR-090` | One payment per proposal in a non-terminal state | L7 unique | `INV-PAY-02` | `409 PAYMENT_ALREADY_IN_PROGRESS` | 4 |
| `VR-091` | Amount equals selected premium + tax, to the paise | L6 | `INV-PAY-03` | `422 PREMIUM_MISMATCH` + financial alert | 4 |
| `VR-092` | No new attempt while a prior one is `AUTHORISED` or `UNCERTAIN` | L6 | `INV-PAY-04` | `409 PAYMENT_STATE_UNCERTAIN` | 4 |
| `VR-093` | `RECONCILED` only on a settlement match by `pgTxnId` **and** amount | job | `INV-PAY-05` | stays `CAPTURED` → `RECONCILIATION_BREAK` | 4 |
| `VR-094` | Refunds above threshold never automatic | L6 | `INV-PAY-06` | held for second authorisation | 4 |
| `VR-095` | Policy never created before its payment is `RECONCILED` | L6 | `INV-POL-01` | `409 PAYMENT_NOT_RECONCILED` | 4 |
| `VR-096` | `policyNumber` unique per insurer, immutable once set | L7 | `INV-POL-02` | store reject | 4 |
| `VR-097` | `ACTIVE` only with documents persisted **and** issuance audit confirmed | L6 | `INV-POL-03` | stays `CONFIRMED`; ops task | 4 |

### 4.6 Journey and configuration

| ID | Rule | Layer | Owns it | Fails with | Slice |
|---|---|---|---|---|---|
| `VR-100` | Only transitions drawn in the state machine are legal | L6 | `INV-JRN-01` | `409 ILLEGAL_TRANSITION` + alert | 2 |
| `VR-101` | Journey stores stage and references, never a business decision | L6 schema | `INV-JRN-02` | build fails | 2 |
| `VR-102` | Every non-terminal journey has a next action and inactivity horizon | L6 | `INV-JRN-03` | validation fails at creation | 2 |
| `VR-103` | `COMPENSATING` exits only to `SOLD`, `COMPENSATED` or `MANUAL_INTERVENTION` | L6 | `INV-JRN-04` | transition rejected | 5 |
| `VR-104` | `SOLD` only with policy `ACTIVE` + payment `RECONCILED` + issuance confirmed + all audit events persisted | L6 | `INV-JRN-05`, `C8` | stays `ISSUED`/`COMPENSATING` | 4 |
| `VR-110` | No business code branches on an insurer/product/LOB/channel literal | ArchUnit | `INV-CFG-01` | build fails | 5 |
| `VR-111` | Configuration is never updated in place; a change is a new effective-dated version | L7 | `INV-CFG-02` | store reject | 5 |
| `VR-112` | Every business record stores the configuration version that governed it | L6 | `INV-CFG-03` | `422 CONFIG_VERSION_REQUIRED` | 5 |
| `VR-113` | Configuration unresolvable ⇒ refuse the action. No compiled-in default exists | L5 | `S-21`, `CF-1` | `422 CONFIGURATION_UNRESOLVABLE` | 5 |
| `VR-120` | No provider SDK or wire type appears outside its adapter package | ArchUnit | `INV-ACL-01` | build fails | 3 |

---

## 5. Coverage

| Source set | Total | Referenced here | Expanded |
|---|---|---|---|
| Compliance hard gates `INV-*` (§6.1) | 13 | 13 | 4 |
| Aggregate invariants `INV-*` (§6.2) | 34 | 34 | 0 |
| Auth/authz rules (WS-2 SSOT) | — | 30 | **30** |
| Standing constraints `C1`–`C8` | 8 | 8 | 3 |

Every invariant in
[`01-domain-model §6`](../platform/ws3-platform/01-domain-model-and-invariants.md#61-compliance-hard-gates-s06-e03-s02)
has a `VR` id here. **No invariant is unrouted** — that is the completeness property this catalogue
must keep, and the one a reviewer should check first.
