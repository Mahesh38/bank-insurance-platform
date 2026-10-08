package com.bank.workforce.bff.api;

import com.bank.workforce.bff.customer.CustomerGateway;
import com.bank.workforce.bff.customer.CustomerGateway.CustomerHit;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CustomerLookupController {

  private final CustomerGateway customers;

  public CustomerLookupController(CustomerGateway customers) {
    this.customers = customers;
  }

  @GetMapping("/customers:search")
  public SearchPage search(
      HttpServletRequest request,
      @RequestParam String by,
      @RequestParam String q,
      @RequestParam(required = false) String countryCode) {
    BffSessionInterceptor.requireSession(request);
    List<CustomerHit> items = customers.search(by, q, countryCode);
    return new SearchPage(items);
  }

  @GetMapping("/customers/{customerId}")
  public CustomerHit get(HttpServletRequest request, @PathVariable String customerId) {
    BffSessionInterceptor.requireSession(request);
    return customers.get(customerId);
  }

  public record SearchPage(List<CustomerHit> items) {}
}
