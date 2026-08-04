package com.bank.insurance.onesb.lob;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.Lob;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LobQuoteHandlerRegistryTest {

    @Test
    void get_registeredLob_returnsHandler() {
        LobQuoteHandler termHandler = fakeHandler(Lob.TERM);
        LobQuoteHandlerRegistry registry = new LobQuoteHandlerRegistry(List.of(termHandler));

        assertThat(registry.get(Lob.TERM)).isSameAs(termHandler);
    }

    @Test
    void get_unregisteredLob_throwsUnsupportedLob() {
        LobQuoteHandlerRegistry registry = new LobQuoteHandlerRegistry(List.of(fakeHandler(Lob.TERM)));

        assertThatThrownBy(() -> registry.get(Lob.HEALTH))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.UNSUPPORTED_LOB));
    }

    @Test
    void get_nullLob_throwsUnsupportedLob() {
        LobQuoteHandlerRegistry registry = new LobQuoteHandlerRegistry(List.of());

        assertThatThrownBy(() -> registry.get(null))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getDetail())
                        .isEqualTo("lob is required"));
    }

    @Test
    void constructor_duplicateHandlerForSameLob_throws() {
        assertThatThrownBy(() -> new LobQuoteHandlerRegistry(List.of(fakeHandler(Lob.TERM), fakeHandler(Lob.TERM))))
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
