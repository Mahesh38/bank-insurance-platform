package com.bank.platform.consent.domain;

import java.time.Instant;

/**
 * A published statement version. Text is frozen; a wording change mints a new version (CNS-R02).
 */
public record PublishedStatement(
    String statementId,
    String version,
    String text,
    String textHash,
    String language,
    ChangeClass changeClass,
    String classifiedBy,
    Instant publishedAt) {

  public PublishedStatement {
    if (statementId == null || statementId.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "statement id is required");
    }
    if (text == null || text.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "statement text is required");
    }
    if (classifiedBy == null || classifiedBy.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "classifiedBy is required");
    }
    String hash = StatementRules.sha256(text);
    if (textHash != null && !textHash.equals(hash)) {
      throw new ConsentRefusal(ConsentCodes.CONFLICT, "statement hash does not match the text");
    }
    textHash = hash;
    StatementRules.major(version);
  }
}
