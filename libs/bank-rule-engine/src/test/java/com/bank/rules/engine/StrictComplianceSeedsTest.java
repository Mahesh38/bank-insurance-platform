package com.bank.rules.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The compliance-strict seed values of DECISIONS-2026-10-22 (C1-C7, E7 and the engineering
 * defaults) are what the seeds actually say. Each seed's own test cases prove its rules; these
 * assertions pin the policy values a service reads directly, so a seed edit that relaxes one is a
 * visible test change rather than a silent one.
 */
class StrictComplianceSeedsTest {

  private static RuleSet seed(String ruleSetId) {
    return RuleSetSeeds.all().stream()
        .map(s -> RuleSetParser.parse(s.json()))
        .filter(r -> r.ruleSetId().equals(ruleSetId))
        .findFirst()
        .orElseThrow(() -> new AssertionError(ruleSetId + " is not in rulesets/index.txt"));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> map(Object o) {
    return (Map<String, Object>) o;
  }

  @Test
  void consentPolicyTakesDataConsentsBeforeSuitability() {
    RuleSet rs = seed("CONSENT.POLICY");
    Map<String, Object> checkpoints = map(rs.parameters().get("checkpoints"));

    // C1: suitability collects income and dependants, so data and suitability consent come first.
    assertThat(map(checkpoints.get("BEFORE_SUITABILITY")).get("purposes"))
        .isEqualTo(List.of("CNS-DP", "CNS-SOL"));
    // The owner's flow at multi-quote: capture CNS-QSH and re-check that DP/SOL are still valid.
    Map<String, Object> mq = map(checkpoints.get("BEFORE_MULTI_QUOTE"));
    assertThat(mq.get("purposes")).isEqualTo(List.of("CNS-DP", "CNS-SOL", "CNS-QSH"));
    assertThat(mq.get("validityDays").toString()).isEqualTo("90");
    Map<String, Object> ps = map(checkpoints.get("BEFORE_PROPOSAL_SUBMIT"));
    assertThat(ps.get("purposes")).isEqualTo(List.of("CNS-SHR"));
    assertThat(ps.get("validityDays").toString()).isEqualTo("30");
    assertThat(ps.get("scope")).isEqualTo("NAMED_INSURER");
  }

  @Test
  void consentPolicyVerificationIsOnTheCustomersDeviceWithSeparateActions() {
    Map<String, Object> p = seed("CONSENT.POLICY").parameters();
    assertThat(p.get("verificationModes")).isEqualTo(List.of("CUSTOMER_DEVICE")); // C2
    assertThat(p.get("separateActions")).isEqualTo(Boolean.TRUE); // C3
    assertThat(p.get("requiredStatementStatus")).isEqualTo("APPROVED"); // C7
  }

  @Test
  void consentCaptureIsBlockedWhileTheStatementIsADraft() {
    RuleSet rs = seed("CONSENT.POLICY");
    ObjectNode in =
        Values.mapper()
            .createObjectNode()
            .put("asOf", "2026-11-01")
            .put("checkpoint", "BEFORE_SUITABILITY")
            .put("dpStatementStatus", "APPROVED")
            .put("solStatementStatus", "PENDING_APPROVAL");

    EvaluationResult r = RuleEngine.evaluate(rs, in);

    assertThat(r.outcome()).isEqualTo("CAPTURE_BLOCKED");
    assertThat(r.reasonCodes())
        .containsExactly("CAPTURE_CNS_DP", "CAPTURE_CNS_SOL", "CNS_SOL_STATEMENT_NOT_APPROVED");
  }

  @Test
  void suitabilityGateAcceptsCautionOnlyFromTheCustomersDevice() {
    RuleSet rs = seed("SUITABILITY.GATE");
    assertThat(rs.parameters().get("allowedOutcomes"))
        .isEqualTo(List.of("SUITABLE", "SUITABLE_WITH_CAUTION"));
    assertThat(rs.parameters().get("cautionAcknowledgementRequired")).isEqualTo(Boolean.TRUE);
    assertThat(rs.parameters().get("cautionAcknowledgementModes"))
        .isEqualTo(List.of("CUSTOMER_DEVICE"));

    ObjectNode in = cautionCase();
    in.put("cautionAcknowledged", true).put("cautionAcknowledgedVia", "RM_ASSISTED");
    assertThat(RuleEngine.evaluate(rs, in).outcome()).isEqualTo("DENY");
    in.put("cautionAcknowledgedVia", "CUSTOMER_DEVICE");
    assertThat(RuleEngine.evaluate(rs, in).outcome()).isEqualTo("ALLOW");
  }

  @Test
  void insufficientDataNeverQuotes() {
    ObjectNode in = cautionCase().put("outcomeForClass", "INSUFFICIENT_DATA");
    EvaluationResult r = RuleEngine.evaluate(seed("SUITABILITY.GATE"), in);
    assertThat(r.outcome()).isEqualTo("DENY"); // C6
    assertThat(r.reasonCodes()).containsExactly("SUITABILITY_INSUFFICIENT_DATA");
  }

  @Test
  void proposalTatIsSevenCalendarDaysFromReceiptPolledEveryFourHours() {
    Map<String, Object> p = seed("PROPOSAL.TAT").parameters();
    assertThat(p.get("decisionDays").toString()).isEqualTo("7");
    assertThat(p.get("basis")).isEqualTo("CALENDAR");
    assertThat(p.get("startEvent")).isEqualTo("PROPOSAL_RECEIVED");
    assertThat(p.get("pollIntervalHours").toString()).isEqualTo("4");
    assertThat(p.get("warningAtDays").toString()).isEqualTo("5");
  }

  @Test
  void proposalTatFailsClosedOnAnUnsupportedBasis() {
    RuleSet rs = seed("PROPOSAL.TAT");
    ObjectNode doc = (ObjectNode) rs.document().deepCopy();
    ((ObjectNode) doc.get("parameters")).put("basis", "BUSINESS");
    ObjectNode in =
        Values.mapper()
            .createObjectNode()
            .put("asOf", "2026-11-02")
            .put("clockStartedOn", "2026-11-01")
            .put("decisionCommunicated", false);

    EvaluationResult r = RuleEngine.evaluate(RuleSetParser.parse(doc), in);

    assertThat(r.outcome()).isEqualTo("BREACHED");
    assertThat(r.reasonCodes()).containsExactly("PROPOSAL_TAT_BASIS_UNSUPPORTED");
  }

  @Test
  void customerSnapshotFreshnessIsTwentyFourHoursAndFailsClosedWhenUnset() {
    RuleSet rs = seed("CUSTOMER.POLICY");
    EvaluationResult resolved = RuleEngine.evaluate(rs, Values.mapper().createObjectNode());
    assertThat(resolved.outcome()).isEqualTo("RESOLVED");
    assertThat(resolved.derived().get("snapshotFreshnessMinutes").toString()).isEqualTo("1440");

    for (var bad :
        List.of(Values.mapper().nullNode(), Values.mapper().getNodeFactory().numberNode(0))) {
      ObjectNode doc = (ObjectNode) rs.document().deepCopy();
      ((ObjectNode) doc.get("parameters")).set("snapshotFreshnessMinutes", bad);
      EvaluationResult broken =
          RuleEngine.evaluate(
              RuleSetParser.parse(doc),
              Values.mapper().createObjectNode().put("snapshotAgeMinutes", 10));
      assertThat(broken.outcome()).as(bad.toString()).isEqualTo("CONFIG_MISSING");
    }
  }

  private static ObjectNode cautionCase() {
    return Values.mapper()
        .createObjectNode()
        .put("asOf", "2026-11-20")
        .put("productClass", "TERM")
        .put("requestCustomerId", "01JCUST0000000000000000001")
        .put("evaluationId", "01JEVAL0000000000000000001")
        .put("evaluationFound", true)
        .put("evaluationStatus", "ACTIVE")
        .put("evaluatedAt", "2026-11-10")
        .put("evaluationCustomerId", "01JCUST0000000000000000001")
        .put("outcomeForClass", "SUITABLE_WITH_CAUTION");
  }
}
