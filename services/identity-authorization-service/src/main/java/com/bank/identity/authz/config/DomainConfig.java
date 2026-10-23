package com.bank.identity.authz.config;

import com.bank.identity.authz.domain.AuthorizationPolicyEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the pure-Java domain so the domain package carries no Spring annotation (ArchUnit). */
@Configuration
public class DomainConfig {

  @Bean
  AuthorizationPolicyEvaluator authorizationPolicyEvaluator(
      PartnerRepresentativeRestrictionProperties partnerRestriction) {
    return new AuthorizationPolicyEvaluator(partnerRestriction.toRestriction());
  }
}
