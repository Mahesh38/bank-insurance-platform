package com.bank.workforce.bff.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("workforce.downstream")
public record DownstreamProperties(
    @NotNull URI providerAdapterBaseUrl,
    @NotNull URI authorizationServiceBaseUrl,
    @NotNull URI leadServiceBaseUrl,
    @NotNull URI customerServiceBaseUrl,
    @NotBlank String leadMode,
    @NotBlank String customerMode) {}
