# Critical Information System classification — proposal (not a register entry)

**Pack:** [`ARB-PREREQUISITE-PACK.md`](../ARB-PREREQUISITE-PACK.md) row 2  
**Status:** `AI-DRAFTED` architecture proposal. **Not entered in the bank CIS register.**  
**Owners who must decide:** Shailja S (Board 6) + bank Information Security / CISO. Deepali reviews the security consequence. Mahesh does **not** classify the system into the bank register.

---

## 1. Proposal

| Field | Proposed value |
|---|---|
| System name | AU Bank Insurance Distribution Platform (NIP) — R0 assisted Life |
| Proposed class | **Critical Information System (CIS)** |
| Why not “High” or below | The platform stores and processes CIF-derived customer identity, PAN and other KYC identifiers, health/suitability attributes, payment orchestration state, and **7-year immutable audit**. A confidentiality, integrity or availability failure is a regulatory and customer-harm event, not only an operational inconvenience. |
| R0 processing location | AWS `ap-south-1` (Mumbai). DR replicas `ap-south-2` (Hyderabad) only. No regulated data outside India (`TI-08`, `FF-08`). |
| Internet exposure | Inbound: Cloudflare → F5-XC → Amazon API Gateway only (`ADR-018`). No public ALB, no public datastore. Outbound: Apigee (`ADR-020`). |
| Privileged access | IRSA per deployable; bank IAM Identity Center / PAM for humans; Keycloak admin console is not a bank-user UI. |

This matches the handling already designed for `RESTRICTED` attributes in
[`02-information-model.md`](../../platform/ws3-platform/02-information-model.md) §2.1
(Aadhaar, PAN, health, biometric, full financial detail — dedicated CMK, never in logs).

---

## 2. What would change if InfoSec classifies it lower

Nothing in the R0 control set is sized on “we are only High”. WORM audit, payment-device isolation (`C4`), fail-closed PDP (`S-02`), and India residency stay. A lower class would be a **risk acceptance**, not a design simplification, and it would have to be written by Shailja + CISO with an expiry.

A higher class (if the bank has a tier above CIS) does not add a new AWS account. It adds monitoring, change-window and VA/PT cadence — Shivanshi + Deepali.

---

## 3. Evidence the classifier should open

| Fact | Where |
|---|---|
| Objective and in/out of scope | [`BOOT.md`](../../context/BOOT.md) §5 |
| Attribute classes | Information model §2 and §4 |
| Trust boundaries | [`04-security-architecture.md`](../../platform/ws3-platform/04-security-architecture.md) §2 |
| Payment isolation | `TB-6`, `FF-14`, payment view |
| Audit immutability | `TI-07`, `ADR-013` (search is not evidence) |

---

## 4. Explicit non-claims

- This file is **not** S02-G5 (data classification schedule approved) and **not** S02-G6 (residency attested).
- GAP-016 (BA ratification of per-attribute rules) remains open; classification of the *system* can still proceed while attribute sheets are completed.
- No production go-live is authorised by this proposal.
