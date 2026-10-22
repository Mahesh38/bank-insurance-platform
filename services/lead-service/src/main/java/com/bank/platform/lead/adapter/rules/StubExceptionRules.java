package com.bank.platform.lead.adapter.rules;

import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.ExceptionRulesPort;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * TEMPORARY (D2, DECISIONS-2026-10-21): deterministic stand-in for rule set {@code LEAD.EXCEPTION}
 * until {@code libs/bank-rule-engine} is wired as the adapter behind {@link ExceptionRulesPort}.
 * Tokens are synthetic test customer ids, never logged. Refused under {@code prod} / {@code uat} by
 * {@code ProductionSafetyGuard}: it would pass every real customer.
 */
@Component
@ConditionalOnProperty(name = "lead.exception-rules", havingValue = "stub", matchIfMissing = true)
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
