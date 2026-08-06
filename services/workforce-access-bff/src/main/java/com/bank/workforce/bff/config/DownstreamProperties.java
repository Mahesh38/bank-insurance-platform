package com.bank.workforce.bff.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties("workforce.downstream")
public record DownstreamProperties(
    @NotNull URI providerAdapterBaseUrl,
    @NotNull URI authorizationServiceBaseUrl
) {}
