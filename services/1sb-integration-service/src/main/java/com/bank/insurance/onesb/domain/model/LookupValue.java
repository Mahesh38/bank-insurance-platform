package com.bank.insurance.onesb.domain.model;

/**
 * Normalised master-data enum entry (bank-facing {@code code}/{@code label}).
 */
public record LookupValue(String code, String label) {

    public static LookupValue of(String value) {
        return new LookupValue(value, value);
    }
}
