# administration-config-service — Administration and configuration (#19)

[← API reference home](../README.md)

| | |
|---|---|
| **Role** | The one store of **versioned, effective-dated, LOB-partitioned** business configuration — including **rule sets** (`rule-set/v1`, ADR-024): consent statements, suitability questionnaires and mapping grids, form field rules, validity windows, routing policy for the Hub, distributor / agent resolution, role→permission grants for the PDP, retention windows. If a rule lived in an `if`, Compliance could not change a consent clause without a deploy (`CF-1`). There is **no compiled-in fallback** anywhere |
| **Module** | `services/administration-config-service` · port 8104 · `service-id: admin-config` · layer L5 · INTERNAL |
| **Base path** | `/internal/v1/rule-sets` (rule-set store) · legacy resolve shape also documented under `/internal/v1/config` in HLD §5.3 |
| **Callers** | Every domain service and the Hub (S-21 resolve) · BFF (`/config/forms/{formId}`) · admin UI (R0 W4, isolated) |
| **Owns data** | Aurora schema **`administration`** — `rule_set_version` (INSERT / replace draft; maker-checker publish) · `outbox_event` · role `app_administration` |
| **Code status** | **IMPLEMENTED** rule-set store (≈ 75 % maturity): create/list/get/replace draft, submit, approve, reject, simulate, resolve, seed import as DRAFT. Outbox relay and PDP `rule-set.approve` still open (see [BUILD-BRIEF](../../contracts/administration-config-service/BUILD-BRIEF.md)) |

## 1. API index

Contract SSOT: [`openapi.yaml`](../../contracts/administration-config-service/openapi.yaml).

| Method | Path | Status | Purpose |
|---|---|---|---|
| `POST` | `/internal/v1/rule-sets/{ruleSetId}/versions` | IMPLEMENTED | Create draft version |
| `GET` | `/internal/v1/rule-sets/{ruleSetId}/versions` | IMPLEMENTED | List versions |
| `GET` | `/internal/v1/rule-sets/{ruleSetId}/versions/{version}` | IMPLEMENTED | Get one version |
| `PUT` | `/internal/v1/rule-sets/{ruleSetId}/versions/{version}` | IMPLEMENTED | Replace draft |
| `POST` | `…/versions/{version}:submit` | IMPLEMENTED | Submit for checker |
| `POST` | `…/versions/{version}:approve` | IMPLEMENTED | Approve (interim `X-Actor-Roles`) |
| `POST` | `…/versions/{version}:reject` | IMPLEMENTED | Reject |
| `POST` | `…/versions/{version}:simulate` | IMPLEMENTED | Dry-run against bank-rule-engine |
| `GET` | `/internal/v1/rule-sets:resolve` | IMPLEMENTED | Resolve effective set at `at` |
| `GET` | `/internal/v1/config/forms/{formId}?lob` | PROPOSED | Field validation rules by `(lob, formId)` for the BFF |

## 2. Resolve contract (S-21)

Request: `ruleSetId` (and optional LOB / product discriminators), `at` (default now).

Caller rules: 300 ms, **no retry**, cache to the resolution TTL (L1 → L2); **fail closed**: no
resolution → refuse the action with `422 CONFIGURATION_UNRESOLVABLE`. Every business record stores
the `config_version_ref` that governed it (`INV-CFG-03`, `CONFIG_VERSION_REQUIRED`).

On publish, `RuleSetPublished` is written to `administration.outbox_event` (catalogue entry present;
relay not yet wired — LLD open item).

## 3. Data

`rule_set_version` plus immutability trigger (PostgreSQL vendor migration). Never mutate a
published version in place (`INV-CFG-02`). DDL and Flyway:
[`13-administration.sql`](../../data-architecture/schemas/13-administration.sql) · service
`V2__administration_rule_set_version` / `V3` trigger.

## 4. Related

- [Contract LLD](../../contracts/administration-config-service/LLD.md)
- [Build brief](../../contracts/administration-config-service/BUILD-BRIEF.md)
- [Rule-engine guide](../../contracts/rule-engine/LLD.md) · [ADR-024](../../architecture-review/ADR-024-configuration-driven-rule-engine.md)
