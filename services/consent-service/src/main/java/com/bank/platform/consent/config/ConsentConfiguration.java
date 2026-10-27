package com.bank.platform.consent.config;

import com.bank.platform.consent.adapter.crypto.AesFieldCipher;
import com.bank.platform.consent.adapter.link.ConfiguredPublicOrigin;
import com.bank.platform.consent.adapter.link.HmacLinkTokens;
import com.bank.platform.consent.adapter.link.LoggingLinkDispatch;
import com.bank.platform.consent.adapter.otp.HmacOtpHasher;
import com.bank.platform.consent.adapter.otp.LoggingOtpDispatch;
import com.bank.platform.consent.adapter.security.LoggingSecurityEventSink;
import com.bank.platform.consent.domain.FieldCipher;
import com.bank.platform.consent.domain.LinkPorts;
import com.bank.platform.consent.domain.OtpDispatch;
import com.bank.platform.consent.domain.OtpHasher;
import com.bank.platform.consent.domain.SecurityEventSink;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

@Configuration
public class ConsentConfiguration {

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  Clock systemClock() {
    return Clock.systemUTC();
  }

  @Bean
  FieldCipher fieldCipher(
      Environment environment,
      @Value("${consent.field-key}") String fieldKey,
      @Value("${consent.key-id}") String keyId) {
    rejectDevKey(environment.getActiveProfiles(), fieldKey);
    return new AesFieldCipher(fieldKey, keyId);
  }

  @Bean
  OtpHasher otpHasher(@Value("${consent.otp-pepper}") String pepper) {
    return new HmacOtpHasher(pepper);
  }

  @Bean
  OtpDispatch otpDispatch() {
    return new LoggingOtpDispatch();
  }

  @Bean
  @ConditionalOnMissingBean(LinkPorts.Tokens.class)
  LinkPorts.Tokens linkTokens(@Value("${consent.link-pepper}") String pepper) {
    return new HmacLinkTokens(pepper);
  }

  @Bean
  @ConditionalOnMissingBean(LinkPorts.Dispatch.class)
  LinkPorts.Dispatch linkDispatch() {
    return new LoggingLinkDispatch();
  }

  @Bean
  LinkPorts.Origin publicOrigin(@Value("${consent.public-base-url}") String baseUrl) {
    return new ConfiguredPublicOrigin(baseUrl);
  }

  @Bean
  PublicLinkRateLimiter publicLinkRateLimiter(
      @Value("${consent.public-permits-per-minute:30}") int perMinute,
      @Value("${consent.public-permits-per-second:100}") int perSecond) {
    return new PublicLinkRateLimiter(perMinute, perSecond);
  }

  @Bean
  FilterRegistrationBean<PublicLinkRateLimitFilter> publicLinkRateLimitFilter(
      PublicLinkRateLimiter limiter) {
    FilterRegistrationBean<PublicLinkRateLimitFilter> registration =
        new FilterRegistrationBean<>(new PublicLinkRateLimitFilter(limiter));
    registration.addUrlPatterns("/public/v1/consent-links/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.setName("publicLinkRateLimitFilter");
    return registration;
  }

  @Bean
  SecurityEventSink securityEventSink() {
    return new LoggingSecurityEventSink();
  }

  /** The all-zero local key is refused when a production profile is active. */
  public static void rejectDevKey(String[] profiles, String fieldKey) {
    if (profiles == null || !AesFieldCipher.isDevKey(fieldKey)) {
      return;
    }
    for (String profile : profiles) {
      if ("prod".equals(profile) || "production".equals(profile)) {
        throw new IllegalStateException("dev field key refused for production");
      }
    }
  }
}
