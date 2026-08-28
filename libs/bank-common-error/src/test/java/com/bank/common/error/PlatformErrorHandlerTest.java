package com.bank.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/** ERR-002 — the shared handler: identity, boundary behaviour, and the order of the two steps. */
class PlatformErrorHandlerTest {

    private static final class PublicHandler extends PlatformErrorHandler {
        PublicHandler() { super("bff", PlatformLayer.L4, Boundary.PUBLIC); }
    }

    private static final class InternalHandler extends PlatformErrorHandler {
        InternalHandler() { super("onesb", PlatformLayer.L5, Boundary.INTERNAL); }
        @Override protected int validationStatus() { return 422; }
    }

    private static final class DefaultingHandler extends PlatformErrorHandler {
        DefaultingHandler() { super("newcomer", PlatformLayer.L5); }
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
        ResponseEntity<ServiceErrorResponse> response = new PublicHandler().handleServiceException(refusal());
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
        ServiceErrorResponse body = new InternalHandler().handleServiceException(refusal()).getBody();

        assertThat(body).isNotNull();
        assertThat(body.carriesDiagnostics())
            .as("below L4 the calling service needs the origin to attribute the failure")
            .isTrue();
        assertThat(body.getOrigin().service()).isEqualTo("journey-orchestration");
        assertThat(body.getDiagnostic().getReason()).contains("expired");
    }

    @Test
    void aHandlerThatDeclaresNoBoundaryRedacts() {
        ServiceErrorResponse body = new DefaultingHandler().handleServiceException(refusal()).getBody();

        assertThat(body).isNotNull();
        assertThat(body.carriesDiagnostics())
            .as("a service that forgets to declare its position must leak nothing")
            .isFalse();
    }

    @Test
    void theRespondingServiceIsStampedWithoutOverwritingTheOriginatingOne() {
        ServiceErrorResponse internal = new InternalHandler().handleServiceException(refusal()).getBody();

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

        ServiceErrorResponse body = new InternalHandler().handleServiceException(plain).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getService()).isEqualTo("onesb");
        assertThat(body.getIncidentId())
            .as("every response carries something support can search on")
            .isNotNull();
    }

    @Test
    void redactionHappensAfterTheDiagnosticIsRecorded() {
        var recorded = new java.util.ArrayList<ServiceErrorResponse>();

        PlatformErrorHandler handler = new PlatformErrorHandler("bff", PlatformLayer.L4, PlatformErrorHandler.Boundary.PUBLIC) {
            @Override
            protected void record(ServiceErrorResponse body, Throwable cause) {
                recorded.add(body);
            }
        };

        ServiceErrorResponse published = handler.handleServiceException(refusal()).getBody();

        assertThat(recorded).hasSize(1);
        assertThat(recorded.getFirst().carriesDiagnostics())
            .as("redacting before logging would destroy the evidence redaction exists to protect")
            .isTrue();
        assertThat(recorded.getFirst().getDiagnostic().getReason()).contains("expired");

        assertThat(published).isNotNull();
        assertThat(published.carriesDiagnostics()).isFalse();
        assertThat(published.getIncidentId())
            .as("the logged half and the sent half must share the id that joins them")
            .isEqualTo(recorded.getFirst().getIncidentId());
    }

    @Test
    void servicesMayDeclareDifferentValidationStatuses() {
        assertThat(new InternalHandler().validationStatus()).isEqualTo(422);
        assertThat(new DefaultingHandler().validationStatus()).isEqualTo(422);

        PlatformErrorHandler persistenceLike = new PlatformErrorHandler("persistence", PlatformLayer.L7, PlatformErrorHandler.Boundary.INTERNAL) {
            @Override protected int validationStatus() { return 400; }
        };
        assertThat(persistenceLike.validationStatus())
            .as("400 here is a published contract, not a default to be unified away")
            .isEqualTo(400);
        assertThat(persistenceLike.malformedBodyStatus()).isEqualTo(400);
    }

    @Test
    void handlerExposesItsOwnConfiguration() {
        InternalHandler h = new InternalHandler();
        assertThat(h.serviceId()).isEqualTo("onesb");
        assertThat(h.layer()).isEqualTo(PlatformLayer.L5);
        assertThat(h.boundary()).isEqualTo(PlatformErrorHandler.Boundary.INTERNAL);
    }
}
