package com.bank.workforce.bff.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Set;

@Validated
@ConfigurationProperties("workforce.session")
public record WorkforceSessionProperties(
    @NotBlank String store,
    @NotBlank String cookieName,
    @NotBlank String encryptionKey,
    @NotNull Duration pendingLoginTtl,
    @NotNull Duration completionCodeTtl,
    @NotNull Duration sessionTtl,
    @NotEmpty Set<String> allowedReturnUris
) {}
