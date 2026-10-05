package com.bank.platform.customer.adapter.apigee;

@FunctionalInterface
public interface ApigeeTokenClient {

    ApigeeAccessTokenHolder.IssuedToken fetchClientCredentials();
}
