# Building blocks field guide

Cross-LOB utilities. Portal hub: [Building blocks](https://docs.1silverbullet.tech/docs/insurance/building-blocks/apiDocs/insurance-gateway-api).

---

## Master Lookup

Working demo paths (2026-09-11):

| LOB | Path |
|-----|------|
| Term (default) | `POST /insurance/lifeterm/v1/master/lookup` |
| Saving / ULIP | `POST /insurance/lifesave/v1/master/lookup` |

`POST /v1/master/lookup` 404s on demo — do not use it. There is no `/insurance/lifeulip/…/master/lookup`.

Bank API remains `POST /v1/master-data/lookup` (`lob` selects the 1SB path). Request body includes `lookUpCategory`, `entityIds[]`, optional `manufacturerId`, and distributor (`distributorID`, `channelType`, `salesChannel`).

| Field | Required | Why |
|-------|----------|-----|
| `lookUpCategory` | Yes | `quote` or `proposal` — context of enums |
| `entityIds[]` | Yes | Which masters to fetch |
| `manufacturerId` | Optional | **Proposal only** — insurer-specific enum variants. Portal: “Manufacturer ID of which need to find the enum value for proposal.” |

Common 1SB entityIds (provider keys, **not** UI fields): `CHANNEL`, `EDUCATN`, `GENDER`, `MARITAL`, `MEMTYPE`, `OCC`, `PPF`, `PPO`, `QUOTECAT`, `QUOTETYP`, `RELWTHLA`, `STATE`, `TITLE`, `TOBACCO`.

### Does master data change by insurer? (portal 2026-09-13)

1SB does **not** document one master list that varies by insurer for every entity. It documents **two contexts**:

| `lookUpCategory` | Insurer-specific? | How 1SB says to call it |
|------------------|-------------------|-------------------------|
| `quote` | **No (gateway-common).** `manufacturerId` is not described for this context. Entity descriptions are “supported by the system” (the 1SB gateway), e.g. gender, tobacco, quote type/category. Treat as Integration Hub **common** masters once mapped to bank language. | `entityIds` + `quote` |
| `proposal` | **Yes.** `manufacturerId` exists specifically to fetch that manufacturer’s proposal enums. | `entityIds` + `proposal` + `manufacturerId` |

Product UI, gate criteria, and the proposal **form schema** are separately keyed by `productId` + `manufacturerId`. Those are product/insurer forms, not this master-lookup entity list.

Live demo has not compared quote lookup with vs without `manufacturerId` (auth 500 as of 2026-09-13). Quote-common is documented; `ASM-014` tracks proof on sandbox.

### Anti-corruption — UI / BFF never see 1SB codes

Hop is **UI → BFF → Integration Hub**. The Hub may call 1SB (or later a direct insurer) behind `adapter.*`. Do **not** suggest or build:

- frontend or BFF calling 1SB master lookup
- forwarding 1SB `entityIds` / 1SB enum strings as the BFF/UI contract

Cache 1SB (or insurer) codes **inside the Hub** so the adapter can send values the provider understands. Expose to BFF/RM UI only **bank-owned** codes and labels (and translate). Quote-category Hub masters can be one list. Proposal-category Hub masters are keyed by insurer.

Today `POST /v1/master-data/lookup` still returns provider-shaped `{code,label}` (often `code ==` 1SB string). That is a Hub-internal feed, not the target BFF contract (`SUG-20260913-hms` parked).

**Bank rule:** Never hardcode dropdowns in the RM UI from 1SB OpenAPI examples. Hub cache TTL + insurer key for proposal masters.

---

## Get Requirement

**Path:** `POST /insurance/:apiId/getReq`

| Field | Required | Why |
|-------|----------|-----|
| `distributorID` | Yes | Tenant |
| `insuranceCompanyCode` | Yes | Insurer |
| `applicationNo` / `policyNo` / `quoteId` | Situational | Locate application |
| `memberDetails` | Optional | Whose requirement |

Response lists requirements with `status`, `category` (Document / Medical…), `name`, `description`, dates, `manuRequirementNo`.

Drive RM task queues from this API after status enters requirements-pending.

---

## Document upload / download

| API | Purpose |
|-----|---------|
| Doc upload | Push KYC / income / medical docs against requirement |
| Doc download | Retrieve documents / acknowledgements |

Mandatory fields typically include distributor, insurer, application identifiers, document metadata, and content/reference. Exact schema: portal pages `doc-upload-...`, `doc-document-...`.

Store bank-side document id ↔ manufacturer requirement number mapping.

---

## OTP send / validate

Used for customer consent / verification steps in insurer journeys.

| API | Role |
|-----|------|
| Get OTP | Trigger OTP to mobile/email on file |
| Validate OTP | Confirm code |

Keep OTP UX in bank app; adapter passes identifiers 1SB expects. Do not log OTP codes.

---

## CKYC

Health CKYC consumer request pulls CKYC details for proposal/KYC stages. Prefer bank CKYC if already available and permitted; otherwise use 1SB CKYC and map into proposal prefill.

---

## Penny drop

Bank account verification before payout/mandate setup. Required fields typically include account number, IFSC, name — confirm portal `penny-drop` schema.

---

## Customer info

Fetch/validate customer information building block — use when insurer requires refreshed customer profile beyond CIF prefill.

---

## Get SP data

Validate salesperson / SP codes before quoting. Essential for RM-assisted: fail fast if RM’s mapped insurer code is invalid for a manufacturer.

---

## Wiring into ports

| Building block | Port |
|----------------|------|
| Master lookup / motor lookers | `MasterDataPort` |
| Requirements | `RequirementPort` |
| Docs | `DocumentPort` |
| OTP / CKYC / penny drop / customer info | `IdentityVerificationPort` |
| SP data | `AgentPort` |
| Payment URL / intimation | `PaymentPort` |
| Application status | `StatusPort` |
