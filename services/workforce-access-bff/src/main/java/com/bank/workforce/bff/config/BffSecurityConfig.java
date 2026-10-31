package com.bank.workforce.bff.config;

import com.bank.workforce.bff.api.BffSessionAuthenticationEntryPoint;
import com.bank.workforce.bff.api.BffSessionAuthenticationFilter;
import com.bank.workforce.bff.api.BffSessionCredentials;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
      BffSessionAuthenticationEntryPoint entryPoint,
      WorkforceSessionProperties sessionProperties,
      WorkforceCorsProperties corsProperties)
      throws Exception {
    CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrf.setCookieName("WORKFORCE-XSRF");
    csrf.setHeaderName("X-XSRF-TOKEN");
    HttpSecurity chain =
        http.csrf(
                configurer ->
                    configurer
                        .csrfTokenRepository(csrf)
                        .ignoringRequestMatchers(
                            request -> BffSessionCredentials.skipsCsrf(request, sessionProperties)))
            .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint))
            .authorizeHttpRequests(
                authorize ->
                    authorize
                        .requestMatchers("/api/v1/auth/**", "/actuator/health/**", "/error")
                        .permitAll()
                        .requestMatchers("/api/v1/**")
                        .authenticated()
                        .anyRequest()
                        .denyAll());
    if (corsProperties.allowedOrigins().isEmpty()) {
      chain.cors(cors -> cors.disable());
    } else {
      chain.cors(Customizer.withDefaults());
    }
    return chain
        .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class)
        .requestCache(cache -> cache.disable())
        .formLogin(login -> login.disable())
        .httpBasic(basic -> basic.disable())
        .build();
  }

  /**
   * The bean name is the one {@code http.cors(Customizer.withDefaults())} looks up. A different
   * name leaves Spring Security on the MVC introspector, which rejects credentialed browser calls.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource(WorkforceCorsProperties corsProperties) {
    return corsSource(corsProperties);
  }

  static CorsConfigurationSource corsSource(WorkforceCorsProperties corsProperties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsProperties.allowedOrigins());
    configuration.setAllowedMethods(
        List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of(
            "Authorization",
            "Content-Type",
            "X-XSRF-TOKEN",
            "X-Session-Handle",
            "X-Correlation-Id",
            "Idempotency-Key",
            "X-Journey-Id"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(Duration.ofMinutes(10));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    if (!corsProperties.allowedOrigins().isEmpty()) {
      source.registerCorsConfiguration("/api/**", configuration);
    }
    return source;
  }
}
