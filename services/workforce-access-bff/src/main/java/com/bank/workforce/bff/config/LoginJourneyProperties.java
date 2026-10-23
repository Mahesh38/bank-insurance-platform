package com.bank.workforce.bff.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("workforce.login")
public record LoginJourneyProperties(
    @NotNull Duration captchaTtl,
    @NotNull Duration otpTtl,
    @NotNull Duration resendDelay,
    @Min(1) int maxOtpAttempts,
    @Min(1) int maxResends,
    @NotNull Duration continuationTtl) {}
