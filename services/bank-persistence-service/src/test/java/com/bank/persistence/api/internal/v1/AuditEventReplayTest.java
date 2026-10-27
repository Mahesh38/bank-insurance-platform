package com.bank.persistence.api.internal.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.persistence.adapter.persistence.AuditEventEntity;
import com.bank.persistence.config.InternalAuthProperties;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * "Same content" for an audit replay: every supplied field counts; defaulted fields may be absent.
 */
@Tag("unit")
class AuditEventReplayTest {

  private static final CreateAuditEventRequest FULL =
      new CreateAuditEventRequest(
          "evt-1",
          Instant.parse("2026-10-21T10:00:00.123456789Z"),
          "A",
          "actor",
          "BANK_RM",
          "LEAD",
          "lead-1",
          "SUCCESS",
          "LIFE",
          "j-1",
          "d-1",
          "ag-1",
          "{}",
          "t-1",
          "{\"p\":1}",
          "{\"n\":2}",
          "c-1",
          "s-1",
          (short) 2,
          Instant.parse("2033-01-01T00:00:00Z"),
          4,
          "SP_ACCOUNTABLE",
          "ins-1",
          "as-1",
          "cfg-1");

  @Test
  void identicalReplayMatches() {
    AuditEventEntity stored = AuditEventController.toEntity("evt-1", FULL);
    assertThat(AuditEventController.sameContent(stored, FULL)).isTrue();
  }

  @Test
  void absentServerDefaultedFieldsMatchWhateverWasStored() throws Exception {
    AuditEventEntity stored = AuditEventController.toEntity("evt-1", FULL);
    CreateAuditEventRequest replay =
        with(with(FULL, "eventTime", null), "eventSchemaVersion", null);
    assertThat(AuditEventController.sameContent(stored, replay)).isTrue();
  }

  @Test
  void anyOtherChangedFieldIsADifferentEvent() throws Exception {
    AuditEventEntity stored = AuditEventController.toEntity("evt-1", FULL);
    for (RecordComponent c : CreateAuditEventRequest.class.getRecordComponents()) {
      if (c.getName().equals("eventId")) {
        continue;
      }
      Object changed =
          switch (c.getType().getSimpleName()) {
            case "Instant" -> Instant.parse("2020-01-01T00:00:00Z");
            case "Short" -> (short) 9;
            case "Integer" -> 99;
            default -> "changed";
          };
      assertThat(AuditEventController.sameContent(stored, with(FULL, c.getName(), changed)))
          .as(c.getName())
          .isFalse();
    }
  }

  @Test
  void internalCallerCredentialsAuthenticateConfiguredKeys() {
    InternalAuthProperties properties =
        new InternalAuthProperties(
            java.util.Map.of("onesb-integration-service", "a-long-enough-secret"),
            java.util.Set.of(),
            java.util.Map.of(
                "onesb-integration-service", java.util.Set.of("jobs", "audit.append")));
    assertThat(properties.credentials().authenticate(null)).isEmpty();
    assertThat(properties.credentials().authenticate(" ")).isEmpty();
    assertThat(properties.credentials().authenticate("a-long-enough-secret"))
        .contains("onesb-integration-service");
    assertThat(properties.toString()).doesNotContain("a-long-enough-secret");
  }

  private static CreateAuditEventRequest with(CreateAuditEventRequest r, String field, Object value)
      throws Exception {
    RecordComponent[] components = CreateAuditEventRequest.class.getRecordComponents();
    Object[] args = new Object[components.length];
    Class<?>[] types = new Class<?>[components.length];
    for (int i = 0; i < components.length; i++) {
      types[i] = components[i].getType();
      args[i] =
          components[i].getName().equals(field) ? value : components[i].getAccessor().invoke(r);
    }
    return CreateAuditEventRequest.class.getDeclaredConstructor(types).newInstance(args);
  }
}
