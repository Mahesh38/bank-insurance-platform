package com.bank.platform.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.customer.adapter.cbs.StubCustomerDirectory;
import com.bank.platform.customer.domain.CustomerInquiryPort.CustomerHit;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-031")
class CustomerSearchServiceTest {

  private final CustomerSearchService service =
      new CustomerSearchService(
          new StubCustomerDirectory(),
          () -> "access-token",
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
}
