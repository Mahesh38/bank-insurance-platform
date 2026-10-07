package com.bank.platform.customer.domain;

public interface AccessTokenPort {

  String currentAccessToken();

  default void invalidate() {}
}
