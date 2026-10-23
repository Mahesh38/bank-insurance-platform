package com.bank.platform.quotation.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuotationClockConfig {

  @Bean
  Clock utcClock() {
    return Clock.systemUTC();
  }
}
