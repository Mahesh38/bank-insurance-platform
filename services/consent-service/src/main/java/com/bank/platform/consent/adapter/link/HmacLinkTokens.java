package com.bank.platform.consent.adapter.link;

import com.bank.platform.consent.domain.LinkPorts;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 256-bit URL token. Only the HMAC is stored, so a database read does not reveal the link. */
public final class HmacLinkTokens implements LinkPorts.Tokens {

  private static final int TOKEN_BYTES = 32;
  private final byte[] pepper;
  private final SecureRandom random = new SecureRandom();

  public HmacLinkTokens(String pepper) {
    if (pepper == null || pepper.isBlank()) {
      throw new IllegalArgumentException("link pepper is required");
    }
    this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String mint() {
    byte[] raw = new byte[TOKEN_BYTES];
    random.nextBytes(raw);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
  }

  @Override
  public String hash(String token) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(token.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("link hash is unavailable");
    }
  }

  @Override
  public boolean wellFormed(String token) {
    if (token == null || !token.matches("^[A-Za-z0-9_-]{43}$")) {
      return false;
    }
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(token);
      return decoded.length == TOKEN_BYTES && MessageDigest.isEqual(decoded, decoded);
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }
}
