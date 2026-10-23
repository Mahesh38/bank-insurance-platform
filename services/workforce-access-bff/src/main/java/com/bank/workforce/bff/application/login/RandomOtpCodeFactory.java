package com.bank.workforce.bff.application.login;

import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RandomOtpCodeFactory implements OtpCodeFactory {

  private final SecureRandom random = new SecureRandom();
  private final String fixed;

  public RandomOtpCodeFactory(@Value("${workforce.login.otp-code:}") String fixed) {
    this.fixed = fixed == null || fixed.isBlank() ? null : fixed;
  }

  @Override
  public String nextCode() {
    if (fixed != null) {
      return fixed;
    }
    return String.format("%06d", random.nextInt(1_000_000));
  }
}
