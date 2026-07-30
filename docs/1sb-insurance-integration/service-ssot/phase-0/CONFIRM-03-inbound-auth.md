# CONFIRM-03 — Inbound Authentication

**Phase:** 0.3  
**Status:** `DECISION PENDING — dual adapters required`  
**Owner:** Security Team + Architect  
**Config example:** [`config/onesb/inbound-auth.example.yaml`](../../../../config/onesb/inbound-auth.example.yaml)  
**Ack:** [PO-DEV-ENV-REQUIREMENTS.md](./PO-DEV-ENV-REQUIREMENTS.md)

---

## Purpose

Bank→service auth is **not finally decided**. All callers live in the **same K8s cluster**.  
Engineering will implement **both JWT and mTLS** and select via config (`JWT` | `MTLS` | `JWT_AND_MTLS`).

---

## SSOT decisions that are binding

| Decision | Statement |
|----------|-----------|
| D6 | `agentId` **mandatory** on proposal submit |
| D7 | `distributorId` from config/secrets **only** — never from caller |
| Dual-ready | Both JWT + mTLS adapters shipped; mode is config-only |

---

## Replaceable auth config

### Application config structure

```yaml
# application.yaml (environment-specific override via env vars)
bank:
  security:
    inbound:
      # Supported modes: JWT | MTLS | NONE
      # NONE is dev-local only — rejected at startup in prod/uat profiles
      mode: ${INBOUND_AUTH_MODE:JWT}

      jwt:
        issuer:    ${JWT_ISSUER:}            # e.g. https://iam.bank.internal/
        jwks-uri:  ${JWT_JWKS_URI:}          # e.g. https://iam.bank.internal/.well-known/jwks.json
        audience:  ${JWT_AUDIENCE:}          # e.g. 1sb-integration-service
        # Claim from which actorId (agentId context) is extracted
        actor-id-claim: ${JWT_ACTOR_ID_CLAIM:sub}
        # Required roles claim (comma-separated, optional — empty = any authenticated caller)
        required-roles: ${JWT_REQUIRED_ROLES:}

      mtls:
        # Path to truststore containing bank CA / client CA certificates
        truststore-path:     ${MTLS_TRUSTSTORE_PATH:}
        truststore-type:     ${MTLS_TRUSTSTORE_TYPE:PKCS12}
        truststore-password: ${MTLS_TRUSTSTORE_PASSWORD:}   # loaded from vault
        # CN or SAN pattern required on client cert (optional, extra defense)
        required-cn-pattern: ${MTLS_REQUIRED_CN_PATTERN:}
```

### Mode switching — zero code change

| Mode | When to use | How to activate |
|------|-------------|-----------------|
| `JWT` | All environments except dev-local; API gateway validates and forwards | Set `INBOUND_AUTH_MODE=JWT` + all `JWT_*` vars |
| `MTLS` | If bank security mandates mutual TLS instead of / in addition to JWT | Set `INBOUND_AUTH_MODE=MTLS` + all `MTLS_*` vars |
| `NONE` | **Developer local only** — test without auth plumbing | Set `INBOUND_AUTH_MODE=NONE` **and** profile `local` |

`NONE` + non-local profile → application **refuses to start** (fail-fast guard in `InboundAuthSecurityConfig`).

---

## How auth is wired (ports/adapters principle)

```
InboundAuthPort (interface)
  ├── JwtInboundAuthAdapter      (active when mode=JWT)
  ├── MtlsInboundAuthAdapter     (active when mode=MTLS)
  └── NoOpInboundAuthAdapter     (active when mode=NONE, local only)
```

Swapping the Security team's preferred mechanism = change one env var + provide the corresponding config values. No changes to `QuoteService`, `ProposalService`, or any business logic.

---

## JWT configuration checklist (Security team sign-off required)

| # | Item | Owner | Status |
|---|------|-------|--------|
| C3-1 | `JWT_ISSUER` value provided (IAM / OIDC provider URL) | Security | **PENDING** |
| C3-2 | `JWT_JWKS_URI` value provided and reachable from service egress | Security | **PENDING** |
| C3-3 | `JWT_AUDIENCE` agreed (service identifier for token validation) | Security + Arch | **PENDING** |
| C3-4 | `actor-id-claim` agreed (which JWT claim carries the calling agent/system identity) | Security + Arch | **PENDING** |
| C3-5 | Sample token (non-prod, short-lived) provided for integration testing | Security | **PENDING** |
| C3-6 | Token contains `actorId` compatible with `agentId` requirement (D6) — or mapping rule documented | Security + Arch | **PENDING** |

## mTLS configuration checklist (if mode=MTLS selected)

| # | Item | Owner | Status |
|---|------|-------|--------|
| C3-7 | Bank client CA certificate provided for truststore | Security | **PENDING** |
| C3-8 | `MTLS_TRUSTSTORE_PATH` agreed (Kubernetes secret mount path) | Platform | **PENDING** |
| C3-9 | CN/SAN pattern for allowed client certs documented | Security | **PENDING** |
| C3-10 | mTLS termination point clarified: API gateway or service itself | Security + Arch | **PENDING** |

---

## Attribute extraction rules (non-negotiable — SSOT decisions D6, D7)

```
agentId  = extracted from validated JWT claim (actor-id-claim)  OR from mTLS client CN mapping
         = NEVER accepted from request body or query param

distributorId = loaded from application config / secret store at startup
              = NEVER accepted from any inbound request field
              = NEVER logged in plaintext (masked to first 4 chars + ***)
```

If a caller provides `distributorId` in a request body, the service **ignores it silently** (not a 4xx — the field simply doesn't exist in public DTOs).  
If `agentId` is absent on a proposal submit, the service returns `422 AGENT_ATTRIBUTION_MISSING` (see FUNC-005 AC).

---

## Checklist (sign-off required before Phase 1)

- [ ] C3-1 through C3-6 completed (JWT path, minimum viable)
- [ ] Auth mode decided and documented in environment runbook
- [ ] Sample token provided and tested against `POST /v1/quotes` in dev/sandbox
- [ ] `agentId` extraction verified end-to-end (token → service → audit event)
- [ ] `distributorId` confirmed absent from all public DTOs (ArchUnit test added)
- [ ] `NONE` mode guard tested: service rejects startup with `INBOUND_AUTH_MODE=NONE` in uat profile

---

## Notes

- Prefer JWT if the bank already has an API gateway (Kong, APIM, etc.) that can issue/forward tokens — this avoids TLS termination complexity at the service.
- If the decision changes after this phase, update this doc and the runbook; no code change is needed if the adapter boundary is respected.
- All auth-related config vars should be stored in Vault / cloud secret manager, not in `application.yaml` committed to git.
