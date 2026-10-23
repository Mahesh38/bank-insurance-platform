package com.bank.platform.consent.domain;

import java.util.List;
import java.util.Map;

/**
 * The values consent reads directly from a resolved {@code CONSENT.POLICY} version. Checkpoint
 * names are the keys of {@code checkpoints}; nothing here hard-codes a checkpoint.
 */
public record PolicySnapshot(
    String ruleSetId,
    String version,
    Map<String, CheckpointRule> checkpoints,
    List<String> verificationModes,
    boolean separateActions,
    String requiredStatementStatus,
    int otpValidityMinutes,
    int maxAttempts,
    int maxIssuesPer24h,
    boolean reuseAcrossJourneys) {

  public String versionRef() {
    return ruleSetId + "@" + version;
  }

  public CheckpointRule checkpoint(String name) {
    CheckpointRule rule = checkpoints.get(name);
    if (rule == null) {
      throw new ConsentRefusal(
          "CONFIGURATION_UNRESOLVABLE", "checkpoint is not in the resolved consent policy");
    }
    return rule;
  }

  public record CheckpointRule(List<String> purposes, Integer validityDays, String scope) {

    public boolean namedInsurer() {
      return "NAMED_INSURER".equals(scope);
    }
  }
}
