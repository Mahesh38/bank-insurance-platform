package com.bank.insurance.onesb.adapter.onesb.eligibility;

import com.bank.common.domain.EligibilitySubmitResult;
import com.bank.common.domain.Lob;
import com.bank.common.domain.ProposalSchema;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.port.outbound.OneSbEligibilityPort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1SB gateCriteria adapter. Paths are supplied by Life LOB handlers.
 */
@Component
public class OneSbEligibilityAdapter implements OneSbEligibilityPort {

    private final OneSbHttpClient httpClient;

    public OneSbEligibilityAdapter(OneSbHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ProposalSchema getCriteria(Lob lob, String productCode, String manufacturerId, String path) {
        Map<String, Object> body = httpClient.get(path, Map.class);
        Map<String, Object> fields = body == null ? Map.of() : new LinkedHashMap<>(body);
        return new ProposalSchema(lob, productCode, manufacturerId, null, fields);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EligibilitySubmitResult submit(String path, Object payload) {
        Map<String, Object> response = httpClient.post(path, payload, Map.class);
        if (response == null) {
            return new EligibilitySubmitResult(true, null, "ACCEPTED");
        }
        String reqId = text(response, "reqId", "requestId");
        if (reqId == null && response.get("data") instanceof Map<?, ?> data) {
            reqId = text((Map<String, Object>) data, "reqId", "requestId");
        }
        return classify(response, reqId);
    }

    @SuppressWarnings("unchecked")
    static EligibilitySubmitResult classify(Map<String, Object> response, String reqId) {
        String blob = String.valueOf(response);
        if (blob.contains("eligibilityMapping")) {
            return new EligibilitySubmitResult(false, reqId, "PROVIDER_FAILURE");
        }
        if (response.get("errors") instanceof java.util.List<?> errors && !errors.isEmpty()) {
            String first = String.valueOf(errors.get(0)).toLowerCase();
            if (first.contains("valid") || first.contains("required") || first.contains("field")) {
                return new EligibilitySubmitResult(false, reqId, "VALIDATION_FAILED");
            }
            return new EligibilitySubmitResult(false, reqId, "REJECTED");
        }
        Object data = response.get("data");
        Map<String, Object> dataMap = data instanceof Map<?, ?> map ? (Map<String, Object>) map : response;
        Object eligible = first(dataMap, "eligible", "isEligible", "eligibility");
        if (eligible != null) {
            boolean yes = "true".equalsIgnoreCase(eligible.toString())
                    || "yes".equalsIgnoreCase(eligible.toString())
                    || "eligible".equalsIgnoreCase(eligible.toString());
            return new EligibilitySubmitResult(yes, reqId, yes ? "ELIGIBLE" : "INELIGIBLE");
        }
        String status = text(dataMap, "status", "businessStatus");
        if (status != null) {
            String key = status.trim().toUpperCase().replace(' ', '_');
            return switch (key) {
                case "ELIGIBLE", "SUCCESS", "ACCEPTED" -> new EligibilitySubmitResult(true, reqId, "ELIGIBLE");
                case "INELIGIBLE", "NOT_ELIGIBLE" -> new EligibilitySubmitResult(false, reqId, "INELIGIBLE");
                case "VALIDATION_FAILED", "INVALID" -> new EligibilitySubmitResult(false, reqId, "VALIDATION_FAILED");
                default -> new EligibilitySubmitResult(false, reqId, "UNRECOGNIZED_STATUS");
            };
        }
        return new EligibilitySubmitResult(true, reqId, "ACCEPTED");
    }

    private static Object first(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                return map.get(key);
            }
        }
        return null;
    }

    private static String text(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null && StringUtils.hasText(v.toString())) {
                return v.toString();
            }
        }
        return null;
    }
}
