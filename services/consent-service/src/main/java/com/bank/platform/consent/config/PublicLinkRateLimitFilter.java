package com.bank.platform.consent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Refuses a burst against the public consent link before the token is looked up. */
public class PublicLinkRateLimitFilter extends OncePerRequestFilter {

  private final PublicLinkRateLimiter limiter;

  public PublicLinkRateLimitFilter(PublicLinkRateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri == null || !uri.startsWith("/public/v1/consent-links/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!limiter.allow(request.getRemoteAddr())) {
      response.setStatus(429);
      response.setHeader("Cache-Control", "no-store");
      response.setContentType("text/plain;charset=UTF-8");
      response.getWriter().write("Please wait a moment and try again.");
      return;
    }
    filterChain.doFilter(request, response);
  }
}
