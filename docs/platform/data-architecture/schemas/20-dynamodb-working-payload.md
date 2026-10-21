# 20 — DynamoDB working-payload tables (quotation, proposal)

Design artefact for [`DB-DEC-0005`](../DB-DEC-0005-dynamodb-working-payload.md). Provisioned by
Terraform at S09 — not by an application at start-up. Region `ap-south-1` (DR replica, if
chosen, `ap-south-2`). One table per owning service per environment.

## 1. Table `{env}-quotation-working` (owner: `quotation-service`)

| Setting | Value |
|---|---|
| Key schema | `PK` (S, partition) · `SK` (S, sort) |
| Billing | On-demand |
| Encryption | SSE-KMS with customer-managed key `alias/{env}-quotation-working` + client-side envelope encryption of `payloadEnc` |
| PITR | Enabled (35 days) |
| TTL attribute | `ttl` (epoch seconds) |
| Streams | `NEW_AND_OLD_IMAGES` → outbox publisher (owned by quotation-service) |
| GSI1 | `GSI1PK` = `JOURNEY#{journeyId}` · `GSI1SK` = `QUOTE#{createdAt}#{quoteId}` — quotes of a journey (projection KEYS_ONLY + control attributes) |
| Deletion protection | On |

### 1.1 Item types

| Item | `PK` | `SK` | Purpose |
|---|---|---|---|
| Working quote | `QUOTE#{quoteId}` | `META` | The quote while the sale is open |
| Outbox event | `QUOTE#{quoteId}` | `OUTBOX#{eventId}` | Written in the same transaction; published from the stream; `ttl` = publish + 7 days |
| Idempotency | `IDEM#{idempotencyKey}` | `IDEM` | `requestHash`, `responseRef`, `ttl` = +24 h |
| Open-quote marker | `JOURNEY#{journeyId}` | `OPEN_QUOTE` | `quoteId`; enforces one open quote per journey (`attribute_not_exists` on create, deleted on terminal state) |

### 1.2 Working quote attributes

| Attribute | Type | Plaintext? | Why it is outside the envelope |
|---|---|---|---|
| `quoteId`, `leadId`, `journeyId`, `customerId` | S | yes | Ids / keys |
| `suitabilityId`, `consentId` | S | yes | C1 / C2 references, checked before first write |
| `lob`, `productClass`, `mode`, `category` | S | yes | Validation, routing |
| `state` | S | yes | `REQUESTED` · `IN_PROGRESS` · `QUOTED` · `PARTIALLY_QUOTED` · `SELECTED` · `CONVERTED` · `EXPIRED` · `FAILED` · `TIMED_OUT` · `REJECTED` |
| `validUntil` | S (ISO-8601) | yes | `INV-QUO-04` |
| `provider`, `providerJobId` | S | yes | Correlation to `bank_persistence.integration_job` |
| `selectedOfferId`, `selectedPremiumAmount`, `selectedTaxAmount`, `selectedSumAssured`, `selectedInsurerCode`, `selectedProductCode` | S / N (exact decimal) | yes | Payment reads these through the Quotation API (`INV-PAY-03`); written atomically with the envelope (`INV-QUO-05`) |
| `paymentInFlight` | BOOL | yes | Set when Payment fetches the selected premium; `ttl` removed while true |
| `payloadEnc` | B | **no** — ciphertext | Request, members, all offers, per-insurer errors |
| `encryptionKeyId`, `payloadSchemaVersion` | S / N | yes | Rotation, envelope shape |
| `configVersionRef` | S | yes | `INV-CFG-03` |
| `materializedAt` | S | yes | Set once after Aurora commit; every later update is refused |
| `version` | N | yes | Optimistic lock |
| `createdAt`, `updatedAt` | S | yes | |
| `ttl` | N | yes | `RET-WORKING`: last update + 90 days (configuration); removed while `paymentInFlight`; materialized + 30 days after materialize |

## 2. Table `{env}-proposal-working` (owner: `proposal-service`)

Same settings (own KMS key, PITR, TTL, stream, GSI1 by journey).

| Item | `PK` | `SK` | Purpose |
|---|---|---|---|
| Working proposal | `PROPOSAL#{proposalId}` | `META` | Form answers, nominee, UW requirements, counter-offer (in `payloadEnc`) |
| Outbox event | `PROPOSAL#{proposalId}` | `OUTBOX#{eventId}` | Same pattern |
| Idempotency | `IDEM#{key}` | `IDEM` | Same pattern |
| Open-proposal marker | `JOURNEY#{journeyId}` | `OPEN_PROPOSAL` | One open proposal per journey |
| Application-number marker | `APPNO#{applicationNumber}` | `APPNO` | Uniqueness of the insurer application number |

Plaintext control attributes: `proposalId`, `leadId`, `journeyId`, `customerId`, `quoteId`,
`offerId`, `consentId`, `lob`, `applicationNumber`, `schemaId`, `schemaVersion`, `state`,
`underwritingState`, `issuanceMode`, `paymentInFlight`, `materializedAt`, `version`, `ttl`,
timestamps. **Nominee and every form value live only inside `payloadEnc`** (`INV-PRP-05`).
Documents go to S3; only object references enter the envelope. Envelope budget 350 KB.

## 3. Write patterns

### 3.1 Create quote (one `TransactWriteItems`)

```text
Put    IDEM#{key}/IDEM                 condition attribute_not_exists(PK)
Put    JOURNEY#{journeyId}/OPEN_QUOTE  condition attribute_not_exists(PK)
Put    QUOTE#{quoteId}/META            condition attribute_not_exists(PK)   state=REQUESTED, version=1, ttl
Put    QUOTE#{quoteId}/OUTBOX#{evt}    QuoteRequested
```

Idempotent replay: the `IDEM` condition fails → read `IDEM` → same `requestHash` returns the
stored `quoteId`; different hash → `409 IDEMPOTENCY_CONFLICT`.

### 3.2 Update (poll result, selection, form save)

```text
Update QUOTE#{quoteId}/META
  SET ..., payloadEnc = :enc, version = version + 1, updatedAt = :now, ttl = :ttl
  CONDITION version = :expected AND attribute_not_exists(materializedAt)
(+ Put OUTBOX item in the same transaction when an event is due)
```

Selection additionally requires `state IN (QUOTED, PARTIALLY_QUOTED)` and `validUntil > :now`.

### 3.3 Payment in flight

`GET /internal/v1/quotes/{quoteId}/selected` (called by Payment) performs
`SET paymentInFlight = true REMOVE ttl` with condition `state = SELECTED`. A `PaymentDeclined` /
`PaymentExpired` event sets `paymentInFlight = false` and restores `ttl`.

### 3.4 Materialize on `PaymentReconciled` (idempotent consumer)

```text
1  GetItem QUOTE#{quoteId}/META (ConsistentRead)
2  If materializedAt is set → ensure quotation.quote exists → ack event
3  Decrypt payloadEnc in process
4  Aurora (app_quotation), ONE transaction:
     INSERT INTO quotation.quote (...)  ON CONFLICT (quote_id) DO NOTHING
     INSERT INTO quotation.offer (...)  ON CONFLICT (offer_id) DO NOTHING   -- selected = SELECTED, others NOT_SELECTED
     INSERT INTO quotation.idempotency_record (eventId) ON CONFLICT DO NOTHING
   COMMIT                                         ← Aurora is now the record
5  UpdateItem META SET materializedAt = :now, state = CONVERTED, ttl = :now + 30d
     CONDITION attribute_not_exists(materializedAt)
6  Delete JOURNEY#{journeyId}/OPEN_QUOTE
```

Proposal is identical into `proposal.proposal`, `uw_requirement`, `uw_document_ref`.

**Repair sweep (daily):** for every `payment.payment` in `RECONCILED` (via Payment API / event
replay) whose `quotation.quote` or `proposal.proposal` row is missing, re-drive step 1–6 and alert
if the DynamoDB item is gone.

## 4. IAM (per service, via IRSA)

```json
{
  "Effect": "Allow",
  "Action": ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem", "dynamodb:DeleteItem",
             "dynamodb:Query", "dynamodb:ConditionCheckItem", "dynamodb:TransactWriteItems",
             "dynamodb:DescribeStream", "dynamodb:GetRecords", "dynamodb:GetShardIterator"],
  "Resource": ["arn:aws:dynamodb:ap-south-1:<acct>:table/{env}-quotation-working",
               "arn:aws:dynamodb:ap-south-1:<acct>:table/{env}-quotation-working/index/*",
               "arn:aws:dynamodb:ap-south-1:<acct>:table/{env}-quotation-working/stream/*"]
}
```

No `Scan`, no `dynamodb:*`, no other table. `kms:Decrypt` / `kms:GenerateDataKey` only on the
service's own keys. Journey, Payment and every other service: **no DynamoDB permission**.

## 5. Local development

DynamoDB Local (or LocalStack) container in compose; tables created by a test fixture mirroring
the Terraform definition. Integration tests use Testcontainers. Endpoint override via
`AWS_DYNAMODB_ENDPOINT` only in `local`/`test` profiles.

## 6. As built (`SUG-20261021-lft`) — deviations to resolve

Code: `services/quotation-service`, `services/proposal-service` (`adapter.dynamodb`, `adapter.crypto`,
`application.*Materializer`). Tested against embedded DynamoDB Local 2.6.1.

| # | Deviation | Why | Decision needed |
|---|---|---|---|
| 1 | Materialise uses `ON CONFLICT DO NOTHING` without a conflict target | H2 (PostgreSQL mode) rejects the targeted form | Also swallows a `ux_offer_selected` violation on Postgres; the domain prevents two selected offers. Aarti to accept or ask for Postgres-only SQL |
| 2 | Partial unique indexes (`ux_offer_selected`, `ux_proposal_application`) live in Postgres-only vendor migrations (`db/vendor/postgresql`); H2 gets an equivalent | Portability | — |
| 3 | `INVALID` offers with non-positive premium / sum assured get no `quotation.offer` row | `ck_offer_positive` in 08 conflicts with "one row per envelope offer" | Aarti / Product: relax the CHECK for `INVALID`, or accept |
| 4 | `proposal.form_values_ref` (NOT NULL) points at the frozen DynamoDB item, which expires 30 days after materialise | No durable evidence store for form values yet | **Open — needs a decision**: S3 evidence object (recommended) or longer frozen-item TTL |
| 5 | Proposal materialises to `PAID`, not `CONVERTED` | `proposal-service.md` §3.5 | Align §3.4 wording |
| 6 | The encrypted data key is packed inside `payloadEnc`; AAD binds ciphertext to the item key; outbox items `ttl` = write + 7 days | Simpler item shape | — |
| 7 | Not built: stream outbox publisher, GSI1 queries, daily repair sweep, a Payment read for proposals, `paymentEnded` endpoint | No MSK / Payment code yet | S11 |
