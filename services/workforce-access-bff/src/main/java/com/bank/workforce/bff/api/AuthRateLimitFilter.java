package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Fixed-window in-memory rate limit for {@code /api/v1/auth/**} (SUG-20261027-iah). 60 requests per
 * client IP per minute. ConcurrentHashMap + opportunistic cleanup — no extra dependencies.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class AuthRateLimitFilter extends OncePerRequestFilter {

  static final int LIMIT_PER_MINUTE = 60;
  private static final long WINDOW_MILLIS = 60_000L;
  private static final int CLEANUP_EVERY = 64;

  private final ServiceErrors errors;
  private final HandlerExceptionResolver resolver;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
  private final AtomicInteger hits = new AtomicInteger();

  public AuthRateLimitFilter(
      ServiceErrors errors,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.errors = errors;
    this.resolver = resolver;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri == null || !uri.startsWith("/api/v1/auth/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String client = clientKey(request);
    long now = System.currentTimeMillis();
    Window window = windows.compute(client, (key, existing) -> refresh(existing, now));
    if (window.count.incrementAndGet() > LIMIT_PER_MINUTE) {
      resolver.resolveException(
          request,
          response,
          null,
          errors
              .error(ErrorCodes.RATE_LIMITED)
              .component("AuthRateLimitFilter")
              .operation("doFilterInternal")
              .reason("auth rate limit exceeded")
              .build());
      return;
    }
    if (hits.incrementAndGet() % CLEANUP_EVERY == 0) {
      cleanup(now);
    }
    filterChain.doFilter(request, response);
  }

  private static Window refresh(Window existing, long now) {
    if (existing == null || now - existing.windowStartMillis >= WINDOW_MILLIS) {
      return new Window(now);
    }
    return existing;
  }

  private void cleanup(long now) {
    Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Window> entry = it.next();
      if (now - entry.getValue().windowStartMillis >= WINDOW_MILLIS * 2) {
        it.remove();
      }
    }
  }

  private static String clientKey(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
    }
    String remote = request.getRemoteAddr();
    return remote == null || remote.isBlank() ? "unknown" : remote;
  }

  private static final class Window {
    final long windowStartMillis;
    final AtomicInteger count = new AtomicInteger();

    Window(long windowStartMillis) {
      this.windowStartMillis = windowStartMillis;
    }
  }
}
