package com.bank.insurance.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.bank.insurance.persistence.persistence.repo")
public class PersistenceJpaConfig {
}
