package com.bank.insurance.onesb.lob.life.term;

import com.bank.insurance.onesb.domain.model.Lob;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("FUNC-004")
class TermProposalHandlerTest {

    private final TermProposalHandler handler = new TermProposalHandler();

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
}
