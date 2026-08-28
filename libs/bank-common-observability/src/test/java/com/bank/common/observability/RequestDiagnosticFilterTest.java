package com.bank.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/** ERR-006 — the filter that finally gives {@link MdcKeys} a producer. */
class RequestDiagnosticFilterTest {

    private final RequestDiagnosticFilter filter = new RequestDiagnosticFilter("onesb");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    /** Captures what the MDC looked like *inside* the chain, which is the only place it exists. */
    private Map<String, String> capture(MockHttpServletRequest request) throws Exception {
        Map<String, String> seen = new HashMap<>();
        FilterChain chain = (req, res) -> {
            Map<String, String> copy = MDC.getCopyOfContextMap();
            if (copy != null) {
                seen.putAll(copy);
            }
        };
        filter.doFilter(request, response, chain);
        return seen;
    }

    @Test
    void aCallerSuppliedCorrelationIdIsHonouredAndEchoed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceHeaders.CORRELATION_ID, "corr-from-caller");

        Map<String, String> inside = capture(request);

        assertThat(inside).containsEntry(MdcKeys.CORRELATION_ID, "corr-from-caller");
        assertThat(inside).containsEntry(MdcKeys.SERVICE, "onesb");
        assertThat(response.getHeader(TraceHeaders.CORRELATION_ID)).isEqualTo("corr-from-caller");
    }

    @Test
    void aMissingCorrelationIdIsGeneratedRatherThanLeftEmpty() throws Exception {
        Map<String, String> inside = capture(new MockHttpServletRequest());

        assertThat(inside.get(MdcKeys.CORRELATION_ID))
            .as("the request that most needs joining up is the one whose caller forgot the header")
            .isNotBlank();
        assertThat(response.getHeader(TraceHeaders.CORRELATION_ID))
            .isEqualTo(inside.get(MdcKeys.CORRELATION_ID));
    }

    @Test
    void aBlankHeaderIsTreatedAsAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceHeaders.CORRELATION_ID, "   ");

        assertThat(capture(request).get(MdcKeys.CORRELATION_ID)).isNotBlank().isNotEqualTo("   ");
    }

    @Test
    void aJourneyIdIsCarriedWhenTheCallerNamesOne() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Journey-Id", "jrn-42");

        assertThat(capture(request)).containsEntry(MdcKeys.JOURNEY_ID, "jrn-42");
        assertThat(capture(new MockHttpServletRequest())).doesNotContainKey(MdcKeys.JOURNEY_ID);
    }

    @Test
    void theContextIsClearedAfterTheRequest() throws Exception {
        capture(new MockHttpServletRequest());

        assertThat(MDC.getCopyOfContextMap())
            .as("on a pooled thread a leftover value attaches one caller's identifiers to the "
                + "next caller's log lines, which is worse than having none")
            .satisfiesAnyOf(
                map -> assertThat(map).isNull(),
                map -> assertThat(map).isEmpty());
    }

    @Test
    void theContextIsClearedEvenWhenTheRequestFails() {
        FilterChain exploding = (req, res) -> {
            throw new IllegalStateException("downstream blew up");
        };

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), response, exploding))
            .as("the original failure must reach the container, not this filter's plumbing")
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("downstream blew up");

        assertThat(MDC.get(MdcKeys.CORRELATION_ID)).isNull();
    }

    @Test
    void aCheckedFailureReachesTheContainerUnwrapped() {
        FilterChain ioFailure = (req, res) -> {
            throw new IOException("socket closed");
        };
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), response, ioFailure))
            .isInstanceOf(IOException.class)
            .hasMessage("socket closed");

        FilterChain servletFailure = (req, res) -> {
            throw new ServletException("bad servlet");
        };
        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), response, servletFailure))
            .isInstanceOf(ServletException.class)
            .hasMessage("bad servlet");
    }

    @Test
    void aNonHttpRequestPassesStraightThrough() throws Exception {
        boolean[] called = {false};
        filter.doFilter(new jakarta.servlet.ServletRequestWrapper(new MockHttpServletRequest()) {},
            response, (req, res) -> called[0] = true);

        assertThat(called[0]).isTrue();
    }
}
