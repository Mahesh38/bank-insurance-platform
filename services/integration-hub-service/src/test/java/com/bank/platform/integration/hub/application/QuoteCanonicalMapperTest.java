package com.bank.platform.integration.hub.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.platform.integration.hub.domain.CanonicalState;
import com.bank.platform.integration.hub.domain.OnesbQuotePort.AdapterOffer;
import com.bank.platform.integration.hub.domain.OnesbQuotePort.AdapterQuoteJob;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-032")
class QuoteCanonicalMapperTest {

  @Test
  void errorSummary_isPartialErrorOnly_AT_HUB_06() {
    AdapterQuoteJob job =
        new AdapterQuoteJob(
            "j1",
            "PARTIAL",
            null,
            List.of(
                new AdapterOffer(
                    "off-1",
                    "INS01",
                    "A",
                    "P1",
                    "Prod",
                    BigDecimal.TEN,
                    "YEARLY",
                    BigDecimal.valueOf(100),
                    false,
                    null,
                    null,
                    10,
                    5,
                    List.of()),
                new AdapterOffer(
                    "off-bad",
                    "INS02",
                    "B",
                    "P2",
                    "Bad",
                    null,
                    null,
                    null,
                    false,
                    null,
                    "rejected",
                    null,
                    null,
                    List.of())));

    var mapped = QuoteCanonicalMapper.mapJob(job);
    assertThat(mapped.state()).isEqualTo(CanonicalState.PARTIALLY_COMPLETED);
    assertThat(mapped.result().offers()).hasSize(1);
    assertThat(mapped.result().offers().getFirst().providerOfferRef()).isEqualTo("off-1");
    assertThat(mapped.result().partialErrors()).hasSize(1);
    assertThat(mapped.result().partialErrors().getFirst().insurerCode()).isEqualTo("INS02");
  }
}
