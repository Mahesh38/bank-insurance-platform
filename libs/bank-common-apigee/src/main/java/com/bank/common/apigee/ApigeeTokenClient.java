package com.bank.common.apigee;

@FunctionalInterface
public interface ApigeeTokenClient {

  IssuedToken fetchClientCredentials();
}
