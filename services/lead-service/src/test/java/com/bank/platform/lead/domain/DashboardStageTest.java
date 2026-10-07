package com.bank.platform.lead.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-030")
class DashboardStageTest {

  @Test
  void leadOwnsPreBiQuoteGeneratedClosedAndIssued() {
    assertThat(DashboardStage.fromLead(LeadState.NEW)).isEqualTo(DashboardStage.NEW);
    assertThat(DashboardStage.fromLead(LeadState.ASSIGNED)).isEqualTo(DashboardStage.NEW);
    assertThat(DashboardStage.fromLead(LeadState.CONTACTED)).isEqualTo(DashboardStage.NEW);
    assertThat(DashboardStage.fromLead(LeadState.QUALIFIED))
        .isEqualTo(DashboardStage.QUOTE_GENERATED);
    assertThat(DashboardStage.fromLead(LeadState.DISQUALIFIED)).isEqualTo(DashboardStage.CLOSED);
    assertThat(DashboardStage.fromLead(LeadState.CONVERTED))
        .isEqualTo(DashboardStage.POLICY_ISSUED);
    assertThat(DashboardStage.fromLead(LeadState.ARCHIVED)).isEqualTo(DashboardStage.POLICY_ISSUED);
  }

  @Test
  void leadDoesNotTransitionThroughUnderwritingQueue() {
    Lead lead =
        new Lead(
            "01JQX4K7R8M2N3P4Q5S6T7V8W9",
            "cust-1",
            "LIFE",
            ProductClass.TERM,
            "BR-1",
            "rm-1",
            Instant.parse("2026-10-05T00:00:00Z"));
    assertThat(lead.dashboardStage()).isEqualTo(DashboardStage.NEW);
    assertThat(lead.dashboardStage()).isNotEqualTo(DashboardStage.UNDERWRITING_QUEUE);
  }
}
