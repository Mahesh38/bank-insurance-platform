package com.bank.insurance.onesb.lob.life.payload;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed 1SB Life proposal submit body ({@code REFACTOR-002}).
 * <p>
 * Dynamic form fields from the bank {@code values} map are serialised at the root via
 * {@link JsonAnyGetter}; distributor attribution is a typed nested object and never taken
 * from the client.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class LifeProposalSubmitBody {

    private final Distributor distributor;
    private final String productCode;
    private final String manufacturerId;
    private final String version;
    private final String offerId;
    private final String schemaId;
    private final Map<String, Object> formFields;

    public LifeProposalSubmitBody(
            Distributor distributor,
            String productCode,
            String manufacturerId,
            String version,
            String offerId,
            String schemaId,
            Map<String, Object> formFields) {
        this.distributor = distributor;
        this.productCode = productCode;
        this.manufacturerId = manufacturerId;
        this.version = version;
        this.offerId = offerId;
        this.schemaId = schemaId;
        this.formFields = formFields == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(formFields));
    }

    public Distributor getDistributor() {
        return distributor;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getManufacturerId() {
        return manufacturerId;
    }

    public String getVersion() {
        return version;
    }

    public String getOfferId() {
        return offerId;
    }

    public String getSchemaId() {
        return schemaId;
    }

    @JsonAnyGetter
    public Map<String, Object> formFields() {
        return formFields;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Distributor(String distributorID, String agentID, String channelType) {}
}
