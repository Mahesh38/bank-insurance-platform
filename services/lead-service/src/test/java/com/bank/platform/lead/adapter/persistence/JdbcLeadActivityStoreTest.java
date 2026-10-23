package com.bank.platform.lead.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.platform.lead.adapter.crypto.FieldCipher;
import com.bank.platform.lead.domain.ConcurrentLeadUpdateException;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadActivityPort.Assignment;
import com.bank.platform.lead.domain.LeadActivityPort.AssignmentKind;
import com.bank.platform.lead.domain.LeadActivityPort.FollowUp;
import com.bank.platform.lead.domain.LeadActivityPort.LeadEvent;
import com.bank.platform.lead.domain.LeadActivityPort.Meeting;
import com.bank.platform.lead.domain.LeadActivityPort.MeetingType;
import com.bank.platform.lead.domain.LeadIdempotencyPort.IdempotencyRecord;
import com.bank.platform.lead.domain.ProductClass;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class JdbcLeadActivityStoreTest {

  private static final Instant T0 = Instant.parse("2026-10-21T09:00:00Z");
  private static final String LEAD = "01JQX4K7R8M2N3P4Q5S6T7V8X1";

  @Autowired JdbcLeadRepository leads;
  @Autowired JdbcLeadActivityStore activity;
  @Autowired JdbcLeadIdempotencyStore idempotency;
  @Autowired FieldCipher cipher;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    for (String table :
        new String[] {
          "lead_exception_decision",
          "lead_exception_evaluation",
          "lead_assignment",
          "lead_meeting",
          "lead_follow_up",
          "outbox_event",
          "idempotency_record"
        }) {
      jdbc.update("DELETE FROM lead_lms." + table);
    }
    jdbc.update("DELETE FROM lead_lms.lead");
    leads.save(new Lead(LEAD, "cust-1", "LIFE", ProductClass.TERM, "BR-1", "rm-1", T0));
  }

  @Test
  void historyAndOutboxRoundTrip() {
    activity.recordAssignment(
        new Assignment(
            "01JQX4K7R8M2N3P4Q5S6T7V8X2",
            LEAD,
            "SP-1",
            null,
            T0,
            "rm-1",
            AssignmentKind.CREATE_ASSIGN));
    activity.append(
        new LeadEvent(
            "01JQX4K7R8M2N3P4Q5S6T7V8X3", LEAD, "LeadAssigned", Map.of("state", "ASSIGNED"), T0));

    assertThat(activity.assignmentsOf(LEAD))
        .singleElement()
        .extracting(Assignment::rmId)
        .isEqualTo("SP-1");
    LeadEvent event = activity.eventsOf(LEAD).get(0);
    assertThat(event.attributes()).containsEntry("state", "ASSIGNED");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM lead_lms.outbox_event WHERE published_at IS NULL",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void noteAndMeetingLinkAreEncryptedAtRest() {
    activity.recordFollowUp(
        new FollowUp("01JQX4K7R8M2N3P4Q5S6T7V8X4", LEAD, "call after six", "rm-1", T0));
    activity.recordMeeting(
        new Meeting(
            "01JQX4K7R8M2N3P4Q5S6T7V8X5",
            LEAD,
            MeetingType.ONLINE,
            T0.plusSeconds(86_400),
            "https://meet.example/secret-room",
            "rm-1",
            T0));

    byte[] note = jdbc.queryForObject("SELECT note_enc FROM lead_lms.lead_follow_up", byte[].class);
    byte[] link =
        jdbc.queryForObject("SELECT meeting_link_enc FROM lead_lms.lead_meeting", byte[].class);
    assertThat(new String(note, StandardCharsets.ISO_8859_1)).doesNotContain("call after six");
    assertThat(new String(link, StandardCharsets.ISO_8859_1)).doesNotContain("secret-room");
    assertThat(cipher.decrypt(note)).isEqualTo("call after six");
    assertThat(cipher.decrypt(link)).isEqualTo("https://meet.example/secret-room");
  }

  @Test
  void aLiveIdempotencyKeyCannotBeClaimedTwice() {
    var record = new IdempotencyRecord("idem-key-1", "hash", LEAD, T0, T0.plusSeconds(3600));
    idempotency.save(record);

    assertThat(idempotency.find("idem-key-1", T0.plusSeconds(10))).isPresent();
    assertThat(idempotency.find("idem-key-1", T0.plusSeconds(7200))).isEmpty();
    assertThatThrownBy(() -> idempotency.save(record))
        .isInstanceOf(ConcurrentLeadUpdateException.class);
    // Once expired, the key may be used again.
    idempotency.save(
        new IdempotencyRecord(
            "idem-key-1", "hash2", LEAD, T0.plusSeconds(7200), T0.plusSeconds(9000)));
    assertThat(idempotency.find("idem-key-1", T0.plusSeconds(7300)).orElseThrow().requestHash())
        .isEqualTo("hash2");
  }
}
