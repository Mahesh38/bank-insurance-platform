package com.bank.platform.integration.direct.config;

import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCallerAuthFilter;
import com.bank.common.security.internal.InternalCallerAuthSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class InternalAuthConfig {

  @Bean
  FilterRegistrationBean<InternalCallerAuthFilter> internalCallerAuthFilter(
      InternalAuthProperties properties,
      ServiceErrors errors,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    return InternalCallerAuthSupport.registration(
        "/internal/v1/*", "/internal/v1", properties.credentials(), errors, resolver);
  }
}
