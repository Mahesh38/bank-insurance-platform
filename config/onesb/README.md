# Config — Provider Configuration

**Location:** `config/onesb/`  
**Scope:** Runtime configuration for the 1SB provider adapter (credentials refs, URL, timeouts, feature flags).  
**SSOT:** [docs/1sb-insurance-integration/service-ssot/README.md](../../docs/1sb-insurance-integration/service-ssot/README.md)

---

## Files in this directory

| File | Purpose |
|------|---------|
| `provider-config.yml` | Full provider config schema with sandbox/prod profile overlays |
| `.env.example` | Environment variable names and vault path conventions — copy to `.env`, fill in real values, never commit |

`.env` is git-ignored. Never commit real credentials.

---

## Config schema overview

`provider-config.yml` is the single source of truth for all 1SB adapter parameters:

```yaml
provider:
  id: ONE_SB              # routing key — change to BANK_MW / DIRECT to swap provider
  displayName: ...
  baseUrl: ${ONESB_BASE_URL:...}
  auth:
    type: BASIC           # or BEARER / MTLS — adapter reads this at startup
    apiKey.envVar: ONESB_API_KEY
    apiSecret.envVar: ONESB_API_SECRET
  distributorId: ${ONESB_DISTRIBUTOR_ID}
  http:
    connectTimeoutMs, readTimeoutMs, pool, ...
  poll:
    intervalMs, maxAttempts, backoffStrategy, ...
  lob:
    term.enabled, health.enabled, motor.enabled, ...
  apiVersions:
    insurance: v1
```

Profile overlays (`---` blocks) for `sandbox` and `prod` override only the values that differ between environments. Everything else inherits from the base block.

---

## Switching providers (replaceability)

> **Switching the upstream insurance middleware from 1SB to a bank-owned aggregator (or a direct insurer) requires only two changes:**
>
> 1. **Config:** Change `provider.id` (e.g. `ONE_SB` → `BANK_MW`) and update `baseUrl`, auth refs, and feature flags in `provider-config.yml`.
> 2. **Adapter bean:** Register a new `@ConditionalOnProperty(name = "provider.id", havingValue = "BANK_MW")` adapter that implements the same port interfaces (`QuotePort`, `ProposalPort`, etc.).
>
> **No domain code, no orchestration code, no bank API contract changes.**

This is the [replaceable middleware pattern](../../docs/1sb-insurance-integration/architecture/replaceable-middleware.md) (Case 2) that the architecture mandates.

### Provider routing key values

| `provider.id` value | Meaning |
|---|---|
| `ONE_SB` | 1Silverbullet gateway (current) |
| `BANK_MW` | Future bank-owned multi-insurer middleware |
| `DIRECT` | Direct insurer API (per-LOB) |
| _(any)_ | Any custom value routed by `RoutingPolicy` |

### How to add a new provider (step-by-step)

1. Add a new config file `config/<new-provider>/provider-config.yml` following the same schema.
2. Implement the adapter (e.g. `BankMwAdapter`) in `adapters/bankmw/` implementing all required port interfaces.
3. Annotate with `@ConditionalOnProperty(name = "provider.id", havingValue = "BANK_MW")`.
4. Add a new `.env.example` for the new provider's credentials.
5. Add LOB feature flags under `lob.*` for the new provider.
6. No other code changes required.

---

## Secrets policy (non-negotiable)

- Real API keys, secrets, and distributor IDs are **never stored in git**.
- All credential references in `provider-config.yml` are environment variable names or vault paths only.
- The application fails fast at startup if a required env var (`ONESB_API_KEY`, `ONESB_API_SECRET`, `ONESB_DISTRIBUTOR_ID`) is absent — this is intentional.
- See [CONFIRM-01-onesb-access.md](../../docs/1sb-insurance-integration/service-ssot/phase-0/CONFIRM-01-onesb-access.md) for vault path conventions and confirmation checklist.

---

## Local development setup

```bash
# 1. Copy the example file
cp config/onesb/.env.example .env

# 2. Fill in your sandbox credentials (obtained from 1SB RM)
#    ONESB_API_KEY=...
#    ONESB_API_SECRET=...
#    ONESB_DISTRIBUTOR_ID=...
#    SPRING_PROFILES_ACTIVE=sandbox

# 3. Start the service (credentials picked up automatically)
./mvnw spring-boot:run
# or: java -jar target/1sb-integration-service.jar
```

---

## Profile activation

| Environment | How to activate |
|---|---|
| Sandbox (default dev) | `SPRING_PROFILES_ACTIVE=sandbox` |
| Production | `SPRING_PROFILES_ACTIVE=prod` |
| CI/CD | Set via pipeline environment variables |
| Kubernetes | Set in `Deployment` env block or `ConfigMap` |
