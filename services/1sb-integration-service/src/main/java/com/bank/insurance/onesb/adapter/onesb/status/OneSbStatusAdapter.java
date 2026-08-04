package com.bank.insurance.onesb.adapter.onesb.status;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.OneSbApplicationStatusResult;
import com.bank.insurance.onesb.domain.port.outbound.OneSbStatusPort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 1SB status adapter — {@code POST /LifeTerm/prostat/} (FUNC-009), per
 * {@code field-guides/application-status.md}. An unknown {@code applicationNo} comes back as a
 * 200 with an empty {@code manufacturer[]} array (not a 4xx), so this adapter maps that to
 * {@link Optional#empty()} and {@code StatusService} raises the 404.
 */
@Component
public class OneSbStatusAdapter implements OneSbStatusPort {

    static final String STATUS_PATH = "/LifeTerm/prostat/";

    private final OneSbHttpClient httpClient;
    private final SecretProvider secretProvider;

    public OneSbStatusAdapter(OneSbHttpClient httpClient, SecretProvider secretProvider) {
        this.httpClient = httpClient;
        this.secretProvider = secretProvider;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<OneSbApplicationStatusResult> getStatus(String applicationNumber, String insurerCode,
                                                             String productCode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("insuranceCompanyCode", insurerCode);
        payload.put("distributorID", secretProvider.getDistributorId());
        payload.put("applicationNo", applicationNumber);
        if (StringUtils.hasText(productCode)) {
            payload.put("productCode", productCode);
        }

        Map<String, Object> response = httpClient.post(STATUS_PATH, payload, Map.class);
        return parseResult(applicationNumber, response);
    }

    @SuppressWarnings("unchecked")
    static Optional<OneSbApplicationStatusResult> parseResult(String applicationNumber,
                                                               Map<String, Object> response) {
        if (response == null) {
            return Optional.empty();
        }
        List<Object> manufacturers = asList(response.get("manufacturer"));
        if (manufacturers.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> manufacturer = asMap(manufacturers.get(0));
        List<Object> products = asList(manufacturer.get("product"));
        if (products.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> product = asMap(products.get(0));

        Map<String, Object> applicationStatus = asMap(product.get("applicationStatus"));
        String rawStatus = text(applicationStatus, "applicationStatus");
        if (!StringUtils.hasText(rawStatus)) {
            return Optional.empty();
        }
        String subStatus = firstText(applicationStatus, "manufacturerAppStatus", "desc");

        Map<String, Object> policyDetails = asMap(product.get("policyDetails"));
        String policyNumber = text(policyDetails, "policyNo");

        return Optional.of(new OneSbApplicationStatusResult(applicationNumber, rawStatus, subStatus, policyNumber));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value) {
        return value instanceof List<?> list ? (List<Object>) list : List.of();
    }

    private static String text(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        String s = v.toString();
        return StringUtils.hasText(s) ? s : null;
    }

    private static String firstText(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            String v = text(map, key);
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
