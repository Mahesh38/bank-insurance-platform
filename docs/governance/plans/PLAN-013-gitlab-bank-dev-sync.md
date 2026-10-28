# PLAN-013 — GitLab bank dev sync into this repository

```yaml
# schema: implementation-plan
id: PLAN-013
work_item: SUG-20261028-gls
origin: SUG-20261028-gls
workstream: WS-3
risk_tier: T3
author: "agent:cursor (persona: Amit)"
date: "2026-10-28"

objective: >
  Keep this GitHub repository and the bank GitLab dev line
  (gitlab-ce.au.bank.in/insurance/backend/nip-backend, tip 42eb8f5d)
  aligned on required behaviour, without copying practices CODING-STANDARD.md
  rejects and without overwriting work that landed here after the snapshot.

problem: >
  The bank dev line started as a snapshot of selected services. After
  9 October 2026 the bank team changed customer search, lead persistence,
  the workforce BFF, Flyway, containers and GitLab CI. This repository
  kept moving on the same services. A tree-level patch from either
  "First commit" baseline collides with later GitHub history.

out_of_scope:
  - Renaming services/1sb-integration-service
  - Replacing the lead JDBC model with the bank Spring Data + MapStruct stack
  - Importing GitLab CI, ECR, Sonar or deploy credentials
  - Copying conflicted or uncommitted GitLab working-tree files
  - Inventing customer_details columns, CBS field maps or BFF JSON fields
    that are not in the october-first-commit.patch

files_expected:
  - build.gradle.kts
  - "**/gradle.lockfile"
  - docs/governance/plans/PLAN-013-gitlab-bank-dev-sync.md
  - docs/governance/registers/SUGGESTION-REGISTER.md
  - docs/governance/registers/PARKED-BACKLOG.md
  - docs/context/DOC-MAP.yaml
```

Freshness check on 2026-10-28 was **WARN** (exit 1): `state_as_of` is 28 days old and `review_due` is 2026-10-28. Work was not blocked. `docs/governance/04-STAGE_GATES.md` is past its 14-day freshness limit.

## What was compared

Source of the bank delta: the uploaded handoff `DEV_CHANGES_AND_REPLICATION_PLAN_8605.md` (prepared 26 October 2026 from local Git objects, no network fetch). Pinned tip `42eb8f5d85b5dcb0ded686a3985f9fb872fe60b7`. The three binary patches named in that handoff were **not** in the upload.

| Baseline | Commit | When to use |
|---|---|---|
| October First commit | `e0efffbb5e57d92dd278f601fc16822a1e53f9a1` | This repository. The October snapshot is the later copy of these services. |
| September First commit | `fff006059ab18a556b1e1be692a655ee536e7eb5` | Only a tree that still matches that earlier skeleton. |
| Initial commit | `cdeb5b0671e3419f2d270f66232794ea6646a2ad` | Only a tree that contains that skeleton alone. |

Patch checksums from the handoff, for the day the files arrive:

```text
37B1B2C26B4C057A56A7AF9DBEDE6802D9F1BCF9660B3EBDEB6A28F93AA528AC  september-first-commit.patch
F7A23B0366576D89D8C476B1417367A57D7117FB5256DA2DAEE6AAD017B9CAE9  october-first-commit.patch
F61FBD57E1A246954A00B5628036AA7DE94CE77605F2E0AA5F59A95C41F0D580  initial-commit.patch
```

`https://gitlab-ce.au.bank.in/insurance/backend/nip-backend` answered HTTP 403 (Cloudflare challenge) from this environment, so the tip could not be fetched.

This repository's `HEAD` when the comparison was made: `bb99024d8809edf71ace9f978b2c2b7bf7c563f4` (28 October 2026, suitability consent link).

## Already on GitHub — do not re-port

These bank notes describe behaviour this tree already has. Copying the bank files would replace it with the older snapshot.

| Bank note | Where it already lives |
|---|---|
| Customer search returns a customer id beside a masked identifier | `CustomerInquiryPort.CustomerHit` carries `customerId` and `maskedCif`. The BFF record uses the same names. |
| Unauthorized CBS call drops the token, retries once, then `UPSTREAM_UNAVAILABLE` | `CustomerSearchService.withToken` |
| Customer absent is not HTTP 200 | `CustomerSearchService.get` raises `RESOURCE_NOT_FOUND` |
| Lead meetings are persisted | `lead_lms.lead_meeting` via `JdbcLeadActivityStore` and Flyway `V3__lead_lms_activity.sql` |
| Lead state, product class, assignment | `Lead`, `LeadState`, `ProductClass`, `V2__lead_lms_lead.sql` |
| Integration service listens on 8080 and owns no Flyway | module `services/1sb-integration-service` |
| Spotless is the formatter | root `build.gradle.kts`, Google Java Format via Spotless |

`maskedCif` is the field name on this contract. The bank handoff says `maskedCid`. Do not rename the GitHub field. A bank client maps `maskedCid` to `maskedCif` until a contract change says otherwise.

## Applied in this change

Flyway **11.20.2** for `flyway-core` and `flyway-database-postgresql`. Spring Boot 3.5.16 pins 11.7.2. The bank commit `02d8ea29` (13 October 2026, "Rename OneSB service and upgrade Flyway for PostgreSQL 18") is the required half of that commit. PostgreSQL 18 is not a recognised database on 11.7.2. The module rename in the same commit is rejected below.

## Port later — rewrite onto this tree, do not apply the patch

Unpark when `october-first-commit.patch` is present and its SHA-256 is `F7A23B0366576D89D8C476B1417367A57D7117FB5256DA2DAEE6AAD017B9CAE9`. Read the bank file, then write the behaviour in this repository's style. Each item gets the test required by `CODING-STANDARD.md` section 19.

| Bank behaviour | How it lands here |
|---|---|
| Customer-service owns the customer snapshot. Lead-service reads and writes it over HTTP. PUT keeps an existing lead snapshot. Query returns the requested lead ids. DELETE removes one. | New customer-service port, application service and controller. Lead-service gets an HTTP adapter. Schema is a **new** Flyway version after the current customer `V1__init_schema.sql`. Columns come from the patch, not from a guess. |
| Lead migrations V2–V7 on the bank line (persistence schema, meetings, masked PAN, optional email, archive customer details) | Do not add those files. GitHub already has `V2__lead_lms_lead.sql`, `V3__lead_lms_activity.sql`, `V4__lead_lms_design_alignment.sql` and `V6__lead_exception_evidence.sql`. A second V2 fails checksums. Bring any missing column in as the next free version, reviewed against `docs/platform/data-architecture/schemas/04-lead_lms.sql`. |
| Only customer **detail** records are cached | In-process cache behind a domain port, keyed by customer id, populated from `findById` only. Search results stay uncached. Cluster Valkey stays on the parked remainder of `SUG-20261006-atk` (ADR-011). Do not add a second Redis client for this. |
| BFF CORS | `local` and `dev` only. `allowCredentials` false. Paths under `/api/**`. Methods and headers copied from the bank profile, not invented. `uat` and `prod` stay without a wildcard origin. Opening Swagger is a separate security review; this tree currently denies it. |
| Lead and BFF response-mapping commits (`5d6a0f02`, `c5b86b60`, `a730c9f1`, `5c8d3339`, `eba83620`, `8be4a368`) | Diff each controller record against the current GitHub record. Add a field only when the patch shows it and the API reference agrees. Keep `ProductClass`, `LeadState` and `DashboardStage`. |
| BootRun profile order: `spring.profiles.active` system property, then `SPRING_PROFILES_ACTIVE`, then Gradle `springProfile`, else `local` | One `bootRun` block in the root build, after the patch shows the exact snippet. |
| Context path and profile YAML | Reconcile URL and context-path keys with this tree. Do not copy bank hostnames, passwords or tokens. The handoff's conflicted `application-local.yml` and `application-uat.yml` are excluded. |
| Hand-written mappers the bank wrote with MapStruct | `{Name}Mapper.from`, in the adapter that owns the translation. No `dto` package. |

## Reject — do not merge these GitLab changes

| Change | Why |
|---|---|
| Rename `services/1sb-integration-service` to `services/onesb-integration-service`, and the matching deletions of the old tree | This repository's module name, ArchUnit boundary and "no Flyway / no JPA in the integration service" rule stay. The handoff itself says not to deploy two copies. |
| MapStruct (`LeadPersistenceMapper`, `LeadMapperConfig`, `LeadPersistenceDto`) | `CODING-STANDARD.md` mapping rule: hand-written `from` or `{Name}Mapper`. MapStruct is rejected. |
| Packages and types named `dto`, and Spring Data repositories placed beside a second JPA model of `Lead` | `CODING-STANDARD.md` forbids a `dto` package. This lead service already persists through JDBC (`JdbcLeadRepository`). A parallel JPA aggregate would fork the model. |
| Deletes of `DashboardStage`, `LeadState`, `ProductClass` | Those types are the current domain model. The bank rename to `ProductType` is the snapshot's vocabulary, not this tree's. |
| GitLab child pipelines, ECR publish, Sonar, runner tags, and the October 21–22 auto-deploy experiments | Bank delivery stays on GitLab. This repository's CI is GitHub Actions. Do not copy registry ids, credentials or deploy targets. |
| Spotless-only sweep (`50cb6142`) | Formatting here is already Spotless. Replaying their format commit would churn files that diverged after the snapshot. |
| Wildcard CORS outside `local` / `dev` | The handoff says those origin rules do not enable a production browser client. |
| Uncommitted GitLab files (`customer-service` README, conflicted local/uat YAML, untracked `docs/`) | The handoff excludes them. |

## Dependency order once the October patch is in hand

1. Confirm the patch checksum. `git apply --check` will fail on this tree. That failure is expected. Use the patch as the specification, not as a commit to apply.
2. Shared masking helper, only if `CustomerMasking` does something `maskedCif` does not already do. Put it in `libs/bank-common-domain` with a test. No PAN, Aadhaar, phone or email in logs.
3. Customer snapshot schema (new version), application service, internal API, tests for PUT-preserve, query-by-lead-id, DELETE, and not-found.
4. Lead HTTP adapter to that API. Remove any temptation to keep a second customer-details table in lead-service. If the bank V7 archive is still required, it is a new lead migration, not their `V7__archive_customer_details.sql` filename.
5. Detail cache and its tests (hit, miss, search-not-cached, unauthorized still retries once).
6. BFF CORS for local/dev, with a test that uat/prod do not register a wildcard.
7. Response fields, one controller at a time, with the existing MockMvc tests updated to the agreed contract.

Do not start step 3 until the patch supplies the column list. Guessing a customer-details table would make the two repositories diverge further.
