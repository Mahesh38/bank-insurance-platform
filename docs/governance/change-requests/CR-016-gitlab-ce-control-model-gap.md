# CR-016 — GitLab Community Edition cannot enforce the approved governance model

**Date:** 2026-08-29
**Type:** CONSTRAINT (with `WAIVER` consequences on approved `CR-014` conditions)
**Raised by:** `agent:claude` in the Shivanshi (SRE / `R10`) persona
**Workstream:** WS-3 · **Stage:** S08 with S09 overlapped
**Driver:** **Validated assumption failure** — `ASM-023` answered, and answered worse than it contemplated
**Related:** [`CR-014`](./CR-014-gitlab-estate-migration.md) `APPROVED_WITH_CONDITIONS` · `RISK-017` **FIRED** · `RISK-023`

> ## Decision: `APPROVED_WITH_CONDITIONS` — 2026-09-01
> Relayed by `human:Mahesh` (repository owner) as authorisation to take the recommended path:
> **Option B now** (compensating CI controls), **Option A as the target** (CE → EE when the bank
> can run the package-migration window), a **time-boxed exception (C)** covering the
> platform-enforcement gap until EE, and **Option D rejected**.
>
> The agent recorded a decision the owner gave; it did not supply one (Rule CC-1, Rule PA-1).
> **This is not a T4 Security signature artefact.** Deepali's human Board 4 sign-off on the
> weaker enforcement boundary remains mandatory and outstanding.
>
> **This CR still does not stop the first push.** It blocks recording §6.2, §6.3 or §9.3 as
> *satisfied*, and it blocks M6.3 / M6.6 until the compensating jobs exist.

---

## 1. What the bank confirmed

| Input | Answer | Register |
|---|---|---|
| Base URL | `https://gitlab-ce.au.bank.in/` | `ASM-023` |
| Version and edition | **GitLab Community Edition v19.1.2** | `ASM-023` **INVALIDATED** |
| `insurance` group | `https://gitlab-ce.au.bank.in/insurance`, **id `820`** | `ASM-024` partial |
| Container Registry | Available | `ASM-017` partial |
| AWS | Account will exist; **conventions not yet confirmed** | `ASM-018` partial |

`ASM-023`'s pre-computed consequence read: *"if Premium, `S08-G5`'s mechanism reopens and only
Deepali may grant the exception."* **Community Edition is below Premium**, so the consequence is
larger than the one the assumption was written against. That is why this is a CR and not a register
update.

---

## 2. The capability position

Verified against GitLab documentation, 2026-08-29. **Every line below must be re-verified against the
actual v19.1.2 instance before it is relied on** — the tier structure is durable, but this version is
newer than the documentation the agent could reach, and the instance is authoritative over any
published matrix.

### 2.1 Absent in CE — these are the finding

| Capability | Tier required | What in the baseline depends on it |
|---|---|---|
| **Required MR approval rules** | Premium+ | §6.3 risk-based approvals. In CE any Developer may approve and **approval never blocks a merge** |
| **CODEOWNERS approval enforcement** | Premium+ | §6.4, §6.2. The file can exist and **does nothing** |
| **Protected environments** (project *and* group level) | Premium+ | §9.3 PROD/DR "protected / manual approval" — the production control model |
| Scan result / security approval policies, Security Dashboard, MR vulnerability widget | Ultimate | MR-blocking on security findings |
| Push rules, group-level protected branches | Premium+ | Group-wide branch governance |

### 2.2 Present in CE — the model is not lost, only the enforcement layer

| Capability | Status | Consequence |
|---|---|---|
| **"Pipelines must succeed"** (`only_allow_merge_if_pipeline_succeeds`) | Available — the setting carries no tier badge and is a project merge check | **`S08-G2` survives.** The `IMP-4` redesign — one required pipeline, every gating job `allow_failure: false` — works in CE. This is the single most important thing that did not break |
| Protected branches (no direct push, no force push, roles allowed to merge/push) | Available | §6.2 branch protection stands |
| Security **analyzers** (SAST, dependency, container, secret detection) | Available in all tiers as CI jobs | **`S08-G5` is achievable as blocking jobs.** Results are JSON artefacts only — no dashboard, no MR widget, no policy gate |
| GitLab-managed Terraform/OpenTofu state | Available, Free self-managed | M3.3 has a viable backend option; the bank standard is still unnamed (`ASM-016`) |
| CI/CD components | Available | `ci-components` and the `IMP-9` job-token model stand |
| Container Registry | Confirmed by the bank | §9.1 image naming and digest promotion stand |

### 2.3 The operational fact that changes the "buy our way out" answer

The instance is the **CE distribution**, not EE running on a Free licence. CE has the paid code
physically removed. Moving to Premium later is therefore a **package migration**
(`gitlab-ce` → `gitlab-ee`), not a licence-key entry — a change with downtime, a maintenance window
and a bank change record. Anyone assuming "we can upgrade later if we need to" should price that
correctly.

---

## 3. What this breaks in the approved position

Five `CR-014` conditions and three baseline sections are **unsatisfiable as written**. None may be
quietly re-interpreted as satisfied.

| Approved item | Status now | Why |
|---|---|---|
| `C-ARC-5` — `CODEOWNERS` reflects ratified ownership | **Unenforceable** | The file is advisory in CE. Mahesh's condition assumed it functions |
| `C-SEC-3` — differential CodeQL / GitLab-SAST run | **Still valid** | Analyzers run in CE; the differential is unaffected |
| Baseline §6.2 — CODEOWNER approval where supported | **Not supported** | The baseline's own "where supported" wording anticipates this, and it is now the case |
| Baseline §6.3 — risk-based approvals | **Not enforceable** | No required approval rules. The whole approval matrix becomes advisory |
| Baseline §9.3 — PROD/DR protected with approval | **Not available** | Manual jobs exist; protected environments with approvers do not |
| `GLM-001` M6.3 — apply risk-based approval rules | **Cannot execute** | Nothing to apply |
| `GLM-001` M6.6 — protected environments DEV→DR | **Cannot execute** | Nothing to apply |

`S08-G1`, `S08-G2` and `S08-G9` are **unaffected**. `S08-G5` is achievable with a changed mechanism
and a Security decision.

---

## 4. Options — owner-relayed choice 2026-09-01

The owner's authorised combination: **B now**, **A as the target**, **C until EE**, **D rejected**.
Deepali's human signature on the weaker enforcement boundary remains outstanding.

| # | Option | Cost | Leaves |
|---|---|---|---|
| **A** | **Licence upgrade to Premium** (and Ultimate for security policies) | Commercial cost; a `gitlab-ce` → `gitlab-ee` package migration with a change window | The approved model intact, as designed |
| **B** | **Compensating controls in CI.** Enforce approvals and environment gates inside the pipeline: a merge-check job that fails unless the required reviewers have approved, manual gated jobs for PROD/DR, scanner jobs that block | No licence cost; real engineering; the controls live in code the same team can change, which is a weaker separation of duties than a platform control | The *outcome* approximated, the *enforcement boundary* weaker |
| **C** | **Record a scoped exception** with an expiry and compensating monitoring | Cheapest now | The gap explicit and dated rather than hidden — but it is a real control gap during R0 |
| **D** | **Re-site the estate** on an instance that meets the baseline | Reopens residency, timeline and the whole M1 input set | The model intact; the programme substantially later |

Option B deserves one honest caveat rather than a recommendation: a pipeline job that checks
approvals is enforced by the same repository the developers control, so it is a *convention with
teeth*, not a platform guarantee. Whether that is acceptable for a bank insurance platform at R0 is
precisely the judgement Deepali and Shailja hold and the agent does not.

---

## 5. Impact

| | |
|---|---|
| **Scope** | No product scope change |
| **Stage** | `GATE-S08` unaffected in substance — `S08-G1/G2/G9` untouched, `S08-G5` needs a mechanism decision |
| **Dependencies** | Blocks `GLM-001` **M6.3** and **M6.6**. Does **not** block M2, M3, M4, M5 or M7 |
| **Effort** | A: procurement + a change window · B: **M**, ~2–3 agent-days of CI work · C: S · D: XL |
| **Risk if not decided** | The estate gets provisioned to the baseline's *shape* while enforcing none of its approval controls — an auditable-looking topology with advisory-only governance. `RISK-023`, exposure 9 |

---

## 6. What this CR does **not** do

- It does **not** manufacture Deepali's T4 Security signature on the weaker CE boundary.
- It does **not** re-open `CR-014`, which stands approved. M2 is unaffected and may proceed.
- It does **not** record §6.2, §6.3 or §9.3 as satisfied, waived, or downgraded.
- It does **not** grant the security exception Deepali declined to pre-approve — it gives her the
  information she said she needed in order to decide.
- It does **not** assert v19.1.2 behaviour as fact. §2 must be re-verified against the instance.

---

## 7. Change request record

```yaml
change_request:
  id: CR-016
  raised_by: "agent:claude"
  date: 2026-08-29
  type: CONSTRAINT
  driver: "validated assumption failure — ASM-023 answered as GitLab Community Edition v19.1.2, below the Premium its consequence contemplated"
  evidence:
    - "Bank confirmation 2026-08-29: https://gitlab-ce.au.bank.in/, GitLab Community Edition v19.1.2, insurance group id 820, container registry available, AWS conventions unconfirmed"
    - "GitLab documentation: required MR approval rules, CODEOWNERS approval enforcement and protected environments are Premium or above"
    - "GitLab documentation: security analyzers run in all tiers; Free and Premium emit JSON artefacts only, without the dashboard or MR widget"
    - "CR-014 Board 4 verdict section 4 — Deepali declined to pre-approve a control downgrade she could not yet see"
  impact:
    scope: "no product scope change"
    stage: "S08-G1/G2/G9 unaffected; S08-G5 needs a mechanism decision"
    dependencies: "blocks GLM-001 M6.3 and M6.6 only; M2 through M5 and M7 unaffected"
    parked_items: "none"
    effort: "A: procurement plus a package migration; B: M; C: S; D: XL"
    risk_if_rejected: >
      The estate is provisioned to the baseline's shape while enforcing none of its approval
      controls — an auditable-looking topology with advisory-only governance.
  alternatives_considered:
    - option: "Re-interpret CODEOWNERS and optional approvals as satisfying sections 6.2 and 6.3"
      consequence: >
        Records a control as present when it is advisory. This is the specific failure the CR exists
        to prevent, and Swapnali's C-QA-5 already forbids the equivalent move on gate evidence.
  decision: APPROVED_WITH_CONDITIONS
  approvers: ["Board 1 Architecture (owner-relayed)", "Board 4 Security (human signature outstanding)"]
  decided_on: "2026-09-01"
  recorded_by: "agent:cursor, from owner authorisation by human:Mahesh on 2026-09-01"
  conditions:
    - "Option B compensating CI (governance-merge-gate + existing manual/restricted-runner apply) is the R0 enforcement"
    - "Option A (gitlab-ce → gitlab-ee) remains the target; package-migration window is a bank change"
    - "Time-boxed exception (C): platform CODEOWNERS / required-approvals / protected-environments are unenforced until EE"
    - "Option D (re-site) is rejected — it reopens C-CMP-1"
    - "Do not record baseline §6.2, §6.3 or §9.3 as satisfied"
    - "Does not gate M5.2 orphan import"
  signature_status: >
    Owner-relayed 2026-09-01. Not a T4 Security signature artefact. Deepali's human
    Board 4 sign-off on the weaker enforcement boundary remains outstanding.
```
