package com.bank.workforce.bff.customer;

import java.util.List;

public interface CustomerGateway {

  List<CustomerHit> search(String by, String query, String countryCode);

  CustomerHit get(String customerId);

  record CustomerHit(
      String customerId,
      String displayName,
      String initials,
      String maskedCif,
      String maskedMobile,
      String eligibility,
      String source) {}
}
