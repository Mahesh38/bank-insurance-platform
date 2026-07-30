# Coupling & Replaceability — Design Rules

**Scope:** `1sb-integration-service` — all phases  
**Authority:** Binding; changes require Architect sign-off  
**SSOT links:** [00-po-architect-design-session.md](../00-po-architect-design-session.md) · [architecture](../../architecture/1sb-integration-service-architecture.md)

---

## 1. Core principle

> **Config-driven behaviour; adapter-isolated provider.**  
> If a value, behaviour, or provider might change (product, auth, LOB, credentials), it must be configurable — not hardcoded, not copy-pasted.

---

## 2. The four configurable axes

### 2.1 Provider / upstream adapter

| Rule | Detail |
|------|--------|
| All upstream HTTP calls go through `InsuranceProviderPort` | Business logic never imports `OneSbHttpClient` directly |
| `adapter.onesb.*` is the only package allowed to know 1SB field names | ArchUnit enforces this boundary |
| Switching provider = new `adapter.<provider>.*` impl wired via Spring profile/flag | Zero changes to `application.*`, `lob.*`, or `api.*` |
| The provider routing flag (`TECH-008`) is the future switch point | Keep the port clean so it works on day one |

**Never hardcode:**
- 1SB base URLs (use `${ONESB_BASE_URL}`)
- 1SB API key/secret (use secrets manager)
- 1SB field names outside `adapter.onesb.*`
- `distributorId` in source code (use `${ONESB_DISTRIBUTOR_ID}` from vault — Decision D7)

### 2.2 Auth mode

| Rule | Detail |
|------|--------|
| Auth mode is `bank.security.inbound.mode: JWT \| MTLS \| NONE` | Set via env var `INBOUND_AUTH_MODE` |
| Each mode has its own `InboundAuthAdapter` impl | No `if (mode == JWT) … else …` in business logic |
| `NONE` mode rejected at startup outside `local` profile | Fail-fast guard, never silently insecure |
| `distributorId` extracted from config, never from JWT/request | Decision D7; ArchUnit can verify no DTO field named `distributorId` |
| `agentId` extracted from validated token claim | Decision D6; missing = 422 before any 1SB call |

See: [CONFIRM-03-inbound-auth.md](./CONFIRM-03-inbound-auth.md)

### 2.3 LOB feature flags

| Rule | Detail |
|------|--------|
| Each LOB has a kill-switch env var | `LOB_TERM_ENABLED`, `LOB_HEALTH_ENABLED`, `LOB_MOTOR_ENABLED` |
| Disabled LOB → `LOB_NOT_SUPPORTED` error; no code removal needed | `LobHandlerRegistry` checks flag before dispatching |
| New LOB = new handler + new flag only | No changes to `QuoteService`, `ProposalService`, etc. |
| LOB order of delivery is SSOT-controlled | Term P0 → Health/Motor P1 → Saving/Annuity/Pension P2 |

### 2.4 Product catalog

| Rule | Detail |
|------|--------|
| `manufacturerId` and `productCode` live only in `config/catalog/term-products.yaml` | Never in Java source as constants |
| Catalog loaded via `@ConfigurationProperties` at startup | Live-refresh if Spring Cloud Config is used |
| `enabled: false` disables a product without removing its config entry | Safe for phased rollout |
| `sandbox-only: true` prevents accidental prod routing | Validated at startup when profile=prod |
| Adding/removing a product = edit YAML + restart | No PR to business logic needed |

See: [CONFIRM-02-term-products.md](./CONFIRM-02-term-products.md) · [term-products.example.yaml](../../../../config/catalog/term-products.example.yaml)

---

## 3. Ports & adapters boundary rules

```
┌─────────────────────────────────────────────────────────┐
│ api.*          (controllers, DTOs)                       │
│   ↓                                                      │
│ application.*  (orchestrators: QuoteService, etc.)       │
│   ↓                                                      │
│ lob.*          (Term/Health/Motor handlers — LOB only)   │
│   ↓  (through port interface only)                       │
│ adapter.onesb.*  ◄── ONLY place 1SB types exist         │
│ adapter.persistence.*                                    │
└─────────────────────────────────────────────────────────┘
```

| Boundary | Rule | Enforced by |
|----------|------|-------------|
| `adapter.onesb` is isolated | No 1SB types in `api`, `application`, or `lob` | ArchUnit |
| `api.*` DTOs are bank-canonical | No 1SB field names in public-facing classes | Code review + ArchUnit |
| `lob.*` touches only LOB mapping | No shared infra (HTTP, poller) duplicated per LOB | Code review |
| Shared infra is in `application.*` or shared JARs | One poller, one HTTP client, one error model | DRY / code review |

---

## 4. What must never be hardcoded

This is the canonical list. Any deviation requires Architect sign-off and an update here.

| Value | Where it belongs | Why |
|-------|-----------------|-----|
| `distributorId` | Vault / secret manager → injected as `${ONESB_DISTRIBUTOR_ID}` | Decision D7: tenant spoofing prevention |
| 1SB base URL | `${ONESB_BASE_URL}` env var | Environment-specific; changes between sandbox/UAT/prod |
| 1SB API key & secret | Vault / secret manager | Security |
| `manufacturerId` / `productCode` | `config/catalog/term-products.yaml` only | Replaceability |
| JWT issuer / JWKS URI / audience | Env vars (`JWT_ISSUER`, `JWT_JWKS_URI`, `JWT_AUDIENCE`) | Auth is replaceable |
| mTLS truststore path/password | Env vars + vault | Security |
| Poll intervals & timeouts | `application.yaml` properties (configurable) | Tunable without redeploy |
| LOB enable/disable flags | Env vars (`LOB_*_ENABLED`) | Operational control |
| Raw payload retention period | `application.yaml` (`compliance.retention.days`) | Policy change without code |
| Master data cache TTL | `application.yaml` (`insurance.masters.cache-ttl`) | Tunable |
| 1SB field names | `adapter.onesb.*` mappers only | Replaceability / no leakage |

---

## 5. Missed-items prevention checklist

Run this checklist at every PR review and sprint retrospective:

- [ ] **No new string constant** containing a provider ID, URL, or credential was added to `application.*` or `lob.*` packages.
- [ ] **No new DTO field** named `distributorId`, `manufacturerId`, or any 1SB-specific field was added outside `adapter.onesb.*`.
- [ ] **No new LOB** was added without a corresponding `LOB_<NAME>_ENABLED` flag.
- [ ] **No new product code** was added in Java source; catalog YAML was updated instead.
- [ ] **ArchUnit** tests still pass (no `adapter.onesb` types leaked outside adapter package).
- [ ] **Auth mode switch** was tested: changing `INBOUND_AUTH_MODE` env var does not require code change.
- [ ] **Timeout/poll values** are in config, not in `@Value` defaults that shadow the property.
- [ ] **Audit events** contain `distributorId` (masked) and `agentId` — never raw `distributorId` from caller.

---

## 6. Config vs code decision tree

```
Is this value environment-specific (sandbox vs prod)?  → env var
Is this value secret (credential, key)?                → vault / secret manager
Is this value a product/insurer identifier?            → catalog YAML
Is this value a feature on/off switch?                 → env var flag
Is this value a tuning parameter (timeout, TTL)?       → application.yaml property
Is this a provider-specific field name?                → adapter layer only
Everything else                                        → discuss with Architect
```

---

## 7. Replaceability proof (E13, P2)

When E13 is implemented, it will prove replaceability by:

1. Creating a `FakeInsuranceProviderAdapter` that implements `InsuranceProviderPort` with hardcoded happy-path responses.
2. Switching the adapter via `INSURANCE_PROVIDER=FAKE` env var.
3. Running the full Term happy-path test suite against `FAKE` adapter (no 1SB dependency).

If this test can be written without touching `application.*` or `lob.*`, the boundary is clean.
