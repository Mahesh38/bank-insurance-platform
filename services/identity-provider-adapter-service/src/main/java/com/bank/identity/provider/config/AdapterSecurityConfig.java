package com.bank.identity.provider.config;

import com.bank.identity.provider.api.AdapterInternalAuthenticationEntryPoint;
import com.bank.identity.provider.api.InternalServiceAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class AdapterSecurityConfig {

  @Bean
  FilterRegistrationBean<InternalServiceAuthFilter> internalServiceAuthFilterRegistration(
      InternalServiceAuthFilter filter) {
    FilterRegistrationBean<InternalServiceAuthFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  SecurityFilterChain adapterSecurityFilterChain(
      HttpSecurity http,
      InternalServiceAuthFilter internalServiceAuthFilter,
      AdapterInternalAuthenticationEntryPoint entryPoint)
      throws Exception {
    return http.csrf(
            csrf -> csrf.ignoringRequestMatchers("/internal/v1/**", "/actuator/**", "/error"))
        .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/actuator/health/**", "/error")
                    .permitAll()
                    .requestMatchers("/internal/v1/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .requestCache(cache -> cache.disable())
        .formLogin(login -> login.disable())
        .httpBasic(basic -> basic.disable())
        .build();
  }
}
