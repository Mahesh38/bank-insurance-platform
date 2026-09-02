# 05 — Critical Journeys & Non-Bypassable Quality Gates

## 1. Protected quality gates

The following require strong evidence whenever materially changed. Testing cannot be waived merely for schedule pressure.

### Authentication and authorization

Positive and negative actor checks, token/session/SSO/OTP changes, RM/customer/insurer/ops/admin isolation, ID manipulation and cross-insurer access.

### Consent

Correct customer, purpose/version, timestamp/channel, downstream propagation and auditability.

### Suitability and eligibility

Age/income/occupation/smoking/coverage/tenure/objectives/boundaries, insurer/product eligibility and false-positive prevention.

### Premium, tax, riders and sum assured

Deterministic values, precision/rounding, boundary cases and source-of-truth reconciliation where applicable.

### Proposal

Correct customer/quote/product/insurer, declarations, dynamic questionnaire, nominee, medical/occupation/income details and correlation identifiers.

### Payment

Success, failure, abandonment, timeout, duplicate/delayed callback, replay, retry, debit-with-acknowledgement-loss, insurer failure after payment and reconciliation mismatch.

### Policy issuance

The platform must distinguish proposal submitted, underwriting pending, payment success, insurer approval, policy-number generation, document availability and final issuance.

> **Payment success ≠ policy issued.**

### Financial reconciliation

Where applicable, prove consistency across bank, aggregator, insurer, payment and ledger/operations records.

### PII/sensitive data

APIs, logs, events, DB, analytics, error messages, browser/client storage and notifications.

### Auditability

Actor, action, timestamp, prior/new state, source, correlation/reference and outcome.

## 2. Insurance scenario library

### Quote

Zero/one/many quotes, stale/expired quote, partial insurer failure, timeout, duplicate/invalid response, ranking, cache and provider restrictions.

### Product ranking

Deterministic ordering, ties, missing premium/sum assured, ineligible-product exclusion and insurer/product restrictions.

### Underwriting

Pending, approved, rejected, medical required, documents required, counter-offer, loading, deferred and unknown provider status.

Unknown statuses must never silently become success.

## 3. What can be reduced

Testing may be proportionally reduced for:

- documentation-only changes;
- text/cosmetic changes with no behaviour impact;
- isolated internal refactoring with unchanged observable contract and strong lower-level evidence;
- genuinely inaccessible feature-flagged code after flag safety is verified.

The reduction itself must be justified by impact analysis.

## 4. Absolute rule

A low probability does not make a catastrophic consequence acceptable. Q0 paths require credible evidence or formal accountable risk handling; QA alone does not convert them to “safe.”
