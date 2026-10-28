package com.bank.platform.quotation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * When {@code stub=true} (local/test), suitability and consent gates always allow. Documented as
 * {@code quotation.gates.stub}.
 */
@Validated
@ConfigurationProperties("quotation.gates")
public record QuotationGatesProperties(boolean stub) {}
