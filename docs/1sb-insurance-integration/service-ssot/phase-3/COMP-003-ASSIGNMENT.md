# Phase 3 — COMP-003 Assignment (Team Lead)

**Backlog ID:** COMP-003  
**Title:** Raw payload encryption at rest (AES-GCM)  
**Branch:** `cursor/comp-003-raw-payload-encryption-c259`  
**Depends on:** FUNC-009 (stack tip) / persistence Flyway `raw_payload`  

## AC (PRODUCT-BACKLOG + TECH-007)

1. `raw_payload.payload_enc` stores **AES-256-GCM** ciphertext — not plaintext (prove via DB/entity assert)
2. Decryption round-trip returns original bytes
3. `encryption_key_id` persisted (Vault/key version id from config)
4. `retain_until` = `created_at` date + **configurable** retention years (default **7**)
5. Access only via store abstraction / internal HTTP — no plaintext in API list responses by default

## Design (KISS + platform-common persistence)

```text
bank-persistence-service:
  AesGcmPayloadCipher  ← key + keyId from bank.persistence.payload-encryption.*
  RawPayloadStore      ← encrypt → RawPayloadRepository
  RawPayloadController ← POST /internal/v1/raw-payloads
                         GET  /internal/v1/raw-payloads/{payloadId}?decrypt=true (optional)

1sb-integration-service (optional but preferred):
  RawPayloadStorePort → HttpRawPayloadStoreAdapter
  OneSbHttpClient / call sites store REQ+RES when jobId available via OneSbCallContext
```

### Config (test/local = properties stand-in for Vault)

```yaml
bank:
  persistence:
    payload-encryption:
      key-id: local-v1
      # 32-byte key, Base64
      key-base64: ...
      retain-years: 7
```

Fail fast at persistence startup if key missing/invalid length.

### HTTP contract

`POST /internal/v1/raw-payloads`

```json
{
  "jobId": "uuid",
  "direction": "REQ",
  "operation": "POST /insurance/lifeterm/v1/quote",
  "lob": "TERM",
  "payloadBase64": "<utf8 body base64>",
  "httpStatus": null
}
```

Response `201`: `payloadId`, `jobId`, `direction`, `encryptionKeyId`, `retainUntil`, `createdAt` — **never** echo plaintext/ciphertext.

`GET .../{id}?decrypt=true` returns plaintext Base64 for compliance/tests only.

## Tests

- `@Tag("COMP-003")`
- Cipher unit: round-trip, tamper fails, ciphertext ≠ plaintext
- API IT: POST → entity `payloadEnc` does not contain plaintext UTF-8; decrypt GET round-trip; retain_until ≈ today+7y
- Persistence jacoco still green

## Out of scope

- Background key-rotation re-encrypt job
- Nightly retain_until purge (separate backlog)
- Changing Flyway schema (table already exists)

## DoD

TL + QA Lead dual APPROVE; PR with AC table.
