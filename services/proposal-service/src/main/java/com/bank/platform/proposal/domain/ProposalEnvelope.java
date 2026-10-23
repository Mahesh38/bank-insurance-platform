package com.bank.platform.proposal.domain;

import java.util.List;

/**
 * Decrypted content of {@code payloadEnc}, envelope v2 (D8): every question asked with its text as
 * shown, schema version and answer; nominee; UW requirements; document references; the insurer's
 * counter-offer. Every answer is PII until proven otherwise, so all of it stays here (INV-PRP-05).
 * Envelope v1 (a {@code formValues} map without question text) is still read, see the codec.
 */
public record ProposalEnvelope(
    List<ProposalAnswer> answers,
    Nominee nominee,
    List<UwRequirement> requirements,
    List<DocumentRef> documents,
    CounterOffer counterOffer) {

  public static final int SCHEMA_VERSION = 2;

  public ProposalEnvelope {
    answers = answers == null ? List.of() : List.copyOf(answers);
    requirements = requirements == null ? List.of() : List.copyOf(requirements);
    documents = documents == null ? List.of() : List.copyOf(documents);
  }

  public static ProposalEnvelope empty() {
    return new ProposalEnvelope(List.of(), null, List.of(), List.of(), null);
  }
}
