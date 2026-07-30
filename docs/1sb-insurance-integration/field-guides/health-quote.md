# Health quote — deltas from Term

**API:** `POST /insurance/lifehealth/v1/quote`  
**Portal:** [Health Get quote](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/health-consumer-request-insurance-v-1-consumer-insurance-post)

Health reuses the distributor + personalInformation + product envelope, with these important differences.

## Controls

| Field | Required | Values / notes |
|-------|----------|----------------|
| `typeOfQuote` | Yes | `Single Quote` / `Multi-Quote` |
| `quoteCategory` | Yes | **`Sum Insured`** (not Premium/SA/Income) |
| `distributor.*` | Yes | Same pattern; `channelType` B2B for RM |

## Members

| Field | Required | Why |
|-------|----------|-----|
| `memberType` | Yes | `Insured` / `Proposer` |
| `memberSequenceNumber` | Yes | Member key |
| `insuredRelWithProposer` | Yes | Family floater relationships (`Self`, `Spouse`, parents, etc.). Mandatory especially for Single Quote |
| `gender`, `dateOfBirth` | Yes | Rating |
| `age` | Yes (health schema) | Often required explicitly in addition to DOB |
| `zipCode` | Yes | Serviceability / zone |

Names/contact: capture for proposer at minimum (payment + CKYC later).

## Product

| Field | Required | Why |
|-------|----------|-----|
| `product` | Yes | LOB group (`health`, …) |
| `healthProductType` | Yes | e.g. `Family Floater`, `Affinity` |
| `insuranceAndProducts` / company + product codes | Conditionally | Single Quote pinning |
| `premiumPaymentFrequency` / `premiumPaymentOption` | Conditionally | Single Quote |
| `policyTerm` | Conditionally | Often 1–3 years for retail health |

## Adjacent health APIs (not quote, but same journey)

| API | Use |
|-----|-----|
| View Plan Details | Brochure, network list, policy wording, logos |
| Get Health Configuration | Distributor-level product config |
| Health Payment URL | LOB payment initiation |
| CKYC building block | KYC before/with proposal/payment |

## Adapter tips

- Model `CoveredMembers[]` explicitly in bank canonical API (family).
- Map bank relationship enums → 1SB `insuredRelWithProposer` via Master Lookup (`RELWTHLA` etc.).
- Keep sum insured as canonical money field; adapter writes health `quoteCategory=Sum Insured`.
