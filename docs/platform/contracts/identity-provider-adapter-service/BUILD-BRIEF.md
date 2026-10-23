# identity-provider-adapter-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Deepali (security) and Amit (engineering) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Service page](../../api-reference/services/identity-provider-adapter-service.md) · [Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

| Area | State |
|---|---|
| Code | **Implemented** (13 main / 6 test classes); 7 / 7 operations coded; ArchUnit test present |
| Gaps (LLD §8) | Keycloak and token-verification failures surface as unmapped `500`; Keycloak clients have **no timeouts**; shared-secret caller auth is interim (mTLS target) |
| Round 2 | **E8**: AD-verify connect **2 s** / read **3 s**; Keycloak connect 2 s / read 3 s; all by environment variable. **E5**: partners are provisioned at creation (caller behaviour changes, this API does not) |

Maturity estimate: ≈ 70 %.

## 2. Scope for this build

1. E8 timeouts as `@ConfigurationProperties` with env overrides for AD-verify, Keycloak token,
   Keycloak admin.
2. Map Keycloak / token-verification failures **only after** the reviewed contract change in LLD §8
   #1 lands (codes `UPSTREAM_UNAVAILABLE` / `UPSTREAM_AUTH_FAILURE` / `AUTHENTICATION_FAILED`).
   Until then keep the contract's current responses (IDP-1).
3. Markers + conformance test; PII/password log-scan test.

## 3. Operations

[openapi.yaml](./openapi.yaml): `verifyAd`, `buildAuthorizationUri`, `exchangeCode`,
`refreshTokens`, `revokeRefreshToken`, `provisionIdentity`, `setIdentityEnabled` — all coded.

## 4. Data owned

None.

## 5. Rule sets

None. Timeouts are technical properties (playbook §6).

## 6. Events

None.

## 7. Synchronous dependencies

| Dependency | Budget (E8) | Until it exists |
|---|---|---|
| Bank AD-verify (Apigee private target) | connect 2 s / read 3 s | Local `stub` mode (refused under `prod`) |
| Keycloak OIDC + admin REST | connect 2 s / read 3 s | Local Keycloak or WireMock fixtures of the Keycloak endpoints used |

Inbound budgets that must stay above these: BFF 4.5 s (E8); identity-authorization provisioning client.

## 8. Invariants

Only authenticated internal callers reach any endpoint · AD stub never runs in prod · password
never logged · ID token audience / nonce / subject verified · one Keycloak user per business user
(`business_user_id` attribute).

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-IDP-01 | No / wrong `X-Internal-Service-Key` → `401 UNAUTHORIZED` | LLD §2 |
| AT-IDP-02 | AD answers 401/403 → `200 {authenticated:false}` | LLD §3.1 |
| AT-IDP-03 | AD answers 5xx, or nothing within 3 s → `503 IDENTITY_PROVIDER_UNAVAILABLE` in < 5.5 s | E8 |
| AT-IDP-04 | Keycloak token endpoint silent for 3 s → client times out (no hang) | E8, LLD §8 #2 |
| AT-IDP-05 | Provision twice with the same `businessUserId` → same `providerSubjectId`; same username with a different `business_user_id` → failure per contract | LLD §3.3 |
| AT-IDP-06 | Startup with AD `stub` under `prod` profile is refused | LLD §6 |
| AT-IDP-07 | Log capture during ad-verify contains no password | LLD §6, `INV-LOG-01` |
| AT-IDP-08 | Token exchange with wrong nonce is refused (current contract response) | LLD §4 |
| AT-IDP-09 | Conformance test passes | ADR-027 |

## 10. Build order

**Tier 1** (identity edge). No platform upstream. Startable now.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| IDP-1 | Contract change for Keycloak / token failure codes (LLD §8 #1) | Deepali / Engineering | Mapping work |
| IDP-2 | Shared-secret auth → Istio STRICT mTLS (ADR-023) | Shivanshi (S09/S11) | Go-live |
| IDP-3 | Bank AD-verify production endpoint and Apigee exposure | Bank | Go-live |
