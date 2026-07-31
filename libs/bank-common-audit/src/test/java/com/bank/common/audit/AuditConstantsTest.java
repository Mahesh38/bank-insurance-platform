package com.bank.common.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditConstantsTest {

    @Test
    void auditActions_areStable() {
        assertThat(AuditActions.QUOTE_JOB_CREATED).isEqualTo("QUOTE_JOB_CREATED");
        assertThat(AuditActions.ONESB_OUTBOUND_CALL).isEqualTo("ONESB_OUTBOUND_CALL");
        assertThat(AuditActions.PAYLOAD_RETENTION_DELETED).isEqualTo("PAYLOAD_RETENTION_DELETED");
        assertThat(AuditActions.CONSENT_REF_MISSING).isEqualTo("CONSENT_REF_MISSING");
        assertThat(AuditActions.PROPOSAL_SUBMITTED).isEqualTo("PROPOSAL_SUBMITTED");
    }

    @Test
    void auditAction_aliases_matchActionsWhereShared() {
        assertThat(AuditAction.QUOTE_JOB_CREATED).isEqualTo(AuditActions.QUOTE_JOB_CREATED);
        assertThat(AuditAction.DOCUMENT_UPLOADED).isEqualTo(AuditActions.DOCUMENT_UPLOADED);
    }

    @Test
    void auditOutcomes_areStable() {
        assertThat(AuditOutcomes.SUCCESS).isEqualTo("SUCCESS");
        assertThat(AuditOutcomes.FAILURE).isEqualTo("FAILURE");
        assertThat(AuditOutcomes.REJECTED).isEqualTo("REJECTED");
        assertThat(AuditOutcomes.TIMEOUT).isEqualTo("TIMEOUT");
        assertThat(AuditOutcomes.PENDING).isEqualTo("PENDING");
        assertThat(AuditOutcomes.WARN).isEqualTo("WARN");
    }
}
