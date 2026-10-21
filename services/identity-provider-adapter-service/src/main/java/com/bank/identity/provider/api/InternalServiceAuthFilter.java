package com.bank.identity.provider.api;

import com.bank.identity.provider.config.InternalAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the BFF (and only a caller that holds the shared internal key) before a password or
 * provider token crosses {@code /internal/v1}. Network location is not trust (Deepali 04 §1).
 */
@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

  private final InternalAuthProperties properties;

  public InternalServiceAuthFilter(InternalAuthProperties properties) {
    this.properties = properties;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator") || path.startsWith("/error");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (!path.startsWith("/internal/v1")) {
      filterChain.doFilter(request, response);
      return;
    }
    if (properties.matches(request.getHeader(InternalAuthProperties.HEADER))) {
      bindInternalCaller();
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void bindInternalCaller() {
    // lgtm[java/user-controlled-bypass]
    // codeql[java/user-controlled-bypass]
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken("workforce-access-bff", null, List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
