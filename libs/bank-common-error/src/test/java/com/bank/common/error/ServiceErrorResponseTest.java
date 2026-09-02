package com.bank.common.error;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * The envelope itself: the generated builder's defaults, its accumulating error list, and its
 * refusal to build without the two fields every consumer keys on.
 *
 * <p>The pre-catalogue factories this class used to test ({@code validation},
 * {@code upstreamBusiness}, {@code unauthorized} …) are gone — they had no production call sites
 * and each hand-built an envelope with a literal title and status, which is the defect the
 * catalogue removes. Construction now goes through {@link ServiceErrorResponse#of(String)} or the
 * builder, and both are covered here.
 */
class ServiceErrorResponseTest {

    @Test
    void ofTakesEverythingItCanFromTheCatalogue() {
        ServiceErrorResponse response = ServiceErrorResponse.of(ErrorCodes.QUOTE_EXPIRED).build();

        ErrorDefinition definition = ErrorCatalogue.require(ErrorCodes.QUOTE_EXPIRED);
        assertThat(response.getStatus()).isEqualTo(definition.httpStatus());
        assertThat(response.getTitle()).isEqualTo(definition.publicTitle());
        assertThat(response.getDetail()).isEqualTo(definition.publicDetail());
        assertThat(response.getCategory()).isEqualTo(definition.category());
        assertThat(response.isRetryable()).isEqualTo(definition.retryability().toBoolean());
        assertThat(response.getIncidentId()).isNotNull();
    }

    @Test
    void typeAndTimestampAreDefaultedRatherThanRequired() {
        ServiceErrorResponse response = ServiceErrorResponse.builder()
            .title("Conflict").status(409).code(ErrorCodes.CONFLICT).build();

        assertThat(response.getType()).isEqualTo("about:blank");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void anExplicitTimestampWins() {
        Instant fixed = Instant.parse("2026-07-30T12:00:00Z");

        assertThat(ServiceErrorResponse.builder()
            .title("Conflict").status(409).code(ErrorCodes.CONFLICT).timestamp(fixed)
            .build()
            .getTimestamp()).isEqualTo(fixed);
    }

    @Test
    void fieldErrorsAccumulateSinglyAndInBulk() {
        ServiceErrorResponse response = ServiceErrorResponse.builder()
            .title("Validation failed").status(422).code(ErrorCodes.VALIDATION_ERROR)
            .addError(ServiceError.ofField(ErrorCodes.MISSING_REQUIRED_FIELD, "lob is required", "lob"))
            .errors(List.of(
                ServiceError.ofField(ErrorCodes.MISSING_REQUIRED_FIELD, "productCode is required", "productCode")))
            .build();

        assertThat(response.getErrors())
            .as("a builder that replaced rather than accumulated would silently drop the first error")
            .hasSize(2)
            .extracting(ServiceError::field)
            .containsExactly("lob", "productCode");
    }

    @Test
    void theErrorListIsImmutableOnceBuilt() {
        ServiceErrorResponse response = ServiceErrorResponse.of(ErrorCodes.VALIDATION_ERROR).build();

        assertThatThrownBy(() -> response.getErrors().add(ServiceError.of("CODE", "msg")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void theTwoFieldsEveryConsumerKeysOnAreRequired() {
        assertThatNullPointerException()
            .isThrownBy(() -> ServiceErrorResponse.builder().code(ErrorCodes.INTERNAL_ERROR).build())
            .withMessageContaining("title");

        assertThatNullPointerException()
            .isThrownBy(() -> ServiceErrorResponse.builder().title("x").build())
            .withMessageContaining("code");
    }

    @Test
    void anExceptionCarriesTheResponsesStatusAndRetryability() {
        ServiceException ex = ServiceException.of(ErrorCodes.UPSTREAM_UNAVAILABLE)
            .reason("1SB did not answer")
            .build();

        assertThat(ex.getHttpStatus()).isEqualTo(503);
        assertThat(ex.isRetryable()).isTrue();
        assertThat(ex.getMessage())
            .as("the exception message is the safe detail, never the reason")
            .isEqualTo(ex.getErrorResponse().getDetail());
        assertThat(ex.getDiagnostic().getReason()).isEqualTo("1SB did not answer");
    }

    @Test
    void anExceptionKeepsItsCause() {
        Exception boom = new IllegalStateException("connection reset");

        ServiceException ex = ServiceException.of(ErrorCodes.UPSTREAM_UNAVAILABLE).cause(boom).build();

        assertThat(ex).hasCause(boom);
        assertThat(ex.getDiagnostic().getCauseChain())
            .contains("IllegalStateException: connection reset");
    }
}
