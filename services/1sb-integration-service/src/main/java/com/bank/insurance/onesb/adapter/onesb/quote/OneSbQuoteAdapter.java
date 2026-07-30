package com.bank.insurance.onesb.adapter.onesb.quote;

import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.QuoteOffer;
import com.bank.insurance.onesb.domain.port.outbound.OneSbQuotePort;
import com.bank.insurance.onesb.lob.LobQuoteHandler;
import com.bank.insurance.onesb.lob.LobQuoteHandlerRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1SB quote adapter — submit + poll with offer normalisation.
 * All 1SB JSON parsing stays inside this adapter.
 */
@Component
public class OneSbQuoteAdapter implements OneSbQuotePort {

    private final OneSbHttpClient httpClient;
    private final LobQuoteHandlerRegistry handlerRegistry;
    private final ObjectMapper objectMapper;

    public OneSbQuoteAdapter(OneSbHttpClient httpClient,
                             LobQuoteHandlerRegistry handlerRegistry,
                             ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.handlerRegistry = handlerRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public String submitQuote(String jobId, CreateQuoteCommand command) {
        LobQuoteHandler handler = handlerRegistry.get(command.lob());
        Object payload = handler.buildSubmitPayload(command);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = httpClient.post(handler.submitPath(), payload, Map.class);
        return extractReqId(response);
    }

    @Override
    public List<QuoteOffer> pollQuoteResult(String jobId, String externalReqId, String lob) {
        String body = httpClient.get(pollPath(lob, externalReqId), String.class);
        return parseOffers(body);
    }

    @Override
    public boolean isPollComplete(String jobId, String externalReqId, String lob) {
        String body = httpClient.get(pollPath(lob, externalReqId), String.class);
        return parseComplete(body) || !parseOffers(body).isEmpty();
    }

    private String pollPath(String lob, String externalReqId) {
        LobQuoteHandler handler = handlerRegistry.get(Lob.valueOf(lob));
        return handler.pollPath(externalReqId);
    }

    private String extractReqId(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("1SB quote submit returned empty body");
        }
        Object reqId = response.get("reqId");
        if (reqId == null && response.get("data") instanceof Map<?, ?> data) {
            reqId = data.get("reqId");
        }
        if (reqId == null || reqId.toString().isBlank()) {
            throw new IllegalStateException("1SB quote submit missing reqId");
        }
        return reqId.toString();
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

    List<QuoteOffer> parseOffers(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            List<QuoteOffer> offers = new ArrayList<>();

            JsonNode quoteNode = data.path("quote");
            if (quoteNode.isArray()) {
                for (JsonNode q : quoteNode) {
                    offers.add(mapOffer(q));
                }
            } else if (quoteNode.isObject() && !quoteNode.isEmpty()) {
                offers.add(mapOffer(quoteNode));
            }

            JsonNode products = data.path("products");
            if (products.isArray()) {
                for (JsonNode p : products) {
                    offers.add(mapOffer(p));
                }
            }

            // Per-manufacturer errors with no product payload → offer-shaped failure rows
            JsonNode errors = data.path("errors");
            if (errors.isMissingNode()) {
                errors = root.path("errors");
            }
            if (errors.isArray()) {
                for (JsonNode err : errors) {
                    String insurer = text(err, "manufacturerId", "insurerCode", "code");
                    String message = text(err, "message", "errorMessage", "detail");
                    if (message == null || message.isBlank()) {
                        continue;
                    }
                    // Skip if we already mapped a successful offer for same insurer with no error
                    boolean already = offers.stream().anyMatch(o ->
                            insurer != null && insurer.equals(o.insurerCode()) && o.errorSummary() == null);
                    if (!already) {
                        offers.add(new QuoteOffer(
                                null,
                                insurer,
                                text(err, "manufacturerName", "insurerName"),
                                text(err, "productCode"),
                                text(err, "productName"),
                                null,
                                null,
                                null,
                                false,
                                "ERROR",
                                message
                        ));
                    } else {
                        // Attach error onto existing? Prefer separate PARTIAL rows: add error offer
                        offers.add(new QuoteOffer(
                                null,
                                insurer,
                                text(err, "manufacturerName", "insurerName"),
                                text(err, "productCode"),
                                text(err, "productName"),
                                null,
                                null,
                                null,
                                false,
                                "ERROR",
                                message
                        ));
                    }
                }
            }
            return List.copyOf(offers);
        } catch (Exception e) {
            return List.of();
        }
    }

    private QuoteOffer mapOffer(JsonNode node) {
        String errorSummary = text(node, "errorSummary", "errorMessage", "error");
        JsonNode errArr = node.path("errors");
        if ((errorSummary == null || errorSummary.isBlank()) && errArr.isArray() && !errArr.isEmpty()) {
            errorSummary = text(errArr.get(0), "message", "errorMessage", "detail");
        }

        BigDecimal premium = decimal(node, "premiumAmount", "premium", "totalPremium");
        if (premium == null && node.path("premium").isObject()) {
            premium = decimal(node.path("premium"), "amount", "premiumAmount");
        }

        BigDecimal sumAssured = decimal(node, "sumAssured", "sum_assured", "quoteAmount");
        boolean oob = node.path("outOfBound").asBoolean(false)
                || "Yes".equalsIgnoreCase(text(node, "outOfBound"));

        String offerStatus = errorSummary != null && !errorSummary.isBlank() ? "ERROR" : "AVAILABLE";
        String statusField = text(node, "offerStatus", "status");
        if (statusField != null) {
            offerStatus = statusField;
        }

        return new QuoteOffer(
                text(node, "offerId", "quoteId", "id"),
                text(node, "insurerCode", "manufacturerId", "manufacturerCode"),
                text(node, "insurerName", "manufacturerName"),
                text(node, "productCode", "productId"),
                text(node, "productName", "product"),
                premium,
                text(node, "premiumFrequency", "frequency", "premiumPaymentFrequency"),
                sumAssured,
                oob,
                offerStatus,
                errorSummary
        );
    }

    private static String text(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String field : fields) {
            JsonNode v = node.path(field);
            if (!v.isMissingNode() && !v.isNull() && v.isValueNode()) {
                String s = v.asText();
                if (s != null && !s.isBlank()) {
                    return s;
                }
            }
        }
        return null;
    }

    private static BigDecimal decimal(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        for (String field : fields) {
            JsonNode v = node.path(field);
            if (v.isNumber()) {
                return v.decimalValue();
            }
            if (v.isTextual()) {
                try {
                    return new BigDecimal(v.asText());
                } catch (NumberFormatException ignored) {
                    // try next
                }
            }
        }
        return null;
    }
}
