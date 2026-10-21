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
