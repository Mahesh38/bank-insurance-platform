package com.bank.platform.customer.api;

import com.bank.platform.customer.application.CustomerSearchService;
import com.bank.platform.customer.domain.CustomerInquiryPort.CustomerHit;
import jakarta.servlet.http.HttpServletRequest;
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
  private final ActorGuard actors;

  public CustomerSearchController(CustomerSearchService search, ActorGuard actors) {
    this.search = search;
    this.actors = actors;
  }

  // q is optional at binding so a missing value is the platform 422, not a framework 400.
  @GetMapping(path = "/search")
  public CustomerSearchPage search(
      HttpServletRequest request,
      @RequestHeader(name = ACTOR_HEADER, required = false) String actorId,
      @RequestParam(required = false) String by,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String countryCode) {
    actors.requireActorId(request, actorId, "search");
    List<CustomerHit> items = search.search(by, q, countryCode);
    return new CustomerSearchPage(items);
  }

  @GetMapping("/{customerId}")
  public CustomerHit get(
      HttpServletRequest request,
      @RequestHeader(name = ACTOR_HEADER, required = false) String actorId,
      @PathVariable String customerId) {
    actors.requireActorId(request, actorId, "get");
    return search.get(customerId);
  }

  public record CustomerSearchPage(List<CustomerHit> items) {}
}
