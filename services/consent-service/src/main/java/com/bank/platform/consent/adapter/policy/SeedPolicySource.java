package com.bank.platform.consent.adapter.policy;

import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.PolicySnapshot;
import com.bank.rules.engine.RuleSet;
import com.bank.rules.engine.RuleSetParser;
import com.bank.rules.engine.RuleSetSeeds;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Resolves the seeded {@code CONSENT.POLICY}. A published replacement invalidates the cache. */
@Component
public class SeedPolicySource implements ConsentPorts.PolicySource {

  private final RuleSet ruleSet;
  private volatile PolicySnapshot snapshot;

  public SeedPolicySource() {
    String json =
        RuleSetSeeds.all().stream()
            .filter(seed -> seed.resource().endsWith("CONSENT.POLICY.json"))
            .findFirst()
            .orElseThrow()
            .json();
    this.ruleSet = RuleSetParser.parse(json);
    this.snapshot = snapshotOf(ruleSet);
  }

  public RuleSet ruleSet() {
    return ruleSet;
  }

  @Override
  public PolicySnapshot current() {
    return snapshot;
  }

  @Override
  public String evaluateOutcome(Map<String, String> inputs, java.time.Instant evaluatedAt) {
    com.fasterxml.jackson.databind.node.ObjectNode node =
        com.bank.rules.engine.Values.mapper().createObjectNode();
    inputs.forEach(
        (key, value) -> {
          if (value == null) {
            node.putNull(key);
          } else {
            node.put(key, value);
          }
        });
    return com.bank.rules.engine.RuleEngine.evaluate(ruleSet, node, evaluatedAt.toString())
        .outcome();
  }

  @Override
  public void invalidate() {
    snapshot = snapshotOf(ruleSet);
  }

  @SuppressWarnings("unchecked")
  private static PolicySnapshot snapshotOf(RuleSet ruleSet) {
    Map<String, Object> parameters = ruleSet.parameters();
    Map<String, Object> rawCheckpoints = (Map<String, Object>) parameters.get("checkpoints");
    Map<String, PolicySnapshot.CheckpointRule> checkpoints = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : rawCheckpoints.entrySet()) {
      Map<String, Object> body = (Map<String, Object>) entry.getValue();
      List<String> purposes =
          ((List<?>) body.get("purposes")).stream().map(String::valueOf).toList();
      Integer days =
          body.get("validityDays") == null ? null : ((Number) body.get("validityDays")).intValue();
      String scope = body.get("scope") == null ? "ALL_INSURERS" : String.valueOf(body.get("scope"));
      checkpoints.put(entry.getKey(), new PolicySnapshot.CheckpointRule(purposes, days, scope));
    }
    Map<String, Object> otp = (Map<String, Object>) parameters.get("otp");
    List<String> modes =
        ((List<?>) parameters.get("verificationModes")).stream().map(String::valueOf).toList();
    return new PolicySnapshot(
        ruleSet.ruleSetId(),
        ruleSet.version(),
        Map.copyOf(checkpoints),
        modes,
        Boolean.TRUE.equals(parameters.get("separateActions")),
        String.valueOf(parameters.get("requiredStatementStatus")),
        ((Number) otp.get("validityMinutes")).intValue(),
        ((Number) otp.get("maxAttempts")).intValue(),
        ((Number) otp.get("maxIssuesPer24h")).intValue(),
        Boolean.TRUE.equals(parameters.get("reuseAcrossJourneys")));
  }
}
