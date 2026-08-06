package com.bank.workforce.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class BffSecurityConfig {

    @Bean
    SecurityFilterChain bffSecurityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieName("WORKFORCE-XSRF");
        csrf.setHeaderName("X-XSRF-TOKEN");
        return http
            .csrf(configurer -> configurer.csrfTokenRepository(csrf))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/v1/auth/**", "/actuator/health/**").permitAll()
                .anyRequest().denyAll())
            .requestCache(cache -> cache.disable())
            .formLogin(login -> login.disable())
            .httpBasic(basic -> basic.disable())
            .build();
    }
}
