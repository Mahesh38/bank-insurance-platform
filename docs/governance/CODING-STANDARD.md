# Java coding standard — bank insurance platform

Status: mandatory for every person and every AI agent that writes Java in this repository.
Baseline: Java 21, Spring Boot 3.5.x, imperative Spring MVC, Gradle. This file is the only coding standard. `ENGINEERING-AND-SECURE-CODING-STANDARDS.md` holds the ENG, SEC, and OBS ids. `CODE-REVIEW-STANDARD.md` holds coverage and the logging merge bar. They do not replace this file.

An agent reads this file before generating or modifying Java, SQL, or tests. A temporary exception is a row in the service README under "Standard exceptions", with the owner, the reason, and the review date. Agents do not invent exceptions.

Work item: `SUG-20261026-cds`.

## 1. How to use this file

Humans review against section 23.
Agents follow this order:

1. Section 2, then the baseline in section 3. Do not generate Boot 4 code.
2. Section 5, and place every new file before writing it. Name it with section 7.
3. Section 8 before introducing a class. Copy an accepted snippet. For any HTTP endpoint, read section 11 before choosing the path, method, or status. Read section 17 before authentication, secrets, or a trust boundary, and section 18 before adding a log line. Then read the layer section for the code you are about to write (sections 10–15).
4. Section 19, and write the tests with the production code.
5. Section 23 before you finish.

If two rules seem to conflict, the dependency rule (section 6) and the file-grouping rules (section 5) win. Prefer the smaller design. When this file and the platform error contract disagree, the error contract wins. When this file and a parked decision disagree, the parked decision wins: do not implement Boot 4 while `SUG-20261008-sb4` is parked.

Examples use `com.bank.platform.lead`. Do not introduce a second root package inside a service.

## 2. Non-negotiables

- A service is one business capability. Inside it, code is layered `api`, `application`, `domain`, `adapter`, `config`. Shared technical code lives in `libs/*`, not copied into a service.
- Domain code imports neither Spring, nor Jakarta Persistence, nor Jackson, nor the Servlet API.
- A controller calls an application service. It does not call a repository, a JPA entity, or an adapter.
- An application service is a concrete class. It orchestrates. Business invariants live in the domain model.
- Adapters implement ports. They contain no business rules. 1SB types stay in `adapter.onesb`.
- Constructor injection only. Dependencies are `private final`. `@Autowired` appears only when the class has more than one constructor, and then only on the constructor Spring must call.
- Requests, responses, commands, views, and value objects are Java records. Aggregates are classes. JPA entities are classes with a no-arg constructor, and they are not the aggregate.
- Bean Validation guards HTTP input. The domain model still rejects an invalid state in its constructor or method.
- Every error response comes from `ErrorCatalogue` through `ServiceErrors` and `PlatformErrorHandler`. A failure is never HTTP 200. A 500 body has catalogue wording and no exception text.
- HTTP follows section 11. Paths are nouns. Methods keep their HTTP meaning. The server stores no client session except the workforce BFF, which hides tokens.
- Schema changes are Flyway migrations. `ddl-auto` is `validate`. `open-in-view` is `false`. `1sb-integration-service` owns no Flyway and no JPA.
- `@Transactional` sits on the application service. Query methods are `readOnly = true`.
- No Lombok. No `I` prefix, no `Impl` suffix, no `DTO` or `Dtos` in a type name, no `util`, `helper`, `dto`, `entity`, or `repo` package.
- Null checks, blank checks, and collections use the JDK. `BeanUtils`, ModelMapper, Commons Lang, and Guava are not used. Mapping is a hand-written `from` or a `{Name}Mapper` in the owning feature.
- A behavior change ships with the test type in section 19. The ArchUnit test stays green. Coverage floors in `CODE-REVIEW-STANDARD.md` stay green.
- Outbound HTTP is `RestClient`, with a connect timeout and a read timeout.
- Logs never contain PAN, Aadhaar, phone, email, health attributes, tokens, secrets, or a URI that still carries `q`.
- Bank applications never call 1SB or a database directly.
- A new library must be on the approved list in section 21, or the change must say why and a human must accept it.
- A new condition on a flow that already has a rule list is a new rule and a new test. The use-case body stays as it is.
- Names use section 7. Layout is Google Java Format 1.22, applied to every Java file. There is one layout.

## 3. Technology baseline

| Choice | Standard |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5.x, imported through the platform BOM. Do not bump Boot inside a feature change. |
| Web | Imperative MVC, `spring-boot-starter-web`. One service is either MVC or WebFlux. New services are MVC. WebFlux is not added to satisfy a style preference. |
| API style | JSON over HTTP. Public contracts are OpenAPI-first. |
| Bean Validation | `spring-boot-starter-validation` |
| Persistence | JDBC or Spring Data JPA, PostgreSQL, Flyway. H2 in `MODE=PostgreSQL` is allowed for slice tests only. A test that claims PostgreSQL behavior uses Testcontainers or the existing Postgres IT. |
| Mapping | Hand-written `from` or `{Name}Mapper`. Not MapStruct, not BeanUtils. |
| Security | Platform security library and the service's own filter chain. Tokens for the workforce client stay in the BFF. |
| Operations | Actuator, Micrometer, `RequestDiagnosticFilter`, catalogue errors, `bank.error.count` |
| Tests | JUnit Jupiter, AssertJ, Mockito, WireMock, ArchUnit. Testcontainers where a test claims PostgreSQL. |
| Build | Gradle Kotlin DSL. Do not convert a module to Maven inside a feature change. |

Boot 4 (`spring-boot-starter-webmvc`, `@MockitoBean`, Jackson 3 `JsonMapper`) is parked under `SUG-20261008-sb4`. Agents generate Boot 3.5 code: `spring-boot-starter-web`, `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`, `com.fasterxml.jackson.databind.ObjectMapper`, `@MockBean` only where an existing test already uses it. Do not introduce `@MockBean` into a new test; prefer a constructor and a plain Mockito mock. `javax.*` is never used; the package is `jakarta.*`.

Do not add `RestTemplate` to new code. The one existing `RestTemplate` is not a pattern.

## 4. Repository layout

This repository is a multi-module Gradle build. One deployable service is one Gradle module under `services/`. Shared code is a module under `libs/`. Do not split one service into several Gradle modules.

```text
bank-insurance-platform/
├── CODING-STANDARD.md is not here — this file is the standard
├── build.gradle.kts
├── config/checkstyle/
├── libs/bank-common-*/
├── services/<capability>-service/
│   ├── build.gradle.kts
│   └── src/main/java/com/bank/.../<capability>/
│       ├── <Capability>Application.java
│       ├── api/
│       ├── application/
│       ├── domain/
│       ├── adapter/
│       └── config/
└── src/test/java/   # mirrors main
```

- Gradle project path is `:services:<artifact>` or `:libs:<artifact>`.
- The application class is `{Capability}Application` and lives in the service's root package. Do not add a second `@SpringBootApplication`.
- `src/test/java` mirrors `src/main/java`.
- `.gitignore` includes local env files and IDE metadata. Secrets never go in git.

`bank-persistence-service` is the shared store. Its HTTP types live in `api`. Its JPA types and Spring Data interfaces live in `adapter.persistence`. It does not grow a second business capability.

## 5. Package layout and file grouping

A service module is one capability. Technical layering happens inside that module, not as a second business package, and not as a technical package at the root (`dto`, `entity`, `repo`, `util`).

```text
lead/
├── package-info.java
├── api/                  # HTTP in and HTTP out
├── application/          # use cases, commands
├── domain/               # aggregates, value objects, domain exceptions, repository ports
├── adapter/
│   ├── persistence/      # JDBC, JPA, outbound HTTP to the platform store
│   ├── rules/            # rule-engine adapter
│   └── <system>/         # one package per outside system
└── config/
```

Feature-specific `@Configuration` and `@ConfigurationProperties` live in `config`. Application-wide beans (security filter chain, `Clock`, the error handler) live in `config` or in `libs`.

### Grouping algorithm

1. Reuse the service module. Do not create a new module for a type.
2. HTTP types and the controller go in `api`.
3. Use-case orchestration, commands, and read models go in `application`.
4. A rule that must remain true for the business object goes in `domain`.
5. JDBC, JPA, HTTP clients, and brokers go in `adapter`. The interface they implement stays in `domain` (a repository) or `application` (every other port).
6. A type moves to `libs/*` only when a second service already needs it and it has no business meaning that belongs to one service.
7. One primary type per file. A record used by only one parent stays nested in that parent.
8. Stop. Do not create a package for a pattern name or a hypothetical future feature.

### Order inside a source file

1. A license header only when this service already has one.
2. The `package` statement.
3. Imports, one type per line. No star import.
4. Exactly one top-level type. A record, exception, or builder used only by that type stays nested.

Class members, top to bottom: `static final` constants, instance fields, constructors, behavior, nested types. A public method is followed by the private helpers that exist only for it.

| Type | Method order |
| --- | --- |
| Controller | `POST` collection, `GET` collection, `GET` item, `PUT`, `PATCH`, action `POST`s, `DELETE` |
| Aggregate | static factories, then mutators, then queries |
| Application service | constructor, the use-case methods, then the private helpers of each |
| Test | the success path, then each rejection |

### Where a new file goes

| You are adding | Package and name |
| --- | --- |
| An endpoint on an existing resource | `api/{Resource}Controller.java` |
| An HTTP body | `api/{Action}{Resource}Request.java` or `{Resource}Response.java` |
| A write use case | `application/{Action}{Resource}Service.java` and `{Action}{Resource}Command.java` |
| A business invariant | a method on the aggregate in `domain` |
| A value with rules of its own | `domain/{Name}.java` as a record |
| Loading and saving an aggregate | `domain/{Aggregate}Repository.java` and `adapter/persistence/{Aggregate}Store.java` |
| A table or column | `src/main/resources/db/migration/V{n}__{description}.sql` and the JPA type in `adapter/persistence` |
| A call to another service | port in `application` or `domain`, adapter in `adapter/<system>` |
| A business failure | throw `ServiceErrors` with an `ErrorCodes` constant. Do not invent a second HTTP body. |
| A new condition on a flow that already has a rule list | a new rule class or a new rule-set entry. The use-case method is not edited. |
| A cross-cutting servlet filter | `libs/bank-common-observability` or the service `config` |

### What is never a package

`controller`, `service`, `repository`, `repo`, `entity`, `model`, `dto`, `util`, `utils`, `helper`, `helpers`, `common`, `base`, `generic`.

A class named `*Utils`, `*Helper`, `*Constants`, `*Dtos`, or `*Manager` is the wrong shape. Put the behavior on the type that owns the data, or name the type for the job it does.

`libs/bank-common-*` is the only cross-service home, and each library has one job (error, security, audit, observability, secrets, events).

## 6. Dependency rule

Source dependencies point inward.

```text
api  →  application  →  domain
              ▲
           adapter
```

| Package | May depend on | Must not depend on |
| --- | --- | --- |
| `domain` | Java, `bank-common-error` types that are not Spring | Spring, JPA, Jackson, Servlet, `api`, `application`, `adapter` |
| `application` | `domain`, catalogue errors, Java | `api`, `adapter`, JPA entities, Servlet |
| `api` | `application`, `domain` (to map), catalogue errors | `adapter`, JPA entities |
| `adapter` | the port it implements, JPA, HTTP clients | `api`, business decisions that belong on the aggregate |
| `config` | Spring, and the types it wires | business rules |

The domain is persistence-ignorant. The JPA entity is not the aggregate. The controller return type is not the aggregate and not the entity.

| Port | Defined in | Implemented in | Name |
| --- | --- | --- | --- |
| Load and save an aggregate | `domain` | `adapter/persistence` | `{Aggregate}Repository` |
| Call another system | `application` or `domain` | `adapter/<system>` | `{System}Client` or `{System}Port` |

An application service gets an interface only when a second implementation exists in this codebase. Repository and client ports are interfaces because the dependency rule needs them.

## 7. Naming

Four shapes cover every name. An acronym inside a name is a word: `LeadId`, `HttpHeaders`, `JsonBody`. `LeadID` and `HTTPHeaders` are the wrong shape. Types from the JDK keep their published spelling (`UUID`, `URI`).

| Kind | Shape | Accepted | Rejected |
| --- | --- | --- | --- |
| Class, interface, record, enum | UpperCamelCase noun | `LeadController`, `CreateLeadService` | `leadController` |
| Method, parameter, field, local | lowerCamelCase | `createLead()` | `CreateLead()` |
| Constant | UPPER_SNAKE_CASE | `MAX_ATTEMPTS` | `maxAttempts` on a constant |
| Package | lowercase, no underscores | `com.bank.platform.lead` | `com.bank.platform.lead.dto` |

| Kind | Pattern | Example |
| --- | --- | --- |
| Application class | `{Capability}Application` | `LeadApplication` |
| Controller | `{Resource}Controller` | `LeadController` |
| HTTP request | `{Action}{Resource}Request` | `CreateLeadRequest` |
| HTTP response | `{Resource}Response` | `LeadResponse` |
| Application service | `{Resource}ApplicationService` or `{Action}{Resource}Service` | `LeadApplicationService` |
| Command | `{Action}{Resource}Command` | `CreateLeadCommand` |
| Aggregate | singular business noun | `Lead` |
| Identifier | `{Aggregate}Id` when it is a value object | `LeadId` |
| Domain exception | use `ServiceErrors` and `ErrorCodes` | `ErrorCodes.INVALID_REQUEST` |
| Repository port | `{Aggregate}Repository` | `LeadRepository` |
| Persistence adapter | `{Aggregate}Store` or `Jdbc{Aggregate}Repository` | `JdbcLeadRepository` |
| JPA entity | `{Aggregate}JpaEntity` or `{Name}Entity` in `adapter.persistence` | `IntegrationJobEntity` |
| Outbound port | `{System}Client` or `{System}Port` | `OneSbQuotePort` |
| Properties | `{Concern}Properties` | `LoginJourneyProperties` |
| Configuration | `{Concern}Config` | `SecurityConfig` |
| Unit test | `{Type}Test` | `LeadTest` |
| Integration test | `{Type}IT` | `JobApiPostgresIT` |

Forbidden type names: `ILeadRepository`, `LeadRepositoryImpl`, `LeadServiceImpl`, `LeadDTO`, `LeadDtos`, `LeadManager`, `LeadHelper`, `LeadUtils`, `BaseController`, `AbstractService`.

- A lookup that throws when the row is missing is `get`. A lookup that may be absent is `find` and returns `Optional`. A collection lookup is `list`.
- Collections are never null. Return `List.of()` or an empty page.
- `Optional` is a return type. It is not a field, not a parameter, and not a wrapper around a collection.
- HTTP paths: public workforce routes are `/api/v1/{resources}`. Internal service routes are `/internal/v1/{resources}`. Resources are plural kebab-case. The path does not contain a verb. A state change that is not CRUD is its own sub-resource.
- JSON fields are camelCase.
- Tables are snake_case plural. Columns are snake_case. Primary key column is `id` unless an existing table already uses a specific name (`job_id`, `lead_id`). Migrations are `V{n}__{snake_description}.sql`. `{n}` only grows. Do not use timestamps as versions.
- Test method: `should{Expected}When{Condition}`, or `behaviour_expectedOutcome` with a `@DisplayName` that says the same thing. The success path comes first.

## 8. Design principles

Copy an accepted snippet. Do not add a class to satisfy a principle name.

### 8.1 Single responsibility

Accepted. One application service method does one use case. Validation of the business state is a method on the aggregate.

Rejected. A `LeadManager` that creates, assigns, searches, expires, and talks to 1SB.

### 8.2 Open/closed, and adding a condition

The aggregate keeps the invariants of its own state. `Lead` still rejects an illegal transition. That check does not move into a strategy.

Conditions that arrive over time (eligibility, suitability, disclosures, fees) become a rule list at the third condition. Two conditions stay ordinary branches. The third condition is the last time the use case is restructured. After that, the use case depends on the rule engine or on `List<{Flow}Rule>` and does not grow. Each rule is one class and one test.

Accepted.

```java
public interface AssignLeadRule {
    void check(Lead lead, AssignLeadCommand command);
}

public void assign(AssignLeadCommand command) {
    Lead lead = leads.get(command.leadId());
    rules.forEach(rule -> rule.check(lead, command));
    lead.assign(command.principalId(), clock.instant());
    leads.save(lead);
}
```

Rejected. Another `else if` in the use case for a condition the business will extend next quarter.

A calculation that was shown to a customer, or an amount that was posted, stores the rule id, the effective date, and the amount that was produced. A later rule change inserts a new decision. It does not update the stored amount.

### 8.3 Dependency inversion

Accepted. `LeadRepository` is an interface in `domain`. `JdbcLeadRepository` is the class in `adapter.persistence`.

Rejected. An interface with one implementation whose only purpose is a `*Service` suffix, and a `*ServiceImpl` beside it.

### 8.4 DRY, after the third copy

Two copies of a ten-line value object are acceptable. The third use, by a second service, is the moment to extract into `libs`. Do not extract a `Utils` class for one caller.

### 8.5 Composition over inheritance

Accepted. A collaborator passed to the constructor.

Rejected. `BaseController`, `AbstractService`, or a class hierarchy whose only job is shared fields.

### 8.6 Fail fast

Constructors and aggregate methods reject invalid state with `Objects.requireNonNull` and a domain failure through `ServiceErrors`. They do not return null to mean "invalid".

### 8.7 What the agent checks before generating a class

- Is there already a type in this service that owns this behavior? Add a method there.
- Is this a request, response, command, or value? It is a record.
- Is this an aggregate with a lifecycle? It is a class with factories and intention-revealing methods.
- Is this Spring Data or JDBC? It stays in `adapter` and implements a port.
- Does a second implementation exist? If not, the application service is a concrete class.
- Can the JDK express this without a new library? Then no new library.

## 9. Design patterns

Use a pattern when the problem it solves is present. Name the type for the business or the technology, not for the pattern (`LeadPolicy`, not `StrategyPattern`).

| Pattern | Use it when | Shape |
| --- | --- | --- |
| Ports and adapters | At every service boundary. This is the default. | Sections 5 and 6 |
| Rule list | The third condition on a flow the business will extend | Section 8.2, or the rule engine |
| Catalogue | An error, an audit action, a metric name | `ErrorCodes`, `AuditActions` |
| Anti-corruption | A call to 1SB or any vendor | `adapter.onesb` or `adapter/<vendor>` only |

Do not add Factory, Strategy, CQRS, Kafka, Event Sourcing, WebFlux, or Redis because the pattern is well known. A new runtime component needs an ADR.

## 10. Layer rules

### API

```java
@RestController
@RequestMapping("/internal/v1/leads")
public class LeadController {

    private final LeadApplicationService leads;
    private final ServiceErrors errors;

    public LeadController(LeadApplicationService leads, ServiceErrors errors) {
        this.leads = Objects.requireNonNull(leads);
        this.errors = Objects.requireNonNull(errors);
    }

    @PostMapping
    public ResponseEntity<LeadResponse> create(
            @RequestHeader("X-Actor-Id") String actorId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateLeadRequest request) {
        Lead lead = leads.create(request.toCommand(actorId), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(LeadResponse.from(lead));
    }
}
```

The controller does not catch an exception to build its own error body. It throws `ServiceErrors`, and `PlatformErrorHandler` writes the response.

### Application

The service is `public`, not `final`, and stateless apart from its collaborators. `@Transactional` is on the public method. A query is `@Transactional(readOnly = true)`.

### Domain

```java
public final class Lead {
    private final String leadId;

    public Lead(String leadId, /* other required facts */) {
        this.leadId = Objects.requireNonNull(leadId);
    }

    public void assign(String principalId, Instant at) {
        // illegal transitions fail here, through ServiceErrors or a domain exception
        // that the application service maps to ServiceErrors
    }
}
```

### Adapter

JPA entities are package-visible when the repository lives in the same package. They have a no-arg constructor because Hibernate needs one, and they are not passed to a controller. A `toString` on a JPA entity prints the id only.

Outbound HTTP uses the injected `RestClient`, `https` outside local profiles, and timeouts from configuration. Certificate checks stay on.

### Mapping

```java
public record LeadResponse(String leadId, String state) {
    public static LeadResponse from(Lead lead) {
        return new LeadResponse(lead.leadId(), lead.state().name());
    }
}
```

Rejected: `BeanUtils.copyProperties`, a JSON round-trip to copy a bean, a `*Dtos` holder, MapStruct added for one mapper.

## 11. REST API

### 11.1 Paths

| Surface | Prefix | Example |
| --- | --- | --- |
| Workforce client, through the BFF | `/api/v1/{resources}` | `/api/v1/leads` |
| Service to service | `/internal/v1/{resources}` | `/internal/v1/leads` |

Resources are plural nouns. A verb in the path is rejected (`/createLead`). A transition that is not CRUD is a sub-resource (`POST /internal/v1/leads/{leadId}/assignments`).

### 11.2 Methods and status

| Method | Meaning | Success |
| --- | --- | --- |
| `GET` | Read. No write. | 200 |
| `POST` collection | Create | 201 and the representation |
| `POST` sub-resource | A command that is not CRUD | 200 or 201 |
| `PUT` | Replace | 200 |
| `PATCH` | Partial update | 200 |
| `DELETE` | Remove | 204 |

A failure uses the catalogue status. It is never 200 with an error flag.

### 11.3 Representations

JSON only. Fields are camelCase. Bean Validation annotations sit on the request record. The body carries the resource id. This platform does not use Spring HATEOAS and does not require a `self` link. Do not add one to match a generic style guide.

`RequestDiagnosticFilter` sets `Cache-Control: no-store` when the handler did not set a cache header. Authenticated and internal responses stay `no-store`. Do not cache a lead, a quote, a token, or an error.

### 11.4 Idempotency

Every new mutating `/v1/**` or `/internal/v1/**` endpoint accepts `Idempotency-Key`. A replay returns the original result. A reused key with a different body is a conflict (`ErrorCodes` conflict), not a second write. Tests cover a missing key, a replay, and a conflict.

### 11.5 Pagination

A list endpoint is paged, or it is known to be a small bounded set (the caller's own open leads). Offset queries use `page` and `size`. Do not `findAll` on a request path. Do not add an unbounded `GET` that loads a table.

### 11.6 Versioning

The path version is `v1`. A breaking change to a public contract needs an ADR or a new version. Internal callers change together with the contract in the same pull request.

### 11.7 Documentation

A public HTTP contract is updated in OpenAPI in the same change. Internal endpoints still have a controller test that asserts the status and the error code.

## 12. Validation and errors

Input is validated twice. Bean Validation rejects a blank required field at the controller. The aggregate rejects a state the annotations cannot see.

Errors:

- Throw `errors.error(ErrorCodes.SOME_CODE).component(...).operation(...).reason(...).build()`.
- `reason` is for logs and diagnostics. It is not the public `detail`. The catalogue owns public wording.
- Do not catch and return `ResponseEntity` with a hand-built problem body.
- Do not throw `IllegalStateException` for a known upstream failure. Translate it to `UPSTREAM_*` or the domain code.
- Field errors use `ErrorCodes.MISSING_REQUIRED_FIELD` or the catalogue code for that field.
- Client-caused categories log at WARN with no stack. Platform and upstream failures log at ERROR with a stack. `Slf4jErrorRecorder` already does this. Do not add `log.error("something went wrong", ex)` beside it.

The response shape is `application/problem+json` as defined in `docs/platform/api-reference/conventions/error-model.md`. Agents do not invent a second envelope.

## 13. Persistence and transactions

- Flyway owns the schema. `ddl-auto: validate`. `open-in-view: false`.
- `@Transactional` is on the public application-service method. The transaction does not wrap an outbound HTTP call. Publish after commit, or write an outbox row in the same transaction.
- The aggregate and the JPA entity are different types. The adapter maps between them.
- A request path is one query, or it is paged. A new filter column gets an index in the same migration.
- H2 with `MODE=PostgreSQL` may back a slice test that does not assert dialect-specific SQL. Migration tests, JSON operators, and locking tests run on PostgreSQL (Testcontainers or `*PostgresIT`). Do not point a production or UAT profile at H2.
- `1sb-integration-service` reaches the store over HTTP. It does not gain a `DataSource`.

## 14. Time, money, and identifiers

- Time comes from an injected `Clock` or from the database column. Do not call `Instant.now()` inside a domain rule that a test must control. `Instant.now()` at the edge of a filter or an error timestamp is acceptable.
- Money the platform calculates is `BigDecimal`. Scale and rounding are explicit (`HALF_EVEN`). Comparison uses `compareTo`. No `double` or `float` for money. An insurer amount that arrives as a string stays a string until this platform calculates with it.
- Identifiers that the bank mints are ULIDs or UUIDs, carried as a value object or a `String` that the aggregate checks. Do not use a database identity column as a business id when the existing table uses a bank-minted id.

## 15. Calls to other services

- Port in `application` or `domain`. Adapter in `adapter/<system>`.
- `RestClient` with connect and read timeouts from `@ConfigurationProperties`.
- Translate non-2xx responses to catalogue codes. Log the status class, the operation, and the code. Do not log the URI, the query, the token, or the body.
- A timeout is an upstream failure, not a generic 500.

## 16. Configuration

A group of settings is a `@ConfigurationProperties` type, preferably a record, with validation. `@Value` is allowed on a constructor only for a single secret or key that is already injected from the environment (`lead.crypto.field-key`). Do not add a new `@Value` for ordinary settings. Do not put a default password in a record or in `application.yml` committed for a shared environment. Local placeholders are obvious fakes and are documented.

Profile names in this repository: `local`, `test`, `dev`, `uat`, `prod`. Do not invent `development`.

## 17. Security and compliance

- Default deny. A new route is authenticated unless it is health, info, or a documented public login step.
- The workforce client never receives an IdP access token or a refresh token. The BFF holds them.
- Do not trust a role supplied only by the caller. Authorization uses the platform principal.
- Secrets come from the environment or `SecretProvider`. They are not in source, tests, fixtures, or logs.
- No PAN, Aadhaar, phone, email, or health attribute in a log, an exception message, or a metric tag.
- Parameterised SQL only. No string-concatenated SQL.
- A new trust boundary or a new crypto choice needs a security review before merge (`SEC-C10`).
- Security-relevant outcomes use catalogue codes (`SEC-C8`).
- Dependencies with a reachable CRITICAL or HIGH CVE that has a fix block merge.

## 18. Logging and observability

- `RequestDiagnosticFilter` seeds `correlationId` from `X-Correlation-Id` and echoes it. New code on the request path does not log before that filter and does not swallow exceptions before `PlatformErrorHandler`.
- `correlationId` stays. Do not remove it in favor of a trace id. Micrometer's trace id may also be present. It does not replace the correlation id the error contract returns to the caller.
- Non-error operational events are DEBUG and never print a token.
- `bank.error.count` is the failure metric. Do not add a second counter for the same failure without an SRE reason.
- Tests for new failure paths assert the error code and assert that the message does not contain a secret or `q`.

## 19. Testing

| Change | Test |
| --- | --- |
| Domain rule | Unit test on the aggregate. No Spring. |
| Application service branch | Unit test with a fake port. |
| Controller status and error code | `@WebMvcTest` or the module's existing slice test. |
| Outbound HTTP | WireMock. Assert zero calls when validation fails. |
| Flyway or dialect-specific SQL | Postgres IT. |
| Architecture boundary | The module's ArchUnit test still passes, and a new boundary gets a rule that fails when broken. |
| Security, token, or error branch | The new branch is executed, not only compiled. |

Rules `R1`–`R10` in `TESTING-RULES.md` still apply. Coverage floors:

| Module | Line | Branch |
| --- | --- | --- |
| `libs/*` | ≥ 80% | ≥ 70% |
| `1sb-integration-service`, `bank-persistence-service` | ≥ 90% | ≥ 70% |
| Other services | ≥ 50% line | publish the measured branch % |

A new library module starts at 80/70. Do not lower a floor. A production type with logic and no test is a must-fix.

Members expanded from Lombok (`equals`, `hashCode`, `toString`, accessors, builder setters, and the all-args constructor) carry `@MechanicallyGenerated`. The simple name contains `Generated` and retention is `CLASS`, so JaCoCo excludes those members the way it excluded `@lombok.Generated`. A hand-written method, including a builder's `build`, stays unmarked and stays in the floor. New code does not add the annotation.

Test names follow section 7. Fixtures are synthetic. No real PII. No sleep over 100ms without a fake clock.

## 20. Language and formatting

- Java 21. Records, switch expressions, text blocks, and pattern matching are welcome.
- No raw types. No wildcard imports.
- Comments explain a business or regulatory constraint. They do not narrate the next line.
- Encoding is UTF-8. Line endings are LF. Files end with a newline.
- The only formatter is Spotless with `googleJavaFormat("1.22.0")` on every `src/*/java/**/*.java` file. Google Java Format uses two-space indents. Palantir and a second formatter are not added. `spotlessCheck` does not use a ratchet: a file that is not formatted fails the build, whether or not this change touched it.
- Checkstyle is the defect and naming set in `config/checkstyle/checkstyle.xml` (`maxWarnings = 0`): star imports, `==` on strings, `equals` without `hashCode`, empty blocks, and the Naming modules that enforce section 7 (`PackageName`, `TypeName`, `MethodName`, `ConstantName`, member/parameter/local names). Formatting belongs to Spotless. Pre-existing MechanicallyGenerated `$` identifiers and Flyway `V{n}__*` types are listed in `config/checkstyle/suppressions.xml` (EPIC-007 / QA-015) — new code does not add suppressions without a work-item id.

JDK first:

| Need | What we write |
| --- | --- |
| Null check | `Objects.requireNonNull` |
| Blank text | `String.isBlank` |
| A fixed collection | `List.of`, `Set.of`, `Map.of` |
| A stream to a list | `Stream.toList` |
| A value that may be absent | `Optional` from `find` |
| A closed set | an enum or a record, not `Map<String, Object>` |

## 21. Dependencies

Approved for production code: the Spring Boot BOM and the libraries already on a module's `build.gradle.kts`, plus the platform `libs/*` modules.

Banned as a direct import. Checkstyle rejects them. A copy that the Boot BOM pulls in transitively (today `commons-lang3`) is not a licence to call it. Our code uses the JDK.

| Dependency | Why |
| --- | --- |
| `org.projectlombok:lombok` | Hides constructors and builders. Write them out. Expanded members already in the tree keep `@MechanicallyGenerated` and do not bring the dependency back. |
| `org.apache.commons:commons-lang3` | The JDK covers it. |
| `com.google.guava:guava` | The JDK covers it. |
| `org.modelmapper:modelmapper` | Mapping is explicit. |
| `commons-beanutils:commons-beanutils` | `BeanUtils` skips invariants. |

A library outside the approved set is named in the change description. The agent does not add it silently. Versions come from the BOM or a lockfile. No floating versions.

## 22. Agent workflow

Before writing code:

1. Read section 2 and section 5. If the change adds an endpoint, read section 11. Read section 17 before authentication or secrets, and section 18 before a log line.
2. Find the service module. Do not create a parallel tree.
3. List the files you will add, each with its layer.
4. Reuse `ServiceErrors`, `ErrorCodes`, and `RequestDiagnosticFilter`. Do not create a second error body or a second correlation filter.
5. Stay on the Boot 3.5 column of section 3.

While writing code:

- Match the shapes in section 10. Same constructor injection, same mapping split, same catalogue errors.
- A service stays concrete and stateless. Do not add Lombok.
- Format with `./gradlew spotlessApply` on the modules you touched. The check is repo-wide.
- A new condition on an existing rule list is a new rule and a test.
- Do not put `@Entity` on a domain type.
- Do not reformat unrelated modules by hand, and do not upgrade Spring Boot inside a feature change.
- Do not leave a TODO in place of the required test, the migration, or the error mapping. A TODO that remains carries a work-item id.

When you finish, the change summary names the module, each file and its layer, the tests, the migration id or "no schema change", and any new dependency with the section 21 reason.

If a rule cannot be followed, say which rule and why. Do not quietly start a second style.

## 23. Review checklist

- [ ] The change lives in the owning service, or in `libs` for a technical type a second service already needs.
- [ ] No new `dto`, `entity`, `repo`, `util`, or `helper` package.
- [ ] Domain files import none of Spring, JPA, Jackson, or Servlet.
- [ ] The controller depends on an application service only.
- [ ] New types match an accepted snippet in section 8 or 10. No one-implementation service interface, no `Utils`, no base class.
- [ ] JPA entities stay in `adapter.persistence` and are not the aggregate.
- [ ] `@Transactional` is on a public application-service method. Queries are read-only.
- [ ] Settings are `@ConfigurationProperties`, except a single existing secret bound with `@Value`.
- [ ] No HTTP call inside an open database transaction without an outbox.
- [ ] Errors go through `ServiceErrors` and the catalogue. No failure is HTTP 200.
- [ ] Paths, methods, and statuses match section 11. Mutating routes accept `Idempotency-Key`.
- [ ] A schema change is a new Flyway migration, and `ddl-auto` remains `validate`.
- [ ] Names match section 7. No `Impl`, no `I` prefix, no `DTO`, no `Dtos`. Checkstyle Naming enforces §7 on `./gradlew checkstyleMain` (EPIC-007 / QA-015).
- [ ] Constructor injection. No Lombok. No new `@Autowired` field.
- [ ] Null and collection code uses the JDK. No BeanUtils, Guava, or Commons Lang.
- [ ] Money the platform calculates is `BigDecimal`. Time in a domain rule comes from `Clock`.
- [ ] The required test from section 19 exists.
- [ ] Logs and exceptions carry no PAN, Aadhaar, phone, email, token, or `q`.
- [ ] `./gradlew spotlessCheck` passes with Google Java Format on the whole module.
- [ ] Coverage for the touched module meets section 19 and the [maturity ladder](../1sb-insurance-integration/service-ssot/COVERAGE.md#module-maturity-ladder-epic-007--qa-015) (scaffold / implementing / Phase-1 hardened).
- [ ] The module's ArchUnit test uses the shared `bank-common-architecture` baseline (EPIC-007 / ARCH-029); 1SB-specific rules stay in `1sb-integration-service`.
