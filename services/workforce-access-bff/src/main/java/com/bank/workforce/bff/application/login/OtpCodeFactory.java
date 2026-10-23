package com.bank.workforce.bff.application.login;

/** Supplies the six-digit OTP. Production generates it; tests may pin a value. */
public interface OtpCodeFactory {

  String nextCode();
}
