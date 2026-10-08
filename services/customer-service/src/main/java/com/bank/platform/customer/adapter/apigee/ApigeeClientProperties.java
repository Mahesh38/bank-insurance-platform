package com.bank.platform.customer.adapter.apigee;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("customer.apigee")
public record ApigeeClientProperties(
    String tokenMode,
    String tokenUrl,
    String clientId,
    String clientSecret,
    @DefaultValue("3000") int connectTimeoutMs,
    @DefaultValue("30000") int readTimeoutMs) {

  URI tokenUri() {
    if (tokenUrl == null || tokenUrl.isBlank()) {
      throw new IllegalStateException("APIGEE_TOKEN_URL is required when token-mode=http");
    }
    return URI.create(tokenUrl);
  }
}
