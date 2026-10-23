package com.bank.platform.lead.adapter.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.platform.lead.domain.ExceptionCheckpoint;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.ExceptionRulesPort.Subject;
import com.bank.platform.lead.domain.ProductClass;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StubExceptionRulesTest {

  private final StubExceptionRules rules = new StubExceptionRules();

  @Test
  void syntheticTokensDriveTheOutcome() {
    assertThat(rules.evaluate(subject("cust-BLOCK-1")).outcome()).isEqualTo(ExceptionOutcome.BLOCK);
    assertThat(rules.evaluate(subject("cust-hold-1")).outcome())
        .isEqualTo(ExceptionOutcome.APPROVAL_REQUIRED);
    assertThat(rules.evaluate(subject("cust-1")).outcome()).isEqualTo(ExceptionOutcome.PASS);
    assertThat(rules.evaluate(subject("cust-1")).ruleSetRef()).isNull();
  }

  private static Subject subject(String customerId) {
    return new Subject(
        "01JQX4K7R8M2N3P4Q5S6T7V8W9",
        customerId,
        "LIFE",
        ProductClass.TERM,
        "BR-1",
        "rm-1",
        ExceptionCheckpoint.START_ONBOARDING,
        Instant.parse("2026-11-15T06:00:00Z"));
  }
}
