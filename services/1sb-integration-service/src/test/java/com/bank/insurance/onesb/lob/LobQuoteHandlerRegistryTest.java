package com.bank.insurance.onesb.lob;

import com.bank.insurance.onesb.TestErrors;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.common.domain.Lob;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LobQuoteHandlerRegistryTest {

    @Test
    void get_registeredLob_returnsHandler() {
        LobQuoteHandler termHandler = fakeHandler(Lob.TERM);
        LobQuoteHandlerRegistry registry = new LobQuoteHandlerRegistry(List.of(termHandler), TestErrors.ONESB);

        assertThat(registry.get(Lob.TERM)).isSameAs(termHandler);
    }

    @Test
    void get_unregisteredLob_throwsUnsupportedLob() {
        LobQuoteHandlerRegistry registry = new LobQuoteHandlerRegistry(List.of(fakeHandler(Lob.TERM)), TestErrors.ONESB);

        assertThatThrownBy(() -> registry.get(Lob.HEALTH))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.UNSUPPORTED_LOB));
    }

    @Test
    void get_nullLob_throwsUnsupportedLob() {
        LobQuoteHandlerRegistry registry = new LobQuoteHandlerRegistry(List.of(), TestErrors.ONESB);

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
        assertThatThrownBy(() -> new LobQuoteHandlerRegistry(List.of(fakeHandler(Lob.TERM), fakeHandler(Lob.TERM)), TestErrors.ONESB))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate LobQuoteHandler for TERM");
    }

    private static LobQuoteHandler fakeHandler(Lob lob) {
        return new LobQuoteHandler() {
            @Override
            public Lob supportedLob() {
                return lob;
            }

            @Override
            public Object buildSubmitPayload(CreateQuoteCommand command) {
                return null;
            }

            @Override
            public String submitPath() {
                return "/fake/submit";
            }

            @Override
            public String pollPath(String externalReqId) {
                return "/fake/poll/" + externalReqId;
            }
        };
    }
}
