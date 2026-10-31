package com.bank.workforce.bff.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins allowed to call the BFF with credentials. Empty means CORS is off. The dev
 * profile lists {@code http://localhost:3000}. {@code prod} and {@code uat} refuse to start when
 * any origin is set. Wildcards are rejected in every profile.
 */
@ConfigurationProperties("workforce.cors")
public record WorkforceCorsProperties(List<String> allowedOrigins) {

  public WorkforceCorsProperties {
    allowedOrigins = explicitOrigins(allowedOrigins);
  }

  private static List<String> explicitOrigins(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    List<String> origins = new ArrayList<>();
    for (String origin : raw) {
      if (origin == null || origin.isBlank()) {
        continue;
      }
      origins.add(requireExplicitOrigin(origin));
    }
    return List.copyOf(origins);
  }

  private static String requireExplicitOrigin(String origin) {
    if (origin.contains("*")) {
      throw new IllegalArgumentException(
          "workforce.cors.allowed-origins rejects wildcards; list explicit origins");
    }
    URI uri;
    try {
      uri = URI.create(origin);
    } catch (IllegalArgumentException malformed) {
      throw new IllegalArgumentException(
          "workforce.cors.allowed-origins contains an invalid origin");
    }
    boolean http = "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
    boolean pathEmpty = uri.getPath() == null || uri.getPath().isEmpty();
    if (!http
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || !pathEmpty
        || uri.getQuery() != null
        || uri.getFragment() != null) {
      throw new IllegalArgumentException(
          "workforce.cors.allowed-origins accepts only explicit http(s) origins with no path");
    }
    return origin;
  }
}
