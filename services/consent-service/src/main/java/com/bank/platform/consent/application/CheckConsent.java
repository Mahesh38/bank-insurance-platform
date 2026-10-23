package com.bank.platform.consent.application;

import com.bank.platform.consent.application.ConsentTypes.CheckView;
import com.bank.platform.consent.application.ConsentTypes.PurposeResult;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.ConsentRow;
import com.bank.platform.consent.domain.PolicySnapshot;
import com.bank.platform.consent.domain.StatementDoc;
import com.bank.rules.engine.Values;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Whether the purposes at a checkpoint are already held. */
@Service
public class CheckConsent {

  private final ConsentPorts.PolicySource policies;
  private final ConsentPorts.StatementSource statements;
  private final ConsentPorts.ConsentStore consents;
  private final Clock clock;

  public CheckConsent(
      ConsentPorts.PolicySource policies,
      ConsentPorts.StatementSource statements,
      ConsentPorts.ConsentStore consents,
      Clock clock) {
    this.policies = policies;
    this.statements = statements;
    this.consents = consents;
    this.clock = clock;
  }

  public CheckView check(
      String customerId, String checkpoint, String lob, String insurerCode, String journeyId) {
    ConsentInput.requireUlid("customerId", customerId);
    if (journeyId != null) {
      ConsentInput.requireUlid("journeyId", journeyId);
    }
    if (!ConsentInput.LOBS.contains(lob)) {
      throw new ConsentRefusal("VALIDATION_ERROR", "lob is not supported");
    }
    PolicySnapshot policy = policies.current();
    PolicySnapshot.CheckpointRule rule = policy.checkpoint(checkpoint);
    if (rule.namedInsurer() && (insurerCode == null || insurerCode.isBlank())) {
      throw new ConsentRefusal("MISSING_REQUIRED_FIELD", "insurer code is required");
    }
    Instant now = clock.instant();
    ObjectNode inputs = Values.mapper().createObjectNode();
    inputs.put("asOf", LocalDate.ofInstant(now, ZoneOffset.UTC).toString());
    inputs.put("checkpoint", checkpoint);
    List<PurposeResult> results = new ArrayList<>();
    boolean blocked = false;
    for (String purpose : rule.purposes()) {
      StatementDoc statement = statements.current(purpose, "en-IN");
      String scopeKey = rule.namedInsurer() ? insurerCode : null;
      ConsentRow head = consents.head(customerId, purpose, scopeKey).orElse(null);
      inputs.put(statusKey(purpose), statement.status());
      if (head != null && "GRANTED".equals(head.state()) && !expired(head, rule, now)) {
        inputs.put(
            grantedKey(purpose), LocalDate.ofInstant(head.validFrom(), ZoneOffset.UTC).toString());
      } else {
        inputs.putNull(grantedKey(purpose));
      }
      String reason = reasonFor(head, statement, policy, rule, insurerCode, now);
      if (reason == null) {
        results.add(PurposeResult.valid(head));
      } else {
        if ("STATEMENT_NOT_APPROVED".equals(reason)) {
          blocked = true;
        }
        results.add(PurposeResult.missing(purpose, reason, statement));
      }
    }
    Map<String, String> facts = new LinkedHashMap<>();
    inputs
        .fields()
        .forEachRemaining(
            entry ->
                facts.put(
                    entry.getKey(), entry.getValue().isNull() ? null : entry.getValue().asText()));
    if ("CAPTURE_BLOCKED".equals(policies.evaluateOutcome(facts, now))) {
      blocked = true;
    }
    List<PurposeResult> missing = results.stream().filter(PurposeResult::missing).toList();
    List<PurposeResult> valid = results.stream().filter(item -> !item.missing()).toList();
    return new CheckView(
        customerId,
        checkpoint,
        missing.isEmpty(),
        blocked,
        policy.ruleSetId(),
        policy.version(),
        valid,
        missing);
  }

  /** Drops the cached rule set after {@code RuleSetPublished}. */
  public void onRuleSetPublished() {
    policies.invalidate();
  }

  private static String reasonFor(
      ConsentRow head,
      StatementDoc statement,
      PolicySnapshot policy,
      PolicySnapshot.CheckpointRule rule,
      String insurerCode,
      Instant now) {
    if (!policy.requiredStatementStatus().equals(statement.status())) {
      return "STATEMENT_NOT_APPROVED";
    }
    if (head == null) {
      return "ABSENT";
    }
    if ("WITHDRAWN".equals(head.state())) {
      return "WITHDRAWN";
    }
    if ("SUPERSEDED".equals(head.state())) {
      return "SUPERSEDED";
    }
    if ("EXPIRED".equals(head.state())
        || ("GRANTED".equals(head.state()) && expired(head, rule, now))) {
      return "EXPIRED";
    }
    if (rule.namedInsurer() && (insurerCode == null || !insurerCode.equals(head.insurerCode()))) {
      return "SCOPE_MISMATCH";
    }
    if (statement.major() != major(head.statementVersion())) {
      return "STATEMENT_MAJOR_CHANGE";
    }
    if (!"GRANTED".equals(head.state())) {
      return "ABSENT";
    }
    return null;
  }

  private static boolean expired(ConsentRow head, PolicySnapshot.CheckpointRule rule, Instant now) {
    if (rule.validityDays() == null || head.validFrom() == null) {
      return false;
    }
    long days =
        ChronoUnit.DAYS.between(
            LocalDate.ofInstant(head.validFrom(), ZoneOffset.UTC),
            LocalDate.ofInstant(now, ZoneOffset.UTC));
    return days > rule.validityDays();
  }

  private static int major(String version) {
    int dot = version.indexOf('.');
    return Integer.parseInt(dot < 0 ? version : version.substring(0, dot));
  }

  private static String statusKey(String purpose) {
    return switch (purpose) {
      case "CNS-DP" -> "dpStatementStatus";
      case "CNS-SOL" -> "solStatementStatus";
      case "CNS-QSH" -> "qshStatementStatus";
      case "CNS-SHR" -> "shrStatementStatus";
      case "CNS-COM" -> "comStatementStatus";
      default -> throw new ConsentRefusal("VALIDATION_ERROR", "purpose is unknown");
    };
  }

  private static String grantedKey(String purpose) {
    return switch (purpose) {
      case "CNS-DP" -> "dpGrantedOn";
      case "CNS-SOL" -> "solGrantedOn";
      case "CNS-QSH" -> "qshGrantedOn";
      case "CNS-SHR" -> "shrGrantedOn";
      case "CNS-COM" -> "comGrantedOn";
      default -> throw new ConsentRefusal("VALIDATION_ERROR", "purpose is unknown");
    };
  }
}
