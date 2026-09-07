package com.bank.insurance.onesb.lob.life.saving;

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

@Tag("FUNC-015")
@ExtendWith(MockitoExtension.class)
class SavingProposalHandlerTest {

    @Mock SecretProvider secretProvider;

    private SavingProposalHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SavingProposalHandler(secretProvider);
    }

    @Test
    void supportedLob_isSaving() {
        assertThat(handler.supportedLob()).isEqualTo(Lob.SAVING);
    }

    @Test
    void schemaSubmitAndPollPaths_useLifesavePrefix() {
        assertThat(handler.schemaPath("S1", "MFG", "2"))
                .isEqualTo("/insurance/lifesave/v1/proposal?productId=S1&manufacturerId=MFG&version=2");
        assertThat(handler.submitPath()).isEqualTo("/insurance/lifesave/v1/proposal");
        assertThat(handler.pollPath("REQ-S"))
                .isEqualTo("/insurance/lifesave/v1/proposal/poll/REQ-S");
    }

    @Test
    void buildSubmitPayload_typedBodyWithServerDistributor() {
        when(secretProvider.getDistributorId()).thenReturn("BCIBL");

        SubmitProposalCommand command = new SubmitProposalCommand(
                Lob.SAVING, "scm-s", "off-s", "S1", "MFG", "2",
                Map.of("proposer.panNumber", "ABCDE1234F", "distributorId", "SPOOF"),
                "consent-s", "agent-top",
                new SubmitProposalCommand.DistributionContext("E1", "ignored", "B2B"),
                "j-s", null, "idem-s", "actor-s"
        );

        LifeProposalSubmitBody payload = handler.buildSubmitPayload(command);

        assertThat(payload.getDistributor().distributorID()).isEqualTo("BCIBL");
        assertThat(payload.getDistributor().agentID()).isEqualTo("agent-top");
        assertThat(payload.formFields()).containsEntry("proposer.panNumber", "ABCDE1234F");
        assertThat(payload.formFields()).doesNotContainKey("distributorId");
        assertThat(payload.getProductCode()).isEqualTo("S1");
    }
}
