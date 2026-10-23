# BFF → customer-service → Apigee → CBS (seams S-04 / S-05 / S-26)

[← API reference home](../README.md)

| | |
|---|---|
| **From → to** | `workforce-access-bff` (`HttpCustomerGateway`) → `customer-service` → Apigee token endpoint + Apigee **private** proxy → CBS / EBS customer inquiry |
| **Style** | Synchronous; RM is on screen |
| **Budget** | S-05: 3 s, 1 retry on 5xx/connect, breaker; configured today: Apigee connect 3 s / read 3 s, one retry on 5xx or connect failure; transport failure → `503 UPSTREAM_UNAVAILABLE` (`SUG-20261021-rul`) |
| **Auth** | BFF → customer: mesh identity (target). Customer → Apigee: OAuth2 client-credentials bearer token |
| **Code status** | **IMPLEMENTED** (search + get); lead-first / resolve / snapshot **CONTRACTED** |

## 1. Network path

```text
customer-service pod
  → Transit Gateway attachment (existing AU-CTO-NETWORK TGW — we are a spoke)
  → (AWS Network Firewall on the pod → Apigee hop, ADR-010 remainder)
  → Apigee PRIVATE target (bank's outbound API plane, ADR-020)
  → EBS / CBS customer inquiry (bank data centre over Direct Connect; VPN standby, ADR-009)
```

Never: pod → public internet → Cloudflare → F5 → bank API (hairpin); never a JDBC URL to CBS;
never a stub in `uat`/`prod` (S-26).

## 2. Token handling

| Step | Detail |
|---|---|
| Mint | `POST {APIGEE_TOKEN_URL}?grant_type=client_credentials` with HTTP Basic `clientId:clientSecret` (AU Bank UAT puts `grant_type` on the query string) |
| Cache | `ApigeeAccessTokenHolder`: one token per process, refreshed **single-flight** under a lock 30 s before expiry — concurrent searches never stampede `/token` |
| Use | `Authorization: Bearer <token>` on the CBS inquiry |
| 401 from CBS | Invalidate, re-mint once, retry once; second 401 → `503 UPSTREAM_UNAVAILABLE` |
| Not | A cluster-wide shared token (Valkey) — parked (`SUG-20261006-atk` remainder) |

## 3. Calls

| BFF call | Customer call | CBS call (adapter contract until the bank publishes CBS OpenAPI, `DEP-20260914-apg`) |
|---|---|---|
| `GET /api/v1/customers:search?by&q&countryCode` | `GET /internal/v1/customers/search?by&q&countryCode` | `GET {CBS_INQUIRY_SEARCH_PATH}?by&q&countryCode` → `{items:[…]}` |
| `GET /api/v1/customers/{id}` | `GET /internal/v1/customers/{id}` | `GET {CBS_INQUIRY_GET_PATH}` → `CustomerHit` |

## 4. Status mapping

| CBS / Apigee | customer-service | BFF → app |
|---|---|---|
| 200 with items | `200 {items}` | `200` masked cards |
| 200 empty | `200 {items: []}` | Empty state; **no customer created, no journey** |
| 401 (twice) | `503 UPSTREAM_UNAVAILABLE` | `503` — "do not proceed" |
| 404 | `404 RESOURCE_NOT_FOUND` | `404` |
| 5xx / timeout | `503 UPSTREAM_UNAVAILABLE` | `503` — "Customer records are unavailable right now. Do not proceed" (`AC-EXC-10`) |
| other 4xx | `502 UPSTREAM_BAD_RESPONSE` | `502` |

The request URI (which contains `q`) never enters an exception message or a log (SEC-C2).

## 5. Data

Today nothing is persisted. Target: CBS answers are normalised into `customer.customer`
(HMAC lookup hash + encrypted CIF/PII) and, when a journey starts, frozen into
`customer.customer_snapshot` (S-04). A cached snapshot may answer inside its freshness window when
CBS is down — never an unbounded stale identity.

## 6. Sources

- `services/customer-service/src/main/java/com/bank/platform/customer/**` · `libs/bank-common-apigee/**`
- [Customer search contract](../../ws3-platform/09-nip-bff-customer-search-contract.md) · [Human direction 2026-09-14](../../../architecture/2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md) · [R0 for developers §7](../../../architecture/R0-E2E-FOR-DEVELOPERS.md)
