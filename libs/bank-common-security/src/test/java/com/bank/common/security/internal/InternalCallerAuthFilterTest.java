package com.bank.common.security.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.HandlerExceptionResolver;

class InternalCallerAuthFilterTest {

  private final InternalCallerCredentials credentials =
      new InternalCallerCredentials(
          Map.of("bff", "local-authz-bff-internal-key"), Set.of("bff"), Map.of());
  private final ServiceErrors errors = ServiceErrors.of("test", PlatformLayer.L5);
  private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);

  @Test
  void skipsOutsidePrefix() throws Exception {
    InternalCallerAuthFilter filter =
        new InternalCallerAuthFilter("/internal/v1", credentials, errors, resolver);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/actuator/health");
    assertThat(filter.shouldNotFilter(request)).isTrue();
  }

  @Test
  void authenticatesAndContinues() throws Exception {
    InternalCallerAuthFilter filter =
        new InternalCallerAuthFilter("/internal/v1", credentials, errors, resolver);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getRequestURI()).thenReturn("/internal/v1/leads");
    when(request.getHeader(InternalCallerAuth.HEADER)).thenReturn("local-authz-bff-internal-key");

    filter.doFilterInternal(request, response, chain);

    verify(request).setAttribute(InternalCallerAuth.CALLER_ATTRIBUTE, "bff");
    verify(chain).doFilter(request, response);
    verify(resolver, never()).resolveException(any(), any(), any(), any());
  }

  @Test
  void refusesMissingKey() throws Exception {
    InternalCallerAuthFilter filter =
        new InternalCallerAuthFilter("/internal/v1", credentials, errors, resolver);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getRequestURI()).thenReturn("/internal/v1/leads");
    when(request.getHeader(InternalCallerAuth.HEADER)).thenReturn(null);

    filter.doFilterInternal(request, response, chain);

    verify(chain, never()).doFilter(any(), any());
    verify(resolver).resolveException(eq(request), eq(response), isNull(), any());
  }
}
