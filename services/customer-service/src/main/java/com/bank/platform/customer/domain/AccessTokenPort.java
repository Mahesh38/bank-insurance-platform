package com.bank.platform.customer.domain;

/**
 * Process-local Apigee access token. {@link #invalidate()} is mandatory — a default no-op let
 * lambdas skip remint after CBS 401.
 */
public interface AccessTokenPort {

  String currentAccessToken();

  void invalidate();
}
