# Board 6 — Risk & Compliance · Position on CR-014

**Change request:** [CR-014](../../CR-014-gitlab-estate-migration.md) · **Plan:** [`GLM-001`](../../../../platform/gitlab-migration/GLM-001-migration-plan.md)
**Board:** 6 — Risk & Compliance (**blocking authority** `B`) · **Role:** `R9`
**Persona:** Shailja S — Compliance & Risk Head
**Reviewer type:** `AGENT` · **Self-review:** false · **Change tier:** `T4` · **Date:** 2026-08-29

> ### ⚠ AI-drafted — the mandatory human Risk & Compliance signature is NOT satisfied
> An AI persona must never generate a regulatory permissibility conclusion as a signature, and must
> never issue `TEMPORARY_EXCEPTION_APPROVED`.
>
> **`signature_status: AI-DRAFTED — mandatory human Board 6 signature outstanding`**

> ## Drafted verdict: `APPROVE-WITH-CONDITIONS`
> **Compliance severity: `R2` — medium.** Nothing in this change is non-compliant on its face; a
> repository move is not a regulated business action. **One finding is capable of becoming `R0`**
> and cannot be closed from inside this repository — `CMP-F01`.

---

## 1. What I reviewed

The standing constraints in [`BOOT.md`](../../../../context/BOOT.md) §5 · the WS-3 `never` list ·
GLM-001 phases M2.4, M5, M9 and M10 · the bank baseline §5 (retention/audit inputs), §7.2 (cutover),
§9.1 (registry) · `render.yaml` · the shape and volume of `docs/` (441 tracked files, ~16 MB).

I did **not** attempt to enumerate the contents of `docs/`. That is condition `C-CMP-2`, and it is
work, not a review finding.

---

## 2. Findings

### `CMP-F01` · **capable of `R0`** — data residency is unresolved, and nothing in the plan asks

This is my principal finding, and GLM-001 does not contain it.

The WS-3 standing constraint is unambiguous: **regulated data, backups, logs or archives outside
AWS India regions — never.** Not "production data". Not "customer data". *Archives.* *Logs.*

GLM-001 M1.2 asks for the GitLab "base URL, version and edition/licence" because those decide
provider capability. It does not ask **where the instance and its storage physically are**, and the
migration proceeds identically either way. That is the gap.

If the bank GitLab is self-managed inside an in-region bank environment, this finding closes with a
written confirmation. If it is GitLab.com, or a managed instance outside India, then repository
content, CI job logs, job artifacts, the container registry and — pointedly — the Terraform state
holding the estate's configuration all sit outside India, and I cannot rule the estate permissible
for anything that will later carry regulated material.

Two things make this urgent rather than theoretical. First, it is cheap now and a re-migration
later. Second, `docs/` is going too, and I do not yet know what is in it (`CMP-F02`).

**I am not ruling `R0` today.** I am ruling that M1.2 is incomplete, that the residency answer is a
Board 6 input rather than an SRE one, and that no push to the bank estate may precede it.

### `CMP-F02` · `R2` — 16 MB of documents entering a bank estate, uninventoried

441 tracked files, ~16 MB, accumulated across 273 commits on a personal account. IMP-12 proposes a
sweep and correctly routes findings to me. I am converting it from a cheap win into a condition and
naming what the sweep must look for, because "sweep for PII" is not an instruction anyone can
execute or evidence:

- customer names, identifiers, contact details, or any real customer record in an example or fixture;
- real premium, quote, proposal or policy values presented as samples;
- insurer or aggregator material received under NDA — 1SB specifics included;
- bank-internal identifiers, hostnames, account numbers or network detail;
- credentials, tokens or keys in documentation or examples;
- screenshots and diagrams, which is where this material actually hides.

The sweep runs **before** the split push, not after. Content in a bank estate is content the bank
holds.

### `CMP-F03` · `R2` — retention has no answer yet, and the estate will hold evidence

GLM-001 M1.9 gathers "retention and audit requirements". That is correctly identified and
under-specified. The concrete risk: gate evidence, pipeline logs, SBOMs and scan reports become
**artifacts** in GitLab, and default artifact expiry is measured in days-to-weeks. Evidence that
must be retained for seven years, sitting in a 30-day artifact, is not retained — it is scheduled
for deletion.

Before any evidence artefact is produced in the new estate, the retention class of each evidence
type must be named, and the mechanism that enforces it must not be a default.

### `CMP-F04` · `R2` — "archived" is a disposition, and it needs naming

GLM-001 M9.4 archives the GitHub origin read-only. Archived is not deleted, which is correct — but
it leaves a permanent, read-only copy of the platform's full history, including whatever `CMP-F02`
finds, **on a named individual's personal GitHub account**. That is a data-custody position, not a
housekeeping detail, and it should be decided rather than defaulted into.

The disposition must name: who owns the account, how long the archive is retained, under what access
controls, what happens on the owner's departure, and the deletion trigger and its authority.

---

## 3. Conditions

| ID | Condition | Control | Must be true before |
|---|---|---|---|
| `C-CMP-1` | Written confirmation of where the bank GitLab instance and its storage — repository, CI logs, artifacts, registry, Terraform state — physically reside, ruled permissible by Board 6 | Data residency standing constraint | GLM-001 M5.2 — the first push |
| `C-CMP-2` | `docs/` sweep executed against the six classes in `CMP-F02`, findings recorded, remediation completed | PII / NDA / minimisation | GLM-001 M5.2 |
| `C-CMP-3` | Retention class named per evidence type (gate evidence, pipeline logs, SBOM, scan reports, audit artefacts) and enforced by configuration, not by default expiry | Retention | GLM-001 M6.9 |
| `C-CMP-4` | GitHub origin archive disposition recorded: owner, retention period, access control, departure handling, deletion trigger and authority | Data custody | GLM-001 M9.4 |
| `C-CMP-5` | No regulated customer data, real premium/quote values or production-like records reach the Render demo target from any new pipeline | Minimisation | Continuous |

---

## 4. What I am not deciding

- I am **not** declaring the estate permissible. `C-CMP-1` is unanswered and it is the gate.
- I am **not** reinterpreting the residency constraint to accommodate a delivery date. `DL` severity
  does not move `R` severity.
- I am **not** declaring anyone's evidence passed — Security's, QA's or SRE's.
- I **cannot** satisfy my own signature, and I cannot issue an exception with regulatory impact
  without the authorised human. The human Board 6 signature on `CR-014` is mandatory and outstanding.
