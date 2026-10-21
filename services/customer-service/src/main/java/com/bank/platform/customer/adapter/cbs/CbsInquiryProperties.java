package com.bank.platform.customer.adapter.cbs;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("customer.cbs")
public record CbsInquiryProperties(
    String inquiryMode, String baseUrl, String searchPath, String getPath) {

  URI baseUri() {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalStateException("CBS_INQUIRY_BASE_URL is required when inquiry-mode=http");
    }
    return URI.create(baseUrl);
  }
}
