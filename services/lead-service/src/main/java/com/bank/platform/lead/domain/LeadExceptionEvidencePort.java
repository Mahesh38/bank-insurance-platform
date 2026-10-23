package com.bank.platform.lead.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Append-only exception evidence ({@code lead_lms.lead_exception_evaluation} and {@code
 * lead_exception_decision}, D2 / ADR-024 §6). Rows are inserted, never updated or deleted; writes
 * join the caller's transaction.
 */
public interface LeadExceptionEvidencePort {

  void recordEvaluation(StoredEvaluation evaluation);

  Optional<StoredEvaluation> findEvaluation(String evaluationId);

  void recordDecision(Decision decision);

  List<Decision> decisionsOf(String leadId);

  record StoredEvaluation(
      String evaluationId,
      String leadId,
      ExceptionCheckpoint checkpoint,
      ExceptionRulesPort.Evaluation evaluation,
      String evaluatedBy,
      Instant evaluatedAt) {}

  enum DecisionKind {
    APPROVED,
    REJECTED
  }

  /** {@code remarks} are plaintext here; the store encrypts them ({@code remarks_enc}). */
  record Decision(
      String decisionId,
      String evaluationId,
      String leadId,
      DecisionKind decision,
      String decisionRef,
      String remarks,
      String decidedBy,
      String decidedByRole,
      Instant decidedAt) {}
}
