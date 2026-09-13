package com.bank.insurance.onesb.api;

import com.bank.common.domain.Lob;
import com.bank.insurance.onesb.api.dto.CreateQuoteRequest;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("FUNC-027")
class QuoteCommandMapperTest {

    @Test
    void toCommand_nullSequenceNumber_defaultsZero_andCopiesSelection() {
        CreateQuoteRequest request = new CreateQuoteRequest(
                Lob.ULIP, "SINGLE", "SUM_ASSURED", new BigDecimal("500000"), null,
                List.of(new CreateQuoteRequest.MemberRequest(
                        "LA", null, "1990-01-15", "M", true, new BigDecimal("1"), "400001")),
                null,
                new CreateQuoteRequest.DistributionRequest("E1", "109337", "B2B"),
                "j-1", "s-1",
                new CreateQuoteRequest.ProductSelectionRequest(
                        "BALIC", List.of("345"), "P1", "C1", "D1", 20, 10, "YEARLY", "REGULAR")
        );

        CreateQuoteCommand command = QuoteCommandMapper.toCommand(request, "idem", "rm-1");

        assertThat(command.members()).hasSize(1);
        assertThat(command.members().get(0).sequenceNumber()).isZero();
        assertThat(command.members().get(0).tobacco()).isTrue();
        assertThat(command.distribution().agentId()).isEqualTo("109337");
        assertThat(command.selection().insurerCode()).isEqualTo("BALIC");
        assertThat(command.actorId()).isEqualTo("rm-1");
    }

    @Test
    void toCommand_blankActor_defaultsSystem_andSkipsOptionalBlocks() {
        CreateQuoteRequest request = new CreateQuoteRequest(
                Lob.TERM, null, null, new BigDecimal("1"), null,
                List.of(new CreateQuoteRequest.MemberRequest(
                        null, 3, "1990-01-15", "F", null, null, null)),
                null, null, null, null, null);

        CreateQuoteCommand command = QuoteCommandMapper.toCommand(request, null, " ");

        assertThat(command.members().get(0).sequenceNumber()).isEqualTo(3);
        assertThat(command.distribution()).isNull();
        assertThat(command.selection()).isNull();
        assertThat(command.actorId()).isEqualTo("system");
    }
}
