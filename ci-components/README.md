# `ci-components` — versioned reusable GitLab CI/CD components

**Status:** `M7.1–M7.8 BUILT, UNEXECUTED` · **Authorised by:** [`CR-014`](../docs/governance/change-requests/CR-014-gitlab-estate-migration.md)
**Plan:** [`GLM-001`](../docs/platform/gitlab-migration/GLM-001-migration-plan.md) Phase M7

> ### Nothing here has run
> No GitLab instance was reachable from the authoring environment. These 16 components
> are written, ported faithfully from the three GitHub Actions workflows, and checked by
> `tests/validate-components.py` — which validates **structure**, not behaviour. First
> execution against `gitlab-ce.au.bank.in` is the real test.

Baseline §3.7 and §8.4: application repositories consume versioned components instead of
copying large YAML blocks. **Do not paste hundreds of lines into every repository.**

---

## Using a component

```yaml
include:
  - component: $CI_SERVER_FQDN/insurance/bank-insurance/engineering/ci-components/java-test@1.0.0
    inputs:
      runner_tag: java
```

Always pin a version. `@main` is a floating dependency on someone else's next commit.

## The 16 components

| Component | Purpose | Gates |
|---|---|---|
| `java-build` | Gradle assemble, cached wrapper | — |
| **`java-test`** | Tests, JaCoCo, **and the three `C-ENG-2` assertions** | `S08-G1`, `G3`, `G4`, `G7` |
| `secret-detection` | gitleaks tree scan (blocking) + history scan | `S08-G5` |
| `sast` | **Semgrep — a re-implementation, not a port** | `S08-G5` |
| `dependency-scan` | Trivy fs, CRITICAL+HIGH block | `S08-G5` |
| `container-scan` | Trivy image | `S08-G5` |
| `sbom` | CycloneDX, with the Java-coverage assertion | `S08-G5` |
| `docker-build` | Kaniko, tagged by commit SHA, emits the digest | — |
| `flutter-build` | analyze + test | frontend |
| `node-build` / `node-test` | language diversity per §3.7 | — |
| `contract-validate` / `contract-compatibility` | OpenAPI/AsyncAPI lint and **breaking-change detection** | §8.3 |
| `terraform-plan` | fmt, validate, plan, destructive-change guard | §11.2 |
| `terraform-apply` | **manual, default-branch only, restricted runner** | §11.1 |
| `gitops-promotion` | **promotes a digest, refuses a tag** | §3.6 |
| **`governance-merge-gate`** | CE compensating control (`CR-016` Option B): required `/approve` notes | not §6.3 |

---

## Four things the port changed, deliberately

### 1. `sast` is a re-implementation (`IMP-3`)

CodeQL is GitHub-only. This uses Semgrep, a different engine with different rules and
severities. **The finding set will differ.**

`C-SEC-3` requires a **differential run** — CodeQL and GitLab SAST on the same commit,
delta recorded, every CodeQL-only finding named and owned — **before cutover**. Until that
happens, a green `sast` job does not mean what a green CodeQL job meant, and `S08-G5` is
not satisfied by this component alone.

### 2. The blocking decision lives in the job, not the platform (`CR-016`)

GitLab CE has no Security Dashboard, no MR vulnerability widget and no scan-result policy
engine. Free/CE analyzers emit JSON artefacts only.

So `sast` reads its own report and exits non-zero. That is a **compensating control**: it
blocks the pipeline, it does not block the merge through a platform policy, and it
produces no dashboard. Do not record §6.3-style security approval as satisfied by it.

### 3. `java-test` asserts its own mechanisms (`C-ENG-2`)

Three gate mechanisms fail **silently and green** if a component drops them — coverage
verification, ArchUnit, and the no-PII-in-logs test. Each is asserted present by a check
that fails when the mechanism is absent. Running the task is not the same as proving it ran.

### 4. `gitops-promotion` refuses a tag

Baseline §3.6: production promotes the exact immutable artefact that passed lower
environments, and must not rebuild. So the component takes a **digest** and rejects
anything that is not `sha256:…`. Promoting by tag is how a rebuild reaches production
while still looking like a promotion.

---

## The backend affected-component design (`C-ENG-3`)

`GLM-001` M7.9 introduces affected-component execution so a one-service change does not
rebuild 20–40 services. **That is the same mechanism Amit removed from the GitHub
workflow**, and his comment there explains why: *a required status check skipped by a path
filter never reports a conclusion*, so the gate either blocks forever or passes vacuously.

GitLab's `rules:` behave the same way. The resolution is not to abandon affected-component
execution — it is to keep the **gating jobs outside the affected-set logic**:

```text
gating jobs        ALWAYS run, ALWAYS report
                   build-signal · java-test · secret-detection · sast · dependency-scan
                        |
                        +-- the MATRIX inside them narrows to the affected services
```

The gate is a fixed set of always-reporting jobs; the work underneath is dynamic.
`C-ENG-4` additionally requires a nightly full build, because affected-component detection
has a real blind spot: a change in `libs/bank-common-*` affects consumers the diff does not
name, and detection tuned for speed will eventually get that wrong.

**M7.9 is not built here** — it belongs in `backend`'s own `.gitlab-ci.yml`, not in a
shared component, and it needs the runner model (`M1.5`) to size the matrix.

---

## Versioning

Release by semver tag (`v1.0.0`); consumers pin. The catalog requires a tagged release,
which `.gitlab-ci.yml` produces.

## Testing

```bash
python3 tests/validate-components.py
```

Checks structure: every component parses as spec + body, every gating job carries
`allow_failure: false`, every artefact sets an explicit expiry (`C-CMP-3` — retention is a
decision, not a default), `terraform-apply` is manual-and-main-only with no decorative
environment block, and `java-test` asserts all three `C-ENG-2` mechanisms.

**It does not check behaviour.** No component has been executed.
