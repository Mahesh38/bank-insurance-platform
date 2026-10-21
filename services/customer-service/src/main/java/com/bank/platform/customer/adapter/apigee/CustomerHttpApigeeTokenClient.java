package com.bank.platform.customer.adapter.apigee;

import com.bank.common.apigee.ApigeeTokenClient;
import com.bank.common.apigee.HttpApigeeTokenClient;
import com.bank.common.apigee.IssuedToken;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "customer.apigee.token-mode", havingValue = "http")
public class CustomerHttpApigeeTokenClient implements ApigeeTokenClient {

  private final HttpApigeeTokenClient delegate;

  public CustomerHttpApigeeTokenClient(
      RestClient restClient, ApigeeClientProperties properties, Clock clock) {
    this.delegate =
        new HttpApigeeTokenClient(
            restClient,
            properties.tokenUri(),
            properties.clientId(),
            properties.clientSecret(),
            clock);
  }

  @Override
  public IssuedToken fetchClientCredentials() {
    return delegate.fetchClientCredentials();
  }
}
