package com.bank.platform.customer.domain;

import java.util.List;
import java.util.Optional;

public interface CustomerInquiryPort {

    List<CustomerHit> search(SearchQuery query, String accessToken);

    Optional<CustomerHit> findById(String customerId, String accessToken);

    record SearchQuery(String by, String value, String countryCode) {}

    record CustomerHit(
        String customerId,
        String displayName,
        String initials,
        String maskedCif,
        String maskedMobile,
        String eligibility,
        String source
    ) {}
}
