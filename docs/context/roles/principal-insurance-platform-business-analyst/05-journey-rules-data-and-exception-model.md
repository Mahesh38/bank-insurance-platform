# 05 — Journey, Rules, Data and Exception Model

## 1. One coherent behaviour model

The Principal BA keeps process, state, rules, information, exceptions and evidence aligned. A
change to one view must be assessed against all the others.

```text
Actor + Trigger
  → Process step
  → Rule / decision
  → Information read or changed
  → State transition
  → Customer / RM / provider / operations outcome
  → Evidence and KPI event
```

## 2. Journey model

For each material journey, record:

- business objective and included/excluded scope;
- actors, channels and journey mode (assisted, self-service or hybrid);
- entry conditions and upstream/downstream handoffs;
- numbered happy path with accountable actor per step;
- decision points and linked rule IDs;
- information captured, derived, disclosed and shared;
- state transitions and visible status wording;
- failure, abandonment, expiry and resume paths;
- manual work, queue ownership, SLA/TAT and escalation;
- customer/RM/operations communications;
- exit criteria, evidence and KPI events.

Swimlanes should distinguish Customer, RM/Bank, platform/1SB, insurer/aggregator, payment and
operations/control functions. A provider response is a handoff, not automatically a business
outcome.

## 3. State model

Each state has:

- one business definition;
- entry event and required facts;
- allowed previous/next states;
- actor/source allowed to cause the transition;
- visible customer/RM/operations meaning;
- timeout/expiry behaviour;
- correction/reversal rules;
- audit timestamp and business-effective timestamp;
- KPI and reconciliation consequence.

Do not merge `quote received`, `proposal submitted`, `payment confirmed`, `policy issued`,
`policy recognised` and `policy sold`. Provider, platform and bank-recognised states may differ
temporarily; the requirement must explain convergence and exception ownership.

## 4. Information model

For every material business attribute, define with Aarti and other authorities as appropriate:

| Property | Meaning |
|---|---|
| Business term | Canonical bank vocabulary and definition |
| Business owner | Authority for meaning/use |
| Source | Customer, bank, provider, derived rule or approved reference |
| Cardinality | One/many and relationship meaning |
| Requiredness | When and why it is mandatory/optional/conditional |
| Validation | Format, range, cross-field and rule constraints |
| Sensitivity/purpose | Classification and permitted purpose, confirmed by authorities |
| Mutability/history | Who may change it and which point-in-time truth is retained |
| Mapping | Provider/channel representations and loss/rounding/default behaviour |
| Evidence | Audit, reconciliation, reporting and acceptance use |

The BA owns business semantics and mapping intent. Aarti owns physical persistence and integrity;
Deepali owns Security conclusions; Shailja owns regulatory permissibility.

## 5. Provider and LoB variation

Use a variation matrix whose axes may include channel, journey mode, LoB, insurer, product,
customer segment and effective period. For every difference classify it as:

- `BUSINESS_VARIANT` — approved behaviour genuinely differs;
- `REGULATORY_OR_PRODUCT_TERM` — specialist/insurer rule with confirmed source;
- `PROVIDER_MAPPING` — external representation of a stable bank concept;
- `TECHNICAL_TRANSPORT` — contract detail for Mahesh/Amit, not a business requirement;
- `UNKNOWN` — evidence/owner required.

Do not leak insurer-specific payload language into the canonical journey unless it represents a
real, approved difference.

## 6. Exception taxonomy

At minimum consider:

- invalid, incomplete, contradictory or stale input;
- customer/RM abandonment, expiry and save/resume;
- ineligible, unsuitable, declined, referred or counter-offered outcome;
- insurer unavailable, slow, partial or semantically inconsistent response;
- duplicate submission/callback and out-of-order event;
- payment failure, pending/unknown confirmation, duplicate debit and reconciliation mismatch;
- issuance pending/declined/late or policy-document mismatch;
- consent withdrawal, role/permission loss or certification expiry;
- data mapping loss, correction, late-arriving truth or source conflict;
- queue backlog, manual retry, maker-checker and escalation failure;
- communication delivery failure and customer-visible status mismatch;
- recovery, reversal, compensation or permanent closure.

For each material exception define detection, business state, customer/RM message, automated
behaviour, manual owner, allowed action, retry/idempotency rule, SLA/TAT, escalation, evidence and
terminal state.

## 7. Decision-log discipline

An unresolved point is not hidden inside a story. Record:

```text
Decision required
Why it matters / affected journey
Confirmed facts and source
Options and recommendation
Business, customer, control, data and operations impact
Authority owner
Required-by trigger/date
Resulting artefacts to update
```

## 8. End-to-end review questions

- Can every actor tell what they must do next?
- Can the customer and RM see a truthful, comprehensible status?
- Is every decision explainable from a confirmed rule/version?
- Can operations find and recover incomplete outcomes without inventing policy?
- Can finance reconcile money with issuance and the approved sold definition?
- Can QA derive deterministic scenarios, including late and duplicate events?
- Can Security, Compliance, Database and SRE identify the behaviour relevant to their jurisdiction?
- Can Product measure the intended outcome without redefining the metric after release?

