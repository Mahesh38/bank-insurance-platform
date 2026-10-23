package com.bank.identity.authz.config;

import com.bank.identity.authz.domain.AuthorizationModels.PartnerActionRestriction;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * What an insurer partner representative can never be granted (DECISIONS-2026-10-22 E5 / E6). The
 * lists are configuration so Security can extend them without code; the seed in {@code
 * application.yml} is the strict default (E9).
 */
@Validated
@ConfigurationProperties("identity.authorization.partner-representative")
public record PartnerRepresentativeRestrictionProperties(
    @NotNull Set<String> prohibitedActions, @NotNull List<String> prohibitedActionSuffixes) {

  public PartnerActionRestriction toRestriction() {
    return new PartnerActionRestriction(prohibitedActions, prohibitedActionSuffixes);
  }
}
