# Motor Proposal Status | 1SB Developer Portal

Headings: ['Motor Proposal Status\u200b']

## Fields (21)
- `distributor` (string) **REQUIRED**
  - Distributor and channel details initiating the proposal status request. | Unique distributor identifier.
- `agentID` (string)
  - Agent identifier (if applicable).
- `agentType` (string)
  - Agent type/category (if applicable).
- `salesChannel` (string)
  - Sales channel name.
- `channelType` (string) **REQUIRED**
  - Channel classification (e.g., B2C/B2B).
- `varFields` (string)
  - Additional distributor-level attributes. | Name of additional field.
- `fieldValue` (string)
  - Value of additional field.
- `insuranceCompanyCode` (string) **REQUIRED**
  - Insurer/manufacturer code.
- `productCode` (string) **REQUIRED**
  - Product code for which proposal status is requested.
- `customerId` (string)
  - Unique customer identifier (if available).
- `applicationNo` (string) **REQUIRED**
  - Proposal/application number for which status is being fetched.
- `UITrackingRefNo` (string) **REQUIRED**
  - UI tracking reference number generated during proposal submission.
- `policyNo` (string)
  - Policy number if already issued; otherwise null.
- `quoteId` (string) **REQUIRED**
  - Quote identifier associated with the proposal.
- `memberDetails` (date)
  - Member/customer validation details. | Date of birth of the insured/customer.
- `varFields` (string)
  - Additional request-level attributes. | Name of additional field.
- `fieldValue` (string)
  - Value of additional field.
- `reqId` (string) **REQUIRED**
  - Unique request identifier generated for the proposal status request.
- `errors` (string)
  - List of errors returned during execution. Empty array indicates success. | Error code identifier.
- `message` (string)
  - Error message description.
- `data` (object) **REQUIRED**
  - Response payload containing proposal status details.  This object may contain proposal, underwriting, payment, or policy issuance information depending on insurer response.