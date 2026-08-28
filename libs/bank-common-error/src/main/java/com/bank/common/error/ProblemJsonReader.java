package com.bank.common.error;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads a peer service's error envelope back into a {@link ServiceErrorResponse} — work item
 * {@code ERR-003}.
 *
 * <p>Without this, {@link ErrorPropagation} has nothing to propagate. A calling service sees a
 * status code and an opaque body, so the incident id and the origin the peer carefully attached are
 * thrown away at the first hop and the failure becomes unattributable exactly where attribution
 * matters most.
 *
 * <p>Deliberately tolerant. A malformed or foreign body is not itself an error worth reporting over
 * the failure that produced it, so anything unreadable becomes {@code UPSTREAM_BAD_RESPONSE} with
 * the peer's status preserved.
 */
public final class ProblemJsonReader {

    private final ObjectMapper objectMapper;

    public ProblemJsonReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param body   the peer's response body; may be null, blank or not JSON at all
     * @param status the peer's HTTP status, used when the body does not carry one
     */
    public ServiceErrorResponse read(String body, int status) {
        if (body == null || body.isBlank()) {
            return fallback(status);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String code = text(root, "code");
            if (code == null) {
                return fallback(status);
            }
            return ServiceErrorResponse.builder()
                .type(text(root, "type"))
                .title(orDefault(text(root, "title"), "Upstream error"))
                .status(root.path("status").isInt() ? root.path("status").asInt() : status)
                .detail(text(root, "detail"))
                .code(code)
                .retryable(root.path("retryable").asBoolean(false))
                .upstreamCode(text(root, "upstreamCode"))
                .incidentId(text(root, "incidentId"))
                .correlationId(text(root, "correlationId"))
                .service(text(root, "service"))
                .category(category(text(root, "category")))
                .origin(origin(root.path("origin")))
                .build();
        } catch (Exception e) {
            return fallback(status);
        }
    }

    private ServiceErrorResponse fallback(int status) {
        return ServiceErrorResponse.of(ErrorCodes.UPSTREAM_BAD_RESPONSE).status(status).build();
    }

    private static ErrorOrigin origin(JsonNode node) {
        String service = text(node, "service");
        String code = text(node, "code");
        if (service == null || code == null) {
            return null;
        }
        PlatformLayer layer = null;
        String raw = text(node, "layer");
        if (raw != null) {
            try {
                layer = PlatformLayer.valueOf(raw);
            } catch (IllegalArgumentException ignored) {
                // A layer this build does not know is not worth failing a propagation over.
            }
        }
        return ErrorOrigin.of(service, code, layer);
    }

    private static ErrorCategory category(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ErrorCategory.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull() || !child.isTextual()) {
            return null;
        }
        String value = child.asText();
        return value.isBlank() ? null : value;
    }

    private static String orDefault(String value, String fallback) {
        return value != null ? value : fallback;
    }
}
