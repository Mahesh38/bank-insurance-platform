# 05 — Platform, Journey and Product Governance

## 1. Journey ownership

Rajal owns the business definition of each journey, including:

- actors and permissions assumptions;
- trigger and entry criteria;
- steps and business state transitions;
- business rules;
- alternate/exception flows;
- required data and evidence;
- external dependencies;
- completion state;
- operational/manual fallback;
- product KPIs.

Architecture determines technical orchestration and state implementation.

## 2. Canonical journey pattern

A journey specification should distinguish:

`Customer/RM Intent → Bank Product Behaviour → Canonical Business State → Provider Interaction → Provider Response → Bank Decision/State → Customer/Operations Outcome`

Provider schemas and insurer-specific sequencing must stay at appropriate integration/product-extension boundaries unless the bank intentionally adopts them.

## 3. Journey inventory

At minimum maintain governed journeys for:

- authentication/customer context;
- lead creation/resume;
- consent/disclosure;
- need analysis/suitability;
- product discovery;
- multi/single quote;
- comparison/selection;
- proposal;
- KYC/verification;
- underwriting/medical/inspection;
- payment;
- issuance;
- policy tracking;
- servicing/endorsement;
- renewal;
- cancellation/refund;
- claims;
- operations/reconciliation;
- communications/reporting.

Only current-scope journeys become active deliverables; the inventory may contain future items.

## 4. Business state discipline

Do not collapse materially different states.

Examples:

- `QUOTE_REQUESTED` ≠ `QUOTE_AVAILABLE`;
- `PROPOSAL_SUBMITTED` ≠ `UNDERWRITING_ACCEPTED`;
- `PAYMENT_SUCCESS` ≠ `POLICY_ISSUED`;
- `POLICY_ISSUED` may still require bank-side confirmation/reconciliation/operations evidence before `SOLD` under the governing definition.

## 5. Product catalogue governance

For every approved product/variant maintain where relevant:

- insurer;
- LoB/category/subcategory;
- product/variant identifier;
- effective dates;
- allowed channels;
- allowed customer segments;
- eligibility rules;
- suitability metadata;
- coverage/tenure/premium/rider configuration;
- provider route/integration capability;
- commercial/commission reference;
- status: draft/active/suspended/withdrawn;
- source and last validation.

Rajal owns business approval of what the bank presents. Technical product synchronisation does not equal business activation.

## 6. Suitability governance

Suitability requirements must state:

- purpose;
- source of each input;
- mandatory/conditional rules;
- result semantics;
- recommendation/presentation implications;
- evidence retained;
- customer/RM visibility;
- compliance decision reference;
- version/effective date.

The Product Owner defines business intent. Compliance validates regulated permissibility/obligations. Deterministic decision logic should remain separate from generative-agent narrative where required.

## 7. Business-rules catalogue

Every material rule should have:

```yaml
business_rule:
  id: BR-0001
  name: "..."
  owner: PRODUCT
  journey: "..."
  lob: "..."
  statement: "..."
  inputs: []
  outcome: "..."
  source: "..."
  effective_from: "..."
  exceptions: []
  compliance_ref: "..."
  version: 1
```

Do not hide business rules solely in UI code, provider adapters or test fixtures.

## 8. Provider-specific variation

Classify a variation as:

- **Canonical** — common bank behaviour;
- **LoB-specific** — genuine insurance-category difference;
- **Product-specific** — genuine product rule;
- **Provider-specific** — insurer/aggregator requirement;
- **Channel-specific** — RM/self-service/B2B behaviour;
- **Temporary exception** — time-bound migration/provider limitation.

Temporary/provider-specific behaviour must not quietly become permanent core policy.

## 9. Failure and resumption

For material journeys define what happens when:

- provider unavailable;
- response partial/invalid;
- quote expires;
- customer abandons;
- RM/customer resumes later;
- duplicate request occurs;
- payment succeeds but downstream step fails;
- underwriting requires more information;
- manual operations intervention is required.

Product defines business expectations; Architecture defines reliable implementation.

## 10. Definition of Ready — Product

Before development, material Product work should have:

- approved problem/objective;
- actor/journey/LoB identified;
- primary rules and acceptance defined;
- material exception paths understood;
- dependencies named;
- blocking clarification resolved;
- required cross-domain reviews identified/completed to the needed stage;
- analytics/evidence requirement defined where relevant.

## 11. Definition of Done — Product

A feature is not Product-done until, as applicable:

- acceptance criteria pass;
- business/exception behaviour is demonstrated;
- required analytics/evidence exists;
- operational path is ready;
- applicable compliance/security/architecture conditions are closed;
- known limitations are recorded;
- release scope/docs are updated;
- business outcome can be measured.
