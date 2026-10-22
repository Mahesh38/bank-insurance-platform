package com.bank.platform.lead.config;

import com.bank.platform.lead.application.LeadSettings;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LeadSettingsConfig {

  @Bean
  LeadSettings leadSettings(LeadProperties properties) {
    Integer days = properties.expiryDays();
    if (days == null) {
      return LeadSettings.defaults();
    }
    if (days <= 0) {
      throw new IllegalStateException("lead.expiry-days must be positive when set");
    }
    return new LeadSettings(Duration.ofDays(days));
  }
}
