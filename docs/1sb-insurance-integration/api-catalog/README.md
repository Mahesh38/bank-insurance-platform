# API catalog

Base URL (demo): `https://demo.api.1silverbullet.tech`  
Auth: HTTP Basic (API key / secret)

Official portal entry: [Insurance Gateway API](https://docs.1silverbullet.tech/docs/insurance/retail/apiDocs/insurance-gateway-api)

---

## How 1SB docs are organized

| Area | Portal path | Use |
|------|-------------|-----|
| Retail LOBs | `/docs/insurance/retail/apiDocs/...` | Primary for bank retail platform |
| Building blocks | `/docs/insurance/building-blocks/apiDocs/...` | Cross-LOB ops |
| Group | `/docs/insurance/group/apiDocs/...` | Group/embedded |
| Application layer | `/docs/insurance/application-layer/...` | Optional 1SB UI / redirection |
| FAQ | `/docs/faq/...` | Infra / infosec / deployment |

Retail LOBs each repeat a similar operation set tagged by LOB.

---

## Shared conventions

| Topic | Convention |
|-------|------------|
| Correlation | `reqId` assigned/returned by 1SB |
| Extensions | `varFields: [{fieldName, fieldValue}]` on many objects |
| Errors | `errors[]` with `errorCode`, `errorMessage`, `errorDisplayMessage`, `errorType`, `errorIdentifier` |
| Dates | ISO-8601 `YYYY-MM-DD` |
| Distributor | Almost always required |
| Dynamic forms | Proposal + gate criteria schemas are data-driven |

---

## 1. Retail — Term (`lifeterm`)

Portal category: Term

| Operation | Method | Path | Doc slug |
|-----------|--------|------|----------|
| Get quote | POST | `/insurance/lifeterm/v1/quote` | `consumer-request-insurance-v-1-consumer-insurance-post` |
| Quote poll | GET | `/insurance/lifeterm/v1/quote/poll/:requestId` | `get-consumer-response-insurance-v-1-request-id-get` |
| Get gate criteria | GET | `/insurance/lifeterm/v1/quote/gateCriteria` | `get-gaecriteria-form-...` |
| Submit gate criteria | POST | (gateCriteria submit) | `post-gatecriteria-form-...` |
| Product UI data | GET | `/insurance/lifeterm/v1/master/getproductuidata` | `get-product-ui-data-...` |
| Get proposal form | GET | `/insurance/lifeterm/v1/proposal` | `get-proposal-form-...` |
| Submit proposal | POST | `/insurance/lifeterm/v1/proposal` | `submit-proposal-form` |
| Proposal poll | GET | proposal response path | `get-proposal-response-...` |

Query params commonly required: `productId`, `manufacturerId`, and for proposal form `version`.

---

## 2. Retail — Health (`lifehealth`)

| Operation | Method | Path / notes | Doc slug |
|-----------|--------|--------------|----------|
| Get quote | POST | `/insurance/lifehealth/v1/quote` | `health-consumer-request-...` |
| Quote poll | GET | health poll by requestId | `get-health-consumer-response-...` |
| Get proposal form | GET | health proposal form | `get-health-proposal-form-...` |
| Submit proposal | POST | health submit | `submit-health-proposal-form-...` |
| Proposal poll | GET | health proposal poll | `get-health-proposal-response-...` |
| View plan details | POST/GET (see portal) | plan literature/network | `view-plan-details-request` |
| Health configuration | GET | distributor health config | `get-health-configuration-get` |
| Health payment URL | POST | LOB payment | `health-payment-url-...` |

---

## 3. Retail — Motor (`motor`)

| Operation | Method | Path / notes | Doc slug |
|-----------|--------|--------------|----------|
| Get quote | POST | `/insurance/motor/v1/quote` | `motor-consumer-request-...` |
| Quote poll | GET | motor poll | `get-motor-consumer-response-...` |
| Status poll | GET | motor status poll | `get-motor-consumer-response-status-poll-...` |
| Proposal form / submit / poll | GET/POST/GET | motor proposal* | `get-motor-proposal-form-...`, `submit-motor-...`, `get-motor-proposal-response-...` |
| Payment URL | POST | motor payment | `motor-payment-url-...` |
| Proposal status | POST/GET | motor proposal status | `motor-proposal-status` |
| Download policy | POST/GET | policy PDF | `motor-download-policy-proposal` |
| Vehicle masters | GET chain | type → make → model → fuel → variant | `get-motor-looker-api-*` |
| RTO / Financier | GET | lookers | `...-rtocode`, `...-financier` |

---

## 4. Retail — Saving / ULIP / Annuity / Pension

| LOB | Quote path (confirmed or patterned) | Notes |
|-----|-------------------------------------|-------|
| Saving | `POST /insurance/lifesave/v1/quote` | Confirmed |
| ULIP | Saving category add-ons | `ulip-list-...`, `ulip-performance-...` |
| Annuity | Confirm prefix with 1SB (`*annuity*`) | Gate criteria + proposal mirror Term |
| Pension | Confirm prefix with 1SB (`*pension*`) | Gate criteria + proposal mirror Term |

Each has: quote, poll, gate criteria get/post, product UI data, proposal get/submit/poll.

---

## 5. Building blocks (cross-LOB)

| Operation | Method | Path | When to use |
|-----------|--------|------|-------------|
| Master lookup | POST | `/v1/master/lookup` | Enums for quote/proposal |
| Payment URL | POST | `/v1/payment/url` | Initiate insurer/gateway payment |
| Payment intimation | POST | (payment intimation API) | Notify insurer of successful pay when required |
| Application status | POST | `/LifeTerm/prostat/` (life; confirm LOB variants) | Post-submit tracking |
| Get requirements | POST | `/insurance/:apiId/getReq` | Pending docs/medicals |
| Doc upload | POST | docupload | Fulfil requirements |
| Doc download | POST | docdownload | Retrieve docs |
| Send OTP | POST | sendotp | Consent / verification |
| Validate OTP | POST | otp-verify | Complete OTP |
| Customer info | — | customer-info | Fetch/validate customer info |
| Penny drop | — | penny-drop | Bank account verification |
| CKYC | POST | health-ckyc consumer request | CKYC pull |
| Get SP data | — | get-sp-data | Validate agent/SP |

Portal index: [Building blocks](https://docs.1silverbullet.tech/docs/insurance/building-blocks/apiDocs/insurance-gateway-api).

---

## 6. Group / Application layer (secondary)

- **Group health / embedded:** quote, poll, combine proposal, submit — use when bank sells group/affinity.
- **Application layer / redirection:** 1SB-hosted journey redirection. Useful for MVP spike; not recommended as long-term system of record for a replaceable bank platform.

---

## Suggested call sequence (Term, RM-assisted)

```text
1. POST /v1/master/lookup                          # warm enums
2. (Bank) suitability + CIF prefill
3. GET  gateCriteria (optional per product)
4. POST gateCriteria
5. POST /insurance/lifeterm/v1/quote                # usually Multi-Quote first
6. GET  /insurance/lifeterm/v1/quote/poll/{reqId}   # until complete
7. GET  getproductuidata / BI as needed
8. (Bank) select offer
9. GET  /insurance/lifeterm/v1/proposal
10.POST /insurance/lifeterm/v1/proposal
11.GET  proposal poll
12.POST application status / getReq loop
13.POST docupload / OTP / CKYC as required
14.POST /v1/payment/url
15.(Customer pays)
16.POST payment intimation if required
17.POST status until POLICY_ISSUED
```

---

## Error handling pattern

Most responses:

```json
{
  "reqId": "...",
  "data": {},
  "errors": [
    {
      "errorCode": "...",
      "errorDisplayMessage": "...",
      "errorIdentifier": "...",
      "errorMessage": "...",
      "errorType": "..."
    }
  ]
}
```

Multi-quote poll may include **per-insurer** error objects (`productId`, `manufacturerId`, `listOfErrors`, `statusCode`) alongside successful offers. Adapter should convert these to per-offer failure reasons, not a single hard fail.
