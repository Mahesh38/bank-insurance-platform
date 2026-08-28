package com.bank.insurance.onesb.lob;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.insurance.onesb.domain.model.Lob;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LobProposalHandlerRegistryTest {

    @Test
    void get_registeredLob_returnsHandler() {
        LobProposalHandler termHandler = fakeHandler(Lob.TERM);
        LobProposalHandlerRegistry registry = new LobProposalHandlerRegistry(List.of(termHandler));

        assertThat(registry.get(Lob.TERM)).isSameAs(termHandler);
    }

    @Test
    void get_unregisteredLob_throwsUnsupportedLob() {
        LobProposalHandlerRegistry registry = new LobProposalHandlerRegistry(List.of(fakeHandler(Lob.TERM)));

        assertThatThrownBy(() -> registry.get(Lob.MOTOR))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.UNSUPPORTED_LOB));
    }

    @Test
    void get_nullLob_throwsUnsupportedLob() {
        LobProposalHandlerRegistry registry = new LobProposalHandlerRegistry(List.of());

        assertThatThrownBy(() -> registry.get(null))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    // `detail` is now the catalogue's fixed wording, one phrasing for this code
                    // everywhere. What is specific to this request travels in `errors[]`, which is
                    // where a caller looks to find out which field they got wrong.
                    assertThat(se.getErrorResponse().getErrors())
                            .singleElement()
                            .satisfies(fieldError -> {
                                assertThat(fieldError.field()).isEqualTo("lob");
                                assertThat(fieldError.message()).isEqualTo("lob is required");
                            });
                    assertThat(se.getDiagnostic().getReason()).isEqualTo("lob is required");
                });
    }

    @Test
    void constructor_duplicateHandlerForSameLob_throws() {
        assertThatThrownBy(() ->
                new LobProposalHandlerRegistry(List.of(fakeHandler(Lob.TERM), fakeHandler(Lob.TERM))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate LobProposalHandler for TERM");
    }

    private static LobProposalHandler fakeHandler(Lob lob) {
        return new LobProposalHandler() {
            @Override
            public Lob supportedLob() {
                return lob;
            }

            @Override
            public String schemaPath(String productCode, String manufacturerId, String version) {
                return "/fake/schema";
            }

            @Override
            public String submitPath() {
                return "/fake/submit";
            }

            @Override
            public String pollPath(String externalReqId) {
                return "/fake/poll/" + externalReqId;
            }

            @Override
            public Object buildSubmitPayload(SubmitProposalCommand command) {
                return null;
            }
        };
    }
}
