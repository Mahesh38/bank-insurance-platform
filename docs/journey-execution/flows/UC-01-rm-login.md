# UC-01 — RM login

**Actor:** Bank RM (`BANK_RM`) · **Entry:** `POST /api/v1/auth/login` → `GET /api/v1/auth/callback`
**Trust boundaries crossed:** `TB-1` Internet→Edge · `TB-2` Edge→Application · `TB-3` Application→Identity
**Status:** `AI-DRAFTED` · Security review required · Slice 1

Assumes [`01-REQUEST-LIFECYCLE-STANDARD`](../01-REQUEST-LIFECYCLE-STANDARD.md). Only deltas are stated.

---

## 1. What actually happens, versus what people assume

A common description of this flow is *"CloudFront → WAF → BFF → authentication service → SSO"*.
The ratified design has **no separate authentication service**:

- `workforce-access-bff` **owns** login, callback, session and logout itself
  ([auth SSOT §4.1](../../platform/authentication-authorization/README.md#41-workforce-access-bff)).
- It calls `identity-provider-adapter-service` — a **provider-neutral port**, not an authenticator.
  The adapter builds authorization requests and exchanges codes; it is stateless and *"does not
  become a second identity provider"* (§4.2).
- **Keycloak** performs the ceremony and federates to bank AD.
- `identity-authorization-service` is a **separate hop** and is *not* on the authentication path at
  all — it resolves the business identity and later answers authorization queries as the PDP. It is
  never the source of truth for authentication, and Keycloak is never the source of truth for
  business authorization.

The distinction is not pedantic: it is why the production IdP choice (Keycloak vs Cognito) can still
be deferred without touching a BFF or authorization contract.

---

## 2. Preconditions

| # | Precondition | If absent |
|---|---|---|
| 1 | The RM has a bank AD account | Ceremony fails at Keycloak; BFF sees no code |
| 2 | A business identity exists in `identity-authorization-service`, `ACTIVE` | `401 AUTHENTICATION_FAILED` |
| 3 | Employment and at least one branch mapping present | `401 AUTHENTICATION_FAILED` |
| 4 | Return location is on the configured allow-list | `400 RETURN_LOCATION_NOT_ALLOWED` |
| — | SP certification valid | **Not a precondition.** Login succeeds; regulated actions are gated later (`VR-017`) |

> Precondition 4's absence is the difference between a login page and an open redirect.
> Precondition "SP certification valid" being *absent* from this list is `AC-1` as architecture:
> certification is checked at the instant of each regulated action, not at login. An RM whose
> certificate lapsed overnight can still log in and do non-selling work.

---

## 3. Route — leg A, initiation

| # | From → to | Layer | Protocol | Style | Budget | What happens |
|---|---|---|---|---|---|---|
| A1 | RM device → CloudFront | L0→L1 | TLS 1.3 | sync | — | TLS terminated at ACM cert |
| A2 | CloudFront → WAF | L1 | — | sync | — | Bot, rate, OWASP managed rules. **No authN, no authZ** |
| A3 | WAF → API Gateway | L1→L2 | HTTPS | sync | — | Request size, route schema, throttle |
| A4 | API Gateway → Internal ALB | L2→L3 | private integration / VPC link | sync | — | No public NLB/ALB onto EKS |
| A5 | Internal ALB → `workforce-access-bff` | L3→L4 | HTTPS internal cert | sync | 5 s edge | Host/path route |
| A6 | BFF → session vault | L4 | — | sync | — | Store the pending-login transaction, keyed by `state` |
| A7 | BFF → `identity-provider-adapter-service` | L4→L5 | private API, service identity | sync | — | `POST /internal/v1/auth/authorization-uri` |
| A8 | adapter → Keycloak | L5 | private | sync | — | Build the provider authorization URI |
| A9 | BFF → RM device | L4→L0 | HTTPS | — | — | Returns the URI **only**. No token, no secret, no identity hint |

## 4. Route — leg B, the ceremony and callback

| # | From → to | Layer | Style | What happens |
|---|---|---|---|---|
| B1 | RM device → Keycloak | L0→identity | sync | The bank-controlled ceremony. MFA required in production |
| B2 | Keycloak → bank AD | identity→external | OIDC / SAML / LDAP | **Phase 2.** R0 `dev` uses Keycloak-local users |
| B3 | Keycloak → RM device | — | 302 | Redirect **only** to the BFF callback |
| B4 | RM device → …edge… → BFF | L0→L4 | sync | `GET /api/v1/auth/callback?code&state` |
| B5 | BFF → session vault | L4 | sync | **Atomic** get-and-consume of the pending-login by `state` |
| B6 | BFF → adapter | L4→L5 | sync | `POST /internal/v1/auth/token-exchange` with the PKCE verifier |
| B7 | adapter → Keycloak | L5 | sync | Redeem the code; validate signature, issuer, audience, `exp` |
| B8 | BFF → `identity-authorization-service` | L4→L5 | sync `TB-3` | Resolve provider subject → business identity |
| B9 | BFF → session vault | L4 | sync | Store the session; provider tokens **encrypted** |
| B10 | BFF → RM device | L4→L0 | 302 | Cookie (web) or opaque handle (native) + redirect to `return_to` |

**The device never talks to Keycloak's token endpoint, and never receives an OAuth token.**
That is `TB-1`'s forbidden-crossing rule and a standing constraint, not a preference.

---

## 5. Validation, in execution order

| # | Rule | Layer | Check | Failure |
|---|---|---|---|---|
| 1 | — | L1 | WAF managed rules, bot, rate | `403`, no body, **no platform audit event** |
| 2 | — | L2 | Route schema, body size, throttle | `400` / `413` / `429` |
| 3 | `VR-010` | L4 | Existing valid session? Return it — do not start a second ceremony | `200` with current status |
| 4 | `VR-010` | L4 | `client_type ∈ {WEB, NATIVE}` | `400 INVALID_LOGIN_REQUEST` |
| 5 | `VR-011` | L4 | `return_to` ∈ configured allow-list | `400 RETURN_LOCATION_NOT_ALLOWED` + security event |
| 6 | `VR-010` | L4 | Mint `state`, `nonce`, PKCE verifier (≥128 bits; **S256 only**) | — |
| 7 | `VR-012` | L4 | Callback `state` resolves to an **unconsumed, unexpired** pending-login, consumed atomically | `400 INVALID_STATE` + security event |
| 8 | `VR-013` | L4→adapter | Code redeemed exactly once | `400 CODE_ALREADY_CONSUMED` |
| 9 | `VR-014` | adapter | `id_token` signature, issuer, audience, `exp`, **and nonce match** | `401 AUTHENTICATION_FAILED` |
| 10 | `VR-015` | L5 | Provider subject → exactly one business identity | `401 AUTHENTICATION_FAILED` *(generic)* |
| 11 | `VR-016` | L5 | Identity `ACTIVE`, employment present, branch scope non-empty | `401 AUTHENTICATION_FAILED` *(generic)* |
| 12 | `VR-017` | L5 | Snapshot certification onto the session — **record, do not gate** | *(login proceeds)* |
| 13 | `VR-018` | L4 | Provider tokens encrypted server-side; device gets an opaque reference | *(design defect if violated)* |
| 14 | `VR-019` | L4 | Web clients: issue a session-bound CSRF token | — |

Full pseudocode: [`03 §2.1`](../03-VALIDATION-RULE-CATALOGUE.md#21-login-initiation) and
[`§2.2`](../03-VALIDATION-RULE-CATALOGUE.md#22-callback--the-security-critical-step).

### 5.1 The four checks that carry the weight

Everything above matters; these four are the ones an attacker probes.

| Check | Attack it stops | Why the naive version fails |
|---|---|---|
| `state` **atomically** consumed (7) | Login-CSRF, state fixation, callback replay | A non-atomic read-then-mark lets two concurrent callbacks both succeed |
| PKCE `S256`, verifier never leaves the server (6, 8) | Code interception on a native client | `plain` is not a challenge; a verifier in the client is not a secret |
| `nonce` matched to the pending-login (9) | `id_token` replay from another session | Validating signature and `exp` alone accepts a token minted for someone else |
| `return_to` allow-listed (5) | Open redirect → token/session theft | A "same-origin looks fine" check is defeated by a crafted path or a lookalike host |

---

## 6. Outcomes — the complete set

This section **is** the test case list.

| # | Outcome | Trigger | Response | Session | Audit |
|---|---|---|---|---|---|
| 1 | **Success — certified RM** | Everything valid, SP certification `ACTIVE` | `302` → `return_to` | Created | `LoginSucceeded` |
| 2 | **Success — certification expired** | Identity valid, certificate lapsed | `302` → `return_to` | Created | `LoginSucceeded` |
| 3 | **Success — already signed in** | Valid session exists | `200` current status | Reused | — |
| 4 | Invalid client type | `client_type` unknown | `400 INVALID_LOGIN_REQUEST` | none | — |
| 5 | Disallowed return location | `return_to` off the allow-list | `400 RETURN_LOCATION_NOT_ALLOWED` | none | **security event** |
| 6 | Callback replay | `state` already consumed | `400 INVALID_STATE` | none | **security event** |
| 7 | Unknown / forged `state` | No pending-login | `400 INVALID_STATE` | none | **security event** |
| 8 | Expired pending-login | RM abandoned the ceremony past TTL | `400 INVALID_STATE` | none | security event |
| 9 | Code already redeemed | Double callback | `400 CODE_ALREADY_CONSUMED` | none | security event |
| 10 | Nonce mismatch | `id_token` from another ceremony | `401 AUTHENTICATION_FAILED` | none | `LoginFailed` |
| 11 | Unknown subject | No business identity | `401 AUTHENTICATION_FAILED` | none | `LoginFailed{NO_BUSINESS_IDENTITY}` |
| 12 | Identity not `ACTIVE` | Disabled or expired | `401 AUTHENTICATION_FAILED` | none | `LoginFailed{IDENTITY_INACTIVE}` |
| 13 | No employment record | Data gap | `401 AUTHENTICATION_FAILED` | none | `LoginFailed{NO_EMPLOYMENT}` |
| 14 | Empty branch scope | Unmapped RM | `401 AUTHENTICATION_FAILED` | none | `LoginFailed{NO_BRANCH_SCOPE}` |
| 15 | Ceremony failed at Keycloak | Wrong password, MFA failed | Provider page; no BFF callback | none | Keycloak event |
| 16 | Adapter unreachable | Provider adapter down | `503 IDENTITY_PROVIDER_UNAVAILABLE` | none | yes + **alert** |
| 17 | Keycloak unreachable | IdP down | `503 IDENTITY_PROVIDER_UNAVAILABLE` | none | yes + **alert** |
| 18 | Authorization service unreachable | Cannot resolve business identity | `503`, **fail closed** | none | yes + **alert** |
| 19 | Blocked at the edge | WAF rule, rate limit | `403` no body | none | **none** — never entered the VPC |

> **Outcomes 11–14 return one identical response.** They are distinguished only in the event
> stream. Any change that lets a caller tell them apart reintroduces user enumeration and is a
> security regression, not a usability improvement.
>
> **Outcome 2 is the one reviewers query.** An RM with a lapsed certificate logs in successfully and
> then hits `403 SP_CERTIFICATION_REQUIRED` on the first regulated action. That is deliberate:
> certification is a per-action attribute (`AC-1`, `INV-ACT-01`), and blocking login would also block
> the non-selling work they are still entitled to do — including renewing the certificate.

---

## 7. Audit events

| Event | When | Carries | Never carries |
|---|---|---|---|
| `LoginSucceeded` | Session created | `subjectId`, `actorType`, `correlationId`, client type | Credentials, raw provider tokens, PII |
| `LoginFailed` | Any of outcomes 10–14 | Subject reference, **specific** reason code | Whether the username exists, in any caller-visible form |
| `SessionRevoked` | Session destroyed | `subjectId`, reason | — |
| security events | Outcomes 5–9 | Reason, correlation id, source metadata | — |

Delivered through the transactional outbox (`S-17`), at-least-once, retried until acknowledged.

---

## 8. Never, in this flow

| # | Never | Source |
|---|---|---|
| 1 | Return an OAuth access or refresh token to the device | Standing constraint · `TB-1` |
| 2 | Expose Keycloak, Cognito or AD directly to Flutter | Standing constraint |
| 3 | Validate an AD password in the BFF, or persist a password anywhere | auth SSOT §4.1 |
| 4 | Fall back to another authentication path when the IdP is down | `VR-023`, §13 |
| 5 | Tell the caller which of the four identity checks failed | `VR-022`, §11 |
| 6 | Treat the login-time certification snapshot as an entitlement | `VR-017`, `AC-1`, `INV-ACT-01` |
| 7 | Accept a `state` twice, or a non-atomically consumed one | `VR-012` |
| 8 | Redirect anywhere but an allow-listed return location | `VR-011` |
| 9 | Log a provider token, a code, a PKCE verifier or any PII | `C5`, `INV-LOG-01` |
| 10 | Let a business service query Keycloak's database | auth SSOT §10, `VR-030` |
