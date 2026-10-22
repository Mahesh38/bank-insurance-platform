package com.bank.persistence.security;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceErrors;
import com.bank.persistence.config.InternalAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses every {@code /internal/v1/**} call that does not carry the shared internal service key.
 * {@code GET /raw-payloads/{id}} returns decrypted 1SB bodies (PII), so cluster reachability is not
 * enough (READINESS-REVIEW §3.7, fail-closed security). Refusal is {@code 401 UNAUTHORIZED} as
 * problem+json; the filter runs before the controllers, so it writes the body itself.
 */
@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

  private final InternalAuthProperties properties;
  private final ServiceErrors errors;
  private final ObjectMapper objectMapper;

  public InternalServiceAuthFilter(
      InternalAuthProperties properties, ServiceErrors errors, ObjectMapper objectMapper) {
    this.properties = properties;
    this.errors = errors;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/internal/v1");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (properties.matches(request.getHeader(InternalAuthProperties.HEADER))) {
      filterChain.doFilter(request, response);
      return;
    }
    ServiceErrorResponse body =
        errors
            .error(ErrorCodes.UNAUTHORIZED)
            .component("InternalServiceAuthFilter")
            .operation(request.getMethod())
            .reason("missing or wrong " + InternalAuthProperties.HEADER)
            .build()
            .getErrorResponse()
            .toPublic();
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
