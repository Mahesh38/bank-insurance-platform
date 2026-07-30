package com.bank.common.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityConstantsTest {

    @Test
    void mdcKeys_areStable() {
        assertThat(MdcKeys.JOB_ID).isEqualTo("jobId");
        assertThat(MdcKeys.LOB).isEqualTo("lob");
        assertThat(MdcKeys.ACTOR_ID).isEqualTo("actorId");
        assertThat(MdcKeys.TRACE_ID).isEqualTo("traceId");
        assertThat(MdcKeys.SPAN_ID).isEqualTo("spanId");
        assertThat(MdcKeys.JOURNEY_ID).isEqualTo("journeyId");
        assertThat(MdcKeys.CORRELATION_ID).isEqualTo("correlationId");
    }

    @Test
    void traceHeaders_areStable() {
        assertThat(TraceHeaders.TRACE_PARENT).isEqualTo("traceparent");
        assertThat(TraceHeaders.TRACE_STATE).isEqualTo("tracestate");
        assertThat(TraceHeaders.CORRELATION_ID).isEqualTo("X-Correlation-Id");
        assertThat(TraceHeaders.REQUEST_ID).isEqualTo("X-Request-Id");
    }

    @Test
    void metricNames_useBankPrefix() {
        assertThat(MetricNames.JOB_CREATED).startsWith("bank.onesb.");
        assertThat(MetricNames.POLL_DURATION).isEqualTo("bank.onesb.poll.duration");
        assertThat(MetricNames.IDEMPOTENCY_HIT).isEqualTo("bank.onesb.idempotency.hit");
        assertThat(BankMetricNames.ONESB_JOB_CREATED).isEqualTo(MetricNames.JOB_CREATED);
        assertThat(BankMetricNames.ONESB_ERROR_COUNT).isEqualTo(MetricNames.ERROR_COUNT);
    }
}
