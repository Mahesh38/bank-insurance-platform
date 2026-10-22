package com.bank.platform.lead.domain;

import java.util.List;
import java.util.Objects;

/**
 * Start Onboarding exception evaluation — rule set {@code LEAD.EXCEPTION} ({@code D2}, {@code
 * ADR-024}): {@code hitPolicy: PRIORITY}, {@code outcomeRank: [BLOCK, APPROVAL_REQUIRED, PASS]}.
 *
 * <p>One port so that swapping the temporary stub for the configuration-driven engine is one
 * adapter, not a change to the application service.
 */
public interface ExceptionRulesPort {

  Evaluation evaluate(Subject subject);

  /** What the rules are asked about. Facts behind EH-INT-001…005 are the adapter's to fetch. */
  record Subject(String leadId, String customerId, String lob, ProductClass productClass) {}

  /**
   * The ranked outcome and the reason codes that produced it; {@code ruleSetRef} identifies the
   * published rule-set version evaluated (null for the stub).
   */
  record Evaluation(ExceptionOutcome outcome, List<String> reasonCodes, String ruleSetRef) {
    public Evaluation {
      Objects.requireNonNull(outcome);
      reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
      if (outcome == ExceptionOutcome.NOT_EVALUATED) {
        throw new IllegalArgumentException("an evaluation always has an outcome");
      }
    }
  }
}
