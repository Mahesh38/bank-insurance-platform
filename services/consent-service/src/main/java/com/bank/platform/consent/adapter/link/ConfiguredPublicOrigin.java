package com.bank.platform.consent.adapter.link;

import com.bank.platform.consent.domain.LinkPorts;

/** Public origin of the customer consent page. The token is the only path segment. */
public final class ConfiguredPublicOrigin implements LinkPorts.Origin {

  private final String baseUrl;

  public ConfiguredPublicOrigin(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException("public base url is required");
    }
    String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.baseUrl = trimmed;
  }

  @Override
  public String url(String token) {
    return baseUrl + "/public/v1/consent-links/" + token;
  }
}
