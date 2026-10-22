# Owner decisions 2026-10-22 — round 2

[← Round 1](./DECISIONS-2026-10-21.md) · Origin:
[`SUG-20261022-rdy`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20261022-rdy--build-readiness-for-all-21-services)

Mahesh's answers to the open items of round 1 (`reviewer_type: HUMAN`,
`authority: repository_owner`), plus the compliance-strict decisions he delegated.

## Owner decisions

| # | Decision | Replaces / amends |
|---|---|---|
| E1 | **Payment only after the insurer's underwriting approval.** `AWAITING_PAYMENT` is reachable only from `UW_APPROVED` (`INV-PRP-03` stands). The payment link is generated after UW approval, not at proposal submission. | Corrects the sequence in [round 1 §D8](./DECISIONS-2026-10-21.md#d8--payment-issuance-tracking-and-the-long-term-record-d-025); `D-025` amended |
| E2 | **ADR-026 accepted:** business audit as outbox events into the append-only, hash-chained `audit` schema owned by audit-compliance-service, archived daily to S3 Object Lock; DB-activity audit stays pgaudit. | [ADR-026](./ADR-026-audit-store-and-transport.md) Proposed → Accepted |
| E3 | **New schemas accepted:** `hub` (`provider_request` idempotency/correlation), `notification` (idempotency + SENT/FAILED), `audit` (E2). | Contracts for hub, notification, audit |
| E4 | **Journey stage `HALTED_CONSENT_WITHDRAWN` accepted** with error `JOURNEY_HALTED_CONSENT_WITHDRAWN`. | Journey contract |
| E5 | **Partner users are created directly by a bank admin — no approval step.** Creation requires the `partner_user.create` permission (bank admin roles only); the user is provisioned in Keycloak at creation. The maker-checker approval flow is retired as the default and kept behind configuration `identity.partner-onboarding.approval-required` (default `false`) so it can be re-enabled without code. | identity-authorization partner flow |
| E6 | **Lead exception approvals are made only by bank employees in the SP hierarchy** (bank admin and configured approver roles). An insurer partner user can create a lead but can never approve an exception. Which hierarchy level approves which rule is configuration (`LEAD.EXCEPTION` parameters `approverRoles` per rule), checked by the PDP permission `lead.exception.approve`, branch-scoped. | Exception Handling BRD §12 approval hierarchy; lead contract |
| E7 | **Every business value is configuration, scoped where it varies:** lead expiry days (per line of business — e.g. LIFE 90, MOTOR 30/60 later), lead exception thresholds, customer snapshot freshness window, insurer TAT and polling, identity-provider timeouts. New rule sets: `LEAD.POLICY` (per `lob`: `expiryDays`, …) and `CUSTOMER.POLICY` (`snapshotFreshnessMinutes`, …). | [ADR-024 §7](./ADR-024-configuration-driven-rule-engine.md#7-rule-sets-in-r0-and-who-approves-their-content) |
| E8 | **AD-verify / identity-provider timeouts:** industry-standard defaults, configurable by environment variable — connect **2 s**, read **3 s** for AD verify; Keycloak connect 2 s / read 3 s; the BFF budget to the adapter (4.5 s) stays above them. | S-26 figures |
| E9 | **Compliance posture for this change set: strict.** Where a compliance question is open, the platform takes the stricter option, records it here, and leaves the relaxation to Compliance as a configuration change. Nothing in this change set crosses a compliance line. | — |

## Compliance-strict decisions taken under E9

| # | Question | Strict decision (seed configuration) | Relaxation needs |
|---|---|---|---|
| C1 | DPDP: may suitability (which collects income, dependants, liabilities) run before data-processing consent? | **No.** `CNS-DP` and `CNS-SOL` are required at checkpoint `BEFORE_SUITABILITY`. The owner's flow is kept for quoting: at `BEFORE_MULTI_QUOTE` the platform checks for valid consent (90-day reuse) and captures only what is missing — `CNS-QSH` (sharing quote inputs with insurers) plus a re-check that `CNS-DP`/`CNS-SOL` are still valid | Compliance confirms the owner's order is permissible → move `CNS-DP`/`CNS-SOL` to `BEFORE_MULTI_QUOTE` in `CONSENT.POLICY` |
| C2 | OTP: may the customer read the OTP to the RM? | **No** — customer enters it on their own device only (`CNS-R13`) | `CONSENT.POLICY.verificationModes` |
| C3 | One acknowledgement for `CNS-DP` + `CNS-SOL`? | **No** — separate affirmative actions (`OPEN-CNS-01`) | `CONSENT.POLICY.separateActions=false` |
| C4 | Insurer decision TAT | **7 days** from proposal receipt, calendar days (IRDAI Master Circular 2024) | `PROPOSAL.TAT` |
| C5 | Suitability outcomes that may quote | `SUITABLE`; `SUITABLE_WITH_CAUTION` only with customer acknowledgement on their own device | `SUITABILITY.GATE` |
| C6 | Savings / ULIP gaps in the rule pack (affordability > 0.20, Savings + `WEALTH_GROWTH`) | Result `INSUFFICIENT_DATA` → **no quote** for that class until Compliance adds the rule | New rule-set version |
| C7 | Consent statement wording (incl. new `CNS-QSH`), customer-facing suitability texts | Drafts only; **the journey cannot capture a purpose whose statement is not approved** (statement `status` must be `APPROVED`) | Compliance approves statements |
| C8 | GAP-006 / GAP-007 | Remain **OPEN** until the human Compliance signature ([sign-off packet](../../au-bank-insurance-platform/rule-packs/SIGNOFF-PACKET-GAP-006-007.md)); no agent or owner instruction substitutes it (`DEC-20260816-06`) | Compliance signature |

## Engineering defaults chosen (all configuration; change without code)

| Value | Default | Where |
|---|---|---|
| Lead expiry, LIFE | 90 days (owner's ideal) | `LEAD.POLICY` scope `lob=LIFE` |
| Customer snapshot freshness | 24 hours | `CUSTOMER.POLICY` |
| Insurer status poll interval | 4 hours, warning at day 5 | `PROPOSAL.TAT` |
| Lead exception thresholds | **unset** — every lead routes to `BLOCK` until set (BRD: "block where configuration unavailable") | `LEAD.EXCEPTION` |
