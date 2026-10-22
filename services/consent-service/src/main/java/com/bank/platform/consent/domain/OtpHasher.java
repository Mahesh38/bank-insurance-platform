package com.bank.platform.consent.domain;

public interface OtpHasher {

  String hash(String otp);

  boolean matches(String presented, String hash);

  String mintCode();
}
