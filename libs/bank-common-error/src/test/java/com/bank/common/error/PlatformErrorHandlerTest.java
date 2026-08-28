package com.bank.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** ERR-002 — the shared handler: identity, boundary behaviour, and the order of the two steps. */
class PlatformErrorHandlerTest {

    private static ErrorHandlingSettings settings(String id, PlatformLayer layer, TrustBoundary boundary) {
        return ErrorHandlingSettings.builder(id).layer(layer).boundary(boundary).build();
    }

    private static PlatformErrorHandler handler(String id, PlatformLayer layer, TrustBoundary boundary) {
        return new PlatformErrorHandler(settings(id, layer, boundary), ErrorRecorder.NONE);
    }

    private static PlatformErrorHandler publicHandler() {
        return handler("bff", PlatformLayer.L4, TrustBoundary.PUBLIC);
    }

    private static PlatformErrorHandler internalHandler() {
        return handler("onesb", PlatformLayer.L5, TrustBoundary.INTERNAL);
    }

    /** A service that configures nothing beyond its id. */
    private static PlatformErrorHandler defaultingHandler() {
        return new PlatformErrorHandler(
            ErrorHandlingSettings.builder("newcomer").build(), ErrorRecorder.NONE);
    }

    private ServiceException refusal() {
        return ServiceException.of(ErrorCodes.SUITABILITY_REQUIRED)
            .service("journey-orchestration")
            .layer(PlatformLayer.L6)
            .origin(ErrorOrigin.of("journey-orchestration", ErrorCodes.SUITABILITY_REQUIRED, PlatformLayer.L6))
            .reason("assessment a3f2 expired at 2026-08-27T08:10Z")
            .build();
    }

    @Test
    void aPublicHandlerStripsTheDiagnosticHalf() {
        ResponseEntity<ServiceErrorResponse> response = publicHandler().handleServiceException(refusal());
        ServiceErrorResponse body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.carriesDiagnostics()).isFalse();
        assertThat(body.getDiagnostic()).isNull();
        assertThat(body.getOrigin()).isNull();
        assertThat(body.getIncidentId()).isNotNull();
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void anInternalHandlerPassesTheDiagnosticToItsCallingService() {
        ServiceErrorResponse body = internalHandler().handleServiceException(refusal()).getBody();

        assertThat(body).isNotNull();
        assertThat(body.carriesDiagnostics())
            .as("below L4 the calling service needs the origin to attribute the failure")
            .isTrue();
        assertThat(body.getOrigin().service()).isEqualTo("journey-orchestration");
        assertThat(body.getDiagnostic().getReason()).contains("expired");
    }

    @Test
    void aHandlerThatDeclaresNoBoundaryRedacts() {
        ServiceErrorResponse body = defaultingHandler().handleServiceException(refusal()).getBody();

        assertThat(body).isNotNull();
        assertThat(body.carriesDiagnostics())
            .as("a service that forgets to declare its position must leak nothing")
            .isFalse();
    }

    @Test
    void theRespondingServiceIsStampedWithoutOverwritingTheOriginatingOne() {
        ServiceErrorResponse internal = internalHandler().handleServiceException(refusal()).getBody();

        assertThat(internal).isNotNull();
        // The response names the service that answered...
        assertThat(internal.getService()).isEqualTo("journey-orchestration");
        // ...and the origin still names where it began.
        assertThat(internal.getDiagnostic().effectiveOriginService()).isEqualTo("journey-orchestration");
    }

    @Test
    void anUnstampedResponseGetsTheHandlersOwnIdentity() {
        ServiceException plain = new ServiceException(
            ServiceErrorResponse.builder()
                .title("Conflict").status(409).code(ErrorCodes.CONFLICT)
                .category(ErrorCategory.CONFLICT)
                .build());

        ServiceErrorResponse body = internalHandler().handleServiceException(plain).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getService()).isEqualTo("onesb");
        assertThat(body.getIncidentId())
            .as("every response carries something support can search on")
            .isNotNull();
    }

    @Test
    void redactionHappensAfterTheDiagnosticIsRecorded() {
        var recorded = new java.util.ArrayList<ServiceErrorResponse>();
        ErrorRecorder capturing = (response, cause) -> recorded.add(response);

        PlatformErrorHandler handler = new PlatformErrorHandler(
            settings("bff", PlatformLayer.L4, TrustBoundary.PUBLIC), capturing);

        ServiceErrorResponse published = handler.handleServiceException(refusal()).getBody();

        assertThat(recorded).hasSize(1);
        assertThat(recorded.getFirst().carriesDiagnostics())
            .as("redacting before recording would destroy the evidence redaction exists to protect")
            .isTrue();
        assertThat(recorded.getFirst().getDiagnostic().getReason()).contains("expired");

        assertThat(published).isNotNull();
        assertThat(published.carriesDiagnostics()).isFalse();
        assertThat(published.getIncidentId())
            .as("the recorded half and the sent half must share the id that joins them")
            .isEqualTo(recorded.getFirst().getIncidentId());
    }

    @Test
    void servicesMayDeclareDifferentValidationStatuses() {
        // 422 platform-wide; persistence publishes 400 on /internal/v1 and says so in configuration
        // rather than by subclassing.
        assertThat(settings("onesb", PlatformLayer.L5, TrustBoundary.INTERNAL).validationStatus())
            .isEqualTo(422);

        ErrorHandlingSettings persistence = ErrorHandlingSettings.builder("persistence")
            .layer(PlatformLayer.L7).boundary(TrustBoundary.INTERNAL).validationStatus(400).build();

        assertThat(persistence.validationStatus()).isEqualTo(400);
        assertThat(persistence.malformedBodyStatus())
            .as("an unparseable body follows the same status unless told otherwise")
            .isEqualTo(400);
    }

    @Test
    void handlerExposesItsOwnConfiguration() {
        PlatformErrorHandler h = internalHandler();
        assertThat(h.settings().serviceId()).isEqualTo("onesb");
        assertThat(h.settings().layer()).isEqualTo(PlatformLayer.L5);
        assertThat(h.settings().boundary()).isEqualTo(TrustBoundary.INTERNAL);
        assertThat(h.settings().redacts()).isFalse();
    }
}
