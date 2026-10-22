package com.bank.platform.consent.domain;

public record EvaluationDecision(
    boolean allowed,
    boolean journeyBlocked,
    String errorCode,
    JourneyEffect effect,
    String consentId,
    ConsentCode code) {

  public static EvaluationDecision allow(ConsentEvidence head, JourneyEffect effect) {
    return new EvaluationDecision(true, false, null, effect, head.rootConsentId(), head.code());
  }

  public static EvaluationDecision refuse(
      ConsentCode code,
      String errorCode,
      boolean journeyBlocked,
      JourneyEffect effect,
      String consentId) {
    return new EvaluationDecision(false, journeyBlocked, errorCode, effect, consentId, code);
  }
}
