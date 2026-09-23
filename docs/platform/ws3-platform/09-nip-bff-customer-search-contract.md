# 09 — NIP BFF customer search (SCR-03) — unified contract

**Workstream:** WS-3 · **Horizon:** H0 / R0 assisted Life (Term + Savings/ULIP)  
**Owner:** Mahesh (Board 1) — public contract and hop · Amit (Board 2) — BFF implementation at S11  
**Status:** `AI-DRAFTED` · T3 · human Board 1 / 4 / 6 signatures outstanding  
**Origin:** `SUG-20260923-scs` · `ARCH-025` · `PLAN-005` · `EPIC-003`  
**Screen:** `SCR-03` “Select customer to begin” (NIP-APP / `insurance.aubank.in`)  
**Authority:** this file is the **single** stakeholder + developer contract for customer search.
[`07-nip-bff-lead-phase-api-lld.md`](./07-nip-bff-lead-phase-api-lld.md) §4.2 / §6.2 and
[`nip-bff-lead-phase.openapi.yaml`](./nip-bff-lead-phase.openapi.yaml) `GET /customers:search`
are projections of this file. If they disagree, **this file wins** until a human Board 1
signature says otherwise.

This is **not** a published portal drop (S12-E06-S01) and it is **not** an implementation of
NIP BFF, Lead (#5), Customer (#4) or CBS. It is **not** the Customer BFF (`#1`, R1 / BOOT
`out_of_scope_now`).

Figma is **reference only** ([R0-SCOPE A11](../../au-bank-insurance-platform/requirements/R0-SCOPE.md#2-working-decisions-locked-unless-overturned)).
Canonical screen id: [S05 §4.3](../../application-lifecycle-bible/evidence/S05-experience-evidence.md#43-screen-inventory--closes-gap-009) `SCR-03`.

---

## 0. How to read this (one document, every seat)

Do not commission a second Product brief, a second BA note, a second OpenAPI or a second
QA sheet for the same screen. Read the row for your seat; the rest is there so the other
seats cannot silently invent a different contract.

| Seat | Must read | May skip |
|---|---|---|
| **Rajal / Product** | §1, §2, §8 empty/error copy, §12 | §9 YAML, §10 Java |
| **Principal BA** | §1, §2, §8, §12, §13 | §10 |
| **Mahesh / Architecture** | whole file except §10 is optional | — |
| **Amit / Engineering + NIP-APP** | §3, §4, §5, §6, §7, §9, §10 | Product copy tables once AC ids are wired |
| **Deepali / Security** | §3.2, §6, §7 | Java sketches |
| **Shailja / Compliance** | §1.4, §6, §12 `OPEN-SEARCH-CIF-MASK` | YAML examples |
| **Aarti / Database** | §5 (what is stored vs what is fetched) | Flutter enablement |
| **Swapnali / QA** | §8, §11 | hop rationale |
| **Shivanshi / SRE** | §7 | Product copy |
| **Kalpana / Delivery** | §0, §12, §14 | field tables |

**Spoken name is Lead** (`CR-013` / `ADR-014`). Search does not create a customer and does
not open a journey (`AC-CUST-010-2`).

---

## 1. Product and BA — what the RM is doing

### 1.1 Why this screen exists

One RM, already authenticated through the token-hiding BFF session (`ADR-015`, WS-2 `A.1`),
must identify **one ETB customer** before a Life lead can start or resume
(`BR-CUST-010`, `AC-CUST-010-1/2/3`, R0-SCOPE §3 Customer).

The business outcome of this API is:

> Given a Customer ID, PAN or registered mobile, show the RM whether this person is already
> on an insurance lead in their book; if not, show the CBS identity so they can start one.
> Never invent a customer. Never proceed when CBS is down and no local lead exists.

That is `AC-CUST-010` plus the lead-first rule stated in this intake.

### 1.2 The screen (canonical layout)

Source: human-supplied NIP-APP capture of “Select customer to begin”.

```text
+-----------------------------------------+
|  <-                                    |
|  Select customer to begin              |
|                                        |
|  Search by                             |
|  +-----------------------------------+ |
|  | Customer ID                    v  | |   by = CUSTOMER_ID | PAN | MOBILE
|  +-----------------------------------+ |
|  Enter Customer ID                     |
|  +-----------------------------------+ |
|  | 560098111                         | |   q  (never logged, never echoed)
|  +-----------------------------------+ |
|  +-----------------------------------+ |
|  |              Search               | |   enabled when by+q are valid
|  +-----------------------------------+ |
|                                        |
|  1 result found               Clear    |
|  +-----------------------------------+ |
|  | (AK)  Abhishek Kummar          *  | |
|  |       CIF XXXXX0433 • ULIP        | |   masked CIF + existing product
|  |       View details                | |
|  +-----------------------------------+ |
+-----------------------------------------+
```

| UI element | Contract field / rule |
|---|---|
| Title “Select customer to begin” | Client copy. S05 `SCR-03`. |
| Back | Client navigation to `SCR-02`. No API. |
| “Search by” dropdown | Query `by`. **R0 values: Customer ID, PAN, Mobile only.** Name search is not on this screen (`OPEN-LEAD-NAME` stays proposed on the lead-phase OpenAPI; this screen does not offer it). |
| Value field placeholder | Changes with `by`: “Enter Customer ID” / “Enter PAN” / “Enter mobile number”. |
| Search button | Client-enabled when `by` + `q` pass format. Server still validates. Disabled look (screenshot: pale) = invalid or in-flight. |
| “N result(s) found” | `query.resultCount`. Zero → S05 empty copy, not a toast. |
| Clear | Client: wipe `q`, results, selection. No API. |
| Avatar initials | `initials`, derived by the BFF from `fullName`. |
| Display name | `fullName`. |
| `CIF XXXXX0433` | `maskedCif` — last four of the CBS CIF. **Full CIF never on the wire.** |
| `• ULIP` | `existingLead.productClass` when `source=EXISTING_LEAD`. Omitted on a CBS-only hit (no insurance product yet). |
| View details | `GET /customers/{customerId}` or reuse the selected hit. Does **not** unlock PAN, DOB, address or income — that is `SCR-07` after `CNS-DP`. |
| Checkmark | Client selection of `customerId`. The only token passed into `SCR-04` / `SCR-05`. |

### 1.3 Search keys (business)

S03 named three keys (`AC-CUST-010-1`). This screen uses exactly those three.

| Dropdown label | `by` | What the RM types | Unique? |
|---|---|---|---|
| Customer ID | `CUSTOMER_ID` | Bank CIF / customer number | Yes — one person |
| PAN | `PAN` | PAN | Yes — one person |
| Mobile number | `MOBILE` | Registered mobile | Usually one; server still returns a list |

Account-number search is **out**. BR-CUST-010’s “e.g. … account — confirm” was resolved by
S03 to CIF / mobile / PAN. Do not add a fourth key here.

### 1.4 What the RM is allowed to see

`AC-CUST-010-1`: “name and **policy-permitted identifiers**”.

| Shown on the card | Not shown (confirm is identity, not prefill) |
|---|---|
| Full name | Full CIF / Customer ID (`cifNumber`) |
| Initials | PAN (even masked — RESTRICTED) |
| Masked CIF (`XXXXX0433`) | DOB, age, gender |
| Masked mobile | Address, pincode |
| Masked email (details sheet) | Income, tobacco, Aadhaar |
| Existing product class if a lead exists | Another RM’s `leadId` (`OPEN-LEAD-XRM`) |
| ETB / not-ETB eligibility | CBS raw payload, Apigee headers |

Masked CIF on this screen is a **delta** versus the 2026-09-07 lead-phase LLD, which forbade
any CIF-shaped field. It is aligned to the shipped screen and to “policy-permitted
identifiers”. Full CIF remains forbidden. Board 6 confirmation is `OPEN-SEARCH-CIF-MASK`.

### 1.5 Lead-first, then CBS — the Product rule

1. The RM searches by one key.
2. The platform first asks: **does an insurance lead already exist** for that Customer ID,
   PAN or mobile, in this RM’s book?
3. **If yes** — show that customer and the existing product class (the screenshot’s
   `CIF … • ULIP`). The RM is resuming or inspecting work, not discovering a stranger.
4. **If no** — fetch the customer from **CBS via Apigee**. Show the CBS identity with no
   product-class chip.
5. **If CBS is down and no lead exists** — S05 error: “Customer records are unavailable
   right now. **Do not proceed.**” (`AC-EXC-10`). No fabricated customer. No journey.

This is not a second search API. It is the orchestration of `GET /customers:search`.

### 1.6 Acceptance (this screen only)

| ID | Given / When / Then |
|---|---|
| `AC-CUST-010-1` | CIF, mobile or PAN → exactly one ETB match → name + policy-permitted identifiers; search audited |
| `AC-CUST-010-2` | No match → empty state; **no** customer created; **no** journey |
| `AC-CUST-010-3` | Non-ETB match → shown ineligible; Continue disabled; no journey |
| `AC-EXC-10` | CBS unreachable **and** no local lead → degraded copy; do not proceed; do not fabricate |
| `AC-SEARCH-LEAD-1` | Lead exists for the key in this RM’s book → card is returned **without** a CBS hop; `source=EXISTING_LEAD`; product class shown |
| `AC-SEARCH-LEAD-2` | No lead → BFF calls Customer #4 → Apigee (private) → CBS; `source=CBS` |
| `AC-SEARCH-LEAD-3` | Flutter never calls CBS, Apigee, Lead or a database |
| `AC-SEARCH-MASK-1` | Response has no `cifNumber`, no PAN, no full mobile, no DOB, no address |
| `AC-SEARCH-AUDIT-1` | Search is a material action (`BR-SEC-030`); audit has actor, `by`, hashed `q`, result count, `source`, `correlationId` — never raw PAN/mobile/CIF |

---

## 2. Architecture — why this hop, and nothing else

### 2.1 Governing question

How should NIP-APP find an ETB customer without becoming a CBS client, without making Lead
the customer master, and without sending PAN/CIF to a log or a Flutter model?

### 2.2 Boundaries

```text
NIP-APP (Flutter)
    │  opaque session only (ADR-015)
    ▼
#2 NIP BFF          ← public /api/v1  (this contract)
    │
    ├─► #3 PDP          authorize customer.search   fail-closed, 300 ms, no retry
    ├─► #4 Customer     local resolve, then CBS snapshot
    │       └─► Apigee (private) ─► CBS / EBS customer inquiry
    ├─► #5 Lead         “does a lead exist for this customerId?”
    └─► #16 Audit       outbox; never on the RM wait-path
```

| Rule | Consequence |
|---|---|
| Flutter talks **only** to `#2` ([HLD §5](../../architecture/R0-HLD.md#5-api-details)) | No CBS, Apigee, Lead or Customer URL on the device |
| Flutter never receives OAuth tokens | `NIPSESSION` cookie **or** `Authorization: Bearer <opaque-session>` |
| Bank apps never call a database | List/search are HTTP, never JDBC |
| 1SB / insurer schemas stop at the Hub | Not on this API |
| Outbound bank APIs leave via **Apigee** (`ADR-020`) | Customer #4 calls an Apigee **private** proxy, not CBS origin, and **not** Cloudflare/F5 hairpin ([human direction](../../architecture/2026-09-14-HUMAN-DIRECTION-APIGEE-EGRESS-IDP.md) §1–§2) |
| Inbound stays AWS API Gateway (`ADR-018`) | Flutter never calls Apigee |
| `#4` owns customer identity | Lead stores `customerId` + working fields. Lead is **not** a PAN/CIF directory |
| `#5` owns lead existence | BFF asks Lead “any own non-terminal lead for this `customerId`?” |
| BFF holds no domain decision | Masking, redaction, error mapping. Duplicate *create* conflict stays on `POST /leads` |
| `distributorId` never caller-supplied | Absent |
| No PII in logs | `q` (PAN / mobile / CIF) is not logged |

Capability ≠ bounded context ≠ deployable. Search is a **BFF orchestration** of `#4` + `#5`.
It is not a new microservice.

### 2.3 Options considered (Mahesh)

| Option | Verdict | Why |
|---|---|---|
| **A. Lead-first, then CBS via Apigee** (this contract) | **Chosen** | Matches the screen (product chip only when a lead exists); avoids a CBS hop on resume; keeps CBS the identity master; Apigee is the ratified egress (`ADR-020`) |
| B. Always CBS, then a second `active-leads` call | Rejected for this screen | The 2026-09-07 LLD. Extra latency on every search; product chip requires a second round-trip the screenshot does not have; CBS becomes a dependency even when the RM is opening their own lead |
| C. Flutter → Apigee → CBS | Rejected | Standing constraint; Flutter never holds bank-API credentials; Apigee is outbound, not the RM ingress |
| D. BFF → CBS origin (skip Apigee) | Rejected | `ADR-020`. CBS / EBS is an internal bank API; private Apigee, no hairpin |
| E. Lead stores PAN/mobile and answers search alone | Rejected | Lead is not the customer SoR ([information model §4.1](./02-information-model.md#41-customer--sor-customer-context-profile-snapshot-cbs-for-the-master)). PAN is RESTRICTED. Identifier resolution stays in `#4` |
| F. One bulk bootstrap (leads + CBS recents + catalogue) | Rejected | Unbounded PII; couples four contexts; PLAN-004 already refused this |

Reversibility: high. Public path stays `GET /customers:search`. Changing the internal order
does not break a published consumer (none exist yet). Revisit if Board 6 forbids masked CIF
or if Apigee onboarding names a different inquiry operation (`OPEN-SEARCH-CBS-OP`).

### 2.4 Sequence

```mermaid
sequenceDiagram
    autonumber
    participant App as NIP-APP
    participant BFF as NIP BFF #2
    participant PDP as PDP #3
    participant Cust as Customer #4
    participant Lead as Lead #5
    participant Apigee as Apigee private
    participant CBS as CBS / EBS

    App->>BFF: GET /customers:search?by=CUSTOMER_ID&q=…&limit=20
    BFF->>PDP: authorize customer.search (BANK_RM, in-book)
    PDP-->>BFF: allow / deny
    alt deny
        BFF-->>App: 401 / 403 public envelope
    end

    Note over BFF,Cust: Step 1 — local resolve only. No CBS.
    BFF->>Cust: GET /internal/v1/customers:resolve?by=&q=
    Cust-->>BFF: 0..n local customerId (book-scoped)

    alt local customerId found
        BFF->>Lead: GET /internal/v1/leads?customerId&assignedRmId=me&active=true
        Lead-->>BFF: 0..n own leads
        alt at least one lead
            BFF-->>App: 200 source=EXISTING_LEAD (masked; product class)
        end
    end

    alt no own lead (no local customer, or local customer with none)
        Note over BFF,CBS: Step 2 — identity from CBS. Mandatory.
        BFF->>Cust: GET /internal/v1/customers:lookup?by=&q=
        Cust->>Apigee: private customer-inquiry proxy
        Apigee->>CBS: bank inquiry (CIF / PAN / mobile)
        alt CBS hit
            CBS-->>Apigee: identity
            Apigee-->>Cust: normalised
            Cust-->>BFF: customerId + profile (full, internal)
            BFF-->>App: 200 source=CBS (masked; no product chip)
        else CBS empty
            Cust-->>BFF: empty
            BFF-->>App: 200 items=[] (AC-CUST-010-2)
        else CBS / Apigee down
            Cust-->>BFF: upstream timeout / 5xx
            BFF-->>App: 503 UPSTREAM_UNAVAILABLE (AC-EXC-10)
        end
    end
```

**Do not proceed** is only when step 2 is required and fails. A lead hit in step 1 is
enough to render the card.

### 2.5 Sync vs poll

Search is **synchronous**. There is no job id and no poll. The RM is on-screen
([communication patterns](../architecture-review/03-communication-patterns.md)).
Quote / proposal / payment polling is a later pack (`SCR-11+`).

### 2.6 What this API is not

| Tempting extra | Where it actually lives |
|---|---|
| Create / resume lead | `POST /leads` — [07 §6.6](./07-nip-bff-lead-phase-api-lld.md#66-post-leads) |
| Duplicate-create conflict | `409` on `POST /leads`, or `GET /customers/{id}/active-leads` on `SCR-05` |
| Prefill of party fields | `SCR-07` after `CNS-DP` (`AC-CUST-020-*`) |
| Pipeline inbox | `GET /workspace/pipeline` (`SCR-02`) |
| Customer self-service search | Customer BFF `#1` — R1, out of scope |
| Campaign / bulk | `ADR-005`, R1 |
| Name search | `OPEN-LEAD-NAME` — not on this dropdown |

---

## 3. Public BFF API (developers)

Base path: `/api/v1` after the inbound gateway (`ADR-018`).  
Success body **is** the resource. No `{success, data, message}` wrapper (lead-phase
convention / `SUG-20260907-std`).  
Errors: RFC 7807 `application/problem+json` = `ServiceErrorResponse.toPublic()`
([ADR-017](../../journey-execution/07-PLATFORM-ERROR-CONTRACT.md#42-public-rendering--crosses-the-trust-boundary)).

### 3.1 `GET /customers:search`

**Operation id:** `searchCustomers`  
**Actor:** `BANK_RM` only. IPR does not originate (`INV-LED-04`).  
**Idempotency-Key:** not used (read).

#### Headers

| Header | Required | Rule |
|---|---|---|
| `X-Correlation-Id` | Yes | UUID; created by the app if missing is **rejected** (`400`). Propagated to #3/#4/#5/Apigee |
| `Cookie: NIPSESSION=…` **or** `Authorization: Bearer <opaque-session>` | Yes | Opaque session. Not an OAuth access token |
| `X-Idempotency-Key` | No | Ignore if sent |

#### Query

| Name | Required | Type | Rule |
|---|---|---|---|
| `by` | Yes | enum | `CUSTOMER_ID` \| `PAN` \| `MOBILE` |
| `q` | Yes | string | 1–80 chars after trim. Validated per `by`. **Never echoed. Never logged.** |
| `page` | No | int | 0-based. Default 0. Max 4. Deep CBS pages are the wrong UX |
| `limit` | No | int | Default 20, hard max 20 |

`by` validation:

| `by` | `q` after trim / normalise | Reject with |
|---|---|---|
| `CUSTOMER_ID` | 1–20 `[A-Za-z0-9]` | `400 VALIDATION_ERROR` field `q` |
| `PAN` | Uppercased `^[A-Z]{5}[0-9]{4}[A-Z]$` | `400 VALIDATION_ERROR` field `q` |
| `MOBILE` | E.164 or 10-digit Indian → digits-only 10 starting `6–9`, or `+91` + that | `400 VALIDATION_ERROR` field `q` |
| anything else | — | `400 VALIDATION_ERROR` field `by` (includes `NAME` on **this** screen) |

Button enablement is client-side. Server still validates. Do not “helpfully” coerce a
PAN-shaped `q` when `by=CUSTOMER_ID`.

#### 200 `SearchPage`

```json
{
  "query": {
    "by": "CUSTOMER_ID",
    "resultCount": 1,
    "source": "EXISTING_LEAD"
  },
  "items": [
    {
      "customerId": "01JQX4K7R8M2N3P4Q5S6T7V8X1",
      "fullName": "Abhishek Kummar",
      "initials": "AK",
      "maskedCif": "XXXXX0433",
      "maskedMobile": "+91 933****412",
      "maskedEmail": "abh*****@gmail.com",
      "eligibility": "ETB",
      "source": "EXISTING_LEAD",
      "existingLead": {
        "leadId": "01JQX4K7R8M2N3P4Q5S6T7V8W9",
        "productClass": "ULIP",
        "lob": "LIFE",
        "state": "ASSIGNED",
        "journeyId": "01JQX4K8A1B2C3D4E5F6G7H8J9",
        "updatedAt": "2026-09-23T09:42:00Z"
      },
      "existingLeadCount": 1
    }
  ],
  "page": { "page": 0, "size": 20, "hasMore": false }
}
```

`query.source` is the **dominant** source for this page: `EXISTING_LEAD` if any item came
from a lead hit; `CBS` if every item is a CBS hit; omitted on an empty page.

One **customer** per row, not one lead per row. If the RM has two active Life leads on the
same CIF, `existingLead` is the most recently updated and `existingLeadCount` is `2`.
View details / `SCR-05` lists them. Do not render two cards with the same name.

#### Field dictionary (public)

| Field | Type | Required | Meaning | Forbidden sibling |
|---|---|---|---|---|
| `customerId` | ULID | yes | Platform id for confirm / create / resume | Not a CIF |
| `fullName` | string(140) | yes | Display name | — |
| `initials` | string(1–4) | yes | BFF-derived from `fullName` (letters only, first+last) | — |
| `maskedCif` | string | yes when known | Last-4 CIF, other digits `X`, prefix length matches SoR CIF. Example `XXXXX0433` | `cifNumber` |
| `maskedMobile` | string | yes when known | `+91 933****412` | full MSISDN |
| `maskedEmail` | string \| null | no | local-part keep 3 + `***@domain` | raw email |
| `eligibility` | `ETB` \| `NOT_ETB` | yes | Continue enabled only for `ETB` | — |
| `source` | `EXISTING_LEAD` \| `CBS` | yes | Which hop produced the row | — |
| `existingLead` | object \| null | yes | Latest own non-terminal lead, or `null` on CBS-only | another RM’s lead |
| `existingLead.leadId` | ULID | if object | Pass as `resumeLeadId` on `POST /leads` | sequential `RR 2024-001` (`OPEN-LEAD-DISPLAY`) |
| `existingLead.productClass` | `TERM` \| `SAVINGS` \| `ULIP` | if object | Chip after the CIF (`• ULIP`) | `HEALTH` (not R0) |
| `existingLead.lob` | `LIFE` | if object | R0 | — |
| `existingLead.state` | LeadState | if object | Working state | — |
| `existingLead.journeyId` | ULID \| null | if object | Null if need-analysis not started | — |
| `existingLeadCount` | int ≥ 0 | yes | `0` on CBS-only | — |
| `query.by` | enum | yes | Echo of interpreted key | `query.q` — **never** |
| `query.resultCount` | int | yes | `items.length` (not a CBS grand total) | — |
| `page.*` | offset page | yes | `hasMore` within the server cap | total-count of the bank book |

`additionalProperties: false` on every public object. Unknown fields from CBS die in `#4`.

#### Empty and non-ETB

```json
{
  "query": { "by": "PAN", "resultCount": 0 },
  "items": [],
  "page": { "page": 0, "size": 20, "hasMore": false }
}
```

Non-ETB (CBS hit, cannot start R0):

```json
{
  "query": { "by": "MOBILE", "resultCount": 1, "source": "CBS" },
  "items": [
    {
      "customerId": "01JQX4K7R8M2N3P4Q5S6T7V8X2",
      "fullName": "Ravi Verma",
      "initials": "RV",
      "maskedCif": "XXXX8871",
      "maskedMobile": "+91 982****100",
      "maskedEmail": null,
      "eligibility": "NOT_ETB",
      "source": "CBS",
      "existingLead": null,
      "existingLeadCount": 0
    }
  ],
  "page": { "page": 0, "size": 20, "hasMore": false }
}
```

Continue / select-to-create stays disabled (`AC-CUST-010-3`). Out-of-book identities are
**absent** (`INV-LED-05`, `INV-LED-07`), not listed as forbidden.

#### CBS-only happy path (no lead)

`GET /customers:search?by=MOBILE&q=9331111412&limit=20`

```json
{
  "query": { "by": "MOBILE", "resultCount": 1, "source": "CBS" },
  "items": [
    {
      "customerId": "01JQX4K7R8M2N3P4Q5S6T7V8X1",
      "fullName": "Abhishek Kummar",
      "initials": "AK",
      "maskedCif": "XXXXX0433",
      "maskedMobile": "+91 933****412",
      "maskedEmail": "abh*****@gmail.com",
      "eligibility": "ETB",
      "source": "CBS",
      "existingLead": null,
      "existingLeadCount": 0
    }
  ],
  "page": { "page": 0, "size": 20, "hasMore": false }
}
```

No `• ULIP` chip. Selecting this row goes to `SCR-04` → `SCR-05` → `POST /leads`.

### 3.2 `GET /customers/{customerId}` — View details / confirm

Same public projection as one search hit (including `existingLead` if still active).
`404` if the id is not in this RM’s book (absent, not named).  
`503` only if the BFF must re-read CBS (no usable local snapshot) and CBS/Apigee is down.

Prefer reusing the search hit while the RM has not left `SCR-03`. This GET exists for
process death and “View details” after the hit fell out of memory.

It still **must not** return PAN, DOB, address, income or a full CIF. Those wait for
`CNS-DP` (`AC-CUST-020-2`).

### 3.3 Errors

Public body always includes `code`, `category`, `retryable`, `incidentId`, `correlationId`.
No `origin`, no `diagnostic`, no vendor / CBS / Apigee text (`ADR-017`).

| HTTP | `code` | `retryable` | RM-visible (S05) |
|---|---|---|---|
| 200 empty | — | — | “No customer found for *(term)*. Check the CIF, mobile or PAN.” Do **not** interpolate the raw `q` if `by=PAN`. Use “that PAN” / “that mobile” / “that Customer ID”. |
| 400 | `VALIDATION_ERROR` | false | Field error; Search stays usable |
| 400 | `MISSING_CORRELATION_ID` | false | Client bug; retry with header |
| 401 | `SESSION_INVALID` / `SESSION_EXPIRED` | false | Re-login `SCR-01` |
| 403 | `DEFAULT_DENY` | false | Generic deny; no named resource |
| 403 | `ORIGINATION_RM_ONLY` | false | IPR / non-RM — should not see the screen |
| 404 | `NOT_FOUND` | false | View-details id not in book (absent) |
| 503 | `UPSTREAM_UNAVAILABLE` | true | “Customer records are unavailable right now. **Do not proceed.**” |

`category` lets the app branch (retry vs re-login vs stay) without parsing `title`.

CBS timeout and Apigee 5xx **collapse** to `UPSTREAM_UNAVAILABLE`. Do not leak which hop.

### 3.4 Masking algorithms (BFF, not Flutter)

| Function | Rule | Example |
|---|---|---|
| `initials` | `[A-Za-z]` only; first letter of first token + first letter of last token; one token → first two letters; empty → `?` | `Abhishek Kummar` → `AK` |
| `maskedCif` | Keep last 4 digits; every other digit → `X`; preserve length | `5600980433` → `XXXXXX0433` · display label `CIF {maskedCif}` |
| `maskedMobile` | Indian: `+91` + first 3 of national number + `****` + last 3 | `9331111412` → `+91 933****412` |
| `maskedEmail` | First 3 of local-part + `***@` + domain; local-part ≤ 3 → `***@domain` | `abhishek@gmail.com` → `abh*****@gmail.com` |

PAN is **never** masked-and-returned. It is request-only.

### 3.5 Pagination and volume

CBS search is ranked identity lookup, not a dump (`AC-CUST-010-1` is “the match”).

| | |
|---|---|
| Default / max `limit` | 20 / 20 |
| Max `page` | 4 (100 rows theoretical; R0 UX stops at page 0) |
| Style | Offset. Cursor is the wrong model for a ranked CBS list |
| Never | Return `items` plus a nested full Customer / Lead / CIF snapshot |

Lead-first hits are also capped at 20 customers.

### 3.6 Caching — forbidden for identity

| Layer | Rule |
|---|---|
| Flutter | Do not cache PAN / mobile / CIF queries across screens |
| BFF | Do not cache CBS hits (`S-04` / `S-05` — stale identity is forbidden) |
| Lead existence | No BFF cache. Authorisation-scoped and mutating |
| Catalogue | Not this API |

A lead-first hit is a **fresh Lead read**, not a cache of a previous CBS body.

### 3.7 Parallel calls

| When | Allowed in parallel | Do not parallelise |
|---|---|---|
| `SCR-03` loading | Nothing with search | Search + full CIF; search + pipeline |
| After a row is selected | `GET /customers/{id}` (if needed) **and** `GET /catalogue/product-classes?lob=LIFE` **and** `GET /customers/{id}/active-leads` | Prefill / consent |
| Search in flight | — | A second search with a different `by` for the same key (client debounce) |

PDP stays on the BFF hop. The app does not call the PDP.

---

## 4. Internal seams (cluster-private)

Not on the public gateway. BFF → service identity. Representative paths:

| Hop | Call | Timeout | Notes |
|---|---|---|---|
| #3 PDP | `POST /internal/v1/authorize` `{action: customer.search}` | 300 ms, **no retry**, fail-closed (`S-02`) | |
| #4 Customer | `GET /internal/v1/customers:resolve?by=&q=` | 200 ms | **Local store only.** Book-scoped. Does not call Apigee. 0..n `customerId` |
| #5 Lead | `GET /internal/v1/leads?customerId={id}&assignedRmId=me&active=true` | 300 ms | Visibility predicate in the store. Cross-RM rows absent |
| #4 Customer | `GET /internal/v1/customers:lookup?by=&q=` | **2 s** (`S-04` / `S-05`) | Snapshot; **does not write CBS**. Triggers Apigee |
| Apigee private | Configurable base URL on `#4` | inside the 2 s | Bank customer-inquiry product. Exact operation: `OPEN-SEARCH-CBS-OP` |
| CBS / EBS | Behind Apigee | — | Master for CIF identity |
| #16 Audit | outbox from BFF / #4 / #5 | async | Never on the wait-path |

`#4` may persist a **snapshot** of a CBS hit (`customerId`, CIF, name, mobile, ETB flag,
`snapshotTakenAt`, `sourceSystem=CBS`) so the next search can resolve locally. That write
is `#4`’s, not the BFF’s, and it is not a journey prefill.

Lead is queried **only** with `customerId`, never with raw PAN. `#4` is the anti-corruption
boundary for CBS field names.

### 4.1 Capability we need from CBS (not a guessed path)

Until the Apigee team onboards the product (`SPIKE-001` remaining answers; `ASM-016`),
Java uses a **configurable** outbound base URL. Do not hardcode a CBS origin or a guessed
resource name.

CBS must be able to:

| Input | Output (internal to `#4`) |
|---|---|
| CIF / customer number | CIF, full name, registered mobile, email (optional), ETB / relationship flag, customer status |
| PAN | same, one person |
| Registered mobile | same, 0..n (R0 expects 0..1 in-book) |

`#4` maps that to the internal lookup DTO. The BFF never sees CBS wire names.

`dev` may stub CBS. `uat` and `prod` may not ([HLD](../../architecture/R0-HLD.md) CBS row):
a journey evidenced against a stub is not an evidenced journey.

### 4.2 Orphan / inconsistency

If Lead has a row whose `customerId` `#4` cannot resolve locally (`OPEN-SEARCH-ORPHAN`):
treat as “no lead”, go to CBS, then `#4` + `#5` reconcile. Do not show a card with a
dangling `leadId` and no name.

---

## 5. Data ownership (Aarti + Mahesh)

| Fact | SoR | Search may |
|---|---|---|
| CIF, PAN, registered mobile, legal name, ETB flag | CBS (master); `#4` snapshot | Read via Apigee; snapshot write in `#4` |
| `customerId` (ULID) | `#4` | Mint on first CBS hit if none exists |
| Lead existence, `productClass`, `state` | `#5` | Read own non-terminal rows |
| `journeyId` | `#9` (opened by Lead) | Read via Lead projection |
| Masked projection | `#2` (derived, not stored) | Compute per response |
| Audit of the search | `#16` | Append-only |

PAN stays RESTRICTED: encrypted store in `#4` only
([information model §4.1](./02-information-model.md#41-customer--sor-customer-context-profile-snapshot-cbs-for-the-master)).
Lead tables do **not** gain a PAN or mobile column for this feature.

No new table is required to publish this contract. Physical indexes for
`customer.cif` / `customer.mobile` / `customer.pan_hash` are S11 and Aarti’s.

---

## 6. Security and compliance

### 6.1 Trust boundary

The public body crosses L4 (BFF) → device. That is where `toPublic()` and masking run.
Apigee credentials, CBS payloads and full CIF live **inside** the cluster.

Deepali owns the security outcome (`AP/B` on exposure / authn). This draft specifies
structure; it does not waive Board 4.

### 6.2 Controls

| Control | How this API satisfies it |
|---|---|
| Token-hiding (`ADR-015`) | Opaque session only |
| PDP fail-closed (`S-02`) | No search without `customer.search` |
| In-book ETB (`INV-LED-05`) | `#4` / Lead visibility predicates; out-of-book absent |
| No PII in logs | Hash `q` (SHA-256 with server pepper) if a diagnostic is required; default is “do not log `q`” |
| RESTRICTED PAN | Request-only; not in response; not in audit clear-text |
| Material action (`BR-SEC-030`) | Audit event `CUSTOMER_SEARCHED` |
| Residency | No CBS payload, log or archive outside AWS India |
| Consent | Search is identity, not prefill. `CNS-DP` is not required to *see* the card; it **is** required before CIF data enters a journey (`AC-CUST-020-2`) |

### 6.3 Audit payload (allowed)

```json
{
  "event": "CUSTOMER_SEARCHED",
  "actorId": "rm-principal-ulid",
  "by": "CUSTOMER_ID",
  "qHash": "sha256:…",
  "resultCount": 1,
  "source": "EXISTING_LEAD",
  "eligibilitySeen": ["ETB"],
  "correlationId": "…",
  "occurredAt": "2026-09-23T09:42:00Z"
}
```

Forbidden in audit: raw `q`, PAN, full mobile, full CIF, full name (name is CONFIDENTIAL —
use `customerId` if a hit must be linked).

### 6.4 Threat notes (for Board 4, not a full T4)

| Threat | Mitigation |
|---|---|
| RM enumerates CIFs | Rate-limit per session + per `by` at the BFF; book scope; no total-count |
| Flutter stores last PAN | Contract forbids echoing `q`; client DoD: do not persist `q` |
| Log shipper captures query string | Gateway + BFF access logs: strip `q` (named deny-list) |
| CBS credential theft | Apigee product + private path; no credential on the BFF if `#4` owns the client |
| Cross-RM lead leak | Lead query `assignedRmId=me`; other `leadId` absent |

---

## 7. SRE and operability

| Topic | Rule |
|---|---|
| SLI | `search_success_ratio` = 2xx / (2xx+5xx), excluding 4xx. Lead-first 2xx counts as success even if CBS is down |
| SLO (R0 target, not yet a Board 7 commitment) | p95 end-to-end search **< 2.3 s** when CBS is needed (PDP 300 + resolve 200 + Lead 300 + CBS 2 s budget); p95 lead-first **< 400 ms** inside `NFR-LAT-01` |
| CBS p95 | `< 300 ms` on the private path (`NFR-NET-04`) — inside the 2 s `#4` timeout |
| Retry | **No** retry on PDP. **No** retry on CBS from the BFF (the RM retries). `#4` may retry Apigee once on connect-reset only, still inside 2 s |
| Circuit | If Apigee / CBS error rate trips, fail `503` for CBS-needed searches. Lead-first remains up |
| Saturation | Search is chatty and PII-bearing. Do not add pods to “fix” CBS slowness — name CBS / Apigee as the bottleneck first |
| Dashboards (S11+) | `source` label (`EXISTING_LEAD` vs `CBS`), `by` label (not `q`), upstream class (`PDP` / `LEAD` / `CUSTOMER` / `APIGEE` / `CBS`) on 503s **internally only** |
| Pager | `AC-EXC-10` rate, not individual RM searches |

Business load (R0 assisted): one RM, one customer, one search. Amplification: 1 public GET →
1 PDP + 1 resolve + 0..1 Lead + 0..1 CBS. The next downstream limit is **CBS / Apigee**, not
the BFF replica count.

---

## 8. QA — test matrix (Swapnali)

Evidence, not intent. Each row is an S11 automated test unless marked `manual`.

| # | Case | Expected |
|---|---|---|
| Q1 | `by=CUSTOMER_ID`, one local customer, one own ULIP lead | 200, `source=EXISTING_LEAD`, `existingLead.productClass=ULIP`, **zero** Apigee/CBS calls |
| Q2 | `by=PAN`, local customer, **no** lead, CBS ETB hit | 200, `source=CBS`, `existingLead=null`, no product chip; Apigee called once |
| Q3 | `by=MOBILE`, no local, CBS empty | 200 `items=[]`; no customer row inserted as a journey; no `POST /leads` |
| Q4 | `by=MOBILE`, CBS `NOT_ETB` | 200, `eligibility=NOT_ETB`; Flutter Continue disabled |
| Q5 | No lead, Apigee 504 / CBS timeout | 503 `UPSTREAM_UNAVAILABLE`; body has no CBS text |
| Q6 | Lead exists, CBS is down | 200 lead card (Q1). 503 is **wrong** here |
| Q7 | `by=PAN` & `q=abc` | 400 `VALIDATION_ERROR` field `q`; no `#4` call |
| Q8 | `by=NAME` | 400 field `by` on this screen |
| Q9 | Session missing / expired | 401; no `#4` / `#5` |
| Q10 | IPR session | 403 `ORIGINATION_RM_ONLY` or `DEFAULT_DENY` |
| Q11 | Out-of-book CIF | 200 empty (absent), not 403 naming the CIF |
| Q12 | Another RM’s lead on this CIF | Card may still come from CBS; `existingLead` is null for the caller (`OPEN-LEAD-XRM`) |
| Q13 | Response schema | No `cifNumber`, `pan`, `dateOfBirth`, `address`, `q` |
| Q14 | Access log of a PAN search | `q` absent; correlation id present |
| Q15 | Audit row | `CUSTOMER_SEARCHED` with `qHash`, no raw PAN |
| Q16 | Two own leads (TERM + ULIP) | One card, `existingLead` = latest, `existingLeadCount=2` |
| Q17 | View details | `GET /customers/{id}` matches the hit; 404 if not in book |
| Q18 | Idempotent repeat of the same search | Two GETs, two audits, same masking; no second customer ULID if snapshot exists |
| Q19 | Contract test | Consumer (NIP-APP) generated from this file / updated OpenAPI, not from Figma |
| Q20 | `manual` / a11y | Search button disabled until format-valid; Clear wipes results; spinner in the field (S05) |

Critical-journey regression: Q1, Q2, Q3, Q5, Q6, Q13, Q14.

---

## 9. OpenAPI fragment (machine contract for this operation)

This fragment is the searchable YAML for `GET /customers:search` and the confirm GET.
The lead-phase file remains the bundle S11 generators should read; it must stay aligned.

```yaml
openapi: 3.0.3
info:
  title: NIP BFF — SCR-03 customer search
  version: 0.2.0
  description: >
    Unified contract ARCH-025. Public surface only. Masking happens in the BFF.
paths:
  /customers:search:
    get:
      operationId: searchCustomers
      summary: Search ETB customers — lead-first, else CBS via Apigee
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
        - name: by
          in: query
          required: true
          schema:
            $ref: '#/components/schemas/SearchBy'
        - name: q
          in: query
          required: true
          schema:
            type: string
            minLength: 1
            maxLength: 80
          description: Never echoed. Never logged.
        - name: page
          in: query
          schema: { type: integer, minimum: 0, maximum: 4, default: 0 }
        - name: limit
          in: query
          schema: { type: integer, minimum: 1, maximum: 20, default: 20 }
      responses:
        '200':
          description: Masked hits. Empty list is a valid 200.
          content:
            application/json:
              schema: { $ref: '#/components/schemas/SearchPage' }
        '400': { $ref: '#/components/responses/BadRequest' }
        '401': { $ref: '#/components/responses/Unauthorized' }
        '403': { $ref: '#/components/responses/Forbidden' }
        '503': { $ref: '#/components/responses/Unavailable' }
  /customers/{customerId}:
    get:
      operationId: getCustomerSummary
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
        - $ref: '#/components/parameters/CustomerId'
      responses:
        '200':
          content:
            application/json:
              schema: { $ref: '#/components/schemas/CustomerSearchHit' }
        '401': { $ref: '#/components/responses/Unauthorized' }
        '404': { $ref: '#/components/responses/NotFound' }
        '503': { $ref: '#/components/responses/Unavailable' }
components:
  schemas:
    SearchBy:
      type: string
      enum: [CUSTOMER_ID, PAN, MOBILE]
    SearchSource:
      type: string
      enum: [EXISTING_LEAD, CBS]
    Eligibility:
      type: string
      enum: [ETB, NOT_ETB]
    ProductClassOnLead:
      type: string
      enum: [TERM, SAVINGS, ULIP]
      description: Display/resume only. POST /leads create-new remains R0-gated separately.
    CustomerSearchHit:
      type: object
      additionalProperties: false
      required: [customerId, fullName, initials, eligibility, source, existingLeadCount]
      properties:
        customerId: { $ref: '#/components/schemas/Ulid' }
        fullName: { type: string, maxLength: 140 }
        initials: { type: string, maxLength: 4 }
        maskedCif: { type: string, description: Last-4 CIF; full cifNumber forbidden }
        maskedMobile: { type: string }
        maskedEmail: { type: string, nullable: true }
        eligibility: { $ref: '#/components/schemas/Eligibility' }
        source: { $ref: '#/components/schemas/SearchSource' }
        existingLead:
          nullable: true
          allOf: [{ $ref: '#/components/schemas/ExistingLeadSummary' }]
        existingLeadCount: { type: integer, minimum: 0 }
    ExistingLeadSummary:
      type: object
      additionalProperties: false
      required: [leadId, productClass, lob, state]
      properties:
        leadId: { $ref: '#/components/schemas/Ulid' }
        productClass: { $ref: '#/components/schemas/ProductClassOnLead' }
        lob: { type: string, enum: [LIFE] }
        state: { type: string }
        journeyId: { $ref: '#/components/schemas/Ulid' }
        updatedAt: { type: string, format: date-time }
    SearchPage:
      type: object
      additionalProperties: false
      required: [query, items, page]
      properties:
        query:
          type: object
          additionalProperties: false
          required: [by, resultCount]
          properties:
            by: { $ref: '#/components/schemas/SearchBy' }
            resultCount: { type: integer, minimum: 0 }
            source: { $ref: '#/components/schemas/SearchSource' }
        items:
          type: array
          items: { $ref: '#/components/schemas/CustomerSearchHit' }
        page:
          type: object
          required: [page, size, hasMore]
          properties:
            page: { type: integer }
            size: { type: integer }
            hasMore: { type: boolean }
```

Shared `Ulid`, error responses and session parameters are those already in
`nip-bff-lead-phase.openapi.yaml`. Do not fork `ServiceErrorResponse`.

---

## 10. Java sketches (S11, generate from OpenAPI)

Package (when implemented): `com.bank.insurance.nip.bff.api.v1.search`.
Internal DTOs stay in `…internal.customer` / `…internal.lead`. Flutter never imports them.

```java
public enum SearchBy { CUSTOMER_ID, PAN, MOBILE }
public enum SearchSource { EXISTING_LEAD, CBS }
public enum Eligibility { ETB, NOT_ETB }
public enum ProductClassOnLead { TERM, SAVINGS, ULIP }

public record ExistingLeadSummary(
        @NotNull Ulid leadId,
        @NotNull ProductClassOnLead productClass,
        @NotNull Lob lob,
        @NotNull LeadState state,
        Ulid journeyId,
        Instant updatedAt) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerSearchHit(
        @NotNull Ulid customerId,
        @NotBlank @Size(max = 140) String fullName,
        @NotBlank @Size(max = 4) String initials,
        String maskedCif,
        String maskedMobile,
        String maskedEmail,
        @NotNull Eligibility eligibility,
        @NotNull SearchSource source,
        ExistingLeadSummary existingLead,
        @Min(0) int existingLeadCount) {}

public record SearchQueryMeta(
        @NotNull SearchBy by,
        @Min(0) int resultCount,
        SearchSource source) {}

public record SearchPage(
        @NotNull SearchQueryMeta query,
        @NotNull List<CustomerSearchHit> items,
        @NotNull OffsetPage page) {}
```

BFF-private (never on the Flutter wire):

```java
/** #4 local resolve — no Apigee. */
public record CustomerResolveInternal(String customerId, boolean inBook) {}

/** #4 CBS lookup after Apigee — full identity, pre-mask. */
public record CustomerLookupInternal(
        String customerId,
        String cifNumber,
        String fullName,
        String mobileE164,
        String email,
        boolean etb,
        boolean inBook) {}
```

Controller: `GET /api/v1/customers:search` and `GET /api/v1/customers/{customerId}` as in
[`08-nip-bff-lead-phase-java-records.md`](./08-nip-bff-lead-phase-java-records.md), returning
`SearchPage` / `CustomerSearchHit`.

---

## 11. NIP-APP implementation notes (Amit + Flutter)

1. Dropdown binds to `by`. Three entries only.
2. Enable Search when format-valid. Keep a spinner **in the field** (S05 loading).
3. Render `items[]`. Chip text = `CIF {maskedCif}` + optional ` • {productClass}`.
4. Selection stores `customerId` (+ `existingLead.leadId` if present).
5. View details: same hit, or `GET /customers/{customerId}`.
6. Clear: drop local results; do not persist `q`.
7. 200 empty → S05 empty copy. 503 → **Do not proceed.** No local “create anyway”.
8. `NOT_ETB` → row visible, primary action disabled.
9. Do not generate models from `#4` / `#5` / CBS / 1SB.
10. After `POST /leads` (next screen) invalidate any in-memory search list.

---

## 12. Open questions (not silently decided)

| ID | Question | Owner | Default in this contract |
|---|---|---|---|
| `OPEN-SEARCH-CIF-MASK` | Is last-4 CIF (`maskedCif`) a policy-permitted identifier on the RM device? | Shailja + Rajal | **Included**, matching the screen and `AC-CUST-010-1`. If Board 6 says no, drop the field — no schema break if clients treat it as optional |
| `OPEN-SEARCH-CBS-OP` | Exact Apigee product / verb / path for CIF·PAN·mobile inquiry | Bank API platform + `#4` owner | Configurable base URL; capability table in §4.1; do not guess a CBS path |
| `OPEN-SEARCH-ORPHAN` | Lead row whose `customerId` `#4` cannot resolve | Mahesh + Aarti | Fall through to CBS; do not render a nameless lead |
| `OPEN-LEAD-XRM` | Other RM already has an active Life lead | Rajal + Shailja | Absent. This RM may still see a CBS card with `existingLead=null` |
| `OPEN-LEAD-NAME` | Name as a fourth key | Rajal + BA | **Not on this dropdown** |
| `OPEN-LEAD-DUP` | Second active Term lead | Rajal | Unchanged: `409` on **create**, not on search |
| `OPEN-SEARCH-CIF-ID` | Customer ID ≠ CIF in CBS | Aarti + BA | Search `CUSTOMER_ID` against the bank customer number CBS uses; `maskedCif` from the CIF SoR field |

---

## 13. Traceability

| Behaviour | Authority |
|---|---|
| Three search keys | `AC-CUST-010-1`, R0-SCOPE §3 Customer, BR-CUST-010, S03 resolution of “account — confirm” |
| Empty does not create | `AC-CUST-010-2` |
| Non-ETB cannot start | `AC-CUST-010-3`, D-009 |
| CBS down → do not proceed | `AC-EXC-10`, S05 `SCR-03` error |
| Screen inventory | S05 §4.3 `SCR-03`, §4.5 states |
| Lead is the spoken path | `CR-013`, `ADR-014` |
| One RM BFF | `ADR-015` |
| Apigee outbound / API Gateway inbound | `ADR-020`, 2026-09-14 human direction |
| Error envelope | `ADR-017` |
| CIF not on Flutter as clear text | information model §4.1; this file permits **masked** last-4 only |
| PAN RESTRICTED | information model §2.1 / §4.1 |
| In-book ETB / RM-only origination | `INV-LED-04`, `INV-LED-05`, `ADR-005` |
| No PII in logs | standing constraint, `BR-SEC-040` / `AC-SEC-040-1` |
| Search audited | `BR-SEC-030` |
| Flutter ↛ 1SB / DB / Apigee | BOOT standing constraints |
| Customer BFF out | BOOT WS-3 `out_of_scope_now` |
| Savings/ULIP as a **display** class | `CR-015` / `EPIC-004` — chip only; create-new gates stay on `POST /leads` |
| FF-15 consumer contract | [`03-solution-architecture-r0.md`](./03-solution-architecture-r0.md) FF-15 |
| Parent pack | `SUG-20260907-ldc` · `EPIC-003` · `ARCH-023` · `PLAN-004` |

---

## 14. What “done” means

`ARCH-025` is documentation-complete when:

1. This file, the lead-phase OpenAPI search operation, and the Java sketches agree on
   `by`, fields, `source`, and codes.
2. S11 implementers can build NIP-APP `SCR-03` without reading Figma or inventing a
   CBS client.
3. Product, BA, Security, Compliance, QA, SRE and Delivery can review **this** file
   without a second artefact.
4. Human Board 1 has signed the public contract (agent draft only). Board 4 / 6 on
   `OPEN-SEARCH-CIF-MASK` and Apigee exposure remain human.

Implementation of NIP BFF / Customer / Lead / Apigee onboarding is **S11-E02 / S11-E06**
and `SPIKE-001`, not this item.

---

## 15. Severity (architecture)

`A2` — manageable, dated: the 2026-09-07 pack searched CBS first and forbade every
CIF-shaped field. This file records the correction against a human screen and `ADR-020`.
Not `A0` (no integrity violation in production; nothing is implemented). Not `A1` if
S11 waits for this contract.

Agent verdict (Board 1 simulation, `self_review: true`, **not** a human T4 signature):
**APPROVED_WITH_CONDITIONS** — conditions = `OPEN-SEARCH-CIF-MASK` and `OPEN-SEARCH-CBS-OP`
recorded, not closed.
