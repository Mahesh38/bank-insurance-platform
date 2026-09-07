package com.bank.insurance.onesb.lob.life.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

/**
 * Typed 1SB Life quote request body shared by Term, Savings and ULIP handlers.
 * <p>
 * Portal alignment ({@code insurance-gateway-api} / retail LOB pages):
 * request {@code product.productType} is {@code LifeTerm} or {@code LifeSave};
 * Savings/ULIP filter via {@code product.savingsProductType} ({@code nonParticipating}|{@code Participating}|{@code ULIP}).
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

    /**
     * @param productType          1SB LOB family token ({@code LifeTerm}, {@code LifeSave}, …)
     * @param savingsProductType   Saving filters; use {@code ULIP} for ULIP quotes on the lifesave API
     * @param product              Legacy alias some Term fixtures used ({@code product.product}); prefer {@code productType}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Product(
            String productType,
            List<String> savingsProductType,
            String product
    ) {
        public static Product term(String token) {
            return new Product(token, null, token);
        }

        public static Product saving(String token, List<String> savingsTypes) {
            return new Product(token, savingsTypes, null);
        }
    }
}
