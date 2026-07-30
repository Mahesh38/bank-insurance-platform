# Motor quote — deltas from life journeys

**API:** `POST /insurance/motor/v1/quote`  
**Portal:** [Motor Get quote](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/motor-consumer-request-insurance-v-1-consumer-insurance-post)

Motor is asset-centric. Do not reuse Term member-risk mental model alone.

## Controls

| Field | Required | Values / why |
|-------|----------|--------------|
| `typeOfQuote` | Yes | Single / Multi |
| `quoteCategory` | Yes | **`New`** or **`Roll-Over`** (business type, not SA/premium) |
| `distributor.*` | Yes | Same bancassurance block |
| `product` | Yes | Always `motor` |
| `motorProductType` | Yes | e.g. `2W-Pvt`, `4W-Pvt` |
| `planOption` | Yes | Plan/cover shape (Comprehensive / TP / etc. — follow portal enums) |

## Vehicle & policy inputs (core required set)

| Area | Required fields (typical) | Why |
|------|---------------------------|-----|
| Registration | `registrationDate`, `rtoLocationCode` | Territory / age of vehicle |
| Vehicle identity | `vehicleMakeCode`, `vehicleModelCode`, `vehicleVariantCode`, `vehicleFuelType` | Rating masters |
| IDV | `idvDetails` | Insured declared value |
| NCB | `ncbDetails`, `ncbPercentageLastYear` | No-claim bonus |
| Previous policy (rollover) | `previousPolicyDetails`, end/OD/TP expiry dates | Continuity / transfer |

Use **Motor Looker APIs** to populate codes (do not free-type make/model):

1. Vehicle type  
2. Make  
3. Model  
4. Fuel type  
5. Variant  
6. RTO code  
7. Financier (if hypothecated)

## Extra motor journey APIs

| API | Why |
|-----|-----|
| Motor Payment URL | Pay |
| Motor Proposal Status / Status Poll | Track issuance |
| Motor Download Policy | Policy document |

## Adapter tips

- Canonical `Vehicle` + `PreviousInsurance` objects → motor quote mapper.
- For `New`, previous policy block may be empty/optional; for `Roll-Over`, expiry dates are mandatory.
- Cache looker master data with TTL; it is high-churn UI dependency.
