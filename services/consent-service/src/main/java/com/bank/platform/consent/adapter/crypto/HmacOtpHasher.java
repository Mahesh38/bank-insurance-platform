package com.bank.platform.consent.adapter.crypto;

import com.bank.platform.consent.domain.OtpHasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA256 of the one-time code. The code itself is never stored. */
public final class HmacOtpHasher implements OtpHasher {

  private final byte[] pepper;
  private final SecureRandom random = new SecureRandom();

  public HmacOtpHasher(String pepper) {
    if (pepper == null || pepper.isBlank()) {
      throw new IllegalArgumentException("otp pepper is required");
    }
    this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String hash(String otp) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(otp.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("otp hash is unavailable");
    }
  }

  @Override
  public boolean matches(String presented, String stored) {
    if (presented == null || presented.isBlank() || stored == null || stored.isBlank()) {
      return false;
    }
    try {
      byte[] expected = HexFormat.of().parseHex(stored);
      byte[] actual = HexFormat.of().parseHex(hash(presented));
      return MessageDigest.isEqual(expected, actual);
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  @Override
  public String mintCode() {
    int value = random.nextInt(1_000_000);
    return String.format(Locale.ROOT, "%06d", value);
  }
}
