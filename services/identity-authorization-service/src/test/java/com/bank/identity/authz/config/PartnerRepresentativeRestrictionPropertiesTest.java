package com.bank.identity.authz.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** The strict seed (E5 / E6, E9) is what application.yml binds. */
@SpringBootTest
class PartnerRepresentativeRestrictionPropertiesTest {

  @Autowired PartnerRepresentativeRestrictionProperties properties;

  @Test
  void strictSeedIsBound() {
    assertThat(properties.prohibitedActionSuffixes()).containsExactly(".approve");
    assertThat(properties.prohibitedActions())
        .containsExactlyInAnyOrder("partner_user.create", "partner_user.bulk_import");
    assertThat(properties.toRestriction().prohibits("lead.exception.approve")).isTrue();
    assertThat(properties.toRestriction().prohibits("lead.read")).isFalse();
  }
}
