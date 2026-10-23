# BFF ↔ session vault (Valkey / ElastiCache)

[← API reference home](../README.md)

| | |
|---|---|
| **From → to** | `workforce-access-bff` → ElastiCache for **Valkey** (Redis protocol) — `ADR-011` |
| **Style** | Synchronous; target budget 50 ms (S-25) |
| **What is stored** | Encrypted login state and sessions — the "key-value store that holds the session" |
| **Implementation** | `RedisSessionStore` when `WORKFORCE_SESSION_STORE=redis`; `InMemorySessionStore` for local tests |
| **Code status** | **IMPLEMENTED** |

## 1. Why a vault and not a JWT in the app

The app must never hold an OAuth token (standing constraint). So the BFF keeps the provider tokens
**server-side**, encrypted, under a random key, and gives the app only that key — the **opaque
session handle**. Stealing the handle from a device gives at most 8 hours of that one session and
no provider token; the vault can be revoked centrally by deleting the key.

## 2. Keys, values and TTLs

| Key | Written when | Value (before encryption) | TTL | Read |
|---|---|---|---|---|
| `workforce:pending:{state}` | Partner login starts | `PendingLogin{state, nonce, codeVerifier, clientType, identitySource, returnUri, createdAt}` | 5 min | `GETDEL` on callback — single use |
| `workforce:session:{sessionId}` | Login succeeds | `WorkforceSession{sessionId, businessUserId, userType, status, policyVersion, insurerCode, providerSubjectId, username, accessToken, refreshToken, idToken, accessTokenExpiresAt, createdAt, providerClaims}` | 8 h | `GET` on every `/api/v1/**` request |
| `workforce:completion:{code}` | Native partner login completes | `NativeCompletion{sessionId}` | 1 min | `GETDEL` on `/auth/native-session` |

Bank AD logins store no provider tokens (AD-verify returns true/false only); partner logins store
the Keycloak access, refresh and id tokens.

## 3. Encryption

`TokenVaultCipher`: AES-256-GCM, 12-byte random IV per write, 128-bit tag; stored as
base64url(IV ‖ ciphertext ‖ tag). Key from `WORKFORCE_SESSION_ENCRYPTION_KEY` — must decode to 32
bytes or the BFF refuses to start; production injects a KMS-managed random key (the dev default of
32 zero bytes is never valid in prod). **Every** decrypt failure — wrong key, tampering, truncation,
malformed base64 — returns the same opaque error, so a caller cannot use the vault as an oracle;
the request ends as `401 SESSION_EXPIRED`.

## 4. Operations per request

```text
login      → SET workforce:session:{id} <ciphertext> EX 28800
request    → GET workforce:session:{handle} → decrypt → bind principal
logout     → GET (to find a refresh token) → DEL → revoke at IdP (best effort)
partner    → SET workforce:pending:{state} EX 300 → … → GETDEL on callback
```

There is **no sliding expiry** today: the session ends 8 hours after login regardless of activity.
Idle timeout and lockout policies from Login BRD v3.1 (`SUG-20261008-otp`) are parked. The accepted BRD also adds Insurance RM Forgot Password and a 60-day platform-password expiry. Those are not in this vault.

## 5. Failure behaviour

| Situation | Effect |
|---|---|
| Valkey unreachable | Session lookup fails → the request cannot be authenticated. Sessions are the **one** thing the BFF cannot read through to another store — this is an availability dependency, not a correctness one |
| Key evicted / expired | `401 SESSION_EXPIRED`; user logs in again |
| Cross-prefix read | Forbidden by Valkey ACL user per deployable (`FF-24`, S09). Read access to the vault is read access to live sessions |

**Never in Valkey:** idempotency records, suitability, consent, payment, certifications, or
configuration past its TTL.

## 6. Sources

- `services/workforce-access-bff/src/main/java/com/bank/workforce/bff/session/RedisSessionStore.java` · `TokenVaultCipher.java` · `SessionModels.java`
- [Store ownership §3.1](../data/store-ownership.md#31-valkey-keys-used-by-the-bff-today) · [R0 HLD Boundary 8](../../../architecture/R0-HLD.md#boundary-8--data-and-persistence)
