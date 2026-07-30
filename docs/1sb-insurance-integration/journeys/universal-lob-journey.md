# Universal LOB journey (and where LOBs differ)

## Universal stages

```text
[1] Intake & suitability
        │
        ▼
[2] Eligibility / gate criteria (when LOB supports it)
        │
        ▼
[3] Create quote (Single or Multi) ──poll──▶ Quote offers ready
        │
        ▼
[4] Compare / select offer (+ plan details / BI / brochure)
        │
        ▼
[5] Get dynamic proposal form → capture → submit → poll proposal
        │
        ▼
[6] Underwriting & requirements (docs, medicals, CKYC, OTP, inspection)
        │
        ▼
[7] Customer acceptance / counter-offer handling (as needed)
        │
        ▼
[8] Payment URL → pay → (intimation) → status = payment success
        │
        ▼
[9] Policy issued → download / welcome kit tracking
```

This matches the bank’s stated mental model and 1SB’s building-block status vocabulary.

---

## Stage-by-stage: APIs and ownership

| Stage | Bank-owned | 1SB APIs (typical) |
|-------|------------|--------------------|
| 1 Intake & suitability | CIF prefill, need analysis, disclosures | Master lookup for enums |
| 2 Gate criteria | Render dynamic eligibility form | GET/POST gateCriteria (life products) |
| 3 Quote | Orchestrate CreateQuote | POST quote + GET poll |
| 4 Select | Comparison UX, RM notes | Product UI data / View Plan Details / BI flags |
| 5 Proposal | Dynamic form renderer | GET proposal form + POST submit + proposal poll |
| 6 UW/requirements | Task list for RM/customer | Get Requirement, Doc upload/download, CKYC, OTP, status |
| 7 Acceptance | Consent capture | Status (`COUNTER_OFFER`, `AWAITING_CLIENT_APPROVAL`, …) |
| 8 Payment | redirectUrl landing + reconciliation | Payment URL (+ LOB variants), Payment Intimation |
| 9 Issuance | Policy vault in bank | Status `POLICY_ISSUED`, download APIs |

---

## Async pattern (quotes & proposals)

1SB quote/proposal creation is **asynchronous**:

1. POST returns / associates a `reqId`
2. Client polls until `isPollComplete` is `true` (quote poll) or proposal poll completes
3. Partial errors may appear per `productId`/`manufacturerId` even when overall poll completes

**Bank adapter should:**

- Create an internal job id immediately
- Poll with backoff (e.g. 1s → 2s → 5s, cap ~30–60s; confirm SLAs with 1SB)
- Surface partial insurer failures without failing the whole multi-quote

---

## Term (LifeTerm) journey specifics

**Path prefix:** `/insurance/lifeterm/v1`

| Step | Method & path |
|------|----------------|
| Quote | `POST /insurance/lifeterm/v1/quote` |
| Quote poll | `GET /insurance/lifeterm/v1/quote/poll/:requestId` |
| Gate criteria get | `GET /insurance/lifeterm/v1/quote/gateCriteria?productId&manufacturerId` |
| Gate criteria submit | `POST .../gateCriteria` (see docs slug `post-gatecriteria-form-...`) |
| Product UI data | `GET /insurance/lifeterm/v1/master/getproductuidata?productId&manufacturerId` |
| Proposal form | `GET /insurance/lifeterm/v1/proposal?...` |
| Submit proposal | `POST /insurance/lifeterm/v1/proposal` |
| Proposal poll | GET proposal response by product/manufacturer/request ids |
| Payment / status | Building blocks (`/v1/payment/url`, `/LifeTerm/prostat/`, …) |

**Extra vs universal:** gate criteria + BI (`includeBI`) + death benefit payout options / riders / ROP addons.

---

## Health journey specifics

**Path prefix:** `/insurance/lifehealth/v1` (quote confirmed)

| Extra capability | Why |
|------------------|-----|
| Multiple insured members + relationships | Family floater |
| `healthProductType` (Family Floater, Affinity, …) | Product family |
| View Plan Details | Network list, brochure, policy wording |
| Health configuration for distributor | Catalog constraints |
| Health Payment URL API | LOB-specific payment initiation |
| CKYC building block | Stronger KYC coupling |

`quoteCategory` is **Sum Insured** oriented (not Premium/SA/Income like Term).

---

## Motor journey specifics

**Path prefix:** `/insurance/motor/v1` (quote confirmed)

| Extra capability | Why |
|------------------|-----|
| Vehicle master lookers (type → make → model → fuel → variant, RTO, financier) | Rating inputs |
| `quoteCategory` = New \| Roll-Over | Business type |
| Registration / IDV / NCB / previous policy dates | Motor UW inputs |
| Motor Payment URL, Proposal Status, Status Poll | LOB ops |
| Motor Download Policy | Issuance artifact |

Motor is the largest deviation from life-style journeys because of asset masters.

---

## Saving / ULIP / Annuity / Pension

Same **life-style** skeleton as Term (quote → poll → gate criteria → product UI → proposal → poll), with LOB-specific quote fields (premium/investment options, annuity options, ULIP fund list/performance APIs).

| LOB | Confirmed quote path |
|-----|----------------------|
| Saving | `POST /insurance/lifesave/v1/quote` |
| ULIP | Under Saving category (`ulip-list`, `ulip-performance` APIs) |
| Annuity / Pension | Mirror Term/Saving structure; confirm exact prefix with 1SB RM (`lifeannuity` / `lifepension` style expected) |

---

## Group / Embedded

Separate catalog for group health / embedded combine-submit flows. Defer unless bank launches affinity/group in phase 1. Pattern still: consumer request → poll → proposal → status.

---

## RM-assisted overlay (all LOBs)

Regardless of LOB, bank journey should add:

1. RM login / branch context → maps to `distributor` + `agentID` + `channelType=B2B`
2. Customer search from CIF → prefill Party context
3. Suitability checklist (bank) before quote
4. Joint screen: RM drives inputs; customer OTP/payment on customer device when needed
5. RM task queue for `REQUIREMENTS_PENDING` / document collection
6. Immutable audit: who changed what before submit

```text
RM App                         Customer App
  │                                │
  ├─ suitability + quote ──────────┤ (view offers)
  ├─ proposal assist ──────────────┼─ e-sign / OTP
  ├─ requirement chase ────────────┼─ upload docs
  └─ track status ─────────────────┴─ pay via paymentUrl
```

---

## Status-driven branching

Use Application Status as the post-submit orchestrator:

| Status (1SB) | Bank next action |
|--------------|------------------|
| `REQUIREMENTS_PENDING` / `DOCUMENT_UPLOAD_*` | Open requirement tasks |
| `UNDERWRITING` / `SCRUTINY` / `PRE_CONVERSION` | Show waiting state |
| `COUNTER_OFFER` | RM + customer decision flow |
| `ACCEPTED` / `PAYMENT_INITIATED` | Create payment session |
| `PAYMENT_SUCCESS` | Wait issuance / intimation retry |
| `PAYMENT_FAILURE` / `PAYMENT_INTIMATION_FAILED` | Retry paths |
| `POLICY_ISSUED` | Fetch docs, close journey |
| `REJECTED` / `CANCELLED` / `PROPOSAL_ERROR` | Terminal / restart rules |
