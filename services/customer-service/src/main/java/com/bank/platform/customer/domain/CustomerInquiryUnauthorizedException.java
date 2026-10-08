package com.bank.platform.customer.domain;

/** CBS / Apigee rejected the current access token. Callers may invalidate and retry once. */
public final class CustomerInquiryUnauthorizedException extends RuntimeException {

  public CustomerInquiryUnauthorizedException() {
    super("CBS inquiry rejected the access token");
  }
}
