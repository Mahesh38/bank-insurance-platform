# Phase 3 — COMP-003 Review Log

| Step | Role | Outcome |
|------|------|---------|
| Assign | TL | COMP-003-ASSIGNMENT.md |
| Implement | Dev | `6ed3ad2` |
| Review | TL + QA Lead | **APPROVE** |
| Close | TL | **Done** → PR |

**Verdict:** **APPROVE**

**AC acceptance:** AC-1…AC-5 **Accepted**. PRODUCT-BACKLOG AC (AES-GCM at rest + key id + configurable `retain_until`) **proven** on persistence path. Known deviation (OneSbHttpClient auto-capture not wired) is **non-blocking**.

---

## AC checklist

| AC | Verdict | Proof |
|----|---------|-------|
| AC-1 `payload_enc` AES-256-GCM ciphertext — not plaintext | **Accepted** | `AesGcmPayloadCipher` (`AES/GCM/NoPadding`, 12-byte IV \|\| ct+tag); IT `post_storesEncryptedPayload_*` asserts entity `payloadEnc` UTF-8/bytes do not contain secret; cipher unit `encrypt_ciphertextDoesNotContainPlaintext` |
| AC-2 Decryption round-trip returns original bytes | **Accepted** | Cipher unit `encryptDecrypt_roundTrip_*`; IT `get_decryptTrue_roundTripsPlaintextBase64` |
| AC-3 `encryption_key_id` persisted (config / Vault stand-in) | **Accepted** | `RawPayloadStore` sets `encryptionProperties.keyId()`; IT asserts `test-v1` on entity + 201 JSON |
| AC-4 `retain_until` = create date + configurable years (default 7) | **Accepted** | `retain-years` config + default 7; IT `retainUntil` = `LocalDate.now().plusYears(7)`; props unit defaults null → 7 |
| AC-5 Access via store / internal HTTP — no plaintext in default responses | **Accepted** | Sole write path `RawPayloadStore` → repo; `POST`/`GET` without `decrypt` omit `payloadBase64` (`JsonInclude.NON_NULL`); IT asserts doesNotExist |

**PRODUCT-BACKLOG:** `raw_payload` blobs encrypted (AES-GCM); key from config (Vault stand-in); `retain_until` set (default 7y configurable) — **Accepted**. Storage encryption ACs do not require live HTTP-client capture.

---

## Strict gates (TL)

| Gate | Result | Notes |
|------|--------|-------|
| AES-256-GCM + fail-fast key | Pass | 32-byte key enforced in cipher ctor + `PayloadEncryptionProperties` compact ctor |
| Store abstraction | Pass | Controller → `RawPayloadStore` only; repository not exposed for custom payload writes |
| HTTP contract | Pass | `POST /internal/v1/raw-payloads` → 201 metadata; `GET ?decrypt=true` optional plaintext Base64 |
| Config shape | Pass | `bank.persistence.payload-encryption.{key-id,key-base64,retain-years}` in main + test YAML |
| `@Tag("COMP-003")` | Pass | Cipher, props, API IT, adapter, call-context tests |
| Persistence jacoco | Pass | `:services:bank-persistence-service:check` (incl. `jacocoTestCoverageVerification`) green on review re-run |
| Integration capture wiring | **Deferred (non-blocking)** | `RawPayloadStorePort` + `HttpRawPayloadStoreAdapter` + `OneSbCallContext` ready; `OneSbHttpClient` does not yet auto-store REQ/RES |

---

## Findings

### Major

_None._

### Minor (non-blocking)

1. **OneSbHttpClient auto-capture not wired** — Assignment design marks integration capture as “optional but preferred.” Port, HTTP adapter, and `OneSbCallContext` exist and are tested; no `OneSbCallContext.set` / store calls from quote/proposal paths or the HTTP client yet. Follow-up story can wire capture when `jobId` is available. **Does not block PRODUCT-BACKLOG or persistence ACs.**
2. **`retain_until` uses `LocalDate.now()`** — Not derived from `createdAt` Instant’s date; same wall-clock instant in practice. Optional tighten later.
3. **Local default key is 32 zero bytes** — Documented Vault stand-in; override via env in real environments (assignment allows).

---

## Test evidence notes

Re-run (reviewer):

```text
./gradlew :services:bank-persistence-service:test \
  --tests 'com.bank.persistence.crypto.AesGcmPayloadCipherTest' \
  --tests 'com.bank.persistence.config.PayloadEncryptionPropertiesTest' \
  --tests 'com.bank.persistence.api.internal.v1.RawPayloadApiTest' \
  :services:1sb-integration-service:test \
  --tests 'com.bank.insurance.onesb.adapter.persistence.HttpRawPayloadStoreAdapterTest' \
  --tests 'com.bank.insurance.onesb.adapter.onesb.client.OneSbCallContextTest'
→ BUILD SUCCESSFUL

./gradlew :services:bank-persistence-service:check
→ BUILD SUCCESSFUL (jacocoTestCoverageVerification)
```

| Suite | Tests | Failures |
|-------|------:|---------:|
| `AesGcmPayloadCipherTest` | 5 | 0 |
| `PayloadEncryptionPropertiesTest` | 3 | 0 |
| `RawPayloadApiTest` | 5 | 0 |
| `HttpRawPayloadStoreAdapterTest` | 2 | 0 |
| `OneSbCallContextTest` | 1 | 0 |
| **Total** | **16** | **0** |

Assignment DoD coverage: cipher round-trip/tamper/≠plaintext + API IT entity encrypt + decrypt GET + retain_until + persistence jacoco — met.

---

## Dual approval

| Role | Verdict | Date | Notes |
|------|---------|------|-------|
| **Tech Lead** | **APPROVE** | 2026-07-30 | AC-1…5 Accepted; capture wiring deferred non-blocking vs storage ACs |
| **QA Lead** | **APPROVE** | 2026-07-30 | See QA section below |

---

## QA Lead notes

**QA verdict: APPROVE**

| AC | Coverage | QA note |
|----|----------|---------|
| AC-1 Ciphertext at rest | Strong | Entity-level IT + cipher unit byte/UTF-8 asserts |
| AC-2 Decrypt round-trip | Strong | Unit + GET `decrypt=true` IT |
| AC-3 `encryption_key_id` | Strong | Config `test-v1` on entity + 201 body |
| AC-4 `retain_until` +7y | Strong | IT date assert + props default unit |
| AC-5 No plaintext by default | Strong | POST/GET omit `payloadBase64`; decrypt opt-in only |
| Tags / regression | Pass | All five classes `@Tag("COMP-003")`; 16/16 green; persistence `check` green |

**QA soft (non-blocking):** No IT that `OneSbHttpClient` stores payloads when context is set (by design deferred). Adapter WireMock coverage is sufficient for the port contract until capture is wired.
