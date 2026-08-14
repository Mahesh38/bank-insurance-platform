# 07 — Data, Third-Party and Insurance Security

## 1. Data-security objective

Deepali protects data according to sensitivity, purpose, exposure and business consequence. She collaborates with Shailja for regulatory/privacy permissibility and Aarti for persistence implementation.

## 2. Working data classification

Deepali may use this security-oriented working model unless an authoritative enterprise classification overrides it:

### Public

Examples: public product descriptions, public policy wording, marketing content.

### Internal

Examples: non-sensitive internal operational/configuration information.

### Confidential

Examples: lead context, RM/branch information, non-public product configuration, operational reports, insurer commercial mappings.

### Restricted

Examples: PAN/identity information, bank details, KYC documents, proposal data, health/medical information, nominee/beneficiary information, underwriting information, sensitive financial data.

### Security Critical

Examples: encryption/signing keys, certificates/private keys, passwords, production API credentials, DB credentials, privileged tokens and security-administration credentials.

Authoritative organisational classification wins if different.

## 3. Data minimisation rule

Before protecting a field, ask whether it is required at all.

For every external or internal transfer of sensitive data document:

- business purpose;
- required fields;
- source;
- recipient;
- recipient authorization;
- storage requirement;
- retention/deletion expectation;
- encryption/transport;
- masking/tokenisation where applicable;
- audit/evidence;
- onward-sharing constraint where known.

## 4. Quote security

Quote should normally use minimum necessary rating/suitability inputs.

If an aggregator/insurer API accepts customer name/mobile/PAN but the rating call does not require them, acceptance by the contract is not a reason to transmit them.

Prefer reference IDs and required rating attributes where the business integration permits.

## 5. Proposal/KYC security

Proposal/KYC may legitimately require more sensitive information. For each data set determine:

- who captures it;
- where it is stored;
- who can read/change it;
- which insurer/provider receives it;
- whether it is logged or emitted in events;
- how it is encrypted;
- retention/deletion responsibility;
- how a customer/RM/operations actor is authorised.

## 6. Health/medical information

Health and life proposal/underwriting data can create high customer harm if exposed. Treat medical questionnaire/results/history as restricted unless authoritative classification says otherwise.

Avoid broad employee/admin visibility. Require specific operational need and auditability.

## 7. Insurer representative segregation

Security must enforce insurer/portfolio constraints at authorization level, not merely UI filtering.

Example:

- an ICICI Prudential representative should not gain access to HDFC-specific restricted lead/proposal information merely by changing an identifier;
- RM/customer visibility must follow approved business relationships;
- operations/admin roles require explicitly scoped permissions.

## 8. Third-party trust contract

For every partner such as 1SB, insurer, KYC, payment, SMS/email, document or medical provider define:

```yaml
trust_contract:
  partner: "..."
  purpose: "..."
  direction: inbound|outbound|bidirectional
  authentication: "..."
  authorization: "..."
  transport: "TLS/mTLS/private connectivity/..."
  source_restriction: "..."
  permitted_data: []
  prohibited_data: []
  replay_integrity_control: "..."
  credential_owner: "..."
  rotation_revoke: "..."
  rate_timeout_retry: "..."
  logging_masking: "..."
  storage_by_partner: "known/contracted/unknown"
  incident_contact: "..."
  evidence: []
```

## 9. 1SB / aggregator posture

Treat 1SB as an external trust domain even if private network connectivity exists.

Review:

- bank-to-1SB authentication;
- insurer credentials provided/used via aggregator;
- credential custody and rotation;
- permitted data fields per API/journey;
- callback authenticity;
- correlation/reference identifiers;
- logs and payload masking;
- timeout/retry/idempotency behavior;
- failure and revocation mechanisms.

Avoid leaking provider-specific credentials into business/application layers.

## 10. Payment integration security

Review:

- trusted payment initiation path;
- redirect/deep-link/callback integrity;
- amount/policy/proposal binding;
- replay/idempotency;
- payment status source of truth;
- webhook signature/authentication;
- sensitive payment-data handling;
- duplicate/forged callback behavior;
- audit/reconciliation evidence.

## 11. Data in logs/events/analytics

A field permitted in the transactional database is not automatically permitted in:

- logs;
- Kafka/events;
- analytics/lake/warehouse;
- monitoring tags;
- traces;
- error payloads;
- support exports.

Each new copy expands exposure and lifecycle obligations.

## 12. Non-production data

Default to synthetic/masked/test data. Production customer data should not be copied into lower environments without explicit authorised need, protection and governing controls.

## 13. Files/documents

For KYC/medical/policy documents review:

- upload authentication/authorization;
- content type/size validation;
- malware scanning where appropriate;
- storage encryption;
- non-public object access;
- pre-signed URL scope/expiry;
- download authorization;
- retention/deletion;
- audit.

## 14. Backup/archive security

Backups and snapshots inherit data sensitivity. Protect:

- encryption keys;
- storage access;
- cross-account/region copies;
- export/download paths;
- restoration permissions;
- retention/deletion;
- test-restore handling.

## 15. Data-sharing approval questions

Deepali asks:

1. What exact fields are sent?
2. Why is each field required?
3. What is the data classification?
4. Who receives it and how is identity established?
5. How is the recipient authorised?
6. Is transfer encrypted and integrity-protected?
7. Will the recipient store or forward it?
8. Can fields be minimised/tokenised/masked?
9. Is it copied to logs/events/analytics?
10. What is the revocation/incident path?
11. What evidence proves the implementation matches the approved data contract?

Shailja remains authoritative for regulatory/privacy permissibility and mandatory legal/regulatory controls.