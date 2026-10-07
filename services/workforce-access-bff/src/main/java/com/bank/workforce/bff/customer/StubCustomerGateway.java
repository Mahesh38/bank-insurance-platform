package com.bank.workforce.bff.customer;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "workforce.downstream.customer-mode",
    havingValue = "stub",
    matchIfMissing = true)
public class StubCustomerGateway implements CustomerGateway {

  private static final Set<String> SEARCH_BY = Set.of("CUSTOMER_ID", "MOBILE", "PAN");

  private final ServiceErrors errors;

  public StubCustomerGateway(ServiceErrors errors) {
    this.errors = errors;
  }

  @Override
  public List<CustomerHit> search(String by, String query, String countryCode) {
    if (by == null || !SEARCH_BY.contains(by)) {
      throw errors
          .error(ErrorCodes.INVALID_REQUEST)
          .component("StubCustomerGateway")
          .operation("search")
          .reason("by must be CUSTOMER_ID, MOBILE or PAN")
          .build();
    }
    if (query == null || query.isBlank() || "NONE".equalsIgnoreCase(query)) {
      return List.of();
    }
    String key = query.trim().toUpperCase(Locale.ROOT);
    String suffix = key.length() <= 4 ? key : key.substring(key.length() - 4);
    return List.of(
        new CustomerHit(
            "CUST-" + suffix,
            "A U Customer",
            "AU",
            "XXXXX" + suffix,
            "XXXXXX" + suffix,
            "ETB",
            "CBS"));
  }

  @Override
  public CustomerHit get(String customerId) {
    if (customerId == null || !customerId.toUpperCase(Locale.ROOT).startsWith("CUST-")) {
      throw errors
          .error(ErrorCodes.RESOURCE_NOT_FOUND)
          .component("StubCustomerGateway")
          .operation("get")
          .reason("customer is absent from this book")
          .build();
    }
    return search("CUSTOMER_ID", customerId, null).getFirst();
  }
}
