# Ports, environments and run modes

[← API reference home](../README.md)

## 1. Local ports and error-contract identity

Every service has a fixed local port and a registered `service-id` that appears in every error
response, log line and metric tag (`bank.error.service-id`).

| Service | Local port | `service-id` | Layer | Error boundary | Datastore at runtime today |
|---|---|---|---|---|---|
| `1sb-integration-service` | 8080 | `onesb` | L5 | INTERNAL | None — HTTP to persistence |
| `bank-persistence-service` | 8081 | `persistence` | L7 | INTERNAL | PostgreSQL / H2 (Flyway V1) |
| `identity-provider-adapter-service` | 8082 | `idp-adapter` | L5 | INTERNAL | None |
| `identity-authorization-service` | 8083 | `authz` | L5 | INTERNAL | PostgreSQL / H2 (Flyway V1, V2) |
| `workforce-access-bff` | 8084 | `bff` | L4 | **PUBLIC** (redacts) | Valkey/Redis or in-memory |
| Keycloak (compose) | 8085 | — | — | — | Keycloak DB |
| `customer-service` | 8090 | `customer` | L5 | INTERNAL | H2 (Flyway V1 placeholder) |
| `lead-service` | 8091 | `lead` | L5 | INTERNAL | `JdbcLeadRepository` → `lead_lms.lead` + activity / idempotency / outbox tables (H2 locally, Flyway `V2`–`V3`); `lead.store=memory` for the in-memory stores |
| `consent-service` | 8092 | `consent` | L5 | INTERNAL | Skeleton |
| `suitability-service` | 8093 | `suitability` | L5 | INTERNAL | Skeleton |
| `product-catalogue-service` | 8094 | `catalogue` | L5 | INTERNAL | Skeleton |
| `journey-orchestration-service` | 8095 | `journey-orchestration` | L6 | INTERNAL | Skeleton |
| `quotation-service` | 8096 | `quotation` | L5 | INTERNAL | Skeleton |
| `proposal-service` | 8097 | `proposal` | L5 | INTERNAL | Skeleton |
| `payment-service` | 8098 | `payment` | L5 | INTERNAL | Skeleton |
| `policy-issuance-service` | 8099 | `policy` | L5 | INTERNAL | Skeleton |
| `integration-hub-service` | 8100 | `integration-hub` | L5 | INTERNAL | Skeleton |
| `audit-compliance-service` | 8101 | `audit` | L5 | INTERNAL | Skeleton |
| `notification-service` | 8102 | `notification` | L5 | INTERNAL | Skeleton |
| `reporting-mis-service` | 8103 | `reporting` | L5 | INTERNAL | Skeleton |
| `administration-config-service` | 8104 | `admin-config` | L5 | INTERNAL | Skeleton |
| `direct-insurer-adapter-service` | 8105 | `direct-insurer` | L5 | INTERNAL | Skeleton |

Every skeleton exposes `GET /internal/v1/<context>/info` returning
`{ "service", "boundedContext", "status": "SKELETON" }` and the actuator health endpoints.

## 2. Run modes — stub vs real

Several services can run against stubs so a developer does not need bank connectivity. **`dev`
may stub; `uat` and `prod` may not** — a journey evidenced against a stub is not evidence
(`ADR-009`, seam S-26).

| Service | Property (env var) | Values | Default | Prod rule |
|---|---|---|---|---|
| BFF → lead | `workforce.downstream.lead-mode` (`LEAD_DOWNSTREAM_MODE`) | `stub` · `http` | `stub` | `http` |
| BFF → customer | `workforce.downstream.customer-mode` (`CUSTOMER_DOWNSTREAM_MODE`) | `stub` · `http` | `stub` | `http` |
| BFF session vault | `workforce.session.store` (`WORKFORCE_SESSION_STORE`) | `memory` · `redis` | `memory` | `redis` (Valkey) |
| IdP adapter → AD | `identity.ad-verify.mode` (`BANK_AD_VERIFY_MODE`) | `stub` · `http` | `stub` | `stub` **refused at startup** under the `prod` profile |
| Customer → Apigee token | `customer.apigee.token-mode` (`CUSTOMER_APIGEE_TOKEN_MODE`) | `stub` · `http` | `stub` | `http` |
| Customer → CBS | `customer.cbs.inquiry-mode` (`CUSTOMER_CBS_INQUIRY_MODE`) | `stub` · `http` | `stub` | `http` |
| 1SB secrets | `insurance.secrets.source` (`INSURANCE_SECRETS_SOURCE`) | `PROPERTIES` · `ENV` · `AWS_SECRETS_MANAGER` | `PROPERTIES` | `AWS_SECRETS_MANAGER` |

Local AD-verify stub users (`identity-provider-adapter-service/application.yml`):
`EMP001` / `local-stub-password` (active) and `EMP002` / `local-stub-password` (inactive — use it
to test the "account not active" path).

## 3. Downstream base URLs (BFF)

| Property | Env var | Local default |
|---|---|---|
| `workforce.downstream.provider-adapter-base-url` | `IDENTITY_PROVIDER_ADAPTER_BASE_URL` | `http://localhost:8082` |
| `workforce.downstream.provider-adapter-internal-key` | `IDENTITY_ADAPTER_INTERNAL_SHARED_SECRET` | dev-only value; ≥ 16 chars; no prod default |
| `workforce.downstream.authorization-service-base-url` | `IDENTITY_AUTHORIZATION_BASE_URL` | `http://localhost:8083` |
| `workforce.downstream.lead-service-base-url` | `LEAD_SERVICE_BASE_URL` | `http://localhost:8091` |
| `workforce.downstream.customer-service-base-url` | `CUSTOMER_SERVICE_BASE_URL` | `http://localhost:8090` |
| `workforce.session.encryption-key` | `WORKFORCE_SESSION_ENCRYPTION_KEY` | 32 zero bytes (**dev only**; prod injects a KMS-managed random key) |
| `spring.data.redis.host` / `port` | `REDIS_HOST` / `REDIS_PORT` | `localhost:6379` |
| `workforce.downstream.timeouts.{lead,customer,authorization,identity-provider}.{connect,read}` | — | 500ms/2s · 500ms/4s · 500ms/2s · 1s/6s |
| `workforce.authorization.decision-timeout` | — | `300ms` (S-02) |

lead-service: `lead.authorization.base-url` (`IDENTITY_AUTHORIZATION_BASE_URL`, default
`http://localhost:8083`), `lead.authorization.decision-timeout` (`300ms`), `lead.crypto.field-key`
/ `key-id` (`LEAD_FIELD_KEY` / `LEAD_FIELD_KEY_ID`; 32 zero bytes **dev only**). Lead create and
assign need a reachable PDP and a granted user — locally see `scripts/local/grant-local-rm.sql`.

Other services: `BANK_PERSISTENCE_BASE_URL` (1SB adapter → persistence, default
`http://localhost:8081`), `ONESB_BASE_URL` (default the 1SB demo host; in UAT/prod this becomes the
Apigee proxy per `ADR-020`), `CBS_INQUIRY_BASE_URL`, `APIGEE_TOKEN_URL`, `BANK_AD_VERIFY_BASE_URL`,
`KEYCLOAK_*`.

## 4. Environments

| Environment | Where | Stubs allowed | Notes |
|---|---|---|---|
| local / `dev` | Developer machine, compose; `dev` lives inside the UAT account (two VPCs) | Yes | `docker-compose.yml` (1SB + persistence + Postgres), `docker-compose.identity.yml` (identity stack + Redis + Keycloak) |
| `uat` | Bank-vended UAT account, `ap-south-1` | **No** | Apigee UAT host for CBS and AD-verify |
| `prod` | Bank-vended prod account, `ap-south-1`; DR `ap-south-2` | **No** | Stub modes refuse to start |
| Render.com | Dev preview only | — | **Never** a data path for PII or production-like data |

## 5. Run a service

```bash
./gradlew :services:<module>:bootRun
curl -s http://localhost:<port>/actuator/health
```

Identity stack end to end (BFF + IdP adapter + PDP + Redis + Keycloak):

```bash
docker compose -f docker-compose.identity.yml up --build
```

## 6. Sources

- `services/*/README.md` and `services/*/src/main/resources/application.yml`
- [Platform error contract §2.2](../../../journey-execution/07-PLATFORM-ERROR-CONTRACT.md#22-service-identity)
- [R0 for developers §8](../../../architecture/R0-E2E-FOR-DEVELOPERS.md)
