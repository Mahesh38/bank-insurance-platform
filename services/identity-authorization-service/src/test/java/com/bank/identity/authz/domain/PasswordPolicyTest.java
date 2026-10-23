package com.bank.identity.authz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.identity.authz.domain.PasswordPolicy.Violation;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

  @Test
  void acceptsAPasswordThatMeetsTheDesignChecklist() {
    assertThat(PasswordPolicy.check("Horse#99", "Horse#99", "rm@aubank.in", false))
        .isEqualTo(Violation.OK);
  }

  @Test
  void rejectsTheDesignFailures() {
    assertThat(PasswordPolicy.check("Horse#99", "Horse#98", "rm@aubank.in", false))
        .isEqualTo(Violation.MISMATCH);
    assertThat(PasswordPolicy.check("Ho#1", "Ho#1", "rm@aubank.in", false))
        .isEqualTo(Violation.LENGTH);
    assertThat(PasswordPolicy.check("horse#99", "horse#99", "rm@aubank.in", false))
        .isEqualTo(Violation.UPPER);
    assertThat(PasswordPolicy.check("HORSE#99", "HORSE#99", "rm@aubank.in", false))
        .isEqualTo(Violation.LOWER);
    assertThat(PasswordPolicy.check("Horse#xx", "Horse#xx", "rm@aubank.in", false))
        .isEqualTo(Violation.NUMBER);
    assertThat(PasswordPolicy.check("Horse99x", "Horse99x", "rm@aubank.in", false))
        .isEqualTo(Violation.SPECIAL);
    assertThat(PasswordPolicy.check("Horse #99", "Horse #99", "rm@aubank.in", false))
        .isEqualTo(Violation.SPACE);
    assertThat(PasswordPolicy.check("123456", "123456", "rm@aubank.in", false))
        .isEqualTo(Violation.DEFAULT);
    assertThat(PasswordPolicy.check("rm@aubank.in1A", "rm@aubank.in1A", "rm@aubank.in", false))
        .isEqualTo(Violation.EMAIL);
    assertThat(PasswordPolicy.check("Horse#99", "Horse#99", "rm@aubank.in", true))
        .isEqualTo(Violation.PREVIOUS);
  }
}
