package com.bank.platform.customer.adapter.apigee;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApigeeClientConfig {

  @Bean
  Clock utcClock() {
    return Clock.systemUTC();
  }

  @Bean
  ApigeeAccessTokenHolder apigeeAccessTokenHolder(ApigeeTokenClient client, Clock clock) {
    return new ApigeeAccessTokenHolder(client, clock);
  }
}
