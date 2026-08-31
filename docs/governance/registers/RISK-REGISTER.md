# Risk Register

Open risks with owners, triggers, and responses. A risk is not a problem — it is a **problem
with a probability**. Risks feed the `R` factor in priority scoring
([05 §4](../05-PRIORITY_MODEL.md#4-the-scoring-model)) and the Risk & Compliance board.

**Owner:** Delivery Lead (register) · named owner per risk
**Reviewed:** at every stage gate

---

## 1. Scoring

`Exposure = Likelihood × Impact`, each 1–3.

| Likelihood | | Impact | |
|---|---|---|---|
| 1 | Unlikely in this stage | 1 | Contained; recoverable in hours |
| 2 | Plausible | 2 | Material; delays a gate or degrades a journey |
| 3 | Expected without action | 3 | Severe; data loss, breach, regulatory finding, or go-live block |

| Exposure | Response |
|----------|----------|
| 1–2 | Accept and monitor |
| 3–4 | Mitigate this stage |
| 6 | Mitigate now — usually a P1/P2 work item |
| 9 | Escalate to the PO and Architect immediately |

---

## 2. Open risks

| ID | Risk | WS | L | I | Exp | Owner | Response | Trigger to escalate |
|----|------|----|---|---|-----|-------|----------|---------------------|
| RISK-003 | Bank AD technology unconfirmed (DEP-010) | WS-2 | 2 | 3 | 6 | PO + Architect | Adapter design must remain federation-agnostic; do not pre-commit | Phase 1 gate PASSED with no confirmation |
| RISK-004 | In-memory idempotency is unsafe multi-instance (TD-010) | WS-1 | 2 | 3 | 6 | Tech Lead | Single-instance constraint documented; Redis scheduled Phase 5.4 | Any plan to run > 1 instance |
| RISK-005 | AWS Secrets Manager provider is a stub; prod profile fails fast (TD-006) | WS-1 | 2 | 3 | 6 | Platform | Keep fail-fast; schedule at Phase 6 | AWS deployment target confirmed |
| RISK-006 | Service coverage is on an interim floor (QA-001 partial) | WS-1 | 2 | 2 | 4 | QA Lead | Close or waive with expiry at gate 4.7 | Phase 4 gate CANDIDATE with 4.7 unresolved |
| RISK-007 | Raw payload capture incomplete for status / master-data (TD-023) | WS-1 | 2 | 2 | 4 | Tech Lead | Scope decided by compliance review 4.4 | Compliance requires full capture |
| RISK-008 | No QA Engineer / QA Lead cycle ran for Phase 4 stories (single-agent branch) | WS-1 | 2 | 2 | 4 | QA Lead | Recorded variance in `phase-4/STATUS.md`; QA pass before UAT sign-off | UAT exposure without a QA cycle |
| RISK-009 | 1SB sandbox instability could stall E2E in CI (gate 4.1) | WS-1 | 2 | 2 | 4 | Eng | Gated nightly fallback already sanctioned by ACTION-PLAN 4.1 | E2E flakiness blocks the pipeline |
| RISK-010 | Governance adopted mid-flight; historical work never passed AIGEM gates | Both | 3 | 1 | 3 | Delivery Lead | Deliberate ([19 §5](../19-PORTING_GUIDE.md#5-bootstrapping-into-an-existing-project-mid-flight)) — forward-only adoption | An incident traces to un-triaged historical work |
| RISK-012 | The five 2026-08-24 platform layers (`ADR-009`…`ADR-013`) raise R0 **fixed** cost above the level the S09 budget line assumed, at ~100 journey starts an hour — so the estate is priced for availability and evidence while the business case is priced for a pilot | WS-3 | 3 | 2 | 6 | Shivanshi / SRE + Kalpana / R12 | Per-environment shapes cap it (`R0-LLD` §1.4): `dev` is deliberately not production-shaped. Envelope produced at S09 as `NFR-OPEN-6` **before** first `apply` to `uat`. A shape is never lowered in `prod` to fit a cost conversation without a Security and SRE verdict | Cost envelope not produced before `GATE-S09` entry, or a cost conversation proposing to drop a `prod` control |
| RISK-013 | Bank-side connectivity work (`DEP-20260824-dx1`) does not land, so `uat` keeps running against CBS and Bank AD stubs and `#4` Customer plus WS-2 Phase 2 cannot be evidenced | WS-3 | 2 | 3 | 6 | Shivanshi / SRE + bank network | VPN before Direct Connect, so the path needs a firewall rule rather than a carrier order; `dev` stubs stay legitimate; chase date on the dependency row | A UAT date is set while the VPN half is still unconfirmed |
| RISK-014 | Operational surface outruns team maturity: three stateful managed services (broker, cache, search), a firewall rule set and two circuits arrive while `GATE-S08` is still open and no service has run in a real environment | WS-3 | 3 | 2 | 6 | Shivanshi / SRE | Managed services only — nothing self-hosted (`R0-LLD` §4.1); shapes sized for availability, not throughput; the outbox keeps a broker outage to a delay rather than a loss; every new tier has a named runbook and a drill in the `P8` proof band | A tier reaches `uat` without its runbook and its drill, or an incident is resolved by disabling a control |
| RISK-015 | Invariant erosion under incident pressure: the cache becomes an idempotency or evidence store, a topic becomes the audit record, or the search index becomes the queried source of truth — each of which looks like a fix at 03:00 | WS-3 | 2 | 3 | 6 | Mahesh / Architecture + Deepali / Security | The forbidden lists are machine checks, not conventions: `FF-23`, `FF-24`, `FF-26`, `FF-27`, `FF-28`. `ADR-011`/`ADR-012`/`ADR-013` each name the temptation explicitly so it is recognised rather than rediscovered | Any proposal to serve configuration past TTL from cache, to extend topic retention for audit purposes, or to answer a compliance query from the index |
| RISK-016 | A live credential sits in the 273-commit / 81-branch history and is pushed into the bank estate, converting a personal-repo exposure into a bank security incident with disclosure obligations | WS-3 | 2 | 3 | 6 | Deepali / Security | `C-SEC-1` blocking full-history scan before the first push; `C-SEC-2` rotate → scrub → migrate, in that order. Never scrub before rotating — it leaves the live secret live and destroys the evidence of what to rotate | Any proposal to push history before the scan is clean, or to scrub before rotating |
| RISK-017 | ~~The bank GitLab edition is Premium rather than Ultimate~~ → **FIRED 2026-08-29. The edition is Community Edition v19.1.2 — below Premium.** Required MR approval rules, CODEOWNERS approval enforcement and protected environments are absent entirely, not merely reduced | WS-3 | 3 | 3 | **9** | Deepali / Security + Mahesh / Architecture | **Escalated to the PO and Architect (exposure 9).** Raised as `CR-016`. Scanning survives as blocking CI jobs — the analyzers run in all tiers — but MR-blocking security policies, the Security Dashboard and the MR vulnerability widget do not exist, and five approved `CR-014` conditions are unsatisfiable as written. Deepali's declined pre-approval now needs an actual decision | **Already fired.** Escalate again if any condition is quietly re-interpreted as satisfied rather than re-decided through `CR-016` |
| RISK-018 | `S08-G1/G2/G5/G9` are evidenced by the CI platform and GitHub Actions run history does not migrate, so four criteria re-open at cutover and are quietly claimed on ported configuration instead of new evidence | WS-3 | 3 | 2 | 6 | Swapnali / QA | `DEC-20260829-01` §2 chose re-evidencing on GitLab. `C-QA-2`/`C-QA-4`/`C-QA-5`: no criterion is satisfied by "ported, therefore equivalent"; `S08-G2` is proven negatively by a failing job that blocks a real merge | Any criterion recorded as passed on configuration review rather than an executed run |
| RISK-019 | The monorepo split breaks cross-project CI after cutover, when the GitHub origin is already read-only — job-token allowlisting is the usual cause | WS-3 | 2 | 2 | 4 | Shivanshi / SRE | Model `gitlab_project_job_token_scope` at M3.6, apply at M6.8, **before** the first consuming pipeline runs (`C-OPS-5`) | A cross-project pipeline is enabled before the allowlist is applied |
| RISK-020 | The freeze window expires with the migration incomplete and both sides stay writable "for a bit", producing the two-sources-of-truth state the baseline forbids — permanently | WS-3 | 2 | 3 | 6 | Kalpana / R12 | `C-OPS-3`: named owner, announced start, end time, and a **pre-agreed expiry action** — roll back to the anchor and re-schedule. Cheaper than dual write, and only cheaper if decided in advance | The window passes without the expiry action being invoked |
| RISK-021 | The bank GitLab instance or its storage sits outside AWS India regions, so repository content, CI logs, artifacts, registry and Terraform state breach the residency standing constraint — **this can invalidate the destination, not merely the schedule** | WS-3 | 2 | 3 | 6 | Shailja / Compliance | `C-CMP-1`: written confirmation of physical residency, ruled permissible by Board 6, before any push. Raised as `CMP-F01` in the CR-014 board round; `GLM-001` M1.2 did not ask the question | Any push to the bank estate before residency is confirmed, or a proposal to migrate first and relocate later |
| RISK-022 | The S09 allocation of `bank_persistence` tables to per-context schemas is a **data migration on a live evidence store** — the 1SB job store and audit ingest — and a partial or unrecovered allocation loses regulatory evidence rather than merely breaking a build | WS-3 | 2 | 3 | 6 | Aarti / Database + Mahesh / Architecture | `CR-015` Option B fixes the target but **not** the migration: Aarti's independent integrity and recovery review (Q4) is a precondition, including a restore test against `DB-DEC-0001`'s targets (RPO 5 min, RTO 30 min). Parked to S09 behind the `CR-014` cutover by `AC-5`, so it never runs inside the migration window | Any proposal to allocate tables during the CR-014 freeze, or to run the migration before Aarti's restore test passes |
| RISK-023 | GitLab CE cannot enforce the baseline's governance model: §6.2 CODEOWNER approval, §6.3 risk-based approvals and §9.3 protected environments for PROD/DR all require Premium or above. The estate can be *provisioned* to the baseline and still not *enforce* it, which is the worst of both — an auditable-looking topology with advisory-only controls | WS-3 | 3 | 3 | **9** | Mahesh / Architecture + Deepali / Security | `CR-016` puts the options to the boards: licence upgrade, compensating controls, or a recorded exception. **A distribution swap, not just a licence key** — CE has the paid code removed, so moving to Premium later is a package migration (`gitlab-ce` → `gitlab-ee`), which is an operational change, not a purchase | Any proposal to record §6.2, §6.3 or §9.3 as satisfied by an unenforced CODEOWNERS file, an optional approval, or a manual job |
| RISK-024 | The migration host runs the secret scan, split or push from a **shallow clone**, so `C-SEC-1` returns a verdict on partial history. Observed live at M2: the session clone was shallow, 85 commits on `main` were absent, and gitleaks reported **9** findings where the complete history reports **5** — the boundary commits presented as roots and re-flagged the same secret five times | WS-3 | 2 | 3 | 6 | Shivanshi / SRE | Proposed condition `C-OPS-7`: assert `git rev-parse --is-shallow-repository` is `false` before any scan, split or push, and record the assertion with the scan report. **A shallow clone that scans clean proves nothing** | Any scan, split or push whose evidence does not carry the non-shallow assertion |
| RISK-025 | The `pre-gitlab-migration` rollback anchor exists only locally. GitHub refuses tag pushes on the session credential (`HTTP 403` at send-pack, twice; the egress proxy recorded no failure, so it is credential scope, not policy). `C-OPS-1` requires the anchor to exist **before the freeze**, and a local-only tag is lost with the container | WS-3 | 2 | 3 | 6 | Shivanshi / SRE | Push `refs/tags/pre-gitlab-migration` at `b8027751738b04d00dbe071a77b2aba56828a2cd` from a credential with tag-write scope, then verify with `git ls-remote --tags`. The tag is annotated and verified locally, so this is a transport problem, not a content one | The freeze is scheduled while `git ls-remote --tags origin` does not show the anchor |
| RISK-026 | Finding B — the `RAW_PAYLOAD_ENCRYPTION_KEY` baked into the Dockerfile 2026-08-04 — is dispositioned **COMPROMISED / POTENTIALLY LIVE** (operator ruling 2026-08-29): a runnable Docker default cannot be declared dead without deployment evidence. If any stored `raw_payload` row decrypts with it, regulated PII under 7-year retention is encrypted under a key that was public in `main`'s history | WS-3 | 2 | 3 | 6 | Deepali / Security + Aarti / Database | `C-SEC-2` in full and in order: rotate or formally retire, determine whether any row decrypts with it, re-encrypt or dispose of affected data, **then** scrub, then re-scan. `filter-repo` is barred until step 1 completes and the rollback anchor is verified on the remote | Any history rewrite before rotation/retirement is recorded, or any claim that the key is dead without deployment evidence |
| RISK-027 | **Bootstrap Terraform state stored inside the estate it controls.** GitLab-managed state is the attractive CE-viable answer to `M1.6`, but the bootstrap state governs the estate's groups and projects — so an apply that damages the estate also damages its own state, and recovery requires the thing just destroyed. It satisfies none of `C-SEC-6`'s three requirements (separate backend, separate key, distinct identity) | WS-3 | 2 | 3 | 6 | Deepali / Security + Shivanshi / SRE | Split `M1.6` in two: **bootstrap state outside the GitLab instance** (enterprise S3/KMS or equivalent); `delivery/infrastructure` state may be GitLab-managed. Raised as `SEC-F07` in the M3-readiness board round | Any `backend.tf` that points bootstrap state at the GitLab instance the bootstrap provisions |
| RISK-028 | **The approved repository split breaks an `ADR-017` enforcement control.** `CatalogueParityTest` in `libs/bank-common-error` reads two ratified documents from `docs/journey-execution/` that the split moves to `platform-governance`; four tests fail on a fresh clone of the split `backend`. `ADR-017`'s stated purpose is that the catalogue is executable rather than paper, and the split returns it to paper | WS-3 | 3 | 2 | 6 | Mahesh / Architecture + Amit / Engineering | Decide between relocating the documents, publishing them as a versioned artefact, or moving them to `contracts` — options at `M2-8-C-ENG-5-split-build.md` §4. **Do not make the test skip when the documents are absent**: `C-ENG-2` forbids that shape, and it converts a red build into a disabled control | Any proposal to conditionally skip `CatalogueParityTest`, or any push of the split `backend` before it builds green |

## 3. Accepted risks

Risks knowingly carried, with the acceptance recorded so they are not re-raised as findings.

| ID | Risk | Accepted by | Until | Why acceptable |
|----|------|-------------|-------|----------------|
| → [RISK-004](#2-open-risks) | In-memory idempotency | Tech Lead | Phase 5.4 | Single instance in UAT; scale-out is gated. **Unchanged by the platform cache tier** — `ADR-011` keeps idempotency in the owning store, so a shared cache existing does not close this |
| → [RISK-010](#2-open-risks) | Forward-only governance adoption | Delivery Lead | — | Backfilling costs days and changes no shipped code |

> `RISK-012` … `RISK-015` are **not** accepted. They are open against a decision set that is
> AI-DRAFTED, and each names the human who has to accept or reject it. An agent recording a risk
> against its own proposal does not thereby carry it.

## 4. Closed risks

| ID | Risk | Closed | How |
|----|------|--------|-----|
| RISK-001 | Current-state file unratified; agents may triage against a wrong stage | 2026-08-10 | Ratified by the Solution Architect (GOV-004); `provisional: false`. `FreshnessCheck` now halts if the state goes stale, so the risk cannot silently return |
| RISK-002 | External UAT dependency (DEP-002) had no owner or date | 2026-08-16 | Assigned to Rajal / Product with follow-up on 2026-08-21 in the dependency and gate-evidence registers |

---

## 5. Raising a risk

```yaml
risk:
  id: RISK-011
  statement: "If X happens, then Y, causing Z"     # not "X is bad"
  workstream: WS-1
  likelihood: 2
  impact: 3
  exposure: 6
  owner: "named person or role"
  response: MITIGATE          # ACCEPT | MITIGATE | TRANSFER | AVOID
  mitigation: "the work item that reduces it"
  escalation_trigger: "the observable event that makes this urgent"
  review_at: "Phase 4 gate"
```

A risk without an owner and an escalation trigger is a worry, not a risk. Worries do not belong
in the register.
