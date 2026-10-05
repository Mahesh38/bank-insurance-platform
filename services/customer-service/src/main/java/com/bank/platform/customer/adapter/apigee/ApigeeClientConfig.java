package com.bank.platform.customer.adapter.apigee;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

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
