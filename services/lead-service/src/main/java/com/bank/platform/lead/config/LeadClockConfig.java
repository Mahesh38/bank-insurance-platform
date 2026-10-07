package com.bank.platform.lead.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LeadClockConfig {

  @Bean
  Clock utcClock() {
    return Clock.systemUTC();
  }
}
