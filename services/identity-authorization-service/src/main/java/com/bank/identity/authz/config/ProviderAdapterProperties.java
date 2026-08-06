package com.bank.identity.authz.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties("identity.provider-adapter")
public record ProviderAdapterProperties(
    @NotNull URI baseUrl
) {}
