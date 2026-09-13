# Sync-check evidence bar

**Authority:** Board 5 condition C12 on [CR-016](../change-requests/CR-016-north-star-and-service-workstream-strategy.md) · [WORKSTREAM-STRATEGY §5.3](./WORKSTREAM-STRATEGY.md#53-dependency-sync-check)  
**Drafted for:** Swapnali (QA) — AI draft 2026-09-13; human adoption when first exercised  
**Binding on agents from:** publish date (honor the bar even before human QA countersign)

---

## 1. When this bar applies

Any work item that:

- appears on **two or more** `SWS-*` boards, or
- changes a producer contract a consumer already depends on, or
- clears a **Waiting to unblock** row that named another `SWS-*` / DEP.

Same-service refactors with no peer contract change do **not** need a sync check.

---

## 2. Minimum evidence (all three, unless waiver path)

| # | Evidence | Acceptable forms | Not acceptable |
|---|---|---|---|
| **E1 Named dependency** | Same id on both boards | `DEP-########-***`, shared `FUNC-###` / plan id, or CR id | “aligned with lead team”, unnamed Slack chat |
| **E2 Contract artefact** | Path both owners can open | OpenAPI/AsyncAPI path + operation, event schema path, error-contract section, shared AC id in a governed doc | Vague “API updated”, screenshot of IDE only |
| **E3 Consumer proof** | Consumer can exercise producer behaviour | Automated test path (unit/component/contract) with report or CI job link; or recorded manual script id with owner | Producer-only unit tests; “works on my machine” |

**Pass** = E1 + E2 + E3 recorded in **both** boards’ Sync log and in the implementing PR description.

**Fail** = leave/keep **Waiting to unblock**. Do not mark Completed on either side.

---

## 3. Waiver path (explicit, dated, owned)

If E3 cannot run yet (e.g. consumer skeleton):

| Field | Required |
|---|---|
| Owner | Named persona or human role (not “team”) |
| Reason | One sentence |
| Expiry | ISO date ≤ 14 days unless Kalpana records a longer DL window |
| Residual risk | What can break if producer ships alone |
| Escalation | Who is notified at expiry (default: Kalpana R12) |

Waivers **expire**. An expired waiver returns the item to Waiting. Waivers are never regulatory evidence (Board 6).

---

## 4. Security / consent / payment edges

If E2 changes behaviour of identity, authorization, consent capture, payment initiation/reconciliation, or audit retention:

1. Sync check still requires E1–E3.
2. Additionally tag the Sync log: `BOARD4_REVIEW=needed` or `BOARD6_REVIEW=needed` as applicable.
3. Do **not** treat Architecture/Engineering agreement alone as Board 4/6 approval.

---

## 5. PR checklist snippet (copy into cross-SWS PRs)

```text
Sync-check:
- [ ] E1 dependency id: DEP-… / FUNC-…
- [ ] E2 contract path: …
- [ ] E3 consumer proof: test/CI …
- [ ] Both SWS boards updated in this PR
- [ ] Waiver? no | yes → owner/expiry/risk
```

---

## 6. What this bar is not

- Not GATE-S08 evidence.
- Not Board 7 operational readiness.
- Not Product acceptance of a journey.
- Not a licence to skip standing Hub / trust-boundary constraints.
