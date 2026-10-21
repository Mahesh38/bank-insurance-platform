# Scenario 02 — An insurer partner rep logs in (Keycloak, OIDC + PKCE)

[← API reference home](../README.md) · Prev: [01](./01-bank-rm-login.md) · Next: [03](./03-session-lifecycle-and-logout.md)

**Status: IMPLEMENTED** (BFF, adapter, PDP). Partner users must first be created by a bank admin
(maker) and approved by a second admin (checker); approval provisions them into Keycloak.

**In one sentence:** the BFF starts an OIDC authorization-code flow with PKCE against the private
Keycloak, the browser logs in at Keycloak, Keycloak redirects back to the BFF callback, the BFF
exchanges the code for tokens through the adapter, resolves the approved partner in the PDP, and
keeps the tokens **encrypted in the vault** — the app again gets only an opaque handle.

## 0. Before the first login — partner onboarding (maker-checker)

```mermaid
sequenceDiagram
    participant Maker as Bank admin (maker)
    participant Checker as Bank admin (checker)
    participant AZ as identity-authorization
    participant DB as Aurora identity
    participant D as ProvisioningOutboxDispatcher (every 5 s)
    participant IDPA as identity-provider-adapter
    participant KC as Keycloak admin API
    Maker->>AZ: POST /internal/v1/partner-users {username, email, insurerCode} (X-Actor-Id maker)
    AZ->>DB: INSERT business_user (INSURER_REPRESENTATIVE, PENDING) + approval_request (PENDING)
    AZ-->>Maker: 202 {userId, approvalId, status: PENDING}
    Checker->>AZ: POST /internal/v1/approval-requests/{approvalId}/approve (X-Actor-Id checker ≠ maker)
    AZ->>DB: approval APPROVED · user ACTIVE · INSERT outbox_event IDENTITY_PROVISIONING_REQUESTED (one txn)
    D->>DB: SELECT unpublished events (attempts < 10)
    D->>IDPA: POST /internal/v1/identities {businessUserId, username, email, insurerCode, enabled}
    IDPA->>KC: client_credentials token · search username · create user (attrs business_user_id, insurer_code; UPDATE_PASSWORD)
    IDPA-->>D: 201 {providerSubjectId}
    D->>DB: business_user.provider_subject = …, outbox published_at
```

## 1. Login sequence

```mermaid
sequenceDiagram
    autonumber
    participant App as NIP-APP (browser / web view)
    participant BFF as workforce-access-bff
    participant KV as Valkey
    participant IDPA as identity-provider-adapter
    participant KC as Keycloak
    participant AZ as identity-authorization

    App->>BFF: POST /api/v1/auth/login {clientType, identitySource: PARTNER, returnUri, loginHint?}
    BFF->>BFF: returnUri ∈ allowed-return-uris · mint state, nonce, PKCE verifier, challenge=S256(verifier)
    BFF->>KV: SET workforce:pending:{state} AES-GCM(PendingLogin) EX 300
    BFF->>IDPA: POST /internal/v1/auth/authorization-uri {state, nonce, codeChallenge, loginHint, identitySource}
    IDPA-->>BFF: {authorizationUri}
    BFF-->>App: 200 {authorizationUri, expiresInSeconds: 300}
    App->>KC: GET authorizationUri (user enters partner credentials at Keycloak)
    KC-->>App: 302 → /api/v1/auth/callback?code&state
    App->>BFF: GET /api/v1/auth/callback?code=…&state=…
    BFF->>KV: GETDEL workforce:pending:{state} (single use)
    BFF->>IDPA: POST /internal/v1/auth/token-exchange {code, codeVerifier, expectedNonce}
    IDPA->>KC: POST token (authorization_code + code_verifier + client secret)
    IDPA->>IDPA: verify id_token signature, audience, nonce
    IDPA->>KC: GET userinfo; sub must match
    IDPA-->>BFF: ProviderSession {sub, username, email, access/refresh/id tokens, expiry, claims}
    BFF->>AZ: POST /internal/v1/identities/resolve {provider: KEYCLOAK, providerSubject: sub, userType: INSURER_REPRESENTATIVE, …}
    AZ-->>BFF: {businessUserId, status: ACTIVE, insurerCode}
    BFF->>KV: SET workforce:session:{id} AES-GCM(session incl. tokens) EX 28800
    alt WEB
        BFF-->>App: 302 Location: returnUri + Set-Cookie WORKFORCE_SESSION
    else NATIVE
        BFF->>KV: SET workforce:completion:{code} EX 60
        BFF-->>App: 302 Location: returnUri?code={completionCode}
        App->>BFF: POST /api/v1/auth/native-session {completionCode}
        BFF->>KV: GETDEL workforce:completion:{code}
        BFF-->>App: 200 {sessionHandle, expiresInSeconds}
    end
```

## 2. Validation and errors

| Step | Check | Failure |
|---|---|---|
| Login | `returnUri` exactly in the allow-list (`WORKFORCE_WEB_RETURN_URI`, `WORKFORCE_NATIVE_RETURN_URI`) | `400 SCHEMA_INVALID` |
| Callback | `state` exists and unused (5 min) | `400 SCHEMA_INVALID` ("Login transaction is invalid or expired") |
| Token exchange | PKCE verifier matches; id-token audience, nonce, subject | Keycloak `400` or adapter failure → upstream error |
| Resolve | Partner exists (approved) | Unknown partner → `403 FORBIDDEN` ("must be approved before first login") |
| Activation | `status = ACTIVE` | Refresh token **revoked**, then `401 AUTHENTICATION_FAILED` |
| Native exchange | Completion code exists and unused (1 min) | `400 SCHEMA_INVALID` |

## 3. Why it is built this way

- **PKCE + nonce + state** — the code is useless without the verifier only the BFF holds; replay
  and CSRF on the callback are blocked; state is single use (`GETDEL`).
- **Tokens stay server-side** — the app could not leak what it never received.
- **Partners exist only after maker-checker** — the IdP account is created by the platform from
  an approved business user, never the other way round; Keycloak is not the source of truth.
- **Partner access is insurer-scoped** — the session carries `insurerCode`; the PDP denies
  cross-insurer access and anything not partner-visible (`INV-ACT-02`, `INV-LED-07`).

## 4. Pages for each hop

[BFF §2.2–§2.4](../services/workforce-access-bff.md#22-post-apiv1authlogin) · [IdP adapter](../services/identity-provider-adapter-service.md) · [PDP](../services/identity-authorization-service.md) · [Session vault](../interactions/bff-session-vault.md)
