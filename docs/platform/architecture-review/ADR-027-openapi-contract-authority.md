# ADR-027 — The checked-in OpenAPI is the contract; code must conform

**Status:** Accepted (owner decision 2026-10-21, `reviewer_type: HUMAN`).
**Date:** 2026-10-21
**Origin:** [`SUG-20261021-rul`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20261021-rul--configuration-driven-rule-engine-and-the-nine-open-decisions) — review decision D9.

## Decision

1. Every service has `docs/platform/contracts/<service>/openapi.yaml` (OpenAPI 3.1) and
   `docs/platform/contracts/<service>/LLD.md`. The OpenAPI is **authoritative on the wire**: paths,
   methods, headers, request/response schemas, status codes and error codes. Where code and contract
   disagree, the code is wrong (or the contract is changed first, by review).
2. Error responses use the platform problem+json (`conventions/error-model.md`); every documented
   error code must exist in `ErrorCodes`.
3. **Enforcement:** `libs/bank-common-test` provides `OpenApiConformance`; each implemented service
   has a test that boots the context, reads springdoc's `/v3/api-docs` and fails when an operation,
   parameter, required field or status code differs from the checked-in file. Skeleton services
   skip until their first endpoint lands.
4. A change to a contract is a reviewed change to the YAML; breaking changes need a new path version
   (`/v2/…`) or a deprecation window.
5. Older contract files (`ws3-platform/*.openapi.yaml`) are superseded by the per-service files and
   carry a banner pointing to them.

## Consequences

Contract drift becomes a failing test instead of a review finding; consumers (Flutter, other
services) can generate clients from the YAML.
