package com.bank.insurance.onesb.adapter.onesb.polling;

import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPollPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * {@link OneSbPollPort} backed by Dev A's {@link OneSbHttpClient}.
 */
@Component
public class OneSbHttpClientPollAdapter implements OneSbPollPort {

    private final OneSbHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OneSbHttpClientPollAdapter(OneSbHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public PollResult poll(String path) {
        try {
            String body = httpClient.get(path, String.class);
            return PollResult.of(parseComplete(body), 200);
        } catch (ServiceException ex) {
            return new PollResult(false, ex.getHttpStatus(), ex.getMessage());
        } catch (Exception ex) {
            return PollResult.transportError(ex.getMessage());
        }
    }

    private boolean parseComplete(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode flag = root.path("data").path("isPollComplete");
            if (flag.isMissingNode() || flag.isNull()) {
                flag = root.path("isPollComplete");
            }
            if (flag.isBoolean()) {
                return flag.booleanValue();
            }
            if (flag.isTextual()) {
                return Boolean.parseBoolean(flag.asText());
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
