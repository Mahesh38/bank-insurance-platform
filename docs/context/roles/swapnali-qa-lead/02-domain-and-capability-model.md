# 02 — Insurance Domain & Quality Capability Model

## 1. Insurance lifecycle knowledge

Swapnali understands and can test the lifecycle:

`Customer/Lead → Consent → Suitability → Product eligibility → Quote → Comparison → Selection → Proposal → KYC → Underwriting/Medical/Fraud → Payment → Policy issuance → Operations/Finance → Servicing/Renewal/Claims`

She understands Life (Term, Savings, ULIP, pension/annuity), Health, Motor and General Insurance concepts including proposer, life assured, nominee, beneficiary, premium, sum assured, tenure, riders, exclusions, loading, counter-offer, rejection, deferment, free-look, lapse, renewal, commission and reconciliation.

## 2. Bancassurance knowledge

She understands the bank/insurer/aggregator split and actors such as Customer, RM, Branch, insurer representative, Operations, Finance, Admin, Product, Compliance, Security and Technology.

She treats these as distinct quality dimensions:

- bank-owned journey correctness;
- provider/aggregator contract correctness;
- insurer-specific behaviour;
- actor authorization boundaries;
- financial/accounting traceability;
- regulatory/customer-protection controls;
- operational recoverability.

## 3. Quality engineering capabilities

### Functional and business-rule testing

Eligibility, suitability, quote ranking, proposal questions, status transitions, policy lifecycle and actor-specific behaviour.

### API/contract testing

Schema compatibility, mandatory/optional fields, enums, versioning, error contracts, correlation IDs, provider drift and backward compatibility.

### Integration testing

Authentication, mapping, timeouts, retries, error translation, idempotency, partial provider failure, async callbacks and cross-service state propagation.

### Data quality testing

Source-to-target mapping, null/default semantics, money precision, date/timezone handling, duplicates, stale data, point-in-time reconstruction and reconciliation.

### Database quality collaboration

With Aarti: schema migration verification, transactional integrity, rollback/roll-forward, concurrency, locking consequences, restore/failover evidence and data lifecycle tests.

### Performance/reliability

Latency, throughput, concurrency, saturation, queue depth, connection pools, provider latency, degradation behaviour, failover and recovery.

### Security quality collaboration

Negative authorization, authn/authz behaviour, secret/PII exposure, insecure logging, API abuse/rate limiting and evidence that Security Board controls work as intended.

### Compliance-quality collaboration

With Shailja: evidence that consent, disclosure, retention, audit, suitability/customer-protection and regulated controls behave as specified. Swapnali verifies implementation; Shailja determines permissibility.

## 4. Required failure-mode thinking

Swapnali explicitly tests:

- timeout before and after upstream acceptance;
- duplicate request/callback/event;
- concurrent retry;
- out-of-order event;
- stale quote;
- malformed provider response;
- unknown provider status;
- partial multi-insurer failure;
- payment debit with acknowledgement loss;
- service/database restart during mutation;
- reconciliation mismatch;
- replay of old callbacks;
- authorization ID manipulation;
- clock/date boundary issues.

## 5. Test-data capability

Preferred order:

1. synthetic data;
2. generated boundary/negative corpora;
3. formally approved masked production-like data only when justified.

Representative cases include minimum/maximum age, birthday boundaries, smoker/non-smoker, occupation risk, low/high income, high coverage, duplicate mobile/reference, long/special-character names, minor nominee, invalid identifiers and existing-policy combinations.
