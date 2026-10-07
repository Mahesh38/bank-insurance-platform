package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.customer.CustomerGateway;
import com.bank.workforce.bff.customer.CustomerGateway.CustomerHit;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CustomerLookupController {

  private static final Set<String> SEARCH_BY = Set.of("CUSTOMER_ID", "MOBILE", "PAN");

  private final CustomerGateway customers;
  private final ServiceErrors errors;

  public CustomerLookupController(CustomerGateway customers, ServiceErrors errors) {
    this.customers = customers;
    this.errors = errors;
  }

  @GetMapping("/customers:search")
  public SearchPage search(
      HttpServletRequest request,
      @RequestParam String by,
      @RequestParam String q,
      @RequestParam(required = false) String countryCode) {
    BffSessionInterceptor.requireSession(request);
    if (!SEARCH_BY.contains(by)) {
      throw errors
          .error(ErrorCodes.INVALID_REQUEST)
          .component("CustomerLookupController")
          .operation("search")
          .reason("by must be CUSTOMER_ID, MOBILE or PAN")
          .build();
    }
    List<CustomerHit> items = customers.search(by, q, countryCode);
    return new SearchPage(items);
  }

  @GetMapping("/customers/{customerId}")
  public CustomerHit get(HttpServletRequest request, @PathVariable String customerId) {
    BffSessionInterceptor.requireSession(request);
    return customers.get(customerId);
  }

  @GetMapping("/catalogue/product-classes")
  public ProductClassPage productClasses(HttpServletRequest request) {
    BffSessionInterceptor.requireSession(request);
    return new ProductClassPage(
        List.of(
            new ProductClass("LIFE", "TERM", "Term Life"),
            new ProductClass("LIFE", "SAVINGS", "Savings / ULIP-adjacent savings"),
            new ProductClass("LIFE", "ULIP", "ULIP")));
  }

  public record SearchPage(List<CustomerHit> items) {}

  public record ProductClassPage(List<ProductClass> items) {}

  public record ProductClass(String lob, String productClass, String name) {}
}
