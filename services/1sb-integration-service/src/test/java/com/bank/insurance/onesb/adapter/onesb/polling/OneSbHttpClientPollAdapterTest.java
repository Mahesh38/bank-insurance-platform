package com.bank.insurance.onesb.adapter.onesb.polling;

import com.bank.common.error.ErrorCatalogue;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPollPort.PollResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@Tag("QA-006")
class OneSbHttpClientPollAdapterTest {

    private static final String PATH = "/insurance/lifeterm/v1/quote/poll/REQ-1";

    private OneSbHttpClient httpClient;
    private OneSbHttpClientPollAdapter adapter;

    @BeforeEach
    void setUp() {
        httpClient = mock(OneSbHttpClient.class);
        adapter = new OneSbHttpClientPollAdapter(httpClient, new ObjectMapper());
    }

    @Test
    void poll_delegatesToHttpClient_andParsesDataIsPollCompleteTrue() {
        when(httpClient.get(PATH, String.class))
                .thenReturn("{\"data\":{\"isPollComplete\":true}}");

        PollResult result = adapter.poll(PATH);

        assertThat(result.complete()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(result.errorMessage()).isNull();
        verify(httpClient).get(eq(PATH), eq(String.class));
    }

    @Test
    void poll_topLevelIsPollCompleteFalse_returnsIncomplete() {
        when(httpClient.get(PATH, String.class))
                .thenReturn("{\"isPollComplete\":false}");

        PollResult result = adapter.poll(PATH);

        assertThat(result.complete()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(200);
    }

    @Test
    void poll_textualIsPollComplete_parsesBoolean() {
        when(httpClient.get(PATH, String.class))
                .thenReturn("{\"data\":{\"isPollComplete\":\"true\"}}");

        PollResult result = adapter.poll(PATH);

        assertThat(result.complete()).isTrue();
        assertThat(result.httpStatus()).isEqualTo(200);
    }

    @Test
    void poll_blankBody_returnsIncomplete() {
        when(httpClient.get(PATH, String.class)).thenReturn("   ");

        PollResult result = adapter.poll(PATH);

        assertThat(result.complete()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(200);
    }

    @Test
    @Tag("FUNC-006")
    void poll_extractsApplicationNumber_whenComplete() {
        when(httpClient.get(PATH, String.class))
                .thenReturn("{\"data\":{\"isPollComplete\":true,\"applicationNumber\":\"APP-POLL\"}}");

        PollResult result = adapter.poll(PATH);

        assertThat(result.complete()).isTrue();
        assertThat(result.applicationNumber()).isEqualTo("APP-POLL");
        assertThat(result.offers()).isEmpty();
    }

    @Test
    void poll_serviceException_mapsHttpStatusAndSafeMessage() {
        ServiceException upstreamAuthFailure = ServiceException.of(ErrorCodes.UPSTREAM_AUTH_FAILURE)
                .reason("1SB returned 401")
                .build();

        when(httpClient.get(PATH, String.class)).thenThrow(upstreamAuthFailure);

        PollResult result = adapter.poll(PATH);

        assertThat(result.complete()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(502);

        // The poll result carries the exception's message, and that message is now the catalogue's
        // safe detail rather than the developer text. The developer text is not lost — it is on the
        // diagnostic, reachable by the incident id — but it no longer travels on anything a caller
        // can read.
        assertThat(result.errorMessage())
                .isEqualTo(ErrorCatalogue.require(ErrorCodes.UPSTREAM_AUTH_FAILURE).publicDetail())
                .doesNotContain("401");
        assertThat(upstreamAuthFailure.getDiagnostic().getReason()).isEqualTo("1SB returned 401");
    }

    @Test
    void poll_genericException_returnsTransportError() {
        when(httpClient.get(PATH, String.class))
                .thenThrow(new RuntimeException("connection reset"));

        PollResult result = adapter.poll(PATH);

        assertThat(result.complete()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(0);
        assertThat(result.errorMessage()).isEqualTo("connection reset");
    }

    @Test
    void poll_invalidJson_returnsIncompleteWith200() {
        when(httpClient.get(PATH, String.class)).thenReturn("not-json");

        PollResult result = adapter.poll(PATH);

        assertThat(result.complete()).isFalse();
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(result.errorMessage()).isNull();
    }
}
