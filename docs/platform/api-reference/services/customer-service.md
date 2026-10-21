# customer-service — Customer (#4)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The platform's window onto the bank's customer master. Finds an **existing-to-bank (ETB)** customer in CBS by Customer ID (CIF), PAN or mobile, returns a **masked** identity, and (target) keeps a journey-scoped **snapshot** so the sale is evidenced against what CBS said at the time. CBS stays the master — this service **never writes CBS** |
| **Module** | `services/customer-service` · port 8090 · `service-id: customer` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1/customers` — cluster-private |
| **Callers** | BFF (search, confirm card) · target: Journey (snapshot / prefill, S-04) |
| **Calls out** | Apigee token endpoint (`client_credentials`) · Apigee **private** proxy → CBS / EBS customer inquiry |
| **Owns data** | Target: Aurora schema **`customer`** (`customer`, `customer_snapshot`) · role `app_customer`. Today: no customer data persisted |
| **Code status** | **IMPLEMENTED-PARTIAL** — search and get via Apigee → CBS (or stub). Local resolve, snapshot, lead-first orchestration and search audit are **CONTRACTED** |

---

## 1. API index

| Method | Path | Status | Caller | Purpose |
|---|---|---|---|---|
| `GET` | `/internal/v1/customers/search?by&q&countryCode` | IMPLEMENTED | BFF | CBS inquiry; masked hits |
| `GET` | `/internal/v1/customers/{customerId}` | IMPLEMENTED | BFF | One masked customer |
| `GET` | `/internal/v1/customers:resolve?by&q` | CONTRACTED (09 §2.4, HLD §5.3) | BFF | **Local store only** — no Apigee; returns book-scoped `customerId`s |
| `GET` | `/internal/v1/customers:lookup?by&q` | CONTRACTED | BFF | Snapshot via Apigee → CBS; full profile internally, never to Flutter |
| `POST` | `/internal/v1/customers/{customerId}/snapshots` | PROPOSED | Journey (S-04) | Freeze a journey-scoped snapshot (`customer_snapshot`) for prefill and evidence |
| `GET` | `/internal/v1/customer/info` | IMPLEMENTED | Ops | Skeleton info |

---

## 2. Endpoints

### 2.1 `GET /internal/v1/customers/search`

| Query | Required | Validation (code) |
|---|---|---|
| `by` | yes | Must be `CUSTOMER_ID`, `MOBILE` or `PAN`, else `422 INVALID_REQUEST` |
| `q` | yes | Non-blank, else `422 MISSING_REQUIRED_FIELD`. **Never logged; never put into an exception message** (SEC-C2) |
| `countryCode` | no | Forwarded for `MOBILE` |

Per-`by` format checks (contract 09 §3.1: CIF `[A-Za-z0-9]{1,20}`, PAN `^[A-Z]{5}[0-9]{4}[A-Z]$`
uppercased, mobile 10 digits starting 6–9 or `+91`) are **contracted at the BFF**, not yet coded.

Response `200`:

```json
{
  "items": [
    { "customerId": "CUST-0433", "displayName": "A U Customer", "initials": "AU",
      "maskedCif": "XXXXX0433", "maskedMobile": "XXXXXX0433", "eligibility": "ETB", "source": "CBS" }
  ]
}
```

Empty CBS answer → `200 { "items": [] }` — the BFF shows "no match", it **never invents a customer
and never opens a journey** (`AC-CUST-010-2`).

**Behind the scenes (`CustomerSearchService` → `HttpCustomerDirectory`)**

```mermaid
sequenceDiagram
    participant BFF
    participant CS as customer-service
    participant TH as ApigeeAccessTokenHolder (in-process)
    participant AT as Apigee /oauth/accesstoken
    participant AX as Apigee private proxy
    participant CBS as CBS / EBS
    BFF->>CS: GET /internal/v1/customers/search?by=CUSTOMER_ID&q=…
    CS->>CS: validate by ∈ {CUSTOMER_ID, MOBILE, PAN}; q non-blank
    CS->>TH: currentAccessToken()
    alt cached token usable (expiry − 30 s)
        TH-->>CS: token
    else missing / expired
        TH->>AT: POST ?grant_type=client_credentials (Basic clientId:secret) — single-flight under lock
        AT-->>TH: access_token, expires_in
    end
    CS->>AX: GET {searchPath}?by&q&countryCode  Authorization: Bearer token
    AX->>CBS: customer inquiry
    alt 200
        CBS-->>CS: {items:[…]} (adapter contract until the bank publishes CBS OpenAPI)
        CS-->>BFF: 200 {items}
    else 401
        CS->>TH: invalidate(); currentAccessToken() (re-mint once)
        CS->>AX: retry once
        Note over CS: second 401 → 503 UPSTREAM_UNAVAILABLE
    else 404
        CS-->>BFF: search: propagated as RESOURCE_NOT_FOUND
    else 5xx
        CS-->>BFF: 503 UPSTREAM_UNAVAILABLE
    else other 4xx
        CS-->>BFF: 502 UPSTREAM_BAD_RESPONSE
    end
```

### 2.2 `GET /internal/v1/customers/{customerId}`

Same token handling; `GET {getPath}` with the id expanded. CBS `404` → `Optional.empty()` →
`404 RESOURCE_NOT_FOUND` ("customer is absent from this book"). Blank id →
`422 MISSING_REQUIRED_FIELD`.

### 2.3 Contracted: `customers:resolve` and `customers:lookup`

The customer-search contract splits search into two internal calls so the BFF can answer
**lead-first** without touching CBS when the RM is resuming their own work:

| Call | Reads | Network | Returns |
|---|---|---|---|
| `GET /internal/v1/customers:resolve?by&q` | `customer.customer` by **HMAC of CIF** (`cif_lookup_hash`) — never plaintext CIF | none | 0..n local `customerId`s in the caller's book |
| `GET /internal/v1/customers:lookup?by&q` | CBS via Apigee | Apigee private → CBS | `customerId` + full normalised profile (internal only); unknown CBS fields die here |

The BFF masks before anything reaches Flutter: `fullName`, `initials`, `maskedCif`
(last 4, rest `X`), `maskedMobile`, `maskedEmail`, `eligibility` (`ETB`/`NOT_ETB`). PAN, DOB,
address, income are **never** on the search wire. See [Scenario 04](../scenarios/04-customer-search.md).

### 2.4 Proposed: journey snapshot

When the journey starts (`INITIATED`, S-04 Journey → Customer, 2 s, 1 retry), the customer
service freezes what CBS said into `customer.customer_snapshot` (`snapshot_id`, `customer_id`,
`journey_id`, `lead_id`, `lob`, `pii_enc`, `snapshot_taken_at`, `source_system`). Journey stores
only `party_snapshot_ref`. Prefill of PAN / DOB / address happens later (`SCR-07`, after
`CNS-DP` consent). Shape is PROPOSED here — confirm with Mahesh / Aarti before building.

---

## 3. Data — schema `customer` (target)

| Table | Columns that matter | Rule |
|---|---|---|
| `customer` | `customer_id` (ULID), `cif_lookup_hash` (HMAC — lookup without decrypt), `cif_enc`, `pii_enc`, `encryption_key_id`, `snapshot_taken_at`, `source_system` | Never plaintext CIF or PAN |
| `customer_snapshot` | `snapshot_id`, `customer_id`, `journey_id`, `lead_id`, `lob`, `pii_enc`, `snapshot_taken_at` | One frozen copy per journey; evidence of what was shown |

Freshness: S-05 allows a cached snapshot **inside a freshness window** when CBS is down; never an
unbounded stale identity.

## 4. Configuration

| Property | Env var | Default |
|---|---|---|
| `customer.apigee.token-mode` | `CUSTOMER_APIGEE_TOKEN_MODE` | `stub` |
| `customer.apigee.token-url` / `client-id` / `client-secret` | `APIGEE_TOKEN_URL` / `APIGEE_CLIENT_ID` / `APIGEE_CLIENT_SECRET` | — |
| `customer.apigee.connect-timeout-ms` / `read-timeout-ms` | | 3000 / 30000 |
| `customer.cbs.inquiry-mode` | `CUSTOMER_CBS_INQUIRY_MODE` | `stub` |
| `customer.cbs.base-url` / `search-path` / `get-path` | `CBS_INQUIRY_BASE_URL` / `CBS_INQUIRY_SEARCH_PATH` / `CBS_INQUIRY_GET_PATH` | — / `/customers/search` / `/customers/{customerId}` |

Stub behaviour (local): any `q` returns one ETB hit `CUST-<last4>`; `q = NONE` or `0000000000`
returns empty; `get` succeeds only for ids starting `CUST-`.

The Apigee token is **one per process** (`bank-common-apigee`), shared with nothing else in this
JVM. The plan to share one AU-Bima-Platform token across `customer-service` and
`1sb-integration-service` through Valkey is parked (`SUG-20261006-atk` remainder, `ASM-020`).

## 5. Sources

- `services/customer-service/src/main/java/com/bank/platform/customer/**` · `src/main/resources/application.yml`
- `libs/bank-common-apigee/src/main/java/com/bank/common/apigee/ApigeeAccessTokenHolder.java`
- [Customer search contract](../../ws3-platform/09-nip-bff-customer-search-contract.md) · [`03-customer.sql`](../../data-architecture/schemas/03-customer.sql) · [External call catalogue §4](../../../journey-execution/05-EXTERNAL-CALL-CATALOGUE.md)
