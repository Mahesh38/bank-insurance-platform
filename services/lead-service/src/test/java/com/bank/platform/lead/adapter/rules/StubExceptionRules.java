package com.bank.platform.lead.adapter.rules;

import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.ExceptionRulesPort;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * TEST ONLY (moved out of the service jar, SUG-20261022-rdy): deterministic stand-in for rule set
 * {@code LEAD.EXCEPTION}, selected with {@code lead.exception-rules=stub} in Spring tests that need
 * a PASS without configuring thresholds. Tokens are synthetic test customer ids. Production wiring
 * is {@code EngineExceptionRules}; {@code ProductionSafetyGuard} refuses anything else.
 */
@Component
@ConditionalOnProperty(name = "lead.exception-rules", havingValue = "stub")
public class StubExceptionRules implements ExceptionRulesPort {

  static final String STUB_REASON = "STUB-NOT-A-RULE";

  @Override
  public Evaluation evaluate(Subject subject) {
    String token =
        subject.customerId() == null ? "" : subject.customerId().toUpperCase(Locale.ROOT);
    if (token.contains("BLOCK")) {
      return new Evaluation(ExceptionOutcome.BLOCK, List.of(STUB_REASON), null);
    }
    if (token.contains("HOLD")) {
      return new Evaluation(ExceptionOutcome.APPROVAL_REQUIRED, List.of(STUB_REASON), null);
    }
    return new Evaluation(ExceptionOutcome.PASS, List.of(), null);
  }
}
