package com.bank.workforce.bff.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("workforce.session")
public record WorkforceSessionProperties(
    @NotBlank String store,
    @NotBlank String cookieName,
    @NotBlank String encryptionKey,
    @NotNull Duration pendingLoginTtl,
    @NotNull Duration completionCodeTtl,
    @NotNull Duration sessionTtl,
    @NotEmpty Set<String> allowedReturnUris,
    /**
     * {@code Secure} on {@code WORKFORCE_SESSION}. Defaults to true. The dev profile sets false so
     * a browser on plain HTTP can store the cookie. {@code prod} and {@code uat} refuse to start
     * when this is false.
     */
    @DefaultValue("true") boolean cookieSecure) {}
