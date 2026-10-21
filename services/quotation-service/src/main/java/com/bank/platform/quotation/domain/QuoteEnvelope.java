package com.bank.platform.quotation.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Decrypted content of {@code payloadEnc} (envelope v1, payload-storage-and-transactions §3.2).
 * Members and offers never leave the envelope while the sale is open.
 */
public record QuoteEnvelope(
    BigDecimal requestedCover,
    String mode,
    String category,
    List<QuoteMember> members,
    List<QuoteOffer> offers,
    List<InsurerError> partialErrors,
    String selectedOfferId) {

  public static final int SCHEMA_VERSION = 1;

  public QuoteEnvelope {
    members = members == null ? List.of() : List.copyOf(members);
    offers = offers == null ? List.of() : List.copyOf(offers);
    partialErrors = partialErrors == null ? List.of() : List.copyOf(partialErrors);
  }
}
