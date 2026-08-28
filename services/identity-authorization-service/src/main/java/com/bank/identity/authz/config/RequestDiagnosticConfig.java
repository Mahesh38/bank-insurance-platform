package com.bank.identity.authz.config;

import com.bank.common.observability.RequestDiagnosticFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Seeds the diagnostic context for every request into this service (ERR-006).
 *
 * <p>Registered at highest precedence so a correlation id exists before any other filter can log
 * or reject: a request refused at the edge is exactly the one support needs to be able to find.
 */
@Configuration
public class RequestDiagnosticConfig {

    @Bean
    public FilterRegistrationBean<RequestDiagnosticFilter> requestDiagnosticFilter() {
        FilterRegistrationBean<RequestDiagnosticFilter> registration =
            new FilterRegistrationBean<>(new RequestDiagnosticFilter("authz"));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
