package com.bank.common.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

  private final EventJsonCodec codec = new EventJsonCodec();

  @Test
  void roundTripIsLosslessAndValidatesAgainstTheEnvelopeSchema() throws Exception {
    EventEnvelope event = TestEvents.paymentReconciled(Ulid.mint());

    String json = codec.encode(event);

    assertThat(SchemaSupport.validate("event-envelope.v1.json", json)).isEmpty();
    assertThat(codec.decode(json)).isEqualTo(event);
    ObjectNode data = (ObjectNode) SchemaSupport.MAPPER.readTree(json).get("data");
    assertThat(SchemaSupport.validate("PaymentReconciled.v1.json", data)).isEmpty();
  }

  @Test
  void partnerRepActorRoundTripsAndValidates() throws Exception {
    EventEnvelope event =
        EventEnvelope.builder()
            .eventType("OfferSelected")
            .source("quotation")
            .aggregate("QUOTE", Ulid.mint())
            .actor(new Actor("ipr-9", Actor.INSURER_PARTNER_REP, "ASSIST_ONLY", "INS01", "rm-1"))
            .traceId("t")
            .correlationId("c")
            .configVersionRef("SUITABILITY.GATE@1.0.0")
            .data(Map.of("quoteId", Ulid.mint(), "state", "SELECTED"))
            .build();

    String json = codec.encode(event);

    assertThat(SchemaSupport.validate("event-envelope.v1.json", json)).isEmpty();
    assertThat(codec.decode(json)).isEqualTo(event);
  }

  @Test
  void schemaRejectsPartnerRepWithoutAttribution() throws Exception {
    ObjectNode json =
        (ObjectNode)
            SchemaSupport.MAPPER.readTree(
                codec.encode(TestEvents.event(Ulid.mint(), "PaymentCaptured")));
    ((ObjectNode) json.get("actor")).put("type", Actor.INSURER_PARTNER_REP);

    assertThat(SchemaSupport.validate("event-envelope.v1.json", json)).isNotEmpty();
  }

  @Test
  void builderMintsIdAndTimeAndTruncatesToMillis() {
    EventEnvelope event =
        EventEnvelope.builder()
            .eventType("LeadCreated")
            .source("lead")
            .aggregate("LEAD", Ulid.mint())
            .occurredAt(Instant.parse("2026-10-21T09:00:00.123456789Z"))
            .actor(Actor.of("rm", "BANK_RM", "SP_ACCOUNTABLE"))
            .traceId("t")
            .correlationId("c")
            .build();

    assertThat(Ulid.isValid(event.eventId())).isTrue();
    assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-10-21T09:00:00.123Z"));
    assertThat(event.schemaVersion()).isEqualTo(1);
  }

  @Test
  void rejectsMalformedFields() {
    assertThatThrownBy(() -> base().eventType("payment_reconciled").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("eventType");
    assertThatThrownBy(() -> base().eventId("not-a-ulid").build()).hasMessageContaining("eventId");
    assertThatThrownBy(() -> base().schemaVersion(0).build()).hasMessageContaining("schemaVersion");
    assertThatThrownBy(() -> base().source("Payment").build()).hasMessageContaining("source");
    assertThatThrownBy(() -> base().aggregate("payment", "x").build())
        .hasMessageContaining("aggregateType");
    assertThatThrownBy(() -> base().aggregate("PAYMENT", " ").build())
        .hasMessageContaining("aggregateId");
    assertThatThrownBy(() -> base().journeyId("x").build()).hasMessageContaining("journeyId");
    assertThatThrownBy(() -> base().leadId("x").build()).hasMessageContaining("leadId");
    assertThatThrownBy(() -> base().traceId("").build()).hasMessageContaining("traceId");
    assertThatThrownBy(() -> base().correlationId(null).build())
        .hasMessageContaining("correlationId");
  }

  @Test
  void actorRules() {
    assertThatThrownBy(() -> Actor.of("", "BANK_RM", null)).hasMessageContaining("actor.id");
    assertThatThrownBy(() -> Actor.of("a", "rm", null)).hasMessageContaining("actor.type");
    assertThatThrownBy(() -> Actor.of("a", "BANK_RM", "OWNER"))
        .hasMessageContaining("actingCapacity");
    assertThatThrownBy(() -> Actor.of("a", Actor.INSURER_PARTNER_REP, "ASSIST_ONLY"))
        .hasMessageContaining("INV-ACT-04");
    assertThat(Actor.system("payment").type()).isEqualTo("SYSTEM");
  }

  @Test
  void decodeRejectsBrokenJson() {
    assertThatThrownBy(() -> codec.decode("{not json"))
        .isInstanceOf(EventCodecException.class)
        .hasMessageContaining("not readable");
    assertThatThrownBy(() -> codec.decode("{\"eventId\":\"x\"}"))
        .isInstanceOf(EventCodecException.class)
        .hasMessageContaining("event-envelope/v1");
  }

  @Test
  void dataKeepsNullsAndIsImmutable() {
    EventEnvelope event = base().data("previousStatus", null).build();

    assertThat(event.data()).containsEntry("previousStatus", null);
    assertThatThrownBy(() -> event.data().put("x", 1))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static EventEnvelope.Builder base() {
    return EventEnvelope.builder()
        .clock(TestEvents.tickingClock())
        .eventType("PaymentCaptured")
        .source("payment")
        .aggregate("PAYMENT", Ulid.mint())
        .actor(Actor.system("payment"))
        .traceId("t")
        .correlationId("c");
  }
}
