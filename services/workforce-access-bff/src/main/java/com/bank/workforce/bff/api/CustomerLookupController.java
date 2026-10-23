package com.bank.workforce.bff.api;

import com.bank.workforce.bff.customer.CustomerGateway;
import com.bank.workforce.bff.customer.CustomerGateway.CustomerHit;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
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
    WorkforceSession session = BffSessionInterceptor.requireSession(request);
    List<CustomerHit> items = customers.search(by, q, countryCode, actorId(session));
    return new SearchPage(items);
  }

  @GetMapping("/customers/{customerId}")
  public CustomerHit get(HttpServletRequest request, @PathVariable String customerId) {
    WorkforceSession session = BffSessionInterceptor.requireSession(request);
    return customers.get(customerId, actorId(session));
  }

  // Same actor rule as the lead routes: the business user, falling back to the username.
  private static String actorId(WorkforceSession session) {
    UUID id = session.businessUserId();
    return id != null ? id.toString() : session.username();
  }

  public record SearchPage(List<CustomerHit> items) {}
}
