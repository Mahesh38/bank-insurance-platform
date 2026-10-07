package com.bank.platform.customer.adapter.cbs;

import com.bank.platform.customer.domain.CustomerInquiryPort;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Local/test CBS directory. Live Apigee → CBS inquiry is S09/S11 wiring ({@code ASM-015}, {@code
 * DEP-20260914-apg}); this stub lets the BFF complete the Lead search screen without fabricating a
 * customer the bank does not have.
 */
@Component
public class StubCustomerDirectory implements CustomerInquiryPort {

  @Override
  public List<CustomerHit> search(SearchQuery query, String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      return List.of();
    }
    if (query == null || query.value() == null || query.value().isBlank()) {
      return List.of();
    }
    String key = query.value().trim().toUpperCase(Locale.ROOT);
    if ("NONE".equals(key) || "0000000000".equals(key)) {
      return List.of();
    }
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
  public Optional<CustomerHit> findById(String customerId, String accessToken) {
    if (customerId == null || !customerId.toUpperCase(Locale.ROOT).startsWith("CUST-")) {
      return Optional.empty();
    }
    List<CustomerHit> hits = search(new SearchQuery("CUSTOMER_ID", customerId, null), accessToken);
    return hits.stream().findFirst();
  }
}
