package com.bank.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** ERR-003 — reading a peer's envelope is what gives propagation something to preserve. */
class ProblemJsonReaderTest {

    private final ProblemJsonReader reader = new ProblemJsonReader(new ObjectMapper());

    @Test
    void aPeersEnvelopeSurvivesTheHopIntact() {
        String body = """
            {"type":"about:blank","title":"Suitability assessment required","status":403,
             "detail":"A completed suitability assessment is needed before this quote can be produced.",
             "code":"SUITABILITY_REQUIRED","category":"COMPLIANCE_GATE","retryable":false,
             "incidentId":"01JQ8F3K2M7Z9V4T0000000000","correlationId":"corr-77","service":"authz",
             "origin":{"service":"journey-orchestration","code":"SUITABILITY_REQUIRED","layer":"L6"}}
            """;

        ServiceErrorResponse parsed = reader.read(body, 403);

        assertThat(parsed.getCode()).isEqualTo(ErrorCodes.SUITABILITY_REQUIRED);
        assertThat(parsed.getStatus()).isEqualTo(403);
        assertThat(parsed.getCategory()).isEqualTo(ErrorCategory.COMPLIANCE_GATE);
        assertThat(parsed.getIncidentId()).isEqualTo("01JQ8F3K2M7Z9V4T0000000000");
        assertThat(parsed.getCorrelationId()).isEqualTo("corr-77");
        assertThat(parsed.getOrigin().service()).isEqualTo("journey-orchestration");
        assertThat(parsed.getOrigin().layer()).isEqualTo(PlatformLayer.L6);
    }

    @Test
    void theWholeChainHoldsFromWireToRethrow() {
        // The scenario the requirement names: the orchestrator refuses, the BFF answers.
        ServiceErrorResponse fromOrchestrator = ServiceException.of(ErrorCodes.SUITABILITY_REQUIRED)
            .service("journey-orchestration").layer(PlatformLayer.L6)
            .reason("assessment a3f2 expired")
            .build()
            .getErrorResponse();

        String onTheWire = """
            {"code":"%s","status":403,"title":"t","detail":"d","incidentId":"%s",
             "origin":{"service":"journey-orchestration","code":"%s","layer":"L6"}}
            """.formatted(fromOrchestrator.getCode(), fromOrchestrator.getIncidentId(),
                          fromOrchestrator.getCode());

        ServiceException atTheBff = ErrorPropagation.from(reader.read(onTheWire, 403))
            .receivedBy("bff", PlatformLayer.L4)
            .calling("journey-orchestration", "createQuote")
            .toException();

        assertThat(atTheBff.getIncidentId()).isEqualTo(fromOrchestrator.getIncidentId());
        assertThat(atTheBff.getErrorResponse().getCode()).isEqualTo(ErrorCodes.SUITABILITY_REQUIRED);
        assertThat(atTheBff.getDiagnostic().effectiveOriginService()).isEqualTo("journey-orchestration");
    }

    @Test
    void anUnreadableBodyDoesNotBecomeAnErrorOfItsOwn() {
        for (String body : new String[] {null, "", "   ", "<html>502 Bad Gateway</html>", "{\"x\":1}"}) {
            ServiceErrorResponse parsed = reader.read(body, 502);

            assertThat(parsed.getCode())
                .as("body: %s", body)
                .isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE);
            assertThat(parsed.getStatus()).isEqualTo(502);
        }
    }

    @Test
    void unknownEnumValuesAreIgnoredRatherThanFatal() {
        String body = """
            {"code":"CONFLICT","status":409,"category":"NOT_A_CATEGORY",
             "origin":{"service":"peer","code":"CONFLICT","layer":"L99"}}
            """;

        ServiceErrorResponse parsed = reader.read(body, 409);

        assertThat(parsed.getCode()).isEqualTo(ErrorCodes.CONFLICT);
        assertThat(parsed.getCategory())
            .as("a category this build does not know is not worth failing a propagation over")
            .isNull();
        assertThat(parsed.getOrigin().service()).isEqualTo("peer");
        assertThat(parsed.getOrigin().layer()).isNull();
    }

    @Test
    void anOriginMissingItsPartsIsDroppedNotHalfBuilt() {
        ServiceErrorResponse parsed = reader.read(
            "{\"code\":\"CONFLICT\",\"status\":409,\"origin\":{\"service\":\"peer\"}}", 409);

        assertThat(parsed.getOrigin()).isNull();
    }
}
