package com.bank.workforce.bff.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("IAM-001")
@Tag("unit")
class DownstreamPropertiesTest {

  @Test
  void toStringRedactsTheAdapterInternalKey() {
    DownstreamProperties properties =
        new DownstreamProperties(
            URI.create("http://adapter.example.test"),
            "local-idp-adapter-internal-key",
            URI.create("http://authz.example.test"),
            URI.create("http://lead.example.test"),
            URI.create("http://customer.example.test"),
            "stub",
            "stub");
    assertThat(properties.toString())
        .contains("providerAdapterInternalKey=redacted")
        .doesNotContain("local-idp-adapter-internal-key");
  }
}
