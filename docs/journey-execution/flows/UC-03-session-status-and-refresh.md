# UC-03 — Session status, validation and refresh

**Actors:** RM, IPR · **Entry:** `GET /api/v1/auth/session`, and **implicitly every other request**
**Status:** `AI-DRAFTED` · Security review required · Slice 1

This is the most-executed flow in the platform: steps 5–9 of the standard request algorithm run on
**every authenticated call**, not only on `GET /session`. Get this wrong and every other flow
inherits it.

---

## 1. Two things this flow does

| | Purpose | Runs when |
|---|---|---|
| **A** Session validation | Decide whether this request has a live principal | Every authenticated request |
| **B** Provider-token refresh | Keep the server-side token usable for internal calls | Lazily, when a down-scoped token has expired |

**B is invisible to the device.** The device holds an opaque reference whose lifetime is governed by
idle and absolute session limits — never by an OAuth token's `exp`. Refresh is a server-side
concern, which is the point of token-hiding.

---

## 2. Route

| # | From → to | Layer | Style | Budget | Notes |
|---|---|---|---|---|---|
| 1 | Device → edge → BFF | L0→L4 | sync | 5 s edge | Cookie (web) or opaque handle (native) |
| 2 | BFF → session vault | L4 | sync | — | Redis/session vault; provider tokens encrypted |
| 3 | BFF → `identity-authorization-service` | L4→L5 `TB-3` | sync | — | `policy_version` comparison |
| 4 | BFF → `identity-provider-adapter-service` | L4→L5 | sync | — | **Only if** a refresh is needed |
| 5 | adapter → Keycloak | L5 | sync | — | Rotating refresh token |
| 6 | BFF → device | L4→L0 | — | — | Status only. **Never a token** |

Hops 4–5 are skipped on the overwhelming majority of requests.

---

## 3. Validation, in execution order

Pseudocode: [`03 §2.3`](../03-VALIDATION-RULE-CATALOGUE.md#23-session-validation-on-every-subsequent-request).

| # | Rule | Check | Failure |
|---|---|---|---|
| 1 | — | Session reference present | `401 SESSION_INVALID` |
| 2 | — | Reference resolves in the vault | `401 SESSION_INVALID` |
| 3 | `VR-020` | `absolute_expiry > now` | `401 SESSION_EXPIRED`, destroy |
| 4 | `VR-020` | `idle_expiry > now` | `401 SESSION_EXPIRED`, destroy |
| 5 | `VR-019` | Web + mutation: CSRF token valid **and** Origin/Referer allow-listed | `403 CSRF_REJECTED` |
| 6 | `VR-025` | `session.policy_version` matches current | revalidate; `401 SESSION_REVOKED` if suspended |
| 7 | `VR-020` | Slide the idle window | — |
| 8 | `VR-021` | Refresh if needed; **reuse detection** | `401 SESSION_REVOKED`, kill the family |
| 9 | `VR-018` | Principal read from the vault, never from the request | — |

### 3.1 The three that carry the weight

**Step 6 — `policy_version` is how revocation lands mid-session.** Without it, a disabled RM keeps
working until their session expires, which on an absolute lifetime measured in hours is not an
acceptable revocation SLA for a regulated seller.

```text
VR-025  revocation_detection(session)
 1  current := authorization_service.current_policy_version(session.principal.id)
 2  IF current = session.policy_version → proceed. This is the common path; keep it cheap.
 3  ELSE
       invalidate every cached authorization decision for this principal
       snapshot := authorization_service.principal_snapshot(principal.id)
       IF snapshot.status ∈ {SUSPENDED, DISABLED}
          destroy the session
          → 401 SESSION_REVOKED
       ELSE
          session.principal := snapshot          ← scope narrowed or widened; both take effect NOW
          session.policy_version := current
 4  Bounded by the NEXT REQUEST, not by token lifetime. That is the revocation SLA.
```

**Step 8 — refresh-token reuse means the token leaked.**

```text
VR-021  refresh(session)
 1  new := adapter.refresh(session.provider_tokens.refresh)     ← rotating refresh token
 2  IF the adapter reports REUSE of an already-rotated refresh token
       terminate the ENTIRE session family for this principal — not just this session
       emit SessionRevoked{reason: REFRESH_REUSE_DETECTED} + ALERT
       → 401 SESSION_REVOKED
    ← Reuse has exactly two explanations: a race, or theft. Treating it as a race is a
      decision to accept the theft case. Kill the family and make the user sign in again.
 3  IF the adapter is unreachable
       → 503. Do NOT extend the session on the assumption that refresh would have worked. [VR-023]
```

**Step 5 — CSRF applies to cookie clients only, and to mutations only.** A native client holding an
opaque handle in the Keychain is not subject to ambient credential attachment; a cookie client is.

---

## 4. What `GET /session` returns

| Field | Value | Notes |
|---|---|---|
| `authenticated` | boolean | — |
| `principal.id` | business identity id | Not the provider subject |
| `principal.actorType` | `BANK_RM` \| `INSURER_PARTNER_REP` | Closed vocabulary |
| `principal.branchScope` | branch codes | — |
| `principal.insurerId` | present for IPR only | — |
| `certification` | status, LOB coverage, expiry | **Display and telemetry only** (`VR-017`) |
| `expiresAt` | absolute expiry | For UX warnings |
| — | **no token of any kind** | `VR-018` |

> The `certification` block is the field most likely to be misused. It exists so the workspace can
> warn *"your certificate expires in 9 days"*. It is **not** an entitlement: the client must never
> branch on it to decide whether an action is permitted. That decision belongs to the PDP at the
> instant of the action (`VR-040`, `INV-ACT-01`). A client that hides a button based on this field
> is a convenience; a client that *relies* on it is a control failure waiting for a direct API call.

---

## 5. Outcomes

| # | Outcome | Trigger | Response | Session after |
|---|---|---|---|---|
| 1 | **Valid** | All checks pass | `200` status | Idle window slid |
| 2 | **Valid, refreshed** | Down-scoped token expired | `200` status | Tokens rotated |
| 3 | No session reference | Not signed in | `401 SESSION_INVALID` | none |
| 4 | Unknown reference | Vault miss, or already destroyed | `401 SESSION_INVALID` | none |
| 5 | Idle timeout | No activity past `idle_ttl` | `401 SESSION_EXPIRED` | destroyed |
| 6 | Absolute timeout | Past `absolute_ttl` regardless of activity | `401 SESSION_EXPIRED` | destroyed |
| 7 | CSRF missing / mismatched | Cookie client, mutation | `403 CSRF_REJECTED` | preserved |
| 8 | Origin/Referer not allow-listed | Cross-site attempt | `403 CSRF_REJECTED` | preserved + security event |
| 9 | **Revoked mid-session** | Disabled or suspended out of band | `401 SESSION_REVOKED` | destroyed |
| 10 | **Scope changed mid-session** | Branch or role changed | `200`; new scope applies immediately | Principal refreshed |
| 11 | **Refresh reuse detected** | Rotated refresh token replayed | `401 SESSION_REVOKED` | **entire family destroyed** + alert |
| 12 | Adapter unreachable on refresh | Provider down | `503` | preserved, not extended |
| 13 | Authorization service unreachable at step 6 | PDP service down | `403 AUTHORIZATION_UNAVAILABLE` for writes; cached reads may serve to TTL | preserved |
| 14 | Step-up required | Privileged action, stale authentication | `401 STEP_UP_REQUIRED` | preserved |

Outcome 10 is the one to test hardest: a scope *narrowing* must take effect on the very next
request. A test that only covers widening will pass while the control fails.

Outcome 14 replays the original request **with its original idempotency key** after MFA — a new key
would turn one intended action into two (`VR-001`).

---

## 6. Never, in this flow

| # | Never | Source |
|---|---|---|
| 1 | Return an OAuth access or refresh token to the device | standing constraint |
| 2 | Read the principal from the request body or a client-supplied header | `VR-018` |
| 3 | Extend a session because refresh *would probably* have worked | `VR-023` |
| 4 | Treat refresh-token reuse as a benign race | `VR-021` |
| 5 | Serve a cached **write** authorization when the PDP is unavailable | `VR-045` |
| 6 | Let the client's `certification` block gate a regulated action | `VR-017`, `INV-ACT-01` |
| 7 | Log a session reference, a provider token or a CSRF token | `C5`, `INV-LOG-01` |
| 8 | Skip CSRF for a cookie client because the request "looks internal" | `VR-019` |
