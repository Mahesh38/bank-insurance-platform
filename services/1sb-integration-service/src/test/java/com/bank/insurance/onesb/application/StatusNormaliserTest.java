package com.bank.insurance.onesb.application;

import com.bank.insurance.onesb.domain.model.BankStage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("FUNC-009")
class StatusNormaliserTest {

    @ParameterizedTest
    @CsvSource({
            "QUOTE_CREATED, QUOTING",
            "QUOTE_UPDATED, QUOTING",
            "quote_created, QUOTING",
            "QUOTE_SELECTED, QUOTE_SELECTED",
            "quote_selected, QUOTE_SELECTED",
            "PROPOSAL_APPLICATION_PENDING, PROPOSAL",
            "PROPOSAL_SUBMISSION_INITIATED, PROPOSAL",
            "PROPOSAL_SUBMITTED, PROPOSAL",
            "PROPOSAL_MODIFICATION_REQUESTED, PROPOSAL",
            "REQUIREMENTS_PENDING, UW_REQUIREMENTS",
            "DOCUMENT_UPLOAD_INITIATED, UW_REQUIREMENTS",
            "DOCUMENT_UPLOAD_PENDING, UW_REQUIREMENTS",
            "DOCUMENTS_UPLOADED, UW_REQUIREMENTS",
            "REQUIREMENT_VERIFICATION, UW_REQUIREMENTS",
            "OTP_VERIFIED, UW_REQUIREMENTS",
            "KYC_SUCCESS, UW_REQUIREMENTS",
            "KYC_FAILED, UW_REQUIREMENTS",
            "INSPECTION_PENDING, UW_REQUIREMENTS",
            "INSPECTION_APPROVED, UW_REQUIREMENTS",
            "UNDERWRITING, UW_IN_PROGRESS",
            "SCRUTINY, UW_IN_PROGRESS",
            "PRE_CONVERSION, UW_IN_PROGRESS",
            "COUNTER_OFFER, CUSTOMER_DECISION",
            "AWAITING_CLIENT_APPROVAL, CUSTOMER_DECISION",
            "ACCEPTED, CUSTOMER_DECISION",
            "PAYMENT_INITIATED, PAYMENT",
            "PAYMENT_SUCCESS, PAYMENT",
            "PAYMENT_FAILURE, PAYMENT",
            "PAYMENT_INTIMATION_FAILED, PAYMENT",
            "POLICY_ISSUED, ISSUED",
            "REJECTED, CLOSED",
            "CANCELLED, CLOSED",
            "PROPOSAL_ERROR, CLOSED",
            "SOMETHING_WEIRD, UNKNOWN",
            "FOO_BAR, UNKNOWN"
    })
    void mapsOnesbStatusToBankStage(String onesb, BankStage expected) {
        assertThat(StatusNormaliser.toBankStage(onesb)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void blankOrNull_mapsToUnknown(String onesb) {
        assertThat(StatusNormaliser.toBankStage(onesb)).isEqualTo(BankStage.UNKNOWN);
    }

    @Test
    void proposalError_isClosed_notProposal() {
        assertThat(StatusNormaliser.toBankStage("PROPOSAL_ERROR")).isEqualTo(BankStage.CLOSED);
        assertThat(StatusNormaliser.toBankStage("PROPOSAL_SUBMITTED")).isEqualTo(BankStage.PROPOSAL);
    }
}
