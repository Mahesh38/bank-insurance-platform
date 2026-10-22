package com.bank.platform.lead.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.platform.lead.domain.ConcurrentLeadUpdateException;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadState;
import com.bank.platform.lead.domain.ProductClass;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class JdbcLeadRepositoryTest {

  private static final Instant T0 = Instant.parse("2026-10-21T09:00:00Z");

  @Autowired JdbcLeadRepository repository;

  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    for (String table :
        new String[] {"lead_assignment", "lead_meeting", "lead_follow_up", "outbox_event"}) {
      jdbc.update("DELETE FROM lead_lms." + table);
    }
    jdbc.update("DELETE FROM lead_lms.lead");
  }

  @Test
  void savedLeadSurvivesARoundTripWithEveryField() {
    Lead lead = newLead("01JQX4K7R8M2N3P4Q5S6T7V8W9", "rm-1", "cust-1");
    repository.save(lead);
    assertThat(lead.version()).isEqualTo(1);

    lead.applyExceptionOutcome(ExceptionOutcome.PASS, T0.plusSeconds(60));
    lead.assign("SP-1001", "SP-2001", T0.plusSeconds(120));
    repository.save(lead);

    Lead read = repository.findById(lead.leadId()).orElseThrow();
    assertThat(read.snapshot()).usingRecursiveComparison().isEqualTo(lead.snapshot());
    assertThat(read.state()).isEqualTo(LeadState.ASSIGNED);
    assertThat(read.accountableSpId()).isEqualTo("SP-1001");
    assertThat(read.version()).isEqualTo(2);
  }

  @Test
  void bookIncludesCreatorAndAssigneesOnly() {
    Lead lead = newLead("01JQX4K7R8M2N3P4Q5S6T7V8X1", "rm-1", "cust-2");
    repository.save(lead);
    lead.applyExceptionOutcome(ExceptionOutcome.PASS, T0);
    lead.assign("SP-1001", null, T0.plusSeconds(1));
    repository.save(lead);

    assertThat(repository.findVisibleTo("rm-1")).hasSize(1);
    assertThat(repository.findVisibleTo("SP-1001")).hasSize(1);
    assertThat(repository.findVisibleTo("someone-else")).isEmpty();
  }

  @Test
  void unfinishedLookupIgnoresClosedLeads() {
    Lead open = newLead("01JQX4K7R8M2N3P4Q5S6T7V8X2", "rm-2", "cust-3");
    repository.save(open);
    assertThat(repository.findUnfinished("rm-2", "cust-3", ProductClass.TERM)).isPresent();

    open.close(T0.plusSeconds(5));
    repository.save(open);
    assertThat(repository.findUnfinished("rm-2", "cust-3", ProductClass.TERM)).isEmpty();
  }

  @Test
  void staleWriteIsRefusedInsteadOfOverwriting() {
    Lead lead = newLead("01JQX4K7R8M2N3P4Q5S6T7V8X3", "rm-3", "cust-4");
    repository.save(lead);

    Lead first = repository.findById(lead.leadId()).orElseThrow();
    Lead second = repository.findById(lead.leadId()).orElseThrow();
    first.applyExceptionOutcome(ExceptionOutcome.PASS, T0.plusSeconds(1));
    repository.save(first);

    second.applyExceptionOutcome(ExceptionOutcome.BLOCK, T0.plusSeconds(2));
    assertThatThrownBy(() -> repository.save(second))
        .isInstanceOf(ConcurrentLeadUpdateException.class);
    assertThat(repository.findById(lead.leadId()).orElseThrow().exceptionOutcome())
        .isEqualTo(ExceptionOutcome.PASS);
  }

  @Test
  void expiresAtRoundTripsAndMayBeAbsent() {
    Lead withExpiry = newLead("01JQX4K7R8M2N3P4Q5S6T7V8X4", "rm-4", "cust-5");
    withExpiry.expireAt(T0.plus(30, ChronoUnit.DAYS));
    repository.save(withExpiry);
    Lead without = newLead("01JQX4K7R8M2N3P4Q5S6T7V8X5", "rm-4", "cust-6");
    repository.save(without);

    assertThat(repository.findById(withExpiry.leadId()).orElseThrow().expiresAt())
        .isEqualTo(T0.plus(30, ChronoUnit.DAYS));
    assertThat(repository.findById(without.leadId()).orElseThrow().expiresAt()).isNull();
  }

  @Test
  void dedupeIndexRefusesASecondOpenLeadButNotAfterClose() {
    Lead first = newLead("01JQX4K7R8M2N3P4Q5S6T7V8X6", "rm-5", "cust-7");
    repository.save(first);

    Lead racing = newLead("01JQX4K7R8M2N3P4Q5S6T7V8X7", "rm-5", "cust-7");
    assertThatThrownBy(() -> repository.save(racing))
        .isInstanceOf(ConcurrentLeadUpdateException.class);

    first.close(T0.plusSeconds(1));
    repository.save(first);
    Lead afterClose = newLead("01JQX4K7R8M2N3P4Q5S6T7V8X8", "rm-5", "cust-7");
    repository.save(afterClose);
    assertThat(repository.findUnfinished("rm-5", "cust-7", ProductClass.TERM))
        .map(Lead::leadId)
        .contains(afterClose.leadId());
  }

  private static Lead newLead(String id, String creator, String customer) {
    return new Lead(
        id,
        customer,
        "LIFE",
        ProductClass.TERM,
        "BR-1",
        creator,
        T0.truncatedTo(ChronoUnit.MILLIS));
  }
}
