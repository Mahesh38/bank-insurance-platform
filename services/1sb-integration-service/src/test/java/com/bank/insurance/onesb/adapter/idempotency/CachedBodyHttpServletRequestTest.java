package com.bank.insurance.onesb.adapter.idempotency;

import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA-001 — the idempotency filter hashes the body and then hands it on. If the cached body were
 * not fully re-readable by <em>both</em> access paths, Spring MVC would deserialise a truncated
 * or empty payload after the filter ran — a silent data-loss bug rather than a visible failure.
 * <p>
 * {@link IdempotencyFilterTest} covers the filter's behaviour end to end via
 * {@code getInputStream()}; this class covers {@code getReader()} (used by any handler taking a
 * {@code Reader}) and the {@code ServletInputStream} contract.
 */
@Tag("unit")
@Tag("R6")
class CachedBodyHttpServletRequestTest {

    private static final String BODY = "{\"lob\":\"TERM\",\"sumAssured\":5000000}";

    private static CachedBodyHttpServletRequest wrap(String body, String encoding) throws IOException {
        MockHttpServletRequest delegate = new MockHttpServletRequest("POST", "/v1/quotes");
        delegate.setContent(body.getBytes(StandardCharsets.UTF_8));
        if (encoding != null) {
            delegate.setCharacterEncoding(encoding);
        } else {
            // MockHttpServletRequest defaults to a non-null encoding; clear it so the wrapper's
            // UTF-8 fallback branch is the one under test.
            delegate.setCharacterEncoding(null);
        }
        return new CachedBodyHttpServletRequest(delegate);
    }

    @Test
    void getReader_returnsFullBody_whenNoCharacterEncodingIsSet() throws IOException {
        try (BufferedReader reader = wrap(BODY, null).getReader()) {
            assertThat(reader.lines().reduce("", String::concat)).isEqualTo(BODY);
        }
    }

    @Test
    void getReader_honoursDeclaredCharacterEncoding() throws IOException {
        try (BufferedReader reader = wrap(BODY, "UTF-8").getReader()) {
            assertThat(reader.lines().reduce("", String::concat)).isEqualTo(BODY);
        }
    }

    /** Each call must hand out an independent stream — the body is read more than once. */
    @Test
    void bodyIsRereadable_acrossReaderAndInputStream() throws IOException {
        CachedBodyHttpServletRequest request = wrap(BODY, "UTF-8");

        String viaReader;
        try (BufferedReader reader = request.getReader()) {
            viaReader = reader.lines().reduce("", String::concat);
        }
        String viaStream = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(viaReader).isEqualTo(BODY);
        assertThat(viaStream).isEqualTo(BODY);
        assertThat(request.getCachedBody()).isEqualTo(BODY.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void inputStream_reportsFinishedOnlyAfterBodyIsConsumed() throws IOException {
        ServletInputStream stream = wrap(BODY, "UTF-8").getInputStream();

        assertThat(stream.isFinished()).isFalse();
        assertThat(stream.isReady()).isTrue();

        stream.readAllBytes();

        assertThat(stream.isFinished()).isTrue();
    }

    @Test
    void emptyBody_isFinishedImmediately_andReadsAsEmpty() throws IOException {
        CachedBodyHttpServletRequest request = wrap("", "UTF-8");

        assertThat(request.getInputStream().isFinished()).isTrue();
        try (BufferedReader reader = request.getReader()) {
            assertThat(reader.readLine()).isNull();
        }
    }

    @Test
    void setReadListener_isANoOp_forBlockingMvc() throws IOException {
        ServletInputStream stream = wrap(BODY, "UTF-8").getInputStream();

        stream.setReadListener(null);

        assertThat(stream.isReady()).isTrue();
    }
}
