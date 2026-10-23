# Headers, session and service-to-service authentication

[← API reference home](../README.md)

Every request on the platform is authenticated twice in two different ways: the **human** is
authenticated by an opaque BFF session, and the **calling service** is authenticated by the mesh
(and today, on the identity adapter, by a shared internal key). This page lists every header that
carries either, and who is allowed to set it.

---

## 1. Public edge — NIP-APP → BFF (`/api/v1/**`)

### 1.1 How the session travels

The BFF accepts the opaque session handle in three places, checked in this order
(`BffSessionCredentials.handle`, `services/workforce-access-bff`):

| Order | Transport | Used by | CSRF required? |
|---|---|---|---|
| 1 | `X-Session-Handle: <handle>` header | Native app (APK / IPA) | No — header transport is not cookie-borne |
| 2 | `Authorization: Bearer <handle>` | Native app, test tools | No. `Authorization: Basic` does **not** skip CSRF |
| 3 | Cookie `WORKFORCE_SESSION=<handle>` | Flutter web in a browser | **Yes** — `X-XSRF-TOKEN` must echo cookie `WORKFORCE-XSRF` |

The handle is **not** an OAuth token. It is a 32-byte random value (base64url, no padding)
minted by the BFF. It is the key of an encrypted record in the session vault
([BFF session vault](../interactions/bff-session-vault.md)). Provider access, refresh and id
tokens stay inside that encrypted record and never leave the BFF.

> Contract docs (`09` customer search) name the cookie `NIPSESSION`. The code names it
> `WORKFORCE_SESSION` (`workforce.session.cookie-name`). Build against the code; the name is
> configuration.

### 1.2 Session cookie attributes (web)

| Attribute | Value | Why |
|---|---|---|
| `HttpOnly` | true | JavaScript cannot read the handle |
| `Secure` | true | HTTPS only |
| `SameSite` | `Strict` | No cross-site sends |
| `Path` | `/` | |
| `Max-Age` | `workforce.session.session-ttl` (default **8 h**) | Same TTL as the vault record |

On logout the BFF sends the same cookie with `Max-Age=0`.

### 1.3 CSRF (web only)

| Item | Value |
|---|---|
| Fetch the token | `GET /api/v1/auth/csrf` → `{ "headerName": "X-XSRF-TOKEN", "token": "…" }` |
| Cookie set | `WORKFORCE-XSRF` (readable by JS by design: `CookieCsrfTokenRepository.withHttpOnlyFalse`) |
| Send on every `POST`/`PUT`/`PATCH`/`DELETE` | `X-XSRF-TOKEN: <token>` |
| Rejection | `403 CSRF_REJECTED` |
| Skipped when | the request carries `X-Session-Handle` or `Authorization: Bearer` |

`POST /api/v1/auth/login` is itself a POST, so a browser must call `GET /api/v1/auth/csrf` first.

### 1.4 Which paths need a session

From `BffSecurityConfig`:

| Path | Rule |
|---|---|
| `/api/v1/auth/**` | `permitAll` — login, callback, native-session, session read, logout, csrf |
| `/actuator/health/**`, `/error` | `permitAll` |
| `/api/v1/**` (everything else) | `authenticated()` — a valid vault session is required |
| anything else | `denyAll` |

A missing handle on a protected path is `401 SESSION_INVALID`; a handle that does not resolve
(unknown, expired, deleted, or fails AES-GCM decryption) is `401 SESSION_EXPIRED`. The two are
deliberately not distinguished further — see [error model](./error-model.md).

### 1.5 Other public headers

| Header | Direction | Required | Rule |
|---|---|---|---|
| `X-Correlation-Id` | request | Contract: **yes** (customer-search contract 09 §3.1). Code today: optional | UUID. Propagated to every downstream hop and stamped into every error |
| `Idempotency-Key` | request | **Yes on every mutating platform API** (`INV-IDM-01`) — contracted; the BFF does not enforce it in code yet | See [idempotency](./idempotency-timeouts-retries.md) |
| `X-Journey-Id` | request | Once a journey exists (HLD §5) | ULID |
| `Content-Type` | request | On bodies | `application/json` |
| `Content-Type` | error response | always | `application/problem+json` |
| `distributorId` | any | **Never accepted** | A caller-supplied value is rejected (`INV-DIS-01`, `ATTRIBUTION_NOT_CALLER_SUPPLIED`) |

---

## 2. Internal hops — service → service (`/internal/v1/**`)

Internal APIs are **cluster-private**. They are not routed on the Istio Ingress Gateway and Flutter
cannot reach them (HLD §5.3).

### 2.1 Service identity

| Layer | Mechanism | Status |
|---|---|---|
| Transport | Istio **STRICT mTLS** east–west (after a dated PERMISSIVE cutover) + retained Kubernetes `NetworkPolicy` (`ADR-023`) | Target (S09/S11) |
| Pod → AWS | IRSA (no static keys in pods) | Target |
| BFF → identity-provider-adapter | `X-Internal-Service-Key: <shared secret>` checked by `InternalServiceAuthFilter` on every `/internal/v1/**` call; wrong or missing key → `401 UNAUTHORIZED` | **Implemented** — production has no default secret (`IDENTITY_ADAPTER_INTERNAL_SHARED_SECRET`) |
| Every other internal hop | Mesh identity only | Target; no app-level check in code today |

> Network location is not trust (Deepali, security §1). Until mesh mTLS is live, only the
> identity adapter checks its caller in code. Do not expose any other `/internal/v1` path outside
> the cluster.

### 2.2 Principal propagation

The human principal crosses internal hops as a **header set by the BFF from the session** — never
from a request body.

| Header | Set by | Value | Read by |
|---|---|---|---|
| `X-Actor-Id` | BFF (from `session.businessUserId`, else `session.username`) | Business user UUID | `lead-service` (required on every call), `identity-authorization-service` (maker / checker), `1sb-integration-service` (`actorId` on jobs and audit; defaults to `system`) |
| `X-Correlation-Id` | BFF (or first service) | UUID | All |
| `Idempotency-Key` | Caller; **server-derived** on internal hops from aggregate id + operation (`03` §5.2) | string ≤ 128 | `1sb-integration-service` today; every mutating internal API in target |
| `X-Internal-Service-Key` | BFF → identity adapter only | secret | `identity-provider-adapter-service` |

**A service must never trust an actor id from a JSON body.** Lead-service, for example, takes the
creator from `X-Actor-Id` and the visible book from that same header — a body cannot widen it.

### 2.3 Calls that leave the building (egress)

| Call | Credential | Where it lives |
|---|---|---|
| `customer-service` → Apigee → CBS | Apigee OAuth2 client-credentials token (`grant_type=client_credentials`, HTTP Basic client id/secret) sent as `Authorization: Bearer` | `APIGEE_CLIENT_ID` / `APIGEE_CLIENT_SECRET` (env / Secrets Manager). One token per process, single-flight refresh (`ApigeeAccessTokenHolder`) |
| `identity-provider-adapter-service` → bank AD-verify API | API key header (`X-API-Key` by default) | `BANK_AD_VERIFY_API_KEY` |
| `identity-provider-adapter-service` → Keycloak | OIDC client secret (BFF client) and admin client-credentials | `KEYCLOAK_BFF_CLIENT_SECRET`, `KEYCLOAK_ADMIN_CLIENT_SECRET` |
| `1sb-integration-service` → 1SB | `Authorization: Basic base64(apiKey:apiSecret)`; `distributorId` injected from secrets | `ONESB_API_KEY`, `ONESB_API_SECRET`, `ONESB_DISTRIBUTOR_ID` via `bank-common-secrets` |
| `payment-service` → AU Bank PG | PG credential | Secrets Manager (target) |

Details of each hop are on its [interaction page](../README.md#4-interactions--how-services-talk-to-each-other).

---

## 3. Sources

- `services/workforce-access-bff/src/main/java/com/bank/workforce/bff/api/BffSessionCredentials.java`
- `services/workforce-access-bff/src/main/java/com/bank/workforce/bff/config/BffSecurityConfig.java`
- `services/workforce-access-bff/src/main/java/com/bank/workforce/bff/api/AuthenticationController.java`
- `services/identity-provider-adapter-service/src/main/java/com/bank/identity/provider/api/InternalServiceAuthFilter.java`
- [R0 HLD §5](../../../architecture/R0-HLD.md#5-api-details) · [Customer search contract](../../ws3-platform/09-nip-bff-customer-search-contract.md) · [Security architecture](../../ws3-platform/04-security-architecture.md)
