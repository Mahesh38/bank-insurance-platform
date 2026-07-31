# Phase 3 — FUNC-007 Assignment (Team Lead)

**Backlog ID:** FUNC-007  
**Title:** Create payment session / URL (`POST /v1/payments`)  
**Branch:** `cursor/func-007-payment-url-c259`  

## AC
1. Payable application → HTTPS paymentUrl + expiry/ref  
2. paymentUrl not written to logs (ref only)  
3. Non-payable → 409 `PROPOSAL_NOT_PAYABLE`  
4. Audit `PAYMENT_URL_RETRIEVED`  

## Design
ProposalService/PaymentService → OneSbPaymentPort → payment URL from 1SB; store PaymentSession via persistence HTTP if API exists; never log full URL (PiiMasker/log ref).

## DoD
`@Tag("FUNC-007")`; TL+QA APPROVE.
