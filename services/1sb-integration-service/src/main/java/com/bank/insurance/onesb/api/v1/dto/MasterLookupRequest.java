package com.bank.insurance.onesb.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Bank-facing request for {@code POST /v1/master-data/lookup}.
 */
public record MasterLookupRequest(
        @NotBlank(message = "lob is required") String lob,
        String lookUpCategory,
        @NotEmpty(message = "entityIds must not be empty") List<@NotBlank(message = "entityId must not be blank") String> entityIds,
        String manufacturerId
) {
    public MasterLookupRequest {
        if (lookUpCategory == null || lookUpCategory.isBlank()) {
            lookUpCategory = "quote";
        }
    }
}
