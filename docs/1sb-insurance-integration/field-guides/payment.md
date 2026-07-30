# Payment URL & payment intimation

## Payment URL API

**Path:** `POST /v1/payment/url`  
**Portal:** [Payment URL API](https://docs.1silverbullet.tech/docs/insurance/building-blocks/apiDocs/payment-url-insurance-v-1-payment-url-post)

Also: Health Payment URL, Motor Payment URL (LOB-specific wrappers — prefer one bank `PaymentPort` that routes).

### Purpose

Initiate payment against an application; response returns a **paymentUrl** for redirect/browser.

### Mandatory / important request fields

| Field | Required | Why |
|-------|----------|-----|
| `AdditionalSetup.currency` | Yes | Payment currency |
| `Distributor.distributorID` | Yes | Distributor authz context |
| `insuranceCompanyCode` | Yes | Which insurer collects premium |
| `productCode` | Yes | Product identity |
| `redirectUrl` | Yes | Bank landing page after payment |
| `MemberDetails.firstName` | Yes | Payer identity |
| `MemberDetails.lastName` | Yes | Payer identity |
| `MemberDetails.mobileNumber` | Yes | Contact / PG |
| `MemberDetails.email` | Yes | Contact / PG |
| `PaymentDetails.premiumPaymentFrequency` | Yes | Premium mode |
| `PaymentDetails.amountToBePaid` | Yes | Exact amount |
| `applicationNo` / `quoteId` / `policyNo` | Situational | Correlate to proposal; send whatever 1SB/insurer issued |
| `UITrackingRefNo` | Optional | End-to-end UI trace id — **store bank journeyId here or in varFields** |
| `agentID` / salesChannel / geo | Optional | Same distribution/telemetry pattern |

### Response (conceptual)

- Echo identifiers (`applicationNo`, `quoteId`, …)
- `PaymentDetails.paymentUrl` ← open for customer
- `redirectUrl` echo

### Bank responsibilities

1. Create payment session row **before** redirect (`PENDING`).
2. Landing route on `redirectUrl` must be idempotent; confirm via Application Status, not only query params.
3. Reconcile amount against selected quote/proposal premium.

---

## Payment Intimation API

**Portal:** [Payment Intimation](https://docs.1silverbullet.tech/docs/insurance/building-blocks/apiDocs/payment-intimation-insurance-v-1-payment-url-post)

### Purpose

Notify insurer/1SB that payment succeeded (needed when PG is not fully inline with insurer confirmation, or when status shows `PAYMENT_INTIMATION_FAILED`).

### Mandatory highlights

| Field | Required | Why |
|-------|----------|-----|
| `trackInfo` | Yes | Tracking / manufacturer application linkage |
| `manufacturerQuoteID` | Yes | Insurer quote id |
| `insuranceCompanyCode` | Yes | Insurer |
| `productCode` | Yes | Product |
| `personalInformation` members | Yes | Payer/life details |
| `paymentDetails.status` | Yes | e.g. `Success` |
| `paymentMode` | Yes | UPI / Cards / NetBanking… |
| `txnAmount` / `txnDate` | Yes | Transaction facts |

### When to call

- After confirmed bank/PG success **if** journey requires intimation
- On retry when status = `PAYMENT_INTIMATION_FAILED`

Keep intimation payload construction inside PaymentPort adapter; orchestration only says `confirmPayment(journeyId, pgReceipt)`.
