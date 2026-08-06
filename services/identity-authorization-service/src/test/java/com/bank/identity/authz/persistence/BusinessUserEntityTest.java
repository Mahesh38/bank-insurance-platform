package com.bank.identity.authz.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessUserEntityTest {

    @Test
    void partnerBecomesActiveOnlyAfterProviderProvisioning() {
        var user = BusinessUserEntity.pendingPartner(
            "partner.entity", "partner.entity@example.test", "Partner", "Entity", "ICICI_PRU");

        user.approve();
        assertThat(user.getStatus().name()).isEqualTo("PENDING");
        assertThat(user.getPolicyVersion()).isEqualTo(2);

        user.markProvisioned("provider-subject");
        assertThat(user.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(user.getProviderSubject()).isEqualTo("provider-subject");
        assertThat(user.getPolicyVersion()).isEqualTo(3);
    }
}
