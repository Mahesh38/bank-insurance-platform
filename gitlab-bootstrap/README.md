# `gitlab-bootstrap` — Terraform for the Bank Insurance GitLab estate

**Status:** `M3 PARTIAL` — built under `R1` of [`DEC-20260829-02`](../docs/governance/DEC-20260829-02-m3-readiness-board-pack.md), 2026-08-29.
**Authorised by:** [`CR-014`](../docs/governance/change-requests/CR-014-gitlab-estate-migration.md) `APPROVED_WITH_CONDITIONS` · [`CR-017`](../docs/governance/change-requests/CR-017-orphan-import-and-file-workbench.md) (orphan import)
**Plan:** [`GLM-001`](../docs/platform/gitlab-migration/GLM-001-migration-plan.md) Phase M3; M5.2 is `scripts/migrate-repositories.sh` (orphan, not `filter-repo`)

> ### Nothing here has been executed, and it cannot be from the authoring environment
> No Terraform binary is installed and `registry.terraform.io` is unreachable through
> the egress proxy. **This configuration has never been `init`-ed, `fmt`-ed, `validate`-d
> or planned.** It is written, reviewed against the baseline, and unvalidated.
> `scripts/validate.sh` is the first thing to run on a host that has Terraform.

Covers the ten topics baseline §12.1 requires of this README.

---

## 1. Purpose and architecture

Provisions and governs `insurance/bank-insurance` as reproducible IaC. Adding a
project should be a **YAML change reviewed through a merge request**, followed by
`plan` and an approved `apply` — never manual GitLab administration.

```
insurance (id 820, PREREQUISITE — read, never created)
└── bank-insurance
    ├── product/      frontend · backend · contracts
    ├── delivery/     infrastructure · gitops
    ├── engineering/  ci-components
    └── governance/   security-policies · gitlab-bootstrap
                      platform-governance   <- ninth, gated by AC-2
```

**Configuration is data.** `config/*.yaml` drives reusable modules. New project =
a YAML entry. That is the baseline §16.1 future-change test, and it is the design.

## 2. Prerequisites and required permissions

| | |
|---|---|
| Instance | `https://gitlab-ce.au.bank.in/` — **Community Edition v19.1.2** |
| Parent group | `insurance`, id **820**, must already exist |
| Rights | create subgroups and projects beneath group 820 — **`ASM-024` unconfirmed**, gates M4.2 |
| Terraform | `>= 1.6.0` |
| Identity | dedicated least-privileged automation service account (§3) |

## 3. Authentication and token handling

`GITLAB_TOKEN` in the environment. Nowhere else.

- A **dedicated automation service account** — never an individual's PAT (baseline §5.1, `C-SEC-5`).
- **Distinct from the application deployment identity.** Two identities, two blast radii.
- Deliberately **not** a Terraform variable: a variable can be set in a `.tfvars`
  file, and a `.tfvars` file can be committed by accident. An environment variable cannot.
- `scripts/validate.sh` fails if any `.tfvars` exists.

## 4. Remote state and recovery

**There is no `backend.tf`, and that is deliberate.** Read
[`backend.tf.deferred`](./backend.tf.deferred) before creating one.

Blocked on `M1.6`, and specifically on **`SEC-F07` / `RISK-027`**: this state
controls the estate, so storing it *inside* the estate means an apply that damages
the estate also destroys its own recovery path. Bootstrap state goes **outside**
the GitLab instance. `delivery/infrastructure` state may be GitLab-managed.

Requirements when it lands (baseline §4.4, all mandatory): encryption at rest ·
restricted access · locking · versioning · **documented and tested** recovery ·
never in git. An untested restore is a belief, not a control.

## 5. Local workflow

```bash
./scripts/validate.sh          # fmt, validate, and the guard checks — no credentials needed
export GITLAB_TOKEN=...        # dedicated automation account
terraform plan -input=false    # requires a backend — blocked until M1.6

# M5.2 orphan import — gates only, then (on a bank host) PUSH=1
PREFLIGHT=1 SRC=/path/to/full-clone \
  COMPANY_GIT_NAME=... COMPANY_GIT_EMAIL=... \
  bash scripts/migrate-repositories.sh
```

M5.2 operator sequence: [`docs/platform/gitlab-migration/M5.2-OPERATOR.md`](../docs/platform/gitlab-migration/M5.2-OPERATOR.md).
`PUSH=1` refuses a destination that already has refs.

`apply` is **never** run locally. It runs from a protected pipeline job, manually,
by an authorised operator (baseline §11.1).

## 6. Importing an existing resource

Never delete a manually created resource so Terraform can recreate it (baseline §11.3).

```
discover -> verify ownership -> terraform import -> plan -> confirm NO destructive change -> manage
```

The `insurance` parent is handled more strongly than import: it is a **data
source**, so it cannot appear under `create` or `destroy` in any plan. The failure
mode is structurally impossible rather than guarded against.

## 7. Adding a subgroup, project, label or permission

1. Edit `config/*.yaml`. 2. Merge request. 3. `plan` in CI. 4. Human review. 5. Approved apply.

No new HCL for a new project. If a change needs new module logic, that is a signal
worth pausing on.

## 8. Provider upgrades

The constraint in `versions.tf` is **provisional and untested** — it could not be
tested here. `M3.11` narrows it to the resolved version and commits
`.terraform.lock.hcl`, which is the actual control. Upgrades go through a merge
request with a reviewed plan (baseline §4.3).

## 9. Drift reconciliation

Scheduled `plan` (M10.1) **reports**; it never auto-applies. Manual emergency
changes are reconciled back into configuration afterwards so desired and actual
state converge (baseline §12).

## 10. Rollback, recovery, ownership

[`docs/rollback.md`](./docs/rollback.md) · [`docs/disaster-recovery.md`](./docs/disaster-recovery.md) ·
[`docs/operating-model.md`](./docs/operating-model.md)

**Owner:** Shivanshi — SRE / Board 7 (`R10`). **Security:** Deepali (Board 4).
**Architecture:** Mahesh (Board 1).

---

## What is deliberately not here

| Not built | Blocked on | Kind of block |
|---|---|---|
| `backend.tf` | `M1.6` + `SEC-F07` | Unknown backend, and `RISK-027` bars the obvious answer |
| `memberships` module | `M1.4` / `ASM-014` | **Content** — identity group names unknown. A flag cannot substitute for data, and a guessed grant is active harm |
| First `terraform plan` (M3.11) | backend + credentials + instance access | Environment |

`branch-governance`, `environments` and the bootstrap `.gitlab-ci.yml` were deferred and
are now **built** — `C-ARC-6` resolved the capability question they were waiting on. See
[`modules/_deferred/README.md`](./modules/_deferred/README.md).

Per **`C-ARC-6`**, no module is omitted *because CE cannot apply it*. The capability
flags in `locals.tf` already model those controls; `output "unavailable_controls"`
reports what this instance cannot enforce, so the gap is visible rather than absent.
See [`modules/_deferred/README.md`](./modules/_deferred/README.md).
