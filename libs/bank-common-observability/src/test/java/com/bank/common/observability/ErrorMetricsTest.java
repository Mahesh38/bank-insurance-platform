package com.bank.common.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** ERR-004 — the one error series, and the guard that keeps it from becoming an outage. */
class ErrorMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ErrorMetrics metrics = new ErrorMetrics(registry);

    @Test
    void oneSeriesAnswersTheDashboardQuestions() {
        metrics.record("bff", "SUITABILITY_REQUIRED", "COMPLIANCE_GATE", "L6",
            "journey-orchestration", false, 403);
        metrics.record("bff", "SUITABILITY_REQUIRED", "COMPLIANCE_GATE", "L6",
            "journey-orchestration", false, 403);
        metrics.record("onesb", "UPSTREAM_UNAVAILABLE", "UPSTREAM", "L5", "persistence", true, 503);

        // "which errors are populating more"
        assertThat(registry.find(ErrorMetrics.ERROR_COUNT)
            .tag("code", "SUITABILITY_REQUIRED").counter().count()).isEqualTo(2.0);

        // "is it us or them" — by the service that actually failed, not the one that answered
        assertThat(registry.find(ErrorMetrics.ERROR_COUNT)
            .tag("originService", "persistence").counter().count()).isEqualTo(1.0);

        // "platform fault vs caller fault"
        assertThat(registry.find(ErrorMetrics.ERROR_COUNT)
            .tag("category", "UPSTREAM").counter().count()).isEqualTo(1.0);

        assertThat(registry.find(ErrorMetrics.ERROR_COUNT)
            .tag("retryable", "true").counter().count()).isEqualTo(1.0);
    }

    @Test
    void anIdentifierNeverBecomesATagValue() {
        // The failure this prevents: one time series per job id exhausts the metrics backend far
        // faster than the traffic that produced it.
        assertThat(ErrorMetrics.safeTag("job-8f2c1e44-0b7a-4a11-9a3e-2f7d5c1b9e00"))
            .isEqualTo(ErrorMetrics.UNKNOWN);
        assertThat(ErrorMetrics.safeTag("Proposal job not found: j-1"))
            .as("a message has spaces and punctuation and is never a tag")
            .isEqualTo(ErrorMetrics.UNKNOWN);
        assertThat(ErrorMetrics.safeTag("/v1/quotes/abc")).isEqualTo(ErrorMetrics.UNKNOWN);
        assertThat(ErrorMetrics.safeTag(null)).isEqualTo(ErrorMetrics.UNKNOWN);
        assertThat(ErrorMetrics.safeTag("  ")).isEqualTo(ErrorMetrics.UNKNOWN);
    }

    @Test
    void boundedTokensPassThroughUnchanged() {
        assertThat(ErrorMetrics.safeTag("SUITABILITY_REQUIRED")).isEqualTo("SUITABILITY_REQUIRED");
        assertThat(ErrorMetrics.safeTag("journey-orchestration")).isEqualTo("journey-orchestration");
        assertThat(ErrorMetrics.safeTag("L6")).isEqualTo("L6");
        assertThat(ErrorMetrics.safeTag(" onesb ")).isEqualTo("onesb");
    }

    @Test
    void aMissingTagStillProducesASeries() {
        metrics.record(null, null, null, null, null, false, 500);

        assertThat(registry.find(ErrorMetrics.ERROR_COUNT)
            .tag("service", ErrorMetrics.UNKNOWN).counter())
            .as("a series must never silently disappear because one tag was absent")
            .isNotNull();
    }
}
