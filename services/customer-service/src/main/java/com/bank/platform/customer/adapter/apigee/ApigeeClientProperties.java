package com.bank.platform.customer.adapter.apigee;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("customer.apigee")
public record ApigeeClientProperties(
    String tokenMode, String tokenUrl, String clientId, String clientSecret) {

  URI tokenUri() {
    if (tokenUrl == null || tokenUrl.isBlank()) {
      throw new IllegalStateException("APIGEE_TOKEN_URL is required when token-mode=http");
    }
    return URI.create(tokenUrl);
  }
}
