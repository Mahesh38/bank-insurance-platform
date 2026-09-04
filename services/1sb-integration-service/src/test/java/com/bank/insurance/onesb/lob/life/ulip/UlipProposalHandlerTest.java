package com.bank.insurance.onesb.lob.life.ulip;

import com.bank.common.domain.Lob;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.insurance.onesb.lob.life.payload.LifeProposalSubmitBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("FUNC-019")
@ExtendWith(MockitoExtension.class)
class UlipProposalHandlerTest {

    @Mock SecretProvider secretProvider;

    private UlipProposalHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UlipProposalHandler(secretProvider);
    }

    @Test
    void supportedLob_isUlip() {
        assertThat(handler.supportedLob()).isEqualTo(Lob.ULIP);
    }

    @Test
    void paths_reuseLifesaveProposalPrefix() {
        assertThat(handler.schemaPath("U1", "MFG", "1"))
                .isEqualTo("/insurance/lifesave/v1/proposal?productId=U1&manufacturerId=MFG&version=1");
        assertThat(handler.submitPath()).isEqualTo("/insurance/lifesave/v1/proposal");
        assertThat(handler.pollPath("REQ-U"))
                .isEqualTo("/insurance/lifesave/v1/proposal/poll/REQ-U");
    }

    @Test
    void buildSubmitPayload_typedBodyWithServerDistributor() {
        when(secretProvider.getDistributorId()).thenReturn("BCIBL");

        SubmitProposalCommand command = new SubmitProposalCommand(
                Lob.ULIP, "scm-u", "off-u", "U1", "MFG", "1",
                Map.of("fund.code", "EQ1"), "consent-u", null,
                new SubmitProposalCommand.DistributionContext(null, "109337", "B2B"),
                "j-u", null, "idem-u", "actor-u"
        );

        LifeProposalSubmitBody payload = handler.buildSubmitPayload(command);

        assertThat(payload.getDistributor().distributorID()).isEqualTo("BCIBL");
        assertThat(payload.getDistributor().agentID()).isEqualTo("109337");
        assertThat(payload.formFields()).containsEntry("fund.code", "EQ1");
        assertThat(payload.getOfferId()).isEqualTo("off-u");
    }
}
