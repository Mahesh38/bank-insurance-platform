package com.bank.insurance.onesb.application.validation;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Closed Life contract enums from the 1SB Insurance Gateway (Saving Get-quote page,
 * 2026-10-03) plus bank aliases. Product-specific lists are <em>not</em> here.
 * <p>
 * Work item: FUNC-028 / SUG-20261003-lvr
 */
public final class LifeContractCatalog {

    public static final Set<String> QUOTE_TYPES = Set.of("SINGLE QUOTE", "MULTI-QUOTE", "MULTI QUOTE");
    public static final Set<String> QUOTE_CATEGORIES = Set.of("PREMIUM", "SUM ASSURED", "INCOME");
    public static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "TRANSGENDER", "OTHERS");
    public static final Set<String> TOBACCO = Set.of("YES", "NO");
    public static final Set<String> CHANNEL_TYPES = Set.of("B2B", "B2C");
    public static final Set<String> SALES_CHANNELS = Set.of("ONLINE", "OTHERS");
    public static final Set<String> MEMBER_TYPES = Set.of("LIFE ASSURED", "PROPOSER");
    public static final Set<String> FREQUENCIES = Set.of("M", "Q", "HY", "Y", "S",
            "MONTHLY", "QUARTERLY", "HALF YEARLY", "HALF-YEARLY", "YEARLY", "SINGLE");
    public static final Set<String> SAVINGS_PRODUCT_TYPES = Set.of("NONPARTICIPATING", "PARTICIPATING", "ULIP");
    public static final Set<String> YES_NO = Set.of("YES", "NO");
    public static final Set<String> OFFER_STATUSES = Set.of(
            "AVAILABLE", "ERROR", "SUCCESS", "COMPLETED", "PENDING", "PROCESSING",
            "FAILED", "REJECTED", "OOB", "OUTOFBOUND", "OUT_OF_BOUND");
    public static final Set<String> POLL_TERMINAL_HINTS = Set.of(
            "COMPLETED", "FAILED", "REJECTED", "ERROR", "SUCCESS", "TIMEOUT");

    private static final Map<String, String> GENDER_WIRE = Map.of(
            "M", "Male",
            "MALE", "Male",
            "F", "Female",
            "FEMALE", "Female",
            "TRANSGENDER", "Transgender",
            "OTHERS", "Others",
            "OTHER", "Others");

    private static final Map<String, String> FREQUENCY_WIRE = Map.ofEntries(
            Map.entry("M", "M"),
            Map.entry("MONTHLY", "M"),
            Map.entry("Q", "Q"),
            Map.entry("QUARTERLY", "Q"),
            Map.entry("HY", "HY"),
            Map.entry("HALF YEARLY", "HY"),
            Map.entry("HALF-YEARLY", "HY"),
            Map.entry("Y", "Y"),
            Map.entry("YEARLY", "Y"),
            Map.entry("S", "S"),
            Map.entry("SINGLE", "S"));

    private static final Map<String, String> SAVINGS_WIRE = Map.of(
            "NONPARTICIPATING", "nonParticipating",
            "NON-PARTICIPATING", "nonParticipating",
            "PARTICIPATING", "Participating",
            "ULIP", "ULIP");

    private LifeContractCatalog() {}

    public static String key(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT).replace('_', ' ');
    }

    public static Optional<String> genderWire(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String k = key(raw).replace(" ", "");
        if ("M".equals(k) || "MALE".equals(k)) {
            return Optional.of("Male");
        }
        if ("F".equals(k) || "FEMALE".equals(k)) {
            return Optional.of("Female");
        }
        return Optional.ofNullable(GENDER_WIRE.get(key(raw)));
    }

    public static boolean isKnownGender(String raw) {
        return genderWire(raw).isPresent();
    }

    public static boolean isKnownCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String k = key(raw);
        return QUOTE_CATEGORIES.contains(k) || "SUM_ASSURED".equals(raw.trim().toUpperCase(Locale.ROOT));
    }

    public static boolean isKnownMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String k = key(raw).replace(' ', '-').replace('_', '-');
        return "SINGLE".equals(k) || "SINGLE-QUOTE".equals(k) || "SQ".equals(k)
                || "MULTI".equals(k) || "MULTI-QUOTE".equals(k) || "MQ".equals(k);
    }

    public static boolean isKnownChannel(String raw) {
        return raw == null || raw.isBlank() || CHANNEL_TYPES.contains(key(raw));
    }

    public static Optional<String> frequencyWire(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String k = key(raw);
        return Optional.ofNullable(FREQUENCY_WIRE.get(k));
    }

    public static boolean isKnownFrequency(String raw) {
        return raw == null || raw.isBlank() || frequencyWire(raw).isPresent();
    }

    public static Optional<String> savingsProductTypeWire(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String k = key(raw).replace(" ", "").replace("-", "");
        if ("NONPARTICIPATING".equals(k)) {
            return Optional.of("nonParticipating");
        }
        if ("PARTICIPATING".equals(k)) {
            return Optional.of("Participating");
        }
        if ("ULIP".equals(k)) {
            return Optional.of("ULIP");
        }
        return Optional.ofNullable(SAVINGS_WIRE.get(key(raw)));
    }

    public static boolean isKnownOfferStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        return OFFER_STATUSES.contains(key(raw).replace(" ", "").replace("-", "_"));
    }

    public static boolean isKnownMemberType(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String k = key(raw);
        return MEMBER_TYPES.contains(k)
                || "LIFE_ASSURED".equals(raw.trim().toUpperCase(Locale.ROOT))
                || "LA".equals(k);
    }
}
