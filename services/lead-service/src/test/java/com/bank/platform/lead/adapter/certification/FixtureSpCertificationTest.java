package com.bank.platform.lead.adapter.certification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-030")
class FixtureSpCertificationTest {

  private final FixtureSpCertification certification = new FixtureSpCertification();

  @Test
  void listedSpecifiedPersonsAreCertifiedAndUnknownIdsFailClosed() {
    assertThat(certification.holdsValidCertification("SP-1001")).isTrue();
    assertThat(certification.holdsValidCertification("SP-1002")).isTrue();
    assertThat(certification.holdsValidCertification("UNCERTIFIED")).isFalse();
    assertThat(certification.holdsValidCertification("sp-1")).isFalse();
    assertThat(certification.holdsValidCertification(null)).isFalse();
  }
}
