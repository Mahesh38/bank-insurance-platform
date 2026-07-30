package com.bank.insurance.onesb.adapter.onesb.payment;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.PaymentStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("FUNC-007")
class OneSbPaymentAdapterTest {

    @Mock OneSbHttpClient httpClient;
    @Mock SecretProvider secretProvider;

    @Test
    void createPaymentSession_postsToPaymentUrl_andParsesNestedPaymentDetails() {
        when(secretProvider.getDistributorId()).thenReturn("TEST_DIST");
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("paymentUrl", "https://payments.1sb.test/session/abc");
        details.put("expiresAt", "2026-07-30T20:00:00Z");
        response.put("PaymentDetails", details);
        response.put("transactionId", "txn-99");
        when(httpClient.post(eq(OneSbPaymentAdapter.PAYMENT_URL_PATH), org.mockito.ArgumentMatchers.any(),
                eq(Map.class))).thenReturn(response);

        OneSbPaymentAdapter adapter = new OneSbPaymentAdapter(httpClient, secretProvider);
        PaymentSession session = adapter.createPaymentSession(
                "job-1", "APP-99", "TERM", "https://bank.test/cb", "actor");

        assertThat(session.sessionId()).isEqualTo("txn-99");
        assertThat(session.paymentUrl()).isEqualTo("https://payments.1sb.test/session/abc");
        assertThat(session.expiresAt()).isEqualTo(java.time.Instant.parse("2026-07-30T20:00:00Z"));
        assertThat(session.status()).isEqualTo(PaymentStatus.AWAITING_PAYMENT);
        assertThat(session.jobId()).isEqualTo("job-1");
        assertThat(session.applicationNumber()).isEqualTo("APP-99");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).post(eq(OneSbPaymentAdapter.PAYMENT_URL_PATH), bodyCaptor.capture(), eq(Map.class));
        Map<String, Object> body = bodyCaptor.getValue();
        assertThat(body.get("applicationNo")).isEqualTo("APP-99");
        assertThat(body.get("redirectUrl")).isEqualTo("https://bank.test/cb");
        @SuppressWarnings("unchecked")
        Map<String, Object> distributor = (Map<String, Object>) body.get("Distributor");
        assertThat(distributor.get("distributorID")).isEqualTo("TEST_DIST");
    }

    @Test
    void createPaymentSession_missingPaymentUrl_throwsUpstreamBadResponse() {
        when(secretProvider.getDistributorId()).thenReturn("TEST_DIST");
        when(httpClient.post(eq(OneSbPaymentAdapter.PAYMENT_URL_PATH), org.mockito.ArgumentMatchers.any(),
                eq(Map.class))).thenReturn(Map.of("ok", true));

        OneSbPaymentAdapter adapter = new OneSbPaymentAdapter(httpClient, secretProvider);
        assertThatThrownBy(() -> adapter.createPaymentSession(
                null, "APP", "TERM", "https://bank.test/cb", "a"))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE));
    }

    @Test
    void parseSession_flatPaymentUrl_defaultsExpiryWhenAbsent() {
        Map<String, Object> response = Map.of("paymentUrl", "https://pay.test/x");
        PaymentSession session = OneSbPaymentAdapter.parseSession(
                null, "APP", "TERM", "https://bank.test/cb", response);
        assertThat(session.paymentUrl()).isEqualTo("https://pay.test/x");
        assertThat(session.expiresAt()).isAfter(java.time.Instant.now());
    }
}
