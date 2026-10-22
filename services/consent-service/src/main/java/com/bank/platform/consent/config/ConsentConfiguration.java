package com.bank.platform.consent.config;

import com.bank.platform.consent.adapter.crypto.AesFieldCipher;
import com.bank.platform.consent.adapter.crypto.HmacOtpHasher;
import com.bank.platform.consent.adapter.otp.LoggingOtpDispatch;
import com.bank.platform.consent.adapter.security.LoggingSecurityEventSink;
import com.bank.platform.consent.domain.ConsentEvaluation;
import com.bank.platform.consent.domain.ConsentPolicyRegistry;
import com.bank.platform.consent.domain.FieldCipher;
import com.bank.platform.consent.domain.OtpDispatch;
import com.bank.platform.consent.domain.OtpHasher;
import com.bank.platform.consent.domain.SecurityEventSink;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ConsentConfiguration {

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  ConsentPolicyRegistry consentPolicyRegistry() {
    return ConsentPolicyRegistry.r0();
  }

  @Bean
  ConsentEvaluation consentEvaluation(ConsentPolicyRegistry registry) {
    return new ConsentEvaluation(registry);
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
