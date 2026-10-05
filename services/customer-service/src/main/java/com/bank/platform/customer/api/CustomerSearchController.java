package com.bank.platform.customer.api;

import com.bank.platform.customer.application.CustomerSearchService;
import com.bank.platform.customer.domain.CustomerInquiryPort.CustomerHit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/v1/customers")
public class CustomerSearchController {

    private final CustomerSearchService search;

    public CustomerSearchController(CustomerSearchService search) {
        this.search = search;
    }

    @GetMapping(path = "/search")
    public CustomerSearchPage search(
        @RequestParam String by,
        @RequestParam String q,
        @RequestParam(required = false) String countryCode
    ) {
        List<CustomerHit> items = search.search(by, q, countryCode);
        return new CustomerSearchPage(items);
    }

    @GetMapping("/{customerId}")
    public CustomerHit get(@PathVariable String customerId) {
        return search.get(customerId);
    }

    public record CustomerSearchPage(List<CustomerHit> items) {}
}
