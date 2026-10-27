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
            "local-lead-bff-internal-key",
            URI.create("http://customer.example.test"),
            "local-customer-bff-internal-key",
            "stub",
            "stub",
            "local-authz-bff-internal-key");
    assertThat(properties.toString())
        .contains("providerAdapterInternalKey=redacted")
        .contains("authorizationServiceInternalKey=redacted")
        .contains("leadServiceInternalKey=redacted")
        .contains("customerServiceInternalKey=redacted")
        .doesNotContain("local-idp-adapter-internal-key")
        .doesNotContain("local-authz-bff-internal-key");
  }
}
