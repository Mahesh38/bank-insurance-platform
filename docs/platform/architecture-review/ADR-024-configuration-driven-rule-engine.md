# ADR-024 — Business and compliance rules are configuration, evaluated by one rule engine

**Status:** Accepted (owner decision 2026-10-21 — `reviewer_type: HUMAN`, `authority: repository_owner`).
Rule **content** (thresholds, outcomes, reason codes, wording) still needs the content owner's
approval per rule set — see §7.
**Date:** 2026-10-21
**Deciders:** Mahesh (owner, architecture). Content owners per domain: Rajal (product behaviour),
Shailja / bank Compliance (permissibility), Exception BRD owner (lead exception thresholds).
**Workstream:** WS-3 platform
**Stage:** S08 design delta
**Origin:** [`SUG-20261021-rul`](../../governance/registers/SUGGESTION-REGISTER.md#sug-20261021-rul--configuration-driven-rule-engine-and-the-nine-open-decisions) — readiness review decision D1 ([review §5](../api-reference/READINESS-REVIEW.md#5-decisions-needed-before-build-everything-owner-list))

## Context

Suitability (48 rules), consent (38 rules), lead exception handling (15 rules, `EH-BR-RULE-001…015`),
product eligibility and Integration Hub routing are all **rules**: inputs → conditions → an outcome,
a reason code and a message. Today they are prose in rule packs and BRDs; the one implemented rule
(`LeadApplicationService.evaluate`) is hard-coded.

Every one of them will change: Compliance may re-band an income multiple, Product may add a caution,
the bank may tighten an exception threshold, IRDAI may change a turnaround time. The owner's
direction: **such a change must be a configuration change, approved and published without a code
release, and must take effect immediately — including precedence and reason codes.**

What is fixed (regulatory or architectural, not configurable): the suitability **gate itself**
(no quote without a usable evaluation — `SUIT-R40`, `INV-QUO-01`), fail-closed behaviour, the
immutability of a published version (`SUIT-R07`), and that every evaluation is recorded with the
exact rule-set version that produced it.

## Decision

We will express every business and compliance rule set as a **versioned, effective-dated JSON
configuration document** (a *rule set*), owned and published by `administration-config-service`,
and evaluate it with **one deterministic, side-effect-free engine library** (`libs/bank-rule-engine`)
that every deciding service embeds.

### 1. What lives where — code vs configuration

| Concern | Lives in | Changed by |
|---|---|---|
| Engine semantics: operators, functions, hit policies, trace format | Code (`libs/bank-rule-engine`) | Release |
| The fact that a gate exists and fails closed | Code (calling service) | Release + ADR |
| Inputs a rule set may use (names, types, required) | Rule set `inputs` | Config |
| Derived values and formulas (e.g. `recommendedCover`) | Rule set `derive` (expression tree) | Config |
| Lookup tables (income bands, age factors) | Rule set `tables` | Config |
| Rules: conditions, outcomes, reason codes, parameters | Rule set `rules` | Config |
| **Precedence**: hit policy, outcome ranking, rule priority | Rule set `decision` | Config |
| Reason-code catalogue: severity, RM text, customer text, message template with `@params` | Rule set `reasonCodes` (or a shared `REASON_CODES` set) | Config |
| Policy values (consent validity days, checkpoints, IRDAI TAT, allowed outcomes for a gate) | Rule set `parameters` | Config |
| Proof the change is right | Rule set `testCases` — publish is refused unless every case passes | Config |

### 2. Rule-set document (format `rule-set/v1`)

```json
{
  "format": "rule-set/v1",
  "ruleSetId": "SUITABILITY.LIFE.TERM",
  "domain": "SUITABILITY",
  "scope": { "lob": "LIFE", "productClass": "TERM" },
  "version": "1.0.0",
  "effectiveFrom": "2026-11-01T00:00:00+05:30",
  "effectiveTo": null,
  "classification": "COMPLIANCE",
  "inputs": [
    { "name": "ageNextBirthday", "type": "INTEGER", "required": true },
    { "name": "annualIncome", "type": "DECIMAL", "required": true },
    { "name": "dependants", "type": "INTEGER", "required": true },
    { "name": "declaredPremium", "type": "DECIMAL", "required": true }
  ],
  "tables": {
    "incomeMultiple": { "type": "RANGE", "key": "annualIncome", "rows": [
      { "from": 0, "to": 300000, "value": 10 }, { "from": 300000, "to": 500000, "value": 12 } ] }
  },
  "derive": [
    { "name": "affordabilityRatio",
      "expr": { "op": "div", "args": [ { "var": "declaredPremium" }, { "var": "annualIncome" } ] } }
  ],
  "decision": {
    "hitPolicy": "PRIORITY",
    "outcomeRank": ["NOT_SUITABLE", "INSUFFICIENT_DATA", "SUITABLE_WITH_CAUTION", "SUITABLE"],
    "noMatchOutcome": "INSUFFICIENT_DATA",
    "missingInputOutcome": "INSUFFICIENT_DATA",
    "collectReasonCodes": "ALL_FIRED"
  },
  "rules": [
    { "ruleId": "SUIT-R12", "priority": 10,
      "when": { "op": "gt", "args": [ { "var": "affordabilityRatio" }, { "param": "unaffordableRatio" } ] },
      "then": { "outcome": "NOT_SUITABLE", "reasonCode": "PREMIUM_UNAFFORDABLE" } }
  ],
  "parameters": { "unaffordableRatio": 0.30 },
  "reasonCodes": {
    "PREMIUM_UNAFFORDABLE": { "severity": "BLOCK",
      "rmText": "Premium is more than @ratioPct% of income.",
      "customerText": "This premium is too high for your stated income." }
  },
  "testCases": [
    { "name": "SUIT-TC-REF-01", "inputs": { "ageNextBirthday": 38, "annualIncome": 1200000 },
      "expect": { "outcome": "SUITABLE", "derived": { "recommendedCover": 25600000 } } }
  ]
}
```

The full JSON-Schema is `libs/bank-rule-engine/src/main/resources/schema/rule-set-v1.schema.json`;
the authoring guide is [`docs/platform/contracts/rule-engine/LLD.md`](../contracts/rule-engine/LLD.md).

### 3. Expressions — a structured tree, never an evaluated string

Conditions and derivations are JSON expression trees (`{"op": …, "args": […]}`), validated by the
JSON-Schema. There is no `eval`, no SpEL, no scripting: a configuration author cannot execute code,
read the environment, loop or call out. The operator set is closed and versioned with the engine:

- **Logic:** `all`, `any`, `not`
- **Compare:** `eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `in`, `notIn`, `between`, `isNull`, `isPresent`
- **Arithmetic** (`BigDecimal`, `HALF_UP`, declared scale): `add`, `sub`, `mul`, `div`, `min`, `max`, `abs`
- **Domain functions:** `roundUpTo(value, unit)`, `lookup(table, key)`, `daysBetween(a, b)`,
  `ageAt(dob, date)`, `coalesce`
- **Operands:** `{"var": name}` (input or derived), `{"param": name}`, `{"const": value}`

A new operator is an engine release; everything else is configuration.

### 4. Precedence and outcomes — configurable hit policies

| `hitPolicy` | Semantics | Used by |
|---|---|---|
| `PRIORITY` | All matching rules fire; the outcome with the **highest rank** in `outcomeRank` wins; ties by rule `priority` | Suitability ("most restrictive wins", `SUIT-R19a`), lead exception (`BLOCK` > `APPROVAL_REQUIRED` > `PASS`) |
| `FIRST` | Rules in ascending `priority`; the first match wins | Hub routing, eligibility bands |
| `COLLECT` | Every match is returned; no single outcome | Consent checkpoint requirements, cautions to disclose |

`outcomeRank`, each rule's `priority`, `noMatchOutcome` and `missingInputOutcome` are configuration,
so precedence can be changed by publishing a new version. Reason codes are always collected in the
configured order (`ALL_FIRED` or `WINNING_ONLY`) and persisted by the caller.

### 5. Lifecycle — versioned, effective-dated, maker-checker, hot

```
DRAFT ──(maker submits)──► PENDING_APPROVAL ──(checker approves; all testCases pass)──► PUBLISHED
   ▲                              │ reject                                               │
   └──────────────────────────────┘                                   effectiveTo set by a newer version
```

1. A published version is **immutable** (`SUIT-R07`); a change is a new version.
2. Approval needs a checker ≠ maker. `classification: COMPLIANCE` rule sets need a checker holding
   the `COMPLIANCE_APPROVER` role; `PRODUCT` needs `PRODUCT_APPROVER`; `OPERATIONAL` needs `CONFIG_APPROVER`.
   This is where Compliance signs: **a rule change Compliance asks for is a configuration version
   Compliance approves**, not a code change.
3. **Simulation:** `POST /internal/v1/rule-sets/{id}/versions/{v}:simulate` runs any inputs (and all
   `testCases`) against a draft. Publishing is refused if any test case fails.
4. **Resolution:** `GET /internal/v1/rule-sets:resolve?domain&lob&productClass&asOf` returns the
   version in force. Services cache it (L1) and are invalidated by `RuleSetPublished` events; the
   cache TTL (default 60 s) bounds staleness if an event is lost. Resolution failure is **fail
   closed**, never a compiled-in default: configuration service unreachable and no cached version →
   `503 UPSTREAM_UNAVAILABLE`; no version in force for the scope → `422 CONFIGURATION_UNRESOLVABLE`
   (the error catalogue's status).
5. **In-flight journeys** keep the version they started with when the rule set says
   `"pinning": "JOURNEY"` (suitability, consent statements); otherwise the version in force at
   evaluation time applies.

### 6. Evaluation result and evidence

Every evaluation returns and the caller persists:

```json
{ "ruleSetId": "SUITABILITY.LIFE.TERM", "version": "1.0.0", "engineVersion": "1.0.0",
  "inputsHash": "sha256:…", "derived": { "affordabilityRatio": 0.0208 },
  "outcome": "SUITABLE", "fired": [ { "ruleId": "SUIT-R09", "outcome": "SUITABLE", "reasonCode": null } ],
  "reasonCodes": [], "messages": [], "evaluatedAt": "…" }
```

The engine is pure: same inputs + same rule-set version → byte-identical result (`SUIT-R05`).

### 7. Rule sets in R0 and who approves their content

| `ruleSetId` | Domain | Seed source | Content approver |
|---|---|---|---|
| `SUITABILITY.LIFE.TERM` / `.SAVINGS` / `.ULIP` | Suitability | Suitability rule pack §2–§4 (`D-022`) | Shailja / Compliance (permissibility), Rajal (behaviour) |
| `SUITABILITY.GATE` | Gate policy: outcomes that permit a quote, caution-acknowledgement requirement, validity days | Rule pack §6 | Shailja |
| `CONSENT.POLICY` | Checkpoints, purposes per checkpoint, validity days, reuse | Consent rule pack §2, §6; owner decision `D-024` | Shailja |
| `LEAD.EXCEPTION` | `EH-BR-RULE-001…015` | Exception Handling BRD §24.3 / §24.5 — thresholds **TBC** | Exception BRD owner |
| `PROPOSAL.TAT` | Insurer decision turnaround, escalation | IRDAI Master Circular 2024 (7 days) — `D-025` | Shailja |
| `HUB.ROUTING` | Provider per (lob, productClass, insurer) | Integration Hub page §3 | Mahesh |

## Alternatives considered

| Option | Why not |
|---|---|
| Keep rules in code | Every compliance change is a release; the owner rejected this explicitly |
| Drools / DMN engine (KIE, Camunda DMN) | Heavy runtime, its own authoring tools and a second rule language to secure and audit; DMN FEEL is an evaluated expression language. Our hit policies borrow DMN's names so a later move is possible |
| SpEL / JEXL / MVEL string expressions in config | A config author could execute code; sandboxing is error-prone; hard to validate before publish |
| Google CEL | Safe and fast, but adds a dependency and a text language reviewers must learn; the JSON tree is UI-editable and schema-validated. Revisit if authoring by hand becomes painful |
| One rule set per product, hard-coded precedence | Precedence was explicitly required to be configurable |

## Consequences

**Positive:** compliance and product changes are configuration with maker-checker and test cases;
precedence and reason codes change without release; every decision is reproducible from
(rule-set version, inputs); one engine replaces five ad-hoc implementations.

**Negative / risks:** a wrong configuration is now a production change — mitigated by mandatory
test cases, simulation, maker-checker, effective dating (schedule ahead, roll back by publishing the
previous content as a new version) and the evaluation trace. The engine's operator set must be
governed like an API.

**Neutral:** `administration-config-service` becomes a tier-1 dependency for gates; it is cached and
fails closed.

## Compliance

- Fitness function: `RuleEngineDeterminismTest` (same inputs twice → identical bytes).
- Every seed rule set's `testCases` include the rule pack's mandatory tests (`SUIT-TC-REF-01`, the
  seven gate cases in suitability §6.2).
- ArchUnit: no deciding service may contain business thresholds in code (review checklist).
