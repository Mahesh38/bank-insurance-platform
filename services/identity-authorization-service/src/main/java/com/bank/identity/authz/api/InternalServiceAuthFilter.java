package com.bank.identity.authz.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.identity.authz.config.InternalAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Authenticates the calling service before anything under {@code /internal/v1} runs — the same
 * {@code X-Internal-Service-Key} scheme as identity-provider-adapter-service (READINESS-REVIEW
 * §3.7: "any in-cluster caller can resolve, create or approve partners"). Network location is not
 * trust (Deepali 04 §1). The authenticated caller id is bound as a request attribute so controllers
 * can decide whether that caller may assert {@code X-Actor-Id}.
 *
 * <p>A plain servlet filter rather than Spring Security: this service has no other security need,
 * and adding the starter would put a default-deny chain over actuator probes as well.
 */
public class InternalServiceAuthFilter extends OncePerRequestFilter {

  public static final String CALLER_ATTRIBUTE = "identity.internal-caller";

  private final InternalAuthProperties properties;
  private final ServiceErrors errors;
  private final HandlerExceptionResolver resolver;

  public InternalServiceAuthFilter(
      InternalAuthProperties properties,
      ServiceErrors errors,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.properties = properties;
    this.errors = errors;
    this.resolver = resolver;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/internal/v1");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Optional<String> caller =
        properties.authenticate(request.getHeader(InternalAuthProperties.HEADER));
    if (caller.isEmpty()) {
      resolver.resolveException(
          request,
          response,
          null,
          errors
              .error(ErrorCodes.UNAUTHORIZED)
              .component("InternalServiceAuthFilter")
              .operation("doFilterInternal")
              .reason("internal caller is not authenticated")
              .build());
      return;
    }
    request.setAttribute(CALLER_ATTRIBUTE, caller.get());
    filterChain.doFilter(request, response);
  }
}
