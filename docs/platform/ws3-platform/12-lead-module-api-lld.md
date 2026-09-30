# 12 — Lead service API LLD (cluster-private, R0)

**Status:** `AI-DRAFTED` · T3 · human Board 1 / 4 outstanding  
**Origin:** `SUG-20260930-lmd` · `EPIC-005` · `ARCH-028` · `PLAN-007`  
**Wire contract:** [`lead-service-internal.openapi.yaml`](./lead-service-internal.openapi.yaml)  
**Public edge:** [`nip-bff-lead-phase.openapi.yaml`](./nip-bff-lead-phase.openapi.yaml) (`EPIC-003`) — do not duplicate public paths here.

---

## 1. Purpose

Define the **Lead #5** cluster-private HTTP API consumed by NIP BFF, Journey, and Quotation.
Flutter never calls these paths. Persistence is via platform-common HTTP store — Lead service owns no Flyway in the 1SB adapter sense; physical schema remains Aarti’s pack.

---

## 2. Conventions (inherit platform)

| Topic | Rule |
|---|---|
| Style | REST, nouns, `application/json` |
| Errors | `application/problem+json` + ADR-017 `category` |
| Success | Bare resource — no `success/data/message` envelope |
| Ids | ULID `leadId`, `journeyId` |
| Idempotency | `Idempotency-Key` required on create and close |
| Auth | mTLS / mesh identity + principal propagation; PDP inside Lead for business grants |
| PII | Prefer references; never log PAN/full mobile |

Base path: `/internal/v1`.

---

## 3. Public BFF → Lead mapping

| Public (EPIC-003) | Lead service | Notes |
|---|---|---|
| `GET /workspace/pipeline` | `GET /internal/v1/leads` | `owner=me`, working states, cursor |
| `GET /customers:search` | `GET /internal/v1/leads:active-for-customer` (optional hop) | Lead-first; CBS is Customer |
| `GET /customers/{id}/active-leads` | `GET /internal/v1/leads?customerId&owner=me&unfinished=true` | Caller-scoped |
| `POST /leads` | `POST /internal/v1/leads` | Dedupe + journey spawn |
| `GET /leads/{leadId}` | `GET /internal/v1/leads/{leadId}` | Book-scoped get |
| *(future BFF)* assign | `POST /internal/v1/leads/{leadId}/assignments` | Pre-BI |
| *(future BFF)* close | `POST /internal/v1/leads/{leadId}/close` | |
| Quote success | `POST /internal/v1/leads/{leadId}/bi-generated` | First BI |
| JourneySold | `POST /internal/v1/leads/{leadId}/convert` or event handler | Idempotent |

Catalogue product-classes stay on BFF → Catalogue; not Lead.

---

## 4. Resources

### 4.1 `POST /internal/v1/leads`

Create Lead + spawn Journey ref.

**Request (logical):**

```json
{
  "customerId": "CUS…",
  "lob": "LIFE",
  "productClass": "TERM",
  "branchId": "BR…",
  "assignedRmId": "certified-SP-Bank-RM",
  "assignedSpId": "when-required-by-role-flow",
  "correlationId": "…"
}
```

**Guards:** `INV-LED-04/05/10`, ALG-DEDUPE, PDP `opportunity.create`, assignee SP cert.

**Responses:** `201` LeadCreated · `409` duplicate · `403` ORIGINATION_ACTOR_DENIED · `422` ASSIGNEE_SP_REQUIRED / validation.

### 4.2 `GET /internal/v1/leads/{leadId}`

Returns Lead; `404` if outside caller visibility (`INV-LED-07` / book scope) — prefer absent over confirming existence for foreign principals.

### 4.3 `GET /internal/v1/leads`

Query: `owner`, `customerId`, `productClass`, `unfinished`, `states`, `cursor`, `limit`.

### 4.4 `POST /internal/v1/leads/{leadId}/assignments`

Body: target SP/RM/branch. Reject if `biGenerated` (`VAL-016` / `BR-REASSIGN-001`).

### 4.5 `POST /internal/v1/leads/{leadId}/close`

Body: `reasonCode`, `remarks?` (max 250). Terminal. No reopen.

### 4.6 `POST /internal/v1/leads/{leadId}/bi-generated`

Body: `biReference`, `occurredAt`. Idempotent first-BI transition.

### 4.7 `POST /internal/v1/leads/{leadId}/convert`

Body: `journeyId`, `policyId`, `paymentId`. Guard `INV-LED-02`.

### 4.8 `POST /internal/v1/leads/{leadId}/archive`

Explicit archive when already terminal; may be combined with convert handler.

### 4.9 `POST /internal/v1/leads/{leadId}/activity-status`

Set configurable disposition while stage remains pre-BI (`BRD §14.2`). `CUSTOMER_NOT_INTERESTED` triggers close path.

---

## 5. Error catalogue (selected)

| Code | HTTP | BRD / INV |
|---|---|---|
| `ORIGINATION_ACTOR_DENIED` | 403 | INV-LED-04 / INV-LED-09 |
| `ASSIGNEE_SP_REQUIRED` | 422 | INV-LED-10 / `D-018` |
| `CUSTOMER_NOT_IN_BOOK` | 422 | INV-LED-05 |
| `LOB_NOT_CERTIFIED` | 422 | INV-LED-05 |
| `RM_NOT_CERTIFIED` | 422 | INV-LED-03 |
| `LEAD_DUPLICATE_UNFINISHED` | 409 | BR-DEDUPE / VAL-012 |
| `REASSIGN_AFTER_BI` | 422 | VAL-016 |
| `ILLEGAL_TRANSITION` | 409 | INV-LED-01 |
| `OPPORTUNITY_REQUIRED` | 422 | INV-LED-06 (callers) |

User-facing copy for BFF mapping uses VAL-* strings from BRD §23 where applicable.

---

## 6. What this LLD refuses

- Public `/leads` duplication (EPIC-003 owns it).
- Insurer wire status as Lead states.
- Campaign/bulk create endpoints.
- Meeting schedule resource (parked).
- Force-duplicate flag (`OPEN-LEAD-DUP`).

---

## 7. Done for this document

- [x] BFF mapping table
- [x] Internal operation set aligned to OpenAPI
- [ ] Human Board 1 / Security signatures
