package com.bank.insurance.onesb.adapter.onesb.productui;

import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.ProductUiData;
import com.bank.insurance.onesb.domain.port.outbound.OneSbProductUiDataPort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Documented Term Product UI Data:
 * {@code GET /insurance/lifeterm/v1/master/getproductuidata?productId=&manufacturerId=}.
 * Saving catalog links to the same Term OpenAPI page — do not invent a lifesave path.
 */
@Component
public class OneSbProductUiDataAdapter implements OneSbProductUiDataPort {

    static final String PATH = "/insurance/lifeterm/v1/master/getproductuidata";

    private final OneSbHttpClient httpClient;

    public OneSbProductUiDataAdapter(OneSbHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public ProductUiData getProductUiData(String productId, String manufacturerId) {
        String path = UriComponentsBuilder.fromPath(PATH)
                .queryParam("productId", productId)
                .queryParam("manufacturerId", manufacturerId)
                .build()
                .encode()
                .toUriString();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = httpClient.get(path, Map.class);
        return wrap(productId, manufacturerId, body);
    }

    @SuppressWarnings("unchecked")
    static ProductUiData wrap(String productId, String manufacturerId, Map<String, Object> body) {
        if (body == null) {
            return new ProductUiData(productId, manufacturerId, null, Map.of());
        }
        String reqId = text(body, "reqId", "requestId");
        Object nested = body.get("data");
        Map<String, Object> data;
        if (nested instanceof Map<?, ?> map) {
            data = new LinkedHashMap<>((Map<String, Object>) map);
        } else {
            data = new LinkedHashMap<>(body);
        }
        return new ProductUiData(productId, manufacturerId, reqId, data);
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
