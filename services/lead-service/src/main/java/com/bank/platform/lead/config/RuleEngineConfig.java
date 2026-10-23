package com.bank.platform.lead.config;

import com.bank.platform.lead.adapter.rules.ClasspathSeedRuleSetResolver;
import com.bank.rules.client.CachingRuleSetResolver;
import com.bank.rules.client.RuleSetResolver;
import com.bank.rules.client.http.HttpRuleSetResolver;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Where rule sets come from (ADR-024 §5 point 4). {@code lead.rule-sets.source=http}: the published
 * versions from administration-config-service, L1-cached (TTL bounds staleness when a {@code
 * RuleSetPublished} event is lost). {@code classpath}: the unapproved seeds, for local runs and
 * tests only — {@code ProductionSafetyGuard} refuses it under prod / uat.
 */
@Configuration
public class RuleEngineConfig {

  @Bean
  @ConditionalOnProperty(name = "lead.rule-sets.source", havingValue = "http")
  RuleSetResolver httpRuleSetResolver(
      RestClient.Builder builder,
      @Value("${lead.rule-sets.base-url}") String baseUrl,
      @Value("${lead.rule-sets.timeout:300ms}") Duration timeout,
      @Value("${lead.rule-sets.cache-ttl:60s}") Duration cacheTtl) {
    return new CachingRuleSetResolver(new HttpRuleSetResolver(builder, baseUrl, timeout), cacheTtl);
  }

  @Bean
  @ConditionalOnProperty(
      name = "lead.rule-sets.source",
      havingValue = "classpath",
      matchIfMissing = true)
  RuleSetResolver classpathRuleSetResolver() {
    return new ClasspathSeedRuleSetResolver();
  }
}
