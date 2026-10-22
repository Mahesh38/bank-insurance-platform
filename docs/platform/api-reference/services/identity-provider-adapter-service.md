# identity-provider-adapter-service — workforce authentication adapter (#3, WS-2)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | Puts every identity provider behind one internal API so the BFF never knows which IdP it is talking to. Today: **bank AD-verify API** for bank RMs, **Keycloak** (private IdP) for partner users and provisioning. Swapping Keycloak for another IdP changes only this service (`ADR-015`, WS-2 out-of-scope "production IdP selection") |
| **Module** | `services/identity-provider-adapter-service` · port 8082 · `service-id: idp-adapter` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1` — cluster-private |
| **Callers** | `workforce-access-bff` (auth) · `identity-authorization-service` (provisioning, after maker-checker approval) |
| **Caller authentication** | `X-Internal-Service-Key` header on **every** `/internal/v1/**` call, compared by `InternalServiceAuthFilter`. Missing / wrong → `401 UNAUTHORIZED` |
| **Calls out** | Bank AD-verify API (UAT/prod: Apigee **private** target; never LDAP from EKS) · Keycloak OIDC endpoints, JWKS and admin REST API — every outbound client bounded at connect 1 s / read 3 s (`identity.ad-verify.*-timeout`, `identity.provider.keycloak.connect-timeout` / `read-timeout`), nesting inside the BFF's 4.5 s budget |
| **ID-token failures** | Signature, audience, nonce or subject mismatch on token exchange / refresh → typed `401 AUTHENTICATION_FAILED` (was an unmapped `500`) |
| **Owns data** | None. Keycloak's realm DB belongs to Keycloak |
| **Code status** | **IMPLEMENTED** — all 7 endpoints |
| **Contract** | **Authoritative (ADR-027):** [`contracts/identity-provider-adapter-service/openapi.yaml`](../../contracts/identity-provider-adapter-service/openapi.yaml) · [LLD](../../contracts/identity-provider-adapter-service/LLD.md). Where this page and the contract differ, the contract wins. |

---

## 1. API index

| Method | Path | Status | Called by | Purpose |
|---|---|---|---|---|
| `POST` | `/internal/v1/auth/ad-verify` | IMPLEMENTED | BFF | Verify bank employee id + password; true/false for an **active** AD account |
| `POST` | `/internal/v1/auth/authorization-uri` | IMPLEMENTED | BFF | Build the Keycloak OIDC authorize URL (PKCE S256) |
| `POST` | `/internal/v1/auth/token-exchange` | IMPLEMENTED | BFF | Exchange authorization code → tokens + verified claims |
| `POST` | `/internal/v1/auth/refresh` | IMPLEMENTED | BFF (not yet wired) | Refresh provider tokens |
| `POST` | `/internal/v1/auth/revoke` | IMPLEMENTED | BFF on logout / failed login | Revoke a refresh token |
| `POST` | `/internal/v1/identities` | IMPLEMENTED | Authz provisioning outbox | Create (or find) a Keycloak user for an approved partner |
| `PATCH` | `/internal/v1/identities/{providerSubjectId}/status` | IMPLEMENTED | Authz | Enable / disable a Keycloak user |

---

## 2. Endpoints

### 2.1 `POST /internal/v1/auth/ad-verify`

**Contract**

| Field | Type | Validation |
|---|---|---|
| `employeeId` | string | `@NotBlank` |
| `password` | string | `@NotBlank`. `toString()` prints only whether an id is present; the password is never logged or retained |

```json
{ "employeeId": "EMP001", "password": "••••••" }
```

Response `200` (always 200 for a rejected credential — a rejection is data, not an error):

```json
{ "authenticated": true, "accountActive": true, "employeeId": "EMP001", "username": "EMP001", "email": "emp001@example.test" }
```

The BFF accepts the login only when `authenticated && accountActive`.

**Behind the scenes (`BankAdVerifyAdapter`)**

1. Re-check `employeeId` non-blank and `password` non-null → else `422`-class `SCHEMA_INVALID` (400).
2. **Mode `stub`** (local only): constant-time compare against `identity.ad-verify.stub.users`.
   Unknown / wrong password → `{false,false}`; known but `active: false` → `{false,false}`.
   `stub` **refuses to start under the `prod` profile**.
3. **Mode `http`**: `POST {BANK_AD_VERIFY_BASE_URL}{BANK_AD_VERIFY_PATH}` (default path
   `/ad/authenticate`) with body `{employeeId, password}` and API key header
   (`X-API-Key` by default). Timeouts 1 s connect / 3 s read (were 3 s / 5 s, which no longer fit
   inside the BFF's 4.5 s identity-provider budget).
4. Response parsing is tolerant because the bank has not published its schema
   (`DEP-20260914-apg`): a bare `true`/`false`, a JSON boolean, a quoted boolean, or an object with
   `authenticated` | `success` | `isAuthenticated` and optional `accountActive` / `active`,
   `username` | `userId` | `employeeId`, `email`.
5. Bank answers `401`/`403` → treated as rejected credentials (`{false,false}`), not an outage.
6. Any other HTTP status, a transport failure, or an unparseable body → `503 IDENTITY_PROVIDER_UNAVAILABLE`.

| HTTP | Code | When |
|---|---|---|
| 200 | — | Accepted or rejected |
| 401 | `UNAUTHORIZED` | Caller lacks the internal key |
| 400 / 422 | `SCHEMA_INVALID` / `VALIDATION_ERROR` | Missing field |
| 503 | `IDENTITY_PROVIDER_UNAVAILABLE` | AD-verify down / unexpected status / unparseable |

Network path in UAT/prod: pod → TGW → (Network Firewall) → **Apigee private** → bank AD-verify.
No Cloudflare/F5 hairpin. See [BFF to IdP adapter](../interactions/bff-to-identity-provider-adapter.md).

### 2.2 `POST /internal/v1/auth/authorization-uri`

| Field | Validation |
|---|---|
| `state`, `nonce`, `codeChallenge`, `identitySource` | `@NotBlank` |
| `loginHint` | optional |

Response `{ "authorizationUri": "https://…/protocol/openid-connect/auth?client_id=workforce-bff&redirect_uri=…/api/v1/auth/callback&response_type=code&scope=openid%20profile%20email&state=…&nonce=…&code_challenge=…&code_challenge_method=S256" }`.
Adds `login_hint` when given and `kc_idp_hint` when `identitySource = BANK_AD` and a hint is
configured (Keycloak brokering to bank AD — Phase 2). Pure URL building; no outbound call.

### 2.3 `POST /internal/v1/auth/token-exchange`

Request `{ "code", "codeVerifier", "expectedNonce" }` (all `@NotBlank`).

Behind the scenes:

1. `POST` Keycloak token endpoint, form `grant_type=authorization_code`, `code`, `code_verifier`,
   `redirect_uri`, `client_id`, `client_secret`.
2. Decode and **verify the ID token** (signature via JWKS); audience must contain the client id;
   `nonce` must equal `expectedNonce`.
3. `GET` userinfo with the access token; userinfo `sub` must equal the ID token subject.
4. Return:

```json
{
  "providerSubjectId": "f3c1…",
  "username": "partner.user",
  "email": "partner.user@insurer.example",
  "accessToken": "…",
  "refreshToken": "…",
  "idToken": "…",
  "accessTokenExpiresAt": "2026-10-21T10:05:00Z",
  "claims": { "sub": "f3c1…", "business_user_id": "…", "insurer_code": "INS01" }
}
```

These tokens go **only** to the BFF, which encrypts them into the session vault. They never reach
Flutter. Audience / nonce / subject mismatch raises `IllegalStateException` (currently unmapped in
this service → 500 to the BFF, surfaced as an upstream failure).

### 2.4 `POST /internal/v1/auth/refresh`

Request `{ "refreshToken" }`. Form `grant_type=refresh_token` → same `ProviderSessionResponse`
(no nonce check). Not called by the BFF yet — sessions currently live for the BFF session TTL.

### 2.5 `POST /internal/v1/auth/revoke`

Request `{ "refreshToken" }`. `POST` Keycloak revocation endpoint with
`token_type_hint=refresh_token`. Response `204`.

### 2.6 `POST /internal/v1/identities`

| Field | Validation |
|---|---|
| `businessUserId` | `@NotBlank` |
| `username` | `@NotBlank` |
| `email` | `@Email @NotBlank` |
| `firstName`, `lastName`, `insurerCode` | optional |
| `enabled` | boolean |

Behind the scenes: admin token via `client_credentials` → search Keycloak users by exact
`username` → if one exists **and** its `business_user_id` attribute matches, return it (idempotent
re-provision); if the username belongs to someone else → failure ("Provider username is already
assigned"); else create the user with attributes `business_user_id`, `insurer_code` and required
action `UPDATE_PASSWORD`. Response `201 { "providerSubjectId": "<keycloak user id>" }`.

### 2.7 `PATCH /internal/v1/identities/{providerSubjectId}/status`

Request `{ "enabled": true|false }` (`@NotNull`). `PUT` Keycloak admin user `{enabled}`. `204`.

---

## 3. What this service must never do

- Hand a provider token to Flutter, or to anyone but the BFF.
- Decide business authorization (Keycloak groups are not permissions — the PDP is).
- Talk LDAP to AD from EKS, or reach AD over the public internet.
- Persist or log a password.

## 4. Sources

- `services/identity-provider-adapter-service/src/main/java/com/bank/identity/provider/api/IdentityProviderController.java` · `InternalServiceAuthFilter.java`
- `…/adapter/adverify/BankAdVerifyAdapter.java` · `…/adapter/keycloak/KeycloakIdentityProviderAdapter.java` · `src/main/resources/application.yml`
- [Authentication & authorization](../../authentication-authorization/README.md) · [IAM-001](../../authentication-authorization/IAM-001.work-item.yaml) · [Human direction 2026-09-14](../../../architecture/2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md)
