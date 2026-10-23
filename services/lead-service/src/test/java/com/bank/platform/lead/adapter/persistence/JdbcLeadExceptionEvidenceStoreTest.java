package com.bank.platform.lead.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.platform.lead.domain.ExceptionCheckpoint;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.ExceptionRulesPort.Evaluation;
import com.bank.platform.lead.domain.ExceptionRulesPort.FiredRule;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadExceptionEvidencePort.Decision;
import com.bank.platform.lead.domain.LeadExceptionEvidencePort.DecisionKind;
import com.bank.platform.lead.domain.LeadExceptionEvidencePort.StoredEvaluation;
import com.bank.platform.lead.domain.ProductClass;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class JdbcLeadExceptionEvidenceStoreTest {

  private static final Instant T0 = Instant.parse("2026-11-15T06:00:00Z");
  private static final String LEAD = "01JQX4K7R8M2N3P4Q5S6T7V8Z1";
  private static final String EVAL = "01JQX4K7R8M2N3P4Q5S6T7V8Z2";

  @Autowired JdbcLeadExceptionEvidenceStore store;
  @Autowired JdbcLeadRepository leads;
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
          "outbox_event"
        }) {
      jdbc.update("DELETE FROM lead_lms." + table);
    }
    jdbc.update("DELETE FROM lead_lms.lead");
    leads.save(new Lead(LEAD, "cust-1", "LIFE", ProductClass.TERM, "BR-1", "rm-1", T0));
  }

  @Test
  void evaluationAndDecisionRoundTripWithEncryptedRemarks() {
    Evaluation evaluation =
        new Evaluation(
            ExceptionOutcome.APPROVAL_REQUIRED,
            List.of("EH-ERR-RULE-001"),
            "LEAD.EXCEPTION",
            "1.1.0",
            "1.0.0",
            "sha256:abc",
            List.of(
                new FiredRule(
                    "EH-BR-RULE-001",
                    "APPROVAL_REQUIRED",
                    "EH-ERR-RULE-001",
                    List.of("BANK_ADMIN"),
                    List.of())),
            List.of("The Proposer's age exceeds the maximum limit of 60 years."),
            "{\"outcome\":\"APPROVAL_REQUIRED\"}");
    store.recordEvaluation(
        new StoredEvaluation(
            EVAL, LEAD, ExceptionCheckpoint.START_ONBOARDING, evaluation, "rm-1", T0));

    StoredEvaluation read = store.findEvaluation(EVAL).orElseThrow();
    assertThat(read.evaluation()).usingRecursiveComparison().isEqualTo(evaluation);
    assertThat(read.checkpoint()).isEqualTo(ExceptionCheckpoint.START_ONBOARDING);

    Decision decision =
        new Decision(
            "01JQX4K7R8M2N3P4Q5S6T7V8Z3",
            EVAL,
            LEAD,
            DecisionKind.APPROVED,
            "AUBIMA-1",
            "approved: long-standing customer",
            "approver-1",
            "BANK_ADMIN",
            T0.plusSeconds(60));
    store.recordDecision(decision);
    assertThat(store.decisionsOf(LEAD)).singleElement().isEqualTo(decision);
    assertThat(
            jdbc.queryForObject(
                "SELECT remarks_enc FROM lead_lms.lead_exception_decision", byte[].class))
        .asString()
        .doesNotContain("long-standing");

    // ux_lead_exc_decision_eval: one decision per evaluation.
    assertThatThrownBy(
            () ->
                store.recordDecision(
                    new Decision(
                        "01JQX4K7R8M2N3P4Q5S6T7V8Z4",
                        EVAL,
                        LEAD,
                        DecisionKind.REJECTED,
                        null,
                        "late",
                        "approver-2",
                        "BANK_ADMIN",
                        T0.plusSeconds(90))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
