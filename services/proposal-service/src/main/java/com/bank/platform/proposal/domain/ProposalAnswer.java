package com.bank.platform.proposal.domain;

import java.time.Instant;

/**
 * One question asked and its answer, as shown to the customer (D8). {@code questionText} is copied
 * from the insurer form-schema version shown — never taken from the caller — and becomes {@code
 * proposal.proposal_answer.question_text}. Lives only inside the encrypted envelope (INV-PRP-05).
 *
 * @param questionText null only for answers read from an envelope v1, which never recorded it
 */
public record ProposalAnswer(
    String questionId,
    String sectionId,
    String questionText,
    String schemaVersion,
    String value,
    Instant answeredAt,
    String answeredBy) {

  /** An answer the structured record can hold: the question as shown is known. */
  public boolean isEvidenceComplete() {
    return questionText != null
        && !questionText.isBlank()
        && schemaVersion != null
        && !schemaVersion.isBlank();
  }
}
