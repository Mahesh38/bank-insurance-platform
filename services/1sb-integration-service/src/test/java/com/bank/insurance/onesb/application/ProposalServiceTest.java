package com.bank.insurance.onesb.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.ProposalSchema;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbProposalPort;
import com.bank.insurance.onesb.lob.LobProposalHandler;
import com.bank.insurance.onesb.lob.LobProposalHandlerRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("FUNC-004")
@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock JobStorePort jobStore;
    @Mock LobProposalHandlerRegistry handlerRegistry;
    @Mock OneSbProposalPort proposalPort;
    @Mock LobProposalHandler handler;

    @InjectMocks ProposalService proposalService;

    @Test
    void getSchema_delegatesToHandlerAndPort() {
        when(handlerRegistry.get(Lob.TERM)).thenReturn(handler);
        when(handler.schemaPath("T1", "HDFC", "1"))
                .thenReturn("/insurance/lifeterm/v1/proposal/form?productCode=T1&manufacturerId=HDFC&version=1");
        ProposalSchema expected = new ProposalSchema(Lob.TERM, "T1", "HDFC", "1", Map.of("a", 1));
        when(proposalPort.getSchema(eq(Lob.TERM), eq("T1"), eq("HDFC"), eq("1"), any()))
                .thenReturn(expected);

        ProposalSchema result = proposalService.getSchema(Lob.TERM, "T1", "HDFC", "1", null);

        assertThat(result).isSameAs(expected);
        verify(jobStore, never()).findQuoteJob(any());
    }

    @Test
    void getSchema_missingJob_throwsQuoteExpired() {
        when(jobStore.findQuoteJob("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.getSchema(Lob.TERM, "T1", "H", "1", "gone"))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getHttpStatus()).isEqualTo(410);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.QUOTE_EXPIRED);
                });
        verify(proposalPort, never()).getSchema(any(), any(), any(), any(), any());
    }

    @Test
    void getSchema_timeoutJob_throwsQuoteExpired() {
        when(jobStore.findQuoteJob("t")).thenReturn(Optional.of(new QuoteJob(
                "t", JobStatus.TIMEOUT, "POLL_TIMEOUT", Lob.TERM, null,
                List.of(), List.of(), Instant.now(), Instant.now()
        )));

        assertThatThrownBy(() -> proposalService.getSchema(Lob.TERM, null, null, null, "t"))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.QUOTE_EXPIRED));
    }
}
