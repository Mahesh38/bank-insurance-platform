package com.bank.platform.integration.hub.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HubClockConfig {

  @Bean
  Clock utcClock() {
    return Clock.systemUTC();
  }
}
