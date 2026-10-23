package com.bank.platform.consent.adapter.policy;

import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.StatementDoc;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Stand-in for administration-config statement storage. Tests flip a purpose to {@code DRAFT} to
 * prove an unapproved statement cannot be captured.
 */
@Component
public class FixtureStatementSource implements ConsentPorts.StatementSource {

  private static final java.util.List<String> PURPOSES =
      java.util.List.of("CNS-DP", "CNS-SOL", "CNS-QSH", "CNS-SHR", "CNS-COM");

  private final Map<String, StatementDoc> statements = new ConcurrentHashMap<>();

  public FixtureStatementSource() {
    for (String purpose : PURPOSES) {
      statements.put(purpose, approved(purpose));
    }
  }

  @Override
  public StatementDoc current(String purpose, String language) {
    if (!"en-IN".equals(language)) {
      throw new ConsentRefusal("VALIDATION_ERROR", "language is not supported");
    }
    StatementDoc doc = statements.get(purpose);
    if (doc == null) {
      throw new ConsentRefusal("CONFIGURATION_UNRESOLVABLE", "statement is not configured");
    }
    return doc;
  }

  @Override
  public void setStatus(String purpose, String status) {
    StatementDoc current = statements.get(purpose);
    if (current == null) {
      throw new ConsentRefusal("CONFIGURATION_UNRESOLVABLE", "statement is not configured");
    }
    statements.put(
        purpose,
        new StatementDoc(
            current.purpose(),
            current.statementId(),
            current.version(),
            current.text(),
            current.textHash(),
            status,
            current.language()));
  }

  private static StatementDoc approved(String purpose) {
    String text = "Approved " + purpose + " statement for the assisted journey.";
    String version = "1.0";
    return new StatementDoc(
        purpose, purpose + "-en-IN-v" + version, version, text, sha256(text), "APPROVED", "en-IN");
  }

  private static String sha256(String text) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 is required");
    }
  }
}
