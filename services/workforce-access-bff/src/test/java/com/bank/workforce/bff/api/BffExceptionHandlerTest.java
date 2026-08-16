package com.bank.workforce.bff.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The BFF error contract.
 *
 * <p>The asymmetry between these two handlers is deliberate and is the reason this test exists:
 * a validation failure tells the caller what was wrong with their request, while an
 * authentication failure deliberately does <b>not</b>. Echoing an internal
 * {@code IllegalStateException} message on the authentication path is how a BFF ends up
 * telling an attacker whether a subject exists, whether a token had expired, or which
 * downstream call failed.
 *
 * <p>{@code authenticationDeniedDoesNotEchoTheInternalMessage} is the assertion that keeps that
 * property from being "fixed" by a well-meaning change that adds the message back for
 * debuggability.
 */
class BffExceptionHandlerTest {

    private final BffExceptionHandler handler = new BffExceptionHandler();

    @Test
    @DisplayName("an invalid request returns 400 and explains what was wrong")
    void invalidRequestReturnsBadRequestWithDetail() {
        ProblemDetail detail = handler.invalidRequest(
            new IllegalArgumentException("returnUri is not in the allow-list"));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getTitle()).isEqualTo("Invalid authentication request");
        assertThat(detail.getDetail()).isEqualTo("returnUri is not in the allow-list");
    }

    @Test
    @DisplayName("a failed authentication returns 401 with a generic detail")
    void authenticationDeniedReturnsUnauthorized() {
        ProblemDetail detail = handler.authenticationDenied(new IllegalStateException("boom"));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(detail.getTitle()).isEqualTo("Authentication unsuccessful");
        assertThat(detail.getDetail()).isEqualTo("The authentication request could not be completed.");
    }

    @Test
    @DisplayName("the 401 response never echoes the internal exception message")
    void authenticationDeniedDoesNotEchoTheInternalMessage() {
        ProblemDetail detail = handler.authenticationDenied(
            new IllegalStateException("Unable to decrypt workforce session for subject rm-e123"));

        assertThat(detail.getDetail())
            .as("internal failure reasons must not reach the caller on the auth path")
            .doesNotContain("decrypt")
            .doesNotContain("rm-e123")
            .isEqualTo("The authentication request could not be completed.");
    }

    @Test
    @DisplayName("a null validation message does not produce a malformed problem document")
    void invalidRequestToleratesANullMessage() {
        ProblemDetail detail = handler.invalidRequest(new IllegalArgumentException());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getDetail()).isNull();
    }
}
