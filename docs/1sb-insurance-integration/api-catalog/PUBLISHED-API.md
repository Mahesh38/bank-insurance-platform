# Published bank-facing API — 1SB Integration Service

**Satisfies (partly):** WS-1 Phase 4 exit criterion **4.2** — "OpenAPI published to the internal
portal; consumer collection available"
([04-STAGE_GATES.md §6](../../governance/04-STAGE_GATES.md#6-project-gates-l3))
**Owner:** Engineering · **Consumers:** bank app / BFF teams

> **Status honesty.** Two of the three parts of 4.2 are done in this repository: the OpenAPI
> document is generated, published here, and verified against the code on every build; the
> consumer collection exists and is checked against that document. The third part — publication
> to the **bank's internal developer portal** — cannot be done from this repository, because the
> portal is not something this repo has or can reach. See §4 for exactly what remains and who
> owns it.

---

## 1. Artefacts

| Artefact | Path | Generated from |
|---|---|---|
| OpenAPI 3 document | [`openapi/1sb-integration-service.json`](./openapi/1sb-integration-service.json) | springdoc, at build time |
| Consumer collection (Postman v2.1) | [`collections/1sb-integration-term-journey.postman_collection.json`](./collections/1sb-integration-term-journey.postman_collection.json) | hand-written, verified against the document |

Both are also uploaded as build artefacts by the **Build & test** workflow, so any CI run has a
downloadable copy without cloning.

**Live endpoints on a running instance** (`springdoc-openapi`):

| URL | Purpose |
|---|---|
| `/v3/api-docs` | The document, as JSON |
| `/swagger-ui.html` | Interactive browser UI |

---

## 2. Why the document is committed as well as generated

springdoc generates the specification at runtime. That is convenient and useless for a consumer
who wants to generate a client before the service is deployed, review the surface in a pull
request, or diff what changed between releases.

Committing a snapshot solves that and creates the obvious risk: the snapshot goes stale the
first time somebody adds a field, and nobody notices until a consumer's generated client breaks.

So the snapshot is **verified, not just stored**. `OpenApiContractTest` fails the build when:

1. the committed document no longer matches the running API;
2. any bank-facing route is missing from it;
3. an `/internal/**` route leaks into it (bank apps must never see the persistence API);
4. the consumer collection calls a route the document does not expose.

A diff in the committed document is therefore an **API change review item**. A breaking change
to a public contract needs an Architecture verdict
([00-GOVERNANCE §8](../../governance/00-GOVERNANCE.md#8-non-negotiables), non-negotiable 3).

### Regenerating after an intentional API change

```bash
./gradlew :services:1sb-integration-service:test \
    --tests '*OpenApiContractTest' -DupdateOpenApi=true
```

Then commit the regenerated file **in the same pull request as the code change** — a separate
"update the spec" commit defeats the point of reviewing the two together.

The `servers` block is stripped during generation: springdoc derives it from the request host,
so leaving it in would make the document differ between a local run and a deployed one. The base
URL belongs in the consumer's own configuration.

---

## 3. Using the consumer collection

1. Import the collection into Postman (or Bruno, via its Postman importer).
2. Create an environment with `baseUrl` set to the UAT host — no trailing slash.
3. Set `actorId` to a real operator identifier. It is sent as `X-Actor-Id` and lands in the
   audit trail; `rm-demo-1` in a real UAT run makes the audit log useless.
4. Run the folder top to bottom with the Collection Runner. Each request captures the identifier
   the next one needs, so the journey chains automatically.

**What the collection is for:** proving connectivity, showing the async job pattern, and making
the failure modes familiar before they are encountered in production. It is not a load test and
not a substitute for the UAT sign-off in criterion 4.3.

### Things integrators get wrong, in order of frequency

| Symptom | Cause |
|---|---|
| `400 MISSING_IDEMPOTENCY_KEY` | Every mutating `/v1` request needs `Idempotency-Key`. Reads do not. |
| Empty `offers` array | The job is still `PENDING`/`RUNNING`. Quoting is asynchronous — poll until terminal. |
| `409 IDEMPOTENCY_CONFLICT` | Same key, different body. Use a new key for a genuinely new request. |
| Repeating a request returns the old job | Correct behaviour, not a cache. Same key means same job. |
| `409 PROPOSAL_NOT_PAYABLE` | Payment attempted before the proposal job reached a payable state. |
| `422 PROPOSAL_REJECTED` | A business rejection. Not retryable — fix the values, use a new key. |
| `502 UPSTREAM_UNAVAILABLE` | The insurer is down or slow. Retryable; the service already backs off internally. |
| `502 UPSTREAM_AUTH_FAILURE` | **Our** credentials to 1SB failed. Not retryable by you — see the [operations runbook §4](../service-ssot/OPERATIONS-RUNBOOK.md). |

---

## 4. What is still outstanding for criterion 4.2

| Part | State | Owner |
|---|---|---|
| OpenAPI document generated and published in-repo | ✅ Done | Eng |
| Document verified against the code on every build | ✅ Done | Eng |
| Consumer collection available and verified | ✅ Done | Eng |
| **Publication to the bank's internal developer portal** | ❌ **Outstanding** | Eng + Platform |

The portal step needs a target that does not exist in this repository: a portal URL, credentials
or a publishing account, and a decision about whether publication happens on merge to `main` or
on tagged release. None of those can be invented here — a fabricated portal URL in the gate
evidence would be worse than an honest gap.

**To close it:** add a publish step to `.github/workflows/build.yml` that pushes
`openapi/1sb-integration-service.json` to the portal, then record the portal URL in the gate
evidence for 4.2. The document itself is already in the right shape for that — this is a
plumbing and credentials task, not an API task.

---

## 5. Related

| Document | Why |
|---|---|
| [api-catalog/README.md](./README.md) | The **upstream** 1SB API catalogue — what we consume, not what we expose |
| [OPERATIONS-RUNBOOK.md](../service-ssot/OPERATIONS-RUNBOOK.md) | What to do when the upstream errors reach a consumer |
| [UAT-ENABLEMENT.md](../service-ssot/UAT-ENABLEMENT.md) | Onboarding a bank caller onto UAT (criterion 4.3) |
| `TermJourneyE2EIT` | The same journey as an automated test |
