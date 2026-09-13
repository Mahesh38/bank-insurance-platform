package com.bank.insurance.onesb.adapter.onesb.ulip;

import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.UlipFundResult;
import com.bank.insurance.onesb.domain.port.outbound.OneSbUlipFundPort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Documented ULIP fund helpers (Saving OpenAPI):
 * {@code POST /insurance/lifesave/v1/fund/list} and
 * {@code POST /insurance/lifesave/v1/fund/performance}.
 * Do not call guessed {@code /quote/ulipList} or {@code /quote/ulipPerformance}.
 */
@Component
public class OneSbUlipFundAdapter implements OneSbUlipFundPort {

    static final String LIST_PATH = "/insurance/lifesave/v1/fund/list";
    static final String PERFORMANCE_PATH = "/insurance/lifesave/v1/fund/performance";

    private final OneSbHttpClient httpClient;

    public OneSbUlipFundAdapter(OneSbHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public UlipFundResult listFunds(Object payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = httpClient.post(LIST_PATH, payload, Map.class);
        return wrap(body);
    }

    @Override
    public UlipFundResult fundPerformance(Object payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = httpClient.post(PERFORMANCE_PATH, payload, Map.class);
        return wrap(body);
    }

    @SuppressWarnings("unchecked")
    static UlipFundResult wrap(Map<String, Object> body) {
        if (body == null) {
            return new UlipFundResult(null, Map.of());
        }
        String reqId = text(body, "reqId", "requestId");
        Object nested = body.get("data");
        Map<String, Object> data;
        if (nested instanceof Map<?, ?> map) {
            data = new LinkedHashMap<>((Map<String, Object>) map);
        } else {
            data = new LinkedHashMap<>(body);
        }
        return new UlipFundResult(reqId, data);
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
