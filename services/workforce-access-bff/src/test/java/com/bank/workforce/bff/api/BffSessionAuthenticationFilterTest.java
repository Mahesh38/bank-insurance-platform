package com.bank.workforce.bff.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import com.bank.workforce.bff.session.SessionStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("FUNC-031")
class BffSessionAuthenticationFilterTest {

  private static final String HANDLE = "session-handle-value";
  private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock SessionStore sessions;
  @Mock HandlerExceptionResolver resolver;
  @Mock FilterChain chain;

  private WorkforceSessionProperties properties;
  private BffSessionAuthenticationFilter filter;
  private WorkforceSession session;

  @BeforeEach
  void setUp() {
    properties =
        new WorkforceSessionProperties(
            "memory",
            "WORKFORCE_SESSION",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            Duration.ofMinutes(5),
            Duration.ofMinutes(1),
            Duration.ofHours(8),
            Set.of("http://localhost"));
    filter =
        new BffSessionAuthenticationFilter(
            sessions, properties, ServiceErrors.of("bff", PlatformLayer.L4), resolver);
    session =
        new WorkforceSession(
            HANDLE,
            USER,
            "BANK_EMPLOYEE",
            "ACTIVE",
            1L,
            null,
            "sub-1",
            "rm.one",
            "access-token",
            "refresh-token",
            "id-token",
            Instant.now().plusSeconds(3600),
            Instant.now(),
            Map.of());
  }

  @Test
  void blankHandleContinuesUnauthenticated() throws Exception {
    MockHttpServletRequest request = request("/api/v1/country-codes");

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    verify(chain).doFilter(eq(request), any());
    verify(sessions, never()).getSession(any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void headerHandleBindsPrincipalWithoutStoringHandle() throws Exception {
    when(sessions.getSession(HANDLE)).thenReturn(Optional.of(session));
    MockHttpServletRequest request = request("/api/v1/country-codes");
    request.addHeader("X-Session-Handle", HANDLE);
    CapturingChain capturing = new CapturingChain();

    filter.doFilter(request, new MockHttpServletResponse(), capturing);

    assertThat(capturing.authentication).isNotNull();
    assertThat(capturing.authentication.getPrincipal()).isEqualTo(USER.toString());
    assertThat(capturing.authentication.getCredentials()).isNull();
    assertThat(request.getAttribute(BffSessionInterceptor.SESSION_ATTR)).isEqualTo(session);
  }

  @Test
  void bearerHandleBindsSession() throws Exception {
    when(sessions.getSession(HANDLE)).thenReturn(Optional.of(session));
    MockHttpServletRequest request = request("/api/v1/country-codes");
    request.addHeader("Authorization", "Bearer " + HANDLE);
    CapturingChain capturing = new CapturingChain();

    filter.doFilter(request, new MockHttpServletResponse(), capturing);

    assertThat(capturing.authentication.getPrincipal()).isEqualTo(USER.toString());
    assertThat(capturing.authentication.getCredentials()).isNull();
  }

  @Test
  void cookieHandleBindsSession() throws Exception {
    when(sessions.getSession(HANDLE)).thenReturn(Optional.of(session));
    MockHttpServletRequest request = request("/api/v1/country-codes");
    request.setCookies(new Cookie("WORKFORCE_SESSION", HANDLE));
    CapturingChain capturing = new CapturingChain();

    filter.doFilter(request, new MockHttpServletResponse(), capturing);

    assertThat(capturing.authentication.getPrincipal()).isEqualTo(USER.toString());
    assertThat(capturing.authentication.getCredentials()).isNull();
    assertThat(request.getAttribute(BffSessionInterceptor.SESSION_ATTR)).isEqualTo(session);
  }

  @Test
  void unknownHandleDoesNotContinueTheChain() throws Exception {
    when(sessions.getSession(HANDLE)).thenReturn(Optional.empty());
    MockHttpServletRequest request = request("/api/v1/country-codes");
    request.addHeader("X-Session-Handle", HANDLE);

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    verify(chain, never()).doFilter(any(), any());
    verify(resolver).resolveException(eq(request), any(), eq(null), any());
  }

  private static MockHttpServletRequest request(String path) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setRequestURI(path);
    return request;
  }

  private static final class CapturingChain implements FilterChain {
    Authentication authentication;

    @Override
    public void doFilter(
        jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
      authentication = SecurityContextHolder.getContext().getAuthentication();
    }
  }
}
