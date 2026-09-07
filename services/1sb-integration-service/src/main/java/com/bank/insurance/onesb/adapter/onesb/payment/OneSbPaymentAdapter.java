package com.bank.insurance.onesb.adapter.onesb.payment;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.config.PaymentProperties;
import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.common.domain.Lob;
import com.bank.insurance.onesb.domain.model.OneSbPaymentUrlResult;
import com.bank.common.domain.RawPayloadDirection;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPaymentPort;
import com.bank.insurance.onesb.domain.port.outbound.RawPayloadStorePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1SB payment adapter — {@code POST /v1/payment/url} (FUNC-007). One path for all LOBs: the
 * upstream API is generic (insurer/product carried in the payload), so unlike quote/proposal
 * there is no per-LOB handler here — matches architecture §2 component diagram
 * ({@code PayC → PayS → PayPt} directly, no {@code lob.*} layer).
 * <p>
 * {@code distributorID} is injected from {@link SecretProvider} only — never client-supplied
 * (COMP-004 / D7), same rule as {@code TermProposalHandler}.
 */
@Component
public class OneSbPaymentAdapter implements OneSbPaymentPort {

    static final String PAYMENT_URL_PATH = "/v1/payment/url";

    private final OneSbHttpClient httpClient;
    private final SecretProvider secretProvider;
    private final Clock clock;
    private final long sessionTtlSeconds;
    private final ObjectMapper objectMapper;
    private final RawPayloadStorePort rawPayloadStorePort;

    @Autowired
    public OneSbPaymentAdapter(OneSbHttpClient httpClient, SecretProvider secretProvider, Clock clock,
                               PaymentProperties paymentProperties, ObjectMapper objectMapper,
                               RawPayloadStorePort rawPayloadStorePort) {
        this(httpClient, secretProvider, clock, paymentProperties.sessionTtlSeconds(),
                objectMapper, rawPayloadStorePort);
    }

    /** Test / manual wiring without Spring properties or raw payload capture. */
    public OneSbPaymentAdapter(OneSbHttpClient httpClient, SecretProvider secretProvider, Clock clock,
                               long sessionTtlSeconds) {
        this(httpClient, secretProvider, clock, sessionTtlSeconds, new ObjectMapper(),
                (jobId, direction, operation, lob, payload, httpStatus) -> { });
    }

    public OneSbPaymentAdapter(OneSbHttpClient httpClient, SecretProvider secretProvider, Clock clock,
                               long sessionTtlSeconds, ObjectMapper objectMapper,
                               RawPayloadStorePort rawPayloadStorePort) {
        this.httpClient = httpClient;
        this.secretProvider = secretProvider;
        this.clock = clock;
        this.sessionTtlSeconds = sessionTtlSeconds > 0 ? sessionTtlSeconds : 3600L;
        this.objectMapper = objectMapper;
        this.rawPayloadStorePort = rawPayloadStorePort;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OneSbPaymentUrlResult createPaymentUrl(CreatePaymentCommand command) {
        Map<String, Object> payload = buildPayload(command);
        captureRaw(command.jobId(), RawPayloadDirection.REQ, PAYMENT_URL_PATH, command.lob(), payload);
        Map<String, Object> response = httpClient.post(PAYMENT_URL_PATH, payload, Map.class);
        captureRaw(command.jobId(), RawPayloadDirection.RES, PAYMENT_URL_PATH, command.lob(), response);
        return parseResult(response, clock.instant().plusSeconds(sessionTtlSeconds));
    }

    private void captureRaw(String jobId, RawPayloadDirection direction, String operation,
                            Lob lob, Object body) {
        if (jobId == null || body == null) {
            return;
        }
        try {
            String json = body instanceof String s ? s : objectMapper.writeValueAsString(body);
            rawPayloadStorePort.store(jobId, direction, operation, lob, json, null);
        } catch (Exception ignored) {
            // best-effort — never block the payment flow on capture failure
        }
    }

    @Override
    public void sendPaymentIntimation(String sessionId) {
        // FUNC-008 (P1) — deferred, TD-022. Not wired to any bank-facing endpoint yet.
        throw new UnsupportedOperationException(
                "Payment intimation (FUNC-008) is not implemented yet — see TECH-DEBT TD-022");
    }

    private Map<String, Object> buildPayload(CreatePaymentCommand command) {
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> additionalSetup = new LinkedHashMap<>();
        additionalSetup.put("currency", StringUtils.hasText(command.currency()) ? command.currency() : "INR");
        root.put("AdditionalSetup", additionalSetup);

        Map<String, Object> distributor = new LinkedHashMap<>();
        distributor.put("distributorID", secretProvider.getDistributorId());
        if (StringUtils.hasText(command.agentId())) {
            distributor.put("agentID", command.agentId());
        }
        root.put("Distributor", distributor);

        root.put("insuranceCompanyCode", command.insurerCode());
        root.put("productCode", command.productCode());
        root.put("applicationNo", command.applicationNumber());
        root.put("redirectUrl", command.redirectUrl());
        if (StringUtils.hasText(command.journeyId())) {
            root.put("UITrackingRefNo", command.journeyId());
        }

        Map<String, Object> memberDetails = new LinkedHashMap<>();
        if (command.payer() != null) {
            memberDetails.put("firstName", command.payer().firstName());
            memberDetails.put("lastName", command.payer().lastName());
            memberDetails.put("mobileNumber", command.payer().mobileNumber());
            memberDetails.put("email", command.payer().email());
        }
        root.put("MemberDetails", memberDetails);

        Map<String, Object> paymentDetails = new LinkedHashMap<>();
        paymentDetails.put("premiumPaymentFrequency", command.premiumFrequency());
        paymentDetails.put("amountToBePaid", command.amountToBePaid());
        root.put("PaymentDetails", paymentDetails);

        return root;
    }

    @SuppressWarnings("unchecked")
    static OneSbPaymentUrlResult parseResult(Map<String, Object> response, Instant expiresAt) {
        if (response == null) {
            throw new IllegalStateException("1SB payment url call returned empty body");
        }
        String paymentUrl = firstText(response, "paymentUrl");
        String externalTxnId = firstText(response, "transactionId", "txnId", "referenceId");
        if (paymentUrl == null && response.get("PaymentDetails") instanceof Map<?, ?> pd) {
            Map<String, Object> paymentDetails = (Map<String, Object>) pd;
            paymentUrl = firstText(paymentDetails, "paymentUrl");
            if (externalTxnId == null) {
                externalTxnId = firstText(paymentDetails, "transactionId", "txnId", "referenceId");
            }
        }
        if (!StringUtils.hasText(paymentUrl)) {
            throw new IllegalStateException("1SB payment url call missing paymentUrl in response");
        }
        return new OneSbPaymentUrlResult(paymentUrl, externalTxnId, expiresAt);
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
}
