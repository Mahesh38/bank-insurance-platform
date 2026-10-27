package com.bank.common.security.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.servlet.HandlerExceptionResolver;

class InternalCallerAuthSupportTest {

  @Test
  void registration_bindsPrefixAndOrder() {
    InternalCallerCredentials credentials =
        new InternalCallerCredentials(
            Map.of("bff", "local-authz-bff-internal-key"), Set.of(), Map.of());
    FilterRegistrationBean<InternalCallerAuthFilter> bean =
        InternalCallerAuthSupport.registration(
            "/internal/v1/*",
            "/internal/v1",
            credentials,
            ServiceErrors.of("test", PlatformLayer.L5),
            mock(HandlerExceptionResolver.class));
    assertThat(bean.getFilter()).isNotNull();
    assertThat(bean.getUrlPatterns()).containsExactly("/internal/v1/*");
    assertThat(bean.getOrder()).isLessThan(0);
  }
}
