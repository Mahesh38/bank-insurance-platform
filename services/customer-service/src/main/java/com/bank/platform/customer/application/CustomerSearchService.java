package com.bank.platform.customer.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.customer.domain.AccessTokenPort;
import com.bank.platform.customer.domain.CustomerInquiryPort;
import com.bank.platform.customer.domain.CustomerInquiryPort.CustomerHit;
import com.bank.platform.customer.domain.CustomerInquiryPort.SearchQuery;
import com.bank.platform.customer.domain.CustomerInquiryUnauthorizedException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class CustomerSearchService {

  private static final Set<String> SEARCH_BY = Set.of("CUSTOMER_ID", "MOBILE", "PAN");
  private static final Pattern CIF = Pattern.compile("^[A-Za-z0-9]{1,20}$");
  private static final Pattern PAN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
  private static final Pattern MOBILE_IN = Pattern.compile("^[6-9]\\d{9}$");
  private static final Pattern MOBILE_E164 = Pattern.compile("^\\+?[1-9]\\d{6,14}$");

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
    String normalised = normalise(by, value.trim(), countryCode);
    return withToken(token -> inquiry.search(new SearchQuery(by, normalised, countryCode), token));
  }

  /**
   * Per-{@code by} format checks from the search contract (09 §3.1). A failure names the field but
   * never echoes {@code q} (SEC-C2). Returns the value CBS is asked for: PAN uppercased, mobile as
   * ten digits.
   */
  String normalise(String by, String q, String countryCode) {
    return switch (by) {
      case "CUSTOMER_ID" -> {
        // Contract 09 §3.1: CIF 1–20 alphanumeric. Not coerced from another shape.
        if (!CIF.matcher(q).matches()) {
          throw invalidQ("CUSTOMER_ID must be 1-20 letters or digits");
        }
        yield q;
      }
      case "PAN" -> {
        String pan = q.toUpperCase(Locale.ROOT);
        if (!PAN.matcher(pan).matches()) {
          throw invalidQ("PAN must match ^[A-Z]{5}[0-9]{4}[A-Z]$");
        }
        yield pan;
      }
      default -> {
        if (isIndia(countryCode)) {
          String digits = q.startsWith("+91") ? q.substring(3) : q;
          if (!MOBILE_IN.matcher(digits).matches()) {
            throw invalidQ("MOBILE must be 10 digits starting 6-9 (optionally +91)");
          }
          yield digits;
        }
        if (!MOBILE_E164.matcher(q).matches()) {
          throw invalidQ("MOBILE must be an E.164 number");
        }
        yield q;
      }
    };
  }

  private static boolean isIndia(String countryCode) {
    return countryCode == null
        || countryCode.isBlank()
        || "IN".equalsIgnoreCase(countryCode.trim())
        || "91".equals(countryCode.trim())
        || "+91".equals(countryCode.trim());
  }

  private ServiceException invalidQ(String rule) {
    return errors
        .error(ErrorCodes.VALIDATION_ERROR)
        .component("CustomerSearchService")
        .operation("search")
        .reason(rule)
        .errors(List.of(ServiceError.ofField(ErrorCodes.VALIDATION_ERROR, rule, "q")))
        .build();
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
      try {
        return call.apply(tokens.currentAccessToken());
      } catch (CustomerInquiryUnauthorizedException reminted) {
        throw errors
            .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
            .component("CustomerSearchService")
            .operation("withToken")
            .reason("CBS rejected the reminted access token")
            .build();
      }
    }
  }
}
