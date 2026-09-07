# 08 — Java record sketches · NIP BFF lead phase

**Companion to** [`07-nip-bff-lead-phase-api-lld.md`](./07-nip-bff-lead-phase-api-lld.md)
and [`nip-bff-lead-phase.openapi.yaml`](./nip-bff-lead-phase.openapi.yaml).

These types are **sketches for S11**. They are not compiled in this change. When NIP BFF is
scaffolded they belong in `com.bank.insurance.nip.bff.api.v1.lead` and should be generated
from the OpenAPI (`AP-5`), not hand-copied. Jackson + Java 21 records.

Internal service DTOs (Lead #5, Customer #4) are **not** the same types. The BFF maps
internal → public and applies masking. Flutter never depends on internal packages.

Error envelope is already implemented:
`com.bank.common.error.ServiceErrorResponse` (`ADR-017`). Do not fork it.

```java
package com.bank.insurance.nip.bff.api.v1.lead;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class LeadPhaseApi {

    private LeadPhaseApi() {}

    public enum InboxFilter { WORKING, UNSTARTED }

    public enum SearchBy { CUSTOMER_ID, MOBILE, PAN, NAME }

    public enum Lob { LIFE }

    public enum ProductClass { TERM }

    public enum LeadState {
        NEW, ASSIGNED, CONTACTED, QUALIFIED, CONVERTED, DISQUALIFIED, EXPIRED, ARCHIVED
    }

    public enum Eligibility { ETB, NOT_ETB }

    public enum NeedAnalysisState { NOT_STARTED, IN_PROGRESS, COMPLETED }

    public enum CreateOutcome { CREATED, RESUMED }

    /** Opaque ULID; not a CIF and not a sequential display id (ID-01). */
    public record Ulid(
            @NotBlank @Pattern(regexp = "^[0-9A-HJKMNP-TV-Z]{26}$") String value) {}

    public record CursorPage(
            int size,
            String nextCursor,
            boolean hasMore) {}

    public record OffsetPage(
            int page,
            int size,
            boolean hasMore) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PipelineRow(
            @NotNull Ulid leadId,
            @NotBlank @Size(max = 140) String customerDisplayName,
            @NotBlank @Size(max = 4) String initials,
            @NotBlank String maskedMobile,
            @NotNull LeadState state,
            @NotNull ProductClass productClass,
            Ulid journeyId,
            String journeyStage,
            @NotNull Instant updatedAt) {}

    public record PipelinePage(
            @NotNull List<PipelineRow> items,
            @NotNull CursorPage page) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CustomerSummary(
            @NotNull Ulid customerId,
            @NotBlank @Size(max = 140) String fullName,
            @NotBlank @Size(max = 4) String initials,
            @NotBlank String maskedMobile,
            String maskedEmail,
            @NotNull Eligibility eligibility) {}

    public record SearchQueryMeta(
            @NotNull SearchBy by,
            @Min(0) int resultCount) {}

    public record SearchPage(
            @NotNull SearchQueryMeta query,
            @NotNull List<CustomerSummary> items,
            @NotNull OffsetPage page) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ActiveLead(
            @NotNull Ulid leadId,
            @NotNull ProductClass productClass,
            @NotNull LeadState state,
            Ulid journeyId,
            @NotNull Instant createdAt) {}

    public record ActiveLeadPage(
            @NotNull @Size(max = 20) List<ActiveLead> items,
            boolean hasMore) {}

    public record ProductClassOption(
            @NotNull ProductClass productClass,
            @NotBlank String label,
            boolean selectable) {}

    public record ProductClassPage(
            @NotNull Lob lob,
            @NotNull List<ProductClassOption> items) {}

    /**
     * New create: customerId + lob + productClass.
     * Resume: resumeLeadId set.
     * {@code distributorId} must not bind — reject unknown properties at the BFF.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateLeadRequest(
            Ulid customerId,
            Lob lob,
            ProductClass productClass,
            Ulid resumeLeadId) {}

    public record LeadCreated(
            @NotNull Ulid leadId,
            @NotNull Ulid journeyId,
            @NotNull Ulid customerId,
            @NotNull Lob lob,
            @NotNull ProductClass productClass,
            @NotNull LeadState state,
            @NotNull Instant createdAt,
            @NotNull CreateOutcome outcome) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LeadSummary(
            @NotNull Ulid leadId,
            @NotNull Ulid journeyId,
            @NotNull Ulid customerId,
            @NotNull Lob lob,
            @NotNull ProductClass productClass,
            @NotNull LeadState state,
            @NotNull Instant createdAt,
            @NotNull CreateOutcome outcome,
            NeedAnalysisState needAnalysisState,
            String journeyStage,
            Instant updatedAt) {}
}
```

## Internal mapping types (BFF-private, not on the Flutter wire)

```java
package com.bank.insurance.nip.bff.internal.lead;

import com.bank.insurance.nip.bff.api.v1.lead.LeadPhaseApi.Eligibility;
import com.bank.insurance.nip.bff.api.v1.lead.LeadPhaseApi.LeadState;
import com.bank.insurance.nip.bff.api.v1.lead.LeadPhaseApi.ProductClass;

import java.time.Instant;

/** Customer #4 lookup row before masking. Never serialised to Flutter. */
public record CustomerLookupInternal(
        String customerId,
        String cifNumber,
        String fullName,
        String mobileE164,
        String email,
        boolean etb,
        boolean inBook) {}

public record LeadListInternal(
        String leadId,
        String customerId,
        String customerDisplayName,
        String mobileE164,
        LeadState state,
        ProductClass productClass,
        String journeyId,
        String journeyStage,
        Instant updatedAt) {}

public final class LeadPhaseMasking {

    private LeadPhaseMasking() {}

    public static String initials(String fullName) { /* implementation at S11 */ return ""; }

    public static String maskMobile(String e164) { /* +91 933****412 */ return ""; }

    public static String maskEmail(String email) { /* abh*****@gmail.com */ return ""; }

    public static Eligibility eligibility(boolean etb) {
        return etb ? Eligibility.ETB : Eligibility.NOT_ETB;
    }
}
```

## Controller method signatures (sketch)

```java
@RestController
@RequestMapping("/api/v1")
class LeadPhaseController {

    PipelinePage getPipeline(
            @RequestParam(defaultValue = "WORKING") InboxFilter inbox,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            @RequestParam(required = false) String cursor);

    SearchPage searchCustomers(
            @RequestParam SearchBy by,
            @RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "0") @Min(0) @Max(4) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(20) int limit);

    CustomerSummary getCustomer(@PathVariable String customerId);

    ActiveLeadPage activeLeads(
            @PathVariable String customerId,
            @RequestParam ProductClass productClass);

    ProductClassPage productClasses(@RequestParam Lob lob);

    ResponseEntity<LeadCreated> createOrResume(
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody @Valid CreateLeadRequest body);

    LeadSummary getLead(@PathVariable String leadId);
}
```

Query beans for pipeline / search should reject unknown `productClass` values with
`422 UNSUPPORTED_LOB` rather than Jackson’s default 400, so the RM copy can name Term as
the R0 class.
