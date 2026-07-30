# Canonical domain contexts

Split the problem into **bounded contexts** so request/response models stay small and purposeful. Each context has a bank-owned language; 1SB field names appear only in adapters.

## Context map

```text
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ Party           │────▶│ Suitability      │────▶│ Quotation       │
│ (Customer, RM)  │     │ Assessment       │     │                 │
└────────┬────────┘     └──────────────────┘     └────────┬────────┘
         │                                                 │
         │            ┌──────────────────┐                 │
         ├───────────▶│ Catalog          │◀────────────────┤
         │            │ Product/Insurer  │                 │
         │            └──────────────────┘                 │
         │                                                 ▼
         │                                    ┌────────────────────┐
         │                                    │ Proposal & UW      │
         │                                    └─────────┬──────────┘
         │                                              │
         ▼                                              ▼
┌─────────────────┐                          ┌────────────────────┐
│ Distribution    │                          │ Payment & Issuance │
│ (Agent/Channel) │                          └────────────────────┘
└─────────────────┘
```

---

## 1) Party context

**Purpose:** Who is buying / covered / assisting.

| Canonical concept | Description | Typical 1SB mapping |
|-------------------|-------------|---------------------|
| `Customer` | Bank CIF party | `personalInformation.individualDetails[]` |
| `MemberRole` | PROPOSER / LIFE_ASSURED / INSURED | `memberType` |
| `MemberSequence` | Stable index in journey | `memberSequenceNumber` |
| `Demographics` | DOB, gender, tobacco, income… | same-named fields |
| `Contact` | mobile, email, address, pincode | mobileNumber, email, zipCode, state, city |
| `Relationship` | insured ↔ proposer | `insuredRelWithProposer`, `relationWithFirstLifeAssured` |

**Bank rule:** Prefill from CIF; only collect gaps required by LOB / insurer.

---

## 2) Distribution context

**Purpose:** Bancassurance / RM attribution.

| Canonical concept | 1SB mapping | Notes |
|-------------------|-------------|-------|
| `DistributorId` | `distributor.distributorID` / `Distributor.distributorID` | Assigned by 1SB; required almost everywhere |
| `AgentCode` | `agentID` / `agentCode` | Insurer SP / PoSP / BQP code |
| `AgentType` | `agentType` = POSP \| SP \| BQP | |
| `ChannelType` | `channelType` = B2B \| B2C | RM-assisted → **B2B** |
| `SalesChannel` | `salesChannel` = Online \| Others | |

Use Master Lookup (`CHANNEL`, etc.) rather than hardcoding enums.

---

## 3) Catalog context

**Purpose:** What can be sold.

| Canonical concept | 1SB mapping |
|-------------------|-------------|
| `LineOfBusiness` | `product` / path prefix (`lifeterm`, `lifehealth`, `motor`, `lifesave`…) |
| `InsurerId` | `manufacturerId` / `insuranceCompanyCode` |
| `ProductCode` | `productCode` |
| `PlanOption` / `CoverOption` | nested under product |
| `Content` | getProductUIData, View Plan Details, brochures |

---

## 4) Suitability / eligibility context

**Purpose:** Bank need-analysis + insurer gate criteria.

| Bank-owned | 1SB-assisted |
|------------|--------------|
| Risk profile, goal, affordability, existing cover | `GET/POST .../gateCriteria` (Term/Saving/Annuity/Pension) |
| Product recommendation | Health configuration for distributor |

Gate criteria responses are **dynamic field groups** (same philosophy as proposal forms).

---

## 5) Quotation context

**Purpose:** Price / benefit discovery across insurers.

### Canonical commands

```text
CreateQuote(lob, mode=SINGLE|MULTI, category, party[], productIntent, options)
GetQuoteJob(quoteJobId)
SelectQuote(quoteJobId, offerId)
```

### Mapping to 1SB

| Canonical | 1SB |
|-----------|-----|
| CreateQuote | `POST /insurance/{lobPrefix}/v1/quote` |
| quoteJobId | bank id; store 1SB `reqId` as external ref |
| GetQuoteJob | `GET .../quote/poll/{requestId}` until `isPollComplete=true` |
| Offer | element inside response `quote` / product array |

### Quote mode semantics (critical)

| Field | Values | Meaning |
|-------|--------|---------|
| `typeOfQuote` | `Single Quote` / `Multi-Quote` | One product/insurer vs fan-out |
| `quoteCategory` | Term: `Premium` \| `Sum Assured` \| `Income`; Health: `Sum Insured`; Motor: `New` \| `Roll-Over` | What the customer is optimizing for / business type |

**Single Quote** requires more product parameters (policyTerm, PPT, frequency, productCode, etc.).  
**Multi-Quote** lets 1SB select eligible products from LOB + customer inputs.

---

## 6) Proposal & underwriting context

**Purpose:** Application capture, UW, requirements.

| Canonical | 1SB |
|-----------|-----|
| GetProposalSchema | `GET .../proposal?productId&manufacturerId&version` |
| SubmitProposal | `POST .../proposal` (body mirrors schema + input values) |
| ProposalJob poll | proposal poll GET with product/manufacturer/request ids |
| Requirements | `POST /insurance/:apiId/getReq` |
| Documents | doc upload / download |
| Status | Application Status API |

**Design insight from 1SB:** they deliberately avoid per-insurer submit signatures. Your bank UI should be a **dynamic form renderer** driven by schema attributes (mandatory, visibility, validation, order).

---

## 7) Payment & issuance context

| Canonical | 1SB |
|-----------|-----|
| CreatePaymentSession | `POST /v1/payment/url` (or LOB-specific Health/Motor payment URL) |
| paymentUrl + redirect back | response `PaymentDetails.paymentUrl`, bank `redirectUrl` |
| NotifyPayment (if required) | Payment Intimation |
| Policy | status `POLICY_ISSUED` + policy download (motor explicit API) |

---

## 8) Shared kernel: Journey aggregate

Bank journey aggregate (source of truth):

```text
Journey {
  journeyId
  lob
  customerId
  rmId
  stage: ASSESSMENT|QUOTING|QUOTE_SELECTED|PROPOSAL|UW|PAYMENT|ISSUED|CLOSED
  selectedOfferId
  externalRefs: { provider, reqId, applicationNo, quoteId, policyNo, insurerIds[] }
  partySnapshot
  audit[]
}
```

Never let UI treat 1SB poll payload as the journey document.

---

## Simplified request/response contexts (what to expose in bank APIs)

Keep bank BFF payloads **thin**:

### QuoteRequest (bank)

```json
{
  "journeyId": "jrn_...",
  "lob": "TERM",
  "mode": "MULTI",
  "category": "SUM_ASSURED",
  "sumAssured": 10000000,
  "members": [{"role":"LIFE_ASSURED","dob":"1990-01-15","gender":"MALE","tobacco":false,"annualIncome":1500000,"pincode":"400001"}],
  "preferences": {"paymentFrequency":"YEARLY","includeBi":false},
  "distribution": {"rmEmployeeId":"E123","insurerAgentCode":"SP001"}
}
```

Adapter expands this into full 1SB quote JSON (distributor block, enums, product section, etc.).

### QuoteOffer (bank)

```json
{
  "offerId": "off_...",
  "insurer": {"code":"HDFCLIFFE","name":"..."},
  "product": {"code":"...","name":"..."},
  "premium": {"amount":12000,"frequency":"YEARLY","tax":...},
  "benefits": {"sumAssured":10000000},
  "flags": {"outOfBound":false},
  "external": {"provider":"ONE_SB","reqId":"...","manufacturerId":"..."}
}
```

Same idea for ProposalSchema, ProposalSubmission, PaymentSession, ApplicationStatus — always bank language first.
