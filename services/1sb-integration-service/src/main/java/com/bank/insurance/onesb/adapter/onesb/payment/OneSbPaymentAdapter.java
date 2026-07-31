package com.bank.insurance.onesb.adapter.onesb.payment;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.PaymentStatus;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPaymentPort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 1SB payment adapter — {@code POST /v1/payment/url} (FUNC-007).
 * Parses payment URL + expiry; does not log the full payment URL.
 */
@Component
public class OneSbPaymentAdapter implements OneSbPaymentPort {

    static final String PAYMENT_URL_PATH = "/v1/payment/url";

    private final OneSbHttpClient httpClient;
    private final SecretProvider secretProvider;

    public OneSbPaymentAdapter(OneSbHttpClient httpClient, SecretProvider secretProvider) {
        this.httpClient = httpClient;
        this.secretProvider = secretProvider;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaymentSession createPaymentSession(String jobId, String applicationNumber, String lob,
                                               String redirectUrl, String actorId) {
        Map<String, Object> payload = buildPayload(applicationNumber, redirectUrl);
        Map<String, Object> response = httpClient.post(PAYMENT_URL_PATH, payload, Map.class);
        return parseSession(jobId, applicationNumber, lob, redirectUrl, response);
    }

    @Override
    public void sendPaymentIntimation(String sessionId) {
        throw new UnsupportedOperationException("Payment intimation is FUNC-008 — not implemented");
    }

    Map<String, Object> buildPayload(String applicationNumber, String redirectUrl) {
        Map<String, Object> distributor = new LinkedHashMap<>();
        distributor.put("distributorID", secretProvider.getDistributorId());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("applicationNo", applicationNumber);
        payload.put("redirectUrl", redirectUrl);
        payload.put("Distributor", distributor);
        return payload;
    }

    static PaymentSession parseSession(String jobId, String applicationNumber, String lob,
                                       String redirectUrl, Map<String, Object> response) {
        if (response == null) {
            throw badUpstream("1SB payment URL returned empty body");
        }

        String paymentUrl = extractPaymentUrl(response);
        if (!StringUtils.hasText(paymentUrl)) {
            throw badUpstream("1SB payment URL response missing paymentUrl");
        }

        Instant expiresAt = extractExpiresAt(response);
        String externalTxnId = firstText(response, "transactionId", "txnId", "paymentRef", "sessionId");
        String sessionId = StringUtils.hasText(externalTxnId) ? externalTxnId : UUID.randomUUID().toString();
        Lob parsedLob = parseLob(lob);

        return new PaymentSession(
                sessionId,
                jobId,
                applicationNumber,
                parsedLob,
                paymentUrl,
                redirectUrl,
                PaymentStatus.AWAITING_PAYMENT,
                externalTxnId,
                Instant.now(),
                expiresAt
        );
    }

    @SuppressWarnings("unchecked")
    static String extractPaymentUrl(Map<String, Object> response) {
        String direct = firstText(response, "paymentUrl", "payment_url");
        if (direct != null) {
            return direct;
        }
        Object details = response.get("PaymentDetails");
        if (details == null) {
            details = response.get("paymentDetails");
        }
        if (details instanceof Map<?, ?> map) {
            return firstText((Map<String, Object>) map, "paymentUrl", "payment_url");
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            String fromData = firstText((Map<String, Object>) dataMap, "paymentUrl", "payment_url");
            if (fromData != null) {
                return fromData;
            }
            Object nested = dataMap.get("PaymentDetails");
            if (nested == null) {
                nested = dataMap.get("paymentDetails");
            }
            if (nested instanceof Map<?, ?> nestedMap) {
                return firstText((Map<String, Object>) nestedMap, "paymentUrl", "payment_url");
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static Instant extractExpiresAt(Map<String, Object> response) {
        Instant direct = parseInstant(firstText(response, "expiresAt", "expiry", "expires_at"));
        if (direct != null) {
            return direct;
        }
        Object details = response.get("PaymentDetails");
        if (details == null) {
            details = response.get("paymentDetails");
        }
        if (details instanceof Map<?, ?> map) {
            Instant nested = parseInstant(firstText((Map<String, Object>) map, "expiresAt", "expiry"));
            if (nested != null) {
                return nested;
            }
        }
        // Default short-lived expiry when upstream omits it
        return Instant.now().plusSeconds(900);
    }

    private static Instant parseInstant(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static Lob parseLob(String lob) {
        if (!StringUtils.hasText(lob)) {
            return Lob.TERM;
        }
        try {
            return Lob.valueOf(lob.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Lob.TERM;
        }
    }

    private static String firstText(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null) {
                String s = v.toString();
                if (StringUtils.hasText(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    private static ServiceException badUpstream(String detail) {
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Upstream Bad Response")
                .status(502)
                .detail(detail)
                .code(ErrorCodes.UPSTREAM_BAD_RESPONSE)
                .retryable(false)
                .build());
    }
}
