package com.bank.platform.proposal.domain;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Decrypted content of {@code payloadEnc}: form answers, nominee, UW requirements and document
 * references. Every form value is PII until proven otherwise, so all of it stays here (INV-PRP-05).
 */
public record ProposalEnvelope(
    Map<String, String> formValues,
    Nominee nominee,
    List<UwRequirement> requirements,
    List<DocumentRef> documents) {

  public static final int SCHEMA_VERSION = 1;

  public ProposalEnvelope {
    // Sorted, so the serialised envelope is stable for the same answers.
    formValues = formValues == null ? Map.of() : new TreeMap<>(formValues);
    requirements = requirements == null ? List.of() : List.copyOf(requirements);
    documents = documents == null ? List.of() : List.copyOf(documents);
  }

  public static ProposalEnvelope empty() {
    return new ProposalEnvelope(Map.of(), null, List.of(), List.of());
  }
}
