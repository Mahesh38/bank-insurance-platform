package com.bank.platform.customer.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.customer.domain.AccessTokenPort;
import com.bank.platform.customer.domain.CustomerInquiryPort;
import com.bank.platform.customer.domain.CustomerInquiryPort.CustomerHit;
import com.bank.platform.customer.domain.CustomerInquiryPort.SearchQuery;
import com.bank.platform.customer.domain.CustomerInquiryUnauthorizedException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class CustomerSearchService {

  private static final Set<String> SEARCH_BY = Set.of("CUSTOMER_ID", "MOBILE", "PAN");

  private final CustomerInquiryPort inquiry;
  private final AccessTokenPort tokens;
  private final ServiceErrors errors;

  public CustomerSearchService(
      CustomerInquiryPort inquiry, AccessTokenPort tokens, ServiceErrors errors) {
    this.inquiry = inquiry;
    this.tokens = tokens;
    this.errors = errors;
  }

  public List<CustomerHit> search(String by, String value, String countryCode) {
    if (by == null || !SEARCH_BY.contains(by)) {
      throw errors
          .error(ErrorCodes.INVALID_REQUEST)
          .component("CustomerSearchService")
          .operation("search")
          .reason("by must be CUSTOMER_ID, MOBILE or PAN")
          .build();
    }
    if (value == null || value.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_REQUIRED_FIELD)
          .component("CustomerSearchService")
          .operation("search")
          .reason("q is required")
          .build();
    }
    return withToken(token -> inquiry.search(new SearchQuery(by, value, countryCode), token));
  }

  public CustomerHit get(String customerId) {
    if (customerId == null || customerId.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_REQUIRED_FIELD)
          .component("CustomerSearchService")
          .operation("get")
          .reason("customerId is required")
          .build();
    }
    return withToken(
        token ->
            inquiry
                .findById(customerId, token)
                .orElseThrow(
                    () ->
                        errors
                            .error(ErrorCodes.RESOURCE_NOT_FOUND)
                            .component("CustomerSearchService")
                            .operation("get")
                            .reason("customer is absent from this book")
                            .build()));
  }

  private <T> T withToken(Function<String, T> call) {
    try {
      return call.apply(tokens.currentAccessToken());
    } catch (CustomerInquiryUnauthorizedException unauthorized) {
      tokens.invalidate();
      return call.apply(tokens.currentAccessToken());
    }
  }
}
