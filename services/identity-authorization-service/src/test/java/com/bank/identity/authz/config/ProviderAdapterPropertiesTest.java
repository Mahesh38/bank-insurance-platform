package com.bank.identity.authz.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("IAM-001")
@Tag("unit")
class ProviderAdapterPropertiesTest {

  @Test
  void toStringRedactsTheInternalKey() {
    ProviderAdapterProperties properties =
        new ProviderAdapterProperties(
            URI.create("http://adapter.example.test"), "local-idp-adapter-internal-key");
    assertThat(properties.toString())
        .isEqualTo(
            "ProviderAdapterProperties[baseUrl=http://adapter.example.test, internalKey=redacted]")
        .doesNotContain("local-idp-adapter-internal-key");
  }
}
