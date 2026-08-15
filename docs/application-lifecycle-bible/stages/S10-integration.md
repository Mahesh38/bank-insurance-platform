# S10 — Integration & Connectivity

**AIGEM stage:** L5 — Connectivity / Integration · **Owner:** Amit (Engineering) + Mahesh (Architecture)
**Central question:** *Can we talk to the outside world reliably?*

---

## 1. Purpose

Establish working, resilient, secure connectivity to every external system the journey depends on
— before a business journey is built on top of them.

This platform has an unusually heavy integration load: an aggregator, core banking, a payment
gateway, an identity provider, notification channels, and per-insurer behaviour behind the
aggregator. Each is owned by someone else, each fails differently, and each will fail.

> **The stage's real subject is failure.** Anyone can call a working API. This stage is about what
> happens when the provider is slow, returns a malformed payload, rejects the credential, or
> answers "success" to a request that did not succeed.

## 2. Entry criteria

- [ ] GATE-S08 passed — you cannot prove integration behaviour without test infrastructure
- [ ] GATE-S09 passed — you cannot reach partner networks without an environment
- [ ] GATE-S07 passed: integration architecture, resilience patterns, partner trust contracts

## 3. Epics and stories

### S10-E01 — Integration catalogue and governance · *Mahesh + Kalpana*

| ID | Story | Acceptance criteria |
|---|---|---|
| S10-E01-S01 | Catalogue every external dependency | System, owner, protocol, environments, SLA, escalation contact, contract reference |
| S10-E01-S02 | Establish access to every sandbox | Credentials, IP allowlisting and connectivity proven from our egress, per environment |
| S10-E01-S03 | Define the dependency criticality tier | Which failures stop a sale, which degrade it, which are invisible to the customer |
| S10-E01-S04 | Track integration readiness as a delivery dependency | Each with a required-by date; overdue items escalate under Kalpana's decision-forcing authority |

### S10-E02 — Aggregator integration (1SB) · *Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S10-E02-S01 | HTTP client with auth, timeouts and no-retry on 401 | Single HTTP stack; credentials from secrets management |
| S10-E02-S02 | Error normalisation to the bank problem model | Provider error shapes never surface to bank callers |
| S10-E02-S03 | Async job and polling infrastructure | Shared across quote and proposal; multi-instance safe |
| S10-E02-S04 | LOB routing behind ports | Adding an LOB requires no orchestration change |
| S10-E02-S05 | Raw payload capture, encrypted | For every provider exchange, retained per the retention schedule |
| S10-E02-S06 | Idempotency on mutating operations | Replay produces the original result, never a duplicate effect |

### S10-E03 — Core banking integration (CBS) · *Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S10-E03-S01 | Customer lookup by CIF | ETB identification with the fields the journey needs |
| S10-E03-S02 | KYC and demographic retrieval for pre-fill | Only fields with a stated purpose; classification honoured |
| S10-E03-S03 | Relationship verification | Confirms the ETB eligibility rule |
| S10-E03-S04 | Define degraded behaviour when CBS is unavailable | Product decision recorded: block, or proceed with manual capture |

### S10-E04 — Payment gateway integration · *Amit + Deepali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S10-E04-S01 | Payment session and link generation | Link issued to the customer's device only — never rendered on an RM device |
| S10-E04-S02 | Payment status callback handling | Authenticated, replay-protected, integrity-verified |
| S10-E04-S03 | Status reconciliation by polling | Callback loss cannot leave a payment in unknown state indefinitely |
| S10-E04-S04 | Handle the uncertain-payment case | Defined behaviour when payment state cannot be determined; never a double charge |
| S10-E04-S05 | Refund and failure paths | Per the S03 payment rules |

### S10-E05 — Identity integration · *Amit + Deepali*

| ID | Story | Acceptance criteria |
|---|---|---|
| S10-E05-S01 | Bank AD federation for workforce identity | Behind the provider adapter; no provider type leaks |
| S10-E05-S02 | Token-hiding BFF session | The client never receives OAuth tokens |
| S10-E05-S03 | Customer authentication for DIY journeys | Digital banking SSO for ETB |
| S10-E05-S04 | Certification metadata retrieval | SP licence validity available to the attribution rule |

### S10-E06 — Notification integration · *Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S10-E06-S01 | SMS delivery for OTP and payment links | Delivery status tracked; failures visible |
| S10-E06-S02 | Email delivery for documents and confirmations | Attachment handling; bounce handling |
| S10-E06-S03 | Template management with versioning | Regulated content versioned like any other regulated artefact |

### S10-E07 — Resilience · *Shivanshi + Amit*

| ID | Story | Acceptance criteria |
|---|---|---|
| S10-E07-S01 | Timeouts on every external call | No unbounded call anywhere; values justified by the NFR sheet |
| S10-E07-S02 | Retry with backoff and jitter, where safe | Only on idempotent operations; never on 401; bounded attempts |
| S10-E07-S03 | Circuit breakers per dependency | Trip thresholds and recovery defined; state observable |
| S10-E07-S04 | Bulkhead isolation | One slow provider cannot exhaust shared resources |
| S10-E07-S05 | Rate limiting toward providers | Respects contracted limits; never amplifies under retry |
| S10-E07-S06 | Dependency health observability | Availability, latency and error rate per provider, **measured from our side** |

## 4. Validation tests

| ID | Validates | Method | Pass condition |
|---|---|---|---|
| S10-VT-01 | Happy path works per integration | Sandbox call | Success with the expected payload |
| S10-VT-02 | Timeouts are enforced | Stub a provider that never responds | Call times out at the configured value; caller gets a normalised error |
| S10-VT-03 | Malformed responses are handled | Stub invalid JSON, truncated payloads, wrong content type | No crash; normalised error; nothing corrupted |
| S10-VT-04 | Auth failure does not retry-storm | Stub 401 | No retry; alert raised |
| S10-VT-05 | Circuit breaker trips and recovers | Stub sustained failure, then recovery | Breaker opens, half-opens, closes; state observable |
| S10-VT-06 | Idempotency holds | Replay a mutating request 5× | One effect, identical response |
| S10-VT-07 | Payment uncertainty is safe | Stub a callback that never arrives | Reconciliation resolves state; no double charge |
| S10-VT-08 | Provider types do not leak | ArchUnit | No provider type outside its adapter package |
| S10-VT-09 | Credentials rotate without outage | Rotate in UAT | Calls continue |
| S10-VT-10 | Rate limits are respected | Drive load beyond contracted limits | Requests shaped locally; provider limit never breached |

## 5. Exit gate — GATE-S10

| # | Criterion | Level | Evidence artefact |
|---|---|---|---|
| S10-G1 | Integration catalogue complete with owners and SLAs | E1 | Catalogue |
| S10-G2 | Connectivity proven to every R0 dependency in UAT | E4 | Successful calls with correlation IDs |
| S10-G3 | Error normalisation verified for every integration | E4 | Test results including malformed and timeout cases |
| S10-G4 | Resilience patterns implemented and proven | E4 | Failure-injection test results |
| S10-G5 | Idempotency proven on every mutating operation | E4 | Replay test results |
| S10-G6 | Credentials in secrets management; rotation exercised | E3 | Rotation record |
| S10-G7 | Third-party security assessment complete per partner | E2 | Assessment records |
| S10-G8 | Dependency observability live | E4 | Dashboards per provider |
| S10-G9 | Provider isolation enforced by fitness function | E4 | ArchUnit run |

**Approvers:** Mahesh (AP) · Amit (AP) · Deepali (AP, B) · Shailja (AP, third-party data) ·
Shivanshi (AP) · Swapnali (RV) · Kalpana (RV)

## 6. Current position in this repository — 🟡 Partial

**1SB integration (S10-E02) is genuinely well built** and is the strongest engineering in the
repository: ports and adapters with ArchUnit-enforced isolation, a single HTTP stack with auth and
401 no-retry, error normalisation to a bank problem model, async job and polling infrastructure
shared across quote and proposal, LOB routing, encrypted raw-payload capture, and idempotency on
mutating APIs. Most of E02's stories are substantively done.

**Everything else in this stage is absent:**

| Integration | State |
|---|---|
| Core banking (CBS) | **Absent** — no customer lookup, no KYC pre-fill, no ETB verification |
| Payment gateway | **Absent** — the money path has no integration at all |
| Bank AD federation | **Deferred** to WS-2 Phase 2; technology unconfirmed |
| Notification (SMS/email) | **Absent** — OTP and payment links have no delivery channel |
| Resilience | **Partial** — timeouts present; circuit breaker parked to Phase 5.5; bulkheads and rate limiting absent |
| Dependency observability | **Absent** — no backend to observe into (S09) |

**The pattern is worth naming.** The one integration with a willing partner, good documentation
and a clear API was built to a high standard. The integrations that require internal coordination
— CBS, the payment gateway, AD — were not started. That is not an engineering failure; it is an
absent dependency-management function, which is exactly what S10-E01-S04 and Kalpana's
decision-forcing authority exist to supply.

**Without CBS, payment and notification, no business journey can complete.** These three are the
critical path to S11, and their required-by dates should be set now, during the foundation
recovery, so partner lead times run in parallel rather than in series.

## 7. Premature at this stage

Multi-LOB expansion · a second aggregator · provider routing flags · performance tuning of
provider calls.

Prove one provider path per integration class works and fails safely. Breadth is S13.
