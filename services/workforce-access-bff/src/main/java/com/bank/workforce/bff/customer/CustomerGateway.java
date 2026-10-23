package com.bank.workforce.bff.customer;

import java.util.List;

public interface CustomerGateway {

  /** {@code actorId} travels as {@code X-Actor-Id}; customer-service requires it (search audit). */
  List<CustomerHit> search(String by, String query, String countryCode, String actorId);

  CustomerHit get(String customerId, String actorId);

  record CustomerHit(
      String customerId,
      String displayName,
      String initials,
      String maskedCif,
      String maskedMobile,
      String eligibility,
      String source) {}
}
