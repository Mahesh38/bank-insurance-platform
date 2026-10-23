# customer-service — Low-level design (contract companion)

**Status:** AI-DRAFTED 2026-10-22 from owner decision D3 (`D-023`), ADR-027 and
[DECISIONS-2026-10-22](../../architecture-review/DECISIONS-2026-10-22.md) (E7 `CUSTOMER.POLICY`; engineering
decision `ED-R2-01`, nested budgets), awaiting Mahesh
(architecture) and Aarti (data) review. The wire contract is [`openapi.yaml`](./openapi.yaml).

Sources: [DECISIONS-2026-10-21 §D3](../../architecture-review/DECISIONS-2026-10-21.md) ·
[ADR-025](../../architecture-review/ADR-025-event-specification-and-publisher.md) ·
[ADR-027](../../architecture-review/ADR-027-openapi-contract-authority.md) ·
[customer search contract 09](../../ws3-platform/09-nip-bff-customer-search-contract.md) ·
[DDL `03-customer.sql`](../../data-architecture/schemas/03-customer.sql) ·
[API reference page](../../api-reference/services/customer-service.md) ·
[seam budgets](../../api-reference/conventions/idempotency-timeouts-retries.md) ·
code `services/customer-service/src/main/java/com/bank/platform/customer/**`.

## 1. Purpose and ownership

customer-service (#4, layer L5, port 8090, INTERNAL) is the platform's only window onto the bank's
customer master (CBS, via Apigee). It **never writes CBS**. It owns:

| Owns | Meaning |
|---|---|
| `customerId` | Platform ULID minted on first resolve; the only customer reference used by any other context (D3) |
| CIF | Held only here — `cif_enc` + `cif_lookup_hash` (HMAC) — and sent only to CBS / 1SB adapters (D3) |
| Masking | `displayName`, `initials`, `maskedCif`, `maskedMobile`, `maskedEmail` are produced **here** (§6). The BFF passes them through; contract 09 §3.4 placed masking at the BFF — superseded by this LLD |
| Snapshot | Journey-scoped, write-once copy of what CBS said (`customer_snapshot`), with a configured freshness window |
| Search audit | `CustomerSearched` evidence event (contract 09 §6.3) |

## 2. API summary and code status

| Operation | Path | Code today | Target |
|---|---|---|---|
| `searchCustomers` | `GET /internal/v1/customers/search` | IMPLEMENTED-PARTIAL: `by ∈ {CUSTOMER_ID, MOBILE, PAN}`, non-blank `q`, CBS call, hits passed through from CBS | Per-`by` format checks; `by=CIF` (alias `CUSTOMER_ID`); resolve each ETB hit to a ULID; mask here; audit event; `X-Actor-Id` required |
| `resolveCustomer` | `POST /internal/v1/customers:resolve` | absent | CIF → `customerId`, idempotent |
| `getCustomer` | `GET /internal/v1/customers/{customerId}` | IMPLEMENTED-PARTIAL: path value is a CBS id, CBS called | Path value is the ULID; local store; no CBS call |
| `takeSnapshot` | `POST /internal/v1/customers/{customerId}/snapshots` | absent | S-04 from Journey |
| `getSnapshot` | `GET /internal/v1/customers/{customerId}/snapshots/{snapshotId}` | absent | Masked identity + non-PII derived inputs |
| `getRegisteredContact` | `GET /internal/v1/customers/{customerId}/registered-contact` | absent | OTP target for consent / suitability (CNS-R11) |

**Deviations of the code from this contract** (to fix when the target is built):

1. `customer.apigee.read-timeout-ms` defaults to **30 000 ms**; S-05 is **3 s**. No retry exists on
   5xx / connect failure; a connect failure (`ResourceAccessException`) is not translated and
   surfaces as `500 INTERNAL_ERROR` instead of `503 UPSTREAM_UNAVAILABLE`.
2. `X-Actor-Id` is not read; no search audit is written.
3. The stub returns `CUST-<last4>` ids and `maskedMobile = XXXXXX<last4>`; neither matches §6.
4. CBS JSON is passed through as `CustomerHit` (no anti-corruption mapping, no masking).
5. The `customer` / `customer_snapshot` tables are not migrated (V1 is a placeholder).

The older api-reference shape `GET /internal/v1/customers:resolve?by&q` (local only) and
`GET …:lookup` are replaced by `POST …:resolve` (CIF in the body, so it never sits in an access log)
and by `search` (which already performs the CBS lookup).

## 3. Operation flows

### 3.1 Search

1. Validate `by` and `q` (§5). `q` is normalised (trim, PAN upper-cased, mobile digits-only) and is
   never logged.
2. CBS inquiry through Apigee (§7: **one** attempt of 3 s — no retry for interactive search, so it
   fits the BFF's 4 s read (`ED-R2-01`); 401 → re-mint token once).
3. Anti-corruption mapping: each CBS record → domain `CbsCustomer` (cif, name, mobile, email, dob,
   eligibility). Unknown CBS fields are dropped here.
4. For each `ETB` record: `resolve(cif)` (§3.2 steps 2–4, without a second CBS call).
5. Mask (§6) → `CustomerHit`.
6. One transaction: any new `customer` rows + outbox `CustomerSearched {actorId, by, qHash,
   resultCount, eligibilitySeen[], correlationId}` (ADR-026 audit transport is PROPOSED; the event
   is written to the outbox either way).

```mermaid
sequenceDiagram
    participant BFF
    participant CS as customer-service
    participant DB as schema customer
    participant AX as Apigee → CBS
    BFF->>CS: GET /customers/search?by=PAN&q=… (X-Actor-Id)
    CS->>CS: validate format per by (422 VALIDATION_ERROR field q)
    CS->>AX: inquiry (3 s; 1 retry on 5xx/connect)
    AX-->>CS: CBS records
    CS->>DB: SELECT customer WHERE cif_lookup_hash = HMAC(cif) (per ETB hit)
    CS->>DB: BEGIN; INSERT missing customer rows; INSERT outbox CustomerSearched; COMMIT
    CS-->>BFF: 200 {items: masked hits with customerId}
```

### 3.2 Resolve (CIF → customerId)

1. Validate CIF format.
2. `h = HMAC-SHA256(lookupKey, cif)`; `SELECT customer_id FROM customer.customer WHERE cif_lookup_hash = h`.
3. Found → `200 {customerId, created:false}` (no CBS call).
4. Not found → CBS inquiry by CIF. No record / `NOT_ETB` → `404 RESOURCE_NOT_FOUND`. Else mint ULID,
   encrypt CIF and PII bundle, `INSERT customer` + outbox `CustomerResolved {customerId}` +
   `idempotency_record` in one transaction → `201`.
5. Unique violation on `ux_customer_cif_hash` (concurrent first resolve) → re-read and return the
   winner's id with `200`. This is what makes resolve idempotent independently of the key.

### 3.3 Snapshot (S-04)

1. Replay check: a snapshot with this `journeyId` exists → `200` with it.
2. Resolve rule set **`CUSTOMER.POLICY`** (E7; seed `snapshotFreshnessMinutes = 1440`, 24 h, the
   engineering default of 2026-10-22, awaiting Rajal) through `libs/bank-rule-engine` (S-21, 300 ms,
   cached to TTL). Outcome `RESOLVED` → window = derived `snapshotFreshnessMinutes`;
   `CONFIG_MISSING` (unset or non-positive) or unresolvable → fail closed
   `422 CONFIGURATION_UNRESOLVABLE`; never a compiled-in value.
3. CBS inquiry by the decrypted CIF (§7).
4. Success → `INSERT customer_snapshot` (`cbs_as_of = now`, `fresh_until = now + window`,
   `payload_hash`, `servedFrom = CBS`), refresh `customer.pii_enc` / `snapshot_taken_at` if
   `payload_hash` changed, outbox `CustomerSnapshotTaken {customerId, snapshotId, journeyId}`.
5. CBS failed → newest snapshot of this customer that is still fresh (`fresh_until > now()`, which
   equals `CUSTOMER.POLICY` outcome `FRESH` for `snapshotAgeMinutes = now − cbs_as_of`) → copy into a new row
   for this journey keeping the original `cbs_as_of` / `fresh_until` (`servedFrom = FRESH_COPY`).
   None → `503 UPSTREAM_UNAVAILABLE`. Journey then holds at `INITIATED` (see journey LLD §3.1).
6. `derived` values (`ageNextBirthday`, `relationshipTenureMonths`, `knownLoanLiabilityInr`) are
   computed from the decrypted bundle **as of `cbs_as_of`**; DOB and loan detail never leave.

### 3.4 Registered contact

Decrypt `pii_enc.mobile` of the latest snapshot / customer row; caller must be `consent` or
`suitability` by mesh identity (until mTLS is live this is an allow-list on a service header and
an open security item, O-4).

## 4. Domain and ports

| Element | Package | Notes |
|---|---|---|
| `Customer` (aggregate: id, cifHash, encrypted CIF, encrypted profile) | `domain` | `resolve` is a factory, idempotent by `cifHash` |
| `CustomerSnapshot` (value, write-once) | `domain` | `isFresh(now)` |
| `Masking` (pure functions §6) | `domain` | Unit-tested against contract 09 examples |
| `CustomerInquiryPort` (exists) | `domain` | Adapter `HttpCustomerDirectory` / stub |
| `CustomerRepository`, `SnapshotRepository`, `OutboxPort`, `CryptoPort` (HMAC + envelope encryption), `ConfigurationPort` | `domain` / `application` | New |

## 5. Validation rules

| Field | Rule | Error code |
|---|---|---|
| `by` | `CIF` \| `MOBILE` \| `PAN` \| `CUSTOMER_ID` (deprecated alias of `CIF`) | `422 INVALID_REQUEST` (code today) |
| `q` (any) | present, 1–80 chars after trim | `422 MISSING_REQUIRED_FIELD` |
| `q` when `by=CIF` | `^[A-Za-z0-9]{1,20}$` | `422 VALIDATION_ERROR`, field `q` |
| `q` when `by=PAN` | upper-cased `^[A-Z]{5}[0-9]{4}[A-Z]$` | `422 VALIDATION_ERROR`, field `q` |
| `q` when `by=MOBILE` | digits-only 10 starting `6–9`, or `+91` + that | `422 VALIDATION_ERROR`, field `q` |
| `cif` (resolve) | `^[A-Za-z0-9]{1,20}$` | `422 VALIDATION_ERROR` |
| `customerId`, `journeyId`, `leadId` | ULID `^[0-9A-HJKMNP-TV-Z]{26}$` | `422 VALIDATION_ERROR` |
| `lob` | `LIFE` \| `HEALTH` \| `GENERAL` | `422 VALIDATION_ERROR` |
| `X-Actor-Id` | present | `422 MISSING_REQUIRED_FIELD` |
| `Idempotency-Key` on POST | present | `400 MISSING_IDEMPOTENCY_KEY` |

Contract 09 states `400 VALIDATION_ERROR`; the error catalogue registers `VALIDATION_ERROR` as
**422**. The catalogue wins (error-model §1).

## 6. Masking formats (owned here)

| Output | Rule | Example |
|---|---|---|
| `initials` | `[A-Za-z]` only; first letter of first and last token; one token → first two letters; empty → `?` | `Abhishek Kummar` → `AK` |
| `maskedCif` | Keep last 4; every other character → `X`; preserve length | `5600980433` → `XXXXXX0433` |
| `maskedMobile` | `+91` + first 3 of national number + `****` + last 3 | `9331111412` → `+91 933****412` |
| `maskedEmail` | First 3 of local part + `*****@` + domain; local part ≤ 3 → `***@domain` | `abhishek@gmail.com` → `abh*****@gmail.com` |
| PAN, DOB, address, income | **Never returned** by any endpoint | — |

## 7. Timeouts, retries, failure modes

| Hop | Budget | Retry | Failure → |
|---|---|---|---|
| Apigee token | connect 3 s / read 3 s | single-flight re-mint on CBS 401, once | second 401 → `503 UPSTREAM_UNAVAILABLE` |
| CBS inquiry (S-05) | **3 s per attempt** (connect ≤ 1 s inside it) | snapshot and resolve: **1** retry on 5xx or connect/read failure, 200 ms jitter (worst 6.2 s); **search: none** | `503 UPSTREAM_UNAVAILABLE`; other 4xx → `502 UPSTREAM_BAD_RESPONSE`; 404 → empty search / `404` |
| Inbound from BFF — search (4 s read) | one 3 s attempt fits | — | — |
| Inbound from Journey — snapshot (S-04: **7 s**, no retry at journey, inside an async journey command) | fits: 6.2 s worst case < 7 s (`ED-R2-01`, [conventions §2.2](../../api-reference/conventions/idempotency-timeouts-retries.md#22-nested-budgets--engineering-decision-ed-r2-01-2026-10-22)) | — | Journey holds at `INITIATED` |

Configuration keys (target): `customer.cbs.connect-timeout-ms=1000`, `customer.cbs.attempt-timeout-ms=3000`,
`customer.cbs.retries=1`, `customer.cbs.search.retry-enabled=false`; breaker per CBS dependency
(resilience table, conventions §3). Technical timeouts are Spring properties; the freshness window is
business configuration (`CUSTOMER.POLICY`).

## 8. DTO → domain → table mapping

| DTO field | Domain | Table.column |
|---|---|---|
| `customerId` | `Customer.id` | `customer.customer_id` (ULID CHECK) |
| `cif` (resolve), CBS `cif` | `Customer.cifHash`, `Customer.cifCipher` | `customer.cif_lookup_hash` (+ `lookup_key_id`), `customer.cif_enc` |
| `displayName`, mobile, email, DOB, address, PAN | `Customer.profile` (encrypted bundle) | `customer.pii_enc`, `encryption_key_id` |
| `maskedCif`, `maskedMobile`, `maskedEmail`, `initials` | computed by `Masking` | not stored |
| `eligibility` | `CbsCustomer.eligibility` | not stored (re-read from CBS) |
| `snapshotTakenAt` | `Customer.snapshotTakenAt` | `customer.snapshot_taken_at` |
| `snapshotId`, `journeyId`, `leadId`, `lob` | `CustomerSnapshot` | `customer_snapshot.snapshot_id`, `journey_id`, `lead_id`, `lob` |
| `cbsAsOf`, `freshUntil` | `CustomerSnapshot.freshness` | `customer_snapshot.cbs_as_of`, `fresh_until` |
| `configVersionRef` | resolved window version | `customer_snapshot.config_version_ref` |
| (internal) | change detection | `customer_snapshot.payload_hash` |
| `derived.*` | computed | not stored |
| Idempotency | — | `customer.idempotency_record` |
| Events | — | `customer.outbox_event` |

**DDL changes this contract needs in `03-customer.sql`** (being applied in parallel by the DDL
owner; listed here so the two can be checked against each other): `lookup_key_id`,
`ck_customer_id_ulid`, nullable `customer_snapshot.journey_id` with partial unique
`ux_snapshot_journey`, `cbs_as_of`, `fresh_until`, `payload_hash`, `config_version_ref`,
`retain_until`, `customer.idempotency_record`, `customer.outbox_event` (ADR-025 columns incl.
`parked_at`). Plus grants for `app_customer` (readiness review §3.5 — missing in `91-grants.sql`).

## 9. Invariants

| Invariant | Enforced at |
|---|---|
| One `customerId` per CIF (D3) | `ux_customer_cif_hash` + resolve race handling (§3.2 step 5) |
| No plaintext CIF / PAN at rest | Domain only holds ciphertext; `CryptoPort` |
| `q`, PAN, mobile, CIF never logged or in an error `reason` (SEC-C2) | Code review + log-scan test |
| Snapshot write-once; never an unbounded stale identity (S-05) | INSERT-only repository; `fresh_until` check |
| Empty CBS answer never invents a customer (`AC-CUST-010-2`) | Search maps only CBS records |

## 10. Events produced (ADR-025 envelope; `data` carries ids only)

| Event | When | `data` |
|---|---|---|
| `CustomerSearched` | every search (including empty) | `actorId`, `by`, `qHash`, `resultCount`, `eligibilitySeen[]` |
| `CustomerResolved` | first resolve of a CIF | `customerId` |
| `CustomerSnapshotTaken` | snapshot written | `customerId`, `snapshotId`, `journeyId`, `servedFrom` |

Topic `{env}.customer.customer.v1`, key `customerId` (search events keyed by `actorId`). Consumes nothing.

## 11. Open items

| # | Item | Owner |
|---|---|---|
| O-1 | CBS inquiry contract (bank OpenAPI) — the adapter shape is ours until published (`DEP-20260914-apg`) | Bank / Aarti |
| O-2 | ~~S-04 (2 s) < S-05 (3 s + 1 retry)~~ — **resolved by `ED-R2-01`**: S-04 7 s inside an async journey command, no journey retry; search makes one CBS attempt | Mahesh (confirm) |
| O-3 | Freshness window — **seeded** in `CUSTOMER.POLICY` as 1440 minutes (engineering default, E7); Rajal confirms, Compliance may shorten it | Rajal + Shailja |
| O-4 | Caller authentication for `registered-contact` before mesh mTLS (ADR-023) | Deepali |
| O-5 | Whether `NOT_ETB` hits may be resolved (R0 is ETB-only) | Rajal |
| O-6 | Prefill read (PAN/DOB/address after `CNS-DP`, `SCR-07`) — not in this contract | Mahesh + Rajal |
