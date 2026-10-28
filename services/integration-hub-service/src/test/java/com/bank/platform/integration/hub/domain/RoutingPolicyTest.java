package com.bank.platform.integration.hub.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-032")
class RoutingPolicyTest {

  @Test
  void lifeTermSavingsUlip_routeToOneSb_AT_HUB_01() {
    assertThat(RoutingPolicy.route("LIFE", "TERM")).contains(RoutingPolicy.PROVIDER_ONE_SB);
    assertThat(RoutingPolicy.route("LIFE", "SAVINGS")).contains(RoutingPolicy.PROVIDER_ONE_SB);
    assertThat(RoutingPolicy.route("LIFE", "ULIP")).contains(RoutingPolicy.PROVIDER_ONE_SB);
  }

  @Test
  void health_noRoute_AT_HUB_01() {
    assertThat(RoutingPolicy.route("HEALTH", "TERM")).isEmpty();
  }

  @Test
  void adapterLob_mapsSavingsToSaving_AT_HUB_07() {
    assertThat(RoutingPolicy.adapterLob("LIFE", "SAVINGS")).contains("SAVING");
    assertThat(RoutingPolicy.adapterLob("LIFE", "TERM")).contains("TERM");
    assertThat(RoutingPolicy.adapterLob("LIFE", "ULIP")).contains("ULIP");
  }
}
