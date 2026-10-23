package com.bank.platform.customer.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.customer.application.CustomerSearchService;
import com.bank.platform.customer.domain.CustomerInquiryPort.CustomerHit;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/customers")
public class CustomerSearchController {

  static final String ACTOR_HEADER = "X-Actor-Id";

  private final CustomerSearchService search;
  private final ServiceErrors errors;

  public CustomerSearchController(CustomerSearchService search, ServiceErrors errors) {
    this.search = search;
    this.errors = errors;
  }

  // q is optional at binding so a missing value is the platform 422, not a framework 400.
  @GetMapping(path = "/search")
  public CustomerSearchPage search(
      @RequestHeader(name = ACTOR_HEADER, required = false) String actorId,
      @RequestParam(required = false) String by,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String countryCode) {
    requireActor(actorId, "search");
    List<CustomerHit> items = search.search(by, q, countryCode);
    return new CustomerSearchPage(items);
  }

  @GetMapping("/{customerId}")
  public CustomerHit get(
      @RequestHeader(name = ACTOR_HEADER, required = false) String actorId,
      @PathVariable String customerId) {
    requireActor(actorId, "get");
    return search.get(customerId);
  }

  /**
   * Every internal call names the acting business user (conventions/headers-session-and-auth.md
   * §2.2) so search can be audited per actor (AC-SEARCH-AUDIT-1).
   */
  private void requireActor(String actorId, String operation) {
    if (actorId == null || actorId.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_REQUIRED_FIELD)
          .component("CustomerSearchController")
          .operation(operation)
          .reason(ACTOR_HEADER + " header is required")
          .errors(
              List.of(
                  ServiceError.ofField(
                      ErrorCodes.MISSING_REQUIRED_FIELD, "header is required", ACTOR_HEADER)))
          .build();
    }
  }

  public record CustomerSearchPage(List<CustomerHit> items) {}
}
