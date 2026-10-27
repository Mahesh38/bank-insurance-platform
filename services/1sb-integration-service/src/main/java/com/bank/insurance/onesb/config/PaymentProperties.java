package com.bank.insurance.onesb.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment-related settings ({@code insurance.payments.*}). 1SB does not return a payment-url
 * expiry, so the bank computes one from this TTL. Redirect URLs must match an allowed scheme+host
 * prefix (SUG-20261027-iah).
 */
@ConfigurationProperties(prefix = "insurance.payments")
public record PaymentProperties(long sessionTtlSeconds, List<String> allowedRedirectUriPrefixes) {
  public PaymentProperties {
    if (sessionTtlSeconds <= 0) {
      sessionTtlSeconds = 3600L;
    }
    allowedRedirectUriPrefixes =
        allowedRedirectUriPrefixes == null || allowedRedirectUriPrefixes.isEmpty()
            ? List.of("https://bank.example/", "https://bank.com/")
            : List.copyOf(allowedRedirectUriPrefixes);
  }

  public boolean isAllowedRedirectUrl(String redirectUrl) {
    if (redirectUrl == null || redirectUrl.isBlank()) {
      return false;
    }
    for (String prefix : allowedRedirectUriPrefixes) {
      if (prefix != null && !prefix.isBlank() && redirectUrl.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
