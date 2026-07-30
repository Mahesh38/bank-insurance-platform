package com.bank.persistence.config;

import com.bank.persistence.crypto.AesGcmPayloadCipher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PayloadEncryptionProperties.class)
public class PayloadEncryptionConfig {

    @Bean
    AesGcmPayloadCipher aesGcmPayloadCipher(PayloadEncryptionProperties properties) {
        return new AesGcmPayloadCipher(properties.decodedKey());
    }
}
