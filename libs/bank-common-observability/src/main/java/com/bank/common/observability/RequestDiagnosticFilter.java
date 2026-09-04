package com.bank.common.observability;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds the diagnostic context at the edge of every request — work item {@code ERR-006}.
 *
 * <p>{@link MdcKeys} has existed since the observability library was written, and until this filter
 * nothing populated it: every key was declared and none was ever set. A log platform cannot join
 * lines across services on a field nobody writes, so the correlation story was aspirational.
 *
 * <p>What this establishes, on every request:
 *
 * <ul>
 *   <li>{@link MdcKeys#CORRELATION_ID} — taken from {@code X-Correlation-Id} when the caller
 *       supplies one, generated when they do not. Generated rather than left empty, because the
 *       request that most needs joining up is the one from a caller who forgot the header.</li>
 *   <li>{@link MdcKeys#JOURNEY_ID} — when the caller names a journey.</li>
 *   <li>{@link MdcKeys#SERVICE} — so a line is attributable once several services' logs are in one
 *       index.</li>
 * </ul>
 *
 * <p>The correlation id is echoed back on the response, so a caller reporting a problem can quote
 * it without reading the body.
 *
 * <p>MDC is cleared in a {@code finally} block. On a pooled request thread, a value left behind is
 * worse than no value at all: it attaches one caller's identifiers to the next caller's log lines.
 */
public class RequestDiagnosticFilter implements Filter {

    private final String serviceId;

    public RequestDiagnosticFilter(String serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String correlationId = firstNonBlank(
            httpRequest.getHeader(TraceHeaders.CORRELATION_ID),
            UUID.randomUUID().toString());

        Map<String, String> context = new LinkedHashMap<>();
        context.put(MdcKeys.CORRELATION_ID, correlationId);
        context.put(MdcKeys.SERVICE, serviceId);

        String journeyId = httpRequest.getHeader("X-Journey-Id");
        if (journeyId != null && !journeyId.isBlank()) {
            context.put(MdcKeys.JOURNEY_ID, journeyId);
        }

        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader(TraceHeaders.CORRELATION_ID, correlationId);
        }

        try {
            MdcContext.with(context, () -> {
                try {
                    chain.doFilter(request, response);
                } catch (IOException | ServletException | RuntimeException e) {
                    throw new FilterFailure(e);
                }
            });
        } catch (FilterFailure wrapper) {
            // Unwrap so the container sees the original failure, not this filter's plumbing.
            switch (wrapper.getCause()) {
                case IOException e -> throw e;
                case ServletException e -> throw e;
                case RuntimeException e -> throw e;
                default -> throw wrapper;
            }
        }
    }

    private static String firstNonBlank(String candidate, String fallback) {
        return candidate != null && !candidate.isBlank() ? candidate : fallback;
    }

    /** Carries a checked exception out of the {@code Runnable} that MDC cleanup wraps. */
    private static final class FilterFailure extends RuntimeException {
        FilterFailure(Throwable cause) {
            super(cause);
        }
    }
}
