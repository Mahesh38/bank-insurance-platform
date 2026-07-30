package com.bank.insurance.onesb.adapter.onesb.status;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.ApplicationStatus;
import com.bank.insurance.onesb.domain.model.BankStage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("FUNC-009")
class OneSbStatusAdapterTest {

    @Mock OneSbHttpClient httpClient;
    @Mock SecretProvider secretProvider;

    @Test
    void fetchStatus_buildsPayload_andParsesNestedManufacturerProduct() {
        when(secretProvider.getDistributorId()).thenReturn("TEST_DIST");

        Map<String, Object> statusNode = new LinkedHashMap<>();
        statusNode.put("applicationStatus", "REQUIREMENTS_PENDING");
        statusNode.put("applicationStatusDesc", "Pending requirements");
        statusNode.put("manufacturerAppStatus", "DOC_WAIT");
        statusNode.put("manufacturerAppStatusDesc", "Waiting for income proof");

        Map<String, Object> policyDetails = new LinkedHashMap<>();
        policyDetails.put("policyNo", "POL-99");

        Map<String, Object> product = new LinkedHashMap<>();
        product.put("productCode", "TERM_1");
        product.put("applicationStatus", statusNode);
        product.put("policyDetails", policyDetails);

        Map<String, Object> manufacturer = new LinkedHashMap<>();
        manufacturer.put("insuranceCompanyCode", "XYZ");
        manufacturer.put("product", List.of(product));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("applicationNo", "APP-99");
        response.put("manufacturer", List.of(manufacturer));

        when(httpClient.post(eq(OneSbStatusAdapter.PATH), org.mockito.ArgumentMatchers.any(), eq(Map.class)))
                .thenReturn(response);

        OneSbStatusAdapter adapter = new OneSbStatusAdapter(httpClient, secretProvider);
        ApplicationStatus status = adapter.fetchStatus("APP-99", "TERM", "XYZ", null);

        assertThat(status.onesbStatus()).isEqualTo("REQUIREMENTS_PENDING");
        assertThat(status.manufacturerAppStatus()).isEqualTo("DOC_WAIT");
        assertThat(status.manufacturerAppStatusDesc()).isEqualTo("Waiting for income proof");
        assertThat(status.policyNumber()).isEqualTo("POL-99");
        assertThat(status.insuranceCompanyCode()).isEqualTo("XYZ");
        assertThat(status.bankStatus()).isEqualTo(BankStage.UNKNOWN);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).post(eq(OneSbStatusAdapter.PATH), bodyCaptor.capture(), eq(Map.class));
        Map<String, Object> body = bodyCaptor.getValue();
        assertThat(body.get("insuranceCompanyCode")).isEqualTo("XYZ");
        assertThat(body.get("applicationNo")).isEqualTo("APP-99");
        assertThat(body.get("distributorID")).isEqualTo("TEST_DIST");
        assertThat(body).doesNotContainKey("policyNo");
    }

    @Test
    void fetchStatus_includesOptionalPolicyNo_inPayload() {
        when(secretProvider.getDistributorId()).thenReturn("TEST_DIST");
        when(httpClient.post(eq(OneSbStatusAdapter.PATH), org.mockito.ArgumentMatchers.any(), eq(Map.class)))
                .thenReturn(Map.of("applicationStatus", "POLICY_ISSUED", "policyNo", "POL-1"));

        OneSbStatusAdapter adapter = new OneSbStatusAdapter(httpClient, secretProvider);
        adapter.fetchStatus("APP-1", "TERM", "XYZ", "POL-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).post(eq(OneSbStatusAdapter.PATH), bodyCaptor.capture(), eq(Map.class));
        assertThat(bodyCaptor.getValue().get("policyNo")).isEqualTo("POL-1");
    }

    @Test
    void parseStatus_flatFields() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("applicationStatus", "PAYMENT_SUCCESS");
        response.put("manufacturerAppStatus", "PAID");
        response.put("manufacturerAppStatusDesc", "Premium received");
        response.put("policyNo", "POL-FLAT");

        ApplicationStatus status = OneSbStatusAdapter.parseStatus("APP-FLAT", "TERM", "ABC", response);

        assertThat(status.onesbStatus()).isEqualTo("PAYMENT_SUCCESS");
        assertThat(status.manufacturerAppStatus()).isEqualTo("PAID");
        assertThat(status.policyNumber()).isEqualTo("POL-FLAT");
    }

    @Test
    void fetchStatus_emptyManufacturer_throws404() {
        when(secretProvider.getDistributorId()).thenReturn("TEST_DIST");
        when(httpClient.post(eq(OneSbStatusAdapter.PATH), org.mockito.ArgumentMatchers.any(), eq(Map.class)))
                .thenReturn(Map.of("manufacturer", List.of()));

        OneSbStatusAdapter adapter = new OneSbStatusAdapter(httpClient, secretProvider);
        assertThatThrownBy(() -> adapter.fetchStatus("APP-EMPTY", "TERM", "XYZ", null))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorResponse().getStatus()).isEqualTo(404);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
                    assertThat(se.getErrorResponse().getTitle()).isEqualTo("Not Found");
                });
    }

    @Test
    void fetchStatus_unsupportedLob_throws422() {
        OneSbStatusAdapter adapter = new OneSbStatusAdapter(httpClient, secretProvider);
        assertThatThrownBy(() -> adapter.fetchStatus("APP-1", "HEALTH", "XYZ", null))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorResponse().getStatus()).isEqualTo(422);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UNSUPPORTED_LOB);
                });
    }

    @Test
    void parseStatus_missingApplicationStatus_throws404() {
        assertThatThrownBy(() -> OneSbStatusAdapter.parseStatus(
                "APP-X", "TERM", "XYZ", Map.of("ok", true)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND));
    }
}
