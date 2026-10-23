# direct-insurer-adapter-service — build brief

**Status:** AI-DRAFTED 2026-10-22 under `SUG-20261022-rdy` (owner-directed), awaiting Mahesh (architecture) review. **DEFERRED — out of R0.**
Snapshot of code at `d1f3dd7`; where this brief and the contract differ, the [contract](./openapi.yaml) wins (ADR-027), then the [LLD](./LLD.md).

[Playbook](../BUILD-PLAYBOOK.md) · [Contracts index](../README.md) · [OpenAPI (port shape)](./openapi.yaml) · [LLD](./LLD.md) ·
[Integration Hub LLD](../integration-hub-service/LLD.md)

## 1. Status today

**Skeleton** (6 main / 2 test classes). 0 / 6 port operations. No insurer is chosen and no insurer
API exists (DIA-1, DIA-2). Maturity ≈ 2 %.

## 2. Scope for this build — port conformance only

This service **cannot reach 90–95 %** without an insurer. What can be built without inventing
anything is the **shared adapter port** so the Hub's routing design (`S07-VT-08`) can be proven:

1. The six port operations with the same canonical shapes and job semantics as the 1SB adapter
   (async jobs, `PARTIAL` = success, **no automatic retry on submit**, `BankApplicationStatus`
   normalisation), backed by an `InsurerPort` with an in-memory **fake insurer** used only in tests.
2. Jobs and encrypted raw payloads through bank-persistence (`external_provider` distinguishes it).
3. `distributorId` from secrets, never a request field; insurer types confined to `adapter.<insurer>.*`.
4. A Hub routing test: a test-resource `HUB.ROUTING` version with a lower-priority rule
   `insurerCode = <X> → DIRECT_<X>` sends that insurer's requests here with no Hub caller change.

No insurer-specific mapping, no wire format, no production profile.

## 3. Operations

[openapi.yaml](./openapi.yaml): `createQuote`, `getQuote`, `submitProposal`, `getProposal`,
`getApplicationStatus`, `findJobByIdempotencyKey`.

## 4. Data owned

None (bank-persistence holds jobs and payloads).

## 5. Rule sets

None here; selected by `HUB.ROUTING`.

## 6. Events

None.

## 7. Synchronous dependencies

| Dependency | Budget | Until it exists |
|---|---|---|
| Insurer API | submit ≤ 20 s inside the Hub's 25 s | Fake insurer (tests only) |
| bank-persistence | as the 1SB adapter | Built |

## 8. Invariants

`INV-DIS-01` · `INV-ACL-01` · submits never auto-retried · raw payloads encrypted, 7 years.

## 9. Acceptance tests

| # | Test | Traces to |
|---|---|---|
| AT-DIA-01 | Port conformance: same canonical request accepted by this adapter and the 1SB adapter, same job states | LLD "The port it implements" |
| AT-DIA-02 | Submit timeout → job stays running; no resend | Hub LLD §8 |
| AT-DIA-03 | `findJobByIdempotencyKey` returns the job created with that key | Hub LLD O-2 |
| AT-DIA-04 | Hub routes an insurer to `DIRECT_<X>` by configuration only | `S07-VT-08` |
| AT-DIA-05 | Body `distributorId` → `422 ATTRIBUTION_NOT_CALLER_SUPPLIED` | `INV-DIS-01` |

## 10. Build order

**Deferred (R1).** Startable now only for the port-conformance scope; lowest priority.

## 11. Open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| DIA-1 | Admission: which insurer, when (evidence of commitment) | Mahesh / Rajal | Everything beyond the port |
| DIA-2 | Insurer API, Apigee exposure, credentials | Bank / insurer | Real adapter |
