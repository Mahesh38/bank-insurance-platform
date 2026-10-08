package com.bank.insurance.onesb.apigee;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-031")
class ApigeeTokenConfigTest {

  @Test
  void stubAndBlankModesAreAccepted() {
    assertThatCode(() -> ApigeeTokenConfig.rejectHttpModeUntilMapping("stub"))
        .doesNotThrowAnyException();
    assertThatCode(() -> ApigeeTokenConfig.rejectHttpModeUntilMapping(null))
        .doesNotThrowAnyException();
    assertThatCode(() -> ApigeeTokenConfig.rejectHttpModeUntilMapping(""))
        .doesNotThrowAnyException();
  }

  @Test
  void httpModeIsRefusedUntilBearerAttachmentExists() {
    assertThatThrownBy(() -> ApigeeTokenConfig.rejectHttpModeUntilMapping("http"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DEP-20260914-apg");
  }
}
