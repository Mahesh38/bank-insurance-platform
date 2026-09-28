# 04 — Flow, lanes and parallel work

## 1. Lanes are capabilities, not stages

A **lane** is a stream-aligned slice of the platform with a human lane lead, any number of AI
executors and its own maturity. Lanes are listed in [`state/lanes.yaml`](../state/lanes.yaml).

| Lane | Covers (product repo) | Replaces |
|---|---|---|
| `integration-hub` | `1sb-integration-service`, Integration Hub contracts, 1SB adapter, Life LOB handlers | WS-1 |
| `persistence-audit` | `bank-persistence-service`, audit store, job store, data migrations | parts of WS-1/WS-3 |
| `workforce-identity` | `workforce-access-bff`, identity adapter, authorization PDP | WS-2 |
| `distribution-journey` | Lead, customer, consent, suitability, quote, proposal, payment, policy contexts | WS-3 product contexts |
| `experience-bff` | NIP BFF contracts evaluated against Figma; client lives in another repo | WS-3 context #2 |
| `platform` | CI/CD, IaC, environments, secrets, observability, connectivity (Apigee, VPN/DX) | WS-3 S08/S09 |
| `governance` | This repository | GOV work type |

Lanes may be added or merged by the delivery lead after a retro — it is a Type-2 decision.

## 2. Classes of service

| Class | Use for | Scheduling rule |
|---|---|---|
| **Expedite** | Production/UAT incident, guardrail breach, exploitable vulnerability, data corruption, a broken main build | Pulled immediately, may exceed WIP; max 1 per lane at a time |
| **Fixed-date** | External commitment: bank UAT slot, regulatory date, insurer onboarding window | Scheduled back from the date with buffer; visible on the board with the date |
| **Standard** | Feature and outcome work | Pulled in Next order |
| **Enabler** | Tech debt, test infra, tooling, platform capability, governance | **Target ≥ 20% of each lane's throughput**, reviewed monthly. Enablers compete on a fixed allocation instead of being argued item by item |
| **Explore** | Spike, prototype, provider behaviour probe, requirement clarification | Timeboxed (≤ 3 days); output is information, a decision draft or a thrown-away prototype |

AIGEM's hard P1 overrides map onto **Expedite**. Its "enabler" handling (19 enablers made visible
by DEC-20260816-10) becomes a standing allocation.

## 3. WIP limits on human attention

AI executors are not the constraint; people are ([00 §4](./00-diagnosis.md#4-where-the-constraint-actually-is)).
So WIP limits are set on the things only humans can do:

| Limit | Default | Why |
|---|---|---|
| Open PRs awaiting a given human reviewer | **≤ 4** | Beyond this, review quality collapses and cycle time explodes |
| Open decisions owned by one person | **≤ 5** | Forces the decision clinic to burn down before more are raised |
| Cards in `Now` per lane | **≤ 2 × human reviewers on the lane** | Keeps finished-but-unreviewed work from piling up |
| Expedite per lane | **1** | Two emergencies at once are one emergency and one mis-classification |

When a limit is hit, agents **stop starting and start finishing**: help review evidence, write
tests for open PRs, break a card smaller, or prepare a decision brief. They do not idle and do not
open more PRs into the queue.

One-in-flight *per agent* is kept as a default for traceability (one agent session → one card),
but any number of agents may work in parallel across lanes and within a lane.

## 4. Breaking dependencies

A dependency is only allowed to **block** a card when it falls in the last row below. Everything
else has a standard way to proceed.

| Dependency kind | Proceed by | Stays blocking for |
|---|---|---|
| **Contract** (needs another lane's API/event/schema) | Consumer and provider agree the contract first (OpenAPI/AsyncAPI/JSON Schema in the product repo); consumer builds against a stub; contract tests on both sides | Promotion to bank-UAT with the real provider |
| **Decision** (someone has not decided) | Build the decision's `default_while_pending` behind configuration or a flag ([05](./05-decisions.md)) | Promotion past dev if the default is not acceptable in UAT |
| **Environment / external** (bank UAT slot, AD technology, VPN/DX, Apigee onboarding, allowlists) | Simulator, WireMock, synthetic data, configurable base URLs; track the external item with owner + chase date in [`state/dependencies.yaml`](../state/dependencies.yaml) | The checkpoint that needs the real thing |
| **Implementation** (needs another card's code) | Slice so the follower needs only the interface; or pair both cards in one lane | Only if slicing fails twice — then it is one card |
| **Genuinely hard** (irreversible data migration against real data, a regulatory ruling, money movement to production) | Nothing — wait, and make the wait visible | Until resolved |

**Rule FL-1.** A card may be marked `blocked` only by naming which row applies and why the
standard way does not work. "Waiting for the stage gate" is not a dependency.

**Rule FL-2.** Every external dependency has an owner and a chase date. A past chase date is a
flow-steward card for the owner, not a silent overdue row.

## 5. Reprioritising and re-slicing

- The **product owner may reorder Next on any day**, without a meeting and without a CR. The
  change is visible in tracker history; that is the audit trail.
- A lane lead may **split** a card at any time. Splitting never needs approval.
- **Scope changes to an outcome** (not to a card) are a Type-2 decision by the PO, recorded in
  one line in [`state/outcomes.yaml`](../state/outcomes.yaml). Only scope changes that alter a
  guardrail, a regulatory commitment or an external contract are Type-1.
- When a requirement flips, **cancel in-flight work cheaply**: close the PR with a link to the
  decision, keep reusable parts. Sunk cost is not a reason to continue.

## 6. The flow steward

The delivery lead's lever in AIGEM was "may force a decision's timing, never its content"
(PA-1). That survives, and is joined by an AI **flow steward** persona
([`personas/cards/flow-steward.md`](../personas/cards/flow-steward.md)) that, daily:

- lists cards aged beyond threshold, blocked cards and their named row from §4;
- lists decisions past SLA and external dependencies past chase date;
- proposes splits, re-lanes and swaps — never decides them;
- posts one short digest; silence when nothing changed.

The old `autopilot.py next` behaviour (pick non-blocked work) becomes the steward's
"what can an agent pull now" answer, across all lanes rather than per stage.
