# Application status

**API:** `POST /LifeTerm/prostat/` (life term path as documented; confirm LOB-specific variants with 1SB)  
**Portal:** [Application Status](https://docs.1silverbullet.tech/docs/insurance/building-blocks/apiDocs/application-status-insurance-v-1-application-status-post)

## Purpose

Unified read of:

- Application / proposal progress
- Policy details (if issued)
- Premium / payment status

## Request fields

| Field | Required | Why |
|-------|----------|-----|
| `insuranceCompanyCode` | Yes | Insurer key |
| `distributorID` | Recommended | Tenant context |
| `applicationNo` | Situational | Primary lookup after proposal |
| `policyNo` | Situational | After issuance |
| `quoteId` | Situational | Earlier stage lookup |
| `memberDetails` | Optional | Disambiguation |
| `varFields` | Optional | Extra insurer keys |

Send the best available identifier for the stage you are in.

## Response shape (conceptual)

```text
applicationNo
manufacturer[] {
  insuranceCompanyCode
  product[] {
    productCode
    applicationStatus { applicationStatus, desc, manufacturerAppStatus... }
    policyDetails { policyNo, dates, statuses... }
    premiumStatus { premiumStatus, amounts, autoPayment... }
  }
}
```

## 1SB applicationStatus enum (normalize these in bank)

`QUOTE_CREATED`, `QUOTE_SELECTED`, `QUOTE_UPDATED`,  
`PROPOSAL_APPLICATION_PENDING`, `PROPOSAL_SUBMISSION_INITIATED`, `PROPOSAL_SUBMITTED`, `PROPOSAL_MODIFICATION_REQUESTED`, `PROPOSAL_ERROR`,  
`REQUIREMENTS_PENDING`, `DOCUMENT_UPLOAD_INITIATED`, `DOCUMENT_UPLOAD_PENDING`, `DOCUMENTS_UPLOADED`, `REQUIREMENT_VERIFICATION`,  
`OTP_VERIFIED`, `PRE_CONVERSION`, `SCRUTINY`, `UNDERWRITING`, `COUNTER_OFFER`, `ACCEPTED`, `REJECTED`,  
`INSPECTION_PENDING`, `INSPECTION_APPROVED`,  
`PAYMENT_INITIATED`, `PAYMENT_SUCCESS`, `PAYMENT_FAILURE`, `PAYMENT_INTIMATION_FAILED`,  
`POLICY_ISSUED`, `AWAITING_CLIENT_APPROVAL`, `KYC_SUCCESS`, `KYC_FAILED`, `CANCELLED`

Payment sub-statuses also documented: initiated / failed / successful / intimation failed / refund.

## Bank mapping example

| 1SB status | Bank stage |
|------------|------------|
| QUOTE_* | QUOTING / QUOTE_SELECTED |
| PROPOSAL_* | PROPOSAL |
| REQUIREMENTS_* / DOCUMENT_* / OTP_* / KYC_* / INSPECTION_* | UW_REQUIREMENTS |
| UNDERWRITING / SCRUTINY / PRE_CONVERSION | UW_IN_PROGRESS |
| COUNTER_OFFER / AWAITING_CLIENT_APPROVAL | CUSTOMER_DECISION |
| PAYMENT_* | PAYMENT |
| POLICY_ISSUED | ISSUED |
| REJECTED / CANCELLED / PROPOSAL_ERROR | CLOSED |

Always store raw manufacturer substatus for RM display / ops.
