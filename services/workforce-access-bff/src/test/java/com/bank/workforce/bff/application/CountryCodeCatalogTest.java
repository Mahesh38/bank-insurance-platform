package com.bank.workforce.bff.application;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@Tag("FUNC-029")
class CountryCodeCatalogTest {

    private final CountryCodeCatalog catalog = new CountryCodeCatalog();

    @Test
    void indiaRuleAcceptsValidMobileAndRejectsLandlinePrefix() {
        assertThat(catalog.list()).hasSize(1);
        assertThat(catalog.list().get(0).phoneCode()).isEqualTo("+91");
        assertThat(catalog.validate("IN", "9876543210").valid()).isTrue();
        assertThat(catalog.validate("IN", "1876543210").valid()).isFalse();
        assertThat(catalog.validate("IN", "98765").valid()).isFalse();
        assertThat(catalog.validate("US", "9876543210").reason()).isEqualTo("UNKNOWN_COUNTRY");
    }
}
