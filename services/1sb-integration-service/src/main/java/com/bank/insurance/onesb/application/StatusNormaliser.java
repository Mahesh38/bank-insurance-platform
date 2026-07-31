package com.bank.insurance.onesb.application;

import com.bank.insurance.onesb.domain.model.BankStage;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * Maps 1SB {@code applicationStatus} values to bank {@link BankStage} (FUNC-009).
 * Case-insensitive; unknown values → {@link BankStage#UNKNOWN}.
 */
public final class StatusNormaliser {

    private static final Set<String> CLOSED = Set.of(
            "REJECTED", "CANCELLED", "PROPOSAL_ERROR");

    private static final Set<String> CUSTOMER_DECISION = Set.of(
            "COUNTER_OFFER", "AWAITING_CLIENT_APPROVAL", "ACCEPTED");

    private static final Set<String> UW_IN_PROGRESS = Set.of(
            "UNDERWRITING", "SCRUTINY", "PRE_CONVERSION");

    private static final Set<String> QUOTING = Set.of(
            "QUOTE_CREATED", "QUOTE_UPDATED");

    private StatusNormaliser() {}

    /**
     * Map a 1SB applicationStatus string to {@link BankStage}.
     * Blank / null → {@link BankStage#UNKNOWN}.
     */
    public static BankStage toBankStage(String onesbStatus) {
        if (!StringUtils.hasText(onesbStatus)) {
            return BankStage.UNKNOWN;
        }
        String status = onesbStatus.trim().toUpperCase(Locale.ROOT);

        if (CLOSED.contains(status)) {
            return BankStage.CLOSED;
        }
        if ("POLICY_ISSUED".equals(status)) {
            return BankStage.ISSUED;
        }
        if (status.startsWith("PAYMENT_")) {
            return BankStage.PAYMENT;
        }
        if (CUSTOMER_DECISION.contains(status)) {
            return BankStage.CUSTOMER_DECISION;
        }
        if (UW_IN_PROGRESS.contains(status)) {
            return BankStage.UW_IN_PROGRESS;
        }
        if (isUwRequirements(status)) {
            return BankStage.UW_REQUIREMENTS;
        }
        if (status.startsWith("PROPOSAL_")) {
            return BankStage.PROPOSAL;
        }
        if ("QUOTE_SELECTED".equals(status)) {
            return BankStage.QUOTE_SELECTED;
        }
        if (QUOTING.contains(status) || status.startsWith("QUOTE_")) {
            return BankStage.QUOTING;
        }
        return BankStage.UNKNOWN;
    }

    private static boolean isUwRequirements(String status) {
        return status.startsWith("REQUIREMENTS_")
                || status.startsWith("DOCUMENT_")
                || status.startsWith("DOCUMENTS_")
                || status.startsWith("OTP_")
                || status.startsWith("KYC_")
                || status.startsWith("INSPECTION_")
                || "REQUIREMENT_VERIFICATION".equals(status);
    }
}
