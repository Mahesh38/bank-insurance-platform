package com.bank.insurance.onesb.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.TestErrors;
import com.bank.insurance.onesb.domain.model.ProductUiData;
import com.bank.insurance.onesb.domain.port.outbound.OneSbProductUiDataPort;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("FUNC-027")
@ExtendWith(MockitoExtension.class)
class ProductUiDataServiceTest {

  @Mock OneSbProductUiDataPort port;

  private ProductUiDataService service;

  @BeforeEach
  void setUp() {
    service = new ProductUiDataService(port, TestErrors.ONESB);
  }

  @Test
  void getProductUiData_delegatesWhenParamsPresent() {
    ProductUiData expected = new ProductUiData("345", "BALIC", "REQ-UI", Map.of("ok", true));
    when(port.getProductUiData("345", "BALIC")).thenReturn(expected);

    assertThat(service.getProductUiData("345", "BALIC")).isSameAs(expected);
    verify(port).getProductUiData("345", "BALIC");
  }

  @Test
  void getProductUiData_emptyPayload_throwsBadResponse() {
    when(port.getProductUiData("345", "BALIC"))
        .thenReturn(new ProductUiData("345", "BALIC", "REQ-UI", Map.of()));

    assertThatThrownBy(() -> service.getProductUiData("345", "BALIC"))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE));
  }

  @Test
  void getProductUiData_blankManufacturer_throws422_noUpstream() {
    assertThatThrownBy(() -> service.getProductUiData("345", " "))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.VALIDATION_ERROR));
    verify(port, never())
        .getProductUiData(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }
}
