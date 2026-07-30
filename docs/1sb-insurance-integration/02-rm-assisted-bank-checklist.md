# RM-assisted bank integration checklist

Practical build order for an existing-customer, RM-assisted insurance platform on 1SB with replaceable architecture.

## A. Foundations

- [ ] Obtain sandbox API key/secret + `distributorID` from 1SB RM
- [ ] Whitelist bank egress IPs; configure Basic Auth secrets in vault
- [ ] Create Bank Insurance Gateway service with ports listed in architecture doc
- [ ] Implement 1SB HTTP client (timeouts, correlation ids, raw payload audit store)
- [ ] Define Journey aggregate + stage enum (bank-owned)

## B. Cross-cutting building blocks

- [ ] Master Lookup adapter + cache
- [ ] Agent/SP validation (Get SP Data) + RM→insurer code mapping table
- [ ] Application Status adapter + status normalizer
- [ ] Requirements + Doc upload/download adapters
- [ ] OTP / CKYC / penny-drop adapters (as needed for first LOB)

## C. First LOB (recommended: Term)

- [ ] Multi-Quote create + poller
- [ ] Offer normalization for comparison UI
- [ ] Product UI / BI link display
- [ ] Gate criteria get/submit (if in scope for selected products)
- [ ] Dynamic proposal form renderer + submit + poll
- [ ] Payment URL + redirect landing + status confirmation
- [ ] Payment intimation retry path
- [ ] End-to-end sandbox journey with one insurer

## D. RM experience

- [ ] RM authentication / branch context
- [ ] Customer CIF search + prefill Party context
- [ ] Suitability questionnaire (bank) before quote
- [ ] Joint journey: RM-driven data entry, customer OTP/payment
- [ ] Requirement task inbox for RM
- [ ] Full audit trail (who submitted proposal, who uploaded docs)

## E. Expand LOBs

- [ ] Health (members + relationships + plan details + health payment)
- [ ] Motor (lookers + New/Rollover + IDV/NCB + policy download)
- [ ] Saving/ULIP/Annuity/Pension as product strategy dictates

## F. Replaceability guards (do these from day 1)

- [ ] No 1SB JSON in browser apps
- [ ] Dual ids: bank ids + external refs
- [ ] Provider routing flag (`ONE_SB` only at first)
- [ ] Contract tests on ports with fake adapter
- [ ] Golden-file tests for 1SB mappers using sanitized sandbox fixtures

## G. Open points to confirm with 1SB RM

- [ ] Production base URL and SLA for quote/proposal poll
- [ ] Exact path prefixes for Annuity/Pension
- [ ] Whether payment intimation is mandatory per insurer
- [ ] Application status path variants per LOB
- [ ] BI document handling and retention rules
- [ ] Data residency / PII logging constraints
- [ ] Supported insurers/products for the bank’s distributor id
