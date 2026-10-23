package com.bank.platform.lead.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Exception evaluation — rule set {@code LEAD.EXCEPTION} ({@code D2}, {@code ADR-024}): {@code
 * hitPolicy: PRIORITY}, {@code outcomeRank: [BLOCK, APPROVAL_REQUIRED, PASS]}.
 *
 * <p>Implementations fail closed: a rule set that cannot be obtained is a {@link
 * RuleConfigurationException}, never a compiled-in default.
 */
public interface ExceptionRulesPort {

  Evaluation evaluate(Subject subject);

  /**
   * What the rules are asked about. {@code sellerPrincipalId} is the transaction employee
   * (EH-BR-RULE-014 and BRD §8.3); facts behind EH-INT-001…005 are the adapter's to fetch.
   */
  record Subject(
      String leadId,
      String customerId,
      String lob,
      ProductClass productClass,
      String branchId,
      String sellerPrincipalId,
      ExceptionCheckpoint checkpoint,
      Instant asOf) {

    public Subject {
      Objects.requireNonNull(leadId);
      Objects.requireNonNull(checkpoint);
      Objects.requireNonNull(asOf);
    }
  }

  /**
   * One fired rule as the evaluation recorded it. {@code approverRoles} are the bank roles the rule
   * set named for this rule at evaluation time (E6) — the approval endpoint checks these, not
   * whatever version is in force when the approver acts.
   */
  record FiredRule(
      String ruleId,
      String outcome,
      String reasonCode,
      List<String> approverRoles,
      List<String> configMissing) {

    public FiredRule {
      approverRoles = approverRoles == null ? List.of() : List.copyOf(approverRoles);
      configMissing = configMissing == null ? List.of() : List.copyOf(configMissing);
    }
  }

  /**
   * The ranked outcome and its evidence (ADR-024 §6). {@code ruleSetId} / {@code ruleSetVersion}
   * are null only for a test double; {@code evidenceJson} is the engine's canonical result.
   */
  record Evaluation(
      ExceptionOutcome outcome,
      List<String> reasonCodes,
      String ruleSetId,
      String ruleSetVersion,
      String engineVersion,
      String inputsHash,
      List<FiredRule> fired,
      List<String> messages,
      String evidenceJson) {

    public Evaluation {
      Objects.requireNonNull(outcome);
      if (outcome == ExceptionOutcome.NOT_EVALUATED) {
        throw new IllegalArgumentException("an evaluation always has an outcome");
      }
      reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
      fired = fired == null ? List.of() : List.copyOf(fired);
      messages = messages == null ? List.of() : List.copyOf(messages);
    }

    /** A bare outcome without engine evidence — test doubles only. */
    public Evaluation(ExceptionOutcome outcome, List<String> reasonCodes, String ruleSetRef) {
      this(outcome, reasonCodes, ruleSetRef, null, null, null, List.of(), List.of(), null);
    }

    /** {@code ruleSetId@version}, or null when no rule set decided (test double). */
    public String ruleSetRef() {
      if (ruleSetId == null) {
        return null;
      }
      return ruleSetVersion == null ? ruleSetId : ruleSetId + "@" + ruleSetVersion;
    }
  }
}
