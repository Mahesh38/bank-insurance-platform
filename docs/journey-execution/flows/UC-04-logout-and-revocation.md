# UC-04 — Logout, disablement and revocation

**Actors:** RM, IPR (self) · admin (out-of-band) · **Entry:** `POST /api/v1/auth/logout`
**Status:** `AI-DRAFTED` · Security review required · Slice 1

Three different things end a session. They have different triggers, different latencies and
different guarantees, and conflating them is how a disabled seller keeps selling.

| | Trigger | Latency | Guarantee |
|---|---|---|---|
| **A** User logout | The user asks | Immediate | Platform session gone before the response returns |
| **B** Administrative revocation | Disablement, scope change, permission change | **Next request** | Bounded by `policy_version`, not by token lifetime |
| **C** Emergency suspension | Security or compliance action | Next request, **and** overrides every grant | Absolute deny even against a stale cached decision |

---

## 1. A — user logout

### Route

| # | From → to | Layer | Style | Notes |
|---|---|---|---|---|
| 1 | Device → edge → BFF | L0→L4 | sync | Session reference required |
| 2 | BFF → session vault | L4 | sync | **Delete first, locally** |
| 3 | BFF → adapter → Keycloak | L4→L5 | sync, **best effort** | Provider revoke |
| 4 | BFF → device | L4→L0 | — | `204`, cookie cleared |

### Algorithm

```text
VR-024  logout(session_ref)
 1  session := vault.get(session_ref)
 2  vault.delete(session_ref)
    ← the PLATFORM session dies FIRST and LOCALLY. Everything after this is best-effort.
      Ordering the provider call first would let a provider outage keep the user signed in.
 3  best_effort: adapter.revoke(session.provider_tokens)
       On failure: raise an operations task. Do NOT fail the logout.
                   Do NOT resurrect the session.
       Exposure is bounded by the short access-token lifetime (5–10 min target).
 4  clear the cookie (WEB clients)
 5  emit SessionRevoked{subjectId, reason: USER_LOGOUT}
 6  → 204
 7  IDEMPOTENT: logging out an already-dead or unknown session is also 204.
    ← never 401. A logout that errors because you are already logged out is a bug
      that teaches users to ignore errors.
```

### Outcomes

| # | Outcome | Trigger | Response | Provider session |
|---|---|---|---|---|
| 1 | Clean logout | Valid session | `204` | Revoked |
| 2 | Logout, provider unreachable | Adapter/Keycloak down | `204` | **Not revoked** — ops task raised |
| 3 | Logout with no session | Already gone | `204` | — |
| 4 | Logout with an unknown reference | Stale client | `204` | — |
| 5 | Concurrent logout | Two devices, same session | `204` both | Revoked once |

Outcome 2 is the honest one: the platform session is definitively gone, the provider session may
briefly survive, and the residual exposure is a short-lived access token that no longer maps to any
platform session. Saying "logout failed" would be both false and worse.

---

## 2. B — administrative revocation

Out-of-band changes do not reach into live sessions. They increment a version, and every session
notices on its next request.

```text
VR-025  administrative_change(identity, change)
 1  Apply the change: disablement · branch or insurer removal · role or permission change
 2  identity.policy_version += 1
 3  Publish the corresponding event:
       IdentityDisabled | BranchAssignmentChanged | RoleRevoked |
       EntitlementRevoked | CertificationChanged | CertificationExpired
 4  No session is touched directly.
 5  Every live session detects the mismatch at step 6 of validate_session (UC-03).

    Result: bounded by the NEXT REQUEST, not by token lifetime.
```

| Change | Effect on a live session | Rule |
|---|---|---|
| Account disabled | `401 SESSION_REVOKED`, destroyed | `VR-025` |
| Branch removed | Principal narrows; out-of-scope resources deny immediately | `VR-044` |
| Insurer relationship ended (IPR) | Rows vanish from reads; writes deny | `VR-043`, `VR-066` |
| Role revoked | Cached decisions invalidated; new decisions deny | `VR-042` |
| Certification expired | **Regulated selling actions** deny; non-selling work continues | `VR-040` |
| Permission granted | Takes effect on the next request, same mechanism | `VR-025` |

> The certification row is the one people get wrong in both directions. It must not deny everything
> (the RM still has legitimate non-selling work, including renewing the certificate), and it must
> not be deferred to the next login (which could be days). Per-action evaluation gives exactly the
> right answer without a special case.

---

## 3. C — emergency suspension

Suspension is not a stronger revocation; it sits at a **different place in the precedence chain**.

```text
VR-026  suspension precedence                                  ← auth SSOT §8.3
    account/global suspension          ← evaluated FIRST, denies unconditionally
      > explicit scoped deny
      > direct scoped grant
      > role-derived scoped grant
      > default deny

 1  A suspended principal is denied even where a direct grant, a break-glass grant, or a
    still-valid cached decision would otherwise allow.
 2  It is not "revoke everything and hope no grant was missed". It is one check, above
    everything, that cannot be out-voted.
 3  ACCESS_ALL does NOT bypass it, and is separately audited in its own right. [VR-047]
```

| # | Outcome | Response | Audit |
|---|---|---|---|
| 6 | Suspended principal, live session | `401 SESSION_REVOKED` on the next request | `SessionRevoked` |
| 7 | Suspended principal, new login | `401 AUTHENTICATION_FAILED` *(generic)* | `LoginFailed{IDENTITY_INACTIVE}` |
| 8 | Suspended principal with a direct grant | `403 ACCOUNT_SUSPENDED` | compliance event |
| 9 | Suspended principal, cached allow within TTL | `403 ACCOUNT_SUSPENDED` | compliance event |

Outcome 9 is the reason suspension is checked at the top rather than implemented as a bulk
grant-removal: a cached decision that has not yet expired must still be overridden.

---

## 4. Complete outcome set

Outcomes 1–5 (§1), 6–9 (§3), plus:

| # | Outcome | Trigger | Response |
|---|---|---|---|
| 10 | Scope narrowed mid-session | Branch removed | `200`, narrower results, immediately |
| 11 | Certification lapses mid-session | Expiry passes during the session | Non-selling `200`; selling `403 SP_CERTIFICATION_REQUIRED` |
| 12 | Authorization service unreachable during check | PDP down | `403 AUTHORIZATION_UNAVAILABLE` — **fail closed** |
| 13 | Revocation event lost | Outbox backlog | `policy_version` still mismatches on the next request; **revocation is not event-dependent** |

Outcome 13 is why revocation reads a version rather than consuming an event: a lost or delayed event
must not become a missed revocation.

---

## 5. Audit events

| Event | When |
|---|---|
| `SessionRevoked` | Every session termination, with its reason |
| `IdentityDisabled` / `IdentityEnabled` / `IdentityExpired` | Lifecycle change |
| `RoleRevoked` / `EntitlementRevoked` | Permission change |
| `BranchAssignmentChanged` / `HierarchyChanged` | Scope change |
| `CertificationChanged` / `CertificationExpired` | Certification change |

Retained per policy — **seven years initially**, configurable pending Compliance confirmation
(auth SSOT accepted decision 8). Retention is configuration, not a code constant.

---

## 6. Never, in this flow

| # | Never | Source |
|---|---|---|
| 1 | Fail a logout because the provider leg failed | `VR-024` |
| 2 | Resurrect a platform session after a failed provider revoke | `VR-024` |
| 3 | Return `401` from a logout on an already-dead session | `VR-024` |
| 4 | Wait for token expiry as the revocation mechanism | `VR-025` |
| 5 | Depend on an event being delivered for a revocation to take effect | `VR-025` |
| 6 | Let any grant, cache or `ACCESS_ALL` out-vote a suspension | `VR-026`, `VR-047` |
| 7 | Deny non-selling work because a certification lapsed | `VR-040` |
| 8 | Allow a write on a cached decision while the PDP is unavailable | `VR-045` |
