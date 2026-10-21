package com.bank.platform.quotation.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Client-side envelope encryption of {@code payloadEnc}: {@code kms} or {@code local}. */
@ConfigurationProperties(prefix = "quotation.envelope")
public record EnvelopeCryptoProperties(
    String provider,
    String kmsKeyId,
    Duration dataKeyCacheTtl,
    String localKey,
    String localKeyId) {

  public EnvelopeCryptoProperties {
    provider = provider == null || provider.isBlank() ? "kms" : provider;
    dataKeyCacheTtl = dataKeyCacheTtl == null ? Duration.ofMinutes(5) : dataKeyCacheTtl;
    localKeyId = localKeyId == null || localKeyId.isBlank() ? "local" : localKeyId;
  }
}
