package com.bank.insurance.onesb.application;

import com.bank.common.domain.Lob;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.TestErrors;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.UlipFundResult;
import com.bank.insurance.onesb.domain.port.outbound.OneSbUlipFundPort;
import com.bank.insurance.onesb.lob.LobQuoteHandler;
import com.bank.insurance.onesb.lob.LobQuoteHandlerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("FUNC-027")
@ExtendWith(MockitoExtension.class)
class UlipFundServiceTest {

    @Mock OneSbUlipFundPort port;
    @Mock LobQuoteHandler ulipHandler;

    private UlipFundService service;

    @BeforeEach
    void setUp() {
        when(ulipHandler.supportedLob()).thenReturn(Lob.ULIP);
        LobQuoteHandlerRegistry registry = new LobQuoteHandlerRegistry(List.of(ulipHandler), TestErrors.ONESB);
        service = new UlipFundService(registry, port, TestErrors.ONESB);
    }

    @Test
    void listFunds_buildsUlipPayload_andDelegates() {
        Object payload = Map.of("productType", "LifeSave");
        when(ulipHandler.buildSubmitPayload(any())).thenReturn(payload);
        UlipFundResult expected = new UlipFundResult("REQ-L", Map.of("funds", List.of()));
        when(port.listFunds(payload)).thenReturn(expected);

        assertThat(service.listFunds(listCommand(Lob.ULIP, false))).isSameAs(expected);
        verify(port).listFunds(payload);
        verify(port, never()).fundPerformance(any());
    }

    @Test
    void fundPerformance_withoutPin_throws422_noUpstream() {
        assertThatThrownBy(() -> service.fundPerformance(listCommand(Lob.ULIP, false)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.VALIDATION_ERROR));
        verify(port, never()).fundPerformance(any());
        verify(ulipHandler, never()).buildSubmitPayload(any());
    }

    @Test
    void fundPerformance_withPin_delegates() {
        Object payload = Map.of("pinned", true);
        when(ulipHandler.buildSubmitPayload(any())).thenReturn(payload);
        UlipFundResult expected = new UlipFundResult("REQ-P", Map.of("performance", List.of()));
        when(port.fundPerformance(payload)).thenReturn(expected);

        assertThat(service.fundPerformance(listCommand(Lob.ULIP, true))).isSameAs(expected);
        verify(port).fundPerformance(payload);
    }

    @Test
    void listFunds_missingMembersOrSumAssured_throws422_noUpstream() {
        CreateQuoteCommand noMembers = new CreateQuoteCommand(
                Lob.ULIP, "MULTI", "SUM_ASSURED", new BigDecimal("500000"), null,
                List.of(), Map.of(), null, "j-1", null, "idem", "rm", null);
        assertThatThrownBy(() -> service.listFunds(noMembers))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.VALIDATION_ERROR));

        CreateQuoteCommand noSum = new CreateQuoteCommand(
                Lob.ULIP, "MULTI", "SUM_ASSURED", null, null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LA", 1, "1990-01-15", "M", false, new BigDecimal("1"), "400001")),
                Map.of(), null, "j-1", null, "idem", "rm", null);
        assertThatThrownBy(() -> service.listFunds(noSum))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.VALIDATION_ERROR));

        verify(port, never()).listFunds(any());
    }

    @Test
    void fundPerformance_blankProductCodes_throws422_noUpstream() {
        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.ULIP, "MULTI", "SUM_ASSURED", new BigDecimal("500000"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LA", 1, "1990-01-15", "M", false, new BigDecimal("1"), "400001")),
                Map.of(), null, "j-1", null, "idem", "rm",
                new CreateQuoteCommand.ProductSelection(
                        "BALIC", List.of(" ", ""), null, null, null, null, null, null, null));
        assertThatThrownBy(() -> service.fundPerformance(command))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.VALIDATION_ERROR));
        verify(port, never()).fundPerformance(any());
    }

    @Test
    void listFunds_nullLob_throwsUnsupported_noUpstream() {
        assertThatThrownBy(() -> service.listFunds(listCommand(null, false)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.UNSUPPORTED_LOB));
        verify(port, never()).listFunds(any());
    }

    @Test
    void listFunds_termLob_throwsUnsupported_noUpstream() {
        assertThatThrownBy(() -> service.listFunds(listCommand(Lob.TERM, false)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.UNSUPPORTED_LOB));
        verify(port, never()).listFunds(any());
    }

    @Test
    void listFunds_nullMembers_throws422_noUpstream() {
        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.ULIP, "MULTI", "SUM_ASSURED", new BigDecimal("500000"), null,
                null, Map.of(), null, "j-1", null, "idem", "rm", null);
        assertThatThrownBy(() -> service.listFunds(command))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.VALIDATION_ERROR));
        verify(port, never()).listFunds(any());
    }

    @Test
    void fundPerformance_nullProductCodes_throws422_noUpstream() {
        CreateQuoteCommand command = new CreateQuoteCommand(
                Lob.ULIP, "MULTI", "SUM_ASSURED", new BigDecimal("500000"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LA", 1, "1990-01-15", "M", false, new BigDecimal("1"), "400001")),
                Map.of(), null, "j-1", null, "idem", "rm",
                new CreateQuoteCommand.ProductSelection(
                        "BALIC", null, null, null, null, null, null, null, null));
        assertThatThrownBy(() -> service.fundPerformance(command))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.VALIDATION_ERROR));
        verify(port, never()).fundPerformance(any());
    }

    private static CreateQuoteCommand listCommand(Lob lob, boolean pin) {
        CreateQuoteCommand.ProductSelection selection = pin
                ? new CreateQuoteCommand.ProductSelection(
                        "BALIC", List.of("345"), null, null, null, null, null, null, null)
                : null;
        return new CreateQuoteCommand(
                lob, "MULTI", "SUM_ASSURED", new BigDecimal("500000"), null,
                List.of(new CreateQuoteCommand.MemberDetail(
                        "LA", 1, "1990-01-15", "M", false, new BigDecimal("1000000"), "400001")),
                Map.of(),
                new CreateQuoteCommand.DistributionContext(null, "109337", "B2B"),
                "j-1", null, "idem-1", "rm-1", selection
        );
    }
}
