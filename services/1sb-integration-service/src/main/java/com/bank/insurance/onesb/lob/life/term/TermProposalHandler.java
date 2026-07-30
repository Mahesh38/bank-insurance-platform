package com.bank.insurance.onesb.lob.life.term;

import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.lob.LobProposalHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Term LOB proposal handler — builds the 1SB get-proposal-form path.
 * <p>
 * Schema path: {@code GET /insurance/lifeterm/v1/proposal/form}
 * with optional query params {@code productCode}, {@code manufacturerId}, {@code version}.
 */
@Component
public class TermProposalHandler implements LobProposalHandler {

    static final String SCHEMA_PATH = "/insurance/lifeterm/v1/proposal/form";

    @Override
    public Lob supportedLob() {
        return Lob.TERM;
    }

    @Override
    public String schemaPath(String productCode, String manufacturerId, String version) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(SCHEMA_PATH);
        if (productCode != null && !productCode.isBlank()) {
            builder.queryParam("productCode", productCode);
        }
        if (manufacturerId != null && !manufacturerId.isBlank()) {
            builder.queryParam("manufacturerId", manufacturerId);
        }
        if (version != null && !version.isBlank()) {
            builder.queryParam("version", version);
        }
        return builder.build().encode().toUriString();
    }
}
