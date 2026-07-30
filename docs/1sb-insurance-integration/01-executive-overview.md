# Executive overview: 1SB for a bank insurance platform

## What 1SB is

1Silverbullet (1SB) is a **B2B insurance gateway / aggregator**. Distributors (brokers, corporate agents, banks) integrate once to 1SB’s standardized APIs and reach multiple insurers for:

- Multi-insurer / single-insurer **quotes**
- Dynamic **proposal forms** and submission
- **Payment** URL initiation and payment intimation
- Product literature / UI data / plan details
- Post-submission **status**, **requirements**, document upload/download
- Shared building blocks: masters, OTP, CKYC, penny-drop, customer info, SP data

Official framing: [Insurance Gateway API](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api).

## What 1SB is not (for bank design)

- Not your CRM / RM workflow / suitability engine
- Not your customer identity / CIF source of truth
- Not your long-term carrier connectivity strategy (unless you choose that permanently)
- Not a frozen schema: proposal forms and some enums are **product/insurer dynamic**

## Bank use case (stated)

| Need | Implication |
|------|-------------|
| Serve **existing bank customers** | Prefill from bank CIF/KYC; map into 1SB personalInformation / proposal fields |
| **RM-assisted** journey | `channelType=B2B`, capture `agentID` / SP / PoSP / BQP, RM session ownership in bank journey state |
| Use middleware **now** | Ship via 1SB adapter quickly |
| Build own middleware **later** | Keep bank domain API + journey state free of 1SB payload leakage |

## 1SB platform layers (as documented)

```text
┌─────────────────────────────────────────────────────────┐
│ Application Layer (optional 1SB UI / redirection flows) │
├─────────────────────────────────────────────────────────┤
│ Insurance Gateway – Retail LOBs (Term, Health, Motor…)  │
│ Insurance Gateway – Group / Embedded                    │
├─────────────────────────────────────────────────────────┤
│ Building Blocks (payment, status, KYC, OTP, docs…)      │
└─────────────────────────────────────────────────────────┘
```

**Recommendation for the bank:** integrate the **Gateway + Building Blocks APIs** and build the bank’s own UI (customer + RM). Prefer not to couple core journey state to 1SB Application Layer redirection unless a temporary MVP needs it.

## Security / access (from Infosec FAQ)

- MFA, role-based access, **IP whitelisting**
- Gateway auth: **credentials + IP whitelist + distributorId**
- Obtain API key/secret from 1SB RM; use HTTP Basic Auth

## Mental model of a transaction

Almost every LOB follows:

1. **Assess / eligibility** (bank suitability + optional 1SB gate criteria)
2. **Quote** (async poll pattern)
3. **Select quote / plan**
4. **Proposal form** (dynamic schema) → **submit** → **proposal poll**
5. **Underwriting / requirements** (docs, medicals, CKYC, OTP, inspection)
6. **Customer acceptance** (where applicable)
7. **Payment** (payment URL → customer pays → optional payment intimation)
8. **Policy issued** (status + document download)

1SB application statuses (normalized) include: `QUOTE_CREATED` → `QUOTE_SELECTED` → `PROPOSAL_*` → `UNDERWRITING` / `REQUIREMENTS_PENDING` → `PAYMENT_*` → `POLICY_ISSUED` (plus KYC, counter-offer, rejection, cancel, etc.). Full list in [application status field guide](./field-guides/application-status.md).

## Success criteria for “easily replaceable”

1. Bank services speak only **canonical contracts**.
2. 1SB request/response JSON never stored as the bank’s primary journey payload (store raw only as audit/adapter artifact).
3. Journey state machine is bank-owned and mapped to 1SB statuses.
4. Product/insurer identifiers are dual-keyed: bank product id + external (`manufacturerId`/`productCode`).
5. Dynamic forms are rendered from a **form schema port**, not Term-only hardcoded screens.
