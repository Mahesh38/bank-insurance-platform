package com.bank.workforce.bff.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoginJourneyConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
