# GitLab provisioning request — AU Bank Insurance Distribution Platform

**Audience:** AU Bank DevOps / GitLab administrators / bank platform engineering  
**From:** Application delivery team — Shivanshi (SRE / R10), with Mahesh (Architecture), Amit (Engineering), Deepali (Security), Swapnali (QA)  
**Work item:** `SUG-20260907-gdv`  
**Status:** Requirements for **bank DevOps to create from scratch**. This is not an instruction for the application team to click GitLab, AWS, Terraform or Terragrunt.  
**Shareable copy for DevOps (Word):** [`AU-SFB-NIP-GitLab-DevOps-Work-Order.docx`](./AU-SFB-NIP-GitLab-DevOps-Work-Order.docx) — regenerate with `python3 scripts/platform/generate-gitlab-devops-work-order.py`  
**Authority:** GitLab is the bank enterprise delivery standard ([ARB dossier §9.1](../../architecture/ARB-ARCHITECTURE-DOSSIER.md); [ADR-016](../../governance/registers/DECISION-REGISTER.md)). Group mapping: [GITLAB-REPO-STRUCTURE.md](./GITLAB-REPO-STRUCTURE.md). Service catalogue: [backend-service-catalog.yaml](./backend-service-catalog.yaml).

---

## 0. How to use this document

Give this file to the assigned bank DevOps engineer. It is the complete create-from-scratch request:

| They need to know | Section |
|---|---|
| Who may touch what | [§1](#1-operating-model--who-does-what) |
| Group / subgroup / project tree | [§3](#3-gitlab-group-hierarchy) |
| Backend vs frontend vs governance vs infra | [§4](#4-what-to-create-now-wave-0) |
| What a new repository must look like | [§7](#7-new-project-template--every-new-repo) |
| Roles and access | [§8](#8-users-groups-and-gitlab-roles) |
| CI the application team will use | [§9](#9-ci--what-the-application-team-needs) |
| CD / Terraform / AWS that only DevOps builds | [§10](#10-cd-and-iac--devops-only) |
| Security, SAST, SCA, coverage | [§11](#11-security-static-analysis-and-coverage) |
| Done when | [§14](#14-acceptance--devops-is-done-when) |

**Do not copy any existing unofficial repository.** Create empty, bank-owned projects on the bank GitLab, then grant the application team Developer access on application projects only. The application team will commit source after the projects exist.

---

## 1. Operating model — who does what

AU Bank policy for this programme:

| Surface | Bank DevOps | Application team (dev / tech / SRE-as-requirements) |
|---|---|---|
| Create GitLab groups, subgroups, projects | **Does** | Does **not** |
| GitLab users, roles, protected branches, runners | **Does** | Does **not** |
| Terragrunt / Terraform / AWS accounts / networking | **Does** | Does **not** — raise a requirement, never apply |
| CD (deploy to `dev` / `sit` / `uat` / `prod` / `dr`) | **Does** | Does **not** — consume promoted artefacts only |
| Application source (Java, Flutter, tests) | Empty project + permissions | **Commits** after the repo exists |
| CI job definitions for build / test / coverage / SAST hooks | Owns **reusable templates** | **Includes** templates; may extend **allowed** build/test jobs only |
| Secrets for deploy and cloud credentials | **Owns** in the approved secret store | Never in git; never in application CI variables except non-secret config |
| Container / package registries | **Provisions** | Push via CI using job identity, not personal keys |

**Hard rule for Loc of CD:** application repositories must not contain Terraform, Terragrunt, AWS account modules, or a job that applies infrastructure. Those live only under the `infra/` subgroup. A pipeline that can `terraform apply` from an application project is a defect.

**Hard rule for GitLab/AWS:** the application team will not be GitLab Owners, will not be AWS console operators, and will not be given Terraform state access. They receive Developer (or equivalent) on application projects and Reporter on infra.

---

## 2. Repository model we are following

Two layers, both required. Do not skip Wave 0.

### 2.1 Wave 0 — buildable now (GATE-S08)

One **backend Gradle monorepo**, one **Flutter application repo**, one **governance/docs repo**, one **CI templates** project, and an **infra** subgroup that only DevOps fills.

Why a backend monorepo first: shared libraries (`libs/bank-common-*`) are compiled with the services. Splitting every microservice into its own GitLab project before a package registry exists will break the build. The architecture still **names** every future per-service project so DevOps can create empty shells or postpone extract until Wave 1.

### 2.2 Wave 1 — multi-repo extract (after package registry)

Each bounded context becomes its own GitLab project under the groups in [GITLAB-REPO-STRUCTURE.md](./GITLAB-REPO-STRUCTURE.md). Shared libraries publish to the GitLab Package Registry (Maven). Services depend on released coordinates, not on a sibling folder.

**Join key** between architecture context, Gradle module, port and GitLab group: [`backend-service-catalog.yaml`](./backend-service-catalog.yaml).

### 2.3 Layout the application team will commit (backend)

```text
nip-backend/                         (Wave 0 GitLab project)
├── libs/
│   ├── bank-common-error
│   ├── bank-common-domain
│   ├── bank-common-security
│   ├── bank-common-audit
│   ├── bank-common-observability
│   └── bank-common-secrets
├── services/                        (one Gradle module per bounded context)
├── templates/microservice-skeleton/ (how a new service is born)
├── scripts/                         (scaffold, quality helpers)
├── gradle/ + settings.gradle.kts
└── .gitlab-ci.yml                   (includes ci-templates; no Terraform)
```

Hexagonal packages inside each service: `api` · `application` · `domain` · `config`.  
Stack: **Java 21**, **Spring Boot**, **Gradle (Kotlin DSL)**.  
Invariants DevOps does not have to implement, but CI must keep enforceable:

- Bank apps never call 1SB or a database directly.
- 1SB types live only in `adapter.onesb.*` (ArchUnit).
- `1sb-integration-service` has no Flyway and no JPA.
- Persistence is platform-common (`bank-persistence-service`), reached over HTTP.
- No PII in logs.
- Flutter never receives OAuth tokens; the BFF holds them.

### 2.4 Layout the application team will commit (frontend)

One client, not several apps ([ADR-015](../../governance/registers/DECISION-REGISTER.md)): **NIP-APP** — Flutter for web + Android + iOS. RM, ISR, admin and operations are **roles in the same app**, not separate repositories.

```text
nip-app/                             (Wave 0 GitLab project)
├── lib/
├── test/
├── pubspec.yaml
└── .gitlab-ci.yml                   (Flutter analyze / test / coverage / web build)
```

The client talks **only** to the token-hiding BFF (`workforce-access-bff`). It never calls 1SB, never calls a database, never receives OAuth access or refresh tokens.

**Not now:** a second customer-facing Flutter app or Customer BFF — R1, after the assisted journey completes a real sale.

---

## 3. GitLab group hierarchy

Create this **exact** tree. If bank naming policy requires a parent (for example `au-sfb/`), place `au-bank-insurance-platform` under that parent and keep the relative paths below.

Visibility: **Internal** (or bank equivalent of “all bank staff with a role, never public”). No public projects. No public snippets. No public package registry.

```text
au-bank-insurance-platform/                    TOP-LEVEL GROUP
│
├── platform/                                  paved road (CI, docs, templates)
│   ├── ci-templates                           PROJECT  — reusable GitLab CI YAML
│   ├── nip-governance                         PROJECT  — AIGEM / architecture / SSOT docs
│   └── microservice-skeleton                  PROJECT  — optional; or keep skeleton inside nip-backend
│
├── backend/                                   Wave 0 application source
│   └── nip-backend                            PROJECT  — Gradle monorepo (libs + all services)
│
├── frontend/                                  Wave 0 client source
│   └── nip-app                                PROJECT  — Flutter NIP-APP (web / Android / iOS)
│
├── platform-common/                           Wave 1 extract targets (create group now)
│   ├── bank-common-error
│   ├── bank-common-domain
│   ├── bank-common-security
│   ├── bank-common-audit
│   ├── bank-common-observability
│   ├── bank-common-secrets
│   └── bank-persistence-service
│
├── ws2-iam/                                   workforce identity plane
│   ├── identity-provider-adapter-service
│   ├── identity-authorization-service
│   └── workforce-access-bff
│
├── ws3-domain/                                core sales & advisory
│   ├── customer-service                       (#4)
│   ├── lead-service                           (#5)
│   ├── consent-service                        (#6)
│   ├── suitability-service                    (#7)
│   ├── product-catalogue-service              (#8)
│   ├── journey-orchestration-service          (#9)
│   ├── quotation-service                      (#10)
│   ├── proposal-service                       (#11)
│   ├── payment-service                        (#12)
│   └── policy-issuance-service                (#13)
│
├── ws3-integration/                           provider connectivity
│   ├── integration-hub-service                (#14)
│   ├── 1sb-integration-service                (#15)
│   └── direct-insurer-adapter-service
│
├── ws3-platform/                              cross-cutting platform services
│   ├── audit-compliance-service               (#16)
│   ├── notification-service                   (#17)
│   ├── reporting-mis-service                  (#18)
│   └── administration-config-service          (#19)
│
├── ws3-edge/                                  channel BFF
│   └── customer-bff                           EMPTY — do not populate until R1
│
├── ws3-mobile/                                (optional alias group; Wave 0 project may live under frontend/)
│   └── (nip-app may be moved here later; do not create a second Flutter app)
│
└── infra/                                     DEVOPS ONLY — application team = Reporter
    ├── terraform-live                         Terragrunt / Terraform live (env folders)
    ├── terraform-modules                      reusable Terraform modules
    ├── gitlab-cd                              CD loc: promote image → deploy → smoke
    └── ansible-ops                            Ansible DR / sanity (bank standard)
```

**Wave 0 projects that must contain a default branch and CI on day one:**

1. `platform/ci-templates`
2. `platform/nip-governance`
3. `backend/nip-backend`
4. `frontend/nip-app`
5. `infra/terraform-live` (empty README + CODEOWNERS; DevOps fills)
6. `infra/terraform-modules`
7. `infra/gitlab-cd`
8. `infra/ansible-ops`

**Wave 1 projects:** create the empty projects under `platform-common/`, `ws2-iam/`, `ws3-*` **now** if bank process is “create the shell when the architecture names it”; otherwise create the **groups** now and add projects on extract. Either is acceptable. Do **not** initialize Wave 1 service projects with sample READMEs that look like they are live — they are extract targets.

`ws3-edge/customer-bff`: create the empty project only if empty projects are cheap; **do not** wire CI, deploy, or DNS. Customer BFF and customer Flutter are out of R0.

---

## 4. What to create now (Wave 0)

### 4.1 `platform/ci-templates`

**Purpose:** single paved-road CI. Application `.gitlab-ci.yml` files only `include:` this project.

Minimum template files (names can follow bank convention; the **jobs** are mandatory):

| Template file | Used by | Jobs |
|---|---|---|
| `/templates/java-monorepo.yml` | `nip-backend` | build, unit/component test, JaCoCo verify, ArchUnit summary, artefact publish |
| `/templates/java-library.yml` | Wave 1 `bank-common-*` | build, test, coverage, publish Maven package |
| `/templates/java-service.yml` | Wave 1 services | build, test, coverage, container build, image scan, SBOM, push digest |
| `/templates/flutter-app.yml` | `nip-app` | `flutter analyze`, `flutter test`, coverage, `flutter build web` |
| `/templates/governance-docs.yml` | `nip-governance` | JDK 21 freshness check, Python schema/link checks |
| `/templates/security-sast.yml` | all application projects | secret scan, SAST, SCA, SBOM (see §11) |
| `/templates/container-scan.yml` | any image-producing pipeline | image scan **before** registry publish |
| `/templates/iac-scan.yml` | `infra/*` only | tfsec / Checkov (or bank equivalent); never included from application repos |

Rules inside the templates:

- `interruptible: true` on MR pipelines (supersede in-flight runs for the same ref).
- Default `GIT_DEPTH` full (`0`) on secret-scan jobs.
- No `allow_failure: true` on gate jobs.
- Application templates **must not** define `terraform`, `terragrunt`, `aws`, or `kubectl apply` deploy jobs.

### 4.2 `backend/nip-backend`

**Purpose:** all Java/Spring services and shared libraries until Wave 1 extract.

| Setting | Value |
|---|---|
| Default branch | `main` |
| README | Short: Java 21, Gradle wrapper, how to run `./gradlew test` |
| `.gitignore` | Gradle, IDE, `.env`, secrets, `build/` |
| Merge method | Merge commit or bank standard; **no** commit-without-MR to `main` |
| Package Registry | Maven (for Wave 1 extract) — enable now |
| Container Registry | Enable now (service images tagged with **git SHA**, never `latest` as a promotion mechanism) |
| CI/CD | Include `java-monorepo.yml` + `security-sast.yml` |
| Wiki / issues | Bank standard; application team uses MRs as the change record |

Local ports the catalogue already reserved (do not collide in deploy manifests later): `8080`–`8084` implemented services; `8090`–`8105` WS-3 skeletons. Canonical table: [`backend-service-catalog.yaml`](./backend-service-catalog.yaml).

### 4.3 `frontend/nip-app`

**Purpose:** the one Flutter client (NIP-APP).

| Setting | Value |
|---|---|
| Default branch | `main` |
| CI | Include `flutter-app.yml` + `security-sast.yml` (Dart/Flutter SAST pack + secret scan + SCA on `pubspec.lock`) |
| Coverage | `flutter test --coverage`; fail the job if coverage **drops** below the baseline Swapnali sets; initial floor to be confirmed by QA (do not invent a Flutter percentage in this request) |
| Store builds | **Not** a Developer self-serve. Signed IPA/APK and Play/App Store upload are CD + Security (Deepali owns store hardening). Wave 0 CI builds **web** and unsigned artefacts only |

### 4.4 `platform/nip-governance`

**Purpose:** programme documentation and the AIGEM operating model — lifecycle, registers, architecture, business SSOT. This is a first-class repository, not a wiki.

Typical paths the application team will commit:

```text
docs/governance/          binding process (AIGEM)
docs/context/             agent routing, BOOT, personas
docs/platform/            cross-service architecture
docs/architecture/        HLD / LLD / ARB
docs/au-bank-insurance-platform/   business SSOT
docs/application-lifecycle-bible/  stage gates
scripts/governance/       freshness + CI checks
scripts/context/          context-load, DOC-MAP
```

CI: JDK 21 + Python 3.12. Jobs equivalent to: run `FreshnessCheck`, `scripts/governance/ci-checks.py`, `scripts/context/validate-context.py`. Merge to `main` blocked on red.

If bank DevOps prefer **one** application repo that also holds `docs/`, that is acceptable **only** if governance CI still runs on every MR that touches `docs/**` or `scripts/governance/**` and still blocks `main`. A dedicated project is preferred so documentation reviewers are not drowned in Java CI.

### 4.5 `infra/*` (create empty, DevOps fills)

These projects are the **Loc of CD**. Application engineers: **Reporter**. No Developer. No Maintainer.

| Project | Contents (DevOps) |
|---|---|
| `terraform-live` | Terragrunt live: `dev/`, `sit/`, `uat/`, `prod/`, `dr/`; AWS India regions only (`ap-south-1` primary; DR `ap-south-2` when S09 says so) |
| `terraform-modules` | Reusable modules; no live state |
| `gitlab-cd` | Pipelines that **promote an immutable image digest** from the registry into an environment, run smoke, record release evidence |
| `ansible-ops` | Bank-standard Ansible for DR drills and post-deploy sanity (ARB §9.2) |

CD input is an **image digest** produced by application CI, never “rebuild in the environment”. Rebuilding per environment breaks traceability (`S09-E02-S02` in the LLD).

---

## 5. Backend inventory (so you create the right shells)

Status values are for DevOps sizing, not for skipping CI: **implemented** modules already have production-shaped code; **skeleton** modules must still compile, test, and pass ArchUnit in the Wave 0 monorepo.

| GitLab group (Wave 1) | Project | Port | Datastore (runtime, not in git) | Status |
|---|---|---|---|---|
| `platform-common` | `bank-common-error` | — | — | implemented lib |
| `platform-common` | `bank-common-domain` | — | — | implemented lib |
| `platform-common` | `bank-common-security` | — | — | implemented lib |
| `platform-common` | `bank-common-audit` | — | — | implemented lib |
| `platform-common` | `bank-common-observability` | — | — | implemented lib |
| `platform-common` | `bank-common-secrets` | — | — | implemented lib |
| `platform-common` | `bank-persistence-service` | 8081 | Aurora PostgreSQL | implemented |
| `ws2-iam` | `identity-provider-adapter-service` | 8082 | none | implemented |
| `ws2-iam` | `identity-authorization-service` | 8083 | Aurora PostgreSQL | implemented |
| `ws2-iam` | `workforce-access-bff` | 8084 | none (session in cache) | implemented |
| `ws3-domain` | `customer-service` | 8090 | Aurora PostgreSQL | skeleton |
| `ws3-domain` | `lead-service` | 8091 | Aurora PostgreSQL | skeleton |
| `ws3-domain` | `consent-service` | 8092 | Aurora PostgreSQL (append-only) | skeleton |
| `ws3-domain` | `suitability-service` | 8093 | Aurora PostgreSQL | skeleton |
| `ws3-domain` | `product-catalogue-service` | 8094 | Aurora + Redis read cache | skeleton |
| `ws3-domain` | `journey-orchestration-service` | 8095 | DynamoDB state | skeleton |
| `ws3-domain` | `quotation-service` | 8096 | DynamoDB + Redis | skeleton |
| `ws3-domain` | `proposal-service` | 8097 | Aurora PostgreSQL | skeleton |
| `ws3-domain` | `payment-service` | 8098 | Aurora PostgreSQL | skeleton |
| `ws3-domain` | `policy-issuance-service` | 8099 | Aurora + S3 (PDFs) | skeleton |
| `ws3-integration` | `integration-hub-service` | 8100 | DynamoDB routing | skeleton |
| `ws3-integration` | `1sb-integration-service` | 8080 | job store via persistence HTTP | implemented |
| `ws3-integration` | `direct-insurer-adapter-service` | 8105 | none | skeleton |
| `ws3-platform` | `audit-compliance-service` | 8101 | DynamoDB + S3 archive | skeleton |
| `ws3-platform` | `notification-service` | 8102 | DynamoDB delivery log | skeleton |
| `ws3-platform` | `reporting-mis-service` | 8103 | warehouse over lake | skeleton |
| `ws3-platform` | `administration-config-service` | 8104 | Aurora PostgreSQL | skeleton |

Canonical group for every row: the `gitlab_group` field in [`backend-service-catalog.yaml`](./backend-service-catalog.yaml). If a ticket and the catalogue disagree, the catalogue wins.

---

## 6. Frontend inventory

| GitLab project | What it is | R0? |
|---|---|---|
| `frontend/nip-app` | One Flutter NIP-APP (web + later store binaries) | **Yes** — assisted Term journey UI |
| Customer Flutter / Customer BFF | DIY customer channel | **No** — R1 |
| Separate admin or ops app | Rejected | **Never** — roles inside NIP-APP |

Distribution later (not Wave 0 CI): EKS-hosted web + Play Store APK + App Store IPA. MDM-only is not the default.

---

## 7. New project template — every new repo

When Architecture names a new module (scaffold script + catalogue row), DevOps creates the GitLab project from this checklist. Do not wait for the application team to “open GitLab and click New project”.

### 7.1 Create

1. Place the project in the **catalogue GitLab group** (`gitlab_group` in [`backend-service-catalog.yaml`](./backend-service-catalog.yaml)).
2. Project name = Gradle module name (`consent-service`, not `Consent Service`).
3. Default branch `main`. Initialize with a README **or** leave empty for the first MR — either is fine.
4. Visibility Internal. Disable: public forks, public packages, container cleanup that deletes SHA tags under 90 days.
5. Enable: Merge requests, CI/CD, Container Registry (services), Package Registry (libs), Secure (secret detection, SAST, dependency scanning, container scanning) if the bank licence includes GitLab Ultimate/equivalent.
6. Assign the shared **project template** / description that points at `platform/ci-templates`.

### 7.2 First commit DevOps may put (optional)

A one-file `.gitlab-ci.yml` that only includes the right template. No application source.

```yaml
include:
  - project: 'au-bank-insurance-platform/platform/ci-templates'
    file: '/templates/java-service.yml'
    ref: main
  - project: 'au-bank-insurance-platform/platform/ci-templates'
    file: '/templates/security-sast.yml'
    ref: main
```

Library projects include `java-library.yml` instead of `java-service.yml`. Flutter includes `flutter-app.yml`. Governance includes `governance-docs.yml`. Infra includes `iac-scan.yml` plus the CD loc — **never** `java-service.yml`.

### 7.3 Protect `main`

On `main` (and on `uat` / `prod` branches if you use them):

- No direct push. Merge request required.
- At least **one** approving review. CODEOWNERS for `*`.
- Dismiss stale approvals on new commits.
- Pipelines must succeed. Required jobs — **exact names DevOps should pin as required**:

  **Backend / Java**

  - `java:test-and-coverage`
  - `java:archunit`
  - `security:secret-scan`
  - `security:sast`
  - `security:sca`
  - `security:sbom` (must contain Maven components; a Dart-only SBOM on a Java repo is a failed job)

  **Frontend / Flutter**

  - `flutter:analyze`
  - `flutter:test`
  - `security:secret-scan`
  - `security:sast`
  - `security:sca`

  **Governance**

  - `governance:freshness`
  - `governance:ci-checks`

- Branches must be up to date before merge (or bank equivalent of “no behind-main merges”).
- No force-push, no branch deletion on `main`.
- Maintainers on application projects still cannot skip the pipeline. If GitLab has “Merge when pipeline succeeds” / “Pipelines must succeed”, turn it on. A skipped required job must **not** count as passed (GATE-S08-G2).

### 7.4 CODEOWNERS (application team will maintain content; DevOps seeds the file)

| Path | Approvers (GitLab groups you create) |
|---|---|
| `*` | `@au-bank-insurance-platform/engineering-reviewers` |
| `/infra/` or any `**/*.tf` | `@au-bank-insurance-platform/devops-only` — and these paths must **not** exist in application repos |
| `**/adapter/onesb/**` | engineering + architecture |
| `**/src/main/**/persistence/**` | engineering + database |
| `docs/governance/**` | architecture + delivery |

### 7.5 What the application team then does

They open a merge request with source, tests, and (if a new Java service) the scaffold from `templates/microservice-skeleton/` (`S08-E06-S03`). They do not ask DevOps to paste business code.

---

## 8. Users, groups and GitLab roles

Create **GitLab groups for people** (separate from the project tree). Map bank AD / SSO identities; no shared users; no personal access tokens stored in chat.

### 8.1 Access groups

| GitLab access group | Members | Role on application projects | Role on `platform/ci-templates` | Role on `infra/*` |
|---|---|---|---|---|
| `nip-devops-admins` | Bank DevOps / GitLab admins | Maintainer | Owner | Owner |
| `nip-sre-platform` | Named SRE (Shivanshi lane) + bank platform | Developer | Maintainer (MR to templates) | Reporter |
| `nip-engineering` | Application developers (Amit lane) | Developer | Developer (MR only) | Reporter |
| `nip-tech-leads` | Named tech leads | Maintainer **if** bank allows; else Developer + CODEOWNERS | Developer | Reporter |
| `nip-architecture` | Mahesh lane | Developer | Reporter | Reporter |
| `nip-security` | Deepali lane | Developer (to read findings) | Reporter | Reporter |
| `nip-qa` | Swapnali lane | Developer (to read reports) or Reporter + job artefacts | Reporter | Reporter |
| `nip-compliance` | Shailja lane | Reporter | Reporter | Reporter |
| `nip-product` | Rajal lane | Reporter | Reporter | None |
| `nip-dba` | Aarti lane | Developer on persistence module only | Reporter | Reporter |
| `nip-auditors` | Internal audit / SOC | Reporter (no clone of `prod` secret files — there must be none) | Reporter | Reporter |

**Nobody from the application team is Owner of the top-level group.**  
**Nobody from the application team is Maintainer of `infra/*`.**  
**Deploy tokens / project access tokens:** created by DevOps, rotated by DevOps, scoped to registry push from CI job identity.

### 8.2 Persona → GitLab (for the ticket)

These are programme roles, not extra GitLab accounts with those names. Map to the bank people sitting in the seats:

| Persona | Seat | GitLab access group |
|---|---|---|
| Rajal — Product | Board 3 · R1 | `nip-product` |
| Mahesh — Architecture | Board 1 · R2 | `nip-architecture` |
| Amit — Engineering | Board 2 · R3 | `nip-engineering` / `nip-tech-leads` |
| Deepali — Security | Board 4 · R8 | `nip-security` |
| Swapnali — QA | Board 5 · R7 | `nip-qa` |
| Shailja — Compliance | Board 6 · R9 | `nip-compliance` |
| Shivanshi — SRE | Board 7 · R10 | `nip-sre-platform` |
| Aarti — DBA | specialist | `nip-dba` |
| Kalpana — Delivery | R12 | Reporter on all (delivery visibility) |

### 8.3 Protected environments (CD)

| Environment | Who can deploy (GitLab environment protection) | Approval |
|---|---|---|
| `dev` | `nip-devops-admins` (+ optional `nip-sre-platform`) | none or one DevOps |
| `sit` | `nip-devops-admins` | one DevOps |
| `uat` | `nip-devops-admins` | DevOps + named tech lead |
| `prod` | `nip-devops-admins` only | DevOps + change record; Security may block |
| `dr` | `nip-devops-admins` only | same as prod |

Application `Developer` role must **not** include “Deploy to production”.

---

## 9. CI — what the application team needs

This is the application team’s contract. DevOps implements it as templates + runners + required checks. The application team writes code and keeps the include file green.

### 9.1 Baseline pipeline (Java)

From the SRE paved road and GATE-S08:

```text
commit / merge request
  → secret scan                         (blocks)
  → SAST                                (blocks on new critical / high)
  → SCA / dependency scan               (blocks on critical / high with a fix available)
  → compile (Java 21)
  → unit + component tests
  → JaCoCo verification                 (blocks below threshold)
  → ArchUnit                            (blocks)
  → static quality (Spotless/Checkstyle or Sonar quality gate — §11)
  → assemble artefacts
  → SBOM (CycloneDX)                    (must list Maven coordinates)
  → container build (services)
  → image scan                          (blocks critical)
  → publish immutable artefact (SHA / digest)
```

Deploy, Terraform, smoke in AWS, DAST against a live URL: **not this pipeline**. Those are §10.

### 9.2 Commands the Java template must run

```bash
./gradlew --no-daemon build test jacocoTestReport jacocoTestCoverageVerification
```

`./gradlew test` alone is **not** the gate: it writes reports and does not fail on coverage. CI must run `jacocoTestCoverageVerification` (or `check` / `build` wired to it).

Report paths to keep as artefacts (14 days tests, 30 days security, 90 days SBOM):

- `**/build/reports/tests/**`
- `**/build/reports/jacoco/test/html/index.html`
- `**/build/reports/jacoco/test/jacocoTestReport.xml`

### 9.3 Flutter template

```bash
flutter pub get
flutter analyze
flutter test --coverage
flutter build web
```

Fail `analyze` on errors. Treat `info`/`warning` policy as Swapnali + Amit; do not `allow_failure`.

### 9.4 Governance template

```bash
java scripts/governance/FreshnessCheck.java
python3 scripts/governance/ci-checks.py
python3 scripts/context/validate-context.py
```

Freshness exit `1` = warning (do not fail). Exit `≥ 2` = fail the pipeline.

### 9.5 Performance of CI (GATE-S08-G9)

- p95 merge-request feedback **under 10 minutes**.
- Flake **under 1%**.
- Use dependency cache (Gradle, Pub), parallel jobs, and `interruptible` MR pipelines.
- If the bank runner pool cannot meet 10 minutes, that is a DevOps capacity defect, not an excuse to drop gates.

### 9.6 Branching

Trunk-based: short-lived branches, MR to `main`. No long-lived `develop` unless bank policy mandates it; if it does, `develop` has the **same** required jobs as `main`.

---

## 10. CD and IaC — DevOps only

The application team will **not** interact with Terragrunt, Terraform, AWS consoles, or GitLab CD loc. They will hand you:

- Image **digest** + SBOM + scan report from CI.
- Release notes / work item IDs.
- Environment config **keys** (never secret values) that the service reads.

You build:

1. AWS accounts / VPCs / EKS / data stores per the R0 LLD (S09). India regions only. No regulated data outside AWS India.
2. Terragrunt live + Terraform modules in `infra/`.
3. `gitlab-cd` promotion: `dev` → `sit` → `uat` → `prod`.
4. Ansible for DR / sanity (ARB §9.2).
5. CloudTrail (who changed AWS) **and** CloudWatch (how the service runs). Both are mandatory. Neither replaces the other.
6. IaC scan on every infra MR (tfsec/Checkov or bank equivalent). Public exposure, unencrypted store, over-broad IAM = fail.

**GitOps note:** enterprise diagrams use **GitLab CI/CD**, not Argo CD. Do not introduce a second CD control plane.

**Render.com / any public PaaS:** not a data path for PII or production-like data. Do not create those integrations.

---

## 11. Security, static analysis and coverage

Owners: Deepali (security property) · Swapnali (evidence that tests ran) · Amit (implementation) · Shivanshi (pipeline mechanics).

### 11.1 Secret scanning — every commit, every project

| Requirement | Detail |
|---|---|
| Tool | GitLab Secret Detection **and/or** gitleaks (pinned version). Bank-standard equivalent is acceptable if it fails the job |
| Working tree | **Blocks** the MR if a credential is present |
| History | Scheduled (weekly) full-history scan. A historical finding is **rotate**, not “delete the line”. Deleting a line does not uncompromise a key |
| Config | Allowlist only via a reviewed config file; no global disable |
| Response | Assume compromise; rotate; record. Owner: Deepali with SRE |

S08 validation: a merge request that contains a realistic test credential **must not merge**.

### 11.2 SAST — static application security testing

| Requirement | Detail |
|---|---|
| Java | Bank **SonarQube** (ARB §9.1) **or** GitLab SAST / Semgrep. Quality gate **fails** the pipeline |
| Flutter / Dart | SonarQube Dart plugin or GitLab SAST equivalent |
| Fail on | **New** critical or high findings on the MR. Existing findings go to the risk register with Deepali’s `S0`–`S3` SLA — they do not silently `allow_failure` |
| Query depth | Security-extended / equivalent high-recall pack for a regulated financial app |

Severity SLA ([`07-SECURITY-COMPLIANCE-CANON.md` §4](../../application-lifecycle-bible/07-SECURITY-COMPLIANCE-CANON.md)):

| S | Meaning | Window |
|---|---|---|
| S0 | Critical, non-bypassable | Immediate; blocks release |
| S1 | High | Before the next release |
| S2 | Medium | Within two releases |
| S3 | Low / hardening | Backlog |

### 11.3 Static analysis (engineering quality, not only CVE)

GATE-S08-G4: ArchUnit **and** static analysis.

| Check | Tool class | Fail the build when |
|---|---|---|
| Architecture boundaries | ArchUnit (in the Java test job) | Any violation (1SB types outside `adapter.onesb.*`, persistence in the adapter, etc.) |
| Formatting / style | Spotless or Checkstyle (Amit publishes the standard) | New violations |
| Code quality | **SonarQube** quality gate (bank standard) | Gate red: new bugs, new vulnerabilities, coverage on new code below the Sonar gate, duplicated new blocks above the gate |
| Flutter | `dart analyze` / `flutter analyze` | Errors |

SonarQube does **not** replace JaCoCo thresholds in Gradle. Both run. JaCoCo is the enforceable module floor; Sonar is the new-code quality gate.

### 11.4 SCA — software composition analysis

| Requirement | Detail |
|---|---|
| Tool | GitLab Dependency Scanning, Trivy, or bank-approved SCA. Must resolve **transitive** Java dependencies (lockfiles / built jars), not only `build.gradle.kts` text |
| Fail on | CRITICAL and HIGH with a reachable/fixable path (`--ignore-unfixed` is acceptable if the unfixed set is exported to the risk register) |
| Flutter | Scan `pubspec.lock` |
| Schedule | Every MR **and** weekly (a clean merge does not stay clean) |
| S08 validation | Introducing a dependency with a known critical CVE **must** fail the pipeline |

### 11.5 Container image scanning

Every image **before** it is published to the bank registry. Critical OS or library CVE blocks publish. Rebuild when remediation requires it. Non-root, minimal/trusted base, no privileged mode (Deepali container baseline).

### 11.6 SBOM

CycloneDX (or bank SBOM format) stored with the artefact, **90-day** minimum retention in GitLab artefacts **and** a copy next to the image in the registry if the bank has that pattern.

A Java pipeline whose SBOM contains **zero** Maven coordinates has failed. Licence policy: disallowed licences fail the build (Shailja + Legal list; if the list is not yet issued, generate the SBOM anyway and do not invent a licence fail set).

### 11.7 DAST

Nightly against `dev` once an environment exists (S09). Not a merge-request gate in Wave 0. New high findings fail the nightly and open a Security ticket. Application team does not configure DAST scanners.

### 11.8 IaC scanning

Only on `infra/*`. Application CI must not skip this by embedding IaC.

### 11.9 Coverage (QA-001) — fail the build, do not ticket

Authority: [`COVERAGE.md`](../../1sb-insurance-integration/service-ssot/COVERAGE.md).

| Module group | Line | Branch |
|---|---|---|
| `libs/*` | **≥ 80%** | **≥ 70%** |
| `services:1sb-integration-service` | **≥ 90%** | **≥ 70%** |
| Other `services/*` | **≥ 50%** line (interim floor) | not gated yet |
| Compliance-gate code (control paths C1–C10) | **100% branch** when those packages exist | no waiver |

Do **not** lower lib gates without Tech Lead + QA Lead co-approval and a TECH-DEBT id with expiry.

Exclusions allowed in JaCoCo: `*Application`, `package-info`, `*Config`, `*Configuration`, `*Properties`.

PR annotation of coverage XML is desirable (QA-005) but not a substitute for the failing Gradle task.

### 11.10 PII in logs

A test job (or a step after tests) must scan emitted logs for PAN, Aadhaar, phone, email and health patterns and **fail** on a match (GATE-S08-G7). Do not log OTPs, tokens, passwords, raw KYC payloads.

---

## 12. Runners, variables and registries

| Item | Request |
|---|---|
| Runners | Linux, Docker-in-Docker or Kaniko for image build, JDK 21 image, Flutter stable matching `sdk: ^3.5.4`, Python 3.12 for governance. Autoscaling if that is the bank GitLab pattern |
| Tags | `nip-java`, `nip-flutter`, `nip-docs`, `nip-docker` so jobs land on the right pool |
| Cache | Gradle (`~/.gradle/caches`), Pub, Maven local |
| Variables in application projects | Non-secret only (`SPRING_PROFILES_ACTIVE` for test, Sonar host URL). **No** AWS keys, **no** DB passwords, **no** 1SB credentials |
| Runtime secrets | AWS Secrets Manager (or bank equivalent), injected at deploy time by CD. Rotation is DevOps + Security |
| Registry | GitLab Container Registry **or** ECR. Promote **digest**. Scan-on-push |
| Maven | GitLab Package Registry for `bank-common-*` |

---

## 13. What not to create

| Request we are **not** making | Why |
|---|---|
| Public GitLab group or public package | Regulated financial application |
| Application-team AWS console users | Bank policy — requirements only |
| Terraform in `nip-backend` | CD loc belongs in `infra/` |
| Argo CD | Bank standard is GitLab CI/CD |
| Second admin/ops Flutter app | ADR-015 — roles in NIP-APP |
| Customer BFF CI/CD | R1 |
| Kafka / second audit database / Flyway inside `1sb-integration-service` | Standing constraints |
| Environments that store PII outside AWS India | Residency |
| Render.com (or similar) connected to bank data | Dev-preview only, never a PII path |
| Shared `admin` / `root` GitLab user for the application team | Named SSO identities |

---

## 14. Acceptance — DevOps is done when

Wave 0 is accepted when **all** of the following are true. Screenshots or exported GitLab settings attached to the handover ticket.

1. Top-level group `au-bank-insurance-platform` exists, Internal, with the subgroup tree in §3.
2. Wave 0 projects in §4 exist with `main` protected as in §7.3.
3. Access groups in §8 exist; a named application engineer can clone `nip-backend` and **cannot** push to `infra/terraform-live`.
4. `ci-templates` includes the files in §4.1; a sample MR on `nip-backend` that **fails a unit test** cannot merge (S08-VT-01).
5. Required pipeline names are pinned on `main` so a skipped job is not a green merge (S08-G2).
6. Secret scan, SAST, SCA, SBOM run on Java and Flutter; image scan is wired for any Dockerfile publish; IaC scan runs on `infra`.
7. JaCoCo verification is in the Java template with the thresholds in §11.9.
8. Package + container registries exist; CD promotes **digests**.
9. Application projects contain **no** Terraform and **no** AWS credentials.
10. The application team has Developer on application projects and a written “how to open the first MR” note (clone URL, runner tags, include path). They still do not have Terraform or AWS.

Until item 4 is demonstrated, GATE-S08 remains open regardless of how complete the group tree looks.

---

## 15. After Wave 0 — extract (do not do this on day one)

1. Publish `bank-common-*` to the Maven registry from `nip-backend`.
2. For each service, create/activate the Wave 1 project in its group.
3. Switch that service’s pipeline to `java-service.yml`.
4. Leave `nip-backend` as the umbrella until the last extract, or convert it to a documentation/aggregator project.
5. Split deploy pipelines by group once S09 platform foundation is green ([GITLAB-REPO-STRUCTURE.md](./GITLAB-REPO-STRUCTURE.md)).

---

## 16. References (approved facts this request is built from)

- [GITLAB-REPO-STRUCTURE.md](./GITLAB-REPO-STRUCTURE.md) — group mapping  
- [backend-service-catalog.yaml](./backend-service-catalog.yaml) — context → module → port → group  
- [MICROSERVICE-SCAFFOLD-TEMPLATE.md](./MICROSERVICE-SCAFFOLD-TEMPLATE.md) — how a new Java service is born  
- [ARB dossier §9](../../architecture/ARB-ARCHITECTURE-DOSSIER.md) — GitLab CI/CD, Terraform, SonarQube, CloudTrail + CloudWatch  
- [S08 — Engineering Foundation](../../application-lifecycle-bible/stages/S08-engineering-foundation.md) — gates G1–G10  
- [Security & Compliance Canon §4](../../application-lifecycle-bible/07-SECURITY-COMPLIANCE-CANON.md) — pipeline security gates  
- [COVERAGE.md](../../1sb-insurance-integration/service-ssot/COVERAGE.md) — JaCoCo floors  
- [TESTING-RULES.md](../../1sb-insurance-integration/service-ssot/TESTING-RULES.md) — test pyramid  
- Shivanshi CI baseline — `docs/context/roles/shivanshi-sre/04-platform-infrastructure-and-cicd.md` §6–§7  
- Deepali DevSecOps flow — `docs/context/roles/deepali-principal-security-architect/06-application-api-and-devsecops-security.md` §10  
- Board 7 checks O1–O8 — `docs/governance/11-REVIEW_GATES.md` §10
