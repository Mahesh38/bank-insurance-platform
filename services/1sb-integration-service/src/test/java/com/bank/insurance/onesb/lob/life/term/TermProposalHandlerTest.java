package com.bank.insurance.onesb.lob.life.term;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.common.domain.Lob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("FUNC-004")
@Tag("FUNC-005")
@ExtendWith(MockitoExtension.class)
class TermProposalHandlerTest {

    @Mock SecretProvider secretProvider;

    private TermProposalHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TermProposalHandler(secretProvider);
    }

    @Test
    void supportedLob_isTerm() {
        assertThat(handler.supportedLob()).isEqualTo(Lob.TERM);
    }

    @Test
    void schemaPath_includesQueryParams() {
        String path = handler.schemaPath("T1", "HDFC", "1");
        assertThat(path).startsWith("/insurance/lifeterm/v1/proposal/form?");
        assertThat(path).contains("productCode=T1");
        assertThat(path).contains("manufacturerId=HDFC");
        assertThat(path).contains("version=1");
    }

    @Test
    void schemaPath_omitsBlankParams() {
        assertThat(handler.schemaPath(null, null, null))
                .isEqualTo("/insurance/lifeterm/v1/proposal/form");
        assertThat(handler.schemaPath("T1", null, ""))
                .isEqualTo("/insurance/lifeterm/v1/proposal/form?productCode=T1");
    }

    @Test
    @Tag("FUNC-005")
    void submitAndPollPaths() {
        assertThat(handler.submitPath()).isEqualTo("/insurance/lifeterm/v1/proposal");
        assertThat(handler.pollPath("REQ-1"))
                .isEqualTo("/insurance/lifeterm/v1/proposal/poll/REQ-1");
    }

    @Test
    @Tag("FUNC-005")
    @SuppressWarnings("unchecked")
    void buildSubmitPayload_injectsDistributorFromSecrets_ignoresClientDistributorId() {
        when(secretProvider.getDistributorId()).thenReturn("BCIBL");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("proposer.panNumber", "ABCDE1234F");
        values.put("distributorId", "SPOOFED");
        values.put("distributor", Map.of("distributorID", "CLIENT_DIST", "agentID", "ignored"));

        SubmitProposalCommand command = new SubmitProposalCommand(
                Lob.TERM, "scm-1", "off-1", "T1", "HDFC", "1",
                values, "consent-1", null,
                new SubmitProposalCommand.DistributionContext("E1", "109337", "B2B"),
                "j-1", null, "idem-1", "actor-1"
        );

        Map<String, Object> payload = (Map<String, Object>) handler.buildSubmitPayload(command);

        assertThat(payload).doesNotContainKey("distributorId");
        assertThat(payload.get("proposer.panNumber")).isEqualTo("ABCDE1234F");
        @SuppressWarnings("unchecked")
        Map<String, Object> distributor = (Map<String, Object>) payload.get("distributor");
        assertThat(distributor.get("distributorID")).isEqualTo("BCIBL");
        assertThat(distributor.get("agentID")).isEqualTo("109337");
        assertThat(distributor.get("channelType")).isEqualTo("B2B");
    }
}
