package com.bank.platform.consent.domain;

import java.time.Instant;

/**
 * Precedence chain for "is this consent enough?". Order is fixed: subject, then head state, then
 * the type policy (scope, expiry, version). Type differences come from {@link ConsentTypePolicy}.
 */
public final class ConsentEvaluation {

  private final ConsentPolicyRegistry policies;

  public ConsentEvaluation(ConsentPolicyRegistry policies) {
    this.policies = policies;
  }

  public EvaluationDecision evaluate(EvaluationRequest request, ConsentEvidence head, Instant now) {
    ConsentTypePolicy policy = policies.require(request.code());
    if (head == null) {
      return EvaluationDecision.refuse(
          request.code(),
          ConsentCodes.CONSENT_REQUIRED,
          policy.absenceBlocksJourney(),
          policy.whenAbsent(),
          null);
    }
    if (!head.customerId().equals(request.customerId()) || head.code() != request.code()) {
      return EvaluationDecision.refuse(
          request.code(),
          ConsentCodes.CONSENT_SUBJECT_MISMATCH,
          true,
          JourneyEffect.HALT_JOURNEY,
          head.rootConsentId());
    }
    ConsentState effective = effectiveState(head, policy, now);
    if (effective == ConsentState.WITHDRAWN) {
      JourneyEffect effect = policy.whenWithdrawn(request.proposalAlreadySubmitted());
      String error =
          effect == JourneyEffect.SUPPRESS_OPTIONAL_COMMUNICATIONS
              ? ConsentCodes.CONSENT_REQUIRED
              : ConsentCodes.JOURNEY_HALTED_CONSENT_WITHDRAWN;
      return EvaluationDecision.refuse(
          request.code(), error, policy.absenceBlocksJourney(), effect, head.rootConsentId());
    }
    if (effective == ConsentState.EXPIRED || effective == ConsentState.ABANDONED) {
      return EvaluationDecision.refuse(
          request.code(),
          ConsentCodes.CONSENT_EXPIRED,
          policy.absenceBlocksJourney(),
          policy.whenAbsent(),
          head.rootConsentId());
    }
    if (effective == ConsentState.SUPERSEDED) {
      return EvaluationDecision.refuse(
          request.code(),
          ConsentCodes.CONSENT_REQUIRED,
          policy.absenceBlocksJourney(),
          policy.whenAbsent(),
          head.rootConsentId());
    }
    if (policy.requiresNamedCounterparty()
        && (request.counterpartyId() == null
            || request.counterpartyId().isBlank()
            || !request.counterpartyId().equals(head.counterpartyId()))) {
      return EvaluationDecision.refuse(
          request.code(),
          ConsentCodes.CONSENT_SCOPE_MISMATCH,
          true,
          JourneyEffect.BLOCK_STEP,
          head.rootConsentId());
    }
    if (request.currentStatementVersion() != null
        && !request.currentStatementVersion().isBlank()
        && StatementRules.isMajorUpgrade(head.statementVersion(), request.currentStatementVersion())
        && policy.majorVersionForcesReconsent(request.pastBlockingPoint())) {
      return EvaluationDecision.refuse(
          request.code(),
          ConsentCodes.CONSENT_REQUIRED,
          policy.absenceBlocksJourney(),
          policy.whenAbsent(),
          head.rootConsentId());
    }
    return EvaluationDecision.allow(head, JourneyEffect.PROCEED);
  }

  /** Head status, with expiry derived so the stored row is never rewritten. */
  public ConsentState effectiveState(ConsentEvidence head, ConsentTypePolicy policy, Instant now) {
    if (head.recordedState() == ConsentState.WITHDRAWN) {
      return ConsentState.WITHDRAWN;
    }
    if (head.recordedState() == ConsentState.GRANTED
        && policy.validity().isPresent()
        && head.validUntil() != null
        && now.isAfter(head.validUntil())) {
      return ConsentState.EXPIRED;
    }
    return head.recordedState();
  }
}
