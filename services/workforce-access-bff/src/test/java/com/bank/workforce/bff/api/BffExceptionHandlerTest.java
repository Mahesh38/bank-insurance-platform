package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCategory;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.IncidentId;
import com.bank.common.error.ServiceErrorResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.bank.common.observability.ErrorMetrics;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The BFF error contract, at the redaction boundary.
 *
 * <p>This test used to guard an asymmetry: the 401 path must never echo an internal message,
 * while the 400 path was allowed to, for debuggability. The 400 half of that has been withdrawn
 * deliberately, not overlooked — the {@link IllegalArgumentException}s that reach it include
 * {@code "workforce.session.encryption-key must decode to 32 bytes"}, so "the message is safe
 * because today's messages are safe" was never a property, only a coincidence.
 *
 * <p>The guard is therefore stronger and now covers both paths:
 * {@link #noPathEchoesAnInternalMessage()} is the assertion that keeps a well-meaning change from
 * putting the message back for debuggability. {@link #theCallerIsGivenSomethingToQuote()} is the
 * other half of the bargain — the caller loses the message but gains an incident id that finds it.
 */
class BffExceptionHandlerTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final BffExceptionHandler handler =
        new BffExceptionHandler(new StaticObjectProvider<>(meterRegistry));

    private ServiceErrorResponse invalid(String message) {
        return body(handler.invalidRequest(new IllegalArgumentException(message)));
    }

    private ServiceErrorResponse denied(String message) {
        return body(handler.authenticationDenied(new IllegalStateException(message)));
    }

    private static ServiceErrorResponse body(ResponseEntity<ServiceErrorResponse> response) {
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    @Test
    @DisplayName("an invalid request returns 400 with a stable code and safe wording")
    void invalidRequestReturnsBadRequest() {
        ServiceErrorResponse detail = invalid("returnUri is not in the allow-list");

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getCode()).isEqualTo(ErrorCodes.SCHEMA_INVALID);
        assertThat(detail.getCategory()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(detail.getService()).isEqualTo("bff");
    }

    @Test
    @DisplayName("a failed authentication returns 401 with a generic detail")
    void authenticationDeniedReturnsUnauthorized() {
        ServiceErrorResponse detail = denied("boom");

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(detail.getCode()).isEqualTo(ErrorCodes.AUTHENTICATION_FAILED);
        assertThat(detail.getDetail()).isEqualTo("The sign-in could not be completed.");
    }

    @Test
    @DisplayName("neither path echoes the internal exception message")
    void noPathEchoesAnInternalMessage() {
        ServiceErrorResponse authFailure =
            denied("Unable to decrypt workforce session for subject rm-e123");

        assertThat(authFailure.getDetail())
            .as("internal failure reasons must not reach the caller on the auth path")
            .doesNotContain("decrypt")
            .doesNotContain("rm-e123");

        ServiceErrorResponse configLeak =
            invalid("workforce.session.encryption-key must decode to 32 bytes");

        assertThat(configLeak.getDetail())
            .as("nor on the validation path — this one is an internal configuration detail")
            .doesNotContain("encryption-key")
            .doesNotContain("32 bytes");
    }

    @Test
    @DisplayName("no response leaving L4 carries a diagnostic")
    void nothingCrossingTheBoundaryCarriesADiagnostic() {
        for (ServiceErrorResponse response : new ServiceErrorResponse[] {
            invalid("returnUri is not in the allow-list"),
            denied("Business identity is not active")
        }) {
            assertThat(response.carriesDiagnostics())
                .as("L4 is the first hop that must never emit a diagnostic")
                .isFalse();
            assertThat(response.getDiagnostic()).isNull();
            assertThat(response.getOrigin()).isNull();
        }
    }

    @Test
    @DisplayName("the caller is given an incident id to quote to support")
    void theCallerIsGivenSomethingToQuote() {
        ServiceErrorResponse detail = denied("Business identity is not active");

        assertThat(detail.getIncidentId())
            .as("the message is withheld, so the id that finds it must not be")
            .isNotNull();
        assertThat(IncidentId.isValid(detail.getIncidentId())).isTrue();
    }

    @Test
    @DisplayName("a null message does not produce a malformed problem document")
    void toleratesANullMessage() {
        ServiceErrorResponse detail = invalid(null);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getDetail()).isNotBlank();
        assertThat(detail.getCode()).isEqualTo(ErrorCodes.SCHEMA_INVALID);
    }

    @Test
    @DisplayName("every refusal is counted, tagged for the dashboard")
    void everyRefusalIsCounted() {
        denied("Business identity is not active");
        invalid("returnUri is not in the allow-list");

        assertThat(meterRegistry.find(ErrorMetrics.ERROR_COUNT).counters())
            .as("a dashboard needs one consistently tagged series, not a log grep")
            .isNotEmpty();

        var authCounter = meterRegistry.find(ErrorMetrics.ERROR_COUNT)
            .tag("code", ErrorCodes.AUTHENTICATION_FAILED)
            .tag("service", "bff")
            .tag("category", "AUTHENTICATION")
            .tag("originService", "bff")
            .counter();

        assertThat(authCounter).isNotNull();
        assertThat(authCounter.count()).isEqualTo(1.0);
    }

    /** Minimal {@link ObjectProvider} so the unit test can hand the handler a real registry. */
    private record StaticObjectProvider<T>(T value) implements ObjectProvider<T> {
        @Override public T getObject() { return value; }
        @Override public T getObject(Object... args) { return value; }
        @Override public T getIfAvailable() { return value; }
        @Override public T getIfUnique() { return value; }
        @Override public java.util.Iterator<T> iterator() { return java.util.List.of(value).iterator(); }
    }
}
