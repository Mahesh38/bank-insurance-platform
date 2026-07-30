package com.bank.insurance.onesb.adapter.onesb.polling;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated executor for 1SB poll loops (architecture §7.4) — separate from Tomcat threads.
 */
@Configuration
@EnableConfigurationProperties(PollingProperties.class)
public class PollingConfig {

    @Bean(name = "pollingExecutor")
    Executor pollingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("polling-");
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.initialize();
        return executor;
    }
}
