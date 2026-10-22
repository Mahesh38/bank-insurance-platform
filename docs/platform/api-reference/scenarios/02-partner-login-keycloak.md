# Scenario 02 — An insurer partner rep logs in (Keycloak, OIDC + PKCE)

[← API reference home](../README.md) · Prev: [01](./01-bank-rm-login.md) · Next: [03](./03-session-lifecycle-and-logout.md)

**Status: IMPLEMENTED** (BFF, adapter, PDP). Partner users are created **directly by a bank admin**
holding `partner_user.create` — no approval step ([E5](../../architecture-review/DECISIONS-2026-10-22.md#owner-decisions)); creation queues provisioning into
Keycloak in the same transaction, and the partner stays `PENDING` until that provisioning succeeds —
only then is it `ACTIVE` and able to log in. An insurer partner can never create partner users or
approve anything (the PDP refuses `partner_user.create` and every `*.approve` action to partner
representatives).

**In one sentence:** the BFF starts an OIDC authorization-code flow with PKCE against the private
Keycloak, the browser logs in at Keycloak, Keycloak redirects back to the BFF callback, the BFF
exchanges the code for tokens through the adapter, resolves the provisioned partner in the PDP, and
keeps the tokens **encrypted in the vault** — the app again gets only an opaque handle.

## 0. Before the first login — partner onboarding by a bank admin (E5)

```mermaid
sequenceDiagram
    participant Admin as Bank admin (BANK_ADMIN)
    participant AZ as identity-authorization
    participant DB as Aurora identity
    participant D as ProvisioningOutboxDispatcher (poll-interval, default 5 s)
    participant IDPA as identity-provider-adapter
    participant KC as Keycloak admin API
    Admin->>AZ: POST /internal/v1/partner-users {username, email, insurerCode} (X-Internal-Service-Key BFF, X-Actor-Id admin)
    AZ->>AZ: PDP: admin allowed partner_user.create on insurerCode? (partner representatives: always FORBIDDEN)
    AZ->>DB: INSERT business_user (INSURER_REPRESENTATIVE, PENDING, created_by) + outbox IDENTITY_PROVISIONING_REQUESTED + outbox PARTNER_USER_CREATED (one txn)
    AZ-->>Admin: 202 {userId, approvalId: null, status: PENDING, approvalRequired: false}
    D->>DB: claim due rows FOR UPDATE SKIP LOCKED · set claimed_until (lease)
    D->>IDPA: POST /internal/v1/identities {businessUserId, username, email, insurerCode, enabled}
    IDPA->>KC: client_credentials token · search username · create user (attrs business_user_id, insurer_code; UPDATE_PASSWORD)
    IDPA-->>D: 201 {providerSubjectId}
    D->>DB: business_user.provider_subject = …, status ACTIVE · outbox published_at
```

**Maker-checker, if re-enabled.** With `identity.partner-onboarding.approval-required=true`
(env `IDENTITY_PARTNER_ONBOARDING_APPROVAL_REQUIRED`, default `false`) the create call opens an
`approval_request` instead of queuing provisioning, and a second bank admin (≠ creator, holding
`partner_user.approve`) calls `POST /internal/v1/approval-requests/{approvalId}/approve`, which
queues it. With the default, that endpoint answers `409 ILLEGAL_TRANSITION`.

A provisioning failure releases the lease and retries; after `identity.provisioning.max-attempts`
(default 10) the row is dead-lettered (`dead_lettered_at`, `ERROR` log) and the partner stays
`PENDING` until an operator acts. A login attempted while the partner is still `PENDING` fails the
activation check below.

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
| Token exchange | PKCE verifier matches; id-token signature, audience, nonce, subject | Keycloak `400` → upstream error; id-token check failure → adapter `401 AUTHENTICATION_FAILED` (was a `500`), relayed by the BFF |
| Resolve | Partner exists (created by a bank admin and provisioned) | Unknown partner → `403 FORBIDDEN` ("must be created and provisioned before first login") |
| Activation | `status = ACTIVE` | Refresh token **revoked**, then `401 AUTHENTICATION_FAILED` |
| Native exchange | Completion code exists and unused (1 min) | `400 SCHEMA_INVALID` |

## 3. Why it is built this way

- **PKCE + nonce + state** — the code is useless without the verifier only the BFF holds; replay
  and CSRF on the callback are blocked; state is single use (`GETDEL`).
- **Tokens stay server-side** — the app could not leak what it never received.
- **Partners exist only when a bank admin creates them** — the IdP account is created by the
  platform from that business user, never the other way round; Keycloak is not the source of truth.
  Who created whom is kept on `business_user.created_by` and in the `PARTNER_USER_CREATED` outbox
  event (E5).
- **Partner access is insurer-scoped** — the session carries `insurerCode`; the PDP denies
  cross-insurer access and anything not partner-visible (`INV-ACT-02`, `INV-LED-07`).

## 4. Pages for each hop

[BFF §2.2–§2.4](../services/workforce-access-bff.md#22-post-apiv1authlogin) · [IdP adapter](../services/identity-provider-adapter-service.md) · [PDP](../services/identity-authorization-service.md) · [Session vault](../interactions/bff-session-vault.md)
