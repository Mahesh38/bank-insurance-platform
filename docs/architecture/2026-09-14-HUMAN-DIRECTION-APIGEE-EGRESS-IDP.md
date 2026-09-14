# Human architecture direction — Apigee egress, env model, identity (2026-09-14)

**Standing:** Record of human Architecture-owner direction that `ADR-020` now binds.
Does **not** close `SPIKE-001` remaining written answers (edition, private URL, per-env
IPs, per-API onboard). Does **not** manufacture T4 or Deepali acceptance. If this file
disagrees with an ADR, the ADR wins (`HA-02`).

**Source:** human follow-up to the R0 E2E teaching open items.  
**Triage:** `SUG-20260914-egr` · `SUG-20260914-uat` · `SUG-20260914-idp`.  
**Assumptions:** `ASM-015`…`ASM-019` (and `ASM-013` invalidated as “all inbound **and** outbound”).

---

## 1. The picture that was confusing, restated

Two different “outsides” were being mixed:

| Direction | Who initiates | What the bank actually uses | What R0 had drawn |
|---|---|---|---|
| **Inbound** (RM / mobile / PG **callback**) | The internet / customer device / PG | Cloudflare → F5-XC → **then AWS**. We **want Amazon API Gateway** as the first AWS hop | `ADR-018` — keep |
| **Outbound** (1SB, SMS, bank internal APIs) | Our pod | **Apigee**, then the real target. 1SB allowlists **Apigee’s** IPs | `ADR-010` NAT EIPs — **contested**; do not publish our EIPs to 1SB |

`1sb-integration-service` must **never** call `https://demo.api.1silverbullet.tech/...` (or the
prod equivalent) from EKS. It calls an **Apigee proxy** that the Apigee team has onboarded.
Apigee calls 1SB. The Integration Hub still sits in front of the adapter. Java still never
speaks 1SB from Quotation.

Internal bank APIs (example: the existing service that checks an RM username/password against
AD) also go **through Apigee**, but that hop must stay **private**. Hairpinning those calls
out to the internet so they re-enter through Cloudflare + F5 is the latency and exposure
defect to refuse.

```text
Inbound (RM / NIP-APP)
  device → Cloudflare → F5-XC → Amazon API Gateway → Internal ALB → nip-web / NIP BFF

Outbound — external partner (1SB)
  1sb-integration-service → Apigee (configured proxy) → 1SB
  1SB allowlist = Apigee egress IPs, not our NAT EIPs

Outbound — internal bank API (AD verify, EBS/CBS, …)
  our service → Apigee (private target) → bank API
  must NOT: our service → IGW → Cloudflare → F5 → bank API
```

---

## 2. What this does to the five open items

### 2.1 Egress / 1SB IP whitelist

The live question is no longer only “spoke AWS Network Firewall vs EDGE FortiGate”.

The load-bearing question is: **who presents an IP to 1SB?**

| If we still publish **our** inspection-VPC NAT EIPs | 1SB allowlists **us**. That is `ADR-010` / `DEP-20260824-eip`. |
| If Apigee is the only caller 1SB sees | 1SB allowlists **Apigee**. We never publish spoke EIPs to 1SB. |

Human direction is the second row. Until the Apigee team writes the product name, the private
URL, and the egress IP list per environment, **do not** send 1SB a list of our NAT addresses
(`C-01` still blocks EIP publication; the likely close is “no spoke EIP for 1SB”).

`ADR-010` is **not** silently withdrawn. Deepali still owns whether a spoke firewall remains
on the short hop **pod → Apigee**. That is a different control from “1SB sees our IP”.

### 2.2 Apigee vs Amazon API Gateway

**Split, do not swap.**

- Ingress from RM / mobile: **keep AWS API Gateway** (`ADR-018`). Apigee might or might not
  also sit on that path later; we do **not** redraw ingress until `SPIKE-001` written answers
  exist. Do not call Apigee from Flutter.
- Egress from the building: **will** go via Apigee. Adapter HTTP clients get an Apigee base
  URL, not the 1SB origin.

`SPIKE-001` remaining written answers (bank API platform + network, not Java):

1. Apigee edition (X / hybrid / on-prem) and how NIP is onboarded as a product.
2. **Private** connectivity from the insurance spoke to Apigee (no Cloudflare/F5 hairpin).
3. Egress IPs 1SB (and PG, if outbound) will whitelist, per environment.
4. Onboarding ticket per 1SB path (`/insurance/lifeterm/v1/quote`, …).
5. PG **callbacks** stay inbound API Gateway unless written otherwise.

Keep Apigee **off the pictures** until those five exist. Coding may use a **configurable**
outbound base URL so the origin is not hardcoded.

### 2.3 Dev inside UAT

Accepted intent: **one UAT AWS account**, `dev` as a slice inside it (bank Onboarding v1.4
default, cost). Not a silent second Control Tower account.

Isolation that must still exist inside that account (otherwise `dev` wipes `uat`):

- EKS namespaces and NetworkPolicies
- IAM roles / IRSA
- Aurora database or schema names, credentials, Flyway history
- Valkey key prefix, MSK topic prefix
- Apigee product / environment (dev vs uat proxies)
- Data: synthetic in `dev`; no production-like PII on the `dev` slice
- Spring profiles remain `dev` vs `uat` even when the account is shared

LLD BOM #1 still says a separate `dev` account — that amendment is **admitted follow-up**,
not done in this file. Cloud still vendors the account.

### 2.4 CUG

**Not required for R0.** Do not invent a fourth production. If the onboarding form still
lists CUG as mandatory, request a **waiver**, do not provision an empty CUG account.

### 2.5 Keycloak vs final IdP

Two planes, already in doctrine (`15` ID-12, `TI-01`):

| Who | Source of truth | How we authenticate | Never |
|---|---|---|---|
| RM / bank employee | **Bank AD** | Existing bank API that verifies username/password against AD, reached **via Apigee on a private path**. We do **not** bind LDAP to AD | Mastering AD users in Keycloak; connecting AD from EKS |
| Insurance sales partner / IPR | **Platform** (created one-by-one or bulk, maker-checker) | IdP (Keycloak is acceptable **if** bank users never see Keycloak’s admin/login chrome) | Putting partners into Bank AD |

**Fireframe look-and-feel.** Keycloak’s default admin console and login theme are **not** the
bank UI. The bank UI is NIP-APP (and Fireframe-styled screens). That UI talks to:

- `identity-provider-adapter-service` — create/disable IdP users (partners); start the
  workforce authentication ceremony
- `identity-authorization-service` (PDP) — roles, permissions, mappings, SP certificate.
  **This** is the business source of truth, not Keycloak groups

If that mapping is done from the bank UI, **Keycloak is fine** as the private credential box.
Cognito vs Keycloak vs another OIDC provider can still change later (`ID-04`). The adapter
stays.

**Hard line (`ID-02`, `ID-03`).** The platform must not become an AD password store. If
Flutter collects the AD password and our BFF posts it to the bank verify API, that is
**credential handling** — allowed only if the bank already treats that API as the standard
app-login ceremony **and** Deepali accepts it (`ID-11`, `A3_JOINT_REVIEW`). The safer
Fireframe-compatible shape is: Fireframe/SSO UI performs the password check; NIP receives an
assertion; we never see the AD password.

Flutter still never holds an OAuth token. The BFF still hides the session.

---

## 3. What developers may do now vs not

**May**

- Keep writing Hub → adapter; keep 1SB types inside `adapter.onesb.*`
- Make the adapter’s HTTP base URL **configuration**, not a 1SB hostname constant
- Keep Amazon API Gateway as the inbound contract in OpenAPI / BFF
- Model partner provisioning APIs on the PDP + adapter, not on Keycloak’s admin REST from Flutter

**Must not**

- Call 1SB origin URLs from EKS
- Publish spoke NAT Elastic IPs to 1SB
- Route internal bank APIs out to Cloudflare/F5 and back
- Draw Apigee on HLD/LLD until `SPIKE-001` written answers land
- Put partner users in Bank AD, or bank employees as Keycloak-mastered identities
- Point Flutter at Keycloak or at Apigee

---

## 4. Owners still named

| Residual | Owner |
|---|---|
| Apigee product + private path + 1SB IP list | Bank API platform + Shivanshi; Mahesh records the ADR when written |
| Spoke firewall on pod→Apigee (`ADR-010` remainder) | Deepali accepts |
| Dev-inside-UAT LLD amendment + Cloud vending | Mahesh (LLD) + Shivanshi + bank Cloud |
| CUG waiver | Shivanshi + Mahesh |
| AD-verify ceremony vs password-in-NIP | Deepali + bank IAM; adapter stays |
