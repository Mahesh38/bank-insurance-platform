package com.bank.insurance.onesb.lob.life.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/**
 * Typed 1SB Life quote request body shared by Term, Savings and ULIP handlers.
 * Product family is selected via {@link Product#product()}; LOB-specific paths stay on the handler.
 * Replaces Map-based payload assembly ({@code REFACTOR-002} / {@code EPIC-002}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LifeQuoteRequest(
        String typeOfQuote,
        String quoteCategory,
        String includeBI,
        String outOfBoundConfig,
        AdditionalSetup additionalSetup,
        Distributor distributor,
        PersonalInformation personalInformation,
        Product product
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AdditionalSetup(String currency, String userCountry) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Distributor(String distributorID, String agentID, String channelType) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PersonalInformation(List<IndividualDetail> individualDetails) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IndividualDetail(
            String memberType,
            int memberSequenceNumber,
            String gender,
            String dateOfBirth,
            String tobacco,
            BigDecimal annualIncome,
            String zipCode,
            BigDecimal quoteAmount
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Product(String product) {}
}
