package com.bank.platform.customer.adapter.apigee;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Tag("unit")
@Tag("FUNC-031")
class ApigeeClientConfigTest {

  @Test
  void timedFactoryIsConfigured() {
    JdkClientHttpRequestFactory factory = ApigeeClientConfig.timedFactory(3000, 30_000);
    assertThat(factory).isNotNull();
  }
}
