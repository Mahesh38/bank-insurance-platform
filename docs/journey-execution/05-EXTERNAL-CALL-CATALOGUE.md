# 05 — External Call Catalogue

**Every call that leaves the VPC: who makes it, how it authenticates, what it costs, and what
happens when it fails.**

Status: `AI-DRAFTED` · Owner: Mahesh (Board 1) + Shivanshi (SRE) + Deepali (Security)
Origin: `SUG-20260821-jx1`

---

## 1. The five external systems

R0 talks to exactly five things outside the VPC. Source:
[`R0-LLD §8`](../architecture/R0-LLD.md#8-external-connectivity-checklist-platform-team--bank-network).

| System | Direction | Protocol | Allowlist | Reached by | Never reached by |
|---|---|---|---|---|---|
| **1SilverBullet** | Egress | HTTPS **mTLS** | Our NAT EIPs on 1SB's list; 1SB IPs on our egress SG | `#15` adapter, via `#14` Hub | Any platform service directly. Flutter. Any bank app |
| **AU Bank PG** | Egress + Ingress + file | HTTPS / file drop | PG endpoints out; **PG source IPs only** in | `#12` Payment | The RM session path (`TB-6`) |
| **CBS / CIF** | Egress | Bank standard over Direct Connect / VPN | Bank network | `#4` Customer | Anything else |
| **Bank AD** | Egress from Keycloak | OIDC / SAML / LDAP | Phase 2 | Keycloak only | The BFF. Any business service |
| **SMS / email gateway** | Egress | HTTPS | Bank comms | `#17` Notification | Anything else |

**Two structural rules, both ArchUnit- or NetworkPolicy-enforced:**

1. **No platform service calls a provider adapter directly.** All provider traffic routes through
   the Integration Hub `#14`. (Standing constraint; `INV-ACL-01`.)
2. **No provider type crosses the adapter boundary.** 1SB wire types live only in `adapter.onesb.*`.
   A leak fails the build.

**R0 has no inbound webhook from 1SB.** We poll (`S-11`). Do not open a public webhook route
"just in case" — it is an unauthenticated ingress with no consumer.

---

## 2. 1SilverBullet — the provider

Reached only as: **domain service → `#14` Integration Hub → `#15` 1SB adapter → 1SB**.

| Call | 1SB endpoint (extracted schema) | Seam | Style | Timeout / retry | On failure |
|---|---|---|---|---|---|
| Master data | `master/consumer-request` | `S-10` | sync | `onesb.client.*` budget | typed error; read cache serves |
| Product UI data | `insurance/v1/product/{id}/manufacturer/{id}` | `S-10` | sync | `onesb.client.*` | typed error |
| Proposal form schema | `insurance/v1/proposal-form/...` | `S-10` | sync | `onesb.client.*` | typed error |
| **Quote request** | `insurance/v1/consumer-insurance` | `S-09`/`S-10` | **async-poll** | submit 3 s connect / 30 s read, **no auto-retry** | poll by external reference |
| Quote response | `insurance/v1/{requestId}` | `S-11` | poll | backoff 1 s → 30 s cap, bounded | budget exhausted → `TIMED_OUT` |
| **Proposal submit** | `submit-proposal-form` | `S-12` | **async-poll** | **no auto-retry on submit** | `SUBMISSION_FAILED` + ops task `F-04` |
| Application status | `insurance/v1/application-status` | `S-12`/`S-16` | poll | bounded | `CONFIRMATION_OVERDUE` |
| Requirements / GAE criteria | `insurance/v1/getrequirement`, `gaecriteria-form` | `S-10` | sync | `onesb.client.*` | typed error |
| Document up/download | `insurance/v1/docupload`, `docdownload` | `S-10` | sync | `onesb.client.*` | typed error |
| OTP send / verify | `insurance/v1/sendotp`, `otp-verify` | `S-10` | sync | `onesb.client.*` | typed error |
| Payment URL | `insurance/v1/payment-url` | `S-10` | sync | `onesb.client.*` | **R0 uses AU Bank PG; see §3** |

> Endpoints marked with their extracted-schema filename are documented in
> [`reference/extracted-schemas/`](../1sb-insurance-integration/reference/extracted-schemas/README.md).
> Health, Motor and Travel variants exist in that folder and are **out of R0 scope** — Term Life only.

### 2.1 Attribution on every outbound provider call

```text
ALGORITHM  hub_dispatch(canonical_request, principal)          ← C3 / INV-DIS-01 / D7
 1  ASSERT no attribution field survived from the caller       ← VR-003 already rejected at L4/L5;
                                                                 this is the defence-in-depth assert
 2  distributorId := secrets_or_config.resolve("distributorId", lob, insurerId)
 3  agentId       := principal.agentId                         ← mandatory on proposal submit (D6)
 4  spLicenceId   := principal.certification.licenceId
 5  IF any is unresolvable → 422 CONFIGURATION_UNRESOLVABLE    ← fail closed; never a default
 6  Attach all three HERE, immediately before adapter dispatch — not earlier, so no
    intermediate layer can be induced to carry a caller-supplied value.
 7  Translate canonical → provider wire model INSIDE the adapter. No 1SB type returns upward.
```

### 2.2 Partial success is success

```text
ALGORITHM  ingest_quote_offers(provider_response)              ← INV-QUO-02 / INV-QUO-03
 1  FOR each offer returned:
       IF premium.amount <= 0 OR benefits.sumAssured <= 0
          mark offer INVALID, exclude from selection, record the reason  [VR-081]
 2  IF at least one valid offer exists
       quote → QUOTED or PARTIALLY_QUOTED                      ← this is SUCCESS
       surface the per-insurer failures alongside the offers   ← the RM sees both
 3  ELSE quote → FAILED                                        ← never a zero-offer "success"
 4  A per-provider bulkhead bounds this: one failing insurer must not consume the
    connection budget that makes every other insurer look down.
```

---

## 3. AU Bank Payment Gateway — the money path

**Three separate channels, deliberately not one.** The customer pays on the hosted PG page on
**their own device**; the platform never sees a card, an account or a PG credential (`C4`, `TB-6`).

| Leg | Seam | Direction | Auth | Timeout / retry | On failure |
|---|---|---|---|---|---|
| Session create | `S-13` | egress | PG credential from Secrets Manager | 3 s / 15 s, **no retry** | `REJECTED`; proposal stays `AWAITING_PAYMENT` |
| Customer pays | — | **not our traffic** | 3-D Secure on the hosted page | — | — |
| Authorisation callback | `S-14` | **ingress** | **PG signature verified in `#12`** + IP allowlist | at-least-once | missing → `UNCERTAIN` |
| Settlement file | `S-15` | ingress / S3 drop | separate from the API path | daily + on demand | unmatched past SLA → `RECONCILIATION_BREAK` |

```text
ALGORITHM  handle_pg_callback(request)                         ← S-14; UC-24
 1  Arrives on a SEPARATE API Gateway route, IP-allowlisted to the PG.
    It is NEVER on the RM session path and carries no RM principal.     [TB-6]
 2  IF source IP ∉ PG allowlist                → drop at the edge
 3  signature_valid := verify(request.body, request.signature, pg_public_key)
    IF NOT signature_valid → 401, emit SECURITY_EVENT
    ← authenticated by the PG signature, NOT by any session            [VR-052 context]
 4  IF replay_window_exceeded(request.timestamp) → 401
 5  IF payment_store.seen(pgTxnId) → return the stored result           ← at-least-once dedup
 6  Apply the authorisation result to the Payment aggregate.
 7  A callback that never arrives does NOT become "unpaid" or "paid".
    It becomes UNCERTAIN, and only reconciliation (S-15) resolves it.   [INV-PAY-05]
```

```text
ALGORITHM  reconcile(settlement_file)                          ← S-15; UC-25; INV-PAY-05
 1  FOR each settlement record: match on pgTxnId AND amount    ← BOTH. Either alone is not a match.
 2  On match  → Payment RECONCILED
 3  No match, inside SLA → stays CAPTURED, retry next run
 4  No match, past SLA   → RECONCILIATION_BREAK, manual procedure F-07
 5  A Policy is NEVER created before its Payment is RECONCILED.         [INV-POL-01, SC-W3-4]
```

**There is no degraded mode on the money path.** Every alternative to "wait and reconcile" is a
guess about money.

---

## 4. CBS / CIF — customer identity

| Property | Value |
|---|---|
| Seam | `S-05` (reached via `S-04` from Journey → `#4` Customer) |
| Timeout / retry | 3 s, 1 retry on 5xx/connect |
| Degraded mode | Cached snapshot **inside the freshness window** only |
| On failure | Journey holds at `INITIATED`; RM sees a retryable error |
| Never | **No stale-unbounded fallback on identity data** |
| Writes | None. `#4` reads CBS; it never writes to it |

```text
ALGORITHM  lookup_customer(cif)                                ← S-04 / S-05
 1  snapshot := cache.get(cif)
 2  IF snapshot exists AND snapshot.age < freshness_window     → return snapshot
 3  result := cbs.lookup(cif)   timeout 3 s, 1 retry on 5xx/connect
 4  IF result succeeds → cache and return
 5  ELSE IF snapshot exists but is STALE
       → FAIL.  ← Do NOT return it. An unbounded-stale identity is how a policy gets
                  sold against the wrong person's KYC.
 6  → 503 typed{dependency: CBS}; journey holds at INITIATED
```

---

## 5. Bank Active Directory — via Keycloak only

| Property | Value |
|---|---|
| Reached by | **Keycloak only.** NetworkPolicy denies BFF→Keycloak and service→Keycloak (`VR-030`) |
| Protocol | OIDC / SAML / approved LDAP federation — **technology not yet confirmed** |
| R0 posture | Phase 2. R0 `dev` runs Keycloak-local users |
| Never | Direct credential forwarding, unless the bank confirms an explicitly approved LDAP/direct-grant arrangement |
| On failure | `503 IDENTITY_PROVIDER_UNAVAILABLE`. **No alternative sign-in path** (`VR-023`) |

The unconfirmed AD protocol is an **open decision**, not a gap in this pack: the adapter boundary
exists precisely so that answering it changes no BFF or authorization contract.

---

## 6. SMS / email gateway — notification

| Property | Value |
|---|---|
| Seam | `S-18`, via `#17` Notification |
| Style | **Async-event through a transactional outbox**, at-least-once |
| Carries in R0 | Consent OTP · payment link. Nothing else — broader notification is R1 |
| On failure | Raises an operations task. **Never blocks the journey** |
| Destination | The **customer's** registered contact. Never an RM or bank-employee device (`C4`) |

---

## 7. What R0 deliberately does not call

| Not called | Why | Revisit |
|---|---|---|
| Insurer callback ingress | R0 **polls** (`S-11`); an unauthenticated public ingress with no consumer is attack surface | R1 |
| A second aggregator | Multi-aggregator routing is extensibility-only until a second commitment exists | On evidence |
| Any Group B insurer API | Catalogue entry + controlled redirect only | R1 |
| 1SB payment URL | R0 takes premium through AU Bank PG on the customer device | — |
| Kafka / any shared broker | Twelve of twenty-two seams are synchronous; a transactional outbox covers the other three | S13 trigger |

---

## 8. Egress and secrets

| Concern | Rule |
|---|---|
| Egress identity | 1SB sees our **NAT EIPs**. Those EIPs are the allowlisted identity — they are stable infrastructure, not a deploy artefact |
| Credentials | AWS Secrets Manager; encryption keys in KMS. `TD-006` records that the AWS provider is still a stub |
| mTLS | Client certificate for 1SB; rotation is a named runbook item (gate `4.5`) |
| Residency | Every store, backup, log sink and archive resolves to an AWS **India** region (`INV-DAT-01`, `C6`) |
| Logging | No secret and no PII in any payload log. Raw provider payloads go to the immutable audit store, never to application logs (`C5`, `C7`) |
