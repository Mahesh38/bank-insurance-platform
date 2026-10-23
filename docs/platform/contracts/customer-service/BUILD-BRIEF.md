# customer-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Mahesh (architecture) and Aarti (data) review.
Snapshot of code at `d1f3dd7` with round-2 work in flight; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI](./openapi.yaml) · [LLD](./LLD.md) ·
[Service page](../../api-reference/services/customer-service.md) · [Scenario 04](../../api-reference/scenarios/04-customer-search.md) ·
[Round-2 decisions](../../architecture-review/DECISIONS-2026-10-22.md)

## 1. Status today

| Area | State |
|---|---|
| Code | **Partial** (20 main / 7 test classes): CBS inquiry through Apigee (HTTP + stub), search and get by CBS id; timeouts now 3 s / 3 s by env |
| Contract operations | `searchCustomers`, `getCustomer` partial; `resolveCustomer`, `takeSnapshot`, `getSnapshot`, `getRegisteredContact` absent |
| Persistence | `V1__init_schema` placeholder only — `customer`, `customer_snapshot`, idempotency, outbox not migrated |
| Deviations (LLD §2) | No anti-corruption mapping or masking; no ULID resolve; no `X-Actor-Id` / search audit; stub ids `CUST-<last4>` |

Maturity estimate: ≈ 20 %.

## 2. Scope for this build

Everything in the contract: ULID identity (D3), masking (owned here), snapshot with freshness
from **`CUSTOMER.POLICY.snapshotFreshnessMinutes`** (E7; engineering default 24 h = 1 440 min —
seed pending, CUS-3), registered-contact read for consent/suitability/notification, search audit,
encryption + HMAC lookup, outbox.

## 3. Operations

| operationId | Today | Notes |
|---|---|---|
| `searchCustomers` | partial | Per-`by` format checks, resolve ETB hits to ULIDs, mask here, `CustomerSearched` |
| `resolveCustomer` | absent | CIF in body → `customerId`; idempotent by `cif_lookup_hash` |
| `getCustomer` | partial | Path value becomes the ULID; local store; no CBS call |
| `takeSnapshot` | absent | S-04 from journey; fresh-copy fallback |
| `getSnapshot` | absent | Masked identity + non-PII derived inputs |
| `getRegisteredContact` | absent | Caller allow-list (CUS-4) |

## 4. Data owned

Schema `customer` — design [`03-customer.sql`](../../data-architecture/schemas/03-customer.sql):
`customer` (`customer_id` ULID, `cif_enc`, `cif_lookup_hash` UNIQUE, `pii_enc`), `customer_snapshot`
(write-once, `cbs_as_of`, `fresh_until`, `payload_hash`, `config_version_ref`), `idempotency_record`,
`outbox_event`. Grants for `app_customer` (LLD §8). Postgres-only: partial unique
`ux_snapshot_journey`.

## 5. Rule sets and configuration

| Value | Source |
|---|---|
| Snapshot freshness window | `CUSTOMER.POLICY.snapshotFreshnessMinutes` (E7). The LLD still names `CUSTOMER.SNAPSHOT.freshnessMinutes`; round 2 names `CUSTOMER.POLICY` — use the round-2 id (CUS-3) |
| CBS / Apigee timeouts, retries | Spring properties (`customer.cbs.*`, LLD §7) |

Unresolvable → `CONFIGURATION_UNRESOLVABLE`, no default.

## 6. Events

Produced: `CustomerSearched`, `CustomerResolved`, `CustomerSnapshotTaken` (LLD §10, topic
`{env}.customer.customer.v1`) — **not yet in the [catalogue](../events/CATALOGUE.md)**: request
schemas (CUS-5); `AuditRecorded` for search (a read that matters, ADR-026). Consumed:
`RuleSetPublished`.

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists |
|---|---|---|
| CBS inquiry via Apigee (S-05) | connect 1 s / read 3 s, 1 retry on 5xx/connect | Existing `StubCustomerDirectory` (refused in prod); bank CBS OpenAPI unpublished (CUS-1) |
| Apigee token | 3 s / 3 s, single-flight re-mint on 401 | Existing stub token client |
| administration-config resolve | 300 ms, cached | `RuleSetSeeds`/test rule set until `CUSTOMER.POLICY` seed exists |

## 8. Invariants

One `customerId` per CIF (D3, `ux_customer_cif_hash` + race re-read) · no plaintext CIF/PAN at rest ·
`q`, PAN, mobile, CIF never logged · snapshot write-once, never an unbounded stale identity ·
empty CBS answer never invents a customer (`AC-CUST-010-2`) · PAN / DOB / address / income never
returned.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-CUS-01 | Resolve the same CIF twice (and concurrently) → same `customerId`; second is `200 created:false` with no CBS call | D3, LLD §3.2 |
| AT-CUS-02 | Resolve a CIF CBS does not know / `NOT_ETB` → `404 RESOURCE_NOT_FOUND`, nothing stored | LLD §3.2 step 4 |
| AT-CUS-03 | `by=PAN` with `abcde1234f` normalises and passes; `ABCD1234F` → `422 VALIDATION_ERROR` field `q` | LLD §5 |
| AT-CUS-04 | Masking examples of LLD §6 reproduced exactly (`XXXXXX0433`, `+91 933****412`, `abh*****@gmail.com`, `AK`) | LLD §6 |
| AT-CUS-05 | Search writes `CustomerSearched` + `AuditRecorded` in one transaction, also for an empty result | LLD §3.1, ADR-026 |
| AT-CUS-06 | CBS slower than 3 s twice → `503 UPSTREAM_UNAVAILABLE`; CBS 4xx → `502 UPSTREAM_BAD_RESPONSE` | LLD §7 |
| AT-CUS-07 | Snapshot: CBS down and a fresh snapshot exists → new row `servedFrom=FRESH_COPY` with original `fresh_until`; none fresh → `503` | LLD §3.3 |
| AT-CUS-08 | Snapshot replay for the same `journeyId` → `200` same snapshot | LLD §3.3 step 1 |
| AT-CUS-09 | `CUSTOMER.POLICY` unresolvable → `CONFIGURATION_UNRESOLVABLE`, no snapshot | E7, ADR-024 |
| AT-CUS-10 | `getRegisteredContact` from a non-allowed caller → `403` | LLD §3.4 |
| AT-CUS-11 | Log capture of search contains no `q` value | `INV-LOG-01` |
| AT-CUS-12 | Migrations on PostgreSQL: unique hash, write-once snapshot (UPDATE refused by grant) | playbook §10 |
| AT-CUS-13 | Conformance test passes | ADR-027 |

## 10. Build order

**Tier 2.** Needs administration-config (stubbable). Consumers: BFF, journey (S-04), consent,
suitability, notification, payment (registered contact). Startable now.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| CUS-1 | Bank CBS inquiry contract (OpenAPI) — the adapter shape is ours until published | Bank / Aarti | Go-live |
| CUS-2 | S-04 (2 s) shorter than S-05 (3 s + retry) — raise S-04 or accept fresh-copy fallback | Mahesh | Nothing at build (fallback exists) |
| CUS-3 | `CUSTOMER.POLICY` seed (24 h default per E7) and its approval; LLD still says `CUSTOMER.SNAPSHOT` | Rule-engine owner; Rajal + Shailja | Snapshot at go-live |
| CUS-4 | Caller authentication for `registered-contact` before mTLS | Deepali | Go-live |
| CUS-5 | Catalogue entries for the three customer events | Events owner | Relay publish |
| CUS-6 | Contact-point read for notification (notification LLD N-3) is not in this contract | Mahesh | Notification sends |
| CUS-7 | Whether `NOT_ETB` hits may be resolved (R0 ETB-only) | Rajal | Nothing (strict: refuse) |
