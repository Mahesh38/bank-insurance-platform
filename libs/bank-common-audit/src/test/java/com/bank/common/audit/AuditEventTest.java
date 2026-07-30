package com.bank.common.audit;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AuditEventTest {

    @Test
    void builderCreatesEvent_withAllFields() {
        Instant now = Instant.now();
        String eventId = UUID.randomUUID().toString();

        AuditEvent event = AuditEvent.builder()
            .eventId(eventId)
            .timestamp(now)
            .actorId("EMP001")
            .actorType("RM")
            .action(AuditActions.QUOTE_JOB_CREATED)
            .resourceType("QUOTE_JOB")
            .resourceId("job-123")
            .outcome(AuditOutcomes.SUCCESS)
            .lob("TERM")
            .journeyId("jrn_abc123")
            .distributorId("DIST_001")
            .agentId("AGT_999")
            .traceId("trace-xyz")
            .metadata("jobId", "job-123")
            .metadata("productCode", "P100")
            .build();

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getTimestamp()).isEqualTo(now);
        assertThat(event.getActorId()).isEqualTo("EMP001");
        assertThat(event.getActorType()).isEqualTo("RM");
        assertThat(event.getAction()).isEqualTo(AuditActions.QUOTE_JOB_CREATED);
        assertThat(event.getResourceType()).isEqualTo("QUOTE_JOB");
        assertThat(event.getResourceId()).isEqualTo("job-123");
        assertThat(event.getOutcome()).isEqualTo(AuditOutcomes.SUCCESS);
        assertThat(event.getLob()).isEqualTo("TERM");
        assertThat(event.getJourneyId()).isEqualTo("jrn_abc123");
        assertThat(event.getDistributorId()).isEqualTo("DIST_001");
        assertThat(event.getAgentId()).isEqualTo("AGT_999");
        assertThat(event.getTraceId()).isEqualTo("trace-xyz");
        assertThat(event.getMetadata()).containsEntry("jobId", "job-123")
                                      .containsEntry("productCode", "P100");
    }

    @Test
    void builder_autoGeneratesEventId_whenNotProvided() {
        AuditEvent event = AuditEvent.builder()
            .actorId("EMP001")
            .action(AuditActions.PROPOSAL_SUBMITTED)
            .outcome(AuditOutcomes.SUCCESS)
            .build();

        assertThat(event.getEventId()).isNotNull().isNotBlank();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void builder_throwsNPE_whenRequiredFieldsMissing() {
        assertThatNullPointerException()
            .isThrownBy(() -> AuditEvent.builder()
                .action(AuditActions.QUOTE_CREATED)
                .outcome(AuditOutcomes.SUCCESS)
                .build())
            .withMessageContaining("actorId");

        assertThatNullPointerException()
            .isThrownBy(() -> AuditEvent.builder()
                .actorId("EMP001")
                .outcome(AuditOutcomes.SUCCESS)
                .build())
            .withMessageContaining("action");
    }

    @Test
    void metadataMapIsImmutable() {
        AuditEvent event = AuditEvent.builder()
            .actorId("EMP001")
            .action(AuditActions.PAYMENT_URL_RETRIEVED)
            .outcome(AuditOutcomes.SUCCESS)
            .metadata("k", "v")
            .build();

        assertThatThrownBy(() -> event.getMetadata().put("x", "y"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void metadataAll_supportsMapInput() {
        AuditEvent event = AuditEvent.builder()
            .actorId("EMP001")
            .action(AuditActions.PROPOSAL_SUBMITTED)
            .outcome(AuditOutcomes.SUCCESS)
            .metadataAll(Map.of("applicationNumber", "APP123", "insurerCode", "ICICI"))
            .build();

        assertThat(event.getMetadata())
            .containsEntry("applicationNumber", "APP123")
            .containsEntry("insurerCode", "ICICI");
    }

    @Test
    void publisherInterface_canBeImplemented_asLambda() {
        var captured = new java.util.ArrayList<AuditEvent>();
        AuditEventPublisher publisher = captured::add;

        AuditEvent event = AuditEvent.builder()
            .actorId("EMP001")
            .action(AuditActions.DOCUMENT_UPLOADED)
            .outcome(AuditOutcomes.SUCCESS)
            .build();

        publisher.publish(event);

        assertThat(captured).hasSize(1).containsExactly(event);
    }
}
