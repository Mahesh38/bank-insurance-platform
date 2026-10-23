package com.bank.identity.authz.config;

import com.bank.common.error.ServiceErrors;
import com.bank.identity.authz.api.InternalServiceAuthFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class InternalAuthConfig {

  @Bean
  FilterRegistrationBean<InternalServiceAuthFilter> internalServiceAuthFilter(
      InternalAuthProperties properties,
      ServiceErrors errors,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    FilterRegistrationBean<InternalServiceAuthFilter> registration =
        new FilterRegistrationBean<>(new InternalServiceAuthFilter(properties, errors, resolver));
    registration.addUrlPatterns("/internal/v1/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }
}
