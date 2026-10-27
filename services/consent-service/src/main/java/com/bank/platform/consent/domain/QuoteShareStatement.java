package com.bank.platform.consent.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Wording rendered for Suitability BRD section 9.10. The sentence is the BRD purpose, stored with
 * the grant so a later wording change cannot rewrite what this customer saw.
 */
public final class QuoteShareStatement {

  public static final String PURPOSE = "CNS-QSH";
  public static final String ID = "CNS-QSH-en-IN-brd-9.10";
  public static final String VERSION = "1.0";
  public static final String TEXT = "Consent for sharing of personal details.";
  public static final String ACK = "SECURE_LINK_CUSTOMER_DEVICE";
  public static final String CHECKPOINT = "BEFORE_MULTI_QUOTE";

  private QuoteShareStatement() {}

  public static String sha256() {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(TEXT.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 is required");
    }
  }
}
