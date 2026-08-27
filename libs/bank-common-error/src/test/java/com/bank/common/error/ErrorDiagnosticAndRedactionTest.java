package com.bank.common.error;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ERR-001 — the diagnostic half, and the redaction the L4 boundary performs on it.
 *
 * <p>The scenario throughout is the one the requirement names: the request reaches the BFF, moves
 * on, and the orchestrator refuses on a compliance gate.
 */
class ErrorDiagnosticAndRedactionTest {

    private ServiceException orchestratorRefusal() {
        return ServiceException.of(ErrorCodes.SUITABILITY_REQUIRED)
            .service("journey-orchestration")
            .layer(PlatformLayer.L6)
            .component("QuoteService")
            .operation("createQuote")
            .correlationId("corr-77")
            .reason("assessment a3f2 expired at 2026-08-27T08:10Z, quote requested 09:41Z")
            .remediation("RM must refresh the suitability assessment, then re-quote. Do not override.")
            .build();
    }

    @Test
    void catalogueDrivenExceptionCarriesBothHalves() {
        ServiceException ex = orchestratorRefusal();
        ServiceErrorResponse response = ex.getErrorResponse();
        ErrorDiagnostic diagnostic = ex.getDiagnostic();

        // Public half — from the registry, identical wherever it is thrown.
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getCode()).isEqualTo(ErrorCodes.SUITABILITY_REQUIRED);
        assertThat(response.getCategory()).isEqualTo(ErrorCategory.COMPLIANCE_GATE);
        assertThat(response.getTitle()).isEqualTo("Suitability assessment required");
        assertThat(response.isRetryable()).isFalse();
        assertThat(response.getCorrelationId()).isEqualTo("corr-77");

        // Engineer half — answers where, why, how and what to do.
        assertThat(diagnostic.getService()).isEqualTo("journey-orchestration");
        assertThat(diagnostic.getLayer()).isEqualTo(PlatformLayer.L6);
        assertThat(diagnostic.getComponent()).isEqualTo("QuoteService");
        assertThat(diagnostic.getOperation()).isEqualTo("createQuote");
        assertThat(diagnostic.getReason()).contains("expired");
        assertThat(diagnostic.getRemediation()).contains("re-quote");
        assertThat(diagnostic.getRunbook()).isEqualTo("RB-SUITABILITY_REQUIRED");

        // One incident, both halves.
        assertThat(response.getIncidentId()).isEqualTo(diagnostic.getIncidentId());
        assertThat(IncidentId.isValid(response.getIncidentId())).isTrue();
    }

    @Test
    void theBoundaryStripsDiagnosticsButKeepsTheIncidentId() {
        ServiceErrorResponse internal = ServiceException.of(ErrorCodes.SUITABILITY_REQUIRED)
            .service("journey-orchestration")
            .layer(PlatformLayer.L6)
            .origin(ErrorOrigin.of("journey-orchestration", ErrorCodes.SUITABILITY_REQUIRED, PlatformLayer.L6))
            .reason("assessment a3f2 expired")
            .build()
            .getErrorResponse();

        assertThat(internal.carriesDiagnostics()).isTrue();

        ServiceErrorResponse published = internal.toPublic();

        assertThat(published.carriesDiagnostics())
            .as("nothing crossing L4 may carry a diagnostic")
            .isFalse();
        assertThat(published.getOrigin()).isNull();
        assertThat(published.getDiagnostic()).isNull();

        assertThat(published.getIncidentId())
            .as("the incident id is what lets support find the diagnostic that was not sent")
            .isEqualTo(internal.getIncidentId());
        assertThat(published.getCode()).isEqualTo(internal.getCode());
        assertThat(published.getStatus()).isEqualTo(internal.getStatus());
    }

    @Test
    void redactionReplacesUnsafeWordingWithCatalogueText() {
        // This is defect D1/D2 as it exists today: upstream text and an internal route in `detail`.
        ServiceErrorResponse leaky = ServiceErrorResponse.builder()
            .title("Upstream Unavailable")
            .status(503)
            .detail("1SB call failed: GET /v1/quote/status — connection reset")
            .code(ErrorCodes.UPSTREAM_UNAVAILABLE)
            .build();

        ServiceErrorResponse published = leaky.toPublic();

        assertThat(published.getDetail())
            .doesNotContain("1SB")
            .doesNotContain("/v1/quote/status")
            .isEqualTo(ErrorCatalogue.require(ErrorCodes.UPSTREAM_UNAVAILABLE).publicDetail());
        assertThat(published.getTitle()).isEqualTo("Service temporarily unavailable");
    }

    @Test
    void redactionLeavesUnregisteredCodesAlone() {
        ServiceErrorResponse odd = ServiceErrorResponse.builder()
            .title("Custom").status(418).detail("teapot").code("NOT_REGISTERED").build();

        ServiceErrorResponse published = odd.toPublic();

        assertThat(published.getTitle()).isEqualTo("Custom");
        assertThat(published.getDetail()).isEqualTo("teapot");
        assertThat(published.carriesDiagnostics()).isFalse();
    }

    @Test
    void aPropagatedFailureNamesTheServiceThatActuallyFailed() {
        ErrorOrigin origin = ErrorOrigin.of("journey-orchestration", ErrorCodes.SUITABILITY_REQUIRED, PlatformLayer.L6);

        ErrorDiagnostic atTheBff = ErrorDiagnostic.builder(ErrorCodes.UPSTREAM_BUSINESS_ERROR)
            .service("bff")
            .layer(PlatformLayer.L4)
            .origin(origin)
            .build();

        assertThat(atTheBff.isPropagated()).isTrue();
        assertThat(atTheBff.effectiveOriginService())
            .as("the originService metric tag is what ends 'is it us or them'")
            .isEqualTo("journey-orchestration");

        ErrorDiagnostic local = ErrorDiagnostic.builder(ErrorCodes.INTERNAL_ERROR).service("bff").build();
        assertThat(local.isPropagated()).isFalse();
        assertThat(local.effectiveOriginService()).isEqualTo("bff");
    }

    @Test
    void causeChainIsRecordedOutermostFirstAndBounded() {
        Throwable deep = new IllegalStateException("innermost");
        for (int i = 0; i < 20; i++) {
            deep = new RuntimeException("wrapper " + i, deep);
        }

        ErrorDiagnostic d = ErrorDiagnostic.builder(ErrorCodes.INTERNAL_ERROR).cause(deep).build();

        assertThat(d.getCauseChain()).hasSize(8);
        assertThat(d.getCauseChain().getFirst()).isEqualTo("RuntimeException: wrapper 19");
        assertThatThrownBy(() -> d.getCauseChain().add("x"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void causeChainToleratesASelfReferencingCause() {
        Throwable loop = new RuntimeException("self") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };
        assertThat(ErrorDiagnostic.builder(ErrorCodes.INTERNAL_ERROR).cause(loop).build().getCauseChain())
            .hasSize(1);
    }

    @Test
    void exceptionBuilderCarriesUpstreamDetailAndCause() {
        Exception boom = new IllegalStateException("connection reset");
        ServiceException ex = ServiceException.of(ErrorCodes.UPSTREAM_UNAVAILABLE)
            .service("onesb")
            .layer(PlatformLayer.L5)
            .upstream("1SB", "GATEWAY_TIMEOUT", 504)
            .errors(List.of(ServiceError.of(ErrorCodes.UPSTREAM_TIMEOUT, "poll budget exhausted")))
            .cause(boom)
            .build();

        assertThat(ex).hasCause(boom);
        assertThat(ex.getHttpStatus()).isEqualTo(503);
        assertThat(ex.isRetryable()).isTrue();
        assertThat(ex.getIncidentId()).isNotNull();
        assertThat(ex.getDiagnostic().getUpstreamSystem()).isEqualTo("1SB");
        assertThat(ex.getDiagnostic().getUpstreamCode()).isEqualTo("GATEWAY_TIMEOUT");
        assertThat(ex.getDiagnostic().getUpstreamStatus()).isEqualTo(504);
        assertThat(ex.getDiagnostic().getCauseChain()).contains("IllegalStateException: connection reset");
        assertThat(ex.getErrorResponse().getErrors()).hasSize(1);

        assertThat(ex.getErrorResponse().getDetail())
            .as("the upstream's own words never reach the caller")
            .doesNotContain("connection reset");
    }

    @Test
    void exceptionBuilderRejectsAnUnregisteredCode() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ServiceException.of("NOPE"))
            .withMessageContaining("Unregistered error code");
    }

    @Test
    void explicitIncidentIdIsAdoptedSoOneFailureKeepsOneId() {
        String upstreamIncident = IncidentId.generate();

        ServiceException ex = ServiceException.of(ErrorCodes.UPSTREAM_BUSINESS_ERROR)
            .service("bff")
            .incidentId(upstreamIncident)
            .build();

        assertThat(ex.getIncidentId()).isEqualTo(upstreamIncident);
        assertThat(ex.getErrorResponse().getIncidentId()).isEqualTo(upstreamIncident);
    }

    @Test
    void attachingADiagnosticAdoptsItsIdentityFields() {
        ErrorDiagnostic d = ErrorDiagnostic.builder(ErrorCodes.CONFLICT)
            .service("persistence")
            .origin(ErrorOrigin.of("persistence", ErrorCodes.CONFLICT, PlatformLayer.L7))
            .build();

        ServiceErrorResponse r = ServiceErrorResponse.builder()
            .title("Conflict").status(409).code(ErrorCodes.CONFLICT)
            .diagnostic(d)
            .build();

        assertThat(r.getIncidentId()).isEqualTo(d.getIncidentId());
        assertThat(r.getService()).isEqualTo("persistence");
        assertThat(r.getOrigin()).isEqualTo(d.getOrigin());
        assertThat(r.getDiagnostic().toString()).contains("persistence");
    }

    @Test
    void ofBuildsFromTheCatalogueWithAFreshIncidentId() {
        ServiceErrorResponse a = ServiceErrorResponse.of(ErrorCodes.QUOTE_EXPIRED).build();
        ServiceErrorResponse b = ServiceErrorResponse.of(ErrorCodes.QUOTE_EXPIRED).build();

        assertThat(a.getStatus()).isEqualTo(409);
        assertThat(a.getTitle()).isEqualTo("Quote expired");
        assertThat(a.getCategory()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(a.getIncidentId()).isNotEqualTo(b.getIncidentId());
    }
}
