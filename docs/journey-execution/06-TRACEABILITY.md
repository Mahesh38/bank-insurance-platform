# 06 — Traceability

**Use case → rule → invariant → seam → error → test.** One row per enforceable thing.

Status: `AI-DRAFTED` · Owner: Mahesh (Board 1) + Swapnali (QA) · Origin: `SUG-20260821-jx1`

---

## 0. Why this file exists

[`05-DOCUMENTATION-CANON.md`](../application-lifecycle-bible/05-DOCUMENTATION-CANON.md) names a
**requirements traceability matrix** as an S03 canonical artefact whose consumer is *"Regulator:
requirement → code → test"*, and marks it 🔴 absent. This file is the **execution half** of that
matrix: it traces a use case to the rules it enforces, the invariant that owns each rule, the seam
it crosses, the error it produces and the test that proves it.

The **business half** — BRD requirement id → use case — is still missing, because the BRD has no
testable acceptance criteria (`GAP-008`). The `Req` column below is therefore deliberately empty
rather than invented. Filling it is `GAP-008` work, not this pack's.

`Test` names the test that *should* exist. `—` means it does not exist yet. Nothing in this column
is evidence of anything until [`GATE-S08`](../governance/state/CURRENT-STATE.yaml) criterion
`S08-G6` (test infrastructure) closes; today every row is a specification of intent.

---

## 1. Slice 1 — access and session

| UC | Rule | Invariant / source | Seam | Error | Layer | Test |
|---|---|---|---|---|---|---|
| `UC-01` | `VR-010` | auth SSOT §5.1.2 | — | `400 INVALID_LOGIN_REQUEST` | L4 | — |
| `UC-01` | `VR-011` | auth SSOT §5.1.2 | — | `400 RETURN_LOCATION_NOT_ALLOWED` | L4 | — |
| `UC-01` | `VR-012` | auth SSOT §5.1.6 | — | `400 INVALID_STATE` | L4 | — |
| `UC-01` | `VR-013` | auth SSOT §5.1.6 | — | `400 CODE_ALREADY_CONSUMED` | L4 | — |
| `UC-01` | `VR-014` | OIDC + §5.1 | — | `401 AUTHENTICATION_FAILED` | adapter | — |
| `UC-01` | `VR-015`/`016` | auth SSOT §5.1.7 | — | `401 AUTHENTICATION_FAILED` | L5 | — |
| `UC-01` | `VR-017` | `INV-ACT-01`, `AC-1` | — | *(login proceeds)* | L5 | — |
| `UC-01` | `VR-018` | standing constraint | `TB-1` | *(design defect)* | L4 | — |
| `UC-01` | `VR-022` | auth SSOT §11 | — | *(generic error only)* | L4 | — |
| `UC-01` | `VR-023` | auth SSOT §13 | — | `503 IDENTITY_PROVIDER_UNAVAILABLE` | L4 | — |
| `UC-02` | `VR-041` | `INV-ACT-02` | `S-02` | `403 ASSIST_ONLY_ACTOR` | L5 PDP | — |
| `UC-02` | `VR-043` | auth SSOT §8.3 | `S-02` | `403 CROSS_INSURER_DENIED` | L5 PDP | — |
| `UC-02` | `VR-027`/`028` | auth SSOT §5.2 | — | `403 SEPARATION_OF_DUTIES` | L5 | — |
| `UC-03` | `VR-019` | auth SSOT §6 | — | `403 CSRF_REJECTED` | L4 | — |
| `UC-03` | `VR-020` | auth SSOT §6 | — | `401 SESSION_EXPIRED` | L4 | — |
| `UC-03` | `VR-021` | auth SSOT §6 | — | `401 SESSION_REVOKED` | L4 | — |
| `UC-03` | `VR-025` | auth SSOT §5.3 | — | `401 SESSION_REVOKED` | L4/L5 | — |
| `UC-04` | `VR-024` | auth SSOT §5.3 | — | *(204, idempotent)* | L4 | — |
| `UC-04` | `VR-026` | auth SSOT §8.3 | `S-02` | `403 ACCOUNT_SUSPENDED` | L5 PDP | — |
| `UC-05` | `VR-042` | auth SSOT §8.3 | `S-02` | `403 <reasonCode>` | L5 PDP | — |
| `UC-05` | `VR-040` | `INV-ACT-01`, `C3` | `S-02` | `403 SP_CERTIFICATION_REQUIRED` | L5 | — |
| `UC-05` | `VR-044` | auth SSOT §8.3 | `S-02` | `403 OUT_OF_BRANCH_SCOPE` | L5 PDP | — |
| `UC-05` | `VR-045` | `S-02`, auth SSOT §13 | `S-02` | `403 AUTHORIZATION_UNAVAILABLE` | L4+L5 | — |
| `UC-05` | `VR-047`/`048` | auth SSOT §8.3 | `S-02` | `403 BREAK_GLASS_INVALID` | L5 PDP | — |
| all | `VR-001`/`002` | `INV-IDM-01` | — | `400`/`409` | L4→L5 | — |
| all | `VR-003` | `INV-DIS-01`, `C3` | — | `422 ATTRIBUTION_NOT_CALLER_SUPPLIED` | L4+L5 | — |
| all | `VR-007` | `INV-LOG-01`, `C5` | — | *(build fails)* | framework + CI | — |

## 2. Slices 2–5 — specified, not expanded

| UC group | Rules | Slice |
|---|---|---|
| `UC-06`–`UC-09` origination | `VR-060`…`VR-066`, `VR-053`, `VR-100`…`VR-102` | 2 |
| `UC-10`–`UC-14` advisory and consent | `VR-070`…`VR-073` | 2 |
| `UC-15`–`UC-21` quotation and proposal | `VR-050`, `VR-051`, `VR-080`…`VR-087`, `VR-120` | 3 |
| `UC-22`–`UC-29` payment and policy | `VR-052`, `VR-090`…`VR-097`, `VR-104` | 4 |
| `UC-30`–`UC-35` cross-cutting | `VR-054`…`VR-056`, `VR-103`, `VR-110`…`VR-113`, `VR-066` | 5 |

---

## 3. Invariant coverage check

The property a reviewer should verify first: **every invariant has a `VR` id, and every `VR` id
appears in at least one use case.**

| Source | Count | Has a `VR` | Reaches a `UC` |
|---|---|---|---|
| `INV-*` compliance hard gates (§6.1) | 13 | 13 ✅ | 13 |
| `INV-*` aggregate invariants (§6.2) | 34 | 34 ✅ | 34 |
| `C1`–`C8` standing constraints | 8 | 8 ✅ | 8 |
| Auth/authz rules (WS-2 SSOT) | 30 | 30 ✅ | 30 |

**Zero unrouted invariants.** If a future change adds an invariant without a `VR` row, this table
is the check that catches it — the same property `DOC-MAP` enforces for documents.

---

## 4. What this pack cannot trace, and why

| Missing link | Blocked by | Owner |
|---|---|---|
| BRD requirement id → `UC` | `GAP-008` — the BRD has no testable acceptance criteria | Rajal + BA |
| `UC` → journey wireframe | `GAP-009` — journey wireframes absent | Digital |
| `VR` → consent rule pack | `GAP-006` — consent rule pack absent, **P0** | Shailja + Rajal |
| `VR-050` → suitability rule pack | `GAP-007` — suitability rule pack absent, **P0** | Shailja + Rajal |
| `VR` → test id | `S08-G6` open — no test infrastructure at every pyramid level | Swapnali |

> `GAP-006` and `GAP-007` matter more than the empty `Test` column. `VR-050` and `VR-051` are the
> two gates the whole R0 outcome rests on, and this pack can specify **where** and **how** they are
> enforced but not **what** they assert — the rule packs that define eligibility and the consent
> purpose set do not exist yet. Slice 3 will document the enforcement mechanism against a rule set
> that is still a placeholder, and it must say so where it does.
