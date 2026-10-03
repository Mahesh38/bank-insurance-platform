package com.bank.insurance.onesb.application.validation;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * Rejects HTTP-success bodies that are not usable JSON (HTML, load-balancer text, blank).
 * FUNC-028 — validate content type and parseability, not HTTP 200 alone.
 */
public final class UpstreamResponseGuard {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UpstreamResponseGuard() {}

    public static JsonNode requireJson(
            String body, MediaType contentType, String operation, ServiceErrors serviceErrors) {
        if (contentType != null && !isJson(contentType)) {
            throw bad(serviceErrors, operation, "1SB returned non-JSON content type: " + contentType);
        }
        if (!StringUtils.hasText(body)) {
            throw bad(serviceErrors, operation, "1SB returned an empty success body");
        }
        String trimmed = body.trim();
        if (looksLikeHtml(trimmed)) {
            throw bad(serviceErrors, operation, "1SB returned HTML/load-balancer content on HTTP success");
        }
        try {
            return MAPPER.readTree(trimmed);
        } catch (Exception ex) {
            throw bad(serviceErrors, operation, "1SB success body is not parseable JSON");
        }
    }

    public static boolean isJson(MediaType contentType) {
        if (contentType == null) {
            return false;
        }
        return MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                || "json".equalsIgnoreCase(contentType.getSubtype())
                || contentType.getSubtype().endsWith("+json");
    }

    public static boolean looksLikeHtml(String body) {
        if (body == null) {
            return false;
        }
        String t = body.stripLeading().toLowerCase();
        return t.startsWith("<!doctype") || t.startsWith("<html") || t.startsWith("<head")
                || t.contains("<html") && t.contains("<body");
    }

    public static ServiceException bad(ServiceErrors serviceErrors, String operation, String reason) {
        return serviceErrors.error(ErrorCodes.UPSTREAM_BAD_RESPONSE)
                .component("UpstreamResponseGuard")
                .operation(operation)
                .upstream("1SB", null, 200)
                .reason(reason)
                .build();
    }
}
