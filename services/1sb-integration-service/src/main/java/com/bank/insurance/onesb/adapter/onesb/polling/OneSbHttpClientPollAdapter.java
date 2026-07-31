package com.bank.insurance.onesb.adapter.onesb.polling;

import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPollPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * {@link OneSbPollPort} backed by Dev A's {@link OneSbHttpClient}.
 * Generic path poll — offers list is always empty (quote offers via {@code OneSbQuotePort}).
 * Extracts {@code applicationNumber} when present (proposal polls).
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
            ParsedPoll parsed = parse(body);
            return PollResult.of(parsed.complete(), 200, List.of(), parsed.applicationNumber());
        } catch (ServiceException ex) {
            return new PollResult(false, ex.getHttpStatus(), ex.getMessage(), List.of());
        } catch (Exception ex) {
            return PollResult.transportError(ex.getMessage());
        }
    }

    private ParsedPoll parse(String body) {
        if (body == null || body.isBlank()) {
            return new ParsedPoll(false, null);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            boolean complete = readCompleteFlag(data.isMissingNode() ? root : data);
            if (!complete) {
                complete = readCompleteFlag(root);
            }
            String applicationNumber = firstText(data,
                    "applicationNumber", "applicationNo", "application_number");
            if (applicationNumber == null) {
                applicationNumber = firstText(root,
                        "applicationNumber", "applicationNo", "application_number");
            }
            if (!complete && StringUtils.hasText(applicationNumber)) {
                complete = true;
            }
            return new ParsedPoll(complete, applicationNumber);
        } catch (Exception e) {
            return new ParsedPoll(false, null);
        }
    }

    private static boolean readCompleteFlag(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return false;
        }
        JsonNode flag = node.path("isPollComplete");
        if (flag.isBoolean()) {
            return flag.booleanValue();
        }
        if (flag.isTextual()) {
            return Boolean.parseBoolean(flag.asText());
        }
        return false;
    }

    private static String firstText(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            JsonNode v = node.path(name);
            if (v.isTextual() && StringUtils.hasText(v.asText())) {
                return v.asText();
            }
        }
        return null;
    }

    private record ParsedPoll(boolean complete, String applicationNumber) {}
}
