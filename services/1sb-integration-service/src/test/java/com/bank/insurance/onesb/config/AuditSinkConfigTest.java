package com.bank.insurance.onesb.config;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.insurance.onesb.adapter.persistence.HttpAuditEventStoreAdapter;
import com.bank.insurance.onesb.observability.CompositeAuditEventPublisher;
import com.bank.insurance.onesb.observability.LoggingAuditEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RISK-012 — sink selection is what keeps the production transport decision open, so the
 * selection logic itself has to be trustworthy.
 *
 * <p>The failure that matters is not a wrong sink; it is a service that boots looking healthy
 * while publishing audit events nowhere. Both routes to that outcome — an empty sink list and
 * the declared-but-unimplemented KAFKA sink — are asserted to fail startup instead.
 */
@Tag("unit")
@Tag("COMP-002")
class AuditSinkConfigTest {

    private final AuditSinkConfig config = new AuditSinkConfig();
    private final RestClient restClient = RestClient.builder().baseUrl("http://unused.invalid").build();

    private AuditEventPublisher build(String... sinks) {
        return config.auditEventPublisher(
                new AuditProperties(List.of(sinks)), restClient, new ObjectMapper());
    }

    @Test
    void defaultIsLogPlusPersistence() {
        AuditProperties defaults = new AuditProperties(List.of("LOG", "PERSISTENCE"));

        assertThat(defaults.resolvedSinks())
                .containsExactly(AuditProperties.Sink.LOG, AuditProperties.Sink.PERSISTENCE);
    }

    @Test
    void logOnlyBuildsASingleLogSink() {
        CompositeAuditEventPublisher publisher = (CompositeAuditEventPublisher) build("LOG");

        assertThat(publisher.sinks()).singleElement().isInstanceOf(LoggingAuditEventPublisher.class);
    }

    @Test
    void persistenceOnlyBuildsASingleHttpSink() {
        CompositeAuditEventPublisher publisher = (CompositeAuditEventPublisher) build("PERSISTENCE");

        assertThat(publisher.sinks()).singleElement()
                .isInstanceOf(HttpAuditEventStoreAdapter.class);
    }

    @Test
    void bothSinksAreBuiltInTheOrderConfigured() {
        CompositeAuditEventPublisher publisher =
                (CompositeAuditEventPublisher) build("LOG", "PERSISTENCE");

        assertThat(publisher.sinks()).hasSize(2);
        assertThat(publisher.sinks().get(0)).isInstanceOf(LoggingAuditEventPublisher.class);
        assertThat(publisher.sinks().get(1)).isInstanceOf(HttpAuditEventStoreAdapter.class);
    }

    @Test
    void sinkNamesAreCaseAndWhitespaceInsensitive() {
        CompositeAuditEventPublisher publisher =
                (CompositeAuditEventPublisher) build(" log ", "Persistence");

        assertThat(publisher.sinks()).hasSize(2);
    }

    @Test
    void duplicateSinkNamesDoNotDoublePublish() {
        CompositeAuditEventPublisher publisher =
                (CompositeAuditEventPublisher) build("LOG", "LOG");

        assertThat(publisher.sinks()).hasSize(1);
    }

    // --- the two ways to silently lose every audit event -----------------------------------

    @Test
    void noSinksFailsStartupRatherThanDiscardingEvents() {
        assertThatThrownBy(() -> build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refusing to start");
    }

    @Test
    void kafkaIsDeclaredButUnimplemented_andFailsStartup() {
        assertThatThrownBy(() -> build("KAFKA"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not implemented");
    }

    @Test
    void anUnknownSinkNameIsRejectedWithTheValidValues() {
        assertThatThrownBy(() -> new AuditProperties(List.of("DATABASE")).resolvedSinks())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown audit sink 'DATABASE'")
                .hasMessageContaining("PERSISTENCE");
    }

    // --- composite behaviour ----------------------------------------------------------------

    /** One broken sink must not suppress the others — that is the point of having several. */
    @Test
    void aThrowingSinkDoesNotPreventTheOthersFromReceivingTheEvent() {
        AtomicInteger delivered = new AtomicInteger();
        CompositeAuditEventPublisher publisher = new CompositeAuditEventPublisher(List.of(
                event -> { throw new IllegalStateException("sink is broken"); },
                event -> delivered.incrementAndGet()));

        assertThatCode(() -> publisher.publish(event())).doesNotThrowAnyException();
        assertThat(delivered.get()).isEqualTo(1);
    }

    @Test
    void nullEventIsIgnored() {
        AtomicInteger delivered = new AtomicInteger();
        CompositeAuditEventPublisher publisher =
                new CompositeAuditEventPublisher(List.of(event -> delivered.incrementAndGet()));

        publisher.publish(null);

        assertThat(delivered.get()).isZero();
    }

    private static AuditEvent event() {
        return AuditEvent.builder()
                .actorId("rm-1")
                .action(AuditActions.QUOTE_JOB_CREATED)
                .outcome(AuditOutcomes.SUCCESS)
                .build();
    }
}
