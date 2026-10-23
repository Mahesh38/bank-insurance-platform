# BFF → identity-provider-adapter → bank AD-verify / Keycloak

[← API reference home](../README.md)

| | |
|---|---|
| **From → to** | `workforce-access-bff` (`IdentityProviderClient`) → `identity-provider-adapter-service` `/internal/v1/auth/*` → bank AD-verify API **or** Keycloak |
| **Style** | Synchronous HTTP (Spring `RestClient`) |
| **Caller auth** | `X-Internal-Service-Key: <IDENTITY_ADAPTER_INTERNAL_SHARED_SECRET>` on every call; adapter answers `401 UNAUTHORIZED` without it. Target: plus Istio STRICT mTLS |
| **Seam** | S-26 (bank systems over the TGW) for AD; private Keycloak |
| **Code status** | **IMPLEMENTED** |

## 1. Which provider for which user

| User | `identitySource` from the app | Adapter call | Provider | Network path (UAT/prod) |
|---|---|---|---|---|
| Bank RM / bank employee | `BANK_AD` | `POST /internal/v1/auth/ad-verify` | **Bank AD-verify API** — returns true/false for an active employee | pod → TGW → (Network Firewall) → **Apigee private** → bank AD-verify. Never LDAP from EKS; never Cloudflare/F5 hairpin |
| Insurer partner rep | `PARTNER` | `authorization-uri` → (browser to Keycloak) → `token-exchange` | **Keycloak** (private IdP realm `bank-insurance`) | Keycloak in-cluster/private; browser reaches Keycloak's login page through the edge only for that page |
| Later: bank AD via Keycloak brokering | `BANK_AD` with `kc_idp_hint` | `authorization-uri` | Keycloak → AD federation | Phase 2 (`OIDC` vs `SAML` vs `LDAP` undecided) |

## 2. Calls

| BFF method | Adapter endpoint | Request | Response | BFF uses it for |
|---|---|---|---|---|
| `verifyAd(employeeId, password)` | `POST /internal/v1/auth/ad-verify` | `{employeeId, password}` | `{authenticated, accountActive, employeeId, username, email}` | Bank login |
| `authorizationUri(…)` | `POST /internal/v1/auth/authorization-uri` | `{state, nonce, codeChallenge, loginHint, identitySource}` | `{authorizationUri}` | Partner login start |
| `exchange(code, verifier, nonce)` | `POST /internal/v1/auth/token-exchange` | `{code, codeVerifier, expectedNonce}` | `ProviderSession{providerSubjectId, username, email, accessToken, refreshToken, idToken, accessTokenExpiresAt, claims}` | Partner callback |
| `revoke(refreshToken)` | `POST /internal/v1/auth/revoke` | `{refreshToken}` | `204` | Logout; failed partner activation |

The password crosses exactly one internal hop (BFF → adapter) and one egress hop (adapter → Apigee
→ AD). Both request records override `toString()` so the password cannot reach a log.

## 3. Error mapping

| Adapter / provider outcome | BFF sees | BFF returns to the app |
|---|---|---|
| AD says false, or `401`/`403` from AD | `200 {authenticated:false}` | `401 AUTHENTICATION_FAILED` |
| AD account inactive | `200 {authenticated:true/false, accountActive:false}` | `401 AUTHENTICATION_FAILED` |
| AD-verify down / odd status / unparseable | `503 IDENTITY_PROVIDER_UNAVAILABLE` (problem+json) | Propagated (incident id kept): `503` |
| Adapter unreachable (no HTTP response) | `RestClientException` | `503 IDENTITY_PROVIDER_UNAVAILABLE` |
| Wrong internal key (misconfiguration) | `401 UNAUTHORIZED` | Propagated per category — fix the secret |
| ID token signature / audience / nonce / subject mismatch | `401 AUTHENTICATION_FAILED` from the adapter | Propagated; login does not complete |

Timeouts: the BFF's client is 1 s connect / **4.5 s** read (`workforce.downstream.timeouts.identity-provider`),
inside the 5 s edge budget (S-01). Inside the adapter, AD-verify and Keycloak calls are each
1 s connect / 3 s read, so the adapter answers before the BFF gives up.

## 4. Sources

- `services/workforce-access-bff/src/main/java/com/bank/workforce/bff/client/IdentityProviderClient.java`
- [identity-provider-adapter-service](../services/identity-provider-adapter-service.md) · [Scenario 01](../scenarios/01-bank-rm-login.md) · [Scenario 02](../scenarios/02-partner-login-keycloak.md)
