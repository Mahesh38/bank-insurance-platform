package com.bank.insurance.onesb.apigee;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("apigee")
public record ApigeeTokenProperties(
    String tokenMode, String tokenUrl, String clientId, String clientSecret) {

  URI tokenUri() {
    if (tokenUrl == null || tokenUrl.isBlank()) {
      throw new IllegalStateException("APIGEE_TOKEN_URL is required when token-mode=http");
    }
    return URI.create(tokenUrl);
  }
}
