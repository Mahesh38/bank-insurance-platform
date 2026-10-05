package com.bank.platform.lead.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class LeadClockConfig {

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }
}
