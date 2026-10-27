package com.bank.platform.suitability.config;

import com.bank.platform.suitability.adapter.crypto.AesFieldCipher;
import com.bank.platform.suitability.adapter.link.HttpConsentLinkClient;
import com.bank.platform.suitability.domain.JourneyModel.ConsentLinks;
import com.bank.platform.suitability.domain.JourneyModel.FieldCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
public class SuitabilityConfiguration {

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  Clock systemClock() {
    return Clock.systemUTC();
  }

  @Bean
  FieldCipher fieldCipher(Environment environment, JourneyProperties properties) {
    rejectDevKey(environment.getActiveProfiles(), properties.getFieldKey());
    return new AesFieldCipher(properties.getFieldKey(), properties.getKeyId());
  }

  @Bean
  @ConditionalOnProperty(
      name = "suitability.journey.consent-client",
      havingValue = "http",
      matchIfMissing = true)
  ConsentLinks consentLinks(JourneyProperties properties, ObjectMapper mapper) {
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    factory.setReadTimeout(Duration.ofSeconds(3));
    String base = properties.getConsentBaseUrl();
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
    RestClient client =
        RestClient.builder()
            .baseUrl(base)
            .requestFactory(factory)
            .messageConverters(
                converters -> {
                  converters.clear();
                  converters.add(new MappingJackson2HttpMessageConverter(mapper));
                })
            .build();
    return new HttpConsentLinkClient(client, properties.getConsentInternalKey());
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
