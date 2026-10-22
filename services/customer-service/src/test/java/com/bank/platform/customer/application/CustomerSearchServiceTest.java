package com.bank.platform.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.customer.adapter.cbs.StubCustomerDirectory;
import com.bank.platform.customer.domain.AccessTokenPort;
import com.bank.platform.customer.domain.CustomerInquiryPort;
import com.bank.platform.customer.domain.CustomerInquiryPort.CustomerHit;
import com.bank.platform.customer.domain.CustomerInquiryPort.SearchQuery;
import com.bank.platform.customer.domain.CustomerInquiryUnauthorizedException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-031")
class CustomerSearchServiceTest {

  private final CustomerSearchService service =
      new CustomerSearchService(
          new StubCustomerDirectory(),
          stubTokens("access-token"),
          ServiceErrors.of("customer", PlatformLayer.L5));

  @Test
  void mobileSearchReturnsMaskedEtbHit() {
    List<CustomerHit> hits = service.search("MOBILE", "9876543210", "IN");

    assertThat(hits).hasSize(1);
    assertThat(hits.get(0).maskedMobile()).startsWith("XXXXXX");
    assertThat(hits.get(0).eligibility()).isEqualTo("ETB");
    assertThat(hits.get(0).source()).isEqualTo("CBS");
  }

  @Test
  void rejectsUnknownSearchMode() {
    assertThatThrownBy(() -> service.search("NAME", "Anand", null))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.INVALID_REQUEST);
  }

  @Test
  void getByIdReturnsMaskedEtbHit() {
    CustomerHit hit = service.get("CUST-3210");

    assertThat(hit.customerId()).isEqualTo("CUST-3210");
    assertThat(hit.eligibility()).isEqualTo("ETB");
  }

  @Test
  void getUnknownIdIsAbsent() {
    assertThatThrownBy(() -> service.get("UNKNOWN"))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
  }

  @Test
  void unauthorizedInquiryInvalidatesAndRetriesOnce() {
    AtomicInteger calls = new AtomicInteger();
    AtomicInteger invalidations = new AtomicInteger();
    CustomerInquiryPort inquiry =
        new CustomerInquiryPort() {
          @Override
          public List<CustomerHit> search(SearchQuery query, String accessToken) {
            if (calls.incrementAndGet() == 1) {
              throw new CustomerInquiryUnauthorizedException();
            }
            return new StubCustomerDirectory().search(query, accessToken);
          }

          @Override
          public java.util.Optional<CustomerHit> findById(String customerId, String accessToken) {
            return java.util.Optional.empty();
          }
        };
    AccessTokenPort tokens =
        new AccessTokenPort() {
          @Override
          public String currentAccessToken() {
            return "access-token";
          }

          @Override
          public void invalidate() {
            invalidations.incrementAndGet();
          }
        };
    CustomerSearchService retrying =
        new CustomerSearchService(inquiry, tokens, ServiceErrors.of("customer", PlatformLayer.L5));

    List<CustomerHit> hits = retrying.search("MOBILE", "9876543210", "IN");

    assertThat(hits).hasSize(1);
    assertThat(calls.get()).isEqualTo(2);
    assertThat(invalidations.get()).isEqualTo(1);
  }

  @Test
  void persistentUnauthorizedAfterRetryIsUpstreamUnavailable() {
    CustomerInquiryPort inquiry =
        new CustomerInquiryPort() {
          @Override
          public List<CustomerHit> search(SearchQuery query, String accessToken) {
            throw new CustomerInquiryUnauthorizedException();
          }

          @Override
          public java.util.Optional<CustomerHit> findById(String customerId, String accessToken) {
            return java.util.Optional.empty();
          }
        };
    AtomicInteger invalidations = new AtomicInteger();
    AccessTokenPort tokens =
        new AccessTokenPort() {
          @Override
          public String currentAccessToken() {
            return "stale";
          }

          @Override
          public void invalidate() {
            invalidations.incrementAndGet();
          }
        };
    CustomerSearchService retrying =
        new CustomerSearchService(inquiry, tokens, ServiceErrors.of("customer", PlatformLayer.L5));

    assertThatThrownBy(() -> retrying.search("MOBILE", "9876543210", "IN"))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
    assertThat(invalidations.get()).isEqualTo(1);
  }

  @Test
  void formatsAreCheckedPerSearchModeWithoutEchoingQ() {
    assertInvalidQ("MOBILE", "5876543210", "IN");
    assertInvalidQ("MOBILE", "98765", null);
    assertInvalidQ("PAN", "ABCD1234FF", null);
    assertInvalidQ("CUSTOMER_ID", "CIF-12/34", null);
    assertInvalidQ("CUSTOMER_ID", "123456789012345678901", null);
  }

  @Test
  void normalisesPanAndIndianMobileBeforeAskingCbs() {
    assertThat(service.normalise("PAN", "abcde1234f", null)).isEqualTo("ABCDE1234F");
    assertThat(service.normalise("MOBILE", "+919876543210", "IN")).isEqualTo("9876543210");
    assertThat(service.normalise("CUSTOMER_ID", "5600980433", null)).isEqualTo("5600980433");
  }

  @Test
  void noMatchIsAnEmptyList() {
    assertThat(service.search("CUSTOMER_ID", "NONE", null)).isEmpty();
  }

  private void assertInvalidQ(String by, String q, String countryCode) {
    assertThatThrownBy(() -> service.search(by, q, countryCode))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            thrown -> {
              ServiceException ex = (ServiceException) thrown;
              assertThat(ex.getErrorResponse().getCode()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
              assertThat(ex.getErrorResponse().getErrors())
                  .singleElement()
                  .satisfies(e -> assertThat(e.field()).isEqualTo("q"));
              assertThat(ex.getDiagnostic().getReason()).doesNotContain(q);
            });
  }

  private static AccessTokenPort stubTokens(String token) {
    return new AccessTokenPort() {
      @Override
      public String currentAccessToken() {
        return token;
      }

      @Override
      public void invalidate() {}
    };
  }
}
