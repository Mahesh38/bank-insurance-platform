package com.bank.insurance.onesb.adapter.idempotency;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.insurance.onesb.domain.port.outbound.IdempotencyPort;
import com.bank.insurance.onesb.domain.port.outbound.IdempotencyPort.CachedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

/**
 * Requires {@code Idempotency-Key} on mutating {@code /v1/**} requests.
 * Same key + same body hash → replay cached response; same key + different body → 409.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH");

    private final IdempotencyPort idempotencyPort;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyPort idempotencyPort, ObjectMapper objectMapper) {
        this.idempotencyPort = idempotencyPort;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/v1/")) {
            return true;
        }
        // Read-like POST: master lookup is cacheable GET-equivalent — no Idempotency-Key.
        if (path.startsWith("/v1/master-data")) {
            return true;
        }
        return !MUTATING.contains(request.getMethod().toUpperCase(Locale.ROOT));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getHeader(IDEMPOTENCY_HEADER);
        if (!StringUtils.hasText(key)) {
            writeError(response, 400, ErrorCodes.MISSING_IDEMPOTENCY_KEY,
                    "Missing Idempotency-Key",
                    "Idempotency-Key header is required for mutating /v1 requests");
            return;
        }

        CachedBodyHttpServletRequest cachingRequest = new CachedBodyHttpServletRequest(request);
        String bodyHash = sha256Hex(cachingRequest.getCachedBody());

        var existing = idempotencyPort.find(key);
        if (existing.isPresent()) {
            CachedResponse cached = existing.get();
            if (!bodyHash.equals(cached.bodyHash())) {
                writeError(response, 409, ErrorCodes.IDEMPOTENCY_CONFLICT,
                        "Idempotency Conflict",
                        "Idempotency-Key was already used with a different request body");
                return;
            }
            response.setStatus(cached.status());
            if (cached.contentType() != null) {
                response.setContentType(cached.contentType());
            }
            response.getOutputStream().write(cached.body() != null ? cached.body() : new byte[0]);
            return;
        }

        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(cachingRequest, cachingResponse);

        byte[] responseBody = cachingResponse.getContentAsByteArray();
        int status = cachingResponse.getStatus();
        if (status < 500) {
            idempotencyPort.store(key, bodyHash, new CachedResponse(
                    status,
                    cachingResponse.getContentType(),
                    responseBody,
                    bodyHash
            ));
        }
        cachingResponse.copyBodyToResponse();
    }

    private void writeError(HttpServletResponse response, int status, String code,
                            String title, String detail) throws IOException {
        ServiceErrorResponse body = ServiceErrorResponse.builder()
                .title(title)
                .status(status)
                .detail(detail)
                .code(code)
                .retryable(false)
                .build();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes != null ? bytes : new byte[0]));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
