# Validate Otp | 1SB Developer Portal

Headings: ['Validate Otp\u200b']

## Fields (16)
- `Distributor` (string)
  - Possible: <= 20 characters
  - Return a JSON object with latitude and longitude properties to point to location of user. | Distributor ID assigned to each consumer by 1SB.
- `agentID` (string)
  - Possible: <= 20 characters
  - PoSP code or SP code or Brokder Qualified Person.
- `salesChannel` (string)
  - Possible: <= 20 characters
  - This is used by insurance companies to identify sales channel.
- `varFields` (string)
  - Possible: <= 20 characters
  - varField array hold any additional data related to Distributor | Name of additional fields
- `fieldValue` (string)
  - Possible: <= 20 characters
  - Value of additional fields
- `insuranceCompanyCode` (string)
  - Possible: <= 15 characters
  - The code to identify for which insurance company this proposal belongs to.
- `applicationNo` (string)
  - Possible: <= 100 characters
  - Reference number generated on submitting the proposal.
- `policyNo` (string)
  - Possible: <= 100 characters
  - Policy reference number.
- `quoteId` (string)
  - Possible: <= 100 characters
  - Reference number generated on submitting the proposal.
- `UITrackingRefNo` (string)
  - UI tracking Reference Number .
- `OTPDetails` (string)
  - Possible: <= 10 characters
  - Enter OTP.
- `OTPTransactionId` (string)
  - OTP Transaction Id.
- `varFields` (string)
  - Possible: <= 20 characters
  - varField array hold any additional data | Name of additional fields
- `fieldValue` (string)
  - Possible: <= 20 characters
  - Value of additional fields
- `OTPAccepted` (boolean)
  - Yes/No.
- `reason` (string)
  - Field populate only when OTP Accepted = No.