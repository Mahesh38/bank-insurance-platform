# administration-config-service — Administration and configuration (#19)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The one store of **versioned, effective-dated, LOB-partitioned** business configuration: consent statements, suitability questionnaires and mapping grids, form field rules, validity windows, routing policy for the Hub, distributor / agent resolution, role→permission grants for the PDP, retention windows. If a rule lived in an `if`, Compliance could not change a consent clause without a deploy (`CF-1`). There is **no compiled-in fallback** anywhere |
| **Module** | `services/administration-config-service` · port 8104 · `service-id: admin-config` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1/config` (HLD §5.3) |
| **Callers** | Every domain service and the Hub (S-21 resolve) · BFF (`/config/forms/{formId}`) · admin UI (R0 W4, isolated) |
| **Owns data** | Aurora schema **`administration`** — `configuration_record` (INSERT-only; a change is a new version) · role `app_administration` |
| **Code status** | **SKELETON**. Resolve CONTRACTED (`GET /internal/v1/config:resolve`); admin writes are maker-checker and **R1** on the BFF; R0 seeds from source-controlled artefacts |

## 1. API index

| Method | Path | Status | Purpose |
|---|---|---|---|
| `GET` | `/internal/v1/config:resolve?domain&lob&key&at` | CONTRACTED | Resolve the version effective at `at` |
| `GET` | `/internal/v1/config/forms/{formId}?lob` | PROPOSED | Field validation rules by `(lob, formId)` for the BFF |
| `POST` | `/internal/v1/config/versions` | PROPOSED (maker) | Draft a new version |
| `POST` | `/internal/v1/config/versions/{configId}/approve` | PROPOSED (checker) | Activate with an effective window |

## 2. Resolve contract (S-21)

Request: `domain` (e.g. `CONSENT_STATEMENT`, `SUITABILITY_QUESTIONNAIRE`, `SUITABILITY_MAPPING`,
`HUB_ROUTING`, `QUOTE_VALIDITY`, `RETENTION`), `lob`, `key` (and optional `insurerId`,
`productCode`, `journeyType`, `formId`, `fieldId`, `roleId`, `actorType`), `at` (default now).

Response `200`: `{ "configId": "01K…", "domain": "QUOTE_VALIDITY", "lob": "LIFE", "version": 4, "effectiveFrom": "…", "effectiveTo": null, "checksum": "…", "payload": { "validityDays": 7 } }`.

Caller rules: 300 ms, **no retry**, cache to the resolution TTL (L1 → L2); **fail closed**: no
resolution → refuse the action with `422 CONFIGURATION_UNRESOLVABLE`. Every business record stores
the `config_version_ref` that governed it (`INV-CFG-03`, `CONFIG_VERSION_REQUIRED`).

## 3. Data

`configuration_record` (`config_id`, `domain`, `lob`, `insurer_id`, `product_code`,
`journey_type`, `form_id`, `field_id`, `role_id`, `actor_type`, `version`, `payload` JSONB,
`effective_from`, `effective_to`, `status`, `checksum`, `seed_ref`, `created_by`). Never updated
in place (`INV-CFG-02`). DDL [`13-administration.sql`](../../data-architecture/schemas/13-administration.sql).

## 4. Sources

- [Configuration resolution](../interactions/configuration-resolution.md) · [Domain model §2.6](../../ws3-platform/01-domain-model-and-invariants.md) · [R0 HLD §2.4](../../../architecture/R0-HLD.md#24-configuration-layer-ships-in-w0b-the-admin-ui-is-r0-w4)
