package com.bank.workforce.bff.config;

import com.bank.workforce.bff.api.BffSessionAuthenticationEntryPoint;
import com.bank.workforce.bff.api.BffSessionAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class BffSecurityConfig {

  @Bean
  FilterRegistrationBean<BffSessionAuthenticationFilter> sessionFilterRegistration(
      BffSessionAuthenticationFilter filter) {
    FilterRegistrationBean<BffSessionAuthenticationFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  SecurityFilterChain bffSecurityFilterChain(
      HttpSecurity http,
      BffSessionAuthenticationFilter sessionFilter,
      BffSessionAuthenticationEntryPoint entryPoint)
      throws Exception {
    CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrf.setCookieName("WORKFORCE-XSRF");
    csrf.setHeaderName("X-XSRF-TOKEN");
    return http.csrf(
            configurer ->
                configurer
                    .csrfTokenRepository(csrf)
                    .ignoringRequestMatchers(
                        request ->
                            request.getHeader("Authorization") != null
                                || request.getHeader("X-Session-Handle") != null))
        .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/api/v1/auth/**", "/actuator/health/**", "/error")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class)
        .requestCache(cache -> cache.disable())
        .formLogin(login -> login.disable())
        .httpBasic(basic -> basic.disable())
        .build();
  }
}
