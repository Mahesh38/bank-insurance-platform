# Any service → configuration resolution (seam S-21)

[← API reference home](../README.md)

| | |
|---|---|
| **From → to** | Every domain service, the Integration Hub, the BFF (forms) → `administration-config-service` `GET /internal/v1/config:resolve` |
| **Style** | Synchronous read, cached: in-process L1 → Valkey L2 (`catalogue:`-style per-service prefix) → service |
| **Budget** | 300 ms, **no retry**, no breaker |
| **Failure** | **Fail closed** — the action is refused (`422 CONFIGURATION_UNRESOLVABLE`). There is no compiled-in default (`CF-1`) |
| **Code status** | **CONTRACTED** (admin-config is a skeleton) |

## 1. What is resolved this way

| Consumer | Domain | Example key |
|---|---|---|
| Consent | `CONSENT_STATEMENT` | purpose set + lob → statement text + version |
| Suitability | `SUITABILITY_QUESTIONNAIRE`, `SUITABILITY_MAPPING` | lob + product class → questionnaire / grid version |
| Quotation | `QUOTE_VALIDITY` | lob → validity days |
| Proposal | form field rules | `(lob, formId)` |
| Integration Hub | `HUB_ROUTING`, distributor / agent resolution | lob + product class + insurer → provider |
| Identity (PDP) | role → permission grants | role code |
| Retention jobs | `RETENTION` | data class → window |
| BFF | `/config/forms/{formId}` | form id + lob |

## 2. Rules

1. Resolve **at the moment of the decision** with `at = now` (or the record's governing time).
2. Store the returned `configId` / version as `config_version_ref` on the business record
   (`INV-CFG-03`) — so a regulator can see which rule produced the outcome.
3. Cached values serve **until their TTL**; an expired cache with an unreachable store **refuses**.
4. A cache miss is a read through, never an error (S-25).
5. Configuration is never edited in place; a change is a new version with an effective window
   (`INV-CFG-02`), maker-checked.

## 3. Sources

- [administration-config-service](../services/administration-config-service.md) · [Domain model §2.6](../../ws3-platform/01-domain-model-and-invariants.md) · [R0 solution architecture §5.3](../../ws3-platform/03-solution-architecture-r0.md#53-resilience-policy-per-dependency-class--s07-e02-s04)
