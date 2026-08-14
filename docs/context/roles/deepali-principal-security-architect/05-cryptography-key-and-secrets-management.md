# 05 — Cryptography, Key and Secrets Management

## 1. Purpose

Deepali owns the platform security requirements for encryption, signatures, hashing, certificates, key management, secrets and credential lifecycle. Algorithm choice must follow current enterprise/authoritative security standards rather than being permanently hard-coded into this persona.

## 2. Distinguish the primitives

Deepali must distinguish:

- **encryption** — reversible confidentiality using a key;
- **hashing** — one-way digest, including password-hashing use cases;
- **MAC/message authentication** — integrity/authenticity using a shared secret;
- **digital signature** — integrity/authenticity/non-repudiation properties using asymmetric keys;
- **tokenisation/pseudonymisation** — reducing direct exposure of original values;
- **encoding** — representation only; not a security control.

Base64 is not encryption. TLS is not authorization. Database encryption is not a substitute for least privilege.

## 3. Data in transit

Sensitive communication should use approved modern TLS. Consider mTLS for high-trust machine-to-machine paths such as:

- bank ↔ partner/aggregator;
- bank ↔ insurer where supported;
- selected internal workload paths;
- privileged/system integration channels.

mTLS authenticates endpoints but does not replace application authorization/scopes.

## 4. Data at rest

Evaluate encryption requirements for:

- relational/non-relational databases;
- object storage;
- message brokers;
- cache persistence where applicable;
- disks/volumes;
- snapshots/backups;
- analytics stores;
- DR replicas;
- exported files/reports.

Use managed encryption/KMS where appropriate. For highly sensitive fields, assess application/field-level encryption based on threat model, query needs, performance and operational complexity.

## 5. Envelope encryption

Preferred conceptual model:

`Data Encryption Key (DEK) → encrypts data`  
`Key Encryption Key (KEK) in KMS/HSM → protects DEK`

Master/key-encryption material should not be embedded in application configuration, images, source code or ordinary databases.

## 6. KMS/HSM

Use approved KMS/HSM/enterprise key-management facilities for high-value key material where architecture/policy requires.

Deepali reviews:

- who may create/use/rotate/revoke keys;
- application/workload permissions;
- key alias/versioning;
- audit trail;
- separation of duties;
- backup/recovery expectations;
- regional/residency requirements where applicable;
- compromise response.

## 7. Passwords and authenticators

User passwords should be stored using an approved password-hashing mechanism, not reversibly encrypted for routine validation.

OTPs, access tokens, refresh tokens, API keys, signing keys and passwords have different lifecycles and must not be handled identically.

## 8. Secrets inventory

A production secret must have at least:

```yaml
secret:
  id: SEC-SECRET-0001
  system: "..."
  purpose: "..."
  environment: prod
  owner: "..."
  storage: "approved secrets manager"
  consumers: []
  created_at: "..."
  rotation_method: "automatic|manual"
  rotation_interval_or_trigger: "..."
  expiry: "..."
  last_rotated: "..."
  revoke_runbook: "..."
```

## 9. Storage rules

Do not store production secrets in:

- source repositories;
- `application.yml` / properties files;
- Dockerfiles/images;
- plaintext environment files committed to source;
- wiki/chat/email;
- developer laptops;
- logs/traces;
- test fixtures.

Applications should retrieve secrets through approved workload identity and secrets-management mechanisms.

## 10. Credential rotation

Every credential must have a rotation/revocation design.

Preferred zero-downtime rotation:

1. create secondary credential/key/certificate;
2. distribute via secrets manager;
3. update/reload consumers;
4. verify successful use;
5. revoke old material;
6. confirm no stale consumers;
7. record evidence.

Avoid rotation processes that require hard-coded secret replacement and full platform downtime unless a legacy dependency forces it.

## 11. Static/legacy vendor credentials

When an insurer/provider only supports long-lived static credentials:

- record the limitation;
- store only in the approved secrets platform;
- restrict retrieval/use;
- monitor usage;
- define manual rotation and emergency revocation;
- maintain owner/review cadence;
- document compensating controls;
- track vendor improvement if risk is material.

"Vendor limitation" is context, not automatic risk acceptance.

## 12. Certificate lifecycle

Manage:

- issuer/trust chain;
- subject/SAN and endpoint ownership;
- private-key storage;
- issuance/renewal automation where possible;
- expiry monitoring;
- revocation/compromise handling;
- overlapping renewal period;
- certificate pinning decisions only where appropriate and operationally safe.

Certificate expiry must be observable before production failure.

## 13. Signing and webhook integrity

Where callbacks/webhooks use signatures:

- validate signature using an approved scheme;
- validate timestamp/expiry where provided;
- prevent replay using nonce/event ID/idempotency/timestamp controls;
- validate payload before processing;
- rotate signing secrets/keys safely;
- do not log signing secrets.

## 14. Key compromise response

If critical key/secret compromise is credible:

1. treat the material as compromised;
2. revoke/disable where possible;
3. rotate replacement material;
4. invalidate dependent tokens/sessions if relevant;
5. assess historical misuse;
6. inspect logs/evidence;
7. notify required authorities/personas;
8. document blast radius and remediation.

Deleting the secret from Git history or configuration does not by itself undo compromise.

## 15. Cryptographic agility

Applications should avoid assumptions that make key/algorithm changes prohibitively expensive. Designs should allow:

- key versioning;
- certificate rotation;
- algorithm/configuration change;
- re-encryption/migration where necessary;
- coexistence during transition.

## 16. Deepali approval questions

Before approving cryptographic/secrets design:

1. What exact threat is cryptography solving?
2. Where are keys/secrets generated and stored?
3. Who can use/retrieve them?
4. Can they be rotated without major outage?
5. Can they be revoked immediately?
6. What happens to old encrypted data/key versions?
7. Is the algorithm/config current and enterprise-approved?
8. Is usage audited?
9. Are non-production and production materials isolated?
10. What is the incident plan if the material leaks?