# 12 — How to author a NIP BFF form

**Workstream:** WS-3 · companion to [`10-nip-bff-screen-descriptor.md`](./10-nip-bff-screen-descriptor.md) (wire) and [`11-nip-bff-screen-runtime.md`](./11-nip-bff-screen-runtime.md) (store / bind / persist)  
**Owner:** R11 BA (fields, rules, AC) · Mahesh (command / ownerContext) · Amit (seed + `FormRuntime` at S11)  
**Status:** `AI-DRAFTED` · T3 · human Board 1 outstanding  
**Origin:** `SUG-20260923-sdu` · `ARCH-026` · `ADR-021`  
**Machine contract:** [`nip-bff-screen-descriptor.openapi.yaml`](./nip-bff-screen-descriptor.openapi.yaml)

This file is the **cookbook**. After reading it, a BA or engineer can seed a new `SCREEN_DOCUMENT`
and a frontend can render and submit it **without inventing keys, URLs, or nested field trees**.

Figma is reference only (`R0-SCOPE` A11). Health picker and meeting scheduler stay parked
(`CR-015`, `SUG-20260907-fig`). URI `/api/v1`, unwrapped success, RFC 7807 errors (`ADR-017`).

---

## 1. What you are building

Three hops. The form is configuration, not a hardcoded Flutter screen.

```text
1. GET  /api/v1/screens/{screenId}           → ScreenDocument (layout + rules + blank POST)
2. GET  optionsUrl (if a SELECT needs live lists)
3. POST submission.href                      → ScreenSubmissionResult
```

| Artefact | Who writes it | When it changes |
|---|---|---|
| `SCREEN_DOCUMENT` seed | BA + Amit (git) | New field / copy / rule / option that uses a **shipped** widget |
| `SCREEN_ACTION` seed | Mahesh + Amit | What `actionId` means (`command`, bindings, defaults) |
| Flutter renderer | Amit at S11 (`FUNC-021`) | New **widget** only (store release) |
| Command handler | Owning context (Lead #5 in R0) | New **command** (service deploy) |

A new capture field that uses `TEXT` / `SELECT` / `RADIO` / … is a **version bump**, not an
app-store submit (`ARCH-026` AC-3).

---

## 2. End-to-end: create a form

Do these in order. Skip none.

1. **Outcome** — Rajal: what must be true after Continue (e.g. “lead is assigned to an RM”).  
2. **`screenId`** — `SCREAMING_SNAKE`, unique, additive on the OpenAPI enum. R0: `LEAD_PRODUCT_CLASS`, `LEAD_ASSIGNMENT`.  
3. **Command** — Mahesh: existing closed command or a new handler + seed (file 11 §4.1).  
4. **Fields** — flat siblings. One `name` per fact. Sections are titles only.  
5. **Widgets** — only the closed enum in §5. Unknown widget is a seed defect.  
6. **Options** — inline for a small closed list; `optionsUrl` for live / cascading lists.  
7. **Dependence** — `visibleWhen` / `requiredWhen` / `enabledWhen` / `dependsOn`. Never nest a field inside another field.  
8. **PII / mask** — every field: `pii` boolean + `mask` enum. Fail closed if omitted.  
9. **Validation** — same object the client and L1 will run (§7).  
10. **`submission.body`** — every writable `name` as a blank token; `READONLY` omitted; known ids prefilled.  
11. **`SCREEN_ACTION`** — `actionId` → `command` + `bindings` + optional `defaults`. Flutter never sees this.  
12. **Seed** — new `version`, `effective_from`, git (`INV-CFG-02`, `CF-4`). Never UPDATE an active row.  
13. **Project GET** — BFF resolves config, writes request-scoped prefill into `READONLY.value` **already masked**.

---

## 3. GET — request

```http
GET /api/v1/screens/{screenId}?leadId=&customerId=&productClass=
X-Correlation-Id: <uuid>
Authorization: <BFF session — Flutter never holds OAuth>
```

| Query | When |
|---|---|
| `customerId` | Prefill name / masked ref on a create screen |
| `leadId` | Prefill Lead ID and drive assignment options |
| `productClass` | Rare; only if the screen is already class-scoped |

Do not poll. Visibility is computed **on the device** from the document.

BFF internally:

```text
config:resolve SCREEN_DOCUMENT (lob, screenId, at=now)
  → payload ScreenDocument
  → stamp READONLY.value from SoR (name, mask, leadId) using each field's mask
  → stamp submission.body.version / leadId / customerId
  → stamp submission.body.values with blank tokens for every writable sibling
  → return unwrapped ScreenDocument
```

Missing / withdrawn screen → `404`. No compiled-in fallback (`S-21`).

---

## 4. GET — response (every top-level key)

```json
{
  "screenId": "LEAD_ASSIGNMENT",
  "version": "2026-09-28.1",
  "title": "Lead created",
  "surfaces": [ /* FORM | LIST | CARD | CAROUSEL */ ],
  "actions": [ /* SUBMIT | NAVIGATE | CANCEL */ ],
  "submission": {
    "method": "POST",
    "href": "/api/v1/screens/LEAD_ASSIGNMENT/submissions",
    "body": { /* ScreenSubmission with blank values */ }
  }
}
```

| Key | Who fills it | Client duty |
|---|---|---|
| `screenId` | Seed | Echo unchanged on POST |
| `version` | Seed (opaque string) | Echo unchanged; stale → re-GET |
| `title` | Seed, or BFF interpolates `{firstName}` | Paint |
| `surfaces` | Seed + prefill | Render in order |
| `actions` | Seed | Primary `SUBMIT` id must match `submission.body.actionId` |
| `submission` | BFF from seed + request ids | **Clone `body`, fill `values`, POST `href`** |

`command` and `ownerContext` are **not** on this resource.

---

## 5. How the client resolves a field

On every keystroke / tap, in **document order** (sections are ignored as parents):

```text
for each field in surfaces[].sections[].fields[] (and CARD.fields[]):
  visible  = (no visibleWhen) OR eval(visibleWhen) == true
  enabled  = (no enabledWhen) OR eval(enabledWhen) == true
  required = visible AND (validation.required OR eval(requiredWhen))
  if field.pii:
      never log name or value; never send to analytics / crash
  paint GET value as-is   # already display-form; do not re-apply mask
  if !visible:
      drop values[name]          # treat as NOT_SET for later predicates
  if optionsUrl and a dependsOn name changed:
      GET optionsUrl with {fieldName} substituted from current values
```

Rules that make this deterministic:

| Rule | Effect |
|---|---|
| Unique `name` | One key in `values` |
| Siblings only | No walk into `options` looking for child fields |
| Hidden → `NOT_SET` | Hide cascades (Y hides ⇒ Z hides) |
| No cycles | Seed CI must reject `A` visibleWhen `B` and `B` visibleWhen `A` |
| Hidden not submitted | Server also drops them (anti-tamper) |
| Do not call the BFF to recompute visibility | The document **is** the rules engine |

Blank tokens the GET already listed stay in `submission.body.values` even while hidden, so the
client does not invent keys. At POST time, omit hidden names (or send blanks — L1 drops them).

---

## 6. Widget catalogue — declare, resolve, submit

`options` on a widget are **choices**, not nested fields. Each option is `{ value, label, iconUrl?, selectable? }`.

### 6.1 Resolution table

| `widget` | JSON type in `values` | Empty token | How the client resolves the value | Typical validation |
|---|---|---|---|---|
| `READONLY` | — (not submitted) | omit | Paint `value` from GET. Ignore edits. | none |
| `TEXT` | string | `""` | Free typing. If `format` set, apply §7. | `required`, `format`, length |
| `TEXTAREA` | string | `""` | Multi-line TEXT | `required`, `maxLength` |
| `SELECT` | string | `""` | Exactly one `options[].value` or one `OptionPage.items[].value` | `required`; value must be in the last fetched list |
| `MULTI_SELECT` | string[] | `[]` | Zero or more option values | `required` means length ≥ 1 |
| `RADIO` | string | `""` | Exactly one option. Same as SELECT, different paint | `required` |
| `CHECKBOX` (no `options`) | boolean | `null` | Unset=`null`, off=`false`, on=`true`. `SET` is `true` only | `required` means must be `true` if used as consent |
| `CHECKBOX` (with `options`) | string[] | `[]` | Same as `MULTI_SELECT` | `required` means length ≥ 1 |
| `TOGGLE` | boolean | `null` | Same as boolean CHECKBOX | |
| `DATE` | string `YYYY-MM-DD` | `""` | Calendar / ISO date only | `min` / `max` as dates |
| `TIME` | string `HH:mm` | `""` | 24h clock | `min` / `max` as times |
| `DATETIME` | string ISO-8601 | `""` | Instant | |
| `TEL` | string | `""` | Same as TEXT + `format: MOBILE_IN` if set | `MOBILE_IN` |
| `EMAIL` | string | `""` | Same as TEXT + `format: EMAIL` | `EMAIL` |

Unknown `widget` on the **client**: paint as `TEXT`, set `unsupportedWidget=true`, **do not
submit**, emit a metric. Unknown `widget` on a **seed**: CI fails. The client fallback is
never the server’s behaviour (file 11 §3.1).

### 6.2 Declare examples (siblings)

**Paint-only fact**

```json
{
  "name": "customerDisplayName",
  "label": "Customer Name",
  "widget": "READONLY",
  "pii": true,
  "mask": "NONE",
  "value": "Abhishek Kumar"
}
```

**Single select, live cascade**

```json
{
  "name": "verticalId",
  "label": "Select vertical",
  "widget": "SELECT",
  "pii": false,
  "mask": "NONE",
  "dependsOn": ["branchId"],
  "visibleWhen": { "all": [{ "field": "branchId", "op": "SET" }] },
  "validation": { "required": true },
  "optionsUrl": "/api/v1/workspace/assignment-options?level=VERTICAL&branchId={branchId}"
}
```

**Radio, closed list (Figma picker)**

```json
{
  "name": "productClass",
  "widget": "RADIO",
  "pii": false,
  "mask": "NONE",
  "validation": { "required": true },
  "options": [
    { "value": "TERM", "label": "Term Life Insurance", "iconUrl": "https://assets.bank.example/nip/classes/term.svg", "selectable": true },
    { "value": "SAVINGS", "label": "Savings Plan", "iconUrl": "https://assets.bank.example/nip/classes/savings.svg", "selectable": true },
    { "value": "ULIP", "label": "ULIP Plan", "iconUrl": "https://assets.bank.example/nip/classes/ulip.svg", "selectable": true }
  ]
}
```

`selectable: false` paints the row but rejects it on submit (use for a parked LOB **only** if
Product asks to show it). R0 Health is **absent**, not disabled.

**Boolean toggle**

```json
{
  "name": "marketingOptIn",
  "label": "Customer agrees to marketing",
  "widget": "TOGGLE",
  "pii": true,
  "mask": "NONE",
  "validation": { "required": false }
}
```

`values.marketingOptIn`: `null` → `true` / `false`.

**Multi checkbox**

```json
{
  "name": "needs",
  "label": "What matters",
  "widget": "CHECKBOX",
  "pii": true,
  "mask": "NONE",
  "validation": { "required": true },
  "options": [
    { "value": "PROTECTION", "label": "Protection" },
    { "value": "SAVINGS", "label": "Saving and growing money" }
  ]
}
```

`values.needs`: `[]` then e.g. `["PROTECTION","SAVINGS"]`.

**Number-as-text**

```json
{
  "name": "annualIncomeInr",
  "label": "Annual income (₹)",
  "widget": "TEXT",
  "pii": true,
  "mask": "REDACT",
  "validation": {
    "required": true,
    "format": "NUMBER",
    "min": 0,
    "decimalPlaces": 2,
    "messages": { "format": "Enter an amount" }
  }
}
```

`values.annualIncomeInr` is still a **string** (`"850000.00"`). L1 parses it. Do not send JSON numbers.

**Date with bounds**

```json
{
  "name": "dob",
  "label": "Date of birth",
  "widget": "DATE",
  "pii": true,
  "mask": "REDACT",
  "validation": { "required": true, "max": "2008-09-28" }
}
```

`values.dob`: `"1990-04-12"`.

### 6.3 What goes in `values` vs what does not

| Goes in `values` | Does not |
|---|---|
| Every writable sibling that is **visible** at submit | `READONLY` |
| Option **`value`** codes (`TERM`, `BR-KOCHI-MGRD`) | Option **labels** (“Term Life Insurance”) |
| Strings / string[] / boolean / `null` | JSON numbers, dates-as-objects |
| Bank language (`lob`, `productClass`, `branchId`) | 1SB codes, CIF, PAN, full mobile |

`iconUrl` is display. It is never a submitted value.

### 6.4 PII and masking — declare on every field

Two required keys. Fail closed if either is missing.

| Key | Type | Who owns the meaning |
|---|---|---|
| `pii` | boolean | Shailja — is this personal / restricted data? |
| `mask` | `NONE` \| `LAST4` \| `MOBILE` \| `EMAIL` \| `REDACT` | Deepali — how GET `value` is painted |

`pii: true` does **not** always hide the field from the RM. Name is PII and still shown (`mask: NONE`). Logs, crash reports and analytics **always** drop `pii: true` values (`INV-LOG-01`, `PII-02`).

| `mask` | When to use | GET `value` |
|---|---|---|
| `NONE` | Not PII, **or** PII the RM must read (name, YES/NO health codes) | As in SoR / blank capture token |
| `LAST4` | Customer ID / CIF-shaped identifiers | `XXXXXX0433` — BFF already applied |
| `MOBILE` | Mobile that may be prefilled | `+91 933****412` |
| `EMAIL` | Email that may be prefilled | `abh*****@gmail.com` |
| `REDACT` | PAN, DOB, income, free-text health notes | `""` or `********` — never raw |

Resolution:

```text
BFF GET:
  SoR raw → apply field.mask → Field.value (display-form)
Client:
  paint Field.value
  do not run LAST4/MOBILE/EMAIL/REDACT again
  if pii: never log
POST:
  capture widgets send the typed value (full mobile, not the masked paint)
  READONLY names stay omitted
400 VALIDATION_ERROR:
  errors[].field = name; body must not echo a pii value
```

Seed rules the CI must fail:

- `pii` and `mask` present on every field
- `pii: false` ⇒ `mask: NONE`
- `mask != NONE` ⇒ `pii: true`
- `format: PAN` ⇒ `mask: REDACT` and no GET `value`
- Double-masking is a client bug, not a seed bug

Worked R0 stamps: `customerDisplayName` `pii:true`/`NONE` · `maskedCustomerRef` `pii:true`/`LAST4` · `leadId` and bank codes `pii:false`/`NONE`.

A TEL capture field the RM types:

```json
{
  "name": "mobile",
  "label": "Mobile",
  "widget": "TEL",
  "pii": true,
  "mask": "MOBILE",
  "validation": { "required": true, "format": "MOBILE_IN" }
}
```

GET blank token stays `""`. If BFF later prefills, it stamps `+91 933****412`. POST still sends `9331111412`.

---

## 7. Validation — same object, two layers

Client runs it for UX. L1 (`FormRuntime`) runs it again on POST against the submitted
`version`. L2 (Lead) still decides assignment / book-scope.

### 7.1 Keys

| Key | When it runs | Pass / fail |
|---|---|---|
| `required` | Field is visible | Empty / `null` / `[]` fails |
| `requiredWhen` | Predicate true **and** visible | Same as required |
| `format` | TEXT / TEXTAREA / TEL / EMAIL | See §7.2 |
| `pattern` | `format=REGEX` only | Full-string match; server adds `^…$` if omitted |
| `minLength` / `maxLength` | string | Unicode code points |
| `min` / `max` | NUMBER, INTEGER, DATE, TIME | Inclusive |
| `decimalPlaces` | NUMBER | Max digits after `.` |
| `messages.*` | any fail | Optional override; client has defaults |

Hidden fields are **not** validated (they are `NOT_SET`).

L1 order per visible field: required → format/pattern → length → min/max → decimalPlaces →
(for SELECT/RADIO) value ∈ last option list.

Fail → `400 VALIDATION_ERROR`, `errors[].field` = `name`. Do not put the rejected
value in the problem body when `pii: true`.

### 7.2 `format` resolution

| `format` | Accepts | Rejects | Example pass |
|---|---|---|---|
| `TEXT` | any string | (length still applies) | `hello` |
| `INTEGER` | optional `-`, digits | `12.5`, `1e3` | `42` |
| `NUMBER` | decimal, optional `-` | `12.` with `decimalPlaces=0` | `12.50` |
| `EMAIL` | `local@domain` | spaces | `rm@aubank.in` |
| `MOBILE_IN` | 10-digit IN, or `+91` + 10 | landline | `9331111412` |
| `PAN` | `[A-Z]{5}[0-9]{4}[A-Z]` | never log the value | (format only; do not echo) |
| `PINCODE` | 6 digits | | `682016` |
| `URI` | `https://` only | `http://` | meeting link |
| `DATE` | `YYYY-MM-DD` | `28/09/2026` | `2026-09-28` |
| `TIME` | `HH:mm` 24h | `3pm` | `14:30` |
| `DATETIME` | ISO-8601 | | `2026-09-28T09:00:00+05:30` |
| `REGEX` | `pattern` required | | |

PAN / Aadhaar formats may exist on **search** screens that already allow those keys. They must
not appear as echoed values on Flutter (`ARCH-023` / `ARCH-025`).

---

## 8. Dependence and operations

The **only** legal dependence is a predicate on a **sibling `name`**.

### 8.1 Where predicates attach

| Slot | Meaning |
|---|---|
| `visibleWhen` | Paint and include in submit only if true |
| `enabledWhen` | Paint but ignore input if false |
| `validation.requiredWhen` | Required if true **and** visible |
| `dependsOn` | **Not** a hide rule. Refetch `optionsUrl` when those names change |

### 8.2 Operators

Evaluate against the **current in-memory `values`**, after hidden names were dropped.

| `op` | `value` on the condition | True when |
|---|---|---|
| `SET` | omitted | Non-empty string, `true`, or non-empty array |
| `NOT_SET` | omitted | Missing, `""`, `null`, `false`, `[]` |
| `EQ` | scalar | Strict equality after stringifying scalars |
| `NEQ` | scalar | Not `EQ` |
| `IN` | array | Field’s scalar is one of the array |
| `NOT_IN` | array | Opposite of `IN` |

`all` = AND · `any` = OR · `not` wraps one predicate. Empty `all` / `any` is a seed defect.

### 8.3 Worked evaluations (assignment)

Start: `{ branchId:"", verticalId:"", assignedRmId:"" }`

| Event | `values` | `verticalId` visible? | `assignedRmId` visible? |
|---|---|---|---|
| Open | all blank | no (`branchId` NOT_SET) | no |
| Pick branch `BR-KOCHI-MGRD` | branch set | yes (`SET`) | no (`verticalId` still NOT_SET) |
| Pick vertical `LIFE` | both set | yes | yes |
| Clear branch | all dropped | no | no (cascade) |

### 8.4 X → Y → Z as siblings (never a tree)

```json
{ "name": "tobaccoUse", "widget": "RADIO", "pii": true, "mask": "NONE", "validation": { "required": true }, "options": [
  { "value": "YES", "label": "Yes" }, { "value": "NO", "label": "No" }
]}
{ "name": "cigarettesPerDay", "widget": "SELECT", "pii": true, "mask": "NONE",
  "visibleWhen": { "all": [{ "field": "tobaccoUse", "op": "EQ", "value": "YES" }] },
  "validation": { "required": true } }
{ "name": "quitCounselNote", "widget": "TEXTAREA", "pii": true, "mask": "REDACT",
  "visibleWhen": { "all": [{ "field": "cigarettesPerDay", "op": "EQ", "value": "20_PLUS" }] },
  "validation": { "required": true, "maxLength": 200 } }
```

`submission.body.values`:

```json
{ "tobaccoUse": "", "cigarettesPerDay": "", "quitCounselNote": "" }
```

AND across two fields:

```json
"visibleWhen": {
  "all": [
    { "field": "tobaccoUse", "op": "EQ", "value": "YES" },
    { "field": "cigarettesPerDay", "op": "IN", "value": ["10_20", "20_PLUS"] }
  ]
}
```

---

## 9. Options: inline vs live

| Source | Use | Seed contains |
|---|---|---|
| `options[]` | Small closed list (TERM / ULIP / YES / NO) | The list |
| `optionsUrl` | Live / book-scoped / cascading | A path with `{fieldName}` tokens |

Token substitution uses **current** `values`, not labels:

```
/api/v1/workspace/assignment-options?level=RM&branchId={branchId}&verticalId={verticalId}
→ /api/v1/workspace/assignment-options?level=RM&branchId=BR-KOCHI-MGRD&verticalId=LIFE
```

`OptionPage`:

```json
{ "level": "RM", "items": [ { "value": "RM-4412", "label": "Priya Nair", "selectable": true } ] }
```

Submit the **`value`**. If `selectable=false`, the client must not accept that row.

R0 live list: `GET /workspace/assignment-options?level=BRANCH|VERTICAL|RM`. Other screens may
add other option endpoints; they return the same `OptionPage` shape.

---

## 10. `submission` — the blank POST

GET always includes it (`ARCH-026` AC-9). Frontend algorithm:

```text
body = clone(document.submission.body)
onChange(name, value):
    body.values[name] = value
onSubmit:
    POST document.submission.href
      headers: Idempotency-Key, X-Correlation-Id
      json: body          # hidden names omitted
```

### 10.1 How BFF builds the blank `values`

Walk every field that is not `READONLY`. Include names that start hidden. Set:

| Widget | Token |
|---|---|
| string-like (TEXT, SELECT, RADIO, DATE, …) | `""` |
| `MULTI_SELECT` / checkbox-with-options | `[]` |
| boolean CHECKBOX / TOGGLE | `null` |

Prefill into **body**, not into `values`: `leadId`, `customerId`, `screenId`, `version`,
`actionId` (the primary `SUBMIT`).

### 10.2 Filled POST (assignment)

```json
{
  "screenId": "LEAD_ASSIGNMENT",
  "version": "2026-09-28.1",
  "actionId": "continue",
  "leadId": "01JQX4K7R8M2N3P4Q5S6T7V8W9",
  "values": {
    "branchId": "BR-KOCHI-MGRD",
    "verticalId": "LIFE",
    "assignedRmId": "RM-4412"
  }
}
```

A seed that adds `notes` (TEXT) adds `"notes": ""` to the next GET’s template. Flutter does
not ship a new model.

---

## 11. POST — what happens

```text
resolve SCREEN_DOCUMENT at body.version     → else 409 STALE_FORM_VERSION
resolve SCREEN_ACTION (screenId, actionId)  → else 422 ACTION_NOT_BOUND
L1 FormRuntime                              → else 400 VALIDATION_ERROR
map values via bindings + defaults          → typed command
owning context L2                           → else 403 / 409
persist SoR + screen_submission + audit
return ScreenSubmissionResult
```

**201** first success · **200** idempotent replay (`outcome=REPLAYED`).

```json
{
  "screenId": "LEAD_ASSIGNMENT",
  "version": "2026-09-28.1",
  "actionId": "continue",
  "outcome": "ACCEPTED",
  "leadId": "01JQX4K7R8M2N3P4Q5S6T7V8W9",
  "journeyId": "01JQX4K8S9N3P4Q5S6T7V8W9X0"
}
```

| `outcome` | Meaning |
|---|---|
| `CREATED` | New aggregate (e.g. `CREATE_LEAD`) |
| `ACCEPTED` | Mutation on an existing aggregate (`ASSIGN_LEAD`) |
| `REPLAYED` | Same `Idempotency-Key` as a prior success |

Flutter never sends `command`. Bindings live in config:

```json
{
  "screenId": "LEAD_PRODUCT_CLASS",
  "actionId": "continue",
  "command": "CREATE_LEAD",
  "ownerContext": "LEAD",
  "idempotent": true,
  "extrasPolicy": "REJECT_UNMAPPED",
  "defaults": { "lob": "LIFE" },
  "bindings": [ { "field": "productClass", "to": "productClass" } ]
}
```

`defaults` are server-side (not painted). `extrasPolicy=STORE_UNMAPPED` keeps unknown keys in
`values_json` until Product promotes them (`OPEN-SCR-PROMOTE`).

R0 commands: `CREATE_LEAD`, `RESUME_LEAD`, `ASSIGN_LEAD`, `SELECT_PRODUCT_CLASS`. New command =
service deploy + seed. `SCHEDULE_MEETING` is parked.

`NAVIGATE` / `CANCEL` have no `SCREEN_ACTION` and no capture row.

---

## 12. Surfaces other than FORM

Use the same envelope. Do not invent a second API family.

| `type` | Children | Submit |
|---|---|---|
| `FORM` | `sections[].fields[]` | `values` from fields |
| `LIST` | `items[]` of CARD | Selected item `payload` merged into `values` before POST |
| `CARD` | `fields[]` + `actions[]` | Same as FORM for its fields |
| `CAROUSEL` | `items[]` of CARD | Same as LIST |

R0 product picker is **FORM + RADIO**, not CAROUSEL (Figma). `payload` merge remains defined
for LIST/CAROUSEL screens you add later. `SCR-03` search stays typed `SearchPage` (`ARCH-025`).

---

## 13. Errors the author must plan for

| HTTP | `code` | Author / client action |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Paint `errors[].field`; do not advance |
| 403 | (L2, e.g. out of book) | Schema was fine; show server message |
| 409 | `STALE_FORM_VERSION` | Re-GET the screen; discard in-memory values |
| 422 | `ACTION_NOT_BOUND` | Seed bug; do not retry the same `actionId` |
| 404 | screen unknown | Wrong `screenId` or not yet seeded |

Problem+json (`ADR-017`). No `{success,data,message}` wrapper.

---

## 14. Worked R0 screens (copy these patterns)

### 14.1 Product class — `CREATE_LEAD`

GET query: `customerId`. One RADIO sibling. `lob` is a binding default, not a painted field.
Client posts `submission.body` with `productClass` filled. See file 10 §5.1.

### 14.2 Assignment — `ASSIGN_LEAD`

GET query: `leadId`. Three writable siblings + three READONLY facts. Cascade via `visibleWhen`
+ `dependsOn` + `optionsUrl`. Blank `values` already lists `branchId`, `verticalId`,
`assignedRmId`. See file 10 §5.2.

---

## 15. Adding a field without a store release

1. Confirm the widget is already in the closed enum.  
2. Add the sibling field to the `SCREEN_DOCUMENT` seed (new `version`).  
3. Add `"newName": ""` (or `[]` / `null`) to what BFF will emit on `submission.body.values`.  
4. If Lead must **decide** on it, add a binding + column (`OPEN-SCR-PROMOTE`). Else
   `STORE_UNMAPPED`.  
5. Activate `effective_from`. Old in-flight GETs keep the old `version` until re-GET.

If you need a widget that does not exist (signature pad, map, file): that is a **store
release**, not a seed.

---

## 16. Seed checklist (CI should fail otherwise)

- [ ] `screenId` in the closed enum (or the PR that adds it)  
- [ ] Every `name` unique  
- [ ] No `reveals`, no field nested under an option  
- [ ] Every `widget` in the enum  
- [ ] Every field has `pii` (boolean) and `mask` (`NONE`\|`LAST4`\|`MOBILE`\|`EMAIL`\|`REDACT`)  
- [ ] `pii: false` only with `mask: NONE`; `mask` other than `NONE` only with `pii: true`  
- [ ] `format: PAN` uses `mask: REDACT` and no GET `value`  
- [ ] Every `visibleWhen.field` names a sibling on this screen  
- [ ] No predicate cycles  
- [ ] `optionsUrl` tokens `{name}` exist as fields  
- [ ] `actions` has a `SUBMIT` whose `id` matches `SCREEN_ACTION.actionId`  
- [ ] `SCREEN_ACTION.command` is in the closed catalogue (or this PR adds the handler)  
- [ ] `iconUrl` is https CDN/BFF, never S3 / 1SB  
- [ ] No CIF / PAN / full mobile in `value` or `values`  
- [ ] Meeting fields absent until `SUG-20260907-fig` unparks  
- [ ] Health not selectable in R0  

---

## 17. Refuse while authoring

| Temptation | Do this instead |
|---|---|
| Nest Y inside X’s option | Sibling Y + `visibleWhen` |
| Client POSTs a made-up path | `submission.href` |
| Hardcode `branchId` in Flutter | Clone `submission.body.values` |
| Put `ASSIGN_LEAD` in the GET | `SCREEN_ACTION` only |
| `http://` icon or raw S3 | Bank CDN https |
| 1SB field names | Bank language |
| `{success,data}` wrapper | Unwrapped resource |
| Client-only validation | L1 + L2 |
| UPDATE the live seed | New version |
| A form microservice | Configuration #19 + owning context |
| Omit `pii` / `mask` | Seed CI fails; do not default to false |
| Client re-masks GET `value` | Paint it; BFF already applied `mask` |
| Log a `pii: true` value | Correlation id only (`INV-LOG-01`) |

---

## 18. Traceability

| Topic | Authority |
|---|---|
| Wire + samples | file 10, OpenAPI |
| Store, L1/L2, commands | file 11, `ADR-007`, `CF-2` |
| Sibling fields | `ADR-021` (2026-09-28 amendment) |
| Field `pii` / `mask` | `CTRL-02`, `INV-LOG-01`, `PII-02`, `ARCH-025`, `SUG-20260928-pii` |
| No store resubmit for a shipped widget | `ARCH-026` AC-3 / AC-9 / AC-11 |
| Figma is reference | `R0-SCOPE` A11 |
| Health / meetings parked | `CR-015`, `SUG-20260907-fig` |
