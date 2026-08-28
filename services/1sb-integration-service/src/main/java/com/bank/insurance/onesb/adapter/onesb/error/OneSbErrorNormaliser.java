package com.bank.insurance.onesb.adapter.onesb.error;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps 1SB HTTP status + {@code errors[]} bodies to bank {@link ServiceException}s.
 * Controllers never see raw 1SB JSON — only normalised bank error envelopes.
 *
 * <p><strong>The upstream's prose is a diagnostic, not a detail.</strong> This class used to set
 * {@code detail} from the parsed 1SB body, which put the provider's own words — and its name and
 * status — into the response the bank caller received (defect D1). The text now goes to
 * {@code reason} on the diagnostic, where support and engineers read it, while {@code detail}
 * comes from {@link com.bank.common.error.ErrorCatalogue}.
 *
 * <p>Field-level errors keep travelling in {@code errors[]}: those describe the caller's own
 * submitted fields, they are what makes the failure actionable, and they are covered by the
 * catalogue's per-item bank code rather than the raw 1SB one.
 */
public class OneSbErrorNormaliser {

    private static final String SERVICE_ID = "onesb";
    private static final String UPSTREAM = "1SB";

    private final ObjectMapper objectMapper;

    public OneSbErrorNormaliser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OneSbErrorNormaliser() {
        this(new ObjectMapper());
    }

    /**
     * Normalise an upstream failure into a {@link ServiceException}.
     *
     * @param httpStatus   1SB HTTP status
     * @param responseBody raw response body (may be null/blank/non-JSON)
     */
    public ServiceException normalise(int httpStatus, String responseBody) {
        if (httpStatus == 401) {
            return ServiceException.of(ErrorCodes.UPSTREAM_AUTH_FAILURE)
                    .service(SERVICE_ID)
                    .layer(PlatformLayer.L5)
                    .component("OneSbErrorNormaliser")
                    .upstream(UPSTREAM, null, httpStatus)
                    .reason("1SB returned 401 Unauthorized — check the API key and IP allow-list")
                    .remediation("Runbook: 1SB credential rotation and IP whitelist.")
                    .build();
        }
        if (httpStatus >= 500) {
            return ServiceException.of(ErrorCodes.UPSTREAM_UNAVAILABLE)
                    .service(SERVICE_ID)
                    .layer(PlatformLayer.L5)
                    .component("OneSbErrorNormaliser")
                    .upstream(UPSTREAM, null, httpStatus)
                    .reason("1SB returned " + httpStatus)
                    .build();
        }
        if (httpStatus >= 400) {
            ParsedErrors parsed = parseErrors(responseBody);
            return ServiceException.of(ErrorCodes.UPSTREAM_BUSINESS_ERROR)
                    .service(SERVICE_ID)
                    .layer(PlatformLayer.L5)
                    .component("OneSbErrorNormaliser")
                    .upstream(UPSTREAM, parsed.upstreamCode(), httpStatus)
                    .reason(parsed.detail() != null ? parsed.detail() : "1SB returned " + httpStatus)
                    .errors(parsed.errors())
                    .build();
        }
        return ServiceException.of(ErrorCodes.UPSTREAM_BAD_RESPONSE)
                .service(SERVICE_ID)
                .layer(PlatformLayer.L5)
                .component("OneSbErrorNormaliser")
                .upstream(UPSTREAM, null, httpStatus)
                .reason("Unexpected 1SB status " + httpStatus)
                .build();
    }

    private ParsedErrors parseErrors(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new ParsedErrors("1SB business error", null, List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errorsNode = root.path("errors");
            if (!errorsNode.isArray() || errorsNode.isEmpty()) {
                String message = textOrNull(root, "message");
                String code = textOrNull(root, "code");
                return new ParsedErrors(
                        message != null ? message : "1SB business error",
                        code,
                        List.of());
            }
            List<ServiceError> errors = new ArrayList<>();
            String firstUpstreamCode = null;
            StringBuilder detail = new StringBuilder();
            for (JsonNode err : errorsNode) {
                String field = textOrNull(err, "field");
                String code = firstNonNull(textOrNull(err, "code"), textOrNull(err, "errorCode"));
                String message = firstNonNull(
                        textOrNull(err, "message"),
                        textOrNull(err, "errorMessage"),
                        "Upstream field error");
                if (firstUpstreamCode == null && code != null) {
                    firstUpstreamCode = code;
                }
                // Bank code on each item — never expose raw 1SB code as the sole response code
                errors.add(ServiceError.ofField(ErrorCodes.UPSTREAM_BUSINESS_ERROR, message, field));
                if (!detail.isEmpty()) {
                    detail.append("; ");
                }
                detail.append(message);
            }
            return new ParsedErrors(
                    !detail.isEmpty() ? detail.toString() : "1SB business error",
                    firstUpstreamCode,
                    errors);
        } catch (Exception e) {
            return new ParsedErrors("1SB business error", null, List.of());
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull() || !child.isTextual()) {
            return null;
        }
        String v = child.asText();
        return v.isBlank() ? null : v;
    }

    @SafeVarargs
    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private record ParsedErrors(String detail, String upstreamCode, List<ServiceError> errors) {}
}
