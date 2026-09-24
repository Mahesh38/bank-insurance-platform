# 10 — NIP BFF screen descriptor (form, list, card, carousel)

**Workstream:** WS-3 · **Horizon:** H0 / R0 assisted Life  
**Owner:** Mahesh (Board 1) — public contract · Amit (Board 2) — renderer at S11 (`FUNC-021`)  
**Status:** `AI-DRAFTED` · T3 · human Board 1 / 4 signatures outstanding  
**Origin:** `SUG-20260923-sdu` · `ARCH-026` · `PLAN-006` · `ADR-021`  
**Machine contract:** [`nip-bff-screen-descriptor.openapi.yaml`](./nip-bff-screen-descriptor.openapi.yaml)  
**Runtime (store, L1/L2 validate, action bind, persist):** [`11-nip-bff-screen-runtime.md`](./11-nip-bff-screen-runtime.md)

This file is the **single** frontend contract for server-driven **surfaces**. NIP-APP (web, iOS,
Android) renders from it so a new field, option, icon or validation rule that uses an
**already-shipped widget** does not require a store resubmit.

It is **not** a Flutter implementation, **not** a 1SB proposal schema on the device, and **not**
a licence to interpret login, pipeline or payment-status as a form (`ARCH-026` out of scope).

Inherits the three platform rules in
[`07-nip-bff-lead-phase-api-lld.md`](./07-nip-bff-lead-phase-api-lld.md#21-platform-conventions-this-pack-inherits)
§2.1: URI `/api/v1`, **unwrapped** success body, RFC 7807 errors (`ADR-017`). Icon URLs are
https on the bank CDN/BFF — never raw S3, never a 1SB host.

---

## 1. What the frontend always receives

One resource, one shape:

```text
ScreenDocument
  screenId, version, title, actions[]
  submission          ← blank POST the client fills (same shape as ScreenSubmission)
  surfaces[]          ← FORM | LIST | CARD | CAROUSEL
    each surface has items[] and/or sections[].fields[]
      Field           ← widget + validation + visibleWhen + reveals
```

| Surface | Use | Children |
|---|---|---|
| `FORM` | Capture / assignment / extra questions | `sections[].fields[]` |
| `LIST` | Vertical collection (search hits, inbox rows) | `items[]` (each item is a `CARD`) |
| `CARD` | One summary tile | `fields[]` (usually `READONLY`) + `actions[]` |
| `CAROUSEL` | Horizontal tiles | `items[]` (each item is a `CARD`) |

A screen may mix surfaces. After customer Continue, `SCR-05` is typically:

1. `FORM` + `RADIO` — “What is {firstName} looking for?” (Figma; Life classes only in R0)  
2. `FORM` — assignment (fields Product admits; meeting stays `SUG-20260907-fig`)

`GET /screens/{screenId}` returns the document. Lookups (`optionsUrl`) stay separate GETs so
dropdowns can cascade without re-downloading the screen.

---

## 2. Field (the only interactive atom)

Every input, label, radio, checkbox and read-only fact is a `Field`. Names are **unique on the
screen** (including nested `reveals`). Values on submit are a **flat map** `name → value`.

```json
{
  "name": "tobaccoUse",
  "label": "Does the customer use tobacco?",
  "widget": "RADIO",
  "validation": { "required": true },
  "options": [
    {
      "value": "YES",
      "label": "Yes",
      "reveals": [
        {
          "name": "cigarettesPerDay",
          "label": "Cigarettes per day",
          "widget": "SELECT",
          "validation": { "required": true },
          "options": [
            { "value": "1_10", "label": "1–10" },
            {
              "value": "20_PLUS",
              "label": "20+",
              "reveals": [
                {
                  "name": "quitCounselNote",
                  "label": "Counselling note",
                  "widget": "TEXTAREA",
                  "validation": { "required": true, "maxLength": 200 }
                }
              ]
            }
          ]
        }
      ]
    },
    { "value": "NO", "label": "No" }
  ]
}
```

That is **X → Y → Z**:

- X = `tobaccoUse` radio  
- Y = `cigarettesPerDay` dropdown, only if X is `YES`  
- Z = `quitCounselNote` textarea, only if Y is `20_PLUS`

Nesting is **`options[].reveals`** (choice widgets) or **`field.reveals`** (CHECKBOX / TOGGLE
when true). Max depth **3**. A fourth level is a contract defect — split the screen.

### 2.1 Closed widget vocabulary

| `widget` | Value type | Notes |
|---|---|---|
| `READONLY` | string | Display only; may send `value` from the server |
| `TEXT` | string | Use `validation.format` for number / email / mobile / regex |
| `TEXTAREA` | string | |
| `SELECT` | string | Single. Options inline or `optionsUrl` |
| `MULTI_SELECT` | string[] | |
| `RADIO` | string | |
| `CHECKBOX` | boolean **or** string[] | boolean if no `options`; string[] if options (multi) |
| `TOGGLE` | boolean | |
| `DATE` | `YYYY-MM-DD` | |
| `TIME` | `HH:mm` | 24h |
| `DATETIME` | ISO-8601 | |
| `TEL` | string | Same rules as `format: MOBILE_IN` if set |
| `EMAIL` | string | Same rules as `format: EMAIL` |

Unknown widget: render as `TEXT`, mark `unsupportedWidget=true`, **do not submit** that field,
emit a client metric. A **new widget** is a store release. A **new field** using a shipped
widget is a `version` bump only.

### 2.2 Cross-field predicates (`visibleWhen` / `requiredWhen` / `enabledWhen`)

Use when the child is **not** owned by one option (e.g. show Z only if X=`A` **and** Y=`B`).

```json
{
  "name": "meetingLink",
  "widget": "TEXT",
  "visibleWhen": {
    "all": [
      { "field": "meetingMode", "op": "EQ", "value": "ONLINE" }
    ]
  },
  "validation": {
    "requiredWhen": {
      "all": [{ "field": "meetingMode", "op": "EQ", "value": "ONLINE" }]
    },
    "format": "URI"
  }
}
```

| `op` | Meaning |
|---|---|
| `SET` | Field has a non-empty value |
| `NOT_SET` | Empty / absent / unchecked |
| `EQ` / `NEQ` | Scalar equality (string/number/boolean) |
| `IN` / `NOT_IN` | `value` is an array |

`all` = AND · `any` = OR · `not` wraps one predicate.

Rules:

1. Predicates reference **names on this screen only**.  
2. A field that is **not visible** is treated as `NOT_SET` for later predicates (hide cascades).  
3. Cycles (`A` visibleWhen `B`, `B` visibleWhen `A`) are invalid — BFF must not publish them.  
4. Hidden field values are **dropped on submit**. Server ignores them if the client sends them
   (anti-tamper).

`dependsOn: ["branchId"]` is a hint to refetch `optionsUrl` when those names change. It does
not hide the field; `visibleWhen` does.

---

## 3. Validation (client + server, same object)

```json
{
  "required": false,
  "requiredWhen": { "all": [{ "field": "x", "op": "EQ", "value": "Y" }] },
  "format": "MOBILE_IN",
  "pattern": null,
  "minLength": 10,
  "maxLength": 13,
  "min": null,
  "max": null,
  "decimalPlaces": null,
  "messages": {
    "required": "Enter a mobile number",
    "format": "Use a 10-digit Indian mobile"
  }
}
```

| Key | Applies to | Rule |
|---|---|---|
| `required` | all | Empty fails if the field is **visible** |
| `requiredWhen` | all | Required only if the predicate holds **and** the field is visible |
| `format` | TEXT / TEXTAREA / TEL / EMAIL | See table below |
| `pattern` | TEXT | Java / ECMA `Pattern` — **only** when `format=REGEX`. Anchored by the server (`^…$` if omitted) |
| `minLength` / `maxLength` | string | Unicode code points |
| `min` / `max` | NUMBER, INTEGER, DATE, TIME | Inclusive. DATE as `YYYY-MM-DD` |
| `decimalPlaces` | NUMBER | Max digits after `.` |
| `messages.*` | all | Optional; client has defaults |

### 3.1 `format` enum

| `format` | Accepts | Example |
|---|---|---|
| `TEXT` | any string (still length-checked) | |
| `INTEGER` | optional `-`, digits only | `42` |
| `NUMBER` | decimal, optional `-` | `12.50` |
| `EMAIL` | `local@domain` | `rm@aubank.in` |
| `MOBILE_IN` | 10-digit Indian, or `+91` + 10 | `9331111412` |
| `PAN` | `[A-Z]{5}[0-9]{4}[A-Z]` | never logged |
| `PINCODE` | 6 digits | `682016` |
| `URI` | `https://` only | meeting link |
| `DATE` / `TIME` / `DATETIME` | as widget | |
| `REGEX` | `pattern` required | |

PAN / Aadhaar **formats** may exist for **search** screens that already allow those keys. They
must not appear as **echoed values** on Flutter (`ARCH-023` / `ARCH-025` forbid-list). A
readonly “Customer ID” on success is a **masked** string the BFF computed, not CIF.

Client validates for UX. **Server re-validates** on submit against the same `version` (**L1**
in `FormRuntime` — file 11 §3). Mismatch → `400 VALIDATION_ERROR` with `errors[].field` = field
`name`. Domain rules (**L2** — book-scope, SP cert, `INV-LED-*`) stay on the owning service and
may still return `403` after L1 passes. The BFF does not become the decision maker.

---

## 4. Client evaluation (deterministic)

On every value change:

1. Build `values` from visible widgets only.  
2. Walk fields in document order, depth-first through `reveals`.  
3. A field is visible if **all** of: parent (if any) is visible; owning option is selected or
   owning checkbox/toggle is true; `visibleWhen` (if present) is true.  
4. A field is enabled unless `enabledWhen` is present and false.  
5. Required = (`required` or `requiredWhen`) **and** visible.  
6. Refetch any `optionsUrl` whose `dependsOn` names changed.  
7. Drop values for fields that just became hidden.

Do **not** call the BFF to recompute visibility. The document is the rules engine.

---

## 5. Public APIs

| Method | Path | Body |
|---|---|---|
| `GET` | `/screens/{screenId}` | — query: `leadId`, `customerId`, `productClass` as needed |
| `GET` | `/workspace/assignment-options` | `level=BRANCH\|VERTICAL\|RM` + parent ids |
| `POST` | `/screens/{screenId}/submissions` | `ScreenSubmission` + `Idempotency-Key` |

`screenId` values in R0 (closed list, additive later):

| `screenId` | Surfaces | When |
|---|---|---|
| `LEAD_PRODUCT_CLASS` | `FORM` (`RADIO`) | After customer Continue (`SCR-05`). Figma: “What is {firstName} looking for?” |
| `LEAD_ASSIGNMENT` | `FORM` | After lead create (fields Product admits; meeting stays `SUG-20260907-fig`) |

Figma is **reference only** (`R0-SCOPE` A11). The Health Insurance row on the picker frame is
**not** in this document (`CR-015` no Health picker; `SUG-20260907-fig`; BOOT out of scope).
Back / Get Helpful are app chrome, not this resource.

### 5.1 Product class — Figma-aligned (`RADIO`)

`GET /screens/LEAD_PRODUCT_CLASS?customerId=01J…`

Title uses the customer’s **first name** (prefill). Section title and option **labels** match
the frame. Option **values** stay bank codes. Order on the frame: Term → Savings → ULIP.
No painted CTA — the client posts `actionId=continue` when the radio changes.

```json
{
  "screenId": "LEAD_PRODUCT_CLASS",
  "version": "2026-09-24.1",
  "title": "What is Abhishek looking for?",
  "surfaces": [
    {
      "id": "life",
      "type": "FORM",
      "title": "Life Insurance",
      "sections": [
        {
          "id": "life-classes",
          "fields": [
            {
              "name": "productClass",
              "widget": "RADIO",
              "validation": { "required": true },
              "options": [
                {
                  "value": "TERM",
                  "label": "Term Life Insurance",
                  "iconUrl": "https://assets.bank.example/nip/classes/term.svg",
                  "selectable": true
                },
                {
                  "value": "SAVINGS",
                  "label": "Savings Plan",
                  "iconUrl": "https://assets.bank.example/nip/classes/savings.svg",
                  "selectable": true
                },
                {
                  "value": "ULIP",
                  "label": "ULIP Plan",
                  "iconUrl": "https://assets.bank.example/nip/classes/ulip.svg",
                  "selectable": true
                }
              ]
            }
          ]
        }
      ]
    }
  ],
  "actions": [
    { "id": "continue", "label": "Continue", "type": "SUBMIT", "surfaceId": "life" }
  ],
  "submission": {
    "method": "POST",
    "href": "/api/v1/screens/LEAD_PRODUCT_CLASS/submissions",
    "body": {
      "screenId": "LEAD_PRODUCT_CLASS",
      "version": "2026-09-24.1",
      "actionId": "continue",
      "customerId": "01JQX4K7R8M2N3P4Q5S6T7V8X1",
      "values": {
        "productClass": ""
      }
    }
  }
}
```

`lob=LIFE` is a `SCREEN_ACTION` default, not a painted field (the frame has no LOB label).
Adding a selectable Health option is a scope change, not a `version` bump.

### 5.2 Nested assignment form example

`GET /screens/LEAD_ASSIGNMENT?leadId=01J…`

```json
{
  "screenId": "LEAD_ASSIGNMENT",
  "version": "2026-09-24.1",
  "title": "Lead created",
  "surfaces": [
    {
      "id": "facts",
      "type": "FORM",
      "sections": [
        {
          "id": "identity",
          "fields": [
            {
              "name": "customerDisplayName",
              "label": "Customer Name",
              "widget": "READONLY",
              "value": "Abhishek Kumar"
            },
            {
              "name": "maskedCustomerRef",
              "label": "Customer ID",
              "widget": "READONLY",
              "value": "XXXXXX0433"
            },
            {
              "name": "leadId",
              "label": "Lead ID",
              "widget": "READONLY",
              "value": "01JQX4K7R8M2N3P4Q5S6T7V8W9"
            }
          ]
        },
        {
          "id": "assignment",
          "title": "Assign lead to SP",
          "fields": [
            {
              "name": "branchId",
              "label": "Select branch",
              "widget": "SELECT",
              "validation": { "required": true },
              "optionsUrl": "/api/v1/workspace/assignment-options?level=BRANCH"
            },
            {
              "name": "verticalId",
              "label": "Select vertical",
              "widget": "SELECT",
              "dependsOn": ["branchId"],
              "visibleWhen": { "all": [{ "field": "branchId", "op": "SET" }] },
              "validation": { "required": true },
              "optionsUrl": "/api/v1/workspace/assignment-options?level=VERTICAL&branchId={branchId}"
            },
            {
              "name": "assignedRmId",
              "label": "Select RM",
              "widget": "SELECT",
              "dependsOn": ["branchId", "verticalId"],
              "visibleWhen": { "all": [{ "field": "verticalId", "op": "SET" }] },
              "validation": { "required": true },
              "optionsUrl": "/api/v1/workspace/assignment-options?level=RM&branchId={branchId}&verticalId={verticalId}"
            }
          ]
        }
      ]
    }
  ],
  "actions": [
    { "id": "continue", "label": "Continue", "type": "SUBMIT", "surfaceId": "facts" }
  ],
  "submission": {
    "method": "POST",
    "href": "/api/v1/screens/LEAD_ASSIGNMENT/submissions",
    "body": {
      "screenId": "LEAD_ASSIGNMENT",
      "version": "2026-09-24.1",
      "actionId": "continue",
      "leadId": "01JQX4K7R8M2N3P4Q5S6T7V8W9",
      "values": {
        "branchId": "",
        "verticalId": "",
        "assignedRmId": ""
      }
    }
  }
}
```

The client **does not invent** the POST shape. It clones `submission.body`, writes each
writable field into `values[name]`, and POSTs that object to `submission.href` with
`Idempotency-Key` + `X-Correlation-Id`. `READONLY` names are omitted from `values`.
Keys for fields that start hidden (`verticalId`, `assignedRmId`) are still listed so a
new field is a `version` bump, not a client change.

Blank tokens: string / radio / select → `""` · `MULTI_SELECT` → `[]` · boolean
checkbox/toggle → `null`. Nested `reveals` names are included as extra keys when the
seed contains them.

Meeting date / time / link are **not** on this document (`SUG-20260907-fig`).

### 5.3 Submit

`POST /screens/LEAD_PRODUCT_CLASS/submissions` — radio `productClass` only; `lob` comes from
the binding default.

```json
{
  "screenId": "LEAD_PRODUCT_CLASS",
  "version": "2026-09-24.1",
  "actionId": "continue",
  "customerId": "01JQX4K7R8M2N3P4Q5S6T7V8X1",
  "values": {
    "productClass": "TERM"
  }
}
```

`POST /screens/LEAD_ASSIGNMENT/submissions`

```json
{
  "screenId": "LEAD_ASSIGNMENT",
  "version": "2026-09-24.1",
  "actionId": "continue",
  "leadId": "01JQX4K7R8M2N3P4Q5S6T7V8W9",
  "values": {
    "branchId": "BR-KOCHI-MGRD",
    "verticalId": "LIFE",
    "assignedRmId": "RM-4412"
  }
}
```

`actionId` selects a `SCREEN_ACTION` binding (file 11 §4). Flutter never sends `command` or
`ownerContext`. Missing binding → `422 ACTION_NOT_BOUND`.

`READONLY` names may be omitted. Hidden names must be omitted. Stale `version` →
`409 IDEMPOTENCY_CONFLICT` is wrong; use `409 CONFLICT` / `errors[].code=STALE_FORM_VERSION`
and re-GET the screen.

---

## 6. LIST / CARD (same envelope)

A search result page is not a special API family. It is `type: LIST` of `CARD` items:

```json
{
  "screenId": "CUSTOMER_SEARCH_RESULTS",
  "version": "2026-09-23.1",
  "surfaces": [
    {
      "id": "hits",
      "type": "LIST",
      "items": [
        {
          "id": "01JQX4K7R8M2N3P4Q5S6T7V8X1",
          "title": "Abhishek Kumar",
          "subtitle": "+91 933****412",
          "selectable": true,
          "payload": { "customerId": "01JQX4K7R8M2N3P4Q5S6T7V8X1" }
        }
      ]
    }
  ]
}
```

`SCR-03` **already** has a typed `SearchPage` (`ARCH-025`). Do **not** rewrite it in this pack.
New list/card screens after this ADR use `ScreenDocument`. Existing typed resources stay.

---

## 7. What this standard refuses

| Ask | Answer |
|---|---|
| Raw S3 URL on `iconUrl` | No — bank CDN/BFF https |
| 1SB field names / `{data,errors,reqId}` | No — bank language (`SUG-20260913-acl`) |
| `{success,data,message}` wrapper | No — §2.1 of the lead LLD |
| Unbounded widget plugins from the server | No — closed enum; unknown → TEXT fallback |
| Nesting deeper than 3 | No — split the screen |
| Login / pipeline / payment-status as FORM | No — `ARCH-026` out of scope |
| Meeting date/time/link in R0 | No — `SUG-20260907-fig` |
| Client-only validation | No — L1 schema + L2 invariant (file 11 §3) |
| A form microservice that owns Lead state | No — file 11 §1 / §7 |
| CIF/PAN/full mobile on the wire as values | No — mask at BFF |

---

## 8. Traceability

| Behaviour | Authority |
|---|---|
| No store resubmit for field/catalogue add | `SUG-20260923-sdu`, `ARCH-026` AC-3 |
| Life × Term / ULIP / Savings tiles | `CR-015`, `FUNC-020` |
| Unwrapped body + problem+json | `ADR-017`, `SUG-20260907-std` |
| Schema-driven capture (proposal already) | S05 `SCR-13`, field-guide `proposal-and-dynamic-forms.md` |
| Token-hiding BFF | `ADR-015` |
| Store, L1/L2, action bind, capture | file 11, `ADR-007`, `CF-2`, `INV-CFG-02/03` |

## 9. Done for this document

`ARCH-026` documentation is complete when this file, file 11, the OpenAPI, and `ADR-021` agree
on surfaces, widgets, predicates, validation, **where the definition is stored**, **L1 vs L2**,
**which `actionId` runs which command**, and **what is persisted**. Human Board 1 is outstanding.
Flutter renderer is `FUNC-021`.
