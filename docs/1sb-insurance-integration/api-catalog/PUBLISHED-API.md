# Bank-facing API — 1SB Integration Service

**Satisfies:** WS-1 Phase 4 exit criterion **4.2**, as amended by
**[CR-002](../../governance/registers/DECISION-REGISTER.md#3-change-requests)**
([04-STAGE_GATES.md §6](../../governance/04-STAGE_GATES.md#6-project-gates-l3))
**Owner:** Engineering · **Consumers:** bank app / BFF teams, inside the VPC

> **There is no portal and no published URL.** The OpenAPI specification is a **local and dev
> testing artefact**: it exists so someone wanting to exercise the API does not have to
> hand-build a request collection. It is **not** a product surface, and the spec endpoints are
> **never served on UAT or production** — see §4. This is a standing constraint applying to every
> service on the platform, not a property of this one.

---

## 1. Artefacts

| Artefact | Path |
|---|---|
| OpenAPI 3 document | [`openapi/1sb-integration-service.json`](./openapi/1sb-integration-service.json) |
| Consumer collection (Postman v2.1) | [`collections/1sb-integration-term-journey.postman_collection.json`](./collections/1sb-integration-term-journey.postman_collection.json) |

Both are committed, and both are uploaded as build artefacts by the **Build & test** workflow so
any CI run has a downloadable copy without cloning.

**The committed file is the artefact.** There is nowhere else to get it — that is the point.

---

## 2. Why the document is committed rather than served

springdoc generates the specification at runtime. If that were the only copy, you would need a
running instance to read the contract, review a surface change in a pull request, or diff what
changed between releases. Committing a snapshot solves all three.

It also creates the obvious risk: the snapshot goes stale the first time somebody adds a field,
and nobody notices until a generated client breaks.

So the snapshot is **verified, not merely stored**. `OpenApiContractTest` fails the build when:

1. the committed document no longer matches the running API;
2. any bank-facing route is missing from it;
3. an `/internal/**` route leaks into it — bank apps must never see the persistence API;
4. the consumer collection calls a route the document does not expose.

A diff in the committed document is therefore an **API change review item**. A breaking change to
a public contract needs an Architecture verdict
([00-GOVERNANCE §8](../../governance/00-GOVERNANCE.md#8-non-negotiables), non-negotiable 3).

### Regenerating after an intentional API change

```bash
./gradlew :services:1sb-integration-service:test \
    --tests '*OpenApiContractTest' -DupdateOpenApi=true
```

Commit the regenerated file **in the same pull request as the code change** — a separate "update
the spec" commit defeats the point of reviewing the two together.

The `servers` block is stripped during generation: springdoc derives it from the request host, so
leaving it in would make the document differ between a local run and a deployed one, and there is
no canonical host to name anyway.

---

## 3. Using the artefacts locally

### The collection

1. Import into Postman (or Bruno, via its Postman importer).
2. Create an environment with `baseUrl` pointing at your local or dev instance — no trailing
   slash.
3. Set `actorId` to a real operator identifier. It is sent as `X-Actor-Id` and lands in the audit
   trail; leaving the placeholder makes that trail useless.
4. Run the folder top to bottom with the Collection Runner. Each request captures the identifier
   the next one needs.

### Swagger UI, when you want it

Disabled by default in every environment. For **local exploration only**:

```bash
SPRINGDOC_ENABLED=true ./gradlew :services:1sb-integration-service:bootRun
# then http://localhost:8080/swagger-ui.html
```

The `uat` and `prod` profiles pin it off, so that variable has no effect there — deliberately
(§4).

### Generating a client

The committed JSON is a normal OpenAPI 3 document; point any generator at the file path rather
than at a URL:

```bash
openapi-generator-cli generate \
  -i docs/1sb-insurance-integration/api-catalog/openapi/1sb-integration-service.json \
  -g java -o ./generated-client
```

---

## 4. Why nothing is exposed

`1sb-integration-service` is reachable **inside the VPC only**. It is not on public cloud and has
no external consumer, so "publishing a contract" has no audience — the callers who need it are
colleagues who can read the repository.

Serving `/v3/api-docs` and Swagger UI anyway would buy nothing and cost something: an API browser
on a host carrying real journey data is attack surface, and an interactive request builder on UAT
is a way to create records nobody intended.

### How it is enforced

| Layer | Control |
|---|---|
| Default | `springdoc.api-docs.enabled` / `swagger-ui.enabled` default to **false** in `application.yml`, so a new environment is safe before anyone configures it |
| `uat`, `prod` | Pinned to `false` in the profile files, so `SPRINGDOC_ENABLED` cannot re-enable them |
| Test | `OpenApiNotExposedTest` boots the `uat` and `prod` profiles **with `SPRINGDOC_ENABLED=true`** and asserts neither endpoint answers — it proves the pin, not just the default |
| Governance | Standing constraint 9 in [01-CURRENT_STATE §5](../../governance/01-CURRENT_STATE.md#5-standing-constraints-apply-to-every-triage-in-this-repo); `never` list in `CURRENT-STATE.yaml` |

> **This was a live gap, not a hypothetical.** Before CR-002 springdoc was enabled by default in
> every profile, and `render.yaml` publishes port 8080 — so on that deployment the Swagger UI was
> publicly reachable. CR-002's `outstanding` field asks the Security Architect to determine how
> long that was the case.

**Applies to every service from now on.** A new service may carry an OpenAPI specification; none
may serve it outside local and dev. Adding a service that does is SF4 / REJECT unless it arrives
as a change request.

---

## 5. Things integrators get wrong, in order of frequency

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
| `404` on `/swagger-ui.html` in a deployed environment | Intended — §4. |

---

## 6. Related

| Document | Why |
|---|---|
| [api-catalog/README.md](./README.md) | The **upstream** 1SB API catalogue — what we consume, not what we expose |
| [OPERATIONS-RUNBOOK.md](../service-ssot/OPERATIONS-RUNBOOK.md) | What to do when upstream errors reach a consumer |
| [UAT-ENABLEMENT.md](../service-ssot/UAT-ENABLEMENT.md) | Onboarding a bank caller onto UAT (criterion 4.3) |
| `TermJourneyE2EIT` | The same journey as an automated test |
